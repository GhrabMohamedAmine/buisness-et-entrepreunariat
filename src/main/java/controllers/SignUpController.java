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
    @FXML private Label firstNameError;
    @FXML private Label lastNameError;
    @FXML private Label emailError;
    @FXML private Label phoneError;
    @FXML private Label deptError;
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

    @FXML
    void handleSignUp(ActionEvent event) {
        // 1. Réinitialiser les erreurs précédentes
        clearErrors();
        boolean isValid = true; // On part du principe que c'est valide

        // Récupération des valeurs
        String nom = lastNameField.getText();
        String prenom = firstNameField.getText();
        String email = emailField.getText();
        String phone = phoneField.getText();
        String dept = deptField.getText();
        String mdp = passwordField.getText();

        String role = (roleComboBox.getValue() != null) ? roleComboBox.getValue() : "Employer";
        String dateInscription = LocalDate.now().toString();

        // --- VALIDATION NOM ---
        String nameRegex = "^[a-zA-ZÀ-ÿ\\s\\-]+$";
        if (nom.isEmpty()) {
            showInlineError(lastNameError, "Le nom est requis.");
            isValid = false;
        } else if (!nom.matches(nameRegex)) {
            showInlineError(lastNameError, "Lettres uniquement.");
            isValid = false;
        }

        // --- VALIDATION PRÉNOM ---
        if (prenom.isEmpty()) {
            showInlineError(firstNameError, "Le prénom est requis.");
            isValid = false;
        } else if (!prenom.matches(nameRegex)) {
            showInlineError(firstNameError, "Lettres uniquement.");
            isValid = false;
        }

        // --- VALIDATION EMAIL ---
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[a-zA-Z]{2,}$";
        if (email.isEmpty()) {
            showInlineError(emailError, "L'email est requis.");
            isValid = false;
        } else if (!email.matches(emailRegex)) {
            showInlineError(emailError, "Format email invalide.");
            isValid = false;
        }

        // --- VALIDATION TÉLÉPHONE ---
        String phoneRegex = "^[0-9\\s+]+$";
        if (phone.isEmpty()) {
            showInlineError(phoneError, "Le téléphone est requis.");
            isValid = false;
        } else if (!phone.matches(phoneRegex)) {
            showInlineError(phoneError, "Chiffres et '+' uniquement.");
            isValid = false;
        }

        // --- VALIDATION DÉPARTEMENT ---
        if (dept.isEmpty()) {
            showInlineError(deptError, "Le département est requis.");
            isValid = false;
        } else if (!dept.matches(nameRegex)) {
            showInlineError(deptError, "Lettres uniquement.");
            isValid = false;
        }

        // --- VALIDATION MOT DE PASSE (Optionnel, juste vide pour l'instant) ---
        if (mdp.isEmpty()) {
            showAlert("Erreur", "Le mot de passe est obligatoire."); // On garde le pop-up ou on ajoute un label pour le MDP aussi
            isValid = false;
        }

        // SI UNE ERREUR A ÉTÉ DÉTECTÉE, ON ARRÊTE TOUT ICI
        if (!isValid) {
            return;
        }

        // --- SUITE DU TRAITEMENT (Création User) ---
        if (imagePath == null) imagePath = "";

        User newUser = new User(0, nom, prenom, email, phone, role, dept, "Actif", dateInscription, imagePath);

        try {
            userService.signupAndLogin(newUser, mdp);
            switchScene(event, "/SignUp/SignUp2.fxml");
        } catch (SQLException e) {
            e.printStackTrace();
            // Ici on garde le pop-up car c'est une erreur système (base de données), pas une erreur de saisie
            showAlert("Erreur SQL", "Impossible de créer le compte : " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


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
    // Méthode pour afficher une erreur sous un champ spécifique
    private void showInlineError(Label errorLabel, String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true); // Le label reprend sa place
    }

    // Méthode pour tout nettoyer avant une nouvelle vérification
    private void clearErrors() {
        if(firstNameError != null) { firstNameError.setVisible(false); firstNameError.setManaged(false); }
        if(lastNameError != null) { lastNameError.setVisible(false); lastNameError.setManaged(false); }
        if(emailError != null) { emailError.setVisible(false); emailError.setManaged(false); }
        if(phoneError != null) { phoneError.setVisible(false); phoneError.setManaged(false); }
        if(deptError != null) { deptError.setVisible(false); deptError.setManaged(false); }
    }
}