package DAOs;

import Models.LeaveTypeModel;
import java.sql.*;
import java.time.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

/**
 * This class manages leave type records.
 *
 * Provides CRUD operations and validation logic for leave types,
 * such as vacation, sick, and emergency leave. 
 *
 * It supports filtering by active status, handles Manila timezone, 
 * and integrates with leave requests and balances.
 */

public class LeaveTypeDAO {

    
    // SQL Queries
    private static final String INSERT_LEAVE_TYPE = 
        "INSERT INTO leavetype (leaveTypeName, leaveDescription, maxDaysPerYear, createdAt) " +
        "VALUES (?, ?, ?, ?)";
    
    private static final String UPDATE_LEAVE_TYPE = 
        "UPDATE leavetype SET leaveTypeName = ?, leaveDescription = ?, maxDaysPerYear = ? " +
        "WHERE leaveTypeId = ?";
    
    private static final String SELECT_BY_ID = 
        "SELECT leaveTypeId, leaveTypeName, leaveDescription, maxDaysPerYear, createdAt " +
        "FROM leavetype WHERE leaveTypeId = ?";
    
    private static final String SELECT_BY_NAME = 
        "SELECT leaveTypeId, leaveTypeName, leaveDescription, maxDaysPerYear, createdAt " +
        "FROM leavetype WHERE leaveTypeName = ?";
    
    private static final String SELECT_ALL = 
        "SELECT leaveTypeId, leaveTypeName, leaveDescription, maxDaysPerYear, createdAt " +
        "FROM leavetype ORDER BY leaveTypeName";
        
    private static final String DELETE_LEAVE_TYPE = 
        "DELETE FROM leavetype WHERE leaveTypeId = ?";
    
    private static final String CHECK_LEAVE_TYPE_USAGE = 
        "SELECT COUNT(*) as usageCount FROM leaverequest WHERE leaveTypeId = ?";
    
    private static final String CHECK_BALANCE_USAGE = 
        "SELECT COUNT(*) as balanceCount FROM leavebalance WHERE leaveTypeId = ?";
    
    private static final String SELECT_LEAVE_TYPE_STATISTICS = 
        "SELECT " +
        "lt.leaveTypeId, " +
        "lt.leaveTypeName, " +
        "COUNT(lr.leaveRequestId) as totalRequests, " +
        "SUM(CASE WHEN lr.approvalStatus = 'Approved' THEN 1 ELSE 0 END) as approvedRequests, " +
        "AVG(CASE WHEN lr.approvalStatus = 'Approved' THEN DATEDIFF(lr.leaveEnd, lr.leaveStart) + 1 ELSE NULL END) as avgLeaveDays " +
        "FROM leavetype lt " +
        "LEFT JOIN leaverequest lr ON lt.leaveTypeId = lr.leaveTypeId " +
        "WHERE YEAR(lr.dateCreated) = ? OR lr.dateCreated IS NULL " +
        "GROUP BY lt.leaveTypeId, lt.leaveTypeName " +
        "ORDER BY totalRequests DESC";
    
    /**
     * Get database connection using centralized DatabaseConnection
     * (Timezone already handled in DatabaseConnection.getConnection())
     */
    private Connection getConnection() throws SQLException {
        return DatabaseConnection.getConnection();
    }
    
    /**
     * Create new leave type with validation
     * @param leaveType
     * @return 
     */
    public boolean createLeaveType(LeaveTypeModel leaveType) {
        if (leaveType == null || !leaveType.isValid()) {
            System.err.println("Invalid leave type provided");
            return false;
        }
        
        // Check if leave type name already exists
        if (isLeaveTypeNameExists(leaveType.getLeaveTypeName())) {
            System.err.println("Leave type name already exists: " + leaveType.getLeaveTypeName());
            return false;
        }
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(INSERT_LEAVE_TYPE, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, leaveType.getLeaveTypeName());
            stmt.setString(2, leaveType.getLeaveDescription());
            
            // Handle null maxDaysPerYear
            if (leaveType.getMaxDaysPerYear() != null) {
                stmt.setInt(3, leaveType.getMaxDaysPerYear());
            } else {
                stmt.setNull(3, Types.INTEGER);
            }
            
            // Use Manila timezone for createdAt
            stmt.setTimestamp(4, Timestamp.valueOf(LeaveTypeModel.getCurrentDateTimeManila()));
            
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        leaveType.setLeaveTypeId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
            
        } catch (SQLException e) {
            System.err.println("Error creating leave type: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Update leave type with validation
     * @param leaveType
     * @return 
     */
    public boolean updateLeaveType(LeaveTypeModel leaveType) {
        if (leaveType == null || leaveType.getLeaveTypeId() == null || 
            leaveType.getLeaveTypeId() <= 0 || !leaveType.isValid()) {
            System.err.println("Invalid leave type provided for update");
            return false;
        }
        
        // Check if new name conflicts with existing leave types (excluding current one)
        LeaveTypeModel existing = getLeaveTypeByName(leaveType.getLeaveTypeName());
        if (existing != null && !existing.getLeaveTypeId().equals(leaveType.getLeaveTypeId())) {
            System.err.println("Leave type name already exists: " + leaveType.getLeaveTypeName());
            return false;
        }
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(UPDATE_LEAVE_TYPE)) {
            
            stmt.setString(1, leaveType.getLeaveTypeName());
            stmt.setString(2, leaveType.getLeaveDescription());
            
            // Handle null maxDaysPerYear
            if (leaveType.getMaxDaysPerYear() != null) {
                stmt.setInt(3, leaveType.getMaxDaysPerYear());
            } else {
                stmt.setNull(3, Types.INTEGER);
            }
            
            stmt.setInt(4, leaveType.getLeaveTypeId());
            
            return stmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Error updating leave type: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Get leave type by ID
     * @param leaveTypeId
     * @return 
     */
    public LeaveTypeModel getLeaveTypeById(Integer leaveTypeId) {
        if (leaveTypeId == null || leaveTypeId <= 0) {
            return null;
        }
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_ID)) {
            
            stmt.setInt(1, leaveTypeId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToLeaveType(rs);
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting leave type by ID: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Get leave type by name
     * @param leaveTypeName
     * @return 
     */
    public LeaveTypeModel getLeaveTypeByName(String leaveTypeName) {
        if (leaveTypeName == null || leaveTypeName.trim().isEmpty()) {
            return null;
        }
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_NAME)) {
            
            stmt.setString(1, leaveTypeName.trim());
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToLeaveType(rs);
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting leave type by name: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Get all leave types
     * @return 
     */
    public List<LeaveTypeModel> getAllLeaveTypes() {
        List<LeaveTypeModel> leaveTypes = new ArrayList<>();
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_ALL)) {
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    leaveTypes.add(mapResultSetToLeaveType(rs));
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting all leave types: " + e.getMessage());
        }
        
        return leaveTypes;
    }
    
    /**
     * Get active leave types only (maxDaysPerYear > 0 OR unlimited)
     * @return 
     */
    public List<LeaveTypeModel> getActiveLeaveTypes() {
        List<LeaveTypeModel> leaveTypes = new ArrayList<>();
        
        // Modified query to include unlimited leave types (NULL maxDaysPerYear)
        String query = "SELECT leaveTypeId, leaveTypeName, leaveDescription, maxDaysPerYear, createdAt " +
                      "FROM leavetype WHERE maxDaysPerYear > 0 OR maxDaysPerYear IS NULL ORDER BY leaveTypeName";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    leaveTypes.add(mapResultSetToLeaveType(rs));
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting active leave types: " + e.getMessage());
        }
        
        return leaveTypes;
    }
    
    /**
     * Delete leave type with validation
     * @param leaveTypeId
     * @return 
     */
    public boolean deleteLeaveType(Integer leaveTypeId) {
        if (leaveTypeId == null || leaveTypeId <= 0) {
            return false;
        }
        
        // Check if leave type is being used
        if (isLeaveTypeInUse(leaveTypeId)) {
            System.err.println("Cannot delete leave type: it is currently in use");
            return false;
        }
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(DELETE_LEAVE_TYPE)) {
            
            stmt.setInt(1, leaveTypeId);
            return stmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Error deleting leave type: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Check if leave type name already exists
     * @param leaveTypeName
     * @return 
     */
    public boolean isLeaveTypeNameExists(String leaveTypeName) {
        return getLeaveTypeByName(leaveTypeName) != null;
    }
    
    /**
     * Check if leave type is currently in use
     * @param leaveTypeId
     * @return 
     */
    public boolean isLeaveTypeInUse(Integer leaveTypeId) {
        if (leaveTypeId == null || leaveTypeId <= 0) {
            return false;
        }
        
        try (Connection conn = getConnection()) {
            
            // Check leave requests
            try (PreparedStatement stmt = conn.prepareStatement(CHECK_LEAVE_TYPE_USAGE)) {
                stmt.setInt(1, leaveTypeId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next() && rs.getInt("usageCount") > 0) {
                        return true;
                    }
                }
            }
            
            // Check leave balances
            try (PreparedStatement stmt = conn.prepareStatement(CHECK_BALANCE_USAGE)) {
                stmt.setInt(1, leaveTypeId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next() && rs.getInt("balanceCount") > 0) {
                        return true;
                    }
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error checking leave type usage: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Initialize standard leave types
     * @return 
     */
    public boolean initializeStandardLeaveTypes() {
        List<LeaveTypeModel> standardTypes = new ArrayList<>();
        
        // Standard Philippine leave types
        standardTypes.add(new LeaveTypeModel("Vacation Leave", "Annual vacation leave for rest and recreation", 15));
        standardTypes.add(new LeaveTypeModel("Sick Leave", "Medical leave for illness or health issues", 15));
        standardTypes.add(new LeaveTypeModel("Emergency Leave", "Leave for emergency situations", 5));
        standardTypes.add(new LeaveTypeModel("Maternity Leave", "Leave for childbirth and maternity care", 105)); // 15 weeks
        standardTypes.add(new LeaveTypeModel("Paternity Leave", "Leave for fathers during childbirth", 7));
        standardTypes.add(new LeaveTypeModel("Bereavement Leave", "Leave for death in immediate family", 5));
        standardTypes.add(new LeaveTypeModel("Study Leave", "Leave for educational purposes", 10));
        
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            
            try {
                int successCount = 0;
                
                for (LeaveTypeModel leaveType : standardTypes) {
                    if (!isLeaveTypeNameExists(leaveType.getLeaveTypeName())) {
                        if (createLeaveType(leaveType)) {
                            successCount++;
                        }
                    } else {
                        successCount++; // Already exists, count as success
                    }
                }
                
                if (successCount == standardTypes.size()) {
                    conn.commit();
                    System.out.println("Successfully initialized " + successCount + " leave types");
                    return true;
                } else {
                    conn.rollback();
                    System.err.println("Failed to initialize all leave types");
                    return false;
                }
                
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
            
        } catch (SQLException e) {
            System.err.println("Error initializing standard leave types: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Get leave type options for dropdown/selection
     * @return 
     */
    public Map<Integer, String> getLeaveTypeOptions() {
        Map<Integer, String> options = new HashMap<>();
        
        List<LeaveTypeModel> activeTypes = getActiveLeaveTypes();
        for (LeaveTypeModel leaveType : activeTypes) {
            options.put(leaveType.getLeaveTypeId(), leaveType.getDisplayNameWithLimits());
        }
        
        return options;
    }
    
    /**
     * Validate leave type configuration
     * @return 
     */
    public List<String> validateLeaveTypeConfiguration() {
        List<String> validationErrors = new ArrayList<>();
        
        try {
            List<LeaveTypeModel> allTypes = getAllLeaveTypes();
            
            if (allTypes.isEmpty()) {
                validationErrors.add("No leave types configured");
                return validationErrors;
            }
            
            // Check for required leave types
            boolean hasVacation = false;
            boolean hasSick = false;
            
            for (LeaveTypeModel type : allTypes) {
                String lowerName = type.getLeaveTypeName().toLowerCase();
                if (lowerName.contains("vacation")) {
                    hasVacation = true;
                }
                if (lowerName.contains("sick")) {
                    hasSick = true;
                }
                
                // Validate individual leave type using Model validation
                if (!type.isValid()) {
                    validationErrors.add("Invalid leave type configuration: " + type.getLeaveTypeName());
                }
            }
            
            if (!hasVacation) {
                validationErrors.add("No vacation leave type configured");
            }
            if (!hasSick) {
                validationErrors.add("No sick leave type configured");
            }
            
        } catch (Exception e) {
            validationErrors.add("Error validating leave type configuration: " + e.getMessage());
        }
        
        return validationErrors;
    }
    
    // Helper methods
    
    /**
     * Map ResultSet to LeaveTypeModel
     */
    private LeaveTypeModel mapResultSetToLeaveType(ResultSet rs) throws SQLException {
        LeaveTypeModel leaveType = new LeaveTypeModel();
        
        leaveType.setLeaveTypeId(rs.getInt("leaveTypeId"));
        leaveType.setLeaveTypeName(rs.getString("leaveTypeName"));
        leaveType.setLeaveDescription(rs.getString("leaveDescription"));
        
        // Handle null maxDaysPerYear properly
        Integer maxDays = rs.getObject("maxDaysPerYear", Integer.class);
        leaveType.setMaxDaysPerYear(maxDays);
        
        // Convert Timestamp to LocalDateTime
        Timestamp createdAtTimestamp = rs.getTimestamp("createdAt");
        if (createdAtTimestamp != null) {
            leaveType.setCreatedAt(createdAtTimestamp.toLocalDateTime());
        }
        
        return leaveType;
    }
    
    /**
     * Get current Manila time
     * @return 
     */
    public LocalDateTime getCurrentManilaTime() {
        return LeaveTypeModel.getCurrentDateTimeManila();
    }
    
    /**
     * Get current Manila date
     * @return 
     */
    public LocalDate getCurrentManilaDate() {
        return LeaveTypeModel.getCurrentDateManila();
    }
}