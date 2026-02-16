package controllers;

import entities.ResourceAssignment;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import services.AssignmentService;

import org.kordamp.ikonli.javafx.FontIcon;

import java.sql.SQLException;
import java.util.List;

public class ClientResourcesController {

    @FXML private FlowPane requestedFlow;
    @FXML private FlowPane acceptedFlow;
    @FXML private FlowPane declinedFlow;

    @FXML private Label requestedCountLabel;
    @FXML private Label acceptedCountLabel;
    @FXML private Label declinedCountLabel;

    private final AssignmentService assignmentService = new AssignmentService();

    // TEMP client code (replace later by session)
    private String clientCode = "CL001";

    @FXML
    public void initialize() {
        loadAndRender();
    }

    // ===================== OPEN POPUP (CREATE MODE) =====================
    @FXML
    private void openRequestPopup() {
        openRequestPopupInternal(null);
    }

    // ===================== OPEN POPUP (EDIT MODE) =====================
    private void openRequestPopupInternal(ResourceAssignment assignmentToEdit) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/front/request-resource-popup.fxml"));
            Parent root = loader.load();

            RequestResourceController popupController = loader.getController();
            popupController.setClientCode(clientCode);

            // Edit mode (must exist in RequestResourceController)
            if (assignmentToEdit != null) {
                popupController.setEditMode(assignmentToEdit);
            }

            Stage stage = new Stage();
            stage.setTitle(assignmentToEdit == null ? "Request Resource" : "Update Request");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.showAndWait();

            loadAndRender();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ===================== LOAD + RENDER =====================
    private void loadAndRender() {
        try {
            List<ResourceAssignment> list = assignmentService.getByClient(clientCode);

            requestedFlow.getChildren().clear();
            acceptedFlow.getChildren().clear();
            declinedFlow.getChildren().clear();

            int req = 0, acc = 0, dec = 0;

            for (ResourceAssignment a : list) {
                String st = normalizeStatus(a.getStatus());

                VBox card = createCard(a, st);

                if (st.equals("PENDING") || st.equals("REQUESTED")) {
                    requestedFlow.getChildren().add(card);
                    req++;
                } else if (st.equals("ACCEPTED") || st.equals("ACTIVE")) {
                    acceptedFlow.getChildren().add(card);
                    acc++;
                } else if (st.equals("DECLINED")) {
                    declinedFlow.getChildren().add(card);
                    dec++;
                } else {
                    requestedFlow.getChildren().add(card);
                    req++;
                }
            }

            requestedCountLabel.setText(String.valueOf(req));
            acceptedCountLabel.setText(String.valueOf(acc));
            declinedCountLabel.setText(String.valueOf(dec));

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String normalizeStatus(String status) {
        return status == null ? "PENDING" : status.trim().toUpperCase();
    }

    // ===================== CARD =====================
    private VBox createCard(ResourceAssignment a, String statusText) {

        VBox card = new VBox(10);
        card.getStyleClass().add("resource-card");
        card.setPrefWidth(320);
        card.setPadding(new Insets(14));

        // Header: Name + Type badge
        Label name = new Label(a.getResourceName() != null ? a.getResourceName() : "Resource");
        name.getStyleClass().add("card-title");

        Label type = new Label(a.getResourceType() != null ? a.getResourceType() : "-");
        type.getStyleClass().add("badge");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(10, name, spacer, type);
        header.setAlignment(Pos.CENTER_LEFT);

        // Details
        Label qty = new Label("Qty: " + a.getQuantity());
        qty.getStyleClass().add("card-line");

        Label date = new Label("Date: " + (a.getAssignmentDate() != null ? a.getAssignmentDate().toString() : "-"));
        date.getStyleClass().add("card-line");

        Label cost = new Label("Cost: " + String.format("%.2f", a.getTotalCost()));
        cost.getStyleClass().add("card-line");

        // Status chip
        Label statusChip = new Label(statusText);
        statusChip.getStyleClass().addAll("status-chip", statusClass(statusText));

        // Footer: icons (only pending/requested/declined) + status
        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_RIGHT);

        if (isEditableStatus(statusText)) {

            Button editBtn = new Button();
            editBtn.getStyleClass().add("icon-btn");
            editBtn.setGraphic(new FontIcon("fas-pencil-alt")); // ✅ good FA5 literal
            editBtn.setTooltip(new Tooltip("Update request"));
            editBtn.setOnAction(e -> openRequestPopupInternal(a));

            Button deleteBtn = new Button();
            deleteBtn.getStyleClass().add("icon-btn");
            deleteBtn.setGraphic(new FontIcon("fas-trash")); // ✅ good FA5 literal
            deleteBtn.setTooltip(new Tooltip("Delete request"));
            deleteBtn.setOnAction(e -> confirmDelete(a));

            footer.getChildren().addAll(editBtn, deleteBtn);
        }

        footer.getChildren().add(statusChip);

        card.getChildren().addAll(header, qty, date, cost, footer);
        return card;
    }

    private boolean isEditableStatus(String st) {
        if (st == null) return false;
        st = st.toUpperCase();
        return st.equals("PENDING") || st.equals("REQUESTED") || st.equals("DECLINED");
    }

    // ===================== DELETE =====================
    private void confirmDelete(ResourceAssignment a) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Request");
        alert.setHeaderText("Are you sure you want to delete this request?");
        alert.setContentText((a.getResourceName() != null ? a.getResourceName() : "Resource") +
                " (Qty: " + a.getQuantity() + ")");

        alert.showAndWait().ifPresent(res -> {
            if (res == ButtonType.OK) {
                try {
                    assignmentService.delete(a.getAssignmentId()); // must exist
                    loadAndRender();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    Alert err = new Alert(Alert.AlertType.ERROR);
                    err.setHeaderText("Delete failed");
                    err.setContentText(ex.getMessage());
                    err.showAndWait();
                }
            }
        });
    }

    private String statusClass(String st) {
        if (st == null) return "status-pending";
        st = st.toUpperCase();

        if (st.equals("PENDING") || st.equals("REQUESTED")) return "status-pending";
        if (st.equals("ACCEPTED") || st.equals("ACTIVE")) return "status-accepted";
        if (st.equals("DECLINED")) return "status-declined";

        return "status-pending";
    }

    public void setClientCode(String clientCode) {
        this.clientCode = clientCode;
        loadAndRender();
    }
}
