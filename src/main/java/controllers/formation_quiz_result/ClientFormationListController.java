package controllers.formation_quiz_result;

import entities.Formation;
import entities.Participer;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import services.FormationService;
import services.ParticiperService;
import services.UserService;
import utils.Router;

import java.util.List;
import java.util.stream.Collectors;

public class ClientFormationListController {

    @FXML private FlowPane cardsFlow;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterCombo;

    private final FormationService formationService = new FormationService();
    private final ParticiperService participerService = new ParticiperService();
    private final int userId = UserService.getCurrentUser().getId();

    private List<Formation> formations;

    @FXML
    public void initialize() {

        // charger toutes les formations
        formations = formationService.getAll();

        // remplir filtre
        filterCombo.getItems().addAll(
                "Toutes",
                "Nouveau",
                "En cours",
                "Réussi",
                "Échoué"
        );
        filterCombo.setValue("Toutes");

        // listeners LIVE (le plus important)
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilter());
        filterCombo.valueProperty().addListener((obs, oldVal, newVal) -> applyFilter());

        applyFilter();
    }

    private void applyFilter() {

        String search = searchField.getText().toLowerCase();
        String filter = filterCombo.getValue();

        List<Formation> filtered = formations.stream()

                // 🔎 recherche texte
                .filter(f ->
                        f.getTitre().toLowerCase().contains(search)
                                || f.getDescription().toLowerCase().contains(search)
                )

                // 🎯 filtre statut
                .filter(f -> {

                    Participer p = participerService.getParticipation(userId, f.getId());

                    if(filter.equals("Toutes")) return true;

                    if(filter.equals("Nouveau")) return p == null;

                    if(p == null) return false;

                    switch(filter){
                        case "En cours": return p.getStatut().equals("EN_COURS");
                        case "Réussi": return p.getStatut().equals("REUSSI");
                        case "Échoué": return p.getStatut().equals("ECHOUE");
                    }

                    return true;
                })

                .collect(Collectors.toList());

        refreshCards(filtered);
    }

    private void refreshCards(List<Formation> list){
        cardsFlow.getChildren().clear();
        list.forEach(f -> cardsFlow.getChildren().add(createCard(f)));
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
