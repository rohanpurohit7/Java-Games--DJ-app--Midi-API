# Electric Guitar Improvisation Studio + MIDI DJ Box

The project is now a guitar-first JavaFX improvisation trainer. Its primary experience combines generated MIDI backing bands, scale overlays, animated guitar licks, lick variation generation, and a persistent saved-lick library. The original MIDI DJ Box remains available inside the application as an advanced backing-groove editor.

## Main Guitar Features

- Electric-guitar fretboard covering six strings and frets 0–12
- Key and scale overlays with separately highlighted root notes
- Minor pentatonic, blues, major pentatonic, natural minor, Dorian, Mixolydian, and Phrygian dominant choices
- Generated backing bands for blues rock, classic rock, funk, ambient minor, Spanish fusion, and world-orchestra styles
- Animated lick playback showing each note on the fretboard in time with MIDI
- `GENERATE NEW LICK` for a fresh scale-aware phrase
- `GENERATE VARIATION` to mutate the current phrase while retaining part of its shape
- `SAVE LICK` and `LOAD SELECTED` for a persistent personal lick library
- Generated MIDI accompaniment rather than copied or embedded commercial backing audio
- Original orchestra/DJ groove interface available through `OPEN LEGACY DJ BOX`

## Guitar Workflow

1. Choose a backing-band style.
2. Select a key and scale.
3. Press `PLAY BACKING` to start the generated orchestra.
4. Improvise using the highlighted fretboard notes.
5. Press `GENERATE NEW LICK` for a suggested phrase.
6. Press `PLAY / ANIMATE LICK` to hear it and watch the notes move across the fretboard.
7. Press `GENERATE VARIATION` to create a related phrase.
8. Press `SAVE LICK` to add it to `saved-guitar-licks.txt`.
9. Double-click a saved entry or use `LOAD SELECTED` to restore it.
10. Open the legacy DJ Box when you want to manually design the backing groove.

## Architecture

```text
GuitarImprovisationStudioApp
|-- key + scale model
|-- generated lick engine
|-- animated JavaFX fretboard
|-- saved lick library
|-- MIDI backing-band sequencer
|-- MIDI lead-guitar sequencer
`-- launch legacy MidiDjBoxFxApp as backing editor
```

## Run

Use JDK 21 or newer.

```bash
gradle run
```

The Gradle application entry point is:

```text
io.github.rohanpurohit7.mididj.GuitarImprovisationStudioApp
```

The original application remains available at:

```text
io.github.rohanpurohit7.mididj.MidiDjBoxFxApp
```

## Project Layout

```text
README.md
|-- build.gradle
|-- src/main/java/io/github/rohanpurohit7/mididj/
|   |-- GuitarImprovisationStudioApp.java
|   `-- MidiDjBoxFxApp.java
|-- src/main/resources/styles/
|   |-- guitar-improv.css
|   `-- midi-djbox.css
`-- musicbox.java
```

## Original DJ and Orchestra Features

The preserved DJ Box includes:

- JavaFX desktop GUI
- Winamp-style dark player skin
- 16 instruments × 16 beat steps
- Indian, Japanese, Chinese, Hungarian, Spanish, English, Mexican, Arabic, and African-inspired orchestra panels
- AI Groove Builder based on raag, mode, maqam, folk-scale, and drum-cycle presets
- Java Sound MIDI playback
- Save/load pattern support
- Tempo control and demo grooves

## Safety and Content

Backing tracks and licks are generated locally through the Java Sound MIDI API. The repository does not copy commercial recordings or backing tracks from third-party services.
