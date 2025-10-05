package tienda;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import netscape.javascript.JSObject;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.ArrayList;

import org.json.simple.*;
import org.json.simple.parser.JSONParser;

public class Cliente extends Application {
    private WebEngine engine; // Variable de instancia para acceder desde procesarCompra

    public static List<JSONObject> obtenerProductos() throws Exception{
        List<JSONObject> productos = new ArrayList<>();

        Socket cliente = new Socket("localhost", 1234);
        PrintWriter escritor = new PrintWriter(cliente.getOutputStream(), true);
        BufferedReader buffer = new BufferedReader(new InputStreamReader(cliente.getInputStream(), StandardCharsets.UTF_8));

        // Enviar solicitud de productos
        escritor.println("OBTENER_PRODUCTOS");

        JSONParser parser = new JSONParser();
        String mensaje;
        while ((mensaje = buffer.readLine()) != null && !"FIN_PRODUCTOS".equals(mensaje)) {
            JSONObject json = (JSONObject) parser.parse(mensaje);
            productos.add(json);
        }
        
        escritor.close();
        buffer.close();
        cliente.close();
        return productos;
    }

    @Override
    public void start(Stage stage) {
        WebView view = new WebView();
        this.engine = view.getEngine();
        engine.load(getClass().getResource("/ui/index.html").toExternalForm());

        // Cuando cargue la página, conectamos el socket y mandamos el JSON
        engine.getLoadWorker().stateProperty().addListener((obs, o, n) -> {
            if (n == javafx.concurrent.Worker.State.SUCCEEDED) {
                try {
                    JSObject window = (JSObject) engine.executeScript("window");
                    window.setMember("clienteJava", this);
                    window.call("limpiarProductos");
                    
                    for (JSONObject producto : obtenerProductos()) {
                        window.call("agregarProducto", producto.toJSONString());
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }
        });

        stage.setScene(new Scene(view, 800, 600));
        stage.show();
    }

    // Método que será llamado desde JavaScript para obtener productos
    public List<JSONObject> obtenerProductosParaJS(){
        try {
            return obtenerProductos();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    // Método que será llamado desde JavaScript
    public void procesarCompra(String carritoJSON){
        try {
            System.out.println("Procesando compra con carrito: " + carritoJSON);
            Carrito carrito = new Carrito(carritoJSON);
            
            boolean compraExitosa = carrito.enviarAlServidor();
            
            JSObject window = (JSObject) engine.executeScript("window");
            if (compraExitosa) {
                window.call("alert", "¡Compra procesada exitosamente! Total: $" + String.format("%.2f", carrito.getTotal()) + "\nEl stock de los productos ha sido actualizado.");
                // Recargar productos para mostrar stock actualizado
                window.call("recargarProductosDesdeServidor");
            } else {
                window.call("alert", "Error: No se pudo procesar la compra.\nPosibles causas:\n- Stock insuficiente de algún producto\n- Producto no encontrado\n\nInténtalo de nuevo.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                JSObject window = (JSObject) engine.executeScript("window");
                window.call("alert", "Error al procesar la compra: " + e.getMessage());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
