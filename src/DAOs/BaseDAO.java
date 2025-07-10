package DAOs;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Enhanced abstract base class for all Data Access Objects (DAOs).
 * This class provides common database operations that all DAOs need,
 * with Manila timezone support and view querying capabilities.
 * Every specific DAO (like EmployeeDAO, AttendanceDAO) should extend this class.
 * 
 * @param <T> The entity type this DAO handles (like Employee, Attendance)
 * @param <ID> The type of the entity's primary key (usually Integer)
 * @author User
 */
public abstract class BaseDAO<T, ID> {
    
    // The database connection instance that this DAO will use
    protected final DatabaseConnection databaseConnection;
    
    /**
     * Constructor that requires a DatabaseConnection
     * Every DAO needs a database connection to work
     * @param databaseConnection The database connection instance to use
     */
    public BaseDAO(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }
    

    // ABSTRACT METHODS - Subclasses must implement these

    
    /**
     * Converts a database row (ResultSet) into a Java object
     * Each DAO needs to know how to convert database data to their specific object type
     * @param rs The ResultSet containing the database row data
     * @return The Java object created from the database row
     * @throws SQLException if there's an error reading from the database
     */
    protected abstract T mapResultSetToEntity(ResultSet rs) throws SQLException;
    
    /**
     * Returns the name of the database table this DAO works with
     * For example: EmployeeDAO returns "employee", AttendanceDAO returns "attendance"
     * @return The table name as a string
     */
    protected abstract String getTableName();
    
    /**
     * Returns the name of the primary key column for this table
     * For example: "employeeId", "attendanceId", etc.
     * @return The primary key column name
     */
    protected abstract String getPrimaryKeyColumn();
    
    /**
     * Sets the parameters for INSERT SQL statements
     * This method tells the database what values to insert for a new record
     * @param stmt The PreparedStatement to set parameters on
     * @param entity The object containing the values to insert
     * @throws SQLException if there's an error setting the parameters
     */
    protected abstract void setInsertParameters(PreparedStatement stmt, T entity) throws SQLException;
    
    /**
     * Sets the parameters for UPDATE SQL statements  
     * This method tells the database what values to update for an existing record
     * @param stmt The PreparedStatement to set parameters on
     * @param entity The object containing the new values
     * @throws SQLException if there's an error setting the parameters
     */
    protected abstract void setUpdateParameters(PreparedStatement stmt, T entity) throws SQLException;
    
    /**
     * Gets the ID value from an entity object
     * This is used to identify which record to update or delete
     * @param entity The object to get the ID from
     * @return The ID value
     */
    protected abstract ID getEntityId(T entity);
    

    // CONCRETE CRUD METHODS - These work for all DAOs

    
    /**
     * Saves a new record to the database (CREATE operation)
     * This method handles the SQL INSERT statement
     * @param entity The object to save to the database
     * @return true if the save was successful, false if it failed
     */
    public boolean save(T entity) {
        // Build the INSERT SQL statement
        String sql = buildInsertSQL();
        
        // Use try-with-resources to automatically close database connections
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            // Set the values for the INSERT statement
            setInsertParameters(stmt, entity);
            
            // Execute the INSERT and see how many rows were affected
            int rowsAffected = stmt.executeUpdate();
            
            // If at least one row was inserted, the operation was successful
            if (rowsAffected > 0) {
                // Get any auto-generated keys (like auto-increment IDs)
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        handleGeneratedKey(entity, generatedKeys);
                    }
                }
                return true;
            }
            return false;
            
        } catch (SQLException e) {
            // If something goes wrong, print the error and return false
            System.err.println("Error saving entity: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Updates an existing record in the database (UPDATE operation)
     * This method handles the SQL UPDATE statement
     * @param entity The object with updated values
     * @return true if the update was successful, false if it failed
     */
    public boolean update(T entity) {
        // Build the UPDATE SQL statement
        String sql = buildUpdateSQL();
        
        // Use try-with-resources to automatically close database connections
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            // Set the values for the UPDATE statement
            setUpdateParameters(stmt, entity);
            
            // Execute the UPDATE and see how many rows were affected
            int rowsAffected = stmt.executeUpdate();
            
            // If at least one row was updated, the operation was successful
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            // If something goes wrong, print the error and return false
            System.err.println("Error updating entity: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Deletes a record from the database by its ID (DELETE operation)
     * This method handles the SQL DELETE statement
     * @param id The ID of the record to delete
     * @return true if the delete was successful, false if it failed
     */
    public boolean delete(ID id) {
        // Build a simple DELETE SQL statement
        String sql = "DELETE FROM " + getTableName() + " WHERE " + getPrimaryKeyColumn() + " = ?";
        
        // Use try-with-resources to automatically close database connections
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            // Set the ID parameter for the WHERE clause
            stmt.setObject(1, id);
            
            // Execute the DELETE and see how many rows were affected
            int rowsAffected = stmt.executeUpdate();
            
            // If at least one row was deleted, the operation was successful
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            // If something goes wrong, print the error and return false
            System.err.println("Error deleting entity with ID " + id + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Finds a single record by its ID (READ operation)
     * This method handles SELECT statements with WHERE clauses
     * @param id The ID to search for
     * @return The object if found, null if not found
     */
    public T findById(ID id) {
        // Build a SELECT statement to find one record by ID
        String sql = "SELECT * FROM " + getTableName() + " WHERE " + getPrimaryKeyColumn() + " = ?";
        
        // Use try-with-resources to automatically close database connections
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            // Set the ID parameter for the WHERE clause
            stmt.setObject(1, id);
            
            // Execute the query and get the results
            try (ResultSet rs = stmt.executeQuery()) {
                // If we found a record, convert it to an object and return it
                if (rs.next()) {
                    return mapResultSetToEntity(rs);
                }
            }
            
        } catch (SQLException e) {
            // If something goes wrong, print the error
            System.err.println("Error finding entity by ID " + id + ": " + e.getMessage());
        }
        
        // Return null if no record was found or if there was an error
        return null;
    }
    
    /**
     * Gets all records from the table (READ operation)
     * This method handles SELECT * statements
     * @return List of all objects in the table
     */
    public List<T> findAll() {
        // Build a simple SELECT * statement
        String sql = "SELECT * FROM " + getTableName();
        return executeQuery(sql);
    }
    

    // UTILITY METHODS - Helper methods for database operations

    
    /**
     * Executes a SELECT query and returns a list of objects
     * This is a helper method that other methods can use to run custom queries
     * @param sql The SQL query to execute
     * @param params Parameters for the query (for WHERE clauses)
     * @return List of objects from the query results
     */
    protected List<T> executeQuery(String sql, Object... params) {
        // Create an empty list to store the results
        List<T> results = new java.util.ArrayList<>();
        
        // Use try-with-resources to automatically close database connections
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            // Set any parameters that were passed in
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            
            // Execute the query and process each row
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    // Convert each database row to an object and add it to the list
                    results.add(mapResultSetToEntity(rs));
                }
            }
            
        } catch (SQLException e) {
            // If something goes wrong, print the error
            System.err.println("Error executing query: " + e.getMessage());
        }
        
        // Return the list of results (empty list if no results or error occurred)
        return results;
    }
    
    /**
     * Executes a SELECT query that should return only one result
     * This is useful for finding a specific record by email, name, etc.
     * @param sql The SQL query to execute
     * @param params Parameters for the query
     * @return The object if found, null if not found
     */
    protected T executeSingleQuery(String sql, Object... params) {
        // Use try-with-resources to automatically close database connections
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            // Set any parameters that were passed in
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            
            // Execute the query and check if we got a result
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    // Convert the database row to an object and return it
                    return mapResultSetToEntity(rs);
                }
            }
            
        } catch (SQLException e) {
            // If something goes wrong, print the error
            System.err.println("Error executing single query: " + e.getMessage());
        }
        
        // Return null if no result was found or if there was an error
        return null;
    }
    
    /**
     * Executes an UPDATE, INSERT, or DELETE statement
     * This is a helper method for operations that modify the database
     * @param sql The SQL statement to execute
     * @param params Parameters for the statement
     * @return Number of rows that were affected by the operation
     */
    protected int executeUpdate(String sql, Object... params) {
        // Use try-with-resources to automatically close database connections
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            // Set any parameters that were passed in
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            
            // Execute the update and return how many rows were affected
            return stmt.executeUpdate();
            
        } catch (SQLException e) {
            // If something goes wrong, print the error and return 0
            System.err.println("Error executing update: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Safely closes database resources
     * This method ensures that ResultSets and Statements are properly closed
     * @param rs ResultSet to close
     * @param stmt Statement to close
     */
    protected void closeResources(ResultSet rs, Statement stmt) {
        try {
            // Close ResultSet if it exists
            if (rs != null) rs.close();
            // Close Statement if it exists
            if (stmt != null) stmt.close();
        } catch (SQLException e) {
            // If there's an error closing resources, print it
            System.err.println("Error closing resources: " + e.getMessage());
        }
    }
    

    // MANILA TIMEZONE SUPPORT METHODS

    
    /**
     * Get current Manila time
     * @return Current LocalDateTime in Manila timezone
     */
    protected LocalDateTime getCurrentManilaTime() {
        return DatabaseConnection.getCurrentManilaTime();
    }
    
    /**
     * Get current Manila date
     * @return Current LocalDate in Manila timezone
     */
    protected LocalDate getCurrentManilaDate() {
        return DatabaseConnection.getCurrentManilaDate();
    }
    
    /**
     * Check if a date is today or in the future (Manila timezone)
     * Used for validating leave requests and OT requests
     * @param date The date to check
     * @return true if the date is today or future
     */
    protected boolean isTodayOrFuture(LocalDate date) {
        return DatabaseConnection.isTodayOrFuture(date);
    }
    
    /**
     * Check if a date is a workday (Monday-Friday)
     * @param date The date to check
     * @return true if the date is a workday
     */
    protected boolean isWorkday(LocalDate date) {
        return DatabaseConnection.isWorkday(date);
    }
    
    /**
     * Get workdays count between two dates
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return Number of workdays between the dates
     */
    protected int getWorkdaysCount(LocalDate startDate, LocalDate endDate) {
        return DatabaseConnection.getWorkdaysCount(startDate, endDate);
    }
    

    // VIEW OPERATIONS SUPPORT

    
    /**
     * Execute a query on a database view
     * Views are read-only, so this method only supports SELECT operations
     * @param viewName The name of the view to query
     * @param whereClause Optional WHERE clause (can be null or empty)
     * @param params Parameters for the WHERE clause
     * @return List of Map objects containing the view data
     */
    protected List<Map<String, Object>> queryView(String viewName, String whereClause, Object... params) {
        String sql = "SELECT * FROM " + viewName;
        if (whereClause != null && !whereClause.trim().isEmpty()) {
            sql += " WHERE " + whereClause;
        }
        
        List<Map<String, Object>> results = new java.util.ArrayList<>();
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            // Set parameters if provided
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            
            try (ResultSet rs = stmt.executeQuery()) {
                // Get column metadata
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();
                
                // Process each row
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = metaData.getColumnName(i);
                        Object value = rs.getObject(i);
                        row.put(columnName, value);
                    }
                    results.add(row);
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error querying view " + viewName + ": " + e.getMessage());
        }
        
        return results;
    }
    
    /**
     * Execute a custom query on a view with custom column selection
     * @param viewName The name of the view
     * @param columns Comma-separated list of columns to select
     * @param whereClause Optional WHERE clause
     * @param orderBy Optional ORDER BY clause
     * @param params Parameters for the query
     * @return List of Map objects containing the selected data
     */
    protected List<Map<String, Object>> queryViewCustom(String viewName, String columns, 
                                                       String whereClause, String orderBy, Object... params) {
        StringBuilder sqlBuilder = new StringBuilder("SELECT ");
        sqlBuilder.append(columns != null && !columns.trim().isEmpty() ? columns : "*");
        sqlBuilder.append(" FROM ").append(viewName);
        
        if (whereClause != null && !whereClause.trim().isEmpty()) {
            sqlBuilder.append(" WHERE ").append(whereClause);
        }
        
        if (orderBy != null && !orderBy.trim().isEmpty()) {
            sqlBuilder.append(" ORDER BY ").append(orderBy);
        }
        
        String sql = sqlBuilder.toString();
        List<Map<String, Object>> results = new java.util.ArrayList<>();
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            // Set parameters if provided
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            
            try (ResultSet rs = stmt.executeQuery()) {
                // Get column metadata
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();
                
                // Process each row
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = metaData.getColumnName(i);
                        Object value = rs.getObject(i);
                        row.put(columnName, value);
                    }
                    results.add(row);
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error executing custom view query: " + e.getMessage());
        }
        
        return results;
    }
    
    /**
     * Count records in a view with optional WHERE clause
     * @param viewName The name of the view
     * @param whereClause Optional WHERE clause
     * @param params Parameters for the WHERE clause
     * @return Number of records matching the criteria
     */
    protected int countFromView(String viewName, String whereClause, Object... params) {
        String sql = "SELECT COUNT(*) FROM " + viewName;
        if (whereClause != null && !whereClause.trim().isEmpty()) {
            sql += " WHERE " + whereClause;
        }
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            // Set parameters if provided
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error counting records in view " + viewName + ": " + e.getMessage());
        }
        
        return 0;
    }
    

    // PAYROLL-SPECIFIC HELPER METHODS

    
    /**
     * Check if an employee is rank-and-file based on department or position
     * @param department Employee's department
     * @param position Employee's position
     * @return true if employee is rank-and-file
     */
    protected boolean isRankAndFile(String department, String position) {
        if (department == null || position == null) {
            return false;
        }
        
        // Check if department is 'rank-and-file' OR position contains 'rank-and-file'
        return department.toLowerCase().contains("rank-and-file") || 
               position.toLowerCase().contains("rank-and-file");
    }
    
    /**
     * Calculate fractional days from late hours
     * Used for rank-and-file employees' attendance calculation
     * @param lateHours Hours the employee was late
     * @return Fractional days to deduct (late hours / 8)
     */
    protected double calculateFractionalDays(double lateHours) {
        return lateHours / DatabaseConnection.STANDARD_WORK_HOURS;
    }
    
    /**
     * Calculate overtime hours for rank-and-file employees
     * @param workedHours Total hours worked in a day
     * @param standardHours Standard working hours per day (usually 8)
     * @return Overtime hours (0 if not applicable)
     */
    protected double calculateOvertimeHours(double workedHours, double standardHours) {
        return Math.max(0, workedHours - standardHours);
    }
    
    /**
     * Calculate overtime pay for rank-and-file employees
     * @param overtimeHours Overtime hours worked
     * @param hourlyRate Employee's hourly rate
     * @return Overtime pay amount
     */
    protected double calculateOvertimePay(double overtimeHours, double hourlyRate) {
        return overtimeHours * hourlyRate * DatabaseConnection.OVERTIME_MULTIPLIER;
    }
    
    /**
     * Check if an attendance record is late
     * @param timeIn Time in string (HH:mm:ss format)
     * @return true if late arrival
     */
    protected boolean isLateArrival(String timeIn) {
        return DatabaseConnection.isLateArrival(timeIn);
    }
    
    /**
     * Calculate hours worked from attendance record
     * @param timeIn Time in (HH:mm:ss format)
     * @param timeOut Time out (HH:mm:ss format)
     * @return Hours worked (excluding lunch break)
     */
    protected double calculateHoursWorked(String timeIn, String timeOut) {
        return DatabaseConnection.calculateHoursWorked(timeIn, timeOut);
    }
    
    /**
     * Calculate late hours from time in
     * @param timeIn Time in (HH:mm:ss format)
     * @return Hours late (0 if not late)
     */
    protected double calculateLateHours(String timeIn) {
        return DatabaseConnection.calculateLateHours(timeIn);
    }
    
    /**
     * Validate leave request dates
     * @param startDate Leave start date
     * @param endDate Leave end date
     * @return true if valid leave date range
     */
    protected boolean isValidLeaveRange(LocalDate startDate, LocalDate endDate) {
        return DatabaseConnection.isValidLeaveRange(startDate, endDate);
    }
    
    /**
     * Validate overtime request dates
     * @param startDateTime Overtime start datetime
     * @param endDateTime Overtime end datetime
     * @return true if valid overtime range
     */
    protected boolean isValidOvertimeRange(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        return DatabaseConnection.isValidOvertimeRange(startDateTime, endDateTime);
    }
    
    /**
     * Round monetary values to 2 decimal places
     * @param value The value to round
     * @return Rounded value with 2 decimal places
     */
    protected double roundMoney(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
    
    /**
     * Get employee's monthly payslip data from view
     * @param employeeId Employee ID
     * @param payMonth Pay month in YYYY-MM format
     * @return Map containing payslip data or null if not found
     */
    protected Map<String, Object> getEmployeePayslip(int employeeId, String payMonth) {
        String sql = "SELECT * FROM " + DatabaseConnection.VIEW_MONTHLY_EMPLOYEE_PAYSLIP + 
                    " WHERE `Employee ID` = ? AND DATE_FORMAT(`Period Start Date`, '%Y-%m') = ?";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, employeeId);
            stmt.setString(2, payMonth);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> payslip = new HashMap<>();
                    ResultSetMetaData metaData = rs.getMetaData();
                    int columnCount = metaData.getColumnCount();
                    
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = metaData.getColumnName(i);
                        Object value = rs.getObject(i);
                        payslip.put(columnName, value);
                    }
                    return payslip;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting employee payslip: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Get attendance records with overtime for current date and future
     * Used for overtime request page
     * @param employeeId Employee ID
     * @return List of attendance records with overtime hours
     */
    protected List<Map<String, Object>> getAttendanceWithOvertimeForRequests(int employeeId) {
        String sql = "SELECT a.attendanceId, a.date, a.timeIn, a.timeOut, " +
                    "ROUND(GREATEST(0, ((TIME_TO_SEC(a.timeOut) - TIME_TO_SEC(a.timeIn)) / 3600.0) - 9.0), 2) AS overtimeHours " +
                    "FROM " + DatabaseConnection.TABLE_ATTENDANCE + " a " +
                    "WHERE a.employeeId = ? AND a.date >= CURDATE() AND a.timeIn IS NOT NULL AND a.timeOut IS NOT NULL " +
                    "ORDER BY a.date DESC";
        
        List<Map<String, Object>> results = new ArrayList<>();
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, employeeId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> record = new HashMap<>();
                    ResultSetMetaData metaData = rs.getMetaData();
                    int columnCount = metaData.getColumnCount();
                    
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = metaData.getColumnName(i);
                        Object value = rs.getObject(i);
                        record.put(columnName, value);
                    }
                    results.add(record);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting attendance with overtime: " + e.getMessage());
        }
        
        return results;
    }
    
    /**
     * Get monthly payroll summary report data
     * @param payMonth Pay month in YYYY-MM format (optional, null for all months)
     * @return List of payroll summary records
     */
    protected List<Map<String, Object>> getMonthlyPayrollSummary(String payMonth) {
        String sql = "SELECT * FROM " + DatabaseConnection.VIEW_MONTHLY_PAYROLL_SUMMARY;
        List<Object> params = new ArrayList<>();
        
        if (payMonth != null && !payMonth.trim().isEmpty()) {
            sql += " WHERE DATE_FORMAT(`Pay Date`, '%Y-%m') = ?";
            params.add(payMonth);
        }
        
        sql += " ORDER BY `Pay Date`, `Employee ID`";
        
        return executeViewQuery(sql, params.toArray());
    }
    
    /**
     * Get government IDs for an employee
     * @param employeeId Employee ID
     * @return Map containing government IDs or null if not found
     */
    protected Map<String, String> getEmployeeGovIds(int employeeId) {
        String sql = "SELECT sss, philhealth, tin, pagibig FROM " + DatabaseConnection.TABLE_GOVID + 
                    " WHERE employeeId = ?";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, employeeId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Map<String, String> govIds = new HashMap<>();
                    govIds.put("sss", rs.getString("sss"));
                    govIds.put("philhealth", rs.getString("philhealth"));
                    govIds.put("tin", rs.getString("tin"));
                    govIds.put("pagibig", rs.getString("pagibig"));
                    return govIds;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting government IDs: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Helper method to execute view queries
     * @param sql SQL query
     * @param params Query parameters
     * @return List of result maps
     */
    private List<Map<String, Object>> executeViewQuery(String sql, Object... params) {
        List<Map<String, Object>> results = new ArrayList<>();
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> record = new HashMap<>();
                    ResultSetMetaData metaData = rs.getMetaData();
                    int columnCount = metaData.getColumnCount();
                    
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = metaData.getColumnName(i);
                        Object value = rs.getObject(i);
                        record.put(columnName, value);
                    }
                    results.add(record);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error executing view query: " + e.getMessage());
        }
        
        return results;
    }
    

    // PRIVATE HELPER METHODS - Internal methods

    
    /**
     * Builds a basic INSERT SQL statement
     * Subclasses can override this if they need custom INSERT logic
     * @return The INSERT SQL string
     */
    private String buildInsertSQL() {
        // This is a very basic implementation
        // Most subclasses will override this with their specific column names
        return "INSERT INTO " + getTableName() + " VALUES (?)";
    }
    
    /**
     * Builds a basic UPDATE SQL statement
     * Subclasses can override this if they need custom UPDATE logic
     * @return The UPDATE SQL string
     */
    private String buildUpdateSQL() {
        // This is a very basic implementation
        // Most subclasses will override this with their specific column names
        return "UPDATE " + getTableName() + " SET ? WHERE " + getPrimaryKeyColumn() + " = ?";
    }
    
    /**
     * Handles auto-generated keys after INSERT operations
     * This method sets the generated ID back on the entity object
     * @param entity The entity that was just inserted
     * @param generatedKeys The ResultSet containing the generated key
     * @throws SQLException if there's an error reading the generated key
     */
    protected void handleGeneratedKey(T entity, ResultSet generatedKeys) throws SQLException {
        // Default implementation does nothing
        // Subclasses can override this to set the generated ID on their entity
    }
    

    // CONNECTION TESTING

    
    /**
     * Tests if the database connection is working
     * This is useful for debugging connection problems
     * @return true if connection works, false if there's a problem
     */
    public boolean testConnection() {
        return databaseConnection.testConnection();
    }
    
    /**
     * Check if a table exists in the database
     * @param tableName The name of the table to check
     * @return true if table exists, false otherwise
     */
    public boolean tableExists(String tableName) {
        return databaseConnection.tableExists(tableName);
    }
    
    /**
     * Check if a view exists in the database
     * @param viewName The name of the view to check
     * @return true if view exists, false otherwise
     */
    public boolean viewExists(String viewName) {
        return databaseConnection.viewExists(viewName);
    }
}