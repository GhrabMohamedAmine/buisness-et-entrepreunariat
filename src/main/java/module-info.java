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

    // 4. Nouveaux modules pour la webcam et le client HTTP
    requires java.net.http;                 // Pour les appels API HTTP
    requires webcam.capture;
    requires com.fasterxml.jackson.databind;
    requires javafx.swing;
    requires jakarta.mail;// Pour la capture webcam (bibliothèque sarxos)

    // 5. Accès pour le chargement des interfaces (FXML)
    opens controllers to javafx.fxml;

    // 6. Accès pour l'affichage des données (TableView)
    opens entities to javafx.base;

    // 7. Exportation des packages
    exports services;
    opens services;
    exports entities;
    exports Mains;
}