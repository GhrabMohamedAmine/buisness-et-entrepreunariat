package controllers;

import entities.Project;
import entities.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import services.CurrentUserService;
import services.ProjectService;
import services.TaskService;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TasksKanbanController {
    @FXML private VBox todoColumn;
    @FXML private VBox inProgressColumn;
    @FXML private VBox doneColumn;
    @FXML private ScrollPane todoScrollPane;
    @FXML private ScrollPane inProgressScrollPane;
    @FXML private ScrollPane doneScrollPane;
    @FXML private Label todoCountLabel;
    @FXML private Label inProgressCountLabel;
    @FXML private Label doneCountLabel;
    @FXML private Label subtitleLabel;

    private final ProjectService projectService = new ProjectService();
    private final TaskService taskService = new TaskService();
    private final CurrentUserService currentUserService = new CurrentUserService();
    private final Map<Integer, Task> taskById = new HashMap<>();

    @FXML
    public void initialize() {
        setupDropTarget(todoColumn, TaskValueMapper.STATUS_TODO);
        setupDropTarget(inProgressColumn, TaskValueMapper.STATUS_IN_PROGRESS);
        setupDropTarget(doneColumn, TaskValueMapper.STATUS_DONE);
        setupDropTarget(todoScrollPane, TaskValueMapper.STATUS_TODO);
        setupDropTarget(inProgressScrollPane, TaskValueMapper.STATUS_IN_PROGRESS);
        setupDropTarget(doneScrollPane, TaskValueMapper.STATUS_DONE);
        if (subtitleLabel != null) {
            subtitleLabel.setText(currentUserService.isCurrentUserManager()
                    ? "All tasks across all projects"
                    : "Your tasks across all projects");
        }
        loadBoard();
    }

    @FXML
    private void onBackToTasks() {
        MainController.setView("tasks.fxml");
    }

    private void loadBoard() {
        todoColumn.getChildren().clear();
        inProgressColumn.getChildren().clear();
        doneColumn.getChildren().clear();
        taskById.clear();

        for (Project project : projectService.getAllProjects()) {
            List<Task> tasks = taskService.getTasksByProject(project.getId());
            for (Task task : tasks) {
                if (!shouldShowTask(task)) {
                    continue;
                }
                taskById.put(task.getId(), task);
                addTaskCard(task);
            }
        }

        addEmptyColumnPlaceholder(todoColumn, "No tasks in To Do");
        addEmptyColumnPlaceholder(inProgressColumn, "No tasks in progress");
        addEmptyColumnPlaceholder(doneColumn, "No completed tasks");
        updateCounts();
    }

    private boolean shouldShowTask(Task task) {
        if (task == null) {
            return false;
        }
        if (currentUserService.isCurrentUserManager()) {
            return true;
        }
        return task.getAssignedTo() == currentUserService.getCurrentUserId();
    }

    private void addTaskCard(Task task) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/tezfx/view/components/TaskRow.fxml"));
            Parent row = loader.load();
            TaskRowController controller = loader.getController();
            row.getProperties().put("taskRowController", controller);

            controller.setTaskData(task);
            controller.setKanbanMode(true);
            controller.setStatusEditingAllowed(canCurrentUserModifyTaskStatus(task));
            controller.setOnStatusToggle(selected -> updateTaskStatus(task, selected ? TaskValueMapper.STATUS_DONE : TaskValueMapper.STATUS_TODO));
            controller.setOnStatusChange(newStatus -> updateTaskStatus(task, newStatus));

            if (row instanceof Region region) {
                region.setMaxWidth(Double.MAX_VALUE);
            }

            VBox targetColumn = resolveColumnByStatus(task.getStatus());
            targetColumn.getChildren().add(row);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean updateTaskStatus(Task task, String newStatus) {
        if (!canCurrentUserModifyTaskStatus(task)) {
            return false;
        }
        boolean updated = taskService.updateTaskStatus(task.getId(), newStatus);
        if (updated) {
            loadBoard();
        }
        return updated;
    }

    private void setupDropTarget(Node target, String newStatus) {
        if (target == null) {
            return;
        }
        target.setOnDragOver(event -> {
            if (event.getDragboard().hasString()) {
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
                    Task task = taskById.get(taskId);
                    success = updateTaskStatus(task, newStatus);
                } catch (NumberFormatException ignored) {
                    success = false;
                }
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }

    private VBox resolveColumnByStatus(String status) {
        return switch (TaskValueMapper.normalizeStatus(status)) {
            case TaskValueMapper.STATUS_DONE -> doneColumn;
            case TaskValueMapper.STATUS_IN_PROGRESS -> inProgressColumn;
            default -> todoColumn;
        };
    }

    private boolean canCurrentUserModifyTaskStatus(Task task) {
        if (task == null) {
            return false;
        }
        int assigneeId = task.getAssignedTo();
        return assigneeId > 0 && assigneeId == currentUserService.getCurrentUserId();
    }

    private void addEmptyColumnPlaceholder(VBox column, String text) {
        if (column == null || !column.getChildren().isEmpty()) {
            return;
        }
        Label label = new Label(text);
        label.getStyleClass().add("text-muted");
        column.getChildren().add(label);
    }

    private void updateCounts() {
        updateCount(todoCountLabel, todoColumn);
        updateCount(inProgressCountLabel, inProgressColumn);
        updateCount(doneCountLabel, doneColumn);
    }

    private void updateCount(Label label, VBox column) {
        if (label == null || column == null) {
            return;
        }
        long taskCount = column.getChildren().stream()
                .filter(node -> node instanceof Parent)
                .filter(node -> node.getProperties().containsKey("taskRowController"))
                .count();
        label.setText(String.valueOf(taskCount));
    }
}
