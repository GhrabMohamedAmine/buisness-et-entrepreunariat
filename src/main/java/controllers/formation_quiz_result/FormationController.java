package controllers.formation_quiz_result;

import entities.Formation;
import entities.Quiz;
import services.FormationService;
import services.QuizService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import utils.Router;

import java.io.File;

public class FormationController {

    @FXML private VBox cardsContainer;

    @FXML private TextField titreField, questionField, r1, r2, r3;
    @FXML private TextArea descField;

    private File v1, v2, v3, quizImage;
    private int formationId;

    FormationService formationService = new FormationService();
    QuizService quizService = new QuizService();

    @FXML
    public void initialize() {
        loadFormations();
    }

    // ================= FORMATIONS =================

    private void loadFormations() {
        cardsContainer.getChildren().clear();

        for (Formation f : formationService.getAll()) {
            VBox card = new VBox(10);
            card.setStyle("""
                -fx-background-color:white;
                -fx-padding:15;
                -fx-background-radius:10;
                -fx-effect:dropshadow(gaussian, rgba(0,0,0,0.2),10,0,0,4);
            """);

            Label title = new Label(f.getTitre());
            title.setStyle("-fx-font-size:18; -fx-font-weight:bold");

            Label desc = new Label(f.getDescription());

            Button addQuiz = new Button("➕ Ajouter Quiz");
            addQuiz.setOnAction(e -> openQuizAdd(f.getId()));

            card.getChildren().addAll(title, desc, addQuiz);
            cardsContainer.getChildren().add(card);
        }
    }

    private void openQuizAdd(int id) {
        formationId = id;
        Router.goTo("quiz_add.fxml");
    }

    // ================= FILE CHOOSERS =================

    @FXML void chooseVideo1() { v1 = chooseFile("Video"); }
    @FXML void chooseVideo2() { v2 = chooseFile("Video"); }
    @FXML void chooseVideo3() { v3 = chooseFile("Video"); }
    @FXML void chooseQuizImage() { quizImage = chooseFile("Image"); }

    private File chooseFile(String type) {
        FileChooser fc = new FileChooser();
        if (type.equals("Video"))
            fc.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Video", "*.mp4")
            );
        else
            fc.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Image", "*.png", "*.jpg")
            );
        return fc.showOpenDialog(null);
    }

    // ================= SAVE =================

    @FXML
    void saveFormation() {
        formationService.add(new Formation(
                titreField.getText(),
                descField.getText(),
                v1.getAbsolutePath(),
                v2.getAbsolutePath(),
                v3.getAbsolutePath()
        ));
        Router.goTo("formation_list.fxml");
    }

    @FXML
    void saveQuiz() {

    }



}
