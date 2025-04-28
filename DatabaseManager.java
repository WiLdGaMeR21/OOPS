import java.sql.*;
import java.util.LinkedList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.swing.JOptionPane;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/vehicle_rental";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "Im@student";
    private static final int MAX_POOL_SIZE = 10;
    private static final int INITIAL_POOL_SIZE = 3;

    private static DatabaseManager instance;
    private final LinkedList<Connection> connectionPool = new LinkedList<>();
    private final Lock poolLock = new ReentrantLock();

    public DatabaseManager() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            initializeDatabase();
            initializeConnectionPool();
        } catch (ClassNotFoundException | SQLException e) {
            JOptionPane.showMessageDialog(null,
                    "Database connection error: " + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    private void initializeConnectionPool() throws SQLException {
        poolLock.lock();
        try {
            for (int i = 0; i < INITIAL_POOL_SIZE; i++) {
                connectionPool.add(createNewConnection());
            }
        } finally {
            poolLock.unlock();
        }
    }

    private Connection createNewConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        conn.setAutoCommit(true); // Default to auto-commit
        return conn;
    }

    private void initializeDatabase() throws SQLException {
        // Create database if not exists
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306", DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS vehicle_rental");
        }

        // Create tables if not exists
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement()) {

            // Create vehicles table
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS vehicles (" +
                            "id INT AUTO_INCREMENT PRIMARY KEY, " +
                            "model VARCHAR(100) NOT NULL, " +
                            "type VARCHAR(50) NOT NULL, " +
                            "rent_per_day DECIMAL(10, 2) NOT NULL, " +
                            "quantity INT NOT NULL DEFAULT 1, " +
                            "available_quantity INT NOT NULL DEFAULT 1)");

            // Create rental_records table
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS rental_records (" +
                            "id INT AUTO_INCREMENT PRIMARY KEY, " +
                            "vehicle_id INT, " +
                            "username VARCHAR(50) NOT NULL, " +
                            "rent_date DATETIME NOT NULL, " +
                            "return_date DATETIME, " +
                            "total_cost DECIMAL(10, 2), " +
                            "FOREIGN KEY (vehicle_id) REFERENCES vehicles(id) ON DELETE SET NULL)");

            // Check if the table is empty, add sample data if needed
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM vehicles");
            rs.next();
            int count = rs.getInt(1);

            if (count == 0) {
                // Insert sample data with quantity information
                String[] insertQueries = {
                        "INSERT INTO vehicles (model, type, rent_per_day, quantity, available_quantity) VALUES ('Toyota Corolla', 'Car', 50, 3, 3)",
                        "INSERT INTO vehicles (model, type, rent_per_day, quantity, available_quantity) VALUES ('Honda Civic', 'Car', 60, 2, 2)",
                        "INSERT INTO vehicles (model, type, rent_per_day, quantity, available_quantity) VALUES ('Yamaha R15', 'Bike', 30, 5, 5)",
                        "INSERT INTO vehicles (model, type, rent_per_day, quantity, available_quantity) VALUES ('Suzuki Swift', 'Car', 55, 2, 2)"
                };

                for (String query : insertQueries) {
                    stmt.executeUpdate(query);
                }
            }
        }
    }

    public Connection getConnection() throws SQLException {
        poolLock.lock();
        try {
            if (connectionPool.isEmpty()) {
                if (connectionPool.size() < MAX_POOL_SIZE) {
                    return createNewConnection();
                } else {
                    // Wait for a connection to become available
                    poolLock.unlock();
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    poolLock.lock();
                    return getConnection();
                }
            }
            Connection conn = connectionPool.removeFirst();

            // Verify connection is still valid
            if (conn.isClosed() || !conn.isValid(1)) {
                conn = createNewConnection();
            }

            return conn;
        } finally {
            poolLock.unlock();
        }
    }

    public void releaseConnection(Connection conn) {
        if (conn == null) return;

        poolLock.lock();
        try {
            // Only add back valid connections
            if (!conn.isClosed() && conn.isValid(1)) {
                // Reset to default state before returning to pool
                if (!conn.getAutoCommit()) {
                    conn.setAutoCommit(true);
                }
                connectionPool.add(conn);
            }
        } catch (SQLException e) {
            // If there's an issue with the connection, don't add it back
            try {
                conn.close();
            } catch (SQLException ex) {
                // Ignore
            }
        } finally {
            poolLock.unlock();
        }
    }

    public void shutdown() {
        poolLock.lock();
        try {
            for (Connection conn : connectionPool) {
                try {
                    if (conn != null && !conn.isClosed()) {
                        conn.close();
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            connectionPool.clear();
        } finally {
            poolLock.unlock();
        }
    }

    // Utility method for executing transactions
    public boolean executeTransaction(TransactionHandler handler) {
        Connection conn = null;
        boolean success = false;

        try {
            conn = getConnection();
            conn.setAutoCommit(false);

            success = handler.execute(conn);

            if (success) {
                conn.commit();
            } else {
                conn.rollback();
            }

            return success;
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                releaseConnection(conn);
            }
        }
    }

    // Interface for transaction handling
    public interface TransactionHandler {
        boolean execute(Connection connection) throws SQLException;
    }
}