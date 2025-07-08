package DAOs;

import Models.PositionBenefit;
import java.sql.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * PositionBenefitDAO - Data Access Object for PositionBenefit model
 * Handles database operations for positionbenefit table (junction table)
 * @author Chad
 */
public class PositionBenefitDAO {
    
    private final DatabaseConnection databaseConnection;
    
    public PositionBenefitDAO(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }
    
    /**
     * Get benefit value for a specific position and benefit name
     * This is the key method used by PayrollManagement
     */
    public BigDecimal getBenefitValueByPositionAndName(Integer positionId, String benefitName) {
        String sql = "SELECT pb.benefitValue FROM positionbenefit pb " +
                    "JOIN benefittype bt ON pb.benefitTypeId = bt.benefitTypeId " +
                    "WHERE pb.positionId = ? AND bt.benefitName = ?";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, positionId);
            pstmt.setString(2, benefitName);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    BigDecimal value = rs.getBigDecimal("benefitValue");
                    return value != null ? value : BigDecimal.ZERO;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting benefit value for position " + positionId + 
                             " and benefit " + benefitName + ": " + e.getMessage());
        }
        
        return BigDecimal.ZERO;
    }
    
    /**
     * Get all benefits for a specific position
     */
    public List<PositionBenefit> getBenefitsByPosition(Integer positionId) {
        List<PositionBenefit> benefits = new ArrayList<>();
        String sql = "SELECT benefitTypeId, positionId, benefitValue FROM positionbenefit WHERE positionId = ?";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, positionId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    PositionBenefit benefit = new PositionBenefit();
                    benefit.setBenefitTypeId(rs.getInt("benefitTypeId"));
                    benefit.setPositionId(rs.getInt("positionId"));
                    benefit.setBenefitValue(rs.getBigDecimal("benefitValue"));
                    benefits.add(benefit);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting benefits for position " + positionId + ": " + e.getMessage());
        }
        
        return benefits;
    }
    
    /**
     * Get total benefits value for a position
     */
    public BigDecimal getTotalBenefitsForPosition(Integer positionId) {
        String sql = "SELECT COALESCE(SUM(benefitValue), 0) as totalBenefits FROM positionbenefit WHERE positionId = ?";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, positionId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    BigDecimal total = rs.getBigDecimal("totalBenefits");
                    return total != null ? total : BigDecimal.ZERO;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting total benefits for position " + positionId + ": " + e.getMessage());
        }
        
        return BigDecimal.ZERO;
    }
    
    /**
     * Get benefit details with benefit type name
     */
    public List<BenefitDetail> getBenefitDetailsForPosition(Integer positionId) {
        List<BenefitDetail> details = new ArrayList<>();
        String sql = "SELECT pb.benefitTypeId, pb.positionId, pb.benefitValue, bt.benefitName, bt.benefitDescription " +
                    "FROM positionbenefit pb " +
                    "JOIN benefittype bt ON pb.benefitTypeId = bt.benefitTypeId " +
                    "WHERE pb.positionId = ?";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, positionId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    BenefitDetail detail = new BenefitDetail();
                    detail.setBenefitTypeId(rs.getInt("benefitTypeId"));
                    detail.setPositionId(rs.getInt("positionId"));
                    detail.setBenefitValue(rs.getBigDecimal("benefitValue"));
                    detail.setBenefitName(rs.getString("benefitName"));
                    detail.setBenefitDescription(rs.getString("benefitDescription"));
                    details.add(detail);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting benefit details for position " + positionId + ": " + e.getMessage());
        }
        
        return details;
    }
    
    /**
     * Save or update a position benefit
     */
    public boolean save(PositionBenefit positionBenefit) {
        if (!positionBenefit.isValid()) {
            System.err.println("Cannot save position benefit: Invalid data");
            return false;
        }
        
        // Check if record exists
        if (exists(positionBenefit.getBenefitTypeId(), positionBenefit.getPositionId())) {
            return update(positionBenefit);
        } else {
            return insert(positionBenefit);
        }
    }
    
    /**
     * Insert new position benefit
     */
    private boolean insert(PositionBenefit positionBenefit) {
        String sql = "INSERT INTO positionbenefit (benefitTypeId, positionId, benefitValue) VALUES (?, ?, ?)";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, positionBenefit.getBenefitTypeId());
            pstmt.setInt(2, positionBenefit.getPositionId());
            pstmt.setBigDecimal(3, positionBenefit.getBenefitValue());
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error inserting position benefit: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Update existing position benefit
     */
    private boolean update(PositionBenefit positionBenefit) {
        String sql = "UPDATE positionbenefit SET benefitValue = ? WHERE benefitTypeId = ? AND positionId = ?";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setBigDecimal(1, positionBenefit.getBenefitValue());
            pstmt.setInt(2, positionBenefit.getBenefitTypeId());
            pstmt.setInt(3, positionBenefit.getPositionId());
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating position benefit: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if position benefit exists
     */
    public boolean exists(Integer benefitTypeId, Integer positionId) {
        String sql = "SELECT COUNT(*) FROM positionbenefit WHERE benefitTypeId = ? AND positionId = ?";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, benefitTypeId);
            pstmt.setInt(2, positionId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error checking position benefit existence: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Delete position benefit
     */
    public boolean delete(Integer benefitTypeId, Integer positionId) {
        String sql = "DELETE FROM positionbenefit WHERE benefitTypeId = ? AND positionId = ?";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, benefitTypeId);
            pstmt.setInt(2, positionId);
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting position benefit: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get all position benefits
     */
    public List<PositionBenefit> findAll() {
        List<PositionBenefit> benefits = new ArrayList<>();
        String sql = "SELECT benefitTypeId, positionId, benefitValue FROM positionbenefit ORDER BY positionId, benefitTypeId";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                PositionBenefit benefit = new PositionBenefit();
                benefit.setBenefitTypeId(rs.getInt("benefitTypeId"));
                benefit.setPositionId(rs.getInt("positionId"));
                benefit.setBenefitValue(rs.getBigDecimal("benefitValue"));
                benefits.add(benefit);
            }
        } catch (SQLException e) {
            System.err.println("Error getting all position benefits: " + e.getMessage());
        }
        
        return benefits;
    }
    
    /**
     * Inner class for benefit details with names
     */
    public static class BenefitDetail extends PositionBenefit {
        private String benefitName;
        private String benefitDescription;
        
        public String getBenefitName() { return benefitName; }
        public void setBenefitName(String benefitName) { this.benefitName = benefitName; }
        
        public String getBenefitDescription() { return benefitDescription; }
        public void setBenefitDescription(String benefitDescription) { this.benefitDescription = benefitDescription; }
        
        @Override
        public String toString() {
            return "BenefitDetail{" +
                    "benefitName='" + benefitName + '\'' +
                    ", benefitValue=" + getBenefitValue() +
                    ", positionId=" + getPositionId() +
                    '}';
        }
    }
}