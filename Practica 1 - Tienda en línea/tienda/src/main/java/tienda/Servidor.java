package tienda;
import java.net.*;
import java.io.*;
import java.util.List;

public class Servidor {
    private static Inventario inventario = Inventario.getInstance();
    
    // Método para procesar las compras recibidas
    public static void procesarCompra(String carritoJSON) {
        try {
            System.out.println("=== PROCESANDO COMPRA ===");
            System.out.println("Datos del carrito: " + carritoJSON);
        
            inventario.mostrarEstadoInventario();
            boolean compraExitosa = inventario.procesarCompra(carritoJSON);
            if (compraExitosa) {
                System.out.println("Compra procesada exitosamente en el inventario");
                inventario.mostrarEstadoInventario();
            } else {
                System.out.println("ERROR: No se pudo procesar la compra - Stock insuficiente o producto no encontrado");
            }
            
            System.out.println("========================");
            
        } catch (Exception e) {
            System.out.println("Error al procesar la compra: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static void main(String[] args) {
        try{
            ServerSocket servidor = new ServerSocket(1234);
            System.out.println("Servidor iniciado");

            for(;;){
                Socket cliente = servidor.accept();
                System.out.println("Cliente conectado en el puerto: " + cliente.getPort() + " desde la IP: " + cliente.getInetAddress());

                // Crear streams de entrada y salida
                PrintWriter escritor = new PrintWriter(new OutputStreamWriter(cliente.getOutputStream(), "UTF-8"), true);
                BufferedReader lector = new BufferedReader(new InputStreamReader(cliente.getInputStream(), "UTF-8"));
                
                // Leer el tipo de solicitud del cliente
                String solicitud = lector.readLine();
                System.out.println("Solicitud recibida: " + solicitud);
                
                if("OBTENER_PRODUCTOS".equals(solicitud)){
                    // Flujo para enviar productos del inventario al cliente
                    List<Articulo> productos = inventario.obtenerTodosLosProductos();
                    
                    for (Articulo producto : productos) {
                        escritor.println(producto.toJSON().toJSONString());
                    }
                    escritor.println("FIN_PRODUCTOS"); // Marcador para indicar fin de productos
                    escritor.flush();
                    
                    System.out.println("Productos del inventario enviados al cliente (" + productos.size() + " productos)");
                    
                } else if(solicitud != null && solicitud.startsWith("COMPRA:")){
                    // Flujo para recibir y procesar compra del cliente
                    String carritoJSON = solicitud.substring(7);
                    System.out.println("Compra recibida del cliente: " + carritoJSON);
                    boolean compraExitosa = inventario.procesarCompra(carritoJSON);
                    
                    if (compraExitosa) {
                        escritor.println("COMPRA_EXITOSA: La compra ha sido procesada correctamente y el stock ha sido actualizado");
                    } else {
                        escritor.println("COMPRA_ERROR: No se pudo procesar la compra - Stock insuficiente o producto no encontrado");
                    }
                }

                escritor.close();
                lector.close();
                cliente.close();
                System.out.println("Conexión cerrada con el cliente");
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
