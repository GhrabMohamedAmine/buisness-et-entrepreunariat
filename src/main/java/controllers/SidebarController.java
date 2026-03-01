package controllers;

import entities.User;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import services.UserService;

import java.io.IOException;

public class SidebarController {

    @FXML private ToggleGroup navGroup;

    @FXML
    private ToggleButton TRbutton;
    @FXML
    private ToggleButton TKbutton;
    @FXML
    private ToggleButton PJbutton;
    @FXML
    private ToggleButton FPbutton;
    @FXML
    private ToggleButton RSbutton;


    private MainController mainCon =new MainController();
    @FXML
    private Button LOGOUT;

    @FXML
    private ToggleButton CMButton;
    @FXML
    private ImageView logoImage;


    //private User userConnected = mainCon.getCurrentuser();;
    @FXML
    public void initialize() {
         User userConnected = mainCon.getCurrentuser();
        if (userConnected == null) {
            System.out.println("couldn't fetch user loading default views");
        }
        else {
            userViewControl(userConnected);
        }
    }

    private void userViewControl(User userConnected) {

        switch(userConnected.getRole().toUpperCase()){
            case "EMPLOYEE"->{
                hideBTN(FPbutton);
            }
            case "CONSULTANT"->{
                hideBTN(RSbutton);
                hideBTN(TRbutton);
                showBTN(FPbutton);
                showBTN(PJbutton);
            }
            default ->{
                showBTN(RSbutton);
                showBTN(TKbutton);
                showBTN(FPbutton);
                showBTN(PJbutton);
                showBTN(TRbutton);
                showBTN(FPbutton);
            }
        }

    }

        private void resetViewSB(){
            showBTN(RSbutton);
            showBTN(TKbutton);
            showBTN(FPbutton);
            showBTN(PJbutton);
            showBTN(TRbutton);
            showBTN(FPbutton);
        }


        @FXML
        private void onDashboardClicked() {
            // This tells the HelloController to swap the middle view
            MainController.setView("dashboard.fxml");
        }

        private void hideBTN(Node node) {
            node.setVisible(false);
            node.setManaged(false);
        }

        private void showBTN(Node node) {
            node.setVisible(true);
            node.setManaged(true);
        }

        @FXML
        private void ghrabclicked() {
            MainController.setView("hello-view.fxml");
        }

        @FXML
        private void onProjectsClicked() {
            MainController.setView("project.fxml");
        }


        @FXML
        private void onTasksClicked() {
            MainController.setView("tasks.fxml");
        }

        @FXML
        private void onFinancialClicked() {
            MainController.setView("/testp1/finance_page.fxml");
        }



    private void loadPage(String pageName) {
        System.out.println("Loading: " + pageName);

    }
    @FXML
    public void handleLogout(ActionEvent event) {
        UserService.logout();
        switchScene(event, "/Start/1ere.fxml");
    }
    private void switchScene(ActionEvent event, String fxmlPath) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            System.err.println("Impossible de charger le fichier FXML : " + fxmlPath);
            e.printStackTrace();
        }
    }

    @FXML
    public void onCommunicationClicked(){
        mainCon.messages();
    }

    @FXML
    public void setResourcesView() {
        mainCon.resources();
    }
}
