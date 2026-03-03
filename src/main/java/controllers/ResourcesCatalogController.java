package controllers;

import entities.Resource;
import entities.User;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
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
import services.ResourceService;
import services.UserService;
import services.ai.AIRecommendationService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.List;

public class ResourcesCatalogController {

    @FXML private FlowPane cardsFlow;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> typeFilter;

    private final ResourceService resourceService = new ResourceService();
    private final ObservableList<Resource> data = FXCollections.observableArrayList();
    private final AIRecommendationService aiService = new AIRecommendationService();

    private FilteredList<Resource> filtered;

    // ✅ session user id (from UserService.currentUser)
    private int userId = 0;

    // Optional: keep it if some other controller still calls it
    public void setUserId(int userId) {
        this.userId = userId;
    }

    @FXML
    public void initialize() {

        // ✅ ALWAYS read current user from UserService session first
        User u = UserService.getCurrentUser();
        this.userId = (u == null) ? 0 : u.getId();

        System.out.println("DEBUG ResourcesCatalogController userId = " + userId);

        if (typeFilter != null) {
            typeFilter.setItems(FXCollections.observableArrayList("ALL", "PHYSICAL", "SOFTWARE"));
            typeFilter.getSelectionModel().select("ALL");
        }

        filtered = new FilteredList<>(data, r -> true);

        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldV, newV) -> applyFilters());
        }
        if (typeFilter != null) {
            typeFilter.valueProperty().addListener((obs, oldV, newV) -> applyFilters());
        }

        loadResources();
    }

    private void loadResources() {
        data.clear();
        try {
            data.addAll(resourceService.getAll());
            applyFilters();
        } catch (SQLException e) {
            showError("DB Error", e.getMessage());
        }
    }

    private void applyFilters() {
        String q = (searchField == null || searchField.getText() == null)
                ? ""
                : searchField.getText().trim().toLowerCase();

        String selectedType = (typeFilter == null || typeFilter.getValue() == null)
                ? "ALL"
                : typeFilter.getValue().trim().toUpperCase();

        filtered.setPredicate(r -> {
            if (r == null) return false;

            String name = (r.getName() == null) ? "" : r.getName().toLowerCase();
            boolean matchName = q.isEmpty() || name.contains(q);

            String type = (r.getType() == null) ? "" : r.getType().trim().toUpperCase();
            boolean matchType = selectedType.equals("ALL") || type.equals(selectedType);

            return matchName && matchType;
        });

        renderCards(filtered);
    }

    private void renderCards(List<Resource> list) {
        cardsFlow.getChildren().clear();

        if (list == null || list.isEmpty()) {
            cardsFlow.getChildren().add(buildEmptyState());
            return;
        }

        for (Resource r : list) {
            cardsFlow.getChildren().add(createResourceCard(r));
        }
    }

    private VBox buildEmptyState() {
        VBox box = new VBox(6);
        box.getStyleClass().add("empty-state");
        box.setPadding(new Insets(18));
        box.setPrefWidth(420);
        box.setAlignment(Pos.CENTER_LEFT);

        Label t = new Label("No resources found");
        t.getStyleClass().add("empty-title");

        Label s = new Label("Try another name or change the type filter.");
        s.getStyleClass().add("empty-sub");

        box.getChildren().addAll(t, s);
        return box;
    }

    private VBox createResourceCard(Resource r) {
        VBox card = new VBox(10);
        card.getStyleClass().add("catalog-card");
        card.setPrefWidth(320);
        card.setPadding(new Insets(14));

        ImageView thumb = new ImageView();
        thumb.getStyleClass().add("catalog-thumb");
        thumb.setFitWidth(292);
        thumb.setFitHeight(140);
        thumb.setPreserveRatio(false);
        thumb.setSmooth(true);

        loadImageInto(thumb, r.getImagePath());

        Label name = new Label(r.getName() != null ? r.getName() : "Resource");
        name.getStyleClass().add("catalog-title");

        Label badge = new Label(r.getType() != null ? r.getType() : "-");
        badge.getStyleClass().add("catalog-badge");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(10, name, spacer, badge);
        header.setAlignment(Pos.CENTER_LEFT);

        GridPane info = new GridPane();
        info.getStyleClass().add("catalog-info");
        info.setHgap(14);
        info.setVgap(6);

        Label unit = new Label("Unit: " + String.format("%.2f", r.getUnitcost()));
        Label total = new Label("Total: " + r.getQuantity());
        Label av = new Label("Available: " + (int) r.getAvquant());
        av.getStyleClass().addAll("catalog-line", availabilityClass(r.getAvquant()));

        info.add(unit, 0, 0);
        info.add(total, 1, 0);
        info.add(av, 0, 1);

        Button requestBtn = new Button("Request");
        requestBtn.getStyleClass().add("catalog-request-btn");

        boolean disabled = r.getAvquant() <= 0;
        requestBtn.setDisable(disabled);
        if (disabled) requestBtn.setTooltip(new Tooltip("Out of stock"));

        requestBtn.setOnAction(e -> openRequestPopup(r));

        HBox footer = new HBox(requestBtn);
        footer.setAlignment(Pos.CENTER_RIGHT);

        card.getChildren().addAll(thumb, header, info, footer);
        return card;
    }

    private void loadImageInto(ImageView view, String path) {
        try {
            if (path == null || path.isBlank() || !Files.exists(Paths.get(path))) {
                view.getStyleClass().add("thumb-empty");
                view.setImage(null);
                return;
            }
            String uri = Paths.get(path).toUri().toString();
            view.setImage(new Image(uri, true));
        } catch (Exception e) {
            view.getStyleClass().add("thumb-empty");
            view.setImage(null);
        }
    }

    private void openRequestPopup(Resource selected) {
        try {
            // ✅ Ensure session exists
            if (userId <= 0) {
                showError("Session Error", "User session not found. Please login again.");
                return;
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/front/request-resource-popup.fxml"));
            Parent root = loader.load();

            RequestResourceController popupController = loader.getController();

            // ✅ Optional now, but safe
            popupController.setForcedResource(selected);

            Stage stage = new Stage();
            stage.setTitle("Request Resource");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.showAndWait();

            loadResources();

        } catch (IOException ex) {
            showError("UI Error", ex.getMessage());
        }
    }

    @FXML
    private void openAIRecommend() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/front/ai-chat.fxml"));
            Parent root = loader.load();

            AiChatController chat = loader.getController();

            chat.setListener((resourceId, qty) -> {
                Resource found = null;
                for (Resource r : data) {
                    if (r.getId() == resourceId) { found = r; break; }
                }
                if (found != null) {
                    Resource finalFound = found;
                    Platform.runLater(() -> {
                        data.remove(finalFound);
                        data.add(0, finalFound);
                        applyFilters();
                    });
                }
            });

            Stage stage = new Stage();
            stage.setTitle("AI Assistant");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goBack() {
        StackController.getInstance().loadPageCR();
    }

    private String availabilityClass(double av) {
        if (av <= 0) return "av-zero";
        if (av <= 5) return "av-low";
        return "av-ok";
    }

    private void showError(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}