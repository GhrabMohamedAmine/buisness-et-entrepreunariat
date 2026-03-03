package controllers;

import entities.User;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import services.EmailService;
import services.UserService;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ResourceBundle;

public class InviteUserController implements Initializable {

    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField emailField;
    @FXML private ComboBox<String> roleCombo;
    @FXML private TextField departmentField;
    @FXML private PasswordField passwordField;

    private final UserService userService = new UserService();
    private Runnable onSaveCallback; // pour rafraîchir la table après ajout

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Liste des rôles (identique à celle de UserTableController)
        roleCombo.setItems(FXCollections.observableArrayList(
                "Admin",
                "Manager",
                "Ressource Manager",
                "Employer",
                "Formateur",
                "Chef Projet",
                "Expert Financier"
        ));
    }

    @FXML
    private void handleSave() {
        // Validation
        if (firstNameField.getText().trim().isEmpty() ||
                lastNameField.getText().trim().isEmpty() ||
                emailField.getText().trim().isEmpty() ||
                roleCombo.getValue() == null ||
                departmentField.getText().trim().isEmpty() ||
                passwordField.getText().trim().isEmpty()) {
            showAlert("Erreur", "Tous les champs sont obligatoires.");
            return;
        }

        String email = emailField.getText().trim();
        if (!email.matches("^[\\w.-]+@[\\w.-]+\\.[a-z]{2,}$")) {
            showAlert("Erreur", "Format d'email invalide.");
            return;
        }

        String password = passwordField.getText().trim();
        if (password.length() < 6) {
            showAlert("Erreur", "Le mot de passe doit contenir au moins 6 caractères.");
            return;
        }

        // Création de l'utilisateur
        User user = new User(
                0, // id temporaire
                lastNameField.getText().trim(),
                firstNameField.getText().trim(),
                email,
                "", // téléphone non renseigné
                roleCombo.getValue(),
                departmentField.getText().trim(),
                "Actif", // statut par défaut
                LocalDate.now().toString(),
                null, // pas d'image
                null  // pas de faceId
        );

        try {
            // Ajout en base
            userService.signup(user, password);

            // Envoi de l'email d'invitation
            EmailService.sendInvitationEmail(email, firstNameField.getText().trim(), password);

            // Fermeture du popup
            close();

            // Rafraîchir la liste si un callback est défini
            if (onSaveCallback != null) {
                onSaveCallback.run();
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'enregistrer l'utilisateur : " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        close();
    }

    private void close() {
        Stage stage = (Stage) firstNameField.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public void setOnSaveCallback(Runnable callback) {
        this.onSaveCallback = callback;
    }
}