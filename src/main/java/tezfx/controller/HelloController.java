package tezfx.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import tezfx.model.Project;
import tezfx.model.Task;
import tezfx.model.User;
import tezfx.model.sql; // You need to import your DAO
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static javafx.scene.layout.Region.USE_PREF_SIZE;

public class HelloController {
    private static final int RECENT_PROJECTS_LIMIT = 5;
    private static final int CURRENT_USER_ID = 1;
    private static final double PROJECT_ROW_HEIGHT = 56.0;
    private static final double PROJECT_HEADER_HEIGHT = 46.0;
    private static final DateTimeFormatter DATE_INPUT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DATE_OUTPUT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    @FXML private TableView<Project> projectsTable;
    @FXML private TableColumn<Project, String> nameCol;
    @FXML private TableColumn<Project, Number> progressCol;
    @FXML private TableColumn<Project, Number> teamCol; // Changed from String to Number
    @FXML private TableColumn<Project, String> startDateCol;
    @FXML private TableColumn<Project, String> endDateCol;
    @FXML private Label tasksInProgressKpiLabel;
    @FXML private Label completedTasksKpiLabel;
    @FXML private Label totalProjectsKpiLabel;
    @FXML private Label upcomingDeadlinesKpiLabel;
    @FXML private VBox tasksListContainer;

    private final sql projectDAO = new sql();
    private Map<Integer, List<User>> assigneesByProject = Map.of();
    private static final String[] AVATAR_COLOR_CLASSES = {
            "purple", "blue", "green", "orange"
    };

    @FXML
    public void initialize() {
        // 1. Basic Column Mapping
        nameCol.setCellValueFactory(data -> data.getValue().nameProperty());
        startDateCol.setCellValueFactory(cellData -> cellData.getValue().startDateProperty());
        endDateCol.setCellValueFactory(cellData -> cellData.getValue().endDateProperty());



        // 2. Progress Bar Column
        progressCol.setCellValueFactory(data -> data.getValue().progressProperty());
        progressCol.setCellFactory(col -> new TableCell<Project, Number>() {
            private final ProgressBar bar = new ProgressBar();
            private final Label percentLabel = new Label();
            private final HBox container = new HBox(10, bar, percentLabel);
            @Override
            protected void updateItem(Number value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setGraphic(null);
                } else {
                    if (!bar.getStyleClass().contains("projects-progress-bar")) {
                        bar.getStyleClass().add("projects-progress-bar");
                    }
                    bar.setProgress(value.doubleValue() / 100.0);
                    bar.setPrefWidth(130);

                    percentLabel.setText(((int) Math.round(value.doubleValue())) + "%");
                    if (!percentLabel.getStyleClass().contains("projects-progress-text")) {
                        percentLabel.getStyleClass().add("projects-progress-text");
                    }

                    container.setAlignment(Pos.CENTER_LEFT);
                    if (!container.getStyleClass().contains("projects-progress-wrap")) {
                        container.getStyleClass().add("projects-progress-wrap");
                    }
                    setGraphic(container);
                }
            }
        });

        // 3. Team/Assigned User Column
        teamCol.setCellValueFactory(data -> data.getValue().assignedToProperty());
        teamCol.setCellFactory(col -> new TableCell<Project, Number>() {
            @Override
            protected void updateItem(Number userId, boolean empty) {
                super.updateItem(userId, empty);
                Project rowProject = getTableRow() == null ? null : (Project) getTableRow().getItem();
                if (empty || rowProject == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    List<User> users = assigneesByProject.getOrDefault(rowProject.getId(), List.of());
                    if (users.isEmpty()) {
                        users = new ArrayList<>();
                        users.add(new User(0, "Unassigned", ""));
                    }

                    HBox wrapper = new HBox(4);
                    wrapper.setStyle("-fx-alignment: CENTER_LEFT;");
                    for (User user : users) {
                        wrapper.getChildren().add(buildInitialAvatar(user.getFullName()));
                    }

                    String names = users.stream().map(User::getFullName).reduce((a, b) -> a + ", " + b).orElse("Unassigned");
                    Tooltip.install(wrapper, new Tooltip(names));
                    setGraphic(wrapper);
                    setText(null);
                }
            }
        });

        startDateCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isBlank()) {
                    setText(null);
                } else {
                    setText(formatDate(item));
                }
            }
        });

        endDateCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isBlank()) {
                    setText(null);
                } else {
                    setText(formatDate(item));
                }
            }
        });

        // 4. LOAD DATA FROM DATABASE
        projectsTable.setFixedCellSize(PROJECT_ROW_HEIGHT);
        loadData();
    }
    @FXML
    private void openAddProjectPopup() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/tezfx/view/add-project.fxml"));
            Parent popup = loader.load();

            AddProjectController popupcontroller = loader.getController();
            popupcontroller.setParentController(this);
            MainController.getStaticContentArea().getChildren().add(popup);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadData() {
        assigneesByProject = projectDAO.getProjectAssigneesMap();
        List<Project> projectsFromDB = projectDAO.getAllProjects();
        int visibleCount = Math.min(projectsFromDB.size(), RECENT_PROJECTS_LIMIT);
        List<Project> recentProjects = projectsFromDB.subList(0, visibleCount);
        ObservableList<Project> observableList = FXCollections.observableArrayList(recentProjects);
        projectsTable.setItems(observableList);
        projectsTable.setPrefHeight(PROJECT_HEADER_HEIGHT + (visibleCount * PROJECT_ROW_HEIGHT));
        projectsTable.setMinHeight(USE_PREF_SIZE);
        projectsTable.setMaxHeight(USE_PREF_SIZE);
        loadCurrentUserTasks();
        loadKpis();
    }

    private void loadKpis() {
        totalProjectsKpiLabel.setText(String.valueOf(projectDAO.getTotalProjectsCount()));
        tasksInProgressKpiLabel.setText(String.valueOf(projectDAO.getTasksInProgressCount()));
        completedTasksKpiLabel.setText(String.valueOf(projectDAO.getCompletedTasksCount()));
        upcomingDeadlinesKpiLabel.setText(String.valueOf(projectDAO.getUpcomingDeadlinesCount(3)));
    }

    private void loadCurrentUserTasks() {
        if (tasksListContainer == null) {
            return;
        }
        tasksListContainer.getChildren().clear();
        List<Task> tasks = projectDAO.getTasksByAssignedUser(CURRENT_USER_ID);
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

        String normalizedStatus = normalizeStatus(task.getStatus());
        boolean done = "DONE".equals(normalizedStatus);

        CheckBox statusCheck = new CheckBox("");
        statusCheck.getStyleClass().add("task-check");
        statusCheck.setSelected(done);

        Label titleLabel = new Label(task.getTitle());
        titleLabel.getStyleClass().add(done ? "task-name-done" : "task-name");
        titleLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        Label assignedTo = new Label(task.getAssignedToName());
        assignedTo.getStyleClass().add("task-assignee-pill");

        statusCheck.setOnAction(e -> {
            String newStatus = statusCheck.isSelected() ? "DONE" : "TODO";
            boolean updated = projectDAO.updateTaskStatus(task.getId(), newStatus);
            if (!updated) {
                statusCheck.setSelected(!statusCheck.isSelected());
                return;
            }
            loadCurrentUserTasks();
            loadKpis();
        });

        row.getChildren().addAll(statusCheck, titleLabel, assignedTo);
        return row;
    }

    private String normalizeStatus(String status) {
        if (status == null) return "TODO";
        String normalized = status.trim().toUpperCase().replace(' ', '_').replace('-', '_');
        if ("TO_DO".equals(normalized) || "À_FAIRE".equals(normalized)) return "TODO";
        if ("EN_COURS".equals(normalized)) return "IN_PROGRESS";
        if ("TERMINE".equals(normalized) || "TERMINÉ".equals(normalized)) return "DONE";
        return normalized;
    }

    private StackPane buildInitialAvatar(String fullName) {
        String safeName = (fullName == null || fullName.isBlank()) ? "U" : fullName.trim();
        String initial = String.valueOf(Character.toUpperCase(safeName.charAt(0)));
        String colorClass = pickAvatarColorClass(safeName);

        Label initialLabel = new Label(initial);
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
