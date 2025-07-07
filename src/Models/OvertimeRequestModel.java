package Models;

import java.time.*;
import java.sql.Timestamp;

public class OvertimeRequestModel {
    private static final ZoneId MANILA_TIMEZONE = ZoneId.of("Asia/Manila");
    private static final LocalTime STANDARD_END_TIME = LocalTime.of(17, 0); // 5:00 PM
    private static final double MAX_OVERTIME_HOURS_PER_DAY = 4.0;
    
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
    
    private int overtimeRequestId;
    private int employeeId;
    private LocalDateTime overtimeStart;        // Changed from Timestamp to LocalDateTime
    private LocalDateTime overtimeEnd;          // Changed from Timestamp to LocalDateTime
    private String overtimeReason;
    private ApprovalStatus approvalStatus;
    private LocalDateTime dateCreated;          // Changed from Timestamp to LocalDateTime
    private LocalDateTime dateApproved;         // Changed from Timestamp to LocalDateTime
    private String supervisorNotes;
    private double overtimeHours;
    private double overtimePay;
    private double hourlyRate;
    
    // Constructors
    public OvertimeRequestModel() {
        this.approvalStatus = ApprovalStatus.PENDING;
        this.dateCreated = LocalDateTime.now(MANILA_TIMEZONE);
    }
    
    public OvertimeRequestModel(int employeeId, LocalDateTime overtimeStart, LocalDateTime overtimeEnd, String overtimeReason) {
        this();
        this.employeeId = employeeId;
        this.overtimeStart = overtimeStart;
        this.overtimeEnd = overtimeEnd;
        this.overtimeReason = overtimeReason;
        calculateOvertimeHours();
    }
    
    // Manila timezone validation and operations
    public LocalDateTime getOvertimeStartInManila() {
        if (overtimeStart == null) return null;
        return overtimeStart.atZone(ZoneId.systemDefault())
                .withZoneSameInstant(MANILA_TIMEZONE).toLocalDateTime();
    }
    
    public LocalDateTime getOvertimeEndInManila() {
        if (overtimeEnd == null) return null;
        return overtimeEnd.atZone(ZoneId.systemDefault())
                .withZoneSameInstant(MANILA_TIMEZONE).toLocalDateTime();
    }
    
    public LocalDateTime getCreatedDateInManila() {
        if (dateCreated == null) return null;
        return dateCreated.atZone(ZoneId.systemDefault())
                .withZoneSameInstant(MANILA_TIMEZONE).toLocalDateTime();
    }
    
    public LocalDateTime getApprovedDateInManila() {
        if (dateApproved == null) return null;
        return dateApproved.atZone(ZoneId.systemDefault())
                .withZoneSameInstant(MANILA_TIMEZONE).toLocalDateTime();
    }
    
    public static LocalDateTime nowInManila() {
        return LocalDateTime.now(MANILA_TIMEZONE);
    }
    
    // Calculate overtime hours
    public final void calculateOvertimeHours() {
        if (overtimeStart != null && overtimeEnd != null) {
            LocalDateTime start = getOvertimeStartInManila();
            LocalDateTime end = getOvertimeEndInManila();
            Duration duration = Duration.between(start, end);
            this.overtimeHours = Math.round(duration.toMinutes() / 60.0 * 100.0) / 100.0; // Round to 2 decimal places
        }
    }
    
    // Calculate overtime pay (usually 1.25x regular rate)
    public void calculateOvertimePay(double hourlyRate) {
        this.hourlyRate = hourlyRate;
        this.overtimePay = this.overtimeHours * hourlyRate * 1.25; // 1.25x multiplier for overtime
    }
    
    // Approval workflow
    public void approve(String supervisorNotes) {
        this.approvalStatus = ApprovalStatus.APPROVED;
        this.dateApproved = LocalDateTime.now(MANILA_TIMEZONE);
        this.supervisorNotes = supervisorNotes;
    }
    
    public void reject(String supervisorNotes) {
        this.approvalStatus = ApprovalStatus.REJECTED;
        this.dateApproved = LocalDateTime.now(MANILA_TIMEZONE);
        this.supervisorNotes = supervisorNotes;
    }
    
    // Status check methods
    public boolean isPending() {
        return approvalStatus == ApprovalStatus.PENDING;
    }
    
    public boolean isApproved() {
        return approvalStatus == ApprovalStatus.APPROVED;
    }
    
    public boolean isRejected() {
        return approvalStatus == ApprovalStatus.REJECTED;
    }
    
    // Validation methods
    public boolean isValidOvertimeRequest() {
        if (employeeId <= 0 || overtimeStart == null || overtimeEnd == null) {
            return false;
        }
        
        LocalDateTime start = getOvertimeStartInManila();
        LocalDateTime end = getOvertimeEndInManila();
        
        // Basic validation: end must be after start
        if (!end.isAfter(start)) {
            return false;
        }
        
        // Must be on the same day
        if (!start.toLocalDate().equals(end.toLocalDate())) {
            return false;
        }
        
        // Overtime must start at or after standard end time (5 PM)
        if (start.toLocalTime().isBefore(STANDARD_END_TIME)) {
            return false;
        }
        
        // Check maximum overtime hours per day
        if (overtimeHours > MAX_OVERTIME_HOURS_PER_DAY) {
            return false;
        }
        
        return isWithinWorkingDay() && isReasonableTimeFrame();
    }
    
    public boolean isWithinWorkingDay() {
        if (overtimeStart == null) return false;
        LocalDateTime start = getOvertimeStartInManila();
        DayOfWeek dayOfWeek = start.getDayOfWeek();
        return dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY;
    }
    
    private boolean isReasonableTimeFrame() {
        if (overtimeStart == null || overtimeEnd == null) return false;
        
        LocalDateTime end = getOvertimeEndInManila();
        
        // Overtime should not end too late (before 11 PM)
        LocalTime maxEndTime = LocalTime.of(23, 0);
        return !end.toLocalTime().isAfter(maxEndTime);
    }
    
    // Business logic methods
    public boolean canBeModified() {
        return isPending();
    }
    
    public boolean canBeCancelled() {
        LocalDateTime now = LocalDateTime.now(MANILA_TIMEZONE);
        LocalDateTime overtimeDateTime = getOvertimeStartInManila();
        
        // Can cancel if pending or if overtime hasn't started yet
        return isPending() || (isApproved() && overtimeDateTime != null && overtimeDateTime.isAfter(now));
    }
    
    public boolean requiresHigherApproval() {
        // Require higher approval for overtime over 3 hours
        return overtimeHours > 3.0;
    }
    
    public String getOvertimeStatusDisplayText() {
        return switch (approvalStatus) {
            case APPROVED -> "Approved";
            case REJECTED -> "Rejected";
            default -> "Pending";
        };
    }
    
    public String getFormattedOvertimeHours() {
        return String.format("%.2f", overtimeHours);
    }
    
    public String getFormattedOvertimePay() {
        return String.format("%.2f", overtimePay);
    }
    
    // Check if overtime conflicts with existing attendance
    public boolean conflictsWithAttendance(Object attendance) { // Changed parameter type
        // Implementation would depend on your AttendanceModel structure
        return false;
    }
    
    // Getters and Setters
    public int getOvertimeRequestId() { return overtimeRequestId; }
    public void setOvertimeRequestId(int overtimeRequestId) { this.overtimeRequestId = overtimeRequestId; }
    
    public int getEmployeeId() { return employeeId; }
    public void setEmployeeId(int employeeId) { this.employeeId = employeeId; }
    
    public LocalDateTime getOvertimeStart() { return overtimeStart; }
    public void setOvertimeStart(LocalDateTime overtimeStart) { 
        this.overtimeStart = overtimeStart; 
        calculateOvertimeHours();
    }
    
    public LocalDateTime getOvertimeEnd() { return overtimeEnd; }
    public void setOvertimeEnd(LocalDateTime overtimeEnd) { 
        this.overtimeEnd = overtimeEnd; 
        calculateOvertimeHours();
    }
    
    public String getOvertimeReason() { return overtimeReason; }
    public void setOvertimeReason(String overtimeReason) { this.overtimeReason = overtimeReason; }
    
    public ApprovalStatus getApprovalStatus() { return approvalStatus; }
    public void setApprovalStatus(ApprovalStatus approvalStatus) { this.approvalStatus = approvalStatus; }
    
    public LocalDateTime getDateCreated() { return dateCreated; }
    public void setDateCreated(LocalDateTime dateCreated) { this.dateCreated = dateCreated; }
    
    public LocalDateTime getDateApproved() { return dateApproved; }
    public void setDateApproved(LocalDateTime dateApproved) { this.dateApproved = dateApproved; }
    
    public String getSupervisorNotes() { return supervisorNotes; }
    public void setSupervisorNotes(String supervisorNotes) { this.supervisorNotes = supervisorNotes; }
    
    public double getOvertimeHours() { return overtimeHours; }
    public void setOvertimeHours(double overtimeHours) { this.overtimeHours = overtimeHours; }
    
    public double getOvertimePay() { return overtimePay; }
    public void setOvertimePay(double overtimePay) { this.overtimePay = overtimePay; }
    
    public double getHourlyRate() { return hourlyRate; }
    public void setHourlyRate(double hourlyRate) { 
        this.hourlyRate = hourlyRate; 
        if (hourlyRate > 0) {
            calculateOvertimePay(hourlyRate);
        }
    }
    
    // Backward compatibility methods for Timestamp (if needed)
    public Timestamp getOvertimeStartAsTimestamp() {
        return overtimeStart != null ? Timestamp.valueOf(overtimeStart) : null;
    }
    
    public void setOvertimeStart(Timestamp overtimeStart) {
        this.overtimeStart = overtimeStart != null ? overtimeStart.toLocalDateTime() : null;
        calculateOvertimeHours();
    }
    
    public Timestamp getOvertimeEndAsTimestamp() {
        return overtimeEnd != null ? Timestamp.valueOf(overtimeEnd) : null;
    }
    
    public void setOvertimeEnd(Timestamp overtimeEnd) {
        this.overtimeEnd = overtimeEnd != null ? overtimeEnd.toLocalDateTime() : null;
        calculateOvertimeHours();
    }
    
    public Timestamp getDateCreatedAsTimestamp() {
        return dateCreated != null ? Timestamp.valueOf(dateCreated) : null;
    }
    
    public void setDateCreated(Timestamp dateCreated) {
        this.dateCreated = dateCreated != null ? dateCreated.toLocalDateTime() : null;
    }
    
    public Timestamp getDateApprovedAsTimestamp() {
        return dateApproved != null ? Timestamp.valueOf(dateApproved) : null;
    }
    
    public void setDateApproved(Timestamp dateApproved) {
        this.dateApproved = dateApproved != null ? dateApproved.toLocalDateTime() : null;
    }
    
    @Override
    public String toString() {
        return String.format("OvertimeRequestModel{overtimeRequestId=%d, employeeId=%d, start=%s, end=%s, hours=%.2f, status=%s, pay=%.2f}",
                overtimeRequestId, employeeId, overtimeStart, overtimeEnd, overtimeHours, approvalStatus, overtimePay);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        OvertimeRequestModel that = (OvertimeRequestModel) obj;
        return overtimeRequestId == that.overtimeRequestId;
    }
    
    @Override
    public int hashCode() {
        return Integer.hashCode(overtimeRequestId);
    }
}