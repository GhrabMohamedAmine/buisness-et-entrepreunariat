package controllers;

import entities.User;
import services.UserService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.event.ActionEvent;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.Random;
import java.util.ResourceBundle;

public class SignInController implements Initializable {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;

    // Nouveaux éléments pour le captcha
    @FXML private ImageView captchaImageView;
    @FXML private TextField captchaField;
    @FXML private Label captchaError;
    @FXML private Button refreshCaptchaBtn;

    private final UserService userService = new UserService();
    private String currentCaptchaCode;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Générer un captcha au chargement de la vue
        generateCaptcha();
    }

    @FXML
    void handleSignIn(ActionEvent event) {
        // 1. Vérifier le captcha
        String userCaptcha = captchaField.getText().trim();
        if (!userCaptcha.equalsIgnoreCase(currentCaptchaCode)) {
            captchaError.setVisible(true);
            captchaError.setManaged(true);
            generateCaptcha();  // Renouveler le captcha après une erreur
            captchaField.clear();
            return;
        }
        captchaError.setVisible(false);
        captchaError.setManaged(false);

        // 2. Récupérer les identifiants
        String email = emailField.getText();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showAlert("Erreur", "Veuillez remplir tous les champs.");
            return;
        }

        try {
            // 3. Authentifier l'utilisateur
            if (userService.authenticate(email, password)) {

                User currentUser = UserService.getCurrentUser();
                String status = currentUser.getStatus();

                // Vérifier si le compte est actif
                if (status == null || (!status.equalsIgnoreCase("actif") && !status.equalsIgnoreCase("active"))) {
                    showAlert("Accès Refusé", "Votre compte n'est pas actif. Veuillez contacter l'administrateur.");
                    return;
                }

                System.out.println("Connexion réussie ! Rôle : " + currentUser.getRole());

                // 4. Redirection selon le rôle
                if ("Admin".equals(currentUser.getRole())) {
                    switchScene(event, "/User/UserTable.fxml");
                } else {
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

    @FXML
    private void refreshCaptcha() {
        generateCaptcha();
    }

    private void generateCaptcha() {
        // Générer un code aléatoire de 6 caractères (chiffres et lettres majuscules sans ambigüité)
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder(6);
        Random random = new Random();
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        currentCaptchaCode = sb.toString();

        // Créer l'image du captcha
        Image captchaImage = createCaptchaImage(currentCaptchaCode);
        captchaImageView.setImage(captchaImage);
    }

    private Image createCaptchaImage(String code) {
        Canvas canvas = new Canvas(150, 50);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // Fond gris clair
        gc.setFill(Color.rgb(240, 240, 240));
        gc.fillRect(0, 0, 150, 50);

        // Lignes de bruit
        gc.setStroke(Color.rgb(200, 200, 200));
        gc.setLineWidth(1);
        Random rand = new Random();
        for (int i = 0; i < 5; i++) {
            gc.strokeLine(rand.nextInt(150), rand.nextInt(50), rand.nextInt(150), rand.nextInt(50));
        }

        // Dessiner le code avec une police et une légère déformation
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        gc.setFill(Color.rgb(50, 50, 50));
        for (int i = 0; i < code.length(); i++) {
            double x = 10 + i * 22 + rand.nextInt(5);
            double y = 35 + rand.nextInt(5);
            gc.fillText(String.valueOf(code.charAt(i)), x, y);
        }

        // Convertir le canvas en image
        return canvas.snapshot(null, null);
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