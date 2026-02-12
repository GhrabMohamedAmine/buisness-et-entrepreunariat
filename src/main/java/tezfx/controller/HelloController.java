package tezfx.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import tezfx.model.Project;
import tezfx.model.sql; // You need to import your DAO
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.time.LocalDate;
import java.util.List;

public class HelloController {

    @FXML private TableView<Project> projectsTable;
    @FXML private TableColumn<Project, String> nameCol;
    @FXML private TableColumn<Project, Number> progressCol;
    @FXML private TableColumn<Project, Number> teamCol; // Changed from String to Number
    @FXML private TableColumn<Project, String> startDateCol;
    @FXML private TableColumn<Project, String> endDateCol;

    private sql projectDAO = new sql();

    @FXML
    public void initialize() {
        // 1. Basic Column Mapping
        nameCol.setCellValueFactory(data -> data.getValue().nameProperty());
        startDateCol.setCellValueFactory(data -> data.getValue().startDateProperty());
        endDateCol.setCellValueFactory(data -> data.getValue().endDateProperty());
        startDateCol.setCellValueFactory(data -> data.getValue().startDateProperty());
        endDateCol.setCellValueFactory(data -> data.getValue().endDateProperty()); // Sets next week as default

        // 2. Progress Bar Column
        progressCol.setCellValueFactory(data -> data.getValue().progressProperty());
        progressCol.setCellFactory(col -> new TableCell<Project, Number>() {
            private final ProgressBar bar = new ProgressBar();
            @Override
            protected void updateItem(Number value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setGraphic(null);
                } else {
                    // Convert integer (0-100) to double (0.0-1.0)
                    bar.setProgress(value.doubleValue() / 100.0);
                    bar.setPrefWidth(120);
                    setGraphic(bar);
                }
            }
        });

        // 3. Team/CreatedBy Column (Fixed for User ID)
        teamCol.setCellValueFactory(data -> data.getValue().createdByProperty());
        teamCol.setCellFactory(col -> new TableCell<Project, Number>() {
            @Override
            protected void updateItem(Number userId, boolean empty) {
                super.updateItem(userId, empty);
                if (empty || userId == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    // For now, displaying "User ID: #"
                    // In the future, you can look up the user's name here
                    Label idLabel = new Label("ID: " + userId);
                    idLabel.setStyle("-fx-background-color: #e1f5fe; -fx-padding: 2 8; -fx-background-radius: 10;");
                    setGraphic(idLabel);
                }
            }
        });

        // 4. LOAD DATA FROM DATABASE
        loadData();
    }
    @FXML
    private void openAddProjectPopup() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/tezfx/view/add-project.fxml"));
            Parent popup = loader.load();

            AddProjectController popupcontroller = loader.getController();
            popupcontroller.setParentController(this);
            MainController.getStaticContentArea().getChildren().setAll(popup);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadData() {
        List<Project> projectsFromDB = projectDAO.getAllProjects();
        ObservableList<Project> observableList = FXCollections.observableArrayList(projectsFromDB);
        projectsTable.setItems(observableList);
    }
}