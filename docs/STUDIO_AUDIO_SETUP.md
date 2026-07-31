# Studio-Quality Guitar and Backing Audio Setup

The application now has two playback tiers:

1. **Studio mode** — an external licensed `.sf2` SoundFont for guitar and instruments, plus lossless `.wav` or `.aiff` backing tracks.
2. **Generated fallback mode** — Java MIDI arrangements used when studio assets are not installed.

The fallback is useful for practicing, but it will not sound like a professionally recorded guitar and rhythm section. Studio quality comes from the sample library and backing-track production quality.

## 1. Install a Guitar SoundFont

Obtain a legally licensed SoundFont containing multisampled electric guitars, basses, keyboards, drums, and orchestral instruments.

In the application:

1. Press **LOAD GUITAR SF2**.
2. Select the `.sf2` file.
3. Confirm that the header shows the loaded soundbank name instead of `(fallback)`.
4. Select a guitar tone preset or adjust the tone overlay.

Do not commit commercial SoundFonts to this public repository unless their license explicitly permits redistribution.

## 2. Install Lossless Backing Tracks

Place your legally licensed or personally recorded backing tracks in one folder. Use 44.1 kHz or 48 kHz, 24-bit WAV when possible. AIFF is also supported.

Press **CHOOSE LOSSLESS BACKING LIBRARY** and choose that folder.

The file names must match the catalog identifiers listed in `backing-tracks/README.md`. For example:

```text
slow-chicago-blues.wav
neo-soul-pocket.wav
santana-minor.wav
midnight-rnb.wav
```

When a matching lossless track exists, **PLAY STUDIO BACKING** uses it. When the file is missing, the app generates a richer MIDI fallback and tells you the exact filename it expected.

## 3. Guitar Tone Overlay

The overlay provides these controls:

- **Twang** — faster attack and brighter upper harmonics.
- **Warmth / Body** — timbral resonance and perceived body.
- **Drive** — stronger note velocity and expression shaping.
- **Brightness** — MIDI filter/brightness control.
- **Sustain** — longer release and lead-note duration.
- **Reverb** — room/plate-style SoundFont reverb send.
- **Chorus** — stereo width and modulation send.
- **Vibrato** — modulation depth.
- **Human Feel** — timing and velocity variation so phrases are not quantized mechanically.

Included presets:

- Soulful Clean
- Texas Blues
- Ambient Lead

The final result depends on whether the chosen SoundFont responds to the corresponding MIDI controllers.

## 4. Thirty Backing Styles

The app includes 30 catalog entries across blues, soul, gospel, funk, rock, jazz, fusion, Latin, world, reggae, country, ambient, and R&B.

The catalog supplies tempo, chord progression, bass movement, guitar program, drum feel, and the expected lossless-stem filename. It is defined in:

```text
src/main/java/io/github/rohanpurohit7/mididj/StudioBackingCatalog.java
```

## 5. Recommended Recording Workflow

For genuinely release-ready sound:

1. Record or obtain a licensed backing arrangement as 24-bit WAV stems.
2. Load a high-quality licensed SF2 for interactive guitar-lick playback.
3. Use the app to practice, generate, animate, vary, and save licks.
4. Record the real electric guitar through an audio interface and amp simulator or miked amplifier.
5. Mix and master in a DAW.

The application can provide high-quality rehearsal playback and musical guidance, but it is not a replacement for a professional guitar recording chain, amp modeler, DAW, and mastering process.
