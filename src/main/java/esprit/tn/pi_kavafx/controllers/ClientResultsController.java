package esprit.tn.pi_kavafx.controllers;

import esprit.tn.pi_kavafx.entities.Resultat;
import esprit.tn.pi_kavafx.services.ResultatService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import esprit.tn.pi_kavafx.services.FormationService;
import javafx.beans.property.SimpleStringProperty;

public class ClientResultsController {

    @FXML private TableView<Resultat> table;
    @FXML private TableColumn<Resultat, String> colFormation;
    @FXML private TableColumn<Resultat, Integer> colScore;
    @FXML private TableColumn<Resultat, Integer> colTotal;
    @FXML private TableColumn<Resultat, String> colDate;

    private final ResultatService service = new ResultatService();
    private final FormationService formationService = new FormationService();

     @FXML
    public void initialize(){

        // ⭐ ICI ON RÉCUPÈRE LE NOM DE FORMATION
        colFormation.setCellValueFactory(cellData -> {

            int formationId = cellData.getValue().getFormationId();

            String titre = formationService.getById(formationId).getTitre();

            return new SimpleStringProperty(titre);
        });

        colScore.setCellValueFactory(new PropertyValueFactory<>("score"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("datePassage"));

        table.setItems(FXCollections.observableArrayList(service.getAll()));
    }


}
