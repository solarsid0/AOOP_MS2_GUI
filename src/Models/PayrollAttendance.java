package Models;

import java.math.BigDecimal;

/**
 * PayrollAttendance - Model class mapping to payrollattendance table
 * Links attendance records to payroll calculations
 * Fields: payrollId, attendanceId, computedHours, computedAmount
 * @author User
 */
public class PayrollAttendance {
    
    private Integer payrollId;
    private Integer attendanceId;
    private BigDecimal computedHours;
    private BigDecimal computedAmount;
    
    // ===============================
    // CONSTRUCTORS
    // ===============================
    
    /**
     * Default constructor
     */
    public PayrollAttendance() {}
    
    /**
     * Constructor with essential fields
     * @param payrollId The payroll ID
     * @param attendanceId The attendance ID
     */
    public PayrollAttendance(Integer payrollId, Integer attendanceId) {
        this.payrollId = payrollId;
        this.attendanceId = attendanceId;
    }
    
    /**
     * Full constructor
     * @param payrollId The payroll ID
     * @param attendanceId The attendance ID
     * @param computedHours Computed hours for this attendance record
     * @param computedAmount Computed amount for this attendance record
     */
    public PayrollAttendance(Integer payrollId, Integer attendanceId, 
                           BigDecimal computedHours, BigDecimal computedAmount) {
        this.payrollId = payrollId;
        this.attendanceId = attendanceId;
        this.computedHours = computedHours;
        this.computedAmount = computedAmount;
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
    
    public Integer getAttendanceId() {
        return attendanceId;
    }
    
    public void setAttendanceId(Integer attendanceId) {
        this.attendanceId = attendanceId;
    }
    
    public BigDecimal getComputedHours() {
        return computedHours;
    }
    
    public void setComputedHours(BigDecimal computedHours) {
        this.computedHours = computedHours;
    }
    
    public BigDecimal getComputedAmount() {
        return computedAmount;
    }
    
    public void setComputedAmount(BigDecimal computedAmount) {
        this.computedAmount = computedAmount;
    }
    
    // ===============================
    // BUSINESS METHODS
    // ===============================
    
    /**
     * Checks if this payroll attendance record has computed values
     * @return true if both hours and amount are computed
     */
    public boolean isComputed() {
        return computedHours != null && computedAmount != null &&
               computedHours.compareTo(BigDecimal.ZERO) >= 0 &&
               computedAmount.compareTo(BigDecimal.ZERO) >= 0;
    }
    
    /**
     * Calculates the effective hourly rate from computed values
     * @return Hourly rate or zero if hours is zero
     */
    public BigDecimal getEffectiveHourlyRate() {
        if (computedHours == null || computedAmount == null || 
            computedHours.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        return computedAmount.divide(computedHours, 2, BigDecimal.ROUND_HALF_UP);
    }
    
    /**
     * Validates the payroll attendance record
     * @return true if valid
     */
    public boolean isValid() {
        if (payrollId == null || attendanceId == null) {
            return false;
        }
        
        // If computed values are set, they should be non-negative
        if (computedHours != null && computedHours.compareTo(BigDecimal.ZERO) < 0) {
            return false;
        }
        
        if (computedAmount != null && computedAmount.compareTo(BigDecimal.ZERO) < 0) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Resets computed values to null
     */
    public void resetComputedValues() {
        this.computedHours = null;
        this.computedAmount = null;
    }
    
    /**
     * Sets computed values with validation
     * @param hours Computed hours
     * @param amount Computed amount
     * @throws IllegalArgumentException if values are negative
     */
    public void setComputedValues(BigDecimal hours, BigDecimal amount) {
        if (hours != null && hours.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Computed hours cannot be negative");
        }
        
        if (amount != null && amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Computed amount cannot be negative");
        }
        
        this.computedHours = hours;
        this.computedAmount = amount;
    }
    
    // ===============================
    // UTILITY METHODS
    // ===============================
    
    @Override
    public String toString() {
        return "PayrollAttendance{" +
                "payrollId=" + payrollId +
                ", attendanceId=" + attendanceId +
                ", computedHours=" + computedHours +
                ", computedAmount=" + computedAmount +
                ", isComputed=" + isComputed() +
                '}';
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        PayrollAttendance that = (PayrollAttendance) obj;
        return payrollId != null && payrollId.equals(that.payrollId) &&
               attendanceId != null && attendanceId.equals(that.attendanceId);
    }
    
    @Override
    public int hashCode() {
        int result = payrollId != null ? payrollId.hashCode() : 0;
        result = 31 * result + (attendanceId != null ? attendanceId.hashCode() : 0);
        return result;
    }
}