package com.example.testp1;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class CurrencyCardController {

    @FXML private Label currencyCode;
    @FXML private Label currencyName;
    @FXML private Label countryName;
    @FXML private Label exchangeRate;

    public void setData(String code, String name, String country, double rate) {
        currencyCode.setText(code);
        currencyName.setText(name);
        countryName.setText(country);
        exchangeRate.setText(String.format("%.3f", rate));
    }
}