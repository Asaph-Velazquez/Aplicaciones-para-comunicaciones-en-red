package backend;

import java.io.*;
import java.net.*;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private static final String DOWNLOADS_DIR = "downloads";
    private static final String WWW_DIR = "www";

    public ClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        try {
            InputStream input = clientSocket.getInputStream();
            OutputStream output = clientSocket.getOutputStream();

            HTTPRequest request = new HTTPRequest(input);
            
            System.out.println("📨 " + request.getMethod() + " " + request.getPath() + 
                             " desde " + clientSocket.getInetAddress().getHostAddress());

            HTTPResponse response = handleRequest(request);
            response.send(output);
            clientSocket.close();

        } catch (IOException e) {
            try {
                if (!clientSocket.isClosed()) {
                    HTTPResponse.errorResponse(500, "Error interno del servidor")
                        .send(clientSocket.getOutputStream());
                }
            } catch (IOException ignored) {
            }
        } finally {
            try {
                clientSocket.close();
            } catch (IOException ignored) {
            }
        }
    }

    private HTTPResponse handleRequest(HTTPRequest request) {
        String path = request.getPath();
        String method = request.getMethod();

        if (!method.equals("GET")) {
            return HTTPResponse.errorResponse(405, "Solo se permite el método GET");
        }

        if (path.equals("/") || path.equals("/index.html")) {
            return serveIndexPage();
        }

        if (path.startsWith("/download/net")) {
            return handleRemoteDownload(request);
        }

        if (path.startsWith("/download/local")) {
            return handleLocalDownload(request);
        }

        if (path.startsWith("/static/")) {
            return serveStaticFile(path);
        }

        return HTTPResponse.errorResponse(404, "Ruta no encontrada: " + path);
    }

    private HTTPResponse serveIndexPage() {
        try {
            File htmlFile = new File(WWW_DIR, "interface.html");
            if (!htmlFile.exists()) {
                return HTTPResponse.errorResponse(500, "Archivo de interfaz no encontrado");
            }

            byte[] htmlContent = java.nio.file.Files.readAllBytes(htmlFile.toPath());
            String html = new String(htmlContent, "UTF-8");

            HTTPResponse response = new HTTPResponse(200, "OK");
            response.setBody(html);
            response.addHeader("Content-Type", "text/html; charset=UTF-8");
            return response;

        } catch (IOException e) {
            return HTTPResponse.errorResponse(500, "Error al leer la interfaz: " + e.getMessage());
        }
    }

    private HTTPResponse handleRemoteDownload(HTTPRequest request) {
        String url = request.getQueryParam("url");
        
        if (url == null || url.isEmpty()) {
            return HTTPResponse.errorResponse(400, "Parámetro 'url' requerido");
        }

        try {
            System.out.println("🌐 Iniciando descarga remota desde: " + url);
            
            File downloadsDir = new File(DOWNLOADS_DIR);
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs();
            }

            String timestamp = String.valueOf(System.currentTimeMillis());
            File outputDir = new File(DOWNLOADS_DIR, "remote_" + timestamp);
            outputDir.mkdirs();

            RemoteDownloader downloader = new RemoteDownloader(5, 100, outputDir.getAbsolutePath());
            RemoteDownloader.DownloadResult result = downloader.downloadRecursive(url);

            File templateFile = new File(WWW_DIR, "result.html");
            if (!templateFile.exists()) {
                return HTTPResponse.errorResponse(500, "Plantilla de resultado no encontrada");
            }

            byte[] templateContent = java.nio.file.Files.readAllBytes(templateFile.toPath());
            String html = new String(templateContent, "UTF-8");

            html = html.replace("{{STATUS_ICON}}", "✅");
            html = html.replace("{{STATUS_TITLE}}", "Descarga Remota Completada");
            html = html.replace("{{FILES_COUNT}}", String.valueOf(result.filesDownloaded));
            html = html.replace("{{BYTES_FORMATTED}}", formatBytes(result.bytesDownloaded));
            html = html.replace("{{TIME_MS}}", String.valueOf(result.getDurationMs()));
            html = html.replace("{{DIRECTORY}}", outputDir.getAbsolutePath());

            String errorsSection = "";
            if (!result.errors.isEmpty()) {
                StringBuilder errors = new StringBuilder();
                errors.append("<div class=\"errors\">\n");
                errors.append("<h3>⚠️ Errores encontrados</h3>\n");
                errors.append("<ul>\n");
                for (String error : result.errors) {
                    errors.append("<li>").append(escapeHtml(error)).append("</li>\n");
                }
                errors.append("</ul>\n");
                errors.append("</div>\n");
                errorsSection = errors.toString();
            }
            html = html.replace("{{ERRORS_SECTION}}", errorsSection);

            HTTPResponse response = new HTTPResponse(200, "OK");
            response.setBody(html);
            response.addHeader("Content-Type", "text/html; charset=UTF-8");
            return response;

        } catch (Exception e) {
            return HTTPResponse.errorResponse(500, "Error en descarga remota: " + e.getMessage());
        }
    }

    private HTTPResponse handleLocalDownload(HTTPRequest request) {
        String path = request.getQueryParam("path");
        
        if (path == null) {
            path = "";
        }

        System.out.println("📂 Descarga local solicitada: " + path);

        try {
            File downloadsDir = new File(DOWNLOADS_DIR);
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs();
            }

            String timestamp = String.valueOf(System.currentTimeMillis());
            File outputDir = new File(DOWNLOADS_DIR, "local_" + timestamp);
            outputDir.mkdirs();

            LocalFileServer fileServer = new LocalFileServer(WWW_DIR, outputDir.getAbsolutePath());
            LocalFileServer.DownloadResult result = fileServer.handleDownload(path);

            if (result == null) {
                return HTTPResponse.errorResponse(404, "Archivo o directorio no encontrado: " + path);
            }

            File templateFile = new File(WWW_DIR, "result.html");
            if (!templateFile.exists()) {
                return HTTPResponse.errorResponse(500, "Plantilla de resultado no encontrada");
            }

            byte[] templateContent = java.nio.file.Files.readAllBytes(templateFile.toPath());
            String html = new String(templateContent, "UTF-8");

            html = html.replace("{{STATUS_ICON}}", "✅");
            html = html.replace("{{STATUS_TITLE}}", "Descarga Local Completada");
            html = html.replace("{{FILES_COUNT}}", String.valueOf(result.filesDownloaded));
            html = html.replace("{{BYTES_FORMATTED}}", formatBytes(result.bytesDownloaded));
            html = html.replace("{{TIME_MS}}", String.valueOf(result.getDurationMs()));
            html = html.replace("{{DIRECTORY}}", outputDir.getAbsolutePath());
            html = html.replace("{{ERRORS_SECTION}}", "");

            HTTPResponse response = new HTTPResponse(200, "OK");
            response.setBody(html);
            response.addHeader("Content-Type", "text/html; charset=UTF-8");
            return response;

        } catch (Exception e) {
            return HTTPResponse.errorResponse(500, "Error en descarga local: " + e.getMessage());
        }
    }

    private HTTPResponse serveStaticFile(String path) {
        try {
            File file = new File(WWW_DIR + path);
            if (!file.exists() || !file.isFile()) {
                return HTTPResponse.errorResponse(404, "Archivo no encontrado");
            }

            byte[] content = java.nio.file.Files.readAllBytes(file.toPath());
            HTTPResponse response = new HTTPResponse(200, "OK");
            response.setBody(content);
            response.addHeader("Content-Type", HTTPResponse.getMimeType(file.getName()));
            return response;

        } catch (IOException e) {
            return HTTPResponse.errorResponse(500, "Error al leer archivo: " + e.getMessage());
        }
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#x27;");
    }
}
