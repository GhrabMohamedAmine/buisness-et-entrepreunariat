package com.example.testp1.services;

import com.example.testp1.entities.ProjectBudget;
import com.example.testp1.entities.Transaction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FinancialDiagnosticsEngine {

    public static class ProfileHealthReport {
        private final double utilizationPercentage;
        private final double hhiScore;
        private final String riskLevel;
        private final String message;
        private final String actionableAdvice;
        private final ProjectBudget flaggedProject; // The project causing the most danger

        public ProfileHealthReport(double utilizationPercentage, double hhiScore, String riskLevel, String message, String actionableAdvice, ProjectBudget flaggedProject) {
            this.utilizationPercentage = utilizationPercentage;
            this.hhiScore = hhiScore;
            this.riskLevel = riskLevel;
            this.message = message;
            this.actionableAdvice = actionableAdvice;
            this.flaggedProject = flaggedProject;
        }

        public double getUtilizationPercentage() { return utilizationPercentage; }
        public double getHhiScore() { return hhiScore; }
        public String getRiskLevel() { return riskLevel; }
        public String getMessage() { return message; }
        public String getActionableAdvice() { return actionableAdvice; }
        public ProjectBudget getFlaggedProject() { return flaggedProject; }
        public double getUtilizationProgressBar() { return Math.min(utilizationPercentage / 100.0, 1.0); }
    }

    public static ProfileHealthReport analyzeProfileHealth(double totalProfileBudget, List<ProjectBudget> projects) {
        // 1. Handle empty state
        if (projects == null || projects.isEmpty()) {
            return new ProfileHealthReport(0.0, 0.0, "SUCCESS", "No active projects. The company budget is fully available.", "► ALL CLEAR: You are clear to initiate new projects.", null);
        }

        double totalAllocated = 0;
        ProjectBudget worstProject = projects.get(0);
        double maxRiskScore = -1;

        // 2. Extract deep metrics and hunt for the most dangerous project
        for (ProjectBudget pb : projects) {
            totalAllocated += pb.getTotalBudget();

            // Risk calculation: how close is it to bursting its budget?
            double projectUtilization = (pb.getTotalBudget() > 0) ? (pb.getActualSpend() / pb.getTotalBudget()) : 0;
            if (projectUtilization > maxRiskScore) {
                maxRiskScore = projectUtilization;
                worstProject = pb;
            }
        }

        // 3. Calculate Global Utilization
        double utilizationPercentage = (totalProfileBudget > 0) ? (totalAllocated / totalProfileBudget) * 100.0 : 0.0;

        // 4. Calculate HHI Concentration
        double hhiScore = 0.0;
        if (totalAllocated > 0) {
            for (ProjectBudget pb : projects) {
                double percentageShare = (pb.getTotalBudget() / totalAllocated) * 100.0;
                hhiScore += (percentageShare * percentageShare);
            }
        }

        // 5. Generate Insight & Risk Level
        String riskLevel = "SUCCESS";
        String insight = "Financial structure is stable and pacing is optimal.";
        String advice = "► ALL CLEAR: No structural modifications required at this time.";

        // Define what constitutes an actual vulnerability
        boolean isVulnerable = false;

        // Thresholds for Danger/Warning
        if (utilizationPercentage > 90.0 || hhiScore > 3500 || maxRiskScore >= 0.90) {
            riskLevel = (maxRiskScore >= 1.0 || utilizationPercentage > 95) ? "DANGER" : "WARNING";
            insight = "Structural vulnerabilities identified in active projects.";
            isVulnerable = true;
        }

        // 6. Generate specific advice ONLY if vulnerable
        if (maxRiskScore >= 1.0) {
            advice = "► IMMEDIATE ACTION: The project '" + worstProject.getName() + "' is actively over budget. Immediate audit required.";
        } else if (utilizationPercentage > 90.0) {
            advice = "► RUNWAY ALERT: Global budget is nearly exhausted. Freeze new project approvals.";
        } else if (maxRiskScore >= 0.90) {
            advice = "► WARNING: The project '" + worstProject.getName() + "' is nearing its maximum budget limit.";
        } else if (hhiScore > 3500) {
            advice = "► CONCENTRATION RISK: Too much of the global budget is tied to a single project. Consider restructuring.";
        }

        // 7. THE FIX: If there are no vulnerabilities, pass 'null' for the flagged project.
        // This tells the UI Controller to completely hide the red Deep Dive box.
        ProjectBudget projectToAudit = isVulnerable ? worstProject : null;

        return new ProfileHealthReport(utilizationPercentage, hhiScore, riskLevel, insight, advice, projectToAudit);
    }

    // NEW: Deep Dive Transaction Analyzer
    public static class TransactionReport {
        public final boolean hasData;
        public final String projectName;
        public final String topCategory;
        public final double topCategoryCost;
        public final double categoryPercentage;
        public final double largestTxCost;
        public final String largestTxDate;
        public final String largestTxRef;

        public TransactionReport(boolean hasData, String projectName, String topCategory, double topCategoryCost, double categoryPercentage, double largestTxCost, String largestTxDate, String largestTxRef) {
            this.hasData = hasData;
            this.projectName = projectName;
            this.topCategory = topCategory;
            this.topCategoryCost = topCategoryCost;
            this.categoryPercentage = categoryPercentage;
            this.largestTxCost = largestTxCost;
            this.largestTxDate = largestTxDate;
            this.largestTxRef = largestTxRef;
        }
    }

    // UPDATED ANALYZER METHOD
    public static TransactionReport generateTransactionAnalysis(ProjectBudget project, List<Transaction> transactions) {
        if (transactions == null || transactions.isEmpty()) {
            return new TransactionReport(false, project.getName(), "", 0, 0, 0, "", "");
        }

        Map<String, Double> categoryTotals = new HashMap<>();
        Transaction largestTx = transactions.get(0);

        for (Transaction t : transactions) {
            String category = (t.getExpenseCategory() != null && !t.getExpenseCategory().isEmpty()) ? t.getExpenseCategory() : "Uncategorized";
            categoryTotals.put(category, categoryTotals.getOrDefault(category, 0.0) + t.getCost());

            if (t.getCost() > largestTx.getCost()) {
                largestTx = t;
            }
        }

        String topCategory = "";
        double topCategoryCost = 0;
        for (Map.Entry<String, Double> entry : categoryTotals.entrySet()) {
            if (entry.getValue() > topCategoryCost) {
                topCategoryCost = entry.getValue();
                topCategory = entry.getKey();
            }
        }

        double categoryPercentage = (project.getActualSpend() > 0) ? (topCategoryCost / project.getActualSpend()) * 100.0 : 0.0;

        return new TransactionReport(
                true,
                project.getName(),
                topCategory,
                topCategoryCost,
                categoryPercentage,
                largestTx.getCost(),
                largestTx.getDateStamp().toString(),
                largestTx.getReference()
        );
    }
}