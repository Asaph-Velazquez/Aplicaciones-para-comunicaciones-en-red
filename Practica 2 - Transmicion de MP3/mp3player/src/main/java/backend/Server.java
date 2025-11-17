package backend;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;

public class Server {
    private static final int WINDOW_SIZE = 10; // Tamaño de la ventana Go-Back-N
    private static final int TIMEOUT = 500; // Timeout en ms                    
    private static final int HEADER_SIZE = 8; // 4 bytes seq + 4 bytes total
    
    public static void main(String[] args) {
        try {
            // Instanciamos Mp3Player
            Mp3Player player = new Mp3Player();

            // Leemos el archivo MP3
            var resource = Mp3Player.class.getClassLoader().getResource("song/cancion.mp3");
            if (resource == null) {
                System.err.println("No se encontró cancion.mp3 en resources/song");
                return;
            }
            String path = new File(resource.toURI()).getAbsolutePath();
            byte[] mp3Bytes = player.leerArchivoMP3(path);

            if (mp3Bytes == null) return;

            // Fragmentamos el MP3 en paquetes de 6500 bytes (dejando espacio para header)
            byte[][] paquetes = player.fragmentarMP3(mp3Bytes, 6500 - HEADER_SIZE);
            System.out.println("MP3 fragmentado en " + paquetes.length + " paquetes");
            System.out.println("Usando Go-Back-N con ventana de tamaño: " + WINDOW_SIZE);

            // Iniciamos el servidor UDP
            DatagramSocket serverSocket = null;
            try {
                serverSocket = new DatagramSocket(1234);
                serverSocket.setReuseAddress(true);
                serverSocket.setSoTimeout(TIMEOUT);
                System.out.println("Servidor iniciado en el puerto: " + serverSocket.getLocalPort());
                System.out.println("Esperando mensaje READY del cliente...");

                // Esperar mensaje READY del cliente
                byte[] readyBuffer = new byte[256];
                DatagramPacket readyPacket = new DatagramPacket(readyBuffer, readyBuffer.length);
                serverSocket.setSoTimeout(10000); // 10 segundos para READY
                serverSocket.receive(readyPacket);
                
                String message = new String(readyPacket.getData(), 0, readyPacket.getLength()).trim();
                if (!message.equals("READY")) {
                    System.err.println("Mensaje inesperado del cliente: " + message);
                    return;
                }
                
                InetAddress clientAddress = readyPacket.getAddress();
                int clientPort = readyPacket.getPort();
                System.out.println("Cliente listo en " + clientAddress + ":" + clientPort);
                
                // Implementación de Go-Back-N
                goBackN(serverSocket, paquetes, clientAddress, clientPort);
                
                System.out.println("✓ Transmisión completada exitosamente");
                
            } finally {
                if (serverSocket != null && !serverSocket.isClosed()) {
                    serverSocket.close();
                    System.out.println("Servidor cerrado correctamente");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static void goBackN(DatagramSocket socket, byte[][] paquetes, 
                                InetAddress clientAddress, int clientPort) throws IOException {
        int base = 0; // Base de la ventana
        int nextSeqNum = 0; // Siguiente número de secuencia a enviar
        int totalPackets = paquetes.length;

        socket.setSoTimeout(TIMEOUT);
        
        System.out.println("\n=== Iniciando transmisión Go-Back-N ===");
        System.out.println("Total de paquetes: " + totalPackets);
        System.out.println("Ventana: " + WINDOW_SIZE + " | Timeout: " + TIMEOUT + "ms\n");
        
        long startTime = System.currentTimeMillis();
        int totalSent = 0;
        int retransmissions = 0;
        
        while (base < totalPackets) {
            // Enviar paquetes dentro de la ventana
            while (nextSeqNum < base + WINDOW_SIZE && nextSeqNum < totalPackets) {
                sendPacket(socket, paquetes[nextSeqNum], nextSeqNum, totalPackets, 
                          clientAddress, clientPort);
                totalSent++;
                nextSeqNum++;
            }
            
            // Esperar ACK
            try {
                byte[] ackBuffer = new byte[256];
                DatagramPacket ackPacket = new DatagramPacket(ackBuffer, ackBuffer.length);
                socket.receive(ackPacket);
                
                String ackMsg = new String(ackPacket.getData(), 0, ackPacket.getLength()).trim();
                
                if (ackMsg.startsWith("ACK:")) {
                    int ackNum = Integer.parseInt(ackMsg.substring(4));
                    
                    if (ackNum >= base) {
                        base = ackNum + 1;
                        
                        // Mostrar progreso cada 50 paquetes confirmados
                        if (base % 50 == 0 || base == totalPackets) {
                            double progress = (base * 100.0) / totalPackets;
                            System.out.printf("Progreso: %.1f%% (%d/%d paquetes confirmados)\n", 
                                            progress, base, totalPackets);
                        }
                    }
                }
                
            } catch (SocketTimeoutException e) {
                // Timeout: retransmitir toda la ventana
                System.out.println("⚠ TIMEOUT! Retransmitiendo desde paquete " + base);
                retransmissions++;
                nextSeqNum = base; // Volver a enviar desde base
            }
        }
        
        long endTime = System.currentTimeMillis();
        double timeSeconds = (endTime - startTime) / 1000.0;
        
        System.out.println("\n=== Estadísticas de transmisión ===");
        System.out.println("Tiempo total: " + String.format("%.2f", timeSeconds) + " segundos");
        System.out.println("Paquetes enviados: " + totalSent);
        System.out.println("Retransmisiones: " + retransmissions);
        System.out.println("Eficiencia: " + String.format("%.2f", (totalPackets * 100.0) / totalSent) + "%");
    }
    
    private static void sendPacket(DatagramSocket socket, byte[] data, int seqNum, 
                                   int totalPackets, InetAddress address, int port) throws IOException {
        // Crear paquete con header: [seqNum (4 bytes)][totalPackets (4 bytes)][data]
        ByteBuffer buffer = ByteBuffer.allocate(HEADER_SIZE + data.length);
        buffer.putInt(seqNum);
        buffer.putInt(totalPackets);
        buffer.put(data);
        
        byte[] packetData = buffer.array();
        DatagramPacket packet = new DatagramPacket(packetData, packetData.length, address, port);
        socket.send(packet);
    }
}
