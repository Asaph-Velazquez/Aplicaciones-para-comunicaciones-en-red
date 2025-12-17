package http_server;

// Clase principal - Configura e inicia el servidor HTTP
public class Main {
    
    // Configuración del servidor
    private static final int PRIMARY_PORT = 8080;
    private static final int SECONDARY_PORT = 8081;
    private static final int POOL_SIZE = 10;

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("  SERVIDOR HTTP SIMPLE - JAVA SE");
        System.out.println("========================================");
        System.out.println();
        System.out.println("Características:");
        System.out.println("  ✓ Protocolo HTTP/1.1 manual");
        System.out.println("  ✓ Métodos: GET, POST, PUT, DELETE, HEAD, TRACE");
        System.out.println("  ✓ Tipos MIME: html, txt, json, png, css, js, etc.");
        System.out.println("  ✓ Pool de hilos: " + POOL_SIZE + " threads");
        System.out.println("  ✓ Redirección automática al 50% de carga");
        System.out.println();
        System.out.println("Configuración:");
        System.out.println("  Puerto primario: " + PRIMARY_PORT);
        System.out.println("  Puerto secundario: " + SECONDARY_PORT);
        System.out.println();
        System.out.println("========================================");
        System.out.println();

        // Crear gestor de redirección
        RedirectionManager redirectionManager = new RedirectionManager(
            PRIMARY_PORT, 
            SECONDARY_PORT, 
            POOL_SIZE
        );

        // Crear servidor primario
        HttpServer primaryServer = new HttpServer(PRIMARY_PORT, POOL_SIZE);
        primaryServer.setRedirectionManager(redirectionManager);

        // Hook para shutdown graceful
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n\nDeteniendo servidores...");
            primaryServer.stop();
            redirectionManager.stopSecondaryServer();
            System.out.println("Servidores detenidos correctamente");
        }));

        // Iniciar servidor primario
        try {
            System.out.println("Accede a la interfaz web en:");
            System.out.println("  http://localhost:" + PRIMARY_PORT);
            System.out.println();
            System.out.println("Presiona Ctrl+C para detener el servidor");
            System.out.println();
            
            primaryServer.start();
        } catch (Exception e) {
            System.err.println("Error fatal: " + e.getMessage());
            e.printStackTrace();
        }
    }
}