package Mains;

import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import utils.database;

public class MainFX extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {

        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());

        primaryStage = stage;

        loadPage("/front/client-resources.fxml");

        stage.setTitle("Admin Dashboard");
        stage.show();
    }

    // 🔥 THIS METHOD SWITCHES PAGES
    public static void loadPage(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(MainFX.class.getResource(fxmlPath));
            Parent root = loader.load();
            primaryStage.setScene(new Scene(root));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            System.out.println("Connected: " + database.getInstance().getConnection());
        } catch (Exception e) {
            e.printStackTrace();
        }
        launch(args);
    }
}
