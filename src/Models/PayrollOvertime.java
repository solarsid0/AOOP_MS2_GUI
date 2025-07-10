package Models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PayrollOvertime {
    private Integer payrollOvertimeId;
    private Integer payrollId;
    private Integer overtimeRequestId;  // ADDED: Missing field from database schema
    private Integer employeeId;
    private Integer payPeriodId;
    private BigDecimal overtimeHours;
    private BigDecimal overtimeRate;
    private BigDecimal overtimePay;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Constructors
    public PayrollOvertime() {}
    
    public PayrollOvertime(Integer payrollId, Integer overtimeRequestId, Integer employeeId, Integer payPeriodId,
                          BigDecimal overtimeHours, BigDecimal overtimeRate) {
        this.payrollId = payrollId;
        this.overtimeRequestId = overtimeRequestId;
        this.employeeId = employeeId;
        this.payPeriodId = payPeriodId;
        this.overtimeHours = overtimeHours != null ? overtimeHours : BigDecimal.ZERO;
        this.overtimeRate = overtimeRate != null ? overtimeRate : BigDecimal.ZERO;
        this.overtimePay = this.overtimeHours.multiply(this.overtimeRate);
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Integer getPayrollOvertimeId() { return payrollOvertimeId; }
    public void setPayrollOvertimeId(Integer payrollOvertimeId) { this.payrollOvertimeId = payrollOvertimeId; }
    
    public Integer getPayrollId() { return payrollId; }
    public void setPayrollId(Integer payrollId) { this.payrollId = payrollId; }
    
    // ADDED: Missing getter/setter for overtimeRequestId
    public Integer getOvertimeRequestId() { return overtimeRequestId; }
    public void setOvertimeRequestId(Integer overtimeRequestId) { this.overtimeRequestId = overtimeRequestId; }
    
    public Integer getEmployeeId() { return employeeId; }
    public void setEmployeeId(Integer employeeId) { this.employeeId = employeeId; }
    
    public Integer getPayPeriodId() { return payPeriodId; }
    public void setPayPeriodId(Integer payPeriodId) { this.payPeriodId = payPeriodId; }
    
    public BigDecimal getOvertimeHours() { return overtimeHours; }
    public void setOvertimeHours(BigDecimal overtimeHours) { this.overtimeHours = overtimeHours; }
    
    public BigDecimal getOvertimeRate() { return overtimeRate; }
    public void setOvertimeRate(BigDecimal overtimeRate) { this.overtimeRate = overtimeRate; }
    
    public BigDecimal getOvertimePay() { return overtimePay; }
    public void setOvertimePay(BigDecimal overtimePay) { this.overtimePay = overtimePay; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    @Override
    public String toString() {
        return "PayrollOvertime{" +
                "payrollOvertimeId=" + payrollOvertimeId +
                ", payrollId=" + payrollId +
                ", overtimeRequestId=" + overtimeRequestId +
                ", employeeId=" + employeeId +
                ", payPeriodId=" + payPeriodId +
                ", overtimeHours=" + overtimeHours +
                ", overtimeRate=" + overtimeRate +
                ", overtimePay=" + overtimePay +
                '}';
    }
}