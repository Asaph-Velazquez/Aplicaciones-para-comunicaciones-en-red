package tienda;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

public class CarritoGUI extends Application {
    
    @Override
    public void start(Stage stage) {
        WebView webView = new WebView();
        WebEngine webEngine = webView.getEngine();
        webEngine.load(getClass().getResource("/ui/carrito.html").toExternalForm());

        stage.setTitle("Carrito de Compras - Tienda en Línea");
        stage.setScene(new Scene(webView, 900, 700));
        stage.show();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}