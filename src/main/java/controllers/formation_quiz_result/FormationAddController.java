package controllers.formation_quiz_result;


import entities.Formation;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import services.FormationService;
import utils.DialogUtil;
import utils.Router;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

public class FormationAddController {

    @FXML private TextField titreField;
    @FXML private TextArea descField;

    @FXML private Label titreError;
    @FXML private Label descError;

    @FXML private Button btnVideo1;
    @FXML private Button btnVideo2;
    @FXML private Button btnVideo3;
    @FXML private Button btnSave;
    @FXML private Button btnBack;

    private File v1, v2, v3;

    private final FormationService formationService = new FormationService();

    // ================= INITIALIZE =================
    @FXML
    public void initialize(){

        btnSave.setDisable(true);

        // live validation
        titreField.textProperty().addListener((obs,o,n)->{
            validateTitre();
            updateSaveButton();
        });

        descField.textProperty().addListener((obs,o,n)->{
            validateDesc();
            updateSaveButton();
        });

        // empêcher espace au début
        titreField.setTextFormatter(new TextFormatter<>(change -> {
            if(change.getControlNewText().startsWith(" "))
                return null;
            return change;
        }));
    }

    // ================= VIDEO CHOOSERS =================
    @FXML
    void chooseVideo1() { v1 = chooseVideo(btnVideo1); updateSaveButton(); }

    @FXML
    void chooseVideo2() { v2 = chooseVideo(btnVideo2); updateSaveButton(); }

    @FXML
    void chooseVideo3() { v3 = chooseVideo(btnVideo3); updateSaveButton(); }

    private File chooseVideo(Button btn) {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Video MP4", "*.mp4"));
        File f = fc.showOpenDialog(null);

        if (f != null) {
            btn.setText("✔ " + f.getName());
        }
        return f;
    }

    // ================= VALIDATION =================
    private boolean validateTitre(){

        String titre = titreField.getText().trim();

        if(titre.isEmpty()){
            titreError.setText("Le titre est obligatoire.");
            setError(titreField);
            return false;
        }

        if(titre.length() < 5){
            titreError.setText("Minimum 5 caractères.");
            setError(titreField);
            return false;
        }

        if(titre.matches(".*\\d.*")){
            titreError.setText("Le titre ne doit pas contenir de chiffres.");
            setError(titreField);
            return false;
        }

        titreError.setText("");
        setValid(titreField);
        return true;
    }

    private boolean validateDesc(){

        String desc = descField.getText().trim();

        if(desc.isEmpty()){
            descError.setText("La description est obligatoire.");
            setError(descField);
            return false;
        }

        if(desc.length() < 15){
            descError.setText("Minimum 15 caractères.");
            setError(descField);
            return false;
        }

        descError.setText("");
        setValid(descField);
        return true;
    }

    private boolean validateVideos(){

        if(v1 == null || v2 == null || v3 == null)
            return false;

        long max = 200 * 1024 * 1024; // 200MB

        if(v1.length()>max || v2.length()>max || v3.length()>max){
            DialogUtil.error("Vidéo trop grande", "Une vidéo dépasse 200MB !");
            return false;
        }

        return true;
    }

    private void updateSaveButton(){
        boolean valid = validateTitre() && validateDesc() && v1!=null && v2!=null && v3!=null;
        btnSave.setDisable(!valid);
    }

    // ================= STYLE HELPERS =================
    private void setError(Control field){
        field.getStyleClass().remove("field-valid");
        if(!field.getStyleClass().contains("field-error"))
            field.getStyleClass().add("field-error");
    }

    private void setValid(Control field){
        field.getStyleClass().remove("field-error");
        if(!field.getStyleClass().contains("field-valid"))
            field.getStyleClass().add("field-valid");
    }

    // ================= SAVE =================
    @FXML
    void saveFormation() {

        try {

            if(!validateTitre() || !validateDesc() || !validateVideos()){
                DialogUtil.error("Formulaire invalide", "Veuillez corriger les champs.");
                return;
            }

            String video1Name = copyVideoToStorage(v1);
            String video2Name = copyVideoToStorage(v2);
            String video3Name = copyVideoToStorage(v3);

            formationService.add(new Formation(
                    titreField.getText().trim(),
                    descField.getText().trim(),
                    video1Name,
                    video2Name,
                    video3Name
            ));

            DialogUtil.success("Succès", "Formation ajoutée !");
            Router.goTo("formation_list.fxml");

        } catch (Exception e) {
            DialogUtil.error("Erreur", e.getMessage());
        }
    }

    // ================= FILE COPY =================
    private String copyVideoToStorage(File source) throws Exception {

        String dirPath = System.getProperty("user.home") + "/pi_kavafx/videos/";
        File dir = new File(dirPath);

        if (!dir.exists())
            dir.mkdirs();

        String fileName = UUID.randomUUID() + "_" + source.getName();
        Path destination = Path.of(dirPath + fileName);

        Files.copy(source.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);

        return fileName;
    }

    // ================= NAVIGATION =================
    @FXML
    void goBack() {
        try {
            Router.goTo("formation_list.fxml");
        } catch (Exception e) {
            DialogUtil.error("Erreur", "Retour impossible.\n" + e.getMessage());
        }
    }
}
