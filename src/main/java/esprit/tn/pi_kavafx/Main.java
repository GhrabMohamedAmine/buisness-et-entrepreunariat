package esprit.tn.pi_kavafx;

import esprit.tn.pi_kavafx.controllers.ParentLoader;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        ParentLoader.setStage(stage);

        Scene scene = new Scene(
                FXMLLoader.load(getClass().getResource("/fxml/layout/client_layout.fxml")),
                1100,
                700
        );

        scene.getStylesheets().add(
                Objects.requireNonNull(
                        getClass().getResource("/css/theme.css")
                ).toExternalForm()
        );

        stage.setTitle("Gestion Formation");
        stage.setScene(scene);
        stage.show();
    }



    public static void main(String[] args) {
        launch();
    }
}
