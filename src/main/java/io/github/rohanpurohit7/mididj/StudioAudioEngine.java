package io.github.rohanpurohit7.mididj;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Soundbank;
import javax.sound.midi.Synthesizer;
import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Studio playback layer.
 *
 * High-quality mode uses an external SF2 soundbank for MIDI instruments and
 * lossless WAV/AIFF files for backing tracks. The JDK default General MIDI
 * bank remains only as a fallback.
 */
public final class StudioAudioEngine implements Closeable {
    public record ToneSettings(
            int twang,
            int warmth,
            int drive,
            int brightness,
            int sustain,
            int reverb,
            int chorus,
            int vibrato,
            int humanize
    ) {
        public ToneSettings {
            twang = clamp(twang);
            warmth = clamp(warmth);
            drive = clamp(drive);
            brightness = clamp(brightness);
            sustain = clamp(sustain);
            reverb = clamp(reverb);
            chorus = clamp(chorus);
            vibrato = clamp(vibrato);
            humanize = clamp(humanize);
        }

        private static int clamp(int value) {
            return Math.max(0, Math.min(127, value));
        }
    }

    private final Synthesizer synthesizer;
    private final Sequencer sequencer;
    private final Receiver synthReceiver;
    private Soundbank activeSoundbank;
    private MediaPlayer backingPlayer;

    public StudioAudioEngine() {
        try {
            synthesizer = MidiSystem.getSynthesizer();
            synthesizer.open();
            synthReceiver = synthesizer.getReceiver();

            sequencer = MidiSystem.getSequencer(false);
            if (sequencer == null) {
                throw new IllegalStateException("No MIDI sequencer is available");
            }
            sequencer.open();
            sequencer.getTransmitter().setReceiver(synthReceiver);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to initialize studio audio engine", ex);
        }
    }

    public boolean loadSoundFont(Path soundFont) throws Exception {
        if (soundFont == null || !Files.isRegularFile(soundFont)) {
            return false;
        }
        Soundbank bank = MidiSystem.getSoundbank(soundFont.toFile());
        if (bank == null || !synthesizer.isSoundbankSupported(bank)) {
            return false;
        }
        if (activeSoundbank != null) {
            synthesizer.unloadAllInstruments(activeSoundbank);
        }
        boolean loaded = synthesizer.loadAllInstruments(bank);
        if (loaded) {
            activeSoundbank = bank;
        }
        return loaded;
    }

    public String soundbankName() {
        if (activeSoundbank != null) {
            return activeSoundbank.getName();
        }
        Soundbank fallback = synthesizer.getDefaultSoundbank();
        return fallback == null ? "No soundbank" : fallback.getName() + " (fallback)";
    }

    public void playSequence(Sequence sequence, float bpm, int loopCount) throws Exception {
        stopMidi();
        sequencer.setSequence(sequence);
        sequencer.setTempoInBPM(bpm);
        sequencer.setLoopCount(loopCount);
        sequencer.start();
    }

    public void playLosslessBacking(Path audioFile, double rate, double volume) {
        stopBackingAudio();
        if (audioFile == null || !Files.isRegularFile(audioFile)) {
            throw new IllegalArgumentException("Backing audio file does not exist: " + audioFile);
        }
        Media media = new Media(audioFile.toUri().toString());
        backingPlayer = new MediaPlayer(media);
        backingPlayer.setCycleCount(MediaPlayer.INDEFINITE);
        backingPlayer.setRate(Math.max(0.5, Math.min(2.0, rate)));
        backingPlayer.setVolume(Math.max(0.0, Math.min(1.0, volume)));
        backingPlayer.play();
    }

    public void applyLeadTone(int channelIndex, ToneSettings settings) {
        MidiChannel[] channels = synthesizer.getChannels();
        if (channelIndex < 0 || channelIndex >= channels.length || channels[channelIndex] == null) {
            return;
        }
        MidiChannel channel = channels[channelIndex];

        // Standard MIDI CC mappings supported by most SoundFonts/synthesizers.
        channel.controlChange(74, settings.brightness());     // filter/brightness
        channel.controlChange(71, settings.warmth());         // resonance/timbre
        channel.controlChange(72, settings.sustain());        // release time
        channel.controlChange(73, Math.max(8, 127 - settings.twang())); // attack time
        channel.controlChange(91, settings.reverb());
        channel.controlChange(93, settings.chorus());
        channel.controlChange(1, settings.vibrato());
        channel.controlChange(11, Math.max(35, 127 - settings.drive() / 4));
        channel.controlChange(7, 112);
    }

    public int humanizedVelocity(int base, ToneSettings settings, int noteIndex) {
        int spread = Math.max(1, settings.humanize() / 8);
        int signedOffset = ((noteIndex * 37) % (spread * 2 + 1)) - spread;
        int driveBoost = settings.drive() / 12;
        return Math.max(35, Math.min(127, base + signedOffset + driveBoost));
    }

    public int humanizedTick(int tick, ToneSettings settings, int noteIndex) {
        int amount = settings.humanize() / 28;
        if (amount == 0) {
            return tick;
        }
        int offset = ((noteIndex * 17) % (amount * 2 + 1)) - amount;
        return Math.max(0, tick + offset);
    }

    public void stopMidi() {
        if (sequencer.isRunning()) {
            sequencer.stop();
        }
    }

    public void stopBackingAudio() {
        if (backingPlayer != null) {
            backingPlayer.stop();
            backingPlayer.dispose();
            backingPlayer = null;
        }
    }

    public void stopAll() {
        stopMidi();
        stopBackingAudio();
    }

    @Override
    public void close() throws IOException {
        stopAll();
        sequencer.close();
        synthReceiver.close();
        synthesizer.close();
    }
}
