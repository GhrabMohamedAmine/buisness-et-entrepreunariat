package tezfx.controller;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.event.ActionEvent;
import javafx.scene.input.MouseEvent;
import org.kordamp.ikonli.javafx.FontIcon;
import tezfx.model.Entities.Task;

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
    private String currentStatus = TaskValueMapper.STATUS_TODO;

    public void setTaskData(Task task) {
        if (task == null) return;

        taskTitle.setText(task.getTitle());
        taskDesc.setText(task.getDescription() != null ? task.getDescription() : "No description");
        dueDateLabel.setText(task.getDueDate() != null ? task.getDueDate() : "No date");

        // SET THE ASSIGNED USER NAME
        // Assuming your Task model has getAssignedTo() or getAssignedUser()
        if (assignedUser != null) {
            String assigneeName = task.getAssignedToName();
            assignedUser.setText(
                    assigneeName == null || assigneeName.isBlank() ? "Unassigned" : assigneeName
            );
        }

        String normalizedStatus = TaskValueMapper.normalizeStatus(task.getStatus());
        currentStatus = normalizedStatus;
        if (doneCheck != null) {
            doneCheck.setSelected(TaskValueMapper.STATUS_DONE.equals(normalizedStatus));
        }
        updateStatusStyle(normalizedStatus);
        updatePriorityStyle(task.getPriority());
    }

    private void updateStatusStyle(String status) {
        String normalized = TaskValueMapper.normalizeStatus(status);
        statusLabel.setText(TaskValueMapper.toStatusLabel(normalized));
        statusLabel.getStyleClass().removeAll("badge-todo", "badge-progress", "badge-done");
        statusLabel.getStyleClass().add(TaskValueMapper.statusBadgeStyleClass(normalized));
        updateInProgressIconStyle(normalized);
    }

    private void updatePriorityStyle(String priority) {
        String normalized = TaskValueMapper.normalizePriority(priority);
        priorityLabel.setText(normalized);
        priorityLabel.getStyleClass().removeAll("badge-priority-low", "badge-priority-medium", "badge-priority-high");
        priorityLabel.getStyleClass().add(TaskValueMapper.priorityBadgeStyleClass(normalized));
    }

    private void updateInProgressIconStyle(String normalizedStatus) {
        if (inProgressIcon == null) return;
        inProgressIcon.getStyleClass().removeAll("gray-icon", "orange-icon");
        if (TaskValueMapper.STATUS_IN_PROGRESS.equals(normalizedStatus)) {
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
        String nextStatus = selected ? TaskValueMapper.STATUS_DONE : TaskValueMapper.STATUS_TODO;

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
        String normalized = TaskValueMapper.normalizeStatus(currentStatus);
        if (TaskValueMapper.STATUS_IN_PROGRESS.equals(normalized)) {
            return;
        }

        boolean updated = onStatusChange != null && Boolean.TRUE.equals(onStatusChange.apply(TaskValueMapper.STATUS_IN_PROGRESS));
        if (!updated) {
            return;
        }

        currentStatus = TaskValueMapper.STATUS_IN_PROGRESS;
        if (doneCheck != null) {
            doneCheck.setSelected(false);
        }
        updateStatusStyle(TaskValueMapper.STATUS_IN_PROGRESS);
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
