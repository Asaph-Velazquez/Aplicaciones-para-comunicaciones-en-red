package backend;

import java.io.*;
import java.util.*;

/**
 * Construye y envía respuestas HTTP de forma manual.
 * Maneja diferentes códigos de estado y tipos MIME.
 */
public class HTTPResponse {
    private int statusCode;
    private String statusMessage;
    private Map<String, String> headers;
    private byte[] body;

    /**
     * Constructor básico
     */
    public HTTPResponse() {
        this.headers = new HashMap<>();
        this.statusCode = 200;
        this.statusMessage = "OK";
    }

    /**
     * Constructor con código de estado
     */
    public HTTPResponse(int statusCode, String statusMessage) {
        this();
        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
    }

    /**
     * Establece el código de estado
     */
    public void setStatus(int code, String message) {
        this.statusCode = code;
        this.statusMessage = message;
    }

    /**
     * Añade un header a la respuesta
     */
    public void addHeader(String name, String value) {
        headers.put(name, value);
    }

    /**
     * Establece el cuerpo de la respuesta como texto
     */
    public void setBody(String text) {
        this.body = text.getBytes();
        addHeader("Content-Length", String.valueOf(body.length));
    }

    /**
     * Establece el cuerpo de la respuesta como bytes
     */
    public void setBody(byte[] data) {
        this.body = data;
        addHeader("Content-Length", String.valueOf(data.length));
    }

    /**
     * Determina el Content-Type basado en la extensión del archivo
     * Soporta múltiples tipos MIME
     */
    public static String getMimeType(String filename) {
        String extension = "";
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0) {
            extension = filename.substring(lastDot + 1).toLowerCase();
        }

        switch (extension) {
            // Texto
            case "html":
            case "htm":
                return "text/html; charset=UTF-8";
            case "css":
                return "text/css; charset=UTF-8";
            case "js":
                return "application/javascript; charset=UTF-8";
            case "json":
                return "application/json; charset=UTF-8";
            case "xml":
                return "application/xml; charset=UTF-8";
            case "txt":
                return "text/plain; charset=UTF-8";

            // Imágenes
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "svg":
                return "image/svg+xml";
            case "ico":
                return "image/x-icon";

            // Documentos
            case "pdf":
                return "application/pdf";
            case "zip":
                return "application/zip";
            case "tar":
                return "application/x-tar";
            case "gz":
                return "application/gzip";

            // Video/Audio
            case "mp4":
                return "video/mp4";
            case "mp3":
                return "audio/mpeg";
            case "wav":
                return "audio/wav";

            default:
                return "application/octet-stream";
        }
    }

    /**
     * Envía la respuesta HTTP al cliente a través del OutputStream
     */
    public void send(OutputStream output) throws IOException {
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(output), true);

        // 1. Línea de estado: HTTP/1.1 200 OK
        writer.print("HTTP/1.1 " + statusCode + " " + statusMessage + "\r\n");

        // 2. Headers
        // Headers por defecto
        if (!headers.containsKey("Content-Type")) {
            headers.put("Content-Type", "text/html; charset=UTF-8");
        }
        if (!headers.containsKey("Connection")) {
            headers.put("Connection", "close");
        }
        if (!headers.containsKey("Server")) {
            headers.put("Server", "JavaHTTPServer/1.0");
        }

        for (Map.Entry<String, String> header : headers.entrySet()) {
            writer.print(header.getKey() + ": " + header.getValue() + "\r\n");
        }

        // 3. Línea en blanco que separa headers del body
        writer.print("\r\n");
        writer.flush();

        // 4. Body (si existe)
        if (body != null && body.length > 0) {
            output.write(body);
            output.flush();
        }
    }

    /**
     * Crea una respuesta de error estándar
     */
    public static HTTPResponse errorResponse(int code, String message) {
        HTTPResponse response = new HTTPResponse(code, getStatusMessage(code));
        String html = "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head><title>" + code + " " + getStatusMessage(code) + "</title></head>\n" +
                "<body>\n" +
                "<h1>" + code + " " + getStatusMessage(code) + "</h1>\n" +
                "<p>" + message + "</p>\n" +
                "<hr>\n" +
                "<p><em>JavaHTTPServer/1.0</em></p>\n" +
                "</body>\n" +
                "</html>";
        response.setBody(html);
        response.addHeader("Content-Type", "text/html; charset=UTF-8");
        return response;
    }

    /**
     * Obtiene el mensaje de estado correspondiente al código
     */
    private static String getStatusMessage(int code) {
        switch (code) {
            case 200: return "OK";
            case 201: return "Created";
            case 204: return "No Content";
            case 301: return "Moved Permanently";
            case 302: return "Found";
            case 304: return "Not Modified";
            case 400: return "Bad Request";
            case 401: return "Unauthorized";
            case 403: return "Forbidden";
            case 404: return "Not Found";
            case 405: return "Method Not Allowed";
            case 500: return "Internal Server Error";
            case 501: return "Not Implemented";
            case 503: return "Service Unavailable";
            default: return "Unknown";
        }
    }
}
