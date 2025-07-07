package DAOs;

import Models.OvertimeRequestModel;
import Models.OvertimeRequestModel.ApprovalStatus;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

/**
 * Data Access Object for OvertimeRequestModel entities.
 * Enhanced with overtime approval workflow.
 * @author User
 */
public class OvertimeRequestDAO extends BaseDAO<OvertimeRequestModel, Integer> {

    /**
     * Constructor that accepts a DatabaseConnection instance
     * @param databaseConnection The database connection to use for all operations
     */
    public OvertimeRequestDAO(DatabaseConnection databaseConnection) {
        super(databaseConnection);
    }

    /**
     * Default constructor using default database connection
     */
    public OvertimeRequestDAO() {
        super(new DatabaseConnection());
    }

    // ABSTRACT METHOD IMPLEMENTATIONS - Required by BaseDAO

    /**
     * Converts a database row into an OvertimeRequestModel object
     * @param rs The ResultSet containing overtime request data from the database
     * @return A fully populated OvertimeRequestModel object
     * @throws SQLException if there's an error reading from the database
     */
    @Override
    protected OvertimeRequestModel mapResultSetToEntity(ResultSet rs) throws SQLException {
        OvertimeRequestModel overtime = new OvertimeRequestModel();
        
        overtime.setOvertimeRequestId(rs.getInt("overtimeRequestId"));
        overtime.setEmployeeId(rs.getInt("employeeId"));
        
        // Handle datetime fields - MySQL datetime maps to LocalDateTime
        Timestamp overtimeStart = rs.getTimestamp("overtimeStart");
        if (overtimeStart != null) {
            overtime.setOvertimeStart(overtimeStart.toLocalDateTime());
        }
        
        Timestamp overtimeEnd = rs.getTimestamp("overtimeEnd");
        if (overtimeEnd != null) {
            overtime.setOvertimeEnd(overtimeEnd.toLocalDateTime());
        }
        
        overtime.setOvertimeReason(rs.getString("overtimeReason"));
        
        // Handle enum for approval status
        String statusStr = rs.getString("approvalStatus");
        if (statusStr != null) {
            overtime.setApprovalStatus(ApprovalStatus.fromString(statusStr));
        }
        
        // Handle datetime fields
        Timestamp dateCreated = rs.getTimestamp("dateCreated");
        if (dateCreated != null) {
            overtime.setDateCreated(dateCreated.toLocalDateTime());
        }
        
        Timestamp dateApproved = rs.getTimestamp("dateApproved");
        if (dateApproved != null) {
            overtime.setDateApproved(dateApproved.toLocalDateTime());
        }
        
        overtime.setSupervisorNotes(rs.getString("supervisorNotes"));
        
        return overtime;
    }

    @Override
    protected String getTableName() {
        return "overtimerequest";
    }

    @Override
    protected String getPrimaryKeyColumn() {
        return "overtimeRequestId";
    }

    @Override
    protected void setInsertParameters(PreparedStatement stmt, OvertimeRequestModel overtime) throws SQLException {
        int paramIndex = 1;
        
        stmt.setInt(paramIndex++, overtime.getEmployeeId());
        
        if (overtime.getOvertimeStart() != null) {
            stmt.setTimestamp(paramIndex++, Timestamp.valueOf(overtime.getOvertimeStart()));
        } else {
            stmt.setNull(paramIndex++, Types.TIMESTAMP);
        }
        
        if (overtime.getOvertimeEnd() != null) {
            stmt.setTimestamp(paramIndex++, Timestamp.valueOf(overtime.getOvertimeEnd()));
        } else {
            stmt.setNull(paramIndex++, Types.TIMESTAMP);
        }
        
        if (overtime.getOvertimeReason() != null) {
            stmt.setString(paramIndex++, overtime.getOvertimeReason());
        } else {
            stmt.setNull(paramIndex++, Types.VARCHAR);
        }
        
        if (overtime.getApprovalStatus() != null) {
            stmt.setString(paramIndex++, overtime.getApprovalStatus().toString());
        } else {
            stmt.setString(paramIndex++, "Pending");
        }
        
        if (overtime.getDateApproved() != null) {
            stmt.setTimestamp(paramIndex++, Timestamp.valueOf(overtime.getDateApproved()));
        } else {
            stmt.setNull(paramIndex++, Types.TIMESTAMP);
        }
        
        if (overtime.getSupervisorNotes() != null) {
            stmt.setString(paramIndex++, overtime.getSupervisorNotes());
        } else {
            stmt.setNull(paramIndex++, Types.VARCHAR);
        }
    }

    @Override
    protected void setUpdateParameters(PreparedStatement stmt, OvertimeRequestModel overtime) throws SQLException {
        int paramIndex = 1;
        
        stmt.setInt(paramIndex++, overtime.getEmployeeId());
        
        if (overtime.getOvertimeStart() != null) {
            stmt.setTimestamp(paramIndex++, Timestamp.valueOf(overtime.getOvertimeStart()));
        } else {
            stmt.setNull(paramIndex++, Types.TIMESTAMP);
        }
        
        if (overtime.getOvertimeEnd() != null) {
            stmt.setTimestamp(paramIndex++, Timestamp.valueOf(overtime.getOvertimeEnd()));
        } else {
            stmt.setNull(paramIndex++, Types.TIMESTAMP);
        }
        
        if (overtime.getOvertimeReason() != null) {
            stmt.setString(paramIndex++, overtime.getOvertimeReason());
        } else {
            stmt.setNull(paramIndex++, Types.VARCHAR);
        }
        
        if (overtime.getApprovalStatus() != null) {
            stmt.setString(paramIndex++, overtime.getApprovalStatus().toString());
        } else {
            stmt.setString(paramIndex++, "Pending");
        }
        
        if (overtime.getDateApproved() != null) {
            stmt.setTimestamp(paramIndex++, Timestamp.valueOf(overtime.getDateApproved()));
        } else {
            stmt.setNull(paramIndex++, Types.TIMESTAMP);
        }
        
        if (overtime.getSupervisorNotes() != null) {
            stmt.setString(paramIndex++, overtime.getSupervisorNotes());
        } else {
            stmt.setNull(paramIndex++, Types.VARCHAR);
        }
        
        stmt.setInt(paramIndex++, overtime.getOvertimeRequestId());
    }

    @Override
    protected Integer getEntityId(OvertimeRequestModel overtime) {
        return overtime.getOvertimeRequestId();
    }

    @Override
    protected void handleGeneratedKey(OvertimeRequestModel entity, ResultSet generatedKeys) throws SQLException {
        if (generatedKeys.next()) {
            entity.setOvertimeRequestId(generatedKeys.getInt(1));
        }
    }

    // CUSTOM SQL BUILDERS

    private String buildInsertSQL() {
        return "INSERT INTO overtimerequest " +
               "(employeeId, overtimeStart, overtimeEnd, overtimeReason, " +
               "approvalStatus, dateApproved, supervisorNotes) " +
               "VALUES (?, ?, ?, ?, ?, ?, ?)";
    }

    private String buildUpdateSQL() {
        return "UPDATE overtimerequest SET " +
               "employeeId = ?, overtimeStart = ?, overtimeEnd = ?, overtimeReason = ?, " +
               "approvalStatus = ?, dateApproved = ?, supervisorNotes = ? " +
               "WHERE overtimeRequestId = ?";
    }

    // ENHANCED OVERTIME APPROVAL WORKFLOW METHODS

    /**
     * Finds all pending overtime requests that need approval
     * @return List of pending overtime requests ordered by date created
     */
    public List<OvertimeRequestModel> findPendingOvertimeRequests() {
        String sql = "SELECT * FROM overtimerequest WHERE approvalStatus = ? ORDER BY dateCreated ASC";
        return executeQuery(sql, "Pending");
    }

    /**
     * Finds all overtime requests for a specific employee
     * @param employeeId The employee ID
     * @return List of overtime requests for the employee ordered by date created (newest first)
     */
    public List<OvertimeRequestModel> findByEmployee(Integer employeeId) {
        String sql = "SELECT * FROM overtimerequest WHERE employeeId = ? ORDER BY dateCreated DESC";
        return executeQuery(sql, employeeId);
    }

    /**
     * Finds overtime requests by approval status
     * @param status The approval status to search for
     * @return List of overtime requests with the specified status
     */
    public List<OvertimeRequestModel> findByStatus(ApprovalStatus status) {
        String sql = "SELECT * FROM overtimerequest WHERE approvalStatus = ? ORDER BY dateCreated DESC";
        return executeQuery(sql, status.toString());
    }

    /**
     * Approves an overtime request with proper workflow
     * @param overtimeRequestId The overtime request ID to approve
     * @param supervisorNotes Optional notes from supervisor
     * @return true if approval was successful
     */
    public boolean approveOvertime(Integer overtimeRequestId, String supervisorNotes) {
        String sql = "UPDATE overtimerequest SET approvalStatus = ?, dateApproved = CURRENT_TIMESTAMP, supervisorNotes = ? WHERE overtimeRequestId = ? AND approvalStatus = 'Pending'";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, "Approved");
            stmt.setString(2, supervisorNotes);
            stmt.setInt(3, overtimeRequestId);
            
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                System.out.println("Overtime request approved: " + overtimeRequestId);
                return true;
            }
            
        } catch (SQLException e) {
            System.err.println("Error approving overtime request: " + e.getMessage());
        }
        
        return false;
    }

    /**
     * Rejects an overtime request with proper workflow
     * @param overtimeRequestId The overtime request ID to reject
     * @param supervisorNotes Required notes explaining rejection
     * @return true if rejection was successful
     */
    public boolean rejectOvertime(Integer overtimeRequestId, String supervisorNotes) {
        if (supervisorNotes == null || supervisorNotes.trim().isEmpty()) {
            System.err.println("Supervisor notes are required for rejection");
            return false;
        }
        
        String sql = "UPDATE overtimerequest SET approvalStatus = ?, dateApproved = CURRENT_TIMESTAMP, supervisorNotes = ? WHERE overtimeRequestId = ? AND approvalStatus = 'Pending'";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, "Rejected");
            stmt.setString(2, supervisorNotes);
            stmt.setInt(3, overtimeRequestId);
            
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                System.out.println("Overtime request rejected: " + overtimeRequestId);
                return true;
            }
            
        } catch (SQLException e) {
            System.err.println("Error rejecting overtime request: " + e.getMessage());
        }
        
        return false;
    }

    /**
     * Finds pending overtime requests for a specific supervisor
     * @param supervisorId The supervisor's employee ID
     * @return List of pending overtime requests requiring approval
     */
    public List<OvertimeRequestModel> findPendingOvertimeRequestsForSupervisor(Integer supervisorId) {
        String sql = "SELECT o.* FROM overtimerequest o " +
                    "JOIN employee e ON o.employeeId = e.employeeId " +
                    "WHERE o.approvalStatus = 'Pending' AND e.supervisorId = ? " +
                    "ORDER BY o.dateCreated ASC";
        return executeQuery(sql, supervisorId);
    }

    /**
     * Calculates overtime pay for a specific overtime request
     * @param overtimeRequestId The overtime request ID
     * @param overtimeMultiplier The overtime pay multiplier (e.g., 1.25 for 25% premium)
     * @return The calculated overtime pay amount, or null if calculation fails
     */
    public BigDecimal calculateOvertimePay(Integer overtimeRequestId, BigDecimal overtimeMultiplier) {
        String sql = "SELECT o.overtimeStart, o.overtimeEnd, e.hourlyRate " +
                    "FROM overtimerequest o " +
                    "JOIN employee e ON o.employeeId = e.employeeId " +
                    "WHERE o.overtimeRequestId = ? AND o.approvalStatus = 'Approved'";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, overtimeRequestId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Timestamp overtimeStart = rs.getTimestamp("overtimeStart");
                    Timestamp overtimeEnd = rs.getTimestamp("overtimeEnd");
                    BigDecimal hourlyRate = rs.getBigDecimal("hourlyRate");
                    
                    if (overtimeStart != null && overtimeEnd != null && hourlyRate != null) {
                        // Calculate overtime hours
                        LocalDateTime start = overtimeStart.toLocalDateTime();
                        LocalDateTime end = overtimeEnd.toLocalDateTime();
                        
                        long minutes = java.time.Duration.between(start, end).toMinutes();
                        BigDecimal overtimeHours = new BigDecimal(minutes).divide(new BigDecimal(60), 2, BigDecimal.ROUND_HALF_UP);
                        
                        // Calculate overtime pay: hours × hourly rate × multiplier
                        BigDecimal overtimePay = overtimeHours.multiply(hourlyRate).multiply(overtimeMultiplier);
                        
                        return overtimePay.setScale(2, BigDecimal.ROUND_HALF_UP);
                    }
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error calculating overtime pay: " + e.getMessage());
        }
        
        return null;
    }

    /**
     * Gets total overtime hours for an employee in a specific month
     * Only includes approved overtime requests
     * @param employeeId The employee ID
     * @param year The year
     * @param month The month (1-12)
     * @return Total overtime hours as BigDecimal
     */
    public BigDecimal getTotalOvertimeHours(Integer employeeId, int year, int month) {
        String sql = "SELECT SUM(TIMESTAMPDIFF(MINUTE, overtimeStart, overtimeEnd)) as totalMinutes " +
                    "FROM overtimerequest " +
                    "WHERE employeeId = ? AND approvalStatus = 'Approved' " +
                    "AND YEAR(overtimeStart) = ? AND MONTH(overtimeStart) = ?";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, employeeId);
            stmt.setInt(2, year);
            stmt.setInt(3, month);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    long totalMinutes = rs.getLong("totalMinutes");
                    if (rs.wasNull()) {
                        return BigDecimal.ZERO;
                    }
                    // Convert minutes to hours
                    return new BigDecimal(totalMinutes).divide(new BigDecimal(60), 2, BigDecimal.ROUND_HALF_UP);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error calculating total overtime hours: " + e.getMessage());
        }
        
        return BigDecimal.ZERO;
    }

    /**
     * Gets total overtime pay for an employee in a specific month
     * @param employeeId The employee ID
     * @param year The year
     * @param month The month (1-12)
     * @param overtimeMultiplier The overtime pay multiplier (e.g., 1.25)
     * @return Total overtime pay as BigDecimal
     */
    public BigDecimal getTotalOvertimePay(Integer employeeId, int year, int month, BigDecimal overtimeMultiplier) {
        String sql = "SELECT o.overtimeStart, o.overtimeEnd, e.hourlyRate " +
                    "FROM overtimerequest o " +
                    "JOIN employee e ON o.employeeId = e.employeeId " +
                    "WHERE o.employeeId = ? AND o.approvalStatus = 'Approved' " +
                    "AND YEAR(o.overtimeStart) = ? AND MONTH(o.overtimeStart) = ?";
        
        BigDecimal totalPay = BigDecimal.ZERO;
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, employeeId);
            stmt.setInt(2, year);
            stmt.setInt(3, month);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Timestamp overtimeStart = rs.getTimestamp("overtimeStart");
                    Timestamp overtimeEnd = rs.getTimestamp("overtimeEnd");
                    BigDecimal hourlyRate = rs.getBigDecimal("hourlyRate");
                    
                    if (overtimeStart != null && overtimeEnd != null && hourlyRate != null) {
                        // Calculate overtime hours for this request
                        long minutes = java.time.Duration.between(
                            overtimeStart.toLocalDateTime(),
                            overtimeEnd.toLocalDateTime()
                        ).toMinutes();
                        
                        BigDecimal overtimeHours = new BigDecimal(minutes).divide(new BigDecimal(60), 2, BigDecimal.ROUND_HALF_UP);
                        BigDecimal overtimePay = overtimeHours.multiply(hourlyRate).multiply(overtimeMultiplier);
                        
                        totalPay = totalPay.add(overtimePay);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error calculating total overtime pay: " + e.getMessage());
        }
        
        return totalPay.setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * Finds overtime requests within a date range
     * @param startDate The start date/time
     * @param endDate The end date/time
     * @return List of overtime requests within the specified range
     */
    public List<OvertimeRequestModel> findByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        String sql = "SELECT * FROM overtimerequest WHERE overtimeStart >= ? AND overtimeEnd <= ? ORDER BY overtimeStart";
        return executeQuery(sql, Timestamp.valueOf(startDate), Timestamp.valueOf(endDate));
    }

    // HELPER METHODS

    /**
     * Helper method to execute queries with parameters
     * @param sql SQL query
     * @param params Query parameters
     * @return List of overtime requests
     */
    @Override
    protected List<OvertimeRequestModel> executeQuery(String sql, Object... params) {
        List<OvertimeRequestModel> results = new ArrayList<>();
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(mapResultSetToEntity(rs));
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error executing overtime query: " + e.getMessage());
        }
        
        return results;
    }

    // OVERRIDE METHODS

    @Override
    public boolean save(OvertimeRequestModel overtime) {
        String sql = buildInsertSQL();
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            setInsertParameters(stmt, overtime);
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        handleGeneratedKey(overtime, generatedKeys);
                    }
                }
                return true;
            }
            return false;
            
        } catch (SQLException e) {
            System.err.println("Error saving overtime request: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean update(OvertimeRequestModel overtime) {
        String sql = buildUpdateSQL();
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            setUpdateParameters(stmt, overtime);
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            System.err.println("Error updating overtime request: " + e.getMessage());
            return false;
        }
    }
}