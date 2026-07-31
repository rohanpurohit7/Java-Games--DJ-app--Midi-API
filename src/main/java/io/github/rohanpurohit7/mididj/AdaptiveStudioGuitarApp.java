package io.github.rohanpurohit7.mididj;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/** Guitar-first studio with explainable AI scale/tone recommendations. */
public final class AdaptiveStudioGuitarApp extends Application {
    private static final String[] NOTES = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
    private static final String[] SCALES = {"Minor Pentatonic", "Blues", "Major Pentatonic", "Natural Minor", "Dorian", "Mixolydian", "Phrygian Dominant"};
    private static final int[] OPEN_STRINGS = {64, 59, 55, 50, 45, 40};
    private static final String[] STRING_NAMES = {"e", "B", "G", "D", "A", "E"};
    private static final Path LICK_FILE = Path.of("saved-guitar-licks.txt");

    private final List<StudioBackingCatalog.Style> styles = StudioBackingCatalog.styles();
    private final List<Integer> currentLick = new ArrayList<>();
    private final Label[][] fretCells = new Label[6][13];
    private final Random random = new Random();

    private StudioAudioEngine audio;
    private ComboBox<StudioBackingCatalog.Style> styleCombo;
    private ComboBox<String> keyCombo;
    private ComboBox<String> scaleCombo;
    private Slider tempo;
    private Slider twang, warmth, drive, brightness, sustain, reverb, chorus, vibrato, humanize;
    private Label status, recommendation, pickup, lickText, soundbank;
    private GuitarVisualCard guitarVisual;
    private Path backingLibrary;
    private Timeline animation;

    @Override public void start(Stage stage) {
        audio = new StudioAudioEngine();
        BorderPane root = new BorderPane();
        root.getStyleClass().add("guitar-studio");
        root.setTop(header(stage));
        root.setCenter(center());
        root.setBottom(transport());
        Scene scene = new Scene(root, 1580, 980);
        var css = getClass().getResource("/styles/guitar-improv.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());
        stage.setTitle("AI Studio Guitar Improvisation");
        stage.setScene(scene);
        stage.setMinWidth(1120);
        stage.setMinHeight(760);
        stage.show();
        refreshFretboard(-1);
        applySuggestion();
    }

    private VBox header(Stage stage) {
        Label title = new Label("AI STUDIO GUITAR IMPROVISATION");
        title.getStyleClass().add("studio-title");
        status = new Label("Choose a soulful track or let AI select the guitar, pickup, scale and tone.");
        status.getStyleClass().add("status-line");
        soundbank = new Label("Soundbank: " + audio.soundbankName());

        styleCombo = new ComboBox<>(); styleCombo.getItems().setAll(styles); styleCombo.setValue(styles.get(0));
        keyCombo = new ComboBox<>(); keyCombo.getItems().setAll(NOTES); keyCombo.setValue("A");
        scaleCombo = new ComboBox<>(); scaleCombo.getItems().setAll(SCALES); scaleCombo.setValue("Minor Pentatonic");
        styleCombo.valueProperty().addListener((o,a,b) -> { if (b != null && tempo != null) tempo.setValue(b.bpm()); });
        keyCombo.valueProperty().addListener((o,a,b) -> refreshFretboard(-1));
        scaleCombo.valueProperty().addListener((o,a,b) -> refreshFretboard(-1));

        Button suggest = button("AI SUGGEST GUITAR + SCALE"); suggest.setOnAction(e -> applySuggestion());
        Button loadSf2 = button("LOAD GUITAR SF2"); loadSf2.setOnAction(e -> {
            FileChooser c = new FileChooser(); c.getExtensionFilters().add(new FileChooser.ExtensionFilter("SoundFont 2", "*.sf2"));
            File f = c.showOpenDialog(stage); if (f == null) return;
            try { audio.loadSoundFont(f.toPath()); soundbank.setText("Soundbank: " + audio.soundbankName()); }
            catch (Exception ex) { status.setText("SoundFont error: " + ex.getMessage()); }
        });
        Button backing = button("CHOOSE WAV/AIFF LIBRARY"); backing.setOnAction(e -> {
            File f = new DirectoryChooser().showDialog(stage); if (f != null) backingLibrary = f.toPath();
        });
        FlowPane row = new FlowPane(10, 8, labeled("TRACK", styleCombo), labeled("KEY", keyCombo), labeled("SCALE", scaleCombo), suggest, loadSf2, backing, soundbank);
        return padded(new VBox(8, title, status, row));
    }

    private HBox center() {
        VBox board = new VBox(10, new Label("ANIMATED SCALE / LICK FRETBOARD"), fretboard(), lickPanel());
        HBox.setHgrow(board, Priority.ALWAYS); board.setPadding(new Insets(16));
        VBox advisor = new VBox(10);
        guitarVisual = new GuitarVisualCard();
        recommendation = new Label(); recommendation.setWrapText(true);
        pickup = new Label();
        advisor.getChildren().addAll(new Label("AI GUITAR ADVISOR"), guitarVisual, pickup, recommendation, toneOverlay());
        advisor.setPadding(new Insets(16)); advisor.setPrefWidth(390);
        return new HBox(12, board, advisor);
    }

    private GridPane fretboard() {
        GridPane g = new GridPane(); g.getStyleClass().add("fretboard"); g.setHgap(2); g.setVgap(2);
        g.add(new Label("String"), 0, 0);
        for (int f=0; f<13; f++) g.add(new Label(Integer.toString(f)), f+1, 0);
        for (int s=0; s<6; s++) {
            g.add(new Label(STRING_NAMES[s]), 0, s+1);
            for (int f=0; f<13; f++) {
                int midi = OPEN_STRINGS[s] + f; Label cell = new Label(NOTES[midi%12]);
                cell.setMinSize(56, 50); cell.setAlignment(Pos.CENTER); cell.getStyleClass().add("fret-cell");
                fretCells[s][f] = cell; g.add(cell, f+1, s+1);
            }
        }
        return g;
    }

    private VBox lickPanel() {
        lickText = new Label("Generate a lick to begin."); lickText.setWrapText(true); lickText.getStyleClass().add("lick-readout");
        return new VBox(4, new Label("CURRENT LICK"), lickText);
    }

    private VBox toneOverlay() {
        twang=knob(70); warmth=knob(80); drive=knob(35); brightness=knob(70); sustain=knob(80);
        reverb=knob(48); chorus=knob(16); vibrato=knob(18); humanize=knob(80);
        Button apply=button("APPLY MANUAL TONE"); apply.setOnAction(e -> audio.applyLeadTone(2, tone()));
        return new VBox(5, new Label("AUTO-TUNER / TONE OVERLAY"), slider("TWANG",twang), slider("WARMTH",warmth),
                slider("DRIVE",drive), slider("BRIGHTNESS",brightness), slider("SUSTAIN",sustain), slider("REVERB",reverb),
                slider("CHORUS",chorus), slider("VIBRATO",vibrato), slider("HUMAN FEEL",humanize), apply);
    }

    private VBox transport() {
        Button play=button("PLAY STUDIO BACKING"); play.setOnAction(e -> playBacking());
        Button generate=button("GENERATE NEW LICK"); generate.setOnAction(e -> generate(false));
        Button variation=button("GENERATE VARIATION"); variation.setOnAction(e -> generate(true));
        Button animate=button("PLAY / ANIMATE LICK"); animate.setOnAction(e -> playLick());
        Button save=button("SAVE LICK"); save.setOnAction(e -> saveLick());
        Button stop=button("STOP ALL"); stop.setOnAction(e -> stopAll());
        Button advanced=button("OPEN FULL STUDIO"); advanced.setOnAction(e -> { try { new StudioQualityGuitarApp().start(new Stage()); } catch(Exception ex){ status.setText(ex.getMessage()); }});
        tempo = new Slider(55,180,styles.get(0).bpm()); tempo.setShowTickLabels(true);
        FlowPane buttons = new FlowPane(8,8,play,generate,variation,animate,save,stop,advanced);
        HBox bpm = new HBox(8,new Label("BPM"),tempo); HBox.setHgrow(tempo,Priority.ALWAYS);
        return padded(new VBox(8,buttons,bpm));
    }

    private void applySuggestion() {
        GuitarStyleAdvisor.Recommendation r = GuitarStyleAdvisor.recommend(styleCombo.getValue());
        scaleCombo.setValue(r.scale()); pickup.setText("Pickup: " + r.pickup()); recommendation.setText(r.explanation());
        guitarVisual.showGuitar(r.guitarType()); setTone(r.tone()); audio.applyLeadTone(2,r.tone());
        status.setText("AI selected " + r.guitarType() + " · " + r.scale() + " · " + r.pickup()); refreshFretboard(-1);
    }

    private void setTone(StudioAudioEngine.ToneSettings t) {
        twang.setValue(t.twang()); warmth.setValue(t.warmth()); drive.setValue(t.drive()); brightness.setValue(t.brightness());
        sustain.setValue(t.sustain()); reverb.setValue(t.reverb()); chorus.setValue(t.chorus()); vibrato.setValue(t.vibrato()); humanize.setValue(t.humanize());
    }

    private void playBacking() {
        StudioBackingCatalog.Style style=styleCombo.getValue(); Path stem=style.resolveStem(backingLibrary);
        try {
            if(stem!=null) audio.playLosslessBacking(stem,tempo.getValue()/style.bpm(),.92);
            else audio.playSequence(generatedBacking(style),(float)tempo.getValue(),Sequencer.LOOP_CONTINUOUSLY);
            status.setText(stem!=null?"Playing lossless studio backing.":"Generated fallback playing; add "+style.stemBaseName()+".wav for studio audio.");
        } catch(Exception ex){ status.setText("Backing error: "+ex.getMessage()); }
    }

    private Sequence generatedBacking(StudioBackingCatalog.Style style) throws Exception {
        Sequence seq=new Sequence(Sequence.PPQ,16); Track t=seq.createTrack(); int root=48+root();
        t.add(event(ShortMessage.PROGRAM_CHANGE,0,style.chordProgram(),0,0)); t.add(event(ShortMessage.PROGRAM_CHANGE,1,style.bassProgram(),0,0));
        for(int bar=0;bar<Math.max(8,style.progression().length);bar++){
            int start=bar*64, cr=root+style.progression()[bar%style.progression().length]; int third=minor()?3:4;
            int[] chord={cr,cr+third,cr+7,cr+10,cr+14}; for(int i=0;i<chord.length;i++) note(t,0,chord[i],50+i*4,start+i*2,start+58);
            for(int i=0;i<8;i++) note(t,1,cr-12+(i%2==0?0:7),70+i%3*4,start+i*8,start+i*8+6);
            for(int step=0;step<16;step++){int tick=start+step*4;if(step==0||step==8) drum(t,36,tick,96);if(step==4||step==12)drum(t,38,tick,88);if(step%2==0)drum(t,42,tick,48);}
        } return seq;
    }

    private void generate(boolean variation) {
        List<Integer> old=new ArrayList<>(currentLick); currentLick.clear();
        currentLick.addAll(variation&&!old.isEmpty()?GuitarTheory.variation(old,random.nextLong()):GuitarTheory.generateLick(57+root(),scaleCombo.getValue(),12,random.nextLong()));
        lickText.setText(currentLick.stream().map(this::name).reduce((a,b)->a+" → "+b).orElse("")); refreshFretboard(-1);
    }

    private void playLick() {
        if(currentLick.isEmpty())generate(false);
        try { Sequence seq=new Sequence(Sequence.PPQ,16);Track t=seq.createTrack();StudioAudioEngine.ToneSettings tone=tone();audio.applyLeadTone(2,tone);
            t.add(event(ShortMessage.PROGRAM_CHANGE,2,styleCombo.getValue().leadProgram(),0,0));
            for(int i=0;i<currentLick.size();i++){int tick=audio.humanizedTick(i*8,tone,i);int vel=audio.humanizedVelocity(98,tone,i);note(t,2,currentLick.get(i),vel,tick,tick+6+tone.sustain()/28);}audio.playSequence(seq,(float)tempo.getValue(),0);animate();
        }catch(Exception ex){status.setText("Lick error: "+ex.getMessage());}
    }

    private void animate(){if(animation!=null)animation.stop();final int[] i={0};animation=new Timeline(new KeyFrame(Duration.millis(60000/tempo.getValue()/2),e->{if(i[0]>=currentLick.size()){animation.stop();refreshFretboard(-1);return;}refreshFretboard(currentLick.get(i[0]++));}));animation.setCycleCount(currentLick.size()+1);animation.play();}
    private void saveLick(){if(currentLick.isEmpty())return;try{Files.writeString(LICK_FILE,keyCombo.getValue()+"|"+scaleCombo.getValue()+"|"+currentLick+System.lineSeparator(),StandardOpenOption.CREATE,StandardOpenOption.APPEND);status.setText("Lick saved locally.");}catch(Exception ex){status.setText(ex.getMessage());}}

    private void refreshFretboard(int active){if(keyCombo==null||scaleCombo==null)return;int root=root();int[] ints=GuitarTheory.intervals(scaleCombo.getValue());for(int s=0;s<6;s++)for(int f=0;f<13;f++){Label c=fretCells[s][f];if(c==null)continue;c.getStyleClass().removeAll("scale-note","root-note","lick-note-active");int m=OPEN_STRINGS[s]+f,rel=Math.floorMod(m%12-root,12);if(Arrays.stream(ints).anyMatch(v->v==rel))c.getStyleClass().add("scale-note");if(m%12==root)c.getStyleClass().add("root-note");if(m==active)c.getStyleClass().add("lick-note-active");}}
    private StudioAudioEngine.ToneSettings tone(){return new StudioAudioEngine.ToneSettings((int)twang.getValue(),(int)warmth.getValue(),(int)drive.getValue(),(int)brightness.getValue(),(int)sustain.getValue(),(int)reverb.getValue(),(int)chorus.getValue(),(int)vibrato.getValue(),(int)humanize.getValue());}
    private int root(){return Arrays.asList(NOTES).indexOf(keyCombo.getValue());} private boolean minor(){String s=scaleCombo.getValue();return s.contains("Minor")||s.equals("Blues")||s.equals("Dorian")||s.contains("Phrygian");} private String name(int m){return NOTES[Math.floorMod(m,12)]+(m/12-1);}
    private void note(Track t,int ch,int n,int v,int st,int en)throws Exception{t.add(event(ShortMessage.NOTE_ON,ch,Math.max(0,Math.min(127,n)),v,st));t.add(event(ShortMessage.NOTE_OFF,ch,Math.max(0,Math.min(127,n)),0,en));}private void drum(Track t,int n,int tick,int v)throws Exception{note(t,9,n,v,tick,tick+2);}private MidiEvent event(int cmd,int ch,int d1,int d2,int tick)throws Exception{ShortMessage m=new ShortMessage();m.setMessage(cmd,ch,d1,d2);return new MidiEvent(m,tick);}
    private Slider knob(int value){return new Slider(0,127,value);} private VBox slider(String n,Slider s){Label v=new Label(Integer.toString((int)s.getValue()));s.valueProperty().addListener((o,a,b)->v.setText(Integer.toString(b.intValue())));HBox h=new HBox(6,s,v);HBox.setHgrow(s,Priority.ALWAYS);return new VBox(2,new Label(n),h);}private VBox labeled(String n,javafx.scene.Node node){return new VBox(2,new Label(n),node);}private Button button(String n){Button b=new Button(n);b.getStyleClass().add("studio-button");return b;}private <T extends javafx.scene.layout.Region>T padded(T r){r.setPadding(new Insets(14));return r;}
    private void stopAll(){if(animation!=null)animation.stop();audio.stopAll();refreshFretboard(-1);} @Override public void stop()throws Exception{stopAll();audio.close();} public static void main(String[]args){launch(args);}
}
