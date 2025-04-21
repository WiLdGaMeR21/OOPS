import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Admin Panel for the Vehicle Rental System
 */
public class AdminPanel {
    private final JDialog adminDialog;
    private final RentalManager rentalManager;
    private final Runnable updateDisplayCallback;

    /**
     * Constructs an admin panel
     * @param parent The parent JFrame
     * @param rentalManager The rental manager instance
     * @param updateDisplayCallback Callback to update the main display
     */
    public AdminPanel(JFrame parent, RentalManager rentalManager, Runnable updateDisplayCallback) {
        this.rentalManager = rentalManager;
        this.updateDisplayCallback = updateDisplayCallback;

        // Create dialog
        adminDialog = new JDialog(parent, "Admin Panel", true);
        adminDialog.setSize(800, 600);
        adminDialog.setLocationRelativeTo(parent);

        // Create tabbed pane
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Add Vehicle", createAddVehiclePanel());
        tabbedPane.addTab("Remove Vehicle", createRemoveVehiclePanel());
        tabbedPane.addTab("View All Vehicles", createViewAllPanel());

        adminDialog.add(tabbedPane);
    }

    public void show() {
        adminDialog.setVisible(true);
    }

    private JPanel createAddVehiclePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Form fields
        JTextField modelField = new JTextField(20);
        JComboBox<String> typeField = new JComboBox<>(new String[]{"Car", "Bike", "Truck", "Van"});
        JTextField rentField = new JTextField(10);

        // Add components to panel
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Model:"), gbc);

        gbc.gridx = 1;
        panel.add(modelField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("Type:"), gbc);

        gbc.gridx = 1;
        panel.add(typeField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel("Rent Per Day ($):"), gbc);

        gbc.gridx = 1;
        panel.add(rentField, gbc);

        // Add button
        JButton addButton = new JButton("Add Vehicle");
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(addButton, gbc);

        // Add action
        addButton.addActionListener(e -> {
            try {
                String model = modelField.getText().trim();
                String type = (String) typeField.getSelectedItem();
                double rent = Double.parseDouble(rentField.getText().trim());

                if (model.isEmpty()) {
                    throw new IllegalArgumentException("Model cannot be empty");
                }

                if (rent <= 0) {
                    throw new IllegalArgumentException("Rent must be greater than zero");
                }

                rentalManager.addVehicle(model, type, rent);
                JOptionPane.showMessageDialog(adminDialog, "Vehicle Added Successfully!");

                // Clear fields
                modelField.setText("");
                rentField.setText("");

                // Update display
                updateDisplayCallback.run();

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(adminDialog, "Please enter a valid number for rent",
                        "Invalid Input", JOptionPane.ERROR_MESSAGE);
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(adminDialog, ex.getMessage(),
                        "Invalid Input", JOptionPane.ERROR_MESSAGE);
            }
        });

        return panel;
    }

    private JPanel createRemoveVehiclePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        // Text area to show all vehicles
        JTextArea vehiclesArea = new JTextArea(15, 50);
        vehiclesArea.setEditable(false);
        vehiclesArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        updateVehicleList(vehiclesArea);
        JScrollPane scrollPane = new JScrollPane(vehiclesArea);

        // Add the scroll pane with vehicle list
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        panel.add(scrollPane, gbc);

        // Create a sub-panel for the ID field and label with FlowLayout
        JPanel idPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        idPanel.add(new JLabel("Vehicle ID to Remove:"));

        JTextField removeIdField = new JTextField(10);
        removeIdField.setPreferredSize(new Dimension(100, 25)); // Set explicit size
        idPanel.add(removeIdField);

        // Add the ID panel
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weighty = 0;
        panel.add(idPanel, gbc);

        // Create a panel for buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));

        JButton removeButton = new JButton("Remove Vehicle");
        JButton refreshButton = new JButton("Refresh Vehicle List");

        buttonPanel.add(removeButton);
        buttonPanel.add(refreshButton);

        // Add the button panel
        gbc.gridy = 2;
        panel.add(buttonPanel, gbc);

        // Set up actions
        removeButton.addActionListener(e -> {
            try {
                int id = Integer.parseInt(removeIdField.getText().trim());
                boolean removed = rentalManager.removeVehicle(id);

                if (removed) {
                    JOptionPane.showMessageDialog(adminDialog, "Vehicle Removed Successfully!");
                    updateVehicleList(vehiclesArea);
                    updateDisplayCallback.run();
                    removeIdField.setText("");
                } else {
                    JOptionPane.showMessageDialog(adminDialog, "Vehicle ID not found",
                            "Remove Failed", JOptionPane.ERROR_MESSAGE);
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(adminDialog, "Please enter a valid Vehicle ID",
                        "Invalid Input", JOptionPane.ERROR_MESSAGE);
            }
        });

        refreshButton.addActionListener(e -> updateVehicleList(vehiclesArea));

        return panel;
    }

    private JPanel createViewAllPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JTextArea vehiclesArea = new JTextArea(20, 50);
        vehiclesArea.setEditable(false);
        vehiclesArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        updateVehicleList(vehiclesArea);

        JScrollPane scrollPane = new JScrollPane(vehiclesArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> updateVehicleList(vehiclesArea));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(refreshButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void updateVehicleList(JTextArea textArea) {
        List<Vehicle> allVehicles = rentalManager.getAllVehicles();
        String formattedTable = VehicleFormatter.formatVehicleTable(allVehicles, "VEHICLE INVENTORY");
        textArea.setText(formattedTable);
    }
}