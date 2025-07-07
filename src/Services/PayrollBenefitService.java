
package Services;

import DAOs.DatabaseConnection;
import DAOs.EmployeeDAO;
import DAOs.ReferenceDataDAO;
import Models.EmployeeModel;
import java.sql.*;
import java.time.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * PayrollBenefit integrates employee benefits with payroll calculations
 * Handles allowances, bonuses, government benefits, insurance, and benefit-based pay adjustments
 * Links employee benefits system to payroll processing
 * @author Chadley
 */

public class PayrollBenefitService {
 private final DatabaseConnection databaseConnection;
    private final EmployeeDAO employeeDAO;
    private final ReferenceDataDAO referenceDataDAO;
    
    // Benefit calculation constants
    private static final BigDecimal SSS_EMPLOYEE_RATE = new BigDecimal("0.045"); // 4.5%
    private static final BigDecimal SSS_EMPLOYER_RATE = new BigDecimal("0.095"); // 9.5%
    private static final BigDecimal PHILHEALTH_EMPLOYEE_RATE = new BigDecimal("0.0225"); // 2.25%
    private static final BigDecimal PHILHEALTH_EMPLOYER_RATE = new BigDecimal("0.0225"); // 2.25%
    private static final BigDecimal PAGIBIG_EMPLOYEE_RATE = new BigDecimal("0.02"); // 2%
    private static final BigDecimal PAGIBIG_EMPLOYER_RATE = new BigDecimal("0.02"); // 2%
    private static final BigDecimal SSS_MAX_SALARY = new BigDecimal("25000");
    private static final BigDecimal PHILHEALTH_MAX_SALARY = new BigDecimal("100000");
    private static final BigDecimal PAGIBIG_MAX_CONTRIBUTION = new BigDecimal("200");
    
    /**
     * Constructor
     * @param databaseConnection Database connection instance
     */
    public PayrollBenefitService(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
        this.employeeDAO = new EmployeeDAO(databaseConnection);
        this.referenceDataDAO = new ReferenceDataDAO(databaseConnection);
    }
    
    /**
     * Calculates benefit-related payroll adjustments for an employee in a specific pay period
     * @param employeeId Employee ID
     * @param payPeriodId Pay period ID
     * @return PayrollBenefitResult with all benefit-based calculations
     */
    public PayrollBenefitResult calculateBenefitPayroll(Integer employeeId, Integer payPeriodId) {
        try {
            // Get employee details
            EmployeeModel employee = employeeDAO.findById(employeeId);
            if (employee == null) {
                throw new IllegalArgumentException("Employee not found: " + employeeId);
            }
            
            // Get pay period dates
            Map<String, Object> payPeriod = getPayPeriodDetails(payPeriodId);
            if (payPeriod == null) {
                throw new IllegalArgumentException("Pay period not found: " + payPeriodId);
            }
            
            LocalDate startDate = ((java.sql.Date) payPeriod.get("startDate")).toLocalDate();
            LocalDate endDate = ((java.sql.Date) payPeriod.get("endDate")).toLocalDate();
            
            // Initialize calculation result
            PayrollBenefitResult result = new PayrollBenefitResult();
            result.setEmployeeId(employeeId);
            result.setPayPeriodId(payPeriodId);
            result.setStartDate(startDate);
            result.setEndDate(endDate);
            
            // Calculate different benefit components
            calculateGovernmentBenefits(result, employee);
            calculateAllowances(result, employee, payPeriodId);
            calculateBonuses(result, employee, payPeriodId);
            calculateInsuranceBenefits(result, employee);
            calculatePerformanceBenefits(result, employee, payPeriodId);
            calculateSpecialBenefits(result, employee, startDate, endDate);
            
            // Calculate totals
            BigDecimal totalBenefitPayments = result.getRiceSubsidy()
                .add(result.getPhoneAllowance())
                .add(result.getClothingAllowance())
                .add(result.getTransportationAllowance())
                .add(result.getMealAllowance())
                .add(result.getPerformanceBonus())
                .add(result.getHolidayBonus())
                .add(result.getOvertimeAllowance())
                .add(result.getSpecialAllowance());
            
            BigDecimal totalBenefitDeductions = result.getSssEmployeeContribution()
                .add(result.getPhilhealthEmployeeContribution())
                .add(result.getPagibigEmployeeContribution())
                .add(result.getHealthInsurancePremium())
                .add(result.getLifeInsurancePremium());
            
            BigDecimal netBenefitAdjustment = totalBenefitPayments.subtract(totalBenefitDeductions);
            
            result.setTotalBenefitPayments(totalBenefitPayments);
            result.setTotalBenefitDeductions(totalBenefitDeductions);
            result.setNetBenefitAdjustment(netBenefitAdjustment);
            
            System.out.println("Benefit payroll calculated for employee " + employeeId + 
                             " for period " + startDate + " to " + endDate);
            System.out.println("Net benefit adjustment: " + netBenefitAdjustment);
            
            return result;
            
        } catch (Exception e) {
            System.err.println("Error calculating benefit payroll: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Calculates government-mandated benefits (SSS, PhilHealth, Pag-IBIG)
     */
    private void calculateGovernmentBenefits(PayrollBenefitResult result, EmployeeModel employee) {
        BigDecimal monthlySalary = employee.getBasicSalary();
        if (monthlySalary == null) {
            monthlySalary = BigDecimal.ZERO;
        }
        
        // SSS Calculation
        BigDecimal sssContributionBase = monthlySalary.min(SSS_MAX_SALARY);
        BigDecimal sssEmployeeContribution = sssContributionBase.multiply(SSS_EMPLOYEE_RATE);
        BigDecimal sssEmployerContribution = sssContributionBase.multiply(SSS_EMPLOYER_RATE);
        
        // PhilHealth Calculation
        BigDecimal philhealthContributionBase = monthlySalary.min(PHILHEALTH_MAX_SALARY);
        BigDecimal philhealthEmployeeContribution = philhealthContributionBase.multiply(PHILHEALTH_EMPLOYEE_RATE);
        BigDecimal philhealthEmployerContribution = philhealthContributionBase.multiply(PHILHEALTH_EMPLOYER_RATE);
        
        // Pag-IBIG Calculation
        BigDecimal pagibigEmployeeContribution = monthlySalary.multiply(PAGIBIG_EMPLOYEE_RATE).min(PAGIBIG_MAX_CONTRIBUTION);
        BigDecimal pagibigEmployerContribution = monthlySalary.multiply(PAGIBIG_EMPLOYER_RATE).min(PAGIBIG_MAX_CONTRIBUTION);
        
        result.setSssEmployeeContribution(sssEmployeeContribution);
        result.setSssEmployerContribution(sssEmployerContribution);
        result.setPhilhealthEmployeeContribution(philhealthEmployeeContribution);
        result.setPhilhealthEmployerContribution(philhealthEmployerContribution);
        result.setPagibigEmployeeContribution(pagibigEmployeeContribution);
        result.setPagibigEmployerContribution(pagibigEmployerContribution);
    }
    
    /**
     * Calculates allowances (rice, phone, clothing, transportation, meal)
     */
    private void calculateAllowances(PayrollBenefitResult result, EmployeeModel employee, Integer payPeriodId) {
        // Get employee's benefit entitlements
        List<Map<String, Object>> employeeBenefits = getEmployeeBenefits(employee.getEmployeeId());
        
        BigDecimal riceSubsidy = BigDecimal.ZERO;
        BigDecimal phoneAllowance = BigDecimal.ZERO;
        BigDecimal clothingAllowance = BigDecimal.ZERO;
        BigDecimal transportationAllowance = BigDecimal.ZERO;
        BigDecimal mealAllowance = BigDecimal.ZERO;
        
        for (Map<String, Object> benefit : employeeBenefits) {
            String benefitName = (String) benefit.get("benefitName");
            BigDecimal amount = (BigDecimal) benefit.get("amount");
            
            if (amount != null && benefitName != null) {
                switch (benefitName.toLowerCase()) {
                    case "rice subsidy":
                    case "rice allowance":
                        riceSubsidy = riceSubsidy.add(amount);
                        break;
                    case "phone allowance":
                    case "communication allowance":
                        phoneAllowance = phoneAllowance.add(amount);
                        break;
                    case "clothing allowance":
                    case "uniform allowance":
                        clothingAllowance = clothingAllowance.add(amount);
                        break;
                    case "transportation allowance":
                    case "travel allowance":
                        transportationAllowance = transportationAllowance.add(amount);
                        break;
                    case "meal allowance":
                    case "food allowance":
                        mealAllowance = mealAllowance.add(amount);
                        break;
                }
            }
        }
        
        result.setRiceSubsidy(riceSubsidy);
        result.setPhoneAllowance(phoneAllowance);
        result.setClothingAllowance(clothingAllowance);
        result.setTransportationAllowance(transportationAllowance);
        result.setMealAllowance(mealAllowance);
    }
    
    /**
     * Calculates bonuses (performance, holiday, 13th month)
     */
    private void calculateBonuses(PayrollBenefitResult result, EmployeeModel employee, Integer payPeriodId) {
        BigDecimal performanceBonus = BigDecimal.ZERO;
        BigDecimal holidayBonus = BigDecimal.ZERO;
        BigDecimal thirteenthMonthPay = BigDecimal.ZERO;
        
        // Get period details to determine if bonuses apply
        Map<String, Object> payPeriod = getPayPeriodDetails(payPeriodId);
        if (payPeriod != null) {
            LocalDate endDate = ((java.sql.Date) payPeriod.get("endDate")).toLocalDate();
            
            // Check for 13th month pay (usually December)
            if (endDate.getMonthValue() == 12) {
                thirteenthMonthPay = calculateThirteenthMonthPay(employee, endDate.getYear());
            }
            
            // Check for holiday bonuses
            holidayBonus = calculateHolidayBonus(employee, endDate);
            
            // Get performance bonus from employee benefits
            performanceBonus = getPerformanceBonus(employee.getEmployeeId(), payPeriodId);
        }
        
        result.setPerformanceBonus(performanceBonus);
        result.setHolidayBonus(holidayBonus);
        result.setThirteenthMonthPay(thirteenthMonthPay);
    }
    
    /**
     * Calculates insurance benefits (health, life insurance)
     */
    private void calculateInsuranceBenefits(PayrollBenefitResult result, EmployeeModel employee) {
        // Get insurance enrollment details
        List<Map<String, Object>> insuranceBenefits = getEmployeeInsurance(employee.getEmployeeId());
        
        BigDecimal healthInsurancePremium = BigDecimal.ZERO;
        BigDecimal lifeInsurancePremium = BigDecimal.ZERO;
        BigDecimal healthInsuranceCompanyCoverage = BigDecimal.ZERO;
        BigDecimal lifeInsuranceCompanyCoverage = BigDecimal.ZERO;
        
        for (Map<String, Object> insurance : insuranceBenefits) {
            String insuranceType = (String) insurance.get("insuranceType");
            BigDecimal employeePremium = (BigDecimal) insurance.get("employeePremium");
            BigDecimal companyContribution = (BigDecimal) insurance.get("companyContribution");
            
            if (insuranceType != null) {
                switch (insuranceType.toLowerCase()) {
                    case "health insurance":
                    case "medical insurance":
                        if (employeePremium != null) {
                            healthInsurancePremium = healthInsurancePremium.add(employeePremium);
                        }
                        if (companyContribution != null) {
                            healthInsuranceCompanyCoverage = healthInsuranceCompanyCoverage.add(companyContribution);
                        }
                        break;
                    case "life insurance":
                        if (employeePremium != null) {
                            lifeInsurancePremium = lifeInsurancePremium.add(employeePremium);
                        }
                        if (companyContribution != null) {
                            lifeInsuranceCompanyCoverage = lifeInsuranceCompanyCoverage.add(companyContribution);
                        }
                        break;
                }
            }
        }
        
        result.setHealthInsurancePremium(healthInsurancePremium);
        result.setLifeInsurancePremium(lifeInsurancePremium);
        result.setHealthInsuranceCompanyCoverage(healthInsuranceCompanyCoverage);
        result.setLifeInsuranceCompanyCoverage(lifeInsuranceCompanyCoverage);
    }
    
    /**
     * Calculates performance-based benefits
     */
    private void calculatePerformanceBenefits(PayrollBenefitResult result, EmployeeModel employee, Integer payPeriodId) {
        // This could be expanded to include performance ratings, sales commissions, etc.
        BigDecimal overtimeAllowance = getOvertimeAllowance(employee.getEmployeeId(), payPeriodId);
        result.setOvertimeAllowance(overtimeAllowance);
    }
    
    /**
     * Calculates special benefits (project-based, one-time allowances)
     */
    private void calculateSpecialBenefits(PayrollBenefitResult result, EmployeeModel employee, 
                                        LocalDate startDate, LocalDate endDate) {
        BigDecimal specialAllowance = getSpecialAllowances(employee.getEmployeeId(), startDate, endDate);
        result.setSpecialAllowance(specialAllowance);
    }
    
    // DATA RETRIEVAL METHODS
    
    /**
     * Gets employee benefit entitlements
     */
    private List<Map<String, Object>> getEmployeeBenefits(Integer employeeId) {
        String sql = "SELECT eb.*, bt.benefitName " +
                    "FROM employeebenefit eb " +
                    "JOIN benefittype bt ON eb.benefitTypeId = bt.benefitTypeId " +
                    "WHERE eb.employeeId = ? AND eb.isActive = true";
        
        return executeQuery(sql, employeeId);
    }
    
    /**
     * Gets employee insurance enrollment details
     */
    private List<Map<String, Object>> getEmployeeInsurance(Integer employeeId) {
        String sql = "SELECT * FROM employee_insurance " +
                    "WHERE employeeId = ? AND isActive = true";
        
        return executeQuery(sql, employeeId);
    }
    
    /**
     * Calculates 13th month pay
     */
    private BigDecimal calculateThirteenthMonthPay(EmployeeModel employee, int year) {
        // 13th month pay is typically 1/12 of annual basic salary
        BigDecimal monthlySalary = employee.getBasicSalary();
        if (monthlySalary == null) {
            return BigDecimal.ZERO;
        }
        
        // Calculate based on months worked in the year
        // For simplicity, assuming full year employment
        return monthlySalary;
    }
    
    /**
     * Calculates holiday bonus
     */
    private BigDecimal calculateHolidayBonus(EmployeeModel employee, LocalDate periodEnd) {
        // Holiday bonus could be based on company policy
        // For example, bonus in December
        if (periodEnd.getMonthValue() == 12) {
            BigDecimal monthlySalary = employee.getBasicSalary();
            if (monthlySalary != null) {
                return monthlySalary.multiply(new BigDecimal("0.5")); // 50% of monthly salary
            }
        }
        return BigDecimal.ZERO;
    }
    
    /**
     * Gets performance bonus for employee in pay period
     */
    private BigDecimal getPerformanceBonus(Integer employeeId, Integer payPeriodId) {
        String sql = "SELECT SUM(amount) as totalBonus FROM performance_bonus " +
                    "WHERE employeeId = ? AND payPeriodId = ?";
        
        List<Map<String, Object>> results = executeQuery(sql, employeeId, payPeriodId);
        if (!results.isEmpty() && results.get(0).get("totalBonus") != null) {
            return (BigDecimal) results.get(0).get("totalBonus");
        }
        return BigDecimal.ZERO;
    }
    
    /**
     * Gets overtime allowance
     */
    private BigDecimal getOvertimeAllowance(Integer employeeId, Integer payPeriodId) {
        String sql = "SELECT SUM(allowanceAmount) as totalAllowance FROM overtime_allowance " +
                    "WHERE employeeId = ? AND payPeriodId = ?";
        
        List<Map<String, Object>> results = executeQuery(sql, employeeId, payPeriodId);
        if (!results.isEmpty() && results.get(0).get("totalAllowance") != null) {
            return (BigDecimal) results.get(0).get("totalAllowance");
        }
        return BigDecimal.ZERO;
    }
    
    /**
     * Gets special allowances for a period
     */
    private BigDecimal getSpecialAllowances(Integer employeeId, LocalDate startDate, LocalDate endDate) {
        String sql = "SELECT SUM(amount) as totalSpecial FROM special_allowance " +
                    "WHERE employeeId = ? AND effectiveDate BETWEEN ? AND ?";
        
        List<Map<String, Object>> results = executeQuery(sql, employeeId, 
            java.sql.Date.valueOf(startDate), java.sql.Date.valueOf(endDate));
        
        if (!results.isEmpty() && results.get(0).get("totalSpecial") != null) {
            return (BigDecimal) results.get(0).get("totalSpecial");
        }
        return BigDecimal.ZERO;
    }
    
    /**
     * Gets pay period details by ID
     */
    private Map<String, Object> getPayPeriodDetails(Integer payPeriodId) {
        String sql = "SELECT payPeriodId, startDate, endDate, payDate, payPeriodDescription " +
                    "FROM payperiod WHERE payPeriodId = ?";
        
        List<Map<String, Object>> results = executeQuery(sql, payPeriodId);
        return results.isEmpty() ? null : results.get(0);
    }
    
    /**
     * Generic query execution method
     */
    private List<Map<String, Object>> executeQuery(String sql, Object... params) {
        List<Map<String, Object>> results = new ArrayList<>();
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            
            try (ResultSet rs = stmt.executeQuery()) {
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();
                
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = metaData.getColumnLabel(i);
                        Object value = rs.getObject(i);
                        row.put(columnName, value);
                    }
                    results.add(row);
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error executing query: " + e.getMessage());
        }
        
        return results;
    }
    
    /**
     * Saves benefit payroll calculation result to database
     * @param result PayrollBenefitResult to save
     * @return true if successful
     */
    public boolean saveBenefitPayrollCalculation(PayrollBenefitResult result) {
        String sql = "INSERT INTO payroll_benefit " +
                    "(employeeId, payPeriodId, sssEmployeeContribution, sssEmployerContribution, " +
                    "philhealthEmployeeContribution, philhealthEmployerContribution, " +
                    "pagibigEmployeeContribution, pagibigEmployerContribution, " +
                    "riceSubsidy, phoneAllowance, clothingAllowance, transportationAllowance, mealAllowance, " +
                    "performanceBonus, holidayBonus, thirteenthMonthPay, " +
                    "healthInsurancePremium, lifeInsurancePremium, " +
                    "healthInsuranceCompanyCoverage, lifeInsuranceCompanyCoverage, " +
                    "overtimeAllowance, specialAllowance, " +
                    "totalBenefitPayments, totalBenefitDeductions, netBenefitAdjustment, calculatedDate) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP) " +
                    "ON DUPLICATE KEY UPDATE " +
                    "sssEmployeeContribution = VALUES(sssEmployeeContribution), " +
                    "sssEmployerContribution = VALUES(sssEmployerContribution), " +
                    "philhealthEmployeeContribution = VALUES(philhealthEmployeeContribution), " +
                    "philhealthEmployerContribution = VALUES(philhealthEmployerContribution), " +
                    "pagibigEmployeeContribution = VALUES(pagibigEmployeeContribution), " +
                    "pagibigEmployerContribution = VALUES(pagibigEmployerContribution), " +
                    "riceSubsidy = VALUES(riceSubsidy), phoneAllowance = VALUES(phoneAllowance), " +
                    "clothingAllowance = VALUES(clothingAllowance), transportationAllowance = VALUES(transportationAllowance), " +
                    "mealAllowance = VALUES(mealAllowance), performanceBonus = VALUES(performanceBonus), " +
                    "holidayBonus = VALUES(holidayBonus), thirteenthMonthPay = VALUES(thirteenthMonthPay), " +
                    "healthInsurancePremium = VALUES(healthInsurancePremium), lifeInsurancePremium = VALUES(lifeInsurancePremium), " +
                    "healthInsuranceCompanyCoverage = VALUES(healthInsuranceCompanyCoverage), " +
                    "lifeInsuranceCompanyCoverage = VALUES(lifeInsuranceCompanyCoverage), " +
                    "overtimeAllowance = VALUES(overtimeAllowance), specialAllowance = VALUES(specialAllowance), " +
                    "totalBenefitPayments = VALUES(totalBenefitPayments), " +
                    "totalBenefitDeductions = VALUES(totalBenefitDeductions), " +
                    "netBenefitAdjustment = VALUES(netBenefitAdjustment), calculatedDate = CURRENT_TIMESTAMP";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, result.getEmployeeId());
            stmt.setInt(2, result.getPayPeriodId());
            stmt.setBigDecimal(3, result.getSssEmployeeContribution());
            stmt.setBigDecimal(4, result.getSssEmployerContribution());
            stmt.setBigDecimal(5, result.getPhilhealthEmployeeContribution());
            stmt.setBigDecimal(6, result.getPhilhealthEmployerContribution());
            stmt.setBigDecimal(7, result.getPagibigEmployeeContribution());
            stmt.setBigDecimal(8, result.getPagibigEmployerContribution());
            stmt.setBigDecimal(9, result.getRiceSubsidy());
            stmt.setBigDecimal(10, result.getPhoneAllowance());
            stmt.setBigDecimal(11, result.getClothingAllowance());
            stmt.setBigDecimal(12, result.getTransportationAllowance());
            stmt.setBigDecimal(13, result.getMealAllowance());
            stmt.setBigDecimal(14, result.getPerformanceBonus());
            stmt.setBigDecimal(15, result.getHolidayBonus());
            stmt.setBigDecimal(16, result.getThirteenthMonthPay());
            stmt.setBigDecimal(17, result.getHealthInsurancePremium());
            stmt.setBigDecimal(18, result.getLifeInsurancePremium());
            stmt.setBigDecimal(19, result.getHealthInsuranceCompanyCoverage());
            stmt.setBigDecimal(20, result.getLifeInsuranceCompanyCoverage());
            stmt.setBigDecimal(21, result.getOvertimeAllowance());
            stmt.setBigDecimal(22, result.getSpecialAllowance());
            stmt.setBigDecimal(23, result.getTotalBenefitPayments());
            stmt.setBigDecimal(24, result.getTotalBenefitDeductions());
            stmt.setBigDecimal(25, result.getNetBenefitAdjustment());
            
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                System.out.println("Benefit payroll calculation saved for employee " + result.getEmployeeId() + 
                                 " pay period " + result.getPayPeriodId());
                return true;
            }
            
        } catch (SQLException e) {
            System.err.println("Error saving benefit payroll calculation: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    /**
     * Inner class to hold benefit payroll calculation results
     */
    public static class PayrollBenefitResult {
        private Integer employeeId;
        private Integer payPeriodId;
        private LocalDate startDate;
        private LocalDate endDate;
        
        // Government benefits - employee contributions (deductions)
        private BigDecimal sssEmployeeContribution = BigDecimal.ZERO;
        private BigDecimal philhealthEmployeeContribution = BigDecimal.ZERO;
        private BigDecimal pagibigEmployeeContribution = BigDecimal.ZERO;
        
        // Government benefits - employer contributions (company expense)
        private BigDecimal sssEmployerContribution = BigDecimal.ZERO;
        private BigDecimal philhealthEmployerContribution = BigDecimal.ZERO;
        private BigDecimal pagibigEmployerContribution = BigDecimal.ZERO;
        
        // Allowances (additions to pay)
        private BigDecimal riceSubsidy = BigDecimal.ZERO;
        private BigDecimal phoneAllowance = BigDecimal.ZERO;
        private BigDecimal clothingAllowance = BigDecimal.ZERO;
        private BigDecimal transportationAllowance = BigDecimal.ZERO;
        private BigDecimal mealAllowance = BigDecimal.ZERO;
        
        // Bonuses (additions to pay)
        private BigDecimal performanceBonus = BigDecimal.ZERO;
        private BigDecimal holidayBonus = BigDecimal.ZERO;
        private BigDecimal thirteenthMonthPay = BigDecimal.ZERO;
        
        // Insurance (employee premiums - deductions)
        private BigDecimal healthInsurancePremium = BigDecimal.ZERO;
        private BigDecimal lifeInsurancePremium = BigDecimal.ZERO;
        
        // Insurance (company contributions - company expense)
        private BigDecimal healthInsuranceCompanyCoverage = BigDecimal.ZERO;
        private BigDecimal lifeInsuranceCompanyCoverage = BigDecimal.ZERO;
        
        // Special benefits
        private BigDecimal overtimeAllowance = BigDecimal.ZERO;
        private BigDecimal specialAllowance = BigDecimal.ZERO;
        
        // Totals
        private BigDecimal totalBenefitPayments = BigDecimal.ZERO;
        private BigDecimal totalBenefitDeductions = BigDecimal.ZERO;
        private BigDecimal netBenefitAdjustment = BigDecimal.ZERO;
        
        // Getters and setters
        public Integer getEmployeeId() { return employeeId; }
        public void setEmployeeId(Integer employeeId) { this.employeeId = employeeId; }
        
        public Integer getPayPeriodId() { return payPeriodId; }
        public void setPayPeriodId(Integer payPeriodId) { this.payPeriodId = payPeriodId; }
        
        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
        
        public LocalDate getEndDate() { return endDate; }
        public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
        
        public BigDecimal getSssEmployeeContribution() { return sssEmployeeContribution; }
        public void setSssEmployeeContribution(BigDecimal sssEmployeeContribution) { this.sssEmployeeContribution = sssEmployeeContribution; }
        
        public BigDecimal getPhilhealthEmployeeContribution() { return philhealthEmployeeContribution; }
        public void setPhilhealthEmployeeContribution(BigDecimal philhealthEmployeeContribution) { this.philhealthEmployeeContribution = philhealthEmployeeContribution; }
        
        public BigDecimal getPagibigEmployeeContribution() { return pagibigEmployeeContribution; }
        public void setPagibigEmployeeContribution(BigDecimal pagibigEmployeeContribution) { this.pagibigEmployeeContribution = pagibigEmployeeContribution; }
        
        public BigDecimal getSssEmployerContribution() { return sssEmployerContribution; }
        public void setSssEmployerContribution(BigDecimal sssEmployerContribution) { this.sssEmployerContribution = sssEmployerContribution; }
        
        public BigDecimal getPhilhealthEmployerContribution() { return philhealthEmployerContribution; }
        public void setPhilhealthEmployerContribution(BigDecimal philhealthEmployerContribution) { this.philhealthEmployerContribution = philhealthEmployerContribution; }
        
        public BigDecimal getPagibigEmployerContribution() { return pagibigEmployerContribution; }
        public void setPagibigEmployerContribution(BigDecimal pagibigEmployerContribution) { this.pagibigEmployerContribution = pagibigEmployerContribution; }
        
        public BigDecimal getRiceSubsidy() { return riceSubsidy; }
        public void setRiceSubsidy(BigDecimal riceSubsidy) { this.riceSubsidy = riceSubsidy; }
        
        public BigDecimal getPhoneAllowance() { return phoneAllowance; }
        public void setPhoneAllowance(BigDecimal phoneAllowance) { this.phoneAllowance = phoneAllowance; }
        
        public BigDecimal getClothingAllowance() { return clothingAllowance; }
        public void setClothingAllowance(BigDecimal clothingAllowance) { this.clothingAllowance = clothingAllowance; }
        
        public BigDecimal getTransportationAllowance() { return transportationAllowance; }
        public void setTransportationAllowance(BigDecimal transportationAllowance) { this.transportationAllowance = transportationAllowance; }
        
        public BigDecimal getMealAllowance() { return mealAllowance; }
        public void setMealAllowance(BigDecimal mealAllowance) { this.mealAllowance = mealAllowance; }
        
        public BigDecimal getPerformanceBonus() { return performanceBonus; }
        public void setPerformanceBonus(BigDecimal performanceBonus) { this.performanceBonus = performanceBonus; }
        
        public BigDecimal getHolidayBonus() { return holidayBonus; }
        public void setHolidayBonus(BigDecimal holidayBonus) { this.holidayBonus = holidayBonus; }
        
        public BigDecimal getThirteenthMonthPay() { return thirteenthMonthPay; }
        public void setThirteenthMonthPay(BigDecimal thirteenthMonthPay) { this.thirteenthMonthPay = thirteenthMonthPay; }
        
        public BigDecimal getHealthInsurancePremium() { return healthInsurancePremium; }
        public void setHealthInsurancePremium(BigDecimal healthInsurancePremium) { this.healthInsurancePremium = healthInsurancePremium; }
        
        public BigDecimal getLifeInsurancePremium() { return lifeInsurancePremium; }
        public void setLifeInsurancePremium(BigDecimal lifeInsurancePremium) { this.lifeInsurancePremium = lifeInsurancePremium; }
        
        public BigDecimal getHealthInsuranceCompanyCoverage() { return healthInsuranceCompanyCoverage; }
        public void setHealthInsuranceCompanyCoverage(BigDecimal healthInsuranceCompanyCoverage) { this.healthInsuranceCompanyCoverage = healthInsuranceCompanyCoverage; }
        
        public BigDecimal getLifeInsuranceCompanyCoverage() { return lifeInsuranceCompanyCoverage; }
        public void setLifeInsuranceCompanyCoverage(BigDecimal lifeInsuranceCompanyCoverage) { this.lifeInsuranceCompanyCoverage = lifeInsuranceCompanyCoverage; }
        
        public BigDecimal getOvertimeAllowance() { return overtimeAllowance; }
        public void setOvertimeAllowance(BigDecimal overtimeAllowance) { this.overtimeAllowance = overtimeAllowance; }
        
        public BigDecimal getSpecialAllowance() { return specialAllowance; }
        public void setSpecialAllowance(BigDecimal specialAllowance) { this.specialAllowance = specialAllowance; }
        
        public BigDecimal getTotalBenefitPayments() { return totalBenefitPayments; }
        public void setTotalBenefitPayments(BigDecimal totalBenefitPayments) { this.totalBenefitPayments = totalBenefitPayments; }
        
        public BigDecimal getTotalBenefitDeductions() { return totalBenefitDeductions; }
        public void setTotalBenefitDeductions(BigDecimal totalBenefitDeductions) { this.totalBenefitDeductions = totalBenefitDeductions; }
        
        public BigDecimal getNetBenefitAdjustment() { return netBenefitAdjustment; }
        public void setNetBenefitAdjustment(BigDecimal netBenefitAdjustment) { this.netBenefitAdjustment = netBenefitAdjustment; }
    }
}