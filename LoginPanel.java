import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.geom.RoundRectangle2D;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.net.URL;
import javax.imageio.ImageIO;

/**
 * Enhanced Login panel for the Vehicle Rental System with integrated UserSession
 */
public class LoginPanel extends JPanel {
    // Default users (username -> credential)
    private static final Map<String, UserCredential> USERS = createDefaultUsers();

    // Color scheme - updated for dark mode
    private static final Color PRIMARY_COLOR = new Color(75, 123, 236); // Bright blue
    private static final Color SECONDARY_COLOR = new Color(99, 166, 255); // Lighter blue accent
    private static final Color BACKGROUND_COLOR = new Color(32, 33, 36); // Dark background
    private static final Color TEXT_COLOR = new Color(230, 237, 243); // Light text
    private static final Color SUCCESS_COLOR = new Color(46, 204, 113); // Green kept as is
    private static final Color ERROR_COLOR = new Color(231, 76, 60); // Red kept as is
    private static final Color BORDER_COLOR = new Color(60, 63, 68); // Darker border for inputs

    // UI components
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton cancelButton;
    private JCheckBox rememberMeCheckbox;
    private LoginListener loginListener;
    private JLabel statusLabel;
    private JPanel cardPanel;
    private CardLayout cardLayout;
    private Timer messageTimer;

    /**
     * Constructs a login panel
     */
    public LoginPanel() {
        initComponents();
        setupLayout();
        setupListeners();
    }

    /**
     * Initialize all UI components
     */
    private void initComponents() {
        // Setup form fields
        usernameField = createStyledTextField("Enter your username");
        passwordField = createStyledPasswordField("Enter your password");

        // Setup buttons
        loginButton = createStyledButton("Login", SUCCESS_COLOR);
        cancelButton = createStyledButton("Cancel", new Color(170, 170, 170));

        // Setup checkbox
        // Setup checkbox for dark mode
        rememberMeCheckbox = new JCheckBox("Remember me");
        rememberMeCheckbox.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        rememberMeCheckbox.setForeground(TEXT_COLOR);
        rememberMeCheckbox.setBackground(BACKGROUND_COLOR);
        rememberMeCheckbox.setOpaque(false);
        rememberMeCheckbox.setFocusPainted(false);

        // Setup status label
        statusLabel = new JLabel();
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        statusLabel.setHorizontalAlignment(JLabel.CENTER);
        statusLabel.setForeground(TEXT_COLOR);

        // Setup card layout for possible animations/transitions
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        cardPanel.setOpaque(false);

        // Setup message timer
        messageTimer = new Timer(3000, e -> {
            statusLabel.setText("");
            statusLabel.setIcon(null);
        });
        messageTimer.setRepeats(false);
    }

    /**
     * Setup the panel layout
     */
    private void setupLayout() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
        setBackground(BACKGROUND_COLOR);

        // Create login form
        JPanel loginFormPanel = createLoginForm();
        cardPanel.add(loginFormPanel, "LOGIN");

        // Add card panel to main panel
        add(cardPanel, BorderLayout.CENTER);
    }

    /**
     * Setup event listeners
     */
    private void setupListeners() {
        // Password field should trigger login on Enter key
        passwordField.addActionListener(e -> performLogin());

        // Button actions
        loginButton.addActionListener(e -> performLogin());
        cancelButton.addActionListener(e -> System.exit(0));
    }

    /**
     * Loads an icon from the resources folder
     */
    private ImageIcon loadIcon(String iconName, int width, int height) {
        try {
            // Assuming icons are stored in a resources/icons folder
            URL iconUrl = getClass().getResource("/icons/" + iconName + ".png");
            if (iconUrl != null) {
                ImageIcon icon = new ImageIcon(iconUrl);
                // Resize the icon if needed
                if (width > 0 && height > 0) {
                    Image img = icon.getImage();
                    Image resizedImg = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
                    return new ImageIcon(resizedImg);
                }
                return icon;
            }
        } catch (Exception e) {
            System.err.println("Error loading icon: " + iconName);
            e.printStackTrace();
        }

        // Return a fallback icon if loading fails
        return createFallbackIcon(iconName);
    }

    /**
     * Creates a fallback icon if the icon file cannot be loaded
     */
    private ImageIcon createFallbackIcon(String iconName) {
        int size = 20;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(PRIMARY_COLOR);

        // Simple fallback designs
        switch (iconName) {
            case "user":
                g2.fillOval(6, 3, 8, 8); // head
                g2.fillOval(2, 13, 16, 7); // body
                break;
            case "lock":
                g2.fillRoundRect(4, 8, 12, 10, 3, 3); // body
                g2.drawRoundRect(6, 3, 8, 8, 4, 4); // top
                g2.fillRoundRect(6, 3, 8, 8, 4, 4); // top
                break;
            case "car":
                g2.fillRoundRect(2, 8, 16, 6, 3, 3);
                g2.fillRect(5, 5, 10, 5);
                g2.setColor(Color.BLACK);
                g2.fillOval(4, 12, 4, 4);
                g2.fillOval(12, 12, 4, 4);
                break;
            case "success":
                g2.setColor(SUCCESS_COLOR);
                g2.setStroke(new BasicStroke(2));
                g2.drawLine(3, 8, 7, 12);
                g2.drawLine(7, 12, 13, 4);
                break;
            case "error":
                g2.setColor(ERROR_COLOR);
                g2.setStroke(new BasicStroke(2));
                g2.drawLine(4, 4, 12, 12);
                g2.drawLine(12, 4, 4, 12);
                break;
            default:
                g2.drawRect(2, 2, 16, 16);
        }

        g2.dispose();
        return new ImageIcon(image);
    }

    /**
     * Creates the map of default users with thread-safe implementation
     */
    private static Map<String, UserCredential> createDefaultUsers() {
        Map<String, UserCredential> users = new ConcurrentHashMap<>();
        users.put("admin", new UserCredential("admin123", UserRole.ADMIN));
        users.put("user1", new UserCredential("user123", UserRole.USER));
        users.put("user2", new UserCredential("user234", UserRole.USER));
        return users;
    }

    /**
     * Creates the complete login form
     */
    private JPanel createLoginForm() {
        JPanel panel = new JPanel(new BorderLayout(20, 20));
        panel.setOpaque(false);

        // Add header with logo
        panel.add(createHeaderPanel(), BorderLayout.NORTH);

        // Create main content with form fields
        JPanel contentPanel = new JPanel(new BorderLayout(15, 15));
        contentPanel.setOpaque(false);
        contentPanel.add(createFormPanel(), BorderLayout.CENTER);
        contentPanel.add(statusLabel, BorderLayout.SOUTH);
        panel.add(contentPanel, BorderLayout.CENTER);

        // Add button panel
        panel.add(createButtonPanel(), BorderLayout.SOUTH);

        return panel;
    }

    /**
     * Creates the header panel with logo and title
     */
    /**
     * Creates the header panel with logo and title
     */
    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 10));
        panel.setOpaque(false);

        // Create logo and title
        JLabel logoLabel = new JLabel(createCarIcon());
        JLabel titleLabel = new JLabel("Vehicle Rental System");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        titleLabel.setForeground(PRIMARY_COLOR);

        JLabel subtitleLabel = new JLabel("Sign in to access your account");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitleLabel.setForeground(TEXT_COLOR);

        // Group the title components
        JPanel textPanel = new JPanel(new GridLayout(2, 1, 0, 5));
        textPanel.setOpaque(false);
        textPanel.add(titleLabel);
        textPanel.add(subtitleLabel);

        panel.add(logoLabel, BorderLayout.WEST);
        panel.add(textPanel, BorderLayout.CENTER);

        // Add a separator line below the header
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(0, 0, 15, 0),
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(70, 73, 78))
        ));

        return panel;
    }

    /**
     * Creates the form panel with input fields
     */
    private JPanel createFormPanel() {
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 5, 8, 5);
        gbc.weightx = 1.0;

        // Configure username field
        JLabel usernameLabel = new JLabel("Username");
        usernameLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        usernameLabel.setForeground(TEXT_COLOR);

        usernameField.setPreferredSize(new Dimension(280, 40));
        JPanel usernamePanel = createInputPanel(usernameField, "user");

        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(usernameLabel, gbc);

        gbc.gridy = 1;
        formPanel.add(usernamePanel, gbc);

        // Configure password field
        JLabel passwordLabel = new JLabel("Password");
        passwordLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        passwordLabel.setForeground(TEXT_COLOR);

        passwordField.setPreferredSize(new Dimension(280, 40));
        JPanel passwordPanel = createInputPanel(passwordField, "lock");

        gbc.gridy = 2;
        gbc.insets = new Insets(15, 5, 8, 5);
        formPanel.add(passwordLabel, gbc);

        gbc.gridy = 3;
        gbc.insets = new Insets(8, 5, 8, 5);
        formPanel.add(passwordPanel, gbc);

        // Add remember me checkbox
        gbc.gridy = 4;
        gbc.insets = new Insets(10, 5, 8, 5);
        formPanel.add(rememberMeCheckbox, gbc);

        return formPanel;
    }

    /**
     * Creates the button panel with login and cancel buttons
     */
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 5, 5, 5);
        gbc.weightx = 1.0;

        // Configure login button
        loginButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        loginButton.setForeground(Color.WHITE);
        loginButton.setPreferredSize(new Dimension(120, 40));

        // Configure cancel button
        cancelButton.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        cancelButton.setForeground(Color.WHITE);
        cancelButton.setPreferredSize(new Dimension(120, 40));

        // Add buttons to panel with proper layout
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.weightx = 0.7;
        panel.add(loginButton, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.3;
        gbc.insets = new Insets(10, 10, 5, 5);
        panel.add(cancelButton, gbc);

        // Add forgot password link
        JLabel forgotPasswordLabel = createForgotPasswordLink();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(15, 5, 5, 5);
        panel.add(forgotPasswordLabel, gbc);

        return panel;
    }

    /**
     * Creates the forgot password link with hover effects
     */
    private JLabel createForgotPasswordLink() {
        JLabel forgotPasswordLabel = new JLabel("Forgot password?");
        forgotPasswordLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        forgotPasswordLabel.setForeground(SECONDARY_COLOR);
        forgotPasswordLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));

        forgotPasswordLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                JOptionPane.showMessageDialog(
                        LoginPanel.this,
                        "Please contact your system administrator to reset your password.",
                        "Password Recovery",
                        JOptionPane.INFORMATION_MESSAGE
                );
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                forgotPasswordLabel.setText("<html><u>Forgot password?</u></html>");
                forgotPasswordLabel.setForeground(SECONDARY_COLOR.brighter());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                forgotPasswordLabel.setText("Forgot password?");
                forgotPasswordLabel.setForeground(SECONDARY_COLOR);
            }
        });

        return forgotPasswordLabel;
    }

    /**
     * Creates a styled text field with placeholder support
     */
    /**
     * Creates a styled text field with placeholder support for dark mode
     */
    private JTextField createStyledTextField(String placeholder) {
        JTextField textField = new PlaceholderTextField(placeholder);
        textField.setBackground(new Color(45, 47, 51));
        textField.setForeground(TEXT_COLOR);
        textField.setCaretColor(TEXT_COLOR);

        textField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        textField.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        // Add focus listener for highlight effect
        textField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                textField.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(PRIMARY_COLOR, 2, true),
                        BorderFactory.createEmptyBorder(4, 9, 4, 9)
                ));
            }

            @Override
            public void focusLost(FocusEvent e) {
                textField.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
                        BorderFactory.createEmptyBorder(5, 10, 5, 10)
                ));
            }
        });

        return textField;
    }

    /**
     * Custom text field with placeholder support
     */
    /**
     * Custom text field with placeholder support for dark mode
     */
    private class PlaceholderTextField extends JTextField {
        private final String placeholder;

        public PlaceholderTextField(String placeholder) {
            this.placeholder = placeholder;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            // Draw placeholder if empty
            if (getText().isEmpty() && !hasFocus()) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setColor(new Color(150, 150, 150, 180)); // Lighter gray for dark mode
                g2.setFont(getFont());
                g2.drawString(placeholder, 10, getHeight() / 2 + g2.getFontMetrics().getAscent() / 2 - 2);
                g2.dispose();
            }
        }
    }

    /**
     * Creates a styled password field with placeholder support
     */
    /**
     * Creates a styled password field with placeholder support for dark mode
     */
    private JPasswordField createStyledPasswordField(String placeholder) {
        JPasswordField passwordField = new PlaceholderPasswordField(placeholder);
        passwordField.setBackground(new Color(45, 47, 51));
        passwordField.setForeground(TEXT_COLOR);
        passwordField.setCaretColor(TEXT_COLOR);

        passwordField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        passwordField.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        // Add focus listener for highlight effect
        passwordField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                passwordField.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(PRIMARY_COLOR, 2, true),
                        BorderFactory.createEmptyBorder(4, 9, 4, 9)
                ));
            }

            @Override
            public void focusLost(FocusEvent e) {
                passwordField.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
                        BorderFactory.createEmptyBorder(5, 10, 5, 10)
                ));
            }
        });

        return passwordField;
    }

    /**
     * Custom password field with placeholder support
     */
    private class PlaceholderPasswordField extends JPasswordField {
        private final String placeholder;

        public PlaceholderPasswordField(String placeholder) {
            this.placeholder = placeholder;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            // Draw placeholder if empty
            if (getPassword().length == 0 && !hasFocus()) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setColor(new Color(150, 150, 150));
                g2.setFont(getFont());
                g2.drawString(placeholder, 10, getHeight() / 2 + g2.getFontMetrics().getAscent() / 2 - 2);
                g2.dispose();
            }
        }
    }

    /**
     * Creates a styled button with gradient background
     */
    private JButton createStyledButton(String text, Color backgroundColor) {
        JButton button = new GradientButton(text, backgroundColor);
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        return button;
    }

    /**
     * Custom button with gradient background
     */
    /**
     * Custom button with enhanced gradient background
     */
    private class GradientButton extends JButton {
        private final Color baseColor;
        private boolean isHovered = false;

        public GradientButton(String text, Color baseColor) {
            super(text);
            this.baseColor = baseColor;

            // Add hover effect
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    isHovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    isHovered = false;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Create a more dramatic gradient for dark mode
            Color startColor = isHovered ?
                    new Color(Math.min(baseColor.getRed() + 20, 255),
                            Math.min(baseColor.getGreen() + 20, 255),
                            Math.min(baseColor.getBlue() + 20, 255)) :
                    baseColor;

            Color endColor = new Color(
                    Math.max(baseColor.getRed() - 40, 0),
                    Math.max(baseColor.getGreen() - 40, 0),
                    Math.max(baseColor.getBlue() - 40, 0)
            );

            // Paint background with enhanced gradient
            GradientPaint gradient = new GradientPaint(
                    0, 0, startColor,
                    0, getHeight(), endColor
            );
            g2.setPaint(gradient);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));

            // Add subtle highlight at top edge for 3D effect
            if (!isHovered) {
                g2.setColor(new Color(255, 255, 255, 30));
                g2.setStroke(new BasicStroke(1.5f));
                g2.draw(new RoundRectangle2D.Float(1, 1, getWidth()-2, 2, 5, 5));
            }

            // Paint text with slight shadow for better readability
            FontMetrics metrics = g2.getFontMetrics(getFont());
            int x = (getWidth() - metrics.stringWidth(getText())) / 2;
            int y = ((getHeight() - metrics.getHeight()) / 2) + metrics.getAscent();

            // Add text shadow
            g2.setFont(getFont());
            g2.setColor(new Color(0, 0, 0, 50));
            g2.drawString(getText(), x+1, y+1);

            // Draw actual text
            g2.setColor(getForeground());
            g2.drawString(getText(), x, y);
            g2.dispose();
        }
    }

    /**
     * Creates a panel with an icon and input field
     */
    private JPanel createInputPanel(JComponent inputField, String iconName) {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        // Create icon label
        JLabel iconLabel = new JLabel(createIcon(iconName));
        iconLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));

        panel.add(iconLabel, BorderLayout.WEST);
        panel.add(inputField, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Creates an icon based on the provided name
     */
    /**
     * Creates an icon based on the provided name using Flaticon resources
     */
    private ImageIcon createIcon(String iconName) {
        // Load icon from Flaticon resources
        return loadIcon(iconName, 20, 20);
    }

    /**
     * Creates a car icon for the header using Flaticon resources
     */
    private ImageIcon createCarIcon() {
        // Load car icon from Flaticon resources
        return loadIcon("car", 50, 40);
    }

    /**
     * Creates a status icon (checkmark or X) based on the provided color
     */
    private ImageIcon createStatusIcon(Color color) {
        String iconName = color.equals(ERROR_COLOR) ? "error" : "success";
        return loadIcon(iconName, 16, 16);
    }

    /**
     * Perform login validation and process
     */
    private void performLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            showStatus("Username and password cannot be empty", "error");
            return;
        }

        // Check credentials with improved security
        UserCredential credential = USERS.get(username);
        if (credential != null && credential.verifyPassword(password)) {
            // Login successful
            showStatus("Login successful!", "success");

            // Delay login processing to show success message
            Timer loginTimer = new Timer(1000, e -> completeLogin(username, credential.getRole()));
            loginTimer.setRepeats(false);
            loginTimer.start();
        } else {
            // Login failed
            showStatus("Invalid username or password", "error");
            passwordField.setText("");
            passwordField.requestFocus();
        }
    }

    /**
     * Complete the login process
     */
    private void completeLogin(String username, UserRole role) {
        // Update user session
        UserSession.getInstance().login(username, role);

        // Notify listener if registered
        if (loginListener != null) {
            loginListener.onLoginSuccess(username, role);
        }
    }

    /**
     * Displays a status message with appropriate icon
     */
    private void showStatus(String message, String type) {
        if (messageTimer.isRunning()) {
            messageTimer.stop();
        }

        switch (type) {
            case "error":
                statusLabel.setForeground(ERROR_COLOR);
                statusLabel.setIcon(createStatusIcon(ERROR_COLOR));
                break;
            case "success":
                statusLabel.setForeground(SUCCESS_COLOR);
                statusLabel.setIcon(createStatusIcon(SUCCESS_COLOR));
                break;
            default:
                statusLabel.setForeground(TEXT_COLOR);
                statusLabel.setIcon(null);
                break;
        }

        statusLabel.setText(message);
        messageTimer.start();
    }

    /**
     * Sets the login listener for handling successful login events
     */
    public void setLoginListener(LoginListener listener) {
        this.loginListener = listener;
    }

    /**
     * Add a new user to the system
     */
    public static void addUser(String username, String password, UserRole role) {
        USERS.put(username, new UserCredential(password, role));
    }

    /**
     * Remove a user from the system
     */
    public static void removeUser(String username) {
        USERS.remove(username);
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
     * Class to store user credentials and role with improved security
     */
    private static class UserCredential {
        private final String password;
        private final UserRole role;

        public UserCredential(String password, UserRole role) {
            this.password = password;
            this.role = role;
        }

        /**
         * Verify if the provided password matches the stored password
         * In a real application, this would use proper password hashing
         */
        public boolean verifyPassword(String inputPassword) {
            return password != null && password.equals(inputPassword);
        }

        public UserRole getRole() {
            return role;
        }
    }

    /**
     * Singleton class to manage user session information with improved thread safety
     */
    public static class UserSession {
        private static volatile UserSession instance;
        private static final Object LOCK = new Object();

        private String username;
        private UserRole role;
        private boolean loggedIn = false;
        private long loginTime;

        private UserSession() {
            // Private constructor to enforce singleton pattern
        }

        /**
         * Gets the single instance of UserSession with double-check locking
         */
        public static UserSession getInstance() {
            if (instance == null) {
                synchronized (LOCK) {
                    if (instance == null) {
                        instance = new UserSession();
                    }
                }
            }
            return instance;
        }

        /**
         * Starts a new user session
         */
        public void login(String username, UserRole role) {
            synchronized (LOCK) {
                this.username = username;
                this.role = role;
                this.loggedIn = true;
                this.loginTime = System.currentTimeMillis();
            }
        }

        /**
         * Ends the current user session
         */
        public void logout() {
            synchronized (LOCK) {
                this.username = null;
                this.role = null;
                this.loggedIn = false;
                this.loginTime = 0;
            }
        }

        /**
         * Checks if user is logged in
         */
        public boolean isLoggedIn() {
            return loggedIn;
        }

        /**
         * Checks if current user is an administrator
         */
        public boolean isAdmin() {
            return loggedIn && role == UserRole.ADMIN;
        }

        /**
         * Gets the current username
         */
        public String getUsername() {
            return username;
        }

        /**
         * Gets the current user role
         */
        public UserRole getRole() {
            return role;
        }

        /**
         * Gets the login time
         */
        public long getLoginTime() {
            return loginTime;
        }

        /**
         * Gets session duration in milliseconds
         */
        public long getSessionDuration() {
            return loggedIn ? System.currentTimeMillis() - loginTime : 0;
        }
    }
}