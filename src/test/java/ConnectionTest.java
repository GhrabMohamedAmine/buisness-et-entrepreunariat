
import com.example.testp1.model.DB;
import java.sql.Connection;
import java.sql.SQLException;


public class ConnectionTest {
    public static void main(String[] args) {
        System.out.println("--- Nexum Database Connection Test ---");

        try {

            DB dbInstance = DB.getInstance();
            Connection connection = dbInstance.getConx();


            if (connection != null && !connection.isClosed()) {
                System.out.println("SUCCESS: Connection is active and ready for Nexum data!");

                System.out.println("Connected to: " + connection.getMetaData().getDatabaseProductVersion());
            } else {
                System.out.println("FAILURE: Connection object is null or closed.");
            }

        } catch (SQLException e) {
            System.err.println("SQL Error during test: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("General Error: " + e.getMessage());
        }
    }
}



