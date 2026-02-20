package controllers;

import entities.Resource;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import services.ResourceService;
import services.ai.AIRecommendationService;
import services.ai.AIRecommendationService.Recommendation;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class ResourcesCatalogController {

    @FXML private TableView<Resource> resourcesTable;

    @FXML private TableColumn<Resource, Integer> codeCol;
    @FXML private TableColumn<Resource, String> nameCol;
    @FXML private TableColumn<Resource, String> typeCol;
    @FXML private TableColumn<Resource, Double> unitCostCol;
    @FXML private TableColumn<Resource, Integer> totalQtyCol;
    @FXML private TableColumn<Resource, Double> avQtyCol;
    @FXML private TableColumn<Resource, Void> actionCol;

    private final ResourceService resourceService = new ResourceService();
    private final ObservableList<Resource> data = FXCollections.observableArrayList();
    private final AIRecommendationService aiService = new AIRecommendationService();
    // Same as your ClientResourcesController
    private String clientCode = "CL001";

    public void setClientCode(String clientCode) {
        this.clientCode = clientCode;
    }

    @FXML
    public void initialize() {
        codeCol.setCellValueFactory(new PropertyValueFactory<>("code"));
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        unitCostCol.setCellValueFactory(new PropertyValueFactory<>("unitcost"));
        totalQtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        avQtyCol.setCellValueFactory(new PropertyValueFactory<>("avquant"));

        setupActionButtons();
        loadResources();
    }
    @FXML
    private void openAIRecommend() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("AI Recommendation");
        dialog.setHeaderText("Describe your project");
        dialog.setContentText("Project description:");

        dialog.showAndWait().ifPresent(desc -> {
            try {
                List<Recommendation> recs = aiService.recommend(desc, 5);

                if (recs.isEmpty()) {
                    new Alert(Alert.AlertType.INFORMATION, "No recommendations found.").showAndWait();
                    return;
                }

                // highlight or auto-sort table
                // easiest: show in alert list
                StringBuilder sb = new StringBuilder();
                for (Recommendation r : recs) {
                    sb.append("• ")
                            .append(r.resource().getName())
                            .append(" | suggested: ").append(r.suggestedQty())
                            .append(" | score: ").append(r.score())
                            .append("\n");
                }

                Alert a = new Alert(Alert.AlertType.INFORMATION);
                a.setTitle("Top Recommendations");
                a.setHeaderText("AI Suggestions");
                a.setContentText(sb.toString());
                a.showAndWait();

            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    private void loadResources() {
        data.clear();
        try {
            data.addAll(resourceService.getAll());
            resourcesTable.setItems(data);
        } catch (SQLException e) {
            showError("DB Error", e.getMessage());
        }
    }

    private void setupActionButtons() {
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("Request");

            {
                btn.setStyle("-fx-background-radius: 10; -fx-padding: 6 14;");
                btn.setOnAction(e -> {
                    Resource r = getTableView().getItems().get(getIndex());
                    openRequestPopup(r);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setGraphic(null);
                    return;
                }

                Resource r = getTableView().getItems().get(getIndex());
                boolean disabled = r.getAvquant() <= 0;
                btn.setDisable(disabled);
                setGraphic(btn);
            }
        });
    }

    private void openRequestPopup(Resource selected) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/front/request-resource-popup.fxml"));
            Parent root = loader.load();

            RequestResourceController popupController = loader.getController();
            popupController.setClientCode(clientCode);

            // ✅ NEW: pass selected resource so user doesn't choose it
            popupController.setForcedResource(selected);            Stage stage = new Stage();
            stage.setTitle("Request Resource");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.showAndWait();

            // refresh after closing popup (optional)
            loadResources();

        } catch (IOException ex) {
            showError("UI Error", ex.getMessage());
        }
    }

    @FXML
    private void goBack() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/front/client-resources.fxml"));
            Stage stage = (Stage) resourcesTable.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showError(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}