package tezfx.controller;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import tezfx.model.Task;
import tezfx.model.User;
import tezfx.model.sql;

import java.time.LocalDate;
import java.util.List;

public class AddTaskController {
    @FXML private TextField titleField;
    @FXML private TextArea descField; // If you don't have a desc field in FXML, use "" in the constructor
    @FXML private DatePicker dueDatePicker;

    @FXML private ComboBox<User> userCombo; // Use a User object, not just a String
    @FXML private ComboBox<String> statusCombo;
    @FXML private ComboBox<String>   priorityCombo;
    @FXML private Label titleErrorLabel;
    @FXML private Label descErrorLabel;
    @FXML private Label dueDateErrorLabel;
    @FXML private Label statusErrorLabel;
    @FXML private Label userErrorLabel;
    @FXML private Label priorityErrorLabel;

    private int projectId;

    @FXML
    public void initialize() {
        if (statusCombo != null) {
            statusCombo.getItems().addAll("To Do", "In Progress", "Done");
        }
        priorityCombo.getItems().addAll("LOW", "MEDIUM", "HIGH");
        userCombo.setPromptText("Select team member...");

        loadUsers();
        clearErrors();
    }
    public void setProjectId(int projectId) {
        this.projectId = projectId;
    }

    private void loadUsers() {
        sql dao = new sql();
        List<User> users = dao.getAllUsers(); // You'll need this method in sql.java
        userCombo.getItems().addAll(users);

        // This makes the ComboBox show the User's name instead of memory address
        userCombo.setConverter(new StringConverter<User>() {
            @Override public String toString(User user) { return user == null ? "" : user.getFullName(); }
            @Override public User fromString(String s) { return null; }
        });
    }

    @FXML
    private void onSaveTask() {
        String title = titleField.getText() == null ? "" : titleField.getText().trim();
        String desc = descField.getText() == null ? "" : descField.getText().trim();
        User selectedUser = userCombo.getValue();
        if (!validateInputs(title, desc, selectedUser)) {
            return;
        }

        Task task = new Task(
                title,
                desc,
                normalizeStatus(statusCombo.getValue()),
                priorityCombo.getValue(),
                LocalDate.now().toString(),
                dueDatePicker.getValue().toString(),
                this.projectId,
                selectedUser.getId(), // assignee
                1 // creator (replace with logged-in user id when auth/session is added)
        );

        new sql().addTask(task);
        closeWindow();
    }
    private void closeWindow() {
        Stage stage = (Stage) titleField.getScene().getWindow();
        stage.close();
    }

    private String normalizeStatus(String status) {
        if (status == null) return "TODO";
        if ("À FAIRE".equalsIgnoreCase(status)) return "TODO";
        if ("EN COURS".equalsIgnoreCase(status)) return "IN_PROGRESS";
        if ("TERMINE".equalsIgnoreCase(status) || "TERMINÉ".equalsIgnoreCase(status)) return "DONE";
        String normalized = status.trim().toUpperCase().replace(' ', '_').replace('-', '_');
        if ("TO_DO".equals(normalized)) return "TODO";
        return normalized;
    }
    @FXML
    private void onCancel() {
        closeWindow();
    }

    private boolean validateInputs(String title, String desc, User selectedUser) {
        clearErrors();
        boolean valid = true;

        if (title.isBlank()) {
            setError(titleErrorLabel, "Task name is required.");
            valid = false;
        } else if (title.length() < 3) {
            setError(titleErrorLabel, "Minimum 3 characters.");
            valid = false;
        } else if (title.length() > 120) {
            setError(titleErrorLabel, "Maximum 120 characters.");
            valid = false;
        }

        if (desc.length() > 1000) {
            setError(descErrorLabel, "Maximum 1000 characters.");
            valid = false;
        }

        if (dueDatePicker.getValue() == null) {
            setError(dueDateErrorLabel, "Due date is required.");
            valid = false;
        }

        if (statusCombo.getValue() == null) {
            setError(statusErrorLabel, "Status is required.");
            valid = false;
        }

        if (selectedUser == null) {
            setError(userErrorLabel, "Assigned user is required.");
            valid = false;
        }

        if (priorityCombo.getValue() == null) {
            setError(priorityErrorLabel, "Priority is required.");
            valid = false;
        }

        return valid;
    }

    private void clearErrors() {
        hideError(titleErrorLabel);
        hideError(descErrorLabel);
        hideError(dueDateErrorLabel);
        hideError(statusErrorLabel);
        hideError(userErrorLabel);
        hideError(priorityErrorLabel);
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
