package controllers;

import Mains.MainFX;
import entities.Resource;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;
import services.ResourceService;

import java.sql.SQLException;
import java.util.List;

public class ManageResourcesController {

    @FXML private TableView<Resource> resourceTable;

    @FXML private TableColumn<Resource, String> nameCol;
    @FXML private TableColumn<Resource, String> typeCol;
    @FXML private TableColumn<Resource, String> statusCol;
    @FXML private TableColumn<Resource, Integer> availableCol;
    @FXML private TableColumn<Resource, Integer> totalCol;
    @FXML private TableColumn<Resource, Void> actionCol;

    @FXML private Label totalResourcesLabel;
    @FXML private Label hardwareLabel;
    @FXML private Label softwareLabel;
    @FXML private Label maintenanceLabel;

    private final ResourceService service = new ResourceService();

    @FXML
    private void goToManageRequests() {
        MainFX.loadPage("/back/manage-requests.fxml");
    }

    @FXML
    public void initialize() {

        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        availableCol.setCellValueFactory(new PropertyValueFactory<>("avquant"));
        totalCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        statusCol.setCellValueFactory(cellData -> {
            if (cellData.getValue().getAvquant() > 0) {
                return new SimpleStringProperty("Available");
            } else {
                return new SimpleStringProperty("Unavailable");
            }
        });

        addActionButtonsWithIkonli();

        refreshAll();
    }

    private void refreshAll() {
        loadResources();
        loadStats();
    }

    private void loadResources() {
        try {
            List<Resource> list = service.getAll();
            ObservableList<Resource> obsList = FXCollections.observableArrayList(list);
            resourceTable.setItems(obsList);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadStats() {
        try {
            int total = service.countAll();
            int hardware = service.countByType("PHYSICAL");
            int software = service.countByType("SOFTWARE");
            int maintenance = service.countMaintenance();

            if (totalResourcesLabel != null) totalResourcesLabel.setText(String.valueOf(total));
            if (hardwareLabel != null) hardwareLabel.setText(String.valueOf(hardware));
            if (softwareLabel != null) softwareLabel.setText(String.valueOf(software));
            if (maintenanceLabel != null) maintenanceLabel.setText(String.valueOf(maintenance));

        } catch (SQLException e) {
            e.printStackTrace();
            if (totalResourcesLabel != null) totalResourcesLabel.setText("0");
            if (hardwareLabel != null) hardwareLabel.setText("0");
            if (softwareLabel != null) softwareLabel.setText("0");
            if (maintenanceLabel != null) maintenanceLabel.setText("0");
        }
    }

    @FXML
    private void handleAddResource() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/back/add-resource-popup.fxml"));
            Parent root = loader.load();

            Stage popupStage = new Stage();
            popupStage.setTitle("Add Resource");
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.setScene(new Scene(root));
            popupStage.setResizable(false);
            popupStage.showAndWait();

            refreshAll();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ✅ IKONLI ACTION BUTTONS
    private void addActionButtonsWithIkonli() {

        actionCol.setCellFactory(col -> new TableCell<>() {

            private final Button editBtn = new Button();
            private final Button deleteBtn = new Button();
            private final HBox pane = new HBox(12, editBtn, deleteBtn);

            {
                pane.setAlignment(Pos.CENTER);

                // ✅ Choose icon literals supported by your pack
                // FontAwesome5 pack examples:
                FontIcon editIcon = new FontIcon("fas-pencil-alt");
                FontIcon trashIcon = new FontIcon("fas-trash");

                // size + color
                editIcon.setIconSize(16);
                trashIcon.setIconSize(16);

                editBtn.setGraphic(editIcon);
                deleteBtn.setGraphic(trashIcon);

                editBtn.getStyleClass().add("icon-btn");
                deleteBtn.getStyleClass().add("icon-btn");

                editBtn.setTooltip(new Tooltip("Update"));
                deleteBtn.setTooltip(new Tooltip("Delete"));

                deleteBtn.setOnAction(event -> {
                    Resource resource = getTableView().getItems().get(getIndex());
                    handleDelete(resource);
                });

                editBtn.setOnAction(event -> {
                    Resource resource = getTableView().getItems().get(getIndex());
                    handleEdit(resource);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
    }

    private void handleDelete(Resource resource) {

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Resource");
        alert.setHeaderText("Are you sure you want to delete this resource?");
        alert.setContentText(resource.getName());

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    service.delete(resource.getId());
                    refreshAll();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void handleEdit(Resource resource) {

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/back/update-resource-popup.fxml"));
            Parent root = loader.load();

            UpdateResourceController controller = loader.getController();
            controller.setResource(resource);

            Stage stage = new Stage();
            stage.setTitle("Update Resource");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.showAndWait();

            refreshAll();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
