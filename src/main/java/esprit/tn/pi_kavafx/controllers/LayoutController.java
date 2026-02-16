package esprit.tn.pi_kavafx.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;

public class LayoutController {

    @FXML
    public StackPane contentArea;

    // IMPORTANT : on garde une instance globale
    public static LayoutController instance;

    @FXML
    public void initialize() {
        instance = this; // ⭐ rend le layout accessible depuis toutes les pages
        loadPage("client_formation_list.fxml"); // page par défaut
    }

    public void loadPage(String fxml) {
        try {

            // 🔴 VERY IMPORTANT — release videos before switching page
            if (!contentArea.getChildren().isEmpty()) {

                Node node = contentArea.getChildren().get(0);

                // if the page has a controller → we can call stopPlayers()
                Object controller = node.getProperties().get("controller");

                if (controller instanceof FormationDetailsController detailsController) {
                    detailsController.stopPlayers();
                }
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/" + fxml));
            Parent view = loader.load();

            // store controller inside node (so next time we can retrieve it)
            view.getProperties().put("controller", loader.getController());

            contentArea.getChildren().setAll(view);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    @FXML
    private void goFormations() {
        loadPage("formation_list.fxml");
    }

    @FXML
    private void goAddFormation() {
        loadPage("formation_add.fxml");
    }


}
