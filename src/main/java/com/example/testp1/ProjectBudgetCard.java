package com.example.testp1;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.RotateTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.util.Objects;

public class ProjectBudgetCard extends AnchorPane {
    private int selectedBudgetId;

    @FXML private Label projectNameLabel, projectTypeLabel, statusLabel;
    @FXML private Label totalBudgetLabel, actualSpendLabel, remainingLabel, dueDateLabel;
    @FXML private StackPane statusContainer;
    @FXML private HBox hoverActions;
    @FXML private FontIcon gearIcon;

    @FXML
    public void initialize() {
        if (hoverActions != null && gearIcon != null) {
            this.setOnMouseEntered(e -> runHoverEffect(true));
            this.setOnMouseExited(e -> runHoverEffect(false));
        }
    }


    public ProjectBudgetCard() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("ProjectBudgetCard.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    public void setProjectData(int id,String name, String type, String status, String budget, String actual, String remaining, String date) {
        this.selectedBudgetId = id;
        projectNameLabel.setText(name);
        projectTypeLabel.setText(type);
        statusLabel.setText(status);
        totalBudgetLabel.setText(budget);
        actualSpendLabel.setText(actual);
        remainingLabel.setText(remaining);
        dueDateLabel.setText(date);

        // Logic to swap status colors
        statusContainer.getStyleClass().removeAll("status-badge-on-track", "status-badge-at-risk", "status-badge-over");
        if (status.equalsIgnoreCase("AT RISK")) {
            statusContainer.getStyleClass().add("status-badge-at-risk");
        } else if (status.equalsIgnoreCase("OVER BUDGET")) {
            statusContainer.getStyleClass().add("status-badge-over");
        } else {
            statusContainer.getStyleClass().add("status-badge-on-track");
        }
    }
    private void runHoverEffect(boolean isActive) {
        Duration duration = Duration.millis(300);

        // 1. Fade Animation: Goes from 0 to 1
        FadeTransition fade = new FadeTransition(duration, hoverActions);
        fade.setFromValue(isActive ? 0.0 : 1.0);
        fade.setToValue(isActive ? 1.0 : 0.0);

        // 2. Slide Animation: Moves the HBox left by 15px when active
        TranslateTransition slide = new TranslateTransition(duration, hoverActions);
        slide.setFromX(isActive ? 15.0 : 0.0);
        slide.setToX(isActive ? 0.0 : 15.0);

        // 3. Rotation: Spins the gear icon
        RotateTransition rotate = new RotateTransition(duration, gearIcon);
        rotate.setToAngle(isActive ? 90 : 0);

        ParallelTransition anim = new ParallelTransition(fade, slide, rotate);
        anim.play();
    }
    public int getSelectedBudgetId() {
        return selectedBudgetId;
    }
}
