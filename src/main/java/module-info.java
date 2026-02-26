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
    requires jcefmaven;
    requires jcef;
    requires com.auth0.jwt;
    requires com.fasterxml.jackson.databind;
    requires spring.messaging;
    requires spring.websocket;

    opens org.example.yedikpromax to javafx.fxml;
    opens services to com.google.gson;
    exports org.example.yedikpromax;

    exports controller;
    opens controller to javafx.fxml;
}
