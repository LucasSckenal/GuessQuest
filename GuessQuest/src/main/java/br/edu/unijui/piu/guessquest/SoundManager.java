package br.edu.unijui.piu.guessquest;

import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import java.net.URL;

/**
 * Singleton para gerenciar áudio.
 * Requer que os arquivos estejam em src/main/resources/br/edu/unijui/piu/guessquest/audio/
 * Suporta .mp3 e .wav graças ao javafx-media adicionado no POM.
 */
public class SoundManager {
    
    private static SoundManager instance;
    private MediaPlayer musicPlayer;
    private double musicVolume = 0.5; // 50% padrão
    private double sfxVolume = 0.5;   // 50% padrão

    private SoundManager() {}

    public static SoundManager getInstance() {
        if (instance == null) {
            instance = new SoundManager();
        }
        return instance;
    }

    public void playMusic(String filename) {
        try {
            URL resource = getClass().getResource("audio/" + filename);
            if (resource == null) {
                System.out.println("Audio não encontrado: " + filename);
                return;
            }

            // Para música anterior
            if (musicPlayer != null) {
                musicPlayer.stop();
                musicPlayer.dispose();
            }

            Media media = new Media(resource.toExternalForm());
            musicPlayer = new MediaPlayer(media);
            musicPlayer.setCycleCount(MediaPlayer.INDEFINITE); // Loop infinito
            musicPlayer.setVolume(musicVolume);
            musicPlayer.play();
            
        } catch (Exception e) {
            System.err.println("Erro ao tocar música (" + filename + "): " + e.getMessage());
        }
    }

    public void stopMusic() {
        if (musicPlayer != null) {
            musicPlayer.stop();
        }
    }

    public void playSound(String filename) {
        try {
            URL resource = getClass().getResource("audio/" + filename);
            if (resource != null) {
                // AudioClip é otimizado para efeitos curtos (tiros, pulos, UI)
                AudioClip clip = new AudioClip(resource.toExternalForm());
                clip.setVolume(sfxVolume);
                clip.play();
            } else {
                System.out.println("SFX não encontrado: " + filename);
            }
        } catch (Exception e) {
            System.err.println("Erro ao tocar SFX (" + filename + "): " + e.getMessage());
        }
    }

    public void setMusicVolume(double volume) {
        this.musicVolume = volume; // 0.0 a 1.0
        if (musicPlayer != null) {
            musicPlayer.setVolume(volume);
        }
    }

    public void setSfxVolume(double volume) {
        this.sfxVolume = volume; // 0.0 a 1.0
    }

    public double getMusicVolume() { return musicVolume; }
    public double getSfxVolume() { return sfxVolume; }
}