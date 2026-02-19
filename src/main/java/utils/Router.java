package utils;

import controllers.formation_quiz_result.ClientFormationDetailsController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;

public class Router {

    private static StackPane mainContainer;

    // called once by ProfileController
    public static void setMainContainer(StackPane pane){
        mainContainer = pane;
    }

    // universal navigation
    public static void goTo(String fxml){
        try{
            if(mainContainer == null){
                System.out.println("Router not initialized!");
                return;
            }

            FXMLLoader loader = new FXMLLoader(
                    Router.class.getResource("/fxml/" + fxml)
            );

            Parent view = loader.load();

            // store controller (important for video stop later)
            view.getProperties().put("controller", loader.getController());

            mainContainer.getChildren().setAll(view);

        }catch(Exception e){
            e.printStackTrace();
        }
    }








    // ⭐ PROFESSIONAL NAVIGATION WITH DATA
    public static void goToFormationDetails(int formationId){
        try{
            FXMLLoader loader = new FXMLLoader(
                    Router.class.getResource("/fxml/client_formation_details.fxml")
            );

            Parent view = loader.load();

            ClientFormationDetailsController controller = loader.getController();
            controller.initData(formationId);

            mainContainer.getChildren().setAll(view);

        }catch(Exception e){
            e.printStackTrace();
        }
    }

}
