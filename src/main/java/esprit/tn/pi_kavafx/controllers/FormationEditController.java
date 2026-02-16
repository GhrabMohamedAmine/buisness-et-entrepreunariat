package esprit.tn.pi_kavafx.controllers;

import esprit.tn.pi_kavafx.entities.Formation;
import esprit.tn.pi_kavafx.services.FormationService;
import esprit.tn.pi_kavafx.utils.DialogUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

public class FormationEditController {

    @FXML private TextField titreField;
    @FXML private TextArea descField;

    @FXML private Button btnVideo1;
    @FXML private Button btnVideo2;
    @FXML private Button btnVideo3;

    @FXML private Button btnSave;
    @FXML private Button btnBack;

    private int formationId;
    private File v1, v2, v3;

    private final FormationService formationService = new FormationService();

    public void setFormationId(int id) {
        this.formationId = id;
        load();
    }

    private void load() {
        try {
            Formation f = formationService.getById(formationId);
            if (f == null) {
                DialogUtil.error("Erreur", "Formation introuvable.");
                goBack();
                return;
            }

            titreField.setText(f.getTitre());
            descField.setText(f.getDescription());

            // videos existantes
            if (f.getVideo1() != null) btnVideo1.setText("✔ " + new File(f.getVideo1()).getName());
            if (f.getVideo2() != null) btnVideo2.setText("✔ " + new File(f.getVideo2()).getName());
            if (f.getVideo3() != null) btnVideo3.setText("✔ " + new File(f.getVideo3()).getName());

            v1 = f.getVideo1() != null ? new File(f.getVideo1()) : null;
            v2 = f.getVideo2() != null ? new File(f.getVideo2()) : null;
            v3 = f.getVideo3() != null ? new File(f.getVideo3()) : null;

        } catch (Exception e) {
            DialogUtil.error("Erreur", "Chargement impossible.\n" + e.getMessage());
        }
    }

    @FXML void chooseVideo1() { v1 = chooseVideo(btnVideo1); }
    @FXML void chooseVideo2() { v2 = chooseVideo(btnVideo2); }
    @FXML void chooseVideo3() { v3 = chooseVideo(btnVideo3); }

    private File chooseVideo(Button btn) {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Video", "*.mp4"));
        File f = fc.showOpenDialog(null);
        if (f != null) btn.setText("✔ " + f.getName());
        return f;
    }

    @FXML
    void save() {
        try {

            if (titreField.getText().isBlank() || descField.getText().isBlank()) {
                DialogUtil.error("Erreur", "Titre et description sont obligatoires.");
                return;
            }

            // copy videos and get filenames
            String video1Name = copyVideoToStorage(v1);
            String video2Name = copyVideoToStorage(v2);
            String video3Name = copyVideoToStorage(v3);

            Formation f = new Formation(
                    formationId,
                    titreField.getText(),
                    descField.getText(),
                    video1Name,
                    video2Name,
                    video3Name
            );

            formationService.update(f);

            DialogUtil.success("Succès", "Formation modifiée avec succès ✅");

            ParentLoader.setRoot("formation_list.fxml");

        } catch (Exception e) {
            e.printStackTrace();
            DialogUtil.error("Erreur", "Modification impossible.\n" + e.getMessage());
        }
    }
    @FXML
    void goBack() {
        try {
            ParentLoader.setRoot("formation_list.fxml");
        } catch (Exception e) {
            DialogUtil.error("Erreur", "Retour impossible.\n" + e.getMessage());
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

}
