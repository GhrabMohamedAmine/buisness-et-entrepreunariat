package controllers;

import javafx.fxml.FXML;
import javafx.application.Platform;
import javafx.animation.PauseTransition;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.controlsfx.control.CheckComboBox;
import entities.Project;
import entities.User;
import services.ProjectService;
import services.UserService;
import javafx.util.StringConverter;
import javafx.collections.ListChangeListener;
import entities.Task;
import services.AiTaskGeneratorService;
import javafx.util.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import services.TaskService;

public class AddProjectController {
    @FXML private TextField nameField;
    @FXML private TextArea descField;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker dueDatePicker;
    @FXML private CheckComboBox<User> assignedUsersCombo;
    @FXML private TextField budgetField;
    @FXML private Label nameErrorLabel;
    @FXML private Label descErrorLabel;
    @FXML private Label startDateErrorLabel;
    @FXML private Label dueDateErrorLabel;
    @FXML private Label assignedUsersErrorLabel;

    private Runnable onProjectCreated;

    private final ProjectService projectService = new ProjectService();
    private final UserService userService = new UserService();
    private final TaskService taskService = new TaskService();
    private final AiTaskGeneratorService aiTaskGeneratorService = new AiTaskGeneratorService();

    @FXML
    public void initialize() {
        assignedUsersCombo.getItems().setAll(userService.getAllUsers());
        assignedUsersCombo.setConverter(new StringConverter<User>() {
            @Override
            public String toString(User user) {
                return user == null ? "" : user.getFullName();
            }

            @Override
            public User fromString(String s) {
                return null;
            }
        });
        assignedUsersCombo.getCheckModel().getCheckedItems().addListener((ListChangeListener<User>) c -> refreshAssignedUsersTitle());
        refreshAssignedUsersTitle();
    }

    @FXML
    private void handleCreate() {
        String name = nameField.getText() == null ? "" : nameField.getText().trim();
        String desc = descField.getText() == null ? "" : descField.getText().trim();
        LocalDate startDate = startDatePicker.getValue();
        LocalDate dueDate = dueDatePicker.getValue();
        List<User> selectedUsers = assignedUsersCombo.getCheckModel().getCheckedItems();
        if (!validateInputs(name, desc, startDate, dueDate, selectedUsers)) {
            return;
        }

        try {
            int progress = 0;

            String startStr = startDate.toString();
            String dueStr = dueDate.toString();

            Project newProject = new Project(name, desc, progress, 0.0, startStr, dueStr, selectedUsers.get(0).getId(), 1);

            // 3. Save and Refresh
            int projectId = projectService.ReturnPrID(newProject);
            if (projectId > 0) {
                List<Integer> userIds = selectedUsers.stream().map(User::getId).collect(Collectors.toList());
                projectService.replaceProjectAssignments(projectId, userIds);
                generateTasksInBackground(projectId, newProject);
            }
            if (onProjectCreated != null) {
                onProjectCreated.run();
            }

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
        this.onProjectCreated = parentController::loadData;
    }

    public void setOnProjectCreated(Runnable onProjectCreated) {
        this.onProjectCreated = onProjectCreated;
    }

    private void closeoverlay() {
        if (nameField != null && nameField.getScene() != null && nameField.getScene().getWindow() instanceof Stage stage) {
            stage.close();
            return;
        }

        StackPane contentArea = MainController.getStaticContentArea();
        if (contentArea == null) {
            return;
        }
        int childCount = contentArea.getChildren().size();
        if (childCount > 0) {
            contentArea.getChildren().remove(childCount - 1);
        }
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

    private void generateTasksInBackground(int projectId, Project project) {
        String projectDescription = project.getDescription();
        if (projectDescription == null || projectDescription.isBlank()) {
            return;
        }

        LocalDate startDate = parseDate(project.getStartDate());
        LocalDate dueDate = parseDate(project.getEndDate());


        Thread taskGeneratorThread = new Thread(() -> {
            try {
                List<AiTaskGeneratorService.GeneratedTask> generatedTasks =
                        aiTaskGeneratorService.generateTasks(project.getName(), projectDescription, startDate, dueDate, 5);

                int createdCount = 0;
                for (AiTaskGeneratorService.GeneratedTask generatedTask : generatedTasks) {
                    Task task = new Task(
                            generatedTask.title(),
                            generatedTask.description(),
                            "TODO",
                            generatedTask.priority(),
                            project.getStartDate(),
                            project.getEndDate(),
                            projectId,
                            0,
                            1
                    );
                    taskService.addTask(task);
                    createdCount++;
                }

                if (createdCount > 0) {
                    final int finalCreatedCount = createdCount;
                    Platform.runLater(() -> {
                        if (onProjectCreated != null) {
                            onProjectCreated.run();
                        }
                        showAiGeneratedToast(project.getName(), finalCreatedCount);
                    });
                }
            } catch (Exception e) {
                System.err.println("AI task generation skipped: " + e.getMessage());
                e.printStackTrace();
            }
        }, "ai-task-generator-" + projectId);
        taskGeneratorThread.setDaemon(true);
        taskGeneratorThread.start();
    }

    private LocalDate parseDate(String dateText) {
        try {
            return dateText == null || dateText.isBlank() ? null : LocalDate.parse(dateText);
        } catch (Exception ex) {
            return null;
        }
    }

    private void showAiGeneratedToast(String projectName, int createdCount) {
        StackPane contentArea = MainController.getStaticContentArea();
        if (contentArea == null || contentArea.getScene() == null) {
            return;
        }

        Window owner = contentArea.getScene().getWindow();
        if (owner == null) {
            return;
        }

        Label messageLabel = new Label("Created " + createdCount + " tasks for \"" + projectName + "\".");
        messageLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12;");

        HBox toastRoot = new HBox(messageLabel);
        toastRoot.setStyle("-fx-background-color: rgba(17,24,39,0.95); -fx-background-radius: 8; -fx-padding: 10 14;");

        Popup popup = new Popup();
        popup.getContent().add(toastRoot);
        popup.setAutoHide(true);
        popup.show(owner);

        double x = owner.getX() + owner.getWidth() - toastRoot.prefWidth(-1) - 24;
        double y = owner.getY() + owner.getHeight() - toastRoot.prefHeight(-1) - 24;
        popup.setX(x);
        popup.setY(y);

        PauseTransition delay = new PauseTransition(Duration.seconds(3));
        delay.setOnFinished(event -> popup.hide());
        delay.play();
    }
}
