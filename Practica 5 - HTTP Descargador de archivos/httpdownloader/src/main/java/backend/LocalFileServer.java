package backend;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

/**
 * Maneja la descarga de archivos desde un directorio local.
 * Permite descargar archivos individuales o directorios completos de forma recursiva.
 */
public class LocalFileServer {
    private final String baseDirectory;
    private final String outputDirectory;

    /**
     * Constructor
     * @param baseDirectory Directorio base desde donde servir archivos
     */
    public LocalFileServer(String baseDirectory) {
        this.baseDirectory = baseDirectory;
        this.outputDirectory = null;
    }

    /**
     * Constructor con directorio de salida
     * @param baseDirectory Directorio base desde donde servir archivos
     * @param outputDirectory Directorio donde copiar los archivos descargados
     */
    public LocalFileServer(String baseDirectory, String outputDirectory) {
        this.baseDirectory = baseDirectory;
        this.outputDirectory = outputDirectory;
    }

    /**
     * Obtiene un archivo o directorio local
     * @param requestedPath Ruta solicitada (relativa al directorio base)
     * @return Respuesta HTTP con el archivo o listado
     */
    public HTTPResponse handleRequest(String requestedPath) {
        try {
            // Sanitizar la ruta para evitar path traversal
            String safePath = sanitizePath(requestedPath);
            File file = new File(baseDirectory, safePath);

            // Verificar que el archivo está dentro del directorio base (seguridad)
            if (!isWithinBaseDirectory(file)) {
                return HTTPResponse.errorResponse(403, "Acceso denegado");
            }

            // Si no existe
            if (!file.exists()) {
                return HTTPResponse.errorResponse(404, "Archivo o directorio no encontrado: " + requestedPath);
            }

            // Si es un directorio
            if (file.isDirectory()) {
                return handleDirectoryRequest(file);
            }

            // Si es un archivo
            return handleFileRequest(file);

        } catch (Exception e) {
            return HTTPResponse.errorResponse(500, "Error interno: " + e.getMessage());
        }
    }

    /**
     * Maneja la descarga copiando archivos al directorio de salida
     * @param requestedPath Ruta solicitada (relativa al directorio base)
     * @return Resultado de la descarga con estadísticas
     */
    public DownloadResult handleDownload(String requestedPath) {
        if (outputDirectory == null) {
            return null;
        }

        DownloadResult result = new DownloadResult();
        result.startTime = System.currentTimeMillis();

        try {
            // Sanitizar la ruta para evitar path traversal
            String safePath = sanitizePath(requestedPath);
            File file = new File(baseDirectory, safePath);

            // LOG: Inicio de descarga local
            System.out.println("\n" + "=".repeat(80));
            System.out.println("📂 DESCARGA LOCAL DEL DIRECTORIO DE PUBLICACIÓN");
            System.out.println("=".repeat(80));
            System.out.println("📍 INFORMACIÓN DE LA PETICIÓN:");
            System.out.println("   Ruta solicitada: " + requestedPath);
            System.out.println("   Ruta sanitizada: " + safePath);
            System.out.println("   Directorio base: " + baseDirectory);
            System.out.println("   Ruta completa: " + file.getAbsolutePath());

            // Verificar que el archivo está dentro del directorio base (seguridad)
            if (!isWithinBaseDirectory(file)) {
                System.err.println("❌ ACCESO DENEGADO: Archivo fuera del directorio base");
                System.out.println("=".repeat(80) + "\n");
                return null;
            }

            // Si no existe
            if (!file.exists()) {
                System.err.println("❌ ERROR: Archivo o directorio no encontrado");
                System.out.println("=".repeat(80) + "\n");
                return null;
            }

            // Mostrar información del archivo/directorio
            System.out.println("\n📋 INFORMACIÓN DEL RECURSO:");
            System.out.println("   Tipo: " + (file.isDirectory() ? "Directorio" : "Archivo"));
            System.out.println("   Nombre: " + file.getName());
            if (file.isFile()) {
                System.out.println("   Tamaño: " + formatFileSize(file.length()));
            }
            System.out.println("   Destino: " + outputDirectory);

            System.out.println("\n📦 INICIANDO COPIA...");

            // Copiar archivo(s) al directorio de salida
            if (file.isDirectory()) {
                copyDirectory(file, new File(outputDirectory, file.getName()), result);
            } else {
                copyFile(file, new File(outputDirectory, file.getName()), result);
            }

            System.out.println("\n✅ DESCARGA LOCAL COMPLETADA:");
            System.out.println("   Archivos copiados: " + result.filesDownloaded);
            System.out.println("   Total de bytes: " + formatFileSize(result.bytesDownloaded));
            System.out.println("   Tiempo: " + result.getDurationMs() + " ms");
            System.out.println("=".repeat(80) + "\n");

        } catch (Exception e) {
            System.err.println("❌ Error en descarga local: " + e.getMessage());
            e.printStackTrace();
            System.out.println("=".repeat(80) + "\n");
        }

        result.endTime = System.currentTimeMillis();
        return result;
    }

    /**
     * Maneja la petición de un archivo individual
     */
    private HTTPResponse handleFileRequest(File file) throws IOException {
        // Leer el archivo
        byte[] fileContent = Files.readAllBytes(file.toPath());

        // Crear respuesta
        HTTPResponse response = new HTTPResponse(200, "OK");
        response.setBody(fileContent);
        response.addHeader("Content-Type", HTTPResponse.getMimeType(file.getName()));
        response.addHeader("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"");

        return response;
    }

    /**
     * Maneja la petición de un directorio
     * Crea un archivo ZIP con todo el contenido de forma recursiva
     */
    private HTTPResponse handleDirectoryRequest(File directory) throws IOException {
        // Crear un ZIP temporal con el contenido del directorio
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zipOut = new ZipOutputStream(baos);

        // Añadir archivos recursivamente al ZIP
        addDirectoryToZip(directory, directory.getName(), zipOut);

        zipOut.close();
        byte[] zipData = baos.toByteArray();

        // Crear respuesta
        HTTPResponse response = new HTTPResponse(200, "OK");
        response.setBody(zipData);
        response.addHeader("Content-Type", "application/zip");
        response.addHeader("Content-Disposition", "attachment; filename=\"" + directory.getName() + ".zip\"");

        return response;
    }

    /**
     * Añade un directorio y su contenido a un archivo ZIP de forma recursiva
     */
    private void addDirectoryToZip(File directory, String basePath, ZipOutputStream zipOut) throws IOException {
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            String entryName = basePath + "/" + file.getName();

            if (file.isDirectory()) {
                // Añadir directorio (entrada con /)
                zipOut.putNextEntry(new ZipEntry(entryName + "/"));
                zipOut.closeEntry();

                // Recursión para subdirectorios
                addDirectoryToZip(file, entryName, zipOut);
            } else {
                // Añadir archivo
                zipOut.putNextEntry(new ZipEntry(entryName));
                Files.copy(file.toPath(), zipOut);
                zipOut.closeEntry();
            }
        }
    }

    /**
     * Genera un listado HTML de un directorio
     * (alternativa a descargar todo como ZIP)
     */
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

    /**
     * Sanitiza una ruta para evitar path traversal attacks
     */
    private String sanitizePath(String path) {
        if (path == null || path.isEmpty() || path.equals("/")) {
            return "";
        }

        // Eliminar barras iniciales y finales
        path = path.replaceAll("^/+", "").replaceAll("/+$", "");

        // Eliminar .. y .
        path = path.replaceAll("\\.\\.", "").replaceAll("\\./", "");

        return path;
    }

    /**
     * Verifica que un archivo está dentro del directorio base (seguridad)
     */
    private boolean isWithinBaseDirectory(File file) {
        try {
            Path basePath = new File(baseDirectory).getCanonicalFile().toPath();
            Path filePath = file.getCanonicalFile().toPath();
            return filePath.startsWith(basePath);
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Formatea el tamaño de un archivo de forma legible
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    /**
     * Copia un archivo individual
     */
    private void copyFile(File source, File destination, DownloadResult result) throws IOException {
        Files.copy(source.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
        result.filesDownloaded++;
        result.bytesDownloaded += source.length();
        System.out.println("   ✅ " + source.getName() + " → " + formatFileSize(source.length()));
    }

    /**
     * Copia un directorio de forma recursiva
     */
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

    /**
     * Clase para almacenar resultado de la descarga local
     */
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
