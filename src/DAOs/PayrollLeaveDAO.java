package DAOs;

import Models.PayrollLeave;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PayrollLeaveDAO {
    
    private DatabaseConnection databaseConnection;
    
    public PayrollLeaveDAO() {
        this.databaseConnection = new DatabaseConnection();
    }
    
    public PayrollLeaveDAO(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }
    
    /**
     * Populate payrollleave table for a specific payroll record
     */
    public int populateForPayroll(Integer payrollId, Integer employeeId, LocalDate startDate, LocalDate endDate) {
        String sql = """
            INSERT INTO payrollleave (payrollId, leaveRequestId, leaveHours)
            SELECT ?, lr.leaveRequestId, 
                   (DATEDIFF(LEAST(lr.leaveEnd, ?), GREATEST(lr.leaveStart, ?)) + 1) * 8 as leaveHours
            FROM leaverequest lr
            WHERE lr.employeeId = ? 
            AND lr.approvalStatus = 'Approved'
            AND lr.leaveStart <= ? 
            AND lr.leaveEnd >= ?
            """;
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, payrollId);
            stmt.setDate(2, Date.valueOf(endDate));
            stmt.setDate(3, Date.valueOf(startDate));
            stmt.setInt(4, employeeId);
            stmt.setDate(5, Date.valueOf(endDate));
            stmt.setDate(6, Date.valueOf(startDate));
            
            return stmt.executeUpdate();
            
        } catch (SQLException e) {
            System.err.println("Error populating payrollleave: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Generate leave records for a pay period - FIXED table and column names
     */
    public int generateLeaveRecords(int payPeriodId) {
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
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, payPeriodId);
            return stmt.executeUpdate();
            
        } catch (SQLException e) {
            System.err.println("Error generating leave records: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Delete records for a pay period - FIXED column names
     */
    public int deleteByPayPeriod(int payPeriodId) {
        String sql = """
            DELETE pl FROM payrollleave pl
            JOIN payroll p ON pl.payrollId = p.payrollId
            WHERE p.payPeriodId = ?
            """;
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, payPeriodId);
            return stmt.executeUpdate();
            
        } catch (SQLException e) {
            System.err.println("Error deleting payroll leave records: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Check if records exist for a pay period - FIXED column names
     */
    public boolean hasRecordsForPeriod(int payPeriodId) {
        String sql = """
            SELECT COUNT(*) FROM payrollleave pl
            JOIN payroll p ON pl.payrollId = p.payrollId
            WHERE p.payPeriodId = ?
            """;
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, payPeriodId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            return false;
            
        } catch (SQLException e) {
            System.err.println("Error checking payroll leave records: " + e.getMessage());
            return false;
        }
    }
    
    public List<PayrollLeave> findByPayPeriod(int payPeriodId) {
        List<PayrollLeave> leaveList = new ArrayList<>();
        String sql = """
            SELECT pl.* FROM payrollleave pl
            JOIN payroll p ON pl.payrollId = p.payrollId
            WHERE p.payPeriodId = ?
            """;
        
        try (Connection connection = databaseConnection.createConnection();
             PreparedStatement pstmt = connection.prepareStatement(sql)) {
            
            pstmt.setInt(1, payPeriodId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                PayrollLeave leave = mapResultSetToModel(rs);
                leaveList.add(leave);
            }
        } catch (SQLException e) {
            System.err.println("Error finding payroll leave: " + e.getMessage());
            e.printStackTrace();
        }
        
        return leaveList;
    }
    
    public PayrollLeave findByPayrollId(int payrollId) {
        String sql = "SELECT * FROM payrollleave WHERE payrollId = ?";
        
        try (Connection connection = databaseConnection.createConnection();
             PreparedStatement pstmt = connection.prepareStatement(sql)) {
            
            pstmt.setInt(1, payrollId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return mapResultSetToModel(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error finding payroll leave by payroll ID: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    public PayrollLeave findByEmployeeAndPeriod(int employeeId, int payPeriodId) {
        String sql = """
            SELECT pl.* FROM payrollleave pl
            JOIN payroll p ON pl.payrollId = p.payrollId
            WHERE p.employeeId = ? AND p.payPeriodId = ?
            """;
        
        try (Connection connection = databaseConnection.createConnection();
             PreparedStatement pstmt = connection.prepareStatement(sql)) {
            
            pstmt.setInt(1, employeeId);
            pstmt.setInt(2, payPeriodId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return mapResultSetToModel(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error finding payroll leave by employee and period: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    private PayrollLeave mapResultSetToModel(ResultSet rs) throws SQLException {
        PayrollLeave leave = new PayrollLeave();
        leave.setPayrollId(rs.getInt("payrollId"));
        leave.setLeaveRequestId(rs.getInt("leaveRequestId"));
        leave.setLeaveHours(rs.getBigDecimal("leaveHours"));
        return leave;
    }
}