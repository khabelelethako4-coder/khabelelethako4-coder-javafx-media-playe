package com.mediaplayer.engine;

import java.io.File;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.image.ImageView;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.javafx.videosurface.ImageViewVideoSurface;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;

/**
 * Plays everything VLC can play: MKV (Matroska), AVI, MOV, WebM, WMV, MPG, TS, 3GP, OGV,
 * FLAC, OGG, OPUS, WMA ... and MP4 files using codecs JavaFX does not support (e.g. H.265/HEVC).
 * Needs VLC (64-bit) installed on the computer.
 */
public class VlcEngine implements PlaybackEngine {

    private final ImageView view = new ImageView();
    private MediaPlayerFactory factory;
    private EmbeddedMediaPlayer player;
    private Listener listener;
    private File current;
    private boolean needsReload = true;
    private double volume = 0.5;
    private boolean muted;
    private long lengthMs;

    public VlcEngine() {
        view.setPreserveRatio(true);
    }

    public ImageView getImageView() {
        return view;
    }

    @Override public void setListener(Listener l) { this.listener = l; }

    /** Creates the VLC player the first time it is needed. */
    private boolean ensureInit() {
        if (player != null) return true;
        try {
            factory = new MediaPlayerFactory("--no-video-title-show", "--no-osd");
            player = factory.mediaPlayers().newEmbeddedMediaPlayer();
            player.videoSurface().set(new ImageViewVideoSurface(view));
            player.events().addMediaPlayerEventListener(new MediaPlayerEventAdapter() {
                @Override public void playing(MediaPlayer mp) {
                    Platform.runLater(() -> {
                        applyAudio();
                        if (listener != null) listener.onState(true);
                    });
                }
                @Override public void paused(MediaPlayer mp) {
                    Platform.runLater(() -> { if (listener != null) listener.onState(false); });
                }
                @Override public void stopped(MediaPlayer mp) {
                    Platform.runLater(() -> {
                        if (listener != null) {
                            listener.onState(false);
                            listener.onTime(0, lengthMs / 1000.0);
                        }
                    });
                }
                @Override public void finished(MediaPlayer mp) {
                    needsReload = true;
                    Platform.runLater(() -> { if (listener != null) listener.onEnded(); });
                }
                @Override public void error(MediaPlayer mp) {
                    Platform.runLater(() -> { if (listener != null) listener.onError("VLC could not play this file."); });
                }
                @Override public void lengthChanged(MediaPlayer mp, long newLength) {
                    lengthMs = newLength;
                }
                @Override public void timeChanged(MediaPlayer mp, long newTime) {
                    long len = lengthMs > 0 ? lengthMs : mp.status().length();
                    Platform.runLater(() -> { if (listener != null) listener.onTime(newTime / 1000.0, len / 1000.0); });
                }
            });
            applyAudio();
            return true;
        } catch (Throwable t) {
            if (factory != null) {
                try { factory.release(); } catch (Throwable ignored) { }
            }
            factory = null;
            player = null;
            return false;
        }
    }

    private void applyAudio() {
        if (player == null) return;
        player.audio().setVolume((int) Math.round(volume * 100));
        player.audio().setMute(muted);
    }

    @Override
    public void load(File file) {
        if (!ensureInit()) {
            if (listener != null) {
                listener.onError("This file type needs VLC media player.\n\n"
                        + "Please install VLC (64-bit, same type as your Java) from https://www.videolan.org and restart this app.");
            }
            return;
        }
        current = file;
        lengthMs = 0;
        needsReload = false;
        player.media().play(file.getAbsolutePath());
    }

    @Override
    public void play() {
        if (player == null || current == null) return;
        if (needsReload) {
            needsReload = false;
            player.media().play(current.getAbsolutePath());
        } else {
            player.controls().play();
        }
    }

    @Override public void pause() { if (player != null) player.controls().setPause(true); }

    @Override
    public void stop() {
        if (player != null) {
            needsReload = true;
            player.controls().stop();
        }
    }

    @Override
    public void seek(double seconds) {
        if (player != null && !needsReload) player.controls().setTime((long) (seconds * 1000));
    }

    @Override
    public void setVolume(double v) {
        volume = v;
        applyAudio();
    }

    @Override
    public void setMute(boolean m) {
        muted = m;
        applyAudio();
    }

    @Override public Node getVideoNode() { return view; }

    @Override
    public void dispose() {
        EmbeddedMediaPlayer p = player;
        MediaPlayerFactory f = factory;
        player = null;
        factory = null;
        if (p != null || f != null) {
            // release off the UI thread so closing the window never freezes
            Thread t = new Thread(() -> {
                try { if (p != null) { p.controls().stop(); p.release(); } } catch (Throwable ignored) { }
                try { if (f != null) f.release(); } catch (Throwable ignored) { }
            }, "vlc-release");
            t.setDaemon(true);
            t.start();
        }
    }
}
