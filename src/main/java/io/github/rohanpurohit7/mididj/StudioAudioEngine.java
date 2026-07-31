package io.github.rohanpurohit7.mididj;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import javax.sound.midi.*;
import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Concurrent studio playback: independent backing, lead and lossless-audio layers. */
public final class StudioAudioEngine implements Closeable {
    public record ToneSettings(int twang, int warmth, int drive, int brightness, int sustain,
                               int reverb, int chorus, int vibrato, int humanize) {
        public ToneSettings {
            twang=clamp(twang); warmth=clamp(warmth); drive=clamp(drive); brightness=clamp(brightness);
            sustain=clamp(sustain); reverb=clamp(reverb); chorus=clamp(chorus);
            vibrato=clamp(vibrato); humanize=clamp(humanize);
        }
        private static int clamp(int v){ return Math.max(0,Math.min(127,v)); }
    }

    private final Synthesizer synthesizer;
    private final Receiver synthReceiver;
    private final Sequencer backingSequencer;
    private final Sequencer leadSequencer;
    private Soundbank activeSoundbank;
    private MediaPlayer backingPlayer;

    public StudioAudioEngine() {
        try {
            synthesizer=MidiSystem.getSynthesizer(); synthesizer.open(); synthReceiver=synthesizer.getReceiver();
            backingSequencer=createSequencer();
            leadSequencer=createSequencer();
        } catch(Exception ex){ throw new IllegalStateException("Unable to initialize studio audio engine",ex); }
    }

    private Sequencer createSequencer() throws Exception {
        Sequencer value=MidiSystem.getSequencer(false);
        if(value==null) throw new IllegalStateException("No MIDI sequencer available");
        value.open(); value.getTransmitter().setReceiver(synthReceiver); return value;
    }

    public boolean loadSoundFont(Path file) throws Exception {
        if(file==null||!Files.isRegularFile(file)) return false;
        Soundbank bank=MidiSystem.getSoundbank(file.toFile());
        if(bank==null||!synthesizer.isSoundbankSupported(bank)) return false;
        if(activeSoundbank!=null) synthesizer.unloadAllInstruments(activeSoundbank);
        boolean loaded=synthesizer.loadAllInstruments(bank);
        if(loaded) activeSoundbank=bank;
        return loaded;
    }

    public String soundbankName(){
        if(activeSoundbank!=null) return activeSoundbank.getName();
        Soundbank fallback=synthesizer.getDefaultSoundbank();
        return fallback==null?"No soundbank":fallback.getName()+" (fallback)";
    }

    /** Backward-compatible alias; now treated as lead playback. */
    public void playSequence(Sequence sequence,float bpm,int loopCount) throws Exception { playLeadSequence(sequence,bpm,loopCount); }

    public void playBackingSequence(Sequence sequence,float bpm,int loopCount) throws Exception {
        if(backingSequencer.isRunning()) backingSequencer.stop();
        backingSequencer.setSequence(sequence); backingSequencer.setTempoInBPM(bpm);
        backingSequencer.setLoopCount(loopCount); backingSequencer.start();
    }

    public void playLeadSequence(Sequence sequence,float bpm,int loopCount) throws Exception {
        if(leadSequencer.isRunning()) leadSequencer.stop();
        leadSequencer.setSequence(sequence); leadSequencer.setTempoInBPM(bpm);
        leadSequencer.setLoopCount(loopCount); leadSequencer.start();
    }

    public boolean isBackingRunning(){ return backingSequencer.isRunning() || (backingPlayer!=null && backingPlayer.getStatus()==MediaPlayer.Status.PLAYING); }

    public void playLosslessBacking(Path file,double rate,double volume){
        stopBackingAudio();
        if(file==null||!Files.isRegularFile(file)) throw new IllegalArgumentException("Backing audio file does not exist: "+file);
        backingPlayer=new MediaPlayer(new Media(file.toUri().toString()));
        backingPlayer.setCycleCount(MediaPlayer.INDEFINITE);
        backingPlayer.setRate(Math.max(.5,Math.min(2,rate)));
        backingPlayer.setVolume(Math.max(0,Math.min(1,volume)));
        backingPlayer.play();
    }

    public void applyLeadTone(int channelIndex,ToneSettings s){
        MidiChannel[] channels=synthesizer.getChannels();
        if(channelIndex<0||channelIndex>=channels.length||channels[channelIndex]==null) return;
        MidiChannel c=channels[channelIndex];
        c.controlChange(74,s.brightness()); c.controlChange(71,s.warmth()); c.controlChange(72,s.sustain());
        c.controlChange(73,Math.max(5,127-s.twang())); c.controlChange(91,s.reverb());
        c.controlChange(93,s.chorus()); c.controlChange(1,s.vibrato());
        c.controlChange(11,Math.max(45,127-s.drive()/4)); c.controlChange(7,116);
        c.setPitchBend(8192);
    }

    public int humanizedVelocity(int base,ToneSettings s,int i){
        int spread=Math.max(1,s.humanize()/9); int offset=((i*37)%(spread*2+1))-spread;
        return Math.max(35,Math.min(127,base+offset+s.drive()/14));
    }
    public int humanizedTick(int tick,ToneSettings s,int i){
        int amount=s.humanize()/30; if(amount==0)return tick;
        return Math.max(0,tick+((i*17)%(amount*2+1))-amount);
    }

    public void stopLead(){ if(leadSequencer.isRunning()) leadSequencer.stop(); }
    public void stopBackingMidi(){ if(backingSequencer.isRunning()) backingSequencer.stop(); }
    public void stopMidi(){ stopLead(); }
    public void stopBackingAudio(){ if(backingPlayer!=null){backingPlayer.stop();backingPlayer.dispose();backingPlayer=null;} }
    public void stopBacking(){ stopBackingMidi(); stopBackingAudio(); }
    public void stopAll(){ stopLead(); stopBacking(); }

    @Override public void close() throws IOException {
        stopAll(); backingSequencer.close(); leadSequencer.close(); synthReceiver.close(); synthesizer.close();
    }
}
