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
import org.json.simple.*;
import org.json.simple.parser.JSONParser;

public class Cliente extends Application {

    @Override
    public void start(Stage stage) {
        WebView view = new WebView();
        WebEngine engine = view.getEngine();
        engine.load(getClass().getResource("/ui/index.html").toExternalForm());

        // Cuando cargue la página, conectamos el socket y mandamos el JSON
        engine.getLoadWorker().stateProperty().addListener((obs, o, n) -> {
            if (n == javafx.concurrent.Worker.State.SUCCEEDED) {
                try {
                    Socket cliente = new Socket("localhost", 1234);
                    BufferedReader buffer = new BufferedReader(
                        new InputStreamReader(cliente.getInputStream(), StandardCharsets.UTF_8)
                    );
                    String mensaje = buffer.readLine();
                    JSONParser parser = new JSONParser();
                    JSONObject json = (JSONObject) parser.parse(mensaje);

                    // Pasar JSON a la página
                    JSObject window = (JSObject) engine.executeScript("window");
                    window.call("mostrarProducto", json.toJSONString());

                    buffer.close();
                    cliente.close();
                } catch (Exception e) { e.printStackTrace(); }
            }
        });

        stage.setScene(new Scene(view, 800, 600));
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
