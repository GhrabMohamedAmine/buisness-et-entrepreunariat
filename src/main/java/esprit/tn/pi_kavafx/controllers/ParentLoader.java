package esprit.tn.pi_kavafx.controllers;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ParentLoader {

    public static Stage stage;

    public static void setStage(Stage primaryStage){
        stage = primaryStage;
    }

    public static void setRoot(String fxml){
        try {
            Parent root = FXMLLoader.load(ParentLoader.class.getResource("/fxml/" + fxml));
            stage.getScene().setRoot(root);
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}
