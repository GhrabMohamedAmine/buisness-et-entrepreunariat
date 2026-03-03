module org.example.yedikpromax {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.materialdesign2;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires com.almasb.fxgl.all;
    requires atlantafx.base;
    requires java.sql;
    requires java.desktop;
    requires java.net.http;
    requires com.sun.jna;
    requires uk.co.caprica.vlcj;
    requires javafx.swing;
    requires com.google.gson;
    requires jxbrowser;
    requires jxbrowser.javafx;
    requires com.auth0.jwt;
    requires com.fasterxml.jackson.databind;
    requires spring.messaging;
    requires spring.websocket;
    requires jakarta.mail;
    requires io.github.cdimascio.dotenv.java;
    requires org.json;
    requires webcam.capture;
    requires com.google.zxing;
    requires com.google.zxing.javase;
    requires org.kordamp.ikonli.feather;
    requires javafx.media;
    requires org.apache.pdfbox;


    requires org.kordamp.ikonli.fontawesome5;
    requires okhttp3;
    requires javafx.graphics;
    requires spring.boot.autoconfigure;
    requires kernel;
    requires layout;
    //requires org.example.yedikpromax;
    //requires org.example.yedikpromax;



    opens org.example.yedikpromax to javafx.fxml;
    opens services to com.google.gson;
    exports org.example.yedikpromax;
    exports com.example.testp1.entities;
    opens Mains to javafx.graphics, javafx.fxml;
    opens com.example.testp1.controllers to javafx.fxml;
    opens com.example.testp1 to javafx.fxml;
    opens entities to javafx.base;
    opens services.ai to com.fasterxml.jackson.databind;
    opens controllers.formation_quiz_result to javafx.fxml;

    exports controllers;
    exports Mains;
    opens controllers to javafx.fxml;
    opens utils to javafx.fxml;
}
