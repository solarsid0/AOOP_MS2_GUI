package Services;

import Models.AttendanceModel;
import Models.TardinessRecordModel;
import DAOs.AttendanceDAO;
import DAOs.TardinessRecordDAO;
import DAOs.EmployeeDAO;
import DAOs.DatabaseConnection;
import java.time.YearMonth;
import java.math.BigDecimal;

import java.sql.*;
import java.time.*;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

/**
 * AttendanceService with TardinessRecordDAO integration
 * Includes Manila timezone operations, BigDecimal precision, and comprehensive business logic
 */
public class AttendanceService {
    private static final ZoneId MANILA_TIMEZONE = ZoneId.of("Asia/Manila");
    private static final LocalTime GRACE_PERIOD_CUTOFF = LocalTime.of(8, 10);  // 8:10 AM grace period
    private static final LocalTime STANDARD_START_TIME = LocalTime.of(8, 0);   // 8:00 AM standard start
    private static final LocalTime STANDARD_END_TIME = LocalTime.of(17, 0);    // 5:00 PM standard end
    
    private AttendanceDAO attendanceDAO;
    private TardinessRecordDAO tardinessRecordDAO;
    private EmployeeDAO employeeDAO;
    private DatabaseConnection databaseConnection;
    
    // Constructors
    public AttendanceService() {
        this.databaseConnection = new DatabaseConnection();
        this.attendanceDAO = new AttendanceDAO();
        this.tardinessRecordDAO = new TardinessRecordDAO(this.databaseConnection);
        this.employeeDAO = new EmployeeDAO(this.databaseConnection);
    }
    
    /**
     * Constructor with DatabaseConnection for service compatibility
     * @param databaseConnection
     */
    public AttendanceService(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
        this.attendanceDAO = new AttendanceDAO(databaseConnection);
        this.tardinessRecordDAO = new TardinessRecordDAO(databaseConnection);
        this.employeeDAO = new EmployeeDAO(databaseConnection);
    }
    
    public AttendanceService(AttendanceDAO attendanceDAO, TardinessRecordDAO tardinessRecordDAO) {
        this.attendanceDAO = attendanceDAO;
        this.tardinessRecordDAO = tardinessRecordDAO;
        this.databaseConnection = new DatabaseConnection();
        this.employeeDAO = new EmployeeDAO(this.databaseConnection);
    }
    
    // Core attendance operations with enhanced grace period logic
    
    /**
     * Record time in for employee with 8:10 AM grace period logic
     * UPDATED: Enhanced tardiness record creation with proper error handling
     * @param employeeId
     * @return 
     */
    public boolean recordTimeIn(int employeeId) {
        try {
            Date currentDate = AttendanceModel.getCurrentDateInManila();
            Time currentTime = AttendanceModel.getCurrentTimeInManila();
            
            // Check if employee already has attendance for today
            AttendanceModel existing = attendanceDAO.getAttendanceByEmployeeAndDate(employeeId, currentDate);
            if (existing != null && existing.hasTimeIn()) {
                System.out.println("Employee " + employeeId + " already timed in today");
                return false; // Already timed in
            }
            
            AttendanceModel attendance;
            if (existing != null) {
                // Update existing record with time in
                existing.setTimeIn(currentTime);
                attendance = existing;
            } else {
                // Create new attendance record
                attendance = new AttendanceModel(currentDate, currentTime, null, employeeId);
            }
            
            // Save attendance
            boolean success = existing != null ? 
                attendanceDAO.updateAttendance(attendance) : 
                attendanceDAO.createAttendance(attendance);
            
            if (success) {
                // Ensure attendance ID is set for tardiness record creation
                if (attendance.getAttendanceId() == null && existing == null) {
                    // Fetch the created attendance to get the ID
                    AttendanceModel created = attendanceDAO.getAttendanceByEmployeeAndDate(employeeId, currentDate);
                    if (created != null) {
                        attendance.setAttendanceId(created.getAttendanceId());
                    }
                }
                
                // Create tardiness record only if beyond grace period (8:10 AM)
                if (attendance.isLateAttendance() && attendance.getAttendanceId() != null) {
                    boolean tardinessCreated = createTardinessRecord(attendance, TardinessRecordModel.TardinessType.LATE);
                    if (tardinessCreated) {
                        System.out.println("Late attendance recorded for employee " + employeeId + 
                                         " at " + currentTime + " (beyond 8:10 AM grace period)");
                    } else {
                        System.err.println("Failed to create tardiness record for employee " + employeeId);
                    }
                } else if (attendance.isWithinGracePeriod()) {
                    System.out.println("Employee " + employeeId + " timed in within grace period at " + currentTime);
                } else {
                    System.out.println("Employee " + employeeId + " timed in on time at " + currentTime);
                }
            }
            
            return success;
        } catch (Exception e) {
            System.err.println("Error recording time in: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Record time out for employee with undertime detection
     * UPDATED: Enhanced tardiness record creation with proper error handling
     */
    public boolean recordTimeOut(int employeeId) {
        try {
            Date currentDate = AttendanceModel.getCurrentDateInManila();
            Time currentTime = AttendanceModel.getCurrentTimeInManila();
            
            // Get existing attendance for today
            AttendanceModel attendance = attendanceDAO.getAttendanceByEmployeeAndDate(employeeId, currentDate);
            if (attendance == null || !attendance.hasTimeIn()) {
                System.out.println("No time in record found for employee " + employeeId + " today");
                return false; // No time in recorded
            }
            
            if (attendance.hasTimeOut()) {
                System.out.println("Employee " + employeeId + " already timed out today");
                return false; // Already timed out
            }
            
            // Update with time out
            attendance.setTimeOut(currentTime);
            
            boolean success = attendanceDAO.updateAttendance(attendance);
            
            if (success) {
                // Create tardiness record for early departure (undertime)
                if (attendance.isEarlyOut() && attendance.getAttendanceId() != null) {
                    boolean tardinessCreated = createTardinessRecord(attendance, TardinessRecordModel.TardinessType.UNDERTIME);
                    if (tardinessCreated) {
                        System.out.println("Early departure recorded for employee " + employeeId + 
                                         " at " + currentTime + " (before 5:00 PM)");
                    } else {
                        System.err.println("Failed to create undertime tardiness record for employee " + employeeId);
                    }
                } else {
                    System.out.println("Employee " + employeeId + " timed out at " + currentTime);
                }
            }
            
            return success;
        } catch (Exception e) {
            System.err.println("Error recording time out: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Manual attendance entry for HR/supervisors with validation
     * UPDATED: Enhanced validation and tardiness record creation
     */
    public boolean createManualAttendance(int employeeId, Date date, Time timeIn, Time timeOut, String reason) {
        try {
            // Validate inputs
            if (employeeId <= 0 || date == null || timeIn == null) {
                System.err.println("Invalid parameters for manual attendance");
                return false;
            }
            
            if (timeOut != null && timeOut.before(timeIn)) {
                System.err.println("Time out cannot be before time in");
                return false;
            }
            
            // Check if attendance already exists
            AttendanceModel existing = attendanceDAO.getAttendanceByEmployeeAndDate(employeeId, date);
            if (existing != null) {
                System.err.println("Attendance already exists for employee " + employeeId + " on " + date);
                return false; // Attendance already exists
            }
            
            AttendanceModel attendance = new AttendanceModel(date, timeIn, timeOut, employeeId);
            
            boolean success = attendanceDAO.createAttendance(attendance);
            
            if (success) {
                // Get the created attendance to ensure we have the ID
                AttendanceModel created = attendanceDAO.getAttendanceByEmployeeAndDate(employeeId, date);
                if (created != null) {
                    attendance.setAttendanceId(created.getAttendanceId());
                    
                    // Create tardiness records if applicable
                    if (attendance.isLateAttendance()) {
                        createTardinessRecord(attendance, TardinessRecordModel.TardinessType.LATE);
                    }
                    if (attendance.isEarlyOut()) {
                        createTardinessRecord(attendance, TardinessRecordModel.TardinessType.UNDERTIME);
                    }
                }
                
                System.out.println("Manual attendance created for employee " + employeeId + 
                                 " on " + date + " - Reason: " + reason);
            }
            
            return success;
        } catch (Exception e) {
            System.err.println("Error creating manual attendance: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Update existing attendance record with tardiness recalculation
     * UPDATED: Enhanced tardiness record updates
     */
    public boolean updateAttendance(AttendanceModel attendance) {
        try {
            if (attendance == null || !attendance.isValidAttendance()) {
                System.err.println("Invalid attendance record provided for update");
                return false;
            }
            
            boolean success = attendanceDAO.updateAttendance(attendance);
            
            if (success) {
                // Update tardiness records
                updateTardinessRecords(attendance);
                System.out.println("Attendance updated for employee " + attendance.getEmployeeId() + 
                                 " on " + attendance.getDate());
            }
            
            return success;
        } catch (Exception e) {
            System.err.println("Error updating attendance: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Get attendance records with calculated hours from payslip view logic.
     * Applies business rules for rank-and-file vs salaried employees
     */
    public List<AttendanceModel> getAttendanceRecordsWithCalculatedHours(int employeeId, String dateFilter) {
        List<AttendanceModel> records = new ArrayList<>();
        
        // Get employee's rank-and-file status
        boolean isRankAndFile = false;
        try {
            isRankAndFile = employeeDAO.isEmployeeRankAndFile(employeeId);
        } catch (Exception e) {
            System.err.println("Error checking employee rank: " + e.getMessage());
        }
        
        String sql = """
            SELECT 
                a.attendanceId,
                a.employeeId,
                a.date,
                a.timeIn,
                a.timeOut,
                -- Hours worked based on employee type
                CASE 
                    WHEN ? = 1 THEN  -- Rank-and-file employee
                        CASE 
                            WHEN a.timeIn IS NOT NULL AND a.timeOut IS NOT NULL THEN
                                GREATEST(0, 
                                    (TIME_TO_SEC(LEAST(a.timeOut, '17:00:00')) - TIME_TO_SEC(GREATEST(a.timeIn, '08:00:00'))) / 3600.0
                                    - CASE 
                                        WHEN GREATEST(a.timeIn, '08:00:00') <= '12:00:00' AND LEAST(a.timeOut, '17:00:00') >= '13:00:00' 
                                        THEN 1.0 
                                        ELSE 0.0 
                                      END
                                )
                            ELSE 0.00 
                        END
                    ELSE  -- Non rank-and-file employee: always 8 hours if present
                        CASE WHEN a.timeIn IS NOT NULL THEN 8.00 ELSE 0.00 END
                END AS hours_worked,
                -- Late hours: only for rank-and-file employees
                CASE 
                    WHEN ? = 1 AND a.timeIn > '08:10:00'
                    THEN (TIME_TO_SEC(a.timeIn) - TIME_TO_SEC('08:00:00')) / 3600.0
                    ELSE 0.00 
                END AS late_hours,
                -- Overtime hours: only for rank-and-file employees
                CASE 
                    WHEN ? = 1 AND a.timeIn IS NOT NULL AND a.timeOut IS NOT NULL THEN
                        CASE WHEN a.timeIn < '08:00:00' 
                             THEN (TIME_TO_SEC('08:00:00') - TIME_TO_SEC(a.timeIn)) / 3600.0 
                             ELSE 0 END
                        +
                        CASE WHEN a.timeOut > '17:00:00' 
                             THEN (TIME_TO_SEC(a.timeOut) - TIME_TO_SEC('17:00:00')) / 3600.0 
                             ELSE 0 END
                    ELSE 0.00 
                END AS overtime_hours
            FROM attendance a
            WHERE a.employeeId = ?
        """;
        
        // Apply date filtering
        if (dateFilter != null && !dateFilter.equals("All")) {
            if (dateFilter.length() == 4) { // Year format
                sql += " AND YEAR(a.date) = ?";
            } else { // Month format
                sql += " AND DATE_FORMAT(a.date, '%Y-%m') = ?";
            }
        } else {
            // Default to last 3 months if no filter
            sql += " AND a.date >= DATE_SUB(CURDATE(), INTERVAL 90 DAY)";
        }
        
        sql += " ORDER BY a.date DESC LIMIT 1000";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, isRankAndFile ? 1 : 0);  // hours_worked logic
            stmt.setInt(2, isRankAndFile ? 1 : 0);  // late_hours logic
            stmt.setInt(3, isRankAndFile ? 1 : 0);  // overtime_hours logic
            stmt.setInt(4, employeeId);
            
            if (dateFilter != null && !dateFilter.equals("All")) {
                if (dateFilter.length() == 4) {
                    stmt.setInt(5, Integer.parseInt(dateFilter));
                } else {
                    stmt.setString(5, dateFilter);
                }
            }
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    AttendanceModel attendance = new AttendanceModel();
                    attendance.setAttendanceId(rs.getInt("attendanceId"));
                    attendance.setEmployeeId(rs.getInt("employeeId"));
                    
                    Date sqlDate = rs.getDate("date");
                    Time sqlTimeIn = rs.getTime("timeIn");
                    Time sqlTimeOut = rs.getTime("timeOut");
                    double hoursWorked = rs.getDouble("hours_worked");
                    double lateHours = rs.getDouble("late_hours");
                    double overtimeHours = rs.getDouble("overtime_hours");
                    
                    // Set attendance data with database-calculated values
                    attendance.setAttendanceDataWithCalculatedValues(
                        sqlDate, sqlTimeIn, sqlTimeOut, hoursWorked, lateHours, overtimeHours);
                    
                    records.add(attendance);
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error fetching attendance records: " + e.getMessage());
            e.printStackTrace();
        }
        
        return records;
    }
    
    // Grace period and rank-and-file specific logic
    
    /**
     * Apply rank-and-file rules for attendance calculation
     */
    public AttendanceModel applyRankAndFileRules(AttendanceModel attendance, boolean isRankAndFile) {
        if (attendance == null || !isRankAndFile) {
            return attendance;
        }
        
        // For rank-and-file employees, deduct late hours from total worked hours
        if (attendance.isLateAttendance() && attendance.getLateHours() > 0) {
            double adjustedHours = Math.max(0, attendance.getComputedHours() - attendance.getLateHours());
            attendance.setComputedHours(adjustedHours);
            
            System.out.println("Rank-and-file adjustment applied: " + attendance.getLateHours() + 
                             " late hours deducted. Adjusted hours: " + adjustedHours);
        }
        
        return attendance;
    }
    
    /**
     * Check if employee is within 8:10 AM grace period
     */
    public boolean isWithinGracePeriod(Time timeIn) {
        if (timeIn == null) return false;
        
        LocalTime actualTimeIn = timeIn.toLocalTime();
        return actualTimeIn.isAfter(STANDARD_START_TIME) && 
               !actualTimeIn.isAfter(GRACE_PERIOD_CUTOFF);
    }
    
    /**
     * Calculate effective work hours considering grace period and rank-and-file rules
     */
    public double calculateEffectiveWorkHours(AttendanceModel attendance, boolean isRankAndFile) {
        if (attendance == null || !attendance.isCompleteAttendance()) {
            return 0.0;
        }
        
        double baseHours = attendance.getComputedHours();
        
        if (isRankAndFile && attendance.isLateAttendance()) {
            // Deduct late hours for rank-and-file employees
            baseHours = Math.max(0, baseHours - attendance.getLateHours());
        }
        
        return baseHours;
    }
    
    /**
     * Check if employee is rank-and-file based on position/department
     */
    public boolean isRankAndFileEmployee(int employeeId) {
        try {
            return employeeDAO.isEmployeeRankAndFile(employeeId);
        } catch (Exception e) {
            System.err.println("Error checking rank-and-file status: " + e.getMessage());
            return false;
        }
    }
    
    // Query methods
    
    public AttendanceModel getAttendanceByEmployeeAndDate(int employeeId, Date date) {
        try {
            return attendanceDAO.getAttendanceByEmployeeAndDate(employeeId, date);
        } catch (Exception e) {
            System.err.println("Error getting attendance: " + e.getMessage());
            return null;
        }
    }
    
    public List<AttendanceModel> getAttendanceByEmployeeAndDateRange(int employeeId, Date startDate, Date endDate) {
        try {
            return attendanceDAO.getAttendanceByEmployeeAndDateRange(employeeId, startDate, endDate);
        } catch (Exception e) {
            System.err.println("Error getting attendance range: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Get enhanced monthly attendance summary for employee with rank-and-file considerations
     */
    public Map<String, Object> getMonthlyAttendanceSummary(int employeeId, int month, int year) {
        try {
            Date startDate = Date.valueOf(LocalDate.of(year, month, 1));
            Date endDate = Date.valueOf(LocalDate.of(year, month, 1).withDayOfMonth(
                LocalDate.of(year, month, 1).lengthOfMonth()));
            
            List<AttendanceModel> attendanceList = getAttendanceByEmployeeAndDateRange(employeeId, startDate, endDate);
            boolean isRankAndFile = isRankAndFileEmployee(employeeId);
            
            Map<String, Object> summary = new HashMap<>();
            
            if (attendanceList != null) {
                int totalDays = attendanceList.size();
                int lateDays = 0;
                int withinGracePeriodDays = 0;
                double totalHours = 0.0;
                double totalEffectiveHours = 0.0;
                double totalOvertimeHours = 0.0;
                double totalLateHours = 0.0;
                
                for (AttendanceModel attendance : attendanceList) {
                    if (attendance.isCompleteAttendance()) {
                        totalHours += attendance.getComputedHours();
                        totalOvertimeHours += attendance.getOvertimeHours();
                        
                        // Calculate effective hours with rank-and-file rules
                        double effectiveHours = calculateEffectiveWorkHours(attendance, isRankAndFile);
                        totalEffectiveHours += effectiveHours;
                    }
                    
                    if (attendance.isLateAttendance()) {
                        lateDays++;
                        totalLateHours += attendance.getLateHours();
                    } else if (attendance.isWithinGracePeriod()) {
                        withinGracePeriodDays++;
                    }
                }
                
                summary.put("totalDays", totalDays);
                summary.put("lateDays", lateDays);
                summary.put("withinGracePeriodDays", withinGracePeriodDays);
                summary.put("totalHours", totalHours);
                summary.put("totalEffectiveHours", totalEffectiveHours);
                summary.put("totalOvertimeHours", totalOvertimeHours);
                summary.put("totalLateHours", totalLateHours);
                summary.put("attendanceRate", totalDays > 0 ? (double) (totalDays - lateDays) / totalDays * 100 : 0);
                summary.put("punctualityRate", totalDays > 0 ? (double) (totalDays - lateDays + withinGracePeriodDays) / totalDays * 100 : 0);
                summary.put("isRankAndFile", isRankAndFile);
                summary.put("workingDaysInMonth", getWorkingDaysInMonth(month, year));
            }
            
            return summary;
        } catch (Exception e) {
            System.err.println("Error getting monthly summary: " + e.getMessage());
            return new HashMap<>();
        }
    }
    
    public AttendanceModel getTodayAttendance(int employeeId) {
        Date today = AttendanceModel.getCurrentDateInManila();
        return getAttendanceByEmployeeAndDate(employeeId, today);
    }
    
    public Map<String, Object> getTodayAttendanceStatus(int employeeId) {
        Map<String, Object> status = new HashMap<>();
        AttendanceModel todayAttendance = getTodayAttendance(employeeId);
        
        if (todayAttendance == null) {
            status.put("hasAttendance", false);
            status.put("canTimeIn", true);
            status.put("canTimeOut", false);
        } else {
            status.put("hasAttendance", true);
            status.put("canTimeIn", !todayAttendance.hasTimeIn());
            status.put("canTimeOut", todayAttendance.hasTimeIn() && !todayAttendance.hasTimeOut());
            status.put("timeIn", todayAttendance.getFormattedTimeIn());
            status.put("timeOut", todayAttendance.getFormattedTimeOut());
            status.put("isLate", todayAttendance.isLateAttendance());
            status.put("isWithinGracePeriod", todayAttendance.isWithinGracePeriod());
            status.put("computedHours", todayAttendance.getComputedHours());
            status.put("lateHours", todayAttendance.getLateHours());
            status.put("overtimeHours", todayAttendance.getOvertimeHours());
        }
        
        return status;
    }
    
    // Validation methods
    
    public boolean canTimeIn(int employeeId) {
        try {
            AttendanceModel todayAttendance = getTodayAttendance(employeeId);
            return todayAttendance == null || !todayAttendance.hasTimeIn();
        } catch (Exception e) {
            return false;
        }
    }
    
    public boolean canTimeOut(int employeeId) {
        try {
            AttendanceModel todayAttendance = getTodayAttendance(employeeId);
            return todayAttendance != null && todayAttendance.hasTimeIn() && !todayAttendance.hasTimeOut();
        } catch (Exception e) {
            return false;
        }
    }
    
    public boolean isAppropriateTimeForTimeIn() {
        LocalTime currentTime = LocalTime.now(MANILA_TIMEZONE);
        LocalTime earliestTime = LocalTime.of(6, 0);  // 6:00 AM earliest
        LocalTime latestTime = LocalTime.of(12, 0);   // 12:00 PM latest for morning time in
        
        return !currentTime.isBefore(earliestTime) && !currentTime.isAfter(latestTime);
    }
    
    public boolean isAppropriateTimeForTimeOut() {
        LocalTime currentTime = LocalTime.now(MANILA_TIMEZONE);
        LocalTime earliestTime = LocalTime.of(12, 0);  // 12:00 PM earliest for time out
        
        return !currentTime.isBefore(earliestTime);
    }
    
    // UPDATED: Enhanced tardiness management with proper TardinessRecordDAO integration
    
    /**
     * UPDATED: Create tardiness record for attendance using factory methods
     */
    private boolean createTardinessRecord(AttendanceModel attendance, TardinessRecordModel.TardinessType type) {
        try {
            if (attendance == null || attendance.getAttendanceId() == null) {
                System.err.println("Cannot create tardiness record: Invalid attendance or missing ID");
                return false;
            }
            
            TardinessRecordModel tardinessRecord = null;
            
            if (type == TardinessRecordModel.TardinessType.LATE) {
                tardinessRecord = TardinessRecordModel.createLateRecord(attendance);
            } else if (type == TardinessRecordModel.TardinessType.UNDERTIME) {
                tardinessRecord = TardinessRecordModel.createUndertimeRecord(attendance);
            }
            
            if (tardinessRecord != null && tardinessRecord.isValidRecord()) {
                boolean success = tardinessRecordDAO.createTardinessRecord(tardinessRecord);
                if (success) {
                    System.out.println("Tardiness record created: " + tardinessRecord.getTardinessDescription());
                    return true;
                } else {
                    System.err.println("Failed to create tardiness record for attendance " + attendance.getAttendanceId());
                    return false;
                }
            } else {
                System.err.println("Invalid tardiness record created for attendance " + attendance.getAttendanceId());
                return false;
            }
        } catch (Exception e) {
            System.err.println("Error creating tardiness record: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * UPDATED: Update tardiness records for attendance with enhanced error handling
     */
    private void updateTardinessRecords(AttendanceModel attendance) {
        try {
            if (attendance == null || attendance.getAttendanceId() == null) {
                System.err.println("Cannot update tardiness records: Invalid attendance or missing ID");
                return;
            }
            
            // Delete existing tardiness records for this attendance
            boolean deleteSuccess = tardinessRecordDAO.deleteTardinessRecordsByAttendance(attendance.getAttendanceId());
            
            if (deleteSuccess) {
                // Create new tardiness records if applicable
                if (attendance.isLateAttendance()) {
                    createTardinessRecord(attendance, TardinessRecordModel.TardinessType.LATE);
                }
                if (attendance.isEarlyOut()) {
                    createTardinessRecord(attendance, TardinessRecordModel.TardinessType.UNDERTIME);
                }
            } else {
                System.err.println("Failed to delete existing tardiness records for attendance " + attendance.getAttendanceId());
            }
        } catch (Exception e) {
            System.err.println("Error updating tardiness records: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * NEW: Get tardiness records for an employee and pay period
     */
    public List<TardinessRecordModel> getTardinessRecords(int employeeId, int payPeriodId) {
        try {
            return tardinessRecordDAO.findByEmployeeAndPeriod(employeeId, payPeriodId);
        } catch (Exception e) {
            System.err.println("Error getting tardiness records: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * NEW: Get tardiness statistics for an employee
     */
    public TardinessRecordDAO.TardinessStatistics getTardinessStatistics(int employeeId, LocalDate startDate, LocalDate endDate) {
        try {
            return tardinessRecordDAO.getTardinessStatistics(employeeId, startDate, endDate);
        } catch (Exception e) {
            System.err.println("Error getting tardiness statistics: " + e.getMessage());
            return new TardinessRecordDAO.TardinessStatistics();
        }
    }
    
    /**
     * NEW: Get tardiness record by ID (for supervisor review)
     */
    public TardinessRecordModel getTardinessRecord(int tardinessRecordId) {
        try {
            return tardinessRecordDAO.findById(tardinessRecordId);
        } catch (Exception e) {
            System.err.println("Error getting tardiness record: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * NEW: Update tardiness record (for supervisor notes)
     */
    public boolean updateTardinessRecord(TardinessRecordModel tardinessRecord) {
        try {
            return tardinessRecordDAO.updateTardinessRecord(tardinessRecord);
        } catch (Exception e) {
            System.err.println("Error updating tardiness record: " + e.getMessage());
            return false;
        }
    }
    
    // Utility methods
    
    public LocalDateTime getCurrentManilaTime() {
        return LocalDateTime.now(MANILA_TIMEZONE);
    }
    
    public boolean isWithinWorkHours() {
        LocalTime currentTime = LocalTime.now(MANILA_TIMEZONE);
        return !currentTime.isBefore(STANDARD_START_TIME) && !currentTime.isAfter(STANDARD_END_TIME);
    }
    
    public int getWorkingDaysInMonth(int month, int year) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        
        int workingDays = 0;
        LocalDate current = startDate;
        
        while (!current.isAfter(endDate)) {
            DayOfWeek dayOfWeek = current.getDayOfWeek();
            if (dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY) {
                workingDays++;
            }
            current = current.plusDays(1);
        }
        
        return workingDays;
    }
    
    public Map<String, Object> getGracePeriodInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("standardStartTime", STANDARD_START_TIME.toString());
        info.put("gracePeriodCutoff", GRACE_PERIOD_CUTOFF.toString());
        info.put("standardEndTime", STANDARD_END_TIME.toString());
        info.put("gracePeriodMinutes", 10);
        return info;
    }
    
    public double calculateAttendanceComplianceRate(int employeeId, Date startDate, Date endDate) {
        try {
            List<AttendanceModel> attendanceList = getAttendanceByEmployeeAndDateRange(employeeId, startDate, endDate);
            int workingDays = attendanceDAO.getWorkingDaysCount(startDate, endDate);
            
            if (workingDays == 0) return 100.0;
            
            int compliantDays = 0;
            for (AttendanceModel attendance : attendanceList) {
                if (attendance.isCompleteAttendance() && 
                    (!attendance.isLateAttendance() || attendance.isWithinGracePeriod())) {
                    compliantDays++;
                }
            }
            
            return (double) compliantDays / workingDays * 100.0;
        } catch (Exception e) {
            System.err.println("Error calculating compliance rate: " + e.getMessage());
            return 0.0;
        }
    }
    
    /**
     * Get daily attendance report for all employees on a specific date
     */
    public List<DailyAttendanceRecord> getDailyAttendanceReport(LocalDate date) {
        List<DailyAttendanceRecord> dailyRecords = new ArrayList<>();
        
        try {
            Date sqlDate = Date.valueOf(date);
            List<AttendanceModel> attendanceList = attendanceDAO.getAttendanceByDate(sqlDate);
            
            for (AttendanceModel attendance : attendanceList) {
                DailyAttendanceRecord record = new DailyAttendanceRecord();
                record.setEmployeeId(attendance.getEmployeeId());
                record.setDate(date);
                record.setTimeIn(attendance.getFormattedTimeIn());
                record.setTimeOut(attendance.getFormattedTimeOut());
                record.setComputedHours(BigDecimal.valueOf(attendance.getComputedHours()));
                
                // Determine status based on attendance
                if (attendance.isCompleteAttendance()) {
                    if (attendance.isLateAttendance()) {
                        record.setStatus("Late");
                    } else {
                        record.setStatus("Present");
                    }
                } else if (attendance.hasTimeIn()) {
                    record.setStatus("Incomplete");
                } else {
                    record.setStatus("Absent");
                }
                
                record.setLateHours(BigDecimal.valueOf(attendance.getLateHours()));
                record.setOvertimeHours(BigDecimal.valueOf(attendance.getOvertimeHours()));
                
                dailyRecords.add(record);
            }
            
        } catch (Exception e) {
            System.err.println("Error generating daily attendance report: " + e.getMessage());
        }
        
        return dailyRecords;
    }
    
    /**
     * Get monthly attendance summary for a specific employee
     */
    public AttendanceSummary getMonthlyAttendanceSummary(Integer employeeId, YearMonth yearMonth) {
        AttendanceSummary summary = new AttendanceSummary();
        
        try {
            Date startDate = Date.valueOf(yearMonth.atDay(1));
            Date endDate = Date.valueOf(yearMonth.atEndOfMonth());
            
            List<AttendanceModel> monthlyAttendance = getAttendanceByEmployeeAndDateRange(
                employeeId, startDate, endDate);
            
            int totalDays = 0;
            int completeDays = 0;
            int lateInstances = 0;
            BigDecimal totalHours = BigDecimal.ZERO;
            
            for (AttendanceModel attendance : monthlyAttendance) {
                totalDays++;
                
                if (attendance.isCompleteAttendance()) {
                    completeDays++;
                    totalHours = totalHours.add(BigDecimal.valueOf(attendance.getComputedHours()));
                }
                
                if (attendance.isLateAttendance()) {
                    lateInstances++;
                }
            }
            
            // Calculate attendance rate
            BigDecimal attendanceRate = totalDays > 0 ? 
                BigDecimal.valueOf(completeDays).divide(BigDecimal.valueOf(totalDays), 4, java.math.RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)) : BigDecimal.ZERO;
            
            summary.setEmployeeId(employeeId);
            summary.setYearMonth(yearMonth);
            summary.setTotalDays(totalDays);
            summary.setCompleteDays(completeDays);
            summary.setTotalHours(totalHours);
            summary.setAttendanceRate(attendanceRate);
            summary.setLateInstances(lateInstances);
            
        } catch (Exception e) {
            System.err.println("Error getting monthly attendance summary: " + e.getMessage());
        }
        
        return summary;
    }
    
    // Inner classes for reporting
    
    public static class DailyAttendanceRecord {
        private Integer employeeId;
        private String employeeName;
        private LocalDate date;
        private String timeIn;
        private String timeOut;
        private BigDecimal computedHours = BigDecimal.ZERO;
        private String status; // Present, Late, Absent, Incomplete
        private BigDecimal lateHours = BigDecimal.ZERO;
        private BigDecimal overtimeHours = BigDecimal.ZERO;
        
        // Getters and setters
        public Integer getEmployeeId() { return employeeId; }
        public void setEmployeeId(Integer employeeId) { this.employeeId = employeeId; }
        
        public String getEmployeeName() { return employeeName; }
        public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
        
        public LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }
        
        public String getTimeIn() { return timeIn; }
        public void setTimeIn(String timeIn) { this.timeIn = timeIn; }
        
        public String getTimeOut() { return timeOut; }
        public void setTimeOut(String timeOut) { this.timeOut = timeOut; }
        
        public BigDecimal getComputedHours() { return computedHours; }
        public void setComputedHours(BigDecimal computedHours) { 
            this.computedHours = computedHours != null ? computedHours : BigDecimal.ZERO; 
        }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public BigDecimal getLateHours() { return lateHours; }
        public void setLateHours(BigDecimal lateHours) { 
            this.lateHours = lateHours != null ? lateHours : BigDecimal.ZERO; 
        }
        
        public BigDecimal getOvertimeHours() { return overtimeHours; }
        public void setOvertimeHours(BigDecimal overtimeHours) { 
            this.overtimeHours = overtimeHours != null ? overtimeHours : BigDecimal.ZERO; 
        }
    }
    
    public static class AttendanceSummary {
        private Integer employeeId;
        private YearMonth yearMonth;
        private int totalDays = 0;
        private int completeDays = 0;
        private BigDecimal totalHours = BigDecimal.ZERO;
        private BigDecimal attendanceRate = BigDecimal.ZERO;
        private int lateInstances = 0;
        
        // Getters and setters
        public Integer getEmployeeId() { return employeeId; }
        public void setEmployeeId(Integer employeeId) { this.employeeId = employeeId; }
        
        public YearMonth getYearMonth() { return yearMonth; }
        public void setYearMonth(YearMonth yearMonth) { this.yearMonth = yearMonth; }
        
        public int getTotalDays() { return totalDays; }
        public void setTotalDays(int totalDays) { this.totalDays = totalDays; }
        
        public int getCompleteDays() { return completeDays; }
        public void setCompleteDays(int completeDays) { this.completeDays = completeDays; }
        
        public BigDecimal getTotalHours() { return totalHours; }
        public void setTotalHours(BigDecimal totalHours) { 
            this.totalHours = totalHours != null ? totalHours : BigDecimal.ZERO; 
        }
        
        public BigDecimal getAttendanceRate() { return attendanceRate; }
        public void setAttendanceRate(BigDecimal attendanceRate) { 
            this.attendanceRate = attendanceRate != null ? attendanceRate : BigDecimal.ZERO; 
        }
        
        public int getLateInstances() { return lateInstances; }
        public void setLateInstances(int lateInstances) { this.lateInstances = lateInstances; }
    }
}