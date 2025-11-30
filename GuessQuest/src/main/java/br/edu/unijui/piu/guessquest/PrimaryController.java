package br.edu.unijui.piu.guessquest;

import java.io.IOException;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.Region;

public class PrimaryController {

    @FXML
    private TextField nameField;
    @FXML
    private ToggleGroup difficultyGroup;
    @FXML
    private RadioButton radioNormal;
    @FXML
    private RadioButton radioHard;
    @FXML
    private RadioButton radioSouls;

    @FXML
    private Slider volumeMusicSlider;
    @FXML
    private Slider volumeSfxSlider;

    @FXML
    public void initialize() {
        // Limita nome a 4 letras e deixa tudo maiúsculo
        nameField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue.length() > 4) {
                nameField.setText(oldValue);
            } else {
                nameField.setText(newValue.toUpperCase());
            }
        });

        // Config Inicial dos Sliders
        SoundManager sound = SoundManager.getInstance();
        volumeMusicSlider.setValue(sound.getMusicVolume() * 100);
        volumeSfxSlider.setValue(sound.getSfxVolume() * 100);

        volumeMusicSlider.valueProperty()
                .addListener((obs, oldVal, newVal) -> sound.setMusicVolume(newVal.doubleValue() / 100.0));

        volumeSfxSlider.valueProperty()
                .addListener((obs, oldVal, newVal) -> sound.setSfxVolume(newVal.doubleValue() / 100.0));
    }

    // -------------------------------
    // INICIAR O JOGO
    // -------------------------------
    @FXML
    private void startGame() throws IOException {
        String name = nameField.getText();

        if (name.isEmpty()) {
            showAlert("ATENÇÃO", "INSIRA SEU NOME (MAX 4 CARACTERES)");
            return;
        }

        GameState state = GameState.getInstance();
        state.setPlayerName(name);

        if (radioNormal.isSelected())
            state.setDifficulty(GameState.Difficulty.NORMAL);
        else if (radioHard.isSelected())
            state.setDifficulty(GameState.Difficulty.HARD);
        else if (radioSouls.isSelected())
            state.setDifficulty(GameState.Difficulty.SOULS);

        state.resetGame();

        SoundManager.getInstance().playSound("correct.mp3");

        // Mantém padrão do seu projeto:
        App.setRoot("secondary");
    }

    // -------------------------------
    // ABRIR TELA DE LEADERBOARD
    // -------------------------------
    @FXML
    private void showLeaderboard() {
        try {
            SoundManager.getInstance().playSound("click.mp3"); 

            // Troca tela usando seu App.setRoot()
            App.setRoot("leaderboard");

        } catch (Exception e) {
            // DIAGNÓSTICO: Imprime o erro no console e mostra no alerta
            e.printStackTrace();
            
            String causa = e.getCause() != null ? e.getCause().toString() : e.getMessage();
            showAlert("ERRO CRÍTICO", "FALHA AO ABRIR RANKING:\n" + causa);
        }
    }

    // -------------------------------
    // SAIR DO JOGO
    // -------------------------------
    @FXML
    private void exitApp() {
        Platform.exit();
        System.exit(0);
    }

    // -------------------------------
    // ALERTA PERSONALIZADO
    // -------------------------------
    private void showAlert(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("SYSTEM ALERT");
        alert.setHeaderText(header);
        alert.setContentText(content);

        alert.getDialogPane().getStylesheets().add(
                getClass().getResource("styles.css").toExternalForm());
        // Garante que o alerta expanda para mostrar a mensagem de erro completa
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alert.showAndWait();
    }
}