
package DAOs;

import Models.PayrollLeave;
import java.math.BigDecimal;
import java.sql.*;
import java.util.List;
import java.util.ArrayList;

/**
 * PayrollLeaveDAO - Junction table DAO for payroll-leave relationships
 * Links payroll records with leave requests and stores computed leave hours
 * @author chad
 */
public class PayrollLeaveDAO {
    
    private final DatabaseConnection databaseConnection;
    
    public PayrollLeaveDAO(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }
    
    /**
     * Create - Link payroll with leave request
     * @param payrollLeave
     * @return 
     */
    public boolean save(PayrollLeave payrollLeave) {
        String sql = "INSERT INTO payrollleave (payrollId, leaveRequestId, leaveHours) VALUES (?, ?, ?)";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, payrollLeave.getPayrollId());
            pstmt.setInt(2, payrollLeave.getLeaveRequestId());
            pstmt.setBigDecimal(3, payrollLeave.getLeaveHours());
            
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Error saving payroll leave: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Read - Find all leave records for a payroll
     * @param payrollId
     * @return 
     */
    public List<PayrollLeave> findByPayrollId(Integer payrollId) {
        List<PayrollLeave> leaveList = new ArrayList<>();
        String sql = "SELECT pl.*, lr.leaveStart, lr.leaveEnd, lr.leaveReason, lr.leaveTypeId " +
                    "FROM payrollleave pl " +
                    "JOIN leaverequest lr ON pl.leaveRequestId = lr.leaveRequestId " +
                    "WHERE pl.payrollId = ? ORDER BY lr.leaveStart";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, payrollId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                PayrollLeave pl = new PayrollLeave();
                pl.setPayrollId(rs.getInt("payrollId"));
                pl.setLeaveRequestId(rs.getInt("leaveRequestId"));
                pl.setLeaveHours(rs.getBigDecimal("leaveHours"));
                leaveList.add(pl);
            }
        } catch (SQLException e) {
            System.err.println("Error finding payroll leave: " + e.getMessage());
        }
        return leaveList;
    }
    
    /**
     * Read - Find leave records by leave request
     * @param leaveRequestId
     * @return 
     */
    public List<PayrollLeave> findByLeaveRequestId(Integer leaveRequestId) {
        List<PayrollLeave> leaveList = new ArrayList<>();
        String sql = "SELECT * FROM payrollleave WHERE leaveRequestId = ?";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, leaveRequestId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                PayrollLeave pl = new PayrollLeave();
                pl.setPayrollId(rs.getInt("payrollId"));
                pl.setLeaveRequestId(rs.getInt("leaveRequestId"));
                pl.setLeaveHours(rs.getBigDecimal("leaveHours"));
                leaveList.add(pl);
            }
        } catch (SQLException e) {
            System.err.println("Error finding payroll leave by request: " + e.getMessage());
        }
        return leaveList;
    }
    
    /**
     * Update leave hours
     * @param payrollLeave
     * @return 
     */
    public boolean update(PayrollLeave payrollLeave) {
        String sql = "UPDATE payrollleave SET leaveHours = ? WHERE payrollId = ? AND leaveRequestId = ?";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setBigDecimal(1, payrollLeave.getLeaveHours());
            pstmt.setInt(2, payrollLeave.getPayrollId());
            pstmt.setInt(3, payrollLeave.getLeaveRequestId());
            
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Error updating payroll leave: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Delete - Remove payroll-leave link
     * @param payrollId
     * @param leaveRequestId
     * @return 
     */
    public boolean delete(Integer payrollId, Integer leaveRequestId) {
        String sql = "DELETE FROM payrollleave WHERE payrollId = ? AND leaveRequestId = ?";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, payrollId);
            pstmt.setInt(2, leaveRequestId);
            
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Error deleting payroll leave: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Delete all leave links for a payroll
     * @param payrollId
     * @return 
     */
    public boolean deleteByPayrollId(Integer payrollId) {
        String sql = "DELETE FROM payrollleave WHERE payrollId = ?";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, payrollId);
            return pstmt.executeUpdate() >= 0;
            
        } catch (SQLException e) {
            System.err.println("Error deleting payroll leave by payroll: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Calculate total leave hours for a payroll
     * @param payrollId
     * @return 
     */
    public BigDecimal getTotalLeaveHours(Integer payrollId) {
        String sql = "SELECT COALESCE(SUM(leaveHours), 0) FROM payrollleave WHERE payrollId = ?";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, payrollId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getBigDecimal(1);
            }
        } catch (SQLException e) {
            System.err.println("Error getting total leave hours: " + e.getMessage());
        }
        return BigDecimal.ZERO;
    }
    
    /**
     * Get leave breakdown by type for a payroll
     * @param payrollId
     * @return 
     */
    public List<LeaveBreakdown> getLeaveBreakdown(Integer payrollId) {
        List<LeaveBreakdown> breakdown = new ArrayList<>();
        String sql = "SELECT lt.leaveTypeName, COALESCE(SUM(pl.leaveHours), 0) as totalHours " +
                    "FROM payrollleave pl " +
                    "JOIN leaverequest lr ON pl.leaveRequestId = lr.leaveRequestId " +
                    "JOIN leavetype lt ON lr.leaveTypeId = lt.leaveTypeId " +
                    "WHERE pl.payrollId = ? " +
                    "GROUP BY lt.leaveTypeId, lt.leaveTypeName";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, payrollId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                LeaveBreakdown item = new LeaveBreakdown();
                item.setLeaveTypeName(rs.getString("leaveTypeName"));
                item.setTotalHours(rs.getBigDecimal("totalHours"));
                breakdown.add(item);
            }
        } catch (SQLException e) {
            System.err.println("Error getting leave breakdown: " + e.getMessage());
        }
        return breakdown;
    }
    
    /**
     * Bulk insert leave records for payroll
     * @param leaveList
     * @return 
     */
    public boolean bulkInsert(List<PayrollLeave> leaveList) {
        if (leaveList == null || leaveList.isEmpty()) {
            return false;
        }
        
        String sql = "INSERT INTO payrollleave (payrollId, leaveRequestId, leaveHours) VALUES (?, ?, ?)";
        
        try (Connection conn = databaseConnection.createConnection()) {
            conn.setAutoCommit(false);
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (PayrollLeave pl : leaveList) {
                    pstmt.setInt(1, pl.getPayrollId());
                    pstmt.setInt(2, pl.getLeaveRequestId());
                    pstmt.setBigDecimal(3, pl.getLeaveHours());
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
            System.err.println("Error bulk inserting payroll leave: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if leave request is already linked to payroll
     * @param payrollId
     * @param leaveRequestId
     * @return 
     */
    public boolean isLinked(Integer payrollId, Integer leaveRequestId) {
        String sql = "SELECT COUNT(*) FROM payrollleave WHERE payrollId = ? AND leaveRequestId = ?";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, payrollId);
            pstmt.setInt(2, leaveRequestId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error checking leave link: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Inner class for leave breakdown
     */
    public static class LeaveBreakdown {
        private String leaveTypeName;
        private BigDecimal totalHours;
        
        public String getLeaveTypeName() { return leaveTypeName; }
        public void setLeaveTypeName(String leaveTypeName) { this.leaveTypeName = leaveTypeName; }
        
        public BigDecimal getTotalHours() { return totalHours; }
        public void setTotalHours(BigDecimal totalHours) { this.totalHours = totalHours; }
        
        @Override
        public String toString() {
            return String.format("%s: %.2f hours", leaveTypeName, totalHours);
        }
    }
}