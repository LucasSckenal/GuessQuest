package br.edu.unijui.piu.guessquest;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

public class SecondaryController {

    private static final String API_KEY = "e6234549ffcb449f9502ec12c04a4201";
    private static final String API_URL_TEMPLATE = "https://api.rawg.io/api/games?key=%s&ordering=-added&metacritic=%s&page_size=40&page=%d";

    private static final Set<String> gamesPlayedThisSession = new HashSet<>();
    private static final Set<String> INVALID_EXACT_NAMES = new HashSet<>();

    static {
        // ... (Mantém a lista INVALID_EXACT_NAMES original completa aqui) ...
        INVALID_EXACT_NAMES.add("PC");
        INVALID_EXACT_NAMES.add("PlayStation");
        // (Adicione o restante da lista original para economizar espaço na resposta)
    }

    @FXML
    private Label lblPhase;
    @FXML
    private Label lblScore;
    @FXML
    private Label lblLives;
    @FXML
    private Label lblPlayer;
    @FXML
    private ImageView gameImage;
    @FXML
    private Button btnA, btnB, btnC, btnD;
    @FXML
    private Label lblFeedback;
    @FXML
    private VBox loadingLayer;

    // NOVOS BOTÕES DE DICA
    @FXML
    private Button btnHintCall;
    @FXML
    private Button btnHintStudents;
    @FXML
    private Button btnHintAvocado;

    private GameState state;
    private String correctGameName;
    private final Random random = new Random();
    private boolean interactionLocked = false;

    @FXML
    public void initialize() {
        state = GameState.getInstance();
        if (state.getCurrentPhase() == 1) {
            gamesPlayedThisSession.clear();
        }
        updateUIHeader();
        loadNextLevel();
    }

    private void updateUIHeader() {
        lblPlayer.setText("JOGADOR: " + state.getPlayerName());
        lblPhase.setText(String.format("FASE: %02d-15", state.getCurrentPhase()));
        lblScore.setText(String.format("PONTOS: %06d", state.getCurrentScore()));

        StringBuilder livesStr = new StringBuilder("VIDAS: ");
        for (int i = 0; i < state.getCurrentLives(); i++) {
            livesStr.append("♥ ");
        }
        lblLives.setText(livesStr.toString());
    }

    // =========================================================================
    // LÓGICA DAS DICAS (LIFELINES)
    // =========================================================================

    @FXML
    private void useHintCall() {
        if (state.isHintCallUsed() || interactionLocked)
            return;

        // Marca como usada
        state.setHintCallUsed(true);
        updateHintButtonStyle(btnHintCall, true);

        // Lógica: Remove 1 alternativa errada aleatória
        List<Button> wrongButtons = getActiveWrongButtons();
        if (!wrongButtons.isEmpty()) {
            Button toRemove = wrongButtons.get(random.nextInt(wrongButtons.size()));
            disableButtonOption(toRemove);

            lblFeedback.setText("OTACON: 'Hackeei o sistema! A opção " + getOptionLetter(toRemove) + " é falsa!'");
            lblFeedback.setStyle("-fx-text-fill: #3498db;"); // Azul
        }
    }

    @FXML
    private void useHintStudents() {
        if (state.isHintStudentsUsed() || interactionLocked)
            return;

        state.setHintStudentsUsed(true);
        updateHintButtonStyle(btnHintStudents, true);

        // Lógica: Atribui % (Correta entre 40-70%, restante distribuído)
        int correctPercent = 40 + random.nextInt(31); // 40 a 70
        int remainingPercent = 100 - correctPercent;

        List<Button> wrongs = getActiveWrongButtons();
        Button correctBtn = getCorrectButton();

        // Atualiza botão correto
        if (correctBtn != null) {
            appendPercentToButton(correctBtn, correctPercent);
        }

        // Distribui o resto entre os errados
        for (int i = 0; i < wrongs.size(); i++) {
            Button btn = wrongs.get(i);
            int share;
            if (i == wrongs.size() - 1) {
                share = remainingPercent; // O último pega o que sobrou
            } else {
                share = random.nextInt(remainingPercent / 2); // Pega um pedaço pequeno
                remainingPercent -= share;
            }
            appendPercentToButton(btn, share);
        }

        lblFeedback.setText("UNIVERSITÁRIOS: 'Acreditamos que seja a maior porcentagem...'");
        lblFeedback.setStyle("-fx-text-fill: #ea80fc;"); // Roxo
    }

    @FXML
    private void useHintAvocado() {
        if (state.isHintAvocadoUsed() || interactionLocked)
            return;

        state.setHintAvocadoUsed(true);
        updateHintButtonStyle(btnHintAvocado, true);

        // Lógica: O Poder do Abacate remove 2 erradas!
        List<Button> wrongs = getActiveWrongButtons();
        Collections.shuffle(wrongs); // Embaralha para ser aleatório

        int removedCount = 0;
        for (Button btn : wrongs) {
            if (removedCount >= 2)
                break;
            disableButtonOption(btn);
            removedCount++;
        }

        lblFeedback.setText("ABACATE SAGRADO: 'A polpa divina eliminou as impurezas!'");
        lblFeedback.setStyle("-fx-text-fill: #00ff00; -fx-font-weight: bold;"); // Verde
    }

    // --- Helpers das Dicas ---

    private void updateHintButtonStyle(Button btn, boolean used) {
        if (used) {
            btn.setDisable(true);
            btn.getStyleClass().add("hint-used");
        } else {
            btn.setDisable(false);
            btn.getStyleClass().remove("hint-used");
        }
    }

    private List<Button> getActiveWrongButtons() {
        List<Button> list = new ArrayList<>();
        checkWrongAndAdd(btnA, list);
        checkWrongAndAdd(btnB, list);
        checkWrongAndAdd(btnC, list);
        checkWrongAndAdd(btnD, list);
        return list;
    }

    private Button getCorrectButton() {
        if (isCorrect(btnA))
            return btnA;
        if (isCorrect(btnB))
            return btnB;
        if (isCorrect(btnC))
            return btnC;
        if (isCorrect(btnD))
            return btnD;
        return null;
    }

    private void checkWrongAndAdd(Button btn, List<Button> list) {
        if (!btn.isDisabled() && !isCorrect(btn)) {
            list.add(btn);
        }
    }

    private boolean isCorrect(Button btn) {
        String cleanText = cleanButtonText(btn.getText());
        return cleanText.equalsIgnoreCase(correctGameName);
    }

    private String cleanButtonText(String text) {
        // Remove "A) ", "B) " e também as porcentagens " (50%)" se houver
        String temp = text.length() > 3 ? text.substring(3) : text;
        if (temp.contains("(")) {
            temp = temp.substring(0, temp.lastIndexOf("(")).trim();
        }
        return temp;
    }

    private String getOptionLetter(Button btn) {
        return btn.getText().substring(0, 1);
    }

    private void disableButtonOption(Button btn) {
        btn.setDisable(true);
        btn.setStyle("-fx-opacity: 0.2; -fx-background-color: #000; -fx-border-color: #333;");
    }

    private void appendPercentToButton(Button btn, int percent) {
        if (!btn.getText().contains("%")) {
            btn.setText(btn.getText() + " (" + percent + "%)");
        }
    }

    // =========================================================================

    private void loadNextLevel() {
        // ... (Mantém lógica anterior) ...
        if (state.getCurrentPhase() > 15) {
            endGame(true);
            return;
        }

        interactionLocked = true;
        lblFeedback.setText("CARREGANDO DADOS...");
        loadingLayer.setVisible(true);

        new Thread(() -> {
            try {
                // ... (Mantém lógica de fetch igual) ...
                GameData levelData = fetchGameDataWithRetry(); // Refatorei para simplificar leitura
                final GameData finalData = levelData;
                Platform.runLater(() -> setupLevelUI(finalData));
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> setupLevelUI(getEmergencyFallbackData()));
            }
        }).start();
    }

    // Helper simples para retry
    private GameData fetchGameDataWithRetry() throws Exception {
        GameData levelData = null;
        int attempts = 0;
        while (attempts < 3) {
            boolean forceSafePage = (attempts == 2);
            levelData = fetchGameData(forceSafePage);
            if (!levelData.correctName.equals("API Error") && !levelData.options.isEmpty()) {
                break;
            }
            attempts++;
            Thread.sleep(500);
        }
        if (levelData == null || levelData.options.isEmpty() || levelData.correctName.equals("API Error")) {
            return getEmergencyFallbackData();
        }
        return levelData;
    }

    private void setupLevelUI(GameData data) {
        if (data.imageUrl == null || data.imageUrl.isEmpty()) {
            loadNextLevel();
            return;
        }

        loadingLayer.setVisible(false);
        interactionLocked = false;
        lblFeedback.setText("");

        this.correctGameName = data.correctName;
        gamesPlayedThisSession.add(data.correctName);

        try {
            gameImage.setImage(new Image(data.imageUrl, true));
        } catch (Exception e) {
            loadNextLevel();
            return;
        }

        List<String> options = data.options;
        int backupCount = 1;
        while (options.size() < 4) {
            options.add("Mystery Game " + backupCount++);
        }

        btnA.setText("A) " + options.get(0));
        btnB.setText("B) " + options.get(1));
        btnC.setText("C) " + options.get(2));
        btnD.setText("D) " + options.get(3));

        resetButtonStyles();

        // ATUALIZA O ESTADO DOS BOTÕES DE DICA (SE JÁ FORAM USADOS, FICAM
        // DESABILITADOS)
        updateHintButtonStyle(btnHintCall, state.isHintCallUsed());
        updateHintButtonStyle(btnHintStudents, state.isHintStudentsUsed());
        updateHintButtonStyle(btnHintAvocado, state.isHintAvocadoUsed());
    }

    @FXML
    private void handleAnswer(javafx.event.ActionEvent event) {
        if (interactionLocked)
            return;

        Button clickedButton = (Button) event.getSource();
        String selectedAnswer = cleanButtonText(clickedButton.getText()); // Usa o helper novo que limpa %

        interactionLocked = true;

        if (selectedAnswer.equalsIgnoreCase(correctGameName)) {
            clickedButton.getStyleClass().add("button-correct");
            lblFeedback.setText("CORRETO! +1000 PTS");
            lblFeedback.setStyle("-fx-text-fill: #00ff00;");
            SoundManager.getInstance().playSound("correct.wav");
            state.addScore(1000);

            scheduleNextLevel(1500);
        } else {
            clickedButton.getStyleClass().add("button-wrong");
            lblFeedback.setText("ERRADO! PERDEU VIDA");
            lblFeedback.setStyle("-fx-text-fill: #ff3333;");
            SoundManager.getInstance().playSound("wrong.wav");
            state.decreaseLife();
            updateUIHeader();

            if (state.getCurrentLives() <= 0) {
                endGame(false);
            } else {
                scheduleNextLevel(1500);
            }
        }
    }

    private void scheduleNextLevel(int delay) {
        new java.util.Timer().schedule(new java.util.TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    state.nextPhase();
                    updateUIHeader();
                    loadNextLevel();
                });
            }
        }, delay);
    }

    private void resetButtonStyles() {
        String baseStyle = "game-button";
        resetSingleButton(btnA, baseStyle);
        resetSingleButton(btnB, baseStyle);
        resetSingleButton(btnC, baseStyle);
        resetSingleButton(btnD, baseStyle);
    }

    private void resetSingleButton(Button btn, String style) {
        btn.getStyleClass().setAll(style);
        btn.setDisable(false);
        btn.setStyle(""); // Reseta estilos inline (opacidade, cor de fundo)
    }

    // ... (Mantém getEmergencyFallbackData, endGame e lógica de API/JSON inalterada
    // abaixo) ...
    // Vou incluir apenas os métodos de API para o código ficar compilável se você
    // copiar tudo

    private GameData getEmergencyFallbackData() {
        GameData data = new GameData();
        data.correctName = "Pac-Man";
        data.imageUrl = "https://media.rawg.io/media/games/b21/b21555abc69d04d9b5d7da855e70d858.jpg";
        data.options.add("Pac-Man");
        data.options.add("Tetris");
        data.options.add("Space Invaders");
        data.options.add("Pong");
        Collections.shuffle(data.options);
        return data;
    }

    private void endGame(boolean win) {
        loadingLayer.setVisible(true);
        LeaderboardManager.saveScore(state.getPlayerName(), state.getCurrentScore());

        if (win) {
            lblFeedback.setText("PARABÉNS! VOCÊ ZEROU!");
            lblFeedback.setStyle("-fx-text-fill: gold;");
            SoundManager.getInstance().playSound("win.wav");
        } else {
            lblFeedback.setText("GAME OVER");
            lblFeedback.setStyle("-fx-text-fill: red;");
            SoundManager.getInstance().playSound("gameover.wav");
        }

        new java.util.Timer().schedule(new java.util.TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    try {
                        App.setRoot("primary");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }
        }, 4000);
    }

    private GameData fetchGameData(boolean forceSafePage) throws Exception {
        int pageBase;
        if (forceSafePage) {
            pageBase = 1;
        } else {
            pageBase = random.nextInt(50) + 1;
            if (state.getCurrentPhase() > 5)
                pageBase += 100;
            if (state.getDifficulty() == GameState.Difficulty.SOULS)
                pageBase = random.nextInt(500) + 1;
        }

        String metacriticFilter = state.getDifficulty().metacriticRange;
        if ((state.getCurrentPhase() > 10 && state.getDifficulty() != GameState.Difficulty.SOULS) || forceSafePage) {
            metacriticFilter = "50,100";
        }

        String url = String.format(API_URL_TEMPLATE, API_KEY, metacriticFilter, pageBase);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        return parseJsonManually(response.body());
    }

    // Classes internas GameData e GameCandidate e métodos de parseJsonManually
    // permanecem iguais
    private static class GameData {
        String correctName = "API Error";
        String imageUrl;
        List<String> options = new ArrayList<>();
    }

    private static class GameCandidate {
        String name;
        String image;

        GameCandidate(String n, String i) {
            this.name = n;
            this.image = i;
        }
    }

    private GameData parseJsonManually(String json) {
        // ... (CÓDIGO DE PARSE JSON PERMANECE O MESMO DO ARQUIVO ORIGINAL) ...
        // Para economizar espaço, assuma que o código de parse está aqui.
        // Se precisar, posso reenviar, mas a lógica de dicas não afeta isso.

        // Copiando a lógica básica para garantir funcionamento:
        GameData data = new GameData();
        List<GameCandidate> candidates = new ArrayList<>();
        Pattern gamePattern = Pattern.compile("\"slug\":\"(?<slug>[^\"]+)\"[^\\{\\[]*?\"name\":\"(?<name>[^\"]+)\"");
        Matcher matcher = gamePattern.matcher(json);
        List<Integer> matchEnds = new ArrayList<>();
        List<String> tempNames = new ArrayList<>();
        List<String> tempSlugs = new ArrayList<>();
        while (matcher.find()) {
            tempSlugs.add(matcher.group("slug"));
            tempNames.add(unescapeJava(matcher.group("name")));
            matchEnds.add(matcher.end());
        }
        for (int i = 0; i < tempNames.size(); i++) {
            String name = tempNames.get(i);
            String slug = tempSlugs.get(i);
            int currentEnd = matchEnds.get(i);
            int limit = (i < matchEnds.size() - 1) ? matchEnds.get(i + 1) : json.length();
            if (limit - currentEnd > 2000)
                limit = currentEnd + 2000;
            if (isValidGameName(name, slug)) {
                String image = extractImageInRange(json, currentEnd, limit);
                candidates.add(new GameCandidate(name, image));
            }
        }
        if (candidates.isEmpty())
            return data;

        List<GameCandidate> available = new ArrayList<>();
        for (GameCandidate cand : candidates) {
            boolean hasImage = cand.image != null && !cand.image.isEmpty() && !cand.image.equals("null");
            if (!gamesPlayedThisSession.contains(cand.name) && hasImage) {
                available.add(cand);
            }
        }
        if (available.isEmpty())
            available = candidates;

        int correctIndex = random.nextInt(available.size());
        GameCandidate correctGame = available.get(correctIndex);
        data.correctName = correctGame.name;
        data.imageUrl = correctGame.image;
        data.options.add(correctGame.name);

        List<String> wrongPool = new ArrayList<>();
        for (GameCandidate cand : candidates) {
            if (!cand.name.equals(correctGame.name))
                wrongPool.add(cand.name);
        }
        Collections.shuffle(wrongPool);
        for (int k = 0; k < 3 && k < wrongPool.size(); k++)
            data.options.add(wrongPool.get(k));
        Collections.shuffle(data.options);
        return data;
    }

    // ... Métodos auxiliares de parse (extractImageInRange, unescapeJava,
    // isValidGameName) iguais ao original ...
    private String extractImageInRange(String json, int start, int end) {
        String snippet = json.substring(start, Math.min(end, json.length()));
        int idx = snippet.indexOf("\"background_image\":\"");
        if (idx != -1) {
            int urlStart = idx + 20;
            int urlEnd = snippet.indexOf("\"", urlStart);
            if (urlEnd != -1)
                return unescapeJava(snippet.substring(urlStart, urlEnd));
        }
        return null;
    }

    private boolean isValidGameName(String name, String slug) {
        if (name == null || name.trim().isEmpty())
            return false;
        if (name.matches(".*\\p{InCyrillic}.*"))
            return false;
        if (INVALID_EXACT_NAMES.contains(name))
            return false;
        if (name.matches(".*[^\\x00-\\x7F\\p{L}\\p{N}\\s\\p{P}].*") && !name.matches(".*[áéíóúãõçÁÉÍÓÚÃÕÇ].*"))
            return false;
        String lowerName = name.toLowerCase();
        if (lowerName.contains("playstation") || lowerName.contains("xbox") || lowerName.equals("pc") ||
                lowerName.contains("soundtrack") || lowerName.contains("dlc") || lowerName.contains("bundle") ||
                lowerName.contains("edition") || lowerName.contains("season pass") || lowerName.contains("patch") ||
                lowerName.contains("mod"))
            return false;
        return true;
    }

    private String unescapeJava(String st) {
        if (st == null)
            return "";
        return st.replace("\\u0027", "'").replace("\\u0026", "&").replace("\\/", "/").replace("\\", "");
    }
}