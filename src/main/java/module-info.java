module com.example.testp1 {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.base;
    requires javafx.web;
    requires javafx.swing;

    requires java.desktop;
    requires java.sql;
    requires java.net.http;

    requires atlantafx.base;
    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.bootstrapfx.core;

    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.feather;
    requires org.kordamp.ikonli.materialdesign2;

    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.databind;
    requires com.google.gson;
    requires org.json;
    requires io.github.cdimascio.dotenv.java;

    requires com.almasb.fxgl.all;

    requires webcam.capture;
    requires com.google.zxing;
    requires com.google.zxing.javase;

    opens com.example.testp1.entities to com.fasterxml.jackson.databind;

    opens com.example.testp1 to javafx.fxml;
    opens com.example.testp1.controllers to javafx.fxml;
    opens com.example.testp1.Mains to javafx.fxml;

    exports com.example.testp1;
    exports com.example.testp1.controllers;
    exports com.example.testp1.Mains;

    exports Mains;
    opens controllers to javafx.fxml;
    opens Mains to javafx.graphics, javafx.fxml;
}
