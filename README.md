# Amp Studio Guitar App

A React and Web Audio guitar-improvisation studio with an amplifier-inspired interface, an independent backing lane, articulated one-note-at-a-time lick playback, continuous scale-aware improvisation, and a bundled open FreePats guitar SoundFont.

## Run the web app

Requirements: Node.js 22 or newer.

```bash
npm install
npm run dev
```

Create a production build with:

```bash
npm run build
npm run preview
```

`npm install`/`npm run build` prepares the browser audio worklet, downloads the small CC0 FreePats clean-electric-guitar SF2 archive, extracts the SoundFont into `public/soundfonts`, and downloads the licensed guitar photograph used by the interface.

## Core experience

1. Select a key and scale.
2. Start the backing lane.
3. Generate an articulated lick.
4. Play the lick over the backing without replacing the accompaniment.
5. Watch one exact string/fret location at a time on the fretboard.
6. Start continuous improvisation for an ongoing stream of scale-aware phrases.
7. Adjust the steel amp controls and stop the lead independently of the backing.

## Articulated guitar playback

The lead engine plays one event at a time through the FreePats sampled guitar bank. Every event contains one selected fretboard position and one technique:

- Pick
- Hammer-on
- Pull-off
- Bend
- Vibrato
- Slide

Hammer-ons and pull-offs transition into a target note. Bends, vibrato, and slides use pitch-wheel movement rather than triggering the same note across multiple fret positions.

## Browser audio architecture

```text
FreePats SF2 -> SpessaSynth Web Audio worklet -> articulated lead lane

Generated backing -> independent Web Audio backing lane
```

Starting or regenerating a lead phrase does not intentionally stop the backing lane. The user can stop the lead and backing separately.

## Fretboard behavior

The browser fretboard displays:

- Six guitar strings
- Frets 0–15
- Notes in the selected scale
- Root-note highlighting
- Exactly one active string/fret position for the current event
- Technique-specific colors and labels
- A phrase strip showing note, string, fret, and technique

## Visual design

The active interface uses:

- React and responsive CSS
- Dark blue and black studio surfaces
- Steel amplifier styling
- Modern `Inter` and `Rajdhani` typography
- Realistic rotary amp controls
- Speaker-grille detailing
- A locally prepared, studio-lit guitar photograph with a dark vignette
- Responsive desktop, tablet, and mobile layouts

## Open guitar library

The build uses the small FreePats **FSBS Electric Guitar Clean #1** SF2 bank. FreePats describes it as direct sampling of an electric guitar processed through an amplifier/effects rack and publishes it under CC0 1.0.

Prepared asset path:

```text
public/soundfonts/freepats-clean-electric-guitar.sf2
```

The browser synthesizer loads this asset automatically after the user first interacts with an audio control, as required by browser autoplay rules.

## Active source layout

```text
src/
├── App.jsx
├── main.jsx
├── styles.css
├── audio/
│   └── guitarEngine.js
└── music/
    └── lickGenerator.js

scripts/
└── prepare-assets.mjs
```

## Main controls

```text
PLAY BACKING
GENERATE LICK
PLAY LICK OVER BAND
PLAY CONTINUOUS IMPROV
STOP LEAD
```

## Current scope

Development is focused on the core web experience:

- Reliable simultaneous backing and lead playback
- Authentic SF2 guitar tone
- One-position-at-a-time fretboard tracing
- Humanized phrase timing
- Hammer-ons, pull-offs, bends, vibrato, and slides
- Continuous scale-aware improvisation
- Sleek responsive amplifier UX

The former JavaFX/DJ launchers are obsolete and are no longer the documented application path.

## Licensing

- FreePats clean electric guitar bank: CC0 1.0, sourced during asset preparation.
- SpessaSynth libraries: Apache-2.0.
- The guitar photograph is downloaded from Wikimedia Commons during asset preparation; review the source-page attribution before public redistribution of a packaged build.
- No commercial guitar libraries or copyrighted backing recordings are bundled.
