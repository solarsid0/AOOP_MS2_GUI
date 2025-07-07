package Models;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface OvertimeManagement {
    
    // BUSINESS RULE CONSTANTS
    double MAX_DAILY_OVERTIME_HOURS = 4.0;
    double MAX_WEEKLY_OVERTIME_HOURS = 20.0;
    double MAX_MONTHLY_OVERTIME_HOURS = 60.0;
    double OVERTIME_MULTIPLIER = 1.25; // 125% of regular rate
    
    // Work schedule constants
    String WORK_START_TIME = "08:00:00";
    String WORK_END_TIME = "17:00:00";
    String LATE_THRESHOLD_TIME = "08:10:00";
    double STANDARD_WORK_HOURS = 8.0;
    double LUNCH_BREAK_HOURS = 1.0;
    
    // Auto-approval thresholds
    double AUTO_APPROVE_HOURS_THRESHOLD = 2.0; // Auto-approve OT <= 2 hours
    double HIGHER_APPROVAL_HOURS_THRESHOLD = 3.0; // Requires higher approval > 3 hours
    
    // CORE OVERTIME OPERATIONS
    
    /**
     * Submit overtime request for approval
     * Only rank-and-file employees can submit overtime requests
     * Request date must be today or future (Manila timezone)
     * @param request The overtime request to submit
     * @return true if successfully submitted
     */
    boolean submitOvertimeRequest(OvertimeRequestModel request);
    
    /**
     * Approve overtime request with workflow validation
     * @param requestId The overtime request ID
     * @param supervisorNotes Approval notes
     * @param approverId ID of the approving supervisor
     * @return true if successfully approved
     */
    boolean approveOvertimeRequest(int requestId, String supervisorNotes, int approverId);
    
    /**
     * Reject overtime request with mandatory reason
     * @param requestId The overtime request ID
     * @param supervisorNotes Required rejection reason
     * @param approverId ID of the rejecting supervisor
     * @return true if successfully rejected
     */
    boolean rejectOvertimeRequest(int requestId, String supervisorNotes, int approverId);
    
    /**
     * Cancel overtime request (only if pending or not yet started)
     * @param requestId The overtime request ID
     * @param employeeId ID of the employee cancelling
     * @return true if successfully cancelled
     */
    boolean cancelOvertimeRequest(int requestId, int employeeId);
    
    // ELIGIBILITY AND VALIDATION METHODS
    
    /**
     * Check if employee is rank-and-file and eligible for overtime
     * Only rank-and-file employees can receive overtime pay
     * @param employeeId Employee ID
     * @return true if employee is rank-and-file and can work overtime
     */
    boolean isRankAndFileEmployee(int employeeId);
    
    /**
     * Validate if employee can request overtime for given period
     * Checks eligibility, timing, conflicts, and business rules
     * @param employeeId Employee ID
     * @param overtimeStart Start time of overtime (must be today or future)
     * @param overtimeEnd End time of overtime
     * @return true if overtime can be requested
     */
    boolean canRequestOvertime(int employeeId, LocalDateTime overtimeStart, LocalDateTime overtimeEnd);
    
    /**
     * Validate overtime timing against work schedule
     * Overtime must start at or after 5:00 PM or after 8 hours of work
     * @param overtimeStart Overtime start time
     * @param overtimeEnd Overtime end time
     * @return true if timing is valid
     */
    boolean isValidOvertimeTiming(LocalDateTime overtimeStart, LocalDateTime overtimeEnd);
    
    /**
     * Check if overtime date is today or future (Manila timezone)
     * @param overtimeDate Date to check
     * @return true if date is valid for overtime request
     */
    boolean isValidOvertimeDate(LocalDate overtimeDate);
    
    /**
     * Check if overtime request exceeds daily limits
     * @param employeeId Employee ID
     * @param date Date to check
     * @param additionalHours Additional overtime hours to check
     * @return true if within daily limits
     */
    boolean isWithinDailyLimit(int employeeId, LocalDate date, double additionalHours);
    
    /**
     * Check if overtime request exceeds weekly limits
     * @param employeeId Employee ID
     * @param weekStartDate Start date of the week
     * @param additionalHours Additional overtime hours to check
     * @return true if within weekly limits
     */
    boolean isWithinWeeklyLimit(int employeeId, LocalDate weekStartDate, double additionalHours);
    
    /**
     * Check if overtime request exceeds monthly limits
     * @param employeeId Employee ID
     * @param month Month to check (1-12)
     * @param year Year to check
     * @param additionalHours Additional overtime hours to check
     * @return true if within monthly limits
     */
    boolean isWithinMonthlyLimit(int employeeId, int month, int year, double additionalHours);
    
    /**
     * Check for conflicts with existing attendance, leave, or overtime
     * @param employeeId Employee ID
     * @param overtimeDate Date of overtime
     * @return true if no conflicts found
     */
    boolean hasNoConflicts(int employeeId, LocalDate overtimeDate);
    
    /**
     * Check if overtime conflicts with approved leave
     * @param employeeId Employee ID
     * @param overtimeDate Date to check
     * @return true if conflicts with leave
     */
    boolean conflictsWithLeave(int employeeId, LocalDate overtimeDate);
    
    /**
     * Check if overtime is on a workday (Monday-Friday)
     * @param overtimeDate Date to check
     * @return true if date is a workday
     */
    boolean isWorkday(LocalDate overtimeDate);
    
    // ATTENDANCE-BASED OVERTIME METHODS
    
    /**
     * Get attendance records with potential overtime for employee
     * Shows current and future attendance where employee can request OT
     * @param employeeId Employee ID
     * @return List of attendance records with overtime calculation
     */
    List<AttendanceOvertimeRecord> getAttendanceWithOvertimeOpportunities(int employeeId);
    
    /**
     * Calculate actual overtime hours from attendance record
     * Based on time in/out and standard 8-hour work day
     * @param timeIn Time in (HH:mm:ss format)
     * @param timeOut Time out (HH:mm:ss format)
     * @return Calculated overtime hours
     */
    double calculateOvertimeFromAttendance(String timeIn, String timeOut);
    
    /**
     * Validate if overtime request matches attendance record
     * @param employeeId Employee ID
     * @param attendanceDate Attendance date
     * @param requestedOvertimeHours Requested overtime hours
     * @return true if request matches actual overtime worked
     */
    boolean validateOvertimeAgainstAttendance(int employeeId, LocalDate attendanceDate, double requestedOvertimeHours);
    
    // QUERY METHODS
    
    /**
     * Get overtime requests for employee with status filter
     * @param employeeId Employee ID
     * @param status Filter by approval status (null for all)
     * @return List of overtime requests
     */
    List<OvertimeRequestModel> getOvertimeRequestsByEmployee(int employeeId, OvertimeRequestModel.ApprovalStatus status);
    
    /**
     * Get pending overtime requests for supervisor
     * @param supervisorId Supervisor ID
     * @return List of pending overtime requests
     */
    List<OvertimeRequestModel> getPendingOvertimeRequests(int supervisorId);
    
    /**
     * Get overtime requests for date range
     * @param employeeId Employee ID (null for all employees)
     * @param startDate Start date
     * @param endDate End date
     * @return List of overtime requests in date range
     */
    List<OvertimeRequestModel> getOvertimeRequestsByDateRange(Integer employeeId, LocalDate startDate, LocalDate endDate);
    
    /**
     * Get approved overtime hours for payroll period
     * Only includes approved overtime for rank-and-file employees
     * @param employeeId Employee ID
     * @param periodStart Period start date
     * @param periodEnd Period end date
     * @return Total approved overtime hours
     */
    double getApprovedOvertimeHours(int employeeId, LocalDate periodStart, LocalDate periodEnd);
    
    /**
     * Get overtime requests requiring higher approval
     * Overtime > 3 hours requires higher level approval
     * @param supervisorId Supervisor ID
     * @return List of overtime requests needing higher approval
     */
    List<OvertimeRequestModel> getOvertimeRequestsRequiringHigherApproval(int supervisorId);
    
    // CALCULATION METHODS
    
    /**
     * Calculate overtime pay for rank-and-file employees
     * Formula: overtime hours × hourly rate × 1.25
     * @param overtimeHours Number of overtime hours
     * @param hourlyRate Employee's hourly rate
     * @return Calculated overtime pay (0 for non rank-and-file)
     */
    double calculateOvertimePay(double overtimeHours, double hourlyRate);
    
    /**
     * Calculate total overtime hours for period
     * @param employeeId Employee ID
     * @param periodStart Period start date
     * @param periodEnd Period end date
     * @return Total overtime hours in period
     */
    double calculateTotalOvertimeHours(int employeeId, LocalDate periodStart, LocalDate periodEnd);
    
    /**
     * Get daily overtime summary for employee
     * @param employeeId Employee ID
     * @param date Specific date
     * @return Daily overtime hours
     */
    double getDailyOvertimeHours(int employeeId, LocalDate date);
    
    /**
     * Get weekly overtime summary for employee
     * @param employeeId Employee ID
     * @param weekStartDate Start of week (Monday)
     * @return Weekly overtime hours
     */
    double getWeeklyOvertimeHours(int employeeId, LocalDate weekStartDate);
    
    /**
     * Get monthly overtime summary for employee
     * @param employeeId Employee ID
     * @param month Month (1-12)
     * @param year Year
     * @return Monthly overtime hours
     */
    double getMonthlyOvertimeHours(int employeeId, int month, int year);
    
    /**
     * Calculate overtime pay for payroll period
     * @param employeeId Employee ID
     * @param periodStart Period start date
     * @param periodEnd Period end date
     * @param hourlyRate Employee's hourly rate
     * @return Total overtime pay for period
     */
    double calculateOvertimePayForPeriod(int employeeId, LocalDate periodStart, LocalDate periodEnd, double hourlyRate);
    
    // BUSINESS RULE ENFORCEMENT
    
    /**
     * Validate overtime request against all business rules
     * Comprehensive validation including timing, limits, eligibility
     * @param request Overtime request to validate
     * @return ValidationResult containing validation status and messages
     */
    OvertimeValidationResult validateOvertimeRequest(OvertimeRequestModel request);
    
    /**
     * Check if overtime requires higher approval level
     * Overtime > 3 hours requires higher approval
     * @param request Overtime request
     * @return true if requires higher approval
     */
    boolean requiresHigherApproval(OvertimeRequestModel request);
    
    /**
     * Get maximum overtime hours allowed for employee
     * @param employeeId Employee ID
     * @param period Period type (DAILY, WEEKLY, MONTHLY)
     * @return Maximum allowed overtime hours
     */
    double getMaxOvertimeHours(int employeeId, OvertimePeriod period);
    
    /**
     * Auto-approve overtime if within auto-approval limits
     * Auto-approve if <= 2 hours and meets all other criteria
     * @param request Overtime request
     * @return true if auto-approved
     */
    boolean autoApproveIfEligible(OvertimeRequestModel request);
    
    /**
     * Validate overtime against Manila timezone requirements
     * Ensures request is for today or future Manila time
     * @param overtimeStart Overtime start datetime
     * @return true if timing is valid
     */
    boolean isValidManilaTimezone(LocalDateTime overtimeStart);
    
    // MANILA TIMEZONE SUPPORT
    
    /**
     * Get current Manila time
     * @return Current LocalDateTime in Manila timezone
     */
    LocalDateTime getCurrentManilaTime();
    
    /**
     * Get current Manila date
     * @return Current LocalDate in Manila timezone
     */
    LocalDate getCurrentManilaDate();
    
    /**
     * Check if date is today or future in Manila timezone
     * @param date Date to check
     * @return true if date is today or future
     */
    boolean isTodayOrFutureInManila(LocalDate date);
    
    // REPORTING METHODS
    
    /**
     * Generate overtime report for employee
     * @param employeeId Employee ID
     * @param startDate Report start date
     * @param endDate Report end date
     * @return Overtime report data
     */
    OvertimeReport generateOvertimeReport(int employeeId, LocalDate startDate, LocalDate endDate);
    
    /**
     * Get overtime statistics for department
     * @param departmentName Department name
     * @param month Month
     * @param year Year
     * @return Department overtime statistics
     */
    OvertimeStatistics getDepartmentOvertimeStatistics(String departmentName, int month, int year);
    
    /**
     * Generate overtime summary for payroll processing
     * @param employeeId Employee ID
     * @param payrollMonth Payroll month (YYYY-MM format)
     * @return Overtime summary for payroll
     */
    OvertimePayrollSummary getOvertimeSummaryForPayroll(int employeeId, String payrollMonth);
    
    // ENUMS AND SUPPORTING CLASSES
    
    enum OvertimePeriod {
        DAILY, WEEKLY, MONTHLY, YEARLY
    }
    
    enum OvertimeType {
        REGULAR,        // Regular overtime after work hours
        WEEKEND,        // Weekend work (if applicable)
        HOLIDAY,        // Holiday work (if applicable)
        EMERGENCY       // Emergency overtime
    }
    
    /**
     * Attendance record with overtime calculation
     */
    class AttendanceOvertimeRecord {
        private int attendanceId;
        private int employeeId;
        private LocalDate date;
        private String timeIn;
        private String timeOut;
        private double hoursWorked;
        private double overtimeHours;
        private boolean canRequestOvertime;
        private String reason; // Why overtime can/cannot be requested
        
        // Constructors
        public AttendanceOvertimeRecord(int attendanceId, int employeeId, LocalDate date) {
            this.attendanceId = attendanceId;
            this.employeeId = employeeId;
            this.date = date;
        }
        
        // Getters and setters
        public int getAttendanceId() { return attendanceId; }
        public void setAttendanceId(int attendanceId) { this.attendanceId = attendanceId; }
        
        public int getEmployeeId() { return employeeId; }
        public void setEmployeeId(int employeeId) { this.employeeId = employeeId; }
        
        public LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }
        
        public String getTimeIn() { return timeIn; }
        public void setTimeIn(String timeIn) { this.timeIn = timeIn; }
        
        public String getTimeOut() { return timeOut; }
        public void setTimeOut(String timeOut) { this.timeOut = timeOut; }
        
        public double getHoursWorked() { return hoursWorked; }
        public void setHoursWorked(double hoursWorked) { this.hoursWorked = hoursWorked; }
        
        public double getOvertimeHours() { return overtimeHours; }
        public void setOvertimeHours(double overtimeHours) { this.overtimeHours = overtimeHours; }
        
        public boolean isCanRequestOvertime() { return canRequestOvertime; }
        public void setCanRequestOvertime(boolean canRequestOvertime) { this.canRequestOvertime = canRequestOvertime; }
        
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
    
    /**
     * Validation result for overtime requests
     */
    class OvertimeValidationResult {
        private boolean isValid;
        private final List<String> validationMessages;
        private final List<String> warnings;
        private boolean requiresHigherApproval;
        private boolean canAutoApprove;
        
        public OvertimeValidationResult(boolean isValid) {
            this.isValid = isValid;
            this.validationMessages = new java.util.ArrayList<>();
            this.warnings = new java.util.ArrayList<>();
        }
        
        public boolean isValid() { return isValid; }
        public void setValid(boolean valid) { isValid = valid; }
        
        public List<String> getValidationMessages() { return validationMessages; }
        public void addValidationMessage(String message) { validationMessages.add(message); }
        
        public List<String> getWarnings() { return warnings; }
        public void addWarning(String warning) { warnings.add(warning); }
        
        public boolean hasWarnings() { return !warnings.isEmpty(); }
        public boolean hasErrors() { return !validationMessages.isEmpty(); }
        
        public boolean isRequiresHigherApproval() { return requiresHigherApproval; }
        public void setRequiresHigherApproval(boolean requiresHigherApproval) { this.requiresHigherApproval = requiresHigherApproval; }
        
        public boolean isCanAutoApprove() { return canAutoApprove; }
        public void setCanAutoApprove(boolean canAutoApprove) { this.canAutoApprove = canAutoApprove; }
    }
    
    /**
     * Overtime payroll summary
     */
    class OvertimePayrollSummary {
        private int employeeId;
        private String payrollMonth;
        private double totalOvertimeHours;
        private double totalOvertimePay;
        private double hourlyRate;
        private int approvedRequestsCount;
        private boolean isRankAndFile;
        
        public OvertimePayrollSummary(int employeeId, String payrollMonth) {
            this.employeeId = employeeId;
            this.payrollMonth = payrollMonth;
        }
        
        // Getters and setters
        public int getEmployeeId() { return employeeId; }
        public void setEmployeeId(int employeeId) { this.employeeId = employeeId; }
        
        public String getPayrollMonth() { return payrollMonth; }
        public void setPayrollMonth(String payrollMonth) { this.payrollMonth = payrollMonth; }
        
        public double getTotalOvertimeHours() { return totalOvertimeHours; }
        public void setTotalOvertimeHours(double totalOvertimeHours) { this.totalOvertimeHours = totalOvertimeHours; }
        
        public double getTotalOvertimePay() { return totalOvertimePay; }
        public void setTotalOvertimePay(double totalOvertimePay) { this.totalOvertimePay = totalOvertimePay; }
        
        public double getHourlyRate() { return hourlyRate; }
        public void setHourlyRate(double hourlyRate) { this.hourlyRate = hourlyRate; }
        
        public int getApprovedRequestsCount() { return approvedRequestsCount; }
        public void setApprovedRequestsCount(int approvedRequestsCount) { this.approvedRequestsCount = approvedRequestsCount; }
        
        public boolean isRankAndFile() { return isRankAndFile; }
        public void setRankAndFile(boolean rankAndFile) { isRankAndFile = rankAndFile; }
    }
    
    /**
     * Overtime report data structure
     */
    class OvertimeReport {
        private int employeeId;
        private String employeeName;
        private LocalDate reportStartDate;
        private LocalDate reportEndDate;
        private double totalOvertimeHours;
        private double totalOvertimePay;
        private List<OvertimeRequestModel> overtimeRequests;
        private int approvedRequestsCount;
        private int rejectedRequestsCount;
        private int pendingRequestsCount;
        private boolean isRankAndFile;
        private double hourlyRate;
        
        public OvertimeReport(int employeeId, LocalDate startDate, LocalDate endDate) {
            this.employeeId = employeeId;
            this.reportStartDate = startDate;
            this.reportEndDate = endDate;
            this.overtimeRequests = new java.util.ArrayList<>();
        }
        
        // Getters and setters
        public int getEmployeeId() { return employeeId; }
        public void setEmployeeId(int employeeId) { this.employeeId = employeeId; }
        
        public String getEmployeeName() { return employeeName; }
        public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
        
        public LocalDate getReportStartDate() { return reportStartDate; }
        public void setReportStartDate(LocalDate reportStartDate) { this.reportStartDate = reportStartDate; }
        
        public LocalDate getReportEndDate() { return reportEndDate; }
        public void setReportEndDate(LocalDate reportEndDate) { this.reportEndDate = reportEndDate; }
        
        public double getTotalOvertimeHours() { return totalOvertimeHours; }
        public void setTotalOvertimeHours(double totalOvertimeHours) { this.totalOvertimeHours = totalOvertimeHours; }
        
        public double getTotalOvertimePay() { return totalOvertimePay; }
        public void setTotalOvertimePay(double totalOvertimePay) { this.totalOvertimePay = totalOvertimePay; }
        
        public List<OvertimeRequestModel> getOvertimeRequests() { return overtimeRequests; }
        public void setOvertimeRequests(List<OvertimeRequestModel> overtimeRequests) { this.overtimeRequests = overtimeRequests; }
        
        public int getApprovedRequestsCount() { return approvedRequestsCount; }
        public void setApprovedRequestsCount(int approvedRequestsCount) { this.approvedRequestsCount = approvedRequestsCount; }
        
        public int getRejectedRequestsCount() { return rejectedRequestsCount; }
        public void setRejectedRequestsCount(int rejectedRequestsCount) { this.rejectedRequestsCount = rejectedRequestsCount; }
        
        public int getPendingRequestsCount() { return pendingRequestsCount; }
        public void setPendingRequestsCount(int pendingRequestsCount) { this.pendingRequestsCount = pendingRequestsCount; }
        
        public boolean isRankAndFile() { return isRankAndFile; }
        public void setRankAndFile(boolean rankAndFile) { isRankAndFile = rankAndFile; }
        
        public double getHourlyRate() { return hourlyRate; }
        public void setHourlyRate(double hourlyRate) { this.hourlyRate = hourlyRate; }
    }
    
    /**
     * Overtime statistics data structure
     */
    class OvertimeStatistics {
        private String departmentName;
        private int month;
        private int year;
        private double totalOvertimeHours;
        private double totalOvertimePay;
        private double averageOvertimeHours;
        private int employeesWithOvertime;
        private int totalRankAndFileEmployees;
        private double overtimeUtilizationRate;
        private int totalOvertimeRequests;
        private int approvedRequests;
        private int rejectedRequests;
        private int pendingRequests;
        
        public OvertimeStatistics(String departmentName, int month, int year) {
            this.departmentName = departmentName;
            this.month = month;
            this.year = year;
        }
        
        // Getters and setters
        public String getDepartmentName() { return departmentName; }
        public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }
        
        public int getMonth() { return month; }
        public void setMonth(int month) { this.month = month; }
        
        public int getYear() { return year; }
        public void setYear(int year) { this.year = year; }
        
        public double getTotalOvertimeHours() { return totalOvertimeHours; }
        public void setTotalOvertimeHours(double totalOvertimeHours) { this.totalOvertimeHours = totalOvertimeHours; }
        
        public double getTotalOvertimePay() { return totalOvertimePay; }
        public void setTotalOvertimePay(double totalOvertimePay) { this.totalOvertimePay = totalOvertimePay; }
        
        public double getAverageOvertimeHours() { return averageOvertimeHours; }
        public void setAverageOvertimeHours(double averageOvertimeHours) { this.averageOvertimeHours = averageOvertimeHours; }
        
        public int getEmployeesWithOvertime() { return employeesWithOvertime; }
        public void setEmployeesWithOvertime(int employeesWithOvertime) { this.employeesWithOvertime = employeesWithOvertime; }
        
        public int getTotalRankAndFileEmployees() { return totalRankAndFileEmployees; }
        public void setTotalRankAndFileEmployees(int totalRankAndFileEmployees) { this.totalRankAndFileEmployees = totalRankAndFileEmployees; }
        
        public double getOvertimeUtilizationRate() { return overtimeUtilizationRate; }
        public void setOvertimeUtilizationRate(double overtimeUtilizationRate) { this.overtimeUtilizationRate = overtimeUtilizationRate; }
        
        public int getTotalOvertimeRequests() { return totalOvertimeRequests; }
        public void setTotalOvertimeRequests(int totalOvertimeRequests) { this.totalOvertimeRequests = totalOvertimeRequests; }
        
        public int getApprovedRequests() { return approvedRequests; }
        public void setApprovedRequests(int approvedRequests) { this.approvedRequests = approvedRequests; }
        
        public int getRejectedRequests() { return rejectedRequests; }
        public void setRejectedRequests(int rejectedRequests) { this.rejectedRequests = rejectedRequests; }
        
        public int getPendingRequests() { return pendingRequests; }
        public void setPendingRequests(int pendingRequests) { this.pendingRequests = pendingRequests; }
    }
}