package io.github.rohanpurohit7.mididj;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.List;
import java.util.Random;

/** Temporary slapstick reaction card using real Creative Commons dog photographs. */
public final class DogLickSticker extends StackPane {
    private record Sticker(String url,String credit,String license,String[] captions) {}
    private static final List<Sticker> STICKERS=List.of(
            new Sticker("https://commons.wikimedia.org/wiki/Special:Redirect/file/Dog_Tongue_Out.jpg?width=1000",
                    "Pdpics / Wikimedia Commons","CC BY-SA 3.0",
                    new String[]{"Nice lick, dude!","Tongue out. Tone on.","That phrase had flavor!"}),
            new Sticker("https://commons.wikimedia.org/wiki/Special:Redirect/file/Dog_sticking_its_tongue_out_a_little_bit_(24044).jpg?width=1000",
                    "Rhododendrites / Wikimedia Commons","CC BY-SA 4.0",
                    new String[]{"Smooooth pull-off!","That bend had bite!","Okay, save THAT one!"}),
            new Sticker("https://commons.wikimedia.org/wiki/Special:Redirect/file/Dog_tongue.png?width=900",
                    "Serial Number 54129 / Wikimedia Commons","CC BY-SA 4.0",
                    new String[]{"Vibrato approved!","Hot lick alert!","The band says: again!"})
    );

    private final Random random=new Random();
    private final ImageView photo=new ImageView();
    private final Label caption=new Label();
    private final Label attribution=new Label();
    private PauseTransition hold;

    public DogLickSticker(){
        setMouseTransparent(true); setVisible(false); setManaged(false); setPickOnBounds(false);
        photo.setPreserveRatio(true); photo.setSmooth(true); photo.setFitWidth(260); photo.setFitHeight(190);
        caption.getStyleClass().add("dog-sticker-caption"); caption.setWrapText(true);caption.setMaxWidth(250);caption.setAlignment(Pos.CENTER);
        attribution.getStyleClass().add("dog-sticker-credit");attribution.setWrapText(true);attribution.setMaxWidth(250);
        VBox card=new VBox(5,photo,caption,attribution);card.setAlignment(Pos.CENTER);card.setPadding(new Insets(8));card.getStyleClass().add("dog-sticker-card");
        getChildren().add(card);setAlignment(Pos.TOP_RIGHT);setPadding(new Insets(18));
    }

    public void celebrate(String action){
        Sticker s=STICKERS.get(random.nextInt(STICKERS.size()));
        String text=s.captions()[random.nextInt(s.captions().length)];
        if(action!=null&&!action.isBlank()&&random.nextBoolean()) text=action;
        photo.setImage(new Image(s.url(),true));caption.setText(text);attribution.setText(s.credit()+" · "+s.license());
        if(hold!=null)hold.stop();setVisible(true);setManaged(true);setOpacity(0);setScaleX(.72);setScaleY(.72);
        FadeTransition fade=new FadeTransition(Duration.millis(180),this);fade.setToValue(1);
        ScaleTransition pop=new ScaleTransition(Duration.millis(250),this);pop.setToX(1);pop.setToY(1);pop.setByAngle(0);fade.play();pop.play();
        hold=new PauseTransition(Duration.seconds(3.2));hold.setOnFinished(e->hideSticker());hold.play();
    }

    private void hideSticker(){FadeTransition out=new FadeTransition(Duration.millis(360),this);out.setToValue(0);out.setOnFinished(e->{setVisible(false);setManaged(false);});out.play();}
}
