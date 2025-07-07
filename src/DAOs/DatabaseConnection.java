package DAOs;

// These imports handle database connections and SQL operations
import java.sql.Connection;        // Main interface for database connections
import java.sql.DriverManager;     // Factory class that creates database connections
import java.sql.SQLException;      // Exception thrown when database operations fail
import java.sql.PreparedStatement; // For prepared statements
import java.sql.ResultSet;         // For query results
import java.util.Properties;       // Key-value pairs for connection settings
import java.time.LocalDateTime;    // For Manila timezone handling
import java.time.LocalDate;        // For date operations
import java.time.ZoneId;          // For timezone operations
import java.time.ZonedDateTime;    // For timezone-aware date/time
import java.time.format.DateTimeFormatter; // For date formatting

/**
 * Enhanced database connection for MotorPH payroll system.
 * This class provides both static and instance methods for database connectivity
 * with Manila timezone support and view querying capabilities.
 * @author User
 */
public class DatabaseConnection {
    
    // Manila timezone constant for payroll system
    public static final ZoneId MANILA_TIMEZONE = ZoneId.of("Asia/Manila");
    public static final DateTimeFormatter MANILA_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter MANILA_DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    // Database table name constants
    public static final String TABLE_EMPLOYEE = "employee";
    public static final String TABLE_POSITION = "position";
    public static final String TABLE_ATTENDANCE = "attendance";
    public static final String TABLE_LEAVE_REQUEST = "leaverequest";
    public static final String TABLE_OVERTIME_REQUEST = "overtimerequest";
    public static final String TABLE_GOVID = "govid";
    public static final String TABLE_DEDUCTION = "deduction";
    public static final String TABLE_BENEFIT_TYPE = "benefittype";
    public static final String TABLE_POSITION_BENEFIT = "positionbenefit";
    public static final String TABLE_PAYROLL = "payroll";
    public static final String TABLE_PAY_PERIOD = "payperiod";
    public static final String TABLE_TARDINESS_RECORD = "tardinessrecord";
    public static final String TABLE_LEAVE_BALANCE = "leavebalance";
    public static final String TABLE_PAYSLIP = "payslip";
    
    // Database view name constants
    public static final String VIEW_MONTHLY_EMPLOYEE_PAYSLIP = "monthly_employee_payslip";
    public static final String VIEW_MONTHLY_PAYROLL_SUMMARY = "monthly_payroll_summary_report";
    
    // Work schedule constants
    public static final String WORK_START_TIME = "08:00:00";
    public static final String LATE_THRESHOLD_TIME = "08:10:00";
    public static final String WORK_END_TIME = "17:00:00";
    public static final double STANDARD_WORK_HOURS = 8.0;
    public static final double LUNCH_BREAK_HOURS = 1.0;
    public static final double OVERTIME_MULTIPLIER = 1.25;
    
    // The "address" where your database is located (server + port + database name)
    private static final String URL = "jdbc:mysql://localhost:3306/payrollsystem_db";
    
    // Username to log into MySQL (change this if your MySQL username is different)
    private static final String USER = "root"; //change depends on your MySQL server's username
    
    // Password to log into MySQL (change this to match your MySQL password)
    private static final String PASSWORD = "Mmdc_2025*"; //change based on your MySQL server's password
    
    
    // Load MySQL driver once when class is loaded
    // This is like "installing the software" that lets Java talk to MySQL
    static {
        try {
            // Tell Java where to find the MySQL driver
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("MySQL driver loaded successfully");
        } catch (ClassNotFoundException e) {
            // If the driver isn't found, give a helpful error message
            System.err.println("MySQL driver not found! Add mysql-connector-j-9.3.0.jar to your libraries folder");
            throw new RuntimeException("MySQL driver not found", e);
        }
    }
    
    //STATIC METHODS
    
    /**
     * Get database connection (static method)
     * This is the "simple way" - just call it directly
     * 
     * @return A working database connection you can use for queries
     * @throws java.sql.SQLException if connection fails (server down, wrong password, etc.)
     */
    public static Connection getConnection() throws SQLException {
        // Create a properties object to hold connection settings
        Properties props = new Properties();
        
        // Basic login credentials
        props.setProperty("user", USER);
        props.setProperty("password", PASSWORD);
        
        // Security and compatibility settings
        props.setProperty("useSSL", "false");                    // No encryption needed for local development
        props.setProperty("allowPublicKeyRetrieval", "true");    // Needed for newer MySQL versions
        props.setProperty("serverTimezone", "Asia/Manila");      // Philippine timezone for correct date/time handling
        
        // Actually create and return the connection
        return DriverManager.getConnection(URL, props);
    }
    
    /**
     * Get current Manila time as LocalDateTime
     * @return Current date and time in Manila timezone
     */
    public static LocalDateTime getCurrentManilaTime() {
        return ZonedDateTime.now(MANILA_TIMEZONE).toLocalDateTime();
    }
    
    /**
     * Get current Manila date as LocalDate
     * @return Current date in Manila timezone
     */
    public static LocalDate getCurrentManilaDate() {
        return ZonedDateTime.now(MANILA_TIMEZONE).toLocalDate();
    }
    
    /**
     * Convert LocalDateTime to Manila timezone string
     * @param dateTime The LocalDateTime to convert
     * @return Formatted string in Manila timezone
     */
    public static String formatManilaDateTime(LocalDateTime dateTime) {
        return dateTime.format(MANILA_DATETIME_FORMAT);
    }
    
    /**
     * Convert LocalDate to Manila timezone string
     * @param date The LocalDate to convert
     * @return Formatted string in Manila timezone
     */
    public static String formatManilaDate(LocalDate date) {
        return date.format(MANILA_DATE_FORMAT);
    }
    
    /**
     * Check if a date is today in Manila timezone
     * @param date The date to check
     * @return true if the date is today in Manila timezone
     */
    public static boolean isToday(LocalDate date) {
        return date.equals(getCurrentManilaDate());
    }
    
    /**
     * Check if a date is in the future (Manila timezone)
     * @param date The date to check
     * @return true if the date is after today in Manila timezone
     */
    public static boolean isFutureDate(LocalDate date) {
        return date.isAfter(getCurrentManilaDate());
    }
    
    /**
     * Check if a date is today or in the future (Manila timezone)
     * Used for leave requests and OT requests validation
     * @param date The date to check
     * @return true if the date is today or future in Manila timezone
     */
    public static boolean isTodayOrFuture(LocalDate date) {
        return !date.isBefore(getCurrentManilaDate());
    }
    
    /**
     * Validate leave request date range
     * @param startDate Leave start date
     * @param endDate Leave end date
     * @return true if valid leave date range
     */
    public static boolean isValidLeaveRange(LocalDate startDate, LocalDate endDate) {
        return startDate != null && endDate != null && 
               !startDate.isAfter(endDate) && 
               isTodayOrFuture(startDate);
    }
    
    /**
     * Validate overtime request datetime range
     * @param startDateTime Overtime start datetime
     * @param endDateTime Overtime end datetime
     * @return true if valid overtime range
     */
    public static boolean isValidOvertimeRange(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        return startDateTime != null && endDateTime != null && 
               startDateTime.isBefore(endDateTime) && 
               isTodayOrFuture(startDateTime.toLocalDate());
    }
    
    /**
     * Check if attendance time is late (after 8:10 AM)
     * @param timeIn Time in as string (HH:mm:ss format)
     * @return true if late, false if on time or early
     */
    public static boolean isLateArrival(String timeIn) {
        if (timeIn == null || timeIn.isEmpty()) {
            return false;
        }
        return timeIn.compareTo(LATE_THRESHOLD_TIME) > 0;
    }
    
    /**
     * Calculate hours worked from time in/out
     * @param timeIn Time in (HH:mm:ss format)
     * @param timeOut Time out (HH:mm:ss format)
     * @return Hours worked (excluding 1-hour lunch break)
     */
    public static double calculateHoursWorked(String timeIn, String timeOut) {
        if (timeIn == null || timeOut == null || timeIn.isEmpty() || timeOut.isEmpty()) {
            return 0.0;
        }
        
        try {
            String[] inParts = timeIn.split(":");
            String[] outParts = timeOut.split(":");
            
            int inSeconds = Integer.parseInt(inParts[0]) * 3600 + 
                           Integer.parseInt(inParts[1]) * 60 + 
                           Integer.parseInt(inParts[2]);
            
            int outSeconds = Integer.parseInt(outParts[0]) * 3600 + 
                            Integer.parseInt(outParts[1]) * 60 + 
                            Integer.parseInt(outParts[2]);
            
            double totalHours = (outSeconds - inSeconds) / 3600.0;
            return Math.max(0, totalHours - LUNCH_BREAK_HOURS); // Subtract lunch break
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
    
    /**
     * Calculate late hours from time in
     * @param timeIn Time in (HH:mm:ss format)
     * @return Hours late (0 if not late)
     */
    public static double calculateLateHours(String timeIn) {
        if (timeIn == null || timeIn.isEmpty() || !isLateArrival(timeIn)) {
            return 0.0;
        }
        
        try {
            String[] inParts = timeIn.split(":");
            String[] startParts = WORK_START_TIME.split(":");
            
            int inSeconds = Integer.parseInt(inParts[0]) * 3600 + 
                           Integer.parseInt(inParts[1]) * 60 + 
                           Integer.parseInt(inParts[2]);
            
            int startSeconds = Integer.parseInt(startParts[0]) * 3600 + 
                              Integer.parseInt(startParts[1]) * 60 + 
                              Integer.parseInt(startParts[2]);
            
            return Math.max(0, (inSeconds - startSeconds) / 3600.0);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
    
    /**
     * Get workdays (Monday-Friday) count between two dates
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return Number of workdays (weekdays only)
     */
    public static int getWorkdaysCount(LocalDate startDate, LocalDate endDate) {
        int workdays = 0;
        LocalDate current = startDate;
        
        while (!current.isAfter(endDate)) {
            // Check if current day is Monday-Friday (1-5)
            if (current.getDayOfWeek().getValue() >= 1 && current.getDayOfWeek().getValue() <= 5) {
                workdays++;
            }
            current = current.plusDays(1);
        }
        
        return workdays;
    }
    
    /**
     * Check if a date is a workday (Monday-Friday)
     * @param date The date to check
     * @return true if the date is Monday-Friday
     */
    public static boolean isWorkday(LocalDate date) {
        int dayOfWeek = date.getDayOfWeek().getValue();
        return dayOfWeek >= 1 && dayOfWeek <= 5; // Monday=1, Friday=5
    }
    
    ///////////////////////////////////////////////////////////////////////////
    
    // INSTANCE METHODS (for BaseDAO compatibility)

    // Instance variables - each DatabaseConnection object has its own copy
    private String url;        // Database URL for this specific instance
    private String username;   // Username for this specific instance
    private String password;   // Password for this specific instance
    
    /**
     * Default constructor
     * Uses the same settings as the static methods (URL, USER, PASSWORD)
     * Most of the time, you'll use this one
     */
    public DatabaseConnection() {
        this.url = URL;           // Use the default database URL
        this.username = USER;     // Use the default username
        this.password = PASSWORD; // Use the default password
    }
    
    /**
     * Custom constructor
     * Use this if you need to connect to a different database or use different credentials
     * Useful for testing or if you have multiple database environments
     * 
     * @param url The database URL (like "jdbc:mysql://localhost:3306/payrollsystem_db")
     * @param username The database username
     * @param password The database password
     */
    public DatabaseConnection(String url, String username, String password) {
        this.url = url;           // Store the custom database URL
        this.username = username; // Store the custom username
        this.password = password; // Store the custom password
    }
    
    /**
     * Get database connection (instance method) 
     * This creates a connection using this object's stored settings
     * Called "createConnection" instead of "getConnection" to avoid conflicts with the static method
     * 
     * @return A working database connection you can use for queries
     * @throws java.sql.SQLException if connection fails
     */
    public Connection createConnection() throws SQLException {
        // Create a properties object to hold connection settings
        Properties props = new Properties();
        
        // Use this instance's stored credentials
        props.setProperty("user", username);
        props.setProperty("password", password);
        
        // Same security and compatibility settings as the static method
        props.setProperty("useSSL", "false");                    // No encryption for local development
        props.setProperty("allowPublicKeyRetrieval", "true");    // Compatibility with newer MySQL
        props.setProperty("serverTimezone", "Asia/Manila");      // Philippine timezone
        
        // Create and return the connection using this instance's URL
        return DriverManager.getConnection(url, props);
    }
    
    /**
     * Test connection
     * Quick way to check if the database is reachable and credentials work
     * Like "pinging" the database to see if it responds
     * 
     * @return true if connection works, false if there's a problem
     */
    public boolean testConnection() {
        try (Connection conn = createConnection()) {
            // Try to create a connection and test if it's valid (5 second timeout)
            return conn != null && conn.isValid(5);
        } catch (SQLException e) {
            // If anything goes wrong, return false
            return false;
        }
        // The "try-with-resources" automatically closes the connection when done
    }
    
    /**
     * Check if a table or view exists in the database
     * @param tableName The name of the table or view to check
     * @return true if the table/view exists, false otherwise
     */
    public boolean tableExists(String tableName) {
        String sql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = ? AND table_name = ?";
        try (Connection conn = createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            // Extract database name from URL
            String dbName = url.substring(url.lastIndexOf('/') + 1);
            stmt.setString(1, dbName);
            stmt.setString(2, tableName);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error checking table existence: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Check if a view exists in the database
     * @param viewName The name of the view to check
     * @return true if the view exists, false otherwise
     */
    public boolean viewExists(String viewName) {
        String sql = "SELECT COUNT(*) FROM information_schema.views WHERE table_schema = ? AND table_name = ?";
        try (Connection conn = createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            // Extract database name from URL
            String dbName = url.substring(url.lastIndexOf('/') + 1);
            stmt.setString(1, dbName);
            stmt.setString(2, viewName);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error checking view existence: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Get column names for a table or view
     * @param tableName The name of the table or view
     * @return Array of column names, or empty array if not found
     */
    public String[] getColumnNames(String tableName) {
        String sql = "SELECT column_name FROM information_schema.columns WHERE table_schema = ? AND table_name = ? ORDER BY ordinal_position";
        try (Connection conn = createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            // Extract database name from URL
            String dbName = url.substring(url.lastIndexOf('/') + 1);
            stmt.setString(1, dbName);
            stmt.setString(2, tableName);
            
            java.util.List<String> columns = new java.util.ArrayList<>();
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    columns.add(rs.getString("column_name"));
                }
            }
            return columns.toArray(String[]::new);
        } catch (SQLException e) {
            System.err.println("Error getting column names: " + e.getMessage());
            return new String[0];
        }
    }
    
}