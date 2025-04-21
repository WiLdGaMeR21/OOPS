import java.util.List;

/**
 * Utility class for formatting vehicle data as tables
 */
public class VehicleFormatter {

    // Column widths for consistent formatting
    private static final int ID_WIDTH = 5;
    private static final int MODEL_WIDTH = 20;
    private static final int TYPE_WIDTH = 10;
    private static final int RENT_WIDTH = 15;
    private static final int STATUS_WIDTH = 10;

    /**
     * Formats a list of vehicles as a nicely formatted table
     */
    public static String formatVehicleTable(List<Vehicle> vehicles, String title) {
        if (vehicles == null || vehicles.isEmpty()) {
            return title + "\nNo vehicles found.";
        }

        StringBuilder tableBuilder = new StringBuilder();
        tableBuilder.append(title).append("\n\n");

        // Add header
        tableBuilder.append(formatSeparator());
        tableBuilder.append(formatHeader());
        tableBuilder.append(formatSeparator());

        // Add rows
        for (Vehicle vehicle : vehicles) {
            tableBuilder.append(formatVehicleRow(vehicle));
        }

        // Add footer
        tableBuilder.append(formatSeparator());

        return tableBuilder.toString();
    }

    /**
     * Formats the table header
     */
    private static String formatHeader() {
        return String.format("| %-" + ID_WIDTH + "s | %-" + MODEL_WIDTH + "s | %-" + TYPE_WIDTH + "s | %-" +
                        RENT_WIDTH + "s | %-" + STATUS_WIDTH + "s |\n",
                "ID", "Model", "Type", "Rent ($/day)", "Status");
    }

    /**
     * Creates a separator line for the table
     */
    private static String formatSeparator() {
        StringBuilder separator = new StringBuilder("+-");

        // ID column
        for (int i = 0; i < ID_WIDTH; i++) {
            separator.append("-");
        }
        separator.append("-+-");

        // Model column
        for (int i = 0; i < MODEL_WIDTH; i++) {
            separator.append("-");
        }
        separator.append("-+-");

        // Type column
        for (int i = 0; i < TYPE_WIDTH; i++) {
            separator.append("-");
        }
        separator.append("-+-");

        // Rent column
        for (int i = 0; i < RENT_WIDTH; i++) {
            separator.append("-");
        }
        separator.append("-+-");

        // Status column
        for (int i = 0; i < STATUS_WIDTH; i++) {
            separator.append("-");
        }
        separator.append("-+\n");

        return separator.toString();
    }

    /**
     * Formats a single vehicle as a table row
     */
    private static String formatVehicleRow(Vehicle vehicle) {
        String model = truncateString(vehicle.getModel(), MODEL_WIDTH);
        String type = truncateString(vehicle.getType(), TYPE_WIDTH);
        String rentPrice = String.format("$%.2f", vehicle.getRentPerDay());
        String status = vehicle.isAvailable() ? "Available" : "Rented";

        return String.format("| %-" + ID_WIDTH + "d | %-" + MODEL_WIDTH + "s | %-" + TYPE_WIDTH + "s | %-" +
                        RENT_WIDTH + "s | %-" + STATUS_WIDTH + "s |\n",
                vehicle.getId(), model, type, rentPrice, status);
    }

    /**
     * Truncates string if it's longer than the specified width
     */
    private static String truncateString(String str, int width) {
        if (str.length() <= width) {
            return str;
        }
        return str.substring(0, width - 3) + "...";
    }
}