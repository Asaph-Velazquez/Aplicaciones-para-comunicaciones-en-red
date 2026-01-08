package backend;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

public class LocalFileServer {
    private final String baseDirectory;
    private final String outputDirectory;

    public LocalFileServer(String baseDirectory) {
        this.baseDirectory = baseDirectory;
        this.outputDirectory = null;
    }

    public LocalFileServer(String baseDirectory, String outputDirectory) {
        this.baseDirectory = baseDirectory;
        this.outputDirectory = outputDirectory;
    }

    public HTTPResponse handleRequest(String requestedPath) {
        try {
            String safePath = sanitizePath(requestedPath);
            File file = new File(baseDirectory, safePath);

            if (!isWithinBaseDirectory(file)) {
                return HTTPResponse.errorResponse(403, "Acceso denegado");
            }

            if (!file.exists()) {
                return HTTPResponse.errorResponse(404, "Archivo o directorio no encontrado: " + requestedPath);
            }

            if (file.isDirectory()) {
                return handleDirectoryRequest(file);
            }

            return handleFileRequest(file);

        } catch (Exception e) {
            return HTTPResponse.errorResponse(500, "Error interno: " + e.getMessage());
        }
    }

    public DownloadResult handleDownload(String requestedPath) {
        if (outputDirectory == null) {
            return null;
        }

        DownloadResult result = new DownloadResult();
        result.startTime = System.currentTimeMillis();

        try {
            String safePath = sanitizePath(requestedPath);
            File file = new File(baseDirectory, safePath);

            System.out.println("\n" + "=".repeat(80));
            System.out.println("📂 DESCARGA LOCAL DEL DIRECTORIO DE PUBLICACIÓN");
            System.out.println("=".repeat(80));
            System.out.println("📍 Ruta solicitada: " + requestedPath);
            System.out.println("   Directorio base: " + baseDirectory);
            System.out.println("   Ruta completa: " + file.getAbsolutePath());

            if (!isWithinBaseDirectory(file)) {
                System.err.println("❌ ACCESO DENEGADO");
                System.out.println("=".repeat(80) + "\n");
                return null;
            }

            if (!file.exists()) {
                System.err.println("❌ ERROR: No encontrado");
                System.out.println("=".repeat(80) + "\n");
                return null;
            }

            System.out.println("   Tipo: " + (file.isDirectory() ? "Directorio" : "Archivo"));
            if (file.isFile()) {
                System.out.println("   Tamaño: " + formatFileSize(file.length()));
            }
            System.out.println("   Destino: " + outputDirectory);
            System.out.println("\n📦 INICIANDO COPIA...");

            if (file.isDirectory()) {
                copyDirectory(file, new File(outputDirectory, file.getName()), result);
            } else {
                copyFile(file, new File(outputDirectory, file.getName()), result);
            }

            System.out.println("\n✅ DESCARGA COMPLETADA:");
            System.out.println("   Archivos: " + result.filesDownloaded);
            System.out.println("   Bytes: " + formatFileSize(result.bytesDownloaded));
            System.out.println("   Tiempo: " + result.getDurationMs() + " ms");
            System.out.println("=".repeat(80) + "\n");

        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            System.out.println("=".repeat(80) + "\n");
        }

        result.endTime = System.currentTimeMillis();
        return result;
    }

    private HTTPResponse handleFileRequest(File file) throws IOException {
        byte[] fileContent = Files.readAllBytes(file.toPath());

        HTTPResponse response = new HTTPResponse(200, "OK");
        response.setBody(fileContent);
        response.addHeader("Content-Type", HTTPResponse.getMimeType(file.getName()));
        response.addHeader("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"");

        return response;
    }

    private HTTPResponse handleDirectoryRequest(File directory) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zipOut = new ZipOutputStream(baos);

        addDirectoryToZip(directory, directory.getName(), zipOut);

        zipOut.close();
        byte[] zipData = baos.toByteArray();

        HTTPResponse response = new HTTPResponse(200, "OK");
        response.setBody(zipData);
        response.addHeader("Content-Type", "application/zip");
        response.addHeader("Content-Disposition", "attachment; filename=\"" + directory.getName() + ".zip\"");

        return response;
    }

    private void addDirectoryToZip(File directory, String basePath, ZipOutputStream zipOut) throws IOException {
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            String entryName = basePath + "/" + file.getName();

            if (file.isDirectory()) {
                zipOut.putNextEntry(new ZipEntry(entryName + "/"));
                zipOut.closeEntry();
                addDirectoryToZip(file, entryName, zipOut);
            } else {
                zipOut.putNextEntry(new ZipEntry(entryName));
                Files.copy(file.toPath(), zipOut);
                zipOut.closeEntry();
            }
        }
    }

    public HTTPResponse getDirectoryListing(String requestedPath) {
        try {
            String safePath = sanitizePath(requestedPath);
            File directory = new File(baseDirectory, safePath);

            if (!directory.exists() || !directory.isDirectory()) {
                return HTTPResponse.errorResponse(404, "Directorio no encontrado");
            }

            if (!isWithinBaseDirectory(directory)) {
                return HTTPResponse.errorResponse(403, "Acceso denegado");
            }

            // Generar HTML con el listado
            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html>\n");
            html.append("<html>\n<head>\n");
            html.append("<title>Listado de ").append(requestedPath).append("</title>\n");
            html.append("<style>\n");
            html.append("body { font-family: Arial, sans-serif; margin: 20px; }\n");
            html.append("h1 { color: #333; }\n");
            html.append("ul { list-style: none; padding: 0; }\n");
            html.append("li { padding: 8px; border-bottom: 1px solid #eee; }\n");
            html.append("a { text-decoration: none; color: #0066cc; }\n");
            html.append("a:hover { text-decoration: underline; }\n");
            html.append(".dir { font-weight: bold; }\n");
            html.append(".file { color: #666; }\n");
            html.append("</style>\n");
            html.append("</head>\n<body>\n");
            html.append("<h1>📂 Listado de ").append(requestedPath).append("</h1>\n");
            html.append("<ul>\n");

            // Añadir enlace al directorio padre
            if (!safePath.isEmpty()) {
                String parentPath = new File(safePath).getParent();
                if (parentPath == null) {
                    parentPath = "";
                }
                html.append("<li><a href=\"/download/local?path=").append(parentPath)
                    .append("\">📁 ..</a> (Subir)</li>\n");
            }

            // Listar archivos y directorios
            File[] files = directory.listFiles();
            if (files != null) {
                Arrays.sort(files, (f1, f2) -> {
                    if (f1.isDirectory() && !f2.isDirectory()) return -1;
                    if (!f1.isDirectory() && f2.isDirectory()) return 1;
                    return f1.getName().compareTo(f2.getName());
                });

                for (File file : files) {
                    String relativePath = safePath.isEmpty() ? file.getName() : safePath + "/" + file.getName();
                    String icon = file.isDirectory() ? "📁" : "📄";
                    String cssClass = file.isDirectory() ? "dir" : "file";
                    
                    html.append("<li class=\"").append(cssClass).append("\">");
                    html.append("<a href=\"/download/local?path=").append(relativePath).append("\">");
                    html.append(icon).append(" ").append(file.getName());
                    
                    if (!file.isDirectory()) {
                        html.append(" <span style=\"color: #999; font-size: 0.9em;\">(")
                            .append(formatFileSize(file.length())).append(")</span>");
                    }
                    
                    html.append("</a></li>\n");
                }
            }

            html.append("</ul>\n");
            html.append("</body>\n</html>");

            HTTPResponse response = new HTTPResponse(200, "OK");
            response.setBody(html.toString());
            response.addHeader("Content-Type", "text/html; charset=UTF-8");
            return response;

        } catch (Exception e) {
            return HTTPResponse.errorResponse(500, "Error: " + e.getMessage());
        }
    }

    private String sanitizePath(String path) {
        if (path == null || path.isEmpty() || path.equals("/")) {
            return "";
        }

        path = path.replaceAll("^/+", "").replaceAll("/+$", "");
        path = path.replaceAll("\\.\\.", "").replaceAll("\\./", "");

        return path;
    }

    private boolean isWithinBaseDirectory(File file) {
        try {
            Path basePath = new File(baseDirectory).getCanonicalFile().toPath();
            Path filePath = file.getCanonicalFile().toPath();
            return filePath.startsWith(basePath);
        } catch (IOException e) {
            return false;
        }
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    private void copyFile(File source, File destination, DownloadResult result) throws IOException {
        Files.copy(source.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
        result.filesDownloaded++;
        result.bytesDownloaded += source.length();
        System.out.println("   ✅ " + source.getName() + " → " + formatFileSize(source.length()));
    }

    private void copyDirectory(File source, File destination, DownloadResult result) throws IOException {
        if (!destination.exists()) {
            destination.mkdirs();
            System.out.println("   📁 Creado directorio: " + destination.getName());
        }

        File[] files = source.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            File destFile = new File(destination, file.getName());
            
            if (file.isDirectory()) {
                copyDirectory(file, destFile, result);
            } else {
                copyFile(file, destFile, result);
            }
        }
    }

    public static class DownloadResult {
        public long startTime;
        public long endTime;
        public int filesDownloaded = 0;
        public long bytesDownloaded = 0;

        public long getDurationMs() {
            return endTime - startTime;
        }
    }
}
