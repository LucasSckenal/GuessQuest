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
    // Mantemos page_size=40 para ter um pool grande de alternativas e evitar repetição
    private static final String API_URL_TEMPLATE = "https://api.rawg.io/api/games?key=%s&ordering=-added&metacritic=%s&page_size=40&page=%d"; 

    // Conjunto estático para armazenar nomes de jogos já usados nesta sessão
    private static final Set<String> gamesPlayedThisSession = new HashSet<>();

    private static final Set<String> INVALID_EXACT_NAMES = new HashSet<>();
    static {
        // Plataformas e lojas
        INVALID_EXACT_NAMES.add("PC");
        INVALID_EXACT_NAMES.add("PlayStation");
        INVALID_EXACT_NAMES.add("Xbox");
        INVALID_EXACT_NAMES.add("Nintendo Switch");
        INVALID_EXACT_NAMES.add("macOS");
        INVALID_EXACT_NAMES.add("Linux");
        INVALID_EXACT_NAMES.add("Android");
        INVALID_EXACT_NAMES.add("iOS");
        INVALID_EXACT_NAMES.add("Steam");
        INVALID_EXACT_NAMES.add("Ubisoft Connect");
        INVALID_EXACT_NAMES.add("GOG");
        INVALID_EXACT_NAMES.add("Epic Games");
        
        // Tags e Gêneros comuns que vazam
        INVALID_EXACT_NAMES.add("Singleplayer");
        INVALID_EXACT_NAMES.add("Multiplayer");
        INVALID_EXACT_NAMES.add("Co-op");
        INVALID_EXACT_NAMES.add("Action");
        INVALID_EXACT_NAMES.add("Adventure");
        INVALID_EXACT_NAMES.add("RPG");
        INVALID_EXACT_NAMES.add("Strategy");
        INVALID_EXACT_NAMES.add("Shooter");
        INVALID_EXACT_NAMES.add("Puzzle");
        INVALID_EXACT_NAMES.add("Casual");
        INVALID_EXACT_NAMES.add("Indie");
        INVALID_EXACT_NAMES.add("Simulation");
        INVALID_EXACT_NAMES.add("Arcade");
        INVALID_EXACT_NAMES.add("Platformer");
        INVALID_EXACT_NAMES.add("Racing");
        INVALID_EXACT_NAMES.add("Sports");
        INVALID_EXACT_NAMES.add("Family");
        INVALID_EXACT_NAMES.add("Fighting");
        INVALID_EXACT_NAMES.add("Board Games");
        INVALID_EXACT_NAMES.add("Educational");
        INVALID_EXACT_NAMES.add("Card");
        INVALID_EXACT_NAMES.add("Massively Multiplayer");
        INVALID_EXACT_NAMES.add("Anime");
        INVALID_EXACT_NAMES.add("Comedy");
        INVALID_EXACT_NAMES.add("Post-apocalyptic");
        INVALID_EXACT_NAMES.add("Space");
        INVALID_EXACT_NAMES.add("Zombies");
        INVALID_EXACT_NAMES.add("Horror");
        INVALID_EXACT_NAMES.add("Survival");
        INVALID_EXACT_NAMES.add("Open World");
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
        
        // Se for a primeira fase, limpa o histórico de jogos para começar do zero
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
                    levelData = getEmergencyFallbackData();
                }
                
                final GameData finalData = levelData;
                Platform.runLater(() -> setupLevelUI(finalData));
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> setupLevelUI(getEmergencyFallbackData()));
            }
        }).start();
    }

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

    private void setupLevelUI(GameData data) {
        // --- LÓGICA DE PROTEÇÃO CONTRA IMAGEM VAZIA ---
        // Se a imagem for nula ou vazia, em vez de mostrar "SEM IMAGEM",
        // recarregamos o nível (buscamos outro jogo)
        if (data.imageUrl == null || data.imageUrl.isEmpty()) {
            System.out.println("Jogo " + data.correctName + " sem imagem. Tentando outro...");
            loadNextLevel();
            return;
        }
        // ----------------------------------------------

        loadingLayer.setVisible(false);
        interactionLocked = false;
        lblFeedback.setText(""); 

        this.correctGameName = data.correctName;
        // Adiciona o jogo atual à lista de jogos usados para não repetir
        gamesPlayedThisSession.add(data.correctName);

        try {
            // Como já validamos null/empty acima, podemos criar a imagem direto
            gameImage.setImage(new Image(data.imageUrl, true)); 
        } catch (Exception e) {
            // Se der erro ao carregar (ex: URL quebrada), também tentamos outro
            loadNextLevel();
            return;
        }

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
            clickedButton.getStyleClass().add("button-correct");
            lblFeedback.setText("CORRETO! +1000 PTS");
            lblFeedback.setStyle("-fx-text-fill: #00ff00;");
            
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
            clickedButton.getStyleClass().add("button-wrong");
            lblFeedback.setText("ERRADO! PERDEU VIDA");
            lblFeedback.setStyle("-fx-text-fill: #ff3333;");
            
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
            SoundManager.getInstance().playSound("win.wav"); 
        } else {
            lblFeedback.setText("GAME OVER");
            lblFeedback.setStyle("-fx-text-fill: red;");
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

    private GameData fetchGameData(boolean forceSafePage) throws Exception {
        int pageBase;
        
        if (forceSafePage) {
            pageBase = 1; 
        } else {
            pageBase = random.nextInt(50) + 1; 
            if (state.getCurrentPhase() > 5) pageBase += 100;
            if (state.getDifficulty() == GameState.Difficulty.SOULS) pageBase = random.nextInt(500) + 1;
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

    private static class GameData {
        String correctName = "API Error";
        String imageUrl;
        List<String> options = new ArrayList<>();
    }

    // Classe auxiliar para manter nome e imagem juntos
    private static class GameCandidate {
        String name;
        String image;
        
        GameCandidate(String n, String i) {
            this.name = n;
            this.image = i;
        }
    }

    private GameData parseJsonManually(String json) {
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
            
            int limit = (i < matchEnds.size() - 1) ? matchEnds.get(i+1) : json.length();
            if (limit - currentEnd > 2000) limit = currentEnd + 2000; 

            if (isValidGameName(name, slug)) {
                String image = extractImageInRange(json, currentEnd, limit);
                candidates.add(new GameCandidate(name, image));
            }
        }
        
        if (candidates.isEmpty()) {
            return data;
        }

        // Filtra jogos já jogados e AGORA TAMBÉM jogos sem imagem
        List<GameCandidate> available = new ArrayList<>();
        for (GameCandidate cand : candidates) {
            boolean hasImage = cand.image != null && !cand.image.isEmpty() && !cand.image.equals("null");
            if (!gamesPlayedThisSession.contains(cand.name) && hasImage) {
                available.add(cand);
            }
        }

        if (available.isEmpty()) available = candidates; 

        // Sorteia o jogo CORRETO
        int correctIndex = random.nextInt(available.size());
        GameCandidate correctGame = available.get(correctIndex);
        
        data.correctName = correctGame.name;
        data.imageUrl = correctGame.image;
        data.options.add(correctGame.name);

        // Escolhe opções ERRADAS (podem ser de qualquer candidato válido da página)
        List<String> wrongPool = new ArrayList<>();
        for (GameCandidate cand : candidates) {
            if (!cand.name.equals(correctGame.name)) {
                wrongPool.add(cand.name);
            }
        }
        Collections.shuffle(wrongPool);
        
        for (int k = 0; k < 3 && k < wrongPool.size(); k++) {
            data.options.add(wrongPool.get(k));
        }
        
        Collections.shuffle(data.options);
        return data;
    }

    private String extractImageInRange(String json, int start, int end) {
        String snippet = json.substring(start, Math.min(end, json.length()));
        // Busca simples pela chave de imagem neste pedaço
        int idx = snippet.indexOf("\"background_image\":\"");
        if (idx != -1) {
            int urlStart = idx + 20; // tamanho de "background_image":"
            int urlEnd = snippet.indexOf("\"", urlStart);
            if (urlEnd != -1) {
                return unescapeJava(snippet.substring(urlStart, urlEnd));
            }
        }
        return null;
    }

    private boolean isValidGameName(String name, String slug) {
        if (name == null || name.trim().isEmpty()) return false;
        
        // 1. BLOQUEIO IMEDIATO DE CIRÍLICO/RUSSO
        if (name.matches(".*\\p{InCyrillic}.*")) return false;

        // 2. Filtro de nomes exatos proibidos (agora expandido)
        if (INVALID_EXACT_NAMES.contains(name)) return false; 
        
        // 3. Filtro de lixo (caracteres estranhos mas sem acentos comuns)
        if (name.matches(".*[^\\x00-\\x7F\\p{L}\\p{N}\\s\\p{P}].*") && !name.matches(".*[áéíóúãõçÁÉÍÓÚÃÕÇ].*")) {
             return false;
        }
        
        String lowerName = name.toLowerCase();
        // 4. Palavras-chave proibidas
        if (lowerName.contains("playstation") || 
            lowerName.contains("xbox") || 
            lowerName.equals("pc") ||
            lowerName.contains("soundtrack") || 
            lowerName.contains("dlc") ||        
            lowerName.contains("bundle") ||
            lowerName.contains("edition") ||
            lowerName.contains("season pass") ||
            lowerName.contains("patch") ||
            lowerName.contains("mod")) {     
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