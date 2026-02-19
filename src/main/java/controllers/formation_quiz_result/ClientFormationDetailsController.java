package controllers.formation_quiz_result;

import entities.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import services.*;
import utils.DialogUtil;

import java.io.File;
import java.util.*;

public class ClientFormationDetailsController {

    @FXML private Label titreLabel;
    @FXML private Label descLabel;
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

    private final FormationService formationService = new FormationService();
    private final QuizService quizService = new QuizService();
    private final ResultatService resultatService = new ResultatService();
    private final ParticiperService participerService = new ParticiperService();
    private final int userId = UserService.getCurrentUser().getId();

    private Formation formation;
    private Participer participation;
    private MediaPlayer player;

    private Map<Quiz,Integer> selectedAnswers = new HashMap<>();

    // ⭐ Called by Router
    public void initData(int formationId){
        loadFormation(formationId);
    }

    private void loadFormation(int formationId){

        // 1️⃣ Load formation
        formation = formationService.getById(formationId);
        if(formation == null){
            DialogUtil.error("Erreur","Formation introuvable");
            return;
        }

        // 2️⃣ Ensure user is enrolled (safe)
        participerService.inscrire(userId,formationId);

        // 3️⃣ Fetch participation FROM DB
        participation = participerService.getParticipation(userId,formationId);

        // 🔐 Ultimate security
        if(participation == null){
            DialogUtil.error("Erreur",
                    "Impossible de récupérer votre progression. Veuillez réessayer.");
            return;
        }

        // 4️⃣ Now we can safely use participation
        titreLabel.setText(formation.getTitre());
        descLabel.setText(formation.getDescription());

        // Playlist buttons
        video1Btn.setOnAction(e->loadVideo(formation.getVideo1(),33));
        video2Btn.setOnAction(e->loadVideo(formation.getVideo2(),66));
        video3Btn.setOnAction(e->loadVideo(formation.getVideo3(),90));

        // Start first video
        loadVideo(formation.getVideo1(),33);

        // 5️⃣ Already passed course
        if("REUSSI".equals(participation.getStatut())){
            quizBox.setDisable(true);
            quizBox.setOpacity(0.6);

            DialogUtil.success("Information",
                    "Vous avez déjà validé cette formation ✔");
            return;
        }

        // 6️⃣ Lock or unlock quiz
        if(participation.getProgression() < 90){

            quizBox.setDisable(true);
            quizBox.setOpacity(.5);

            DialogUtil.confirm(
                    "Quiz verrouillé",
                    "Vous devez regarder toutes les vidéos avant de passer le quiz.\n" +
                            "Progression actuelle : " + participation.getProgression() + "%"
            );

        }else{
            loadQuiz();
        }
    }

    private void loadVideo(String filename,int progressValue){

        try{
            if(player!=null){
                player.stop();
                player.dispose();
            }

            File video=new File(System.getProperty("user.home")+"/pi_kavafx/videos/"+filename);
            if(!video.exists()) return;

            Media media=new Media(video.toURI().toString());
            player=new MediaPlayer(media);
            mainVideo.setMediaPlayer(player);

            playBtn.setOnAction(e->player.play());
            pauseBtn.setOnAction(e->player.pause());
            stopBtn.setOnAction(e->player.stop());

            player.setOnEndOfMedia(()->{

                if(participation.getProgression()<progressValue){
                    participerService.updateProgress(userId,formation.getId(),progressValue);
                    participation.setProgression(progressValue);
                }

                if(progressValue>=90){
                    quizBox.setDisable(false);
                    quizBox.setOpacity(1);
                    loadQuiz();
                }
            });

        }catch(Exception e){e.printStackTrace();}
    }

    private void loadQuiz(){

        quizBox.getChildren().clear();
        selectedAnswers.clear();

        for(Quiz q:quizService.getByFormationId(formation.getId())){
            try{
                FXMLLoader loader=new FXMLLoader(getClass().getResource("/fxml/client_quiz_card.fxml"));
                VBox card=loader.load();
                ClientQuizCardController c=loader.getController();
                c.setData(q,(quiz,ans)->selectedAnswers.put(quiz,ans));
                quizBox.getChildren().add(card);
            }catch(Exception e){e.printStackTrace();}
        }
    }

    @FXML
    private void submitQuiz(){

        // 🔐 HARD VALIDATION (cannot cheat)
        if(!canPassQuiz())
            return;

        if(selectedAnswers.isEmpty()){
            DialogUtil.error("Quiz",
                    "Veuillez répondre aux questions avant de soumettre.");
            return;
        }

        int score = 0;

        for(Quiz q : selectedAnswers.keySet()){
            if(selectedAnswers.get(q) == q.getCorrect())
                score++;
        }

        int total = selectedAnswers.size();

        resultatService.save(new Resultat(formation.getId(),score,total));
        try{
            participerService.finishFormation(userId,formation.getId(),score,total);
        }catch(RuntimeException ex){
            DialogUtil.error("Quiz bloqué",
                    "Vous avez déjà validé cette formation.\n" +
                            "Il n'est pas possible de repasser le quiz.");
            return;
        }

        double percent = (score*100.0)/total;

        if(percent >= 60){
            DialogUtil.success(
                    "Félicitations 🎓",
                    "Vous avez validé la formation !\nScore : "
                            + score + "/" + total +
                            "\nRéussite : " + (int)percent + "%"
            );
        }else{
            DialogUtil.error(
                    "Formation non validée",
                    "Score : " + score + "/" + total +
                            "\nIl faut au moins 60% pour valider la formation."
            );
        }
    }


    private boolean canPassQuiz(){

        if(participation == null)
            return false;

        // must watch all videos
        if(participation.getProgression() < 90){
            DialogUtil.error(
                    "Quiz verrouillé",
                    "Vous devez regarder toutes les vidéos avant de passer le quiz.\n" +
                            "Progression actuelle : " + participation.getProgression() + "%"
            );
            return false;
        }

        // already validated
        if("REUSSI".equals(participation.getStatut())){
            DialogUtil.success(
                    "Formation déjà validée",
                    "Vous avez déjà réussi cette formation ✔"
            );
            return false;
        }

        return true;
    }

}
