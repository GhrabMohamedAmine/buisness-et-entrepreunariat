package com.example.testp1;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ResourceBundle;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {

        ResourceBundle bundle = ResourceBundle.getBundle("com.bundles.text");


        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("finance_page.fxml"), bundle);

        Scene scene = new Scene(fxmlLoader.load(), 1213, 800);
        stage.setTitle("Nexum");
        stage.setScene(scene);
        stage.show();
    }
}
