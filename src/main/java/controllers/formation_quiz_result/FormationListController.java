package controllers.formation_quiz_result;

import entities.Formation;
import  services.FormationService;
import utils.DialogUtil;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class FormationListController {

    @FXML private FlowPane cardsFlow;

    private final FormationService formationService = new FormationService();

    @FXML
    public void initialize() {
        refresh();
    }

    private void refresh() {
        cardsFlow.getChildren().clear();

        try {
            for (Formation f : formationService.getAll()) {
                cardsFlow.getChildren().add(createCard(f));
            }
        } catch (Exception e) {
            DialogUtil.error("Erreur", "Impossible de charger les formations.\n" + e.getMessage());
        }
    }

    private VBox createCard(Formation f) {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setPrefWidth(340);
        Button delete = new Button("Supprimer");
        delete.getStyleClass().add("btn-danger");

        Label title = new Label(f.getTitre());
        title.getStyleClass().add("card-title");

        Label desc = new Label(f.getDescription());
        desc.getStyleClass().add("card-desc");
        desc.setMaxHeight(60);

        Button consulter = new Button("Consulter");
        consulter.getStyleClass().add("btn-primary");
        consulter.setOnAction(e -> openDetails(f.getId()));

        Button modifier = new Button("Modifier");
        modifier.getStyleClass().add("btn-outline");
        modifier.setOnAction(e -> openEdit(f.getId()));
        delete.setOnAction(e -> {

            boolean confirm = DialogUtil.confirm(
                    "Supprimer formation",
                    "Cette action supprimera aussi tous les quiz associés.\nContinuer ?"
            );

            if(confirm){
                formationService.delete(f.getId());
                refresh(); // refresh la liste
            }
        });

        HBox actions = new HBox(10, consulter, modifier , delete);

        card.getChildren().addAll(title, desc, actions);
        return card;
    }

    private void openDetails(int formationId) {
        FormationDetailsController.setSelectedFormation(formationId);
        utils.Router.goTo("formation_details.fxml");
    }


    private void openEdit(int formationId) {
        FormationEditController.open(formationId);
    }

}
