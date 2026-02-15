package tezfx.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.BoxBlur;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.layout.HBox;
import javafx.geometry.Pos;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.input.MouseEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import tezfx.model.Project;
import tezfx.model.Task;
import tezfx.model.User;
import tezfx.model.sql;
import javafx.scene.paint.Color;

import javafx.scene.control.Button;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class ProjectDetailsController {
    private static final String[] AVATAR_COLOR_CLASSES = {"purple", "blue", "green", "orange"};

    @FXML
    private Label detailName, detailDesc, detailDate, detailPercent;
    @FXML private ProgressBar detailProgress;
    @FXML private Label totalTasksLabel, completedTasksLabel;
    @FXML private Label overdueTasksLabel;
    @FXML private VBox tasksContainer;
    @FXML private ScrollPane tasksScrollPane;
    @FXML private HBox overviewContainer;
    @FXML private VBox teamContainer;
    @FXML private Project currentProject;
    @FXML private Button tasksBtn, overviewTab;
    @FXML private Label breadcrumbCurrent;



    private final sql dao = new sql();

    public void setProjectData(Project project) {
        this.currentProject = project;
        // 1. Fill basic info
        detailName.setText(project.getName());
        detailDesc.setText(project.getDescription());
        breadcrumbCurrent.setText(project.getName());
        detailDate.setText("Due " + project.getEndDate());


        double progressValue = project.getProgress() / 100.0;

        // Set the value on the progress bar
        detailProgress.setProgress(progressValue);



        // 2. Load Stats (We will write this SQL next)
        loadProjectStats(project.getId());
        loadTeamMembers(project.getId());
    }

    private void loadProjectStats(int projectId) {
        // Here you will call your DAO to count tasks from the 'tasks' table
        sql dao = new sql();

        // Fetch real numbers from MAMP
        int total = dao.getTaskCount(projectId);
        int completed = dao.getTaskCountByStatus(projectId, "DONE");
        int overdue = dao.getOverdueTaskCount(projectId);

        // Update the UI
        totalTasksLabel.setText(String.valueOf(total));
        completedTasksLabel.setText(String.valueOf(completed));

        double progress = total == 0 ? 0.0 : (completed * 100.0) / total;
        detailProgress.setProgress(progress / 100.0);
        if (detailPercent != null) {
            detailPercent.setText((int) Math.round(progress) + "%");
        }

        overdueTasksLabel.setText(String.valueOf(overdue));
    }

    private void loadTeamMembers(int projectId) {
        if (teamContainer == null) {
            return;
        }
        teamContainer.getChildren().clear();

        Map<Integer, List<User>> assigneesByProject = dao.getProjectAssigneesMap();
        List<User> users = assigneesByProject.getOrDefault(projectId, List.of());
        if (users.isEmpty()) {
            HBox row = new HBox(8, buildAvatar("U", "gray"), new Label("Unassigned"));
            row.setAlignment(Pos.CENTER_LEFT);
            teamContainer.getChildren().add(row);
            return;
        }

        for (User user : users) {
            String fullName = user.getFullName();
            HBox row = new HBox(8, buildAvatarInitial(fullName), new Label(fullName));
            row.setAlignment(Pos.CENTER_LEFT);
            Tooltip.install(row, new Tooltip(fullName));
            teamContainer.getChildren().add(row);
        }
    }

    private StackPane buildAvatarInitial(String fullName) {
        String safeName = (fullName == null || fullName.isBlank()) ? "U" : fullName.trim();
        String initial = String.valueOf(Character.toUpperCase(safeName.charAt(0)));
        return buildAvatar(initial, pickAvatarColorClass(safeName));
    }

    private StackPane buildAvatar(String text, String colorClass) {
        Label initialLabel = new Label(text);
        initialLabel.getStyleClass().add("avatar-initial");

        StackPane circle = new StackPane(initialLabel);
        circle.setPrefSize(22, 22);
        circle.setMinSize(22, 22);
        circle.setMaxSize(22, 22);
        circle.getStyleClass().addAll("avatar", colorClass);
        return circle;
    }

    private String pickAvatarColorClass(String key) {
        int index = Math.abs(key.hashCode()) % AVATAR_COLOR_CLASSES.length;
        return AVATAR_COLOR_CLASSES[index];
    }


    private void loadTasks(int projectId) {
        tasksContainer.getChildren().clear();
        List<Task> tasks = dao.getTasksByProject(projectId);

        for (Task t : tasks) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/tezfx/view/components/TaskRow.fxml"));
                Parent row = loader.load();

                // Get TaskRowController and set data
                TaskRowController controller = loader.getController();
                controller.setTaskData(t);

                tasksContainer.getChildren().add(row);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    // Inside ProjectDetailsController
    @FXML private VBox taskListContainer; // The VBox inside your "Tasks" Tab

    public void showTasksTab() {
        if (currentProject == null) {
            return;
        }

        // 1. Hide the "Overview" content, Show the "Tasks" content
        overviewContainer.setVisible(false);
        overviewContainer.setManaged(false);
        tasksScrollPane.setVisible(true);
        tasksScrollPane.setManaged(true);

        // 2. Load the rows
        taskListContainer.getChildren().clear();
        List<Task> tasks = dao.getTasksByProject(currentProject.getId());

        for (Task t : tasks) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/tezfx/view/components/TaskRow.fxml"));
                Parent row = loader.load(); // Load the HBox we designed

                TaskRowController controller = loader.getController();
                controller.setTaskData(t);
                controller.setOnEdit(() -> openUpdateTaskModal(t));
                controller.setOnDelete(() -> deleteTask(t));

                taskListContainer.getChildren().add(row); // Add it to the list!
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public void showOverviewTab() {
        // 1. Toggle visibility
        overviewContainer.setVisible(true);
        overviewContainer.setManaged(true);
        tasksScrollPane.setVisible(false);
        tasksScrollPane.setManaged(false);

        // 2. Refresh the UI labels with the current project data
        if (currentProject != null) {
            detailName.setText(currentProject.getName());
            detailDate.setText("Due " + currentProject.getEndDate().toString());
            loadProjectStats(currentProject.getId());
        }
    }
    @FXML
    private void onTasksTabClicked() {
        // 1. Update Tab Styles
        overviewTab.getStyleClass().setAll("tab-inactive");
        tasksBtn.getStyleClass().setAll("tab-active");

        // 2. Hide Overview
        overviewContainer.setVisible(false);
        overviewContainer.setManaged(false);

        // 3. Show Tasks
        tasksScrollPane.setVisible(true);
        tasksScrollPane.setManaged(true);

        // Refresh your task list logic here
        showTasksTab();
    }
    @FXML
    private void onOverviewTabClicked() {
        // 1. Update Tab Styles
        overviewTab.getStyleClass().setAll("tab-active");
        tasksBtn.getStyleClass().setAll("tab-inactive");

        // 2. Show Overview
        overviewContainer.setVisible(true);
        overviewContainer.setManaged(true);

        // 3. Hide Tasks (The fix for your issue)
        tasksScrollPane.setVisible(false);
        tasksScrollPane.setManaged(false);
    }
    @FXML
    private void handleAddTask() {
        Node mainLayout = tasksContainer.getScene().getRoot();
        try {
            // 1. Load the FXML for the Add Task window
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/tezfx/view/AddTaskModal.fxml"));
            Parent root = loader.load();

            // 2. Get the controller to pass the current project ID
            AddTaskController controller = loader.getController();
            controller.setProjectId(currentProject.getId());

            // 3. Create a new Stage (Window)
            Stage popupStage = new Stage();
            popupStage.initStyle(StageStyle.TRANSPARENT);
            // Create the blur effect
            BoxBlur blur = new BoxBlur(8, 8, 3);
            ColorAdjust dim = new ColorAdjust();
            dim.setBrightness(-0.3);

            // Combine them: Apply blur to dim, then apply both to the main layout
            dim.setInput(blur);
            mainLayout.setEffect(dim);


            // 4. Set Modality: This blocks the main window until this one is closed
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.initOwner(tasksContainer.getScene().getWindow());


            // 5. Show and Wait
            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            popupStage.setScene(scene);
            popupStage.centerOnScreen();
            popupStage.showAndWait();

            // 3. CLEAN UP: Remove the effect when the popup is closed
            mainLayout.setEffect(null);

            // 6. Refresh the task list once the popup is closed
            showTasksTab();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleDeleteProject(MouseEvent event) {
        if (currentProject == null) {
            return;
        }

        Node mainLayout = tasksContainer.getScene().getRoot();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/tezfx/view/DeleteProjectModal.fxml"));
            Parent root = loader.load();

            DeleteProjectModalController controller = loader.getController();
            controller.setProjectName(currentProject.getName());

            Stage popupStage = new Stage();
            popupStage.initStyle(StageStyle.TRANSPARENT);

            BoxBlur blur = new BoxBlur(8, 8, 3);
            ColorAdjust dim = new ColorAdjust();
            dim.setBrightness(-0.3);
            dim.setInput(blur);
            mainLayout.setEffect(dim);

            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.initOwner(tasksContainer.getScene().getWindow());

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            popupStage.setScene(scene);
            popupStage.centerOnScreen();
            popupStage.showAndWait();

            mainLayout.setEffect(null);

            if (controller.isConfirmed()) {
                boolean deleted = dao.deleteProjectById(currentProject.getId());
                if (deleted) {
                    MainController.setView("project.fxml");
                }
            }
        } catch (IOException e) {
            mainLayout.setEffect(null);
            e.printStackTrace();
        }
    }

    @FXML
    private void handleEditProject(MouseEvent event) {
        if (currentProject == null) {
            return;
        }

        Node mainLayout = tasksContainer.getScene().getRoot();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/tezfx/view/UpdateProject.fxml"));
            Parent root = loader.load();

            UpdateProjectModalController controller = loader.getController();
            controller.setProject(currentProject);

            Stage popupStage = new Stage();
            popupStage.initStyle(StageStyle.TRANSPARENT);

            BoxBlur blur = new BoxBlur(8, 8, 3);
            ColorAdjust dim = new ColorAdjust();
            dim.setBrightness(-0.3);
            dim.setInput(blur);
            mainLayout.setEffect(dim);

            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.initOwner(tasksContainer.getScene().getWindow());

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            popupStage.setScene(scene);
            popupStage.centerOnScreen();
            popupStage.showAndWait();

            mainLayout.setEffect(null);

            if (controller.isSaved() && controller.getUpdatedProject() != null) {
                currentProject = controller.getUpdatedProject();
                setProjectData(currentProject);
            }
        } catch (IOException e) {
            mainLayout.setEffect(null);
            e.printStackTrace();
        }
    }

    private void openUpdateTaskModal(Task task) {
        if (task == null) return;
        Node mainLayout = tasksContainer.getScene().getRoot();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/tezfx/view/UpdateTaskModal.fxml"));
            Parent root = loader.load();

            UpdateTaskModalController controller = loader.getController();
            controller.setTask(task);

            Stage popupStage = new Stage();
            popupStage.initStyle(StageStyle.TRANSPARENT);

            BoxBlur blur = new BoxBlur(8, 8, 3);
            ColorAdjust dim = new ColorAdjust();
            dim.setBrightness(-0.3);
            dim.setInput(blur);
            mainLayout.setEffect(dim);

            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.initOwner(tasksContainer.getScene().getWindow());

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            popupStage.setScene(scene);
            popupStage.centerOnScreen();
            popupStage.showAndWait();

            mainLayout.setEffect(null);

            if (controller.isSaved()) {
                showTasksTab();
                loadProjectStats(currentProject.getId());
            }
        } catch (IOException e) {
            mainLayout.setEffect(null);
            e.printStackTrace();
        }
    }

    private void deleteTask(Task task) {
        if (task == null) return;
        Node mainLayout = tasksContainer.getScene().getRoot();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/tezfx/view/DeleteTaskModal.fxml"));
            Parent root = loader.load();

            DeleteTaskModalController controller = loader.getController();
            controller.setTaskName(task.getTitle());

            Stage popupStage = new Stage();
            popupStage.initStyle(StageStyle.TRANSPARENT);

            BoxBlur blur = new BoxBlur(8, 8, 3);
            ColorAdjust dim = new ColorAdjust();
            dim.setBrightness(-0.3);
            dim.setInput(blur);
            mainLayout.setEffect(dim);

            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.initOwner(tasksContainer.getScene().getWindow());

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            popupStage.setScene(scene);
            popupStage.centerOnScreen();
            popupStage.showAndWait();

            mainLayout.setEffect(null);

            if (controller.isConfirmed() && dao.deleteTaskById(task.getId())) {
                showTasksTab();
                loadProjectStats(currentProject.getId());
            }
        } catch (IOException e) {
            mainLayout.setEffect(null);
            e.printStackTrace();
        }
    }


}
