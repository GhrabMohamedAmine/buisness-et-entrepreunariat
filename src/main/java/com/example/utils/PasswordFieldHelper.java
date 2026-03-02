package com.example.utils;

import atlantafx.base.controls.PasswordTextField;
import javafx.scene.Cursor;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

public class PasswordFieldHelper {
    public static void setupPasswordToggle(PasswordTextField field) {
        var icon = new FontIcon(Feather.EYE_OFF);
        icon.setCursor(Cursor.HAND);

        icon.setOnMouseClicked(e -> {
            boolean isRevealed = field.getRevealPassword();
            field.setRevealPassword(!isRevealed);
            icon.setIconCode(isRevealed ? Feather.EYE_OFF : Feather.EYE);
        });

        field.setRight(icon);
    }
}
