package com.example.testp1;

import com.example.testp1.entities.ProjectBudget;
import com.example.testp1.entities.Transaction;
import com.example.testp1.model.ProjectBudgetDAO;
import com.example.testp1.services.ServiceTransaction;
import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.effect.BoxBlur;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;

public class AddTransaction extends StackPane {

    @FXML private VBox popupCard;
    @FXML private Region dimOverlay;
    @FXML private TextField refField, costField, categoryField;
    @FXML private DatePicker transactionDatePicker;
    @FXML private Label titleLabel;
    @FXML private Button saveButton;
    @FXML private Label DateErrorLabel;
    @FXML private Label RefErrorLabel;
    @FXML private Label amountErrorLabel;
    @FXML private Label CatErrorLabel;
    private ProjectBudgetDAO budgetDAO = new ProjectBudgetDAO();

    private final ServiceTransaction service = new ServiceTransaction();
    private Runnable onSaveSuccess;
    private int currentBudgetId;


    public AddTransaction() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("AddTransaction.fxml"));
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
     * Entry point for adding a new transaction.
     * @param budgetId The ID of the budget receiving this expense.
     * @param background The mainPageWrapper to blur.
     */
    public void show(int budgetId, Node background) {
        this.currentBudgetId = budgetId;

        // Reset fields for a clean entry
        refField.clear();
        costField.clear();
        categoryField.clear();
        transactionDatePicker.setValue(LocalDate.now());

        triggerOpenSequence(background);
    }

    /**
     * Premium Open Sequence: Applies Blur, Dimming, and Scale Animations.
     */
    private void triggerOpenSequence(Node backgroundToBlur) {
        this.setVisible(true);
        this.setMouseTransparent(false);

        // 1. Background Visuals: Blur + Dim
        BoxBlur blur = new BoxBlur(8, 8, 3);
        ColorAdjust dim = new ColorAdjust();
        dim.setBrightness(-0.3);
        dim.setInput(blur);
        backgroundToBlur.setEffect(dim);

        // 2. Animations: Scale the card and Fade the overlay
        ScaleTransition scale = new ScaleTransition(Duration.millis(300), popupCard);
        scale.setFromX(0.7); scale.setFromY(0.7);
        scale.setToX(1.0); scale.setToY(1.0);

        FadeTransition fade = new FadeTransition(Duration.millis(300), dimOverlay);
        fade.setFromValue(0); fade.setToValue(1);

        new ParallelTransition(scale, fade).play();
    }

    @FXML
    public void hide() {
        // 1. Remove background effects by finding the sibling in the StackPane
        if (getParent() instanceof StackPane) {
            StackPane parent = (StackPane) getParent();
            // Assumes the first child is the mainPageWrapper
            parent.getChildren().get(0).setEffect(null);
        }

        // 2. Exit Animations
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
    private void handleSave() {
        clearErrors();
        boolean isValid = true;

        // 1. Reference Check (Regex: TX- followed by 7 numbers)
        String ref = refField.getText().trim();
        if (!ref.matches("^TX-\\d{7}$")) {
            setError(RefErrorLabel, "Invalid format. Use TX-XXXXXXX (7 digits).");
            isValid = false;
        }

        // 2. Category Check
        String cat = categoryField.getText().trim();
        if (cat.isEmpty()) {
            setError(CatErrorLabel, "Category is required.");
            isValid = false;
        }

        // 3. Date Timeline Logic
        LocalDate txDate = transactionDatePicker.getValue();
        if (txDate == null) {
            setError(DateErrorLabel, "Please select a date.");
            isValid = false;
        } else {
            LocalDate dueDate = budgetDAO.getDueDateById(currentBudgetId);
            if (dueDate != null) {
                if (txDate.isAfter(dueDate)) {
                    setError(DateErrorLabel, "Date exceeds budget deadline (" + dueDate + ").");
                    isValid = false;
                } else if (txDate.isBefore(dueDate.minusYears(1))) {
                    setError(DateErrorLabel, "Date cannot be more than 1 year prior to deadline.");
                    isValid = false;
                }
            }
        }

        // 4. Amount and 110% Over-Budget Guard
        String costStr = costField.getText().trim();
        if (costStr.isEmpty()) {
            setError(amountErrorLabel, "Amount is required.");
            isValid = false;
        } else {
            try {
                double inputCost = Double.parseDouble(costStr);
                ProjectBudget budget = budgetDAO.getById(currentBudgetId);

                if (budget != null) {
                    double maxAllowed = budget.getTotalBudget() * 1.10;
                    double projectedTotal = budget.getActualSpend() + inputCost;

                    if (inputCost <= 0) {
                        setError(amountErrorLabel, "Amount must be positive.");
                        isValid = false;
                    } else if (projectedTotal > maxAllowed) {
                        setError(amountErrorLabel, String.format("Exceeds 110%% limit (Max Total: $%,.2f)", maxAllowed));
                        isValid = false;
                    }
                }
            } catch (NumberFormatException e) {
                setError(amountErrorLabel, "Invalid amount or DB error.");
                isValid = false;
            }
        }

        // 5. Execution
        if (isValid) {
            try {
                Transaction t = new Transaction();
                t.setReference(ref);
                t.setCost(Double.parseDouble(costStr));
                t.setExpenseCategory(cat);
                t.setDateStamp(txDate);
                t.setProjectBudgetId(currentBudgetId);
                t.setDescription(0);

                service.add(t);
                if (onSaveSuccess != null) onSaveSuccess.run();
                hide();
            } catch (SQLException e) {
                setError(RefErrorLabel, "Database Error: " + e.getMessage());
            }
        }
    }

    public void setOnSaveSuccess(Runnable callback) {
        this.onSaveSuccess = callback;
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