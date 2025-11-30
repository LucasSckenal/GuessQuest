package br.edu.unijui.piu.guessquest;

import java.io.IOException;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
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

    // Sliders e Ícones de Volume
    @FXML private Slider volumeMusicSlider;
    @FXML private Slider volumeSfxSlider;
    @FXML private Label btnMusicIcon;
    @FXML private Label btnSfxIcon;

    // Armazena o volume anterior para poder "desmutar"
    private double lastMusicVol = 50;
    private double lastSfxVol = 50;

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

        // --- Configuração de Áudio ---
        SoundManager sound = SoundManager.getInstance();
        
        // Carrega valores atuais
        volumeMusicSlider.setValue(sound.getMusicVolume() * 100);
        volumeSfxSlider.setValue(sound.getSfxVolume() * 100);
        
        // Listener Música
        volumeMusicSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            double val = newVal.doubleValue();
            sound.setMusicVolume(val / 100.0);
            updateIconStyle(btnMusicIcon, val);
        });

        // Listener SFX
        volumeSfxSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            double val = newVal.doubleValue();
            sound.setSfxVolume(val / 100.0);
            updateIconStyle(btnSfxIcon, val);
        });

        // Atualiza visual inicial dos ícones
        updateIconStyle(btnMusicIcon, volumeMusicSlider.getValue());
        updateIconStyle(btnSfxIcon, volumeSfxSlider.getValue());
    }

    // Lógica para alternar Mudo/Som ao clicar no ícone
    @FXML
    private void toggleMusic() {
        double current = volumeMusicSlider.getValue();
        if (current > 0) {
            lastMusicVol = current; // Salva para restaurar depois
            volumeMusicSlider.setValue(0);
        } else {
            // Restaura o último volume ou vai para 50 se for zero
            volumeMusicSlider.setValue(lastMusicVol > 0 ? lastMusicVol : 50);
        }
    }

    @FXML
    private void toggleSfx() {
        double current = volumeSfxSlider.getValue();
        if (current > 0) {
            lastSfxVol = current;
            volumeSfxSlider.setValue(0);
        } else {
            volumeSfxSlider.setValue(lastSfxVol > 0 ? lastSfxVol : 50);
        }
    }

    // Muda a cor do ícone se estiver mudo
    private void updateIconStyle(Label icon, double volume) {
        if (volume <= 0) {
            if (!icon.getStyleClass().contains("icon-muted")) {
                icon.getStyleClass().add("icon-muted");
            }
        } else {
            icon.getStyleClass().remove("icon-muted");
        }
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

        SoundManager.getInstance().playSound("correct.wav");

        App.setRoot("secondary");
    }

    // -------------------------------
    // ABRIR TELA DE LEADERBOARD
    // -------------------------------
    @FXML
    private void showLeaderboard() {
        try {
            SoundManager.getInstance().playSound("click.wav"); 
            App.setRoot("leaderboard");
        } catch (Exception e) {
            e.printStackTrace();
            String causa = e.getCause() != null ? e.getCause().toString() : e.getMessage();
            showAlert("ERRO CRÍTICO", "FALHA AO ABRIR RANKING:\n" + causa);
        }
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
        alert.getDialogPane().getStylesheets().add(
                getClass().getResource("styles.css").toExternalForm());
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alert.showAndWait();
    }
}