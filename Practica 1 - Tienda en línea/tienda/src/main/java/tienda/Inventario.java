package tienda;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.json.simple.*;
import org.json.simple.parser.JSONParser;

public class Inventario {
    private static Inventario instance;
    private Map<String, Articulo> productos;
    
    private Inventario() {
        productos = new ConcurrentHashMap<>();
        inicializarProductos();
    }
    
    public static Inventario getInstance() {
        if (instance == null) {
            instance = new Inventario();
        }
        return instance;
    }
    
    private void inicializarProductos() {
        // Productos iniciales del inventario
        Articulo articulo1 = new Articulo("Electrónica", "Laptop", "Dell", "Laptop para uso personal", 1500.0, 10);
        Articulo articulo2 = new Articulo("Hogar", "Aspiradora", "Dyson", "Aspiradora potente", 300.0, 5);
        Articulo articulo3 = new Articulo("Comedor", "Mesa", "Ikea", "Mesa de comedor grande", 200.0, 3);
        Articulo articulo4 = new Articulo("Electrónica", "ALIENWARE", "Dell", "Laptop para uso personal", 1500.0, 10);

        // Usar nombre como clave única
        productos.put("Laptop-Dell", articulo1);
        productos.put("Aspiradora-Dyson", articulo2);
        productos.put("Mesa-Ikea", articulo3);
        productos.put("Laptop2-Dell", articulo4);
        
        System.out.println("Inventario inicializado con " + productos.size() + " productos");
    }
    
    public List<Articulo> obtenerTodosLosProductos() {
        return new ArrayList<>(productos.values());
    }
    
    public Articulo obtenerProducto(String nombre, String marca) {
        String clave = nombre + "-" + marca;
        return productos.get(clave);
    }
    
    public boolean actualizarStock(String nombre, String marca, int cantidadComprada) {
        String clave = nombre + "-" + marca;
        Articulo producto = productos.get(clave);
        
        if (producto == null) {
            System.out.println("Producto no encontrado: " + clave);
            return false;
        }
        
        if (producto.cantidad < cantidadComprada) {
            System.out.println("Stock insuficiente para " + clave + ". Disponible: " + producto.cantidad + ", Solicitado: " + cantidadComprada);
            return false;
        }
        
        producto.cantidad -= cantidadComprada;
        System.out.println("Stock actualizado para " + clave + ". Nuevo stock: " + producto.cantidad);
        return true;
    }
    
    public boolean procesarCompra(String carritoJSON) {
        try {
            JSONParser parser = new JSONParser();
            JSONObject carritoObj = (JSONObject) parser.parse(carritoJSON);
            JSONArray productosArray = (JSONArray) carritoObj.get("productos");
            
            System.out.println("=== PROCESANDO COMPRA EN INVENTARIO ===");
            System.out.println("Productos a procesar: " + productosArray.size());
            
            // Primero validar que todos los productos tienen stock suficiente
            for (Object obj : productosArray) {
                JSONObject productoJSON = (JSONObject) obj;
                String nombre = (String) productoJSON.get("nombre");
                String marca = (String) productoJSON.get("marca");
                int cantidad = 1; // Por ahora asumimos cantidad 1 por producto
                
                Articulo producto = obtenerProducto(nombre, marca);
                if (producto == null) {
                    System.out.println("ERROR: Producto no encontrado - " + nombre + " " + marca);
                    return false;
                }
                
                if (producto.cantidad < cantidad) {
                    System.out.println("ERROR: Stock insuficiente para " + nombre + " " + marca + 
                                     ". Disponible: " + producto.cantidad + ", Solicitado: " + cantidad);
                    return false;
                }
            }
            
            // Si todos tienen stock suficiente, proceder con la actualización
            for (Object obj : productosArray) {
                JSONObject productoJSON = (JSONObject) obj;
                String nombre = (String) productoJSON.get("nombre");
                String marca = (String) productoJSON.get("marca");
                int cantidad = 1; // Por ahora asumimos cantidad 1 por producto
                
                actualizarStock(nombre, marca, cantidad);
            }
            
            System.out.println("Compra procesada exitosamente en el inventario");
            System.out.println("===============================================");
            return true;
            
        } catch (Exception e) {
            System.out.println("Error al procesar compra en inventario: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    public void mostrarEstadoInventario() {
        System.out.println("\n=== ESTADO ACTUAL DEL INVENTARIO ===");
        for (Articulo producto : productos.values()) {
            System.out.println(producto.nombre + " " + producto.marca + " - Stock: " + producto.cantidad);
        }
        System.out.println("=====================================\n");
    }
}
