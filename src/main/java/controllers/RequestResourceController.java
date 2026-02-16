package controllers;

import entities.Resource;
import entities.ResourceAssignment;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import services.AssignmentService;
import services.ResourceService;

import java.sql.SQLException;
import java.util.List;

public class RequestResourceController {

    @FXML private TextField projectCodeField;
    @FXML private ComboBox<Resource> resourceCombo;
    @FXML private TextField quantityField;
    @FXML private Label costLabel;

    @FXML private Button submitBtn; // ✅ add fx:id to Send button in FXML

    private final ResourceService resourceService = new ResourceService();
    private final AssignmentService assignmentService = new AssignmentService();

    private String clientCode = "CL001";

    private ResourceAssignment editing = null; // ✅ if not null -> edit mode

    @FXML
    public void initialize() {
        loadResources();

        quantityField.textProperty().addListener((obs, oldV, newV) -> updateEstimatedCost());
        resourceCombo.valueProperty().addListener((obs, oldV, newV) -> updateEstimatedCost());
    }

    private void loadResources() {
        try {
            List<Resource> list = resourceService.getAll();
            resourceCombo.setItems(FXCollections.observableArrayList(list));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ✅ called from ClientResourcesController when edit icon is clicked
    public void setEditMode(ResourceAssignment a) {
        this.editing = a;

        projectCodeField.setText(a.getProjectCode());
        quantityField.setText(String.valueOf(a.getQuantity()));

        // select the resource in combo by resourceId
        if (resourceCombo.getItems() != null) {
            for (Resource r : resourceCombo.getItems()) {
                if (r.getId() == a.getResourceId()) {
                    resourceCombo.setValue(r);
                    break;
                }
            }
        }

        updateEstimatedCost();

        // change UI text
        if (submitBtn != null) submitBtn.setText("Save Changes");
    }

    private void updateEstimatedCost() {
        try {
            Resource r = resourceCombo.getValue();
            if (r == null) { costLabel.setText("0.00"); return; }

            int qty = Integer.parseInt(quantityField.getText().trim());
            double cost = qty * r.getUnitcost();
            costLabel.setText(String.format("%.2f", cost));

        } catch (Exception ex) {
            costLabel.setText("0.00");
        }
    }

    @FXML
    private void handleRequest() {

        Resource selected = resourceCombo.getValue();
        if (selected == null) {
            showError("Please choose a resource.");
            return;
        }

        String projectCode = projectCodeField.getText().trim();
        if (projectCode.isEmpty()) {
            showError("Project code is required.");
            return;
        }

        int qty;
        try {
            qty = Integer.parseInt(quantityField.getText().trim());
        } catch (Exception e) {
            showError("Quantity must be a number.");
            return;
        }

        if (qty <= 0) {
            showError("Quantity must be greater than 0.");
            return;
        }

        double totalCost = qty * selected.getUnitcost();

        try {
            if (editing == null) {
                // CREATE
                assignmentService.requestResource(selected.getId(), projectCode, clientCode, qty, totalCost);
            } else {
                // UPDATE (reset to pending)
                assignmentService.updateRequest(editing.getAssignmentId(), selected.getId(), projectCode, qty, totalCost);
            }

            closeWindow();

        } catch (SQLException e) {
            e.printStackTrace();
            showError("Database error: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) projectCodeField.getScene().getWindow();
        stage.close();
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setHeaderText("Request Failed");
        a.setContentText(msg);
        a.showAndWait();
    }

    public void setClientCode(String clientCode) {
        this.clientCode = clientCode;
    }
}
