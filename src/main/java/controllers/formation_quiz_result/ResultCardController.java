package controllers.formation_quiz_result;

import entities.Resultat;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
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

    private Resultat resultat;

    public void setData(Resultat r){

        this.resultat = r;

        scoreLabel.setText(String.valueOf(r.getScore()));
        totalLabel.setText(String.valueOf(r.getTotal()));
        dateLabel.setText(r.getDatePassage().toString());

        double percent = (r.getScore()*100.0)/r.getTotal();
        progressBar.setProgress(percent/100.0);

        if(percent >= 60){
            statusLabel.setText("Réussi ✔");
            statusLabel.setStyle("-fx-text-fill: #16a34a;");

            viewBtn.setVisible(true);
            downloadBtn.setVisible(true);

            viewBtn.setOnAction(e -> viewCertificate());
            downloadBtn.setOnAction(e -> downloadCertificate());
        }
        else{
            statusLabel.setText("Échoué");
            statusLabel.setStyle("-fx-text-fill: #dc2626;");

            viewBtn.setVisible(false);
            downloadBtn.setVisible(false);
        }
    }

    private void viewCertificate(){
        try{
            File file = CertificateGenerator.generate(resultat);
            java.awt.Desktop.getDesktop().open(file);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private void downloadCertificate(){
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
