import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.sql.*;
import javax.swing.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.awt.Desktop;
import java.util.Date;

/**
 * Handles receipt generation and logging for the Vehicle Rental System
 */
public class ReceiptManager {
    private static final String RECEIPTS_DIRECTORY = "receipts";
    private static final String LOGS_DIRECTORY = "logs";
    private final DatabaseManager dbManager;

    /**
     * Constructs a new receipt manager
     * @param dbManager The database manager instance
     */
    public ReceiptManager(DatabaseManager dbManager) {
        this.dbManager = dbManager;
        initializeDirectories();
    }

    /**
     * Creates necessary directories for receipts and logs
     */
    private void initializeDirectories() {
        try {
            Files.createDirectories(Paths.get(RECEIPTS_DIRECTORY));
            Files.createDirectories(Paths.get(LOGS_DIRECTORY));
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null,
                    "Failed to create receipt/log directories: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    /**
     * Creates a receipt when a vehicle is rented
     * @param username The username of the renter
     * @param vehicleId The ID of the rented vehicle
     * @return True if receipt creation was successful
     */
    public boolean createRentalReceipt(String username, int vehicleId) {
        try (Connection conn = dbManager.getConnection()) {
            // Get vehicle information
            Vehicle vehicle = getVehicleById(vehicleId);
            if (vehicle == null) {
                return false;
            }

            LocalDateTime rentalTime = LocalDateTime.now();
            String formattedDateTime = formatDateTime(rentalTime);

            // Create receipt file
            String receiptFileName = String.format("%s_rental_%d_%s.txt",
                    username, vehicleId, formattedDateTime.replace(":", "-").replace(" ", "_"));

            Path receiptPath = Paths.get(RECEIPTS_DIRECTORY, receiptFileName);

            try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(receiptPath))) {
                writer.println("=========================================");
                writer.println("          VEHICLE RENTAL RECEIPT         ");
                writer.println("=========================================");
                writer.println("Receipt ID: " + UUID.randomUUID().toString().substring(0, 8));
                writer.println("Date & Time: " + formattedDateTime);
                writer.println("-----------------------------------------");
                writer.println("Customer: " + username);
                writer.println("-----------------------------------------");
                writer.println("Vehicle Details:");
                writer.println("  ID: " + vehicle.getId());
                writer.println("  Model: " + vehicle.getModel());
                writer.println("  Type: " + vehicle.getType());
                writer.println("  Daily Rate: $" + String.format("%.2f", vehicle.getRentPerDay()));
                writer.println("-----------------------------------------");
                writer.println("Please return the vehicle in good condition.");
                writer.println("Late fees may apply for delayed returns.");
                writer.println("=========================================");
                writer.println("Thank you for choosing our service!");
                writer.println("=========================================");
            }

            // Log the rental
            logRental(username, vehicleId, vehicle.getModel(), rentalTime);

            // Show receipt to user
            showReceiptToUser(receiptPath.toString());

            return true;

        } catch (SQLException | IOException e) {
            JOptionPane.showMessageDialog(null,
                    "Failed to create rental receipt: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Creates a return receipt and calculates the amount to be paid
     * @param username The username of the renter
     * @param vehicleId The ID of the returned vehicle
     * @return True if receipt creation was successful
     */
    public boolean createReturnReceipt(String username, int vehicleId) {
        try (Connection conn = dbManager.getConnection()) {
            // Get vehicle information
            Vehicle vehicle = getVehicleById(vehicleId);
            if (vehicle == null) {
                return false;
            }

            // Get rental record to calculate duration
            LocalDateTime rentalTime = getRentalTime(vehicleId);
            if (rentalTime == null) {
                JOptionPane.showMessageDialog(null,
                        "No active rental found for this vehicle",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }

            LocalDateTime returnTime = LocalDateTime.now();

            // Calculate rental duration and cost
            long hours = ChronoUnit.HOURS.between(rentalTime, returnTime);
            int days = (int) Math.ceil(hours / 24.0); // Round up to the nearest day
            if (days == 0) days = 1; // Minimum 1 day charge

            double totalCost = days * vehicle.getRentPerDay();

            // Create receipt file
            String formattedDateTime = formatDateTime(returnTime);
            String receiptFileName = String.format("%s_return_%d_%s.txt",
                    username, vehicleId, formattedDateTime.replace(":", "-").replace(" ", "_"));

            Path receiptPath = Paths.get(RECEIPTS_DIRECTORY, receiptFileName);

            try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(receiptPath))) {
                writer.println("=========================================");
                writer.println("         VEHICLE RETURN RECEIPT          ");
                writer.println("=========================================");
                writer.println("Receipt ID: " + UUID.randomUUID().toString().substring(0, 8));
                writer.println("Return Date & Time: " + formattedDateTime);
                writer.println("-----------------------------------------");
                writer.println("Customer: " + username);
                writer.println("-----------------------------------------");
                writer.println("Vehicle Details:");
                writer.println("  ID: " + vehicle.getId());
                writer.println("  Model: " + vehicle.getModel());
                writer.println("  Type: " + vehicle.getType());
                writer.println("-----------------------------------------");
                writer.println("Rental Information:");
                writer.println("  Rental Date: " + formatDateTime(rentalTime));
                writer.println("  Return Date: " + formattedDateTime);
                writer.println("  Duration: " + days + " day(s)");
                writer.println("  Rate per Day: $" + String.format("%.2f", vehicle.getRentPerDay()));
                writer.println("-----------------------------------------");
                writer.println("Total Amount Due: $" + String.format("%.2f", totalCost));
                writer.println("=========================================");
                writer.println("Thank you for choosing our service!");
                writer.println("=========================================");
            }

            // Log the return
            logReturn(username, vehicleId, vehicle.getModel(), returnTime, totalCost);

            // Update rental record with cost
            updateRentalRecord(vehicleId, totalCost);

            // Show receipt and amount due to user
            showReceiptToUser(receiptPath.toString());

            // Show payment due popup
            JOptionPane.showMessageDialog(null,
                    "Thank you for returning Vehicle #" + vehicleId + "\n" +
                            "Amount Due: $" + String.format("%.2f", totalCost) + "\n" +
                            "Rental Duration: " + days + " day(s)",
                    "Payment Due", JOptionPane.INFORMATION_MESSAGE);

            return true;

        } catch (SQLException | IOException e) {
            JOptionPane.showMessageDialog(null,
                    "Failed to create return receipt: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Updates the rental record with the final cost
     */
    private void updateRentalRecord(int vehicleId, double totalCost) throws SQLException {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE rental_records SET total_cost = ? WHERE vehicle_id = ? AND return_date IS NOT NULL ORDER BY return_date DESC LIMIT 1")) {

            stmt.setDouble(1, totalCost);
            stmt.setInt(2, vehicleId);
            stmt.executeUpdate();
        }
    }

    /**
     * Gets a vehicle by its ID
     */
    private Vehicle getVehicleById(int vehicleId) throws SQLException {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM vehicles WHERE id = ?")) {

            stmt.setInt(1, vehicleId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String model = rs.getString("model");
                String type = rs.getString("type");
                double rentPerDay = rs.getDouble("rent_per_day");
                boolean isAvailable = rs.getBoolean("is_available");

                return new Vehicle(vehicleId, model, type, rentPerDay, isAvailable);
            }
        }

        return null;
    }

    /**
     * Gets the rental time for a vehicle
     */
    private LocalDateTime getRentalTime(int vehicleId) throws SQLException {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT rent_date FROM rental_records WHERE vehicle_id = ? AND return_date IS NULL")) {

            stmt.setInt(1, vehicleId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Timestamp rentDate = rs.getTimestamp("rent_date");
                return rentDate.toLocalDateTime();
            } else {
                // If no active rental found, check for the most recently returned rental
                try (PreparedStatement returnedStmt = conn.prepareStatement(
                        "SELECT rent_date FROM rental_records WHERE vehicle_id = ? ORDER BY return_date DESC LIMIT 1")) {

                    returnedStmt.setInt(1, vehicleId);
                    ResultSet returnedRs = returnedStmt.executeQuery();

                    if (returnedRs.next()) {
                        Timestamp rentDate = returnedRs.getTimestamp("rent_date");
                        return rentDate.toLocalDateTime();
                    }
                }
            }
        }

        return null;
    }

    /**
     * Logs a rental transaction to the log file
     */
    private void logRental(String username, int vehicleId, String vehicleModel, LocalDateTime rentalTime) {
        String logFileName = "rental_log_" + new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + ".log";
        Path logPath = Paths.get(LOGS_DIRECTORY, logFileName);

        try {
            Files.createDirectories(logPath.getParent());

            try (PrintWriter writer = new PrintWriter(
                    new FileWriter(logPath.toFile(), true))) {
                String logEntry = String.format("[%s] RENTAL - User: %s, Vehicle ID: %d, Model: %s",
                        formatDateTime(rentalTime), username, vehicleId, vehicleModel);
                writer.println(logEntry);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Logs a return transaction to the log file
     */
    private void logReturn(String username, int vehicleId, String vehicleModel,
                           LocalDateTime returnTime, double totalCost) {
        String logFileName = "rental_log_" + new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + ".log";
        Path logPath = Paths.get(LOGS_DIRECTORY, logFileName);

        try {
            Files.createDirectories(logPath.getParent());

            try (PrintWriter writer = new PrintWriter(
                    new FileWriter(logPath.toFile(), true))) {
                String logEntry = String.format("[%s] RETURN - User: %s, Vehicle ID: %d, Model: %s, Amount: $%.2f",
                        formatDateTime(returnTime), username, vehicleId, vehicleModel, totalCost);
                writer.println(logEntry);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Formats LocalDateTime to a standard string format
     */
    private String formatDateTime(LocalDateTime dateTime) {
        return String.format("%04d-%02d-%02d %02d:%02d:%02d",
                dateTime.getYear(), dateTime.getMonthValue(), dateTime.getDayOfMonth(),
                dateTime.getHour(), dateTime.getMinute(), dateTime.getSecond());
    }

    /**
     * Opens the receipt file for the user
     */
    private void showReceiptToUser(String filePath) {
        try {
            File file = new File(filePath);
            if (Desktop.isDesktopSupported() && file.exists()) {
                Desktop.getDesktop().open(file);
            } else {
                // If can't open file, at least show the path
                JOptionPane.showMessageDialog(null,
                        "Receipt saved to: " + filePath,
                        "Receipt Generated", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null,
                    "Receipt saved to: " + filePath + "\nCould not open file automatically.",
                    "Receipt Generated", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * Gets all rental history for reporting purposes
     * @return List of rental history entries
     */
    public List<RentalRecord> getRentalHistory() {
        List<RentalRecord> history = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT r.id, r.vehicle_id, v.model, r.username, r.rent_date, " +
                             "r.return_date, r.total_cost FROM rental_records r " +
                             "JOIN vehicles v ON r.vehicle_id = v.id " +
                             "ORDER BY r.rent_date DESC")) {

            while (rs.next()) {
                int id = rs.getInt("id");
                int vehicleId = rs.getInt("vehicle_id");
                String model = rs.getString("model");
                String username = rs.getString("username");
                LocalDateTime rentDate = rs.getTimestamp("rent_date").toLocalDateTime();

                Timestamp returnTimestamp = rs.getTimestamp("return_date");
                LocalDateTime returnDate = returnTimestamp != null ?
                        returnTimestamp.toLocalDateTime() : null;

                double totalCost = rs.getDouble("total_cost");

                RentalRecord record = new RentalRecord(id, vehicleId, model, username,
                        rentDate, returnDate, totalCost);
                history.add(record);
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null,
                    "Failed to retrieve rental history: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }

        return history;
    }

    /**
     * Class representing a rental record for reporting
     */
    public static class RentalRecord {
        private final int id;
        private final int vehicleId;
        private final String vehicleModel;
        private final String username;
        private final LocalDateTime rentDate;
        private final LocalDateTime returnDate;
        private final double totalCost;

        public RentalRecord(int id, int vehicleId, String vehicleModel,
                            String username, LocalDateTime rentDate,
                            LocalDateTime returnDate, double totalCost) {
            this.id = id;
            this.vehicleId = vehicleId;
            this.vehicleModel = vehicleModel;
            this.username = username;
            this.rentDate = rentDate;
            this.returnDate = returnDate;
            this.totalCost = totalCost;
        }

        public int getId() { return id; }
        public int getVehicleId() { return vehicleId; }
        public String getVehicleModel() { return vehicleModel; }
        public String getUsername() { return username; }
        public LocalDateTime getRentDate() { return rentDate; }
        public LocalDateTime getReturnDate() { return returnDate; }
        public double getTotalCost() { return totalCost; }

        public boolean isActive() { return returnDate == null; }

        public long getDurationDays() {
            if (returnDate == null) {
                long hours = ChronoUnit.HOURS.between(rentDate, LocalDateTime.now());
                return (long) Math.ceil(hours / 24.0);
            } else {
                long hours = ChronoUnit.HOURS.between(rentDate, returnDate);
                return (long) Math.ceil(hours / 24.0);
            }
        }
    }
}