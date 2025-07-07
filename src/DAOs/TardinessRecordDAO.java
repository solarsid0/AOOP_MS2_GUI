package DAOs;

import Models.TardinessRecordModel;
import Models.TardinessRecordModel.TardinessType;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.ArrayList;

/**
 * Data Access Object for TardinessRecordModel entities.
 * Enhanced with Manila timezone support and late/undertime tracking
 * @author User
 */
public class TardinessRecordDAO extends BaseDAO<TardinessRecordModel, Integer> {
    
    private static final ZoneId MANILA_TIMEZONE = ZoneId.of("Asia/Manila");

    /**
     * Constructor that accepts a DatabaseConnection instance
     * @param databaseConnection The database connection to use for all operations
     */
    public TardinessRecordDAO(DatabaseConnection databaseConnection) {
        super(databaseConnection);
    }

    /**
     * Default constructor using default database connection
     */
    public TardinessRecordDAO() {
        super(new DatabaseConnection());
    }

    // ABSTRACT METHOD IMPLEMENTATIONS - Required by BaseDAO

    /**
     * Converts a database row into a TardinessRecordModel object
     * @param rs The ResultSet containing tardiness data from the database
     * @return A fully populated TardinessRecordModel object
     * @throws SQLException if there's an error reading from the database
     */
    @Override
    protected TardinessRecordModel mapResultSetToEntity(ResultSet rs) throws SQLException {
        TardinessRecordModel tardiness = new TardinessRecordModel();
        
        tardiness.setTardinessId(rs.getInt("tardinessId"));
        tardiness.setAttendanceId(rs.getInt("attendanceId"));
        tardiness.setTardinessHours(rs.getBigDecimal("tardinessHours"));
        
        // Handle enum for tardiness type - fixed to use correct method
        String typeStr = rs.getString("tardinessType");
        if (typeStr != null) {
            tardiness.setTardinessType(TardinessType.fromString(typeStr));
        }
        
        tardiness.setSupervisorNotes(rs.getString("supervisorNotes"));
        
        // Handle createdAt timestamp with Manila timezone awareness
        Timestamp createdAt = rs.getTimestamp("createdAt");
        if (createdAt != null) {
            tardiness.setCreatedAt(createdAt);
        }
        
        return tardiness;
    }

    @Override
    protected String getTableName() {
        return "tardinessrecord";
    }

    @Override
    protected String getPrimaryKeyColumn() {
        return "tardinessId";
    }

    @Override
    protected void setInsertParameters(PreparedStatement stmt, TardinessRecordModel tardiness) throws SQLException {
        int paramIndex = 1;
        
        stmt.setInt(paramIndex++, tardiness.getAttendanceId());
        stmt.setBigDecimal(paramIndex++, tardiness.getTardinessHours());
        stmt.setString(paramIndex++, tardiness.getTardinessType().getValue()); // Fixed to use getValue()
        
        if (tardiness.getSupervisorNotes() != null) {
            stmt.setString(paramIndex++, tardiness.getSupervisorNotes());
        } else {
            stmt.setNull(paramIndex++, Types.VARCHAR);
        }
        
        // Set createdAt with Manila timezone
        if (tardiness.getCreatedAt() != null) {
            stmt.setTimestamp(paramIndex++, tardiness.getCreatedAt());
        } else {
            stmt.setTimestamp(paramIndex++, Timestamp.valueOf(LocalDateTime.now(MANILA_TIMEZONE)));
        }
    }

    @Override
    protected void setUpdateParameters(PreparedStatement stmt, TardinessRecordModel tardiness) throws SQLException {
        int paramIndex = 1;
        
        stmt.setInt(paramIndex++, tardiness.getAttendanceId());
        stmt.setBigDecimal(paramIndex++, tardiness.getTardinessHours());
        stmt.setString(paramIndex++, tardiness.getTardinessType().getValue()); // Fixed to use getValue()
        
        if (tardiness.getSupervisorNotes() != null) {
            stmt.setString(paramIndex++, tardiness.getSupervisorNotes());
        } else {
            stmt.setNull(paramIndex++, Types.VARCHAR);
        }
        
        // Set the ID for WHERE clause
        stmt.setInt(paramIndex++, tardiness.getTardinessId());
    }

    @Override
    protected Integer getEntityId(TardinessRecordModel tardiness) {
        return tardiness.getTardinessId();
    }

    @Override
    protected void handleGeneratedKey(TardinessRecordModel entity, ResultSet generatedKeys) throws SQLException {
        if (generatedKeys.next()) {
            entity.setTardinessId(generatedKeys.getInt(1));
        }
    }

    // CUSTOM SQL BUILDERS

    private String buildInsertSQL() {
        return "INSERT INTO tardinessrecord " +
               "(attendanceId, tardinessHours, tardinessType, supervisorNotes, createdAt) " +
               "VALUES (?, ?, ?, ?, ?)";
    }

    private String buildUpdateSQL() {
        return "UPDATE tardinessrecord SET " +
               "attendanceId = ?, tardinessHours = ?, tardinessType = ?, supervisorNotes = ? " +
               "WHERE tardinessId = ?";
    }

    // CUSTOM TARDINESS METHODS

    /**
     * Creates a new tardiness record (implementation of missing method)
     * @param tardinessRecord The tardiness record to create
     * @return true if successful, false otherwise
     */
    public boolean createTardinessRecord(TardinessRecordModel tardinessRecord) {
        if (tardinessRecord == null || !tardinessRecord.isValidRecord()) {
            System.err.println("Invalid tardiness record provided");
            return false;
        }
        
        return save(tardinessRecord);
    }

    /**
     * Deletes all tardiness records for a specific attendance (implementation of missing method)
     * @param attendanceId The attendance ID
     * @return true if successful, false otherwise
     */
    public boolean deleteTardinessRecordsByAttendance(int attendanceId) {
        String sql = "DELETE FROM tardinessrecord WHERE attendanceId = ?";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, attendanceId);
            int rowsAffected = stmt.executeUpdate();
            
            System.out.println("Deleted " + rowsAffected + " tardiness records for attendance ID: " + attendanceId);
            return true;
            
        } catch (SQLException e) {
            System.err.println("Error deleting tardiness records by attendance: " + e.getMessage());
            return false;
        }
    }

    /**
     * Finds all tardiness records for a specific attendance record
     * @param attendanceId The attendance ID
     * @return List of tardiness records
     */
    public List<TardinessRecordModel> findByAttendanceId(Integer attendanceId) {
        if (attendanceId == null || attendanceId <= 0) {
            return new ArrayList<>();
        }
        
        String sql = "SELECT * FROM tardinessrecord WHERE attendanceId = ? ORDER BY createdAt DESC";
        return executeQuery(sql, attendanceId);
    }

    /**
     * Finds tardiness records by type
     * @param tardinessType The tardiness type to search for
     * @return List of tardiness records with the specified type
     */
    public List<TardinessRecordModel> findByType(TardinessType tardinessType) {
        if (tardinessType == null) {
            return new ArrayList<>();
        }
        
        String sql = "SELECT * FROM tardinessrecord WHERE tardinessType = ? ORDER BY createdAt DESC";
        return executeQuery(sql, tardinessType.getValue());
    }

    /**
     * Gets tardiness records for an employee within a date range
     * Enhanced with Manila timezone support
     * @param employeeId The employee ID
     * @param startDate The start date
     * @param endDate The end date
     * @return List of tardiness records within the date range
     */
    public List<TardinessRecordModel> getTardinessRecordsForEmployee(Integer employeeId, 
                                                                   LocalDateTime startDate, 
                                                                   LocalDateTime endDate) {
        if (employeeId == null || employeeId <= 0 || startDate == null || endDate == null) {
            return new ArrayList<>();
        }
        
        String sql = "SELECT t.* FROM tardinessrecord t " +
                    "JOIN attendance a ON t.attendanceId = a.attendanceId " +
                    "WHERE a.employeeId = ? AND t.createdAt BETWEEN ? AND ? " +
                    "ORDER BY t.createdAt DESC";
        
        return executeQuery(sql, employeeId, 
                          Timestamp.valueOf(startDate), 
                          Timestamp.valueOf(endDate));
    }

    /**
     * Gets tardiness records for a specific month (Manila timezone)
     * @param employeeId The employee ID
     * @param yearMonth The year and month
     * @return List of tardiness records for the month
     */
    public List<TardinessRecordModel> getTardinessRecordsForMonth(Integer employeeId, YearMonth yearMonth) {
        if (employeeId == null || employeeId <= 0 || yearMonth == null) {
            return new ArrayList<>();
        }
        
        // Use Manila timezone for month boundaries
        LocalDateTime startDate = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime endDate = yearMonth.atEndOfMonth().atTime(23, 59, 59);
        
        return getTardinessRecordsForEmployee(employeeId, startDate, endDate);
    }

    /**
     * Gets tardiness records for current month (Manila timezone)
     * @param employeeId The employee ID
     * @return List of tardiness records for current month
     */
    public List<TardinessRecordModel> getCurrentMonthTardinessRecords(Integer employeeId) {
        YearMonth currentMonth = YearMonth.now(MANILA_TIMEZONE);
        return getTardinessRecordsForMonth(employeeId, currentMonth);
    }

    /**
     * Gets late records for an employee in a date range
     * @param employeeId The employee ID
     * @param startDate The start date
     * @param endDate The end date
     * @return List of late tardiness records
     */
    public List<TardinessRecordModel> getLateRecordsForEmployee(Integer employeeId, 
                                                              LocalDateTime startDate, 
                                                              LocalDateTime endDate) {
        if (employeeId == null || employeeId <= 0 || startDate == null || endDate == null) {
            return new ArrayList<>();
        }
        
        String sql = "SELECT t.* FROM tardinessrecord t " +
                    "JOIN attendance a ON t.attendanceId = a.attendanceId " +
                    "WHERE a.employeeId = ? AND t.tardinessType = 'Late' " +
                    "AND t.createdAt BETWEEN ? AND ? " +
                    "ORDER BY t.createdAt DESC";
        
        return executeQuery(sql, employeeId, 
                          Timestamp.valueOf(startDate), 
                          Timestamp.valueOf(endDate));
    }

    /**
     * Gets undertime records for an employee in a date range
     * @param employeeId The employee ID
     * @param startDate The start date
     * @param endDate The end date
     * @return List of undertime tardiness records
     */
    public List<TardinessRecordModel> getUndertimeRecordsForEmployee(Integer employeeId, 
                                                                   LocalDateTime startDate, 
                                                                   LocalDateTime endDate) {
        if (employeeId == null || employeeId <= 0 || startDate == null || endDate == null) {
            return new ArrayList<>();
        }
        
        String sql = "SELECT t.* FROM tardinessrecord t " +
                    "JOIN attendance a ON t.attendanceId = a.attendanceId " +
                    "WHERE a.employeeId = ? AND t.tardinessType = 'Undertime' " +
                    "AND t.createdAt BETWEEN ? AND ? " +
                    "ORDER BY t.createdAt DESC";
        
        return executeQuery(sql, employeeId, 
                          Timestamp.valueOf(startDate), 
                          Timestamp.valueOf(endDate));
    }

    /**
     * Gets total tardiness hours for an employee in a date range
     * @param employeeId The employee ID
     * @param startDate The start date
     * @param endDate The end date
     * @param tardinessType The type of tardiness (null for all types)
     * @return Total tardiness hours
     */
    public double getTotalTardinessHours(Integer employeeId, 
                                       LocalDateTime startDate, 
                                       LocalDateTime endDate,
                                       TardinessType tardinessType) {
        if (employeeId == null || employeeId <= 0 || startDate == null || endDate == null) {
            return 0.0;
        }
        
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COALESCE(SUM(t.tardinessHours), 0) as totalHours ");
        sql.append("FROM tardinessrecord t ");
        sql.append("JOIN attendance a ON t.attendanceId = a.attendanceId ");
        sql.append("WHERE a.employeeId = ? AND t.createdAt BETWEEN ? AND ? ");
        
        List<Object> params = new ArrayList<>();
        params.add(employeeId);
        params.add(Timestamp.valueOf(startDate));
        params.add(Timestamp.valueOf(endDate));
        
        if (tardinessType != null) {
            sql.append("AND t.tardinessType = ? ");
            params.add(tardinessType.getValue());
        }
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("totalHours");
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting total tardiness hours: " + e.getMessage());
        }
        
        return 0.0;
    }

    /**
     * Counts tardiness occurrences for an employee
     * @param employeeId The employee ID
     * @param startDate The start date
     * @param endDate The end date
     * @param tardinessType The type of tardiness (null for all types)
     * @return Number of tardiness occurrences
     */
    public int countTardinessOccurrences(Integer employeeId, 
                                       LocalDateTime startDate, 
                                       LocalDateTime endDate,
                                       TardinessType tardinessType) {
        if (employeeId == null || employeeId <= 0 || startDate == null || endDate == null) {
            return 0;
        }
        
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(*) as occurrences ");
        sql.append("FROM tardinessrecord t ");
        sql.append("JOIN attendance a ON t.attendanceId = a.attendanceId ");
        sql.append("WHERE a.employeeId = ? AND t.createdAt BETWEEN ? AND ? ");
        
        List<Object> params = new ArrayList<>();
        params.add(employeeId);
        params.add(Timestamp.valueOf(startDate));
        params.add(Timestamp.valueOf(endDate));
        
        if (tardinessType != null) {
            sql.append("AND t.tardinessType = ? ");
            params.add(tardinessType.getValue());
        }
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("occurrences");
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error counting tardiness occurrences: " + e.getMessage());
        }
        
        return 0;
    }

    // OVERRIDE METHODS

    @Override
    public boolean save(TardinessRecordModel tardiness) {
        if (tardiness == null || !tardiness.isValidRecord()) {
            System.err.println("Invalid tardiness record provided for save");
            return false;
        }
        
        String sql = buildInsertSQL();
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            setInsertParameters(stmt, tardiness);
            
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        handleGeneratedKey(tardiness, generatedKeys);
                    }
                }
                return true;
            }
            
            return false;
            
        } catch (SQLException e) {
            System.err.println("Error saving tardiness record: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean update(TardinessRecordModel tardiness) {
        if (tardiness == null || tardiness.getTardinessId() <= 0 || !tardiness.isValidRecord()) {
            System.err.println("Invalid tardiness record provided for update");
            return false;
        }
        
        String sql = buildUpdateSQL();
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            setUpdateParameters(stmt, tardiness);
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            System.err.println("Error updating tardiness record: " + e.getMessage());
            return false;
        }
    }

    /**
     * Gets current Manila timezone
     * @return Current LocalDateTime in Manila timezone
     */
    @Override
    public LocalDateTime getCurrentManilaTime() {
        return LocalDateTime.now(MANILA_TIMEZONE);
    }

    /**
     * Helper method to execute queries with parameters
     * @param sql The SQL query
     * @param params The parameters
     * @return List of tardiness records
     */
    @Override
    protected List<TardinessRecordModel> executeQuery(String sql, Object... params) {
        List<TardinessRecordModel> results = new ArrayList<>();
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            // Set parameters
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(mapResultSetToEntity(rs));
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error executing tardiness query: " + e.getMessage());
        }
        
        return results;
    }
}