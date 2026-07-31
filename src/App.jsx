import { useEffect, useMemo, useRef, useState } from 'react';
import { GuitarAudioEngine } from './audio/guitarEngine.js';
import { buildFretboard, generateLick, noteName, scalePitchClasses, SCALES } from './music/lickGenerator.js';

const ROOTS = [
  ['C', 48], ['C#', 49], ['D', 50], ['D#', 51], ['E', 52], ['F', 53],
  ['F#', 54], ['G', 55], ['G#', 56], ['A', 57], ['A#', 58], ['B', 59]
];

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
      <button className="knob" style={{ '--rotation': `${rotation}deg` }} type="button" aria-label={label}>
        <i />
      </button>
      <input type="range" min="0" max="100" value={value} onChange={(event) => onChange(Number(event.target.value))} />
      <b>{value}</b>
    </label>
  );
}

function App() {
  const engineRef = useRef(null);
  const continuousRef = useRef(false);
  const [rootMidi, setRootMidi] = useState(57);
  const [scaleName, setScaleName] = useState('Blues');
  const [bpm, setBpm] = useState(92);
  const [phrase, setPhrase] = useState(() => generateLick());
  const [active, setActive] = useState(null);
  const [status, setStatus] = useState('Click PLAY BACKING to initialize the FreePats guitar engine.');
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

  const makePhrase = () => {
    const next = generateLick({ rootMidi, scaleName, length: 12 });
    setPhrase(next);
    setActive(null);
    setStatus('New one-position-at-a-time articulated lick generated.');
    return next;
  };

  const playPhrase = async (selectedPhrase = phrase) => {
    try {
      setStatus('Playing articulated FreePats guitar over the backing lane...');
      await engineRef.current.playPhrase(selectedPhrase, bpm, (event) => setActive(event));
      if (!continuousRef.current) setStatus('Lick complete. The backing lane remains independent.');
    } catch (error) {
      setStatus(error.message);
    }
  };

  const toggleBacking = async () => {
    try {
      if (backing) {
        engineRef.current.stopBacking();
        setBacking(false);
        setStatus('Backing stopped.');
      } else {
        await engineRef.current.startBacking(bpm, rootMidi - 12, scaleName.includes('Major') || scaleName === 'Mixolydian' ? 'major' : 'minor');
        setBacking(true);
        setStatus('Backing playing on its own audio lane.');
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
    if (!backing) await toggleBacking();
    while (continuousRef.current) {
      const next = generateLick({ rootMidi, scaleName, length: 9 + Math.floor(Math.random() * 5) });
      setPhrase(next);
      await playPhrase(next);
      await new Promise((resolve) => setTimeout(resolve, Math.max(110, 60000 / bpm / 3)));
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
          <span className="eyebrow">WEB AUDIO • SF2 • REACT</span>
          <h1>AMP STUDIO <em>GUITAR</em></h1>
          <p>Scale-aware improvisation with a continuous backing lane and one fret position highlighted at a time.</p>
        </div>
        <div className={`status-led ${backing ? 'live' : ''}`}><i />{status}</div>
      </header>

      <section className="studio-grid">
        <article className="panel fret-panel">
          <div className="panel-heading">
            <div><span>PERFORMANCE VIEW</span><h2>Articulated Lick Trace</h2></div>
            <div className="selectors">
              <label>KEY<select value={rootMidi} onChange={(event) => setRootMidi(Number(event.target.value))}>{ROOTS.map(([name, midi]) => <option key={name} value={midi}>{name}</option>)}</select></label>
              <label>SCALE<select value={scaleName} onChange={(event) => setScaleName(event.target.value)}>{Object.keys(SCALES).map((name) => <option key={name}>{name}</option>)}</select></label>
              <label>BPM<input type="number" min="55" max="180" value={bpm} onChange={(event) => setBpm(Number(event.target.value))} /></label>
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
            {phrase.map((event, index) => <div key={`${event.id}-${index}`} className={active === event ? 'now' : ''}><b>{noteName(event.midi)}</b><span>S{event.stringNumber} F{event.fret}</span><em>{TECH_LABELS[event.technique]}</em></div>)}
          </div>
        </article>

        <aside className="panel amp-panel">
          <div className="guitar-image">
            {imageLoaded && <img src="/assets/studio-electric-guitar.jpg" alt="Studio-lit electric guitar" onError={() => setImageLoaded(false)} />}
            {!imageLoaded && <div className="image-fallback"><strong>STUDIO ELECTRIC</strong><span>Asset will be installed by npm run assets</span></div>}
            <div className="image-vignette" />
            <div className="image-caption"><span>SELECTED INSTRUMENT</span><b>FreePats Clean Electric</b><small>CC0 sampled guitar • bridge pickup</small></div>
          </div>

          <div className="amp-face">
            <div className="amp-brand">NIGHT<span>DRIVE</span></div>
            <div className="knob-grid">
              {Object.entries(tone).map(([name, value]) => <Knob key={name} label={name.toUpperCase()} value={value} onChange={(next) => setTone((current) => ({ ...current, [name]: next }))} />)}
            </div>
            <div className="speaker-grille"><div className="speaker" /><span>FREEPATS SF2 SIGNAL CHAIN</span></div>
          </div>
        </aside>
      </section>

      <nav className="transport">
        <button onClick={toggleBacking} className={backing ? 'active' : ''}>{backing ? 'STOP BACKING' : 'PLAY BACKING'}</button>
        <button onClick={makePhrase}>GENERATE LICK</button>
        <button onClick={() => playPhrase()}>PLAY LICK OVER BAND</button>
        <button onClick={startContinuous} className={continuous ? 'danger' : ''}>{continuous ? 'STOP CONTINUOUS' : 'PLAY CONTINUOUS IMPROV'}</button>
        <button onClick={stopLead}>STOP LEAD</button>
      </nav>
    </main>
  );
}

export default App;
