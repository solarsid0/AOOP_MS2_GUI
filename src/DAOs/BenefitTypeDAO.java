
package DAOs;

import Models.BenefitTypeModel;
import Models.BenefitTypeModel.BenefitName;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

/**
 * BenefitTypeDAO - Data Access Object for BenefitTypeModel
 * Handles all database operations for benefit types
 * @author User
 */
public class BenefitTypeDAO {
    
    public BenefitTypeDAO() {
        
    }
   
    
    /**
     * Create - Insert new benefit type
     * @param benefitType
     * @return 
     */
    public boolean save(BenefitTypeModel benefitType) {
        String sql = "INSERT INTO benefittype (benefitName, benefitDescription) VALUES (?, ?)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, benefitType.getBenefitName().getDisplayName());
            pstmt.setString(2, benefitType.getBenefitDescription());
            
            int affectedRows = pstmt.executeUpdate();
            
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        benefitType.setBenefitTypeId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error saving benefit type: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Read - Find benefit type by ID
     * @param benefitTypeId
     * @return 
     */
    public BenefitTypeModel findById(int benefitTypeId) {
        String sql = "SELECT * FROM benefittype WHERE benefitTypeId = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, benefitTypeId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return mapResultSetToBenefitType(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error finding benefit type: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Read - Find benefit type by name
     * @param benefitName
     * @return 
     */
    public BenefitTypeModel findByName(BenefitName benefitName) {
        String sql = "SELECT * FROM benefittype WHERE benefitName = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, benefitName.getDisplayName());
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return mapResultSetToBenefitType(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error finding benefit type by name: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Read - Get all benefit types
     * @return 
     */
    public List<BenefitTypeModel> findAll() {
        List<BenefitTypeModel> benefitTypes = new ArrayList<>();
        String sql = "SELECT * FROM benefittype ORDER BY benefitName";
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                benefitTypes.add(mapResultSetToBenefitType(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving benefit types: " + e.getMessage());
        }
        return benefitTypes;
    }
    
    /**
     * Update - Update existing benefit type
     * @param benefitType
     * @return 
     */
    public boolean update(BenefitTypeModel benefitType) {
        if (benefitType.getBenefitTypeId() == null || benefitType.getBenefitTypeId() <= 0) {
            System.err.println("Cannot update benefit type: Invalid benefit type ID");
            return false;
        }
        
        String sql = "UPDATE benefittype SET benefitName = ?, benefitDescription = ? WHERE benefitTypeId = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, benefitType.getBenefitName().getDisplayName());
            pstmt.setString(2, benefitType.getBenefitDescription());
            pstmt.setInt(3, benefitType.getBenefitTypeId());
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating benefit type: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Delete - Remove benefit type
     * @param benefitType
     * @return 
     */
    public boolean delete(BenefitTypeModel benefitType) {
        return deleteById(benefitType.getBenefitTypeId());
    }
    
    /**
     * Delete - Remove benefit type by ID
     * @param benefitTypeId
     * @return 
     */
    public boolean deleteById(int benefitTypeId) {
        // Check if being used first
        if (isBeingUsed(benefitTypeId)) {
            System.err.println("Cannot delete benefit type: It is being used in position benefits");
            return false;
        }
        
        String sql = "DELETE FROM benefittype WHERE benefitTypeId = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, benefitTypeId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting benefit type: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Get benefit amount for a specific position
     * @param benefitTypeId
     * @param positionId
     * @return 
     */
    public BigDecimal getBenefitAmountForPosition(int benefitTypeId, int positionId) {
        String sql = "SELECT benefitValue FROM positionbenefit WHERE benefitTypeId = ? AND positionId = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, benefitTypeId);
            pstmt.setInt(2, positionId);
            
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getBigDecimal("benefitValue");
            }
        } catch (SQLException e) {
            System.err.println("Error getting benefit amount for position: " + e.getMessage());
        }
        return BigDecimal.ZERO;
    }
    
    /**
     * Set benefit amount for a specific position
     * @param benefitTypeId
     * @param positionId
     * @param amount
     * @return 
     */
    public boolean setBenefitAmountForPosition(int benefitTypeId, int positionId, BigDecimal amount) {
        // First check if the position-benefit relationship exists
        String checkSql = "SELECT COUNT(*) FROM positionbenefit WHERE benefitTypeId = ? AND positionId = ?";
        String insertSql = "INSERT INTO positionbenefit (benefitTypeId, positionId, benefitValue) VALUES (?, ?, ?)";
        String updateSql = "UPDATE positionbenefit SET benefitValue = ? WHERE benefitTypeId = ? AND positionId = ?";
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Check if relationship exists
            boolean exists = false;
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setInt(1, benefitTypeId);
                checkStmt.setInt(2, positionId);
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next()) {
                    exists = rs.getInt(1) > 0;
                }
            }
            
            // Insert or update based on existence
            if (exists) {
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setBigDecimal(1, amount);
                    updateStmt.setInt(2, benefitTypeId);
                    updateStmt.setInt(3, positionId);
                    return updateStmt.executeUpdate() > 0;
                }
            } else {
                try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                    insertStmt.setInt(1, benefitTypeId);
                    insertStmt.setInt(2, positionId);
                    insertStmt.setBigDecimal(3, amount);
                    return insertStmt.executeUpdate() > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error setting benefit amount for position: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Get all positions that have this benefit type
     * @param benefitTypeId
     * @return 
     */
    public List<Integer> getPositionsWithBenefit(int benefitTypeId) {
        List<Integer> positions = new ArrayList<>();
        String sql = "SELECT positionId FROM positionbenefit WHERE benefitTypeId = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, benefitTypeId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                positions.add(rs.getInt("positionId"));
            }
        } catch (SQLException e) {
            System.err.println("Error getting positions with benefit: " + e.getMessage());
        }
        return positions;
    }
    
    /**
     * Remove benefit from all positions
     * @param benefitTypeId
     * @return 
     */
    public boolean removeFromAllPositions(int benefitTypeId) {
        String sql = "DELETE FROM positionbenefit WHERE benefitTypeId = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, benefitTypeId);
            return pstmt.executeUpdate() >= 0; // Returns true even if 0 rows affected
        } catch (SQLException e) {
            System.err.println("Error removing benefit from positions: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Check if benefit type is applicable to a specific position
     * @param benefitTypeId
     * @param positionId
     * @return 
     */
    public boolean isApplicableToPosition(int benefitTypeId, int positionId) {
        String sql = "SELECT COUNT(*) FROM positionbenefit WHERE benefitTypeId = ? AND positionId = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, benefitTypeId);
            pstmt.setInt(2, positionId);
            
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error checking benefit applicability: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Check if benefit type is being used
     * @param benefitTypeId
     * @return 
     */
    public boolean isBeingUsed(int benefitTypeId) {
        String sql = "SELECT COUNT(*) FROM positionbenefit WHERE benefitTypeId = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, benefitTypeId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error checking benefit type usage: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Get benefit types for a specific position
     * @param positionId
     * @return 
     */
    public List<Map<String, Object>> getBenefitsForPosition(int positionId) {
        List<Map<String, Object>> benefits = new ArrayList<>();
        String sql = "SELECT bt.benefitTypeId, bt.benefitName, bt.benefitDescription, pb.benefitValue " +
                    "FROM benefittype bt " +
                    "JOIN positionbenefit pb ON bt.benefitTypeId = pb.benefitTypeId " +
                    "WHERE pb.positionId = ? " +
                    "ORDER BY bt.benefitName";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, positionId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Map<String, Object> benefit = new HashMap<>();
                benefit.put("benefitTypeId", rs.getInt("benefitTypeId"));
                benefit.put("benefitName", rs.getString("benefitName"));
                benefit.put("benefitDescription", rs.getString("benefitDescription"));
                benefit.put("benefitValue", rs.getBigDecimal("benefitValue"));
                benefits.add(benefit);
            }
        } catch (SQLException e) {
            System.err.println("Error getting benefits for position: " + e.getMessage());
        }
        return benefits;
    }
    
    /**
     * Check if benefit type exists
     * @param benefitTypeId
     * @return 
     */
    public boolean exists(int benefitTypeId) {
        String sql = "SELECT COUNT(*) FROM benefittype WHERE benefitTypeId = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, benefitTypeId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error checking benefit type existence: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Helper method to map ResultSet to BenefitTypeModel
     */
    private BenefitTypeModel mapResultSetToBenefitType(ResultSet rs) throws SQLException {
        return new BenefitTypeModel(
            rs.getInt("benefitTypeId"),
            BenefitName.fromString(rs.getString("benefitName")),
            rs.getString("benefitDescription"),
            null // Amount is handled separately through positionbenefit table
        );
    }
}