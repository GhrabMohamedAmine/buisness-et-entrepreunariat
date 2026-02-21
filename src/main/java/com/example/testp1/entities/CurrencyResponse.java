package com.example.testp1.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;
@JsonIgnoreProperties(ignoreUnknown = true)
public class CurrencyResponse {
    public String result;
    public String base_code;

    public void setResult(String result) {
        this.result = result;
    }

    public void setBase_code(String base_code) {
        this.base_code = base_code;
    }

    public void setRates(Map<String, Double> rates) {
        this.rates = rates;
    }

    public String getResult() {
        return result;
    }

    public String getBase_code() {
        return base_code;
    }

    public Map<String, Double> getRates() {
        return rates;
    }

    @JsonProperty("conversion_rates")
    public Map<String, Double> rates;
}
