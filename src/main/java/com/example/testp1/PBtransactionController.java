package com.example.testp1;

import com.example.testp1.entities.Transaction;
import com.example.testp1.model.TransactionDAO;
import com.example.testp1.services.ServiceTransaction;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class PBtransactionController {

    @FXML
    private VBox transactionListContainer;
    private PBpageController parentController;
    private final ServiceTransaction service = new ServiceTransaction();

    public void setParentController(PBpageController parent) {
        this.parentController = parent;
    }

    /**
     * Call this when you have real data from the database.
     */

    public void refreshList() {
        // 1. Clear the static/dummy rows from the VBox
        transactionListContainer.getChildren().clear();

        try {
            int activeBudgetId = parentController.getCurrentBudget().getId();
            // 2. Fetch real data from the database using the service
            List<Transaction> realTransactions = service.getTransactionsByBudget(activeBudgetId);

            // 3. Loop through the results and create a row for each one
            if (realTransactions != null && !realTransactions.isEmpty()) {
                for (Transaction t : realTransactions) {
                    loadRow(t);
                }
            } else {
                // Optional: Show a "No Transactions Found" label if the list is empty
                System.out.println("No transactions found for budget ID: " + activeBudgetId);
            }

        } catch (SQLException e) {
            System.err.println("Error fetching transactions: " + e.getMessage());
        }
    }
    public void requestEdit(Transaction t) {
        // The Tab forwards the data to the Page Controller
        parentController.triggerUpdateTransactionSequence(t);
    }

    public void requestDelete(Transaction t) {
        if (parentController != null) {
            // Forward the specific transaction to the Boss's delete sequence
            parentController.triggerDeleteTransactionSequence(t);
        }
    }

    /**
     * Internal helper to load the row FXML and bind the width.
     */
    private void loadRow(Transaction t) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("TransactionRow.fxml"));
            HBox row = loader.load();

            // Dimensional Alignment: Force the row to match the parent container width
            row.prefWidthProperty().bind(transactionListContainer.widthProperty());

            // Use the controller you shared to set the data
            TransactionRowController rowController = loader.getController();
            if (t != null) {
                rowController.setTransactionData(t);
                rowController.setTransaction(t);
                rowController.setParentListController(this);
            }

            transactionListContainer.getChildren().add(row);

        } catch (IOException e) {
            System.err.println("Error loading TransactionRow.fxml: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAddNewExpense() {
        // Check if the link to the Boss (PBpageController) is active
        if (parentController != null) {
            // Call the show function we just created in the Boss
            parentController.triggerAddTransactionSequence();
        } else {
            System.err.println("Communication Error: Parent Controller is not linked!");
        }
    }
}