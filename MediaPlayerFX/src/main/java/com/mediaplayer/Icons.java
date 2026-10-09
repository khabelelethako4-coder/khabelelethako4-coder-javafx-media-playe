package com.mediaplayer;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 * All picture (PNG) paths and the functions that load/show them live here.
 *
 * The files are stored inside the project at:
 *     src/main/resources/com/mediaplayer/icons/
 * and are loaded from the classpath using the paths below.
 */
public final class Icons {

    private Icons() { }

    // ---------------------------------------------------------------- URLs (paths) of the photos
    private static final String FOLDER = "/com/mediaplayer/icons/";

    public static final String PLAY   = FOLDER + "play.png";
    public static final String PAUSE  = FOLDER + "pause.png";
    public static final String STOP   = FOLDER + "stop.png";
    public static final String PREV   = FOLDER + "prev.png";
    public static final String NEXT   = FOLDER + "next.png";
    public static final String VOLUME = FOLDER + "volume.png";
    public static final String MUTED  = FOLDER + "muted.png";
    public static final String ADD    = FOLDER + "add.png";
    public static final String REMOVE = FOLDER + "remove.png";
    public static final String CLEAR  = FOLDER + "clear.png";

    // ---------------------------------------------------------------- functions

    private static final Map<String, Image> CACHE = new HashMap<>();

    /** FUNCTION 1: loads a picture from its path (each picture is only read once). */
    public static Image load(String path) {
        return CACHE.computeIfAbsent(path, p -> {
            InputStream in = Icons.class.getResourceAsStream(p);
            if (in == null) throw new IllegalStateException("Icon not found: " + p);
            return new Image(in);
        });
    }

    /** FUNCTION 2: creates a ready-to-use ImageView of the picture at the given size (pixels). */
    public static ImageView view(String path, double size) {
        ImageView iv = new ImageView(load(path));
        iv.setFitWidth(size);
        iv.setFitHeight(size);
        iv.setPreserveRatio(true);
        iv.setSmooth(true);
        return iv;
    }

    /** FUNCTION 3: puts the picture on a button (any text on the button stays next to it). */
    public static void apply(Button button, String path, double size) {
        button.setGraphic(view(path, size));
        button.setContentDisplay(ContentDisplay.LEFT);
    }
}
