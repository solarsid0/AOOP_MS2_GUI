package Models;

/**
 * PositionModel - Enhanced for rank-and-file detection
 * Maps to position table with department classification methods
 * @author Chad
 */
public class PositionModel {
    
    // Database fields
    private Integer positionId;
    private String position;
    private String positionDescription;
    private String department;
    
    // Constructors
    public PositionModel() {}
    
    public PositionModel(String position, String department) {
        this.position = position;
        this.department = department;
    }
    
    public PositionModel(String position, String positionDescription, String department) {
        this.position = position;
        this.positionDescription = positionDescription;
        this.department = department;
    }
    
    // ================================
    // RANK-AND-FILE DETECTION METHODS
    // ================================
    
    /**
     * Check if this position is rank-and-file
     * Uses the same logic as database views
     * @return true if rank-and-file position
     */
    public boolean isRankAndFile() {
        // Rule from database view: department = 'rank-and-file' OR position LIKE '%rank%file%'
        return (department != null && department.toLowerCase().equals("rank-and-file")) ||
               (position != null && position.toLowerCase().contains("rank") && position.toLowerCase().contains("file"));
    }
    
    /**
     * Get position classification
     * @return classification string
     */
    public String getClassification() {
        return isRankAndFile() ? "Rank-and-File" : "Non Rank-and-File";
    }
    
    /**
     * Check if position is eligible for overtime
     * Only rank-and-file positions are eligible
     * @return true if eligible for overtime
     */
    public boolean isOvertimeEligible() {
        return isRankAndFile();
    }
    
    /**
     * Check if position is management level
     * @return true if management (non rank-and-file)
     */
    public boolean isManagementLevel() {
        return !isRankAndFile();
    }
    
    /**
     * Get overtime eligibility message
     * @return eligibility message
     */
    public String getOvertimeEligibilityMessage() {
        if (isRankAndFile()) {
            return "✅ Position '" + position + "' is eligible for overtime pay";
        } else {
            return "❌ Position '" + position + "' is not eligible for overtime pay";
        }
    }
    
    // ================================
    // DEPARTMENT METHODS
    // ================================
    
    /**
     * Check if department is rank-and-file
     * @return true if department is rank-and-file
     */
    public boolean isDepartmentRankAndFile() {
        return department != null && department.toLowerCase().equals("rank-and-file");
    }
    
    /**
     * Check if position name contains rank-and-file keywords
     * @return true if position name indicates rank-and-file
     */
    public boolean isPositionNameRankAndFile() {
        return position != null && 
               position.toLowerCase().contains("rank") && 
               position.toLowerCase().contains("file");
    }
    
    /**
     * Get department display name
     * @return formatted department name
     */
    public String getDepartmentDisplayName() {
        if (department == null) {
            return "Unknown Department";
        }
        // Capitalize first letter of each word
        String[] words = department.split("-");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (result.length() > 0) {
                result.append("-");
            }
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                      .append(word.substring(1).toLowerCase());
            }
        }
        return result.toString();
    }
    
    /**
     * Get position display name
     * @return formatted position name
     */
    public String getPositionDisplayName() {
        if (position == null) {
            return "Unknown Position";
        }
        return position;
    }
    
    // ================================
    // VALIDATION METHODS
    // ================================
    
    /**
     * Validate position data
     * @return true if valid
     */
    public boolean isValid() {
        return position != null && !position.trim().isEmpty() &&
               department != null && !department.trim().isEmpty();
    }
    
    /**
     * Check if position is complete (has description)
     * @return true if has description
     */
    public boolean isComplete() {
        return isValid() && positionDescription != null && !positionDescription.trim().isEmpty();
    }
    
    // ================================
    // UTILITY METHODS
    // ================================
    
    /**
     * Get full position title
     * @return formatted title with department
     */
    public String getFullTitle() {
        return getPositionDisplayName() + " - " + getDepartmentDisplayName();
    }
    
    /**
     * Get position summary for display
     * @return summary string
     */
    public String getPositionSummary() {
        return getFullTitle() + " (" + getClassification() + ")";
    }
    
    /**
     * Create a copy of this position
     * @return new PositionModel with same data
     */
    public PositionModel copy() {
        PositionModel copy = new PositionModel();
        copy.positionId = this.positionId;
        copy.position = this.position;
        copy.positionDescription = this.positionDescription;
        copy.department = this.department;
        return copy;
    }
    
    // ================================
    // GETTERS AND SETTERS
    // ================================
    
    public Integer getPositionId() { return positionId; }
    public void setPositionId(Integer positionId) { this.positionId = positionId; }
    
    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }
    
    public String getPositionDescription() { return positionDescription; }
    public void setPositionDescription(String positionDescription) { this.positionDescription = positionDescription; }
    
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    
    // ================================
    // OBJECT METHODS
    // ================================
    
    @Override
    public String toString() {
        return "PositionModel{" +
                "positionId=" + positionId +
                ", position='" + position + '\'' +
                ", department='" + department + '\'' +
                ", classification='" + getClassification() + '\'' +
                '}';
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        PositionModel that = (PositionModel) obj;
        return positionId != null && positionId.equals(that.positionId);
    }
    
    @Override
    public int hashCode() {
        return positionId != null ? positionId.hashCode() : 0;
    }
}