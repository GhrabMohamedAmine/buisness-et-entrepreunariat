package controllers;

import java.time.LocalDate;
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
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;
import services.ReclamationService;
import services.UserService;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class ReclamationController implements Initializable {

    @FXML private Label topName;
    @FXML private Circle topAvatar;
    @FXML
    private TableView<Reclamation> reclamationTable;
    @FXML
    private TableColumn<Reclamation, String> colTitre, colCategorie, colProjet, colStatut, colDate;
    @FXML
    private TableColumn<Reclamation, Void> colReponse;
    @FXML private TextField titreField;
    @FXML private TextField categorieField;
    @FXML private TextField projetField;
    @FXML private ComboBox<String> statutCombo;

    @FXML private Label titreError;
    @FXML private Label categorieError;
    @FXML private Label projetError;

    // Ajout du service
    private ReclamationService reclamationService;
    private ObservableList<Reclamation> reclamationList;

    private void loadCurrentUserProfile() {
        User currentUser = UserService.getCurrentUser();

        if (currentUser != null) {
            // 1. Mise à jour du nom
            String fullName = currentUser.getFirstName() + " " + currentUser.getName();
            topName.setText(fullName);

            // 2. Mise à jour de l'avatar avec les données binaires
            byte[] imageData = currentUser.getImageData();
            if (imageData != null && imageData.length > 0) {
                try (ByteArrayInputStream bais = new ByteArrayInputStream(imageData)) {
                    Image img = new Image(bais, 32, 32, true, true);
                    if (!img.isError()) {
                        topAvatar.setFill(new ImagePattern(img));
                    } else {
                        topAvatar.setFill(javafx.scene.paint.Color.web("#E0E7FF"));
                    }
                } catch (Exception e) {
                    System.out.println("Erreur chargement image profil : " + e.getMessage());
                    topAvatar.setFill(javafx.scene.paint.Color.web("#E0E7FF"));
                }
            } else {
                topAvatar.setFill(javafx.scene.paint.Color.web("#E0E7FF"));
            }
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        reclamationService = new ReclamationService();
        if (topName != null) {
            loadCurrentUserProfile();
        }
        if (reclamationTable != null) {
            setupTable();
            loadData();
        }
    }

    private void loadData() {
        reclamationList = reclamationService.getAll();
        reclamationTable.setItems(reclamationList);
    }

    private void setupTable() {
        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colProjet.setCellValueFactory(new PropertyValueFactory<>("projet"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));

        // Colonne CATÉGORIE
        colCategorie.setCellValueFactory(new PropertyValueFactory<>("categorie"));
        colCategorie.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    badge.getStyleClass().add("category-pill");
                    setGraphic(badge);
                }
            }
        });

        // Colonne STATUT
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colStatut.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    badge.getStyleClass().add("status-pill");
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

        // Colonne RÉPONSE (boutons)
        colReponse.setCellFactory(column -> new TableCell<>() {
            private final Button replyBtn = new Button();
            private final Button deleteBtn = new Button();

            {
                FontIcon replyIcon = new FontIcon("mdi2m-message-processing-outline");
                replyIcon.setIconSize(18);
                replyBtn.setGraphic(replyIcon);
                replyBtn.getStyleClass().add("btn-response");
                replyBtn.setTooltip(new Tooltip("Voir la réponse"));
                replyBtn.setOnAction(event -> {
                    Reclamation rec = getTableView().getItems().get(getIndex());
                    openModifyPopup(rec);
                });

                FontIcon deleteIcon = new FontIcon("mdi2t-trash-can-outline");
                deleteIcon.setIconSize(18);
                deleteBtn.setGraphic(deleteIcon);
                deleteBtn.getStyleClass().addAll("action-btn", "action-btn-delete");
                deleteBtn.setTooltip(new Tooltip("Supprimer"));
                deleteBtn.setOnAction(event -> {
                    Reclamation rec = getTableView().getItems().get(getIndex());
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("Confirmation");
                    alert.setHeaderText(null);
                    alert.setContentText("Voulez-vous vraiment supprimer : " + rec.getTitre() + " ?");
                    if (alert.showAndWait().get() == ButtonType.OK) {
                        reclamationService.delete(rec.getId());
                        getTableView().getItems().remove(rec);
                    }
                });
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

            popupStage.showAndWait();

            if (reclamationTable != null) {
                loadData();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void saveReclamation(ActionEvent event) {
        if (!validerSaisie()) {
            return;
        }
        String titre = titreField.getText();
        String cat = categorieField.getText();
        String proj = projetField.getText();
        String statut = statutCombo.getValue();

        String date = LocalDate.now().toString();

        Reclamation r = new Reclamation(titre, cat, proj, statut, date);
        reclamationService.add(r);

        System.out.println("Reclamation ajoutée : " + r.getTitre());
        closePopup(event);
    }

    @FXML
    public void closePopup(ActionEvent event) {
        ((Node) event.getSource()).getScene().getWindow().hide();
    }

    private int idAModifier = 0;

    public void initData(Reclamation r) {
        if (r != null) {
            this.idAModifier = r.getId();
            titreField.setText(r.getTitre());
            categorieField.setText(r.getCategorie());
            projetField.setText(r.getProjet());
            statutCombo.setValue(r.getStatut());
        }
    }

    @FXML
    public void updateReclamation(ActionEvent event) {
        if (!validerSaisie()) {
            return;
        }
        String titre = titreField.getText();
        String cat = categorieField.getText();
        String proj = projetField.getText();
        String statut = statutCombo.getValue();
        String date = java.time.LocalDate.now().toString();

        Reclamation r = new Reclamation(titre, cat, proj, statut, date);
        r.setId(idAModifier);

        reclamationService.update(r);

        closePopup(event);
    }

    private void openModifyPopup(Reclamation rec) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Reclamation/popUpmodif.fxml"));
            Parent root = loader.load();

            ReclamationController controller = loader.getController();
            controller.initData(rec);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Modifier Réclamation");
            stage.setScene(new Scene(root));
            stage.showAndWait();

            loadData();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showInlineError(Label label, String text) {
        if (label != null) {
            label.setText(text);
            label.setVisible(true);
            label.setManaged(true);
        }
    }

    private void clearErrors() {
        if (titreError != null) { titreError.setVisible(false); titreError.setManaged(false); }
        if (categorieError != null) { categorieError.setVisible(false); categorieError.setManaged(false); }
        if (projetError != null) { projetError.setVisible(false); projetError.setManaged(false); }
    }

    private boolean validerSaisie() {
        clearErrors();
        boolean isValid = true;

        String titre = titreField.getText();
        String cat = categorieField.getText();
        String projet = projetField.getText();

        if (titre == null || titre.trim().isEmpty()) {
            showInlineError(titreError, "Le titre est obligatoire.");
            isValid = false;
        } else if (titre.length() < 3) {
            showInlineError(titreError, "Le titre est trop court.");
            isValid = false;
        }

        if (cat == null || cat.trim().isEmpty()) {
            showInlineError(categorieError, "La catégorie est requise.");
            isValid = false;
        } else if (!cat.matches("^[a-zA-ZÀ-ÿ\\s]+$")) {
            showInlineError(categorieError, "Lettres uniquement.");
            isValid = false;
        }

        if (projet == null || projet.trim().isEmpty()) {
            showInlineError(projetError, "Le projet est requis.");
            isValid = false;
        } else if (!projet.matches("^[a-zA-Z0-9\\s\\-]+$")) {
            showInlineError(projetError, "Caractères non autorisés.");
            isValid = false;
        }

        return isValid;
    }
}