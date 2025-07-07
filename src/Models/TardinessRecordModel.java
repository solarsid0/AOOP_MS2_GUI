package Models;

import java.time.*;
import java.sql.Timestamp;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class TardinessRecordModel {
    private static final ZoneId MANILA_TIMEZONE = ZoneId.of("Asia/Manila");
    private static final LocalTime STANDARD_START_TIME = LocalTime.of(8, 0);   // 8:00 AM
    private static final LocalTime STANDARD_END_TIME = LocalTime.of(17, 0);    // 5:00 PM
    private static final LocalTime GRACE_PERIOD_CUTOFF = LocalTime.of(8, 10);  // 8:10 AM grace period
    
    public enum TardinessType {
        LATE("Late"),
        UNDERTIME("Undertime");
        
        private final String value;
        TardinessType(String value) { this.value = value; }
        
        public String getValue() { return value; }
        public String getDisplayName() { return value; } // Added for compatibility with DAO
        
        public static TardinessType fromString(String type) {
            if (type == null) return LATE;
            
            for (TardinessType tt : TardinessType.values()) {
                if (tt.value.equalsIgnoreCase(type.trim())) {
                    return tt;
                }
            }
            return LATE;
        }
    }
    
    private int tardinessId;
    private int attendanceId;
    private BigDecimal tardinessHours;
    private TardinessType tardinessType;
    private String supervisorNotes;
    private Timestamp createdAt;
    
    // Additional fields for calculations
    private AttendanceModel relatedAttendance;
    private double hourlyRate;
    private double deductionAmount;
    
    // Constructors
    public TardinessRecordModel() {
        this.createdAt = Timestamp.valueOf(LocalDateTime.now(MANILA_TIMEZONE));
    }
    
    public TardinessRecordModel(int attendanceId, double tardinessHours, TardinessType tardinessType) {
        this();
        this.attendanceId = attendanceId;
        this.tardinessHours = BigDecimal.valueOf(tardinessHours).setScale(2, RoundingMode.HALF_UP);
        this.tardinessType = tardinessType;
    }
    
    public TardinessRecordModel(AttendanceModel attendance, TardinessType tardinessType) {
        this();
        if (attendance != null) {
            this.attendanceId = attendance.getAttendanceId();
            this.relatedAttendance = attendance;
            this.tardinessType = tardinessType;
            calculateTardinessHours(attendance);
        }
    }
    
    // Manila timezone operations
    public LocalDateTime getCreatedAtInManila() {
        if (createdAt == null) return null;
        return createdAt.toLocalDateTime().atZone(ZoneId.systemDefault())
                .withZoneSameInstant(MANILA_TIMEZONE).toLocalDateTime();
    }
    
    public static LocalDateTime nowInManila() {
        return LocalDateTime.now(MANILA_TIMEZONE);
    }
    
    // Late/undertime calculations with Manila timezone
    private void calculateTardinessHours(AttendanceModel attendance) {
        if (attendance == null) return;
        
        if (tardinessType == TardinessType.LATE) {
            calculateLateHours(attendance);
        } else if (tardinessType == TardinessType.UNDERTIME) {
            calculateUndertimeHours(attendance);
        }
    }
    
    private void calculateLateHours(AttendanceModel attendance) {
        if (attendance.getTimeIn() == null) return;
        
        LocalTime actualTimeIn = attendance.getTimeInManila();
        
        // Only count as late if beyond grace period (8:10 AM)
        if (actualTimeIn.isAfter(GRACE_PERIOD_CUTOFF)) {
            Duration lateDuration = Duration.between(STANDARD_START_TIME, actualTimeIn);
            double lateHours = lateDuration.toMinutes() / 60.0;
            this.tardinessHours = BigDecimal.valueOf(lateHours).setScale(2, RoundingMode.HALF_UP);
        } else {
            this.tardinessHours = BigDecimal.ZERO;
        }
    }
    
    private void calculateUndertimeHours(AttendanceModel attendance) {
        if (attendance.getTimeOut() == null) return;
        
        LocalTime actualTimeOut = attendance.getTimeOutManila();
        
        // Count as undertime if left before standard end time
        if (actualTimeOut.isBefore(STANDARD_END_TIME)) {
            Duration undertimeDuration = Duration.between(actualTimeOut, STANDARD_END_TIME);
            double undertimeHours = undertimeDuration.toMinutes() / 60.0;
            this.tardinessHours = BigDecimal.valueOf(undertimeHours).setScale(2, RoundingMode.HALF_UP);
        } else {
            this.tardinessHours = BigDecimal.ZERO;
        }
    }
    
    // Enhanced factory methods for creating tardiness records with grace period logic
    public static TardinessRecordModel createLateRecord(AttendanceModel attendance) {
        if (attendance == null || attendance.getTimeIn() == null) {
            return null;
        }
        
        // Check if actually late (beyond grace period)
        LocalTime actualTimeIn = attendance.getTimeInManila();
        if (!actualTimeIn.isAfter(GRACE_PERIOD_CUTOFF)) {
            return null; // Within grace period, no tardiness record needed
        }
        
        TardinessRecordModel record = new TardinessRecordModel(attendance, TardinessType.LATE);
        return record.tardinessHours != null && record.tardinessHours.compareTo(BigDecimal.ZERO) > 0 ? record : null;
    }
    
    public static TardinessRecordModel createUndertimeRecord(AttendanceModel attendance) {
        if (attendance == null || attendance.getTimeOut() == null) {
            return null;
        }
        
        // Check if actually undertime (left before standard end time)
        LocalTime actualTimeOut = attendance.getTimeOutManila();
        if (!actualTimeOut.isBefore(STANDARD_END_TIME)) {
            return null; // Left at or after standard time, no undertime
        }
        
        TardinessRecordModel record = new TardinessRecordModel(attendance, TardinessType.UNDERTIME);
        return record.tardinessHours != null && record.tardinessHours.compareTo(BigDecimal.ZERO) > 0 ? record : null;
    }
    
    // Calculate deduction amount based on hourly rate
    public void calculateDeductionAmount(double hourlyRate) {
        if (tardinessHours != null && hourlyRate > 0) {
            this.hourlyRate = hourlyRate;
            this.deductionAmount = tardinessHours.doubleValue() * hourlyRate;
        }
    }
    
    // Enhanced validation methods
    public boolean isValidRecord() {
        return attendanceId > 0 && 
               tardinessHours != null && 
               tardinessHours.compareTo(BigDecimal.ZERO) > 0 &&
               tardinessType != null;
    }
    
    public boolean hasSignificantTardiness() {
        // Consider tardiness significant if more than 5 minutes (0.083 hours)
        BigDecimal threshold = BigDecimal.valueOf(0.083);
        return tardinessHours != null && tardinessHours.compareTo(threshold) > 0;
    }
    
    public boolean isWithinGracePeriod() {
        // Check if the tardiness would have been within grace period
        if (tardinessType != TardinessType.LATE || tardinessHours == null) {
            return false;
        }
        
        // If late hours is less than 10 minutes (grace period), it was within grace
        BigDecimal gracePeriodMinutes = BigDecimal.valueOf(10.0 / 60.0); // 10 minutes in hours
        return tardinessHours.compareTo(gracePeriodMinutes) <= 0;
    }
    
    // Business logic methods
    public boolean requiresSupervisorNotes() {
        // Require supervisor notes for tardiness over 1 hour
        BigDecimal oneHour = BigDecimal.ONE;
        return tardinessHours != null && tardinessHours.compareTo(oneHour) > 0;
    }
    
    public String getFormattedTardinessHours() {
        if (tardinessHours == null) return "0.00";
        return tardinessHours.setScale(2, RoundingMode.HALF_UP).toString();
    }
    
    public String getFormattedTardinessMinutes() {
        if (tardinessHours == null) return "0";
        double minutes = tardinessHours.doubleValue() * 60;
        return String.format("%.0f", minutes);
    }
    
    public String getTardinessDescription() {
        if (tardinessHours == null || tardinessType == null) return "";
        
        StringBuilder description = new StringBuilder();
        description.append(tardinessType.getValue()).append(": ");
        description.append(getFormattedTardinessMinutes()).append(" minutes");
        
        if (tardinessType == TardinessType.LATE) {
            description.append(" late arrival");
            if (isWithinGracePeriod()) {
                description.append(" (within grace period)");
            }
        } else {
            description.append(" early departure");
        }
        
        return description.toString();
    }
    
    public String getDetailedDescription() {
        StringBuilder description = new StringBuilder();
        description.append(getTardinessDescription());
        
        if (deductionAmount > 0) {
            description.append(" - Deduction: ₱").append(String.format("%.2f", deductionAmount));
        }
        
        if (supervisorNotes != null && !supervisorNotes.trim().isEmpty()) {
            description.append(" - Notes: ").append(supervisorNotes);
        }
        
        return description.toString();
    }
    
    // Utility methods for reporting and rank-and-file calculations
    public boolean isLate() {
        return tardinessType == TardinessType.LATE;
    }
    
    public boolean isUndertime() {
        return tardinessType == TardinessType.UNDERTIME;
    }
    
    public double getTardinessHoursAsDouble() {
        return tardinessHours != null ? tardinessHours.doubleValue() : 0.0;
    }
    
    public double getTardinessMinutesAsDouble() {
        return getTardinessHoursAsDouble() * 60.0;
    }
    
    /**
     * Calculates rank-and-file deduction impact
     * For rank-and-file employees, late hours are deducted from total work hours
     * @param totalWorkHours The total work hours for the day
     * @return Adjusted work hours after tardiness deduction
     */
    public double calculateRankAndFileAdjustedHours(double totalWorkHours) {
        if (tardinessType != TardinessType.LATE || tardinessHours == null) {
            return totalWorkHours;
        }
        
        // For rank-and-file, deduct late hours from total work hours
        double adjustedHours = totalWorkHours - getTardinessHoursAsDouble();
        return Math.max(0, adjustedHours); // Cannot be negative
    }
    
    /**
     * Checks if this tardiness record requires disciplinary action
     * @return true if disciplinary action may be needed
     */
    public boolean requiresDisciplinaryAction() {
        if (tardinessHours == null) return false;
        
        // More than 2 hours tardiness may require disciplinary action
        BigDecimal threshold = BigDecimal.valueOf(2.0);
        return tardinessHours.compareTo(threshold) > 0;
    }
    
    /**
     * Gets the severity level of tardiness
     * @return Severity level (LOW, MEDIUM, HIGH, CRITICAL)
     */
    public String getSeverityLevel() {
        if (tardinessHours == null) return "NONE";
        
        double hours = tardinessHours.doubleValue();
        
        if (hours <= 0.25) { // 15 minutes or less
            return "LOW";
        } else if (hours <= 1.0) { // 1 hour or less
            return "MEDIUM";
        } else if (hours <= 2.0) { // 2 hours or less
            return "HIGH";
        } else {
            return "CRITICAL";
        }
    }
    
    /**
     * Calculates the grace period adjustment for Manila timezone
     * @param originalLateMinutes The original late minutes
     * @return Adjusted late minutes after grace period
     */
    public static double calculateGracePeriodAdjustment(double originalLateMinutes) {
        final double GRACE_PERIOD_MINUTES = 10.0; // 8:10 AM grace period
        
        if (originalLateMinutes <= GRACE_PERIOD_MINUTES) {
            return 0.0; // Within grace period
        } else {
            return originalLateMinutes - GRACE_PERIOD_MINUTES;
        }
    }
    
    /**
     * Checks if this tardiness occurred on a Monday (often has different rules)
     * @return true if the tardiness was on a Monday
     */
    public boolean isMonday() {
        if (createdAt == null) return false;
        
        LocalDateTime dateTime = getCreatedAtInManila();
        return dateTime != null && dateTime.getDayOfWeek() == DayOfWeek.MONDAY;
    }
    
    /**
     * Gets the day of week for this tardiness record
     * @return DayOfWeek enum value
     */
    public DayOfWeek getDayOfWeek() {
        if (createdAt == null) return null;
        
        LocalDateTime dateTime = getCreatedAtInManila();
        return dateTime != null ? dateTime.getDayOfWeek() : null;
    }
    
    /**
     * Formats the tardiness for payroll display
     * @return Formatted string for payroll systems
     */
    public String getPayrollDisplayFormat() {
        return String.format("%s: %.2f hrs (₱%.2f deduction)", 
                           tardinessType.getValue(), 
                           getTardinessHoursAsDouble(), 
                           deductionAmount);
    }
    
    // Getters and Setters
    public int getTardinessId() { return tardinessId; }
    public void setTardinessId(int tardinessId) { this.tardinessId = tardinessId; }
    
    public int getAttendanceId() { return attendanceId; }
    public void setAttendanceId(int attendanceId) { this.attendanceId = attendanceId; }
    
    public BigDecimal getTardinessHours() { return tardinessHours; }
    public void setTardinessHours(BigDecimal tardinessHours) { 
        this.tardinessHours = tardinessHours != null ? 
            tardinessHours.setScale(2, RoundingMode.HALF_UP) : null; 
    }
    
    public TardinessType getTardinessType() { return tardinessType; }
    public void setTardinessType(TardinessType tardinessType) { this.tardinessType = tardinessType; }
    
    public String getSupervisorNotes() { return supervisorNotes; }
    public void setSupervisorNotes(String supervisorNotes) { 
        this.supervisorNotes = supervisorNotes != null ? supervisorNotes.trim() : null; 
    }
    
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { 
        this.createdAt = createdAt != null ? Timestamp.valueOf(createdAt) : null; 
    }
    
    public AttendanceModel getRelatedAttendance() { return relatedAttendance; }
    public void setRelatedAttendance(AttendanceModel relatedAttendance) { 
        this.relatedAttendance = relatedAttendance; 
    }
    
    public double getHourlyRate() { return hourlyRate; }
    public void setHourlyRate(double hourlyRate) { 
        this.hourlyRate = hourlyRate;
        if (tardinessHours != null && hourlyRate > 0) {
            calculateDeductionAmount(hourlyRate);
        }
    }
    
    public double getDeductionAmount() { return deductionAmount; }
    public void setDeductionAmount(double deductionAmount) { this.deductionAmount = deductionAmount; }
    
    // Static utility methods for Manila timezone
    public static LocalDateTime getCurrentManilaTime() {
        return LocalDateTime.now(MANILA_TIMEZONE);
    }
    
    public static LocalTime getStandardStartTime() {
        return STANDARD_START_TIME;
    }
    
    public static LocalTime getStandardEndTime() {
        return STANDARD_END_TIME;
    }
    
    public static LocalTime getGracePeriodCutoff() {
        return GRACE_PERIOD_CUTOFF;
    }
    
    @Override
    public String toString() {
        return String.format("TardinessRecordModel{tardinessId=%d, attendanceId=%d, tardinessHours=%s, type=%s, deduction=%.2f, severity=%s}",
                tardinessId, attendanceId, getFormattedTardinessHours(), tardinessType, deductionAmount, getSeverityLevel());
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        TardinessRecordModel that = (TardinessRecordModel) obj;
        return tardinessId == that.tardinessId;
    }
    
    @Override
    public int hashCode() {
        return Integer.hashCode(tardinessId);
    }
}