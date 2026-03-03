package controllers;

import entities.Resource;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import services.ResourceService;
import services.ai.OllamaChatService;
import services.ai.ResourceContextBuilder;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AiChatController {

    @FXML private VBox chatBox;
    @FXML private TextField chatInput;
    @FXML private Label hintLabel;

    private final ResourceService resourceService = new ResourceService();
    private final OllamaChatService ollama = new OllamaChatService();

    private final List<OllamaChatService.Message> conversation = new ArrayList<>();
    private List<Resource> cachedResources = new ArrayList<>();

    // callback to tell catalog controller what resource to select
    private ResourceSelectionListener listener;

    public interface ResourceSelectionListener {
        void onSelected(int resourceId, int qty);
    }

    public void setListener(ResourceSelectionListener listener) {
        this.listener = listener;
    }

    @FXML
    public void initialize() {
        addAiBubble("Hi! Tell me what you need and I’ll suggest the best resources.");
        loadResources();
    }

    private void loadResources() {
        try {
            cachedResources = resourceService.getAll();
        } catch (SQLException e) {
            addAiBubble("DB error: " + e.getMessage());
        }
    }

    @FXML
    private void sendChat() {
        String userText = chatInput.getText();
        if (userText == null || userText.trim().isEmpty()) return;
        if (cachedResources == null || cachedResources.isEmpty()) {
            addAiBubble("I couldn't load the resource catalog from database. Please check DB connection, then reopen.");
            return;
        }

        chatInput.clear();
        addUserBubble(userText);

        // Build DB grounding context (internal)
        String context = ResourceContextBuilder.buildContext(cachedResources);

        if (conversation.isEmpty()) {
            conversation.add(new OllamaChatService.Message(
                    "system",
                    "You're a resource recommendation assistant.\n" +
                            "RULES:\n" +
                            "1) NEVER say you don't have information.\n" +
                            "2) If the user is vague (e.g. 'javafx project'), ASK exactly 1 short question, THEN still recommend 2 resources from the list.\n" +
                            "3) Recommend ONLY from the provided catalog. Never invent.\n" +
                            "4) Do NOT show ids/codes/types. Show only names, suggested quantity, and total cost.\n" +
                            "5) Reply in MAX 5 lines.\n" +
                            "6) If confident, include internal command on a NEW LAST LINE exactly like:\n" +
                            "SELECT_RESOURCE:<id> QTY:<n>\n" +
                            "(That SELECT_RESOURCE line will be hidden from the user.)\n\n" +
                            "Do NOT repeat the resource catalog in your reply.\n"+
                            context
            ));
        }

        conversation.add(new OllamaChatService.Message("user", userText));

        Task<String> task = new Task<>() {
            @Override protected String call() throws IOException {
                return ollama.chat(conversation);
            }
        };

        task.setOnSucceeded(ev -> {
            String ai = task.getValue();
            conversation.add(new OllamaChatService.Message("assistant", ai));

            String cleaned = removeInternal(ai);
            addAiBubble(cleaned);

            // parse selection, notify catalog controller
            Selection sel = parseSelection(ai);
            if (sel != null && listener != null) {
                listener.onSelected(sel.resourceId, sel.qty);
                hintLabel.setText("Selected resource from AI. You can now request it.");
            }
        });

        task.setOnFailed(ev -> addAiBubble("AI error: " + task.getException().getMessage()));

        new Thread(task).start();
    }

    private record Selection(int resourceId, int qty) {}

    private Selection parseSelection(String text) {
        try {
            String up = text.toUpperCase();
            if (!up.contains("SELECT_RESOURCE:")) return null;

            int idStart = up.indexOf("SELECT_RESOURCE:") + "SELECT_RESOURCE:".length();
            String after = text.substring(idStart).trim();

            int id = Integer.parseInt(after.replaceAll("[^0-9].*", "").replaceAll("[^0-9]", ""));

            int qty = 1;
            if (up.contains("QTY:")) {
                int qStart = up.indexOf("QTY:") + "QTY:".length();
                String qAfter = text.substring(qStart).trim();
                qty = Integer.parseInt(qAfter.replaceAll("[^0-9]", ""));
            }
            return new Selection(id, qty);
        } catch (Exception e) {
            return null;
        }
    }

    private String removeInternal(String text) {
        StringBuilder sb = new StringBuilder();
        for (String line : text.split("\\n")) {
            if (!line.toUpperCase().contains("SELECT_RESOURCE")) sb.append(line).append("\n");
        }
        return sb.toString().trim();
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

    @FXML
    private void close() {
        Stage s = (Stage) chatBox.getScene().getWindow();
        s.close();
    }
}