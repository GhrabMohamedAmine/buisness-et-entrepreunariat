package tezfx.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class DeleteConfirmModalController {
    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;
    @FXML private Label itemNameLabel;
    @FXML private Label consequenceLabel;
    @FXML private Button confirmButton;

    private boolean confirmed = false;

    public void configure(String entityName, String itemName, String consequenceText, String confirmText) {
        String safeEntity = (entityName == null || entityName.isBlank()) ? "Item" : entityName.trim();
        String safeItem = (itemName == null || itemName.isBlank()) ? ("this " + safeEntity.toLowerCase()) : itemName;

        titleLabel.setText("Delete " + safeEntity + "?");
        subtitleLabel.setText("This action cannot be undone.");
        itemNameLabel.setText(safeItem);
        consequenceLabel.setText(consequenceText == null ? "" : consequenceText);
        confirmButton.setText(confirmText == null || confirmText.isBlank() ? "Delete" : confirmText);
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
        Stage stage = (Stage) titleLabel.getScene().getWindow();
        stage.close();
    }
}
