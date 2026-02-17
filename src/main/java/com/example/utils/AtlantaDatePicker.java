package com.example.utils; // Adjust to your utils package

import javafx.scene.control.DatePicker;
import javafx.util.StringConverter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class AtlantaDatePicker {

    public static void apply(DatePicker datePicker) {
        final LocalDate today = LocalDate.now();
        final DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE;

        datePicker.setValue(today);
        datePicker.setPromptText("yyyy-MM-dd");
        datePicker.setEditable(true);

        System.out.println("Applying AtlantaFX Styles");

        datePicker.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalDate localDate) {
                return (localDate == null) ? "" : formatter.format(localDate);
            }

            @Override
            public LocalDate fromString(String dateString) {
                if (dateString == null || dateString.trim().isEmpty()) {
                    return today;
                }
                try {
                    return LocalDate.parse(dateString, formatter);
                } catch (DateTimeParseException e) {
                    return today;
                }
            }
        });
    }
}
