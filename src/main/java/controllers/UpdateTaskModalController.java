package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import entities.Task;
import entities.User;
import services.TaskService;
import services.UserService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final UserService userService = new UserService();
    private final TaskService taskService = new TaskService();
    private final services.ProjectService projectService = new services.ProjectService();
    private final List<User> allUsers = new ArrayList<>();

    @FXML
    public void initialize() {
        statusCombo.getItems().setAll(
                TaskValueMapper.toStatusLabel(TaskValueMapper.STATUS_TODO),
                TaskValueMapper.toStatusLabel(TaskValueMapper.STATUS_IN_PROGRESS),
                TaskValueMapper.toStatusLabel(TaskValueMapper.STATUS_DONE)
        );
        priorityCombo.getItems().setAll(
                TaskValueMapper.PRIORITY_LOW,
                TaskValueMapper.PRIORITY_MEDIUM,
                TaskValueMapper.PRIORITY_HIGH
        );

        allUsers.clear();
        allUsers.addAll(userService.getAllUsers());
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
        statusCombo.setValue(TaskValueMapper.toStatusLabel(task.getStatus()));
        priorityCombo.setValue(TaskValueMapper.normalizePriority(task.getPriority()));

        loadAssignableUsersForProject(task.getProjectId());
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
                TaskValueMapper.normalizeStatus(statusCombo.getValue()),
                TaskValueMapper.normalizePriority(priorityCombo.getValue()),
                currentTask.getStartDate(),
                dueDatePicker.getValue().toString(),
                currentTask.getProjectId(),
                userCombo.getValue().getId(),
                currentTask.getCreatedby()
        );
        updated.setId(currentTask.getId());

        if (taskService.updateTask(updated)) {
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

    private boolean validateInputs(String title, String desc, User selectedUser) {
        clearErrors();
        boolean valid = true;

        if (title.isBlank()) {
            setError(titleErrorLabel, "Task name is required.");
            valid = false;
        } else if (!InputValidationUtils.hasMeaningfulText(title)) {
            setError(titleErrorLabel, "Task name must include letters or numbers.");
            valid = false;
        } else if (title.length() < 3) {
            setError(titleErrorLabel, "Minimum 3 characters.");
            valid = false;
        } else if (title.length() > 120) {
            setError(titleErrorLabel, "Maximum 120 characters.");
            valid = false;
        }

        if (!desc.isBlank() && !InputValidationUtils.hasMeaningfulText(desc)) {
            setError(descErrorLabel, "Description must include letters or numbers.");
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

    private void loadAssignableUsersForProject(int projectId) {
        Set<Integer> assignedUserIds = projectService.getProjectAssignmentUserIds(projectId)
                .stream()
                .collect(Collectors.toSet());

        List<User> allowedUsers = allUsers.stream()
                .filter(user -> assignedUserIds.contains(user.getId()))
                .toList();
        userCombo.getItems().setAll(allowedUsers);
        userCombo.setPromptText(allowedUsers.isEmpty() ? "No members assigned to this project" : "Select team member...");
    }
}
