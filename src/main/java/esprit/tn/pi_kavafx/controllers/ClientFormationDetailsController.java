package esprit.tn.pi_kavafx.controllers;

import esprit.tn.pi_kavafx.entities.Formation;
import esprit.tn.pi_kavafx.entities.Quiz;
import esprit.tn.pi_kavafx.entities.Resultat;
import esprit.tn.pi_kavafx.services.FormationService;
import esprit.tn.pi_kavafx.services.QuizService;
import esprit.tn.pi_kavafx.services.ResultatService;
import esprit.tn.pi_kavafx.utils.DialogUtil;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;

import java.io.File;
import java.util.*;

public class ClientFormationDetailsController {

    @FXML private Label titreLabel;
    @FXML private Label descLabel;

    @FXML private MediaView mv1;
    @FXML private MediaView mv2;
    @FXML private MediaView mv3;

    @FXML private VBox quizBox;
    @FXML private MediaView mainVideo;
    @FXML private Button playBtn;
    @FXML private Button pauseBtn;
    @FXML private Button stopBtn;
    @FXML private Slider progressSlider;
    @FXML private Label timeLabel;

    @FXML private Button video1Btn;
    @FXML private Button video2Btn;
    @FXML private Button video3Btn;

    private MediaPlayer currentPlayer;
    private Formation currentFormation;

    private final FormationService formationService = new FormationService();
    private final QuizService quizService = new QuizService();
    private final ResultatService resultatService = new ResultatService();

    private int formationId;

    private Map<Quiz, ToggleGroup> answers = new HashMap<>();

    public void setFormationId(int id){
        formationId = id;
        load();
    }

    private void load(){

        currentFormation = formationService.getById(formationId);

        titreLabel.setText(currentFormation.getTitre());
        descLabel.setText(currentFormation.getDescription());

        // boutons playlist
        video1Btn.setOnAction(e -> loadVideo(currentFormation.getVideo1()));
        video2Btn.setOnAction(e -> loadVideo(currentFormation.getVideo2()));
        video3Btn.setOnAction(e -> loadVideo(currentFormation.getVideo3()));

        // charger la première vidéo automatiquement
        loadVideo(currentFormation.getVideo1());

        loadQuiz();
    }
    private void loadQuiz(){

        quizBox.getChildren().clear();
        answers.clear();

        List<Quiz> quizzes = quizService.getByFormationId(formationId);

        for(Quiz q : quizzes){

            VBox card = new VBox(10);
            card.getStyleClass().add("card");

            Label question = new Label(q.getQuestion());
            question.getStyleClass().add("card-title");
            question.setWrapText(true);

            ToggleGroup group = new ToggleGroup();

            RadioButton r1 = new RadioButton(q.getR1());
            RadioButton r2 = new RadioButton(q.getR2());
            RadioButton r3 = new RadioButton(q.getR3());

            r1.setToggleGroup(group);
            r2.setToggleGroup(group);
            r3.setToggleGroup(group);

            r1.setWrapText(true);
            r2.setWrapText(true);
            r3.setWrapText(true);

            answers.put(q,group);

            card.getChildren().addAll(question,r1,r2,r3);
            quizBox.getChildren().add(card);
        }
    }

    private void loadVideo(String filename){

        try{

            if(currentPlayer != null){
                currentPlayer.stop();
                currentPlayer.dispose();
            }

            String baseDir = System.getProperty("user.home") + "/pi_kavafx/videos/";
            File video = new File(baseDir + filename);

            Media media = new Media(video.toURI().toString());
            currentPlayer = new MediaPlayer(media);

            mainVideo.setMediaPlayer(currentPlayer);

            // PLAY
            playBtn.setOnAction(e -> currentPlayer.play());

            // PAUSE ⭐
            pauseBtn.setOnAction(e -> currentPlayer.pause());

            // STOP
            stopBtn.setOnAction(e -> currentPlayer.stop());

            // PROGRESS BAR
            currentPlayer.currentTimeProperty().addListener((obs,oldTime,newTime)->{
                if(!progressSlider.isValueChanging()){
                    progressSlider.setValue(
                            newTime.toSeconds() /
                                    currentPlayer.getTotalDuration().toSeconds() * 100
                    );
                }

                updateTimeLabel();
            });

            progressSlider.valueProperty().addListener((obs,oldVal,newVal)->{
                if(progressSlider.isValueChanging()){
                    currentPlayer.seek(
                            currentPlayer.getTotalDuration().multiply(newVal.doubleValue()/100)
                    );
                }
            });

        }catch(Exception e){
            e.printStackTrace();
        }
    }
    private void updateTimeLabel(){

        if(currentPlayer == null) return;

        int current = (int) currentPlayer.getCurrentTime().toSeconds();
        int total = (int) currentPlayer.getTotalDuration().toSeconds();

        String c = String.format("%02d:%02d", current/60, current%60);
        String t = String.format("%02d:%02d", total/60, total%60);

        timeLabel.setText(c + " / " + t);
    }
    public void stopPlayers(){
        if(currentPlayer != null){
            currentPlayer.stop();
            currentPlayer.dispose();
        }
    }


    @FXML
    private void submitQuiz(){

        int score=0;
        int total=answers.size();

        for(Quiz q:answers.keySet()){

            ToggleGroup g = answers.get(q);
            RadioButton selected = (RadioButton) g.getSelectedToggle();

            if(selected==null) continue;

            int index =
                    selected.getText().equals(q.getR1())?1:
                            selected.getText().equals(q.getR2())?2:3;

            if(index==q.getCorrect())
                score++;
        }

        resultatService.save(new Resultat(formationId,score,total));

        DialogUtil.success("Résultat",
                "Votre score : "+score+" / "+total);
    }
}
