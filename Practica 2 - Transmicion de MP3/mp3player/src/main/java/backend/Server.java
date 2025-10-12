package backend;
import java.io.*;
import java.net.*;

public class Server {
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

            // Fragmentamos el MP3 en paquetes de 6500 bytes
            byte[][] paquetes = player.fragmentarMP3(mp3Bytes, 6500);
            System.out.println("MP3 fragmentado en " + paquetes.length + " paquetes");

            // Iniciamos el servidor UDP
            DatagramSocket serverSocket = new DatagramSocket(1234);
            serverSocket.setReuseAddress(true);
            System.out.println("Servidor iniciado en el puerto: " + serverSocket.getLocalPort());

            InetAddress clientAddress = InetAddress.getByName("127.0.0.1"); // ejemplo cliente local
            int clientPort = 5678; // puerto del cliente

            // Enviar los paquetes
            for (int i = 0; i < paquetes.length; i++) {
                DatagramPacket packet = new DatagramPacket(paquetes[i], paquetes[i].length, clientAddress, clientPort);
                serverSocket.send(packet);
                System.out.println("Paquete " + i + " enviado");
            }

            serverSocket.close();
            System.out.println("Todos los paquetes enviados, servidor cerrado");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
