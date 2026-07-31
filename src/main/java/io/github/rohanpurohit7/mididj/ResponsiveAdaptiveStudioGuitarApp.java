package io.github.rohanpurohit7.mididj;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
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

/** Fully responsive guitar improvisation studio with all controls preserved. */
public final class ResponsiveAdaptiveStudioGuitarApp extends Application {
    private static final String[] NOTES = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
    private static final String[] SCALES = {"Minor Pentatonic", "Blues", "Major Pentatonic", "Natural Minor", "Dorian", "Mixolydian", "Phrygian Dominant"};
    private static final int[] OPEN_STRINGS = {64, 59, 55, 50, 45, 40};
    private static final String[] STRING_NAMES = {"e", "B", "G", "D", "A", "E"};
    private static final Path LICK_FILE = Path.of("saved-guitar-licks.txt");

    private final List<StudioBackingCatalog.Style> styles = StudioBackingCatalog.styles();
    private final List<Integer> currentLick = new ArrayList<>();
    private final Label[][] fretCells = new Label[6][13];
    private final Random random = new Random();
    private final ListView<String> savedLicks = new ListView<>();

    private StudioAudioEngine audio;
    private ComboBox<StudioBackingCatalog.Style> styleCombo;
    private ComboBox<String> keyCombo;
    private ComboBox<String> scaleCombo;
    private Slider tempo;
    private Slider twang, warmth, drive, brightness, sustain, reverb, chorus, vibrato, humanize;
    private Label status, recommendation, pickup, lickText, soundbank;
    private GuitarPhotoCard guitarPhoto;
    private Path backingLibrary;
    private Timeline animation;
    private VBox contentColumn;
    private HBox wideCenter;
    private VBox narrowCenter;
    private Node fretboardPanel;
    private Node advisorPanel;

    @Override
    public void start(Stage stage) {
        audio = new StudioAudioEngine();

        BorderPane shell = new BorderPane();
        shell.getStyleClass().add("guitar-studio");
        shell.setTop(buildHeader(stage));
        shell.setCenter(buildScrollableCenter());
        shell.setBottom(buildAlwaysVisibleTransport());

        Scene scene = new Scene(shell, 1500, 940);
        var css = getClass().getResource("/styles/guitar-improv.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());
        stage.setTitle("AI Studio Guitar Improvisation");
        stage.setScene(scene);
        stage.setMinWidth(760);
        stage.setMinHeight(600);

        scene.widthProperty().addListener((obs, oldWidth, newWidth) -> updateResponsiveLayout(newWidth.doubleValue()));
        stage.show();
        updateResponsiveLayout(scene.getWidth());
        refreshFretboard(-1);
        loadSavedLicks();
        applySuggestion();
    }

    private VBox buildHeader(Stage stage) {
        Label title = new Label("AI STUDIO GUITAR IMPROVISATION");
        title.getStyleClass().add("studio-title");
        status = new Label("All controls are available below. Resize the window—the interface will reflow automatically.");
        status.getStyleClass().add("status-line");
        status.setWrapText(true);
        soundbank = new Label("Soundbank: " + audio.soundbankName());
        soundbank.setWrapText(true);

        styleCombo = new ComboBox<>();
        styleCombo.getItems().setAll(styles);
        styleCombo.setValue(styles.get(0));
        styleCombo.setMaxWidth(Double.MAX_VALUE);

        keyCombo = new ComboBox<>();
        keyCombo.getItems().setAll(NOTES);
        keyCombo.setValue("A");

        scaleCombo = new ComboBox<>();
        scaleCombo.getItems().setAll(SCALES);
        scaleCombo.setValue("Minor Pentatonic");

        styleCombo.valueProperty().addListener((o, a, b) -> {
            if (b != null && tempo != null) tempo.setValue(b.bpm());
        });
        keyCombo.valueProperty().addListener((o, a, b) -> refreshFretboard(-1));
        scaleCombo.valueProperty().addListener((o, a, b) -> refreshFretboard(-1));

        Button suggest = button("AI SUGGEST GUITAR + SCALE");
        suggest.setOnAction(e -> applySuggestion());

        Button loadSf2 = button("LOAD GUITAR SF2");
        loadSf2.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Choose a licensed guitar SoundFont");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SoundFont 2", "*.sf2"));
            File selected = chooser.showOpenDialog(stage);
            if (selected == null) return;
            try {
                audio.loadSoundFont(selected.toPath());
                soundbank.setText("Soundbank: " + audio.soundbankName());
                status.setText("Guitar SoundFont loaded.");
            } catch (Exception ex) {
                status.setText("SoundFont error: " + ex.getMessage());
            }
        });

        Button backing = button("CHOOSE WAV/AIFF LIBRARY");
        backing.setOnAction(e -> {
            File selected = new DirectoryChooser().showDialog(stage);
            if (selected != null) {
                backingLibrary = selected.toPath();
                status.setText("Lossless backing library: " + backingLibrary);
            }
        });

        FlowPane selectors = new FlowPane(10, 8,
                labeled("TRACK", styleCombo), labeled("KEY", keyCombo), labeled("SCALE", scaleCombo),
                suggest, loadSf2, backing, soundbank);
        selectors.setAlignment(Pos.CENTER_LEFT);
        selectors.setPrefWrapLength(1200);

        VBox header = new VBox(8, title, status, selectors);
        header.setPadding(new Insets(14, 18, 10, 18));
        return header;
    }

    private ScrollPane buildScrollableCenter() {
        fretboardPanel = buildFretboardPanel();
        advisorPanel = buildAdvisorPanel();
        wideCenter = new HBox(12, fretboardPanel, advisorPanel);
        HBox.setHgrow(fretboardPanel, Priority.ALWAYS);
        narrowCenter = new VBox(12, fretboardPanel, advisorPanel);

        contentColumn = new VBox(wideCenter);
        contentColumn.setFillWidth(true);
        contentColumn.setPadding(new Insets(4, 12, 8, 12));

        ScrollPane scroll = new ScrollPane(contentColumn);
        scroll.setFitToWidth(true);
        scroll.setPannable(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        return scroll;
    }

    private VBox buildFretboardPanel() {
        GridPane board = new GridPane();
        board.getStyleClass().add("fretboard");
        board.setHgap(2);
        board.setVgap(2);
        board.add(new Label("String"), 0, 0);
        for (int fret = 0; fret < 13; fret++) board.add(new Label(Integer.toString(fret)), fret + 1, 0);
        for (int string = 0; string < 6; string++) {
            board.add(new Label(STRING_NAMES[string]), 0, string + 1);
            for (int fret = 0; fret < 13; fret++) {
                int midi = OPEN_STRINGS[string] + fret;
                Label cell = new Label(NOTES[midi % 12]);
                cell.setMinSize(48, 44);
                cell.setPrefSize(58, 50);
                cell.setAlignment(Pos.CENTER);
                cell.getStyleClass().add("fret-cell");
                fretCells[string][fret] = cell;
                board.add(cell, fret + 1, string + 1);
            }
        }

        ScrollPane boardScroll = new ScrollPane(board);
        boardScroll.setFitToHeight(true);
        boardScroll.setPannable(true);
        boardScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        boardScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        boardScroll.setMinHeight(390);

        lickText = new Label("Generate a lick to begin.");
        lickText.setWrapText(true);
        lickText.getStyleClass().add("lick-readout");

        VBox panel = new VBox(10,
                new Label("ANIMATED SCALE / LICK FRETBOARD"), boardScroll,
                new Label("CURRENT LICK"), lickText,
                new Label("SAVED LICKS — double-click to load"), savedLicks);
        panel.setPadding(new Insets(12));
        panel.setMinWidth(0);
        savedLicks.setPrefHeight(130);
        savedLicks.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) loadSelectedLick();
        });
        return panel;
    }

    private VBox buildAdvisorPanel() {
        guitarPhoto = new GuitarPhotoCard();
        recommendation = new Label();
        recommendation.setWrapText(true);
        pickup = new Label();
        pickup.setWrapText(true);

        VBox panel = new VBox(10,
                new Label("AI GUITAR ADVISOR"), guitarPhoto, pickup, recommendation,
                new Separator(Orientation.HORIZONTAL), buildToneOverlay());
        panel.setPadding(new Insets(12));
        panel.setPrefWidth(410);
        panel.setMinWidth(300);
        return panel;
    }

    private VBox buildToneOverlay() {
        twang = knob(70); warmth = knob(80); drive = knob(35); brightness = knob(70); sustain = knob(80);
        reverb = knob(48); chorus = knob(16); vibrato = knob(18); humanize = knob(80);
        Button apply = button("APPLY MANUAL TONE");
        apply.setOnAction(e -> {
            audio.applyLeadTone(2, tone());
            status.setText("Manual tone settings applied.");
        });
        return new VBox(5, new Label("AUTO-TUNER / TONE OVERLAY"),
                slider("TWANG", twang), slider("WARMTH", warmth), slider("DRIVE", drive),
                slider("BRIGHTNESS", brightness), slider("SUSTAIN", sustain), slider("REVERB", reverb),
                slider("CHORUS", chorus), slider("VIBRATO", vibrato), slider("HUMAN FEEL", humanize), apply);
    }

    private VBox buildAlwaysVisibleTransport() {
        Button play = button("PLAY STUDIO BACKING"); play.setOnAction(e -> playBacking());
        Button stop = button("STOP ALL"); stop.setOnAction(e -> stopAll());
        Button generate = button("GENERATE NEW LICK"); generate.setOnAction(e -> generate(false));
        Button variation = button("GENERATE VARIATION"); variation.setOnAction(e -> generate(true));
        Button animate = button("PLAY / ANIMATE LICK"); animate.setOnAction(e -> playLick());
        Button save = button("SAVE LICK"); save.setOnAction(e -> saveLick());
        Button load = button("LOAD SELECTED LICK"); load.setOnAction(e -> loadSelectedLick());
        Button ai = button("AI SUGGEST"); ai.setOnAction(e -> applySuggestion());
        Button dj = button("OPEN DJ BACKING EDITOR");
        dj.setOnAction(e -> {
            try { new MidiDjBoxFxApp().start(new Stage()); }
            catch (Exception ex) { status.setText("DJ editor error: " + ex.getMessage()); }
        });
        Button fullStudio = button("OPEN FULL STUDIO");
        fullStudio.setOnAction(e -> {
            try { new StudioQualityGuitarApp().start(new Stage()); }
            catch (Exception ex) { status.setText("Studio error: " + ex.getMessage()); }
        });

        FlowPane primary = new FlowPane(8, 8, play, stop, generate, variation, animate, save, load);
        FlowPane secondary = new FlowPane(8, 8, ai, dj, fullStudio);
        primary.setPrefWrapLength(1200);
        secondary.setPrefWrapLength(900);

        tempo = new Slider(55, 180, styles.get(0).bpm());
        tempo.setShowTickLabels(true);
        tempo.setShowTickMarks(true);
        HBox bpm = new HBox(8, new Label("BPM"), tempo);
        bpm.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(tempo, Priority.ALWAYS);

        VBox transport = new VBox(7, primary, secondary, bpm);
        transport.setPadding(new Insets(10, 18, 14, 18));
        transport.getStyleClass().add("transport");
        return transport;
    }

    private void updateResponsiveLayout(double width) {
        if (contentColumn == null || fretboardPanel == null || advisorPanel == null) return;
        contentColumn.getChildren().clear();
        if (width < 1180) {
            narrowCenter.getChildren().setAll(fretboardPanel, advisorPanel);
            contentColumn.getChildren().add(narrowCenter);
            ((Region) advisorPanel).setMaxWidth(Double.MAX_VALUE);
        } else {
            wideCenter.getChildren().setAll(fretboardPanel, advisorPanel);
            contentColumn.getChildren().add(wideCenter);
            ((Region) advisorPanel).setMaxWidth(430);
        }
    }

    private void applySuggestion() {
        GuitarStyleAdvisor.Recommendation result = GuitarStyleAdvisor.recommend(styleCombo.getValue());
        scaleCombo.setValue(result.scale());
        pickup.setText("Pickup: " + result.pickup());
        recommendation.setText(result.explanation());
        guitarPhoto.showGuitar(result.guitarType());
        setTone(result.tone());
        audio.applyLeadTone(2, result.tone());
        status.setText("AI selected " + result.guitarType() + " · " + result.scale() + " · " + result.pickup());
        refreshFretboard(-1);
    }

    private void setTone(StudioAudioEngine.ToneSettings value) {
        twang.setValue(value.twang()); warmth.setValue(value.warmth()); drive.setValue(value.drive());
        brightness.setValue(value.brightness()); sustain.setValue(value.sustain()); reverb.setValue(value.reverb());
        chorus.setValue(value.chorus()); vibrato.setValue(value.vibrato()); humanize.setValue(value.humanize());
    }

    private void playBacking() {
        StudioBackingCatalog.Style style = styleCombo.getValue();
        Path stem = style.resolveStem(backingLibrary);
        try {
            if (stem != null) audio.playLosslessBacking(stem, tempo.getValue() / style.bpm(), .92);
            else audio.playSequence(generatedBacking(style), (float) tempo.getValue(), Sequencer.LOOP_CONTINUOUSLY);
            status.setText(stem != null ? "Playing lossless studio backing." :
                    "Generated fallback playing; add " + style.stemBaseName() + ".wav for studio audio.");
        } catch (Exception ex) {
            status.setText("Backing error: " + ex.getMessage());
        }
    }

    private Sequence generatedBacking(StudioBackingCatalog.Style style) throws Exception {
        Sequence sequence = new Sequence(Sequence.PPQ, 16);
        Track track = sequence.createTrack();
        int base = 48 + root();
        track.add(event(ShortMessage.PROGRAM_CHANGE, 0, style.chordProgram(), 0, 0));
        track.add(event(ShortMessage.PROGRAM_CHANGE, 1, style.bassProgram(), 0, 0));
        for (int bar = 0; bar < Math.max(8, style.progression().length); bar++) {
            int start = bar * 64;
            int chordRoot = base + style.progression()[bar % style.progression().length];
            int third = minor() ? 3 : 4;
            int[] chord = {chordRoot, chordRoot + third, chordRoot + 7, chordRoot + 10, chordRoot + 14};
            for (int i = 0; i < chord.length; i++) note(track, 0, chord[i], 50 + i * 4, start + i * 2, start + 58);
            for (int i = 0; i < 8; i++) note(track, 1, chordRoot - 12 + (i % 2 == 0 ? 0 : 7), 70 + i % 3 * 4, start + i * 8, start + i * 8 + 6);
            for (int step = 0; step < 16; step++) {
                int tick = start + step * 4;
                if (step == 0 || step == 8) drum(track, 36, tick, 96);
                if (step == 4 || step == 12) drum(track, 38, tick, 88);
                if (step % 2 == 0) drum(track, 42, tick, 48);
            }
        }
        return sequence;
    }

    private void generate(boolean variation) {
        List<Integer> previous = new ArrayList<>(currentLick);
        currentLick.clear();
        currentLick.addAll(variation && !previous.isEmpty()
                ? GuitarTheory.variation(previous, random.nextLong())
                : GuitarTheory.generateLick(57 + root(), scaleCombo.getValue(), 12, random.nextLong()));
        lickText.setText(currentLick.stream().map(this::name).reduce((a, b) -> a + " → " + b).orElse(""));
        refreshFretboard(-1);
    }

    private void playLick() {
        if (currentLick.isEmpty()) generate(false);
        try {
            Sequence sequence = new Sequence(Sequence.PPQ, 16);
            Track track = sequence.createTrack();
            StudioAudioEngine.ToneSettings settings = tone();
            audio.applyLeadTone(2, settings);
            track.add(event(ShortMessage.PROGRAM_CHANGE, 2, styleCombo.getValue().leadProgram(), 0, 0));
            for (int i = 0; i < currentLick.size(); i++) {
                int tick = audio.humanizedTick(i * 8, settings, i);
                int velocity = audio.humanizedVelocity(98, settings, i);
                note(track, 2, currentLick.get(i), velocity, tick, tick + 6 + settings.sustain() / 28);
            }
            audio.playSequence(sequence, (float) tempo.getValue(), 0);
            animate();
        } catch (Exception ex) {
            status.setText("Lick error: " + ex.getMessage());
        }
    }

    private void animate() {
        if (animation != null) animation.stop();
        final int[] index = {0};
        animation = new Timeline(new KeyFrame(Duration.millis(60000 / tempo.getValue() / 2), event -> {
            if (index[0] >= currentLick.size()) {
                animation.stop();
                refreshFretboard(-1);
                return;
            }
            refreshFretboard(currentLick.get(index[0]++));
        }));
        animation.setCycleCount(currentLick.size() + 1);
        animation.play();
    }

    private void saveLick() {
        if (currentLick.isEmpty()) {
            status.setText("Generate a lick before saving.");
            return;
        }
        try {
            String line = keyCombo.getValue() + "|" + scaleCombo.getValue() + "|" + currentLick + System.lineSeparator();
            Files.writeString(LICK_FILE, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            loadSavedLicks();
            status.setText("Lick saved locally.");
        } catch (Exception ex) {
            status.setText("Save error: " + ex.getMessage());
        }
    }

    private void loadSavedLicks() {
        savedLicks.getItems().clear();
        if (!Files.exists(LICK_FILE)) return;
        try { savedLicks.getItems().addAll(Files.readAllLines(LICK_FILE)); }
        catch (Exception ex) { status.setText("Saved lick read error: " + ex.getMessage()); }
    }

    private void loadSelectedLick() {
        String selected = savedLicks.getSelectionModel().getSelectedItem();
        if (selected == null) {
            status.setText("Select a saved lick first.");
            return;
        }
        try {
            String[] parts = selected.split("\\|", 3);
            keyCombo.setValue(parts[0]);
            scaleCombo.setValue(parts[1]);
            currentLick.clear();
            String notes = parts[2].replace("[", "").replace("]", "").trim();
            if (!notes.isBlank()) for (String note : notes.split(",")) currentLick.add(Integer.parseInt(note.trim()));
            lickText.setText(currentLick.stream().map(this::name).reduce((a, b) -> a + " → " + b).orElse(""));
            refreshFretboard(-1);
            status.setText("Saved lick loaded.");
        } catch (Exception ex) {
            status.setText("Saved lick format error: " + ex.getMessage());
        }
    }

    private void refreshFretboard(int active) {
        if (keyCombo == null || scaleCombo == null) return;
        int root = root();
        int[] intervals = GuitarTheory.intervals(scaleCombo.getValue());
        for (int string = 0; string < 6; string++) for (int fret = 0; fret < 13; fret++) {
            Label cell = fretCells[string][fret];
            if (cell == null) continue;
            cell.getStyleClass().removeAll("scale-note", "root-note", "lick-note-active");
            int midi = OPEN_STRINGS[string] + fret;
            int relative = Math.floorMod(midi % 12 - root, 12);
            if (Arrays.stream(intervals).anyMatch(value -> value == relative)) cell.getStyleClass().add("scale-note");
            if (midi % 12 == root) cell.getStyleClass().add("root-note");
            if (midi == active) cell.getStyleClass().add("lick-note-active");
        }
    }

    private StudioAudioEngine.ToneSettings tone() {
        return new StudioAudioEngine.ToneSettings((int) twang.getValue(), (int) warmth.getValue(), (int) drive.getValue(),
                (int) brightness.getValue(), (int) sustain.getValue(), (int) reverb.getValue(),
                (int) chorus.getValue(), (int) vibrato.getValue(), (int) humanize.getValue());
    }

    private int root() { return Arrays.asList(NOTES).indexOf(keyCombo.getValue()); }
    private boolean minor() {
        String value = scaleCombo.getValue();
        return value.contains("Minor") || value.equals("Blues") || value.equals("Dorian") || value.contains("Phrygian");
    }
    private String name(int midi) { return NOTES[Math.floorMod(midi, 12)] + (midi / 12 - 1); }

    private void note(Track track, int channel, int note, int velocity, int start, int end) throws Exception {
        track.add(event(ShortMessage.NOTE_ON, channel, Math.max(0, Math.min(127, note)), velocity, start));
        track.add(event(ShortMessage.NOTE_OFF, channel, Math.max(0, Math.min(127, note)), 0, end));
    }
    private void drum(Track track, int note, int tick, int velocity) throws Exception { note(track, 9, note, velocity, tick, tick + 2); }
    private MidiEvent event(int command, int channel, int data1, int data2, int tick) throws Exception {
        ShortMessage message = new ShortMessage();
        message.setMessage(command, channel, data1, data2);
        return new MidiEvent(message, tick);
    }

    private Slider knob(int value) {
        Slider slider = new Slider(0, 127, value);
        slider.setMaxWidth(Double.MAX_VALUE);
        return slider;
    }
    private VBox slider(String name, Slider slider) {
        Label value = new Label(Integer.toString((int) slider.getValue()));
        slider.valueProperty().addListener((o, a, b) -> value.setText(Integer.toString(b.intValue())));
        HBox row = new HBox(6, slider, value);
        HBox.setHgrow(slider, Priority.ALWAYS);
        return new VBox(2, new Label(name), row);
    }
    private VBox labeled(String name, Node node) { return new VBox(2, new Label(name), node); }
    private Button button(String name) {
        Button button = new Button(name);
        button.getStyleClass().add("studio-button");
        button.setMinWidth(Region.USE_PREF_SIZE);
        return button;
    }

    private void stopAll() {
        if (animation != null) animation.stop();
        audio.stopAll();
        refreshFretboard(-1);
        status.setText("Stopped.");
    }

    @Override public void stop() throws Exception {
        stopAll();
        audio.close();
    }

    public static void main(String[] args) { launch(args); }
}
