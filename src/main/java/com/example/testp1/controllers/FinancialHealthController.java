package com.example.testp1.controllers;

import com.example.testp1.entities.BudgetProfil;
import com.example.testp1.entities.ProjectBudget;
import com.example.testp1.model.BudgetProfilDAO;
import com.example.testp1.model.ProjectBudgetDAO;
import com.example.testp1.model.TransactionDAO;
import com.example.testp1.services.FinancialDiagnosticsEngine;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

public class FinancialHealthController {

    @FXML private ProgressBar utilizationBar;
    @FXML private Label utilizationLabel;
    @FXML private Label riskLevelLabel;
    @FXML private Label hhiScoreLabel;
    @FXML private Label insightMessageLabel;
    @FXML private Label actionableAdviceLabel;
    @FXML private Label transactionAnalysisLabel;
    @FXML private VBox deepDiveContainer;
    @FXML private Label ddProjectBadge;
    @FXML private Label ddCategoryLabel;
    @FXML private Label ddCategoryStatsLabel;
    @FXML private Label ddAnomalyCostLabel;
    @FXML private Label ddAnomalyRefLabel;
    @FXML private Label ddInterventionLabel;// NEW

    private final BudgetProfilDAO profileDAO = new BudgetProfilDAO();
    private final ProjectBudgetDAO projectBudgetDAO = new ProjectBudgetDAO();
    private final TransactionDAO transactionDAO = new TransactionDAO();

    @FXML
    public void initialize() {
        loadDiagnosticData();
    }

    @FXML
    public void handleBackAction() {
        // Logic to close this pane or switch back to the main Financial Overview
        FinanceController.getInstance().loadView("Overviewpage.fxml");
        System.out.println("Navigating back to Financial Overview...");
    }

    private void loadDiagnosticData() {
        try {
            BudgetProfil globalProfile = profileDAO.getActiveProfile();
            if (globalProfile == null) {
                insightMessageLabel.setText("System Notice: No active global Budget Profile found.");
                riskLevelLabel.setText("OFFLINE");
                return;
            }

            double totalCompanyBudget = globalProfile.getBudgetDisposable();

            // Pass the FULL list of project objects to the new engine
            List<ProjectBudget> allProjects = projectBudgetDAO.getAll();

            if (allProjects.isEmpty()) {
                insightMessageLabel.setText("System Notice: No active projects to analyze.");
                riskLevelLabel.setText("STANDBY");
                return;
            }

            FinancialDiagnosticsEngine.ProfileHealthReport report =
                    FinancialDiagnosticsEngine.analyzeProfileHealth(totalCompanyBudget, allProjects);

            // Update standard UI Values
            utilizationBar.setProgress(report.getUtilizationProgressBar());
            utilizationLabel.setText(String.format("%.1f%%", report.getUtilizationPercentage()));
            hhiScoreLabel.setText(String.format("%.2f", report.getHhiScore()));
            riskLevelLabel.setText(report.getRiskLevel());
            insightMessageLabel.setText(report.getMessage());
            actionableAdviceLabel.setText(report.getActionableAdvice());

            // 2. NEW: Deep Dive into the worst project's Transaction History
            // ... (Macro Engine logic stays the same) ...

            // 2. Deep Dive into the worst project's Transaction History
            ProjectBudget worstProject = report.getFlaggedProject();

            if (worstProject != null) {
                deepDiveContainer.setVisible(true);
                deepDiveContainer.setManaged(true);

                List<com.example.testp1.entities.Transaction> worstProjectTx = transactionDAO.getByBudgetId(worstProject.getId());
                FinancialDiagnosticsEngine.TransactionReport txReport = FinancialDiagnosticsEngine.generateTransactionAnalysis(worstProject, worstProjectTx);

                if (txReport.hasData) {
                    ddProjectBadge.setText(txReport.projectName.toUpperCase());
                    ddCategoryLabel.setText(txReport.topCategory);
                    ddCategoryStatsLabel.setText(String.format("$%.2f (%.1f%% of project spend)", txReport.topCategoryCost, txReport.categoryPercentage));

                    ddAnomalyCostLabel.setText(String.format("$%.2f", txReport.largestTxCost));
                    ddAnomalyRefLabel.setText(String.format("Ref: %s | Date: %s", txReport.largestTxRef, txReport.largestTxDate));

                    double threshold = txReport.largestTxCost * 0.5;
                    ddInterventionLabel.setText(String.format("Immediately review all '%s' expenses. Mandate strict management approval for any future transactions in this category exceeding $%.2f.", txReport.topCategory, threshold));
                } else {
                    ddProjectBadge.setText(worstProject.getName().toUpperCase());
                    ddInterventionLabel.setText("No transaction history available to determine the root cause of the budget variance.");
                }
            } else {
                // Hide the box entirely if the company is perfectly healthy
                deepDiveContainer.setVisible(false);
                deepDiveContainer.setManaged(false);
            }

            updateRiskColors(report.getRiskLevel());

        } catch (Exception e) {
            insightMessageLabel.setText("Error loading diagnostics: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateRiskColors(String riskLevel) {
        // 1. Strip your specific badge classes
        riskLevelLabel.getStyleClass().removeAll("status-badge-OT", "status-badge-AR", "status-badge-OB");

        // 2. Strip our new dynamic progress bar classes
        utilizationBar.getStyleClass().removeAll("progress-success", "progress-warning", "progress-danger");

        // 3. Apply the correct classes based on your theme
        switch (riskLevel) {
            case "SUCCESS":
                riskLevelLabel.setText("HEALTHY");
                riskLevelLabel.getStyleClass().add("status-badge-OT");   // Your Green Badge
                utilizationBar.getStyleClass().add("progress-success");
                break;
            case "WARNING":
                riskLevelLabel.setText("MODERATE RISK");
                riskLevelLabel.getStyleClass().add("status-badge-AR");   // Your Amber Badge
                utilizationBar.getStyleClass().add("progress-warning");
                break;
            case "DANGER":
                riskLevelLabel.setText("CRITICAL RISK");
                riskLevelLabel.getStyleClass().add("status-badge-OB");   // Your Red Badge
                utilizationBar.getStyleClass().add("progress-danger");
                break;
        }
    }
}