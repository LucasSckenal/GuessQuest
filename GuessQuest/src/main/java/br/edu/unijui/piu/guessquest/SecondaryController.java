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
    // Aumentei page_size para 40 para ter mais chances de achar jogos válidos numa única requisição

    private static final Set<String> INVALID_EXACT_NAMES = new HashSet<>();
    static {
        INVALID_EXACT_NAMES.add("PC");
        INVALID_EXACT_NAMES.add("PlayStation");
        INVALID_EXACT_NAMES.add("Xbox");
        // ... (lista mantida e expandida na lógica)
    }

    @FXML private Label lblPhase;
    @FXML private Label lblScore;
    @FXML private Label lblLives;
    @FXML private Label lblPlayer;
    @FXML private ImageView gameImage;
    @FXML private Button btnA, btnB, btnC, btnD;
    @FXML private Label lblFeedback; 
    @FXML private VBox loadingLayer; 

    private GameState state;
    private String correctGameName;
    private final Random random = new Random();
    private boolean interactionLocked = false;

    @FXML
    public void initialize() {
        state = GameState.getInstance();
        updateUIHeader();
        loadNextLevel();
    }

    private void updateUIHeader() {
        lblPlayer.setText("JOGADOR: " + state.getPlayerName());
        lblPhase.setText(String.format("FASE: %02d-15", state.getCurrentPhase()));
        lblScore.setText(String.format("PONTOS: %06d", state.getCurrentScore()));
        
        // MUDANÇA: Usando Joystick 🕹 ao invés de Coração ❤
        StringBuilder livesStr = new StringBuilder("VIDAS: ");
        for (int i = 0; i < state.getCurrentLives(); i++) {
            livesStr.append("🕹 "); 
        }
        lblLives.setText(livesStr.toString());
    }

    private void loadNextLevel() {
        if (state.getCurrentPhase() > 15) {
            endGame(true);
            return;
        }

        interactionLocked = true;
        lblFeedback.setText("CARREGANDO DADOS...");
        loadingLayer.setVisible(true);

        new Thread(() -> {
            try {
                GameData levelData = null;
                int attempts = 0;
                
                // Tenta buscar dados válidos até 3 vezes
                while (attempts < 3) {
                    // Se for a última tentativa, força página 1 para garantir dados
                    boolean forceSafePage = (attempts == 2); 
                    levelData = fetchGameData(forceSafePage);
                    
                    if (!levelData.correctName.equals("API Error") && !levelData.options.isEmpty()) {
                        break;
                    }
                    attempts++;
                    Thread.sleep(500); // Pequena pausa antes de tentar de novo
                }
                
                // Se ainda assim falhar (ex: sem internet), usa o Fallback de Emergência
                if (levelData == null || levelData.options.isEmpty() || levelData.correctName.equals("API Error")) {
                    levelData = getEmergencyFallbackData();
                }
                
                final GameData finalData = levelData;
                Platform.runLater(() -> setupLevelUI(finalData));
            } catch (Exception e) {
                e.printStackTrace();
                // Em caso de crash total da thread, carrega fallback na UI
                Platform.runLater(() -> setupLevelUI(getEmergencyFallbackData()));
            }
        }).start();
    }

    // Dados de emergência para o jogo nunca travar "Sem Imagem"
    private GameData getEmergencyFallbackData() {
        GameData data = new GameData();
        data.correctName = "Pac-Man";
        // URL de imagem confiável ou placeholder
        data.imageUrl = "https://media.rawg.io/media/games/b21/b21555abc69d04d9b5d7da855e70d858.jpg"; 
        data.options.add("Pac-Man");
        data.options.add("Tetris");
        data.options.add("Space Invaders");
        data.options.add("Pong");
        Collections.shuffle(data.options);
        return data;
    }

    private void setupLevelUI(GameData data) {
        loadingLayer.setVisible(false);
        interactionLocked = false;
        lblFeedback.setText(""); 

        this.correctGameName = data.correctName;

        try {
            if (data.imageUrl != null && !data.imageUrl.isEmpty()) {
                gameImage.setImage(new Image(data.imageUrl, true)); // true = load in background
            } else {
                gameImage.setImage(null);
                lblFeedback.setText("SEM IMAGEM");
            }
        } catch (Exception e) {
            lblFeedback.setText("ERRO NA IMAGEM");
        }

        // Garante 4 opções mesmo se a API falhar parcialmente
        List<String> options = data.options;
        int backupCount = 1;
        while(options.size() < 4) {
            options.add("Mystery Game " + backupCount++);
        }
        
        btnA.setText("A) " + options.get(0));
        btnB.setText("B) " + options.get(1));
        btnC.setText("C) " + options.get(2));
        btnD.setText("D) " + options.get(3));
        
        resetButtonStyles();
    }

    @FXML
    private void handleAnswer(javafx.event.ActionEvent event) {
        if (interactionLocked) return;
        
        Button clickedButton = (Button) event.getSource();
        String text = clickedButton.getText();
        String selectedAnswer = text.length() > 3 ? text.substring(3) : text;
        
        interactionLocked = true;

        if (selectedAnswer.equalsIgnoreCase(correctGameName)) {
            // ACERTOU
            clickedButton.getStyleClass().add("button-correct");
            lblFeedback.setText("CORRETO! +1000 PTS");
            lblFeedback.setStyle("-fx-text-fill: #00ff00;");
            
            // CORREÇÃO: Usa .wav conforme seus arquivos
            SoundManager.getInstance().playSound("correct.wav");
            
            state.addScore(1000);
            
            new java.util.Timer().schedule(new java.util.TimerTask() {
                @Override public void run() {
                    Platform.runLater(() -> {
                        state.nextPhase();
                        updateUIHeader();
                        loadNextLevel();
                    });
                }
            }, 1500);

        } else {
            // ERROU
            clickedButton.getStyleClass().add("button-wrong");
            lblFeedback.setText("ERRADO! PERDEU VIDA");
            lblFeedback.setStyle("-fx-text-fill: #ff3333;");
            
            // CORREÇÃO: Usa .wav
            SoundManager.getInstance().playSound("wrong.wav");
            
            state.decreaseLife();
            updateUIHeader();

            if (state.getCurrentLives() <= 0) {
                endGame(false);
            } else {
                new java.util.Timer().schedule(new java.util.TimerTask() {
                     @Override public void run() {
                         Platform.runLater(() -> {
                             state.nextPhase();
                             updateUIHeader();
                             loadNextLevel();
                         });
                     }
                 }, 1500);
            }
        }
    }

    private void endGame(boolean win) {
        loadingLayer.setVisible(true);
        LeaderboardManager.saveScore(state.getPlayerName(), state.getCurrentScore());

        if (win) {
            lblFeedback.setText("PARABÉNS! VOCÊ ZEROU!");
            lblFeedback.setStyle("-fx-text-fill: gold;");
            // Se você não tiver win.wav, pode comentar ou converter o arquivo
            SoundManager.getInstance().playSound("win.wav"); 
        } else {
            lblFeedback.setText("GAME OVER");
            lblFeedback.setStyle("-fx-text-fill: red;");
            // Se você não tiver gameover.mp3/wav, o som simplesmente não toca (sem erro)
            SoundManager.getInstance().playSound("gameover.wav");
        }
        
        new java.util.Timer().schedule(new java.util.TimerTask() {
            @Override public void run() {
                Platform.runLater(() -> {
                    try { App.setRoot("primary"); } catch (IOException e) { e.printStackTrace(); }
                });
            }
        }, 4000);
    }

    private void resetButtonStyles() {
        String baseStyle = "game-button";
        btnA.getStyleClass().setAll(baseStyle);
        btnB.getStyleClass().setAll(baseStyle);
        btnC.getStyleClass().setAll(baseStyle);
        btnD.getStyleClass().setAll(baseStyle);
    }

    // Adicionado parametro forceSafePage
    private GameData fetchGameData(boolean forceSafePage) throws Exception {
        int pageBase;
        
        if (forceSafePage) {
            pageBase = 1; // Página segura garantida
        } else {
            pageBase = random.nextInt(50) + 1; 
            if (state.getCurrentPhase() > 5) pageBase += 100;
            // No modo SOULS arrisca páginas profundas, mas se falhar o retry cai no forceSafePage
            if (state.getDifficulty() == GameState.Difficulty.SOULS) pageBase = random.nextInt(500) + 1;
        }

        String metacriticFilter = state.getDifficulty().metacriticRange;
        // Relaxa filtro em fases avançadas ou fallback
        if ((state.getCurrentPhase() > 10 && state.getDifficulty() != GameState.Difficulty.SOULS) || forceSafePage) {
            metacriticFilter = "50,100";
        }

        String url = String.format(API_URL_TEMPLATE, API_KEY, metacriticFilter, pageBase);
        
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        return parseJsonManually(response.body());
    }

    private static class GameData {
        String correctName = "API Error";
        String imageUrl;
        List<String> options = new ArrayList<>();
    }

    private GameData parseJsonManually(String json) {
        GameData data = new GameData();
        List<String> namesFound = new ArrayList<>();
        List<String> imagesFound = new ArrayList<>();

        // Regex ajustada
        Pattern gamePattern = Pattern.compile("\"slug\":\"(?<slug>[^\"]+)\"[^\\{\\[]*?\"name\":\"(?<name>[^\"]+)\"");
        Matcher matcher = gamePattern.matcher(json);

        while (matcher.find()) {
            String name = unescapeJava(matcher.group("name"));
            String slug = matcher.group("slug");
            
            if (isValidGameName(name, slug)) {
                namesFound.add(name);
            }
        }

        Pattern imgPattern = Pattern.compile("\"background_image\":\"(?<url>[^\"]+)\"");
        Matcher imgMatcher = imgPattern.matcher(json);
        while (imgMatcher.find()) {
            imagesFound.add(unescapeJava(imgMatcher.group("url")));
        }
        
        if (namesFound.isEmpty()) {
            return data;
        }

        // Tenta pegar aleatório, mas garante limites
        int maxIndex = Math.min(namesFound.size(), 4);
        int correctIndex = random.nextInt(maxIndex);
        
        data.correctName = namesFound.get(correctIndex);
        
        if (correctIndex < imagesFound.size()) {
            data.imageUrl = imagesFound.get(correctIndex);
        } else if (!imagesFound.isEmpty()) {
            data.imageUrl = imagesFound.get(0);
        }

        data.options.add(data.correctName);
        for (int i = 0; i < namesFound.size(); i++) {
            if (data.options.size() < 4) {
                String n = namesFound.get(i);
                if (!data.options.contains(n)) {
                    data.options.add(n);
                }
            }
        }
        
        Collections.shuffle(data.options);
        return data;
    }

    private boolean isValidGameName(String name, String slug) {
        if (name == null || name.trim().isEmpty()) return false;
        if (INVALID_EXACT_NAMES.contains(name)) return false; // Usa a lista static definida no início
        
        // Bloqueia caracteres não latinos (Russo, Chinês, etc)
        // Isso evita "Для одного игрока"
        if (name.matches(".*[^\\x00-\\x7F\\p{L}\\p{N}\\s\\p{P}].*") && !name.matches(".*[áéíóúãõçÁÉÍÓÚÃÕÇ].*")) {
             // A regex acima é estrita para ASCII, mas permitimos acentos portugueses básicos.
             // Se tiver caracteres muito estranhos (cirílico), rejeita.
             if (name.matches(".*\\p{InCyrillic}.*")) return false;
        }
        
        String lowerName = name.toLowerCase();
        if (lowerName.contains("playstation") || 
            lowerName.contains("xbox") || 
            lowerName.equals("pc") ||
            lowerName.contains("soundtrack") || 
            lowerName.contains("dlc") ||        
            lowerName.contains("bundle") ||
            lowerName.contains("edition") || // Evita "Deluxe Edition" como nome de jogo
            lowerName.contains("season pass")) {     
            return false;
        }
        
        return true;
    }
    
    private String unescapeJava(String st) {
        if (st == null) return "";
        return st.replace("\\u0027", "'")
                 .replace("\\u0026", "&")
                 .replace("\\/", "/")
                 .replace("\\", "");
    }
}