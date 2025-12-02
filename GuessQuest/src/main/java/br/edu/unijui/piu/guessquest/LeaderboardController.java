package br.edu.unijui.piu.guessquest;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.IOException;
import java.util.List;

public class LeaderboardController {

    @FXML
    private VBox scoreBox; 

    @FXML
    public void initialize() {
        loadLeaderboard();
    }

    /**
     * Carrega os scores do LeaderboardManager e cria as linhas animadas.
     */
    private void loadLeaderboard() {
        // Carrega dados e debuga quantidade
        List<LeaderboardManager.ScoreEntry> scores = LeaderboardManager.loadScores();
        System.out.println("LeaderboardController recebeu " + scores.size() + " registros.");

        scoreBox.getChildren().clear();

        int index = 0;

        for (LeaderboardManager.ScoreEntry entry : scores) {
            HBox line = new HBox(40);
            line.setAlignment(Pos.CENTER);
            line.getStyleClass().add("leaderboard-row");
            
            // Definido o estado visual inicial para animação
            line.setOpacity(0); 
            line.setTranslateY(20);

            String rankClass = (index == 0) ? "rank-1" : (index == 1) ? "rank-2" : (index == 2) ? "rank-3" : "rank-others";

            Label name = new Label(entry.name);
            name.getStyleClass().addAll("rank-name", rankClass);

            Label value = new Label(String.format("%06d", entry.score));
            value.getStyleClass().addAll("rank-score", rankClass);

            if (index == 0) {
                Label crown = new Label("👑");
                crown.setStyle("-fx-font-size: 36px; -fx-text-fill: gold;");
                line.getChildren().addAll(crown, name, value);
            } else {
                line.getChildren().addAll(name, value);
            }

            scoreBox.getChildren().add(line);
            
            // Animação Java de entrada
            animateEntry(line, index * 0.15); // 0.15s de delay por item

            index++;
        }

        if (scores.isEmpty()) {
            Label empty = new Label("SEM REGISTROS");
            empty.getStyleClass().add("label-retro");
            empty.setStyle("-fx-font-size: 20px; -fx-text-fill: #888;");
            scoreBox.getChildren().add(empty);
        }
    }

    // Método auxiliar para criar a animação de entrada
    private void animateEntry(javafx.scene.Node node, double delaySeconds) {
        // Animação de Opacidade (aparecer)
        FadeTransition fade = new FadeTransition(Duration.seconds(0.5), node);
        fade.setFromValue(0);
        fade.setToValue(1);

        // Animação de Posição (subir um pouco)
        TranslateTransition translate = new TranslateTransition(Duration.seconds(0.5), node);
        translate.setFromY(20);
        translate.setToY(0);

        // Roda as duas juntas
        ParallelTransition parallel = new ParallelTransition(fade, translate);
        parallel.setDelay(Duration.seconds(delaySeconds));
        parallel.play();
    }

    @FXML
    public void goBack() throws IOException {
        
        // Toca som de seleção para feedback ao usuário
        SoundManager.getInstance().playSound("select.wav");

        App.setRoot("primary");
    }
}