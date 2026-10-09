package com.mediaplayer.engine;

import java.io.File;
import javafx.scene.Node;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.util.Duration;

/** Plays formats supported natively by JavaFX (mp4/H.264, m4v, mp3, wav, aac, flv, hls). */
public class FxEngine implements PlaybackEngine {

    private final MediaView view = new MediaView();
    private MediaPlayer player;
    private Listener listener;
    private double volume = 0.5;
    private boolean muted;
    private boolean errorReported;

    public FxEngine() {
        view.setPreserveRatio(true);
    }

    public MediaView getMediaView() {
        return view;
    }

    @Override public void setListener(Listener l) { this.listener = l; }

    @Override
    public void load(File file) {
        release();
        errorReported = false;
        try {
            Media media = new Media(file.toURI().toString());
            MediaPlayer p = new MediaPlayer(media);
            player = p;
            p.setVolume(volume);
            p.setMute(muted);

            media.setOnError(() -> reportError(media.getError() == null ? "Unsupported media" : media.getError().getMessage()));
            p.setOnError(() -> reportError(p.getError() == null ? "Playback error" : p.getError().getMessage()));

            p.setOnReady(() -> {
                if (p != player) return;
                fireTime(0, p.getTotalDuration().toSeconds());
                p.play();
            });
            p.setOnPlaying(() -> { if (p == player && listener != null) listener.onState(true); });
            p.setOnPaused(() -> { if (p == player && listener != null) listener.onState(false); });
            p.setOnStopped(() -> {
                if (p != player) return;
                if (listener != null) listener.onState(false);
                fireTime(0, p.getTotalDuration().toSeconds());
            });
            p.setOnEndOfMedia(() -> { if (p == player && listener != null) listener.onEnded(); });
            p.currentTimeProperty().addListener((obs, o, n) -> {
                if (p == player) fireTime(n.toSeconds(), p.getTotalDuration().toSeconds());
            });

            view.setMediaPlayer(p);
        } catch (Exception ex) {
            reportError(ex.getMessage() == null ? ex.toString() : ex.getMessage());
        }
    }

    private void fireTime(double cur, double total) {
        if (listener != null) listener.onTime(cur, Double.isNaN(total) || Double.isInfinite(total) ? 0 : total);
    }

    private void reportError(String msg) {
        if (errorReported) return;
        errorReported = true;
        if (listener != null) listener.onError(msg);
    }

    private void release() {
        if (player != null) {
            MediaPlayer old = player;
            player = null;
            old.setOnError(null);
            old.setOnReady(null);
            old.setOnEndOfMedia(null);
            try { old.stop(); } catch (Exception ignored) { }
            old.dispose();
        }
        view.setMediaPlayer(null);
    }

    @Override public void play()  { if (player != null) player.play(); }
    @Override public void pause() { if (player != null) player.pause(); }
    @Override public void stop()  { if (player != null) player.stop(); }

    @Override
    public void seek(double seconds) {
        if (player != null) player.seek(Duration.seconds(seconds));
    }

    @Override
    public void setVolume(double v) {
        volume = v;
        if (player != null) player.setVolume(v);
    }

    @Override
    public void setMute(boolean m) {
        muted = m;
        if (player != null) player.setMute(m);
    }

    @Override public Node getVideoNode() { return view; }

    @Override public void dispose() { release(); }
}
