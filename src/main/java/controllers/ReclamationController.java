package controllers;

import java.awt.*;
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
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;
import services.EmailService;
import services.ReclamationService;
import services.UserService;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.util.ResourceBundle;

public class ReclamationController implements Initializable {

    @FXML private Label topName;
    @FXML private Circle topAvatar;
    @FXML private TableView<Reclamation> reclamationTable;
    @FXML private TableColumn<Reclamation, String> colTitre, colCategorie, colProjet, colStatut, colDate;
    @FXML private TableColumn<Reclamation, Void> colReponse;
    @FXML private TextField titreField;
    @FXML private TextField categorieField;
    @FXML private TextField projetField;
    @FXML private ComboBox<String> statutCombo;

    // Éléments pour le fichier
    @FXML private Button btnChoisirFichier;
    @FXML private Label lblNomFichier;

    @FXML private Label titreError;
    @FXML private Label categorieError;
    @FXML private Label projetError;

    private ReclamationService reclamationService;
    private ObservableList<Reclamation> reclamationList;

    // Pour la modification
    private int idAModifier = 0;
    private File selectedFile;              // Fichier sélectionné
    private Reclamation currentReclamation; // Réclamation en cours de modification

    // =========================================================================
    // Initialisation
    // =========================================================================
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        reclamationService = new ReclamationService();
        if (topName != null) {
            loadCurrentUserProfile();
        }
        if (reclamationTable != null) {
            setupTable();
            loadData();
            // Ajout du double-clic pour ouvrir le fichier
            setupDoubleClick();
        }
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

    private void loadData() {
        reclamationList = reclamationService.getAll();
        reclamationTable.setItems(reclamationList);
    }

    private void setupTable() {
        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colProjet.setCellValueFactory(new PropertyValueFactory<>("projet"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));

        // Colonne CATÉGORIE avec badge
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

        // Colonne STATUT avec badge
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colStatut.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    badge.getStyleClass().addAll("status-pill");
                    switch (item) {
                        case "En attente" -> badge.getStyleClass().add("status-open");
                        case "En cours"   -> badge.getStyleClass().add("status-progress");
                        case "Rèsolu"     -> badge.getStyleClass().add("status-resolved");
                        case "Fermer"     -> badge.getStyleClass().add("status-closed");
                        default           -> badge.getStyleClass().add("status-closed");
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
                replyBtn.setTooltip(new Tooltip("Voir / Modifier"));
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

    /**
     * Active le double-clic sur une ligne pour ouvrir le fichier joint (s'il existe).
     */
    private void setupDoubleClick() {
        reclamationTable.setRowFactory(tv -> {
            TableRow<Reclamation> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    Reclamation rec = row.getItem();
                    openAttachedFile(rec);
                }
            });
            // Option : ajouter un tooltip pour indiquer la fonctionnalité
            row.hoverProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal && !row.isEmpty()) {
                    // On ne peut pas savoir s'il y a un fichier sans requête, on met un message générique
                    row.setTooltip(new Tooltip("Double-cliquer pour ouvrir la pièce jointe (si existante)"));
                }
            });
            return row;
        });
    }

    /**
     * Ouvre le fichier attaché à une réclamation.
     */
    private void openAttachedFile(Reclamation rec) {
        // Récupérer la réclamation complète (avec le fichier)
        Reclamation fullRec = reclamationService.getById(rec.getId());
        if (fullRec == null || fullRec.getFichier() == null || fullRec.getFichier().length == 0) {
            showAlert("Information", "Aucune pièce jointe pour cette réclamation.");
            return;
        }

        // Créer un fichier temporaire
        try {
            // Déterminer une extension approximative (on ne connaît pas le type original)
            // On peut essayer de deviner ou simplement mettre .tmp
            String suffix = ".tmp";
            // Option : on pourrait stocker le nom original du fichier dans une autre colonne.
            // Ici on utilise un nom générique.
            File tempFile = File.createTempFile("reclamation_" + rec.getId() + "_", suffix);
            tempFile.deleteOnExit();

            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(fullRec.getFichier());
            }

            // Ouvrir avec l'application par défaut
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(tempFile);
            } else {
                showAlert("Erreur", "L'ouverture de fichiers n'est pas supportée sur ce système.");
            }
        } catch (IOException e) {
            showAlert("Erreur", "Impossible d'ouvrir le fichier : " + e.getMessage());
        }
    }

    // =========================================================================
    // Navigation
    // =========================================================================
    public void handleUserManagementNavigation(ActionEvent event) {
        switchScene(event, "/User/UserTable.fxml");
    }

    public void handleLogout(ActionEvent event) {
        UserService.logout();
        switchScene(event, "/Start/1ere.fxml");
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

    // =========================================================================
    // Gestion des popups (Ajout / Modification)
    // =========================================================================
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

    public void initData(Reclamation r) {
        if (r != null) {
            this.idAModifier = r.getId();
            this.currentReclamation = reclamationService.getById(r.getId());
            titreField.setText(currentReclamation.getTitre());
            categorieField.setText(currentReclamation.getCategorie());
            projetField.setText(currentReclamation.getProjet());
            statutCombo.setValue(currentReclamation.getStatut());

            // Affichage du fichier existant
            if (currentReclamation.getFichier() != null && currentReclamation.getFichier().length > 0) {
                // Le nom original n'étant pas stocké, on utilise un affichage générique
                lblNomFichier.setText("Fichier existant");
                FontIcon icon = new FontIcon("mdi2f-file");
                icon.setIconSize(18);
                lblNomFichier.setGraphic(icon);
            } else {
                lblNomFichier.setText("Aucun fichier");
                lblNomFichier.setGraphic(null);
            }
            selectedFile = null;
        }
    }

    // =========================================================================
    // Sauvegarde (Ajout / Modification)
    // =========================================================================
    @FXML
    public void saveReclamation(ActionEvent event) {
        if (!validerSaisie()) return;

        String titre = titreField.getText().trim();
        String cat = categorieField.getText().trim();
        String proj = projetField.getText().trim();
        String statut = statutCombo.getValue();
        String date = LocalDate.now().toString();

        Reclamation r = new Reclamation(titre, cat, proj, statut, date);

        if (selectedFile != null) {
            try {
                byte[] fileBytes = Files.readAllBytes(selectedFile.toPath());
                r.setFichier(fileBytes);
            } catch (IOException e) {
                showAlert("Erreur", "Impossible de lire le fichier : " + e.getMessage());
                return;
            }
        }

        reclamationService.add(r);
        closePopup(event);
    }

    @FXML
    public void updateReclamation(ActionEvent event) {
        if (!validerSaisie()) return;

        String titre = titreField.getText().trim();
        String cat = categorieField.getText().trim();
        String proj = projetField.getText().trim();
        String statut = statutCombo.getValue();
        String date = LocalDate.now().toString();

        Reclamation r = new Reclamation(titre, cat, proj, statut, date);
        r.setId(idAModifier);

        if (selectedFile != null) {
            try {
                byte[] fileBytes = Files.readAllBytes(selectedFile.toPath());
                r.setFichier(fileBytes);
            } catch (IOException e) {
                showAlert("Erreur", "Impossible de lire le fichier : " + e.getMessage());
                return;
            }
        } else {
            // Conserver l'ancien fichier
            if (currentReclamation != null) {
                r.setFichier(currentReclamation.getFichier());
            }
        }

        // Vérifier si le statut a changé vers "Rèsolu" ou "Fermer"
        String oldStatus = currentReclamation != null ? currentReclamation.getStatut() : "";
        boolean statusChanged = !oldStatus.equals(statut);
        boolean shouldNotify = statusChanged && (statut.equals("Rèsolu") || statut.equals("Fermer"));

        // Mettre à jour en base
        reclamationService.update(r);

        // Envoyer un email si nécessaire
        if (shouldNotify) {
            User currentUser = UserService.getCurrentUser();
            if (currentUser != null && currentUser.getEmail() != null) {
                EmailService.sendReclamationStatusEmail(
                        currentUser.getEmail(),
                        r.getTitre(),
                        r.getProjet(),
                        r.getStatut()
                );
            } else {
                System.out.println("Impossible d'envoyer l'email : utilisateur non connecté ou email manquant.");
            }
        }

        closePopup(event);
    }

    @FXML
    public void closePopup(ActionEvent event) {
        ((Node) event.getSource()).getScene().getWindow().hide();
    }

    // =========================================================================
    // Gestion du fichier (choix)
    // =========================================================================
    @FXML
    private void choisirFichier() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une pièce jointe");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Tous les fichiers", "*.*"),
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"),
                new FileChooser.ExtensionFilter("PDF", "*.pdf"),
                new FileChooser.ExtensionFilter("Documents", "*.docx", "*.xlsx", "*.txt")
        );
        selectedFile = fileChooser.showOpenDialog(btnChoisirFichier.getScene().getWindow());
        updateFileDisplay(selectedFile);
    }

    // =========================================================================
    // Validation des champs
    // =========================================================================
    private void clearErrors() {
        if (titreError != null) { titreError.setVisible(false); titreError.setManaged(false); }
        if (categorieError != null) { categorieError.setVisible(false); categorieError.setManaged(false); }
        if (projetError != null) { projetError.setVisible(false); projetError.setManaged(false); }
    }

    private void showInlineError(Label label, String text) {
        if (label != null) {
            label.setText(text);
            label.setVisible(true);
            label.setManaged(true);
        }
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

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    /**
     * Met à jour l'affichage du label avec le nom du fichier et une icône adaptée.
     * @param file le fichier sélectionné (ou null pour réinitialiser)
     */
    private void updateFileDisplay(File file) {
        if (file != null) {
            String fileName = file.getName();
            lblNomFichier.setText(fileName);
            lblNomFichier.setGraphic(getFileIcon(fileName));
        } else {
            lblNomFichier.setText("Aucun fichier");
            lblNomFichier.setGraphic(null);
        }
    }

    /**
     * Retourne une icône FontIcon basée sur l'extension du fichier.
     * @param fileName nom du fichier
     * @return FontIcon correspondant
     */
    private FontIcon getFileIcon(String fileName) {
        String ext = "";
        int i = fileName.lastIndexOf('.');
        if (i > 0) {
            ext = fileName.substring(i + 1).toLowerCase();
        }
        String iconLiteral;
        switch (ext) {
            case "pdf":
                iconLiteral = "mdi2f-file-pdf";
                break;
            case "jpg":
            case "jpeg":
            case "png":
            case "gif":
            case "bmp":
                iconLiteral = "mdi2f-file-image";
                break;
            case "doc":
            case "docx":
                iconLiteral = "mdi2f-file-word";
                break;
            case "xls":
            case "xlsx":
                iconLiteral = "mdi2f-file-excel";
                break;
            case "txt":
                iconLiteral = "mdi2f-file-document";
                break;
            default:
                iconLiteral = "mdi2f-file";
                break;
        }
        FontIcon icon = new FontIcon(iconLiteral);
        icon.setIconSize(18);
        // Option : définir une couleur personnalisée si besoin
        // icon.setIconColor(javafx.scene.paint.Color.web("#6B7280"));
        return icon;
    }
}