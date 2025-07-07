package DAOs;

import Models.DeductionModel;
import Models.DeductionModel.DeductionType;
import java.math.BigDecimal;
import java.sql.*;
import java.util.List;
import java.util.ArrayList;

/**
 * DeductionDAO - Data Access Object for DeductionModel
 * Handles all database operations for government deductions and tax calculations
 * @author User
 */
public class DeductionDAO {
    
    private final DatabaseConnection databaseConnection;
    
    public DeductionDAO(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }
    
    /**
     * Create - Insert new deduction rule
     * @param deduction
     * @return 
     */
    public boolean save(DeductionModel deduction) {
        String sql = "INSERT INTO deduction (typeName, deductionAmount, lowerLimit, upperLimit, baseTax, deductionRate, payrollId) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, deduction.getTypeName().name());
            pstmt.setBigDecimal(2, deduction.getDeductionAmount());
            pstmt.setBigDecimal(3, deduction.getLowerLimit());
            pstmt.setBigDecimal(4, deduction.getUpperLimit());
            pstmt.setBigDecimal(5, deduction.getBaseTax());
            pstmt.setBigDecimal(6, deduction.getDeductionRate());
            
            // Handle optional payrollId
            if (deduction.getPayrollId() != null) {
                pstmt.setInt(7, deduction.getPayrollId());
            } else {
                pstmt.setNull(7, Types.INTEGER);
            }
            
            int affectedRows = pstmt.executeUpdate();
            
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        deduction.setDeductionId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error saving deduction: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Read - Find deduction by ID
     * @param deductionId
     * @return 
     */
    public DeductionModel findById(Integer deductionId) {
        String sql = "SELECT * FROM deduction WHERE deductionId = ?";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, deductionId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return mapResultSetToDeduction(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error finding deduction: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Read - Find deductions by type (SSS, PhilHealth, etc.)
     * @param deductionType
     * @return 
     */
    public List<DeductionModel> findByType(DeductionType deductionType) {
        List<DeductionModel> deductions = new ArrayList<>();
        String sql = "SELECT * FROM deduction WHERE typeName = ? AND payrollId IS NULL ORDER BY lowerLimit";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, deductionType.name());
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                deductions.add(mapResultSetToDeduction(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error finding deductions by type: " + e.getMessage());
        }
        return deductions;
    }
    
    /**
     * Read - Find deductions for a specific payroll
     * @param payrollId
     * @return 
     */
    public List<DeductionModel> findByPayrollId(Integer payrollId) {
        List<DeductionModel> deductions = new ArrayList<>();
        String sql = "SELECT * FROM deduction WHERE payrollId = ? ORDER BY typeName";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, payrollId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                deductions.add(mapResultSetToDeduction(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error finding deductions by payroll: " + e.getMessage());
        }
        return deductions;
    }
    
    /**
     * Read - Get all global deduction rules (not tied to specific payroll)
     * @return 
     */
    public List<DeductionModel> findGlobalRules() {
        List<DeductionModel> deductions = new ArrayList<>();
        String sql = "SELECT * FROM deduction WHERE payrollId IS NULL ORDER BY typeName, lowerLimit";
        
        try (Connection conn = databaseConnection.createConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                deductions.add(mapResultSetToDeduction(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving global deduction rules: " + e.getMessage());
        }
        return deductions;
    }
    
    /**
     * Update existing deduction
     * @param deduction
     * @return 
     */
    public boolean update(DeductionModel deduction) {
        if (deduction.getDeductionId() == null || deduction.getDeductionId() <= 0) {
            System.err.println("Cannot update deduction: Invalid deduction ID");
            return false;
        }
        
        String sql = "UPDATE deduction SET typeName = ?, deductionAmount = ?, lowerLimit = ?, " +
                    "upperLimit = ?, baseTax = ?, deductionRate = ?, payrollId = ? WHERE deductionId = ?";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, deduction.getTypeName().name());
            pstmt.setBigDecimal(2, deduction.getDeductionAmount());
            pstmt.setBigDecimal(3, deduction.getLowerLimit());
            pstmt.setBigDecimal(4, deduction.getUpperLimit());
            pstmt.setBigDecimal(5, deduction.getBaseTax());
            pstmt.setBigDecimal(6, deduction.getDeductionRate());
            
            if (deduction.getPayrollId() != null) {
                pstmt.setInt(7, deduction.getPayrollId());
            } else {
                pstmt.setNull(7, Types.INTEGER);
            }
            
            pstmt.setInt(8, deduction.getDeductionId());
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating deduction: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Delete deduction
     * @param deductionId
     * @return 
     */
    public boolean deleteById(Integer deductionId) {
        String sql = "DELETE FROM deduction WHERE deductionId = ?";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, deductionId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting deduction: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Delete all deductions for a payroll
     * @param payrollId
     * @return 
     */
    public boolean deleteByPayrollId(Integer payrollId) {
        String sql = "DELETE FROM deduction WHERE payrollId = ?";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, payrollId);
            return pstmt.executeUpdate() >= 0;
        } catch (SQLException e) {
            System.err.println("Error deleting deductions by payroll: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Calculate SSS deduction based on monthly salary
     * @param monthlySalary
     * @return 
     */
    public BigDecimal calculateSSSDeduction(BigDecimal monthlySalary) {
        String sql = "SELECT deductionAmount FROM deduction " +
                    "WHERE typeName = 'SSS' AND payrollId IS NULL " +
                    "AND ? >= lowerLimit AND ? <= upperLimit " +
                    "LIMIT 1";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setBigDecimal(1, monthlySalary);
            pstmt.setBigDecimal(2, monthlySalary);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getBigDecimal("deductionAmount");
            }
        } catch (SQLException e) {
            System.err.println("Error calculating SSS deduction: " + e.getMessage());
        }
        return BigDecimal.ZERO;
    }
    
    /**
     * Calculate PhilHealth deduction based on monthly salary
     * @param monthlySalary
     * @return 
     */
    public BigDecimal calculatePhilHealthDeduction(BigDecimal monthlySalary) {
        String sql = "SELECT deductionAmount, deductionRate FROM deduction " +
                    "WHERE typeName = 'PhilHealth' AND payrollId IS NULL " +
                    "LIMIT 1";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                BigDecimal rate = rs.getBigDecimal("deductionRate");
                BigDecimal fixedAmount = rs.getBigDecimal("deductionAmount");
                
                // If rate is specified, calculate percentage; otherwise use fixed amount
                if (rate != null && rate.compareTo(BigDecimal.ZERO) > 0) {
                    return monthlySalary.multiply(rate).setScale(2, java.math.RoundingMode.HALF_UP);
                } else if (fixedAmount != null) {
                    return fixedAmount;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error calculating PhilHealth deduction: " + e.getMessage());
        }
        return BigDecimal.ZERO;
    }
    
    /**
     * Calculate Pag-IBIG deduction (2% of monthly salary, max ₱100)
     * @param monthlySalary
     * @return 
     */
    public BigDecimal calculatePagIbigDeduction(BigDecimal monthlySalary) {
        // Standard Pag-IBIG calculation: 2% of monthly salary, max ₱100
        BigDecimal rate = new BigDecimal("0.02"); // 2%
        BigDecimal maxDeduction = new BigDecimal("100.00");
        
        BigDecimal calculated = monthlySalary.multiply(rate).setScale(2, java.math.RoundingMode.HALF_UP);
        
        // Return the smaller of calculated amount or maximum
        return calculated.compareTo(maxDeduction) > 0 ? maxDeduction : calculated;
    }
    
    /**
     * Calculate withholding tax based on taxable income
     * @param taxableIncome
     * @return 
     */
    public BigDecimal calculateWithholdingTax(BigDecimal taxableIncome) {
        String sql = "SELECT deductionAmount, lowerLimit, upperLimit, baseTax, deductionRate FROM deduction " +
                    "WHERE typeName = 'Withholding Tax' AND payrollId IS NULL " +
                    "AND ? >= lowerLimit AND ? <= upperLimit " +
                    "ORDER BY lowerLimit " +
                    "LIMIT 1";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setBigDecimal(1, taxableIncome);
            pstmt.setBigDecimal(2, taxableIncome);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                BigDecimal baseTax = rs.getBigDecimal("baseTax");
                BigDecimal rate = rs.getBigDecimal("deductionRate");
                BigDecimal lowerLimit = rs.getBigDecimal("lowerLimit");
                BigDecimal fixedAmount = rs.getBigDecimal("deductionAmount");
                
                // If progressive tax (has base tax and rate)
                if (baseTax != null && rate != null && lowerLimit != null) {
                    BigDecimal excessAmount = taxableIncome.subtract(lowerLimit);
                    return baseTax.add(excessAmount.multiply(rate)).setScale(2, java.math.RoundingMode.HALF_UP);
                }
                // If fixed amount
                else if (fixedAmount != null) {
                    return fixedAmount;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error calculating withholding tax: " + e.getMessage());
        }
        return BigDecimal.ZERO;
    }
    
    /**
     * Calculate late deduction based on hours late
     * @param hoursLate
     * @param hourlyRate
     * @return 
     */
    public BigDecimal calculateLateDeduction(BigDecimal hoursLate, BigDecimal hourlyRate) {
        if (hoursLate == null || hourlyRate == null) {
            return BigDecimal.ZERO;
        }
        
        // Late deduction = hours late × hourly rate
        return hoursLate.multiply(hourlyRate).setScale(2, java.math.RoundingMode.HALF_UP);
    }
    
    /**
     * Get total deductions for a salary amount
     * @param monthlySalary
     * @param taxableIncome
     * @return 
     */
    public DeductionSummary calculateAllDeductions(BigDecimal monthlySalary, BigDecimal taxableIncome) {
        DeductionSummary summary = new DeductionSummary();
        
        summary.setSssDeduction(calculateSSSDeduction(monthlySalary));
        summary.setPhilhealthDeduction(calculatePhilHealthDeduction(monthlySalary));
        summary.setPagibigDeduction(calculatePagIbigDeduction(monthlySalary));
        summary.setWithholdingTax(calculateWithholdingTax(taxableIncome));
        
        // Calculate total
        BigDecimal total = summary.getSssDeduction()
                .add(summary.getPhilhealthDeduction())
                .add(summary.getPagibigDeduction())
                .add(summary.getWithholdingTax());
        summary.setTotalDeductions(total);
        
        return summary;
    }
    
    /**
     * Initialize default government deduction rules
     * This method sets up the standard deduction brackets and rates
     * @return 
     */
    public boolean initializeDefaultRules() {
        try (Connection conn = databaseConnection.createConnection()) {
            conn.setAutoCommit(false);
            
            try {
                // Clear existing rules
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("DELETE FROM deduction WHERE payrollId IS NULL");
                }
                
                // Insert SSS brackets (2024 rates)
                insertSSSRules(conn);
                
                // Insert PhilHealth rules
                insertPhilHealthRules(conn);
                
                // Insert Withholding Tax brackets
                insertWithholdingTaxRules(conn);
                
                conn.commit();
                System.out.println("✅ Default deduction rules initialized successfully");
                return true;
                
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
            
        } catch (SQLException e) {
            System.err.println("Error initializing default rules: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Helper method to insert SSS contribution tables
     */
    private void insertSSSRules(Connection conn) throws SQLException {
        String sql = "INSERT INTO deduction (typeName, deductionAmount, lowerLimit, upperLimit) VALUES (?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            // SSS contribution brackets (sample rates)
            Object[][] sssRates = {
                {new BigDecimal("4000"), new BigDecimal("0"), new BigDecimal("4249.99")},
                {new BigDecimal("4500"), new BigDecimal("4250"), new BigDecimal("4749.99")},
                {new BigDecimal("5000"), new BigDecimal("4750"), new BigDecimal("5249.99")},
                // Add more brackets as needed
                {new BigDecimal("30000"), new BigDecimal("29750"), new BigDecimal("999999.99")}
            };
            
            for (Object[] rate : sssRates) {
                pstmt.setString(1, "SSS");
                pstmt.setBigDecimal(2, (BigDecimal) rate[0]);
                pstmt.setBigDecimal(3, (BigDecimal) rate[1]);
                pstmt.setBigDecimal(4, (BigDecimal) rate[2]);
                pstmt.addBatch();
            }
            
            pstmt.executeBatch();
        }
    }
    
    /**
     * Helper method to insert PhilHealth rules
     */
    private void insertPhilHealthRules(Connection conn) throws SQLException {
        String sql = "INSERT INTO deduction (typeName, deductionRate) VALUES (?, ?)";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "PhilHealth");
            pstmt.setBigDecimal(2, new BigDecimal("0.0275")); // 2.75% employee share
            pstmt.executeUpdate();
        }
    }
    
    /**
     * Helper method to insert withholding tax brackets
     */
    private void insertWithholdingTaxRules(Connection conn) throws SQLException {
        String sql = "INSERT INTO deduction (typeName, lowerLimit, upperLimit, baseTax, deductionRate) VALUES (?, ?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            // Philippine withholding tax brackets (sample)
            Object[][] taxBrackets = {
                {new BigDecimal("0"), new BigDecimal("20833"), new BigDecimal("0"), new BigDecimal("0")},
                {new BigDecimal("20834"), new BigDecimal("33333"), new BigDecimal("0"), new BigDecimal("0.20")},
                {new BigDecimal("33334"), new BigDecimal("66667"), new BigDecimal("2500"), new BigDecimal("0.25")},
                {new BigDecimal("66668"), new BigDecimal("166667"), new BigDecimal("10833"), new BigDecimal("0.30")},
                {new BigDecimal("166668"), new BigDecimal("666667"), new BigDecimal("40833"), new BigDecimal("0.32")},
                {new BigDecimal("666668"), new BigDecimal("999999999"), new BigDecimal("200833"), new BigDecimal("0.35")}
            };
            
            for (Object[] bracket : taxBrackets) {
                pstmt.setString(1, "Withholding Tax");
                pstmt.setBigDecimal(2, (BigDecimal) bracket[0]);
                pstmt.setBigDecimal(3, (BigDecimal) bracket[1]);
                pstmt.setBigDecimal(4, (BigDecimal) bracket[2]);
                pstmt.setBigDecimal(5, (BigDecimal) bracket[3]);
                pstmt.addBatch();
            }
            
            pstmt.executeBatch();
        }
    }
    
    /**
     * Check if deduction exists
     * @param deductionId
     * @return 
     */
    public boolean exists(Integer deductionId) {
        String sql = "SELECT COUNT(*) FROM deduction WHERE deductionId = ?";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, deductionId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error checking deduction existence: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Helper method to map ResultSet to DeductionModel
     */
    private DeductionModel mapResultSetToDeduction(ResultSet rs) throws SQLException {
        DeductionModel deduction = new DeductionModel();
        
        deduction.setDeductionId(rs.getInt("deductionId"));
        deduction.setTypeName(DeductionType.valueOf(rs.getString("typeName")));
        deduction.setDeductionAmount(rs.getBigDecimal("deductionAmount"));
        deduction.setLowerLimit(rs.getBigDecimal("lowerLimit"));
        deduction.setUpperLimit(rs.getBigDecimal("upperLimit"));
        deduction.setBaseTax(rs.getBigDecimal("baseTax"));
        deduction.setDeductionRate(rs.getBigDecimal("deductionRate"));
        
        // Handle nullable payrollId
        int payrollId = rs.getInt("payrollId");
        if (!rs.wasNull()) {
            deduction.setPayrollId(payrollId);
        }
        
        return deduction;
    }
    
    /**
     * Inner class for deduction summary
     */
    public static class DeductionSummary {
        private BigDecimal sssDeduction = BigDecimal.ZERO;
        private BigDecimal philhealthDeduction = BigDecimal.ZERO;
        private BigDecimal pagibigDeduction = BigDecimal.ZERO;
        private BigDecimal withholdingTax = BigDecimal.ZERO;
        private BigDecimal lateDeduction = BigDecimal.ZERO;
        private BigDecimal totalDeductions = BigDecimal.ZERO;
        
        // Getters and setters
        public BigDecimal getSssDeduction() { return sssDeduction; }
        public void setSssDeduction(BigDecimal sssDeduction) { this.sssDeduction = sssDeduction; }
        
        public BigDecimal getPhilhealthDeduction() { return philhealthDeduction; }
        public void setPhilhealthDeduction(BigDecimal philhealthDeduction) { this.philhealthDeduction = philhealthDeduction; }
        
        public BigDecimal getPagibigDeduction() { return pagibigDeduction; }
        public void setPagibigDeduction(BigDecimal pagibigDeduction) { this.pagibigDeduction = pagibigDeduction; }
        
        public BigDecimal getWithholdingTax() { return withholdingTax; }
        public void setWithholdingTax(BigDecimal withholdingTax) { this.withholdingTax = withholdingTax; }
        
        public BigDecimal getLateDeduction() { return lateDeduction; }
        public void setLateDeduction(BigDecimal lateDeduction) { this.lateDeduction = lateDeduction; }
        
        public BigDecimal getTotalDeductions() { return totalDeductions; }
        public void setTotalDeductions(BigDecimal totalDeductions) { this.totalDeductions = totalDeductions; }
        
        @Override
        public String toString() {
            return String.format("Deductions: SSS=₱%.2f, PhilHealth=₱%.2f, Pag-IBIG=₱%.2f, Tax=₱%.2f, Total=₱%.2f",
                    sssDeduction, philhealthDeduction, pagibigDeduction, withholdingTax, totalDeductions);
        }
    }
}