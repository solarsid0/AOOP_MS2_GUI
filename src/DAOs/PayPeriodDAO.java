package DAOs;

import Models.PayPeriodModel;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

/**
 * PayPeriodDAO - Data Access Object for PayPeriodModel
 * Handles all database operations for pay periods
 * @author User
 */
public class PayPeriodDAO {
    
    private final DatabaseConnection dbConnection;
    
    public PayPeriodDAO() {
        this.dbConnection = new DatabaseConnection();
    }
    
    /**
     * Create - Insert new pay period
     * @param payPeriod
     * @return 
     */
    public boolean save(PayPeriodModel payPeriod) {
        if (!payPeriod.isValid()) {
            System.err.println("Cannot save pay period: Invalid data");
            return false;
        }
        
        String sql = "INSERT INTO payperiod (startDate, endDate, periodName) VALUES (?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setDate(1, java.sql.Date.valueOf(payPeriod.getStartDate()));
            pstmt.setDate(2, java.sql.Date.valueOf(payPeriod.getEndDate()));
            pstmt.setString(3, payPeriod.getPeriodName());
            
            int affectedRows = pstmt.executeUpdate();
            
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        payPeriod.setPayPeriodId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error saving pay period: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Read - Find pay period by ID
     * @param payPeriodId
     * @return 
     */
    public PayPeriodModel findById(int payPeriodId) {
        String sql = "SELECT * FROM payperiod WHERE payPeriodId = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, payPeriodId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return mapResultSetToPayPeriod(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error finding pay period: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Read - Find pay period by name
     * @param periodName
     * @return 
     */
    public PayPeriodModel findByName(String periodName) {
        String sql = "SELECT * FROM payperiod WHERE periodName = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, periodName);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return mapResultSetToPayPeriod(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error finding pay period by name: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Read - Find pay period that contains a specific date
     * @param date
     * @return 
     */
    public PayPeriodModel findByDate(LocalDate date) {
        String sql = "SELECT * FROM payperiod WHERE ? BETWEEN startDate AND endDate";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setDate(1, java.sql.Date.valueOf(date));
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return mapResultSetToPayPeriod(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error finding pay period by date: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Read - Get all pay periods
     * @return 
     */
    public List<PayPeriodModel> findAll() {
        List<PayPeriodModel> payPeriods = new ArrayList<>();
        String sql = "SELECT * FROM payperiod ORDER BY startDate DESC";
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                payPeriods.add(mapResultSetToPayPeriod(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving pay periods: " + e.getMessage());
        }
        return payPeriods;
    }
    
    /**
     * Read - Get pay periods for a specific year
     * @param year
     * @return 
     */
    public List<PayPeriodModel> findByYear(int year) {
        List<PayPeriodModel> payPeriods = new ArrayList<>();
        String sql = "SELECT * FROM payperiod WHERE YEAR(startDate) = ? OR YEAR(endDate) = ? ORDER BY startDate";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, year);
            pstmt.setInt(2, year);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                payPeriods.add(mapResultSetToPayPeriod(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error finding pay periods by year: " + e.getMessage());
        }
        return payPeriods;
    }
    
    /**
     * Read - Get pay periods within a date range
     * @param start
     * @param end
     * @return 
     */
    public List<PayPeriodModel> findByDateRange(LocalDate start, LocalDate end) {
        List<PayPeriodModel> payPeriods = new ArrayList<>();
        String sql = "SELECT * FROM payperiod WHERE " +
                    "(startDate BETWEEN ? AND ?) OR " +
                    "(endDate BETWEEN ? AND ?) OR " +
                    "(startDate <= ? AND endDate >= ?) " +
                    "ORDER BY startDate";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            java.sql.Date startSql = java.sql.Date.valueOf(start);
            java.sql.Date endSql = java.sql.Date.valueOf(end);
            
            pstmt.setDate(1, startSql);
            pstmt.setDate(2, endSql);
            pstmt.setDate(3, startSql);
            pstmt.setDate(4, endSql);
            pstmt.setDate(5, startSql);
            pstmt.setDate(6, endSql);
            
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                payPeriods.add(mapResultSetToPayPeriod(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error finding pay periods by date range: " + e.getMessage());
        }
        return payPeriods;
    }
    
    /**
     * Update - Update existing pay period
     * @param payPeriod
     * @return 
     */
    public boolean update(PayPeriodModel payPeriod) {
        if (payPeriod.getPayPeriodId() == null || payPeriod.getPayPeriodId() <= 0) {
            System.err.println("Cannot update pay period: Invalid pay period ID");
            return false;
        }
        
        if (!payPeriod.isValid()) {
            System.err.println("Cannot update pay period: Invalid data");
            return false;
        }
        
        String sql = "UPDATE payperiod SET startDate = ?, endDate = ?, periodName = ? WHERE payPeriodId = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setDate(1, java.sql.Date.valueOf(payPeriod.getStartDate()));
            pstmt.setDate(2, java.sql.Date.valueOf(payPeriod.getEndDate()));
            pstmt.setString(3, payPeriod.getPeriodName());
            pstmt.setInt(4, payPeriod.getPayPeriodId());
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating pay period: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Delete - Remove pay period from database
     * @param payPeriod
     * @return 
     */
    public boolean delete(PayPeriodModel payPeriod) {
        return deleteById(payPeriod.getPayPeriodId());
    }
    
    /**
     * Delete - Remove pay period by ID
     * @param payPeriodId
     * @return 
     */
    public boolean deleteById(int payPeriodId) {
        // Check if pay period has associated payroll records
        if (hasPayrollRecords(payPeriodId)) {
            System.err.println("Cannot delete pay period: It has associated payroll records");
            return false;
        }
        
        String sql = "DELETE FROM payperiod WHERE payPeriodId = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, payPeriodId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting pay period: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Check if pay period has associated payroll records
     * @param payPeriodId
     * @return 
     */
    public boolean hasPayrollRecords(int payPeriodId) {
        String sql = "SELECT COUNT(*) FROM payroll WHERE payPeriodId = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, payPeriodId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error checking payroll records: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Get number of payroll records for this period
     * @param payPeriodId
     * @return 
     */
    public int getPayrollCount(int payPeriodId) {
        String sql = "SELECT COUNT(*) FROM payroll WHERE payPeriodId = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, payPeriodId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error getting payroll count: " + e.getMessage());
        }
        return 0;
    }
    
    /**
     * Get the current pay period (contains today's date)
     * @return 
     */
    public PayPeriodModel getCurrentPayPeriod() {
        return findByDate(LocalDate.now());
    }
    
    /**
     * Get the previous pay period
     * @param payPeriodId
     * @return 
     */
    public PayPeriodModel getPreviousPeriod(int payPeriodId) {
        PayPeriodModel currentPeriod = findById(payPeriodId);
        if (currentPeriod == null) {
            return null;
        }
        
        String sql = "SELECT * FROM payperiod WHERE endDate < ? ORDER BY endDate DESC LIMIT 1";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setDate(1, java.sql.Date.valueOf(currentPeriod.getStartDate()));
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return mapResultSetToPayPeriod(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error finding previous pay period: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Get the next pay period
     * @param payPeriodId
     * @return 
     */
    public PayPeriodModel getNextPeriod(int payPeriodId) {
        PayPeriodModel currentPeriod = findById(payPeriodId);
        if (currentPeriod == null) {
            return null;
        }
        
        String sql = "SELECT * FROM payperiod WHERE startDate > ? ORDER BY startDate ASC LIMIT 1";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setDate(1, java.sql.Date.valueOf(currentPeriod.getEndDate()));
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return mapResultSetToPayPeriod(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error finding next pay period: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Check if pay period exists
     * @param payPeriodId
     * @return 
     */
    public boolean exists(int payPeriodId) {
        String sql = "SELECT COUNT(*) FROM payperiod WHERE payPeriodId = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, payPeriodId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error checking pay period existence: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Get overlapping pay periods
     * @param startDate
     * @param endDate
     * @param excludeId
     * @return 
     */
    public List<PayPeriodModel> findOverlappingPeriods(LocalDate startDate, LocalDate endDate, Integer excludeId) {
        List<PayPeriodModel> overlapping = new ArrayList<>();
        String sql = "SELECT * FROM payperiod WHERE " +
                    "NOT (endDate < ? OR startDate > ?)";
        
        if (excludeId != null) {
            sql += " AND payPeriodId != ?";
        }
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setDate(1, java.sql.Date.valueOf(startDate));
            pstmt.setDate(2, java.sql.Date.valueOf(endDate));
            
            if (excludeId != null) {
                pstmt.setInt(3, excludeId);
            }
            
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                overlapping.add(mapResultSetToPayPeriod(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error finding overlapping periods: " + e.getMessage());
        }
        return overlapping;
    }
    
    /**
     * Helper method to map ResultSet to PayPeriodModel
     */
    private PayPeriodModel mapResultSetToPayPeriod(ResultSet rs) throws SQLException {
        return new PayPeriodModel(
            rs.getInt("payPeriodId"),
            rs.getDate("startDate").toLocalDate(),
            rs.getDate("endDate").toLocalDate(),
            rs.getString("periodName")
        );
    }
}