package controllers;

import entities.ResourceAssignment;
import entities.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import services.AssignmentService;
import services.UserService;

import org.kordamp.ikonli.javafx.FontIcon;

import java.nio.file.Files;
import java.nio.file.Paths;
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

    // ✅ session user id
    private int userId = 0;

    @FXML
    public void initialize() {
        User u = UserService.getCurrentUser();
        this.userId = (u == null) ? 0 : u.getId();

        System.out.println("DEBUG ClientResourcesController userId = " + userId);

        loadAndRender();
    }

    // ===================== OPEN POPUP (CREATE MODE) =====================
    @FXML
    private void openRequestPopup() {
        openRequestPopupInternal(null);
    }

    @FXML
    private void openRequestPage() {
        StackController.getInstance().loadPageRR();
    }

    // ===================== OPEN POPUP (EDIT MODE) =====================
    private void openRequestPopupInternal(ResourceAssignment assignmentToEdit) {
        try {
            // ✅ session check
            if (userId <= 0) {
                showErr("Session Error", "User session not found. Please login again.");
                return;
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/front/request-resource-popup.fxml"));
            Parent root = loader.load();

            RequestResourceController popupController = loader.getController();

            // ✅ DO NOT pass userId anymore; controller reads session itself.
            // popupController.setUserId(userId);

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
            showErr("UI Error", e.getMessage());
        }
    }

    // ===================== LOAD + RENDER =====================
    private void loadAndRender() {
        try {
            if (userId <= 0) return;

            // ✅ load by userId
            List<ResourceAssignment> list = assignmentService.getByUser(userId);

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
            showErr("DB Error", e.getMessage());
        }
    }

    private String normalizeStatus(String status) {
        return status == null ? "PENDING" : status.trim().toUpperCase();
    }

    // ===================== CARD =====================
    private VBox createCard(ResourceAssignment a, String statusText) {

        VBox card = new VBox(10);
        card.getStyleClass().add("resource-card");
        card.setPrefWidth(340);
        card.setPadding(new Insets(14));

        ImageView thumb = new ImageView();
        thumb.getStyleClass().add("thumb");
        thumb.setFitWidth(72);
        thumb.setFitHeight(72);
        thumb.setPreserveRatio(true);
        thumb.setSmooth(true);

        loadThumbImage(thumb, a.getResourceImagePath());

        Label name = new Label(a.getResourceName() != null ? a.getResourceName() : "Resource");
        name.getStyleClass().add("card-title");

        Label type = new Label(a.getResourceType() != null ? a.getResourceType() : "-");
        type.getStyleClass().add("badge");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox titleRow = new HBox(10, name, spacer, type);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label qty = new Label("Qty: " + a.getQuantity());
        qty.getStyleClass().add("card-line");

        Label date = new Label("Date: " + (a.getAssignmentDate() != null ? a.getAssignmentDate().toString() : "-"));
        date.getStyleClass().add("card-line");

        Label cost = new Label("Cost: " + String.format("%.2f", a.getTotalCost()));
        cost.getStyleClass().add("card-line");

        VBox infoBox = new VBox(6, titleRow, qty, date, cost);
        infoBox.getStyleClass().add("card-info");
        infoBox.setAlignment(Pos.TOP_LEFT);

        HBox top = new HBox(12, thumb, infoBox);
        top.setAlignment(Pos.TOP_LEFT);

        Label statusChip = new Label(statusText);
        statusChip.getStyleClass().addAll("status-chip", statusClass(statusText));

        HBox footer = new HBox(10);
        footer.getStyleClass().add("card-footer");
        footer.setAlignment(Pos.CENTER_RIGHT);

        if (isEditableStatus(statusText)) {

            Button editBtn = new Button();
            editBtn.getStyleClass().add("icon-btn");
            editBtn.setGraphic(new FontIcon("fas-pencil-alt"));
            editBtn.setTooltip(new Tooltip("Update request"));
            editBtn.setOnAction(e -> openRequestPopupInternal(a));

            Button deleteBtn = new Button();
            deleteBtn.getStyleClass().add("icon-btn");
            deleteBtn.setGraphic(new FontIcon("fas-trash"));
            deleteBtn.setTooltip(new Tooltip("Delete request"));
            deleteBtn.setOnAction(e -> confirmDelete(a));

            footer.getChildren().addAll(editBtn, deleteBtn);
        }

        footer.getChildren().add(statusChip);

        card.getChildren().addAll(top, footer);
        return card;
    }

    private void loadThumbImage(ImageView thumb, String imagePath) {
        try {
            if (imagePath == null || imagePath.isBlank()) {
                thumb.getStyleClass().add("thumb-empty");
                return;
            }
            if (!Files.exists(Paths.get(imagePath))) {
                thumb.getStyleClass().add("thumb-empty");
                return;
            }
            String uri = Paths.get(imagePath).toUri().toString();
            thumb.setImage(new Image(uri, true));
        } catch (Exception e) {
            thumb.getStyleClass().add("thumb-empty");
        }
    }

    private boolean isEditableStatus(String st) {
        if (st == null) return false;
        st = st.toUpperCase();
        return st.equals("PENDING") || st.equals("REQUESTED") || st.equals("DECLINED");
    }

    private void confirmDelete(ResourceAssignment a) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Request");
        alert.setHeaderText("Are you sure you want to delete this request?");
        alert.setContentText((a.getResourceName() != null ? a.getResourceName() : "Resource") +
                " (Qty: " + a.getQuantity() + ")");

        alert.showAndWait().ifPresent(res -> {
            if (res == ButtonType.OK) {
                try {
                    assignmentService.delete(a.getAssignmentId());
                    loadAndRender();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    showErr("Delete failed", ex.getMessage());
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

    private void showErr(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}