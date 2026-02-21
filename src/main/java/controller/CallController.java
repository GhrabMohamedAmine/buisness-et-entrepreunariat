package controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class CallController {

    @FXML private WebView webView;
    @FXML private Label titleLabel;
    @FXML private Label subLabel;

    private Stage stage;
    private WebEngine engine;

    private String callUrl;
    private boolean videoEnabled;

    public void init(Stage stage, String roomName, boolean videoEnabled) {
        this.stage = stage;
        this.videoEnabled = videoEnabled;

        if (webView != null) {
            this.engine = webView.getEngine();
        }

        titleLabel.setText(videoEnabled ? "Video call" : "Audio call");
        if (subLabel != null) subLabel.setText("Connecting…");

        String base = "https://meet.jit.si";
        String room = URLEncoder.encode(roomName, StandardCharsets.UTF_8);

        this.callUrl = base + "/" + room
                + "#config.prejoinPageEnabled=false"
                + "&config.startWithVideoMuted=" + (!videoEnabled);

        // Try WebView (may fail for mic/cam). Keep it anyway.
        if (engine != null) engine.load(callUrl);
    }

    @FXML
    private void openInBrowser() {
        if (callUrl == null) return;
        try {
            Desktop.getDesktop().browse(URI.create(callUrl));
            if (subLabel != null) subLabel.setText("Opened in browser");
        } catch (Exception e) {
            e.printStackTrace();
            if (subLabel != null) subLabel.setText("Failed to open browser");
        }
    }

    @FXML
    private void toggleMute() {
        // WebView control of Jitsi requires JS injection; keep as no-op for now.
        if (subLabel != null) subLabel.setText("Mute toggle (not wired yet)");
    }

    @FXML
    private void toggleCamera() {
        // WebView control of Jitsi requires JS injection; keep as no-op for now.
        if (subLabel != null) subLabel.setText("Camera toggle (not wired yet)");
    }

    @FXML
    private void hangup() {
        // Stop the WebView page + close
        try {
            if (engine != null) engine.load("about:blank");
        } catch (Exception ignored) {}
        if (stage != null) stage.close();
    }
}