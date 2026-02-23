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
    requires org.json;
    requires webcam.capture;
    requires com.google.zxing;
    requires com.google.zxing.javase;
    requires javafx.swing;

    opens com.example.testp1.entities to com.fasterxml.jackson.databind;


    opens com.example.testp1 to javafx.fxml;
    exports com.example.testp1;
    exports com.example.testp1.controllers;
    opens com.example.testp1.controllers to javafx.fxml;
    exports com.example.testp1.Mains;
    opens com.example.testp1.Mains to javafx.fxml;
}