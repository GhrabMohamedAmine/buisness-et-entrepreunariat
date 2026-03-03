package controllers.formation_quiz_result;

import entities.Formation;
import entities.Resultat;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import services.FormationService;
import utils.CertificateGenerator;

import java.io.File;

public class ResultCardController {

    @FXML private Label formationTitle;
    @FXML private Label scoreLabel;
    @FXML private Label totalLabel;
    @FXML private Label dateLabel;
    @FXML private Label statusLabel;
    @FXML private ProgressBar progressBar;

    @FXML private Button viewBtn;
    @FXML private Button downloadBtn;
    private final FormationService formationService = new FormationService();
    private Resultat resultat;
    @FXML
    private AnchorPane root;

    @FXML
    public void initialize(){

        root.setOnMouseEntered(e -> {
            root.setTranslateY(-4);
            root.setStyle(
                    "-fx-background-color:white;" +
                            "-fx-background-radius:14;" +
                            "-fx-effect:dropshadow(gaussian, rgba(109,93,252,0.35), 28, 0.35, 0, 10);"
            );
        });

        root.setOnMouseExited(e -> {
            root.setTranslateY(0);
            root.setStyle(
                    "-fx-background-color:white;" +
                            "-fx-background-radius:14;" +
                            "-fx-effect:dropshadow(gaussian, rgba(0,0,0,0.12), 18, 0.25, 0, 5);"
            );
        });
    }
    public void setData(Resultat r){
        this.resultat = r; // ✅ IMPORTANT sinon resultat reste null

        Formation f = formationService.getById(r.getFormationId());
        formationTitle.setText(f.getTitre());
        int score = r.getScore();
        int total = r.getTotal();

        scoreLabel.setText(String.valueOf(score));
        totalLabel.setText(String.valueOf(total));

        double percent = (double) score / total;
        progressBar.setProgress(percent);

        dateLabel.setText("Passé le : " + r.getDatePassage());

        // STATUS COLOR
        if(percent >= 0.7){
            statusLabel.setText("Réussi ✔");
            statusLabel.setStyle(
                    "-fx-font-weight:bold;" +
                            "-fx-padding:4 12;" +
                            "-fx-background-radius:20;" +
                            "-fx-text-fill:white;" +
                            "-fx-background-color:#22c55e;"
            );
        }
        else if(percent >= 0.5){
            statusLabel.setText("Passable");
            statusLabel.setStyle(
                    "-fx-font-weight:bold;" +
                            "-fx-padding:4 12;" +
                            "-fx-background-radius:20;" +
                            "-fx-text-fill:white;" +
                            "-fx-background-color:#f59e0b;"
            );
        }
        else{
            statusLabel.setText("Échoué");
            statusLabel.setStyle(
                    "-fx-font-weight:bold;" +
                            "-fx-padding:4 12;" +
                            "-fx-background-radius:20;" +
                            "-fx-text-fill:white;" +
                            "-fx-background-color:#ef4444;"
            );
        }
    }
    @FXML
    public void viewCertificate(){
        try{
            File file = CertificateGenerator.generate(resultat);
            java.awt.Desktop.getDesktop().open(file);
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    @FXML
    public void downloadCertificate(){
        try{
            File generated = CertificateGenerator.generate(resultat);

            FileChooser chooser = new FileChooser();
            chooser.setInitialFileName(generated.getName());

            File dest = chooser.showSaveDialog(null);

            if(dest != null){
                java.nio.file.Files.copy(
                        generated.toPath(),
                        dest.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING
                );
            }

        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
