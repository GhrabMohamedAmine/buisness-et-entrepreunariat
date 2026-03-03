package Mains;

import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.net.URL;
import utils.database;

public class MainFX extends Application {

    private static Stage primaryStage;
    @Override
    public void start(Stage stage) throws Exception {
        // 1. Application du thème
        primaryStage = stage;
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());

        // 2. CORRECTION DU CHEMIN : Vérifiez bien le nom du dossier dans src/main/resources
        // Si votre fichier est dans src/main/resources/UserManagement/manage-resources.fxml :
        URL fxmlLocation = getClass().getResource("/Start/1ere.fxml");
        if (fxmlLocation == null) {
            throw new RuntimeException("Erreur : Fichier FXML non trouvé. Vérifiez le nom du dossier dans resources !");
        }

        FXMLLoader loader = new FXMLLoader(fxmlLocation);
        Scene scene = new Scene(loader.load());
        

        stage.setTitle("Gestion des Utilisateurs");
        stage.setScene(scene);
        stage.show();
    }

    public static void loadPage(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(MainFX.class.getResource(fxmlPath));
            Parent root = loader.load();

            // 3. Force the new Scene to ALWAYS be your exact fixed dimensions
            Scene scene = new Scene(root,1300 , 750);
            primaryStage.setScene(scene);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 3. CORRECTION DU MAIN : La méthode doit s'appeler 'main' (minuscule) et non 'MainFX'
    public static void main(String[] args) {
        try {
            System.out.println("Connected: " + database.getInstance().getConnection());
        } catch (Exception e) {
            e.printStackTrace();
        }
        launch(args);
    }
}