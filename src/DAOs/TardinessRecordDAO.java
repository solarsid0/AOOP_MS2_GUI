package DAOs;

import Models.TardinessRecordModel;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;

public class TardinessRecordDAO {
    
    private DatabaseConnection databaseConnection;
    
    public TardinessRecordDAO() {
        this.databaseConnection = new DatabaseConnection();
    }
    
    public TardinessRecordDAO(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }
    
    /**
     * Create a tardiness record
     */
    public boolean createTardinessRecord(TardinessRecordModel tardinessRecord) {
        String sql = """
            INSERT INTO tardinessrecord (attendanceId, tardinessHours, tardinessType, supervisorNotes, createdAt)
            VALUES (?, ?, ?, ?, NOW())
            """;
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setInt(1, tardinessRecord.getAttendanceId());
            stmt.setBigDecimal(2, tardinessRecord.getTardinessHours());
            stmt.setString(3, tardinessRecord.getTardinessType().toString());
            stmt.setString(4, tardinessRecord.getSupervisorNotes());
            
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        tardinessRecord.setTardinessRecordId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
            return false;
            
        } catch (SQLException e) {
            System.err.println("Error creating tardiness record: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Delete tardiness records by attendance ID
     */
    public boolean deleteTardinessRecordsByAttendance(int attendanceId) {
        String sql = "DELETE FROM tardinessrecord WHERE attendanceId = ?";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, attendanceId);
            int deletedRows = stmt.executeUpdate();
            
            if (deletedRows > 0) {
                System.out.println("Deleted " + deletedRows + " tardiness records for attendance ID: " + attendanceId);
            }
            
            return true; // Return true even if no rows deleted (no existing records)
            
        } catch (SQLException e) {
            System.err.println("Error deleting tardiness records by attendance: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Generate tardiness records for a pay period
     */
    public int generateTardinessRecords(int payPeriodId) {
        String sql = """
            INSERT INTO tardinessrecord (attendanceId, tardinessHours, tardinessType, supervisorNotes, createdAt)
            SELECT a.attendanceId,
                   CASE 
                       WHEN a.timeIn > '08:10:00' THEN 
                           (TIME_TO_SEC(a.timeIn) - TIME_TO_SEC('08:00:00')) / 3600.0
                       ELSE 0.00
                   END as tardinessHours,
                   'LATE' as tardinessType,
                   CONCAT('Late arrival at ', TIME_FORMAT(a.timeIn, '%H:%i'), 
                          ' - Auto-generated for payroll period ', pp.periodName) as supervisorNotes,
                   NOW() as createdAt
            FROM attendance a
            JOIN employee e ON a.employeeId = e.employeeId
            JOIN payroll p ON e.employeeId = p.employeeId
            JOIN payperiod pp ON p.payPeriodId = pp.payPeriodId
            WHERE p.payPeriodId = ?
              AND a.timeIn > '08:10:00'
              AND a.date BETWEEN pp.startDate AND pp.endDate
              AND NOT EXISTS (
                  SELECT 1 FROM tardinessrecord tr 
                  WHERE tr.attendanceId = a.attendanceId
              )
            """;
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, payPeriodId);
            int insertedRows = stmt.executeUpdate();
            
            System.out.println("Generated " + insertedRows + " tardiness records for pay period " + payPeriodId);
            return insertedRows;
            
        } catch (SQLException e) {
            System.err.println("Error generating tardiness records: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Delete tardiness records by pay period
     */
    public int deleteByPayPeriod(int payPeriodId) {
        String sql = """
            DELETE tr FROM tardinessrecord tr
            JOIN attendance a ON tr.attendanceId = a.attendanceId
            JOIN payroll p ON a.employeeId = p.employeeId
            WHERE p.payPeriodId = ?
            """;
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, payPeriodId);
            int deletedRows = stmt.executeUpdate();
            
            System.out.println("Deleted " + deletedRows + " tardiness records for pay period " + payPeriodId);
            return deletedRows;
            
        } catch (SQLException e) {
            System.err.println("Error deleting tardiness records by pay period: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Check if records exist for a pay period
     */
    public boolean hasRecordsForPeriod(int payPeriodId) {
        String sql = """
            SELECT COUNT(*) FROM tardinessrecord tr
            JOIN attendance a ON tr.attendanceId = a.attendanceId
            JOIN payroll p ON a.employeeId = p.employeeId
            WHERE p.payPeriodId = ?
            """;
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, payPeriodId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            return false;
            
        } catch (SQLException e) {
            System.err.println("Error checking tardiness records for pay period: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Find tardiness records by pay period
     */
    public List<TardinessRecordModel> findByPayPeriod(int payPeriodId) {
        List<TardinessRecordModel> tardinessRecords = new ArrayList<>();
        String sql = """
            SELECT tr.* FROM tardinessrecord tr
            JOIN attendance a ON tr.attendanceId = a.attendanceId
            JOIN payroll p ON a.employeeId = p.employeeId
            WHERE p.payPeriodId = ?
            ORDER BY tr.createdAt DESC
            """;
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, payPeriodId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                TardinessRecordModel record = mapResultSetToModel(rs);
                tardinessRecords.add(record);
            }
            
        } catch (SQLException e) {
            System.err.println("Error finding tardiness records by pay period: " + e.getMessage());
        }
        
        return tardinessRecords;
    }
    
    /**
     * Find tardiness records by employee and period
     */
    public List<TardinessRecordModel> findByEmployeeAndPeriod(int employeeId, int payPeriodId) {
        List<TardinessRecordModel> tardinessRecords = new ArrayList<>();
        String sql = """
            SELECT tr.* FROM tardinessrecord tr
            JOIN attendance a ON tr.attendanceId = a.attendanceId
            JOIN payroll p ON a.employeeId = p.employeeId
            WHERE a.employeeId = ? AND p.payPeriodId = ?
            ORDER BY a.date DESC
            """;
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, employeeId);
            stmt.setInt(2, payPeriodId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                TardinessRecordModel record = mapResultSetToModel(rs);
                tardinessRecords.add(record);
            }
            
        } catch (SQLException e) {
            System.err.println("Error finding tardiness records by employee and period: " + e.getMessage());
        }
        
        return tardinessRecords;
    }
    
    /**
     * Find all tardiness records for an employee
     */
    public List<TardinessRecordModel> findByEmployee(int employeeId) {
        List<TardinessRecordModel> tardinessRecords = new ArrayList<>();
        String sql = """
            SELECT tr.* FROM tardinessrecord tr
            JOIN attendance a ON tr.attendanceId = a.attendanceId
            WHERE a.employeeId = ?
            ORDER BY a.date DESC
            """;
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, employeeId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                TardinessRecordModel record = mapResultSetToModel(rs);
                tardinessRecords.add(record);
            }
            
        } catch (SQLException e) {
            System.err.println("Error finding tardiness records by employee: " + e.getMessage());
        }
        
        return tardinessRecords;
    }
    
    /**
     * Find tardiness record by ID
     */
    public TardinessRecordModel findById(int tardinessRecordId) {
        String sql = "SELECT * FROM tardinessrecord WHERE tardinessRecordId = ?";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, tardinessRecordId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return mapResultSetToModel(rs);
            }
            
        } catch (SQLException e) {
            System.err.println("Error finding tardiness record by ID: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Update tardiness record
     */
    public boolean updateTardinessRecord(TardinessRecordModel tardinessRecord) {
        String sql = """
            UPDATE tardinessrecord 
            SET tardinessHours = ?, tardinessType = ?, supervisorNotes = ?
            WHERE tardinessRecordId = ?
            """;
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setBigDecimal(1, tardinessRecord.getTardinessHours());
            stmt.setString(2, tardinessRecord.getTardinessType().toString());
            stmt.setString(3, tardinessRecord.getSupervisorNotes());
            stmt.setInt(4, tardinessRecord.getTardinessRecordId());
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            System.err.println("Error updating tardiness record: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Delete tardiness record by ID
     */
    public boolean deleteTardinessRecord(int tardinessRecordId) {
        String sql = "DELETE FROM tardinessrecord WHERE tardinessRecordId = ?";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, tardinessRecordId);
            int deletedRows = stmt.executeUpdate();
            
            return deletedRows > 0;
            
        } catch (SQLException e) {
            System.err.println("Error deleting tardiness record: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get tardiness statistics for an employee
     */
    public TardinessStatistics getTardinessStatistics(int employeeId, LocalDate startDate, LocalDate endDate) {
        String sql = """
            SELECT 
                COUNT(*) as totalInstances,
                SUM(tr.tardinessHours) as totalHours,
                AVG(tr.tardinessHours) as averageHours,
                MAX(tr.tardinessHours) as maxHours,
                COUNT(CASE WHEN tr.tardinessType = 'LATE' THEN 1 END) as lateInstances,
                COUNT(CASE WHEN tr.tardinessType = 'UNDERTIME' THEN 1 END) as undertimeInstances
            FROM tardinessrecord tr
            JOIN attendance a ON tr.attendanceId = a.attendanceId
            WHERE a.employeeId = ?
              AND a.date BETWEEN ? AND ?
            """;
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, employeeId);
            stmt.setDate(2, Date.valueOf(startDate));
            stmt.setDate(3, Date.valueOf(endDate));
            
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                TardinessStatistics stats = new TardinessStatistics();
                stats.setEmployeeId(employeeId);
                stats.setStartDate(startDate);
                stats.setEndDate(endDate);
                stats.setTotalInstances(rs.getInt("totalInstances"));
                stats.setTotalHours(rs.getBigDecimal("totalHours"));
                stats.setAverageHours(rs.getBigDecimal("averageHours"));
                stats.setMaxHours(rs.getBigDecimal("maxHours"));
                stats.setLateInstances(rs.getInt("lateInstances"));
                stats.setUndertimeInstances(rs.getInt("undertimeInstances"));
                return stats;
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting tardiness statistics: " + e.getMessage());
        }
        
        return new TardinessStatistics(); // Return empty stats if error
    }
    
    /**
     * Get monthly tardiness summary for all employees
     */
    public List<TardinessStatistics> getMonthlyTardinessSummary(int year, int month) {
        List<TardinessStatistics> summaryList = new ArrayList<>();
        String sql = """
            SELECT 
                a.employeeId,
                COUNT(*) as totalInstances,
                SUM(tr.tardinessHours) as totalHours,
                AVG(tr.tardinessHours) as averageHours,
                MAX(tr.tardinessHours) as maxHours,
                COUNT(CASE WHEN tr.tardinessType = 'LATE' THEN 1 END) as lateInstances,
                COUNT(CASE WHEN tr.tardinessType = 'UNDERTIME' THEN 1 END) as undertimeInstances
            FROM tardinessrecord tr
            JOIN attendance a ON tr.attendanceId = a.attendanceId
            WHERE YEAR(a.date) = ? AND MONTH(a.date) = ?
            GROUP BY a.employeeId
            ORDER BY totalHours DESC
            """;
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, year);
            stmt.setInt(2, month);
            
            ResultSet rs = stmt.executeQuery();
            
            LocalDate startDate = LocalDate.of(year, month, 1);
            LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
            
            while (rs.next()) {
                TardinessStatistics stats = new TardinessStatistics();
                stats.setEmployeeId(rs.getInt("employeeId"));
                stats.setStartDate(startDate);
                stats.setEndDate(endDate);
                stats.setTotalInstances(rs.getInt("totalInstances"));
                stats.setTotalHours(rs.getBigDecimal("totalHours"));
                stats.setAverageHours(rs.getBigDecimal("averageHours"));
                stats.setMaxHours(rs.getBigDecimal("maxHours"));
                stats.setLateInstances(rs.getInt("lateInstances"));
                stats.setUndertimeInstances(rs.getInt("undertimeInstances"));
                summaryList.add(stats);
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting monthly tardiness summary: " + e.getMessage());
        }
        
        return summaryList;
    }
    
    /**
     * Map ResultSet to TardinessRecordModel
     */
    private TardinessRecordModel mapResultSetToModel(ResultSet rs) throws SQLException {
        TardinessRecordModel record = new TardinessRecordModel();
        record.setTardinessRecordId(rs.getInt("tardinessRecordId"));
        record.setAttendanceId(rs.getInt("attendanceId"));
        record.setTardinessHours(rs.getBigDecimal("tardinessHours"));
        
        String typeString = rs.getString("tardinessType");
        if (typeString != null) {
            try {
                record.setTardinessType(TardinessRecordModel.TardinessType.valueOf(typeString));
            } catch (IllegalArgumentException e) {
                record.setTardinessType(TardinessRecordModel.TardinessType.LATE); // Default
            }
        }
        
        record.setSupervisorNotes(rs.getString("supervisorNotes"));
        return record;
    }
    
    /**
     * Inner class for tardiness statistics
     */
    public static class TardinessStatistics {
        private int employeeId;
        private LocalDate startDate;
        private LocalDate endDate;
        private int totalInstances = 0;
        private BigDecimal totalHours = BigDecimal.ZERO;
        private BigDecimal averageHours = BigDecimal.ZERO;
        private BigDecimal maxHours = BigDecimal.ZERO;
        private int lateInstances = 0;
        private int undertimeInstances = 0;
        
        // Getters and setters
        public int getEmployeeId() { return employeeId; }
        public void setEmployeeId(int employeeId) { this.employeeId = employeeId; }
        
        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
        
        public LocalDate getEndDate() { return endDate; }
        public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
        
        public int getTotalInstances() { return totalInstances; }
        public void setTotalInstances(int totalInstances) { this.totalInstances = totalInstances; }
        
        public BigDecimal getTotalHours() { return totalHours; }
        public void setTotalHours(BigDecimal totalHours) { 
            this.totalHours = totalHours != null ? totalHours : BigDecimal.ZERO; 
        }
        
        public BigDecimal getAverageHours() { return averageHours; }
        public void setAverageHours(BigDecimal averageHours) { 
            this.averageHours = averageHours != null ? averageHours : BigDecimal.ZERO; 
        }
        
        public BigDecimal getMaxHours() { return maxHours; }
        public void setMaxHours(BigDecimal maxHours) { 
            this.maxHours = maxHours != null ? maxHours : BigDecimal.ZERO; 
        }
        
        public int getLateInstances() { return lateInstances; }
        public void setLateInstances(int lateInstances) { this.lateInstances = lateInstances; }
        
        public int getUndertimeInstances() { return undertimeInstances; }
        public void setUndertimeInstances(int undertimeInstances) { this.undertimeInstances = undertimeInstances; }
        
        /**
         * Calculate tardiness rate as percentage
         */
        public BigDecimal getTardinessRate(int workingDays) {
            if (workingDays <= 0) return BigDecimal.ZERO;
            return BigDecimal.valueOf(totalInstances)
                             .divide(BigDecimal.valueOf(workingDays), 4, BigDecimal.ROUND_HALF_UP)
                             .multiply(BigDecimal.valueOf(100));
        }
        
        /**
         * Get tardiness severity level
         */
        public String getSeverityLevel() {
            if (totalHours.compareTo(BigDecimal.ZERO) <= 0) {
                return "None";
            } else if (averageHours.compareTo(new BigDecimal("0.5")) <= 0) {
                return "Low";
            } else if (averageHours.compareTo(new BigDecimal("1.0")) <= 0) {
                return "Moderate";
            } else if (averageHours.compareTo(new BigDecimal("2.0")) <= 0) {
                return "High";
            } else {
                return "Critical";
            }
        }
        
        @Override
        public String toString() {
            return "TardinessStatistics{" +
                    "employeeId=" + employeeId +
                    ", totalInstances=" + totalInstances +
                    ", totalHours=" + totalHours +
                    ", averageHours=" + averageHours +
                    ", lateInstances=" + lateInstances +
                    ", undertimeInstances=" + undertimeInstances +
                    ", severityLevel='" + getSeverityLevel() + '\'' +
                    '}';
        }
    }
}