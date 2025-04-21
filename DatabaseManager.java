import java.sql.*;
import javax.swing.JOptionPane;
import java.util.Date;

/**
 * Database helper for the rental system
 */
public class DatabaseManager {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/vehicle_rental";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "Im@student";

    private Connection connection;

    public DatabaseManager() {
        try {
            // Register JDBC driver
            Class.forName("com.mysql.cj.jdbc.Driver");

            // Initialize database if not exists
            initializeDatabase();
        } catch (ClassNotFoundException | SQLException e) {
            JOptionPane.showMessageDialog(null,
                    "Database connection error: " + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void initializeDatabase() throws SQLException {
        // First connect to MySQL without selecting a database
        try (Connection conn = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306", DB_USER, DB_PASSWORD)) {

            // Create database if not exists
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS vehicle_rental");
            }
        }

        // Now connect to the database and create tables if they don't exist
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // Create vehicles table
            String createVehiclesTableSQL =
                    "CREATE TABLE IF NOT EXISTS vehicles (" +
                            "id INT AUTO_INCREMENT PRIMARY KEY, " +
                            "model VARCHAR(100) NOT NULL, " +
                            "type VARCHAR(50) NOT NULL, " +
                            "rent_per_day DECIMAL(10, 2) NOT NULL, " +
                            "is_available BOOLEAN NOT NULL DEFAULT TRUE)";

            stmt.executeUpdate(createVehiclesTableSQL);

            // Create user_wallets table
            String createWalletsTableSQL =
                    "CREATE TABLE IF NOT EXISTS user_wallets (" +
                            "username VARCHAR(50) PRIMARY KEY, " +
                            "balance DECIMAL(10, 2) NOT NULL DEFAULT 0.0)";

            stmt.executeUpdate(createWalletsTableSQL);

            // Create rental_records table
            String createRentalRecordsTableSQL =
                    "CREATE TABLE IF NOT EXISTS rental_records (" +
                            "id INT AUTO_INCREMENT PRIMARY KEY, " +
                            "vehicle_id INT NOT NULL, " +
                            "username VARCHAR(50) NOT NULL, " +
                            "rent_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                            "return_date TIMESTAMP NULL, " +
                            "total_cost DECIMAL(10, 2) NULL, " +
                            "FOREIGN KEY (vehicle_id) REFERENCES vehicles(id), " +
                            "FOREIGN KEY (username) REFERENCES user_wallets(username))";

            stmt.executeUpdate(createRentalRecordsTableSQL);

            // Check if the vehicles table is empty, add sample data if needed
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM vehicles");
            rs.next();
            int count = rs.getInt(1);

            if (count == 0) {
                // Insert sample data
                String[] insertQueries = {
                        "INSERT INTO vehicles (model, type, rent_per_day, is_available) VALUES ('Toyota Corolla', 'Car', 50, TRUE)",
                        "INSERT INTO vehicles (model, type, rent_per_day, is_available) VALUES ('Honda Civic', 'Car', 60, TRUE)",
                        "INSERT INTO vehicles (model, type, rent_per_day, is_available) VALUES ('Yamaha R15', 'Bike', 30, TRUE)",
                        "INSERT INTO vehicles (model, type, rent_per_day, is_available) VALUES ('Suzuki Swift', 'Car', 55, TRUE)"
                };

                for (String query : insertQueries) {
                    stmt.executeUpdate(query);
                }
            }

            // Initialize default user wallets
            // First, check if they exist
            rs = stmt.executeQuery("SELECT COUNT(*) FROM user_wallets");
            rs.next();
            int walletCount = rs.getInt(1);

            if (walletCount == 0) {
                // Insert default wallets for users
                String[] insertWallets = {
                        "INSERT INTO user_wallets (username, balance) VALUES ('admin', 1000.0)",
                        "INSERT INTO user_wallets (username, balance) VALUES ('user1', 500.0)",
                        "INSERT INTO user_wallets (username, balance) VALUES ('user2', 500.0)"
                };

                for (String query : insertWallets) {
                    stmt.executeUpdate(query);
                }
            }
        }
    }

    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        }
        return connection;
    }

    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}