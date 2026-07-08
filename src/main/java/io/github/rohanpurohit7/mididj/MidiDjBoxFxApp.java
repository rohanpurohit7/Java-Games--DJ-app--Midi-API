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
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
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

    private final String[] instrumentNames = {
            "Bass Drum", "Closed Hi-Hat", "Open Hi-Hat", "Acoustic Snare", "Crash Cymbal", "Hand Clap",
            "High Tom", "Hi Bongo", "Maracas", "Whistle", "Low Conga", "Cowbell", "Vibraslap",
            "Low-mid Tom", "High Agogo", "Open Hi Conga"
    };

    private final int[] instruments = {35, 42, 46, 38, 49, 39, 50, 60, 70, 72, 64, 56, 58, 47, 67, 63};
    private final List<CheckBox> pads = new ArrayList<>();
    private final List<Rectangle> stepLights = new ArrayList<>();

    private Sequencer sequencer;
    private Sequence sequence;
    private Track track;
    private Timeline playheadTimeline;
    private Slider tempoSlider;
    private Label statusLabel;
    private Label tempoReadout;
    private int playhead;

    @Override
    public void start(Stage stage) {
        setupMidi();

        BorderPane shell = new BorderPane();
        shell.getStyleClass().add("player-shell");

        shell.setTop(buildChromeHeader());
        shell.setCenter(buildSequencerPanel());
        shell.setBottom(buildTransport());

        Scene scene = new Scene(shell, 1180, 780);
        scene.getStylesheets().add(MidiDjBoxFxApp.class.getResource("/styles/midi-djbox.css").toExternalForm());

        stage.setTitle("MIDI DJ Box — Winamp Skin JavaFX Edition");
        stage.setScene(scene);
        stage.setMinWidth(1040);
        stage.setMinHeight(690);
        stage.show();
    }

    private VBox buildChromeHeader() {
        Label title = new Label("MIDI DJ BOX");
        title.getStyleClass().add("winamp-title");

        statusLabel = new Label("Ready — build a 16-step groove and press PLAY.");
        statusLabel.getStyleClass().add("lcd-text");

        tempoReadout = new Label("120 BPM");
        tempoReadout.getStyleClass().add("tempo-readout");

        HBox row = new HBox(18, title, statusLabel, tempoReadout);
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(statusLabel, Priority.ALWAYS);

        VBox header = new VBox(row);
        header.setPadding(new Insets(18, 22, 14, 22));
        header.getStyleClass().add("chrome-header");
        return header;
    }

    private BorderPane buildSequencerPanel() {
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
        return panel;
    }

    private HBox buildTransport() {
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

        HBox transport = new HBox(12, play, stop, clear, save, load, demo, tempo, tempoSlider);
        transport.setAlignment(Pos.CENTER_LEFT);
        transport.setPadding(new Insets(16, 22, 22, 22));
        transport.getStyleClass().add("transport");
        HBox.setHgrow(tempoSlider, Priority.ALWAYS);
        return transport;
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
            statusLabel.setText("Playing pattern — Java Sound MIDI sequencer active.");
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

        for (int row = 0; row < ROWS; row++) {
            int key = instruments[row];
            for (int step = 0; step < STEPS; step++) {
                if (isPadSelected(row, step)) {
                    track.add(makeEvent(ShortMessage.NOTE_ON, 9, key, 100, step));
                    track.add(makeEvent(ShortMessage.NOTE_OFF, 9, key, 100, step + 1));
                }
            }
            track.add(makeEvent(ShortMessage.CONTROL_CHANGE, 1, 127, 0, STEPS));
        }
        track.add(makeEvent(ShortMessage.PROGRAM_CHANGE, 9, 1, 0, STEPS - 1));
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
