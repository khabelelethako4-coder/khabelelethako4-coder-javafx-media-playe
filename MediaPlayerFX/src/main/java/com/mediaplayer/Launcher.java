package com.mediaplayer;

import javafx.application.Application;

/** Plain launcher class (required so the shaded jar can start JavaFX without module-path errors). */
public class Launcher {
    public static void main(String[] args) {
        Application.launch(MediaPlayerApp.class, args);
    }
}
