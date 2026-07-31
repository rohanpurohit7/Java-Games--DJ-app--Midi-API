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
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Electric-guitar improvisation trainer backed by a generated MIDI orchestra.
 * The existing DJ groove engine remains available as a legacy backing editor.
 */
public final class GuitarImprovisationStudioApp extends Application {
    private static final int STRINGS = 6;
    private static final int FRETS = 13;
    private static final int[] OPEN_STRING_MIDI = {64, 59, 55, 50, 45, 40}; // E B G D A E
    private static final String[] STRING_NAMES = {"e", "B", "G", "D", "A", "E"};
    private static final String[] NOTE_NAMES = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
    private static final Path LICK_LIBRARY = Path.of("saved-guitar-licks.txt");

    private record ScaleChoice(String name, int[] intervals) {
        @Override public String toString() { return name; }
    }

    private record BackingStyle(String name, int program, int bassProgram, int[] chordDegrees, int drumPattern) {
        @Override public String toString() { return name; }
    }

    private record GuitarNote(int stringIndex, int fret, int midi, int step, int duration) {
        String display() {
            return STRING_NAMES[stringIndex] + fret + " (" + NOTE_NAMES[midi % 12] + ")";
        }
    }

    private final List<ScaleChoice> scales = List.of(
            new ScaleChoice("Minor Pentatonic", new int[]{0, 3, 5, 7, 10}),
            new ScaleChoice("Blues", new int[]{0, 3, 5, 6, 7, 10}),
            new ScaleChoice("Major Pentatonic", new int[]{0, 2, 4, 7, 9}),
            new ScaleChoice("Natural Minor", new int[]{0, 2, 3, 5, 7, 8, 10}),
            new ScaleChoice("Dorian", new int[]{0, 2, 3, 5, 7, 9, 10}),
            new ScaleChoice("Mixolydian", new int[]{0, 2, 4, 5, 7, 9, 10}),
            new ScaleChoice("Phrygian Dominant", new int[]{0, 1, 4, 5, 7, 8, 10})
    );

    private final List<BackingStyle> backingStyles = List.of(
            new BackingStyle("Blues Rock Band", 29, 33, new int[]{0, 5, 7}, 0),
            new BackingStyle("Classic Rock Orchestra", 30, 34, new int[]{0, 8, 5, 7}, 1),
            new BackingStyle("Funk Jam", 27, 36, new int[]{0, 5, 7, 5}, 2),
            new BackingStyle("Ambient Minor", 89, 38, new int[]{0, 8, 3, 10}, 3),
            new BackingStyle("Spanish Fusion", 24, 32, new int[]{0, 1, 10, 8}, 4),
            new BackingStyle("World Orchestra", 48, 43, new int[]{0, 5, 3, 7}, 5)
    );

    private final Random random = new Random();
    private final Label[][] fretCells = new Label[STRINGS][FRETS];
    private final List<GuitarNote> currentLick = new ArrayList<>();
    private final ListView<String> lickList = new ListView<>();

    private ComboBox<String> keyCombo;
    private ComboBox<ScaleChoice> scaleCombo;
    private ComboBox<BackingStyle> backingCombo;
    private Slider tempoSlider;
    private Label status;
    private Label lickReadout;
    private Sequencer backingSequencer;
    private Sequencer lickSequencer;
    private Timeline lickAnimation;

    @Override
    public void start(Stage stage) {
        setupMidi();

        BorderPane root = new BorderPane();
        root.getStyleClass().add("guitar-studio");
        root.setTop(buildHeader());
        root.setCenter(buildMainArea());
        root.setBottom(buildTransport());

        Scene scene = new Scene(root, 1360, 900);
        var css = GuitarImprovisationStudioApp.class.getResource("/styles/guitar-improv.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());

        stage.setTitle("Electric Guitar Improvisation Studio");
        stage.setScene(scene);
        stage.setMinWidth(980);
        stage.setMinHeight(700);
        stage.show();

        updateScaleOverlay();
        loadSavedLicks();
    }

    private VBox buildHeader() {
        Label title = new Label("ELECTRIC GUITAR IMPROVISATION STUDIO");
        title.getStyleClass().add("studio-title");
        status = new Label("Choose a backing style, key and scale. Generate a lick or improv freely.");
        status.getStyleClass().add("status-line");

        keyCombo = new ComboBox<>();
        keyCombo.getItems().addAll(NOTE_NAMES);
        keyCombo.setValue("A");
        keyCombo.valueProperty().addListener((o, a, b) -> updateScaleOverlay());

        scaleCombo = new ComboBox<>();
        scaleCombo.getItems().addAll(scales);
        scaleCombo.setValue(scales.get(0));
        scaleCombo.valueProperty().addListener((o, a, b) -> updateScaleOverlay());

        backingCombo = new ComboBox<>();
        backingCombo.getItems().addAll(backingStyles);
        backingCombo.setValue(backingStyles.get(0));

        HBox selectors = new HBox(12,
                labeled("BACKING BAND", backingCombo),
                labeled("KEY", keyCombo),
                labeled("SCALE", scaleCombo));
        selectors.setAlignment(Pos.CENTER_LEFT);

        VBox header = new VBox(10, title, status, selectors);
        header.setPadding(new Insets(18));
        return header;
    }

    private VBox labeled(String text, javafx.scene.Node node) {
        Label label = new Label(text);
        label.getStyleClass().add("field-label");
        return new VBox(4, label, node);
    }

    private HBox buildMainArea() {
        VBox fretboardPanel = new VBox(12, new Label("ANIMATED SCALE & LICK FRETBOARD"), buildFretboard(), buildLickReadout());
        fretboardPanel.setPadding(new Insets(18));
        HBox.setHgrow(fretboardPanel, Priority.ALWAYS);

        VBox library = new VBox(10, new Label("SAVED LICKS"), lickList);
        library.setPadding(new Insets(18));
        library.setPrefWidth(330);
        lickList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) loadSelectedLick();
        });

        return new HBox(14, fretboardPanel, library);
    }

    private GridPane buildFretboard() {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("fretboard");
        grid.setHgap(2);
        grid.setVgap(2);

        grid.add(new Label("String"), 0, 0);
        for (int fret = 0; fret < FRETS; fret++) {
            Label fretLabel = new Label(Integer.toString(fret));
            fretLabel.getStyleClass().add("fret-number");
            grid.add(fretLabel, fret + 1, 0);
        }

        for (int string = 0; string < STRINGS; string++) {
            Label name = new Label(STRING_NAMES[string]);
            name.getStyleClass().add("string-name");
            grid.add(name, 0, string + 1);
            for (int fret = 0; fret < FRETS; fret++) {
                int midi = OPEN_STRING_MIDI[string] + fret;
                Label cell = new Label(NOTE_NAMES[midi % 12]);
                cell.setMinSize(58, 52);
                cell.setAlignment(Pos.CENTER);
                cell.getStyleClass().add("fret-cell");
                fretCells[string][fret] = cell;
                grid.add(cell, fret + 1, string + 1);
            }
        }
        return grid;
    }

    private VBox buildLickReadout() {
        lickReadout = new Label("No generated lick yet.");
        lickReadout.setWrapText(true);
        lickReadout.getStyleClass().add("lick-readout");
        return new VBox(6, new Label("CURRENT LICK"), lickReadout);
    }

    private VBox buildTransport() {
        Button playBacking = button("PLAY BACKING");
        playBacking.setOnAction(e -> playBacking());
        Button stop = button("STOP ALL");
        stop.setOnAction(e -> stopAll());
        Button generate = button("GENERATE NEW LICK");
        generate.setOnAction(e -> generateLick(false));
        Button variation = button("GENERATE VARIATION");
        variation.setOnAction(e -> generateLick(true));
        Button playLick = button("PLAY / ANIMATE LICK");
        playLick.setOnAction(e -> playCurrentLick());
        Button saveLick = button("SAVE LICK");
        saveLick.setOnAction(e -> saveCurrentLick());
        Button loadLick = button("LOAD SELECTED");
        loadLick.setOnAction(e -> loadSelectedLick());
        Button legacy = button("OPEN LEGACY DJ BOX");
        legacy.setOnAction(e -> {
            try { new MidiDjBoxFxApp().start(new Stage()); }
            catch (Exception ex) { status.setText("Unable to open legacy DJ box: " + ex.getMessage()); }
        });

        tempoSlider = new Slider(60, 180, 105);
        tempoSlider.setShowTickLabels(true);
        tempoSlider.setShowTickMarks(true);

        HBox buttons = new HBox(9, playBacking, stop, generate, variation, playLick, saveLick, loadLick, legacy);
        buttons.setAlignment(Pos.CENTER_LEFT);
        HBox tempo = new HBox(10, new Label("BPM"), tempoSlider);
        tempo.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(tempoSlider, Priority.ALWAYS);

        VBox box = new VBox(10, buttons, tempo);
        box.setPadding(new Insets(16, 18, 20, 18));
        return box;
    }

    private Button button(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("studio-button");
        return button;
    }

    private void setupMidi() {
        try {
            backingSequencer = MidiSystem.getSequencer();
            lickSequencer = MidiSystem.getSequencer();
            if (backingSequencer == null || lickSequencer == null) throw new IllegalStateException("MIDI sequencer unavailable");
            backingSequencer.open();
            lickSequencer.open();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to initialize MIDI", ex);
        }
    }

    private int rootPitchClass() {
        return Arrays.asList(NOTE_NAMES).indexOf(keyCombo.getValue());
    }

    private boolean inScale(int midi) {
        int relative = Math.floorMod((midi % 12) - rootPitchClass(), 12);
        return Arrays.stream(scaleCombo.getValue().intervals()).anyMatch(i -> i == relative);
    }

    private void updateScaleOverlay() {
        if (keyCombo == null || scaleCombo == null) return;
        for (int s = 0; s < STRINGS; s++) {
            for (int f = 0; f < FRETS; f++) {
                Label cell = fretCells[s][f];
                if (cell == null) continue;
                cell.getStyleClass().removeAll("scale-note", "root-note", "lick-note-active");
                int midi = OPEN_STRING_MIDI[s] + f;
                if (inScale(midi)) cell.getStyleClass().add("scale-note");
                if (midi % 12 == rootPitchClass()) cell.getStyleClass().add("root-note");
            }
        }
        status.setText(keyCombo.getValue() + " " + scaleCombo.getValue().name() + " overlay ready for improvisation.");
    }

    private void generateLick(boolean variation) {
        List<GuitarNote> previous = new ArrayList<>(currentLick);
        currentLick.clear();
        int noteCount = 8 + random.nextInt(5);
        int previousMidi = 57 + rootPitchClass();

        for (int step = 0; step < noteCount; step++) {
            GuitarNote note;
            if (variation && !previous.isEmpty() && random.nextDouble() < 0.55) {
                GuitarNote seed = previous.get(step % previous.size());
                int shifted = seed.midi() + (random.nextBoolean() ? 0 : (random.nextBoolean() ? 12 : -12));
                note = nearestPlayableScaleNote(shifted, step * 2, random.nextBoolean() ? 1 : 2);
            } else {
                int direction = random.nextInt(7) - 3;
                note = nearestPlayableScaleNote(previousMidi + direction, step * 2, random.nextBoolean() ? 1 : 2);
            }
            currentLick.add(note);
            previousMidi = note.midi();
        }
        lickReadout.setText(currentLick.stream().map(GuitarNote::display).reduce((a, b) -> a + "  →  " + b).orElse(""));
        status.setText((variation ? "Variation" : "New lick") + " generated in " + keyCombo.getValue() + " " + scaleCombo.getValue().name() + ".");
        showLickStaticOverlay();
    }

    private GuitarNote nearestPlayableScaleNote(int targetMidi, int step, int duration) {
        GuitarNote best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (int s = 0; s < STRINGS; s++) {
            for (int f = 0; f < FRETS; f++) {
                int midi = OPEN_STRING_MIDI[s] + f;
                if (!inScale(midi)) continue;
                int distance = Math.abs(midi - targetMidi) + (f > 9 ? 2 : 0);
                if (distance < bestDistance) {
                    best = new GuitarNote(s, f, midi, step, duration);
                    bestDistance = distance;
                }
            }
        }
        if (best == null) throw new IllegalStateException("No playable scale note found");
        return best;
    }

    private void showLickStaticOverlay() {
        updateScaleOverlay();
        for (GuitarNote note : currentLick) {
            fretCells[note.stringIndex()][note.fret()].getStyleClass().add("lick-note");
        }
    }

    private void playBacking() {
        try {
            Sequence sequence = new Sequence(Sequence.PPQ, 4);
            Track track = sequence.createTrack();
            BackingStyle style = backingCombo.getValue();
            int root = 48 + rootPitchClass();
            track.add(event(ShortMessage.PROGRAM_CHANGE, 0, style.program(), 0, 0));
            track.add(event(ShortMessage.PROGRAM_CHANGE, 1, style.bassProgram(), 0, 0));

            for (int bar = 0; bar < 4; bar++) {
                int degree = style.chordDegrees()[bar % style.chordDegrees().length];
                int chordRoot = root + degree;
                int start = bar * 16;
                int[] chord = {chordRoot, chordRoot + 7, chordRoot + (scaleCombo.getValue().name().contains("Minor") ? 15 : 16)};
                for (int note : chord) {
                    track.add(event(ShortMessage.NOTE_ON, 0, note, 56, start));
                    track.add(event(ShortMessage.NOTE_OFF, 0, note, 0, start + 15));
                }
                for (int beat = 0; beat < 16; beat += 4) {
                    track.add(event(ShortMessage.NOTE_ON, 1, chordRoot - 12, 76, start + beat));
                    track.add(event(ShortMessage.NOTE_OFF, 1, chordRoot - 12, 0, start + beat + 3));
                }
                addDrums(track, start, style.drumPattern());
            }
            backingSequencer.setSequence(sequence);
            backingSequencer.setLoopCount(Sequencer.LOOP_CONTINUOUSLY);
            backingSequencer.setTempoInBPM((float) tempoSlider.getValue());
            backingSequencer.start();
            status.setText(style.name() + " backing band playing. Improvise using the highlighted scale notes.");
        } catch (Exception ex) {
            status.setText("Backing track error: " + ex.getMessage());
        }
    }

    private void addDrums(Track track, int start, int pattern) throws InvalidMidiDataException {
        for (int step = 0; step < 16; step++) {
            if (step % 4 == 0) addDrum(track, 36, start + step, 96);
            if (step % 8 == 4) addDrum(track, 38, start + step, 90);
            if (step % 2 == 0) addDrum(track, pattern == 2 ? 42 : 44, start + step, 52);
            if ((pattern == 0 || pattern == 1) && (step == 6 || step == 14)) addDrum(track, 38, start + step, 70);
        }
    }

    private void addDrum(Track track, int note, int tick, int velocity) throws InvalidMidiDataException {
        track.add(event(ShortMessage.NOTE_ON, 9, note, velocity, tick));
        track.add(event(ShortMessage.NOTE_OFF, 9, note, 0, tick + 1));
    }

    private void playCurrentLick() {
        if (currentLick.isEmpty()) generateLick(false);
        try {
            Sequence sequence = new Sequence(Sequence.PPQ, 4);
            Track track = sequence.createTrack();
            track.add(event(ShortMessage.PROGRAM_CHANGE, 2, 29, 0, 0));
            for (GuitarNote note : currentLick) {
                track.add(event(ShortMessage.NOTE_ON, 2, note.midi(), 108, note.step()));
                track.add(event(ShortMessage.NOTE_OFF, 2, note.midi(), 0, note.step() + note.duration()));
            }
            lickSequencer.setSequence(sequence);
            lickSequencer.setTempoInBPM((float) tempoSlider.getValue());
            lickSequencer.start();
            animateLick();
        } catch (Exception ex) {
            status.setText("Lick playback error: " + ex.getMessage());
        }
    }

    private void animateLick() {
        if (lickAnimation != null) lickAnimation.stop();
        updateScaleOverlay();
        final int[] index = {0};
        double millisPerEighth = 60000.0 / tempoSlider.getValue() / 2.0;
        lickAnimation = new Timeline(new KeyFrame(Duration.millis(millisPerEighth), e -> {
            for (int s = 0; s < STRINGS; s++) for (int f = 0; f < FRETS; f++)
                fretCells[s][f].getStyleClass().remove("lick-note-active");
            if (index[0] < currentLick.size()) {
                GuitarNote note = currentLick.get(index[0]++);
                fretCells[note.stringIndex()][note.fret()].getStyleClass().add("lick-note-active");
                status.setText("Play: " + note.display());
            } else {
                lickAnimation.stop();
                showLickStaticOverlay();
                status.setText("Lick complete. Continue improvising over the backing band.");
            }
        }));
        lickAnimation.setCycleCount(currentLick.size() + 1);
        lickAnimation.play();
    }

    private void saveCurrentLick() {
        if (currentLick.isEmpty()) {
            status.setText("Generate a lick before saving.");
            return;
        }
        String name = keyCombo.getValue() + " " + scaleCombo.getValue().name() + " - " + System.currentTimeMillis();
        String notes = currentLick.stream()
                .map(n -> n.stringIndex() + ":" + n.fret() + ":" + n.midi() + ":" + n.step() + ":" + n.duration())
                .reduce((a, b) -> a + "," + b).orElse("");
        String line = name + "|" + keyCombo.getValue() + "|" + scaleCombo.getValue().name() + "|" + notes + System.lineSeparator();
        try {
            Files.writeString(LICK_LIBRARY, line, StandardCharsets.UTF_8,
                    Files.exists(LICK_LIBRARY) ? java.nio.file.StandardOpenOption.APPEND : java.nio.file.StandardOpenOption.CREATE);
            loadSavedLicks();
            status.setText("Saved lick: " + name);
        } catch (IOException ex) {
            status.setText("Unable to save lick: " + ex.getMessage());
        }
    }

    private void loadSavedLicks() {
        lickList.getItems().clear();
        if (!Files.exists(LICK_LIBRARY)) return;
        try {
            Files.readAllLines(LICK_LIBRARY, StandardCharsets.UTF_8).stream()
                    .filter(line -> !line.isBlank()).forEach(lickList.getItems()::add);
        } catch (IOException ex) {
            status.setText("Unable to read saved licks: " + ex.getMessage());
        }
    }

    private void loadSelectedLick() {
        String line = lickList.getSelectionModel().getSelectedItem();
        if (line == null) {
            status.setText("Select a saved lick first.");
            return;
        }
        try {
            String[] sections = line.split("\\|", 4);
            keyCombo.setValue(sections[1]);
            scales.stream().filter(s -> s.name().equals(sections[2])).findFirst().ifPresent(scaleCombo::setValue);
            currentLick.clear();
            for (String token : sections[3].split(",")) {
                String[] fields = token.split(":");
                currentLick.add(new GuitarNote(
                        Integer.parseInt(fields[0]), Integer.parseInt(fields[1]), Integer.parseInt(fields[2]),
                        Integer.parseInt(fields[3]), Integer.parseInt(fields[4])));
            }
            lickReadout.setText(currentLick.stream().map(GuitarNote::display).reduce((a, b) -> a + "  →  " + b).orElse(""));
            showLickStaticOverlay();
            status.setText("Loaded saved lick: " + sections[0]);
        } catch (RuntimeException ex) {
            status.setText("Saved lick is malformed: " + ex.getMessage());
        }
    }

    private MidiEvent event(int command, int channel, int data1, int data2, int tick) throws InvalidMidiDataException {
        ShortMessage message = new ShortMessage();
        message.setMessage(command, channel, data1, data2);
        return new MidiEvent(message, tick);
    }

    private void stopAll() {
        if (backingSequencer != null) backingSequencer.stop();
        if (lickSequencer != null) lickSequencer.stop();
        if (lickAnimation != null) lickAnimation.stop();
        updateScaleOverlay();
        status.setText("Stopped. Scale overlay remains available for free improvisation.");
    }

    @Override
    public void stop() {
        stopAll();
        if (backingSequencer != null) backingSequencer.close();
        if (lickSequencer != null) lickSequencer.close();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
