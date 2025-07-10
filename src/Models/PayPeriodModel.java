package Models;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * PayPeriodModel class that maps to the payperiod table
 * Fields: payPeriodId, startDate, endDate, periodName
 * Handles payroll cycles with Manila timezone support
 * @author User
 */
public class PayPeriodModel {
    
    // Manila timezone constant
    public static final ZoneId MANILA_TIMEZONE = ZoneId.of("Asia/Manila");
    
    private Integer payPeriodId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String periodName;
    
    // Enum for period status (if needed for future enhancement)
    public enum PeriodStatus {
        ACTIVE("Active"),
        CLOSED("Closed"),
        PROCESSING("Processing");
        
        private final String displayName;
        
        PeriodStatus(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    // Constructors
    public PayPeriodModel() {}
    
    public PayPeriodModel(LocalDate startDate, LocalDate endDate, String periodName) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.periodName = periodName;
    }
    
    public PayPeriodModel(Integer payPeriodId, LocalDate startDate, LocalDate endDate, String periodName) {
        this.payPeriodId = payPeriodId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.periodName = periodName;
    }
    
    // Getters and Setters
    public Integer getPayPeriodId() { return payPeriodId; }
    public void setPayPeriodId(Integer payPeriodId) { this.payPeriodId = payPeriodId; }
    
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    
    public String getPeriodName() { return periodName; }
    public void setPeriodName(String periodName) { this.periodName = periodName; }
    
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
     * Calculate the number of working days in the pay period (Monday to Friday only)
     * Excludes weekends as per business rules
     * @return 
     */
    public int getWorkingDays() {
        if (startDate == null || endDate == null) {
            return 0;
        }
        
        int workingDays = 0;
        LocalDate current = startDate;
        
        while (!current.isAfter(endDate)) {
            // Monday = 1, Sunday = 7 (only count Monday to Friday)
            if (current.getDayOfWeek().getValue() <= 5) {
                workingDays++;
            }
            current = current.plusDays(1);
        }
        
        return workingDays;
    }
    
    /**
     * Calculate total calendar days in the pay period
     * @return 
     */
    public int getTotalDays() {
        if (startDate == null || endDate == null) {
            return 0;
        }
        
        return (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }
    
    /**
     * Calculate total weekdays in the pay period (Monday to Friday only)
     * This is the key method for absence calculations
     * @return 
     */
    public int getTotalWeekdays() {
        return getWorkingDays(); // Same as working days since we exclude weekends
    }
    
    /**
     * Check if a date falls within this pay period
     * @param date
     * @return 
     */
    public boolean containsDate(LocalDate date) {
        if (startDate == null || endDate == null || date == null) {
            return false;
        }
        
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }
    
    /**
     * Check if this pay period overlaps with another
     * @param other
     * @return 
     */
    public boolean overlapsWith(PayPeriodModel other) {
        if (other == null || startDate == null || endDate == null || 
            other.startDate == null || other.endDate == null) {
            return false;
        }
        
        return !(endDate.isBefore(other.startDate) || startDate.isAfter(other.endDate));
    }
    
    /**
     * Check if this is a current pay period (contains today's date in Manila timezone)
     * @return 
     */
    public boolean isCurrent() {
        return containsDate(getCurrentDateManila());
    }
    
    /**
     * Check if this is a future pay period (based on Manila timezone)
     * @return 
     */
    public boolean isFuture() {
        LocalDate today = getCurrentDateManila();
        return startDate != null && startDate.isAfter(today);
    }
    
    /**
     * Check if this is a past pay period (based on Manila timezone)
     * @return 
     */
    public boolean isPast() {
        LocalDate today = getCurrentDateManila();
        return endDate != null && endDate.isBefore(today);
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
     * Get all weekdays within this pay period
     * @return 
     */
    public java.util.List<LocalDate> getWeekdaysInPeriod() {
        java.util.List<LocalDate> weekdays = new java.util.ArrayList<>();
        
        if (startDate == null || endDate == null) {
            return weekdays;
        }
        
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            if (isWeekday(current)) {
                weekdays.add(current);
            }
            current = current.plusDays(1);
        }
        
        return weekdays;
    }
    
    /**
     * Check if this pay period is open for attendance submission
     * (within current pay period or future, based on Manila timezone)
     * @return 
     */
    public boolean isOpenForAttendance() {
        LocalDate today = getCurrentDateManila();
        return containsDate(today) || isFuture();
    }
    
    /**
     * Check if this pay period is open for leave requests
     * (current or future periods only, based on Manila timezone)
     * @return 
     */
    public boolean isOpenForLeaveRequests() {
        LocalDate today = getCurrentDateManila();
        return !isPast() && (containsDate(today) || isFuture());
    }
    
    /**
     * Get formatted period string
     * @return 
     */
    public String getFormattedPeriod() {
        if (startDate == null || endDate == null) {
            return periodName != null ? periodName : "Invalid Period";
        }
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
        return String.format("%s - %s", 
            startDate.format(formatter), 
            endDate.format(formatter)
        );
    }
    
    /**
     * Get short formatted period string
     * @return 
     */
    public String getShortFormattedPeriod() {
        if (startDate == null || endDate == null) {
            return periodName != null ? periodName : "Invalid Period";
        }
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yy");
        return String.format("%s - %s", 
            startDate.format(formatter), 
            endDate.format(formatter)
        );
    }
    
    /**
     * Validate pay period data
     * @return 
     */
    public boolean isValid() {
        if (startDate == null || endDate == null) {
            return false;
        }
        
        if (startDate.isAfter(endDate)) {
            return false;
        }
        
        if (periodName == null || periodName.trim().isEmpty()) {
            return false;
        }
        
        return periodName.length() <= 30;
    }
    
    /**
     * Calculate period length in weeks
     * @return 
     */
    public double getPeriodLengthInWeeks() {
        if (startDate == null || endDate == null) {
            return 0.0;
        }
        
        long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        return days / 7.0;
    }
    
    /**
     * Check if this is a bi-weekly pay period (approximately 14 days)
     * @return 
     */
    public boolean isBiWeekly() {
        int totalDays = getTotalDays();
        return totalDays >= 13 && totalDays <= 15;
    }
    
    /**
     * Check if this is a monthly pay period (approximately 28-31 days)
     * @return 
     */
    public boolean isMonthly() {
        int totalDays = getTotalDays();
        return totalDays >= 28 && totalDays <= 31;
    }
    
    /**
     * Get period type description
     * @return 
     */
    public String getPeriodType() {
        if (isBiWeekly()) {
            return "Bi-Weekly";
        } else if (isMonthly()) {
            return "Monthly";
        } else {
            return "Custom (" + getTotalDays() + " days)";
        }
    }
    
    /**
     * Generate automatic period name based on dates
     * @return 
     */
    public String generateAutomaticPeriodName() {
        if (startDate == null || endDate == null) {
            return "Unknown Period";
        }
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd");
        int year = startDate.getYear();
        
        if (isBiWeekly()) {
            LocalDate yearStart = LocalDate.of(year, 1, 1);
            long weeksSinceYearStart = ChronoUnit.WEEKS.between(yearStart, startDate);
            int periodNumber = (int) (weeksSinceYearStart / 2) + 1;
            
            return String.format("%d-P%02d (%s to %s)", 
                year, periodNumber,
                startDate.format(formatter),
                endDate.format(formatter)
            );
        } else if (isMonthly()) {
            return String.format("%d-%02d (%s)", 
                year, startDate.getMonthValue(),
                startDate.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
            );
        } else {
            return String.format("%s to %s", 
                startDate.format(formatter),
                endDate.format(formatter)
            );
        }
    }
    
    /**
     * Get period statistics for payroll processing
     * @return 
     */
    public String getPeriodStatistics() {
        return String.format("Total Days: %d, Working Days: %d, Weekends: %d", 
            getTotalDays(), getWorkingDays(), getTotalDays() - getWorkingDays());
    }
    
    @Override
    public String toString() {
        return String.format("PayPeriodModel{payPeriodId=%d, startDate=%s, endDate=%s, periodName='%s'}", 
                           payPeriodId, startDate, endDate, periodName);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        PayPeriodModel payPeriod = (PayPeriodModel) obj;
        return Objects.equals(payPeriodId, payPeriod.payPeriodId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(payPeriodId);
    }
}