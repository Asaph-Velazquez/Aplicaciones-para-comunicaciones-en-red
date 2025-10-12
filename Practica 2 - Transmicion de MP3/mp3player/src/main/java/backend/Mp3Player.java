package backend;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javafx.application.Application;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Mp3Player extends Application{
    private MediaPlayer mediaPlayer;

    @Override
    public void start(Stage primaryStage) {
        
        //Rutas del archivo mp3 (usando getResource para resources)
        String path;
        var resource = getClass().getClassLoader().getResource("song/cancion.mp3");
        if (resource != null) {
            path = resource.toString();
        } else {
            System.err.println("No se encontró cancion.mp3 en src/main/resources/song");
            System.err.println("Coloca el archivo MP3 en: src/main/resources/song/cancion.mp3");
            return;
        }
        
        Media media = new Media(path);

        //creamos el media player
        mediaPlayer = new MediaPlayer(media);

        //botones de control
        Button playButton = new Button("Play");
        Button pauseButton = new Button("Pause");
        Button stopButton = new Button("Stop");

        playButton.setOnAction(e -> mediaPlayer.play());
        pauseButton.setOnAction(e -> mediaPlayer.pause());
        stopButton.setOnAction(e -> mediaPlayer.stop());

        //layout
        HBox root = new HBox(10, playButton, pauseButton, stopButton);
        Scene scene = new Scene(root, 300, 100);

        primaryStage.setTitle("Simple MP3 Player");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public byte[] leerArchivoMP3(String ruta){
        try{
            File file = new File(ruta);
            FileInputStream fis = new FileInputStream(file);
            byte[] data = fis.readAllBytes();
            return data;
        }catch(IOException e){
            System.err.println("Error al leer el archivo MP3: " + e.getMessage());
            return null;
        }
    }
    
    public byte[][] fragmentarMP3(byte[] data, int tamPackage){
        if (data == null || tamPackage <= 0) return null;

        int totalFragments = (int) Math.ceil((double) data.length / tamPackage);
        byte[][] fragments = new byte[totalFragments][];

        for (int i = 0; i < totalFragments; i++) {
            int start = i * tamPackage;
            int end = Math.min(start + tamPackage, data.length);
            int length = end - start;

            fragments[i] = new byte[length];
            System.arraycopy(data, start, fragments[i], 0, length);
        }

        return fragments;
    }


    public static void main(String[] args) {
        launch(args);
    }
}
