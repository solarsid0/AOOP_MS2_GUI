package Services;

import DAOs.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.sql.*;
import java.util.*;

/**
 * Enhanced ReportService - Uses database views for complex payroll calculations
 * Leverages monthly_employee_payslip and monthly_payroll_summary_report views
 * @author chad
 */
public class ReportService {
    
    // Manila timezone constant
    public static final ZoneId MANILA_TIMEZONE = ZoneId.of("Asia/Manila");
    
    // DAO Dependencies
    private final DatabaseConnection databaseConnection;
    private final EmployeeDAO employeeDAO;
    private final PayrollDAO payrollDAO;
    private final PayslipDAO payslipDAO;
    private final AttendanceDAO attendanceDAO;
    private final LeaveDAO leaveDAO;
    private final OvertimeRequestDAO overtimeDAO;
    private final PayPeriodDAO payPeriodDAO;
    
    // Service Dependencies
    private final AttendanceService attendanceService;
    private final PayrollService payrollService;
    private final LeaveService leaveService;
    private final OvertimeService overtimeService;
    
    /**
     * Constructor - initializes required DAOs and services
     */
    public ReportService() {
        this.databaseConnection = new DatabaseConnection();
        this.employeeDAO = new EmployeeDAO(databaseConnection);
        this.payrollDAO = new PayrollDAO(databaseConnection);
        this.payslipDAO = new PayslipDAO(databaseConnection);
        this.attendanceDAO = new AttendanceDAO(databaseConnection);
        this.leaveDAO = new LeaveDAO(databaseConnection);
        this.overtimeDAO = new OvertimeRequestDAO(databaseConnection);
        this.payPeriodDAO = new PayPeriodDAO();
        
        // Initialize services
        this.attendanceService = new AttendanceService(databaseConnection);
        this.payrollService = new PayrollService(databaseConnection);
        this.leaveService = new LeaveService(databaseConnection);
        this.overtimeService = new OvertimeService(databaseConnection);
    }
    
    /**
     * Constructor with custom database connection
     */
    public ReportService(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
        this.employeeDAO = new EmployeeDAO(databaseConnection);
        this.payrollDAO = new PayrollDAO(databaseConnection);
        this.payslipDAO = new PayslipDAO(databaseConnection);
        this.attendanceDAO = new AttendanceDAO(databaseConnection);
        this.leaveDAO = new LeaveDAO(databaseConnection);
        this.overtimeDAO = new OvertimeRequestDAO(databaseConnection);
        this.payPeriodDAO = new PayPeriodDAO();
        
        this.attendanceService = new AttendanceService(databaseConnection);
        this.payrollService = new PayrollService(databaseConnection);
        this.leaveService = new LeaveService(databaseConnection);
        this.overtimeService = new OvertimeService(databaseConnection);
    }
    
    // ================================
    // DATABASE VIEW-BASED PAYROLL REPORTS
    // ================================
    
    /**
     * Generate monthly payroll summary using monthly_payroll_summary_report view
     * This leverages all the complex business logic in the database view
     */
    public MonthlyPayrollSummaryReport generateMonthlyPayrollSummaryFromView(YearMonth yearMonth) {
        MonthlyPayrollSummaryReport report = new MonthlyPayrollSummaryReport();
        report.setYearMonth(yearMonth);
        report.setGeneratedDate(LocalDate.now(MANILA_TIMEZONE));
        
        String sql = """
            SELECT * FROM monthly_payroll_summary_report 
            WHERE DATE_FORMAT(`Pay Date`, '%Y-%m') = ?
            ORDER BY `Employee ID`
            """;
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            String yearMonthStr = yearMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            stmt.setString(1, yearMonthStr);
            
            ResultSet rs = stmt.executeQuery();
            List<PayrollSummaryEntry> entries = new ArrayList<>();
            PayrollTotals totals = new PayrollTotals();
            
            while (rs.next()) {
                String employeeIdStr = rs.getString("Employee ID");
                
                // Skip TOTAL row - handle separately
                if ("TOTAL".equals(employeeIdStr)) {
                    totals.setTotalBaseSalary(rs.getBigDecimal("Base Salary"));
                    totals.setTotalLeaves(rs.getBigDecimal("Leaves"));
                    totals.setTotalOvertime(rs.getBigDecimal("Overtime"));
                    totals.setTotalGrossIncome(rs.getBigDecimal("GROSS INCOME"));
                    totals.setTotalBenefits(rs.getBigDecimal("TOTAL BENEFITS"));
                    totals.setTotalDeductions(rs.getBigDecimal("TOTAL DEDUCTIONS"));
                    totals.setTotalNetPay(rs.getBigDecimal("NET PAY"));
                    continue;
                }
                
                PayrollSummaryEntry entry = new PayrollSummaryEntry();
                entry.setEmployeeId(Integer.parseInt(employeeIdStr));
                entry.setEmployeeName(rs.getString("Employee Name"));
                entry.setPosition(rs.getString("Position"));
                entry.setDepartment(rs.getString("Department"));
                entry.setBaseSalary(rs.getBigDecimal("Base Salary"));
                entry.setLeaves(rs.getBigDecimal("Leaves"));
                entry.setOvertime(rs.getBigDecimal("Overtime"));
                entry.setGrossIncome(rs.getBigDecimal("GROSS INCOME"));
                entry.setRiceSubsidy(rs.getBigDecimal("Rice Subsidy"));
                entry.setPhoneAllowance(rs.getBigDecimal("Phone Allowance"));
                entry.setClothingAllowance(rs.getBigDecimal("Clothing Allowance"));
                entry.setTotalBenefits(rs.getBigDecimal("TOTAL BENEFITS"));
                entry.setSssNo(rs.getString("Social Security No"));
                entry.setSssContribution(rs.getBigDecimal("Social Security Contribution"));
                entry.setPhilhealthNo(rs.getString("Philhealth No"));
                entry.setPhilhealthContribution(rs.getBigDecimal("Philhealth Contribution"));
                entry.setPagibigNo(rs.getString("Pag-Ibig No"));
                entry.setPagibigContribution(rs.getBigDecimal("Pag-Ibig Contribution"));
                entry.setTin(rs.getString("TIN"));
                entry.setWithholdingTax(rs.getBigDecimal("Withholding Tax"));
                entry.setTotalDeductions(rs.getBigDecimal("TOTAL DEDUCTIONS"));
                entry.setNetPay(rs.getBigDecimal("NET PAY"));
                
                // Determine if rank-and-file
                entry.setRankAndFile(employeeDAO.isEmployeeRankAndFile(entry.getEmployeeId()));
                
                entries.add(entry);
            }
            
            report.setPayrollEntries(entries);
            report.setTotals(totals);
            report.setTotalEmployees(entries.size());
            report.setSuccess(true);
            
            System.out.println("Generated monthly payroll summary from database view: " + 
                             entries.size() + " employees processed");
            
        } catch (SQLException e) {
            report.setSuccess(false);
            report.setErrorMessage("Error generating payroll summary from view: " + e.getMessage());
            System.err.println("Error generating payroll summary: " + e.getMessage());
        }
        
        return report;
    }
    
    /**
     * Generate employee payslip using monthly_employee_payslip view
     * This gives complete payslip details with all calculations done by the database
     */
    public EmployeePayslipReport generateEmployeePayslipFromView(Integer employeeId, YearMonth yearMonth) {
        EmployeePayslipReport report = new EmployeePayslipReport();
        report.setEmployeeId(employeeId);
        report.setYearMonth(yearMonth);
        report.setGeneratedDate(LocalDate.now(MANILA_TIMEZONE));
        
        String sql = """
            SELECT * FROM monthly_employee_payslip 
            WHERE `Employee ID` = ?
            AND DATE_FORMAT(`Period Start Date`, '%Y-%m') = ?
            """;
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, employeeId);
            String yearMonthStr = yearMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            stmt.setString(2, yearMonthStr);
            
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                PayslipDetails payslip = new PayslipDetails();
                
                // Basic Information
                payslip.setPayslipNo(rs.getString("Payslip No"));
                payslip.setEmployeeId(rs.getInt("Employee ID"));
                payslip.setEmployeeName(rs.getString("Employee Name"));
                payslip.setPeriodStartDate(rs.getDate("Period Start Date").toLocalDate());
                payslip.setPeriodEndDate(rs.getDate("Period End Date").toLocalDate());
                payslip.setPayDate(rs.getDate("Pay Date").toLocalDate());
                payslip.setEmployeePosition(rs.getString("Employee Position"));
                payslip.setDepartment(rs.getString("Department"));
                
                // Government IDs
                payslip.setTin(rs.getString("TIN"));
                payslip.setSssNo(rs.getString("SSS No"));
                payslip.setPagibigNo(rs.getString("Pagibig No"));
                payslip.setPhilhealthNo(rs.getString("Philhealth No"));
                
                // Salary Information
                payslip.setMonthlyRate(rs.getBigDecimal("Monthly Rate"));
                payslip.setDailyRate(rs.getBigDecimal("Daily Rate"));
                payslip.setDaysWorked(rs.getBigDecimal("Days Worked"));
                payslip.setLeavesTaken(rs.getBigDecimal("Leaves Taken"));
                payslip.setOvertimeHours(rs.getBigDecimal("Overtime Hours"));
                payslip.setGrossIncome(rs.getBigDecimal("GROSS INCOME"));
                
                // Benefits
                payslip.setRiceSubsidy(rs.getBigDecimal("Rice Subsidy"));
                payslip.setPhoneAllowance(rs.getBigDecimal("Phone Allowance"));
                payslip.setClothingAllowance(rs.getBigDecimal("Clothing Allowance"));
                payslip.setTotalBenefits(rs.getBigDecimal("TOTAL BENEFITS"));
                
                // Deductions
                payslip.setSocialSecuritySystem(rs.getBigDecimal("Social Security System"));
                payslip.setPhilhealth(rs.getBigDecimal("Philhealth"));
                payslip.setPagIbig(rs.getBigDecimal("Pag-Ibig"));
                payslip.setWithholdingTax(rs.getBigDecimal("Withholding Tax"));
                payslip.setTotalDeductions(rs.getBigDecimal("TOTAL DEDUCTIONS"));
                
                // Summary
                payslip.setGrossIncomeSummary(rs.getBigDecimal("GROSS INCOME SUMMARY"));
                payslip.setTotalBenefitsSummary(rs.getBigDecimal("TOTAL BENEFITS SUMMARY"));
                payslip.setTotalDeductionsSummary(rs.getBigDecimal("TOTAL DEDUCTIONS SUMMARY"));
                payslip.setNetPay(rs.getBigDecimal("NET PAY"));
                
                // Determine if rank-and-file
                payslip.setRankAndFile(employeeDAO.isEmployeeRankAndFile(employeeId));
                
                report.setPayslip(payslip);
                report.setSuccess(true);
                
                System.out.println("Generated payslip from database view for employee: " + 
                                 payslip.getEmployeeName() + " (" + yearMonth + ")");
                
            } else {
                report.setSuccess(false);
                report.setErrorMessage("No payslip data found for employee " + employeeId + " in " + yearMonth);
            }
            
        } catch (SQLException e) {
            report.setSuccess(false);
            report.setErrorMessage("Error generating payslip from view: " + e.getMessage());
            System.err.println("Error generating payslip: " + e.getMessage());
        }
        
        return report;
    }
    
    /**
     * Generate rank-and-file overtime report using database views
     * Focuses on employees eligible for overtime (rank-and-file only)
     */
    public RankAndFileOvertimeReport generateRankAndFileOvertimeReport(YearMonth yearMonth) {
        RankAndFileOvertimeReport report = new RankAndFileOvertimeReport();
        report.setYearMonth(yearMonth);
        report.setGeneratedDate(LocalDate.now(MANILA_TIMEZONE));
        
        String sql = """
            SELECT * FROM monthly_employee_payslip 
            WHERE DATE_FORMAT(`Period Start Date`, '%Y-%m') = ?
            AND `Overtime Hours` > 0
            ORDER BY `Overtime Hours` DESC
            """;
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            String yearMonthStr = yearMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            stmt.setString(1, yearMonthStr);
            
            ResultSet rs = stmt.executeQuery();
            List<OvertimeEntry> overtimeEntries = new ArrayList<>();
            BigDecimal totalOvertimeHours = BigDecimal.ZERO;
            BigDecimal totalOvertimePay = BigDecimal.ZERO;
            
            while (rs.next()) {
                Integer empId = rs.getInt("Employee ID");
                
                // Only include rank-and-file employees (they're the only ones eligible for overtime)
                if (employeeDAO.isEmployeeRankAndFile(empId)) {
                    OvertimeEntry entry = new OvertimeEntry();
                    entry.setEmployeeId(empId);
                    entry.setEmployeeName(rs.getString("Employee Name"));
                    entry.setDepartment(rs.getString("Department"));
                    entry.setOvertimeHours(rs.getBigDecimal("Overtime Hours"));
                    entry.setDailyRate(rs.getBigDecimal("Daily Rate"));
                    
                    // Calculate overtime pay (1.25x rate for rank-and-file)
                    BigDecimal hourlyRate = rs.getBigDecimal("Daily Rate").divide(new BigDecimal("8"), 2, RoundingMode.HALF_UP);
                    BigDecimal overtimePay = entry.getOvertimeHours()
                                               .multiply(hourlyRate)
                                               .multiply(new BigDecimal("1.25"));
                    entry.setOvertimePay(overtimePay);
                    entry.setRankAndFile(true);
                    
                    overtimeEntries.add(entry);
                    totalOvertimeHours = totalOvertimeHours.add(entry.getOvertimeHours());
                    totalOvertimePay = totalOvertimePay.add(overtimePay);
                }
            }
            
            report.setOvertimeEntries(overtimeEntries);
            report.setTotalOvertimeHours(totalOvertimeHours);
            report.setTotalOvertimePay(totalOvertimePay);
            report.setTotalRankAndFileEmployeesWithOvertime(overtimeEntries.size());
            
            if (!overtimeEntries.isEmpty()) {
                BigDecimal avgHours = totalOvertimeHours.divide(new BigDecimal(overtimeEntries.size()), 2, RoundingMode.HALF_UP);
                report.setAverageOvertimeHoursPerEmployee(avgHours);
            }
            
            report.setSuccess(true);
            
            System.out.println("Generated rank-and-file overtime report: " + 
                             overtimeEntries.size() + " employees with overtime");
            
        } catch (SQLException e) {
            report.setSuccess(false);
            report.setErrorMessage("Error generating rank-and-file overtime report: " + e.getMessage());
            System.err.println("Error generating overtime report: " + e.getMessage());
        }
        
        return report;
    }
    
    /**
     * Generate government compliance report using database views
     * Pulls deduction data directly from the payroll views
     */
    public GovernmentComplianceReport generateGovernmentComplianceFromView(YearMonth yearMonth) {
        GovernmentComplianceReport report = new GovernmentComplianceReport();
        report.setYearMonth(yearMonth);
        report.setGeneratedDate(LocalDate.now(MANILA_TIMEZONE));
        
        String sql = """
            SELECT 
                SUM(`Social Security Contribution`) as total_sss,
                SUM(`Philhealth Contribution`) as total_philhealth,
                SUM(`Pag-Ibig Contribution`) as total_pagibig,
                SUM(`Withholding Tax`) as total_withholding_tax,
                COUNT(*) as total_employees
            FROM monthly_payroll_summary_report 
            WHERE DATE_FORMAT(`Pay Date`, '%Y-%m') = ?
            AND `Employee ID` != 'TOTAL'
            """;
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            String yearMonthStr = yearMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            stmt.setString(1, yearMonthStr);
            
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                report.setTotalSSS(rs.getBigDecimal("total_sss"));
                report.setTotalPhilHealth(rs.getBigDecimal("total_philhealth"));
                report.setTotalPagIbig(rs.getBigDecimal("total_pagibig"));
                report.setTotalWithholdingTax(rs.getBigDecimal("total_withholding_tax"));
                report.setTotalEmployees(rs.getInt("total_employees"));
                report.setSuccess(true);
                
                System.out.println("Generated government compliance report from database view for " + yearMonth);
            } else {
                report.setSuccess(false);
                report.setErrorMessage("No payroll data found for " + yearMonth);
            }
            
        } catch (SQLException e) {
            report.setSuccess(false);
            report.setErrorMessage("Error generating compliance report: " + e.getMessage());
            System.err.println("Error generating compliance report: " + e.getMessage());
        }
        
        return report;
    }
    
    // ================================
    // ATTENDANCE REPORTS (Existing - Enhanced)
    // ================================
    
    /**
     * Generates daily attendance report
     */
    public AttendanceReport generateDailyAttendanceReport(LocalDate date) {
        AttendanceReport report = new AttendanceReport();
        report.setReportDate(date);
        report.setGeneratedDate(LocalDate.now(MANILA_TIMEZONE));
        report.setReportType("Daily Attendance");
        
        try {
            List<AttendanceService.DailyAttendanceRecord> attendanceRecords = 
                    attendanceService.getDailyAttendanceReport(date);
            
            report.setAttendanceRecords(attendanceRecords);
            
            // Calculate statistics
            long presentCount = attendanceRecords.stream().filter(r -> "Present".equals(r.getStatus())).count();
            long lateCount = attendanceRecords.stream().filter(r -> "Late".equals(r.getStatus())).count();
            long absentCount = attendanceRecords.stream().filter(r -> "Absent".equals(r.getStatus())).count();
            
            report.setPresentCount((int)presentCount);
            report.setLateCount((int)lateCount);
            report.setAbsentCount((int)absentCount);
            report.setTotalEmployees(attendanceRecords.size());
            
            if (attendanceRecords.size() > 0) {
                BigDecimal attendanceRate = new BigDecimal(presentCount + lateCount)
                        .divide(new BigDecimal(attendanceRecords.size()), 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal(100));
                report.setAttendanceRate(attendanceRate);
            }
            
            report.setSuccess(true);
            
        } catch (Exception e) {
            report.setSuccess(false);
            report.setErrorMessage("Error generating attendance report: " + e.getMessage());
        }
        
        return report;
    }
    
    // ================================
    // HELPER METHODS
    // ================================
    
    /**
     * Get current Manila time for timestamping reports
     */
    public LocalDate getCurrentManilaDate() {
        return LocalDate.now(MANILA_TIMEZONE);
    }
    
    /**
     * Format currency for display
     */
    public String formatCurrency(BigDecimal amount) {
        return "₱" + String.format("%,.2f", amount);
    }
    
    /**
     * Format date for display
     */
    public String formatDate(LocalDate date) {
        return date.format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"));
    }
    
    /**
     * Format percentage for display
     */
    public String formatPercentage(BigDecimal percentage) {
        return String.format("%.2f%%", percentage);
    }
    
    // ================================
    // NEW REPORT MODELS FOR DATABASE VIEWS
    // ================================
    
    /**
     * Monthly Payroll Summary Report (from monthly_payroll_summary_report view)
     */
    public static class MonthlyPayrollSummaryReport {
        private boolean success = false;
        private String errorMessage = "";
        private YearMonth yearMonth;
        private LocalDate generatedDate;
        private int totalEmployees = 0;
        private List<PayrollSummaryEntry> payrollEntries = new ArrayList<>();
        private PayrollTotals totals = new PayrollTotals();
        
        // Getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public YearMonth getYearMonth() { return yearMonth; }
        public void setYearMonth(YearMonth yearMonth) { this.yearMonth = yearMonth; }
        public LocalDate getGeneratedDate() { return generatedDate; }
        public void setGeneratedDate(LocalDate generatedDate) { this.generatedDate = generatedDate; }
        public int getTotalEmployees() { return totalEmployees; }
        public void setTotalEmployees(int totalEmployees) { this.totalEmployees = totalEmployees; }
        public List<PayrollSummaryEntry> getPayrollEntries() { return payrollEntries; }
        public void setPayrollEntries(List<PayrollSummaryEntry> payrollEntries) { this.payrollEntries = payrollEntries; }
        public PayrollTotals getTotals() { return totals; }
        public void setTotals(PayrollTotals totals) { this.totals = totals; }
    }
    
    /**
     * Individual entry from monthly_payroll_summary_report view
     */
    public static class PayrollSummaryEntry {
        private Integer employeeId;
        private String employeeName;
        private String position;
        private String department;
        private boolean rankAndFile = false;
        private BigDecimal baseSalary = BigDecimal.ZERO;
        private BigDecimal leaves = BigDecimal.ZERO;
        private BigDecimal overtime = BigDecimal.ZERO;
        private BigDecimal grossIncome = BigDecimal.ZERO;
        private BigDecimal riceSubsidy = BigDecimal.ZERO;
        private BigDecimal phoneAllowance = BigDecimal.ZERO;
        private BigDecimal clothingAllowance = BigDecimal.ZERO;
        private BigDecimal totalBenefits = BigDecimal.ZERO;
        private String sssNo;
        private BigDecimal sssContribution = BigDecimal.ZERO;
        private String philhealthNo;
        private BigDecimal philhealthContribution = BigDecimal.ZERO;
        private String pagibigNo;
        private BigDecimal pagibigContribution = BigDecimal.ZERO;
        private String tin;
        private BigDecimal withholdingTax = BigDecimal.ZERO;
        private BigDecimal totalDeductions = BigDecimal.ZERO;
        private BigDecimal netPay = BigDecimal.ZERO;
        
        // Getters and setters
        public Integer getEmployeeId() { return employeeId; }
        public void setEmployeeId(Integer employeeId) { this.employeeId = employeeId; }
        public String getEmployeeName() { return employeeName; }
        public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
        public String getPosition() { return position; }
        public void setPosition(String position) { this.position = position; }
        public String getDepartment() { return department; }
        public void setDepartment(String department) { this.department = department; }
        public boolean isRankAndFile() { return rankAndFile; }
        public void setRankAndFile(boolean rankAndFile) { this.rankAndFile = rankAndFile; }
        public BigDecimal getBaseSalary() { return baseSalary; }
        public void setBaseSalary(BigDecimal baseSalary) { this.baseSalary = baseSalary; }
        public BigDecimal getLeaves() { return leaves; }
        public void setLeaves(BigDecimal leaves) { this.leaves = leaves; }
        public BigDecimal getOvertime() { return overtime; }
        public void setOvertime(BigDecimal overtime) { this.overtime = overtime; }
        public BigDecimal getGrossIncome() { return grossIncome; }
        public void setGrossIncome(BigDecimal grossIncome) { this.grossIncome = grossIncome; }
        public BigDecimal getRiceSubsidy() { return riceSubsidy; }
        public void setRiceSubsidy(BigDecimal riceSubsidy) { this.riceSubsidy = riceSubsidy; }
        public BigDecimal getPhoneAllowance() { return phoneAllowance; }
        public void setPhoneAllowance(BigDecimal phoneAllowance) { this.phoneAllowance = phoneAllowance; }
        public BigDecimal getClothingAllowance() { return clothingAllowance; }
        public void setClothingAllowance(BigDecimal clothingAllowance) { this.clothingAllowance = clothingAllowance; }
        public BigDecimal getTotalBenefits() { return totalBenefits; }
        public void setTotalBenefits(BigDecimal totalBenefits) { this.totalBenefits = totalBenefits; }
        public String getSssNo() { return sssNo; }
        public void setSssNo(String sssNo) { this.sssNo = sssNo; }
        public BigDecimal getSssContribution() { return sssContribution; }
        public void setSssContribution(BigDecimal sssContribution) { this.sssContribution = sssContribution; }
        public String getPhilhealthNo() { return philhealthNo; }
        public void setPhilhealthNo(String philhealthNo) { this.philhealthNo = philhealthNo; }
        public BigDecimal getPhilhealthContribution() { return philhealthContribution; }
        public void setPhilhealthContribution(BigDecimal philhealthContribution) { this.philhealthContribution = philhealthContribution; }
        public String getPagibigNo() { return pagibigNo; }
        public void setPagibigNo(String pagibigNo) { this.pagibigNo = pagibigNo; }
        public BigDecimal getPagibigContribution() { return pagibigContribution; }
        public void setPagibigContribution(BigDecimal pagibigContribution) { this.pagibigContribution = pagibigContribution; }
        public String getTin() { return tin; }
        public void setTin(String tin) { this.tin = tin; }
        public BigDecimal getWithholdingTax() { return withholdingTax; }
        public void setWithholdingTax(BigDecimal withholdingTax) { this.withholdingTax = withholdingTax; }
        public BigDecimal getTotalDeductions() { return totalDeductions; }
        public void setTotalDeductions(BigDecimal totalDeductions) { this.totalDeductions = totalDeductions; }
        public BigDecimal getNetPay() { return netPay; }
        public void setNetPay(BigDecimal netPay) { this.netPay = netPay; }
        
        public String getEmployeeCategory() {
            return rankAndFile ? "Rank-and-File" : "Non Rank-and-File";
        }
    }
    
    /**
     * Payroll totals from the TOTAL row in monthly_payroll_summary_report
     */
    public static class PayrollTotals {
        private BigDecimal totalBaseSalary = BigDecimal.ZERO;
        private BigDecimal totalLeaves = BigDecimal.ZERO;
        private BigDecimal totalOvertime = BigDecimal.ZERO;
        private BigDecimal totalGrossIncome = BigDecimal.ZERO;
        private BigDecimal totalBenefits = BigDecimal.ZERO;
        private BigDecimal totalDeductions = BigDecimal.ZERO;
        private BigDecimal totalNetPay = BigDecimal.ZERO;
        
        // Getters and setters
        public BigDecimal getTotalBaseSalary() { return totalBaseSalary; }
        public void setTotalBaseSalary(BigDecimal totalBaseSalary) { this.totalBaseSalary = totalBaseSalary; }
        public BigDecimal getTotalLeaves() { return totalLeaves; }
        public void setTotalLeaves(BigDecimal totalLeaves) { this.totalLeaves = totalLeaves; }
        public BigDecimal getTotalOvertime() { return totalOvertime; }
        public void setTotalOvertime(BigDecimal totalOvertime) { this.totalOvertime = totalOvertime; }
        public BigDecimal getTotalGrossIncome() { return totalGrossIncome; }
        public void setTotalGrossIncome(BigDecimal totalGrossIncome) { this.totalGrossIncome = totalGrossIncome; }
        public BigDecimal getTotalBenefits() { return totalBenefits; }
        public void setTotalBenefits(BigDecimal totalBenefits) { this.totalBenefits = totalBenefits; }
        public BigDecimal getTotalDeductions() { return totalDeductions; }
        public void setTotalDeductions(BigDecimal totalDeductions) { this.totalDeductions = totalDeductions; }
        public BigDecimal getTotalNetPay() { return totalNetPay; }
        public void setTotalNetPay(BigDecimal totalNetPay) { this.totalNetPay = totalNetPay; }
    }
    
    /**
     * Employee Payslip Report (from monthly_employee_payslip view)
     */
    public static class EmployeePayslipReport {
        private boolean success = false;
        private String errorMessage = "";
        private Integer employeeId;
        private YearMonth yearMonth;
        private LocalDate generatedDate;
        private PayslipDetails payslip;
        
        // Getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public Integer getEmployeeId() { return employeeId; }
        public void setEmployeeId(Integer employeeId) { this.employeeId = employeeId; }
        public YearMonth getYearMonth() { return yearMonth; }
        public void setYearMonth(YearMonth yearMonth) { this.yearMonth = yearMonth; }
        public LocalDate getGeneratedDate() { return generatedDate; }
        public void setGeneratedDate(LocalDate generatedDate) { this.generatedDate = generatedDate; }
        public PayslipDetails getPayslip() { return payslip; }
        public void setPayslip(PayslipDetails payslip) { this.payslip = payslip; }
    }
    
    /**
     * Complete payslip details from monthly_employee_payslip view
     */
    public static class PayslipDetails {
        private String payslipNo;
        private Integer employeeId;
        private String employeeName;
        private LocalDate periodStartDate;
        private LocalDate periodEndDate;
        private LocalDate payDate;
        private String employeePosition;
        private String department;
        private boolean rankAndFile = false;
        
        // Government IDs
        private String tin;
        private String sssNo;
        private String pagibigNo;
        private String philhealthNo;
        
        // Salary Information
        private BigDecimal monthlyRate = BigDecimal.ZERO;
        private BigDecimal dailyRate = BigDecimal.ZERO;
        private BigDecimal daysWorked = BigDecimal.ZERO;
        private BigDecimal leavesTaken = BigDecimal.ZERO;
        private BigDecimal overtimeHours = BigDecimal.ZERO;
        private BigDecimal grossIncome = BigDecimal.ZERO;
        
        // Benefits
        private BigDecimal riceSubsidy = BigDecimal.ZERO;
        private BigDecimal phoneAllowance = BigDecimal.ZERO;
        private BigDecimal clothingAllowance = BigDecimal.ZERO;
        private BigDecimal totalBenefits = BigDecimal.ZERO;
        
        // Deductions
        private BigDecimal socialSecuritySystem = BigDecimal.ZERO;
        private BigDecimal philhealth = BigDecimal.ZERO;
        private BigDecimal pagIbig = BigDecimal.ZERO;
        private BigDecimal withholdingTax = BigDecimal.ZERO;
        private BigDecimal totalDeductions = BigDecimal.ZERO;
        
        // Summary
        private BigDecimal grossIncomeSummary = BigDecimal.ZERO;
        private BigDecimal totalBenefitsSummary = BigDecimal.ZERO;
        private BigDecimal totalDeductionsSummary = BigDecimal.ZERO;
        private BigDecimal netPay = BigDecimal.ZERO;
        
        // Getters and setters
        public String getPayslipNo() { return payslipNo; }
        public void setPayslipNo(String payslipNo) { this.payslipNo = payslipNo; }
        public Integer getEmployeeId() { return employeeId; }
        public void setEmployeeId(Integer employeeId) { this.employeeId = employeeId; }
        public String getEmployeeName() { return employeeName; }
        public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
        public LocalDate getPeriodStartDate() { return periodStartDate; }
        public void setPeriodStartDate(LocalDate periodStartDate) { this.periodStartDate = periodStartDate; }
        public LocalDate getPeriodEndDate() { return periodEndDate; }
        public void setPeriodEndDate(LocalDate periodEndDate) { this.periodEndDate = periodEndDate; }
        public LocalDate getPayDate() { return payDate; }
        public void setPayDate(LocalDate payDate) { this.payDate = payDate; }
        public String getEmployeePosition() { return employeePosition; }
        public void setEmployeePosition(String employeePosition) { this.employeePosition = employeePosition; }
        public String getDepartment() { return department; }
        public void setDepartment(String department) { this.department = department; }
        public boolean isRankAndFile() { return rankAndFile; }
        public void setRankAndFile(boolean rankAndFile) { this.rankAndFile = rankAndFile; }
        public String getTin() { return tin; }
        public void setTin(String tin) { this.tin = tin; }
        public String getSssNo() { return sssNo; }
        public void setSssNo(String sssNo) { this.sssNo = sssNo; }
        public String getPagibigNo() { return pagibigNo; }
        public void setPagibigNo(String pagibigNo) { this.pagibigNo = pagibigNo; }
        public String getPhilhealthNo() { return philhealthNo; }
        public void setPhilhealthNo(String philhealthNo) { this.philhealthNo = philhealthNo; }
        public BigDecimal getMonthlyRate() { return monthlyRate; }
        public void setMonthlyRate(BigDecimal monthlyRate) { this.monthlyRate = monthlyRate; }
        public BigDecimal getDailyRate() { return dailyRate; }
        public void setDailyRate(BigDecimal dailyRate) { this.dailyRate = dailyRate; }
        public BigDecimal getDaysWorked() { return daysWorked; }
        public void setDaysWorked(BigDecimal daysWorked) { this.daysWorked = daysWorked; }
        public BigDecimal getLeavesTaken() { return leavesTaken; }
        public void setLeavesTaken(BigDecimal leavesTaken) { this.leavesTaken = leavesTaken; }
        public BigDecimal getOvertimeHours() { return overtimeHours; }
        public void setOvertimeHours(BigDecimal overtimeHours) { this.overtimeHours = overtimeHours; }
        public BigDecimal getGrossIncome() { return grossIncome; }
        public void setGrossIncome(BigDecimal grossIncome) { this.grossIncome = grossIncome; }
        public BigDecimal getRiceSubsidy() { return riceSubsidy; }
        public void setRiceSubsidy(BigDecimal riceSubsidy) { this.riceSubsidy = riceSubsidy; }
        public BigDecimal getPhoneAllowance() { return phoneAllowance; }
        public void setPhoneAllowance(BigDecimal phoneAllowance) { this.phoneAllowance = phoneAllowance; }
        public BigDecimal getClothingAllowance() { return clothingAllowance; }
        public void setClothingAllowance(BigDecimal clothingAllowance) { this.clothingAllowance = clothingAllowance; }
        public BigDecimal getTotalBenefits() { return totalBenefits; }
        public void setTotalBenefits(BigDecimal totalBenefits) { this.totalBenefits = totalBenefits; }
        public BigDecimal getSocialSecuritySystem() { return socialSecuritySystem; }
        public void setSocialSecuritySystem(BigDecimal socialSecuritySystem) { this.socialSecuritySystem = socialSecuritySystem; }
        public BigDecimal getPhilhealth() { return philhealth; }
        public void setPhilhealth(BigDecimal philhealth) { this.philhealth = philhealth; }
        public BigDecimal getPagIbig() { return pagIbig; }
        public void setPagIbig(BigDecimal pagIbig) { this.pagIbig = pagIbig; }
        public BigDecimal getWithholdingTax() { return withholdingTax; }
        public void setWithholdingTax(BigDecimal withholdingTax) { this.withholdingTax = withholdingTax; }
        public BigDecimal getTotalDeductions() { return totalDeductions; }
        public void setTotalDeductions(BigDecimal totalDeductions) { this.totalDeductions = totalDeductions; }
        public BigDecimal getGrossIncomeSummary() { return grossIncomeSummary; }
        public void setGrossIncomeSummary(BigDecimal grossIncomeSummary) { this.grossIncomeSummary = grossIncomeSummary; }
        public BigDecimal getTotalBenefitsSummary() { return totalBenefitsSummary; }
        public void setTotalBenefitsSummary(BigDecimal totalBenefitsSummary) { this.totalBenefitsSummary = totalBenefitsSummary; }
        public BigDecimal getTotalDeductionsSummary() { return totalDeductionsSummary; }
        public void setTotalDeductionsSummary(BigDecimal totalDeductionsSummary) { this.totalDeductionsSummary = totalDeductionsSummary; }
        public BigDecimal getNetPay() { return netPay; }
        public void setNetPay(BigDecimal netPay) { this.netPay = netPay; }
        
        public String getEmployeeCategory() {
            return rankAndFile ? "Rank-and-File (Overtime Eligible)" : "Non Rank-and-File";
        }
        
        public String getOvertimeInfo() {
            if (rankAndFile && overtimeHours.compareTo(BigDecimal.ZERO) > 0) {
                return String.format("%.2f hours at 1.25x rate", overtimeHours);
            } else if (rankAndFile) {
                return "No overtime (eligible for 1.25x rate)";
            } else {
                return "Not eligible for overtime";
            }
        }
    }
    
    /**
     * Rank-and-File Overtime Report
     */
    public static class RankAndFileOvertimeReport {
        private boolean success = false;
        private String errorMessage = "";
        private YearMonth yearMonth;
        private LocalDate generatedDate;
        private BigDecimal totalOvertimeHours = BigDecimal.ZERO;
        private BigDecimal totalOvertimePay = BigDecimal.ZERO;
        private int totalRankAndFileEmployeesWithOvertime = 0;
        private BigDecimal averageOvertimeHoursPerEmployee = BigDecimal.ZERO;
        private List<OvertimeEntry> overtimeEntries = new ArrayList<>();
        
        // Getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public YearMonth getYearMonth() { return yearMonth; }
        public void setYearMonth(YearMonth yearMonth) { this.yearMonth = yearMonth; }
        public LocalDate getGeneratedDate() { return generatedDate; }
        public void setGeneratedDate(LocalDate generatedDate) { this.generatedDate = generatedDate; }
        public BigDecimal getTotalOvertimeHours() { return totalOvertimeHours; }
        public void setTotalOvertimeHours(BigDecimal totalOvertimeHours) { this.totalOvertimeHours = totalOvertimeHours; }
        public BigDecimal getTotalOvertimePay() { return totalOvertimePay; }
        public void setTotalOvertimePay(BigDecimal totalOvertimePay) { this.totalOvertimePay = totalOvertimePay; }
        public int getTotalRankAndFileEmployeesWithOvertime() { return totalRankAndFileEmployeesWithOvertime; }
        public void setTotalRankAndFileEmployeesWithOvertime(int totalRankAndFileEmployeesWithOvertime) { this.totalRankAndFileEmployeesWithOvertime = totalRankAndFileEmployeesWithOvertime; }
        public BigDecimal getAverageOvertimeHoursPerEmployee() { return averageOvertimeHoursPerEmployee; }
        public void setAverageOvertimeHoursPerEmployee(BigDecimal averageOvertimeHoursPerEmployee) { this.averageOvertimeHoursPerEmployee = averageOvertimeHoursPerEmployee; }
        public List<OvertimeEntry> getOvertimeEntries() { return overtimeEntries; }
        public void setOvertimeEntries(List<OvertimeEntry> overtimeEntries) { this.overtimeEntries = overtimeEntries; }
    }
    
    /**
     * Individual overtime entry for rank-and-file employees
     */
    public static class OvertimeEntry {
        private Integer employeeId;
        private String employeeName;
        private String department;
        private BigDecimal overtimeHours = BigDecimal.ZERO;
        private BigDecimal dailyRate = BigDecimal.ZERO;
        private BigDecimal overtimePay = BigDecimal.ZERO;
        private boolean rankAndFile = true;
        
        // Getters and setters
        public Integer getEmployeeId() { return employeeId; }
        public void setEmployeeId(Integer employeeId) { this.employeeId = employeeId; }
        public String getEmployeeName() { return employeeName; }
        public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
        public String getDepartment() { return department; }
        public void setDepartment(String department) { this.department = department; }
        public BigDecimal getOvertimeHours() { return overtimeHours; }
        public void setOvertimeHours(BigDecimal overtimeHours) { this.overtimeHours = overtimeHours; }
        public BigDecimal getDailyRate() { return dailyRate; }
        public void setDailyRate(BigDecimal dailyRate) { this.dailyRate = dailyRate; }
        public BigDecimal getOvertimePay() { return overtimePay; }
        public void setOvertimePay(BigDecimal overtimePay) { this.overtimePay = overtimePay; }
        public boolean isRankAndFile() { return rankAndFile; }
        public void setRankAndFile(boolean rankAndFile) { this.rankAndFile = rankAndFile; }
        
        public String getOvertimeMultiplier() {
            return rankAndFile ? "1.25x (Rank-and-File Rate)" : "N/A";
        }
    }
    
    /**
     * Government Compliance Report
     */
    public static class GovernmentComplianceReport {
        private boolean success = false;
        private String errorMessage = "";
        private YearMonth yearMonth;
        private LocalDate generatedDate;
        private int totalEmployees = 0;
        private BigDecimal totalSSS = BigDecimal.ZERO;
        private BigDecimal totalPhilHealth = BigDecimal.ZERO;
        private BigDecimal totalPagIbig = BigDecimal.ZERO;
        private BigDecimal totalWithholdingTax = BigDecimal.ZERO;
        
        // Getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public YearMonth getYearMonth() { return yearMonth; }
        public void setYearMonth(YearMonth yearMonth) { this.yearMonth = yearMonth; }
        public LocalDate getGeneratedDate() { return generatedDate; }
        public void setGeneratedDate(LocalDate generatedDate) { this.generatedDate = generatedDate; }
        public int getTotalEmployees() { return totalEmployees; }
        public void setTotalEmployees(int totalEmployees) { this.totalEmployees = totalEmployees; }
        public BigDecimal getTotalSSS() { return totalSSS; }
        public void setTotalSSS(BigDecimal totalSSS) { this.totalSSS = totalSSS; }
        public BigDecimal getTotalPhilHealth() { return totalPhilHealth; }
        public void setTotalPhilHealth(BigDecimal totalPhilHealth) { this.totalPhilHealth = totalPhilHealth; }
        public BigDecimal getTotalPagIbig() { return totalPagIbig; }
        public void setTotalPagIbig(BigDecimal totalPagIbig) { this.totalPagIbig = totalPagIbig; }
        public BigDecimal getTotalWithholdingTax() { return totalWithholdingTax; }
        public void setTotalWithholdingTax(BigDecimal totalWithholdingTax) { this.totalWithholdingTax = totalWithholdingTax; }
        
        public BigDecimal getTotalGovernmentContributions() {
            return totalSSS.add(totalPhilHealth).add(totalPagIbig).add(totalWithholdingTax);
        }
    }
    
    /**
     * Attendance report (keeping existing structure)
     */
    public static class AttendanceReport {
        private boolean success = false;
        private String errorMessage = "";
        private LocalDate reportDate;
        private LocalDate generatedDate;
        private String reportType;
        private int totalEmployees = 0;
        private int presentCount = 0;
        private int lateCount = 0;
        private int absentCount = 0;
        private BigDecimal attendanceRate = BigDecimal.ZERO;
        private List<AttendanceService.DailyAttendanceRecord> attendanceRecords = new ArrayList<>();
        
        // Getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public LocalDate getReportDate() { return reportDate; }
        public void setReportDate(LocalDate reportDate) { this.reportDate = reportDate; }
        public LocalDate getGeneratedDate() { return generatedDate; }
        public void setGeneratedDate(LocalDate generatedDate) { this.generatedDate = generatedDate; }
        public String getReportType() { return reportType; }
        public void setReportType(String reportType) { this.reportType = reportType; }
        public int getTotalEmployees() { return totalEmployees; }
        public void setTotalEmployees(int totalEmployees) { this.totalEmployees = totalEmployees; }
        public int getPresentCount() { return presentCount; }
        public void setPresentCount(int presentCount) { this.presentCount = presentCount; }
        public int getLateCount() { return lateCount; }
        public void setLateCount(int lateCount) { this.lateCount = lateCount; }
        public int getAbsentCount() { return absentCount; }
        public void setAbsentCount(int absentCount) { this.absentCount = absentCount; }
        public BigDecimal getAttendanceRate() { return attendanceRate; }
        public void setAttendanceRate(BigDecimal attendanceRate) { this.attendanceRate = attendanceRate; }
        public List<AttendanceService.DailyAttendanceRecord> getAttendanceRecords() { return attendanceRecords; }
        public void setAttendanceRecords(List<AttendanceService.DailyAttendanceRecord> attendanceRecords) { this.attendanceRecords = attendanceRecords; }
    }
}
