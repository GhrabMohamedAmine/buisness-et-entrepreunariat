package controllers.formation_quiz_result;

import entities.Quiz;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.File;
import java.util.function.BiConsumer;

public class ClientQuizCardController {

    @FXML private Button a1;
    @FXML private Button a2;
    @FXML private Button a3;
    @FXML private ImageView quizImage;
    @FXML private javafx.scene.control.Label questionLabel;

    private int selected = 0;

    public void setData(Quiz q, BiConsumer<Quiz,Integer> onAnswer){

        questionLabel.setText(q.getQuestion());

        a1.setText("A. " + q.getR1());
        a2.setText("B. " + q.getR2());
        a3.setText("C. " + q.getR3());

        setupAnswer(a1,1,q,onAnswer);
        setupAnswer(a2,2,q,onAnswer);
        setupAnswer(a3,3,q,onAnswer);

        // image
        if(q.getImage()!=null){
            File imgFile = new File(System.getProperty("user.home")
                    + "/pi_kavafx/quiz_images/" + q.getImage());

            if(imgFile.exists()){
                quizImage.setImage(new Image(imgFile.toURI().toString(),500,240,false,true));
                quizImage.setVisible(true);
            }
        }
    }

    private void setupAnswer(Button btn,int index,Quiz q,BiConsumer<Quiz,Integer> onAnswer){

        btn.setOnAction(e->{
            clearSelection();
            btn.getStyleClass().add("selected-answer");
            selected=index;
            onAnswer.accept(q,index);
        });
    }

    private void clearSelection(){
        a1.getStyleClass().remove("selected-answer");
        a2.getStyleClass().remove("selected-answer");
        a3.getStyleClass().remove("selected-answer");
    }
}
