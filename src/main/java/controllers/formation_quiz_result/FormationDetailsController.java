package controllers.formation_quiz_result;

import entities.Formation;
import entities.Quiz;
import javafx.fxml.FXMLLoader;
import services.FormationService;
import services.QuizService;
import utils.DialogUtil;
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
import utils.Router;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
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

            try {

                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/fxml/quiz_card.fxml")
                );

                VBox card = loader.load();

                QuizCardController controller = loader.getController();

                controller.setData(q,
                        this::openEditDialog,
                        quizToDelete -> {
                            if(DialogUtil.confirm("Supprimer","Confirmer suppression ?")){
                                quizService.delete(quizToDelete.getId());
                                refreshQuizList();
                            }
                        }
                );

                quizCards.getChildren().add(card);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    private static int selectedFormationId;

    public static void setSelectedFormation(int id){
        selectedFormationId = id;
    }

    @FXML
    public void initialize(){
        setFormationId(selectedFormationId);
    }

    @FXML
    void onAjouterQuiz() {

        try {

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Ajouter Quiz");

            ButtonType addBtn = new ButtonType("Ajouter", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(addBtn, ButtonType.CANCEL);

            // ===== CHAMPS =====
            TextField question = new TextField();
            question.setPromptText("Question");

            Label qError = new Label();
            qError.getStyleClass().add("error-label");

            TextField r1 = new TextField();
            r1.setPromptText("Réponse 1");
            Label r1Error = new Label();
            r1Error.getStyleClass().add("error-label");

            TextField r2 = new TextField();
            r2.setPromptText("Réponse 2");
            Label r2Error = new Label();
            r2Error.getStyleClass().add("error-label");

            TextField r3 = new TextField();
            r3.setPromptText("Réponse 3");
            Label r3Error = new Label();
            r3Error.getStyleClass().add("error-label");

            ComboBox<String> correct = new ComboBox<>();
            correct.getItems().addAll("Réponse 1","Réponse 2","Réponse 3");

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

            VBox box = new VBox(8,
                    new Label("Question"), question, qError,
                    new Label("Réponse 1"), r1, r1Error,
                    new Label("Réponse 2"), r2, r2Error,
                    new Label("Réponse 3"), r3, r3Error,
                    new Label("Bonne réponse"), correct,
                    new VBox(6, chooseImg, imgPath)
            );

            dialog.getDialogPane().setContent(box);

            // ===== VALIDATION LIVE =====
            Button addButton = (Button) dialog.getDialogPane().lookupButton(addBtn);
            addButton.setDisable(true);

            Runnable validator = () -> {

                boolean valid = true;

                // question
                if(question.getText().trim().length() < 10){
                    qError.setText("Minimum 10 caractères.");
                    valid = false;
                } else qError.setText("");

                // réponses
                if(r1.getText().trim().length() < 2){
                    r1Error.setText("Réponse trop courte");
                    valid = false;
                } else r1Error.setText("");

                if(r2.getText().trim().length() < 2){
                    r2Error.setText("Réponse trop courte");
                    valid = false;
                } else r2Error.setText("");

                if(r3.getText().trim().length() < 2){
                    r3Error.setText("Réponse trop courte");
                    valid = false;
                } else r3Error.setText("");

                // réponses différentes
                if(r1.getText().equalsIgnoreCase(r2.getText())
                        || r1.getText().equalsIgnoreCase(r3.getText())
                        || r2.getText().equalsIgnoreCase(r3.getText())){

                    r3Error.setText("Les réponses doivent être différentes !");
                    valid = false;
                }

                // bonne réponse
                if(correct.getValue() == null){
                    valid = false;
                }

                addButton.setDisable(!valid);
            };

            question.textProperty().addListener((o,a,b)->validator.run());
            r1.textProperty().addListener((o,a,b)->validator.run());
            r2.textProperty().addListener((o,a,b)->validator.run());
            r3.textProperty().addListener((o,a,b)->validator.run());
            correct.valueProperty().addListener((o,a,b)->validator.run());

            // ===== INTERCEPT OK BUTTON =====
            addButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {

                if(addButton.isDisabled()){
                    event.consume();
                }
            });

            // ===== RESULT =====
            if(dialog.showAndWait().orElse(ButtonType.CANCEL) != addBtn)
                return;

            int correctIndex = correct.getSelectionModel().getSelectedIndex() + 1;

            String imageName = null;
            if (img[0] != null)
                imageName = saveQuizImage(img[0]);

            quizService.add(new Quiz(
                    question.getText().trim(),
                    r1.getText().trim(),
                    r2.getText().trim(),
                    r3.getText().trim(),
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
            Router.goTo("formation_list.fxml");
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
