# Run and Play Guide - MIDI DJ Box

## 1. Prerequisites

Install:

- JDK 21 or newer
- Gradle
- IntelliJ IDEA, Cursor, VS Code, or another Java IDE

Check your versions:

```bash
java -version
gradle -version
```

## 2. Open the Project in IntelliJ IDEA

1. Open IntelliJ IDEA.
2. Select **File > Open**.
3. Choose the cloned `Java-Games--DJ-app--Midi-API` repository folder.
4. When prompted, open it as a **Gradle project**.
5. Wait for Gradle sync to finish.
6. Confirm the project SDK is JDK 21 or newer under **File > Project Structure > Project SDK**.

## 3. Open the Project in Cursor

1. Open Cursor.
2. Select **Open Folder**.
3. Choose the `Java-Games--DJ-app--Midi-API` repository folder.
4. Let Cursor index the project.
5. Open the integrated terminal.
6. Run:

```bash
gradle run
```

## 4. Run from Any IDE Terminal

From the repository root:

```bash
gradle run
```

On Windows, this project also includes a launcher that can find JDKs installed under `%USERPROFILE%\.jdks`:

```bat
run-midi-djbox.bat
```

The JavaFX window should open with the title:

```text
MIDI DJ Box - Winamp Skin JavaFX Edition
```

## 5. Run by Main Class

After Gradle import, run this class:

```text
io.github.rohanpurohit7.mididj.MidiDjBoxFxApp
```

## 6. How to Use the MIDI DJ Box

1. Each row is a percussion instrument.
2. Each column is one step in a 16-step pattern.
3. Click pads to turn beats on or off.
4. Press **PLAY** to start looping the groove.
5. Press **STOP** to stop playback.
6. Move the **TEMPO** slider to change BPM.
7. Press **DEMO GROOVE** to load a starter rhythm.
8. Choose an **ORCHESTRA** and **SCALE / GROOVE** to switch the instrument panel.
9. Press **AI GROOVE** to generate a pattern from the selected cultural scale or drum cycle.
10. Press **SAVE** to save the current pattern to `midi-djbox-pattern.txt`.
11. Press **LOAD** to reload the saved pattern.
12. Press **CLEAR** to reset the grid.

## 7. Rapid Prototyping Ideas

Use Cursor or IntelliJ to quickly modify:

| File | What to Prototype |
|---|---|
| `MidiDjBoxFxApp.java` | instruments, beat grid, demo groove, playback behavior |
| `midi-djbox.css` | Winamp-style skin, colors, glowing pads, layout |
| `winamp-skin-concept.svg` | player mockup and visual direction |
| `musicbox.java` | preserved original Swing implementation |

## 8. Common Fixes

### No sound plays

Confirm your system has an available MIDI synthesizer and that audio output is not muted. The Java Sound API depends on local sound support.

### JavaFX window does not open

Confirm you opened the project as a Gradle project and that the Gradle JavaFX plugin downloaded dependencies.

### Wrong JDK

Set the SDK to JDK 21 or newer.

### Gradle command not found

Install Gradle or run from an IDE that includes Gradle support.

## 9. Portfolio Demo Script

Say:

> This started as a Swing MIDI checkbox sequencer. I preserved the original file and added a JavaFX Winamp-style groovebox UI using the Java Sound MIDI API, a 16-step percussion grid, tempo control, save/load behavior, and Gradle packaging so it can run cleanly in IntelliJ or Cursor.
