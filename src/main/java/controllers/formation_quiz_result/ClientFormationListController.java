package controllers.formation_quiz_result;

import entities.Formation;
import entities.Participer;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import services.FormationService;
import services.ParticiperService;
import services.UserService;
import utils.Router;

public class ClientFormationListController {

    @FXML private FlowPane cardsFlow;

    private final FormationService formationService = new FormationService();
    private final ParticiperService participerService = new ParticiperService();
    private final int userId = UserService.getCurrentUser().getId();

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

        Participer p = participerService.getParticipation(userId, f.getId());

        VBox card = new VBox(12);
        card.getStyleClass().add("formation-card");
        card.setPrefWidth(340);

        Label title = new Label(f.getTitre());
        title.getStyleClass().add("formation-title");

        Label desc = new Label(f.getDescription());
        desc.setWrapText(true);
        desc.getStyleClass().add("formation-desc");

        Label badge = new Label();
        badge.getStyleClass().add("formation-badge");

        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(300);

        Button actionBtn = new Button();
        actionBtn.getStyleClass().add("formation-btn");

        if(p == null){
            badge.setText("Nouveau cours");
            badge.getStyleClass().add("badge-new");

            actionBtn.setText("Commencer");
            actionBtn.setOnAction(e -> openFormation(f.getId()));

            progressBar.setVisible(false);
        }
        else{
            progressBar.setVisible(true);
            progressBar.setProgress(p.getProgression()/100.0);

            switch(p.getStatut()){
                case "EN_COURS":
                    badge.setText("En cours");
                    badge.getStyleClass().add("badge-progress");
                    actionBtn.setText("Continuer");
                    break;

                case "REUSSI":
                    badge.setText("Réussi ✔");
                    badge.getStyleClass().add("badge-success");
                    actionBtn.setText("Revoir");
                    break;

                case "ECHOUE":
                    badge.setText("Échoué");
                    badge.getStyleClass().add("badge-fail");
                    actionBtn.setText("Repasser");
                    break;
            }

            actionBtn.setOnAction(e -> openFormation(f.getId()));
        }

        card.getChildren().addAll(title, badge, desc, progressBar, actionBtn);
        return card;
    }

    private void openFormation(int id) {
        participerService.inscrire(userId,id);
        Router.goToFormationDetails(id);
    }
}
