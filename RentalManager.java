import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;


/**
 * Manages operations related to vehicle rental
 */
public class RentalManager {
    private final DatabaseManager dbManager;
    private final UserSession userSession;

    /**
     * Constructs a new rental manager
     */
    public RentalManager() {
        this.dbManager = new DatabaseManager();
        this.userSession = UserSession.getInstance();
    }

    /**
     * Gets all vehicles in the system
     */
    public List<Vehicle> getAllVehicles() {
        List<Vehicle> vehicles = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM vehicles ORDER BY id")) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String model = rs.getString("model");
                String type = rs.getString("type");
                double rentPerDay = rs.getDouble("rent_per_day");
                boolean isAvailable = rs.getBoolean("is_available");

                vehicles.add(new Vehicle(id, model, type, rentPerDay, isAvailable));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null,
                    "Database error: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }

        return vehicles;
    }

    /**
     * Gets only the available vehicles
     */
    public List<Vehicle> getAvailableVehicles() {
        List<Vehicle> vehicles = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM vehicles WHERE is_available = TRUE ORDER BY id")) {

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                int id = rs.getInt("id");
                String model = rs.getString("model");
                String type = rs.getString("type");
                double rentPerDay = rs.getDouble("rent_per_day");
                boolean isAvailable = rs.getBoolean("is_available");

                vehicles.add(new Vehicle(id, model, type, rentPerDay, isAvailable));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null,
                    "Database error: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }

        return vehicles;
    }

    /**
     * Rents a vehicle
     */
    public boolean rentVehicle(int vehicleId) {
        if (!userSession.isLoggedIn()) {
            return false;
        }

        try (Connection conn = dbManager.getConnection()) {
            // First check if vehicle exists and is available
            try (PreparedStatement checkStmt = conn.prepareStatement(
                    "SELECT * FROM vehicles WHERE id = ? AND is_available = TRUE")) {

                checkStmt.setInt(1, vehicleId);
                ResultSet rs = checkStmt.executeQuery();

                if (!rs.next()) {
                    // Vehicle not found or not available
                    return false;
                }

                // Vehicle is available, update its status
                try (PreparedStatement updateStmt = conn.prepareStatement(
                        "UPDATE vehicles SET is_available = FALSE WHERE id = ?")) {

                    updateStmt.setInt(1, vehicleId);
                    updateStmt.executeUpdate();
                }

                // Create rental record
                try (PreparedStatement insertStmt = conn.prepareStatement(
                        "INSERT INTO rental_records (vehicle_id, username) VALUES (?, ?)")) {

                    insertStmt.setInt(1, vehicleId);
                    insertStmt.setString(2, userSession.getUsername());
                    insertStmt.executeUpdate();
                }

                return true;
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null,
                    "Database error: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Returns a rented vehicle
     */
    public boolean returnVehicle(int vehicleId) {
        if (!userSession.isLoggedIn()) {
            return false;
        }

        try (Connection conn = dbManager.getConnection()) {
            // First check if vehicle exists and is currently rented
            try (PreparedStatement checkStmt = conn.prepareStatement(
                    "SELECT * FROM vehicles WHERE id = ? AND is_available = FALSE")) {

                checkStmt.setInt(1, vehicleId);
                ResultSet rs = checkStmt.executeQuery();

                if (!rs.next()) {
                    // Vehicle not found or not rented
                    return false;
                }

                // Get rental record
                try (PreparedStatement rentalStmt = conn.prepareStatement(
                        "SELECT id, rent_date FROM rental_records WHERE vehicle_id = ? AND return_date IS NULL")) {

                    rentalStmt.setInt(1, vehicleId);
                    ResultSet rentalRs = rentalStmt.executeQuery();

                    if (!rentalRs.next()) {
                        // No active rental found
                        return false;
                    }

                    int rentalId = rentalRs.getInt("id");

                    // Update vehicle status
                    try (PreparedStatement updateVehicleStmt = conn.prepareStatement(
                            "UPDATE vehicles SET is_available = TRUE WHERE id = ?")) {

                        updateVehicleStmt.setInt(1, vehicleId);
                        updateVehicleStmt.executeUpdate();
                    }

                    // Update rental record
                    try (PreparedStatement updateRentalStmt = conn.prepareStatement(
                            "UPDATE rental_records SET return_date = CURRENT_TIMESTAMP WHERE id = ?")) {

                        updateRentalStmt.setInt(1, rentalId);
                        updateRentalStmt.executeUpdate();
                    }

                    return true;
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null,
                    "Database error: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Adds a new vehicle to the system
     */
    public void addVehicle(String model, String type, double rentPerDay) {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO vehicles (model, type, rent_per_day, is_available) VALUES (?, ?, ?, TRUE)")) {

            stmt.setString(1, model);
            stmt.setString(2, type);
            stmt.setDouble(3, rentPerDay);

            stmt.executeUpdate();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null,
                    "Database error: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    /**
     * Removes a vehicle from the system
     */
    public boolean removeVehicle(int vehicleId) {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "DELETE FROM vehicles WHERE id = ? AND is_available = TRUE")) {

            stmt.setInt(1, vehicleId);
            int rowsAffected = stmt.executeUpdate();

            return rowsAffected > 0;
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null,
                    "Database error: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Close resources when application shuts down
     */
    public void closeResources() {
        dbManager.closeConnection();
    }
}