package Mains;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.net.URL;
import utils.database;

public class MainFX extends Application {
    @Override
    public void start(Stage stage) throws Exception {

            // 1️⃣ Keep default JavaFX base skin (VERY IMPORTANT)
     //       Application.setUserAgentStylesheet(Application.STYLESHEET_MODENA);

            // 2️⃣ Then apply AtlantaFX ON TOP
       //     Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());

            URL fxmlLocation = getClass().getResource("/Start/1ere.fxml");

            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            Scene scene = new Scene(loader.load());

            // 3️⃣ Apply YOUR css LAST (highest priority)
        URL cssUrl = getClass().getResource("/css/styles123.css");

        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        } else {
            System.out.println("CSS not found: /css/styles123.css");
        }

            stage.setScene(scene);
            stage.setTitle("Gestion des Utilisateurs");
            stage.show();
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