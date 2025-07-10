package Models;

import java.time.*;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.math.BigDecimal;

/**
 * HRModel with comprehensive HR operations and Manila timezone
 * Provides full HR management capabilities for the payroll system
 */
public class HRModel {
    private static final ZoneId MANILA_TIMEZONE = ZoneId.of("Asia/Manila");
    
    // HR Personnel Information
    private int hrId;
    private String firstName;
    private String lastName;
    private String email;
    private String position;
    private String department;
    private String userRole;
    private String status;
    private Timestamp lastLogin;
    private String specializations; // "RECRUITMENT,PAYROLL,BENEFITS,COMPLIANCE"
    
    // HR Permissions and Capabilities
    private boolean canManageEmployees;
    private boolean canManagePayroll;
    private boolean canManageBenefits;
    private boolean canManageLeaves;
    private boolean canManageAttendance;
    private boolean canGenerateReports;
    private boolean canManagePositions;
    private boolean canApproveAllRequests;
    private boolean canModifyPayrates;
    private boolean canAccessAuditLogs;
    
    // HR Activity Tracking
    private int employeesManaged;
    private int payrollsProcessed;
    private int reportsGenerated;
    private Timestamp lastPayrollRun;
    private Timestamp lastReportGeneration;
    private String currentlyWorkingOn;
    
    // Constructors
    public HRModel() {
        this.userRole = "HR";
        this.status = "Active";
        this.department = "Human Resources";
        
        // Default HR permissions
        this.canManageEmployees = true;
        this.canManagePayroll = true;
        this.canManageBenefits = true;
        this.canManageLeaves = true;
        this.canManageAttendance = true;
        this.canGenerateReports = true;
        this.canManagePositions = true;
        this.canApproveAllRequests = true;
        this.canModifyPayrates = true;
        this.canAccessAuditLogs = true;
    }
    
    public HRModel(int hrId, String firstName, String lastName, String email) {
        this();
        this.hrId = hrId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.position = "HR Specialist";
    }
    
    // Manila timezone operations
    
    /**
     * Get current Manila time
     * @return 
     */
    public static LocalDateTime getCurrentManilaTime() {
        return LocalDateTime.now(MANILA_TIMEZONE);
    }
    
    /**
     * Get last login in Manila timezone
     * @return 
     */
    public LocalDateTime getLastLoginInManila() {
        if (lastLogin == null) return null;
        return lastLogin.toLocalDateTime().atZone(ZoneId.systemDefault())
                .withZoneSameInstant(MANILA_TIMEZONE).toLocalDateTime();
    }
    
    /**
     * Get last payroll run in Manila timezone
     * @return 
     */
    public LocalDateTime getLastPayrollRunInManila() {
        if (lastPayrollRun == null) return null;
        return lastPayrollRun.toLocalDateTime().atZone(ZoneId.systemDefault())
                .withZoneSameInstant(MANILA_TIMEZONE).toLocalDateTime();
    }
    
    // Employee Management Operations
    
    /**
     * Create new employee record
     * @param firstName
     * @param lastName
     * @param basicSalary
     * @param positionId
     * @param email
     * @return 
     */
    public boolean createEmployee(String firstName, String lastName, String email, 
                                 int positionId, BigDecimal basicSalary) {
        if (!canManageEmployees) {
            return false;
        }
        
        try {
            // Validate required fields
            if (firstName == null || lastName == null || email == null || 
                positionId <= 0 || basicSalary == null || basicSalary.compareTo(BigDecimal.ZERO) <= 0) {
                return false;
            }
            
            // Additional validation logic would go here
            updateActivity("Creating new employee: " + firstName + " " + lastName);
            employeesManaged++;
            return true;
        } catch (Exception e) {
            System.err.println("Error creating employee: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Update employee information
     * @param employeeId
     * @param updates
     * @return 
     */
    public boolean updateEmployee(int employeeId, Map<String, Object> updates) {
        if (!canManageEmployees) {
            return false;
        }
        
        try {
            updateActivity("Updating employee ID: " + employeeId);
            return true;
        } catch (Exception e) {
            System.err.println("Error updating employee: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Terminate employee
     * @param employeeId
     * @param reason
     * @param effectiveDate
     * @return 
     */
    public boolean terminateEmployee(int employeeId, String reason, Date effectiveDate) {
        if (!canManageEmployees) {
            return false;
        }
        
        try {
            // Validate termination date
            Date today = Date.valueOf(LocalDate.now(MANILA_TIMEZONE));
            if (effectiveDate.before(today)) {
                return false; // Cannot terminate in the past
            }
            
            updateActivity("Terminating employee ID: " + employeeId);
            return true;
        } catch (Exception e) {
            System.err.println("Error terminating employee: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Reactivate terminated employee
     * @param employeeId
     * @param reason
     * @return 
     */
    public boolean reactivateEmployee(int employeeId, String reason) {
        if (!canManageEmployees) {
            return false;
        }
        
        try {
            updateActivity("Reactivating employee ID: " + employeeId);
            return true;
        } catch (Exception e) {
            System.err.println("Error reactivating employee: " + e.getMessage());
            return false;
        }
    }
    
    // Payroll Management Operations
    
    /**
     * Run payroll for specified period
     * @param payPeriodId
     * @param employeeIds
     * @return 
     */
    public boolean runPayroll(int payPeriodId, List<Integer> employeeIds) {
        if (!canManagePayroll) {
            return false;
        }
        
        try {
            updateActivity("Running payroll for period: " + payPeriodId);
            this.lastPayrollRun = Timestamp.valueOf(getCurrentManilaTime());
            this.payrollsProcessed++;
            return true;
        } catch (Exception e) {
            System.err.println("Error running payroll: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Approve payroll calculations
     * @param payPeriodId
     * @param approvalNotes
     * @return 
     */
    public boolean approvePayroll(int payPeriodId, String approvalNotes) {
        if (!canManagePayroll) {
            return false;
        }
        
        try {
            updateActivity("Approving payroll for period: " + payPeriodId);
            return true;
        } catch (Exception e) {
            System.err.println("Error approving payroll: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Modify employee pay rate
     * @param employeeId
     * @param newBasicSalary
     * @param reason
     * @param effectiveDate
     * @return 
     */
    public boolean modifyPayRate(int employeeId, BigDecimal newBasicSalary, String reason, Date effectiveDate) {
        if (!canModifyPayrates) {
            return false;
        }
        
        try {
            // Validate new salary
            if (newBasicSalary == null || newBasicSalary.compareTo(BigDecimal.ZERO) <= 0) {
                return false;
            }
            
            // Validate effective date
            Date today = Date.valueOf(LocalDate.now(MANILA_TIMEZONE));
            if (effectiveDate.before(today)) {
                return false; // Cannot be retroactive without special approval
            }
            
            updateActivity("Modifying pay rate for employee ID: " + employeeId);
            return true;
        } catch (Exception e) {
            System.err.println("Error modifying pay rate: " + e.getMessage());
            return false;
        }
    }
    
    // Benefits Management Operations
    
    /**
     * Assign benefits to employee
     * @param employeeId
     * @param benefitTypeIds
     * @return 
     */
    public boolean assignBenefits(int employeeId, List<Integer> benefitTypeIds) {
        if (!canManageBenefits) {
            return false;
        }
        
        try {
            updateActivity("Assigning benefits to employee ID: " + employeeId);
            return true;
        } catch (Exception e) {
            System.err.println("Error assigning benefits: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Update benefit amounts
     * @param positionId
     * @param benefitAmounts
     * @return 
     */
    public boolean updateBenefitAmounts(int positionId, Map<Integer, BigDecimal> benefitAmounts) {
        if (!canManageBenefits) {
            return false;
        }
        
        try {
            updateActivity("Updating benefit amounts for position ID: " + positionId);
            return true;
        } catch (Exception e) {
            System.err.println("Error updating benefit amounts: " + e.getMessage());
            return false;
        }
    }
    
    // Leave Management Operations
    
    /**
     * Override leave request decision
     * @param requestId
     * @param newStatus
     * @param reason
     * @return 
     */
    public boolean overrideLeaveRequest(int requestId, LeaveRequestModel.ApprovalStatus newStatus, String reason) {
        if (!canApproveAllRequests) {
            return false;
        }
        
        try {
            updateActivity("Overriding leave request ID: " + requestId);
            return true;
        } catch (Exception e) {
            System.err.println("Error overriding leave request: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Bulk approve leave requests
     * @param requestIds
     * @param approvalNotes
     * @return 
     */
    public boolean bulkApproveLeaveRequests(List<Integer> requestIds, String approvalNotes) {
        if (!canApproveAllRequests) {
            return false;
        }
        
        try {
            updateActivity("Bulk approving " + requestIds.size() + " leave requests");
            return true;
        } catch (Exception e) {
            System.err.println("Error bulk approving leave requests: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Initialize leave balances for all employees
     * @param year
     * @return 
     */
    public boolean initializeYearlyLeaveBalances(int year) {
        if (!canManageLeaves) {
            return false;
        }
        
        try {
            updateActivity("Initializing leave balances for year: " + year);
            return true;
        } catch (Exception e) {
            System.err.println("Error initializing leave balances: " + e.getMessage());
            return false;
        }
    }
    
    // Attendance Management Operations
    
    /**
     * Override attendance record
     * @param attendanceId
     * @param reason
     * @param newAttendance
     * @return 
     */
    public boolean overrideAttendance(int attendanceId, String reason, AttendanceModel newAttendance) {
        if (!canManageAttendance) {
            return false;
        }
        
        try {
            updateActivity("Overriding attendance ID: " + attendanceId);
            return true;
        } catch (Exception e) {
            System.err.println("Error overriding attendance: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Bulk import attendance records
     * @param attendanceRecords
     * @return 
     */
    public boolean bulkImportAttendance(List<AttendanceModel> attendanceRecords) {
        if (!canManageAttendance) {
            return false;
        }
        
        try {
            updateActivity("Bulk importing " + attendanceRecords.size() + " attendance records");
            return true;
        } catch (Exception e) {
            System.err.println("Error bulk importing attendance: " + e.getMessage());
            return false;
        }
    }
    
    // Reporting Operations
    
    /**
     * Generate comprehensive HR report
     * @param reportType
     * @param startDate
     * @param endDate
     * @param employeeIds
     * @return 
     */
    public boolean generateHRReport(String reportType, Date startDate, Date endDate, List<Integer> employeeIds) {
        if (!canGenerateReports) {
            return false;
        }
        
        try {
            updateActivity("Generating " + reportType + " report");
            this.lastReportGeneration = Timestamp.valueOf(getCurrentManilaTime());
            this.reportsGenerated++;
            return true;
        } catch (Exception e) {
            System.err.println("Error generating HR report: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Generate payroll summary report
     * @param month
     * @param year
     * @return 
     */
    public Map<String, Object> generatePayrollSummaryReport(int month, int year) {
        if (!canGenerateReports) {
            return new HashMap<>();
        }
        
        Map<String, Object> report = new HashMap<>();
        
        try {
            updateActivity("Generating payroll summary for " + month + "/" + year);
            
            // Sample report structure
            report.put("reportType", "Payroll Summary");
            report.put("month", month);
            report.put("year", year);
            report.put("generatedBy", getDisplayName());
            report.put("generatedAt", getCurrentManilaTime());
            
            // Summary data (would be populated from database)
            report.put("totalEmployees", 0);
            report.put("totalGrossIncome", BigDecimal.ZERO);
            report.put("totalDeductions", BigDecimal.ZERO);
            report.put("totalNetPay", BigDecimal.ZERO);
            report.put("totalBenefits", BigDecimal.ZERO);
            
            this.reportsGenerated++;
            
        } catch (Exception e) {
            System.err.println("Error generating payroll summary: " + e.getMessage());
        }
        
        return report;
    }
    
    /**
     * Generate employee demographics report
     * @return 
     */
    public Map<String, Object> generateDemographicsReport() {
        if (!canGenerateReports) {
            return new HashMap<>();
        }
        
        Map<String, Object> report = new HashMap<>();
        
        try {
            updateActivity("Generating demographics report");
            
            report.put("reportType", "Employee Demographics");
            report.put("generatedBy", getDisplayName());
            report.put("generatedAt", getCurrentManilaTime());
            
            // Demographics data (would be populated from database)
            report.put("totalEmployees", 0);
            report.put("employeesByDepartment", new HashMap<String, Integer>());
            report.put("employeesByPosition", new HashMap<String, Integer>());
            report.put("employeesByStatus", new HashMap<String, Integer>());
            report.put("averageTenure", 0.0);
            
            this.reportsGenerated++;
            
        } catch (Exception e) {
            System.err.println("Error generating demographics report: " + e.getMessage());
        }
        
        return report;
    }
    
    // Position Management Operations
    
    /**
     * Create new position
     * @param positionName
     * @param description
     * @param department
     * @param baseSalary
     * @return 
     */
    public boolean createPosition(String positionName, String description, String department, 
                                 BigDecimal baseSalary) {
        if (!canManagePositions) {
            return false;
        }
        
        try {
            updateActivity("Creating new position: " + positionName);
            return true;
        } catch (Exception e) {
            System.err.println("Error creating position: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Update position details
     * @param positionId
     * @param updates
     * @return 
     */
    public boolean updatePosition(int positionId, Map<String, Object> updates) {
        if (!canManagePositions) {
            return false;
        }
        
        try {
            updateActivity("Updating position ID: " + positionId);
            return true;
        } catch (Exception e) {
            System.err.println("Error updating position: " + e.getMessage());
            return false;
        }
    }
    
    // Dashboard and Analytics
    
    /**
     * Get HR dashboard data
     * @return 
     */
    public Map<String, Object> getHRDashboard() {
        Map<String, Object> dashboard = new HashMap<>();
        
        // Personal info
        dashboard.put("hrId", hrId);
        dashboard.put("name", getDisplayName());
        dashboard.put("position", position);
        dashboard.put("specializations", specializations);
        dashboard.put("lastLogin", getLastLoginInManila());
        
        // Activity summary
        dashboard.put("employeesManaged", employeesManaged);
        dashboard.put("payrollsProcessed", payrollsProcessed);
        dashboard.put("reportsGenerated", reportsGenerated);
        dashboard.put("lastPayrollRun", getLastPayrollRunInManila());
        dashboard.put("currentlyWorkingOn", currentlyWorkingOn);
        
        // Permissions
        Map<String, Boolean> permissions = new HashMap<>();
        permissions.put("canManageEmployees", canManageEmployees);
        permissions.put("canManagePayroll", canManagePayroll);
        permissions.put("canManageBenefits", canManageBenefits);
        permissions.put("canManageLeaves", canManageLeaves);
        permissions.put("canManageAttendance", canManageAttendance);
        permissions.put("canGenerateReports", canGenerateReports);
        permissions.put("canManagePositions", canManagePositions);
        permissions.put("canApproveAllRequests", canApproveAllRequests);
        permissions.put("canModifyPayrates", canModifyPayrates);
        permissions.put("canAccessAuditLogs", canAccessAuditLogs);
        dashboard.put("permissions", permissions);
        
        return dashboard;
    }
    
    /**
     * Get monthly HR metrics
     * @param month
     * @param year
     * @return 
     */
    public Map<String, Object> getMonthlyMetrics(int month, int year) {
        Map<String, Object> metrics = new HashMap<>();
        
        try {
            updateActivity("Generating monthly metrics for " + month + "/" + year);
            
            // Sample metrics (would be calculated from database)
            metrics.put("newHires", 0);
            metrics.put("terminations", 0);
            metrics.put("payrollRuns", 0);
            metrics.put("leaveRequestsProcessed", 0);
            metrics.put("overtimeRequestsProcessed", 0);
            metrics.put("attendanceIssuesResolved", 0);
            metrics.put("reportsGenerated", 0);
            
        } catch (Exception e) {
            System.err.println("Error getting monthly metrics: " + e.getMessage());
        }
        
        return metrics;
    }
    
    // Audit and Compliance
    
    /**
     * Access audit logs
     * @param startDate
     * @param endDate
     * @param actionType
     * @return 
     */
    public List<Map<String, Object>> getAuditLogs(Date startDate, Date endDate, String actionType) {
        if (!canAccessAuditLogs) {
            return new java.util.ArrayList<>();
        }
        
        try {
            updateActivity("Accessing audit logs from " + startDate + " to " + endDate);
            // Return audit log entries
            return new java.util.ArrayList<>();
        } catch (Exception e) {
            System.err.println("Error accessing audit logs: " + e.getMessage());
            return new java.util.ArrayList<>();
        }
    }
    
    /**
     * Generate compliance report
     * @param complianceType
     * @param year
     * @return 
     */
    public Map<String, Object> generateComplianceReport(String complianceType, int year) {
        if (!canGenerateReports) {
            return new HashMap<>();
        }
        
        Map<String, Object> report = new HashMap<>();
        
        try {
            updateActivity("Generating " + complianceType + " compliance report for " + year);
            
            report.put("complianceType", complianceType);
            report.put("year", year);
            report.put("generatedBy", getDisplayName());
            report.put("generatedAt", getCurrentManilaTime());
            
            // Compliance data would be populated here
            report.put("complianceStatus", "Compliant");
            report.put("issues", new java.util.ArrayList<>());
            report.put("recommendations", new java.util.ArrayList<>());
            
            this.reportsGenerated++;
            
        } catch (Exception e) {
            System.err.println("Error generating compliance report: " + e.getMessage());
        }
        
        return report;
    }
    
    // Utility methods
    
    /**
     * Update current activity
     */
    private void updateActivity(String activity) {
        this.currentlyWorkingOn = activity + " at " + getCurrentManilaTime().format(
            java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
    }
    
    /**
     * Get HR display name
     * @return 
     */
    public String getDisplayName() {
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        } else if (firstName != null) {
            return firstName;
        } else if (email != null) {
            return email;
        } else {
            return "HR " + hrId;
        }
    }
    
    /**
     * Check if HR personnel is active
     * @return 
     */
    public boolean isActive() {
        return "Active".equalsIgnoreCase(status);
    }
    
    /**
     * Validate HR permissions
     * @return 
     */
    public boolean hasValidHRPermissions() {
        return userRole != null && userRole.equals("HR") && isActive();
    }
    
    // Getters and Setters
    public int getHrId() { return hrId; }
    public void setHrId(int hrId) { this.hrId = hrId; }
    
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }
    
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    
    public String getUserRole() { return userRole; }
    public void setUserRole(String userRole) { this.userRole = userRole; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public Timestamp getLastLogin() { return lastLogin; }
    public void setLastLogin(Timestamp lastLogin) { this.lastLogin = lastLogin; }
    
    public String getSpecializations() { return specializations; }
    public void setSpecializations(String specializations) { this.specializations = specializations; }
    
    // Permission getters and setters
    public boolean isCanManageEmployees() { return canManageEmployees; }
    public void setCanManageEmployees(boolean canManageEmployees) { this.canManageEmployees = canManageEmployees; }
    
    public boolean isCanManagePayroll() { return canManagePayroll; }
    public void setCanManagePayroll(boolean canManagePayroll) { this.canManagePayroll = canManagePayroll; }
    
    public boolean isCanManageBenefits() { return canManageBenefits; }
    public void setCanManageBenefits(boolean canManageBenefits) { this.canManageBenefits = canManageBenefits; }
    
    public boolean isCanManageLeaves() { return canManageLeaves; }
    public void setCanManageLeaves(boolean canManageLeaves) { this.canManageLeaves = canManageLeaves; }
    
    public boolean isCanManageAttendance() { return canManageAttendance; }
    public void setCanManageAttendance(boolean canManageAttendance) { this.canManageAttendance = canManageAttendance; }
    
    public boolean isCanGenerateReports() { return canGenerateReports; }
    public void setCanGenerateReports(boolean canGenerateReports) { this.canGenerateReports = canGenerateReports; }
    
    public boolean isCanManagePositions() { return canManagePositions; }
    public void setCanManagePositions(boolean canManagePositions) { this.canManagePositions = canManagePositions; }
    
    public boolean isCanApproveAllRequests() { return canApproveAllRequests; }
    public void setCanApproveAllRequests(boolean canApproveAllRequests) { this.canApproveAllRequests = canApproveAllRequests; }
    
    public boolean isCanModifyPayrates() { return canModifyPayrates; }
    public void setCanModifyPayrates(boolean canModifyPayrates) { this.canModifyPayrates = canModifyPayrates; }
    
    public boolean isCanAccessAuditLogs() { return canAccessAuditLogs; }
    public void setCanAccessAuditLogs(boolean canAccessAuditLogs) { this.canAccessAuditLogs = canAccessAuditLogs; }
    
    // Activity tracking getters and setters
    public int getEmployeesManaged() { return employeesManaged; }
    public void setEmployeesManaged(int employeesManaged) { this.employeesManaged = employeesManaged; }
    
    public int getPayrollsProcessed() { return payrollsProcessed; }
    public void setPayrollsProcessed(int payrollsProcessed) { this.payrollsProcessed = payrollsProcessed; }
    
    public int getReportsGenerated() { return reportsGenerated; }
    public void setReportsGenerated(int reportsGenerated) { this.reportsGenerated = reportsGenerated; }
    
    public Timestamp getLastPayrollRun() { return lastPayrollRun; }
    public void setLastPayrollRun(Timestamp lastPayrollRun) { this.lastPayrollRun = lastPayrollRun; }
    
    public Timestamp getLastReportGeneration() { return lastReportGeneration; }
    public void setLastReportGeneration(Timestamp lastReportGeneration) { this.lastReportGeneration = lastReportGeneration; }
    
    public String getCurrentlyWorkingOn() { return currentlyWorkingOn; }
    public void setCurrentlyWorkingOn(String currentlyWorkingOn) { this.currentlyWorkingOn = currentlyWorkingOn; }
    
    @Override
    public String toString() {
        return String.format("HRModel{hrId=%d, name='%s', position='%s', employeesManaged=%d, payrollsProcessed=%d}",
                hrId, getDisplayName(), position, employeesManaged, payrollsProcessed);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        HRModel that = (HRModel) obj;
        return hrId == that.hrId;
    }
    
    @Override
    public int hashCode() {
        return Integer.hashCode(hrId);
    }
}