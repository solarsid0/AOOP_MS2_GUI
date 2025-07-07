package DAOs;

import Models.PayrollModel;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.LocalDate;
import java.util.List;
import java.time.ZoneId;
import java.util.ArrayList;
import java.time.LocalDateTime;
/**
 * Data Access Object for PayrollModel entities.
 * This class handles all database operations related to payroll processing.
 * It extends BaseDAO to inherit common CRUD operations and adds payroll-specific methods.
 * @author User
 */
public class PayrollDAO extends BaseDAO<PayrollModel, Integer> {
    
    public static final ZoneId MANILA_TIMEZONE = ZoneId.of("Asia/Manila");
    /**
     * Constructor that accepts a DatabaseConnection instance
     * @param databaseConnection The database connection to use for all operations
     */
    public PayrollDAO(DatabaseConnection databaseConnection) {
        super(databaseConnection);
    }
    

    // ABSTRACT METHOD IMPLEMENTATIONS - Required by BaseDAO

    
    /**
     * Converts a database row into a PayrollModel object
     * This method reads each column from the ResultSet and creates a PayrollModel
     * @param rs The ResultSet containing payroll data from the database
     * @return A fully populated PayrollModel object
     * @throws SQLException if there's an error reading from the database
     */
    @Override
    protected PayrollModel mapResultSetToEntity(ResultSet rs) throws SQLException {
        PayrollModel payroll = new PayrollModel();
        
        // Set basic payroll information
        payroll.setPayrollId(rs.getInt("payrollId"));
        payroll.setBasicSalary(rs.getBigDecimal("basicSalary"));
        payroll.setGrossIncome(rs.getBigDecimal("grossIncome"));
        payroll.setTotalBenefit(rs.getBigDecimal("totalBenefit"));
        payroll.setTotalDeduction(rs.getBigDecimal("totalDeduction"));
        payroll.setNetSalary(rs.getBigDecimal("netSalary"));
        
        // Handle timestamp fields
        Timestamp createdAt = rs.getTimestamp("createdAt");
        if (createdAt != null) {
            payroll.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        Timestamp updatedAt = rs.getTimestamp("updatedAt");
        if (updatedAt != null) {
            payroll.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        
        // Handle foreign keys
        payroll.setPayPeriodId(rs.getInt("payPeriodId"));
        payroll.setEmployeeId(rs.getInt("employeeId"));
        
        return payroll;
    }
    
    /**
     * Returns the database table name for payroll
     * @return "payroll" - the name of the payroll table in the database
     */
    @Override
    protected String getTableName() {
        return "payroll";
    }
    
    /**
     * Returns the primary key column name for the payroll table
     * @return "payrollId" - the primary key column name
     */
    @Override
    protected String getPrimaryKeyColumn() {
        return "payrollId";
    }
    
    /**
     * Sets parameters for INSERT operations when creating new payroll records
     * This method maps PayrollModel object properties to SQL parameters
     * @param stmt The PreparedStatement to set parameters on
     * @param payroll The PayrollModel object to get values from
     * @throws SQLException if there's an error setting parameters
     */
    @Override
    protected void setInsertParameters(PreparedStatement stmt, PayrollModel payroll) throws SQLException {
        // Note: payrollId is auto-increment, createdAt and updatedAt have defaults
        int paramIndex = 1;
        
        // Set financial information
        stmt.setBigDecimal(paramIndex++, payroll.getBasicSalary());
        stmt.setBigDecimal(paramIndex++, payroll.getGrossIncome());
        stmt.setBigDecimal(paramIndex++, payroll.getTotalBenefit());
        stmt.setBigDecimal(paramIndex++, payroll.getTotalDeduction());
        stmt.setBigDecimal(paramIndex++, payroll.getNetSalary());
        
        // Set foreign keys
        stmt.setInt(paramIndex++, payroll.getPayPeriodId());
        stmt.setInt(paramIndex++, payroll.getEmployeeId());
    }
    
    /**
     * Sets parameters for UPDATE operations when modifying existing payroll records
     * This method maps PayrollModel object properties to SQL parameters for updates
     * @param stmt The PreparedStatement to set parameters on
     * @param payroll The PayrollModel object with updated values
     * @throws SQLException if there's an error setting parameters
     */
    @Override
    protected void setUpdateParameters(PreparedStatement stmt, PayrollModel payroll) throws SQLException {
        int paramIndex = 1;
        
        // Set all the same fields as INSERT (excluding auto-increment ID and timestamps)
        stmt.setBigDecimal(paramIndex++, payroll.getBasicSalary());
        stmt.setBigDecimal(paramIndex++, payroll.getGrossIncome());
        stmt.setBigDecimal(paramIndex++, payroll.getTotalBenefit());
        stmt.setBigDecimal(paramIndex++, payroll.getTotalDeduction());
        stmt.setBigDecimal(paramIndex++, payroll.getNetSalary());
        stmt.setInt(paramIndex++, payroll.getPayPeriodId());
        stmt.setInt(paramIndex++, payroll.getEmployeeId());
        
        // Finally, set the payroll ID for the WHERE clause
        stmt.setInt(paramIndex++, payroll.getPayrollId());
    }
    
    /**
     * Gets the ID from a PayrollModel object
     * This is used by BaseDAO for update and delete operations
     * @param payroll The PayrollModel object to get ID from
     * @return The payroll's ID
     */
    @Override
    protected Integer getEntityId(PayrollModel payroll) {
        return payroll.getPayrollId();
    }
    
    /**
     * Handles auto-generated payroll IDs after INSERT operations
     * This method sets the generated payrollId back on the PayrollModel object
     * @param entity The PayrollModel that was just inserted
     * @param generatedKeys The ResultSet containing the generated payrollId
     * @throws SQLException if there's an error reading the generated key
     */
    @Override
    protected void handleGeneratedKey(PayrollModel entity, ResultSet generatedKeys) throws SQLException {
        if (generatedKeys.next()) {
            entity.setPayrollId(generatedKeys.getInt(1));
        }
    }
    

    // CUSTOM SQL BUILDERS

    
    /**
     * Builds the complete INSERT SQL statement for payroll
     * @return The complete INSERT SQL statement
     */
    private String buildInsertSQL() {
        return "INSERT INTO payroll " +
               "(basicSalary, grossIncome, totalBenefit, totalDeduction, netSalary, payPeriodId, employeeId) " +
               "VALUES (?, ?, ?, ?, ?, ?, ?)";
    }
    
    /**
     * Builds the complete UPDATE SQL statement for payroll
     * @return The complete UPDATE SQL statement
     */
    private String buildUpdateSQL() {
        return "UPDATE payroll SET " +
               "basicSalary = ?, grossIncome = ?, totalBenefit = ?, totalDeduction = ?, " +
               "netSalary = ?, payPeriodId = ?, employeeId = ?, updatedAt = CURRENT_TIMESTAMP " +
               "WHERE payrollId = ?";
    }
    

    // CUSTOM PAYROLL METHODS

    
    /**
     * Finds all payroll records for a specific pay period
     * @param payPeriodId The pay period ID
     * @return List of payroll records for the specified pay period
     */
    public List<PayrollModel> findByPayPeriod(Integer payPeriodId) {
        String sql = "SELECT p.*, e.firstName, e.lastName " +
                    "FROM payroll p " +
                    "JOIN employee e ON p.employeeId = e.employeeId " +
                    "WHERE p.payPeriodId = ? " +
                    "ORDER BY e.lastName, e.firstName";
        return executeQuery(sql, payPeriodId);
    }
    
    /**
     * Generates payroll for all active employees in a specific pay period
     * This method creates payroll records by calculating salary, benefits, and deductions
     * @param payPeriodId The pay period ID to generate payroll for
     * @return Number of payroll records generated successfully
     */
    public int generatePayroll(Integer payPeriodId) {
        // First, get pay period information
        String periodSql = "SELECT startDate, endDate FROM payperiod WHERE payPeriodId = ?";
        LocalDate periodStart = null;
        LocalDate periodEnd = null;
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(periodSql)) {
            
            stmt.setInt(1, payPeriodId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    periodStart = rs.getDate("startDate").toLocalDate();
                    periodEnd = rs.getDate("endDate").toLocalDate();
                } else {
                    System.err.println("Pay period not found: " + payPeriodId);
                    return 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting pay period information: " + e.getMessage());
            return 0;
        }
        
        // Generate payroll for all active employees
        String employeeSql = "SELECT employeeId, firstName, lastName, basicSalary, hourlyRate " +
                            "FROM employee " +
                            "WHERE status != 'Terminated'";
        
        int generatedCount = 0;
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(employeeSql)) {
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Integer employeeId = rs.getInt("employeeId");
                    BigDecimal basicSalary = rs.getBigDecimal("basicSalary");
                    BigDecimal hourlyRate = rs.getBigDecimal("hourlyRate");
                    
                    // Check if payroll already exists for this employee and period
                    if (!payrollExists(employeeId, payPeriodId)) {
                        // Generate payroll for this employee
                        PayrollModel payroll = generateEmployeePayroll(
                            employeeId, basicSalary, hourlyRate, payPeriodId, periodStart, periodEnd
                        );
                        
                        if (save(payroll)) {
                            generatedCount++;
                            System.out.println("✅ Generated payroll for employee " + employeeId);
                        }
                    } else {
                        System.out.println("⚠️ Payroll already exists for employee " + employeeId + " in period " + payPeriodId);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error generating payroll: " + e.getMessage());
            e.printStackTrace();
        }
        
        return generatedCount;
    }
    
    /**
     * Updates payroll status by modifying the updatedAt timestamp
     * @param payPeriodId The pay period ID
     * @param status The new status description
     * @return Number of payroll records updated
     */
    public int updatePayrollStatus(Integer payPeriodId, String status) {
        String sql = "UPDATE payroll SET updatedAt = CURRENT_TIMESTAMP WHERE payPeriodId = ?";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, payPeriodId);
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                System.out.println("✅ Updated " + rowsAffected + " payroll records to status: " + status);
            }
            
            return rowsAffected;
            
        } catch (SQLException e) {
            System.err.println("Error updating payroll status: " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }
    
    /**
     * Gets payroll history for a specific employee
     * @param employeeId The employee ID
     * @param limit Maximum number of records to return (0 for all records)
     * @return List of historical payroll records for the employee
     */
    public List<PayrollModel> getPayrollHistory(Integer employeeId, int limit) {
        String sql = "SELECT p.*, pp.periodName, pp.startDate, pp.endDate " +
                    "FROM payroll p " +
                    "JOIN payperiod pp ON p.payPeriodId = pp.payPeriodId " +
                    "WHERE p.employeeId = ? " +
                    "ORDER BY pp.endDate DESC";
        
        if (limit > 0) {
            sql += " LIMIT " + limit;
        }
        
        return executeQuery(sql, employeeId);
    }
    
    /**
     * Finds payroll records by employee ID
     * @param employeeId The employee ID
     * @return List of payroll records for the employee
     */
    public List<PayrollModel> findByEmployee(Integer employeeId) {
        String sql = "SELECT * FROM payroll WHERE employeeId = ? ORDER BY createdAt DESC";
        return executeQuery(sql, employeeId);
    }
    
    /**
     * Gets payroll summary for a pay period
     * @param payPeriodId The pay period ID
     * @return PayrollSummary with totals
     */
    public PayrollSummary getPayrollSummary(Integer payPeriodId) {
        String sql = "SELECT " +
                    "COUNT(*) as employeeCount, " +
                    "SUM(grossIncome) as totalGrossIncome, " +
                    "SUM(netSalary) as totalNetSalary, " +
                    "SUM(totalDeduction) as totalDeductions, " +
                    "SUM(totalBenefit) as totalBenefits " +
                    "FROM payroll WHERE payPeriodId = ?";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, payPeriodId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    PayrollSummary summary = new PayrollSummary();
                    summary.setEmployeeCount(rs.getInt("employeeCount"));
                    summary.setTotalGrossIncome(rs.getBigDecimal("totalGrossIncome"));
                    summary.setTotalNetSalary(rs.getBigDecimal("totalNetSalary"));
                    summary.setTotalDeductions(rs.getBigDecimal("totalDeductions"));
                    summary.setTotalBenefits(rs.getBigDecimal("totalBenefits"));
                    return summary;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting payroll summary: " + e.getMessage());
        }
        
        return new PayrollSummary(); // Return empty summary if error
    }
    
    /**
     * Deletes all payroll records for a specific pay period
     * @param payPeriodId The pay period ID
     * @return Number of payroll records deleted
     */
    public int deletePayrollByPeriod(Integer payPeriodId) {
        String sql = "DELETE FROM payroll WHERE payPeriodId = ?";
        return executeUpdate(sql, payPeriodId);
    }
    
    /**
     * Checks if payroll has been generated for a pay period
     * @param payPeriodId The pay period ID
     * @return true if payroll exists for the period
     */
    public boolean isPayrollGenerated(Integer payPeriodId) {
        String sql = "SELECT COUNT(*) FROM payroll WHERE payPeriodId = ?";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, payPeriodId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error checking payroll generation: " + e.getMessage());
        }
        
        return false;
    }
    
    public LocalDateTime getCurrentManilaTime() {
        return LocalDateTime.now(MANILA_TIMEZONE);
    }

    // HELPER METHODS

    
    /**
     * Checks if a payroll record already exists for an employee in a specific pay period
     * @param employeeId The employee ID
     * @param payPeriodId The pay period ID
     * @return true if payroll exists, false otherwise
     */
    private boolean payrollExists(Integer employeeId, Integer payPeriodId) {
        String sql = "SELECT COUNT(*) FROM payroll WHERE employeeId = ? AND payPeriodId = ?";
        
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
            System.err.println("Error checking payroll existence: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Generates a payroll record for a specific employee
     * This method calculates all salary components, benefits, and deductions
     * @param employeeId The employee ID
     * @param basicSalary The employee's basic salary
     * @param hourlyRate The employee's hourly rate
     * @param payPeriodId The pay period ID
     * @param periodStart The pay period start date
     * @param periodEnd The pay period end date
     * @return A fully calculated PayrollModel
     */
    private PayrollModel generateEmployeePayroll(Integer employeeId, BigDecimal basicSalary,
                                               BigDecimal hourlyRate, Integer payPeriodId,
                                               LocalDate periodStart, LocalDate periodEnd) {
        
        PayrollModel payroll = new PayrollModel();
        
        // Set basic information
        payroll.setEmployeeId(employeeId);
        payroll.setPayPeriodId(payPeriodId);
        payroll.setBasicSalary(basicSalary);
        
        // Calculate total benefits
        BigDecimal totalBenefits = calculateTotalBenefits(employeeId, periodStart, periodEnd);
        payroll.setTotalBenefit(totalBenefits);
        
        // Calculate overtime pay
        BigDecimal overtimePay = calculateOvertimePayEnhanced(employeeId, periodStart, periodEnd, hourlyRate);
        
        // Calculate gross income (basic + overtime + benefits)
        BigDecimal grossIncome = basicSalary.add(overtimePay).add(totalBenefits);
        payroll.setGrossIncome(grossIncome);
        
        // Calculate total deductions
        BigDecimal totalDeductions = calculateTotalDeductions(employeeId, basicSalary, grossIncome);
        payroll.setTotalDeduction(totalDeductions);
        
        // Calculate net salary
        BigDecimal netSalary = grossIncome.subtract(totalDeductions);
        payroll.setNetSalary(netSalary);
        
        return payroll;
    }
    
    /**
     * Calculates total benefits for an employee
     * @param employeeId The employee ID
     * @param periodStart The period start date
     * @param periodEnd The period end date
     * @return Total benefits amount
     */
    private BigDecimal calculateTotalBenefits(Integer employeeId, LocalDate periodStart, LocalDate periodEnd) {
        String sql = "SELECT COALESCE(SUM(pb.benefitValue), 0) as totalBenefits " +
                    "FROM employee e " +
                    "JOIN positionbenefit pb ON e.positionId = pb.positionId " +
                    "WHERE e.employeeId = ?";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, employeeId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    BigDecimal benefits = rs.getBigDecimal("totalBenefits");
                    return benefits != null ? benefits : BigDecimal.ZERO;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error calculating benefits: " + e.getMessage());
        }
        
        return BigDecimal.ZERO;
    }
    
    /**
     * Calculates overtime pay from approved overtime requests
     * @param employeeId The employee ID
     * @param periodStart The period start date
     * @param periodEnd The period end date
     * @param hourlyRate The employee's hourly rate
     * @return Total overtime pay
     */
    private BigDecimal calculateOvertimePayEnhanced(Integer employeeId, LocalDate periodStart, LocalDate periodEnd, BigDecimal hourlyRate) {
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
     * Calculates total deductions (SSS, PhilHealth, Pag-IBIG, Withholding Tax)
     * @param employeeId The employee ID
     * @param basicSalary The basic salary
     * @param grossIncome The gross income
     * @return Total deductions amount
     */
    private BigDecimal calculateTotalDeductions(Integer employeeId, BigDecimal basicSalary, BigDecimal grossIncome) {
        // SSS: 4.5% of basic salary (employee share)
        BigDecimal sss = basicSalary.multiply(new BigDecimal("0.045")).setScale(2, RoundingMode.HALF_UP);
        
        // PhilHealth: 2.75% of basic salary (employee share)
        BigDecimal philhealth = basicSalary.multiply(new BigDecimal("0.0275")).setScale(2, RoundingMode.HALF_UP);
        
        // Pag-IBIG: 2% of basic salary (employee share)
        BigDecimal pagibig = basicSalary.multiply(new BigDecimal("0.02")).setScale(2, RoundingMode.HALF_UP);
        
        // Withholding Tax: Simplified calculation
        BigDecimal withholdingTax = calculateWithholdingTax(grossIncome);
        
        return sss.add(philhealth).add(pagibig).add(withholdingTax);
    }
    
    /**
     * Calculates withholding tax (simplified)
     * @param grossIncome The gross income
     * @return Withholding tax amount
     */
    private BigDecimal calculateWithholdingTax(BigDecimal grossIncome) {
        BigDecimal monthlyGross = grossIncome;
        
        if (monthlyGross.compareTo(new BigDecimal("20833")) <= 0) {
            return BigDecimal.ZERO; // No tax for income 250,000 and below annually
        } else if (monthlyGross.compareTo(new BigDecimal("33333")) <= 0) {
            // 20% tax rate
            return monthlyGross.subtract(new BigDecimal("20833")).multiply(new BigDecimal("0.20"));
        } else {
            // Higher tax rates
            return monthlyGross.multiply(new BigDecimal("0.25")).setScale(2, RoundingMode.HALF_UP);
        }
    }
    

    // OVERRIDE METHODS

    
    /**
     * Override the save method to use custom INSERT SQL
     * @param payroll The payroll to save
     * @return true if save was successful, false otherwise
     */
    @Override
    public boolean save(PayrollModel payroll) {
        String sql = buildInsertSQL();
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            setInsertParameters(stmt, payroll);
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        handleGeneratedKey(payroll, generatedKeys);
                    }
                }
                return true;
            }
            return false;
            
        } catch (SQLException e) {
            System.err.println("Error saving payroll: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Override the update method to use custom UPDATE SQL
     * @param payroll The payroll to update
     * @return true if update was successful, false otherwise
     */
    @Override
    public boolean update(PayrollModel payroll) {
        String sql = buildUpdateSQL();
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            setUpdateParameters(stmt, payroll);
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            System.err.println("Error updating payroll: " + e.getMessage());
            e.printStackTrace();
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
        private BigDecimal totalNetSalary = BigDecimal.ZERO;
        private BigDecimal totalDeductions = BigDecimal.ZERO;
        private BigDecimal totalBenefits = BigDecimal.ZERO;
        
        // Getters and setters
        public int getEmployeeCount() { return employeeCount; }
        public void setEmployeeCount(int employeeCount) { this.employeeCount = employeeCount; }
        
        public BigDecimal getTotalGrossIncome() { return totalGrossIncome; }
        public void setTotalGrossIncome(BigDecimal totalGrossIncome) { 
            this.totalGrossIncome = totalGrossIncome != null ? totalGrossIncome : BigDecimal.ZERO; 
        }
        
        public BigDecimal getTotalNetSalary() { return totalNetSalary; }
        public void setTotalNetSalary(BigDecimal totalNetSalary) { 
            this.totalNetSalary = totalNetSalary != null ? totalNetSalary : BigDecimal.ZERO; 
        }
        
        public BigDecimal getTotalDeductions() { return totalDeductions; }
        public void setTotalDeductions(BigDecimal totalDeductions) { 
            this.totalDeductions = totalDeductions != null ? totalDeductions : BigDecimal.ZERO; 
        }
        
        public BigDecimal getTotalBenefits() { return totalBenefits; }
        public void setTotalBenefits(BigDecimal totalBenefits) { 
            this.totalBenefits = totalBenefits != null ? totalBenefits : BigDecimal.ZERO; 
        }
        
        @Override
        public String toString() {
            return String.format("PayrollSummary{employees=%d, grossIncome=%s, netSalary=%s, deductions=%s, benefits=%s}",
                    employeeCount, totalGrossIncome, totalNetSalary, totalDeductions, totalBenefits);
        }
    }
}