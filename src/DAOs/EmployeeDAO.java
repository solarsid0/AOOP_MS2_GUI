package DAOs;

import Models.EmployeeModel;
import Models.EmployeeModel.EmployeeStatus;
import Models.PositionModel;
import Models.GovIdModel;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.ArrayList;

/**
 * Enhanced EmployeeDAO with integrated Government ID management and Position integration
 * Handles employee, government ID, and position operations in one place
 * @author User
 */
public class EmployeeDAO extends BaseDAO<EmployeeModel, Integer> {
    
    // Manila timezone constant
    public static final ZoneId MANILA_TIMEZONE = ZoneId.of("Asia/Manila");
    
    /**
     * Constructor that accepts a DatabaseConnection instance
     * @param databaseConnection
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
    
    // GOVERNMENT ID INTEGRATION METHODS
    
    /**
     * Save employee with government IDs in a transaction
     * @param employee Employee to save
     * @param govIds Government IDs to save
     * @return true if both employee and government IDs were saved successfully
     */
    public boolean saveEmployeeWithGovIds(EmployeeModel employee, GovIdModel govIds) {
        Connection conn = null;
        try {
            conn = databaseConnection.createConnection();
            conn.setAutoCommit(false); // Start transaction
            
            // 1. Save employee first
            boolean employeeSaved = saveEmployeeInTransaction(employee, conn);
            if (!employeeSaved) {
                conn.rollback();
                return false;
            }
            
            // 2. Save government IDs with the employee ID
            if (govIds != null) {
                govIds.setEmployeeId(employee.getEmployeeId());
                boolean govIdsSaved = saveGovIdsInTransaction(govIds, conn);
                if (!govIdsSaved) {
                    conn.rollback();
                    return false;
                }
            }
            
            conn.commit(); // Commit transaction
            System.out.println("Employee and government IDs saved successfully");
            return true;
            
        } catch (Exception e) {
            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (SQLException rollbackEx) {
                System.err.println("Error rolling back transaction: " + rollbackEx.getMessage());
            }
            System.err.println("Error saving employee with government IDs: " + e.getMessage());
            return false;
        } finally {
            try {
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close();
                }
            } catch (SQLException e) {
                System.err.println("Error closing connection: " + e.getMessage());
            }
        }
    }
    
    /**
     * Update employee with government IDs in a transaction
     * @param employee Employee to update
     * @param govIds Government IDs to update
     * @return true if both employee and government IDs were updated successfully
     */
    public boolean updateEmployeeWithGovIds(EmployeeModel employee, GovIdModel govIds) {
        Connection conn = null;
        try {
            conn = databaseConnection.createConnection();
            conn.setAutoCommit(false); // Start transaction
            
            // 1. Update employee
            boolean employeeUpdated = updateEmployeeInTransaction(employee, conn);
            if (!employeeUpdated) {
                conn.rollback();
                return false;
            }
            
            // 2. Update or insert government IDs
            if (govIds != null) {
                govIds.setEmployeeId(employee.getEmployeeId());
                boolean govIdsUpdated = saveOrUpdateGovIdsInTransaction(govIds, conn);
                if (!govIdsUpdated) {
                    conn.rollback();
                    return false;
                }
            }
            
            conn.commit(); // Commit transaction
            System.out.println("Employee and government IDs updated successfully");
            return true;
            
        } catch (Exception e) {
            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (SQLException rollbackEx) {
                System.err.println("Error rolling back transaction: " + rollbackEx.getMessage());
            }
            System.err.println("Error updating employee with government IDs: " + e.getMessage());
            return false;
        } finally {
            try {
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close();
                }
            } catch (SQLException e) {
                System.err.println("Error closing connection: " + e.getMessage());
            }
        }
    }
    
    /**
     * Get employee with government IDs
     * @param employeeId Employee ID
     * @return EmployeeWithGovIds object or null if not found
     */
    public EmployeeWithGovIds getEmployeeWithGovIds(Integer employeeId) {
        if (employeeId == null) {
            return null;
        }
        
        EmployeeModel employee = findById(employeeId);
        if (employee == null) {
            return null;
        }
        
        GovIdModel govIds = getEmployeeGovIds(employeeId);
        
        return new EmployeeWithGovIds(employee, govIds);
    }
    
    /**
     * Get government IDs for an employee
     * @param employeeId Employee ID
     * @return GovIdModel or null if not found
     */
    public GovIdModel getEmployeeGovIds(Integer employeeId) {
        if (employeeId == null) {
            return null;
        }
        
        String sql = "SELECT * FROM govid WHERE employeeId = ?";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, employeeId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return mapResultSetToGovId(rs);
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting government IDs: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Delete employee and associated government IDs
     * @param employeeId Employee ID to delete
     * @return true if deletion was successful
     */
    public boolean deleteEmployeeWithGovIds(Integer employeeId) {
        Connection conn = null;
        try {
            conn = databaseConnection.createConnection();
            conn.setAutoCommit(false); // Start transaction
            
            // 1. Delete government IDs first (foreign key constraint)
            boolean govIdsDeleted = deleteGovIdsInTransaction(employeeId, conn);
            
            // 2. Delete employee
            boolean employeeDeleted = deleteEmployeeInTransaction(employeeId, conn);
            
            if (employeeDeleted) {
                conn.commit(); // Commit transaction
                System.out.println("✅ Employee and government IDs deleted successfully");
                return true;
            } else {
                conn.rollback();
                return false;
            }
            
        } catch (Exception e) {
            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (SQLException rollbackEx) {
                System.err.println("Error rolling back transaction: " + rollbackEx.getMessage());
            }
            System.err.println("Error deleting employee with government IDs: " + e.getMessage());
            return false;
        } finally {
            try {
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close();
                }
            } catch (SQLException e) {
                System.err.println("Error closing connection: " + e.getMessage());
            }
        }
    }
    
    // POSITION INTEGRATION METHODS
    
    /**
     * Get employee with position information
     * @param employeeId Employee ID
     * @return EmployeeWithPosition object or null if not found
     */
    public EmployeeWithPosition getEmployeeWithPosition(Integer employeeId) {
        if (employeeId == null) {
            return null;
        }
        
        EmployeeModel employee = findById(employeeId);
        if (employee == null) {
            return null;
        }
        
        PositionModel position = null;
        if (employee.getPositionId() != null) {
            PositionDAO positionDAO = new PositionDAO(databaseConnection);
            position = positionDAO.findById(employee.getPositionId());
        }
        
        return new EmployeeWithPosition(employee, position);
    }
    
    /**
     * Get all employees with their position information
     * @return List of EmployeeWithPosition objects
     */
    public List<EmployeeWithPosition> getAllEmployeesWithPosition() {
        List<EmployeeModel> employees = findAll();
        List<EmployeeWithPosition> result = new ArrayList<>();
        
        PositionDAO positionDAO = new PositionDAO(databaseConnection);
        
        for (EmployeeModel employee : employees) {
            PositionModel position = null;
            if (employee.getPositionId() != null) {
                position = positionDAO.findById(employee.getPositionId());
            }
            result.add(new EmployeeWithPosition(employee, position));
        }
        
        return result;
    }
    
    /**
     * Get active employees with their position information
     * @return List of active EmployeeWithPosition objects
     */
    public List<EmployeeWithPosition> getActiveEmployeesWithPosition() {
        List<EmployeeModel> employees = getActiveEmployees();
        List<EmployeeWithPosition> result = new ArrayList<>();
        
        PositionDAO positionDAO = new PositionDAO(databaseConnection);
        
        for (EmployeeModel employee : employees) {
            PositionModel position = null;
            if (employee.getPositionId() != null) {
                position = positionDAO.findById(employee.getPositionId());
            }
            result.add(new EmployeeWithPosition(employee, position));
        }
        
        return result;
    }
    
    /**
     * Get employees by department with position information
     * @param department Department name
     * @return List of EmployeeWithPosition objects
     */
    public List<EmployeeWithPosition> getEmployeesByDepartmentWithPosition(String department) {
        List<EmployeeModel> employees = getEmployeesByDepartment(department);
        List<EmployeeWithPosition> result = new ArrayList<>();
        
        PositionDAO positionDAO = new PositionDAO(databaseConnection);
        
        for (EmployeeModel employee : employees) {
            PositionModel position = null;
            if (employee.getPositionId() != null) {
                position = positionDAO.findById(employee.getPositionId());
            }
            result.add(new EmployeeWithPosition(employee, position));
        }
        
        return result;
    }
    
    // PRIVATE TRANSACTION HELPER METHODS
    
    /**
     * Save employee within a transaction
     */
    private boolean saveEmployeeInTransaction(EmployeeModel employee, Connection conn) throws SQLException {
        String sql = """
            INSERT INTO employee 
            (firstName, lastName, birthDate, phoneNumber, email, basicSalary, 
             hourlyRate, userRole, passwordHash, status, lastLogin, positionId, supervisorId) 
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setInsertParameters(stmt, employee);
            
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        employee.setEmployeeId(generatedKeys.getInt(1));
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
    
    /**
     * Update employee within a transaction
     */
    private boolean updateEmployeeInTransaction(EmployeeModel employee, Connection conn) throws SQLException {
        String sql = """
            UPDATE employee SET 
            firstName = ?, lastName = ?, birthDate = ?, phoneNumber = ?, email = ?, 
            basicSalary = ?, hourlyRate = ?, userRole = ?, passwordHash = ?, status = ?, 
            lastLogin = ?, positionId = ?, supervisorId = ?, updatedAt = ? 
            WHERE employeeId = ?
            """;
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            setUpdateParameters(stmt, employee);
            // Set Manila time for updatedAt
            stmt.setTimestamp(14, Timestamp.valueOf(getManilaTime()));
            
            return stmt.executeUpdate() > 0;
        }
    }
    
    /**
     * Save government IDs within a transaction
     */
    private boolean saveGovIdsInTransaction(GovIdModel govIds, Connection conn) throws SQLException {
        String sql = "INSERT INTO govid (sss, philhealth, tin, pagibig, employeeId) VALUES (?, ?, ?, ?, ?)";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, govIds.getSss());
            stmt.setString(2, govIds.getPhilhealth());
            stmt.setString(3, govIds.getTin());
            stmt.setString(4, govIds.getPagibig());
            stmt.setInt(5, govIds.getEmployeeId());
            
            return stmt.executeUpdate() > 0;
        }
    }
    
    /**
     * Save or update government IDs within a transaction
     */
    private boolean saveOrUpdateGovIdsInTransaction(GovIdModel govIds, Connection conn) throws SQLException {
        // Check if government IDs already exist
        String checkSQL = "SELECT govId FROM govid WHERE employeeId = ?";
        
        try (PreparedStatement checkStmt = conn.prepareStatement(checkSQL)) {
            checkStmt.setInt(1, govIds.getEmployeeId());
            ResultSet rs = checkStmt.executeQuery();
            
            if (rs.next()) {
                // Update existing government IDs
                String updateSQL = "UPDATE govid SET sss = ?, philhealth = ?, tin = ?, pagibig = ? WHERE employeeId = ?";
                
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSQL)) {
                    updateStmt.setString(1, govIds.getSss());
                    updateStmt.setString(2, govIds.getPhilhealth());
                    updateStmt.setString(3, govIds.getTin());
                    updateStmt.setString(4, govIds.getPagibig());
                    updateStmt.setInt(5, govIds.getEmployeeId());
                    
                    return updateStmt.executeUpdate() > 0;
                }
            } else {
                // Insert new government IDs
                return saveGovIdsInTransaction(govIds, conn);
            }
        }
    }
    
    /**
     * Delete government IDs within a transaction
     */
    private boolean deleteGovIdsInTransaction(Integer employeeId, Connection conn) throws SQLException {
        String sql = "DELETE FROM govid WHERE employeeId = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, employeeId);
            stmt.executeUpdate(); // Don't check rows affected - it's okay if no gov IDs exist
            return true;
        }
    }
    
    /**
     * Delete employee within a transaction
     */
    private boolean deleteEmployeeInTransaction(Integer employeeId, Connection conn) throws SQLException {
        String sql = "DELETE FROM employee WHERE employeeId = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, employeeId);
            return stmt.executeUpdate() > 0;
        }
    }
    
    /**
     * Map ResultSet to GovIdModel
     */
    private GovIdModel mapResultSetToGovId(ResultSet rs) throws SQLException {
        GovIdModel govId = new GovIdModel();
        govId.setGovId(rs.getInt("govId"));
        govId.setSss(rs.getString("sss"));
        govId.setPhilhealth(rs.getString("philhealth"));
        govId.setTin(rs.getString("tin"));
        govId.setPagibig(rs.getString("pagibig"));
        govId.setEmployeeId(rs.getInt("employeeId"));
        return govId;
    }
    
    // EXISTING METHODS FROM YOUR ORIGINAL DAO
    
    /**
     * Deactivates an employee by setting their status to TERMINATED
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
    
    /**
     * Gets current Manila time
     */
    public static LocalDateTime getManilaTime() {
        return ZonedDateTime.now(MANILA_TIMEZONE).toLocalDateTime();
    }
    
    /**
     * Get all active employees (not terminated)
     * @return 
     */
    public List<EmployeeModel> getActiveEmployees() {
        String sql = "SELECT * FROM employee WHERE status != 'Terminated' ORDER BY employeeId ASC";
        return executeQuery(sql);
    }
    
    /**
     * Find employee by email
     */
    public EmployeeModel findByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }
        String sql = "SELECT * FROM employee WHERE email = ?";
        return executeSingleQuery(sql, email);
    }
    
    /**
     * Find all employees by position ID
     * @param positionId Position ID to search for
     * @return List of employees in the specified position
     */
    public List<EmployeeModel> findByPosition(Integer positionId) {
        if (positionId == null) {
            return new ArrayList<>();
        }
        
        String sql = "SELECT * FROM employee WHERE positionId = ? AND status != 'Terminated' ORDER BY lastName, firstName";
        return executeQuery(sql, positionId);
    }
    
    /**
     * Find all employees by position ID including terminated employees
     * @param positionId Position ID to search for
     * @return List of all employees (including terminated) in the specified position
     */
    public List<EmployeeModel> findByPositionIncludingTerminated(Integer positionId) {
        if (positionId == null) {
            return new ArrayList<>();
        }
        
        String sql = "SELECT * FROM employee WHERE positionId = ? ORDER BY lastName, firstName";
        return executeQuery(sql, positionId);
    }
    
    /**
     * Find employees by position with position information
     * @param positionId Position ID to search for
     * @return List of EmployeeWithPosition objects
     */
    public List<EmployeeWithPosition> findByPositionWithPosition(Integer positionId) {
        List<EmployeeModel> employees = findByPosition(positionId);
        List<EmployeeWithPosition> result = new ArrayList<>();
        
        PositionDAO positionDAO = new PositionDAO(databaseConnection);
        PositionModel position = positionDAO.findById(positionId);
        
        for (EmployeeModel employee : employees) {
            result.add(new EmployeeWithPosition(employee, position));
        }
        
        return result;
    }
    
    /**
     * Count employees in a specific position
     * @param positionId Position ID to count
     * @return Number of active employees in the position
     */
    public int countEmployeesByPosition(Integer positionId) {
        if (positionId == null) {
            return 0;
        }
        
        String sql = "SELECT COUNT(*) FROM employee WHERE positionId = ? AND status != 'Terminated'";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, positionId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1);
            }
            
        } catch (SQLException e) {
            System.err.println("Error counting employees by position: " + e.getMessage());
        }
        
        return 0;
    }
    
    /**
     * Get employees by department
     * @param department
     */
    public List<EmployeeModel> getEmployeesByDepartment(String department) {
        if (department == null || department.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        String sql = """
            SELECT e.* FROM employee e 
            JOIN position p ON e.positionId = p.positionId 
            WHERE p.department = ? 
            AND e.status != 'Terminated'
            ORDER BY e.lastName, e.firstName
            """;
        return executeQuery(sql, department);
    }
    
    /**
     * Check if employee is rank-and-file
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
            SELECT e.* FROM employee e 
            JOIN position p ON e.positionId = p.positionId 
            WHERE e.status != 'Terminated'
            AND (LOWER(p.department) = 'rank-and-file' 
                 OR LOWER(p.position) LIKE '%rank%file%'
                 OR (LOWER(p.position) NOT LIKE '%manager%' 
                     AND LOWER(p.position) NOT LIKE '%supervisor%' 
                     AND LOWER(p.position) NOT LIKE '%head%' 
                     AND LOWER(p.position) NOT LIKE '%director%' 
                     AND LOWER(p.position) NOT LIKE '%ceo%' 
                     AND LOWER(p.position) NOT LIKE '%coo%' 
                     AND LOWER(p.position) NOT LIKE '%cfo%' 
                     AND LOWER(p.department) != 'leadership'))
            ORDER BY e.lastName, e.firstName
            """;
        return executeQuery(sql);
    }
    
    /**
     * Get all non rank-and-file employees (management/leadership)
     * @return List of non rank-and-file employees
     */
    public List<EmployeeModel> getNonRankAndFileEmployees() {
        String sql = """
            SELECT e.* FROM employee e 
            JOIN position p ON e.positionId = p.positionId 
            WHERE e.status != 'Terminated'
            AND (LOWER(p.position) LIKE '%manager%' 
                 OR LOWER(p.position) LIKE '%supervisor%' 
                 OR LOWER(p.position) LIKE '%head%' 
                 OR LOWER(p.position) LIKE '%director%' 
                 OR LOWER(p.position) LIKE '%ceo%' 
                 OR LOWER(p.position) LIKE '%coo%' 
                 OR LOWER(p.position) LIKE '%cfo%' 
                 OR LOWER(p.department) = 'leadership')
            ORDER BY e.lastName, e.firstName
            """;
        return executeQuery(sql);
    }
    
    /**
     * Get rank-and-file employees with position information
     * @return List of rank-and-file EmployeeWithPosition objects
     */
    public List<EmployeeWithPosition> getRankAndFileEmployeesWithPosition() {
        List<EmployeeModel> employees = getRankAndFileEmployees();
        List<EmployeeWithPosition> result = new ArrayList<>();
        
        PositionDAO positionDAO = new PositionDAO(databaseConnection);
        
        for (EmployeeModel employee : employees) {
            PositionModel position = null;
            if (employee.getPositionId() != null) {
                position = positionDAO.findById(employee.getPositionId());
            }
            result.add(new EmployeeWithPosition(employee, position));
        }
        
        return result;
    }
    
    /**
     * Get non rank-and-file employees with position information
     * @return List of non rank-and-file EmployeeWithPosition objects
     */
    public List<EmployeeWithPosition> getNonRankAndFileEmployeesWithPosition() {
        List<EmployeeModel> employees = getNonRankAndFileEmployees();
        List<EmployeeWithPosition> result = new ArrayList<>();
        
        PositionDAO positionDAO = new PositionDAO(databaseConnection);
        
        for (EmployeeModel employee : employees) {
            PositionModel position = null;
            if (employee.getPositionId() != null) {
                position = positionDAO.findById(employee.getPositionId());
            }
            result.add(new EmployeeWithPosition(employee, position));
        }
        
        return result;
    }
    
    // OVERRIDE SAVE AND UPDATE METHODS TO USE CUSTOM SQL
    
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

        String sql = """
            INSERT INTO employee 
            (firstName, lastName, birthDate, phoneNumber, email, basicSalary, 
             hourlyRate, userRole, passwordHash, status, lastLogin, positionId, supervisorId) 
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

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

            String sql = """
                UPDATE employee SET 
                firstName = ?, lastName = ?, birthDate = ?, phoneNumber = ?, email = ?, 
                basicSalary = ?, hourlyRate = ?, userRole = ?, passwordHash = ?, status = ?, 
                lastLogin = ?, positionId = ?, supervisorId = ?, updatedAt = ? 
                WHERE employeeId = ?
                """;

            try (Connection conn = databaseConnection.createConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                int paramIndex = 1;

                // Set all fields except employeeId
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

                // Set updatedAt timestamp
                stmt.setTimestamp(paramIndex++, Timestamp.valueOf(getManilaTime()));

                // Finally set employeeId for WHERE clause
                stmt.setInt(paramIndex++, employee.getEmployeeId());

                int rowsAffected = stmt.executeUpdate();
                return rowsAffected > 0;

            } catch (SQLException e) {
                System.err.println("Error updating employee: " + e.getMessage());
                return false;
            }
        }
    
    // INNER CLASSES FOR DATA TRANSFER OBJECTS
    
    /**
     * Data transfer object for employee with government IDs
     */
    public static class EmployeeWithGovIds {
        private final EmployeeModel employee;
        private final GovIdModel govIds;
        
        public EmployeeWithGovIds(EmployeeModel employee, GovIdModel govIds) {
            this.employee = employee;
            this.govIds = govIds;
        }
        
        public EmployeeModel getEmployee() {
            return employee;
        }
        
        public GovIdModel getGovIds() {
            return govIds;
        }
        
        /**
         * Check if employee has complete government IDs
         */
        public boolean hasCompleteGovIds() {
            return govIds != null && govIds.isComplete();
        }
        
        /**
         * Check if all government IDs are valid
         */
        public boolean hasValidGovIds() {
            return govIds != null && govIds.isAllValid();
        }
        
        @Override
        public String toString() {
            return String.format("EmployeeWithGovIds{employee=%s, govIdsComplete=%s}", 
                               employee.getFullName(), hasCompleteGovIds());
        }
    }
    
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
         * Get position name or empty string if no position
         */
        public String getPositionName() {
            return position != null ? position.getPosition() : "";
        }
        
        /**
         * Get department or empty string if no position
         */
        public String getDepartment() {
            return position != null ? position.getDepartment() : "";
        }
        
        /**
         * Check if employee has a position assigned
         */
        public boolean hasPosition() {
            return position != null;
        }
        
        /**
         * Get full employee name with position
         */
        public String getEmployeeWithPositionInfo() {
            String name = employee.getFullName();
            if (hasPosition()) {
                return name + " (" + getPositionName() + ")";
            }
            return name;
        }
        
        /**
         * Check if employee is in management position
         */
        public boolean isManagement() {
            if (!hasPosition()) {
                return false;
            }
            String positionName = getPositionName().toLowerCase();
            String department = getDepartment().toLowerCase();
            
            return positionName.contains("manager") || 
                   positionName.contains("supervisor") || 
                   positionName.contains("head") || 
                   positionName.contains("director") || 
                   positionName.contains("ceo") || 
                   positionName.contains("coo") || 
                   positionName.contains("cfo") || 
                   department.contains("leadership");
        }
        
        /**
         * Check if employee is rank-and-file
         */
        public boolean isRankAndFile() {
            if (!hasPosition()) {
                return false;
            }
            String department = getDepartment().toLowerCase();
            String positionName = getPositionName().toLowerCase();
            
            return department.contains("rank") || 
                   positionName.contains("rank") || 
                   (!isManagement() && !department.contains("leadership"));
        }
        
        @Override
        public String toString() {
            return String.format("EmployeeWithPosition{employee=%s, position=%s}", 
                               employee.getFullName(), getPositionName());
        }
    }
}