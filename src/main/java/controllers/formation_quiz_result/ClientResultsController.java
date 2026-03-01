package controllers.formation_quiz_result;


import entities.Resultat;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.AnchorPane;
import services.ResultatService;

import java.io.IOException;
import java.util.List;

public class ClientResultsController {

    @FXML
    private FlowPane cardsFlow;

    private final ResultatService service = new ResultatService();

    @FXML
    public void initialize() {

        List<Resultat> results = service.getAll();

        for(Resultat r : results){

            try {

                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/fxml/result_card.fxml")
                );

                AnchorPane card = loader.load();

                ResultCardController controller = loader.getController();
                controller.setData(r);

                cardsFlow.getChildren().add(card);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
