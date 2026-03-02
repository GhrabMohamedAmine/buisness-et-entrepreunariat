package com.example.testp1.services;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamResolution;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class QRScannerService {

    private Webcam webcam;
    private Thread scannerThread;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    /**
     * Starts the camera in the background.
     * @param viewfinder The JavaFX ImageView where the live video will be shown.
     * @param onQrDecoded The function to run when a QR code is successfully read.
     */
    public void startScanner(ImageView viewfinder, Consumer<String> onQrDecoded) {
        if (isRunning.get()) return;

        webcam = Webcam.getDefault();
        if (webcam == null) {
            System.err.println("-> No webcam found!");
            return;
        }

        webcam.setViewSize(WebcamResolution.VGA.getSize());
        webcam.open();
        isRunning.set(true);

        scannerThread = new Thread(() -> {
            MultiFormatReader reader = new MultiFormatReader();

            while (isRunning.get()) {
                try {
                    // 1. Grab the hardware frame
                    BufferedImage frame = webcam.getImage();
                    if (frame == null) continue;

                    // 2. Push the frame to the JavaFX UI so the user can see it
                    Image fxImage = SwingFXUtils.toFXImage(frame, null);
                    Platform.runLater(() -> viewfinder.setImage(fxImage));

                    // 3. Try to find a QR Code in this frame
                    LuminanceSource source = new BufferedImageLuminanceSource(frame);
                    BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

                    try {
                        Result result = reader.decode(bitmap);
                        // BOOM! We found a QR code!
                        String decodedText = result.getText();
                        System.out.println("-> QR Code Found: " + decodedText);

                        // Stop scanning and send the text back to the UI
                        stopScanner();
                        Platform.runLater(() -> onQrDecoded.accept(decodedText));

                    } catch (NotFoundException e) {
                        // No QR code in this frame, perfectly normal. Loop continues.
                    }

                    // Run at roughly ~30 FPS
                    Thread.sleep(33);

                } catch (InterruptedException e) {
                    break;
                }
            }
        });

        scannerThread.setDaemon(true);
        scannerThread.start();
    }

    public void stopScanner() {
        isRunning.set(false);
        if (webcam != null && webcam.isOpen()) {
            webcam.close();
        }
    }
}