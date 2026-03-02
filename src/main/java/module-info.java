module NEXUM {
    // 1. Modules JavaFX requis
    requires javafx.controls;
    requires javafx.fxml;
    // Nécessaire pour les TableView et les PropertyValueFactory

    // 2. Le thème AtlantaFX
    requires atlantafx.base;

    // 3. Les icônes Ikonli
    requires org.kordamp.ikonli.core;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.materialdesign2;
    requires org.kordamp.ikonli.feather;
    requires java.sql;
    requires javafx.media;
    requires kernel;
    requires layout;
    requires jakarta.mail;
    requires java.desktop;
    requires org.json;

    exports services;
    opens services to org.junit.platform.commons, javafx.fxml;
    exports entities;
    opens entities to org.junit.platform.commons, javafx.fxml;

    opens controllers to javafx.fxml;
    opens controllers.formation_quiz_result to javafx.fxml;
     opens utils to javafx.fxml;




    exports Mains;
}