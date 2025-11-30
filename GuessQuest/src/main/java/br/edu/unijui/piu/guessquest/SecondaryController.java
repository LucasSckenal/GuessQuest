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
    private static final String API_URL_TEMPLATE = "https://api.rawg.io/api/games?key=%s&ordering=-added&metacritic=%s&page_size=4&page=%d";

    // Lista negra expandida para remover plataformas E tags comuns que a API retorna como "name"
    private static final Set<String> INVALID_EXACT_NAMES = new HashSet<>();
    static {
        // Plataformas
        INVALID_EXACT_NAMES.add("PC");
        INVALID_EXACT_NAMES.add("PlayStation");
        INVALID_EXACT_NAMES.add("Xbox");
        INVALID_EXACT_NAMES.add("Nintendo Switch");
        INVALID_EXACT_NAMES.add("macOS");
        INVALID_EXACT_NAMES.add("Linux");
        INVALID_EXACT_NAMES.add("Android");
        INVALID_EXACT_NAMES.add("iOS");
        // Tags e Gêneros comuns (Inglês e Russo comum na API)
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
        
        StringBuilder livesStr = new StringBuilder("VIDAS: ");
        for (int i = 0; i < state.getCurrentLives(); i++) {
            livesStr.append("❤ ");
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
                // Loop de segurança: Se a API retornar só lixo (tags), tenta outra página
                GameData levelData = null;
                int attempts = 0;
                while (attempts < 3) {
                    levelData = fetchGameData();
                    if (!levelData.correctName.equals("API Error")) {
                        break;
                    }
                    attempts++;
                }
                
                final GameData finalData = levelData;
                Platform.runLater(() -> setupLevelUI(finalData));
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    lblFeedback.setText("ERRO DE CONEXÃO. RETENTANDO...");
                    new java.util.Timer().schedule(new java.util.TimerTask() {
                        @Override public void run() { Platform.runLater(() -> loadNextLevel()); }
                    }, 2000);
                });
            }
        }).start();
    }

    private void setupLevelUI(GameData data) {
        loadingLayer.setVisible(false);
        interactionLocked = false;
        lblFeedback.setText(""); 

        this.correctGameName = data.correctName;

        try {
            if (data.imageUrl != null && !data.imageUrl.isEmpty()) {
                gameImage.setImage(new Image(data.imageUrl, true));
            } else {
                gameImage.setImage(null);
                lblFeedback.setText("SEM IMAGEM");
            }
        } catch (Exception e) {
            lblFeedback.setText("ERRO NA IMAGEM");
        }

        List<String> options = data.options;
        while(options.size() < 4) options.add("Unknown Game " + (options.size()+1));
        
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
            state.addScore(1000);
            
            new java.util.Timer().schedule( 
                new java.util.TimerTask() {
                    @Override
                    public void run() {
                        Platform.runLater(() -> {
                            state.nextPhase();
                            updateUIHeader();
                            loadNextLevel();
                        });
                    }
                }, 
                1500 
            );

        } else {
            clickedButton.getStyleClass().add("button-wrong");
            lblFeedback.setText("ERRADO! PERDEU VIDA");
            lblFeedback.setStyle("-fx-text-fill: #ff3333;");
            state.decreaseLife();
            updateUIHeader();

            if (state.getCurrentLives() <= 0) {
                endGame(false);
            } else {
                new java.util.Timer().schedule( 
                     new java.util.TimerTask() {
                         @Override
                         public void run() {
                             Platform.runLater(() -> {
                                 state.nextPhase();
                                 updateUIHeader();
                                 loadNextLevel();
                             });
                         }
                     }, 
                     1500 
                 );
            }
        }
    }

    private void endGame(boolean win) {
        loadingLayer.setVisible(true);
        LeaderboardManager.saveScore(state.getPlayerName(), state.getCurrentScore());

        if (win) {
            lblFeedback.setText("PARABÉNS! VOCÊ ZEROU!");
            lblFeedback.setStyle("-fx-text-fill: gold;");
        } else {
            lblFeedback.setText("GAME OVER");
            lblFeedback.setStyle("-fx-text-fill: red;");
        }
        
        new java.util.Timer().schedule(
            new java.util.TimerTask() {
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
            },
            4000
        );
    }

    private void resetButtonStyles() {
        String baseStyle = "game-button";
        btnA.getStyleClass().setAll(baseStyle);
        btnB.getStyleClass().setAll(baseStyle);
        btnC.getStyleClass().setAll(baseStyle);
        btnD.getStyleClass().setAll(baseStyle);
    }

    private GameData fetchGameData() throws Exception {
        int pageBase = random.nextInt(50) + 1; 
        if (state.getCurrentPhase() > 5) pageBase += 100;
        if (state.getDifficulty() == GameState.Difficulty.SOULS) pageBase = random.nextInt(500) + 1;

        String metacriticFilter = state.getDifficulty().metacriticRange;
        // Se for fase avançada, relaxa filtro para ter mais variedade, mas evita lixo
        if (state.getCurrentPhase() > 10 && state.getDifficulty() != GameState.Difficulty.SOULS) {
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

        // REGEX RIGOROSA: 
        // 1. Procura "slug": "valor"
        // 2. Procura imediatamente "name": "valor" 
        // 3. NÃO permite abrir chaves { ou colchetes [ entre eles (evita entrar em tags/genres)
        Pattern gamePattern = Pattern.compile("\"slug\":\"(?<slug>[^\"]+)\"[^\\{\\[]*?\"name\":\"(?<name>[^\"]+)\"");
        Matcher matcher = gamePattern.matcher(json);

        while (matcher.find()) {
            String name = unescapeJava(matcher.group("name"));
            String slug = matcher.group("slug");
            
            if (isValidGameName(name, slug)) {
                namesFound.add(name);
            }
        }

        // Regex Imagens
        Pattern imgPattern = Pattern.compile("\"background_image\":\"(?<url>[^\"]+)\"");
        Matcher imgMatcher = imgPattern.matcher(json);
        while (imgMatcher.find()) {
            imagesFound.add(unescapeJava(imgMatcher.group("url")));
        }
        
        if (namesFound.isEmpty()) {
            return data; // Retorna erro para tentar outra página
        }

        int maxIndex = Math.min(namesFound.size(), 4);
        int correctIndex = random.nextInt(maxIndex);
        
        data.correctName = namesFound.get(correctIndex);
        
        // Tenta associar imagem
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

        // 1. Blacklist exata (Gêneros, Tags, Plataformas)
        if (INVALID_EXACT_NAMES.contains(name)) return false;
        
        // 2. Filtro de caracteres: Só aceita Latin (e números/pontuação comum)
        // Rejeita Cirílico (Russo), CJK (Chinês/Japones), etc.
        // Regex: Se tiver qualquer caractere que NÃO seja ASCII estendido básico, rejeita.
        // Ou mais simples: rejeita blocos Unicode específicos.
        // Aqui verificamos se tem caracteres cirílicos
        if (name.matches(".*\\p{InCyrillic}.*")) return false; 
        
        // 3. Filtro heurístico por palavras chave no slug ou nome
        String lowerName = name.toLowerCase();
        if (lowerName.contains("playstation") || 
            lowerName.contains("xbox") || 
            lowerName.equals("pc") ||
            lowerName.contains("soundtrack") || // Remove trilhas sonoras
            lowerName.contains("dlc") ||        // Remove DLCs
            lowerName.contains("bundle")) {     // Remove pacotes
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