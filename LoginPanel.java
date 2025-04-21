import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.util.HashMap;
import java.util.Map;
import javax.swing.border.EmptyBorder;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

/**
 * Enhanced Login panel for the Vehicle Rental System
 */
public class LoginPanel extends JPanel {
    // Default users (username -> password, role)
    private static final Map<String, UserCredential> USERS = new HashMap<>();

    static {
        // Initialize with some default users
        USERS.put("admin", new UserCredential("admin123", UserRole.ADMIN));
        USERS.put("user1", new UserCredential("user123", UserRole.USER));
        USERS.put("user2", new UserCredential("user234", UserRole.USER));
    }

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton cancelButton;
    private LoginListener loginListener;
    private BufferedImage carImage;
    private JLabel statusLabel;
    private Timer loginAnimationTimer;
    private Color originalLoginButtonColor;
    private Color originalCancelButtonColor;

    /**
     * Constructs a login panel with enhanced UI
     */
    private void loadCarImage() {
        try {
            // Try different approaches to load the image
            InputStream is = getClass().getResourceAsStream("/resources/car_icon.png");
            if (is != null) {
                System.out.println("Found car image in resources folder");
                carImage = ImageIO.read(is);
            } else {
                System.out.println("Car image not found in resources, trying different path");
                // Try alternative paths
                is = getClass().getResourceAsStream("/car_icon.png");
                if (is != null) {
                    carImage = ImageIO.read(is);
                } else {
                    System.out.println("Creating placeholder car image");
                    // Create a placeholder image
                    carImage = new BufferedImage(100, 60, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g2d = carImage.createGraphics();
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2d.setColor(new Color(70, 130, 180));
                    g2d.fillRoundRect(10, 20, 80, 30, 10, 10);
                    g2d.setColor(new Color(60, 60, 60));
                    g2d.fillOval(20, 45, 15, 15);
                    g2d.fillOval(65, 45, 15, 15);
                    g2d.dispose();
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading car image: " + e.getMessage());
            e.printStackTrace();
            // Create placeholder
            carImage = new BufferedImage(100, 60, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = carImage.createGraphics();
            g2d.setColor(new Color(70, 130, 180));
            g2d.fillRoundRect(10, 20, 80, 30, 10, 10);
            g2d.dispose();
        }
    }

    public LoginPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        setBackground(new Color(245, 247, 250)); // Slightly blue-tinted background

        // Load car image
        loadCarImage();

        // Create all panels
        JPanel headerPanel = createHeaderPanel();
        JPanel formPanel = createFormPanel();
        JPanel buttonPanel = createButtonPanel();

        // Add all panels
        add(headerPanel, BorderLayout.NORTH);
        add(formPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        // Status label for messages
        statusLabel = new JLabel("", JLabel.CENTER);
        statusLabel.setForeground(new Color(150, 150, 150));
        statusLabel.setBorder(new EmptyBorder(10, 0, 0, 0));
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        // Create a panel for the status label
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setOpaque(false);
        statusPanel.add(statusLabel, BorderLayout.CENTER);
        add(statusPanel, BorderLayout.PAGE_END);
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 30, 0));

        // Image label - with transparent background
        JLabel imageLabel = new JLabel(new ImageIcon(carImage));
        imageLabel.setOpaque(false);
        imageLabel.setPreferredSize(new Dimension(100, 80));

        // Create a fancier title with shadow effect
        JLabel headerLabel = new JLabel("Vehicle Rental System", JLabel.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                // Draw shadow
                g2d.setFont(getFont());
                g2d.setColor(new Color(0, 0, 0, 30));
                g2d.drawString(getText(), 3, 43);

                // Draw main text
                g2d.setColor(new Color(41, 128, 185));
                g2d.drawString(getText(), 2, 42);
                g2d.dispose();
            }
        };
        headerLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        headerLabel.setPreferredSize(new Dimension(0, 50));
        headerLabel.setOpaque(false);

        // Use a FlowLayout for center panel
        JPanel centerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        centerPanel.setOpaque(false);
        centerPanel.add(imageLabel);

        panel.add(centerPanel, BorderLayout.NORTH);
        panel.add(headerLabel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createFormPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setOpaque(false);

        JPanel formPanel = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Create rounded rectangle with light color and shadow
                RoundRectangle2D roundedRect = new RoundRectangle2D.Float(
                        0, 0, getWidth() - 1, getHeight() - 1, 15, 15);

                // Draw shadow
                g2d.setColor(new Color(0, 0, 0, 20));
                g2d.fill(new RoundRectangle2D.Float(3, 3, getWidth() - 1, getHeight() - 1, 15, 15));

                // Draw panel background
                g2d.setColor(new Color(255, 255, 255));
                g2d.fill(roundedRect);

                // Draw border
                g2d.setColor(new Color(230, 230, 230));
                g2d.draw(roundedRect);

                g2d.dispose();
            }
        };
        formPanel.setOpaque(false);
        formPanel.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 10, 5);

        // Username field
        JLabel usernameLabel = new JLabel("Username");
        usernameLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        usernameField = createStyledTextField(new JTextField(15));

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        formPanel.add(usernameLabel, gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(0, 5, 15, 5);
        formPanel.add(usernameField, gbc);

        // Password field
        JLabel passwordLabel = new JLabel("Password");
        passwordLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        passwordField = createStyledPasswordField(new JPasswordField(15));

        gbc.gridy = 2;
        gbc.insets = new Insets(5, 5, 10, 5);
        formPanel.add(passwordLabel, gbc);

        gbc.gridy = 3;
        gbc.insets = new Insets(0, 5, 5, 5);
        formPanel.add(passwordField, gbc);

        // Remember me checkbox
        JCheckBox rememberMeCheckbox = new JCheckBox("Remember me");
        rememberMeCheckbox.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        rememberMeCheckbox.setOpaque(false);
        rememberMeCheckbox.setFocusPainted(false);

        gbc.gridy = 4;
        gbc.insets = new Insets(10, 5, 5, 5);
        formPanel.add(rememberMeCheckbox, gbc);

        // Add action for pressing Enter in the password field
        passwordField.addActionListener(e -> performLogin());

        // Center the form panel
        mainPanel.add(Box.createHorizontalStrut(80), BorderLayout.WEST);
        mainPanel.add(formPanel, BorderLayout.CENTER);
        mainPanel.add(Box.createHorizontalStrut(80), BorderLayout.EAST);

        return mainPanel;
    }

    private JTextField createStyledTextField(JTextField field) {
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        return field;
    }

    private JPasswordField createStyledPasswordField(JPasswordField field) {
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        return field;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        // Create properly styled buttons using our method
        loginButton = createStyledButton("Login", new Color(52, 152, 219));
        cancelButton = createStyledButton("Cancel", new Color(189, 195, 199));

        // Store original colors
        originalLoginButtonColor = loginButton.getBackground();
        originalCancelButtonColor = cancelButton.getBackground();

        // Add hover effects to buttons
        addButtonHoverEffect(loginButton);
        addButtonHoverEffect(cancelButton);

        // Add action listeners
        loginButton.addActionListener(e -> performLoginWithAnimation());
        cancelButton.addActionListener(e -> System.exit(0));

        // Add to panel
        panel.add(loginButton);
        panel.add(cancelButton);

        return panel;
    }

    private JButton createStyledButton(String text, Color bgColor) {
        // Create a button with modern styling
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int width = getWidth();
                int height = getHeight();

                // Draw background with rounded corners
                RoundRectangle2D roundedRectangle = new RoundRectangle2D.Float(0, 0, width - 1, height - 1, 8, 8);
                g2d.setColor(getBackground());
                g2d.fill(roundedRectangle);

                // Draw slight gradient overlay for 3D effect
                GradientPaint gradient = new GradientPaint(
                        0, 0, new Color(255, 255, 255, 40),
                        0, height, new Color(0, 0, 0, 20)
                );
                g2d.setPaint(gradient);
                g2d.fill(roundedRectangle);

                // Draw text
                FontMetrics metrics = g2d.getFontMetrics(getFont());
                int textWidth = metrics.stringWidth(getText());
                int textHeight = metrics.getHeight();
                int x = (width - textWidth) / 2;
                int y = ((height - textHeight) / 2) + metrics.getAscent();

                g2d.setColor(getForeground());
                g2d.setFont(getFont());
                g2d.drawString(getText(), x, y);

                g2d.dispose();
            }

            @Override
            protected void paintBorder(Graphics g) {
                // No visible border
            }
        };

        // Set basic properties
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setPreferredSize(new Dimension(120, 40));

        return button;
    }

    private void addButtonHoverEffect(JButton button) {
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                Color currentColor = button.getBackground();
                button.setBackground(darken(currentColor, 0.1f));
                setCursor(new Cursor(Cursor.HAND_CURSOR));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (button == loginButton && loginAnimationTimer != null && loginAnimationTimer.isRunning()) {
                    return; // Don't revert color during animation
                }

                if (button == loginButton) {
                    button.setBackground(originalLoginButtonColor);
                } else {
                    button.setBackground(originalCancelButtonColor);
                }
                setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }
        });
    }

    private Color darken(Color color, float factor) {
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        return Color.getHSBColor(hsb[0], hsb[1], Math.max(0, hsb[2] - factor));
    }

    private Color lighten(Color color, float factor) {
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        return Color.getHSBColor(hsb[0], Math.max(0, hsb[1] - factor), Math.min(1.0f, hsb[2] + factor));
    }

    private void performLoginWithAnimation() {
        // Disable buttons during animation
        loginButton.setEnabled(false);
        cancelButton.setEnabled(false);

        // Store original colors
        final Color originalColor = loginButton.getBackground();

        // Create animation timer
        if (loginAnimationTimer != null && loginAnimationTimer.isRunning()) {
            loginAnimationTimer.stop();
        }

        statusLabel.setText("Logging in...");

        loginAnimationTimer = new Timer(30, new ActionListener() {
            private int pulseCount = 0;
            private final int totalPulses = 10;
            private boolean brightening = false;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (pulseCount >= totalPulses) {
                    loginAnimationTimer.stop();
                    loginButton.setBackground(originalColor);
                    loginButton.setEnabled(true);
                    cancelButton.setEnabled(true);
                    performLogin();
                    return;
                }

                Color currentColor = loginButton.getBackground();
                Color newColor;

                if (brightening) {
                    newColor = lighten(currentColor, 0.1f);
                    if (pulseCount % 2 == 0) brightening = false;
                } else {
                    newColor = darken(currentColor, 0.1f);
                    if (pulseCount % 2 == 1) brightening = true;
                }

                loginButton.setBackground(newColor);
                pulseCount++;
            }
        });

        loginAnimationTimer.start();
    }

    private void performLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Username and password cannot be empty");
            statusLabel.setForeground(new Color(231, 76, 60));
            animateErrorShake();
            return;
        }

        // Check credentials
        UserCredential credential = USERS.get(username);
        if (credential != null && credential.checkPassword(password)) {
            // Login successful
            statusLabel.setText("Login successful!");
            statusLabel.setForeground(new Color(46, 204, 113));

            if (loginListener != null) {
                // Short delay to show success message
                Timer successTimer = new Timer(800, e -> {
                    ((Timer)e.getSource()).stop();
                    loginListener.onLoginSuccess(username, credential.getRole());
                });
                successTimer.setRepeats(false);
                successTimer.start();
            }
        } else {
            // Login failed
            statusLabel.setText("Invalid username or password");
            statusLabel.setForeground(new Color(231, 76, 60));
            passwordField.setText("");
            animateErrorShake();
        }
    }

    private void animateErrorShake() {
        final Point originalLocation = usernameField.getLocation();
        final int shakeDistance = 10;
        final int shakeSpeed = 40;

        Timer shakeTimer = new Timer(shakeSpeed, new ActionListener() {
            private int shakeCount = 0;
            private final int totalShakes = 6;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (shakeCount >= totalShakes) {
                    ((Timer)e.getSource()).stop();

                    // Reset positions after shaking
                    Container parent = usernameField.getParent();
                    Point loc = SwingUtilities.convertPoint(
                            usernameField, originalLocation, parent);

                    usernameField.setLocation(originalLocation);
                    passwordField.setLocation(
                            originalLocation.x,
                            passwordField.getLocation().y);

                    return;
                }

                int offsetX = (shakeCount % 2 == 0) ? shakeDistance : -shakeDistance;

                Container parent = usernameField.getParent();
                Point loc = SwingUtilities.convertPoint(
                        usernameField,
                        new Point(originalLocation.x + offsetX, originalLocation.y),
                        parent);

                usernameField.setLocation(originalLocation.x + offsetX, originalLocation.y);
                passwordField.setLocation(originalLocation.x + offsetX, passwordField.getLocation().y);

                shakeCount++;
            }
        });

        shakeTimer.start();
    }

    /**
     * Sets the login listener for handling successful login events
     */
    public void setLoginListener(LoginListener listener) {
        this.loginListener = listener;
    }

    /**
     * Interface for login event callbacks
     */
    public interface LoginListener {
        void onLoginSuccess(String username, UserRole role);
    }

    /**
     * Enum representing user roles in the system
     */
    public enum UserRole {
        ADMIN, USER
    }

    /**
     * Class to store user credentials and role
     */
    private static class UserCredential {
        private final String password;
        private final UserRole role;

        public UserCredential(String password, UserRole role) {
            this.password = password;
            this.role = role;
        }

        public boolean checkPassword(String inputPassword) {
            return password.equals(inputPassword);
        }

        public UserRole getRole() {
            return role;
        }
    }
}