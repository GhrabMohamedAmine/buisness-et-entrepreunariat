package controllers;

import entities.Resource;
import entities.ResourceAssignment;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import services.AssignmentService;
import services.ResourceService;
import services.ai.OpenAiChatService;
import services.ai.ResourceContextBuilder;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RequestResourceController {

    @FXML private TextField projectCodeField;
    @FXML private ComboBox<Resource> resourceCombo;
    @FXML private TextField quantityField;
    @FXML private Label costLabel;
    @FXML private Button submitBtn;

    // ===== Chat UI =====
    @FXML private VBox chatBox;
    @FXML private TextField chatInput;
    @FXML private Label aiHintLabel;

    private final ResourceService resourceService = new ResourceService();
    private final AssignmentService assignmentService = new AssignmentService();

    // Real AI
    private final OpenAiChatService openAi = new OpenAiChatService();
    private final List<OpenAiChatService.ChatMessage> conversation = new ArrayList<>();

    private String clientCode = "CL001";
    private ResourceAssignment editing = null;

    private Resource forcedResource = null;
    private List<Resource> cachedResources = new ArrayList<>();

    @FXML
    public void initialize() {
        loadResources();

        quantityField.textProperty().addListener((obs, oldV, newV) -> updateEstimatedCost());
        resourceCombo.valueProperty().addListener((obs, oldV, newV) -> updateEstimatedCost());

        // Welcome message
        addAiBubble("Hi! Describe your project and what resources you need (type, quantity, duration, budget).");
    }

    private void loadResources() {
        try {
            cachedResources = resourceService.getAll();
            resourceCombo.setItems(FXCollections.observableArrayList(cachedResources));

            if (forcedResource != null) {
                applyForcedResource();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

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

    public void setEditMode(ResourceAssignment a) {
        this.editing = a;

        projectCodeField.setText(a.getProjectCode());
        quantityField.setText(String.valueOf(a.getQuantity()));

        forcedResource = null;
        if (resourceCombo != null) {
            resourceCombo.setDisable(false);
        }

        if (resourceCombo.getItems() != null) {
            for (Resource r : resourceCombo.getItems()) {
                if (r.getId() == a.getResourceId()) {
                    resourceCombo.setValue(r);
                    break;
                }
            }
        }

        updateEstimatedCost();
        if (submitBtn != null) submitBtn.setText("Save Changes");
    }

    private void updateEstimatedCost() {
        try {
            Resource r = (forcedResource != null) ? forcedResource : resourceCombo.getValue();
            if (r == null) { costLabel.setText("0.00"); return; }

            int qty = Integer.parseInt(quantityField.getText().trim());
            double cost = qty * r.getUnitcost();
            costLabel.setText(String.format("%.2f", cost));

        } catch (Exception ex) {
            costLabel.setText("0.00");
        }
    }

    // ===================== REAL AI CHAT =====================
    @FXML
    private void sendChat() {
        String userText = chatInput.getText();
        if (userText == null || userText.trim().isEmpty()) return;

        chatInput.clear();
        addUserBubble(userText);

        // Build DB grounding context
        String context = ResourceContextBuilder.buildContext(cachedResources);

        // Add to conversation
        conversation.add(new OpenAiChatService.ChatMessage("user", userText));

        Task<String> task = new Task<>() {
            @Override protected String call() throws IOException, SQLException {
                String systemPrompt =
                        "You are an AI assistant inside a Resource Management app.\n" +
                                "You must only use the database resources provided.\n" +
                                "Be concise, professional, and helpful.\n\n" +
                                context;

                return openAi.reply(systemPrompt, conversation);
            }
        };

        task.setOnSucceeded(ev -> {
            String ai = task.getValue();
            addAiBubble(ai);

            // keep assistant message in history
            conversation.add(new OpenAiChatService.ChatMessage("assistant", ai));

            // Try autofill if AI selected a resource
            tryAutoFillFromAi(ai);
        });

        task.setOnFailed(ev -> {
            Throwable ex = task.getException();
            addAiBubble("AI error: " + (ex != null ? ex.getMessage() : "Unknown error"));
        });

        new Thread(task).start();
    }

    private void tryAutoFillFromAi(String aiText) {
        // Expected format:
        // SELECT_RESOURCE: <id> QTY: <number>
        try {
            String upper = aiText.toUpperCase();
            if (!upper.contains("SELECT_RESOURCE:")) return;

            int idStart = upper.indexOf("SELECT_RESOURCE:") + "SELECT_RESOURCE:".length();
            String after = aiText.substring(idStart).trim();

            String[] parts = after.split("\\s+");
            int resourceId = Integer.parseInt(parts[0].replaceAll("[^0-9]", ""));

            int qty = 1;
            if (upper.contains("QTY:")) {
                int qStart = upper.indexOf("QTY:") + "QTY:".length();
                String qAfter = aiText.substring(qStart).trim();
                qty = Integer.parseInt(qAfter.replaceAll("[^0-9]", ""));
            }

            // Find resource in cached list
            Resource found = null;
            for (Resource r : cachedResources) {
                if (r.getId() == resourceId) { found = r; break; }
            }
            if (found == null) return;

            final int finalQty = qty;
            final Resource finalFound = found;

            Platform.runLater(() -> {
                if (forcedResource == null) {
                    resourceCombo.setValue(finalFound);
                }
                quantityField.setText(String.valueOf(finalQty));
                updateEstimatedCost();
            });

        } catch (Exception ignored) {
        }
    }

    private void addUserBubble(String text) {
        Label l = new Label("You: " + text);
        l.setWrapText(true);
        l.setStyle("-fx-background-color:#e5e7eb; -fx-padding:8 10; -fx-background-radius:10;");
        chatBox.getChildren().add(l);
    }

    private void addAiBubble(String text) {
        Label l = new Label("AI: " + text);
        l.setWrapText(true);
        l.setStyle("-fx-background-color:#eef2ff; -fx-padding:8 10; -fx-background-radius:10;");
        chatBox.getChildren().add(l);
    }

    // ===================== REQUEST SUBMIT =====================
    @FXML
    private void handleRequest() {
        Resource selected = (forcedResource != null) ? forcedResource : resourceCombo.getValue();
        if (selected == null) { showError("Please choose a resource."); return; }

        String projectCode = projectCodeField.getText().trim();
        if (projectCode.isEmpty()) { showError("Project code is required."); return; }

        int qty;
        try { qty = Integer.parseInt(quantityField.getText().trim()); }
        catch (Exception e) { showError("Quantity must be a number."); return; }

        if (qty <= 0) { showError("Quantity must be greater than 0."); return; }

        if (qty > selected.getAvquant()) {
            showError("Not enough available quantity. Available: " + (int) selected.getAvquant());
            return;
        }

        double totalCost = qty * selected.getUnitcost();

        try {
            if (editing == null) {
                assignmentService.requestResource(selected.getId(), projectCode, clientCode, qty, totalCost);
            } else {
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