package DAOs;

import Models.AttendanceModel;
import java.sql.*;
import java.time.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class AttendanceDAO {
    private static final ZoneId MANILA_TIMEZONE = ZoneId.of("Asia/Manila");
    private static final String DB_URL = "jdbc:mysql://localhost:3306/payrollsystem_db";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "";
    
    private DatabaseConnection databaseConnection;

    /**
     * Constructor with DatabaseConnection (for service compatibility)
     * @param databaseConnection The database connection to use
     */
    public AttendanceDAO(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }

    /**
     * Default constructor (maintains existing functionality)
     */
    public AttendanceDAO() {
        this.databaseConnection = null; // Use existing connection method
    }
    
    // SQL Queries
    private static final String INSERT_ATTENDANCE = 
        "INSERT INTO attendance (date, timeIn, timeOut, employeeId) VALUES (?, ?, ?, ?)";
    
    private static final String UPDATE_ATTENDANCE = 
        "UPDATE attendance SET date = ?, timeIn = ?, timeOut = ?, employeeId = ? WHERE attendanceId = ?";
    
    private static final String SELECT_BY_ID = 
        "SELECT attendanceId, date, timeIn, timeOut, employeeId FROM attendance WHERE attendanceId = ?";
    
    private static final String SELECT_BY_EMPLOYEE_DATE = 
        "SELECT attendanceId, date, timeIn, timeOut, employeeId FROM attendance WHERE employeeId = ? AND date = ?";
    
    private static final String SELECT_BY_EMPLOYEE_DATE_RANGE = 
        "SELECT attendanceId, date, timeIn, timeOut, employeeId FROM attendance " +
        "WHERE employeeId = ? AND date BETWEEN ? AND ? ORDER BY date DESC";
    
    private static final String SELECT_MONTHLY_ATTENDANCE = 
        "SELECT attendanceId, date, timeIn, timeOut, employeeId FROM attendance " +
        "WHERE employeeId = ? AND YEAR(date) = ? AND MONTH(date) = ? ORDER BY date";
    
    private static final String DELETE_ATTENDANCE = 
        "DELETE FROM attendance WHERE attendanceId = ?";
    
    private static final String SELECT_INCOMPLETE_ATTENDANCE = 
        "SELECT attendanceId, date, timeIn, timeOut, employeeId FROM attendance " +
        "WHERE employeeId = ? AND (timeIn IS NULL OR timeOut IS NULL) ORDER BY date DESC";
    
    private static final String SELECT_RECENT_ATTENDANCE = 
        "SELECT attendanceId, date, timeIn, timeOut, employeeId FROM attendance " +
        "WHERE employeeId = ? ORDER BY date DESC, attendanceId DESC LIMIT ?";
    
    private static final String SELECT_ATTENDANCE_SUMMARY = 
        "SELECT COUNT(*) as totalDays, " +
        "SUM(CASE WHEN timeIn IS NOT NULL AND timeOut IS NOT NULL THEN 1 ELSE 0 END) as completeDays, " +
        "SUM(CASE WHEN timeIn > '08:10:00' THEN 1 ELSE 0 END) as lateDays " +
        "FROM attendance WHERE employeeId = ? AND date BETWEEN ? AND ?";
    
    /**
     * Get database connection with Manila timezone
     */
    private Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        
        // Set connection timezone to Manila
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("SET time_zone = '+08:00'");
        }
        
        return conn;
    }
    
    /**
     * Create new attendance record with Manila timezone
     * @param attendance
     * @return 
     */
    public boolean createAttendance(AttendanceModel attendance) {
        if (attendance == null || !attendance.isValidAttendance()) {
            return false;
        }
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(INSERT_ATTENDANCE, Statement.RETURN_GENERATED_KEYS)) {
            
            // Convert to Manila timezone before saving
            Date manilaDate = convertToManilaDate(attendance.getDate());
            Time manilaTimeIn = convertToManilaTime(attendance.getTimeIn());
            Time manilaTimeOut = convertToManilaTime(attendance.getTimeOut());
            
            stmt.setDate(1, manilaDate);
            stmt.setTime(2, manilaTimeIn);
            stmt.setTime(3, manilaTimeOut);
            stmt.setInt(4, attendance.getEmployeeId());
            
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        attendance.setAttendanceId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
            
        } catch (SQLException e) {
            System.err.println("Error creating attendance: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Update existing attendance record
     * @param attendance
     * @return 
     */
    public boolean updateAttendance(AttendanceModel attendance) {
        if (attendance == null || attendance.getAttendanceId() <= 0) {
            return false;
        }
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(UPDATE_ATTENDANCE)) {
            
            // Convert to Manila timezone before saving
            Date manilaDate = convertToManilaDate(attendance.getDate());
            Time manilaTimeIn = convertToManilaTime(attendance.getTimeIn());
            Time manilaTimeOut = convertToManilaTime(attendance.getTimeOut());
            
            stmt.setDate(1, manilaDate);
            stmt.setTime(2, manilaTimeIn);
            stmt.setTime(3, manilaTimeOut);
            stmt.setInt(4, attendance.getEmployeeId());
            stmt.setInt(5, attendance.getAttendanceId());
            
            return stmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Error updating attendance: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Get attendance record by ID
     * @param attendanceId
     * @return 
     */
    public AttendanceModel getAttendanceById(int attendanceId) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_ID)) {
            
            stmt.setInt(1, attendanceId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToAttendance(rs);
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting attendance by ID: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Get attendance record by employee and date
     * @param employeeId
     * @param date
     * @return 
     */
    public AttendanceModel getAttendanceByEmployeeAndDate(int employeeId, Date date) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_EMPLOYEE_DATE)) {
            
            stmt.setInt(1, employeeId);
            stmt.setDate(2, convertToManilaDate(date));
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToAttendance(rs);
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting attendance by employee and date: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Get attendance records by employee and date range
     * @param employeeId
     * @param startDate
     * @param endDate
     * @return 
     */
    public List<AttendanceModel> getAttendanceByEmployeeAndDateRange(int employeeId, Date startDate, Date endDate) {
        List<AttendanceModel> attendanceList = new ArrayList<>();
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_EMPLOYEE_DATE_RANGE)) {
            
            stmt.setInt(1, employeeId);
            stmt.setDate(2, convertToManilaDate(startDate));
            stmt.setDate(3, convertToManilaDate(endDate));
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    attendanceList.add(mapResultSetToAttendance(rs));
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting attendance by date range: " + e.getMessage());
        }
        
        return attendanceList;
    }
    
    /**
     * Get monthly attendance for employee
     * @param employeeId
     * @param month
     * @param year
     * @return 
     */
    public List<AttendanceModel> getMonthlyAttendance(int employeeId, int month, int year) {
        List<AttendanceModel> attendanceList = new ArrayList<>();
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_MONTHLY_ATTENDANCE)) {
            
            stmt.setInt(1, employeeId);
            stmt.setInt(2, year);
            stmt.setInt(3, month);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    attendanceList.add(mapResultSetToAttendance(rs));
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting monthly attendance: " + e.getMessage());
        }
        
        return attendanceList;
    }
    
    /**
     * Get incomplete attendance records (missing time in/out)
     * @param employeeId
     * @return 
     */
    public List<AttendanceModel> getIncompleteAttendance(int employeeId) {
        List<AttendanceModel> attendanceList = new ArrayList<>();
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_INCOMPLETE_ATTENDANCE)) {
            
            stmt.setInt(1, employeeId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    attendanceList.add(mapResultSetToAttendance(rs));
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting incomplete attendance: " + e.getMessage());
        }
        
        return attendanceList;
    }
    
    /**
     * Get recent attendance records for employee
     * @param employeeId
     * @param limit
     * @return 
     */
    public List<AttendanceModel> getRecentAttendance(int employeeId, int limit) {
        List<AttendanceModel> attendanceList = new ArrayList<>();
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_RECENT_ATTENDANCE)) {
            
            stmt.setInt(1, employeeId);
            stmt.setInt(2, limit);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    attendanceList.add(mapResultSetToAttendance(rs));
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting recent attendance: " + e.getMessage());
        }
        
        return attendanceList;
    }
    
    /**
     * Get attendance summary for date range
     * @param employeeId
     * @param startDate
     * @param endDate
     * @return 
     */
    public Map<String, Object> getAttendanceSummary(int employeeId, Date startDate, Date endDate) {
        Map<String, Object> summary = new HashMap<>();
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_ATTENDANCE_SUMMARY)) {
            
            stmt.setInt(1, employeeId);
            stmt.setDate(2, convertToManilaDate(startDate));
            stmt.setDate(3, convertToManilaDate(endDate));
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    summary.put("totalDays", rs.getInt("totalDays"));
                    summary.put("completeDays", rs.getInt("completeDays"));
                    summary.put("lateDays", rs.getInt("lateDays"));
                    summary.put("presentDays", rs.getInt("completeDays"));
                    summary.put("attendanceRate", rs.getInt("completeDays") > 0 ? 
                        (double) rs.getInt("completeDays") / rs.getInt("totalDays") * 100 : 0);
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting attendance summary: " + e.getMessage());
        }
        
        return summary;
    }
    
    /**
     * Delete attendance record
     * @param attendanceId
     * @return 
     */
    public boolean deleteAttendance(int attendanceId) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(DELETE_ATTENDANCE)) {
            
            stmt.setInt(1, attendanceId);
            return stmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Error deleting attendance: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Clock in operation with Manila timezone
     * @param employeeId
     * @return 
     */
    public boolean clockIn(int employeeId) {
        LocalDateTime nowManila = LocalDateTime.now(MANILA_TIMEZONE);
        Date currentDate = Date.valueOf(nowManila.toLocalDate());
        Time currentTime = Time.valueOf(nowManila.toLocalTime());
        
        // Check if employee already clocked in today
        AttendanceModel existing = getAttendanceByEmployeeAndDate(employeeId, currentDate);
        
        if (existing != null && existing.hasTimeIn()) {
            return false; // Already clocked in
        }
        
        if (existing != null) {
            // Update existing record with time in
            existing.setTimeIn(currentTime);
            return updateAttendance(existing);
        } else {
            // Create new attendance record
            AttendanceModel attendance = new AttendanceModel(currentDate, currentTime, null, employeeId);
            return createAttendance(attendance);
        }
    }
    
    /**
     * Clock out operation with Manila timezone
     * @param employeeId
     * @return 
     */
    public boolean clockOut(int employeeId) {
        LocalDateTime nowManila = LocalDateTime.now(MANILA_TIMEZONE);
        Date currentDate = Date.valueOf(nowManila.toLocalDate());
        Time currentTime = Time.valueOf(nowManila.toLocalTime());
        
        // Get existing attendance record for today
        AttendanceModel attendance = getAttendanceByEmployeeAndDate(employeeId, currentDate);
        
        if (attendance == null || !attendance.hasTimeIn()) {
            return false; // No time in recorded
        }
        
        if (attendance.hasTimeOut()) {
            return false; // Already clocked out
        }
        
        // Update with time out
        attendance.setTimeOut(currentTime);
        return updateAttendance(attendance);
    }
    
    /**
     * Get today's attendance for employee
     * @param employeeId
     * @return 
     */
    public AttendanceModel getTodayAttendance(int employeeId) {
        LocalDate today = LocalDate.now(MANILA_TIMEZONE);
        Date todayDate = Date.valueOf(today);
        return getAttendanceByEmployeeAndDate(employeeId, todayDate);
    }
    
    /**
     * Check if employee can clock in
     * @param employeeId
     * @return 
     */
    public boolean canClockIn(int employeeId) {
        AttendanceModel todayAttendance = getTodayAttendance(employeeId);
        return todayAttendance == null || !todayAttendance.hasTimeIn();
    }
    
    /**
     * Check if employee can clock out
     * @param employeeId
     * @return 
     */
    public boolean canClockOut(int employeeId) {
        AttendanceModel todayAttendance = getTodayAttendance(employeeId);
        return todayAttendance != null && todayAttendance.hasTimeIn() && !todayAttendance.hasTimeOut();
    }
    
    /**
     * Get current Manila time
     * @return 
     */
    public LocalDateTime getCurrentManilaTime() {
        return LocalDateTime.now(MANILA_TIMEZONE);
    }
    
    /**
     * Get attendance records requiring review (late/incomplete)
     * @param date
     * @return 
     */
    public List<AttendanceModel> getAttendanceRequiringReview(Date date) {
        String query = "SELECT attendanceId, date, timeIn, timeOut, employeeId FROM attendance " +
                      "WHERE date = ? AND (timeIn > '08:10:00' OR timeOut IS NULL)";
        
        List<AttendanceModel> attendanceList = new ArrayList<>();
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setDate(1, convertToManilaDate(date));
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    attendanceList.add(mapResultSetToAttendance(rs));
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting attendance requiring review: " + e.getMessage());
        }
        
        return attendanceList;
    }
    
    /**
     * Bulk update attendance records
     * @param attendanceList
     * @return 
     */
    public boolean bulkUpdateAttendance(List<AttendanceModel> attendanceList) {
        if (attendanceList == null || attendanceList.isEmpty()) {
            return false;
        }
        
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            
            try (PreparedStatement stmt = conn.prepareStatement(UPDATE_ATTENDANCE)) {
                for (AttendanceModel attendance : attendanceList) {
                    if (attendance.getAttendanceId() <= 0) continue;
                    
                    Date manilaDate = convertToManilaDate(attendance.getDate());
                    Time manilaTimeIn = convertToManilaTime(attendance.getTimeIn());
                    Time manilaTimeOut = convertToManilaTime(attendance.getTimeOut());
                    
                    stmt.setDate(1, manilaDate);
                    stmt.setTime(2, manilaTimeIn);
                    stmt.setTime(3, manilaTimeOut);
                    stmt.setInt(4, attendance.getEmployeeId());
                    stmt.setInt(5, attendance.getAttendanceId());
                    
                    stmt.addBatch();
                }
                
                int[] results = stmt.executeBatch();
                conn.commit();
                
                // Check if all updates were successful
                for (int result : results) {
                    if (result <= 0) {
                        return false;
                    }
                }
                
                return true;
                
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
            
        } catch (SQLException e) {
            System.err.println("Error bulk updating attendance: " + e.getMessage());
        }
        
        return false;
    }
    /**
 * Get attendance records for all employees on a specific date
 * Used by AttendanceService for daily attendance reports
 * @param date Date to get attendance for
 * @return List of attendance records for all employees on that date
 */
public List<AttendanceModel> getAttendanceByDate(Date date) {
    List<AttendanceModel> attendanceList = new ArrayList<>();
    String sql = "SELECT attendanceId, date, timeIn, timeOut, employeeId FROM attendance " +
                 "WHERE date = ? ORDER BY employeeId";
    
    try (Connection conn = getConnection();  // Use your existing getConnection() method
         PreparedStatement stmt = conn.prepareStatement(sql)) {
        
        stmt.setDate(1, convertToManilaDate(date));
        
        try (ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                AttendanceModel attendance = mapResultSetToAttendance(rs);
                if (attendance != null) {
                    attendanceList.add(attendance);
                }
            }
        }
        
    } catch (SQLException e) {
        System.err.println("Error getting attendance by date: " + e.getMessage());
    }
    
    return attendanceList;
}
    
    // Helper methods
    
    /**
     * Map ResultSet to AttendanceModel
     */
    private AttendanceModel mapResultSetToAttendance(ResultSet rs) throws SQLException {
        AttendanceModel attendance = new AttendanceModel();
        
        attendance.setAttendanceId(rs.getInt("attendanceId"));
        attendance.setDate(rs.getDate("date"));
        attendance.setTimeIn(rs.getTime("timeIn"));
        attendance.setTimeOut(rs.getTime("timeOut"));
        attendance.setEmployeeId(rs.getInt("employeeId"));
        
        return attendance;
    }
    
    /**
     * Convert Date to Manila timezone
     */
    private Date convertToManilaDate(Date date) {
        if (date == null) return null;
        
        // Convert to Manila timezone date
        LocalDate localDate = date.toLocalDate();
        ZonedDateTime manilaDateTime = localDate.atStartOfDay(MANILA_TIMEZONE);
        return Date.valueOf(manilaDateTime.toLocalDate());
    }
    
    /**
     * Convert Time to Manila timezone
     */
    private Time convertToManilaTime(Time time) {
        if (time == null) return null;
        
        // For time-only values, we assume they're already in Manila timezone
        // In a real implementation, you might need more sophisticated conversion
        return time;
    }
    
    /**
     * Get working days count in date range
     * @param startDate
     * @param endDate
     * @return 
     */
    public int getWorkingDaysCount(Date startDate, Date endDate) {
        LocalDate start = startDate.toLocalDate();
        LocalDate end = endDate.toLocalDate();
        int workingDays = 0;
        
        LocalDate current = start;
        while (!current.isAfter(end)) {
            DayOfWeek dayOfWeek = current.getDayOfWeek();
            if (dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY) {
                workingDays++;
            }
            current = current.plusDays(1);
        }
        
        return workingDays;
    }
}