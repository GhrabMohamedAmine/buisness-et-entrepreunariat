package com.example.testp1;

import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.BoxBlur;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.layout.*;
import javafx.util.Duration;
import java.io.IOException;

public class DeleteConfirmation extends StackPane {

    @FXML private VBox popupCard;
    @FXML private Region dimOverlay;
    @FXML private Label deleteLabel;
    @FXML private Button deleteButton;
    @FXML private Label deleteText;

    private Runnable onConfirmAction;

    public DeleteConfirmation() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("DeleteConfirmation.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        try {
            loader.load();
            this.setVisible(false);
            this.setMouseTransparent(true);
            popupCard.prefWidthProperty().bind(this.widthProperty().multiply(0.35));
            popupCard.prefHeightProperty().bind(this.heightProperty().multiply(0.25));
        } catch (IOException e) { e.printStackTrace(); }
    }

    public void show(Node backgroundToBlur, Runnable confirmAction) {
        this.onConfirmAction = confirmAction; // Store the "What to do" logic
        this.setVisible(true);
        this.setMouseTransparent(false);


        // Standard Nexum Blur
        BoxBlur blur = new BoxBlur(8, 8, 3);
        ColorAdjust dim = new ColorAdjust();
        dim.setBrightness(-0.3);
        dim.setInput(blur);
        backgroundToBlur.setEffect(dim);

        ScaleTransition scale = new ScaleTransition(Duration.millis(300), popupCard);
        scale.setFromX(0.7); scale.setFromY(0.7);
        scale.setToX(1.0); scale.setToY(1.0);
        scale.play();
    }

    public void showTransactionDelete(Node backgroundToBlur, Runnable confirmAction){
        this.onConfirmAction = confirmAction; // Store the "What to do" logic
        this.setVisible(true);
        this.setMouseTransparent(false);

        this.deleteButton.setText("delete transaction");
        this.deleteLabel.setText("Confirm Deletion");
        this.deleteText.setText("Are you sure you want to delete this transaction?");


        triggerOpenSequence(backgroundToBlur);
    }

    public void showBudgetDelete(Node backgroundToBlur, Runnable confirmAction){
        this.onConfirmAction = confirmAction; // Store the "What to do" logic
        this.setVisible(true);
        this.setMouseTransparent(false);

        this.deleteButton.setText("delete budget");
        this.deleteLabel.setText("Confirm Deletion");
        this.deleteText.setText("Are you sure you want to delete this budget?");


        triggerOpenSequence(backgroundToBlur);
    }

    private void triggerOpenSequence(Node backgroundToBlur) {
        this.setVisible(true);
        this.setMouseTransparent(false);

        // 1. Background Visuals: Blur + Dim
        BoxBlur blur = new BoxBlur(8, 8, 3);
        ColorAdjust dim = new ColorAdjust();
        dim.setBrightness(-0.3);
        dim.setInput(blur);
        backgroundToBlur.setEffect(dim);

        // 2. Animations: Scale the card and Fade the overlay
        ScaleTransition scale = new ScaleTransition(Duration.millis(300), popupCard);
        scale.setFromX(0.7); scale.setFromY(0.7);
        scale.setToX(1.0); scale.setToY(1.0);

        FadeTransition fade = new FadeTransition(Duration.millis(300), dimOverlay);
        fade.setFromValue(0); fade.setToValue(1);

        new ParallelTransition(scale, fade).play();
    }

    @FXML
    private void handleConfirm() {
        if (onConfirmAction != null) {
            onConfirmAction.run(); // Execute the actual delete code from Controller
        }
        hide();
    }

    @FXML
    public void hide() {
        if (getParent() instanceof StackPane) {
            ((StackPane) getParent()).getChildren().get(0).setEffect(null);
        }
        this.setVisible(false);
        this.setMouseTransparent(true);
    }
}