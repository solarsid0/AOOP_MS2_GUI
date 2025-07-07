
package DAOs;

import Models.PayrollAttendance;
import java.math.BigDecimal;
import java.sql.*;
import java.util.List;
import java.util.ArrayList;

/**
 * PayrollAttendanceDAO - Junction table DAO for payroll-attendance relationships
 * Links payroll records with attendance records and stores computed hours/amounts
 * @author Chad
 */
public class PayrollAttendanceDAO {
    
    private final DatabaseConnection databaseConnection;
    
    public PayrollAttendanceDAO(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }
    
    /**
     * Create - Link payroll with attendance record
     * @param payrollAttendance
     * @return 
     */
    public boolean save(PayrollAttendance payrollAttendance) {
        String sql = "INSERT INTO payrollattendance (payrollId, attendanceId, computedHours, computedAmount) VALUES (?, ?, ?, ?)";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, payrollAttendance.getPayrollId());
            pstmt.setInt(2, payrollAttendance.getAttendanceId());
            pstmt.setBigDecimal(3, payrollAttendance.getComputedHours());
            pstmt.setBigDecimal(4, payrollAttendance.getComputedAmount());
            
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Error saving payroll attendance: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Read - Find all attendance records for a payroll
     * @param payrollId
     * @return 
     */
    public List<PayrollAttendance> findByPayrollId(Integer payrollId) {
        List<PayrollAttendance> attendanceList = new ArrayList<>();
        String sql = "SELECT pa.*, a.date, a.timeIn, a.timeOut, a.employeeId " +
                    "FROM payrollattendance pa " +
                    "JOIN attendance a ON pa.attendanceId = a.attendanceId " +
                    "WHERE pa.payrollId = ? ORDER BY a.date";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, payrollId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                PayrollAttendance pa = new PayrollAttendance();
                pa.setPayrollId(rs.getInt("payrollId"));
                pa.setAttendanceId(rs.getInt("attendanceId"));
                pa.setComputedHours(rs.getBigDecimal("computedHours"));
                pa.setComputedAmount(rs.getBigDecimal("computedAmount"));
                attendanceList.add(pa);
            }
        } catch (SQLException e) {
            System.err.println("Error finding payroll attendance: " + e.getMessage());
        }
        return attendanceList;
    }
    
    /**
     * Update computed hours and amount
     * @param payrollAttendance
     * @return 
     */
    public boolean update(PayrollAttendance payrollAttendance) {
        String sql = "UPDATE payrollattendance SET computedHours = ?, computedAmount = ? WHERE payrollId = ? AND attendanceId = ?";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setBigDecimal(1, payrollAttendance.getComputedHours());
            pstmt.setBigDecimal(2, payrollAttendance.getComputedAmount());
            pstmt.setInt(3, payrollAttendance.getPayrollId());
            pstmt.setInt(4, payrollAttendance.getAttendanceId());
            
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Error updating payroll attendance: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Delete - Remove payroll-attendance link
     * @param payrollId
     * @param attendanceId
     * @return 
     */
    public boolean delete(Integer payrollId, Integer attendanceId) {
        String sql = "DELETE FROM payrollattendance WHERE payrollId = ? AND attendanceId = ?";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, payrollId);
            pstmt.setInt(2, attendanceId);
            
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Error deleting payroll attendance: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Delete all attendance links for a payroll
     * @param payrollId
     * @return 
     */
    public boolean deleteByPayrollId(Integer payrollId) {
        String sql = "DELETE FROM payrollattendance WHERE payrollId = ?";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, payrollId);
            return pstmt.executeUpdate() >= 0;
            
        } catch (SQLException e) {
            System.err.println("Error deleting payroll attendance by payroll: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Calculate total hours for a payroll
     * @param payrollId
     * @return 
     */
    public BigDecimal getTotalHours(Integer payrollId) {
        String sql = "SELECT COALESCE(SUM(computedHours), 0) FROM payrollattendance WHERE payrollId = ?";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, payrollId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getBigDecimal(1);
            }
        } catch (SQLException e) {
            System.err.println("Error getting total hours: " + e.getMessage());
        }
        return BigDecimal.ZERO;
    }
    
    /**
     * Calculate total amount for a payroll
     * @param payrollId
     * @return 
     */
    public BigDecimal getTotalAmount(Integer payrollId) {
        String sql = "SELECT COALESCE(SUM(computedAmount), 0) FROM payrollattendance WHERE payrollId = ?";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, payrollId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getBigDecimal(1);
            }
        } catch (SQLException e) {
            System.err.println("Error getting total amount: " + e.getMessage());
        }
        return BigDecimal.ZERO;
    }
    
    /**
     * Bulk insert attendance records for payroll
     * @param attendanceList
     * @return 
     */
    public boolean bulkInsert(List<PayrollAttendance> attendanceList) {
        if (attendanceList == null || attendanceList.isEmpty()) {
            return false;
        }
        
        String sql = "INSERT INTO payrollattendance (payrollId, attendanceId, computedHours, computedAmount) VALUES (?, ?, ?, ?)";
        
        try (Connection conn = databaseConnection.createConnection()) {
            conn.setAutoCommit(false);
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (PayrollAttendance pa : attendanceList) {
                    pstmt.setInt(1, pa.getPayrollId());
                    pstmt.setInt(2, pa.getAttendanceId());
                    pstmt.setBigDecimal(3, pa.getComputedHours());
                    pstmt.setBigDecimal(4, pa.getComputedAmount());
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
            System.err.println("Error bulk inserting payroll attendance: " + e.getMessage());
            return false;
        }
    }
}