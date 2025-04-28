import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.util.List;
import java.util.Objects;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;

/**
 * Utility class for formatting vehicle data as tables (plain text, HTML, and JTable)
 */
public final class VehicleFormatter {

    // Private constructor to prevent instantiation of utility class
    private VehicleFormatter() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    // Column widths for consistent formatting in text tables
    private static final int ID_WIDTH = 5;
    private static final int MODEL_WIDTH = 20;
    private static final int TYPE_WIDTH = 10;
    private static final int RENT_WIDTH = 15;
    private static final int QUANTITY_WIDTH = 10;
    private static final int STATUS_WIDTH = 15;

    // Column names for JTable
    private static final String[] COLUMN_NAMES = {"ID", "Model", "Type", "Rent ($/day)", "Quantity", "Status"};

    // Common colors for consistent styling
    private static final Color HEADER_BG_COLOR = new Color(52, 152, 219);
    private static final Color ALTERNATE_ROW_COLOR = new Color(242, 242, 242);
    private static final Color GRID_COLOR = new Color(189, 195, 199);
    private static final Color SELECTION_COLOR = new Color(133, 193, 233);
    private static final Color STATUS_RENTED_COLOR = new Color(231, 76, 60);
    private static final Color STATUS_AVAILABLE_COLOR = new Color(39, 174, 96);
    private static final Color STATUS_PARTIAL_COLOR = new Color(230, 126, 34);

    /**
     * Creates a JTable to display vehicles with custom styling
     * @param vehicles List of vehicles to display
     * @return JScrollPane containing the styled JTable
     * @throws NullPointerException if vehicles is null
     */
    public static JScrollPane createVehicleJTable(List<Vehicle> vehicles) {
        Objects.requireNonNull(vehicles, "Vehicles list cannot be null");

        // Create a table model with non-editable cells
        DefaultTableModel model = new NonEditableTableModel();

        // Add columns
        for (String columnName : COLUMN_NAMES) {
            model.addColumn(columnName);
        }

        // Add vehicle data
        for (Vehicle vehicle : vehicles) {
            model.addRow(createVehicleRow(vehicle));
        }

        // Create the table with the model
        JTable table = new JTable(model);

        // Style the table
        styleTable(table);

        // Set preferred column widths
        configureColumnWidths(table.getColumnModel());

        // Add sorting capability
        table.setAutoCreateRowSorter(true);

        // Add the table to a scroll pane
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        return scrollPane;
    }

    /**
     * Creates a row of data for a vehicle
     */
    private static Object[] createVehicleRow(Vehicle vehicle) {
        return new Object[]{
                vehicle.getId(),
                vehicle.getModel(),
                vehicle.getType(),
                String.format("$%.2f", vehicle.getRentPerDay()),
                vehicle.getQuantity() + " (" + vehicle.getAvailableQuantity() + " avail.)",
                vehicle.getAvailabilityStatus()
        };
    }

    /**
     * Configures column widths for the JTable
     */
    private static void configureColumnWidths(TableColumnModel columnModel) {
        columnModel.getColumn(0).setPreferredWidth(40);   // ID
        columnModel.getColumn(1).setPreferredWidth(180);  // Model
        columnModel.getColumn(2).setPreferredWidth(80);   // Type
        columnModel.getColumn(3).setPreferredWidth(80);   // Rent
        columnModel.getColumn(4).setPreferredWidth(80);   // Quantity
        columnModel.getColumn(5).setPreferredWidth(100);  // Status

        // Add custom renderer for the Status column
        columnModel.getColumn(5).setCellRenderer(new StatusColumnRenderer());
    }

    /**
     * Apply styling to the JTable
     */
    private static void styleTable(JTable table) {
        // Set row height and font
        table.setRowHeight(25);
        Font baseFont = new Font("SansSerif", Font.PLAIN, 14);
        table.setFont(baseFont);

        // Style header
        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("SansSerif", Font.BOLD, 14));
        header.setBackground(HEADER_BG_COLOR);
        header.setForeground(Color.WHITE);

        // Set selection colors
        table.setSelectionBackground(SELECTION_COLOR);
        table.setSelectionForeground(Color.BLACK);

        // Set grid lines
        table.setShowGrid(true);
        table.setGridColor(GRID_COLOR);

        // Set alternating row colors
        table.setDefaultRenderer(Object.class, new AlternatingRowRenderer());
    }

    /**
     * Non-editable table model to prevent cells from being edited
     */
    private static class NonEditableTableModel extends DefaultTableModel {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;  // Make all cells non-editable
        }
    }

    /**
     * Custom renderer for alternating row colors
     */
    static class AlternatingRowRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);

            if (!isSelected) {
                c.setBackground(row % 2 == 0 ? Color.WHITE : ALTERNATE_ROW_COLOR);
            }

            return c;
        }
    }

    /**
     * Custom renderer for status column with color indicators
     */
    static class StatusColumnRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);

            if (!isSelected && value != null) {
                String strValue = value.toString();
                if (strValue.contains("Rented")) {
                    c.setForeground(STATUS_RENTED_COLOR);
                } else if (strValue.contains("Available")) {
                    c.setForeground(STATUS_AVAILABLE_COLOR);
                } else {
                    // Partial availability
                    c.setForeground(STATUS_PARTIAL_COLOR);
                }
            }

            return c;
        }
    }

    /**
     * Formats a list of vehicles as a nicely formatted text table
     * @param vehicles List of vehicles to format
     * @param title Title for the table
     * @return Formatted text table as string
     * @throws NullPointerException if vehicles is null
     */
    public static String formatVehicleTable(List<Vehicle> vehicles, String title) {
        Objects.requireNonNull(vehicles, "Vehicles list cannot be null");
        Objects.requireNonNull(title, "Title cannot be null");

        if (vehicles.isEmpty()) {
            return title + "\n\nNo vehicles found.";
        }

        StringBuilder table = new StringBuilder(vehicles.size() * 100); // Pre-allocate capacity

        // Add the title
        table.append(title).append("\n\n");

        // Add the header row
        table.append(formatHeader());

        // Add separator line
        table.append(formatSeparator());

        // Add each vehicle row
        for (Vehicle vehicle : vehicles) {
            table.append(formatVehicle(vehicle));
        }

        return table.toString();
    }

    /**
     * Formats a list of vehicles as a colorful HTML table
     * @param vehicles List of vehicles to format
     * @param title Title for the table
     * @return Formatted HTML table as string
     * @throws NullPointerException if vehicles is null
     */
    public static String formatHTMLVehicleTable(List<Vehicle> vehicles, String title) {
        Objects.requireNonNull(vehicles, "Vehicles list cannot be null");
        Objects.requireNonNull(title, "Title cannot be null");

        if (vehicles.isEmpty()) {
            return "<html><body><h2>" + escapeHtml(title) + "</h2><p>No vehicles found.</p></body></html>";
        }

        StringBuilder html = new StringBuilder(vehicles.size() * 500); // Pre-allocate capacity
        html.append("<html><body>");

        // Add the title with styling
        html.append("<h2 style='color: #2c3e50;'>").append(escapeHtml(title)).append("</h2>");

        // Start the table with styling
        html.append("<table style='width: 100%; border-collapse: collapse; margin-top: 10px;'>");

        // Add the header row
        appendHtmlTableHeader(html);

        // Add each vehicle row with alternating row colors
        boolean alternate = false;
        for (Vehicle vehicle : vehicles) {
            appendHtmlVehicleRow(html, vehicle, alternate);
            alternate = !alternate; // Toggle for next row
        }

        html.append("</table></body></html>");
        return html.toString();
    }

    /**
     * Appends HTML table header to the StringBuilder
     */
    private static void appendHtmlTableHeader(StringBuilder html) {
        html.append("<tr style='background-color: #3498db; color: white;'>");
        html.append("<th style='padding: 8px; text-align: left; border: 1px solid #ddd;'>ID</th>");
        html.append("<th style='padding: 8px; text-align: left; border: 1px solid #ddd;'>Model</th>");
        html.append("<th style='padding: 8px; text-align: left; border: 1px solid #ddd;'>Type</th>");
        html.append("<th style='padding: 8px; text-align: left; border: 1px solid #ddd;'>Rent ($/day)</th>");
        html.append("<th style='padding: 8px; text-align: left; border: 1px solid #ddd;'>Quantity</th>");
        html.append("<th style='padding: 8px; text-align: left; border: 1px solid #ddd;'>Status</th>");
        html.append("</tr>");
    }

    /**
     * Appends an HTML table row for a vehicle to the StringBuilder
     */
    private static void appendHtmlVehicleRow(StringBuilder html, Vehicle vehicle, boolean alternate) {
        String rowColor = alternate ? "#f2f2f2" : "white";
        html.append("<tr style='background-color: ").append(rowColor).append(";'>");

        // Common cell style
        String cellStyle = "padding: 8px; text-align: left; border: 1px solid #ddd;";

        // Add cells
        html.append("<td style='").append(cellStyle).append("'>").append(vehicle.getId()).append("</td>");
        html.append("<td style='").append(cellStyle).append("'>").append(escapeHtml(vehicle.getModel())).append("</td>");
        html.append("<td style='").append(cellStyle).append("'>").append(escapeHtml(vehicle.getType())).append("</td>");
        html.append("<td style='").append(cellStyle).append("'>$").append(String.format("%.2f", vehicle.getRentPerDay())).append("</td>");
        html.append("<td style='").append(cellStyle).append("'>").append(vehicle.getQuantity())
                .append(" (").append(vehicle.getAvailableQuantity()).append(" avail.)</td>");

        // Format status with color
        String status = vehicle.getAvailabilityStatus();
        String statusColor = getStatusColor(status);
        html.append("<td style='").append(cellStyle).append("color: ").append(statusColor).append(";'>")
                .append(escapeHtml(status)).append("</td>");

        html.append("</tr>");
    }

    /**
     * Determines the appropriate color for status text
     */
    private static String getStatusColor(String status) {
        if (status.contains("Rented")) {
            return "red";
        } else if (status.startsWith("Available")) {
            return "green";
        } else {
            return "orange"; // Partial availability
        }
    }

    /**
     * Escapes HTML special characters to prevent XSS
     */
    private static String escapeHtml(String content) {
        return content
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    /**
     * Formats a message as HTML content
     * @param message Message to format
     * @return Formatted HTML message
     */
    public static String formatHTMLMessage(String message) {
        Objects.requireNonNull(message, "Message cannot be null");
        return "<html><body><p style='color: #2c3e50; font-size: 14px;'>" + escapeHtml(message) + "</p></body></html>";
    }

    /**
     * Formats the table header
     */
    private static String formatHeader() {
        return String.format("| %-" + ID_WIDTH + "s | %-" + MODEL_WIDTH + "s | %-" + TYPE_WIDTH + "s | %-" +
                        RENT_WIDTH + "s | %-" + QUANTITY_WIDTH + "s | %-" + STATUS_WIDTH + "s |\n",
                "ID", "Model", "Type", "Rent ($/day)", "Quantity", "Status");
    }

    /**
     * Creates a separator line for the table
     */
    private static String formatSeparator() {
        return "+-" + "-".repeat(ID_WIDTH) + "-+-" +
                "-".repeat(MODEL_WIDTH) + "-+-" +
                "-".repeat(TYPE_WIDTH) + "-+-" +
                "-".repeat(RENT_WIDTH) + "-+-" +
                "-".repeat(QUANTITY_WIDTH) + "-+-" +
                "-".repeat(STATUS_WIDTH) + "-+\n";
    }

    /**
     * Formats a single vehicle as a table row
     */
    private static String formatVehicle(Vehicle vehicle) {
        return String.format("| %-" + ID_WIDTH + "d | %-" + MODEL_WIDTH + "s | %-" + TYPE_WIDTH + "s | $%-" +
                        (RENT_WIDTH-1) + ".2f | %-" + QUANTITY_WIDTH + "d | %-" + STATUS_WIDTH + "s |\n",
                vehicle.getId(),
                truncateString(vehicle.getModel(), MODEL_WIDTH),
                vehicle.getType(),
                vehicle.getRentPerDay(),
                vehicle.getQuantity(),
                truncateString(vehicle.getAvailabilityStatus(), STATUS_WIDTH));
    }

    /**
     * Truncates string if it's longer than the specified width
     */
    private static String truncateString(String str, int width) {
        if (str == null) {
            return "";
        }

        if (str.length() <= width) {
            return str;
        }
        return str.substring(0, width - 3) + "...";
    }
}