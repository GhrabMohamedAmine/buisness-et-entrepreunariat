package esprit.tn.pi_kavafx.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;

public class ClientLayoutController {

    @FXML
    public StackPane contentArea;

    public static ClientLayoutController instance;

    @FXML
    public void initialize() {
        instance = this;
        loadPage("client_formation_list.fxml");
    }

    public void loadPage(String fxml) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/" + fxml));
            Parent view = loader.load();
            contentArea.getChildren().setAll(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void goCatalogue(){
        loadPage("client_formation_list.fxml");
    }

    public void goResults(){
        loadPage("client_results.fxml");
    }

    public void goHome(){
        ParentLoader.setRoot("role_choice.fxml");
    }
}
