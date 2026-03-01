package controllers;

import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;
import utils.Router;

import static utils.Router.goToFormationList;

public class MRformationController {
    @FXML
    private StackPane MariemStack;

    public void initialize() {

        // ⭐ register the container ONCE
        Router.setMainContainer(MariemStack);
        System.out.println("calling fxml..");

        // default page
        Router.goTo("formation_list.fxml");
    }



}
