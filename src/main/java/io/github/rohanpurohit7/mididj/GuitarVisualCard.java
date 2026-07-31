package io.github.rohanpurohit7.mididj;

import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.util.Duration;

/** Original, non-branded guitar illustration that cross-fades between archetypes. */
public final class GuitarVisualCard extends StackPane {
    private final Canvas canvas = new Canvas(330, 250);
    private String guitarType = "Versatile Double-Cut";

    public GuitarVisualCard() {
        setPadding(new Insets(12));
        getStyleClass().add("guitar-visual-card");
        getChildren().add(canvas);
        draw();
    }

    public void showGuitar(String type) {
        guitarType = type == null ? "Versatile Double-Cut" : type;
        FadeTransition out = new FadeTransition(Duration.millis(140), canvas);
        out.setFromValue(1.0);
        out.setToValue(0.15);
        out.setOnFinished(event -> {
            draw();
            FadeTransition in = new FadeTransition(Duration.millis(320), canvas);
            in.setFromValue(0.15);
            in.setToValue(1.0);
            in.play();
        });
        out.play();
    }

    private void draw() {
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        g.setFill(Color.web("#0d1520"));
        g.fillRoundRect(0, 0, 330, 250, 24, 24);
        g.setStroke(Color.web("#4f6478"));
        g.setLineWidth(2.0);
        g.strokeRoundRect(1, 1, 328, 248, 24, 24);

        boolean acoustic = guitarType.toLowerCase().contains("nylon") || guitarType.toLowerCase().contains("flamenco") || guitarType.toLowerCase().contains("cedar");
        boolean hollow = guitarType.toLowerCase().contains("hollow") || guitarType.toLowerCase().contains("semi-hollow");
        boolean singleCut = guitarType.toLowerCase().contains("single-cut");
        boolean tStyle = guitarType.toLowerCase().contains("t-style");

        Color body = acoustic ? Color.web("#b96f38") :
                hollow ? Color.web("#a54332") :
                tStyle ? Color.web("#d3b34c") :
                singleCut ? Color.web("#7b3144") : Color.web("#245f75");
        Color edge = body.brighter();

        g.save();
        g.translate(55, 34);
        g.rotate(-8);

        // Neck and headstock.
        g.setFill(Color.web("#70472b"));
        g.fillRoundRect(137, 92, 112, 20, 7, 7);
        g.setFill(Color.web("#9a6235"));
        g.fillRoundRect(242, 86, 38, 32, 10, 10);
        g.setStroke(Color.web("#dbc9a5"));
        g.setLineWidth(1);
        for (int i = 0; i < 6; i++) {
            double y = 95 + i * 3;
            g.strokeLine(92, y, 275, y + (i - 2.5) * 0.35);
        }
        for (int i = 0; i < 9; i++) {
            double x = 148 + i * 10;
            g.strokeLine(x, 92, x, 112);
        }

        // Body silhouette.
        g.setFill(body);
        g.setStroke(edge);
        g.setLineWidth(4);
        if (acoustic) {
            g.fillOval(15, 44, 115, 128);
            g.strokeOval(15, 44, 115, 128);
            g.setFill(Color.web("#2a1b14"));
            g.fillOval(72, 91, 28, 28);
            g.setStroke(Color.web("#d7a86e"));
            g.strokeOval(67, 86, 38, 38);
            g.setFill(Color.web("#4b2a1d"));
            g.fillRoundRect(70, 131, 40, 8, 4, 4);
        } else if (tStyle) {
            g.fillRoundRect(20, 55, 112, 108, 42, 42);
            g.strokeRoundRect(20, 55, 112, 108, 42, 42);
            g.clearRect(100, 54, 38, 34);
        } else if (singleCut) {
            g.fillOval(18, 54, 112, 112);
            g.strokeOval(18, 54, 112, 112);
            g.clearRect(102, 49, 35, 42);
        } else {
            g.fillOval(18, 48, 112, 120);
            g.strokeOval(18, 48, 112, 120);
            g.clearRect(100, 48, 34, 38);
            g.clearRect(100, 130, 34, 38);
        }

        if (!acoustic) {
            g.setFill(Color.web("#1b2027"));
            g.fillRoundRect(75, 86, 19, 45, 5, 5);
            g.fillRoundRect(99, 86, 19, 45, 5, 5);
            g.setFill(Color.web("#d8d9dc"));
            g.fillRoundRect(68, 137, 48, 9, 4, 4);
            if (hollow) {
                g.setStroke(Color.web("#25130f"));
                g.setLineWidth(4);
                g.strokeArc(39, 78, 20, 56, 80, 180, javafx.scene.shape.ArcType.OPEN);
            }
        }
        g.restore();

        g.setFill(Color.web("#e8edf2"));
        g.setFont(javafx.scene.text.Font.font("System", 15));
        g.fillText(guitarType, 18, 226);
        g.setFill(Color.web("#8fa7ba"));
        g.setFont(javafx.scene.text.Font.font("System", 11));
        g.fillText("AI-selected original guitar archetype", 18, 243);
    }
}
