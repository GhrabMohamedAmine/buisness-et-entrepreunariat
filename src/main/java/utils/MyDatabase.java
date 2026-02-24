package utils;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;



public class MyDatabase {

    private static final String URL ="jdbc:mysql://localhost:3306/nexum";
    private static final String USERNAME ="root";
    private static final String PWD ="";

    private  static Connection conx;

    public static MyDatabase instance;

    private MyDatabase() throws SQLException {
        conx = DriverManager.getConnection(URL,USERNAME,PWD);
        System.out.println("Connexion établie!");
    }

    public static synchronized MyDatabase getInstance(){
        if (instance == null){
            try {
                instance = new MyDatabase();
            } catch (SQLException e) {
                throw new RuntimeException("Unable to initialize database connection", e);
            }
        }
        return instance;

    }


    public static synchronized Connection getConnection() throws SQLException {
        if (instance == null) {
            getInstance();
        }
        if (conx == null || conx.isClosed() || !conx.isValid(2)) {
            conx = DriverManager.getConnection(URL, USERNAME, PWD);
        }
        return conx;
    }
}
