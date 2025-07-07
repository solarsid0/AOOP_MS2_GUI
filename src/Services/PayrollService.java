
package Services;

import Models.*;
import DAOs.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;

/**
 * Enhanced PayrollService that uses database views for complex payroll calculations.
 * This service implements the business logic using monthly_employee_payslip and 
 * monthly_payroll_summary_report views as specified in the task document.
 * @author chad
 */
public class PayrollService {
    
    // Manila timezone constant for payroll system
    public static final ZoneId MANILA_TIMEZONE = ZoneId.of("Asia/Manila");
    
    // DAOs for database operations
    private PayrollDAO payrollDAO;
    private PayslipDAO payslipDAO;
    private EmployeeDAO employeeDAO;
    private DatabaseConnection databaseConnection;
    
    /**
     * Constructor with database connection
     * @param databaseConnection The database connection to use
     */
    public PayrollService(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
        this.payrollDAO = new PayrollDAO(databaseConnection);
        this.payslipDAO = new PayslipDAO(databaseConnection);
        this.employeeDAO = new EmployeeDAO(databaseConnection);
    }
    
    // CORE VIEW-BASED METHODS - Using Database Views as specified in task document
    
    /**
     * Gets employee payslip using monthly_employee_payslip view
     * This method uses the database view for complex payroll calculations
     * @param employeeId The employee ID
     * @param payMonth The pay month in YYYY-MM format
     * @return PayslipModel with complete payroll breakdown from view
     */
    public PayslipModel getEmployeePayslip(Integer employeeId, String payMonth) {
        String sql = """
            SELECT * FROM monthly_employee_payslip
            WHERE `Employee ID` = ?
            AND DATE_FORMAT(`Period Start Date`, '%Y-%m') = ?
            """;
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, employeeId);
            stmt.setString(2, payMonth);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapViewToPayslip(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting employee payslip from view: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * Gets monthly payroll summary using monthly_payroll_summary_report view
     * @param payMonth The pay month in YYYY-MM format
     * @return List of payroll summary records
     */
    public List<PayrollSummaryModel> getMonthlyPayrollSummary(String payMonth) {
        String sql = """
            SELECT * FROM monthly_payroll_summary_report
            WHERE DATE_FORMAT(`Pay Date`, '%Y-%m') = ?
            ORDER BY `Employee ID`
            """;
        
        List<PayrollSummaryModel> summaryList = new ArrayList<>();
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, payMonth);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    PayrollSummaryModel summary = mapViewToSummary(rs);
                    if (summary != null) {
                        summaryList.add(summary);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting payroll summary from view: " + e.getMessage());
            e.printStackTrace();
        }
        
        return summaryList;
    }
    
    /**
     * Gets current month payroll data using views
     * @return List of current month payroll records from view
     */
    public List<PayslipModel> getCurrentMonthPayroll() {
        LocalDate currentDate = getCurrentManilaDate();
        String currentMonth = currentDate.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        
        String sql = """
            SELECT * FROM monthly_employee_payslip
            WHERE DATE_FORMAT(`Period Start Date`, '%Y-%m') = ?
            ORDER BY `Employee ID`
            """;
        
        List<PayslipModel> payslips = new ArrayList<>();
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, currentMonth);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    PayslipModel payslip = mapViewToPayslip(rs);
                    if (payslip != null) {
                        payslips.add(payslip);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting current month payroll: " + e.getMessage());
            e.printStackTrace();
        }
        
        return payslips;
    }
    
    // RANK-AND-FILE DETECTION - Using position table relationship
    
    /**
     * Checks if an employee is rank-and-file using position table
     * @param employeeId The employee ID
     * @return true if employee is rank-and-file
     */
    public boolean isEmployeeRankAndFile(Integer employeeId) {
        String sql = """
            SELECT COUNT(*) FROM employee e 
            JOIN position p ON e.positionId = p.positionId 
            WHERE e.employeeId = ? AND (
                LOWER(p.department) = 'rank-and-file' 
                OR LOWER(p.position) LIKE '%rank%file%'
            )
            """;
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, employeeId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error checking rank-and-file status: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Gets all rank-and-file employees
     * @return List of rank-and-file employees
     */
    public List<EmployeeModel> getRankAndFileEmployees() {
        return employeeDAO.getRankAndFileEmployees();
    }
    
    /**
     * Gets all non rank-and-file employees
     * @return List of non rank-and-file employees
     */
    public List<EmployeeModel> getNonRankAndFileEmployees() {
        return employeeDAO.getNonRankAndFileEmployees();
    }
    
    // PAYROLL PROCESSING METHODS
    
    /**
     * Processes payroll for a specific month using business logic from views
     * @param payMonth The pay month in YYYY-MM format
     * @return Number of employees processed
     */
    public int processMonthlyPayroll(String payMonth) {
        System.out.println("🚀 Starting payroll processing for month: " + payMonth);
        
        // Get all active employees
        List<EmployeeModel> employees = employeeDAO.getActiveEmployees();
        int processedCount = 0;
        
        for (EmployeeModel employee : employees) {
            try {
                // Check if payroll already exists for this employee and month
                if (!payrollExistsForMonth(employee.getEmployeeId(), payMonth)) {
                    
                    // Process individual employee payroll using views
                    PayslipModel payslip = processEmployeePayroll(employee.getEmployeeId(), payMonth);
                    
                    if (payslip != null) {
                        processedCount++;
                        System.out.println("✅ Processed payroll for employee: " + employee.getEmployeeId());
                    } else {
                        System.out.println("⚠️ No attendance/leave data for employee: " + employee.getEmployeeId());
                    }
                } else {
                    System.out.println("ℹ️ Payroll already exists for employee: " + employee.getEmployeeId());
                }
                
            } catch (Exception e) {
                System.err.println("❌ Error processing employee " + employee.getEmployeeId() + ": " + e.getMessage());
            }
        }
        
        System.out.println("🎉 Payroll processing completed. Processed: " + processedCount + " employees");
        return processedCount;
    }
    
    /**
     * Processes individual employee payroll by querying the view
     * @param employeeId The employee ID
     * @param payMonth The pay month
     * @return PayslipModel if successful, null if no data
     */
    public PayslipModel processEmployeePayroll(Integer employeeId, String payMonth) {
        // The view handles all the complex business logic, so we just query it
        return getEmployeePayslip(employeeId, payMonth);
    }
    
    /**
     * Regenerates payroll for a specific month (deletes existing and recreates)
     * @param payMonth The pay month in YYYY-MM format
     * @return Number of employees processed
     */
    public int regenerateMonthlyPayroll(String payMonth) {
        System.out.println("🔄 Regenerating payroll for month: " + payMonth);
        
        // First, delete existing payroll records for the month
        int deletedCount = deletePayrollForMonth(payMonth);
        System.out.println("🗑️ Deleted " + deletedCount + " existing payroll records");
        
        // Then process fresh payroll
        return processMonthlyPayroll(payMonth);
    }
    
    // VALIDATION METHODS - Manila Timezone Support
    
    /**
     * Validates leave request date (can only be today or future in Manila time)
     * @param leaveDate The leave date to validate
     * @return true if valid leave date
     */
    public boolean isValidLeaveDate(LocalDate leaveDate) {
        LocalDate today = getCurrentManilaDate();
        return !leaveDate.isBefore(today); // Can only request today or future dates
    }
    
    /**
     * Validates overtime request date (can only be today or future in Manila time)
     * @param overtimeDate The overtime date to validate
     * @return true if valid overtime date
     */
    public boolean isValidOvertimeDate(LocalDate overtimeDate) {
        LocalDate today = getCurrentManilaDate();
        return !overtimeDate.isBefore(today); // Can only request today or future dates
    }
    
    /**
     * Checks if time is within grace period (8:00-8:10 AM)
     * @param timeIn The time in
     * @return true if within grace period
     */
    public boolean isWithinGracePeriod(String timeIn) {
        if (timeIn == null || timeIn.isEmpty()) {
            return false;
        }
        return timeIn.compareTo("08:00:00") >= 0 && timeIn.compareTo("08:10:00") <= 0;
    }
    
    /**
     * Checks if attendance time is late (after 8:10 AM)
     * @param timeIn The time in
     * @return true if late
     */
    public boolean isLateArrival(String timeIn) {
        if (timeIn == null || timeIn.isEmpty()) {
            return false;
        }
        return timeIn.compareTo("08:10:00") > 0;
    }
    
    // REPORTING METHODS
    
    /**
     * Gets payroll totals for a specific month
     * @param payMonth The pay month
     * @return PayrollTotals object with summary information
     */
    public PayrollTotals getPayrollTotals(String payMonth) {
        String sql = """
            SELECT 
                COUNT(*) as employeeCount,
                SUM(`GROSS INCOME`) as totalGrossIncome,
                SUM(`TOTAL BENEFITS`) as totalBenefits,
                SUM(`TOTAL DEDUCTIONS`) as totalDeductions,
                SUM(`NET PAY`) as totalNetPay
            FROM monthly_payroll_summary_report
            WHERE DATE_FORMAT(`Pay Date`, '%Y-%m') = ?
            AND `Employee ID` != 'TOTAL'
            """;
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, payMonth);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    PayrollTotals totals = new PayrollTotals();
                    totals.setEmployeeCount(rs.getInt("employeeCount"));
                    totals.setTotalGrossIncome(rs.getBigDecimal("totalGrossIncome"));
                    totals.setTotalBenefits(rs.getBigDecimal("totalBenefits"));
                    totals.setTotalDeductions(rs.getBigDecimal("totalDeductions"));
                    totals.setTotalNetPay(rs.getBigDecimal("totalNetPay"));
                    return totals;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting payroll totals: " + e.getMessage());
        }
        
        return new PayrollTotals(); // Return empty totals if error
    }
    
    // HELPER METHODS
    
    /**
     * Maps database view result to PayslipModel (enhanced to work with view)
     * @param rs ResultSet from monthly_employee_payslip view
     * @return PayslipModel object
     * @throws SQLException if error reading ResultSet
     */
    private PayslipModel mapViewToPayslip(ResultSet rs) throws SQLException {
        PayslipModel payslip = new PayslipModel();
        
        // Map all the view columns to PayslipModel properties
        payslip.setPayslipNo(rs.getString("Payslip No"));
        payslip.setEmployeeId(rs.getInt("Employee ID"));
        payslip.setEmployeeName(rs.getString("Employee Name"));
        payslip.setPeriodStartDate(rs.getDate("Period Start Date").toLocalDate());
        payslip.setPeriodEndDate(rs.getDate("Period End Date").toLocalDate());
        payslip.setPayDate(rs.getDate("Pay Date").toLocalDate());
        payslip.setEmployeePosition(rs.getString("Employee Position"));
        payslip.setDepartment(rs.getString("Department"));
        payslip.setTin(rs.getString("TIN"));
        payslip.setSssNo(rs.getString("SSS No"));
        payslip.setPagibigNo(rs.getString("Pagibig No"));
        payslip.setPhilhealthNo(rs.getString("Philhealth No"));
        payslip.setMonthlyRate(rs.getBigDecimal("Monthly Rate"));
        payslip.setDailyRate(rs.getBigDecimal("Daily Rate"));
        
        // Convert BigDecimal to Integer for daysWorked (to match existing model)
        BigDecimal daysWorkedBD = rs.getBigDecimal("Days Worked");
        if (daysWorkedBD != null) {
            payslip.setDaysWorked(daysWorkedBD.intValue());
        }
        
        payslip.setLeavesTaken(rs.getBigDecimal("Leaves Taken"));
        payslip.setOvertimeHours(rs.getBigDecimal("Overtime Hours"));
        payslip.setGrossIncome(rs.getBigDecimal("GROSS INCOME"));
        payslip.setRiceSubsidy(rs.getBigDecimal("Rice Subsidy"));
        payslip.setPhoneAllowance(rs.getBigDecimal("Phone Allowance"));
        payslip.setClothingAllowance(rs.getBigDecimal("Clothing Allowance"));
        payslip.setTotalBenefits(rs.getBigDecimal("TOTAL BENEFITS"));
        payslip.setSssDeduction(rs.getBigDecimal("Social Security System"));
        payslip.setPhilhealthDeduction(rs.getBigDecimal("Philhealth"));
        payslip.setPagibigDeduction(rs.getBigDecimal("Pag-Ibig"));
        payslip.setWithholdingTax(rs.getBigDecimal("Withholding Tax"));
        payslip.setTotalDeductions(rs.getBigDecimal("TOTAL DEDUCTIONS"));
        payslip.setNetPay(rs.getBigDecimal("NET PAY"));
        
        return payslip;
    }
    
    /**
     * Maps database view result to PayrollSummaryModel
     * @param rs ResultSet from monthly_payroll_summary_report view
     * @return PayrollSummaryModel object
     * @throws SQLException if error reading ResultSet
     */
    private PayrollSummaryModel mapViewToSummary(ResultSet rs) throws SQLException {
        PayrollSummaryModel summary = new PayrollSummaryModel();
        
        summary.setPayDate(rs.getDate("Pay Date").toLocalDate());
        summary.setEmployeeId(rs.getString("Employee ID"));
        summary.setEmployeeName(rs.getString("Employee Name"));
        summary.setPosition(rs.getString("Position"));
        summary.setDepartment(rs.getString("Department"));
        summary.setBaseSalary(rs.getBigDecimal("Base Salary"));
        summary.setLeaves(rs.getBigDecimal("Leaves"));
        summary.setOvertime(rs.getBigDecimal("Overtime"));
        summary.setGrossIncome(rs.getBigDecimal("GROSS INCOME"));
        summary.setRiceSubsidy(rs.getBigDecimal("Rice Subsidy"));
        summary.setPhoneAllowance(rs.getBigDecimal("Phone Allowance"));
        summary.setClothingAllowance(rs.getBigDecimal("Clothing Allowance"));
        summary.setTotalBenefits(rs.getBigDecimal("TOTAL BENEFITS"));
        summary.setSocialSecurityNo(rs.getString("Social Security No"));
        summary.setSocialSecurityContribution(rs.getBigDecimal("Social Security Contribution"));
        summary.setPhilhealthNo(rs.getString("Philhealth No"));
        summary.setPhilhealthContribution(rs.getBigDecimal("Philhealth Contribution"));
        summary.setPagIbigNo(rs.getString("Pag-Ibig No"));
        summary.setPagIbigContribution(rs.getBigDecimal("Pag-Ibig Contribution"));
        summary.setTin(rs.getString("TIN"));
        summary.setWithholdingTax(rs.getBigDecimal("Withholding Tax"));
        summary.setTotalDeductions(rs.getBigDecimal("TOTAL DEDUCTIONS"));
        summary.setNetPay(rs.getBigDecimal("NET PAY"));
        
        return summary;
    }
    
    /**
     * Gets current Manila date
     * @return Current date in Manila timezone
     */
    private LocalDate getCurrentManilaDate() {
        return LocalDate.now(MANILA_TIMEZONE);
    }
    
    /**
     * Gets current Manila time
     * @return Current datetime in Manila timezone
     */
    private LocalDateTime getCurrentManilaTime() {
        return LocalDateTime.now(MANILA_TIMEZONE);
    }
    
    /**
     * Checks if payroll exists for a specific month
     * @param employeeId The employee ID
     * @param payMonth The pay month
     * @return true if payroll exists
     */
    private boolean payrollExistsForMonth(Integer employeeId, String payMonth) {
        String sql = """
            SELECT COUNT(*) FROM monthly_employee_payslip
            WHERE `Employee ID` = ?
            AND DATE_FORMAT(`Period Start Date`, '%Y-%m') = ?
            """;
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, employeeId);
            stmt.setString(2, payMonth);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error checking payroll existence: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Deletes payroll records for a specific month
     * @param payMonth The pay month
     * @return Number of records deleted
     */
    private int deletePayrollForMonth(String payMonth) {
        // This would need to be implemented based on your payroll table structure
        // For now, return 0 as the view is read-only
        return 0;
    }
    
    // INNER CLASS - PayrollTotals for summary information
    
    /**
     * Inner class to hold payroll totals information
     */
    public static class PayrollTotals {
        private int employeeCount;
        private BigDecimal totalGrossIncome = BigDecimal.ZERO;
        private BigDecimal totalBenefits = BigDecimal.ZERO;
        private BigDecimal totalDeductions = BigDecimal.ZERO;
        private BigDecimal totalNetPay = BigDecimal.ZERO;
        
        // Getters and setters
        public int getEmployeeCount() { return employeeCount; }
        public void setEmployeeCount(int employeeCount) { this.employeeCount = employeeCount; }
        
        public BigDecimal getTotalGrossIncome() { return totalGrossIncome; }
        public void setTotalGrossIncome(BigDecimal totalGrossIncome) { 
            this.totalGrossIncome = totalGrossIncome != null ? totalGrossIncome : BigDecimal.ZERO; 
        }
        
        public BigDecimal getTotalBenefits() { return totalBenefits; }
        public void setTotalBenefits(BigDecimal totalBenefits) { 
            this.totalBenefits = totalBenefits != null ? totalBenefits : BigDecimal.ZERO; 
        }
        
        public BigDecimal getTotalDeductions() { return totalDeductions; }
        public void setTotalDeductions(BigDecimal totalDeductions) { 
            this.totalDeductions = totalDeductions != null ? totalDeductions : BigDecimal.ZERO; 
        }
        
        public BigDecimal getTotalNetPay() { return totalNetPay; }
        public void setTotalNetPay(BigDecimal totalNetPay) { 
            this.totalNetPay = totalNetPay != null ? totalNetPay : BigDecimal.ZERO; 
        }
        
        @Override
        public String toString() {
            return String.format("PayrollTotals{employees=%d, grossIncome=%s, benefits=%s, deductions=%s, netPay=%s}",
                    employeeCount, totalGrossIncome, totalBenefits, totalDeductions, totalNetPay);
        }
    }
}