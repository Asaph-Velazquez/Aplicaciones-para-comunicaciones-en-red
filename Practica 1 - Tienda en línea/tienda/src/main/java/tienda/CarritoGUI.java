package tienda;
import java.util.List;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import netscape.javascript.JSObject;
import org.json.simple.*;

public class CarritoGUI extends Application {
    private Carrito carrito;
    
    public CarritoGUI() {
        this.carrito = new Carrito();
    }
    
    @Override
    public void start(Stage stage) {
        WebView webView = new WebView();
        WebEngine webEngine = webView.getEngine();
        webEngine.load(getClass().getResource("/ui/carrito.html").toExternalForm());

        // Conectar el carrito con la interfaz
        webEngine.getLoadWorker().stateProperty().addListener((obs, o, n) -> {
            if (n == javafx.concurrent.Worker.State.SUCCEEDED) {
                try {
                    JSObject window = (JSObject) webEngine.executeScript("window");

                    // Obtener productos del servidor y agregarlos al carrito
                    List<JSONObject> productos = Cliente.obtenerProductos();

                    // Convertir cada JSON a Articulo y agregar al carrito
                    for (JSONObject jsonProducto : productos) {
                        Articulo articulo = jsonToArticulo(jsonProducto);
                        carrito.agregarArticulo(articulo);
                    }

                    // Crear un arreglo JSON para enviar al JavaScript
                    JSONArray arrayCarrito = new JSONArray();
                    for (Articulo art : carrito.getArticulos()) {
                        arrayCarrito.add(art.toJSON());
                    }

                    // Pasar datos del carrito a la página HTML
                    window.call("mostrarCarritoCompleto", arrayCarrito.toJSONString(), carrito.getTotal());

                } catch (Exception e) {
                    e.printStackTrace();
                    // En caso de error, mostrar carrito vacío
                    JSObject window = (JSObject) webEngine.executeScript("window");
                    window.call("mostrarError", "Error al cargar productos del servidor");
                }
            }
        });

        stage.setTitle("Carrito de Compras - Tienda en Línea");
        stage.setScene(new Scene(webView, 900, 700));
        stage.show();
    }
    
    /**
     * Convierte un JSONObject a un objeto Articulo
     */
    private Articulo jsonToArticulo(JSONObject json) {
        String categoria = (String) json.get("categoria");
        String nombre = (String) json.get("nombre");
        String marca = (String) json.get("marca");
        String descripcion = (String) json.get("descripcion");
        
        // Manejar precio como Number para evitar errores de casting
        Number precioNum = (Number) json.get("precio");
        double precio = precioNum.doubleValue();
        
        // Manejar cantidad como Number para evitar errores de casting
        Number cantidadNum = (Number) json.get("cantidad");
        int cantidad = cantidadNum.intValue();
        
        return new Articulo(categoria, nombre, marca, descripcion, precio, cantidad);
    }
    
    public Carrito getCarrito() {
        return carrito;
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}