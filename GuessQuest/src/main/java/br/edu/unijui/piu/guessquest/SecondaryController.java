package br.edu.unijui.piu.guessquest;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Random;

public class SecondaryController {

    private static final String API_KEY = "e6234549ffcb449f9502ec12c04a4201";
    private static final String API_URL = "https://api.rawg.io/api/games?key=%s&ordering=-added&metacritic=85,100&page_size=1&page=%d";

    @FXML private ImageView gameImageView;
    @FXML private ProgressIndicator loadingSpinner;
    @FXML private Label scoreLabel;
    @FXML private Label livesLabel;
    @FXML private Label feedbackLabel;
    @FXML private Label hintLabel;
    @FXML private TextField guessField;
    @FXML private Button submitButton;
    @FXML private StackPane monitorPane;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Gson gson = new Gson();
    private final Random random = new Random();

    // Game State
    private String currentGameName = "";
    private Image originalImage; 
    private int score = 0;
    private int lives = 0;
    private int currentPixelationWidth = 35; 

    @FXML
    public void initialize() {
        this.lives = App.currentDifficulty.lives;
        
        // Responsividade da imagem
        gameImageView.fitWidthProperty().bind(monitorPane.widthProperty().subtract(40));
        gameImageView.fitHeightProperty().bind(monitorPane.heightProperty().subtract(40));
        
        updateUI();
        loadNextGame();
    }

    @FXML
    private void onGuess() {
        String userGuess = guessField.getText().trim();
        if (userGuess.isEmpty()) return;

        if (isCorrectAnswer(userGuess, currentGameName)) {
            int points = (int) (100 * App.currentDifficulty.multiplier);
            score += points;
            feedbackLabel.setText("CORRECT! " + currentGameName);
            feedbackLabel.setStyle("-fx-text-fill: #00ff00;");
            finishRound(true);
        } else {
            lives--;
            feedbackLabel.setText("TRY AGAIN!");
            feedbackLabel.setStyle("-fx-text-fill: #ff4444;");
            
            hintLabel.setText(generateWordleHint(userGuess, currentGameName));
            
            currentPixelationWidth = (int) (currentPixelationWidth * 1.5);
            applyPixelation(originalImage);

            if (lives <= 0) {
                gameOver();
            } else {
                updateUI();
            }
        }
        guessField.clear();
        guessField.requestFocus();
    }
    
    // --- NOVO MÉTODO: Pular Jogo ---
    @FXML
    private void onSkip() {
        // Penalidade de 5 pontos
        score = Math.max(0, score - 5);
        
        // Mostra o nome do jogo
        feedbackLabel.setText("SKIPPED! WAS: " + currentGameName);
        feedbackLabel.setStyle("-fx-text-fill: #ffaa00;");
        
        // Revela imagem e aguarda, similar ao acerto
        finishRound(true);
    }

    private String generateWordleHint(String guess, String answer) {
        guess = guess.toUpperCase();
        answer = answer.toUpperCase();
        StringBuilder hint = new StringBuilder();
        
        for (int i = 0; i < answer.length(); i++) {
            char targetChar = answer.charAt(i);
            
            // Se for símbolo ou espaço, mostra direto
            if (!Character.isLetterOrDigit(targetChar)) {
                hint.append(targetChar).append(" ");
                continue;
            }
            
            // Lógica estilo Wordle/Forca simplificada
            // Se acertou a letra na posição, mostra a letra. Senão, mostra underscore.
            if (i < guess.length() && guess.charAt(i) == targetChar) {
                hint.append(targetChar).append(" "); // Sem colchetes para não bugar o layout
            } else {
                hint.append("_ ");
            }
        }
        return hint.toString();
    }

    @FXML
    private void onGiveUp() throws IOException {
        gameOver();
    }
    
    private void finishRound(boolean won) {
        submitButton.setDisable(true);
        updateUI();
        
        if (won) {
            gameImageView.setImage(originalImage);
            gameImageView.setEffect(null);
        }

        new Thread(() -> {
            try { Thread.sleep(2500); } catch (Exception e) {} // Tempo para ler o nome
            Platform.runLater(() -> {
                if (lives > 0) loadNextGame();
            });
        }).start();
    }

    private void gameOver() {
        feedbackLabel.setText("GAME OVER");
        App.addScore(score);
        submitButton.setDisable(true);
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("GAME OVER");
        alert.setHeaderText("GAME OVER");
        alert.setContentText("FINAL SCORE: " + score + "\nANSWER: " + currentGameName);
        alert.showAndWait();

        try {
            App.setRoot("primary");
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void loadNextGame() {
        Platform.runLater(() -> {
            feedbackLabel.setText("LOADING CARTRIDGE...");
            feedbackLabel.setStyle("-fx-text-fill: #aaaaaa;");
            hintLabel.setText("");
            guessField.setDisable(true);
            submitButton.setDisable(true);
            loadingSpinner.setVisible(true);
            gameImageView.setOpacity(0.5);
            
            currentPixelationWidth = 35; 
        });

        int page = random.nextInt(400) + 1;
        String url = String.format(API_URL, API_KEY, page);
        
        httpClient.sendAsync(HttpRequest.newBuilder(URI.create(url)).build(), HttpResponse.BodyHandlers.ofString())
                .thenAccept(resp -> Platform.runLater(() -> processGame(resp.body())))
                .exceptionally(e -> { 
                     Platform.runLater(() -> feedbackLabel.setText("CONNECTION ERROR")); 
                     return null; 
                });
    }

    private void processGame(String json) {
        try {
            JsonObject root = gson.fromJson(json, JsonObject.class);
            JsonArray results = root.getAsJsonArray("results");
            if (results.size() == 0) { loadNextGame(); return; }

            JsonObject game = results.get(0).getAsJsonObject();
            currentGameName = game.get("name").getAsString();
            String imgUrl = game.get("background_image").getAsString();

            originalImage = new Image(imgUrl, true);
            originalImage.progressProperty().addListener((obs, old, val) -> {
                if (val.doubleValue() >= 1.0) {
                    applyPixelation(originalImage);
                    loadingSpinner.setVisible(false);
                    gameImageView.setOpacity(1.0);
                    guessField.setDisable(false);
                    submitButton.setDisable(false);
                    guessField.requestFocus();
                    feedbackLabel.setText("WHO IS THIS?");
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            loadNextGame();
        }
    }
    
    private void applyPixelation(Image source) {
        if (source == null || source.getWidth() == 0) return;
        
        ImageView tempView = new ImageView(source);
        tempView.setFitWidth(currentPixelationWidth); 
        tempView.setPreserveRatio(true);
        tempView.setSmooth(false); 
        
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.BLACK);
        WritableImage pixelatedParams = tempView.snapshot(params, null);
        
        gameImageView.setImage(pixelatedParams);
        gameImageView.setSmooth(false); 
    }

    private void updateUI() {
        scoreLabel.setText(String.format("SCORE %05d", score));
        livesLabel.setText("HP " + "♥".repeat(Math.max(0, lives)));
    }

    private boolean isCorrectAnswer(String guess, String correct) {
        String cleanGuess = guess.toLowerCase().replaceAll("[^a-z0-9]", "");
        String cleanCorrect = correct.toLowerCase().replaceAll("[^a-z0-9]", "");
        return cleanCorrect.contains(cleanGuess) && cleanGuess.length() > 2;
    }
}