package controllers;

import java.time.LocalDate;
import entities.Reclamation;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;
import services.ReclamationService;
import services.UserService;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class ReclamationController implements Initializable {

    @FXML
    private TableView<Reclamation> reclamationTable;

    @FXML
    private TableColumn<Reclamation, String> colTitre, colCategorie, colProjet, colStatut, colDate;

    @FXML
    private TableColumn<Reclamation, Void> colReponse;



    // Ajout du service
    private ReclamationService reclamationService;
    private ObservableList<Reclamation> reclamationList;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Initialisation du service
        reclamationService = new ReclamationService();

        if (reclamationTable != null) {
            setupTable();
            loadData(); // Chargement des données réelles
        }// Chargement des données réelles
    }

    private void loadData() {
        // Récupération des données depuis la base de données via le service
        reclamationList = reclamationService.getAll();
        reclamationTable.setItems(reclamationList);
    }

    private void setupTable() {
        // 1. Configuration des colonnes simples (Texte)
        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colProjet.setCellValueFactory(new PropertyValueFactory<>("projet"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));

        // 2. Configuration de la colonne CATÉGORIE (Badge Gris) - DESIGN CONSERVÉ
        colCategorie.setCellValueFactory(new PropertyValueFactory<>("categorie"));
        colCategorie.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    badge.getStyleClass().add("category-pill"); // Défini dans style.css
                    setGraphic(badge);
                }
            }
        });

        // 3. Configuration de la colonne STATUT (Badges Colorés) - DESIGN CONSERVÉ
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colStatut.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    badge.getStyleClass().add("status-pill"); // Forme de base

                    // Application de la classe CSS selon le statut
                    switch (item) {
                        case "En attente":
                            badge.getStyleClass().add("status-open");
                            break;
                        case "En cours":
                            badge.getStyleClass().add("status-progress");
                            break;
                        case "Rèsolu":
                            badge.getStyleClass().add("status-resolved");
                            break;
                        case "Fermer":
                            badge.getStyleClass().add("status-closed");
                            break;
                        default:
                            badge.getStyleClass().add("status-closed");
                            break;
                    }
                    setGraphic(badge);
                }
            }
        });

        // 4. Configuration de la colonne RÉPONSE (Boutons) - DESIGN CONSERVÉ
        colReponse.setCellFactory(column -> new TableCell<>() {
            private final Button replyBtn = new Button();
            private final Button deleteBtn = new Button();

            {
                // --- Bouton 1 : Voir / Répondre (Violet) ---
                FontIcon replyIcon = new FontIcon("mdi2m-message-processing-outline");
                replyIcon.setIconSize(18);
                replyBtn.setGraphic(replyIcon);
                replyBtn.getStyleClass().add("btn-response");
                replyBtn.setTooltip(new Tooltip("Voir la réponse"));

                replyBtn.setOnAction(event -> {
                    Reclamation rec = getTableView().getItems().get(getIndex());
                    openModifyPopup(rec);
                });

                // --- Bouton 2 : Supprimer (Rouge) ---
                FontIcon deleteIcon = new FontIcon("mdi2t-trash-can-outline");
                deleteIcon.setIconSize(18);
                deleteBtn.setGraphic(deleteIcon);
                deleteBtn.getStyleClass().addAll("action-btn", "action-btn-delete");
                deleteBtn.setTooltip(new Tooltip("Supprimer"));

                // IMPORTANT: This must be the ONLY setOnAction for deleteBtn
                deleteBtn.setOnAction(event -> {
                    // 1. Get the selected item
                    Reclamation rec = getTableView().getItems().get(getIndex());

                    // 2. Confirmation Alert
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("Confirmation");
                    alert.setHeaderText(null);
                    alert.setContentText("Voulez-vous vraiment supprimer : " + rec.getTitre() + " ?");

                    // 3. Delete if OK is clicked
                    if (alert.showAndWait().get() == ButtonType.OK) {
                        // Delete from Database
                        reclamationService.delete(rec.getId());
                        // Remove from TableView (UI)
                        getTableView().getItems().remove(rec);
                    }
                });

                // REMOVED THE DUPLICATE EMPTY CODE THAT WAS HERE
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox buttons = new HBox(8, replyBtn, deleteBtn);
                    buttons.setAlignment(Pos.CENTER);
                    setGraphic(buttons);
                }
            }
        });
    }

    public void handleUserManagementNavigation(ActionEvent event) {
        switchScene(event, "/User/UserTable.fxml");
    }

    private void switchScene(ActionEvent event, String fxmlPath) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void handleLogout(ActionEvent event) {
        UserService.logout();
        switchScene(event, "/Start/1ere.fxml");
    }

    @FXML
    public void addreclamation(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Reclamation/popUpRec.fxml"));
            Parent root = loader.load();

            Stage popupStage = new Stage();
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.setScene(new Scene(root));

            // Le code s'arrête ici tant que le popup est ouvert
            popupStage.showAndWait();

            // --- C'est ICI qu'il faut rafraîchir ---
            // Cette ligne s'exécute uniquement quand le popup est fermé
            if (reclamationTable != null) {
                loadData();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML private TextField titreField;
    @FXML private TextField categorieField;
    @FXML private TextField projetField;
    @FXML private ComboBox<String> statutCombo;
    @FXML private Button submitBtn;

    // NOTE : J'ai supprimé @FXML private TextField dateField; car il n'existe plus dans le FXML

    @FXML
    public void saveReclamation(ActionEvent event) {
        // 1. Récupération des données saisies
        String titre = titreField.getText();
        String cat = categorieField.getText();
        String proj = projetField.getText();
        String statut = statutCombo.getValue();

        // 2. Génération automatique de la date du jour
        String date = LocalDate.now().toString(); // Donne format "2023-10-27"

        // 3. Création de l'objet
        Reclamation r = new Reclamation(titre, cat, proj, statut, date);

        // 4. Appel au service (Assurez-vous d'avoir créé la méthode add dans ReclamationService !)
        reclamationService.add(r);

        System.out.println("Reclamation ajoutée : " + r.getTitre());

        // 5. Fermer la fenêtre
        closePopup(event);
    }

    @FXML
    public void closePopup(ActionEvent event) {
        ((Node) event.getSource()).getScene().getWindow().hide();
    }
    // 1. Variable pour stocker l'ID de la réclamation en cours de modification
    private int idAModifier = 0;

    // 2. Méthode pour pré-remplir les champs (appelée à l'ouverture du popup)
    public void initData(Reclamation r) {
        if (r != null) {
            this.idAModifier = r.getId(); // On garde l'ID en mémoire
            titreField.setText(r.getTitre());
            categorieField.setText(r.getCategorie());
            projetField.setText(r.getProjet());
            statutCombo.setValue(r.getStatut());
        }
    }

    // 3. Méthode appelée par le bouton "Modifier/Valider" du popup
    @FXML
    public void updateReclamation(ActionEvent event) {
        // Récupérer les nouvelles valeurs
        String titre = titreField.getText();
        String cat = categorieField.getText();
        String proj = projetField.getText();
        String statut = statutCombo.getValue();
        // On garde la date existante ou on en met une nouvelle, ici je garde l'ancienne logique date
        String date = java.time.LocalDate.now().toString();

        // Créer l'objet avec l'ID sauvegardé
        Reclamation r = new Reclamation(titre, cat, proj, statut, date);
        r.setId(idAModifier); // IMPORTANT : remettre l'ID

        // Appel au service
        reclamationService.update(r);

        // Fermer la fenêtre
        closePopup(event);
    }
    private void openModifyPopup(Reclamation rec) {
        try {
            // 1. Charger le fichier FXML de modification
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Reclamation/popUpmodif.fxml"));
            Parent root = loader.load();

            // 2. Récupérer le contrôleur de cette nouvelle fenêtre
            ReclamationController controller = loader.getController();

            // 3. Lui passer les données de la ligne sélectionnée
            controller.initData(rec);

            // 4. Afficher la fenêtre
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Modifier Réclamation");
            stage.setScene(new Scene(root));
            stage.showAndWait(); // Attendre la fermeture

            // 5. Rafraîchir le tableau principal après modification
            loadData();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}