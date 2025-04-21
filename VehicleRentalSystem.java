import javax.swing.*;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import java.awt.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Main application class for the Vehicle Rental System
 */
public class VehicleRentalSystem {
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final UserSession userSession = UserSession.getInstance();
    private final DatabaseManager dbManager = new DatabaseManager();
    private final ReceiptManager receiptManager = new ReceiptManager(dbManager);
    private final RentalManager rentalManager = new RentalManager();

    // UI Components
    private JFrame mainFrame;
    private JEditorPane displayArea; // Changed from JTextArea to JEditorPane
    private JTextField vehicleIdField;
    private JButton adminButton;
    private JButton logoutButton;
    private JLabel statusLabel;

    public VehicleRentalSystem() {
        // Create main application frame
        mainFrame = new JFrame("Vehicle Rental System");
        mainFrame.setSize(800, 600); // Increased size for better table display
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Show login screen first
        showLoginScreen();
    }

    private void showLoginScreen() {
        mainFrame.getContentPane().removeAll();

        // Create login panel
        LoginPanel loginPanel = new LoginPanel();
        loginPanel.setLoginListener((username, role) -> {
            // Handle successful login
            userSession.login(username, role);
            showMainApplication();
        });

        mainFrame.getContentPane().add(loginPanel);
        mainFrame.setLocationRelativeTo(null);
        mainFrame.setVisible(true);
    }

    private void showMainApplication() {
        mainFrame.getContentPane().removeAll();
        mainFrame.setLayout(new BorderLayout(10, 10));

        // Setup display area with HTML support
        displayArea = new JEditorPane();
        displayArea.setEditable(false);
        displayArea.setContentType("text/html");

        // Add a custom style sheet for better appearance
        HTMLEditorKit kit = new HTMLEditorKit();
        StyleSheet styleSheet = kit.getStyleSheet();
        styleSheet.addRule("body {font-family: SansSerif; font-size: 12pt; margin: 10px;}");
        styleSheet.addRule("h2 {margin-top: 5px; margin-bottom: 10px;}");
        styleSheet.addRule("table {width: 100%; border-collapse: collapse;}");
        styleSheet.addRule("th, td {padding: 8px; border: 1px solid #ddd;}");
        styleSheet.addRule("th {background-color: #3498db; color: white;}");
        displayArea.setEditorKit(kit);

        JScrollPane scrollPane = new JScrollPane(displayArea);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        mainFrame.add(scrollPane, BorderLayout.CENTER);

        // Setup top panel with input controls
        JPanel topPanel = new JPanel(new BorderLayout());

        // Status bar showing logged-in user
        JPanel statusPanel = createStatusPanel();
        topPanel.add(statusPanel, BorderLayout.NORTH);

        // Input controls
        JPanel inputPanel = createInputPanel();
        topPanel.add(inputPanel, BorderLayout.CENTER);

        mainFrame.add(topPanel, BorderLayout.NORTH);

        // Setup bottom panel with additional buttons
        JPanel bottomPanel = createBottomPanel();
        mainFrame.add(bottomPanel, BorderLayout.SOUTH);

        // Show initial vehicle list
        updateVehicleDisplay();

        // Refresh the frame
        mainFrame.revalidate();
        mainFrame.repaint();
    }

    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        // Status label showing logged in user and role
        statusLabel = new JLabel();
        updateStatusLabel();
        panel.add(statusLabel, BorderLayout.WEST);

        // Logout button
        logoutButton = new JButton("Logout");
        logoutButton.addActionListener(e -> {
            userSession.logout();
            showLoginScreen();
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(logoutButton);
        panel.add(buttonPanel, BorderLayout.EAST);

        return panel;
    }

    private void updateStatusLabel() {
        String role = userSession.isAdmin() ? "Administrator" : "Regular User";
        statusLabel.setText("Logged in as: " + userSession.getUsername() + " (" + role + ")");
    }

    private JPanel createInputPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();

        // Vehicle ID label and field
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 0, 5, 5);
        panel.add(new JLabel("Vehicle ID:"), gbc);

        vehicleIdField = new JTextField(10);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(vehicleIdField, gbc);

        // Rent button
        JButton rentButton = new JButton("Rent Vehicle");
        rentButton.addActionListener(e -> executeTask(this::rentVehicle));
        gbc.gridx = 2;
        gbc.insets = new Insets(0, 5, 5, 0);
        panel.add(rentButton, gbc);

        // Return button
        JButton returnButton = new JButton("Return Vehicle");
        returnButton.addActionListener(e -> executeTask(this::returnVehicle));
        gbc.gridx = 3;
        panel.add(returnButton, gbc);

        return panel;
    }

    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));

        JButton viewAllButton = new JButton("View All Vehicles");
        viewAllButton.addActionListener(e -> executeTask(this::viewAllVehicles));

        JButton viewAvailableButton = new JButton("View Available Vehicles");
        viewAvailableButton.addActionListener(e -> executeTask(this::viewAvailableVehicles));

        JButton rentalHistoryButton = new JButton("View Rental History");
        rentalHistoryButton.addActionListener(e -> executeTask(this::viewRentalHistory));

        adminButton = new JButton("Admin Panel");
        adminButton.addActionListener(e -> executeTask(this::openAdminPanel));
        // Only show admin button to admin users
        adminButton.setVisible(userSession.isAdmin());

        panel.add(viewAllButton);
        panel.add(viewAvailableButton);
        panel.add(rentalHistoryButton);
        panel.add(adminButton);

        return panel;
    }

    private void executeTask(Runnable task) {
        executorService.submit(() -> {
            try {
                task.run();
            } catch (NumberFormatException ex) {
                displayMessage("Please enter a valid numeric ID");
            } catch (Exception ex) {
                displayMessage("Error: " + ex.getMessage());
                ex.printStackTrace();
            }
        });
    }

    private void rentVehicle() {
        try {
            int vehicleId = Integer.parseInt(vehicleIdField.getText().trim());

            // Updated to match RentalManager implementation
            boolean success = rentalManager.rentVehicle(vehicleId);

            if (success) {
                // Generate rental receipt
                receiptManager.createRentalReceipt(userSession.getUsername(), vehicleId);
                displayMessage("Vehicle ID " + vehicleId + " rented successfully!");
                updateVehicleDisplay();
            } else {
                displayMessage("Vehicle not available or invalid ID.");
            }
        } catch (NumberFormatException e) {
            throw new NumberFormatException("Please enter a valid Vehicle ID");
        }
    }

    private void returnVehicle() {
        try {
            int vehicleId = Integer.parseInt(vehicleIdField.getText().trim());

            // Updated to match RentalManager implementation
            boolean success = rentalManager.returnVehicle(vehicleId);

            if (success) {
                // Generate return receipt
                receiptManager.createReturnReceipt(userSession.getUsername(), vehicleId);
                displayMessage("Vehicle ID " + vehicleId + " returned successfully!");
                updateVehicleDisplay();
            } else {
                displayMessage("Invalid Vehicle ID or already returned.");
            }
        } catch (NumberFormatException e) {
            throw new NumberFormatException("Please enter a valid Vehicle ID");
        }
    }

    private void viewAllVehicles() {
        List<Vehicle> allVehicles = rentalManager.getAllVehicles();
        displayVehicles("ALL VEHICLES", allVehicles);
    }

    private void viewAvailableVehicles() {
        List<Vehicle> availableVehicles = rentalManager.getAvailableVehicles();
        displayVehicles("AVAILABLE VEHICLES", availableVehicles);
    }

    private void viewRentalHistory() {
        List<ReceiptManager.RentalRecord> history = receiptManager.getRentalHistory();
        displayRentalHistory("RENTAL HISTORY", history);
    }

    private void displayRentalHistory(String header, List<ReceiptManager.RentalRecord> records) {
        // Format rental history as HTML
        StringBuilder html = new StringBuilder();
        html.append("<html><body>");
        html.append("<h2>").append(header).append("</h2>");

        if (records.isEmpty()) {
            html.append("<p>No rental records found.</p>");
        } else {
            html.append("<table>");

            // Header row
            html.append("<tr>");
            html.append("<th>ID</th>");
            html.append("<th>Vehicle</th>");
            html.append("<th>Customer</th>");
            html.append("<th>Rent Date</th>");
            html.append("<th>Return Date</th>");
            html.append("<th>Duration</th>");
            html.append("<th>Cost</th>");
            html.append("<th>Status</th>");
            html.append("</tr>");

            // Data rows
            for (ReceiptManager.RentalRecord record : records) {
                html.append("<tr>");
                html.append("<td>").append(record.getId()).append("</td>");
                html.append("<td>").append(record.getVehicleId()).append(" - ").append(record.getVehicleModel()).append("</td>");
                html.append("<td>").append(record.getUsername()).append("</td>");
                html.append("<td>").append(record.getRentDate().toString().replace("T", " ")).append("</td>");

                if (record.getReturnDate() != null) {
                    html.append("<td>").append(record.getReturnDate().toString().replace("T", " ")).append("</td>");
                } else {
                    html.append("<td>-</td>");
                }

                html.append("<td>").append(record.getDurationDays()).append(" day(s)</td>");

                if (record.isActive()) {
                    html.append("<td>Pending</td>");
                    html.append("<td style='color:#e67e22'>Active</td>");
                } else {
                    html.append("<td>$").append(String.format("%.2f", record.getTotalCost())).append("</td>");
                    html.append("<td style='color:#27ae60'>Returned</td>");
                }

                html.append("</tr>");
            }

            html.append("</table>");
        }

        html.append("</body></html>");
        displayArea.setText(html.toString());
    }

    private void displayVehicles(String header, List<Vehicle> vehicles) {
        // Format vehicles as HTML
        StringBuilder html = new StringBuilder();
        html.append("<html><body>");
        html.append("<h2>").append(header).append("</h2>");

        if (vehicles.isEmpty()) {
            html.append("<p>No vehicles found.</p>");
        } else {
            html.append("<table>");

            // Header row
            html.append("<tr>");
            html.append("<th>ID</th>");
            html.append("<th>Model</th>");
            html.append("<th>Type</th>");
            html.append("<th>Rent ($/day)</th>");
            html.append("<th>Status</th>");
            html.append("</tr>");

            // Data rows
            for (Vehicle vehicle : vehicles) {
                html.append("<tr>");
                html.append("<td>").append(vehicle.getId()).append("</td>");
                html.append("<td>").append(vehicle.getModel()).append("</td>");
                html.append("<td>").append(vehicle.getType()).append("</td>");
                html.append("<td>$").append(String.format("%.2f", vehicle.getRentPerDay())).append("</td>");

                // Status with color
                String statusText = vehicle.isAvailable() ? "Available" : "Rented";
                String statusColor = vehicle.isAvailable() ? "#27ae60" : "#e74c3c";
                html.append("<td style='color:").append(statusColor).append("'>")
                        .append(statusText).append("</td>");

                html.append("</tr>");
            }

            html.append("</table>");
        }

        html.append("</body></html>");
        displayArea.setText(html.toString());
    }

    private void updateVehicleDisplay() {
        viewAvailableVehicles(); // Default to showing available vehicles
    }

    private void displayMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            displayArea.setText("<html><body><h3>" + message + "</h3></body></html>");
        });
    }

    private void openAdminPanel() {
        if (userSession.isAdmin()) {
            AdminPanel adminPanel = new AdminPanel(mainFrame, rentalManager, this::updateVehicleDisplay);
            adminPanel.show();
        } else {
            displayMessage("Access denied. Administrator privileges required.");
        }
    }

    // Cleanup resources when application closes
    private void shutdown() {
        executorService.shutdown();
        dbManager.closeConnection();
    }

    public static void main(String[] args) {
        try {
            // Set Nimbus look and feel
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            // Fall back to default look and feel
        }

        SwingUtilities.invokeLater(() -> {
            VehicleRentalSystem system = new VehicleRentalSystem();

            // Add shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(system::shutdown));
        });
    }
}