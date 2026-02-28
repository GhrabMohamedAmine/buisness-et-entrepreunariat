package utils;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;



public class MyDatabase {

    private static final String URL = System.getenv().getOrDefault("DB_URL", "jdbc:mysql://localhost:8889/Nexum");
    private static final String USERNAME = System.getenv().getOrDefault("DB_USERNAME", "root");
    private static final String PWD = System.getenv().getOrDefault("DB_PASSWORD", "root");

    private  static Connection conx;

    public static MyDatabase instance;

    private MyDatabase() {}

    public static synchronized MyDatabase getInstance(){
        if (instance == null){
            instance = new MyDatabase();
        }
        return instance;

    }


    public static synchronized Connection getConnection() throws SQLException {
        if (instance == null) {
            getInstance();
        }
        if (conx == null || conx.isClosed() || !conx.isValid(2)) {
            conx = DriverManager.getConnection(URL, USERNAME, PWD);
            System.out.println("Connexion etablie!");
        }
        return conx;
    }
}
