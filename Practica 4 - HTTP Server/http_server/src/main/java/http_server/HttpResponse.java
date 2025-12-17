package http_server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

// Modelo de respuesta HTTP
public class HttpResponse {
    private int statusCode;
    private String reasonPhrase;
    private Map<String, String> headers;
    private byte[] body;

    public HttpResponse() {
        this.headers = new HashMap<>();
        this.statusCode = 200;
        this.reasonPhrase = "OK";
    }

    public HttpResponse(int statusCode, String reasonPhrase) {
        this();
        this.statusCode = statusCode;
        this.reasonPhrase = reasonPhrase;
    }

    public void setStatus(int statusCode, String reasonPhrase) {
        this.statusCode = statusCode;
        this.reasonPhrase = reasonPhrase;
    }
    
    public int getStatusCode() {
        return statusCode;
    }
    
    public String getReasonPhrase() {
        return reasonPhrase;
    }

    public void addHeader(String key, String value) {
        this.headers.put(key, value);
    }

    public void setBody(byte[] body) {
        this.body = body;
        addHeader("Content-Length", String.valueOf(body.length));
    }

    public void setBody(String bodyText) {
        setBody(bodyText.getBytes());
    }

    /**
     * Construye la respuesta HTTP completa como array de bytes.
     * Formato: Status Line + Headers + CRLF + Body
     */
    public byte[] toBytes() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        
        // Status Line: HTTP/1.1 200 OK\r\n
        String statusLine = "HTTP/1.1 " + statusCode + " " + reasonPhrase + "\r\n";
        output.write(statusLine.getBytes());
        
        // Headers: cada header en formato "Key: Value\r\n"
        for (Map.Entry<String, String> header : headers.entrySet()) {
            String headerLine = header.getKey() + ": " + header.getValue() + "\r\n";
            output.write(headerLine.getBytes());
        }
        
        // Línea vacía que separa headers del body
        output.write("\r\n".getBytes());
        
        // Body (si existe)
        if (body != null && body.length > 0) {
            output.write(body);
        }
        
        return output.toByteArray();
    }

    /**
     * Métodos helper para respuestas comunes
     */
    public static HttpResponse ok(String contentType, byte[] body) {
        HttpResponse response = new HttpResponse(200, "OK");
        response.addHeader("Content-Type", contentType);
        response.setBody(body);
        return response;
    }

    public static HttpResponse notFound() {
        HttpResponse response = new HttpResponse(404, "Not Found");
        response.setBody("<html><body><h1>404 Not Found</h1></body></html>");
        response.addHeader("Content-Type", "text/html");
        return response;
    }

    public static HttpResponse methodNotAllowed() {
        HttpResponse response = new HttpResponse(405, "Method Not Allowed");
        response.setBody("<html><body><h1>405 Method Not Allowed</h1></body></html>");
        response.addHeader("Content-Type", "text/html");
        return response;
    }

    public static HttpResponse redirect(String location, int code) {
        String phrase = (code == 307) ? "Temporary Redirect" : "Found";
        HttpResponse response = new HttpResponse(code, phrase);
        response.addHeader("Location", location);
        response.setBody("<html><body><h1>Redirecting...</h1></body></html>");
        response.addHeader("Content-Type", "text/html");
        return response;
    }

    public static HttpResponse internalError() {
        HttpResponse response = new HttpResponse(500, "Internal Server Error");
        response.setBody("<html><body><h1>500 Internal Server Error</h1></body></html>");
        response.addHeader("Content-Type", "text/html");
        return response;
    }
}
