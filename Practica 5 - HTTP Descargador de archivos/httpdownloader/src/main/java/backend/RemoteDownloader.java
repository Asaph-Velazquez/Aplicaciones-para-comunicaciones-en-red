package backend;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Implementa descarga recursiva de archivos desde servidores remotos.
 * Actúa como cliente HTTP usando sockets para realizar peticiones GET.
 * Comportamiento similar a wget -r
 */
public class RemoteDownloader {
    private final ExecutorService threadPool;
    private final Set<String> visitedUrls;
    private final Set<String> downloadedFiles;
    private final int maxDepth;
    private final String outputDir;

    /**
     * Constructor
     * @param maxThreads Número de hilos para descargas concurrentes
     * @param maxDepth Profundidad máxima de recursión
     * @param outputDir Directorio donde guardar los archivos descargados
     */
    public RemoteDownloader(int maxThreads, int maxDepth, String outputDir) {
        this.threadPool = Executors.newFixedThreadPool(maxThreads);
        this.visitedUrls = Collections.synchronizedSet(new HashSet<>());
        this.downloadedFiles = Collections.synchronizedSet(new HashSet<>());
        this.maxDepth = maxDepth;
        this.outputDir = outputDir;
    }

    /**
     * Inicia la descarga recursiva desde una URL
     * @param startUrl URL inicial
     * @return Información sobre los archivos descargados
     */
    public DownloadResult downloadRecursive(String startUrl) {
        DownloadResult result = new DownloadResult();
        result.startTime = System.currentTimeMillis();

        try {
            // Crear directorio de salida
            File dir = new File(outputDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // Iniciar descarga recursiva
            downloadRecursiveInternal(startUrl, 0, result);

            // Esperar a que todos los hilos terminen
            threadPool.shutdown();
            threadPool.awaitTermination(60, TimeUnit.SECONDS);

        } catch (Exception e) {
            result.errors.add("Error general: " + e.getMessage());
        }

        result.endTime = System.currentTimeMillis();
        return result;
    }

    /**
     * Método interno recursivo para descargar
     */
    private void downloadRecursiveInternal(String url, int depth, DownloadResult result) {
        // Verificar profundidad máxima
        if (depth > maxDepth) {
            return;
        }

        // Normalizar y verificar si ya fue visitada
        url = HTMLParser.normalizeUrl(url);
        if (!visitedUrls.add(url)) {
            return; // Ya visitada
        }

        try {
            // Descargar el contenido
            DownloadedContent content = downloadUrl(url);
            if (content == null) {
                result.errors.add("No se pudo descargar: " + url);
                return;
            }

            // Guardar el archivo
            String filename = getFilenameFromUrl(url);
            File outputFile = new File(outputDir, filename);
            saveToFile(content.data, outputFile);
            
            downloadedFiles.add(filename);
            result.filesDownloaded++;
            result.bytesDownloaded += content.data.length;

            // Si es HTML, extraer y seguir enlaces
            if (content.contentType != null && content.contentType.contains("text/html")) {
                String html = new String(content.data);
                List<String> links = HTMLParser.extractLinks(html, url);
                
                // Filtrar solo enlaces del mismo dominio
                for (String link : links) {
                    if (HTMLParser.isSameDomain(url, link)) {
                        // Descargar enlaces en paralelo (con límite de profundidad)
                        final int nextDepth = depth + 1;
                        threadPool.submit(() -> downloadRecursiveInternal(link, nextDepth, result));
                    }
                }
            }

        } catch (Exception e) {
            result.errors.add("Error descargando " + url + ": " + e.getMessage());
        }
    }

    /**
     * Descarga una URL usando sockets y peticiones GET
     * @param urlString URL a descargar
     * @return Contenido descargado o null si hay error
     */
    public DownloadedContent downloadUrl(String urlString) {
        Socket socket = null;
        try {
            URL url = new URL(urlString);
            String host = url.getHost();
            int port = url.getPort() == -1 ? 80 : url.getPort();
            String path = url.getPath().isEmpty() ? "/" : url.getPath();
            if (url.getQuery() != null) {
                path += "?" + url.getQuery();
            }

            // Conectar usando socket
            socket = new Socket(host, port);
            socket.setSoTimeout(15000); // Timeout de 15 segundos

            // Construir petición HTTP GET
            OutputStream outputStream = socket.getOutputStream();
            String request = "GET " + path + " HTTP/1.1\r\n" +
                           "Host: " + host + "\r\n" +
                           "User-Agent: JavaHTTPDownloader/1.0\r\n" +
                           "Accept: */*\r\n" +
                           "Connection: close\r\n" +
                           "\r\n";
            
            // LOG: Mostrar petición enviada
            System.out.println("\n" + "=".repeat(80));
            System.out.println("🌐 PETICIÓN GET A: " + urlString);
            System.out.println("=".repeat(80));
            System.out.println("📤 ENCABEZADOS DE PETICIÓN:");
            System.out.println("   GET " + path + " HTTP/1.1");
            System.out.println("   Host: " + host);
            System.out.println("   User-Agent: JavaHTTPDownloader/1.0");
            System.out.println("   Accept: */*");
            System.out.println("   Connection: close");
            
            outputStream.write(request.getBytes("UTF-8"));
            outputStream.flush();

            // Leer respuesta completa
            InputStream input = socket.getInputStream();
            
            // Leer línea de estado (byte por byte hasta encontrar \r\n)
            String statusLine = readLine(input);
            
            // LOG: Mostrar respuesta
            System.out.println("\n📥 RESPUESTA DEL SERVIDOR:");
            System.out.println("   " + statusLine);
            
            if (statusLine == null || !statusLine.contains("200")) {
                System.err.println("❌ Estado HTTP no OK para " + urlString + ": " + statusLine);
                System.out.println("=".repeat(80) + "\n");
                return null;
            }

            // Leer headers (línea por línea hasta encontrar línea vacía)
            Map<String, String> headers = new HashMap<>();
            System.out.println("\n📋 ENCABEZADOS DE RESPUESTA:");
            String line;
            while ((line = readLine(input)) != null && !line.isEmpty()) {
                System.out.println("   " + line);
                int colonIndex = line.indexOf(':');
                if (colonIndex > 0) {
                    String headerName = line.substring(0, colonIndex).trim().toLowerCase();
                    String headerValue = line.substring(colonIndex + 1).trim();
                    headers.put(headerName, headerValue);
                }
            }

            // Leer body completo
            ByteArrayOutputStream bodyStream = new ByteArrayOutputStream();
            
            System.out.println("\n📦 CONTENIDO:");
            
            // Verificar si tiene Content-Length
            String contentLengthStr = headers.get("content-length");
            if (contentLengthStr != null) {
                try {
                    int contentLength = Integer.parseInt(contentLengthStr);
                    System.out.println("   Content-Length: " + contentLength + " bytes");
                    byte[] buffer = new byte[8192];
                    int totalRead = 0;
                    int bytesRead;
                    
                    while (totalRead < contentLength && (bytesRead = input.read(buffer, 0, 
                            Math.min(buffer.length, contentLength - totalRead))) != -1) {
                        bodyStream.write(buffer, 0, bytesRead);
                        totalRead += bytesRead;
                    }
                    System.out.println("   Bytes leídos: " + totalRead);
                } catch (NumberFormatException e) {
                    // Si el Content-Length no es válido, leer hasta el final
                    System.out.println("   Content-Length inválido, leyendo hasta el final...");
                    readUntilEnd(input, bodyStream);
                }
            } else {
                // No hay Content-Length, leer hasta que se cierre la conexión
                System.out.println("   Sin Content-Length, leyendo hasta cierre de conexión...");
                readUntilEnd(input, bodyStream);
            }

            DownloadedContent content = new DownloadedContent();
            content.data = bodyStream.toByteArray();
            content.contentType = headers.get("content-type");
            
            System.out.println("\n✅ DESCARGA EXITOSA:");
            System.out.println("   URL: " + urlString);
            System.out.println("   Tamaño final: " + content.data.length + " bytes");
            System.out.println("   Content-Type: " + (content.contentType != null ? content.contentType : "desconocido"));
            
            // Mostrar preview del contenido si es texto
            if (content.contentType != null && content.contentType.contains("text")) {
                String preview = new String(content.data, "UTF-8");
                if (preview.length() > 200) {
                    preview = preview.substring(0, 200) + "...";
                }
                System.out.println("\n📄 PREVIEW DEL CONTENIDO:");
                System.out.println("   " + preview.replace("\n", "\n   "));
            }
            System.out.println("=".repeat(80) + "\n");
            
            return content;

        } catch (Exception e) {
            System.err.println("❌ Error descargando " + urlString + ": " + e.getMessage());
            return null;
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ignored) {}
            }
        }
    }

    /**
     * Lee una línea del InputStream (hasta encontrar \r\n o \n)
     */
    private String readLine(InputStream input) throws IOException {
        ByteArrayOutputStream lineBuffer = new ByteArrayOutputStream();
        int b;
        int prev = -1;
        
        while ((b = input.read()) != -1) {
            if (b == '\n') {
                // Encontramos fin de línea
                break;
            }
            if (prev == '\r') {
                // Si el anterior era \r y este no es \n, agregar el \r
                if (b != '\n') {
                    lineBuffer.write(prev);
                }
            }
            if (b != '\r') {
                lineBuffer.write(b);
            }
            prev = b;
        }
        
        if (lineBuffer.size() == 0 && b == -1) {
            return null;
        }
        
        return new String(lineBuffer.toByteArray(), "UTF-8");
    }

    /**
     * Lee datos hasta que el stream se cierre
     */
    private void readUntilEnd(InputStream input, ByteArrayOutputStream output) throws IOException {
        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = input.read(buffer)) != -1) {
            output.write(buffer, 0, bytesRead);
        }
    }

    /**
     * Guarda datos en un archivo
     */
    private void saveToFile(byte[] data, File file) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(data);
        }
    }

    /**
     * Extrae un nombre de archivo válido desde una URL
     */
    private String getFilenameFromUrl(String url) {
        try {
            URL urlObj = new URL(url);
            String path = urlObj.getPath();
            
            if (path.isEmpty() || path.equals("/")) {
                return "index.html";
            }
            
            // Obtener el último segmento
            String[] segments = path.split("/");
            String filename = segments[segments.length - 1];
            
            if (filename.isEmpty()) {
                return "index.html";
            }
            
            // Sanitizar el nombre
            filename = filename.replaceAll("[^a-zA-Z0-9._-]", "_");
            
            // Si no tiene extensión, asumir HTML
            if (!filename.contains(".")) {
                filename += ".html";
            }
            
            // Evitar colisiones añadiendo timestamp si ya existe
            File file = new File(outputDir, filename);
            if (downloadedFiles.contains(filename)) {
                String name = filename.substring(0, filename.lastIndexOf('.'));
                String ext = filename.substring(filename.lastIndexOf('.'));
                filename = name + "_" + System.currentTimeMillis() + ext;
            }
            
            return filename;
        } catch (Exception e) {
            return "file_" + System.currentTimeMillis() + ".html";
        }
    }

    /**
     * Clase para almacenar contenido descargado
     */
    public static class DownloadedContent {
        public byte[] data;
        public String contentType;
    }

    /**
     * Clase para almacenar resultado de la descarga
     */
    public static class DownloadResult {
        public long startTime;
        public long endTime;
        public int filesDownloaded = 0;
        public long bytesDownloaded = 0;
        public List<String> errors = new ArrayList<>();

        public long getDurationMs() {
            return endTime - startTime;
        }
    }
}
