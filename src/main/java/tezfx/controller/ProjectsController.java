package tezfx.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import tezfx.model.Project;
import tezfx.model.User;
import tezfx.model.sql;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class ProjectsController {

    @FXML
    private FlowPane projectsGrid;

    @FXML
    private ScrollPane scrollPane;

    @FXML
    private TextField searchField;

    private final sql dao = new sql();

    @FXML
    public void initialize() {
        projectsGrid.setHgap(20);
        projectsGrid.setVgap(20);
        loadData();
        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldValue, newValue) -> loadData(newValue));
        }
    }

    public void loadData() {
        loadData(null);
    }

    private void loadData(String query) {
        projectsGrid.getChildren().clear();

        List<Project> projectList = dao.getAllProjects();
        Map<Integer, List<User>> assigneesByProject = dao.getProjectAssigneesMap();
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase();
        if (!normalizedQuery.isEmpty()) {
            projectList = projectList.stream()
                    .filter(p -> (p.getName() != null && p.getName().toLowerCase().contains(normalizedQuery))
                            || (p.getDescription() != null && p.getDescription().toLowerCase().contains(normalizedQuery)))
                    .toList();
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

    @FXML
    private void openAddProjectPopup() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/tezfx/view/add-project.fxml"));
            Parent popup = loader.load();

            AddProjectController popupController = loader.getController();
            popupController.setOnProjectCreated(this::loadData);
            MainController.getStaticContentArea().getChildren().add(popup);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
