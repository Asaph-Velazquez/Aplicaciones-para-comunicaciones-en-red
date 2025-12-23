package backend;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

/**
 * Servidor HTTP básico que utiliza sockets y hilos para manejar conexiones concurrentes.
 * Implementa HTTP/1.1 de forma manual sin frameworks externos.
 */
public class HTTPServer {
    private final int port;
    private final ExecutorService threadPool;
    private ServerSocket serverSocket;
    private volatile boolean running;

    /**
     * Constructor del servidor HTTP
     * @param port Puerto en el que escuchará el servidor
     * @param threadPoolSize Tamaño del pool de hilos para manejar conexiones concurrentes
     */
    public HTTPServer(int port, int threadPoolSize) {
        this.port = port;
        this.threadPool = Executors.newFixedThreadPool(threadPoolSize);
        this.running = false;
    }

    /**
     * Inicia el servidor y comienza a aceptar conexiones
     */
    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            running = true;
            System.out.println("✅ Servidor HTTP iniciado en el puerto " + port);
            System.out.println("🌐 Accede a: http://localhost:" + port);
            System.out.println("📥 Descarga remota: http://localhost:" + port + "/download/net?url=http://example.com");
            System.out.println("📂 Descarga local: http://localhost:" + port + "/download/local?path=/docs");
            System.out.println();

            // Bucle principal que acepta conexiones
            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    // Delegar el manejo de la conexión a un hilo del pool
                    threadPool.execute(new ClientHandler(clientSocket));
                } catch (SocketException e) {
                    if (!running) {
                        // El servidor se está cerrando, es normal
                        break;
                    }
                    System.err.println("Error en el socket: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("❌ Error al iniciar el servidor: " + e.getMessage());
        }
    }

    /**
     * Detiene el servidor y libera recursos
     */
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

    /**
     * Verifica si el servidor está en ejecución
     */
    public boolean isRunning() {
        return running;
    }
}
