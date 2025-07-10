package Models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Model class representing a payroll record in the database.
 * This class contains all the financial information for an employee's pay period.
 * @author User
 */
public class PayrollModel {
    
    // Primary key
    private Integer payrollId;
    
    // Financial information
    private BigDecimal basicSalary;
    private BigDecimal grossIncome;
    private BigDecimal totalBenefit;
    private BigDecimal totalDeduction;
    private BigDecimal netSalary;
    
    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Foreign keys
    private Integer payPeriodId;
    private Integer employeeId;
    
    // Default constructor
    public PayrollModel() {
        // Initialize BigDecimal fields to ZERO to prevent null pointer exceptions
        this.basicSalary = BigDecimal.ZERO;
        this.grossIncome = BigDecimal.ZERO;
        this.totalBenefit = BigDecimal.ZERO;
        this.totalDeduction = BigDecimal.ZERO;
        this.netSalary = BigDecimal.ZERO;
    }
    
    // Constructor with basic information
    public PayrollModel(Integer employeeId, Integer payPeriodId, BigDecimal basicSalary) {
        this();
        this.employeeId = employeeId;
        this.payPeriodId = payPeriodId;
        this.basicSalary = basicSalary != null ? basicSalary : BigDecimal.ZERO;
    }
    
    // Getters and Setters
    
    public Integer getPayrollId() {
        return payrollId;
    }
    
    public void setPayrollId(Integer payrollId) {
        this.payrollId = payrollId;
    }
    
    public BigDecimal getBasicSalary() {
        return basicSalary;
    }
    
    public void setBasicSalary(BigDecimal basicSalary) {
        this.basicSalary = basicSalary != null ? basicSalary : BigDecimal.ZERO;
    }
    
    public BigDecimal getGrossIncome() {
        return grossIncome;
    }
    
    public void setGrossIncome(BigDecimal grossIncome) {
        this.grossIncome = grossIncome != null ? grossIncome : BigDecimal.ZERO;
    }
    
    public BigDecimal getTotalBenefit() {
        return totalBenefit;
    }
    
    public void setTotalBenefit(BigDecimal totalBenefit) {
        this.totalBenefit = totalBenefit != null ? totalBenefit : BigDecimal.ZERO;
    }
    
    public BigDecimal getTotalDeduction() {
        return totalDeduction;
    }
    
    public void setTotalDeduction(BigDecimal totalDeduction) {
        this.totalDeduction = totalDeduction != null ? totalDeduction : BigDecimal.ZERO;
    }
    
    public BigDecimal getNetSalary() {
        return netSalary;
    }
    
    public void setNetSalary(BigDecimal netSalary) {
        this.netSalary = netSalary != null ? netSalary : BigDecimal.ZERO;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public Integer getPayPeriodId() {
        return payPeriodId;
    }
    
    public void setPayPeriodId(Integer payPeriodId) {
        this.payPeriodId = payPeriodId;
    }
    
    public Integer getEmployeeId() {
        return employeeId;
    }
    
    public void setEmployeeId(Integer employeeId) {
        this.employeeId = employeeId;
    }
    
    // Utility methods
    
    /**
     * Calculates the gross income as basic salary + benefits
     */
    public void calculateGrossIncome() {
        this.grossIncome = this.basicSalary.add(this.totalBenefit);
    }
    
    /**
     * Calculates the net salary as gross income - total deductions
     */
    public void calculateNetSalary() {
        this.netSalary = this.grossIncome.subtract(this.totalDeduction);
    }
    
    /**
     * Validates that all required fields are set
     * @return true if valid, false otherwise
     */
    public boolean isValid() {
        return employeeId != null && 
               payPeriodId != null && 
               basicSalary != null && 
               grossIncome != null && 
               totalDeduction != null && 
               netSalary != null;
    }
    
    @Override
    public String toString() {
        return "PayrollModel{" +
                "payrollId=" + payrollId +
                ", employeeId=" + employeeId +
                ", payPeriodId=" + payPeriodId +
                ", basicSalary=" + basicSalary +
                ", grossIncome=" + grossIncome +
                ", totalBenefit=" + totalBenefit +
                ", totalDeduction=" + totalDeduction +
                ", netSalary=" + netSalary +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        PayrollModel that = (PayrollModel) obj;
        return payrollId != null ? payrollId.equals(that.payrollId) : that.payrollId == null;
    }
    
    @Override
    public int hashCode() {
        return payrollId != null ? payrollId.hashCode() : 0;
    }
}