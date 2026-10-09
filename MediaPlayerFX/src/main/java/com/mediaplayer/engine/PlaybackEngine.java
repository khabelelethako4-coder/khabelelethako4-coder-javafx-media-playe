package com.mediaplayer.engine;

import java.io.File;
import javafx.scene.Node;

/** Common interface so the UI does not care whether JavaFX or VLC is playing the file. */
public interface PlaybackEngine {

    interface Listener {
        void onState(boolean playing);
        void onTime(double currentSeconds, double totalSeconds);
        void onEnded();
        void onError(String message);
    }

    void setListener(Listener listener);

    /** Loads the file and starts playing it automatically. */
    void load(File file);

    void play();
    void pause();
    void stop();

    void seek(double seconds);

    /** volume between 0.0 and 1.0 */
    void setVolume(double volume);
    void setMute(boolean muted);

    /** The node that shows the picture (MediaView or ImageView). */
    Node getVideoNode();

    void dispose();
}
