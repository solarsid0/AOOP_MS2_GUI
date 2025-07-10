package DAOs;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Access Object for reference data operations
 * Handles positions, benefit types, leave types, and other reference data
 * @author USER
 */
public class ReferenceDataDAO {
    
    private final DatabaseConnection databaseConnection;
    
    public ReferenceDataDAO(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }
    
    /**
     * Validates if a position ID exists in the database
     * @param positionId Position ID to validate
     * @return true if position exists, false otherwise
     */
    public boolean isValidPositionId(Integer positionId) {
        if (positionId == null) return false;
        
        String sql = "SELECT COUNT(*) FROM position WHERE positionId = ?";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, positionId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            
        } catch (SQLException e) {
            System.err.println("Error validating position ID: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Validates if a benefit type ID exists in the database
     * @param benefitTypeId Benefit type ID to validate
     * @return true if benefit type exists, false otherwise
     */
    public boolean isValidBenefitTypeId(Integer benefitTypeId) {
        if (benefitTypeId == null) return false;
        
        String sql = "SELECT COUNT(*) FROM benefittype WHERE benefitTypeId = ?";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, benefitTypeId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            
        } catch (SQLException e) {
            System.err.println("Error validating benefit type ID: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Gets all positions in a specific department
     * @param department Department name
     * @return List of positions in the department
     */
    public List<Map<String, Object>> getPositionsByDepartment(String department) {
        List<Map<String, Object>> positions = new ArrayList<>();
        String sql = """
            SELECT positionId, positionTitle, department, description
            FROM position 
            WHERE department = ? 
            ORDER BY positionTitle
            """;
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, department);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Map<String, Object> position = new HashMap<>();
                position.put("positionId", rs.getInt("positionId"));
                position.put("positionTitle", rs.getString("positionTitle"));
                position.put("department", rs.getString("department"));
                position.put("description", rs.getString("description"));
                positions.add(position);
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting positions by department: " + e.getMessage());
        }
        
        return positions;
    }
    
    /**
     * Gets all departments
     * @return List of unique department names
     */
    public List<String> getAllDepartments() {
        List<String> departments = new ArrayList<>();
        String sql = "SELECT DISTINCT department FROM position ORDER BY department";
        
        try (Connection conn = databaseConnection.createConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                String department = rs.getString("department");
                if (department != null && !department.trim().isEmpty()) {
                    departments.add(department);
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting all departments: " + e.getMessage());
        }
        
        return departments;
    }
    
    /**
     * Gets all positions
     * @return List of all positions
     */
    public List<Map<String, Object>> getAllPositions() {
        List<Map<String, Object>> positions = new ArrayList<>();
        String sql = """
            SELECT positionId, positionTitle, department, description
            FROM position 
            ORDER BY department, positionTitle
            """;
        
        try (Connection conn = databaseConnection.createConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                Map<String, Object> position = new HashMap<>();
                position.put("positionId", rs.getInt("positionId"));
                position.put("positionTitle", rs.getString("positionTitle"));
                position.put("department", rs.getString("department"));
                position.put("description", rs.getString("description"));
                positions.add(position);
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting all positions: " + e.getMessage());
        }
        
        return positions;
    }
    
    /**
     * Gets all benefit types
     * @return List of all benefit types
     */
    public List<Map<String, Object>> getAllBenefitTypes() {
        List<Map<String, Object>> benefitTypes = new ArrayList<>();
        String sql = """
            SELECT benefitTypeId, benefitName, description, isActive
            FROM benefittype 
            WHERE isActive = true
            ORDER BY benefitName
            """;
        
        try (Connection conn = databaseConnection.createConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                Map<String, Object> benefitType = new HashMap<>();
                benefitType.put("benefitTypeId", rs.getInt("benefitTypeId"));
                benefitType.put("benefitName", rs.getString("benefitName"));
                benefitType.put("description", rs.getString("description"));
                benefitType.put("isActive", rs.getBoolean("isActive"));
                benefitTypes.add(benefitType);
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting all benefit types: " + e.getMessage());
        }
        
        return benefitTypes;
    }
    
    /**
     * Gets all leave types
     * @return List of all leave types
     */
    public List<Map<String, Object>> getAllLeaveTypes() {
        List<Map<String, Object>> leaveTypes = new ArrayList<>();
        String sql = """
            SELECT leaveTypeId, leaveTypeName, description, maxDays, isActive
            FROM leavetype 
            WHERE isActive = true
            ORDER BY leaveTypeName
            """;
        
        try (Connection conn = databaseConnection.createConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                Map<String, Object> leaveType = new HashMap<>();
                leaveType.put("leaveTypeId", rs.getInt("leaveTypeId"));
                leaveType.put("leaveTypeName", rs.getString("leaveTypeName"));
                leaveType.put("description", rs.getString("description"));
                leaveType.put("maxDays", rs.getInt("maxDays"));
                leaveType.put("isActive", rs.getBoolean("isActive"));
                leaveTypes.add(leaveType);
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting all leave types: " + e.getMessage());
        }
        
        return leaveTypes;
    }
    
    /**
     * Gets position details by ID
     * @param positionId Position ID
     * @return Position details or null if not found
     */
    public Map<String, Object> getPositionById(Integer positionId) {
        String sql = """
            SELECT positionId, positionTitle, department, description
            FROM position 
            WHERE positionId = ?
            """;
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, positionId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                Map<String, Object> position = new HashMap<>();
                position.put("positionId", rs.getInt("positionId"));
                position.put("positionTitle", rs.getString("positionTitle"));
                position.put("department", rs.getString("department"));
                position.put("description", rs.getString("description"));
                return position;
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting position by ID: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Gets benefit type details by ID
     * @param benefitTypeId Benefit type ID
     * @return Benefit type details or null if not found
     */
    public Map<String, Object> getBenefitTypeById(Integer benefitTypeId) {
        String sql = """
            SELECT benefitTypeId, benefitName, description, isActive
            FROM benefittype 
            WHERE benefitTypeId = ?
            """;
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, benefitTypeId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                Map<String, Object> benefitType = new HashMap<>();
                benefitType.put("benefitTypeId", rs.getInt("benefitTypeId"));
                benefitType.put("benefitName", rs.getString("benefitName"));
                benefitType.put("description", rs.getString("description"));
                benefitType.put("isActive", rs.getBoolean("isActive"));
                return benefitType;
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting benefit type by ID: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Gets leave type details by ID (ADDED TO FIX JasperReportGenerator)
     * @param leaveTypeId Leave type ID
     * @return Leave type details or null if not found
     */
    public Map<String, Object> getLeaveTypeById(Integer leaveTypeId) {
        if (leaveTypeId == null) return null;
        
        String sql = """
            SELECT leaveTypeId, leaveTypeName, description, maxDays, isActive
            FROM leavetype 
            WHERE leaveTypeId = ?
            """;
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, leaveTypeId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                Map<String, Object> leaveType = new HashMap<>();
                leaveType.put("leaveTypeId", rs.getInt("leaveTypeId"));
                leaveType.put("leaveTypeName", rs.getString("leaveTypeName"));
                leaveType.put("description", rs.getString("description"));
                leaveType.put("maxDays", rs.getInt("maxDays"));
                leaveType.put("isActive", rs.getBoolean("isActive"));
                return leaveType;
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting leave type by ID: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Validates if a leave type ID exists in the database
     * @param leaveTypeId Leave type ID to validate
     * @return true if leave type exists, false otherwise
     */
    public boolean isValidLeaveTypeId(Integer leaveTypeId) {
        if (leaveTypeId == null) return false;
        
        String sql = "SELECT COUNT(*) FROM leavetype WHERE leaveTypeId = ? AND isActive = true";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, leaveTypeId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            
        } catch (SQLException e) {
            System.err.println("Error validating leave type ID: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Creates a new benefit type
     * @param benefitName Benefit name
     * @param description Benefit description
     * @return true if successful, false otherwise
     */
    public boolean createBenefitType(String benefitName, String description) {
        String sql = """
            INSERT INTO benefittype (benefitName, description, isActive, createdDate)
            VALUES (?, ?, true, CURRENT_TIMESTAMP)
            """;
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, benefitName);
            pstmt.setString(2, description);
            
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Error creating benefit type: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Creates a new leave type
     * @param leaveTypeName Leave type name
     * @param description Leave type description
     * @param maxDays Maximum days allowed for this leave type
     * @return true if successful, false otherwise
     */
    public boolean createLeaveType(String leaveTypeName, String description, Integer maxDays) {
        String sql = """
            INSERT INTO leavetype (leaveTypeName, description, maxDays, isActive, createdDate)
            VALUES (?, ?, ?, true, CURRENT_TIMESTAMP)
            """;
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, leaveTypeName);
            pstmt.setString(2, description);
            if (maxDays != null) {
                pstmt.setInt(3, maxDays);
            } else {
                pstmt.setNull(3, Types.INTEGER);
            }
            
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Error creating leave type: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Creates a new position
     * @param positionTitle Position title
     * @param department Department name
     * @param description Position description
     * @return true if successful, false otherwise
     */
    public boolean createPosition(String positionTitle, String department, String description) {
        String sql = """
            INSERT INTO position (positionTitle, department, description, createdDate)
            VALUES (?, ?, ?, CURRENT_TIMESTAMP)
            """;
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, positionTitle);
            pstmt.setString(2, department);
            pstmt.setString(3, description);
            
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Error creating position: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Updates a position
     * @param positionId Position ID
     * @param positionTitle New position title
     * @param department New department
     * @param description New description
     * @return true if successful, false otherwise
     */
    public boolean updatePosition(Integer positionId, String positionTitle, String department, String description) {
        String sql = """
            UPDATE position 
            SET positionTitle = ?, department = ?, description = ?, updatedDate = CURRENT_TIMESTAMP
            WHERE positionId = ?
            """;
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, positionTitle);
            pstmt.setString(2, department);
            pstmt.setString(3, description);
            pstmt.setInt(4, positionId);
            
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Error updating position: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Updates a benefit type
     * @param benefitTypeId Benefit type ID
     * @param benefitName New benefit name
     * @param description New description
     * @return true if successful, false otherwise
     */
    public boolean updateBenefitType(Integer benefitTypeId, String benefitName, String description) {
        String sql = """
            UPDATE benefittype 
            SET benefitName = ?, description = ?, updatedDate = CURRENT_TIMESTAMP
            WHERE benefitTypeId = ?
            """;
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, benefitName);
            pstmt.setString(2, description);
            pstmt.setInt(3, benefitTypeId);
            
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Error updating benefit type: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Updates a leave type
     * @param leaveTypeId Leave type ID
     * @param leaveTypeName New leave type name
     * @param description New description
     * @param maxDays New maximum days
     * @return true if successful, false otherwise
     */
    public boolean updateLeaveType(Integer leaveTypeId, String leaveTypeName, String description, Integer maxDays) {
        String sql = """
            UPDATE leavetype 
            SET leaveTypeName = ?, description = ?, maxDays = ?, updatedDate = CURRENT_TIMESTAMP
            WHERE leaveTypeId = ?
            """;
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, leaveTypeName);
            pstmt.setString(2, description);
            if (maxDays != null) {
                pstmt.setInt(3, maxDays);
            } else {
                pstmt.setNull(3, Types.INTEGER);
            }
            pstmt.setInt(4, leaveTypeId);
            
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Error updating leave type: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Deactivates a benefit type
     * @param benefitTypeId Benefit type ID
     * @return true if successful, false otherwise
     */
    public boolean deactivateBenefitType(Integer benefitTypeId) {
        String sql = """
            UPDATE benefittype 
            SET isActive = false, updatedDate = CURRENT_TIMESTAMP
            WHERE benefitTypeId = ?
            """;
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, benefitTypeId);
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Error deactivating benefit type: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Deactivates a leave type
     * @param leaveTypeId Leave type ID
     * @return true if successful, false otherwise
     */
    public boolean deactivateLeaveType(Integer leaveTypeId) {
        String sql = """
            UPDATE leavetype 
            SET isActive = false, updatedDate = CURRENT_TIMESTAMP
            WHERE leaveTypeId = ?
            """;
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, leaveTypeId);
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Error deactivating leave type: " + e.getMessage());
        }
        
        return false;
    }
}