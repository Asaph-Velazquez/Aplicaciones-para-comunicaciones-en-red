package http_server;

import java.util.HashMap;
import java.util.Map;

// Modelo de solicitud HTTP parseada
public class HttpRequest {
    private String method;           // GET, POST, PUT, DELETE, etc.
    private String resource;         // /index.html, /api/data, etc.
    private String version;          // HTTP/1.1
    private Map<String, String> headers;
    private byte[] body;

    public HttpRequest() {
        this.headers = new HashMap<>();
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void addHeader(String key, String value) {
        this.headers.put(key.toLowerCase(), value);
    }

    public String getHeader(String key) {
        return this.headers.get(key.toLowerCase());
    }

    public byte[] getBody() {
        return body;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    @Override
    public String toString() {
        return method + " " + resource + " " + version;
    }
}
