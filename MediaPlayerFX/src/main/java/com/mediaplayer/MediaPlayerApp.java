package com.mediaplayer;

import com.mediaplayer.engine.FxEngine;
import com.mediaplayer.engine.PlaybackEngine;
import com.mediaplayer.engine.VlcEngine;
import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.MediaView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/**
 * JavaFX Media Player - Assignment 2 (BIIM3210 Interactive Multimedia)
 *
 *  Part A: MediaView video playback + Play / Pause / Stop
 *  Part B: keyboard controls (SPACE, S, N, P, UP, DOWN, M  + extras)
 *  Part C: ListView playlist with FileChooser add and remove
 *
 * Files JavaFX can play natively use a MediaView. Everything else (MKV, AVI, MOV, WebM, WMV...)
 * is played by VLC through vlcj, and the app falls back to VLC automatically if JavaFX fails.
 */
public class MediaPlayerApp extends Application {

    /** Extensions JavaFX Media handles itself. */
    private static final Set<String> FX_NATIVE = Set.of(
            "mp4", "m4v", "m4a", "mp3", "wav", "aac", "aif", "aiff", "fxm", "flv", "m3u8");

    /** Extensions offered in the file chooser / accepted by drag and drop. */
    private static final List<String> SUPPORTED = List.of(
            "mp4", "m4v", "mkv", "avi", "mov", "wmv", "flv", "webm", "mpg", "mpeg", "ts", "m2ts",
            "3gp", "ogv", "vob", "mts",
            "mp3", "wav", "aac", "m4a", "flac", "ogg", "oga", "opus", "wma", "aif", "aiff");

    private static final double VOLUME_STEP = 0.05;
    private static final double SEEK_STEP_SECONDS = 5;

    // ---- model ----
    private final ObservableList<File> playlist = FXCollections.observableArrayList();
    private final FxEngine fxEngine = new FxEngine();
    private final VlcEngine vlcEngine = new VlcEngine();
    private PlaybackEngine active;
    private int currentIndex = -1;
    private boolean fallbackTried;
    private boolean playing;
    private boolean muted;
    private boolean userSeeking;
    private double duration;

    // ---- UI ----
    private Stage stage;
    private final ListView<File> listView = new ListView<>(playlist);
    private final StackPane videoPane = new StackPane();
    private final Label placeholder = new Label("Add media files to begin\n(or drag and drop them here)");
    private final Label nowPlaying = new Label("Nothing playing");
    private final Label timeLabel = new Label("0:00 / 0:00");
    private final Label volumeLabel = new Label("50%");
    private final Label statusLabel = new Label();
    private final Slider seekSlider = new Slider(0, 1, 0);
    private final Slider volumeSlider = new Slider(0, 100, 50);
    private final Button playButton = new Button();
    private final Button muteButton = new Button();

    @Override
    public void start(Stage primaryStage) {
        this.stage = primaryStage;

        fxEngine.setListener(new Events(fxEngine));
        vlcEngine.setListener(new Events(vlcEngine));

        // video area
        MediaView mediaView = fxEngine.getMediaView();
        ImageView imageView = vlcEngine.getImageView();
        mediaView.fitWidthProperty().bind(videoPane.widthProperty());
        mediaView.fitHeightProperty().bind(videoPane.heightProperty());
        imageView.fitWidthProperty().bind(videoPane.widthProperty());
        imageView.fitHeightProperty().bind(videoPane.heightProperty());
        mediaView.setVisible(false);
        imageView.setVisible(false);
        placeholder.getStyleClass().add("placeholder");
        videoPane.getStyleClass().add("video-pane");
        videoPane.setMinSize(0, 0);
        videoPane.setPrefSize(820, 460);
        videoPane.getChildren().addAll(placeholder, mediaView, imageView);
        videoPane.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) toggleFullScreen();
        });

        // top / title
        nowPlaying.getStyleClass().add("now-playing");
        HBox top = new HBox(nowPlaying);
        top.getStyleClass().add("top-bar");

        // transport controls
        Button prevButton = styled(new Button(), "Previous (P)");
        Button stopButton = styled(new Button(), "Stop (S)");
        Button nextButton = styled(new Button(), "Next (N)");
        styled(playButton, "Play / Pause (Space)");
        styled(muteButton, "Mute / Unmute (M)");
        Icons.apply(prevButton, Icons.PREV, 22);
        Icons.apply(stopButton, Icons.STOP, 22);
        Icons.apply(nextButton, Icons.NEXT, 22);
        Icons.apply(playButton, Icons.PLAY, 26);
        Icons.apply(muteButton, Icons.VOLUME, 22);
        playButton.getStyleClass().add("primary");
        playButton.setMinWidth(54);

        prevButton.setOnAction(e -> previous());
        playButton.setOnAction(e -> togglePlayPause());
        stopButton.setOnAction(e -> stopPlayback());
        nextButton.setOnAction(e -> next());
        muteButton.setOnAction(e -> toggleMute());

        seekSlider.setFocusTraversable(false);
        seekSlider.setMaxWidth(Double.MAX_VALUE);
        seekSlider.setOnMousePressed(e -> userSeeking = true);
        seekSlider.setOnMouseReleased(e -> {
            if (active != null && duration > 0) active.seek(seekSlider.getValue() * duration);
            userSeeking = false;
        });
        seekSlider.valueProperty().addListener((o, ov, nv) -> {
            if (userSeeking) timeLabel.setText(format(nv.doubleValue() * duration) + " / " + format(duration));
        });

        volumeSlider.setFocusTraversable(false);
        volumeSlider.setPrefWidth(120);
        volumeSlider.valueProperty().addListener((o, ov, nv) -> {
            double v = nv.doubleValue() / 100.0;
            fxEngine.setVolume(v);
            vlcEngine.setVolume(v);
            volumeLabel.setText((int) Math.round(nv.doubleValue()) + "%");
        });
        fxEngine.setVolume(0.5);
        vlcEngine.setVolume(0.5);

        timeLabel.getStyleClass().add("time");
        volumeLabel.getStyleClass().add("time");
        volumeLabel.setMinWidth(40);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        ImageView volTitle = Icons.view(Icons.VOLUME, 18);
        volTitle.setOpacity(0.7);
        HBox buttons = new HBox(8, prevButton, playButton, stopButton, nextButton, muteButton, spacer,
                volTitle, volumeSlider, volumeLabel);
        buttons.setAlignment(Pos.CENTER_LEFT);

        HBox seekRow = new HBox(10, seekSlider, timeLabel);
        seekRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(seekSlider, Priority.ALWAYS);

        VBox controls = new VBox(8, seekRow, buttons);
        controls.getStyleClass().add("controls");

        BorderPane left = new BorderPane();
        left.setTop(top);
        left.setCenter(videoPane);
        left.setBottom(controls);

        // playlist panel
        Label playlistTitle = new Label("PLAYLIST");
        playlistTitle.getStyleClass().add("panel-title");
        Button addButton = styled(new Button("Add"), "Add files (Ctrl+O)");
        Button removeButton = styled(new Button("Remove"), "Remove selected (Delete)");
        Button clearButton = styled(new Button("Clear"), "Clear playlist");
        Icons.apply(addButton, Icons.ADD, 14);
        Icons.apply(removeButton, Icons.REMOVE, 14);
        Icons.apply(clearButton, Icons.CLEAR, 14);
        addButton.setOnAction(e -> addFiles());
        removeButton.setOnAction(e -> removeSelected());
        clearButton.setOnAction(e -> clearPlaylist());
        HBox playlistButtons = new HBox(6, addButton, removeButton, clearButton);

        listView.setFocusTraversable(false);
        listView.setPlaceholder(new Label("Playlist is empty"));
        listView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(File item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    getStyleClass().remove("current-item");
                } else {
                    boolean isCurrent = getIndex() == currentIndex;
                    setText((isCurrent ? "> " : "") + (getIndex() + 1) + ".  " + item.getName());
                    if (isCurrent) {
                        if (!getStyleClass().contains("current-item")) getStyleClass().add("current-item");
                    } else {
                        getStyleClass().remove("current-item");
                    }
                }
            }
        });
        // clicking an item makes the player switch to it automatically
        listView.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                int idx = listView.getSelectionModel().getSelectedIndex();
                if (idx >= 0 && idx != currentIndex) playIndex(idx);
                else if (idx >= 0 && e.getClickCount() == 2) playIndex(idx);
            }
        });
        VBox.setVgrow(listView, Priority.ALWAYS);

        VBox right = new VBox(8, playlistTitle, playlistButtons, listView);
        right.getStyleClass().add("side-panel");
        right.setPrefWidth(300);
        right.setMinWidth(220);

        statusLabel.setText("Space: Play/Pause   S: Stop   N: Next   P: Previous   Up/Down: Volume   M: Mute"
                + "   Left/Right: Seek   F: Fullscreen   Ctrl+O: Add   Del: Remove   Enter: Play selected");
        statusLabel.getStyleClass().add("status");
        HBox statusBar = new HBox(statusLabel);
        statusBar.getStyleClass().add("status-bar");

        BorderPane root = new BorderPane();
        root.setCenter(left);
        root.setRight(right);
        root.setBottom(statusBar);

        Scene scene = new Scene(root, 1150, 680);
        var css = getClass().getResource("/com/mediaplayer/style.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());

        // Keyboard shortcuts. A FILTER is used so they work no matter which control has focus.
        scene.addEventFilter(KeyEvent.KEY_PRESSED, this::handleKey);

        // Drag and drop files onto the window
        scene.setOnDragOver((DragEvent e) -> {
            if (e.getDragboard().hasFiles()) e.acceptTransferModes(TransferMode.COPY);
            e.consume();
        });
        scene.setOnDragDropped((DragEvent e) -> {
            if (e.getDragboard().hasFiles()) addToPlaylist(e.getDragboard().getFiles());
            e.setDropCompleted(true);
            e.consume();
        });

        stage.setTitle("JavaFX Media Player");
        stage.setScene(scene);
        stage.setMinWidth(760);
        stage.setMinHeight(480);
        stage.setOnCloseRequest(e -> shutdown());
        stage.show();
        root.requestFocus();
    }

    private void setPlayIcon(boolean isPlaying) {
        Icons.apply(playButton, isPlaying ? Icons.PAUSE : Icons.PLAY, 26);
    }

    private Button styled(Button b, String tip) {
        b.setFocusTraversable(false);
        b.setTooltip(new Tooltip(tip));
        return b;
    }

    // ------------------------------------------------------------------ keyboard

    private void handleKey(KeyEvent e) {
        KeyCode code = e.getCode();

        if (e.isShortcutDown() && code == KeyCode.O) {
            addFiles();
            e.consume();
            return;
        }
        if (e.isShortcutDown() || e.isAltDown()) return;

        switch (code) {
            case SPACE -> { togglePlayPause(); e.consume(); }
            case S -> { stopPlayback(); e.consume(); }
            case N -> { next(); e.consume(); }
            case P -> { previous(); e.consume(); }
            case UP -> { changeVolume(+VOLUME_STEP); e.consume(); }
            case DOWN -> { changeVolume(-VOLUME_STEP); e.consume(); }
            case M -> { toggleMute(); e.consume(); }
            case LEFT -> { seekBy(-SEEK_STEP_SECONDS); e.consume(); }
            case RIGHT -> { seekBy(+SEEK_STEP_SECONDS); e.consume(); }
            case F -> { toggleFullScreen(); e.consume(); }
            case DELETE -> { removeSelected(); e.consume(); }
            case ENTER -> {
                int idx = listView.getSelectionModel().getSelectedIndex();
                if (idx >= 0) playIndex(idx);
                e.consume();
            }
            default -> { }
        }
    }

    // ------------------------------------------------------------------ player actions

    private void togglePlayPause() {
        if (active == null || currentIndex < 0) {
            if (playlist.isEmpty()) {
                addFiles();
                return;
            }
            int idx = listView.getSelectionModel().getSelectedIndex();
            playIndex(idx >= 0 ? idx : 0);
            return;
        }
        if (playing) active.pause();
        else active.play();
    }

    private void stopPlayback() {
        if (active != null) active.stop();
        playing = false;
        setPlayIcon(false);
        seekSlider.setValue(0);
        timeLabel.setText("0:00 / " + format(duration));
    }

    private void next() {
        if (playlist.isEmpty()) return;
        playIndex((currentIndex + 1) % playlist.size());
    }

    private void previous() {
        if (playlist.isEmpty()) return;
        playIndex(currentIndex <= 0 ? playlist.size() - 1 : currentIndex - 1);
    }

    private void changeVolume(double delta) {
        double v = Math.max(0, Math.min(100, volumeSlider.getValue() + delta * 100));
        volumeSlider.setValue(v);
        if (muted && delta > 0) toggleMute();
    }

    private void toggleMute() {
        muted = !muted;
        fxEngine.setMute(muted);
        vlcEngine.setMute(muted);
        Icons.apply(muteButton, muted ? Icons.MUTED : Icons.VOLUME, 22);
    }

    private void seekBy(double seconds) {
        if (active == null || duration <= 0) return;
        double current = seekSlider.getValue() * duration;
        active.seek(Math.max(0, Math.min(duration, current + seconds)));
    }

    private void toggleFullScreen() {
        stage.setFullScreen(!stage.isFullScreen());
    }

    // ------------------------------------------------------------------ playlist

    private void addFiles() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Add media files");
        List<String> patterns = SUPPORTED.stream().map(x -> "*." + x).toList();
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("All media (video + audio)", patterns),
                new FileChooser.ExtensionFilter("All files", "*.*"));
        List<File> files = fc.showOpenMultipleDialog(stage);
        if (files != null) addToPlaylist(files);
    }

    private void addToPlaylist(List<File> files) {
        boolean nothingPlaying = currentIndex < 0;
        int firstNew = playlist.size();
        for (File f : files) {
            if (f.isFile() && SUPPORTED.contains(extension(f)) && !playlist.contains(f)) playlist.add(f);
        }
        if (nothingPlaying && playlist.size() > firstNew) playIndex(firstNew);
    }

    private void removeSelected() {
        int idx = listView.getSelectionModel().getSelectedIndex();
        if (idx < 0) return;
        boolean removingCurrent = idx == currentIndex;
        playlist.remove(idx);
        if (removingCurrent) {
            currentIndex = -1;
            if (playlist.isEmpty()) {
                resetPlayer();
            } else {
                playIndex(Math.min(idx, playlist.size() - 1));
            }
        } else if (idx < currentIndex) {
            currentIndex--;
        }
        listView.refresh();
    }

    private void clearPlaylist() {
        playlist.clear();
        resetPlayer();
    }

    private void resetPlayer() {
        if (active != null) active.stop();
        active = null;
        currentIndex = -1;
        playing = false;
        duration = 0;
        setPlayIcon(false);
        seekSlider.setValue(0);
        timeLabel.setText("0:00 / 0:00");
        nowPlaying.setText("Nothing playing");
        fxEngine.getVideoNode().setVisible(false);
        vlcEngine.getVideoNode().setVisible(false);
        placeholder.setVisible(true);
        listView.refresh();
    }

    // ------------------------------------------------------------------ playback

    private void playIndex(int index) {
        if (index < 0 || index >= playlist.size()) return;
        currentIndex = index;
        fallbackTried = false;
        listView.getSelectionModel().select(index);
        listView.refresh();
        File file = playlist.get(index);
        nowPlaying.setText(file.getName());
        start(file, FX_NATIVE.contains(extension(file)));
    }

    private void start(File file, boolean useFx) {
        PlaybackEngine chosen = useFx ? fxEngine : vlcEngine;
        if (active != null && active != chosen) active.stop();
        active = chosen;
        fxEngine.getVideoNode().setVisible(chosen == fxEngine);
        vlcEngine.getVideoNode().setVisible(chosen == vlcEngine);
        placeholder.setVisible(false);
        duration = 0;
        seekSlider.setValue(0);
        timeLabel.setText("0:00 / 0:00");
        chosen.load(file);
    }

    private void handleEngineError(PlaybackEngine source, String message) {
        if (source != active || currentIndex < 0) return;
        File file = playlist.get(currentIndex);
        if (source == fxEngine && !fallbackTried) {
            // JavaFX cannot decode this file (e.g. HEVC mp4) -> try VLC instead
            fallbackTried = true;
            start(file, false);
            return;
        }
        playing = false;
        setPlayIcon(false);
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Cannot play file");
        alert.setHeaderText(file.getName());
        alert.setContentText(message == null ? "Unknown error" : message);
        alert.initOwner(stage);
        alert.show();
    }

    private void shutdown() {
        fxEngine.dispose();
        vlcEngine.dispose();
    }

    @Override
    public void stop() {
        shutdown();
    }

    /** Receives events from one engine and ignores them if that engine is not the active one. */
    private class Events implements PlaybackEngine.Listener {
        private final PlaybackEngine owner;

        Events(PlaybackEngine owner) {
            this.owner = owner;
        }

        @Override public void onState(boolean isPlaying) {
            if (owner != active) return;
            playing = isPlaying;
            setPlayIcon(isPlaying);
        }

        @Override public void onTime(double current, double total) {
            if (owner != active) return;
            duration = total;
            if (!userSeeking) {
                seekSlider.setValue(total > 0 ? Math.min(1.0, current / total) : 0);
                timeLabel.setText(format(current) + " / " + format(total));
            }
        }

        @Override public void onEnded() {
            if (owner != active) return;
            if (currentIndex >= 0 && currentIndex < playlist.size() - 1) next();
            else stopPlayback();
        }

        @Override public void onError(String message) {
            Platform.runLater(() -> handleEngineError(owner, message));
        }
    }

    // ------------------------------------------------------------------ helpers

    private static String extension(File f) {
        String n = f.getName();
        int dot = n.lastIndexOf('.');
        return dot < 0 ? "" : n.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static String format(double seconds) {
        if (Double.isNaN(seconds) || seconds < 0) seconds = 0;
        long s = (long) seconds;
        long h = s / 3600, m = (s % 3600) / 60, sec = s % 60;
        return h > 0 ? String.format("%d:%02d:%02d", h, m, sec) : String.format("%d:%02d", m, sec);
    }
}
