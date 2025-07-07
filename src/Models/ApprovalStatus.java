package Models;

/**
 * ApprovalStatus - Enum that matches database exactly
 * Used for leave requests and overtime requests
 * @author Chad
 */
public enum ApprovalStatus {
    PENDING("Pending"),
    APPROVED("Approved"), 
    REJECTED("Rejected");
    
    private final String value;
    
    ApprovalStatus(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    /**
     * Convert string to ApprovalStatus
     * @param status string value from database
     * @return corresponding ApprovalStatus
     */
    public static ApprovalStatus fromString(String status) {
        if (status == null) {
            return PENDING; // Default
        }
        
        for (ApprovalStatus s : ApprovalStatus.values()) {
            if (s.value.equalsIgnoreCase(status.trim())) {
                return s;
            }
        }
        return PENDING; // Default fallback
    }
    
    /**
     * Check if status is pending
     * @return true if pending
     */
    public boolean isPending() {
        return this == PENDING;
    }
    
    /**
     * Check if status is approved
     * @return true if approved
     */
    public boolean isApproved() {
        return this == APPROVED;
    }
    
    /**
     * Check if status is rejected
     * @return true if rejected
     */
    public boolean isRejected() {
        return this == REJECTED;
    }
    
    /**
     * Check if status is processed (approved or rejected)
     * @return true if processed
     */
    public boolean isProcessed() {
        return this == APPROVED || this == REJECTED;
    }
    
    /**
     * Get status color for UI
     * @return color string
     */
    public String getStatusColor() {
        switch (this) {
            case PENDING: return "orange";
            case APPROVED: return "green";
            case REJECTED: return "red";
            default: return "gray";
        }
    }
    
    /**
     * Get status icon for UI
     * @return icon string
     */
    public String getStatusIcon() {
        switch (this) {
            case PENDING: return "⏳";
            case APPROVED: return "✅";
            case REJECTED: return "❌";
            default: return "❓";
        }
    }
    
    /**
     * Get display string with icon
     * @return formatted display string
     */
    public String getDisplayString() {
        return getStatusIcon() + " " + getValue();
    }
    
    @Override
    public String toString() {
        return value;
    }
}