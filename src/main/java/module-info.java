module esprit.tn.pi_kavafx {

    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires javafx.media;   // 🔥 OBLIGATOIRE

    // pour FXML
    opens esprit.tn.pi_kavafx to javafx.fxml;
    opens esprit.tn.pi_kavafx.controllers to javafx.fxml;

    // ⭐⭐⭐ LA LIGNE QUI FIXE TON ERREUR
    opens esprit.tn.pi_kavafx.entities to javafx.base;

    exports esprit.tn.pi_kavafx;
}
