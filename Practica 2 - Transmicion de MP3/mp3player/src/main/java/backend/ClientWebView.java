package backend;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

public class ClientWebView extends Application {
    private static volatile boolean downloadComplete = false;
    private static String songPath = null;
    private static final int HEADER_SIZE = 8; // 4 bytes seq + 4 bytes total

    public static void main(String[] args) {
        // Forzar uso de software rendering en lugar de hardware
        System.setProperty("prism.order", "sw");
        System.setProperty("prism.verbose", "true");
        
        // Iniciar descarga en hilo separado
        new Thread(() -> {
            try {
                downloadSong();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        // Lanzar JavaFX
        launch(args);
    }

    private static void downloadSong() {
        try {
            // Puerto donde el cliente escucha
            int clientPort = 5678;
            DatagramSocket clientSocket = new DatagramSocket(clientPort);
            clientSocket.setReuseAddress(true);
            clientSocket.setSoTimeout(30000); // Timeout de 30 segundos
            
            System.out.println("Cliente UDP iniciado en el puerto: " + clientSocket.getLocalPort());
            System.out.println("Usando protocolo Go-Back-N ARQ");
            
            // Enviar mensaje READY al servidor para indicar que estamos listos
            byte[] readyMsg = "READY".getBytes();
            InetAddress serverAddress = InetAddress.getByName("127.0.0.1");
            DatagramPacket readyPacket = new DatagramPacket(readyMsg, readyMsg.length, serverAddress, 1234);
            clientSocket.send(readyPacket);
            System.out.println("Mensaje READY enviado al servidor\n");

            // Recibir con Go-Back-N
            receiveGoBackN(clientSocket, serverAddress, 1234);
            
            clientSocket.close();
            downloadComplete = true;

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
        // Guardar en resources/song/
        File songDir = new File("src/main/resources/song");
        if (!songDir.exists()) {
            songDir.mkdirs();
        }
        
        File outputFile = new File(songDir, "cancion_recibida.mp3");
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (byte[] packet : packets) {
            if (packet != null) {
                baos.write(packet);
            }
        }
        
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            baos.writeTo(fos);
        }
        baos.close();
        
        songPath = outputFile.getAbsolutePath();
        System.out.println("\n✓ Archivo MP3 reconstruido: " + songPath);
        System.out.println("Tamaño: " + (baos.size() / 1024) + " KB");
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            // Crear WebView
            WebView webView = new WebView();
            
            // Cargar el archivo HTML
            File htmlFile = new File("src/main/resources/html/index.html");
            if (!htmlFile.exists()) {
                System.err.println("No se encontró index.html en src/main/resources/html/");
                Platform.exit();
                return;
            }
            
            String url = htmlFile.toURI().toString();
            System.out.println("Cargando interfaz web: " + url);
            webView.getEngine().load(url);

            // Configurar escena y ventana
            Scene scene = new Scene(webView, 1000, 700);
            primaryStage.setTitle("MP3 Player - Cliente UDP con WebView");
            primaryStage.setScene(scene);
            primaryStage.show();

            // Esperar a que se complete la descarga y recargar el audio
            new Thread(() -> {
                while (!downloadComplete) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                
                // Cuando termine la descarga, recargar el audio en el WebView
                Platform.runLater(() -> {
                    System.out.println("Descarga completa, recargando audio...");
                    webView.getEngine().executeScript(
                        "var audio = document.getElementById('audio');" +
                        "audio.load();" +
                        "console.log('Audio recargado después de recibir el archivo');"
                    );
                });
            }).start();

            primaryStage.setOnCloseRequest(e -> {
                Platform.exit();
                System.exit(0);
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
