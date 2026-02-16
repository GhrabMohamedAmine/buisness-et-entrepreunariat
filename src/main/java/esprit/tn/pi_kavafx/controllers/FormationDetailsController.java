package esprit.tn.pi_kavafx.controllers;

import esprit.tn.pi_kavafx.entities.Formation;
import esprit.tn.pi_kavafx.entities.Quiz;
import esprit.tn.pi_kavafx.services.FormationService;
import esprit.tn.pi_kavafx.services.QuizService;
import esprit.tn.pi_kavafx.utils.DialogUtil;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

public class FormationDetailsController {

    @FXML private Label titreLabel;
    @FXML private Label descLabel;

    @FXML private MediaView mv1;
    @FXML private MediaView mv2;
    @FXML private MediaView mv3;
    @FXML
    private FlowPane quizCards;

    @FXML private Button btnAjouterQuiz;
    @FXML private Button btnBack;

    @FXML private ListView<String> quizList;

    private int formationId;

    private final FormationService formationService = new FormationService();
    private final QuizService quizService = new QuizService();

    private MediaPlayer p1, p2, p3;

    public void setFormationId(int id) {
        this.formationId = id;
        loadData();
    }

    private void loadData() {
        try {
            Formation f = formationService.getById(formationId);
            if (f == null) {
                DialogUtil.error("Erreur", "Formation introuvable.");
                goBack();
                return;
            }

            titreLabel.setText(f.getTitre());
            descLabel.setText(f.getDescription());

            bindVideo(mv1, f.getVideo1());
            bindVideo(mv2, f.getVideo2());
            bindVideo(mv3, f.getVideo3());


            refreshQuizList();
        } catch (Exception e) {
            DialogUtil.error("Erreur", "Chargement détails impossible.\n" + e.getMessage());
        }
    }

    private void bindVideo(MediaView mv, String filename) {
        if (filename != null)
            makePlayable(mv, filename);
    }

    private void refreshQuizList() {

        quizCards.getChildren().clear();
        List<Quiz> quizzes = quizService.getByFormationId(formationId);

        for (Quiz q : quizzes) {

            VBox card = new VBox(10);
            card.getStyleClass().add("card");
            card.setPrefWidth(280);

            // QUESTION
            Label question = new Label(q.getQuestion());
            question.getStyleClass().add("card-title");
            question.setWrapText(true);

            // IMAGE
            if(q.getImage()!=null){
                File imgFile = new File(System.getProperty("user.home")
                        + "/pi_kavafx/quiz_images/" + q.getImage());

                if(imgFile.exists()){
                    ImageView iv = new ImageView(imgFile.toURI().toString());
                    iv.setFitWidth(240);
                    iv.setPreserveRatio(true);
                    card.getChildren().add(iv);
                }
            }

            // REPONSES
            Label r1 = new Label("• " + q.getR1());
            Label r2 = new Label("• " + q.getR2());
            Label r3 = new Label("• " + q.getR3());

            r1.getStyleClass().add(q.getCorrect()==1 ? "correct-answer":"wrong-answer");
            r2.getStyleClass().add(q.getCorrect()==2 ? "correct-answer":"wrong-answer");
            r3.getStyleClass().add(q.getCorrect()==3 ? "correct-answer":"wrong-answer");

            // BOUTONS
            Button edit = new Button("Modifier");
            edit.getStyleClass().add("btn-outline");

            Button delete = new Button("Supprimer");
            delete.getStyleClass().add("btn-danger");
            edit.setOnAction(e -> openEditDialog(q));  // ⭐ LA LIGNE QUI MANQUE

            delete.setOnAction(e->{
                if(DialogUtil.confirm("Supprimer","Confirmer suppression ?")){
                    quizService.delete(q.getId());
                    refreshQuizList();
                }
            });

            HBox actions = new HBox(8, edit, delete);

            card.getChildren().addAll(question,r1,r2,r3,actions);
            quizCards.getChildren().add(card);
        }
    }

    @FXML
    void onAjouterQuiz() {

        try {

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Ajouter Quiz");

            // boutons
            ButtonType addBtn = new ButtonType("Ajouter", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(addBtn, ButtonType.CANCEL);

            // champs
            TextField question = new TextField();
            question.setPromptText("Question");

            TextField r1 = new TextField();
            r1.setPromptText("Réponse 1");

            TextField r2 = new TextField();
            r2.setPromptText("Réponse 2");

            TextField r3 = new TextField();
            r3.setPromptText("Réponse 3");

            // ⭐ choix bonne réponse
            ComboBox<String> correct = new ComboBox<>();
            correct.getItems().addAll("Réponse 1", "Réponse 2", "Réponse 3");
            correct.setValue("Réponse 1");

            // image
            Button chooseImg = new Button("Choisir image");
            chooseImg.getStyleClass().add("btn-outline");

            Label imgPath = new Label("Aucune image");

            final File[] img = new File[1];

            chooseImg.setOnAction(ev -> {
                FileChooser fc = new FileChooser();
                fc.getExtensionFilters().add(
                        new FileChooser.ExtensionFilter("Image", "*.png", "*.jpg", "*.jpeg")
                );

                File f = fc.showOpenDialog(null);

                if (f != null) {
                    img[0] = f;
                    imgPath.setText(f.getName());
                }
            });

            // layout
            VBox box = new VBox(12,
                    new Label("Ajouter un quiz à cette formation :"),
                    question,
                    r1,
                    r2,
                    r3,
                    new Label("Bonne réponse :"),
                    correct,
                    new VBox(6, chooseImg, imgPath)
            );

            dialog.getDialogPane().setContent(box);

            // attendre validation
            ButtonType res = dialog.showAndWait().orElse(ButtonType.CANCEL);
            if (res != addBtn) return;

            // validation
            if (question.getText().isBlank()
                    || r1.getText().isBlank()
                    || r2.getText().isBlank()
                    || r3.getText().isBlank()) {

                DialogUtil.error("Erreur", "Tous les champs sont obligatoires !");
                return;
            }

            // index bonne réponse (1,2,3)
            int correctIndex = correct.getSelectionModel().getSelectedIndex() + 1;

            // sauvegarde image
            String imageName = null;
            if (img[0] != null)
                imageName = saveQuizImage(img[0]);

            // insertion DB
            quizService.add(new Quiz(
                    question.getText(),
                    r1.getText(),
                    r2.getText(),
                    r3.getText(),
                    imageName,
                    formationId,
                    correctIndex
            ));

            DialogUtil.success("Succès", "Quiz ajouté avec succès ✅");

            refreshQuizList();

        } catch (Exception e) {
            DialogUtil.error("Erreur", "Ajout quiz impossible.\n" + e.getMessage());
            e.printStackTrace();
        }
    }

    private void makePlayable(MediaView mv, String filename) {

        try {

            String baseDir = System.getProperty("user.home") + "/pi_kavafx/videos/";
            File video = new File(baseDir + filename);

            if (!video.exists()) {
                System.out.println("VIDEO INTROUVABLE -> " + video.getAbsolutePath());
                return;
            }

            String uri = video.toURI().toString();

            Media media = new Media(uri);
            MediaPlayer player = new MediaPlayer(media);

            mv.setMediaPlayer(player);

            // STORE PLAYER (CRITICAL)
            if (mv == mv1) p1 = player;
            if (mv == mv2) p2 = player;
            if (mv == mv3) p3 = player;

            mv.setOnMouseClicked(e -> {
                if (player.getStatus() == MediaPlayer.Status.PLAYING)
                    player.pause();
                else
                    player.play();
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private String saveQuizImage(File img) throws Exception {

        String dir = System.getProperty("user.home") + "/pi_kavafx/quiz_images/";
        File folder = new File(dir);
        if(!folder.exists()) folder.mkdirs();

        String name = UUID.randomUUID() + "_" + img.getName();

        Files.copy(img.toPath(),
                Path.of(dir + name),
                StandardCopyOption.REPLACE_EXISTING);

        return name;
    }

    @FXML
    void goBack() {
        stopPlayers();
        try {
            ParentLoader.setRoot("formation_list.fxml");
            } catch (Exception e) {
            DialogUtil.error("Erreur", "Retour impossible.\n" + e.getMessage());
        }
    }

    public void stopPlayers() {
        try { if (p1 != null) p1.stop(); } catch (Exception ignored) {}
        try { if (p2 != null) p2.stop(); } catch (Exception ignored) {}
        try { if (p3 != null) p3.stop(); } catch (Exception ignored) {}
    }
    private void openEditDialog(Quiz q){

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Modifier Quiz");

        TextField question = new TextField(q.getQuestion());
        TextField r1 = new TextField(q.getR1());
        TextField r2 = new TextField(q.getR2());
        TextField r3 = new TextField(q.getR3());

        ComboBox<String> correct = new ComboBox<>();
        correct.getItems().addAll("Réponse 1","Réponse 2","Réponse 3");
        correct.getSelectionModel().select(q.getCorrect()-1);

        VBox box = new VBox(10,
                new Label("Question"),question,
                new Label("R1"),r1,
                new Label("R2"),r2,
                new Label("R3"),r3,
                new Label("Bonne réponse"),correct
        );

        dialog.getDialogPane().setContent(box);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK,ButtonType.CANCEL);

        if(dialog.showAndWait().orElse(ButtonType.CANCEL)==ButtonType.OK){

            int c = correct.getSelectionModel().getSelectedIndex()+1;

            quizService.update(new Quiz(
                    q.getId(),
                    question.getText(),
                    r1.getText(),
                    r2.getText(),
                    r3.getText(),
                    q.getImage(),
                    formationId,
                    c
            ));

            refreshQuizList();
        }
    }

}
