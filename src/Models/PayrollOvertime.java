package Models;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * PayrollOvertime - Model class mapping to payrollovertime table
 * Links overtime requests to payroll calculations and handles overtime pay
 * Fields: payrollId, overtimeRequestId, overtimeHours, overtimePay
 * @author User
 */
public class PayrollOvertime {
    
    private Integer payrollId;
    private Integer overtimeRequestId;
    private BigDecimal overtimeHours;
    private BigDecimal overtimePay;
    
    // Business constants for overtime calculations
    private static final BigDecimal DEFAULT_OVERTIME_MULTIPLIER = new BigDecimal("1.5"); // Time and a half
    private static final BigDecimal HOLIDAY_OVERTIME_MULTIPLIER = new BigDecimal("2.0"); // Double time
    private static final BigDecimal NIGHT_DIFFERENTIAL_RATE = new BigDecimal("0.10"); // 10% night differential
    
    // ===============================
    // CONSTRUCTORS
    // ===============================
    
    /**
     * Default constructor
     */
    public PayrollOvertime() {}
    
    /**
     * Constructor with essential fields
     * @param payrollId The payroll ID
     * @param overtimeRequestId The overtime request ID
     */
    public PayrollOvertime(Integer payrollId, Integer overtimeRequestId) {
        this.payrollId = payrollId;
        this.overtimeRequestId = overtimeRequestId;
    }
    
    /**
     * Full constructor
     * @param payrollId The payroll ID
     * @param overtimeRequestId The overtime request ID
     * @param overtimeHours Hours of overtime worked
     * @param overtimePay Calculated overtime pay
     */
    public PayrollOvertime(Integer payrollId, Integer overtimeRequestId, 
                         BigDecimal overtimeHours, BigDecimal overtimePay) {
        this.payrollId = payrollId;
        this.overtimeRequestId = overtimeRequestId;
        this.overtimeHours = overtimeHours;
        this.overtimePay = overtimePay;
    }
    
    // ===============================
    // GETTERS AND SETTERS
    // ===============================
    
    public Integer getPayrollId() {
        return payrollId;
    }
    
    public void setPayrollId(Integer payrollId) {
        this.payrollId = payrollId;
    }
    
    public Integer getOvertimeRequestId() {
        return overtimeRequestId;
    }
    
    public void setOvertimeRequestId(Integer overtimeRequestId) {
        this.overtimeRequestId = overtimeRequestId;
    }
    
    public BigDecimal getOvertimeHours() {
        return overtimeHours;
    }
    
    public void setOvertimeHours(BigDecimal overtimeHours) {
        this.overtimeHours = overtimeHours;
    }
    
    public BigDecimal getOvertimePay() {
        return overtimePay;
    }
    
    public void setOvertimePay(BigDecimal overtimePay) {
        this.overtimePay = overtimePay;
    }
    
    // ===============================
    // BUSINESS METHODS
    // ===============================
    
    /**
     * Checks if overtime hours are set
     * @return true if overtime hours are specified and positive
     */
    public boolean hasOvertimeHours() {
        return overtimeHours != null && overtimeHours.compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * Checks if overtime pay is calculated
     * @return true if overtime pay is set
     */
    public boolean hasOvertimePay() {
        return overtimePay != null && overtimePay.compareTo(BigDecimal.ZERO) >= 0;
    }
    
    /**
     * Calculates overtime pay based on regular hourly rate
     * @param regularHourlyRate Employee's regular hourly rate
     * @return Calculated overtime pay
     */
    public BigDecimal calculateOvertimePay(BigDecimal regularHourlyRate) {
        return calculateOvertimePay(regularHourlyRate, DEFAULT_OVERTIME_MULTIPLIER);
    }
    
    /**
     * Calculates overtime pay with specified multiplier
     * @param regularHourlyRate Employee's regular hourly rate
     * @param overtimeMultiplier Overtime rate multiplier (e.g., 1.5 for time and a half)
     * @return Calculated overtime pay
     */
    public BigDecimal calculateOvertimePay(BigDecimal regularHourlyRate, BigDecimal overtimeMultiplier) {
        if (!hasOvertimeHours() || regularHourlyRate == null || overtimeMultiplier == null) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal overtimeRate = regularHourlyRate.multiply(overtimeMultiplier);
        return overtimeHours.multiply(overtimeRate).setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * Calculates holiday overtime pay (double time)
     * @param regularHourlyRate Employee's regular hourly rate
     * @return Calculated holiday overtime pay
     */
    public BigDecimal calculateHolidayOvertimePay(BigDecimal regularHourlyRate) {
        return calculateOvertimePay(regularHourlyRate, HOLIDAY_OVERTIME_MULTIPLIER);
    }
    
    /**
     * Calculates night differential overtime pay
     * @param regularHourlyRate Employee's regular hourly rate
     * @return Calculated night differential overtime pay
     */
    public BigDecimal calculateNightDifferentialOvertimePay(BigDecimal regularHourlyRate) {
        if (!hasOvertimeHours() || regularHourlyRate == null) {
            return BigDecimal.ZERO;
        }
        
        // Regular overtime rate + night differential
        BigDecimal overtimeRate = regularHourlyRate.multiply(DEFAULT_OVERTIME_MULTIPLIER);
        BigDecimal nightDifferential = regularHourlyRate.multiply(NIGHT_DIFFERENTIAL_RATE);
        BigDecimal totalRate = overtimeRate.add(nightDifferential);
        
        return overtimeHours.multiply(totalRate).setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * Gets the effective overtime rate based on current pay and hours
     * @return Effective hourly overtime rate
     */
    public BigDecimal getEffectiveOvertimeRate() {
        if (!hasOvertimeHours() || !hasOvertimePay() || 
            overtimeHours.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        return overtimePay.divide(overtimeHours, 2, RoundingMode.HALF_UP);
    }
    
    /**
     * Updates overtime pay using standard time-and-a-half calculation
     * @param regularHourlyRate Employee's regular hourly rate
     */
    public void updateOvertimePay(BigDecimal regularHourlyRate) {
        this.overtimePay = calculateOvertimePay(regularHourlyRate);
    }
    
    /**
     * Updates overtime pay with custom multiplier
     * @param regularHourlyRate Employee's regular hourly rate
     * @param overtimeMultiplier Overtime rate multiplier
     */
    public void updateOvertimePay(BigDecimal regularHourlyRate, BigDecimal overtimeMultiplier) {
        this.overtimePay = calculateOvertimePay(regularHourlyRate, overtimeMultiplier);
    }
    
    /**
     * Validates the payroll overtime record
     * @return true if valid
     */
    public boolean isValid() {
        if (payrollId == null || overtimeRequestId == null) {
            return false;
        }
        
        // Overtime hours should be non-negative if set
        if (overtimeHours != null && overtimeHours.compareTo(BigDecimal.ZERO) < 0) {
            return false;
        }
        
        // Overtime pay should be non-negative if set
        if (overtimePay != null && overtimePay.compareTo(BigDecimal.ZERO) < 0) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Checks if this represents significant overtime (more than 2 hours)
     * @return true if more than 2 hours of overtime
     */
    public boolean isSignificantOvertime() {
        return hasOvertimeHours() && overtimeHours.compareTo(new BigDecimal("2")) > 0;
    }
    
    /**
     * Gets overtime category based on hours worked
     * @return String description of overtime category
     */
    public String getOvertimeCategory() {
        if (!hasOvertimeHours()) {
            return "No Overtime";
        }
        
        BigDecimal hours = overtimeHours;
        if (hours.compareTo(new BigDecimal("4")) >= 0) {
            return "Extended Overtime (4+ hours)";
        } else if (hours.compareTo(new BigDecimal("2")) >= 0) {
            return "Regular Overtime (2-4 hours)";
        } else {
            return "Minimal Overtime (< 2 hours)";
        }
    }
    
    /**
     * Resets calculated pay values
     */
    public void resetCalculatedValues() {
        this.overtimePay = null;
    }
    
    // ===============================
    // UTILITY METHODS
    // ===============================
    
    @Override
    public String toString() {
        return "PayrollOvertime{" +
                "payrollId=" + payrollId +
                ", overtimeRequestId=" + overtimeRequestId +
                ", overtimeHours=" + overtimeHours +
                ", overtimePay=" + overtimePay +
                ", effectiveRate=" + getEffectiveOvertimeRate() +
                ", category='" + getOvertimeCategory() + '\'' +
                '}';
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        PayrollOvertime that = (PayrollOvertime) obj;
        return payrollId != null && payrollId.equals(that.payrollId) &&
               overtimeRequestId != null && overtimeRequestId.equals(that.overtimeRequestId);
    }
    
    @Override
    public int hashCode() {
        int result = payrollId != null ? payrollId.hashCode() : 0;
        result = 31 * result + (overtimeRequestId != null ? overtimeRequestId.hashCode() : 0);
        return result;
    }
}