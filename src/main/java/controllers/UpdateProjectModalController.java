package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import javafx.collections.ListChangeListener;
import org.controlsfx.control.CheckComboBox;
import entities.Project;
import entities.User;
import services.ProjectService;
import services.UserService;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class UpdateProjectModalController {
    @FXML private TextField nameField;
    @FXML private TextArea descField;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker dueDatePicker;
    @FXML private CheckComboBox<User> assignedUsersCombo;
    @FXML private Label nameErrorLabel;
    @FXML private Label descErrorLabel;
    @FXML private Label startDateErrorLabel;
    @FXML private Label dueDateErrorLabel;
    @FXML private Label assignedUsersErrorLabel;

    private Project originalProject;
    private Project updatedProject;
    private boolean saved;
    private final ProjectService projectService = new ProjectService();
    private final UserService userService = new UserService();

    @FXML
    public void initialize() {
        assignedUsersCombo.getItems().setAll(userService.getAllUsers());
        assignedUsersCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(User user) {
                return user == null ? "" : user.getFullName();
            }

            @Override
            public User fromString(String string) {
                return null;
            }
        });
        assignedUsersCombo.getCheckModel().getCheckedItems().addListener((ListChangeListener<User>) c -> refreshAssignedUsersTitle());
        refreshAssignedUsersTitle();
        clearErrors();
    }

    public void setProject(Project project) {
        this.originalProject = project;
        if (project == null) {
            return;
        }

        nameField.setText(project.getName());
        descField.setText(project.getDescription());

        if (project.getStartDate() != null && !project.getStartDate().isBlank()) {
            startDatePicker.setValue(LocalDate.parse(project.getStartDate()));
        }
        if (project.getEndDate() != null && !project.getEndDate().isBlank()) {
            dueDatePicker.setValue(LocalDate.parse(project.getEndDate()));
        }

        List<Integer> assignedIds = projectService.getProjectAssignmentUserIds(project.getId());
        if (assignedIds.isEmpty() && project.getAssignedTo() > 0) {
            assignedIds = List.of(project.getAssignedTo());
        }
        for (int i = 0; i < assignedUsersCombo.getItems().size(); i++) {
            User user = assignedUsersCombo.getItems().get(i);
            if (assignedIds.contains(user.getId())) {
                assignedUsersCombo.getCheckModel().check(i);
            }
        }
        refreshAssignedUsersTitle();
    }

    public boolean isSaved() {
        return saved;
    }

    public Project getUpdatedProject() {
        return updatedProject;
    }

    @FXML
    private void onSave() {
        String name = nameField.getText() == null ? "" : nameField.getText().trim();
        String desc = descField.getText() == null ? "" : descField.getText().trim();
        LocalDate startDate = startDatePicker.getValue();
        LocalDate dueDate = dueDatePicker.getValue();
        List<User> selectedUsers = assignedUsersCombo.getCheckModel().getCheckedItems();
        if (originalProject == null || !validateInputs(name, desc, startDate, dueDate, selectedUsers)) {
            return;
        }

        Project candidate = new Project(
                originalProject.getId(),
                name,
                desc,
                originalProject.getProgress(),
                originalProject.getBudget(),
                startDate.toString(),
                dueDate.toString(),
                selectedUsers.get(0).getId(),
                originalProject.getCreatedby()
        );

        boolean ok = projectService.updateProject(candidate);
        if (ok) {
            projectService.replaceProjectAssignments(candidate.getId(), selectedUsers.stream().map(User::getId).collect(Collectors.toList()));
            saved = true;
            updatedProject = candidate;
            close();
        }
    }

    @FXML
    private void onCancel() {
        close();
    }

    private void close() {
        Stage stage = (Stage) nameField.getScene().getWindow();
        stage.close();
    }

    private void refreshAssignedUsersTitle() {
        List<User> checked = assignedUsersCombo.getCheckModel().getCheckedItems();
        if (checked == null || checked.isEmpty()) {
            assignedUsersCombo.setTitle("Select team members");
            return;
        }
        String title = checked.stream().map(User::getFullName).collect(Collectors.joining(", "));
        assignedUsersCombo.setTitle(title);
    }

    private boolean validateInputs(String name, String desc, LocalDate startDate, LocalDate dueDate, List<User> selectedUsers) {
        clearErrors();
        boolean valid = true;

        if (name.isBlank()) {
            setError(nameErrorLabel, "Project name is required.");
            valid = false;
        } else if (!InputValidationUtils.hasMeaningfulText(name)) {
            setError(nameErrorLabel, "Project name must include letters or numbers.");
            valid = false;
        } else if (name.length() < 3) {
            setError(nameErrorLabel, "Minimum 3 characters.");
            valid = false;
        } else if (name.length() > 100) {
            setError(nameErrorLabel, "Maximum 100 characters.");
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

        if (startDate == null) {
            setError(startDateErrorLabel, "Start date is required.");
            valid = false;
        }

        if (dueDate == null) {
            setError(dueDateErrorLabel, "Due date is required.");
            valid = false;
        }

        if (startDate != null && dueDate != null && dueDate.isBefore(startDate)) {
            setError(dueDateErrorLabel, "Due date must be on or after start date.");
            valid = false;
        }

        if (selectedUsers == null || selectedUsers.isEmpty()) {
            setError(assignedUsersErrorLabel, "Select at least one team member.");
            valid = false;
        }

        return valid;
    }

    private void clearErrors() {
        hideError(nameErrorLabel);
        hideError(descErrorLabel);
        hideError(startDateErrorLabel);
        hideError(dueDateErrorLabel);
        hideError(assignedUsersErrorLabel);
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
