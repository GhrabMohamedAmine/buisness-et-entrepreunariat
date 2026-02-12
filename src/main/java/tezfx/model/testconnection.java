package tezfx.model;

import java.sql.Connection;
import java.sql.SQLException;

public class testconnection {
    public static void main(String[] args) {
        System.out.println("Testing connection to MAMP...");

        try (Connection connection = databaseconnection.getConnection()) {
            if (connection != null && !connection.isClosed()) {
                System.out.println("✅ SUCCESS: Connected to the database!");

                // Optional: Print database metadata to be 100% sure
                System.out.println("Database Product: " + connection.getMetaData().getDatabaseProductName());
            }
        } catch (SQLException e) {
            System.err.println("❌ FAILURE: Could not connect to the database.");
            System.err.println("Error Message: " + e.getMessage());
            System.err.println("Error Code: " + e.getErrorCode());

            // Helpful tip for MAMP users:
            if (e.getMessage().contains("Access denied")) {
                System.err.println("Tip: Check if your password is 'root' (MAMP default).");
            }
        }
    }
}