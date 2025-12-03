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
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

public class PrimaryController {

    // Containers das diferentes telas do menu
    @FXML private VBox screenNameInput;
    @FXML private VBox screenDifficulty;
    @FXML private VBox screenSettings;
    @FXML private VBox screenCredits;
    @FXML private VBox bottomMenu;

    // Elementos da interface sujeitos a tradução (Labels e Botões)
    @FXML private Label blinkLabel; 
    @FXML private Label lblEnterInitials;
    @FXML private Button btnConfirmName;
    
    @FXML private Label lblSelectDiff;
    @FXML private Button btnBackDiff;
    @FXML private Button btnStartGame;
    
    @FXML private Label lblServiceMenu;
    @FXML private Label lblMusicVol;
    @FXML private Label lblSfxVol;
    @FXML private Button btnLanguage;
    @FXML private Button btnAboutTeam;
    @FXML private Button btnBackSettings;
    
    @FXML private Label lblDevTeam;
    @FXML private Label lblDevRole1;
    @FXML private Label lblDevRole2;
    @FXML private Label lblDevRole3;
    @FXML private Button btnBackCredits;
    
    @FXML private Button btnRank;
    @FXML private Button btnOptions;
    @FXML private Button btnExit;
    @FXML private Label lblF11Hint;

    // Componentes de entrada de nome
    @FXML private TextField nameField;
    @FXML private Label slot1, slot2, slot3, slot4;
    private FadeTransition cursorBlink;
    
    // Variáveis para controle de códigos de trapaça (Easter Eggs)
    private StringBuilder cheatBuffer = new StringBuilder();
    private boolean isEasterEggActive = false;

    // Grupo de seleção de dificuldade
    @FXML private ToggleGroup difficultyGroup;
    @FXML private ToggleButton tglNormal, tglHard, tglSouls;
    @FXML private ToggleButton tglInfinity;

    // Controles de volume
    @FXML private Slider volumeMusicSlider;
    @FXML private Slider volumeSfxSlider;

    // Imagens dos desenvolvedores
    @FXML private ImageView imgDev1, imgDev2, imgDev3;

    /**
     * Método de inicialização do controlador.
     * Configura áudio, animações, inputs e carrega recursos iniciais.
     */
    @FXML
    public void initialize() {
        setupAudio();
        
        // Aplica o idioma configurado no estado global
        updateLanguageUI();

        // Configura animação de piscar para o texto "Insert Coin"
        FadeTransition ft = new FadeTransition(Duration.seconds(0.8), blinkLabel);
        ft.setFromValue(1.0);
        ft.setToValue(0.1);
        ft.setCycleCount(Animation.INDEFINITE);
        ft.setAutoReverse(true);
        ft.play();

        setupNameInput();
        setupCheatCode();

        // Carrega avatares dos desenvolvedores
        loadDevAvatar(imgDev1, "Henrique-Fritz");
        loadDevAvatar(imgDev2, "LuanVitorCD");
        loadDevAvatar(imgDev3, "LucasSckenal");

        // Adiciona som de feedback ao alterar a dificuldade
        difficultyGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                SoundManager.getInstance().playSound("select.wav");
            }
        });
    }

    /**
     * Gerencia eventos de teclado globais no container raiz.
     * Utilizado para atalhos de navegação como a tecla Enter.
     */
    @FXML
    private void handleGlobalKey(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            
            // Inicia o jogo se estiver na tela de seleção de dificuldade e não houver menus sobrepostos
            if (screenDifficulty.isVisible() && !screenSettings.isVisible() && !screenCredits.isVisible()) {
                try {
                    startGame();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            // Confirma o nome se estiver na tela de entrada de iniciais
            else if (screenNameInput.isVisible()) {
                confirmName();
            }
        }
    }

    /**
     * Alterna o idioma do jogo e atualiza a interface.
     */
    @FXML
    private void toggleLanguage() {
        GameState.getInstance().toggleLanguage();
        SoundManager.getInstance().playSound("select.wav");
        updateLanguageUI();
    }

    /**
     * Atualiza os textos de todos os componentes da interface baseado no idioma atual.
     */
    private void updateLanguageUI() {
        boolean isPT = GameState.getInstance().getLanguage() == GameState.Language.PT;

        blinkLabel.setText(isPT ? "INSERIR FICHA" : "INSERT COIN");
        lblEnterInitials.setText(isPT ? "DIGITE INICIAIS" : "ENTER INITIALS");
        btnConfirmName.setText(isPT ? "CONFIRMAR" : "CONFIRM");
        
        lblSelectDiff.setText(isPT ? "SELECIONE DIFICULDADE" : "SELECT DIFFICULTY");
        btnBackDiff.setText(isPT ? "< VOLTAR" : "< BACK");
        btnStartGame.setText(isPT ? "INICIAR JOGO" : "START GAME");
        
        lblServiceMenu.setText(isPT ? "MENU DE SERVIÇO" : "SERVICE MENU");
        lblMusicVol.setText(isPT ? "VOLUME MÚSICA" : "MUSIC VOLUME");
        lblSfxVol.setText(isPT ? "VOLUME EFEITOS" : "SFX VOLUME");
        btnLanguage.setText(isPT ? "IDIOMA: PORTUGUÊS" : "LANGUAGE: ENGLISH");
        btnAboutTeam.setText(isPT ? "SOBRE A EQUIPE" : "ABOUT TEAM");
        btnBackSettings.setText(isPT ? "< VOLTAR" : "< BACK");
        
        lblDevTeam.setText(isPT ? "EQUIPE DE DEV" : "DEVELOPMENT TEAM");
        
        String roleText = isPT ? "Ciência da Computação" : "Computer Science";
        lblDevRole1.setText(roleText);
        lblDevRole2.setText(roleText);
        lblDevRole3.setText(roleText);

        btnBackCredits.setText(isPT ? "< VOLTAR" : "< BACK");
        
        btnRank.setText(isPT ? "RANKING" : "RANK");
        btnOptions.setText(isPT ? "OPÇÕES" : "OPTIONS");
        btnExit.setText(isPT ? "SAIR" : "EXIT");

        lblF11Hint.setText(isPT ? "PRESSIONE F11 PARA ALTERNAR O MODO DE TELA CHEIA" : "PRESS F11 TO TOGGLE FULLSCREEN");
    }

    /**
     * Configura o comportamento do campo de texto para entrada de nome.
     * Limita caracteres e gerencia os slots visuais.
     */
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

    /**
     * Configura o listener para detecção de códigos de trapaça (Cheat Codes).
     */
    private void setupCheatCode() {
        nameField.setOnKeyPressed((KeyEvent event) -> {
            if (isEasterEggActive) return;

            if (event.getCode() == KeyCode.ENTER) return;

            String key = event.getText().toUpperCase();
            if (key.matches("[A-Z]")) {
                cheatBuffer.append(key);
                if (cheatBuffer.length() > 10) cheatBuffer.deleteCharAt(0);
                
                String bufferContent = cheatBuffer.toString();

                if (bufferContent.endsWith("ABACATE")) {
                    unlockInfinityMode();
                } else if (bufferContent.endsWith("LORI")) {
                    triggerAudioOnlyEasterEgg("ieeeeei.wav", "IEEEEEI!");
                } else if (bufferContent.endsWith("GATO")) {
                    triggerAnimalEasterEgg("cat");
                } else if (bufferContent.endsWith("DOG")) {
                    triggerAnimalEasterEgg("dog");
                } else if (bufferContent.endsWith("DUCK") || bufferContent.endsWith("PATO")) {
                    triggerAnimalEasterEgg("duck");
                }
            }
        });
    }

    /**
     * Ativa um easter egg somente de áudio.
     */
    private void triggerAudioOnlyEasterEgg(String soundFile, String message) {
        isEasterEggActive = true; 
        cheatBuffer.setLength(0);
        
        SoundManager.getInstance().playSound(soundFile);
        showToastMessage(message);

        PauseTransition unlock = new PauseTransition(Duration.seconds(2));
        unlock.setOnFinished(e -> isEasterEggActive = false);
        unlock.play();
    }

    /**
     * Inicia o processo de buscar e exibir uma imagem de animal (Easter Egg).
     */
    private void triggerAnimalEasterEgg(String type) {
        isEasterEggActive = true; 
        cheatBuffer.setLength(0);

        String soundFile = type + ".wav";
        SoundManager.getInstance().playSound(soundFile);
        blinkLabel.setText("SUMMONING " + type.toUpperCase() + "...");
        
        new Thread(() -> fetchAndShowAnimalImage(type)).start();
    }

    /**
     * Realiza a chamada à API correspondente para buscar a imagem do animal.
     */
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
                unlockSpam();
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

    /**
     * Extrai a URL da imagem a partir do JSON de resposta das APIs.
     */
    private String extractUrlFromJson(String json) {
        Pattern p = Pattern.compile("\"(url|message)\":\"([^\"]+)\"");
        Matcher m = p.matcher(json);
        
        while (m.find()) {
            String rawUrl = m.group(2);
            String cleanUrl = rawUrl.replace("\\/", "/");
            String lowerUrl = cleanUrl.toLowerCase();

            if (cleanUrl.startsWith("http:")) {
                cleanUrl = cleanUrl.replace("http:", "https:");
            }

            if (lowerUrl.endsWith(".jpg") || lowerUrl.endsWith(".jpeg") || 
                lowerUrl.endsWith(".png") || lowerUrl.endsWith(".gif")) {
                return cleanUrl;
            }
        }
        return null;
    }

    /**
     * Exibe a imagem do Easter Egg sobre a interface com animações.
     */
    private void displayEasterEggImage(String url) {
        ImageView eggView = new ImageView(new Image(url));
        eggView.setFitWidth(500);
        eggView.setFitHeight(400);
        eggView.setPreserveRatio(false);
        eggView.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 20, 0, 0, 0);");

        if (screenNameInput.getParent() instanceof Pane) {
            Pane parent = (Pane) screenNameInput.getParent();
            parent.getChildren().add(eggView);

            eggView.setOpacity(0);
            eggView.setScaleX(0.5);
            eggView.setScaleY(0.5);

            FadeTransition fadeIn = new FadeTransition(Duration.seconds(0.5), eggView);
            fadeIn.setToValue(1.0);
            
            PauseTransition delay = new PauseTransition(Duration.seconds(3));
            delay.setOnFinished(e -> {
                FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.5), eggView);
                fadeOut.setToValue(0);
                fadeOut.setOnFinished(ev -> {
                    parent.getChildren().remove(eggView);
                    isEasterEggActive = false;
                });
                fadeOut.play();
                blinkLabel.setText("INSERT COIN");
            });

            fadeIn.play();
            delay.play();
        }
    }
    
    /**
     * Exibe uma mensagem temporária na label principal.
     */
    private void showToastMessage(String msg) {
         blinkLabel.setText(msg);
         PauseTransition delay = new PauseTransition(Duration.seconds(2));
         delay.setOnFinished(e -> {
            boolean isPT = GameState.getInstance().getLanguage() == GameState.Language.PT;
            blinkLabel.setText(isPT ? "INSERIR FICHA" : "INSERT COIN");
         });
         delay.play();
    }

    /**
     * Desbloqueia o modo de dificuldade Infinity.
     */
    private void unlockInfinityMode() {
        isEasterEggActive = true; 
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
        
        PauseTransition unlock = new PauseTransition(Duration.seconds(1));
        unlock.setOnFinished(e -> isEasterEggActive = false);
        unlock.play();
    }

    /**
     * Configura a imagem do avatar recortada em formato circular.
     */
    private void loadDevAvatar(ImageView imgView, String githubUser) {
        Circle clip = new Circle(50, 50, 50);
        imgView.setClip(clip);
        String url = "https://github.com/" + githubUser + ".png";
        Image img = new Image(url, true);
        imgView.setImage(img);
    }

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

    /**
     * Atualiza os slots visuais de caracteres do nome conforme o usuário digita.
     */
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

        if (chars.length > 0) slot1.getStyleClass().add("char-slot-filled");
        if (chars.length > 1) slot2.getStyleClass().add("char-slot-filled");
        if (chars.length > 2) slot3.getStyleClass().add("char-slot-filled");
        if (chars.length > 3) slot4.getStyleClass().add("char-slot-filled");

        Node nextTarget = slot1;
        if (chars.length == 1) nextTarget = slot2;
        else if (chars.length == 2) nextTarget = slot3;
        else if (chars.length >= 3) nextTarget = slot4;

        blinkSlot(nextTarget);
    }

    /**
     * Inicia a animação de cursor piscando no slot alvo.
     */
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

    /**
     * Inicializa os sliders de volume com os valores atuais do SoundManager.
     */
    private void setupAudio() {
        SoundManager sound = SoundManager.getInstance();
        volumeMusicSlider.setValue(sound.getMusicVolume() * 100);
        volumeSfxSlider.setValue(sound.getSfxVolume() * 100);
        volumeMusicSlider.valueProperty()
                .addListener((o, oldV, newV) -> sound.setMusicVolume(newV.doubleValue() / 100.0));
        volumeSfxSlider.valueProperty().addListener((o, oldV, newV) -> sound.setSfxVolume(newV.doubleValue() / 100.0));

        volumeMusicSlider.setOnMouseReleased(e -> sound.playSound("select.wav"));
        volumeSfxSlider.setOnMouseReleased(e -> sound.playSound("select.wav"));
    }

    @FXML
    private void confirmName() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) return;
        GameState.getInstance().setPlayerName(name.toUpperCase());
        SoundManager.getInstance().playSound("select.wav");
        screenNameInput.setVisible(false);
        screenDifficulty.setVisible(true);
    }

    @FXML
    private void backToNameInput() {
        SoundManager.getInstance().playSound("select.wav");
        screenDifficulty.setVisible(false);
        screenNameInput.setVisible(true);
        Platform.runLater(() -> nameField.requestFocus());
    }

    @FXML
    private void startGame() throws IOException {
        GameState.Difficulty selectedDiff = GameState.Difficulty.NORMAL;
        if (tglHard.isSelected()) selectedDiff = GameState.Difficulty.HARD;
        else if (tglSouls.isSelected()) selectedDiff = GameState.Difficulty.SOULS;
        else if (tglInfinity.isSelected()) selectedDiff = GameState.Difficulty.INFINITY;

        GameState.getInstance().setDifficulty(selectedDiff);
        GameState.getInstance().resetGame();

        SoundManager.getInstance().playSound("select.wav");
        App.setRoot("secondary");
    }

    @FXML
    private void showLeaderboard() throws IOException {
        SoundManager.getInstance().playSound("select.wav");
        App.setRoot("leaderboard");
    }

    @FXML
    private void goToSettings() {
        SoundManager.getInstance().playSound("select.wav");
        screenNameInput.setVisible(false);
        screenDifficulty.setVisible(false);
        bottomMenu.setVisible(false);
        screenSettings.setVisible(true);
        screenSettings.toFront();
    }

    @FXML
    private void backToName() {
        SoundManager.getInstance().playSound("select.wav");
        screenSettings.setVisible(false);
        screenNameInput.setVisible(true);
        bottomMenu.setVisible(true);
        Platform.runLater(() -> nameField.requestFocus());
    }

    @FXML
    private void exitApp() {
        SoundManager.getInstance().playSound("select.wav");
        try {
            Thread.sleep(500); 
        } catch (InterruptedException e) { e.printStackTrace(); }
        Platform.exit();
        System.exit(0);
    }
}