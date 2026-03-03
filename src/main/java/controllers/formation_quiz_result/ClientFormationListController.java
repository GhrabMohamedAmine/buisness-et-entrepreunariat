package controllers.formation_quiz_result;

import entities.Formation;
import entities.Participer;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
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
        System.out.println("userId = " + userId);
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
        card.setPrefWidth(340);
        card.setPadding(new Insets(14));
        card.setStyle(
                "-fx-background-color:white;" +
                        "-fx-background-radius:14;" +
                        "-fx-effect:dropshadow(gaussian, rgba(0,0,0,0.12), 18, 0.25, 0, 5);"
        );

        // TITLE
        Label title = new Label(f.getTitre());
        title.setWrapText(true);
        title.setStyle(
                "-fx-font-size:17px;" +
                        "-fx-font-weight:bold;" +
                        "-fx-text-fill:#3b2f6b;"
        );

        // DESCRIPTION
        Label desc = new Label(f.getDescription());
        desc.setWrapText(true);
        desc.setStyle(
                "-fx-text-fill:#6b7280;"
        );

        // BADGE
        Label badge = new Label();
        badge.setPadding(new Insets(4,10,4,10));
        badge.setStyle(
                "-fx-font-weight:bold;" +
                        "-fx-background-radius:20;" +
                        "-fx-text-fill:white;"
        );

        // PROGRESS BAR
        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(300);
        progressBar.setStyle("-fx-background-color: #7C3AED;"+ /* This is the Nexum Purple */
        "-fx-background-radius: 5;");

        // ACTION BUTTON
        Button actionBtn = new Button();
        actionBtn.setStyle(
                "-fx-background-color:#6d5dfc;" +
                        "-fx-text-fill:white;" +
                        "-fx-font-weight:bold;" +
                        "-fx-background-radius:8;" +
                        "-fx-cursor:hand;"
        );

        if(p == null){
            badge.setText("Nouveau cours");
            badge.setStyle(badge.getStyle() + "-fx-background-color:#6366f1;");

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
                    badge.setStyle(badge.getStyle() + "-fx-background-color:#f59e0b;");
                    actionBtn.setText("Continuer");
                    break;

                case "REUSSI":
                    badge.setText("Réussi ✔");
                    badge.setStyle(badge.getStyle() + "-fx-background-color:#22c55e;");
                    actionBtn.setText("Revoir");
                    break;

                case "ECHOUE":
                    badge.setText("Échoué");
                    badge.setStyle(badge.getStyle() + "-fx-background-color:#ef4444;");
                    actionBtn.setText("Repasser");
                    break;
            }

            actionBtn.setOnAction(e -> openFormation(f.getId()));
        }

        // Hover animation (VERY IMPORTANT visually)
        card.setOnMouseEntered(e -> {
            card.setTranslateY(-5);
            card.setStyle(
                    "-fx-background-color:white;" +
                            "-fx-background-radius:14;" +
                            "-fx-effect:dropshadow(gaussian, rgba(109,93,252,0.35), 28, 0.35, 0, 10);"
            );
        });

        card.setOnMouseExited(e -> {
            card.setTranslateY(0);
            card.setStyle(
                    "-fx-background-color:white;" +
                            "-fx-background-radius:14;" +
                            "-fx-effect:dropshadow(gaussian, rgba(0,0,0,0.12), 18, 0.25, 0, 5);"
            );
        });

        card.getChildren().addAll(title, badge, desc, progressBar, actionBtn);
        return card;
    }
    private void openFormation(int id) {
        participerService.inscrire(userId,id);
        Router.goToFormationDetails(id);
    }
}
