package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.stage.Stage;

public class TasksFilterModalController {
    @FXML private ComboBox<String> statusCombo;
    @FXML private ComboBox<String> priorityCombo;

    private boolean applied;
    private String selectedStatusFilter = TaskValueMapper.FILTER_ALL;
    private String selectedPriorityFilter = TaskValueMapper.FILTER_ALL;

    @FXML
    public void initialize() {
        statusCombo.getItems().setAll(
                "All",
                TaskValueMapper.toStatusLabel(TaskValueMapper.STATUS_TODO),
                TaskValueMapper.toStatusLabel(TaskValueMapper.STATUS_IN_PROGRESS),
                TaskValueMapper.toStatusLabel(TaskValueMapper.STATUS_DONE)
        );
        priorityCombo.getItems().setAll("All", "Low", "Medium", "High");
        statusCombo.setValue("All");
        priorityCombo.setValue("All");
    }

    public void setInitialFilters(String statusFilter, String priorityFilter) {
        selectedStatusFilter = (statusFilter == null || statusFilter.isBlank()) ? TaskValueMapper.FILTER_ALL : statusFilter;
        selectedPriorityFilter = (priorityFilter == null || priorityFilter.isBlank()) ? TaskValueMapper.FILTER_ALL : priorityFilter;
        statusCombo.setValue(TaskValueMapper.toStatusFilterLabel(selectedStatusFilter));
        priorityCombo.setValue(TaskValueMapper.toPriorityFilterLabel(selectedPriorityFilter));
    }

    public boolean isApplied() {
        return applied;
    }

    public String getSelectedStatusFilter() {
        return selectedStatusFilter;
    }

    public String getSelectedPriorityFilter() {
        return selectedPriorityFilter;
    }

    @FXML
    private void onApply() {
        applied = true;
        selectedStatusFilter = TaskValueMapper.fromStatusFilterLabel(statusCombo.getValue());
        selectedPriorityFilter = TaskValueMapper.fromPriorityFilterLabel(priorityCombo.getValue());
        close();
    }

    @FXML
    private void onClear() {
        applied = true;
        selectedStatusFilter = TaskValueMapper.FILTER_ALL;
        selectedPriorityFilter = TaskValueMapper.FILTER_ALL;
        close();
    }

    @FXML
    private void onCancel() {
        close();
    }

    private void close() {
        Stage stage = (Stage) statusCombo.getScene().getWindow();
        stage.close();
    }
}
