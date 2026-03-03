package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import java.io.IOException;

public class StartController {

    @FXML
    void handleGetStarted(ActionEvent event) {
        switchScene(event, "/SignUp/SignUp1.fxml");
    }

    @FXML
    void handleSignIn(ActionEvent event) {
        // Redirection vers l'écran de connexion (SignIn.fxml)
        switchScene(event, "/SignIn/SignIn.fxml");
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
}