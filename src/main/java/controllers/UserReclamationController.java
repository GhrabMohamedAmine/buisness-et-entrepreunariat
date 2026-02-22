package controllers;

import entities.Reclamation;
import entities.User;
import javafx.collections.FXCollections;
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
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
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

import java.awt.*;
import java.io.*;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class UserReclamationController implements Initializable {

    @FXML private Label topName;
    @FXML private Circle btnProfil;
    @FXML private FlowPane ticketsContainer;
    @FXML private Label lblOpen, lblProgress, lblSolved;
    @FXML private TextField searchField;

    // Nouveaux boutons de filtre
    @FXML private ToggleButton filterAll;
    @FXML private ToggleButton filterOpen;
    @FXML private ToggleButton filterProgress;
    @FXML private ToggleButton filterResolved;

    private ReclamationService service;
    private User currentUser;
    private ObservableList<Reclamation> myReclamations;
    private ToggleGroup filterGroup;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        service = new ReclamationService();
        currentUser = UserService.getCurrentUser();

        if (currentUser == null) {
            System.err.println("Aucun utilisateur connecté !");
            return;
        }

        // Initialisation du groupe de filtres
        filterGroup = new ToggleGroup();
        filterAll.setToggleGroup(filterGroup);
        filterOpen.setToggleGroup(filterGroup);
        filterProgress.setToggleGroup(filterGroup);
        filterResolved.setToggleGroup(filterGroup);
        filterAll.setSelected(true);

        loadData();
        loadCurrentUserProfile();

        // Écouteurs pour la recherche et les filtres
        searchField.textProperty().addListener((observable, oldValue, newValue) -> applyFilters());
        filterGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> applyFilters());
    }

    private void loadCurrentUserProfile() {
        User currentUser = UserService.getCurrentUser();
        if (currentUser != null) {
            String fullName = currentUser.getFirstName() + " " + currentUser.getName();
            topName.setText(fullName);

            byte[] imageData = currentUser.getImageData();
            if (imageData != null && imageData.length > 0) {
                try (ByteArrayInputStream bais = new ByteArrayInputStream(imageData)) {
                    Image img = new Image(bais, 32, 32, true, true);
                    if (!img.isError()) {
                        btnProfil.setFill(new ImagePattern(img));
                    } else {
                        btnProfil.setFill(javafx.scene.paint.Color.web("#E0E7FF"));
                    }
                } catch (Exception e) {
                    System.out.println("Erreur chargement image profil : " + e.getMessage());
                    btnProfil.setFill(javafx.scene.paint.Color.web("#E0E7FF"));
                }
            } else {
                btnProfil.setFill(javafx.scene.paint.Color.web("#E0E7FF"));
            }
        }
    }

    private void loadData() {
        myReclamations = service.getReclamationsByUserId(currentUser.getId());
        updateCounts();
        applyFilters();
    }

    private void updateCounts() {
        int open = 0, progress = 0, solved = 0;
        for (Reclamation r : myReclamations) {
            if ("En attente".equalsIgnoreCase(r.getStatut())) open++;
            else if ("En cours".equalsIgnoreCase(r.getStatut())) progress++;
            else if ("Résolu".equalsIgnoreCase(r.getStatut())) solved++;
        }
        lblOpen.setText(String.valueOf(open));
        lblProgress.setText(String.valueOf(progress));
        lblSolved.setText(String.valueOf(solved));
    }

    private void applyFilters() {
        String searchText = searchField.getText().toLowerCase().trim();
        Toggle selectedFilter = filterGroup.getSelectedToggle();

        Predicate<Reclamation> statusPredicate = r -> {
            if (selectedFilter == null || selectedFilter == filterAll) return true;
            if (selectedFilter == filterOpen) return "En attente".equalsIgnoreCase(r.getStatut());
            if (selectedFilter == filterProgress) return "En cours".equalsIgnoreCase(r.getStatut());
            if (selectedFilter == filterResolved) return "Résolu".equalsIgnoreCase(r.getStatut());
            return true;
        };

        Predicate<Reclamation> searchPredicate = r -> {
            if (searchText.isEmpty()) return true;
            return r.getTitre().toLowerCase().contains(searchText)
                    || r.getProjet().toLowerCase().contains(searchText)
                    || r.getCategorie().toLowerCase().contains(searchText);
        };

        // Filtrer la liste
        ObservableList<Reclamation> filtered = myReclamations.stream()
                .filter(statusPredicate.and(searchPredicate))
                .collect(Collectors.toCollection(FXCollections::observableArrayList));

        // Afficher les cartes
        ticketsContainer.getChildren().clear();
        for (Reclamation r : filtered) {
            ticketsContainer.getChildren().add(createTicketCard(r));
        }
    }

    private VBox createTicketCard(Reclamation r) {
        VBox card = new VBox();
        card.getStyleClass().add("ticket-card");
        card.setPrefWidth(280);
        card.setMinHeight(180);

        // Ajout de l'événement de clic pour ouvrir le fichier
        card.setOnMouseClicked(event -> {
            // On évite de déclencher si on clique sur un bouton (les boutons consomment l'événement)
            if (!(event.getTarget() instanceof Button) && !(event.getTarget() instanceof FontIcon)) {
                openAttachedFile(r);
            }
        });

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setSpacing(10);

        Label title = new Label(r.getTitre());
        title.getStyleClass().add("ticket-title");
        title.setWrapText(true);
        title.setMaxWidth(180);

        Label status = new Label(r.getStatut());
        status.getStyleClass().add("status-pill");

        String s = r.getStatut().toLowerCase();
        if (s.contains("cours")) status.getStyleClass().add("status-progress");
        else if (s.contains("rèsolu") || s.contains("resolu")) status.getStyleClass().add("status-resolved");
        else status.getStyleClass().add("status-open");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(title, spacer, status);

        Label subtitle = new Label(r.getCategorie() + " • " + r.getProjet());
        subtitle.getStyleClass().add("ticket-subtitle");

        Label desc = new Label("Ticket créé le " + r.getDate());
        desc.getStyleClass().add("ticket-desc");
        desc.setWrapText(true);
        VBox.setVgrow(desc, Priority.ALWAYS);

        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setStyle("-fx-padding: 15 0 0 0; -fx-border-color: #F3F4F6; -fx-border-width: 1 0 0 0;");
        footer.setSpacing(8);

        Button btnEdit = new Button();
        btnEdit.getStyleClass().add("icon-btn");
        FontIcon editIcon = new FontIcon("mdi2p-pencil-outline");
        editIcon.setIconColor(Color.web("#6B7280"));
        btnEdit.setGraphic(editIcon);

        boolean modifiable = "En attente".equals(r.getStatut());
        btnEdit.setDisable(!modifiable);
        if (!modifiable) {
            btnEdit.setTooltip(new Tooltip("Impossible de modifier une réclamation dont le statut n'est pas 'En attente'"));
        } else {
            btnEdit.setTooltip(new Tooltip("Modifier la réclamation"));
        }
        btnEdit.setOnAction(e -> openModifyModal(r));

        Button btnDelete = new Button();
        btnDelete.getStyleClass().add("icon-btn");
        FontIcon delIcon = new FontIcon("mdi2t-trash-can-outline");
        delIcon.setIconColor(Color.web("#EF4444"));
        btnDelete.setGraphic(delIcon);
        btnDelete.setOnAction(e -> handleDelete(r));

        footer.getChildren().addAll(btnEdit, btnDelete);

        card.getChildren().addAll(header, subtitle, desc, footer);
        return card;
    }

    private void openAttachedFile(Reclamation rec) {
        Reclamation fullRec = service.getById(rec.getId());
        if (fullRec == null || fullRec.getFichier() == null || fullRec.getFichier().length == 0) {
            showAlert("Information", "Aucune pièce jointe pour cette réclamation.");
            return;
        }

        try {
            File tempFile = File.createTempFile("reclamation_" + rec.getId() + "_", ".tmp");
            tempFile.deleteOnExit();

            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(fullRec.getFichier());
            }

            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(tempFile);
            } else {
                showAlert("Erreur", "L'ouverture de fichiers n'est pas supportée sur ce système.");
            }
        } catch (IOException e) {
            showAlert("Erreur", "Impossible d'ouvrir le fichier : " + e.getMessage());
        }
    }

    @FXML
    public void openNewReclamationModal(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Reclamation/popUpRec.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Nouveau Ticket");
            stage.setScene(new Scene(root));
            stage.showAndWait();

            loadData();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openModifyModal(Reclamation r) {
        if (!"En attente".equals(r.getStatut())) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Modification impossible");
            alert.setHeaderText(null);
            alert.setContentText("Seules les réclamations avec le statut 'En attente' peuvent être modifiées.");
            alert.showAndWait();
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Reclamation/popUpmodif.fxml"));
            Parent root = loader.load();

            ReclamationController controller = loader.getController();
            controller.initData(r);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Modifier Ticket");
            stage.setScene(new Scene(root));
            stage.showAndWait();

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
            service.delete(r.getId());
            loadData();
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

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}