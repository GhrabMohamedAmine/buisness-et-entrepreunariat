package controllers;

import entities.User;
import services.UserService;
import services.CompreFaceService;
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
import javafx.stage.Modality;
import javafx.event.ActionEvent;
import javafx.application.Platform;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.Random;
import java.util.ResourceBundle;

public class SignInController implements Initializable {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private ImageView captchaImageView;
    @FXML private TextField captchaField;
    @FXML private Label captchaError;
    @FXML private Button refreshCaptchaBtn;

    private final UserService userService = new UserService();
    private final CompreFaceService compreFaceService = new CompreFaceService();
    private String currentCaptchaCode;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        generateCaptcha();
    }

    @FXML
    void handleSignIn(ActionEvent event) {
        String userCaptcha = captchaField.getText().trim();
        if (!userCaptcha.equalsIgnoreCase(currentCaptchaCode)) {
            captchaError.setVisible(true);
            captchaError.setManaged(true);
            generateCaptcha();
            captchaField.clear();
            return;
        }
        captchaError.setVisible(false);
        captchaError.setManaged(false);

        String email = emailField.getText();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showAlert("Erreur", "Veuillez remplir tous les champs.");
            return;
        }

        try {
            if (userService.authenticate(email, password)) {
                User currentUser = UserService.getCurrentUser();
                String status = currentUser.getStatus();
                if (status == null || (!status.equalsIgnoreCase("actif") && !status.equalsIgnoreCase("active"))) {
                    showAlert("Accès Refusé", "Votre compte n'est pas actif. Veuillez contacter l'administrateur.");
                    return;
                }
                System.out.println("Connexion réussie ! Rôle : " + currentUser.getRole());
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
    void handleFaceSignIn(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/CaptureFace/CaptureFace.fxml"));
            Parent root = loader.load();
            CaptureFaceController captureController = loader.getController();

            Stage captureStage = new Stage();
            captureStage.setTitle("Reconnaissance faciale");
            captureStage.setScene(new Scene(root));
            captureStage.initModality(Modality.APPLICATION_MODAL);
            captureStage.setOnHidden(e -> captureController.closeWebcam());

            captureController.setOnCaptureCallback(() -> {
                byte[] faceImage = captureController.getCapturedImageData();
                if (faceImage != null) {
                    String recognizedSubject = compreFaceService.recognizeFace(faceImage);
                    if (recognizedSubject != null) {
                        try {
                            User user = userService.getUserByEmail(recognizedSubject);
                            if (user != null && ("actif".equalsIgnoreCase(user.getStatus()) || "active".equalsIgnoreCase(user.getStatus()))) {
                                UserService.setCurrentUser(user); // méthode statique à ajouter dans UserService
                                Platform.runLater(() -> {
                                    captureStage.close();
                                    if ("Admin".equals(user.getRole())) {
                                        switchScene(event, "/User/UserTable.fxml");
                                    } else {
                                        switchScene(event, "/Profile/Profile1.fxml");
                                    }
                                });
                            } else {
                                Platform.runLater(() -> showAlert("Accès refusé", "Compte inactif ou introuvable"));
                            }
                        } catch (SQLException e) {
                            e.printStackTrace();
                            Platform.runLater(() -> showAlert("Erreur", "Erreur base de données"));
                        }
                    } else {
                        Platform.runLater(() -> showAlert("Échec", "Visage non reconnu"));
                    }
                } else {
                    Platform.runLater(() -> showAlert("Erreur", "Aucune image capturée"));
                }
            });

            captureStage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir la fenêtre de capture.");
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
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder(6);
        Random random = new Random();
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        currentCaptchaCode = sb.toString();
        Image captchaImage = createCaptchaImage(currentCaptchaCode);
        captchaImageView.setImage(captchaImage);
    }

    private Image createCaptchaImage(String code) {
        Canvas canvas = new Canvas(150, 50);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.rgb(240, 240, 240));
        gc.fillRect(0, 0, 150, 50);
        gc.setStroke(Color.rgb(200, 200, 200));
        gc.setLineWidth(1);
        Random rand = new Random();
        for (int i = 0; i < 5; i++) {
            gc.strokeLine(rand.nextInt(150), rand.nextInt(50), rand.nextInt(150), rand.nextInt(50));
        }
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        gc.setFill(Color.rgb(50, 50, 50));
        for (int i = 0; i < code.length(); i++) {
            double x = 10 + i * 22 + rand.nextInt(5);
            double y = 35 + rand.nextInt(5);
            gc.fillText(String.valueOf(code.charAt(i)), x, y);
        }
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
    @FXML
    private void handleForgotPassword(ActionEvent event) {
        switchScene(event, "/SignIn/ForgotPassword_Step1.fxml");
    }
}