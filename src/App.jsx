import { useEffect, useMemo, useRef, useState } from 'react';
import { GuitarAudioEngine } from './audio/guitarEngine.js';
import { BACKING_TRACKS, suggestedScale } from './music/backingCatalog.js';
import { buildFretboard, generateLick, noteName, scalePitchClasses, SCALES } from './music/lickGenerator.js';

const ROOTS = [
  ['C', 48], ['C#', 49], ['D', 50], ['D#', 51], ['E', 52], ['F', 53],
  ['F#', 54], ['G', 55], ['G#', 56], ['A', 57], ['A#', 58], ['B', 59]
];
const ROOT_MIDI = Object.fromEntries(ROOTS);

const TECH_LABELS = {
  pick: 'PICK',
  'hammer-on': 'HAMMER-ON',
  'pull-off': 'PULL-OFF',
  bend: 'BEND',
  vibrato: 'VIBRATO',
  slide: 'SLIDE'
};

function Knob({ label, value, onChange }) {
  const rotation = -135 + (value / 100) * 270;
  return (
    <label className="knob-wrap">
      <span>{label}</span>
      <button className="knob" style={{ '--rotation': `${rotation}deg` }} type="button" aria-label={label}><i /></button>
      <input type="range" min="0" max="100" value={value} onChange={(event) => onChange(Number(event.target.value))} />
      <b>{value}</b>
    </label>
  );
}

function App() {
  const engineRef = useRef(null);
  const continuousRef = useRef(false);
  const [trackId, setTrackId] = useState(BACKING_TRACKS[0].id);
  const track = useMemo(() => BACKING_TRACKS.find((item) => item.id === trackId) ?? BACKING_TRACKS[0], [trackId]);
  const [rootMidi, setRootMidi] = useState(ROOT_MIDI[track.key] ?? 57);
  const [scaleName, setScaleName] = useState(suggestedScale(track));
  const [bpm, setBpm] = useState(track.bpm);
  const [audioFileName, setAudioFileName] = useState('No real backing audio selected');
  const [phrase, setPhrase] = useState(() => generateLick({ rootMidi: 52, scaleName: 'Blues', bpm: 58, feel: 'slow-blues' }));
  const [active, setActive] = useState(null);
  const [status, setStatus] = useState('Choose a curated track, download it from the source page, then load the audio file.');
  const [backing, setBacking] = useState(false);
  const [continuous, setContinuous] = useState(false);
  const [imageLoaded, setImageLoaded] = useState(true);
  const [tone, setTone] = useState({ gain: 42, presence: 68, warmth: 61, sustain: 72, reverb: 34, vibrato: 48 });

  const fretboard = useMemo(() => buildFretboard(15), []);
  const scaleNotes = useMemo(() => scalePitchClasses(rootMidi, scaleName), [rootMidi, scaleName]);
  const strings = useMemo(() => Array.from({ length: 6 }, (_, index) => fretboard.filter((note) => note.stringIndex === 5 - index)), [fretboard]);

  useEffect(() => {
    engineRef.current = new GuitarAudioEngine();
    return () => engineRef.current?.destroy();
  }, []);

  useEffect(() => {
    const nextRoot = ROOT_MIDI[track.key] ?? 57;
    const nextScale = suggestedScale(track);
    setRootMidi(nextRoot);
    setScaleName(nextScale);
    setBpm(track.bpm);
    setPhrase(generateLick({ rootMidi: nextRoot, scaleName: nextScale, feel: track.feel, mode: track.mode, length: 8 }));
    setActive(null);
    setBacking(false);
    engineRef.current?.stopBacking();
    setAudioFileName('No real backing audio selected');
    setStatus(`Selected ${track.title}: ${track.key} ${track.mode}, ${track.bpm} BPM, ${track.feel}. Download and load its audio.`);
  }, [track]);

  const makePhrase = () => {
    const next = generateLick({ rootMidi, scaleName, feel: track.feel, mode: track.mode, length: 8 });
    setPhrase(next);
    setActive(null);
    setStatus(`Generated a spacious ${track.feel} phrase that resolves in ${track.key} ${track.mode}.`);
    return next;
  };

  const playPhrase = async (selectedPhrase = phrase) => {
    try {
      setStatus(`Playing a paced ${track.feel} guitar phrase at ${bpm} BPM over the real backing lane...`);
      await engineRef.current.playPhrase(selectedPhrase, bpm, (event) => setActive(event));
      if (!continuousRef.current) setStatus('Phrase complete. The backing track continues independently.');
    } catch (error) {
      setStatus(error.message);
    }
  };

  const loadBacking = (file) => {
    if (!file) return;
    engineRef.current.loadBackingFile(file);
    engineRef.current.setBackingVolume(.82);
    setAudioFileName(file.name);
    setStatus(`Loaded ${file.name}. Track metadata is set to ${track.key} ${track.mode}, ${track.bpm} BPM.`);
  };

  const toggleBacking = async () => {
    try {
      if (backing) {
        engineRef.current.pauseBacking();
        setBacking(false);
        setStatus('Backing paused at its current position.');
      } else {
        await engineRef.current.startBacking();
        setBacking(true);
        setStatus(`Playing real audio: ${audioFileName}. Generated licks use its BPM, feel and key.`);
      }
    } catch (error) {
      setStatus(error.message);
    }
  };

  const startContinuous = async () => {
    const enabled = !continuous;
    continuousRef.current = enabled;
    setContinuous(enabled);
    if (!enabled) {
      engineRef.current.stopLead();
      setStatus('Continuous improvisation stopped; backing is unchanged.');
      return;
    }
    if (!backing) {
      try { await engineRef.current.startBacking(); setBacking(true); }
      catch (error) { setContinuous(false); continuousRef.current = false; setStatus(error.message); return; }
    }
    while (continuousRef.current) {
      const next = generateLick({ rootMidi, scaleName, feel: track.feel, mode: track.mode, length: 6 + Math.floor(Math.random() * 4) });
      setPhrase(next);
      await playPhrase(next);
      await new Promise((resolve) => setTimeout(resolve, Math.max(180, 60000 / bpm * .75)));
    }
  };

  const stopLead = () => {
    continuousRef.current = false;
    setContinuous(false);
    engineRef.current.stopLead();
    setActive(null);
    setStatus('Lead stopped. Backing continues.');
  };

  return (
    <main className="app-shell">
      <header className="topbar">
        <div>
          <span className="eyebrow">REAL AUDIO • SAMPLED GUITAR • GROOVE-AWARE PHRASES</span>
          <h1>AMP STUDIO <em>GUITAR</em></h1>
          <p>Download a real Creative Commons backing track, load it locally, and improvise with slow, articulated phrases shaped to its key, BPM and feel.</p>
        </div>
        <div className={`status-led ${backing ? 'live' : ''}`}><i />{status}</div>
      </header>

      <section className="track-browser panel">
        <div className="track-copy">
          <span className="eyebrow">CURATED REAL-AUDIO LIBRARY</span>
          <h2>{track.title}</h2>
          <p>{track.artist} · {track.genre} · {track.key} {track.mode} · {track.bpm} BPM · {track.meter}</p>
          <div className="chord-row">{track.chords.map((chord) => <b key={chord}>{chord}</b>)}</div>
        </div>
        <div className="track-actions">
          <label>BACKING TRACK
            <select value={trackId} onChange={(event) => setTrackId(event.target.value)}>
              {BACKING_TRACKS.map((item) => <option key={item.id} value={item.id}>{item.title} — {item.genre} — {item.key} — {item.bpm} BPM</option>)}
            </select>
          </label>
          <a className="download-link" href={track.source} target="_blank" rel="noreferrer">OPEN DOWNLOAD PAGE</a>
          <label className="file-button">LOAD DOWNLOADED AUDIO
            <input type="file" accept="audio/*,.wav,.mp3,.flac,.ogg,.m4a" onChange={(event) => loadBacking(event.target.files?.[0])} />
          </label>
          <small>{audioFileName} · {track.license}</small>
        </div>
      </section>

      <section className="studio-grid">
        <article className="panel fret-panel">
          <div className="panel-heading">
            <div><span>PERFORMANCE VIEW</span><h2>Articulated Lick Trace</h2></div>
            <div className="selectors">
              <label>KEY<select value={rootMidi} onChange={(event) => setRootMidi(Number(event.target.value))}>{ROOTS.map(([name, midi]) => <option key={name} value={midi}>{name}</option>)}</select></label>
              <label>SCALE<select value={scaleName} onChange={(event) => setScaleName(event.target.value)}>{Object.keys(SCALES).map((name) => <option key={name}>{name}</option>)}</select></label>
              <label>BPM<input type="number" min="40" max="220" value={bpm} onChange={(event) => setBpm(Number(event.target.value))} /></label>
            </div>
          </div>

          <div className="fretboard-scroll">
            <div className="fretboard">
              <div className="fret-numbers"><span />{Array.from({ length: 16 }, (_, fret) => <span key={fret}>{fret}</span>)}</div>
              {strings.map((stringNotes, stringIndex) => (
                <div className="string-row" key={stringIndex}>
                  <b>{6 - stringIndex}</b>
                  {stringNotes.map((position) => {
                    const inScale = scaleNotes.has(position.midi % 12);
                    const isActive = active?.id === position.id;
                    const isRoot = position.midi % 12 === rootMidi % 12;
                    return <span key={position.id} className={`${inScale ? 'scale' : ''} ${isRoot ? 'root' : ''} ${isActive ? `active ${active.technique}` : ''}`} title={`${position.name} • string ${position.stringNumber}, fret ${position.fret}`}>{inScale ? position.name.replace(/\d/g, '') : ''}{isActive && <small>{TECH_LABELS[active.technique]}</small>}</span>;
                  })}
                </div>
              ))}
            </div>
          </div>

          <div className="phrase-strip">
            {phrase.map((event, index) => <div key={`${event.id}-${index}`} className={active === event ? 'now' : ''}><b>{noteName(event.midi)}</b><span>S{event.stringNumber} F{event.fret}</span><em>{TECH_LABELS[event.technique]}</em><small>{event.waitBeats.toFixed(2)} beat rest · {event.durationBeats.toFixed(2)} beat note</small></div>)}
          </div>
        </article>

        <aside className="panel amp-panel">
          <div className="guitar-image">
            {imageLoaded && <img src="/assets/studio-electric-guitar.jpg" alt="Studio-lit sampled electric guitar" onError={() => setImageLoaded(false)} />}
            {!imageLoaded && <div className="image-fallback"><strong>FREEPATS CLEAN ELECTRIC</strong><span>Run npm run assets to install the guitar image and sampled instrument.</span></div>}
            <div className="image-vignette" />
            <div className="image-caption"><span>SELECTED INSTRUMENT</span><b>FreePats Clean Electric</b><small>CC0 multisampled guitar · expressive lead</small></div>
          </div>

          <div className="amp-face">
            <div className="amp-brand">NIGHT<span>DRIVE</span></div>
            <div className="knob-grid">
              {Object.entries(tone).map(([name, value]) => <Knob key={name} label={name.toUpperCase()} value={value} onChange={(next) => setTone((current) => ({ ...current, [name]: next }))} />)}
            </div>
            <div className="speaker-grille"><div className="speaker" /><span>FREEPATS SAMPLED GUITAR SIGNAL CHAIN</span></div>
          </div>
        </aside>
      </section>

      <nav className="transport">
        <button onClick={toggleBacking} className={backing ? 'active' : ''}>{backing ? 'PAUSE REAL BACKING' : 'PLAY REAL BACKING'}</button>
        <button onClick={makePhrase}>GENERATE GROOVED LICK</button>
        <button onClick={() => playPhrase()}>PLAY LICK OVER BAND</button>
        <button onClick={startContinuous} className={continuous ? 'danger' : ''}>{continuous ? 'STOP CONTINUOUS' : 'PLAY CONTINUOUS IMPROV'}</button>
        <button onClick={stopLead}>STOP LEAD</button>
      </nav>
    </main>
  );
}

export default App;
