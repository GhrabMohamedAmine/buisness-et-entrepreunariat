package tezfx.model;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class databaseconnection
{
    // MAMP default credentials
    private static final String URL = "jdbc:mysql://localhost:8889/Nexum?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASS = "root"; // MAMP default is 'root'

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }

}
