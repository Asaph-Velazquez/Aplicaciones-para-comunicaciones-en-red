package http_server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

// Servidor HTTP con pool de hilos y monitoreo de conexiones
public class HttpServer {
    
    private final int port;
    private final int poolSize;
    private final ExecutorService threadPool;
    private final AtomicInteger activeConnections;
    private ServerSocket serverSocket;
    private volatile boolean running;
    private RedirectionManager redirectionManager;

    public HttpServer(int port, int poolSize) {
        this.port = port;
        this.poolSize = poolSize;
        this.threadPool = Executors.newFixedThreadPool(poolSize);
        this.activeConnections = new AtomicInteger(0);
        this.running = false;
    }

    public void setRedirectionManager(RedirectionManager manager) {
        this.redirectionManager = manager;
    }

    // Inicia el servidor y acepta conexiones
    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            running = true;
            System.out.println("Servidor HTTP iniciado en puerto " + port);
            System.out.println("Pool de hilos: " + poolSize + " threads");
            System.out.println("Sirviendo archivos desde: www/");
            System.out.println("----------------------------------------");

            // Loop principal: aceptar conexiones
            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    
                    // Verificar si debemos redirigir (sobrecarga)
                    if (shouldRedirect()) {
                        handleRedirect(clientSocket);
                    } else {
                        // Procesar normalmente en el pool
                        threadPool.execute(new ClientHandler(clientSocket, this));
                    }
                } catch (IOException e) {
                    if (running) {
                        System.err.println("Error aceptando conexión: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error iniciando servidor: " + e.getMessage());
        }
    }

    /**
     * Detiene el servidor
     */
    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            threadPool.shutdown();
            System.out.println("Servidor en puerto " + port + " detenido");
        } catch (IOException e) {
            System.err.println("Error cerrando servidor: " + e.getMessage());
        }
    }

    /**
     * Incrementa el contador de conexiones activas
     */
    public void incrementConnections() {
        int current = activeConnections.incrementAndGet();
        double loadPercentage = (current * 100.0) / poolSize;
        
        String bar = "█".repeat((int)(loadPercentage / 5));
        String empty = "░".repeat(20 - (int)(loadPercentage / 5));
        
        System.out.println("┌────────────────────────────────────────────────────────────┐");
        System.out.println("│ 🔗 CONEXIONES: " + current + "/" + poolSize + " | Carga: [" + bar + empty + "] " + String.format("%.1f%%", loadPercentage) + String.format("%" + (60 - 29 - String.valueOf(current).length() - String.valueOf(poolSize).length() - String.format("%.1f", loadPercentage).length()) + "s", "") + "│");
        System.out.println("└────────────────────────────────────────────────────────────┘");
        
        // Si superamos el 50%, notificar al gestor de redirección
        if (loadPercentage > 50 && redirectionManager != null) {
            System.out.println("⚠️  ALTA CARGA DETECTADA - Activando sistema de redirección...\n");
            redirectionManager.onHighLoad();
        }
    }

    /**
     * Decrementa el contador de conexiones activas
     */
    public void decrementConnections() {
        activeConnections.decrementAndGet();
    }

    /**
     * Determina si debemos redirigir la petición al servidor secundario
     */
    private boolean shouldRedirect() {
        if (redirectionManager == null) {
            return false;
        }
        
        double loadPercentage = (activeConnections.get() * 100.0) / poolSize;
        return loadPercentage > 50 && redirectionManager.isSecondaryServerRunning();
    }

    /**
     * Maneja la redirección enviando un 307 Temporary Redirect
     */
    private void handleRedirect(Socket clientSocket) {
        try (OutputStream output = clientSocket.getOutputStream()) {
            String location = "http://localhost:" + redirectionManager.getSecondaryPort() + "/";
            HttpResponse response = HttpResponse.redirect(location, 307);
            output.write(response.toBytes());
            output.flush();
            
            System.out.println("┌────────────────────────────────────────────────────────────┐");
            System.out.println("│ 🔄 REDIRECCIÓN: 307 Temporary Redirect → Puerto " + redirectionManager.getSecondaryPort() + String.format("%" + (60 - 49 - String.valueOf(redirectionManager.getSecondaryPort()).length()) + "s", "") + "│");
            System.out.println("└────────────────────────────────────────────────────────────┘\n");
        } catch (IOException e) {
            System.err.println("❌ Error redirigiendo: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                // Ignorar
            }
        }
    }

    /**
     * Handler que procesa cada conexión de cliente en un hilo del pool
     */
    private static class ClientHandler implements Runnable {
        private final Socket socket;
        private final HttpServer server;

        public ClientHandler(Socket socket, HttpServer server) {
            this.socket = socket;
            this.server = server;
        }

        @Override
        public void run() {
            server.incrementConnections();
            long startTime = System.currentTimeMillis();
            
            try (InputStream input = socket.getInputStream();
                 OutputStream output = socket.getOutputStream()) {
                
                // 1. Parsear la petición HTTP
                HttpRequest request = HttpRequestParser.parse(input);
                
                String clientAddr = socket.getInetAddress().getHostAddress();
                System.out.println("┌────────────────────────────────────────────────────────────┐");
                System.out.println("│ 📥 REQUEST: " + request.getMethod() + " " + request.getResource() + String.format("%" + (60 - 15 - request.getMethod().length() - request.getResource().length()) + "s", "") + "│");
                System.out.println("│ 💻 Cliente: " + clientAddr + String.format("%" + (60 - 13 - clientAddr.length()) + "s", "") + "│");
                
                // Mostrar Content-Length si existe
                String contentLength = request.getHeader("content-length");
                if (contentLength != null) {
                    System.out.println("│ 📎 Body: " + contentLength + " bytes" + String.format("%" + (60 - 14 - contentLength.length()) + "s", "") + "│");
                }
                
                // 2. Procesar la petición y generar respuesta
                HttpResponse response = RequestHandler.handleRequest(request);
                
                // Simular procesamiento (descomentar para pruebas de carga)
                // try { Thread.sleep(100); } catch (InterruptedException e) {}
                
                // 3. Enviar la respuesta
                output.write(response.toBytes());
                output.flush();
                
                long duration = System.currentTimeMillis() - startTime;
                System.out.println("│ 📤 RESPONSE: " + response.getStatusCode() + " " + response.getReasonPhrase() + String.format("%" + (60 - 17 - String.valueOf(response.getStatusCode()).length() - response.getReasonPhrase().length()) + "s", "") + "│");
                System.out.println("│ ⏱️  Tiempo: " + duration + " ms" + String.format("%" + (60 - 15 - String.valueOf(duration).length()) + "s", "") + "│");
                System.out.println("└────────────────────────────────────────────────────────────┘\n");
                
            } catch (IOException e) {
                System.err.println("❌ Error procesando petición: " + e.getMessage());
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    // Ignorar
                }
                server.decrementConnections();
            }
        }
    }

    public int getPort() {
        return port;
    }
}
