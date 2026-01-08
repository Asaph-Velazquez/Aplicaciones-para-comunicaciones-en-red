package backend;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class HTTPServer {
    private final int port;
    private final ExecutorService threadPool;
    private ServerSocket serverSocket;
    private volatile boolean running;

    public HTTPServer(int port, int threadPoolSize) {
        this.port = port;
        this.threadPool = Executors.newFixedThreadPool(threadPoolSize);
        this.running = false;
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            running = true;
            System.out.println("✅ Servidor HTTP iniciado en el puerto " + port);
            System.out.println("🌐 Accede a: http://localhost:" + port);
            System.out.println("📥 Descarga remota: http://localhost:" + port + "/download/net?url=http://example.com");
            System.out.println("📂 Descarga local: http://localhost:" + port + "/download/local?path=/docs");
            System.out.println();

            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    threadPool.execute(new ClientHandler(clientSocket));
                } catch (SocketException e) {
                    if (!running) {
                        break;
                    }
                    System.err.println("Error en el socket: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("❌ Error al iniciar el servidor: " + e.getMessage());
        }
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            threadPool.shutdown();
            if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
            }
            System.out.println("🛑 Servidor detenido");
        } catch (IOException | InterruptedException e) {
            System.err.println("Error al detener el servidor: " + e.getMessage());
        }
    }

    public boolean isRunning() {
        return running;
    }
}
