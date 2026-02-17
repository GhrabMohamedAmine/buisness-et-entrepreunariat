package com.example.testp1;

import com.example.testp1.entities.BudgetProfil;
import com.example.testp1.services.ServiceBudgetProfil;
import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.effect.BoxBlur;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.layout.*;
import javafx.util.Duration;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Year;

public class AddBudgetProfile extends StackPane {

    @FXML private VBox popupCard;
    @FXML private Region dimOverlay;
    @FXML private TextField yearField, totalBudgetField;
    @FXML private Label titleLabel;
    @FXML private Button saveButton;
    @FXML private Label yearErrorLabel;
    @FXML private Label totalErrorLabel;

    private Runnable onSaveSuccess;
    private int currentProfileId;

    public AddBudgetProfile() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("SetBudgetProfil.fxml"));
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

    public void show(Node backgroundToBlur) {
        // 1. Clear fields ONLY if this is a fresh "Add"
        yearField.clear();
        totalBudgetField.clear();
        this.currentProfileId = 0; // Reset ID for safety
        titleLabel.setText("Set Global Budget");
        saveButton.setText("Initialize Profile");

        // 2. Run the common visibility and animation logic
        triggerOpenSequence(backgroundToBlur);
    }

    public void showForUpdate(BudgetProfil p, Node background) {
        this.currentProfileId = p.getId();
        titleLabel.setText("Update Fiscal Profile");
        saveButton.setText("Save Changes");

        // 1. Fill the fields
        yearField.setText(String.valueOf(p.getFiscalYear().getValue()));
        totalBudgetField.setText(String.valueOf(p.getBudgetDisposable()));

        // 2. Run ONLY the visibility and animation logic (Don't call show()!)
        triggerOpenSequence(background);
    }

    /**
     * Common logic for opening the popup without clearing data
     */
    private void triggerOpenSequence(Node backgroundToBlur) {
        this.setVisible(true);
        this.setMouseTransparent(false);

        // Background Blur logic
        BoxBlur blur = new BoxBlur(8, 8, 3);
        ColorAdjust dim = new ColorAdjust();
        dim.setBrightness(-0.3);
        dim.setInput(blur);
        backgroundToBlur.setEffect(dim);

        // Entry Animations
        ScaleTransition scale = new ScaleTransition(Duration.millis(300), popupCard);
        scale.setFromX(0.7); scale.setFromY(0.7);
        scale.setToX(1.0); scale.setToY(1.0);

        FadeTransition fade = new FadeTransition(Duration.millis(300), dimOverlay);
        fade.setFromValue(0); fade.setToValue(1);

        new ParallelTransition(scale, fade).play();
    }

    @FXML
    public void hide() {
        // Remove background effects
        clearErrors();
        if (getParent() instanceof StackPane) {
            StackPane parent = (StackPane) getParent();
            parent.getChildren().get(0).setEffect(null);
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

    private void setupInputValidation() {
        yearField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) yearField.setText(oldVal);
            clearErrors();
        });
        totalBudgetField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*(\\.\\d*)?")) totalBudgetField.setText(oldVal);
            clearErrors();
        });
    }

    @FXML
    private void handleSave() {
        clearErrors();
        boolean isValid = true;

        // 1. Validate Fiscal Year
        String yearText = yearField.getText().trim();
        if (yearText.isEmpty()) {
            setError(yearErrorLabel, "Fiscal year is required.");
            isValid = false;
        } else {
            int yearValue = Integer.parseInt(yearText);
            int currentYear = Year.now().getValue();
            if (yearValue < 2000 || yearValue > 2100) {
                setError(yearErrorLabel, "Please enter a valid year (2000-2100).");
                isValid = false;
            }
        }

        // 2. Validate Budget Amount
        String totalText = totalBudgetField.getText().trim();
        if (totalText.isEmpty()) {
            setError(totalErrorLabel, "Budget amount is required.");
            isValid = false;
        } else {
            try {
                double disposable = Double.parseDouble(totalText);
                if (disposable <= 0) {
                    setError(totalErrorLabel, "Budget must be greater than 0.");
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                setError(totalErrorLabel, "Invalid number format.");
                isValid = false;
            }
        }

        // 3. Process Save if valid
        if (isValid) {
            try {
                Year year = Year.of(Integer.parseInt(yearField.getText()));
                double disposable = Double.parseDouble(totalBudgetField.getText());
                ServiceBudgetProfil service = new ServiceBudgetProfil();

                if (currentProfileId == 0) {
                    service.add(new BudgetProfil(year, disposable, 0.0, 0.0f));
                } else {
                    BudgetProfil updated = new BudgetProfil(currentProfileId, year, disposable, 0.0, 0.0f);
                    service.update(updated);
                }

                if (onSaveSuccess != null) onSaveSuccess.run();
                hide();
                currentProfileId = 0;
            } catch (SQLException e) {
                // Handle business rule violations (e.g., unique constraints)
                setError(totalErrorLabel, "Database Error: " + e.getMessage());
            }
        }
    }

    public void setOnSaveSuccess(Runnable callback) {
        this.onSaveSuccess = callback;
    }

    private void clearErrors() {
        hideError(yearErrorLabel);
        hideError(totalErrorLabel);
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