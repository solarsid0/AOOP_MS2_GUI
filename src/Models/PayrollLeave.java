package Models;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PayrollLeave {
    private Integer payrollLeaveId;
    private Integer payrollId;
    private Integer leaveRequestId;
    private BigDecimal leaveHours;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Constructors
    public PayrollLeave() {}
    
    // Getters and Setters
    public Integer getPayrollLeaveId() { return payrollLeaveId; }
    public void setPayrollLeaveId(Integer payrollLeaveId) { this.payrollLeaveId = payrollLeaveId; }
    
    public Integer getPayrollId() { return payrollId; }
    public void setPayrollId(Integer payrollId) { this.payrollId = payrollId; }
    
    public Integer getLeaveRequestId() { return leaveRequestId; }
    public void setLeaveRequestId(Integer leaveRequestId) { this.leaveRequestId = leaveRequestId; }
    
    public BigDecimal getLeaveHours() { return leaveHours; }
    public void setLeaveHours(BigDecimal leaveHours) { this.leaveHours = leaveHours; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}