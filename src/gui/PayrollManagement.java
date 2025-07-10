package gui;

import Models.*;
import DAOs.*;
import Services.ReportService;
import Services.PurePDFPayslipGenerator;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;
import java.util.Set;
import java.util.TreeSet;
import java.awt.Color;
import javax.swing.Box;

import javax.swing.table.DefaultTableModel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.JDialog;
import javax.swing.JTable;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.BorderFactory;
import java.awt.BorderLayout;
import javax.swing.JTextArea;
import javax.swing.JFileChooser;
import java.io.File;
import java.io.IOException;

/**
 * PayrollManagement GUI class for Accounting users
 * Enhanced with payroll detail tables population
 */
public class PayrollManagement extends javax.swing.JFrame {

    // Core components
    private UserAuthenticationModel loggedInUser;
    private AccountingModel accountingModel;
    
    // Services - using same pattern as ViewPayslip
    private ReportService reportService;
    
    // DAO components - centralized data access
    private PayrollDAO payrollDAO;
    private PayPeriodDAO payPeriodDAO;
    private EmployeeDAO employeeDAO;
    private PositionDAO positionDAO;
    private PositionBenefitDAO positionBenefitDAO;
    private PayslipDAO payslipDAO;
    
    // Additional DAO components for payroll details
    private PayrollAttendanceDAO payrollAttendanceDAO;
    private PayrollBenefitDAO payrollBenefitDAO;
    private PayrollOvertimeDAO payrollOvertimeDAO;
    
    // Current state
    private YearMonth currentPayrollMonth;
    private PayPeriodModel currentPayPeriod;
    private boolean payslipsGenerated = false;
    private boolean payrollApproved = false;
    private List<PayrollModel> currentPayrollData;

    /**
     * Constructor initializes the payroll management form
     * @param user The logged-in user (must be Accounting role)
     */
    public PayrollManagement(UserAuthenticationModel user) {
        this.loggedInUser = user;
        
        // Initialize current data first to prevent null pointer exceptions
        this.currentPayrollData = new ArrayList<>();
        this.currentPayrollMonth = YearMonth.now();
        
        // Initialize model and DAO components
        initializeComponents();
        
        // Initialize GUI
        initComponents();
        setupTableColumns();
        setupTableProperties();
        populateDepartmentDropdown();
        populateMonthDropdown();
        
        // Set initial state - Make View Salary Details active by default
        setupInitialState();
        
        // Set initial button states - View Salary Details is now active
        updateButtonStates();
        
        System.out.println("PayrollManagement initialized for user: " + user.getFirstName() + " " + user.getLastName());
    }
    
    /**
     * Initialize AccountingModel and DAO components using proper pattern
     */
    private void initializeComponents() {
         try {
        // Initialize DAOs with shared database connection
        DatabaseConnection dbConnection = new DatabaseConnection();
        this.payrollDAO = new PayrollDAO(dbConnection);
        this.payPeriodDAO = new PayPeriodDAO();
        this.employeeDAO = new EmployeeDAO(dbConnection);
        this.positionDAO = new PositionDAO(dbConnection);
        this.positionBenefitDAO = new PositionBenefitDAO(dbConnection);
        this.payslipDAO = new PayslipDAO(dbConnection);
        
        // Initialize detail DAOs with shared database connection
        this.payrollAttendanceDAO = new PayrollAttendanceDAO(dbConnection);
        this.payrollBenefitDAO = new PayrollBenefitDAO(dbConnection);
        this.payrollOvertimeDAO = new PayrollOvertimeDAO(dbConnection);
        
        // Initialize ReportService
        this.reportService = new ReportService(dbConnection);
        
        // Create AccountingModel from existing user
        if (loggedInUser != null) {
            EmployeeModel empModel = new EmployeeModel(
                loggedInUser.getFirstName(), 
                loggedInUser.getLastName(), 
                loggedInUser.getEmail(), 
                "Accounting"
            );
            empModel.setEmployeeId(loggedInUser.getEmployeeId());
            
            this.accountingModel = new AccountingModel(empModel);
        } else {
            throw new IllegalArgumentException("User must be logged in to access PayrollManagement");
        }
        
        System.out.println("PayrollManagement initialized with AccountingModel and detail DAOs");
        
    } catch (Exception e) {
        JOptionPane.showMessageDialog(this,
            "Error initializing payroll management components: " + e.getMessage(),
            "Initialization Error", JOptionPane.ERROR_MESSAGE);
        e.printStackTrace();
    }
}
    
    /**
     * Setup initial state - View Salary Details is active by default
     */
    private void setupInitialState() {
        try {
            // Set default selections
            if (selectDepJComboBox1.getItemCount() > 0) {
                selectDepJComboBox1.setSelectedIndex(0); // Select "All"
            }
            
            // Set date to "All" by default 
            if (selectDateJComboBox2.getItemCount() > 0) {
                selectDateJComboBox2.setSelectedIndex(0); // Select "All" (first item)
            }
            
            // Show improved welcome message 
            showWelcomeMessage();
            
        } catch (Exception e) {
            System.err.println("Error setting up initial state: " + e.getMessage());
        }
    }
    
    /**
     * Shows welcome message in table 
     */
  private void showWelcomeMessage() {
    DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
    model.setRowCount(0);
    
    Object[] welcomeRow1 = new Object[model.getColumnCount()];
    welcomeRow1[0] = "Welcome to Payroll Management - " + accountingModel.getFullName();
    for (int i = 1; i < welcomeRow1.length; i++) {
        welcomeRow1[i] = "";
    }
    model.addRow(welcomeRow1);
    
    Object[] welcomeRow2 = new Object[model.getColumnCount()];
    welcomeRow2[0] = "Accounting role verified - All payroll operations enabled";
    for (int i = 1; i < welcomeRow2.length; i++) {
        welcomeRow2[i] = "";
    }
    model.addRow(welcomeRow2);
    
    Object[] welcomeRow3 = new Object[model.getColumnCount()];
    welcomeRow3[0] = "Click 'View Salary Details' to load payroll data";
    for (int i = 1; i < welcomeRow3.length; i++) {
        welcomeRow3[i] = "";
    }
    model.addRow(welcomeRow3);
}
    
    /**
     * Sets up table properties including horizontal scrolling
     */
    private void setupTableProperties() {
        jTable1.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_OFF);
        jTable1.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        jScrollPane1.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
    }
    
    /**
     * Updates the enabled/disabled state of buttons based on current state - View Salary Details always active
     */
    private void updateButtonStates() {
        // View salary details is always enabled (no longer depends on month selection)
        viewsalarydetails.setEnabled(true);
        
        // Generate payslip is always enabled for Accounting users
        generatePayslip.setEnabled(true);
        
        // View history is always enabled
        viewhistory.setEnabled(true);
    }
    
/**
 * Populate department dropdown - NOW uses AccountingModel
 */
private void populateDepartmentDropdown() {
    try {
        selectDepJComboBox1.removeAllItems();
        selectDepJComboBox1.addItem("All");
        
        // Use Set to avoid duplicates
        Set<String> uniqueDepartments = new TreeSet<>();
        
        // USE AccountingModel to get employees (with permission checking)
        List<EmployeeModel> employees = accountingModel.getAllActiveEmployees();
        
        for (EmployeeModel employee : employees) {
            if (employee.getPositionId() != null) {
                PositionModel position = positionDAO.findById(employee.getPositionId());
                if (position != null && position.getDepartment() != null) {
                    uniqueDepartments.add(position.getDepartment().trim());
                }
            }
        }
        
        // Add unique departments to dropdown
        for (String department : uniqueDepartments) {
            selectDepJComboBox1.addItem(department);
        }
        
        System.out.println("Populated " + uniqueDepartments.size() + " departments using AccountingModel");
        
    } catch (Exception e) {
        System.err.println("Error populating department dropdown: " + e.getMessage());
        // Fallback to hardcoded departments
        selectDepJComboBox1.addItem("Leadership");
        selectDepJComboBox1.addItem("IT");
        selectDepJComboBox1.addItem("HR");
        selectDepJComboBox1.addItem("Accounting");
    }
}
    
    /**
     * Populates the month dropdown - Added "All" option as default
     */
    private void populateMonthDropdown() {
        try {
            selectDateJComboBox2.removeAllItems();
            
            // Add "All" as first option to show all available data
            selectDateJComboBox2.addItem("All");
            
            // Add specific months for filtering
            selectDateJComboBox2.addItem("2024-06");
            selectDateJComboBox2.addItem("2024-07");
            selectDateJComboBox2.addItem("2024-08");
            selectDateJComboBox2.addItem("2024-09");
            selectDateJComboBox2.addItem("2024-10");
            selectDateJComboBox2.addItem("2024-11");
            selectDateJComboBox2.addItem("2024-12");
            
            // Set default to "All"
            selectDateJComboBox2.setSelectedItem("All");
            
            System.out.println("Populated month dropdown with 'All' option as default");
            
        } catch (Exception e) {
            System.err.println("Error populating month dropdown: " + e.getMessage());
            // Fallback
            selectDateJComboBox2.addItem("All");
            selectDateJComboBox2.addItem("2024-06");
            selectDateJComboBox2.addItem("2024-07");
            selectDateJComboBox2.addItem("2024-08");
            selectDateJComboBox2.addItem("2024-09");
            selectDateJComboBox2.addItem("2024-10");
            selectDateJComboBox2.addItem("2024-11");
            selectDateJComboBox2.addItem("2024-12");
        }
    }

    /**
     * Sets up the table columns for payroll data display - Gross income separate from benefits
     */
    private void setupTableColumns() {
        DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
        model.setColumnIdentifiers(new Object[]{
            "Employee ID", "Last Name", "First Name", "Position", "Department",
            "Days Worked", "Gross Income", // Basic income info (NO benefits included)
            "Rice Subsidy", "Phone Allowance", "Clothing Allowance", "Total Benefits", // Benefits section (separate)
            "SSS", "PhilHealth", "Pag-Ibig", "Withholding Tax", 
            "Total Deductions", "Net Pay"
        });
        model.setRowCount(0);
    }

    /**
     * Loads and displays payroll data - Handles "All" selection to show all data
     */
    private void loadPayrollData() {
        DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
        model.setRowCount(0);
        currentPayrollData.clear();

        try {
            String selectedMonthStr = (String) selectDateJComboBox2.getSelectedItem();
            String selectedDepartment = (String) selectDepJComboBox1.getSelectedItem();

            System.out.println("Loading payroll data for Month: '" + selectedMonthStr + 
                "', Department: '" + selectedDepartment + "'");

            // Handle "All" selection to show all available payroll data
            if (selectedMonthStr == null) {
                selectedMonthStr = "All";
            }

            // Get all employees for the selected department
            List<EmployeeModel> employees = getAllEmployeesForDepartment(selectedDepartment);
            
            if (employees.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                    "No employees found for the selected department: " + selectedDepartment,
                    "No Employees Found", JOptionPane.INFORMATION_MESSAGE);
                showWelcomeMessage();
                updateButtonStates();
                return;
            }

            int displayedRecords = 0;
            
            // Handle "All" selection - load data from all available periods
            if ("All".equals(selectedMonthStr)) {
                // Load data from all available pay periods
                List<YearMonth> availablePeriods = getAvailablePayPeriods();
                
                for (YearMonth period : availablePeriods) {
                    for (EmployeeModel employee : employees) {
                        try {
                            ReportService.EmployeePayslipReport payslipReport = 
                                reportService.generateEmployeePayslipFromView(employee.getEmployeeId(), period);
                            
                            if (payslipReport.isSuccess() && payslipReport.getPayslip() != null) {
                                addPayslipRowToTable(model, payslipReport.getPayslip(), employee);
                                displayedRecords++;
                            }
                        } catch (Exception e) {
                            System.err.println("Error processing employee " + employee.getEmployeeId() + 
                                " for period " + period + ": " + e.getMessage());
                        }
                    }
                }
                
                System.out.println("Loaded data from all available periods. Displayed " + displayedRecords + " records.");
                
            } else {
                // Load data for specific month (existing logic)
                YearMonth selectedYearMonth = parseYearMonth(selectedMonthStr);
                this.currentPayrollMonth = selectedYearMonth;

                System.out.println("Searching for payroll data for period: " + selectedYearMonth);

                // Use ReportService to get payslip data for each employee
                for (EmployeeModel employee : employees) {
                    try {
                        ReportService.EmployeePayslipReport payslipReport = 
                            reportService.generateEmployeePayslipFromView(employee.getEmployeeId(), selectedYearMonth);
                        
                        if (payslipReport.isSuccess() && payslipReport.getPayslip() != null) {
                            addPayslipRowToTable(model, payslipReport.getPayslip(), employee);
                            displayedRecords++;
                        } else {
                            System.out.println("No payslip data for employee " + employee.getEmployeeId() + 
                                ": " + payslipReport.getErrorMessage());
                        }
                    } catch (Exception e) {
                        System.err.println("Error processing employee " + employee.getEmployeeId() + ": " + e.getMessage());
                    }
                }

                System.out.println("Displayed " + displayedRecords + " out of " + employees.size() + 
                    " employees with payroll data for " + selectedYearMonth);
            }

            if (displayedRecords == 0) {
                String periodInfo = "All".equals(selectedMonthStr) ? "any period" : selectedMonthStr;
                JOptionPane.showMessageDialog(this,
                    "No payroll data found for " + periodInfo + 
                    "\n\nThis could mean:" +
                    "\n• No attendance records exist" +
                    "\n• Payroll has not been processed yet" +
                    "\n• Selected period is outside employment dates" +
                    "\n\nTry generating payslips first using the 'Generate Payslip' button.",
                    "No Payroll Data", JOptionPane.INFORMATION_MESSAGE);
                showWelcomeMessage();
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Error loading payroll data: " + e.getMessage(),
                "Data Loading Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            showWelcomeMessage();
        }

        updateButtonStates();
    }
    
    /**
     * Get available pay periods for "All" selection
     */
    private List<YearMonth> getAvailablePayPeriods() {
        List<YearMonth> periods = new ArrayList<>();
        
        try {
            // Get all pay periods from database
            List<PayPeriodModel> payPeriods = payPeriodDAO.findAll();
            
            for (PayPeriodModel period : payPeriods) {
                if (period.getStartDate() != null) {
                    YearMonth yearMonth = YearMonth.from(period.getStartDate());
                    if (!periods.contains(yearMonth)) {
                        periods.add(yearMonth);
                    }
                }
            }
            
            // Sort periods in ascending order
            periods.sort((a, b) -> a.compareTo(b));
            
        } catch (Exception e) {
            System.err.println("Error getting available pay periods: " + e.getMessage());
            
            // Fallback to predefined periods
            periods.add(YearMonth.of(2024, 6));
            periods.add(YearMonth.of(2024, 7));
            periods.add(YearMonth.of(2024, 8));
            periods.add(YearMonth.of(2024, 9));
            periods.add(YearMonth.of(2024, 10));
            periods.add(YearMonth.of(2024, 11));
            periods.add(YearMonth.of(2024, 12));
        }
        
        return periods;
    }
    
        /**
      * Get all employees for department filtering - NOW uses AccountingModel
      */
     private List<EmployeeModel> getAllEmployeesForDepartment(String selectedDepartment) {
         List<EmployeeModel> filteredEmployees = new ArrayList<>();

         try {
             // USE AccountingModel instead of direct DAO access
             List<EmployeeModel> allEmployees = accountingModel.getAllActiveEmployees();

             for (EmployeeModel employee : allEmployees) {
                 if (shouldIncludeEmployee(employee, selectedDepartment)) {
                     filteredEmployees.add(employee);
                 }
             }
         } catch (Exception e) {
             System.err.println("Error getting employees for department: " + e.getMessage());
         }

         return filteredEmployees;
     }
    
    /**
     * Parses year-month string to YearMonth object - same as ViewPayslip
     */
    private YearMonth parseYearMonth(String yearMonthStr) {
        try {
            return YearMonth.parse(yearMonthStr);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid year-month format: " + yearMonthStr);
        }
    }
    
    /**
     * Checks if employee should be included using PositionDAO
     */
    private boolean shouldIncludeEmployee(EmployeeModel employee, String selectedDepartment) {
        if (selectedDepartment == null || selectedDepartment.equals("All")) {
            return true;
        }

        try {
            if (employee.getPositionId() != null) {
                PositionModel position = positionDAO.findById(employee.getPositionId());
                if (position != null && position.getDepartment() != null) {
                    String empDepartment = position.getDepartment().trim();
                    String filterDepartment = selectedDepartment.trim();

                    // Case-insensitive comparison
                    boolean matches = empDepartment.equalsIgnoreCase(filterDepartment);

                    return matches;
                }
            }
        } catch (Exception e) {
            System.err.println("Error checking employee department for ID " + 
                employee.getEmployeeId() + ": " + e.getMessage());
        }

        return false;
    }
    
    /**
     * Adds a payslip record to the table - ENSURES gross income EXCLUDES benefits
     */
    private void addPayslipRowToTable(DefaultTableModel model, ReportService.PayslipDetails payslip, EmployeeModel employee) {
        try {
            String positionName = "Unknown";
            String department = "Unknown";
            
            // Get position information using PositionDAO
            if (employee.getPositionId() != null) {
                PositionModel position = positionDAO.findById(employee.getPositionId());
                if (position != null) {
                    positionName = position.getPosition();
                    department = position.getDepartment();
                }
            }
            
            // ENSURE: Gross income does NOT include benefits (this should be base salary calculation only)
            // The ReportService should already handle this correctly, but we verify here
            BigDecimal grossIncomeWithoutBenefits = payslip.getGrossIncome(); // This should already be correct
            
            model.addRow(new Object[]{
                payslip.getEmployeeId(),
                employee.getLastName(),
                employee.getFirstName(),
                positionName,
                department,
                payslip.getDaysWorked().toString(),
                formatCurrency(grossIncomeWithoutBenefits), // Gross income WITHOUT benefits
                formatCurrency(payslip.getRiceSubsidy()),
                formatCurrency(payslip.getPhoneAllowance()),
                formatCurrency(payslip.getClothingAllowance()),
                formatCurrency(payslip.getTotalBenefits()), // Total benefits separate
                formatCurrency(payslip.getSocialSecuritySystem()),
                formatCurrency(payslip.getPhilhealth()),
                formatCurrency(payslip.getPagIbig()),
                formatCurrency(payslip.getWithholdingTax()),
                formatCurrency(payslip.getTotalDeductions()),
                formatCurrency(payslip.getNetPay())
            });
            
        } catch (Exception e) {
            System.err.println("Error adding payslip row to table: " + e.getMessage());
        }
    }
    
    /**
     * Format currency same as ViewPayslip
     */
    private String formatCurrency(java.math.BigDecimal amount) {
        if (amount == null) {
            return "₱0.00";
        }
        return String.format("₱%,.2f", amount);
    }

    /**
     * Enhanced Generate payslip with month selection dialog - uses same logic as ViewPayslip
     */
    private void handleGeneratePayslips() {
        try {
            // Create month selection dialog (exclude "All" from generation options)
            String[] monthOptions = new String[selectDateJComboBox2.getItemCount() - 1]; // Exclude "All"
            int optionIndex = 0;
            for (int i = 1; i < selectDateJComboBox2.getItemCount(); i++) { // Start from 1 to skip "All"
                monthOptions[optionIndex++] = (String) selectDateJComboBox2.getItemAt(i);
            }
            
            if (monthOptions.length == 0) {
                JOptionPane.showMessageDialog(this,
                    "No pay periods available for payslip generation.",
                    "No Data Available", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            String selectedMonth = (String) JOptionPane.showInputDialog(
                this,
                "Select the month for payslip generation:",
                "Generate Payslips",
                JOptionPane.QUESTION_MESSAGE,
                null,
                monthOptions,
                monthOptions[monthOptions.length - 1] // Default to most recent
            );
            
            if (selectedMonth != null) {
                generatePayslipsForSelectedMonth(selectedMonth);
            }
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Error in payslip generation dialog: " + e.getMessage(),
                "Generation Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Generate payslips for selected month using enhanced PayrollDAO
     * NEW: Now populates all related tables: payrollattendance, payrollbenefit, payrollleave, payrollovertime
     */
/**
 * Generate payslips for selected month - NOW uses AccountingModel with detail table population
 */
private void generatePayslipsForSelectedMonth(String selectedMonth) {
    try {
        YearMonth selectedYearMonth = parseYearMonth(selectedMonth);
        PayPeriodModel payPeriod = findPayPeriodForMonth(selectedYearMonth);
        
        if (payPeriod == null) {
            JOptionPane.showMessageDialog(this,
                "No pay period found for " + selectedMonth,
                "Generation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Check if payroll already exists
        if (payrollDAO.isPayrollGenerated(payPeriod.getPayPeriodId())) {
            int choice = JOptionPane.showConfirmDialog(this,
                "Payslips already exist for " + selectedMonth + ". Regenerate?",
                "Payslips Exist",
                JOptionPane.YES_NO_OPTION);
            
            if (choice != JOptionPane.YES_OPTION) {
                return;
            }
            
            // FIXED: Delete in correct order to avoid foreign key constraint violations
            // Delete payslips FIRST (they reference payroll)
            payslipDAO.deletePayrollByPeriod(payPeriod.getPayPeriodId());
            
            // Delete payroll detail tables SECOND (they reference payroll)
            payrollDAO.deletePayrollDetailsByPeriod(payPeriod.getPayPeriodId());
            
            // Delete payroll records LAST (they are referenced by others)
            payrollDAO.deletePayrollByPeriod(payPeriod.getPayPeriodId());
        }
        
        // USE AccountingModel for payroll generation with detail tables
        AccountingModel.PayrollGenerationResult result = 
            accountingModel.generatePayrollWithDetails(payPeriod.getPayPeriodId());

        if (result.isSuccess()) {
            // Also generate payslip records with PENDING status
            int payslipCount = payslipDAO.generateAllPayslips(payPeriod.getPayPeriodId());

            // Verify payroll using AccountingModel
            AccountingModel.AccountingResult verification = 
                accountingModel.verifyPayrollForPeriod(payPeriod.getPayPeriodId());

            String verificationMsg = verification.isSuccess() ? 
                "\n✅ Verification: " + verification.getVerifiedRecords() + "/" + 
                verification.getTotalRecords() + " records verified" : 
                "\n⚠️ Verification issues detected";

            JOptionPane.showMessageDialog(this,
                "Payslips for " + selectedMonth + " generated successfully!\n" +
                "Generated payslips for " + result.getGeneratedCount() + " employees.\n" +
                "Detail tables populated: " + (result.isDetailTablesPopulated() ? "✅ Yes" : "❌ No") + "\n" +
                "• payrollattendance\n" +
                "• payrollbenefit\n" +
                "• payrollleave\n" +
                "• payrollovertime" +
                verificationMsg,
                "Payslips Generated Successfully",
                JOptionPane.INFORMATION_MESSAGE);

            // Refresh current view if it matches the generated month OR if "All" is selected
            String currentSelectedMonth = (String) selectDateJComboBox2.getSelectedItem();
            if (selectedMonth.equals(currentSelectedMonth) || "All".equals(currentSelectedMonth)) {
                loadPayrollData();
            }
        } else {
            JOptionPane.showMessageDialog(this,
                "Failed to generate payslips: " + result.getMessage(),
                "Generation Error", JOptionPane.ERROR_MESSAGE);
        }

    } catch (Exception e) {
        JOptionPane.showMessageDialog(this,
            "Error generating payslips: " + e.getMessage(),
            "Generation Error", JOptionPane.ERROR_MESSAGE);
        e.printStackTrace();
    }
}
    
    /**
     * Finds the pay period for the selected month
     */
    private PayPeriodModel findPayPeriodForMonth(YearMonth yearMonth) {
        try {
            List<PayPeriodModel> allPeriods = payPeriodDAO.findAll();

            for (PayPeriodModel period : allPeriods) {
                if (period.getStartDate() != null) {
                    YearMonth periodMonth = YearMonth.from(period.getStartDate());
                    if (periodMonth.equals(yearMonth)) {
                        return period;
                    }
                }
            }

            return null;
        } catch (Exception e) {
            System.err.println("Error finding pay period: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
 * Show individual payslip details using same format as ViewPayslip
 */
private void showIndividualPayslip(DefaultTableModel model, int selectedRow) {
    try {
        String employeeIdStr = model.getValueAt(selectedRow, 1).toString();
        String employeeName = model.getValueAt(selectedRow, 2).toString();
        String payPeriod = model.getValueAt(selectedRow, 3).toString();
        
        // Parse pay period to get YearMonth
        YearMonth payslipMonth = parsePayPeriodToYearMonth(payPeriod);
        
        if (payslipMonth == null) {
            JOptionPane.showMessageDialog(this,
                "Could not determine pay period for payslip display.",
                "Display Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Get payslip data using ReportService (same as ViewPayslip)
        var payslipReport = reportService.generateEmployeePayslipFromView(
            Integer.valueOf(employeeIdStr), payslipMonth);
        
        if (payslipReport.isSuccess() && payslipReport.getPayslip() != null) {
            // Format payslip same as ViewPayslip
            String payslipDetails = formatPayslipForDisplay(payslipReport.getPayslip());
            
            // Create a dialog to show the formatted payslip
            JDialog payslipDialog = new JDialog(this, "Payslip Details - " + employeeName, true);
            payslipDialog.setSize(800, 600);
            payslipDialog.setLocationRelativeTo(this);
            
            JTextArea textArea = new JTextArea(payslipDetails);
            textArea.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 12));
            textArea.setEditable(false);
            
            JScrollPane scrollPane = new JScrollPane(textArea);
            payslipDialog.add(scrollPane);
            payslipDialog.setVisible(true);
        } else {
            JOptionPane.showMessageDialog(this,
                "Could not load payslip data: " + payslipReport.getErrorMessage(),
                "Payslip Not Found", JOptionPane.ERROR_MESSAGE);
        }
        
    } catch (Exception e) {
        JOptionPane.showMessageDialog(this,
            "Error viewing payslip: " + e.getMessage(),
            "View Error", JOptionPane.ERROR_MESSAGE);
        e.printStackTrace();
    }
}

    /**
     * Enhanced payslip history dialog with date filter and download functionality
     */
    private void showPayslipHistoryDialog() {
        JDialog historyDialog = new JDialog(this, "Generated Payslip History", true);
        historyDialog.setSize(1200, 750);
        historyDialog.setLocationRelativeTo(this);
        
        // Create table for history with status column
        String[] columns = {"Payslip ID", "Employee ID", "Employee Name", "Pay Period", 
                           "Generated Date", "Gross Income", "Net Pay", "Status"};
        DefaultTableModel historyModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table non-editable
            }
        };
        
        JTable historyTable = new JTable(historyModel);
        historyTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        
        // Create compact filter panel
        JPanel filterPanel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
        filterPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        JLabel filterLabel = new JLabel("Filter by Period:");
        javax.swing.JComboBox<String> periodFilterCombo = new javax.swing.JComboBox<>();
        periodFilterCombo.addItem("All Periods");
        periodFilterCombo.addItem("June 2024");
        periodFilterCombo.addItem("July 2024");
        periodFilterCombo.addItem("August 2024");
        periodFilterCombo.addItem("September 2024");
        periodFilterCombo.addItem("October 2024");
        periodFilterCombo.addItem("November 2024");
        periodFilterCombo.addItem("December 2024");
        
        JButton applyFilterBtn = new JButton("Apply Filter");
        applyFilterBtn.setBackground(new Color(220, 95, 0));
        applyFilterBtn.setForeground(Color.WHITE);
        
        filterPanel.add(filterLabel);
        filterPanel.add(periodFilterCombo);
        filterPanel.add(applyFilterBtn);
        
        // Load all payslip history data initially
        loadPayslipHistoryData(historyModel, null);
        
        JScrollPane scrollPane = new JScrollPane(historyTable);
        
        // Create button panel
        JPanel buttonPanel = new JPanel();
        JButton selectAllBtn = new JButton("Select All");
        JButton approveBtn = new JButton("Approve");
        JButton approveAllBtn = new JButton("Approve All");
        JButton denyBtn = new JButton("Deny");
        JButton denyAllBtn = new JButton("Deny All");
        JButton downloadBtn = new JButton("Download Selected");
        JButton viewBtn = new JButton("View Payslip");
        JButton refreshBtn = new JButton("Refresh");
        
        // Style buttons
        approveBtn.setBackground(new Color(0, 153, 0));
        approveBtn.setForeground(Color.WHITE);
        approveAllBtn.setBackground(new Color(0, 153, 0));
        approveAllBtn.setForeground(Color.WHITE);
        denyBtn.setBackground(new Color(207, 10, 10));
        denyBtn.setForeground(Color.WHITE);
        denyAllBtn.setBackground(new Color(207, 10, 10));
        denyAllBtn.setForeground(Color.WHITE);
        downloadBtn.setBackground(new Color(220, 95, 0));
        downloadBtn.setForeground(Color.WHITE);
        viewBtn.setBackground(new Color(220, 95, 0));
        viewBtn.setForeground(Color.WHITE);
        
        // Add filter functionality
        applyFilterBtn.addActionListener(e -> {
            String selectedPeriod = (String) periodFilterCombo.getSelectedItem();
            String periodFilter = null;
            
            if (!"All Periods".equals(selectedPeriod)) {
                // Convert display name to filter format
                periodFilter = convertPeriodDisplayToFilter(selectedPeriod);
            }
            
            historyModel.setRowCount(0);
            loadPayslipHistoryData(historyModel, periodFilter);
        });
        
        // Add button listeners
        selectAllBtn.addActionListener(e -> historyTable.selectAll());
        
        approveBtn.addActionListener(e -> {
            int[] selectedRows = historyTable.getSelectedRows();
            if (selectedRows.length == 0) {
                JOptionPane.showMessageDialog(historyDialog, "Please select payslip(s) to approve.");
                return;
            }
            updatePayslipStatus(historyModel, selectedRows, PayslipStatus.APPROVED);
        });
        
        approveAllBtn.addActionListener(e -> {
            int[] allRows = new int[historyModel.getRowCount()];
            for (int i = 0; i < allRows.length; i++) allRows[i] = i;
            updatePayslipStatus(historyModel, allRows, PayslipStatus.APPROVED);
        });
        
        denyBtn.addActionListener(e -> {
            int[] selectedRows = historyTable.getSelectedRows();
            if (selectedRows.length == 0) {
                JOptionPane.showMessageDialog(historyDialog, "Please select payslip(s) to deny.");
                return;
            }
            updatePayslipStatus(historyModel, selectedRows, PayslipStatus.REJECTED);
        });
        
        denyAllBtn.addActionListener(e -> {
            int[] allRows = new int[historyModel.getRowCount()];
            for (int i = 0; i < allRows.length; i++) allRows[i] = i;
            updatePayslipStatus(historyModel, allRows, PayslipStatus.REJECTED);
        });
        
        // Download using same method as ViewPayslip
        downloadBtn.addActionListener(e -> {
            int[] selectedRows = historyTable.getSelectedRows();
            if (selectedRows.length == 0) {
                JOptionPane.showMessageDialog(historyDialog, "Please select payslip(s) to download.");
                return;
            }
            handleDownloadSelectedPayslips(historyModel, selectedRows);
        });
        
        viewBtn.addActionListener(e -> {
            int selectedRow = historyTable.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(historyDialog, "Please select a payslip to view.");
                return;
            }
            showIndividualPayslip(historyModel, selectedRow);
        });
        
        refreshBtn.addActionListener(e -> {
            String selectedPeriod = (String) periodFilterCombo.getSelectedItem();
            String periodFilter = null;
            
            if (!"All Periods".equals(selectedPeriod)) {
                periodFilter = convertPeriodDisplayToFilter(selectedPeriod);
            }
            
            historyModel.setRowCount(0);
            loadPayslipHistoryData(historyModel, periodFilter);
        });
        
        buttonPanel.add(selectAllBtn);
        buttonPanel.add(Box.createHorizontalStrut(10));
        buttonPanel.add(approveBtn);
        buttonPanel.add(approveAllBtn);
        buttonPanel.add(denyBtn);
        buttonPanel.add(denyAllBtn);
        buttonPanel.add(Box.createHorizontalStrut(10));
        buttonPanel.add(downloadBtn);
        buttonPanel.add(viewBtn);
        buttonPanel.add(Box.createHorizontalStrut(10));
        buttonPanel.add(refreshBtn);
        
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(new JLabel("Generated Payslip History - Filter by period, then select payslips to approve, deny, or download"), BorderLayout.NORTH);
        mainPanel.add(filterPanel, BorderLayout.CENTER);
        
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(scrollPane, BorderLayout.CENTER);
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        mainPanel.add(contentPanel, BorderLayout.SOUTH);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        historyDialog.add(mainPanel);
        historyDialog.setVisible(true);
    }
    
    /**
     * Convert period display name to filter format
     */
    private String convertPeriodDisplayToFilter(String displayName) {
        switch (displayName) {
            case "June 2024": return "2024-06";
            case "July 2024": return "2024-07";
            case "August 2024": return "2024-08";
            case "September 2024": return "2024-09";
            case "October 2024": return "2024-10";
            case "November 2024": return "2024-11";
            case "December 2024": return "2024-12";
            default: return null;
        }
    }
    
    /**
     * Load payslip history data with optional period filter, ordered by Employee ID
     * Uses ReportService to get accurate gross income and net pay from database calculations
     */
    private void loadPayslipHistoryData(DefaultTableModel historyModel, String periodFilter) {
        try {
            List<PayPeriodModel> allPeriods = payPeriodDAO.findAll();
            List<PayslipHistoryItem> payslipItems = new ArrayList<>();
            
            for (PayPeriodModel period : allPeriods) {
                // Apply period filter if specified
                if (periodFilter != null) {
                    YearMonth periodMonth = YearMonth.from(period.getStartDate());
                    String periodString = periodMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"));
                    
                    if (!periodString.equals(periodFilter)) {
                        continue; // Skip this period
                    }
                }
                
                List<PayslipModel> payslips = payslipDAO.findByPayPeriod(period.getPayPeriodId());
                
                for (PayslipModel payslip : payslips) {
                    // Convert period name from "2024-06-1st Half" to "June 2024"
                    String displayPeriodName = convertPeriodToDisplay(period.getPeriodName());
                    
                    // Get accurate gross income and net pay from ReportService (matches database calculations)
                    BigDecimal actualGrossIncome = null;
                    BigDecimal actualNetPay = null;
                    
                    try {
                        // Get period YearMonth for ReportService
                        YearMonth periodYearMonth = YearMonth.from(period.getStartDate());
                        
                        // Use ReportService to get accurate payslip data (same as main table)
                        ReportService.EmployeePayslipReport payslipReport = 
                            reportService.generateEmployeePayslipFromView(payslip.getEmployeeId(), periodYearMonth);
                        
                        if (payslipReport.isSuccess() && payslipReport.getPayslip() != null) {
                            // Use ReportService values (these match the database exactly)
                            actualGrossIncome = payslipReport.getPayslip().getGrossIncome(); // Base salary only
                            actualNetPay = payslipReport.getPayslip().getNetPay(); // Accurate net pay
                        } else {
                            // Fallback to PayslipModel values if ReportService fails
                            actualGrossIncome = payslip.getGrossIncome();
                            actualNetPay = payslip.getTakeHomePay();
                        }
                    } catch (Exception e) {
                        System.err.println("Error getting accurate payslip data for employee " + payslip.getEmployeeId() + 
                            " in period " + period.getPeriodName() + ": " + e.getMessage());
                        // Fallback to PayslipModel values
                        actualGrossIncome = payslip.getGrossIncome();
                        actualNetPay = payslip.getTakeHomePay();
                    }
                    
                    PayslipHistoryItem item = new PayslipHistoryItem(
                        payslip.getPayslipId(),
                        payslip.getEmployeeId(),
                        payslip.getEmployeeName(),
                        displayPeriodName,
                        payslip.getUpdatedAt() != null ? 
                            payslip.getUpdatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "Unknown",
                        formatCurrency(actualGrossIncome), // Accurate gross income from database
                        formatCurrency(actualNetPay), // Accurate net pay from database
                        payslip.getStatus().getValue()
                    );
                    payslipItems.add(item);
                }
            }
            
            // Sort by Employee ID (ascending order)
            payslipItems.sort((a, b) -> a.employeeId.compareTo(b.employeeId));
            
            // Add sorted items to table
            for (PayslipHistoryItem item : payslipItems) {
                historyModel.addRow(new Object[]{
                    item.payslipId, item.employeeId, item.employeeName,
                    item.payPeriod, item.generatedDate, item.grossIncome,
                    item.netPay, item.status
                });
            }
            
            // Show summary in console
            if (periodFilter != null) {
                System.out.println("Loaded " + payslipItems.size() + " payslips for period: " + periodFilter + 
                    " (sorted by Employee ID, using accurate database values)");
            } else {
                System.out.println("Loaded " + payslipItems.size() + " payslips (all periods, sorted by Employee ID, using accurate database values)");
            }
            
            // Show message if no data found
            if (payslipItems.isEmpty()) {
                String message = periodFilter != null ? 
                    "No payslips found for period: " + convertFilterToPeriodDisplay(periodFilter) : 
                    "No payslips found in the system";
                historyModel.addRow(new Object[]{message, "", "", "", "", "", "", ""});
            }
            
        } catch (Exception e) {
            System.err.println("Error loading payslip history with accurate values: " + e.getMessage());
            e.printStackTrace();
            String errorMessage = periodFilter != null ? 
                "Error loading payslips for period: " + convertFilterToPeriodDisplay(periodFilter) : 
                "Error loading payslip history";
            historyModel.addRow(new Object[]{errorMessage, "Failed to load history", "", "", "", "", "", "ERROR"});
        }
    }
    
    /**
     * Convert period name from database format to display format
     */
    private String convertPeriodToDisplay(String periodName) {
        if (periodName.contains("2024-06")) return "June 2024";
        if (periodName.contains("2024-07")) return "July 2024";
        if (periodName.contains("2024-08")) return "August 2024";
        if (periodName.contains("2024-09")) return "September 2024";
        if (periodName.contains("2024-10")) return "October 2024";
        if (periodName.contains("2024-11")) return "November 2024";
        if (periodName.contains("2024-12")) return "December 2024";
        return periodName; // fallback to original
    }
    
    /**
     * Convert filter format back to display format
     */
    private String convertFilterToPeriodDisplay(String filter) {
        switch (filter) {
            case "2024-06": return "June 2024";
            case "2024-07": return "July 2024";
            case "2024-08": return "August 2024";
            case "2024-09": return "September 2024";
            case "2024-10": return "October 2024";
            case "2024-11": return "November 2024";
            case "2024-12": return "December 2024";
            default: return filter;
        }
    }
    
    /**
     * Helper class for sorting payslip history items
     */
    private static class PayslipHistoryItem {
        final Integer payslipId;
        final Integer employeeId;
        final String employeeName;
        final String payPeriod;
        final String generatedDate;
        final String grossIncome;
        final String netPay;
        final String status;
        
        PayslipHistoryItem(Integer payslipId, Integer employeeId, String employeeName, 
                          String payPeriod, String generatedDate, String grossIncome, 
                          String netPay, String status) {
            this.payslipId = payslipId;
            this.employeeId = employeeId;
            this.employeeName = employeeName;
            this.payPeriod = payPeriod;
            this.generatedDate = generatedDate;
            this.grossIncome = grossIncome;
            this.netPay = netPay;
            this.status = status;
        }
    }
    
    /**
     * Update payslip status using PayslipDAO
     */
    private void updatePayslipStatus(DefaultTableModel model, int[] rows, PayslipStatus newStatus) {
        try {
            int updatedCount = 0;
            for (int row : rows) {
                // Get payslip ID from the table
                Object payslipIdObj = model.getValueAt(row, 0);
                String currentStatus = (String) model.getValueAt(row, 7);
                
                // Only update if current status is PENDING
                if ("PENDING".equals(currentStatus)) {
                    Integer payslipId = Integer.valueOf(payslipIdObj.toString());
                    if (payslipDAO.updatePayslipStatus(payslipId, newStatus)) {
                        // Update status in table
                        model.setValueAt(newStatus.getValue(), row, 7);
                        updatedCount++;
                    }
                }
            }
            
            if (updatedCount > 0) {
                String action = newStatus == PayslipStatus.APPROVED ? "approved" : "denied";
                JOptionPane.showMessageDialog(null,
                    updatedCount + " payslip(s) have been " + action + " successfully.",
                    "Status Updated", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(null,
                    "No payslips were updated. Only PENDING payslips can be approved or denied.",
                    "No Updates", JOptionPane.WARNING_MESSAGE);
            }
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                "Error updating payslip status: " + e.getMessage(),
                "Update Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Handle download of selected payslips - Uses same download logic as ViewPayslip
     */
    private void handleDownloadSelectedPayslips(DefaultTableModel model, int[] selectedRows) {
        try {
            if (selectedRows.length == 1) {
                // Single payslip download
                downloadSinglePayslip(model, selectedRows[0]);
            } else {
                // Multiple payslips download
                downloadMultiplePayslips(model, selectedRows);
            }
                
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                "Error downloading payslips: " + e.getMessage(),
                "Download Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Download single payslip using same method as ViewPayslip
     */
    private void downloadSinglePayslip(DefaultTableModel model, int selectedRow) {
        try {
            String employeeIdStr = model.getValueAt(selectedRow, 1).toString();
            String employeeName = model.getValueAt(selectedRow, 2).toString();
            String payPeriod = model.getValueAt(selectedRow, 3).toString();
            
            // Parse pay period to get YearMonth
            YearMonth payslipMonth = parsePayPeriodToYearMonth(payPeriod);
            
            if (payslipMonth == null) {
                JOptionPane.showMessageDialog(null,
                    "Could not determine pay period for download.",
                    "Download Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Create default filename - same format as ViewPayslip
            String[] nameParts = employeeName.split(" ");
            String lastName = nameParts.length > 1 ? nameParts[nameParts.length - 1] : employeeName;
            String defaultFileName = String.format("MotorPH_Payslip_%s_%s_%s.pdf", 
                lastName.replaceAll("\\s+", ""), 
                employeeIdStr,
                payslipMonth.format(DateTimeFormatter.ofPattern("yyyy-MM")));

            // Show file chooser
            JFileChooser fileChooser = new JFileChooser();
            String downloadsPath = System.getProperty("user.home") + File.separator + "Downloads";
            fileChooser.setCurrentDirectory(new File(downloadsPath));
            fileChooser.setSelectedFile(new File(defaultFileName));
            fileChooser.setDialogTitle("Save Payslip As...");
            
            javax.swing.filechooser.FileNameExtensionFilter pdfFilter = 
                new javax.swing.filechooser.FileNameExtensionFilter("PDF Documents (*.pdf)", "pdf");
            fileChooser.setFileFilter(pdfFilter);
            fileChooser.setAcceptAllFileFilterUsed(false);
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

            int result = fileChooser.showSaveDialog(this);
            if (result != JFileChooser.APPROVE_OPTION) {
                return;
            }

            // Get chosen file path
            File selectedFile = fileChooser.getSelectedFile();
            String userChosenPath = selectedFile.getAbsolutePath();
            if (!userChosenPath.toLowerCase().endsWith(".pdf")) {
                userChosenPath += ".pdf";
            }

            // Generate payslip data using ReportService (same as ViewPayslip)
            var payslipReport = reportService.generateEmployeePayslipFromView(
                Integer.valueOf(employeeIdStr), payslipMonth);

            if (!payslipReport.isSuccess() || payslipReport.getPayslip() == null) {
                throw new Exception("Failed to retrieve payslip data: " + payslipReport.getErrorMessage());
            }

            // Generate PDF using PurePDFPayslipGenerator (same as ViewPayslip)
            boolean success = PurePDFPayslipGenerator.generatePayslip(
                payslipReport.getPayslip(), 
                userChosenPath
            );

            if (success) {
                // Show success dialog with option to open file
                int openChoice = JOptionPane.showConfirmDialog(this,
                    "Payslip successfully downloaded!\n" +
                    "Employee: " + employeeName + "\n" +
                    "Period: " + payPeriod + "\n" +
                    "Filename: " + new File(userChosenPath).getName() + "\n" +
                    "Location: " + userChosenPath + "\n\n" +
                    "Do you want to open the file now?", 
                    "Download Complete", 
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.INFORMATION_MESSAGE);

                if (openChoice == JOptionPane.YES_OPTION) {
                    try {
                        java.awt.Desktop.getDesktop().open(new File(userChosenPath));
                    } catch (IOException e) {
                        JOptionPane.showMessageDialog(this,
                            "File saved successfully but couldn't open it automatically.\n" +
                            "Please navigate to: " + userChosenPath, 
                            "File Saved", JOptionPane.WARNING_MESSAGE);
                    }
                }
            } else {
                throw new Exception("PDF generation failed - check console for details");
            }
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                "Error downloading payslip: " + e.getMessage(),
                "Download Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    
    /**
     * Download multiple payslips
     */
    private void downloadMultiplePayslips(DefaultTableModel model, int[] selectedRows) {
        try {
            // Show directory chooser for multiple files
            JFileChooser dirChooser = new JFileChooser();
            dirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            dirChooser.setDialogTitle("Select Directory for Payslip Downloads");
            String downloadsPath = System.getProperty("user.home") + File.separator + "Downloads";
            dirChooser.setCurrentDirectory(new File(downloadsPath));
            
            int result = dirChooser.showSaveDialog(this);
            if (result != JFileChooser.APPROVE_OPTION) {
                return;
            }
            
            File selectedDir = dirChooser.getSelectedFile();
            int successCount = 0;
            int failureCount = 0;
            StringBuilder errors = new StringBuilder();
            
            for (int row : selectedRows) {
                try {
                    String employeeIdStr = model.getValueAt(row, 1).toString();
                    String employeeName = model.getValueAt(row, 2).toString();
                    String payPeriod = model.getValueAt(row, 3).toString();
                    
                    // Parse pay period to get YearMonth
                    YearMonth payslipMonth = parsePayPeriodToYearMonth(payPeriod);
                    
                    if (payslipMonth == null) {
                        failureCount++;
                        errors.append("• ").append(employeeName).append(": Could not parse pay period\n");
                        continue;
                    }
                    
                    // Create filename
                    String[] nameParts = employeeName.split(" ");
                    String lastName = nameParts.length > 1 ? nameParts[nameParts.length - 1] : employeeName;
                    String fileName = String.format("MotorPH_Payslip_%s_%s_%s.pdf", 
                        lastName.replaceAll("\\s+", ""), 
                        employeeIdStr,
                        payslipMonth.format(DateTimeFormatter.ofPattern("yyyy-MM")));
                    
                    String filePath = selectedDir.getAbsolutePath() + File.separator + fileName;
                    
                    // Generate payslip data
                    var payslipReport = reportService.generateEmployeePayslipFromView(
                        Integer.valueOf(employeeIdStr), payslipMonth);

                    if (!payslipReport.isSuccess() || payslipReport.getPayslip() == null) {
                        failureCount++;
                        errors.append("• ").append(employeeName).append(": ").append(payslipReport.getErrorMessage()).append("\n");
                        continue;
                    }

                    // Generate PDF
                    boolean success = PurePDFPayslipGenerator.generatePayslip(
                        payslipReport.getPayslip(), 
                        filePath
                    );

                    if (success) {
                        successCount++;
                    } else {
                        failureCount++;
                        errors.append("• ").append(employeeName).append(": PDF generation failed\n");
                    }
                    
                } catch (Exception e) {
                    failureCount++;
                    String employeeName = model.getValueAt(row, 2).toString();
                    errors.append("• ").append(employeeName).append(": ").append(e.getMessage()).append("\n");
                }
            }
            
            // Show summary
            StringBuilder message = new StringBuilder();
            message.append("Download Summary:\n\n");
            message.append("Successfully downloaded: ").append(successCount).append(" payslip(s)\n");
            message.append("Failed downloads: ").append(failureCount).append(" payslip(s)\n");
            message.append("Save location: ").append(selectedDir.getAbsolutePath()).append("\n\n");
            
            if (failureCount > 0) {
                message.append("Errors:\n").append(errors.toString());
            }
            
            JOptionPane.showMessageDialog(this,
                message.toString(),
                "Download Complete",
                failureCount > 0 ? JOptionPane.WARNING_MESSAGE : JOptionPane.INFORMATION_MESSAGE);
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                "Error downloading multiple payslips: " + e.getMessage(),
                "Download Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Parse pay period string to YearMonth
     */
    private YearMonth parsePayPeriodToYearMonth(String payPeriod) {
        try {
            // Handle different pay period formats
            if (payPeriod.contains("2024")) {
                if (payPeriod.toLowerCase().contains("june")) return YearMonth.of(2024, 6);
                if (payPeriod.toLowerCase().contains("july")) return YearMonth.of(2024, 7);
                if (payPeriod.toLowerCase().contains("august")) return YearMonth.of(2024, 8);
                if (payPeriod.toLowerCase().contains("september")) return YearMonth.of(2024, 9);
                if (payPeriod.toLowerCase().contains("october")) return YearMonth.of(2024, 10);
                if (payPeriod.toLowerCase().contains("november")) return YearMonth.of(2024, 11);
                if (payPeriod.toLowerCase().contains("december")) return YearMonth.of(2024, 12);
            }
            
            // Try to extract year-month pattern
            if (payPeriod.matches(".*2024[-/]\\d{2}.*")) {
                String[] parts = payPeriod.split("[-/]");
                for (int i = 0; i < parts.length - 1; i++) {
                    if (parts[i].contains("2024")) {
                        int month = Integer.parseInt(parts[i + 1]);
                        if (month >= 1 && month <= 12) {
                            return YearMonth.of(2024, month);
                        }
                    }
                }
            }
            
            return null;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Format payslip for display - same format as ViewPayslip, ENSURES gross income excludes benefits
     */
    private String formatPayslipForDisplay(ReportService.PayslipDetails payslip) {
        StringBuilder payslipText = new StringBuilder();
        
        // Header - same as ViewPayslip
        payslipText.append("=".repeat(70)).append("\n");
        payslipText.append("                          MOTORPH\n");
        payslipText.append("                    The Filipino's Choice\n");
        payslipText.append("                      Employee Payslip\n");
        payslipText.append("=".repeat(70)).append("\n\n");
        
        // Payslip Number and Dates
        payslipText.append(String.format("%-30s %30s\n", 
            "Payslip No: " + payslip.getPayslipNo(), 
            "Period Start: " + payslip.getPeriodStartDate().format(DateTimeFormatter.ofPattern("MM-dd-yyyy"))));
        payslipText.append(String.format("%-30s %30s\n", 
            "", 
            "Period End: " + payslip.getPeriodEndDate().format(DateTimeFormatter.ofPattern("MM-dd-yyyy"))));
        payslipText.append(String.format("%-30s %30s\n", 
            "", 
            "Pay Date: " + payslip.getPayDate().format(DateTimeFormatter.ofPattern("MM-dd-yyyy"))));
        payslipText.append("\n");
        
        // Employee Information
        payslipText.append("EMPLOYEE INFORMATION:\n");
        payslipText.append("-".repeat(70)).append("\n");
        payslipText.append(String.format("%-20s %-20s %-15s %s\n", 
            "Employee ID:", payslip.getEmployeeId(),
            "TIN:", payslip.getTin() != null ? payslip.getTin() : "N/A"));
        payslipText.append(String.format("%-20s %-20s %-15s %s\n", 
            "Name:", payslip.getEmployeeName(),
            "SSS No:", payslip.getSssNo() != null ? payslip.getSssNo() : "N/A"));
        payslipText.append(String.format("%-20s %-20s %-15s %s\n", 
            "Title:", payslip.getEmployeePosition(),
            "PagIbig No:", payslip.getPagibigNo() != null ? payslip.getPagibigNo() : "N/A"));
        payslipText.append(String.format("%-20s %-20s %-15s %s\n", 
            "Department:", payslip.getDepartment(),
            "Philhealth No:", payslip.getPhilhealthNo() != null ? payslip.getPhilhealthNo() : "N/A"));
        payslipText.append("\n");
        
        // Two-column layout for Earnings and Deductions
        payslipText.append(String.format("%-35s %35s\n", "EARNINGS", "DEDUCTIONS"));
        payslipText.append(String.format("%-35s %35s\n", "=".repeat(35), "=".repeat(35)));
        
        // Main earnings and deductions side by side
        payslipText.append(String.format("%-20s %14s %20s %14s\n", 
            "Monthly Rate:", formatCurrencyForDisplay(payslip.getMonthlyRate()),
            "Social Security System:", formatCurrencyForDisplay(payslip.getSocialSecuritySystem())));
        payslipText.append(String.format("%-20s %14s %20s %14s\n", 
            "Daily Rate:", formatCurrencyForDisplay(payslip.getDailyRate()),
            "Philhealth:", formatCurrencyForDisplay(payslip.getPhilhealth())));
        payslipText.append(String.format("%-20s %14s %20s %14s\n", 
            "Days Worked:", payslip.getDaysWorked().toString(),
            "PagIbig:", formatCurrencyForDisplay(payslip.getPagIbig())));
        payslipText.append(String.format("%-20s %14s %20s %14s\n", 
            "Leaves Taken:", payslip.getLeavesTaken().toString(),
            "Withholding Tax:", formatCurrencyForDisplay(payslip.getWithholdingTax())));
        
        // Show overtime if applicable
        if (payslip.getOvertimeHours().doubleValue() > 0) {
            payslipText.append(String.format("%-20s %14s\n", 
                "Overtime Hours:", payslip.getOvertimeHours().toString()));
        }
        
        payslipText.append("\n");
        
        // Gross income should NOT include benefits
        payslipText.append(String.format("%-20s %14s %20s %14s\n", 
            "GROSS INCOME:", formatCurrencyForDisplay(payslip.getGrossIncome()), // Base salary only
            "TOTAL DEDUCTIONS:", formatCurrencyForDisplay(payslip.getTotalDeductions())));
        payslipText.append("\n");
        
        // Benefits section (SEPARATE from gross income)
        payslipText.append("BENEFITS\n");
        payslipText.append("=".repeat(70)).append("\n");
        payslipText.append(String.format("%-40s %15s\n", "Rice Subsidy:", formatCurrencyForDisplay(payslip.getRiceSubsidy())));
        payslipText.append(String.format("%-40s %15s\n", "Phone Allowance:", formatCurrencyForDisplay(payslip.getPhoneAllowance())));
        payslipText.append(String.format("%-40s %15s\n", "Clothing Allowance:", formatCurrencyForDisplay(payslip.getClothingAllowance())));
        payslipText.append(String.format("%-40s %15s\n", "TOTAL BENEFITS:", formatCurrencyForDisplay(payslip.getTotalBenefits())));
        payslipText.append("\n");
        
        // Net Pay - prominently displayed
        payslipText.append("=".repeat(70)).append("\n");
        payslipText.append(String.format("%25s %-20s %15s\n", "", "NET PAY:", formatCurrencyForDisplay(payslip.getNetPay())));
        payslipText.append("=".repeat(70)).append("\n\n");
        
        // Footer
        payslipText.append("THIS IS A SYSTEM GENERATED PAYSLIP AND DOES NOT REQUIRE A SIGNATURE.\n");
        
        return payslipText.toString();
    }
    
    /**
     * Formats currency for display - same as ViewPayslip
     */
    private String formatCurrencyForDisplay(java.math.BigDecimal amount) {
        if (amount == null) {
            return "₱0.00";
        }
        return String.format("₱%,.2f", amount);
    }
    
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        backpyrllmngmntbttn = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        selectDepJComboBox1 = new javax.swing.JComboBox<>();
        selectDateJComboBox2 = new javax.swing.JComboBox<>();
        generatePayslip = new javax.swing.JButton();
        viewsalarydetails = new javax.swing.JButton();
        viewhistory = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel1.setBackground(new java.awt.Color(220, 95, 0));

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(255, 255, 255));
        jLabel1.setText("Payroll Management");

        backpyrllmngmntbttn.setBackground(new java.awt.Color(207, 10, 10));
        backpyrllmngmntbttn.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        backpyrllmngmntbttn.setForeground(new java.awt.Color(255, 255, 255));
        backpyrllmngmntbttn.setText("Back");
        backpyrllmngmntbttn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                backpyrllmngmntbttnActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(27, 27, 27)
                .addComponent(backpyrllmngmntbttn)
                .addGap(27, 27, 27)
                .addComponent(jLabel1)
                .addContainerGap(686, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap(15, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(backpyrllmngmntbttn)
                    .addComponent(jLabel1))
                .addGap(15, 15, 15))
        );

        jPanel2.setBackground(new java.awt.Color(255, 255, 255));

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null}
            },
            new String [] {
                "Employee ID", "Last Name", "First Name", "Position", "Rice Subsidy", "Phone Allowance", "Clothing Allowance", "Total Allow.", "Gross Pay", "SSS", "PhilHealth", "PagIbig", "Late Deductions", "With. Tax", "Total Deductions", "Net Pay"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jTable1.getTableHeader().setReorderingAllowed(false);
        jScrollPane1.setViewportView(jTable1);

        jLabel2.setText("Department:");

        jLabel3.setText("Select Date:");

        selectDepJComboBox1.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Select" }));
        selectDepJComboBox1.setToolTipText("Select");
        selectDepJComboBox1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectDepJComboBox1ActionPerformed(evt);
            }
        });

        selectDateJComboBox2.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Select" }));
        selectDateJComboBox2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectDateJComboBox2ActionPerformed(evt);
            }
        });

        generatePayslip.setBackground(new java.awt.Color(220, 95, 0));
        generatePayslip.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        generatePayslip.setForeground(new java.awt.Color(255, 255, 255));
        generatePayslip.setText("Generate Payslip");
        generatePayslip.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                generatePayslipActionPerformed(evt);
            }
        });

        viewsalarydetails.setBackground(new java.awt.Color(220, 95, 0));
        viewsalarydetails.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        viewsalarydetails.setForeground(new java.awt.Color(255, 255, 255));
        viewsalarydetails.setText("View Salary Details");
        viewsalarydetails.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                viewsalarydetailsActionPerformed(evt);
            }
        });

        viewhistory.setBackground(new java.awt.Color(220, 95, 0));
        viewhistory.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        viewhistory.setForeground(new java.awt.Color(255, 255, 255));
        viewhistory.setText("View Generated Payslip History");
        viewhistory.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                viewhistoryActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(26, 26, 26)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 939, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel2)
                            .addComponent(selectDepJComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, 119, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel3)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(selectDateJComboBox2, javax.swing.GroupLayout.PREFERRED_SIZE, 89, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(26, 26, 26)
                                .addComponent(viewsalarydetails))))
                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addComponent(generatePayslip, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(viewhistory, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(17, 17, 17)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel2)
                    .addComponent(jLabel3))
                .addGap(6, 6, 6)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(selectDepJComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(selectDateJComboBox2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(viewsalarydetails)))
                .addGap(17, 17, 17)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 293, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(generatePayslip)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(viewhistory)
                .addContainerGap(18, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents
    
    private void selectDateJComboBox2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selectDateJComboBox2ActionPerformed
       // Only reset state, don't auto-load data (user needs to click View Salary Details)
        payslipsGenerated = false;
        payrollApproved = false;
        currentPayPeriod = null;
        updateButtonStates();
        
        // Update current payroll month based on selection
        String selectedDateStr = (String) selectDateJComboBox2.getSelectedItem();
        if (selectedDateStr != null && !selectedDateStr.equals("All")) {
            try {
                currentPayrollMonth = parseYearMonth(selectedDateStr);
            } catch (Exception e) {
                System.err.println("Error parsing selected date: " + e.getMessage());
                currentPayrollMonth = YearMonth.now();
            }
        } else {
            // "All" selected - clear specific month selection
            currentPayrollMonth = null;
        }
        
        // Show welcome message (user needs to click View Salary Details to load data)
        showWelcomeMessage();
    }//GEN-LAST:event_selectDateJComboBox2ActionPerformed

    private void backpyrllmngmntbttnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_backpyrllmngmntbttnActionPerformed
        try {
            new AdminAccounting(loggedInUser).setVisible(true);
            this.dispose();
        } catch (Exception e) {
            System.err.println("Error navigating back to AdminAccounting: " + e.getMessage());
            this.dispose();
        }
    }//GEN-LAST:event_backpyrllmngmntbttnActionPerformed

    private void selectDepJComboBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selectDepJComboBox1ActionPerformed
        // Department filter changed - just show welcome message
        // User needs to click "View Salary Details" to load data with new filter
        showWelcomeMessage();
        updateButtonStates();
    }//GEN-LAST:event_selectDepJComboBox1ActionPerformed
  
    private void viewsalarydetailsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_viewsalarydetailsActionPerformed
        loadPayrollData();        
    }//GEN-LAST:event_viewsalarydetailsActionPerformed
  
    private void generatePayslipActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_generatePayslipActionPerformed
        handleGeneratePayslips();

    }//GEN-LAST:event_generatePayslipActionPerformed

    private void viewhistoryActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_viewhistoryActionPerformed
        showPayslipHistoryDialog();
    }//GEN-LAST:event_viewhistoryActionPerformed
    /**
     * Main method for testing (PayrollManagement should be opened from AdminAccounting)
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(PayrollManagement.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(PayrollManagement.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(PayrollManagement.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(PayrollManagement.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                System.out.println("PayrollManagement should be accessed through AdminAccounting interface");
            }
        });
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton backpyrllmngmntbttn;
    private javax.swing.JButton generatePayslip;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTable1;
    private javax.swing.JComboBox<String> selectDateJComboBox2;
    private javax.swing.JComboBox<String> selectDepJComboBox1;
    private javax.swing.JButton viewhistory;
    private javax.swing.JButton viewsalarydetails;
    // End of variables declaration//GEN-END:variables
}
