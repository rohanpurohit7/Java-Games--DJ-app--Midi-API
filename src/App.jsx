import { useEffect, useMemo, useRef, useState } from 'react';
import { GuitarAudioEngine } from './audio/guitarEngine.js';
import { BACKING_TRACKS, GUITAR_INSTRUMENTS, suggestedScale } from './music/backingCatalog.js';
import { buildFretboard, generateLick, noteName, scalePitchClasses, SCALES } from './music/lickGenerator.js';

const ROOTS = [['C',48],['C#',49],['D',50],['D#',51],['E',52],['F',53],['F#',54],['G',55],['G#',56],['A',57],['A#',58],['B',59]];
const ROOT_MIDI = Object.fromEntries(ROOTS);
const TECH_LABELS = { pick:'PICK','hammer-on':'HAMMER-ON','pull-off':'PULL-OFF',bend:'BEND',vibrato:'VIBRATO',slide:'SLIDE' };

function Knob({ label, value, onChange }) {
  const rotation = -135 + (value / 100) * 270;
  return <label className="knob-wrap"><span>{label}</span><button className="knob" style={{'--rotation':`${rotation}deg`}} type="button"><i /></button><input type="range" min="0" max="100" value={value} onChange={(e)=>onChange(Number(e.target.value))}/><b>{value}</b></label>;
}

export default function App() {
  const engineRef = useRef(null);
  const continuousRef = useRef(false);
  const preloadedRef = useRef(false);
  const customUrlRef = useRef(null);
  const [trackId,setTrackId] = useState(BACKING_TRACKS[0].id);
  const baseTrack = useMemo(()=>BACKING_TRACKS.find(t=>t.id===trackId) ?? BACKING_TRACKS[0],[trackId]);
  const [customTrack,setCustomTrack] = useState(null);
  const track = customTrack ?? baseTrack;
  const [instrumentId,setInstrumentId] = useState(track.guitar);
  const instrument = useMemo(()=>GUITAR_INSTRUMENTS.find(g=>g.id===instrumentId) ?? GUITAR_INSTRUMENTS[0],[instrumentId]);
  const [rootMidi,setRootMidi] = useState(ROOT_MIDI[track.key] ?? 57);
  const [scaleName,setScaleName] = useState(suggestedScale(track));
  const [bpm,setBpm] = useState(track.bpm);
  const [phrase,setPhrase] = useState(()=>generateLick({rootMidi:57,scaleName:'Dorian',feel:'neo-soul'}));
  const [active,setActive] = useState(null);
  const [status,setStatus] = useState('Tracks and guitar banks are installed locally. Click PLAY BACKING to load them into audio memory.');
  const [backing,setBacking] = useState(false);
  const [continuous,setContinuous] = useState(false);
  const [loading,setLoading] = useState(false);
  const [tone,setTone] = useState({gain:42,presence:68,warmth:61,sustain:72,reverb:34,vibrato:48});

  const fretboard = useMemo(()=>buildFretboard(15),[]);
  const scaleNotes = useMemo(()=>scalePitchClasses(rootMidi,scaleName),[rootMidi,scaleName]);
  const strings = useMemo(()=>Array.from({length:6},(_,i)=>fretboard.filter(n=>n.stringIndex===5-i)),[fretboard]);

  useEffect(()=>{ engineRef.current = new GuitarAudioEngine(); return ()=>{ if(customUrlRef.current) URL.revokeObjectURL(customUrlRef.current); engineRef.current?.destroy(); }; },[]);

  useEffect(()=>{
    if(customTrack) return;
    const nextRoot=ROOT_MIDI[baseTrack.key] ?? 57;
    const nextScale=suggestedScale(baseTrack);
    setRootMidi(nextRoot); setScaleName(nextScale); setBpm(baseTrack.bpm); setInstrumentId(baseTrack.guitar);
    setPhrase(generateLick({rootMidi:nextRoot,scaleName:nextScale,feel:baseTrack.feel,mode:baseTrack.mode,length:8}));
    setActive(null);
    if(backing){ engineRef.current.stopBacking(); setBacking(false); }
    setStatus(`${baseTrack.title} ready from the built-in library — ${baseTrack.key} ${baseTrack.mode}, ${baseTrack.bpm} BPM.`);
  },[baseTrack]);

  async function prepareAudio() {
    setLoading(true);
    try {
      await engineRef.current.loadInstrument(instrument);
      if(!preloadedRef.current){
        setStatus('Loading the complete backing catalog into audio memory...');
        await engineRef.current.preloadBackingTracks(BACKING_TRACKS);
        preloadedRef.current=true;
      }
    } finally { setLoading(false); }
  }

  useEffect(()=>{
    if(!engineRef.current?.ready) return;
    engineRef.current.loadInstrument(instrument).then(()=>setStatus(`${instrument.name} sampled guitar loaded.`)).catch(e=>setStatus(e.message));
  },[instrument]);

  const makePhrase=()=>{
    const next=generateLick({rootMidi,scaleName,feel:track.feel,mode:track.mode,length:8});
    setPhrase(next); setActive(null); setStatus(`Generated a spacious ${track.feel} phrase at ${bpm} BPM.`); return next;
  };

  const playPhrase=async(selected=phrase)=>{
    try { await prepareAudio(); setStatus(`Playing ${instrument.name} over ${track.title}...`); await engineRef.current.playPhrase(selected,bpm,e=>setActive(e)); if(!continuousRef.current)setStatus('Phrase complete. Backing continues.'); }
    catch(e){ setStatus(e.message); }
  };

  const toggleBacking=async()=>{
    try{
      if(backing){ engineRef.current.stopBacking(); setBacking(false); setStatus('Backing stopped.'); return; }
      await prepareAudio(); await engineRef.current.startBacking(track); setBacking(true); setStatus(`${track.title} is playing from the in-memory backing library.`);
    }catch(e){setStatus(e.message);}
  };

  const startContinuous=async()=>{
    const enabled=!continuous; continuousRef.current=enabled; setContinuous(enabled);
    if(!enabled){engineRef.current.stopLead();setStatus('Continuous improv stopped; backing continues.');return;}
    if(!backing){await prepareAudio();await engineRef.current.startBacking(track);setBacking(true);}
    while(continuousRef.current){const next=generateLick({rootMidi,scaleName,feel:track.feel,mode:track.mode,length:6+Math.floor(Math.random()*4)});setPhrase(next);await playPhrase(next);await new Promise(r=>setTimeout(r,60000/bpm*.75));}
  };

  const loadPersonalTrack=(file)=>{
    if(!file)return;
    if(customUrlRef.current)URL.revokeObjectURL(customUrlRef.current);
    customUrlRef.current=URL.createObjectURL(file);
    setCustomTrack({...baseTrack,id:'custom',title:file.name,audioUrl:customUrlRef.current});
    setStatus(`${file.name} loaded as an optional personal backing track.`);
    engineRef.current.stopBacking(); setBacking(false);
  };

  return <main className="app-shell">
    <header className="topbar"><div><span className="eyebrow">IN-MEMORY REAL AUDIO • MULTI-SF2 GUITARS • GROOVE-AWARE IMPROV</span><h1>AMP STUDIO <em>GUITAR</em></h1><p>Choose a bundled real recording and a sampled electric or nylon guitar. No file download is required for the built-in library.</p></div><div className={`status-led ${backing?'live':''}`}><i/>{loading?'Loading audio assets...':status}</div></header>

    <section className="track-browser panel">
      <div className="track-copy"><span className="eyebrow">BUILT-IN BACKING LIBRARY</span><h2>{track.title}</h2><p>{track.artist} · {track.genre} · {track.key} {track.mode} · {bpm} BPM · {track.meter}</p><div className="chord-row">{track.chords.map((c,i)=><b key={`${c}-${i}`}>{c}</b>)}</div></div>
      <div className="track-actions">
        <label>BACKING TRACK<select value={customTrack?'custom':trackId} onChange={(e)=>{setCustomTrack(null);setTrackId(e.target.value)}}>{BACKING_TRACKS.map(t=><option key={t.id} value={t.id}>{t.title} — {t.genre} — {t.bpm} BPM</option>)}{customTrack&&<option value="custom">{customTrack.title}</option>}</select></label>
        <label>GUITAR SOUND<select value={instrumentId} onChange={(e)=>setInstrumentId(e.target.value)}>{GUITAR_INSTRUMENTS.map(g=><option key={g.id} value={g.id}>{g.name} — {g.family}</option>)}</select></label>
        <label className="file-button">OPTIONAL PERSONAL TRACK<input type="file" accept="audio/*,.wav,.mp3,.flac,.ogg,.m4a" onChange={(e)=>loadPersonalTrack(e.target.files?.[0])}/></label>
        <small>{track.license} · <a href={track.source} target="_blank" rel="noreferrer">source and attribution</a></small>
      </div>
    </section>

    <section className="studio-grid">
      <article className="panel fret-panel">
        <div className="panel-heading"><div><span>PERFORMANCE VIEW</span><h2>Articulated Lick Trace</h2></div><div className="selectors"><label>KEY<select value={rootMidi} onChange={e=>setRootMidi(Number(e.target.value))}>{ROOTS.map(([n,m])=><option key={n} value={m}>{n}</option>)}</select></label><label>SCALE<select value={scaleName} onChange={e=>setScaleName(e.target.value)}>{Object.keys(SCALES).map(n=><option key={n}>{n}</option>)}</select></label><label>BPM<input type="number" min="40" max="220" value={bpm} onChange={e=>setBpm(Number(e.target.value))}/></label></div></div>
        <div className="fretboard-scroll"><div className="fretboard"><div className="fret-numbers"><span/>{Array.from({length:16},(_,f)=><span key={f}>{f}</span>)}</div>{strings.map((notes,si)=><div className="string-row" key={si}><b>{6-si}</b>{notes.map(p=>{const inScale=scaleNotes.has(p.midi%12),isActive=active?.id===p.id,isRoot=p.midi%12===rootMidi%12;return <span key={p.id} className={`${inScale?'scale':''} ${isRoot?'root':''} ${isActive?`active ${active.technique}`:''}`}>{inScale?p.name.replace(/\d/g,''):''}{isActive&&<small>{TECH_LABELS[active.technique]}</small>}</span>})}</div>)}</div></div>
        <div className="phrase-strip">{phrase.map((e,i)=><div key={`${e.id}-${i}`} className={active===e?'now':''}><b>{noteName(e.midi)}</b><span>S{e.stringNumber} F{e.fret}</span><em>{TECH_LABELS[e.technique]}</em><small>{e.waitBeats.toFixed(2)} rest · {e.durationBeats.toFixed(2)} beats</small></div>)}</div>
      </article>

      <aside className="panel amp-panel"><div className="guitar-image"><img src={instrument.imageUrl} alt={`${instrument.name} guitar`}/><div className="image-vignette"/><div className="image-caption"><span>SELECTED INSTRUMENT</span><b>{instrument.name}</b><small>{instrument.description}</small></div></div><div className="amp-face"><div className="amp-brand">NIGHT<span>DRIVE</span></div><div className="knob-grid">{Object.entries(tone).map(([n,v])=><Knob key={n} label={n.toUpperCase()} value={v} onChange={x=>setTone(t=>({...t,[n]:x}))}/>)}</div><div className="speaker-grille"><div className="speaker"/><span>{instrument.family.toUpperCase()} SAMPLED SIGNAL CHAIN</span></div></div></aside>
    </section>

    <nav className="transport"><button disabled={loading} onClick={toggleBacking} className={backing?'active':''}>{backing?'STOP BACKING':'PLAY BACKING'}</button><button onClick={makePhrase}>GENERATE GROOVED LICK</button><button disabled={loading} onClick={()=>playPhrase()}>PLAY LICK OVER BAND</button><button disabled={loading} onClick={startContinuous} className={continuous?'danger':''}>{continuous?'STOP CONTINUOUS':'PLAY CONTINUOUS IMPROV'}</button><button onClick={()=>{continuousRef.current=false;setContinuous(false);engineRef.current.stopLead();setActive(null);setStatus('Lead stopped. Backing continues.')}}>STOP LEAD</button></nav>
  </main>;
}
