package backend;

public class Main {
    private static final int PORT = 8080;
    private static final int THREAD_POOL_SIZE = 10;

    public static void main(String[] args) {
        printBanner();
        
        HTTPServer server = new HTTPServer(PORT, THREAD_POOL_SIZE);
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n🛑 Cerrando servidor...");
            server.stop();
        }));
        
        server.start();
    }

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