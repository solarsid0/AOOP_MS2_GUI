package Services;

import Models.LeaveRequestModel;
import Models.LeaveBalance;
import Models.AttendanceModel;
import DAOs.LeaveRequestDAO;
import DAOs.LeaveBalanceDAO;
import DAOs.AttendanceDAO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import DAOs.DatabaseConnection;

import java.sql.Date;
import java.time.*;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

public class LeaveService {
    private static final ZoneId MANILA_TIMEZONE = ZoneId.of("Asia/Manila");
    
    private LeaveRequestDAO leaveRequestDAO;
    private LeaveBalanceDAO leaveBalanceDAO;
    private AttendanceDAO attendanceDAO;
    
    // Constructors - Fixed to avoid EmployeeDAO dependency issues
    public LeaveService() {
        this.leaveRequestDAO = new LeaveRequestDAO();
        this.leaveBalanceDAO = new LeaveBalanceDAO();
        this.attendanceDAO = new AttendanceDAO();
    }
   
   
public LeaveService(DatabaseConnection databaseConnection) {
    this.leaveRequestDAO = new LeaveRequestDAO();
    this.leaveBalanceDAO = new LeaveBalanceDAO();
    this.attendanceDAO = new AttendanceDAO();
}
    
    public LeaveService(LeaveRequestDAO leaveRequestDAO, LeaveBalanceDAO leaveBalanceDAO, 
                       AttendanceDAO attendanceDAO) {
        this.leaveRequestDAO = leaveRequestDAO;
        this.leaveBalanceDAO = leaveBalanceDAO;
        this.attendanceDAO = attendanceDAO;
    }
    
    // Core leave operations with enhanced conflict resolution
    
    /**
     * Submit leave request with comprehensive conflict checking
     * @param request Leave request to submit
     * @return true if successful
     */
    public boolean submitLeaveRequest(LeaveRequestModel request) {
        try {
            // Validate request
            if (request == null || !request.isValidLeaveRequest()) {
                System.err.println("Invalid leave request provided");
                return false;
            }
            
            // Check leave balance
            if (!hasSufficientLeaveBalance(request.getEmployeeId(), request.getLeaveTypeId(), request.getWorkingDaysCount())) {
                System.err.println("Insufficient leave balance for request");
                return false;
            }
            
            // Check for conflicts with existing attendance
            Map<String, Object> conflictAnalysis = analyzeAttendanceConflicts(request);
            if ((Boolean) conflictAnalysis.get("hasConflicts")) {
                request.setHasAttendanceConflict(true);
                System.out.println("Leave request has attendance conflicts but will be allowed with conflict flag");
                // Still allow submission but mark the conflict for HR review
            }
            
            // Check for overlapping leave requests
            if (hasOverlappingLeaveRequests(request)) {
                System.err.println("Overlapping leave requests found");
                return false;
            }
            
            // Submit the request
            boolean success = leaveRequestDAO.createLeaveRequest(request);
            
            if (success) {
                System.out.println("Leave request submitted successfully for employee " + request.getEmployeeId());
                
                if (request.isApproved()) {
                    // If auto-approved, update leave balance and resolve conflicts
                    updateLeaveBalanceForApproval(request);
                    resolveAttendanceConflictsForApprovedLeave(request);
                }
            }
            
            return success;
        } catch (Exception e) {
            System.err.println("Error submitting leave request: " + e.getMessage());
            return false;
        }
    }
    /**
 * Get employee leave summary for a specific year
 * @param employeeId Employee ID
 * @param year Year for the summary
 * @return Leave summary for the employee
 */
public LeaveSummary getEmployeeLeaveSummary(Integer employeeId, Integer year) {
    LeaveSummary summary = new LeaveSummary();
    
    try {
        // Get all leave balances for the employee for the year
        List<LeaveBalance> balances = leaveBalanceDAO.getLeaveBalancesByEmployee(employeeId, Year.of(year));
        
        int totalAllocatedDays = 0;
        int totalUsedDays = 0;
        int totalRemainingDays = 0;
        
        for (LeaveBalance balance : balances) {
            totalAllocatedDays += balance.getTotalLeaveDays();
            totalUsedDays += balance.getUsedLeaveDays();
            totalRemainingDays += balance.getRemainingLeaveDays();
        }
        
        summary.setEmployeeId(employeeId);
        summary.setYear(year);
        summary.setTotalAllocatedDays(totalAllocatedDays);
        summary.setTotalUsedDays(totalUsedDays);
        summary.setTotalRemainingDays(totalRemainingDays);
        
        // Calculate usage percentage
        if (totalAllocatedDays > 0) {
            BigDecimal usagePercentage = new BigDecimal(totalUsedDays)
                .divide(new BigDecimal(totalAllocatedDays), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal(100));
            summary.setUsagePercentage(usagePercentage);
        }
        
    } catch (Exception e) {
        System.err.println("Error getting employee leave summary: " + e.getMessage());
    }
    
    return summary;
}
/**
 * Employee leave summary for reporting
 */
public static class LeaveSummary {
    private Integer employeeId;
    private Integer year;
    private int totalAllocatedDays = 0;
    private int totalUsedDays = 0;
    private int totalRemainingDays = 0;
    private BigDecimal usagePercentage = BigDecimal.ZERO;
    
    // Getters and setters
    public Integer getEmployeeId() { return employeeId; }
    public void setEmployeeId(Integer employeeId) { this.employeeId = employeeId; }
    
    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }
    
    public int getTotalAllocatedDays() { return totalAllocatedDays; }
    public void setTotalAllocatedDays(int totalAllocatedDays) { this.totalAllocatedDays = totalAllocatedDays; }
    
    public int getTotalUsedDays() { return totalUsedDays; }
    public void setTotalUsedDays(int totalUsedDays) { this.totalUsedDays = totalUsedDays; }
    
    public int getTotalRemainingDays() { return totalRemainingDays; }
    public void setTotalRemainingDays(int totalRemainingDays) { this.totalRemainingDays = totalRemainingDays; }
    
    public BigDecimal getUsagePercentage() { return usagePercentage; }
    public void setUsagePercentage(BigDecimal usagePercentage) { 
        this.usagePercentage = usagePercentage != null ? usagePercentage : BigDecimal.ZERO; 
    }
}
    /**
     * Approve leave request with enhanced conflict resolution
     * @param requestId Leave request ID
     * @param supervisorNotes Approval notes
     * @param approverId Approver ID
     * @return true if successful
     */
    public boolean approveLeaveRequest(int requestId, String supervisorNotes, int approverId) {
        try {
            LeaveRequestModel request = leaveRequestDAO.getLeaveRequestById(requestId);
            if (request == null || !request.isPending()) {
                System.err.println("Leave request not found or not pending approval");
                return false;
            }
            
            // Re-check leave balance before approval
            if (!hasSufficientLeaveBalance(request.getEmployeeId(), request.getLeaveTypeId(), request.getWorkingDaysCount())) {
                System.err.println("Insufficient leave balance at approval time");
                return false;
            }
            
            // Analyze and resolve attendance conflicts during approval
            Map<String, Object> conflictAnalysis = analyzeAttendanceConflicts(request);
            if ((Boolean) conflictAnalysis.get("hasConflicts")) {
                System.out.println("Resolving attendance conflicts during leave approval...");
                resolveAttendanceConflictsForApprovedLeave(request);
            }
            
            // Approve the request
            request.approve(supervisorNotes);
            boolean success = leaveRequestDAO.updateLeaveRequest(request);
            
            if (success) {
                // Update leave balance
                updateLeaveBalanceForApproval(request);
                System.out.println("Leave request approved and balance updated for employee " + request.getEmployeeId());
            }
            
            return success;
        } catch (Exception e) {
            System.err.println("Error approving leave request: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Reject leave request
     * @param requestId Leave request ID
     * @param supervisorNotes Rejection reason
     * @param approverId Rejector ID
     * @return true if successful
     */
    public boolean rejectLeaveRequest(int requestId, String supervisorNotes, int approverId) {
        try {
            LeaveRequestModel request = leaveRequestDAO.getLeaveRequestById(requestId);
            if (request == null || !request.isPending()) {
                System.err.println("Leave request not found or not pending");
                return false;
            }
            
            request.reject(supervisorNotes);
            boolean success = leaveRequestDAO.updateLeaveRequest(request);
            
            if (success) {
                System.out.println("Leave request rejected for employee " + request.getEmployeeId() + 
                                 " - Reason: " + supervisorNotes);
            }
            
            return success;
        } catch (Exception e) {
            System.err.println("Error rejecting leave request: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Cancel leave request with balance restoration
     * @param requestId Leave request ID
     * @param employeeId Employee ID for security check
     * @return true if successful
     */
    public boolean cancelLeaveRequest(int requestId, int employeeId) {
        try {
            LeaveRequestModel request = leaveRequestDAO.getLeaveRequestById(requestId);
            if (request == null || request.getEmployeeId() != employeeId || !request.canBeCancelled()) {
                System.err.println("Cannot cancel leave request - invalid request or permissions");
                return false;
            }
            
            // If already approved, restore leave balance
            if (request.isApproved()) {
                restoreLeaveBalance(request);
                System.out.println("Leave balance restored for cancelled request");
            }
            
            // Delete the request
            boolean success = leaveRequestDAO.deleteLeaveRequest(requestId);
            
            if (success) {
                System.out.println("Leave request cancelled successfully for employee " + employeeId);
            }
            
            return success;
        } catch (Exception e) {
            System.err.println("Error cancelling leave request: " + e.getMessage());
            return false;
        }
    }
    
    // Enhanced conflict resolution methods
    
    /**
     * Comprehensive analysis of attendance conflicts
     * Business Rule: If leave date = attendance date → count as worked day, NOT leave
     * @param request Leave request to analyze
     * @return Analysis results with conflict details
     */
    public Map<String, Object> analyzeAttendanceConflicts(LeaveRequestModel request) {
        Map<String, Object> analysis = new HashMap<>();
        List<Date> conflictDates = new ArrayList<>();
        List<Date> workingLeaveDates = new ArrayList<>();
        
        try {
            List<Date> leaveDates = request.getWorkingDayLeaveDates();
            int originalLeaveDays = leaveDates.size();
            int conflictingDays = 0;
            
            for (Date leaveDate : leaveDates) {
                workingLeaveDates.add(leaveDate);
                
                AttendanceModel attendance = attendanceDAO.getAttendanceByEmployeeAndDate(
                    request.getEmployeeId(), leaveDate);
                
                if (attendance != null && attendance.isCompleteAttendance()) {
                    conflictDates.add(leaveDate);
                    conflictingDays++;
                    System.out.println("Conflict detected: Employee " + request.getEmployeeId() + 
                                     " has attendance on leave date " + leaveDate);
                }
            }
            
            // Calculate effective leave days (excluding conflicts)
            int effectiveLeaveDays = originalLeaveDays - conflictingDays;
            
            analysis.put("hasConflicts", !conflictDates.isEmpty());
            analysis.put("conflictDates", conflictDates);
            analysis.put("workingLeaveDates", workingLeaveDates);
            analysis.put("originalLeaveDays", originalLeaveDays);
            analysis.put("conflictingDays", conflictingDays);
            analysis.put("effectiveLeaveDays", effectiveLeaveDays);
            analysis.put("conflictResolution", "Conflicting dates count as worked days, not leave days");
            
        } catch (Exception e) {
            System.err.println("Error analyzing attendance conflicts: " + e.getMessage());
            analysis.put("hasConflicts", false);
            analysis.put("error", e.getMessage());
        }
        
        return analysis;
    }
    
    /**
     * Check for basic attendance conflicts (simplified check)
     * @param request Leave request
     * @return true if conflicts exist
     */
    public boolean hasAttendanceConflicts(LeaveRequestModel request) {
        Map<String, Object> analysis = analyzeAttendanceConflicts(request);
        return (Boolean) analysis.get("hasConflicts");
    }
    
    /**
     * Resolve attendance conflicts for approved leave
     * Business Rule Implementation: leave date = attendance date → count as worked day
     * @param request Approved leave request
     */
    private void resolveAttendanceConflictsForApprovedLeave(LeaveRequestModel request) {
        try {
            Map<String, Object> analysis = analyzeAttendanceConflicts(request);
            @SuppressWarnings("unchecked")
            List<Date> conflictDates = (List<Date>) analysis.get("conflictDates");
            
            if (conflictDates != null && !conflictDates.isEmpty()) {
                System.out.println("Resolving " + conflictDates.size() + " attendance conflicts for approved leave...");
                
                for (Date conflictDate : conflictDates) {
                    AttendanceModel attendance = attendanceDAO.getAttendanceByEmployeeAndDate(
                        request.getEmployeeId(), conflictDate);
                    
                    if (attendance != null && attendance.isCompleteAttendance()) {
                        // Business Rule: Keep attendance as worked day, don't count as leave
                        // Mark attendance as "leave-conflict-resolved" for tracking
                        // The attendance remains as a worked day for payroll calculation
                        
                        System.out.println("Conflict resolved for " + conflictDate + 
                                         ": Attendance preserved as worked day (not counted as leave)");
                        
                        // Optional: Add a note or flag to track this was a leave conflict
                        // This could be implemented as a separate tracking table or attendance notes
                    }
                }
                
                // Adjust the leave request to reflect only non-conflicting days
                int effectiveLeaveDays = (Integer) analysis.get("effectiveLeaveDays");
                System.out.println("Effective leave days after conflict resolution: " + effectiveLeaveDays);
                
                // Note: The leave balance should only be deducted for effective leave days
                // This is handled in the updateLeaveBalanceForApproval method
            }
        } catch (Exception e) {
            System.err.println("Error resolving attendance conflicts: " + e.getMessage());
        }
    }
    
    /**
     * Check for overlapping leave requests
     * @param request Leave request to check
     * @return true if overlapping requests exist
     */
    private boolean hasOverlappingLeaveRequests(LeaveRequestModel request) {
        try {
            List<LeaveRequestModel> existingRequests = leaveRequestDAO.getLeaveRequestsByEmployeeAndDateRange(
                request.getEmployeeId(), request.getLeaveStart(), request.getLeaveEnd());
            
            for (LeaveRequestModel existing : existingRequests) {
                if (existing.getLeaveRequestId() != request.getLeaveRequestId() && 
                    (existing.isPending() || existing.isApproved())) {
                    System.out.println("Overlapping leave request found: " + existing.getLeaveRequestId());
                    return true; // Overlapping request found
                }
            }
            
            return false;
        } catch (Exception e) {
            System.err.println("Error checking overlapping requests: " + e.getMessage());
            return false;
        }
    }
    
    // Enhanced leave balance management with conflict consideration
    
    /**
     * Check if employee has sufficient leave balance
     * @param employeeId Employee ID
     * @param leaveTypeId Leave type ID
     * @param requestedDays Requested leave days
     * @return true if sufficient balance
     */
    public boolean hasSufficientLeaveBalance(int employeeId, int leaveTypeId, int requestedDays) {
        try {
            Year currentYear = Year.now(MANILA_TIMEZONE);
            LeaveBalance balance = leaveBalanceDAO.getLeaveBalance(employeeId, leaveTypeId, currentYear);
            
            boolean sufficient = balance != null && balance.canTakeLeave(requestedDays);
            
            if (!sufficient) {
                System.out.println(", Requested: " +
                        ", Available: " +
                        "Insufficient leave balance - Employee: " + employeeId + requestedDays + (balance != null ? balance.getRemainingLeaveDays() : 0));
            }
            
            return sufficient;
        } catch (Exception e) {
            System.err.println("Error checking leave balance: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Update leave balance after approval (considering conflicts)
     * Only deduct effective leave days (excluding attendance conflicts)
     * @param request Approved leave request
     */
    private void updateLeaveBalanceForApproval(LeaveRequestModel request) {
        try {
            // Analyze conflicts to get effective leave days
            Map<String, Object> analysis = analyzeAttendanceConflicts(request);
            int effectiveLeaveDays = (Integer) analysis.get("effectiveLeaveDays");
            
            Year currentYear = Year.now(MANILA_TIMEZONE);
            LeaveBalance balance = leaveBalanceDAO.getLeaveBalance(
                request.getEmployeeId(), request.getLeaveTypeId(), currentYear);
            
            if (balance != null && effectiveLeaveDays > 0) {
                if (balance.deductLeave(effectiveLeaveDays)) {
                    leaveBalanceDAO.updateLeaveBalance(balance);
                    System.out.println("Leave balance updated - Deducted: " + effectiveLeaveDays + 
                                     " days (after conflict resolution)");
                } else {
                    System.err.println("Failed to deduct leave from balance");
                }
            } else if (effectiveLeaveDays == 0) {
                System.out.println("No leave balance deduction needed - all leave dates had attendance conflicts");
            }
        } catch (Exception e) {
            System.err.println("Error updating leave balance: " + e.getMessage());
        }
    }
    
    /**
     * Restore leave balance after cancellation
     * @param request Cancelled leave request
     */
    private void restoreLeaveBalance(LeaveRequestModel request) {
        try {
            // Calculate how much was actually deducted (considering past conflicts)
            Map<String, Object> analysis = analyzeAttendanceConflicts(request);
            int effectiveLeaveDays = (Integer) analysis.get("effectiveLeaveDays");
            
            Year currentYear = Year.now(MANILA_TIMEZONE);
            LeaveBalance balance = leaveBalanceDAO.getLeaveBalance(
                request.getEmployeeId(), request.getLeaveTypeId(), currentYear);
            
            if (balance != null && effectiveLeaveDays > 0) {
                balance.addLeave(effectiveLeaveDays);
                leaveBalanceDAO.updateLeaveBalance(balance);
                System.out.println("Leave balance restored - Added back: " + effectiveLeaveDays + " days");
            }
        } catch (Exception e) {
            System.err.println("Error restoring leave balance: " + e.getMessage());
        }
    }
    
    // Query methods
    
    /**
     * Get leave requests by employee with optional status filter
     * @param employeeId Employee ID
     * @param status Approval status filter (null for all)
     * @return List of leave requests
     */
    public List<LeaveRequestModel> getLeaveRequestsByEmployee(int employeeId, LeaveRequestModel.ApprovalStatus status) {
        try {
            return leaveRequestDAO.getLeaveRequestsByEmployee(employeeId, status);
        } catch (Exception e) {
            System.err.println("Error getting leave requests: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Get pending leave requests for supervisor
     * @param supervisorId Supervisor ID
     * @return List of pending leave requests
     */
    public List<LeaveRequestModel> getPendingLeaveRequestsForSupervisor(int supervisorId) {
        try {
            return leaveRequestDAO.getPendingLeaveRequestsForSupervisor(supervisorId);
        } catch (Exception e) {
            System.err.println("Error getting pending requests: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Get leave requests in date range
     * @param employeeId Employee ID
     * @param startDate Start date
     * @param endDate End date
     * @return List of leave requests in range
     */
    public List<LeaveRequestModel> getLeaveRequestsByDateRange(int employeeId, Date startDate, Date endDate) {
        try {
            return leaveRequestDAO.getLeaveRequestsByEmployeeAndDateRange(employeeId, startDate, endDate);
        } catch (Exception e) {
            System.err.println("Error getting leave requests by date range: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Get leave requests with attendance conflicts
     * @param employeeId Employee ID
     * @param startDate Start date
     * @param endDate End date
     * @return List of leave requests with conflicts
     */
    public List<LeaveRequestModel> getLeaveRequestsWithConflicts(int employeeId, Date startDate, Date endDate) {
        try {
            List<LeaveRequestModel> allRequests = getLeaveRequestsByDateRange(employeeId, startDate, endDate);
            List<LeaveRequestModel> conflictRequests = new ArrayList<>();
            
            for (LeaveRequestModel request : allRequests) {
                if (hasAttendanceConflicts(request)) {
                    conflictRequests.add(request);
                }
            }
            
            return conflictRequests;
        } catch (Exception e) {
            System.err.println("Error getting leave requests with conflicts: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Get leave balance for employee
     * @param employeeId Employee ID
     * @param leaveTypeId Leave type ID
     * @return Leave balance
     */
    public LeaveBalance getLeaveBalance(int employeeId, int leaveTypeId) {
        try {
            Year currentYear = Year.now(MANILA_TIMEZONE);
            return leaveBalanceDAO.getLeaveBalance(employeeId, leaveTypeId, currentYear);
        } catch (Exception e) {
            System.err.println("Error getting leave balance: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Get all leave balances for employee
     * @param employeeId Employee ID
     * @return List of all leave balances
     */
    public List<LeaveBalance> getAllLeaveBalances(int employeeId) {
        try {
            Year currentYear = Year.now(MANILA_TIMEZONE);
            return leaveBalanceDAO.getLeaveBalancesByEmployee(employeeId, currentYear);
        } catch (Exception e) {
            System.err.println("Error getting all leave balances: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    // Enhanced reporting and analytics
    
    /**
     * Get enhanced monthly leave summary with conflict analysis
     * @param employeeId Employee ID
     * @param month Month
     * @param year Year
     * @return Monthly leave summary with conflict data
     */
    public Map<String, Object> getMonthlyLeaveSummary(int employeeId, int month, int year) {
        try {
            Date startDate = Date.valueOf(LocalDate.of(year, month, 1));
            Date endDate = Date.valueOf(LocalDate.of(year, month, 1).withDayOfMonth(
                LocalDate.of(year, month, 1).lengthOfMonth()));
            
            List<LeaveRequestModel> leaveRequests = getLeaveRequestsByDateRange(employeeId, startDate, endDate);
            
            Map<String, Object> summary = new HashMap<>();
            
            if (leaveRequests != null) {
                int totalRequests = leaveRequests.size();
                int approvedRequests = 0;
                int rejectedRequests = 0;
                int pendingRequests = 0;
                int totalLeaveDays = 0;
                int conflictingRequests = 0;
                int totalConflictDays = 0;
                int effectiveLeaveDays = 0;
                
                for (LeaveRequestModel request : leaveRequests) {
                    if (request.isApproved()) {
                        approvedRequests++;
                        
                        // Analyze conflicts for approved requests
                        Map<String, Object> analysis = analyzeAttendanceConflicts(request);
                        int originalDays = (Integer) analysis.get("originalLeaveDays");
                        int conflictDays = (Integer) analysis.get("conflictingDays");
                        int effectiveDays = (Integer) analysis.get("effectiveLeaveDays");
                        
                        totalLeaveDays += originalDays;
                        effectiveLeaveDays += effectiveDays;
                        
                        if ((Boolean) analysis.get("hasConflicts")) {
                            conflictingRequests++;
                            totalConflictDays += conflictDays;
                        }
                        
                    } else if (request.isRejected()) {
                        rejectedRequests++;
                    } else {
                        pendingRequests++;
                    }
                }
                
                summary.put("totalRequests", totalRequests);
                summary.put("approvedRequests", approvedRequests);
                summary.put("rejectedRequests", rejectedRequests);
                summary.put("pendingRequests", pendingRequests);
                summary.put("totalLeaveDays", totalLeaveDays);
                summary.put("effectiveLeaveDays", effectiveLeaveDays);
                summary.put("conflictingRequests", conflictingRequests);
                summary.put("totalConflictDays", totalConflictDays);
                summary.put("approvalRate", totalRequests > 0 ? (double) approvedRequests / totalRequests * 100 : 0);
                summary.put("conflictRate", approvedRequests > 0 ? (double) conflictingRequests / approvedRequests * 100 : 0);
                summary.put("conflictResolutionNote", "Conflict days count as worked days, not leave days");
            }
            
            return summary;
        } catch (Exception e) {
            System.err.println("Error getting monthly leave summary: " + e.getMessage());
            return new HashMap<>();
        }
    }
    
    /**
     * Get leave utilization report with conflict analysis
     * @param employeeId Employee ID
     * @param year Year
     * @return Leave utilization report
     */
    public Map<String, Object> getLeaveUtilizationReport(int employeeId, int year) {
        try {
            Map<String, Object> report = new HashMap<>();
            List<LeaveBalance> balances = leaveBalanceDAO.getLeaveBalancesByEmployee(employeeId, Year.of(year));
            
            if (balances != null) {
                for (LeaveBalance balance : balances) {
                    Map<String, Object> balanceInfo = new HashMap<>();
                    balanceInfo.put("totalDays", balance.getTotalLeaveDays());
                    balanceInfo.put("usedDays", balance.getUsedLeaveDays());
                    balanceInfo.put("remainingDays", balance.getRemainingLeaveDays());
                    balanceInfo.put("carryOverDays", balance.getCarryOverDays());
                    balanceInfo.put("utilizationRate", balance.getUtilizationRate());
                    balanceInfo.put("status", balance.getBalanceStatus());
                    
                    report.put("leaveType_" + balance.getLeaveTypeId(), balanceInfo);
                }
                
                // Add conflict analysis for the year
                Date yearStart = Date.valueOf(LocalDate.of(year, 1, 1));
                Date yearEnd = Date.valueOf(LocalDate.of(year, 12, 31));
                List<LeaveRequestModel> conflictRequests = getLeaveRequestsWithConflicts(employeeId, yearStart, yearEnd);
                
                report.put("conflictAnalysis", Map.of(
                    "conflictingRequests", conflictRequests.size(),
                    "conflictResolutionPolicy", "Attendance takes precedence - conflict days count as worked days"
                ));
            }
            
            return report;
        } catch (Exception e) {
            System.err.println("Error getting leave utilization report: " + e.getMessage());
            return new HashMap<>();
        }
    }
    
    // Validation methods
    
    /**
     * Enhanced leave request eligibility validation
     * @param employeeId Employee ID
     * @param leaveTypeId Leave type ID
     * @param startDate Start date
     * @param endDate End date
     * @return true if eligible
     */
    public boolean canRequestLeave(int employeeId, int leaveTypeId, Date startDate, Date endDate) {
        try {
            // Check date validity
            if (startDate == null || endDate == null || endDate.before(startDate)) {
                System.err.println("Invalid date range for leave request");
                return false;
            }
            
            // Check if dates are in the past
            Date today = Date.valueOf(LocalDate.now(MANILA_TIMEZONE));
            if (startDate.before(today)) {
                System.err.println("Cannot request leave for past dates");
                return false;
            }
            
            // Create temporary request to check working days
            LeaveRequestModel tempRequest = new LeaveRequestModel(employeeId, leaveTypeId, startDate, endDate, "");
            
            // Check if there are working days in the range
            if (!tempRequest.hasWorkingDays()) {
                System.err.println("No working days in the specified leave range");
                return false;
            }
            
            // Check leave balance
            boolean hasBalance = hasSufficientLeaveBalance(employeeId, leaveTypeId, tempRequest.getWorkingDaysCount());
            if (!hasBalance) {
                System.err.println("Insufficient leave balance for the requested period");
            }
            
            return hasBalance;
            
        } catch (Exception e) {
            System.err.println("Error checking leave eligibility: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get upcoming leaves for employee
     * @param employeeId Employee ID
     * @return List of upcoming approved/pending leaves
     */
    public List<LeaveRequestModel> getUpcomingLeaves(int employeeId) {
        try {
            Date today = Date.valueOf(LocalDate.now(MANILA_TIMEZONE));
            Date futureDate = Date.valueOf(LocalDate.now(MANILA_TIMEZONE).plusMonths(3));
            
            List<LeaveRequestModel> allRequests = getLeaveRequestsByDateRange(employeeId, today, futureDate);
            
            return allRequests.stream()
                    .filter(request -> request.isApproved() || request.isPending())
                    .collect(java.util.stream.Collectors.toList());
        } catch (Exception e) {
            System.err.println("Error getting upcoming leaves: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    // Utility methods
    
    /**
     * Get current Manila time
     * @return Current LocalDateTime in Manila timezone
     */
    public LocalDateTime getCurrentManilaTime() {
        return LocalDateTime.now(MANILA_TIMEZONE);
    }
    
    /**
     * Initialize leave balances for new employee
     * @param employeeId Employee ID
     * @param positionId Position ID
     * @return true if successful
     */
    public boolean initializeLeaveBalancesForEmployee(int employeeId, int positionId) {
        try {
            // This would typically get leave entitlements based on position/company policy
            Year currentYear = Year.now(MANILA_TIMEZONE);
            
            // Standard leave allocations (these should come from configuration or position-based rules)
            Map<Integer, Integer> standardAllocations = new HashMap<>();
            standardAllocations.put(1, 15); // Vacation Leave - 15 days
            standardAllocations.put(2, 15); // Sick Leave - 15 days
            standardAllocations.put(3, 5);  // Emergency Leave - 5 days
            
            boolean allSuccess = true;
            
            for (Map.Entry<Integer, Integer> allocation : standardAllocations.entrySet()) {
                LeaveBalance balance = new LeaveBalance(employeeId, allocation.getKey(), allocation.getValue(), currentYear);
                if (!leaveBalanceDAO.createLeaveBalance(balance)) {
                    allSuccess = false;
                    System.err.println("Failed to create leave balance for leave type: " + allocation.getKey());
                }
            }
            
            if (allSuccess) {
                System.out.println("Leave balances initialized successfully for employee " + employeeId);
            }
            
            return allSuccess;
        } catch (Exception e) {
            System.err.println("Error initializing leave balances: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get conflict resolution summary for a specific leave request
     * @param requestId Leave request ID
     * @return Conflict resolution details
     */
    public Map<String, Object> getConflictResolutionSummary(int requestId) {
        try {
            LeaveRequestModel request = leaveRequestDAO.getLeaveRequestById(requestId);
            if (request == null) {
                return new HashMap<>();
            }
            
            Map<String, Object> summary = analyzeAttendanceConflicts(request);
            summary.put("requestId", requestId);
            summary.put("employeeId", request.getEmployeeId());
            summary.put("leaveStart", request.getLeaveStart());
            summary.put("leaveEnd", request.getLeaveEnd());
            summary.put("approvalStatus", request.getApprovalStatus().toString());
            
            return summary;
        } catch (Exception e) {
            System.err.println("Error getting conflict resolution summary: " + e.getMessage());
            return new HashMap<>();
        }
    }
    
    /**
     * Batch process conflict resolution for multiple leave requests
     * Useful for processing past approved leaves that may have attendance conflicts
     * @param employeeId Employee ID
     * @param startDate Start date for processing
     * @param endDate End date for processing
     * @return Processing results
     */
    public Map<String, Object> batchProcessConflictResolution(int employeeId, Date startDate, Date endDate) {
        Map<String, Object> results = new HashMap<>();
        List<String> processedRequests = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        
        try {
            List<LeaveRequestModel> approvedLeaves = getLeaveRequestsByDateRange(employeeId, startDate, endDate)
                .stream()
                .filter(LeaveRequestModel::isApproved)
                .collect(java.util.stream.Collectors.toList());
            
            int totalProcessed = 0;
            int conflictsResolved = 0;
            
            for (LeaveRequestModel request : approvedLeaves) {
                try {
                    Map<String, Object> analysis = analyzeAttendanceConflicts(request);
                    
                    if ((Boolean) analysis.get("hasConflicts")) {
                        resolveAttendanceConflictsForApprovedLeave(request);
                        conflictsResolved++;
                        processedRequests.add("Request " + request.getLeaveRequestId() + 
                                            " - Resolved " + analysis.get("conflictingDays") + " conflicts");
                    }
                    
                    totalProcessed++;
                } catch (Exception e) {
                    errors.add("Error processing request " + request.getLeaveRequestId() + ": " + e.getMessage());
                }
            }
            
            results.put("totalProcessed", totalProcessed);
            results.put("conflictsResolved", conflictsResolved);
            results.put("processedRequests", processedRequests);
            results.put("errors", errors);
            results.put("success", errors.isEmpty());
            
        } catch (Exception e) {
            results.put("success", false);
            results.put("error", e.getMessage());
        }
        
        return results;
    }
    
    /**
     * Generate leave audit report with conflict analysis
     * @param employeeId Employee ID
     * @param year Year for audit
     * @return Comprehensive audit report
     */
    public Map<String, Object> generateLeaveAuditReport(int employeeId, int year) {
        Map<String, Object> auditReport = new HashMap<>();
        
        try {
            Date yearStart = Date.valueOf(LocalDate.of(year, 1, 1));
            Date yearEnd = Date.valueOf(LocalDate.of(year, 12, 31));
            
            // Get all leave requests for the year
            List<LeaveRequestModel> allRequests = getLeaveRequestsByDateRange(employeeId, yearStart, yearEnd);
            
            // Analyze each approved request for conflicts
            List<Map<String, Object>> conflictAnalyses = new ArrayList<>();
            int totalConflictDays = 0;
            int totalOriginalLeaveDays = 0;
            int totalEffectiveLeaveDays = 0;
            
            for (LeaveRequestModel request : allRequests) {
                if (request.isApproved()) {
                    Map<String, Object> analysis = analyzeAttendanceConflicts(request);
                    analysis.put("requestId", request.getLeaveRequestId());
                    analysis.put("leaveType", request.getLeaveTypeId());
                    analysis.put("dateRange", request.getLeaveStart() + " to " + request.getLeaveEnd());
                    
                    conflictAnalyses.add(analysis);
                    
                    totalOriginalLeaveDays += (Integer) analysis.get("originalLeaveDays");
                    totalEffectiveLeaveDays += (Integer) analysis.get("effectiveLeaveDays");
                    totalConflictDays += (Integer) analysis.get("conflictingDays");
                }
            }
            
            // Get leave balances
            List<LeaveBalance> balances = getAllLeaveBalances(employeeId);
            
            // Compile audit report
            auditReport.put("employeeId", employeeId);
            auditReport.put("auditYear", year);
            auditReport.put("totalRequests", allRequests.size());
            auditReport.put("approvedRequests", allRequests.stream().mapToInt(r -> r.isApproved() ? 1 : 0).sum());
            auditReport.put("totalOriginalLeaveDays", totalOriginalLeaveDays);
            auditReport.put("totalEffectiveLeaveDays", totalEffectiveLeaveDays);
            auditReport.put("totalConflictDays", totalConflictDays);
            auditReport.put("conflictRate", totalOriginalLeaveDays > 0 ? 
                (double) totalConflictDays / totalOriginalLeaveDays * 100 : 0);
            auditReport.put("conflictAnalyses", conflictAnalyses);
            auditReport.put("leaveBalances", balances);
            auditReport.put("auditTimestamp", getCurrentManilaTime());
            auditReport.put("conflictResolutionPolicy", 
                "When leave date = attendance date, the day counts as worked (not leave)");
            
        } catch (Exception e) {
            auditReport.put("error", e.getMessage());
            auditReport.put("success", false);
        }
        
        return auditReport;
    }
    
    /**
     * Check if a specific date has leave-attendance conflict for employee
     * @param employeeId Employee ID
     * @param date Date to check
     * @return Conflict details
     */
    public Map<String, Object> checkDateConflict(int employeeId, Date date) {
        Map<String, Object> conflictInfo = new HashMap<>();
        
        try {
            // Check for attendance on this date
            AttendanceModel attendance = attendanceDAO.getAttendanceByEmployeeAndDate(employeeId, date);
            
            // Check for approved leave on this date
            List<LeaveRequestModel> leaveRequests = getLeaveRequestsByDateRange(employeeId, date, date);
            LeaveRequestModel approvedLeave = leaveRequests.stream()
                .filter(LeaveRequestModel::isApproved)
                .findFirst()
                .orElse(null);
            
            boolean hasAttendance = attendance != null && attendance.isCompleteAttendance();
            boolean hasApprovedLeave = approvedLeave != null;
            boolean hasConflict = hasAttendance && hasApprovedLeave;
            
            conflictInfo.put("date", date);
            conflictInfo.put("hasAttendance", hasAttendance);
            conflictInfo.put("hasApprovedLeave", hasApprovedLeave);
            conflictInfo.put("hasConflict", hasConflict);
            
            if (hasAttendance) {
                conflictInfo.put("attendanceDetails", Map.of(
                    "timeIn", attendance.getFormattedTimeIn(),
                    "timeOut", attendance.getFormattedTimeOut(),
                    "computedHours", attendance.getComputedHours()
                ));
            }
            
            if (hasApprovedLeave) {
                conflictInfo.put("leaveDetails", Map.of(
                    "requestId", approvedLeave.getLeaveRequestId(),
                    "leaveTypeId", approvedLeave.getLeaveTypeId(),
                    "reason", approvedLeave.getLeaveReason()
                ));
            }
            
            if (hasConflict) {
                conflictInfo.put("resolution", "Date counts as worked day (attendance takes precedence over leave)");
                conflictInfo.put("payrollImpact", "Employee receives regular pay for attendance, leave balance not deducted");
            }
            
        } catch (Exception e) {
            conflictInfo.put("error", e.getMessage());
        }
        
        return conflictInfo;
    }
    
    /**
     * Get leave effectiveness rate (how much approved leave was actually taken as leave vs worked)
     * @param employeeId Employee ID
     * @param startDate Start date
     * @param endDate End date
     * @return Effectiveness metrics
     */
    public Map<String, Object> getLeaveEffectivenessRate(int employeeId, Date startDate, Date endDate) {
        Map<String, Object> effectiveness = new HashMap<>();
        
        try {
            List<LeaveRequestModel> approvedLeaves = getLeaveRequestsByDateRange(employeeId, startDate, endDate)
                .stream()
                .filter(LeaveRequestModel::isApproved)
                .collect(java.util.stream.Collectors.toList());
            
            int totalApprovedDays = 0;
            int totalEffectiveLeaveDays = 0;
            int totalWorkedInsteadDays = 0;
            
            for (LeaveRequestModel request : approvedLeaves) {
                Map<String, Object> analysis = analyzeAttendanceConflicts(request);
                
                totalApprovedDays += (Integer) analysis.get("originalLeaveDays");
                totalEffectiveLeaveDays += (Integer) analysis.get("effectiveLeaveDays");
                totalWorkedInsteadDays += (Integer) analysis.get("conflictingDays");
            }
            
            double effectivenessRate = totalApprovedDays > 0 ? 
                (double) totalEffectiveLeaveDays / totalApprovedDays * 100 : 100.0;
            
            effectiveness.put("totalApprovedDays", totalApprovedDays);
            effectiveness.put("totalEffectiveLeaveDays", totalEffectiveLeaveDays);
            effectiveness.put("totalWorkedInsteadDays", totalWorkedInsteadDays);
            effectiveness.put("effectivenessRate", effectivenessRate);
            effectiveness.put("dateRange", startDate + " to " + endDate);
            effectiveness.put("interpretation", 
                effectivenessRate >= 90 ? "High effectiveness - most leave was taken as intended" :
                effectivenessRate >= 70 ? "Moderate effectiveness - some leave-work conflicts" :
                "Low effectiveness - significant leave-work conflicts");
            
        } catch (Exception e) {
            effectiveness.put("error", e.getMessage());
        }
        
        return effectiveness;
    }
}