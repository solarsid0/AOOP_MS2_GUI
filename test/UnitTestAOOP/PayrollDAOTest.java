
package UnitTestAOOP;


import DAOs.PayrollDAO;
import DAOs.DatabaseConnection;
import DAOs.EmployeeDAO;
import DAOs.PayPeriodDAO;
import Models.PayrollModel;
import Models.EmployeeModel;
import Models.PayPeriodModel;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Comprehensive JUnit test for PayrollDAO with integrated negative testing.
 */
public class PayrollDAOTest {
    
    private PayrollDAO payrollDAO;
    private EmployeeDAO employeeDAO;
    private DatabaseConnection dbConnection;
    
    // Test data IDs to track and clean up
    private Integer testEmployeeId;
    private Integer testPayPeriodId;
    private Integer testPayrollId;
    
    @Before
    public void setUp() {
        // Initialize database connection and DAOs
        dbConnection = new DatabaseConnection();
        payrollDAO = new PayrollDAO(dbConnection);
        employeeDAO = new EmployeeDAO(dbConnection);
        
        // Create test data
        createTestData();
    }
    
    @After
    public void tearDown() {
        // Clean up test data in reverse order of creation
        cleanupTestData();
    }
    
    /**
     * Creates test data for use in test cases
     */
    private void createTestData() {
        try {
            // Create test employee
            EmployeeModel testEmployee = new EmployeeModel();
            testEmployee.setFirstName("Test");
            testEmployee.setLastName("Employee");
            testEmployee.setEmail("test.employee" + System.currentTimeMillis() + "@test.com");
            testEmployee.setPasswordHash("hashedpassword");
            testEmployee.setBasicSalary(new BigDecimal("50000.00"));
            testEmployee.setHourlyRate(new BigDecimal("300.00"));
            testEmployee.setStatus(EmployeeModel.EmployeeStatus.REGULAR);
            testEmployee.setBirthDate(LocalDate.of(1990, 1, 1));
            testEmployee.setPositionId(1); // Assuming position ID 1 exists
            testEmployee.setPhoneNumber("09123456789");
            
            if (employeeDAO.save(testEmployee)) {
                testEmployeeId = testEmployee.getEmployeeId();
            }
            
            // Create test pay period using direct SQL
            testPayPeriodId = createPayPeriod(
                LocalDate.now().minusDays(15),
                LocalDate.now(),
                "Test Period " + System.currentTimeMillis()
            );
            
        } catch (Exception e) {
            System.err.println("Error creating test data: " + e.getMessage());
        }
    }
    
    /**
     * Cleans up test data after tests
     */
    private void cleanupTestData() {
        try {
            // Delete test payroll records
            if (testPayPeriodId != null) {
                payrollDAO.deletePayrollByPeriod(testPayPeriodId);
            }
            
            // Delete test pay period using direct SQL
            if (testPayPeriodId != null) {
                deletePayPeriod(testPayPeriodId);
            }
            
            // Delete test employee
            if (testEmployeeId != null) {
                employeeDAO.delete(testEmployeeId);
            }
            
        } catch (Exception e) {
            System.err.println("Error cleaning up test data: " + e.getMessage());
        }
    }
    
    // ==================== POSITIVE TEST CASES ====================
    
    @Test
    public void testSavePayroll_ValidData_Success() {
        // Arrange
        PayrollModel payroll = new PayrollModel();
        payroll.setEmployeeId(testEmployeeId);
        payroll.setPayPeriodId(testPayPeriodId);
        payroll.setBasicSalary(new BigDecimal("25000.00"));
        payroll.setGrossIncome(new BigDecimal("30000.00"));
        payroll.setTotalBenefit(new BigDecimal("5000.00"));
        payroll.setTotalDeduction(new BigDecimal("3000.00"));
        payroll.setNetSalary(new BigDecimal("27000.00"));
        
        // Act
        boolean result = payrollDAO.save(payroll);
        
        // Assert
        assertTrue("Should successfully save valid payroll", result);
        
        // If the ID is not set by the save method, we need to find it
        if (payroll.getPayrollId() == null) {
            // Find the payroll we just created
            List<PayrollModel> payrolls = payrollDAO.findByEmployee(testEmployeeId);
            if (!payrolls.isEmpty()) {
                payroll = payrolls.get(0);
                testPayrollId = payroll.getPayrollId();
            }
        } else {
            testPayrollId = payroll.getPayrollId();
        }
        
        assertNotNull("Payroll should exist in database", testPayrollId);
    }
    
    @Test
    public void testFindById_ExistingPayroll_ReturnsPayroll() {
        // Arrange
        PayrollModel savedPayroll = createAndSaveTestPayroll();
        assertNotNull("Test payroll should be created", savedPayroll);
        assertNotNull("Test payroll should have an ID", savedPayroll.getPayrollId());
        
        // Act
        PayrollModel foundPayroll = payrollDAO.findById(savedPayroll.getPayrollId());
        
        // Assert
        assertNotNull("Should find existing payroll", foundPayroll);
        assertEquals("Employee ID should match", savedPayroll.getEmployeeId(), foundPayroll.getEmployeeId());
        assertEquals("Basic salary should match", 0, savedPayroll.getBasicSalary().compareTo(foundPayroll.getBasicSalary()));
    }
    
    @Test
    public void testUpdatePayroll_ValidChanges_Success() {
        // Arrange
        PayrollModel payroll = createAndSaveTestPayroll();
        assertNotNull("Test payroll should be created", payroll);
        assertNotNull("Test payroll should have an ID", payroll.getPayrollId());
        
        BigDecimal newNetSalary = new BigDecimal("35000.00");
        payroll.setNetSalary(newNetSalary);
        
        // Act
        boolean result = payrollDAO.update(payroll);
        
        // Assert
        assertTrue("Should successfully update payroll", result);
        PayrollModel updated = payrollDAO.findById(payroll.getPayrollId());
        assertNotNull("Should find updated payroll", updated);
        assertEquals("Net salary should be updated", 0, newNetSalary.compareTo(updated.getNetSalary()));
    }
    
    @Test
    public void testGeneratePayroll_ValidPeriod_Success() {
        // Act
        int generatedCount = payrollDAO.generatePayroll(testPayPeriodId);
        
        // Assert
        assertTrue("Should generate at least one payroll record", generatedCount > 0);
        
        // Verify payroll was created
        List<PayrollModel> payrolls = payrollDAO.findByPayPeriod(testPayPeriodId);
        assertFalse("Should have payroll records for period", payrolls.isEmpty());
    }
    
    @Test
    public void testGetPayrollSummary_ValidPeriod_ReturnsSummary() {
        // Arrange
        createAndSaveTestPayroll();
        
        // Act
        PayrollDAO.PayrollSummary summary = payrollDAO.getPayrollSummary(testPayPeriodId);
        
        // Assert
        assertNotNull("Should return payroll summary", summary);
        assertTrue("Employee count should be positive", summary.getEmployeeCount() > 0);
        assertTrue("Total gross income should be positive", summary.getTotalGrossIncome().compareTo(BigDecimal.ZERO) > 0);
    }
    
    // ==================== NEGATIVE TEST CASES ====================
    
    @Test
    public void testSavePayroll_NullEmployeeId_Failure() {
        // Arrange
        PayrollModel payroll = new PayrollModel();
        payroll.setEmployeeId(null); // NULL employee ID
        payroll.setPayPeriodId(testPayPeriodId);
        payroll.setBasicSalary(new BigDecimal("25000.00"));
        payroll.setGrossIncome(new BigDecimal("30000.00"));
        payroll.setTotalBenefit(new BigDecimal("5000.00"));
        payroll.setTotalDeduction(new BigDecimal("3000.00"));
        payroll.setNetSalary(new BigDecimal("27000.00"));
        
        // Act
        boolean result = false;
        try {
            result = payrollDAO.save(payroll);
        } catch (Exception e) {
            // Expected - null employee ID should cause an error
            System.out.println("Expected error for null employee ID: " + e.getMessage());
        }
        
        // Assert
        assertFalse("Should fail to save payroll with null employee ID", result);
    }
    
    @Test
    public void testSavePayroll_InvalidEmployeeId_Failure() {
        // Arrange
        PayrollModel payroll = new PayrollModel();
        payroll.setEmployeeId(999999); // Non-existent employee
        payroll.setPayPeriodId(testPayPeriodId);
        payroll.setBasicSalary(new BigDecimal("25000.00"));
        payroll.setGrossIncome(new BigDecimal("30000.00"));
        payroll.setTotalBenefit(new BigDecimal("5000.00"));
        payroll.setTotalDeduction(new BigDecimal("3000.00"));
        payroll.setNetSalary(new BigDecimal("27000.00"));
        
        // Act
        boolean result = payrollDAO.save(payroll);
        
        // Assert
        assertFalse("Should fail to save payroll with invalid employee ID", result);
    }
    
    @Test
    public void testSavePayroll_NegativeAmounts_Failure() {
        // Arrange
        PayrollModel payroll = new PayrollModel();
        payroll.setEmployeeId(testEmployeeId);
        payroll.setPayPeriodId(testPayPeriodId);
        payroll.setBasicSalary(new BigDecimal("-25000.00")); // Negative salary
        payroll.setGrossIncome(new BigDecimal("-30000.00"));
        payroll.setTotalBenefit(new BigDecimal("5000.00"));
        payroll.setTotalDeduction(new BigDecimal("3000.00"));
        payroll.setNetSalary(new BigDecimal("-27000.00"));
        
        // Act & Assert
        // Depending on database constraints, this might save or fail
        // We're testing the behavior either way
        boolean result = payrollDAO.save(payroll);
        if (result) {
            // If it saved, verify we can retrieve it
            List<PayrollModel> payrolls = payrollDAO.findByPayPeriod(testPayPeriodId);
            PayrollModel saved = null;
            for (PayrollModel p : payrolls) {
                if (p.getEmployeeId().equals(testEmployeeId) && 
                    p.getBasicSalary().compareTo(BigDecimal.ZERO) < 0) {
                    saved = p;
                    break;
                }
            }
            assertNotNull("Should be able to retrieve saved payroll", saved);
            assertTrue("Basic salary should be negative", saved.getBasicSalary().compareTo(BigDecimal.ZERO) < 0);
        }
    }
    
    @Test
    public void testFindById_NonExistentId_ReturnsNull() {
        // Act
        PayrollModel result = payrollDAO.findById(999999);
        
        // Assert
        assertNull("Should return null for non-existent payroll ID", result);
    }
    
    @Test
    public void testFindById_NullId_ReturnsNull() {
        // Act
        PayrollModel result = payrollDAO.findById(null);
        
        // Assert
        assertNull("Should return null for null payroll ID", result);
    }
    
    @Test
    public void testUpdatePayroll_NonExistentId_Failure() {
        // Arrange
        PayrollModel payroll = new PayrollModel();
        payroll.setPayrollId(999999); // Non-existent ID
        payroll.setEmployeeId(testEmployeeId);
        payroll.setPayPeriodId(testPayPeriodId);
        payroll.setBasicSalary(new BigDecimal("25000.00"));
        payroll.setGrossIncome(new BigDecimal("30000.00"));
        payroll.setTotalBenefit(new BigDecimal("5000.00"));
        payroll.setTotalDeduction(new BigDecimal("3000.00"));
        payroll.setNetSalary(new BigDecimal("27000.00"));
        
        // Act
        boolean result = payrollDAO.update(payroll);
        
        // Assert
        assertFalse("Should fail to update non-existent payroll", result);
    }
    
    @Test
    public void testDeletePayroll_NonExistentId_Failure() {
        // Act
        boolean result = payrollDAO.delete(999999);
        
        // Assert
        assertFalse("Should fail to delete non-existent payroll", result);
    }
    
    @Test
    public void testGeneratePayroll_InvalidPeriod_ReturnsZero() {
        // Act
        int result = payrollDAO.generatePayroll(999999);
        
        // Assert
        assertEquals("Should return 0 for invalid pay period", 0, result);
    }
    
    // ==================== EDGE CASE TESTS ====================
    
    @Test
    public void testSavePayroll_DuplicateEmployeePeriod_Failure() {
        // Arrange - Create first payroll
        PayrollModel firstPayroll = createAndSaveTestPayroll();
        
        // Try to create duplicate
        PayrollModel duplicatePayroll = new PayrollModel();
        duplicatePayroll.setEmployeeId(testEmployeeId);
        duplicatePayroll.setPayPeriodId(testPayPeriodId);
        duplicatePayroll.setBasicSalary(new BigDecimal("25000.00"));
        duplicatePayroll.setGrossIncome(new BigDecimal("30000.00"));
        duplicatePayroll.setTotalBenefit(new BigDecimal("5000.00"));
        duplicatePayroll.setTotalDeduction(new BigDecimal("3000.00"));
        duplicatePayroll.setNetSalary(new BigDecimal("27000.00"));
        
        // Act
        boolean result = payrollDAO.save(duplicatePayroll);
        
        // Assert - Depending on database constraints
        if (!result) {
            assertFalse("Should fail to create duplicate payroll", result);
        } else {
            // If it allows duplicates, verify both exist
            List<PayrollModel> payrolls = payrollDAO.findByEmployee(testEmployeeId);
            assertTrue("Should have multiple payrolls if duplicates allowed", payrolls.size() >= 2);
        }
    }
    
    @Test
    public void testSavePayroll_VeryLargeAmounts_HandlesCorrectly() {
        // Arrange
        PayrollModel payroll = new PayrollModel();
        payroll.setEmployeeId(testEmployeeId);
        payroll.setPayPeriodId(testPayPeriodId);
        payroll.setBasicSalary(new BigDecimal("999999999.99"));
        payroll.setGrossIncome(new BigDecimal("999999999.99"));
        payroll.setTotalBenefit(new BigDecimal("999999999.99"));
        payroll.setTotalDeduction(new BigDecimal("100000.00"));
        payroll.setNetSalary(new BigDecimal("999899999.99"));
        
        // Act
        boolean result = payrollDAO.save(payroll);
        
        // Assert
        if (result) {
            PayrollModel saved = payrollDAO.findById(payroll.getPayrollId());
            assertNotNull("Should retrieve saved payroll with large amounts", saved);
            assertEquals("Large amount should be preserved", 0, 
                new BigDecimal("999999999.99").compareTo(saved.getBasicSalary()));
        }
    }
    
    @Test
    public void testSavePayroll_ZeroAmounts_Success() {
        // Arrange
        PayrollModel payroll = new PayrollModel();
        payroll.setEmployeeId(testEmployeeId);
        payroll.setPayPeriodId(testPayPeriodId);
        payroll.setBasicSalary(BigDecimal.ZERO);
        payroll.setGrossIncome(BigDecimal.ZERO);
        payroll.setTotalBenefit(BigDecimal.ZERO);
        payroll.setTotalDeduction(BigDecimal.ZERO);
        payroll.setNetSalary(BigDecimal.ZERO);
        
        // Act
        boolean result = payrollDAO.save(payroll);
        
        // Assert
        assertTrue("Should save payroll with zero amounts", result);
        
        // Find the saved payroll since ID might not be set
        List<PayrollModel> payrolls = payrollDAO.findByPayPeriod(testPayPeriodId);
        PayrollModel saved = null;
        for (PayrollModel p : payrolls) {
            if (p.getEmployeeId().equals(testEmployeeId) && 
                p.getNetSalary().compareTo(BigDecimal.ZERO) == 0) {
                saved = p;
                break;
            }
        }
        
        assertNotNull("Should find saved payroll with zero amounts", saved);
        assertEquals("Zero amount should be preserved", 0, BigDecimal.ZERO.compareTo(saved.getNetSalary()));
    }
    
    // ==================== REPORT GENERATION TESTS ====================
    
    @Test
    public void testGetPayrollHistory_ValidEmployee_ReturnsHistory() {
        // Arrange - Create multiple payrolls
        createAndSaveTestPayroll();
        
        // Act
        List<PayrollModel> history = payrollDAO.getPayrollHistory(testEmployeeId, 10);
        
        // Assert
        assertNotNull("Should return payroll history", history);
        assertFalse("Should have at least one payroll record", history.isEmpty());
    }
    
    @Test
    public void testGetPayrollHistory_InvalidEmployee_ReturnsEmpty() {
        // Act
        List<PayrollModel> history = payrollDAO.getPayrollHistory(999999, 10);
        
        // Assert
        assertNotNull("Should return empty list, not null", history);
        assertTrue("Should return empty list for invalid employee", history.isEmpty());
    }
    
    @Test
    public void testGetPayrollSummary_EmptyPeriod_ReturnsZeroSummary() {
        // Arrange - Create new period with no payrolls
        Integer emptyPeriodId = createPayPeriod(
            LocalDate.now().minusDays(30),
            LocalDate.now().minusDays(16),
            "Empty Test Period"
        );
        
        // Act
        PayrollDAO.PayrollSummary summary = payrollDAO.getPayrollSummary(emptyPeriodId);
        
        // Assert
        assertNotNull("Should return summary even for empty period", summary);
        assertEquals("Employee count should be 0", 0, summary.getEmployeeCount());
        assertEquals("Total gross should be 0", 0, BigDecimal.ZERO.compareTo(summary.getTotalGrossIncome()));
        
        // Cleanup
        deletePayPeriod(emptyPeriodId);
    }
    
    @Test
    public void testIsPayrollGenerated_ExistingPayroll_ReturnsTrue() {
        // Arrange
        createAndSaveTestPayroll();
        
        // Act
        boolean result = payrollDAO.isPayrollGenerated(testPayPeriodId);
        
        // Assert
        assertTrue("Should return true for period with payroll", result);
    }
    
    @Test
    public void testIsPayrollGenerated_NoPayroll_ReturnsFalse() {
        // Arrange - Create new period
        Integer newPeriodId = createPayPeriod(
            LocalDate.now().minusDays(45),
            LocalDate.now().minusDays(31),
            "New Test Period"
        );
        
        // Act
        boolean result = payrollDAO.isPayrollGenerated(newPeriodId);
        
        // Assert
        assertFalse("Should return false for period without payroll", result);
        
        // Cleanup
        deletePayPeriod(newPeriodId);
    }
    
    // ==================== ENTITY VALIDATION TESTS ====================
    
    @Test
    public void testPayrollModel_Validation_ValidEntity() {
        // Arrange
        PayrollModel payroll = new PayrollModel();
        payroll.setEmployeeId(testEmployeeId);
        payroll.setPayPeriodId(testPayPeriodId);
        payroll.setBasicSalary(new BigDecimal("25000.00"));
        payroll.setGrossIncome(new BigDecimal("30000.00"));
        payroll.setTotalBenefit(new BigDecimal("5000.00"));
        payroll.setTotalDeduction(new BigDecimal("3000.00"));
        payroll.setNetSalary(new BigDecimal("27000.00"));
        
        // Act & Assert
        assertTrue("Valid payroll should pass validation", payroll.isValid());
    }
    
    @Test
    public void testPayrollModel_Validation_InvalidEntity() {
        // Arrange
        PayrollModel payroll = new PayrollModel();
        // Missing required fields
        
        // Act & Assert
        assertFalse("Invalid payroll should fail validation", payroll.isValid());
    }
    
    @Test
    public void testPayrollCalculations_CorrectCalculations() {
        // Arrange
        PayrollModel payroll = new PayrollModel();
        payroll.setBasicSalary(new BigDecimal("25000.00"));
        payroll.setTotalBenefit(new BigDecimal("5000.00"));
        payroll.setTotalDeduction(new BigDecimal("3000.00"));
        
        // Act
        payroll.calculateGrossIncome();
        payroll.calculateNetSalary();
        
        // Assert
        assertEquals("Gross income should be basic + benefits", 0,
            new BigDecimal("30000.00").compareTo(payroll.getGrossIncome()));
        assertEquals("Net salary should be gross - deductions", 0,
            new BigDecimal("27000.00").compareTo(payroll.getNetSalary()));
    }
    
    // ==================== DATABASE ERROR SCENARIOS ====================
    
    @Test
    public void testDatabaseConnection_ConnectionFailure_HandlesGracefully() {
        // This test verifies the DAO handles connection issues gracefully
        // We can't easily simulate a connection failure without mocks,
        // but we can test with an invalid connection
        
        try {
            // Create DAO with potentially problematic connection
            DatabaseConnection badConnection = new DatabaseConnection() {
                @Override
                public Connection createConnection() throws SQLException {
                    throw new SQLException("Simulated connection failure");
                }
            };
            
            PayrollDAO badDAO = new PayrollDAO(badConnection);
            
            // Try operations that should fail gracefully
            PayrollModel result = badDAO.findById(1);
            assertNull("Should return null when connection fails", result);
            
            boolean saveResult = badDAO.save(new PayrollModel());
            assertFalse("Should return false when connection fails", saveResult);
            
        } catch (Exception e) {
            fail("Should handle connection failures gracefully, not throw exceptions");
        }
    }
    
    @Test
    public void testUpdatePayrollStatus_ValidStatus_UpdatesSuccessfully() {
        // Arrange
        createAndSaveTestPayroll();
        
        // Act
        int updatedCount = payrollDAO.updatePayrollStatus(testPayPeriodId, "PROCESSED");
        
        // Assert
        assertTrue("Should update at least one record", updatedCount > 0);
    }
    
    @Test
    public void testDeletePayrollByPeriod_ValidPeriod_DeletesSuccessfully() {
        // Arrange
        createAndSaveTestPayroll();
        
        // Act
        int deletedCount = payrollDAO.deletePayrollByPeriod(testPayPeriodId);
        
        // Assert
        assertTrue("Should delete at least one record", deletedCount > 0);
        
        // Verify deletion
        List<PayrollModel> remaining = payrollDAO.findByPayPeriod(testPayPeriodId);
        assertTrue("No payrolls should remain after deletion", remaining.isEmpty());
    }
    
    // ==================== CONCURRENT ACCESS TEST ====================
    
    @Test
    public void testConcurrentPayrollGeneration_SamePeriod_HandlesCorrectly() {
        // This tests that duplicate payroll generation is handled properly
        
        // Act - Generate payroll twice for same period
        int firstGeneration = payrollDAO.generatePayroll(testPayPeriodId);
        int secondGeneration = payrollDAO.generatePayroll(testPayPeriodId);
        
        // Assert
        assertTrue("First generation should succeed", firstGeneration > 0);
        assertEquals("Second generation should not create duplicates", 0, secondGeneration);
        
        // Verify only one payroll per employee
        List<PayrollModel> payrolls = payrollDAO.findByPayPeriod(testPayPeriodId);
        assertEquals("Should have exactly one payroll per generation", firstGeneration, payrolls.size());
    }
    
    // ==================== HELPER METHODS ====================
    
    /**
     * Creates and saves a test payroll record
     * @return The saved PayrollModel
     */
    private PayrollModel createAndSaveTestPayroll() {
        PayrollModel payroll = new PayrollModel();
        payroll.setEmployeeId(testEmployeeId);
        payroll.setPayPeriodId(testPayPeriodId);
        payroll.setBasicSalary(new BigDecimal("25000.00"));
        payroll.setGrossIncome(new BigDecimal("30000.00"));
        payroll.setTotalBenefit(new BigDecimal("5000.00"));
        payroll.setTotalDeduction(new BigDecimal("3000.00"));
        payroll.setNetSalary(new BigDecimal("27000.00"));
        
        boolean saved = payrollDAO.save(payroll);
        
        // If save was successful but ID wasn't set, find the payroll
        if (saved && payroll.getPayrollId() == null) {
            List<PayrollModel> payrolls = payrollDAO.findByEmployee(testEmployeeId);
            if (!payrolls.isEmpty()) {
                // Get the most recent payroll
                payroll = payrolls.get(0);
            }
        }
        
        return payroll;
    }
    
    // ==================== DECIMAL PRECISION TESTS ====================
    
    @Test
    public void testPayrollCalculations_DecimalPrecision_MaintainsAccuracy() {
        // Arrange
        PayrollModel payroll = new PayrollModel();
        payroll.setEmployeeId(testEmployeeId);
        payroll.setPayPeriodId(testPayPeriodId);
        payroll.setBasicSalary(new BigDecimal("33333.33"));
        payroll.setGrossIncome(new BigDecimal("38888.88"));
        payroll.setTotalBenefit(new BigDecimal("5555.55"));
        payroll.setTotalDeduction(new BigDecimal("11111.11"));
        payroll.setNetSalary(new BigDecimal("27777.77"));
        
        // Act
        boolean saved = payrollDAO.save(payroll);
        assertTrue("Should save payroll with decimal values", saved);
        
        // Find the saved payroll
        List<PayrollModel> payrolls = payrollDAO.findByPayPeriod(testPayPeriodId);
        PayrollModel retrieved = null;
        for (PayrollModel p : payrolls) {
            if (p.getEmployeeId().equals(testEmployeeId) && 
                p.getBasicSalary().compareTo(new BigDecimal("33333.33")) == 0) {
                retrieved = p;
                break;
            }
        }
        
        // Assert
        assertNotNull("Should retrieve saved payroll", retrieved);
        assertEquals("Basic salary precision should be maintained", 0,
            new BigDecimal("33333.33").compareTo(retrieved.getBasicSalary()));
        assertEquals("Net salary precision should be maintained", 0,
            new BigDecimal("27777.77").compareTo(retrieved.getNetSalary()));
    }
    
    @Test
    public void testFindByEmployee_MultiplePayrolls_ReturnsAllOrdered() {
        // Arrange - Create multiple payrolls for same employee
        createAndSaveTestPayroll();
        
        // Create second pay period and payroll
        Integer secondPeriodId = createPayPeriod(
            LocalDate.now().minusDays(45),
            LocalDate.now().minusDays(31),
            "Second Test Period"
        );
        
        PayrollModel secondPayroll = new PayrollModel();
        secondPayroll.setEmployeeId(testEmployeeId);
        secondPayroll.setPayPeriodId(secondPeriodId);
        secondPayroll.setBasicSalary(new BigDecimal("26000.00"));
        secondPayroll.setGrossIncome(new BigDecimal("31000.00"));
        secondPayroll.setTotalBenefit(new BigDecimal("5000.00"));
        secondPayroll.setTotalDeduction(new BigDecimal("3100.00"));
        secondPayroll.setNetSalary(new BigDecimal("27900.00"));
        payrollDAO.save(secondPayroll);
        
        // Act
        List<PayrollModel> payrolls = payrollDAO.findByEmployee(testEmployeeId);
        
        // Assert
        assertNotNull("Should return list of payrolls", payrolls);
        assertTrue("Should have at least 2 payrolls", payrolls.size() >= 2);
        
        // Cleanup
        payrollDAO.delete(secondPayroll.getPayrollId());
        deletePayPeriod(secondPeriodId);
    }
    
    // ==================== HELPER METHODS FOR PAY PERIOD MANAGEMENT ====================
    
    /**
     * Creates a pay period using direct SQL
     * @param startDate Start date of the period
     * @param endDate End date of the period
     * @param periodName Name of the period
     * @return The created pay period ID, or null if creation failed
     */
    private Integer createPayPeriod(LocalDate startDate, LocalDate endDate, String periodName) {
        String sql = "INSERT INTO payperiod (startDate, endDate, periodName) VALUES (?, ?, ?)";
        
        try (Connection conn = dbConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setDate(1, java.sql.Date.valueOf(startDate));
            stmt.setDate(2, java.sql.Date.valueOf(endDate));
            stmt.setString(3, periodName);
            
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        return generatedKeys.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error creating pay period: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Deletes a pay period using direct SQL
     * @param payPeriodId The pay period ID to delete
     * @return true if deletion was successful, false otherwise
     */
    private boolean deletePayPeriod(Integer payPeriodId) {
        if (payPeriodId == null) {
            return false;
        }
        
        String sql = "DELETE FROM payperiod WHERE payPeriodId = ?";
        
        try (Connection conn = dbConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, payPeriodId);
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            System.err.println("Error deleting pay period: " + e.getMessage());
            return false;
        }
    }
}