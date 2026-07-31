package io.github.rohanpurohit7.mididj;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Responsive photo card backed by real, high-resolution Creative Commons
 * photographs hosted by Wikimedia Commons. Images load from the internet and
 * are never replaced with generated artwork.
 */
public final class GuitarPhotoCard extends VBox {
    private record Photo(String url, String title, String credit, String license) {}

    private static final Map<String, Photo> PHOTOS = photos();

    private final StackPane frame = new StackPane();
    private final ImageView imageView = new ImageView();
    private final Label loading = new Label("Loading licensed guitar photograph…");
    private final Label title = new Label();
    private final Label attribution = new Label();

    public GuitarPhotoCard() {
        setSpacing(7);
        setPadding(new Insets(8));
        getStyleClass().add("guitar-photo-card");

        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        imageView.setCache(true);
        imageView.fitWidthProperty().bind(frame.widthProperty().subtract(16));
        imageView.fitHeightProperty().bind(frame.heightProperty().subtract(16));

        loading.setWrapText(true);
        loading.setAlignment(Pos.CENTER);
        frame.getChildren().addAll(imageView, loading);
        frame.setAlignment(Pos.CENTER);
        frame.setMinHeight(210);
        frame.setPrefHeight(320);
        frame.setMaxHeight(520);
        VBox.setVgrow(frame, Priority.ALWAYS);

        title.getStyleClass().add("guitar-photo-title");
        attribution.getStyleClass().add("guitar-photo-attribution");
        attribution.setWrapText(true);
        getChildren().addAll(frame, title, attribution);
    }

    public void showGuitar(String guitarType) {
        Photo photo = select(guitarType);
        title.setText(guitarType);
        attribution.setText(photo.title() + " — " + photo.credit() + " — " + photo.license());
        loading.setText("Loading licensed guitar photograph…");
        loading.setVisible(true);

        Image incoming = new Image(photo.url(), true);
        incoming.progressProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue.doubleValue() >= 1.0) {
                Platform.runLater(() -> crossFade(incoming));
            }
        });
        incoming.errorProperty().addListener((obs, oldValue, hasError) -> {
            if (hasError) {
                Platform.runLater(() -> loading.setText(
                        "Photo could not be loaded. Check the internet connection.\n" + photo.title()));
            }
        });
        if (incoming.getProgress() >= 1.0 && !incoming.isError()) {
            crossFade(incoming);
        }
    }

    private void crossFade(Image incoming) {
        FadeTransition out = new FadeTransition(Duration.millis(160), imageView);
        out.setToValue(0.0);
        out.setOnFinished(event -> {
            imageView.setImage(incoming);
            loading.setVisible(false);
            FadeTransition in = new FadeTransition(Duration.millis(360), imageView);
            in.setFromValue(0.0);
            in.setToValue(1.0);
            in.play();
        });
        out.play();
    }

    private static Photo select(String guitarType) {
        String value = guitarType == null ? "" : guitarType.toLowerCase(Locale.ROOT);
        if (value.contains("flamenco") || value.contains("negra")) return PHOTOS.get("flamenco");
        if (value.contains("cedar") || value.contains("nylon") || value.contains("classical")) return PHOTOS.get("nylon");
        if (value.contains("hollow") || value.contains("jazz")) return PHOTOS.get("hollow");
        if (value.contains("semi")) return PHOTOS.get("semi-hollow");
        if (value.contains("single-cut") || value.contains("humbucker") || value.contains("les paul")) return PHOTOS.get("single-cut");
        if (value.contains("t-style") || value.contains("country")) return PHOTOS.get("country");
        return PHOTOS.get("single-coil");
    }

    private static Map<String, Photo> photos() {
        Map<String, Photo> result = new LinkedHashMap<>();
        result.put("single-coil", new Photo(
                "https://commons.wikimedia.org/wiki/Special:Redirect/file/Squier_Fender_Stratocaster.jpg?width=1600",
                "Squier Fender Stratocaster",
                "Elmschrat / Wikimedia Commons",
                "CC BY-SA 4.0"));
        result.put("country", new Photo(
                "https://commons.wikimedia.org/wiki/Special:Redirect/file/Squier_Stratocaster_body.jpg?width=1600",
                "Squier Stratocaster body",
                "Elmschrat / Wikimedia Commons",
                "CC BY-SA 4.0"));
        result.put("single-cut", new Photo(
                "https://commons.wikimedia.org/wiki/Special:Redirect/file/Fender_stratocaster_black.jpg?width=1400",
                "Black electric guitar",
                "Elias.gomez / Wikimedia Commons",
                "CC BY-SA 2.0"));
        result.put("semi-hollow", new Photo(
                "https://commons.wikimedia.org/wiki/Special:Redirect/file/E_Gitarre_Hollow-Body_vs_Solid-Body.jpg?width=1600",
                "Hollow-body and solid-body electric guitars",
                "Auge=mit / Wikimedia Commons",
                "CC BY-SA 4.0"));
        result.put("hollow", new Photo(
                "https://commons.wikimedia.org/wiki/Special:Redirect/file/Schecter_Jazz_Series_-_Jazz-6_Guitar.jpg?width=900",
                "Schecter Jazz-6 hollow-body guitar",
                "Clickside / Wikimedia Commons",
                "Creative Commons license; see source page"));
        result.put("flamenco", new Photo(
                "https://commons.wikimedia.org/wiki/Special:Redirect/file/Lester_DeVoe_guitar.jpg?width=1200",
                "Lester DeVoe flamenco guitar",
                "Miheco / Wikimedia Commons",
                "CC BY-SA 2.0"));
        result.put("nylon", new Photo(
                "https://commons.wikimedia.org/wiki/Special:Redirect/file/Guitarra_Conde_tapa_001.jpg?width=1000",
                "Flamenco guitar top",
                "Villanueva / Wikimedia Commons",
                "CC BY-SA 3.0"));
        return Map.copyOf(result);
    }
}
