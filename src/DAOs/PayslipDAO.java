package DAOs;

import Models.PayslipModel;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Data Access Object for PayslipModel entities.
 * This class handles all database operations related to payslips (individual employee pay statements).
 * It extends BaseDAO to inherit common CRUD operations and adds payslip-specific methods.
 * @author User
 */
public class PayslipDAO extends BaseDAO<PayslipModel, Integer> {
    
    /**
     * Constructor that accepts a DatabaseConnection instance
     * @param databaseConnection The database connection to use for all operations
     */
    public PayslipDAO(DatabaseConnection databaseConnection) {
        super(databaseConnection);
    }
    

    // ABSTRACT METHOD IMPLEMENTATIONS - Required by BaseDAO

    
    /**
     * Converts a database row into a PayslipModel object
     * This method reads each column from the ResultSet and creates a PayslipModel
     * @param rs The ResultSet containing payslip data from the database
     * @return A fully populated PayslipModel object
     * @throws SQLException if there's an error reading from the database
     */
    @Override
    protected PayslipModel mapResultSetToEntity(ResultSet rs) throws SQLException {
        PayslipModel payslip = new PayslipModel();
        
        // Set basic payslip information
        payslip.setPayslipId(rs.getInt("payslipId"));
        payslip.setEmployeeName(rs.getString("employeeName"));
        
        // Handle date fields
        Date periodStart = rs.getDate("periodStart");
        if (periodStart != null) {
            payslip.setPeriodStart(periodStart.toLocalDate());
        }
        
        Date periodEnd = rs.getDate("periodEnd");
        if (periodEnd != null) {
            payslip.setPeriodEnd(periodEnd.toLocalDate());
        }
        
        // Handle salary information
        payslip.setMonthlyRate(rs.getBigDecimal("monthlyRate"));
        payslip.setDailyRate(rs.getBigDecimal("dailyRate"));
        payslip.setDaysWorked(rs.getInt("daysWorked"));
        
        // Handle earnings
        payslip.setOvertime(rs.getBigDecimal("overtime"));
        
        // Handle benefits
        payslip.setRiceSubsidy(rs.getBigDecimal("riceSubsidy"));
        payslip.setPhoneAllowance(rs.getBigDecimal("phoneAllowance"));
        payslip.setClothingAllowance(rs.getBigDecimal("clothingAllowance"));
        
        // Handle deductions
        payslip.setSss(rs.getBigDecimal("sss"));
        payslip.setPhilhealth(rs.getBigDecimal("philhealth"));
        payslip.setPagibig(rs.getBigDecimal("pagibig"));
        payslip.setWithholdingTax(rs.getBigDecimal("withholdingTax"));
        
        // Handle totals
        payslip.setGrossIncome(rs.getBigDecimal("grossIncome"));
        payslip.setTakeHomePay(rs.getBigDecimal("takeHomePay"));
        
        // Handle foreign keys
        payslip.setPayPeriodId(rs.getInt("payPeriodId"));
        payslip.setPayrollId(rs.getInt("payrollId"));
        payslip.setEmployeeId(rs.getInt("employeeId"));
        payslip.setPositionId(rs.getInt("positionId"));
        
        return payslip;
    }
    
    /**
     * Returns the database table name for payslips
     * @return "payslip" - the name of the payslip table in the database
     */
    @Override
    protected String getTableName() {
        return "payslip";
    }
    
    /**
     * Returns the primary key column name for the payslip table
     * @return "payslipId" - the primary key column name
     */
    @Override
    protected String getPrimaryKeyColumn() {
        return "payslipId";
    }
    
    /**
     * Sets parameters for INSERT operations when creating new payslips
     * This method maps PayslipModel object properties to SQL parameters
     * @param stmt The PreparedStatement to set parameters on
     * @param payslip The PayslipModel object to get values from
     * @throws SQLException if there's an error setting parameters
     */
    @Override
    protected void setInsertParameters(PreparedStatement stmt, PayslipModel payslip) throws SQLException {
        int paramIndex = 1;
        
        // Set basic information
        stmt.setString(paramIndex++, payslip.getEmployeeName());
        stmt.setDate(paramIndex++, Date.valueOf(payslip.getPeriodStart()));
        stmt.setDate(paramIndex++, Date.valueOf(payslip.getPeriodEnd()));
        
        // Set salary information
        stmt.setBigDecimal(paramIndex++, payslip.getMonthlyRate());
        stmt.setBigDecimal(paramIndex++, payslip.getDailyRate());
        stmt.setInt(paramIndex++, payslip.getDaysWorked());
        
        // Set earnings
        stmt.setBigDecimal(paramIndex++, payslip.getOvertime());
        
        // Set benefits
        stmt.setBigDecimal(paramIndex++, payslip.getRiceSubsidy());
        stmt.setBigDecimal(paramIndex++, payslip.getPhoneAllowance());
        stmt.setBigDecimal(paramIndex++, payslip.getClothingAllowance());
        
        // Set deductions
        stmt.setBigDecimal(paramIndex++, payslip.getSss());
        stmt.setBigDecimal(paramIndex++, payslip.getPhilhealth());
        stmt.setBigDecimal(paramIndex++, payslip.getPagibig());
        stmt.setBigDecimal(paramIndex++, payslip.getWithholdingTax());
        
        // Set totals
        stmt.setBigDecimal(paramIndex++, payslip.getGrossIncome());
        stmt.setBigDecimal(paramIndex++, payslip.getTakeHomePay());
        
        // Set foreign keys
        stmt.setInt(paramIndex++, payslip.getPayPeriodId());
        stmt.setInt(paramIndex++, payslip.getPayrollId());
        stmt.setInt(paramIndex++, payslip.getEmployeeId());
        stmt.setInt(paramIndex++, payslip.getPositionId());
    }
    
    /**
     * Sets parameters for UPDATE operations when modifying existing payslips
     * This method maps PayslipModel object properties to SQL parameters for updates
     * @param stmt The PreparedStatement to set parameters on
     * @param payslip The PayslipModel object with updated values
     * @throws SQLException if there's an error setting parameters
     */
    @Override
    protected void setUpdateParameters(PreparedStatement stmt, PayslipModel payslip) throws SQLException {
        int paramIndex = 1;
        
        // Set all the same fields as INSERT (excluding auto-increment ID)
        stmt.setString(paramIndex++, payslip.getEmployeeName());
        stmt.setDate(paramIndex++, Date.valueOf(payslip.getPeriodStart()));
        stmt.setDate(paramIndex++, Date.valueOf(payslip.getPeriodEnd()));
        stmt.setBigDecimal(paramIndex++, payslip.getMonthlyRate());
        stmt.setBigDecimal(paramIndex++, payslip.getDailyRate());
        stmt.setInt(paramIndex++, payslip.getDaysWorked());
        stmt.setBigDecimal(paramIndex++, payslip.getOvertime());
        stmt.setBigDecimal(paramIndex++, payslip.getRiceSubsidy());
        stmt.setBigDecimal(paramIndex++, payslip.getPhoneAllowance());
        stmt.setBigDecimal(paramIndex++, payslip.getClothingAllowance());
        stmt.setBigDecimal(paramIndex++, payslip.getSss());
        stmt.setBigDecimal(paramIndex++, payslip.getPhilhealth());
        stmt.setBigDecimal(paramIndex++, payslip.getPagibig());
        stmt.setBigDecimal(paramIndex++, payslip.getWithholdingTax());
        stmt.setBigDecimal(paramIndex++, payslip.getGrossIncome());
        stmt.setBigDecimal(paramIndex++, payslip.getTakeHomePay());
        stmt.setInt(paramIndex++, payslip.getPayPeriodId());
        stmt.setInt(paramIndex++, payslip.getPayrollId());
        stmt.setInt(paramIndex++, payslip.getEmployeeId());
        stmt.setInt(paramIndex++, payslip.getPositionId());
        
        // Finally, set the payslip ID for the WHERE clause
        stmt.setInt(paramIndex++, payslip.getPayslipId());
    }
    
    /**
     * Gets the ID from a PayslipModel object
     * This is used by BaseDAO for update and delete operations
     * @param payslip The PayslipModel object to get ID from
     * @return The payslip's ID
     */
    @Override
    protected Integer getEntityId(PayslipModel payslip) {
        return payslip.getPayslipId();
    }
    
    /**
     * Handles auto-generated payslip IDs after INSERT operations
     * This method sets the generated payslipId back on the PayslipModel object
     * @param entity The PayslipModel that was just inserted
     * @param generatedKeys The ResultSet containing the generated payslipId
     * @throws SQLException if there's an error reading the generated key
     */
    @Override
    protected void handleGeneratedKey(PayslipModel entity, ResultSet generatedKeys) throws SQLException {
        if (generatedKeys.next()) {
            entity.setPayslipId(generatedKeys.getInt(1));
        }
    }
    

    // CUSTOM SQL BUILDERS

    
    /**
     * Builds the complete INSERT SQL statement for payslips
     * @return The complete INSERT SQL statement
     */
    private String buildInsertSQL() {
        return "INSERT INTO payslip " +
               "(employeeName, periodStart, periodEnd, monthlyRate, dailyRate, daysWorked, " +
               "overtime, riceSubsidy, phoneAllowance, clothingAllowance, sss, philhealth, " +
               "pagibig, withholdingTax, grossIncome, takeHomePay, payPeriodId, payrollId, " +
               "employeeId, positionId) " +
               "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    }
    
    /**
     * Builds the complete UPDATE SQL statement for payslips
     * @return The complete UPDATE SQL statement
     */
    private String buildUpdateSQL() {
        return "UPDATE payslip SET " +
               "employeeName = ?, periodStart = ?, periodEnd = ?, monthlyRate = ?, dailyRate = ?, " +
               "daysWorked = ?, overtime = ?, riceSubsidy = ?, phoneAllowance = ?, clothingAllowance = ?, " +
               "sss = ?, philhealth = ?, pagibig = ?, withholdingTax = ?, grossIncome = ?, takeHomePay = ?, " +
               "payPeriodId = ?, payrollId = ?, employeeId = ?, positionId = ? " +
               "WHERE payslipId = ?";
    }
    

    // CUSTOM PAYSLIP METHODS

    
    /**
     * Generates a payslip for a specific employee based on their payroll record
     * This method creates a detailed payslip from the payroll data
     * @param employeeId The employee ID
     * @param payPeriodId The pay period ID
     * @return The generated PayslipModel, or null if generation fails
     */
    public PayslipModel generatePayslip(Integer employeeId, Integer payPeriodId) {
        // First, check if payslip already exists
        if (payslipExists(employeeId, payPeriodId)) {
            System.out.println("⚠️ Payslip already exists for employee " + employeeId + " in period " + payPeriodId);
            return findExistingPayslip(employeeId, payPeriodId);
        }
        
        // Get employee and payroll information
        String sql = "SELECT e.employeeId, e.firstName, e.lastName, e.basicSalary, e.hourlyRate, e.positionId, " +
                    "p.payrollId, p.grossIncome, p.totalBenefit, p.totalDeduction, p.netSalary, " +
                    "pp.startDate, pp.endDate " +
                    "FROM employee e " +
                    "JOIN payroll p ON e.employeeId = p.employeeId " +
                    "JOIN payperiod pp ON p.payPeriodId = pp.payPeriodId " +
                    "WHERE e.employeeId = ? AND p.payPeriodId = ?";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, employeeId);
            stmt.setInt(2, payPeriodId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    // Create payslip from data
                    PayslipModel payslip = buildPayslipFromData(rs, payPeriodId);
                    
                    // Save the payslip
                    if (save(payslip)) {
                        System.out.println("✅ Generated payslip for employee " + employeeId);
                        return payslip;
                    } else {
                        System.err.println("❌ Failed to save payslip for employee " + employeeId);
                    }
                } else {
                    System.err.println("❌ No payroll data found for employee " + employeeId + " in period " + payPeriodId);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error generating payslip: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Generates payslips for all employees in a pay period
     * @param payPeriodId The pay period ID
     * @return Number of payslips generated
     */
    public int generateAllPayslips(Integer payPeriodId) {
        String sql = "SELECT DISTINCT employeeId FROM payroll WHERE payPeriodId = ?";
        int generatedCount = 0;
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, payPeriodId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Integer employeeId = rs.getInt("employeeId");
                    if (generatePayslip(employeeId, payPeriodId) != null) {
                        generatedCount++;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error generating all payslips: " + e.getMessage());
        }
        
        return generatedCount;
    }
    
    /**
     * Finds all payslips for a specific employee
     * @param employeeId The employee ID
     * @return List of payslips for the employee ordered by period end date (newest first)
     */
    public List<PayslipModel> findByEmployee(Integer employeeId) {
        String sql = "SELECT ps.*, pp.periodName " +
                    "FROM payslip ps " +
                    "JOIN payperiod pp ON ps.payPeriodId = pp.payPeriodId " +
                    "WHERE ps.employeeId = ? " +
                    "ORDER BY ps.periodEnd DESC";
        return executeQuery(sql, employeeId);
    }
    
    /**
     * Finds all payslips for a specific pay period
     * @param payPeriodId The pay period ID
     * @return List of payslips for the specified pay period ordered by employee name
     */
    public List<PayslipModel> findByPayPeriod(Integer payPeriodId) {
        String sql = "SELECT * FROM payslip WHERE payPeriodId = ? ORDER BY employeeName";
        return executeQuery(sql, payPeriodId);
    }
    
    /**
     * Prints/formats a payslip for display or printing
     * This method returns a formatted string representation of the payslip
     * @param payslipId The payslip ID to print
     * @return Formatted payslip string, or null if payslip not found
     */
    public String printPayslip(Integer payslipId) {
        PayslipModel payslip = findById(payslipId);
        if (payslip == null) {
            return "Payslip not found with ID: " + payslipId;
        }
        
        return formatPayslipForPrint(payslip);
    }
    
    /**
     * Gets payroll summary for a pay period
     * @param payPeriodId The pay period ID
     * @return PayrollSummary with totals
     */
    public PayrollSummary getPayrollSummary(Integer payPeriodId) {
        String sql = "SELECT " +
                    "COUNT(*) as employeeCount, " +
                    "COALESCE(SUM(grossIncome), 0) as totalGrossIncome, " +
                    "COALESCE(SUM(takeHomePay), 0) as totalTakeHomePay, " +
                    "COALESCE(SUM(sss + philhealth + pagibig + withholdingTax), 0) as totalDeductions " +
                    "FROM payslip WHERE payPeriodId = ?";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, payPeriodId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    PayrollSummary summary = new PayrollSummary();
                    summary.setEmployeeCount(rs.getInt("employeeCount"));
                    summary.setTotalGrossIncome(rs.getBigDecimal("totalGrossIncome"));
                    summary.setTotalTakeHomePay(rs.getBigDecimal("totalTakeHomePay"));
                    summary.setTotalDeductions(rs.getBigDecimal("totalDeductions"));
                    return summary;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting payroll summary: " + e.getMessage());
        }
        
        return new PayrollSummary(); // Return empty summary if error
    }
    
    /**
     * Deletes all payslips for a specific pay period
     * @param payPeriodId The pay period ID
     * @return Number of payslips deleted
     */
    public int deletePayrollByPeriod(Integer payPeriodId) {
        String sql = "DELETE FROM payslip WHERE payPeriodId = ?";
        return executeUpdate(sql, payPeriodId);
    }
    

    // HELPER METHODS

    
    /**
     * Checks if a payslip already exists for an employee in a specific pay period
     * @param employeeId The employee ID
     * @param payPeriodId The pay period ID
     * @return true if payslip exists, false otherwise
     */
    private boolean payslipExists(Integer employeeId, Integer payPeriodId) {
        String sql = "SELECT COUNT(*) FROM payslip WHERE employeeId = ? AND payPeriodId = ?";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, employeeId);
            stmt.setInt(2, payPeriodId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error checking payslip existence: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Finds existing payslip for employee and pay period
     * @param employeeId The employee ID
     * @param payPeriodId The pay period ID
     * @return Existing PayslipModel or null
     */
    private PayslipModel findExistingPayslip(Integer employeeId, Integer payPeriodId) {
        String sql = "SELECT * FROM payslip WHERE employeeId = ? AND payPeriodId = ?";
        List<PayslipModel> results = executeQuery(sql, employeeId, payPeriodId);
        return results.isEmpty() ? null : results.get(0);
    }
    
    /**
     * Builds a PayslipModel from database result set
     * @param rs The ResultSet containing employee and payroll data
     * @param payPeriodId The pay period ID
     * @return Fully populated PayslipModel
     * @throws SQLException if there's an error reading the result set
     */
    private PayslipModel buildPayslipFromData(ResultSet rs, Integer payPeriodId) throws SQLException {
        PayslipModel payslip = new PayslipModel();
        
        // Basic information
        payslip.setEmployeeId(rs.getInt("employeeId"));
        payslip.setEmployeeName(rs.getString("firstName") + " " + rs.getString("lastName"));
        payslip.setPositionId(rs.getInt("positionId"));
        payslip.setPayPeriodId(payPeriodId);
        payslip.setPayrollId(rs.getInt("payrollId"));
        
        // Period dates
        Date startDate = rs.getDate("startDate");
        Date endDate = rs.getDate("endDate");
        if (startDate != null) payslip.setPeriodStart(startDate.toLocalDate());
        if (endDate != null) payslip.setPeriodEnd(endDate.toLocalDate());
        
        // Basic salary information
        BigDecimal basicSalary = rs.getBigDecimal("basicSalary");
        payslip.setMonthlyRate(basicSalary);
        
        // Calculate daily rate (assuming 22 working days per month)
        BigDecimal dailyRate = basicSalary.divide(new BigDecimal("22"), 2, RoundingMode.HALF_UP);
        payslip.setDailyRate(dailyRate);
        
        // Calculate detailed breakdown
        calculatePayslipDetails(payslip, rs);
        
        return payslip;
    }
    
    /**
     * Calculates detailed payslip breakdown (days worked, overtime, benefits, deductions)
     * @param payslip The PayslipModel to populate
     * @param rs The ResultSet containing payroll data
     * @throws SQLException if there's an error reading the result set
     */
    private void calculatePayslipDetails(PayslipModel payslip, ResultSet rs) throws SQLException {
        Integer employeeId = payslip.getEmployeeId();
        LocalDate periodStart = payslip.getPeriodStart();
        LocalDate periodEnd = payslip.getPeriodEnd();
        BigDecimal hourlyRate = rs.getBigDecimal("hourlyRate");
        
        // Calculate days worked from attendance
        int daysWorked = calculateDaysWorked(employeeId, periodStart, periodEnd);
        payslip.setDaysWorked(daysWorked);
        
        // Calculate overtime pay
        BigDecimal overtimePay = calculateOvertimePay(employeeId, periodStart, periodEnd, hourlyRate);
        payslip.setOvertime(overtimePay);
        
        // Calculate benefits
        BigDecimal[] benefits = calculateBenefits(payslip.getPositionId());
        payslip.setRiceSubsidy(benefits[0]);
        payslip.setPhoneAllowance(benefits[1]);
        payslip.setClothingAllowance(benefits[2]);
        
        // Calculate deductions
        BigDecimal[] deductions = calculateDeductions(payslip.getMonthlyRate());
        payslip.setSss(deductions[0]);
        payslip.setPhilhealth(deductions[1]);
        payslip.setPagibig(deductions[2]);
        payslip.setWithholdingTax(deductions[3]);
        
        // Set totals from payroll record
        payslip.setGrossIncome(rs.getBigDecimal("grossIncome"));
        payslip.setTakeHomePay(rs.getBigDecimal("netSalary"));
    }
    
    /**
     * Calculates days worked from attendance records
     * @param employeeId The employee ID
     * @param periodStart The period start date
     * @param periodEnd The period end date
     * @return Number of days worked
     */
    private int calculateDaysWorked(Integer employeeId, LocalDate periodStart, LocalDate periodEnd) {
        String sql = "SELECT COUNT(*) FROM attendance " +
                    "WHERE employeeId = ? AND date BETWEEN ? AND ? " +
                    "AND timeIn IS NOT NULL AND timeOut IS NOT NULL";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, employeeId);
            stmt.setDate(2, Date.valueOf(periodStart));
            stmt.setDate(3, Date.valueOf(periodEnd));
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error calculating days worked: " + e.getMessage());
        }
        
        return 22; // Default to full month if calculation fails
    }
    
    /**
     * Calculates overtime pay from approved overtime requests
     * @param employeeId The employee ID
     * @param periodStart The period start date
     * @param periodEnd The period end date
     * @param hourlyRate The employee's hourly rate
     * @return Total overtime pay
     */
    private BigDecimal calculateOvertimePay(Integer employeeId, LocalDate periodStart, LocalDate periodEnd, BigDecimal hourlyRate) {
        String sql = "SELECT COALESCE(SUM(TIMESTAMPDIFF(MINUTE, overtimeStart, overtimeEnd)), 0) as totalMinutes " +
                    "FROM overtimerequest " +
                    "WHERE employeeId = ? AND approvalStatus = 'Approved' " +
                    "AND DATE(overtimeStart) BETWEEN ? AND ?";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, employeeId);
            stmt.setDate(2, Date.valueOf(periodStart));
            stmt.setDate(3, Date.valueOf(periodEnd));
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    long totalMinutes = rs.getLong("totalMinutes");
                    if (totalMinutes > 0) {
                        // Convert minutes to hours and multiply by 1.5 (time-and-a-half)
                        BigDecimal overtimeHours = new BigDecimal(totalMinutes).divide(new BigDecimal("60"), 2, RoundingMode.HALF_UP);
                        return overtimeHours.multiply(hourlyRate).multiply(new BigDecimal("1.5"));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error calculating overtime pay: " + e.getMessage());
        }
        
        return BigDecimal.ZERO;
    }
    
    /**
     * Calculates benefits based on position
     * @param positionId The position ID
     * @return Array of benefits [riceSubsidy, phoneAllowance, clothingAllowance]
     */
    private BigDecimal[] calculateBenefits(Integer positionId) {
        String sql = "SELECT bt.benefitName, pb.benefitValue " +
                    "FROM positionbenefit pb " +
                    "JOIN benefittype bt ON pb.benefitTypeId = bt.benefitTypeId " +
                    "WHERE pb.positionId = ?";
        
        BigDecimal riceSubsidy = BigDecimal.ZERO;
        BigDecimal phoneAllowance = BigDecimal.ZERO;
        BigDecimal clothingAllowance = BigDecimal.ZERO;
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, positionId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String benefitName = rs.getString("benefitName");
                    BigDecimal benefitValue = rs.getBigDecimal("benefitValue");
                    
                    if (benefitValue != null) {
                        switch (benefitName) {
                            case "Rice Subsidy" -> riceSubsidy = benefitValue;
                            case "Phone Allowance" -> phoneAllowance = benefitValue;
                            case "Clothing Allowance" -> clothingAllowance = benefitValue;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error calculating benefits: " + e.getMessage());
        }
        
        return new BigDecimal[]{riceSubsidy, phoneAllowance, clothingAllowance};
    }
    
    /**
     * Calculates deductions (SSS, PhilHealth, Pag-IBIG, Withholding Tax)
     * @param basicSalary The basic salary
     * @return Array of deductions [sss, philhealth, pagibig, withholdingTax]
     */
    private BigDecimal[] calculateDeductions(BigDecimal basicSalary) {
        // SSS: 4.5% of basic salary (employee share)
        BigDecimal sss = basicSalary.multiply(new BigDecimal("0.045")).setScale(2, RoundingMode.HALF_UP);
        
        // PhilHealth: 2.75% of basic salary (employee share)
        BigDecimal philhealth = basicSalary.multiply(new BigDecimal("0.0275")).setScale(2, RoundingMode.HALF_UP);
        
        // Pag-IBIG: 2% of basic salary (employee share)
        BigDecimal pagibig = basicSalary.multiply(new BigDecimal("0.02")).setScale(2, RoundingMode.HALF_UP);
        
        // Withholding Tax: Simplified calculation
        BigDecimal withholdingTax = calculateWithholdingTax(basicSalary);
        
        return new BigDecimal[]{sss, philhealth, pagibig, withholdingTax};
    }
    
    /**
     * Calculates withholding tax (simplified)
     * @param grossIncome The gross income
     * @return Withholding tax amount
     */
    private BigDecimal calculateWithholdingTax(BigDecimal grossIncome) {
        if (grossIncome.compareTo(new BigDecimal("20833")) <= 0) {
            return BigDecimal.ZERO; // No tax for income 250,000 and below annually
        } else if (grossIncome.compareTo(new BigDecimal("33333")) <= 0) {
            // 20% tax rate
            return grossIncome.subtract(new BigDecimal("20833")).multiply(new BigDecimal("0.20")).setScale(2, RoundingMode.HALF_UP);
        } else {
            // Higher tax rates
            return grossIncome.multiply(new BigDecimal("0.25")).setScale(2, RoundingMode.HALF_UP);
        }
    }
    
    /**
     * Formats a payslip for printing/display
     * @param payslip The PayslipModel to format
     * @return Formatted payslip string
     */
    private String formatPayslipForPrint(PayslipModel payslip) {
        StringBuilder sb = new StringBuilder();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
        
        // Header
        sb.append("═".repeat(60)).append("\n");
        sb.append("                    MOTORPH PAYSLIP                     \n");
        sb.append("═".repeat(60)).append("\n");
        sb.append("Employee: ").append(payslip.getEmployeeName()).append("\n");
        sb.append("Pay Period: ").append(payslip.getPeriodStart().format(formatter))
          .append(" - ").append(payslip.getPeriodEnd().format(formatter)).append("\n");
        sb.append("Payslip ID: ").append(payslip.getPayslipId()).append("\n");
        sb.append("─".repeat(60)).append("\n");
        
        // Earnings Section
        sb.append("EARNINGS:\n");
        sb.append(String.format("Basic Salary (%d days @ ₱%.2f)    ₱%,9.2f\n", 
                payslip.getDaysWorked(), payslip.getDailyRate(), 
                payslip.getDailyRate().multiply(new BigDecimal(payslip.getDaysWorked()))));
        
        if (payslip.getOvertime().compareTo(BigDecimal.ZERO) > 0) {
            sb.append(String.format("Overtime Pay                       ₱%,9.2f\n", payslip.getOvertime()));
        }
        
        // Benefits
        if (payslip.getRiceSubsidy().compareTo(BigDecimal.ZERO) > 0) {
            sb.append(String.format("Rice Subsidy                       ₱%,9.2f\n", payslip.getRiceSubsidy()));
        }
        if (payslip.getPhoneAllowance().compareTo(BigDecimal.ZERO) > 0) {
            sb.append(String.format("Phone Allowance                    ₱%,9.2f\n", payslip.getPhoneAllowance()));
        }
        if (payslip.getClothingAllowance().compareTo(BigDecimal.ZERO) > 0) {
            sb.append(String.format("Clothing Allowance                 ₱%,9.2f\n", payslip.getClothingAllowance()));
        }
        
        sb.append("─".repeat(60)).append("\n");
        sb.append(String.format("GROSS INCOME                       ₱%,9.2f\n", payslip.getGrossIncome()));
        sb.append("─".repeat(60)).append("\n");
        
        // Deductions Section
        sb.append("DEDUCTIONS:\n");
        sb.append(String.format("SSS Contribution                   ₱%,9.2f\n", payslip.getSss()));
        sb.append(String.format("PhilHealth Contribution            ₱%,9.2f\n", payslip.getPhilhealth()));
        sb.append(String.format("Pag-IBIG Contribution              ₱%,9.2f\n", payslip.getPagibig()));
        sb.append(String.format("Withholding Tax                    ₱%,9.2f\n", payslip.getWithholdingTax()));
        sb.append("─".repeat(60)).append("\n");
        
        BigDecimal totalDeductions = payslip.getSss().add(payslip.getPhilhealth()).add(payslip.getPagibig()).add(payslip.getWithholdingTax());
        sb.append(String.format("TOTAL DEDUCTIONS                  ₱%,9.2f\n", totalDeductions));
        sb.append("─".repeat(60)).append("\n");
        
        // Net Pay
        sb.append(String.format("NET PAY                            ₱%,9.2f\n", payslip.getTakeHomePay()));
        sb.append("═".repeat(60)).append("\n");
        
        // Footer
        sb.append("This is a computer-generated payslip.\n");
        sb.append("Generated on: ").append(java.time.LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        
        return sb.toString();
    }
    

    // OVERRIDE METHODS

    /**
     * Override the save method to use custom INSERT SQL
     * @param payslip The payslip to save
     * @return true if save was successful, false otherwise
     */
    @Override
    public boolean save(PayslipModel payslip) {
        String sql = buildInsertSQL();
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            setInsertParameters(stmt, payslip);
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        handleGeneratedKey(payslip, generatedKeys);
                    }
                }
                return true;
            }
            return false;
            
        } catch (SQLException e) {
            System.err.println("Error saving payslip: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Override the update method to use custom UPDATE SQL
     * @param payslip The payslip to update
     * @return true if update was successful, false otherwise
     */
    @Override
    public boolean update(PayslipModel payslip) {
        String sql = buildUpdateSQL();
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            setUpdateParameters(stmt, payslip);
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            System.err.println("Error updating payslip: " + e.getMessage());
            return false;
        }
    }
    

    // INNER CLASS - For payroll summary

    
    /**
     * Inner class to hold payroll summary information
     */
    public static class PayrollSummary {
        private int employeeCount;
        private BigDecimal totalGrossIncome = BigDecimal.ZERO;
        private BigDecimal totalTakeHomePay = BigDecimal.ZERO;
        private BigDecimal totalDeductions = BigDecimal.ZERO;
        
        // Getters and setters
        public int getEmployeeCount() { return employeeCount; }
        public void setEmployeeCount(int employeeCount) { this.employeeCount = employeeCount; }
        
        public BigDecimal getTotalGrossIncome() { return totalGrossIncome; }
        public void setTotalGrossIncome(BigDecimal totalGrossIncome) { 
            this.totalGrossIncome = totalGrossIncome != null ? totalGrossIncome : BigDecimal.ZERO; 
        }
        
        public BigDecimal getTotalTakeHomePay() { return totalTakeHomePay; }
        public void setTotalTakeHomePay(BigDecimal totalTakeHomePay) { 
            this.totalTakeHomePay = totalTakeHomePay != null ? totalTakeHomePay : BigDecimal.ZERO; 
        }
        
        public BigDecimal getTotalDeductions() { return totalDeductions; }
        public void setTotalDeductions(BigDecimal totalDeductions) { 
            this.totalDeductions = totalDeductions != null ? totalDeductions : BigDecimal.ZERO; 
        }
        
        @Override
        public String toString() {
            return String.format("PayrollSummary{employees=%d, grossIncome=%s, takeHomePay=%s, deductions=%s}",
                    employeeCount, totalGrossIncome, totalTakeHomePay, totalDeductions);
        }
    }
}