package com.example.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CurrencyDetails {
    private static final Map<String, String[]> details = new HashMap<>();

    static {
        // Format: {Full Name, Country}
        // GLOBAL TAB (Major Markets)
        details.put("USD", new String[]{"US Dollar", "United States"});
        details.put("EUR", new String[]{"Euro", "European Union"});
        details.put("GBP", new String[]{"British Pound", "United Kingdom"});
        details.put("JPY", new String[]{"Japanese Yen", "Japan"});
        details.put("CAD", new String[]{"Canadian Dollar", "Canada"});
        details.put("CHF", new String[]{"Swiss Franc", "Switzerland"});
        details.put("AUD", new String[]{"Australian Dollar", "Australia"});
        details.put("CNY", new String[]{"Chinese Yuan", "China"});
        details.put("INR", new String[]{"Indian Rupee", "India"});
        details.put("BRL", new String[]{"Brazilian Real", "Brazil"});
        // ... Add more to reach 20 for Global

        // REGIONAL TAB (Africa & Middle East)
        details.put("TND", new String[]{"Tunisian Dinar", "Tunisia"});
        details.put("SAR", new String[]{"Saudi Riyal", "Saudi Arabia"});
        details.put("AED", new String[]{"UAE Dirham", "United Arab Emirates"});
        details.put("MAD", new String[]{"Moroccan Dirham", "Morocco"});
        details.put("EGP", new String[]{"Egyptian Pound", "Egypt"});
        details.put("LYD", new String[]{"Libyan Dinar", "Libya"});
        details.put("DZD", new String[]{"Algerian Dinar", "Algeria"});
        details.put("QAR", new String[]{"Qatari Riyal", "Qatar"});
        details.put("KWD", new String[]{"Kuwaiti Dinar", "Kuwait"});
        details.put("TRY", new String[]{"Turkish Lira", "Turkey"});
        // ... Add more to reach 20 for Regional
    }

    public static String getName(String code) { return details.getOrDefault(code, new String[]{code, ""})[0]; }
    public static String getCountry(String code) { return details.getOrDefault(code, new String[]{"", ""})[1]; }

    public static Set<String> getMajorCodes() {
        return Set.of("USD", "EUR", "GBP", "JPY", "CAD", "CHF", "AUD", "CNY", "INR", "BRL", "NZD", "SGD", "HKD", "KRW", "MXN", "NOK", "SEK", "DKK", "PLN", "ZAR");
    }

    public static Set<String> getRegionalCodes() {
        return Set.of("TND", "SAR", "AED", "MAD", "EGP", "LYD", "DZD", "QAR", "KWD", "BHD", "OMR", "JOD", "TRY", "ILS", "GHS", "KES", "NGN", "MUR", "MRO", "XOF");
    }
}