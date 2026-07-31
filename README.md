# Amp Studio Guitar App

A React/Web Audio guitar-improvisation studio built around real backing-track audio, sampled FreePats electric and nylon guitars, groove-aware phrasing, one-position-at-a-time fretboard tracing, and expressive techniques such as hammer-ons, pull-offs, bends, vibrato, and slides.

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

## Automatic asset preparation

`npm run dev` and `npm run build` run `npm run assets` automatically. The asset step downloads and prepares the open audio used by the application:

```text
public/spessasynth_processor.min.js
public/soundfonts/freepats-electric-clean.sf2
public/soundfonts/freepats-electric-jazz.sf2
public/soundfonts/freepats-nylon-spanish.sf2
public/backing-tracks/*.ogg
public/assets/studio-electric-guitar.jpg
```

Users do not have to visit download pages or manually load files to use the built-in catalog. After the first audio button click, the browser decodes the complete backing catalog into an in-memory `AudioBuffer` cache. Browser autoplay rules prevent audio initialization before a user interaction.

## Built-in real-audio backing library

The dropdown contains locally prepared recordings rather than oscillator or General MIDI accompaniment. Current styles include:

- Soul funk
- Slow piano blues
- Blues practice accompaniment
- Contemporary jazz
- Jazz guitar
- Atmospheric soul groove
- Jazz piano
- Soulful piano

Selecting a track automatically sets its key, mode, BPM, feel, chord suggestions, scale recommendation, and preferred guitar bank. Each track remains linked to its Wikimedia Commons source page and license attribution.

The optional **PERSONAL TRACK** control remains available for MP3, WAV, FLAC, OGG, and M4A files. Personal files stay local to the browser session.

## Sampled guitar library

Three CC0 FreePats SF2 instruments are installed automatically:

- **Clean Electric** — clean amplified electric-guitar samples
- **Jazz Electric** — warmer clean electric samples for jazz and soul
- **Spanish Nylon** — sampled Spanish classical nylon-string guitar

The **GUITAR SOUND** dropdown switches the actual SoundFont used for lead playback. Changing the selected backing track also chooses a suitable default guitar, while manual override remains available.

## Musical lick behavior

The lick engine uses the selected backing track's key, mode, BPM, meter, and feel. Phrases are intentionally spacious rather than filling every subdivision. Each event contains:

- A rest before the note
- A duration measured in beats
- Velocity and accent information
- One exact string and fret
- One articulation
- A harmonic role in the phrase

Strong phrase points target the root, third, or fifth. Passing notes connect those targets. Phrase endings favor longer bends or vibrato and resolve toward stable notes.

Different feels produce different pacing:

- Slow blues: long notes, space, bends, and vibrato
- Neo-soul: laid-back timing, legato, and extended rests
- Funk: short syncopated phrases
- Jazz: warmer phrasing and chord-tone targeting
- Soulful piano: slow nylon or clean-electric melodic responses

## Audio architecture

```text
Bundled OGG backing files
        ↓
Browser decodeAudioData
        ↓
In-memory AudioBuffer catalog
        ↓
Independent looping backing lane

Selected FreePats SF2
        ↓
SpessaSynth Web Audio worklet
        ↓
Independent articulated lead lane
```

Generating, starting, switching, or stopping a lick does not intentionally stop the backing lane. Backing and lead can be stopped independently.

## Main controls

```text
PLAY BACKING / STOP BACKING
GENERATE GROOVED LICK
PLAY LICK OVER BAND
PLAY CONTINUOUS IMPROV
STOP LEAD
BACKING TRACK dropdown
GUITAR SOUND dropdown
OPTIONAL PERSONAL TRACK
```

## Fretboard trace

The fretboard displays exactly one active string/fret position for each performed event. The phrase strip shows:

- Note name
- String and fret
- Articulation
- Rest length in beats
- Note duration in beats

Technique colors distinguish picks, hammer-ons, pull-offs, bends, vibrato, and slides.

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

- FreePats clean electric, jazz electric, and Spanish classical guitar banks: CC0 1.0.
- SpessaSynth: Apache-2.0.
- Built-in backing recordings: Wikimedia Commons files under their listed CC0, CC BY, or CC BY-SA terms. Source and attribution links remain visible in the interface.
- Guitar photograph: prepared from Wikimedia Commons; review its source-page attribution before redistributing a packaged build.
- No commercial guitar libraries are bundled.
