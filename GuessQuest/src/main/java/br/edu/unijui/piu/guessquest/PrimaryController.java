package br.edu.unijui.piu.guessquest;

import java.io.IOException;
import javafx.animation.FadeTransition;
import javafx.animation.Animation;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class PrimaryController {

    // --- TELAS (CONTAINERS) ---
    @FXML
    private VBox screenNameInput;
    @FXML
    private VBox screenDifficulty;
    @FXML
    private VBox screenSettings;
    @FXML
    private VBox bottomMenu;

    // --- TELA 1: NOME ---
    @FXML
    private TextField nameField;
    @FXML
    private Label slot1, slot2, slot3, slot4;
    private FadeTransition cursorBlink;

    // Buffer para o cheat code
    private StringBuilder cheatBuffer = new StringBuilder();

    // --- TELA 2: DIFICULDADE ---
    @FXML
    private ToggleGroup difficultyGroup;
    @FXML
    private ToggleButton tglNormal, tglHard, tglSouls;

    // Botão secreto
    @FXML
    private ToggleButton tglInfinity;

    // REMOVIDO: private FadeTransition activeDifficultyBlink; (Não vamos mais
    // piscar)

    // --- TELA 3: SETTINGS ---
    @FXML
    private Slider volumeMusicSlider;
    @FXML
    private Slider volumeSfxSlider;

    // --- EXTRAS ---
    @FXML
    private Label blinkLabel;

    @FXML
    public void initialize() {
        setupAudio();

        // Efeito "INSERT COIN" piscando (Esse mantive pois é estético do título)
        FadeTransition ft = new FadeTransition(Duration.seconds(0.8), blinkLabel);
        ft.setFromValue(1.0);
        ft.setToValue(0.1);
        ft.setCycleCount(Animation.INDEFINITE);
        ft.setAutoReverse(true);
        ft.play();

        setupNameInput();
        setupCheatCode();
    }

    private void setupNameInput() {
        nameField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.length() > 4) {
                nameField.setText(oldVal);
                return;
            }
            updateNameSlots(newVal.toUpperCase());
        });

        Platform.runLater(() -> {
            nameField.requestFocus();
            blinkSlot(slot1);
        });
    }

    // =========================================================================
    // LÓGICA DO EASTER EGG (ABACATE)
    // =========================================================================
    private void setupCheatCode() {
        nameField.setOnKeyPressed((KeyEvent event) -> {
            String key = event.getText().toUpperCase();

            if (key.matches("[A-Z]")) {
                cheatBuffer.append(key);
                if (cheatBuffer.length() > 7) {
                    cheatBuffer.deleteCharAt(0);
                }
                if (cheatBuffer.toString().equals("ABACATE")) {
                    unlockInfinityMode();
                }
            }
        });
    }

    private void unlockInfinityMode() {
        SoundManager.getInstance().playSound("correct.wav");

        tglInfinity.setVisible(true);
        tglInfinity.setManaged(true);

        blinkLabel.setText("INFINITY UNLOCKED!");
        blinkLabel.setStyle("-fx-text-fill: #ffd700; -fx-effect: dropshadow(gaussian, #ffd700, 10, 0.8, 0, 0);");

        cheatBuffer.setLength(0);
    }
    // =========================================================================

    private void updateNameSlots(String text) {
        char[] chars = text.toCharArray();
        slot1.setText(chars.length > 0 ? String.valueOf(chars[0]) : "_");
        slot2.setText(chars.length > 1 ? String.valueOf(chars[1]) : "_");
        slot3.setText(chars.length > 2 ? String.valueOf(chars[2]) : "_");
        slot4.setText(chars.length > 3 ? String.valueOf(chars[3]) : "_");

        slot1.getStyleClass().remove("char-slot-filled");
        slot2.getStyleClass().remove("char-slot-filled");
        slot3.getStyleClass().remove("char-slot-filled");
        slot4.getStyleClass().remove("char-slot-filled");

        if (chars.length > 0)
            slot1.getStyleClass().add("char-slot-filled");
        if (chars.length > 1)
            slot2.getStyleClass().add("char-slot-filled");
        if (chars.length > 2)
            slot3.getStyleClass().add("char-slot-filled");
        if (chars.length > 3)
            slot4.getStyleClass().add("char-slot-filled");

        Node nextTarget = slot1;
        if (chars.length == 1)
            nextTarget = slot2;
        else if (chars.length == 2)
            nextTarget = slot3;
        else if (chars.length >= 3)
            nextTarget = slot4;

        blinkSlot(nextTarget);
    }

    private void blinkSlot(Node target) {
        stopCursorBlink();
        cursorBlink = new FadeTransition(Duration.seconds(0.4), target);
        cursorBlink.setFromValue(1.0);
        cursorBlink.setToValue(0.2);
        cursorBlink.setCycleCount(Animation.INDEFINITE);
        cursorBlink.setAutoReverse(true);
        cursorBlink.play();
    }

    private void stopCursorBlink() {
        if (cursorBlink != null) {
            cursorBlink.stop();
            cursorBlink.getNode().setOpacity(1.0);
        }
        slot1.setOpacity(1.0);
        slot2.setOpacity(1.0);
        slot3.setOpacity(1.0);
        slot4.setOpacity(1.0);
    }

    private void setupAudio() {
        SoundManager sound = SoundManager.getInstance();
        volumeMusicSlider.setValue(sound.getMusicVolume() * 100);
        volumeSfxSlider.setValue(sound.getSfxVolume() * 100);
        volumeMusicSlider.valueProperty()
                .addListener((o, oldV, newV) -> sound.setMusicVolume(newV.doubleValue() / 100.0));
        volumeSfxSlider.valueProperty().addListener((o, oldV, newV) -> sound.setSfxVolume(newV.doubleValue() / 100.0));
    }

    // --- NAVEGAÇÃO ---

    @FXML
    private void confirmName() {
        String name = nameField.getText().trim();
        if (name.isEmpty())
            return;

        GameState.getInstance().setPlayerName(name.toUpperCase());
        SoundManager.getInstance().playSound("select.wav");

        screenNameInput.setVisible(false);
        screenDifficulty.setVisible(true);

        // Removido: startDiffAnimation(tglNormal); - Não pisca mais
    }

    // NOVO: Ação do botão voltar
    @FXML
    private void backToNameInput() {
        SoundManager.getInstance().playSound("select.wav"); // Som de cancelamento/seleção
        screenDifficulty.setVisible(false);
        screenNameInput.setVisible(true);

        // Foca no nome novamente para continuar digitando
        Platform.runLater(() -> nameField.requestFocus());
    }

    @FXML
    private void startGame() throws IOException {
        GameState.Difficulty selectedDiff = GameState.Difficulty.NORMAL;

        if (tglHard.isSelected())
            selectedDiff = GameState.Difficulty.HARD;
        else if (tglSouls.isSelected())
            selectedDiff = GameState.Difficulty.SOULS;
        else if (tglInfinity.isSelected())
            selectedDiff = GameState.Difficulty.INFINITY;

        GameState.getInstance().setDifficulty(selectedDiff);
        GameState.getInstance().resetGame();

        SoundManager.getInstance().playSound("start.wav");

        App.setRoot("secondary");
    }

    @FXML
    private void goToSettings() {
        screenSettings.setVisible(true);
        screenSettings.toFront();
        bottomMenu.setVisible(false);
    }

    @FXML
    private void backToName() {
        screenSettings.setVisible(false);
        bottomMenu.setVisible(true);
    }

    @FXML
    private void showLeaderboard() {
        // App.setRoot("leaderboard");
    }

    @FXML
    private void exitApp() {
        Platform.exit();
        System.exit(0);
    }
}