package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.BoxBlur;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import entities.Project;
import entities.User;
import services.ProjectService;
import services.UserService;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ProjectsController {

    @FXML
    private FlowPane projectsGrid;
    @FXML
    private Button Addbutton;

    @FXML
    private ScrollPane scrollPane;

    @FXML
    private TextField searchField;

    private final ProjectService projectService = new ProjectService();
    private final UserService userService = new UserService();

    @FXML
    public void initialize() {
        projectsGrid.setHgap(20);
        projectsGrid.setVgap(20);

        System.out.println(isUserManager());

        Addbutton.setVisible(isUserManager());
        Addbutton.setManaged(isUserManager());

        loadData();
        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldValue, newValue) -> loadData(newValue));
        }
    }

    public boolean isUserManager(){
        User loggedUser = userService.getCurrentUser();
        return loggedUser != null
                && loggedUser.getRole() != null
                && "MANAGER".equals(loggedUser.getRole().trim().toUpperCase(Locale.ROOT));
    }

    public void loadData() {
        loadData(null);
    }

    private void loadData(String query) {
        projectsGrid.getChildren().clear();

        List<Project> projectList = getVisibleProjectsForCurrentUser();
        Map<Integer, List<User>> assigneesByProject = projectService.getProjectAssigneesMap();
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase();
        if (!normalizedQuery.isEmpty()) {
            projectList = projectList.stream()
                    .filter(p -> (p.getName() != null && p.getName().toLowerCase().contains(normalizedQuery))
                            || (p.getDescription() != null && p.getDescription().toLowerCase().contains(normalizedQuery)))
                    .toList();
        }

        if (projectList.isEmpty()) {
            Label emptyLabel = new Label(isCurrentUserEmployee()
                    ? "No projects are assigned to you yet."
                    : "No projects found.");
            emptyLabel.getStyleClass().add("task-time");
            projectsGrid.getChildren().add(emptyLabel);
            return;
        }

        System.out.println("Projects found in DB: " + projectList.size());

        for (Project project : projectList) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/tezfx/view/components/ProjectCard.fxml"));
                Parent projectCard = loader.load();

                ProjectCardController cardController = loader.getController();
                if (cardController != null) {
                    cardController.setData(project);
                    cardController.setAssignees(assigneesByProject.getOrDefault(project.getId(), List.of()));
                } else {
                    System.err.println("Could not find ProjectCardController!");
                }

                projectsGrid.getChildren().add(projectCard);

            } catch (IOException e) {
                System.err.println("Error loading ProjectCard: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private List<Project> getVisibleProjectsForCurrentUser() {
        User loggedUser = userService.getCurrentUser();
        if (loggedUser == null || loggedUser.getRole() == null) {
            return projectService.getAllProjects();
        }
        if ("EMPLOYEE".equals(loggedUser.getRole().trim().toUpperCase(Locale.ROOT))) {
            return projectService.getProjectsForUser(loggedUser.getId());
        }
        return projectService.getAllProjects();
    }

    private boolean isCurrentUserEmployee() {
        User loggedUser = userService.getCurrentUser();
        return loggedUser != null
                && loggedUser.getRole() != null
                && "EMPLOYEE".equals(loggedUser.getRole().trim().toUpperCase(Locale.ROOT));
    }

    @FXML
    private void openAddProjectPopup() {
        if (projectsGrid == null || projectsGrid.getScene() == null) {
            return;
        }
        Node mainLayout = projectsGrid.getScene().getRoot();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/tezfx/view/add-project.fxml"));
            Parent root = loader.load();

            AddProjectController popupController = loader.getController();
            popupController.setOnProjectCreated(this::loadData);

            Stage popupStage = new Stage();
            popupStage.initStyle(StageStyle.TRANSPARENT);

            BoxBlur blur = new BoxBlur(8, 8, 3);
            ColorAdjust dim = new ColorAdjust();
            dim.setBrightness(-0.3);
            dim.setInput(blur);
            mainLayout.setEffect(dim);

            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.initOwner(projectsGrid.getScene().getWindow());

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            popupStage.setScene(scene);
            popupStage.centerOnScreen();
            popupStage.showAndWait();

            mainLayout.setEffect(null);
            loadData();
        } catch (Exception e) {
            mainLayout.setEffect(null);
            e.printStackTrace();
        }
    }

    @FXML
    private void onCalendarIconClicked() {
        MainController.setView("calendar.fxml");
    }
}
