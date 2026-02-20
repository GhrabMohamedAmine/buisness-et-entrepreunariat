package controllers;

import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import entities.Project;
import entities.Task;
import entities.User;
import services.ProjectService;
import services.TaskService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.effect.BoxBlur;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;


public class HelloController {
    private static final int RECENT_PROJECTS_LIMIT = 5;
    private static final int CURRENT_USER_ID = 1;
    private static final int MAX_VISIBLE_ASSIGNEE_AVATARS = 4;
    private static final DateTimeFormatter DATE_INPUT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DATE_OUTPUT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    @FXML private VBox recentProjectsContainer;
    @FXML private Label tasksInProgressKpiLabel;
    @FXML private Label completedTasksKpiLabel;
    @FXML private Label totalProjectsKpiLabel;
    @FXML private Label upcomingDeadlinesKpiLabel;
    @FXML private Label overdueUndoneKpiLabel;
    @FXML private VBox tasksListContainer;

    private final ProjectService projectService = new ProjectService();
    private final TaskService taskService = new TaskService();
    private Map<Integer, List<User>> assigneesByProject = Map.of();
    private static final String[] AVATAR_COLOR_CLASSES = {
            "purple", "blue", "green", "orange"
    };

    @FXML
    public void initialize() {
        loadData();
    }
    @FXML
    private void openAddProjectPopup() {
        if (recentProjectsContainer == null || recentProjectsContainer.getScene() == null) {
            return;
        }
        Node mainLayout = recentProjectsContainer.getScene().getRoot();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/tezfx/view/add-project.fxml"));
            Parent root = loader.load();

            AddProjectController popupcontroller = loader.getController();
            popupcontroller.setOnProjectCreated(this::loadData);

            Stage popupStage = new Stage();
            popupStage.initStyle(StageStyle.TRANSPARENT);

            BoxBlur blur = new BoxBlur(8, 8, 3);
            ColorAdjust dim = new ColorAdjust();
            dim.setBrightness(-0.3);
            dim.setInput(blur);
            mainLayout.setEffect(dim);

            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.initOwner(recentProjectsContainer.getScene().getWindow());

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            popupStage.setScene(scene);
            popupStage.centerOnScreen();
            popupStage.showAndWait();

            mainLayout.setEffect(null);
            loadData();
        } catch (Exception e) {
            mainLayout.setEffect(null);
            e.printStackTrace();
        }
    }

    @FXML
    private void openPersonalTaskModal() {
        if (tasksListContainer == null || tasksListContainer.getScene() == null) {
            return;
        }
        Node mainLayout = tasksListContainer.getScene().getRoot();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/tezfx/view/AddTaskModal.fxml"));
            Parent root = loader.load();

            AddTaskController controller = loader.getController();
            controller.setAssignedUserId(CURRENT_USER_ID);
            controller.setSubtitleText("Create a personal task assigned to you");

            Stage popupStage = new Stage();
            popupStage.initStyle(StageStyle.TRANSPARENT);

            BoxBlur blur = new BoxBlur(8, 8, 3);
            ColorAdjust dim = new ColorAdjust();
            dim.setBrightness(-0.3);
            dim.setInput(blur);
            mainLayout.setEffect(dim);

            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.initOwner(tasksListContainer.getScene().getWindow());

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            popupStage.setScene(scene);
            popupStage.centerOnScreen();
            popupStage.showAndWait();

            mainLayout.setEffect(null);
            loadData();
        } catch (Exception e) {
            mainLayout.setEffect(null);
            e.printStackTrace();
        }
    }

    public void loadData() {
        assigneesByProject = projectService.getProjectAssigneesMap();
        List<Project> projectsFromDB = projectService.getAllProjects().stream()
                .filter(project -> taskService.getTaskCount(project.getId()) > 0)
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        projectsFromDB.sort(Comparator.comparingInt(Project::getId).reversed());
        int visibleCount = Math.min(projectsFromDB.size(), RECENT_PROJECTS_LIMIT);
        List<Project> recentProjects = projectsFromDB.subList(0, visibleCount);
        renderRecentProjects(recentProjects);
        loadCurrentUserTasks();
        loadKpis();
    }

    private void renderRecentProjects(List<Project> projects) {
        if (recentProjectsContainer == null) {
            return;
        }
        recentProjectsContainer.getChildren().clear();
        if (projects == null || projects.isEmpty()) {
            Label emptyLabel = new Label("No recent projects with tasks.");
            emptyLabel.getStyleClass().add("task-time");
            recentProjectsContainer.getChildren().add(emptyLabel);
            return;
        }

        for (Project project : projects) {
            recentProjectsContainer.getChildren().add(buildRecentProjectCard(project));
        }
    }

    private VBox buildRecentProjectCard(Project project) {
        VBox card = new VBox(10);
        card.getStyleClass().add("recent-project-item");

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        Label name = new Label(project.getName());
        name.getStyleClass().add("recent-project-name");
        name.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(name, Priority.ALWAYS);

        Label percent = new Label(project.getProgress() + "%");
        percent.getStyleClass().add("recent-project-percent");

        header.getChildren().addAll(name, percent);

        ProgressBar bar = new ProgressBar(project.getProgress() / 100.0);
        bar.getStyleClass().add("projects-progress-bar");
        bar.setPrefHeight(8);
        bar.setMaxWidth(Double.MAX_VALUE);

        HBox metaRow = new HBox(10);
        metaRow.setAlignment(Pos.CENTER_LEFT);

        Label startDate = new Label("Start " + formatDate(project.getStartDate()));
        startDate.getStyleClass().add("recent-project-meta");

        Label endDate = new Label("End " + formatDate(project.getEndDate()));
        endDate.getStyleClass().add("recent-project-meta");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox avatars = new HBox(-6);
        avatars.setAlignment(Pos.CENTER_LEFT);

        List<User> users = assigneesByProject.getOrDefault(project.getId(), List.of());
        if (users.isEmpty()) {
            users = new ArrayList<>();
            users.add(new User(0, "Unassigned", ""));
        }

        int visible = Math.min(users.size(), MAX_VISIBLE_ASSIGNEE_AVATARS);
        for (int i = 0; i < visible; i++) {
            StackPane avatar = buildInitialAvatar(users.get(i).getFullName());
            if (i > 0) {
                HBox.setMargin(avatar, new Insets(0, 0, 0, -6));
            }
            avatars.getChildren().add(avatar);
        }
        if (users.size() > MAX_VISIBLE_ASSIGNEE_AVATARS) {
            StackPane countAvatar = buildCountAvatar("+" + (users.size() - MAX_VISIBLE_ASSIGNEE_AVATARS));
            HBox.setMargin(countAvatar, new Insets(0, 0, 0, -6));
            avatars.getChildren().add(countAvatar);
        }
        String names = users.stream().map(User::getFullName).reduce((a, b) -> a + ", " + b).orElse("Unassigned");
        Tooltip.install(avatars, new Tooltip(names));

        metaRow.getChildren().addAll(startDate, endDate, spacer, avatars);

        card.getChildren().addAll(header, bar, metaRow);
        return card;
    }

    private void loadKpis() {
        totalProjectsKpiLabel.setText(String.valueOf(projectService.getTotalProjectsCount()));
        tasksInProgressKpiLabel.setText(String.valueOf(taskService.getTasksInProgressCount()));
        completedTasksKpiLabel.setText(String.valueOf(taskService.getCompletedTasksCount()));
        upcomingDeadlinesKpiLabel.setText(String.valueOf(taskService.getUpcomingDeadlinesCount(3)));
        overdueUndoneKpiLabel.setText(String.valueOf(taskService.getOverdueUndoneTasksCount()));
    }

    private void loadCurrentUserTasks() {
        if (tasksListContainer == null) {
            return;
        }
        tasksListContainer.getChildren().clear();
        List<Task> tasks = taskService.getTasksByAssignedUser(CURRENT_USER_ID);
        if (tasks.isEmpty()) {
            Label emptyLabel = new Label("No tasks assigned.");
            emptyLabel.getStyleClass().add("task-time");
            tasksListContainer.getChildren().add(emptyLabel);
            return;
        }

        for (Task task : tasks) {
            tasksListContainer.getChildren().add(buildTaskRow(task));
        }
    }

    private HBox buildTaskRow(Task task) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("task-row-item");

        String normalizedStatus = TaskValueMapper.normalizeStatus(task.getStatus());
        boolean done = TaskValueMapper.STATUS_DONE.equals(normalizedStatus);

        CheckBox statusCheck = new CheckBox("");
        statusCheck.getStyleClass().add("task-check");
        statusCheck.setSelected(done);

        Label titleLabel = new Label(task.getTitle());
        titleLabel.getStyleClass().add(done ? "task-name-done" : "task-name");
        titleLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        FontIcon inProgressIcon = new FontIcon("mdi2c-clock-outline");
        inProgressIcon.setIconSize(18);
        inProgressIcon.getStyleClass().add("gray-icon");
        updateInProgressIconStyle(inProgressIcon, normalizedStatus);

        Label priority = new Label(TaskValueMapper.normalizePriority(task.getPriority()));
        priority.getStyleClass().add(TaskValueMapper.priorityPillStyleClass(task.getPriority()));

        Label status = new Label(TaskValueMapper.toStatusLabel(normalizedStatus));
        status.getStyleClass().add("task-status-pill");
        status.getStyleClass().add(TaskValueMapper.statusPillStyleClass(normalizedStatus));

        statusCheck.setOnAction(e -> {
            String newStatus = statusCheck.isSelected() ? TaskValueMapper.STATUS_DONE : TaskValueMapper.STATUS_TODO;
            boolean updated = taskService.updateTaskStatus(task.getId(), newStatus);
            if (!updated) {
                statusCheck.setSelected(!statusCheck.isSelected());
                return;
            }
            loadData();
        });

        inProgressIcon.setOnMouseClicked(e -> {
            if (TaskValueMapper.STATUS_IN_PROGRESS.equals(TaskValueMapper.normalizeStatus(task.getStatus()))) {
                return;
            }
            boolean updated = taskService.updateTaskStatus(task.getId(), TaskValueMapper.STATUS_IN_PROGRESS);
            if (updated) {
                loadData();
            }
        });

        row.getChildren().addAll(statusCheck, inProgressIcon, titleLabel, priority, status);
        return row;
    }

    private void updateInProgressIconStyle(FontIcon inProgressIcon, String normalizedStatus) {
        inProgressIcon.getStyleClass().removeAll("gray-icon", "orange-icon");
        if (TaskValueMapper.STATUS_IN_PROGRESS.equals(normalizedStatus)) {
            inProgressIcon.getStyleClass().add("orange-icon");
        } else {
            inProgressIcon.getStyleClass().add("gray-icon");
        }
    }

    private StackPane buildInitialAvatar(String fullName) {
        String safeName = (fullName == null || fullName.isBlank()) ? "U" : fullName;
        String initial = String.valueOf(Character.toUpperCase(safeName.charAt(0)));
        String colorClass = pickAvatarColorClass(safeName);

        return buildAvatar(initial, colorClass);
    }

    private StackPane buildCountAvatar(String text) {
        return buildAvatar(text, "dark");
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

    private String formatDate(String dateStr) {
        try {
            return LocalDate.parse(dateStr, DATE_INPUT).format(DATE_OUTPUT);
        } catch (Exception e) {
            return dateStr;
        }
    }
}
