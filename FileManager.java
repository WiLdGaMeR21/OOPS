import javax.swing.JOptionPane;
import java.awt.Desktop;
import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * FileManager handles all file operations including logs and receipts for the rental system
 */
public class FileManager {
    private static final String RECEIPTS_DIRECTORY = "receipts";
    private static final String LOGS_DIRECTORY = "logs";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final SimpleDateFormat LOG_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    // Thread pool for asynchronous logging operations
    private final ExecutorService logExecutor = Executors.newSingleThreadExecutor();

    // Singleton pattern
    private static FileManager instance;

    private FileManager() {
        initializeDirectories();
    }

    /**
     * Get the singleton instance of FileManager
     */
    public static synchronized FileManager getInstance() {
        if (instance == null) {
            instance = new FileManager();
        }
        return instance;
    }

    /**
     * Creates the necessary directories for logs and receipts
     */
    private void initializeDirectories() {
        try {
            Files.createDirectories(Paths.get(RECEIPTS_DIRECTORY));
            Files.createDirectories(Paths.get(LOGS_DIRECTORY));
        } catch (IOException e) {
            handleException("Failed to create required directories", e);
        }
    }

    /**
     * Creates a receipt and logs when a vehicle is rented
     */
    public boolean processRental(String username, int vehicleId, RentalManager rentalManager) {
        Optional<Vehicle> vehicleOpt = rentalManager.getVehicleById(vehicleId);
        if (vehicleOpt.isEmpty()) {
            return false;
        }

        Vehicle vehicle = vehicleOpt.get();
        LocalDateTime rentalTime = LocalDateTime.now();

        // Create rental receipt
        boolean receiptCreated = createRentalReceipt(username, vehicle, rentalTime);

        // Log the rental asynchronously
        logAsync(() -> String.format("[%s] RENTAL - User: %s, Vehicle ID: %d, Model: %s",
                formatDateTime(rentalTime), username, vehicleId, vehicle.getModel()));

        return receiptCreated;
    }

    /**
     * Creates a receipt and logs when a vehicle is returned
     */
    public boolean processReturn(String username, int vehicleId, RentalManager rentalManager) {
        Optional<Vehicle> vehicleOpt = rentalManager.getVehicleById(vehicleId);
        if (vehicleOpt.isEmpty()) {
            return false;
        }

        Vehicle vehicle = vehicleOpt.get();
        LocalDateTime rentalTime = rentalManager.getRentalTime(vehicleId);

        if (rentalTime == null) {
            JOptionPane.showMessageDialog(null,
                    "No active rental found for this vehicle",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        LocalDateTime returnTime = LocalDateTime.now();
        RentalCalculation calculation = calculateRental(rentalTime, returnTime, vehicle.getRentPerDay());

        // Create return receipt
        boolean receiptCreated = createReturnReceipt(username, vehicle, rentalTime, returnTime,
                calculation.days, calculation.hours, calculation.totalCost);

        // Log the return asynchronously
        logAsync(() -> String.format("[%s] RETURN - User: %s, Vehicle ID: %d, Model: %s, Amount: $%.2f",
                formatDateTime(returnTime), username, vehicleId, vehicle.getModel(), calculation.totalCost));

        return receiptCreated;
    }

    /**
     * Calculate rental duration and cost
     */
    private RentalCalculation calculateRental(LocalDateTime rentalTime, LocalDateTime returnTime, double dailyRate) {
        RentalCalculation calc = new RentalCalculation();
        calc.hours = ChronoUnit.HOURS.between(rentalTime, returnTime);
        calc.days = (int) Math.ceil(calc.hours / 24.0); // Round up to the nearest day
        if (calc.days == 0) calc.days = 1; // Minimum 1 day charge
        calc.totalCost = calc.days * dailyRate;
        return calc;
    }

    /**
     * Creates a receipt when a vehicle is rented
     */
    private boolean createRentalReceipt(String username, Vehicle vehicle, LocalDateTime rentalTime) {
        String formattedDateTime = formatDateTime(rentalTime);
        String filenameSafeDateTime = formattedDateTime.replace(":", "-").replace(" ", "_");
        String receiptFileName = String.format("%s_rental_%d_%s.txt",
                username, vehicle.getId(), filenameSafeDateTime);

        Path receiptPath = Paths.get(RECEIPTS_DIRECTORY, receiptFileName);

        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(receiptPath))) {
            writer.println("=========================================");
            writer.println("          VEHICLE RENTAL RECEIPT         ");
            writer.println("=========================================");
            writer.println("Receipt ID: " + generateReceiptId());
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

            showReceiptToUser(receiptPath.toString());
            return true;
        } catch (IOException e) {
            handleException("Failed to create rental receipt", e);
            return false;
        }
    }

    /**
     * Creates a return receipt with detailed cost information
     */
    private boolean createReturnReceipt(String username, Vehicle vehicle, LocalDateTime rentalTime,
                                        LocalDateTime returnTime, int days, long hours, double totalCost) {
        String formattedDateTime = formatDateTime(returnTime);
        String filenameSafeDateTime = formattedDateTime.replace(":", "-").replace(" ", "_");
        String receiptFileName = String.format("%s_return_%d_%s.txt",
                username, vehicle.getId(), filenameSafeDateTime);

        Path receiptPath = Paths.get(RECEIPTS_DIRECTORY, receiptFileName);

        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(receiptPath))) {
            writer.println("=========================================");
            writer.println("         VEHICLE RETURN RECEIPT          ");
            writer.println("=========================================");
            writer.println("Receipt ID: " + generateReceiptId());
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
            writer.println("  Duration: " + days + " day(s) (" + hours + " hours)");
            writer.println("-----------------------------------------");
            writer.println("Financial Summary:");
            writer.println("  Daily Rate: $" + String.format("%.2f", vehicle.getRentPerDay()));
            writer.println("  Total Cost: $" + String.format("%.2f", totalCost));
            writer.println("-----------------------------------------");
            writer.println("Thank you for returning the vehicle!");
            writer.println("=========================================");

            showReceiptToUser(receiptPath.toString());
            return true;
        } catch (IOException e) {
            handleException("Failed to create return receipt", e);
            return false;
        }
    }

    /**
     * Log an entry asynchronously
     */
    private void logAsync(Supplier<String> logEntrySupplier) {
        logExecutor.submit(() -> writeToLog(logEntrySupplier.get()));
    }

    /**
     * Writes a log entry to the current day's log file
     */
    private void writeToLog(String logEntry) {
        String logFileName = "rental_log_" + LOG_DATE_FORMAT.format(new Date()) + ".log";
        Path logPath = Paths.get(LOGS_DIRECTORY, logFileName);

        try {
            Files.createDirectories(logPath.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(
                    logPath, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                writer.write(logEntry);
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace(); // Don't show dialog for background logging errors
        }
    }

    /**
     * Display the receipt to the user
     */
    private void showReceiptToUser(String receiptPath) {
        try {
            File receiptFile = new File(receiptPath);
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(receiptFile);
            } else {
                JOptionPane.showMessageDialog(null,
                        "Receipt saved at: " + receiptPath,
                        "Receipt Created", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null,
                    "Receipt saved, but couldn't open automatically: " + e.getMessage(),
                    "Warning", JOptionPane.WARNING_MESSAGE);
        }
    }

    /**
     * Generate a unique receipt ID
     */
    private String generateReceiptId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Format datetime to a readable string
     */
    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime.format(DATE_TIME_FORMATTER);
    }

    /**
     * Gets the path to a specific receipt file
     */
    public String getReceiptPath(String fileName) {
        return Paths.get(RECEIPTS_DIRECTORY, fileName).toString();
    }

    /**
     * Lists all receipts for a specific user
     */
    public String[] getUserReceipts(String username) {
        try {
            Path directory = Paths.get(RECEIPTS_DIRECTORY);
            return Files.list(directory)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.startsWith(username + "_"))
                    .toArray(String[]::new);
        } catch (Exception e) {
            handleException("Failed to list user receipts", e);
            return new String[0];
        }
    }

    /**
     * Gets all log entries for a specific date
     */
    public String[] getLogEntriesForDate(String date) {
        String logFileName = "rental_log_" + date + ".log";
        Path logPath = Paths.get(LOGS_DIRECTORY, logFileName);

        try {
            if (Files.exists(logPath)) {
                return Files.lines(logPath).toArray(String[]::new);
            }
        } catch (IOException e) {
            handleException("Failed to read log file", e);
        }
        return new String[0];
    }

    /**
     * Handle exceptions in a consistent way
     */
    private void handleException(String message, Exception e) {
        JOptionPane.showMessageDialog(null,
                message + ": " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        e.printStackTrace();
    }

    /**
     * Clean up resources when application closes
     */
    public void shutdown() {
        logExecutor.shutdown();
    }

    /**
     * Internal class to hold rental calculation results
     */
    private static class RentalCalculation {
        int days;
        long hours;
        double totalCost;
    }
}