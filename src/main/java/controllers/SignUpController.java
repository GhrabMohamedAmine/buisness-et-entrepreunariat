package controllers;

import entities.User;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import services.UserService;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ResourceBundle;

public class SignUpController implements Initializable {

    // --- Champs existants ---
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private TextField deptField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<String> roleComboBox;

    // --- NOUVEAUX CHAMPS POUR L'IMAGE ---
    @FXML private Circle profileCircle; // Lié au fx:id="profileCircle" dans le FXML
    private String imagePath; // Pour stocker le chemin (ex: file:/C:/Images/photo.png)

    private final UserService userService = new UserService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Initialisation des rôles
        if (roleComboBox != null) {
            roleComboBox.setItems(FXCollections.observableArrayList(
                     "Manager", "Ressource Manager", "Employer",
                    "Formateur", "Chef Projet", "Expert Financier"
            ));
        }
    }

    // --- NOUVELLE MÉTHODE : GESTION DE L'UPLOAD ---
    @FXML
    void handleImageUpload(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une photo de profil");

        // Filtre pour ne montrer que les images
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg")
        );

        // Récupérer la fenêtre actuelle pour afficher le dialogue
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            // 1. Sauvegarder le chemin pour la base de données
            // toURI().toString() convertit le chemin système (C:\...) en format URL compatible JavaFX (file:/C:/...)
            imagePath = selectedFile.toURI().toString();

            // 2. Afficher l'aperçu dans le cercle
            if (profileCircle != null) {
                profileCircle.setFill(new ImagePattern(new Image(imagePath)));
            }
        }
    }

    // --- MÉTHODE MISE À JOUR : INSCRIPTION ---
    @FXML
    void handleSignUp(ActionEvent event) {
        // Récupération des valeurs
        String nom = lastNameField.getText();
        String prenom = firstNameField.getText();
        String email = emailField.getText();
        String phone = phoneField.getText();
        // Valeur par défaut si null
        String role = (roleComboBox.getValue() != null) ? roleComboBox.getValue() : "Employer";
        String dept = deptField.getText();
        String mdp = passwordField.getText();
        String dateInscription = LocalDate.now().toString();

        // Validation simple
        if (nom.isEmpty() || prenom.isEmpty() || email.isEmpty() || mdp.isEmpty()) {
            showAlert("Champs manquants", "Veuillez remplir au moins le nom, prénom, email et mot de passe.");
            return;
        }

        // Gestion de l'image par défaut si l'utilisateur n'en a pas choisi
        if (imagePath == null) {
            imagePath = "";
        }

        // Création de l'utilisateur avec le NOUVEAU constructeur (incluant imagePath à la fin)
        User newUser = new User(
                0,              // ID (auto-incrémenté en DB)
                nom,
                prenom,
                email,
                phone,
                role,
                dept,
                "Actif",        // Statut par défaut
                dateInscription,
                imagePath       // Le lien de l'image
        );

        try {
            // Appel au service pour sauvegarder
            userService.signupAndLogin(newUser, mdp);

            // Succès
            System.out.println("Succès : Utilisateur " + email + " inscrit avec l'image : " + imagePath);

            // Redirection vers la suite (ex: SignUp2 ou Dashboard)
            switchScene(event, "/SignUp/SignUp2.fxml");

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Erreur SQL", "Impossible de créer le compte : " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Une erreur inattendue est survenue.");
        }
    }

    // --- Navigation et Utilitaires ---

    @FXML
    void handleBack(ActionEvent event) {
        switchScene(event, "/Start/1ere.fxml"); // Retour à la page d'accueil ou login
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
    private void switchScen(MouseEvent event, String fxmlPath) {
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

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public void handleBack1(ActionEvent event) {
        userService.logout();
        switchScene(event, "/SignUp/SignUp1.fxml");
    }

    public void handleNext2(ActionEvent event) {
        switchScene(event, "/Profile/Profile1.fxml");
    }

    public void handleNext(ActionEvent event) {
        switchScene(event, "/SignUp/SignUp3.fxml");
    }

    public void goToProjects(MouseEvent event) {
        switchScen(event, "/Profile/Profile1.fxml");
    }

    public void goToTrainings(MouseEvent event) {
        switchScen(event, "/Profile/Profile1.fxml");
    }


    public void goToDashboard(MouseEvent event) {
        switchScen(event, "/Profile/Profile1.fxml");
    }
}