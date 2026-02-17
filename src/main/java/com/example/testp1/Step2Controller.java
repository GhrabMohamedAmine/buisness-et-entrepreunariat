package com.example.testp1;


import javafx.fxml.FXML;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.javafx.FontIcon;

public class Step2Controller {

    @FXML
    private FontIcon pieIcon;

    @FXML
    public void initialize() {
        // Set the icon color to the specific green from your design
        pieIcon.setIconColor(Color.web("#22C55E"));
    }
}