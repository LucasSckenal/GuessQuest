package br.edu.unijui.piu.guessquest;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.IOException;
import java.util.List;

public class LeaderboardController {

    @FXML
    private VBox scoreBox;

    @FXML
    private Label lblTopPlayers;

    @FXML
    private Button btnBack;

    @FXML
    public void initialize() {
        boolean isPT = GameState.getInstance().getLanguage() == GameState.Language.PT;
        lblTopPlayers.setText(isPT ? "TOP 5 JOGADORES" : "TOP 5 PLAYERS");
        btnBack.setText(isPT ? "< VOLTAR" : "< BACK");

        loadLeaderboard();
    }

    private void loadLeaderboard() {
        List<LeaderboardManager.ScoreEntry> scores = LeaderboardManager.loadScores();

        scoreBox.getChildren().clear();

        int index = 0;

        for (LeaderboardManager.ScoreEntry entry : scores) {
            HBox line = new HBox(40);
            line.setAlignment(Pos.CENTER);
            line.getStyleClass().add("leaderboard-row");

            line.setOpacity(0);
            line.setTranslateY(20);

            String rankClass = (index == 0) ? "rank-1"
                    : (index == 1) ? "rank-2" : (index == 2) ? "rank-3" : "rank-others";

            Label name = new Label(entry.name);
            name.getStyleClass().addAll("rank-name", rankClass);

            Label value = new Label(String.format("%06d", entry.score));
            value.getStyleClass().addAll("rank-score", rankClass);

            if (index == 0) {
                // --- AJUSTE DE ALINHAMENTO VISUAL ---

                Label crown = new Label("👑");
               
                crown.setStyle("-fx-font-size: 28px; -fx-text-fill: gold; -fx-padding: -10 5 0 0;"); 

                GridPane nameContainer = new GridPane();
                nameContainer.setAlignment(Pos.CENTER);                

                nameContainer.add(crown, 0, 0);                
                nameContainer.add(name, 1, 0);

                // Força o alinhamento vertical das células para o CENTRO
                GridPane.setValignment(crown, javafx.geometry.VPos.CENTER); 
                GridPane.setValignment(name, javafx.geometry.VPos.CENTER); 

                line.getChildren().addAll(nameContainer, value);                

                // -----------------------------------------------------------
            } else {
                line.getChildren().addAll(name, value);
            }

            scoreBox.getChildren().add(line);

            animateEntry(line, index * 0.15);

            index++;
            if (index >= 5)
                break;
        }

        if (scores.isEmpty()) {
            boolean isPT = GameState.getInstance().getLanguage() == GameState.Language.PT;
            Label empty = new Label(isPT ? "SEM REGISTROS" : "NO RECORDS");
            empty.getStyleClass().add("label-retro");
            empty.setStyle("-fx-font-size: 20px; -fx-text-fill: #888;");
            scoreBox.getChildren().add(empty);
        }
    }

    private void animateEntry(javafx.scene.Node node, double delaySeconds) {
        FadeTransition fade = new FadeTransition(Duration.seconds(0.5), node);
        fade.setFromValue(0);
        fade.setToValue(1);

        TranslateTransition translate = new TranslateTransition(Duration.seconds(0.5), node);
        translate.setFromY(20);
        translate.setToY(0);

        ParallelTransition parallel = new ParallelTransition(fade, translate);
        parallel.setDelay(Duration.seconds(delaySeconds));
        parallel.play();
    }

    @FXML
    public void goBack() throws IOException {
        SoundManager.getInstance().playSound("select.wav");
        App.setRoot("primary");
    }
}