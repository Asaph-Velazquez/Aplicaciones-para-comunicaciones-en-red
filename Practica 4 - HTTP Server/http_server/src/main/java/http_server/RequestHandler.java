package http_server;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

// Manejador de peticiones HTTP (GET, POST, PUT, DELETE, HEAD, TRACE)
public class RequestHandler {
    
    private static final String WWW_ROOT = "www";
    
    // Mapa de tipos MIME soportados
    private static final Map<String, String> MIME_TYPES = new HashMap<>();
    static {
        MIME_TYPES.put(".html", "text/html");
        MIME_TYPES.put(".htm", "text/html");
        MIME_TYPES.put(".txt", "text/plain");
        MIME_TYPES.put(".json", "application/json");
        MIME_TYPES.put(".png", "image/png");
        MIME_TYPES.put(".jpg", "image/jpeg");
        MIME_TYPES.put(".jpeg", "image/jpeg");
        MIME_TYPES.put(".gif", "image/gif");
        MIME_TYPES.put(".css", "text/css");
        MIME_TYPES.put(".js", "application/javascript");
        MIME_TYPES.put(".pdf", "application/pdf");
        MIME_TYPES.put(".xml", "application/xml");
    }

    // Procesa una request HTTP y genera la respuesta apropiada
    public static HttpResponse handleRequest(HttpRequest request) {
        try {
            String method = request.getMethod();
            
            switch (method) {
                case "GET":
                    return handleGet(request);
                case "POST":
                    return handlePost(request);
                case "PUT":
                    return handlePut(request);
                case "DELETE":
                    return handleDelete(request);
                case "HEAD":
                    return handleHead(request);
                case "TRACE":
                    return handleTrace(request);
                default:
                    return HttpResponse.methodNotAllowed();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return HttpResponse.internalError();
        }
    }

    // GET: Sirve archivos estáticos desde www/ o lista archivos de /api/files
    private static HttpResponse handleGet(HttpRequest request) {
        String resource = request.getResource();
        
        if (resource.equals("/api/files")) {
            return listUploadsFiles();
        }
        
        if (resource.equals("/")) {
            resource = "/index.html";
        }

        Path filePath = Paths.get(WWW_ROOT + resource).normalize();
        File file = filePath.toFile();

        if (!file.exists() || !file.isFile()) {
            return HttpResponse.notFound();
        }

        // Verificar seguridad: que no se salga del directorio www
        try {
            String canonicalWww = new File(WWW_ROOT).getCanonicalPath();
            String canonicalFile = file.getCanonicalPath();
            if (!canonicalFile.startsWith(canonicalWww)) {
                return HttpResponse.notFound();
            }
        } catch (IOException e) {
            return HttpResponse.internalError();
        }

        // Leer el archivo
        byte[] fileContent = readFile(file);
        if (fileContent == null) {
            return HttpResponse.internalError();
        }

        // Determinar tipo MIME
        String mimeType = getMimeType(file.getName());

        // Crear respuesta
        HttpResponse response = HttpResponse.ok(mimeType, fileContent);
        response.addHeader("Server", "SimpleHttpServer/1.0");
        return response;
    }

    // POST: Crea un nuevo archivo en uploads/ (409 Conflict si existe)
    private static HttpResponse handlePost(HttpRequest request) {
        byte[] body = request.getBody();
        String resource = request.getResource();
        
        if (body == null || body.length == 0) {
            HttpResponse response = new HttpResponse(400, "Bad Request");
            response.addHeader("Content-Type", "text/plain");
            response.setBody("POST requiere un body");
            response.addHeader("Server", "SimpleHttpServer/1.0");
            return response;
        }
        
        try {
            // Guardar en uploads/ si no especifica ruta
            String filePath = resource.startsWith("/uploads/") ? resource : "/uploads" + resource;
            Path targetPath = Paths.get(WWW_ROOT + filePath).normalize();
            File targetFile = targetPath.toFile();
            
            // Verificar seguridad
            String canonicalWww = new File(WWW_ROOT).getCanonicalPath();
            String canonicalFile = targetFile.getCanonicalPath();
            if (!canonicalFile.startsWith(canonicalWww)) {
                return HttpResponse.notFound();
            }
            
            // Crear directorio si no existe
            targetFile.getParentFile().mkdirs();
            
            // POST: Solo crear si NO existe (409 Conflict si existe)
            if (targetFile.exists()) {
                String responseBody = "✗ Conflicto: El archivo ya existe: " + filePath + "\nUsa PUT para actualizar archivos existentes.";
                HttpResponse response = new HttpResponse(409, "Conflict");
                response.addHeader("Content-Type", "text/plain; charset=utf-8");
                response.setBody(responseBody);
                response.addHeader("Server", "SimpleHttpServer/1.0");
                return response;
            }
            
            // Guardar archivo
            Files.write(targetPath, body);
            
            String responseBody = "✓ Archivo creado: " + filePath + "\nTamaño: " + body.length + " bytes\n\nContenido:\n" + new String(body);
            
            HttpResponse response = new HttpResponse(201, "Created");
            response.addHeader("Content-Type", "text/plain; charset=utf-8");
            response.setBody(responseBody);
            response.addHeader("Server", "SimpleHttpServer/1.0");
            response.addHeader("Location", filePath);
            return response;
            
        } catch (IOException e) {
            e.printStackTrace();
            return HttpResponse.internalError();
        }
    }

    // PUT: Actualiza o crea un archivo en uploads/
    private static HttpResponse handlePut(HttpRequest request) {
        byte[] body = request.getBody();
        String resource = request.getResource();
        
        if (body == null || body.length == 0) {
            HttpResponse response = new HttpResponse(400, "Bad Request");
            response.addHeader("Content-Type", "text/plain");
            response.setBody("PUT requiere un body");
            response.addHeader("Server", "SimpleHttpServer/1.0");
            return response;
        }
        
        try {
            // Guardar en uploads/ si no especifica ruta
            String filePath = resource.startsWith("/uploads/") ? resource : "/uploads" + resource;
            Path targetPath = Paths.get(WWW_ROOT + filePath).normalize();
            File targetFile = targetPath.toFile();
            
            // Verificar seguridad
            String canonicalWww = new File(WWW_ROOT).getCanonicalPath();
            String canonicalFile = targetFile.getCanonicalPath();
            if (!canonicalFile.startsWith(canonicalWww)) {
                return HttpResponse.notFound();
            }
            
            // Crear directorio si no existe
            targetFile.getParentFile().mkdirs();
            
            boolean existed = targetFile.exists();
            
            // Guardar archivo
            Files.write(targetPath, body);
            
            String action = existed ? "actualizado" : "creado";
            String responseBody = "✓ Archivo " + action + ": " + filePath + "\nTamaño: " + body.length + " bytes\n\nContenido:\n" + new String(body);
            
            HttpResponse response = new HttpResponse(200, "OK");
            response.addHeader("Content-Type", "text/plain; charset=utf-8");
            response.setBody(responseBody);
            response.addHeader("Server", "SimpleHttpServer/1.0");
            return response;
            
        } catch (IOException e) {
            e.printStackTrace();
            return HttpResponse.internalError();
        }
    }

    // DELETE: Elimina un archivo de uploads/
    private static HttpResponse handleDelete(HttpRequest request) {
        String resource = request.getResource();
        
        try {
            // Solo permitir borrar de uploads/
            String filePath = resource.startsWith("/uploads/") ? resource : "/uploads" + resource;
            Path targetPath = Paths.get(WWW_ROOT + filePath).normalize();
            File targetFile = targetPath.toFile();
            
            // Verificar seguridad
            String canonicalWww = new File(WWW_ROOT).getCanonicalPath();
            String canonicalFile = targetFile.getCanonicalPath();
            if (!canonicalFile.startsWith(canonicalWww)) {
                return HttpResponse.notFound();
            }
            
            if (!targetFile.exists() || !targetFile.isFile()) {
                HttpResponse response = new HttpResponse(404, "Not Found");
                response.addHeader("Content-Type", "text/plain");
                response.setBody("✗ Archivo no encontrado: " + filePath);
                response.addHeader("Server", "SimpleHttpServer/1.0");
                return response;
            }
            
            // Leer nombre antes de borrar
            String fileName = targetFile.getName();
            long fileSize = targetFile.length();
            
            // Eliminar archivo
            boolean deleted = targetFile.delete();
            
            if (deleted) {
                String responseBody = "✓ Archivo eliminado: " + filePath + "\nNombre: " + fileName + "\nTamaño: " + fileSize + " bytes";
                HttpResponse response = new HttpResponse(200, "OK");
                response.addHeader("Content-Type", "text/plain; charset=utf-8");
                response.setBody(responseBody);
                response.addHeader("Server", "SimpleHttpServer/1.0");
                return response;
            } else {
                return HttpResponse.internalError();
            }
            
        } catch (IOException e) {
            e.printStackTrace();
            return HttpResponse.internalError();
        }
    }

    // HEAD: Igual que GET pero sin body
    private static HttpResponse handleHead(HttpRequest request) {
        HttpResponse response = handleGet(request);
        // HEAD retorna los mismos headers que GET pero sin body
        response.setBody(new byte[0]);
        return response;
    }

    // TRACE: Devuelve la request recibida (diagnóstico)
    private static HttpResponse handleTrace(HttpRequest request) {
        StringBuilder trace = new StringBuilder();
        trace.append(request.toString()).append("\r\n");
        
        for (Map.Entry<String, String> header : request.getHeaders().entrySet()) {
            trace.append(header.getKey()).append(": ").append(header.getValue()).append("\r\n");
        }
        
        HttpResponse response = new HttpResponse(200, "OK");
        response.addHeader("Content-Type", "message/http");
        response.setBody(trace.toString());
        response.addHeader("Server", "SimpleHttpServer/1.0");
        return response;
    }

    // Lee un archivo completo y retorna su contenido como bytes
    private static byte[] readFile(File file) {
        try {
            return Files.readAllBytes(file.toPath());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Determina el tipo MIME basado en la extensión del archivo
    private static String getMimeType(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex >= 0) {
            String extension = fileName.substring(dotIndex).toLowerCase();
            return MIME_TYPES.getOrDefault(extension, "application/octet-stream");
        }
        return "application/octet-stream";
    }
    
    // Lista todos los archivos en el directorio uploads/
    private static HttpResponse listUploadsFiles() {
        File uploadsDir = new File(WWW_ROOT + "/uploads");
        
        if (!uploadsDir.exists() || !uploadsDir.isDirectory()) {
            return HttpResponse.ok("application/json", "[]".getBytes());
        }
        
        StringBuilder json = new StringBuilder("[");
        File[] files = uploadsDir.listFiles();
        
        if (files != null) {
            boolean first = true;
            for (File file : files) {
                if (file.isFile()) {
                    if (!first) {
                        json.append(",");
                    }
                    first = false;
                    
                    String fileName = file.getName();
                    String mimeType = getMimeType(fileName);
                    String icon = getFileIcon(mimeType);
                    
                    json.append("{");
                    json.append("\"name\":\"").append(fileName).append("\",");
                    json.append("\"mimeType\":\"").append(mimeType).append("\",");
                    json.append("\"icon\":\"").append(icon).append("\",");
                    json.append("\"size\":").append(file.length());
                    json.append("}");
                }
            }
        }
        
        json.append("]");
        
        HttpResponse response = HttpResponse.ok("application/json", json.toString().getBytes());
        response.addHeader("Server", "SimpleHttpServer/1.0");
        response.addHeader("Cache-Control", "no-cache");
        return response;
    }
    
    // Determina el icono apropiado según el tipo MIME
    private static String getFileIcon(String mimeType) {
        if (mimeType.startsWith("text/html")) return "📄";
        if (mimeType.startsWith("text/plain")) return "📝";
        if (mimeType.startsWith("application/json")) return "📋";
        if (mimeType.startsWith("image/")) return "🖼️";
        if (mimeType.startsWith("application/pdf")) return "📕";
        if (mimeType.startsWith("text/css")) return "🎨";
        if (mimeType.startsWith("application/javascript")) return "⚙️";
        return "📄";
    }
}
