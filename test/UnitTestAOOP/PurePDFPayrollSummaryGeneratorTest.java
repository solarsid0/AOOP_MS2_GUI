package UnitTestAOOP;

import Services.PurePDFPayrollSummaryGenerator;
import Services.ReportService.MonthlyPayrollSummaryReport;
import Services.ReportService.PayrollSummaryEntry;
import Services.ReportService.PayrollTotals;
import org.junit.*;
import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class PurePDFPayrollSummaryGeneratorTest {
    private static final Logger logger = Logger.getLogger(PurePDFPayrollSummaryGeneratorTest.class.getName());
    
    private static Connection connection;
    private MonthlyPayrollSummaryReport testData;
    private static Path tempDir;
    
    @BeforeClass
    public static void setUpClass() throws Exception {
        // Create temporary directory for test output
        tempDir = Paths.get(System.getProperty("java.io.tmpdir"), "pdf-test-" + System.currentTimeMillis());
        tempDir.toFile().mkdirs();
        logger.info("Using temporary directory: " + tempDir.toString());
        
        // Initialize database connection
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/payrollsystem_db",
                "root",
                "Mmdc_2025*"
            );
            connection.setAutoCommit(false); // We'll rollback any changes after tests
            logger.info("Database connection established successfully");
        } catch (Exception e) {
            logger.severe("Failed to establish database connection: " + e.getMessage());
            throw new RuntimeException("Database connection failed", e);
        }
    }
    
    @AfterClass
    public static void tearDownClass() throws Exception {
        // Clean up database connection
        if (connection != null) {
            try {
                connection.rollback();
                connection.close();
                logger.info("Database connection closed successfully");
            } catch (SQLException e) {
                logger.warning("Error closing database connection: " + e.getMessage());
            }
        }
        
        // Clean up temporary directory
        if (tempDir != null) {
            File[] files = tempDir.toFile().listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
            tempDir.toFile().delete();
            logger.info("Deleted temporary directory: " + tempDir.toString());
        }
    }
    
    @Before
    public void setUp() throws SQLException {
        // Initialize test data from actual database using correct table names
        testData = new MonthlyPayrollSummaryReport();
        List<PayrollSummaryEntry> entries = new ArrayList<>();
        
        // Use the monthly_payroll_summary_report view directly since it matches our needs
        String query = "SELECT * FROM monthly_payroll_summary_report WHERE `Employee ID` != 'TOTAL' LIMIT 5";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                PayrollSummaryEntry entry = new PayrollSummaryEntry();
                entry.setEmployeeId(Integer.parseInt(rs.getString("Employee ID")));
                entry.setEmployeeName(rs.getString("Employee Name"));
                entry.setPosition(rs.getString("Position"));
                entry.setDepartment(rs.getString("Department"));
                entry.setBaseSalary(rs.getBigDecimal("Base Salary"));
                entry.setLeaves(rs.getBigDecimal("Leaves"));
                entry.setOvertime(rs.getBigDecimal("Overtime"));
                entry.setGrossIncome(rs.getBigDecimal("GROSS INCOME"));
                entry.setTotalBenefits(rs.getBigDecimal("TOTAL BENEFITS"));
                entry.setSssContribution(rs.getBigDecimal("Social Security Contribution"));
                entry.setPhilhealthContribution(rs.getBigDecimal("Philhealth Contribution"));
                entry.setPagibigContribution(rs.getBigDecimal("Pag-Ibig Contribution"));
                entry.setWithholdingTax(rs.getBigDecimal("Withholding Tax"));
                entry.setNetPay(rs.getBigDecimal("NET PAY"));
                
                entries.add(entry);
            }
        }
        
        // Calculate totals from the same view
        PayrollTotals totals = new PayrollTotals();
        String totalQuery = "SELECT SUM(`Base Salary`) AS total_base_salary, " +
                "SUM(`Leaves`) AS total_leaves, " +
                "SUM(`Overtime`) AS total_overtime, " +
                "SUM(`GROSS INCOME`) AS total_gross_income, " +
                "SUM(`TOTAL BENEFITS`) AS total_benefits, " +
                "SUM(`NET PAY`) AS total_net_pay " +
                "FROM monthly_payroll_summary_report " +
                "WHERE `Employee ID` != 'TOTAL'";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(totalQuery)) {
            
            if (rs.next()) {
                totals.setTotalBaseSalary(rs.getBigDecimal("total_base_salary") != null ? rs.getBigDecimal("total_base_salary") : BigDecimal.ZERO);
                totals.setTotalLeaves(rs.getBigDecimal("total_leaves") != null ? rs.getBigDecimal("total_leaves") : BigDecimal.ZERO);
                totals.setTotalOvertime(rs.getBigDecimal("total_overtime") != null ? rs.getBigDecimal("total_overtime") : BigDecimal.ZERO);
                totals.setTotalGrossIncome(rs.getBigDecimal("total_gross_income") != null ? rs.getBigDecimal("total_gross_income") : BigDecimal.ZERO);
                totals.setTotalBenefits(rs.getBigDecimal("total_benefits") != null ? rs.getBigDecimal("total_benefits") : BigDecimal.ZERO);
                totals.setTotalNetPay(rs.getBigDecimal("total_net_pay") != null ? rs.getBigDecimal("total_net_pay") : BigDecimal.ZERO);
            }
        }
        
        testData.setPayrollEntries(entries);
        testData.setTotals(totals);
        
        logger.info("Test data initialized with " + entries.size() + " payroll records");
    }
    
    @After
    public void tearDown() throws SQLException {
        // Rollback any changes made during tests
        if (connection != null) {
            connection.rollback();
            logger.info("Test database changes rolled back");
        }
    }
    
    @Test
    public void testGeneratePayrollSummaryPDF_WithValidData() {
        String outputPath = tempDir.resolve("valid_payroll_summary.pdf").toString();
        
        boolean result = PurePDFPayrollSummaryGenerator.generatePayrollSummaryPDF(
            testData, 
            "June 2024", 
            "All Departments", 
            "Unit Test", 
            outputPath
        );
        
        Assert.assertTrue("PDF generation should succeed with valid data", result);
        
        File outputFile = new File(outputPath);
        Assert.assertTrue("Output PDF file should exist", outputFile.exists());
        Assert.assertTrue("Output PDF file should not be empty", outputFile.length() > 0);
    }
    
    @Test
    public void testGeneratePayrollSummaryPDF_WithNullData() {
        String outputPath = tempDir.resolve("null_data_payroll_summary.pdf").toString();
        
        boolean result = PurePDFPayrollSummaryGenerator.generatePayrollSummaryPDF(
            null, 
            "June 2024", 
            "All Departments", 
            "Unit Test", 
            outputPath
        );
        
        Assert.assertFalse("PDF generation should fail with null data", result);
    }
    
    @Test
    public void testGeneratePayrollSummaryPDF_WithEmptyData() {
        String outputPath = tempDir.resolve("empty_data_payroll_summary.pdf").toString();
        
        MonthlyPayrollSummaryReport emptyData = new MonthlyPayrollSummaryReport();
        emptyData.setPayrollEntries(new ArrayList<>());
        emptyData.setTotals(new PayrollTotals());
        
        boolean result = PurePDFPayrollSummaryGenerator.generatePayrollSummaryPDF(
            emptyData, 
            "June 2024", 
            "All Departments", 
            "Unit Test", 
            outputPath
        );
        
        Assert.assertTrue("PDF generation should succeed with empty data", result);
        Assert.assertTrue("PDF file should be created even with empty data", 
            new File(outputPath).exists());
    }
    
    @Test
    public void testGenerateUniqueFilePath_NewFile() {
        String basePath = tempDir.resolve("new_file.pdf").toString();
        String result = PurePDFPayrollSummaryGenerator.generateUniqueFilePath(basePath);
        Assert.assertEquals("Should return original path for non-existing file", 
            basePath, result);
    }
    
    @Test
    public void testGenerateUniqueFilePath_ExistingFile() throws Exception {
        String basePath = tempDir.resolve("existing_file.pdf").toString();
        
        // Create the file first
        File file = new File(basePath);
        file.createNewFile();
        
        String result = PurePDFPayrollSummaryGenerator.generateUniqueFilePath(basePath);
        
        Assert.assertNotEquals("Should return different path for existing file", 
            basePath, result);
        Assert.assertTrue("Should contain version number", 
            result.contains("(1)"));
    }
    
    @Test
    public void testGenerateUniqueFilePath_NullInput() {
        String result = PurePDFPayrollSummaryGenerator.generateUniqueFilePath(null);
        Assert.assertNull("Should return null for null input", result);
    }
    
    @Test
    public void testGeneratePayrollSummaryPDF_WithSpecialCharacters() throws SQLException {
        // Create test data with special characters using corrected table names
        MonthlyPayrollSummaryReport specialData = new MonthlyPayrollSummaryReport();
        List<PayrollSummaryEntry> entries = new ArrayList<>();
        
        // Query an employee with special characters in their name (if exists)
        String query = "SELECT e.employeeId, CONCAT(e.firstName, ' ', e.lastName) AS employee_name, " +
                     "p.position, p.department, e.basicSalary " +
                     "FROM employee e " +
                     "JOIN position p ON e.positionId = p.positionId " +
                     "WHERE (e.firstName LIKE '%ñ%' OR e.firstName LIKE '%Ñ%' OR " +
                     "e.lastName LIKE '%ñ%' OR e.lastName LIKE '%Ñ%') " +
                     "LIMIT 1";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            if (rs.next()) {
                PayrollSummaryEntry entry = new PayrollSummaryEntry();
                entry.setEmployeeId(rs.getInt("employeeId"));
                entry.setEmployeeName(rs.getString("employee_name"));
                entry.setPosition(rs.getString("position") + " & Developer");
                entry.setDepartment(rs.getString("department") + " & IT");
                entry.setBaseSalary(rs.getBigDecimal("basicSalary"));
                entry.setLeaves(BigDecimal.ZERO);
                entry.setOvertime(BigDecimal.ZERO);
                entry.setGrossIncome(rs.getBigDecimal("basicSalary"));
                entry.setTotalBenefits(BigDecimal.ZERO);
                entry.setSssContribution(BigDecimal.ZERO);
                entry.setPhilhealthContribution(BigDecimal.ZERO);
                entry.setPagibigContribution(BigDecimal.ZERO);
                entry.setWithholdingTax(BigDecimal.ZERO);
                entry.setNetPay(rs.getBigDecimal("basicSalary"));
                
                entries.add(entry);
            } else {
                // Create a dummy entry with special characters if no real data exists
                PayrollSummaryEntry entry = new PayrollSummaryEntry();
                entry.setEmployeeId(9999);
                entry.setEmployeeName("José Rañola");
                entry.setPosition("Developer & Analyst");
                entry.setDepartment("IT & Support");
                entry.setBaseSalary(new BigDecimal("50000.00"));
                entry.setLeaves(BigDecimal.ZERO);
                entry.setOvertime(BigDecimal.ZERO);
                entry.setGrossIncome(new BigDecimal("50000.00"));
                entry.setTotalBenefits(BigDecimal.ZERO);
                entry.setSssContribution(BigDecimal.ZERO);
                entry.setPhilhealthContribution(BigDecimal.ZERO);
                entry.setPagibigContribution(BigDecimal.ZERO);
                entry.setWithholdingTax(BigDecimal.ZERO);
                entry.setNetPay(new BigDecimal("50000.00"));
                
                entries.add(entry);
            }
        }
        
        specialData.setPayrollEntries(entries);
        specialData.setTotals(new PayrollTotals());
        
        String outputPath = tempDir.resolve("special_chars_payroll_summary.pdf").toString();
        
        boolean result = PurePDFPayrollSummaryGenerator.generatePayrollSummaryPDF(
            specialData, 
            "June 2024", 
            "All Departments", 
            "Unit Test", 
            outputPath
        );
        
        Assert.assertTrue("PDF generation should succeed with special characters", result);
        Assert.assertTrue("PDF file should be created", 
            new File(outputPath).exists());
    }
    
    @Test
    public void testGeneratePayrollSummaryPDF_WithSpecificDepartment() throws SQLException {
        // Get a specific department from the database using correct table name
        String departmentQuery = "SELECT department FROM position WHERE department IS NOT NULL LIMIT 1";
        String department = "All Departments"; // Default if no department found
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(departmentQuery)) {
            if (rs.next()) {
                department = rs.getString("department");
            }
        }
        
        // Create test data filtered by department using the view
        MonthlyPayrollSummaryReport departmentData = new MonthlyPayrollSummaryReport();
        List<PayrollSummaryEntry> entries = new ArrayList<>();
        
        String query = "SELECT * FROM monthly_payroll_summary_report " +
                     "WHERE Department = '" + department + "' AND `Employee ID` != 'TOTAL' " +
                     "LIMIT 3";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                PayrollSummaryEntry entry = new PayrollSummaryEntry();
                entry.setEmployeeId(Integer.parseInt(rs.getString("Employee ID")));
                entry.setEmployeeName(rs.getString("Employee Name"));
                entry.setPosition(rs.getString("Position"));
                entry.setDepartment(rs.getString("Department"));
                entry.setBaseSalary(rs.getBigDecimal("Base Salary"));
                entry.setLeaves(rs.getBigDecimal("Leaves"));
                entry.setOvertime(rs.getBigDecimal("Overtime"));
                entry.setGrossIncome(rs.getBigDecimal("GROSS INCOME"));
                entry.setTotalBenefits(rs.getBigDecimal("TOTAL BENEFITS"));
                entry.setSssContribution(rs.getBigDecimal("Social Security Contribution"));
                entry.setPhilhealthContribution(rs.getBigDecimal("Philhealth Contribution"));
                entry.setPagibigContribution(rs.getBigDecimal("Pag-Ibig Contribution"));
                entry.setWithholdingTax(rs.getBigDecimal("Withholding Tax"));
                entry.setNetPay(rs.getBigDecimal("NET PAY"));
                
                entries.add(entry);
            }
        }
        
        // Calculate totals for the department
        PayrollTotals totals = new PayrollTotals();
        String totalQuery = "SELECT SUM(`Base Salary`) AS total_base_salary, " +
                "SUM(`Leaves`) AS total_leaves, " +
                "SUM(`Overtime`) AS total_overtime, " +
                "SUM(`GROSS INCOME`) AS total_gross_income, " +
                "SUM(`TOTAL BENEFITS`) AS total_benefits, " +
                "SUM(`NET PAY`) AS total_net_pay " +
                "FROM monthly_payroll_summary_report " +
                "WHERE Department = '" + department + "' AND `Employee ID` != 'TOTAL'";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(totalQuery)) {
            
            if (rs.next()) {
                totals.setTotalBaseSalary(rs.getBigDecimal("total_base_salary") != null ? rs.getBigDecimal("total_base_salary") : BigDecimal.ZERO);
                totals.setTotalLeaves(rs.getBigDecimal("total_leaves") != null ? rs.getBigDecimal("total_leaves") : BigDecimal.ZERO);
                totals.setTotalOvertime(rs.getBigDecimal("total_overtime") != null ? rs.getBigDecimal("total_overtime") : BigDecimal.ZERO);
                totals.setTotalGrossIncome(rs.getBigDecimal("total_gross_income") != null ? rs.getBigDecimal("total_gross_income") : BigDecimal.ZERO);
                totals.setTotalBenefits(rs.getBigDecimal("total_benefits") != null ? rs.getBigDecimal("total_benefits") : BigDecimal.ZERO);
                totals.setTotalNetPay(rs.getBigDecimal("total_net_pay") != null ? rs.getBigDecimal("total_net_pay") : BigDecimal.ZERO);
            }
        }
        
        departmentData.setPayrollEntries(entries);
        departmentData.setTotals(totals);
        
        String outputPath = tempDir.resolve("department_payroll_summary.pdf").toString();
        
        boolean result = PurePDFPayrollSummaryGenerator.generatePayrollSummaryPDF(
            departmentData, 
            "June 2024", 
            department, 
            "Unit Test", 
            outputPath
        );
        
        Assert.assertTrue("PDF generation should succeed with department data", result);
        Assert.assertTrue("PDF file should be created for department", 
            new File(outputPath).exists());
    }
}
