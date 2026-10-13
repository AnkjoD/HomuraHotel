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
    private Connection guardedConnection;
    private final java.util.concurrent.locks.ReentrantLock operationLock = new java.util.concurrent.locks.ReentrantLock(true);

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
            try (InputStream is = getClass().getClassLoader().getResourceAsStream("resources/db.properties")) {
                if (is != null) {
                    props.load(is);
                }
            } catch (IOException ignored) {
            }
        }

        this.dbUrl = props.getProperty("db.url",
            "jdbc:sqlserver://localhost:1433;databaseName=HotelMs;encrypt=false;trustServerCertificate=true");
        this.dbUser = props.getProperty("db.user", "sa");
        this.dbPassword = props.getProperty("db.password", "");
        Properties envFile = new Properties();
        File dotEnv = new File(".env");
        if(dotEnv.exists()) try(FileInputStream input=new FileInputStream(dotEnv)){ envFile.load(input); } catch(IOException e){ LOGGER.warning("Cannot read .env"); }
        this.dbUrl = configValue("DB_URL", envFile, this.dbUrl);
        this.dbUser = configValue("DB_USER", envFile, this.dbUser);
        this.dbPassword = configValue("DB_PASSWORD", envFile, configValue("MSSQL_SA_PASSWORD",envFile,this.dbPassword));
        this.dbDriver = props.getProperty("db.driver", "com.microsoft.sqlserver.jdbc.SQLServerDriver");
    }

    private static String configValue(String key, Properties file, String fallback) {
        String value=System.getenv(key);
        if(value==null || value.isBlank()) value=file.getProperty(key);
        if(value==null || value.isBlank())return fallback;
        value=value.trim();
        if(value.length()>1 && ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'")))) value=value.substring(1,value.length()-1);
        return value;
    }

    public Connection getConnection() throws SQLException {
        operationLock.lock();
        try {
        if (demoMode) {
            throw new SQLException("Đang ở chế độ Demo (không kết nối SQL Server).");
        }
        try {
            if (connection == null || connection.isClosed()) {
                Class.forName(dbDriver);
                DriverManager.setLoginTimeout(3);
                connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
                guardedConnection = guardConnection(connection);
            }
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQL Server JDBC Driver không tìm thấy: " + e.getMessage());
        }
        return guardedConnection;
        } finally { operationLock.unlock(); }
    }

    // Keep a statement's lifetime outside another thread's business transaction.
    private Connection guardConnection(Connection delegate) {
        return (Connection) java.lang.reflect.Proxy.newProxyInstance(Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class}, (proxy, method, args) -> {
                    boolean statement = method.getName().startsWith("prepare") || method.getName().equals("createStatement");
                    if (statement) operationLock.lock();
                    try {
                        Object result = method.invoke(delegate, args);
                        if (!statement) return result;
                        Statement actual = (Statement) result;
                        Class<?> type = actual instanceof CallableStatement ? CallableStatement.class
                                : actual instanceof PreparedStatement ? PreparedStatement.class : Statement.class;
                        java.util.concurrent.atomic.AtomicBoolean closed = new java.util.concurrent.atomic.AtomicBoolean();
                        return java.lang.reflect.Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (p, m, a) -> {
                            try { return m.invoke(actual, a); }
                            catch (java.lang.reflect.InvocationTargetException e) { throw e.getCause(); }
                            finally {
                                if (m.getName().equals("close") && closed.compareAndSet(false, true)) operationLock.unlock();
                            }
                        });
                    } catch (java.lang.reflect.InvocationTargetException e) {
                        if (statement) operationLock.unlock();
                        throw e.getCause();
                    } catch (RuntimeException | IllegalAccessException e) {
                        if (statement) operationLock.unlock();
                        throw e;
                    }
                });
    }

    void lockOperation() { operationLock.lock(); }
    void unlockOperation() { operationLock.unlock(); }

    public void beginTransaction() throws SQLException {
        operationLock.lock();
        try {
            if (operationLock.getHoldCount() != 1) throw new SQLException("Nested transaction is not supported");
            if (demoMode) DemoRepository.begin();
            else getConnection().setAutoCommit(false);
        } catch (SQLException | RuntimeException e) { operationLock.unlock(); throw e; }
    }

    public void commitTransaction() throws SQLException {
        if (!operationLock.isHeldByCurrentThread()) throw new SQLException("No active transaction");
        try {
            if (demoMode) DemoRepository.commit();
            else {
                try { connection.commit(); }
                catch (SQLException e) { connection.rollback(); throw e; }
                finally { connection.setAutoCommit(true); }
            }
        } finally { operationLock.unlock(); }
    }

    public void rollbackTransaction() {
        if (!operationLock.isHeldByCurrentThread()) return;
        try {
            if (demoMode) DemoRepository.rollback();
            else if (connection != null && !connection.isClosed()) {
                connection.rollback();
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi rollback transaction", e);
        } finally { operationLock.unlock(); }
    }

    public void closeConnection() {
        operationLock.lock();
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException ignored) {
        } finally { operationLock.unlock(); }
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
