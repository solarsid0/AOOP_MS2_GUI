package DAOs;

import Models.UserAuthenticationModel;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Data Access Object for user authentication operations
 * Enhanced with Manila timezone support and session management
 */
public class UserAuthenticationDAO {
    
    // Manila timezone constant
    public static final ZoneId MANILA_TIMEZONE = ZoneId.of("Asia/Manila");
    
    // Database connection constants
    private static final String DB_URL = "jdbc:mysql://localhost:3306/payrollsystem_db";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "Mmdc_2025*";
    
    // Get database connection
    private Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        
        // Set connection timezone to Manila
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("SET time_zone = '+08:00'");
        }
        
        return conn;
    }
    
    // MANILA TIMEZONE UTILITIES
    
    /**
     * Gets current Manila time
     * @return LocalDateTime in Manila timezone
     */
    public static LocalDateTime getManilaTime() {
        return ZonedDateTime.now(MANILA_TIMEZONE).toLocalDateTime();
    }
    
    /**
     * Converts any LocalDateTime to Manila timezone
     * @param dateTime LocalDateTime to convert
     * @return LocalDateTime in Manila timezone
     */
    public static LocalDateTime toManilaTime(LocalDateTime dateTime) {
        if (dateTime == null) return null;
        return dateTime.atZone(ZoneId.systemDefault())
                      .withZoneSameInstant(MANILA_TIMEZONE)
                      .toLocalDateTime();
    }
    
    /**
     * Creates a Manila timezone timestamp for database operations
     * @return Timestamp in Manila timezone
     */
    public static Timestamp getManilaTimestamp() {
        return Timestamp.valueOf(getManilaTime());
    }
    
    // PASSWORD HASHING UTILITIES (since PasswordHasher is not available)
    
    /**
     * Hash password with salt using SHA-256
     */
    private String hashPassword(String password) {
        try {
            String salt = generateSalt();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(salt.getBytes());
            byte[] hash = digest.digest(password.getBytes());
            String hashedPassword = Base64.getEncoder().encodeToString(hash);
            return salt + ":" + hashedPassword; // Store salt:hash format
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Error hashing password: " + e.getMessage());
            return null;
        }
    }
    
        /**
         * Verify password against stored hash - handles plain text passwords
         */
        private boolean verifyPassword(String plainPassword, String storedHash) {
            try {
                if (storedHash == null || plainPassword == null) {
                    return false;
                }

                // Check if stored hash is in new salt:hash format
                if (storedHash.contains(":")) {
                    // New format - use existing salt:hash verification
                    String[] parts = storedHash.split(":");
                    if (parts.length != 2) {
                        return false;
                    }

                    String salt = parts[0];
                    String hash = parts[1];

                    MessageDigest digest = MessageDigest.getInstance("SHA-256");
                    digest.update(salt.getBytes());
                    byte[] newHash = digest.digest(plainPassword.getBytes());
                    String newHashString = Base64.getEncoder().encodeToString(newHash);

                    return hash.equals(newHashString);
                } else {
                    // Legacy format - check if it's plain text (for your current data)
                    return plainPassword.equals(storedHash);
                }

            } catch (NoSuchAlgorithmException e) {
                System.err.println("Error verifying password: " + e.getMessage());
                return false;
            }
        }
    
    /**
     * Generate salt for password hashing
     */
    private String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] saltBytes = new byte[16];
        random.nextBytes(saltBytes);
        return Base64.getEncoder().encodeToString(saltBytes);
    }
    
    /**
     * Simple password validation (private helper)
     */
    private boolean validatePassword(String password) {
        return password != null && password.length() >= 8;
    }
    
    /**
     * Get password requirements message
     */
    private String getPasswordRequirements() {
        return "Password must be at least 8 characters long";
    }
    
    // AUTHENTICATION METHODS WITH MANILA TIMEZONE
    
    /**
     * Authenticates a user with email and password using Manila timezone
     * @param email User's email
     * @param password Plain text password
     * @return UserAuthenticationModel if successful, null if failed
     */
    public UserAuthenticationModel authenticateUser(String email, String password) {
        if (email == null || password == null) {
            return null;
        }
        
        String sql = """
            SELECT e.employeeId, e.email, e.passwordHash, e.userRole, e.status, 
                   e.lastLogin, e.firstName, e.lastName, p.position, p.department
            FROM employee e 
            LEFT JOIN position p ON e.positionId = p.positionId 
            WHERE e.email = ?
            """;
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                String storedHash = rs.getString("passwordHash");
                
                // Verify password
                if (verifyPassword(password, storedHash)) {
                    UserAuthenticationModel user = extractUserFromResultSet(rs);
                    
                    // Check if account is active
                    if (isAccountActive(user)) {
                        // Update last login in database with Manila time
                        updateLastLoginManilaTime(user.getEmployeeId());
                        
                        // Reset login attempts on successful login
                        resetLoginAttempts(user.getEmployeeId());
                        
                        // Log successful authentication
                        logAuthenticationAttempt(email, true, "Successful login");
                        
                        return user;
                    } else {
                        logAuthenticationAttempt(email, false, "Account inactive");
                    }
                } else {
                    // Increment login attempts on failed password
                    incrementLoginAttempts(email);
                    logAuthenticationAttempt(email, false, "Invalid password");
                }
            } else {
                logAuthenticationAttempt(email, false, "Email not found");
            }
            
        } catch (SQLException e) {
            System.err.println("Error authenticating user: " + e.getMessage());
            logAuthenticationAttempt(email, false, "Database error: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Check if account is active
     */
    private boolean isAccountActive(UserAuthenticationModel user) {
        String status = user.getStatus();
        return status != null && !"Terminated".equalsIgnoreCase(status);
    }
    
    /**
     * Gets user information by employee ID with position details
     * @param employeeId The employee ID
     * @return UserAuthenticationModel or null if not found
     */
    public UserAuthenticationModel getUserById(int employeeId) {
        String sql = """
            SELECT e.employeeId, e.email, e.passwordHash, e.userRole, e.status, 
                   e.lastLogin, e.firstName, e.lastName, p.position, p.department
            FROM employee e 
            LEFT JOIN position p ON e.positionId = p.positionId 
            WHERE e.employeeId = ?
            """;
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, employeeId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return extractUserFromResultSet(rs);
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting user by ID: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Gets user information by email with position details
     * @param email The user's email
     * @return UserAuthenticationModel or null if not found
     */
    public UserAuthenticationModel getUserByEmail(String email) {
        String sql = """
            SELECT e.employeeId, e.email, e.passwordHash, e.userRole, e.status, 
                   e.lastLogin, e.firstName, e.lastName, p.position, p.department
            FROM employee e 
            LEFT JOIN position p ON e.positionId = p.positionId 
            WHERE e.email = ?
            """;
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return extractUserFromResultSet(rs);
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting user by email: " + e.getMessage());
        }
        return null;
    }
    
    // SESSION MANAGEMENT WITH MANILA TIMEZONE
    
    /**
     * Updates the last login timestamp for a user with Manila time
     * @param employeeId The employee ID
     * @return true if successful, false otherwise
     */
    public boolean updateLastLoginManilaTime(int employeeId) {
        String sql = "UPDATE employee SET lastLogin = ? WHERE employeeId = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setTimestamp(1, getManilaTimestamp());
            pstmt.setInt(2, employeeId);
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Error updating last login with Manila time: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Legacy method for backward compatibility
     * @param employeeId
     * @return 
     */
    public boolean updateLastLogin(int employeeId) {
        return updateLastLoginManilaTime(employeeId);
    }
    
    /**
     * Creates a session record in database (if you want to persist sessions)
     * @param employeeId Employee ID
     * @param sessionToken Session token
     * @param durationMinutes Session duration in minutes
     * @return true if session was created successfully
     */
    public boolean createSession(int employeeId, String sessionToken, int durationMinutes) {
        LocalDateTime manilaTime = getManilaTime();
        LocalDateTime expiryTime = manilaTime.plusMinutes(durationMinutes);
        
        // You could implement session persistence here if needed
        System.out.println(String.format(
            "Session created for employee %d: token=%s, created=%s, expires=%s", 
            employeeId, sessionToken, manilaTime, expiryTime));
        
        return true;
    }
    
    /**
     * Validates user session with Manila timezone
     * @param employeeId Employee ID
     * @param sessionToken Session token
     * @return true if session is valid, false otherwise
     */
    public boolean validateSessionWithManilaTime(int employeeId, String sessionToken) {
        UserAuthenticationModel user = getUserById(employeeId);
        
        if (user == null || !isAccountActive(user)) {
            return false;
        }
        
        // Check if user has been logged in recently (within last 8 hours)
        if (user.getLastLogin() != null) {
            LocalDateTime lastLoginManilaTime = toManilaTime(user.getLastLogin().toLocalDateTime());
            LocalDateTime cutoffTime = getManilaTime().minusHours(8);
            
            return lastLoginManilaTime.isAfter(cutoffTime);
        }
        
        return false;
    }
    
    /**
     * Legacy method for backward compatibility
     * @param employeeId
     * @param sessionToken
     * @return 
     */
    public boolean validateSession(int employeeId, String sessionToken) {
        return validateSessionWithManilaTime(employeeId, sessionToken);
    }
    
    /**
     * Extends user session with Manila timezone
     * @param employeeId Employee ID
     * @param additionalMinutes Additional minutes to extend session
     * @return true if session was extended
     */
    public boolean extendSession(int employeeId, int additionalMinutes) {
        return updateLastLoginManilaTime(employeeId);
    }
    
    /**
     * Invalidates user session
     * @param employeeId Employee ID
     * @param sessionToken Session token
     * @return true if session was invalidated
     */
    public boolean invalidateSession(int employeeId, String sessionToken) {
        logAuthenticationAttempt(
            getUserById(employeeId) != null ? getUserById(employeeId).getEmail() : "Unknown", 
            true, 
            "User logged out");
        
        return true;
    }
    
    // LOGIN ATTEMPT TRACKING
    
    /**
     * Increments login attempts for an email address
     * @param email Email address
     */
    private void incrementLoginAttempts(String email) {
        System.out.println("Login attempt failed for email: " + email + " at " + getManilaTime());
    }
    
    /**
     * Resets login attempts for an employee
     * @param employeeId Employee ID
     */
    private void resetLoginAttempts(int employeeId) {
        System.out.println("Login attempts reset for employee: " + employeeId + " at " + getManilaTime());
    }
    
    /**
     * Checks if an account is locked due to failed login attempts
     * @param email Email address
     * @return true if account is locked
     */
    public boolean isAccountLocked(String email) {
        return false; // Implement based on your requirements
    }
    
    // PASSWORD MANAGEMENT WITH MANILA TIMEZONE
    
    /**
     * Updates user's password with Manila timezone tracking
     * @param employeeId The employee ID
     * @param newPassword The new plain text password
     * @return true if successful, false otherwise
     */
    public boolean updatePassword(int employeeId, String newPassword) {
        if (!validatePassword(newPassword)) {
            throw new IllegalArgumentException(getPasswordRequirements());
        }
        
        String hashedPassword = hashPassword(newPassword);
        if (hashedPassword == null) {
            return false;
        }
        
        String sql = "UPDATE employee SET passwordHash = ?, updatedAt = ? WHERE employeeId = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, hashedPassword);
            pstmt.setTimestamp(2, getManilaTimestamp());
            pstmt.setInt(3, employeeId);
            
            boolean success = pstmt.executeUpdate() > 0;
            
            if (success) {
                logAuthenticationAttempt(
                    getUserById(employeeId) != null ? getUserById(employeeId).getEmail() : "Unknown",
                    true,
                    "Password updated");
            }
            
            return success;
            
        } catch (SQLException e) {
            System.err.println("Error updating password: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Validates password strength and requirements
     * @param password Password to validate
     * @return true if password meets requirements
     */
    public boolean isPasswordValid(String password) {
        return validatePassword(password);
    }
    
    // USER MANAGEMENT WITH MANILA TIMEZONE
    
    /**
     * Creates a new user account with Manila timezone tracking
     * @param email User's email
     * @param password Plain text password
     * @param userRole User's role
     * @param firstName First name
     * @param lastName Last name
     * @param positionId Position ID
     * @return true if successful, false otherwise
     */
    public boolean createUser(String email, String password, String userRole, String firstName, String lastName, int positionId) {
        if (!validatePassword(password)) {
            throw new IllegalArgumentException(getPasswordRequirements());
        }
        
        String hashedPassword = hashPassword(password);
        if (hashedPassword == null) {
            return false;
        }
        
        String sql = """
            INSERT INTO employee (firstName, lastName, email, passwordHash, userRole, positionId, 
                                 birthDate, basicSalary, hourlyRate, status, createdAt, updatedAt) 
            VALUES (?, ?, ?, ?, ?, ?, '1990-01-01', 25000.00, 120.00, 'Probationary', ?, ?)
            """;
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            Timestamp manilaTime = getManilaTimestamp();
            
            pstmt.setString(1, firstName);
            pstmt.setString(2, lastName);
            pstmt.setString(3, email);
            pstmt.setString(4, hashedPassword);
            pstmt.setString(5, userRole);
            pstmt.setInt(6, positionId);
            pstmt.setTimestamp(7, manilaTime);
            pstmt.setTimestamp(8, manilaTime);
            
            boolean success = pstmt.executeUpdate() > 0;
            
            if (success) {
                logAuthenticationAttempt(email, true, "User account created");
            }
            
            return success;
            
        } catch (SQLException e) {
            System.err.println("Error creating user: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Deactivates a user account with Manila timezone tracking
     * @param employeeId The employee ID
     * @return true if successful, false otherwise
     */
    public boolean deactivateUser(int employeeId) {
        String sql = "UPDATE employee SET status = 'Terminated', updatedAt = ? WHERE employeeId = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setTimestamp(1, getManilaTimestamp());
            pstmt.setInt(2, employeeId);
            
            boolean success = pstmt.executeUpdate() > 0;
            
            if (success) {
                UserAuthenticationModel user = getUserById(employeeId);
                logAuthenticationAttempt(
                    user != null ? user.getEmail() : "Unknown",
                    true,
                    "User account deactivated");
            }
            
            return success;
            
        } catch (SQLException e) {
            System.err.println("Error deactivating user: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Activates a user account with Manila timezone tracking
     * @param employeeId The employee ID
     * @param newStatus New status (Regular or Probationary)
     * @return true if successful, false otherwise
     */
    public boolean activateUser(int employeeId, String newStatus) {
        if (!"Regular".equals(newStatus) && !"Probationary".equals(newStatus)) {
            throw new IllegalArgumentException("Status must be 'Regular' or 'Probationary'");
        }
        
        String sql = "UPDATE employee SET status = ?, updatedAt = ? WHERE employeeId = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, newStatus);
            pstmt.setTimestamp(2, getManilaTimestamp());
            pstmt.setInt(3, employeeId);
            
            boolean success = pstmt.executeUpdate() > 0;
            
            if (success) {
                UserAuthenticationModel user = getUserById(employeeId);
                logAuthenticationAttempt(
                    user != null ? user.getEmail() : "Unknown",
                    true,
                    "User account activated with status: " + newStatus);
            }
            
            return success;
            
        } catch (SQLException e) {
            System.err.println("Error activating user: " + e.getMessage());
        }
        return false;
    }
    
    // VALIDATION METHODS
    
    /**
     * Checks if an email already exists in the database
     * @param email The email to check
     * @return true if email exists, false otherwise
     */
    public boolean emailExists(String email) {
        String sql = "SELECT COUNT(*) FROM employee WHERE email = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            
        } catch (SQLException e) {
            System.err.println("Error checking email existence: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Validates if employee can submit leave requests today
     * @param employeeId Employee ID
     * @return true if employee can submit leave requests
     */
    public boolean canSubmitLeaveRequest(int employeeId) {
        UserAuthenticationModel user = getUserById(employeeId);
        // All active employees can submit leave requests
        
        return !(user == null || !isAccountActive(user)); 
    }
    
    /**
     * Validates if employee can submit overtime requests today
     * @param employeeId Employee ID
     * @return true if employee can submit overtime requests (rank-and-file only)
     */
    public boolean canSubmitOvertimeRequest(int employeeId) {
        String sql = """
            SELECT COUNT(*) > 0
            FROM employee e 
            JOIN position p ON e.positionId = p.positionId 
            WHERE e.employeeId = ?
            AND e.status != 'Terminated'
            AND (LOWER(p.department) = 'rank-and-file' 
                 OR LOWER(p.position) LIKE '%rank-and-file%')
            """;
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, employeeId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getBoolean(1);
            }
            
        } catch (SQLException e) {
            System.err.println("Error checking overtime request eligibility: " + e.getMessage());
        }
        return false;
    }
    
    // REPORTING AND ANALYTICS
    
    /**
     * Gets authentication statistics
     * @return Map with authentication metrics
     */
    public Map<String, Object> getAuthenticationStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        String totalUsersSQL = "SELECT COUNT(*) FROM employee";
        String activeUsersSQL = "SELECT COUNT(*) FROM employee WHERE status != 'Terminated'";
        String usersByRoleSQL = """
            SELECT userRole, COUNT(*) as count 
            FROM employee 
            WHERE status != 'Terminated' 
            GROUP BY userRole
            """;
        String recentLoginsSQL = """
            SELECT COUNT(*) 
            FROM employee 
            WHERE lastLogin >= ?
            """;
        
        try (Connection conn = getConnection()) {
            // Total users
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(totalUsersSQL)) {
                if (rs.next()) {
                    stats.put("total_users", rs.getInt(1));
                }
            }
            
            // Active users
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(activeUsersSQL)) {
                if (rs.next()) {
                    stats.put("active_users", rs.getInt(1));
                }
            }
            
            // Users by role
            Map<String, Integer> roleStats = new HashMap<>();
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(usersByRoleSQL)) {
                while (rs.next()) {
                    roleStats.put(rs.getString("userRole"), rs.getInt("count"));
                }
            }
            stats.put("users_by_role", roleStats);
            
            // Recent logins
            LocalDateTime yesterday = getManilaTime().minusHours(24);
            try (PreparedStatement pstmt = conn.prepareStatement(recentLoginsSQL)) {
                pstmt.setTimestamp(1, Timestamp.valueOf(yesterday));
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    stats.put("recent_logins_24h", rs.getInt(1));
                }
            }
            
            stats.put("generated_at", getManilaTime().toString());
            stats.put("timezone", "Asia/Manila");
            
        } catch (SQLException e) {
            System.err.println("Error getting authentication statistics: " + e.getMessage());
        }
        
        return stats;
    }
    
    /**
     * Gets list of users by role with position information
     * @param userRole User role to filter by
     * @return List of users with specified role
     */
    public List<Map<String, Object>> getUsersByRole(String userRole) {
        List<Map<String, Object>> users = new ArrayList<>();
        String sql = """
            SELECT e.employeeId, e.firstName, e.lastName, e.email, e.userRole, 
                   e.status, e.lastLogin, e.createdAt,
                   p.position, p.department
            FROM employee e
            LEFT JOIN position p ON e.positionId = p.positionId
            WHERE e.userRole = ?
            ORDER BY e.lastName, e.firstName
            """;
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, userRole);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Map<String, Object> user = new HashMap<>();
                user.put("employeeId", rs.getInt("employeeId"));
                user.put("firstName", rs.getString("firstName"));
                user.put("lastName", rs.getString("lastName"));
                user.put("email", rs.getString("email"));
                user.put("userRole", rs.getString("userRole"));
                user.put("status", rs.getString("status"));
                user.put("position", rs.getString("position"));
                user.put("department", rs.getString("department"));
                
                Timestamp lastLogin = rs.getTimestamp("lastLogin");
                if (lastLogin != null) {
                    user.put("lastLogin", toManilaTime(lastLogin.toLocalDateTime()));
                }
                
                Timestamp createdAt = rs.getTimestamp("createdAt");
                if (createdAt != null) {
                    user.put("createdAt", toManilaTime(createdAt.toLocalDateTime()));
                }
                
                users.add(user);
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting users by role: " + e.getMessage());
        }
        
        return users;
    }
    
    // AUDIT AND LOGGING
    
    /**
     * Logs authentication attempt for audit purposes with Manila timezone
     * @param email Email used for login
     * @param success Whether login was successful
     * @param details Additional details about the attempt
     */
    public void logAuthenticationAttempt(String email, boolean success, String details) {
        try {
            LocalDateTime manilaTime = getManilaTime();
            String logMessage = String.format("[AUTH] %s - Email: %s, Success: %s, Details: %s",
                manilaTime, email, success, details);
            System.out.println(logMessage);
            
        } catch (Exception e) {
            System.err.println("Error logging authentication attempt: " + e.getMessage());
        }
    }
    
    /**
     * Gets recent authentication logs
     * @param hours Number of hours to look back
     * @return List of authentication events
     */
    public List<Map<String, Object>> getRecentAuthenticationLogs(int hours) {
        List<Map<String, Object>> logs = new ArrayList<>();
        
        Map<String, Object> sampleLog = new HashMap<>();
        sampleLog.put("timestamp", getManilaTime());
        sampleLog.put("message", "Authentication logging would require audit_log table");
        logs.add(sampleLog);
        
        return logs;
    }
    
    /**
     * Extracts UserAuthenticationModel from ResultSet with Manila timezone handling
     * @param rs ResultSet from database query
     * @return UserAuthenticationModel object
     * @throws SQLException if database error occurs
     */
    private UserAuthenticationModel extractUserFromResultSet(ResultSet rs) throws SQLException {
        UserAuthenticationModel user = new UserAuthenticationModel();
        user.setEmployeeId(rs.getInt("employeeId"));
        user.setEmail(rs.getString("email"));
        user.setPasswordHash(rs.getString("passwordHash"));
        user.setUserRole(rs.getString("userRole"));
        user.setStatus(rs.getString("status"));
        user.setFirstName(rs.getString("firstName"));
        user.setLastName(rs.getString("lastName"));
        
        // Handle lastLogin with Manila timezone conversion
        Timestamp lastLogin = rs.getTimestamp("lastLogin");
        if (lastLogin != null) {
            user.setLastLogin(lastLogin);
        }
        
        // Add position information if available
        try {
            String position = rs.getString("position");
            String department = rs.getString("department");
            user.setPosition(position);
            user.setDepartment(department);
        } catch (SQLException e) {
            // Position columns not available in this query - that's okay
        }
        
        return user;
    }
}