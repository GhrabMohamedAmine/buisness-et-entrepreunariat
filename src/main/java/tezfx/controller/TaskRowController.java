package tezfx.controller;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.event.ActionEvent;
import javafx.scene.input.MouseEvent;
import org.kordamp.ikonli.javafx.FontIcon;
import tezfx.model.Task;

import java.util.function.Function;

public class TaskRowController {
    // Add assignedUser here
    @FXML private Label taskTitle, taskDesc, priorityLabel, statusLabel, dueDateLabel, assignedUser;
    @FXML private CheckBox doneCheck;
    @FXML private FontIcon inProgressIcon;
    private Runnable onEdit;
    private Runnable onDelete;
    private Function<Boolean, Boolean> onStatusToggle;
    private Function<String, Boolean> onStatusChange;
    private String currentStatus = "TODO";

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

        String normalizedStatus = normalizeStatus(task.getStatus());
        currentStatus = normalizedStatus;
        if (doneCheck != null) {
            doneCheck.setSelected("DONE".equals(normalizedStatus));
        }
        updateStatusStyle(normalizedStatus);
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
        updateInProgressIconStyle(normalized);
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

    private void updateInProgressIconStyle(String normalizedStatus) {
        if (inProgressIcon == null) return;
        inProgressIcon.getStyleClass().removeAll("gray-icon", "orange-icon");
        if ("IN_PROGRESS".equals(normalizedStatus)) {
            inProgressIcon.getStyleClass().add("orange-icon");
        } else {
            inProgressIcon.getStyleClass().add("gray-icon");
        }
    }

    public void setOnEdit(Runnable onEdit) {
        this.onEdit = onEdit;
    }

    public void setOnDelete(Runnable onDelete) {
        this.onDelete = onDelete;
    }

    public void setOnStatusToggle(Function<Boolean, Boolean> onStatusToggle) {
        this.onStatusToggle = onStatusToggle;
    }

    public void setOnStatusChange(Function<String, Boolean> onStatusChange) {
        this.onStatusChange = onStatusChange;
    }

    @FXML
    private void onDoneToggle(ActionEvent event) {
        if (doneCheck == null) return;
        boolean selected = doneCheck.isSelected();
        String nextStatus = selected ? "DONE" : "TODO";

        if (onStatusToggle != null) {
            boolean updated = Boolean.TRUE.equals(onStatusToggle.apply(selected));
            if (!updated) {
                doneCheck.setSelected(!selected);
                return;
            }
        } else if (onStatusChange != null) {
            boolean updated = Boolean.TRUE.equals(onStatusChange.apply(nextStatus));
            if (!updated) {
                doneCheck.setSelected(!selected);
                return;
            }
        }

        currentStatus = nextStatus;
        updateStatusStyle(nextStatus);
    }

    @FXML
    private void onInProgressClick(MouseEvent event) {
        String normalized = normalizeStatus(currentStatus);
        if ("IN_PROGRESS".equals(normalized)) {
            return;
        }

        boolean updated = onStatusChange != null && Boolean.TRUE.equals(onStatusChange.apply("IN_PROGRESS"));
        if (!updated) {
            return;
        }

        currentStatus = "IN_PROGRESS";
        if (doneCheck != null) {
            doneCheck.setSelected(false);
        }
        updateStatusStyle("IN_PROGRESS");
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
