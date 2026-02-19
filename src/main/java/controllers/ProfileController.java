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
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.scene.Scene;
import services.UserService;

import java.io.File;
import java.io.IOException;
import java.net.URL;
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
    private String selectedImagePath;

    private UserService userService;
    private User userConnecte;

    private void setupTopProfile() {
        // 1. Get current user
        User currentUser = UserService.getCurrentUser();

        if (currentUser != null) {
            // 2. Update Name ONLY if the label exists (is not null)
            if (topName != null) {
                String fullName = (currentUser.getFirstName() != null ? currentUser.getFirstName() : "")
                        + " " + (currentUser.getName() != null ? currentUser.getName() : "");
                topName.setText(fullName.trim());
            }

            // 3. Update Avatar ONLY if the circle exists (is not null)
            if (topAvatar != null) {
                String imagePath = currentUser.getImageLink();
                if (imagePath != null && !imagePath.isEmpty()) {
                    try {
                        Image img = new Image(imagePath, 32, 32, true, true);
                        if (!img.isError()) {
                            topAvatar.setFill(new ImagePattern(img));
                        }
                    } catch (Exception e) {
                        System.err.println("Error loading top profile image: " + e.getMessage());
                    }
                }
            }
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        userService = new UserService();
        userConnecte = UserService.getCurrentUser();

        // CAS 1 : On est sur le formulaire (Profile2) -> On remplit les champs
        if (tfNom != null && userConnecte != null) {
            remplirChamps();
        }
        // CAS 2 : On est sur la barre latérale (Profile1) -> On charge le formulaire au centre
        else if (contentArea != null) {
            chargerVue("/Profile/Profile2.fxml");
        }
        setupTopProfile();
    }

    // Méthode utilitaire pour charger une vue dans le contentArea
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

        // Chargement de l'image existante
        String imagePath = userConnecte.getImageLink();
        if (imagePath != null && !imagePath.isEmpty()) {
            try {
                // Puisque c'est sauvegardé en URI, on peut le charger directement
                Image image = new Image(imagePath, false);
                profileCircle.setFill(new ImagePattern(image));
                selectedImagePath = imagePath;
            } catch (Exception e) {
                System.out.println("Erreur chargement image : " + e.getMessage());
            }
        }
    }

    private void chargerImageDansCercle(String path) {
        try {
            Image image;
            // Vérifie si c'est un fichier local ou une URL
            File file = new File(path);
            if (file.exists()) {
                image = new Image(file.toURI().toString());
            } else {
                image = new Image(path); // Essai comme URL web ou resource
            }
            profileCircle.setFill(new ImagePattern(image));
        } catch (Exception e) {
            System.out.println("Erreur chargement image : " + e.getMessage());
        }
    }

    // --- ACTION : Importer une nouvelle image ---
    @FXML
    public void importerImage(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une image de profil");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg")
        );

        // Récupérer la fenêtre actuelle pour afficher le dialogue
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            selectedImagePath = selectedFile.toURI().toString(); // On sauvegarde le chemin absolu

            // On met à jour l'aperçu visuel immédiatement
            Image image = new Image(selectedFile.toURI().toString());
            profileCircle.setFill(new ImagePattern(image));
        }
    }

    // --- ACTION : Sauvegarder ---
    @FXML
    private void sauvegarderModifications() {
        // 1. Nettoyer les erreurs précédentes
        clearErrors();
        boolean isValid = true;

        // 2. Récupérer les valeurs
        String nom = tfNom.getText();
        String prenom = tfPrenom.getText();
        String email = tfEmail.getText();
        String phone = tfTelephone.getText();
        String dept = tfDepartement.getText();

        // --- VALIDATION (REGEX) ---

        // Regex pour Nom, Prénom, Département (Lettres, espaces, tirets)
        String nameRegex = "^[a-zA-ZÀ-ÿ\\s\\-]+$";

        // Nom
        if (nom.isEmpty()) {
            showInlineError(lblErrorNom, "Le nom est requis.");
            isValid = false;
        } else if (!nom.matches(nameRegex)) {
            showInlineError(lblErrorNom, "Lettres uniquement.");
            isValid = false;
        }

        // Prénom
        if (prenom.isEmpty()) {
            showInlineError(lblErrorPrenom, "Le prénom est requis.");
            isValid = false;
        } else if (!prenom.matches(nameRegex)) {
            showInlineError(lblErrorPrenom, "Lettres uniquement.");
            isValid = false;
        }

        // Email
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[a-zA-Z]{2,}$";
        if (email.isEmpty()) {
            showInlineError(lblErrorEmail, "L'email est requis.");
            isValid = false;
        } else if (!email.matches(emailRegex)) {
            showInlineError(lblErrorEmail, "Format email invalide.");
            isValid = false;
        }

        // Téléphone (Chiffres, espaces, +)
        String phoneRegex = "^[0-9\\s+]+$";
        if (phone.isEmpty()) {
            showInlineError(lblErrorPhone, "Le téléphone est requis.");
            isValid = false;
        } else if (!phone.matches(phoneRegex)) {
            showInlineError(lblErrorPhone, "Chiffres et '+' uniquement.");
            isValid = false;
        }

        // Département
        if (dept.isEmpty()) {
            showInlineError(lblErrorDept, "Le département est requis.");
            isValid = false;
        } else if (!dept.matches(nameRegex)) {
            showInlineError(lblErrorDept, "Lettres uniquement.");
            isValid = false;
        }

        // Si une erreur est détectée, on arrête ici
        if (!isValid) {
            return;
        }

        // --- SAUVEGARDE EN BASE DE DONNÉES ---
        try {
            // Mise à jour de l'objet User localement
            userConnecte.setName(nom);
            userConnecte.setFirstName(prenom);
            userConnecte.setEmail(email);
            userConnecte.setPhone(phone);
            userConnecte.setDepartment(dept);

            // Mise à jour de l'image si changée
            if (selectedImagePath != null) {
                userConnecte.setImageLink(selectedImagePath);
            }

            // Appel au service BDD
            userService.modifierProfil(userConnecte); // [cite: 58]

            // Succès
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Profil mis à jour avec succès.");

            // Mise à jour optionnelle de l'affichage du haut (Header) si présent
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
                // 1. Suppression BDD
                userService.supprimerCompte(userConnecte.getId());
                showAlert(Alert.AlertType.INFORMATION, "Compte supprimé", "Votre compte a été supprimé. Au revoir.");

                // 2. Redirection vers 1ere.fxml dans le dossier Start
                try {
                    Parent root = FXMLLoader.load(getClass().getResource("/Start/1ere.fxml"));

                    // On récupère le Stage actuel (peu importe le composant source)
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

    // --- NAVIGATION SIDEBAR (Profile1.fxml) ---

    @FXML
    public void showProfile(ActionEvent event) {
        chargerVue("/Profile/Profile2.fxml");
    }

    @FXML
    public void showNotifications(ActionEvent event) {
        // chargerVue("/Profile/Notifications.fxml"); // À créer si besoin
        System.out.println("Notifications clicked");
    }

    @FXML
    public void showSecurity(ActionEvent event) {
        // chargerVue("/Profile/Security.fxml"); // À créer si besoin
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
    // Affiche le message d'erreur et rend le label visible
    private void showInlineError(Label label, String text) {
        label.setText(text);
        label.setVisible(true);
        label.setManaged(true);
    }

    // Cache toutes les erreurs
    private void clearErrors() {
        if(lblErrorNom != null) { lblErrorNom.setVisible(false); lblErrorNom.setManaged(false); }
        if(lblErrorPrenom != null) { lblErrorPrenom.setVisible(false); lblErrorPrenom.setManaged(false); }
        if(lblErrorEmail != null) { lblErrorEmail.setVisible(false); lblErrorEmail.setManaged(false); }
        if(lblErrorPhone != null) { lblErrorPhone.setVisible(false); lblErrorPhone.setManaged(false); }
        if(lblErrorDept != null) { lblErrorDept.setVisible(false); lblErrorDept.setManaged(false); }
    }
}