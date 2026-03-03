package controllers;

import entities.User;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import Mains.ViewLoader;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.example.yedikpromax.HelloApplication;
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


    public void applyFullScreen(boolean fullScreen) {
        Stage stage = getStage();
        if (stage == null) return;

        // Optional: enable exiting fullscreen with ESC automatically
        stage.setFullScreenExitHint(""); // hide hint
        // stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH); // disable ESC if you want

        stage.setFullScreen(fullScreen);

        // If you prefer "fullscreen-like" without OS fullscreen mode:
        // stage.setMaximized(fullScreen);
    }

    /** Borderless "fullscreen" (covers screen without Stage fullscreen mode). */
    public void applyBorderlessFullScreen() {
        Stage stage = getStage();
        if (stage == null) return;

        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        stage.setX(bounds.getMinX());
        stage.setY(bounds.getMinY());
        stage.setWidth(bounds.getWidth());
        stage.setHeight(bounds.getHeight());
        stage.setMaximized(true);
    }

    /** Ensure root stretches to fill scene; useful when resizing/fullscreening. */
    public void bindRootToScene() {
        Scene scene = mainView.getScene();
        if (scene == null) return;

        mainView.prefWidthProperty().bind(scene.widthProperty());
        mainView.prefHeightProperty().bind(scene.heightProperty());
    }

    private void toggleFullScreen() {
        if (mainView == null || mainView.getScene() == null) return;

        Stage stage = (Stage) mainView.getScene().getWindow();
        if (stage == null) return;

        stage.setFullScreenExitHint(""); // optional
        stage.setFullScreen(!stage.isFullScreen());
    }

    private Stage getStage() {
        if (mainView == null || mainView.getScene() == null) return null;
        if (!(mainView.getScene().getWindow() instanceof Stage s)) return null;
        return s;
    }

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
//


        // ✅ Your CSS after theme

        staticContentArea = contentArea;
        //currentuser = service.getCurrentUser();
        System.out.println("Current user: " + currentuser.toString());
        setUserinfo();

        Platform.runLater(() -> {
            Scene scene = mainView.getScene();
            if (scene == null) return;

            scene.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.F11) {
                    toggleFullScreen();
                    event.consume();
                }
            });
        });

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

    public void messages() {
        setView("/org/example/yedikpromax/chat-view.fxml");
    }
    public void resources() {
        setView("/front/RSstack.fxml");
    }

    public void formations() {
        setView("/fxml/layout/client_layout.fxml");
    }

    public void manFormations(){
        setView("/tezfx/view/MRformation.fxml");
    }

    public void resultFormations(){
        setView("/fxml/client_results.fxml");
    }


}
