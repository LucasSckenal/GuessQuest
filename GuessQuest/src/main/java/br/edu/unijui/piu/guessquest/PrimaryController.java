package br.edu.unijui.piu.guessquest;

import java.io.IOException;
import java.util.List;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.Region;

public class PrimaryController {

    @FXML private TextField nameField;
    @FXML private ToggleGroup difficultyGroup;
    @FXML private RadioButton radioNormal;
    @FXML private RadioButton radioHard;
    @FXML private RadioButton radioSouls;

    @FXML
    public void initialize() {
        // Limita o campo de nome a 4 caracteres (estilo arcade: AAA)
        nameField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.length() > 4) {
                nameField.setText(oldValue);
            }
            // Força caixa alta
            nameField.setText(nameField.getText().toUpperCase());
        });
    }

    @FXML
    private void startGame() throws IOException {
        String name = nameField.getText();
        
        if (name.isEmpty()) {
            showAlert("ATENÇÃO", "INSIRA SEU NOME (MAX 4 CARACTERES)");
            return;
        }

        // Configura o estado do jogo
        GameState state = GameState.getInstance();
        state.setPlayerName(name);

        if (radioNormal.isSelected()) state.setDifficulty(GameState.Difficulty.NORMAL);
        else if (radioHard.isSelected()) state.setDifficulty(GameState.Difficulty.HARD);
        else if (radioSouls.isSelected()) state.setDifficulty(GameState.Difficulty.SOULS);

        state.resetGame();

        // Troca para a tela do jogo
        App.setRoot("secondary");
    }
    
    @FXML
    private void showLeaderboard() {
        List<LeaderboardManager.ScoreEntry> scores = LeaderboardManager.loadScores();
        StringBuilder sb = new StringBuilder();
        
        if (scores.isEmpty()) {
            sb.append("SEM RECORDES AINDA...\nSEJA O PRIMEIRO!");
        } else {
            sb.append("TOP 5 JOGADORES\n\n");
            for (int i = 0; i < scores.size(); i++) {
                sb.append(String.format("#%d  %s\n", i + 1, scores.get(i).toString()));
            }
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("HALL OF FAME");
        alert.setHeaderText("RECORDES LOCAIS");
        alert.setContentText(sb.toString());
        alert.getDialogPane().getStylesheets().add(getClass().getResource("styles.css").toExternalForm());
        alert.getDialogPane().getStyleClass().add("arcade-alert");
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alert.showAndWait();
    }

    @FXML
    private void exitApp() {
        Platform.exit();
        System.exit(0);
    }

    private void showAlert(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("SYSTEM ALERT");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.getDialogPane().getStylesheets().add(getClass().getResource("styles.css").toExternalForm());
        alert.showAndWait();
    }
}