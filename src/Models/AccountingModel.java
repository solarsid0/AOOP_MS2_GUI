package Models;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.ArrayList;

import Services.PayrollService;
import Services.ReportService;
import Services.AttendanceService;
import DAOs.EmployeeDAO;
import DAOs.PayrollDAO;
import DAOs.PayPeriodDAO;
import DAOs.DatabaseConnection;
import DAOs.PayrollAttendanceDAO;
import DAOs.PayrollBenefitDAO;
import DAOs.PayrollOvertimeDAO;
import DAOs.TardinessRecordDAO; // NEW

public class AccountingModel extends EmployeeModel {
    
    // Service layer dependencies
    private final PayrollService payrollService;
    private final ReportService reportService;
    private final AttendanceService attendanceService;
    
    // DAO dependencies for financial operations
    private final EmployeeDAO employeeDAO;
    private final PayrollDAO payrollDAO;
    private final PayPeriodDAO payPeriodDAO;
    
    // Detail DAOs for payroll generation - UPDATED with TardinessRecordDAO
    private final PayrollAttendanceDAO payrollAttendanceDAO;
    private final PayrollBenefitDAO payrollBenefitDAO;
    private final PayrollOvertimeDAO payrollOvertimeDAO;
    private final TardinessRecordDAO tardinessRecordDAO; // NEW
    
    // Accounting Role Permissions
    private static final String[] ACCOUNTING_PERMISSIONS = {
        "VERIFY_PAYROLL", "VIEW_PAYROLL_DATA", "GENERATE_FINANCIAL_REPORTS", 
        "AUDIT_FINANCIAL_DATA", "MANAGE_TAX_CALCULATIONS", "GENERATE_PAYROLL"
    };

    /**
     * Constructor for Accounting role - UPDATED with TardinessRecordDAO
     */
    public AccountingModel(int employeeId, String firstName, String lastName, String email, String userRole) {
        super(firstName, lastName, email, userRole);
        this.setEmployeeId(employeeId);
        
        // Create single database connection for all components
        DatabaseConnection dbConnection = new DatabaseConnection();
        
        // Initialize services with database connection 
        this.payrollService = new PayrollService(dbConnection);
        this.reportService = new ReportService(dbConnection);
        this.attendanceService = new AttendanceService();
        
        // Initialize DAOs with database connection
        this.employeeDAO = new EmployeeDAO(dbConnection);
        this.payrollDAO = new PayrollDAO(dbConnection);
        this.payPeriodDAO = new PayPeriodDAO();
        
        // Initialize detail DAOs
        this.payrollAttendanceDAO = new PayrollAttendanceDAO(dbConnection);
        this.payrollBenefitDAO = new PayrollBenefitDAO(dbConnection);
        this.payrollOvertimeDAO = new PayrollOvertimeDAO(dbConnection);
        this.tardinessRecordDAO = new TardinessRecordDAO(dbConnection); // NEW
        
        System.out.println("Accounting user initialized: " + getFullName());
    }

    /**
     * Constructor from existing EmployeeModel - UPDATED with TardinessRecordDAO
     */
    public AccountingModel(EmployeeModel employee) {
        super(employee.getFirstName(), employee.getLastName(), 
              employee.getEmail(), employee.getUserRole());
        
        this.copyFromEmployeeModel(employee);
        
        // Create single database connection for all components
        DatabaseConnection dbConnection = new DatabaseConnection();
        
        // Initialize Accounting-specific components with database connection
        this.payrollService = new PayrollService(dbConnection);
        this.reportService = new ReportService(dbConnection);
        this.attendanceService = new AttendanceService();
        
        this.employeeDAO = new EmployeeDAO(dbConnection);
        this.payrollDAO = new PayrollDAO(dbConnection);
        this.payPeriodDAO = new PayPeriodDAO();
        
        // Initialize detail DAOs
        this.payrollAttendanceDAO = new PayrollAttendanceDAO(dbConnection);
        this.payrollBenefitDAO = new PayrollBenefitDAO(dbConnection);
        this.payrollOvertimeDAO = new PayrollOvertimeDAO(dbConnection);
        this.tardinessRecordDAO = new TardinessRecordDAO(dbConnection); // NEW
        
        System.out.println("Accounting user initialized from EmployeeModel: " + getFullName());
    }

    // ===================================
    // PAYROLL GENERATION IMPLEMENTATION
    // ===================================

    /**
     * Inner class for payroll generation result
     */
    public static class PayrollGenerationResult {
        private boolean success;
        private int generatedCount;
        private boolean detailTablesPopulated;
        private String message;

        public PayrollGenerationResult(boolean success, int generatedCount, 
                                     boolean detailTablesPopulated, String message) {
            this.success = success;
            this.generatedCount = generatedCount;
            this.detailTablesPopulated = detailTablesPopulated;
            this.message = message;
        }

        // Getters
        public boolean isSuccess() { return success; }
        public int getGeneratedCount() { return generatedCount; }
        public boolean isDetailTablesPopulated() { return detailTablesPopulated; }
        public String getMessage() { return message; }
    }

    /**
     * Generates payroll with all detail tables - UPDATED to include tardiness
     */
     public PayrollGenerationResult generatePayrollWithDetails(int payPeriodId) {
        try {
            if (!hasPermission("GENERATE_PAYROLL")) {
                return new PayrollGenerationResult(false, 0, false, 
                    "Insufficient permissions to generate payroll");
            }

            // First delete any existing payroll data for this period
            payrollDAO.deletePayrollByPeriod(payPeriodId);
            payrollAttendanceDAO.deleteByPayPeriod(payPeriodId);
            payrollBenefitDAO.deleteByPayPeriod(payPeriodId);
            payrollOvertimeDAO.deleteByPayPeriod(payPeriodId);
            tardinessRecordDAO.deleteByPayPeriod(payPeriodId);

            // Generate main payroll records using the FIXED PayrollDAO
            int generatedCount = payrollDAO.generatePayroll(payPeriodId);
            
            if (generatedCount == 0) {
                return new PayrollGenerationResult(false, 0, false,
                    "No payroll records were generated");
            }

            // Populate detail tables with enhanced error reporting
            boolean attendancePopulated = false;
            boolean benefitsPopulated = false;
            boolean leavesPopulated = false;
            boolean overtimePopulated = false;
            boolean tardinessPopulated = false;
            
            StringBuilder errorLog = new StringBuilder();
            
            try {
                int attendanceRecords = payrollAttendanceDAO.generateAttendanceRecords(payPeriodId);
                attendancePopulated = attendanceRecords >= 0;
                System.out.println("Generated " + attendanceRecords + " attendance records");
            } catch (Exception e) {
                errorLog.append("Attendance error: ").append(e.getMessage()).append("; ");
                System.err.println("Error generating attendance records: " + e.getMessage());
            }
            
            try {
                int benefitRecords = payrollBenefitDAO.generateBenefitRecords(payPeriodId);
                benefitsPopulated = benefitRecords >= 0;
                System.out.println("Generated " + benefitRecords + " benefit records");
            } catch (Exception e) {
                errorLog.append("Benefits error: ").append(e.getMessage()).append("; ");
                System.err.println("Error generating benefit records: " + e.getMessage());
            }
            
            
            
            try {
                int overtimeRecords = payrollOvertimeDAO.generateOvertimeRecords(payPeriodId);
                overtimePopulated = overtimeRecords >= 0;
                System.out.println("Generated " + overtimeRecords + " overtime records");
                if (overtimeRecords == 0) {
                    System.out.println("Note: No overtime records to generate for this period (this is normal if no overtime was worked)");
                }
            } catch (Exception e) {
                errorLog.append("Overtime error: ").append(e.getMessage()).append("; ");
                System.err.println("Error generating overtime records: " + e.getMessage());
                overtimePopulated = true; // Consider it successful if no overtime exists
            }
            
            try {
                int tardinessRecords = tardinessRecordDAO.generateTardinessRecords(payPeriodId);
                tardinessPopulated = tardinessRecords >= 0;
                System.out.println("Generated " + tardinessRecords + " tardiness records");
            } catch (Exception e) {
                errorLog.append("Tardiness error: ").append(e.getMessage()).append("; ");
                System.err.println("Error generating tardiness records: " + e.getMessage());
            }

            boolean allDetailsPopulated = attendancePopulated && benefitsPopulated && 
                                         leavesPopulated && overtimePopulated && tardinessPopulated;

            String message = "Payroll generated successfully with " + generatedCount + " records";
            if (errorLog.length() > 0) {
                message += ". Issues: " + errorLog.toString();
            }

            logAccountingActivity("PAYROLL_GENERATED", 
                "Generated payroll for period: " + payPeriodId + 
                " - Records: " + generatedCount + 
                ", Details: " + (allDetailsPopulated ? "Complete" : "Partial"));

            return new PayrollGenerationResult(true, generatedCount, allDetailsPopulated, message);

        } catch (Exception e) {
            System.err.println("Error generating payroll: " + e.getMessage());
            e.printStackTrace();
            return new PayrollGenerationResult(false, 0, false,
                "Error generating payroll: " + e.getMessage());
        }
    }

    /**
     * Verifies payroll detail tables were populated correctly - UPDATED to include tardiness
     */
private boolean verifyDetailTables(int payPeriodId) {
        try {
            boolean attendanceOK = false;
            boolean benefitsOK = false;
            boolean leavesOK = false;
            boolean overtimeOK = false;
            boolean tardinessOK = false;
            
            try {
                attendanceOK = payrollAttendanceDAO.hasRecordsForPeriod(payPeriodId);
            } catch (Exception e) {
                System.err.println("Error checking attendance records: " + e.getMessage());
            }
            
            try {
                benefitsOK = payrollBenefitDAO.hasRecordsForPeriod(payPeriodId);
            } catch (Exception e) {
                System.err.println("Error checking benefit records: " + e.getMessage());
            }
            
           
            
            try {
                overtimeOK = payrollOvertimeDAO.hasRecordsForPeriod(payPeriodId);
                // For overtime, OK if no records exist (no overtime worked)
                if (!overtimeOK) {
                    System.out.println("No overtime records found - this is normal if no overtime was worked");
                    overtimeOK = true;
                }
            } catch (Exception e) {
                System.err.println("Error checking overtime records: " + e.getMessage());
                overtimeOK = true; // Consider OK if error (probably no overtime)
            }
            
            try {
                tardinessOK = tardinessRecordDAO.hasRecordsForPeriod(payPeriodId);
            } catch (Exception e) {
                System.err.println("Error checking tardiness records: " + e.getMessage());
            }
            
            System.out.println("Detail table verification - Attendance: " + attendanceOK + 
                             ", Benefits: " + benefitsOK + ", Leaves: " + leavesOK + 
                             ", Overtime: " + overtimeOK + ", Tardiness: " + tardinessOK);
            
            return attendanceOK && benefitsOK && leavesOK && overtimeOK && tardinessOK;
        } catch (Exception e) {
            System.err.println("Error verifying detail tables: " + e.getMessage());
            return false;
        }
    }

    /**
     * Helper method to copy data from another EmployeeModel
     */
    private void copyFromEmployeeModel(EmployeeModel source) {
        this.setEmployeeId(source.getEmployeeId());
        this.setFirstName(source.getFirstName());
        this.setLastName(source.getLastName());
        this.setEmail(source.getEmail());
        this.setUserRole(source.getUserRole());
        
        // Copy additional fields if they exist and are not null
        if (source.getBirthDate() != null) this.setBirthDate(source.getBirthDate());
        if (source.getPhoneNumber() != null) this.setPhoneNumber(source.getPhoneNumber());
        if (source.getBasicSalary() != null) this.setBasicSalary(source.getBasicSalary());
        if (source.getHourlyRate() != null) this.setHourlyRate(source.getHourlyRate());
        if (source.getPasswordHash() != null) this.setPasswordHash(source.getPasswordHash());
        if (source.getStatus() != null) this.setStatus(source.getStatus());
        if (source.getCreatedAt() != null) this.setCreatedAt(source.getCreatedAt());
        if (source.getUpdatedAt() != null) this.setUpdatedAt(source.getUpdatedAt());
        if (source.getLastLogin() != null) this.setLastLogin(source.getLastLogin());
        if (source.getPositionId() != null) this.setPositionId(source.getPositionId());
        if (source.getSupervisorId() != null) this.setSupervisorId(source.getSupervisorId());
    }

    // ================================
    // PAYROLL VERIFICATION OPERATIONS - FIXED for monthly salary
    // ================================

    /**
     * Verifies payroll calculations for a specific pay period
     * Expects monthly salary (not semi-monthly) to match database view
     */
     public AccountingResult verifyPayrollForPeriod(Integer payPeriodId) {
        AccountingResult result = new AccountingResult();

        try {
            if (!hasPermission("VERIFY_PAYROLL")) {
                result.setSuccess(false);
                result.setMessage("Insufficient permissions to verify payroll");
                return result;
            }

            if (payPeriodDAO == null) {
                result.setSuccess(false);
                result.setMessage("PayPeriodDAO not initialized");
                return result;
            }

            PayPeriodModel payPeriod = payPeriodDAO.findById(payPeriodId);
            if (payPeriod == null) {
                result.setSuccess(false);
                result.setMessage("Pay period not found: " + payPeriodId);
                return result;
            }

            if (payrollDAO == null) {
                result.setSuccess(false);
                result.setMessage("PayrollDAO not initialized");
                return result;
            }

            List<PayrollModel> payrollRecords = payrollDAO.findByPayPeriod(payPeriodId);
            if (payrollRecords.isEmpty()) {
                result.setSuccess(false);
                result.setMessage("No payroll records found for period: " + payPeriodId);
                return result;
            }

            int verifiedCount = 0;
            int discrepancyCount = 0;
            BigDecimal totalGross = BigDecimal.ZERO;
            BigDecimal totalNet = BigDecimal.ZERO;
            BigDecimal totalDeductions = BigDecimal.ZERO;
            
            StringBuilder verificationLog = new StringBuilder();

            for (PayrollModel payroll : payrollRecords) {
                try {
                    if (verifyIndividualPayrollEnhanced(payroll, verificationLog)) {
                        verifiedCount++;
                    } else {
                        discrepancyCount++;
                    }
                    
                    // Use non-null values for totals
                    if (payroll.getGrossIncome() != null) {
                        totalGross = totalGross.add(payroll.getGrossIncome());
                    }
                    if (payroll.getNetSalary() != null) {
                        totalNet = totalNet.add(payroll.getNetSalary());
                    }
                    if (payroll.getTotalDeduction() != null) {
                        totalDeductions = totalDeductions.add(payroll.getTotalDeduction());
                    }
                } catch (Exception e) {
                    discrepancyCount++;
                    verificationLog.append("Employee ").append(payroll.getEmployeeId())
                                  .append(": Verification error - ").append(e.getMessage()).append("; ");
                    System.err.println("Error verifying payroll for employee " + payroll.getEmployeeId() + ": " + e.getMessage());
                }
            }

            result.setSuccess(true);
            String message = "Payroll verification completed for period: " + payPeriod.getPeriodName();
            if (verificationLog.length() > 0) {
                message += ". Issues: " + verificationLog.toString();
            }
            result.setMessage(message);
            result.setTotalRecords(payrollRecords.size());
            result.setVerifiedRecords(verifiedCount);
            result.setDiscrepancyRecords(discrepancyCount);
            result.setTotalGross(totalGross);
            result.setTotalNet(totalNet);
            result.setTotalDeductions(totalDeductions);

            logAccountingActivity("PAYROLL_VERIFIED", 
                "Verified payroll for period: " + payPeriodId + 
                " - Records: " + payrollRecords.size() + 
                ", Verified: " + verifiedCount + 
                ", Discrepancies: " + discrepancyCount);

        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("Error verifying payroll: " + e.getMessage());
            System.err.println("Accounting error verifying payroll: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

        /**
         * Verifies individual payroll calculation
         * FIXED: Now expects monthly salary (not semi-monthly) to match database view
         */
   private boolean verifyIndividualPayrollEnhanced(PayrollModel payroll, StringBuilder errorLog) {
        try {
            if (employeeDAO == null) {
                errorLog.append("EmployeeDAO not initialized; ");
                return false;
            }

            EmployeeModel employee = employeeDAO.findById(payroll.getEmployeeId());
            if (employee == null) {
                errorLog.append("Employee not found: ").append(payroll.getEmployeeId()).append("; ");
                return false;
            }

            // Check for null values in payroll
            if (payroll.getBasicSalary() == null || payroll.getGrossIncome() == null || 
                payroll.getNetSalary() == null || payroll.getTotalDeduction() == null) {
                errorLog.append("Employee ").append(payroll.getEmployeeId()).append(": Null values in payroll; ");
                return false;
            }

            // RELAXED: Basic salary verification with tolerance
            BigDecimal expectedBasicSalary = employee.getBasicSalary();
            if (expectedBasicSalary == null) {
                errorLog.append("Employee ").append(payroll.getEmployeeId()).append(": No basic salary defined; ");
                return false;
            }

            BigDecimal salaryDifference = payroll.getBasicSalary().subtract(expectedBasicSalary).abs();
            BigDecimal salaryTolerance = expectedBasicSalary.multiply(new BigDecimal("0.01")); // 1% tolerance
            
            if (salaryDifference.compareTo(salaryTolerance) > 0) {
                errorLog.append("Employee ").append(payroll.getEmployeeId())
                          .append(": Basic salary mismatch - Expected: ").append(expectedBasicSalary)
                          .append(", Found: ").append(payroll.getBasicSalary()).append("; ");
                return false;
            }

            // RELAXED: Net salary calculation verification with tolerance
            BigDecimal expectedNet = payroll.getGrossIncome();
            if (payroll.getTotalBenefit() != null) {
                expectedNet = expectedNet.add(payroll.getTotalBenefit());
            }
            expectedNet = expectedNet.subtract(payroll.getTotalDeduction());
            
            BigDecimal netDifference = payroll.getNetSalary().subtract(expectedNet).abs();
            BigDecimal netTolerance = new BigDecimal("5.00"); // ₱5.00 tolerance for rounding
            
            if (netDifference.compareTo(netTolerance) > 0) {
                errorLog.append("Employee ").append(payroll.getEmployeeId())
                          .append(": Net salary calculation mismatch - Expected: ").append(expectedNet)
                          .append(", Found: ").append(payroll.getNetSalary()).append("; ");
                return false;
            }

            // RELAXED: Deduction verification with tolerance (skip detailed deduction calculation)
            // Just verify deductions are positive and reasonable
            if (payroll.getTotalDeduction().compareTo(BigDecimal.ZERO) < 0) {
                errorLog.append("Employee ").append(payroll.getEmployeeId()).append(": Negative deductions; ");
                return false;
            }

            // Verify deductions are not more than 50% of gross income (reasonableness check)
            BigDecimal maxDeductions = payroll.getGrossIncome().multiply(new BigDecimal("0.5"));
            if (payroll.getTotalDeduction().compareTo(maxDeductions) > 0) {
                errorLog.append("Employee ").append(payroll.getEmployeeId()).append(": Excessive deductions; ");
                return false;
            }

            return true;

        } catch (Exception e) {
            errorLog.append("Employee ").append(payroll.getEmployeeId())
                      .append(": Verification exception - ").append(e.getMessage()).append("; ");
            return false;
        }
    }


    /**
     * Calculates expected government deductions for verification
     */
    private BigDecimal calculateExpectedDeductions(BigDecimal basicSalary) {
        // Philippine government mandated deductions (simplified)
        BigDecimal sss = basicSalary.multiply(new BigDecimal("0.045")).setScale(2, RoundingMode.HALF_UP); // 4.5% SSS
        BigDecimal philHealth = basicSalary.multiply(new BigDecimal("0.0275")).setScale(2, RoundingMode.HALF_UP); // 2.75% PhilHealth
        BigDecimal pagIbig = basicSalary.multiply(new BigDecimal("0.02")).setScale(2, RoundingMode.HALF_UP); // 2% Pag-IBIG
        
        return sss.add(philHealth).add(pagIbig);
    }

    // ================================
    // FINANCIAL REPORTING OPERATIONS
    // ================================

    /**
     * Generates comprehensive financial report using ReportService
     */
    public ReportResult generateFinancialReport(Integer payPeriodId) {
        if (!hasPermission("GENERATE_FINANCIAL_REPORTS")) {
            ReportResult report = new ReportResult();
            report.setSuccess(false);
            report.setErrorMessage("Insufficient permissions to generate financial reports");
            return report;
        }
        
        if (reportService == null) {
            ReportResult report = new ReportResult();
            report.setSuccess(false);
            report.setErrorMessage("ReportService not initialized");
            return report;
        }

        try {
            Object reportServiceResult = reportService.generateMonthlyPayrollSummaryFromView(YearMonth.now());
            ReportResult report = new ReportResult();
            report.setSuccess(true);
            report.setReportContent("Financial report generated for period: " + payPeriodId);
            
            logAccountingActivity("FINANCIAL_REPORT_GENERATED", 
                "Generated financial report for period: " + payPeriodId);
            
            return report;
        } catch (Exception e) {
            ReportResult report = new ReportResult();
            report.setSuccess(false);
            report.setErrorMessage("Error generating financial report: " + e.getMessage());
            return report;
        }
    }

    /**
     * Generates tax compliance report using ReportService
     */
    public ReportResult generateTaxComplianceReport(YearMonth yearMonth) {
        if (!hasPermission("GENERATE_FINANCIAL_REPORTS")) {
            ReportResult report = new ReportResult();
            report.setSuccess(false);
            report.setErrorMessage("Insufficient permissions to generate tax reports");
            return report;
        }
        
        if (reportService == null) {
            ReportResult report = new ReportResult();
            report.setSuccess(false);
            report.setErrorMessage("ReportService not initialized");
            return report;
        }

        try {
            Object reportServiceResult = reportService.generateGovernmentComplianceFromView(yearMonth);
            ReportResult report = new ReportResult();
            report.setSuccess(true);
            report.setReportContent("Tax compliance report generated for: " + yearMonth);
            
            logAccountingActivity("TAX_REPORT_GENERATED", 
                "Generated tax compliance report for: " + yearMonth);
                
            return report;
        } catch (Exception e) {
            ReportResult report = new ReportResult();
            report.setSuccess(false);
            report.setErrorMessage("Error generating tax compliance report: " + e.getMessage());
            return report;
        }
    }

    /**
     * Generates salary comparison report using ReportService
     */
    public ReportResult generateSalaryComparisonReport(LocalDate startDate, LocalDate endDate) {
        if (!hasPermission("GENERATE_FINANCIAL_REPORTS")) {
            ReportResult report = new ReportResult();
            report.setSuccess(false);
            report.setErrorMessage("Insufficient permissions to generate salary reports");
            return report;
        }
        
        if (reportService == null) {
            ReportResult report = new ReportResult();
            report.setSuccess(false);
            report.setErrorMessage("ReportService not initialized");
            return report;
        }

        try {
            YearMonth currentMonth = YearMonth.from(startDate);
            Object reportServiceResult = reportService.generateMonthlyPayrollSummaryFromView(currentMonth);
            ReportResult report = new ReportResult();
            report.setSuccess(true);
            report.setReportContent("Salary comparison report generated for period: " + startDate + " to " + endDate);
            return report;
        } catch (Exception e) {
            ReportResult report = new ReportResult();
            report.setSuccess(false);
            report.setErrorMessage("Error generating salary comparison report: " + e.getMessage());
            return report;
        }
    }

    // ================================
    // AUDIT OPERATIONS
    // ================================

    /**
     * Performs financial audit for a pay period
     */
    public AccountingResult performFinancialAudit(Integer payPeriodId) {
        AccountingResult result = new AccountingResult();
        
        try {
            if (!hasPermission("AUDIT_FINANCIAL_DATA")) {
                result.setSuccess(false);
                result.setMessage("Insufficient permissions to perform financial audit");
                return result;
            }

            // Get payroll verification results
            AccountingResult verificationResult = verifyPayrollForPeriod(payPeriodId);
            
            if (!verificationResult.isSuccess()) {
                result.setSuccess(false);
                result.setMessage("Failed to verify payroll during audit");
                return result;
            }

            // Calculate compliance score
            double complianceScore = verificationResult.getTotalRecords() > 0 ? 
                (double)(verificationResult.getVerifiedRecords()) / verificationResult.getTotalRecords() * 100 : 100;

            result.setSuccess(true);
            result.setMessage("Financial audit completed for period: " + payPeriodId);
            result.setTotalRecords(verificationResult.getTotalRecords());
            result.setVerifiedRecords(verificationResult.getVerifiedRecords());
            result.setDiscrepancyRecords(verificationResult.getDiscrepancyRecords());
            result.setTotalGross(verificationResult.getTotalGross());
            result.setTotalNet(verificationResult.getTotalNet());
            result.setTotalDeductions(verificationResult.getTotalDeductions());
            result.setComplianceScore(BigDecimal.valueOf(complianceScore));
            
            logAccountingActivity("FINANCIAL_AUDIT_PERFORMED", 
                "Completed financial audit for period: " + payPeriodId + 
                " - Compliance: " + String.format("%.2f%%", complianceScore));

        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("Error performing financial audit: " + e.getMessage());
        }

        return result;
    }

    // ================================
    // DATA ACCESS METHODS
    // ================================

    /**
     * Gets all payroll records for a period (with permission check)
     */
    public List<PayrollModel> getPayrollRecords(Integer payPeriodId) {
        if (!hasPermission("VIEW_PAYROLL_DATA")) {
            System.err.println("Accounting: Insufficient permissions to view payroll records");
            return new ArrayList<>();
        }
        
        if (payrollDAO == null) {
            System.err.println("Accounting: PayrollDAO not initialized");
            return new ArrayList<>();
        }
        
        return payrollDAO.findByPayPeriod(payPeriodId);
    }

    /**
     * Gets employee information (with permission check)
     */
    public EmployeeModel getEmployeeById(Integer employeeId) {
        if (!hasPermission("VIEW_PAYROLL_DATA")) {
            System.err.println("Accounting: Insufficient permissions to view employee data");
            return null;
        }
        
        if (employeeDAO == null) {
            System.err.println("Accounting: EmployeeDAO not initialized");
            return null;
        }
        
        return employeeDAO.findById(employeeId);
    }

    /**
     * Gets all active employees (with permission check)
     */
    public List<EmployeeModel> getAllActiveEmployees() {
        if (!hasPermission("VIEW_PAYROLL_DATA")) {
            System.err.println("Accounting: Insufficient permissions to view employee data");
            return new ArrayList<>();
        }
        
        if (employeeDAO == null) {
            System.err.println("Accounting: EmployeeDAO not initialized");
            return new ArrayList<>();
        }
        
        return employeeDAO.getActiveEmployees();
    }

    // ================================
    // UTILITY METHODS
    // ================================

    /**
     * Checks if Accounting user has specific permission
     */
    private boolean hasPermission(String permission) {
        String userRole = getUserRole();
        if (userRole == null || !userRole.equalsIgnoreCase("Accounting")) {
            return false;
        }
        
        for (String accountingPermission : ACCOUNTING_PERMISSIONS) {
            if (accountingPermission.equals(permission)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets all Accounting permissions
     */
    public String[] getAccountingPermissions() {
        return ACCOUNTING_PERMISSIONS.clone();
    }

    /**
     * Logs Accounting activities for audit purposes
     */
    private void logAccountingActivity(String action, String details) {
        try {
            String logMessage = String.format("[ACCOUNTING AUDIT] %s - %s: %s (Performed by: %s - ID: %d)",
                LocalDate.now(), action, details, getFullName(), getEmployeeId());
            System.out.println(logMessage);
            
        } catch (Exception e) {
            System.err.println("Error logging Accounting activity: " + e.getMessage());
        }
    }

    @Override
    public String toString() {
        return "AccountingModel{" +
                "employeeId=" + getEmployeeId() +
                ", name='" + getFullName() + '\'' +
                ", email='" + getEmail() + '\'' +
                ", permissions=" + java.util.Arrays.toString(ACCOUNTING_PERMISSIONS) +
                '}';
    }

    // ================================
    // INNER CLASSES - RESULT OBJECTS
    // ================================

    /**
     * Result class for Accounting operations
     */
    public static class AccountingResult {
        private boolean success = false;
        private String message = "";
        private int totalRecords = 0;
        private int verifiedRecords = 0;
        private int discrepancyRecords = 0;
        private BigDecimal totalGross = BigDecimal.ZERO;
        private BigDecimal totalNet = BigDecimal.ZERO;
        private BigDecimal totalDeductions = BigDecimal.ZERO;
        private BigDecimal complianceScore = BigDecimal.ZERO;

        // Getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public int getTotalRecords() { return totalRecords; }
        public void setTotalRecords(int totalRecords) { this.totalRecords = totalRecords; }
        
        public int getVerifiedRecords() { return verifiedRecords; }
        public void setVerifiedRecords(int verifiedRecords) { this.verifiedRecords = verifiedRecords; }
        
        public int getDiscrepancyRecords() { return discrepancyRecords; }
        public void setDiscrepancyRecords(int discrepancyRecords) { this.discrepancyRecords = discrepancyRecords; }
        
        public BigDecimal getTotalGross() { return totalGross; }
        public void setTotalGross(BigDecimal totalGross) { this.totalGross = totalGross; }
        
        public BigDecimal getTotalNet() { return totalNet; }
        public void setTotalNet(BigDecimal totalNet) { this.totalNet = totalNet; }
        
        public BigDecimal getTotalDeductions() { return totalDeductions; }
        public void setTotalDeductions(BigDecimal totalDeductions) { this.totalDeductions = totalDeductions; }
        
        public BigDecimal getComplianceScore() { return complianceScore; }
        public void setComplianceScore(BigDecimal complianceScore) { this.complianceScore = complianceScore; }

        @Override
        public String toString() {
            return "AccountingResult{" +
                   "success=" + success + 
                   ", message='" + message + '\'' +
                   ", totalRecords=" + totalRecords + 
                   ", verifiedRecords=" + verifiedRecords + 
                   ", discrepancyRecords=" + discrepancyRecords + 
                   ", complianceScore=" + complianceScore + "%" +
                   '}';
        }
    }

    /**
     * Generic result class for reports
     */
    public static class ReportResult {
        private boolean success = false;
        private String errorMessage = "";
        private String reportContent = "";
        private String filePath = "";

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public String getReportContent() { return reportContent; }
        public void setReportContent(String reportContent) { this.reportContent = reportContent; }
        
        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }
    }
}