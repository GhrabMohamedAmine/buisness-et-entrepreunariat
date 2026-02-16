module Nexum {

    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires atlantafx.base;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.fontawesome5;


    opens controllers to javafx.fxml;
    opens entities to javafx.base;

    exports Mains;
}
