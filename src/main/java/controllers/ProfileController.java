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

    // --- Elements venant de Profile1.fxml (Le conteneur principal) ---
    @FXML
    private StackPane contentArea;
    @FXML private Label topName;
    @FXML private Circle topAvatar;

    // --- Elements venant de Profile2.fxml (Le formulaire) ---
    @FXML private TextField tfNom;
    @FXML private TextField tfPrenom;
    @FXML private TextField tfEmail;
    @FXML private TextField tfTelephone;
    @FXML private TextField tfDepartement;
    @FXML private TextField tfRole;
    @FXML private TextField tfDateInscription;

    // --- Gestion de l'image ---
    @FXML private Circle profileCircle; // Lié au cercle dans le FXML
    private String selectedImagePath;   // Stocke le chemin de la nouvelle image

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
        try {
            // Mise à jour de l'objet User
            userConnecte.setName(tfNom.getText());
            userConnecte.setFirstName(tfPrenom.getText());
            userConnecte.setEmail(tfEmail.getText());
            userConnecte.setPhone(tfTelephone.getText());
            userConnecte.setDepartment(tfDepartement.getText());

            // Sauvegarde de l'image si elle a changé
            if (selectedImagePath != null) {
                userConnecte.setImageLink(selectedImagePath);
            }

            // Appel au service BDD
            userService.modifierProfil(userConnecte);
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Profil mis à jour avec succès.");

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
}