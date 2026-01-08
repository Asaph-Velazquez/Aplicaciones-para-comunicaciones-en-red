package backend;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class RemoteDownloader {
    private final ExecutorService threadPool;
    private final Set<String> visitedUrls;
    private final Set<String> downloadedFiles;
    private final int maxDepth;
    private final String outputDir;
    private String startUrlPath;
    private String baseHost;
    private final Object dirLock = new Object();

    public RemoteDownloader(int maxThreads, int maxDepth, String outputDir) {
        this.threadPool = Executors.newFixedThreadPool(maxThreads);
        this.visitedUrls = Collections.synchronizedSet(new HashSet<>());
        this.downloadedFiles = Collections.synchronizedSet(new HashSet<>());
        this.maxDepth = maxDepth;
        this.outputDir = outputDir;
    }

    public DownloadResult downloadRecursive(String startUrl) {
        DownloadResult result = new DownloadResult();
        result.startTime = System.currentTimeMillis();

        try {
            URL urlObj = new URL(startUrl);
            this.startUrlPath = urlObj.getPath();
            this.baseHost = urlObj.getHost();
            if (!this.startUrlPath.endsWith("/")) {
                int lastSlash = this.startUrlPath.lastIndexOf('/');
                if (lastSlash > 0) {
                    this.startUrlPath = this.startUrlPath.substring(0, lastSlash + 1);
                }
            }
            
            System.out.println("🎯 Scope de descarga limitado a: " + this.baseHost + this.startUrlPath + " y subdirectorios");
            
            File dir = new File(outputDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            downloadRecursiveInternal(startUrl, 0, result);

            System.out.println("\n⏳ Esperando a que terminen todas las descargas...");
            
            ThreadPoolExecutor executor = (ThreadPoolExecutor) threadPool;
            int lastActiveCount = -1;
            int stableCount = 0;
            
            while (true) {
                int activeCount = executor.getActiveCount();
                long queueSize = executor.getQueue().size();
                long completedTasks = executor.getCompletedTaskCount();
                
                if (activeCount != lastActiveCount) {
                    System.out.println(String.format(
                        "   Hilos activos: %d | En cola: %d | Completadas: %d",
                        activeCount, queueSize, completedTasks
                    ));
                    lastActiveCount = activeCount;
                    stableCount = 0;
                }
                
                if (activeCount == 0 && queueSize == 0) {
                    stableCount++;
                    if (stableCount >= 3) {
                        break;
                    }
                }
                
                Thread.sleep(500);
            }
            
            System.out.println("✅ Todas las descargas completadas\n");
            
            threadPool.shutdown();
            threadPool.awaitTermination(10, TimeUnit.SECONDS);

        } catch (Exception e) {
            result.errors.add("Error general: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (!threadPool.isShutdown()) {
                threadPool.shutdownNow();
            }
        }

        result.endTime = System.currentTimeMillis();
        return result;
    }

    private boolean isWithinScope(String url) {
        if (startUrlPath == null) return true;
        
        try {
            URL urlObj = new URL(url);
            String path = urlObj.getPath().replaceAll("/+", "/");
            String normalizedStartPath = startUrlPath.replaceAll("/+", "/");
            
            boolean withinScope = path.startsWith(normalizedStartPath);
            
            if (!withinScope) {
                System.out.println("    ⛔ Fuera de scope: " + url);
                System.out.println("       Path actual: " + path);
                System.out.println("       Path base: " + normalizedStartPath);
            }
            
            return withinScope;
        } catch (Exception e) {
            return false;
        }
    }

    private void downloadRecursiveInternal(String url, int depth, DownloadResult result) {
        if (depth > maxDepth) {
            System.out.println("⚠️  Profundidad máxima alcanzada (" + maxDepth + ") para: " + url);
            return;
        }
        
        String indent = "  ".repeat(depth);
        System.out.println(indent + "🔽 Nivel " + depth + ": " + url);

        url = HTMLParser.normalizeUrl(url);
        if (!visitedUrls.add(url)) {
            System.out.println(indent + "⏭️  Ya visitada: " + url);
            return;
        }

        try {
            DownloadedContent content = downloadUrl(url);
            if (content == null) {
                System.out.println(indent + "⚠️  No se pudo descargar");
                return;
            }

            boolean isDirectoryListing = false;
            if (content.contentType != null && content.contentType.contains("text/html")) {
                String html = new String(content.data);
                List<String> directoryFiles = HTMLParser.extractDirectoryListing(html, url);
                isDirectoryListing = !directoryFiles.isEmpty();
            }

            String relativePath = getRelativePathFromUrl(url, isDirectoryListing);
            try {
                File outputFile = createFileWithPath(url, isDirectoryListing);
                saveToFile(content.data, outputFile);
                
                downloadedFiles.add(relativePath);
                result.filesDownloaded++;
                result.bytesDownloaded += content.data.length;
            } catch (IOException e) {
                String errorMsg = "Error guardando " + url + ": " + e.getMessage();
                System.err.println(indent + "❌ " + errorMsg);
                result.errors.add(errorMsg);
                return;
            }

            if (content.contentType != null && content.contentType.contains("text/html")) {
                String html = new String(content.data);
                List<String> directoryFiles = HTMLParser.extractDirectoryListing(html, url);
                
                if (!directoryFiles.isEmpty()) {
                    System.out.println("📁 Procesando listado de directorio: " + directoryFiles.size() + " elementos");
                    System.out.println("📝 HTML del listado guardado en: " + relativePath);
                    
                    for (String fileUrl : directoryFiles) {
                        System.out.println("  🔍 Analizando: " + fileUrl);
                        
                        if (!isWithinScope(fileUrl)) continue;
                        
                        if (!HTMLParser.isSameDomain(url, fileUrl)) {
                            System.out.println("    ⚠️ Dominio diferente, saltando");
                            continue;
                        }
                        
                        if (fileUrl.endsWith("/")) {
                            System.out.println("    📂 → Subdirectorio");
                            final int nextDepth = depth + 1;
                            threadPool.submit(() -> downloadRecursiveInternal(fileUrl, nextDepth, result));
                        } else if (HTMLParser.isLikelyFile(fileUrl)) {
                            System.out.println("    📄 → Archivo");
                            threadPool.submit(() -> downloadResourceOnly(fileUrl, result));
                        } else {
                            System.out.println("    ❓ → Elemento ambiguo");
                            final int nextDepth = depth + 1;
                            threadPool.submit(() -> tryAsDirectoryFirst(fileUrl, nextDepth, result));
                        }
                    }
                } else {
                    List<String> resources = HTMLParser.extractResources(html, url);
                    
                    if (!resources.isEmpty()) {
                        System.out.println("🔍 Encontrados " + resources.size() + " recursos");
                        
                        for (String resource : resources) {
                            if (HTMLParser.isSameDomain(url, resource)) {
                                System.out.println("  📦 Descargando recurso: " + resource);
                                threadPool.submit(() -> downloadResourceOnly(resource, result));
                            }
                        }
                    }
                    
                    List<String> links = HTMLParser.extractLinks(html, url);
                    for (String link : links) {
                        if (HTMLParser.isSameDomain(url, link)) {
                            final int nextDepth = depth + 1;
                            threadPool.submit(() -> downloadRecursiveInternal(link, nextDepth, result));
                        }
                    }
                }
            }

        } catch (Exception e) {
            result.errors.add("Error descargando " + url + ": " + e.getMessage());
        }
    }

    private void downloadResourceOnly(String url, DownloadResult result) {
        url = HTMLParser.normalizeUrl(url);
        if (!visitedUrls.add(url)) return;

        try {
            System.out.println("    ⬇️  Descargando: " + url);
            
            DownloadedContent content = downloadUrl(url);
            if (content == null) {
                System.out.println("    ⚠️  No disponible");
                return;
            }

            try {
                File outputFile = createFileWithPath(url);
                saveToFile(content.data, outputFile);
                
                String relativePath = getRelativePathFromUrl(url);
                downloadedFiles.add(relativePath);
                result.filesDownloaded++;
                result.bytesDownloaded += content.data.length;
                
                System.out.println("    ✅ Guardado: " + relativePath + " (" + content.data.length + " bytes)");
            } catch (IOException e) {
                String errorMsg = "Error guardando " + url + ": " + e.getMessage();
                System.err.println("    ❌ " + errorMsg);
                result.errors.add(errorMsg);
            }

        } catch (Exception e) {
            System.err.println("    ❌ Error: " + e.getMessage());
            result.errors.add("Error descargando " + url + ": " + e.getMessage());
        }
    }

    private void tryAsDirectoryFirst(String url, int depth, DownloadResult result) {
        String dirUrl = url.endsWith("/") ? url : url + "/";
        String normalizedDirUrl = HTMLParser.normalizeUrl(dirUrl);
        
        if (visitedUrls.contains(normalizedDirUrl)) {
            System.out.println("    ⏭️  Ya visitado como directorio");
            return;
        }
        
        System.out.println("    🔍 Intentando como directorio: " + dirUrl);
        
        DownloadedContent content = downloadUrl(dirUrl);
        
        if (content != null && content.contentType != null && content.contentType.contains("text/html")) {
            System.out.println("    ✅ Confirmado como directorio");
            visitedUrls.add(normalizedDirUrl);
            
            try {
                File outputFile = createFileWithPath(dirUrl, true);
                saveToFile(content.data, outputFile);
                
                String relativePath = getRelativePathFromUrl(dirUrl, true);
                downloadedFiles.add(relativePath);
                result.filesDownloaded++;
                result.bytesDownloaded += content.data.length;
            } catch (Exception e) {
                result.errors.add("Error guardando directorio " + dirUrl + ": " + e.getMessage());
            }
            
            String html = new String(content.data);
            List<String> directoryFiles = HTMLParser.extractDirectoryListing(html, dirUrl);
            
            if (!directoryFiles.isEmpty()) {
                System.out.println("    📁 Encontrados " + directoryFiles.size() + " elementos");
                
                for (String fileUrl : directoryFiles) {
                    if (HTMLParser.isSameDomain(dirUrl, fileUrl)) {
                        if (fileUrl.endsWith("/")) {
                            final int nextDepth = depth + 1;
                            threadPool.submit(() -> downloadRecursiveInternal(fileUrl, nextDepth, result));
                        } else if (HTMLParser.isLikelyFile(fileUrl)) {
                            threadPool.submit(() -> downloadResourceOnly(fileUrl, result));
                        } else {
                            final int nextDepth = depth + 1;
                            threadPool.submit(() -> tryAsDirectoryFirst(fileUrl, nextDepth, result));
                        }
                    }
                }
            }
        } else {
            System.out.println("    🔄 No es directorio, intentando como archivo");
            
            String normalizedUrl = HTMLParser.normalizeUrl(url);
            if (visitedUrls.contains(normalizedUrl)) {
                System.out.println("    ⏭️  Ya visitado como archivo");
                return;
            }
            
            DownloadedContent fileContent = downloadUrl(url);
            if (fileContent == null) {
                System.out.println("    ❌ No existe");
                return;
            }
            
            visitedUrls.add(normalizedUrl);
            try {
                File outputFile = createFileWithPath(url);
                saveToFile(fileContent.data, outputFile);
                
                String relativePath = getRelativePathFromUrl(url);
                downloadedFiles.add(relativePath);
                result.filesDownloaded++;
                result.bytesDownloaded += fileContent.data.length;
                
                System.out.println("    ✅ Guardado: " + relativePath + " (" + fileContent.data.length + " bytes)");
            } catch (Exception e) {
                System.err.println("    ❌ Error guardando: " + e.getMessage());
            }
        }
    }

    public DownloadedContent downloadUrl(String urlString) {
        return downloadUrlWithRedirects(urlString, 0);
    }

    private DownloadedContent downloadUrlWithRedirects(String urlString, int redirectCount) {
        final int MAX_REDIRECTS = 10;
        
        if (redirectCount > MAX_REDIRECTS) {
            System.err.println("❌ Demasiadas redirecciones para: " + urlString);
            return null;
        }
        
        Socket socket = null;
        try {
            URL url = new URL(urlString);
            String host = url.getHost();
            int port = url.getPort() == -1 ? 80 : url.getPort();
            String path = url.getPath().isEmpty() ? "/" : url.getPath();
            if (url.getQuery() != null) {
                path += "?" + url.getQuery();
            }

            socket = new Socket(host, port);
            socket.setSoTimeout(15000);

            String request = "GET " + path + " HTTP/1.1\r\n" +
                           "Host: " + host + "\r\n" +
                           "User-Agent: JavaHTTPDownloader/1.0\r\n" +
                           "Accept: */*\r\n" +
                           "Connection: close\r\n" +
                           "\r\n";
            
            System.out.println("\n" + "=".repeat(80));
            System.out.println("🌐 PETICIÓN GET A: " + urlString);
            System.out.println("=".repeat(80));
            System.out.println("📤 ENCABEZADOS DE PETICIÓN:");
            System.out.println("   GET " + path + " HTTP/1.1");
            System.out.println("   Host: " + host);
            System.out.println("   User-Agent: JavaHTTPDownloader/1.0");
            System.out.println("   Accept: */*");
            System.out.println("   Connection: close");
            
            socket.getOutputStream().write(request.getBytes("UTF-8"));
            socket.getOutputStream().flush();

            InputStream input = socket.getInputStream();
            String statusLine = readLine(input);
            
            System.out.println("\n📥 RESPUESTA DEL SERVIDOR:");
            System.out.println("   " + statusLine);
            
            int statusCode = 0;
            try {
                String[] parts = statusLine.split("\\s+");
                if (parts.length >= 2) {
                    statusCode = Integer.parseInt(parts[1]);
                }
            } catch (Exception e) {
                System.err.println("❌ No se pudo parsear código de estado");
                System.out.println("=".repeat(80) + "\n");
                return null;
            }

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

            if (statusCode >= 300 && statusCode < 400) {
                String location = headers.get("location");
                if (location != null && !location.isEmpty()) {
                    String redirectUrl;
                    if (location.startsWith("http://") || location.startsWith("https://")) {
                        redirectUrl = location;
                    } else {
                        URL baseUrl = new URL(urlString);
                        URL resolvedUrl = new URL(baseUrl, location);
                        redirectUrl = resolvedUrl.toString();
                    }
                    
                    System.out.println("\n🔄 REDIRECCIONANDO (" + statusCode + ") a: " + redirectUrl);
                    System.out.println("=".repeat(80) + "\n");
                    
                    socket.close();
                    return downloadUrlWithRedirects(redirectUrl, redirectCount + 1);
                } else {
                    System.err.println("❌ Redirección sin header Location");
                    System.out.println("=".repeat(80) + "\n");
                    return null;
                }
            }
            
            if (statusCode < 200 || statusCode >= 300) {
                System.err.println("❌ Estado HTTP no exitoso: " + statusCode);
                System.out.println("=".repeat(80) + "\n");
                return null;
            }

            ByteArrayOutputStream bodyStream = new ByteArrayOutputStream();
            System.out.println("\n📦 CONTENIDO:");
            
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
                    System.out.println("   Content-Length inválido, leyendo hasta el final...");
                    readUntilEnd(input, bodyStream);
                }
            } else {
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

    private String readLine(InputStream input) throws IOException {
        ByteArrayOutputStream lineBuffer = new ByteArrayOutputStream();
        int b, prev = -1;
        
        while ((b = input.read()) != -1) {
            if (b == '\n') break;
            if (prev == '\r' && b != '\n') lineBuffer.write(prev);
            if (b != '\r') lineBuffer.write(b);
            prev = b;
        }
        
        return (lineBuffer.size() == 0 && b == -1) ? null : new String(lineBuffer.toByteArray(), "UTF-8");
    }

    private void readUntilEnd(InputStream input, ByteArrayOutputStream output) throws IOException {
        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = input.read(buffer)) != -1) {
            output.write(buffer, 0, bytesRead);
        }
    }

    private void saveToFile(byte[] data, File file) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(data);
        }
    }

    private String getRelativePathFromUrl(String url, boolean isDirectoryListing) {
        try {
            URL urlObj = new URL(url);
            String fullPath = urlObj.getHost() + urlObj.getPath();
            
            if (isDirectoryListing) {
                fullPath += fullPath.endsWith("/") ? "index.html" : "/index.html";
            } else if (fullPath.endsWith("/")) {
                fullPath += "index.html";
            }
            
            try {
                fullPath = URLDecoder.decode(fullPath, "UTF-8");
            } catch (Exception ignored) {}
            
            return fullPath;
        } catch (Exception e) {
            return "unknown_" + System.currentTimeMillis();
        }
    }
    
    private String getRelativePathFromUrl(String url) {
        return getRelativePathFromUrl(url, false);
    }

    private File createFileWithPath(String url, boolean isDirectoryListing) throws IOException {
        String relativePath = getRelativePathFromUrl(url, isDirectoryListing);
        File outputFile = new File(outputDir, relativePath);
        
        if (outputFile.getAbsolutePath().length() > 260) {
            throw new IOException("Ruta demasiado larga. Windows limita a 260 caracteres.");
        }
        
        File parentDir = outputFile.getParentFile();
        if (parentDir != null) {
            synchronized (dirLock) {
                if (!parentDir.exists()) {
                    File rootDir = new File(outputDir);
                    if (!rootDir.exists() && !rootDir.mkdirs()) {
                        throw new IOException("No se pudo crear el directorio raíz: " + outputDir);
                    }
                    
                    String rootPath = rootDir.getAbsolutePath();
                    String parentPath = parentDir.getAbsolutePath();
                    
                    if (parentPath.startsWith(rootPath)) {
                        String relativePart = parentPath.substring(rootPath.length());
                        if (relativePart.startsWith(File.separator)) {
                            relativePart = relativePart.substring(File.separator.length());
                        }
                        
                        String[] parts = relativePart.split("[\\\\/]");
                        File current = rootDir;
                        
                        for (String part : parts) {
                            if (part.isEmpty()) continue;
                            
                            current = new File(current, part);
                            if (!current.exists() && !current.mkdir() && !current.mkdirs() && !current.exists()) {
                                throw new IOException("No se pudo crear directorio: " + current.getAbsolutePath());
                            }
                        }
                    } else {
                        if (!parentDir.mkdirs() && !parentDir.exists()) {
                            throw new IOException("mkdirs() falló para: " + parentDir.getAbsolutePath());
                        }
                    }
                    
                    if (!parentDir.exists() || !parentDir.isDirectory()) {
                        throw new IOException("El directorio no existe o no es válido: " + parentDir.getAbsolutePath());
                    }
                }
            }
        }
        
        if (outputFile.exists() && outputFile.isDirectory()) {
            throw new IOException("El path ya existe como directorio: " + outputFile.getAbsolutePath());
        }
        
        return outputFile;
    }
    
    private File createFileWithPath(String url) throws IOException {
        return createFileWithPath(url, false);
    }

    public static class DownloadedContent {
        public byte[] data;
        public String contentType;
    }

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
