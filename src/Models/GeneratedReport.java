
package Models;

import DAOs.DatabaseConnection;
import DAOs.EmployeeDAO;
import DAOs.ReferenceDataDAO;
import Services.JasperReportGenerator;
import Models.EmployeeModel;
import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.io.File;
import java.util.*;

/**
 * GeneratedReport manages report generation, storage, and distribution
 * Handles report requests, generation tracking, report metadata, and report scheduling
 * Integrates with JasperReportGenerator and provides comprehensive report management
 * @author Chadley
 */

public class GeneratedReport {
  private final DatabaseConnection databaseConnection;
    private final EmployeeDAO employeeDAO;
    private final ReferenceDataDAO referenceDataDAO;
    private final JasperReportGenerator reportGenerator;
    
    // Report status constants
    public static final String STATUS_PENDING = "Pending";
    public static final String STATUS_GENERATING = "Generating";
    public static final String STATUS_COMPLETED = "Completed";
    public static final String STATUS_FAILED = "Failed";
    public static final String STATUS_EXPIRED = "Expired";
    
    // Report type constants
    public static final String TYPE_PAYSLIP = "Payslip";
    public static final String TYPE_ATTENDANCE = "Attendance";
    public static final String TYPE_LEAVE = "Leave";
    public static final String TYPE_OVERTIME = "Overtime";
    public static final String TYPE_EMPLOYEE_LIST = "EmployeeList";
    public static final String TYPE_PAYROLL_SUMMARY = "PayrollSummary";
    public static final String TYPE_BENEFIT_REPORT = "BenefitReport";
    public static final String TYPE_DEDUCTION_REPORT = "DeductionReport";
    public static final String TYPE_CUSTOM = "Custom";
    
    /**
     * Constructor
     * @param databaseConnection Database connection instance
     */
    public GeneratedReport(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
        this.employeeDAO = new EmployeeDAO(databaseConnection);
        this.referenceDataDAO = new ReferenceDataDAO(databaseConnection);
        this.reportGenerator = new JasperReportGenerator(databaseConnection);
    }
    
    /**
     * Requests a new report generation
     * @param reportType Type of report to generate
     * @param requestedBy Employee ID who requested the report
     * @param reportName Custom name for the report
     * @param parameters Report parameters (filters, dates, etc.)
     * @param format Output format (PDF, EXCEL, etc.)
     * @return Report request ID if successful, null if failed
     */
    public Integer requestReport(String reportType, Integer requestedBy, String reportName, 
                               Map<String, Object> parameters, String format) {
        try {
            // Validate requester
            EmployeeModel requester = employeeDAO.findById(requestedBy);
            if (requester == null) {
                System.err.println("Invalid requester ID: " + requestedBy);
                return null;
            }
            
            // Validate report type
            if (!isValidReportType(reportType)) {
                System.err.println("Invalid report type: " + reportType);
                return null;
            }
            
            // Generate unique report filename
            String fileName = generateReportFileName(reportType, reportName, format);
            
            // Create report request record
            String sql = "INSERT INTO generated_report " +
                        "(reportType, reportName, fileName, format, status, requestedBy, " +
                        "requestedDate, parameters, expiryDate) " +
                        "VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, ?, DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 30 DAY))";
            
            try (Connection conn = databaseConnection.createConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                
                stmt.setString(1, reportType);
                stmt.setString(2, reportName);
                stmt.setString(3, fileName);
                stmt.setString(4, format.toUpperCase());
                stmt.setString(5, STATUS_PENDING);
                stmt.setInt(6, requestedBy);
                stmt.setString(7, serializeParameters(parameters));
                
                int rowsAffected = stmt.executeUpdate();
                
                if (rowsAffected > 0) {
                    try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            Integer reportId = generatedKeys.getInt(1);
                            
                            System.out.println("Report request created successfully:");
                            System.out.println("Report ID: " + reportId);
                            System.out.println("Type: " + reportType);
                            System.out.println("Requested by: " + requester.getFirstName() + " " + requester.getLastName());
                            
                            // Trigger report generation
                            generateReportAsync(reportId);
                            
                            return reportId;
                        }
                    }
                }
                
            }
            
        } catch (SQLException e) {
            System.err.println("Error requesting report: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * Generates a report synchronously
     * @param reportId Report request ID
     * @return true if successful, false otherwise
     */
    public boolean generateReport(Integer reportId) {
        try {
            // Get report details
            ReportDetails reportDetails = getReportDetails(reportId);
            if (reportDetails == null) {
                System.err.println("Report not found: " + reportId);
                return false;
            }
            
            // Update status to generating
            updateReportStatus(reportId, STATUS_GENERATING, null);
            
            // Generate the actual report
            String generatedFilePath = null;
            Map<String, Object> parameters = deserializeParameters(reportDetails.getParameters());
            
            switch (reportDetails.getReportType()) {
                case TYPE_PAYSLIP:
                    generatedFilePath = generatePayslipReport(parameters, reportDetails.getFormat());
                    break;
                case TYPE_ATTENDANCE:
                    generatedFilePath = generateAttendanceReport(parameters, reportDetails.getFormat());
                    break;
                case TYPE_LEAVE:
                    generatedFilePath = generateLeaveReport(parameters, reportDetails.getFormat());
                    break;
                case TYPE_OVERTIME:
                    generatedFilePath = generateOvertimeReport(parameters, reportDetails.getFormat());
                    break;
                case TYPE_EMPLOYEE_LIST:
                    generatedFilePath = generateEmployeeListReport(parameters, reportDetails.getFormat());
                    break;
                case TYPE_PAYROLL_SUMMARY:
                    generatedFilePath = generatePayrollSummaryReport(parameters, reportDetails.getFormat());
                    break;
                default:
                    System.err.println("Unsupported report type: " + reportDetails.getReportType());
                    updateReportStatus(reportId, STATUS_FAILED, "Unsupported report type");
                    return false;
            }
            
            if (generatedFilePath != null) {
                // Update report with file details
                updateReportCompletion(reportId, generatedFilePath);
                System.out.println("Report generated successfully: " + generatedFilePath);
                return true;
            } else {
                updateReportStatus(reportId, STATUS_FAILED, "Report generation failed");
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("Error generating report: " + e.getMessage());
            e.printStackTrace();
            updateReportStatus(reportId, STATUS_FAILED, e.getMessage());
            return false;
        }
    }
    
    /**
     * Generates a report asynchronously (placeholder for background processing)
     * @param reportId Report request ID
     */
    private void generateReportAsync(Integer reportId) {
        // In a real implementation, this would use a thread pool or job queue
        // For now, we'll generate synchronously
        new Thread(() -> generateReport(reportId)).start();
    }
    
    /**
     * Gets list of reports for a user
     * @param userId Employee ID
     * @param status Filter by status (null for all)
     * @param reportType Filter by type (null for all)
     * @return List of user's reports
     */
    public List<Map<String, Object>> getUserReports(Integer userId, String status, String reportType) {
        StringBuilder sql = new StringBuilder(
            "SELECT gr.*, e.firstName, e.lastName " +
            "FROM generated_report gr " +
            "JOIN employee e ON gr.requestedBy = e.employeeId " +
            "WHERE gr.requestedBy = ?");
        
        List<Object> params = new ArrayList<>();
        params.add(userId);
        
        if (status != null && !status.trim().isEmpty()) {
            sql.append(" AND gr.status = ?");
            params.add(status);
        }
        
        if (reportType != null && !reportType.trim().isEmpty()) {
            sql.append(" AND gr.reportType = ?");
            params.add(reportType);
        }
        
        sql.append(" ORDER BY gr.requestedDate DESC");
        
        return executeQuery(sql.toString(), params.toArray());
    }
    
    /**
     * Gets all reports (for admin view)
     * @param status Filter by status (null for all)
     * @param reportType Filter by type (null for all)
     * @param startDate Filter from date (null for no filter)
     * @param endDate Filter to date (null for no filter)
     * @return List of all reports
     */
    public List<Map<String, Object>> getAllReports(String status, String reportType, 
                                                  LocalDate startDate, LocalDate endDate) {
        StringBuilder sql = new StringBuilder(
            "SELECT gr.*, e.firstName, e.lastName " +
            "FROM generated_report gr " +
            "JOIN employee e ON gr.requestedBy = e.employeeId " +
            "WHERE 1=1");
        
        List<Object> params = new ArrayList<>();
        
        if (status != null && !status.trim().isEmpty()) {
            sql.append(" AND gr.status = ?");
            params.add(status);
        }
        
        if (reportType != null && !reportType.trim().isEmpty()) {
            sql.append(" AND gr.reportType = ?");
            params.add(reportType);
        }
        
        if (startDate != null) {
            sql.append(" AND DATE(gr.requestedDate) >= ?");
            params.add(java.sql.Date.valueOf(startDate));
        }
        
        if (endDate != null) {
            sql.append(" AND DATE(gr.requestedDate) <= ?");
            params.add(java.sql.Date.valueOf(endDate));
        }
        
        sql.append(" ORDER BY gr.requestedDate DESC");
        
        return executeQuery(sql.toString(), params.toArray());
    }
    
    /**
     * Downloads a completed report
     * @param reportId Report ID
     * @param userId User requesting download (for security)
     * @return File path if accessible, null otherwise
     */
    public String downloadReport(Integer reportId, Integer userId) {
        try {
            ReportDetails reportDetails = getReportDetails(reportId);
            if (reportDetails == null) {
                System.err.println("Report not found: " + reportId);
                return null;
            }
            
            // Check if user has access to this report
            if (!hasReportAccess(reportId, userId)) {
                System.err.println("User does not have access to report: " + reportId);
                return null;
            }
            
            // Check if report is completed
            if (!STATUS_COMPLETED.equals(reportDetails.getStatus())) {
                System.err.println("Report is not completed: " + reportDetails.getStatus());
                return null;
            }
            
            // Check if file exists
            String filePath = reportDetails.getFilePath();
            if (filePath != null && new File(filePath).exists()) {
                // Update download count
                updateDownloadCount(reportId);
                return filePath;
            } else {
                System.err.println("Report file not found: " + filePath);
                return null;
            }
            
        } catch (Exception e) {
            System.err.println("Error downloading report: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Deletes a report
     * @param reportId Report ID
     * @param userId User requesting deletion (for security)
     * @return true if successful
     */
    public boolean deleteReport(Integer reportId, Integer userId) {
        try {
            // Check if user has access to this report
            if (!hasReportAccess(reportId, userId)) {
                System.err.println("User does not have access to report: " + reportId);
                return false;
            }
            
            // Get report details to delete file
            ReportDetails reportDetails = getReportDetails(reportId);
            if (reportDetails != null && reportDetails.getFilePath() != null) {
                File reportFile = new File(reportDetails.getFilePath());
                if (reportFile.exists()) {
                    reportFile.delete();
                }
            }
            
            // Delete from database
            String sql = "DELETE FROM generated_report WHERE reportId = ?";
            
            try (Connection conn = databaseConnection.createConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setInt(1, reportId);
                int rowsAffected = stmt.executeUpdate();
                
                if (rowsAffected > 0) {
                    System.out.println("Report deleted successfully: " + reportId);
                    return true;
                }
                
            }
            
        } catch (SQLException e) {
            System.err.println("Error deleting report: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Cleans up expired reports
     * @return Number of reports cleaned up
     */
    public int cleanupExpiredReports() {
        int cleanedUp = 0;
        
        try {
            // Get expired reports
            String selectSql = "SELECT reportId, filePath FROM generated_report " +
                             "WHERE expiryDate < CURRENT_TIMESTAMP OR status = ?";
            
            List<Map<String, Object>> expiredReports = executeQuery(selectSql, STATUS_EXPIRED);
            
            // Delete files and database records
            String deleteSql = "DELETE FROM generated_report WHERE reportId = ?";
            
            try (Connection conn = databaseConnection.createConnection();
                 PreparedStatement stmt = conn.prepareStatement(deleteSql)) {
                
                for (Map<String, Object> report : expiredReports) {
                    Integer reportId = (Integer) report.get("reportId");
                    String filePath = (String) report.get("filePath");
                    
                    // Delete file if exists
                    if (filePath != null) {
                        File reportFile = new File(filePath);
                        if (reportFile.exists()) {
                            reportFile.delete();
                        }
                    }
                    
                    // Delete database record
                    stmt.setInt(1, reportId);
                    stmt.executeUpdate();
                    cleanedUp++;
                }
                
            }
            
            if (cleanedUp > 0) {
                System.out.println("Cleaned up " + cleanedUp + " expired reports");
            }
            
        } catch (SQLException e) {
            System.err.println("Error cleaning up expired reports: " + e.getMessage());
        }
        
        return cleanedUp;
    }
    
    // REPORT GENERATION METHODS
    
    private String generatePayslipReport(Map<String, Object> parameters, String format) {
        Integer employeeId = (Integer) parameters.get("employeeId");
        Integer payPeriodId = (Integer) parameters.get("payPeriodId");
        
        if (employeeId != null && payPeriodId != null) {
            return reportGenerator.generatePayslipReport(employeeId, payPeriodId, format);
        }
        return null;
    }
    
    private String generateAttendanceReport(Map<String, Object> parameters, String format) {
        Integer employeeId = (Integer) parameters.get("employeeId");
        LocalDate startDate = (LocalDate) parameters.get("startDate");
        LocalDate endDate = (LocalDate) parameters.get("endDate");
        
        if (startDate != null && endDate != null) {
            return reportGenerator.generateAttendanceReport(employeeId, startDate, endDate, format);
        }
        return null;
    }
    
    private String generateLeaveReport(Map<String, Object> parameters, String format) {
        Integer employeeId = (Integer) parameters.get("employeeId");
        LocalDate startDate = (LocalDate) parameters.get("startDate");
        LocalDate endDate = (LocalDate) parameters.get("endDate");
        Integer leaveTypeId = (Integer) parameters.get("leaveTypeId");
        
        if (startDate != null && endDate != null) {
            return reportGenerator.generateLeaveReport(employeeId, startDate, endDate, leaveTypeId, format);
        }
        return null;
    }
    
    private String generateOvertimeReport(Map<String, Object> parameters, String format) {
        Integer employeeId = (Integer) parameters.get("employeeId");
        LocalDate startDate = (LocalDate) parameters.get("startDate");
        LocalDate endDate = (LocalDate) parameters.get("endDate");
        
        if (startDate != null && endDate != null) {
            return reportGenerator.generateOvertimeReport(employeeId, startDate, endDate, format);
        }
        return null;
    }
    
    private String generateEmployeeListReport(Map<String, Object> parameters, String format) {
        String department = (String) parameters.get("department");
        String status = (String) parameters.get("status");
        
        return reportGenerator.generateEmployeeListReport(department, status, format);
    }
    
    private String generatePayrollSummaryReport(Map<String, Object> parameters, String format) {
        Integer payPeriodId = (Integer) parameters.get("payPeriodId");
        String department = (String) parameters.get("department");
        
        if (payPeriodId != null) {
            return reportGenerator.generatePayrollSummaryReport(payPeriodId, department, format);
        }
        return null;
    }
    
    // UTILITY METHODS
    
    private boolean isValidReportType(String reportType) {
        return Arrays.asList(TYPE_PAYSLIP, TYPE_ATTENDANCE, TYPE_LEAVE, TYPE_OVERTIME,
                           TYPE_EMPLOYEE_LIST, TYPE_PAYROLL_SUMMARY, TYPE_BENEFIT_REPORT,
                           TYPE_DEDUCTION_REPORT, TYPE_CUSTOM).contains(reportType);
    }
    
    private String generateReportFileName(String reportType, String reportName, String format) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String baseName = reportName != null ? reportName.replaceAll("[^a-zA-Z0-9]", "_") : reportType;
        return String.format("%s_%s.%s", baseName, timestamp, format.toLowerCase());
    }
    
    private String serializeParameters(Map<String, Object> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return "{}";
        }
        
        // Simple JSON-like serialization (you could use a proper JSON library)
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(entry.getKey()).append("\":\"").append(entry.getValue()).append("\"");
            first = false;
        }
        
        sb.append("}");
        return sb.toString();
    }
    
    private Map<String, Object> deserializeParameters(String parametersStr) {
        Map<String, Object> parameters = new HashMap<>();
        
        if (parametersStr == null || parametersStr.trim().isEmpty() || "{}".equals(parametersStr)) {
            return parameters;
        }
        
        // Simple JSON-like deserialization (you could use a proper JSON library)
        // This is a basic implementation for demonstration
        parametersStr = parametersStr.replace("{", "").replace("}", "");
        String[] pairs = parametersStr.split(",");
        
        for (String pair : pairs) {
            String[] keyValue = pair.split(":");
            if (keyValue.length == 2) {
                String key = keyValue[0].replace("\"", "").trim();
                String value = keyValue[1].replace("\"", "").trim();
                parameters.put(key, value);
            }
        }
        
        return parameters;
    }
    
    private ReportDetails getReportDetails(Integer reportId) {
        String sql = "SELECT * FROM generated_report WHERE reportId = ?";
        
        List<Map<String, Object>> results = executeQuery(sql, reportId);
        if (!results.isEmpty()) {
            Map<String, Object> row = results.get(0);
            
            ReportDetails details = new ReportDetails();
            details.setReportId((Integer) row.get("reportId"));
            details.setReportType((String) row.get("reportType"));
            details.setReportName((String) row.get("reportName"));
            details.setFileName((String) row.get("fileName"));
            details.setFormat((String) row.get("format"));
            details.setStatus((String) row.get("status"));
            details.setFilePath((String) row.get("filePath"));
            details.setParameters((String) row.get("parameters"));
            details.setRequestedBy((Integer) row.get("requestedBy"));
            
            return details;
        }
        
        return null;
    }
    
    private boolean hasReportAccess(Integer reportId, Integer userId) {
        // Check if user requested the report or has admin privileges
        String sql = "SELECT COUNT(*) FROM generated_report gr " +
                    "JOIN employee e ON gr.requestedBy = e.employeeId " +
                    "WHERE gr.reportId = ? AND (gr.requestedBy = ? OR e.userRole IN ('HR', 'IT', 'Accounting'))";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, reportId);
            stmt.setInt(2, userId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
            
        } catch (SQLException e) {
            System.err.println("Error checking report access: " + e.getMessage());
            return false;
        }
    }
    
    private void updateReportStatus(Integer reportId, String status, String errorMessage) {
        String sql = "UPDATE generated_report SET status = ?, errorMessage = ?, updatedDate = CURRENT_TIMESTAMP WHERE reportId = ?";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, status);
            stmt.setString(2, errorMessage);
            stmt.setInt(3, reportId);
            
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            System.err.println("Error updating report status: " + e.getMessage());
        }
    }
    
    private void updateReportCompletion(Integer reportId, String filePath) {
        File file = new File(filePath);
        long fileSize = file.exists() ? file.length() : 0;
        
        String sql = "UPDATE generated_report SET status = ?, filePath = ?, fileSize = ?, " +
                    "completedDate = CURRENT_TIMESTAMP, updatedDate = CURRENT_TIMESTAMP WHERE reportId = ?";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, STATUS_COMPLETED);
            stmt.setString(2, filePath);
            stmt.setLong(3, fileSize);
            stmt.setInt(4, reportId);
            
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            System.err.println("Error updating report completion: " + e.getMessage());
        }
    }
    
    private void updateDownloadCount(Integer reportId) {
        String sql = "UPDATE generated_report SET downloadCount = downloadCount + 1, " +
                    "lastDownloadDate = CURRENT_TIMESTAMP WHERE reportId = ?";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, reportId);
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            System.err.println("Error updating download count: " + e.getMessage());
        }
    }
    
    private List<Map<String, Object>> executeQuery(String sql, Object... params) {
        List<Map<String, Object>> results = new ArrayList<>();
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            
            try (ResultSet rs = stmt.executeQuery()) {
                ResultSetMetaData metaData = rs.getMetaData();
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
        }
        
        return results;
    }
    
    /**
     * Inner class to hold report details
     */
    public static class ReportDetails {
        private Integer reportId;
        private String reportType;
        private String reportName;
        private String fileName;
        private String format;
        private String status;
        private String filePath;
        private String parameters;
        private Integer requestedBy;
        
        // Getters and setters
        public Integer getReportId() { return reportId; }
        public void setReportId(Integer reportId) { this.reportId = reportId; }
        
        public String getReportType() { return reportType; }
        public void setReportType(String reportType) { this.reportType = reportType; }
        
        public String getReportName() { return reportName; }
        public void setReportName(String reportName) { this.reportName = reportName; }
        
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        
        public String getFormat() { return format; }
        public void setFormat(String format) { this.format = format; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }
        
        public String getParameters() { return parameters; }
        public void setParameters(String parameters) { this.parameters = parameters; }
        
        public Integer getRequestedBy() { return requestedBy; }
        public void setRequestedBy(Integer requestedBy) { this.requestedBy = requestedBy; }
    }
}
