package br.edu.unijui.piu.guessquest;

/**
 * Singleton para gerenciar o estado global do jogo (Vidas, Pontos,
 * Configurações).
 */
public class GameState {

    private static GameState instance;

    public enum Difficulty {
        NORMAL(5, 1.0, "80,100"),
        HARD(3, 1.5, "60,100"),
        SOULS(1, 2.0, "1,100"),

        // --- NOVO MODO SECRETO ---
        INFINITY(99, 10.0, "1,100");

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

    // --- VARIÁVEIS DE DICAS ---
    private boolean hintCallUsed;
    private boolean hintStudentsUsed;
    private boolean hintAvocadoUsed;

    private GameState() {
        this.difficulty = Difficulty.NORMAL;
        this.currentScore = 0;
        this.currentPhase = 1;
        resetHints();
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
        resetHints();
    }

    private void resetHints() {
        this.hintCallUsed = false;
        this.hintStudentsUsed = false;
        this.hintAvocadoUsed = false;
    }

    // Getters e Setters das Dicas
    public boolean isHintCallUsed() {
        return hintCallUsed;
    }

    public void setHintCallUsed(boolean used) {
        this.hintCallUsed = used;
    }

    public boolean isHintStudentsUsed() {
        return hintStudentsUsed;
    }

    public void setHintStudentsUsed(boolean used) {
        this.hintStudentsUsed = used;
    }

    public boolean isHintAvocadoUsed() {
        return hintAvocadoUsed;
    }

    public void setHintAvocadoUsed(boolean used) {
        this.hintAvocadoUsed = used;
    }

    // Getters e Setters existentes
    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    public int getCurrentLives() {
        return currentLives;
    }

    public void decreaseLife() {
        this.currentLives--;
    }

    public int getCurrentScore() {
        return currentScore;
    }

    public void addScore(int points) {
        this.currentScore += (points * difficulty.scoreMultiplier);
    }

    public int getCurrentPhase() {
        return currentPhase;
    }

    public void nextPhase() {
        this.currentPhase++;
    }
}