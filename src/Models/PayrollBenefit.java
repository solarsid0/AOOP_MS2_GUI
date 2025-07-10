package Models;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PayrollBenefit {
    private Integer payrollBenefitId;
    private Integer payrollId;
    private Integer benefitTypeId;
    private BigDecimal benefitAmount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Constructors
    public PayrollBenefit() {}
    
    // Getters and Setters
    public Integer getPayrollBenefitId() { return payrollBenefitId; }
    public void setPayrollBenefitId(Integer payrollBenefitId) { this.payrollBenefitId = payrollBenefitId; }
    
    public Integer getPayrollId() { return payrollId; }
    public void setPayrollId(Integer payrollId) { this.payrollId = payrollId; }
    
    public Integer getBenefitTypeId() { return benefitTypeId; }
    public void setBenefitTypeId(Integer benefitTypeId) { this.benefitTypeId = benefitTypeId; }
    
    public BigDecimal getBenefitAmount() { return benefitAmount; }
    public void setBenefitAmount(BigDecimal benefitAmount) { this.benefitAmount = benefitAmount; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
