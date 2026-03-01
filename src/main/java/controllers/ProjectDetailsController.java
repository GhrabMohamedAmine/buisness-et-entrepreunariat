package controllers;

import javafx.animation.PauseTransition;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.control.Alert;
import javafx.scene.effect.BoxBlur;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.geometry.Pos;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.util.Duration;
import entities.Project;
import entities.Task;
import entities.User;
import org.kordamp.ikonli.javafx.FontIcon;
import services.ActivityService;
import services.ProjectHealthService;
import services.ProjectService;
import services.ProjectReportService;
import services.TaskService;
import services.UserService;
import javafx.scene.paint.Color;

import javafx.scene.control.Button;
import java.io.IOException;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class ProjectDetailsController {
    private static final String[] AVATAR_COLOR_CLASSES = {"purple", "blue", "green", "orange"};
    private static final DateTimeFormatter ACTIVITY_DATE_INPUT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter ACTIVITY_DATE_OUTPUT = DateTimeFormatter.ofPattern("dd MMM", Locale.ENGLISH);
    private ProjectsController projectC = new ProjectsController();


    @FXML
    private Label detailName, detailDesc, detailDate, detailPercent;
    @FXML private ProgressBar detailProgress;
    @FXML private Label totalTasksLabel, completedTasksLabel;
    @FXML private Label overdueTasksLabel;
    @FXML private Label healthScoreLabel;
    @FXML private Label healthStatusLabel;
    @FXML private VBox tasksContainer;
    @FXML private ScrollPane tasksScrollPane;
    @FXML private HBox overviewContainer;
    @FXML private VBox kanbanContainer;
    @FXML private VBox todoColumn;
    @FXML private VBox inProgressColumn;
    @FXML private VBox doneColumn;
    @FXML private ScrollPane todoScrollPane;
    @FXML private ScrollPane inProgressScrollPane;
    @FXML private ScrollPane doneScrollPane;
    @FXML private Label todoCountLabel;
    @FXML private Label inProgressCountLabel;
    @FXML private Label doneCountLabel;
    @FXML private VBox teamContainer;
    @FXML private VBox recentActivityContainer;
    @FXML private Project currentProject;
    @FXML private Button tasksBtn, overviewTab, kanbanTab;
    @FXML
    FontIcon updateButton,deleteButton;

    private final ProjectService projectService = new ProjectService();
    private final TaskService taskService = new TaskService();
    private final UserService userService = new UserService();
    private final ActivityService activityService = new ActivityService();
    private final ProjectHealthService projectHealthService = new ProjectHealthService();
    private final ProjectReportService projectReportService = new ProjectReportService();
    private final Map<Integer, Task> kanbanTaskById = new java.util.HashMap<>();

    private static class ActivityItem {
        private final long sortKey;
        private final Node node;

        private ActivityItem(long sortKey, Node node) {
            this.sortKey = sortKey;
            this.node = node;
        }
    }

    @FXML
    private void initialize() {
        setupDropTarget(todoColumn, TaskValueMapper.STATUS_TODO);
        setupDropTarget(inProgressColumn, TaskValueMapper.STATUS_IN_PROGRESS);
        setupDropTarget(doneColumn, TaskValueMapper.STATUS_DONE);
        setupDropTarget(todoScrollPane, TaskValueMapper.STATUS_TODO);
        setupDropTarget(inProgressScrollPane, TaskValueMapper.STATUS_IN_PROGRESS);
        setupDropTarget(doneScrollPane, TaskValueMapper.STATUS_DONE);

        updateButton.setVisible(projectC.isUserManager());
        deleteButton.setVisible(projectC.isUserManager());
    }

    public void ProjectDataLoad(Project project) {
        if (project == null) {
            return;
        }
        this.currentProject = project;
        if (detailName != null) {
            detailName.setText(project.getName());
        }
        if (detailDesc != null) {
            detailDesc.setText(project.getDescription());
        }
        if (detailDate != null) {
            detailDate.setText("Due " + project.getEndDate());
        }


        double progressValue = project.getProgress() / 100.0;

        if (detailProgress != null) {
            detailProgress.setProgress(progressValue);
        }



        loadProjectStats(project.getId());
        loadTeamMembers(project.getId());
        loadRecentActivities(project.getId());
    }

    private void loadProjectStats(int projectId) {
        List<Task> tasks = taskService.getTasksByProject(projectId);
        int total = tasks.size();
        int completed = countDoneTasks(tasks);
        int overdue = taskService.getOverdueTaskCount(projectId);

        totalTasksLabel.setText(String.valueOf(total));
        completedTasksLabel.setText(String.valueOf(completed));

        double progress = total == 0 ? 0.0 : (completed * 100.0) / total;
        detailProgress.setProgress(progress / 100.0);
        if (detailPercent != null) {
            detailPercent.setText((int) Math.round(progress) + "%");
        }

        overdueTasksLabel.setText(String.valueOf(overdue));

        ProjectHealthService.ProjectHealthResult health = projectHealthService.calculateForProject(tasks);
        if (healthScoreLabel != null) {
            healthScoreLabel.setText(health.getScore() + "/100");
        }
        if (healthStatusLabel != null) {
            healthStatusLabel.setText(health.getLevel().getLabel());
            healthStatusLabel.setStyle(switch (health.getLevel()) {
                case HEALTHY -> "-fx-font-weight: bold; -fx-font-size: 16; -fx-text-fill: #166534;";
                case AT_RISK -> "-fx-font-weight: bold; -fx-font-size: 16; -fx-text-fill: #92400e;";
                case CRITICAL -> "-fx-font-weight: bold; -fx-font-size: 16; -fx-text-fill: #991b1b;";
            });
        }
    }

    private int countDoneTasks(List<Task> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return 0;
        }
        int done = 0;
        for (Task task : tasks) {
            if (task == null || task.getStatus() == null) {
                continue;
            }
            String normalized = task.getStatus().trim().toUpperCase(Locale.ROOT);
            if ("DONE".equals(normalized)) {
                done++;
            }
        }
        return done;
    }

    private void loadTeamMembers(int projectId) {


        teamContainer.getChildren().clear();

        Map<Integer, List<User>> assigneesByProject = projectService.getProjectAssigneesMap();
        List<User> users = assigneesByProject.getOrDefault(projectId, List.of());
        if (users.isEmpty()) {
            HBox row = new HBox(8, buildAvatar("U", "gray"), new Label("Unassigned"));
            row.setAlignment(Pos.CENTER_LEFT);
            teamContainer.getChildren().add(row);
            return;
        }

        for (User user : users) {
            String fullName = user.getFullName();
            HBox row = new HBox(8, buildAvatarInitial(fullName), new Label(fullName));
            row.setAlignment(Pos.CENTER_LEFT);
            Tooltip.install(row, new Tooltip(fullName));
            teamContainer.getChildren().add(row);
        }
    }

    private void loadRecentActivities(int projectId) {
        recentActivityContainer.getChildren().clear();

        Map<Integer, String> userNamesById = new java.util.HashMap<>();
        for (User user : userService.getAllUsers()) {
            userNamesById.put(user.getId(), user.getFullName());
        }

        List<ActivityItem> items = new ArrayList<>();
        List<ActivityService.TaskActivity> activities = activityService.getTaskActivitiesByProject(projectId, 20);
        for (ActivityService.TaskActivity activity : activities) {
            String type = normalizeActivityType(activity.getType());
            String title = switch (type) {
                case "CREATED" -> "Task Created";
                case "COMPLETED" -> "Task Completed";
                case "DELETED" -> "Task Deleted";
                default -> "Task Updated";
            };
            String actor = userNamesById.getOrDefault(activity.getActorUserId(), "A user");
            String taskTitle = activity.getTaskTitle() == null || activity.getTaskTitle().isBlank()
                    ? "Untitled task"
                    : activity.getTaskTitle();
            String message = switch (type) {
                case "CREATED" -> actor + " created \"" + taskTitle + "\"";
                case "COMPLETED" -> actor + " completed \"" + taskTitle + "\"";
                case "DELETED" -> actor + " deleted \"" + taskTitle + "\"";
                default -> actor + " updated \"" + taskTitle + "\"";
            };
            String iconStyle = switch (type) {
                case "CREATED" -> "activity-icon-created";
                case "COMPLETED" -> "activity-icon-completed";
                case "DELETED" -> "activity-icon-deleted";
                default -> "activity-icon-updated";
            };

            items.add(new ActivityItem(
                    activity.getTimestamp(),
                    createActivityCard(title, message, activity.getActivityDate(), iconStyle)
            ));
        }

        if (items.isEmpty()) {
            List<Task> tasks = taskService.getTasksByProject(projectId);
            for (Task task : tasks) {
                String createdBy = userNamesById.getOrDefault(task.getCreatedby(), "A user");
                items.add(new ActivityItem(
                        (long) task.getId() * 10 + 1,
                        createActivityCard(
                                "Task Created",
                                createdBy + " created \"" + task.getTitle() + "\"",
                                task.getStartDate(),
                                "activity-icon-created"
                        )
                ));
            }
        }

        if (items.isEmpty()) {
            Label empty = new Label("No activity yet.");
            empty.getStyleClass().add("text-muted");
            recentActivityContainer.getChildren().add(empty);
            return;
        }

        items.sort(Comparator.comparingLong((ActivityItem i) -> i.sortKey).reversed());
        int maxItems = Math.min(items.size(), 6);
        for (int i = 0; i < maxItems; i++) {
            recentActivityContainer.getChildren().add(items.get(i).node);
        }
    }

    private Node createActivityCard(String title, String message, String date, String iconStyleClass) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/tezfx/view/components/ActivityCard.fxml"));
            Node card = loader.load();
            ActivityCardController controller = loader.getController();
            controller.setData(title, message, formatActivityDate(date), iconStyleClass);
            return card;
        } catch (IOException e) {
            Label fallback = new Label(message);
            fallback.getStyleClass().add("text-muted");
            return fallback;
        }
    }

    private String formatActivityDate(String date) {
        if (date == null || date.isBlank()) {
            return "Date unavailable";
        }
        try {
            return LocalDate.parse(date, ACTIVITY_DATE_INPUT).format(ACTIVITY_DATE_OUTPUT).toUpperCase(Locale.ENGLISH);
        } catch (Exception e) {
            return date;
        }
    }



    private String normalizeStatus(String status) {
        if (status == null) return "TODO";
        return status.trim().toUpperCase();
    }

    private String normalizeActivityType(String type) {
        if (type == null || type.isBlank()) return "UPDATED";
        return type ;
    }

    private StackPane buildAvatarInitial(String fullName) {
        String safeName = (fullName == null || fullName.isBlank()) ? "U" : fullName.trim();
        String initial = String.valueOf(Character.toUpperCase(safeName.charAt(0)));
        return buildAvatar(initial, pickAvatarColorClass(safeName));
    }

    private StackPane buildAvatar(String text, String colorClass) {
        Label initialLabel = new Label(text);
        initialLabel.getStyleClass().add("avatar-initial");

        StackPane circle = new StackPane(initialLabel);
        circle.setPrefSize(22, 22);
        circle.setMinSize(22, 22);
        circle.setMaxSize(22, 22);
        circle.getStyleClass().addAll("avatar", colorClass);
        return circle;
    }

    private String pickAvatarColorClass(String key) {
        int index = Math.abs(key.hashCode()) % AVATAR_COLOR_CLASSES.length;
        return AVATAR_COLOR_CLASSES[index];
    }


    private void loadTasks(int projectId) {
        tasksContainer.getChildren().clear();
        List<Task> tasks = taskService.getTasksByProject(projectId);

        for (Task t : tasks) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/tezfx/view/components/TaskRow.fxml"));
                Parent row = loader.load();

                // Get TaskRowController and set data
                TaskRowController controller = loader.getController();
                controller.setTaskData(t);

                tasksContainer.getChildren().add(row);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    // Inside ProjectDetailsController
    @FXML private VBox taskListContainer; // The VBox inside your "Tasks" Tab

    public void showTasksTab() {
        if (currentProject == null) {
            return;
        }

        // 1. Hide the "Overview" and "Kanban" content, show the "Tasks" content
        overviewContainer.setVisible(false);
        overviewContainer.setManaged(false);
        kanbanContainer.setVisible(false);
        kanbanContainer.setManaged(false);
        tasksScrollPane.setVisible(true);
        tasksScrollPane.setManaged(true);

        // 2. Load the rows
        taskListContainer.getChildren().clear();
        List<Task> tasks = taskService.getTasksByProject(currentProject.getId());

        for (Task t : tasks) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/tezfx/view/components/TaskRow.fxml"));
                Parent row = loader.load(); // Load the HBox we designed

                TaskRowController controller = loader.getController();
                controller.setTaskData(t);
                boolean canModifyTask = canCurrentUserModifyTask(t);
                controller.setStatusEditingAllowed(canModifyTask);
                controller.setTaskActionsAllowed(canModifyTask);
                controller.setOnStatusToggle(selected -> {
                    if (!canModifyTask) {
                        return false;
                    }
                    String newStatus = selected ? "DONE" : "TODO";
                    boolean updated = taskService.updateTaskStatus(t.getId(), newStatus);
                    if (updated && currentProject != null) {
                        loadProjectStats(currentProject.getId());
                        showTasksTab();
                    }
                    return updated;
                });
                controller.setOnStatusChange(newStatus -> {
                    if (!canModifyTask) {
                        return false;
                    }
                    boolean updated = taskService.updateTaskStatus(t.getId(), newStatus);
                    if (updated && currentProject != null) {
                        loadProjectStats(currentProject.getId());
                        showTasksTab();
                    }
                    return updated;
                });
                controller.setOnEdit(() -> openUpdateTaskModal(t));
                controller.setOnDelete(() -> deleteTask(t));

                if (row instanceof Region region) {
                    region.setMaxWidth(Double.MAX_VALUE);
                    VBox.setVgrow(region, Priority.NEVER);
                }
                HBox.setHgrow(row, Priority.ALWAYS);
                taskListContainer.getChildren().add(row); // Add it to the list!
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void onTasksTabClicked() {
        // 1. Update Tab Styles
        overviewTab.getStyleClass().setAll("tab-inactive");
        tasksBtn.getStyleClass().setAll("tab-active");
        kanbanTab.getStyleClass().setAll("tab-inactive");

        // 2. Hide Overview
        overviewContainer.setVisible(false);
        overviewContainer.setManaged(false);

        // 3. Show Tasks
        tasksScrollPane.setVisible(true);
        tasksScrollPane.setManaged(true);
        kanbanContainer.setVisible(false);
        kanbanContainer.setManaged(false);

        // Refresh your task list logic here
        showTasksTab();
    }
    @FXML
    private void onOverviewTabClicked() {
        // 1. Update Tab Styles
        overviewTab.getStyleClass().setAll("tab-active");
        tasksBtn.getStyleClass().setAll("tab-inactive");
        kanbanTab.getStyleClass().setAll("tab-inactive");

        // 2. Show Overview
        overviewContainer.setVisible(true);
        overviewContainer.setManaged(true);

        // 3. Hide Tasks (The fix for your issue)
        tasksScrollPane.setVisible(false);
        tasksScrollPane.setManaged(false);
        kanbanContainer.setVisible(false);
        kanbanContainer.setManaged(false);
    }

    @FXML
    private void onKanbanTabClicked() {
        if (currentProject == null) {
            return;
        }

        overviewTab.getStyleClass().setAll("tab-inactive");
        tasksBtn.getStyleClass().setAll("tab-inactive");
        kanbanTab.getStyleClass().setAll("tab-active");

        overviewContainer.setVisible(false);
        overviewContainer.setManaged(false);
        tasksScrollPane.setVisible(false);
        tasksScrollPane.setManaged(false);
        kanbanContainer.setVisible(true);
        kanbanContainer.setManaged(true);

        loadKanbanColumns();
        animateKanbanContainerReveal();
    }

    private void loadKanbanColumns() {
        if (currentProject == null) {
            return;
        }
        todoColumn.getChildren().clear();
        inProgressColumn.getChildren().clear();
        doneColumn.getChildren().clear();
        kanbanTaskById.clear();

        List<Task> tasks = taskService.getTasksByProject(currentProject.getId());
        for (Task task : tasks) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/tezfx/view/components/TaskRow.fxml"));
                Parent row = loader.load();
                TaskRowController controller = loader.getController();
                row.getProperties().put("taskRowController", controller);
                kanbanTaskById.put(task.getId(), task);

                controller.setTaskData(task);
                controller.setKanbanMode(true);
                boolean canModifyTask = canCurrentUserModifyTask(task);
                controller.setStatusEditingAllowed(canModifyTask);
                controller.setTaskActionsAllowed(canModifyTask);
                controller.setOnStatusToggle(selected -> {
                    if (!canModifyTask) {
                        return false;
                    }
                    String newStatus = selected ? TaskValueMapper.STATUS_DONE : TaskValueMapper.STATUS_TODO;
                    boolean updated = taskService.updateTaskStatus(task.getId(), newStatus);
                    if (updated) {
                        loadProjectStats(currentProject.getId());
                        loadRecentActivities(currentProject.getId());
                        loadKanbanColumns();
                    }
                    return updated;
                });
                controller.setOnStatusChange(newStatus -> {
                    if (!canModifyTask) {
                        return false;
                    }
                    boolean updated = taskService.updateTaskStatus(task.getId(), newStatus);
                    if (updated) {
                        loadProjectStats(currentProject.getId());
                        loadRecentActivities(currentProject.getId());
                        loadKanbanColumns();
                    }
                    return updated;
                });
                controller.setOnEdit(() -> openUpdateTaskModal(task));
                controller.setOnDelete(() -> deleteTask(task));

                VBox targetColumn = switch (TaskValueMapper.normalizeStatus(task.getStatus())) {
                    case TaskValueMapper.STATUS_DONE -> doneColumn;
                    case TaskValueMapper.STATUS_IN_PROGRESS -> inProgressColumn;
                    default -> todoColumn;
                };

                addCardToKanbanColumn(targetColumn, row);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        addEmptyColumnPlaceholder(todoColumn, "No tasks in To Do");
        addEmptyColumnPlaceholder(inProgressColumn, "No tasks in progress");
        addEmptyColumnPlaceholder(doneColumn, "No completed tasks");
        updateKanbanCounts();
        animateKanbanColumnsEntrance();
    }

    private void refreshVisibleTaskView() {
        if (currentProject == null) {
            return;
        }
        loadProjectStats(currentProject.getId());
        loadRecentActivities(currentProject.getId());
        if (kanbanContainer != null && kanbanContainer.isVisible()) {
            loadKanbanColumns();
            return;
        }
        showTasksTab();
    }

    @FXML
    private void handleAddTask() {
        Node mainLayout = tasksContainer.getScene().getRoot();
        try {
            // 1. Load the FXML for the Add Task window
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/tezfx/view/AddTaskModal.fxml"));
            Parent root = loader.load();

            // 2. Get the controller to pass the current project ID
            AddTaskController controller = loader.getController();
            controller.setProjectId(currentProject.getId());

            // 3. Create a new Stage (Window)
            Stage popupStage = new Stage();
            popupStage.initStyle(StageStyle.TRANSPARENT);
            // Create the blur effect
            BoxBlur blur = new BoxBlur(8, 8, 3);
            ColorAdjust dim = new ColorAdjust();
            dim.setBrightness(-0.3);

            // Combine them: Apply blur to dim, then apply both to the main layout
            dim.setInput(blur);
            mainLayout.setEffect(dim);


            // 4. Set Modality: This blocks the main window until this one is closed
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.initOwner(tasksContainer.getScene().getWindow());


            // 5. Show and Wait
            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            popupStage.setScene(scene);
            popupStage.centerOnScreen();
            popupStage.showAndWait();

            // 3. CLEAN UP: Remove the effect when the popup is closed
            mainLayout.setEffect(null);

            // 6. Refresh the task list once the popup is closed
            refreshVisibleTaskView();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleExportReport() {
        if (currentProject == null || tasksContainer == null || tasksContainer.getScene() == null) {
            showAlert(Alert.AlertType.WARNING, "Export rapport", "Aucun projet sélectionné.");
            return;
        }

        List<Task> tasks = taskService.getTasksByProject(currentProject.getId());
        List<User> teamMembers = projectService.getProjectAssigneesMap().getOrDefault(currentProject.getId(), List.of());

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Enregistrer le rapport PDF");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF files", "*.pdf"));
        chooser.setInitialFileName(buildReportFileName(currentProject.getName()));

        File file = chooser.showSaveDialog(tasksContainer.getScene().getWindow());
        if (file == null) {
            return;
        }

        try {
            projectReportService.exportProjectPerformanceReport(
                    currentProject,
                    tasks,
                    teamMembers,
                    file.toPath()
            );
            showExportSuccessToast(file.getAbsolutePath());
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Export rapport", "Échec de génération du PDF:\n" + e.getMessage());
        }
    }

    private String buildReportFileName(String projectName) {
        String base = projectName == null || projectName.isBlank()
                ? "rapport-projet"
                : projectName.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
        return base + "-performance-report.pdf";
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        if (tasksContainer != null && tasksContainer.getScene() != null) {
            alert.initOwner(tasksContainer.getScene().getWindow());
        }
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showExportSuccessToast(String savedPath) {
        if (tasksContainer == null || tasksContainer.getScene() == null) {
            return;
        }

        Window owner = tasksContainer.getScene().getWindow();
        if (owner == null) {
            return;
        }

        Label toastLabel = new Label("Rapport PDF généré avec succès.\n" + savedPath);
        toastLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12;");

        HBox toastRoot = new HBox(toastLabel);
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

    @FXML
    private void handleDeleteProject(MouseEvent event) {
        if (currentProject == null) {
            return;
        }

        Node mainLayout = tasksContainer.getScene().getRoot();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/tezfx/view/DeleteConfirmModal.fxml"));
            Parent root = loader.load();

            DeleteConfirmModalController controller = loader.getController();
            controller.configure(
                    "Project",
                    currentProject.getName(),
                    "All tasks linked to this project will be removed as well.",
                    "Delete Project"
            );

            Stage popupStage = new Stage();
            popupStage.initStyle(StageStyle.TRANSPARENT);

            BoxBlur blur = new BoxBlur(8, 8, 3);
            ColorAdjust dim = new ColorAdjust();
            dim.setBrightness(-0.3);
            dim.setInput(blur);
            mainLayout.setEffect(dim);

            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.initOwner(tasksContainer.getScene().getWindow());

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            popupStage.setScene(scene);
            popupStage.centerOnScreen();
            popupStage.showAndWait();

            mainLayout.setEffect(null);

            if (controller.isConfirmed()) {
                boolean deleted = projectService.deleteProjectById(currentProject.getId());
                if (deleted) {
                    MainController.setView("project.fxml");
                }
            }
        } catch (IOException e) {
            mainLayout.setEffect(null);
            e.printStackTrace();
        }
    }

    @FXML
    private void handleEditProject(MouseEvent event) {
        if (currentProject == null) {
            return;
        }

        Node mainLayout = tasksContainer.getScene().getRoot();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/tezfx/view/UpdateProject.fxml"));
            Parent root = loader.load();

            UpdateProjectModalController controller = loader.getController();
            controller.setProject(currentProject);

            Stage popupStage = new Stage();
            popupStage.initStyle(StageStyle.TRANSPARENT);

            BoxBlur blur = new BoxBlur(8, 8, 3);
            ColorAdjust dim = new ColorAdjust();
            dim.setBrightness(-0.3);
            dim.setInput(blur);
            mainLayout.setEffect(dim);

            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.initOwner(tasksContainer.getScene().getWindow());

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            popupStage.setScene(scene);
            popupStage.centerOnScreen();
            popupStage.showAndWait();

            mainLayout.setEffect(null);

            if (controller.isSaved() && controller.getUpdatedProject() != null) {
                currentProject = controller.getUpdatedProject();
                ProjectDataLoad(currentProject);
            }
        } catch (IOException e) {
            mainLayout.setEffect(null);
            e.printStackTrace();
        }
    }

    private void openUpdateTaskModal(Task task) {
        if (task == null) return;
        if (!canCurrentUserModifyTask(task)) return;
        Node mainLayout = tasksContainer.getScene().getRoot();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/tezfx/view/UpdateTaskModal.fxml"));
            Parent root = loader.load();

            UpdateTaskModalController controller = loader.getController();
            controller.setTask(task);

            Stage popupStage = new Stage();
            popupStage.initStyle(StageStyle.TRANSPARENT);

            BoxBlur blur = new BoxBlur(8, 8, 3);
            ColorAdjust dim = new ColorAdjust();
            dim.setBrightness(-0.3);
            dim.setInput(blur);
            mainLayout.setEffect(dim);

            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.initOwner(tasksContainer.getScene().getWindow());

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            popupStage.setScene(scene);
            popupStage.centerOnScreen();
            popupStage.showAndWait();

            mainLayout.setEffect(null);

            if (controller.isSaved()) {
                refreshVisibleTaskView();
            }
        } catch (IOException e) {
            mainLayout.setEffect(null);
            e.printStackTrace();
        }
    }

    private void deleteTask(Task task) {
        if (task == null) return;
        if (!canCurrentUserModifyTask(task)) return;
        Node mainLayout = tasksContainer.getScene().getRoot();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/tezfx/view/DeleteConfirmModal.fxml"));
            Parent root = loader.load();

            DeleteConfirmModalController controller = loader.getController();
            controller.configure(
                    "Task",
                    task.getTitle(),
                    "This task will be removed from your project.",
                    "Delete Task"
            );

            Stage popupStage = new Stage();
            popupStage.initStyle(StageStyle.TRANSPARENT);

            BoxBlur blur = new BoxBlur(8, 8, 3);
            ColorAdjust dim = new ColorAdjust();
            dim.setBrightness(-0.3);
            dim.setInput(blur);
            mainLayout.setEffect(dim);

            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.initOwner(tasksContainer.getScene().getWindow());

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            popupStage.setScene(scene);
            popupStage.centerOnScreen();
            popupStage.showAndWait();

            mainLayout.setEffect(null);

            if (controller.isConfirmed() && taskService.deleteTaskById(task.getId())) {
                refreshVisibleTaskView();
            }
        } catch (IOException e) {
            mainLayout.setEffect(null);
            e.printStackTrace();
        }
    }
    private void setupDropTarget(VBox column, String newStatus) {
        if (column == null) {
            return;
        }
        setupDropTarget((Node) column, newStatus);
    }

    private void setupDropTarget(ScrollPane scrollPane, String newStatus) {
        if (scrollPane == null) {
            return;
        }
        setupDropTarget((Node) scrollPane, newStatus);
    }

    private void setupDropTarget(Node target, String newStatus) {
        if (target == null) {
            return;
        }
        target.setOnDragOver(event -> {
            if (event.getGestureSource() != target && event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.MOVE);
            }
            event.consume();
        });

        target.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;

            if (db.hasString()) {
                try {
                    int taskId = Integer.parseInt(db.getString());
                    Task draggedTask = kanbanTaskById.get(taskId);
                    if (!canCurrentUserModifyTask(draggedTask)) {
                        event.setDropCompleted(false);
                        event.consume();
                        return;
                    }
                    success = taskService.updateTaskStatus(taskId, newStatus);
                    if (success && currentProject != null) {
                        moveDraggedTaskToColumn(event.getGestureSource(), newStatus);
                        loadProjectStats(currentProject.getId());
                        loadRecentActivities(currentProject.getId());
                        updateKanbanCounts();
                    }
                } catch (NumberFormatException ignored) {
                    success = false;
                }
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }

    private boolean canCurrentUserModifyTask(Task task) {
        if (task == null) {
            return false;
        }
        User currentUser = UserService.getCurrentUser();
        if (currentUser == null || currentUser.getRole() == null) {
            return false;
        }
        String normalizedRole = currentUser.getRole().trim().toUpperCase(Locale.ROOT);
        if ("MANAGER".equals(normalizedRole)) {
            return true;
        }
        return task.getAssignedTo() == currentUser.getId();
    }

    private void addCardToKanbanColumn(VBox column, Parent row) {
        if (row instanceof Region region) {
            region.setMaxWidth(Double.MAX_VALUE);
        }
        row.getStyleClass().add("kanban-task-card");
        column.getChildren().add(row);
    }

    private void moveDraggedTaskToColumn(Object gestureSource, String newStatus) {
        if (!(gestureSource instanceof Node draggedNode)) {
            return;
        }
        VBox targetColumn = resolveColumnByStatus(newStatus);
        if (targetColumn == null) {
            return;
        }

        clearEmptyPlaceholder(todoColumn);
        clearEmptyPlaceholder(inProgressColumn);
        clearEmptyPlaceholder(doneColumn);

        if (draggedNode.getParent() instanceof Pane currentParent) {
            currentParent.getChildren().remove(draggedNode);
        }
        if (!targetColumn.getChildren().contains(draggedNode)) {
            targetColumn.getChildren().add(draggedNode);
        }
        syncDraggedTaskVisualStatus(draggedNode, newStatus);

        addEmptyColumnPlaceholder(todoColumn, "No tasks in To Do");
        addEmptyColumnPlaceholder(inProgressColumn, "No tasks in progress");
        addEmptyColumnPlaceholder(doneColumn, "No completed tasks");
        animateMovedKanbanCard(draggedNode);
    }

    private VBox resolveColumnByStatus(String status) {
        String normalized = TaskValueMapper.normalizeStatus(status);
        return switch (normalized) {
            case TaskValueMapper.STATUS_DONE -> doneColumn;
            case TaskValueMapper.STATUS_IN_PROGRESS -> inProgressColumn;
            default -> todoColumn;
        };
    }

    private void clearEmptyPlaceholder(VBox column) {
        if (column == null) {
            return;
        }
        column.getChildren().removeIf(node -> node instanceof Label);
    }

    private void animateMovedKanbanCard(Node card) {
        card.setOpacity(0.75);
        card.setTranslateY(-6);

        FadeTransition fade = new FadeTransition(Duration.millis(180), card);
        fade.setFromValue(0.75);
        fade.setToValue(1.0);

        TranslateTransition settle = new TranslateTransition(Duration.millis(180), card);
        settle.setFromY(-6);
        settle.setToY(0);

        new ParallelTransition(fade, settle).play();
    }

    private void syncDraggedTaskVisualStatus(Node draggedNode, String newStatus) {
        Object controllerObj = draggedNode.getProperties().get("taskRowController");
        if (controllerObj instanceof TaskRowController taskRowController) {
            taskRowController.setStatusFromBoard(newStatus);
        }
    }

    private void addEmptyColumnPlaceholder(VBox column, String text) {
        if (column == null || !column.getChildren().isEmpty()) {
            return;
        }
        Label emptyLabel = new Label(text);
        emptyLabel.getStyleClass().add("text-muted");
        emptyLabel.setWrapText(true);
        emptyLabel.setStyle("-fx-font-size: 12; -fx-padding: 8 4;");
        column.getChildren().add(emptyLabel);
    }

    private void updateKanbanCounts() {
        if (todoCountLabel != null && todoColumn != null) {
            todoCountLabel.setText(String.valueOf(countKanbanTasks(todoColumn)));
        }
        if (inProgressCountLabel != null && inProgressColumn != null) {
            inProgressCountLabel.setText(String.valueOf(countKanbanTasks(inProgressColumn)));
        }
        if (doneCountLabel != null && doneColumn != null) {
            doneCountLabel.setText(String.valueOf(countKanbanTasks(doneColumn)));
        }
    }

    private int countKanbanTasks(VBox column) {
        if (column == null) {
            return 0;
        }
        int count = 0;
        for (Node node : column.getChildren()) {
            if (!(node instanceof Label)) {
                count++;
            }
        }
        return count;
    }

    private void animateKanbanContainerReveal() {
        if (kanbanContainer == null) {
            return;
        }
        kanbanContainer.setOpacity(0);
        kanbanContainer.setTranslateY(10);

        FadeTransition fade = new FadeTransition(Duration.millis(260), kanbanContainer);
        fade.setFromValue(0);
        fade.setToValue(1);

        TranslateTransition slide = new TranslateTransition(Duration.millis(260), kanbanContainer);
        slide.setFromY(10);
        slide.setToY(0);

        new ParallelTransition(fade, slide).play();
    }

    private void animateKanbanColumnsEntrance() {
        animateKanbanColumn(todoColumn, 0);
        animateKanbanColumn(inProgressColumn, 1);
        animateKanbanColumn(doneColumn, 2);
    }

    private void animateKanbanColumn(VBox column, int columnIndex) {
        if (column == null || column.getChildren().isEmpty()) {
            return;
        }
        AtomicInteger itemIndex = new AtomicInteger(0);
        for (Node node : column.getChildren()) {
            node.setOpacity(0);
            node.setTranslateY(14);

            FadeTransition fade = new FadeTransition(Duration.millis(220), node);
            fade.setFromValue(0);
            fade.setToValue(1);

            TranslateTransition slide = new TranslateTransition(Duration.millis(220), node);
            slide.setFromY(14);
            slide.setToY(0);

            ParallelTransition cardTransition = new ParallelTransition(fade, slide);
            cardTransition.setDelay(Duration.millis(columnIndex * 70L + itemIndex.getAndIncrement() * 40L));
            cardTransition.play();
        }
    }


}