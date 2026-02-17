package com.example.testp1;

import com.example.testp1.entities.ProjectBudget;
import com.example.testp1.entities.Transaction;
import com.example.testp1.model.ProjectDAO;
import com.example.testp1.services.ServiceProjectBudget; // Corrected service name
import com.example.testp1.services.ServiceTransaction;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import org.kordamp.ikonli.javafx.FontIcon;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

public class PBpageController {

    // --- FXML UI Fields ---
    @FXML private Label breadcrumbProjectName;
    @FXML private Label projectNameLabel;
    @FXML private Label budgetCodeLabel;
    @FXML private Label statusBadge;

    @FXML private Label allocatedLabel;
    @FXML private Label spentLabel;
    @FXML private Label remainingLabel;

    @FXML private Label utilizationPercentLabel;
    @FXML private ProgressBar utilizationBar;
    private final ProjectDAO projectDAO = new ProjectDAO();


    @FXML
    private AnchorPane mainPageWrapper;
    @FXML
    private AddTransaction addTransactionPage;
    @FXML
    private UpdateTransaction updateTransactionPage;
    @FXML
    private DeleteConfirmation deleteTransactionPopUp;
    @FXML
    private FontIcon deleteIcon;
    @FXML
    private AddProjectBudget updateCurrentProjectBudget;
    @FXML
    private FontIcon editIcon;
    @FXML
    private AnchorPane overviewContent;
    @FXML
    private AnchorPane analysisContent;


    public ProjectBudget getCurrentBudget() {
        return currentBudget;
    }

    private ProjectBudget currentBudget;

    private ServiceTransaction serviceTransaction = new ServiceTransaction();
    @FXML
    private Button overviewTab;
    @FXML
    private Button expensesTab;
    @FXML
    private Button analysisTab;

    private List<Button> tabButtons;
    @FXML
    private PBtransactionController transactionTabController;
    @FXML
    private AnchorPane expensesContent;

    @FXML
    public void initialize() {

        int targetId = FinanceController.getCurrentBudgetId();

        tabButtons = Arrays.asList(overviewTab, expensesTab, analysisTab);

        if (transactionTabController != null) {
            transactionTabController.setParentController(this);
            System.out.println("Handshake Complete: PBtransactionController now knows its Boss.");
        }

        updateTabStyles(overviewTab);
        updateCurrentProjectBudget.setOnSaveSuccess( ()-> {
            loadBudgetData(targetId);
        });

        if (targetId != 0) {
            loadBudgetData(targetId);
        } else {
            System.err.println("Navigation Context Error: No Budget ID found.");
        }
    }

    public void triggerAddTransactionSequence() {
        if (currentBudget != null) {
            addTransactionPage.show(currentBudget.getId(), mainPageWrapper);
            addTransactionPage.setOnSaveSuccess(() ->{
                loadBudgetData(currentBudget.getId());

                // Tell the tab to reload the transaction rows
                transactionTabController.refreshList();
            });

        }
    }

    public void triggerDeleteTransactionSequence(Transaction t) {
        if (t != null && currentBudget != null) {

            // 2. Show the delete confirmation (using your generic delete controller)
            deleteTransactionPopUp.showTransactionDelete(mainPageWrapper, () -> {
                try {
                    // This is the actual DB logic that runs inside the popup's 'handleConfirm'
                    serviceTransaction.delete(t);
                    System.out.println("Transaction " + t.getId() + " Deleted Successfully");

                    // 2. Refresh everything after deletion
                    loadBudgetData(currentBudget.getId()); // Updates header totals ($5k/$0)
                    transactionTabController.refreshList(); // Reloads the 1086px rows
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    public void triggerUpdateTransactionSequence(Transaction t) {
        if (currentBudget != null) {
            updateTransactionPage.show(t,mainPageWrapper);
            updateTransactionPage.setOnUpdateSuccess(() ->{
                loadBudgetData(currentBudget.getId());

                // Tell the tab to reload the transaction rows
                transactionTabController.refreshList();
            });

        }
    }

    /**
     * Uses ServiceProjectBudget to fetch data from the database.
     */
    private void loadBudgetData(int id) {
        try {
            // 1. Initialize the service and fetch the budget object
            ServiceProjectBudget service = new ServiceProjectBudget();
            this.currentBudget = service.getById(id);

            if (this.currentBudget != null) {
                // 2. Fetch the associated project name using the projectId from the budget
                ProjectDAO projectDAO = new ProjectDAO();
                String associatedProjectName = projectDAO.getNameById(this.currentBudget.getProjectId());

                // 3. Populate the UI labels and components
                populateUI(this.currentBudget, associatedProjectName);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    /**
     * Maps data to the FXML components to match the design reference.
     */
    private void populateUI(ProjectBudget budget, String projectName) {
        // Header Section
        projectNameLabel.setText(budget.getName());
        breadcrumbProjectName.setText(budget.getName());
        budgetCodeLabel.setText("Project: " + projectName);

        // Status
        statusBadge.setText(budget.getStatus().toUpperCase());
        applyStatusStyle(budget.getStatus());


        // Calculations
        double total = budget.getTotalBudget();
        double spent = budget.getActualSpend();
        double remaining = total - spent;
        double utilization = (total > 0) ? (spent / total) : 0;

        allocatedLabel.setText(formatCurrency(total));
        spentLabel.setText(formatCurrency(spent));
        remainingLabel.setText(formatCurrency(remaining));

        // Progress Bar Update
        utilizationBar.setProgress(utilization);
        utilizationPercentLabel.setText(String.format("%.1f%%", utilization * 100));
    }

    private String formatCurrency(double value) {
        if (Math.abs(value) >= 1000) {
            return String.format("$%.0fk", value / 1000);
        }
        return String.format("$%.0f", value);
    }

    private void applyStatusStyle(String status) {

        statusBadge.setStyle(null);
        statusBadge.getStyleClass().removeAll("status-badge-OT", "status-badge-OB", "status-badge-AT");
        if (status.equalsIgnoreCase("AT RISK")) {
            statusBadge.getStyleClass().add("status-badge-AR");
        } else if (status.equalsIgnoreCase("OVER BUDGET")) {
            statusBadge.getStyleClass().add("status-badge-OB");
        } else {
            statusBadge.getStyleClass().add("status-badge-OT");
        }
    }

    @FXML
    private void handleEditBudget() {
        // 1. Get the list of project names for the ComboBox
        List<String> projectNames = projectDAO.getAvailableProjectNames();

        // 2. Call your modified showForUpdate method
        // This will pre-fill fields and change labels to "Update".
        updateCurrentProjectBudget.showForUpdate(currentBudget, projectNames,mainPageWrapper);
    }

    @FXML
    private void handleDeleteBudget() {
        // Call your delete confirmation overlay
        deleteTransactionPopUp.show(mainPageWrapper, () -> {
            try {
                ServiceProjectBudget service = new ServiceProjectBudget();
                service.delete(currentBudget);

                // After deletion, navigate back to Overview
                FinanceController.getInstance().loadView("Overviewpage.fxml");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Returns to the main overview screen.
     */
    @FXML
    private void handleBackToOverview() {
        FinanceController.getInstance().loadView("Overviewpage.fxml");
    }

    @FXML
    private void onOverviewTabClicked() {
        updateTabStyles(overviewTab);
        showContent(overviewContent);
        System.out.println("Visual Check: Overview Active");
    }

    @FXML
    private void onExpensesTabClicked() {
        updateTabStyles(expensesTab);

        // 2. Toggle the visibility of the containers
        showContent(expensesContent);

        // 3. Trigger the dummy rows for testing
        if (transactionTabController != null) {
            transactionTabController.refreshList();
        } else {
            System.out.println("Warning: transactionTabController was not injected. Check your fx:id.");
        }  // Toggles visibility

    }

    /**
     * Generic visibility switcher
     */
    private void showContent(Node activeNode) {
        // List of all possible content nodes in the stack
        List<Node> allContents = Arrays.asList(expensesContent,overviewContent,analysisContent);

        for (Node node : allContents) {
            if (node == activeNode) {
                node.setVisible(true);
                node.setManaged(true);
            } else {
                node.setVisible(false);
                node.setManaged(false);
            }
        }
    }

    @FXML
    private void onAnalysisTabClicked() {
        updateTabStyles(analysisTab);
        showContent(analysisContent);
        System.out.println("Visual Check: Analysis Active");
    }

    /**
     * Logic to swap the .tab-active and .tab-inactive classes.
     */
    private void updateTabStyles(Button selectedBtn) {
        for (Button btn : tabButtons) {
            // Remove both classes first to ensure a clean slate
            btn.getStyleClass().removeAll("tab-active", "tab-inactive");

            if (btn == selectedBtn) {
                btn.getStyleClass().add("tab-active");
            } else {
                btn.getStyleClass().add("tab-inactive");
            }
        }
    }


}