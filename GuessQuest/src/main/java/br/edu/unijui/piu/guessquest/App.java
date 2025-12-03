package br.edu.unijui.piu.guessquest;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

import java.io.IOException;

public class App extends Application {

    private static Scene scene;
    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage;
        // Define o tamanho inicial da janela
        scene = new Scene(loadFXML("primary"), 1024, 768); 
        scene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());
        
        // Ativa o CSS responsivo
        setupResponsiveCSS(scene);

        // Lógica de Fullscreen com F11
        scene.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (KeyCode.F11 == event.getCode()) {
                stage.setFullScreen(!stage.isFullScreen());
            }
        });

        stage.setTitle("Guess Quest");
        stage.setScene(scene);
        stage.setResizable(true);
        stage.show();

        // Inicia a música de fundo
        SoundManager.getInstance().playMusic("bgm.mp3");
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

    // =================================================================================
    // MÉTODOS PARA CSS RESPONSIVO (DETECÇÃO DE TELA GRANDE)
    // =================================================================================

    // Configura o CSS responsivo para a cena fornecida
    private void setupResponsiveCSS(Scene scene) {

        // Adiciona listeners que ouvem quando a janela muda de tamanho (Largura ou Altura)
        scene.widthProperty().addListener((o, oldV, newV) -> applyResponsiveClass(scene));
        scene.heightProperty().addListener((o, oldV, newV) -> applyResponsiveClass(scene));

        // Adiciona listener para quando a tela muda (Ex: foi de Primary para Secondary)
        scene.rootProperty().addListener((o, oldRoot, newRoot) -> applyResponsiveClass(scene));
        
        // Aplica a classe responsiva na inicialização
        applyResponsiveClass(scene);
    }

    private void applyResponsiveClass(Scene scene) {
        if (scene.getRoot() == null) return;

        // Definido como "tela grande" se largura >=1800 e altura >=900 (o mínimo necessário para não ter clipping com o overlay)
        boolean isBigScreen = scene.getWidth() >= 1800 && scene.getHeight() >= 900;

        // Lógica para adicionar/remover a classe 'desktop-mode' conforme o tamanho da tela
        if (isBigScreen) {
            if (!scene.getRoot().getStyleClass().contains("desktop-mode")) {
                scene.getRoot().getStyleClass().add("desktop-mode");
            }
        } else {
            scene.getRoot().getStyleClass().remove("desktop-mode");
        }
    }
}