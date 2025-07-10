package Models;

/**
 * Enum for payslip approval status values that match the database schema exactly
 * Used for payslip approval workflows in the payroll management system
 * 
 * Database enum values: 'PENDING', 'APPROVED', 'REJECTED'
 * Default value: 'PENDING'
 * 
 * @author User
 */
public enum PayslipStatus {
    PENDING("PENDING"),
    APPROVED("APPROVED"),
    REJECTED("REJECTED");
    
    private final String value;
    
    PayslipStatus(String value) {
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
     * Gets PayslipStatus from string value (case-insensitive)
     * @param value String value from database or user input
     * @return PayslipStatus enum or PENDING if not found
     */
    public static PayslipStatus fromValue(String value) {
        if (value == null) return PENDING;
        
        for (PayslipStatus status : PayslipStatus.values()) {
            if (status.getValue().equalsIgnoreCase(value)) {
                return status;
            }
        }
        
        return PENDING;
    }
    
    /**
     * Gets the default payslip status (matches database default)
     * @return PENDING status
     */
    public static PayslipStatus getDefault() {
        return PENDING;
    }
    
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
     * Only pending payslips can typically be modified
     * @return true if status allows modifications
     */
    public boolean allowsModification() {
        return this == PENDING;
    }
    
    /**
     * Gets the display color for UI purposes
     * @return Color code for status display
     */
    public String getDisplayColor() {
        return switch (this) {
            case PENDING -> "#FFA500";
            case APPROVED -> "#28A745";
            case REJECTED -> "#DC3545";
        };
    }
    
    /**
     * Gets the display icon for UI purposes
     * @return Icon symbol for status display
     */
    public String getDisplayIcon() {
        return switch (this) {
            case PENDING -> "⏳";
            case APPROVED -> "✅";
            case REJECTED -> "❌";
        };
    }
    
    @Override
    public String toString() {
        return value;
    }
}