package database;

import java.sql.*;

/**
 * Quản lý kết nối SQL Server 2012
 * Sử dụng Singleton pattern
 */
public class DatabaseConnection {

    private static final String DB_URL  = "jdbc:sqlserver://localhost:1433;databaseName=HotelMs;encrypt=false;trustServerCertificate=true";
    private static final String DB_USER = "sa";
    private static final String DB_PASS = "sapassword";
    private static final String DB_DRIVER = "com.microsoft.sqlserver.jdbc.SQLServerDriver";

    private static DatabaseConnection instance;
    private Connection connection;

    private DatabaseConnection() {}

    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        try {
            if (connection == null || connection.isClosed()) {
                Class.forName(DB_DRIVER);
                DriverManager.setLoginTimeout(5);
                connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
            }
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQL Server JDBC Driver không tìm thấy: " + e.getMessage());
        }
        return connection;
    }

    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            System.err.println("Lỗi đóng kết nối: " + e.getMessage());
        }
    }

    public boolean testConnection() {
        try {
            getConnection();
            System.out.println("✓ Kết nối SQL Server thành công!");
            System.out.println("  URL: " + DB_URL);
            System.out.println("  User: " + DB_USER);
            return true;
        } catch (SQLException e) {
            System.err.println("✗ Kết nối thất bại: " + e.getMessage());
            return false;
        }
    }
}