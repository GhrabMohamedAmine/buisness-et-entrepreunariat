package tezfx.controller;

import javafx.scene.Parent;
import tezfx.app.ViewLoader;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;

public class MainController {
    @FXML private StackPane contentArea;
    private static StackPane staticContentArea;

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
    }
}
