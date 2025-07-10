package Models;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PayrollAttendance {
    private Integer payrollAttendanceId;
    private Integer payrollId;
    private Integer attendanceId;
    private BigDecimal computedHours;
    private BigDecimal computedAmount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Constructors
    public PayrollAttendance() {}
    
    // Getters and Setters
    public Integer getPayrollAttendanceId() { return payrollAttendanceId; }
    public void setPayrollAttendanceId(Integer payrollAttendanceId) { this.payrollAttendanceId = payrollAttendanceId; }
    
    public Integer getPayrollId() { return payrollId; }
    public void setPayrollId(Integer payrollId) { this.payrollId = payrollId; }
    
    public Integer getAttendanceId() { return attendanceId; }
    public void setAttendanceId(Integer attendanceId) { this.attendanceId = attendanceId; }
    
    public BigDecimal getComputedHours() { return computedHours; }
    public void setComputedHours(BigDecimal computedHours) { this.computedHours = computedHours; }
    
    public BigDecimal getComputedAmount() { return computedAmount; }
    public void setComputedAmount(BigDecimal computedAmount) { this.computedAmount = computedAmount; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}