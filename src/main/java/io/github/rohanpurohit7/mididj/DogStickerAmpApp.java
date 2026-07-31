package io.github.rohanpurohit7.mididj;

import javafx.application.Application;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/** Adds non-blocking slapstick dog-photo reactions over the full amp studio. */
public final class DogStickerAmpApp extends Application {
    @Override
    public void start(Stage stage) {
        new AmpStudioGuitarApp().start(stage);
        Scene existing = stage.getScene();
        Parent studio = existing.getRoot();
        DogLickSticker sticker = new DogLickSticker();
        StackPane layered = new StackPane(studio, sticker);
        existing.setRoot(layered);

        layered.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            Button button = findButton(event.getTarget());
            if (button == null) return;
            String text = button.getText() == null ? "" : button.getText().toUpperCase();
            if (text.contains("PLAY LICK")) sticker.celebrate("Nice lick, dude!");
            else if (text.contains("CONTINUOUS IMPROV")) sticker.celebrate("The dog joined the jam!");
            else if (text.contains("GENERATE LICK")) sticker.celebrate("Fresh lick incoming!");
            else if (text.contains("SAVE")) sticker.celebrate("Okay, save THAT one!");
            else if (text.contains("TRACE")) sticker.celebrate("Follow that fret trail!");
        });
    }

    private static Button findButton(Object target) {
        if (!(target instanceof Node node)) return null;
        Node cursor = node;
        while (cursor != null) {
            if (cursor instanceof Button button) return button;
            cursor = cursor.getParent();
        }
        return null;
    }

    public static void main(String[] args) { launch(args); }
}
