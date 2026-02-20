package controllers;

import entities.User;
import javafx.application.Platform;
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
import javafx.stage.Modality;
import javafx.stage.Stage;
import services.CompreFaceService;
import services.UserService;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
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
    @FXML private Circle profileCircle;

    // Stockage des données binaires de l'image sélectionnée
    private byte[] selectedImageData;

    private final UserService userService = new UserService();
    private final CompreFaceService compreFaceService = new CompreFaceService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (roleComboBox != null) {
            roleComboBox.setItems(FXCollections.observableArrayList(
                    "Manager", "Ressource Manager", "Employer",
                    "Formateur", "Chef Projet", "Expert Financier"
            ));
        }
    }

    // --- Gestion de l'upload d'image (conversion en byte[]) ---
    @FXML
    void handleImageUpload(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une photo de profil");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg")
        );

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            try {
                // Lire le fichier en tableau d'octets
                selectedImageData = Files.readAllBytes(selectedFile.toPath());

                // Mettre à jour l'aperçu
                if (profileCircle != null) {
                    Image image = new Image(new ByteArrayInputStream(selectedImageData));
                    profileCircle.setFill(new ImagePattern(image));
                }
            } catch (IOException e) {
                showAlert("Erreur", "Impossible de lire le fichier image : " + e.getMessage());
            }
        }
    }

    @FXML
    void handleSignUp(ActionEvent event) {
        clearErrors();
        boolean isValid = true;

        String nom = lastNameField.getText();
        String prenom = firstNameField.getText();
        String email = emailField.getText();
        String phone = phoneField.getText();
        String dept = deptField.getText();
        String mdp = passwordField.getText();

        String role = (roleComboBox.getValue() != null) ? roleComboBox.getValue() : "Employer";
        String dateInscription = LocalDate.now().toString();

        String nameRegex = "^[a-zA-ZÀ-ÿ\\s\\-]+$";

        // Validation Nom
        if (nom.isEmpty()) {
            showInlineError(lastNameError, "Le nom est requis.");
            isValid = false;
        } else if (!nom.matches(nameRegex)) {
            showInlineError(lastNameError, "Lettres uniquement.");
            isValid = false;
        }

        // Validation Prénom
        if (prenom.isEmpty()) {
            showInlineError(firstNameError, "Le prénom est requis.");
            isValid = false;
        } else if (!prenom.matches(nameRegex)) {
            showInlineError(firstNameError, "Lettres uniquement.");
            isValid = false;
        }

        // Validation Email
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[a-zA-Z]{2,}$";
        if (email.isEmpty()) {
            showInlineError(emailError, "L'email est requis.");
            isValid = false;
        } else if (!email.matches(emailRegex)) {
            showInlineError(emailError, "Format email invalide.");
            isValid = false;
        }

        // Validation Téléphone
        String phoneRegex = "^[0-9\\s+]+$";
        if (phone.isEmpty()) {
            showInlineError(phoneError, "Le téléphone est requis.");
            isValid = false;
        } else if (!phone.matches(phoneRegex)) {
            showInlineError(phoneError, "Chiffres et '+' uniquement.");
            isValid = false;
        }

        // Validation Département
        if (dept.isEmpty()) {
            showInlineError(deptError, "Le département est requis.");
            isValid = false;
        } else if (!dept.matches(nameRegex)) {
            showInlineError(deptError, "Lettres uniquement.");
            isValid = false;
        }

        // Validation Mot de passe
        if (mdp.isEmpty()) {
            showAlert("Erreur", "Le mot de passe est obligatoire.");
            isValid = false;
        }

        if (!isValid) {
            return;
        }

        // Si aucune image n'a été sélectionnée, on laisse un tableau vide
        if (selectedImageData == null) {
            selectedImageData = new byte[0];
        }
        System.out.println("Taille de l'image de profil : " + selectedImageData.length + " octets");
        // Création de l'utilisateur temporaire (sans faceId)
        User newUser = new User(0, nom, prenom, email, phone, role, dept, "en attente", dateInscription, selectedImageData, null);

        // Ouvrir la fenêtre de capture faciale
        openFaceCaptureWindow(event, newUser, mdp);
    }

    private void openFaceCaptureWindow(ActionEvent event, User user, String password) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/CaptureFace/CaptureFace.fxml"));
            Parent root = loader.load();
            CaptureFaceController captureController = loader.getController();

            Stage captureStage = new Stage();
            captureStage.setTitle("Capture faciale");
            captureStage.setScene(new Scene(root));
            captureStage.initModality(Modality.APPLICATION_MODAL);
            captureStage.setOnHidden(e -> captureController.closeWebcam()); // fermer la webcam à la fermeture

            captureController.setOnCaptureCallback(() -> {
                byte[] faceImage = captureController.getCapturedImageData();
                if (faceImage != null) {
                    // Enregistrer le visage via CompreFace
                    String faceId = compreFaceService.registerFace(faceImage, user.getEmail());
                    if (faceId != null) {
                        user.setFaceId(faceId);
                        try {
                            userService.signup(user, password);
                            Platform.runLater(() -> {
                                captureStage.close();
                                switchScene(event, "/SignUp/SignUp2.fxml");
                            });
                        } catch (SQLException e) {
                            e.printStackTrace();
                            Platform.runLater(() -> showAlert("Erreur", "Échec de la création du compte : " + e.getMessage()));
                        }
                    } else {
                        Platform.runLater(() -> showAlert("Erreur", "Échec de l'enregistrement facial. Veuillez réessayer."));
                    }
                } else {
                    Platform.runLater(() -> showAlert("Erreur", "Aucune image capturée."));
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
        switchScene(event, "/SignIn/SignIn.fxml");
    }

    public void handleNext(ActionEvent event) {
        switchScene(event, "/SignUp/SignUp3.fxml");
    }

    public void goToProjects(MouseEvent event) {
        switchScen(event, "/SignIn/SignIn.fxml");
    }

    public void goToTrainings(MouseEvent event) {
        switchScen(event, "/SignIn/SignIn.fxml");
    }

    public void goToDashboard(MouseEvent event) {
        switchScen(event, "/SignIn/SignIn.fxml");
    }

    private void showInlineError(Label errorLabel, String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void clearErrors() {
        if(firstNameError != null) { firstNameError.setVisible(false); firstNameError.setManaged(false); }
        if(lastNameError != null) { lastNameError.setVisible(false); lastNameError.setManaged(false); }
        if(emailError != null) { emailError.setVisible(false); emailError.setManaged(false); }
        if(phoneError != null) { phoneError.setVisible(false); phoneError.setManaged(false); }
        if(deptError != null) { deptError.setVisible(false); deptError.setManaged(false); }
    }
}