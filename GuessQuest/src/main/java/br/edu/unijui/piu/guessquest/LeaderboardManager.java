package br.edu.unijui.piu.guessquest;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class LeaderboardManager {
    
    // Caminho do arquivo.
    // ATENÇÃO: Verifique no console onde este caminho está apontando ao rodar!
    private static final String FILE_PATH = System.getProperty("user.home") + File.separator + "guessquest_leaderboard.txt";
    private static final int MAX_SCORES = 5;

    public static class ScoreEntry implements Comparable<ScoreEntry> {
        public String name;
        public int score;

        public ScoreEntry(String name, int score) {
            this.name = name;
            this.score = score;
        }

        @Override
        public int compareTo(ScoreEntry o) {
            return Integer.compare(o.score, this.score); 
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
        scores.sort(null); 

        if (scores.size() > MAX_SCORES) {
            scores = scores.subList(0, MAX_SCORES);
        }

        saveToFile(scores);
    }

    public static List<ScoreEntry> loadScores() {
        List<ScoreEntry> list = new ArrayList<>();
        File file = new File(FILE_PATH);

        // DEBUG: Mostra no console onde o jogo está procurando o arquivo
        System.out.println("--- LEADERBOARD DEBUG ---");
        System.out.println("Procurando arquivo em: " + FILE_PATH);

        if (!file.exists()) {
            System.out.println("RESULTADO: Arquivo NAO encontrado neste local.");
            return list;
        }
        
        System.out.println("RESULTADO: Arquivo encontrado! Lendo...");

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                String[] parts = line.split(";");
                if (parts.length == 2) {
                    try {
                        // .trim() remove espaços em branco antes/depois que causam erro no parseInt
                        String savedName = parts[0].trim();
                        int savedScore = Integer.parseInt(parts[1].trim());
                        
                        list.add(new ScoreEntry(savedName, savedScore));
                        System.out.println("Lido com sucesso: " + savedName + " - " + savedScore);

                    } catch (NumberFormatException e) {
                        System.err.println("Erro ao ler linha (numero invalido): " + line);
                    }
                } else {
                    System.err.println("Linha com formato invalido (precisa ser NOME;PONTOS): " + line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

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