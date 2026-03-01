package controllers;

import entities.User;
import services.UserService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import java.io.IOException;
import java.sql.SQLException;

public class SignInController {

    @FXML private TextField emailField;       // Lié au TextField du FXML
    @FXML private PasswordField passwordField; // Lié au PasswordField du FXML

    private final UserService userService = new UserService();


    @FXML
    void handleSignIn(ActionEvent event) {
        String email = emailField.getText();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showAlert("Erreur", "Veuillez remplir tous les champs.");
            return;
        }

        try {
            // 1. Tenter l'authentification
            if (userService.authenticate(email, password)) {

                // 2. Récupérer l'utilisateur connecté
                User currentUser = UserService.getCurrentUser();
                System.out.println("Connexion réussie ! Rôle : " + currentUser.getRole());

                // 3. Vérifier le rôle pour la redirection
                // Adaptez "Admin" selon ce qui est écrit exactement dans votre base de données (ex: "ADMIN", "Administrateur")
                if ("Admin".equalsIgnoreCase(currentUser.getRole())) {
                    // Si c'est un admin -> Table des utilisateurs
                    switchScene(event, "/User/UserTable.fxml");
                } else {
                    // Si c'est un autre rôle (Client, User, etc.) -> Page de profil
                    switchScene(event, "/Profile/Profile1.fxml");
                }

            } else {
                showAlert("Échec", "Email ou mot de passe incorrect.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Erreur SQL", "Problème lors de la connexion à la base de données.");
        }
    }

    @FXML
    void handleBack(ActionEvent event) {
        switchScene(event, "/Start/1ere.fxml");
    }

    @FXML
    void showMainPage(ActionEvent event) {
        switchScene(event, "/Start/1ere.fxml");
    }

    private void switchScene(ActionEvent event, String fxmlPath) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            System.err.println("Erreur de navigation vers : " + fxmlPath);
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
}