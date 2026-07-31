package io.github.rohanpurohit7.mididj;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import javax.sound.midi.*;
import java.io.File;
import java.nio.file.Path;
import java.util.*;

/** Responsive amp-style improvisation studio with independent backing and articulated lead lanes. */
public final class AmpStudioGuitarApp extends Application {
    private static final String[] NOTES={"C","C#","D","D#","E","F","F#","G","G#","A","A#","B"};
    private static final String[] SCALES={"Minor Pentatonic","Blues","Major Pentatonic","Natural Minor","Dorian","Mixolydian","Phrygian Dominant"};
    private static final int[] OPEN={64,59,55,50,45,40};
    private static final String[] STRINGS={"e","B","G","D","A","E"};

    private final List<StudioBackingCatalog.Style> styles=StudioBackingCatalog.styles();
    private final Label[][] cells=new Label[6][13];
    private final Random random=new Random();
    private List<Integer> lick=new ArrayList<>();
    private List<GuitarArticulationEngine.ArticulatedNote> phrase=List.of();

    private StudioAudioEngine audio;
    private ComboBox<StudioBackingCatalog.Style> style;
    private ComboBox<String> key,scale;
    private Slider bpm,twang,warmth,drive,brightness,sustain,reverb,chorus,vibrato,humanize;
    private Label status,soundbank,technique,phraseText,pickup,recommendation;
    private GuitarPhotoCard photo;
    private Timeline traceTimeline,continuousTimeline;
    private Path backingLibrary;
    private VBox responsiveBody;
    private Node boardPanel,ampPanel;
    private int phraseGeneration;

    @Override public void start(Stage stage){
        audio=new StudioAudioEngine();
        BorderPane shell=new BorderPane(); shell.getStyleClass().addAll("guitar-studio","amp-shell");
        shell.setTop(header(stage)); shell.setCenter(body()); shell.setBottom(transport());
        Scene scene=new Scene(shell,1500,950);
        var css=getClass().getResource("/styles/guitar-improv.css"); if(css!=null)scene.getStylesheets().add(css.toExternalForm());
        stage.setTitle("Studio Guitar Improv Amp"); stage.setScene(scene); stage.setMinWidth(760); stage.setMinHeight(620);
        scene.widthProperty().addListener((o,a,b)->reflow(b.doubleValue()));
        stage.show(); reflow(scene.getWidth()); refresh(-1,null); suggest(); autoLoadSoundFont();
    }

    private VBox header(Stage stage){
        Label title=new Label("STUDIO GUITAR IMPROV AMP"); title.getStyleClass().add("amp-logo");
        status=new Label("The backing band continues while licks and continuous improvisations play over it."); status.setWrapText(true); status.getStyleClass().add("status-line");
        soundbank=new Label("Guitar library: "+audio.soundbankName());
        style=new ComboBox<>(); style.getItems().setAll(styles); style.setValue(styles.get(0));
        key=new ComboBox<>(); key.getItems().setAll(NOTES); key.setValue("A");
        scale=new ComboBox<>(); scale.getItems().setAll(SCALES); scale.setValue("Blues");
        style.valueProperty().addListener((o,a,b)->{if(b!=null&&bpm!=null)bpm.setValue(b.bpm()); if(photo!=null)suggest();});
        key.valueProperty().addListener((o,a,b)->refresh(-1,null)); scale.valueProperty().addListener((o,a,b)->refresh(-1,null));
        Button install=button("INSTALL FREEPATS GUITAR"); install.setOnAction(e->installFreePats());
        Button library=button("CHOOSE WAV/AIFF BAND"); library.setOnAction(e->{File f=new DirectoryChooser().showDialog(stage);if(f!=null)backingLibrary=f.toPath();});
        FlowPane controls=new FlowPane(10,8,labeled("BACKING",style),labeled("KEY",key),labeled("SCALE",scale),install,library,soundbank);
        VBox box=new VBox(8,title,status,controls); box.setPadding(new Insets(14,18,10,18)); return box;
    }

    private ScrollPane body(){
        boardPanel=boardPanel(); ampPanel=ampPanel(); responsiveBody=new VBox(); responsiveBody.setPadding(new Insets(10));
        ScrollPane scroll=new ScrollPane(responsiveBody); scroll.setFitToWidth(true); scroll.setPannable(true); return scroll;
    }

    private VBox boardPanel(){
        GridPane grid=new GridPane(); grid.getStyleClass().add("fretboard"); grid.setHgap(2);grid.setVgap(2);
        grid.add(new Label("String"),0,0); for(int f=0;f<13;f++)grid.add(new Label(String.valueOf(f)),f+1,0);
        for(int s=0;s<6;s++){grid.add(new Label(STRINGS[s]),0,s+1);for(int f=0;f<13;f++){int midi=OPEN[s]+f;Label c=new Label(NOTES[midi%12]);c.setMinSize(48,46);c.setAlignment(Pos.CENTER);c.getStyleClass().add("fret-cell");cells[s][f]=c;grid.add(c,f+1,s+1);}}
        ScrollPane fretScroll=new ScrollPane(grid);fretScroll.setFitToHeight(true);fretScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);fretScroll.setMinHeight(390);
        technique=new Label("Technique: —");technique.getStyleClass().add("technique-display");
        phraseText=new Label("Generate a lick to see H (hammer-on), P (pull-off), B↑ (bend), ~ (vibrato), and / (slide).");phraseText.setWrapText(true);phraseText.getStyleClass().add("lick-readout");
        VBox box=new VBox(10,new Label("ARTICULATED LICK TRACE"),fretScroll,technique,phraseText);box.getStyleClass().add("speaker-cabinet");box.setPadding(new Insets(14));return box;
    }

    private VBox ampPanel(){
        photo=new GuitarPhotoCard();pickup=new Label();recommendation=new Label();recommendation.setWrapText(true);
        twang=knob(75);warmth=knob(80);drive=knob(42);brightness=knob(72);sustain=knob(88);reverb=knob(48);chorus=knob(16);vibrato=knob(62);humanize=knob(92);
        GridPane knobs=new GridPane();knobs.setHgap(10);knobs.setVgap(8);
        String[] names={"TWANG","WARMTH","DRIVE","BRIGHT","SUSTAIN","REVERB","CHORUS","VIBRATO","HUMAN"};
        Slider[] values={twang,warmth,drive,brightness,sustain,reverb,chorus,vibrato,humanize};
        for(int i=0;i<values.length;i++)knobs.add(knobControl(names[i],values[i]),i%3,i/3);
        Button suggest=button("AI MATCH GUITAR + TONE");suggest.setOnAction(e->suggest());
        Button apply=button("APPLY AMP SETTINGS");apply.setOnAction(e->audio.applyLeadTone(2,tone()));
        VBox box=new VBox(10,new Label("AMP HEAD / GUITAR ADVISOR"),photo,pickup,recommendation,knobs,new FlowPane(8,8,suggest,apply));box.getStyleClass().add("amp-head");box.setPadding(new Insets(14));box.setPrefWidth(420);return box;
    }

    private VBox transport(){
        Button play=button("PLAY BACKING BAND");play.setOnAction(e->playBacking());
        Button gen=button("GENERATE LICK");gen.setOnAction(e->generate(false));
        Button lead=button("PLAY LICK OVER BAND");lead.setOnAction(e->playLead());
        Button trace=button("SEE TRACE");trace.setOnAction(e->startTraceOnly());
        Button continuous=button("PLAY CONTINUOUS IMPROV");continuous.setOnAction(e->startContinuousImprov());
        Button stopLead=button("STOP LEAD / TRACE");stopLead.setOnAction(e->stopLeadAndTrace());
        Button stopBand=button("STOP BAND");stopBand.setOnAction(e->audio.stopBacking());
        bpm=new Slider(55,180,styles.get(0).bpm());bpm.setShowTickLabels(true);bpm.setShowTickMarks(true);
        FlowPane buttons=new FlowPane(8,8,play,gen,lead,trace,continuous,stopLead,stopBand);
        HBox tempo=new HBox(8,new Label("BPM"),bpm);HBox.setHgrow(bpm,Priority.ALWAYS);
        VBox box=new VBox(8,buttons,tempo);box.getStyleClass().add("amp-transport");box.setPadding(new Insets(10,18,14,18));return box;
    }

    private void reflow(double width){if(responsiveBody==null)return;responsiveBody.getChildren().clear();if(width<1160)responsiveBody.getChildren().add(new VBox(12,boardPanel,ampPanel));else{HBox row=new HBox(12,boardPanel,ampPanel);HBox.setHgrow(boardPanel,Priority.ALWAYS);responsiveBody.getChildren().add(row);}}

    private void autoLoadSoundFont(){Path installed=FreePatsGuitarLibrary.installedCleanSf2();if(installed!=null)try{audio.loadSoundFont(installed);soundbank.setText("Guitar library: "+audio.soundbankName());}catch(Exception ignored){}}
    private void installFreePats(){status.setText("Downloading CC0 FreePats clean electric guitar…");new Thread(()->{try{Path sf2=FreePatsGuitarLibrary.installDefault();boolean loaded=audio.loadSoundFont(sf2);Platform.runLater(()->{soundbank.setText("Guitar library: "+audio.soundbankName());status.setText(loaded?"FreePats guitar installed and loaded.":"Downloaded SF2 was unsupported.");});}catch(Exception ex){Platform.runLater(()->status.setText("FreePats install failed: "+ex.getMessage()));}},"freepats-installer").start();}

    private void suggest(){if(style==null||style.getValue()==null||scale==null)return;var r=GuitarStyleAdvisor.recommend(style.getValue());scale.setValue(r.scale());pickup.setText("Pickup: "+r.pickup());recommendation.setText(r.explanation());photo.showGuitar(r.guitarType());setTone(r.tone());audio.applyLeadTone(2,r.tone());refresh(-1,null);}
    private void setTone(StudioAudioEngine.ToneSettings t){twang.setValue(t.twang());warmth.setValue(t.warmth());drive.setValue(t.drive());brightness.setValue(t.brightness());sustain.setValue(t.sustain());reverb.setValue(t.reverb());chorus.setValue(t.chorus());vibrato.setValue(t.vibrato());humanize.setValue(t.humanize());}

    private void playBacking(){try{var s=style.getValue();Path stem=s.resolveStem(backingLibrary);if(stem!=null)audio.playLosslessBacking(stem,bpm.getValue()/s.bpm(),.92);else audio.playBackingSequence(backingSequence(s),(float)bpm.getValue(),Sequencer.LOOP_CONTINUOUSLY);status.setText("Backing band playing. Lead lane remains independent.");}catch(Exception ex){status.setText("Backing error: "+ex.getMessage());}}
    private Sequence backingSequence(StudioBackingCatalog.Style s)throws Exception{Sequence seq=new Sequence(Sequence.PPQ,16);Track t=seq.createTrack();int base=48+root();add(t,ShortMessage.PROGRAM_CHANGE,0,s.chordProgram(),0,0);add(t,ShortMessage.PROGRAM_CHANGE,1,s.bassProgram(),0,0);for(int bar=0;bar<Math.max(8,s.progression().length);bar++){int start=bar*64,cr=base+s.progression()[bar%s.progression().length],third=isMinor()?3:4;for(int n:new int[]{cr,cr+third,cr+7,cr+10,cr+14})note(t,0,n,56,start,start+58);for(int i=0;i<8;i++)note(t,1,cr-12+(i%2==0?0:7),72,start+i*8,start+i*8+6);for(int step=0;step<16;step++){int tick=start+step*4;if(step==0||step==8)note(t,9,36,98,tick,tick+2);if(step==4||step==12)note(t,9,38,90,tick,tick+2);if(step%2==0)note(t,9,42,50,tick,tick+2);}}return seq;}

    private void generate(boolean variation){List<Integer> old=new ArrayList<>(lick);lick=variation&&!old.isEmpty()?GuitarTheory.variation(old,random.nextLong()):GuitarTheory.generateLick(57+root(),scale.getValue(),12,random.nextLong());phrase=GuitarArticulationEngine.articulate(lick,tone(),random.nextLong());phraseText.setText(renderPhrase());refresh(-1,null);}
    private String renderPhrase(){StringBuilder b=new StringBuilder();for(var n:phrase)b.append(noteName(n.midi())).append(' ').append(GuitarArticulationEngine.symbol(n.technique())).append("  →  ");return b.length()>4?b.substring(0,b.length()-5):"";}
    private void playLead(){if(phrase.isEmpty())generate(false);try{audio.applyLeadTone(2,tone());audio.playLeadSequence(GuitarArticulationEngine.toSequence(phrase,style.getValue().leadProgram()),(float)bpm.getValue(),0);animatePhrase(false);}catch(Exception ex){status.setText("Lead error: "+ex.getMessage());}}

    private void startTraceOnly(){if(phrase.isEmpty())generate(false);animatePhrase(true);status.setText("Trace-only mode: silent theory-aware lick path.");}
    private void startContinuousImprov(){stopLeadAndTrace();if(!audio.isBackingRunning())playBacking();phraseGeneration=0;continuousTimeline=new Timeline(new KeyFrame(Duration.seconds(0.1),e->playNextContinuousPhrase()));continuousTimeline.setCycleCount(Timeline.INDEFINITE);continuousTimeline.play();status.setText("Continuous articulated improvisation playing over the backing band.");}
    private void playNextContinuousPhrase(){if(audio==null)return;if(traceTimeline!=null&&traceTimeline.getStatus()==Timeline.Status.RUNNING)return;List<Integer> previous=new ArrayList<>(lick);lick=previous.isEmpty()||phraseGeneration%4==0?GuitarTheory.generateLick(57+root(),scale.getValue(),10+random.nextInt(5),random.nextLong()):GuitarTheory.variation(previous,random.nextLong());phrase=GuitarArticulationEngine.articulate(lick,tone(),random.nextLong());phraseText.setText(renderPhrase());phraseGeneration++;try{audio.applyLeadTone(2,tone());audio.playLeadSequence(GuitarArticulationEngine.toSequence(phrase,style.getValue().leadProgram()),(float)bpm.getValue(),0);animatePhrase(false);}catch(Exception ex){status.setText(ex.getMessage());}}

    private void animatePhrase(boolean silent){if(traceTimeline!=null)traceTimeline.stop();final int[] i={0};double interval=60000.0/bpm.getValue()/2;traceTimeline=new Timeline(new KeyFrame(Duration.millis(interval),e->{if(i[0]>=phrase.size()){traceTimeline.stop();refresh(-1,null);return;}var n=phrase.get(i[0]++);refresh(n.midi(),n.technique());technique.setText("Technique: "+n.technique()+"  "+GuitarArticulationEngine.symbol(n.technique()));}));traceTimeline.setCycleCount(phrase.size()+1);traceTimeline.play();if(silent)audio.stopLead();}
    private void stopLeadAndTrace(){audio.stopLead();if(traceTimeline!=null)traceTimeline.stop();if(continuousTimeline!=null)continuousTimeline.stop();refresh(-1,null);status.setText("Lead and trace stopped; backing remains active.");}

    private void refresh(int active,GuitarArticulationEngine.Technique tech){int root=root();int[] intervals=GuitarTheory.intervals(scale.getValue());for(int s=0;s<6;s++)for(int f=0;f<13;f++){Label c=cells[s][f];if(c==null)continue;c.getStyleClass().removeAll("scale-note","root-note","lick-note-active","tech-hammer","tech-pull","tech-bend","tech-vibrato","tech-slide");int midi=OPEN[s]+f,rel=Math.floorMod(midi%12-root,12);if(Arrays.stream(intervals).anyMatch(v->v==rel))c.getStyleClass().add("scale-note");if(midi%12==root)c.getStyleClass().add("root-note");if(midi==active){c.getStyleClass().add("lick-note-active");if(tech!=null)c.getStyleClass().add(switch(tech){case HAMMER_ON->"tech-hammer";case PULL_OFF->"tech-pull";case BEND->"tech-bend";case VIBRATO->"tech-vibrato";case SLIDE->"tech-slide";default->"lick-note-active";});c.setText(NOTES[midi%12]+"\n"+GuitarArticulationEngine.symbol(tech));}else c.setText(NOTES[midi%12]);}}

    private StudioAudioEngine.ToneSettings tone(){return new StudioAudioEngine.ToneSettings((int)twang.getValue(),(int)warmth.getValue(),(int)drive.getValue(),(int)brightness.getValue(),(int)sustain.getValue(),(int)reverb.getValue(),(int)chorus.getValue(),(int)vibrato.getValue(),(int)humanize.getValue());}
    private int root(){return Arrays.asList(NOTES).indexOf(key.getValue());}private boolean isMinor(){String s=scale.getValue();return s.contains("Minor")||s.equals("Blues")||s.equals("Dorian")||s.contains("Phrygian");}private String noteName(int midi){return NOTES[Math.floorMod(midi,12)]+(midi/12-1);}
    private void note(Track t,int ch,int n,int v,int start,int end)throws Exception{add(t,ShortMessage.NOTE_ON,ch,n,v,start);add(t,ShortMessage.NOTE_OFF,ch,n,0,end);}private void add(Track t,int cmd,int ch,int d1,int d2,int tick)throws Exception{ShortMessage m=new ShortMessage();m.setMessage(cmd,ch,d1,d2);t.add(new MidiEvent(m,tick));}
    private Slider knob(int value){Slider s=new Slider(0,127,value);s.getStyleClass().add("amp-knob");return s;}private VBox knobControl(String name,Slider s){Label v=new Label(String.valueOf((int)s.getValue()));s.valueProperty().addListener((o,a,b)->v.setText(String.valueOf(b.intValue())));VBox box=new VBox(3,new Label(name),s,v);box.setAlignment(Pos.CENTER);box.getStyleClass().add("knob-well");return box;}private VBox labeled(String name,Node node){return new VBox(2,new Label(name),node);}private Button button(String name){Button b=new Button(name);b.getStyleClass().add("studio-button");b.setMinWidth(Region.USE_PREF_SIZE);return b;}
    @Override public void stop()throws Exception{if(traceTimeline!=null)traceTimeline.stop();if(continuousTimeline!=null)continuousTimeline.stop();audio.close();}public static void main(String[]args){launch(args);}
}
