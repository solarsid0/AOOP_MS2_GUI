package DAOs;

import Models.PayrollBenefit;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PayrollBenefitDAO {
    
    private DatabaseConnection databaseConnection;
    
    public PayrollBenefitDAO() {
        this.databaseConnection = new DatabaseConnection();
    }
    
    public PayrollBenefitDAO(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }
    
    /**
     * Populate payrollbenefit table for a specific payroll record
     */
    public int populateForPayroll(Integer payrollId, Integer employeeId) {
        String sql = """
            INSERT INTO payrollbenefit (payrollId, benefitTypeId, benefitAmount)
            SELECT ?, pb.benefitTypeId, pb.benefitValue
            FROM employee e
            JOIN positionbenefit pb ON e.positionId = pb.positionId
            WHERE e.employeeId = ?
            """;
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, payrollId);
            stmt.setInt(2, employeeId);
            
            return stmt.executeUpdate();
            
        } catch (SQLException e) {
            System.err.println("Error populating payrollbenefit: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Generate benefit records for a pay period - FIXED table and column names
     */
    public int generateBenefitRecords(int payPeriodId) {
        String sql = """
            INSERT INTO payrollbenefit (payrollId, benefitTypeId, benefitAmount)
            SELECT p.payrollId, pb.benefitTypeId, pb.benefitValue
            FROM payroll p
            JOIN employee e ON p.employeeId = e.employeeId
            JOIN positionbenefit pb ON e.positionId = pb.positionId
            WHERE p.payPeriodId = ?
            """;
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, payPeriodId);
            return stmt.executeUpdate();
            
        } catch (SQLException e) {
            System.err.println("Error generating benefit records: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Delete records for a pay period - FIXED column names
     */
    public int deleteByPayPeriod(int payPeriodId) {
        String sql = """
            DELETE pb FROM payrollbenefit pb
            JOIN payroll p ON pb.payrollId = p.payrollId
            WHERE p.payPeriodId = ?
            """;
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, payPeriodId);
            return stmt.executeUpdate();
            
        } catch (SQLException e) {
            System.err.println("Error deleting payroll benefit records: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Check if records exist for a pay period - FIXED column names
     */
    public boolean hasRecordsForPeriod(int payPeriodId) {
        String sql = """
            SELECT COUNT(*) FROM payrollbenefit pb
            JOIN payroll p ON pb.payrollId = p.payrollId
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
            System.err.println("Error checking payroll benefit records: " + e.getMessage());
            return false;
        }
    }
    
    public List<PayrollBenefit> findByPayPeriod(int payPeriodId) {
        List<PayrollBenefit> benefitList = new ArrayList<>();
        String sql = """
            SELECT pb.* FROM payrollbenefit pb
            JOIN payroll p ON pb.payrollId = p.payrollId
            WHERE p.payPeriodId = ?
            """;
        
        try (Connection connection = databaseConnection.createConnection();
             PreparedStatement pstmt = connection.prepareStatement(sql)) {
            
            pstmt.setInt(1, payPeriodId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                PayrollBenefit benefit = mapResultSetToModel(rs);
                benefitList.add(benefit);
            }
        } catch (SQLException e) {
            System.err.println("Error finding payroll benefits: " + e.getMessage());
            e.printStackTrace();
        }
        
        return benefitList;
    }
    
    public PayrollBenefit findByPayrollId(int payrollId) {
        String sql = "SELECT * FROM payrollbenefit WHERE payrollId = ?";
        
        try (Connection connection = databaseConnection.createConnection();
             PreparedStatement pstmt = connection.prepareStatement(sql)) {
            
            pstmt.setInt(1, payrollId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return mapResultSetToModel(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error finding payroll benefit by payroll ID: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    public PayrollBenefit findByEmployeeAndPeriod(int employeeId, int payPeriodId) {
        String sql = """
            SELECT pb.* FROM payrollbenefit pb
            JOIN payroll p ON pb.payrollId = p.payrollId
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
            System.err.println("Error finding payroll benefit by employee and period: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    private PayrollBenefit mapResultSetToModel(ResultSet rs) throws SQLException {
        PayrollBenefit benefit = new PayrollBenefit();
        benefit.setPayrollBenefitId(rs.getInt("payrollBenefitId"));
        benefit.setPayrollId(rs.getInt("payrollId"));
        benefit.setBenefitTypeId(rs.getInt("benefitTypeId"));
        benefit.setBenefitAmount(rs.getBigDecimal("benefitAmount"));
        return benefit;
    }
}