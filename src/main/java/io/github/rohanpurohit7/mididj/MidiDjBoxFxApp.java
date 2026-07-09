package io.github.rohanpurohit7.mididj;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * JavaFX modernization of the original Swing MIDI checkbox sequencer.
 * Uses the Java Sound MIDI API and a custom Winamp-inspired skin.
 */
public class MidiDjBoxFxApp extends Application {
    private static final int ROWS = 16;
    private static final int STEPS = 16;
    private static final Path PATTERN_FILE = Path.of("midi-djbox-pattern.txt");

    private record CultureProfile(String name, String[] instrumentNames, int[] midiKeys,
                                  List<ScaleGroove> grooves, int defaultBpm) {
        @Override
        public String toString() {
            return name;
        }
    }

    private record ScaleGroove(String name, int[] scaleDegrees, int[] accents, int[] response,
                               int pulseEvery, int melodicProgram) {
        @Override
        public String toString() {
            return name;
        }
    }

    private final List<CultureProfile> cultureProfiles = createCultureProfiles();
    private final List<CheckBox> pads = new ArrayList<>();
    private final List<Label> instrumentLabels = new ArrayList<>();
    private final List<Rectangle> stepLights = new ArrayList<>();
    private CultureProfile currentProfile = cultureProfiles.get(0);
    private ScaleGroove currentGroove = currentProfile.grooves().get(0);
    private final String[] instrumentNames = currentProfile.instrumentNames().clone();
    private final int[] instruments = currentProfile.midiKeys().clone();

    private Sequencer sequencer;
    private Sequence sequence;
    private Track track;
    private Timeline playheadTimeline;
    private ComboBox<CultureProfile> cultureCombo;
    private ComboBox<ScaleGroove> grooveCombo;
    private Slider tempoSlider;
    private Label statusLabel;
    private Label tempoReadout;
    private int playhead;

    private static List<CultureProfile> createCultureProfiles() {
        return List.of(
                profile("Indian Orchestra",
                        names("Tabla Bayan", "Tabla Dayan", "Dholak", "Mridangam", "Kanjira", "Manjira",
                                "Tanpura Pulse", "Sitar Phrase", "Sarod Phrase", "Bansuri Phrase", "Shehnai Phrase",
                                "Santoor Phrase", "Harmonium Phrase", "Ghatam", "Dhol Accent", "Temple Bell"),
                        keys(41, 37, 38, 45, 39, 42, 56, 60, 64, 67, 70, 72, 75, 47, 43, 81),
                        List.of(
                                groove("Raag Yaman", degrees(0, 2, 4, 6, 7, 9, 11), beats(0, 4, 8, 12), beats(2, 6, 10, 14), 2, 104),
                                groove("Raag Bhairav", degrees(0, 1, 4, 5, 7, 8, 11), beats(0, 6, 8, 14), beats(3, 7, 11, 15), 3, 104),
                                groove("Raag Kafi", degrees(0, 2, 3, 5, 7, 9, 10), beats(0, 4, 10, 12), beats(2, 5, 8, 14), 2, 21)),
                        118),
                profile("Japanese Orchestra",
                        names("Taiko Low", "Taiko High", "Otsuzumi", "Kotsuzumi", "Hyoshigi", "Kane",
                                "Koto Phrase", "Shamisen Phrase", "Shakuhachi Phrase", "Fue Phrase", "Biwa Phrase",
                                "Sho Drone", "Temple Block", "Rim Accent", "Bell Tree", "Kabuki Accent"),
                        keys(41, 45, 47, 50, 76, 81, 60, 62, 65, 67, 70, 72, 77, 37, 81, 39),
                        List.of(
                                groove("In Sen Mode", degrees(0, 1, 5, 7, 10), beats(0, 4, 8, 12), beats(3, 7, 11, 15), 4, 107),
                                groove("Hirajoshi Mode", degrees(0, 2, 3, 7, 8), beats(0, 5, 8, 13), beats(2, 6, 10, 14), 3, 107),
                                groove("Yo Mode", degrees(0, 2, 5, 7, 9), beats(0, 4, 9, 12), beats(1, 6, 10, 15), 2, 77)),
                        112),
                profile("Chinese Orchestra",
                        names("Tanggu", "Bangu", "Paigu", "Bo Cymbal", "Luo Gong", "Woodblock",
                                "Guzheng Phrase", "Erhu Phrase", "Dizi Phrase", "Pipa Phrase", "Yangqin Phrase",
                                "Sheng Phrase", "Suona Phrase", "Temple Bell", "Opera Clap", "Lion Dance Hit"),
                        keys(36, 38, 45, 49, 52, 76, 60, 62, 67, 69, 72, 74, 77, 81, 39, 55),
                        List.of(
                                groove("Gong Pentatonic", degrees(0, 2, 4, 7, 9), beats(0, 4, 8, 12), beats(2, 6, 10, 14), 2, 108),
                                groove("Shang Pentatonic", degrees(0, 2, 5, 7, 10), beats(0, 3, 8, 11), beats(4, 6, 12, 14), 3, 108),
                                groove("Yu Pentatonic", degrees(0, 3, 5, 7, 10), beats(0, 5, 8, 13), beats(2, 7, 10, 15), 2, 72)),
                        116),
                profile("Hungarian Orchestra",
                        names("Tapan Drum", "Side Drum", "Cimbalom Hit", "Tambourine", "Clap", "Triangle",
                                "Cimbalom Phrase", "Violin Phrase", "Clarinet Phrase", "Dulcimer Phrase", "Brass Stab",
                                "Bass Pulse", "Snare Roll", "Foot Stomp", "Accent Cymbal", "Dance Bell"),
                        keys(36, 38, 47, 54, 39, 81, 60, 64, 67, 72, 74, 41, 40, 35, 49, 80),
                        List.of(
                                groove("Hungarian Minor", degrees(0, 2, 3, 6, 7, 8, 11), beats(0, 4, 8, 12), beats(2, 5, 10, 13), 2, 15),
                                groove("Verbunkos Dance", degrees(0, 2, 4, 6, 7, 9, 10), beats(0, 3, 8, 11), beats(4, 7, 12, 15), 3, 41)),
                        132),
                profile("Spanish Orchestra",
                        names("Cajon Bass", "Cajon Slap", "Palmas", "Castanets", "Tambourine", "Heel Tap",
                                "Guitar Phrase", "Nylon Lead", "Flamenco Picado", "Bandurria Phrase", "Trumpet Stab",
                                "Bass Pulse", "Cymbal Roll", "Hand Clap", "Accent Hit", "Jaleo Bell"),
                        keys(36, 38, 39, 75, 54, 37, 60, 64, 67, 69, 72, 41, 49, 39, 45, 81),
                        List.of(
                                groove("Phrygian Dominant", degrees(0, 1, 4, 5, 7, 8, 10), beats(0, 3, 6, 8, 11, 14), beats(2, 5, 9, 12), 2, 24),
                                groove("Flamenco Cadence", degrees(0, 1, 3, 4, 5, 7, 8, 10), beats(0, 4, 7, 10, 12, 15), beats(2, 6, 9, 13), 3, 24)),
                        124),
                profile("English Orchestra",
                        names("Kick Drum", "Snare Drum", "Tabor", "Tambourine", "Handbells", "Triangle",
                                "Fiddle Phrase", "Piano Phrase", "Recorder Phrase", "Concertina Phrase", "Brass Phrase",
                                "Cello Pulse", "Side Stick", "Clap", "Crash", "Morris Bell"),
                        keys(36, 38, 45, 54, 80, 81, 60, 64, 67, 69, 72, 41, 37, 39, 49, 81),
                        List.of(
                                groove("Major Folk", degrees(0, 2, 4, 5, 7, 9, 11), beats(0, 4, 8, 12), beats(2, 6, 10, 14), 2, 41),
                                groove("Dorian Folk", degrees(0, 2, 3, 5, 7, 9, 10), beats(0, 3, 8, 11), beats(4, 6, 12, 14), 3, 41)),
                        120),
                profile("Mexican Orchestra",
                        names("Bombo", "Tarola", "Guitarron Pulse", "Vihuela Chop", "Maracas", "Clap",
                                "Trumpet Phrase", "Violin Phrase", "Guitar Phrase", "Accordion Phrase", "Jarana Phrase",
                                "Requinto Phrase", "Guiro", "Cowbell", "Crash", "Grito Accent"),
                        keys(36, 38, 41, 37, 70, 39, 60, 64, 67, 69, 72, 74, 73, 56, 49, 55),
                        List.of(
                                groove("Son Jarocho", degrees(0, 2, 4, 5, 7, 9, 10), beats(0, 3, 6, 8, 11, 14), beats(2, 5, 9, 12), 2, 25),
                                groove("Ranchera Major", degrees(0, 2, 4, 5, 7, 9, 11), beats(0, 4, 8, 12), beats(3, 7, 11, 15), 2, 57)),
                        126),
                profile("Arabic Orchestra",
                        names("Darbuka Doum", "Darbuka Tek", "Riq", "Bendir", "Frame Drum", "Finger Cymbals",
                                "Oud Phrase", "Qanun Phrase", "Nay Phrase", "Mizmar Phrase", "Rababa Phrase",
                                "Accordion Phrase", "Sagat", "Clap", "Daff Accent", "Pearl Inlay Drum"),
                        keys(36, 38, 54, 45, 47, 80, 60, 64, 67, 70, 72, 74, 81, 39, 43, 41),
                        List.of(
                                groove("Maqam Hijaz", degrees(0, 1, 4, 5, 7, 8, 10), beats(0, 4, 8, 12), beats(2, 6, 10, 14), 2, 105),
                                groove("Maqam Bayati", degrees(0, 1, 3, 5, 7, 8, 10), beats(0, 3, 8, 11), beats(4, 6, 12, 14), 3, 105),
                                groove("Egyptian Darbuka Saidi", degrees(0, 2, 4, 5, 7, 8, 10), beats(0, 3, 7, 8, 11, 15), beats(2, 5, 10, 13), 2, 105),
                                groove("Maqsum Baladi", degrees(0, 2, 3, 5, 7, 9, 10), beats(0, 4, 6, 8, 12, 14), beats(2, 5, 10, 13), 2, 105)),
                        108),
                profile("African Orchestra",
                        names("Djembe Bass", "Djembe Tone", "Djembe Slap", "Dunun", "Talking Drum", "Shekere",
                                "Balafon Phrase", "Kora Phrase", "Mbira Phrase", "Flute Phrase", "Horn Phrase",
                                "Bass Pulse", "Agogo", "Claves", "Shaker", "Call Bell"),
                        keys(36, 38, 39, 41, 47, 70, 60, 64, 67, 69, 72, 43, 67, 75, 82, 56),
                        List.of(
                                groove("Kuku Pentatonic", degrees(0, 2, 5, 7, 10), beats(0, 3, 6, 8, 11, 14), beats(2, 5, 10, 13), 2, 107),
                                groove("Adowa Bell Cycle", degrees(0, 3, 5, 7, 10), beats(0, 2, 5, 7, 10, 12, 15), beats(3, 6, 9, 13), 3, 107),
                                groove("Afrobeat Minor", degrees(0, 2, 3, 5, 7, 10), beats(0, 4, 8, 11, 14), beats(2, 6, 10, 12), 2, 18)),
                        118)
        );
    }

    private static CultureProfile profile(String name, String[] instrumentNames, int[] midiKeys,
                                          List<ScaleGroove> grooves, int defaultBpm) {
        return new CultureProfile(name, instrumentNames, midiKeys, grooves, defaultBpm);
    }

    private static ScaleGroove groove(String name, int[] scaleDegrees, int[] accents, int[] response,
                                      int pulseEvery, int melodicProgram) {
        return new ScaleGroove(name, scaleDegrees, accents, response, pulseEvery, melodicProgram);
    }

    private static String[] names(String... names) {
        return names;
    }

    private static int[] keys(int... keys) {
        return keys;
    }

    private static int[] degrees(int... degrees) {
        return degrees;
    }

    private static int[] beats(int... beats) {
        return beats;
    }

    @Override
    public void start(Stage stage) {
        setupMidi();

        BorderPane shell = new BorderPane();
        shell.getStyleClass().add("player-shell");

        shell.setTop(buildChromeHeader());
        shell.setCenter(buildSequencerPanel());
        shell.setBottom(buildTransport());

        Scene scene = new Scene(shell, 1240, 860);
        URL stylesheet = MidiDjBoxFxApp.class.getResource("/styles/midi-djbox.css");
        if (stylesheet == null) {
            throw new IllegalStateException("Missing /styles/midi-djbox.css. Add src/main/resources to the classpath.");
        }
        scene.getStylesheets().add(stylesheet.toExternalForm());

        stage.setTitle("MIDI DJ Box - Winamp Skin JavaFX Edition");
        stage.setScene(scene);
        stage.setMinWidth(760);
        stage.setMinHeight(560);
        stage.show();
    }

    private VBox buildChromeHeader() {
        Label title = new Label("MIDI DJ BOX");
        title.getStyleClass().add("winamp-title");

        statusLabel = new Label("Ready - build a 16-step groove and press PLAY.");
        statusLabel.getStyleClass().add("lcd-text");

        tempoReadout = new Label("120 BPM");
        tempoReadout.getStyleClass().add("tempo-readout");

        HBox row = new HBox(18, title, statusLabel, tempoReadout);
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(statusLabel, Priority.ALWAYS);

        cultureCombo = new ComboBox<>();
        cultureCombo.getItems().setAll(cultureProfiles);
        cultureCombo.setValue(currentProfile);
        cultureCombo.getStyleClass().add("orchestra-select");
        cultureCombo.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) {
                applyCulture(newValue);
            }
        });

        grooveCombo = new ComboBox<>();
        grooveCombo.getItems().setAll(currentProfile.grooves());
        grooveCombo.setValue(currentGroove);
        grooveCombo.getStyleClass().add("orchestra-select");
        grooveCombo.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) {
                currentGroove = newValue;
                statusLabel.setText(currentProfile.name() + " selected: " + currentGroove.name());
            }
        });

        Label orchestra = new Label("ORCHESTRA");
        orchestra.getStyleClass().add("transport-label");
        Label scale = new Label("SCALE / GROOVE");
        scale.getStyleClass().add("transport-label");
        FlowPane selectorRow = new FlowPane(12, 8, orchestra, cultureCombo, scale, grooveCombo);
        selectorRow.setAlignment(Pos.CENTER_LEFT);

        VBox header = new VBox(12, row, selectorRow);
        header.setPadding(new Insets(18, 22, 14, 22));
        header.getStyleClass().add("chrome-header");
        return header;
    }

    private ScrollPane buildSequencerPanel() {
        GridPane grid = new GridPane();
        grid.setHgap(7);
        grid.setVgap(7);
        grid.getStyleClass().add("sequencer-grid");

        for (int step = 0; step < STEPS; step++) {
            Rectangle light = new Rectangle(31, 8);
            light.getStyleClass().add("step-light");
            stepLights.add(light);
            grid.add(light, step + 1, 0);
        }

        for (int row = 0; row < ROWS; row++) {
            Label instrument = new Label(instrumentNames[row]);
            instrument.getStyleClass().add("instrument-label");
            instrumentLabels.add(instrument);
            grid.add(instrument, 0, row + 1);

            for (int step = 0; step < STEPS; step++) {
                CheckBox pad = new CheckBox();
                pad.getStyleClass().add("beat-pad");
                pads.add(pad);
                grid.add(pad, step + 1, row + 1);
            }
        }

        BorderPane panel = new BorderPane(grid);
        panel.setPadding(new Insets(22));
        panel.getStyleClass().add("main-panel");

        ScrollPane scrollPane = new ScrollPane(panel);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setPannable(true);
        scrollPane.getStyleClass().add("sequencer-scroll");
        return scrollPane;
    }

    private VBox buildTransport() {
        Button play = commandButton("PLAY");
        play.setOnAction(event -> playPattern());

        Button stop = commandButton("STOP");
        stop.setOnAction(event -> stopPattern());

        Button clear = commandButton("CLEAR");
        clear.setOnAction(event -> clearPattern());

        Button save = commandButton("SAVE");
        save.setOnAction(event -> savePattern());

        Button load = commandButton("LOAD");
        load.setOnAction(event -> loadPattern());

        Button demo = commandButton("DEMO GROOVE");
        demo.setOnAction(event -> loadDemoGroove());

        Button aiGroove = commandButton("AI GROOVE");
        aiGroove.setOnAction(event -> buildAiGroove());

        tempoSlider = new Slider(70, 190, 120);
        tempoSlider.getStyleClass().add("tempo-slider");
        tempoSlider.valueProperty().addListener((obs, oldValue, newValue) -> {
            int bpm = newValue.intValue();
            tempoReadout.setText(bpm + " BPM");
            if (sequencer != null) {
                sequencer.setTempoInBPM(bpm);
            }
        });

        Label tempo = new Label("TEMPO");
        tempo.getStyleClass().add("transport-label");

        FlowPane controls = new FlowPane(12, 10, play, stop, clear, save, load, demo, aiGroove);
        controls.setAlignment(Pos.CENTER_LEFT);

        HBox tempoControl = new HBox(12, tempo, tempoSlider);
        tempoControl.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(tempoSlider, Priority.ALWAYS);

        VBox transport = new VBox(12, controls, tempoControl);
        transport.setPadding(new Insets(16, 22, 22, 22));
        transport.getStyleClass().add("transport");
        return transport;
    }

    private void applyCulture(CultureProfile profile) {
        currentProfile = profile;
        currentGroove = profile.grooves().get(0);

        for (int index = 0; index < ROWS; index++) {
            instrumentNames[index] = profile.instrumentNames()[index];
            instruments[index] = profile.midiKeys()[index];
            if (index < instrumentLabels.size()) {
                instrumentLabels.get(index).setText(instrumentNames[index]);
            }
        }

        if (grooveCombo != null) {
            grooveCombo.getItems().setAll(profile.grooves());
            grooveCombo.setValue(currentGroove);
        }
        if (tempoSlider != null) {
            tempoSlider.setValue(profile.defaultBpm());
        }
        statusLabel.setText(profile.name() + " loaded. Pick a scale/groove or press AI GROOVE.");
    }

    private Button commandButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("transport-button");
        return button;
    }

    private void setupMidi() {
        try {
            sequencer = MidiSystem.getSequencer();
            if (sequencer == null) {
                throw new IllegalStateException("No MIDI sequencer available on this system.");
            }
            sequencer.open();
            sequence = new Sequence(Sequence.PPQ, 4);
            track = sequence.createTrack();
            sequencer.setTempoInBPM(120);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to initialize MIDI sequencer", ex);
        }
    }

    private void playPattern() {
        try {
            buildTrack();
            sequencer.setSequence(sequence);
            sequencer.setLoopCount(Sequencer.LOOP_CONTINUOUSLY);
            sequencer.setTempoInBPM((int) tempoSlider.getValue());
            sequencer.start();
            startPlayheadAnimation();
            statusLabel.setText("Playing pattern - Java Sound MIDI sequencer active.");
        } catch (Exception ex) {
            statusLabel.setText("Unable to play pattern: " + ex.getMessage());
        }
    }

    private void stopPattern() {
        if (sequencer != null) {
            sequencer.stop();
        }
        if (playheadTimeline != null) {
            playheadTimeline.stop();
        }
        stepLights.forEach(light -> light.getStyleClass().remove("step-active"));
        statusLabel.setText("Stopped.");
    }

    private void buildTrack() throws InvalidMidiDataException {
        sequence.deleteTrack(track);
        track = sequence.createTrack();
        track.add(makeEvent(ShortMessage.PROGRAM_CHANGE, 0, currentGroove.melodicProgram(), 0, 0));

        for (int row = 0; row < ROWS; row++) {
            int key = instruments[row];
            for (int step = 0; step < STEPS; step++) {
                if (isPadSelected(row, step)) {
                    track.add(makeEvent(ShortMessage.NOTE_ON, 9, key, 100, step));
                    track.add(makeEvent(ShortMessage.NOTE_OFF, 9, key, 100, step + 1));
                    if (row >= 6) {
                        int note = melodyNoteForRow(row);
                        track.add(makeEvent(ShortMessage.NOTE_ON, 0, note, 72, step));
                        track.add(makeEvent(ShortMessage.NOTE_OFF, 0, note, 0, step + 1));
                    }
                }
            }
            track.add(makeEvent(ShortMessage.CONTROL_CHANGE, 1, 127, 0, STEPS));
        }
        track.add(makeEvent(ShortMessage.PROGRAM_CHANGE, 9, 1, 0, STEPS - 1));
    }

    private int melodyNoteForRow(int row) {
        int[] degrees = currentGroove.scaleDegrees();
        int melodicRow = row - 6;
        int degree = degrees[melodicRow % degrees.length];
        int octave = melodicRow / degrees.length;
        return 60 + degree + (octave * 12);
    }

    private MidiEvent makeEvent(int command, int channel, int data1, int data2, int tick) throws InvalidMidiDataException {
        ShortMessage message = new ShortMessage();
        message.setMessage(command, channel, data1, data2);
        return new MidiEvent(message, tick);
    }

    private boolean isPadSelected(int row, int step) {
        return pads.get(row * STEPS + step).isSelected();
    }

    private void setPadSelected(int row, int step, boolean selected) {
        pads.get(row * STEPS + step).setSelected(selected);
    }

    private void startPlayheadAnimation() {
        playhead = 0;
        if (playheadTimeline != null) {
            playheadTimeline.stop();
        }
        playheadTimeline = new Timeline(new KeyFrame(Duration.millis(125), event -> {
            stepLights.forEach(light -> light.getStyleClass().remove("step-active"));
            Rectangle active = stepLights.get(playhead % STEPS);
            active.getStyleClass().add("step-active");
            playhead++;
        }));
        playheadTimeline.setCycleCount(Timeline.INDEFINITE);
        playheadTimeline.play();
    }

    private void clearPattern() {
        pads.forEach(pad -> pad.setSelected(false));
        statusLabel.setText("Pattern cleared.");
    }

    private void buildAiGroove() {
        pads.forEach(pad -> pad.setSelected(false));

        for (int step : currentGroove.accents()) {
            setStep(0, step);
            setStep(15, step);
            setStep(1, step + 1);
        }

        for (int step : currentGroove.response()) {
            setStep(2, step);
            setStep(3, step);
            setStep(4, step + 1);
        }

        for (int step = 0; step < STEPS; step += Math.max(1, currentGroove.pulseEvery())) {
            setStep(5, step);
            setStep(14, step);
        }

        int melodyIndex = 0;
        for (int step = 0; step < STEPS; step++) {
            boolean activeStep = contains(currentGroove.accents(), step)
                    || contains(currentGroove.response(), step)
                    || step % Math.max(1, currentGroove.pulseEvery()) == 0;
            if (activeStep) {
                int degree = currentGroove.scaleDegrees()[melodyIndex % currentGroove.scaleDegrees().length];
                int row = 6 + Math.floorMod(degree + melodyIndex, 10);
                setStep(row, step);
                if (degree % 2 == 0) {
                    setStep(row, step + 1);
                }
                melodyIndex++;
            }
        }

        addCultureSpecificAccents();
        statusLabel.setText("AI groove built: " + currentProfile.name() + " / " + currentGroove.name());
        flashGrid();
    }

    private void addCultureSpecificAccents() {
        String culture = currentProfile.name();
        String groove = currentGroove.name();

        if (culture.contains("Arabic")) {
            if (groove.contains("Saidi")) {
                for (int step : new int[]{0, 3, 7, 8, 11, 15}) {
                    setStep(0, step);
                }
                for (int step : new int[]{2, 5, 10, 13}) {
                    setStep(1, step);
                    setStep(15, step);
                }
            } else if (groove.contains("Maqsum")) {
                for (int step : new int[]{0, 4, 6, 8, 12, 14}) {
                    setStep(0, step);
                }
                for (int step : new int[]{2, 5, 10, 13}) {
                    setStep(1, step);
                    setStep(2, step);
                }
            }
        } else if (culture.contains("Indian")) {
            for (int step : new int[]{0, 4, 8, 12}) {
                setStep(0, step);
                setStep(1, step + 2);
            }
        } else if (culture.contains("African")) {
            for (int step : new int[]{0, 2, 5, 7, 10, 12, 15}) {
                setStep(13, step);
            }
        } else if (culture.contains("Spanish")) {
            for (int step : new int[]{0, 3, 6, 8, 11, 14}) {
                setStep(2, step);
                setStep(3, step + 1);
            }
        }
    }

    private void setStep(int row, int step) {
        if (row >= 0 && row < ROWS && step >= 0 && step < STEPS) {
            setPadSelected(row, step, true);
        }
    }

    private boolean contains(int[] values, int needle) {
        for (int value : values) {
            if (value == needle) {
                return true;
            }
        }
        return false;
    }

    private void savePattern() {
        List<String> rows = new ArrayList<>();
        for (int row = 0; row < ROWS; row++) {
            StringBuilder builder = new StringBuilder();
            for (int step = 0; step < STEPS; step++) {
                builder.append(isPadSelected(row, step) ? '1' : '0');
            }
            rows.add(builder.toString());
        }
        try {
            Files.write(PATTERN_FILE, rows);
            statusLabel.setText("Saved pattern to " + PATTERN_FILE.toAbsolutePath());
        } catch (IOException ex) {
            statusLabel.setText("Save failed: " + ex.getMessage());
        }
    }

    private void loadPattern() {
        try {
            List<String> rows = Files.readAllLines(PATTERN_FILE);
            for (int row = 0; row < Math.min(rows.size(), ROWS); row++) {
                String pattern = rows.get(row).trim();
                for (int step = 0; step < Math.min(pattern.length(), STEPS); step++) {
                    setPadSelected(row, step, pattern.charAt(step) == '1');
                }
            }
            statusLabel.setText("Loaded saved pattern.");
        } catch (IOException ex) {
            statusLabel.setText("Load failed. Save a pattern first.");
        }
    }

    private void loadDemoGroove() {
        clearPattern();
        for (int step : new int[]{0, 4, 8, 12}) {
            setPadSelected(0, step, true);
        }
        for (int step : new int[]{4, 12}) {
            setPadSelected(3, step, true);
        }
        for (int step = 0; step < STEPS; step += 2) {
            setPadSelected(1, step, true);
        }
        for (int step : new int[]{2, 6, 10, 14}) {
            setPadSelected(5, step, true);
        }
        for (int step : new int[]{7, 15}) {
            setPadSelected(11, step, true);
        }
        statusLabel.setText("Demo groove loaded. Press PLAY.");
        flashGrid();
    }

    private void flashGrid() {
        FadeTransition fade = new FadeTransition(Duration.millis(220), stepLights.get(0).getParent());
        fade.setFromValue(0.55);
        fade.setToValue(1.0);
        fade.setCycleCount(2);
        fade.setAutoReverse(true);
        fade.play();
    }

    @Override
    public void stop() {
        if (playheadTimeline != null) {
            playheadTimeline.stop();
        }
        if (sequencer != null) {
            sequencer.stop();
            sequencer.close();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
