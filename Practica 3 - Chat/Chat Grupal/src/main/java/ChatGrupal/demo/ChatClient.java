package ChatGrupal.demo;

import java.io.*;
import java.net.*;
import java.util.Scanner;

/**
 * Cliente de Chat Grupal
 * Permite a los usuarios conectarse a salas y enviar/recibir mensajes
 */
public class ChatClient {
    private static final String SERVER_IP = "localhost";
    private static final int SERVER_PORT = 5000;
    private DatagramSocket clientSocket;
    private String nombreUsuario;
    private String salaActual;

    public ChatClient() throws SocketException {
        this.clientSocket = new DatagramSocket();
    }

    public static void main(String[] args) {
        try {
            ChatClient cliente = new ChatClient();
            cliente.iniciarCliente();
        } catch (SocketException e) {
            System.err.println("Error al crear socket: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void iniciarCliente() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("=== Cliente de Chat Grupal ===");

        System.out.print("Ingrese su nombre de usuario: ");
        nombreUsuario = scanner.nextLine().trim();

        boolean activo = true;
        while (activo) {
            mostrarMenu();
            System.out.print("Seleccione una opción: ");
            String opcion = scanner.nextLine().trim();

            switch (opcion) {
                case "1":
                    unirseASala(scanner);
                    break;
                case "2":
                    listarUsuariosEnSala();
                    break;
                case "3":
                    enviarMensaje(scanner);
                    break;
                case "4":
                    abandonarSala();
                    break;
                case "5":
                    activo = false;
                    System.out.println("Desconectando...");
                    break;
                default:
                    System.out.println("Opción no válida");
            }
        }

        scanner.close();
        clientSocket.close();
    }

    private void mostrarMenu() {
        System.out.println("\n--- Menú Principal ---");
        System.out.println("1. Unirse a una sala");
        System.out.println("2. Listar usuarios en la sala actual");
        System.out.println("3. Enviar mensaje");
        System.out.println("4. Abandonar sala");
        System.out.println("5. Salir");
    }

    private void unirseASala(Scanner scanner) {
        System.out.print("Ingrese el nombre de la sala: ");
        salaActual = scanner.nextLine().trim();

        if (salaActual.isEmpty()) {
            System.out.println("El nombre de la sala no puede estar vacío");
            return;
        }

        try {
            String mensaje = "JOIN|" + nombreUsuario + "|" + salaActual;
            enviarMensajeAlServidor(mensaje);

            String respuesta = recibirRespuesta();
            if (respuesta.startsWith("OK")) {
                String[] partes = respuesta.split("\\|");
                String usuarios = partes.length > 1 ? partes[1] : "";
                System.out.println("✓ Unido a la sala: " + salaActual);
                System.out.println("Usuarios en la sala: " + usuarios);
            }
        } catch (IOException e) {
            System.err.println("Error al unirse a la sala: " + e.getMessage());
        }
    }

    private void listarUsuariosEnSala() {
        if (salaActual == null || salaActual.isEmpty()) {
            System.out.println("Primero debe unirse a una sala");
            return;
        }

        try {
            String mensaje = "LIST|" + salaActual;
            enviarMensajeAlServidor(mensaje);

            String respuesta = recibirRespuesta();
            if (respuesta.startsWith("LIST")) {
                String[] partes = respuesta.split("\\|");
                String usuarios = partes.length > 1 ? partes[1] : "";
                System.out.println("Usuarios en " + salaActual + ": " + usuarios);
            }
        } catch (IOException e) {
            System.err.println("Error al listar usuarios: " + e.getMessage());
        }
    }

    private void enviarMensaje(Scanner scanner) {
        if (salaActual == null || salaActual.isEmpty()) {
            System.out.println("Primero debe unirse a una sala");
            return;
        }

        System.out.print("Ingrese su mensaje: ");
        String mensaje = scanner.nextLine().trim();

        if (mensaje.isEmpty()) {
            System.out.println("El mensaje no puede estar vacío");
            return;
        }

        System.out.println("[" + nombreUsuario + " en " + salaActual + "]: " + mensaje);
        System.out.println("Mensaje enviado...");
    }

    private void abandonarSala() {
        if (salaActual == null || salaActual.isEmpty()) {
            System.out.println("No está en ninguna sala");
            return;
        }

        try {
            String mensaje = "LEAVE|" + nombreUsuario + "|" + salaActual;
            enviarMensajeAlServidor(mensaje);
            System.out.println("✓ Ha abandonado la sala: " + salaActual);
            salaActual = null;
        } catch (IOException e) {
            System.err.println("Error al abandonar la sala: " + e.getMessage());
        }
    }

    private void enviarMensajeAlServidor(String mensaje) throws IOException {
        byte[] buffer = mensaje.getBytes();
        InetAddress serverAddress = InetAddress.getByName(SERVER_IP);
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, serverAddress, SERVER_PORT);
        clientSocket.send(packet);
    }

    private String recibirRespuesta() throws IOException {
        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        clientSocket.receive(packet);
        return new String(packet.getData(), 0, packet.getLength());
    }
}
