package Models;

import java.sql.Timestamp;
import java.time.*;
import java.util.UUID;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * UserAuthenticationModel with Manila timezone session handling
 * Enhanced security features and session management
 */
public class UserAuthenticationModel {
    private static final ZoneId MANILA_TIMEZONE = ZoneId.of("Asia/Manila");
    private static final int SESSION_TIMEOUT_MINUTES = 480; // 8 hours
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final int LOCKOUT_DURATION_MINUTES = 30;
    
    // User authentication fields
    private int employeeId;
    private String email;
    private String passwordHash;
    private String userRole;
    private String status;
    private Timestamp lastLogin;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    
    // Session management fields
    private String sessionId;
    private Timestamp sessionStartTime;
    private Timestamp sessionLastActivity;
    private Timestamp sessionExpireTime;
    private boolean sessionActive;
    private String ipAddress;
    private String userAgent;
    
    // Security fields
    private String salt;
    private int loginAttempts;
    private Timestamp lastFailedAttempt;
    private Timestamp accountLockedUntil;
    private boolean isAccountLocked;
    private String resetPasswordToken;
    private Timestamp resetPasswordExpiry;
    
    // Additional fields
    private String firstName;
    private String lastName;
    private String position;
    private String department;
    
    // Constructors
    public UserAuthenticationModel() {
        this.sessionActive = false;
        this.loginAttempts = 0;
        this.isAccountLocked = false;
        this.createdAt = Timestamp.valueOf(LocalDateTime.now(MANILA_TIMEZONE));
        this.updatedAt = this.createdAt;
    }
    
    public UserAuthenticationModel(int employeeId, String email, String userRole) {
        this();
        this.employeeId = employeeId;
        this.email = email;
        this.userRole = userRole;
        this.status = "Active";
    }
    
    // Manila timezone session handling
    
    /**
     * Start new session with Manila timezone
     * @param ipAddress
     * @param userAgent
     * @return 
     */
    public boolean startSession(String ipAddress, String userAgent) {
        try {
            if (isAccountLocked()) {
                return false;
            }
            
            LocalDateTime nowManila = LocalDateTime.now(MANILA_TIMEZONE);
            
            this.sessionId = generateSessionId();
            this.sessionStartTime = Timestamp.valueOf(nowManila);
            this.sessionLastActivity = this.sessionStartTime;
            this.sessionExpireTime = Timestamp.valueOf(nowManila.plusMinutes(SESSION_TIMEOUT_MINUTES));
            this.sessionActive = true;
            this.lastLogin = this.sessionStartTime;
            this.ipAddress = ipAddress;
            this.userAgent = userAgent;
            
            // Reset login attempts on successful login
            this.loginAttempts = 0;
            this.lastFailedAttempt = null;
            this.accountLockedUntil = null;
            
            updateTimestamp();
            return true;
        } catch (Exception e) {
            System.err.println("Error starting session: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Update session activity with Manila timezone
     * @return 
     */
    public boolean updateSessionActivity() {
        if (!sessionActive || sessionId == null) {
            return false;
        }
        
        LocalDateTime nowManila = LocalDateTime.now(MANILA_TIMEZONE);
        Timestamp now = Timestamp.valueOf(nowManila);
        
        // Check if session has expired
        if (now.after(sessionExpireTime)) {
            endSession();
            return false;
        }
        
        // Update last activity and extend expiry
        this.sessionLastActivity = now;
        this.sessionExpireTime = Timestamp.valueOf(nowManila.plusMinutes(SESSION_TIMEOUT_MINUTES));
        
        return true;
    }
    
    /**
     * End current session
     */
    public void endSession() {
        this.sessionActive = false;
        this.sessionId = null;
        this.sessionLastActivity = Timestamp.valueOf(LocalDateTime.now(MANILA_TIMEZONE));
        updateTimestamp();
    }
    
    /**
     * Check if session is valid
     * @return 
     */
    public boolean isSessionValid() {
        if (!sessionActive || sessionId == null || sessionExpireTime == null) {
            return false;
        }
        
        Timestamp now = Timestamp.valueOf(LocalDateTime.now(MANILA_TIMEZONE));
        return now.before(sessionExpireTime);
    }
    
    /**
     * Get session duration in minutes
     * @return 
     */
    public long getSessionDurationMinutes() {
        if (sessionStartTime == null || sessionLastActivity == null) {
            return 0;
        }
        
        Duration duration = Duration.between(
            sessionStartTime.toLocalDateTime(),
            sessionLastActivity.toLocalDateTime()
        );
        return duration.toMinutes();
    }
    
    /**
     * Get time until session expires
     * @return 
     */
    public long getTimeUntilExpiryMinutes() {
        if (!sessionActive || sessionExpireTime == null) {
            return 0;
        }
        
        LocalDateTime now = LocalDateTime.now(MANILA_TIMEZONE);
        LocalDateTime expiry = sessionExpireTime.toLocalDateTime();
        
        if (expiry.isBefore(now)) {
            return 0;
        }
        
        Duration duration = Duration.between(now, expiry);
        return duration.toMinutes();
    }
    
    // Authentication methods
    
    /**
     * Authenticate user with password
     * @param password
     * @return 
     */
    public boolean authenticate(String password) {
        if (isAccountLocked()) {
            return false;
        }
        
        if (verifyPassword(password)) {
            // Reset login attempts on successful authentication
            this.loginAttempts = 0;
            this.lastFailedAttempt = null;
            return true;
        } else {
            // Increment failed attempts
            recordFailedAttempt();
            return false;
        }
    }
    
    /**
     * Record failed login attempt
     */
    private void recordFailedAttempt() {
        this.loginAttempts++;
        this.lastFailedAttempt = Timestamp.valueOf(LocalDateTime.now(MANILA_TIMEZONE));
        
        // Lock account if max attempts exceeded
        if (this.loginAttempts >= MAX_LOGIN_ATTEMPTS) {
            lockAccount();
        }
        
        updateTimestamp();
    }
    
    /**
     * Lock user account
     */
    private void lockAccount() {
        this.isAccountLocked = true;
        this.accountLockedUntil = Timestamp.valueOf(
            LocalDateTime.now(MANILA_TIMEZONE).plusMinutes(LOCKOUT_DURATION_MINUTES)
        );
        endSession(); // End any active session
    }
    
    /**
     * Check if account is currently locked
     * @return 
     */
    public boolean isAccountLocked() {
        if (!isAccountLocked) {
            return false;
        }
        
        if (accountLockedUntil == null) {
            return true;
        }
        
        Timestamp now = Timestamp.valueOf(LocalDateTime.now(MANILA_TIMEZONE));
        if (now.after(accountLockedUntil)) {
            // Unlock account
            this.isAccountLocked = false;
            this.accountLockedUntil = null;
            this.loginAttempts = 0;
            updateTimestamp();
            return false;
        }
        
        return true;
    }
    
    /**
     * Manually unlock account
     */
    public void unlockAccount() {
        this.isAccountLocked = false;
        this.accountLockedUntil = null;
        this.loginAttempts = 0;
        updateTimestamp();
    }
    
    // Password management
    
    /**
     * Set password with salt and hashing
     * @param plainPassword
     * @return 
     */
    public boolean setPassword(String plainPassword) {
        try {
            this.salt = generateSalt();
            this.passwordHash = hashPassword(plainPassword, this.salt);
            updateTimestamp();
            return true;
        } catch (Exception e) {
            System.err.println("Error setting password: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Verify password against stored hash
     * @param plainPassword
     * @return 
     */
    public boolean verifyPassword(String plainPassword) {
        try {
            if (passwordHash == null || salt == null) {
                return false;
            }
            
            String hashedInput = hashPassword(plainPassword, salt);
            return passwordHash.equals(hashedInput);
        } catch (Exception e) {
            System.err.println("Error verifying password: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Generate password reset token
     * @return 
     */
    public String generatePasswordResetToken() {
        this.resetPasswordToken = UUID.randomUUID().toString();
        this.resetPasswordExpiry = Timestamp.valueOf(
            LocalDateTime.now(MANILA_TIMEZONE).plusHours(24) // Token valid for 24 hours
        );
        updateTimestamp();
        return this.resetPasswordToken;
    }
    
    /**
     * Verify password reset token
     * @param token
     * @return 
     */
    public boolean verifyPasswordResetToken(String token) {
        if (resetPasswordToken == null || token == null) {
            return false;
        }
        
        if (!resetPasswordToken.equals(token)) {
            return false;
        }
        
        if (resetPasswordExpiry == null) {
            return false;
        }
        
        Timestamp now = Timestamp.valueOf(LocalDateTime.now(MANILA_TIMEZONE));
        return now.before(resetPasswordExpiry);
    }
    
    /**
     * Reset password using token
     * @param token
     * @param newPassword
     * @return 
     */
    public boolean resetPassword(String token, String newPassword) {
        if (!verifyPasswordResetToken(token)) {
            return false;
        }
        
        boolean success = setPassword(newPassword);
        if (success) {
            // Clear reset token
            this.resetPasswordToken = null;
            this.resetPasswordExpiry = null;
            // Unlock account if locked
            unlockAccount();
        }
        
        return success;
    }
    
    // Role and permission methods
    
    /**
     * Check if user has specific role
     * @param role
     * @return 
     */
    public boolean hasRole(String role) {
        return userRole != null && userRole.equalsIgnoreCase(role);
    }
    
    /**
     * Check if user is admin
     * @return 
     */
    public boolean isAdmin() {
        return hasRole("Admin") || hasRole("HR") || hasRole("IT");
    }
    
    /**
     * Check if user is supervisor
     * @return 
     */
    public boolean isSupervisor() {
        return hasRole("Supervisor") || hasRole("Manager") || isAdmin();
    }
    
    /**
     * Check if user is employee
     * @return 
     */
    public boolean isEmployee() {
        return hasRole("Employee");
    }
    
    // Utility methods
    
    /**
     * Generate unique session ID
     */
    private String generateSessionId() {
        return UUID.randomUUID().toString() + "_" + System.currentTimeMillis();
    }
    
  /**
 * Generate salt for password hashing
 */
    private String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] saltBytes = new byte[16];  // Changed from 'salt' to 'saltBytes'
        random.nextBytes(saltBytes);
        return Base64.getEncoder().encodeToString(saltBytes);
}
    
    /**
     * Hash password with salt using SHA-256
     */
    private String hashPassword(String password, String salt) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(salt.getBytes());
        byte[] hash = digest.digest(password.getBytes());
        return Base64.getEncoder().encodeToString(hash);
    }
    
    /**
     * Update timestamp
     */
    private void updateTimestamp() {
        this.updatedAt = Timestamp.valueOf(LocalDateTime.now(MANILA_TIMEZONE));
    }
    
    /**
     * Get formatted last login time in Manila timezone
     * @return 
     */
    public String getFormattedLastLogin() {
        if (lastLogin == null) return "Never";
        
        LocalDateTime manilaTime = lastLogin.toLocalDateTime()
                .atZone(ZoneId.systemDefault())
                .withZoneSameInstant(MANILA_TIMEZONE)
                .toLocalDateTime();
        
        return manilaTime.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
    
    /**
     * Get user display name
     * @return 
     */
    public String getDisplayName() {
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        } else if (firstName != null) {
            return firstName;
        } else if (email != null) {
            return email;
        } else {
            return "User " + employeeId;
        }
    }
    
    // Getters and Setters
    public int getEmployeeId() { return employeeId; }
    public void setEmployeeId(int employeeId) { this.employeeId = employeeId; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    
    public String getUserRole() { return userRole; }
    public void setUserRole(String userRole) { this.userRole = userRole; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public Timestamp getLastLogin() { return lastLogin; }
    public void setLastLogin(Timestamp lastLogin) { this.lastLogin = lastLogin; }
    
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    
    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
    
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    
    public Timestamp getSessionStartTime() { return sessionStartTime; }
    public void setSessionStartTime(Timestamp sessionStartTime) { this.sessionStartTime = sessionStartTime; }
    
    public Timestamp getSessionLastActivity() { return sessionLastActivity; }
    public void setSessionLastActivity(Timestamp sessionLastActivity) { this.sessionLastActivity = sessionLastActivity; }
    
    public Timestamp getSessionExpireTime() { return sessionExpireTime; }
    public void setSessionExpireTime(Timestamp sessionExpireTime) { this.sessionExpireTime = sessionExpireTime; }
    
    public boolean isSessionActive() { return sessionActive; }
    public void setSessionActive(boolean sessionActive) { this.sessionActive = sessionActive; }
    
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    
    public String getSalt() { return salt; }
    public void setSalt(String salt) { this.salt = salt; }
    
    public int getLoginAttempts() { return loginAttempts; }
    public void setLoginAttempts(int loginAttempts) { this.loginAttempts = loginAttempts; }
    
    public Timestamp getLastFailedAttempt() { return lastFailedAttempt; }
    public void setLastFailedAttempt(Timestamp lastFailedAttempt) { this.lastFailedAttempt = lastFailedAttempt; }
    
    public Timestamp getAccountLockedUntil() { return accountLockedUntil; }
    public void setAccountLockedUntil(Timestamp accountLockedUntil) { this.accountLockedUntil = accountLockedUntil; }
    
    public boolean getIsAccountLocked() { return isAccountLocked; }
    public void setIsAccountLocked(boolean accountLocked) { isAccountLocked = accountLocked; }
    
    public String getResetPasswordToken() { return resetPasswordToken; }
    public void setResetPasswordToken(String resetPasswordToken) { this.resetPasswordToken = resetPasswordToken; }
    
    public Timestamp getResetPasswordExpiry() { return resetPasswordExpiry; }
    public void setResetPasswordExpiry(Timestamp resetPasswordExpiry) { this.resetPasswordExpiry = resetPasswordExpiry; }
    
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    
    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }
    
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    
    @Override
    public String toString() {
        return String.format("UserAuthenticationModel{employeeId=%d, email='%s', userRole='%s', status='%s', sessionActive=%s, isLocked=%s}",
                employeeId, email, userRole, status, sessionActive, isAccountLocked);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        UserAuthenticationModel that = (UserAuthenticationModel) obj;
        return employeeId == that.employeeId;
    }
    
    @Override
    public int hashCode() {
        return Integer.hashCode(employeeId);
    }
}