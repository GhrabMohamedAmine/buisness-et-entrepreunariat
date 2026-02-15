package tezfx.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.effect.BoxBlur;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.kordamp.ikonli.javafx.FontIcon;
import tezfx.model.Project;
import tezfx.model.Task;
import tezfx.model.sql;

import java.io.IOException;
import java.util.Locale;
import java.util.List;

public class TasksController {
    @FXML private VBox tasksGroupsContainer;
    @FXML private TextField searchField;

    private final sql dao = new sql();

    @FXML
    public void initialize() {
        loadTasks(null);
        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldValue, newValue) -> loadTasks(newValue));
        }
    }

    private void loadTasks(String query) {
        tasksGroupsContainer.getChildren().clear();

        String normalizedQuery = normalizeSearch(query);
        List<Project> projects = dao.getAllProjects();
        int colorIndex = 0;

        for (Project project : projects) {
            List<Task> projectTasks = dao.getTasksByProject(project.getId());
            if (!normalizedQuery.isEmpty()) {
                final String projectName = project.getName();
                projectTasks = projectTasks.stream()
                        .filter(task -> matchesQuery(normalizedQuery, projectName, task))
                        .toList();
            }

            if (projectTasks.isEmpty()) {
                continue;
            }

            tasksGroupsContainer.getChildren().add(createProjectBlock(project, projectTasks, colorIndex++));
        }
    }

    private VBox createProjectBlock(Project project, List<Task> tasks, int colorIndex) {
        VBox block = new VBox();
        block.getStyleClass().add("task-project-block");

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("task-project-header");

        FontIcon chevron = new FontIcon("mdi2c-chevron-down");
        chevron.setIconSize(14);
        chevron.getStyleClass().add("task-muted-icon");

        StackPane dot = new StackPane();
        dot.getStyleClass().add(switch (colorIndex % 3) {
            case 1 -> "task-project-dot-blue";
            case 2 -> "task-project-dot-pink";
            default -> "task-project-dot-purple";
        });

        Label name = new Label(project.getName());
        name.getStyleClass().add("task-project-name");

        Label count = new Label(String.valueOf(tasks.size()));
        count.getStyleClass().add("task-count-pill");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label plus = new Label("+");
        plus.getStyleClass().add("task-plus");
        plus.setOnMouseClicked(e -> openAddTaskModal(project));

        header.getChildren().addAll(chevron, dot, name, count, spacer, plus);
        block.getChildren().add(header);

        for (Task task : tasks) {
            block.getChildren().add(createTaskRow(task));
        }

        return block;
    }

    private HBox createTaskRow(Task task) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("task-row-item");

        CheckBox doneCheck = new CheckBox();
        doneCheck.getStyleClass().add("task-check");

        boolean completed = isDone(task.getStatus());
        doneCheck.setSelected(completed);

        Label title = new Label(task.getTitle());
        title.getStyleClass().add(completed ? "task-name-done" : "task-name");
        title.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(title, Priority.ALWAYS);

        Label priority = new Label(normalizePriority(task.getPriority()));
        priority.getStyleClass().add(getPriorityStyleClass(task.getPriority()));

        HBox dateBox = new HBox(6);
        dateBox.setAlignment(Pos.CENTER_LEFT);
        dateBox.getStyleClass().add("task-date-box");

        FontIcon calendar = new FontIcon("mdi2c-calendar-month-outline");
        calendar.setIconSize(13);
        calendar.getStyleClass().add("task-muted-icon");

        Label dueDate = new Label(task.getDueDate() == null || task.getDueDate().isBlank() ? "-" : task.getDueDate());
        dueDate.getStyleClass().add("task-date-text");
        dateBox.getChildren().addAll(calendar, dueDate);

        Label assignedTo = new Label(task.getAssignedToName());
        assignedTo.getStyleClass().add("task-assignee-pill");

        FontIcon editIcon = new FontIcon("mdi2p-pencil-outline");
        editIcon.setIconSize(18);
        editIcon.getStyleClass().add("btn-icon-outline");
        editIcon.setOnMouseClicked(e -> openUpdateTaskModal(task));

        FontIcon deleteIcon = new FontIcon("mdi2d-delete-empty-outline");
        deleteIcon.setIconSize(18);
        deleteIcon.getStyleClass().add("btn-icon-danger");
        deleteIcon.setOnMouseClicked(e -> deleteTask(task));

        doneCheck.setOnAction(e -> {
            String newStatus = doneCheck.isSelected() ? "DONE" : "TODO";
            boolean updated = dao.updateTaskStatus(task.getId(), newStatus);
            if (!updated) {
                doneCheck.setSelected(!doneCheck.isSelected());
                return;
            }
            loadTasks(currentSearchQuery());
        });

        row.getChildren().addAll(doneCheck, title, priority, dateBox, assignedTo, editIcon, deleteIcon);
        return row;
    }

    private boolean isDone(String status) {
        if (status == null) return false;
        String normalized = status.trim().toUpperCase().replace(' ', '_').replace('-', '_');
        return "DONE".equals(normalized);
    }

    private String normalizePriority(String priority) {
        if (priority == null || priority.isBlank()) {
            return "LOW";
        }
        return priority.trim().toUpperCase();
    }

    private String getPriorityStyleClass(String priority) {
        String normalized = normalizePriority(priority);
        if ("HIGH".equals(normalized)) return "task-priority-high";
        if ("MEDIUM".equals(normalized)) return "task-priority-medium";
        return "task-priority-low";
    }

    private String normalizeSearch(String value) {
        if (value == null) return "";
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private boolean matchesQuery(String normalizedQuery, String projectName, Task task) {
        return containsIgnoreCase(projectName, normalizedQuery)
                || containsIgnoreCase(task.getTitle(), normalizedQuery)
                || containsIgnoreCase(task.getAssignedToName(), normalizedQuery)
                || containsIgnoreCase(task.getDueDate(), normalizedQuery)
                || containsIgnoreCase(task.getStatus(), normalizedQuery)
                || containsIgnoreCase(task.getPriority(), normalizedQuery);
    }

    private boolean containsIgnoreCase(String source, String normalizedQuery) {
        if (source == null || source.isBlank()) return false;
        return source.toLowerCase(Locale.ROOT).contains(normalizedQuery);
    }

    private void openUpdateTaskModal(Task task) {
        if (task == null) return;
        Node mainLayout = tasksGroupsContainer.getScene().getRoot();
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
            popupStage.initOwner(tasksGroupsContainer.getScene().getWindow());

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            popupStage.setScene(scene);
            popupStage.centerOnScreen();
            popupStage.showAndWait();

            mainLayout.setEffect(null);

            if (controller.isSaved()) {
                loadTasks(currentSearchQuery());
            }
        } catch (IOException e) {
            mainLayout.setEffect(null);
            e.printStackTrace();
        }
    }

    private void openAddTaskModal(Project project) {
        if (project == null) return;
        Node mainLayout = tasksGroupsContainer.getScene().getRoot();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/tezfx/view/AddTaskModal.fxml"));
            Parent root = loader.load();

            AddTaskController controller = loader.getController();
            controller.setProjectId(project.getId());

            Stage popupStage = new Stage();
            popupStage.initStyle(StageStyle.TRANSPARENT);

            BoxBlur blur = new BoxBlur(8, 8, 3);
            ColorAdjust dim = new ColorAdjust();
            dim.setBrightness(-0.3);
            dim.setInput(blur);
            mainLayout.setEffect(dim);

            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.initOwner(tasksGroupsContainer.getScene().getWindow());

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            popupStage.setScene(scene);
            popupStage.centerOnScreen();
            popupStage.showAndWait();

            mainLayout.setEffect(null);
            loadTasks(currentSearchQuery());
        } catch (IOException e) {
            mainLayout.setEffect(null);
            e.printStackTrace();
        }
    }

    private void deleteTask(Task task) {
        if (task == null) return;
        Node mainLayout = tasksGroupsContainer.getScene().getRoot();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/tezfx/view/DeleteTaskModal.fxml"));
            Parent root = loader.load();

            DeleteTaskModalController controller = loader.getController();
            controller.setTaskName(task.getTitle());

            Stage popupStage = new Stage();
            popupStage.initStyle(StageStyle.TRANSPARENT);

            BoxBlur blur = new BoxBlur(8, 8, 3);
            ColorAdjust dim = new ColorAdjust();
            dim.setBrightness(-0.3);
            dim.setInput(blur);
            mainLayout.setEffect(dim);

            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.initOwner(tasksGroupsContainer.getScene().getWindow());

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            popupStage.setScene(scene);
            popupStage.centerOnScreen();
            popupStage.showAndWait();

            mainLayout.setEffect(null);

            if (controller.isConfirmed() && dao.deleteTaskById(task.getId())) {
                loadTasks(currentSearchQuery());
            }
        } catch (IOException e) {
            mainLayout.setEffect(null);
            e.printStackTrace();
        }
    }

    private String currentSearchQuery() {
        return searchField == null ? null : searchField.getText();
    }
}
