package controllers;

import entities.User;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;
import services.UserService;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class UserTableController implements Initializable {


    @FXML private Label topName;
    @FXML private Circle topAvatar;
    @FXML private TableView<User> userTable;

    // Modification ici : <User, User> pour accéder à l'objet complet (Image + Nom)
    @FXML private TableColumn<User, User> colUser;

    @FXML private TableColumn<User, String> colEmail, colPhone, colRole, colDept, colStatus, colJoined;
    @FXML private TableColumn<User, Void> colActions;

    // ObservableList pour mettre à jour l'interface automatiquement
    private ObservableList<User> userList = FXCollections.observableArrayList();

    // Service pour la base de données
    private final UserService userService = new UserService();

    private void loadCurrentUserProfile() {
        User currentUser = UserService.getCurrentUser();

        if (currentUser != null) {
            // 1. Update the Name (Alex -> Real Name)
            String fullName = currentUser.getFirstName() + " " + currentUser.getName();
            topName.setText(fullName);

            // 2. Update the Avatar Circle
            String imagePath = currentUser.getImageLink();
            if (imagePath != null && !imagePath.isEmpty()) {
                try {
                    Image img = new Image(imagePath, 32, 32, true, true);
                    if (!img.isError()) {
                        topAvatar.setFill(new ImagePattern(img));
                    }
                } catch (Exception e) {
                    System.out.println("Error loading profile image: " + e.getMessage());
                }
            }
        }
    }
    // --- LISTE DES RÔLES ---
    private final ObservableList<String> roleOptions = FXCollections.observableArrayList(
            "Admin",
            "Manager",
            "Ressource Manager",
            "Employer",
            "Formateur",
            "Chef Projet",
            "Expert Financier"
    );

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupColumns();
        loadUsersFromDatabase();
        loadCurrentUserProfile();
    }

    private void setupColumns() {
        // --- Colonnes Texte Standard ---
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colDept.setCellValueFactory(new PropertyValueFactory<>("department"));
        colJoined.setCellValueFactory(new PropertyValueFactory<>("joinedDate"));

        // =========================================================================
        // --- 1. COLONNE USER (AVEC IMAGE) - C'est ici que j'ai ajouté l'image ---
        // =========================================================================
        colUser.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue()));
        colUser.setCellFactory(column -> new TableCell<User, User>() {
            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);

                if (empty || user == null) {
                    setGraphic(null);
                } else {
                    HBox box = new HBox(10);
                    box.setAlignment(Pos.CENTER_LEFT);

                    // Création du cercle pour l'image
                    Circle avatar = new Circle(18);
                    avatar.setStroke(Color.web("#E5E7EB"));
                    avatar.setStrokeWidth(1);

                    // --- LOGIQUE DE CHARGEMENT DE L'IMAGE ---
                    String imagePath = user.getImageLink();
                    boolean imageLoaded = false;

                    if (imagePath != null && !imagePath.isEmpty()) {
                        try {
                            // On charge l'image en arrière-plan
                            Image img = new Image(imagePath, 40, 40, true, true);
                            if (!img.isError()) {
                                avatar.setFill(new ImagePattern(img));
                                imageLoaded = true;
                            }
                        } catch (Exception e) {
                            // En cas d'erreur de chemin, on garde le défaut
                            System.err.println("Erreur image pour " + user.getName() + ": " + e.getMessage());
                        }
                    }

                    // Si pas d'image ou erreur, on met une couleur par défaut
                    if (!imageLoaded) {
                        avatar.setFill(Color.web("#E0E7FF"));
                    }

                    // Concaténation Nom + Prénom
                    String fullName = (user.getFirstName() != null ? user.getFirstName() : "")
                            + " " + (user.getName() != null ? user.getName() : "");

                    Label nameLabel = new Label(fullName.trim());
                    nameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #111827; -fx-font-size: 13px;");

                    box.getChildren().addAll(avatar, nameLabel);
                    setGraphic(box);
                }
            }
        });

        // =========================================================================
        // --- 2. COLONNE RÔLE (Votre code intact) ---
        // =========================================================================
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colRole.setCellFactory(column -> new TableCell<User, String>() {
            private final ComboBox<String> roleCombo = new ComboBox<>(roleOptions);

            {
                roleCombo.getStyleClass().add("role-combo");
                roleCombo.setOnAction(event -> {
                    // Attention : ici on utilise getTableView().getItems().get(getIndex()) car updateItem ne donne que le String
                    if (getTableRow() != null && getTableRow().getItem() != null) {
                        User user = (User) getTableRow().getItem();
                        String newRole = roleCombo.getValue();
                        if (newRole != null && !newRole.equals(user.getRole())) {
                            handleRoleUpdate(user, newRole);
                        }
                    }
                });
            }

            @Override
            protected void updateItem(String role, boolean empty) {
                super.updateItem(role, empty);
                if (empty || role == null) {
                    setGraphic(null);
                } else {
                    roleCombo.setValue(role);
                    setGraphic(roleCombo);
                }
            }
        });

        // =========================================================================
        // --- 3. COLONNE STATUT (Votre code intact avec les Pills) ---
        // =========================================================================
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStatus.setCellFactory(column -> new TableCell<User, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setGraphic(null);
                } else {
                    Label badge = new Label(status);
                    badge.getStyleClass().add("status-pill");

                    badge.getStyleClass().removeAll("pill-active", "pill-pending", "pill-suspended");

                    switch (status.toLowerCase()) {
                        case "active":
                        case "actif":
                            badge.getStyleClass().add("pill-active");
                            break;
                        case "pending":
                        case "en attente":
                            badge.getStyleClass().add("pill-pending");
                            break;
                        default:
                            badge.getStyleClass().add("pill-suspended");
                            break;
                    }
                    setGraphic(badge);
                }
            }
        });

        // =========================================================================
        // --- 4. COLONNE ACTIONS (Votre code intact avec les Icônes) ---
        // =========================================================================
        colActions.setCellFactory(param -> new TableCell<User, Void>() {
            private final Button deleteBtn = new Button();
            private final Button statusBtn = new Button();
            private final FontIcon statusIcon = new FontIcon();

            {
                // Bouton Supprimer
                FontIcon deleteIcon = new FontIcon("mdi2t-trash-can-outline");
                deleteIcon.setIconSize(18);
                deleteBtn.setGraphic(deleteIcon);
                deleteBtn.getStyleClass().addAll("action-btn", "action-btn-delete");
                deleteBtn.setTooltip(new Tooltip("Supprimer cet utilisateur"));

                deleteBtn.setOnAction(e -> {
                    User user = getTableView().getItems().get(getIndex());
                    handleDeleteUser(user);
                });

                // Bouton Statut
                statusIcon.setIconSize(18);
                statusBtn.setGraphic(statusIcon);
                statusBtn.getStyleClass().addAll("action-btn", "action-btn-state");

                statusBtn.setOnAction(e -> {
                    User user = getTableView().getItems().get(getIndex());
                    handleStatusSwitch(user);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    User user = getTableView().getItems().get(getIndex());

                    String status = user.getStatus().toLowerCase();
                    if (status.equals("active") || status.equals("actif")) {
                        statusIcon.setIconLiteral("mdi2b-block-helper");
                        statusIcon.setIconColor(Color.web("#D97706")); // Orange
                        statusBtn.setTooltip(new Tooltip("Suspendre le compte"));
                    } else {
                        statusIcon.setIconLiteral("mdi2c-check-circle-outline");
                        statusIcon.setIconColor(Color.web("#15803D")); // Vert
                        statusBtn.setTooltip(new Tooltip("Activer le compte"));
                    }

                    HBox buttons = new HBox(8, statusBtn, deleteBtn);
                    buttons.setAlignment(Pos.CENTER);
                    setGraphic(buttons);
                }
            }
        });
    }

    // --- LOGIQUE METIER (Intacte) ---

    private void handleRoleUpdate(User user, String newRole) {
        try {
            userService.modifierRole(user.getId(), newRole);
            user.setRole(newRole);
            System.out.println("Role mis à jour : " + newRole);
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de modifier le rôle : " + e.getMessage());
            loadUsersFromDatabase();
        }
    }

    private void handleStatusSwitch(User user) {
        String currentStatus = user.getStatus().toLowerCase();
        String newStatus;

        if (currentStatus.equals("active") || currentStatus.equals("actif")) {
            newStatus = "Suspendu";
        } else {
            newStatus = "Active";
        }

        try {
            userService.modifierStatut(user.getId(), newStatus);
            loadUsersFromDatabase();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de modifier le statut : " + e.getMessage());
        }
    }

    private void handleDeleteUser(User user) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation de suppression");
        alert.setHeaderText(null);
        alert.setContentText("Êtes-vous sûr de vouloir supprimer " + user.getFirstName() + " " + user.getName() + " ?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                userService.supprimer(user.getId());
                userList.remove(user);
            } catch (SQLException e) {
                e.printStackTrace();
                showAlert("Erreur", "Impossible de supprimer l'utilisateur : " + e.getMessage());
            }
        }
    }

    private void loadUsersFromDatabase() {
        try {
            List<User> users = userService.recupererTous();
            userList.clear();
            userList.addAll(users);
            userTable.setItems(userList);
        } catch (SQLException e) {
            System.err.println("Erreur chargement utilisateurs: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.show();
    }

    // --- NAVIGATION (Intacte) ---

    @FXML
    public void handleReclamationsNavigation(ActionEvent event) {
        switchScene(event, "/Reclamation/Reclamation.fxml");
    }

    public void handleLogout(ActionEvent event) {
        // Décommentez si vous avez la méthode static UserService.logout()
        UserService.logout();
        switchScene(event, "/Start/1ere.fxml");
    }

    private void switchScene(ActionEvent event, String fxmlPath) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            System.err.println("Erreur chargement FXML: " + fxmlPath);
            e.printStackTrace();
        }
    }
}