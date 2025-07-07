package Models;

import java.time.*;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class ImmediateSupervisorModel {
    private static final ZoneId MANILA_TIMEZONE = ZoneId.of("Asia/Manila");
    
    // Supervisor information
    private int supervisorId;
    private String firstName;
    private String lastName;
    private String email;
    private String position;
    private String department;
    private String userRole;
    private String status;
    private Timestamp lastLogin;
    
    // Team management fields
    private List<Integer> subordinateIds;
    private int teamSize;
    private String managementLevel; // "IMMEDIATE", "MIDDLE", "SENIOR"
    private boolean canApproveLeave;
    private boolean canApproveOvertime;
    private boolean canManageAttendance;
    private double maxOvertimeApprovalHours;
    private int maxLeaveApprovalDays;
    
    // Performance tracking
    private int pendingApprovals;
    private int totalApprovalsThisMonth;
    private double averageApprovalTime; // in hours
    private Timestamp lastApprovalAction;
    
    // Constructors
    public ImmediateSupervisorModel() {
        this.canApproveLeave = true;
        this.canApproveOvertime = true;
        this.canManageAttendance = true;
        this.maxOvertimeApprovalHours = 4.0; // Default 4 hours max
        this.maxLeaveApprovalDays = 5; // Default 5 days max
        this.managementLevel = "IMMEDIATE";
    }
    
    public ImmediateSupervisorModel(int supervisorId, String firstName, String lastName, String email) {
        this();
        this.supervisorId = supervisorId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.userRole = "Supervisor";
        this.status = "Active";
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
     * Get last approval action in Manila timezone
     * @return 
     */
    public LocalDateTime getLastApprovalActionInManila() {
        if (lastApprovalAction == null) return null;
        return lastApprovalAction.toLocalDateTime().atZone(ZoneId.systemDefault())
                .withZoneSameInstant(MANILA_TIMEZONE).toLocalDateTime();
    }
    
    // Team management methods
    
    /**
     * Check if employee is under this supervisor's management
     * @param employeeId
     * @return 
     */
    public boolean managesEmployee(int employeeId) {
        return subordinateIds != null && subordinateIds.contains(employeeId);
    }
    
    /**
     * Add employee to team
     * @param employeeId
     * @return 
     */
    public boolean addTeamMember(int employeeId) {
        if (subordinateIds == null) {
            subordinateIds = new java.util.ArrayList<>();
        }
        
        if (!subordinateIds.contains(employeeId)) {
            subordinateIds.add(employeeId);
            updateTeamSize();
            return true;
        }
        return false;
    }
    
    /**
     * Remove employee from team
     * @param employeeId
     * @return 
     */
    public boolean removeTeamMember(int employeeId) {
        if (subordinateIds != null) {
            boolean removed = subordinateIds.remove(Integer.valueOf(employeeId));
            if (removed) {
                updateTeamSize();
            }
            return removed;
        }
        return false;
    }
    
    /**
     * Update team size
     */
    private void updateTeamSize() {
        this.teamSize = subordinateIds != null ? subordinateIds.size() : 0;
    }
    
    /**
     * Get team members list
     * @return 
     */
    public List<Integer> getTeamMembers() {
        return subordinateIds != null ? new java.util.ArrayList<>(subordinateIds) : new java.util.ArrayList<>();
    }
    
    // Approval methods
    
    /**
     * Check if supervisor can approve leave request
     * @param request
     * @return 
     */
    public boolean canApproveLeaveRequest(LeaveRequestModel request) {
        if (!canApproveLeave || request == null) {
            return false;
        }
        
        // Check if employee is under supervision
        if (!managesEmployee(request.getEmployeeId())) {
            return false;
        }
        
        // Check if within approval limits
        if (request.getWorkingDaysCount() > maxLeaveApprovalDays) {
            return false; // Requires higher approval
        }
        
        return request.isPending();
    }
    
    /**
     * Check if supervisor can approve overtime request
     * @param request
     * @return 
     */
    public boolean canApproveOvertimeRequest(OvertimeRequestModel request) {
        if (!canApproveOvertime || request == null) {
            return false;
        }
        
        // Check if employee is under supervision
        if (!managesEmployee(request.getEmployeeId())) {
            return false;
        }
        
        // Check if within approval limits
        if (request.getOvertimeHours() > maxOvertimeApprovalHours) {
            return false; // Requires higher approval
        }
        
        return request.isPending();
    }
    
    /**
     * Approve leave request
     * @param request
     * @param notes
     * @return 
     */
    public boolean approveLeaveRequest(LeaveRequestModel request, String notes) {
        if (!canApproveLeaveRequest(request)) {
            return false;
        }
        
        request.approve(notes);
        recordApprovalAction();
        return true;
    }
    
    /**
     * Reject leave request
     * @param request
     * @param notes
     * @return 
     */
    public boolean rejectLeaveRequest(LeaveRequestModel request, String notes) {
        if (!canApproveLeaveRequest(request)) {
            return false;
        }
        
        request.reject(notes);
        recordApprovalAction();
        return true;
    }
    
    /**
     * Approve overtime request
     * @param request
     * @param notes
     * @return 
     */
    public boolean approveOvertimeRequest(OvertimeRequestModel request, String notes) {
        if (!canApproveOvertimeRequest(request)) {
            return false;
        }
        
        request.approve(notes);
        recordApprovalAction();
        return true;
    }
    
    /**
     * Reject overtime request
     * @param request
     * @param notes
     * @return 
     */
    public boolean rejectOvertimeRequest(OvertimeRequestModel request, String notes) {
        if (!canApproveOvertimeRequest(request)) {
            return false;
        }
        
        request.reject(notes);
        recordApprovalAction();
        return true;
    }
    
    /**
     * Record approval action for tracking
     */
    private void recordApprovalAction() {
        this.lastApprovalAction = Timestamp.valueOf(getCurrentManilaTime());
        this.totalApprovalsThisMonth++;
        updatePendingApprovalsCount();
    }
    
    // Attendance management
    
    /**
     * Check if supervisor can manage employee attendance
     * @param employeeId
     * @return 
     */
    public boolean canManageEmployeeAttendance(int employeeId) {
        return canManageAttendance && managesEmployee(employeeId);
    }
    
    /**
     * Approve manual attendance entry
     * @param attendance
     * @param reason
     * @return 
     */
    public boolean approveManualAttendance(AttendanceModel attendance, String reason) {
        if (!canManageEmployeeAttendance(attendance.getEmployeeId())) {
            return false;
        }
        
        // Additional validation can be added here
        recordApprovalAction();
        return true;
    }
    
    /**
     * Review tardiness record
     * @param tardiness
     * @param notes
     * @return 
     */
    public boolean reviewTardinessRecord(TardinessRecordModel tardiness, String notes) {
        if (tardiness.getRelatedAttendance() != null && 
            canManageEmployeeAttendance(tardiness.getRelatedAttendance().getEmployeeId())) {
            
            tardiness.setSupervisorNotes(notes);
            return true;
        }
        return false;
    }
    
    // Dashboard and reporting methods
    
    /**
     * Get supervisor dashboard data
     * @return 
     */
    public Map<String, Object> getDashboardData() {
        Map<String, Object> dashboard = new HashMap<>();
        
        // Team overview
        dashboard.put("teamSize", teamSize);
        dashboard.put("managementLevel", managementLevel);
        dashboard.put("pendingApprovals", pendingApprovals);
        dashboard.put("totalApprovalsThisMonth", totalApprovalsThisMonth);
        dashboard.put("averageApprovalTime", averageApprovalTime);
        
        // Approval permissions
        dashboard.put("canApproveLeave", canApproveLeave);
        dashboard.put("canApproveOvertime", canApproveOvertime);
        dashboard.put("canManageAttendance", canManageAttendance);
        dashboard.put("maxOvertimeApprovalHours", maxOvertimeApprovalHours);
        dashboard.put("maxLeaveApprovalDays", maxLeaveApprovalDays);
        
        // Last activity
        dashboard.put("lastLogin", getLastLoginInManila());
        dashboard.put("lastApprovalAction", getLastApprovalActionInManila());
        
        return dashboard;
    }
    
    /**
     * Get team attendance summary for current month
     * @return 
     */
    public Map<String, Object> getTeamAttendanceSummary() {
        Map<String, Object> summary = new HashMap<>();
        
        if (subordinateIds == null || subordinateIds.isEmpty()) {
            return summary;
        }
        
        LocalDate now = LocalDate.now(MANILA_TIMEZONE);
        Date startOfMonth = Date.valueOf(now.withDayOfMonth(1));
        Date endOfMonth = Date.valueOf(now.withDayOfMonth(now.lengthOfMonth()));
        
        int totalEmployees = teamSize;
        int employeesWithPerfectAttendance = 0;
        int employeesWithTardiness = 0;
        double teamAttendanceRate = 0.0;
        
        // This would typically query the database for actual data
        // For now, providing structure for the summary
        
        summary.put("totalEmployees", totalEmployees);
        summary.put("employeesWithPerfectAttendance", employeesWithPerfectAttendance);
        summary.put("employeesWithTardiness", employeesWithTardiness);
        summary.put("teamAttendanceRate", teamAttendanceRate);
        summary.put("periodStart", startOfMonth);
        summary.put("periodEnd", endOfMonth);
        
        return summary;
    }
    
    /**
     * Get pending items requiring supervisor action
     * @return 
     */
    public Map<String, Object> getPendingItems() {
        Map<String, Object> pending = new HashMap<>();
        
        // This would query the database for actual pending items
        int pendingLeaveRequests = 0;
        int pendingOvertimeRequests = 0;
        int manualAttendanceReviews = 0;
        int tardinessReviews = 0;
        
        pending.put("pendingLeaveRequests", pendingLeaveRequests);
        pending.put("pendingOvertimeRequests", pendingOvertimeRequests);
        pending.put("manualAttendanceReviews", manualAttendanceReviews);
        pending.put("tardinessReviews", tardinessReviews);
        pending.put("totalPending", pendingLeaveRequests + pendingOvertimeRequests + 
                                   manualAttendanceReviews + tardinessReviews);
        
        return pending;
    }
    
    // Validation methods
    
    /**
     * Validate supervisor permissions
     * @return 
     */
    public boolean hasValidPermissions() {
        return userRole != null && 
               (userRole.equals("Supervisor") || userRole.equals("Manager") || 
                userRole.equals("HR") || userRole.equals("Admin"));
    }
    
    /**
     * Check if supervisor is active
     * @return 
     */
    public boolean isActive() {
        return "Active".equalsIgnoreCase(status);
    }
    
    /**
     * Update pending approvals count
     */
    private void updatePendingApprovalsCount() {
        // This would typically query the database
        // For now, just decrement if positive
        if (pendingApprovals > 0) {
            pendingApprovals--;
        }
    }
    
    // Utility methods
    
    /**
     * Get supervisor display name
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
            return "Supervisor " + supervisorId;
        }
    }
    
    /**
     * Get supervisor full name with position
     * @return 
     */
    public String getFullNameWithPosition() {
        String name = getDisplayName();
        if (position != null) {
            return name + " (" + position + ")";
        }
        return name;
    }
    
    // Getters and Setters
    public int getSupervisorId() { return supervisorId; }
    public void setSupervisorId(int supervisorId) { this.supervisorId = supervisorId; }
    
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
    
    public List<Integer> getSubordinateIds() { return subordinateIds; }
    public void setSubordinateIds(List<Integer> subordinateIds) { 
        this.subordinateIds = subordinateIds; 
        updateTeamSize();
    }
    
    public int getTeamSize() { return teamSize; }
    public void setTeamSize(int teamSize) { this.teamSize = teamSize; }
    
    public String getManagementLevel() { return managementLevel; }
    public void setManagementLevel(String managementLevel) { this.managementLevel = managementLevel; }
    
    public boolean isCanApproveLeave() { return canApproveLeave; }
    public void setCanApproveLeave(boolean canApproveLeave) { this.canApproveLeave = canApproveLeave; }
    
    public boolean isCanApproveOvertime() { return canApproveOvertime; }
    public void setCanApproveOvertime(boolean canApproveOvertime) { this.canApproveOvertime = canApproveOvertime; }
    
    public boolean isCanManageAttendance() { return canManageAttendance; }
    public void setCanManageAttendance(boolean canManageAttendance) { this.canManageAttendance = canManageAttendance; }
    
    public double getMaxOvertimeApprovalHours() { return maxOvertimeApprovalHours; }
    public void setMaxOvertimeApprovalHours(double maxOvertimeApprovalHours) { this.maxOvertimeApprovalHours = maxOvertimeApprovalHours; }
    
    public int getMaxLeaveApprovalDays() { return maxLeaveApprovalDays; }
    public void setMaxLeaveApprovalDays(int maxLeaveApprovalDays) { this.maxLeaveApprovalDays = maxLeaveApprovalDays; }
    
    public int getPendingApprovals() { return pendingApprovals; }
    public void setPendingApprovals(int pendingApprovals) { this.pendingApprovals = pendingApprovals; }
    
    public int getTotalApprovalsThisMonth() { return totalApprovalsThisMonth; }
    public void setTotalApprovalsThisMonth(int totalApprovalsThisMonth) { this.totalApprovalsThisMonth = totalApprovalsThisMonth; }
    
    public double getAverageApprovalTime() { return averageApprovalTime; }
    public void setAverageApprovalTime(double averageApprovalTime) { this.averageApprovalTime = averageApprovalTime; }
    
    public Timestamp getLastApprovalAction() { return lastApprovalAction; }
    public void setLastApprovalAction(Timestamp lastApprovalAction) { this.lastApprovalAction = lastApprovalAction; }
    
    @Override
    public String toString() {
        return String.format("ImmediateSupervisorModel{supervisorId=%d, name='%s', position='%s', teamSize=%d, pendingApprovals=%d}",
                supervisorId, getDisplayName(), position, teamSize, pendingApprovals);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ImmediateSupervisorModel that = (ImmediateSupervisorModel) obj;
        return supervisorId == that.supervisorId;
    }
    
    @Override
    public int hashCode() {
        return Integer.hashCode(supervisorId);
    }
}