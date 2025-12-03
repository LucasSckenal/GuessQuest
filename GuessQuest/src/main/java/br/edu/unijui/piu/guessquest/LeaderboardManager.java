package br.edu.unijui.piu.guessquest;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Gerenciador responsável pela persistência dos recordes (High Scores).
 * Salva e carrega dados de um arquivo de texto local no diretório do usuário.
 * Mantém apenas os top 5 melhores resultados.
 */
public class LeaderboardManager {
    
    // Caminho do arquivo de persistência.
    // Utiliza System.getProperty("user.home") para garantir permissão de escrita independente do OS.
    private static final String FILE_PATH = System.getProperty("user.home") + File.separator + "guessquest_leaderboard.txt";
    private static final int MAX_SCORES = 5;

    /**
     * Classe interna que representa uma entrada única no placar.
     * Implementa Comparable para facilitar a ordenação decrescente por pontuação.
     */
    public static class ScoreEntry implements Comparable<ScoreEntry> {
        public String name;
        public int score;

        public ScoreEntry(String name, int score) {
            this.name = name;
            this.score = score;
        }

        // Ordenação decrescente (maior score primeiro)
        @Override
        public int compareTo(ScoreEntry o) {
            return Integer.compare(o.score, this.score); 
        }
        
        @Override
        public String toString() {
            return String.format("%-10s %06d", name, score);
        }
    }

    /**
     * Salva uma nova pontuação no arquivo.
     * Carrega pontuações existentes, adiciona a nova, ordena e mantém apenas o top 5.
     * * @param name Nome/Iniciais do jogador.
     * @param score Pontuação final.
     */
    public static void saveScore(String name, int score) {
        if (score <= 0) return; // Ignora pontuações zeradas ou negativas

        List<ScoreEntry> scores = loadScores();
        scores.add(new ScoreEntry(name, score));
        
        // Ordena a lista usando compareTo (Decrescente)
        scores.sort(null); 

        // Corta a lista para manter apenas o MAX_SCORES
        if (scores.size() > MAX_SCORES) {
            scores = scores.subList(0, MAX_SCORES);
        }

        saveToFile(scores);
    }

    /**
     * Carrega as pontuações do arquivo local.
     * Realiza o parsing linha por linha no formato "NOME;PONTOS".
     * * @return Lista de ScoreEntry ordenada.
     */
    public static List<ScoreEntry> loadScores() {
        List<ScoreEntry> list = new ArrayList<>();
        File file = new File(FILE_PATH);

        // Debug de localização do arquivo para fins de suporte
        System.out.println("--- LEADERBOARD DEBUG ---");
        System.out.println("Procurando arquivo em: " + FILE_PATH);

        if (!file.exists()) {
            System.out.println("RESULTADO: Arquivo NAO encontrado neste local (será criado ao salvar).");
            return list;
        }
        
        System.out.println("RESULTADO: Arquivo encontrado! Lendo...");

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                // Formato esperado: NOME;1000
                String[] parts = line.split(";");
                if (parts.length == 2) {
                    try {
                        String savedName = parts[0].trim();
                        // .trim() evita NumberFormatException por espaços extras
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

        // Garante que a lista retornada esteja ordenada, caso o arquivo tenha sido editado manualmente
        list.sort(null);
        return list;
    }

    /**
     * Escreve a lista de pontuações no arquivo, sobrescrevendo o conteúdo anterior.
     */
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