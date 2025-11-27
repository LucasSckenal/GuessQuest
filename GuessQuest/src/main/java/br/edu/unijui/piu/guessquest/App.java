package br.edu.unijui.piu.guessquest;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class App extends Application {

    private static Scene scene;
    private static Stage stage;

    // --- ESTADO GLOBAL DO JOGO ---
    public enum Difficulty {
        NORMAL(10, 1.0),
        HARD(5, 1.25),
        MAGE(1, 1.5);

        public final int lives;
        public final double multiplier;

        Difficulty(int lives, double multiplier) {
            this.lives = lives;
            this.multiplier = multiplier;
        }
    }

    public static class ScoreEntry {
        public String name;
        public int score;

        public ScoreEntry(String name, int score) {
            this.name = name;
            this.score = score;
        }
    }

    public static String playerName = "PLAY";
    public static Difficulty currentDifficulty = Difficulty.NORMAL;
    public static List<ScoreEntry> leaderboard = new ArrayList<>();

    @Override
    public void start(Stage stage) throws IOException {
        App.stage = stage;
        
        // Carrega Fonte
        try {
            Font.loadFont(getClass().getResourceAsStream("/fonts/Ithaca.ttf"), 12);
        } catch (Exception e) {
            System.err.println("Fonte não encontrada.");
        }

        scene = new Scene(loadFXML("primary"), 800, 600);
        
        // Atalho para Fullscreen (F11)
        scene.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.F11) {
                stage.setFullScreen(!stage.isFullScreen());
            }
        });

        stage.setScene(scene);
        stage.setTitle("GuessQuest Arcade");
        stage.show();
    }

    static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }
    
    // Adiciona pontuação e ordena
    public static void addScore(int score) {
        leaderboard.add(new ScoreEntry(playerName, score));
        leaderboard.sort(Comparator.comparingInt((ScoreEntry s) -> s.score).reversed());
        if (leaderboard.size() > 5) leaderboard = leaderboard.subList(0, 5); // Mantém top 5
    }

    public static void main(String[] args) {
        launch();
    }
}