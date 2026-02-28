package controllers;

import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;

import java.io.IOException;

public class SmAdminStackController {
    @FXML
    private StackPane RSmanagepage;

    public static SmAdminStackController instance;


    public void initialize(){
        instance = this;

        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());

        loadViewSB("manage-resources.fxml");
    }

    public void loadViewSB(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/back/"+fxmlPath));
            Parent view = loader.load();

            // Replaces the current child of the StackPane with the new view
            RSmanagepage.getChildren().setAll(view);
            StackPane.setAlignment(view, javafx.geometry.Pos.TOP_CENTER);

            System.out.println("Navigation: Successfully loaded " + fxmlPath);
        } catch (IOException e) {
            System.err.println("Navigation Error: Could not load " + fxmlPath);
            e.printStackTrace();
        }
    }

    public void loadPageDF(){
        loadViewSB("demand-forecast.fxml");
    }

    public void loadPageMR(){
        loadViewSB("manage-requests.fxml");
    }

    public void loadPageMRS(){
        loadViewSB("manage-resources.fxml");
    }

    public void loadPagePHYR(){
        loadViewSB("physical-returns.fxml");
    }

    public static SmAdminStackController getInstance() {
        return instance;
    }
}
