package tezfx.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class DeleteTaskModalController {

    @FXML
    private Label taskNameLabel;

    private boolean confirmed = false;

    public void setTaskName(String taskName) {
        taskNameLabel.setText(taskName == null || taskName.isBlank() ? "this task" : taskName);
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    @FXML
    private void onCancel() {
        close();
    }

    @FXML
    private void onConfirmDelete() {
        confirmed = true;
        close();
    }

    private void close() {
        Stage stage = (Stage) taskNameLabel.getScene().getWindow();
        stage.close();
    }
}
