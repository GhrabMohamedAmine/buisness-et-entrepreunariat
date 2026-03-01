package controllers;

import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;

import java.io.IOException;

public class StackController {

    @FXML
    private StackPane Resourcepage;

    public static StackController instance;

    private String currentClientCode = "CL001";

    public String getCurrentClientCode() {
        return currentClientCode;
    }

    public void setCurrentClientCode(String clientCode) {
        this.currentClientCode = clientCode;
    }

    public void initialize(){
        instance = this;

        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());

        loadViewS("client-resources.fxml");
    }

    public void loadViewS(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/front/"+fxmlPath));
            Parent view = loader.load();

            // Replaces the current child of the StackPane with the new view
            Resourcepage.getChildren().setAll(view);
            StackPane.setAlignment(view, javafx.geometry.Pos.TOP_CENTER);

            System.out.println("Navigation: Successfully loaded " + fxmlPath);
        } catch (IOException e) {
            System.err.println("Navigation Error: Could not load " + fxmlPath);
            e.printStackTrace();
        }
    }

    public void loadPageCR(){
        loadViewS("client-resources.fxml");
    }

    public void loadPageRR(){
        loadViewS("resources-catalog.fxml");
    }

    public static StackController getInstance() {
        return instance;
    }
}
