package esprit.tn.pi_kavafx.controllers;

import esprit.tn.pi_kavafx.entities.Formation;
import esprit.tn.pi_kavafx.services.FormationService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class ClientFormationListController {

    @FXML private FlowPane cardsFlow;

    private final FormationService formationService = new FormationService();

    @FXML
    public void initialize() {
        refresh();
    }

    private void refresh() {
        cardsFlow.getChildren().clear();

        for (Formation f : formationService.getAll()) {
            cardsFlow.getChildren().add(createCard(f));
        }
    }

    private VBox createCard(Formation f) {

        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setPrefWidth(320);

        Label title = new Label(f.getTitre());
        title.getStyleClass().add("card-title");

        Label desc = new Label(f.getDescription());
        desc.setWrapText(true);
        desc.getStyleClass().add("card-desc");

        Button start = new Button("Commencer la formation");
        start.getStyleClass().add("btn-primary");

        start.setOnAction(e -> openFormation(f.getId()));

        card.getChildren().addAll(title, desc, start);
        return card;
    }

    private void openFormation(int id) {
        try {

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/client_formation_details.fxml"));
            Node page = loader.load();

            ClientFormationDetailsController controller = loader.getController();
            controller.setFormationId(id);

            ClientLayoutController.instance.contentArea.getChildren().setAll(page);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
