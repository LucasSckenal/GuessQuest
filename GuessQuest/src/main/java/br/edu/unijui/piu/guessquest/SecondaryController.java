package br.edu.unijui.piu.guessquest;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Random;

public class SecondaryController {

    // --- CONFIGURAÇÕES ---
    private static final String API_KEY = "e6234549ffcb449f9502ec12c04a4201";

    // URL AJUSTADA:
    // - page_size=1: Traz apenas 1 jogo (o escolhido).
    // - ordering=-added: Ordena pelos mais populares (que mais pessoas têm na biblioteca).
    // - metacritic=80,100: Apenas jogos com nota entre 80 e 100 (Só os famosos/bons).
    private static final String API_URL_TEMPLATE = "https://api.rawg.io/api/games?key=%s&ordering=-added&metacritic=80,100&page_size=1&page=%d";

    @FXML private ImageView gameImageView;
    @FXML private ProgressIndicator loadingSpinner;
    @FXML private Label scoreLabel;
    @FXML private Label feedbackLabel;
    @FXML private TextField guessField;
    @FXML private Button submitButton;
    @FXML private Button nextButton;
    @FXML private Button backButton;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Gson gson = new Gson();
    private final Random random = new Random();
    
    // Removemos a fila (Queue) para buscar um por um direto da API
    private String currentGameName = "";
    private int score = 0;

    @FXML
    public void initialize() {
        loadNextGame();
    }

    @FXML
    private void switchToPrimary() throws IOException {
        App.setRoot("primary");
    }

    @FXML
    private void onGuess() {
        String userGuess = guessField.getText().trim();
        if (userGuess.isEmpty()) return;

        if (isCorrectAnswer(userGuess, currentGameName)) {
            score += 10;
            updateScoreUI();
            feedbackLabel.setText("CORRECT! IT WAS " + currentGameName.toUpperCase());
            feedbackLabel.setTextFill(Color.LIGHTGREEN);
            
            submitButton.setDisable(true);
            // Espera 2 segundos e carrega o próximo
            new Thread(() -> {
                try { Thread.sleep(2000); } catch (InterruptedException e) {}
                Platform.runLater(this::loadNextGame);
            }).start();
        } else {
            feedbackLabel.setText("WRONG! TRY AGAIN.");
            feedbackLabel.setTextFill(Color.SALMON);
        }
    }

    @FXML
    private void onNextGame() {
        score = Math.max(0, score - 5);
        updateScoreUI();
        loadNextGame();
    }

    private void loadNextGame() {
        // Prepara a UI para o carregamento
        feedbackLabel.setText("LOADING...");
        feedbackLabel.setTextFill(Color.LIGHTGRAY);
        guessField.clear();
        submitButton.setDisable(true);
        guessField.setDisable(true);
        loadingSpinner.setVisible(true);
        gameImageView.setOpacity(0.3); // Deixa a imagem anterior transparente

        fetchRandomGame();
    }

    private void fetchRandomGame() {
        // Sorteia uma página entre 1 e 500.
        // Como estamos filtrando por popularidade e nota alta, 
        // os primeiros 500 resultados são garantidos de serem jogos conhecidos.
        int randomPage = random.nextInt(500) + 1;
        
        String url = String.format(API_URL_TEMPLATE, API_KEY, randomPage);
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(responseBody -> Platform.runLater(() -> processApiResponse(responseBody)))
                .exceptionally(e -> {
                    Platform.runLater(() -> {
                        feedbackLabel.setText("CONNECTION ERROR");
                        loadingSpinner.setVisible(false);
                    });
                    return null;
                });
    }

    private void processApiResponse(String jsonResponse) {
        try {
            JsonObject jsonObject = gson.fromJson(jsonResponse, JsonObject.class);
            JsonArray results = jsonObject.getAsJsonArray("results");

            if (results.size() > 0) {
                // Pega o ÚNICO jogo retornado (índice 0)
                JsonObject game = results.get(0).getAsJsonObject();
                displayGame(game);
            } else {
                feedbackLabel.setText("NO GAMES FOUND");
                loadingSpinner.setVisible(false);
            }
        } catch (Exception e) {
            e.printStackTrace();
            feedbackLabel.setText("DATA ERROR");
            loadingSpinner.setVisible(false);
        }
    }

    private void displayGame(JsonObject game) {
        currentGameName = game.get("name").getAsString();
        String imageUrl = game.get("background_image").getAsString();
        
        System.out.println("DEBUG (Resposta): " + currentGameName);

        // Carrega a imagem (capa/screenshot)
        Image image = new Image(imageUrl, true);
        image.progressProperty().addListener((obs, oldV, newV) -> {
            if (newV.doubleValue() == 1.0) {
                // Quando terminar de baixar a imagem:
                loadingSpinner.setVisible(false);
                gameImageView.setOpacity(1.0);
                submitButton.setDisable(false);
                guessField.setDisable(false);
                feedbackLabel.setText("WHAT GAME IS THIS?");
                guessField.requestFocus();
            }
        });
        gameImageView.setImage(image);
    }

    private boolean isCorrectAnswer(String guess, String correct) {
        String cleanGuess = guess.toLowerCase().replaceAll("[^a-z0-9]", "");
        String cleanCorrect = correct.toLowerCase().replaceAll("[^a-z0-9]", "");
        // Verifica se contém e se tem pelo menos 3 letras para evitar chutes de "a" ou "1"
        return cleanCorrect.contains(cleanGuess) && cleanGuess.length() > 2;
    }
    
    private void updateScoreUI() {
        scoreLabel.setText("SCORE: " + score);
    }
}