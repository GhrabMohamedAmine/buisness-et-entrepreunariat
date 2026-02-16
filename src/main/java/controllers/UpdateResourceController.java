package controllers;

import entities.Resource;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import services.ResourceService;

public class UpdateResourceController {

    @FXML private TextField codeField;
    @FXML private TextField nameField;
    @FXML private ComboBox<String> typeCombo;
    @FXML private TextField unitCostField;
    @FXML private TextField totalQtyField;
    @FXML private TextField availableQtyField;
    @FXML private ComboBox<String> statusCombo;

    private final ResourceService service = new ResourceService();
    private Resource resource; // the selected row

    @FXML
    public void initialize() {
        typeCombo.getItems().addAll("PHYSICAL", "SOFTWARE");
        statusCombo.getItems().addAll("AVAILABLE", "IN_USE", "MAINTENANCE", "UNAVAILABLE");
    }

    // called from ManageResourcesController after loading the FXML
    public void setResource(Resource r) {
        this.resource = r;

        // Pre-fill fields
        codeField.setText(String.valueOf(r.getCode()));
        nameField.setText(r.getName());
        typeCombo.setValue(r.getType());
        unitCostField.setText(String.valueOf(r.getUnitcost()));
        totalQtyField.setText(String.valueOf(r.getQuantity()));
        availableQtyField.setText(String.valueOf(r.getAvquant()));

        // If you don't store status in your Resource entity, choose default:
        statusCombo.setValue("AVAILABLE");
    }

    @FXML
    private void handleSave() {
        try {
            if (resource == null) return;

            // Update resource object
            resource.setCode(Integer.parseInt(codeField.getText()));
            resource.setName(nameField.getText());
            resource.setType(typeCombo.getValue());
            resource.setUnitcost(Double.parseDouble(unitCostField.getText()));
            resource.setQuantity(Integer.parseInt(totalQtyField.getText()));
            resource.setAvquant(Double.parseDouble(availableQtyField.getText()));

            // If you have status in DB but not in entity, you can ignore or add later

            service.update(resource);

            closeWindow();

        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText("Update failed");
            alert.setContentText("Check your inputs.\n" + e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    private void handleCancel() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) codeField.getScene().getWindow();
        stage.close();
    }
}
