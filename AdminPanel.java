import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

/**
 * Admin Panel for the Vehicle Rental System
 */
public class AdminPanel {

    // UI theme constants - adjusted to match screenshot
    private static final Color DARK_BG = new Color(18, 18, 18);
    private static final Color DARK_PANEL = new Color(30, 30, 30);
    private static final Color LIGHT_TEXT = new Color(230, 230, 230);
    private static final Color HEADER_BG = new Color(90, 90, 90);
    private static final Color HEADER_TEXT = new Color(240, 240, 240);
    private static final Color DARK_ROW_EVEN = new Color(35, 35, 35);
    private static final Color DARK_ROW_ODD = new Color(45, 45, 45);
    private static final Color DARK_SELECTION = new Color(65, 105, 225);
    private static final Color REMOVE_BUTTON_COLOR = new Color(231, 76, 60);
    private static final Color UPDATE_BUTTON_COLOR = new Color(52, 152, 219);
    private static final Color CLOSE_BUTTON_COLOR = new Color(70, 70, 70);
    private static final Color SUCCESS_COLOR = new Color(39, 174, 96);

    // Font constants
    private static final Font LABEL_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 16);
    private static final Font BUTTON_FONT = new Font("Segoe UI", Font.BOLD, 14);
    private static final Font TABLE_FONT = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font HEADER_FONT = new Font("Segoe UI", Font.BOLD, 13);

    // Component dimension constants
    private static final Dimension FIELD_DIMENSION = new Dimension(200, 28);
    private static final Dimension SMALL_FIELD_DIMENSION = new Dimension(80, 28);
    private static final Dimension TINY_FIELD_DIMENSION = new Dimension(60, 28);
    private static final Insets STANDARD_INSETS = new Insets(8, 8, 8, 8);

    // Core components
    private final JDialog adminDialog;
    private final RentalManager rentalManager;
    private final Runnable updateDisplayCallback;

    // Table components
    private DefaultTableModel vehicleTableModel;
    private JTable vehicleTable;

    // Input fields
    private JTextField idField;
    private JTextField quantityField;

    /**
     * Constructor for the AdminPanel
     *
     * @param parent The parent frame for modal dialog
     * @param rentalManager The rental manager instance
     * @param updateDisplayCallback Callback to update the main UI
     */
    public AdminPanel(JFrame parent, RentalManager rentalManager, Runnable updateDisplayCallback) {
        this.rentalManager = rentalManager;
        this.updateDisplayCallback = updateDisplayCallback;

        // Create admin dialog
        adminDialog = new JDialog(parent, "Admin Panel", true);
        adminDialog.setSize(850, 550);
        adminDialog.setLocationRelativeTo(parent);
        adminDialog.setResizable(true);
        adminDialog.setLayout(new BorderLayout(0, 0));
        adminDialog.getContentPane().setBackground(DARK_BG);

        // Create tabbed pane with proper styling
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setForeground(LIGHT_TEXT);
        tabbedPane.setBackground(DARK_BG);
        tabbedPane.setUI(new javax.swing.plaf.basic.BasicTabbedPaneUI() {
            @Override
            protected void installDefaults() {
                super.installDefaults();
                highlight = DARK_BG.brighter();
                lightHighlight = DARK_BG.brighter();
                shadow = DARK_BG;
                darkShadow = DARK_BG;
                focus = DARK_BG;
            }
        });

        tabbedPane.addTab("Add Vehicle", createAddVehiclePanel());
        tabbedPane.addTab("Manage Vehicles", createManageVehiclesPanel());
        adminDialog.add(tabbedPane, BorderLayout.CENTER);

        // Close button at the bottom with proper styling
        JButton closeButton = createStyledButton("Close Admin Panel", CLOSE_BUTTON_COLOR);
        closeButton.setPreferredSize(new Dimension(200, 30));
        closeButton.addActionListener(e -> adminDialog.dispose());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBackground(DARK_BG);
        buttonPanel.add(closeButton);
        adminDialog.add(buttonPanel, BorderLayout.SOUTH);
    }

    /**
     * Shows the admin panel dialog
     */
    public void show() {
        updateVehicleTable(); // Refresh data before showing
        adminDialog.setVisible(true);
    }

    /**
     * Creates the panel for adding new vehicles
     */
    private JPanel createAddVehiclePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(DARK_PANEL);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = STANDARD_INSETS;

        // Panel title
        JLabel titleLabel = createTitleLabel("Add New Vehicle");
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(titleLabel, gbc);

        // Form fields
        JTextField modelField = createStyledTextField();

        JComboBox<String> typeField = new JComboBox<>(new String[]{"Car", "Bike", "Truck", "Van"});
        typeField.setPreferredSize(FIELD_DIMENSION);
        typeField.setBackground(DARK_ROW_EVEN);
        typeField.setForeground(LIGHT_TEXT);

        JTextField rentField = createStyledTextField();
        JTextField quantityAddField = createStyledTextField();
        quantityAddField.setText("1"); // Default quantity is 1

        // Form layout
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;

        addFormRow(panel, gbc, "Model:", modelField, 1);
        addFormRow(panel, gbc, "Type:", typeField, 2);
        addFormRow(panel, gbc, "Rent Per Day ($):", rentField, 3);
        addFormRow(panel, gbc, "Quantity:", quantityAddField, 4);

        // Add button
        JButton addButton = createStyledButton("Add Vehicle", UPDATE_BUTTON_COLOR);
        addButton.setPreferredSize(new Dimension(150, 35));

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(20, 8, 8, 8);
        panel.add(addButton, gbc);

        // Add button action
        addButton.addActionListener(e -> addVehicle(modelField, typeField, rentField, quantityAddField));

        return panel;
    }

    /**
     * Handles the vehicle addition process
     */
    private void addVehicle(JTextField modelField, JComboBox<String> typeField,
                            JTextField rentField, JTextField quantityField) {
        try {
            String model = modelField.getText().trim();
            String type = (String) typeField.getSelectedItem();

            // Input validation
            if (model.isEmpty()) {
                throw new IllegalArgumentException("Model cannot be empty");
            }

            double rent = Double.parseDouble(rentField.getText().trim());
            if (rent <= 0) {
                throw new IllegalArgumentException("Rent must be greater than zero");
            }

            int quantity = Integer.parseInt(quantityField.getText().trim());
            if (quantity <= 0) {
                throw new IllegalArgumentException("Quantity must be greater than zero");
            }

            // Add vehicle
            rentalManager.addVehicle(model, type, rent, quantity);
            JOptionPane.showMessageDialog(adminDialog, "Vehicle Added Successfully!");

            // Clear fields
            modelField.setText("");
            rentField.setText("");
            quantityField.setText("1");

            // Update UI
            updateVehicleTable();
            updateDisplayCallback.run();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(adminDialog,
                    "Please enter valid numbers for rent and quantity",
                    "Invalid Input", JOptionPane.ERROR_MESSAGE);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(adminDialog, ex.getMessage(),
                    "Invalid Input", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Creates the panel for managing existing vehicles
     */
    private JPanel createManageVehiclesPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        panel.setBackground(DARK_BG);

        createVehicleTable();
        JScrollPane scrollPane = new JScrollPane(vehicleTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(DARK_PANEL);

        // Set up selection listener
        vehicleTable.getSelectionModel().addListSelectionListener(
                new ListSelectionListener() {
                    @Override
                    public void valueChanged(ListSelectionEvent e) {
                        handleTableSelection(e);
                    }
                }
        );

        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(createControlPanel(), BorderLayout.SOUTH);

        return panel;
    }

    /**
     * Creates the vehicle table
     */
    private void createVehicleTable() {
        vehicleTableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make all cells non-editable
            }

            @Override
            public Class<?> getColumnClass(int column) {
                switch (column) {
                    case 0: case 4: case 5: return Integer.class;
                    case 3: return Double.class;
                    default: return String.class;
                }
            }
        };

        String[] columns = {"ID", "Model", "Type", "Rent ($/day)", "Total Quantity", "Available", "Status"};
        for (String column : columns) {
            vehicleTableModel.addColumn(column);
        }

        vehicleTable = new JTable(vehicleTableModel);
        styleVehicleTable();
    }

    /**
     * Applies styling to the vehicle table
     */
    private void styleVehicleTable() {
        // Table styling
        vehicleTable.setFont(TABLE_FONT);
        vehicleTable.setRowHeight(28);
        vehicleTable.setFillsViewportHeight(true);
        vehicleTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        vehicleTable.setAutoCreateRowSorter(true);
        vehicleTable.setShowGrid(false);
        vehicleTable.setIntercellSpacing(new Dimension(0, 0));
        vehicleTable.setBackground(DARK_PANEL);
        vehicleTable.setForeground(LIGHT_TEXT);
        vehicleTable.setBorder(null);

        // Header styling - fixed to ensure visibility
        JTableHeader header = vehicleTable.getTableHeader();
        header.setFont(HEADER_FONT);
        header.setBackground(HEADER_BG);
        header.setForeground(HEADER_TEXT);
        header.setBorder(BorderFactory.createEmptyBorder());

        // Header renderer to ensure proper coloring
        ((DefaultTableCellRenderer)header.getDefaultRenderer()).setBackground(HEADER_BG);
        ((DefaultTableCellRenderer)header.getDefaultRenderer()).setForeground(HEADER_TEXT);
        ((DefaultTableCellRenderer)header.getDefaultRenderer()).setHorizontalAlignment(JLabel.LEFT);

        // Column widths
        int[] widths = {50, 180, 80, 100, 100, 80, 100};
        TableColumnModel columnModel = vehicleTable.getColumnModel();
        for (int i = 0; i < Math.min(widths.length, columnModel.getColumnCount()); i++) {
            columnModel.getColumn(i).setPreferredWidth(widths[i]);
        }

        // Create reusable renderers
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        centerRenderer.setBackground(DARK_PANEL);
        centerRenderer.setForeground(LIGHT_TEXT);

        // Apply center alignment to specific columns
        for (int i : new int[]{0, 4, 5}) {
            if (i < columnModel.getColumnCount()) {
                columnModel.getColumn(i).setCellRenderer(centerRenderer);
            }
        }

        // Currency renderer
        columnModel.getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(JLabel.RIGHT);

                if (value instanceof Double) {
                    setText(String.format("$%.2f", (Double) value));
                }

                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? DARK_ROW_EVEN : DARK_ROW_ODD);
                    c.setForeground(LIGHT_TEXT);
                }

                return c;
            }
        });

        // Status column renderer - with green color for Available status
        columnModel.getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? DARK_ROW_EVEN : DARK_ROW_ODD);

                    String status = value != null ? value.toString() : "";
                    if (status.contains("Available")) {
                        setForeground(SUCCESS_COLOR);  // Green for available
                    } else {
                        setForeground(LIGHT_TEXT);  // Default text color
                    }
                }
                return c;
            }
        });

        // Default renderer for alternating row colors
        DefaultTableCellRenderer alternatingRowRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? DARK_ROW_EVEN : DARK_ROW_ODD);
                    c.setForeground(LIGHT_TEXT);
                } else {
                    c.setBackground(DARK_SELECTION);
                    c.setForeground(Color.WHITE);
                }
                return c;
            }
        };

        // Apply default renderer to all columns
        for (int i = 0; i < columnModel.getColumnCount(); i++) {
            if (i != 3 && i != 6) { // Skip columns with custom renderers
                columnModel.getColumn(i).setCellRenderer(alternatingRowRenderer);
            }
        }

        // Selection colors
        vehicleTable.setSelectionBackground(DARK_SELECTION);
        vehicleTable.setSelectionForeground(Color.WHITE);
    }

    /**
     * Handles table row selection events
     */
    private void handleTableSelection(ListSelectionEvent e) {
        if (!e.getValueIsAdjusting() && vehicleTable.getSelectedRow() != -1) {
            int modelRow = vehicleTable.convertRowIndexToModel(vehicleTable.getSelectedRow());
            int id = (Integer) vehicleTableModel.getValueAt(modelRow, 0);
            idField.setText(String.valueOf(id));

            // Set current quantity as default
            int totalQuantity = (Integer) vehicleTableModel.getValueAt(modelRow, 4);
            quantityField.setText(String.valueOf(totalQuantity));
        }
    }

    /**
     * Creates the control panel for vehicle management actions
     */
    private JPanel createControlPanel() {
        JPanel controlPanel = new JPanel(new BorderLayout());
        controlPanel.setBackground(DARK_BG);

        // Left control panel (remove & update)
        JPanel leftControlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        leftControlPanel.setBackground(DARK_BG);

        // ID field
        JLabel idLabel = new JLabel("Vehicle ID:");
        idLabel.setForeground(LIGHT_TEXT);
        leftControlPanel.add(idLabel);

        idField = new JTextField(5);
        idField.setPreferredSize(SMALL_FIELD_DIMENSION);
        idField.setBackground(DARK_ROW_EVEN);
        idField.setForeground(LIGHT_TEXT);
        idField.setCaretColor(LIGHT_TEXT);
        leftControlPanel.add(idField);

        // Remove button
        JButton removeButton = createStyledButton("Remove Vehicle", REMOVE_BUTTON_COLOR);
        leftControlPanel.add(removeButton);

        // Quantity editing
        JLabel quantityLabel = new JLabel("New Quantity:");
        quantityLabel.setForeground(LIGHT_TEXT);
        leftControlPanel.add(quantityLabel);

        quantityField = new JTextField(3);
        quantityField.setPreferredSize(TINY_FIELD_DIMENSION);
        quantityField.setBackground(DARK_ROW_EVEN);
        quantityField.setForeground(LIGHT_TEXT);
        quantityField.setCaretColor(LIGHT_TEXT);
        leftControlPanel.add(quantityField);

        // Update button
        JButton updateQuantityButton = createStyledButton("Update Quantity", UPDATE_BUTTON_COLOR);
        leftControlPanel.add(updateQuantityButton);

        controlPanel.add(leftControlPanel, BorderLayout.WEST);

        // Right control panel (refresh)
        JPanel rightControlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rightControlPanel.setBackground(DARK_BG);

        JButton refreshButton = createStyledButton("Refresh Table", UPDATE_BUTTON_COLOR);
        rightControlPanel.add(refreshButton);
        controlPanel.add(rightControlPanel, BorderLayout.EAST);

        // Set up action listeners
        removeButton.addActionListener(e -> removeVehicle());
        updateQuantityButton.addActionListener(e -> updateQuantity());
        refreshButton.addActionListener(e -> updateVehicleTable());

        return controlPanel;
    }

    /**
     * Handles the vehicle removal process
     */
    private void removeVehicle() {
        try {
            int id = getSelectedOrEnteredVehicleId();
            if (id == -1) return;

            int confirm = JOptionPane.showConfirmDialog(adminDialog,
                    "Are you sure you want to remove vehicle ID " + id + "?",
                    "Confirm Removal", JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                boolean removed = rentalManager.removeVehicle(id);

                if (removed) {
                    JOptionPane.showMessageDialog(adminDialog, "Vehicle Removed Successfully!");
                    updateVehicleTable();
                    updateDisplayCallback.run();
                    idField.setText("");
                    quantityField.setText("");
                } else {
                    JOptionPane.showMessageDialog(adminDialog, "Vehicle ID not found or cannot be removed",
                            "Remove Failed", JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(adminDialog, "Please enter a valid Vehicle ID",
                    "Invalid Input", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Handles the quantity update process
     */
    private void updateQuantity() {
        try {
            int id = getSelectedOrEnteredVehicleId();
            if (id == -1) return;

            // Get new quantity
            String quantityText = quantityField.getText().trim();
            if (quantityText.isEmpty()) {
                JOptionPane.showMessageDialog(adminDialog,
                        "Please enter a quantity value",
                        "Missing Input", JOptionPane.ERROR_MESSAGE);
                return;
            }

            int newQuantity = Integer.parseInt(quantityText);
            if (newQuantity <= 0) {
                JOptionPane.showMessageDialog(adminDialog,
                        "Quantity must be greater than zero",
                        "Invalid Input", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Get current vehicle details
            int row = findVehicleRowById(id);
            if (row == -1) {
                JOptionPane.showMessageDialog(adminDialog,
                        "Vehicle ID not found", "Update Failed", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String model = (String) vehicleTableModel.getValueAt(row, 1);
            int currentQuantity = (Integer) vehicleTableModel.getValueAt(row, 4);
            int availableQuantity = (Integer) vehicleTableModel.getValueAt(row, 5);
            int rentedQuantity = currentQuantity - availableQuantity;

            // Check if there are rented vehicles that would be affected
            if (newQuantity < rentedQuantity) {
                JOptionPane.showMessageDialog(adminDialog,
                        "Cannot reduce quantity below the number of currently rented vehicles (" +
                                rentedQuantity + ")",
                        "Invalid Operation", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Confirm update
            int confirm = JOptionPane.showConfirmDialog(adminDialog,
                    "Update quantity of " + model + " (ID: " + id + ") from " +
                            currentQuantity + " to " + newQuantity + "?",
                    "Confirm Update", JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                boolean updated = rentalManager.updateVehicleQuantity(id, newQuantity);
                if (updated) {
                    JOptionPane.showMessageDialog(adminDialog,
                            "Vehicle quantity updated successfully!");
                    updateVehicleTable();
                    updateDisplayCallback.run();
                } else {
                    JOptionPane.showMessageDialog(adminDialog,
                            "Failed to update vehicle quantity",
                            "Update Failed", JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(adminDialog,
                    "Please enter valid numeric values for ID and quantity",
                    "Invalid Input", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Updates the vehicle table with current data
     */
    private void updateVehicleTable() {
        vehicleTableModel.setRowCount(0);
        List<Vehicle> vehicles = rentalManager.getAllVehicles();

        for (Vehicle vehicle : vehicles) {
            vehicleTableModel.addRow(new Object[] {
                    vehicle.getId(),
                    vehicle.getModel(),
                    vehicle.getType(),
                    vehicle.getRentPerDay(),
                    vehicle.getQuantity(),
                    vehicle.getAvailableQuantity(),
                    vehicle.getAvailabilityStatus()
            });
        }
    }

    // Helper methods for UI components

    /**
     * Creates a styled title label
     */
    private JLabel createTitleLabel(String text) {
        JLabel label = new JLabel(text, JLabel.CENTER);
        label.setFont(TITLE_FONT);
        label.setForeground(LIGHT_TEXT);
        return label;
    }

    /**
     * Creates a styled text field
     */
    private JTextField createStyledTextField() {
        JTextField field = new JTextField();
        field.setPreferredSize(FIELD_DIMENSION);
        field.setBackground(DARK_ROW_EVEN);
        field.setForeground(LIGHT_TEXT);
        field.setCaretColor(LIGHT_TEXT);
        return field;
    }

    /**
     * Creates a styled button
     */
    private JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setFont(BUTTON_FONT);
        button.setOpaque(true);
        button.setBorderPainted(false);

        // Use a flatter look to match screenshot
        button.setUI(new javax.swing.plaf.basic.BasicButtonUI());

        return button;
    }

    /**
     * Adds a form row with a label and component
     */
    private void addFormRow(JPanel panel, GridBagConstraints gbc,
                            String labelText, JComponent component, int row) {
        JLabel label = new JLabel(labelText);
        label.setFont(LABEL_FONT);
        label.setForeground(LIGHT_TEXT);

        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(label, gbc);

        gbc.gridx = 1;
        panel.add(component, gbc);
    }

    /**
     * Gets the selected or manually entered vehicle ID
     */
    private int getSelectedOrEnteredVehicleId() {
        String idText = idField.getText().trim();

        if (!idText.isEmpty()) {
            return Integer.parseInt(idText);
        } else if (vehicleTable.getSelectedRow() != -1) {
            int modelRow = vehicleTable.convertRowIndexToModel(vehicleTable.getSelectedRow());
            return (Integer) vehicleTableModel.getValueAt(modelRow, 0);
        } else {
            JOptionPane.showMessageDialog(adminDialog,
                    "Please select a vehicle or enter an ID",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            return -1;
        }
    }

    /**
     * Finds a vehicle row by ID
     */
    private int findVehicleRowById(int id) {
        for (int i = 0; i < vehicleTableModel.getRowCount(); i++) {
            if ((Integer) vehicleTableModel.getValueAt(i, 0) == id) {
                return i;
            }
        }
        return -1;
    }
}