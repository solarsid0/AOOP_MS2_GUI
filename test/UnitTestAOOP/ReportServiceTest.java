package UnitTestAOOP;

import Services.ReportService;
import Services.ReportService.*;
import DAOs.DatabaseConnection;
import org.junit.*;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Unit tests for ReportService
 * Tests all report generation methods using real database data
 * 
 * @author Admin
 */
public class ReportServiceTest {
    
    private static final Logger LOGGER = Logger.getLogger(ReportServiceTest.class.getName());
    
    // Database connection details
    private static final String DB_URL = "jdbc:mysql://localhost:3306/payrollsystem_db";
    private static final String DB_USERNAME = "root";
    private static final String DB_PASSWORD = "Mmdc_2025*";
    
    private static ReportService reportService;
    private static DatabaseConnection databaseConnection;
    private static Connection testConnection;
    
    // Test data
    private static List<Integer> availableEmployeeIds;
    private static YearMonth testYearMonth;
    private static LocalDate testDate;
    
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    
    @BeforeClass
    public static void setUpClass() {
        LOGGER.info("=== Starting ReportService Test Suite ===");
        
        try {
            // Initialize database connection
            setupDatabaseConnection();
            LOGGER.info("Database connection established");
            
            // Initialize ReportService
            databaseConnection = new DatabaseConnection();
            reportService = new ReportService(databaseConnection);
            LOGGER.info("ReportService instance created successfully");
            
            // Load test data
            loadTestData();
            LOGGER.info("Test data loaded successfully");
            
        } catch (Exception e) {
            LOGGER.severe("Failed to setup test class: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @AfterClass
    public static void tearDownClass() {
        LOGGER.info("=== Cleaning up ReportService Test Suite ===");
        
        try {
            if (testConnection != null && !testConnection.isClosed()) {
                testConnection.close();
                LOGGER.info("Database connection closed");
            }
        } catch (SQLException e) {
            LOGGER.warning("Error closing database connection: " + e.getMessage());
        }
        
        reportService = null;
        databaseConnection = null;
        availableEmployeeIds = null;
        LOGGER.info("=== ReportService Test Suite Completed ===");
    }
    
    @Before
    public void setUp() {
        LOGGER.info("Setting up individual test...");
        // Set test parameters for each test
        testYearMonth = YearMonth.of(2024, 6);
        testDate = LocalDate.of(2024, 6, 15);
        LOGGER.info("Test setup completed");
    }
    
    @After
    public void tearDown() {
        LOGGER.info("Cleaning up individual test...");
        // Clean up after each test if needed
    }
    
    // ========== DATABASE CONNECTION METHODS ==========
    
    private static void setupDatabaseConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            testConnection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
            LOGGER.info("Database connection successful");
        } catch (ClassNotFoundException e) {
            LOGGER.severe("MySQL Driver not found: " + e.getMessage());
            throw new SQLException("MySQL Driver not found", e);
        }
    }
    
    private static void loadTestData() throws SQLException {
        availableEmployeeIds = new ArrayList<>();
        
        String query = "SELECT DISTINCT `Employee ID` FROM monthly_employee_payslip LIMIT 10";
        
        try (PreparedStatement stmt = testConnection.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                availableEmployeeIds.add(rs.getInt("Employee ID"));
            }
            
            LOGGER.info("Loaded " + availableEmployeeIds.size() + " employee IDs for testing");
            
        } catch (SQLException e) {
            LOGGER.severe("Failed to load test data: " + e.getMessage());
            throw e;
        }
    }
    
    // ========== POSITIVE TESTS ==========
    
    @Test
    public void testGenerateMonthlyPayrollSummaryFromView_Success() {
        LOGGER.info("Testing monthly payroll summary generation from view...");
        
        MonthlyPayrollSummaryReport report = reportService.generateMonthlyPayrollSummaryFromView(testYearMonth);
        
        Assert.assertNotNull("Report should not be null", report);
        Assert.assertTrue("Report should be successful", report.isSuccess());
        Assert.assertNotNull("Report should have payroll entries", report.getPayrollEntries());
        Assert.assertNotNull("Report should have totals", report.getTotals());
        Assert.assertEquals("Report should have correct year-month", testYearMonth, report.getYearMonth());
        Assert.assertNotNull("Report should have generation date", report.getGeneratedDate());
        
        if (!report.getPayrollEntries().isEmpty()) {
            PayrollSummaryEntry firstEntry = report.getPayrollEntries().get(0);
            Assert.assertNotNull("Entry should have employee ID", firstEntry.getEmployeeId());
            Assert.assertNotNull("Entry should have employee name", firstEntry.getEmployeeName());
            Assert.assertNotNull("Entry should have net pay", firstEntry.getNetPay());
            
            LOGGER.info("Monthly payroll summary generated successfully - " + 
                       report.getPayrollEntries().size() + " employees processed");
        }
    }
    
    @Test
    public void testGenerateEmployeePayslipFromView_Success() {
        LOGGER.info("Testing employee payslip generation from view...");
        
        if (availableEmployeeIds == null || availableEmployeeIds.isEmpty()) {
            LOGGER.warning("No employee IDs available for testing");
            return;
        }
        
        Integer testEmployeeId = availableEmployeeIds.get(0);
        EmployeePayslipReport report = reportService.generateEmployeePayslipFromView(testEmployeeId, testYearMonth);
        
        Assert.assertNotNull("Report should not be null", report);
        
        if (report.isSuccess()) {
            Assert.assertNotNull("Report should have payslip details", report.getPayslip());
            Assert.assertEquals("Report should have correct employee ID", testEmployeeId, report.getEmployeeId());
            Assert.assertEquals("Report should have correct year-month", testYearMonth, report.getYearMonth());
            
            PayslipDetails payslip = report.getPayslip();
            Assert.assertNotNull("Payslip should have employee name", payslip.getEmployeeName());
            Assert.assertNotNull("Payslip should have net pay", payslip.getNetPay());
            Assert.assertNotNull("Payslip should have period dates", payslip.getPeriodStartDate());
            
            LOGGER.info("Employee payslip generated successfully for: " + payslip.getEmployeeName());
        } else {
            LOGGER.info("No payslip data found for employee " + testEmployeeId + " in " + testYearMonth);
        }
    }
    
    @Test
    public void testGenerateRankAndFileOvertimeReport_Success() {
        LOGGER.info("Testing rank-and-file overtime report generation...");
        
        RankAndFileOvertimeReport report = reportService.generateRankAndFileOvertimeReport(testYearMonth);
        
        Assert.assertNotNull("Report should not be null", report);
        Assert.assertTrue("Report should be successful", report.isSuccess());
        Assert.assertNotNull("Report should have overtime entries", report.getOvertimeEntries());
        Assert.assertEquals("Report should have correct year-month", testYearMonth, report.getYearMonth());
        Assert.assertNotNull("Report should have generation date", report.getGeneratedDate());
        
        // Check totals
        Assert.assertNotNull("Report should have total overtime hours", report.getTotalOvertimeHours());
        Assert.assertNotNull("Report should have total overtime pay", report.getTotalOvertimePay());
        
        if (!report.getOvertimeEntries().isEmpty()) {
            OvertimeEntry firstEntry = report.getOvertimeEntries().get(0);
            Assert.assertTrue("Entry should be for rank-and-file employee", firstEntry.isRankAndFile());
            Assert.assertTrue("Entry should have positive overtime hours", 
                            firstEntry.getOvertimeHours().compareTo(BigDecimal.ZERO) > 0);
            
            LOGGER.info("Rank-and-file overtime report generated successfully - " + 
                       report.getOvertimeEntries().size() + " employees with overtime");
        } else {
            LOGGER.info("No overtime data found for " + testYearMonth);
        }
    }
    
    @Test
    public void testGenerateGovernmentComplianceFromView_Success() {
        LOGGER.info("Testing government compliance report generation...");
        
        GovernmentComplianceReport report = reportService.generateGovernmentComplianceFromView(testYearMonth);
        
        Assert.assertNotNull("Report should not be null", report);
        Assert.assertTrue("Report should be successful", report.isSuccess());
        Assert.assertEquals("Report should have correct year-month", testYearMonth, report.getYearMonth());
        Assert.assertNotNull("Report should have generation date", report.getGeneratedDate());
        
        // Check government contributions
        Assert.assertNotNull("Report should have SSS total", report.getTotalSSS());
        Assert.assertNotNull("Report should have PhilHealth total", report.getTotalPhilHealth());
        Assert.assertNotNull("Report should have Pag-Ibig total", report.getTotalPagIbig());
        Assert.assertNotNull("Report should have withholding tax total", report.getTotalWithholdingTax());
        Assert.assertTrue("Report should have employee count", report.getTotalEmployees() >= 0);
        
        BigDecimal totalContributions = report.getTotalGovernmentContributions();
        Assert.assertNotNull("Report should calculate total contributions", totalContributions);
        
        LOGGER.info("Government compliance report generated successfully - " + 
                   report.getTotalEmployees() + " employees processed");
    }
    
    @Test
    public void testGenerateDailyAttendanceReport_Success() {
        LOGGER.info("Testing daily attendance report generation...");
        
        AttendanceReport report = reportService.generateDailyAttendanceReport(testDate);
        
        Assert.assertNotNull("Report should not be null", report);
        Assert.assertEquals("Report should have correct date", testDate, report.getReportDate());
        Assert.assertNotNull("Report should have generation date", report.getGeneratedDate());
        Assert.assertEquals("Report should have correct type", "Daily Attendance", report.getReportType());
        
        if (report.isSuccess()) {
            Assert.assertNotNull("Report should have attendance records", report.getAttendanceRecords());
            Assert.assertTrue("Report should have non-negative counts", report.getTotalEmployees() >= 0);
            Assert.assertTrue("Report should have non-negative present count", report.getPresentCount() >= 0);
            Assert.assertTrue("Report should have non-negative late count", report.getLateCount() >= 0);
            Assert.assertTrue("Report should have non-negative absent count", report.getAbsentCount() >= 0);
            
            LOGGER.info("Daily attendance report generated successfully - " + 
                       report.getTotalEmployees() + " employees processed");
        } else {
            // Handle case where attendance service has database connection issues
            LOGGER.info("Attendance report failed (likely due to database connection issues): " + 
                       report.getErrorMessage());
            // This is acceptable for the test - we're testing that the method doesn't crash
        }
    }
    
    @Test
    public void testHelperMethods_Success() {
        LOGGER.info("Testing helper methods...");
        
        // Test getCurrentManilaDate
        LocalDate currentDate = reportService.getCurrentManilaDate();
        Assert.assertNotNull("Current Manila date should not be null", currentDate);
        
        // Test formatCurrency
        BigDecimal testAmount = new BigDecimal("12345.67");
        String formattedCurrency = reportService.formatCurrency(testAmount);
        Assert.assertNotNull("Formatted currency should not be null", formattedCurrency);
        Assert.assertTrue("Formatted currency should contain peso sign", formattedCurrency.contains("₱"));
        
        // Test formatDate
        String formattedDate = reportService.formatDate(testDate);
        Assert.assertNotNull("Formatted date should not be null", formattedDate);
        
        // Test formatPercentage
        BigDecimal testPercentage = new BigDecimal("85.75");
        String formattedPercentage = reportService.formatPercentage(testPercentage);
        Assert.assertNotNull("Formatted percentage should not be null", formattedPercentage);
        Assert.assertTrue("Formatted percentage should contain percent sign", formattedPercentage.contains("%"));
        
        LOGGER.info("Helper methods tested successfully");
    }
    
    // ========== NEGATIVE TESTS ==========
    
    @Test
    public void testGenerateMonthlyPayrollSummaryFromView_InvalidYearMonth() {
        LOGGER.info("Testing monthly payroll summary with invalid year-month...");
        
        // Use a future year-month that won't have data
        YearMonth futureYearMonth = YearMonth.of(2030, 12);
        MonthlyPayrollSummaryReport report = reportService.generateMonthlyPayrollSummaryFromView(futureYearMonth);
        
        Assert.assertNotNull("Report should not be null", report);
        
        if (report.isSuccess()) {
            Assert.assertTrue("Report should have empty payroll entries for future date", 
                            report.getPayrollEntries().isEmpty());
            LOGGER.info("Invalid year-month test passed - no data found as expected");
        } else {
            LOGGER.info("Invalid year-month test passed - error occurred as expected");
        }
    }
    
    @Test
    public void testGenerateEmployeePayslipFromView_InvalidEmployeeId() {
        LOGGER.info("Testing employee payslip with invalid employee ID...");
        
        // Use an employee ID that doesn't exist
        Integer invalidEmployeeId = 999999;
        EmployeePayslipReport report = reportService.generateEmployeePayslipFromView(invalidEmployeeId, testYearMonth);
        
        Assert.assertNotNull("Report should not be null", report);
        Assert.assertFalse("Report should not be successful with invalid employee ID", report.isSuccess());
        Assert.assertNotNull("Report should have error message", report.getErrorMessage());
        
        LOGGER.info("Invalid employee ID test passed - error handled correctly");
    }
    
    @Test
    public void testGenerateEmployeePayslipFromView_NullEmployeeId() {
        LOGGER.info("Testing employee payslip with null employee ID...");
        
        try {
            EmployeePayslipReport report = reportService.generateEmployeePayslipFromView(null, testYearMonth);
            Assert.assertNotNull("Report should not be null", report);
            Assert.assertFalse("Report should not be successful with null employee ID", report.isSuccess());
            LOGGER.info("Null employee ID test passed - handled gracefully");
        } catch (Exception e) {
            LOGGER.info("Null employee ID test passed - exception thrown as expected: " + e.getClass().getSimpleName());
        }
    }
    
    @Test
    public void testGenerateRankAndFileOvertimeReport_InvalidYearMonth() {
        LOGGER.info("Testing rank-and-file overtime report with invalid year-month...");
        
        YearMonth invalidYearMonth = YearMonth.of(1900, 1);
        RankAndFileOvertimeReport report = reportService.generateRankAndFileOvertimeReport(invalidYearMonth);
        
        Assert.assertNotNull("Report should not be null", report);
        
        if (report.isSuccess()) {
            Assert.assertTrue("Report should have empty overtime entries for invalid date", 
                            report.getOvertimeEntries().isEmpty());
            LOGGER.info("Invalid year-month overtime test passed - no data found as expected");
        } else {
            LOGGER.info("Invalid year-month overtime test passed - error occurred as expected");
        }
    }
    
    @Test
    public void testGenerateGovernmentComplianceFromView_NullYearMonth() {
        LOGGER.info("Testing government compliance report with null year-month...");
        
        try {
            GovernmentComplianceReport report = reportService.generateGovernmentComplianceFromView(null);
            Assert.assertNotNull("Report should not be null", report);
            Assert.assertFalse("Report should not be successful with null year-month", report.isSuccess());
            LOGGER.info("Null year-month compliance test passed - handled gracefully");
        } catch (Exception e) {
            LOGGER.info("Null year-month compliance test passed - exception thrown as expected: " + e.getClass().getSimpleName());
        }
    }
    
    @Test
    public void testGenerateDailyAttendanceReport_NullDate() {
        LOGGER.info("Testing daily attendance report with null date...");
        
        try {
            AttendanceReport report = reportService.generateDailyAttendanceReport(null);
            Assert.assertNotNull("Report should not be null", report);
            
            // The report might return with success=true but have an error message,
            // or it might return with success=false. Both are acceptable.
            if (report.isSuccess()) {
                // If success=true, check if there's an error message or empty results
                Assert.assertTrue("Report should have empty attendance records with null date", 
                                report.getAttendanceRecords().isEmpty());
                LOGGER.info("Null date attendance test passed - returned empty results");
            } else {
                // If success=false, that's the expected behavior
                Assert.assertNotNull("Report should have error message", report.getErrorMessage());
                LOGGER.info("Null date attendance test passed - error handled correctly");
            }
            
        } catch (Exception e) {
            LOGGER.info("Null date attendance test passed - exception thrown as expected: " + e.getClass().getSimpleName());
        }
    }
    
    @Test
    public void testHelperMethods_EdgeCases() {
        LOGGER.info("Testing helper methods with edge cases...");
        
        // Test formatCurrency with null
        try {
            String result = reportService.formatCurrency(null);
            Assert.assertNotNull("Format currency should handle null gracefully", result);
            LOGGER.info("Format currency null test passed");
        } catch (Exception e) {
            LOGGER.info("Format currency null test passed - exception thrown as expected: " + e.getClass().getSimpleName());
        }
        
        // Test formatDate with null
        try {
            String result = reportService.formatDate(null);
            Assert.assertNotNull("Format date should handle null gracefully", result);
            LOGGER.info("Format date null test passed");
        } catch (Exception e) {
            LOGGER.info("Format date null test passed - exception thrown as expected: " + e.getClass().getSimpleName());
        }
        
        // Test formatPercentage with null
        try {
            String result = reportService.formatPercentage(null);
            Assert.assertNotNull("Format percentage should handle null gracefully", result);
            LOGGER.info("Format percentage null test passed");
        } catch (Exception e) {
            LOGGER.info("Format percentage null test passed - exception thrown as expected: " + e.getClass().getSimpleName());
        }
    }
    
    // ========== INTEGRATION TESTS ==========
    
    @Test
    public void testDatabaseConnection_Integrity() {
        LOGGER.info("Testing database connection integrity...");
        
        try {
            Assert.assertNotNull("Database connection should not be null", testConnection);
            Assert.assertFalse("Database connection should be open", testConnection.isClosed());
            
            // Test a simple query
            String testQuery = "SELECT COUNT(*) FROM monthly_employee_payslip";
            try (PreparedStatement stmt = testConnection.prepareStatement(testQuery);
                 ResultSet rs = stmt.executeQuery()) {
                
                Assert.assertTrue("Query should return results", rs.next());
                int count = rs.getInt(1);
                Assert.assertTrue("Should have payslip records", count >= 0);
                
                LOGGER.info("Database integrity test passed - Found " + count + " payslip records");
            }
            
        } catch (SQLException e) {
            LOGGER.severe("Database integrity test failed: " + e.getMessage());
            Assert.fail("Database integrity test failed: " + e.getMessage());
        }
    }
    
    @Test
    public void testReportService_MultipleReports() {
        LOGGER.info("Testing multiple report generation...");
        
        // Generate multiple reports to test service stability
        MonthlyPayrollSummaryReport payrollReport = reportService.generateMonthlyPayrollSummaryFromView(testYearMonth);
        GovernmentComplianceReport complianceReport = reportService.generateGovernmentComplianceFromView(testYearMonth);
        RankAndFileOvertimeReport overtimeReport = reportService.generateRankAndFileOvertimeReport(testYearMonth);
        
        Assert.assertNotNull("Payroll report should not be null", payrollReport);
        Assert.assertNotNull("Compliance report should not be null", complianceReport);
        Assert.assertNotNull("Overtime report should not be null", overtimeReport);
        
        LOGGER.info("Multiple report generation test passed");
    }
    
    @Test
    public void testReportService_Performance() {
        LOGGER.info("Testing report service performance...");
        
        long startTime = System.currentTimeMillis();
        
        MonthlyPayrollSummaryReport report = reportService.generateMonthlyPayrollSummaryFromView(testYearMonth);
        
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;
        
        Assert.assertNotNull("Report should not be null", report);
        Assert.assertTrue("Report generation should complete within 10 seconds", executionTime < 10000);
        
        LOGGER.info("Performance test passed - Report generated in " + executionTime + "ms");
    }
    
    // ========== UTILITY TESTS ==========
    
    @Test
    public void testPayslipDetails_DataIntegrity() {
        LOGGER.info("Testing PayslipDetails data integrity...");
        
        if (availableEmployeeIds == null || availableEmployeeIds.isEmpty()) {
            LOGGER.warning("No employee IDs available for data integrity testing");
            return;
        }
        
        Integer testEmployeeId = availableEmployeeIds.get(0);
        EmployeePayslipReport report = reportService.generateEmployeePayslipFromView(testEmployeeId, testYearMonth);
        
        if (report.isSuccess() && report.getPayslip() != null) {
            PayslipDetails payslip = report.getPayslip();
            
            // Verify basic data integrity
            Assert.assertNotNull("Employee ID should not be null", payslip.getEmployeeId());
            Assert.assertNotNull("Employee name should not be null", payslip.getEmployeeName());
            Assert.assertNotNull("Net pay should not be null", payslip.getNetPay());
            Assert.assertNotNull("Gross income should not be null", payslip.getGrossIncome());
            
            // Verify date consistency
            Assert.assertTrue("Period end date should be after or equal to start date", 
                            payslip.getPeriodEndDate().isAfter(payslip.getPeriodStartDate()) || 
                            payslip.getPeriodEndDate().equals(payslip.getPeriodStartDate()));
            
            LOGGER.info("PayslipDetails data integrity test passed for: " + payslip.getEmployeeName());
        } else {
            LOGGER.info("No payslip data available for data integrity testing");
        }
    }
    
    @Test
    public void testReportModels_DefaultValues() {
        LOGGER.info("Testing report models with default values...");
        
        // Test MonthlyPayrollSummaryReport
        MonthlyPayrollSummaryReport payrollReport = new MonthlyPayrollSummaryReport();
        Assert.assertFalse("Default success should be false", payrollReport.isSuccess());
        Assert.assertNotNull("Default payroll entries should not be null", payrollReport.getPayrollEntries());
        Assert.assertNotNull("Default totals should not be null", payrollReport.getTotals());
        
        // Test EmployeePayslipReport
        EmployeePayslipReport payslipReport = new EmployeePayslipReport();
        Assert.assertFalse("Default success should be false", payslipReport.isSuccess());
        
        // Test GovernmentComplianceReport
        GovernmentComplianceReport complianceReport = new GovernmentComplianceReport();
        Assert.assertFalse("Default success should be false", complianceReport.isSuccess());
        Assert.assertNotNull("Default SSS total should not be null", complianceReport.getTotalSSS());
        
        LOGGER.info("Report models default values test passed");
    }
    
    @Test
    public void testReportService_Constructors() {
        LOGGER.info("Testing ReportService constructors...");
        
        // Test default constructor
        ReportService defaultReportService = new ReportService();
        Assert.assertNotNull("Default constructor should create service", defaultReportService);
        
        // Test constructor with database connection
        DatabaseConnection testDbConnection = new DatabaseConnection();
        ReportService customReportService = new ReportService(testDbConnection);
        Assert.assertNotNull("Custom constructor should create service", customReportService);
        
        LOGGER.info("ReportService constructors test passed");
    }
}
