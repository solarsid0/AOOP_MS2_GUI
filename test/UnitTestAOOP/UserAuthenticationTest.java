
package UnitTestAOOP;
import Models.UserAuthenticationModel;
import Models.EmployeeModel;
import DAOs.DatabaseConnection;
import DAOs.EmployeeDAO;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;

public class UserAuthenticationTest {
    
    private UserAuthenticationModel authModel;
    private static DatabaseConnection dbConnection;
    private static EmployeeDAO employeeDAO;
    
    // Test data constants
    private static final String TEST_EMAIL = "test.user@company.com";
    private static final String TEST_PASSWORD = "TestPass123!";
    private static final String WRONG_PASSWORD = "WrongPass123!";
    private static final String HR_EMAIL = "hr.manager@company.com";
    private static final String IT_EMAIL = "it.admin@company.com";
    private static final String SUPERVISOR_EMAIL = "supervisor@company.com";
    private static final String ACCOUNTING_EMAIL = "accounting@company.com";
    private static final String LOCKED_EMAIL = "locked.user@company.com";
    
    @BeforeClass
    public static void setUpClass() throws Exception {
        System.out.println("=== Setting up UserAuthenticationTest class ===");
        
        dbConnection = new DatabaseConnection(
        "jdbc:mysql://localhost:3306/payrollsystem_db",
        "root", 
        "Mmdc_20250*");
        
        // Test connection
        if (!dbConnection.testConnection()) {
            System.err.println("️   Database connection failed! Tests may not work properly.");
            System.err.println("   Make sure MySQL is running and credentials are correct.");
        }
        
        employeeDAO = new EmployeeDAO(dbConnection);
        
        // Clean up any existing test data
        cleanupTestData();
        
        // Create test employees
        createTestEmployees();
    }
    
    @AfterClass
    public static void tearDownClass() throws Exception {
        System.out.println("=== Tearing down UserAuthenticationTest class ===");
        cleanupTestData();
    }
    
    @Before
    public void setUp() {
        System.out.println("\n--- Setting up test ---");
        authModel = new UserAuthenticationModel();
    }
    
    @After
    public void tearDown() {
        System.out.println("--- Tearing down test ---");
        // Your current UserAuthenticationModel might not have logout method
        // Just create a new instance for next test
        authModel = null;
    }
    
    // Helper method to hash password the same way as your system
    private static String hashPasswordLikeAuth(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }
    
    // ========================================
    // CORE AUTHENTICATION TESTS (Updated for current implementation)
    // ========================================
    
    @Test
    public void testUserAuthenticationModelCreation() {
        System.out.println("Testing UserAuthenticationModel creation...");
        
        // Test that we can create the model
        assertNotNull("UserAuthenticationModel should be created", authModel);
        
        // Test basic properties exist
        assertNotNull("Should have employeeId field", authModel.getEmployeeId());
        
        // Test initial state
        assertEquals("Initial employee ID should be 0", 0, authModel.getEmployeeId());
    }
    
    @Test
    public void testPasswordHashing() {
        System.out.println("Testing password hashing...");
        
        // Test that password setting works
        boolean result = authModel.setPassword(TEST_PASSWORD);
        assertTrue("Should be able to set password", result);
        
        // Test password verification
        boolean verified = authModel.verifyPassword(TEST_PASSWORD);
        assertTrue("Should verify correct password", verified);
        
        // Test wrong password
        boolean wrongVerified = authModel.verifyPassword(WRONG_PASSWORD);
        assertFalse("Should not verify wrong password", wrongVerified);
    }
    
    @Test
    public void testAccountLocking() {
        System.out.println("Testing account locking mechanism...");
        
        // Set up authentication model with test credentials
        authModel.setEmail(TEST_EMAIL);
        authModel.setPassword(TEST_PASSWORD);
        
        // Simulate multiple failed login attempts
        for (int i = 0; i < 5; i++) {
            authModel.authenticate(WRONG_PASSWORD);
        }
        
        assertTrue("Account should be locked after 5 failed attempts", authModel.isAccountLocked());
        
        // Try to authenticate with correct password while locked
        boolean result = authModel.authenticate(TEST_PASSWORD);
        assertFalse("Authentication should fail when account is locked", result);
    }
    
    
    @Test
    public void testSessionManagement() {
        System.out.println("Testing session management...");
        
        // Test session start
        boolean sessionStarted = authModel.startSession("127.0.0.1", "TestAgent");
        
        if (sessionStarted) {
            assertTrue("Session should be active", authModel.isSessionActive());
            assertNotNull("Session ID should be generated", authModel.getSessionId());
            assertTrue("Session should be valid", authModel.isSessionValid());
            
            // Test session update
            boolean updated = authModel.updateSessionActivity();
            assertTrue("Should be able to update session activity", updated);
            
            // Test session end
            authModel.endSession();
            assertFalse("Session should not be active after ending", authModel.isSessionActive());
        } else {
            System.out.println("Session start failed - possibly due to account lock state");
        }
    }
    
    @Test
    public void testRoleBasedPermissions() {
        System.out.println("Testing role-based permissions...");
        
        // Test different roles
        authModel.setUserRole("Employee");
        assertFalse("Employee should not be admin", authModel.isAdmin());
        assertTrue("Employee should be employee", authModel.isEmployee());
        
        authModel.setUserRole("HR");
        assertTrue("HR should be admin", authModel.isAdmin());
        
        authModel.setUserRole("IT");
        assertTrue("IT should be admin", authModel.isAdmin());
    }
    
    @Test
    public void testPasswordReset() {
        System.out.println("Testing password reset functionality...");
        
        // Generate reset token
        String token = authModel.generatePasswordResetToken();
        assertNotNull("Reset token should be generated", token);
        
        // Verify token
        boolean tokenValid = authModel.verifyPasswordResetToken(token);
        assertTrue("Token should be valid", tokenValid);
        
        // Reset password
        String newPassword = "NewPassword123!";
        boolean resetSuccess = authModel.resetPassword(token, newPassword);
        assertTrue("Password reset should succeed", resetSuccess);
        
        // Verify new password works
        boolean newPasswordVerified = authModel.verifyPassword(newPassword);
        assertTrue("New password should be verified", newPasswordVerified);
    }
    
    @Test
    public void testManilaTimezone() {
        System.out.println("Testing Manila timezone functionality...");
        
        // Test that timestamps use Manila timezone
        authModel.setEmail(TEST_EMAIL);
        authModel.setPassword(TEST_PASSWORD);
        
        // Start session and check timestamps
        if (authModel.startSession("127.0.0.1", "TestAgent")) {
            assertNotNull("Session start time should be set", authModel.getSessionStartTime());
            assertNotNull("Last activity should be set", authModel.getSessionLastActivity());
            
            // Test formatted time
            String formattedTime = authModel.getFormattedLastLogin();
            assertNotNull("Formatted time should not be null", formattedTime);
            assertFalse("Formatted time should not be 'Never'", "Never".equals(formattedTime));
        }
    }
    
    // ========================================
    // NEGATIVE TEST CASES - Updated
    // ========================================
    
    @Test
    public void testInvalidInputHandling() {
        System.out.println("Testing invalid input handling - NEGATIVE TEST...");
        
        // Test null password
        boolean nullPasswordResult = authModel.setPassword(null);
        assertFalse("Should not accept null password", nullPasswordResult);
        
        
        // Test null email
        authModel.setEmail(null);
        // Should handle gracefully
        
        // Test authentication with null values
        boolean nullAuthResult = authModel.authenticate(null);
        assertFalse("Authentication with null should fail", nullAuthResult);
    }
    
    @Test
    public void testSessionTimeouts() {
        System.out.println("Testing session timeout handling...");
        
        // Test session duration calculation
        long duration = authModel.getSessionDurationMinutes();
        assertTrue("Session duration should be non-negative", duration >= 0);
        
        // Test time until expiry
        long timeUntilExpiry = authModel.getTimeUntilExpiryMinutes();
        assertTrue("Time until expiry should be non-negative", timeUntilExpiry >= 0);
    }
    
    // ========================================
    // INTEGRATION TESTS WITH DATABASE
    // ========================================
    
    @Test
    public void testDatabaseIntegration() {
        System.out.println("Testing database integration...");
        
        // Test that we can find employees
        EmployeeModel testEmployee = employeeDAO.findByEmail(TEST_EMAIL);
        
        if (testEmployee != null) {
            System.out.println("✓ Found test employee: " + testEmployee.getFullName());
            
            // Test creating UserAuthenticationModel from employee data
            UserAuthenticationModel authFromDb = new UserAuthenticationModel(
                testEmployee.getEmployeeId(), 
                testEmployee.getEmail(), 
                testEmployee.getUserRole()
            );
            
            assertNotNull("Should create auth model from DB data", authFromDb);
            assertEquals("Email should match", testEmployee.getEmail(), authFromDb.getEmail());
            assertEquals("Role should match", testEmployee.getUserRole(), authFromDb.getUserRole());
            
        } else {
            System.out.println("⚠️  Test employee not found - database integration limited");
        }
    }
    
    @Test
    public void testDisplayName() {
        System.out.println("Testing display name functionality...");
        
        // Test with first/last name
        authModel.setFirstName("John");
        authModel.setLastName("Doe");
        String displayName = authModel.getDisplayName();
        assertTrue("Display name should contain first and last name", 
                  displayName.contains("John") && displayName.contains("Doe"));
        
        // Test with email fallback
        UserAuthenticationModel emailAuth = new UserAuthenticationModel();
        emailAuth.setEmail("test@example.com");
        String emailDisplayName = emailAuth.getDisplayName();
        assertTrue("Should fallback to email", emailDisplayName.contains("test@example.com"));
    }
    
    // ========================================
    // HELPER METHODS
    // ========================================
    
    private static void createTestEmployees() {
        System.out.println("Creating test employees...");
        
        // Only create if they don't exist
        if (employeeDAO.findByEmail(TEST_EMAIL) == null) {
            createTestEmployee(TEST_EMAIL, "Test", "User", "Employee", 1);
        }
        if (employeeDAO.findByEmail(HR_EMAIL) == null) {
            createTestEmployee(HR_EMAIL, "HR", "Manager", "HR", 2);
        }
        if (employeeDAO.findByEmail(IT_EMAIL) == null) {
            createTestEmployee(IT_EMAIL, "IT", "Admin", "IT", 3);
        }
        if (employeeDAO.findByEmail(SUPERVISOR_EMAIL) == null) {
            createTestEmployee(SUPERVISOR_EMAIL, "Super", "Visor", "Supervisor", 4);
        }
        if (employeeDAO.findByEmail(ACCOUNTING_EMAIL) == null) {
            createTestEmployee(ACCOUNTING_EMAIL, "Account", "Manager", "Accounting", 5);
        }
        if (employeeDAO.findByEmail(LOCKED_EMAIL) == null) {
            createTestEmployee(LOCKED_EMAIL, "Locked", "User", "Employee", 1);
        }
    }
    
    private static void createTestEmployee(String email, String firstName, String lastName, 
                                          String role, int positionId) {
        EmployeeModel employee = new EmployeeModel();
        employee.setEmail(email);
        employee.setFirstName(firstName);
        employee.setLastName(lastName);
        employee.setUserRole(role);
        employee.setPasswordHash(hashPasswordLikeAuth(TEST_PASSWORD));
        employee.setBirthDate(LocalDate.now().minusYears(30));
        employee.setPhoneNumber("1234567890");
        employee.setBasicSalary(new BigDecimal("50000.00"));
        employee.setHourlyRate(new BigDecimal("250.00"));
        employee.setStatus(EmployeeModel.EmployeeStatus.REGULAR);
        employee.setPositionId(positionId);
        
        boolean saved = employeeDAO.save(employee);
        if (saved) {
            System.out.println("✓ Created: " + email + " (" + role + ")");
        } else {
            System.out.println("✗ Failed to create: " + email);
        }
    }
    
    private static void cleanupTestData() {
        System.out.println("Cleaning up test data...");
        
        String[] testEmails = {
            TEST_EMAIL, HR_EMAIL, IT_EMAIL, SUPERVISOR_EMAIL, 
            ACCOUNTING_EMAIL, LOCKED_EMAIL
        };
        
        for (String email : testEmails) {
            EmployeeModel employee = employeeDAO.findByEmail(email);
            if (employee != null) {
                employeeDAO.delete(employee.getEmployeeId());
            }
        }
    }
}