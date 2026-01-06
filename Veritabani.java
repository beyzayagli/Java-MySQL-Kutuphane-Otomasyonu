import java.sql.*;

public class Veritabani {
    private static final String URL = "jdbc:mysql://localhost:3306/kutuphanesistemi?useUnicode=true&characterEncoding=UTF-8";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    public static Connection baglan() {
        try {
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException e) {
            System.out.println("Hata: " + e.getMessage());
            return null;
        }
    }
}