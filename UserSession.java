
/**
 * Singleton class to manage user session information
 */
public class UserSession {
    private static UserSession instance;
    
    private String username;
    private LoginPanel.UserRole role;
    private boolean loggedIn = false;
    
    private UserSession() {
        // Private constructor to enforce singleton pattern
    }
    
    /**
     * Gets the single instance of UserSession
     */
    public static synchronized UserSession getInstance() {
        if (instance == null) {
            instance = new UserSession();
        }
        return instance;
    }
    
    /**
     * Starts a new user session
     */
    public void login(String username, LoginPanel.UserRole role) {
        this.username = username;
        this.role = role;
        this.loggedIn = true;
    }
    
    /**
     * Ends the current user session
     */
    public void logout() {
        this.username = null;
        this.role = null;
        this.loggedIn = false;
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
        return loggedIn && role == LoginPanel.UserRole.ADMIN;
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
    public LoginPanel.UserRole getRole() {
        return role;
    }
}