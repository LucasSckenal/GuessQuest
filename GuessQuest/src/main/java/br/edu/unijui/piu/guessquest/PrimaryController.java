package br.edu.unijui.piu.guessquest;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;

import java.io.IOException;

public class PrimaryController {

    @FXML private TextField char1;
    @FXML private TextField char2;
    @FXML private TextField char3;
    @FXML private TextField char4;
    
    @FXML private Label leaderboardLabel;
    @FXML private Button btnNormal;
    @FXML private Button btnHard;
    @FXML private Button btnMage;

    @FXML
    public void initialize() {
        setupCharInput(char1, char2);
        setupCharInput(char2, char3);
        setupCharInput(char3, char4);
        setupCharInput(char4, null); // Último não foca próximo

        updateLeaderboard();
        selectDifficulty(App.Difficulty.NORMAL);
    }

    private void setupCharInput(TextField current, TextField next) {
        // Formatter para aceitar apenas 1 caractere maiúsculo
        current.setTextFormatter(new TextFormatter<>((change) -> {
            change.setText(change.getText().toUpperCase());
            if (change.getControlNewText().length() > 1) {
                return null;
            }
            return change;
        }));

        // Pula para o próximo ao digitar
        current.textProperty().addListener((obs, oldV, newV) -> {
            if (newV.length() == 1 && next != null) {
                next.requestFocus();
            }
        });
    }

    private void updateLeaderboard() {
        StringBuilder sb = new StringBuilder();
        if (App.leaderboard.isEmpty()) {
            sb.append("NO RECORDS YET");
        } else {
            for (int i = 0; i < App.leaderboard.size(); i++) {
                App.ScoreEntry entry = App.leaderboard.get(i);
                sb.append(String.format("%d. %s - %04d\n", i + 1, entry.name, entry.score));
            }
        }
        leaderboardLabel.setText(sb.toString());
    }

    @FXML
    private void onNormal() { selectDifficulty(App.Difficulty.NORMAL); }
    
    @FXML
    private void onHard() { selectDifficulty(App.Difficulty.HARD); }
    
    @FXML
    private void onMage() { selectDifficulty(App.Difficulty.MAGE); }

    private void selectDifficulty(App.Difficulty diff) {
        App.currentDifficulty = diff;
        
        String selectedStyle = "-fx-background-color: transparent; -fx-text-fill: white; -fx-border-color: white; -fx-border-width: 2;";
        String defaultStyle = "-fx-background-color: transparent; -fx-text-fill: gray; -fx-border-color: #444; -fx-border-width: 2;";

        // Reseta todos
        btnNormal.setStyle(defaultStyle);
        btnHard.setStyle(defaultStyle);
        btnMage.setStyle(defaultStyle);

        // Aplica estilo no selecionado
        if (diff == App.Difficulty.NORMAL) btnNormal.setStyle(selectedStyle + "-fx-border-color: #4CAF50;");
        if (diff == App.Difficulty.HARD) btnHard.setStyle(selectedStyle + "-fx-border-color: #ffaa00;");
        if (diff == App.Difficulty.MAGE) btnMage.setStyle(selectedStyle + "-fx-border-color: #ff4444;");
    }

    @FXML
    private void startGame() throws IOException {
        // Junta os caracteres
        String name = char1.getText() + char2.getText() + char3.getText() + char4.getText();
        if (name.trim().isEmpty()) {
            name = "GUES";
        }
        App.playerName = name;
        App.setRoot("secondary");
    }
}