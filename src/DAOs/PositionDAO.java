package DAOs;

import Models.PositionModel;
import java.sql.*;
import java.util.List;
import java.util.ArrayList;

/**
 * PositionDAO - Minimal implementation for rank-and-file detection
 * @author Chad
 */
public class PositionDAO {
    
    private final DatabaseConnection databaseConnection;
    
    public PositionDAO(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }
    
    /**
     * Find position by ID - CRITICAL for rank-and-file detection
     * @param positionId
     * @return PositionModel or null
     */
    public PositionModel findById(Integer positionId) {
        String sql = "SELECT positionId, position, positionDescription, department FROM position WHERE positionId = ?";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, positionId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                PositionModel position = new PositionModel();
                position.setPositionId(rs.getInt("positionId"));
                position.setPosition(rs.getString("position"));
                position.setPositionDescription(rs.getString("positionDescription"));
                position.setDepartment(rs.getString("department"));
                return position;
            }
        } catch (SQLException e) {
            System.err.println("Error finding position: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Get all positions
     * @return List of positions
     */
    public List<PositionModel> findAll() {
        List<PositionModel> positions = new ArrayList<>();
        String sql = "SELECT positionId, position, positionDescription, department FROM position ORDER BY department, position";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                PositionModel position = new PositionModel();
                position.setPositionId(rs.getInt("positionId"));
                position.setPosition(rs.getString("position"));
                position.setPositionDescription(rs.getString("positionDescription"));
                position.setDepartment(rs.getString("department"));
                positions.add(position);
            }
        } catch (SQLException e) {
            System.err.println("Error finding all positions: " + e.getMessage());
        }
        return positions;
    }
    
    /**
     * Get rank-and-file positions only
     * @return List of rank-and-file positions
     */
    public List<PositionModel> getRankAndFilePositions() {
        List<PositionModel> positions = new ArrayList<>();
        String sql = "SELECT positionId, position, positionDescription, department FROM position " +
                    "WHERE LOWER(department) = 'rank-and-file' OR LOWER(position) LIKE '%rank%file%' " +
                    "ORDER BY position";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                PositionModel position = new PositionModel();
                position.setPositionId(rs.getInt("positionId"));
                position.setPosition(rs.getString("position"));
                position.setPositionDescription(rs.getString("positionDescription"));
                position.setDepartment(rs.getString("department"));
                positions.add(position);
            }
        } catch (SQLException e) {
            System.err.println("Error finding rank-and-file positions: " + e.getMessage());
        }
        return positions;
    }
}