package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/library_management"
            + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String USER = "root";

    // run.bat / run.sh ask for the password and pass it as DB_PASSWORD.
    // If you run Java manually, you can also change the fallback below.
    private static final String DEFAULT_PASSWORD = "your_mysql_password";

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL driver jar not found. Start the project using run.bat (Windows) or run.sh (Linux/Mac).");
        }
        String env = System.getenv("DB_PASSWORD");
        String password = (env != null) ? env : DEFAULT_PASSWORD;
        return DriverManager.getConnection(URL, USER, password);
    }
}
