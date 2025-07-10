package Models;

import DAOs.PositionDAO;
import DAOs.DatabaseConnection;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.sql.Timestamp;

/**
 * EmployeeModel - Enhanced with rank-and-file detection using position table
 * Implements Manila timezone support and business logic validation
 * @author Chad
 */
public class EmployeeModel {
    
    // Manila timezone constant
    private static final ZoneId MANILA_TIMEZONE = ZoneId.of("Asia/Manila");
    
    // Database fields
    private Integer employeeId;
    private String firstName;
    private String lastName;
    private LocalDate birthDate;
    private String phoneNumber;
    private String email;
    private BigDecimal basicSalary;
    private BigDecimal hourlyRate;
    private String userRole;
    private String passwordHash;
    private EmployeeStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Timestamp lastLogin;
    private Integer positionId;
    private Integer supervisorId;
    
    // DAO for position lookup
    private PositionDAO positionDAO;
    
    // Employee Status Enum
    public enum EmployeeStatus {
        PROBATIONARY("Probationary"),
        REGULAR("Regular"),
        TERMINATED("Terminated");
        
        private final String value;
        
        EmployeeStatus(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
        
        public static EmployeeStatus fromString(String status) {
            for (EmployeeStatus s : EmployeeStatus.values()) {
                if (s.value.equalsIgnoreCase(status)) {
                    return s;
                }
            }
            return PROBATIONARY; // Default
        }
    }
    
    // Constructors
    public EmployeeModel() {
        this.status = EmployeeStatus.PROBATIONARY;
        this.userRole = "Employee";
        this.createdAt = getCurrentManilaTime();
        this.updatedAt = getCurrentManilaTime();
        
        // Initialize DAO
        DatabaseConnection dbConnection = new DatabaseConnection();
        this.positionDAO = new PositionDAO(dbConnection);
    }
    
    public EmployeeModel(String firstName, String lastName, String email, String userRole) {
        this();
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.userRole = userRole;
    }
    
    // ================================
    // RANK-AND-FILE BUSINESS LOGIC
    // ================================
    
    /**
     * Check if employee is rank-and-file using position table
     * CRITICAL: Uses database view business logic
     * @return true if employee is rank-and-file
     */
    public boolean isRankAndFile() {
        if (this.positionId == null) {
            return false;
        }
        
        try {
            // Query position table through DAO to get department and position
            PositionModel position = positionDAO.findById(this.positionId);
            if (position == null) {
                return false;
            }
            
            String dept = position.getDepartment();
            String pos = position.getPosition();
            
            // Rule from database view: department = 'rank-and-file' OR position LIKE '%rank%file%'
            return (dept != null && dept.toLowerCase().equals("rank-and-file")) ||
                   (pos != null && pos.toLowerCase().contains("rank") && pos.toLowerCase().contains("file"));
                   
        } catch (Exception e) {
            System.err.println("Error checking rank-and-file status for employee " + employeeId + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get employee classification for payroll processing
     * @return String classification
     */
    public String getEmployeeClassification() {
        return isRankAndFile() ? "Rank-and-File" : "Non Rank-and-File";
    }
    
    /**
     * Check if employee is eligible for overtime
     * Only rank-and-file employees are eligible for overtime
     * @return true if eligible for overtime
     */
    public boolean isEligibleForOvertime() {
        return isRankAndFile();
    }
    
    /**
     * Get overtime eligibility message
     * @return eligibility message
     */
    public String getOvertimeEligibilityMessage() {
        if (isRankAndFile()) {
            return "✅ " + getFullName() + " (Rank-and-File) is eligible for overtime pay";
        } else {
            return "❌ " + getFullName() + " (Non Rank-and-File) is not eligible for overtime pay";
        }
    }
    
    // ================================
    // MANILA TIMEZONE OPERATIONS
    // ================================
    
    /**
     * Get current Manila time
     * @return current time in Manila timezone
     */
    public LocalDateTime getCurrentManilaTime() {
        return LocalDateTime.now(MANILA_TIMEZONE);
    }
    
    /**
     * Check if given date is valid for leave requests (today or future)
     * @param requestDate date to check
     * @return true if valid
     */
    public boolean isValidLeaveDate(LocalDate requestDate) {
        LocalDate today = LocalDate.now(MANILA_TIMEZONE);
        return !requestDate.isBefore(today);
    }
    
    /**
     * Update the lastLogin to current Manila time
     */
    public void updateLastLogin() {
        this.lastLogin = Timestamp.valueOf(getCurrentManilaTime());
        this.updatedAt = getCurrentManilaTime();
    }
    
    // ================================
    // VALIDATION METHODS
    // ================================
    
    /**
     * Validate employee data
     * @return true if valid
     */
    public boolean isValid() {
        return firstName != null && !firstName.trim().isEmpty() &&
               lastName != null && !lastName.trim().isEmpty() &&
               email != null && !email.trim().isEmpty() &&
               basicSalary != null && basicSalary.compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * Check if employee is active
     * @return true if employee status is not TERMINATED
     */
    public boolean isActive() {
        return status != EmployeeStatus.TERMINATED;
    }
    
    /**
     * Check if employee can access the system
     * @return true if active and has valid credentials
     */
    public boolean canLogin() {
        return isActive() && email != null && passwordHash != null;
    }
    
    // ================================
    // UTILITY METHODS
    // ================================
    
    /**
     * Get full name
     * @return formatted full name
     */
    public String getFullName() {
        if (firstName == null && lastName == null) {
            return "Unknown Employee";
        }
        return (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
    }
    
    /**
     * Get display name with ID
     * @return formatted name with ID
     */
    public String getDisplayName() {
        return getFullName() + " (" + (employeeId != null ? employeeId : "New") + ")";
    }
    
    /**
     * Calculate daily rate from hourly rate
     * @return daily rate (8 hours)
     */
    public BigDecimal getDailyRate() {
        if (hourlyRate == null) {
            return BigDecimal.ZERO;
        }
        return hourlyRate.multiply(new BigDecimal("8"));
    }
    
    /**
     * Calculate hourly rate from basic salary (assuming 22 working days)
     * @return calculated hourly rate
     */
    public BigDecimal calculateHourlyRate() {
        if (basicSalary == null) {
            return BigDecimal.ZERO;
        }
        // basicSalary / 22 days / 8 hours
        return basicSalary.divide(new BigDecimal("176"), 2, BigDecimal.ROUND_HALF_UP);
    }
    
    /**
     * Update timestamps
     */
    public void updateTimestamps() {
        this.updatedAt = getCurrentManilaTime();
        if (this.createdAt == null) {
            this.createdAt = getCurrentManilaTime();
        }
    }
    
    // ================================
    // GETTERS AND SETTERS
    // ================================
    
    public Integer getEmployeeId() { return employeeId; }
    public void setEmployeeId(Integer employeeId) { this.employeeId = employeeId; }
    
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { 
        this.firstName = firstName;
        updateTimestamps();
    }
    
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { 
        this.lastName = lastName;
        updateTimestamps();
    }
    
    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { 
        this.birthDate = birthDate;
        updateTimestamps();
    }
    
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { 
        this.phoneNumber = phoneNumber;
        updateTimestamps();
    }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { 
        this.email = email;
        updateTimestamps();
    }
    
    public BigDecimal getBasicSalary() { return basicSalary; }
    public void setBasicSalary(BigDecimal basicSalary) { 
        this.basicSalary = basicSalary;
        // Auto-calculate hourly rate when basic salary is set
        if (basicSalary != null && this.hourlyRate == null) {
            this.hourlyRate = calculateHourlyRate();
        }
        updateTimestamps();
    }
    
    public BigDecimal getHourlyRate() { return hourlyRate; }
    public void setHourlyRate(BigDecimal hourlyRate) { 
        this.hourlyRate = hourlyRate;
        updateTimestamps();
    }
    
    public String getUserRole() { return userRole; }
    public void setUserRole(String userRole) { 
        this.userRole = userRole;
        updateTimestamps();
    }
    
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { 
        this.passwordHash = passwordHash;
        updateTimestamps();
    }
    
    public EmployeeStatus getStatus() { return status; }
    public void setStatus(EmployeeStatus status) { 
        this.status = status;
        updateTimestamps();
    }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public Timestamp getLastLogin() { return lastLogin; }
    public void setLastLogin(Timestamp lastLogin) { this.lastLogin = lastLogin; }
    
    public Integer getPositionId() { return positionId; }
    public void setPositionId(Integer positionId) { 
        this.positionId = positionId;
        updateTimestamps();
    }
    
    public Integer getSupervisorId() { return supervisorId; }
    public void setSupervisorId(Integer supervisorId) { 
        this.supervisorId = supervisorId;
        updateTimestamps();
    }
    
    // ================================
    // OBJECT METHODS
    // ================================
    
    @Override
    public String toString() {
        return "EmployeeModel{" +
                "employeeId=" + employeeId +
                ", name='" + getFullName() + '\'' +
                ", email='" + email + '\'' +
                ", classification='" + getEmployeeClassification() + '\'' +
                ", status=" + status +
                ", userRole='" + userRole + '\'' +
                '}';
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        EmployeeModel that = (EmployeeModel) obj;
        return employeeId != null && employeeId.equals(that.employeeId);
    }
    
    @Override
    public int hashCode() {
        return employeeId != null ? employeeId.hashCode() : 0;
    }
}