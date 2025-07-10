package DAOs;

import Models.PayrollAttendance;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PayrollAttendanceDAO {
    
    private DatabaseConnection databaseConnection;
    
    public PayrollAttendanceDAO() {
        this.databaseConnection = new DatabaseConnection();
    }
    
    public PayrollAttendanceDAO(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }
    
    /**
     * Populate payrollattendance table for a specific payroll record
     */
    public int populateForPayroll(Integer payrollId, Integer employeeId, LocalDate startDate, LocalDate endDate) {
        String sql = """
            INSERT INTO payrollattendance (payrollId, attendanceId, computedHours, computedAmount)
            SELECT ?, a.attendanceId, 
                   CASE WHEN a.timeIn IS NOT NULL AND a.timeOut IS NOT NULL 
                        THEN GREATEST(0, (TIME_TO_SEC(a.timeOut) - TIME_TO_SEC(a.timeIn)) / 3600.0 - 1.0)
                        ELSE 0 END as computedHours,
                   CASE WHEN a.timeIn IS NOT NULL AND a.timeOut IS NOT NULL 
                        THEN GREATEST(0, (TIME_TO_SEC(a.timeOut) - TIME_TO_SEC(a.timeIn)) / 3600.0 - 1.0) * e.hourlyRate
                        ELSE 0 END as computedAmount
            FROM attendance a
            JOIN employee e ON a.employeeId = e.employeeId
            WHERE a.employeeId = ? AND a.date BETWEEN ? AND ?
            """;
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, payrollId);
            stmt.setInt(2, employeeId);
            stmt.setDate(3, Date.valueOf(startDate));
            stmt.setDate(4, Date.valueOf(endDate));
            
            return stmt.executeUpdate();
            
        } catch (SQLException e) {
            System.err.println("Error populating payrollattendance: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Generate attendance records for a pay period - FIXED table and column names
     */
    public int generateAttendanceRecords(int payPeriodId) {
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

        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, payPeriodId);
            return stmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Error generating attendance records: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Delete by pay period - FIXED column names
     */
    public int deleteByPayPeriod(int payPeriodId) {
        String sql = """
            DELETE pa FROM payrollattendance pa
            JOIN payroll p ON pa.payrollId = p.payrollId
            WHERE p.payPeriodId = ?
            """;

        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, payPeriodId);
            return stmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Error deleting payroll attendance records: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Check if records exist for a pay period - FIXED column names
     */
    public boolean hasRecordsForPeriod(int payPeriodId) {
        String sql = """
            SELECT COUNT(*) FROM payrollattendance pa
            JOIN payroll p ON pa.payrollId = p.payrollId
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
            System.err.println("Error checking payroll attendance records: " + e.getMessage());
            return false;
        }
    }
    
    public List<PayrollAttendance> findByPayPeriod(int payPeriodId) {
        List<PayrollAttendance> attendanceList = new ArrayList<>();
        String sql = """
            SELECT pa.* FROM payrollattendance pa
            JOIN payroll p ON pa.payrollId = p.payrollId
            WHERE p.payPeriodId = ?
            """;
        
        try (Connection connection = databaseConnection.createConnection();
             PreparedStatement pstmt = connection.prepareStatement(sql)) {
            
            pstmt.setInt(1, payPeriodId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                PayrollAttendance attendance = mapResultSetToModel(rs);
                attendanceList.add(attendance);
            }
        } catch (SQLException e) {
            System.err.println("Error finding payroll attendance: " + e.getMessage());
            e.printStackTrace();
        }
        
        return attendanceList;
    }
    
    public PayrollAttendance findByPayrollId(int payrollId) {
        String sql = "SELECT * FROM payrollattendance WHERE payrollId = ?";
        
        try (Connection connection = databaseConnection.createConnection();
             PreparedStatement pstmt = connection.prepareStatement(sql)) {
            
            pstmt.setInt(1, payrollId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return mapResultSetToModel(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error finding payroll attendance by payroll ID: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    public PayrollAttendance findByEmployeeAndPeriod(int employeeId, int payPeriodId) {
        String sql = """
            SELECT pa.* FROM payrollattendance pa
            JOIN payroll p ON pa.payrollId = p.payrollId
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
            System.err.println("Error finding payroll attendance by employee and period: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    private PayrollAttendance mapResultSetToModel(ResultSet rs) throws SQLException {
        PayrollAttendance attendance = new PayrollAttendance();
        attendance.setPayrollId(rs.getInt("payrollId"));
        attendance.setAttendanceId(rs.getInt("attendanceId"));
        attendance.setComputedHours(rs.getBigDecimal("computedHours"));
        attendance.setComputedAmount(rs.getBigDecimal("computedAmount"));
        return attendance;
    }
}