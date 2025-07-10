package Models;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;

public class TardinessRecordModel {
    
    // ===== FIELDS =====
    private Integer tardinessRecordId;
    private Integer attendanceId;
    private BigDecimal tardinessHours = BigDecimal.ZERO;
    private TardinessType tardinessType;
    private String supervisorNotes;
    
    // ===== ENUMS =====
    public enum TardinessType {
        LATE, UNDERTIME, ABSENCE
    }
    
    // ===== CONSTRUCTORS =====
    public TardinessRecordModel() {
        this.tardinessHours = BigDecimal.ZERO;
    }
    
    public TardinessRecordModel(Integer attendanceId, BigDecimal tardinessHours, 
                               TardinessType tardinessType, String supervisorNotes) {
        this.attendanceId = attendanceId;
        this.tardinessHours = tardinessHours != null ? tardinessHours.setScale(4, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        this.tardinessType = tardinessType;
        this.supervisorNotes = supervisorNotes;
    }
    
    // ===== GETTERS AND SETTERS =====
    
    public Integer getTardinessRecordId() {
        return tardinessRecordId;
    }
    
    public void setTardinessRecordId(Integer tardinessRecordId) {
        this.tardinessRecordId = tardinessRecordId;
    }
    
    public Integer getAttendanceId() {
        return attendanceId;
    }
    
    public void setAttendanceId(Integer attendanceId) {
        this.attendanceId = attendanceId;
    }
    
    public BigDecimal getTardinessHours() {
        return tardinessHours != null ? tardinessHours : BigDecimal.ZERO;
    }
    
    public void setTardinessHours(BigDecimal tardinessHours) {
        this.tardinessHours = tardinessHours != null ? tardinessHours.setScale(4, RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }
    
    public TardinessType getTardinessType() {
        return tardinessType;
    }
    
    public void setTardinessType(TardinessType tardinessType) {
        this.tardinessType = tardinessType;
    }
    
    public String getSupervisorNotes() {
        return supervisorNotes;
    }
    
    public void setSupervisorNotes(String supervisorNotes) {
        this.supervisorNotes = supervisorNotes;
    }
    
    // ===== BACKWARD COMPATIBILITY METHODS =====
    
    /**
     * Get tardiness hours as double for backward compatibility
     */
    @Deprecated
    public double getTardinessHoursAsDouble() {
        return tardinessHours != null ? tardinessHours.doubleValue() : 0.0;
    }
    
    /**
     * Set tardiness hours from double for backward compatibility
     */
    @Deprecated
    public void setTardinessHoursFromDouble(double tardinessHours) {
        this.tardinessHours = BigDecimal.valueOf(tardinessHours).setScale(4, RoundingMode.HALF_UP);
    }
    
    // ===== FACTORY METHODS =====
    
    /**
     * Create a late tardiness record from attendance
     */
    public static TardinessRecordModel createLateRecord(AttendanceModel attendance) {
        if (attendance == null || !attendance.isLateAttendance()) {
            return null;
        }
        
        // Check if attendance has a valid ID
        if (attendance.getAttendanceId() == null || attendance.getAttendanceId() <= 0) {
            System.err.println("Cannot create tardiness record: Attendance ID is null or invalid");
            return null;
        }
        
        BigDecimal lateHours = attendance.getLateHoursBigDecimal();
        String notes = "Late arrival at " + attendance.getFormattedTimeIn() + 
                      " - " + lateHours + " hours late";
        
        return new TardinessRecordModel(
            attendance.getAttendanceId(),
            lateHours,
            TardinessType.LATE,
            notes
        );
    }
    
    /**
     * Create an undertime tardiness record from attendance
     */
    public static TardinessRecordModel createUndertimeRecord(AttendanceModel attendance) {
        if (attendance == null || !attendance.isEarlyOut()) {
            return null;
        }
        
        // Check if attendance has a valid ID
        if (attendance.getAttendanceId() == null || attendance.getAttendanceId() <= 0) {
            System.err.println("Cannot create tardiness record: Attendance ID is null or invalid");
            return null;
        }
        
        BigDecimal undertimeHours = attendance.getUndertimeHoursBigDecimal();
        String notes = "Early departure at " + attendance.getFormattedTimeOut() + 
                      " - " + undertimeHours + " hours undertime";
        
        return new TardinessRecordModel(
            attendance.getAttendanceId(),
            undertimeHours,
            TardinessType.UNDERTIME,
            notes
        );
    }
    
    /**
     * Create an absence tardiness record
     */
    public static TardinessRecordModel createAbsenceRecord(Integer attendanceId, String notes) {
        return new TardinessRecordModel(
            attendanceId,
            new BigDecimal("8.0"), // Full day absence = 8 hours
            TardinessType.ABSENCE,
            notes != null ? notes : "Full day absence"
        );
    }
    
    // ===== VALIDATION METHODS =====
    
    /**
     * Check if the tardiness record is valid
     */
    public boolean isValidRecord() {
        return attendanceId != null && attendanceId > 0 && 
               tardinessHours != null && 
               tardinessHours.compareTo(BigDecimal.ZERO) > 0 && 
               tardinessType != null;
    }
    
    /**
     * Get a description of the tardiness
     */
    public String getTardinessDescription() {
        if (!isValidRecord()) {
            return "Invalid tardiness record";
        }
        
        return tardinessType + " - " + tardinessHours + " hours";
    }
    
    /**
     * Check if this is a severe tardiness (over 2 hours)
     */
    public boolean isSevereTardiness() {
        return tardinessHours != null && 
               tardinessHours.compareTo(new BigDecimal("2.0")) > 0;
    }
    
    /**
     * Check if this is a minor tardiness (under 30 minutes)
     */
    public boolean isMinorTardiness() {
        return tardinessHours != null && 
               tardinessHours.compareTo(new BigDecimal("0.5")) <= 0;
    }
    
    // ===== UTILITY METHODS =====
    
    /**
     * Round tardiness hours to specified decimal places
     */
    public void roundTardinessHours(int decimalPlaces) {
        if (tardinessHours != null) {
            this.tardinessHours = tardinessHours.setScale(decimalPlaces, RoundingMode.HALF_UP);
        }
    }
    
    /**
     * Add hours to tardiness (for combining multiple tardiness instances)
     */
    public void addTardinessHours(BigDecimal additionalHours) {
        if (additionalHours != null && additionalHours.compareTo(BigDecimal.ZERO) > 0) {
            this.tardinessHours = this.tardinessHours.add(additionalHours);
        }
    }
    
    /**
     * Check if tardiness exceeds a threshold
     */
    public boolean exceedsThreshold(BigDecimal threshold) {
        return tardinessHours != null && 
               threshold != null && 
               tardinessHours.compareTo(threshold) > 0;
    }
    
    /**
     * Get tardiness impact level
     */
    public String getTardinessImpactLevel() {
        if (tardinessHours == null || tardinessHours.compareTo(BigDecimal.ZERO) <= 0) {
            return "None";
        } else if (tardinessHours.compareTo(new BigDecimal("0.25")) <= 0) {
            return "Minimal"; // 15 minutes or less
        } else if (tardinessHours.compareTo(new BigDecimal("0.5")) <= 0) {
            return "Minor"; // 30 minutes or less
        } else if (tardinessHours.compareTo(new BigDecimal("1.0")) <= 0) {
            return "Moderate"; // 1 hour or less
        } else if (tardinessHours.compareTo(new BigDecimal("2.0")) <= 0) {
            return "Significant"; // 2 hours or less
        } else {
            return "Severe"; // Over 2 hours
        }
    }
    
    /**
     * Calculate potential salary deduction based on hourly rate
     */
    public BigDecimal calculateSalaryDeduction(BigDecimal hourlyRate) {
        if (tardinessHours == null || hourlyRate == null) {
            return BigDecimal.ZERO;
        }
        return tardinessHours.multiply(hourlyRate).setScale(2, RoundingMode.HALF_UP);
    }
    
    // ===== toString, equals, hashCode =====
    
    @Override
    public String toString() {
        return "TardinessRecordModel{" +
                "tardinessRecordId=" + tardinessRecordId +
                ", attendanceId=" + attendanceId +
                ", tardinessHours=" + tardinessHours +
                ", tardinessType=" + tardinessType +
                ", supervisorNotes='" + supervisorNotes + '\'' +
                ", impactLevel='" + getTardinessImpactLevel() + '\'' +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        TardinessRecordModel that = (TardinessRecordModel) o;
        
        return tardinessRecordId != null && tardinessRecordId.equals(that.tardinessRecordId);
    }
    
    @Override
    public int hashCode() {
        return tardinessRecordId != null ? tardinessRecordId.hashCode() : 0;
    }
}