package Models;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * BenefitTypeModel class that maps to the benefittype table
 * Fields: benefitTypeId, benefitName, description, amount
 * Includes benefit calculations aligned with business logic
 * @author User
 */
public class BenefitTypeModel {
    
    // Enum for benefit names to match database constraints
    public enum BenefitName {
        RICE_SUBSIDY("Rice Subsidy"),
        PHONE_ALLOWANCE("Phone Allowance"),
        CLOTHING_ALLOWANCE("Clothing Allowance");
        
        private final String displayName;
        
        BenefitName(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public static BenefitName fromString(String text) {
            for (BenefitName b : BenefitName.values()) {
                if (b.displayName.equalsIgnoreCase(text)) {
                    return b;
                }
            }
            throw new IllegalArgumentException("No constant with text " + text + " found");
        }
    }
    
    private Integer benefitTypeId;
    private BenefitName benefitName;
    private String benefitDescription;
    private BigDecimal amount; // Default amount for this benefit type
    
    // Constructors
    public BenefitTypeModel() {}
    
    public BenefitTypeModel(BenefitName benefitName, String benefitDescription) {
        this.benefitName = benefitName;
        this.benefitDescription = benefitDescription;
    }
    
    public BenefitTypeModel(BenefitName benefitName, String benefitDescription, BigDecimal amount) {
        this.benefitName = benefitName;
        this.benefitDescription = benefitDescription;
        this.amount = amount;
    }
    
    public BenefitTypeModel(Integer benefitTypeId, BenefitName benefitName, String benefitDescription, BigDecimal amount) {
        this.benefitTypeId = benefitTypeId;
        this.benefitName = benefitName;
        this.benefitDescription = benefitDescription;
        this.amount = amount;
    }
    
    // Getters and Setters
    public Integer getBenefitTypeId() { return benefitTypeId; }
    public void setBenefitTypeId(Integer benefitTypeId) { this.benefitTypeId = benefitTypeId; }
    
    public BenefitName getBenefitName() { return benefitName; }
    public void setBenefitName(BenefitName benefitName) { this.benefitName = benefitName; }
    
    public String getBenefitDescription() { return benefitDescription; }
    public void setBenefitDescription(String benefitDescription) { this.benefitDescription = benefitDescription; }
    
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    
    // Business Methods - Benefit Calculations
    
    /**
     * Calculate monthly benefit amount based on position
     * All employees get full benefits based on their position
     * @param positionBenefitAmount Amount from positionbenefit table
     * @return 
     */
    public BigDecimal calculateMonthlyBenefit(BigDecimal positionBenefitAmount) {
        if (positionBenefitAmount == null) {
            return BigDecimal.ZERO;
        }
        
        // Return the position-specific benefit amount
        return positionBenefitAmount.setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * Calculate prorated benefit amount based on days worked
     * (Optional method for future use if needed)
     * @param positionBenefitAmount
     * @param daysWorked
     * @param totalDaysInPeriod
     * @return 
     */
    public BigDecimal calculateProratedBenefit(BigDecimal positionBenefitAmount, int daysWorked, int totalDaysInPeriod) {
        if (totalDaysInPeriod <= 0 || positionBenefitAmount == null) {
            return BigDecimal.ZERO;
        }
        
        if (daysWorked >= totalDaysInPeriod) {
            return positionBenefitAmount.setScale(2, RoundingMode.HALF_UP);
        }
        
        BigDecimal proration = new BigDecimal(daysWorked)
            .divide(new BigDecimal(totalDaysInPeriod), 4, RoundingMode.HALF_UP);
        
        return positionBenefitAmount.multiply(proration).setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * Calculate benefit based on percentage of basic salary
     * @param basicSalary
     * @param percentage
     * @return 
     */
    public BigDecimal calculatePercentageBenefit(BigDecimal basicSalary, BigDecimal percentage) {
        if (basicSalary == null || percentage == null) {
            return BigDecimal.ZERO;
        }
        
        return basicSalary.multiply(percentage.divide(new BigDecimal(100), 4, RoundingMode.HALF_UP))
                          .setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * Validate benefit amount against business rules
     * @param benefitAmount
     * @return 
     */
    public boolean isValidBenefitAmount(BigDecimal benefitAmount) {
        if (benefitAmount == null) {
            return false;
        }
        
        // Benefit amount should be non-negative
        if (benefitAmount.compareTo(BigDecimal.ZERO) < 0) {
            return false;
        }
        
        // Set reasonable upper limits for each benefit type
        BigDecimal maxAmount = getMaxAllowedAmount();
        return benefitAmount.compareTo(maxAmount) <= 0;
    }
    
    /**
     * Get maximum allowed amount for this benefit type
     * @return 
     */
    private BigDecimal getMaxAllowedAmount() {
        return switch (benefitName) {
            case RICE_SUBSIDY -> new BigDecimal("5000.00");
            case PHONE_ALLOWANCE -> new BigDecimal("3000.00");
            case CLOTHING_ALLOWANCE -> new BigDecimal("2000.00");
            default -> new BigDecimal("10000.00");
        }; // Max ₱5,000
        // Max ₱3,000
        // Max ₱2,000
        // Default max
    }
    
    /**
     * Check if this benefit type has a fixed amount
     * @return 
     */
    public boolean hasFixedAmount() {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * Get formatted description with amount info
     * @return 
     */
    public String getFormattedDescription() {
        StringBuilder description = new StringBuilder();
        
        if (benefitDescription != null && !benefitDescription.trim().isEmpty()) {
            description.append(benefitDescription);
        }
        
        if (hasFixedAmount()) {
            if (description.length() > 0) {
                description.append(" - ");
            }
            description.append("₱").append(amount);
        }
        
        return description.toString();
    }
    
    /**
     * Get benefit category for reporting
     * @return 
     */
    public String getBenefitCategory() {
        return switch (benefitName) {
            case RICE_SUBSIDY -> "Food Allowance";
            case PHONE_ALLOWANCE -> "Communication Allowance";
            case CLOTHING_ALLOWANCE -> "Uniform Allowance";
            default -> "Other Benefits";
        };
    }
    
    /**
     * Check if this benefit is taxable
     * @return 
     */
    public boolean isTaxable() {
        // For Philippine tax law, most allowances are taxable
        // Rice subsidy up to certain limits may be tax-exempt
        return switch (benefitName) {
            case RICE_SUBSIDY -> amount != null && amount.compareTo(new BigDecimal("1500.00")) > 0;
            case PHONE_ALLOWANCE, CLOTHING_ALLOWANCE -> true;
            default -> true;
        }; // Rice subsidy is generally tax-exempt up to ₱1,500 per month
        // Generally taxable
    }
    
    /**
     * Calculate tax-exempt portion of the benefit
     * @return 
     */
    public BigDecimal getTaxExemptAmount() {
        if (!isTaxable()) {
            return amount != null ? amount : BigDecimal.ZERO;
        }
        
        switch (benefitName) {
            case RICE_SUBSIDY -> {
                BigDecimal exemptLimit = new BigDecimal("1500.00");
                if (amount != null && amount.compareTo(exemptLimit) <= 0) {
                    return amount;
                } else {
                    return exemptLimit;
                }
            }
            default -> {
                return BigDecimal.ZERO;
            }
        }
    }
    
    /**
     * Calculate taxable portion of the benefit
     * @return 
     */
    public BigDecimal getTaxableAmount() {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal taxExempt = getTaxExemptAmount();
        return amount.subtract(taxExempt).max(BigDecimal.ZERO);
    }
    
    /**
     * Validate benefit type data
     * @return 
     */
    public boolean isValid() {
        if (benefitName == null) {
            return false;
        }
        
        if (benefitDescription == null || benefitDescription.trim().isEmpty()) {
            return false;
        }
        
        if (benefitDescription.length() > 255) {
            return false;
        }
        // If amount is provided, validate it
        
        return !(amount != null && !isValidBenefitAmount(amount));
    }
    
    /**
     * Check if this benefit applies to all employees
     * @return 
     */
    public boolean isUniversalBenefit() {
        // All benefit types in the system apply to all employees based on position
        return true;
    }
    
    /**
     * Get benefit application rule
     * @return 
     */
    public String getBenefitApplicationRule() {
        return "Applied to all employees based on position";
    }
    
    /**
     * Format benefit amount for display
     * @param benefitAmount
     * @return 
     */
    public String formatBenefitAmount(BigDecimal benefitAmount) {
        if (benefitAmount == null) {
            return "₱0.00";
        }
        
        return String.format("₱%,.2f", benefitAmount);
    }
    
    /**
     * Get benefit priority for processing (lower number = higher priority)
     * @return 
     */
    public int getBenefitPriority() {
        return switch (benefitName) {
            case RICE_SUBSIDY -> 1;
            case PHONE_ALLOWANCE -> 2;
            case CLOTHING_ALLOWANCE -> 3;
            default -> 99;
        };
    }
    
    /**
     * Check if benefit requires approval
     * @return 
     */
    public boolean requiresApproval() {
        // For now, all benefits are automatically approved based on position
        return false;
    }
    
    @Override
    public String toString() {
        return String.format("BenefitTypeModel{benefitTypeId=%d, benefitName=%s, benefitDescription='%s', amount=%s}", 
                           benefitTypeId, benefitName, benefitDescription, amount);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        BenefitTypeModel that = (BenefitTypeModel) obj;
        return Objects.equals(benefitTypeId, that.benefitTypeId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(benefitTypeId);
    }
}