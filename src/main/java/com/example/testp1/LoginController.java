package com.example.testp1;

import atlantafx.base.controls.CustomTextField;
import atlantafx.base.controls.PasswordTextField;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

public class LoginController {
    @FXML
    private PasswordTextField passwordInput;
    @FXML
    private CustomTextField emailField;

    @FXML
    public void initialize() {

        FontIcon mailIcon = new FontIcon(Feather.MAIL);
        emailField.setLeft(mailIcon);

        var icon = new FontIcon(Feather.EYE_OFF);
        icon.setCursor(Cursor.HAND);

        FontIcon lockIcon = new FontIcon(Feather.LOCK);
        passwordInput.setLeft(lockIcon);


        icon.setOnMouseClicked(e -> {
            // Check current state and flip it
            boolean isRevealed = passwordInput.getRevealPassword();
            passwordInput.setRevealPassword(!isRevealed);

            // Update the icon look
            icon.setIconCode(isRevealed ? Feather.EYE_OFF : Feather.EYE);
        });

        passwordInput.setRight(icon);


    }
}
