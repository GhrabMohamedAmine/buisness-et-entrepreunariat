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

    // 1. Define your fixed global dimensions here
    private static final double APP_WIDTH = 1300;
    private static final double APP_HEIGHT = 700;

    @Override
    public void start(Stage stage) throws Exception {

        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());

        primaryStage = stage;

        // 2. Lock the window so the user cannot drag the edges
        stage.setResizable(true);
        stage.setTitle("Admin Dashboard");

        loadPage("/back/SmAdminStack.fxml");

        stage.show();
    }

    // 🔥 THIS METHOD SWITCHES PAGES
    public static void loadPage(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(MainFX.class.getResource(fxmlPath));
            Parent root = loader.load();

            // 3. Force the new Scene to ALWAYS be your exact fixed dimensions
            Scene scene = new Scene(root, APP_WIDTH, APP_HEIGHT);
            primaryStage.setScene(scene);

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