package http_server;

// Gestor de redirección - activa servidor secundario cuando carga >50%
public class RedirectionManager {
    
    private final int primaryPort;
    private final int secondaryPort;
    private final int poolSize;
    private HttpServer secondaryServer;
    private Thread secondaryServerThread;
    private volatile boolean secondaryRunning;

    public RedirectionManager(int primaryPort, int secondaryPort, int poolSize) {
        this.primaryPort = primaryPort;
        this.secondaryPort = secondaryPort;
        this.poolSize = poolSize;
        this.secondaryRunning = false;
    }

    /**
     * Callback invocado cuando el servidor primario detecta alta carga
     */
    public synchronized void onHighLoad() {
        if (!secondaryRunning) {
            startSecondaryServer();
        }
    }

    /**
     * Inicia el servidor secundario en un hilo separado
     */
    private void startSecondaryServer() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("⚠️  ACTIVANDO SERVIDOR SECUNDARIO");
        System.out.println("=".repeat(60));
        System.out.println("🔴 Motivo: Carga del servidor primario >50%");
        System.out.println("🔵 Puerto secundario: " + secondaryPort);
        System.out.println("🟢 Pool de hilos: " + poolSize);
        System.out.println("=".repeat(60) + "\n");

        secondaryServer = new HttpServer(secondaryPort, poolSize);
        secondaryServerThread = new Thread(() -> {
            secondaryServer.start();
        }, "SecondaryServerThread");
        
        secondaryServerThread.start();
        secondaryRunning = true;

        System.out.println("✅ Servidor secundario ACTIVO");
        System.out.println("➡️  Nuevas peticiones serán redirigidas (HTTP 307)\n");
    }

    /**
     * Detiene el servidor secundario
     */
    public synchronized void stopSecondaryServer() {
        if (secondaryRunning && secondaryServer != null) {
            secondaryServer.stop();
            secondaryRunning = false;
            System.out.println("Servidor secundario detenido");
        }
    }

    /**
     * Indica si el servidor secundario está corriendo
     */
    public boolean isSecondaryServerRunning() {
        return secondaryRunning;
    }

    /**
     * Obtiene el puerto del servidor secundario
     */
    public int getSecondaryPort() {
        return secondaryPort;
    }

    /**
     * Obtiene el puerto del servidor primario
     */
    public int getPrimaryPort() {
        return primaryPort;
    }
}
