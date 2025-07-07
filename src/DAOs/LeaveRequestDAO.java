package DAOs;

import Models.LeaveRequestModel;
import java.sql.*;
import java.time.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

/**
 * This class manages employee leave requests, including creation, validation, status updates, conflict checks, and supervisor approval workflows.
 */
public class LeaveRequestDAO {
    private static final ZoneId MANILA_TIMEZONE = ZoneId.of("Asia/Manila");
    
    // SQL Queries
    private static final String INSERT_LEAVE_REQUEST = 
        "INSERT INTO leaverequest (employeeId, leaveTypeId, leaveStart, leaveEnd, leaveReason, " +
        "approvalStatus, dateCreated) VALUES (?, ?, ?, ?, ?, ?, ?)";
    
    private static final String UPDATE_LEAVE_REQUEST = 
        "UPDATE leaverequest SET employeeId = ?, leaveTypeId = ?, leaveStart = ?, leaveEnd = ?, " +
        "leaveReason = ?, approvalStatus = ?, dateApproved = ?, supervisorNotes = ? " +
        "WHERE leaveRequestId = ?";
    
    private static final String SELECT_BY_ID = 
        "SELECT leaveRequestId, employeeId, leaveTypeId, leaveStart, leaveEnd, leaveReason, " +
        "approvalStatus, dateCreated, dateApproved, supervisorNotes FROM leaverequest " +
        "WHERE leaveRequestId = ?";
    
    private static final String SELECT_BY_EMPLOYEE = 
        "SELECT leaveRequestId, employeeId, leaveTypeId, leaveStart, leaveEnd, leaveReason, " +
        "approvalStatus, dateCreated, dateApproved, supervisorNotes FROM leaverequest " +
        "WHERE employeeId = ? ORDER BY dateCreated DESC";
    
    private static final String SELECT_BY_EMPLOYEE_AND_STATUS = 
        "SELECT leaveRequestId, employeeId, leaveTypeId, leaveStart, leaveEnd, leaveReason, " +
        "approvalStatus, dateCreated, dateApproved, supervisorNotes FROM leaverequest " +
        "WHERE employeeId = ? AND approvalStatus = ? ORDER BY dateCreated DESC";
    
    private static final String SELECT_BY_EMPLOYEE_DATE_RANGE = 
        "SELECT leaveRequestId, employeeId, leaveTypeId, leaveStart, leaveEnd, leaveReason, " +
        "approvalStatus, dateCreated, dateApproved, supervisorNotes FROM leaverequest " +
        "WHERE employeeId = ? AND ((leaveStart BETWEEN ? AND ?) OR (leaveEnd BETWEEN ? AND ?) OR " +
        "(leaveStart <= ? AND leaveEnd >= ?)) ORDER BY leaveStart";
    
    private static final String SELECT_PENDING_FOR_SUPERVISOR = 
        "SELECT lr.leaveRequestId, lr.employeeId, lr.leaveTypeId, lr.leaveStart, lr.leaveEnd, " +
        "lr.leaveReason, lr.approvalStatus, lr.dateCreated, lr.dateApproved, lr.supervisorNotes " +
        "FROM leaverequest lr " +
        "INNER JOIN employee e ON lr.employeeId = e.employeeId " +
        "WHERE e.supervisorId = ? AND lr.approvalStatus = 'Pending' " +
        "ORDER BY lr.dateCreated ASC";
    
    private static final String SELECT_PENDING_REQUESTS = 
        "SELECT leaveRequestId, employeeId, leaveTypeId, leaveStart, leaveEnd, leaveReason, " +
        "approvalStatus, dateCreated, dateApproved, supervisorNotes FROM leaverequest " +
        "WHERE approvalStatus = 'Pending' ORDER BY dateCreated ASC";
    
    private static final String DELETE_LEAVE_REQUEST = 
        "DELETE FROM leaverequest WHERE leaveRequestId = ?";
    
    private static final String SELECT_OVERLAPPING_REQUESTS = 
        "SELECT leaveRequestId, employeeId, leaveTypeId, leaveStart, leaveEnd, leaveReason, " +
        "approvalStatus, dateCreated, dateApproved, supervisorNotes FROM leaverequest " +
        "WHERE employeeId = ? AND leaveRequestId != ? AND approvalStatus IN ('Pending', 'Approved') " +
        "AND ((leaveStart BETWEEN ? AND ?) OR (leaveEnd BETWEEN ? AND ?) OR " +
        "(leaveStart <= ? AND leaveEnd >= ?))";
    
    private static final String SELECT_UPCOMING_LEAVES = 
        "SELECT leaveRequestId, employeeId, leaveTypeId, leaveStart, leaveEnd, leaveReason, " +
        "approvalStatus, dateCreated, dateApproved, supervisorNotes FROM leaverequest " +
        "WHERE employeeId = ? AND approvalStatus = 'Approved' AND leaveStart >= ? " +
        "ORDER BY leaveStart ASC";
    
    private static final String SELECT_LEAVE_SUMMARY = 
        "SELECT " +
        "COUNT(*) as totalRequests, " +
        "SUM(CASE WHEN approvalStatus = 'Approved' THEN 1 ELSE 0 END) as approvedRequests, " +
        "SUM(CASE WHEN approvalStatus = 'Rejected' THEN 1 ELSE 0 END) as rejectedRequests, " +
        "SUM(CASE WHEN approvalStatus = 'Pending' THEN 1 ELSE 0 END) as pendingRequests " +
        "FROM leaverequest WHERE employeeId = ? AND dateCreated BETWEEN ? AND ?";
    
    /**
     * Get database connection using centralized DatabaseConnection
     * (Timezone already handled in DatabaseConnection.getConnection())
     */
    private Connection getConnection() throws SQLException {
        return DatabaseConnection.getConnection();
    }
    
    /**
     * Create new leave request
     * @param leaveRequest
     * @return 
     */
    public boolean createLeaveRequest(LeaveRequestModel leaveRequest) {
        if (leaveRequest == null || !leaveRequest.isValidLeaveRequest()) {
            return false;
        }
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(INSERT_LEAVE_REQUEST, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setInt(1, leaveRequest.getEmployeeId());
            stmt.setInt(2, leaveRequest.getLeaveTypeId());
            stmt.setDate(3, leaveRequest.getLeaveStart());
            stmt.setDate(4, leaveRequest.getLeaveEnd());
            stmt.setString(5, leaveRequest.getLeaveReason());
            stmt.setString(6, leaveRequest.getApprovalStatus().getValue());
            stmt.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now(MANILA_TIMEZONE)));
            
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        leaveRequest.setLeaveRequestId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
            
        } catch (SQLException e) {
            System.err.println("Error creating leave request: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Update leave request
     * @param leaveRequest
     * @return 
     */
    public boolean updateLeaveRequest(LeaveRequestModel leaveRequest) {
        if (leaveRequest == null || leaveRequest.getLeaveRequestId() <= 0) {
            return false;
        }
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(UPDATE_LEAVE_REQUEST)) {
            
            stmt.setInt(1, leaveRequest.getEmployeeId());
            stmt.setInt(2, leaveRequest.getLeaveTypeId());
            stmt.setDate(3, leaveRequest.getLeaveStart());
            stmt.setDate(4, leaveRequest.getLeaveEnd());
            stmt.setString(5, leaveRequest.getLeaveReason());
            stmt.setString(6, leaveRequest.getApprovalStatus().getValue());
            stmt.setTimestamp(7, leaveRequest.getDateApproved());
            stmt.setString(8, leaveRequest.getSupervisorNotes());
            stmt.setInt(9, leaveRequest.getLeaveRequestId());
            
            return stmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Error updating leave request: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Get leave request by ID
     * @param leaveRequestId
     * @return 
     */
    public LeaveRequestModel getLeaveRequestById(int leaveRequestId) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_ID)) {
            
            stmt.setInt(1, leaveRequestId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToLeaveRequest(rs);
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting leave request by ID: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Get leave requests by employee
     * @param employeeId
     * @param status
     * @return 
     */
    public List<LeaveRequestModel> getLeaveRequestsByEmployee(int employeeId, LeaveRequestModel.ApprovalStatus status) {
        List<LeaveRequestModel> requests = new ArrayList<>();
        String query = status != null ? SELECT_BY_EMPLOYEE_AND_STATUS : SELECT_BY_EMPLOYEE;
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, employeeId);
            if (status != null) {
                stmt.setString(2, status.getValue());
            }
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    requests.add(mapResultSetToLeaveRequest(rs));
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting leave requests by employee: " + e.getMessage());
        }
        
        return requests;
    }
    
    /**
     * Get leave requests by employee and date range
     * @param employeeId
     * @param startDate
     * @param endDate
     * @return 
     */
    public List<LeaveRequestModel> getLeaveRequestsByEmployeeAndDateRange(int employeeId, Date startDate, Date endDate) {
        List<LeaveRequestModel> requests = new ArrayList<>();
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_EMPLOYEE_DATE_RANGE)) {
            
            stmt.setInt(1, employeeId);
            stmt.setDate(2, startDate);
            stmt.setDate(3, endDate);
            stmt.setDate(4, startDate);
            stmt.setDate(5, endDate);
            stmt.setDate(6, startDate);
            stmt.setDate(7, endDate);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    requests.add(mapResultSetToLeaveRequest(rs));
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting leave requests by employee and date range: " + e.getMessage());
        }
        
        return requests;
    }
    
    /**
     * Get pending leave requests for supervisor approval workflow
     * @param supervisorId
     * @return 
     */
    public List<LeaveRequestModel> getPendingLeaveRequestsForSupervisor(int supervisorId) {
        List<LeaveRequestModel> requests = new ArrayList<>();
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_PENDING_FOR_SUPERVISOR)) {
            
            stmt.setInt(1, supervisorId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    requests.add(mapResultSetToLeaveRequest(rs));
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting pending leave requests for supervisor: " + e.getMessage());
        }
        
        return requests;
    }
    
    /**
     * Get all pending leave requests
     * @return 
     */
    public List<LeaveRequestModel> getAllPendingLeaveRequests() {
        List<LeaveRequestModel> requests = new ArrayList<>();
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_PENDING_REQUESTS)) {
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    requests.add(mapResultSetToLeaveRequest(rs));
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting all pending leave requests: " + e.getMessage());
        }
        
        return requests;
    }
    
    /**
     * Check for overlapping leave requests
     * @param leaveRequest
     * @return 
     */
    public List<LeaveRequestModel> getOverlappingLeaveRequests(LeaveRequestModel leaveRequest) {
        List<LeaveRequestModel> overlapping = new ArrayList<>();
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_OVERLAPPING_REQUESTS)) {
            
            int requestId = leaveRequest.getLeaveRequestId() > 0 ? leaveRequest.getLeaveRequestId() : -1;
            
            stmt.setInt(1, leaveRequest.getEmployeeId());
            stmt.setInt(2, requestId);
            stmt.setDate(3, leaveRequest.getLeaveStart());
            stmt.setDate(4, leaveRequest.getLeaveEnd());
            stmt.setDate(5, leaveRequest.getLeaveStart());
            stmt.setDate(6, leaveRequest.getLeaveEnd());
            stmt.setDate(7, leaveRequest.getLeaveStart());
            stmt.setDate(8, leaveRequest.getLeaveEnd());
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    overlapping.add(mapResultSetToLeaveRequest(rs));
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error checking overlapping leave requests: " + e.getMessage());
        }
        
        return overlapping;
    }
    
    /**
     * Get upcoming approved leaves for employee
     * @param employeeId
     * @return 
     */
    public List<LeaveRequestModel> getUpcomingLeaves(int employeeId) {
        List<LeaveRequestModel> upcoming = new ArrayList<>();
        Date today = Date.valueOf(LocalDate.now(MANILA_TIMEZONE));
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_UPCOMING_LEAVES)) {
            
            stmt.setInt(1, employeeId);
            stmt.setDate(2, today);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    upcoming.add(mapResultSetToLeaveRequest(rs));
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting upcoming leaves: " + e.getMessage());
        }
        
        return upcoming;
    }
    
    /**
     * Get leave request summary for employee
     * @param employeeId
     * @param startDate
     * @param endDate
     * @return 
     */
    public Map<String, Object> getLeaveSummary(int employeeId, Date startDate, Date endDate) {
        Map<String, Object> summary = new HashMap<>();
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_LEAVE_SUMMARY)) {
            
            stmt.setInt(1, employeeId);
            stmt.setTimestamp(2, Timestamp.valueOf(startDate.toLocalDate().atStartOfDay()));
            stmt.setTimestamp(3, Timestamp.valueOf(endDate.toLocalDate().atTime(23, 59, 59)));
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int totalRequests = rs.getInt("totalRequests");
                    int approvedRequests = rs.getInt("approvedRequests");
                    int rejectedRequests = rs.getInt("rejectedRequests");
                    int pendingRequests = rs.getInt("pendingRequests");
                    
                    summary.put("totalRequests", totalRequests);
                    summary.put("approvedRequests", approvedRequests);
                    summary.put("rejectedRequests", rejectedRequests);
                    summary.put("pendingRequests", pendingRequests);
                    summary.put("approvalRate", totalRequests > 0 ? (double) approvedRequests / totalRequests * 100 : 0);
                    summary.put("rejectionRate", totalRequests > 0 ? (double) rejectedRequests / totalRequests * 100 : 0);
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting leave summary: " + e.getMessage());
        }
        
        return summary;
    }
    
    /**
     * Delete leave request
     * @param leaveRequestId
     * @return 
     */
    public boolean deleteLeaveRequest(int leaveRequestId) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(DELETE_LEAVE_REQUEST)) {
            
            stmt.setInt(1, leaveRequestId);
            return stmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Error deleting leave request: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Approve leave request with supervisor workflow
     * @param leaveRequestId
     * @param supervisorNotes
     * @param approverId
     * @return 
     */
    public boolean approveLeaveRequest(int leaveRequestId, String supervisorNotes, int approverId) {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            
            try {
                // Get the leave request
                LeaveRequestModel request = getLeaveRequestById(leaveRequestId);
                if (request == null || !request.isPending()) {
                    conn.rollback();
                    return false;
                }
                
                // Update the request status
                request.approve(supervisorNotes);
                
                // Update in database
                boolean updated = updateLeaveRequest(request);
                if (!updated) {
                    conn.rollback();
                    return false;
                }
                
                // Log the approval action
                logApprovalAction(conn, leaveRequestId, approverId, "APPROVED", supervisorNotes);
                
                conn.commit();
                return true;
                
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
            
        } catch (SQLException e) {
            System.err.println("Error approving leave request: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Reject leave request with supervisor workflow
     * @param leaveRequestId
     * @param supervisorNotes
     * @param approverId
     * @return 
     */
    public boolean rejectLeaveRequest(int leaveRequestId, String supervisorNotes, int approverId) {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            
            try {
                // Get the leave request
                LeaveRequestModel request = getLeaveRequestById(leaveRequestId);
                if (request == null || !request.isPending()) {
                    conn.rollback();
                    return false;
                }
                
                // Update the request status
                request.reject(supervisorNotes);
                
                // Update in database
                boolean updated = updateLeaveRequest(request);
                if (!updated) {
                    conn.rollback();
                    return false;
                }
                
                // Log the rejection action
                logApprovalAction(conn, leaveRequestId, approverId, "REJECTED", supervisorNotes);
                
                conn.commit();
                return true;
                
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
            
        } catch (SQLException e) {
            System.err.println("Error rejecting leave request: " + e.getMessage());
        }
        
        return false;
    }
    
    // Helper methods
    
    /**
     * Map ResultSet to LeaveRequestModel
     */
    private LeaveRequestModel mapResultSetToLeaveRequest(ResultSet rs) throws SQLException {
        LeaveRequestModel request = new LeaveRequestModel();
        
        request.setLeaveRequestId(rs.getInt("leaveRequestId"));
        request.setEmployeeId(rs.getInt("employeeId"));
        request.setLeaveTypeId(rs.getInt("leaveTypeId"));
        request.setLeaveStart(rs.getDate("leaveStart"));
        request.setLeaveEnd(rs.getDate("leaveEnd"));
        request.setLeaveReason(rs.getString("leaveReason"));
        request.setApprovalStatus(LeaveRequestModel.ApprovalStatus.fromString(rs.getString("approvalStatus")));
        request.setDateCreated(rs.getTimestamp("dateCreated"));
        request.setDateApproved(rs.getTimestamp("dateApproved"));
        request.setSupervisorNotes(rs.getString("supervisorNotes"));
        
        return request;
    }
    
    /**
     * Log approval action for audit trail
     */
    private void logApprovalAction(Connection conn, int leaveRequestId, int approverId, String action, String notes) 
            throws SQLException {
        String logQuery = "INSERT INTO leave_approval_log (leaveRequestId, approverId, action, notes, actionDate) " +
                         "VALUES (?, ?, ?, ?, ?)";
        
        try (PreparedStatement stmt = conn.prepareStatement(logQuery)) {
            stmt.setInt(1, leaveRequestId);
            stmt.setInt(2, approverId);
            stmt.setString(3, action);
            stmt.setString(4, notes);
            stmt.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now(MANILA_TIMEZONE)));
            
            stmt.executeUpdate();
        } catch (SQLException e) {
            // Log table might not exist, so we'll continue without logging
            System.err.println("Could not log approval action: " + e.getMessage());
        }
    }
    
    /**
     * Get current Manila time
     * @return 
     */
    public LocalDateTime getCurrentManilaTime() {
        return LocalDateTime.now(MANILA_TIMEZONE);
    }
    
    /**
     * Check if leave request conflicts with existing approved leaves
     * @param newRequest
     * @return 
     */
    public boolean hasConflictWithApprovedLeaves(LeaveRequestModel newRequest) {
        List<LeaveRequestModel> overlapping = getOverlappingLeaveRequests(newRequest);
        return overlapping.stream().anyMatch(request -> request.isApproved());
    }
}