package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;

public class SidebarController {
    @FXML
    private ToggleButton btnHome, btnSettings, btnProfile;
    @FXML private ToggleGroup navGroup;



        @FXML
        private void onDashboardClicked() {
            // This tells the HelloController to swap the middle view
            MainController.setView("dashboard.fxml");
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
}
