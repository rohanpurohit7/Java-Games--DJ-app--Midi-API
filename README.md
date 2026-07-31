# Electric Guitar Improvisation Studio + MIDI DJ Box

The project is now a guitar-first JavaFX improvisation trainer. Its primary experience combines generated MIDI backing bands, scale overlays, animated guitar licks, lick variation generation, and a persistent saved-lick library. The original MIDI DJ Box remains available inside the application as an advanced backing-groove editor.

## Start Here

- [Guitar Improvisation Demo with annotated screenshots](docs/GUITAR_IMPROV_DEMO.md)
- [DevOps build and quality gate](.github/workflows/guitar-improv-devops-gate.yml)
- Run locally with `gradle run`

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
3. Start the generated backing band.
4. Improvise using the highlighted fretboard notes.
5. Press `GENERATE NEW LICK` for a suggested phrase.
6. Play the animated lick to hear it and watch the notes move across the fretboard.
7. Press `GENERATE VARIATION` to create a related phrase.
8. Press `SAVE LICK` to add it to `saved-guitar-licks.txt`.
9. Select a saved entry and use `LOAD SELECTED` to restore it.
10. Open the legacy DJ Box when you want to manually design the backing groove.

## Architecture

```text
GuitarImprovisationStudioApp
|-- GuitarTheory headless scale and lick engine
|-- key + scale model
|-- generated lick and variation engine
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

## Test and Build

Run the same core compilation and unit-test tasks used by the DevOps gate:

```bash
gradle --no-daemon clean test classes
```

The test suite validates scale intervals, fretboard overlays, scale-safe lick generation, phrase variations, and invalid-input handling. GitHub Actions additionally verifies the demo files, both application entry points, local-only lick persistence, and obvious committed-secret patterns.

## Project Layout

```text
README.md
|-- build.gradle
|-- .github/workflows/guitar-improv-devops-gate.yml
|-- docs/GUITAR_IMPROV_DEMO.md
|-- docs/screenshots/*.svg
|-- src/main/java/io/github/rohanpurohit7/mididj/
|   |-- GuitarImprovisationStudioApp.java
|   |-- GuitarTheory.java
|   `-- MidiDjBoxFxApp.java
|-- src/test/java/io/github/rohanpurohit7/mididj/GuitarTheoryTest.java
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
