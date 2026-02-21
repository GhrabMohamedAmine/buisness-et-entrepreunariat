module Nexum {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.net.http;

    requires atlantafx.base;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.fontawesome5;
    requires okhttp3;
    requires com.fasterxml.jackson.databind;

    opens controllers to javafx.fxml;
    opens entities to javafx.base;

    exports Mains;
}