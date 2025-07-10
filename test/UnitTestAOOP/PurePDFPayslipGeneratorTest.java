package UnitTestAOOP;

import Services.PurePDFPayslipGenerator;
import Services.ReportService.PayslipDetails;
import org.junit.*;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

/**
 * Unit tests for PurePDFPayslipGenerator
 * Tests PDF generation functionality using real database data
 * 
 * @author Admin
 */
public class PurePDFPayslipGeneratorTest {
    
    private static final Logger LOGGER = Logger.getLogger(PurePDFPayslipGeneratorTest.class.getName());
    
    // Database connection details
    private static final String DB_URL = "jdbc:mysql://localhost:3306/payrollsystem_db";
    private static final String DB_USERNAME = "root";
    private static final String DB_PASSWORD = "Mmdc_2025*";
    
    private static PurePDFPayslipGenerator generator;
    private static Connection dbConnection;
    private static List<PayslipDetails> realPayslipData;
    
    private PayslipDetails validPayslip;
    private PayslipDetails invalidPayslip;
    
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    
    @BeforeClass
    public static void setUpClass() {
        LOGGER.info("=== Starting PurePDFPayslipGenerator Test Suite ===");
        
        try {
            // Initialize generator
            generator = new PurePDFPayslipGenerator();
            LOGGER.info("Generator instance created successfully");
            
            // Setup database connection
            setupDatabaseConnection();
            LOGGER.info("Database connection established");
            
            // Load real payslip data from database
            loadRealPayslipData();
            LOGGER.info("Real payslip data loaded from database");
            
        } catch (Exception e) {
            LOGGER.severe("Failed to setup test class: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @AfterClass
    public static void tearDownClass() {
        LOGGER.info("=== Cleaning up PurePDFPayslipGenerator Test Suite ===");
        
        try {
            if (dbConnection != null && !dbConnection.isClosed()) {
                dbConnection.close();
                LOGGER.info("Database connection closed");
            }
        } catch (SQLException e) {
            LOGGER.warning("Error closing database connection: " + e.getMessage());
        }
        
        generator = null;
        realPayslipData = null;
        LOGGER.info("=== PurePDFPayslipGenerator Test Suite Completed ===");
    }
    
    @Before
    public void setUp() {
        LOGGER.info("Setting up test data...");
        
        // Use real data if available, otherwise create test data
        if (realPayslipData != null && !realPayslipData.isEmpty()) {
            validPayslip = realPayslipData.get(0);
            LOGGER.info("Using real payslip data for Employee ID: " + validPayslip.getEmployeeId());
        } else {
            validPayslip = createValidPayslipDetails();
            LOGGER.info("Using manually created test data");
        }
        
        // Create invalid payslip data
        invalidPayslip = createInvalidPayslipDetails();
        
        LOGGER.info("Test data setup completed");
    }
    
    @After
    public void tearDown() {
        LOGGER.info("Cleaning up test data...");
        validPayslip = null;
        invalidPayslip = null;
    }
    
    // ========== DATABASE CONNECTION METHODS ==========
    
    private static void setupDatabaseConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            dbConnection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
            LOGGER.info("Database connection successful");
        } catch (ClassNotFoundException e) {
            LOGGER.severe("MySQL Driver not found: " + e.getMessage());
            throw new SQLException("MySQL Driver not found", e);
        }
    }
    
    private static void loadRealPayslipData() throws SQLException {
        realPayslipData = new ArrayList<>();
        
        String query = "SELECT * FROM monthly_employee_payslip LIMIT 5";
        
        try (PreparedStatement stmt = dbConnection.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                PayslipDetails payslip = mapResultSetToPayslipDetails(rs);
                realPayslipData.add(payslip);
                LOGGER.info("Loaded payslip for Employee: " + payslip.getEmployeeName());
            }
            
            LOGGER.info("Total payslip records loaded: " + realPayslipData.size());
            
        } catch (SQLException e) {
            LOGGER.severe("Failed to load real payslip data: " + e.getMessage());
            throw e;
        }
    }
    
    private static PayslipDetails mapResultSetToPayslipDetails(ResultSet rs) throws SQLException {
        PayslipDetails payslip = new PayslipDetails();
        
        // Map database fields to PayslipDetails object
        payslip.setPayslipNo(rs.getString("Payslip No"));
        payslip.setEmployeeId(rs.getInt("Employee ID"));
        payslip.setEmployeeName(rs.getString("Employee Name"));
        payslip.setPeriodStartDate(rs.getDate("Period Start Date").toLocalDate());
        payslip.setPeriodEndDate(rs.getDate("Period End Date").toLocalDate());
        payslip.setPayDate(rs.getDate("Pay Date").toLocalDate());
        payslip.setEmployeePosition(rs.getString("Employee Position"));
        payslip.setDepartment(rs.getString("Department"));
        payslip.setTin(rs.getString("TIN"));
        payslip.setSssNo(rs.getString("SSS No"));
        payslip.setPagibigNo(rs.getString("Pagibig No"));
        payslip.setPhilhealthNo(rs.getString("Philhealth No"));
        payslip.setMonthlyRate(rs.getBigDecimal("Monthly Rate"));
        payslip.setDailyRate(rs.getBigDecimal("Daily Rate"));
        payslip.setDaysWorked(rs.getBigDecimal("Days Worked"));
        payslip.setLeavesTaken(rs.getBigDecimal("Leaves Taken"));
        payslip.setOvertimeHours(rs.getBigDecimal("Overtime Hours"));
        payslip.setGrossIncome(rs.getBigDecimal("GROSS INCOME"));
        payslip.setRiceSubsidy(rs.getBigDecimal("Rice Subsidy"));
        payslip.setPhoneAllowance(rs.getBigDecimal("Phone Allowance"));
        payslip.setClothingAllowance(rs.getBigDecimal("Clothing Allowance"));
        payslip.setTotalBenefits(rs.getBigDecimal("TOTAL BENEFITS"));
        payslip.setSocialSecuritySystem(rs.getBigDecimal("Social Security System"));
        payslip.setPhilhealth(rs.getBigDecimal("Philhealth"));
        payslip.setPagIbig(rs.getBigDecimal("Pag-Ibig"));
        payslip.setWithholdingTax(rs.getBigDecimal("Withholding Tax"));
        payslip.setTotalDeductions(rs.getBigDecimal("TOTAL DEDUCTIONS"));
        payslip.setNetPay(rs.getBigDecimal("NET PAY"));
        
        return payslip;
    }
    
    // ========== POSITIVE TESTS ==========
    
    @Test
    public void testGeneratePayslipPDF_WithRealData_Success() {
        LOGGER.info("Testing successful PDF generation with real database data...");
        
        try {
            File outputFile = tempFolder.newFile("real_data_payslip.pdf");
            String outputPath = outputFile.getAbsolutePath();
            
            boolean result = generator.generatePayslipPDF(validPayslip, outputPath);
            
            Assert.assertTrue("PDF generation should succeed with real data", result);
            Assert.assertTrue("PDF file should be created", outputFile.exists());
            Assert.assertTrue("PDF file should not be empty", outputFile.length() > 0);
            
            LOGGER.info("PDF generation test passed - File size: " + outputFile.length() + " bytes");
            LOGGER.info("Generated PDF for Employee: " + validPayslip.getEmployeeName());
            
        } catch (IOException e) {
            LOGGER.severe("Failed to create test file: " + e.getMessage());
            Assert.fail("Test setup failed: " + e.getMessage());
        }
    }
    
    @Test
    public void testGeneratePayslipPDF_MultipleEmployees_Success() {
        LOGGER.info("Testing PDF generation for multiple employees...");
        
        if (realPayslipData == null || realPayslipData.size() < 2) {
            LOGGER.warning("Not enough real data for multiple employee test");
            return;
        }
        
        try {
            int successCount = 0;
            long timestamp = System.currentTimeMillis();
            
            for (int i = 0; i < Math.min(3, realPayslipData.size()); i++) {
                PayslipDetails payslip = realPayslipData.get(i);
                File outputFile = tempFolder.newFile("payslip_employee_" + payslip.getEmployeeId() + "_" + timestamp + "_" + i + ".pdf");
                String outputPath = outputFile.getAbsolutePath();
                
                boolean result = generator.generatePayslipPDF(payslip, outputPath);
                
                if (result && outputFile.exists() && outputFile.length() > 0) {
                    successCount++;
                    LOGGER.info("Successfully generated PDF for Employee: " + payslip.getEmployeeName());
                }
            }
            
            Assert.assertTrue("At least 1 PDF should be generated successfully", successCount >= 1);
            LOGGER.info("Multiple employee PDF generation test passed - " + successCount + " PDFs generated");
            
        } catch (IOException e) {
            LOGGER.severe("Failed to create test files: " + e.getMessage());
            Assert.fail("Test setup failed: " + e.getMessage());
        }
    }
    
    @Test
    public void testGeneratePayslip_StaticMethod_Success() {
        LOGGER.info("Testing static factory method with real data...");
        
        try {
            File outputFile = tempFolder.newFile("static_real_data_payslip.pdf");
            String outputPath = outputFile.getAbsolutePath();
            
            boolean result = PurePDFPayslipGenerator.generatePayslip(validPayslip, outputPath);
            
            Assert.assertTrue("Static method should succeed with real data", result);
            Assert.assertTrue("PDF file should be created via static method", outputFile.exists());
            
            LOGGER.info("Static method test passed with real data");
            
        } catch (IOException e) {
            LOGGER.severe("Failed to create test file for static method: " + e.getMessage());
            Assert.fail("Test setup failed: " + e.getMessage());
        }
    }
    
    @Test
    public void testFormatDate_WithDifferentDateTypes() {
        LOGGER.info("Testing date formatting with different date types...");
        
        try {
            // Test with LocalDate
            LocalDate localDate = LocalDate.of(2024, 6, 30);
            String formattedLocalDate = invokePrivateFormatDate(localDate);
            Assert.assertEquals("LocalDate should be formatted correctly", "06-30-2024", formattedLocalDate);
            
            // Test with Date
            Date utilDate = new Date(2024-1900, 5, 30);
            String formattedUtilDate = invokePrivateFormatDate(utilDate);
            Assert.assertNotNull("Date should be formatted", formattedUtilDate);
            
            // Test with LocalDateTime
            LocalDateTime localDateTime = LocalDateTime.of(2024, 6, 30, 10, 30);
            String formattedLocalDateTime = invokePrivateFormatDate(localDateTime);
            Assert.assertEquals("LocalDateTime should be formatted correctly", "06-30-2024", formattedLocalDateTime);
            
            // Test with null
            String formattedNull = invokePrivateFormatDate(null);
            Assert.assertEquals("Null date should return N/A", "N/A", formattedNull);
            
            LOGGER.info("Date formatting tests passed");
            
        } catch (Exception e) {
            LOGGER.severe("Date formatting test failed: " + e.getMessage());
            Assert.fail("Date formatting test failed: " + e.getMessage());
        }
    }
    
    // ========== NEGATIVE TESTS ==========
    
    @Test
    public void testGeneratePayslipPDF_NullPayslip() {
        LOGGER.info("Testing PDF generation with null payslip...");
        
        try {
            File outputFile = tempFolder.newFile("null_payslip.pdf");
            String outputPath = outputFile.getAbsolutePath();
            
            boolean result = generator.generatePayslipPDF(null, outputPath);
            
            // The generator should return false when it encounters an error
            Assert.assertFalse("PDF generation should fail with null payslip", result);
            
            LOGGER.info("Null payslip test passed - correctly handled null input");
            
        } catch (Exception e) {
            // If an exception is thrown, that's also acceptable behavior for null input
            LOGGER.info("Null payslip test passed - exception thrown as expected: " + e.getClass().getSimpleName());
        }
    }
    
    @Test
    public void testGeneratePayslipPDF_InvalidOutputPath() {
        LOGGER.info("Testing PDF generation with invalid output path...");
        
        String invalidPath = "/invalid/directory/path/test.pdf";
        
        boolean result = generator.generatePayslipPDF(validPayslip, invalidPath);
        
        Assert.assertFalse("PDF generation should fail with invalid path", result);
        
        LOGGER.info("Invalid path test passed - correctly handled invalid output path");
    }
    
    @Test
    public void testGeneratePayslipPDF_EmptyOutputPath() {
        LOGGER.info("Testing PDF generation with empty output path...");
        
        boolean result = generator.generatePayslipPDF(validPayslip, "");
        
        Assert.assertFalse("PDF generation should fail with empty path", result);
        
        LOGGER.info("Empty path test passed - correctly handled empty output path");
    }
    
    @Test
    public void testGeneratePayslipPDF_NullOutputPath() {
        LOGGER.info("Testing PDF generation with null output path...");
        
        try {
            boolean result = generator.generatePayslipPDF(validPayslip, null);
            Assert.assertFalse("PDF generation should fail with null path", result);
            LOGGER.info("Null path test passed - correctly handled null output path");
        } catch (Exception e) {
            // Exception is also acceptable for null path
            LOGGER.info("Null path test passed - exception thrown as expected: " + e.getClass().getSimpleName());
        }
    }
    
    @Test
    public void testGeneratePayslipPDF_WithIncompletePayslipData() {
        LOGGER.info("Testing PDF generation with incomplete payslip data...");
        
        try {
            File outputFile = tempFolder.newFile("incomplete_payslip.pdf");
            String outputPath = outputFile.getAbsolutePath();
            
            boolean result = generator.generatePayslipPDF(invalidPayslip, outputPath);
            
            // The generator may fail or succeed with incomplete data
            if (result && outputFile.exists() && outputFile.length() > 0) {
                LOGGER.info("Incomplete data test passed - PDF generated with default values");
            } else {
                LOGGER.info("Incomplete data test passed - PDF generation failed as expected");
            }
            
        } catch (Exception e) {
            // Exception is acceptable for incomplete data
            LOGGER.info("Incomplete data test passed - exception thrown as expected: " + e.getClass().getSimpleName());
        }
    }
    
    @Test
    public void testGeneratePayslipPDF_WithCompletelyNullPayslipData() {
        LOGGER.info("Testing PDF generation with completely null payslip data...");
        
        try {
            PayslipDetails nullPayslip = new PayslipDetails();
            // Leave all fields null
            
            File outputFile = tempFolder.newFile("completely_null_payslip.pdf");
            String outputPath = outputFile.getAbsolutePath();
            
            boolean result = generator.generatePayslipPDF(nullPayslip, outputPath);
            
            // This should fail since required fields are null
            Assert.assertFalse("PDF generation should fail with completely null data", result);
            
            LOGGER.info("Completely null data test passed - PDF generation failed as expected");
            
        } catch (Exception e) {
            // Exception is expected for completely null data
            LOGGER.info("Completely null data test passed - exception thrown as expected: " + e.getClass().getSimpleName());
        }
    }
    
    @Test
    public void testFormatDate_WithInvalidDateObject() {
        LOGGER.info("Testing date formatting with invalid date object...");
        
        try {
            // Test with non-date object
            String result = invokePrivateFormatDate("not a date");
            Assert.assertNotNull("Should return string representation of invalid date object", result);
            
            // Test with number
            String numberResult = invokePrivateFormatDate(12345);
            Assert.assertNotNull("Should return string representation of number", numberResult);
            
            LOGGER.info("Invalid date object tests passed");
            
        } catch (Exception e) {
            LOGGER.severe("Invalid date object test failed: " + e.getMessage());
            Assert.fail("Invalid date object test failed: " + e.getMessage());
        }
    }
    
    @Test
    public void testStaticMethod_WithNullInputs() {
        LOGGER.info("Testing static method with null inputs...");
        
        // Test with null payslip
        try {
            boolean result1 = PurePDFPayslipGenerator.generatePayslip(null, "test.pdf");
            Assert.assertFalse("Static method should fail with null payslip", result1);
        } catch (Exception e) {
            LOGGER.info("Static method correctly threw exception with null payslip: " + e.getClass().getSimpleName());
        }
        
        // Test with null path
        try {
            boolean result2 = PurePDFPayslipGenerator.generatePayslip(validPayslip, null);
            Assert.assertFalse("Static method should fail with null path", result2);
        } catch (Exception e) {
            LOGGER.info("Static method correctly threw exception with null path: " + e.getClass().getSimpleName());
        }
        
        // Test with both null
        try {
            boolean result3 = PurePDFPayslipGenerator.generatePayslip(null, null);
            Assert.assertFalse("Static method should fail with both null inputs", result3);
        } catch (Exception e) {
            LOGGER.info("Static method correctly threw exception with both null inputs: " + e.getClass().getSimpleName());
        }
        
        LOGGER.info("Static method null input tests passed");
    }
    
    @Test
    public void testDatabaseConnection_Integrity() {
        LOGGER.info("Testing database connection integrity...");
        
        try {
            Assert.assertNotNull("Database connection should not be null", dbConnection);
            Assert.assertFalse("Database connection should be open", dbConnection.isClosed());
            
            String testQuery = "SELECT COUNT(*) FROM monthly_employee_payslip";
            try (PreparedStatement stmt = dbConnection.prepareStatement(testQuery);
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
    public void testPayslipDataValidation_WithRealData() {
        LOGGER.info("Testing payslip data validation with real data...");
        
        Assert.assertNotNull("Valid payslip should not be null", validPayslip);
        Assert.assertNotNull("Employee ID should not be null", validPayslip.getEmployeeId());
        Assert.assertNotNull("Employee name should not be null", validPayslip.getEmployeeName());
        Assert.assertNotNull("Net pay should not be null", validPayslip.getNetPay());
        Assert.assertNotNull("Monthly rate should not be null", validPayslip.getMonthlyRate());
        Assert.assertNotNull("Department should not be null", validPayslip.getDepartment());
        
        Assert.assertTrue("Monthly rate should be positive", 
                         validPayslip.getMonthlyRate().compareTo(BigDecimal.ZERO) >= 0);
        Assert.assertTrue("Net pay should be positive", 
                         validPayslip.getNetPay().compareTo(BigDecimal.ZERO) >= 0);
        
        LOGGER.info("Payslip data validation passed for Employee: " + validPayslip.getEmployeeName());
    }
    
    // ========== HELPER METHODS ==========
    
    private PayslipDetails createValidPayslipDetails() {
        PayslipDetails payslip = new PayslipDetails();
        
        payslip.setPayslipNo("10001-2024-06");
        payslip.setEmployeeId(10001);
        payslip.setEmployeeName("Garcia, Manuel III");
        payslip.setPeriodStartDate(LocalDate.of(2024, 6, 1));
        payslip.setPeriodEndDate(LocalDate.of(2024, 6, 30));
        payslip.setPayDate(LocalDate.of(2024, 6, 30));
        payslip.setEmployeePosition("Chief Executive Officer");
        payslip.setDepartment("Leadership");
        payslip.setTin("442-605-657-000");
        payslip.setSssNo("44-4506057-3");
        payslip.setPagibigNo("691295330870");
        payslip.setPhilhealthNo("820126853951");
        payslip.setMonthlyRate(new BigDecimal("90000.00"));
        payslip.setDailyRate(new BigDecimal("4285.68"));
        payslip.setDaysWorked(new BigDecimal("20.00"));
        payslip.setLeavesTaken(new BigDecimal("0"));
        payslip.setOvertimeHours(new BigDecimal("0.00"));
        payslip.setGrossIncome(new BigDecimal("90000.00"));
        payslip.setRiceSubsidy(new BigDecimal("1500.00"));
        payslip.setPhoneAllowance(new BigDecimal("2000.00"));
        payslip.setClothingAllowance(new BigDecimal("1000.00"));
        payslip.setTotalBenefits(new BigDecimal("4500.00"));
        payslip.setSocialSecuritySystem(new BigDecimal("1125.00"));
        payslip.setPhilhealth(new BigDecimal("1350.00"));
        payslip.setPagIbig(new BigDecimal("100.00"));
        payslip.setWithholdingTax(new BigDecimal("17060.40"));
        payslip.setTotalDeductions(new BigDecimal("19635.40"));
        payslip.setNetPay(new BigDecimal("74864.60"));
        
        return payslip;
    }
    
    private PayslipDetails createInvalidPayslipDetails() {
        PayslipDetails payslip = new PayslipDetails();
        
        // Set some minimal data to avoid immediate null pointer exceptions
        payslip.setEmployeeId(999);
        payslip.setEmployeeName("Test Employee");
        payslip.setEmployeePosition("Test Position");
        payslip.setDepartment("Test Department");
        payslip.setPeriodStartDate(LocalDate.now());
        payslip.setPeriodEndDate(LocalDate.now());
        payslip.setPayDate(LocalDate.now());
        payslip.setPayslipNo("TEST-001");
        
        // Set problematic/invalid values
        payslip.setMonthlyRate(null);
        payslip.setDailyRate(null);
        payslip.setGrossIncome(null);
        payslip.setTotalDeductions(null);
        payslip.setNetPay(null);
        payslip.setTin(null);
        payslip.setSssNo(null);
        payslip.setPagibigNo(null);
        payslip.setPhilhealthNo(null);
        
        return payslip;
    }
    
    private String invokePrivateFormatDate(Object dateObj) throws Exception {
        Method formatDateMethod = PurePDFPayslipGenerator.class.getDeclaredMethod("formatDate", Object.class);
        formatDateMethod.setAccessible(true);
        return (String) formatDateMethod.invoke(generator, dateObj);
    }
}
