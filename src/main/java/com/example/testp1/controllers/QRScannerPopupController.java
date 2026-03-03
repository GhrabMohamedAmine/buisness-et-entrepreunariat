package com.example.testp1.controllers;

import com.example.testp1.services.QRScannerService;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.effect.BoxBlur;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.function.Consumer;

public class QRScannerPopupController {

    @FXML private StackPane rootPane;
    @FXML private Region dimOverlay;
    @FXML private VBox popupCard;
    @FXML private ImageView viewfinder;
    @FXML private Label statusLabel;

    private QRScannerService scannerService;
    private Consumer<String> onScanComplete;

    @FXML
    public void initialize() {
        scannerService = new QRScannerService();
        // Set initial states for animation
        popupCard.setScaleX(0.7);
        popupCard.setScaleY(0.7);
    }

    /**
     * Called by PBPageController to open the scanner.
     */
    public void show(Node backgroundToBlur, Consumer<String> onScanComplete) {
        this.onScanComplete = onScanComplete;
        statusLabel.setText("Scanning for QR Codes...");
        statusLabel.setStyle("-fx-text-fill: #10B981;");

        // 1. Play your exact Nexum UI Animation
        triggerOpenSequence(backgroundToBlur);

        // 2. Start the Hardware background thread
        scannerService.startScanner(viewfinder, (decodedText) -> {

            // Turn off camera the millisecond it finds a code
            scannerService.stopScanner();

            // Hand the data back and run the hide animation
            Platform.runLater(() -> {
                if (this.onScanComplete != null) {
                    this.onScanComplete.accept(decodedText);
                }
                hide();
            });
        });
    }

    // --- YOUR EXACT ANIMATION LOGIC ---

    private void triggerOpenSequence(Node backgroundToBlur) {
        rootPane.setVisible(true);
        rootPane.setMouseTransparent(false);

        BoxBlur blur = new BoxBlur(8, 8, 3);
        ColorAdjust dim = new ColorAdjust();
        dim.setBrightness(-0.3);
        dim.setInput(blur);
        backgroundToBlur.setEffect(dim);

        ScaleTransition scale = new ScaleTransition(Duration.millis(300), popupCard);
        scale.setFromX(0.7); scale.setFromY(0.7);
        scale.setToX(1.0); scale.setToY(1.0);

        FadeTransition fade = new FadeTransition(Duration.millis(300), dimOverlay);
        fade.setFromValue(0); fade.setToValue(1);

        new ParallelTransition(scale, fade).play();
    }

    @FXML
    public void hide() {
        // ALWAYS kill the camera if the user clicks cancel or the background
        if (scannerService != null) {
            scannerService.stopScanner();
        }

        if (rootPane.getParent() instanceof StackPane) {
            StackPane parent = (StackPane) rootPane.getParent();
            parent.getChildren().get(0).setEffect(null); // Clear full-page blur
        }

        ScaleTransition scale = new ScaleTransition(Duration.millis(200), popupCard);
        scale.setToX(0.7); scale.setToY(0.7);

        FadeTransition fade = new FadeTransition(Duration.millis(200), dimOverlay);
        fade.setToValue(0);

        ParallelTransition anim = new ParallelTransition(scale, fade);
        anim.setOnFinished(e -> {
            rootPane.setVisible(false);
            rootPane.setMouseTransparent(true);
            viewfinder.setImage(null); // Clear the frozen frame
        });
        anim.play();
    }
}