package com.example.testp1;

import com.example.testp1.entities.Article;
import com.example.testp1.entities.BudgetProfil;
import com.example.testp1.entities.ProjectBudget;
import com.example.testp1.model.ProjectDAO;
import com.example.testp1.services.ServiceBudgetProfil;
import com.example.testp1.services.ServiceMarketingHub;
import com.example.testp1.services.ServiceProjectBudget;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class OverviewController {

    @FXML
    private StatCard cardTotal;
    @FXML private StatCard cardExpense;
    @FXML private StatCard cardRemaining;
    @FXML private StatCard cardUtil;
    @FXML
    private GridPane budgetGrid;
    private final ServiceProjectBudget budgetService = new ServiceProjectBudget();
    private final ProjectDAO projectDAO = new ProjectDAO();

    @FXML
    private ActionTrayController actionTrayController; // Injected by JavaFX

    @FXML
    private AddProjectBudget addProjectBudget;
    @FXML
    private AnchorPane ovContent;
    @FXML
    private AddBudgetProfile addBudgetProfile;
    @FXML
    private Warning warningOverlay;
    @FXML
    private DeleteConfirmation deleteConfirmOverlay;
    @FXML
    private VBox newsSidebarVBox;
    @FXML
    private VBox placeholderContent;

    private ServiceMarketingHub marketService = new ServiceMarketingHub();
    @FXML
    private TabPane sidebarTabPane;
    @FXML
    private Tab miscTab;

    @FXML
    public void initialize() {

        if (sidebarTabPane != null) {
            sidebarTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
                if (newTab == miscTab) {
                    newsSidebarVBox.getChildren().clear();
                    // Only load if the VBox is empty (prevents re-fetching every click)
                    if (newsSidebarVBox.getChildren().isEmpty()) {
                        System.out.println("it reached thourgh here");
                        syncNewsFeed();
                    }
                }
            });
        }

        if (actionTrayController != null) {
            actionTrayController.setOverviewController(this);
        }

        loadProjectBudgets();
        updateDashboardHeader();
        addBudgetProfile.setOnSaveSuccess(() -> {
            try {
                new ServiceBudgetProfil().syncdata();
                updateDashboardHeader();
                System.out.println("Profile saved and expenses synchronized.");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
        addProjectBudget.setOnSaveSuccess(this::loadProjectBudgets);
    }
    @FXML
    public void handleAddClick(){
        List<String> names = projectDAO.getAvailableProjectNames();
        addProjectBudget.show(names,ovContent);

    }
    @FXML
    public void handleAddBPClick(){
        try {
            ServiceBudgetProfil service = new ServiceBudgetProfil();
            if (service.getActiveProfile() != null) {
                // Profile exists: Block access
                warningOverlay.show(ovContent);
            } else {
                // No profile: Proceed with normal add
                addBudgetProfile.show(ovContent);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    @FXML
    public void handleUpdateProfileClick() {
        try {
            ServiceBudgetProfil service = new ServiceBudgetProfil();
            BudgetProfil active = service.getActiveProfile();

            if (active != null) {
                // Open it with data pre-filled
                addBudgetProfile.showForUpdate(active, ovContent);
            } else {
                System.out.println("No profile found to update. Use 'Set New' instead.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleDeleteProfile() {
        // We call the overlay and pass the actual DB logic as a lambda
        deleteConfirmOverlay.show(ovContent, () -> {
            try {
                ServiceBudgetProfil service = new ServiceBudgetProfil();
                BudgetProfil active = service.getActiveProfile();

                if (active != null) {
                    service.delete(active);
                    System.out.println("Profile Deleted Successfully");

                    // Refresh everything
                    updateDashboardHeader();
                    // Any other UI refresh logic here
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }
    @FXML
    public void navigateToMH(){
        FinanceController.getInstance().navigateToMH();
    }

    public void syncNewsFeed() {
        System.out.println("[DEBUG] syncNewsFeed triggered...");

        marketService.getLatestBusinessNews().thenAccept(newsResponse -> {
            Platform.runLater(() -> {
                // Check if API actually sent data
                if (newsResponse == null || newsResponse.articles == null || newsResponse.articles.isEmpty()) {
                    System.out.println("[DEBUG] API returned ZERO articles. Check your API Key!");
                    return;
                }

                System.out.println("[DEBUG] Received " + newsResponse.articles.size() + " articles.");
                newsSidebarVBox.getChildren().clear();

                for (Article article : newsResponse.articles) {
                    try {
                        // Try using an absolute path to be safe
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/testp1/NewsCard.fxml"));
                        HBox card = loader.load();

                        NewsCardController controller = loader.getController();
                        controller.setArticleData(article);

                        newsSidebarVBox.getChildren().add(card);
                    } catch (Exception e) {
                        System.err.println("[DEBUG] Failed to load NewsCard.fxml for article: " + article.title);
                        e.printStackTrace(); // This will tell us if a dependency (like Ikonli) is missing
                    }
                }
                System.out.println("[DEBUG] Injection complete. " + newsSidebarVBox.getChildren().size() + " cards added.");
            });
        }).exceptionally(ex -> {
            System.err.println("[DEBUG] API call failed: " + ex.getMessage());
            ex.printStackTrace();
            return null;
        });
    }

    private void updateDashboardHeader() {
        try {
            ServiceBudgetProfil profilService = new ServiceBudgetProfil();
            BudgetProfil profil = profilService.getActiveProfile();

            if (profil == null) {
                // CASE: N/A - No profile exists in the database yet
                cardTotal.setStatData("Total Budget", "N/A", "", "mdi2w-wallet", "gray");
                cardExpense.setStatData("Total Expenses", "N/A", "", "mdi2t-trending-down", "gray");
                cardRemaining.setStatData("Remaining", "N/A", "", "mdi2t-trending-up", "gray");
                cardUtil.setStatData("Utilization", "N/A", "", "mdi2p-percent-outline", "gray");
            } else {
                // CASE: Data exists - Calculating values from BudgetProfil entity
                double limit = profil.getBudgetDisposable();
                double spent = profil.getTotalExpense();
                double remaining = limit - spent;
                double util = (limit > 0) ? (spent / limit) * 100 : 0;

                // Mapping to your card method: (Title, Value, Change, Icon, Color)
                cardTotal.setStatData("Total Budget",
                        String.format("$%,.0f", limit),
                        "", "mdi2w-wallet", "blue");

                cardExpense.setStatData("Total Expenses",
                        String.format("$%,.0f", spent),
                        "", "mdi2t-trending-down", "red");

                cardRemaining.setStatData("Remaining",
                        String.format("$%,.0f", remaining),
                        "", "mdi2t-trending-up", "green");

                // For utilization, we can dynamically pick the color based on the percentage
                String utilColor = (util > 90) ? "red" : (util > 70) ? "orange" : "purple";
                cardUtil.setStatData("Utilization",
                        String.format("%.1f%%", util),
                        "", "mdi2p-percent-outline", utilColor);
            }
        } catch (SQLException e) {
            System.err.println("Dashboard Header Update Failed: " + e.getMessage());
            e.printStackTrace();
        }
    }



    private void loadProjectBudgets() {
        try {
            // Fetch the data
            List<ProjectBudget> budgets = budgetService.getAll();

            // Clear to avoid overlaps on refresh
            budgetGrid.getChildren().clear();

            int column = 0;
            int row = 0;
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy");

            for (ProjectBudget budget : budgets) {
                // Instantiate your custom self-loading component
                ProjectBudgetCard card = new ProjectBudgetCard();
                String name = projectDAO.getNameById(budget.getProjectId());

                // Logic for the status badges based on your reference image
                double remaining = budget.getTotalBudget() - budget.getActualSpend();
                String status = "ON TRACK";
                if (remaining < 0) status = "OVER BUDGET";
                else if (remaining < (budget.getTotalBudget() * 0.10)) status = "AT RISK";
                String formattedDate = "No Date Set";
                if (budget.getDueDate() != null) {
                    formattedDate = budget.getDueDate().format(formatter);
                }

                // Map data to the card labels
                card.setProjectData(
                        budget.getId(),
                        budget.getName(), // ID for now
                        name,              // Placeholder type
                        status,
                        String.format("$%,.0f", budget.getTotalBudget()),
                        String.format("$%,.0f", budget.getActualSpend()),
                        String.format("$%,.0f", remaining),
                        formattedDate                       // Placeholder date
                );
                card.setOnMouseClicked(event -> {
                    // Pass the ID and tell the central controller to move
                    FinanceController.getInstance().navigateToPBPage(budget.getId());
                });

                // Add to the grid
                budgetGrid.add(card, column, row);
                GridPane.setValignment(card, javafx.geometry.VPos.TOP);
                GridPane.setHalignment(card, javafx.geometry.HPos.LEFT);

                // Grid management: 2 columns wide
                column++;
                if (column == 2) {
                    column = 0;
                    row++;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
