# MIDI DJ Box - Winamp Skin JavaFX Edition

A modernized JavaFX version of the original Swing MIDI sequencer. The original `musicbox.java` file remains preserved, while the new application adds a Winamp-inspired player skin, 16-step beat grid, transport controls, tempo slider, demo groove, save/load pattern support, and IntelliJ/Cursor-friendly Gradle configuration.

## Highlights

- JavaFX desktop GUI
- Winamp-style dark player skin
- 16 instruments x 16 beat steps
- Ethnic orchestra selector for Indian, Japanese, Chinese, Hungarian, Spanish, English, Mexican, Arabic, and African-inspired panels
- AI Groove Builder that fills the grid from selected raag, mode, maqam, folk scale, or drum-cycle presets
- Java Sound MIDI API playback
- Play, stop, clear, save, load, and demo groove controls
- Tempo slider with live BPM readout
- Preserved original Swing implementation

## Navigation

```text
README.md
|-- build.gradle                         JavaFX Gradle build
|-- run-midi-djbox.bat                   Windows Gradle launcher
|-- run-midi-djbox-standalone.bat        Windows direct JavaFX launcher
|-- settings.gradle                      Gradle project settings
|-- docs/
|   `-- winamp-skin-concept.svg          Visual skin concept
|-- src/main/java/io/github/rohanpurohit7/mididj/
|   `-- MidiDjBoxFxApp.java              JavaFX MIDI sequencer entry point
|-- src/main/resources/styles/
|   `-- midi-djbox.css                   Winamp-inspired skin
`-- musicbox.java                        Original Swing MIDI project
```

## Run in IntelliJ IDEA or Cursor

1. Open the repository folder as a Gradle project.
2. Let the IDE import Gradle dependencies.
3. Use JDK 21 or newer.
4. Run the Gradle task or the JavaFX main class.

```bash
gradle run
```

On Windows, if `java` is not on PATH but a JDK is installed under `%USERPROFILE%\.jdks`, run:

```bat
run-midi-djbox.bat
```

The Gradle launcher sequence is:

```text
run-midi-djbox.bat -> Gradle application plugin -> io.github.rohanpurohit7.mididj.MidiDjBoxFxApp
```

## Run Standalone Without Gradle

You can also compile and run the JavaFX main class directly:

```bat
run-midi-djbox-standalone.bat
```

The direct JavaFX launcher sequence is:

```text
run-midi-djbox-standalone.bat -> javac -> java --module-path JavaFX -> io.github.rohanpurohit7.mididj.MidiDjBoxFxApp
```

To verify direct compilation without opening the app window:

```bat
run-midi-djbox-standalone.bat --compile-only
```

The standalone launcher runs:

```text
io.github.rohanpurohit7.mididj.MidiDjBoxFxApp
```

It looks for JavaFX in this order:

- `%JAVAFX_HOME%\lib`
- `%PATH_TO_FX%`
- OpenJFX jars already downloaded by Gradle under `%USERPROFILE%\.gradle`

If you use an IDE run configuration instead of the script, use the same main class and add VM options like:

```text
--module-path C:\path\to\javafx-sdk-21.0.5\lib --add-modules javafx.controls,javafx.graphics
```

Also keep `src\main\resources` on the runtime classpath so `/styles/midi-djbox.css` can load.

## Play Directions

1. Launch the app with `run-midi-djbox.bat` or `run-midi-djbox-standalone.bat`.
2. Choose an orchestra and groove preset from the top controls.
3. Click pads in the 16-step grid to build a rhythm.
4. Press `PLAY` to start MIDI playback.
5. Adjust tempo with the BPM slider while the groove is playing.
6. Use `DEMO`, `CLEAR`, `SAVE`, and `LOAD` to explore or persist patterns.

## Musician and DJ Playbook

Open `docs/MUSICIAN_DJ_PLAYBOOK.md` for a practical runbook on groove mapping, cultural orchestra presets, AI groove generation, demo patterns, and rhythm discovery workflows.

You can also run the main class directly after Gradle import:

```text
io.github.rohanpurohit7.mididj.MidiDjBoxFxApp
```

Optional: if you want a checked-in Gradle wrapper later, run `gradle wrapper` locally and commit the generated wrapper files.

## UX Concept

Open `docs/winamp-skin-concept.svg` to preview the visual direction: a compact dark audio-player shell, LCD status display, glowing step pads, transport controls, and tempo slider.

## Design Narrative

The original project demonstrated MIDI sequencing with a Swing checkbox grid. The JavaFX version turns the exercise into a polished beat-making tool. Each checked pad becomes a percussion note in the Java Sound MIDI sequencer. The UI acts like a small groovebox: build a pattern, press play, adjust tempo, save the pattern, and reload it later.

## Portfolio Value

This project now demonstrates:

- Java Sound MIDI API usage
- event-driven JavaFX GUI programming
- desktop application styling
- sequencer state management
- simple persistence using a text pattern file
- Gradle-based Java application packaging
