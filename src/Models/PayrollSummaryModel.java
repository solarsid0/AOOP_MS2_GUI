
package Models;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Model class representing a payroll summary record from the monthly_payroll_summary_report view.
 * This class maps exactly to the database view columns for payroll reporting.
 * @author chad
 */


public class PayrollSummaryModel {
 // Basic information
    private LocalDate payDate;
    private String employeeId;
    private String employeeName;
    private String position;
    private String department;
    
    // Salary components
    private BigDecimal baseSalary;
    private BigDecimal leaves;
    private BigDecimal overtime;
    private BigDecimal grossIncome;
    
    // Benefits
    private BigDecimal riceSubsidy;
    private BigDecimal phoneAllowance;
    private BigDecimal clothingAllowance;
    private BigDecimal totalBenefits;
    
    // Government contributions and deductions
    private String socialSecurityNo;
    private BigDecimal socialSecurityContribution;
    private String philhealthNo;
    private BigDecimal philhealthContribution;
    private String pagIbigNo;
    private BigDecimal pagIbigContribution;
    private String tin;
    private BigDecimal withholdingTax;
    private BigDecimal totalDeductions;
    
    // Final amount
    private BigDecimal netPay;
    
    // Default constructor
    public PayrollSummaryModel() {
        // Initialize BigDecimal fields to ZERO to prevent null pointer exceptions
        this.baseSalary = BigDecimal.ZERO;
        this.leaves = BigDecimal.ZERO;
        this.overtime = BigDecimal.ZERO;
        this.grossIncome = BigDecimal.ZERO;
        this.riceSubsidy = BigDecimal.ZERO;
        this.phoneAllowance = BigDecimal.ZERO;
        this.clothingAllowance = BigDecimal.ZERO;
        this.totalBenefits = BigDecimal.ZERO;
        this.socialSecurityContribution = BigDecimal.ZERO;
        this.philhealthContribution = BigDecimal.ZERO;
        this.pagIbigContribution = BigDecimal.ZERO;
        this.withholdingTax = BigDecimal.ZERO;
        this.totalDeductions = BigDecimal.ZERO;
        this.netPay = BigDecimal.ZERO;
    }
    
    // Constructor with basic information
    public PayrollSummaryModel(LocalDate payDate, String employeeId, String employeeName) {
        this();
        this.payDate = payDate;
        this.employeeId = employeeId;
        this.employeeName = employeeName;
    }
    
    // Getters and Setters
    
    public LocalDate getPayDate() {
        return payDate;
    }
    
    public void setPayDate(LocalDate payDate) {
        this.payDate = payDate;
    }
    
    public String getEmployeeId() {
        return employeeId;
    }
    
    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }
    
    public String getEmployeeName() {
        return employeeName;
    }
    
    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }
    
    public String getPosition() {
        return position;
    }
    
    public void setPosition(String position) {
        this.position = position;
    }
    
    public String getDepartment() {
        return department;
    }
    
    public void setDepartment(String department) {
        this.department = department;
    }
    
    public BigDecimal getBaseSalary() {
        return baseSalary;
    }
    
    public void setBaseSalary(BigDecimal baseSalary) {
        this.baseSalary = baseSalary != null ? baseSalary : BigDecimal.ZERO;
    }
    
    public BigDecimal getLeaves() {
        return leaves;
    }
    
    public void setLeaves(BigDecimal leaves) {
        this.leaves = leaves != null ? leaves : BigDecimal.ZERO;
    }
    
    public BigDecimal getOvertime() {
        return overtime;
    }
    
    public void setOvertime(BigDecimal overtime) {
        this.overtime = overtime != null ? overtime : BigDecimal.ZERO;
    }
    
    public BigDecimal getGrossIncome() {
        return grossIncome;
    }
    
    public void setGrossIncome(BigDecimal grossIncome) {
        this.grossIncome = grossIncome != null ? grossIncome : BigDecimal.ZERO;
    }
    
    public BigDecimal getRiceSubsidy() {
        return riceSubsidy;
    }
    
    public void setRiceSubsidy(BigDecimal riceSubsidy) {
        this.riceSubsidy = riceSubsidy != null ? riceSubsidy : BigDecimal.ZERO;
    }
    
    public BigDecimal getPhoneAllowance() {
        return phoneAllowance;
    }
    
    public void setPhoneAllowance(BigDecimal phoneAllowance) {
        this.phoneAllowance = phoneAllowance != null ? phoneAllowance : BigDecimal.ZERO;
    }
    
    public BigDecimal getClothingAllowance() {
        return clothingAllowance;
    }
    
    public void setClothingAllowance(BigDecimal clothingAllowance) {
        this.clothingAllowance = clothingAllowance != null ? clothingAllowance : BigDecimal.ZERO;
    }
    
    public BigDecimal getTotalBenefits() {
        return totalBenefits;
    }
    
    public void setTotalBenefits(BigDecimal totalBenefits) {
        this.totalBenefits = totalBenefits != null ? totalBenefits : BigDecimal.ZERO;
    }
    
    public String getSocialSecurityNo() {
        return socialSecurityNo;
    }
    
    public void setSocialSecurityNo(String socialSecurityNo) {
        this.socialSecurityNo = socialSecurityNo;
    }
    
    public BigDecimal getSocialSecurityContribution() {
        return socialSecurityContribution;
    }
    
    public void setSocialSecurityContribution(BigDecimal socialSecurityContribution) {
        this.socialSecurityContribution = socialSecurityContribution != null ? socialSecurityContribution : BigDecimal.ZERO;
    }
    
    public String getPhilhealthNo() {
        return philhealthNo;
    }
    
    public void setPhilhealthNo(String philhealthNo) {
        this.philhealthNo = philhealthNo;
    }
    
    public BigDecimal getPhilhealthContribution() {
        return philhealthContribution;
    }
    
    public void setPhilhealthContribution(BigDecimal philhealthContribution) {
        this.philhealthContribution = philhealthContribution != null ? philhealthContribution : BigDecimal.ZERO;
    }
    
    public String getPagIbigNo() {
        return pagIbigNo;
    }
    
    public void setPagIbigNo(String pagIbigNo) {
        this.pagIbigNo = pagIbigNo;
    }
    
    public BigDecimal getPagIbigContribution() {
        return pagIbigContribution;
    }
    
    public void setPagIbigContribution(BigDecimal pagIbigContribution) {
        this.pagIbigContribution = pagIbigContribution != null ? pagIbigContribution : BigDecimal.ZERO;
    }
    
    public String getTin() {
        return tin;
    }
    
    public void setTin(String tin) {
        this.tin = tin;
    }
    
    public BigDecimal getWithholdingTax() {
        return withholdingTax;
    }
    
    public void setWithholdingTax(BigDecimal withholdingTax) {
        this.withholdingTax = withholdingTax != null ? withholdingTax : BigDecimal.ZERO;
    }
    
    public BigDecimal getTotalDeductions() {
        return totalDeductions;
    }
    
    public void setTotalDeductions(BigDecimal totalDeductions) {
        this.totalDeductions = totalDeductions != null ? totalDeductions : BigDecimal.ZERO;
    }
    
    public BigDecimal getNetPay() {
        return netPay;
    }
    
    public void setNetPay(BigDecimal netPay) {
        this.netPay = netPay != null ? netPay : BigDecimal.ZERO;
    }
    
    // Utility methods
    
    /**
     * Checks if this is a rank-and-file employee based on department
     * @return true if rank-and-file employee
     */
    public boolean isRankAndFile() {
        if (department == null) return false;
        String dept = department.toLowerCase();
        String pos = position != null ? position.toLowerCase() : "";
        
        return dept.equals("rank-and-file") || 
               (pos.contains("rank") && pos.contains("file"));
    }
    
    /**
     * Checks if this is a summary row (total row)
     * @return true if this is a total/summary row
     */
    public boolean isTotalRow() {
        return "TOTAL".equals(employeeId);
    }
    
    /**
     * Calculates total earnings (gross income + benefits)
     * @return Total earnings before deductions
     */
    public BigDecimal getTotalEarnings() {
        return grossIncome.add(totalBenefits);
    }
    
    /**
     * Gets formatted employee info for display
     * @return Formatted string like "10001 - Doe, John"
     */
    public String getFormattedEmployeeInfo() {
        if (isTotalRow()) {
            return "TOTAL SUMMARY";
        }
        return String.format("%s - %s", employeeId, employeeName);
    }
    
    @Override
    public String toString() {
        return "PayrollSummaryModel{" +
                "employeeId='" + employeeId + '\'' +
                ", employeeName='" + employeeName + '\'' +
                ", position='" + position + '\'' +
                ", department='" + department + '\'' +
                ", grossIncome=" + grossIncome +
                ", totalBenefits=" + totalBenefits +
                ", totalDeductions=" + totalDeductions +
                ", netPay=" + netPay +
                ", payDate=" + payDate +
                '}';
    }
}
