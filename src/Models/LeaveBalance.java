package Models;

import java.time.Year;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * LeaveBalance with balance calculation and conflict resolution
 * Aligned with leavebalance table structure
 */
public class LeaveBalance {
    private static final ZoneId MANILA_TIMEZONE = ZoneId.of("Asia/Manila");
    
    private int leaveBalanceId;
    private int employeeId;
    private int leaveTypeId;
    private Integer totalLeaveDays;
    private int usedLeaveDays;
    private Integer remainingLeaveDays;
    private int carryOverDays;
    private Year balanceYear;
    private Timestamp lastUpdated;
    
    // Additional fields for business logic
    private String leaveTypeName;
    private String employeeName;
    private boolean isActive;
    
    // Constructors
    public LeaveBalance() {
        this.usedLeaveDays = 0;
        this.carryOverDays = 0;
        this.balanceYear = Year.now(MANILA_TIMEZONE);
        this.lastUpdated = Timestamp.valueOf(LocalDateTime.now(MANILA_TIMEZONE));
        this.isActive = true;
    }
    
    public LeaveBalance(int employeeId, int leaveTypeId, int totalLeaveDays) {
        this();
        this.employeeId = employeeId;
        this.leaveTypeId = leaveTypeId;
        this.totalLeaveDays = totalLeaveDays;
        calculateRemainingDays();
    }
    
    public LeaveBalance(int employeeId, int leaveTypeId, int totalLeaveDays, Year balanceYear) {
        this();
        this.employeeId = employeeId;
        this.leaveTypeId = leaveTypeId;
        this.totalLeaveDays = totalLeaveDays;
        this.balanceYear = balanceYear;
        calculateRemainingDays();
    }
    
    // Balance calculation with conflict resolution
    public final void calculateRemainingDays() {
        if (totalLeaveDays != null) {
            this.remainingLeaveDays = totalLeaveDays + carryOverDays - usedLeaveDays;
            this.remainingLeaveDays = Math.max(0, this.remainingLeaveDays);
        } else {
            this.remainingLeaveDays = 0;
        }
        updateTimestamp();
    }
    
    public boolean canTakeLeave(int requestedDays) {
        if (requestedDays <= 0) return false;
        return remainingLeaveDays != null && remainingLeaveDays >= requestedDays;
    }
    
    public boolean deductLeave(int daysToDeduct) {
        if (!canTakeLeave(daysToDeduct)) {
            return false;
        }
        
        this.usedLeaveDays += daysToDeduct;
        calculateRemainingDays();
        return true;
    }
    
    public void addLeave(int daysToAdd) {
        if (daysToAdd <= 0) return;
        this.usedLeaveDays = Math.max(0, this.usedLeaveDays - daysToAdd);
        calculateRemainingDays();
    }
    
    public void resetBalance() {
        this.usedLeaveDays = 0;
        this.carryOverDays = 0;
        calculateRemainingDays();
    }
    
    // Conflict resolution for leave balance updates
    public void resolveConflict(LeaveBalance otherBalance) {
        if (otherBalance == null || 
            this.employeeId != otherBalance.employeeId || 
            this.leaveTypeId != otherBalance.leaveTypeId ||
            !this.balanceYear.equals(otherBalance.balanceYear)) {
            return;
        }
        
        // Use the most recent update (handle null timestamps)
        boolean shouldUpdate = false;
        
        if (this.lastUpdated == null && otherBalance.lastUpdated != null) {
            shouldUpdate = true;
        } else if (this.lastUpdated != null && otherBalance.lastUpdated != null) {
            shouldUpdate = otherBalance.lastUpdated.after(this.lastUpdated);
        }
        // If both are null or other is null, don't update
        
        if (shouldUpdate) {
            this.usedLeaveDays = otherBalance.usedLeaveDays;
            this.carryOverDays = otherBalance.carryOverDays;
            this.totalLeaveDays = otherBalance.totalLeaveDays;
            this.lastUpdated = otherBalance.lastUpdated;
            calculateRemainingDays();
        }
    }
    
    // Merge balances (useful for database synchronization)
    public LeaveBalance mergeWith(LeaveBalance otherBalance) {
        if (otherBalance == null) return this;
        
        LeaveBalance merged = new LeaveBalance();
        merged.employeeId = this.employeeId;
        merged.leaveTypeId = this.leaveTypeId;
        merged.balanceYear = this.balanceYear;
        
        // Use the higher total leave days
        merged.totalLeaveDays = Math.max(
            this.totalLeaveDays != null ? this.totalLeaveDays : 0,
            otherBalance.totalLeaveDays != null ? otherBalance.totalLeaveDays : 0
        );
        
        // Use the higher used leave days (more conservative)
        merged.usedLeaveDays = Math.max(this.usedLeaveDays, otherBalance.usedLeaveDays);
        
        // Use the higher carry over days
        merged.carryOverDays = Math.max(this.carryOverDays, otherBalance.carryOverDays);
        
        // Use the most recent timestamp
        merged.lastUpdated = this.lastUpdated.after(otherBalance.lastUpdated) ? 
                           this.lastUpdated : otherBalance.lastUpdated;
        
        merged.calculateRemainingDays();
        return merged;
    }
    
    private void updateTimestamp() {
        this.lastUpdated = Timestamp.valueOf(LocalDateTime.now(MANILA_TIMEZONE));
    }
    
    // Manila timezone operations
    public LocalDateTime getLastUpdatedInManila() {
        if (lastUpdated == null) return null;
        return lastUpdated.toLocalDateTime().atZone(ZoneId.systemDefault())
                .withZoneSameInstant(MANILA_TIMEZONE).toLocalDateTime();
    }
    
    public static LocalDateTime nowInManila() {
        return LocalDateTime.now(MANILA_TIMEZONE);
    }
    
    // Validation methods
    public boolean isValidBalance() {
        return employeeId > 0 && leaveTypeId > 0 && 
               totalLeaveDays != null && totalLeaveDays >= 0 &&
               usedLeaveDays >= 0 && carryOverDays >= 0 &&
               balanceYear != null;
    }
    
    public boolean isCurrentYear() {
        Year currentYear = Year.now(MANILA_TIMEZONE);
        return balanceYear.equals(currentYear);
    }
    
    public boolean isExpired() {
        Year currentYear = Year.now(MANILA_TIMEZONE);
        return balanceYear.isBefore(currentYear);
    }
    
    // Business logic methods
    public double getUtilizationRate() {
        if (totalLeaveDays == null || totalLeaveDays == 0) return 0.0;
        return (double) usedLeaveDays / (totalLeaveDays + carryOverDays) * 100.0;
    }
    
    public boolean isFullyUtilized() {
        return remainingLeaveDays != null && remainingLeaveDays == 0;
    }
    
    public boolean isOverUtilized() {
        return remainingLeaveDays != null && remainingLeaveDays < 0;
    }
    
    public int getAvailableDaysIncludingCarryOver() {
        if (totalLeaveDays == null) return carryOverDays;
        return totalLeaveDays + carryOverDays;
    }
    
    public String getBalanceStatus() {
        if (isOverUtilized()) return "Over-utilized";
        if (isFullyUtilized()) return "Fully utilized";
        if (getUtilizationRate() > 80) return "High utilization";
        if (getUtilizationRate() > 50) return "Medium utilization";
        return "Low utilization";
    }
    
    // Carry over logic for year-end processing
    public LeaveBalance createNextYearBalance(int maxCarryOverDays) {
        int carryOver = Math.min(remainingLeaveDays != null ? remainingLeaveDays : 0, maxCarryOverDays);
        
        LeaveBalance nextYearBalance = new LeaveBalance();
        nextYearBalance.employeeId = this.employeeId;
        nextYearBalance.leaveTypeId = this.leaveTypeId;
        nextYearBalance.totalLeaveDays = this.totalLeaveDays;
        nextYearBalance.balanceYear = this.balanceYear.plusYears(1);
        nextYearBalance.carryOverDays = carryOver;
        nextYearBalance.usedLeaveDays = 0;
        nextYearBalance.calculateRemainingDays();
        
        return nextYearBalance;
    }
    
    // Getters and Setters
    public int getLeaveBalanceId() { return leaveBalanceId; }
    public void setLeaveBalanceId(int leaveBalanceId) { this.leaveBalanceId = leaveBalanceId; }
    
    public int getEmployeeId() { return employeeId; }
    public void setEmployeeId(int employeeId) { this.employeeId = employeeId; }
    
    public int getLeaveTypeId() { return leaveTypeId; }
    public void setLeaveTypeId(int leaveTypeId) { this.leaveTypeId = leaveTypeId; }
    
    public Integer getTotalLeaveDays() { return totalLeaveDays; }
    public void setTotalLeaveDays(Integer totalLeaveDays) { 
        this.totalLeaveDays = totalLeaveDays; 
        calculateRemainingDays();
    }
    
    public int getUsedLeaveDays() { return usedLeaveDays; }
    public void setUsedLeaveDays(int usedLeaveDays) { 
        this.usedLeaveDays = Math.max(0, usedLeaveDays); 
        calculateRemainingDays();
    }
    
    public Integer getRemainingLeaveDays() { return remainingLeaveDays; }
    
    public int getCarryOverDays() { return carryOverDays; }
    public void setCarryOverDays(int carryOverDays) { 
        this.carryOverDays = Math.max(0, carryOverDays); 
        calculateRemainingDays();
    }
    
    public Year getBalanceYear() { return balanceYear; }
    public void setBalanceYear(Year balanceYear) { 
        this.balanceYear = balanceYear != null ? balanceYear : Year.now(MANILA_TIMEZONE); 
    }
    
    public Timestamp getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(Timestamp lastUpdated) { this.lastUpdated = lastUpdated; }
    
    public String getLeaveTypeName() { return leaveTypeName; }
    public void setLeaveTypeName(String leaveTypeName) { this.leaveTypeName = leaveTypeName; }
    
    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
    
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    
    @Override
    public String toString() {
        return String.format("LeaveBalance{employeeId=%d, leaveTypeId=%d, year=%s, total=%d, used=%d, remaining=%d, carryOver=%d, utilization=%.1f%%}",
                employeeId, leaveTypeId, balanceYear, totalLeaveDays, usedLeaveDays, remainingLeaveDays, carryOverDays, getUtilizationRate());
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        LeaveBalance that = (LeaveBalance) obj;
        return leaveBalanceId == that.leaveBalanceId;
    }
    
    @Override
    public int hashCode() {
        return Integer.hashCode(leaveBalanceId);
    }
}