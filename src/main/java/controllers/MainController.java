package controllers;

import entities.User;
import javafx.scene.Parent;
import Mains.ViewLoader;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import services.UserService;

public class MainController {
    @FXML private StackPane contentArea;
    private static StackPane staticContentArea;
    @FXML
    private BorderPane mainView;
    private UserService service =  new UserService();
    private User currentuser;

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
        mainView.getStylesheets().addAll(
                getClass().getResource("/styles/stylesheet.css").toExternalForm(),
                getClass().getResource("/styles/buttons.css").toExternalForm(),
                getClass().getResource("/styles/project-css.css").toExternalForm()
        );
        staticContentArea = contentArea;
        currentuser = service.getCurrentUser();
        System.out.println("Current user: " + currentuser.toString());

    }
    public User getCurrentuser() {
        return currentuser;
    }
}
