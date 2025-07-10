
package Services;

import DAOs.DatabaseConnection;
import DAOs.EmployeeDAO;
import DAOs.ReferenceDataDAO;
import Models.EmployeeModel;
import java.sql.*;
import java.time.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * PositionBenefit manages the relationship between job positions and benefit entitlements
 * Handles position-based benefit packages, eligibility rules, and benefit calculations
 * Links position hierarchy to benefit structure for comprehensive compensation management
 * @author Chadley
 */

public class PositionBenefitService {
 private final DatabaseConnection databaseConnection;
    private final EmployeeDAO employeeDAO;
    private final ReferenceDataDAO referenceDataDAO;
    
    // Benefit calculation constants
    private static final int PROBATIONARY_PERIOD_MONTHS = 6;
    private static final int FULL_BENEFIT_MONTHS = 12;
    
    /**
     * Constructor
     * @param databaseConnection Database connection instance
     */
    public PositionBenefitService(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
        this.employeeDAO = new EmployeeDAO(databaseConnection);
        this.referenceDataDAO = new ReferenceDataDAO(databaseConnection);
    }
    
    /**
     * Gets all benefit entitlements for a specific position
     * @param positionId Position ID
     * @return List of benefits available for this position
     */
    public List<Map<String, Object>> getPositionBenefits(Integer positionId) {
        String sql = "SELECT pb.*, bt.benefitName, bt.description, p.positionTitle " +
                    "FROM position_benefit pb " +
                    "JOIN benefittype bt ON pb.benefitTypeId = bt.benefitTypeId " +
                    "JOIN position p ON pb.positionId = p.positionId " +
                    "WHERE pb.positionId = ? AND pb.isActive = true " +
                    "ORDER BY bt.benefitName";
        
        return executeQuery(sql, positionId);
    }
    
    /**
     * Gets all benefits available to an employee based on their position
     * @param employeeId Employee ID
     * @return List of benefits available to this employee
     */
    public List<Map<String, Object>> getEmployeeBenefitsByPosition(Integer employeeId) {
        // Get employee's position
        EmployeeModel employee = employeeDAO.findById(employeeId);
        if (employee == null || employee.getPositionId() == null) {
            return new ArrayList<>();
        }
        
        // Get position benefits
        List<Map<String, Object>> positionBenefits = getPositionBenefits(employee.getPositionId());
        
        // Apply eligibility rules
        return filterBenefitsByEligibility(positionBenefits, employee);
    }
    
    /**
     * Creates or updates a position benefit entitlement
     * @param positionId Position ID
     * @param benefitTypeId Benefit type ID
     * @param amount Benefit amount (if applicable)
     * @param percentage Benefit percentage (if applicable)
     * @param eligibilityMonths Months of employment required for eligibility
     * @param maxAmount Maximum benefit amount (if applicable)
     * @param isPercentageBased Whether benefit is percentage-based
     * @param notes Additional notes
     * @return true if successful
     */
    public boolean createPositionBenefit(Integer positionId, Integer benefitTypeId, 
                                       BigDecimal amount, BigDecimal percentage,
                                       Integer eligibilityMonths, BigDecimal maxAmount,
                                       boolean isPercentageBased, String notes) {
        try {
            // Validate position and benefit type exist
            if (!referenceDataDAO.isValidPositionId(positionId) || 
                !referenceDataDAO.isValidBenefitTypeId(benefitTypeId)) {
                System.err.println("Invalid position ID or benefit type ID");
                return false;
            }
            
            String sql = "INSERT INTO position_benefit " +
                        "(positionId, benefitTypeId, amount, percentage, eligibilityMonths, " +
                        "maxAmount, isPercentageBased, notes, isActive, createdDate) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, true, CURRENT_TIMESTAMP) " +
                        "ON DUPLICATE KEY UPDATE " +
                        "amount = VALUES(amount), percentage = VALUES(percentage), " +
                        "eligibilityMonths = VALUES(eligibilityMonths), maxAmount = VALUES(maxAmount), " +
                        "isPercentageBased = VALUES(isPercentageBased), notes = VALUES(notes), " +
                        "isActive = true, updatedDate = CURRENT_TIMESTAMP";
            
            try (Connection conn = databaseConnection.createConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setInt(1, positionId);
                stmt.setInt(2, benefitTypeId);
                stmt.setBigDecimal(3, amount);
                stmt.setBigDecimal(4, percentage);
                stmt.setObject(5, eligibilityMonths, Types.INTEGER);
                stmt.setBigDecimal(6, maxAmount);
                stmt.setBoolean(7, isPercentageBased);
                stmt.setString(8, notes);
                
                int rowsAffected = stmt.executeUpdate();
                
                if (rowsAffected > 0) {
                    System.out.println("Position benefit created/updated successfully");
                    return true;
                }
                
            }
            
        } catch (SQLException e) {
            System.err.println("Error creating position benefit: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    /**
     * Calculates actual benefit amount for an employee
     * @param employeeId Employee ID
     * @param benefitTypeId Benefit type ID
     * @return Calculated benefit amount
     */
    public BigDecimal calculateEmployeeBenefit(Integer employeeId, Integer benefitTypeId) {
        try {
            EmployeeModel employee = employeeDAO.findById(employeeId);
            if (employee == null || employee.getPositionId() == null) {
                return BigDecimal.ZERO;
            }
            
            // Get position benefit details
            Map<String, Object> positionBenefit = getPositionBenefitDetails(employee.getPositionId(), benefitTypeId);
            if (positionBenefit == null) {
                return BigDecimal.ZERO;
            }
            
            // Check eligibility
            if (!isEmployeeEligible(employee, positionBenefit)) {
                return BigDecimal.ZERO;
            }
            
            // Calculate benefit amount
            Boolean isPercentageBased = (Boolean) positionBenefit.get("isPercentageBased");
            BigDecimal amount = (BigDecimal) positionBenefit.get("amount");
            BigDecimal percentage = (BigDecimal) positionBenefit.get("percentage");
            BigDecimal maxAmount = (BigDecimal) positionBenefit.get("maxAmount");
            
            BigDecimal calculatedAmount = BigDecimal.ZERO;
            
            if (isPercentageBased != null && isPercentageBased && percentage != null) {
                // Percentage-based benefit (e.g., % of salary)
                BigDecimal baseSalary = employee.getBasicSalary() != null ? employee.getBasicSalary() : BigDecimal.ZERO;
                calculatedAmount = baseSalary.multiply(percentage.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP));
                
                // Apply maximum limit if specified
                if (maxAmount != null && calculatedAmount.compareTo(maxAmount) > 0) {
                    calculatedAmount = maxAmount;
                }
            } else if (amount != null) {
                // Fixed amount benefit
                calculatedAmount = amount;
            }
            
            return calculatedAmount;
            
        } catch (Exception e) {
            System.err.println("Error calculating employee benefit: " + e.getMessage());
            return BigDecimal.ZERO;
        }
    }
    
    /**
     * Gets benefit summary for an employee (all benefits with calculated amounts)
     * @param employeeId Employee ID
     * @return List of benefits with calculated amounts
     */
    public List<Map<String, Object>> getEmployeeBenefitSummary(Integer employeeId) {
        List<Map<String, Object>> benefitSummary = new ArrayList<>();
        
        EmployeeModel employee = employeeDAO.findById(employeeId);
        if (employee == null || employee.getPositionId() == null) {
            return benefitSummary;
        }
        
        List<Map<String, Object>> positionBenefits = getPositionBenefits(employee.getPositionId());
        
        for (Map<String, Object> benefit : positionBenefits) {
            Integer benefitTypeId = (Integer) benefit.get("benefitTypeId");
            String benefitName = (String) benefit.get("benefitName");
            String description = (String) benefit.get("description");
            
            // Calculate actual benefit amount
            BigDecimal calculatedAmount = calculateEmployeeBenefit(employeeId, benefitTypeId);
            
            // Check eligibility
            boolean isEligible = isEmployeeEligible(employee, benefit);
            
            Map<String, Object> summary = new HashMap<>();
            summary.put("benefitTypeId", benefitTypeId);
            summary.put("benefitName", benefitName);
            summary.put("description", description);
            summary.put("calculatedAmount", calculatedAmount);
            summary.put("isEligible", isEligible);
            summary.put("positionBenefit", benefit);
            
            benefitSummary.add(summary);
        }
        
        return benefitSummary;
    }
    
    /**
     * Enrolls an employee in available benefits
     * @param employeeId Employee ID
     * @param benefitTypeIds List of benefit types to enroll in
     * @return true if successful
     */
    public boolean enrollEmployeeInBenefits(Integer employeeId, List<Integer> benefitTypeIds) {
        try {
            EmployeeModel employee = employeeDAO.findById(employeeId);
            if (employee == null) {
                System.err.println("Employee not found: " + employeeId);
                return false;
            }
            
            String sql = "INSERT INTO employee_benefit " +
                        "(employeeId, benefitTypeId, enrollmentDate, isActive, calculatedAmount) " +
                        "VALUES (?, ?, CURRENT_DATE, true, ?) " +
                        "ON DUPLICATE KEY UPDATE " +
                        "isActive = true, enrollmentDate = CURRENT_DATE, calculatedAmount = VALUES(calculatedAmount)";
            
            try (Connection conn = databaseConnection.createConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                int enrolled = 0;
                
                for (Integer benefitTypeId : benefitTypeIds) {
                    BigDecimal calculatedAmount = calculateEmployeeBenefit(employeeId, benefitTypeId);
                    
                    if (calculatedAmount.compareTo(BigDecimal.ZERO) > 0) {
                        stmt.setInt(1, employeeId);
                        stmt.setInt(2, benefitTypeId);
                        stmt.setBigDecimal(3, calculatedAmount);
                        
                        stmt.executeUpdate();
                        enrolled++;
                    }
                }
                
                if (enrolled > 0) {
                    System.out.println("Employee enrolled in " + enrolled + " benefits");
                    return true;
                }
                
            }
            
        } catch (SQLException e) {
            System.err.println("Error enrolling employee in benefits: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    /**
     * Gets benefit cost analysis for a position
     * @param positionId Position ID
     * @return Benefit cost summary
     */
    public Map<String, Object> getPositionBenefitCostAnalysis(Integer positionId) {
        Map<String, Object> costAnalysis = new HashMap<>();
        
        // Get all employees in this position
        List<EmployeeModel> positionEmployees = employeeDAO.findByPosition(positionId);
        
        if (positionEmployees.isEmpty()) {
            costAnalysis.put("totalEmployees", 0);
            costAnalysis.put("totalBenefitCost", BigDecimal.ZERO);
            return costAnalysis;
        }
        
        BigDecimal totalBenefitCost = BigDecimal.ZERO;
        int eligibleEmployees = 0;
        
        List<Map<String, Object>> positionBenefits = getPositionBenefits(positionId);
        
        for (EmployeeModel employee : positionEmployees) {
            BigDecimal employeeBenefitCost = BigDecimal.ZERO;
            boolean hasAnyBenefit = false;
            
            for (Map<String, Object> benefit : positionBenefits) {
                Integer benefitTypeId = (Integer) benefit.get("benefitTypeId");
                BigDecimal benefitAmount = calculateEmployeeBenefit(employee.getEmployeeId(), benefitTypeId);
                
                if (benefitAmount.compareTo(BigDecimal.ZERO) > 0) {
                    employeeBenefitCost = employeeBenefitCost.add(benefitAmount);
                    hasAnyBenefit = true;
                }
            }
            
            totalBenefitCost = totalBenefitCost.add(employeeBenefitCost);
            if (hasAnyBenefit) {
                eligibleEmployees++;
            }
        }
        
        costAnalysis.put("positionId", positionId);
        costAnalysis.put("totalEmployees", positionEmployees.size());
        costAnalysis.put("eligibleEmployees", eligibleEmployees);
        costAnalysis.put("totalBenefitCost", totalBenefitCost);
        costAnalysis.put("averageBenefitCostPerEmployee", 
            positionEmployees.size() > 0 ? 
            totalBenefitCost.divide(new BigDecimal(positionEmployees.size()), 2, RoundingMode.HALF_UP) : 
            BigDecimal.ZERO);
        
        return costAnalysis;
    }
    
    /**
     * Gets department-wide benefit cost analysis
     * @param department Department name
     * @return Department benefit cost summary
     */
    public Map<String, Object> getDepartmentBenefitCostAnalysis(String department) {
        Map<String, Object> costAnalysis = new HashMap<>();
        
        // Get all positions in department
        List<Map<String, Object>> departmentPositions = referenceDataDAO.getPositionsByDepartment(department);
        
        BigDecimal totalDepartmentCost = BigDecimal.ZERO;
        int totalEmployees = 0;
        List<Map<String, Object>> positionCosts = new ArrayList<>();
        
        for (Map<String, Object> position : departmentPositions) {
            Integer positionId = (Integer) position.get("positionId");
            String positionTitle = (String) position.get("positionTitle");
            
            Map<String, Object> positionCostAnalysis = getPositionBenefitCostAnalysis(positionId);
            positionCostAnalysis.put("positionTitle", positionTitle);
            
            BigDecimal positionCost = (BigDecimal) positionCostAnalysis.get("totalBenefitCost");
            Integer positionEmployees = (Integer) positionCostAnalysis.get("totalEmployees");
            
            totalDepartmentCost = totalDepartmentCost.add(positionCost);
            totalEmployees += positionEmployees;
            
            positionCosts.add(positionCostAnalysis);
        }
        
        costAnalysis.put("department", department);
        costAnalysis.put("totalEmployees", totalEmployees);
        costAnalysis.put("totalBenefitCost", totalDepartmentCost);
        costAnalysis.put("averageBenefitCostPerEmployee", 
            totalEmployees > 0 ? 
            totalDepartmentCost.divide(new BigDecimal(totalEmployees), 2, RoundingMode.HALF_UP) : 
            BigDecimal.ZERO);
        costAnalysis.put("positionBreakdown", positionCosts);
        
        return costAnalysis;
    }
    
    /**
     * Copies benefit structure from one position to another
     * @param sourcePositionId Source position ID
     * @param targetPositionId Target position ID
     * @return true if successful
     */
    public boolean copyPositionBenefits(Integer sourcePositionId, Integer targetPositionId) {
        try {
            // Get source position benefits
            List<Map<String, Object>> sourceBenefits = getPositionBenefits(sourcePositionId);
            
            if (sourceBenefits.isEmpty()) {
                System.out.println("No benefits found for source position: " + sourcePositionId);
                return true;
            }
            
            String sql = "INSERT INTO position_benefit " +
                        "(positionId, benefitTypeId, amount, percentage, eligibilityMonths, " +
                        "maxAmount, isPercentageBased, notes, isActive, createdDate) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, true, CURRENT_TIMESTAMP)";
            
            try (Connection conn = databaseConnection.createConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                int copied = 0;
                
                for (Map<String, Object> benefit : sourceBenefits) {
                    stmt.setInt(1, targetPositionId);
                    stmt.setInt(2, (Integer) benefit.get("benefitTypeId"));
                    stmt.setBigDecimal(3, (BigDecimal) benefit.get("amount"));
                    stmt.setBigDecimal(4, (BigDecimal) benefit.get("percentage"));
                    stmt.setObject(5, benefit.get("eligibilityMonths"), Types.INTEGER);
                    stmt.setBigDecimal(6, (BigDecimal) benefit.get("maxAmount"));
                    stmt.setBoolean(7, (Boolean) benefit.get("isPercentageBased"));
                    stmt.setString(8, "Copied from position " + sourcePositionId);
                    
                    stmt.executeUpdate();
                    copied++;
                }
                
                if (copied > 0) {
                    System.out.println("Copied " + copied + " benefits from position " + 
                                     sourcePositionId + " to " + targetPositionId);
                    return true;
                }
                
            }
            
        } catch (SQLException e) {
            System.err.println("Error copying position benefits: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    /**
     * Deactivates a position benefit
     * @param positionId Position ID
     * @param benefitTypeId Benefit type ID
     * @return true if successful
     */
    public boolean deactivatePositionBenefit(Integer positionId, Integer benefitTypeId) {
        String sql = "UPDATE position_benefit SET isActive = false, updatedDate = CURRENT_TIMESTAMP " +
                    "WHERE positionId = ? AND benefitTypeId = ?";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, positionId);
            stmt.setInt(2, benefitTypeId);
            
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                System.out.println("Position benefit deactivated successfully");
                return true;
            }
            
        } catch (SQLException e) {
            System.err.println("Error deactivating position benefit: " + e.getMessage());
        }
        
        return false;
    }
    
    // UTILITY METHODS
    
    /**
     * Filters benefits by employee eligibility
     */
    private List<Map<String, Object>> filterBenefitsByEligibility(List<Map<String, Object>> benefits, 
                                                                 EmployeeModel employee) {
        List<Map<String, Object>> eligibleBenefits = new ArrayList<>();
        
        for (Map<String, Object> benefit : benefits) {
            if (isEmployeeEligible(employee, benefit)) {
                eligibleBenefits.add(benefit);
            }
        }
        
        return eligibleBenefits;
    }
    
    /**
     * Checks if employee is eligible for a specific benefit
     */
    private boolean isEmployeeEligible(EmployeeModel employee, Map<String, Object> benefit) {
        Integer eligibilityMonths = (Integer) benefit.get("eligibilityMonths");
        
        if (eligibilityMonths == null || eligibilityMonths <= 0) {
            return true; // No eligibility requirement
        }
        
        // Calculate months of employment
        LocalDateTime createdAt = employee.getCreatedAt();
        if (createdAt == null) {
            return false; // Cannot determine employment start date
        }
        
        LocalDate startDate = createdAt.toLocalDate();
        LocalDate currentDate = LocalDate.now();
        
        long monthsEmployed = java.time.temporal.ChronoUnit.MONTHS.between(startDate, currentDate);
        
        return monthsEmployed >= eligibilityMonths;
    }
    
    /**
     * Gets position benefit details for a specific benefit type
     */
    private Map<String, Object> getPositionBenefitDetails(Integer positionId, Integer benefitTypeId) {
        String sql = "SELECT * FROM position_benefit " +
                    "WHERE positionId = ? AND benefitTypeId = ? AND isActive = true";
        
        List<Map<String, Object>> results = executeQuery(sql, positionId, benefitTypeId);
        return results.isEmpty() ? null : results.get(0);
    }
    
    /**
     * Generic query execution method
     */
    private List<Map<String, Object>> executeQuery(String sql, Object... params) {
        List<Map<String, Object>> results = new ArrayList<>();
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            
            try (ResultSet rs = stmt.executeQuery()) {
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();
                
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = metaData.getColumnLabel(i);
                        Object value = rs.getObject(i);
                        row.put(columnName, value);
                    }
                    results.add(row);
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error executing query: " + e.getMessage());
        }
        
        return results;
    }
}