package Models;

import java.util.regex.Pattern;

/**
 * GovIdModel - Enhanced with validation for Philippine Government IDs
 * Maps to govid table with proper validation for SSS, PhilHealth, TIN, Pag-Ibig
 * @author Chad
 */
public class GovIdModel {
    
    // Database fields
    private Integer govId;
    private String sss;
    private String philhealth;
    private String tin;
    private String pagibig;
    private Integer employeeId;
    
    // Validation patterns for Philippine Government IDs
    private static final Pattern SSS_PATTERN = Pattern.compile("\\d{2}-\\d{7}-\\d{1}");
    private static final Pattern PHILHEALTH_PATTERN = Pattern.compile("\\d{2}-\\d{9}-\\d{1}");
    private static final Pattern TIN_PATTERN = Pattern.compile("\\d{3}-\\d{3}-\\d{3}-\\d{3}");
    private static final Pattern PAGIBIG_PATTERN = Pattern.compile("\\d{4}-\\d{4}-\\d{4}");
    
    // Alternative patterns (numbers only)
    private static final Pattern SSS_NUMBERS_PATTERN = Pattern.compile("\\d{10}");
    private static final Pattern PHILHEALTH_NUMBERS_PATTERN = Pattern.compile("\\d{12}");
    private static final Pattern TIN_NUMBERS_PATTERN = Pattern.compile("\\d{9,12}");
    private static final Pattern PAGIBIG_NUMBERS_PATTERN = Pattern.compile("\\d{12}");
    
    // Constructors
    public GovIdModel() {}
    
    public GovIdModel(Integer employeeId) {
        this.employeeId = employeeId;
    }
    
    public GovIdModel(Integer employeeId, String sss, String philhealth, String tin, String pagibig) {
        this.employeeId = employeeId;
        this.sss = sss;
        this.philhealth = philhealth;
        this.tin = tin;
        this.pagibig = pagibig;
    }
    
    // ================================
    // SSS VALIDATION METHODS
    // ================================
    
    /**
     * Validate SSS number format
     * @return true if valid SSS format
     */
    public boolean isValidSSS() {
        if (sss == null || sss.trim().isEmpty()) {
            return false;
        }
        
        String cleanSSS = sss.trim();
        return SSS_PATTERN.matcher(cleanSSS).matches() || 
               SSS_NUMBERS_PATTERN.matcher(cleanSSS).matches();
    }
    
    /**
     * Format SSS number to standard format
     * @return formatted SSS number
     */
    public String getFormattedSSS() {
        if (sss == null || sss.trim().isEmpty()) {
            return "";
        }
        
        String numbersOnly = sss.replaceAll("\\D", "");
        if (numbersOnly.length() == 10) {
            return numbersOnly.substring(0, 2) + "-" + 
                   numbersOnly.substring(2, 9) + "-" + 
                   numbersOnly.substring(9);
        }
        return sss; // Return original if can't format
    }
    
    /**
     * Get SSS validation message
     * @return validation message
     */
    public String getSSSValidationMessage() {
        if (sss == null || sss.trim().isEmpty()) {
            return "SSS number is required";
        } else if (!isValidSSS()) {
            return "Invalid SSS format. Should be XX-XXXXXXX-X or 10 digits";
        } else {
            return "✅ Valid SSS number";
        }
    }
    
    // ================================
    // PHILHEALTH VALIDATION METHODS
    // ================================
    
    /**
     * Validate PhilHealth number format
     * @return true if valid PhilHealth format
     */
    public boolean isValidPhilHealth() {
        if (philhealth == null || philhealth.trim().isEmpty()) {
            return false;
        }
        
        String cleanPhilHealth = philhealth.trim();
        return PHILHEALTH_PATTERN.matcher(cleanPhilHealth).matches() || 
               PHILHEALTH_NUMBERS_PATTERN.matcher(cleanPhilHealth).matches();
    }
    
    /**
     * Format PhilHealth number to standard format
     * @return formatted PhilHealth number
     */
    public String getFormattedPhilHealth() {
        if (philhealth == null || philhealth.trim().isEmpty()) {
            return "";
        }
        
        String numbersOnly = philhealth.replaceAll("\\D", "");
        if (numbersOnly.length() == 12) {
            return numbersOnly.substring(0, 2) + "-" + 
                   numbersOnly.substring(2, 11) + "-" + 
                   numbersOnly.substring(11);
        }
        return philhealth; // Return original if can't format
    }
    
    /**
     * Get PhilHealth validation message
     * @return validation message
     */
    public String getPhilHealthValidationMessage() {
        if (philhealth == null || philhealth.trim().isEmpty()) {
            return "PhilHealth number is required";
        } else if (!isValidPhilHealth()) {
            return "Invalid PhilHealth format. Should be XX-XXXXXXXXX-X or 12 digits";
        } else {
            return "✅ Valid PhilHealth number";
        }
    }
    
    // ================================
    // TIN VALIDATION METHODS
    // ================================
    
    /**
     * Validate TIN number format
     * @return true if valid TIN format
     */
    public boolean isValidTIN() {
        if (tin == null || tin.trim().isEmpty()) {
            return false;
        }
        
        String cleanTIN = tin.trim();
        return TIN_PATTERN.matcher(cleanTIN).matches() || 
               TIN_NUMBERS_PATTERN.matcher(cleanTIN).matches();
    }
    
    /**
     * Format TIN number to standard format
     * @return formatted TIN number
     */
    public String getFormattedTIN() {
        if (tin == null || tin.trim().isEmpty()) {
            return "";
        }
        
        String numbersOnly = tin.replaceAll("\\D", "");
        if (numbersOnly.length() >= 9) {
            String base = numbersOnly.substring(0, 9);
            String formatted = base.substring(0, 3) + "-" + 
                              base.substring(3, 6) + "-" + 
                              base.substring(6, 9);
            
            // Add branch code if available
            if (numbersOnly.length() >= 12) {
                formatted += "-" + numbersOnly.substring(9, 12);
            }
            return formatted;
        }
        return tin; // Return original if can't format
    }
    
    /**
     * Get TIN validation message
     * @return validation message
     */
    public String getTINValidationMessage() {
        if (tin == null || tin.trim().isEmpty()) {
            return "TIN is required";
        } else if (!isValidTIN()) {
            return "Invalid TIN format. Should be XXX-XXX-XXX-XXX or 9-12 digits";
        } else {
            return "✅ Valid TIN";
        }
    }
    
    // ================================
    // PAG-IBIG VALIDATION METHODS
    // ================================
    
    /**
     * Validate Pag-Ibig number format
     * @return true if valid Pag-Ibig format
     */
    public boolean isValidPagIbig() {
        if (pagibig == null || pagibig.trim().isEmpty()) {
            return false;
        }
        
        String cleanPagIbig = pagibig.trim();
        return PAGIBIG_PATTERN.matcher(cleanPagIbig).matches() || 
               PAGIBIG_NUMBERS_PATTERN.matcher(cleanPagIbig).matches();
    }
    
    /**
     * Format Pag-Ibig number to standard format
     * @return formatted Pag-Ibig number
     */
    public String getFormattedPagIbig() {
        if (pagibig == null || pagibig.trim().isEmpty()) {
            return "";
        }
        
        String numbersOnly = pagibig.replaceAll("\\D", "");
        if (numbersOnly.length() == 12) {
            return numbersOnly.substring(0, 4) + "-" + 
                   numbersOnly.substring(4, 8) + "-" + 
                   numbersOnly.substring(8);
        }
        return pagibig; // Return original if can't format
    }
    
    /**
     * Get Pag-Ibig validation message
     * @return validation message
     */
    public String getPagIbigValidationMessage() {
        if (pagibig == null || pagibig.trim().isEmpty()) {
            return "Pag-Ibig number is required";
        } else if (!isValidPagIbig()) {
            return "Invalid Pag-Ibig format. Should be XXXX-XXXX-XXXX or 12 digits";
        } else {
            return "✅ Valid Pag-Ibig number";
        }
    }
    
    // ================================
    // COMPREHENSIVE VALIDATION
    // ================================
    
    /**
     * Check if all government IDs are valid
     * @return true if all required IDs are valid
     */
    public boolean isAllValid() {
        return isValidSSS() && isValidPhilHealth() && isValidTIN() && isValidPagIbig();
    }
    
    /**
     * Check if employee has complete government IDs
     * @return true if all IDs are provided
     */
    public boolean isComplete() {
        return sss != null && !sss.trim().isEmpty() &&
               philhealth != null && !philhealth.trim().isEmpty() &&
               tin != null && !tin.trim().isEmpty() &&
               pagibig != null && !pagibig.trim().isEmpty();
    }
    
    /**
     * Get validation summary
     * @return comprehensive validation message
     */
    public String getValidationSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Government ID Validation:\n");
        summary.append("• SSS: ").append(getSSSValidationMessage()).append("\n");
        summary.append("• PhilHealth: ").append(getPhilHealthValidationMessage()).append("\n");
        summary.append("• TIN: ").append(getTINValidationMessage()).append("\n");
        summary.append("• Pag-Ibig: ").append(getPagIbigValidationMessage());
        return summary.toString();
    }
    
    /**
     * Get completion percentage
     * @return percentage of completed IDs
     */
    public double getCompletionPercentage() {
        int completed = 0;
        if (sss != null && !sss.trim().isEmpty()) completed++;
        if (philhealth != null && !philhealth.trim().isEmpty()) completed++;
        if (tin != null && !tin.trim().isEmpty()) completed++;
        if (pagibig != null && !pagibig.trim().isEmpty()) completed++;
        
        return (completed / 4.0) * 100.0;
    }
    
    // ================================
    // UTILITY METHODS
    // ================================
    
    /**
     * Format all government IDs to standard format
     */
    public void formatAllIds() {
        if (sss != null) {
            this.sss = getFormattedSSS();
        }
        if (philhealth != null) {
            this.philhealth = getFormattedPhilHealth();
        }
        if (tin != null) {
            this.tin = getFormattedTIN();
        }
        if (pagibig != null) {
            this.pagibig = getFormattedPagIbig();
        }
    }
    
    /**
     * Create a copy of this GovId
     * @return new GovIdModel with same data
     */
    public GovIdModel copy() {
        GovIdModel copy = new GovIdModel();
        copy.govId = this.govId;
        copy.sss = this.sss;
        copy.philhealth = this.philhealth;
        copy.tin = this.tin;
        copy.pagibig = this.pagibig;
        copy.employeeId = this.employeeId;
        return copy;
    }
    
    // ================================
    // GETTERS AND SETTERS
    // ================================
    
    public Integer getGovId() { return govId; }
    public void setGovId(Integer govId) { this.govId = govId; }
    
    public String getSss() { return sss; }
    public void setSss(String sss) { this.sss = sss; }
    
    public String getPhilhealth() { return philhealth; }
    public void setPhilhealth(String philhealth) { this.philhealth = philhealth; }
    
    public String getTin() { return tin; }
    public void setTin(String tin) { this.tin = tin; }
    
    public String getPagibig() { return pagibig; }
    public void setPagibig(String pagibig) { this.pagibig = pagibig; }
    
    public Integer getEmployeeId() { return employeeId; }
    public void setEmployeeId(Integer employeeId) { this.employeeId = employeeId; }
    
    // ================================
    // OBJECT METHODS
    // ================================
    
    @Override
    public String toString() {
        return "GovIdModel{" +
                "govId=" + govId +
                ", employeeId=" + employeeId +
                ", completion=" + String.format("%.1f%%", getCompletionPercentage()) +
                ", allValid=" + isAllValid() +
                '}';
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        GovIdModel that = (GovIdModel) obj;
        return govId != null && govId.equals(that.govId);
    }
    
    @Override
    public int hashCode() {
        return govId != null ? govId.hashCode() : 0;
    }
}