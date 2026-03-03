package Mains;

import atlantafx.base.theme.CupertinoLight;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

public class HelloApplication extends Application {

    private Stage primaryStage;
    @Override
    public void start(Stage stage) throws Exception {
        Application.setUserAgentStylesheet(
                new CupertinoLight().getUserAgentStylesheet()
        );
    this.primaryStage = stage;


        primaryStage.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.F11) {
                primaryStage.setFullScreen(!primaryStage.isFullScreen());
                event.consume(); // Prevents other components from handling F11
            }
        });

        // 3. Global Listener for Layout Changes
        // Use this to hide sidebars or expand charts when in full-screen
        primaryStage.fullScreenProperty().addListener((obs, wasFullScreen, isNowFullScreen) -> {
            if (isNowFullScreen) {
                System.out.println("Mode Plein Écran : Ajustement de l'interface Nexum.");
            } else {
                System.out.println("Mode Fenêtré : Retour à la mise en page standard.");
            }
        });




        FXMLLoader loader = new FXMLLoader(
                HelloApplication.class.getResource("/tezfx/view/hello-view.fxml")
        );

        Scene scene = new Scene(loader.load(), 1200, 800);
        scene.getStylesheets().addAll(
                getClass().getResource("/styles/stylesheet.css").toExternalForm(),
                getClass().getResource("/styles/buttons.css").toExternalForm(),
                getClass().getResource("/styles/project-css.css").toExternalForm()
        );

        primaryStage.setTitle("Nexum - Gestion de Projet");
        primaryStage.setScene(scene);
        primaryStage.show();




    }

    public static void main(String[] args) {
        launch(args);
    }
}
