package controllers;

import Mains.MainFX;
import entities.Resource;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
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

    @FXML private TextField searchField;

    private final ResourceService service = new ResourceService();

    // ✅ MASTER LIST (source of truth)
    private final ObservableList<Resource> masterData = FXCollections.observableArrayList();

    // ✅ Filter + Sort wrappers
    private FilteredList<Resource> filteredData;

    @FXML
    public void initialize() {

        // ===== Columns =====
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        availableCol.setCellValueFactory(new PropertyValueFactory<>("avquant"));
        totalCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        // STATUS computed from available quantity
        statusCol.setCellValueFactory(cellData -> {
            int av = cellData.getValue() != null ? (int) cellData.getValue().getAvquant() : 0;
            return new SimpleStringProperty(av > 0 ? "Available" : "Unavailable");
        });

        // Actions
        addActionButtonsWithIkonli();

        // ✅ Search + sorting wiring (must be before loading OR after, both ok)
        setupSearch();

        // Load everything
        refreshAll();
    }

    // ===================== NAVIGATION =====================
    @FXML
    private void goToManageRequests() {
        MainFX.loadPage("/back/manage-requests.fxml");
    }

    // ===================== REFRESH =====================
    private void refreshAll() {
        loadResources();
        loadStats();
    }

    // ✅ IMPORTANT: load into masterData (NOT resourceTable.setItems(newList))
    private void loadResources() {
        try {
            List<Resource> list = service.getAll();
            masterData.setAll(list);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ===================== SEARCH (BY NAME) =====================
    private void setupSearch() {

        filteredData = new FilteredList<>(masterData, r -> true);

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String keyword = (newVal == null) ? "" : newVal.trim().toLowerCase();

            filteredData.setPredicate(resource -> {
                if (keyword.isEmpty()) return true;
                if (resource == null) return false;

                String name = resource.getName() == null ? "" : resource.getName().toLowerCase();

                // ✅ Search by resource_name
                return name.contains(keyword);
            });
        });

        SortedList<Resource> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(resourceTable.comparatorProperty());

        resourceTable.setItems(sortedData);
    }

    // ===================== STATS =====================
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

    // ===================== ADD RESOURCE POPUP =====================
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

    // ===================== ACTION BUTTONS =====================
    private void addActionButtonsWithIkonli() {

        actionCol.setCellFactory(col -> new TableCell<>() {

            private final Button editBtn = new Button();
            private final Button deleteBtn = new Button();
            private final HBox pane = new HBox(12, editBtn, deleteBtn);

            {
                pane.setAlignment(Pos.CENTER);

                FontIcon editIcon = new FontIcon("fas-pencil-alt");
                FontIcon trashIcon = new FontIcon("fas-trash");

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

    // ===================== DELETE =====================
    private void handleDelete(Resource resource) {
        if (resource == null) return;

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

    // ===================== EDIT =====================
    private void handleEdit(Resource resource) {
        if (resource == null) return;

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
