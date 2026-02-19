package controllers.formation_quiz_result;

import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;
import utils.Router;

public class ClientLayoutController {

    @FXML
    private StackPane contentArea;

    @FXML
    public void initialize() {

        // ⭐ register the container ONCE
        Router.setMainContainer(contentArea);

        // default page
        Router.goTo("client_formation_list.fxml");
    }

    public void goCatalogue(){
        Router.goTo("client_formation_list.fxml");
    }

    public void goResults(){
        Router.goTo("client_results.fxml");
    }

    public void goHome(){
        Router.goTo("role_choice.fxml");
    }
}
