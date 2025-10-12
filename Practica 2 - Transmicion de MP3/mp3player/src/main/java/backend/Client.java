package backend;
import java.net.*;
import java.io.*;

public class Client {
    public static void main(String[] args) {
        try {
            // Puerto donde el cliente escucha
            int clientPort = 5678;
            DatagramSocket clientSocket = new DatagramSocket(clientPort);
            clientSocket.setReuseAddress(true);
            
            System.out.println("Cliente iniciado en el puerto: " + clientSocket.getLocalPort());

            // Preparar para recibir
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] receiveData = new byte[6500];
            
            int totalPaquetes = 10; 
            
            for (int i = 0; i < totalPaquetes; i++) {
                DatagramPacket packet = new DatagramPacket(receiveData, receiveData.length);
                clientSocket.receive(packet);
                
                System.out.println("Paquete " + i + " recibido, tamaño: " + packet.getLength() + " bytes");
                baos.write(packet.getData(), 0, packet.getLength());
            }

            // Guardar archivo recibido
            FileOutputStream fos = new FileOutputStream("cancion_recibida.mp3");
            baos.writeTo(fos);
            fos.close();
            baos.close();

            System.out.println("Archivo MP3 reconstruido: cancion_recibida.mp3");

            clientSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
