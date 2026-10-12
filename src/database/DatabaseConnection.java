package database;

import java.io.*;
import java.sql.*;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseConnection {

    private static final Logger LOGGER = Logger.getLogger(DatabaseConnection.class.getName());
    private static DatabaseConnection instance;
    private static boolean demoMode = false;
    private Connection connection;

    private String dbUrl;
    private String dbUser;
    private String dbPassword;
    private String dbDriver;

    private DatabaseConnection() {
        loadProperties();
    }

    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    public static boolean isDemoMode() {
        return demoMode;
    }

    public static void setDemoMode(boolean mode) {
        demoMode = mode;
        if (mode) {
            LOGGER.info("Ứng dụng chạy ở chế độ Demo (Mock In-Memory DB)");
        }
    }

    private void loadProperties() {
        Properties props = new Properties();
        String[] paths = {
            "resources/db.properties",
            "../resources/db.properties",
            "db.properties"
        };
        
        boolean loaded = false;
        for (String path : paths) {
            File file = new File(path);
            if (file.exists()) {
                try (FileInputStream fis = new FileInputStream(file)) {
                    props.load(fis);
                    loaded = true;
                    break;
                } catch (IOException ignored) {
                }
            }
        }

        if (!loaded) {
            try (InputStream is = getClass().getClassLoader().getResourceAsStream("db.properties")) {
                if (is != null) {
                    props.load(is);
                }
            } catch (IOException ignored) {
            }
        }

        this.dbUrl = props.getProperty("db.url",
            "jdbc:sqlserver://localhost:1433;databaseName=HotelMs;encrypt=false;trustServerCertificate=true");
        this.dbUser = props.getProperty("db.user", "sa");
        this.dbPassword = props.getProperty("db.password", "sapassword");
        this.dbDriver = props.getProperty("db.driver", "com.microsoft.sqlserver.jdbc.SQLServerDriver");
    }

    public synchronized Connection getConnection() throws SQLException {
        if (demoMode) {
            throw new SQLException("Đang ở chế độ Demo (không kết nối SQL Server).");
        }
        try {
            if (connection == null || connection.isClosed()) {
                Class.forName(dbDriver);
                DriverManager.setLoginTimeout(3);
                connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
            }
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQL Server JDBC Driver không tìm thấy: " + e.getMessage());
        }
        return connection;
    }

    public void beginTransaction() throws SQLException {
        if (demoMode) return;
        getConnection().setAutoCommit(false);
    }

    public void commitTransaction() throws SQLException {
        if (demoMode) return;
        if (connection != null && !connection.isClosed()) {
            connection.commit();
            connection.setAutoCommit(true);
        }
    }

    public void rollbackTransaction() {
        if (demoMode) return;
        try {
            if (connection != null && !connection.isClosed()) {
                connection.rollback();
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi rollback transaction", e);
        }
    }

    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException ignored) {
        }
    }

    public boolean testConnection() {
        if (demoMode) return false;
        try {
            getConnection();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }
}
