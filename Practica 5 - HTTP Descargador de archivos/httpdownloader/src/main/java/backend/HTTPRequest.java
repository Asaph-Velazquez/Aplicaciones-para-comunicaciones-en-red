package backend;

import java.io.*;
import java.util.*;

public class HTTPRequest {
    private String method;
    private String path;
    private String version;
    private Map<String, String> headers;
    private Map<String, String> queryParams;
    private String body;

    public HTTPRequest(InputStream input) throws IOException {
        headers = new HashMap<>();
        queryParams = new HashMap<>();
        parseRequest(input);
    }

    private void parseRequest(InputStream input) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));

        String requestLine = reader.readLine();
        if (requestLine == null || requestLine.isEmpty()) {
            throw new IOException("Request line vacía");
        }

        parseRequestLine(requestLine);

        String line;
        while ((line = reader.readLine()) != null && !line.isEmpty()) {
            int separatorIndex = line.indexOf(':');
            if (separatorIndex > 0) {
                String headerName = line.substring(0, separatorIndex).trim();
                String headerValue = line.substring(separatorIndex + 1).trim();
                headers.put(headerName.toLowerCase(), headerValue);
            }
        }

        if (headers.containsKey("content-length")) {
            int contentLength = Integer.parseInt(headers.get("content-length"));
            if (contentLength > 0) {
                char[] bodyChars = new char[contentLength];
                reader.read(bodyChars, 0, contentLength);
                body = new String(bodyChars);
            }
        }
    }

    private void parseRequestLine(String requestLine) {
        String[] parts = requestLine.split(" ");
        if (parts.length < 3) {
            throw new IllegalArgumentException("Request line inválida: " + requestLine);
        }

        method = parts[0];
        String fullPath = parts[1];
        version = parts[2];

        int queryIndex = fullPath.indexOf('?');
        if (queryIndex != -1) {
            path = fullPath.substring(0, queryIndex);
            String queryString = fullPath.substring(queryIndex + 1);
            parseQueryString(queryString);
        } else {
            path = fullPath;
        }
    }

    private void parseQueryString(String queryString) {
        String[] params = queryString.split("&");
        for (String param : params) {
            int equalsIndex = param.indexOf('=');
            if (equalsIndex > 0) {
                String key = param.substring(0, equalsIndex);
                String value = param.substring(equalsIndex + 1);
                try {
                    key = java.net.URLDecoder.decode(key, "UTF-8");
                    value = java.net.URLDecoder.decode(value, "UTF-8");
                    queryParams.put(key, value);
                } catch (UnsupportedEncodingException e) {
                }
            }
        }
    }

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
