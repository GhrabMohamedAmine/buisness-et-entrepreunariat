package tezfx.controller;

import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.ProgressBar;
import javafx.fxml.FXML;
import tezfx.model.Project;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import tezfx.model.User;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

// tezfx.controller.ProjectCardController.java
public class ProjectCardController {
    private static final int PROJECT_DESCRIPTION_MAX_CHARS = 140;
    private static final int MAX_VISIBLE_ASSIGNEE_AVATARS = 4;
    private static final String[] AVATAR_COLOR_CLASSES = {
            "purple", "blue", "green", "orange"
    };

    @FXML
    private Label nameLabel, descriptionLabel, progressText, dateLabel;
    @FXML
    private ProgressBar progressBar;
    @FXML
    private HBox assigneesContainer;

    private Project currentProject;
    private List<User> assignees = List.of();

    public void setData(Project project) {
        if (project == null) return;
        this.currentProject = project;

        // Use System.out to verify the data is reaching the card
        System.out.println("Filling card for: " + project.getName());
        nameLabel.setText(project.getName());
        String fullDescription = project.getDescription();
        descriptionLabel.setText(truncateDescription(fullDescription));
        descriptionLabel.setTooltip(
                fullDescription == null || fullDescription.isBlank() ? null : new Tooltip(fullDescription)
        );

        int progress = project.getProgress();
        progressText.setText(progress + "%");

        progressBar.setProgress(progress / 100.0);

        dateLabel.setText(project.getEndDate());


    }

    public void setAssignees(List<User> users) {
        this.assignees = users == null ? List.of() : new ArrayList<>(users);
        renderAssignees();
    }

    private String truncateDescription(String description) {
        if (description == null || description.isBlank()) {
            return "No description";
        }
        String normalized = description.trim().replaceAll("\\s+", " ");
        if (normalized.length() <= PROJECT_DESCRIPTION_MAX_CHARS) {
            return normalized;
        }
        return normalized.substring(0, PROJECT_DESCRIPTION_MAX_CHARS - 3) + "...";
    }

    private void renderAssignees() {
        if (assigneesContainer == null) return;
        assigneesContainer.getChildren().clear();

        List<User> users = assignees == null ? List.of() : assignees;
        if (users.isEmpty()) {
            assigneesContainer.getChildren().add(buildInitialAvatar("U", "gray"));
            Tooltip.install(assigneesContainer, new Tooltip("Unassigned"));
            return;
        }

        int visible = Math.min(users.size(), MAX_VISIBLE_ASSIGNEE_AVATARS);
        for (int i = 0; i < visible; i++) {
            String fullName = users.get(i).getFullName();
            assigneesContainer.getChildren().add(buildInitialAvatar(fullName, pickAvatarColorClass(fullName)));
        }

        if (users.size() > MAX_VISIBLE_ASSIGNEE_AVATARS) {
            int remaining = users.size() - MAX_VISIBLE_ASSIGNEE_AVATARS;
            assigneesContainer.getChildren().add(buildCountAvatar("+" + remaining));
        }

        String names = users.stream().map(User::getFullName).collect(Collectors.joining(", "));
        Tooltip.install(assigneesContainer, new Tooltip(names));
    }

    private StackPane buildInitialAvatar(String fullName, String colorClass) {
        String safe = (fullName == null || fullName.isBlank()) ? "U" : fullName.trim();
        String initial = String.valueOf(Character.toUpperCase(safe.charAt(0)));
        return buildAvatar(initial, colorClass);
    }

    private StackPane buildCountAvatar(String text) {
        return buildAvatar(text, "dark");
    }

    private StackPane buildAvatar(String text, String colorClass) {
        Label label = new Label(text);
        label.getStyleClass().add("avatar-initial");

        StackPane circle = new StackPane(label);
        circle.setAlignment(Pos.CENTER);
        circle.getStyleClass().addAll("avatar", colorClass);
        return circle;
    }

    private String pickAvatarColorClass(String key) {
        String safe = key == null ? "U" : key;
        int index = Math.abs(safe.hashCode()) % AVATAR_COLOR_CLASSES.length;
        return AVATAR_COLOR_CLASSES[index];
    }

    @FXML
    private void handleViewDetails() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/tezfx/view/project-details.fxml"));
            Parent root = loader.load();

            // Get the detail controller and send the current project
            ProjectDetailsController controller = loader.getController();
            controller.setProjectData(this.currentProject); // Pass the project object

            // Switch the main content area to this new root
            MainController.setContent(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
