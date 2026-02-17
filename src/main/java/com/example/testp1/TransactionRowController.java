package com.example.testp1;

import com.example.testp1.entities.Transaction;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import java.time.format.DateTimeFormatter;

public class TransactionRowController {

    @FXML private Label Reference;
    @FXML private Label amountLabel;
    @FXML private Label dateLabel;
    @FXML private Label categoryLabel;
    private Transaction transaction;
    private PBtransactionController parentListController;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public void setParentListController(PBtransactionController parentListController) {
        this.parentListController = parentListController;
    }

    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

    public void setTransactionData(Transaction transaction) {
        // 1. Map Reference
        Reference.setText(transaction.getReference());

        // 2. Map Cost
        // Using red color as defined in your FXML for expenses
        amountLabel.setText(String.format("-$%,.2f", Math.abs(transaction.getCost())));

        // 3. Map Date
        if (transaction.getDateStamp() != null) {
            dateLabel.setText(transaction.getDateStamp().format(formatter));
        }

        // 4. Map Category
        // You'll need to link this Label in your FXML with fx:id="categoryLabel"
        if (categoryLabel != null) {
            categoryLabel.setText(transaction.getExpenseCategory());
        }
    }

    @FXML
    private void onEditTransaction() {
        this.parentListController.requestEdit(transaction);
    }

    @FXML
    private void onDeleteTransaction() {
        if (parentListController != null && transaction != null) {
            parentListController.requestDelete(this.transaction);
        }
    }
}