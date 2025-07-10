package DAOs;

import Models.PayrollOvertime;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PayrollOvertimeDAO {
    
    private DatabaseConnection databaseConnection;
    
    public PayrollOvertimeDAO() {
        this.databaseConnection = new DatabaseConnection();
    }
    
    public PayrollOvertimeDAO(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }
    
    /**
     * Populate payrollovertime table for a specific payroll record
     */
    public int populateForPayroll(Integer payrollId, Integer employeeId, LocalDate startDate, LocalDate endDate) {
        String sql = """
            INSERT INTO payrollovertime (payrollId, overtimeRequestId, overtimeHours, overtimePay)
            SELECT ?, ot.overtimeRequestId,
                   TIMESTAMPDIFF(HOUR, ot.overtimeStart, ot.overtimeEnd) as overtimeHours,
                   TIMESTAMPDIFF(HOUR, ot.overtimeStart, ot.overtimeEnd) * e.hourlyRate * 1.25 as overtimePay
            FROM overtimerequest ot
            JOIN employee e ON ot.employeeId = e.employeeId
            WHERE ot.employeeId = ? 
            AND ot.approvalStatus = 'Approved'
            AND DATE(ot.overtimeStart) BETWEEN ? AND ?
            """;
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, payrollId);
            stmt.setInt(2, employeeId);
            stmt.setDate(3, Date.valueOf(startDate));
            stmt.setDate(4, Date.valueOf(endDate));
            
            return stmt.executeUpdate();
            
        } catch (SQLException e) {
            System.err.println("Error populating payrollovertime: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Generate overtime records for a pay period - FIXED table and column names
     */
    public int generateOvertimeRecords(int payPeriodId) {
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
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, payPeriodId);
            return stmt.executeUpdate();
            
        } catch (SQLException e) {
            System.err.println("Error generating overtime records: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Delete records for a pay period - FIXED column names
     */
    public int deleteByPayPeriod(int payPeriodId) {
        String sql = """
            DELETE po FROM payrollovertime po
            JOIN payroll p ON po.payrollId = p.payrollId
            WHERE p.payPeriodId = ?
            """;
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, payPeriodId);
            return stmt.executeUpdate();
            
        } catch (SQLException e) {
            System.err.println("Error deleting payroll overtime records: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Check if records exist for a pay period - FIXED column names
     */
    public boolean hasRecordsForPeriod(int payPeriodId) {
        String sql = """
            SELECT COUNT(*) FROM payrollovertime po
            JOIN payroll p ON po.payrollId = p.payrollId
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
            System.err.println("Error checking payroll overtime records: " + e.getMessage());
            return false;
        }
    }
    
    public List<PayrollOvertime> findByPayPeriod(int payPeriodId) {
        List<PayrollOvertime> overtimeList = new ArrayList<>();
        String sql = """
            SELECT po.* FROM payrollovertime po
            JOIN payroll p ON po.payrollId = p.payrollId
            WHERE p.payPeriodId = ?
            """;
        
        try (Connection connection = databaseConnection.createConnection();
             PreparedStatement pstmt = connection.prepareStatement(sql)) {
            
            pstmt.setInt(1, payPeriodId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                PayrollOvertime overtime = mapResultSetToModel(rs);
                overtimeList.add(overtime);
            }
        } catch (SQLException e) {
            System.err.println("Error finding payroll overtime: " + e.getMessage());
            e.printStackTrace();
        }
        
        return overtimeList;
    }
    
    public PayrollOvertime findByPayrollId(int payrollId) {
        String sql = "SELECT * FROM payrollovertime WHERE payrollId = ?";
        
        try (Connection connection = databaseConnection.createConnection();
             PreparedStatement pstmt = connection.prepareStatement(sql)) {
            
            pstmt.setInt(1, payrollId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return mapResultSetToModel(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error finding payroll overtime by payroll ID: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    public PayrollOvertime findByEmployeeAndPeriod(int employeeId, int payPeriodId) {
        String sql = """
            SELECT po.* FROM payrollovertime po
            JOIN payroll p ON po.payrollId = p.payrollId
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
            System.err.println("Error finding payroll overtime by employee and period: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    private PayrollOvertime mapResultSetToModel(ResultSet rs) throws SQLException {
        PayrollOvertime overtime = new PayrollOvertime();
        overtime.setPayrollId(rs.getInt("payrollId"));
        overtime.setOvertimeRequestId(rs.getInt("overtimeRequestId"));
        overtime.setOvertimeHours(rs.getBigDecimal("overtimeHours"));
        overtime.setOvertimePay(rs.getBigDecimal("overtimePay"));
        return overtime;
    }
}