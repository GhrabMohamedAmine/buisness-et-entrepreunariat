package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import entities.Project;
import entities.Task;
import entities.User;
import services.ProjectService;
import services.TaskService;
import services.UserService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class AddTaskController {
    @FXML private TextField titleField;
    @FXML private TextArea descField; // If you don't have a desc field in FXML, use "" in the constructor
    @FXML private DatePicker dueDatePicker;

    @FXML private ComboBox<User> userCombo; // Use a User object, not just a String
    @FXML private ComboBox<String> statusCombo;
    @FXML private ComboBox<String>   priorityCombo;
    @FXML private ComboBox<Project> projectCombo;
    @FXML private Label subtitleLabel;
    @FXML private Label titleErrorLabel;
    @FXML private Label descErrorLabel;
    @FXML private Label dueDateErrorLabel;
    @FXML private Label statusErrorLabel;
    @FXML private Label userErrorLabel;
    @FXML private Label priorityErrorLabel;
    @FXML private Label projectErrorLabel;

    private int projectId;
    private int fixedAssignedUserId = -1;
    private final ProjectService projectService = new ProjectService();
    private final UserService userService = new UserService();
    private final TaskService taskService = new TaskService();
    private final List<User> allUsers = new ArrayList<>();

    @FXML
    public void initialize() {
        if (statusCombo != null) {
            statusCombo.getItems().addAll(
                    TaskValueMapper.toStatusLabel(TaskValueMapper.STATUS_TODO),
                    TaskValueMapper.toStatusLabel(TaskValueMapper.STATUS_IN_PROGRESS),
                    TaskValueMapper.toStatusLabel(TaskValueMapper.STATUS_DONE)
            );
        }
        priorityCombo.getItems().addAll(
                TaskValueMapper.PRIORITY_LOW,
                TaskValueMapper.PRIORITY_MEDIUM,
                TaskValueMapper.PRIORITY_HIGH
        );
        userCombo.setPromptText("Select team member...");

        loadProjects();
        loadUsers();
        if (projectCombo != null) {
            projectCombo.valueProperty().addListener((obs, oldValue, newValue) -> refreshAssignableUsers());
        }
        clearErrors();
    }

    public void setProjectId(int projectId) {
        this.projectId = projectId;
        if (projectCombo == null) return;

        for (Project project : projectCombo.getItems()) {
            if (project.getId() == projectId) {
                projectCombo.setValue(project);
                projectCombo.setDisable(true);
                refreshAssignableUsers();
                return;
            }
        }
    }

    private void loadProjects() {
        List<Project> projects = projectService.getAllProjects();
        projectCombo.getItems().setAll(projects);
        projectCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(Project project) {
                return project == null ? "" : project.getName();
            }

            @Override
            public Project fromString(String string) {
                return null;
            }
        });
    }

    private void loadUsers() {
        allUsers.clear();
        allUsers.addAll(userService.getAllUsers());

        userCombo.setConverter(new StringConverter<User>() {
            @Override public String toString(User user) { return user == null ? "" : user.getFullName(); }
            @Override public User fromString(String s) { return null; }
        });

        refreshAssignableUsers();

        if (fixedAssignedUserId > 0) {
            applyFixedAssignedUser();
        }
    }

    public void setAssignedUserId(int userId) {
        this.fixedAssignedUserId = userId;
        if (userCombo != null) {
            applyFixedAssignedUser();
        }
    }

    public void setSubtitleText(String subtitleText) {
        if (subtitleLabel == null) {
            return;
        }
        if (subtitleText == null || subtitleText.isBlank()) {
            return;
        }
        subtitleLabel.setText(subtitleText);
    }

    @FXML
    private void onSaveTask() {
        String title = titleField.getText() == null ? "" : titleField.getText().trim();
        String desc = descField.getText() == null ? "" : descField.getText().trim();
        User selectedUser = userCombo.getValue();
        Project selectedProject = projectCombo.getValue();
        if (!validateInputs(title, desc, selectedUser, selectedProject)) {
            return;
        }

        int resolvedProjectId = projectId > 0 ? projectId : selectedProject.getId();
        Task task = new Task(
                title,
                desc,
                TaskValueMapper.normalizeStatus(statusCombo.getValue()),
                TaskValueMapper.normalizePriority(priorityCombo.getValue()),
                LocalDate.now().toString(),
                dueDatePicker.getValue().toString(),
                resolvedProjectId,
                selectedUser.getId(), // assignee
                1 // creator (replace with logged-in user id when auth/session is added)
        );

        taskService.addTask(task);
        closeWindow();
    }
    private void closeWindow() {
        Stage stage = (Stage) titleField.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void onCancel() {
        closeWindow();
    }

    private boolean validateInputs(String title, String desc, User selectedUser, Project selectedProject) {
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
        }

        if (!desc.isBlank() && !InputValidationUtils.hasMeaningfulText(desc)) {
            setError(descErrorLabel, "Description must include letters or numbers.");
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

        if (projectId <= 0 && selectedProject == null) {
            setError(projectErrorLabel, "Project is required.");
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
        hideError(projectErrorLabel);
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

    private void applyFixedAssignedUser() {
        for (User user : userCombo.getItems()) {
            if (user.getId() == fixedAssignedUserId) {
                userCombo.setValue(user);
                userCombo.setDisable(true);
                return;
            }
        }
        userCombo.setValue(null);
        userCombo.setDisable(false);
    }

    private void refreshAssignableUsers() {
        int effectiveProjectId = resolveEffectiveProjectId();
        if (effectiveProjectId <= 0) {
            userCombo.getItems().clear();
            userCombo.setValue(null);
            userCombo.setPromptText("Select a project first...");
            return;
        }

        Set<Integer> assignedUserIds = projectService.getProjectAssignmentUserIds(effectiveProjectId)
                .stream()
                .collect(Collectors.toSet());

        List<User> allowedUsers = allUsers.stream()
                .filter(user -> assignedUserIds.contains(user.getId()))
                .toList();

        User currentSelection = userCombo.getValue();
        userCombo.getItems().setAll(allowedUsers);
        if (currentSelection != null && allowedUsers.stream().anyMatch(user -> user.getId() == currentSelection.getId())) {
            userCombo.setValue(currentSelection);
        } else {
            userCombo.setValue(null);
        }
        userCombo.setPromptText(allowedUsers.isEmpty() ? "No members assigned to this project" : "Select team member...");

        if (fixedAssignedUserId > 0) {
            applyFixedAssignedUser();
        }
    }

    private int resolveEffectiveProjectId() {
        if (projectId > 0) {
            return projectId;
        }
        Project selectedProject = projectCombo == null ? null : projectCombo.getValue();
        return selectedProject == null ? -1 : selectedProject.getId();
    }

}
