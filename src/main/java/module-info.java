module NEXUM {
    // 1. Modules JavaFX requis
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.base; // Nécessaire pour les TableView et les PropertyValueFactory

    // 2. Le thème AtlantaFX
    requires atlantafx.base;

    // 3. Les icônes Ikonli
    requires org.kordamp.ikonli.core;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.materialdesign2;
    requires org.kordamp.ikonli.feather;
    requires java.desktop;
    requires javafx.graphics;
    requires java.sql;

    // 4. Accès pour le chargement des interfaces (FXML)
    // Permet à JavaFX de lier le fichier UserTable.fxml à sa classe contrôleur
    opens controllers to javafx.fxml;

    // 5. Accès pour l'affichage des données (TableView)
    // IMPORTANT : Permet à la TableView de lire les champs de votre classe User
    opens entities to javafx.base;

    // 6. Exportation du package principal pour lancer l'application
    exports Mains;
}