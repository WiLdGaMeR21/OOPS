import java.sql.*;
import java.util.*;
import javax.swing.JOptionPane;
import java.time.LocalDateTime;

/**
 * Data model for the rental system using JDBC
 */
public class RentalManager {
    private final DatabaseManager dbManager;
    private final FileManager fileManager;

    public RentalManager() {
        dbManager = new DatabaseManager();
        fileManager = FileManager.getInstance();
    }

    public synchronized List<Vehicle> getAllVehicles() {
        List<Vehicle> vehicles = new ArrayList<>();
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement()) {
            boolean hasQuantityColumns = false;
            try (ResultSet columns = conn.getMetaData().getColumns(null, null, "vehicles", "quantity")) {
                hasQuantityColumns = columns.next();
            }

            String query = hasQuantityColumns ?
                    "SELECT id, model, type, rent_per_day, is_available, quantity, available_quantity FROM vehicles" :
                    "SELECT id, model, type, rent_per_day, is_available FROM vehicles";

            try (ResultSet rs = stmt.executeQuery(query)) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String model = rs.getString("model");
                    String type = rs.getString("type");
                    double rentPerDay = rs.getDouble("rent_per_day");
                    boolean isAvailable = rs.getBoolean("is_available");

                    vehicles.add(hasQuantityColumns ?
                            new Vehicle(id, model, type, rentPerDay, isAvailable,
                                    rs.getInt("quantity"), rs.getInt("available_quantity")) :
                            new Vehicle(id, model, type, rentPerDay, isAvailable));
                }
            }
        } catch (SQLException e) {
            showError("Error retrieving vehicles", e);
        }
        return vehicles;
    }

    public synchronized List<Vehicle> getAvailableVehicles() {
        List<Vehicle> availableVehicles = new ArrayList<>();
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM vehicles WHERE available_quantity > 0")) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    availableVehicles.add(new Vehicle(
                            rs.getInt("id"), rs.getString("model"), rs.getString("type"),
                            rs.getDouble("rent_per_day"), rs.getBoolean("is_available"),
                            rs.getInt("quantity"), rs.getInt("available_quantity")));
                }
            }
        } catch (SQLException e) {
            showError("Error retrieving available vehicles", e);
        }
        return availableVehicles;
    }

    public synchronized Optional<Vehicle> getVehicleById(int id) {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM vehicles WHERE id = ?")) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Vehicle(
                            rs.getInt("id"), rs.getString("model"), rs.getString("type"),
                            rs.getDouble("rent_per_day"), rs.getBoolean("is_available")));
                }
            }
        } catch (SQLException e) {
            showError("Error retrieving vehicle", e);
        }
        return Optional.empty();
    }

    public synchronized boolean updateVehicleQuantity(int vehicleId, int newQuantity) {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement checkStmt = conn.prepareStatement("SELECT id, is_available FROM vehicles WHERE id = ?")) {
            checkStmt.setInt(1, vehicleId);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (!rs.next()) return false;

                boolean isAvailable = rs.getBoolean("is_available");
                boolean hasQuantityColumns = false;
                int currentRented = 0;

                try (Statement stmt = conn.createStatement()) {
                    try (ResultSet columns = conn.getMetaData().getColumns(null, null, "vehicles", "quantity")) {
                        hasQuantityColumns = columns.next();
                    }

                    if (!hasQuantityColumns) {
                        stmt.executeUpdate("ALTER TABLE vehicles ADD COLUMN quantity INT DEFAULT 1");
                        stmt.executeUpdate("ALTER TABLE vehicles ADD COLUMN available_quantity INT DEFAULT 1");
                        stmt.executeUpdate("UPDATE vehicles SET quantity = 1, " +
                                "available_quantity = CASE WHEN is_available = 1 THEN 1 ELSE 0 END");
                    }
                }

                if (hasQuantityColumns) {
                    try (PreparedStatement quantStmt = conn.prepareStatement(
                            "SELECT quantity, available_quantity FROM vehicles WHERE id = ?")) {
                        quantStmt.setInt(1, vehicleId);
                        try (ResultSet qrs = quantStmt.executeQuery()) {
                            if (qrs.next()) {
                                currentRented = qrs.getInt("quantity") - qrs.getInt("available_quantity");
                            }
                        }
                    }
                } else {
                    currentRented = isAvailable ? 0 : 1;
                }

                if (newQuantity < currentRented) return false;

                int newAvailable = newQuantity - currentRented;
                try (PreparedStatement updateStmt = conn.prepareStatement(
                        "UPDATE vehicles SET quantity = ?, available_quantity = ?, is_available = ? WHERE id = ?")) {
                    updateStmt.setInt(1, newQuantity);
                    updateStmt.setInt(2, newAvailable);
                    updateStmt.setBoolean(3, newAvailable > 0);
                    updateStmt.setInt(4, vehicleId);
                    return updateStmt.executeUpdate() > 0;
                }
            }
        } catch (SQLException e) {
            showError("Error updating vehicle quantity", e);
            return false;
        }
    }

    public synchronized boolean rentVehicle(int id) {
        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            conn.setAutoCommit(false);

            // Check if the vehicle is available first
            try (PreparedStatement checkStmt = conn.prepareStatement(
                    "SELECT available_quantity FROM vehicles WHERE id = ? FOR UPDATE")) {
                checkStmt.setInt(1, id);
                boolean canRent = false;

                try (ResultSet rs = checkStmt.executeQuery()) {
                    canRent = rs.next() && rs.getInt("available_quantity") > 0;
                }

                if (canRent) {
                    // Update vehicle available quantity
                    try (PreparedStatement updateStmt = conn.prepareStatement(
                            "UPDATE vehicles SET available_quantity = available_quantity - 1, " +
                                    "is_available = CASE WHEN available_quantity - 1 > 0 THEN 1 ELSE 0 END " +
                                    "WHERE id = ?")) {
                        updateStmt.setInt(1, id);
                        updateStmt.executeUpdate();
                    }

                    // Insert rental record
                    String username = LoginPanel.UserSession.getInstance().getUsername();
                    try (PreparedStatement insertRentalStmt = conn.prepareStatement(
                            "INSERT INTO rental_records (vehicle_id, username, rent_date) VALUES (?, ?, ?)")) {
                        insertRentalStmt.setInt(1, id);
                        insertRentalStmt.setString(2, username);
                        insertRentalStmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
                        insertRentalStmt.executeUpdate();
                    }

                    conn.commit();
                    fileManager.processRental(LoginPanel.UserSession.getInstance().getUsername(), id, this);
                    return true;
                }

                conn.rollback();
                return false;
            }
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    // Log rollback error
                    rollbackEx.printStackTrace();
                }
            }
            showError("Error renting vehicle", e);
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    // Don't close the connection here, let it be managed by dbManager
                } catch (SQLException e) {
                    // Log autocommit error
                    e.printStackTrace();
                }
            }
        }
    }

    public synchronized boolean returnVehicle(int vehicleId) {
        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            conn.setAutoCommit(false);

            // Check if the vehicle exists and was rented
            String checkQuery = "SELECT v.id, v.quantity, v.available_quantity FROM vehicles v " +
                    "WHERE v.id = ?";

            try (PreparedStatement checkStmt = conn.prepareStatement(checkQuery)) {
                checkStmt.setInt(1, vehicleId);
                ResultSet rs = checkStmt.executeQuery();

                if (!rs.next()) {
                    conn.rollback();
                    return false; // Vehicle doesn't exist
                }

                int availableQty = rs.getInt("available_quantity");
                int totalQty = rs.getInt("quantity");

                // Can't return more than total
                if (availableQty >= totalQty) {
                    conn.rollback();
                    return false; // Already all returned
                }

                // Get the latest rental record for this vehicle that hasn't been returned
                String rentalQuery = "SELECT id, username FROM rental_records " +
                        "WHERE vehicle_id = ? AND return_date IS NULL " +
                        "ORDER BY rent_date DESC LIMIT 1";

                int rentalId = -1;
                String username = "";
                try (PreparedStatement rentalStmt = conn.prepareStatement(rentalQuery)) {
                    rentalStmt.setInt(1, vehicleId);
                    ResultSet rentalRs = rentalStmt.executeQuery();

                    if (!rentalRs.next()) {
                        conn.rollback();
                        return false; // No active rental found
                    }

                    rentalId = rentalRs.getInt("id");
                    username = rentalRs.getString("username");
                }

                // Update the rental record
                String updateRentalQuery = "UPDATE rental_records SET return_date = NOW() " +
                        "WHERE id = ?";

                try (PreparedStatement updateRentalStmt = conn.prepareStatement(updateRentalQuery)) {
                    updateRentalStmt.setInt(1, rentalId);
                    updateRentalStmt.executeUpdate();
                }

                // Update the vehicle availability
                String updateVehicleQuery = "UPDATE vehicles SET available_quantity = available_quantity + 1, " +
                        "is_available = 1 " +
                        "WHERE id = ?";

                try (PreparedStatement updateVehicleStmt = conn.prepareStatement(updateVehicleQuery)) {
                    updateVehicleStmt.setInt(1, vehicleId);
                    updateVehicleStmt.executeUpdate();
                }

                // Process the return through FileManager
                fileManager.processReturn(username, vehicleId, this);

                conn.commit();
                return true;
            }
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    rollbackEx.printStackTrace();
                }
            }
            showError("Error returning vehicle", e);
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public synchronized void addVehicle(String model, String type, double rentPerDay, int quantity) {
        validateVehicleData(model, type, rentPerDay);
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO vehicles (model, type, rent_per_day, is_available, quantity, available_quantity) VALUES (?, ?, ?, ?, ?, ?)")) {
            stmt.setString(1, model);
            stmt.setString(2, type);
            stmt.setDouble(3, rentPerDay);
            stmt.setBoolean(4, true);
            stmt.setInt(5, quantity);
            stmt.setInt(6, quantity);
            stmt.executeUpdate();
        } catch (SQLException e) {
            showError("Error adding vehicle", e);
        }
    }

    public synchronized void addVehicle(String model, String type, double rentPerDay) {
        addVehicle(model, type, rentPerDay, 1);
    }

    private void validateVehicleData(String model, String type, double rentPerDay) {
        if (model == null || model.trim().isEmpty()) throw new IllegalArgumentException("Model cannot be empty");
        if (type == null || type.trim().isEmpty()) throw new IllegalArgumentException("Type cannot be empty");
        if (rentPerDay <= 0) throw new IllegalArgumentException("Rent per day must be positive");
    }

    public synchronized boolean removeVehicle(int id) {
        try (Connection conn = dbManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement checkRentalStmt = conn.prepareStatement(
                        "SELECT COUNT(*) FROM rental_records WHERE vehicle_id = ? AND return_date IS NULL")) {
                    checkRentalStmt.setInt(1, id);
                    try (ResultSet rs = checkRentalStmt.executeQuery()) {
                        if (rs.next() && rs.getInt(1) == 0) {
                            try (PreparedStatement deleteStmt = conn.prepareStatement("DELETE FROM vehicles WHERE id = ?")) {
                                deleteStmt.setInt(1, id);
                                int rowsAffected = deleteStmt.executeUpdate();
                                if (rowsAffected > 0) {
                                    conn.commit();
                                    return true;
                                }
                            }
                        } else {
                            JOptionPane.showMessageDialog(null, "Cannot remove vehicle with active rentals",
                                    "Remove Failed", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
                conn.rollback();
                return false;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            showError("Error removing vehicle", e);
            return false;
        }
    }

    public synchronized boolean updateVehicle(int id, String model, String type, double rentPerDay) {
        validateVehicleData(model, type, rentPerDay);
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE vehicles SET model = ?, type = ?, rent_per_day = ? WHERE id = ?")) {
            stmt.setString(1, model);
            stmt.setString(2, type);
            stmt.setDouble(3, rentPerDay);
            stmt.setInt(4, id);
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                JOptionPane.showMessageDialog(null, "No vehicle found with ID: " + id,
                        "Update Failed", JOptionPane.ERROR_MESSAGE);
            }
            return rowsAffected > 0;
        } catch (SQLException e) {
            showError("Error updating vehicle", e);
            return false;
        }
    }

    public synchronized LocalDateTime getRentalTime(int vehicleId) {
        try (Connection conn = dbManager.getConnection()) {
            // First check active rentals
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT rent_date FROM rental_records WHERE vehicle_id = ? AND return_date IS NULL")) {
                stmt.setInt(1, vehicleId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) return rs.getTimestamp("rent_date").toLocalDateTime();
                }
            }

            // If no active rental, check most recently returned
            try (PreparedStatement returnedStmt = conn.prepareStatement(
                    "SELECT rent_date FROM rental_records WHERE vehicle_id = ? ORDER BY return_date DESC LIMIT 1")) {
                returnedStmt.setInt(1, vehicleId);
                try (ResultSet returnedRs = returnedStmt.executeQuery()) {
                    if (returnedRs.next()) return returnedRs.getTimestamp("rent_date").toLocalDateTime();
                }
            }
        } catch (SQLException e) {
            showError("Failed to retrieve rental time", e);
        }
        return null;
    }

    public synchronized void updateRentalRecord(int vehicleId, double totalCost) {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE rental_records SET total_cost = ? WHERE vehicle_id = ? AND return_date IS NOT NULL " +
                             "ORDER BY return_date DESC LIMIT 1")) {
            stmt.setDouble(1, totalCost);
            stmt.setInt(2, vehicleId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            showError("Failed to update rental record", e);
        }
    }

    public synchronized List<RentalRecord> getUserRentalHistory(String username) {
        List<RentalRecord> rentalRecords = new ArrayList<>();
        String query = "SELECT r.*, v.model, v.type FROM rental_records r " +
                "JOIN vehicles v ON r.vehicle_id = v.id " +
                "WHERE r.username = ? ORDER BY r.rent_date DESC";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    rentalRecords.add(new RentalRecord(
                            rs.getInt("id"), rs.getInt("vehicle_id"), rs.getString("username"),
                            rs.getTimestamp("rent_date").toLocalDateTime(),
                            rs.getTimestamp("return_date") != null ? rs.getTimestamp("return_date").toLocalDateTime() : null,
                            rs.getDouble("total_cost"), rs.getString("model"), rs.getString("type")
                    ));
                }
            }
        } catch (SQLException e) {
            showError("Error retrieving rental history", e);
        }
        return rentalRecords;
    }

    public synchronized List<RentalRecord> getActiveRentals() {
        List<RentalRecord> activeRentals = new ArrayList<>();
        String query = "SELECT r.*, v.model, v.type FROM rental_records r " +
                "JOIN vehicles v ON r.vehicle_id = v.id " +
                "WHERE r.return_date IS NULL ORDER BY r.rent_date ASC";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                activeRentals.add(new RentalRecord(
                        rs.getInt("id"), rs.getInt("vehicle_id"), rs.getString("username"),
                        rs.getTimestamp("rent_date").toLocalDateTime(), null, 0.0,
                        rs.getString("model"), rs.getString("type")
                ));
            }
        } catch (SQLException e) {
            showError("Error retrieving active rentals", e);
        }
        return activeRentals;
    }

    public synchronized boolean isVehicleRentedByUser(String username, int vehicleId) {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT COUNT(*) FROM rental_records WHERE vehicle_id = ? AND username = ? AND return_date IS NULL")) {
            stmt.setInt(1, vehicleId);
            stmt.setString(2, username);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            showError("Error checking rental status", e);
            return false;
        }
    }

    public synchronized RentalStatistics getRentalStatistics() {
        int totalVehicles = 0, availableVehicles = 0, activeRentals = 0;
        double totalRevenue = 0.0;

        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement()) {
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM vehicles")) {
                if (rs.next()) totalVehicles = rs.getInt(1);
            }
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM vehicles WHERE is_available = true")) {
                if (rs.next()) availableVehicles = rs.getInt(1);
            }
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM rental_records WHERE return_date IS NULL")) {
                if (rs.next()) activeRentals = rs.getInt(1);
            }
            try (ResultSet rs = stmt.executeQuery("SELECT SUM(total_cost) FROM rental_records WHERE return_date IS NOT NULL")) {
                if (rs.next()) totalRevenue = rs.getDouble(1);
            }
        } catch (SQLException e) {
            showError("Error retrieving rental statistics", e);
        }

        return new RentalStatistics(totalVehicles, availableVehicles, activeRentals, totalRevenue);
    }

    public DatabaseManager getDbManager() {
        return dbManager;
    }

    public void closeResources() {
        // Use shutdown() instead of closeConnection()
        dbManager.shutdown();
        fileManager.shutdown();
    }

    private void showError(String message, Exception e) {
        JOptionPane.showMessageDialog(null, message + ": " + e.getMessage(),
                "Database Error", JOptionPane.ERROR_MESSAGE);
        e.printStackTrace();
    }

    // Inner classes
    public static class RentalRecord {
        private int id, vehicleId;
        private String username, vehicleModel, vehicleType;
        private LocalDateTime rentDate, returnDate;
        private double totalCost;

        public RentalRecord(int id, int vehicleId, String username, LocalDateTime rentDate,
                            LocalDateTime returnDate, double totalCost, String vehicleModel, String vehicleType) {
            this.id = id; this.vehicleId = vehicleId; this.username = username;
            this.rentDate = rentDate; this.returnDate = returnDate; this.totalCost = totalCost;
            this.vehicleModel = vehicleModel; this.vehicleType = vehicleType;
        }

        public int getId() { return id; }
        public int getVehicleId() { return vehicleId; }
        public String getUsername() { return username; }
        public LocalDateTime getRentDate() { return rentDate; }
        public LocalDateTime getReturnDate() { return returnDate; }
        public double getTotalCost() { return totalCost; }
        public String getVehicleModel() { return vehicleModel; }
        public String getVehicleType() { return vehicleType; }
        public boolean isActive() { return returnDate == null; }

        public int getDurationDays() {
            if (returnDate == null) return -1;
            long hours = java.time.Duration.between(rentDate, returnDate).toHours();
            return (int) Math.ceil(hours / 24.0); // Round up to the nearest day
        }

        @Override
        public String toString() {
            return "RentalRecord{id=" + id + ", vehicleId=" + vehicleId +
                    ", username='" + username + "', vehicleModel='" + vehicleModel +
                    "', rentDate=" + rentDate + ", returnDate=" + returnDate +
                    ", totalCost=" + totalCost + '}';
        }
    }

    public static class RentalStatistics {
        private int totalVehicles, availableVehicles, activeRentals;
        private double totalRevenue;

        public RentalStatistics(int totalVehicles, int availableVehicles, int activeRentals, double totalRevenue) {
            this.totalVehicles = totalVehicles; this.availableVehicles = availableVehicles;
            this.activeRentals = activeRentals; this.totalRevenue = totalRevenue;
        }

        public int getTotalVehicles() { return totalVehicles; }
        public int getAvailableVehicles() { return availableVehicles; }
        public int getActiveRentals() { return activeRentals; }
        public double getTotalRevenue() { return totalRevenue; }
        public int getRentedVehicles() { return totalVehicles - availableVehicles; }

        public double getRentedPercentage() {
            return totalVehicles == 0 ? 0.0 :
                    (double)(totalVehicles - availableVehicles) / totalVehicles * 100.0;
        }
    }
}