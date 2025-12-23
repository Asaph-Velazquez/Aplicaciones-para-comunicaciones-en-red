package backend;

import java.io.*;
import java.util.*;

/**
 * Representa una petición HTTP recibida por el servidor.
 * Parsea manualmente la línea de petición, headers y body según HTTP/1.1
 */
public class HTTPRequest {
    private String method;
    private String path;
    private String version;
    private Map<String, String> headers;
    private Map<String, String> queryParams;
    private String body;

    /**
     * Constructor que parsea una petición HTTP desde un InputStream
     * @param input Stream de entrada desde el socket del cliente
     * @throws IOException Si hay un error al leer la petición
     */
    public HTTPRequest(InputStream input) throws IOException {
        headers = new HashMap<>();
        queryParams = new HashMap<>();
        parseRequest(input);
    }

    /**
     * Parsea la petición HTTP completa
     */
    private void parseRequest(InputStream input) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));

        // 1. Parsear la línea de petición (Request Line)
        String requestLine = reader.readLine();
        if (requestLine == null || requestLine.isEmpty()) {
            throw new IOException("Request line vacía");
        }

        parseRequestLine(requestLine);

        // 2. Parsear los headers
        String line;
        while ((line = reader.readLine()) != null && !line.isEmpty()) {
            int separatorIndex = line.indexOf(':');
            if (separatorIndex > 0) {
                String headerName = line.substring(0, separatorIndex).trim();
                String headerValue = line.substring(separatorIndex + 1).trim();
                headers.put(headerName.toLowerCase(), headerValue);
            }
        }

        // 3. Parsear el body (si existe y hay Content-Length)
        if (headers.containsKey("content-length")) {
            int contentLength = Integer.parseInt(headers.get("content-length"));
            if (contentLength > 0) {
                char[] bodyChars = new char[contentLength];
                reader.read(bodyChars, 0, contentLength);
                body = new String(bodyChars);
            }
        }
    }

    /**
     * Parsea la línea de petición: GET /path?query HTTP/1.1
     */
    private void parseRequestLine(String requestLine) {
        String[] parts = requestLine.split(" ");
        if (parts.length < 3) {
            throw new IllegalArgumentException("Request line inválida: " + requestLine);
        }

        method = parts[0];
        String fullPath = parts[1];
        version = parts[2];

        // Separar path y query string
        int queryIndex = fullPath.indexOf('?');
        if (queryIndex != -1) {
            path = fullPath.substring(0, queryIndex);
            String queryString = fullPath.substring(queryIndex + 1);
            parseQueryString(queryString);
        } else {
            path = fullPath;
        }
    }

    /**
     * Parsea los parámetros de query string: param1=value1&param2=value2
     */
    private void parseQueryString(String queryString) {
        String[] params = queryString.split("&");
        for (String param : params) {
            int equalsIndex = param.indexOf('=');
            if (equalsIndex > 0) {
                String key = param.substring(0, equalsIndex);
                String value = param.substring(equalsIndex + 1);
                try {
                    // Decodificar URL encoding
                    key = java.net.URLDecoder.decode(key, "UTF-8");
                    value = java.net.URLDecoder.decode(value, "UTF-8");
                    queryParams.put(key, value);
                } catch (UnsupportedEncodingException e) {
                    // UTF-8 siempre está soportado
                }
            }
        }
    }

    // Getters
    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public String getVersion() {
        return version;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getHeader(String name) {
        return headers.get(name.toLowerCase());
    }

    public Map<String, String> getQueryParams() {
        return queryParams;
    }

    public String getQueryParam(String name) {
        return queryParams.get(name);
    }

    public String getBody() {
        return body;
    }

    @Override
    public String toString() {
        return String.format("%s %s %s", method, path, version);
    }
}
