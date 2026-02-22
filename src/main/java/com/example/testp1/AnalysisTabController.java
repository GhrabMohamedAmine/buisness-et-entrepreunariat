package com.example.testp1;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.layout.VBox;
import java.util.concurrent.CompletableFuture;
import javafx.application.Platform;
import com.example.testp1.services.ServiceProjectAnalysis;
import javafx.util.Duration;

public class AnalysisTabController {


    @FXML private VBox setupView;
    @FXML private VBox loadingView;
    @FXML private VBox resultView;



    @FXML private AnalysisSetupController setupViewController;
    @FXML private AnalysisLoadingController loadingViewController;
    @FXML private AnalysisResultController resultViewController;



    private double currentProjectBudget;
    private double currentProjectSpent;
    private String transactionHistoryJson;

    @FXML
    public void initialize() {

        setupViewController.setOnSimulationRequested(this::startAiAnalysis);
    }

    /**
     * The Main Page calls this method to hand over the necessary data.
     */
    public void injectProjectData(double budget, double spent, String jsonHistory) {
        this.currentProjectBudget = budget;
        this.currentProjectSpent = spent;
        this.transactionHistoryJson = jsonHistory;
    }

    /**
     * Triggered by the Setup view's button.
     */
    private void startAiAnalysis(String userContext) {
        // 1. Switch UI to Loading State
        setupView.setVisible(false);
        setupView.setManaged(false);
        loadingView.setVisible(true);
        loadingView.setManaged(true);

        loadingViewController.playAnimation(); // Start the CSS-style rotation

        System.out.println("-> Starting 7-second simulation timer...");
        PauseTransition apiSimulationTimer = new PauseTransition(Duration.seconds(7));

        // This block runs exactly when the 7 seconds are up
        apiSimulationTimer.setOnFinished(event -> {

            System.out.println("-> Simulation finished! Transitioning to Results Dashboard.");

            // 3. Stop the spinning animation and hide the loading view
            loadingViewController.stopAnimation();
            loadingView.setVisible(false);
            loadingView.setManaged(false);

            // 4. Show the Results View
            resultView.setVisible(true);
            resultView.setManaged(true);

            // 5. Fire the mock data to trigger the circle animation and draw the chart!
            if (resultViewController != null) {
                resultViewController.injectMockData();
            }
        });

        // Start the timer!
        apiSimulationTimer.play();
    }
}