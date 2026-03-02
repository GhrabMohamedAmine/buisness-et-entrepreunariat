package controllers;

import entities.User;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
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
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;
import services.EmailService;
import services.UserService;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Predicate;

public class UserTableController implements Initializable {

    @FXML private Label topName;
    @FXML private Circle topAvatar;
    @FXML private TableView<User> userTable;
    @FXML private TextField searchField;

    // Statistiques
    @FXML private Label totalUsersCount;
    @FXML private Label activeUsersCount;
    @FXML private Label pendingUsersCount;

    // Toggle buttons pour le filtre par statut
    @FXML private ToggleButton allToggle;
    @FXML private ToggleButton activeToggle;
    @FXML private ToggleButton pendingToggle;

    @FXML private TableColumn<User, User> colUser;
    @FXML private TableColumn<User, String> colEmail, colPhone, colRole, colDept, colStatus, colJoined;
    @FXML private TableColumn<User, Void> colActions;

    private ObservableList<User> userList = FXCollections.observableArrayList();
    private FilteredList<User> filteredData;
    private final UserService userService = new UserService();

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

        // Initialisation du groupe de toggles AVANT de charger les données
        ToggleGroup statusGroup = new ToggleGroup();
        allToggle.setToggleGroup(statusGroup);
        activeToggle.setToggleGroup(statusGroup);
        pendingToggle.setToggleGroup(statusGroup);

        // Initialisation de la liste filtrée
        filteredData = new FilteredList<>(userList, p -> true);
        userTable.setItems(filteredData);

        // Écouteur de changement de filtre statut
        statusGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                // Si aucun sélectionné, on force "All"
                allToggle.setSelected(true);
            } else {
                applyFilters();
            }
        });

        // Écouteur de recherche
        searchField.textProperty().addListener((observable, oldValue, newValue) -> applyFilters());

        // Charger les données (cela va appliquer les filtres avec le groupe déjà initialisé)
        loadUsersFromDatabase();
        loadCurrentUserProfile();

        // Initialisation : "All" sélectionné
        allToggle.setSelected(true);
    }

    private void loadCurrentUserProfile() {
        User currentUser = UserService.getCurrentUser();
        if (currentUser != null) {
            String fullName = currentUser.getFirstName() + " " + currentUser.getName();
            topName.setText(fullName);

            byte[] imageData = currentUser.getImageData();
            if (imageData != null && imageData.length > 0) {
                try (ByteArrayInputStream bais = new ByteArrayInputStream(imageData)) {
                    Image img = new Image(bais, 32, 32, true, true);
                    if (!img.isError()) {
                        topAvatar.setFill(new ImagePattern(img));
                    } else {
                        topAvatar.setFill(Color.web("#E0E7FF"));
                    }
                } catch (Exception e) {
                    System.out.println("Erreur chargement image profil : " + e.getMessage());
                    topAvatar.setFill(Color.web("#E0E7FF"));
                }
            } else {
                topAvatar.setFill(Color.web("#E0E7FF"));
            }
        }
    }

    private void setupColumns() {
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colDept.setCellValueFactory(new PropertyValueFactory<>("department"));
        colJoined.setCellValueFactory(new PropertyValueFactory<>("joinedDate"));

        // Colonne USER avec avatar et nom
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

                    Circle avatar = new Circle(18);
                    avatar.setStroke(Color.web("#E5E7EB"));
                    avatar.setStrokeWidth(1);

                    byte[] imageData = user.getImageData();
                    boolean imageLoaded = false;

                    if (imageData != null && imageData.length > 0) {
                        try (ByteArrayInputStream bais = new ByteArrayInputStream(imageData)) {
                            Image img = new Image(bais, 40, 40, true, true);
                            if (!img.isError()) {
                                avatar.setFill(new ImagePattern(img));
                                imageLoaded = true;
                            }
                        } catch (Exception e) {
                            System.err.println("Erreur image pour " + user.getName() + " : " + e.getMessage());
                        }
                    }

                    if (!imageLoaded) {
                        avatar.setFill(Color.web("#E0E7FF"));
                    }

                    String fullName = (user.getFirstName() != null ? user.getFirstName() : "")
                            + " " + (user.getName() != null ? user.getName() : "");
                    Label nameLabel = new Label(fullName.trim());
                    nameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #111827; -fx-font-size: 13px;");

                    box.getChildren().addAll(avatar, nameLabel);
                    setGraphic(box);
                }
            }
        });

        // Colonne ROLE avec ComboBox
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colRole.setCellFactory(column -> new TableCell<User, String>() {
            private final ComboBox<String> roleCombo = new ComboBox<>(roleOptions);

            {
                roleCombo.getStyleClass().add("role-combo");
                roleCombo.setOnAction(event -> {
                    if (getTableRow() != null && getTableRow().getItem() != null) {
                        User user = getTableRow().getItem();
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

        // Colonne STATUS avec badge
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

        // Colonne ACTIONS (boutons)
        colActions.setCellFactory(param -> new TableCell<User, Void>() {
            private final Button deleteBtn = new Button();
            private final Button statusBtn = new Button();
            private final FontIcon statusIcon = new FontIcon();

            {
                FontIcon deleteIcon = new FontIcon("mdi2t-trash-can-outline");
                deleteIcon.setIconSize(18);
                deleteBtn.setGraphic(deleteIcon);
                deleteBtn.getStyleClass().addAll("action-btn", "action-btn-delete");
                deleteBtn.setTooltip(new Tooltip("Supprimer cet utilisateur"));

                deleteBtn.setOnAction(e -> {
                    User user = getTableView().getItems().get(getIndex());
                    handleDeleteUser(user);
                });

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
                        statusIcon.setIconColor(Color.web("#D97706"));
                        statusBtn.setTooltip(new Tooltip("Suspendre le compte"));
                    } else {
                        statusIcon.setIconLiteral("mdi2c-check-circle-outline");
                        statusIcon.setIconColor(Color.web("#15803D"));
                        statusBtn.setTooltip(new Tooltip("Activer le compte"));
                    }

                    HBox buttons = new HBox(8, statusBtn, deleteBtn);
                    buttons.setAlignment(Pos.CENTER);
                    setGraphic(buttons);
                }
            }
        });
    }

    private void handleRoleUpdate(User user, String newRole) {
        try {
            userService.modifierRole(user.getId(), newRole);
            user.setRole(newRole);
            System.out.println("Rôle mis à jour : " + newRole);
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de modifier le rôle : " + e.getMessage());
            loadUsersFromDatabase();
        }
    }

    private void handleStatusSwitch(User user) {
        String currentStatus = user.getStatus().toLowerCase();
        String newStatus = (currentStatus.equals("active") || currentStatus.equals("actif")) ? "Suspendu" : "Active";

        try {
            userService.modifierStatut(user.getId(), newStatus);
            // Recharger les données pour mettre à jour les compteurs et le filtre
            loadUsersFromDatabase();
            // Envoyer un email dans un thread séparé
            new Thread(() -> EmailService.sendStatusChangeEmail(user.getEmail(), newStatus)).start();
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
                updateCounts(); // Mettre à jour les compteurs après suppression
                applyFilters(); // Réappliquer les filtres
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
            updateCounts();
            applyFilters(); // Réappliquer les filtres après chargement
        } catch (SQLException e) {
            System.err.println("Erreur chargement utilisateurs : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateCounts() {
        long total = userList.size();
        long active = userList.stream()
                .filter(u -> {
                    String s = u.getStatus().toLowerCase();
                    return s.equals("active") || s.equals("actif");
                })
                .count();
        long pending = userList.stream()
                .filter(u -> {
                    String s = u.getStatus().toLowerCase();
                    return s.equals("pending") || s.equals("en attente");
                })
                .count();

        totalUsersCount.setText(String.valueOf(total));
        activeUsersCount.setText(String.valueOf(active));
        pendingUsersCount.setText(String.valueOf(pending));
    }

    private void applyFilters() {
        String searchText = searchField.getText() == null ? "" : searchField.getText().toLowerCase();
        Toggle selectedToggle = allToggle.getToggleGroup().getSelectedToggle();

        Predicate<User> statusPredicate = user -> {
            if (selectedToggle == null || selectedToggle == allToggle) {
                return true; // tous
            } else if (selectedToggle == activeToggle) {
                String s = user.getStatus().toLowerCase();
                return s.equals("active") || s.equals("actif");
            } else if (selectedToggle == pendingToggle) {
                String s = user.getStatus().toLowerCase();
                return s.equals("pending") || s.equals("en attente");
            }
            return true;
        };

        Predicate<User> searchPredicate = user -> {
            if (searchText.isEmpty()) return true;
            // Recherche sur nom complet
            String fullName = "";
            if (user.getFirstName() != null && user.getName() != null) {
                fullName = (user.getFirstName() + " " + user.getName()).toLowerCase();
                if (fullName.contains(searchText)) return true;
            }
            // Recherche individuelle
            return (user.getFirstName() != null && user.getFirstName().toLowerCase().contains(searchText))
                    || (user.getName() != null && user.getName().toLowerCase().contains(searchText))
                    || (user.getEmail() != null && user.getEmail().toLowerCase().contains(searchText))
                    || (user.getDepartment() != null && user.getDepartment().toLowerCase().contains(searchText))
                    || (user.getRole() != null && user.getRole().toLowerCase().contains(searchText))
                    || (user.getPhone() != null && user.getPhone().toLowerCase().contains(searchText));
        };

        filteredData.setPredicate(statusPredicate.and(searchPredicate));
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.show();
    }

    @FXML
    public void handleReclamationsNavigation(ActionEvent event) {
        switchScene(event, "/Reclamation/Reclamation.fxml");
    }

    public void handleLogout(ActionEvent event) {
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
            System.err.println("Erreur chargement FXML : " + fxmlPath);
            e.printStackTrace();
        }
    }

    @FXML
    public void handleInviteUser(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/User/InviteUserPopup.fxml"));
            Parent root = loader.load();

            InviteUserController controller = loader.getController();
            controller.setOnSaveCallback(() -> {
                loadUsersFromDatabase(); // recharger après ajout
            });

            Stage popupStage = new Stage();
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.setTitle("Inviter un utilisateur");
            popupStage.setScene(new Scene(root));
            popupStage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir le popup d'invitation.");
        }
    }
}