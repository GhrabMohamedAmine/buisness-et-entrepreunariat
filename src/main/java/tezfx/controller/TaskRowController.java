package tezfx.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import tezfx.model.Task;

public class TaskRowController {
    // Add assignedUser here
    @FXML private Label taskTitle, taskDesc, priorityLabel, statusLabel, dueDateLabel, assignedUser;
    private Runnable onEdit;
    private Runnable onDelete;

    public void setTaskData(Task task) {
        if (task == null) return;

        taskTitle.setText(task.getTitle());
        taskDesc.setText(task.getDescription() != null ? task.getDescription() : "No description");
        dueDateLabel.setText(task.getDueDate() != null ? task.getDueDate() : "No date");

        // SET THE ASSIGNED USER NAME
        // Assuming your Task model has getAssignedTo() or getAssignedUser()
        String user = task.getAssignedToName();
        if (assignedUser != null) {
            assignedUser.setText(task.getAssignedToName());
        }

        updateStatusStyle(task.getStatus());
        updatePriorityStyle(task.getPriority());
    }

    private void updateStatusStyle(String status) {
        String normalized = normalizeStatus(status);
        statusLabel.setText(formatStatusLabel(normalized));
        statusLabel.getStyleClass().removeAll("badge-todo", "badge-progress", "badge-done");

        if ("DONE".equals(normalized)) {
            statusLabel.getStyleClass().add("badge-done");
        } else if ("IN_PROGRESS".equals(normalized)) {
            statusLabel.getStyleClass().add("badge-progress");
        } else {
            statusLabel.getStyleClass().add("badge-todo");
        }
    }

    private void updatePriorityStyle(String priority) {
        String normalized = priority == null ? "" : priority.trim().toUpperCase();
        priorityLabel.setText(normalized);
        priorityLabel.getStyleClass().removeAll("badge-priority-low", "badge-priority-medium", "badge-priority-high");

        if ("HIGH".equals(normalized)) {
            priorityLabel.getStyleClass().add("badge-priority-high");
        } else if ("MEDIUM".equals(normalized)) {
            priorityLabel.getStyleClass().add("badge-priority-medium");
        } else {
            priorityLabel.getStyleClass().add("badge-priority-low");
        }
    }

    private String normalizeStatus(String status) {
        if (status == null) return "TODO";
        String normalized = status.trim().toUpperCase().replace(' ', '_').replace('-', '_');
        if ("TO_DO".equals(normalized)) return "TODO";
        return normalized;
    }

    private String formatStatusLabel(String normalizedStatus) {
        if ("DONE".equals(normalizedStatus)) return "Done";
        if ("IN_PROGRESS".equals(normalizedStatus)) return "In Progress";
        return "To Do";
    }

    public void setOnEdit(Runnable onEdit) {
        this.onEdit = onEdit;
    }

    public void setOnDelete(Runnable onDelete) {
        this.onDelete = onDelete;
    }

    @FXML
    private void onEditTask(MouseEvent event) {
        if (onEdit != null) onEdit.run();
    }

    @FXML
    private void onDeleteTask(MouseEvent event) {
        if (onDelete != null) onDelete.run();
    }
}
