package tezfx.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import tezfx.model.Project;
import tezfx.model.sql;
import java.sql.Date;
import java.time.LocalDate;

public class AddProjectController {
    @FXML private TextField nameField;
    @FXML private TextArea descField;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker dueDatePicker;
    @FXML private Slider progressSlider;
    @FXML private TextField budgetField;

    private HelloController parentController;;


    private sql dao = new sql();

    @FXML
    private void handleCreate() {
        if (startDatePicker.getValue() == null || dueDatePicker.getValue() == null) {
            System.out.println("❌ Error: You must pick both dates!");
            return; // This is the most important line!
        }
        // 1. Check for NULLS before doing anything else
        if (nameField.getText().isEmpty() ||
                startDatePicker.getValue() == null ||
                dueDatePicker.getValue() == null) {

            System.out.println("Please fill required fields!");
            return; // <--- CRITICAL: This stops the code from crashing!
        }

        try {
            // 2. Now it is safe to grab the values
            String name = nameField.getText();
            String desc = descField.getText();
            int progress = (int) progressSlider.getValue();

            // Convert LocalDate to String for your Project constructor
            String startStr = startDatePicker.getValue().toString();
            String dueStr = dueDatePicker.getValue().toString();

            Project newProject = new Project(name, desc, progress, 0.0, startStr, dueStr, 1, 1);

            // 3. Save and Refresh
            dao.addProject(newProject);

            // 4. Close the window
            closeoverlay();

        } catch (Exception e) {
            System.err.println("Database Error: " + e.getMessage());
        }
    }


    @FXML
    private void handleCancel() {
        closeoverlay();
    }
    public void setParentController(HelloController parentController) {
        this.parentController = parentController;
    }

    private void closeoverlay() {
        StackPane contentArea = MainController.getStaticContentArea();
        if (!contentArea.getChildren().isEmpty()) {
            contentArea.getChildren().remove(0);
        }
    }
}