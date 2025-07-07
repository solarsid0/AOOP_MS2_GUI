package Models;

import java.math.BigDecimal;

/**
 * PayrollBenefit - Simple model class for payrollbenefit table
 * Junction table linking payroll records with benefit types and amounts
 * Fields: payrollBenefitId, benefitAmount, payrollId, benefitTypeId
 * @author Chad
 */
public class PayrollBenefit {
    
    private Integer payrollBenefitId;
    private BigDecimal benefitAmount;
    private Integer payrollId;
    private Integer benefitTypeId;
    
    // ===============================
    // CONSTRUCTORS
    // ===============================
    
    /**
     * Default constructor - REQUIRED for DAO operations
     */
    public PayrollBenefit() {}
    
    /**
     * Constructor with essential fields
     */
    public PayrollBenefit(Integer payrollId, Integer benefitTypeId, BigDecimal benefitAmount) {
        this.payrollId = payrollId;
        this.benefitTypeId = benefitTypeId;
        this.benefitAmount = benefitAmount;
    }
    
    // ===============================
    // GETTERS AND SETTERS - REQUIRED FOR DAO
    // ===============================
    
    public Integer getPayrollBenefitId() {
        return payrollBenefitId;
    }
    
    public void setPayrollBenefitId(Integer payrollBenefitId) {
        this.payrollBenefitId = payrollBenefitId;
    }
    
    public BigDecimal getBenefitAmount() {
        return benefitAmount;
    }
    
    public void setBenefitAmount(BigDecimal benefitAmount) {
        this.benefitAmount = benefitAmount;
    }
    
    public Integer getPayrollId() {
        return payrollId;
    }
    
    public void setPayrollId(Integer payrollId) {
        this.payrollId = payrollId;
    }
    
    public Integer getBenefitTypeId() {
        return benefitTypeId;
    }
    
    public void setBenefitTypeId(Integer benefitTypeId) {
        this.benefitTypeId = benefitTypeId;
    }
    
    // ===============================
    // UTILITY METHODS
    // ===============================
    
    public boolean isValid() {
        return payrollId != null && benefitTypeId != null && 
               benefitAmount != null && benefitAmount.compareTo(BigDecimal.ZERO) >= 0;
    }
    
    @Override
    public String toString() {
        return "PayrollBenefit{" +
                "payrollBenefitId=" + payrollBenefitId +
                ", payrollId=" + payrollId +
                ", benefitTypeId=" + benefitTypeId +
                ", benefitAmount=" + benefitAmount +
                '}';
    }
}