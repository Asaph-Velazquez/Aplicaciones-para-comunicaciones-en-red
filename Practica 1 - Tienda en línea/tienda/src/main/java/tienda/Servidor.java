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
                JSONObject JsonArticulo = new JSONObject();
                Articulo articulo1 = new Articulo("Laptop", "Dell", "Laptop para uso personal", 1500.0, 10);
                articulo1.toJSON();
                PrintWriter escritor = new PrintWriter(new OutputStreamWriter(cliente.getOutputStream(), "UTF-8"), true);
                escritor.println(articulo1.toJSON().toJSONString());
                escritor.flush();
                System.out.println("JSON enviado al cliente: " + articulo1.toJSON().toJSONString());
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
