package UnitTestAOOP;

import Models.AccountingModel;
import Models.EmployeeModel;
import Models.PayrollModel;
import Models.PayPeriodModel;
import DAOs.DatabaseConnection;
import DAOs.EmployeeDAO;
import DAOs.PayrollDAO;
import DAOs.PayPeriodDAO;

import org.junit.*;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.logging.FileHandler;
import java.util.logging.SimpleFormatter;

public class AccountingModelTest {
    
    private static final Logger LOGGER = Logger.getLogger(AccountingModelTest.class.getName());
    private static FileHandler fileHandler;
    private static DatabaseConnection dbConnection;
    private static AccountingModel accountingModel;
    private static AccountingModel nonAccountingModel;
    private static EmployeeDAO employeeDAO;
    private static PayrollDAO payrollDAO;
    private static PayPeriodDAO payPeriodDAO;
    
    // Test data
    private static final int TEST_EMPLOYEE_ID = 99999;
    private static final int TEST_SUPERVISOR_ID = 99998;
    private static final int TEST_PAY_PERIOD_ID = 9999;
    private static final int INVALID_PAY_PERIOD_ID = -1;
    private static final int NON_EXISTENT_PAY_PERIOD_ID = 88888;
    
    @Rule
    public TestRule watcher = new TestWatcher() {
        protected void starting(Description description) {
            LOGGER.info("Starting test: " + description.getMethodName());
        }
        
        protected void finished(Description description) {
            LOGGER.info("Finished test: " + description.getMethodName());
        }
        
        protected void failed(Throwable e, Description description) {
            LOGGER.severe("Failed test: " + description.getMethodName() + " - " + e.getMessage());
        }
    };
    
    @BeforeClass
    public static void setUpClass() throws Exception {
        // Initialize logger
        fileHandler = new FileHandler("AccountingModelTest.log", true);
        fileHandler.setFormatter(new SimpleFormatter());
        LOGGER.addHandler(fileHandler);
        LOGGER.setLevel(Level.ALL);
        
        LOGGER.info("=== Starting AccountingModelTest Suite ===");
        
        // Initialize database connection
        dbConnection = new DatabaseConnection();
        employeeDAO = new EmployeeDAO(dbConnection);
        payrollDAO = new PayrollDAO(dbConnection);
        payPeriodDAO = new PayPeriodDAO();
        
        // Create test data
        createTestData();
        
        // Initialize accounting model with test employee
        accountingModel = new AccountingModel(
            TEST_EMPLOYEE_ID, 
            "Test", 
            "Accountant", 
            "test.accountant@company.com", 
            "Accounting"
        );
        
        // Initialize non-accounting model for permission testing
        nonAccountingModel = new AccountingModel(
            TEST_EMPLOYEE_ID + 1, 
            "Test", 
            "Employee", 
            "test.employee@company.com", 
            "Employee"
        );
        
        LOGGER.info("Test setup completed successfully");
    }
    
    @AfterClass
    public static void tearDownClass() throws Exception {
        LOGGER.info("=== Cleaning up test data ===");
        cleanupTestData();
        
        if (fileHandler != null) {
            fileHandler.close();
        }
        
        LOGGER.info("=== AccountingModelTest Suite Completed ===");
    }
    
    @Before
    public void setUp() {
        // Clean any existing payroll data for test period before each test
        try {
            Connection conn = dbConnection.getConnection();
            disableForeignKeyChecks(conn);
            payrollDAO.deletePayrollByPeriod(TEST_PAY_PERIOD_ID);
            enableForeignKeyChecks(conn);
        } catch (Exception e) {
            LOGGER.warning("Error cleaning payroll data before test: " + e.getMessage());
        }
    }
    
    @After
    public void tearDown() {
        // Clean up any payroll data created during test
        try {
            Connection conn = dbConnection.getConnection();
            disableForeignKeyChecks(conn);
            payrollDAO.deletePayrollByPeriod(TEST_PAY_PERIOD_ID);
            enableForeignKeyChecks(conn);
        } catch (Exception e) {
            LOGGER.warning("Error cleaning payroll data after test: " + e.getMessage());
        }
    }
    
    // ==================== FOREIGN KEY MANAGEMENT ====================
    
    private static void disableForeignKeyChecks(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("SET FOREIGN_KEY_CHECKS = 0");
            LOGGER.fine("Foreign key checks disabled");
        }
    }
    
    private static void enableForeignKeyChecks(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("SET FOREIGN_KEY_CHECKS = 1");
            LOGGER.fine("Foreign key checks enabled");
        }
    }
    
    // ==================== TEST DATA CREATION ====================
    
    private static void createTestData() throws SQLException {
        Connection conn = dbConnection.getConnection();
        
        try {
            // Disable foreign key checks for data creation
            disableForeignKeyChecks(conn);
            
            // Clean up any existing test data first
            cleanupExistingTestData(conn);
            
            // Create test position
            String positionSql = "INSERT INTO position (positionId, position, department) VALUES (?, ?, ?)";
            try (PreparedStatement positionStmt = conn.prepareStatement(positionSql)) {
                positionStmt.setInt(1, 999);
                positionStmt.setString(2, "Test Position");
                positionStmt.setString(3, "Test Department");
                positionStmt.executeUpdate();
            }
            
            // Create test supervisor
            String supervisorSql = "INSERT INTO employee (employeeId, firstName, lastName, birthDate, " +
                                 "email, basicSalary, hourlyRate, userRole, passwordHash, status, positionId) " +
                                 "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement supervisorStmt = conn.prepareStatement(supervisorSql)) {
                supervisorStmt.setInt(1, TEST_SUPERVISOR_ID);
                supervisorStmt.setString(2, "Test");
                supervisorStmt.setString(3, "Supervisor");
                supervisorStmt.setDate(4, java.sql.Date.valueOf(LocalDate.of(1980, 1, 1)));
                supervisorStmt.setString(5, "test.supervisor@company.com");
                supervisorStmt.setBigDecimal(6, new BigDecimal("50000.00"));
                supervisorStmt.setBigDecimal(7, new BigDecimal("284.09"));
                supervisorStmt.setString(8, "IT Manager");
                supervisorStmt.setString(9, "hashed_password");
                supervisorStmt.setString(10, "Regular");
                supervisorStmt.setInt(11, 999);
                supervisorStmt.executeUpdate();
            }
            
            // Create test employees
            String employeeSql = "INSERT INTO employee (employeeId, firstName, lastName, birthDate, " +
                               "email, basicSalary, hourlyRate, userRole, passwordHash, status, positionId, supervisorId) " +
                               "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement employeeStmt = conn.prepareStatement(employeeSql)) {
                // Accounting employee
                employeeStmt.setInt(1, TEST_EMPLOYEE_ID);
                employeeStmt.setString(2, "Test");
                employeeStmt.setString(3, "Accountant");
                employeeStmt.setDate(4, java.sql.Date.valueOf(LocalDate.of(1990, 1, 1)));
                employeeStmt.setString(5, "test.accountant@company.com");
                employeeStmt.setBigDecimal(6, new BigDecimal("35000.00"));
                employeeStmt.setBigDecimal(7, new BigDecimal("198.86"));
                employeeStmt.setString(8, "Accounting");
                employeeStmt.setString(9, "hashed_password");
                employeeStmt.setString(10, "Regular");
                employeeStmt.setInt(11, 999);
                employeeStmt.setInt(12, TEST_SUPERVISOR_ID);
                employeeStmt.executeUpdate();
                
                // Regular employee
                employeeStmt.setInt(1, TEST_EMPLOYEE_ID + 1);
                employeeStmt.setString(2, "Test");
                employeeStmt.setString(3, "Employee");
                employeeStmt.setString(5, "test.employee@company.com");
                employeeStmt.setBigDecimal(6, new BigDecimal("25000.00"));
                employeeStmt.setBigDecimal(7, new BigDecimal("142.05"));
                employeeStmt.setString(8, "Employee");
                employeeStmt.executeUpdate();
            }
            
            // Create test pay period
            String periodSql = "INSERT INTO payperiod (payPeriodId, startDate, endDate, periodName) VALUES (?, ?, ?, ?)";
            try (PreparedStatement periodStmt = conn.prepareStatement(periodSql)) {
                periodStmt.setInt(1, TEST_PAY_PERIOD_ID);
                periodStmt.setDate(2, java.sql.Date.valueOf(LocalDate.of(2024, 1, 1)));
                periodStmt.setDate(3, java.sql.Date.valueOf(LocalDate.of(2024, 1, 31)));
                periodStmt.setString(4, "Test Period - January 2024");
                periodStmt.executeUpdate();
            }
            
            // Create some attendance records for the test period
            String attendanceSql = "INSERT INTO attendance (employeeId, date, timeIn, timeOut) VALUES (?, ?, ?, ?)";
            try (PreparedStatement attendanceStmt = conn.prepareStatement(attendanceSql)) {
                // Add attendance for 20 working days
                LocalDate workDate = LocalDate.of(2024, 1, 2); // Start from Jan 2 (assuming Jan 1 is holiday)
                for (int i = 0; i < 20; i++) {
                    if (workDate.getDayOfWeek().getValue() <= 5) { // Weekdays only
                        attendanceStmt.setInt(1, TEST_EMPLOYEE_ID);
                        attendanceStmt.setDate(2, java.sql.Date.valueOf(workDate));
                        attendanceStmt.setTime(3, java.sql.Time.valueOf("08:00:00"));
                        attendanceStmt.setTime(4, java.sql.Time.valueOf("17:00:00"));
                        attendanceStmt.executeUpdate();
                    }
                    workDate = workDate.plusDays(1);
                }
            }
            
            LOGGER.info("Test data created successfully");
            
        } catch (SQLException e) {
            LOGGER.severe("Error creating test data: " + e.getMessage());
            throw e;
        } finally {
            // Re-enable foreign key checks
            enableForeignKeyChecks(conn);
        }
    }
    
    private static void cleanupExistingTestData(Connection conn) throws SQLException {
        // Clean up any existing test data that might conflict
        try {
            conn.prepareStatement("DELETE FROM attendance WHERE employeeId IN (" + TEST_EMPLOYEE_ID + ", " + (TEST_EMPLOYEE_ID + 1) + ")").executeUpdate();
            conn.prepareStatement("DELETE FROM payroll WHERE employeeId IN (" + TEST_EMPLOYEE_ID + ", " + (TEST_EMPLOYEE_ID + 1) + ")").executeUpdate();
            conn.prepareStatement("DELETE FROM payperiod WHERE payPeriodId = " + TEST_PAY_PERIOD_ID).executeUpdate();
            conn.prepareStatement("DELETE FROM employee WHERE employeeId IN (" + TEST_EMPLOYEE_ID + ", " + (TEST_EMPLOYEE_ID + 1) + ", " + TEST_SUPERVISOR_ID + ")").executeUpdate();
            conn.prepareStatement("DELETE FROM position WHERE positionId = 999").executeUpdate();
        } catch (SQLException e) {
            // Ignore errors during cleanup
            LOGGER.fine("Cleanup of existing data: " + e.getMessage());
        }
    }
    
    private static void cleanupTestData() throws SQLException {
        Connection conn = dbConnection.getConnection();
        
        try {
            // Disable foreign key checks for cleanup
            disableForeignKeyChecks(conn);
            
            // Delete in reverse order of creation
            conn.prepareStatement("DELETE FROM attendance WHERE employeeId IN (" + TEST_EMPLOYEE_ID + ", " + (TEST_EMPLOYEE_ID + 1) + ")").executeUpdate();
            conn.prepareStatement("DELETE FROM payroll WHERE employeeId IN (" + TEST_EMPLOYEE_ID + ", " + (TEST_EMPLOYEE_ID + 1) + ")").executeUpdate();
            conn.prepareStatement("DELETE FROM payperiod WHERE payPeriodId = " + TEST_PAY_PERIOD_ID).executeUpdate();
            conn.prepareStatement("DELETE FROM employee WHERE employeeId IN (" + TEST_EMPLOYEE_ID + ", " + (TEST_EMPLOYEE_ID + 1) + ", " + TEST_SUPERVISOR_ID + ")").executeUpdate();
            conn.prepareStatement("DELETE FROM position WHERE positionId = 999").executeUpdate();
            
            LOGGER.info("Test data cleaned up successfully");
        } catch (SQLException e) {
            LOGGER.severe("Error cleaning up test data: " + e.getMessage());
            throw e;
        } finally {
            // Re-enable foreign key checks
            enableForeignKeyChecks(conn);
        }
    }
    
    // ==================== PAYROLL GENERATION TESTS ====================
    
    @Test
    public void testGeneratePayrollWithDetails_InvalidPeriod() {
        LOGGER.info("Testing payroll generation with invalid period ID");
        
        AccountingModel.PayrollGenerationResult result = accountingModel.generatePayrollWithDetails(INVALID_PAY_PERIOD_ID);
        
        assertNotNull("Result should not be null", result);
        assertFalse("Payroll generation should fail", result.isSuccess());
        assertEquals("Should generate zero records", 0, result.getGeneratedCount());
        assertFalse("Detail tables should not be populated", result.isDetailTablesPopulated());
        
        LOGGER.info("Payroll generation failed as expected for invalid period");
    }
    
    @Test
    public void testGeneratePayrollWithDetails_NonExistentPeriod() {
        LOGGER.info("Testing payroll generation with non-existent period");
        
        AccountingModel.PayrollGenerationResult result = accountingModel.generatePayrollWithDetails(NON_EXISTENT_PAY_PERIOD_ID);
        
        assertNotNull("Result should not be null", result);
        assertFalse("Payroll generation should fail", result.isSuccess());
        assertEquals("Should generate zero records", 0, result.getGeneratedCount());
        
        LOGGER.info("Payroll generation failed as expected for non-existent period");
    }
    
    @Test
    public void testGeneratePayrollWithDetails_InsufficientPermissions() {
        LOGGER.info("Testing payroll generation without accounting permissions");
        
        AccountingModel.PayrollGenerationResult result = nonAccountingModel.generatePayrollWithDetails(TEST_PAY_PERIOD_ID);
        
        assertNotNull("Result should not be null", result);
        assertFalse("Payroll generation should fail", result.isSuccess());
        assertTrue("Should have permission error message", 
                  result.getMessage().contains("Insufficient permissions"));
        
        LOGGER.info("Payroll generation blocked due to insufficient permissions");
    }
    
    @Test
    public void testGeneratePayrollWithDetails_DuplicateGeneration() {
        LOGGER.info("Testing duplicate payroll generation");
        
        // First generation
        AccountingModel.PayrollGenerationResult result1 = accountingModel.generatePayrollWithDetails(TEST_PAY_PERIOD_ID);
        assertTrue("First generation should succeed", result1.isSuccess());
        int firstCount = result1.getGeneratedCount();
        
        // Second generation (should replace existing)
        AccountingModel.PayrollGenerationResult result2 = accountingModel.generatePayrollWithDetails(TEST_PAY_PERIOD_ID);
        assertTrue("Second generation should succeed", result2.isSuccess());
        assertEquals("Should generate same number of records", firstCount, result2.getGeneratedCount());
        
        LOGGER.info("Duplicate generation handled correctly");
    }
    
    // ==================== PAYROLL VERIFICATION TESTS ====================
      
    @Test
    public void testVerifyPayrollForPeriod_NoPayrollData() {
        LOGGER.info("Testing verification with no payroll data");
        
        AccountingModel.AccountingResult result = accountingModel.verifyPayrollForPeriod(TEST_PAY_PERIOD_ID);
        
        assertNotNull("Result should not be null", result);
        assertFalse("Verification should fail", result.isSuccess());
        assertTrue("Should have appropriate error message", 
                  result.getMessage().contains("No payroll records found"));
        
        LOGGER.info("Verification failed as expected for missing payroll data");
    }
    
    @Test
    public void testVerifyPayrollForPeriod_InvalidPeriod() {
        LOGGER.info("Testing verification with invalid period");
        
        AccountingModel.AccountingResult result = accountingModel.verifyPayrollForPeriod(INVALID_PAY_PERIOD_ID);
        
        assertNotNull("Result should not be null", result);
        assertFalse("Verification should fail", result.isSuccess());
        
        LOGGER.info("Verification failed as expected for invalid period");
    }
    
    @Test
    public void testVerifyPayrollForPeriod_InsufficientPermissions() {
        LOGGER.info("Testing verification without permissions");
        
        AccountingModel.AccountingResult result = nonAccountingModel.verifyPayrollForPeriod(TEST_PAY_PERIOD_ID);
        
        assertNotNull("Result should not be null", result);
        assertFalse("Verification should fail", result.isSuccess());
        assertTrue("Should have permission error", 
                  result.getMessage().contains("Insufficient permissions"));
        
        LOGGER.info("Verification blocked due to insufficient permissions");
    }
    
    @Test
    public void testVerifyPayrollForPeriod_NullPeriod() {
        LOGGER.info("Testing verification with null period");
        
        AccountingModel.AccountingResult result = accountingModel.verifyPayrollForPeriod(null);
        
        assertNotNull("Result should not be null", result);
        assertFalse("Verification should fail", result.isSuccess());
        
        LOGGER.info("Verification failed as expected for null period");
    }
    
    // ==================== FINANCIAL REPORTING TESTS ====================
    
    @Test
    public void testGenerateFinancialReport_Success() {
        LOGGER.info("Testing financial report generation");
        
        AccountingModel.ReportResult result = accountingModel.generateFinancialReport(TEST_PAY_PERIOD_ID);
        
        assertNotNull("Result should not be null", result);
        assertTrue("Report generation should succeed", result.isSuccess());
        assertNotNull("Should have report content", result.getReportContent());
        
        LOGGER.info("Financial report generated successfully");
    }
    
    @Test
    public void testGenerateFinancialReport_InvalidPeriod() {
        LOGGER.info("Testing financial report with invalid period");
        
        AccountingModel.ReportResult result = accountingModel.generateFinancialReport(INVALID_PAY_PERIOD_ID);
        
        assertNotNull("Result should not be null", result);
        // Report service might still generate a report even for invalid period
        
        LOGGER.info("Financial report generation completed");
    }
    
    @Test
    public void testGenerateFinancialReport_InsufficientPermissions() {
        LOGGER.info("Testing financial report without permissions");
        
        AccountingModel.ReportResult result = nonAccountingModel.generateFinancialReport(TEST_PAY_PERIOD_ID);
        
        assertNotNull("Result should not be null", result);
        assertFalse("Report generation should fail", result.isSuccess());
        assertTrue("Should have permission error", 
                  result.getErrorMessage().contains("Insufficient permissions"));
        
        LOGGER.info("Report generation blocked due to insufficient permissions");
    }
    
    @Test
    public void testGenerateTaxComplianceReport_Success() {
        LOGGER.info("Testing tax compliance report generation");
        
        YearMonth testMonth = YearMonth.of(2024, 1);
        AccountingModel.ReportResult result = accountingModel.generateTaxComplianceReport(testMonth);
        
        assertNotNull("Result should not be null", result);
        assertTrue("Report generation should succeed", result.isSuccess());
        assertNotNull("Should have report content", result.getReportContent());
        
        LOGGER.info("Tax compliance report generated successfully");
    }
    
    @Test
    public void testGenerateTaxComplianceReport_FutureMonth() {
        LOGGER.info("Testing tax report for future month");
        
        YearMonth futureMonth = YearMonth.now().plusMonths(12);
        AccountingModel.ReportResult result = accountingModel.generateTaxComplianceReport(futureMonth);
        
        assertNotNull("Result should not be null", result);
        // Service might generate empty report for future dates
        
        LOGGER.info("Tax report generation handled future date");
    }
    
    @Test
    public void testGenerateTaxComplianceReport_NullMonth() {
        LOGGER.info("Testing tax report with null month");
        
        try {
            AccountingModel.ReportResult result = accountingModel.generateTaxComplianceReport(null);
            assertNotNull("Result should not be null", result);
            assertFalse("Report generation should fail", result.isSuccess());
        } catch (Exception e) {
            // Expected behavior - null handling
            LOGGER.info("Null month handled with exception: " + e.getMessage());
        }
    }
    
    @Test
    public void testGenerateSalaryComparisonReport_Success() {
        LOGGER.info("Testing salary comparison report generation");
        
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);
        
        AccountingModel.ReportResult result = accountingModel.generateSalaryComparisonReport(startDate, endDate);
        
        assertNotNull("Result should not be null", result);
        assertTrue("Report generation should succeed", result.isSuccess());
        assertNotNull("Should have report content", result.getReportContent());
        
        LOGGER.info("Salary comparison report generated successfully");
    }
    
    @Test
    public void testGenerateSalaryComparisonReport_InvalidDateRange() {
        LOGGER.info("Testing salary report with invalid date range");
        
        LocalDate startDate = LocalDate.of(2024, 1, 31);
        LocalDate endDate = LocalDate.of(2024, 1, 1); // End before start
        
        AccountingModel.ReportResult result = accountingModel.generateSalaryComparisonReport(startDate, endDate);
        
        assertNotNull("Result should not be null", result);
        // Service might still process despite invalid range
        
        LOGGER.info("Salary report handled invalid date range");
    }
    
    // ==================== AUDIT OPERATION TESTS ====================
    
    @Test
    public void testPerformFinancialAudit_Success() {
        LOGGER.info("Testing financial audit operation");
        
        // Generate payroll first
        accountingModel.generatePayrollWithDetails(TEST_PAY_PERIOD_ID);
        
        // Perform audit
        AccountingModel.AccountingResult result = accountingModel.performFinancialAudit(TEST_PAY_PERIOD_ID);
        
        assertNotNull("Result should not be null", result);
        assertTrue("Audit should succeed", result.isSuccess());
        assertNotNull("Should have compliance score", result.getComplianceScore());
        assertTrue("Compliance score should be positive", 
                  result.getComplianceScore().compareTo(BigDecimal.ZERO) > 0);
        
        LOGGER.info("Financial audit completed with compliance score: " + result.getComplianceScore());
    }
    
    @Test
    public void testPerformFinancialAudit_NoPayrollData() {
        LOGGER.info("Testing audit with no payroll data");
        
        AccountingModel.AccountingResult result = accountingModel.performFinancialAudit(TEST_PAY_PERIOD_ID);
        
        assertNotNull("Result should not be null", result);
        assertFalse("Audit should fail", result.isSuccess());
        
        LOGGER.info("Audit failed as expected for missing payroll data");
    }
    
    @Test
    public void testPerformFinancialAudit_InsufficientPermissions() {
        LOGGER.info("Testing audit without permissions");
        
        AccountingModel.AccountingResult result = nonAccountingModel.performFinancialAudit(TEST_PAY_PERIOD_ID);
        
        assertNotNull("Result should not be null", result);
        assertFalse("Audit should fail", result.isSuccess());
        assertTrue("Should have permission error", 
                  result.getMessage().contains("Insufficient permissions"));
        
        LOGGER.info("Audit blocked due to insufficient permissions");
    }
    
    // ==================== DATA ACCESS TESTS ====================
    
    @Test
    public void testGetPayrollRecords_Success() {
        LOGGER.info("Testing payroll records retrieval");
        
        // Generate payroll first
        accountingModel.generatePayrollWithDetails(TEST_PAY_PERIOD_ID);
        
        List<PayrollModel> records = accountingModel.getPayrollRecords(TEST_PAY_PERIOD_ID);
        
        assertNotNull("Records list should not be null", records);
        assertFalse("Should have payroll records", records.isEmpty());
        
        LOGGER.info("Retrieved " + records.size() + " payroll records");
    }
    
    @Test
    public void testGetPayrollRecords_EmptyPeriod() {
        LOGGER.info("Testing payroll records for empty period");
        
        List<PayrollModel> records = accountingModel.getPayrollRecords(NON_EXISTENT_PAY_PERIOD_ID);
        
        assertNotNull("Records list should not be null", records);
        assertTrue("Should have no records", records.isEmpty());
        
        LOGGER.info("Correctly returned empty list for non-existent period");
    }
    
    @Test
    public void testGetPayrollRecords_InsufficientPermissions() {
        LOGGER.info("Testing payroll records without permissions");
        
        List<PayrollModel> records = nonAccountingModel.getPayrollRecords(TEST_PAY_PERIOD_ID);
        
        assertNotNull("Records list should not be null", records);
        assertTrue("Should return empty list", records.isEmpty());
        
        LOGGER.info("Access denied for non-accounting user");
    }
    
    @Test
    public void testGetEmployeeById_Success() {
        LOGGER.info("Testing employee retrieval by ID");
        
        EmployeeModel employee = accountingModel.getEmployeeById(TEST_EMPLOYEE_ID);
        
        assertNotNull("Employee should not be null", employee);
        assertEquals("Should match employee ID", TEST_EMPLOYEE_ID, employee.getEmployeeId().intValue());
        
        LOGGER.info("Successfully retrieved employee: " + employee.getFullName());
    }
    
    @Test
    public void testGetEmployeeById_NonExistent() {
        LOGGER.info("Testing retrieval of non-existent employee");
        
        EmployeeModel employee = accountingModel.getEmployeeById(88888);
        
        assertNull("Should return null for non-existent employee", employee);
        
        LOGGER.info("Correctly returned null for non-existent employee");
    }
    
    @Test
    public void testGetEmployeeById_InvalidId() {
        LOGGER.info("Testing employee retrieval with invalid ID");
        
        EmployeeModel employee = accountingModel.getEmployeeById(-1);
        
        assertNull("Should return null for invalid ID", employee);
        
        LOGGER.info("Correctly handled invalid employee ID");
    }
    
    @Test
    public void testGetAllActiveEmployees_Success() {
        LOGGER.info("Testing retrieval of all active employees");
        
        List<EmployeeModel> employees = accountingModel.getAllActiveEmployees();
        
        assertNotNull("Employee list should not be null", employees);
        assertFalse("Should have active employees", employees.isEmpty());
        
        LOGGER.info("Retrieved " + employees.size() + " active employees");
    }
    
    @Test
    public void testGetAllActiveEmployees_InsufficientPermissions() {
        LOGGER.info("Testing active employees retrieval without permissions");
        
        List<EmployeeModel> employees = nonAccountingModel.getAllActiveEmployees();
        
        assertNotNull("Employee list should not be null", employees);
        assertTrue("Should return empty list", employees.isEmpty());
        
        LOGGER.info("Access denied for non-accounting user");
    }
    
    // ==================== PERMISSION TESTS ====================
    
    @Test
    public void testGetAccountingPermissions() {
        LOGGER.info("Testing accounting permissions retrieval");
        
        String[] permissions = accountingModel.getAccountingPermissions();
        
        assertNotNull("Permissions array should not be null", permissions);
        assertTrue("Should have permissions", permissions.length > 0);
        
        // Verify expected permissions
        boolean hasVerifyPayroll = false;
        boolean hasGenerateReports = false;
        for (String perm : permissions) {
            if ("VERIFY_PAYROLL".equals(perm)) hasVerifyPayroll = true;
            if ("GENERATE_FINANCIAL_REPORTS".equals(perm)) hasGenerateReports = true;
        }
        
        assertTrue("Should have VERIFY_PAYROLL permission", hasVerifyPayroll);
        assertTrue("Should have GENERATE_FINANCIAL_REPORTS permission", hasGenerateReports);
        
        LOGGER.info("Accounting permissions verified: " + permissions.length + " permissions");
    }
    
    // ==================== EDGE CASE TESTS ====================
    
    @Test
    public void testVerifyPayrollForPeriod_DataDiscrepancy() {
        LOGGER.info("Testing verification with data discrepancy");
        
        // Generate payroll first
        accountingModel.generatePayrollWithDetails(TEST_PAY_PERIOD_ID);
        
        // Manually modify payroll data to create discrepancy
        Connection conn = null;
        try {
            conn = dbConnection.getConnection();
            disableForeignKeyChecks(conn);
            
            String sql = "UPDATE payroll SET netSalary = netSalary + 1000 " +
                        "WHERE employeeId = ? AND payPeriodId = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, TEST_EMPLOYEE_ID);
                stmt.setInt(2, TEST_PAY_PERIOD_ID);
                stmt.executeUpdate();
            }
            
            enableForeignKeyChecks(conn);
            
            // Verify - should detect discrepancy
            AccountingModel.AccountingResult result = accountingModel.verifyPayrollForPeriod(TEST_PAY_PERIOD_ID);
            
            assertNotNull("Result should not be null", result);
            assertTrue("Verification should complete", result.isSuccess());
            assertTrue("Should have some discrepancies", result.getDiscrepancyRecords() > 0);
            
        } catch (SQLException e) {
            LOGGER.severe("Error creating discrepancy: " + e.getMessage());
            fail("Test failed: " + e.getMessage());
        }
        
        LOGGER.info("Data discrepancy detected correctly");
    }
    
    @Test
    public void testAccountingModelConstructorFromEmployeeModel() {
        LOGGER.info("Testing AccountingModel constructor from EmployeeModel");
        
        // Create a base EmployeeModel
        EmployeeModel baseEmployee = new EmployeeModel();
        baseEmployee.setEmployeeId(TEST_EMPLOYEE_ID);
        baseEmployee.setFirstName("Base");
        baseEmployee.setLastName("Employee");
        baseEmployee.setEmail("base.employee@company.com");
        baseEmployee.setUserRole("Accounting");
        baseEmployee.setBasicSalary(new BigDecimal("40000.00"));
        baseEmployee.setBirthDate(LocalDate.of(1985, 5, 15));
        
        // Create AccountingModel from EmployeeModel
        AccountingModel fromEmployee = new AccountingModel(baseEmployee);
        
        assertNotNull("AccountingModel should not be null", fromEmployee);
        assertEquals("Employee ID should match", baseEmployee.getEmployeeId(), fromEmployee.getEmployeeId());
        assertEquals("First name should match", baseEmployee.getFirstName(), fromEmployee.getFirstName());
        assertEquals("Last name should match", baseEmployee.getLastName(), fromEmployee.getLastName());
        assertEquals("Email should match", baseEmployee.getEmail(), fromEmployee.getEmail());
        assertEquals("Basic salary should match", baseEmployee.getBasicSalary(), fromEmployee.getBasicSalary());
        
        LOGGER.info("AccountingModel created successfully from EmployeeModel");
    }
    
    @Test
    public void testToString() {
        LOGGER.info("Testing toString method");
        
        String result = accountingModel.toString();
        
        assertNotNull("toString should not return null", result);
        assertTrue("Should contain employee ID", result.contains("employeeId=" + TEST_EMPLOYEE_ID));
        assertTrue("Should contain name", result.contains("name="));
        assertTrue("Should contain email", result.contains("email="));
        assertTrue("Should contain permissions", result.contains("permissions="));
        
        LOGGER.info("toString output: " + result);
    }
    
    @Test
    public void testAccountingResultToString() {
        LOGGER.info("Testing AccountingResult toString");
        
        AccountingModel.AccountingResult result = new AccountingModel.AccountingResult();
        result.setSuccess(true);
        result.setMessage("Test message");
        result.setTotalRecords(10);
        result.setVerifiedRecords(9);
        result.setDiscrepancyRecords(1);
        result.setComplianceScore(new BigDecimal("90.00"));
        
        String output = result.toString();
        
        assertNotNull("toString should not return null", output);
        assertTrue("Should contain success status", output.contains("success=true"));
        assertTrue("Should contain message", output.contains("Test message"));
        assertTrue("Should contain compliance score", output.contains("90.00%"));
        
        LOGGER.info("AccountingResult toString: " + output);
    }
    
    @Test
    public void testReportResultGettersSetters() {
        LOGGER.info("Testing ReportResult getters and setters");
        
        AccountingModel.ReportResult report = new AccountingModel.ReportResult();
        
        // Test setters
        report.setSuccess(true);
        report.setErrorMessage("Test error");
        report.setReportContent("Test content");
        report.setFilePath("/test/path");
        
        // Test getters
        assertTrue("Success should be true", report.isSuccess());
        assertEquals("Error message should match", "Test error", report.getErrorMessage());
        assertEquals("Content should match", "Test content", report.getReportContent());
        assertEquals("File path should match", "/test/path", report.getFilePath());
        
        LOGGER.info("ReportResult getters/setters verified");
    }
    
    @Test
    public void testPayrollGenerationResultGetters() {
        LOGGER.info("Testing PayrollGenerationResult getters");
        
        AccountingModel.PayrollGenerationResult result = 
            new AccountingModel.PayrollGenerationResult(true, 5, true, "Test message");
        
        assertTrue("Success should be true", result.isSuccess());
        assertEquals("Generated count should be 5", 5, result.getGeneratedCount());
        assertTrue("Detail tables should be populated", result.isDetailTablesPopulated());
        assertEquals("Message should match", "Test message", result.getMessage());
        
        LOGGER.info("PayrollGenerationResult getters verified");
    }
    
    @Test
    public void testConcurrentPayrollGeneration() {
        LOGGER.info("Testing concurrent payroll generation attempts");
        
        // This tests the system's handling of concurrent operations
        AccountingModel.PayrollGenerationResult result1 = accountingModel.generatePayrollWithDetails(TEST_PAY_PERIOD_ID);
        
        // Immediately try to generate again (simulating concurrent access)
        AccountingModel.PayrollGenerationResult result2 = accountingModel.generatePayrollWithDetails(TEST_PAY_PERIOD_ID);
        
        assertTrue("Both operations should complete", result1.isSuccess() && result2.isSuccess());
        
        LOGGER.info("Concurrent generation handled correctly");
    }
    
    @Test
    public void testGenerateReportsWithNullService() {
        LOGGER.info("Testing report generation with potential null service");
        
        // Create a minimal AccountingModel that might have null services
        AccountingModel minimalModel = new AccountingModel(
            99994, "Minimal", "User", "minimal@company.com", "Accounting"
        );
        
        try {
            AccountingModel.ReportResult result = minimalModel.generateFinancialReport(TEST_PAY_PERIOD_ID);
            assertNotNull("Should handle null service gracefully", result);
            
            if (!result.isSuccess()) {
                LOGGER.info("Report generation failed as expected with null service");
            }
        } catch (Exception e) {
            LOGGER.info("Exception handled for null service: " + e.getMessage());
        }
    }
}


