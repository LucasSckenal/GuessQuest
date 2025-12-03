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

/**
 * Controlador responsável pela lógica principal do jogo (Gameplay).
 * Gerencia o ciclo de vida das fases, interação com a API externa (RAWG),
 * sistema de dicas, pontuação e transição entre níveis.
 */
public class SecondaryController {

    // --- Configurações da API ---
    private static final String API_KEY = "e6234549ffcb449f9502ec12c04a4201";
    // Template de URL para busca de jogos, ordenados por popularidade e filtrados por metacritic
    private static final String API_URL_TEMPLATE = "https://api.rawg.io/api/games?key=%s&ordering=-added&metacritic=%s&page_size=40&page=%d";

    // --- Controle de Sessão ---
    // Armazena jogos já jogados para evitar repetições na mesma partida
    private static final Set<String> gamesPlayedThisSession = new HashSet<>();
    
    // Lista de termos ou nomes genéricos que devem ser invalidados como respostas
    private static final Set<String> INVALID_EXACT_NAMES = new HashSet<>();

    static {
        INVALID_EXACT_NAMES.add("PC");
        INVALID_EXACT_NAMES.add("PlayStation");
        INVALID_EXACT_NAMES.add("Xbox");
        INVALID_EXACT_NAMES.add("Nintendo");
        INVALID_EXACT_NAMES.add("Mobile");
        INVALID_EXACT_NAMES.add("Steam");
        INVALID_EXACT_NAMES.add("Epic Games");
    }

    // --- Elementos de Interface ---
    @FXML private Label lblPhase;
    @FXML private Label lblScore;
    @FXML private Label lblLives;
    @FXML private Label lblPlayer;
    @FXML private ImageView gameImage;
    @FXML private Button btnA, btnB, btnC, btnD; // Botões de opção de resposta
    @FXML private Label lblFeedback; // Área de mensagens para o usuário
    @FXML private VBox loadingLayer; // Overlay de carregamento
    @FXML private Label lblLoading;
    @FXML private Button btnQuit;

    // --- Botões de Dicas ---
    @FXML private Button btnHintCall;
    @FXML private Button btnHintStudents;
    @FXML private Button btnHintAvocado;
    
    // --- Controles de Áudio na HUD ---
    @FXML private Button btnMuteMusic;
    @FXML private Button btnMuteSfx;

    // --- Estado Interno ---
    private GameState state;
    private String correctGameName;
    private final Random random = new Random();
    private boolean interactionLocked = false; // Previne múltiplos cliques durante animações
    private boolean isPT = false; // Cache local da preferência de idioma

    /**
     * Inicializa o controlador da cena de jogo.
     * Configura o estado inicial, idioma e carrega o primeiro nível.
     */
    @FXML
    public void initialize() {
        state = GameState.getInstance();
        isPT = state.getLanguage() == GameState.Language.PT;
        
        // Limpa histórico de jogos ao iniciar uma nova partida (Fase 1)
        if (state.getCurrentPhase() == 1) {
            gamesPlayedThisSession.clear();
        }
        
        updateMuteButtonsVisuals();
        updateStaticInterfaceText();
        updateUIHeader();
        loadNextLevel();
    }

    /**
     * Atualiza os textos fixos da interface (botões de dica, sair) conforme o idioma.
     */
    private void updateStaticInterfaceText() {
        if (isPT) {
            btnHintCall.setText("📞 LIGAR (OTACON)");
            btnHintStudents.setText("🎓 UNIVERSITÁRIOS");
            btnHintAvocado.setText("🥑 PODER DO ABACATE");
            btnQuit.setText("SAIR DO JOGO");
            lblLoading.setText("CARREGANDO...");
        } else {
            btnHintCall.setText("📞 CALL (OTACON)");
            btnHintStudents.setText("🎓 STUDENTS");
            btnHintAvocado.setText("🥑 AVOCADO POWER");
            btnQuit.setText("QUIT GAME");
            lblLoading.setText("LOADING...");
        }
    }

    /**
     * Atualiza o cabeçalho (HUD) com informações vitais: Vidas, Fase, Pontuação e Nome.
     */
    private void updateUIHeader() {
        lblPlayer.setText((isPT ? "JOGADOR: " : "PLAYER: ") + state.getPlayerName());
        lblPhase.setText(String.format((isPT ? "FASE: %02d-15" : "PHASE: %02d-15"), state.getCurrentPhase()));
        lblScore.setText(String.format((isPT ? "PONTOS: %06d" : "SCORE: %06d"), state.getCurrentScore()));

        StringBuilder livesStr = new StringBuilder(isPT ? "VIDAS: " : "LIVES: ");
        for (int i = 0; i < state.getCurrentLives(); i++) {
            livesStr.append("♥ ");
        }
        lblLives.setText(livesStr.toString());
    }

    // =========================================================================
    // LÓGICA DE CONTROLE DE ÁUDIO (HUD)
    // =========================================================================
    
    /**
     * Alterna o estado de mudo da música de fundo e atualiza o ícone visual.
     */
    @FXML
    private void toggleMusic() {
        SoundManager.getInstance().toggleMusicMute();
        updateMuteButtonsVisuals();
        // Remove foco do botão para evitar acionamento acidental com Teclado/Enter
        btnMuteMusic.getParent().requestFocus();
    }
    
    /**
     * Alterna o estado de mudo dos efeitos sonoros (SFX) e atualiza o ícone visual.
     */
    @FXML
    private void toggleSfx() {
        SoundManager.getInstance().toggleSfxMute();
        updateMuteButtonsVisuals();
        btnMuteSfx.getParent().requestFocus();
    }
    
    /**
     * Atualiza o texto e opacidade dos botões de áudio com base no estado atual do SoundManager.
     */
    private void updateMuteButtonsVisuals() {
        boolean musicMuted = SoundManager.getInstance().isMusicMuted();
        boolean sfxMuted = SoundManager.getInstance().isSfxMuted();
        
        btnMuteMusic.setText(musicMuted ? "❌" : "♪");
        btnMuteMusic.setStyle(musicMuted ? "-fx-opacity: 0.5;" : "-fx-opacity: 1.0;");
        
        btnMuteSfx.setText(sfxMuted ? "🔇" : "🔊");
        btnMuteSfx.setStyle(sfxMuted ? "-fx-opacity: 0.5;" : "-fx-opacity: 1.0;");
    }

    /**
     * Aborta o jogo atual e retorna ao menu principal.
     */
    @FXML
    private void exitToMenu() throws IOException {
        SoundManager.getInstance().playSound("select.wav");
        App.setRoot("primary");
    }

    // =========================================================================
    // SISTEMA DE DICAS (POWER-UPS)
    // =========================================================================

    /**
     * Dica: Ligar para Otacon.
     * Elimina uma opção incorreta aleatória.
     */
    @FXML
    private void useHintCall() {
        if (state.isHintCallUsed() || interactionLocked) return;

        state.setHintCallUsed(true);
        updateHintButtonStyle(btnHintCall, true);

        List<Button> wrongButtons = getActiveWrongButtons();
        if (!wrongButtons.isEmpty()) {
            Button toRemove = wrongButtons.get(random.nextInt(wrongButtons.size()));
            disableButtonOption(toRemove);
            SoundManager.getInstance().playSound("hint.wav");
            
            String msg = isPT 
                ? "OTACON: 'Sistema hackeado! A opção " + getOptionLetter(toRemove) + " é falsa!'"
                : "OTACON: 'System hacked! Option " + getOptionLetter(toRemove) + " is false!'";
            
            lblFeedback.setText(msg);
            lblFeedback.setStyle("-fx-text-fill: #3498db;");
        }
    }

    /**
     * Dica: Universitários.
     * Atribui porcentagens de probabilidade às opções, favorecendo a correta.
     */
    @FXML
    private void useHintStudents() {
        if (state.isHintStudentsUsed() || interactionLocked) return;

        state.setHintStudentsUsed(true);
        updateHintButtonStyle(btnHintStudents, true);

        // Define chance da resposta correta entre 40% e 70%
        int correctPercent = 40 + random.nextInt(31); 
        int remainingPercent = 100 - correctPercent;

        List<Button> wrongs = getActiveWrongButtons();
        Button correctBtn = getCorrectButton();

        if (correctBtn != null) {
            appendPercentToButton(correctBtn, correctPercent);
        }

        // Distribui o restante da porcentagem entre as erradas
        for (int i = 0; i < wrongs.size(); i++) {
            Button btn = wrongs.get(i);
            int share;
            if (i == wrongs.size() - 1) {
                share = remainingPercent; 
            } else {
                share = random.nextInt(remainingPercent / 2); 
                remainingPercent -= share;
            }
            appendPercentToButton(btn, share);
        }

        SoundManager.getInstance().playSound("hint.wav");
        String msg = isPT
            ? "UNIVERSITÁRIOS: 'Acreditamos que seja a maior porcentagem...'"
            : "STUDENTS: 'We believe it's the highest percentage...'";
        
        lblFeedback.setText(msg);
        lblFeedback.setStyle("-fx-text-fill: #ea80fc;");
    }

    /**
     * Dica: Poder do Abacate.
     * Elimina duas opções incorretas aleatórias.
     */
    @FXML
    private void useHintAvocado() {
        if (state.isHintAvocadoUsed() || interactionLocked) return;

        state.setHintAvocadoUsed(true);
        updateHintButtonStyle(btnHintAvocado, true);

        List<Button> wrongs = getActiveWrongButtons();
        Collections.shuffle(wrongs); 

        int removedCount = 0;
        for (Button btn : wrongs) {
            if (removedCount >= 2) break;
            disableButtonOption(btn);
            removedCount++;
        }

        SoundManager.getInstance().playSound("abacate.wav");
        String msg = isPT
            ? "SANTO ABACATE: 'A polpa divina eliminou as impurezas!'"
            : "HOLY AVOCADO: 'The divine pulp has eliminated the impurities!'";
            
        lblFeedback.setText(msg);
        lblFeedback.setStyle("-fx-text-fill: #00ff00; -fx-font-weight: bold;");
    }

    /**
     * Atualiza o estilo visual do botão de dica (ativo ou usado/desabilitado).
     */
    private void updateHintButtonStyle(Button btn, boolean used) {
        if (used) {
            btn.setDisable(true);
            btn.getStyleClass().add("hint-used");
        } else {
            btn.setDisable(false);
            btn.getStyleClass().remove("hint-used");
        }
    }

    // --- Métodos Auxiliares para Manipulação de Botões ---

    private List<Button> getActiveWrongButtons() {
        List<Button> list = new ArrayList<>();
        checkWrongAndAdd(btnA, list);
        checkWrongAndAdd(btnB, list);
        checkWrongAndAdd(btnC, list);
        checkWrongAndAdd(btnD, list);
        return list;
    }

    private Button getCorrectButton() {
        if (isCorrect(btnA)) return btnA;
        if (isCorrect(btnB)) return btnB;
        if (isCorrect(btnC)) return btnC;
        if (isCorrect(btnD)) return btnD;
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
        // Remove prefixos "A) " e sufixos de porcentagem "(50%)" para comparação
        String temp = text.length() > 3 ? text.substring(3) : text;
        return temp.replaceAll("\\s*\\(\\d+%\\)$", "").trim();
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
    // LÓGICA DE GERENCIAMENTO DE NÍVEL E API
    // =========================================================================

    /**
     * Inicia o carregamento assíncrono do próximo nível.
     * Verifica se o jogo acabou (venceu após 15 fases).
     */
    private void loadNextLevel() {
        if (state.getCurrentPhase() > 15) {
            endGame(true);
            return;
        }

        interactionLocked = true;
        lblFeedback.setText(isPT ? "CARREGANDO DADOS..." : "LOADING DATA...");
        loadingLayer.setVisible(true);

        // Executa busca de dados em thread separada para não travar a UI
        new Thread(() -> {
            try {
                GameData levelData = fetchGameDataWithRetry();
                final GameData finalData = levelData;
                Platform.runLater(() -> setupLevelUI(finalData));
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> setupLevelUI(getEmergencyFallbackData()));
            }
        }).start();
    }

    /**
     * Tenta buscar dados válidos da API com até 3 tentativas.
     * Se falhar, utiliza uma página segura (página 1).
     */
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

    /**
     * Configura a interface com os dados do jogo carregado (Imagem e Opções).
     */
    private void setupLevelUI(GameData data) {
        if (data.imageUrl == null || data.imageUrl.isEmpty()) {
            loadNextLevel(); // Tenta carregar outro se a imagem for inválida
            return;
        }

        loadingLayer.setVisible(false);
        interactionLocked = false;
        lblFeedback.setText("");

        this.correctGameName = data.correctName;
        gamesPlayedThisSession.add(data.correctName);

        try {
            // Carrega imagem em background (segundo parâmetro true)
            gameImage.setImage(new Image(data.imageUrl, true));
        } catch (Exception e) {
            loadNextLevel();
            return;
        }

        List<String> options = data.options;
        // Completa opções se a API não retornou suficientes
        int backupCount = 1;
        while (options.size() < 4) {
            options.add("Mystery Game " + backupCount++);
        }

        btnA.setText("A) " + options.get(0));
        btnB.setText("B) " + options.get(1));
        btnC.setText("C) " + options.get(2));
        btnD.setText("D) " + options.get(3));

        resetButtonStyles();

        // Atualiza estado visual dos botões de dica
        updateHintButtonStyle(btnHintCall, state.isHintCallUsed());
        updateHintButtonStyle(btnHintStudents, state.isHintStudentsUsed());
        updateHintButtonStyle(btnHintAvocado, state.isHintAvocadoUsed());
    }

    /**
     * Processa a resposta do usuário ao clicar em um botão.
     * Verifica vitória/derrota e atualiza pontuação e vidas.
     */
    @FXML
    private void handleAnswer(javafx.event.ActionEvent event) {
        if (interactionLocked) return;

        Button clickedButton = (Button) event.getSource();
        String selectedAnswer = cleanButtonText(clickedButton.getText());

        interactionLocked = true;
        boolean isWin = selectedAnswer.equalsIgnoreCase(correctGameName);

        revealBoard(clickedButton);

        if (isWin) {
            lblFeedback.setText(isPT ? "CORRETO! +1000 PTS" : "CORRECT! +1000 PTS");
            lblFeedback.setStyle("-fx-text-fill: #00ff00;");
            SoundManager.getInstance().playSound("correct.wav");
            
            state.addScore(1000);
            scheduleNextLevel(1500);
        } else {
            lblFeedback.setText(isPT ? "ERRADO! PERDEU VIDA" : "WRONG! LIFE LOST");
            lblFeedback.setStyle("-fx-text-fill: #ff3333;");
            SoundManager.getInstance().playSound("wrong.wav");
            
            state.decreaseLife();
            updateUIHeader();

            if (state.getCurrentLives() <= 0) {
                endGame(false);
            } else {
                scheduleNextLevel(2500); // Delay maior para ver o erro
            }
        }
    }

    /**
     * Revela a resposta correta e a escolhida no tabuleiro.
     */
    private void revealBoard(Button clickedButton) {
        List<Button> allButtons = new ArrayList<>();
        allButtons.add(btnA);
        allButtons.add(btnB);
        allButtons.add(btnC);
        allButtons.add(btnD);

        for (Button btn : allButtons) {
            if (isCorrect(btn)) {
                if (!btn.getStyleClass().contains("btn-correct")) {
                    btn.getStyleClass().add("btn-correct");
                }
                btn.setOpacity(1.0);
            } else if (btn == clickedButton) {
                // Marca como errado se foi o clicado
                if (!btn.getStyleClass().contains("btn-wrong")) {
                    btn.getStyleClass().add("btn-wrong");
                }
                btn.setOpacity(1.0);
            } else {
                // Esmaece as outras opções
                btn.setStyle("-fx-opacity: 0.3;");
            }
        }
    }

    /**
     * Agenda a execução do carregamento do próximo nível após um delay.
     */
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

    /**
     * Reseta estilos e estados dos botões de opção para o padrão.
     */
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
        btn.setStyle(""); 
        btn.setOpacity(1.0);
    }

    /**
     * Fornece dados de fallback (segurança) caso a API falhe completamente.
     */
    private GameData getEmergencyFallbackData() {
        GameData data = new GameData();
        data.correctName = "Pac-Man";
        data.imageUrl = "https://i.imgur.com/EtVkAMm.jpeg"; 
        data.options.add("Pac-Man");
        data.options.add("Tetris");
        data.options.add("Space Invaders");
        data.options.add("Pong");
        Collections.shuffle(data.options);
        return data;
    }

    /**
     * Finaliza a sessão de jogo (Vitória ou Game Over).
     * Salva a pontuação no Leaderboard e retorna ao menu.
     */
    private void endGame(boolean win) {
        loadingLayer.setVisible(true);
        LeaderboardManager.saveScore(state.getPlayerName(), state.getCurrentScore());

        if (win) {
            lblFeedback.setText(isPT ? "PARABENS! VOCE ZEROU!" : "CONGRATULATIONS! YOU BEAT THE GAME!");
            lblFeedback.setStyle("-fx-text-fill: gold;");
            SoundManager.getInstance().playSound("win.wav");
        } else {
            lblFeedback.setText("GAME OVER");
            lblFeedback.setStyle("-fx-text-fill: red;");
            SoundManager.getInstance().playSound("gameover.wav");
        }

        // Aguarda 4 segundos antes de voltar ao menu
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

    /**
     * Realiza a requisição HTTP à API RAWG para buscar jogos.
     * Implementa lógica de paginação aleatória para variedade.
     */
    private GameData fetchGameData(boolean forceSafePage) throws Exception {
        int pageBase;
        if (forceSafePage) {
            pageBase = 1;
        } else {
            // Randomiza a página para não pegar sempre os mesmos jogos populares
            pageBase = random.nextInt(50) + 1;
            if (state.getCurrentPhase() > 5)
                pageBase += 100;
            // Modo Souls busca jogos muito obscuros (paginas distantes)
            if (state.getDifficulty() == GameState.Difficulty.SOULS)
                pageBase = random.nextInt(500) + 1;
        }

        String metacriticFilter = state.getDifficulty().metacriticRange;
        // Facilita busca em fases avançadas para garantir que existam jogos
        if ((state.getCurrentPhase() > 10 && state.getDifficulty() != GameState.Difficulty.SOULS) || forceSafePage) {
            metacriticFilter = "50,100";
        }

        String url = String.format(API_URL_TEMPLATE, API_KEY, metacriticFilter, pageBase);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        return parseJsonManually(response.body());
    }

    // --- Classes Internas de Dados ---
    
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

    /**
     * Realiza o parse manual do JSON retornado pela API.
     * Extrai slug, nome e imagem, e seleciona um jogo correto e 3 errados.
     */
    private GameData parseJsonManually(String json) {
        GameData data = new GameData();
        List<GameCandidate> candidates = new ArrayList<>();
        
        // Regex para encontrar blocos de jogos
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
        
        // Processa candidatos encontrados
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
        
        if (candidates.isEmpty()) return data;

        // Filtra candidatos já jogados
        List<GameCandidate> available = new ArrayList<>();
        for (GameCandidate cand : candidates) {
            boolean hasImage = cand.image != null && !cand.image.isEmpty() && !cand.image.equals("null");
            if (!gamesPlayedThisSession.contains(cand.name) && hasImage) {
                available.add(cand);
            }
        }
        // Se todos foram jogados, reseta pool
        if (available.isEmpty()) available = candidates;

        // Seleciona o vencedor
        int correctIndex = random.nextInt(available.size());
        GameCandidate correctGame = available.get(correctIndex);
        data.correctName = correctGame.name;
        data.imageUrl = correctGame.image;
        data.options.add(correctGame.name);

        // Seleciona os 3 errados do resto da lista
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

    /**
     * Extrai a URL da imagem de background de um trecho JSON usando substrings.
     */
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

    /**
     * Valida nomes de jogos para remover entradas indesejadas (DLCs, Soundtracks, nomes com caracteres inválidos).
     */
    private boolean isValidGameName(String name, String slug) {
        if (name == null || name.trim().isEmpty()) return false;
        if (name.matches(".*\\p{InCyrillic}.*")) return false; // Remove Russo
        if (INVALID_EXACT_NAMES.contains(name)) return false;
        // Filtra caracteres estranhos (mantendo acentos comuns)
        if (name.matches(".*[^\\x00-\\x7F\\p{L}\\p{N}\\s\\p{P}].*") && !name.matches(".*[áéíóúãõçÁÉÍÓÚÃÕÇ].*")) return false;
        
        String lowerName = name.toLowerCase();
        if (lowerName.contains("playstation") || lowerName.contains("xbox") || lowerName.equals("pc") ||
                lowerName.contains("soundtrack") || lowerName.contains("dlc") || lowerName.contains("bundle") ||
                lowerName.contains("edition") || lowerName.contains("season pass") || lowerName.contains("patch") ||
                lowerName.contains("mod"))
            return false;
        return true;
    }

    /**
     * Decodifica caracteres Unicode escapados em strings JSON.
     */
    private String unescapeJava(String st) {
        if (st == null) return "";
        return st.replace("\\u0027", "'").replace("\\u0026", "&").replace("\\/", "/").replace("\\", "");
    }
}