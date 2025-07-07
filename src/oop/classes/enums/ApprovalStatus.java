package oop.classes.enums;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Enum for approval status values that match the database schema exactly
 * Used for leave requests, overtime requests, and other approval workflows
 * 
 * Database enum values: 'Pending', 'Approved', 'Rejected'
 * Default value: 'Pending'
 * 
 * 
 * @author User
 */
public enum ApprovalStatus {
    PENDING("Pending"),
    APPROVED("Approved"),
    REJECTED("Rejected");
    
    private final String value;
    
    // Manila timezone for timestamp operations
    public static final ZoneId MANILA_TIMEZONE = ZoneId.of("Asia/Manila");
    
    ApprovalStatus(String value) {
        this.value = value;
    }
    
    /**
     * Gets the string value that matches the database enum exactly
     * @return String value for database operations
     */
    public String getValue() {
        return value;
    }
    
    /**
     * Gets ApprovalStatus from string value (case-insensitive)
     * @param value String value from database or user input
     * @return ApprovalStatus enum or null if not found
     */
    public static ApprovalStatus fromValue(String value) {
        if (value == null) return null;
        
        // Try exact match first
        for (ApprovalStatus status : ApprovalStatus.values()) {
            if (status.getValue().equals(value)) {
                return status;
            }
        }
        
        // Try case-insensitive match
        for (ApprovalStatus status : ApprovalStatus.values()) {
            if (status.getValue().equalsIgnoreCase(value)) {
                return status;
            }
        }
        
        return null;
    }
    
    /**
     * Gets ApprovalStatus from string value with default fallback
     * @param value String value from database or user input
     * @param defaultStatus Default status to return if value is invalid
     * @return ApprovalStatus enum or defaultStatus if not found
     */
    public static ApprovalStatus fromValueWithDefault(String value, ApprovalStatus defaultStatus) {
        ApprovalStatus status = fromValue(value);
        return status != null ? status : defaultStatus;
    }
    
    /**
     * Gets the default approval status (matches database default)
     * @return PENDING status
     */
    public static ApprovalStatus getDefault() {
        return PENDING;
    }
    
    /**
     * Gets all possible approval status values as strings
     * @return Array of all status values
     */
    public static String[] getAllValues() {
        return Arrays.stream(values())
                     .map(ApprovalStatus::getValue)
                     .toArray(String[]::new);
    }
    
    /**
     * Gets all ApprovalStatus values as a list
     * @return List of all ApprovalStatus values
     */
    public static List<ApprovalStatus> getAllStatuses() {
        return Arrays.asList(values());
    }
    

    // STATUS CHECK METHODS
    
    /**
     * Checks if the status represents an approved state
     * @return true if approved, false otherwise
     */
    public boolean isApproved() {
        return this == APPROVED;
    }
    
    /**
     * Checks if the status represents a pending state
     * @return true if pending, false otherwise
     */
    public boolean isPending() {
        return this == PENDING;
    }
    
    /**
     * Checks if the status represents a rejected state
     * @return true if rejected, false otherwise
     */
    public boolean isRejected() {
        return this == REJECTED;
    }
    
    /**
     * Checks if the status represents a final state (not pending)
     * @return true if approved or rejected, false if pending
     */
    public boolean isFinal() {
        return this == APPROVED || this == REJECTED;
    }
    
    /**
     * Checks if the status allows for modifications
     * Only pending requests can typically be modified
     * @return true if status allows modifications
     */
    public boolean allowsModification() {
        return this == PENDING;
    }
    
    /**
     * Checks if the status requires supervisor action
     * @return true if status requires supervisor review
     */
    public boolean requiresSupervisorAction() {
        return this == PENDING;
    }
    
    /**
     * Checks if the status affects payroll calculations
     * Only approved requests affect payroll
     * @return true if status affects payroll
     */
    public boolean affectsPayroll() {
        return this == APPROVED;
    }
    
    // BUSINESS LOGIC METHODS
    
    /**
     * Validates if a status transition is allowed
     * @param newStatus The status to transition to
     * @return true if transition is allowed
     */
    public boolean canTransitionTo(ApprovalStatus newStatus) {
        if (newStatus == null) return false;
        
        return switch (this) {
            case PENDING -> true;
            case APPROVED -> newStatus == PENDING;
            case REJECTED -> newStatus == PENDING;
            default -> false;
        }; // Pending can transition to any status
        // Approved can only be changed back to pending (for corrections)
        // Rejected can only be changed back to pending (for reconsideration)
    }
    
    /**
     * Gets the next logical status in the approval workflow
     * @return Next status or null if no logical next status
     */
    public ApprovalStatus getNextStatus() {
        return switch (this) {
            case PENDING -> APPROVED;
            case APPROVED, REJECTED -> null;
            default -> null;
        }; // Most common next step
        // Final states
    }
    
    /**
     * Gets the display color for UI purposes
     * @return Color code or name for status display
     */
    public String getDisplayColor() {
        return switch (this) {
            case PENDING -> "#FFA500";
            case APPROVED -> "#28A745";
            case REJECTED -> "#DC3545";
            default -> "#6C757D";
        }; // Orange
        // Green
        // Red
        // Gray
    }
    
    /**
     * Gets the display icon for UI purposes
     * @return Icon name or symbol for status display
     */
    public String getDisplayIcon() {
        return switch (this) {
            case PENDING -> "⏳";
            case APPROVED -> "✅";
            case REJECTED -> "❌";
            default -> "❓";
        }; // Hourglass
        // Check mark
        // X mark
        // Question mark
    }
    
    /**
     * Gets the display priority for sorting
     * Lower numbers have higher priority
     * @return Priority number for sorting
     */
    public int getDisplayPriority() {
        return switch (this) {
            case PENDING -> 1;
            case APPROVED -> 2;
            case REJECTED -> 3;
            default -> 4;
        }; // Highest priority (needs action)
        // Medium priority
        // Lowest priority
    }
    
    /**
     * Gets a detailed description of the status
     * @return Detailed description for user understanding
     */
    public String getDescription() {
        return switch (this) {
            case PENDING -> "Request is awaiting supervisor approval";
            case APPROVED -> "Request has been approved and will be processed";
            case REJECTED -> "Request has been rejected and will not be processed";
            default -> "Unknown status";
        };
    }
    
    /**
     * Gets the action required for this status
     * @return Action description
     */
    public String getRequiredAction() {
        return switch (this) {
            case PENDING -> "Awaiting supervisor review";
            case APPROVED -> "No action required - approved";
            case REJECTED -> "No action required - rejected";
            default -> "Unknown action";
        };
    }
    

    // PAYROLL INTEGRATION METHODS
    
    /**
     * Checks if leave request with this status should be included in payroll
     * @return true if leave should be paid
     */
    public boolean shouldIncludeLeaveInPayroll() {
        return this == APPROVED;
    }
    
    /**
     * Checks if overtime request with this status should be included in payroll
     * @return true if overtime should be paid
     */
    public boolean shouldIncludeOvertimeInPayroll() {
        return this == APPROVED;
    }
    
    /**
     * Gets the payroll impact description
     * @return Description of how this status affects payroll
     */
    public String getPayrollImpact() {
        return switch (this) {
            case PENDING -> "Not included in payroll calculations until approved";
            case APPROVED -> "Included in payroll calculations";
            case REJECTED -> "Not included in payroll calculations";
            default -> "Unknown payroll impact";
        };
    }
    
    // WORKFLOW METHODS
    
    /**
     * Creates an approval workflow step
     * @param timestamp When the status was set
     * @param approverName Who set the status
     * @param notes Any notes about the status change
     * @return WorkflowStep object
     */
    public WorkflowStep createWorkflowStep(LocalDateTime timestamp, String approverName, String notes) {
        return new WorkflowStep(this, timestamp, approverName, notes);
    }
    
    /**
     * Creates a workflow step with current Manila time
     * @param approverName Who set the status
     * @param notes Any notes about the status change
     * @return WorkflowStep object
     */
    public WorkflowStep createWorkflowStep(String approverName, String notes) {
        LocalDateTime manilaTime = ZonedDateTime.now(MANILA_TIMEZONE).toLocalDateTime();
        return createWorkflowStep(manilaTime, approverName, notes);
    }
    
    /**
     * Validates if the current user can change the status
     * @param userRole Role of the user attempting to change status
     * @return true if user has permission to change status
     */
    public boolean canBeChangedByUser(String userRole) {
        if (userRole == null) return false;
        
        return switch (this) {
            case PENDING -> userRole.contains("SUPERVISOR") || 
                userRole.contains("HR") ||
                userRole.contains("MANAGER");
            case APPROVED, REJECTED -> userRole.contains("HR") || 
                userRole.contains("IMMEDIATESUPERVISOR");
            default -> false;
        }; // Pending can be approved/rejected by supervisors or HR
        // Final statuses can only be changed by HR or higher authority
    }
    
    /**
     * Gets the minimum user role required to set this status
     * @return Minimum required user role
     */
    public String getMinimumRequiredRole() {
        return switch (this) {
            case PENDING -> "EMPLOYEE";
            case APPROVED, REJECTED -> "SUPERVISOR";
            default -> "ADMIN";
        }; // Anyone can create a pending request
        // Supervisors can approve/reject
    }
    

    // VALIDATION METHODS
    
    /**
     * Validates if this status is appropriate for a leave request
     * @return true if valid for leave requests
     */
    public boolean isValidForLeaveRequest() {
        return true; // All statuses are valid for leave requests
    }
    
    /**
     * Validates if this status is appropriate for an overtime request
     * @return true if valid for overtime requests
     */
    public boolean isValidForOvertimeRequest() {
        return true; // All statuses are valid for overtime requests
    }
    
    /**
     * Validates if the status is consistent with the database schema
     * @return true if status matches database enum values
     */
    public boolean isValidDatabaseValue() {
        return this.value.equals("Pending") || 
               this.value.equals("Approved") || 
               this.value.equals("Rejected");
    }
    
    @Override
    public String toString() {
        return value;
    }
    
    /**
     * Returns a formatted string with status and description
     * @return Formatted status string
     */
    public String toDisplayString() {
        return String.format("%s %s - %s", getDisplayIcon(), getValue(), getDescription());
    }
    

    // WORKFLOW STEP INNER CLASS
    /**
     * Represents a step in the approval workflow
     */
    public static class WorkflowStep {
        private final ApprovalStatus status;
        private final LocalDateTime timestamp;
        private final String approverName;
        private final String notes;
        
        public WorkflowStep(ApprovalStatus status, LocalDateTime timestamp, String approverName, String notes) {
            this.status = status;
            this.timestamp = timestamp;
            this.approverName = approverName;
            this.notes = notes;
        }
        
        public ApprovalStatus getStatus() {
            return status;
        }
        
        public LocalDateTime getTimestamp() {
            return timestamp;
        }
        
        public String getApproverName() {
            return approverName;
        }
        
        public String getNotes() {
            return notes;
        }
        
        @Override
        public String toString() {
            return String.format("WorkflowStep{status=%s, timestamp=%s, approver='%s', notes='%s'}", 
                               status, timestamp, approverName, notes);
        }
    }
}