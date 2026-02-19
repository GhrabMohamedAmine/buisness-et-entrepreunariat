package controllers.formation_quiz_result;

import entities.Quiz;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

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

    public void setData(Quiz q, Consumer<Quiz> onEdit, Consumer<Quiz> onDelete) {
        this.quiz = q;

        questionLabel.setText(q.getQuestion());

        r1.setText("A. " + q.getR1());
        r2.setText("B. " + q.getR2());
        r3.setText("C. " + q.getR3());

        highlightCorrect(q.getCorrect());

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
        r1.getStyleClass().removeAll("correct","wrong");
        r2.getStyleClass().removeAll("correct","wrong");
        r3.getStyleClass().removeAll("correct","wrong");

        if(c==1) r1.getStyleClass().add("correct"); else r1.getStyleClass().add("wrong");
        if(c==2) r2.getStyleClass().add("correct"); else r2.getStyleClass().add("wrong");
        if(c==3) r3.getStyleClass().add("correct"); else r3.getStyleClass().add("wrong");
    }
}
