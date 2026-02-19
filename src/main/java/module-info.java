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
    requires java.desktop;
    requires java.sql;
    requires javafx.media;
    requires kernel;
    requires layout;


    opens controllers to javafx.fxml;
    opens controllers.formation_quiz_result to javafx.fxml;

    opens utils to javafx.fxml;

    opens entities to javafx.base;

    exports Mains;
}