package br.edu.unijui.piu.guessquest;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class LeaderboardManager {
    
    // Salva na pasta do usuário (ex: C:\Users\SeuNome\guessquest_leaderboard.txt)
    private static final String FILE_PATH = System.getProperty("user.home") + File.separator + "guessquest_leaderboard.txt";
    private static final int MAX_SCORES = 5;

    public static class ScoreEntry implements Comparable<ScoreEntry> {
        String name;
        int score;

        public ScoreEntry(String name, int score) {
            this.name = name;
            this.score = score;
        }

        @Override
        public int compareTo(ScoreEntry o) {
            return Integer.compare(o.score, this.score); // Decrescente
        }
        
        @Override
        public String toString() {
            return String.format("%-10s %06d", name, score);
        }
    }

    public static void saveScore(String name, int score) {
        if (score <= 0) return;

        List<ScoreEntry> scores = loadScores();
        scores.add(new ScoreEntry(name, score));
        scores.sort(null); // Ordena natural (decrescente)

        // Mantém apenas os top 5
        if (scores.size() > MAX_SCORES) {
            scores = scores.subList(0, MAX_SCORES);
        }

        saveToFile(scores);
    }

    public static List<ScoreEntry> loadScores() {
        List<ScoreEntry> list = new ArrayList<>();
        File file = new File(FILE_PATH);

        if (!file.exists()) {
            return list;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(";");
                if (parts.length == 2) {
                    try {
                        String savedName = parts[0];
                        int savedScore = Integer.parseInt(parts[1]);
                        list.add(new ScoreEntry(savedName, savedScore));
                    } catch (NumberFormatException ignored) {
                        // Ignora linhas corrompidas
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Garante ordenação
        list.sort(null);
        return list;
    }

    private static void saveToFile(List<ScoreEntry> scores) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_PATH))) {
            for (ScoreEntry entry : scores) {
                writer.write(entry.name + ";" + entry.score);
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}