package br.edu.unijui.piu.guessquest;

/**
 * Singleton para gerenciar o estado global do jogo (Vidas, Pontos, Configurações).
 */
public class GameState {
    
    private static GameState instance;

    public enum Difficulty {
        NORMAL(5, 1.0, "80,100"), // Mais vidas, jogos aclamados
        HARD(3, 1.5, "60,100"),   // Menos vidas, jogos bons
        SOULS(1, 2.0, "1,100");   // 1 vida, qualquer jogo (incluindo ruins/desconhecidos)

        final int initialLives;
        final double scoreMultiplier;
        final String metacriticRange;

        Difficulty(int lives, double multiplier, String range) {
            this.initialLives = lives;
            this.scoreMultiplier = multiplier;
            this.metacriticRange = range;
        }
    }

    private String playerName;
    private Difficulty difficulty;
    private int currentLives;
    private int currentScore;
    private int currentPhase;

    private GameState() {
        // Valores padrão
        this.difficulty = Difficulty.NORMAL;
        this.currentScore = 0;
        this.currentPhase = 1;
    }

    public static GameState getInstance() {
        if (instance == null) {
            instance = new GameState();
        }
        return instance;
    }

    public void resetGame() {
        this.currentLives = this.difficulty.initialLives;
        this.currentScore = 0;
        this.currentPhase = 1;
    }

    // Getters e Setters
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }

    public Difficulty getDifficulty() { return difficulty; }
    public void setDifficulty(Difficulty difficulty) { this.difficulty = difficulty; }

    public int getCurrentLives() { return currentLives; }
    public void decreaseLife() { this.currentLives--; }

    public int getCurrentScore() { return currentScore; }
    public void addScore(int points) { this.currentScore += (points * difficulty.scoreMultiplier); }

    public int getCurrentPhase() { return currentPhase; }
    public void nextPhase() { this.currentPhase++; }
}