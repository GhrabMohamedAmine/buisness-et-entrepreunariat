package esprit.tn.pi_kavafx.controllers;

import esprit.tn.pi_kavafx.entities.Formation;
import esprit.tn.pi_kavafx.services.FormationService;
import esprit.tn.pi_kavafx.utils.DialogUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import java.io.File;

public class FormationAddController {

    @FXML private TextField titreField;
    @FXML private TextArea descField;

    @FXML private Button btnVideo1;
    @FXML private Button btnVideo2;
    @FXML private Button btnVideo3;
    @FXML private Button btnSave;
    @FXML private Button btnBack;

    private File v1, v2, v3;

    private final FormationService formationService = new FormationService();

    @FXML
    void chooseVideo1() { v1 = chooseVideo(btnVideo1); }

    @FXML
    void chooseVideo2() { v2 = chooseVideo(btnVideo2); }

    @FXML
    void chooseVideo3() { v3 = chooseVideo(btnVideo3); }

    private File chooseVideo(Button btn) {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Video", "*.mp4"));
        File f = fc.showOpenDialog(null);
        if (f != null) btn.setText("✔ " + f.getName());
        return f;
    }

    @FXML
    void saveFormation() {

        try {

            if (v1 == null || v2 == null || v3 == null) {
                DialogUtil.error("Erreur", "Veuillez sélectionner les 3 vidéos.");
                return;
            }

            String video1Name = copyVideoToStorage(v1);
            String video2Name = copyVideoToStorage(v2);
            String video3Name = copyVideoToStorage(v3);


            formationService.add(new Formation(
                    titreField.getText(),
                    descField.getText(),
                    video1Name,
                    video2Name,
                    video3Name
            ));

            DialogUtil.success("Succès", "Formation ajoutée !");
            ParentLoader.setRoot("formation_list.fxml");

        } catch (Exception e) {
            DialogUtil.error("Erreur", e.getMessage());
        }
    }
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

    @FXML
    void goBack() {
        try {
            ParentLoader.setRoot("formation_list.fxml");
        } catch (Exception e) {
            DialogUtil.error("Erreur", "Retour impossible.\n" + e.getMessage());
        }
    }
}
