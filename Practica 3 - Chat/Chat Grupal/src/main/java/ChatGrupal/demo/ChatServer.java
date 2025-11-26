package ChatGrupal.demo;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Servidor de Chat Grupal UDP con Hilos
 * Cumple requisitos: Sockets UDP + Manejo de hilos
 */
public class ChatServer {
    private static final int PORT = 5000;
    private static Map<String, Set<String>> salas = new HashMap<>();
    private static final Object lock = new Object();
    private static int threadCounter = 0;

    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════════════════╗");
        System.out.println("║      SERVIDOR UDP DE CHAT GRUPAL CON HILOS            ║");
        System.out.println("╚════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("🔧 Configuración:");
        System.out.println("   └─ Protocolo: UDP (DatagramSocket)");
        System.out.println("   └─ Puerto: " + PORT);
        System.out.println("   └─ Manejo: Multihilo (Thread por cliente)");
        System.out.println();

        try (DatagramSocket serverSocket = new DatagramSocket(PORT)) {
            System.out.println("✅ Socket UDP creado exitosamente");
            System.out.println("   └─ Tipo: java.net.DatagramSocket");
            System.out.println("   └─ Puerto local: " + serverSocket.getLocalPort());
            System.out.println("   └─ Buffer tamaño: 1024 bytes");
            System.out.println();
            System.out.println("🎧 Servidor escuchando conexiones UDP...");
            System.out.println("════════════════════════════════════════════════════════");
            System.out.println();

            byte[] receiveBuffer = new byte[1024];

            while (true) {
                DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                
                // Bloquea hasta recibir paquete
                serverSocket.receive(receivePacket);
                
                // Incrementar contador de hilos
                threadCounter++;
                
                System.out.println("📨 [NUEVO PAQUETE UDP RECIBIDO]");
                System.out.println("   ├─ Desde: " + receivePacket.getAddress().getHostAddress() + ":" + receivePacket.getPort());
                System.out.println("   ├─ Tamaño: " + receivePacket.getLength() + " bytes");
                System.out.println("   └─ Datos: " + new String(receivePacket.getData(), 0, receivePacket.getLength(), StandardCharsets.UTF_8));
                
                // Crear y lanzar nuevo hilo
                Thread clientThread = new Thread(new ClientHandler(receivePacket, serverSocket, salas, threadCounter));
                clientThread.setName("ClientHandler-" + threadCounter);
                
                System.out.println();
                System.out.println("🧵 [CREANDO NUEVO HILO]");
                System.out.println("   ├─ ID Hilo: #" + threadCounter);
                System.out.println("   ├─ Nombre: " + clientThread.getName());
                System.out.println("   ├─ Estado: " + clientThread.getState());
                System.out.println("   └─ Cliente: " + receivePacket.getAddress().getHostAddress());
                
                clientThread.start();
                
                System.out.println("   ✅ Hilo iniciado");
                System.out.println("   └─ Total hilos activos: ~" + Thread.activeCount());
                System.out.println("════════════════════════════════════════════════════════");
                System.out.println();
            }
        } catch (IOException e) {
            System.err.println("❌ Error en el servidor UDP: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Clase interna para manejar cada cliente en un hilo separado
     */
    static class ClientHandler implements Runnable {
        private DatagramPacket packet;
        private DatagramSocket socket;
        private Map<String, Set<String>> salas;
        private int threadId;

        public ClientHandler(DatagramPacket packet, DatagramSocket socket, Map<String, Set<String>> salas, int threadId) {
            this.packet = packet;
            this.socket = socket;
            this.salas = salas;
            this.threadId = threadId;
        }

        @Override
        public void run() {
            String threadName = Thread.currentThread().getName();
            System.out.println("🔄 [HILO #" + threadId + " EJECUTÁNDOSE]");
            System.out.println("   └─ Thread: " + threadName);
            
            try {
                String mensaje = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
                String[] partes = mensaje.split("\\|");
                String comando = partes[0];

                System.out.println("   ├─ Procesando comando: " + comando);

                if ("JOIN".equals(comando)) {
                    String usuario = partes[1];
                    String sala = partes[2];
                    System.out.println("   ├─ Usuario: " + usuario);
                    System.out.println("   └─ Sala: " + sala);
                    agregarUsuarioASala(usuario, sala);
                    responderAlCliente(usuario, sala);
                } else if ("LEAVE".equals(comando)) {
                    String usuario = partes[1];
                    String sala = partes[2];
                    System.out.println("   ├─ Usuario: " + usuario);
                    System.out.println("   └─ Sala: " + sala);
                    removerUsuarioDeSala(usuario, sala);
                } else if ("LIST".equals(comando)) {
                    String sala = partes[1];
                    System.out.println("   └─ Sala: " + sala);
                    responderListaUsuarios(sala);
                } else if ("SEND".equals(comando)) {
                    String usuario = partes[1];
                    String sala = partes[2];
                    String contenido = String.join("|", java.util.Arrays.copyOfRange(partes, 3, partes.length));
                    System.out.println("   ├─ Usuario: " + usuario);
                    System.out.println("   ├─ Sala: " + sala);
                    System.out.println("   └─ Mensaje: " + contenido);
                    // Solo registrar, el bridge maneja el broadcast
                } else if ("PRIVATE".equals(comando)) {
                    String fromUser = partes[1];
                    String toUser = partes[2];
                    String contenido = String.join("|", java.util.Arrays.copyOfRange(partes, 3, partes.length));
                    System.out.println("   ├─ De: " + fromUser);
                    System.out.println("   ├─ Para: " + toUser);
                    System.out.println("   └─ Mensaje privado: " + contenido);
                    // Solo registrar, el bridge maneja el envío privado
                }
                
                System.out.println("   ✅ Hilo #" + threadId + " completado");
            } catch (Exception e) {
                System.err.println("   ❌ Error en hilo #" + threadId + ": " + e.getMessage());
            }
        }

        private void agregarUsuarioASala(String usuario, String sala) {
            synchronized (lock) {
                salas.computeIfAbsent(sala, k -> new HashSet<>()).add(usuario);
                System.out.println("   📝 Usuario '" + usuario + "' agregado a sala '" + sala + "'");
                System.out.println("   └─ Usuarios en '" + sala + "': " + salas.get(sala));
            }
        }

        private void removerUsuarioDeSala(String usuario, String sala) {
            synchronized (lock) {
                if (salas.containsKey(sala)) {
                    salas.get(sala).remove(usuario);
                    System.out.println("   🗑️  Usuario '" + usuario + "' removido de sala '" + sala + "'");
                }
            }
        }

        private void responderAlCliente(String usuario, String sala) throws IOException {
            synchronized (lock) {
                Set<String> usuarios = salas.getOrDefault(sala, new HashSet<>());
                String respuesta = "OK|" + String.join(",", usuarios);
                
                System.out.println("   📤 Enviando respuesta UDP:");
                System.out.println("      ├─ Destino: " + packet.getAddress().getHostAddress() + ":" + packet.getPort());
                System.out.println("      └─ Contenido: " + respuesta);
                
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
