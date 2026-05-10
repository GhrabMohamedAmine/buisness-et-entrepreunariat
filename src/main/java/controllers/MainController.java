package controllers;

import javafx.scene.Parent;
import Mains.ViewLoader;
import entities.User;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import services.CurrentUserService;

public class MainController {
    @FXML private StackPane contentArea;
    @FXML private Label currentUserNameLabel;
    @FXML private Label currentUserRoleLabel;
    private static StackPane staticContentArea;
    private final CurrentUserService currentUserService = new CurrentUserService();

    public static void setView(String fxmlFileName) {
        Node node = ViewLoader.load(fxmlFileName);
        if (node != null && staticContentArea != null) {
            staticContentArea.getChildren().setAll(node);
        }
    }
    public static StackPane getStaticContentArea() {
        return staticContentArea;
    }

    public static void setContent(Parent root) {
        if (root != null && staticContentArea != null) {
            staticContentArea.getChildren().setAll(root);
        }
    }

    @FXML
    public void initialize() {
        staticContentArea = contentArea;
        User currentUser = currentUserService.getCurrentUser();
        if (currentUserNameLabel != null) {
            currentUserNameLabel.setText(currentUser.getFullName());
        }
        if (currentUserRoleLabel != null) {
            currentUserRoleLabel.setText(formatRole(currentUser.getRole()));
        }
    }

    private String formatRole(String role) {
        if (role == null || role.isBlank()) {
            return "";
        }
        String cleaned = role.replace("ROLE_", "").replace('_', ' ').trim().toLowerCase();
        StringBuilder formatted = new StringBuilder();
        for (String part : cleaned.split("\\s+")) {
            if (part.isBlank()) continue;
            if (!formatted.isEmpty()) {
                formatted.append(' ');
            }
            formatted.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return formatted.toString();
    }
}
