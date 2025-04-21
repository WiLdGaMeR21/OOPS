import java.util.List;

/**
 * Utility class for formatting vehicle data as HTML tables with colors
 */
public class HTMLVehicleFormatter {

    /**
     * Formats a list of vehicles as a colorful HTML table
     */
    public static String formatVehicleTable(List<Vehicle> vehicles, String title) {
        if (vehicles == null || vehicles.isEmpty()) {
            return "<html><body><h2>" + title + "</h2><p>No vehicles found.</p></body></html>";
        }

        StringBuilder html = new StringBuilder();
        html.append("<html><body>");

        // Add the title with blue color
        html.append("<h2 style='color: #2c3e50;'>" + title + "</h2>");

        // Start the table with styling
        html.append("<table style='width: 100%; border-collapse: collapse; margin-top: 10px;'>");

        // Add the header row
        html.append("<tr style='background-color: #3498db; color: white;'>");
        html.append("<th style='padding: 8px; text-align: left; border: 1px solid #ddd;'>ID</th>");
        html.append("<th style='padding: 8px; text-align: left; border: 1px solid #ddd;'>Model</th>");
        html.append("<th style='padding: 8px; text-align: left; border: 1px solid #ddd;'>Type</th>");
        html.append("<th style='padding: 8px; text-align: left; border: 1px solid #ddd;'>Rent ($/day)</th>");
        html.append("<th style='padding: 8px; text-align: left; border: 1px solid #ddd;'>Status</th>");
        html.append("</tr>");

        // Add each vehicle row with alternating row colors
        boolean alternate = false;
        for (Vehicle vehicle : vehicles) {
            String rowColor = alternate ? "#f2f2f2" : "white";
            html.append("<tr style='background-color: " + rowColor + ";'>");

            html.append("<td style='padding: 8px; text-align: left; border: 1px solid #ddd;'>" + vehicle.getId() + "</td>");
            html.append("<td style='padding: 8px; text-align: left; border: 1px solid #ddd;'>" + vehicle.getModel() + "</td>");
            html.append("<td style='padding: 8px; text-align: left; border: 1px solid #ddd;'>" + vehicle.getType() + "</td>");
            html.append("<td style='padding: 8px; text-align: left; border: 1px solid #ddd;'>$" + String.format("%.2f", vehicle.getRentPerDay()) + "</td>");

            // Color-code the status: green for available, red for rented
            String statusColor = vehicle.isAvailable() ? "green" : "red";
            String status = vehicle.isAvailable() ? "Available" : "Rented";
            html.append("<td style='padding: 8px; text-align: left; border: 1px solid #ddd; color: " + statusColor + ";'>" + status + "</td>");

            html.append("</tr>");
            alternate = !alternate; // Toggle for next row
        }

        html.append("</table></body></html>");
        return html.toString();
    }

    /**
     * Formats a message as HTML content
     */
    public static String formatMessage(String message) {
        return "<html><body><p style='color: #2c3e50; font-size: 14px;'>" + message + "</p></body></html>";
    }
}