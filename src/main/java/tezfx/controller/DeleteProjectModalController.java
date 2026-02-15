package tezfx.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class DeleteProjectModalController {

    @FXML
    private Label projectNameLabel;

    private boolean confirmed = false;

    public void setProjectName(String projectName) {
        projectNameLabel.setText(projectName == null || projectName.isBlank() ? "this project" : projectName);
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
        Stage stage = (Stage) projectNameLabel.getScene().getWindow();
        stage.close();
    }
}
