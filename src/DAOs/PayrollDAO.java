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
 * COMPLETE FIXED PayrollDAO - Corrected column names and full monthly salary
 */
public class PayrollDAO extends BaseDAO<PayrollModel, Integer> {
    
    public static final ZoneId MANILA_TIMEZONE = ZoneId.of("Asia/Manila");
    
    public PayrollDAO(DatabaseConnection databaseConnection) {
        super(databaseConnection);
    }
    
    // ABSTRACT METHOD IMPLEMENTATIONS
    
    @Override
    protected PayrollModel mapResultSetToEntity(ResultSet rs) throws SQLException {
        PayrollModel payroll = new PayrollModel();
        
        payroll.setPayrollId(rs.getInt("payrollId"));
        payroll.setBasicSalary(rs.getBigDecimal("basicSalary"));
        payroll.setGrossIncome(rs.getBigDecimal("grossIncome"));
        payroll.setTotalBenefit(rs.getBigDecimal("totalBenefit"));
        payroll.setTotalDeduction(rs.getBigDecimal("totalDeduction"));
        payroll.setNetSalary(rs.getBigDecimal("netSalary"));
        
        Timestamp createdAt = rs.getTimestamp("createdAt");
        if (createdAt != null) {
            payroll.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        Timestamp updatedAt = rs.getTimestamp("updatedAt");
        if (updatedAt != null) {
            payroll.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        
        payroll.setPayPeriodId(rs.getInt("payPeriodId"));
        payroll.setEmployeeId(rs.getInt("employeeId"));
        
        return payroll;
    }
    
    @Override
    protected String getTableName() {
        return "payroll";
    }
    
    @Override
    protected String getPrimaryKeyColumn() {
        return "payrollId";
    }
    
    @Override
    protected void setInsertParameters(PreparedStatement stmt, PayrollModel payroll) throws SQLException {
        int paramIndex = 1;
        
        stmt.setBigDecimal(paramIndex++, payroll.getBasicSalary());
        stmt.setBigDecimal(paramIndex++, payroll.getGrossIncome());
        stmt.setBigDecimal(paramIndex++, payroll.getTotalBenefit());
        stmt.setBigDecimal(paramIndex++, payroll.getTotalDeduction());
        stmt.setBigDecimal(paramIndex++, payroll.getNetSalary());
        stmt.setInt(paramIndex++, payroll.getPayPeriodId());
        stmt.setInt(paramIndex++, payroll.getEmployeeId());
    }
    
    @Override
    protected void setUpdateParameters(PreparedStatement stmt, PayrollModel payroll) throws SQLException {
        int paramIndex = 1;
        
        stmt.setBigDecimal(paramIndex++, payroll.getBasicSalary());
        stmt.setBigDecimal(paramIndex++, payroll.getGrossIncome());
        stmt.setBigDecimal(paramIndex++, payroll.getTotalBenefit());
        stmt.setBigDecimal(paramIndex++, payroll.getTotalDeduction());
        stmt.setBigDecimal(paramIndex++, payroll.getNetSalary());
        stmt.setInt(paramIndex++, payroll.getPayPeriodId());
        stmt.setInt(paramIndex++, payroll.getEmployeeId());
        stmt.setInt(paramIndex++, payroll.getPayrollId());
    }
    
    @Override
    protected Integer getEntityId(PayrollModel payroll) {
        return payroll.getPayrollId();
    }
    
    @Override
    protected void handleGeneratedKey(PayrollModel entity, ResultSet generatedKeys) throws SQLException {
        if (generatedKeys.next()) {
            entity.setPayrollId(generatedKeys.getInt(1));
        }
    }
    
    // CUSTOM SQL BUILDERS
    
    private String buildInsertSQL() {
        return "INSERT INTO payroll " +
               "(basicSalary, grossIncome, totalBenefit, totalDeduction, netSalary, payPeriodId, employeeId) " +
               "VALUES (?, ?, ?, ?, ?, ?, ?)";
    }
    
    private String buildUpdateSQL() {
        return "UPDATE payroll SET " +
               "basicSalary = ?, grossIncome = ?, totalBenefit = ?, totalDeduction = ?, " +
               "netSalary = ?, payPeriodId = ?, employeeId = ?, updatedAt = CURRENT_TIMESTAMP " +
               "WHERE payrollId = ?";
    }
    
    // CUSTOM PAYROLL METHODS
    
    public List<PayrollModel> findByPayPeriod(Integer payPeriodId) {
        String sql = "SELECT p.*, e.firstName, e.lastName " +
                    "FROM payroll p " +
                    "JOIN employee e ON p.employeeId = e.employeeId " +
                    "WHERE p.payPeriodId = ? " +
                    "ORDER BY e.lastName, e.firstName";
        return executeQuery(sql, payPeriodId);
    }
    
    /**
     * FIXED: Generate payroll using FULL MONTHLY SALARY (not divided by 2)
     */
    public int generatePayroll(Integer payPeriodId) {
        // Get pay period information
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
                    
                    // Check if payroll already exists
                    if (!payrollExists(employeeId, payPeriodId)) {
                        // Generate payroll for this employee
                        PayrollModel payroll = generateEmployeePayroll(
                            employeeId, basicSalary, hourlyRate, payPeriodId, periodStart, periodEnd
                        );
                        
                        if (save(payroll)) {
                            generatedCount++;
                            System.out.println("✅ Generated payroll for employee " + employeeId);
                        }
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
     * FIXED: Delete payroll details using CORRECT column names
     */
    public void deletePayrollDetailsByPeriod(int payPeriodId) {
        String[] deleteQueries = {
            "DELETE pa FROM payrollattendance pa JOIN payroll p ON pa.payrollId = p.payrollId WHERE p.payPeriodId = ?",
            "DELETE pb FROM payrollbenefit pb JOIN payroll p ON pb.payrollId = p.payrollId WHERE p.payPeriodId = ?",
            "DELETE pl FROM payrollleave pl JOIN payroll p ON pl.payrollId = p.payrollId WHERE p.payPeriodId = ?",
            "DELETE po FROM payrollovertime po JOIN payroll p ON po.payrollId = p.payrollId WHERE p.payPeriodId = ?"
        };
        
        String[] tableNames = {"payrollattendance", "payrollbenefit", "payrollleave", "payrollovertime"};
        
        for (int i = 0; i < deleteQueries.length; i++) {
            try (Connection connection = databaseConnection.createConnection();
                 PreparedStatement pstmt = connection.prepareStatement(deleteQueries[i])) {
                pstmt.setInt(1, payPeriodId);
                int deleted = pstmt.executeUpdate();
                System.out.println("Deleted " + deleted + " records from " + tableNames[i]);
            } catch (SQLException e) {
                System.err.println("Error deleting from " + tableNames[i] + ": " + e.getMessage());
            }
        }
    }
    
    /**
     * FIXED: Populate payrollattendance using CORRECT column names
     */
    private void populatePayrollAttendance(int payPeriodId) {
        String sql = """
            INSERT INTO payrollattendance (payrollId, attendanceId, computedHours, computedAmount)
            SELECT p.payrollId, a.attendanceId,
                   CASE WHEN a.timeIn IS NOT NULL AND a.timeOut IS NOT NULL 
                        THEN GREATEST(0, (TIME_TO_SEC(a.timeOut) - TIME_TO_SEC(a.timeIn)) / 3600.0 - 1.0)
                        ELSE 0 END as computedHours,
                   CASE WHEN a.timeIn IS NOT NULL AND a.timeOut IS NOT NULL 
                        THEN GREATEST(0, (TIME_TO_SEC(a.timeOut) - TIME_TO_SEC(a.timeIn)) / 3600.0 - 1.0) * e.hourlyRate
                        ELSE 0 END as computedAmount
            FROM payroll p
            JOIN employee e ON p.employeeId = e.employeeId
            JOIN attendance a ON p.employeeId = a.employeeId
            JOIN payperiod pp ON p.payPeriodId = pp.payPeriodId
            WHERE p.payPeriodId = ? 
            AND a.date BETWEEN pp.startDate AND pp.endDate
            """;
        
        try (Connection connection = databaseConnection.createConnection();
             PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, payPeriodId);
            int inserted = pstmt.executeUpdate();
            System.out.println("Populated " + inserted + " payroll attendance records");
        } catch (SQLException e) {
            System.err.println("Error generating attendance records: " + e.getMessage());
        }
    }

    /**
     * FIXED: Populate payrollbenefit using CORRECT column names
     */
    private void populatePayrollBenefits(int payPeriodId) {
        String sql = """
            INSERT INTO payrollbenefit (payrollId, benefitTypeId, benefitAmount)
            SELECT p.payrollId, pb.benefitTypeId, pb.benefitValue
            FROM payroll p
            JOIN employee e ON p.employeeId = e.employeeId
            JOIN positionbenefit pb ON e.positionId = pb.positionId
            WHERE p.payPeriodId = ?
            """;
        
        try (Connection connection = databaseConnection.createConnection();
             PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, payPeriodId);
            int inserted = pstmt.executeUpdate();
            System.out.println("Populated " + inserted + " payroll benefit records");
        } catch (SQLException e) {
            System.err.println("Error generating benefit records: " + e.getMessage());
        }
    }

    /**
     * FIXED: Populate payrollleave using CORRECT column names
     */
    private void populatePayrollLeave(int payPeriodId) {
        String sql = """
            INSERT INTO payrollleave (payrollId, leaveRequestId, leaveHours)
            SELECT p.payrollId, lr.leaveRequestId,
                   (DATEDIFF(LEAST(lr.leaveEnd, pp.endDate), 
                             GREATEST(lr.leaveStart, pp.startDate)) + 1) * 8 as leaveHours
            FROM payroll p
            JOIN payperiod pp ON p.payPeriodId = pp.payPeriodId
            JOIN leaverequest lr ON p.employeeId = lr.employeeId
            WHERE p.payPeriodId = ?
            AND lr.approvalStatus = 'Approved'
            AND lr.leaveStart <= pp.endDate
            AND lr.leaveEnd >= pp.startDate
            """;
        
        try (Connection connection = databaseConnection.createConnection();
             PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, payPeriodId);
            int inserted = pstmt.executeUpdate();
            System.out.println("Populated " + inserted + " payroll leave records");
        } catch (SQLException e) {
            System.err.println("Error generating leave records: " + e.getMessage());
        }
    }

    /**
     * FIXED: Populate payrollovertime using CORRECT column names
     */
    private void populatePayrollOvertime(int payPeriodId) {
        String sql = """
            INSERT INTO payrollovertime (payrollId, overtimeRequestId, overtimeHours, overtimePay)
            SELECT p.payrollId, ot.overtimeRequestId,
                   TIMESTAMPDIFF(HOUR, ot.overtimeStart, ot.overtimeEnd) as overtimeHours,
                   TIMESTAMPDIFF(HOUR, ot.overtimeStart, ot.overtimeEnd) * e.hourlyRate * 1.25 as overtimePay
            FROM payroll p
            JOIN employee e ON p.employeeId = e.employeeId
            JOIN overtimerequest ot ON p.employeeId = ot.employeeId
            JOIN payperiod pp ON p.payPeriodId = pp.payPeriodId
            WHERE p.payPeriodId = ?
            AND ot.approvalStatus = 'Approved'
            AND DATE(ot.overtimeStart) BETWEEN pp.startDate AND pp.endDate
            """;
        
        try (Connection connection = databaseConnection.createConnection();
             PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, payPeriodId);
            int inserted = pstmt.executeUpdate();
            System.out.println("Populated " + inserted + " payroll overtime records");
        } catch (SQLException e) {
            System.err.println("Error generating overtime records: " + e.getMessage());
        }
    }
    
    /**
     * NEW: Generate payroll with all related tables
     */
    public int generatePayrollWithDetails(int payPeriodId) {
        int generatedCount = 0;
        
        try {
            // First generate main payroll records
            generatedCount = generatePayroll(payPeriodId);
            
            if (generatedCount > 0) {
                // Then populate related tables using FIXED methods
                populatePayrollAttendance(payPeriodId);
                populatePayrollBenefits(payPeriodId);
                populatePayrollLeave(payPeriodId);
                populatePayrollOvertime(payPeriodId);
            }
            
        } catch (Exception e) {
            System.err.println("Error generating payroll with details: " + e.getMessage());
            e.printStackTrace();
        }
        
        return generatedCount;
    }
    
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
    
    public List<PayrollModel> findByEmployee(Integer employeeId) {
        String sql = "SELECT * FROM payroll WHERE employeeId = ? ORDER BY createdAt DESC";
        return executeQuery(sql, employeeId);
    }
    
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
        
        return new PayrollSummary();
    }
    
    public int deletePayrollByPeriod(Integer payPeriodId) {
        String sql = "DELETE FROM payroll WHERE payPeriodId = ?";
        return executeUpdate(sql, payPeriodId);
    }
    
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
     * FIXED: Generate payroll for specific employee using FULL MONTHLY SALARY
     */
    private PayrollModel generateEmployeePayroll(Integer employeeId, BigDecimal basicSalary,
                                               BigDecimal hourlyRate, Integer payPeriodId,
                                               LocalDate periodStart, LocalDate periodEnd) {
        
        PayrollModel payroll = new PayrollModel();
        
        // Set basic information
        payroll.setEmployeeId(employeeId);
        payroll.setPayPeriodId(payPeriodId);
        payroll.setBasicSalary(basicSalary); // FIXED: Use full monthly salary
        
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
    
    private BigDecimal calculateWithholdingTax(BigDecimal grossIncome) {
        BigDecimal monthlyGross = grossIncome;
        
        if (monthlyGross.compareTo(new BigDecimal("20833")) <= 0) {
            return BigDecimal.ZERO;
        } else if (monthlyGross.compareTo(new BigDecimal("33333")) <= 0) {
            return monthlyGross.subtract(new BigDecimal("20833")).multiply(new BigDecimal("0.20"));
        } else {
            return monthlyGross.multiply(new BigDecimal("0.25")).setScale(2, RoundingMode.HALF_UP);
        }
    }
    
    // OVERRIDE METHODS
    
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