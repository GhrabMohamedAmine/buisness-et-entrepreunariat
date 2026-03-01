package controllers;

import Mains.MainFX;
import entities.ResourceAssignment;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import services.AssignmentService;

import java.sql.SQLException;
import java.sql.Date;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class PhysicalReturnsController {

    @FXML private Label infoLabel;

    @FXML private TableView<ResourceAssignment> table;

    @FXML private TableColumn<ResourceAssignment, String> clientCol;
    @FXML private TableColumn<ResourceAssignment, String> projectCol;
    @FXML private TableColumn<ResourceAssignment, String> resourceCol;
    @FXML private TableColumn<ResourceAssignment, Integer> qtyCol;
    @FXML private TableColumn<ResourceAssignment, Date> assignedCol;

    // These are "computed" columns (not from entity fields)
    @FXML private TableColumn<ResourceAssignment, String> endDateCol;
    @FXML private TableColumn<ResourceAssignment, String> lateDaysCol;

    @FXML private TableColumn<ResourceAssignment, Void> actionCol;

    private final AssignmentService service = new AssignmentService();

    @FXML
    public void initialize() {
        //clientCol.setCellValueFactory(new PropertyValueFactory<>("user_id"));
        projectCol.setCellValueFactory(new PropertyValueFactory<>("projectCode"));
        resourceCol.setCellValueFactory(new PropertyValueFactory<>("resourceName"));
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        assignedCol.setCellValueFactory(new PropertyValueFactory<>("assignmentDate"));

        // computed: project end date text
        endDateCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setText(null); return; }

                ResourceAssignment a = getTableView().getItems().get(getIndex());
                try {
                    Date end = service.getProjectEndDate(a.getProjectCode());
                    setText(end == null ? "-" : end.toString());
                } catch (SQLException e) {
                    setText("ERR");
                }
            }
        });

        // computed: late days
        lateDaysCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setText(null); return; }

                ResourceAssignment a = getTableView().getItems().get(getIndex());
                try {
                    Date end = service.getProjectEndDate(a.getProjectCode());
                    if (end == null) { setText("-"); return; }

                    long late = ChronoUnit.DAYS.between(end.toLocalDate(), LocalDate.now());
                    setText(late > 0 ? String.valueOf(late) : "0");

                } catch (SQLException e) {
                    setText("ERR");
                }
            }
        });

        setupActions();

        // Apply penalties safely (uses penalty_days_applied)
        try {
            service.applyLatePenaltiesForPhysicalOut();
        } catch (SQLException e) {
            System.out.println("[Penalty] " + e.getMessage());
        }

        load();
    }

    private void setupActions() {
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button backBtn = new Button("Mark Returned");
            private final HBox box = new HBox(8, backBtn);

            {
                backBtn.getStyleClass().add("secondary-btn");

                backBtn.setOnAction(e -> {
                    ResourceAssignment a = getTableView().getItems().get(getIndex());
                    handleReturned(a);
                });
            }


            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    private void handleReturned(ResourceAssignment a) {
        try {
            if (!confirm("Mark as returned?", "This will set RETURNED, set return_date, and restore stock.")) return;

            service.markReturned(a.getAssignmentId());
            load();

        } catch (SQLException ex) {
            showError(ex.getMessage());
        }
    }

    private void load() {
        try {
            List<ResourceAssignment> list = service.getPhysicalOut();
            ObservableList<ResourceAssignment> obs = FXCollections.observableArrayList(list);

            table.setItems(obs);

            infoLabel.setText("Showing " + obs.size() + " physical resources currently out.");

        } catch (SQLException e) {
            e.printStackTrace();
            showError(e.getMessage());
        }
    }

    @FXML
    private void handleRefresh() {
        try {
            service.applyLatePenaltiesForPhysicalOut();
        } catch (SQLException e) {
            System.out.println("[Penalty] " + e.getMessage());
        }
        load();
    }

    @FXML
    private void goBack() {
        SmAdminStackController.getInstance().loadPageMRS();
        //MainFX.loadPage("/back/manage-requests.fxml"); // change if you want another page
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