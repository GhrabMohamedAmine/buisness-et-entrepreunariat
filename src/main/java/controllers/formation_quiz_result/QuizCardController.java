package controllers.formation_quiz_result;

import entities.Quiz;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

import java.io.File;
import java.util.function.Consumer;

public class QuizCardController {

    @FXML private Label questionLabel;
    @FXML private Label r1;
    @FXML private Label r2;
    @FXML private Label r3;
    @FXML private ImageView quizImage;
    @FXML private Button editBtn;
    @FXML private Button deleteBtn;

    private Quiz quiz;
    @FXML
    private VBox root;

    @FXML
    public void initialize() {
        String cssPath = getClass().getResource("/css/styles123.css").toExternalForm();

        // 2. Add it to the container
        root.getStylesheets().add(cssPath);
        root.setOnMouseEntered(e -> {
            root.setStyle(
                    "-fx-background-color:white;" +
                            "-fx-background-radius:14;" +
                            "-fx-padding:14;" +
                            "-fx-effect:dropshadow(gaussian, rgba(109,93,252,0.35), 25, 0.35, 0, 8);"
            );
            root.setTranslateY(-4);
        });

        root.setOnMouseExited(e -> {
            root.setStyle(
                    "-fx-background-color:white;" +
                            "-fx-background-radius:14;" +
                            "-fx-padding:14;" +
                            "-fx-effect:dropshadow(gaussian, rgba(0,0,0,0.10), 16, 0.25, 0, 5);"
            );
            root.setTranslateY(0);
        });
    }
    public void setData(Quiz q, Consumer<Quiz> onEdit, Consumer<Quiz> onDelete) {
        this.quiz = q;

        questionLabel.setText(q.getQuestion());

        r1.setText("A. " + q.getR1());
        r2.setText("B. " + q.getR2());
        r3.setText("C. " + q.getR3());

        highlightCorrect(q.getCorrect());
        String run = String.valueOf(r1.getStyleClass());
        System.out.println(run);
        // image
        if(q.getImage()!=null){
            File imgFile = new File(System.getProperty("user.home")
                    + "/pi_kavafx/quiz_images/" + q.getImage());

            if(imgFile.exists()){
                quizImage.setImage(new Image(imgFile.toURI().toString()));
                quizImage.setVisible(true);
            }
        }

        editBtn.setOnAction(e -> onEdit.accept(q));
        deleteBtn.setOnAction(e -> onDelete.accept(q));
    }

    private void highlightCorrect(int c){
        r1.getStyleClass().removeAll(".correct",".wrong");
        r2.getStyleClass().removeAll(".correct",".wrong");
        r3.getStyleClass().removeAll(".correct",".wrong");


        if(c==1) r1.getStyleClass().add(".correct"); else r1.getStyleClass().add(".wrong");
        if(c==2) r2.getStyleClass().add(".correct"); else r2.getStyleClass().add(".wrong");
        if(c==3) r3.getStyleClass().add(".correct"); else r3.getStyleClass().add(".wrong");
    }
}
