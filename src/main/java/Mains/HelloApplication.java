package Mains;

import atlantafx.base.theme.CupertinoLight;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        Application.setUserAgentStylesheet(
                new CupertinoLight().getUserAgentStylesheet()
        );

        FXMLLoader loader = new FXMLLoader(
                HelloApplication.class.getResource("/tezfx/view/hello-view.fxml")
        );

        Scene scene = new Scene(loader.load(), 1200, 800);
        scene.getStylesheets().addAll(
                getClass().getResource("/styles/stylesheet.css").toExternalForm(),
                getClass().getResource("/styles/buttons.css").toExternalForm(),
                getClass().getResource("/styles/project-css.css").toExternalForm()
        );


        stage.setScene(scene);
        stage.setTitle("TezFx");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
