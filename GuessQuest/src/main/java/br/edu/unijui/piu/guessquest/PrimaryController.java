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

    // --- TELAS (CONTAINERS) PARA CADA ESTADO ---
    @FXML
    private VBox screenNameInput;
    @FXML
    private VBox screenDifficulty;
    @FXML
    private VBox screenSettings;
    @FXML
    private VBox bottomMenu;

    // --- ESTADO TELA 1: NOME ---
    @FXML
    private TextField nameField;
    @FXML
    private Label slot1, slot2, slot3, slot4;
    private FadeTransition cursorBlink;

    private StringBuilder cheatBuffer = new StringBuilder();

    // --- ESTADO TELA 2: DIFICULDADE ---
    @FXML
    private ToggleGroup difficultyGroup;
    @FXML
    private ToggleButton tglNormal, tglHard, tglSouls;
    @FXML
    private ToggleButton tglInfinity;

    // --- ESTADO TELA 3: SETTINGS ---
    @FXML
    private Slider volumeMusicSlider;
    @FXML
    private Slider volumeSfxSlider;

    @FXML
    private Label blinkLabel;

    // Inicialização do controller
    @FXML
    public void initialize() {
        setupAudio();

        FadeTransition ft = new FadeTransition(Duration.seconds(0.8), blinkLabel);
        ft.setFromValue(1.0);
        ft.setToValue(0.1);
        ft.setCycleCount(Animation.INDEFINITE);
        ft.setAutoReverse(true);
        ft.play();

        setupNameInput();
        setupCheatCode();
    }

    // Lógica para evitar mais de 4 caracteres e atualizar os slots visuais
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

    // Lógica para ser possível desbloquear o modo INFINITY com o cheat code "ABACATE" ao estilo trapaça código KONAMI
    private void setupCheatCode() {
        nameField.setOnKeyPressed((KeyEvent event) -> {
            String key = event.getText().toUpperCase();
            if (key.matches("[A-Z]")) {
                cheatBuffer.append(key);
                if (cheatBuffer.length() > 7)
                    cheatBuffer.deleteCharAt(0);
                if (cheatBuffer.toString().equals("ABACATE"))
                    unlockInfinityMode();
            }
        });
    }

    // Método chamado quando o cheat code é detectado para alterar a UI e desbloquear o modo INFINITY
    private void unlockInfinityMode() {
        SoundManager.getInstance().playSound("correct.wav");

        try {
            Thread.sleep(300); // Pequena pausa antes de mostrar o desbloqueio
            SoundManager.getInstance().playSound("abacate.wav");
            
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        tglInfinity.setVisible(true);
        tglInfinity.setManaged(true);
        blinkLabel.setText("INFINITY UNLOCKED!");
        blinkLabel.setStyle("-fx-text-fill: #ffd700; -fx-effect: dropshadow(gaussian, #B8860B, 10, 0.0, 0, 0);");
        cheatBuffer.setLength(0);
    }

    // Atualiza os "slots visuais" (espaço das letras para o nome) com os caracteres do nome em si
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

    // Anima o cursor piscando no slot atual
    private void blinkSlot(Node target) {
        stopCursorBlink();
        cursorBlink = new FadeTransition(Duration.seconds(0.4), target);
        cursorBlink.setFromValue(1.0);
        cursorBlink.setToValue(0.2);
        cursorBlink.setCycleCount(Animation.INDEFINITE);
        cursorBlink.setAutoReverse(true);
        cursorBlink.play();
    }

    // Para a animação de piscar do cursor
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

    // Configura os sliders de volume para música e efeitos sonoros
    private void setupAudio() {
        SoundManager sound = SoundManager.getInstance();
        volumeMusicSlider.setValue(sound.getMusicVolume() * 100);
        volumeSfxSlider.setValue(sound.getSfxVolume() * 100);
        volumeMusicSlider.valueProperty()
                .addListener((o, oldV, newV) -> sound.setMusicVolume(newV.doubleValue() / 100.0));
        volumeSfxSlider.valueProperty().addListener((o, oldV, newV) -> sound.setSfxVolume(newV.doubleValue() / 100.0));
    }

    // === LÓGICA DE TROCA DE TELA PARA INICIAR O JOGO ===
    @FXML
    private void confirmName() {
        String name = nameField.getText().trim();
        if (name.isEmpty())
            return;
        GameState.getInstance().setPlayerName(name.toUpperCase());
        SoundManager.getInstance().playSound("select.wav");
        screenNameInput.setVisible(false);
        screenDifficulty.setVisible(true);
    }

    // === LÓGICA DE TROCA DE TELA PARA VOLTAR AO MENU INICIAL ===
    @FXML
    private void backToNameInput() {
        SoundManager.getInstance().playSound("select.wav");
        screenDifficulty.setVisible(false);
        screenNameInput.setVisible(true);
        Platform.runLater(() -> nameField.requestFocus());
    }

    // === LÓGICA DE INÍCIO DO JOGO ===
    // É trocado para a tela "secondary.fxml", que contém o jogo em si
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

        SoundManager.getInstance().playSound("select.wav");
        App.setRoot("secondary");
    }

    // === LÓGICA DE TROCA DE TELA PARA LEADERBOARD ===
    // É trocado para a tela "leaderboard.fxml", que contém a tabela de pontuações
    @FXML
    private void showLeaderboard() throws IOException {
        SoundManager.getInstance().playSound("select.wav");
        App.setRoot("leaderboard");
    }

    // === LÓGICA DE TROCA DE "TELA" PARA SETTINGS ===
    // Mostra a "tela" de settings (na verdade só um container que fica sobreposto)
    @FXML
    private void goToSettings() {

        // Toca som de seleção para feedback ao usuário
        SoundManager.getInstance().playSound("select.wav");

        // Esconde tudo o que está na tela principal
        screenNameInput.setVisible(false);
        screenDifficulty.setVisible(false); // Caso esteja nessa tela
        bottomMenu.setVisible(false); // Esconde os botões de baixo

        // Mostra a tela de settings
        screenSettings.setVisible(true);
        screenSettings.toFront();
    }

    // Lógica para esconder a "tela" de settings e voltar para a tela inicial (com os elementos visíveis de novo)
    @FXML
    private void backToName() {

        // Toca som de seleção para feedback ao usuário
        SoundManager.getInstance().playSound("select.wav");

        // Esconde settings
        screenSettings.setVisible(false);

        // Restaura a tela inicial
        screenNameInput.setVisible(true);
        bottomMenu.setVisible(true); // Traz os botões de volta

        Platform.runLater(() -> nameField.requestFocus());
    }

    // Lógica para sair do aplicativo com som de feedback
    @FXML
    private void exitApp() {
        
        // Toca som de seleção para feedback ao usuário
        SoundManager.getInstance().playSound("select.wav");

        try {
            Thread.sleep(500); // Espera meio segundo para o som tocar antes de fechar
        } catch (InterruptedException e) {
            e.printStackTrace(); // Só para fins de debug (eu realmente quero que o usuário veja (ou ouça nesse caso) o som tocar)
        }

        Platform.exit();
        System.exit(0);
    }
}