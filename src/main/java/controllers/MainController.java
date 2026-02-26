package controllers;

import entities.User;
import javafx.scene.Parent;
import Mains.ViewLoader;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import services.UserService;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static controllers.ProfileController.DEFAULT_COLOR;


public class MainController {
    @FXML private StackPane contentArea;
    private static StackPane staticContentArea;
    @FXML
    private BorderPane mainView;
    @FXML
    private Label userName,role;
    @FXML
    private Circle Pfimage;
    private UserService service =  new UserService();
    private User currentuser = service.getCurrentUser();

    private static MainController instance;

    public static void setView(String fxmlFileName) {
        Node node = ViewLoader.load(fxmlFileName);
        if (node != null && staticContentArea != null) {
            staticContentArea.getChildren().setAll(node);
        }
    }
    public static StackPane getStaticContentArea() {
        return staticContentArea;
    }

    public static void setContent(Parent root) {
        if (root != null && staticContentArea != null) {
            staticContentArea.getChildren().setAll(root);
        }
    }

    @FXML
    public void initialize() {
        instance = this;
//        mainView.getStylesheets().addAll(
//                getClass().getResource("/styles/stylesheet.css").toExternalForm(),
//                getClass().getResource("/styles/buttons.css").toExternalForm(),
//                getClass().getResource("/styles/project-css.css").toExternalForm()
//        );
        staticContentArea = contentArea;
        //currentuser = service.getCurrentUser();
        System.out.println("Current user: " + currentuser.toString());
        setUserinfo();

    }
    public void Reclamations(){
        setView("/UserReclamations/UserReclamations.fxml");
    }

    public static MainController getInstance() {
        return instance;
    }

    public void setUserinfo(){
        if (currentuser == null){
            currentuser = service.getCurrentUser();
        }

        userName.setText(currentuser.getFullName());
        role.setText(currentuser.getRole());
        setImage();
    }

    public void setImage(){
        if (currentuser != null) {

            if (Pfimage != null) {
                byte[] imageData = currentuser.getImageData();
                ImagePattern pattern = null;

                if (imageData != null && imageData.length > 0) {
                    try (ByteArrayInputStream bais = new ByteArrayInputStream(imageData)) {
                        Image img = new Image(bais, 32, 32, true, true);
                        if (!img.isError()) {
                            pattern = new ImagePattern(img);
                        }
                    } catch (Exception e) {
                        System.err.println("Erreur chargement image utilisateur : " + e.getMessage());
                    }
                }

                // Si pas d'image valide, essayer l'image par défaut
                if (pattern == null) {
                    Image defaultImg = getDefaultImage();
                    if (defaultImg != null) {
                        pattern = new ImagePattern(defaultImg);
                    } else {
                        // Fallback : couleur unie
                        Pfimage.setFill(DEFAULT_COLOR);
                        return;
                    }
                }
                Pfimage.setFill(pattern);
            }
        }
    }

    private Image getDefaultImage() {
        try (InputStream is = getClass().getResourceAsStream("/images/default-avatar.png")) {
            if (is != null) {
                return new Image(is);
            }
        } catch (Exception e) {
            System.err.println("Erreur chargement image par défaut : " + e.getMessage());
        }
        return null;
    }

    public User getCurrentuser() {
        return currentuser;
    }


    @FXML
    private void profile() {
        setView("/Profile/Profile2.fxml");
    }

    public  void reloadInterface() {
        //currentuser = service.getCurrentUser();

        // Mettre à jour l'affichage des informations utilisateur
        setUserinfo();

    }


}
