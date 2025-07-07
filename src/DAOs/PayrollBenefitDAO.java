
package DAOs;

import Models.PayrollBenefit;
import java.math.BigDecimal;
import java.sql.*;
import java.util.List;
import java.util.ArrayList;

/**
 * PayrollBenefitDAO - Junction table DAO for payroll-benefit relationships
 * Links payroll records with benefit types and stores benefit amounts
 * @author Chad
 */
public class PayrollBenefitDAO {
    
    private final DatabaseConnection databaseConnection;
    
    public PayrollBenefitDAO(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }
    
    /**
     * Create - Add benefit to payroll
     * @param payrollBenefit
     * @return 
     */
    public boolean save(PayrollBenefit payrollBenefit) {
        String sql = "INSERT INTO payrollbenefit (benefitAmount, payrollId, benefitTypeId) VALUES (?, ?, ?)";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setBigDecimal(1, payrollBenefit.getBenefitAmount());
            pstmt.setInt(2, payrollBenefit.getPayrollId());
            pstmt.setInt(3, payrollBenefit.getBenefitTypeId());
            
            int rowsAffected = pstmt.executeUpdate();
            
            if (rowsAffected > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        payrollBenefit.setPayrollBenefitId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
            return false;
            
        } catch (SQLException e) {
            System.err.println("Error saving payroll benefit: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Read - Find payroll benefit by ID
     * @param payrollBenefitId
     * @return 
     */
    public PayrollBenefit findById(Integer payrollBenefitId) {
        String sql = "SELECT pb.*, bt.benefitName " +
                    "FROM payrollbenefit pb " +
                    "JOIN benefittype bt ON pb.benefitTypeId = bt.benefitTypeId " +
                    "WHERE pb.payrollBenefitId = ?";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, payrollBenefitId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                PayrollBenefit pb = new PayrollBenefit();
                pb.setPayrollBenefitId(rs.getInt("payrollBenefitId"));
                pb.setBenefitAmount(rs.getBigDecimal("benefitAmount"));
                pb.setPayrollId(rs.getInt("payrollId"));
                pb.setBenefitTypeId(rs.getInt("benefitTypeId"));
                return pb;
            }
        } catch (SQLException e) {
            System.err.println("Error finding payroll benefit: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Read - Find all benefits for a payroll
     * @param payrollId
     * @return 
     */
    public List<PayrollBenefit> findByPayrollId(Integer payrollId) {
        List<PayrollBenefit> benefitList = new ArrayList<>();
        String sql = "SELECT pb.*, bt.benefitName, bt.benefitDescription " +
                    "FROM payrollbenefit pb " +
                    "JOIN benefittype bt ON pb.benefitTypeId = bt.benefitTypeId " +
                    "WHERE pb.payrollId = ? ORDER BY bt.benefitName";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, payrollId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                PayrollBenefit pb = new PayrollBenefit();
                pb.setPayrollBenefitId(rs.getInt("payrollBenefitId"));
                pb.setBenefitAmount(rs.getBigDecimal("benefitAmount"));
                pb.setPayrollId(rs.getInt("payrollId"));
                pb.setBenefitTypeId(rs.getInt("benefitTypeId"));
                benefitList.add(pb);
            }
        } catch (SQLException e) {
            System.err.println("Error finding payroll benefits: " + e.getMessage());
        }
        return benefitList;
    }
    
    /**
     * Read - Find all payrolls that have a specific benefit type
     * @param benefitTypeId
     * @return 
     */
    public List<PayrollBenefit> findByBenefitTypeId(Integer benefitTypeId) {
        List<PayrollBenefit> benefitList = new ArrayList<>();
        String sql = "SELECT * FROM payrollbenefit WHERE benefitTypeId = ? ORDER BY payrollId";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, benefitTypeId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                PayrollBenefit pb = new PayrollBenefit();
                pb.setPayrollBenefitId(rs.getInt("payrollBenefitId"));
                pb.setBenefitAmount(rs.getBigDecimal("benefitAmount"));
                pb.setPayrollId(rs.getInt("payrollId"));
                pb.setBenefitTypeId(rs.getInt("benefitTypeId"));
                benefitList.add(pb);
            }
        } catch (SQLException e) {
            System.err.println("Error finding payroll benefits by type: " + e.getMessage());
        }
        return benefitList;
    }
    
    /**
     * Update benefit amount
     * @param payrollBenefit
     * @return 
     */
    public boolean update(PayrollBenefit payrollBenefit) {
        String sql = "UPDATE payrollbenefit SET benefitAmount = ?, payrollId = ?, benefitTypeId = ? WHERE payrollBenefitId = ?";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setBigDecimal(1, payrollBenefit.getBenefitAmount());
            pstmt.setInt(2, payrollBenefit.getPayrollId());
            pstmt.setInt(3, payrollBenefit.getBenefitTypeId());
            pstmt.setInt(4, payrollBenefit.getPayrollBenefitId());
            
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Error updating payroll benefit: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Delete payroll benefit
     * @param payrollBenefitId
     * @return 
     */
    public boolean deleteById(Integer payrollBenefitId) {
        String sql = "DELETE FROM payrollbenefit WHERE payrollBenefitId = ?";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, payrollBenefitId);
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Error deleting payroll benefit: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Delete all benefits for a payroll
     * @param payrollId
     * @return 
     */
    public boolean deleteByPayrollId(Integer payrollId) {
        String sql = "DELETE FROM payrollbenefit WHERE payrollId = ?";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, payrollId);
            return pstmt.executeUpdate() >= 0;
            
        } catch (SQLException e) {
            System.err.println("Error deleting payroll benefits by payroll: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Calculate total benefits for a payroll
     * @param payrollId
     * @return 
     */
    public BigDecimal getTotalBenefits(Integer payrollId) {
        String sql = "SELECT COALESCE(SUM(benefitAmount), 0) FROM payrollbenefit WHERE payrollId = ?";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, payrollId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getBigDecimal(1);
            }
        } catch (SQLException e) {
            System.err.println("Error getting total benefits: " + e.getMessage());
        }
        return BigDecimal.ZERO;
    }
    
    /**
     * Get benefit breakdown for a payroll
     * @param payrollId
     * @return 
     */
    public List<BenefitBreakdown> getBenefitBreakdown(Integer payrollId) {
        List<BenefitBreakdown> breakdown = new ArrayList<>();
        String sql = "SELECT bt.benefitName, pb.benefitAmount " +
                    "FROM payrollbenefit pb " +
                    "JOIN benefittype bt ON pb.benefitTypeId = bt.benefitTypeId " +
                    "WHERE pb.payrollId = ? " +
                    "ORDER BY bt.benefitName";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, payrollId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                BenefitBreakdown item = new BenefitBreakdown();
                item.setBenefitName(rs.getString("benefitName"));
                item.setBenefitAmount(rs.getBigDecimal("benefitAmount"));
                breakdown.add(item);
            }
        } catch (SQLException e) {
            System.err.println("Error getting benefit breakdown: " + e.getMessage());
        }
        return breakdown;
    }
    
    /**
     * Generate benefits for payroll based on employee position
     * This method automatically creates benefit records based on position-benefit mappings
     * @param payrollId
     * @param employeeId
     * @return 
     */
    public boolean generateBenefitsForPayroll(Integer payrollId, Integer employeeId) {
        String sql = "INSERT INTO payrollbenefit (benefitAmount, payrollId, benefitTypeId) " +
                    "SELECT pb.benefitValue, ?, pb.benefitTypeId " +
                    "FROM employee e " +
                    "JOIN positionbenefit pb ON e.positionId = pb.positionId " +
                    "WHERE e.employeeId = ? " +
                    "AND pb.benefitValue > 0";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, payrollId);
            pstmt.setInt(2, employeeId);
            
            int rowsAffected = pstmt.executeUpdate();
            System.out.println("✅ Generated " + rowsAffected + " benefits for payroll " + payrollId);
            return rowsAffected >= 0; // Return true even if 0 benefits (employee might not have benefits)
            
        } catch (SQLException e) {
            System.err.println("Error generating benefits for payroll: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get specific benefit amount for payroll
     * @param payrollId
     * @param benefitTypeId
     * @return 
     */
    public BigDecimal getBenefitAmount(Integer payrollId, Integer benefitTypeId) {
        String sql = "SELECT benefitAmount FROM payrollbenefit WHERE payrollId = ? AND benefitTypeId = ?";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, payrollId);
            pstmt.setInt(2, benefitTypeId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getBigDecimal("benefitAmount");
            }
        } catch (SQLException e) {
            System.err.println("Error getting benefit amount: " + e.getMessage());
        }
        return BigDecimal.ZERO;
    }
    
    /**
     * Update specific benefit amount
     * @param payrollId
     * @param benefitTypeId
     * @param amount
     * @return 
     */
    public boolean updateBenefitAmount(Integer payrollId, Integer benefitTypeId, BigDecimal amount) {
        // First check if benefit exists
        if (getBenefitAmount(payrollId, benefitTypeId).compareTo(BigDecimal.ZERO) > 0) {
            // Update existing
            String sql = "UPDATE payrollbenefit SET benefitAmount = ? WHERE payrollId = ? AND benefitTypeId = ?";
            
            try (Connection conn = databaseConnection.createConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                
                pstmt.setBigDecimal(1, amount);
                pstmt.setInt(2, payrollId);
                pstmt.setInt(3, benefitTypeId);
                
                return pstmt.executeUpdate() > 0;
                
            } catch (SQLException e) {
                System.err.println("Error updating benefit amount: " + e.getMessage());
                return false;
            }
        } else {
            // Create new
            PayrollBenefit pb = new PayrollBenefit();
            pb.setPayrollId(payrollId);
            pb.setBenefitTypeId(benefitTypeId);
            pb.setBenefitAmount(amount);
            return save(pb);
        }
    }
    
    /**
     * Bulk insert benefits for payroll
     * @param benefitList
     * @return 
     */
    public boolean bulkInsert(List<PayrollBenefit> benefitList) {
        if (benefitList == null || benefitList.isEmpty()) {
            return false;
        }
        
        String sql = "INSERT INTO payrollbenefit (benefitAmount, payrollId, benefitTypeId) VALUES (?, ?, ?)";
        
        try (Connection conn = databaseConnection.createConnection()) {
            conn.setAutoCommit(false);
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (PayrollBenefit pb : benefitList) {
                    pstmt.setBigDecimal(1, pb.getBenefitAmount());
                    pstmt.setInt(2, pb.getPayrollId());
                    pstmt.setInt(3, pb.getBenefitTypeId());
                    pstmt.addBatch();
                }
                
                int[] results = pstmt.executeBatch();
                conn.commit();
                
                // Check if all inserts were successful
                for (int result : results) {
                    if (result <= 0) {
                        return false;
                    }
                }
                return true;
                
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
            
        } catch (SQLException e) {
            System.err.println("Error bulk inserting payroll benefits: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if benefit exists for payroll
     * @param payrollId
     * @param benefitTypeId
     * @return 
     */
    public boolean benefitExists(Integer payrollId, Integer benefitTypeId) {
        String sql = "SELECT COUNT(*) FROM payrollbenefit WHERE payrollId = ? AND benefitTypeId = ?";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, payrollId);
            pstmt.setInt(2, benefitTypeId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error checking benefit existence: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Inner class for benefit breakdown
     */
    public static class BenefitBreakdown {
        private String benefitName;
        private BigDecimal benefitAmount;
        
        public String getBenefitName() { return benefitName; }
        public void setBenefitName(String benefitName) { this.benefitName = benefitName; }
        
        public BigDecimal getBenefitAmount() { return benefitAmount; }
        public void setBenefitAmount(BigDecimal benefitAmount) { this.benefitAmount = benefitAmount; }
        
        @Override
        public String toString() {
            return String.format("%s: ₱%.2f", benefitName, benefitAmount);
        }
    }
}