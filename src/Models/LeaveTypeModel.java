package Models;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

/**
 * LeaveTypeModel class that maps to the leavetype table
 * Fields: leaveTypeId, leaveTypeName, maxDays, description
 * Includes leave validation rules and business logic
 * @author User
 */
public class LeaveTypeModel {
    
    // Manila timezone constant
    public static final ZoneId MANILA_TIMEZONE = ZoneId.of("Asia/Manila");
    
    private Integer leaveTypeId;
    private String leaveTypeName;
    private String leaveDescription;
    private Integer maxDaysPerYear;
    private LocalDateTime createdAt;
    
    // Constructors
    public LeaveTypeModel() {}
    
    public LeaveTypeModel(String leaveTypeName, String leaveDescription, Integer maxDaysPerYear) {
        this.leaveTypeName = leaveTypeName;
        this.leaveDescription = leaveDescription;
        this.maxDaysPerYear = maxDaysPerYear;
    }
    
    public LeaveTypeModel(Integer leaveTypeId, String leaveTypeName, String leaveDescription, 
                         Integer maxDaysPerYear, LocalDateTime createdAt) {
        this.leaveTypeId = leaveTypeId;
        this.leaveTypeName = leaveTypeName;
        this.leaveDescription = leaveDescription;
        this.maxDaysPerYear = maxDaysPerYear;
        this.createdAt = createdAt;
    }
    
    // Getters and Setters
    public Integer getLeaveTypeId() { return leaveTypeId; }
    public void setLeaveTypeId(Integer leaveTypeId) { this.leaveTypeId = leaveTypeId; }
    
    public String getLeaveTypeName() { return leaveTypeName; }
    public void setLeaveTypeName(String leaveTypeName) { this.leaveTypeName = leaveTypeName; }
    
    public String getLeaveDescription() { return leaveDescription; }
    public void setLeaveDescription(String leaveDescription) { this.leaveDescription = leaveDescription; }
    
    public Integer getMaxDaysPerYear() { return maxDaysPerYear; }
    public void setMaxDaysPerYear(Integer maxDaysPerYear) { this.maxDaysPerYear = maxDaysPerYear; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    // Business Methods
    
    /**
     * Get current date in Manila timezone
     * @return 
     */
    public static LocalDate getCurrentDateManila() {
        return LocalDateTime.now(MANILA_TIMEZONE).toLocalDate();
    }
    
    /**
     * Get current date time in Manila timezone
     * @return 
     */
    public static LocalDateTime getCurrentDateTimeManila() {
        return LocalDateTime.now(MANILA_TIMEZONE);
    }
    
    /**
     * Validate leave request dates according to business rules
     * Employee can only submit leave request dated today or onwards (Manila Time)
     * @param leaveStartDate
     * @param leaveEndDate
     * @return 
     */
    public static boolean isValidLeaveRequestDate(LocalDate leaveStartDate, LocalDate leaveEndDate) {
        if (leaveStartDate == null || leaveEndDate == null) {
            return false;
        }
        
        LocalDate today = getCurrentDateManila();
        
        // Leave start date must be today or in the future
        if (leaveStartDate.isBefore(today)) {
            return false;
        }
        
        // Leave end date must be on or after start date
        if (leaveEndDate.isBefore(leaveStartDate)) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Calculate number of weekdays in a leave period
     * Only Monday-Friday count as leave days
     * @param leaveStartDate
     * @param leaveEndDate
     * @return 
     */
    public static int calculateWeekdaysInLeave(LocalDate leaveStartDate, LocalDate leaveEndDate) {
        if (leaveStartDate == null || leaveEndDate == null) {
            return 0;
        }
        
        if (leaveStartDate.isAfter(leaveEndDate)) {
            return 0;
        }
        
        int weekdays = 0;
        LocalDate current = leaveStartDate;
        
        while (!current.isAfter(leaveEndDate)) {
            // Monday = 1, Sunday = 7 (count only Monday to Friday)
            if (current.getDayOfWeek().getValue() <= 5) {
                weekdays++;
            }
            current = current.plusDays(1);
        }
        
        return weekdays;
    }
    
    /**
     * Check if there's a leave-attendance conflict
     * If leave date = attendance date → count as worked day, NOT leave
     * @param leaveDate
     * @param attendanceDates
     * @return true if there's a conflict
     */
    public static boolean hasLeaveAttendanceConflict(LocalDate leaveDate, List<LocalDate> attendanceDates) {
        if (leaveDate == null || attendanceDates == null) {
            return false;
        }
        
        return attendanceDates.contains(leaveDate);
    }
    
    /**
     * Filter out leave dates that conflict with attendance
     * @param leaveDates
     * @param attendanceDates
     * @return 
     */
    public static List<LocalDate> filterValidLeaveDates(List<LocalDate> leaveDates, List<LocalDate> attendanceDates) {
        if (leaveDates == null) {
            return new java.util.ArrayList<>();
        }
        
        List<LocalDate> validLeaveDates = new java.util.ArrayList<>();
        
        for (LocalDate leaveDate : leaveDates) {
            // Only count as leave if it's a weekday and doesn't conflict with attendance
            if (isWeekday(leaveDate) && !hasLeaveAttendanceConflict(leaveDate, attendanceDates)) {
                validLeaveDates.add(leaveDate);
            }
        }
        
        return validLeaveDates;
    }
    
    /**
     * Check if a date is a weekday (Monday to Friday)
     * @param date
     * @return 
     */
    public static boolean isWeekday(LocalDate date) {
        if (date == null) return false;
        return date.getDayOfWeek().getValue() <= 5; // Monday=1, Friday=5
    }
    
    /**
     * Check if a date is a weekend (Saturday or Sunday)
     * @param date
     * @return 
     */
    public static boolean isWeekend(LocalDate date) {
        if (date == null) return false;
        return date.getDayOfWeek().getValue() > 5; // Saturday=6, Sunday=7
    }
    
    /**
     * Check if leave type has unlimited days (null or negative maxDaysPerYear)
     * @return 
     */
    public boolean isUnlimited() {
        return maxDaysPerYear == null || maxDaysPerYear <= 0;
    }
    
    /**
     * Check if a requested number of days is within the annual limit
     * @param requestedWeekdays
     * @param alreadyUsedDays
     * @return 
     */
    public boolean isWithinAnnualLimit(int requestedWeekdays, int alreadyUsedDays) {
        if (isUnlimited()) {
            return true; // No limit
        }
        
        return (alreadyUsedDays + requestedWeekdays) <= maxDaysPerYear;
    }
    
    /**
     * Calculate remaining days available for the year
     * @param alreadyUsedDays
     * @return 
     */
    public int getRemainingDays(int alreadyUsedDays) {
        if (isUnlimited()) {
            return Integer.MAX_VALUE; // Unlimited
        }
        
        int remaining = maxDaysPerYear - alreadyUsedDays;
        return Math.max(0, remaining); // Don't return negative
    }
    
    /**
     * Validate leave request against all business rules
     * @param leaveStartDate
     * @param leaveEndDate
     * @param alreadyUsedDays
     * @param attendanceDates
     * @return 
     */
    public boolean isValidLeaveRequest(LocalDate leaveStartDate, LocalDate leaveEndDate, 
                                      int alreadyUsedDays, List<LocalDate> attendanceDates) {
        // Check date validity
        if (!isValidLeaveRequestDate(leaveStartDate, leaveEndDate)) {
            return false;
        }
        
        // Calculate weekdays in leave period
        int requestedWeekdays = calculateWeekdaysInLeave(leaveStartDate, leaveEndDate);
        
        // Check if within annual limit
        if (!isWithinAnnualLimit(requestedWeekdays, alreadyUsedDays)) {
            return false;
        }
        
        // Additional validation can be added here
        return true;
    }
    
    /**
     * Get leave request validation messages
     * @param leaveStartDate
     * @param leaveEndDate
     * @param alreadyUsedDays
     * @return 
     */
    public String getLeaveValidationMessage(LocalDate leaveStartDate, LocalDate leaveEndDate, int alreadyUsedDays) {
        if (!isValidLeaveRequestDate(leaveStartDate, leaveEndDate)) {
            return "Leave dates must be today or in the future (Manila time)";
        }
        
        int requestedWeekdays = calculateWeekdaysInLeave(leaveStartDate, leaveEndDate);
        
        if (!isWithinAnnualLimit(requestedWeekdays, alreadyUsedDays)) {
            return String.format("Request exceeds annual limit. You have %d days remaining.", 
                               getRemainingDays(alreadyUsedDays));
        }
        
        return "Valid leave request";
    }
    
    /**
     * Check if leave type is for paid leave
     * @return 
     */
    public boolean isPaidLeave() {
        // Most leave types are paid - this could be enhanced with a database flag
        String lowerName = leaveTypeName != null ? leaveTypeName.toLowerCase() : "";
        return !lowerName.contains("unpaid") && !lowerName.contains("lwop");
    }
    
    /**
     * Get formatted description with max days info
     * @return 
     */
    public String getFormattedDescription() {
        StringBuilder description = new StringBuilder();
        
        if (leaveDescription != null && !leaveDescription.trim().isEmpty()) {
            description.append(leaveDescription);
        }
        
        if (maxDaysPerYear != null && maxDaysPerYear > 0) {
            if (description.length() > 0) {
                description.append(" - ");
            }
            description.append("Max ").append(maxDaysPerYear).append(" days per year");
        } else {
            if (description.length() > 0) {
                description.append(" - ");
            }
            description.append("No annual limit");
        }
        
        return description.toString();
    }
    
    /**
     * Validate leave type data
     * @return 
     */
    public boolean isValid() {
        if (leaveTypeName == null || leaveTypeName.trim().isEmpty()) {
            return false;
        }
        
        if (leaveTypeName.length() > 50) { // Based on database constraint
            return false;
        }
        
        return !(leaveDescription != null && leaveDescription.length() > 255);
    }
    
    /**
     * Check if this leave type allows partial days
     * @return 
     */
    public boolean allowsPartialDays() {
        // This could be extended to include a flag in the database
        // For now, assume all leave types allow partial days
        return true;
    }
    
    /**
     * Get display name with limits
     * @return 
     */
    public String getDisplayNameWithLimits() {
        if (isUnlimited()) {
            return leaveTypeName + " (Unlimited)";
        } else {
            return leaveTypeName + " (Max: " + maxDaysPerYear + " days/year)";
        }
    }
    
    /**
     * Calculate percentage of annual allowance used
     * @param usedDays
     * @return 
     */
    public double getUsagePercentage(int usedDays) {
        if (isUnlimited() || maxDaysPerYear == 0) {
            return 0.0;
        }
        
        return (double) usedDays / maxDaysPerYear * 100.0;
    }
    
    /**
     * Check if usage is approaching the limit (within 80%)
     * @param usedDays
     * @return 
     */
    public boolean isApproachingLimit(int usedDays) {
        if (isUnlimited()) {
            return false;
        }
        
        return getUsagePercentage(usedDays) >= 80.0;
    }
    
    /**
     * Check if the annual limit has been exceeded
     * @param usedDays
     * @return 
     */
    public boolean isLimitExceeded(int usedDays) {
        if (isUnlimited()) {
            return false;
        }
        
        return usedDays > maxDaysPerYear;
    }
    
    /**
     * Get leave type priority (for processing order)
     * @return 
     */
    public int getLeavePriority() {
        String lowerName = leaveTypeName != null ? leaveTypeName.toLowerCase() : "";
        
        if (lowerName.contains("vacation") || lowerName.contains("annual")) {
            return 1;
        } else if (lowerName.contains("sick")) {
            return 2;
        } else if (lowerName.contains("emergency")) {
            return 3;
        } else if (lowerName.contains("maternity") || lowerName.contains("paternity")) {
            return 4;
        } else {
            return 5;
        }
    }
    
    /**
     * Check if leave requires medical certificate
     * @return 
     */
    public boolean requiresMedicalCertificate() {
        String lowerName = leaveTypeName != null ? leaveTypeName.toLowerCase() : "";
        return lowerName.contains("sick") || lowerName.contains("medical");
    }
    
    /**
     * Get formatted leave duration
     * @param days
     * @return 
     */
    public String formatLeaveDuration(int days) {
        if (days == 1) {
            return "1 day";
        } else {
            return days + " days";
        }
    }
    
    @Override
    public String toString() {
        return String.format("LeaveTypeModel{leaveTypeId=%d, leaveTypeName='%s', leaveDescription='%s', maxDaysPerYear=%s, createdAt=%s}", 
                           leaveTypeId, leaveTypeName, leaveDescription, maxDaysPerYear, createdAt);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        LeaveTypeModel leaveType = (LeaveTypeModel) obj;
        return Objects.equals(leaveTypeId, leaveType.leaveTypeId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(leaveTypeId);
    }
}