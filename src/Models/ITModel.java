package Models;

import java.time.*;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * ITModel with system administration features and Manila timezone operations
 * Provides comprehensive IT management capabilities for the payroll system
 */
public class ITModel {
    private static final ZoneId MANILA_TIMEZONE = ZoneId.of("Asia/Manila");
    
    // IT Personnel Information
    private int itId;
    private String firstName;
    private String lastName;
    private String email;
    private String position;
    private String department;
    private String userRole;
    private String status;
    private Timestamp lastLogin;
    private String specializations; // "NETWORK,DATABASE,SECURITY,DEVELOPMENT"
    private String accessLevel; // "ADMIN", "SENIOR", "JUNIOR"
    
    // IT System Permissions
    private boolean canManageUsers;
    private boolean canManageDatabase;
    private boolean canManageBackups;
    private boolean canManageSecurity;
    private boolean canAccessSystemLogs;
    private boolean canModifySystemSettings;
    private boolean canManageIntegrations;
    private boolean canPerformMaintenance;
    private boolean canResetPasswords;
    private boolean canManageReports;
    private boolean canAccessAuditLogs;
    private boolean canManageSystemUpdates;
    
    // IT Activity Tracking
    private int usersManaged;
    private int backupsPerformed;
    private int securityIncidentsHandled;
    private int systemUpdatesApplied;
    private int helpTicketsResolved;
    private Timestamp lastBackupRun;
    private Timestamp lastSecurityScan;
    private Timestamp lastSystemMaintenance;
    private String currentTask;
    
    // System Health Monitoring
    private String systemStatus; // "ONLINE", "MAINTENANCE", "WARNING", "CRITICAL"
    private double cpuUsage;
    private double memoryUsage;
    private double diskUsage;
    private double networkLatency;
    private int activeConnections;
    private Timestamp lastSystemCheck;
    
    // Constructors
    public ITModel() {
        this.userRole = "IT";
        this.status = "Active";
        this.department = "Information Technology";
        this.accessLevel = "ADMIN";
        this.systemStatus = "ONLINE";
        
        // Default IT permissions for admin level
        this.canManageUsers = true;
        this.canManageDatabase = true;
        this.canManageBackups = true;
        this.canManageSecurity = true;
        this.canAccessSystemLogs = true;
        this.canModifySystemSettings = true;
        this.canManageIntegrations = true;
        this.canPerformMaintenance = true;
        this.canResetPasswords = true;
        this.canManageReports = true;
        this.canAccessAuditLogs = true;
        this.canManageSystemUpdates = true;
    }
    
    public ITModel(int itId, String firstName, String lastName, String email) {
        this();
        this.itId = itId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.position = "IT Administrator";
    }
    
    // Manila timezone operations
    
    /**
     * Get current Manila time
     * @return 
     */
    public static LocalDateTime getCurrentManilaTime() {
        return LocalDateTime.now(MANILA_TIMEZONE);
    }
    
    /**
     * Get last login in Manila timezone
     * @return 
     */
    public LocalDateTime getLastLoginInManila() {
        if (lastLogin == null) return null;
        return lastLogin.toLocalDateTime().atZone(ZoneId.systemDefault())
                .withZoneSameInstant(MANILA_TIMEZONE).toLocalDateTime();
    }
    
    /**
     * Get last backup run in Manila timezone
     * @return 
     */
    public LocalDateTime getLastBackupRunInManila() {
        if (lastBackupRun == null) return null;
        return lastBackupRun.toLocalDateTime().atZone(ZoneId.systemDefault())
                .withZoneSameInstant(MANILA_TIMEZONE).toLocalDateTime();
    }
    
    /**
     * Get last security scan in Manila timezone
     * @return 
     */
    public LocalDateTime getLastSecurityScanInManila() {
        if (lastSecurityScan == null) return null;
        return lastSecurityScan.toLocalDateTime().atZone(ZoneId.systemDefault())
                .withZoneSameInstant(MANILA_TIMEZONE).toLocalDateTime();
    }
    
    /**
     * Get last system maintenance in Manila timezone
     * @return 
     */
    public LocalDateTime getLastSystemMaintenanceInManila() {
        if (lastSystemMaintenance == null) return null;
        return lastSystemMaintenance.toLocalDateTime().atZone(ZoneId.systemDefault())
                .withZoneSameInstant(MANILA_TIMEZONE).toLocalDateTime();
    }
    
    // User Management Operations
    
    /**
     * Create new user account
     * @param employeeId
     * @param email
     * @param role
     * @param temporaryPassword
     * @return 
     */
    public boolean createUserAccount(int employeeId, String email, String role, String temporaryPassword) {
        if (!canManageUsers) {
            return false;
        }
        
        try {
            // Validate inputs
            if (employeeId <= 0 || email == null || role == null || temporaryPassword == null) {
                return false;
            }
            
            updateCurrentTask("Creating user account for employee ID: " + employeeId);
            usersManaged++;
            return true;
        } catch (Exception e) {
            System.err.println("Error creating user account: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Reset user password
     * @param employeeId
     * @param reason
     * @return 
     */
    public boolean resetUserPassword(int employeeId, String reason) {
        if (!canResetPasswords) {
            return false;
        }
        
        try {
            updateCurrentTask("Resetting password for employee ID: " + employeeId);
            return true;
        } catch (Exception e) {
            System.err.println("Error resetting password: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Lock/unlock user account
     * @param employeeId
     * @param reason
     * @param lock
     * @return 
     */
    public boolean toggleUserAccountLock(int employeeId, boolean lock, String reason) {
        if (!canManageUsers) {
            return false;
        }
        
        try {
            String action = lock ? "Locking" : "Unlocking";
            updateCurrentTask(action + " user account for employee ID: " + employeeId);
            return true;
        } catch (Exception e) {
            System.err.println("Error toggling account lock: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Update user permissions
     * @param employeeId
     * @param permissions
     * @return 
     */
    public boolean updateUserPermissions(int employeeId, Map<String, Boolean> permissions) {
        if (!canManageUsers) {
            return false;
        }
        
        try {
            updateCurrentTask("Updating permissions for employee ID: " + employeeId);
            return true;
        } catch (Exception e) {
            System.err.println("Error updating user permissions: " + e.getMessage());
            return false;
        }
    }
    
    // Database Management Operations
    
    /**
     * Perform database backup
     * @param backupType
     * @return 
     */
    public boolean performDatabaseBackup(String backupType) {
        if (!canManageBackups) {
            return false;
        }
        
        try {
            updateCurrentTask("Performing " + backupType + " database backup");
            this.lastBackupRun = Timestamp.valueOf(getCurrentManilaTime());
            this.backupsPerformed++;
            return true;
        } catch (Exception e) {
            System.err.println("Error performing backup: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Restore database from backup
     * @param backupPath
     * @param reason
     * @return 
     */
    public boolean restoreDatabaseFromBackup(String backupPath, String reason) {
        if (!canManageDatabase) {
            return false;
        }
        
        try {
            updateCurrentTask("Restoring database from backup: " + backupPath);
            return true;
        } catch (Exception e) {
            System.err.println("Error restoring database: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Optimize database performance
     * @return 
     */
    public boolean optimizeDatabase() {
        if (!canManageDatabase) {
            return false;
        }
        
        try {
            updateCurrentTask("Optimizing database performance");
            return true;
        } catch (Exception e) {
            System.err.println("Error optimizing database: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Execute database maintenance scripts
     * @param scripts
     * @return 
     */
    public boolean executeDatabaseMaintenance(List<String> scripts) {
        if (!canPerformMaintenance) {
            return false;
        }
        
        try {
            updateCurrentTask("Executing database maintenance scripts");
            this.lastSystemMaintenance = Timestamp.valueOf(getCurrentManilaTime());
            return true;
        } catch (Exception e) {
            System.err.println("Error executing maintenance: " + e.getMessage());
            return false;
        }
    }
    
    // Security Management Operations
    
    /**
     * Perform security scan
     * @param scanType
     * @return 
     */
    public boolean performSecurityScan(String scanType) {
        if (!canManageSecurity) {
            return false;
        }
        
        try {
            updateCurrentTask("Performing " + scanType + " security scan");
            this.lastSecurityScan = Timestamp.valueOf(getCurrentManilaTime());
            return true;
        } catch (Exception e) {
            System.err.println("Error performing security scan: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Handle security incident
     * @param incidentType
     * @param description
     * @param resolution
     * @return 
     */
    public boolean handleSecurityIncident(String incidentType, String description, String resolution) {
        if (!canManageSecurity) {
            return false;
        }
        
        try {
            updateCurrentTask("Handling security incident: " + incidentType);
            this.securityIncidentsHandled++;
            return true;
        } catch (Exception e) {
            System.err.println("Error handling security incident: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Update security policies
     * @param policies
     * @return 
     */
    public boolean updateSecurityPolicies(Map<String, Object> policies) {
        if (!canManageSecurity) {
            return false;
        }
        
        try {
            updateCurrentTask("Updating security policies");
            return true;
        } catch (Exception e) {
            System.err.println("Error updating security policies: " + e.getMessage());
            return false;
        }
    }
    
    // System Monitoring and Maintenance
    
    /**
     * Check system health
     * @return 
     */
    public Map<String, Object> checkSystemHealth() {
        Map<String, Object> healthCheck = new HashMap<>();
        
        try {
            updateCurrentTask("Performing system health check");
            
            // Simulate system metrics collection
            this.cpuUsage = Math.random() * 100;
            this.memoryUsage = Math.random() * 100;
            this.diskUsage = Math.random() * 100;
            this.networkLatency = Math.random() * 100;
            this.activeConnections = (int)(Math.random() * 1000);
            this.lastSystemCheck = Timestamp.valueOf(getCurrentManilaTime());
            
            // Determine system status
            if (cpuUsage > 90 || memoryUsage > 90 || diskUsage > 95) {
                this.systemStatus = "CRITICAL";
            } else if (cpuUsage > 80 || memoryUsage > 80 || diskUsage > 85) {
                this.systemStatus = "WARNING";
            } else {
                this.systemStatus = "ONLINE";
            }
            
            healthCheck.put("systemStatus", systemStatus);
            healthCheck.put("cpuUsage", cpuUsage);
            healthCheck.put("memoryUsage", memoryUsage);
            healthCheck.put("diskUsage", diskUsage);
            healthCheck.put("networkLatency", networkLatency);
            healthCheck.put("activeConnections", activeConnections);
            healthCheck.put("lastCheck", getCurrentManilaTime());
            
        } catch (Exception e) {
            System.err.println("Error checking system health: " + e.getMessage());
            healthCheck.put("error", e.getMessage());
        }
        
        return healthCheck;
    }
    
    /**
     * Schedule system maintenance
     * @param maintenanceTime
     * @param maintenanceType
     * @param durationMinutes
     * @return 
     */
    public boolean scheduleSystemMaintenance(LocalDateTime maintenanceTime, String maintenanceType, int durationMinutes) {
        if (!canPerformMaintenance) {
            return false;
        }
        
        try {
            updateCurrentTask("Scheduling " + maintenanceType + " maintenance for " + maintenanceTime);
            return true;
        } catch (Exception e) {
            System.err.println("Error scheduling maintenance: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Apply system updates
     * @param updatePackages
     * @return 
     */
    public boolean applySystemUpdates(List<String> updatePackages) {
        if (!canManageSystemUpdates) {
            return false;
        }
        
        try {
            updateCurrentTask("Applying system updates: " + updatePackages.size() + " packages");
            this.systemUpdatesApplied += updatePackages.size();
            return true;
        } catch (Exception e) {
            System.err.println("Error applying updates: " + e.getMessage());
            return false;
        }
    }
    
    // Log Management Operations
    
    /**
     * Access system logs
     * @param startDate
     * @param endDate
     * @param logLevel
     * @return 
     */
    public List<Map<String, Object>> getSystemLogs(Date startDate, Date endDate, String logLevel) {
        if (!canAccessSystemLogs) {
            return new java.util.ArrayList<>();
        }
        
        try {
            updateCurrentTask("Accessing system logs from " + startDate + " to " + endDate);
            // Return log entries (would query actual log files/database)
            return new java.util.ArrayList<>();
        } catch (Exception e) {
            System.err.println("Error accessing system logs: " + e.getMessage());
            return new java.util.ArrayList<>();
        }
    }
    
    /**
     * Clear old logs
     * @param daysToKeep
     * @return 
     */
    public boolean clearOldLogs(int daysToKeep) {
        if (!canAccessSystemLogs) {
            return false;
        }
        
        try {
            updateCurrentTask("Clearing logs older than " + daysToKeep + " days");
            return true;
        } catch (Exception e) {
            System.err.println("Error clearing logs: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Generate system report
     * @param reportType
     * @param startDate
     * @param endDate
     * @return 
     */
    public Map<String, Object> generateSystemReport(String reportType, Date startDate, Date endDate) {
        if (!canManageReports) {
            return new HashMap<>();
        }
        
        Map<String, Object> report = new HashMap<>();
        
        try {
            updateCurrentTask("Generating " + reportType + " system report");
            
            report.put("reportType", reportType);
            report.put("startDate", startDate);
            report.put("endDate", endDate);
            report.put("generatedBy", getDisplayName());
            report.put("generatedAt", getCurrentManilaTime());
            
            // System metrics
            report.put("systemHealth", checkSystemHealth());
            report.put("backupsPerformed", backupsPerformed);
            report.put("securityIncidentsHandled", securityIncidentsHandled);
            report.put("systemUpdatesApplied", systemUpdatesApplied);
            report.put("helpTicketsResolved", helpTicketsResolved);
            
        } catch (Exception e) {
            System.err.println("Error generating system report: " + e.getMessage());
        }
        
        return report;
    }
    
    // Integration Management
    
    /**
     * Manage third-party integrations
     * @param integrationType
     * @param action
     * @param parameters
     * @return 
     */
    public boolean manageIntegration(String integrationType, String action, Map<String, String> parameters) {
        if (!canManageIntegrations) {
            return false;
        }
        
        try {
            updateCurrentTask(action + " " + integrationType + " integration");
            return true;
        } catch (Exception e) {
            System.err.println("Error managing integration: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Test system integrations
     * @return 
     */
    public Map<String, Boolean> testSystemIntegrations() {
        if (!canManageIntegrations) {
            return new HashMap<>();
        }
        
        Map<String, Boolean> integrationStatus = new HashMap<>();
        
        try {
            updateCurrentTask("Testing system integrations");
            
            // Test various integrations (sample)
            integrationStatus.put("database", true);
            integrationStatus.put("email", true);
            integrationStatus.put("fileStorage", true);
            integrationStatus.put("backup", true);
            integrationStatus.put("monitoring", true);
            
        } catch (Exception e) {
            System.err.println("Error testing integrations: " + e.getMessage());
        }
        
        return integrationStatus;
    }
    
    // Dashboard and Analytics
    
    /**
     * Get IT dashboard data
     * @return 
     */
    public Map<String, Object> getITDashboard() {
        Map<String, Object> dashboard = new HashMap<>();
        
        // Personal info
        dashboard.put("itId", itId);
        dashboard.put("name", getDisplayName());
        dashboard.put("position", position);
        dashboard.put("specializations", specializations);
        dashboard.put("accessLevel", accessLevel);
        dashboard.put("lastLogin", getLastLoginInManila());
        dashboard.put("currentTask", currentTask);
        
        // System status
        dashboard.put("systemStatus", systemStatus);
        dashboard.put("systemHealth", checkSystemHealth());
        dashboard.put("lastSystemCheck", lastSystemCheck);
        
        // Activity summary
        dashboard.put("usersManaged", usersManaged);
        dashboard.put("backupsPerformed", backupsPerformed);
        dashboard.put("securityIncidentsHandled", securityIncidentsHandled);
        dashboard.put("systemUpdatesApplied", systemUpdatesApplied);
        dashboard.put("helpTicketsResolved", helpTicketsResolved);
        dashboard.put("lastBackupRun", getLastBackupRunInManila());
        dashboard.put("lastSecurityScan", getLastSecurityScanInManila());
        dashboard.put("lastSystemMaintenance", getLastSystemMaintenanceInManila());
        
        // Permissions
        Map<String, Boolean> permissions = new HashMap<>();
        permissions.put("canManageUsers", canManageUsers);
        permissions.put("canManageDatabase", canManageDatabase);
        permissions.put("canManageBackups", canManageBackups);
        permissions.put("canManageSecurity", canManageSecurity);
        permissions.put("canAccessSystemLogs", canAccessSystemLogs);
        permissions.put("canModifySystemSettings", canModifySystemSettings);
        permissions.put("canManageIntegrations", canManageIntegrations);
        permissions.put("canPerformMaintenance", canPerformMaintenance);
        permissions.put("canResetPasswords", canResetPasswords);
        permissions.put("canManageReports", canManageReports);
        permissions.put("canAccessAuditLogs", canAccessAuditLogs);
        permissions.put("canManageSystemUpdates", canManageSystemUpdates);
        dashboard.put("permissions", permissions);
        
        return dashboard;
    }
    
    /**
     * Get monthly IT metrics
     * @param month
     * @param year
     * @return 
     */
    public Map<String, Object> getMonthlyITMetrics(int month, int year) {
        Map<String, Object> metrics = new HashMap<>();
        
        try {
            updateCurrentTask("Generating monthly IT metrics for " + month + "/" + year);
            
            metrics.put("month", month);
            metrics.put("year", year);
            metrics.put("systemUptime", 99.8); // Percentage
            metrics.put("backupsCompleted", backupsPerformed);
            metrics.put("securityIncidents", securityIncidentsHandled);
            metrics.put("systemUpdates", systemUpdatesApplied);
            metrics.put("helpTicketsResolved", helpTicketsResolved);
            metrics.put("averageResponseTime", 2.5); // Hours
            metrics.put("userAccountsCreated", usersManaged);
            
        } catch (Exception e) {
            System.err.println("Error getting monthly IT metrics: " + e.getMessage());
        }
        
        return metrics;
    }
    
    // Utility methods
    
    /**
     * Update current task
     */
    private void updateCurrentTask(String task) {
        this.currentTask = task + " at " + getCurrentManilaTime().format(
            java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
    }
    
    /**
     * Get IT display name
     * @return 
     */
    public String getDisplayName() {
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        } else if (firstName != null) {
            return firstName;
        } else if (email != null) {
            return email;
        } else {
            return "IT " + itId;
        }
    }
    
    /**
     * Check if IT personnel is active
     * @return 
     */
    public boolean isActive() {
        return "Active".equalsIgnoreCase(status);
    }
    
    /**
     * Validate IT permissions
     * @return 
     */
    public boolean hasValidITPermissions() {
        return userRole != null && userRole.equals("IT") && isActive();
    }
    
    /**
     * Check if system is healthy
     * @return 
     */
    public boolean isSystemHealthy() {
        return "ONLINE".equals(systemStatus) || "WARNING".equals(systemStatus);
    }
    
    /**
     * Get system alert level
     * @return 
     */
    public String getSystemAlertLevel() {
        return switch (systemStatus) {
            case "CRITICAL" -> "HIGH";
            case "WARNING" -> "MEDIUM";
            case "MAINTENANCE" -> "LOW";
            default -> "NONE";
        };
    }
    
    // Getters and Setters
    public int getItId() { return itId; }
    public void setItId(int itId) { this.itId = itId; }
    
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }
    
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    
    public String getUserRole() { return userRole; }
    public void setUserRole(String userRole) { this.userRole = userRole; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public Timestamp getLastLogin() { return lastLogin; }
    public void setLastLogin(Timestamp lastLogin) { this.lastLogin = lastLogin; }
    
    public String getSpecializations() { return specializations; }
    public void setSpecializations(String specializations) { this.specializations = specializations; }
    
    public String getAccessLevel() { return accessLevel; }
    public void setAccessLevel(String accessLevel) { this.accessLevel = accessLevel; }
    
    // Permission getters and setters
    public boolean isCanManageUsers() { return canManageUsers; }
    public void setCanManageUsers(boolean canManageUsers) { this.canManageUsers = canManageUsers; }
    
    public boolean isCanManageDatabase() { return canManageDatabase; }
    public void setCanManageDatabase(boolean canManageDatabase) { this.canManageDatabase = canManageDatabase; }
    
    public boolean isCanManageBackups() { return canManageBackups; }
    public void setCanManageBackups(boolean canManageBackups) { this.canManageBackups = canManageBackups; }
    
    public boolean isCanManageSecurity() { return canManageSecurity; }
    public void setCanManageSecurity(boolean canManageSecurity) { this.canManageSecurity = canManageSecurity; }
    
    public boolean isCanAccessSystemLogs() { return canAccessSystemLogs; }
    public void setCanAccessSystemLogs(boolean canAccessSystemLogs) { this.canAccessSystemLogs = canAccessSystemLogs; }
    
    public boolean isCanModifySystemSettings() { return canModifySystemSettings; }
    public void setCanModifySystemSettings(boolean canModifySystemSettings) { this.canModifySystemSettings = canModifySystemSettings; }
    
    public boolean isCanManageIntegrations() { return canManageIntegrations; }
    public void setCanManageIntegrations(boolean canManageIntegrations) { this.canManageIntegrations = canManageIntegrations; }
    
    public boolean isCanPerformMaintenance() { return canPerformMaintenance; }
    public void setCanPerformMaintenance(boolean canPerformMaintenance) { this.canPerformMaintenance = canPerformMaintenance; }
    
    public boolean isCanResetPasswords() { return canResetPasswords; }
    public void setCanResetPasswords(boolean canResetPasswords) { this.canResetPasswords = canResetPasswords; }
    
    public boolean isCanManageReports() { return canManageReports; }
    public void setCanManageReports(boolean canManageReports) { this.canManageReports = canManageReports; }
    
    public boolean isCanAccessAuditLogs() { return canAccessAuditLogs; }
    public void setCanAccessAuditLogs(boolean canAccessAuditLogs) { this.canAccessAuditLogs = canAccessAuditLogs; }
    
    public boolean isCanManageSystemUpdates() { return canManageSystemUpdates; }
    public void setCanManageSystemUpdates(boolean canManageSystemUpdates) { this.canManageSystemUpdates = canManageSystemUpdates; }
    
    // Activity tracking getters and setters
    public int getUsersManaged() { return usersManaged; }
    public void setUsersManaged(int usersManaged) { this.usersManaged = usersManaged; }
    
    public int getBackupsPerformed() { return backupsPerformed; }
    public void setBackupsPerformed(int backupsPerformed) { this.backupsPerformed = backupsPerformed; }
    
    public int getSecurityIncidentsHandled() { return securityIncidentsHandled; }
    public void setSecurityIncidentsHandled(int securityIncidentsHandled) { this.securityIncidentsHandled = securityIncidentsHandled; }
    
    public int getSystemUpdatesApplied() { return systemUpdatesApplied; }
    public void setSystemUpdatesApplied(int systemUpdatesApplied) { this.systemUpdatesApplied = systemUpdatesApplied; }
    
    public int getHelpTicketsResolved() { return helpTicketsResolved; }
    public void setHelpTicketsResolved(int helpTicketsResolved) { this.helpTicketsResolved = helpTicketsResolved; }
    
    public Timestamp getLastBackupRun() { return lastBackupRun; }
    public void setLastBackupRun(Timestamp lastBackupRun) { this.lastBackupRun = lastBackupRun; }
    
    public Timestamp getLastSecurityScan() { return lastSecurityScan; }
    public void setLastSecurityScan(Timestamp lastSecurityScan) { this.lastSecurityScan = lastSecurityScan; }
    
    public Timestamp getLastSystemMaintenance() { return lastSystemMaintenance; }
    public void setLastSystemMaintenance(Timestamp lastSystemMaintenance) { this.lastSystemMaintenance = lastSystemMaintenance; }
    
    public String getCurrentTask() { return currentTask; }
    public void setCurrentTask(String currentTask) { this.currentTask = currentTask; }
    
    // System health getters and setters
    public String getSystemStatus() { return systemStatus; }
    public void setSystemStatus(String systemStatus) { this.systemStatus = systemStatus; }
    
    public double getCpuUsage() { return cpuUsage; }
    public void setCpuUsage(double cpuUsage) { this.cpuUsage = cpuUsage; }
    
    public double getMemoryUsage() { return memoryUsage; }
    public void setMemoryUsage(double memoryUsage) { this.memoryUsage = memoryUsage; }
    
    public double getDiskUsage() { return diskUsage; }
    public void setDiskUsage(double diskUsage) { this.diskUsage = diskUsage; }
    
    public double getNetworkLatency() { return networkLatency; }
    public void setNetworkLatency(double networkLatency) { this.networkLatency = networkLatency; }
    
    public int getActiveConnections() { return activeConnections; }
    public void setActiveConnections(int activeConnections) { this.activeConnections = activeConnections; }
    
    public Timestamp getLastSystemCheck() { return lastSystemCheck; }
    public void setLastSystemCheck(Timestamp lastSystemCheck) { this.lastSystemCheck = lastSystemCheck; }
    
    @Override
    public String toString() {
        return String.format("ITModel{itId=%d, name='%s', position='%s', accessLevel='%s', systemStatus='%s', usersManaged=%d}",
                itId, getDisplayName(), position, accessLevel, systemStatus, usersManaged);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ITModel that = (ITModel) obj;
        return itId == that.itId;
    }
    
    @Override
    public int hashCode() {
        return Integer.hashCode(itId);
    }
}