# Amp Studio Guitar App

A JavaFX electric-guitar improvisation studio built around an amplifier-style interface, independent backing and lead playback, scale-aware lick generation, articulated fretboard tracing, continuous improvisation, and optional SoundFont guitar tones.

The active application is:

```text
io.github.rohanpurohit7.mididj.AmpStudioGuitarApp
```

Run it with:

```bash
gradle run
```

## Core Experience

1. Choose a backing style, key, and scale.
2. Start the backing band.
3. Generate a scale-aware guitar lick.
4. Play the lick over the backing without replacing the accompaniment.
5. Follow the note path and articulation symbols on the fretboard.
6. Use trace-only mode to study the phrase silently.
7. Start continuous improvisation for an ongoing stream of generated phrases.
8. Adjust the amp-style tone controls or apply the recommended guitar and tone profile.
9. Install or load a supported guitar SoundFont for more realistic playback.

## Main Features

### Independent backing and lead playback

The audio engine uses separate playback lanes for accompaniment and lead guitar. Starting, regenerating, stopping, or tracing a lead phrase does not intentionally stop the active backing band.

Backing can use:

- Generated MIDI arrangements
- User-selected WAV or AIFF backing tracks
- Continuous looping while lead phrases play on a separate lane

### Guitar lick generation

The app generates phrases from the selected key and scale, including:

- Minor Pentatonic
- Blues
- Major Pentatonic
- Natural Minor
- Dorian
- Mixolydian
- Phrygian Dominant

Generated phrases can be turned into related variations and used as the basis for continuous improvisation.

### Articulated guitar playback

The lead engine models guitar-oriented techniques through MIDI timing, note transitions, velocity, pitch bend, modulation, and phrase shaping.

Supported articulation concepts include:

- Hammer-ons — `H`
- Pull-offs — `P`
- Bends — `B↑`
- Vibrato — `~`
- Slides — `/`
- Picked notes

The active technique is shown on the fretboard while the phrase plays.

### Two continuous-improvisation modes

The interface keeps continuous improvisation intentionally simple:

- **SEE TRACE** — displays the continuing fretboard path without lead audio
- **PLAY CONTINUOUS IMPROV** — plays theory-aware articulated phrases over the backing band

The phrase stream alternates between new motifs and variations so it does not simply repeat one fixed lick.

### Amp-style tone controls

The responsive amp panel includes controls for:

- Twang
- Warmth
- Drive
- Brightness
- Sustain
- Reverb
- Chorus
- Vibrato
- Human feel

The recommendation engine can select a scale, generic guitar archetype, pickup position, and tone profile for the chosen backing style. All settings remain manually adjustable.

### Guitar SoundFont support

The app supports external `.sf2` SoundFonts through the Java Sound synthesizer.

The **INSTALL FREEPATS GUITAR** control installs the configured open-license guitar library when the remote package is available. Previously installed compatible SoundFonts can be loaded automatically. Java's default General MIDI soundbank remains a fallback when no external guitar bank is available.

A SoundFont can improve the sampled guitar timbre, but realism still depends on the quality and articulation coverage of the selected library.

### Fretboard visualization

The six-string fretboard displays:

- Frets 0–12
- Notes in the selected scale
- Root notes
- The currently active lick note
- Technique-specific highlighting for hammer-ons, pull-offs, bends, vibrato, and slides

### Responsive amplifier interface

The JavaFX interface includes:

- Amp-head control panel
- Speaker-cabinet fretboard area
- Responsive wide and narrow layouts
- Scrollable fretboard where needed
- Real guitar photograph panel with source attribution
- Optional tongue-out dog reaction stickers after selected actions

## Current Controls

```text
PLAY BACKING BAND
GENERATE LICK
PLAY LICK OVER BAND
SEE TRACE
PLAY CONTINUOUS IMPROV
STOP LEAD / TRACE
STOP BAND
INSTALL FREEPATS GUITAR
CHOOSE WAV/AIFF BAND
AI MATCH GUITAR + TONE
APPLY AMP SETTINGS
```

## Requirements

- JDK 21 or newer
- Gradle 8.x
- JavaFX 21
- Graphical desktop environment
- Working MIDI synthesizer
- Internet access for remote SoundFont installation and guitar photographs

## Build and Run

```bash
gradle clean classes
gradle run
```

The Gradle entry point is configured as:

```groovy
application {
    mainClass = 'io.github.rohanpurohit7.mididj.AmpStudioGuitarApp'
}
```

## Active Source Layout

```text
src/main/java/io/github/rohanpurohit7/mididj/
├── AmpStudioGuitarApp.java
├── StudioAudioEngine.java
├── StudioBackingCatalog.java
├── GuitarArticulationEngine.java
├── GuitarTheory.java
├── GuitarStyleAdvisor.java
├── GuitarPhotoCard.java
├── FreePatsGuitarLibrary.java
├── ContinuousImprovPlanner.java
└── DogLickSticker.java
```

### Class responsibilities

| Class | Responsibility |
|---|---|
| `AmpStudioGuitarApp` | Main JavaFX application, amp interface, fretboard, controls, playback coordination |
| `StudioAudioEngine` | Independent backing and lead sequencers, lossless backing playback, SoundFont loading, tone controls |
| `StudioBackingCatalog` | Backing-style definitions, tempo, programs, progressions, and optional stem names |
| `GuitarArticulationEngine` | Articulation selection and articulated MIDI sequence generation |
| `GuitarTheory` | Scales, fretboard mappings, lick generation, and phrase variation |
| `GuitarStyleAdvisor` | Explainable scale, guitar-archetype, pickup, and tone recommendations |
| `GuitarPhotoCard` | Responsive licensed guitar-photo display and attribution |
| `FreePatsGuitarLibrary` | Open-license guitar SoundFont download and installation support |
| `ContinuousImprovPlanner` | Stateful continuous phrase and motif-variation planning |
| `DogLickSticker` | Temporary reaction-photo overlays with original captions |

## Audio Architecture

```text
Generated MIDI backing ──> Backing sequencer ──┐
                                               ├──> Synthesizer / audio output
Articulated guitar lick ──> Lead sequencer ────┘

WAV or AIFF backing ──────> JavaFX MediaPlayer
```

The independent lanes are intended to let a lead lick or continuous improvisation play over the accompaniment instead of replacing it.

## Backing-Track Library

To use recorded backing tracks:

1. Prepare legally licensed WAV or AIFF files.
2. Select **CHOOSE WAV/AIFF BAND**.
3. Point the app to the backing-track directory.
4. Choose a matching style.
5. Start the backing band.

When no matching audio stem is available, the app uses its generated MIDI backing arrangement.

## Project Scope

Current development is focused on the core product experience:

- Reliable simultaneous backing and lead playback
- Better guitar SoundFont integration
- More natural phrasing and timing
- Guitar articulations
- Continuous scale-aware improvisation
- Fretboard tracing
- Responsive amplifier-themed UX
- Reliable guitar-photo loading

The deleted DJ Box and earlier experimental launchers are no longer part of the active source tree. They remain recoverable through Git history.

## Known Limitations

- Sound quality depends heavily on the installed SoundFont and audio hardware.
- General MIDI fallback instruments can still sound synthetic.
- MIDI pitch bends and legato gestures approximate physical guitar techniques; they are not a replacement for a dedicated sampled-guitar engine.
- Remote images and the optional SoundFont installer require network access.
- Recorded backing tracks are not bundled unless their redistribution rights are explicitly documented.

## Content and Licensing

The repository does not bundle commercial guitar libraries or copyrighted backing recordings. External SoundFonts, audio tracks, and photographs must have licenses that permit their intended use and redistribution. Attribution is displayed for remotely sourced Creative Commons photographs where configured.
