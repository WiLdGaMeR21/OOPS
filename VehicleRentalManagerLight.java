//import javax.swing.*;
//import java.awt.*;
//import java.util.List;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.concurrent.TimeUnit;
//import javax.swing.table.DefaultTableCellRenderer;
//import javax.swing.SwingConstants;
//import java.util.function.Consumer;
//
//public class VehicleRentalSystem {
//    private final RentalManager rentalManager = new RentalManager();
//    private final FileManager fileManager = FileManager.getInstance();
//    private final ExecutorService executorService = Executors.newFixedThreadPool(2); // Reduced thread pool size
//
//    private final JFrame mainFrame;
//    private JPanel displayPanel;
//    private JTextField vehicleIdField;
//    private JLabel statusLabel;
//
//    private static final Font HEADER_FONT = new Font("Segoe UI", Font.BOLD, 16);
//    private static final Font REGULAR_FONT = new Font("Segoe UI", Font.PLAIN, 14);
//    private static final Font TABLE_HEADER_FONT = new Font("Segoe UI", Font.BOLD, 12);
//    private static final Font TABLE_FONT = new Font("Segoe UI", Font.PLAIN, 12);
//
//    // Then add a method to set application-wide fonts in the constructor after the UIManager setup:
//    private void setupFonts() {
//        // Set default fonts for common components
//        UIManager.put("Label.font", REGULAR_FONT);
//        UIManager.put("TextField.font", REGULAR_FONT);
//        UIManager.put("Button.font", REGULAR_FONT);
//        UIManager.put("ComboBox.font", REGULAR_FONT);
//        UIManager.put("Table.font", TABLE_FONT);
//        UIManager.put("TableHeader.font", TABLE_HEADER_FONT);
//        UIManager.put("TabbedPane.font", REGULAR_FONT);
//    }
//    // Constants for UI
//    private static final int WINDOW_WIDTH = 850;
//    private static final int WINDOW_HEIGHT = 600;
//    private static final Color SUCCESS_COLOR = new Color(39, 174, 96);
//    private static final Color WARNING_COLOR = new Color(230, 126, 34);
//    private static final Color ERROR_COLOR = new Color(231, 76, 60);
//    private static final int TABLE_ROW_HEIGHT = 28;
//
//    public VehicleRentalSystem() {
//        mainFrame = new JFrame("Vehicle Rental System");
//        mainFrame.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
//        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//
//        // Set application look and feel
//        try {
//            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
//            setupFonts(); // Apply custom fonts
//        } catch (Exception e) {
//            // Fallback to default
//        }
//
//        showLoginScreen();
//    }
//
//    private void showLoginScreen() {
//        mainFrame.getContentPane().removeAll();
//
//        LoginPanel loginPanel = new LoginPanel();
//        loginPanel.setLoginListener((username, role) -> {
//            LoginPanel.UserSession.getInstance().login(username, role);
//            showMainApplication();
//        });
//
//        mainFrame.getContentPane().add(loginPanel);
//        mainFrame.setLocationRelativeTo(null);
//        mainFrame.setVisible(true);
//    }
//
//    private void showMainApplication() {
//        mainFrame.getContentPane().removeAll();
//        mainFrame.setLayout(new BorderLayout(10, 10));
//
//        // Create a gradient header panel
//        JPanel headerPanel = createGradientHeaderPanel();
//        mainFrame.add(headerPanel, BorderLayout.NORTH);
//
//        // Main content area with padding
//        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
//        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 15, 15));
//
//        // Display panel with a subtle border
//        displayPanel = new JPanel(new BorderLayout());
//        displayPanel.setBorder(BorderFactory.createCompoundBorder(
//                BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
//                BorderFactory.createEmptyBorder(5, 5, 5, 5)
//        ));
//
//        contentPanel.add(createControlPanel(), BorderLayout.NORTH);
//        contentPanel.add(displayPanel, BorderLayout.CENTER);
//        contentPanel.add(createBottomPanel(), BorderLayout.SOUTH);
//
//        mainFrame.add(contentPanel, BorderLayout.CENTER);
//
//        updateVehicleDisplay();
//        mainFrame.revalidate();
//        mainFrame.repaint();
//    }
//
//    private JPanel createGradientHeaderPanel() {
//        JPanel headerPanel = new JPanel(new BorderLayout()) {
//            @Override
//            protected void paintComponent(Graphics g) {
//                super.paintComponent(g);
//                Graphics2D g2d = (Graphics2D) g.create();
//                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//
//                int w = getWidth();
//                int h = getHeight();
//                GradientPaint gp = new GradientPaint(0, 0, new Color(41, 128, 185), w, h, new Color(44, 62, 80));
//                g2d.setPaint(gp);
//                g2d.fillRect(0, 0, w, h);
//                g2d.dispose();
//            }
//        };
//        headerPanel.setPreferredSize(new Dimension(WINDOW_WIDTH, 70));
//
//        JPanel statusPanel = createStatusPanel();
//        statusPanel.setOpaque(false);
//        headerPanel.add(statusPanel, BorderLayout.CENTER);
//
//        return headerPanel;
//    }
//
//    private JPanel createStatusPanel() {
//        JPanel panel = new JPanel(new BorderLayout());
//        panel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
//        panel.setOpaque(false);
//
//        statusLabel = new JLabel();
//        statusLabel.setForeground(Color.WHITE);
//        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
//        updateStatusLabel();
//        panel.add(statusLabel, BorderLayout.WEST);
//
//        JButton logoutButton = new JButton("Logout");
//        logoutButton.setFocusPainted(false);
//        logoutButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
//        logoutButton.addActionListener(e -> {
//            LoginPanel.UserSession.getInstance().logout();
//            showLoginScreen();
//        });
//
//        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
//        buttonPanel.setOpaque(false);
//        buttonPanel.add(logoutButton);
//        panel.add(buttonPanel, BorderLayout.EAST);
//
//        return panel;
//    }
//
//    private void updateStatusLabel() {
//        LoginPanel.UserSession userSession = LoginPanel.UserSession.getInstance();
//        String role = userSession.isAdmin() ? "Administrator" : "Regular User";
//        statusLabel.setText("Logged in as: " + userSession.getUsername() + " (" + role + ")");
//    }
//
//    private JPanel createControlPanel() {
//        JPanel panel = new JPanel(new BorderLayout(10, 10));
//
//        // Search panel with rounded border
//        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
//        searchPanel.setBorder(BorderFactory.createCompoundBorder(
//                BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
//                BorderFactory.createEmptyBorder(8, 10, 8, 10)
//        ));
//
//        JLabel idLabel = new JLabel("Vehicle ID:");
//        vehicleIdField = new JTextField(10);
//        vehicleIdField.setPreferredSize(new Dimension(100, 30));
//
//        JButton rentButton = createStyledButton("Rent Vehicle", SUCCESS_COLOR);
//        rentButton.addActionListener(e -> executeTask(this::rentVehicle));
//
//        JButton returnButton = createStyledButton("Return Vehicle", WARNING_COLOR);
//        returnButton.addActionListener(e -> executeTask(this::returnVehicle));
//
//        searchPanel.add(idLabel);
//        searchPanel.add(vehicleIdField);
//        searchPanel.add(rentButton);
//        searchPanel.add(returnButton);
//
//        panel.add(searchPanel, BorderLayout.CENTER);
//        return panel;
//    }
//
//    private JButton createStyledButton(String text, Color baseColor) {
//        JButton button = new JButton(text);
//        button.setFocusPainted(false);
//        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
//        button.setForeground(Color.WHITE);
//        button.setBackground(baseColor);
//        button.setPreferredSize(new Dimension(120, 30));
//        button.setOpaque(true);  // This is crucial - ensures background is painted
//        button.setBorderPainted(false);  // Remove default button border
//
//        // Create a custom UI to override look and feel
//        button.setUI(new javax.swing.plaf.basic.BasicButtonUI());
//
//        button.addMouseListener(new java.awt.event.MouseAdapter() {
//            @Override
//            public void mouseEntered(java.awt.event.MouseEvent evt) {
//                button.setBackground(baseColor.darker());
//            }
//
//            @Override
//            public void mouseExited(java.awt.event.MouseEvent evt) {
//                button.setBackground(baseColor);
//            }
//        });
//
//        return button;
//    }
//
//    private JPanel createBottomPanel() {
//        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
//
//        JButton viewAllButton = createStyledButton("View All Vehicles", new Color(52, 152, 219));
//        viewAllButton.addActionListener(e -> executeTask(this::viewAllVehicles));
//
//        JButton viewAvailableButton = createStyledButton("View Available", new Color(52, 152, 219));
//        viewAvailableButton.addActionListener(e -> executeTask(this::viewAvailableVehicles));
//
//        JButton adminButton = createStyledButton("Admin Panel", new Color(142, 68, 173));
//        adminButton.addActionListener(e -> executeTask(this::openAdminPanel));
//        adminButton.setVisible(LoginPanel.UserSession.getInstance().isAdmin());
//
//        panel.add(viewAllButton);
//        panel.add(viewAvailableButton);
//        panel.add(adminButton);
//
//        return panel;
//    }
//
//    private void executeTask(Runnable task) {
//        executorService.submit(() -> {
//            try {
//                task.run();
//            } catch (NumberFormatException ex) {
//                SwingUtilities.invokeLater(() -> displayMessage("Please enter a valid numeric ID"));
//            } catch (Exception ex) {
//                SwingUtilities.invokeLater(() -> displayMessage("Error: " + ex.getMessage()));
//            }
//        });
//    }
//
//    private void rentVehicle() {
//        try {
//            String vehicleIdText = vehicleIdField.getText().trim();
//            if (vehicleIdText.isEmpty()) {
//                throw new IllegalArgumentException("Vehicle ID cannot be empty");
//            }
//
//            int vehicleId = Integer.parseInt(vehicleIdText);
//            if (rentalManager.rentVehicle(vehicleId)) {
//                displayMessage("Vehicle ID " + vehicleId + " rented successfully!");
//                updateVehicleDisplay();
//            } else {
//                displayMessage("Vehicle not available or invalid ID.");
//            }
//        } catch (NumberFormatException e) {
//            throw new NumberFormatException("Please enter a valid Vehicle ID");
//        }
//    }
//
//    private void returnVehicle() {
//        try {
//            String vehicleIdText = vehicleIdField.getText().trim();
//            if (vehicleIdText.isEmpty()) {
//                throw new IllegalArgumentException("Vehicle ID cannot be empty");
//            }
//
//            int vehicleId = Integer.parseInt(vehicleIdText);
//            if (rentalManager.returnVehicle(vehicleId)) {
//                displayMessage("Vehicle ID " + vehicleId + " returned successfully!");
//                updateVehicleDisplay();
//            } else {
//                displayMessage("Invalid Vehicle ID or already returned.");
//            }
//        } catch (NumberFormatException e) {
//            throw new NumberFormatException("Please enter a valid Vehicle ID");
//        }
//    }
//
//    private void viewAllVehicles() {
//        displayVehicles("All Vehicles", rentalManager.getAllVehicles());
//    }
//
//    private void viewAvailableVehicles() {
//        List<Vehicle> availableVehicles = rentalManager.getAvailableVehicles();
//
//        if (availableVehicles.isEmpty()) {
//            displayMessage("No vehicles are currently available for rent.");
//            return;
//        }
//
//        displayVehicles("Available Vehicles", availableVehicles);
//    }
//
//    private void displayVehicles(String header, List<Vehicle> vehicles) {
//        SwingUtilities.invokeLater(() -> {
//            displayPanel.removeAll();
//
//            // Create header with gradient background
//            JPanel headerPanel = new JPanel(new BorderLayout()) {
//                @Override
//                protected void paintComponent(Graphics g) {
//                    super.paintComponent(g);
//                    Graphics2D g2d = (Graphics2D) g.create();
//                    int w = getWidth();
//                    int h = getHeight();
//                    GradientPaint gp = new GradientPaint(0, 0, new Color(52, 152, 219), w, h, new Color(52, 152, 219).darker());
//                    g2d.setPaint(gp);
//                    g2d.fillRect(0, 0, w, h);
//                    g2d.dispose();
//                }
//            };
//
//            JLabel titleLabel = new JLabel(header.toUpperCase());
//            titleLabel.setForeground(Color.WHITE);
//            titleLabel.setFont(HEADER_FONT);
//            titleLabel.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
//            headerPanel.add(titleLabel, BorderLayout.WEST);
//
//            String[] columnNames = {"ID", "Type", "Model", "Rent/Day", "Available", "Total", "Status"};
//            Object[][] data = new Object[vehicles.size()][7];
//
//            for (int i = 0; i < vehicles.size(); i++) {
//                Vehicle vehicle = vehicles.get(i);
//                data[i][0] = vehicle.getId();
//                data[i][1] = vehicle.getType();
//                data[i][2] = vehicle.getModel();
//                data[i][3] = String.format("$%.2f", vehicle.getRentPerDay());
//                data[i][4] = vehicle.getAvailableQuantity();
//                data[i][5] = vehicle.getQuantity();
//                data[i][6] = vehicle.getAvailabilityStatus();
//            }
//
//            JTable vehicleTable = new JTable(data, columnNames) {
//                @Override
//                public boolean isCellEditable(int row, int column) {
//                    return false;
//                }
//
//                // Alternating row colors
//                @Override
//                public Component prepareRenderer(javax.swing.table.TableCellRenderer renderer, int row, int column) {
//                    Component c = super.prepareRenderer(renderer, row, column);
//                    if (!isRowSelected(row)) {
//                        c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(245, 245, 245));
//                    }
//                    return c;
//                }
//            };
//
//            vehicleTable.setFillsViewportHeight(true);
//            vehicleTable.setRowHeight(TABLE_ROW_HEIGHT);
//            vehicleTable.getTableHeader().setReorderingAllowed(false);
//            vehicleTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
//            vehicleTable.setShowGrid(false);
//            vehicleTable.setIntercellSpacing(new Dimension(0, 0));
//
//            // Style header
//            vehicleTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
//            vehicleTable.getTableHeader().setPreferredSize(new Dimension(0, 30));
//
//            // Column widths
//            vehicleTable.getColumnModel().getColumn(0).setPreferredWidth(40);
//            vehicleTable.getColumnModel().getColumn(1).setPreferredWidth(80);
//            vehicleTable.getColumnModel().getColumn(2).setPreferredWidth(180);
//            vehicleTable.getColumnModel().getColumn(3).setPreferredWidth(80);
//            vehicleTable.getColumnModel().getColumn(4).setPreferredWidth(60);
//            vehicleTable.getColumnModel().getColumn(5).setPreferredWidth(60);
//            vehicleTable.getColumnModel().getColumn(6).setPreferredWidth(100);
//
//            // Cell renderers
//            DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
//            centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
//            vehicleTable.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
//
//            DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
//            rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
//            vehicleTable.getColumnModel().getColumn(3).setCellRenderer(rightRenderer);
//
//            // Available column renderer
//            vehicleTable.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
//                @Override
//                public Component getTableCellRendererComponent(JTable table, Object value,
//                                                               boolean isSelected, boolean hasFocus,
//                                                               int row, int column) {
//                    Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
//                    setHorizontalAlignment(SwingConstants.CENTER);
//
//                    if (!isSelected) {
//                        int availableCount = (Integer) value;
//                        if (availableCount <= 0) {
//                            setForeground(ERROR_COLOR);
//                        } else if (availableCount < 3) {
//                            setForeground(WARNING_COLOR);
//                        } else {
//                            setForeground(SUCCESS_COLOR);
//                        }
//                    }
//                    return c;
//                }
//            });
//
//            vehicleTable.getColumnModel().getColumn(5).setCellRenderer(centerRenderer);
//
//            // Status column renderer
//            vehicleTable.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
//                @Override
//                public Component getTableCellRendererComponent(JTable table, Object value,
//                                                               boolean isSelected, boolean hasFocus,
//                                                               int row, int column) {
//                    Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
//
//                    if (!isSelected) {
//                        String strValue = value.toString();
//                        if (strValue.contains("Rented")) {
//                            setForeground(ERROR_COLOR);
//                        } else if (strValue.contains("Available")) {
//                            setForeground(SUCCESS_COLOR);
//                        } else {
//                            setForeground(WARNING_COLOR);
//                        }
//                    }
//
//                    return c;
//                }
//            });
//
//            // Row selection listener
//            vehicleTable.getSelectionModel().addListSelectionListener(e -> {
//                if (!e.getValueIsAdjusting()) {
//                    int selectedRow = vehicleTable.getSelectedRow();
//                    if (selectedRow >= 0) {
//                        Object idValue = vehicleTable.getValueAt(selectedRow, 0);
//                        vehicleIdField.setText(idValue.toString());
//                    }
//                }
//            });
//
//            // Table scroll pane with custom border
//            JScrollPane tableScrollPane = new JScrollPane(vehicleTable);
//            tableScrollPane.setBorder(BorderFactory.createEmptyBorder());
//
//            // Summary panel
//            int totalVehicles = vehicles.size();
//            int totalQuantity = vehicles.stream().mapToInt(Vehicle::getQuantity).sum();
//            int availableQuantity = vehicles.stream().mapToInt(Vehicle::getAvailableQuantity).sum();
//            int rentedQuantity = totalQuantity - availableQuantity;
//
//            JPanel summaryPanel = new JPanel(new BorderLayout());
//            summaryPanel.setBorder(BorderFactory.createCompoundBorder(
//                    BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY),
//                    BorderFactory.createEmptyBorder(8, 5, 8, 5)
//            ));
//
//            JLabel countLabel = new JLabel(String.format(
//                    "<html><b>Summary:</b> %d models (%d units, <font color='#27ae60'>%d available</font>, <font color='#e74c3c'>%d rented</font>)</html>",
//                    totalVehicles, totalQuantity, availableQuantity, rentedQuantity));
//
//            summaryPanel.add(countLabel, BorderLayout.WEST);
//
//            displayPanel.add(headerPanel, BorderLayout.NORTH);
//            displayPanel.add(tableScrollPane, BorderLayout.CENTER);
//            displayPanel.add(summaryPanel, BorderLayout.SOUTH);
//
//            displayPanel.revalidate();
//            displayPanel.repaint();
//        });
//    }
//
//    private void updateVehicleDisplay() {
//        viewAvailableVehicles();
//    }
//
//    private void displayMessage(String message) {
//        SwingUtilities.invokeLater(() -> {
//            displayPanel.removeAll();
//
//            JPanel messagePanel = new JPanel(new BorderLayout());
//            messagePanel.setBorder(BorderFactory.createEmptyBorder(40, 20, 40, 20));
//
//            JLabel messageLabel = new JLabel(message);
//            messageLabel.setFont(REGULAR_FONT);
//            messageLabel.setHorizontalAlignment(JLabel.CENTER);
//
//            messagePanel.add(messageLabel, BorderLayout.CENTER);
//            displayPanel.add(messagePanel, BorderLayout.CENTER);
//
//            displayPanel.revalidate();
//            displayPanel.repaint();
//        });
//    }
//
//    private void openAdminPanel() {
//        if (LoginPanel.UserSession.getInstance().isAdmin()) {
//            AdminPanel adminPanel = new AdminPanel(mainFrame, rentalManager, this::updateVehicleDisplay);
//            adminPanel.show();
//        } else {
//            displayMessage("Access denied. Administrator privileges required.");
//        }
//    }
//
//    private void shutdown() {
//        executorService.shutdown();
//        try {
//            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
//                executorService.shutdownNow();
//            }
//        } catch (InterruptedException e) {
//            executorService.shutdownNow();
//            Thread.currentThread().interrupt();
//        }
//        fileManager.shutdown();
//        rentalManager.closeResources();
//    }
//
//    public static void main(String[] args) {
//        try {
//            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
//        } catch (Exception e) {
//            // Fall back to default look and feel
//        }
//
//        SwingUtilities.invokeLater(() -> {
//            VehicleRentalSystem system = new VehicleRentalSystem();
//            Runtime.getRuntime().addShutdownHook(new Thread(system::shutdown));
//        });
//    }
//}