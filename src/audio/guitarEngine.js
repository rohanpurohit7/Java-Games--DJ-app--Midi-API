import { WorkletSynthesizer } from 'spessasynth_lib';

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

export class GuitarAudioEngine {
  constructor() {
    this.context = null;
    this.synth = null;
    this.backing = null;
    this.backingTimer = null;
    this.leadToken = 0;
    this.ready = false;
  }

  async initialize() {
    if (this.ready) {
      if (this.context.state === 'suspended') await this.context.resume();
      return;
    }

    this.context = new AudioContext({ latencyHint: 'interactive', sampleRate: 44100 });
    await this.context.audioWorklet.addModule('/spessasynth_processor.min.js');
    this.synth = new WorkletSynthesizer(this.context);
    this.synth.connect(this.context.destination);
    await this.synth.isReady;

    const response = await fetch('/soundfonts/freepats-clean-electric-guitar.sf2');
    if (!response.ok) throw new Error('The bundled FreePats guitar SoundFont could not be loaded.');
    await this.synth.soundBankManager.addSoundBank(await response.arrayBuffer(), 'freepats-guitar');
    this.synth.programChange?.(0, 0);
    this.synth.setPitchWheelRange?.(0, 2);
    this.ready = true;
  }

  async ensureReady() {
    await this.initialize();
    if (this.context.state === 'suspended') await this.context.resume();
  }

  async playPhrase(phrase, bpm, onStep) {
    await this.ensureReady();
    const token = ++this.leadToken;
    const eighth = 60000 / bpm / 2;

    this.synth.stopAll?.(0);
    this.synth.pitchWheel?.(0, 8192);

    for (let index = 0; index < phrase.length && token === this.leadToken; index += 1) {
      const event = phrase[index];
      onStep?.(event, index);
      await this.playArticulatedNote(event, eighth, token);
    }

    if (token === this.leadToken) {
      this.synth.stopAll?.(0);
      this.synth.pitchWheel?.(0, 8192);
      onStep?.(null, -1);
    }
  }

  async playArticulatedNote(event, duration, token) {
    const velocity = Math.max(52, Math.min(124, event.velocity ?? 96));
    const noteLength = duration * (event.length ?? 0.92);
    const nextNote = event.targetMidi ?? event.midi;

    this.synth.noteOn(0, event.midi, velocity);

    if (event.technique === 'hammer-on') {
      await sleep(noteLength * 0.42);
      if (token !== this.leadToken) return;
      this.synth.noteOn(0, nextNote, Math.max(48, velocity - 13));
      this.synth.noteOff(0, event.midi);
      await sleep(noteLength * 0.58);
      this.synth.noteOff(0, nextNote);
      return;
    }

    if (event.technique === 'pull-off') {
      await sleep(noteLength * 0.38);
      if (token !== this.leadToken) return;
      this.synth.noteOn(0, nextNote, Math.max(44, velocity - 18));
      this.synth.noteOff(0, event.midi);
      await sleep(noteLength * 0.62);
      this.synth.noteOff(0, nextNote);
      return;
    }

    if (event.technique === 'bend') {
      const frames = 10;
      for (let i = 1; i <= frames; i += 1) {
        this.synth.pitchWheel?.(0, 8192 + Math.round((8191 * i) / frames));
        await sleep(noteLength * 0.58 / frames);
      }
      await sleep(noteLength * 0.32);
      this.synth.noteOff(0, event.midi);
      this.synth.pitchWheel?.(0, 8192);
      return;
    }

    if (event.technique === 'vibrato') {
      const pulses = 9;
      for (let i = 0; i < pulses; i += 1) {
        this.synth.pitchWheel?.(0, 8192 + (i % 2 === 0 ? 920 : -920));
        await sleep(noteLength / pulses);
      }
      this.synth.noteOff(0, event.midi);
      this.synth.pitchWheel?.(0, 8192);
      return;
    }

    if (event.technique === 'slide') {
      const semitones = Math.max(-4, Math.min(4, nextNote - event.midi));
      const frames = 12;
      for (let i = 1; i <= frames; i += 1) {
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

  async startBacking(bpm = 92, rootMidi = 45, mode = 'minor') {
    await this.ensureReady();
    this.stopBacking();

    const gain = this.context.createGain();
    gain.gain.value = 0.2;
    gain.connect(this.context.destination);
    this.backing = gain;

    const beatMs = 60000 / bpm;
    const progression = mode === 'major' ? [0, 5, 7, 0] : [0, 5, 3, 7];
    let beat = 0;

    const scheduleBeat = () => {
      if (!this.backing) return;
      const chordRoot = rootMidi + progression[Math.floor(beat / 4) % progression.length];
      const intervals = mode === 'major' ? [0, 4, 7, 10] : [0, 3, 7, 10];
      const now = this.context.currentTime;

      if (beat % 4 === 0) {
        intervals.forEach((interval, voice) => {
          const oscillator = this.context.createOscillator();
          const voiceGain = this.context.createGain();
          oscillator.type = voice === 0 ? 'triangle' : 'sine';
          oscillator.frequency.value = 440 * 2 ** ((chordRoot + interval - 69) / 12);
          voiceGain.gain.setValueAtTime(0.0001, now);
          voiceGain.gain.exponentialRampToValueAtTime(0.055, now + 0.04);
          voiceGain.gain.exponentialRampToValueAtTime(0.0001, now + beatMs * 0.0036);
          oscillator.connect(voiceGain).connect(gain);
          oscillator.start(now);
          oscillator.stop(now + beatMs * 0.0038);
        });
      }

      const bass = this.context.createOscillator();
      const bassGain = this.context.createGain();
      bass.type = 'triangle';
      bass.frequency.value = 440 * 2 ** ((chordRoot - 12 - 69) / 12);
      bassGain.gain.setValueAtTime(0.07, now);
      bassGain.gain.exponentialRampToValueAtTime(0.0001, now + beatMs * 0.00075);
      bass.connect(bassGain).connect(gain);
      bass.start(now);
      bass.stop(now + beatMs * 0.0008);

      beat += 1;
      this.backingTimer = window.setTimeout(scheduleBeat, beatMs);
    };

    scheduleBeat();
  }

  stopBacking() {
    if (this.backingTimer) window.clearTimeout(this.backingTimer);
    this.backingTimer = null;
    this.backing?.disconnect();
    this.backing = null;
  }

  destroy() {
    this.stopLead();
    this.stopBacking();
    this.synth?.destroy?.();
    this.context?.close?.();
  }
}
