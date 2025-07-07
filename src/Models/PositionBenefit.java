package Models;

import java.math.BigDecimal;

/**
 * PositionBenefit - Simple model class for positionbenefit table
 * Junction table linking positions with benefit types and values
 * Fields: benefitTypeId, positionId, benefitValue
 * @author Chad
 */
public class PositionBenefit {
    
    private Integer benefitTypeId;
    private Integer positionId;
    private BigDecimal benefitValue;
    
    // ===============================
    // CONSTRUCTORS
    // ===============================
    
    /**
     * Default constructor - REQUIRED for DAO operations
     */
    public PositionBenefit() {}
    
    /**
     * Constructor with all fields
     */
    public PositionBenefit(Integer benefitTypeId, Integer positionId, BigDecimal benefitValue) {
        this.benefitTypeId = benefitTypeId;
        this.positionId = positionId;
        this.benefitValue = benefitValue;
    }
    
    // ===============================
    // GETTERS AND SETTERS - REQUIRED FOR DAO
    // ===============================
    
    public Integer getBenefitTypeId() {
        return benefitTypeId;
    }
    
    public void setBenefitTypeId(Integer benefitTypeId) {
        this.benefitTypeId = benefitTypeId;
    }
    
    public Integer getPositionId() {
        return positionId;
    }
    
    public void setPositionId(Integer positionId) {
        this.positionId = positionId;
    }
    
    public BigDecimal getBenefitValue() {
        return benefitValue;
    }
    
    public void setBenefitValue(BigDecimal benefitValue) {
        this.benefitValue = benefitValue;
    }
    
    // ===============================
    // UTILITY METHODS
    // ===============================
    
    public boolean isValid() {
        return benefitTypeId != null && positionId != null && 
               benefitValue != null && benefitValue.compareTo(BigDecimal.ZERO) >= 0;
    }
    
    @Override
    public String toString() {
        return "PositionBenefit{" +
                "benefitTypeId=" + benefitTypeId +
                ", positionId=" + positionId +
                ", benefitValue=" + benefitValue +
                '}';
    }
}