package Services;

import DAOs.DatabaseConnection;
import DAOs.ReferenceDataDAO;
import DAOs.EmployeeDAO;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

import java.io.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Enhanced JasperReports generator for MotorPH Payroll System
 * Leverages database views for complex payroll calculations
 * Uses monthly_employee_payslip and monthly_payroll_summary_report views
 * @author User
 */
public class JasperReportGenerator {
    
    // Manila timezone constant
    public static final ZoneId MANILA_TIMEZONE = ZoneId.of("Asia/Manila");
    
    private final DatabaseConnection databaseConnection;
    private final ReferenceDataDAO referenceDataDAO;
    private final EmployeeDAO employeeDAO;
    
    // Report template paths - Modified for better path resolution
    private static final String REPORTS_PATH = "Reports/";  // Simplified path
    private static final String OUTPUT_PATH = "reports/output/";
    
    /**
     * Constructor
     * @param databaseConnection Database connection instance
     */
    public JasperReportGenerator(DatabaseConnection databaseConnection) {
        
        // DEBUG: Check if commons-logging is available
        try {
            Class.forName("org.apache.commons.logging.LogFactory");
            System.out.println("✅ Commons-logging found in classpath");
        } catch (ClassNotFoundException e) {
            System.err.println("❌ Commons-logging NOT found in classpath");
        }
        
        // Create output directory if it doesn't exist
        createOutputDirectory();
            
        this.databaseConnection = databaseConnection;
        this.referenceDataDAO = new ReferenceDataDAO(databaseConnection);
        this.employeeDAO = new EmployeeDAO(databaseConnection);
        
        // Test template path resolution on startup
        testTemplatePaths();
    }
    
    /**
     * Test and find the correct template path
     */
    private void testTemplatePaths() {
        System.out.println("=== TEMPLATE PATH RESOLUTION ===");
        String templateName = "MotorPH Employee Payslip.jrxml";
        
        String[] possiblePaths = {
            "Reports/" + templateName,
            "src/Reports/" + templateName,
            templateName,
            "src/" + templateName,
            "./Reports/" + templateName,
            "./src/Reports/" + templateName
        };
        
        for (String path : possiblePaths) {
            File testFile = new File(path);
            System.out.println("Testing: " + path);
            System.out.println("  Absolute: " + testFile.getAbsolutePath());
            System.out.println("  Exists: " + testFile.exists());
            if (testFile.exists()) {
                System.out.println("  ✅ FOUND TEMPLATE AT: " + path);
            }
            System.out.println();
        }
    }
    
    /**
     * Find the correct template path dynamically
     */
    private String findTemplatePath(String templateName) {
        String[] possiblePaths = {
            "Reports/" + templateName,
            "src/Reports/" + templateName,
            templateName,
            "./Reports/" + templateName,
            "./src/Reports/" + templateName,
            "src/" + templateName
        };
        
        for (String path : possiblePaths) {
            File testFile = new File(path);
            if (testFile.exists()) {
                System.out.println("Found template at: " + testFile.getAbsolutePath());
                return path;
            }
        }
        
        System.err.println("Template not found: " + templateName);
        return null;
    }
    
    // ================================
    // DATABASE VIEW-BASED REPORT METHODS
    // ================================
    
    /**
     * Generates payslip using monthly_employee_payslip view
     * This leverages all the complex business logic in the database view
     * @param employeeId Employee ID
     * @param payMonth Pay month in YYYY-MM format
     * @param format Output format ("PDF", "EXCEL", "XLSX")
     * @return Generated file path
     */
    public String generatePayslipFromView(Integer employeeId, String payMonth, String format) {
        try {
            System.out.println("=== GENERATING PAYSLIP FROM VIEW ===");
            System.out.println("Employee ID: " + employeeId);
            System.out.println("Pay Month: " + payMonth);
            System.out.println("Format: " + format);
            
            // Find the correct template path
            String templateName = "MotorPH Employee Payslip.jrxml";
            String templatePath = findTemplatePath(templateName);
            
            if (templatePath == null) {
                System.err.println("ERROR: Template file not found: " + templateName);
                return null;
            }
            
            // Prepare parameters - Enhanced for JRXML compatibility
            Map<String, Object> parameters = new HashMap<>();
            
            // Core parameters that your JRXML expects
            parameters.put("EMPLOYEE_ID", employeeId);
            parameters.put("YEAR_MONTH", payMonth);
            parameters.put("PAYSLIP_NO_PARAM", employeeId + "-" + payMonth);
            
            // Logo path - try multiple possible locations
            String logoPath = findLogoPath();
            parameters.put("LOGO_PATH", logoPath);
            
            // Additional parameters
            parameters.put("PAY_MONTH", payMonth);
            parameters.put("COMPANY_NAME", "MotorPH");
            parameters.put("REPORT_DATE", new Date());
            parameters.put("GENERATED_DATE", LocalDate.now(MANILA_TIMEZONE).format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")));

            System.out.println("Parameters set:");
            parameters.forEach((key, value) -> System.out.println("  " + key + ": " + value));

            // Get employee details for filename
            var employee = employeeDAO.findById(employeeId);
            String employeeName = employee != null ? employee.getLastName() : "Employee";

            // Method 1: Try using database connection (let JRXML handle the query)
            String outputPath = generateUsingDatabaseConnection(templatePath, parameters, employeeName, employeeId, payMonth, format);
            
            if (outputPath != null) {
                return outputPath;
            }
            
            // Method 2: Fallback to using data source
            System.out.println("Trying fallback method with data source...");
            List<Map<String, Object>> payslipData = getPayslipDataFromView(employeeId, payMonth);

            if (payslipData.isEmpty()) {
                System.err.println("No payslip data found for employee " + employeeId + " in " + payMonth);
                return null;
            }

            // Add rank-and-file information
            boolean isRankAndFile = employeeDAO.isEmployeeRankAndFile(employeeId);
            parameters.put("IS_RANK_AND_FILE", isRankAndFile);
            parameters.put("OVERTIME_ELIGIBLE", isRankAndFile ? "Yes (1.25x rate)" : "No");

            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(payslipData);

            String outputFileName = String.format("Payslip_%s_%s_%s.%s", 
                employeeName, employeeId, payMonth, format.toLowerCase());

            System.out.println("Generating payslip from database view for employee: " + employeeId);
            return generateReport(templatePath, parameters, dataSource, outputFileName, format);

        } catch (Exception e) {
            System.err.println("Error generating payslip from view: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Try generating using database connection (for JRXML with embedded SQL)
     */
    private String generateUsingDatabaseConnection(String templatePath, Map<String, Object> parameters, 
                                                  String employeeName, Integer employeeId, String payMonth, String format) {
        try {
            System.out.println("Attempting generation using database connection...");
            
            // Compile template
            JasperReport jasperReport = JasperCompileManager.compileReport(templatePath);
            System.out.println("Template compiled successfully");
            
            // Fill report using database connection
            JasperPrint jasperPrint;
            try (Connection connection = databaseConnection.getConnection()) {
                if (connection == null) {
                    System.err.println("Database connection is null");
                    return null;
                }
                
                System.out.println("Filling report with database connection...");
                jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, connection);
            }
            
            // Check if data was found
            if (jasperPrint == null || jasperPrint.getPages().isEmpty()) {
                System.err.println("No data found using database connection method");
                return null;
            }
            
            // Generate output file
            String timestamp = String.valueOf(System.currentTimeMillis());
            String outputFileName = String.format("Payslip_%s_%s_%s_%s.%s", 
                employeeName, employeeId, payMonth, timestamp, format.toLowerCase());
            String outputPath = OUTPUT_PATH + outputFileName;
            
            // Export to PDF
            JasperExportManager.exportReportToPdfFile(jasperPrint, outputPath);
            
            // Verify file was created
            File outputFile = new File(outputPath);
            if (outputFile.exists() && outputFile.length() > 0) {
                System.out.println("SUCCESS: PDF generated using database connection method");
                System.out.println("File: " + outputFile.getAbsolutePath());
                return outputPath;
            } else {
                System.err.println("PDF file was not created or is empty");
                return null;
            }
            
        } catch (Exception e) {
            System.err.println("Database connection method failed: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Find logo file path
     */
    private String findLogoPath() {
        String logoName = "OG Logo _ 100X124.png";
        String[] possiblePaths = {
            "src/media/" + logoName,
            "media/" + logoName,
            logoName,
            "./src/media/" + logoName,
            "./media/" + logoName
        };
        
        for (String path : possiblePaths) {
            File logoFile = new File(path);
            if (logoFile.exists()) {
                System.out.println("Found logo at: " + logoFile.getAbsolutePath());
                return path;
            }
        }
        
        System.err.println("Logo file not found: " + logoName);
        return "src/media/" + logoName; // Return default path
    }

    /**
     * Generates monthly payroll summary using monthly_payroll_summary_report view
     * @param payMonth Pay month in YYYY-MM format
     * @param format Output format
     * @return Generated file path
     */
    public String generateMonthlyPayrollSummaryFromView(String payMonth, String format) {
        try {
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("PAY_MONTH", payMonth);
            parameters.put("COMPANY_NAME", "MotorPH");
            parameters.put("REPORT_DATE", new Date());
            parameters.put("GENERATED_DATE", LocalDate.now(MANILA_TIMEZONE).format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")));
            
            // Use the database view for complete summary
            List<Map<String, Object>> summaryData = getPayrollSummaryFromView(payMonth);
            
            if (summaryData.isEmpty()) {
                System.err.println("No payroll summary data found for " + payMonth);
                return null;
            }
            
            // Calculate totals from the view data
            PayrollTotals totals = calculateTotalsFromSummary(summaryData);
            parameters.put("TOTAL_EMPLOYEES", totals.getEmployeeCount());
            parameters.put("TOTAL_GROSS_INCOME", totals.getTotalGrossIncome());
            parameters.put("TOTAL_BENEFITS", totals.getTotalBenefits());
            parameters.put("TOTAL_DEDUCTIONS", totals.getTotalDeductions());
            parameters.put("TOTAL_NET_PAY", totals.getTotalNetPay());
            
            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(summaryData);
            
            String templatePath = findTemplatePath("payroll_summary_view_template.jrxml");
            if (templatePath == null) {
                System.err.println("Payroll summary template not found");
                return null;
            }
            
            String outputFileName = String.format("Payroll_Summary_%s.%s", payMonth, format.toLowerCase());
            
            System.out.println("Generating payroll summary from database view for: " + payMonth);
            return generateReport(templatePath, parameters, dataSource, outputFileName, format);
            
        } catch (Exception e) {
            System.err.println("Error generating payroll summary from view: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Generates rank-and-file overtime report using database views
     * @param payMonth Pay month in YYYY-MM format
     * @param format Output format
     * @return Generated file path
     */
    public String generateRankAndFileOvertimeReport(String payMonth, String format) {
        try {
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("PAY_MONTH", payMonth);
            parameters.put("COMPANY_NAME", "MotorPH");
            parameters.put("REPORT_DATE", new Date());
            parameters.put("GENERATED_DATE", LocalDate.now(MANILA_TIMEZONE).format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")));
            
            // Get overtime data for rank-and-file employees only
            List<Map<String, Object>> overtimeData = getRankAndFileOvertimeFromView(payMonth);
            
            if (overtimeData.isEmpty()) {
                System.err.println("No rank-and-file overtime data found for " + payMonth);
                return null;
            }
            
            // Calculate overtime totals
            double totalOvertimeHours = overtimeData.stream()
                    .mapToDouble(row -> ((Number) row.getOrDefault("Overtime Hours", 0)).doubleValue())
                    .sum();
            
            parameters.put("TOTAL_OVERTIME_HOURS", totalOvertimeHours);
            parameters.put("ELIGIBLE_EMPLOYEES", overtimeData.size());
            parameters.put("OVERTIME_RATE", "1.25x (Rank-and-File Rate)");
            
            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(overtimeData);
            
            String templatePath = findTemplatePath("rank_and_file_overtime_template.jrxml");
            if (templatePath == null) {
                System.err.println("Rank-and-file overtime template not found");
                return null;
            }
            
            String outputFileName = String.format("Rank_And_File_Overtime_%s.%s", payMonth, format.toLowerCase());
            
            System.out.println("Generating rank-and-file overtime report from database view for: " + payMonth);
            return generateReport(templatePath, parameters, dataSource, outputFileName, format);
            
        } catch (Exception e) {
            System.err.println("Error generating rank-and-file overtime report: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Generates government compliance report using database views
     * @param payMonth Pay month in YYYY-MM format
     * @param format Output format
     * @return Generated file path
     */
    public String generateGovernmentComplianceReport(String payMonth, String format) {
        try {
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("PAY_MONTH", payMonth);
            parameters.put("COMPANY_NAME", "MotorPH");
            parameters.put("REPORT_DATE", new Date());
            parameters.put("GENERATED_DATE", LocalDate.now(MANILA_TIMEZONE).format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")));
            
            // Get compliance data from view
            Map<String, Object> complianceData = getGovernmentComplianceFromView(payMonth);
            
            if (complianceData.isEmpty()) {
                System.err.println("No compliance data found for " + payMonth);
                return null;
            }
            
            // Add compliance data to parameters
            parameters.putAll(complianceData);
            
            // Create a single-row data source for the compliance report
            List<Map<String, Object>> reportData = Arrays.asList(complianceData);
            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(reportData);
            
            String templatePath = findTemplatePath("government_compliance_template.jrxml");
            if (templatePath == null) {
                System.err.println("Government compliance template not found");
                return null;
            }
            
            String outputFileName = String.format("Government_Compliance_%s.%s", payMonth, format.toLowerCase());
            
            System.out.println("Generating government compliance report from database view for: " + payMonth);
            return generateReport(templatePath, parameters, dataSource, outputFileName, format);
            
        } catch (Exception e) {
            System.err.println("Error generating government compliance report: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    // ================================
    // LEGACY REPORT METHODS (Enhanced)
    // ================================
    
    /**
     * Generates a payslip report for a specific employee and pay period (legacy method)
     * @param employeeId Employee ID
     * @param payPeriodId Pay period ID
     * @param format Output format ("PDF", "EXCEL", "XLSX")
     * @return Generated file path
     */
    public String generatePayslipReport(Integer employeeId, Integer payPeriodId, String format) {
        try {
            // Convert payPeriodId to payMonth format for view-based generation
            String payMonth = getCurrentMonthString(); // You might want to get actual month from payPeriodId
            return generatePayslipFromView(employeeId, payMonth, format);
            
        } catch (Exception e) {
            System.err.println("Error generating payslip report: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Generates attendance report for an employee within date range
     * @param employeeId Employee ID (null for all employees)
     * @param startDate Start date
     * @param endDate End date
     * @param format Output format
     * @return Generated file path
     */
    public String generateAttendanceReport(Integer employeeId, LocalDate startDate, LocalDate endDate, String format) {
        try {
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("START_DATE", java.sql.Date.valueOf(startDate));
            parameters.put("END_DATE", java.sql.Date.valueOf(endDate));
            parameters.put("COMPANY_NAME", "MotorPH");
            parameters.put("REPORT_DATE", new Date());
            parameters.put("GENERATED_DATE", LocalDate.now(MANILA_TIMEZONE).format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")));
            
            String reportScope = "All_Employees";
            if (employeeId != null) {
                parameters.put("EMPLOYEE_ID", employeeId);
                var employee = employeeDAO.findById(employeeId);
                if (employee != null) {
                    parameters.put("EMPLOYEE_NAME", employee.getFirstName() + " " + employee.getLastName());
                    reportScope = employee.getLastName() + "_" + employeeId;
                }
            }
            
            // Get attendance data
            List<Map<String, Object>> attendanceData = getAttendanceData(employeeId, startDate, endDate);
            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(attendanceData);
            
            String templatePath = findTemplatePath("attendance_template.jrxml");
            if (templatePath == null) {
                System.err.println("Attendance template not found");
                return null;
            }
            
            String outputFileName = String.format("Attendance_%s_%s_to_%s.%s", 
                reportScope, startDate.toString(), endDate.toString(), format.toLowerCase());
            
            return generateReport(templatePath, parameters, dataSource, outputFileName, format);
            
        } catch (Exception e) {
            System.err.println("Error generating attendance report: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Generates employee list report
     * @param department Department filter (null for all departments)
     * @param status Status filter (null for all statuses)
     * @param format Output format
     * @return Generated file path
     */
    public String generateEmployeeListReport(String department, String status, String format) {
        try {
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("COMPANY_NAME", "MotorPH");
            parameters.put("REPORT_DATE", new Date());
            parameters.put("GENERATED_DATE", LocalDate.now(MANILA_TIMEZONE).format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")));
            parameters.put("DEPARTMENT_FILTER", department != null ? department : "All Departments");
            parameters.put("STATUS_FILTER", status != null ? status : "All Statuses");
            
            // Get employee data with rank-and-file classification
            List<Map<String, Object>> employeeData = getEmployeeListDataWithClassification(department, status);
            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(employeeData);
            
            String templatePath = findTemplatePath("employee_list_template.jrxml");
            if (templatePath == null) {
                System.err.println("Employee list template not found");
                return null;
            }
            
            String outputFileName = String.format("Employee_List_%s_%s_%s.%s", 
                department != null ? department : "All", 
                status != null ? status : "All",
                LocalDate.now().toString(), format.toLowerCase());
            
            return generateReport(templatePath, parameters, dataSource, outputFileName, format);
            
        } catch (Exception e) {
            System.err.println("Error generating employee list report: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Generates leave report for employees
     * @param employeeId Employee ID (null for all employees)
     * @param startDate Start date
     * @param endDate End date
     * @param leaveTypeId Leave type filter (null for all types)
     * @param format Output format
     * @return Generated file path
     */
    public String generateLeaveReport(Integer employeeId, LocalDate startDate, LocalDate endDate, 
                                    Integer leaveTypeId, String format) {
        try {
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("START_DATE", java.sql.Date.valueOf(startDate));
            parameters.put("END_DATE", java.sql.Date.valueOf(endDate));
            parameters.put("COMPANY_NAME", "MotorPH");
            parameters.put("REPORT_DATE", new Date());
            parameters.put("GENERATED_DATE", LocalDate.now(MANILA_TIMEZONE).format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")));
            
            String reportScope = "All_Employees";
            if (employeeId != null) {
                parameters.put("EMPLOYEE_ID", employeeId);
                var employee = employeeDAO.findById(employeeId);
                if (employee != null) {
                    reportScope = employee.getLastName() + "_" + employeeId;
                }
            }
            
            // Handle leave type parameter with proper null checking
            if (leaveTypeId != null) {
                var leaveType = referenceDataDAO.getLeaveTypeById(leaveTypeId);
                if (leaveType != null) {
                    String leaveTypeName = (String) leaveType.get("leaveTypeName");
                    parameters.put("LEAVE_TYPE", leaveTypeName != null ? leaveTypeName : "Unknown");
                    parameters.put("LEAVE_TYPE_ID", leaveTypeId);
                } else {
                    System.err.println("Warning: Leave type not found for ID: " + leaveTypeId);
                    parameters.put("LEAVE_TYPE", "Unknown Leave Type");
                    parameters.put("LEAVE_TYPE_ID", leaveTypeId);
                }
            } else {
                parameters.put("LEAVE_TYPE", "All Leave Types");
                parameters.put("LEAVE_TYPE_ID", null);
            }
            
            // Get leave data
            List<Map<String, Object>> leaveData = getLeaveData(employeeId, startDate, endDate, leaveTypeId);
            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(leaveData);
            
            String templatePath = findTemplatePath("leave_template.jrxml");
            if (templatePath == null) {
                System.err.println("Leave template not found");
                return null;
            }
            
            String outputFileName = String.format("Leave_Report_%s_%s_to_%s.%s", 
                reportScope, startDate.toString(), endDate.toString(), format.toLowerCase());
            
            return generateReport(templatePath, parameters, dataSource, outputFileName, format);
            
        } catch (Exception e) {
            System.err.println("Error generating leave report: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Generates overtime report
     * @param employeeId Employee ID (null for all employees)
     * @param startDate Start date
     * @param endDate End date
     * @param format Output format
     * @return Generated file path
     */
    public String generateOvertimeReport(Integer employeeId, LocalDate startDate, LocalDate endDate, String format) {
        try {
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("START_DATE", java.sql.Date.valueOf(startDate));
            parameters.put("END_DATE", java.sql.Date.valueOf(endDate));
            parameters.put("COMPANY_NAME", "MotorPH");
            parameters.put("REPORT_DATE", new Date());
            parameters.put("GENERATED_DATE", LocalDate.now(MANILA_TIMEZONE).format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")));
            
            String reportScope = "All_Employees";
            if (employeeId != null) {
                parameters.put("EMPLOYEE_ID", employeeId);
                var employee = employeeDAO.findById(employeeId);
                if (employee != null) {
                    reportScope = employee.getLastName() + "_" + employeeId;
                }
            }
            
            // Get overtime data
            List<Map<String, Object>> overtimeData = getOvertimeData(employeeId, startDate, endDate);
            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(overtimeData);
            
            String templatePath = findTemplatePath("overtime_template.jrxml");
            if (templatePath == null) {
                System.err.println("Overtime template not found");
                return null;
            }
            
            String outputFileName = String.format("Overtime_Report_%s_%s_to_%s.%s", 
                reportScope, startDate.toString(), endDate.toString(), format.toLowerCase());
            
            return generateReport(templatePath, parameters, dataSource, outputFileName, format);
            
        } catch (Exception e) {
            System.err.println("Error generating overtime report: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Generates payroll summary report for a pay period
     * @param payPeriodId Pay period ID
     * @param department Department filter (null for all)
     * @param format Output format
     * @return Generated file path
     */
    public String generatePayrollSummaryReport(Integer payPeriodId, String department, String format) {
        try {
            // Convert to current month for view-based generation
            String payMonth = getCurrentMonthString();
            return generateMonthlyPayrollSummaryFromView(payMonth, format);
            
        } catch (Exception e) {
            System.err.println("Error generating payroll summary report: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    // ================================
    // DATABASE VIEW DATA RETRIEVAL METHODS
    // ================================
    
    /**
     * Gets payslip data from monthly_employee_payslip view
     * @param employeeId Employee ID
     * @param payMonth Pay month in YYYY-MM format
     * @return List of payslip data
     */
    private List<Map<String, Object>> getPayslipDataFromView(Integer employeeId, String payMonth) {
        String sql = """
            SELECT * FROM monthly_employee_payslip
            WHERE `Employee ID` = ?
            AND DATE_FORMAT(`Period Start Date`, '%Y-%m') = ?
            """;
        
        return executeQuery(sql, employeeId, payMonth);
    }
    
    /**
     * Gets payroll summary data from monthly_payroll_summary_report view
     * @param payMonth Pay month in YYYY-MM format
     * @return List of payroll summary data
     */
    private List<Map<String, Object>> getPayrollSummaryFromView(String payMonth) {
        String sql = """
            SELECT * FROM monthly_payroll_summary_report
            WHERE DATE_FORMAT(`Pay Date`, '%Y-%m') = ?
            AND `Employee ID` != 'TOTAL'
            ORDER BY `Employee ID`
            """;
        
        return executeQuery(sql, payMonth);
    }
    
    /**
     * Gets rank-and-file overtime data from monthly_employee_payslip view
     * @param payMonth Pay month in YYYY-MM format
     * @return List of overtime data for rank-and-file employees only
     */
    private List<Map<String, Object>> getRankAndFileOvertimeFromView(String payMonth) {
        String sql = """
            SELECT p.`Employee ID`, p.`Employee Name`, p.`Department`, 
                   p.`Overtime Hours`, p.`Daily Rate`, p.`Employee Position`
            FROM monthly_employee_payslip p
            JOIN employee e ON p.`Employee ID` = e.employeeId
            JOIN position pos ON e.positionId = pos.positionId
            WHERE DATE_FORMAT(p.`Period Start Date`, '%Y-%m') = ?
            AND p.`Overtime Hours` > 0
            AND (LOWER(pos.department) = 'rank-and-file' 
                 OR LOWER(pos.position) LIKE '%rank%file%')
            ORDER BY p.`Overtime Hours` DESC
            """;
        
        return executeQuery(sql, payMonth);
    }
    
    /**
     * Gets government compliance data from monthly_payroll_summary_report view
     * @param payMonth Pay month in YYYY-MM format
     * @return Map of compliance totals
     */
    private Map<String, Object> getGovernmentComplianceFromView(String payMonth) {
        String sql = """
            SELECT 
                COUNT(*) as totalEmployees,
                SUM(`Social Security Contribution`) as totalSSS,
                SUM(`Philhealth Contribution`) as totalPhilHealth,
                SUM(`Pag-Ibig Contribution`) as totalPagIbig,
                SUM(`Withholding Tax`) as totalWithholdingTax
            FROM monthly_payroll_summary_report
            WHERE DATE_FORMAT(`Pay Date`, '%Y-%m') = ?
            AND `Employee ID` != 'TOTAL'
            """;
        
        List<Map<String, Object>> results = executeQuery(sql, payMonth);
        return results.isEmpty() ? new HashMap<>() : results.get(0);
    }
    
    // ================================
    // LEGACY DATA RETRIEVAL METHODS
    // ================================
    
    private List<Map<String, Object>> getAttendanceData(Integer employeeId, LocalDate startDate, LocalDate endDate) {
        StringBuilder sql = new StringBuilder("""
            SELECT e.employeeId, e.firstName, e.lastName,
                   a.date as attendanceDate, a.timeIn, a.timeOut
            FROM employee e
            LEFT JOIN attendance a ON e.employeeId = a.employeeId
            WHERE a.date BETWEEN ? AND ?
            """);
        
        List<Object> params = new ArrayList<>();
        params.add(java.sql.Date.valueOf(startDate));
        params.add(java.sql.Date.valueOf(endDate));
        
        if (employeeId != null) {
            sql.append(" AND e.employeeId = ?");
            params.add(employeeId);
        }
        
        sql.append(" ORDER BY e.lastName, e.firstName, a.date");
        
        return executeQuery(sql.toString(), params.toArray());
    }
    
    private List<Map<String, Object>> getEmployeeListDataWithClassification(String department, String status) {
        StringBuilder sql = new StringBuilder("""
            SELECT e.employeeId, e.firstName, e.lastName, e.email, e.phoneNumber,
                   e.status, e.userRole, p.position as positionTitle, p.department, e.basicSalary,
                   CASE WHEN (LOWER(p.department) = 'rank-and-file' 
                             OR LOWER(p.position) LIKE '%rank%file%') 
                        THEN 'Rank-and-File' 
                        ELSE 'Non Rank-and-File' END as employeeCategory,
                   CASE WHEN (LOWER(p.department) = 'rank-and-file' 
                             OR LOWER(p.position) LIKE '%rank%file%') 
                        THEN 'Yes (1.25x rate)' 
                        ELSE 'No' END as overtimeEligible
            FROM employee e
            JOIN position p ON e.positionId = p.positionId
            WHERE 1=1
            """);
        
        List<Object> params = new ArrayList<>();
        
        if (department != null && !department.trim().isEmpty()) {
            sql.append(" AND p.department = ?");
            params.add(department);
        }
        
        if (status != null && !status.trim().isEmpty()) {
            sql.append(" AND e.status = ?");
            params.add(status);
        }
        
        sql.append(" ORDER BY p.department, e.lastName, e.firstName");
        
        return executeQuery(sql.toString(), params.toArray());
    }
    
    private List<Map<String, Object>> getLeaveData(Integer employeeId, LocalDate startDate, 
                                                  LocalDate endDate, Integer leaveTypeId) {
        StringBuilder sql = new StringBuilder("""
            SELECT e.employeeId, e.firstName, e.lastName,
                   lr.leaveRequestId, lr.leaveStart as startDate, lr.leaveEnd as endDate, 
                   lr.approvalStatus as status, lt.leaveTypeName, lr.leaveReason as reason
            FROM employee e
            LEFT JOIN leaverequest lr ON e.employeeId = lr.employeeId
            LEFT JOIN leavetype lt ON lr.leaveTypeId = lt.leaveTypeId
            WHERE lr.leaveStart <= ? AND lr.leaveEnd >= ?
            """);
        
        List<Object> params = new ArrayList<>();
        params.add(java.sql.Date.valueOf(endDate));
        params.add(java.sql.Date.valueOf(startDate));
        
        if (employeeId != null) {
            sql.append(" AND e.employeeId = ?");
            params.add(employeeId);
        }
        
        if (leaveTypeId != null) {
            sql.append(" AND lr.leaveTypeId = ?");
            params.add(leaveTypeId);
        }
        
        sql.append(" ORDER BY e.lastName, e.firstName, lr.leaveStart");
        
        return executeQuery(sql.toString(), params.toArray());
    }
    
    private List<Map<String, Object>> getOvertimeData(Integer employeeId, LocalDate startDate, LocalDate endDate) {
        StringBuilder sql = new StringBuilder("""
            SELECT e.employeeId, e.firstName, e.lastName,
                   o.overtimeStart, o.overtimeEnd, o.approvalStatus as status, o.overtimeReason as reason,
                   TIMESTAMPDIFF(MINUTE, o.overtimeStart, o.overtimeEnd) / 60.0 as overtimeHours,
                   CASE WHEN (LOWER(p.department) = 'rank-and-file' 
                             OR LOWER(p.position) LIKE '%rank%file%') 
                        THEN 'Yes (1.25x rate)' 
                        ELSE 'Not Eligible' END as overtimeEligible
            FROM employee e
            LEFT JOIN overtimerequest o ON e.employeeId = o.employeeId
            JOIN position p ON e.positionId = p.positionId
            WHERE DATE(o.overtimeStart) BETWEEN ? AND ?
            """);
        
        List<Object> params = new ArrayList<>();
        params.add(java.sql.Date.valueOf(startDate));
        params.add(java.sql.Date.valueOf(endDate));
        
        if (employeeId != null) {
            sql.append(" AND e.employeeId = ?");
            params.add(employeeId);
        }
        
        sql.append(" ORDER BY e.lastName, e.firstName, o.overtimeStart");
        
        return executeQuery(sql.toString(), params.toArray());
    }
    
    // ================================
    // HELPER METHODS
    // ================================
    
    /**
     * Core method to generate reports
     * @param templatePath Path to .jrxml template
     * @param parameters Report parameters
     * @param dataSource Data source for the report
     * @param outputFileName Output file name
     * @param format Output format
     * @return Generated file path
     */
    private String generateReport(String templatePath, Map<String, Object> parameters, 
                                JRBeanCollectionDataSource dataSource, String outputFileName, String format) {
        try {
            // Validate template exists
            if (!templateExists(templatePath)) {
                throw new FileNotFoundException("Report template not found: " + templatePath);
            }
            
            // Compile the report template
            JasperReport jasperReport = JasperCompileManager.compileReport(templatePath);
            
            // Fill the report with data
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
            
            // Export based on format
            String outputPath = OUTPUT_PATH + outputFileName;
            
            switch (format.toUpperCase()) {
                case "PDF":
                    JasperExportManager.exportReportToPdfFile(jasperPrint, outputPath);
                    break;
                case "EXCEL":
                case "XLS":
                case "XLSX":
                    // For now, just export as PDF - you can add Excel export later
                    outputPath = outputPath.replace(".xls", ".pdf").replace(".xlsx", ".pdf");
                    JasperExportManager.exportReportToPdfFile(jasperPrint, outputPath);
                    System.out.println("Note: Excel export not implemented yet, exported as PDF instead");
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported format: " + format);
            }
            
            System.out.println("Report generated successfully: " + outputPath);
            return outputPath;
            
        } catch (Exception e) {
            System.err.println("Error generating report: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Calculates totals from payroll summary data
     * @param summaryData List of payroll summary records
     * @return PayrollTotals object
     */
    private PayrollTotals calculateTotalsFromSummary(List<Map<String, Object>> summaryData) {
        PayrollTotals totals = new PayrollTotals();
        
        for (Map<String, Object> row : summaryData) {
            // Skip the TOTAL row if it exists
            if ("TOTAL".equals(row.get("Employee ID"))) {
                continue;
            }
            
            totals.incrementEmployeeCount();
            totals.addToTotalGrossIncome(getBigDecimalValue(row, "GROSS INCOME"));
            totals.addToTotalBenefits(getBigDecimalValue(row, "TOTAL BENEFITS"));
            totals.addToTotalDeductions(getBigDecimalValue(row, "TOTAL DEDUCTIONS"));
            totals.addToTotalNetPay(getBigDecimalValue(row, "NET PAY"));
        }
        
        return totals;
    }
    
    /**
     * Safely gets BigDecimal value from map
     * @param row Data row
     * @param key Column key
     * @return BigDecimal value or ZERO if null/invalid
     */
    private java.math.BigDecimal getBigDecimalValue(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value == null) {
            return java.math.BigDecimal.ZERO;
        }
        if (value instanceof java.math.BigDecimal) {
            return (java.math.BigDecimal) value;
        }
        if (value instanceof Number) {
            return java.math.BigDecimal.valueOf(((Number) value).doubleValue());
        }
        try {
            return new java.math.BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            return java.math.BigDecimal.ZERO;
        }
    }
    
    /**
     * Gets current month string in YYYY-MM format
     * @return Current month string
     */
    private String getCurrentMonthString() {
        return LocalDate.now(MANILA_TIMEZONE).format(DateTimeFormatter.ofPattern("yyyy-MM"));
    }
    
    /**
     * Generic query execution method
     */
    private List<Map<String, Object>> executeQuery(String sql, Object... params) {
        List<Map<String, Object>> results = new ArrayList<>();
        
        try (Connection conn = databaseConnection.createConnection();
             var stmt = conn.prepareStatement(sql)) {
            
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            
            try (var rs = stmt.executeQuery()) {
                var metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();
                
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = metaData.getColumnLabel(i);
                        Object value = rs.getObject(i);
                        row.put(columnName, value);
                    }
                    results.add(row);
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error executing query: " + e.getMessage());
            e.printStackTrace();
        }
        
        return results;
    }
    
    /**
     * Creates output directory if it doesn't exist
     */
    private void createOutputDirectory() {
        File dir = new File(OUTPUT_PATH);
        if (!dir.exists()) {
            dir.mkdirs();
            System.out.println("Created output directory: " + OUTPUT_PATH);
        }
    }
    
    /**
     * Gets list of available report templates
     * @return List of available templates
     */
    public List<String> getAvailableTemplates() {
        List<String> templates = new ArrayList<>();
        File reportsDir = new File(REPORTS_PATH);
        
        if (reportsDir.exists() && reportsDir.isDirectory()) {
            File[] files = reportsDir.listFiles((dir, name) -> name.endsWith(".jrxml"));
            if (files != null) {
                for (File file : files) {
                    templates.add(file.getName());
                }
            }
        }
        
        return templates;
    }
    
    /**
     * Validates if a template exists
     * @param templatePath Template file path
     * @return true if template exists
     */
    public boolean templateExists(String templatePath) {
        File templateFile = new File(templatePath);
        return templateFile.exists() && templateFile.isFile();
    }
    
    /**
     * Gets current Manila date
     * @return Current date in Manila timezone
     */
    public LocalDate getCurrentManilaDate() {
        return LocalDate.now(MANILA_TIMEZONE);
    }
    
    /**
     * Test method to verify everything is set up correctly - FOR DEBUGGING
     */
    public boolean testSetup() {
        try {
            System.out.println("=== JASPERREPORTS SETUP TEST ===");
            
            // Test 1: Template path resolution
            String templateName = "MotorPH Employee Payslip.jrxml";
            String templatePath = findTemplatePath(templateName);
            System.out.println("1. Template resolution: " + (templatePath != null ? "SUCCESS" : "FAILED"));
            
            // Test 2: Logo path resolution
            String logoPath = findLogoPath();
            System.out.println("2. Logo resolution: " + (new File(logoPath).exists() ? "SUCCESS" : "FAILED"));
            
            // Test 3: Output directory
            File outputDir = new File(OUTPUT_PATH);
            System.out.println("3. Output directory: " + (outputDir.exists() ? "EXISTS" : "CREATED"));
            
            // Test 4: Database connection
            try (Connection conn = databaseConnection.getConnection()) {
                System.out.println("4. Database connection: " + (conn != null ? "SUCCESS" : "FAILED"));
            }
            
            // Test 5: Template compilation
            if (templatePath != null) {
                try {
                    JasperReport report = JasperCompileManager.compileReport(templatePath);
                    System.out.println("5. Template compilation: " + (report != null ? "SUCCESS" : "FAILED"));
                } catch (Exception e) {
                    System.out.println("5. Template compilation: FAILED - " + e.getMessage());
                }
            }
            
            System.out.println("=== SETUP TEST COMPLETE ===");
            return true;
            
        } catch (Exception e) {
            System.err.println("Setup test failed: " + e.getMessage());
            return false;
        }
    }
    
    // ================================
    // INNER CLASSES
    // ================================
    
    /**
     * Helper class for calculating payroll totals
     */
    public static class PayrollTotals {
        private int employeeCount = 0;
        private java.math.BigDecimal totalGrossIncome = java.math.BigDecimal.ZERO;
        private java.math.BigDecimal totalBenefits = java.math.BigDecimal.ZERO;
        private java.math.BigDecimal totalDeductions = java.math.BigDecimal.ZERO;
        private java.math.BigDecimal totalNetPay = java.math.BigDecimal.ZERO;
        
        public void incrementEmployeeCount() {
            this.employeeCount++;
        }
        
        public void addToTotalGrossIncome(java.math.BigDecimal amount) {
            this.totalGrossIncome = this.totalGrossIncome.add(amount != null ? amount : java.math.BigDecimal.ZERO);
        }
        
        public void addToTotalBenefits(java.math.BigDecimal amount) {
            this.totalBenefits = this.totalBenefits.add(amount != null ? amount : java.math.BigDecimal.ZERO);
        }
        
        public void addToTotalDeductions(java.math.BigDecimal amount) {
            this.totalDeductions = this.totalDeductions.add(amount != null ? amount : java.math.BigDecimal.ZERO);
        }
        
        public void addToTotalNetPay(java.math.BigDecimal amount) {
            this.totalNetPay = this.totalNetPay.add(amount != null ? amount : java.math.BigDecimal.ZERO);
        }
        
        // Getters
        public int getEmployeeCount() { return employeeCount; }
        public java.math.BigDecimal getTotalGrossIncome() { return totalGrossIncome; }
        public java.math.BigDecimal getTotalBenefits() { return totalBenefits; }
        public java.math.BigDecimal getTotalDeductions() { return totalDeductions; }
        public java.math.BigDecimal getTotalNetPay() { return totalNetPay; }
        
        @Override
        public String toString() {
            return String.format("PayrollTotals{employees=%d, gross=%s, benefits=%s, deductions=%s, net=%s}",
                    employeeCount, totalGrossIncome, totalBenefits, totalDeductions, totalNetPay);
        }
    }
}