# JavaFX Media Player (BIIM3210 Assignment 2)

## Requirements
* JDK 17 or newer
* Maven 3.8+ (or open the folder in IntelliJ / NetBeans / Eclipse as a Maven project)
* **VLC media player (64-bit)** from https://www.videolan.org  - needed for MKV, AVI, MOV, WebM, WMV, MPG, FLAC, OGG, HEVC etc.
  (MP4/H.264, MP3, WAV, AAC play without VLC.) Install the 64-bit VLC if your Java is 64-bit.

## Run
    mvn clean javafx:run

## Build a runnable jar
    mvn clean package
    java -jar target/media-player-fx-1.0.0-shaded.jar

## Keyboard
| Key | Action |
|---|---|
| Space | Play / Pause |
| S | Stop |
| N / P | Next / Previous |
| Up / Down | Volume up / down |
| M | Mute / Unmute |
| Left / Right | Seek -5s / +5s |
| F | Fullscreen (Esc exits) |
| Ctrl+O | Add files |
| Delete | Remove selected item |
| Enter | Play selected item |

Click a playlist item to play it. Files can also be dragged onto the window.
