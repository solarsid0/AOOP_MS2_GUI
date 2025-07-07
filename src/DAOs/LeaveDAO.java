package DAOs;

import Models.LeaveRequestModel;
import Models.LeaveRequestModel.ApprovalStatus;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Data Access Object for LeaveRequestModel entities.
 * This class handles all database operations related to leave requests.
 * It extends BaseDAO to inherit common CRUD operations and adds leave-specific methods.
 * @author User
 */
public class LeaveDAO extends BaseDAO<LeaveRequestModel, Integer> {
    
    /**
     * Constructor that accepts a DatabaseConnection instance
     * @param databaseConnection The database connection to use for all operations
     */
    public LeaveDAO(DatabaseConnection databaseConnection) {
        super(databaseConnection);
    }
    

    // ABSTRACT METHOD IMPLEMENTATIONS - Required by BaseDAO

    
    /**
     * Converts a database row into a LeaveRequestModel object
     * This method reads each column from the ResultSet and creates a LeaveRequestModel
     * @param rs The ResultSet containing leave request data from the database
     * @return A fully populated LeaveRequestModel object
     * @throws SQLException if there's an error reading from the database
     */
    @Override
    protected LeaveRequestModel mapResultSetToEntity(ResultSet rs) throws SQLException {
        LeaveRequestModel leave = new LeaveRequestModel();
        
        // Use correct database column names
        leave.setLeaveRequestId(rs.getInt("leaveRequestId"));
        leave.setEmployeeId(rs.getInt("employeeId"));
        leave.setLeaveTypeId(rs.getInt("leaveTypeId"));
        
        // Handle date fields - Direct assignment (no conversion needed)
        leave.setLeaveStart(rs.getDate("leaveStart"));
        leave.setLeaveEnd(rs.getDate("leaveEnd"));
        
        // Handle other fields
        leave.setLeaveReason(rs.getString("leaveReason"));
        
        // Handle enum for approval status
        String statusStr = rs.getString("approvalStatus");
        if (statusStr != null) {
            leave.setApprovalStatus(ApprovalStatus.fromString(statusStr));
        }
        
        // Handle timestamp fields - Direct assignment (no conversion needed)
        leave.setDateCreated(rs.getTimestamp("dateCreated"));
        leave.setDateApproved(rs.getTimestamp("dateApproved"));
        
        leave.setSupervisorNotes(rs.getString("supervisorNotes"));
        
        return leave;
    }
    
    /**
     * Returns the database table name for leave requests
     * @return "leaverequest" - the name of the leaverequest table in the database
     */
    @Override
    protected String getTableName() {
        return "leaverequest";
    }
    
    /**
     * Returns the primary key column name for the leaverequest table
     * @return "leaveRequestId" - the primary key column name
     */
    @Override
    protected String getPrimaryKeyColumn() {
        return "leaveRequestId";
    }
    
    /**
     * Sets parameters for INSERT operations when creating new leave requests
     * This method maps LeaveRequestModel object properties to SQL parameters
     * @param stmt The PreparedStatement to set parameters on
     * @param leave The LeaveRequestModel object to get values from
     * @throws SQLException if there's an error setting parameters
     */
    @Override
    protected void setInsertParameters(PreparedStatement stmt, LeaveRequestModel leave) throws SQLException {
        int paramIndex = 1;
        
        // Set required fields in correct order
        stmt.setInt(paramIndex++, leave.getEmployeeId());
        stmt.setInt(paramIndex++, leave.getLeaveTypeId());
        
        // Handle Date fields - Direct assignment
        stmt.setDate(paramIndex++, leave.getLeaveStart());
        stmt.setDate(paramIndex++, leave.getLeaveEnd());
        
        // Handle optional fields
        if (leave.getLeaveReason() != null) {
            stmt.setString(paramIndex++, leave.getLeaveReason());
        } else {
            stmt.setNull(paramIndex++, Types.VARCHAR);
        }
        
        // Handle enum properly
        if (leave.getApprovalStatus() != null) {
            stmt.setString(paramIndex++, leave.getApprovalStatus().getValue());
        } else {
            stmt.setString(paramIndex++, ApprovalStatus.PENDING.getValue());
        }
        
        // Handle optional timestamp fields - Direct assignment
        stmt.setTimestamp(paramIndex++, leave.getDateApproved());
        
        if (leave.getSupervisorNotes() != null) {
            stmt.setString(paramIndex++, leave.getSupervisorNotes());
        } else {
            stmt.setNull(paramIndex++, Types.VARCHAR);
        }
    }
    
    /**
     * Sets parameters for UPDATE operations when modifying existing leave requests
     * This method maps LeaveRequestModel object properties to SQL parameters for updates
     * @param stmt The PreparedStatement to set parameters on
     * @param leave The LeaveRequestModel object with updated values
     * @throws SQLException if there's an error setting parameters
     */
    @Override
    protected void setUpdateParameters(PreparedStatement stmt, LeaveRequestModel leave) throws SQLException {
        int paramIndex = 1;
        
        // Set all the same fields as INSERT (excluding auto-increment ID)
        stmt.setInt(paramIndex++, leave.getEmployeeId());
        stmt.setInt(paramIndex++, leave.getLeaveTypeId());
        
        // Handle Date fields - Direct assignment
        stmt.setDate(paramIndex++, leave.getLeaveStart());
        stmt.setDate(paramIndex++, leave.getLeaveEnd());
        
        if (leave.getLeaveReason() != null) {
            stmt.setString(paramIndex++, leave.getLeaveReason());
        } else {
            stmt.setNull(paramIndex++, Types.VARCHAR);
        }
        
        if (leave.getApprovalStatus() != null) {
            stmt.setString(paramIndex++, leave.getApprovalStatus().getValue());
        } else {
            stmt.setString(paramIndex++, ApprovalStatus.PENDING.getValue());
        }
        
        // Handle timestamp - Direct assignment
        stmt.setTimestamp(paramIndex++, leave.getDateApproved());
        
        if (leave.getSupervisorNotes() != null) {
            stmt.setString(paramIndex++, leave.getSupervisorNotes());
        } else {
            stmt.setNull(paramIndex++, Types.VARCHAR);
        }
        
        // Finally, set the leave request ID for the WHERE clause
        stmt.setInt(paramIndex++, leave.getLeaveRequestId());
    }
    
    /**
     * Gets the ID from a LeaveRequestModel object
     * This is used by BaseDAO for update and delete operations
     * @param leave The LeaveRequestModel object to get ID from
     * @return The leave request's ID
     */
    @Override
    protected Integer getEntityId(LeaveRequestModel leave) {
        return leave.getLeaveRequestId();
    }
    
    /**
     * Handles auto-generated leave request IDs after INSERT operations
     * This method sets the generated leaveRequestId back on the LeaveRequestModel object
     * @param entity The LeaveRequestModel that was just inserted
     * @param generatedKeys The ResultSet containing the generated leaveRequestId
     * @throws SQLException if there's an error reading the generated key
     */
    @Override
    protected void handleGeneratedKey(LeaveRequestModel entity, ResultSet generatedKeys) throws SQLException {
        if (generatedKeys.next()) {
            entity.setLeaveRequestId(generatedKeys.getInt(1));
        }
    }
    

    // CUSTOM SQL BUILDERS

    
    /**
     * Builds the complete INSERT SQL statement for leave requests
     * @return The complete INSERT SQL statement
     */
    private String buildInsertSQL() {
        return "INSERT INTO leaverequest " +
               "(employeeId, leaveTypeId, leaveStart, leaveEnd, leaveReason, " +
               "approvalStatus, dateApproved, supervisorNotes) " +
               "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    }
    
    /**
     * Builds the complete UPDATE SQL statement for leave requests
     * @return The complete UPDATE SQL statement
     */
    private String buildUpdateSQL() {
        return "UPDATE leaverequest SET " +
               "employeeId = ?, leaveTypeId = ?, leaveStart = ?, leaveEnd = ?, " +
               "leaveReason = ?, approvalStatus = ?, dateApproved = ?, supervisorNotes = ? " +
               "WHERE leaveRequestId = ?";
    }
    

    // CUSTOM LEAVE REQUEST METHODS

    
    /**
     * Finds all leave requests for a specific employee
     * @param employeeId The employee ID
     * @return List of leave requests for the employee
     */
    public List<LeaveRequestModel> findByEmployee(Integer employeeId) {
        String sql = "SELECT * FROM leaverequest WHERE employeeId = ? ORDER BY dateCreated DESC";
        return executeQuery(sql, employeeId);
    }
    
    /**
     * Finds leave requests by approval status
     * @param status The approval status to search for
     * @return List of leave requests with the specified status
     */
    public List<LeaveRequestModel> findByStatus(ApprovalStatus status) {
        String sql = "SELECT * FROM leaverequest WHERE approvalStatus = ? ORDER BY dateCreated DESC";
        return executeQuery(sql, status.getValue());
    }
    
    /**
     * Finds pending leave requests that need approval
     * @return List of pending leave requests
     */
    public List<LeaveRequestModel> findPendingRequests() {
        return findByStatus(ApprovalStatus.PENDING);
    }
    
    /**
     * Approves a leave request
     * @param leaveRequestId The leave request ID
     * @param supervisorNotes Optional notes from supervisor
     * @return true if approval was successful
     */
    public boolean approveLeaveRequest(Integer leaveRequestId, String supervisorNotes) {
        String sql = "UPDATE leaverequest SET approvalStatus = ?, dateApproved = CURRENT_TIMESTAMP, supervisorNotes = ? WHERE leaveRequestId = ?";
        int rowsAffected = executeUpdate(sql, ApprovalStatus.APPROVED.getValue(), supervisorNotes, leaveRequestId);
        return rowsAffected > 0;
    }
    
    /**
     * Rejects a leave request
     * @param leaveRequestId The leave request ID
     * @param supervisorNotes Required notes explaining rejection
     * @return true if rejection was successful
     */
    public boolean rejectLeaveRequest(Integer leaveRequestId, String supervisorNotes) {
        String sql = "UPDATE leaverequest SET approvalStatus = ?, dateApproved = CURRENT_TIMESTAMP, supervisorNotes = ? WHERE leaveRequestId = ?";
        int rowsAffected = executeUpdate(sql, ApprovalStatus.REJECTED.getValue(), supervisorNotes, leaveRequestId);
        return rowsAffected > 0;
    }
    

    // OVERRIDE METHODS

    
    /**
     * Override the save method to use custom INSERT SQL
     * @param leave The leave request to save
     * @return true if save was successful, false otherwise
     */
    @Override
    public boolean save(LeaveRequestModel leave) {
        String sql = buildInsertSQL();
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            setInsertParameters(stmt, leave);
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        handleGeneratedKey(leave, generatedKeys);
                    }
                }
                return true;
            }
            return false;
            
        } catch (SQLException e) {
            System.err.println("Error saving leave request: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Override the update method to use custom UPDATE SQL
     * @param leave The leave request to update
     * @return true if update was successful, false otherwise
     */
    @Override
    public boolean update(LeaveRequestModel leave) {
        String sql = buildUpdateSQL();
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            setUpdateParameters(stmt, leave);
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            System.err.println("Error updating leave request: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}