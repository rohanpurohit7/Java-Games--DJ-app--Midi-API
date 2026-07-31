# Amp Studio Guitar App

A React/Web Audio guitar-improvisation studio built around real backing-track audio, a sampled FreePats electric guitar, groove-aware phrasing, one-position-at-a-time fretboard tracing, and expressive techniques such as hammer-ons, pull-offs, bends, vibrato, and slides.

## Launch

Requirements: Node.js 22+ and npm 10+.

```bash
git clone https://github.com/rohanpurohit7/AmpStudioGuitarApp.git
cd AmpStudioGuitarApp
npm install
npm run dev
```

Open:

```text
http://localhost:5173
```

Production build:

```bash
npm run build
npm run preview
```

## First-run assets

`npm run dev` and `npm run build` automatically run `npm run assets`, which prepares:

```text
public/spessasynth_processor.min.js
public/soundfonts/freepats-clean-electric-guitar.sf2
public/assets/studio-electric-guitar.jpg
```

The FreePats bank is used for the lead-guitar lane. Browser audio starts only after a user click because of browser autoplay restrictions.

## Using real backing tracks

The app no longer generates oscillator or MIDI-style backing tones. It provides a curated catalog of 20 Creative Commons instrumental tracks covering:

- Slow blues and blues rock
- Southern blues
- Country blues rock
- Soul and R&B
- Neo-soul and soulful trap
- Funk and funk-pop
- Jazz R&B and Dixieland jazz
- Boogie woogie
- Hip-hop instrumentals

Each catalog entry includes its source page, license, key, BPM, meter, feel, and a suggested chord progression.

To use one:

1. Select a track in **CURATED REAL-AUDIO LIBRARY**.
2. Click **OPEN DOWNLOAD PAGE**.
3. Download the audio from the Free Music Archive source page.
4. Return to the app and click **LOAD DOWNLOADED AUDIO**.
5. Select the downloaded MP3, WAV, FLAC, OGG, or M4A file.
6. Click **PLAY REAL BACKING**.
7. Generate and play a lick over the backing track.

The browser keeps the downloaded file local. The repository does not redistribute the backing recordings.

## Musical lick behavior

The lick engine now uses the selected backing track's:

- Key and mode
- BPM
- Meter
- Genre feel
- Suggested chord movement

Phrases are intentionally spacious rather than filling every subdivision. Each note has:

- A rest before the note
- A duration measured in beats
- A velocity/accent level
- One exact string and fret
- One articulation
- A harmonic role in the phrase

Strong phrase points target the root, third, or fifth. Passing notes connect those targets. Phrase endings favor longer bends or vibrato and resolve toward stable notes.

Different feels produce different pacing:

- Slow blues: long notes, space, bends, vibrato
- Neo-soul/R&B: laid-back timing, legato, extended rests
- Funk: short syncopated phrases
- Swing/boogie: swung note placement
- Soul waltz: longer three-beat phrasing

## Audio architecture

```text
Downloaded MP3/WAV/FLAC/OGG/M4A -> HTML audio backing lane

FreePats SF2 -> SpessaSynth Web Audio worklet -> articulated lead lane
```

The backing and lead lanes are independent. Generating, starting, or stopping a lick does not stop the backing track.

## Main controls

```text
PLAY REAL BACKING / PAUSE REAL BACKING
GENERATE GROOVED LICK
PLAY LICK OVER BAND
PLAY CONTINUOUS IMPROV
STOP LEAD
```

## Fretboard trace

The fretboard displays exactly one active position per performed event. The phrase strip shows:

- Note name
- String and fret
- Articulation
- Rest length in beats
- Note duration in beats

Technique colors distinguish picks, hammer-ons, pull-offs, bends, vibrato, and slides.

## Guitar sound

The displayed instrument is the same generic clean electric-guitar profile used by the FreePats sampled bank. The application does not substitute oscillator beeps for the lead. The SF2 bank is loaded automatically after the first audio interaction.

Sound realism still depends on the sample bank and browser audio implementation. The articulation engine improves phrasing through timing, pitch-wheel bends, slides, vibrato curves, legato transitions, rests, and velocity accents.

## Source layout

```text
src/
├── App.jsx
├── main.jsx
├── styles.css
├── track-library.css
├── audio/
│   └── guitarEngine.js
└── music/
    ├── backingCatalog.js
    └── lickGenerator.js

scripts/
└── prepare-assets.mjs
```

## Licensing

- FreePats clean electric guitar: CC0 1.0.
- SpessaSynth: Apache-2.0.
- Curated backing tracks: linked from Free Music Archive and identified as CC BY 4.0 on their individual source pages. Attribution remains required.
- Guitar photograph: prepared from Wikimedia Commons; confirm the source-page attribution before redistributing a packaged build.
- No commercial guitar libraries or copyrighted backing recordings are bundled in the repository.
