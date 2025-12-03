package br.edu.unijui.piu.guessquest;

import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import java.net.URL;

/**
 * Singleton responsável por gerenciar toda a reprodução de áudio da aplicação.
 * Controla música de fundo (MediaPlayer) e efeitos sonoros curtos (AudioClip) separadamente,
 * permitindo controle de volume independente e funcionalidades de Mute.
 * * Requer que os arquivos de áudio estejam localizados em:
 * src/main/resources/br/edu/unijui/piu/guessquest/audio/
 */
public class SoundManager {
    
    private static SoundManager instance;
    private MediaPlayer musicPlayer;
    
    // Volumes atuais (Range: 0.0 a 1.0)
    private double musicVolume = 0.5; 
    private double sfxVolume = 0.5;   

    // Flags de estado Mute
    private boolean isMusicMuted = false;
    private boolean isSfxMuted = false;

    // Armazena o volume anterior para restaurar ao desmutar
    private double lastMusicVolume = 0.5;
    private double lastSfxVolume = 0.5;

    // Construtor privado para garantir o padrão Singleton
    private SoundManager() {}

    /**
     * Retorna a instância única do SoundManager.
     */
    public static SoundManager getInstance() {
        if (instance == null) {
            instance = new SoundManager();
        }
        return instance;
    }

    /**
     * Toca uma música de fundo em loop.
     * Se outra música estiver tocando, ela é interrompida.
     * Utiliza MediaPlayer, adequado para áudios longos.
     * * @param filename Nome do arquivo (ex: "bgm.mp3")
     */
    public void playMusic(String filename) {
        try {
            URL resource = getClass().getResource("audio/" + filename);
            if (resource == null) {
                System.out.println("Audio não encontrado: " + filename);
                return;
            }

            if (musicPlayer != null) {
                musicPlayer.stop();
                musicPlayer.dispose();
            }

            Media media = new Media(resource.toExternalForm());
            musicPlayer = new MediaPlayer(media);
            musicPlayer.setCycleCount(MediaPlayer.INDEFINITE); // Loop infinito
            
            // Aplica volume atual ou 0 se estiver mutado
            musicPlayer.setVolume(isMusicMuted ? 0 : musicVolume);
            
            musicPlayer.play();
            
        } catch (Exception e) {
            System.err.println("Erro ao tocar música (" + filename + "): " + e.getMessage());
        }
    }

    /**
     * Interrompe a reprodução da música atual.
     */
    public void stopMusic() {
        if (musicPlayer != null) {
            musicPlayer.stop();
        }
    }

    /**
     * Toca um efeito sonoro (SFX) uma única vez ("fire and forget").
     * Utiliza AudioClip, otimizado para sons curtos e baixa latência.
     * * @param filename Nome do arquivo (ex: "select.wav")
     */
    public void playSound(String filename) {
        // Se estiver mutado, aborta imediatamente
        if (isSfxMuted) return;

        try {
            URL resource = getClass().getResource("audio/" + filename);
            if (resource != null) {
                AudioClip clip = new AudioClip(resource.toExternalForm());
                clip.setVolume(sfxVolume);
                clip.play();
            }
        } catch (Exception e) {
            System.err.println("Erro ao tocar SFX (" + filename + "): " + e.getMessage());
        }
    }

    // =========================================================================
    // MÉTODOS DE CONTROLE DE VOLUME E MUTE
    // =========================================================================

    /**
     * Define o volume da música (0.0 a 1.0).
     * Se não estiver mutado, atualiza o MediaPlayer em tempo real.
     */
    public void setMusicVolume(double volume) {
        this.musicVolume = volume;
        if (!isMusicMuted) {
            this.lastMusicVolume = volume; // Atualiza memória apenas se audível
            if (musicPlayer != null) {
                musicPlayer.setVolume(volume);
            }
        }
    }

    /**
     * Define o volume dos efeitos sonoros.
     */
    public void setSfxVolume(double volume) {
        this.sfxVolume = volume;
        if (!isSfxMuted) {
            this.lastSfxVolume = volume;
        }
    }

    /**
     * Alterna o estado Mute da música.
     * Zera o volume se mutado, ou restaura o volume definido se desmutado.
     */
    public void toggleMusicMute() {
        isMusicMuted = !isMusicMuted;
        if (musicPlayer != null) {
            musicPlayer.setVolume(isMusicMuted ? 0 : musicVolume);
        }
    }

    /**
     * Alterna o estado Mute dos efeitos sonoros.
     * O controle é feito logicamente no método playSound.
     */
    public void toggleSfxMute() {
        isSfxMuted = !isSfxMuted;
    }

    public boolean isMusicMuted() { return isMusicMuted; }
    public boolean isSfxMuted() { return isSfxMuted; }

    public double getMusicVolume() { return musicVolume; }
    public double getSfxVolume() { return sfxVolume; }
}