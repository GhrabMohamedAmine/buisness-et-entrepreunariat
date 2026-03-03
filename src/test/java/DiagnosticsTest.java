

import com.example.testp1.model.BudgetProfilDAO;
import com.example.testp1.model.ProjectBudgetDAO;
import com.example.testp1.entities.BudgetProfil;
import com.example.testp1.entities.ProjectBudget;
import com.example.testp1.services.FinancialDiagnosticsEngine;

import java.util.ArrayList;
import java.util.List;

public class DiagnosticsTest {

    public static void main(String[] args) {
        System.out.println("--- STARTING LIVE GLOBAL COMPANY HEALTH TEST ---");

        // 1. Initialize your existing DAOs
        BudgetProfilDAO profileDAO = new BudgetProfilDAO();
        ProjectBudgetDAO projectBudgetDAO = new ProjectBudgetDAO();

        try {
            // 2. Fetch the single, active global company profile
            BudgetProfil globalProfile = profileDAO.getActiveProfile();

            if (globalProfile == null) {
                System.out.println("TEST ABORTED: No active Budget Profile found in the database.");
                return;
            }

            // Using budget_disposable as the maximum allowed company limit
            double totalCompanyBudget = globalProfile.getBudgetDisposable();
            System.out.println("Global Company Budget Limit: $" + totalCompanyBudget);
            System.out.println("Currently Spent (Synced): $" + globalProfile.getTotalExpense());

            // 3. Fetch ALL project budgets using your existing getAll() method
            List<ProjectBudget> allProjects = projectBudgetDAO.getAll();
            List<Double> projectAllocations = new ArrayList<>();

            System.out.println("Found " + allProjects.size() + " active projects across the company.");

            for (ProjectBudget pb : allProjects) {
                // We use total_budget to see how much money is locked into these projects
                projectAllocations.add(pb.getTotalBudget());
                System.out.println(" - " + pb.getName() + ": $" + pb.getTotalBudget());
            }

            if (projectAllocations.isEmpty()) {
                System.out.println("TEST ABORTED: No projects found to analyze.");
                return;
            }

            // 4. Feed the extracted numbers into the math engine we built
            FinancialDiagnosticsEngine.ProfileHealthReport report =
                    FinancialDiagnosticsEngine.analyzeProfileHealth(totalCompanyBudget, projectAllocations);

            // 5. Print the final executive results
            System.out.println("\n--- FINAL EXECUTIVE ALGORITHM OUTPUT ---");
            System.out.println("Budget Utilization   : " + String.format("%.2f", report.getUtilizationPercentage()) + "%");
            System.out.println("Concentration (HHI)  : " + String.format("%.2f", report.getHhiScore()));
            System.out.println("Assigned Risk Level  : " + report.getRiskLevel());
            System.out.println("System Message       : " + report.getMessage());
            System.out.println("UI Progress Bar Value: " + String.format("%.2f", report.getUtilizationProgressBar()));

        } catch (Exception e) {
            System.err.println("TEST ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }
}