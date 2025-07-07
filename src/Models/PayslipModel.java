package Models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Enhanced Model class representing a payslip record in the database.
 * This class works with both the payslip table and monthly_employee_payslip view.
 * Contains detailed breakdown of an employee's pay for a specific period.
 * @author chad
 */
public class PayslipModel {
    
    // Primary key (for table operations)
    private Integer payslipId;
    
    // Employee information
    private String employeeName;
    private Integer employeeId;
    
    // Additional view fields for complete payslip information
    private String payslipNo;  // From view: Payslip No
    private String employeePosition;  // From view: Employee Position
    private String department;  // From view: Department
    
    // Government IDs (from view)
    private String tin;
    private String sssNo;
    private String pagibigNo;
    private String philhealthNo;
    
    // Pay period information
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private LocalDate payDate;  // From view: Pay Date
    
    // Salary information
    private BigDecimal monthlyRate;
    private BigDecimal dailyRate;
    private Integer daysWorked;
    private BigDecimal leavesTaken;  // From view: Leaves Taken
    private BigDecimal overtimeHours;  // From view: Overtime Hours
    
    // Earnings
    private BigDecimal overtime;
    
    // Benefits/Allowances
    private BigDecimal riceSubsidy;
    private BigDecimal phoneAllowance;
    private BigDecimal clothingAllowance;
    
    // Deductions
    private BigDecimal sss;
    private BigDecimal philhealth;
    private BigDecimal pagibig;
    private BigDecimal withholdingTax;
    
    // Totals
    private BigDecimal grossIncome;
    private BigDecimal takeHomePay;
    private BigDecimal totalBenefits;  // From view: TOTAL BENEFITS
    private BigDecimal totalDeductions;  // From view: TOTAL DEDUCTIONS
    private BigDecimal netPay;  // From view: NET PAY
    
    // Foreign keys
    private Integer payPeriodId;
    private Integer payrollId;
    private Integer positionId;
    
    // Default constructor
    public PayslipModel() {
        // Initialize BigDecimal fields to ZERO to prevent null pointer exceptions
        this.monthlyRate = BigDecimal.ZERO;
        this.dailyRate = BigDecimal.ZERO;
        this.overtime = BigDecimal.ZERO;
        this.riceSubsidy = BigDecimal.ZERO;
        this.phoneAllowance = BigDecimal.ZERO;
        this.clothingAllowance = BigDecimal.ZERO;
        this.sss = BigDecimal.ZERO;
        this.philhealth = BigDecimal.ZERO;
        this.pagibig = BigDecimal.ZERO;
        this.withholdingTax = BigDecimal.ZERO;
        this.grossIncome = BigDecimal.ZERO;
        this.takeHomePay = BigDecimal.ZERO;
        this.totalBenefits = BigDecimal.ZERO;
        this.totalDeductions = BigDecimal.ZERO;
        this.netPay = BigDecimal.ZERO;
        this.leavesTaken = BigDecimal.ZERO;
        this.overtimeHours = BigDecimal.ZERO;
        this.daysWorked = 0;
    }
    
    // Constructor with basic information
    public PayslipModel(Integer employeeId, String employeeName, Integer payPeriodId) {
        this();
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.payPeriodId = payPeriodId;
    }
    
    // Getters and Setters for existing fields
    
    public Integer getPayslipId() {
        return payslipId;
    }
    
    public void setPayslipId(Integer payslipId) {
        this.payslipId = payslipId;
    }
    
    public String getEmployeeName() {
        return employeeName;
    }
    
    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }
    
    public Integer getEmployeeId() {
        return employeeId;
    }
    
    public void setEmployeeId(Integer employeeId) {
        this.employeeId = employeeId;
    }
    
    public LocalDate getPeriodStart() {
        return periodStart;
    }
    
    public void setPeriodStart(LocalDate periodStart) {
        this.periodStart = periodStart;
    }
    
    public LocalDate getPeriodEnd() {
        return periodEnd;
    }
    
    public void setPeriodEnd(LocalDate periodEnd) {
        this.periodEnd = periodEnd;
    }
    
    public BigDecimal getMonthlyRate() {
        return monthlyRate;
    }
    
    public void setMonthlyRate(BigDecimal monthlyRate) {
        this.monthlyRate = monthlyRate != null ? monthlyRate : BigDecimal.ZERO;
    }
    
    public BigDecimal getDailyRate() {
        return dailyRate;
    }
    
    public void setDailyRate(BigDecimal dailyRate) {
        this.dailyRate = dailyRate != null ? dailyRate : BigDecimal.ZERO;
    }
    
    public Integer getDaysWorked() {
        return daysWorked;
    }
    
    public void setDaysWorked(Integer daysWorked) {
        this.daysWorked = daysWorked != null ? daysWorked : 0;
    }
    
    public BigDecimal getOvertime() {
        return overtime;
    }
    
    public void setOvertime(BigDecimal overtime) {
        this.overtime = overtime != null ? overtime : BigDecimal.ZERO;
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
    
    public BigDecimal getSss() {
        return sss;
    }
    
    public void setSss(BigDecimal sss) {
        this.sss = sss != null ? sss : BigDecimal.ZERO;
    }
    
    public BigDecimal getPhilhealth() {
        return philhealth;
    }
    
    public void setPhilhealth(BigDecimal philhealth) {
        this.philhealth = philhealth != null ? philhealth : BigDecimal.ZERO;
    }
    
    public BigDecimal getPagibig() {
        return pagibig;
    }
    
    public void setPagibig(BigDecimal pagibig) {
        this.pagibig = pagibig != null ? pagibig : BigDecimal.ZERO;
    }
    
    public BigDecimal getWithholdingTax() {
        return withholdingTax;
    }
    
    public void setWithholdingTax(BigDecimal withholdingTax) {
        this.withholdingTax = withholdingTax != null ? withholdingTax : BigDecimal.ZERO;
    }
    
    public BigDecimal getGrossIncome() {
        return grossIncome;
    }
    
    public void setGrossIncome(BigDecimal grossIncome) {
        this.grossIncome = grossIncome != null ? grossIncome : BigDecimal.ZERO;
    }
    
    public BigDecimal getTakeHomePay() {
        return takeHomePay;
    }
    
    public void setTakeHomePay(BigDecimal takeHomePay) {
        this.takeHomePay = takeHomePay != null ? takeHomePay : BigDecimal.ZERO;
    }
    
    public Integer getPayPeriodId() {
        return payPeriodId;
    }
    
    public void setPayPeriodId(Integer payPeriodId) {
        this.payPeriodId = payPeriodId;
    }
    
    public Integer getPayrollId() {
        return payrollId;
    }
    
    public void setPayrollId(Integer payrollId) {
        this.payrollId = payrollId;
    }
    
    public Integer getPositionId() {
        return positionId;
    }
    
    public void setPositionId(Integer positionId) {
        this.positionId = positionId;
    }
    
    // NEW GETTERS AND SETTERS for view fields
    
    public String getPayslipNo() {
        return payslipNo;
    }
    
    public void setPayslipNo(String payslipNo) {
        this.payslipNo = payslipNo;
    }
    
    public String getEmployeePosition() {
        return employeePosition;
    }
    
    public void setEmployeePosition(String employeePosition) {
        this.employeePosition = employeePosition;
    }
    
    public String getDepartment() {
        return department;
    }
    
    public void setDepartment(String department) {
        this.department = department;
    }
    
    public String getTin() {
        return tin;
    }
    
    public void setTin(String tin) {
        this.tin = tin;
    }
    
    public String getSssNo() {
        return sssNo;
    }
    
    public void setSssNo(String sssNo) {
        this.sssNo = sssNo;
    }
    
    public String getPagibigNo() {
        return pagibigNo;
    }
    
    public void setPagibigNo(String pagibigNo) {
        this.pagibigNo = pagibigNo;
    }
    
    public String getPhilhealthNo() {
        return philhealthNo;
    }
    
    public void setPhilhealthNo(String philhealthNo) {
        this.philhealthNo = philhealthNo;
    }
    
    public LocalDate getPayDate() {
        return payDate;
    }
    
    public void setPayDate(LocalDate payDate) {
        this.payDate = payDate;
    }
    
    public BigDecimal getLeavesTaken() {
        return leavesTaken;
    }
    
    public void setLeavesTaken(BigDecimal leavesTaken) {
        this.leavesTaken = leavesTaken != null ? leavesTaken : BigDecimal.ZERO;
    }
    
    public BigDecimal getOvertimeHours() {
        return overtimeHours;
    }
    
    public void setOvertimeHours(BigDecimal overtimeHours) {
        this.overtimeHours = overtimeHours != null ? overtimeHours : BigDecimal.ZERO;
    }
    
    public BigDecimal getTotalBenefits() {
        return totalBenefits;
    }
    
    public void setTotalBenefits(BigDecimal totalBenefits) {
        this.totalBenefits = totalBenefits != null ? totalBenefits : BigDecimal.ZERO;
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
    
    // NEW METHODS for view compatibility
    
    // Alias methods for backward compatibility with existing table operations
    public void setPeriodStartDate(LocalDate periodStartDate) {
        this.periodStart = periodStartDate;
    }
    
    public LocalDate getPeriodStartDate() {
        return this.periodStart;
    }
    
    public void setPeriodEndDate(LocalDate periodEndDate) {
        this.periodEnd = periodEndDate;
    }
    
    public LocalDate getPeriodEndDate() {
        return this.periodEnd;
    }
    
    // Alias methods for deduction fields (view uses different names)
    public void setSssDeduction(BigDecimal sssDeduction) {
        this.sss = sssDeduction;
    }
    
    public BigDecimal getSssDeduction() {
        return this.sss;
    }
    
    public void setPhilhealthDeduction(BigDecimal philhealthDeduction) {
        this.philhealth = philhealthDeduction;
    }
    
    public BigDecimal getPhilhealthDeduction() {
        return this.philhealth;
    }
    
    public void setPagibigDeduction(BigDecimal pagibigDeduction) {
        this.pagibig = pagibigDeduction;
    }
    
    public BigDecimal getPagibigDeduction() {
        return this.pagibig;
    }
    
    // Utility methods (existing + enhanced)
    
    /**
     * Returns formatted pay period string (e.g., "January 1, 2024 - January 31, 2024")
     * @return Formatted pay period string
     */
    public String getFormattedPayPeriod() {
        if (periodStart != null && periodEnd != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy");
            return periodStart.format(formatter) + " - " + periodEnd.format(formatter);
        }
        return "N/A";
    }
    
    /**
     * Calculates total deductions (uses existing fields first, falls back to totalDeductions)
     * @return Total deductions amount
     */
    public BigDecimal calculateTotalDeductions() {
        if (totalDeductions != null && totalDeductions.compareTo(BigDecimal.ZERO) > 0) {
            return totalDeductions; // Use view calculation if available
        }
        return sss.add(philhealth).add(pagibig).add(withholdingTax); // Fallback to manual calculation
    }
    
    /**
     * Calculates total benefits/allowances (uses existing fields first, falls back to totalBenefits)
     * @return Total benefits amount
     */
    public BigDecimal calculateTotalBenefits() {
        if (totalBenefits != null && totalBenefits.compareTo(BigDecimal.ZERO) > 0) {
            return totalBenefits; // Use view calculation if available
        }
        return riceSubsidy.add(phoneAllowance).add(clothingAllowance); // Fallback to manual calculation
    }
    
    /**
     * Calculates basic pay (days worked * daily rate)
     * @return Basic pay amount
     */
    public BigDecimal getBasicPay() {
        return dailyRate.multiply(new BigDecimal(daysWorked));
    }
    
    /**
     * Checks if this is a rank-and-file employee based on department
     * @return true if rank-and-file employee
     */
    public boolean isRankAndFile() {
        if (department == null) return false;
        String dept = department.toLowerCase();
        String pos = employeePosition != null ? employeePosition.toLowerCase() : "";
        
        return dept.equals("rank-and-file") || 
               (pos.contains("rank") && pos.contains("file"));
    }
    
    /**
     * Checks if this payslip has any overtime hours
     * @return true if employee worked overtime
     */
    public boolean hasOvertime() {
        return (overtimeHours != null && overtimeHours.compareTo(BigDecimal.ZERO) > 0) ||
               (overtime != null && overtime.compareTo(BigDecimal.ZERO) > 0);
    }
    
    /**
     * Checks if this payslip has any leave days taken
     * @return true if employee took leaves
     */
    public boolean hasLeaves() {
        return leavesTaken != null && leavesTaken.compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * Gets the effective working days (days worked + leaves taken)
     * @return Total effective working days
     */
    public BigDecimal getEffectiveWorkingDays() {
        BigDecimal days = new BigDecimal(daysWorked);
        return days.add(leavesTaken);
    }
    
    /**
     * Calculates take-home pay (prioritizes netPay from view, falls back to calculation)
     * @return Take-home pay amount
     */
    public BigDecimal calculateTakeHomePay() {
        if (netPay != null && netPay.compareTo(BigDecimal.ZERO) > 0) {
            return netPay; // Use view calculation if available
        }
        if (takeHomePay != null && takeHomePay.compareTo(BigDecimal.ZERO) > 0) {
            return takeHomePay; // Use existing field
        }
        // Fallback calculation
        BigDecimal totalEarnings = grossIncome.add(calculateTotalBenefits());
        return totalEarnings.subtract(calculateTotalDeductions());
    }
    
    /**
     * Gets formatted period string (e.g., "January 1-31, 2025") for view compatibility
     * @return Formatted pay period string
     */
    public String getFormattedPeriod() {
        return getFormattedPayPeriod();
    }
    
    /**
     * Validates that all required fields are set
     * @return true if payslip is valid
     */
    public boolean isValid() {
        return employeeId != null && 
               employeeName != null && !employeeName.trim().isEmpty() &&
               payPeriodId != null &&
               periodStart != null &&
               periodEnd != null &&
               monthlyRate != null &&
               grossIncome != null &&
               (takeHomePay != null || netPay != null);
    }
    
    @Override
    public String toString() {
        return "PayslipModel{" +
                "payslipId=" + payslipId +
                ", payslipNo='" + payslipNo + '\'' +
                ", employeeName='" + employeeName + '\'' +
                ", employeeId=" + employeeId +
                ", payPeriodId=" + payPeriodId +
                ", periodStart=" + periodStart +
                ", periodEnd=" + periodEnd +
                ", grossIncome=" + grossIncome +
                ", netPay=" + netPay +
                ", takeHomePay=" + takeHomePay +
                ", daysWorked=" + daysWorked +
                '}';
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        PayslipModel that = (PayslipModel) obj;
        return payslipId != null ? payslipId.equals(that.payslipId) : that.payslipId == null;
    }
    
    @Override
    public int hashCode() {
        return payslipId != null ? payslipId.hashCode() : 0;
    }
}