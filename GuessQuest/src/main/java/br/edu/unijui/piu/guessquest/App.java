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
 * Responsável por iniciar o Stage principal, carregar a cena inicial,
 * configurar estilos globais e gerenciar eventos de janela (como Fullscreen e CSS responsivo).
 */
public class App extends Application {

    private static Scene scene;
    private static Stage primaryStage;

    /**
     * Ponto de entrada da aplicação JavaFX.
     */
    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage;
        
        // Define o tamanho inicial da janela e carrega a view principal
        scene = new Scene(loadFXML("primary"), 1024, 768); 
        scene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());
        
        // Inicializa o sistema de CSS responsivo para overlay
        setupResponsiveCSS(scene);

        // Configura atalho global F11 para alternar tela cheia
        scene.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (KeyCode.F11 == event.getCode()) {
                stage.setFullScreen(!stage.isFullScreen());
            }
        });

        stage.setTitle("Guess Quest");
        stage.setScene(scene);
        stage.setResizable(true);
        stage.show();

        // Inicia a reprodução da música de fundo (BGM)
        SoundManager.getInstance().playMusic("bgm.mp3");
    }

    /**
     * Substitui o conteúdo raiz da cena atual por um novo FXML.
     * Utilizado para navegação entre telas maiores (ex: Menu -> Jogo).
     * * @param fxml Nome do arquivo FXML (sem extensão) a ser carregado.
     */
    public static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    /**
     * Carrega a hierarquia de objetos de um arquivo FXML.
     */
    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }

    public static void main(String[] args) {
        launch();
    }

    // =================================================================================
    // GERENCIAMENTO DE CSS RESPONSIVO (DETECÇÃO DE MODO DESKTOP/ARCADE)
    // =================================================================================

    /**
     * Configura listeners para detectar mudanças no tamanho da janela e adaptar o CSS.
     * Adiciona a classe 'desktop-mode' quando a tela é grande o suficiente para mostrar o gabinete arcade.
     */
    private void setupResponsiveCSS(Scene scene) {

        // Adiciona listeners para redimensionamento (Largura e Altura)
        scene.widthProperty().addListener((o, oldV, newV) -> applyResponsiveClass(scene));
        scene.heightProperty().addListener((o, oldV, newV) -> applyResponsiveClass(scene));

        // Adiciona listener para quando a raiz da cena mudar (navegação entre telas)
        // para garantir que a classe seja reaplicada na nova view
        scene.rootProperty().addListener((o, oldRoot, newRoot) -> applyResponsiveClass(scene));
        
        // Aplicação inicial
        applyResponsiveClass(scene);
    }

    /**
     * Aplica ou remove a classe CSS 'desktop-mode' baseada nas dimensões atuais.
     */
    private void applyResponsiveClass(Scene scene) {
        if (scene.getRoot() == null) return;

        // Critério: Largura >= 1800 e Altura >= 900 para evitar clipping do overlay gráfico
        boolean isBigScreen = scene.getWidth() >= 1800 && scene.getHeight() >= 900;

        if (isBigScreen) {
            if (!scene.getRoot().getStyleClass().contains("desktop-mode")) {
                scene.getRoot().getStyleClass().add("desktop-mode");
            }
        } else {
            scene.getRoot().getStyleClass().remove("desktop-mode");
        }
    }
}