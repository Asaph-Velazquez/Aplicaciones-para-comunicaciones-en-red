package backend;

/**
 * Clase principal que inicia el servidor HTTP.
 * 
 * Este proyecto implementa un servidor HTTP básico usando exclusivamente:
 * - Java SE puro (sin frameworks)
 * - Sockets (ServerSocket y Socket)
 * - Hilos (ExecutorService, Thread, Runnable)
 * - Parsing manual de HTTP/1.1
 * 
 * Funcionalidades:
 * 1. Descarga recursiva desde servidores remotos (comportamiento tipo wget -r)
 * 2. Descarga de archivos desde directorio local
 * 3. Soporte para múltiples tipos MIME (HTML, CSS, JS, PDF, imágenes, etc.)
 * 4. Interfaz web para interactuar con el servidor
 * 5. Manejo concurrente de múltiples clientes
 * 
 * Endpoints disponibles:
 * - GET / - Interfaz web principal
 * - GET /download/net?url=<URL> - Descarga recursiva desde servidor remoto
 * - GET /download/local?path=<PATH> - Descarga desde directorio local
 * 
 * @author Proyecto Académico - Práctica 5
 * @version 1.0
 */
public class Main {
    // Puerto en el que escuchará el servidor
    private static final int PORT = 8080;
    
    // Tamaño del pool de hilos para manejar conexiones concurrentes
    private static final int THREAD_POOL_SIZE = 10;

    public static void main(String[] args) {
        printBanner();
        
        // Crear e iniciar el servidor HTTP
        HTTPServer server = new HTTPServer(PORT, THREAD_POOL_SIZE);
        
        // Añadir hook para cerrar el servidor correctamente al terminar la aplicación
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n🛑 Cerrando servidor...");
            server.stop();
        }));
        
        // Iniciar el servidor (bloqueante)
        server.start();
    }

    /**
     * Imprime un banner informativo al iniciar el servidor
     */
    private static void printBanner() {
        System.out.println("╔═══════════════════════════════════════════════════════════╗");
        System.out.println("║                                                           ║");
        System.out.println("║     🌐  SERVIDOR HTTP - JAVA SE PURO                     ║");
        System.out.println("║                                                           ║");
        System.out.println("║     📚 Práctica 5: Descargador de Archivos               ║");
        System.out.println("║                                                           ║");
        System.out.println("║     ✅ Características:                                   ║");
        System.out.println("║        • Sockets puros (ServerSocket/Socket)             ║");
        System.out.println("║        • Hilos para concurrencia (ExecutorService)       ║");
        System.out.println("║        • HTTP/1.1 manual (sin frameworks)                ║");
        System.out.println("║        • Descarga recursiva tipo wget -r                 ║");
        System.out.println("║        • Servir archivos locales                         ║");
        System.out.println("║        • Soporte múltiples tipos MIME                    ║");
        System.out.println("║                                                           ║");
        System.out.println("╚═══════════════════════════════════════════════════════════╝");
        System.out.println();
    }
}