package com.example.testp1;

import com.example.testp1.entities.ProjectAnalysisResult;
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

    private PBpageController parentController = new PBpageController();

    private int currentIdSubject = parentController.getAnaTarget();




    private double currentProjectBudget;
    private double currentProjectSpent;
    private String transactionHistoryJson;

    @FXML
    public void initialize() {

        setupViewController.setOnSimulationRequested(this::startAiAnalysis);
        System.out.println(currentIdSubject);
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

        CompletableFuture.runAsync(() -> {
            ServiceProjectAnalysis aiService = new ServiceProjectAnalysis();

            // USE THE ID WE GRABBED EARLIER!
            int budgetId = currentIdSubject;

            // HAND THE TEXT DIRECTLY TO THE AI SERVICE HERE!
            ProjectAnalysisResult realAiData = aiService.analyzeProject(budgetId, userContext);

            // 3. When finished, switch UI back on the Main Thread
            Platform.runLater(() -> {
                loadingViewController.stopAnimation();
                loadingView.setVisible(false);
                loadingView.setManaged(false);

                // Show Results View
                resultView.setVisible(true);
                resultView.setManaged(true);

                // Inject the data directly into the UI!
                if (realAiData != null && resultViewController != null) {
                    // We pass the AI data, and extract the original budget we saved in the model
                    resultViewController.injectRealData(realAiData);
                } else {
                    System.err.println("-> Failed to load results: realAiData or resultViewController is null.");
                }
            });
        });
    }
}