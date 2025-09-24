package tienda;
import java.net.*;
import java.io.*;
import org.json.simple.*;

public class Servidor {
    public static void main(String[] args) {
        try{
            ServerSocket servidor = new ServerSocket(1234);
            System.out.println("Servidor iniciado");

            for(;;){
                Socket cliente = servidor.accept();
                System.out.println("Cliente conectado en el puerto: " + cliente.getPort() + " desde la IP: " + cliente.getInetAddress());

                //Flujo para enviar el json al cliente
                Articulo articulo1 = new Articulo("Electrónica", "Laptop", "Dell", "Laptop para uso personal", 1500.0, 10);
                Articulo articulo2 = new Articulo("Hogar", "Aspiradora", "Dyson", "Aspiradora potente", 300.0, 5);
                Articulo articulo3 = new Articulo("Comedor", "Mesa", "Ikea", "Mesa de comedor grande", 200.0, 3);

                PrintWriter escritor = new PrintWriter(new OutputStreamWriter(cliente.getOutputStream(), "UTF-8"), true);
                escritor.println(articulo1.toJSON().toJSONString());
                escritor.println(articulo2.toJSON().toJSONString());
                escritor.println(articulo3.toJSON().toJSONString());
                escritor.flush();
                
                System.out.println("JSON enviado al cliente: " + articulo1.toJSON().toJSONString());
                System.out.println("JSON enviado al cliente: " + articulo2.toJSON().toJSONString());
                
                escritor.close();
                cliente.close();
                System.out.println("Conexión cerrada con el cliente");
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
