import { WorkletSynthesizer } from 'spessasynth_lib';

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

export class GuitarAudioEngine {
  constructor() {
    this.context = null;
    this.synth = null;
    this.backingSource = null;
    this.backingGain = null;
    this.leadToken = 0;
    this.ready = false;
    this.instrumentId = null;
    this.soundfontBuffers = new Map();
    this.backingBuffers = new Map();
  }

  async initialize() {
    if (this.ready) {
      if (this.context.state === 'suspended') await this.context.resume();
      return;
    }
    this.context = new AudioContext({ latencyHint: 'interactive', sampleRate: 44100 });
    await this.context.audioWorklet.addModule('/spessasynth_processor.min.js');
    this.ready = true;
  }

  async ensureReady() {
    await this.initialize();
    if (this.context.state === 'suspended') await this.context.resume();
  }

  async fetchArrayBuffer(url, cache) {
    if (cache.has(url)) return cache.get(url);
    const response = await fetch(url);
    if (!response.ok) throw new Error(`Unable to load audio asset: ${url}`);
    const promise = response.arrayBuffer();
    cache.set(url, promise);
    return promise;
  }

  async preloadBackingTracks(tracks) {
    await this.ensureReady();
    await Promise.all(tracks.map(async (track) => {
      if (this.backingBuffers.has(track.audioUrl)) return;
      const bytes = await this.fetchArrayBuffer(track.audioUrl, new Map());
      const decoded = await this.context.decodeAudioData(bytes.slice(0));
      this.backingBuffers.set(track.audioUrl, decoded);
    }));
  }

  async loadInstrument(instrument) {
    await this.ensureReady();
    if (this.instrumentId === instrument.id && this.synth) return;
    this.stopLead();
    this.synth?.destroy?.();
    this.synth = new WorkletSynthesizer(this.context);
    this.synth.connect(this.context.destination);
    await this.synth.isReady;
    const soundfont = await this.fetchArrayBuffer(instrument.soundfontUrl, this.soundfontBuffers);
    await this.synth.soundBankManager.addSoundBank(soundfont.slice(0), instrument.id);
    this.synth.programChange?.(0, 0);
    this.synth.setPitchWheelRange?.(0, 2);
    this.instrumentId = instrument.id;
  }

  async playPhrase(phrase, bpm, onStep) {
    await this.ensureReady();
    if (!this.synth) throw new Error('Select a guitar instrument before playing a lick.');
    const token = ++this.leadToken;
    const beatMs = 60000 / bpm;
    this.synth.stopAll?.(0);
    this.synth.pitchWheel?.(0, 8192);

    for (let index = 0; index < phrase.length && token === this.leadToken; index += 1) {
      const event = phrase[index];
      const restMs = beatMs * Math.max(0, event.restBeats ?? 0);
      if (restMs) await sleep(restMs);
      if (token !== this.leadToken) break;
      onStep?.(event, index);
      await this.playArticulatedNote(event, beatMs * (event.durationBeats ?? 0.75), token);
    }

    if (token === this.leadToken) {
      this.synth.stopAll?.(0);
      this.synth.pitchWheel?.(0, 8192);
      onStep?.(null, -1);
    }
  }

  async playArticulatedNote(event, duration, token) {
    const velocity = Math.max(42, Math.min(124, event.velocity ?? 88));
    const noteLength = Math.max(80, duration * (event.length ?? 0.9));
    const nextNote = event.targetMidi ?? event.midi;
    this.synth.noteOn(0, event.midi, velocity);

    if (event.technique === 'hammer-on' || event.technique === 'pull-off') {
      const split = event.technique === 'hammer-on' ? 0.4 : 0.35;
      await sleep(noteLength * split);
      if (token !== this.leadToken) return;
      this.synth.noteOn(0, nextNote, Math.max(38, velocity - (event.technique === 'hammer-on' ? 10 : 16)));
      this.synth.noteOff(0, event.midi);
      await sleep(noteLength * (1 - split));
      this.synth.noteOff(0, nextNote);
      return;
    }

    if (event.technique === 'bend') {
      const frames = 14;
      for (let i = 1; i <= frames; i += 1) {
        if (token !== this.leadToken) return;
        this.synth.pitchWheel?.(0, 8192 + Math.round((8191 * i) / frames));
        await sleep(noteLength * 0.5 / frames);
      }
      await sleep(noteLength * 0.34);
      for (let i = frames; i >= 0; i -= 1) {
        this.synth.pitchWheel?.(0, 8192 + Math.round((8191 * i) / frames));
        await sleep(noteLength * 0.16 / frames);
      }
      this.synth.noteOff(0, event.midi);
      this.synth.pitchWheel?.(0, 8192);
      return;
    }

    if (event.technique === 'vibrato') {
      const pulses = Math.max(7, Math.round(noteLength / 75));
      for (let i = 0; i < pulses; i += 1) {
        if (token !== this.leadToken) return;
        const curve = Math.sin((i / pulses) * Math.PI * 6);
        this.synth.pitchWheel?.(0, 8192 + Math.round(curve * 760));
        await sleep(noteLength / pulses);
      }
      this.synth.noteOff(0, event.midi);
      this.synth.pitchWheel?.(0, 8192);
      return;
    }

    if (event.technique === 'slide') {
      const semitones = Math.max(-4, Math.min(4, nextNote - event.midi));
      const frames = 16;
      for (let i = 1; i <= frames; i += 1) {
        if (token !== this.leadToken) return;
        const bend = 8192 + Math.round((8191 * semitones * i) / (2 * frames));
        this.synth.pitchWheel?.(0, Math.max(0, Math.min(16383, bend)));
        await sleep(noteLength / frames);
      }
      this.synth.noteOff(0, event.midi);
      this.synth.pitchWheel?.(0, 8192);
      return;
    }

    await sleep(noteLength);
    this.synth.noteOff(0, event.midi);
  }

  stopLead() {
    this.leadToken += 1;
    this.synth?.stopAll?.(0);
    this.synth?.pitchWheel?.(0, 8192);
  }

  async startBacking(track) {
    await this.ensureReady();
    this.stopBacking();
    let buffer = this.backingBuffers.get(track.audioUrl);
    if (!buffer) {
      const bytes = await this.fetchArrayBuffer(track.audioUrl, new Map());
      buffer = await this.context.decodeAudioData(bytes.slice(0));
      this.backingBuffers.set(track.audioUrl, buffer);
    }
    this.backingSource = this.context.createBufferSource();
    this.backingGain = this.context.createGain();
    this.backingGain.gain.value = 0.72;
    this.backingSource.buffer = buffer;
    this.backingSource.loop = true;
    this.backingSource.connect(this.backingGain).connect(this.context.destination);
    this.backingSource.start();
  }

  stopBacking() {
    try { this.backingSource?.stop(); } catch { /* already stopped */ }
    this.backingSource?.disconnect();
    this.backingGain?.disconnect();
    this.backingSource = null;
    this.backingGain = null;
  }

  destroy() {
    this.stopLead();
    this.stopBacking();
    this.synth?.destroy?.();
    this.context?.close?.();
  }
}
