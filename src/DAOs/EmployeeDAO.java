package DAOs;

import Models.EmployeeModel;
import Models.EmployeeModel.EmployeeStatus;
import Models.PositionModel;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.ArrayList;

/**
 * Data Access Object for EmployeeModel entities.
 * Enhanced with Manila timezone support, rank-and-file business logic, and soft deletion
 * 
 * @author User
 */
public class EmployeeDAO extends BaseDAO<EmployeeModel, Integer> {
    
    // Manila timezone constant
    public static final ZoneId MANILA_TIMEZONE = ZoneId.of("Asia/Manila");
    
    /**
     * Constructor that accepts a DatabaseConnection instance
     * @param databaseConnection The database connection to use for all operations
     */
    public EmployeeDAO(DatabaseConnection databaseConnection) {
        super(databaseConnection);
    }
    
    // ABSTRACT METHOD IMPLEMENTATIONS - Required by BaseDAO
    
    @Override
    protected EmployeeModel mapResultSetToEntity(ResultSet rs) throws SQLException {
        EmployeeModel employee = new EmployeeModel();
        
        // Set basic employee information
        employee.setEmployeeId(rs.getInt("employeeId"));
        employee.setFirstName(rs.getString("firstName"));
        employee.setLastName(rs.getString("lastName"));
        
        // Handle birth date (could be null in database)
        Date birthDate = rs.getDate("birthDate");
        if (birthDate != null) {
            employee.setBirthDate(birthDate.toLocalDate());
        }
        
        // Set contact information
        employee.setPhoneNumber(rs.getString("phoneNumber"));
        employee.setEmail(rs.getString("email"));
        
        // Handle salary information (BigDecimal for precise money calculations)
        BigDecimal basicSalary = rs.getBigDecimal("basicSalary");
        if (basicSalary != null) {
            employee.setBasicSalary(basicSalary);
        }
        
        BigDecimal hourlyRate = rs.getBigDecimal("hourlyRate");
        if (hourlyRate != null) {
            employee.setHourlyRate(hourlyRate);
        }
        
        // Set system information
        employee.setUserRole(rs.getString("userRole"));
        employee.setPasswordHash(rs.getString("passwordHash"));
        
        // Handle employee status (convert string to enum)
        String statusStr = rs.getString("status");
        if (statusStr != null) {
            employee.setStatus(EmployeeStatus.fromString(statusStr));
        }
        
        // Handle timestamp fields
        Timestamp createdAt = rs.getTimestamp("createdAt");
        if (createdAt != null) {
            employee.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        Timestamp updatedAt = rs.getTimestamp("updatedAt");
        if (updatedAt != null) {
            employee.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        
        // Handle lastLogin as Timestamp
        Timestamp lastLogin = rs.getTimestamp("lastLogin");
        if (lastLogin != null) {
            employee.setLastLogin(lastLogin);
        }
        
        // Handle foreign key relationships (could be null)
        int positionId = rs.getInt("positionId");
        if (!rs.wasNull()) {
            employee.setPositionId(positionId);
        }
        
        int supervisorId = rs.getInt("supervisorId");
        if (!rs.wasNull()) {
            employee.setSupervisorId(supervisorId);
        }
        
        return employee;
    }
    
    // Alternative method name for compatibility with existing code
    protected EmployeeModel extractEmployeeFromResultSet(ResultSet rs) throws SQLException {
        return mapResultSetToEntity(rs);
    }
    
    @Override
    protected String getTableName() {
        return "employee";
    }
    
    @Override
    protected String getPrimaryKeyColumn() {
        return "employeeId";
    }
    
    @Override
    protected void setInsertParameters(PreparedStatement stmt, EmployeeModel employee) throws SQLException {
        int paramIndex = 1;
        
        // Set basic employee information
        stmt.setString(paramIndex++, employee.getFirstName());
        stmt.setString(paramIndex++, employee.getLastName());
        
        // Handle birth date (could be null)
        if (employee.getBirthDate() != null) {
            stmt.setDate(paramIndex++, Date.valueOf(employee.getBirthDate()));
        } else {
            stmt.setNull(paramIndex++, Types.DATE);
        }
        
        // Set contact information
        stmt.setString(paramIndex++, employee.getPhoneNumber());
        stmt.setString(paramIndex++, employee.getEmail());
        
        // Set salary information
        stmt.setBigDecimal(paramIndex++, employee.getBasicSalary());
        stmt.setBigDecimal(paramIndex++, employee.getHourlyRate());
        
        // Set system information
        stmt.setString(paramIndex++, employee.getUserRole());
        stmt.setString(paramIndex++, employee.getPasswordHash());
        stmt.setString(paramIndex++, employee.getStatus().getValue());
        
        // Handle last login (could be null for new employees)
        if (employee.getLastLogin() != null) {
            stmt.setTimestamp(paramIndex++, employee.getLastLogin());
        } else {
            stmt.setNull(paramIndex++, Types.TIMESTAMP);
        }
        
        // Set required position ID
        stmt.setInt(paramIndex++, employee.getPositionId());
        
        // Handle supervisor ID (could be null for top-level employees)
        if (employee.getSupervisorId() != null) {
            stmt.setInt(paramIndex++, employee.getSupervisorId());
        } else {
            stmt.setNull(paramIndex++, Types.INTEGER);
        }
    }
    
    @Override
    protected void setUpdateParameters(PreparedStatement stmt, EmployeeModel employee) throws SQLException {
        int paramIndex = 1;
        
        // Set all the same fields as INSERT
        stmt.setString(paramIndex++, employee.getFirstName());
        stmt.setString(paramIndex++, employee.getLastName());
        
        if (employee.getBirthDate() != null) {
            stmt.setDate(paramIndex++, Date.valueOf(employee.getBirthDate()));
        } else {
            stmt.setNull(paramIndex++, Types.DATE);
        }
        
        stmt.setString(paramIndex++, employee.getPhoneNumber());
        stmt.setString(paramIndex++, employee.getEmail());
        stmt.setBigDecimal(paramIndex++, employee.getBasicSalary());
        stmt.setBigDecimal(paramIndex++, employee.getHourlyRate());
        stmt.setString(paramIndex++, employee.getUserRole());
        stmt.setString(paramIndex++, employee.getPasswordHash());
        stmt.setString(paramIndex++, employee.getStatus().getValue());
        
        if (employee.getLastLogin() != null) {
            stmt.setTimestamp(paramIndex++, employee.getLastLogin());
        } else {
            stmt.setNull(paramIndex++, Types.TIMESTAMP);
        }
        
        stmt.setInt(paramIndex++, employee.getPositionId());
        
        if (employee.getSupervisorId() != null) {
            stmt.setInt(paramIndex++, employee.getSupervisorId());
        } else {
            stmt.setNull(paramIndex++, Types.INTEGER);
        }
        
        // Finally, set the employee ID for the WHERE clause
        stmt.setInt(paramIndex++, employee.getEmployeeId());
    }
    
    @Override
    protected Integer getEntityId(EmployeeModel employee) {
        return employee.getEmployeeId();
    }
    
    @Override
    protected void handleGeneratedKey(EmployeeModel entity, ResultSet generatedKeys) throws SQLException {
        if (generatedKeys.next()) {
            entity.setEmployeeId(generatedKeys.getInt(1));
        }
    }
    
    // NEW METHOD FOR SOFT DELETION
    
    /**
     * Deactivates an employee by setting their status to TERMINATED and updating the timestamp
     * @param employeeId The ID of the employee to deactivate
     * @return true if deactivation was successful, false otherwise
     */
    public boolean deactivateEmployee(Integer employeeId) {
        if (employeeId == null) {
            System.err.println("Cannot deactivate employee with null ID");
            return false;
        }
        
        String sql = "UPDATE employee SET status = ?, updatedAt = ? WHERE employeeId = ?";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, EmployeeStatus.TERMINATED.getValue());
            stmt.setTimestamp(2, Timestamp.valueOf(getManilaTime()));
            stmt.setInt(3, employeeId);
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            System.err.println("Error deactivating employee: " + e.getMessage());
            return false;
        }
    }
    
    // MANILA TIMEZONE UTILITIES
    
    /**
     * Gets current Manila time
     * @return LocalDateTime in Manila timezone
     */
    public static LocalDateTime getManilaTime() {
        return ZonedDateTime.now(MANILA_TIMEZONE).toLocalDateTime();
    }
    
    /**
     * Gets current Manila date
     * @return LocalDate in Manila timezone
     */
    public static LocalDate getManilaDate() {
        return ZonedDateTime.now(MANILA_TIMEZONE).toLocalDate();
    }
    
    /**
     * Converts any LocalDateTime to Manila timezone
     * @param dateTime LocalDateTime to convert
     * @return LocalDateTime in Manila timezone
     */
    public static LocalDateTime toManilaTime(LocalDateTime dateTime) {
        if (dateTime == null) return null;
        return dateTime.atZone(ZoneId.systemDefault())
                      .withZoneSameInstant(MANILA_TIMEZONE)
                      .toLocalDateTime();
    }
    
    // RANK-AND-FILE CLASSIFICATION METHODS
    
    /**
     * Check if employee is rank-and-file based on position
     * Rule: department = 'rank-and-file' OR position LIKE '%rank-and-file%'
     * @param employeeId The employee ID to check
     * @return true if employee is rank-and-file
     */
    public boolean isEmployeeRankAndFile(Integer employeeId) {
        if (employeeId == null) {
            return false;
        }
        
        String sql = """
            SELECT COUNT(*) > 0 
            FROM employee e 
            JOIN position p ON e.positionId = p.positionId 
            WHERE e.employeeId = ? 
            AND (LOWER(p.department) = 'rank-and-file' 
                 OR LOWER(p.position) LIKE '%rank%file%')
            """;
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, employeeId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getBoolean(1);
            }
            
        } catch (SQLException e) {
            System.err.println("Error checking rank-and-file status: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Get all rank-and-file employees
     * @return List of rank-and-file employees
     */
    public List<EmployeeModel> getRankAndFileEmployees() {
        String sql = """
            SELECT e.* 
            FROM employee e 
            JOIN position p ON e.positionId = p.positionId 
            WHERE (LOWER(p.department) = 'rank-and-file' 
                   OR LOWER(p.position) LIKE '%rank%file%')
            AND e.status != 'Terminated'
            ORDER BY e.lastName, e.firstName
            """;
        
        return executeQuery(sql);
    }
    
    /**
     * Get all non rank-and-file employees
     * @return List of non rank-and-file employees
     */
    public List<EmployeeModel> getNonRankAndFileEmployees() {
        String sql = """
            SELECT e.* 
            FROM employee e 
            JOIN position p ON e.positionId = p.positionId 
            WHERE NOT (LOWER(p.department) = 'rank-and-file' 
                      OR LOWER(p.position) LIKE '%rank%file%')
            AND e.status != 'Terminated'
            ORDER BY e.lastName, e.firstName
            """;
        
        return executeQuery(sql);
    }
    
    /**
     * Get employees by rank-and-file classification
     * @param isRankAndFile true for rank-and-file, false for non rank-and-file
     * @return List of employees in the specified category
     */
    public List<EmployeeModel> getEmployeesByRankAndFileStatus(boolean isRankAndFile) {
        if (isRankAndFile) {
            return getRankAndFileEmployees();
        } else {
            return getNonRankAndFileEmployees();
        }
    }
    
    /**
     * Get employee with position information for payroll processing
     * @param employeeId The employee ID
     * @return EmployeeWithPosition object containing both employee and position data
     */
    public EmployeeWithPosition getEmployeeWithPosition(Integer employeeId) {
        if (employeeId == null) {
            return null;
        }
        
        String sql = """
            SELECT e.*, p.position, p.department, p.positionDescription
            FROM employee e 
            JOIN position p ON e.positionId = p.positionId 
            WHERE e.employeeId = ?
            """;
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, employeeId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                EmployeeModel employee = mapResultSetToEntity(rs);
                PositionModel position = new PositionModel();
                position.setPositionId(employee.getPositionId());
                position.setPosition(rs.getString("position"));
                position.setPositionDescription(rs.getString("positionDescription"));
                position.setDepartment(rs.getString("department"));
                
                return new EmployeeWithPosition(employee, position);
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting employee with position: " + e.getMessage());
        }
        
        return null;
    }
    
    // OVERTIME ELIGIBILITY METHODS
    
    /**
     * Get employees eligible for overtime pay (rank-and-file employees only)
     * @return List of employees eligible for overtime
     */
    public List<EmployeeModel> getOvertimeEligibleEmployees() {
        return getRankAndFileEmployees();
    }
    
    /**
     * Check if employee is eligible for overtime pay
     * @param employeeId Employee ID to check
     * @return true if employee is eligible for overtime
     */
    public boolean isEligibleForOvertime(Integer employeeId) {
        return isEmployeeRankAndFile(employeeId);
    }
    
    // LEAVE REQUEST VALIDATION METHODS
    
    /**
     * Check if employee can submit leave request for the given date
     * Rule: Can only submit for today or future dates (Manila time)
     * @param employeeId Employee ID
     * @param leaveDate Requested leave date
     * @return true if leave request is valid
     */
    public boolean canSubmitLeaveRequest(Integer employeeId, LocalDate leaveDate) {
        if (employeeId == null || leaveDate == null) {
            return false;
        }
        
        // Check if employee exists and is active
        EmployeeModel employee = findById(employeeId);
        if (employee == null || !employee.isActive()) {
            return false;
        }
        
        // Check date validity - can only request for today or future dates
        LocalDate today = getManilaDate();
        return !leaveDate.isBefore(today);
    }
    
    /**
     * Check if employee can submit overtime request for the given date
     * Rule: Can only submit for today or future dates (Manila time) and only rank-and-file
     * @param employeeId Employee ID
     * @param overtimeDate Requested overtime date
     * @return true if overtime request is valid
     */
    public boolean canSubmitOvertimeRequest(Integer employeeId, LocalDate overtimeDate) {
        if (employeeId == null || overtimeDate == null) {
            return false;
        }
        
        // Check if employee exists and is active
        EmployeeModel employee = findById(employeeId);
        if (employee == null || !employee.isActive()) {
            return false;
        }
        
        // Check if employee is eligible for overtime (rank-and-file only)
        if (!isEmployeeRankAndFile(employeeId)) {
            return false;
        }
        
        // Check date validity - can only request for today or future dates
        LocalDate today = getManilaDate();
        return !overtimeDate.isBefore(today);
    }
    
    // PAYROLL STATISTICS AND REPORTING
    
    /**
     * Get payroll summary statistics
     * @return PayrollStatistics object with employee counts by category
     */
    public PayrollStatistics getPayrollStatistics() {
        String sql = """
            SELECT 
                COUNT(*) as total_employees,
                SUM(CASE WHEN (LOWER(p.department) = 'rank-and-file' 
                              OR LOWER(p.position) LIKE '%rank%file%') 
                         THEN 1 ELSE 0 END) as rank_and_file_count,
                SUM(CASE WHEN NOT (LOWER(p.department) = 'rank-and-file' 
                                  OR LOWER(p.position) LIKE '%rank%file%') 
                         THEN 1 ELSE 0 END) as non_rank_and_file_count,
                COUNT(CASE WHEN e.status = 'Terminated' THEN 1 END) as terminated_count
            FROM employee e 
            JOIN position p ON e.positionId = p.positionId
            """;
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return new PayrollStatistics(
                    rs.getInt("total_employees"),
                    rs.getInt("rank_and_file_count"),
                    rs.getInt("non_rank_and_file_count"),
                    rs.getInt("terminated_count")
                );
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting payroll statistics: " + e.getMessage());
        }
        
        return new PayrollStatistics(0, 0, 0, 0);
    }
    
    /**
     * Get employees ready for payroll processing (active employees with valid positions and salary info)
     * @return List of employees ready for payroll
     */
    public List<EmployeeModel> getPayrollReadyEmployees() {
        String sql = """
            SELECT e.* 
            FROM employee e 
            JOIN position p ON e.positionId = p.positionId 
            WHERE e.status != 'Terminated'
            AND e.basicSalary IS NOT NULL
            AND e.hourlyRate IS NOT NULL
            AND e.basicSalary > 0
            AND e.hourlyRate > 0
            ORDER BY e.lastName, e.firstName
            """;
        
        return executeQuery(sql);
    }
    
    /**
     * Get employees with missing salary information
     * @return List of employees with incomplete salary data
     */
    public List<EmployeeModel> getEmployeesWithMissingSalaryInfo() {
        String sql = """
            SELECT e.* 
            FROM employee e 
            WHERE e.status != 'Terminated'
            AND (e.basicSalary IS NULL 
                 OR e.hourlyRate IS NULL 
                 OR e.basicSalary <= 0 
                 OR e.hourlyRate <= 0)
            ORDER BY e.lastName, e.firstName
            """;
        
        return executeQuery(sql);
    }
    
    // WORK SCHEDULE VALIDATION METHODS
    
    /**
     * Check if a date is a valid workday (Monday to Friday)
     * @param date Date to check
     * @return true if it's a weekday
     */
    public static boolean isWorkDay(LocalDate date) {
        if (date == null) return false;
        
        java.time.DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek.getValue() >= 1 && dayOfWeek.getValue() <= 5; // Monday = 1, Friday = 5
    }
    
    /**
     * Calculate total weekdays in a given month
     * @param year Year
     * @param month Month (1-12)
     * @return Number of weekdays in the month
     */
    public static int calculateWeekdaysInMonth(int year, int month) {
        LocalDate firstDay = LocalDate.of(year, month, 1);
        LocalDate lastDay = firstDay.withDayOfMonth(firstDay.lengthOfMonth());
        
        int weekdays = 0;
        for (LocalDate date = firstDay; !date.isAfter(lastDay); date = date.plusDays(1)) {
            if (isWorkDay(date)) {
                weekdays++;
            }
        }
        
        return weekdays;
    }
    
    /**
     * Calculate total weekdays between two dates (inclusive)
     * @param startDate Start date
     * @param endDate End date
     * @return Number of weekdays between the dates
     */
    public static int calculateWeekdaysBetween(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            return 0;
        }
        
        int weekdays = 0;
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            if (isWorkDay(date)) {
                weekdays++;
            }
        }
        
        return weekdays;
    }
    
    // METHODS REQUIRED BY OTHER CLASSES
    
    /**
     * Gets all employees supervised by a specific supervisor
     * @param supervisorId The supervisor's employee ID
     * @return List of employees under this supervisor
     */
    public List<EmployeeModel> getEmployeesBySupervisor(Integer supervisorId) {
        if (supervisorId == null) {
            return new ArrayList<>();
        }
        
        String sql = """
            SELECT e.* 
            FROM employee e 
            WHERE e.supervisorId = ? 
            AND e.status != 'Terminated'
            ORDER BY e.lastName, e.firstName
            """;
        
        return executeQuery(sql, supervisorId);
    }
    
    /**
     * Gets all employees in a specific position
     * @param positionId The position ID
     * @return List of employees in the position
     */
    public List<EmployeeModel> findByPosition(Integer positionId) {
        if (positionId == null) {
            return new ArrayList<>();
        }
        
        String sql = """
            SELECT e.* 
            FROM employee e 
            WHERE e.positionId = ? 
            AND e.status != 'Terminated'
            ORDER BY e.lastName, e.firstName
            """;
        
        return executeQuery(sql, positionId);
    }
    
    // CUSTOM EMPLOYEE METHODS
    
    /**
     * Finds an employee by their email address
     * @param email The email address to search for
     * @return The EmployeeModel if found, null if not found
     */
    public EmployeeModel findByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }
        String sql = "SELECT * FROM employee WHERE email = ?";
        return executeSingleQuery(sql, email);
    }
    
    /**
     * Finds all employees with a specific status
     * @param status The employee status to search for
     * @return List of employees with the specified status
     */
    public List<EmployeeModel> findByStatus(EmployeeStatus status) {
        if (status == null) {
            return new ArrayList<>();
        }
        String sql = "SELECT * FROM employee WHERE status = ?";
        return executeQuery(sql, status.getValue());
    }
    
    /**
     * Updates the last login timestamp for an employee to Manila time
     * @param employeeId The employee ID
     * @return true if update was successful, false otherwise
     */
    public boolean updateLastLoginToManilaTime(Integer employeeId) {
        if (employeeId == null) {
            return false;
        }
        
        String sql = "UPDATE employee SET lastLogin = ? WHERE employeeId = ?";
        Timestamp manilaTime = Timestamp.valueOf(getManilaTime());
        
        int rowsAffected = executeUpdate(sql, manilaTime, employeeId);
        return rowsAffected > 0;
    }
    
    /**
     * Gets all active employees (not terminated)
     * @return List of active employees
     */
    public List<EmployeeModel> getActiveEmployees() {
        String sql = "SELECT * FROM employee WHERE status != 'Terminated' ORDER BY lastName, firstName";
        return executeQuery(sql);
    }
    
    /**
     * Finds employees by department name
     * @param department The department name to search for
     * @return List of employees in the specified department
     */
    public List<EmployeeModel> getEmployeesByDepartment(String department) {
        if (department == null || department.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        String sql = """
            SELECT e.* FROM employee e 
            JOIN position p ON e.positionId = p.positionId 
            WHERE p.department =

 ? 
            AND e.status != 'Terminated'
            ORDER BY e.lastName, e.firstName
            """;
        return executeQuery(sql, department);
    }
    
    /**
     * Finds employees by user role
     * @param userRole The user role to search for
     * @return List of employees with the specified role
     */
    public List<EmployeeModel> getEmployeesByRole(String userRole) {
        String sql = "SELECT * FROM employee WHERE userRole = ? AND status != 'Terminated' ORDER BY lastName, firstName";
        return executeQuery(sql, userRole);
    }
    
    /**
     * Updates employee salary information with Manila timezone tracking
     * @param employeeId The employee ID to update
     * @param basicSalary The new basic salary
     * @param hourlyRate The new hourly rate
     * @return true if update was successful, false otherwise
     */
    public boolean updateSalaryWithManilaTime(Integer employeeId, BigDecimal basicSalary, BigDecimal hourlyRate) {
        if (employeeId == null || basicSalary == null || hourlyRate == null) {
            return false;
        }
        
        String sql = "UPDATE employee SET basicSalary = ?, hourlyRate = ?, updatedAt = ? WHERE employeeId = ?";
        Timestamp manilaTime = Timestamp.valueOf(getManilaTime());
        
        int rowsAffected = executeUpdate(sql, basicSalary, hourlyRate, manilaTime, employeeId);
        return rowsAffected > 0;
    }
    
    // OVERRIDE METHODS - Use custom SQL
    
    private String buildInsertSQL() {
        return "INSERT INTO employee " +
               "(firstName, lastName, birthDate, phoneNumber, email, basicSalary, " +
               "hourlyRate, userRole, passwordHash, status, lastLogin, positionId, supervisorId) " +
               "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    }
    
    private String buildUpdateSQL() {
        return "UPDATE employee SET " +
               "firstName = ?, lastName = ?, birthDate = ?, phoneNumber = ?, email = ?, " +
               "basicSalary = ?, hourlyRate = ?, userRole = ?, passwordHash = ?, status = ?, " +
               "lastLogin = ?, positionId = ?, supervisorId = ?, updatedAt = ? " +
               "WHERE employeeId = ?";
    }
    
    @Override
    public boolean save(EmployeeModel employee) {
        if (employee == null) {
            System.err.println("Cannot save null employee");
            return false;
        }

        // Validate required fields
        if (employee.getFirstName() == null || employee.getFirstName().trim().isEmpty() ||
            employee.getLastName() == null || employee.getLastName().trim().isEmpty() ||
            employee.getEmail() == null || employee.getEmail().trim().isEmpty() ||
            employee.getPositionId() == null) {
            System.err.println("Missing required employee fields");
            return false;
        }

        String sql = buildInsertSQL();

        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            setInsertParameters(stmt, employee);
            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int generatedId = generatedKeys.getInt(1);
                        employee.setEmployeeId(generatedId);
                    }
                }
                return true;
            }
            return false;

        } catch (SQLException e) {
            System.err.println("Error saving employee: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean update(EmployeeModel employee) {
        if (employee == null || employee.getEmployeeId() == null) {
            return false;
        }
        
        String sql = buildUpdateSQL();
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            setUpdateParameters(stmt, employee);
            // Set Manila time for updatedAt
            stmt.setTimestamp(14, Timestamp.valueOf(getManilaTime()));
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            System.err.println("Error updating employee: " + e.getMessage());
            return false;
        }
    }
    
    // INNER CLASSES FOR COMPLEX OPERATIONS
    
    /**
     * Data transfer object for employee with position information
     */
    public static class EmployeeWithPosition {
        private final EmployeeModel employee;
        private final PositionModel position;
        
        public EmployeeWithPosition(EmployeeModel employee, PositionModel position) {
            this.employee = employee;
            this.position = position;
        }
        
        public EmployeeModel getEmployee() {
            return employee;
        }
        
        public PositionModel getPosition() {
            return position;
        }
        
        /**
         * Check if this employee is rank-and-file
         * @return true if rank-and-file
         */
        public boolean isRankAndFile() {
            if (position == null) return false;
            
            String department = position.getDepartment();
            String positionTitle = position.getPosition();
            
            if (department != null && department.toLowerCase().equals("rank-and-file")) {
                return true;
            }
            
            if (positionTitle != null && positionTitle.toLowerCase().contains("rank") && 
                positionTitle.toLowerCase().contains("file")) {
                return true;
            }
            
            return false;
        }
        
        /**
         * Check if employee is eligible for overtime
         * @return true if eligible for overtime
         */
        public boolean isEligibleForOvertime() {
            return isRankAndFile();
        }
        
        /**
         * Get employee category for payroll
         * @return Category string
         */
        public String getPayrollCategory() {
            return isRankAndFile() ? "Rank-and-File" : "Non Rank-and-File";
        }
        
        /**
         * Get monthly rate calculation method
         * @return Description of how monthly rate is calculated
         */
        public String getMonthlyRateMethod() {
            return isRankAndFile() ? 
                "Daily Rate × Days Worked (with late deductions)" : 
                "Fixed Basic Salary (no late deductions)";
        }
        
        @Override
        public String toString() {
            return String.format("EmployeeWithPosition{employee=%s, position=%s, category=%s}", 
                               employee.getFullName(), 
                               position != null ? position.getPosition() : "Unknown",
                               getPayrollCategory());
        }
    }
    
    /**
     * Statistics object for payroll reporting
     */
    public static class PayrollStatistics {
        private final int totalEmployees;
        private final int rankAndFileCount;
        private final int nonRankAndFileCount;
        private final int terminatedCount;
        
        public PayrollStatistics(int totalEmployees, int rankAndFileCount, 
                               int nonRankAndFileCount, int terminatedCount) {
            this.totalEmployees = totalEmployees;
            this.rankAndFileCount = rankAndFileCount;
            this.nonRankAndFileCount = nonRankAndFileCount;
            this.terminatedCount = terminatedCount;
        }
        
        public int getTotalEmployees() {
            return totalEmployees;
        }
        
        public int getRankAndFileCount() {
            return rankAndFileCount;
        }
        
        public int getNonRankAndFileCount() {
            return nonRankAndFileCount;
        }
        
        public int getTerminatedCount() {
            return terminatedCount;
        }
        
        public int getActiveEmployees() {
            return totalEmployees - terminatedCount;
        }
        
        public int getOvertimeEligibleCount() {
            return rankAndFileCount;
        }
        
        public double getRankAndFilePercentage() {
            return totalEmployees > 0 ? (double) rankAndFileCount / totalEmployees * 100 : 0;
        }
        
        public double getNonRankAndFilePercentage() {
            return totalEmployees > 0 ? (double) nonRankAndFileCount / totalEmployees * 100 : 0;
        }
        
        @Override
        public String toString() {
            return String.format("PayrollStatistics{total=%d, rankAndFile=%d (%.1f%%), " +
                               "nonRankAndFile=%d (%.1f%%), active=%d, terminated=%d, overtimeEligible=%d}", 
                               totalEmployees, rankAndFileCount, getRankAndFilePercentage(),
                               nonRankAndFileCount, getNonRankAndFilePercentage(),
                               getActiveEmployees(), terminatedCount, getOvertimeEligibleCount());
        }
    }
}