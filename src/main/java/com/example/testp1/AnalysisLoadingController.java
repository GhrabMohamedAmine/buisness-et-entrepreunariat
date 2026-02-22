package com.example.testp1;

import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class AnalysisLoadingController {

    @FXML private StackPane loaderPane;
    @FXML private Rectangle stick;

    private RotateTransition mainRotation;
    private RotateTransition stickRotation;

    @FXML
    public void initialize() {
        // 1. Main loader rotation (matches: animation: rotate 1.5s ease-in infinite alternate)
        mainRotation = new RotateTransition(Duration.seconds(1.5), loaderPane);
        mainRotation.setByAngle(360);
        mainRotation.setCycleCount(RotateTransition.INDEFINITE);
        mainRotation.setAutoReverse(true); // "alternate"
        mainRotation.setInterpolator(Interpolator.EASE_IN); // "ease-in"


        stickRotation = new RotateTransition(Duration.seconds(1.2), stick);
        stickRotation.setByAngle(-360); // Negative makes it "reverse"
        stickRotation.setCycleCount(RotateTransition.INDEFINITE);
        stickRotation.setAutoReverse(true); // "alternate"
        stickRotation.setInterpolator(Interpolator.LINEAR); // "linear"
    }

    // Call this when the view is shown on screen
    public void playAnimation() {
        mainRotation.play();
        stickRotation.play();
    }

    // Call this to clean up resources when the view is hidden
    public void stopAnimation() {
        mainRotation.stop();
        stickRotation.stop();
    }
}