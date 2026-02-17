package com.example.testp1;

import com.example.testp1.entities.ProjectBudget;
import com.example.testp1.entities.Transaction;
import com.example.testp1.model.ProjectBudgetDAO;
import com.example.testp1.services.ServiceTransaction;
import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.effect.BoxBlur;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.layout.*;
import javafx.util.Duration;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;

public class UpdateTransaction extends StackPane {

    @FXML private VBox popupCard;
    @FXML private Region dimOverlay;
    @FXML private TextField refField, costField, categoryField;
    @FXML private DatePicker transactionDatePicker;
    @FXML private Button saveButton;

    private final ServiceTransaction service = new ServiceTransaction();
    private Runnable onUpdateSuccess;
    private int currentTransactionId;
    private int currentBudgetId;
    @FXML private Label DateErrorLabel;
    @FXML private Label RefErrorLabel;
    @FXML private Label amountErrorLabel;
    @FXML private Label CatErrorLabel;
    private ProjectBudgetDAO budgetDAO = new ProjectBudgetDAO();
    private Double originalCost;

    public UpdateTransaction() {
        // Load the new dedicated FXML file
        FXMLLoader loader = new FXMLLoader(getClass().getResource("UpdateTransaction.fxml"));
        loader.setRoot(this);
        loader.setController(this);

        try {
            loader.load();
            this.setVisible(false);
            this.setMouseTransparent(true);
            setupInputValidation();
            clearErrors();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void setupInputValidation() {
        costField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*(\\.\\d*)?")) costField.setText(oldVal);
            hideError(amountErrorLabel);
        });
        refField.textProperty().addListener((obs, oldVal, newVal) -> hideError(RefErrorLabel));
        categoryField.textProperty().addListener((obs, oldVal, newVal) -> hideError(CatErrorLabel));
        transactionDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> hideError(DateErrorLabel));
    }

    /**
     * Entry point: Pre-fills info and triggers the animation.
     */
    public void show(Transaction t, Node background) {
        this.currentTransactionId = t.getId();
        this.currentBudgetId = t.getProjectBudgetId();
        this.currentTransactionId = t.getId();
        this.currentBudgetId = t.getProjectBudgetId();
        this.originalCost = t.getCost();

        // 1. Pre-fill the exact same FXIDs from the entity
        refField.setText(t.getReference());
        costField.setText(String.valueOf(t.getCost()));
        categoryField.setText(t.getExpenseCategory());
        transactionDatePicker.setValue(t.getDateStamp());

        // 2. Apply visual sequence
        triggerOpenSequence(background);
    }

    private void triggerOpenSequence(Node backgroundToBlur) {
        this.setVisible(true);
        this.setMouseTransparent(false);

        // Standard Nexum Blur + Dim
        BoxBlur blur = new BoxBlur(8, 8, 3);
        ColorAdjust dim = new ColorAdjust();
        dim.setBrightness(-0.3);
        dim.setInput(blur);
        backgroundToBlur.setEffect(dim);

        ScaleTransition scale = new ScaleTransition(Duration.millis(300), popupCard);
        scale.setFromX(0.7); scale.setFromY(0.7);
        scale.setToX(1.0); scale.setToY(1.0);

        FadeTransition fade = new FadeTransition(Duration.millis(300), dimOverlay);
        fade.setFromValue(0); fade.setToValue(1);

        new ParallelTransition(scale, fade).play();
    }

    @FXML
    public void hide() {
        if (getParent() instanceof StackPane) {
            StackPane parent = (StackPane) getParent();
            parent.getChildren().get(0).setEffect(null); // Clear full-page blur
        }

        ScaleTransition scale = new ScaleTransition(Duration.millis(200), popupCard);
        scale.setToX(0.7); scale.setToY(0.7);
        FadeTransition fade = new FadeTransition(Duration.millis(200), dimOverlay);
        fade.setToValue(0);

        ParallelTransition anim = new ParallelTransition(scale, fade);
        anim.setOnFinished(e -> {
            this.setVisible(false);
            this.setMouseTransparent(true);
        });
        anim.play();
    }

    @FXML
    private void handleUpdate() {
        clearErrors();
        boolean isValid = true;

        // 1. Regex Reference Validation (TX-XXXXXXX)
        String ref = refField.getText().trim();
        if (!ref.matches("^TX-\\d{7}$")) {
            setError(RefErrorLabel, "Format: TX-XXXXXXX (7 digits)");
            isValid = false;
        }

        // 2. Category Validation
        String cat = categoryField.getText().trim();
        if (cat.isEmpty()) {
            setError(CatErrorLabel, "Category is required");
            isValid = false;
        }

        // 3. Date Timeline Validation
        LocalDate txDate = transactionDatePicker.getValue();
        if (txDate == null) {
            setError(DateErrorLabel, "Date is required");
            isValid = false;
        } else {
            LocalDate dueDate = budgetDAO.getDueDateById(currentBudgetId);
            if (dueDate != null) {
                if (txDate.isAfter(dueDate)) {
                    setError(DateErrorLabel, "Exceeds budget deadline (" + dueDate + ")");
                    isValid = false;
                } else if (txDate.isBefore(dueDate.minusYears(1))) {
                    setError(DateErrorLabel, "Cannot be >1 year before deadline");
                    isValid = false;
                }
            }
        }

        // 4. Smart Budget Limit Validation (110% Rule)
        String costStr = costField.getText().trim();
        double newCost = 0;
        if (costStr.isEmpty()) {
            setError(amountErrorLabel, "Amount is required");
            isValid = false;
        } else {
            try {
                newCost = Double.parseDouble(costStr);
                ProjectBudget budget = budgetDAO.getById(currentBudgetId);
                if (budget != null) {
                    double maxAllowed = budget.getTotalBudget() * 1.10;
                    // Formula: (Current Spend - Old Cost) + New Cost
                    double projectedTotal = (budget.getActualSpend() - originalCost) + newCost;

                    if (newCost <= 0) {
                        setError(amountErrorLabel, "Amount must be positive");
                        isValid = false;
                    } else if (projectedTotal > maxAllowed) {
                        setError(amountErrorLabel, String.format("Exceeds 110%% limit ($%,.2f)", maxAllowed));
                        isValid = false;
                    }
                }
            } catch (NumberFormatException e) {
                setError(amountErrorLabel, "Invalid number or DB error");
                isValid = false;
            }
        }

        // 5. Save if all conditions met
        if (isValid) {
            try {
                Transaction t = new Transaction();
                t.setId(currentTransactionId);
                t.setReference(ref);
                t.setCost(newCost);
                t.setExpenseCategory(cat);
                t.setDateStamp(txDate);
                t.setProjectBudgetId(currentBudgetId);

                service.update(t);
                if (onUpdateSuccess != null) onUpdateSuccess.run();
                hide();
            } catch (SQLException e) {
                setError(RefErrorLabel, "SQL Error: " + e.getMessage());
            }
        }
    }

    public void setOnUpdateSuccess(Runnable callback) {
        this.onUpdateSuccess = callback;
    }
    private void clearErrors() {
        hideError(DateErrorLabel);
        hideError(amountErrorLabel);
        hideError(RefErrorLabel);
        hideError(CatErrorLabel);
    }

    private void setError(Label label, String message) {
        label.setText(message);
        label.setVisible(true);
        label.setManaged(true);
    }

    private void hideError(Label label) {
        label.setText("");
        label.setVisible(false);
        label.setManaged(false);
    }
}