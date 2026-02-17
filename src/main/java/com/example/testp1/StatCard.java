package com.example.testp1;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;
import java.io.IOException;

public class StatCard extends VBox {

    @FXML private StackPane iconContainer;
    @FXML private FontIcon statIcon;
    @FXML private Label percentageLabel;
    @FXML private Label titleLabel;
    @FXML private Label valueLabel;

    public StatCard() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("StatCard.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    public void setStatData(String title, String value, String percent, String iconCode, String theme) {
        titleLabel.setText(title);
        valueLabel.setText(value);
        percentageLabel.setText(percent);
        statIcon.setIconLiteral(iconCode);

        iconContainer.getStyleClass().removeAll("bg-blue", "bg-red", "bg-green", "bg-purple");
        iconContainer.getStyleClass().add("bg-" + theme);

        percentageLabel.getStyleClass().removeAll("badge-success", "badge-danger");
        if (percent.contains("-")) {
            percentageLabel.getStyleClass().add("badge-danger");
        } else {
            percentageLabel.getStyleClass().add("badge-success");
        }
    }
}