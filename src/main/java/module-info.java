module com.example.testp1 {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;
    requires atlantafx.base;
    requires org.kordamp.ikonli.feather;
    requires javafx.graphics;
    requires java.desktop;
    requires java.sql;
    requires javafx.base;
    requires com.fasterxml.jackson.annotation;
    requires io.github.cdimascio.dotenv.java;
    requires com.fasterxml.jackson.databind;
    requires java.net.http;

    opens com.example.testp1.entities to com.fasterxml.jackson.databind;


    opens com.example.testp1 to javafx.fxml;
    exports com.example.testp1;
}