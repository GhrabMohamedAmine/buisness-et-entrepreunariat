package controllers;

import entities.User;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.scene.Scene;
import services.UserService;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.Optional;
import java.util.ResourceBundle;

public class ProfileController implements Initializable {
    @FXML
    private StackPane contentArea;
    @FXML private Label topName;
    @FXML private Circle topAvatar;
    @FXML private Label lblErrorNom;
    @FXML private Label lblErrorPrenom;
    @FXML private Label lblErrorEmail;
    @FXML private Label lblErrorPhone;
    @FXML private Label lblErrorDept;
    @FXML private TextField tfNom;
    @FXML private TextField tfPrenom;
    @FXML private TextField tfEmail;
    @FXML private TextField tfTelephone;
    @FXML private TextField tfDepartement;
    @FXML private TextField tfRole;
    @FXML private TextField tfDateInscription;

    // --- Gestion de l'image ---
    @FXML private Circle profileCircle;
    private byte[] selectedImageData; // Stocke les nouvelles données binaires si changées

    private UserService userService;
    private User userConnecte;

    // Couleur de secours si l'image par défaut est introuvable
    private static final Color DEFAULT_COLOR = Color.web("#E0E7FF");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        userService = new UserService();
        userConnecte = UserService.getCurrentUser();

        if (tfNom != null && userConnecte != null) {
            remplirChamps();
        }
        else if (contentArea != null) {
            chargerVue("/Profile/Profile2.fxml");
        }
        setupTopProfile();
    }

    /**
     * Charge l'image par défaut depuis les ressources.
     * @return Image par défaut, ou null si non trouvée.
     */
    private Image getDefaultImage() {
        try (InputStream is = getClass().getResourceAsStream("/images/default-avatar.png")) {
            if (is != null) {
                return new Image(is);
            }
        } catch (Exception e) {
            System.err.println("Erreur chargement image par défaut : " + e.getMessage());
        }
        return null;
    }

    /**
     * Met à jour l'avatar du bandeau supérieur.
     */
    private void setupTopProfile() {
        User currentUser = UserService.getCurrentUser();

        if (currentUser != null) {
            if (topName != null) {
                String fullName = (currentUser.getFirstName() != null ? currentUser.getFirstName() : "")
                        + " " + (currentUser.getName() != null ? currentUser.getName() : "");
                topName.setText(fullName.trim());
            }

            if (topAvatar != null) {
                byte[] imageData = currentUser.getImageData();
                ImagePattern pattern = null;

                if (imageData != null && imageData.length > 0) {
                    try (ByteArrayInputStream bais = new ByteArrayInputStream(imageData)) {
                        Image img = new Image(bais, 32, 32, true, true);
                        if (!img.isError()) {
                            pattern = new ImagePattern(img);
                        }
                    } catch (Exception e) {
                        System.err.println("Erreur chargement image utilisateur : " + e.getMessage());
                    }
                }

                // Si pas d'image valide, essayer l'image par défaut
                if (pattern == null) {
                    Image defaultImg = getDefaultImage();
                    if (defaultImg != null) {
                        pattern = new ImagePattern(defaultImg);
                    } else {
                        // Fallback : couleur unie
                        topAvatar.setFill(DEFAULT_COLOR);
                        return;
                    }
                }
                topAvatar.setFill(pattern);
            }
        }
    }

    private void chargerVue(String fxmlPath) {
        try {
            Parent fxml = FXMLLoader.load(getClass().getResource(fxmlPath));
            contentArea.getChildren().setAll(fxml);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void remplirChamps() {
        tfNom.setText(userConnecte.getName());
        tfPrenom.setText(userConnecte.getFirstName());
        tfEmail.setText(userConnecte.getEmail());
        tfTelephone.setText(userConnecte.getPhone());
        tfDepartement.setText(userConnecte.getDepartment());
        tfRole.setText(userConnecte.getRole());
        tfDateInscription.setText(userConnecte.getJoinedDate());

        // Chargement de l'image du profil (cercle principal)
        byte[] imageData = userConnecte.getImageData();
        ImagePattern pattern = null;

        if (imageData != null && imageData.length > 0) {
            try (ByteArrayInputStream bais = new ByteArrayInputStream(imageData)) {
                Image img = new Image(bais);
                if (!img.isError()) {
                    pattern = new ImagePattern(img);
                }
            } catch (Exception e) {
                System.out.println("Erreur chargement image : " + e.getMessage());
            }
        }

        // Si pas d'image valide, essayer l'image par défaut
        if (pattern == null) {
            Image defaultImg = getDefaultImage();
            if (defaultImg != null) {
                pattern = new ImagePattern(defaultImg);
            } else {
                profileCircle.setFill(DEFAULT_COLOR);
                return;
            }
        }
        profileCircle.setFill(pattern);
    }

    // --- ACTION : Importer une nouvelle image ---
    @FXML
    public void importerImage(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une image de profil");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg")
        );

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            try {
                byte[] fileBytes = Files.readAllBytes(selectedFile.toPath());
                selectedImageData = fileBytes;

                // Mettre à jour l'aperçu immédiatement
                Image image = new Image(new ByteArrayInputStream(fileBytes));
                profileCircle.setFill(new ImagePattern(image));
            } catch (IOException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de lire le fichier : " + e.getMessage());
            }
        }
    }

    // --- ACTION : Sauvegarder ---
    @FXML
    private void sauvegarderModifications() {
        clearErrors();
        boolean isValid = true;

        String nom = tfNom.getText();
        String prenom = tfPrenom.getText();
        String email = tfEmail.getText();
        String phone = tfTelephone.getText();
        String dept = tfDepartement.getText();

        String nameRegex = "^[a-zA-ZÀ-ÿ\\s\\-]+$";

        if (nom.isEmpty()) {
            showInlineError(lblErrorNom, "Le nom est requis.");
            isValid = false;
        } else if (!nom.matches(nameRegex)) {
            showInlineError(lblErrorNom, "Lettres uniquement.");
            isValid = false;
        }

        if (prenom.isEmpty()) {
            showInlineError(lblErrorPrenom, "Le prénom est requis.");
            isValid = false;
        } else if (!prenom.matches(nameRegex)) {
            showInlineError(lblErrorPrenom, "Lettres uniquement.");
            isValid = false;
        }

        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[a-zA-Z]{2,}$";
        if (email.isEmpty()) {
            showInlineError(lblErrorEmail, "L'email est requis.");
            isValid = false;
        } else if (!email.matches(emailRegex)) {
            showInlineError(lblErrorEmail, "Format email invalide.");
            isValid = false;
        }

        String phoneRegex = "^[0-9\\s+]+$";
        if (phone.isEmpty()) {
            showInlineError(lblErrorPhone, "Le téléphone est requis.");
            isValid = false;
        } else if (!phone.matches(phoneRegex)) {
            showInlineError(lblErrorPhone, "Chiffres et '+' uniquement.");
            isValid = false;
        }

        if (dept.isEmpty()) {
            showInlineError(lblErrorDept, "Le département est requis.");
            isValid = false;
        } else if (!dept.matches(nameRegex)) {
            showInlineError(lblErrorDept, "Lettres uniquement.");
            isValid = false;
        }

        if (!isValid) {
            return;
        }

        try {
            userConnecte.setName(nom);
            userConnecte.setFirstName(prenom);
            userConnecte.setEmail(email);
            userConnecte.setPhone(phone);
            userConnecte.setDepartment(dept);

            if (selectedImageData != null) {
                userConnecte.setImageData(selectedImageData);
            }

            userService.modifierProfil(userConnecte);

            showAlert(Alert.AlertType.INFORMATION, "Succès", "Profil mis à jour avec succès.");
            setupTopProfile();

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur BDD", "Impossible de modifier le profil : " + e.getMessage());
        }
    }

    // --- ACTION : Supprimer Compte ---
    @FXML
    private void supprimerCompte() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Suppression de compte");
        alert.setHeaderText("Êtes-vous sûr de vouloir supprimer votre compte ?");
        alert.setContentText("Cette action est irréversible.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                userService.supprimerCompte(userConnecte.getId());
                showAlert(Alert.AlertType.INFORMATION, "Compte supprimé", "Votre compte a été supprimé. Au revoir.");

                try {
                    Parent root = FXMLLoader.load(getClass().getResource("/Start/1ere.fxml"));
                    Stage stage;
                    if (contentArea != null) stage = (Stage) contentArea.getScene().getWindow();
                    else stage = (Stage) tfNom.getScene().getWindow();
                    stage.setScene(new Scene(root));
                    stage.show();
                } catch (IOException e) {
                    e.printStackTrace();
                    showAlert(Alert.AlertType.ERROR, "Erreur Navigation", "Impossible de trouver /Start/1ere.fxml");
                }

            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Echec de la suppression : " + e.getMessage());
            }
        }
    }

    // --- NAVIGATION SIDEBAR ---
    @FXML
    public void showProfile(ActionEvent event) {
        chargerVue("/Profile/Profile2.fxml");
    }

    @FXML
    public void showNotifications(ActionEvent event) {
        System.out.println("Notifications clicked");
    }

    @FXML
    public void showSecurity(ActionEvent event) {
        System.out.println("Security clicked");
    }

    @FXML
    public void logout(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/SignUp/SignUp1.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public void handleRec(ActionEvent event) {
        switchScene(event, "/UserReclamations/UserReclamations.fxml");
    }

    private void switchScene(ActionEvent event, String fxmlPath) {
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

    private void showInlineError(Label label, String text) {
        label.setText(text);
        label.setVisible(true);
        label.setManaged(true);
    }

    private void clearErrors() {
        if (lblErrorNom != null) { lblErrorNom.setVisible(false); lblErrorNom.setManaged(false); }
        if (lblErrorPrenom != null) { lblErrorPrenom.setVisible(false); lblErrorPrenom.setManaged(false); }
        if (lblErrorEmail != null) { lblErrorEmail.setVisible(false); lblErrorEmail.setManaged(false); }
        if (lblErrorPhone != null) { lblErrorPhone.setVisible(false); lblErrorPhone.setManaged(false); }
        if (lblErrorDept != null) { lblErrorDept.setVisible(false); lblErrorDept.setManaged(false); }
    }
}