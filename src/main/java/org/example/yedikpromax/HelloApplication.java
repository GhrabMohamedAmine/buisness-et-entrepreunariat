package org.example.yedikpromax;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                HelloApplication.class.getResource("/org/example/yedikpromax/chat-view.fxml")
        );

        Scene scene = new Scene(loader.load(), 1300, 650);

        // ✅ AtlantaFX theme (defines -color-* tokens)
        scene.getStylesheets().add(
                atlantafx.base.theme.PrimerLight.class
                        .getResource("primer-light.css")
                        .toExternalForm()
        );

        // ✅ Your CSS after theme
        scene.getStylesheets().add(
                HelloApplication.class
                        .getResource("/org/example/yedikpromax/chat-view.css")
                        .toExternalForm()
        );

        stage.setScene(scene);
        stage.show();
    }
}
