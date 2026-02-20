package controllers;

import com.github.sarxos.webcam.Webcam;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CaptureFaceController {

    @FXML
    private ImageView webcamView;

    @FXML
    private Button captureBtn;

    @FXML
    private Label statusLabel;

    private Webcam webcam;
    private ScheduledExecutorService scheduler;
    private byte[] capturedImageData;
    private Runnable onCaptureCallback;

    @FXML
    public void initialize() {
        openWebcam();
        captureBtn.setOnAction(e -> captureImage());
    }

    private void openWebcam() {
        try {
            webcam = Webcam.getDefault();
            if (webcam == null) {
                statusLabel.setText("Aucune webcam trouvée");
                return;
            }
            webcam.open();
            scheduler = Executors.newSingleThreadScheduledExecutor();
            scheduler.scheduleAtFixedRate(this::updateWebcamView, 0, 33, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            statusLabel.setText("Erreur webcam: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateWebcamView() {
        if (webcam != null && webcam.isOpen()) {
            BufferedImage image = webcam.getImage();
            if (image != null) {
                Image fxImage = SwingFXUtils.toFXImage(image, null);
                Platform.runLater(() -> webcamView.setImage(fxImage));
            }
        }
    }

    private void captureImage() {
        if (webcam != null && webcam.isOpen()) {
            BufferedImage image = webcam.getImage();
            if (image != null) {
                try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    ImageIO.write(image, "jpg", baos);
                    capturedImageData = baos.toByteArray();
                    statusLabel.setText("Photo capturée !");
                    if (onCaptureCallback != null) {
                        onCaptureCallback.run();
                    }
                } catch (IOException e) {
                    statusLabel.setText("Erreur conversion image");
                    e.printStackTrace();
                }
            } else {
                statusLabel.setText("Impossible de capturer l'image");
            }
        }
    }

    public byte[] getCapturedImageData() {
        return capturedImageData;
    }

    public void setOnCaptureCallback(Runnable callback) {
        this.onCaptureCallback = callback;
    }

    public void closeWebcam() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
        if (webcam != null && webcam.isOpen()) {
            webcam.close();
        }
    }
}