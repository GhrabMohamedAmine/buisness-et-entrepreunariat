package com.example.utils;

import io.github.cdimascio.dotenv.Dotenv;

public class Config {
    // Load the .env file once when the class is initialized
    private static final Dotenv dotenv = Dotenv.load();

    /**
     * Retrieves a value from the .env file.
     * @param key The name of the variable (e.g., "CURRENCY_API_KEY")
     * @return The value of the key, or null if not found.
     */
    public static String get(String key) {
        return dotenv.get(key);
    }
}