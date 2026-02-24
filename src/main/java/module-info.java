module tezfx {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires atlantafx.base;
    requires org.kordamp.ikonli.materialdesign2;
    requires java.sql;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires com.almasb.fxgl.all;
    requires java.desktop;
    requires java.net.http;
    requires com.google.gson;

    exports Mains;
    opens controllers to javafx.fxml;
    opens Mains to javafx.graphics, javafx.fxml;

}
