package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    private final String URL = "jdbc:mysql://localhost:3306/nexum?useSSL=false&serverTimezone=UTC";
    private final String USER = "root";
    private final String PASSWORD = ""; // change if needed (sometimes "")

    private Connection connection;
    private static DBConnection instance;

    private DBConnection() {
        try {
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Connection established");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public static DBConnection getInstance() {
        if (instance == null) instance = new DBConnection();
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }
}
