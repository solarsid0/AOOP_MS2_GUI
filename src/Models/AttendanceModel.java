package Models;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.sql.Date;
import java.sql.Time;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class AttendanceModel {
    private static final ZoneId MANILA_TIMEZONE = ZoneId.of("Asia/Manila");
    private static final LocalTime GRACE_PERIOD_CUTOFF = LocalTime.of(8, 10); // 8:10 AM grace period
    private static final LocalTime STANDARD_START_TIME = LocalTime.of(8, 0);  // 8:00 AM standard start
    private static final LocalTime STANDARD_END_TIME = LocalTime.of(17, 0);   // 5:00 PM standard end
    
    private Integer attendanceId; // Changed to Integer to allow null
    private Date date;
    private Time timeIn;
    private Time timeOut;
    private int employeeId;
    private double computedHours;
    private double computedAmount;
    private boolean isLate;
    private double lateHours;
    private double overtimeHours;
    
    // Constructors
    public AttendanceModel() {}
    
    public AttendanceModel(Date date, Time timeIn, Time timeOut, int employeeId) {
        this.date = date;
        this.timeIn = timeIn;
        this.timeOut = timeOut;
        this.employeeId = employeeId;
        calculateAttendanceMetrics();
    }
    
    // Manila timezone operations
    public LocalDateTime getDateTimeInManila() {
        if (date == null) return null;
        return date.toLocalDate().atStartOfDay().atZone(ZoneId.systemDefault())
                .withZoneSameInstant(MANILA_TIMEZONE).toLocalDateTime();
    }
    
    public LocalTime getTimeInManila() {
        if (timeIn == null) return null;
        return timeIn.toLocalTime();
    }
    
    public LocalTime getTimeOutManila() {
        if (timeOut == null) return null;
        return timeOut.toLocalTime();
    }
    
    public static LocalDateTime nowInManila() {
        return LocalDateTime.now(MANILA_TIMEZONE);
    }
    
    public static Date getCurrentDateInManila() {
        return Date.valueOf(LocalDate.now(MANILA_TIMEZONE));
    }
    
    public static Time getCurrentTimeInManila() {
        return Time.valueOf(LocalTime.now(MANILA_TIMEZONE));
    }
    
    // Grace period logic
    public boolean isWithinGracePeriod() {
        if (timeIn == null) return false;
        LocalTime actualTimeIn = timeIn.toLocalTime();
        return actualTimeIn.isAfter(STANDARD_START_TIME) && 
               !actualTimeIn.isAfter(GRACE_PERIOD_CUTOFF);
    }
    
    public boolean isLateAttendance() {
        if (timeIn == null) return false;
        return timeIn.toLocalTime().isAfter(GRACE_PERIOD_CUTOFF);
    }
    
    public boolean isEarlyOut() {
        if (timeOut == null) return false;
        return timeOut.toLocalTime().isBefore(STANDARD_END_TIME);
    }
    
    // Calculate attendance metrics based on database view logic
    public final void calculateAttendanceMetrics() {
        if (timeIn != null && timeOut != null) {
            LocalTime in = timeIn.toLocalTime();
            LocalTime out = timeOut.toLocalTime();
            
            // Calculate total hours worked (minus 1 hour lunch break)
            Duration workDuration = Duration.between(in, out);
            double totalHours = workDuration.toMinutes() / 60.0;
            this.computedHours = Math.max(0, totalHours - 1.0); // Subtract lunch break
            
            // Calculate late hours
            if (isLateAttendance()) {
                this.isLate = true;
                Duration lateDuration = Duration.between(STANDARD_START_TIME, in);
                this.lateHours = lateDuration.toMinutes() / 60.0;
            } else {
                this.isLate = false;
                this.lateHours = 0.0;
            }
            
            // Calculate overtime hours (beyond 8 hours of work)
            this.overtimeHours = Math.max(0, this.computedHours - 8.0);
        }
    }
    
    // Calculate computed amount based on hourly rate
    public void calculateComputedAmount(double hourlyRate) {
        this.computedAmount = this.computedHours * hourlyRate;
    }
    
    // ADDED: BigDecimal support methods for TardinessRecordModel compatibility
    
    /**
     * Get late hours as BigDecimal for TardinessRecordModel
     */
    public BigDecimal getLateHoursBigDecimal() {
        return BigDecimal.valueOf(lateHours).setScale(4, RoundingMode.HALF_UP);
    }
    
    /**
     * Get overtime hours as BigDecimal
     */
    public BigDecimal getOvertimeHoursBigDecimal() {
        return BigDecimal.valueOf(overtimeHours).setScale(4, RoundingMode.HALF_UP);
    }
    
    /**
     * Get computed hours as BigDecimal
     */
    public BigDecimal getComputedHoursBigDecimal() {
        return BigDecimal.valueOf(computedHours).setScale(4, RoundingMode.HALF_UP);
    }
    
    /**
     * Calculate undertime hours as BigDecimal
     */
    public BigDecimal getUndertimeHoursBigDecimal() {
        if (timeOut == null) {
            return BigDecimal.ZERO;
        }
        
        LocalTime actualTimeOut = timeOut.toLocalTime();
        if (actualTimeOut.isBefore(STANDARD_END_TIME)) {
            long minutesEarly = Duration.between(actualTimeOut, STANDARD_END_TIME).toMinutes();
            return BigDecimal.valueOf(minutesEarly)
                             .divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);
        }
        
        return BigDecimal.ZERO;
    }
    
    // Validation methods
    public boolean isValidAttendance() {
        return date != null && timeIn != null && timeOut != null && 
               employeeId > 0 && timeOut.after(timeIn);
    }
    
    public boolean isCompleteAttendance() {
        return timeIn != null && timeOut != null;
    }
    
    public boolean hasTimeIn() {
        return timeIn != null;
    }
    
    public boolean hasTimeOut() {
        return timeOut != null;
    }
    
    // Utility methods
    public String getFormattedDate() {
        if (date == null) return "";
        return date.toLocalDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }
    
    public String getFormattedTimeIn() {
        if (timeIn == null) return "";
        return timeIn.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"));
    }
    
    public String getFormattedTimeOut() {
        if (timeOut == null) return "";
        return timeOut.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"));
    }
    
    public String getPayMonth() {
        if (date == null) return "";
        return date.toLocalDate().format(DateTimeFormatter.ofPattern("yyyy-MM"));
    }
    
    // Check if attendance is for a working day (Monday-Friday)
    public boolean isWorkingDay() {
        if (date == null) return false;
        DayOfWeek dayOfWeek = date.toLocalDate().getDayOfWeek();
        return dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY;
    }
    
    // Enhanced methods for database-calculated values
    
    /**
     * Set late hours directly for database-calculated values
     */
    public void setLateHours(double lateHours) { 
        this.lateHours = lateHours; 
        this.isLate = lateHours > 0;
    }
    
    /**
     * Set overtime hours directly for database-calculated values
     */
    public void setOvertimeHours(double overtimeHours) { 
        this.overtimeHours = overtimeHours; 
    }
    
    /**
     * Set attendance data without triggering automatic calculations
     */
    public void setAttendanceDataWithCalculatedValues(Date date, Time timeIn, Time timeOut, 
                                                      double computedHours, double lateHours, double overtimeHours) {
        this.date = date;
        this.timeIn = timeIn;
        this.timeOut = timeOut;
        this.computedHours = computedHours;
        this.lateHours = lateHours;
        this.overtimeHours = overtimeHours;
        this.isLate = lateHours > 0;
        // Don't call calculateAttendanceMetrics() since we're setting calculated values
    }
    
    // Getters and Setters
    public Integer getAttendanceId() { return attendanceId; }
    public void setAttendanceId(Integer attendanceId) { this.attendanceId = attendanceId; }
    
    public Date getDate() { return date; }
    public void setDate(Date date) { 
        this.date = date; 
        calculateAttendanceMetrics();
    }
    
    public Time getTimeIn() { return timeIn; }
    public void setTimeIn(Time timeIn) { 
        this.timeIn = timeIn; 
        calculateAttendanceMetrics();
    }
    
    public Time getTimeOut() { return timeOut; }
    public void setTimeOut(Time timeOut) { 
        this.timeOut = timeOut; 
        calculateAttendanceMetrics();
    }
    
    public int getEmployeeId() { return employeeId; }
    public void setEmployeeId(int employeeId) { this.employeeId = employeeId; }
    
    public double getComputedHours() { return computedHours; }
    public void setComputedHours(double computedHours) { this.computedHours = computedHours; }
    
    public double getComputedAmount() { return computedAmount; }
    public void setComputedAmount(double computedAmount) { this.computedAmount = computedAmount; }
    
    public boolean isLate() { return isLate; }
    public double getLateHours() { return lateHours; }
    public double getOvertimeHours() { return overtimeHours; }
    
    @Override
    public String toString() {
        return String.format("AttendanceModel{attendanceId=%d, employeeId=%d, date=%s, timeIn=%s, timeOut=%s, computedHours=%.2f, isLate=%s, lateHours=%.2f, overtimeHours=%.2f}",
                attendanceId, employeeId, date, timeIn, timeOut, computedHours, isLate, lateHours, overtimeHours);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        AttendanceModel that = (AttendanceModel) obj;
        return attendanceId != null && attendanceId.equals(that.attendanceId);
    }
    
    @Override
    public int hashCode() {
        return attendanceId != null ? attendanceId.hashCode() : 0;
    }
}