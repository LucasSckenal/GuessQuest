package br.edu.unijui.piu.guessquest;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Classe principal da aplicação JavaFX.
 */
public class App extends Application {

    private static Scene scene;
    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage;
        // Tamanho inicial razoável, mas redimensionável
        scene = new Scene(loadFXML("primary"), 1024, 768); 
        scene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());
        
        // Atalho opcional para alternar fullscreen manualmente (F11)
        scene.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (KeyCode.F11 == event.getCode()) {
                stage.setFullScreen(!stage.isFullScreen());
            }
        });

        stage.setTitle("Guess Quest Arcade");
        stage.setScene(scene);
        stage.setResizable(true); // Permite redimensionar/maximizar
        stage.show();
    }

    public static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }

    public static void main(String[] args) {
        launch();
    }
}