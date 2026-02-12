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

    exports tezfx.app;
    opens tezfx.controller to javafx.fxml;
    opens tezfx.app to javafx.graphics, javafx.fxml;

}
