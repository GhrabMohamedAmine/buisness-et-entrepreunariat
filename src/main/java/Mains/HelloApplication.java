package Mains;

import atlantafx.base.theme.CupertinoLight;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

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
                getClass().getResource("/css/stylesheet.css").toExternalForm(),
                getClass().getResource("/css/buttons.css").toExternalForm(),
                getClass().getResource("/css/project-css.css").toExternalForm()
        );


        stage.setScene(scene);
        stage.setTitle("TezFx");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
