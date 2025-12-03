package br.edu.unijui.piu.guessquest;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.Animation;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
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
    private VBox screenCredits;
    @FXML
    private VBox bottomMenu;

    // --- ESTADO TELA 1: NOME ---
    @FXML
    private TextField nameField;
    @FXML
    private Label slot1, slot2, slot3, slot4;
    private FadeTransition cursorBlink;

    private StringBuilder cheatBuffer = new StringBuilder();
    private boolean isEasterEggActive = false;

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

    // --- ESTADO TELA 4: CREDITS (AVATARES) ---
    @FXML private ImageView imgDev1, imgDev2, imgDev3;

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

        // Carrega avatares do time (Exemplo usando GitHub - troque pelos users reais)
        loadDevAvatar(imgDev1, "Henrique-Fritz");
        loadDevAvatar(imgDev2, "LuanVitorCD");
        loadDevAvatar(imgDev3, "LucasSckenal");

        // Adiciona feedback sonoro ao selecionar a dificuldade
        difficultyGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                SoundManager.getInstance().playSound("select.wav");
            }
        });
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

    // === LÓGICA DE EASTER EGGS E CHEATS ===
    private void setupCheatCode() {
        nameField.setOnKeyPressed((KeyEvent event) -> {
            // Se já tem easter egg rodando, ignora input para evitar caos sonoro
            if (isEasterEggActive) return;

            String key = event.getText().toUpperCase();
            if (key.matches("[A-Z]")) {
                cheatBuffer.append(key);
                if (cheatBuffer.length() > 10) cheatBuffer.deleteCharAt(0);
                
                String bufferContent = cheatBuffer.toString();

                if (bufferContent.endsWith("ABACATE")) {
                    unlockInfinityMode();
                } else if (bufferContent.endsWith("LORI")) {
                    triggerAudioOnlyEasterEgg("ieeeeei.wav", "IEEEEEI!");
                } else if (bufferContent.endsWith("GATO") || bufferContent.endsWith("CAT")) {
                    triggerAnimalEasterEgg("cat");
                } else if (bufferContent.endsWith("DOG")) {
                    triggerAnimalEasterEgg("dog");
                } else if (bufferContent.endsWith("DUCK") || bufferContent.endsWith("PATO")) {
                    triggerAnimalEasterEgg("duck");
                }
            }
        });
    }

    // Easter Egg só de áudio (Lori) com trava de tempo
    private void triggerAudioOnlyEasterEgg(String soundFile, String message) {
        isEasterEggActive = true; // Trava
        cheatBuffer.setLength(0);
        
        SoundManager.getInstance().playSound(soundFile);
        showToastMessage(message);

        // Destrava após 2 segundos
        PauseTransition unlock = new PauseTransition(Duration.seconds(2));
        unlock.setOnFinished(e -> isEasterEggActive = false);
        unlock.play();
    }

    private void triggerAnimalEasterEgg(String type) {
        isEasterEggActive = true; // Trava
        cheatBuffer.setLength(0);

        String soundFile = type + ".wav";
        SoundManager.getInstance().playSound(soundFile);
        blinkLabel.setText("SUMMONING " + type.toUpperCase() + "...");
        
        new Thread(() -> fetchAndShowAnimalImage(type)).start();
    }

    private void fetchAndShowAnimalImage(String type) {
        String apiUrl = "";
        switch (type) {
            case "cat":  apiUrl = "https://api.thecatapi.com/v1/images/search"; break;
            case "dog":  apiUrl = "https://dog.ceo/api/breeds/image/random"; break;
            case "duck": apiUrl = "https://random-d.uk/api/v2/random"; break;
        }

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(apiUrl)).build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            String imageUrl = extractUrlFromJson(response.body());

            if (imageUrl != null) {
                Platform.runLater(() -> displayEasterEggImage(imageUrl));
            } else {
                unlockSpam(); // Destrava se falhar
            }
        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() -> blinkLabel.setText("FAILED TO SUMMON!"));
            unlockSpam();
        }
    }

    private void unlockSpam() {
        isEasterEggActive = false;
    }

    // Extrator simples de URL do JSON (para as 3 APIs usadas)
    private String extractUrlFromJson(String json) {
        // Regex unificado que captura tanto "url" quanto "message"
        Pattern p = Pattern.compile("\"(url|message)\":\"([^\"]+)\"");
        Matcher m = p.matcher(json);
        
        // Itera sobre TODAS as ocorrências encontradas
        while (m.find()) {
            String rawUrl = m.group(2);
            String cleanUrl = rawUrl.replace("\\/", "/");
            String lowerUrl = cleanUrl.toLowerCase();

            // Força HTTPS para evitar problemas de mixed content (API do pato retorna HTTP)
            if (cleanUrl.startsWith("http:")) {
                cleanUrl = cleanUrl.replace("http:", "https:");
            }

            // Valida se o link termina com extensão de imagem
            if (lowerUrl.endsWith(".jpg") || lowerUrl.endsWith(".jpeg") || 
                lowerUrl.endsWith(".png") || lowerUrl.endsWith(".gif")) {
                System.out.println(cleanUrl);
                return cleanUrl;
            }
        }
        return null;
    }

    // Mostra a imagem na tela por cima de tudo
    private void displayEasterEggImage(String url) {
        // Cria a imagem
        ImageView eggView = new ImageView(new Image(url));
        
        // FORÇA UM TAMANHO PADRÃO FIXO (500x400)
        eggView.setFitWidth(500);
        eggView.setFitHeight(400);
        
        // Mantém a proporção fixa para evitar distorções
        eggView.setPreserveRatio(false);
        
        eggView.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 20, 0, 0, 0);");

        // Adiciona ao StackPane pai (para ficar por cima do input de nome)
        if (screenNameInput.getParent() instanceof Pane) {
            Pane parent = (Pane) screenNameInput.getParent();
            parent.getChildren().add(eggView);

            // Animação de entrada (Fade In + Scale)
            eggView.setOpacity(0);
            eggView.setScaleX(0.5);
            eggView.setScaleY(0.5);

            FadeTransition fadeIn = new FadeTransition(Duration.seconds(0.5), eggView);
            fadeIn.setToValue(1.0);
            
            // Remove a imagem depois de 3 segundos
            PauseTransition delay = new PauseTransition(Duration.seconds(3));
            delay.setOnFinished(e -> {
                FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.5), eggView);
                fadeOut.setToValue(0);
                fadeOut.setOnFinished(ev -> {
                    parent.getChildren().remove(eggView);
                    isEasterEggActive = false; // Destrava input
                });
                fadeOut.play();
                blinkLabel.setText("INSERT COIN"); // Restaura texto original
            });

            fadeIn.play();
            delay.play();
        }
    }
    
    // Pequena mensagem temporária para feedback do "Lori"
    private void showToastMessage(String msg) {
         blinkLabel.setText(msg);
         PauseTransition delay = new PauseTransition(Duration.seconds(2));
         delay.setOnFinished(e -> blinkLabel.setText("INSERT COIN"));
         delay.play();
    }

    // Método chamado quando o cheat code é detectado para alterar a UI e desbloquear o modo INFINITY
    private void unlockInfinityMode() {
        isEasterEggActive = true; // Trava brevemente
        cheatBuffer.setLength(0);
        SoundManager.getInstance().playSound("correct.wav");
        try {
            Thread.sleep(300);
            SoundManager.getInstance().playSound("abacate.wav");
        } catch (InterruptedException e) { e.printStackTrace(); }

        tglInfinity.setVisible(true);
        tglInfinity.setManaged(true);
        blinkLabel.setText("INFINITY UNLOCKED!");
        blinkLabel.setStyle("-fx-text-fill: #ffd700; -fx-effect: dropshadow(gaussian, #B8860B, 10, 0.0, 0, 0);");
        
        // Destrava input
        PauseTransition unlock = new PauseTransition(Duration.seconds(1));
        unlock.setOnFinished(e -> isEasterEggActive = false);
        unlock.play();
    }

    // === LÓGICA DE AVATARES DO TIME (GITHUB) ===
    private void loadDevAvatar(ImageView imgView, String githubUser) {
        // Cria recorte circular
        Circle clip = new Circle(50, 50, 50); // Raio 50 (Imagem 100x100)
        imgView.setClip(clip);
        
        // URL Padrão do GitHub: https://github.com/usuario.png
        String url = "https://github.com/" + githubUser + ".png";
        
        // Carregamento em background (true)
        Image img = new Image(url, true);

        imgView.setImage(img);
    }

    // === NAVEGAÇÃO SETTINGS / CREDITS ===
    @FXML
    private void goToCredits() {
        SoundManager.getInstance().playSound("select.wav");
        screenSettings.setVisible(false);
        screenCredits.setVisible(true);
        screenCredits.toFront();
    }

    @FXML
    private void backToSettings() {
        SoundManager.getInstance().playSound("select.wav");
        screenCredits.setVisible(false);
        screenSettings.setVisible(true);
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

        // Feedback sonoro ao ajustar os sliders
        volumeMusicSlider.setOnMouseReleased(e -> sound.playSound("select.wav"));
        volumeSfxSlider.setOnMouseReleased(e -> sound.playSound("select.wav"));
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
        screenDifficulty.setVisible(false);
        bottomMenu.setVisible(false);

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