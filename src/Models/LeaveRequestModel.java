package Models;

import java.time.*;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;
import java.util.ArrayList;

public class LeaveRequestModel {
    private static final ZoneId MANILA_TIMEZONE = ZoneId.of("Asia/Manila");
    
    public enum ApprovalStatus {
        PENDING("Pending"),
        APPROVED("Approved"), 
        REJECTED("Rejected");
        
        private final String value;
        ApprovalStatus(String value) { this.value = value; }
        public String getValue() { return value; }
        
        public static ApprovalStatus fromString(String status) {
            for (ApprovalStatus as : ApprovalStatus.values()) {
                if (as.value.equalsIgnoreCase(status)) {
                    return as;
                }
            }
            return PENDING;
        }
    }
    
    private int leaveRequestId;
    private int employeeId;
    private int leaveTypeId;
    private Date leaveStart;
    private Date leaveEnd;
    private String leaveReason;
    private ApprovalStatus approvalStatus;
    private Timestamp dateCreated;
    private Timestamp dateApproved;
    private String supervisorNotes;
    private boolean hasAttendanceConflict;
    private int workingDaysCount;
    
    // Constructors
    public LeaveRequestModel() {
        this.approvalStatus = ApprovalStatus.PENDING;
        this.dateCreated = Timestamp.valueOf(LocalDateTime.now(MANILA_TIMEZONE));
        this.hasAttendanceConflict = false;
    }
    
    public LeaveRequestModel(int employeeId, int leaveTypeId, Date leaveStart, Date leaveEnd, String leaveReason) {
        this();
        this.employeeId = employeeId;
        this.leaveTypeId = leaveTypeId;
        this.leaveStart = leaveStart;
        this.leaveEnd = leaveEnd;
        this.leaveReason = leaveReason;
        calculateWorkingDays();
    }
    
    // Manila timezone operations
    public LocalDateTime getCreatedDateInManila() {
        if (dateCreated == null) return null;
        return dateCreated.toLocalDateTime().atZone(ZoneId.systemDefault())
                .withZoneSameInstant(MANILA_TIMEZONE).toLocalDateTime();
    }
    
    public LocalDateTime getApprovedDateInManila() {
        if (dateApproved == null) return null;
        return dateApproved.toLocalDateTime().atZone(ZoneId.systemDefault())
                .withZoneSameInstant(MANILA_TIMEZONE).toLocalDateTime();
    }
    
    public static LocalDateTime nowInManila() {
        return LocalDateTime.now(MANILA_TIMEZONE);
    }
    
    // Conflict checking - matches database view logic
    public boolean hasConflictWithAttendance(Date attendanceDate) {
        if (leaveStart == null || leaveEnd == null || attendanceDate == null) {
            return false;
        }
        
        LocalDate leaveStartLocal = leaveStart.toLocalDate();
        LocalDate leaveEndLocal = leaveEnd.toLocalDate();
        LocalDate attendanceDateLocal = attendanceDate.toLocalDate();
        
        // Leave date conflicts with attendance if attendance date falls within leave period
        return !attendanceDateLocal.isBefore(leaveStartLocal) && 
               !attendanceDateLocal.isAfter(leaveEndLocal);
    }
    
    public List<Date> getConflictingAttendanceDates(List<Date> attendanceDates) {
        List<Date> conflicts = new ArrayList<>();
        if (attendanceDates != null) {
            for (Date attendanceDate : attendanceDates) {
                if (hasConflictWithAttendance(attendanceDate)) {
                    conflicts.add(attendanceDate);
                }
            }
        }
        return conflicts;
    }
    
    // Get all leave dates between start and end (including weekends)
    public List<Date> getAllLeaveDates() {
        List<Date> leaveDates = new ArrayList<>();
        if (leaveStart == null || leaveEnd == null) return leaveDates;
        
        LocalDate current = leaveStart.toLocalDate();
        LocalDate end = leaveEnd.toLocalDate();
        
        while (!current.isAfter(end)) {
            leaveDates.add(Date.valueOf(current));
            current = current.plusDays(1);
        }
        
        return leaveDates;
    }
    
    // Get only working day leave dates (Monday-Friday)
    public List<Date> getWorkingDayLeaveDates() {
        List<Date> workingDayDates = new ArrayList<>();
        if (leaveStart == null || leaveEnd == null) return workingDayDates;
        
        LocalDate current = leaveStart.toLocalDate();
        LocalDate end = leaveEnd.toLocalDate();
        
        while (!current.isAfter(end)) {
            DayOfWeek dayOfWeek = current.getDayOfWeek();
            if (dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY) {
                workingDayDates.add(Date.valueOf(current));
            }
            current = current.plusDays(1);
        }
        
        return workingDayDates;
    }
    
    // Calculate working days count
    private void calculateWorkingDays() {
        this.workingDaysCount = getWorkingDayLeaveDates().size();
    }
    
    // Validation methods
    public boolean isValidLeaveRequest() {
        return employeeId > 0 && leaveTypeId > 0 && 
               leaveStart != null && leaveEnd != null && 
               !leaveEnd.before(leaveStart) &&
               isValidDateRange();
    }
    
    private boolean isValidDateRange() {
        if (leaveStart == null || leaveEnd == null) return false;
        
        LocalDate startDate = leaveStart.toLocalDate();
        LocalDate endDate = leaveEnd.toLocalDate();
        LocalDate today = LocalDate.now(MANILA_TIMEZONE);
        
        // Leave cannot be in the past (except for today)
        return !startDate.isBefore(today) && !endDate.isBefore(startDate);
    }
    
    private boolean isWorkingDay(Date date) {
        if (date == null) return false;
        DayOfWeek dayOfWeek = date.toLocalDate().getDayOfWeek();
        return dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY;
    }
    
    public boolean hasWorkingDays() {
        return workingDaysCount > 0;
    }
    
    // Approval workflow
    public void approve(String supervisorNotes) {
        this.approvalStatus = ApprovalStatus.APPROVED;
        this.dateApproved = Timestamp.valueOf(LocalDateTime.now(MANILA_TIMEZONE));
        this.supervisorNotes = supervisorNotes;
    }
    
    public void reject(String supervisorNotes) {
        this.approvalStatus = ApprovalStatus.REJECTED;
        this.dateApproved = Timestamp.valueOf(LocalDateTime.now(MANILA_TIMEZONE));
        this.supervisorNotes = supervisorNotes;
    }
    
    public boolean isPending() {
        return approvalStatus == ApprovalStatus.PENDING;
    }
    
    public boolean isApproved() {
        return approvalStatus == ApprovalStatus.APPROVED;
    }
    
    public boolean isRejected() {
        return approvalStatus == ApprovalStatus.REJECTED;
    }
    
    // Business logic methods
    public boolean canBeModified() {
        return isPending();
    }
    
    public boolean canBeCancelled() {
        return isPending() || (isApproved() && leaveStart.toLocalDate().isAfter(LocalDate.now(MANILA_TIMEZONE)));
    }
    
    public String getLeaveStatusDisplayText() {
        return switch (approvalStatus) {
            case APPROVED -> "Approved";
            case REJECTED -> "Rejected";
            default -> "Pending";
        };
    }
    
    // Getters and Setters
    public int getLeaveRequestId() { return leaveRequestId; }
    public void setLeaveRequestId(int leaveRequestId) { this.leaveRequestId = leaveRequestId; }
    
    public int getEmployeeId() { return employeeId; }
    public void setEmployeeId(int employeeId) { this.employeeId = employeeId; }
    
    public int getLeaveTypeId() { return leaveTypeId; }
    public void setLeaveTypeId(int leaveTypeId) { this.leaveTypeId = leaveTypeId; }
    
    public Date getLeaveStart() { return leaveStart; }
    public void setLeaveStart(Date leaveStart) { 
        this.leaveStart = leaveStart; 
        calculateWorkingDays();
    }
    
    public Date getLeaveEnd() { return leaveEnd; }
    public void setLeaveEnd(Date leaveEnd) { 
        this.leaveEnd = leaveEnd; 
        calculateWorkingDays();
    }
    
    public String getLeaveReason() { return leaveReason; }
    public void setLeaveReason(String leaveReason) { this.leaveReason = leaveReason; }
    
    public ApprovalStatus getApprovalStatus() { return approvalStatus; }
    public void setApprovalStatus(ApprovalStatus approvalStatus) { this.approvalStatus = approvalStatus; }
    
    public Timestamp getDateCreated() { return dateCreated; }
    public void setDateCreated(Timestamp dateCreated) { this.dateCreated = dateCreated; }
    
    public Timestamp getDateApproved() { return dateApproved; }
    public void setDateApproved(Timestamp dateApproved) { this.dateApproved = dateApproved; }
    
    public String getSupervisorNotes() { return supervisorNotes; }
    public void setSupervisorNotes(String supervisorNotes) { this.supervisorNotes = supervisorNotes; }
    
    public boolean isHasAttendanceConflict() { return hasAttendanceConflict; }
    public void setHasAttendanceConflict(boolean hasAttendanceConflict) { this.hasAttendanceConflict = hasAttendanceConflict; }
    
    public int getWorkingDaysCount() { return workingDaysCount; }
    public void setWorkingDaysCount(int workingDaysCount) { this.workingDaysCount = workingDaysCount; }
    
    @Override
    public String toString() {
        return String.format("LeaveRequestModel{leaveRequestId=%d, employeeId=%d, leaveTypeId=%d, leaveStart=%s, leaveEnd=%s, approvalStatus=%s, workingDays=%d}",
                leaveRequestId, employeeId, leaveTypeId, leaveStart, leaveEnd, approvalStatus, workingDaysCount);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        LeaveRequestModel that = (LeaveRequestModel) obj;
        return leaveRequestId == that.leaveRequestId;
    }
    
    @Override
    public int hashCode() {
        return Integer.hashCode(leaveRequestId);
    }
}