package controllers;

import entities.ResourceAssignment;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import services.AdminRequestService;
import Mains.MainFX;

import java.sql.SQLException;
import java.util.List;

public class ManageRequestsController {

    @FXML private TableView<ResourceAssignment> requestsTable;

    @FXML private TableColumn<ResourceAssignment, String> clientCol;
    @FXML private TableColumn<ResourceAssignment, String> projectCol;
    @FXML private TableColumn<ResourceAssignment, String> resourceCol;
    @FXML private TableColumn<ResourceAssignment, String> typeCol;
    @FXML private TableColumn<ResourceAssignment, Integer> qtyCol;
    @FXML private TableColumn<ResourceAssignment, java.sql.Date> dateCol;
    @FXML private TableColumn<ResourceAssignment, Double> costCol;
    @FXML private TableColumn<ResourceAssignment, Void> actionCol;
    @FXML
    private void goBack() {
        MainFX.loadPage("/back/manage-resources.fxml");
    }

    private final AdminRequestService service = new AdminRequestService();

    @FXML
    public void initialize() {
        clientCol.setCellValueFactory(new PropertyValueFactory<>("clientCode"));
        projectCol.setCellValueFactory(new PropertyValueFactory<>("projectCode"));
        resourceCol.setCellValueFactory(new PropertyValueFactory<>("resourceName"));
        typeCol.setCellValueFactory(new PropertyValueFactory<>("resourceType"));
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        dateCol.setCellValueFactory(new PropertyValueFactory<>("assignmentDate"));
        costCol.setCellValueFactory(new PropertyValueFactory<>("totalCost"));

        setupActionButtons();
        loadPending();
    }

    private void setupActionButtons() {
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button acceptBtn = new Button("Accept");
            private final Button declineBtn = new Button("Decline");
            private final HBox box = new HBox(8, acceptBtn, declineBtn);

            {
                acceptBtn.getStyleClass().add("accept-btn");
                declineBtn.getStyleClass().add("decline-btn");

                acceptBtn.setOnAction(e -> {
                    ResourceAssignment a = getTableView().getItems().get(getIndex());
                    handleAccept(a);
                });

                declineBtn.setOnAction(e -> {
                    ResourceAssignment a = getTableView().getItems().get(getIndex());
                    handleDecline(a);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    private void handleAccept(ResourceAssignment a) {
        try {
            // Optional confirmation
            if (!confirm("Accept request?", "This will reduce available quantity.")) return;

            service.acceptRequest(a.getAssignmentId());
            loadPending();

        } catch (SQLException ex) {
            showError(ex.getMessage());
        }
    }

    private void handleDecline(ResourceAssignment a) {
        try {
            if (!confirm("Decline request?", "The request will be marked as DECLINED.")) return;

            service.declineRequest(a.getAssignmentId());
            loadPending();

        } catch (SQLException ex) {
            showError(ex.getMessage());
        }
    }

    @FXML
    private void handleRefresh() {
        loadPending();
    }

    private void loadPending() {
        try {
            List<ResourceAssignment> list = service.getPendingRequests();
            ObservableList<ResourceAssignment> obs = FXCollections.observableArrayList(list);
            requestsTable.setItems(obs);
        } catch (SQLException e) {
            e.printStackTrace();
            showError(e.getMessage());
        }
    }

    private boolean confirm(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle(title);
        a.setHeaderText(title);
        a.setContentText(msg);
        return a.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setHeaderText("Error");
        a.setContentText(msg);
        a.showAndWait();
    }
}
