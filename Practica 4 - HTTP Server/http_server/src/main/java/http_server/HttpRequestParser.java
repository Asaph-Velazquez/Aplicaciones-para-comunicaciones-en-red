package http_server;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

// Parser manual de solicitudes HTTP/1.1
public class HttpRequestParser {

    // Parsea una solicitud HTTP completa (request line, headers, body)
    public static HttpRequest parse(InputStream input) throws IOException {
        HttpRequest request = new HttpRequest();

        // 1. Parsear Request Line: GET /index.html HTTP/1.1
        String requestLine = readLine(input);
        if (requestLine == null || requestLine.isEmpty()) {
            throw new IOException("Empty request line");
        }

        parseRequestLine(request, requestLine);

        // 2. Parsear Headers: cada línea hasta encontrar línea vacía
        String headerLine;
        while (!(headerLine = readLine(input)).isEmpty()) {
            parseHeader(request, headerLine);
        }

        // 3. Parsear Body si existe (según Content-Length)
        String contentLengthStr = request.getHeader("content-length");
        if (contentLengthStr != null) {
            try {
                int contentLength = Integer.parseInt(contentLengthStr);
                if (contentLength > 0) {
                    byte[] body = readBody(input, contentLength);
                    request.setBody(body);
                }
            } catch (NumberFormatException e) {
                // Content-Length inválido, ignorar body
            }
        }

        return request;
    }
    
    // Lee una línea del InputStream byte por byte (termina en \r\n)
    private static String readLine(InputStream input) throws IOException {
        ByteArrayOutputStream lineBuffer = new ByteArrayOutputStream();
        int current;
        int previous = -1;
        
        while ((current = input.read()) != -1) {
            if (previous == '\r' && current == '\n') {
                // Encontramos \r\n, retornar sin incluir \r\n
                byte[] bytes = lineBuffer.toByteArray();
                if (bytes.length > 0) {
                    // Quitar el \r que ya agregamos
                    return new String(bytes, 0, bytes.length - 1);
                }
                return "";
            }
            lineBuffer.write(current);
            previous = current;
        }
        
        // Si llegamos aquí, el stream terminó
        byte[] bytes = lineBuffer.toByteArray();
        return bytes.length > 0 ? new String(bytes) : null;
    }

    /**
     * Parsea la línea de request: METHOD RESOURCE VERSION
     * Ejemplo: GET /index.html HTTP/1.1
     */
    private static void parseRequestLine(HttpRequest request, String line) {
        String[] parts = line.split(" ");
        if (parts.length >= 3) {
            request.setMethod(parts[0].toUpperCase());
            request.setResource(parts[1]);
            request.setVersion(parts[2]);
        } else {
            // Petición malformada, usar valores por defecto
            request.setMethod("GET");
            request.setResource("/");
            request.setVersion("HTTP/1.1");
        }
    }

    /**
     * Parsea una línea de header: Key: Value
     * Ejemplo: Content-Type: application/json
     */
    private static void parseHeader(HttpRequest request, String line) {
        int colonIndex = line.indexOf(':');
        if (colonIndex > 0) {
            String key = line.substring(0, colonIndex).trim();
            String value = line.substring(colonIndex + 1).trim();
            request.addHeader(key, value);
        }
    }

    /**
     * Lee el body de la petición usando Content-Length.
     * Lee exactamente contentLength bytes del stream.
     */
    private static byte[] readBody(InputStream input, int contentLength) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int bytesRead = 0;
        byte[] chunk = new byte[1024];
        
        while (bytesRead < contentLength) {
            int toRead = Math.min(chunk.length, contentLength - bytesRead);
            int read = input.read(chunk, 0, toRead);
            
            if (read == -1) {
                break; // EOF alcanzado
            }
            
            buffer.write(chunk, 0, read);
            bytesRead += read;
        }
        
        return buffer.toByteArray();
    }
}
