package com.example.testp1;

import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import org.kordamp.ikonli.javafx.FontIcon;
import javafx.util.Duration;

public class ActionTrayController {

    @FXML private HBox container;
    @FXML private HBox iconTray;
    @FXML private Button triggerBtn;
    @FXML private FontIcon triggerIcon;

    @FXML private FontIcon addIcon;
    @FXML private FontIcon editIcon;
    @FXML private FontIcon deleteIcon;

    private boolean isExpanded = false;
    private final double COLLAPSED_WIDTH = 45.0;
    private final double EXPANDED_WIDTH = 175.0;

    @FXML
    public void initialize() {
        // Apply programmatic hover effects to the pure icons
        setupIconInteraction(addIcon);
        setupIconInteraction(editIcon);
        setupIconInteraction(deleteIcon);
    }

    /**
     * Programmatic Hover & Cursor Control
     */
    private void setupIconInteraction(FontIcon icon) {

        icon.setCursor(Cursor.HAND);

        icon.setOnMouseEntered(e -> {
            ScaleTransition scaleUp = new ScaleTransition(Duration.millis(150), icon);
            scaleUp.setToX(1.3);
            scaleUp.setToY(1.3);
            scaleUp.play();
        });

        // 3. Mouse Exited: Scale Back to Normal
        icon.setOnMouseExited(e -> {
            ScaleTransition scaleDown = new ScaleTransition(Duration.millis(150), icon);
            scaleDown.setToX(1.0);
            scaleDown.setToY(1.0);
            scaleDown.play();
        });
    }

    @FXML
    private void toggleTray() {
        if (!isExpanded) expand();
        else collapse();
        isExpanded = !isExpanded;
    }

    private void expand() {
        iconTray.setVisible(true);

        Timeline widthAnim = new Timeline(
                new KeyFrame(Duration.millis(350),
                        new KeyValue(container.maxWidthProperty(), EXPANDED_WIDTH, Interpolator.EASE_OUT))
        );

        iconTray.setTranslateX(40);
        TranslateTransition slide = new TranslateTransition(Duration.millis(350), iconTray);
        slide.setToX(0);
        slide.setInterpolator(Interpolator.EASE_OUT);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(250), iconTray);
        fadeIn.setToValue(1.0);

        // Rotate only the internal icon to preserve button shape
        RotateTransition rotate = new RotateTransition(Duration.millis(300), triggerIcon);
        rotate.setToAngle(90);

        new ParallelTransition(widthAnim, slide, fadeIn, rotate).play();
    }

    private void collapse() {
        Timeline widthAnim = new Timeline(
                new KeyFrame(Duration.millis(300),
                        new KeyValue(container.maxWidthProperty(), COLLAPSED_WIDTH, Interpolator.EASE_IN))
        );

        TranslateTransition slideBack = new TranslateTransition(Duration.millis(300), iconTray);
        slideBack.setToX(40);

        FadeTransition fadeOut = new FadeTransition(Duration.millis(200), iconTray);
        fadeOut.setToValue(0.0);

        RotateTransition rotate = new RotateTransition(Duration.millis(250), triggerIcon);
        rotate.setToAngle(0);

        ParallelTransition pt = new ParallelTransition(widthAnim, slideBack, fadeOut, rotate);
        pt.setOnFinished(e -> iconTray.setVisible(false));
        pt.play();
    }

    private OverviewController overviewController; // Use the specific controller type

    public void setOverviewController(OverviewController controller) {
        this.overviewController = controller;
    }

    @FXML private void onAdd() {
        if (overviewController != null) {
        overviewController.handleAddBPClick(); // Triggers "Set Budget Profile"
        }
    }
    @FXML private void onEdit() {
        if (overviewController != null) {
        overviewController.handleUpdateProfileClick(); // Triggers "update Profile"
        }
    }
    @FXML private void onDelete() {
        if (overviewController != null) {
        overviewController.handleDeleteProfile(); // Triggers "Reset Profil"
        }
    }
}