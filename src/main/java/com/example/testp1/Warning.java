package com.example.testp1;

import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.effect.BoxBlur;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.layout.*;
import javafx.util.Duration;
import java.io.IOException;

public class Warning extends StackPane {

    @FXML private VBox popupCard;
    @FXML private Region dimOverlay;

    public Warning() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("Warning.fxml"));
        loader.setRoot(this);
        loader.setController(this);

        try {
            loader.load();
            this.setVisible(false);
            this.setMouseTransparent(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void show(Node backgroundToBlur) {
        this.setVisible(true);
        this.setMouseTransparent(false);

        // Apply Nexum standard background dimming
        BoxBlur blur = new BoxBlur(8, 8, 3);
        ColorAdjust dim = new ColorAdjust();
        dim.setBrightness(-0.3);
        dim.setInput(blur);
        backgroundToBlur.setEffect(dim);

        // Zoom-in Animation
        ScaleTransition scale = new ScaleTransition(Duration.millis(300), popupCard);
        scale.setFromX(0.7); scale.setFromY(0.7);
        scale.setToX(1.0); scale.setToY(1.0);

        FadeTransition fade = new FadeTransition(Duration.millis(300), dimOverlay);
        fade.setFromValue(0); fade.setToValue(1);

        new ParallelTransition(scale, fade).play();
    }

    @FXML
    public void hide() {
        // Clean up background effects
        if (getParent() instanceof StackPane) {
            StackPane parent = (StackPane) getParent();
            parent.getChildren().get(0).setEffect(null);
        }

        this.setVisible(false);
        this.setMouseTransparent(true);
    }
}