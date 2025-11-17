package ChatGrupal.demo;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Servidor de Chat Grupal
 * Retransmite la lista de usuarios activos en cada sala
 */
public class ChatServer {
    private static final int PORT = 5000;
    private static Map<String, Set<String>> salas = new HashMap<>();
    private static final Object lock = new Object();

    public static void main(String[] args) {
        System.out.println("Iniciando Servidor de Chat...");
        System.out.println("Escuchando en puerto: " + PORT);

        try (DatagramSocket serverSocket = new DatagramSocket(PORT)) {
            byte[] receiveBuffer = new byte[1024];

            while (true) {
                DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                serverSocket.receive(receivePacket);

                // Procesar solicitud del cliente en un nuevo hilo
                new Thread(new ClientHandler(receivePacket, serverSocket, salas)).start();
            }
        } catch (IOException e) {
            System.err.println("Error en el servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Clase interna para manejar cada cliente
     */
    static class ClientHandler implements Runnable {
        private DatagramPacket packet;
        private DatagramSocket socket;
        private Map<String, Set<String>> salas;

        public ClientHandler(DatagramPacket packet, DatagramSocket socket, Map<String, Set<String>> salas) {
            this.packet = packet;
            this.socket = socket;
            this.salas = salas;
        }

        @Override
        public void run() {
            try {
                String mensaje = new String(packet.getData(), 0, packet.getLength());
                String[] partes = mensaje.split("\\|");
                String comando = partes[0];

                System.out.println("Mensaje recibido: " + mensaje);

                if ("JOIN".equals(comando)) {
                    String usuario = partes[1];
                    String sala = partes[2];
                    agregarUsuarioASala(usuario, sala);
                    responderAlCliente(usuario, sala);
                } else if ("LEAVE".equals(comando)) {
                    String usuario = partes[1];
                    String sala = partes[2];
                    removerUsuarioDeSala(usuario, sala);
                } else if ("LIST".equals(comando)) {
                    String sala = partes[1];
                    responderListaUsuarios(sala);
                }
            } catch (Exception e) {
                System.err.println("Error procesando cliente: " + e.getMessage());
            }
        }

        private void agregarUsuarioASala(String usuario, String sala) {
            synchronized (lock) {
                salas.computeIfAbsent(sala, k -> new HashSet<>()).add(usuario);
                System.out.println("Usuario " + usuario + " agregado a sala " + sala);
                System.out.println("Usuarios en " + sala + ": " + salas.get(sala));
            }
        }

        private void removerUsuarioDeSala(String usuario, String sala) {
            synchronized (lock) {
                if (salas.containsKey(sala)) {
                    salas.get(sala).remove(usuario);
                    System.out.println("Usuario " + usuario + " removido de sala " + sala);
                }
            }
        }

        private void responderAlCliente(String usuario, String sala) throws IOException {
            synchronized (lock) {
                Set<String> usuarios = salas.getOrDefault(sala, new HashSet<>());
                String respuesta = "OK|" + String.join(",", usuarios);
                enviarRespuesta(respuesta);
            }
        }

        private void responderListaUsuarios(String sala) throws IOException {
            synchronized (lock) {
                Set<String> usuarios = salas.getOrDefault(sala, new HashSet<>());
                String respuesta = "LIST|" + String.join(",", usuarios);
                enviarRespuesta(respuesta);
            }
        }

        private void enviarRespuesta(String respuesta) throws IOException {
            byte[] responseBuffer = respuesta.getBytes();
            DatagramPacket responsePacket = new DatagramPacket(
                    responseBuffer,
                    responseBuffer.length,
                    packet.getAddress(),
                    packet.getPort()
            );
            socket.send(responsePacket);
        }
    }
}
