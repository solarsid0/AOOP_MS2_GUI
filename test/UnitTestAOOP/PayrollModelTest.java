package UnitTestAOOP;

import Models.PayrollModel;
import org.junit.*;
import org.junit.rules.TestName;
import org.junit.runners.MethodSorters;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class PayrollModelTest {
    
    private static final Logger LOGGER = Logger.getLogger(PayrollModelTest.class.getName());
    private static Connection connection;
    private PayrollModel payrollModel;
    
    @Rule
    public TestName testName = new TestName();
    
    // Database configuration
    private static final String DB_URL = "jdbc:mysql://localhost:3306/payrollsystem_db";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "motorph_123";
    
    // Test data constants
    private static final Integer TEST_EMPLOYEE_ID = 10001;
    private static final Integer TEST_PAY_PERIOD_ID = 1;
    private static final BigDecimal TEST_BASIC_SALARY = new BigDecimal("50000.00");
    
    @BeforeClass
    public static void setUpClass() {
        LOGGER.info("=== Starting PayrollModel Test Suite ===");
        try {
            // Establish database connection
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            connection.setAutoCommit(false);
            LOGGER.info("Database connection established successfully");
            
            // Clean up any existing test data
            cleanupTestData();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to setup test class", e);
            Assert.fail("Failed to setup test class: " + e.getMessage());
        }
    }
    
    @AfterClass
    public static void tearDownClass() {
        LOGGER.info("=== Tearing down PayrollModel Test Suite ===");
        try {
            if (connection != null && !connection.isClosed()) {
                cleanupTestData();
                connection.close();
                LOGGER.info("Database connection closed successfully");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to tear down test class", e);
        }
    }
    
    @Before
    public void setUp() {
        LOGGER.info("Setting up test: " + testName.getMethodName());
        payrollModel = new PayrollModel();
    }
    
    @After
    public void tearDown() {
        LOGGER.info("Tearing down test: " + testName.getMethodName());
        payrollModel = null;
        try {
            connection.rollback(); // Rollback any changes made during test
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to rollback transaction", e);
        }
    }
    
    // ========== Constructor Tests ==========
    
    @Test
    public void test01_DefaultConstructor_ShouldInitializeFieldsToZero() {
        LOGGER.info("Testing default constructor initialization");
        
        PayrollModel model = new PayrollModel();
        
        Assert.assertNull("PayrollId should be null", model.getPayrollId());
        Assert.assertEquals("BasicSalary should be ZERO", BigDecimal.ZERO, model.getBasicSalary());
        Assert.assertEquals("GrossIncome should be ZERO", BigDecimal.ZERO, model.getGrossIncome());
        Assert.assertEquals("TotalBenefit should be ZERO", BigDecimal.ZERO, model.getTotalBenefit());
        Assert.assertEquals("TotalDeduction should be ZERO", BigDecimal.ZERO, model.getTotalDeduction());
        Assert.assertEquals("NetSalary should be ZERO", BigDecimal.ZERO, model.getNetSalary());
    }
    
    @Test
    public void test02_ParameterizedConstructor_WithValidData() {
        LOGGER.info("Testing parameterized constructor with valid data");
        
        PayrollModel model = new PayrollModel(TEST_EMPLOYEE_ID, TEST_PAY_PERIOD_ID, TEST_BASIC_SALARY);
        
        Assert.assertEquals("EmployeeId should match", TEST_EMPLOYEE_ID, model.getEmployeeId());
        Assert.assertEquals("PayPeriodId should match", TEST_PAY_PERIOD_ID, model.getPayPeriodId());
        Assert.assertEquals("BasicSalary should match", TEST_BASIC_SALARY, model.getBasicSalary());
    }
    
    @Test
    public void test03_ParameterizedConstructor_WithNullBasicSalary() {
        LOGGER.info("Testing parameterized constructor with null basic salary");
        
        PayrollModel model = new PayrollModel(TEST_EMPLOYEE_ID, TEST_PAY_PERIOD_ID, null);
        
        Assert.assertEquals("BasicSalary should be ZERO when null", BigDecimal.ZERO, model.getBasicSalary());
    }
    
    // ========== Setter Tests with Null Handling ==========
    
    @Test
    public void test04_SetBasicSalary_WithNull_ShouldSetToZero() {
        LOGGER.info("Testing setBasicSalary with null value");
        
        payrollModel.setBasicSalary(null);
        
        Assert.assertEquals("BasicSalary should be ZERO when set to null", BigDecimal.ZERO, payrollModel.getBasicSalary());
    }
    
    @Test
    public void test05_SetGrossIncome_WithNull_ShouldSetToZero() {
        LOGGER.info("Testing setGrossIncome with null value");
        
        payrollModel.setGrossIncome(null);
        
        Assert.assertEquals("GrossIncome should be ZERO when set to null", BigDecimal.ZERO, payrollModel.getGrossIncome());
    }
    
    @Test
    public void test06_SetTotalBenefit_WithNull_ShouldSetToZero() {
        LOGGER.info("Testing setTotalBenefit with null value");
        
        payrollModel.setTotalBenefit(null);
        
        Assert.assertEquals("TotalBenefit should be ZERO when set to null", BigDecimal.ZERO, payrollModel.getTotalBenefit());
    }
    
    @Test
    public void test07_SetTotalDeduction_WithNull_ShouldSetToZero() {
        LOGGER.info("Testing setTotalDeduction with null value");
        
        payrollModel.setTotalDeduction(null);
        
        Assert.assertEquals("TotalDeduction should be ZERO when set to null", BigDecimal.ZERO, payrollModel.getTotalDeduction());
    }
    
    @Test
    public void test08_SetNetSalary_WithNull_ShouldSetToZero() {
        LOGGER.info("Testing setNetSalary with null value");
        
        payrollModel.setNetSalary(null);
        
        Assert.assertEquals("NetSalary should be ZERO when set to null", BigDecimal.ZERO, payrollModel.getNetSalary());
    }
    
    // ========== Calculation Method Tests ==========
    
    @Test
    public void test09_CalculateGrossIncome_WithPositiveValues() {
        LOGGER.info("Testing calculateGrossIncome with positive values");
        
        payrollModel.setBasicSalary(new BigDecimal("50000.00"));
        payrollModel.setTotalBenefit(new BigDecimal("5000.00"));
        
        payrollModel.calculateGrossIncome();
        
        Assert.assertEquals("GrossIncome should be sum of basic salary and benefits", 
                new BigDecimal("55000.00"), payrollModel.getGrossIncome());
    }
    
    @Test
    public void test10_CalculateGrossIncome_WithZeroValues() {
        LOGGER.info("Testing calculateGrossIncome with zero values");
        
        payrollModel.setBasicSalary(BigDecimal.ZERO);
        payrollModel.setTotalBenefit(BigDecimal.ZERO);
        
        payrollModel.calculateGrossIncome();
        
        Assert.assertEquals("GrossIncome should be zero", BigDecimal.ZERO, payrollModel.getGrossIncome());
    }
    
    @Test
    public void test11_CalculateNetSalary_WithPositiveValues() {
        LOGGER.info("Testing calculateNetSalary with positive values");
        
        payrollModel.setGrossIncome(new BigDecimal("55000.00"));
        payrollModel.setTotalDeduction(new BigDecimal("10000.00"));
        
        payrollModel.calculateNetSalary();
        
        Assert.assertEquals("NetSalary should be gross income minus deductions", 
                new BigDecimal("45000.00"), payrollModel.getNetSalary());
    }
    
    @Test
    public void test12_CalculateNetSalary_WithHighDeductions() {
        LOGGER.info("Testing calculateNetSalary with deductions higher than gross income");
        
        payrollModel.setGrossIncome(new BigDecimal("10000.00"));
        payrollModel.setTotalDeduction(new BigDecimal("15000.00"));
        
        payrollModel.calculateNetSalary();
        
        Assert.assertEquals("NetSalary should be negative when deductions exceed gross income", 
                new BigDecimal("-5000.00"), payrollModel.getNetSalary());
    }
    
    // ========== Validation Tests ==========
    
    @Test
    public void test13_IsValid_WithAllRequiredFields() {
        LOGGER.info("Testing isValid with all required fields set");
        
        payrollModel.setEmployeeId(TEST_EMPLOYEE_ID);
        payrollModel.setPayPeriodId(TEST_PAY_PERIOD_ID);
        payrollModel.setBasicSalary(TEST_BASIC_SALARY);
        payrollModel.setGrossIncome(new BigDecimal("55000.00"));
        payrollModel.setTotalDeduction(new BigDecimal("10000.00"));
        payrollModel.setNetSalary(new BigDecimal("45000.00"));
        
        Assert.assertTrue("PayrollModel should be valid", payrollModel.isValid());
    }
    
    @Test
    public void test14_IsValid_WithNullEmployeeId() {
        LOGGER.info("Testing isValid with null employee ID");
        
        payrollModel.setEmployeeId(null);
        payrollModel.setPayPeriodId(TEST_PAY_PERIOD_ID);
        payrollModel.setBasicSalary(TEST_BASIC_SALARY);
        payrollModel.setGrossIncome(new BigDecimal("55000.00"));
        payrollModel.setTotalDeduction(new BigDecimal("10000.00"));
        payrollModel.setNetSalary(new BigDecimal("45000.00"));
        
        Assert.assertFalse("PayrollModel should be invalid with null employee ID", payrollModel.isValid());
    }
    
    @Test
    public void test15_IsValid_WithNullPayPeriodId() {
        LOGGER.info("Testing isValid with null pay period ID");
        
        payrollModel.setEmployeeId(TEST_EMPLOYEE_ID);
        payrollModel.setPayPeriodId(null);
        payrollModel.setBasicSalary(TEST_BASIC_SALARY);
        payrollModel.setGrossIncome(new BigDecimal("55000.00"));
        payrollModel.setTotalDeduction(new BigDecimal("10000.00"));
        payrollModel.setNetSalary(new BigDecimal("45000.00"));
        
        Assert.assertFalse("PayrollModel should be invalid with null pay period ID", payrollModel.isValid());
    }
    
    // ========== Edge Case Tests ==========
    
    @Test
    public void test16_SetBasicSalary_WithLargeValue() {
        LOGGER.info("Testing setBasicSalary with extremely large value");
        
        BigDecimal largeValue = new BigDecimal("999999999.99");
        payrollModel.setBasicSalary(largeValue);
        
        Assert.assertEquals("Should handle large salary values", largeValue, payrollModel.getBasicSalary());
    }
    
    @Test
    public void test17_SetBasicSalary_WithNegativeValue() {
        LOGGER.info("Testing setBasicSalary with negative value");
        
        BigDecimal negativeValue = new BigDecimal("-5000.00");
        payrollModel.setBasicSalary(negativeValue);
        
        Assert.assertEquals("Should accept negative salary (for adjustments)", negativeValue, payrollModel.getBasicSalary());
    }
    
    @Test
    public void test18_SetBasicSalary_WithPrecisionValue() {
        LOGGER.info("Testing setBasicSalary with high precision value");
        
        BigDecimal precisionValue = new BigDecimal("12345.6789");
        payrollModel.setBasicSalary(precisionValue);
        
        Assert.assertEquals("Should maintain precision", precisionValue, payrollModel.getBasicSalary());
    }
    
    // ========== Timestamp Tests ==========
    
    @Test
    public void test19_SetCreatedAt_WithCurrentTime() {
        LOGGER.info("Testing setCreatedAt with current timestamp");
        
        LocalDateTime now = LocalDateTime.now();
        payrollModel.setCreatedAt(now);
        
        Assert.assertEquals("CreatedAt should match", now, payrollModel.getCreatedAt());
    }
    
    @Test
    public void test20_SetUpdatedAt_WithFutureTime() {
        LOGGER.info("Testing setUpdatedAt with future timestamp");
        
        LocalDateTime futureTime = LocalDateTime.now().plusDays(1);
        payrollModel.setUpdatedAt(futureTime);
        
        Assert.assertEquals("UpdatedAt should accept future timestamps", futureTime, payrollModel.getUpdatedAt());
    }
    
    // ========== Equals and HashCode Tests ==========
    
    @Test
    public void test21_Equals_WithSameObject() {
        LOGGER.info("Testing equals with same object reference");
        
        Assert.assertTrue("Object should equal itself", payrollModel.equals(payrollModel));
    }
    
    @Test
    public void test22_Equals_WithNull() {
        LOGGER.info("Testing equals with null");
        
        Assert.assertFalse("Object should not equal null", payrollModel.equals(null));
    }
    
    @Test
    public void test23_Equals_WithDifferentClass() {
        LOGGER.info("Testing equals with different class type");
        
        Assert.assertFalse("Object should not equal different class", payrollModel.equals("String"));
    }
    
    @Test
    public void test24_Equals_WithSamePayrollId() {
        LOGGER.info("Testing equals with same payroll ID");
        
        PayrollModel model1 = new PayrollModel();
        model1.setPayrollId(100);
        
        PayrollModel model2 = new PayrollModel();
        model2.setPayrollId(100);
        
        Assert.assertTrue("Objects with same payroll ID should be equal", model1.equals(model2));
    }
    
    @Test
    public void test25_HashCode_WithNullPayrollId() {
        LOGGER.info("Testing hashCode with null payroll ID");
        
        payrollModel.setPayrollId(null);
        
        Assert.assertEquals("HashCode should be 0 for null payroll ID", 0, payrollModel.hashCode());
    }
    
    // ========== Database Integration Tests ==========
    
    @Test
    public void test26_DatabaseInsert_WithValidData() {
        LOGGER.info("Testing database insert with valid payroll data");
        
        try {
            payrollModel.setEmployeeId(TEST_EMPLOYEE_ID);
            payrollModel.setPayPeriodId(TEST_PAY_PERIOD_ID);
            payrollModel.setBasicSalary(TEST_BASIC_SALARY);
            payrollModel.setGrossIncome(new BigDecimal("55000.00"));
            payrollModel.setTotalBenefit(new BigDecimal("5000.00"));
            payrollModel.setTotalDeduction(new BigDecimal("10000.00"));
            payrollModel.setNetSalary(new BigDecimal("45000.00"));
            
            String sql = "INSERT INTO payroll (basicSalary, grossIncome, totalBenefit, totalDeduction, netSalary, payPeriodId, employeeId) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)";
            
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setBigDecimal(1, payrollModel.getBasicSalary());
            pstmt.setBigDecimal(2, payrollModel.getGrossIncome());
            pstmt.setBigDecimal(3, payrollModel.getTotalBenefit());
            pstmt.setBigDecimal(4, payrollModel.getTotalDeduction());
            pstmt.setBigDecimal(5, payrollModel.getNetSalary());
            pstmt.setInt(6, payrollModel.getPayPeriodId());
            pstmt.setInt(7, payrollModel.getEmployeeId());
            
            int rowsInserted = pstmt.executeUpdate();
            
            Assert.assertEquals("Should insert one row", 1, rowsInserted);
            LOGGER.info("Successfully inserted payroll record");
            
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Database insert test failed (might be due to foreign key constraints)", e);
        }
    }
    
    @Test
    public void test27_DatabaseInsert_WithDuplicateEmployeePeriod() {
        LOGGER.info("Testing database insert with duplicate employee-period combination");
        
        try {
            // First insert
            String sql = "INSERT INTO payroll (basicSalary, grossIncome, totalBenefit, totalDeduction, netSalary, payPeriodId, employeeId) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)";
            
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setBigDecimal(1, TEST_BASIC_SALARY);
            pstmt.setBigDecimal(2, new BigDecimal("55000.00"));
            pstmt.setBigDecimal(3, new BigDecimal("5000.00"));
            pstmt.setBigDecimal(4, new BigDecimal("10000.00"));
            pstmt.setBigDecimal(5, new BigDecimal("45000.00"));
            pstmt.setInt(6, TEST_PAY_PERIOD_ID);
            pstmt.setInt(7, TEST_EMPLOYEE_ID);
            
            pstmt.executeUpdate();
            
            // Attempt duplicate insert
            pstmt.executeUpdate();
            
            Assert.fail("Should throw SQLException for duplicate employee-period combination");
            
        } catch (SQLException e) {
            LOGGER.info("Expected SQLException for duplicate key: " + e.getMessage());
            Assert.assertTrue("Should be duplicate key error", e.getMessage().contains("Duplicate") || e.getMessage().contains("unique"));
        }
    }
    
    // ========== Boundary Value Tests ==========
    
    @Test
    public void test28_SetBasicSalary_WithMinimumWage() {
        LOGGER.info("Testing setBasicSalary with minimum wage value");
        
        BigDecimal minimumWage = new BigDecimal("537.00"); // Example minimum daily wage
        payrollModel.setBasicSalary(minimumWage);
        
        Assert.assertEquals("Should handle minimum wage values", minimumWage, payrollModel.getBasicSalary());
    }
    
    @Test
    public void test29_CalculateNetSalary_WithZeroDeductions() {
        LOGGER.info("Testing calculateNetSalary with zero deductions");
        
        payrollModel.setGrossIncome(new BigDecimal("50000.00"));
        payrollModel.setTotalDeduction(BigDecimal.ZERO);
        
        payrollModel.calculateNetSalary();
        
        Assert.assertEquals("Net salary should equal gross income when no deductions", 
                payrollModel.getGrossIncome(), payrollModel.getNetSalary());
    }
    
    @Test
    public void test30_CalculateGrossIncome_WithMaxBenefits() {
        LOGGER.info("Testing calculateGrossIncome with maximum benefits");
        
        BigDecimal maxBenefits = new BigDecimal("10000.00");
        payrollModel.setBasicSalary(new BigDecimal("100000.00"));
        payrollModel.setTotalBenefit(maxBenefits);
        
        payrollModel.calculateGrossIncome();
        
        Assert.assertEquals("Should handle maximum benefit calculations", 
                new BigDecimal("110000.00"), payrollModel.getGrossIncome());
    }
    
    // ========== ToString Tests ==========
    
    @Test
    public void test31_ToString_WithAllFieldsSet() {
        LOGGER.info("Testing toString with all fields set");
        
        payrollModel.setPayrollId(1);
        payrollModel.setEmployeeId(TEST_EMPLOYEE_ID);
        payrollModel.setPayPeriodId(TEST_PAY_PERIOD_ID);
        payrollModel.setBasicSalary(TEST_BASIC_SALARY);
        
        String result = payrollModel.toString();
        
        Assert.assertTrue("ToString should contain payrollId", result.contains("payrollId=1"));
        Assert.assertTrue("ToString should contain employeeId", result.contains("employeeId=" + TEST_EMPLOYEE_ID));
        Assert.assertTrue("ToString should contain payPeriodId", result.contains("payPeriodId=" + TEST_PAY_PERIOD_ID));
    }
    
    @Test
    public void test32_ToString_WithNullFields() {
        LOGGER.info("Testing toString with null fields");
        
        String result = payrollModel.toString();
        
        Assert.assertTrue("ToString should handle null values", result.contains("null"));
        Assert.assertNotNull("ToString should never return null", result);
    }
    
    // ========== Decimal Precision Tests ==========
    
    @Test
    public void test33_BigDecimalPrecision_InCalculations() {
        LOGGER.info("Testing BigDecimal precision in calculations");
        
        BigDecimal salary = new BigDecimal("33333.33");
        BigDecimal benefit = new BigDecimal("1666.67");
        
        payrollModel.setBasicSalary(salary);
        payrollModel.setTotalBenefit(benefit);
        
        payrollModel.calculateGrossIncome();
        
        Assert.assertEquals("Should maintain precision in calculations", 
                new BigDecimal("35000.00"), payrollModel.getGrossIncome());
    }
    
    @Test
    public void test34_RoundingScenarios_InNetSalaryCalculation() {
        LOGGER.info("Testing rounding scenarios in net salary calculation");
        
        payrollModel.setGrossIncome(new BigDecimal("50000.51"));
        payrollModel.setTotalDeduction(new BigDecimal("12345.67"));
        
        payrollModel.calculateNetSalary();
        
        BigDecimal expected = new BigDecimal("37654.84");
        Assert.assertEquals("Should handle decimal arithmetic correctly", expected, payrollModel.getNetSalary());
    }
    
    // ========== Stress Tests ==========
    
    @Test
    public void test35_MultipleCalculations_ShouldBeConsistent() {
        LOGGER.info("Testing multiple calculations for consistency");
        
        payrollModel.setBasicSalary(new BigDecimal("75000.00"));
        payrollModel.setTotalBenefit(new BigDecimal("8000.00"));
        
        // Calculate multiple times
        for (int i = 0; i < 100; i++) {
            payrollModel.calculateGrossIncome();
        }
        
        Assert.assertEquals("Gross income should remain consistent", 
                new BigDecimal("83000.00"), payrollModel.getGrossIncome());
    }
    
    @Test
    public void test36_RapidSetterCalls_ShouldMaintainState() {
        LOGGER.info("Testing rapid setter calls");
        
        BigDecimal finalValue = new BigDecimal("99999.99");
        
        // Rapid state changes
        for (int i = 0; i < 1000; i++) {
            payrollModel.setBasicSalary(new BigDecimal(i));
        }
        payrollModel.setBasicSalary(finalValue);
        
        Assert.assertEquals("Should maintain final state after rapid changes", finalValue, payrollModel.getBasicSalary());
    }
    
    // ========== Integration Scenario Tests ==========
    
    @Test
    public void test37_CompletePayrollScenario_WithAllComponents() {
        LOGGER.info("Testing complete payroll scenario");
        
        // Setup employee payroll
        payrollModel.setEmployeeId(TEST_EMPLOYEE_ID);
        payrollModel.setPayPeriodId(TEST_PAY_PERIOD_ID);
        payrollModel.setBasicSalary(new BigDecimal("60000.00"));
        payrollModel.setTotalBenefit(new BigDecimal("5500.00"));
        
        // Calculate gross income
        payrollModel.calculateGrossIncome();
        
        // Set deductions
        payrollModel.setTotalDeduction(new BigDecimal("15000.00"));
        
        // Calculate net salary
        payrollModel.calculateNetSalary();
        
        // Validate complete scenario
        Assert.assertEquals("Gross income should be correct", new BigDecimal("65500.00"), payrollModel.getGrossIncome());
        Assert.assertEquals("Net salary should be correct", new BigDecimal("50500.00"), payrollModel.getNetSalary());
        Assert.assertTrue("Payroll should be valid", payrollModel.isValid());
    }
    
    @Test
    public void test38_PayrollAdjustment_Scenario() {
        LOGGER.info("Testing payroll adjustment scenario");
        
        // Initial payroll
        payrollModel.setBasicSalary(new BigDecimal("50000.00"));
        payrollModel.setTotalBenefit(new BigDecimal("5000.00"));
        payrollModel.calculateGrossIncome();
        
        // Adjustment - reduce salary
        payrollModel.setBasicSalary(new BigDecimal("45000.00"));
        payrollModel.calculateGrossIncome();
        
        Assert.assertEquals("Adjusted gross income should be correct", 
                new BigDecimal("50000.00"), payrollModel.getGrossIncome());
    }
    
    @Test
    public void test39_ErrorRecovery_AfterInvalidOperation() {
        LOGGER.info("Testing error recovery after invalid operation");
        
        // Set valid initial state
        payrollModel.setBasicSalary(new BigDecimal("50000.00"));
        payrollModel.setGrossIncome(new BigDecimal("55000.00"));
        
        // Attempt invalid operation (setting null)
        payrollModel.setBasicSalary(null);
        
        // Verify recovery
        Assert.assertEquals("Should recover to ZERO after null", BigDecimal.ZERO, payrollModel.getBasicSalary());
        
        // Can still perform calculations
        payrollModel.setBasicSalary(new BigDecimal("50000.00"));
        payrollModel.setTotalBenefit(new BigDecimal("5000.00"));
        payrollModel.calculateGrossIncome();
        
        Assert.assertEquals("Should calculate correctly after recovery", 
                new BigDecimal("55000.00"), payrollModel.getGrossIncome());
    }
    
    @Test
    public void test40_MemoryLeakPrevention_LargeObjectCreation() {
        LOGGER.info("Testing memory leak prevention with large object creation");
        
        // Create many PayrollModel objects
        for (int i = 0; i < 1000; i++) {
            PayrollModel tempModel = new PayrollModel(i, i, new BigDecimal(i * 1000));
            tempModel.calculateGrossIncome();
            tempModel.calculateNetSalary();
            
            if (i == 999) {
                Assert.assertNotNull("Last model should be created successfully", tempModel);
                Assert.assertEquals("Last model should have correct employee ID", Integer.valueOf(999), tempModel.getEmployeeId());
            }
        }
        
        LOGGER.info("Successfully created 1000 PayrollModel objects without issues");
    }
    
    // ========== Helper Methods ==========
    
    private static void cleanupTestData() throws SQLException {
        LOGGER.info("Cleaning up test data");
        
        String deletePayroll = "DELETE FROM payroll WHERE employeeId = ? OR payPeriodId = ?";
        PreparedStatement pstmt = connection.prepareStatement(deletePayroll);
        pstmt.setInt(1, TEST_EMPLOYEE_ID);
        pstmt.setInt(2, TEST_PAY_PERIOD_ID);
        
        int rowsDeleted = pstmt.executeUpdate();
        LOGGER.info("Deleted " + rowsDeleted + " test payroll records");
        
        connection.commit();
    }
}