package br.edu.unijui.piu.guessquest;

import java.io.IOException;
import javafx.animation.FadeTransition;
import javafx.animation.Animation;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class PrimaryController {

    // --- TELAS (CONTAINERS) ---
    @FXML
    private VBox screenNameInput;
    @FXML
    private VBox screenDifficulty;
    @FXML
    private VBox screenSettings; // Nova tela
    @FXML
    private VBox bottomMenu; // Barra de botões inferior

    // --- TELA 1: NOME ---
    @FXML
    private TextField nameField;
    @FXML
    private Label slot1, slot2, slot3, slot4;
    private FadeTransition cursorBlink;

    // --- TELA 2: DIFICULDADE ---
    @FXML
    private ToggleGroup difficultyGroup;
    @FXML
    private ToggleButton tglNormal, tglHard, tglSouls;
    private FadeTransition activeDifficultyBlink;

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
        setupNameInput();
        setupAudio();
        setupDifficultyBlink();
        startArcadeAnimations();

        // Estado Inicial: Apenas Tela de Nome visível
        showScreen(screenNameInput);

        // Inicia piscando o primeiro slot
        blinkSlot(slot1);
    }

    // Método auxiliar para trocar de tela limpo
    private void showScreen(VBox screenToShow) {
        screenNameInput.setVisible(false);
        screenDifficulty.setVisible(false);
        screenSettings.setVisible(false);

        screenToShow.setVisible(true);

        // O menu de baixo (Rank/Options/Exit) só aparece na tela de nome
        bottomMenu.setVisible(screenToShow == screenNameInput);
    }

    // --- NAVEGAÇÃO ---

    @FXML
    private void goToDifficulty() {
        String name = nameField.getText();
        if (name.isEmpty()) {
            SoundManager.getInstance().playSound("error.wav");
            return;
        }
        stopCursorBlink();
        SoundManager.getInstance().playSound("click.wav");

        showScreen(screenDifficulty);
    }

    @FXML
    private void goToSettings() {
        SoundManager.getInstance().playSound("click.wav");
        stopCursorBlink(); // Para de piscar o cursor enquanto configura
        showScreen(screenSettings);
    }

    @FXML
    private void backToName() {
        SoundManager.getInstance().playSound("click.wav");
        showScreen(screenNameInput);

        // Retoma o foco e o pisca do nome
        Platform.runLater(() -> nameField.requestFocus());
        updateSlots(nameField.getText());
    }

    @FXML
    private void startGame() throws IOException {
        GameState state = GameState.getInstance();
        state.setPlayerName(nameField.getText());

        if (tglNormal.isSelected())
            state.setDifficulty(GameState.Difficulty.NORMAL);
        else if (tglHard.isSelected())
            state.setDifficulty(GameState.Difficulty.HARD);
        else
            state.setDifficulty(GameState.Difficulty.SOULS);

        state.resetGame();
        SoundManager.getInstance().playSound("coin.wav");
        App.setRoot("secondary");
    }

    @FXML
    private void showLeaderboard() throws IOException {
        SoundManager.getInstance().playSound("click.wav");
        App.setRoot("leaderboard");
    }

    @FXML
    private void exitApp() {
        Platform.exit();
        System.exit(0);
    }

    // --- (ABAIXO TUDO IGUAL: Lógica de Nome, Audio e Pisca) ---

    private void setupNameInput() {
        Platform.runLater(() -> nameField.requestFocus());
        nameField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.length() > 4) {
                nameField.setText(oldVal);
                return;
            }
            String text = newVal.toUpperCase();
            if (!text.equals(newVal))
                nameField.setText(text);
            updateSlots(text);
        });
    }

    private void updateSlots(String text) {
        setSlot(slot1, text, 0);
        setSlot(slot2, text, 1);
        setSlot(slot3, text, 2);
        setSlot(slot4, text, 3);

        int len = text.length();
        if (len == 0)
            blinkSlot(slot1);
        else if (len == 1)
            blinkSlot(slot2);
        else if (len == 2)
            blinkSlot(slot3);
        else if (len == 3)
            blinkSlot(slot4);
        else
            stopCursorBlink();
    }

    private void setSlot(Label slot, String text, int index) {
        if (text.length() > index) {
            slot.setText(String.valueOf(text.charAt(index)));
            if (!slot.getStyleClass().contains("char-slot-filled"))
                slot.getStyleClass().add("char-slot-filled");
        } else {
            slot.setText("");
            slot.getStyleClass().remove("char-slot-filled");
        }
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

    private void setupDifficultyBlink() {
        difficultyGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (oldVal != null)
                ((Node) oldVal).setOpacity(1.0);
            if (newVal != null) {
                startDiffBlink((Node) newVal);
                SoundManager.getInstance().playSound("click.wav");
            }
        });
        if (difficultyGroup.getSelectedToggle() != null)
            startDiffBlink((Node) difficultyGroup.getSelectedToggle());
    }

    private void startDiffBlink(Node node) {
        if (activeDifficultyBlink != null)
            activeDifficultyBlink.stop();
        activeDifficultyBlink = new FadeTransition(Duration.seconds(0.15), node);
        activeDifficultyBlink.setFromValue(1.0);
        activeDifficultyBlink.setToValue(0.4);
        activeDifficultyBlink.setCycleCount(Animation.INDEFINITE);
        activeDifficultyBlink.setAutoReverse(true);
        activeDifficultyBlink.play();
    }

    private void setupAudio() {
        SoundManager sound = SoundManager.getInstance();
        volumeMusicSlider.setValue(sound.getMusicVolume() * 100);
        volumeSfxSlider.setValue(sound.getSfxVolume() * 100);
        volumeMusicSlider.valueProperty()
                .addListener((o, oldV, newV) -> sound.setMusicVolume(newV.doubleValue() / 100.0));
        volumeSfxSlider.valueProperty().addListener((o, oldV, newV) -> sound.setSfxVolume(newV.doubleValue() / 100.0));
    }

    private void startArcadeAnimations() {
        FadeTransition blink = new FadeTransition(Duration.seconds(0.6), blinkLabel);
        blink.setFromValue(1.0);
        blink.setToValue(0.0);
        blink.setCycleCount(Animation.INDEFINITE);
        blink.setAutoReverse(true);
        blink.play();
    }
}