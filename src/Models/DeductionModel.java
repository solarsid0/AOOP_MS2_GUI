package Models;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.List;
import java.util.ArrayList;

/**
 * DeductionModel class that maps to the deduction table
 * Fields: deductionId, typeName, deductionAmount, lowerLimit, upperLimit, baseTax, deductionRate, payrollId
 * 
 * IMPORTANT: This class now uses DATABASE-DRIVEN deduction calculations instead of hardcoded values.
 * Deduction brackets and rates are stored in the deduction table (where payrollId is null for master rules).
 * This allows for easy updates to tax rates and contribution brackets without code changes.
 * 
 * @author User
 */
public class DeductionModel {
    
    // Enum for deduction types to match database constraints
    public enum DeductionType {
        SSS("SSS"),
        PHILHEALTH("PhilHealth"),
        PAG_IBIG("Pag-Ibig"),
        WITHHOLDING_TAX("Withholding Tax"),
        LATE_DEDUCTION("Late Deduction");
        
        private final String displayName;
        
        DeductionType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public static DeductionType fromString(String text) {
            for (DeductionType d : DeductionType.values()) {
                if (d.displayName.equalsIgnoreCase(text)) {
                    return d;
                }
            }
            throw new IllegalArgumentException("No constant with text " + text + " found");
        }
    }
    
    private Integer deductionId;
    private DeductionType deductionType; // Changed from typeName to deductionType
    private BigDecimal amount; // Changed from deductionAmount to amount
    private BigDecimal lowerLimit;
    private BigDecimal upperLimit;
    private BigDecimal baseTax;
    private BigDecimal deductionRate;
    private Integer payrollId; // This maps to payrollId in database
    private Integer payPeriodId; // Added for compatibility with DAO
    private Integer employeeId; // Added for compatibility with DAO
    
    // Constructors
    public DeductionModel() {}
    
    public DeductionModel(DeductionType deductionType, BigDecimal amount) {
        this.deductionType = deductionType;
        this.amount = amount;
    }
    
    public DeductionModel(DeductionType deductionType, BigDecimal amount, Integer payrollId) {
        this.deductionType = deductionType;
        this.amount = amount;
        this.payrollId = payrollId;
    }
    
    // Full constructor with tax bracket fields
    public DeductionModel(Integer deductionId, DeductionType deductionType, BigDecimal amount, 
                         BigDecimal lowerLimit, BigDecimal upperLimit, BigDecimal baseTax, 
                         BigDecimal deductionRate, Integer payrollId) {
        this.deductionId = deductionId;
        this.deductionType = deductionType;
        this.amount = amount;
        this.lowerLimit = lowerLimit;
        this.upperLimit = upperLimit;
        this.baseTax = baseTax;
        this.deductionRate = deductionRate;
        this.payrollId = payrollId;
    }
    
    // Getters and Setters
    public Integer getDeductionId() { return deductionId; }
    public void setDeductionId(Integer deductionId) { this.deductionId = deductionId; }
    
    // DeductionType methods
    public DeductionType getDeductionType() { return deductionType; }
    public void setDeductionType(DeductionType deductionType) { this.deductionType = deductionType; }
    
    // Legacy compatibility methods
    public DeductionType getTypeName() { return deductionType; }
    public void setTypeName(DeductionType deductionType) { this.deductionType = deductionType; }
    
    // Amount methods
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    
    // Legacy compatibility methods
    public BigDecimal getDeductionAmount() { return amount; }
    public void setDeductionAmount(BigDecimal amount) { this.amount = amount; }
    
    public BigDecimal getLowerLimit() { return lowerLimit; }
    public void setLowerLimit(BigDecimal lowerLimit) { this.lowerLimit = lowerLimit; }
    
    public BigDecimal getUpperLimit() { return upperLimit; }
    public void setUpperLimit(BigDecimal upperLimit) { this.upperLimit = upperLimit; }
    
    public BigDecimal getBaseTax() { return baseTax; }
    public void setBaseTax(BigDecimal baseTax) { this.baseTax = baseTax; }
    
    public BigDecimal getDeductionRate() { return deductionRate; }
    public void setDeductionRate(BigDecimal deductionRate) { this.deductionRate = deductionRate; }
    
    public Integer getPayrollId() { return payrollId; }
    public void setPayrollId(Integer payrollId) { 
        this.payrollId = payrollId;
        this.payPeriodId = payrollId; // Keep both in sync
    }
    
    // PayPeriodId methods for DAO compatibility
    public Integer getPayPeriodId() { return payPeriodId != null ? payPeriodId : payrollId; }
    public void setPayPeriodId(Integer payPeriodId) { 
        this.payPeriodId = payPeriodId;
        this.payrollId = payPeriodId; // Keep both in sync
    }
    
    // EmployeeId methods for DAO compatibility
    public Integer getEmployeeId() { return employeeId; }
    public void setEmployeeId(Integer employeeId) { this.employeeId = employeeId; }
    
    // Business Methods - Database-Driven Deduction Calculations
    
    /**
     * Calculate SSS deduction based on gross income using database brackets
     * @param grossIncome
     * @param sssDeductionBrackets List of SSS deduction brackets from database (where payrollId is null)
     * @return 
     */
    public static BigDecimal calculateSSSDeduction(BigDecimal grossIncome, List<DeductionModel> sssDeductionBrackets) {
        if (grossIncome == null || grossIncome.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        
        if (sssDeductionBrackets == null || sssDeductionBrackets.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        // Find the appropriate bracket for the gross income
        for (DeductionModel bracket : sssDeductionBrackets) {
            if (bracket.getDeductionType() == DeductionType.SSS && 
                isIncomeWithinRange(grossIncome, bracket.getLowerLimit(), bracket.getUpperLimit())) {
                return bracket.getAmount() != null ? 
                       bracket.getAmount().setScale(2, RoundingMode.HALF_UP) : 
                       BigDecimal.ZERO;
            }
        }
        
        return BigDecimal.ZERO;
    }
    
    /**
     * Calculate PhilHealth deduction based on monthly rate using database rules
     * @param monthlyRate
     * @param philHealthDeductionRules List of PhilHealth deduction rules from database (where payrollId is null)
     * @return 
     */
    public static BigDecimal calculatePhilHealthDeduction(BigDecimal monthlyRate, List<DeductionModel> philHealthDeductionRules) {
        if (monthlyRate == null || monthlyRate.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        
        if (philHealthDeductionRules == null || philHealthDeductionRules.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        // Find PhilHealth rule (usually percentage-based)
        for (DeductionModel rule : philHealthDeductionRules) {
            if (rule.getDeductionType() == DeductionType.PHILHEALTH) {
                if (rule.getDeductionRate() != null) {
                    // Percentage-based calculation
                    BigDecimal contribution = monthlyRate.multiply(rule.getDeductionRate());
                    
                    // Apply minimum and maximum limits if specified
                    if (rule.getLowerLimit() != null && contribution.compareTo(rule.getLowerLimit()) < 0) {
                        contribution = rule.getLowerLimit();
                    }
                    if (rule.getUpperLimit() != null && contribution.compareTo(rule.getUpperLimit()) > 0) {
                        contribution = rule.getUpperLimit();
                    }
                    
                    return contribution.setScale(2, RoundingMode.HALF_UP);
                } else if (rule.getAmount() != null) {
                    // Fixed amount
                    return rule.getAmount().setScale(2, RoundingMode.HALF_UP);
                }
            }
        }
        
        return BigDecimal.ZERO;
    }
    
    /**
     * Calculate Pag-IBIG deduction based on monthly rate using database brackets
     * @param monthlyRate
     * @param pagIbigDeductionBrackets List of Pag-IBIG deduction brackets from database (where payrollId is null)
     * @return 
     */
    public static BigDecimal calculatePagIbigDeduction(BigDecimal monthlyRate, List<DeductionModel> pagIbigDeductionBrackets) {
        if (monthlyRate == null || monthlyRate.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        
        if (pagIbigDeductionBrackets == null || pagIbigDeductionBrackets.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        // Find the appropriate bracket for the monthly rate
        for (DeductionModel bracket : pagIbigDeductionBrackets) {
            if (bracket.getDeductionType() == DeductionType.PAG_IBIG && 
                isIncomeWithinRange(monthlyRate, bracket.getLowerLimit(), bracket.getUpperLimit())) {
                if (bracket.getDeductionRate() != null) {
                    // Percentage-based calculation
                    BigDecimal contribution = monthlyRate.multiply(bracket.getDeductionRate());
                    
                    // Apply maximum limit if specified
                    if (bracket.getUpperLimit() != null && contribution.compareTo(bracket.getUpperLimit()) > 0) {
                        contribution = bracket.getUpperLimit();
                    }
                    
                    return contribution.setScale(2, RoundingMode.HALF_UP);
                } else if (bracket.getAmount() != null) {
                    // Fixed amount
                    return bracket.getAmount().setScale(2, RoundingMode.HALF_UP);
                }
            }
        }
        
        return BigDecimal.ZERO;
    }
    
    /**
     * Calculate withholding tax based on taxable income using database brackets
     * @param taxableIncome
     * @param withholdingTaxBrackets List of withholding tax brackets from database (where payrollId is null)
     * @return 
     */
    public static BigDecimal calculateWithholdingTax(BigDecimal taxableIncome, List<DeductionModel> withholdingTaxBrackets) {
        if (taxableIncome == null || taxableIncome.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        
        if (withholdingTaxBrackets == null || withholdingTaxBrackets.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        // Find the appropriate tax bracket for the taxable income
        for (DeductionModel bracket : withholdingTaxBrackets) {
            if (bracket.getDeductionType() == DeductionType.WITHHOLDING_TAX && 
                isIncomeWithinRange(taxableIncome, bracket.getLowerLimit(), bracket.getUpperLimit())) {
                return calculateTaxFromBracket(taxableIncome, bracket);
            }
        }
        
        return BigDecimal.ZERO;
    }
    
    /**
     * Calculate late deduction based on hours and hourly rate
     * For rank-and-file employees only
     * @param hourlyRate
     * @param hoursLate
     * @return 
     */
    public static BigDecimal calculateLateDeduction(BigDecimal hourlyRate, BigDecimal hoursLate) {
        if (hourlyRate == null || hoursLate == null || 
            hourlyRate.compareTo(BigDecimal.ZERO) <= 0 || hoursLate.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        
        return hourlyRate.multiply(hoursLate).setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * Calculate total mandatory deductions for an employee using database rules
     * @param grossIncome
     * @param monthlyRate
     * @param allDeductionRules List of all deduction rules from database (where payrollId is null)
     * @return 
     */
    public static BigDecimal calculateTotalMandatoryDeductions(BigDecimal grossIncome, BigDecimal monthlyRate, 
                                                             List<DeductionModel> allDeductionRules) {
        if (grossIncome == null || monthlyRate == null || allDeductionRules == null) {
            return BigDecimal.ZERO;
        }
        
        // Filter deduction rules by type
        List<DeductionModel> sssRules = filterDeductionsByType(allDeductionRules, DeductionType.SSS);
        List<DeductionModel> philHealthRules = filterDeductionsByType(allDeductionRules, DeductionType.PHILHEALTH);
        List<DeductionModel> pagIbigRules = filterDeductionsByType(allDeductionRules, DeductionType.PAG_IBIG);
        List<DeductionModel> taxRules = filterDeductionsByType(allDeductionRules, DeductionType.WITHHOLDING_TAX);
        
        BigDecimal sss = calculateSSSDeduction(grossIncome, sssRules);
        BigDecimal philHealth = calculatePhilHealthDeduction(monthlyRate, philHealthRules);
        BigDecimal pagIbig = calculatePagIbigDeduction(monthlyRate, pagIbigRules);
        
        // Calculate taxable income (monthly rate minus contributions)
        BigDecimal taxableIncome = monthlyRate.subtract(sss).subtract(philHealth).subtract(pagIbig);
        BigDecimal withholdingTax = calculateWithholdingTax(taxableIncome, taxRules);
        
        return sss.add(philHealth).add(pagIbig).add(withholdingTax);
    }
    
    /**
     * Filter deduction rules by type
     * @param allDeductionRules
     * @param type
     * @return 
     */
    public static List<DeductionModel> filterDeductionsByType(List<DeductionModel> allDeductionRules, DeductionType type) {
        List<DeductionModel> filtered = new ArrayList<>();
        
        if (allDeductionRules == null || type == null) {
            return filtered;
        }
        
        for (DeductionModel rule : allDeductionRules) {
            if (rule.getDeductionType() == type && rule.getPayrollId() == null) {
                // Only include master rules (where payrollId is null)
                filtered.add(rule);
            }
        }
        
        return filtered;
    }
    
    /**
     * Find specific deduction rule for an income amount
     * @param deductionRules
     * @param income
     * @param type
     * @return 
     */
    public static DeductionModel findApplicableDeductionRule(List<DeductionModel> deductionRules, 
                                                           BigDecimal income, DeductionType type) {
        if (deductionRules == null || income == null || type == null) {
            return null;
        }
        
        for (DeductionModel rule : deductionRules) {
            if (rule.getDeductionType() == type && 
                isIncomeWithinRange(income, rule.getLowerLimit(), rule.getUpperLimit())) {
                return rule;
            }
        }
        
        return null;
    }
    
    /**
     * Helper method to check if income is within range
     * @param income
     * @param lowerLimit
     * @param upperLimit
     * @return 
     */
    private static boolean isIncomeWithinRange(BigDecimal income, BigDecimal lowerLimit, BigDecimal upperLimit) {
        if (income == null) return false;
        
        boolean withinLower = (lowerLimit == null) || (income.compareTo(lowerLimit) >= 0);
        boolean withinUpper = (upperLimit == null) || (income.compareTo(upperLimit) <= 0);
        
        return withinLower && withinUpper;
    }
    
    /**
     * Helper method to calculate tax from bracket
     * @param taxableIncome
     * @param bracket
     * @return 
     */
    private static BigDecimal calculateTaxFromBracket(BigDecimal taxableIncome, DeductionModel bracket) {
        if (taxableIncome == null) {
            return BigDecimal.ZERO;
        }
        
        if (bracket.getDeductionRate() != null && bracket.getBaseTax() != null) {
            // Progressive tax calculation
            BigDecimal excess = taxableIncome.subtract(bracket.getLowerLimit() != null ? bracket.getLowerLimit() : BigDecimal.ZERO);
            return bracket.getBaseTax().add(excess.multiply(bracket.getDeductionRate())).setScale(2, RoundingMode.HALF_UP);
        } else if (bracket.getDeductionRate() != null) {
            // Percentage-based calculation
            return taxableIncome.multiply(bracket.getDeductionRate()).setScale(2, RoundingMode.HALF_UP);
        } else {
            // Fixed amount
            return bracket.getAmount() != null ? bracket.getAmount() : BigDecimal.ZERO;
        }
    }
    
    /**
     * Check if this deduction is within valid salary range
     * @param salary
     * @return 
     */
    public boolean isWithinRange(BigDecimal salary) {
        return isIncomeWithinRange(salary, this.lowerLimit, this.upperLimit);
    }
    
    /**
     * Check if this is a government mandated deduction
     * @return 
     */
    public boolean isMandatoryDeduction() {
        return deductionType == DeductionType.SSS || 
               deductionType == DeductionType.PHILHEALTH || 
               deductionType == DeductionType.PAG_IBIG || 
               deductionType == DeductionType.WITHHOLDING_TAX;
    }
    
    /**
     * Check if this is a penalty deduction
     * @return 
     */
    public boolean isPenaltyDeduction() {
        return deductionType == DeductionType.LATE_DEDUCTION;
    }
    
    /**
     * Get deduction category
     * @return 
     */
    public String getDeductionCategory() {
        if (isMandatoryDeduction()) {
            return "Government Contribution";
        } else if (isPenaltyDeduction()) {
            return "Penalty";
        } else {
            return "Other Deduction";
        }
    }
    
    /**
     * Get formatted deduction description
     * @return 
     */
    public String getFormattedDescription() {
        return String.format("%s: ₱%,.2f", deductionType.getDisplayName(), amount);
    }
    
    /**
     * Calculate deduction based on bracket system
     * @param income
     * @return 
     */
    public BigDecimal calculateBracketDeduction(BigDecimal income) {
        return calculateTaxFromBracket(income, this);
    }
    
    /**
     * Validate deduction amount
     * @return 
     */
    public boolean isValidDeductionAmount() {
        if (amount == null) {
            return false;
        }
        
        // Deduction amount should be non-negative
        return amount.compareTo(BigDecimal.ZERO) >= 0;
    }
    
    /**
     * Get deduction processing priority (lower number = higher priority)
     * @return 
     */
    public int getDeductionPriority() {
        return switch (deductionType) {
            case SSS -> 1;
            case PHILHEALTH -> 2;
            case PAG_IBIG -> 3;
            case WITHHOLDING_TAX -> 4;
            case LATE_DEDUCTION -> 5;
            default -> 99;
        };
    }
    
    @Override
    public String toString() {
        return String.format("DeductionModel{deductionId=%d, deductionType=%s, amount=%s, payrollId=%d}", 
                           deductionId, deductionType, amount, payrollId);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        DeductionModel that = (DeductionModel) obj;
        return Objects.equals(deductionId, that.deductionId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(deductionId);
    }
}