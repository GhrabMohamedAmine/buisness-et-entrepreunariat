package com.example.testp1;

import com.example.testp1.entities.ProjectBudget;
import com.example.testp1.model.ProjectDAO;
import com.example.testp1.services.ServiceProjectBudget;
import com.example.utils.AtlantaDatePicker;
import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.effect.BoxBlur;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.layout.*;
import javafx.util.Duration;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class AddProjectBudget extends StackPane {

    @FXML private VBox popupCard;
    @FXML private Region dimOverlay;
    @FXML private ComboBox<String> projectComboBox; // Assuming project names for now
    @FXML private TextField budgetNameField, totalAmountField;
    @FXML private DatePicker dueDatePicker;
    private Runnable onSaveSuccess;
    private ProjectBudget currentProjectBudget;
    @FXML
    private Label titleLabel;
    @FXML
    private Label bodyLabel;
    @FXML
    private Button saveButton;
    @FXML private Label DateErrorLabel;
    @FXML private Label PNameErrorLabel;
    @FXML private Label nameErrorLabel;
    @FXML private Label amountErrorLabel;

    public AddProjectBudget() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("AddProjectBudget.fxml"));
        loader.setRoot(this);
        loader.setController(this); // This links your methods to the FXML

        try {
            loader.load();
            this.setVisible(false);
            setupInputValidation();
            clearErrors();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void show(List<String> projectNames, javafx.scene.Node backgroundToBlur) {
        this.setVisible(true);
        projectComboBox.getItems().setAll(projectNames); // Populate the list

        javafx.scene.effect.BoxBlur blur = new javafx.scene.effect.BoxBlur(8, 8, 3);
        javafx.scene.effect.ColorAdjust dim = new javafx.scene.effect.ColorAdjust();
        dim.setBrightness(-0.3);
        dim.setInput(blur);

        // Apply it ONLY to the dashboard background
        backgroundToBlur.setEffect(dim);

        FadeTransition fade = new FadeTransition(Duration.millis(300), dimOverlay);
        fade.setFromValue(0);
        fade.setToValue(1);

        ScaleTransition scale = new ScaleTransition(Duration.millis(300), popupCard);
        scale.setFromX(0.7);
        scale.setFromY(0.7);
        scale.setToX(1.0);
        scale.setToY(1.0);

        new ParallelTransition(fade, scale).play();
    }

    public void showForUpdate(ProjectBudget budget, List<String> projectNames, javafx.scene.Node backgroundToBlur) {
        this.currentProjectBudget = budget; // Set the budget we are editing

        // 1. Populate Fields
        projectComboBox.getItems().setAll(projectNames);

        // Find and select the current project name based on ID
        String currentProjectName = projectDao.getNameById(budget.getProjectId());
        projectComboBox.setValue(currentProjectName);

        budgetNameField.setText(budget.getName());
        totalAmountField.setText(String.valueOf(budget.getTotalBudget()));
        dueDatePicker.setValue(budget.getDueDate());

        // 2. Change Labels for "Update" Mode
        if (titleLabel != null) titleLabel.setText("Update Project Budget");
        if (bodyLabel != null) bodyLabel.setText("Set the new values of the Budget");
        if (saveButton != null) saveButton.setText("Save Changes");

        // 3. Trigger the standard show animation
        this.show(projectNames, backgroundToBlur);
    }

    @FXML
    public void hide() {
        FadeTransition fade = new FadeTransition(Duration.millis(200), dimOverlay);
        fade.setToValue(0);

        ScaleTransition scale = new ScaleTransition(Duration.millis(200), popupCard);
        scale.setToX(0.7);
        scale.setToY(0.7);

        ParallelTransition anim = new ParallelTransition(fade, scale);
        if (getParent() instanceof StackPane) {
            StackPane parent = (StackPane) getParent();
            // The first child of our StackPane is the AnchorPane dashboard
            parent.getChildren().get(0).setEffect(null);
        }
        anim.setOnFinished(e -> this.setVisible(false));
        anim.play();
    }

    private final ProjectDAO projectDao = new ProjectDAO(); // Your DAO
    private final ServiceProjectBudget budgetService = new ServiceProjectBudget();


    private void setupInputValidation() {
        totalAmountField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*(\\.\\d*)?")) totalAmountField.setText(oldVal);
            hideError(amountErrorLabel);
        });

        budgetNameField.textProperty().addListener((obs, oldVal, newVal) -> hideError(nameErrorLabel));
        projectComboBox.valueProperty().addListener((obs, oldVal, newVal) -> hideError(PNameErrorLabel));
        dueDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> hideError(DateErrorLabel));
    }

    @FXML
    private void handleSave() {
        clearErrors();
        boolean isValid = true;

        // 1. Validate Project Selection
        String selectedProjectName = projectComboBox.getValue();
        if (selectedProjectName == null) {
            setError(PNameErrorLabel, "Please select a project.");
            isValid = false;
        }

        // 2. Validate Budget Title
        String budgetTitle = budgetNameField.getText().trim();
        if (budgetTitle.isEmpty()) {
            setError(nameErrorLabel, "Budget name is required.");
            isValid = false;
        }

        // 3. Validate Amount
        String amountStr = totalAmountField.getText().trim();
        double amount = 0;
        if (amountStr.isEmpty()) {
            setError(amountErrorLabel, "Amount is required.");
            isValid = false;
        } else {
            try {
                amount = Double.parseDouble(amountStr);
                if (amount <= 0) {
                    setError(amountErrorLabel, "Amount must be positive.");
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                setError(amountErrorLabel, "Invalid number format.");
                isValid = false;
            }
        }

        // 4. Validate Due Date against Project End Date
        LocalDate dueDate = dueDatePicker.getValue();
        if (dueDate == null) {
            setError(DateErrorLabel, "Due date is required.");
            isValid = false;
        } else if (selectedProjectName != null) {
            // Using your new DAO function!
            LocalDate projectEndDate = projectDao.getEndDateByName(selectedProjectName);
            if (projectEndDate != null && dueDate.isAfter(projectEndDate)) {
                setError(DateErrorLabel, "Due date cannot exceed project end (" + projectEndDate + ").");
                isValid = false;
            }
        }

        // 5. Final Processing
        if (isValid) {
            int projectId = projectDao.getIdByName(selectedProjectName);
            try {
                if (currentProjectBudget == null) {
                    // MODE: ADD
                    ProjectBudget newBudget = new ProjectBudget();
                    newBudget.setProjectId(projectId);
                    newBudget.setName(budgetTitle);
                    newBudget.setTotalBudget(amount);
                    newBudget.setActualSpend(0.0);
                    newBudget.setDueDate(dueDate);
                    newBudget.setStatus("ON TRACK");
                    budgetService.add(newBudget);
                } else {
                    // MODE: UPDATE
                    currentProjectBudget.setProjectId(projectId);
                    currentProjectBudget.setName(budgetTitle);
                    currentProjectBudget.setTotalBudget(amount);
                    currentProjectBudget.setDueDate(dueDate);
                    budgetService.update(currentProjectBudget);
                }

                if (onSaveSuccess != null) onSaveSuccess.run();
                hide();
                resetState();
            } catch (SQLException e) {
                setError(nameErrorLabel, "Database Error: " + e.getMessage());
            }
        }
    }
    private void resetState() {
        currentProjectBudget = null;
        budgetNameField.clear();
        totalAmountField.clear();
        dueDatePicker.setValue(null);
        if (titleLabel != null) titleLabel.setText("Configure Budget");
        if (saveButton != null) saveButton.setText("Create Budget");
        if (bodyLabel != null) bodyLabel.setText("Associate a new budget with an existing project");
    }

    public void setOnSaveSuccess(Runnable callback) {
        this.onSaveSuccess = callback;
    }

    private void clearErrors() {
        hideError(DateErrorLabel);
        hideError(amountErrorLabel);
        hideError(PNameErrorLabel);
        hideError(nameErrorLabel);
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