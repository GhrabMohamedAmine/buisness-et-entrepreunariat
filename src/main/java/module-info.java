module NEXUM {
    // JavaFX modules
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.base;
    requires javafx.graphics;
    requires javafx.web;
    requires javafx.swing;

    // Java standard modules
    requires java.desktop;
    requires java.sql;
    requires java.net.http;

    // AtlantaFX theme
    requires atlantafx.base;

    // Ikonli icons
    requires org.kordamp.ikonli.core;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.materialdesign2;
    requires org.kordamp.ikonli.feather;

    // Additional UI libraries
    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.bootstrapfx.core;

    // Webcam capture
    requires webcam.capture;

    // Barcode / QR
    requires com.google.zxing;
    requires com.google.zxing.javase;

    // JSON processing
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.databind;
    requires com.google.gson;
    requires org.json;

    // Environment variables
    requires io.github.cdimascio.dotenv.java;

    // Email
    requires jakarta.mail;

    // Game library (FXGL)
    requires com.almasb.fxgl.all;

    // Open packages for FXML and reflection
    opens controllers to javafx.fxml;
    opens entities to javafx.base;
    opens Mains to javafx.graphics, javafx.fxml;
    opens services;   // ouvert pour la réflexion si nécessaire

    // Open packages du module com.example.testp1
    opens com.example.testp1.entities to com.fasterxml.jackson.databind;
    opens com.example.testp1 to javafx.fxml;
    opens com.example.testp1.controllers to javafx.fxml;
    opens com.example.testp1.Mains to javafx.fxml;

    // Export des packages
    exports services;
    exports entities;
    exports Mains;
    exports com.example.testp1;
    exports com.example.testp1.controllers;
    exports com.example.testp1.Mains;
}