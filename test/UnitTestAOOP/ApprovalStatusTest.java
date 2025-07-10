package UnitTestAOOP;

import Models.ApprovalStatus;
import org.junit.*;
import static org.junit.Assert.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.logging.ConsoleHandler;
import java.util.logging.SimpleFormatter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ApprovalStatusTest {
    
    private static final Logger LOGGER = Logger.getLogger(ApprovalStatusTest.class.getName());
    private static Connection connection;
    private static final String DB_URL = "jdbc:mysql://localhost:3306/payrollsystem_db";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "motorph_123";
    
    @BeforeClass
    public static void setUpClass() {
        // Configure logger
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Level.ALL);
        consoleHandler.setFormatter(new SimpleFormatter());
        LOGGER.addHandler(consoleHandler);
        LOGGER.setLevel(Level.ALL);
        
        LOGGER.info("========== Starting ApprovalStatus Test Suite ==========");
        
        // Set up database connection
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            LOGGER.info("Database connection established successfully");
        } catch (ClassNotFoundException | SQLException e) {
            LOGGER.severe("Failed to establish database connection: " + e.getMessage());
        }
    }
    
    @AfterClass
    public static void tearDownClass() {
        // Close database connection
        if (connection != null) {
            try {
                connection.close();
                LOGGER.info("Database connection closed");
            } catch (SQLException e) {
                LOGGER.severe("Error closing database connection: " + e.getMessage());
            }
        }
        LOGGER.info("========== ApprovalStatus Test Suite Completed ==========");
    }
    
    @Before
    public void setUp() {
        LOGGER.info("Starting test method");
    }
    
    @After
    public void tearDown() {
        LOGGER.info("Test method completed");
    }
    
    // ========== Enum Value Tests ==========
    
    @Test
    public void testEnumValues() {
        LOGGER.info("Testing enum values existence");
        ApprovalStatus[] values = ApprovalStatus.values();
        assertEquals("Should have exactly 3 approval statuses", 3, values.length);
        
        // Verify all enum constants exist
        assertNotNull("PENDING should exist", ApprovalStatus.PENDING);
        assertNotNull("APPROVED should exist", ApprovalStatus.APPROVED);
        assertNotNull("REJECTED should exist", ApprovalStatus.REJECTED);
    }
    
    @Test
    public void testGetValue() {
        LOGGER.info("Testing getValue() method");
        assertEquals("Pending", ApprovalStatus.PENDING.getValue());
        assertEquals("Approved", ApprovalStatus.APPROVED.getValue());
        assertEquals("Rejected", ApprovalStatus.REJECTED.getValue());
    }
    
    // ========== fromString() Method Tests ==========
    
    @Test
    public void testFromStringValidValues() {
        LOGGER.info("Testing fromString() with valid values");
        assertEquals(ApprovalStatus.PENDING, ApprovalStatus.fromString("Pending"));
        assertEquals(ApprovalStatus.APPROVED, ApprovalStatus.fromString("Approved"));
        assertEquals(ApprovalStatus.REJECTED, ApprovalStatus.fromString("Rejected"));
    }
    
    @Test
    public void testFromStringCaseInsensitive() {
        LOGGER.info("Testing fromString() case insensitivity");
        assertEquals(ApprovalStatus.PENDING, ApprovalStatus.fromString("pending"));
        assertEquals(ApprovalStatus.APPROVED, ApprovalStatus.fromString("APPROVED"));
        assertEquals(ApprovalStatus.REJECTED, ApprovalStatus.fromString("rEjEcTeD"));
    }
    
    @Test
    public void testFromStringWithWhitespace() {
        LOGGER.info("Testing fromString() with whitespace");
        assertEquals(ApprovalStatus.PENDING, ApprovalStatus.fromString("  Pending  "));
        assertEquals(ApprovalStatus.APPROVED, ApprovalStatus.fromString("\tApproved\n"));
        assertEquals(ApprovalStatus.REJECTED, ApprovalStatus.fromString(" Rejected "));
    }
    
    @Test
    public void testFromStringNull() {
        LOGGER.info("Testing fromString() with null - negative test");
        ApprovalStatus result = ApprovalStatus.fromString(null);
        assertEquals("Null should return PENDING as default", ApprovalStatus.PENDING, result);
    }
    
    @Test
    public void testFromStringEmpty() {
        LOGGER.info("Testing fromString() with empty string - negative test");
        ApprovalStatus result = ApprovalStatus.fromString("");
        assertEquals("Empty string should return PENDING as default", ApprovalStatus.PENDING, result);
    }
    
    @Test
    public void testFromStringInvalid() {
        LOGGER.info("Testing fromString() with invalid values - negative test");
        assertEquals(ApprovalStatus.PENDING, ApprovalStatus.fromString("InvalidStatus"));
        assertEquals(ApprovalStatus.PENDING, ApprovalStatus.fromString("Completed"));
        assertEquals(ApprovalStatus.PENDING, ApprovalStatus.fromString("123"));
        assertEquals(ApprovalStatus.PENDING, ApprovalStatus.fromString("@#$%"));
    }
    
    // ========== Status Check Methods Tests ==========
    
    @Test
    public void testIsPending() {
        LOGGER.info("Testing isPending() method");
        assertTrue("PENDING should return true", ApprovalStatus.PENDING.isPending());
        assertFalse("APPROVED should return false", ApprovalStatus.APPROVED.isPending());
        assertFalse("REJECTED should return false", ApprovalStatus.REJECTED.isPending());
    }
    
    @Test
    public void testIsApproved() {
        LOGGER.info("Testing isApproved() method");
        assertFalse("PENDING should return false", ApprovalStatus.PENDING.isApproved());
        assertTrue("APPROVED should return true", ApprovalStatus.APPROVED.isApproved());
        assertFalse("REJECTED should return false", ApprovalStatus.REJECTED.isApproved());
    }
    
    @Test
    public void testIsRejected() {
        LOGGER.info("Testing isRejected() method");
        assertFalse("PENDING should return false", ApprovalStatus.PENDING.isRejected());
        assertFalse("APPROVED should return false", ApprovalStatus.APPROVED.isRejected());
        assertTrue("REJECTED should return true", ApprovalStatus.REJECTED.isRejected());
    }
    
    @Test
    public void testIsProcessed() {
        LOGGER.info("Testing isProcessed() method");
        assertFalse("PENDING should return false", ApprovalStatus.PENDING.isProcessed());
        assertTrue("APPROVED should return true", ApprovalStatus.APPROVED.isProcessed());
        assertTrue("REJECTED should return true", ApprovalStatus.REJECTED.isProcessed());
    }
    
    // ========== UI Helper Methods Tests ==========
    
    @Test
    public void testGetStatusColor() {
        LOGGER.info("Testing getStatusColor() method");
        assertEquals("orange", ApprovalStatus.PENDING.getStatusColor());
        assertEquals("green", ApprovalStatus.APPROVED.getStatusColor());
        assertEquals("red", ApprovalStatus.REJECTED.getStatusColor());
    }
    
    @Test
    public void testGetStatusIcon() {
        LOGGER.info("Testing getStatusIcon() method");
        assertEquals("⏳", ApprovalStatus.PENDING.getStatusIcon());
        assertEquals("✅", ApprovalStatus.APPROVED.getStatusIcon());
        assertEquals("❌", ApprovalStatus.REJECTED.getStatusIcon());
    }
    
    @Test
    public void testGetDisplayString() {
        LOGGER.info("Testing getDisplayString() method");
        assertEquals("⏳ Pending", ApprovalStatus.PENDING.getDisplayString());
        assertEquals("✅ Approved", ApprovalStatus.APPROVED.getDisplayString());
        assertEquals("❌ Rejected", ApprovalStatus.REJECTED.getDisplayString());
    }
    
    @Test
    public void testToString() {
        LOGGER.info("Testing toString() method");
        assertEquals("Pending", ApprovalStatus.PENDING.toString());
        assertEquals("Approved", ApprovalStatus.APPROVED.toString());
        assertEquals("Rejected", ApprovalStatus.REJECTED.toString());
    }
    
    // ========== Database Integration Tests ==========
    
    @Test
    public void testDatabaseEnumCompatibility() {
        LOGGER.info("Testing database enum compatibility");
        if (connection == null) {
            LOGGER.warning("Skipping database test - no connection");
            return;
        }
        
        try {
            // Test leave request table
            String query = "SELECT DISTINCT approvalStatus FROM leaverequest LIMIT 10";
            PreparedStatement stmt = connection.prepareStatement(query);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                String dbValue = rs.getString("approvalStatus");
                if (dbValue != null) {
                    ApprovalStatus status = ApprovalStatus.fromString(dbValue);
                    assertNotNull("Should parse database value: " + dbValue, status);
                    LOGGER.info("Successfully parsed database value: " + dbValue + " to " + status);
                }
            }
            
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            LOGGER.severe("Database test failed: " + e.getMessage());
            fail("Database test failed: " + e.getMessage());
        }
    }
    
    @Test
    public void testDatabaseOvertimeRequestCompatibility() {
        LOGGER.info("Testing overtime request database compatibility");
        if (connection == null) {
            LOGGER.warning("Skipping database test - no connection");
            return;
        }
        
        try {
            String query = "SELECT DISTINCT approvalStatus FROM overtimerequest LIMIT 10";
            PreparedStatement stmt = connection.prepareStatement(query);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                String dbValue = rs.getString("approvalStatus");
                if (dbValue != null) {
                    ApprovalStatus status = ApprovalStatus.fromString(dbValue);
                    assertNotNull("Should parse database value: " + dbValue, status);
                }
            }
            
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            LOGGER.severe("Database test failed: " + e.getMessage());
            fail("Database test failed: " + e.getMessage());
        }
    }
    
    // ========== Edge Case and Boundary Tests ==========
    
    @Test
    public void testEnumOrdinal() {
        LOGGER.info("Testing enum ordinal values");
        assertEquals(0, ApprovalStatus.PENDING.ordinal());
        assertEquals(1, ApprovalStatus.APPROVED.ordinal());
        assertEquals(2, ApprovalStatus.REJECTED.ordinal());
    }
    
    @Test
    public void testEnumName() {
        LOGGER.info("Testing enum name() method");
        assertEquals("PENDING", ApprovalStatus.PENDING.name());
        assertEquals("APPROVED", ApprovalStatus.APPROVED.name());
        assertEquals("REJECTED", ApprovalStatus.REJECTED.name());
    }
    
    @Test
    public void testValueOfMethod() {
        LOGGER.info("Testing valueOf() method");
        assertEquals(ApprovalStatus.PENDING, ApprovalStatus.valueOf("PENDING"));
        assertEquals(ApprovalStatus.APPROVED, ApprovalStatus.valueOf("APPROVED"));
        assertEquals(ApprovalStatus.REJECTED, ApprovalStatus.valueOf("REJECTED"));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testValueOfInvalid() {
        LOGGER.info("Testing valueOf() with invalid value - negative test");
        ApprovalStatus.valueOf("INVALID");
    }
    
    @Test(expected = NullPointerException.class)
    public void testValueOfNull() {
        LOGGER.info("Testing valueOf() with null - negative test");
        ApprovalStatus.valueOf(null);
    }
    
    // ========== Special Character and Unicode Tests ==========
    
    @Test
    public void testFromStringSpecialCharacters() {
        LOGGER.info("Testing fromString() with special characters - negative test");
        assertEquals(ApprovalStatus.PENDING, ApprovalStatus.fromString("Pending!@#"));
        assertEquals(ApprovalStatus.PENDING, ApprovalStatus.fromString("$Approved$"));
        assertEquals(ApprovalStatus.PENDING, ApprovalStatus.fromString("Rejected%"));
    }
    
    @Test
    public void testFromStringUnicode() {
        LOGGER.info("Testing fromString() with unicode - negative test");
        assertEquals(ApprovalStatus.PENDING, ApprovalStatus.fromString("待定"));
        assertEquals(ApprovalStatus.PENDING, ApprovalStatus.fromString("承認済み"));
        assertEquals(ApprovalStatus.PENDING, ApprovalStatus.fromString("مرفوض"));
    }
    
    // ========== Performance and Stress Tests ==========
    
    @Test
    public void testFromStringPerformance() {
        LOGGER.info("Testing fromString() performance");
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < 10000; i++) {
            ApprovalStatus.fromString("Pending");
            ApprovalStatus.fromString("Approved");
            ApprovalStatus.fromString("Rejected");
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        LOGGER.info("Performance test completed in " + duration + "ms");
        assertTrue("Performance test should complete within 1 second", duration < 1000);
    }
    
    @Test
    public void testStatusCheckPerformance() {
        LOGGER.info("Testing status check methods performance");
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < 100000; i++) {
            ApprovalStatus.PENDING.isPending();
            ApprovalStatus.APPROVED.isApproved();
            ApprovalStatus.REJECTED.isRejected();
            ApprovalStatus.APPROVED.isProcessed();
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        LOGGER.info("Status check performance test completed in " + duration + "ms");
        assertTrue("Status check should be very fast", duration < 100);
    }
    
    // ========== Consistency Tests ==========
    
    @Test
    public void testConsistencyBetweenMethods() {
        LOGGER.info("Testing consistency between different methods");
        
        for (ApprovalStatus status : ApprovalStatus.values()) {
            // getValue() and toString() should match
            assertEquals(status.getValue(), status.toString());
            
            // fromString should return the same enum
            assertEquals(status, ApprovalStatus.fromString(status.getValue()));
            
            // Display string should contain the value
            assertTrue(status.getDisplayString().contains(status.getValue()));
        }
    }
    
    @Test
    public void testMutualExclusivity() {
        LOGGER.info("Testing mutual exclusivity of status checks");
        
        for (ApprovalStatus status : ApprovalStatus.values()) {
            int trueCount = 0;
            if (status.isPending()) trueCount++;
            if (status.isApproved()) trueCount++;
            if (status.isRejected()) trueCount++;
            
            assertEquals("Only one status should be true at a time", 1, trueCount);
        }
    }
    
    // ========== SQL Injection Prevention Tests ==========
    
    @Test
    public void testFromStringSQLInjectionAttempts() {
        LOGGER.info("Testing fromString() with SQL injection attempts - negative test");
        assertEquals(ApprovalStatus.PENDING, ApprovalStatus.fromString("'; DROP TABLE employee; --"));
        assertEquals(ApprovalStatus.PENDING, ApprovalStatus.fromString("1=1; DELETE FROM attendance"));
        assertEquals(ApprovalStatus.PENDING, ApprovalStatus.fromString("UNION SELECT * FROM employee"));
    }
    
    // ========== Extreme Length Tests ==========
    
    @Test
    public void testFromStringVeryLongInput() {
        LOGGER.info("Testing fromString() with very long input - negative test");
        StringBuilder longString = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longString.append("Pending");
        }
        assertEquals(ApprovalStatus.PENDING, ApprovalStatus.fromString(longString.toString()));
    }
    
    @Test
    public void testFromStringEmptyWhitespace() {
        LOGGER.info("Testing fromString() with various whitespace - negative test");
        assertEquals(ApprovalStatus.PENDING, ApprovalStatus.fromString("   "));
        assertEquals(ApprovalStatus.PENDING, ApprovalStatus.fromString("\t\t"));
        assertEquals(ApprovalStatus.PENDING, ApprovalStatus.fromString("\n\n"));
        assertEquals(ApprovalStatus.PENDING, ApprovalStatus.fromString("\r\n"));
    }
    
    // ========== Thread Safety Test ==========
    
    @Test
    public void testThreadSafety() throws InterruptedException {
        LOGGER.info("Testing thread safety of enum methods");
        
        final int THREAD_COUNT = 10;
        final int ITERATIONS = 1000;
        final boolean[] errors = new boolean[1];
        
        Thread[] threads = new Thread[THREAD_COUNT];
        
        for (int i = 0; i < THREAD_COUNT; i++) {
            threads[i] = new Thread(() -> {
                try {
                    for (int j = 0; j < ITERATIONS; j++) {
                        ApprovalStatus status = ApprovalStatus.fromString("Approved");
                        if (status != ApprovalStatus.APPROVED) {
                            errors[0] = true;
                        }
                        
                        boolean approved = ApprovalStatus.APPROVED.isApproved();
                        if (!approved) {
                            errors[0] = true;
                        }
                    }
                } catch (Exception e) {
                    errors[0] = true;
                    LOGGER.severe("Thread error: " + e.getMessage());
                }
            });
            threads[i].start();
        }
        
        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }
        
        assertFalse("No errors should occur in concurrent access", errors[0]);
    }
    
    // ========== Memory and Resource Tests ==========
    
    @Test
    public void testNoMemoryLeaks() {
        LOGGER.info("Testing for potential memory leaks");
        
        Runtime runtime = Runtime.getRuntime();
        runtime.gc();
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();
        
        // Perform many operations
        for (int i = 0; i < 100000; i++) {
            ApprovalStatus.fromString("Pending");
            ApprovalStatus.PENDING.getDisplayString();
            ApprovalStatus.APPROVED.getStatusColor();
        }
        
        runtime.gc();
        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
        long memoryIncrease = memoryAfter - memoryBefore;
        
        LOGGER.info("Memory increase: " + memoryIncrease + " bytes");
        assertTrue("Memory increase should be minimal", memoryIncrease < 10_000_000); // 10MB threshold
    }
    
    // ========== Comprehensive Coverage Test ==========
    
    @Test
    public void testComprehensiveCoverage() {
        LOGGER.info("Testing comprehensive coverage of all enum values and methods");
        
        for (ApprovalStatus status : ApprovalStatus.values()) {
            // Test all methods for each enum value
            assertNotNull(status.getValue());
            assertNotNull(status.toString());
            assertNotNull(status.getStatusColor());
            assertNotNull(status.getStatusIcon());
            assertNotNull(status.getDisplayString());
            
            // Test boolean methods return valid results
            boolean isPending = status.isPending();
            boolean isApproved = status.isApproved();
            boolean isRejected = status.isRejected();
            boolean isProcessed = status.isProcessed();
            
            // At least one should be true
            assertTrue(isPending || isApproved || isRejected);
            
            // Processed should match approved or rejected
            assertEquals(isProcessed, isApproved || isRejected);
            
            LOGGER.info("Status: " + status + " - Display: " + status.getDisplayString());
        }
    }
}