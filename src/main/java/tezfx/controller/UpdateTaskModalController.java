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

public class UpdateTaskModalController {
    @FXML private TextField titleField;
    @FXML private TextArea descField;
    @FXML private DatePicker dueDatePicker;
    @FXML private ComboBox<User> userCombo;
    @FXML private ComboBox<String> statusCombo;
    @FXML private ComboBox<String> priorityCombo;
    @FXML private Label titleErrorLabel;
    @FXML private Label descErrorLabel;
    @FXML private Label dueDateErrorLabel;
    @FXML private Label statusErrorLabel;
    @FXML private Label userErrorLabel;
    @FXML private Label priorityErrorLabel;

    private Task currentTask;
    private boolean saved;

    @FXML
    public void initialize() {
        statusCombo.getItems().setAll("To Do", "In Progress", "Done");
        priorityCombo.getItems().setAll("LOW", "MEDIUM", "HIGH");

        sql dao = new sql();
        List<User> users = dao.getAllUsers();
        userCombo.getItems().setAll(users);
        userCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(User user) {
                return user == null ? "" : user.getFullName();
            }

            @Override
            public User fromString(String string) {
                return null;
            }
        });
        clearErrors();
    }

    public void setTask(Task task) {
        this.currentTask = task;
        if (task == null) return;

        titleField.setText(task.getTitle());
        descField.setText(task.getDescription());
        if (task.getDueDate() != null && !task.getDueDate().isBlank()) {
            dueDatePicker.setValue(LocalDate.parse(task.getDueDate()));
        }
        statusCombo.setValue(formatStatusLabelEn(normalizeStatus(task.getStatus())));
        priorityCombo.setValue(task.getPriority() == null ? "LOW" : task.getPriority().trim().toUpperCase());

        for (User user : userCombo.getItems()) {
            if (user.getId() == task.getAssignedTo()) {
                userCombo.setValue(user);
                break;
            }
        }
    }

    public boolean isSaved() {
        return saved;
    }

    @FXML
    private void onSaveTask() {
        String title = titleField.getText() == null ? "" : titleField.getText().trim();
        String desc = descField.getText() == null ? "" : descField.getText().trim();
        if (currentTask == null || !validateInputs(title, desc, userCombo.getValue())) {
            return;
        }

        Task updated = new Task(
                title,
                desc,
                normalizeStatus(statusCombo.getValue()),
                priorityCombo.getValue().trim().toUpperCase(),
                currentTask.getStartDate(),
                dueDatePicker.getValue().toString(),
                currentTask.getProjectId(),
                userCombo.getValue().getId(),
                currentTask.getCreatedby()
        );
        updated.setId(currentTask.getId());

        if (new sql().updateTask(updated)) {
            saved = true;
            closeWindow();
        }
    }

    @FXML
    private void onCancel() {
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

    private String formatStatusLabelEn(String normalizedStatus) {
        if ("DONE".equals(normalizedStatus)) return "Done";
        if ("IN_PROGRESS".equals(normalizedStatus)) return "In Progress";
        return "To Do";
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
