package controllers;

import entities.User;
import services.EmailService;
import services.UserService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.ResourceBundle;

public class ForgotPasswordController implements Initializable {

    // Champs FXML pour chaque étape
    @FXML private TextField emailField;
    @FXML private TextField codeField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    // Labels d'erreur (optionnels)
    @FXML private Label emailError;
    @FXML private Label codeError;
    @FXML private Label passwordError;

    // Service
    private final UserService userService = new UserService();

    // Gestion de session statique pour partager les données entre les étapes
    private static class ResetSession {
        static String email;
        static String code;
        static LocalDateTime expiry;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialisation si nécessaire
    }

    // ======================== Navigation ========================

    @FXML
    private void backToSignIn(ActionEvent event) {
        switchSceneFromEvent(event, "/SignIn/SignIn.fxml");
    }

    @FXML
    private void backToStep1(ActionEvent event) {
        switchSceneFromEvent(event, "/SignIn/ForgotPassword_Step1.fxml");
    }

    // Méthode utilitaire pour changer de scène à partir d'un événement
    private void switchSceneFromEvent(ActionEvent event, String fxmlPath) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ======================== Étape 1 : Envoi du code ========================

    @FXML
    private void handleSendCode(ActionEvent event) {
        String email = emailField.getText().trim();
        if (email.isEmpty()) {
            showAlert("Erreur", "Veuillez saisir votre adresse email.");
            return;
        }
        if (!email.matches("^[\\w.-]+@[\\w.-]+\\.[a-z]{2,}$")) {
            showAlert("Erreur", "Format d'email invalide.");
            return;
        }

        // Vérifier si l'email existe dans la base
        try {
            User user = userService.getUserByEmail(email);
            if (user == null) {
                showAlert("Erreur", "Aucun compte associé à cette adresse email.");
                return;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Erreur", "Problème de connexion à la base de données.");
            return;
        }

        // Générer un code à 6 chiffres
        String code = String.format("%06d", new Random().nextInt(999999));
        ResetSession.email = email;
        ResetSession.code = code;
        ResetSession.expiry = LocalDateTime.now().plusMinutes(10);

        // Envoyer l'email
        EmailService.sendPasswordResetCode(email, code);

        // Passer à l'étape 2
        switchSceneFromEvent(event, "/SignIn/ForgotPassword_Step2.fxml");
    }

    // ======================== Étape 2 : Vérification du code ========================

    @FXML
    private void handleVerifyCode(ActionEvent event) {
        String inputCode = codeField.getText().trim();
        if (inputCode.isEmpty()) {
            showAlert("Erreur", "Veuillez saisir le code de vérification.");
            return;
        }

        // Vérifier si la session est encore valide
        if (ResetSession.code == null || ResetSession.expiry == null) {
            showAlert("Erreur", "Aucune demande de réinitialisation en cours. Veuillez recommencer.");
            switchSceneFromEvent(event, "/SignIn/ForgotPassword_Step1.fxml");
            return;
        }

        if (LocalDateTime.now().isAfter(ResetSession.expiry)) {
            showAlert("Erreur", "Le code a expiré. Veuillez demander un nouveau code.");
            ResetSession.code = null;
            ResetSession.expiry = null;
            switchSceneFromEvent(event, "/SignIn/ForgotPassword_Step1.fxml");
            return;
        }

        if (!inputCode.equals(ResetSession.code)) {
            showAlert("Erreur", "Code incorrect. Veuillez réessayer.");
            return;
        }

        // Code valide, passer à l'étape 3
        switchSceneFromEvent(event, "/SignIn/ForgotPassword_Step3.fxml");
    }

    @FXML
    private void handleResendCode(ActionEvent event) {
        if (ResetSession.email == null) {
            showAlert("Erreur", "Aucune demande en cours. Veuillez recommencer.");
            switchSceneFromEvent(event, "/SignIn/ForgotPassword_Step1.fxml");
            return;
        }

        // Générer un nouveau code
        String code = String.format("%06d", new Random().nextInt(999999));
        ResetSession.code = code;
        ResetSession.expiry = LocalDateTime.now().plusMinutes(10);

        // Renvoyer l'email
        EmailService.sendPasswordResetCode(ResetSession.email, code);

        showAlert("Info", "Un nouveau code vous a été envoyé par email.");
    }

    // ======================== Étape 3 : Nouveau mot de passe ========================

    @FXML
    private void handleResetPassword(ActionEvent event) {
        String newPass = newPasswordField.getText();
        String confirmPass = confirmPasswordField.getText();

        if (newPass.isEmpty() || confirmPass.isEmpty()) {
            showAlert("Erreur", "Veuillez remplir tous les champs.");
            return;
        }

        if (newPass.length() < 6) {
            showAlert("Erreur", "Le mot de passe doit contenir au moins 6 caractères.");
            return;
        }

        if (!newPass.equals(confirmPass)) {
            showAlert("Erreur", "Les mots de passe ne correspondent pas.");
            return;
        }

        // Mettre à jour le mot de passe dans la base
        try {
            User user = userService.getUserByEmail(ResetSession.email);
            if (user == null) {
                showAlert("Erreur", "Utilisateur introuvable.");
                return;
            }

            userService.updatePassword(user.getId(), newPass);

            // Nettoyer la session
            ResetSession.email = null;
            ResetSession.code = null;
            ResetSession.expiry = null;

            // Passer à l'étape 4 (succès)
            switchSceneFromEvent(event, "/SignIn/ForgotPassword_Step4.fxml");

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de mettre à jour le mot de passe.");
        }
    }

    // ======================== Utilitaires ========================

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}