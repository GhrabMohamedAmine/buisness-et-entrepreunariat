package controllers;

import com.example.testp1.model.ProjectDAO;
import entities.Resource;
import entities.ResourceAssignment;
import entities.User;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import services.AssignmentService;
import services.ResourceService;
import services.UserService;

import java.sql.SQLException;
import java.util.List;

public class RequestResourceController {

    @FXML private TextField projectCodeField;
    @FXML private ComboBox<Resource> resourceCombo;
    @FXML private TextField quantityField;
    @FXML private Label costLabel;
    @FXML private Button submitBtn;
    @FXML private ComboBox<String> ProjectCombo;

    private final ProjectDAO projectDAO = new ProjectDAO();
    private final ResourceService resourceService = new ResourceService();
    private final AssignmentService assignmentService = new AssignmentService();

    // ✅ session user id (from UserService.currentUser)
    private int userId;

    private ResourceAssignment editing = null;
    private Resource forcedResource = null;

    @FXML
    public void initialize() {

        // ✅ ALWAYS read the current user from UserService session
        User u = UserService.getCurrentUser();
        this.userId = (u == null) ? 0 : u.getId();

        System.out.println("DEBUG RequestResourceController userId = " + userId);

        if (userId <= 0) {
            showError("Session not found. Please login again.");
            // Disable submit to avoid FK errors
            if (submitBtn != null) submitBtn.setDisable(true);
            return;
        }

        loadResources();

        ProjectCombo.setItems(
                FXCollections.observableArrayList(projectDAO.getAvailableProjectNames())
        );

        quantityField.textProperty().addListener((obs, oldV, newV) -> updateEstimatedCost());
        resourceCombo.valueProperty().addListener((obs, oldV, newV) -> updateEstimatedCost());
        ProjectCombo.valueProperty().addListener((obs, oldV, newV) -> updateEstimatedCost());
    }

    private void loadResources() {
        try {
            List<Resource> list = resourceService.getAll();
            resourceCombo.setItems(FXCollections.observableArrayList(list));

            if (forcedResource != null) {
                applyForcedResource();
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showError("Database error: " + e.getMessage());
        }
    }

    // ================= FORCED RESOURCE (FROM CATALOG) =================
    public void setForcedResource(Resource r) {
        this.forcedResource = r;

        if (resourceCombo != null) {
            applyForcedResource();
        }

        updateEstimatedCost();
    }

    private void applyForcedResource() {
        resourceCombo.getItems().setAll(forcedResource);
        resourceCombo.getSelectionModel().select(forcedResource);
        resourceCombo.setDisable(true);
    }

    // ================= EDIT MODE =================
    public void setEditMode(ResourceAssignment a) {
        this.editing = a;

        ProjectCombo.setValue(projectDAO.getNameById(Integer.parseInt(a.getProjectCode())));
        quantityField.setText(String.valueOf(a.getQuantity()));

        forcedResource = null;
        resourceCombo.setDisable(false);

        for (Resource r : resourceCombo.getItems()) {
            if (r.getId() == a.getResourceId()) {
                resourceCombo.setValue(r);
                break;
            }
        }

        updateEstimatedCost();

        if (submitBtn != null) {
            submitBtn.setText("Save Changes");
        }
    }

    // ================= COST =================
    private void updateEstimatedCost() {
        try {
            Resource r = (forcedResource != null) ? forcedResource : resourceCombo.getValue();
            if (r == null) {
                costLabel.setText("0.00");
                return;
            }

            int qty = Integer.parseInt(quantityField.getText().trim());
            double cost = qty * r.getUnitcost();
            costLabel.setText(String.format("%.2f", cost));

        } catch (Exception ex) {
            costLabel.setText("0.00");
        }
    }

    // ================= SUBMIT =================
    @FXML
    private void handleRequest() {

        if (userId <= 0) {
            showError("Session not found. Please login again.");
            return;
        }

        Resource selected = (forcedResource != null) ? forcedResource : resourceCombo.getValue();
        if (selected == null) {
            showError("Please choose a resource.");
            return;
        }

        if (ProjectCombo.getValue() == null) {
            showError("Please choose a project.");
            return;
        }

        String projectName = ProjectCombo.getValue();
        String projectCode = String.valueOf(projectDAO.getIdByName(projectName));

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

        if (qty > selected.getAvquant()) {
            showError("Not enough available quantity. Available: " + (int) selected.getAvquant());
            return;
        }

        double totalCost = qty * selected.getUnitcost();

        try {
            if (editing == null) {
                assignmentService.requestResource(
                        selected.getId(),
                        projectCode,
                        userId,
                        qty,
                        totalCost
                );
            } else {
                assignmentService.updateRequest(
                        editing.getAssignmentId(),
                        selected.getId(),
                        projectCode,
                        qty,
                        totalCost
                );
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
        Stage stage = (Stage) ProjectCombo.getScene().getWindow();
        stage.close();
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setHeaderText("Request Failed");
        a.setContentText(msg);
        a.showAndWait();
    }
}