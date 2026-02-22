package com.example.testp1;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import java.util.function.Consumer;

public class AnalysisSetupController {

    @FXML private TextArea contextTextArea;
    @FXML private Button runButton;

    // 1. This is the "Wire" that connects back to the main TabController
    private Consumer<String> onSimulationRequested;

    /**
     * 2. The main TabController calls this to plug into the wire.
     */
    public void setOnSimulationRequested(Consumer<String> onSimulationRequested) {
        this.onSimulationRequested = onSimulationRequested;
    }

    /**
     * 3. This is triggered by your FXML button: onAction="#handleRunSimulation"
     */
    @FXML
    private void handleRunSimulation(ActionEvent event) {
        // Grab whatever the user typed
        String userContext = contextTextArea.getText() != null ? contextTextArea.getText() : "";

        // Send the message across the wire to the parent controller!
        if (onSimulationRequested != null) {
            onSimulationRequested.accept(userContext); // This instantly triggers startAiAnalysis() in your TabController
        }
    }
}