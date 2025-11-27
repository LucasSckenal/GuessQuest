package br.edu.unijui.piu.guessquest;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.IOException;

public class App extends Application {

    private static Scene scene;

    @Override
    public void start(Stage stage) throws IOException {
        // 1. Carregar a Fonte Retrô globalmente
        // O caminho deve bater com sua pasta resources: /fonts/Ithaca-LVB75.ttf
        try {
            Font retroFont = Font.loadFont(getClass().getResourceAsStream("/fonts/Ithaca.ttf"), 12);
            if (retroFont != null) {
                System.out.println("Fonte carregada: " + retroFont.getFamily());
            } else {
                System.err.println("Não foi possível carregar a fonte. Verifique o caminho /fonts/Ithaca-LVB75.ttf");
            }
        } catch (Exception e) {
            System.err.println("Erro ao carregar fonte: " + e.getMessage());
        }

        // 2. Carregar a tela inicial (primary)
        scene = new Scene(loadFXML("primary"), 700, 600);
        stage.setScene(scene);
        stage.setTitle("GuessQuest - Retro Edition");
        stage.setResizable(false);
        stage.show();
    }

    // Método estático para trocar de tela facilmente de qualquer Controller
    static void setRoot(String fxml) throws IOException {
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