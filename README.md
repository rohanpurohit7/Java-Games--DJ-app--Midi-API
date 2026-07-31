# Amp Studio Guitar App

A React and Web Audio guitar-improvisation studio with an amplifier-inspired interface, an independent backing lane, articulated one-note-at-a-time lick playback, continuous scale-aware improvisation, and a bundled open FreePats guitar SoundFont.

## Launch the application

### Requirements

- Node.js 22 or newer
- npm 10 or newer
- A current Chrome, Edge, Firefox, or Safari browser
- Internet access during the first asset-preparation run

Check your installed versions:

```bash
node --version
npm --version
```

### First launch on Windows, macOS, or Linux

Clone the repository and enter its directory:

```bash
git clone https://github.com/rohanpurohit7/AmpStudioGuitarApp.git
cd AmpStudioGuitarApp
```

Install dependencies:

```bash
npm install
```

Start the development server:

```bash
npm run dev
```

Open this address in your browser:

```text
http://localhost:5173
```

Vite also prints the exact local URL in the terminal. Keep the terminal window open while using the application. Press `Ctrl+C` in the terminal to stop it.

### Windows PowerShell quick start

```powershell
git clone https://github.com/rohanpurohit7/AmpStudioGuitarApp.git
Set-Location AmpStudioGuitarApp
npm install
npm run dev
```

### Production build and local preview

Create the optimized web build:

```bash
npm run build
```

The deployable application is written to:

```text
dist/
```

Preview the production build locally:

```bash
npm run preview
```

Then open the URL printed by Vite, normally:

```text
http://localhost:4173
```

### First-run audio and image preparation

Before development and production builds, the npm scripts run:

```bash
npm run assets
```

This process:

1. Copies the SpessaSynth browser audio worklet into `public/`.
2. Downloads the CC0 FreePats clean-electric-guitar archive.
3. Extracts the `.sf2` bank into `public/soundfonts/`.
4. Downloads the licensed guitar photograph into `public/assets/`.

Prepared files include:

```text
public/spessasynth_processor.min.js
public/soundfonts/freepats-clean-electric-guitar.sf2
public/assets/studio-electric-guitar.jpg
```

After these files exist, later launches reuse them instead of downloading them again.

### Starting audio in the browser

Browsers block audio until the user interacts with the page. After the interface opens:

1. Click **PLAY BACKING** to initialize Web Audio and load the bundled SoundFont.
2. Click **GENERATE LICK**.
3. Click **PLAY LICK OVER BAND**.
4. Confirm that one string/fret position is highlighted at a time while the backing continues.
5. Use **PLAY CONTINUOUS IMPROV** for ongoing articulated phrases.
6. Use **STOP LEAD** to stop the guitar without stopping the backing.

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
