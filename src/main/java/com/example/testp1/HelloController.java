package com.example.testp1;

import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

public class HelloController {
    @FXML
    private Label welcomeText;

    @FXML
    protected void onHelloButtonClick() {
        welcomeText.setText("Welcome to JavaFX Application!");
    }

    @FXML
    private Button getStartedButton;
    @FXML
    private FontIcon buttonIcon;

    @FXML
    public void initialize() {

        TranslateTransition slideRight = new TranslateTransition(Duration.millis(200), buttonIcon);
        slideRight.setToX(5);

        TranslateTransition slideLeft = new TranslateTransition(Duration.millis(200), buttonIcon);
        slideLeft.setToX(0);

        getStartedButton.setOnMouseEntered(e -> slideRight.playFromStart());
        getStartedButton.setOnMouseExited(e -> slideLeft.playFromStart());
    }
}
