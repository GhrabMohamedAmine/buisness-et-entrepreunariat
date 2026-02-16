package controllers;

import entities.Reclamation;
import entities.User;
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
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;
import services.ReclamationService;
import services.UserService;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

public class UserReclamationController implements Initializable {

    @FXML private Label topName;
    @FXML private Circle btnProfil;
    @FXML
    private FlowPane ticketsContainer; // Le conteneur des cartes

    @FXML
    private Label lblOpen, lblProgress, lblSolved; // Les stats

    @FXML
    private TextField searchField;

    private ReclamationService service;
    private User currentUser;
    private ObservableList<Reclamation> myReclamations;

    private void loadCurrentUserProfile() {
        User currentUser = UserService.getCurrentUser();

        if (currentUser != null) {
            // 1. Update the Name (Alex -> Real Name)
            String fullName = currentUser.getFirstName() + " " + currentUser.getName();
            topName.setText(fullName);

            // 2. Update the Avatar Circle
            String imagePath = currentUser.getImageLink();
            if (imagePath != null && !imagePath.isEmpty()) {
                try {
                    Image img = new Image(imagePath, 32, 32, true, true);
                    if (!img.isError()) {
                        btnProfil.setFill(new ImagePattern(img));
                    }
                } catch (Exception e) {
                    System.out.println("Error loading profile image: " + e.getMessage());
                }
            }
        }
    }
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        service = new ReclamationService();

        // 1. Récupérer l'utilisateur connecté depuis ton UserService
        // Assure-toi que UserService.getCurrentUser() renvoie bien l'objet statique
        currentUser = UserService.getCurrentUser();

        if (currentUser == null) {
            System.err.println("Aucun utilisateur connecté !");
            return;
        }

        // 2. Charger les données et afficher
        loadData();
        loadCurrentUserProfile();

        // 3. Ajouter la recherche dynamique
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterReclamations(newValue);
        });
    }

    private void loadData() {
        // Vider l'affichage actuel
        ticketsContainer.getChildren().clear();

        // Récupérer SEULEMENT les réclamations de ce user
        myReclamations = service.getReclamationsByUserId(currentUser.getId());

        int open = 0, progress = 0, solved = 0;

        // Boucle pour créer les cartes
        for (Reclamation r : myReclamations) {
            // Calcul des stats
            if ("En attente".equalsIgnoreCase(r.getStatut())) open++;
            else if ("En cours".equalsIgnoreCase(r.getStatut())) progress++;
            else if ("Résolu".equalsIgnoreCase(r.getStatut())) solved++;

            // Création graphique de la carte
            VBox card = createTicketCard(r);
            ticketsContainer.getChildren().add(card);
        }

        // Mise à jour des labels de stats
        lblOpen.setText(String.valueOf(open));
        lblProgress.setText(String.valueOf(progress));
        lblSolved.setText(String.valueOf(solved));
    }

    // --- CRÉATION DYNAMIQUE DES CARTES (DESIGN JAVA) ---
    private VBox createTicketCard(Reclamation r) {
        VBox card = new VBox();
        card.getStyleClass().add("ticket-card");
        card.setPrefWidth(280);
        card.setMinHeight(180);

        // 1. En-tête (Titre + Badge Statut)
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setSpacing(10);

        Label title = new Label(r.getTitre());
        title.getStyleClass().add("ticket-title");
        title.setWrapText(true);
        title.setMaxWidth(180);

        Label status = new Label(r.getStatut());
        status.getStyleClass().add("status-pill");

        // Couleur selon statut
        String s = r.getStatut().toLowerCase();
        if (s.contains("cours")) status.getStyleClass().add("status-progress");
        else if (s.contains("rèsolu") || s.contains("resolu")) status.getStyleClass().add("status-resolved");
        else status.getStyleClass().add("status-open");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(title, spacer, status);

        // 2. Sous-titre (Catégorie / Projet)
        Label subtitle = new Label(r.getCategorie() + " • " + r.getProjet());
        subtitle.getStyleClass().add("ticket-subtitle");

        // 3. Description (On met juste un placeholder ou le début du contenu si dispo)
        Label desc = new Label("Ticket créé le " + r.getDate());
        desc.getStyleClass().add("ticket-desc");
        desc.setWrapText(true);
        VBox.setVgrow(desc, Priority.ALWAYS); // Pousse le footer vers le bas

        // 4. Footer (Boutons Actions)
        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setStyle("-fx-padding: 15 0 0 0; -fx-border-color: #F3F4F6; -fx-border-width: 1 0 0 0;");
        footer.setSpacing(8);

        // Bouton Modifier (Seulement si pas résolu, par exemple)
        Button btnEdit = new Button();
        btnEdit.getStyleClass().add("icon-btn");
        FontIcon editIcon = new FontIcon("mdi2p-pencil-outline");
        editIcon.setIconColor(Color.web("#6B7280"));
        btnEdit.setGraphic(editIcon);
        btnEdit.setOnAction(e -> openModifyModal(r)); // Action Modifier

        // Bouton Supprimer
        Button btnDelete = new Button();
        btnDelete.getStyleClass().add("icon-btn");
        FontIcon delIcon = new FontIcon("mdi2t-trash-can-outline");
        delIcon.setIconColor(Color.web("#EF4444"));
        btnDelete.setGraphic(delIcon);
        btnDelete.setOnAction(e -> handleDelete(r)); // Action Supprimer

        footer.getChildren().addAll(btnEdit, btnDelete);

        card.getChildren().addAll(header, subtitle, desc, footer);
        return card;
    }

    // --- ACTIONS & POPUPS ---

    @FXML
    public void openNewReclamationModal(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Reclamation/popUpRec.fxml"));
            // Attention : Vérifie le chemin exact de ton FXML
            Parent root = loader.load();

            // Note: Le controller lié à popUpRec (ReclamationController) va gérer l'ajout.
            // S'il utilise UserService.getCurrentUser(), ça ajoutera bien au bon user.

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Nouveau Ticket");
            stage.setScene(new Scene(root));
            stage.showAndWait(); // On attend la fermeture

            // Une fois fermé, on recharge la grille pour voir le nouveau ticket
            loadData();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openModifyModal(Reclamation r) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Reclamation/popUpmodif.fxml"));
            Parent root = loader.load();

            // On doit passer les données au contrôleur de modification
            // Je suppose que ton ReclamationController a une méthode initData(Reclamation r)
            // Si c'est un autre controller, change le type ici.
            ReclamationController controller = loader.getController();
            controller.initData(r);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Modifier Ticket");
            stage.setScene(new Scene(root));
            stage.showAndWait();

            // Recharger après modif
            loadData();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleDelete(Reclamation r) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Suppression");
        alert.setHeaderText("Supprimer le ticket ?");
        alert.setContentText("Cette action est irréversible.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            service.delete(r.getId()); // Assure-toi d'avoir delete(int id) dans le service
            loadData(); // Rafraîchir l'affichage
        }
    }

    // Petite méthode pour la recherche locale (filtrage visuel)
    private void filterReclamations(String query) {
        ticketsContainer.getChildren().clear();
        for (Reclamation r : myReclamations) {
            if (r.getTitre().toLowerCase().contains(query.toLowerCase()) ||
                    r.getProjet().toLowerCase().contains(query.toLowerCase())) {
                ticketsContainer.getChildren().add(createTicketCard(r));
            }
        }
    }
    private void switchScene(MouseEvent event, String fxmlPath) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            System.err.println("Impossible de charger le fichier FXML : " + fxmlPath);
            e.printStackTrace();
        }
    }

    public void versProfil(MouseEvent event) {
        switchScene(event, "/Profile/Profile1.fxml");
    }
}