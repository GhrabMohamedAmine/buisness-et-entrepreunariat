package com.example.testp1;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;
import java.io.IOException;

public class FinanceController {

    @FXML
    private StackPane mainStack;



    private static FinanceController instance;

    // 2. Variable to hold the ID for the PBpage to "pick up"
    private static int currentBudgetId;

    public static FinanceController getInstance() {
        return instance;
    }

    // 3. Getter for PBpageController to retrieve the ID
    public static int getCurrentBudgetId() {
        return currentBudgetId;
    }

    @FXML
    public void initialize() {
        instance = this;
        // Load the dashboard on startup
        loadView("Overviewpage.fxml");
    }

    /**
     * Specialized navigation for PBpage that sets the ID context first.
     */
    public void navigateToPBPage(int budgetId) {
        currentBudgetId = budgetId; // Store the ID before loading
        loadView("PBpage.fxml");     // Your original loading logic
    }
    public void navigateToMH() {
        loadView("MarketHub.fxml");
    }

    /**
     * Swaps the content of the main StackPane.
     * @param fxmlPath The path to the FXML file to load.
     */
    public void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();

            // Replaces the current child of the StackPane with the new view
            mainStack.getChildren().setAll(view);
            StackPane.setAlignment(view, javafx.geometry.Pos.TOP_CENTER);

            System.out.println("Navigation: Successfully loaded " + fxmlPath);
        } catch (IOException e) {
            System.err.println("Navigation Error: Could not load " + fxmlPath);
            e.printStackTrace();
        }
    }
}