
package DAOs;

import Models.PayrollOvertime;
import java.math.BigDecimal;
import java.sql.*;
import java.util.List;
import java.util.ArrayList;

/**
 * PayrollOvertimeDAO - Junction table DAO for payroll-overtime relationships
 * Links payroll records with overtime requests and stores computed hours/pay
 * @author Chad
 */
public class PayrollOvertimeDAO {
    
    private final DatabaseConnection databaseConnection;
    
    public PayrollOvertimeDAO(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }
    
    /**
     * Create - Link payroll with overtime request
     * @param payrollOvertime
     * @return 
     */
    public boolean save(PayrollOvertime payrollOvertime) {
        String sql = "INSERT INTO payrollovertime (payrollId, overtimeRequestId, overtimeHours, overtimePay) VALUES (?, ?, ?, ?)";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, payrollOvertime.getPayrollId());
            pstmt.setInt(2, payrollOvertime.getOvertimeRequestId());
            pstmt.setBigDecimal(3, payrollOvertime.getOvertimeHours());
            pstmt.setBigDecimal(4, payrollOvertime.getOvertimePay());
            
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Error saving payroll overtime: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Read - Find all overtime records for a payroll
     * @param payrollId
     * @return 
     */
    public List<PayrollOvertime> findByPayrollId(Integer payrollId) {
        List<PayrollOvertime> overtimeList = new ArrayList<>();
        String sql = "SELECT po.*, ot.overtimeStart, ot.overtimeEnd, ot.overtimeReason " +
                    "FROM payrollovertime po " +
                    "JOIN overtimerequest ot ON po.overtimeRequestId = ot.overtimeRequestId " +
                    "WHERE po.payrollId = ? ORDER BY ot.overtimeStart";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, payrollId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                PayrollOvertime po = new PayrollOvertime();
                po.setPayrollId(rs.getInt("payrollId"));
                po.setOvertimeRequestId(rs.getInt("overtimeRequestId"));
                po.setOvertimeHours(rs.getBigDecimal("overtimeHours"));
                po.setOvertimePay(rs.getBigDecimal("overtimePay"));
                overtimeList.add(po);
            }
        } catch (SQLException e) {
            System.err.println("Error finding payroll overtime: " + e.getMessage());
        }
        return overtimeList;
    }
    
    /**
     * Read - Find overtime records by overtime request
     * @param overtimeRequestId
     * @return 
     */
    public List<PayrollOvertime> findByOvertimeRequestId(Integer overtimeRequestId) {
        List<PayrollOvertime> overtimeList = new ArrayList<>();
        String sql = "SELECT * FROM payrollovertime WHERE overtimeRequestId = ?";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, overtimeRequestId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                PayrollOvertime po = new PayrollOvertime();
                po.setPayrollId(rs.getInt("payrollId"));
                po.setOvertimeRequestId(rs.getInt("overtimeRequestId"));
                po.setOvertimeHours(rs.getBigDecimal("overtimeHours"));
                po.setOvertimePay(rs.getBigDecimal("overtimePay"));
                overtimeList.add(po);
            }
        } catch (SQLException e) {
            System.err.println("Error finding payroll overtime by request: " + e.getMessage());
        }
        return overtimeList;
    }
    
    /**
     * Update overtime hours and pay
     * @param payrollOvertime
     * @return 
     */
    public boolean update(PayrollOvertime payrollOvertime) {
        String sql = "UPDATE payrollovertime SET overtimeHours = ?, overtimePay = ? WHERE payrollId = ? AND overtimeRequestId = ?";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setBigDecimal(1, payrollOvertime.getOvertimeHours());
            pstmt.setBigDecimal(2, payrollOvertime.getOvertimePay());
            pstmt.setInt(3, payrollOvertime.getPayrollId());
            pstmt.setInt(4, payrollOvertime.getOvertimeRequestId());
            
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Error updating payroll overtime: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Delete - Remove payroll-overtime link
     * @param payrollId
     * @param overtimeRequestId
     * @return 
     */
    public boolean delete(Integer payrollId, Integer overtimeRequestId) {
        String sql = "DELETE FROM payrollovertime WHERE payrollId = ? AND overtimeRequestId = ?";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, payrollId);
            pstmt.setInt(2, overtimeRequestId);
            
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Error deleting payroll overtime: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Delete all overtime links for a payroll
     * @param payrollId
     * @return 
     */
    public boolean deleteByPayrollId(Integer payrollId) {
        String sql = "DELETE FROM payrollovertime WHERE payrollId = ?";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, payrollId);
            return pstmt.executeUpdate() >= 0;
            
        } catch (SQLException e) {
            System.err.println("Error deleting payroll overtime by payroll: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Calculate total overtime hours for a payroll
     * @param payrollId
     * @return 
     */
    public BigDecimal getTotalOvertimeHours(Integer payrollId) {
        String sql = "SELECT COALESCE(SUM(overtimeHours), 0) FROM payrollovertime WHERE payrollId = ?";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, payrollId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getBigDecimal(1);
            }
        } catch (SQLException e) {
            System.err.println("Error getting total overtime hours: " + e.getMessage());
        }
        return BigDecimal.ZERO;
    }
    
    /**
     * Calculate total overtime pay for a payroll
     * @param payrollId
     * @return 
     */
    public BigDecimal getTotalOvertimePay(Integer payrollId) {
        String sql = "SELECT COALESCE(SUM(overtimePay), 0) FROM payrollovertime WHERE payrollId = ?";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, payrollId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getBigDecimal(1);
            }
        } catch (SQLException e) {
            System.err.println("Error getting total overtime pay: " + e.getMessage());
        }
        return BigDecimal.ZERO;
    }
    
    /**
     * Get overtime summary for a payroll (regular vs weekend vs holiday)
     * @param payrollId
     * @return 
     */
    public OvertimeSummary getOvertimeSummary(Integer payrollId) {
        OvertimeSummary summary = new OvertimeSummary();
        String sql = "SELECT " +
                    "SUM(CASE WHEN DAYOFWEEK(ot.overtimeStart) IN (1, 7) THEN po.overtimeHours ELSE 0 END) as weekendHours, " +
                    "SUM(CASE WHEN DAYOFWEEK(ot.overtimeStart) NOT IN (1, 7) THEN po.overtimeHours ELSE 0 END) as regularHours, " +
                    "SUM(CASE WHEN DAYOFWEEK(ot.overtimeStart) IN (1, 7) THEN po.overtimePay ELSE 0 END) as weekendPay, " +
                    "SUM(CASE WHEN DAYOFWEEK(ot.overtimeStart) NOT IN (1, 7) THEN po.overtimePay ELSE 0 END) as regularPay " +
                    "FROM payrollovertime po " +
                    "JOIN overtimerequest ot ON po.overtimeRequestId = ot.overtimeRequestId " +
                    "WHERE po.payrollId = ?";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, payrollId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                summary.setWeekendHours(rs.getBigDecimal("weekendHours") != null ? rs.getBigDecimal("weekendHours") : BigDecimal.ZERO);
                summary.setRegularHours(rs.getBigDecimal("regularHours") != null ? rs.getBigDecimal("regularHours") : BigDecimal.ZERO);
                summary.setWeekendPay(rs.getBigDecimal("weekendPay") != null ? rs.getBigDecimal("weekendPay") : BigDecimal.ZERO);
                summary.setRegularPay(rs.getBigDecimal("regularPay") != null ? rs.getBigDecimal("regularPay") : BigDecimal.ZERO);
            }
        } catch (SQLException e) {
            System.err.println("Error getting overtime summary: " + e.getMessage());
        }
        return summary;
    }
    
    /**
     * Bulk insert overtime records for payroll
     * @param overtimeList
     * @return 
     */
    public boolean bulkInsert(List<PayrollOvertime> overtimeList) {
        if (overtimeList == null || overtimeList.isEmpty()) {
            return false;
        }
        
        String sql = "INSERT INTO payrollovertime (payrollId, overtimeRequestId, overtimeHours, overtimePay) VALUES (?, ?, ?, ?)";
        
        try (Connection conn = databaseConnection.createConnection()) {
            conn.setAutoCommit(false);
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (PayrollOvertime po : overtimeList) {
                    pstmt.setInt(1, po.getPayrollId());
                    pstmt.setInt(2, po.getOvertimeRequestId());
                    pstmt.setBigDecimal(3, po.getOvertimeHours());
                    pstmt.setBigDecimal(4, po.getOvertimePay());
                    pstmt.addBatch();
                }
                
                int[] results = pstmt.executeBatch();
                conn.commit();
                
                // Check if all inserts were successful
                for (int result : results) {
                    if (result <= 0) {
                        return false;
                    }
                }
                return true;
                
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
            
        } catch (SQLException e) {
            System.err.println("Error bulk inserting payroll overtime: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if overtime request is already linked to payroll
     * @param payrollId
     * @param overtimeRequestId
     * @return 
     */
    public boolean isLinked(Integer payrollId, Integer overtimeRequestId) {
        String sql = "SELECT COUNT(*) FROM payrollovertime WHERE payrollId = ? AND overtimeRequestId = ?";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, payrollId);
            pstmt.setInt(2, overtimeRequestId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error checking overtime link: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Get rank-and-file employees' overtime for payroll period
     * Based on business logic: Only rank-and-file employees are eligible for overtime
     * @param payrollId
     * @return 
     */
    public List<PayrollOvertime> getRankAndFileOvertime(Integer payrollId) {
        List<PayrollOvertime> overtimeList = new ArrayList<>();
        String sql = "SELECT po.*, ot.overtimeStart, ot.overtimeEnd, p.position, p.department " +
                    "FROM payrollovertime po " +
                    "JOIN overtimerequest ot ON po.overtimeRequestId = ot.overtimeRequestId " +
                    "JOIN payroll pr ON po.payrollId = pr.payrollId " +
                    "JOIN employee e ON pr.employeeId = e.employeeId " +
                    "JOIN position p ON e.positionId = p.positionId " +
                    "WHERE po.payrollId = ? " +
                    "AND (LOWER(p.department) = 'rank-and-file' OR LOWER(p.position) LIKE '%rank%file%') " +
                    "ORDER BY ot.overtimeStart";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, payrollId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                PayrollOvertime po = new PayrollOvertime();
                po.setPayrollId(rs.getInt("payrollId"));
                po.setOvertimeRequestId(rs.getInt("overtimeRequestId"));
                po.setOvertimeHours(rs.getBigDecimal("overtimeHours"));
                po.setOvertimePay(rs.getBigDecimal("overtimePay"));
                overtimeList.add(po);
            }
        } catch (SQLException e) {
            System.err.println("Error finding rank-and-file overtime: " + e.getMessage());
        }
        return overtimeList;
    }
    
    /**
     * Inner class for overtime summary
     */
    public static class OvertimeSummary {
        private BigDecimal regularHours = BigDecimal.ZERO;
        private BigDecimal weekendHours = BigDecimal.ZERO;
        private BigDecimal regularPay = BigDecimal.ZERO;
        private BigDecimal weekendPay = BigDecimal.ZERO;
        
        public BigDecimal getRegularHours() { return regularHours; }
        public void setRegularHours(BigDecimal regularHours) { this.regularHours = regularHours; }
        
        public BigDecimal getWeekendHours() { return weekendHours; }
        public void setWeekendHours(BigDecimal weekendHours) { this.weekendHours = weekendHours; }
        
        public BigDecimal getRegularPay() { return regularPay; }
        public void setRegularPay(BigDecimal regularPay) { this.regularPay = regularPay; }
        
        public BigDecimal getWeekendPay() { return weekendPay; }
        public void setWeekendPay(BigDecimal weekendPay) { this.weekendPay = weekendPay; }
        
        public BigDecimal getTotalHours() {
            return regularHours.add(weekendHours);
        }
        
        public BigDecimal getTotalPay() {
            return regularPay.add(weekendPay);
        }
        
        @Override
        public String toString() {
            return String.format("Overtime Summary: %.2f hours (Regular: %.2f, Weekend: %.2f), Total Pay: ₱%.2f", 
                    getTotalHours(), regularHours, weekendHours, getTotalPay());
        }
    }
}