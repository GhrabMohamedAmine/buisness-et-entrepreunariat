package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.event.ActionEvent;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import org.kordamp.ikonli.javafx.FontIcon;
import entities.Task;

import java.util.function.Function;

public class TaskRowController {
    // Add assignedUser here
    @FXML private Label taskTitle, taskDesc, priorityLabel, statusLabel, dueDateLabel, assignedUser;
    @FXML private CheckBox doneCheck;
    @FXML private FontIcon inProgressIcon, editIcon, deleteIcon;
    @FXML private HBox taskRowRoot;
    private Runnable onEdit;
    private Runnable onDelete;
    private Function<Boolean, Boolean> onStatusToggle;
    private Function<String, Boolean> onStatusChange;
    private String currentStatus = TaskValueMapper.STATUS_TODO;
    private boolean kanbanMode = false;

    public void setTaskData(Task task) {
        if (task == null) return;

        if (taskRowRoot != null) {
            taskRowRoot.setOnDragDetected(event -> {
                Dragboard db = taskRowRoot.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
                content.putString(String.valueOf(task.getId()));
            db.setContent(content);
                db.setDragView(taskRowRoot.snapshot(null, null));
                event.consume();
            });
        }



        taskTitle.setText(task.getTitle());
        taskDesc.setText(task.getDescription() != null ? task.getDescription() : "No description");
        dueDateLabel.setText(task.getDueDate() != null ? task.getDueDate() : "No date");


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
        applyMode();

    }

    public void setKanbanMode(boolean enabled) {
        this.kanbanMode = enabled;
        applyMode();
    }

    private void applyMode() {
        if (taskRowRoot == null) return;
        if (kanbanMode) {
            taskRowRoot.setSpacing(10);
            setNodeVisibleManaged(taskDesc, false);
            setNodeVisibleManaged(assignedUser, false);
            setNodeVisibleManaged(dueDateLabel, false);
            setNodeVisibleManaged(editIcon, false);
            setNodeVisibleManaged(deleteIcon, false);
            setNodeVisibleManaged(inProgressIcon, false);
        } else {
            taskRowRoot.setSpacing(20);
            setNodeVisibleManaged(taskDesc, true);
            setNodeVisibleManaged(assignedUser, true);
            setNodeVisibleManaged(dueDateLabel, true);
            setNodeVisibleManaged(editIcon, true);
            setNodeVisibleManaged(deleteIcon, true);
            setNodeVisibleManaged(inProgressIcon, true);
        }
    }

    private void setNodeVisibleManaged(javafx.scene.Node node, boolean value) {
        if (node == null) return;
        node.setVisible(value);
        node.setManaged(value);
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
