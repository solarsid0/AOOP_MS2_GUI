package Models;

import java.math.BigDecimal;

/**
 * PayrollLeave - Model class mapping to payrollleave table
 * Links leave requests to payroll calculations and handles leave deductions
 * Fields: payrollId, leaveRequestId, leaveHours
 * @author User
 */
public class PayrollLeave {
    
    private Integer payrollId;
    private Integer leaveRequestId;
    private BigDecimal leaveHours;
    
    // ===============================
    // CONSTRUCTORS
    // ===============================
    
    /**
     * Default constructor
     */
    public PayrollLeave() {}
    
    /**
     * Constructor with essential fields
     * @param payrollId The payroll ID
     * @param leaveRequestId The leave request ID
     */
    public PayrollLeave(Integer payrollId, Integer leaveRequestId) {
        this.payrollId = payrollId;
        this.leaveRequestId = leaveRequestId;
    }
    
    /**
     * Full constructor
     * @param payrollId The payroll ID
     * @param leaveRequestId The leave request ID
     * @param leaveHours Hours of leave taken
     */
    public PayrollLeave(Integer payrollId, Integer leaveRequestId, BigDecimal leaveHours) {
        this.payrollId = payrollId;
        this.leaveRequestId = leaveRequestId;
        this.leaveHours = leaveHours;
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
    
    public Integer getLeaveRequestId() {
        return leaveRequestId;
    }
    
    public void setLeaveRequestId(Integer leaveRequestId) {
        this.leaveRequestId = leaveRequestId;
    }
    
    public BigDecimal getLeaveHours() {
        return leaveHours;
    }
    
    public void setLeaveHours(BigDecimal leaveHours) {
        this.leaveHours = leaveHours;
    }
    
    // ===============================
    // BUSINESS METHODS
    // ===============================
    
    /**
     * Checks if leave hours are set
     * @return true if leave hours are specified
     */
    public boolean hasLeaveHours() {
        return leaveHours != null && leaveHours.compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * Calculates leave deduction amount based on hourly rate
     * @param hourlyRate Employee's hourly rate
     * @param isPaidLeave Whether this is paid leave or unpaid
     * @return Deduction amount (0 for paid leave, calculated amount for unpaid)
     */
    public BigDecimal calculateLeaveDeduction(BigDecimal hourlyRate, boolean isPaidLeave) {
        if (!hasLeaveHours() || hourlyRate == null) {
            return BigDecimal.ZERO;
        }
        
        if (isPaidLeave) {
            return BigDecimal.ZERO; // No deduction for paid leave
        }
        
        // Calculate deduction for unpaid leave
        return leaveHours.multiply(hourlyRate);
    }
    
    /**
     * Calculates leave payment for paid leave
     * @param hourlyRate Employee's hourly rate
     * @param isPaidLeave Whether this is paid leave
     * @return Payment amount (calculated amount for paid leave, 0 for unpaid)
     */
    public BigDecimal calculateLeavePayment(BigDecimal hourlyRate, boolean isPaidLeave) {
        if (!hasLeaveHours() || hourlyRate == null) {
            return BigDecimal.ZERO;
        }
        
        if (!isPaidLeave) {
            return BigDecimal.ZERO; // No payment for unpaid leave
        }
        
        // Calculate payment for paid leave
        return leaveHours.multiply(hourlyRate);
    }
    
    /**
     * Converts leave hours to days (assuming 8 hours per day)
     * @return Number of leave days
     */
    public BigDecimal getLeaveDays() {
        if (!hasLeaveHours()) {
            return BigDecimal.ZERO;
        }
        
        return leaveHours.divide(new BigDecimal("8"), 2, BigDecimal.ROUND_HALF_UP);
    }
    
    /**
     * Sets leave hours from days (assuming 8 hours per day)
     * @param leaveDays Number of leave days
     */
    public void setLeaveHoursFromDays(BigDecimal leaveDays) {
        if (leaveDays == null) {
            this.leaveHours = null;
        } else {
            this.leaveHours = leaveDays.multiply(new BigDecimal("8"));
        }
    }
    
    /**
     * Validates the payroll leave record
     * @return true if valid
     */
    public boolean isValid() {
        if (payrollId == null || leaveRequestId == null) {
            return false;
        }
        
        // Leave hours should be non-negative if set
        if (leaveHours != null && leaveHours.compareTo(BigDecimal.ZERO) < 0) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Checks if this is a full-day leave (8 hours or more)
     * @return true if 8 hours or more
     */
    public boolean isFullDayLeave() {
        return hasLeaveHours() && leaveHours.compareTo(new BigDecimal("8")) >= 0;
    }
    
    /**
     * Checks if this is a half-day leave (4 hours)
     * @return true if exactly 4 hours
     */
    public boolean isHalfDayLeave() {
        return hasLeaveHours() && leaveHours.compareTo(new BigDecimal("4")) == 0;
    }
    
    /**
     * Gets the leave type based on hours
     * @return String description of leave type
     */
    public String getLeaveType() {
        if (!hasLeaveHours()) {
            return "No Leave";
        }
        
        BigDecimal hours = leaveHours;
        if (hours.compareTo(new BigDecimal("8")) >= 0) {
            return "Full Day";
        } else if (hours.compareTo(new BigDecimal("4")) == 0) {
            return "Half Day";
        } else {
            return "Partial Day (" + hours + " hours)";
        }
    }
    
    // ===============================
    // UTILITY METHODS
    // ===============================
    
    @Override
    public String toString() {
        return "PayrollLeave{" +
                "payrollId=" + payrollId +
                ", leaveRequestId=" + leaveRequestId +
                ", leaveHours=" + leaveHours +
                ", leaveDays=" + getLeaveDays() +
                ", leaveType='" + getLeaveType() + '\'' +
                '}';
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        PayrollLeave that = (PayrollLeave) obj;
        return payrollId != null && payrollId.equals(that.payrollId) &&
               leaveRequestId != null && leaveRequestId.equals(that.leaveRequestId);
    }
    
    @Override
    public int hashCode() {
        int result = payrollId != null ? payrollId.hashCode() : 0;
        result = 31 * result + (leaveRequestId != null ? leaveRequestId.hashCode() : 0);
        return result;
    }
}