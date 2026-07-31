import { WorkletSynthesizer } from 'spessasynth_lib';

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

export class GuitarAudioEngine {
  constructor() {
    this.context = null;
    this.synth = null;
    this.backingAudio = new Audio();
    this.backingAudio.loop = true;
    this.backingAudio.preload = 'auto';
    this.backingObjectUrl = null;
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
    if (!response.ok) throw new Error('The FreePats sampled guitar could not be loaded. Run npm run assets.');
    await this.synth.soundBankManager.addSoundBank(await response.arrayBuffer(), 'freepats-guitar');
    this.synth.programChange?.(0, 0);
    this.synth.setPitchWheelRange?.(0, 2);
    this.synth.controllerChange?.(0, 91, 38);
    this.synth.controllerChange?.(0, 93, 12);
    this.ready = true;
  }

  async ensureReady() {
    await this.initialize();
    if (this.context.state === 'suspended') await this.context.resume();
  }

  loadBackingFile(file) {
    if (!file) return;
    if (this.backingObjectUrl) URL.revokeObjectURL(this.backingObjectUrl);
    this.backingObjectUrl = URL.createObjectURL(file);
    this.backingAudio.src = this.backingObjectUrl;
    this.backingAudio.load();
  }

  hasBackingFile() {
    return Boolean(this.backingAudio.src);
  }

  async startBacking() {
    if (!this.hasBackingFile()) throw new Error('Download a real backing track, then choose the audio file in the app.');
    this.backingAudio.currentTime = this.backingAudio.ended ? 0 : this.backingAudio.currentTime;
    await this.backingAudio.play();
  }

  pauseBacking() {
    this.backingAudio.pause();
  }

  stopBacking() {
    this.backingAudio.pause();
    this.backingAudio.currentTime = 0;
  }

  backingTime() {
    return this.backingAudio.currentTime || 0;
  }

  setBackingVolume(value) {
    this.backingAudio.volume = Math.max(0, Math.min(1, value));
  }

  async playPhrase(phrase, bpm, onStep) {
    await this.ensureReady();
    const token = ++this.leadToken;
    const beatMs = 60000 / Math.max(40, bpm);

    this.synth.stopAll?.(0);
    this.synth.pitchWheel?.(0, 8192);

    for (let index = 0; index < phrase.length && token === this.leadToken; index += 1) {
      const event = phrase[index];
      if (event.waitBeats > 0) await sleep(event.waitBeats * beatMs);
      if (token !== this.leadToken) break;
      onStep?.(event, index);
      await this.playArticulatedNote(event, event.durationBeats * beatMs, token);
    }

    if (token === this.leadToken) {
      this.synth.stopAll?.(0);
      this.synth.pitchWheel?.(0, 8192);
      onStep?.(null, -1);
    }
  }

  async playArticulatedNote(event, duration, token) {
    const velocity = Math.max(48, Math.min(120, event.velocity ?? 92));
    const noteLength = Math.max(90, duration);
    const target = event.targetMidi ?? event.midi;

    this.synth.noteOn(0, event.midi, velocity);

    if (event.technique === 'hammer-on' || event.technique === 'pull-off') {
      const split = event.technique === 'hammer-on' ? .44 : .38;
      await sleep(noteLength * split);
      if (token !== this.leadToken) return;
      this.synth.noteOn(0, target, Math.max(42, velocity - (event.technique === 'hammer-on' ? 10 : 16)));
      this.synth.noteOff(0, event.midi);
      await sleep(noteLength * (1 - split));
      this.synth.noteOff(0, target);
      return;
    }

    if (event.technique === 'bend') {
      const frames = 14;
      for (let i = 1; i <= frames; i += 1) {
        this.synth.pitchWheel?.(0, 8192 + Math.round((8191 * i) / frames));
        await sleep(noteLength * .48 / frames);
      }
      await sleep(noteLength * .34);
      for (let i = frames; i >= 0; i -= 1) {
        this.synth.pitchWheel?.(0, 8192 + Math.round((8191 * i) / frames));
        await sleep(noteLength * .14 / frames);
      }
      this.synth.noteOff(0, event.midi);
      this.synth.pitchWheel?.(0, 8192);
      return;
    }

    if (event.technique === 'vibrato') {
      const pulses = Math.max(6, Math.round(noteLength / 90));
      for (let i = 0; i < pulses; i += 1) {
        const depth = 500 + Math.round(220 * Math.sin((i / pulses) * Math.PI));
        this.synth.pitchWheel?.(0, 8192 + (i % 2 === 0 ? depth : -depth));
        await sleep(noteLength / pulses);
      }
      this.synth.noteOff(0, event.midi);
      this.synth.pitchWheel?.(0, 8192);
      return;
    }

    if (event.technique === 'slide') {
      const semitones = Math.max(-4, Math.min(4, target - event.midi));
      const frames = 16;
      for (let i = 1; i <= frames; i += 1) {
        const bend = 8192 + Math.round((8191 * semitones * i) / (2 * frames));
        this.synth.pitchWheel?.(0, Math.max(0, Math.min(16383, bend)));
        await sleep(noteLength * .68 / frames);
      }
      await sleep(noteLength * .25);
      this.synth.noteOff(0, event.midi);
      this.synth.pitchWheel?.(0, 8192);
      return;
    }

    await sleep(noteLength * .9);
    this.synth.noteOff(0, event.midi);
    await sleep(noteLength * .1);
  }

  stopLead() {
    this.leadToken += 1;
    this.synth?.stopAll?.(0);
    this.synth?.pitchWheel?.(0, 8192);
  }

  destroy() {
    this.stopLead();
    this.stopBacking();
    if (this.backingObjectUrl) URL.revokeObjectURL(this.backingObjectUrl);
    this.synth?.destroy?.();
    this.context?.close?.();
  }
}
