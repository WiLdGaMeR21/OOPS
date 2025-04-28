import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.SwingConstants;
import java.util.concurrent.atomic.AtomicBoolean;

public class VehicleRentalSystem {
    private final RentalManager rentalManager = new RentalManager();
    private final FileManager fileManager = FileManager.getInstance();
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final AtomicBoolean isShutdownInitiated = new AtomicBoolean(false);

    // Dark mode color scheme - consolidated in one place
    private static final ColorScheme colors = new ColorScheme();

    private final JFrame mainFrame;
    private JPanel displayPanel;
    private JTextField vehicleIdField;
    private JLabel statusLabel;

    // Font constants
    private static final Font HEADER_FONT = new Font("Segoe UI", Font.BOLD, 16);
    private static final Font REGULAR_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font TABLE_HEADER_FONT = new Font("Segoe UI", Font.BOLD, 12);
    private static final Font TABLE_FONT = new Font("Segoe UI", Font.PLAIN, 12);

    // Size constants
    private static final int WINDOW_WIDTH = 850;
    private static final int WINDOW_HEIGHT = 600;
    private static final int TABLE_ROW_HEIGHT = 28;

    // Singleton class for color scheme
    private static class ColorScheme {
        final Color DARK_BG = new Color(18, 18, 18);
        final Color DARK_PANEL = new Color(30, 30, 30);
        final Color DARK_TEXT = new Color(220, 220, 220);  // Brighter text for better readability
        final Color DARK_ACCENT = new Color(52, 73, 94);   // Slightly brighter accent color
        final Color DARK_HOVER = new Color(65, 88, 110);   // Adjusted hover color
        final Color DARK_SUCCESS = new Color(46, 204, 113);
        final Color DARK_WARNING = new Color(230, 126, 34);
        final Color DARK_ERROR = new Color(231, 76, 60);
        final Color TABLE_HEADER_BG = new Color(40, 55, 71); // Distinct color for table headers
        final Color TABLE_BORDER = new Color(70, 70, 70);    // More visible border color
    }

    private void setupFonts() {
        // Set default fonts for common components
        UIManager.put("Label.font", REGULAR_FONT);
        UIManager.put("TextField.font", REGULAR_FONT);
        UIManager.put("Button.font", REGULAR_FONT);
        UIManager.put("ComboBox.font", REGULAR_FONT);
        UIManager.put("Table.font", TABLE_FONT);
        UIManager.put("TableHeader.font", TABLE_HEADER_FONT);
        UIManager.put("TabbedPane.font", REGULAR_FONT);
    }

    private void setupDarkModeUI() {
        UIManager.put("Panel.background", colors.DARK_BG);
        UIManager.put("Label.foreground", colors.DARK_TEXT);
        UIManager.put("TextField.background", colors.DARK_PANEL);
        UIManager.put("TextField.foreground", colors.DARK_TEXT);
        UIManager.put("Table.background", colors.DARK_PANEL);
        UIManager.put("Table.foreground", colors.DARK_TEXT);
        UIManager.put("TableHeader.background", colors.TABLE_HEADER_BG);
        UIManager.put("TableHeader.foreground", colors.DARK_TEXT);
        UIManager.put("ScrollPane.background", colors.DARK_BG);
        UIManager.put("TextArea.foreground", colors.DARK_TEXT);
        UIManager.put("TextArea.background", colors.DARK_PANEL);
        UIManager.put("ScrollBar.background", colors.DARK_BG);
        UIManager.put("ScrollBar.thumb", colors.DARK_ACCENT);
    }

    public VehicleRentalSystem() {
        mainFrame = new JFrame("Vehicle Rental System");
        mainFrame.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        mainFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        // Proper window closing with resource cleanup
        mainFrame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                shutdown();
                System.exit(0);
            }
        });

        // Set dark mode defaults
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            setupFonts();
            setupDarkModeUI();
        } catch (Exception e) {
            // Fallback to default, log warning
            System.err.println("Could not set look and feel: " + e.getMessage());
        }

        showLoginScreen();
    }

    private void showLoginScreen() {
        mainFrame.getContentPane().removeAll();
        LoginPanel loginPanel = new LoginPanel();
        loginPanel.setLoginListener((username, role) -> {
            LoginPanel.UserSession.getInstance().login(username, role);
            showMainApplication();
        });
        mainFrame.getContentPane().add(loginPanel);
        mainFrame.setLocationRelativeTo(null);
        mainFrame.setVisible(true);
    }

    private void showMainApplication() {
        // Ensure we're on the EDT
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::showMainApplication);
            return;
        }

        mainFrame.getContentPane().removeAll();
        mainFrame.setLayout(new BorderLayout(10, 10));

        JPanel headerPanel = createGradientHeaderPanel();
        mainFrame.add(headerPanel, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 15, 15));
        contentPanel.setBackground(colors.DARK_BG);

        displayPanel = new JPanel(new BorderLayout());
        displayPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(colors.DARK_ACCENT, 1),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        displayPanel.setBackground(colors.DARK_PANEL);

        contentPanel.add(createControlPanel(), BorderLayout.NORTH);
        contentPanel.add(displayPanel, BorderLayout.CENTER);
        contentPanel.add(createBottomPanel(), BorderLayout.SOUTH);

        mainFrame.add(contentPanel, BorderLayout.CENTER);

        // Load data asynchronously to improve startup time
        executeTask(this::updateVehicleDisplay);

        mainFrame.revalidate();
        mainFrame.repaint();
    }

    private JPanel createGradientHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();
                GradientPaint gp = new GradientPaint(0, 0, colors.DARK_ACCENT, w, h, colors.DARK_ACCENT.darker());
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, w, h);
                g2d.dispose();
            }
        };
        headerPanel.setPreferredSize(new Dimension(WINDOW_WIDTH, 70));
        headerPanel.add(createStatusPanel(), BorderLayout.CENTER);
        return headerPanel;
    }

    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        panel.setOpaque(false);

        statusLabel = new JLabel();
        statusLabel.setForeground(colors.DARK_TEXT);
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        updateStatusLabel();

        JButton logoutButton = createStyledButton("Logout", colors.DARK_ACCENT);
        logoutButton.addActionListener(e -> {
            LoginPanel.UserSession.getInstance().logout();
            showLoginScreen();
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setOpaque(false);
        buttonPanel.add(logoutButton);

        panel.add(statusLabel, BorderLayout.WEST);
        panel.add(buttonPanel, BorderLayout.EAST);
        return panel;
    }

    private void updateStatusLabel() {
        LoginPanel.UserSession userSession = LoginPanel.UserSession.getInstance();
        String role = userSession.isAdmin() ? "Administrator" : "Regular User";
        statusLabel.setText("Logged in as: " + userSession.getUsername() + " (" + role + ")");
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(colors.DARK_BG);

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        searchPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(colors.DARK_ACCENT, 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        searchPanel.setBackground(colors.DARK_PANEL);

        JLabel idLabel = new JLabel("Vehicle ID:");
        idLabel.setForeground(colors.DARK_TEXT);

        vehicleIdField = new JTextField(10);
        vehicleIdField.setPreferredSize(new Dimension(100, 30));
        vehicleIdField.setBackground(colors.DARK_PANEL);
        vehicleIdField.setForeground(colors.DARK_TEXT);
        vehicleIdField.setCaretColor(colors.DARK_TEXT);

        JButton rentButton = createStyledButton("Rent Vehicle", colors.DARK_SUCCESS);
        rentButton.addActionListener(e -> executeTask(this::rentVehicle));

        JButton returnButton = createStyledButton("Return Vehicle", colors.DARK_WARNING);
        returnButton.addActionListener(e -> executeTask(this::returnVehicle));

        searchPanel.add(idLabel);
        searchPanel.add(vehicleIdField);
        searchPanel.add(rentButton);
        searchPanel.add(returnButton);

        panel.add(searchPanel, BorderLayout.CENTER);
        return panel;
    }

    private JButton createStyledButton(String text, Color baseColor) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setForeground(Color.WHITE);
        button.setBackground(baseColor);
        button.setPreferredSize(new Dimension(120, 30));
        button.setOpaque(true);
        button.setBorderPainted(false);
        button.setUI(new javax.swing.plaf.basic.BasicButtonUI());

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(baseColor.brighter());
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(baseColor);
            }
        });
        return button;
    }

    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        panel.setBackground(colors.DARK_BG);

        JButton viewAllButton = createStyledButton("View All Vehicles", colors.DARK_ACCENT);
        viewAllButton.addActionListener(e -> executeTask(this::viewAllVehicles));

        JButton viewAvailableButton = createStyledButton("View Available", colors.DARK_ACCENT);
        viewAvailableButton.addActionListener(e -> executeTask(this::viewAvailableVehicles));

        JButton adminButton = createStyledButton("Admin Panel", colors.DARK_HOVER);
        adminButton.addActionListener(e -> executeTask(this::openAdminPanel));
        adminButton.setVisible(LoginPanel.UserSession.getInstance().isAdmin());

        panel.add(viewAllButton);
        panel.add(viewAvailableButton);
        panel.add(adminButton);
        return panel;
    }

    private void executeTask(Runnable task) {
        // Don't accept new tasks if shutdown is initiated
        if (isShutdownInitiated.get()) {
            return;
        }

        executorService.submit(() -> {
            try {
                task.run();
            } catch (NumberFormatException ex) {
                SwingUtilities.invokeLater(() ->
                        displayMessage("Please enter a valid numeric ID", colors.DARK_WARNING));
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() ->
                        displayMessage("Error: " + ex.getMessage(), colors.DARK_ERROR));
                // Log the full stack trace
                ex.printStackTrace();
            }
        });
    }

    private void rentVehicle() {
        try {
            String vehicleIdText = vehicleIdField.getText().trim();
            if (vehicleIdText.isEmpty()) {
                throw new IllegalArgumentException("Vehicle ID cannot be empty");
            }

            int vehicleId = Integer.parseInt(vehicleIdText);
            if (rentalManager.rentVehicle(vehicleId)) {
                displayMessage("Vehicle ID " + vehicleId + " rented successfully!", colors.DARK_SUCCESS);
                updateVehicleDisplay();
            } else {
                displayMessage("Vehicle not available or invalid ID.", colors.DARK_WARNING);
            }
        } catch (NumberFormatException e) {
            throw new NumberFormatException("Please enter a valid Vehicle ID");
        }
    }

    private void returnVehicle() {
        try {
            String vehicleIdText = vehicleIdField.getText().trim();
            if (vehicleIdText.isEmpty()) {
                throw new IllegalArgumentException("Vehicle ID cannot be empty");
            }

            int vehicleId = Integer.parseInt(vehicleIdText);
            if (rentalManager.returnVehicle(vehicleId)) {
                displayMessage("Vehicle ID " + vehicleId + " returned successfully!", colors.DARK_SUCCESS);
                updateVehicleDisplay();
            } else {
                displayMessage("Invalid Vehicle ID or already returned.", colors.DARK_WARNING);
            }
        } catch (NumberFormatException e) {
            throw new NumberFormatException("Please enter a valid Vehicle ID");
        }
    }

    private void viewAllVehicles() {
        List<Vehicle> allVehicles = rentalManager.getAllVehicles();
        if (allVehicles.isEmpty()) {
            displayMessage("No vehicles found in the system.", colors.DARK_WARNING);
            return;
        }
        displayVehicles("All Vehicles", allVehicles);
    }

    private void viewAvailableVehicles() {
        List<Vehicle> availableVehicles = rentalManager.getAvailableVehicles();

        if (availableVehicles.isEmpty()) {
            displayMessage("No vehicles are currently available for rent.", colors.DARK_WARNING);
            return;
        }

        displayVehicles("Available Vehicles", availableVehicles);
    }

    private void displayVehicles(String header, List<Vehicle> vehicles) {
        SwingUtilities.invokeLater(() -> {
            displayPanel.removeAll();

            JPanel headerPanel = new JPanel(new BorderLayout()) {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2d = (Graphics2D) g.create();
                    int w = getWidth();
                    int h = getHeight();
                    GradientPaint gp = new GradientPaint(0, 0, colors.DARK_ACCENT, w, h, colors.DARK_ACCENT.darker());
                    g2d.setPaint(gp);
                    g2d.fillRect(0, 0, w, h);
                    g2d.dispose();
                }
            };
            headerPanel.setOpaque(false);

            JLabel titleLabel = new JLabel(header.toUpperCase());
            titleLabel.setForeground(Color.WHITE);
            titleLabel.setFont(HEADER_FONT);
            titleLabel.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
            headerPanel.add(titleLabel, BorderLayout.WEST);

            // Table setup with dark mode colors
            String[] columnNames = {"ID", "Type", "Model", "Rent/Day", "Available", "Total", "Status"};
            Object[][] data = new Object[vehicles.size()][7];

            for (int i = 0; i < vehicles.size(); i++) {
                Vehicle vehicle = vehicles.get(i);
                data[i][0] = vehicle.getId();
                data[i][1] = vehicle.getType();
                data[i][2] = vehicle.getModel();
                data[i][3] = String.format("$%.2f", vehicle.getRentPerDay());
                data[i][4] = vehicle.getAvailableQuantity();
                data[i][5] = vehicle.getQuantity();
                data[i][6] = vehicle.getAvailabilityStatus();
            }

            // Create the table with a custom model to prevent editing
            JTable vehicleTable = new JTable(data, columnNames) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }

                @Override
                public Component prepareRenderer(javax.swing.table.TableCellRenderer renderer, int row, int column) {
                    Component c = super.prepareRenderer(renderer, row, column);
                    if (!isRowSelected(row)) {
                        // Alternating row colors with better contrast
                        c.setBackground(row % 2 == 0 ? colors.DARK_PANEL : new Color(35, 35, 35));
                        c.setForeground(colors.DARK_TEXT);

                        // Add subtle border to cells
                        ((JComponent)c).setBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, colors.TABLE_BORDER));
                    }
                    return c;
                }
            };

            // Set table properties
            vehicleTable.setBackground(colors.DARK_PANEL);
            vehicleTable.setForeground(colors.DARK_TEXT);
            vehicleTable.setGridColor(colors.TABLE_BORDER);
            vehicleTable.setFillsViewportHeight(true);
            vehicleTable.setRowHeight(TABLE_ROW_HEIGHT);
            vehicleTable.getTableHeader().setReorderingAllowed(false);
            vehicleTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

            // Add a more visible border to the table
            vehicleTable.setBorder(BorderFactory.createLineBorder(colors.TABLE_BORDER));

            // Header styling - enhanced to be more visible
            vehicleTable.getTableHeader().setFont(TABLE_HEADER_FONT);
            vehicleTable.getTableHeader().setBackground(colors.TABLE_HEADER_BG);
            vehicleTable.getTableHeader().setForeground(colors.DARK_TEXT);
            vehicleTable.getTableHeader().setPreferredSize(new Dimension(0, 35));
            // Add a bottom border to header
            vehicleTable.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, colors.TABLE_BORDER));

            // Set column renderers
            setupTableRenderers(vehicleTable);

            // Add selection listener
            vehicleTable.getSelectionModel().addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting()) {
                    int selectedRow = vehicleTable.getSelectedRow();
                    if (selectedRow >= 0) {
                        vehicleIdField.setText(vehicleTable.getValueAt(selectedRow, 0).toString());
                    }
                }
            });

            JScrollPane tableScrollPane = new JScrollPane(vehicleTable);
            // Improve scroll pane borders
            tableScrollPane.setBorder(BorderFactory.createLineBorder(colors.TABLE_BORDER, 1));
            tableScrollPane.getViewport().setBackground(colors.DARK_PANEL);

            // Summary panel with statistics
            JPanel summaryPanel = createSummaryPanel(vehicles);

            displayPanel.add(headerPanel, BorderLayout.NORTH);
            displayPanel.add(tableScrollPane, BorderLayout.CENTER);
            displayPanel.add(summaryPanel, BorderLayout.SOUTH);
            displayPanel.revalidate();
            displayPanel.repaint();
        });
    }

    // Update the table display method to enhance header and border visibility
    private void setupTableRenderers(JTable table) {
        // Make table grid lines more visible
        table.setGridColor(colors.TABLE_BORDER);
        table.setShowGrid(true);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(true);

        // Increase table header height for better visibility
        table.getTableHeader().setPreferredSize(new Dimension(0, 35));

        // Make the table header font slightly larger and bolder
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));

        // Center renderer for ID column
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);

        // Right renderer for price column
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        table.getColumnModel().getColumn(3).setCellRenderer(rightRenderer);

        // Available column renderer with color coding
        table.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(SwingConstants.CENTER);
                if (!isSelected) {
                    int availableCount = (Integer) value;
                    if (availableCount <= 0) setForeground(colors.DARK_ERROR);
                    else if (availableCount < 3) setForeground(colors.DARK_WARNING);
                    else setForeground(colors.DARK_SUCCESS);
                }
                return c;
            }
        });

        // Status column renderer with color coding
        table.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    String status = value.toString();
                    if (status.contains("Rented")) setForeground(colors.DARK_ERROR);
                    else if (status.contains("Available")) setForeground(colors.DARK_SUCCESS);
                    else setForeground(colors.DARK_WARNING);
                }
                return c;
            }
        });
    }

    private JPanel createSummaryPanel(List<Vehicle> vehicles) {
        JPanel summaryPanel = new JPanel(new BorderLayout());
        summaryPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, colors.DARK_ACCENT),
                BorderFactory.createEmptyBorder(8, 5, 8, 5)
        ));
        summaryPanel.setBackground(colors.DARK_PANEL);

        int totalVehicles = vehicles.size();
        int totalQuantity = vehicles.stream().mapToInt(Vehicle::getQuantity).sum();
        int availableQuantity = vehicles.stream().mapToInt(Vehicle::getAvailableQuantity).sum();
        int rentedQuantity = totalQuantity - availableQuantity;

        JLabel countLabel = new JLabel(String.format(
                "<html><b style='color:%s'>Summary:</b> %d models (%d units, <font color='%s'>%d available</font>, <font color='%s'>%d rented</font></html>",
                Integer.toHexString(colors.DARK_TEXT.getRGB() & 0xFFFFFF), totalVehicles, totalQuantity,
                Integer.toHexString(colors.DARK_SUCCESS.getRGB() & 0xFFFFFF), availableQuantity,
                Integer.toHexString(colors.DARK_ERROR.getRGB() & 0xFFFFFF), rentedQuantity));
        countLabel.setForeground(colors.DARK_TEXT);

        summaryPanel.add(countLabel, BorderLayout.WEST);
        return summaryPanel;
    }

    private void updateVehicleDisplay() {
        viewAvailableVehicles();
    }

    private void displayMessage(String message, Color messageColor) {
        SwingUtilities.invokeLater(() -> {
            displayPanel.removeAll();

            JPanel messagePanel = new JPanel(new BorderLayout());
            messagePanel.setBorder(BorderFactory.createEmptyBorder(40, 20, 40, 20));
            messagePanel.setBackground(colors.DARK_PANEL);

            JLabel messageLabel = new JLabel(message);
            messageLabel.setFont(REGULAR_FONT);
            messageLabel.setForeground(messageColor);
            messageLabel.setHorizontalAlignment(JLabel.CENTER);

            messagePanel.add(messageLabel, BorderLayout.CENTER);
            displayPanel.add(messagePanel, BorderLayout.CENTER);

            displayPanel.revalidate();
            displayPanel.repaint();
        });
    }

    private void displayMessage(String message) {
        displayMessage(message, colors.DARK_TEXT);
    }

    private void openAdminPanel() {
        if (LoginPanel.UserSession.getInstance().isAdmin()) {
            AdminPanel adminPanel = new AdminPanel(mainFrame, rentalManager, this::updateVehicleDisplay);
            adminPanel.show();
        } else {
            displayMessage("Access denied. Administrator privileges required.", colors.DARK_ERROR);
        }
    }

    public void shutdown() {
        // Prevent multiple shutdown attempts
        if (isShutdownInitiated.getAndSet(true)) {
            return;
        }

        try {
            // Proper resource cleanup
            if (executorService != null && !executorService.isShutdown()) {
                executorService.shutdown();
                try {
                    // Wait for tasks to complete with timeout
                    if (!executorService.awaitTermination(3, TimeUnit.SECONDS)) {
                        // Force shutdown if tasks don't complete in time
                        executorService.shutdownNow();
                        if (!executorService.awaitTermination(2, TimeUnit.SECONDS)) {
                            System.err.println("ExecutorService did not terminate");
                        }
                    }
                } catch (InterruptedException e) {
                    executorService.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }

            // Close other resources
            if (fileManager != null) {
                fileManager.shutdown();
            }

            if (rentalManager != null) {
                rentalManager.closeResources();
            }

            System.out.println("Application resources cleaned up successfully");

        } catch (Exception e) {
            System.err.println("Error during shutdown: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Fall back to default look and feel
            System.err.println("Could not set system look and feel: " + e.getMessage());
        }

        SwingUtilities.invokeLater(() -> {
            final VehicleRentalSystem system = new VehicleRentalSystem();
            Runtime.getRuntime().addShutdownHook(new Thread(system::shutdown));
        });
    }
}