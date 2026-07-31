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

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Studio-oriented electric guitar improvisation experience.
 *
 * Uses an external SF2 SoundFont and optional lossless backing stems when
 * available. Generated MIDI remains a fallback for users without an audio
 * library installed.
 */
public final class StudioQualityGuitarApp extends Application {
    private static final String[] NOTES = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
    private static final String[] SCALES = {
            "Minor Pentatonic", "Blues", "Major Pentatonic", "Natural Minor",
            "Dorian", "Mixolydian", "Phrygian Dominant"
    };
    private static final int[] OPEN_STRINGS = {64, 59, 55, 50, 45, 40};
    private static final String[] STRING_NAMES = {"e", "B", "G", "D", "A", "E"};
    private static final int FRETS = 13;

    private final Random random = new Random();
    private final Label[][] fretCells = new Label[6][FRETS];
    private final List<Integer> currentLick = new ArrayList<>();
    private final List<StudioBackingCatalog.Style> styles = StudioBackingCatalog.styles();

    private StudioAudioEngine audio;
    private ComboBox<StudioBackingCatalog.Style> styleCombo;
    private ComboBox<String> keyCombo;
    private ComboBox<String> scaleCombo;
    private Slider tempo;
    private Label status;
    private Label soundbankStatus;
    private Label lickText;
    private Timeline animation;
    private Path backingLibrary;

    private Slider twang;
    private Slider warmth;
    private Slider drive;
    private Slider brightness;
    private Slider sustain;
    private Slider reverb;
    private Slider chorus;
    private Slider vibrato;
    private Slider humanize;

    @Override
    public void start(Stage stage) {
        audio = new StudioAudioEngine();

        BorderPane root = new BorderPane();
        root.getStyleClass().add("guitar-studio");
        root.setTop(buildHeader(stage));
        root.setCenter(buildCenter());
        root.setBottom(buildTransport());

        Scene scene = new Scene(root, 1500, 940);
        var css = StudioQualityGuitarApp.class.getResource("/styles/guitar-improv.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());

        stage.setTitle("Studio Guitar Improvisation Studio");
        stage.setScene(scene);
        stage.setMinWidth(1100);
        stage.setMinHeight(760);
        stage.show();

        refreshScaleOverlay(-1);
    }

    private VBox buildHeader(Stage stage) {
        Label title = new Label("STUDIO GUITAR IMPROVISATION STUDIO");
        title.getStyleClass().add("studio-title");
        status = new Label("Load a quality SF2 guitar soundbank and optional WAV/AIFF backing library for studio mode.");
        status.getStyleClass().add("status-line");
        soundbankStatus = new Label("Soundbank: " + audio.soundbankName());

        styleCombo = new ComboBox<>();
        styleCombo.getItems().setAll(styles);
        styleCombo.setValue(styles.get(0));
        styleCombo.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null && tempo != null) tempo.setValue(newValue.bpm());
        });

        keyCombo = new ComboBox<>();
        keyCombo.getItems().setAll(NOTES);
        keyCombo.setValue("A");
        keyCombo.valueProperty().addListener((obs, oldValue, newValue) -> refreshScaleOverlay(-1));

        scaleCombo = new ComboBox<>();
        scaleCombo.getItems().setAll(SCALES);
        scaleCombo.setValue("Minor Pentatonic");
        scaleCombo.valueProperty().addListener((obs, oldValue, newValue) -> refreshScaleOverlay(-1));

        Button loadSoundFont = command("LOAD GUITAR SF2");
        loadSoundFont.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Choose a licensed guitar SoundFont");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SoundFont 2", "*.sf2"));
            File selected = chooser.showOpenDialog(stage);
            if (selected == null) return;
            try {
                boolean loaded = audio.loadSoundFont(selected.toPath());
                soundbankStatus.setText("Soundbank: " + audio.soundbankName());
                status.setText(loaded ? "Studio SoundFont loaded." : "The selected SoundFont is unsupported by this synthesizer.");
            } catch (Exception ex) {
                status.setText("Unable to load SoundFont: " + ex.getMessage());
            }
        });

        Button chooseBackingLibrary = command("CHOOSE LOSSLESS BACKING LIBRARY");
        chooseBackingLibrary.setOnAction(event -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Choose folder containing WAV/AIFF backing tracks");
            File selected = chooser.showDialog(stage);
            if (selected != null) {
                backingLibrary = selected.toPath();
                status.setText("Lossless backing library: " + backingLibrary);
            }
        });

        FlowPane choices = new FlowPane(10, 8,
                labeled("SOULFUL TRACK", styleCombo),
                labeled("KEY", keyCombo),
                labeled("SCALE", scaleCombo),
                loadSoundFont,
                chooseBackingLibrary,
                soundbankStatus);
        choices.setAlignment(Pos.CENTER_LEFT);

        VBox box = new VBox(9, title, status, choices);
        box.setPadding(new Insets(18));
        return box;
    }

    private HBox buildCenter() {
        VBox fretboard = new VBox(10,
                new Label("ANIMATED FRETBOARD / SCALE OVERLAY"),
                createFretboard(),
                createLickReadout());
        fretboard.setPadding(new Insets(18));
        HBox.setHgrow(fretboard, Priority.ALWAYS);

        VBox toneOverlay = buildToneOverlay();
        toneOverlay.setPrefWidth(370);
        return new HBox(14, fretboard, toneOverlay);
    }

    private GridPane createFretboard() {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("fretboard");
        grid.setHgap(2);
        grid.setVgap(2);
        grid.add(new Label("String"), 0, 0);
        for (int fret = 0; fret < FRETS; fret++) {
            grid.add(new Label(Integer.toString(fret)), fret + 1, 0);
        }
        for (int string = 0; string < 6; string++) {
            grid.add(new Label(STRING_NAMES[string]), 0, string + 1);
            for (int fret = 0; fret < FRETS; fret++) {
                int midi = OPEN_STRINGS[string] + fret;
                Label cell = new Label(NOTES[midi % 12]);
                cell.setAlignment(Pos.CENTER);
                cell.setMinSize(58, 52);
                cell.getStyleClass().add("fret-cell");
                fretCells[string][fret] = cell;
                grid.add(cell, fret + 1, string + 1);
            }
        }
        return grid;
    }

    private VBox createLickReadout() {
        lickText = new Label("Generate a lick to begin.");
        lickText.setWrapText(true);
        lickText.getStyleClass().add("lick-readout");
        return new VBox(4, new Label("CURRENT STUDIO LICK"), lickText);
    }

    private VBox buildToneOverlay() {
        Label title = new Label("GUITAR TONE OVERLAY");
        title.getStyleClass().add("studio-title");
        twang = knob("Twang", 72);
        warmth = knob("Warmth / Body", 82);
        drive = knob("Drive", 45);
        brightness = knob("Brightness", 70);
        sustain = knob("Sustain", 76);
        reverb = knob("Reverb", 42);
        chorus = knob("Chorus", 18);
        vibrato = knob("Vibrato", 20);
        humanize = knob("Human Feel", 68);

        Button apply = command("APPLY TONE");
        apply.setOnAction(event -> {
            audio.applyLeadTone(2, toneSettings());
            status.setText("Tone applied: twang " + (int) twang.getValue() + ", warmth " + (int) warmth.getValue()
                    + ", drive " + (int) drive.getValue() + ".");
        });
        Button clean = command("SOULFUL CLEAN");
        clean.setOnAction(event -> preset(62, 92, 22, 66, 84, 56, 18, 16, 78));
        Button blues = command("TEXAS BLUES");
        blues.setOnAction(event -> preset(98, 64, 62, 88, 72, 38, 8, 24, 74));
        Button ambient = command("AMBIENT LEAD");
        ambient.setOnAction(event -> preset(28, 88, 36, 58, 102, 104, 62, 34, 62));

        VBox box = new VBox(8, title,
                labeledSlider("TWANG", twang), labeledSlider("WARMTH / BODY", warmth),
                labeledSlider("DRIVE", drive), labeledSlider("BRIGHTNESS", brightness),
                labeledSlider("SUSTAIN", sustain), labeledSlider("REVERB", reverb),
                labeledSlider("CHORUS", chorus), labeledSlider("VIBRATO", vibrato),
                labeledSlider("HUMAN FEEL", humanize),
                new FlowPane(8, 8, apply, clean, blues, ambient));
        box.setPadding(new Insets(18));
        box.getStyleClass().add("tone-overlay");
        return box;
    }

    private VBox buildTransport() {
        Button playBacking = command("PLAY STUDIO BACKING");
        playBacking.setOnAction(event -> playBacking());
        Button generate = command("GENERATE SOULFUL LICK");
        generate.setOnAction(event -> generateLick(false));
        Button variation = command("NEW VARIATION");
        variation.setOnAction(event -> generateLick(true));
        Button playLick = command("PLAY / ANIMATE LICK");
        playLick.setOnAction(event -> playLick());
        Button stop = command("STOP ALL");
        stop.setOnAction(event -> stopAll());
        Button legacy = command("OPEN DJ BACKING EDITOR");
        legacy.setOnAction(event -> {
            try { new MidiDjBoxFxApp().start(new Stage()); }
            catch (Exception ex) { status.setText("Unable to open DJ editor: " + ex.getMessage()); }
        });

        tempo = new Slider(55, 180, styles.get(0).bpm());
        tempo.setShowTickLabels(true);
        tempo.setShowTickMarks(true);
        FlowPane controls = new FlowPane(10, 8, playBacking, generate, variation, playLick, stop, legacy);
        HBox tempoRow = new HBox(10, new Label("BPM"), tempo);
        HBox.setHgrow(tempo, Priority.ALWAYS);

        VBox box = new VBox(10, controls, tempoRow);
        box.setPadding(new Insets(14, 18, 20, 18));
        return box;
    }

    private void playBacking() {
        StudioBackingCatalog.Style style = styleCombo.getValue();
        Path stem = style.resolveStem(backingLibrary);
        try {
            if (stem != null) {
                audio.playLosslessBacking(stem, tempo.getValue() / style.bpm(), 0.92);
                status.setText("Playing studio lossless backing: " + style.name());
            } else {
                audio.playSequence(buildGeneratedBacking(style), (float) tempo.getValue(), Sequencer.LOOP_CONTINUOUSLY);
                status.setText(style.name() + " generated fallback playing. Add " + style.stemBaseName()
                        + ".wav for studio-quality backing.");
            }
        } catch (Exception ex) {
            status.setText("Backing playback failed: " + ex.getMessage());
        }
    }

    private Sequence buildGeneratedBacking(StudioBackingCatalog.Style style) throws Exception {
        Sequence sequence = new Sequence(Sequence.PPQ, 16);
        Track track = sequence.createTrack();
        track.add(event(ShortMessage.PROGRAM_CHANGE, 0, style.chordProgram(), 0, 0));
        track.add(event(ShortMessage.PROGRAM_CHANGE, 1, style.bassProgram(), 0, 0));
        int root = 48 + rootPitchClass();
        int bars = Math.max(8, style.progression().length);
        for (int bar = 0; bar < bars; bar++) {
            int start = bar * 64;
            int chordRoot = root + style.progression()[bar % style.progression().length];
            int third = isMinorScale() ? 3 : 4;
            int[] voicing = {chordRoot, chordRoot + third, chordRoot + 7, chordRoot + 10, chordRoot + 14};
            for (int voice = 0; voice < voicing.length; voice++) {
                int delay = voice * 2;
                addNote(track, 0, voicing[voice], 48 + voice * 5, start + delay, start + 58);
            }
            int[] bassPattern = {0, 7, 12, 7, 0, 10, 12, 7};
            for (int beat = 0; beat < bassPattern.length; beat++) {
                int tick = start + beat * 8;
                addNote(track, 1, chordRoot - 12 + bassPattern[beat], 68 + (beat % 3) * 5, tick, tick + 6);
            }
            addSoulDrums(track, start, style.groove());
        }
        return sequence;
    }

    private void addSoulDrums(Track track, int start, int groove) throws Exception {
        for (int step = 0; step < 16; step++) {
            int tick = start + step * 4;
            int swing = (step % 2 == 1) ? 1 + groove % 2 : 0;
            if (step == 0 || step == 8 || (groove % 4 == 0 && step == 11)) addDrum(track, 36, tick + swing, 98);
            if (step == 4 || step == 12) addDrum(track, 38, tick + swing, 88);
            if (step % 2 == 0) addDrum(track, (groove % 3 == 0) ? 42 : 44, tick + swing, 44 + (step % 4) * 4);
            if ((groove % 5 == 0) && (step == 14 || step == 15)) addDrum(track, 40, tick + swing, 54);
        }
    }

    private void generateLick(boolean variation) {
        List<Integer> source = new ArrayList<>(currentLick);
        currentLick.clear();
        if (variation && !source.isEmpty()) {
            currentLick.addAll(GuitarTheory.variation(source, random.nextLong()));
        } else {
            int rootMidi = 57 + rootPitchClass();
            currentLick.addAll(GuitarTheory.generateLick(rootMidi, scaleCombo.getValue(), 10 + random.nextInt(5), random.nextLong()));
        }
        lickText.setText(currentLick.stream().map(this::noteName).reduce((a, b) -> a + "  →  " + b).orElse(""));
        refreshScaleOverlay(-1);
        status.setText((variation ? "Soulful variation" : "New studio lick") + " generated. Human feel is applied during playback.");
    }

    private void playLick() {
        if (currentLick.isEmpty()) generateLick(false);
        try {
            StudioBackingCatalog.Style style = styleCombo.getValue();
            Sequence sequence = new Sequence(Sequence.PPQ, 16);
            Track track = sequence.createTrack();
            track.add(event(ShortMessage.PROGRAM_CHANGE, 2, style.leadProgram(), 0, 0));
            StudioAudioEngine.ToneSettings settings = toneSettings();
            audio.applyLeadTone(2, settings);
            for (int index = 0; index < currentLick.size(); index++) {
                int note = currentLick.get(index);
                int tick = audio.humanizedTick(index * 8, settings, index);
                int velocity = audio.humanizedVelocity(94 + (index % 4) * 3, settings, index);
                int duration = 5 + settings.sustain() / 24;
                addNote(track, 2, note, velocity, tick, tick + duration);
                if (settings.twang() > 85 && index % 3 == 1) {
                    addNote(track, 2, note + 12, Math.max(42, velocity - 30), tick + 1, tick + 4);
                }
            }
            audio.playSequence(sequence, (float) tempo.getValue(), 0);
            animateLick();
        } catch (Exception ex) {
            status.setText("Lick playback failed: " + ex.getMessage());
        }
    }

    private void animateLick() {
        if (animation != null) animation.stop();
        final int[] index = {0};
        double interval = 60000.0 / tempo.getValue() / 2.0;
        animation = new Timeline(new KeyFrame(Duration.millis(interval), event -> {
            if (index[0] >= currentLick.size()) {
                animation.stop();
                refreshScaleOverlay(-1);
                status.setText("Lick complete. Continue improvising over the backing track.");
                return;
            }
            int midi = currentLick.get(index[0]++);
            refreshScaleOverlay(midi);
            status.setText("Play " + noteName(midi));
        }));
        animation.setCycleCount(currentLick.size() + 1);
        animation.play();
    }

    private void refreshScaleOverlay(int activeMidi) {
        if (keyCombo == null || scaleCombo == null) return;
        int root = rootPitchClass();
        int[] intervals = GuitarTheory.intervals(scaleCombo.getValue());
        for (int string = 0; string < 6; string++) {
            for (int fret = 0; fret < FRETS; fret++) {
                Label cell = fretCells[string][fret];
                if (cell == null) continue;
                cell.getStyleClass().removeAll("scale-note", "root-note", "lick-note-active");
                int midi = OPEN_STRINGS[string] + fret;
                int relative = Math.floorMod(midi % 12 - root, 12);
                if (Arrays.stream(intervals).anyMatch(value -> value == relative)) cell.getStyleClass().add("scale-note");
                if (midi % 12 == root) cell.getStyleClass().add("root-note");
                if (midi == activeMidi) cell.getStyleClass().add("lick-note-active");
            }
        }
    }

    private StudioAudioEngine.ToneSettings toneSettings() {
        return new StudioAudioEngine.ToneSettings(
                (int) twang.getValue(), (int) warmth.getValue(), (int) drive.getValue(),
                (int) brightness.getValue(), (int) sustain.getValue(), (int) reverb.getValue(),
                (int) chorus.getValue(), (int) vibrato.getValue(), (int) humanize.getValue());
    }

    private void preset(int a, int b, int c, int d, int e, int f, int g, int h, int i) {
        twang.setValue(a); warmth.setValue(b); drive.setValue(c); brightness.setValue(d);
        sustain.setValue(e); reverb.setValue(f); chorus.setValue(g); vibrato.setValue(h); humanize.setValue(i);
        audio.applyLeadTone(2, toneSettings());
    }

    private int rootPitchClass() { return Arrays.asList(NOTES).indexOf(keyCombo.getValue()); }
    private boolean isMinorScale() {
        String value = scaleCombo.getValue();
        return value.contains("Minor") || value.equals("Blues") || value.equals("Dorian") || value.contains("Phrygian");
    }
    private String noteName(int midi) { return NOTES[Math.floorMod(midi, 12)] + (midi / 12 - 1); }

    private void addNote(Track track, int channel, int note, int velocity, int start, int end) throws Exception {
        track.add(event(ShortMessage.NOTE_ON, channel, Math.max(0, Math.min(127, note)), velocity, start));
        track.add(event(ShortMessage.NOTE_OFF, channel, Math.max(0, Math.min(127, note)), 0, end));
    }

    private void addDrum(Track track, int note, int tick, int velocity) throws Exception {
        addNote(track, 9, note, velocity, tick, tick + 2);
    }

    private MidiEvent event(int command, int channel, int data1, int data2, int tick) throws InvalidMidiDataException {
        ShortMessage message = new ShortMessage();
        message.setMessage(command, channel, data1, data2);
        return new MidiEvent(message, tick);
    }

    private Slider knob(String name, int value) {
        Slider slider = new Slider(0, 127, value);
        slider.setShowTickMarks(true);
        slider.setBlockIncrement(4);
        slider.setAccessibleText(name);
        return slider;
    }

    private VBox labeledSlider(String text, Slider slider) {
        Label value = new Label(Integer.toString((int) slider.getValue()));
        slider.valueProperty().addListener((obs, oldValue, newValue) -> value.setText(Integer.toString(newValue.intValue())));
        HBox row = new HBox(8, slider, value);
        HBox.setHgrow(slider, Priority.ALWAYS);
        return new VBox(2, new Label(text), row);
    }

    private VBox labeled(String text, javafx.scene.Node node) { return new VBox(3, new Label(text), node); }
    private Button command(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("studio-button");
        return button;
    }

    private void stopAll() {
        if (animation != null) animation.stop();
        audio.stopAll();
        refreshScaleOverlay(-1);
        status.setText("Stopped.");
    }

    @Override
    public void stop() throws Exception {
        stopAll();
        audio.close();
    }

    public static void main(String[] args) { launch(args); }
}
