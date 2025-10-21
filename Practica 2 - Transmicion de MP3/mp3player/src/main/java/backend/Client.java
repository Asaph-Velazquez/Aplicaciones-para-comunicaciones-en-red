package backend;
import java.net.*;
import java.io.*;
import java.nio.ByteBuffer;

public class Client {
    private static final int HEADER_SIZE = 8; // 4 bytes seq + 4 bytes total

    public static void main(String[] args) {
        try {
            // Puerto donde el cliente escucha
            int clientPort = 5678;
            DatagramSocket clientSocket = new DatagramSocket(clientPort);
            clientSocket.setReuseAddress(true);
            clientSocket.setSoTimeout(30000); // Timeout de 30 segundos
            
            System.out.println("Cliente iniciado en el puerto: " + clientSocket.getLocalPort());
            System.out.println("Usando protocolo Go-Back-N ARQ");

            // Enviar mensaje READY al servidor
            String readyMsg = "READY";
            InetAddress serverAddress = InetAddress.getByName("localhost");
            int serverPort = 1234;
            
            DatagramPacket readyPacket = new DatagramPacket(
                readyMsg.getBytes(), 
                readyMsg.length(), 
                serverAddress, 
                serverPort
            );
            clientSocket.send(readyPacket);
            System.out.println("Mensaje READY enviado al servidor\n");

            // Recibir con Go-Back-N
            receiveGoBackN(clientSocket, serverAddress, serverPort);

            clientSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static void receiveGoBackN(DatagramSocket socket, InetAddress serverAddress, 
                                       int serverPort) throws IOException {
        int expectedSeqNum = 0; // Número de secuencia esperado
        int totalPackets = -1; // Total de paquetes (se lee del primer paquete)
        byte[][] receivedPackets = null;
        
        byte[] receiveBuffer = new byte[7000]; // Buffer para recibir paquetes
        
        System.out.println("=== Iniciando recepción Go-Back-N ===\n");
        long startTime = System.currentTimeMillis();
        int packetsReceived = 0;
        int duplicates = 0;
        
        while (expectedSeqNum < totalPackets || totalPackets == -1) {
            DatagramPacket packet = new DatagramPacket(receiveBuffer, receiveBuffer.length);
            
            try {
                socket.receive(packet);
                packetsReceived++;
                
                // Extraer header
                ByteBuffer buffer = ByteBuffer.wrap(packet.getData(), 0, packet.getLength());
                int seqNum = buffer.getInt();
                int total = buffer.getInt();
                
                // Inicializar array si es el primer paquete
                if (totalPackets == -1) {
                    totalPackets = total;
                    receivedPackets = new byte[totalPackets][];
                    System.out.println("Total de paquetes a recibir: " + totalPackets + "\n");
                }
                
                // Extraer datos
                int dataLength = packet.getLength() - HEADER_SIZE;
                byte[] data = new byte[dataLength];
                buffer.get(data);
                
                if (seqNum == expectedSeqNum) {
                    // Paquete esperado: guardar y avanzar
                    receivedPackets[seqNum] = data;
                    expectedSeqNum++;
                    
                    // Enviar ACK acumulativo
                    sendAck(socket, expectedSeqNum - 1, serverAddress, serverPort);
                    
                    // Mostrar progreso cada 50 paquetes
                    if (expectedSeqNum % 50 == 0) {
                        double progress = (expectedSeqNum * 100.0) / totalPackets;
                        System.out.printf("Recibido: %.1f%% (%d/%d paquetes)\n", 
                                        progress, expectedSeqNum, totalPackets);
                    }
                    
                } else if (seqNum < expectedSeqNum) {
                    // Paquete duplicado: reenviar ACK
                    duplicates++;
                    sendAck(socket, expectedSeqNum - 1, serverAddress, serverPort);
                    
                } else {
                    // Paquete fuera de orden: descartar y reenviar último ACK
                    System.out.println("⚠ Paquete fuera de orden: " + seqNum + 
                                     " (esperado: " + expectedSeqNum + ")");
                    sendAck(socket, expectedSeqNum - 1, serverAddress, serverPort);
                }
                
            } catch (SocketTimeoutException e) {
                System.out.println("⚠ Timeout esperando paquetes");
                break;
            }
        }
        
        long endTime = System.currentTimeMillis();
        double timeSeconds = (endTime - startTime) / 1000.0;
        
        // Reconstruir archivo
        if (expectedSeqNum == totalPackets) {
            reconstructFile(receivedPackets);
            
            System.out.println("\n=== Estadísticas de recepción ===");
            System.out.println("✓ Archivo recibido completamente");
            System.out.println("Tiempo total: " + String.format("%.2f", timeSeconds) + " segundos");
            System.out.println("Paquetes recibidos: " + packetsReceived);
            System.out.println("Duplicados: " + duplicates);
        } else {
            System.out.println("\n⚠ Recepción incompleta: " + expectedSeqNum + "/" + totalPackets);
        }
    }
    
    private static void sendAck(DatagramSocket socket, int ackNum, 
                               InetAddress address, int port) throws IOException {
        String ackMsg = "ACK:" + ackNum;
        byte[] ackData = ackMsg.getBytes();
        DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length, address, port);
        socket.send(ackPacket);
    }
    
    private static void reconstructFile(byte[][] packets) throws IOException {
        // Determinar ruta donde guardar el archivo
        var resource = Client.class.getClassLoader().getResource("song/");
        String outputPath;
        
        if (resource != null) {
            // Guardar en resources/song/
            File resourceDir = new File(resource.getFile());
            outputPath = new File(resourceDir, "cancion_recibida.mp3").getAbsolutePath();
        } else {
            // Guardar en directorio actual
            outputPath = "cancion_recibida.mp3";
        }
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (byte[] packet : packets) {
            if (packet != null) {
                baos.write(packet);
            }
        }
        
        FileOutputStream fos = new FileOutputStream(outputPath);
        baos.writeTo(fos);
        fos.close();
        baos.close();
        
        System.out.println("\n✓ Archivo MP3 reconstruido: " + outputPath);
        System.out.println("Tamaño: " + (baos.size() / 1024) + " KB");
    }
}
