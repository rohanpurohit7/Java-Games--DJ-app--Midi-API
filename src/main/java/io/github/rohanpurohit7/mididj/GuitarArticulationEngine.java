package io.github.rohanpurohit7.mididj;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Builds playable lead phrases with visible and audible guitar articulations. */
public final class GuitarArticulationEngine {
    public enum Technique { PICK, HAMMER_ON, PULL_OFF, BEND, VIBRATO, SLIDE }
    public record ArticulatedNote(int midi,int tick,int duration,int velocity,Technique technique) {}

    private GuitarArticulationEngine() {}

    public static List<ArticulatedNote> articulate(List<Integer> notes, StudioAudioEngine.ToneSettings tone, long seed){
        Random r=new Random(seed); List<ArticulatedNote> result=new ArrayList<>(); int tick=0;
        for(int i=0;i<notes.size();i++){
            int current=notes.get(i); int previous=i==0?current:notes.get(i-1); int delta=current-previous;
            Technique technique;
            if(i==0) technique=Technique.PICK;
            else if(delta>0&&delta<=3) technique=Technique.HAMMER_ON;
            else if(delta<0&&delta>=-3) technique=Technique.PULL_OFF;
            else if(Math.abs(delta)>=4&&r.nextBoolean()) technique=Technique.SLIDE;
            else if(i%5==4) technique=Technique.BEND;
            else if(i%4==3||tone.vibrato()>70) technique=Technique.VIBRATO;
            else technique=Technique.PICK;
            int duration=(technique==Technique.BEND||technique==Technique.VIBRATO)?12:7+r.nextInt(4);
            int velocity=Math.max(45,Math.min(124,92+r.nextInt(17)-8+(technique==Technique.PICK?6:-4)));
            int micro=i==0?0:r.nextInt(Math.max(1,tone.humanize()/20+1))-tone.humanize()/40;
            result.add(new ArticulatedNote(current,Math.max(0,tick+micro),duration,velocity,technique));
            tick+=8+(r.nextDouble()<.22?4:0);
        }
        return result;
    }

    public static Sequence toSequence(List<ArticulatedNote> phrase,int program) throws Exception{
        Sequence sequence=new Sequence(Sequence.PPQ,16); Track track=sequence.createTrack();
        add(track,ShortMessage.PROGRAM_CHANGE,2,program,0,0);
        for(ArticulatedNote n:phrase){
            int start=n.tick(); int end=start+n.duration();
            switch(n.technique()){
                case HAMMER_ON -> { add(track,ShortMessage.CONTROL_CHANGE,2,68,127,start); note(track,n.midi(),n.velocity()-8,start,end); }
                case PULL_OFF -> { add(track,ShortMessage.CONTROL_CHANGE,2,68,100,start); note(track,n.midi(),n.velocity()-12,start,end); }
                case SLIDE -> { pitch(track,8192,start); pitch(track,n.midi()>60?9800:7000,start+1); note(track,n.midi(),n.velocity()-5,start,end); pitch(track,8192,start+4); }
                case BEND -> { note(track,n.midi(),n.velocity(),start,end); pitch(track,8192,start); pitch(track,10240,start+3); pitch(track,12288,start+6); pitch(track,8192,end-1); }
                case VIBRATO -> { note(track,n.midi(),n.velocity()-3,start,end); for(int t=start+2;t<end;t+=2) pitch(track,(t/2)%2==0?8600:7780,t); pitch(track,8192,end-1); }
                case PICK -> note(track,n.midi(),n.velocity(),start,end);
            }
        }
        return sequence;
    }

    public static String symbol(Technique t){
        return switch(t){case PICK->"●";case HAMMER_ON->"H";case PULL_OFF->"P";case BEND->"B↑";case VIBRATO->"~";case SLIDE->"/";};
    }

    private static void note(Track t,int midi,int velocity,int start,int end)throws Exception{
        add(t,ShortMessage.NOTE_ON,2,midi,Math.max(1,velocity),start);
        add(t,ShortMessage.NOTE_OFF,2,midi,0,end);
    }
    private static void pitch(Track t,int value,int tick)throws Exception{
        int v=Math.max(0,Math.min(16383,value)); add(t,ShortMessage.PITCH_BEND,2,v&127,(v>>7)&127,tick);
    }
    private static void add(Track t,int command,int channel,int d1,int d2,int tick)throws Exception{
        ShortMessage m=new ShortMessage(); m.setMessage(command,channel,d1,d2); t.add(new MidiEvent(m,tick));
    }
}
