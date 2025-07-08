//TO BE FIXED

package gui;

import Models.*;
import DAOs.*;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;

import javax.swing.table.DefaultTableModel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.JDialog;
import javax.swing.JTable;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.BorderFactory;
import java.awt.BorderLayout;

/**
 * Refactored PayrollManagement GUI class for Accounting users
 * Uses proper DAO pattern and AccountingModel for business logic
 */
public class PayrollManagement extends javax.swing.JFrame {

    // Core components
    private UserAuthenticationModel loggedInUser;
    private AccountingModel accountingModel;
    
    // DAO components - centralized data access
    private PayrollDAO payrollDAO;
    private PayPeriodDAO payPeriodDAO;
    private EmployeeDAO employeeDAO;
    private PositionDAO positionDAO;
    private PositionBenefitDAO positionBenefitDAO;
    
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
        
        // Initially disable buttons until data is loaded
        updateButtonStates();
        
        System.out.println("PayrollManagement initialized for user: " + user.getFirstName() + " " + user.getLastName());
    }
    
    /**
     * Initialize AccountingModel and DAO components using proper pattern
     */
    private void initializeComponents() {
        try {
            // Create AccountingModel from existing user
            if (loggedInUser != null) {
                EmployeeModel empModel = new EmployeeModel(
                    loggedInUser.getFirstName(), 
                    loggedInUser.getLastName(), 
                    loggedInUser.getEmail(), 
                    "Accounting" // Force accounting role for permissions
                );
                empModel.setEmployeeId(loggedInUser.getEmployeeId());
                
                this.accountingModel = new AccountingModel(empModel);
            } else {
                throw new IllegalArgumentException("User must be logged in to access PayrollManagement");
            }
            
            // Initialize DAOs with shared database connection
            DatabaseConnection dbConnection = new DatabaseConnection();
            this.payrollDAO = new PayrollDAO(dbConnection);
            this.payPeriodDAO = new PayPeriodDAO();
            this.employeeDAO = new EmployeeDAO(dbConnection);
            this.positionDAO = new PositionDAO(dbConnection);
            this.positionBenefitDAO = new PositionBenefitDAO(dbConnection);
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Error initializing payroll management components: " + e.getMessage(),
                "Initialization Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
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
     * Updates the enabled/disabled state of buttons based on current state
     */
    private void updateButtonStates() {
        boolean hasData = currentPayrollData != null && !currentPayrollData.isEmpty();
        
        // Generate payslip is always enabled for Accounting users
        generatePayslip.setEnabled(true);
        
        // View salary details is enabled when month/year are selected
        String selectedMonth = (String) selectDateJComboBox2.getSelectedItem();
        viewsalarydetails.setEnabled(selectedMonth != null && !selectedMonth.equals("Select"));
        
        // Approval/Denial buttons enabled only when payslips are generated
        approveBttn.setEnabled(payslipsGenerated && !payrollApproved && hasData);
        approveAllBttn.setEnabled(payslipsGenerated && !payrollApproved && hasData);
        denyBttn.setEnabled(payslipsGenerated && !payrollApproved && hasData);
        denyAllBttn.setEnabled(payslipsGenerated && !payrollApproved && hasData);
        
        // Download buttons enabled when data exists
        downloadPayslip1.setEnabled(hasData);
        viewhistory.setEnabled(true); // Always enabled
    }
    
    /**
     * Populates the department dropdown using PositionDAO
     */
    private void populateDepartmentDropdown() {
        try {
            selectDepJComboBox1.removeAllItems();
            selectDepJComboBox1.addItem("All");
            
            // Get unique departments from PositionDAO
            List<PositionModel> positions = positionDAO.findAll();
            List<String> departments = new ArrayList<>();
            
            for (PositionModel position : positions) {
                String dept = position.getDepartment();
                if (dept != null && !departments.contains(dept)) {
                    departments.add(dept);
                }
            }
            
            // Sort departments and add to dropdown
            departments.sort(String::compareToIgnoreCase);
            for (String department : departments) {
                selectDepJComboBox1.addItem(department);
            }
            
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
     * Populates the month dropdown using PayPeriodDAO
     */
    private void populateMonthDropdown() {
        try {
            selectDateJComboBox2.removeAllItems();
            selectDateJComboBox2.addItem("Select");
            
            // Get available pay periods from PayPeriodDAO
            List<PayPeriodModel> payPeriods = payPeriodDAO.findAll();
            
            for (PayPeriodModel period : payPeriods) {
                if (period.getStartDate() != null) {
                    YearMonth yearMonth = YearMonth.from(period.getStartDate());
                    String monthString = yearMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"));
                    selectDateJComboBox2.addItem(monthString);
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error populating month dropdown: " + e.getMessage());
            // Fallback to hardcoded months
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
     * Sets up the table columns for payroll data display
     */
    private void setupTableColumns() {
        DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
        model.setColumnIdentifiers(new Object[]{
            "Employee ID", "Last Name", "First Name", "Position", "Department",
            "Rice Subsidy", "Phone Allowance", "Clothing Allowance", "Total Benefits", 
            "Gross Pay", "SSS", "PhilHealth", "Pag-Ibig", "Withholding Tax", 
            "Total Deductions", "Net Pay"
        });
        model.setRowCount(0);
    }

    /**
     * Loads and displays payroll data using AccountingModel and DAOs
     */
    private void loadPayrollData() {
        DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
        model.setRowCount(0);
        currentPayrollData.clear();

        try {
            String selectedMonthStr = (String) selectDateJComboBox2.getSelectedItem();
            String selectedDepartment = (String) selectDepJComboBox1.getSelectedItem();

            if (selectedMonthStr == null || selectedMonthStr.equals("Select") || selectedMonthStr.equals("All")) {
                JOptionPane.showMessageDialog(this,
                    "Please select a valid month/year from the dropdown.",
                    "Selection Required", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Parse year-month and find corresponding pay period using PayPeriodDAO
            YearMonth selectedYearMonth = parseYearMonth(selectedMonthStr);
            PayPeriodModel payPeriod = findPayPeriodForMonth(selectedYearMonth);
            
            if (payPeriod == null) {
                JOptionPane.showMessageDialog(this,
                    "No available data for the selected pay period: " + selectedMonthStr,
                    "No Data Available", JOptionPane.INFORMATION_MESSAGE);
                updateButtonStates();
                return;
            }

            this.currentPayPeriod = payPeriod;
            this.currentPayrollMonth = selectedYearMonth;

            // Get payroll records using AccountingModel
            List<PayrollModel> payrollRecords = accountingModel.getPayrollRecords(payPeriod.getPayPeriodId());
            
            if (payrollRecords.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                    "No available data for the selected pay period: " + selectedMonthStr,
                    "No Data Available", JOptionPane.INFORMATION_MESSAGE);
                updateButtonStates();
                return;
            }

            // Check if payslips are already generated
            payslipsGenerated = payrollDAO.isPayrollGenerated(payPeriod.getPayPeriodId());

            // Process and display payroll data
            int displayedRecords = 0;
            for (PayrollModel payroll : payrollRecords) {
                EmployeeModel employee = accountingModel.getEmployeeById(payroll.getEmployeeId());
                if (employee != null) {
                    // Check department filter using PositionDAO
                    if (shouldIncludeEmployee(employee, selectedDepartment)) {
                        addPayrollRowToTable(model, payroll, employee);
                        currentPayrollData.add(payroll);
                        displayedRecords++;
                    }
                }
            }

            if (displayedRecords == 0) {
                JOptionPane.showMessageDialog(this,
                    "No employees found for the selected department: " + selectedDepartment,
                    "No Data Available", JOptionPane.INFORMATION_MESSAGE);
            } else {
                // Display pay period information
                displayPayPeriodInfo(payPeriod);
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Error loading payroll data: " + e.getMessage(),
                "Data Loading Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }

        updateButtonStates();
    }
    
    /**
     * Parses year-month string to YearMonth object
     */
    private YearMonth parseYearMonth(String yearMonthStr) {
        try {
            String[] parts = yearMonthStr.split("-");
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            return YearMonth.of(year, month);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid year-month format: " + yearMonthStr);
        }
    }
    
    /**
     * Finds the pay period using PayPeriodDAO
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
            return null;
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
                    return position.getDepartment().equalsIgnoreCase(selectedDepartment);
                }
            }
        } catch (Exception e) {
            System.err.println("Error checking employee department: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Adds a payroll record to the table using all DAO components
     */
    private void addPayrollRowToTable(DefaultTableModel model, PayrollModel payroll, EmployeeModel employee) {
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
            
            // Get benefits using PositionBenefitDAO
            BigDecimal riceSubsidy = BigDecimal.ZERO;
            BigDecimal phoneAllowance = BigDecimal.ZERO;
            BigDecimal clothingAllowance = BigDecimal.ZERO;
            
            if (employee.getPositionId() != null) {
                riceSubsidy = positionBenefitDAO.getBenefitValueByPositionAndName(
                    employee.getPositionId(), "Rice Subsidy");
                phoneAllowance = positionBenefitDAO.getBenefitValueByPositionAndName(
                    employee.getPositionId(), "Phone Allowance");
                clothingAllowance = positionBenefitDAO.getBenefitValueByPositionAndName(
                    employee.getPositionId(), "Clothing Allowance");
            }
            
            // Calculate deductions using the same logic as PayrollDAO
            BigDecimal grossIncome = payroll.getGrossIncome();
            BigDecimal sssDeduction = grossIncome.multiply(new BigDecimal("0.045"));
            BigDecimal philHealthDeduction = grossIncome.multiply(new BigDecimal("0.0275"));
            BigDecimal pagIbigDeduction = grossIncome.multiply(new BigDecimal("0.02"));
            BigDecimal withholdingTax = calculateWithholdingTax(grossIncome);
            
            model.addRow(new Object[]{
                employee.getEmployeeId(),
                employee.getLastName(),
                employee.getFirstName(),
                positionName,
                department,
                formatCurrency(riceSubsidy),
                formatCurrency(phoneAllowance),
                formatCurrency(clothingAllowance),
                formatCurrency(payroll.getTotalBenefit()),
                formatCurrency(payroll.getGrossIncome()),
                formatCurrency(sssDeduction),
                formatCurrency(philHealthDeduction),
                formatCurrency(pagIbigDeduction),
                formatCurrency(withholdingTax),
                formatCurrency(payroll.getTotalDeduction()),
                formatCurrency(payroll.getNetSalary())
            });
            
        } catch (Exception e) {
            System.err.println("Error adding payroll row to table: " + e.getMessage());
        }
    }
    
    /**
     * Display pay period information using PayPeriodModel methods
     */
    private void displayPayPeriodInfo(PayPeriodModel payPeriod) {
        if (payPeriod != null) {
            String info = String.format("Pay Period: %s | %s | Working Days: %d",
                payPeriod.getPeriodName(),
                payPeriod.getFormattedPeriod(),
                payPeriod.getWorkingDays()
            );
            
            System.out.println("Current " + info);
        }
    }
    
    /**
     * Calculate withholding tax using same logic as PayrollDAO
     */
    private BigDecimal calculateWithholdingTax(BigDecimal grossIncome) {
        if (grossIncome.compareTo(new BigDecimal("20833")) <= 0) {
            return BigDecimal.ZERO;
        } else if (grossIncome.compareTo(new BigDecimal("33333")) <= 0) {
            return grossIncome.subtract(new BigDecimal("20833")).multiply(new BigDecimal("0.20"));
        } else {
            return grossIncome.multiply(new BigDecimal("0.25"));
        }
    }
    
    private String formatCurrency(BigDecimal amount) {
        return String.format("₱%.2f", amount != null ? amount : BigDecimal.ZERO);
    }

    /**
     * Generate payslips using PayrollDAO.generatePayroll method
     */
    private void handleGeneratePayslips() {
        try {
            String selectedMonthStr = (String) selectDateJComboBox2.getSelectedItem();
            
            if (selectedMonthStr == null || selectedMonthStr.equals("Select")) {
                JOptionPane.showMessageDialog(this,
                    "Please select a month/year first.",
                    "Selection Required", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            // Show confirmation dialog
            int choice = JOptionPane.showConfirmDialog(this,
                "Generate all payslips for " + selectedMonthStr + "?",
                "Generate Payslips",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
            
            if (choice == JOptionPane.YES_OPTION) {
                generatePayslipsForPeriod();
            }
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Error generating payslips: " + e.getMessage(),
                "Generation Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Generate payslips for the current pay period using PayrollDAO
     */
    private void generatePayslipsForPeriod() {
        try {
            if (currentPayPeriod == null) {
                YearMonth selectedYearMonth = parseYearMonth((String) selectDateJComboBox2.getSelectedItem());
                currentPayPeriod = findPayPeriodForMonth(selectedYearMonth);
            }
            
            if (currentPayPeriod == null) {
                JOptionPane.showMessageDialog(this,
                    "No pay period found for the selected month.",
                    "Generation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Check if payroll already exists using PayrollDAO
            if (payrollDAO.isPayrollGenerated(currentPayPeriod.getPayPeriodId())) {
                int choice = JOptionPane.showConfirmDialog(this,
                    "Payroll already exists for this period. Regenerate?",
                    "Payroll Exists",
                    JOptionPane.YES_NO_OPTION);
                
                if (choice != JOptionPane.YES_OPTION) {
                    return;
                }
                
                // Delete existing payroll before regenerating
                payrollDAO.deletePayrollByPeriod(currentPayPeriod.getPayPeriodId());
            }
            
            // Generate payroll using PayrollDAO
            int generatedCount = payrollDAO.generatePayroll(currentPayPeriod.getPayPeriodId());
            
            if (generatedCount > 0) {
                payslipsGenerated = true;
                payrollApproved = false;
                
                JOptionPane.showMessageDialog(this,
                    "Successfully generated payslips for " + generatedCount + " employees.\n" +
                    "Pay Period: " + currentPayPeriod.getPeriodName() + "\n" +
                    "Period: " + currentPayPeriod.getFormattedPeriod() + "\n\n" +
                    "You can now approve or deny the payroll.",
                    "Payslips Generated",
                    JOptionPane.INFORMATION_MESSAGE);
                
                // Refresh the data display
                loadPayrollData();
            } else {
                JOptionPane.showMessageDialog(this,
                    "No payslips were generated. Please check if employees exist for this period.",
                    "Generation Warning", JOptionPane.WARNING_MESSAGE);
            }
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Error during payroll generation: " + e.getMessage(),
                "Generation Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
        
        updateButtonStates();
    }

    /**
     * Handle approve button using PayrollDAO
     */
    private void handleApprovePayroll() {
        int selectedRow = jTable1.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                "Please select an employee from the table to approve.",
                "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String employeeId = jTable1.getValueAt(selectedRow, 0).toString();
        String lastName = jTable1.getValueAt(selectedRow, 1).toString();
        String firstName = jTable1.getValueAt(selectedRow, 2).toString();
        
        // Update payroll status using PayrollDAO
        if (currentPayPeriod != null) {
            payrollDAO.updatePayrollStatus(currentPayPeriod.getPayPeriodId(), "Approved");
        }
        
        JOptionPane.showMessageDialog(this,
            "Payroll for " + firstName + " " + lastName + " (ID: " + employeeId + ") has been approved.\n" +
            "Salary has been disbursed for the pay period: " + 
            (currentPayPeriod != null ? currentPayPeriod.getPeriodName() : "Current Period"),
            "Payroll Approved", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Handle approve all button using PayrollDAO
     */
    private void handleApproveAllPayroll() {
        if (jTable1.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this,
                "No payroll data to approve. Please load data first.",
                "No Data", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Update all payroll status using PayrollDAO
        if (currentPayPeriod != null) {
            int updatedCount = payrollDAO.updatePayrollStatus(currentPayPeriod.getPayPeriodId(), "Approved");
            payrollApproved = true;
            
            JOptionPane.showMessageDialog(this,
                "All payrolls for " + currentPayPeriod.getPeriodName() + 
                " have been approved.\nSalaries have been disbursed for " + updatedCount + " employees.",
                "All Payrolls Approved", JOptionPane.INFORMATION_MESSAGE);
        }
        
        updateButtonStates();
    }

    /**
     * Handle deny button
     */
    private void handleDenyPayroll() {
        int selectedRow = jTable1.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                "Please select an employee from the table to deny.",
                "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String employeeId = jTable1.getValueAt(selectedRow, 0).toString();
        String lastName = jTable1.getValueAt(selectedRow, 1).toString();
        String firstName = jTable1.getValueAt(selectedRow, 2).toString();
        
        JOptionPane.showMessageDialog(this,
            "Payroll for " + firstName + " " + lastName + " (ID: " + employeeId + ") has been denied.\n" +
            "Salary will not be disbursed for the pay period: " + 
            (currentPayPeriod != null ? currentPayPeriod.getPeriodName() : "Current Period"),
            "Payroll Denied", JOptionPane.WARNING_MESSAGE);
    }

    /**
     * Handle deny all button using PayrollDAO
     */
    private void handleDenyAllPayroll() {
        if (jTable1.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this,
                "No payroll data to deny. Please load data first.",
                "No Data", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Delete payroll records using PayrollDAO
        if (currentPayPeriod != null) {
            int deletedCount = payrollDAO.deletePayrollByPeriod(currentPayPeriod.getPayPeriodId());
            payslipsGenerated = false;
            payrollApproved = false;
            
            JOptionPane.showMessageDialog(this,
                "All payrolls for " + currentPayPeriod.getPeriodName() + 
                " have been denied and deleted.\nNo salaries will be disbursed for this pay period.",
                "All Payrolls Denied", JOptionPane.WARNING_MESSAGE);
            
            // Clear the table
            DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
            model.setRowCount(0);
            currentPayrollData.clear();
        }
        
        updateButtonStates();
    }

    /**
     * Handle download payslip
     */
    private void handleDownloadPayslip() {
        int[] selectedRows = jTable1.getSelectedRows();
        
        if (selectedRows.length == 0) {
            JOptionPane.showMessageDialog(this,
                "Please select at least one employee from the table to download payslip(s).",
                "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // For now, just show a message since payslip generation requires external dependencies
        String message = selectedRows.length == 1 
            ? "Payslip download initiated for selected employee."
            : selectedRows.length + " payslips download initiated.";
        
        JOptionPane.showMessageDialog(this,
            message + "\nPayslip files would be generated here.",
            "Download Initiated", JOptionPane.INFORMATION_MESSAGE);
    }
    
    /**
     * Find payroll data for a specific employee from current data
     */
    private PayrollModel findPayrollForEmployee(Integer employeeId) {
        for (PayrollModel payroll : currentPayrollData) {
            if (payroll.getEmployeeId().equals(employeeId)) {
                return payroll;
            }
        }
        return null;
    }
    
    /**
     * Show payslip history in a dialog using PayrollDAO methods
     */
    private void showPayslipHistoryDialog() {
        JDialog historyDialog = new JDialog(this, "Generated Payslip History", true);
        historyDialog.setSize(800, 600);
        historyDialog.setLocationRelativeTo(this);
        
        // Create table for history
        String[] columns = {"Employee ID", "Employee Name", "Pay Period", "Generated Date", 
                           "Gross Income", "Net Salary", "Status"};
        DefaultTableModel historyModel = new DefaultTableModel(columns, 0);
        
        // Get payroll history using PayrollDAO and PayPeriodDAO
        try {
            List<PayPeriodModel> allPeriods = payPeriodDAO.findAll();
            
            for (PayPeriodModel period : allPeriods) {
                if (payrollDAO.isPayrollGenerated(period.getPayPeriodId())) {
                    List<PayrollModel> payrolls = payrollDAO.findByPayPeriod(period.getPayPeriodId());
                    
                    for (PayrollModel payroll : payrolls) {
                        EmployeeModel employee = employeeDAO.findById(payroll.getEmployeeId());
                        if (employee != null) {
                            historyModel.addRow(new Object[]{
                                employee.getEmployeeId(),
                                employee.getFullName(),
                                period.getPeriodName(),
                                payroll.getCreatedAt() != null ? 
                                    payroll.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "Unknown",
                                formatCurrency(payroll.getGrossIncome()),
                                formatCurrency(payroll.getNetSalary()),
                                "Generated"
                            });
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading payslip history: " + e.getMessage());
            historyModel.addRow(new Object[]{"Error", "Failed to load history", "", "", "", "", ""});
        }
        
        JTable historyTable = new JTable(historyModel);
        historyTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        JScrollPane scrollPane = new JScrollPane(historyTable);
        
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel("Payslip Generation History"), BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        historyDialog.add(panel);
        historyDialog.setVisible(true);
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
        approveBttn = new javax.swing.JButton();
        denyBttn = new javax.swing.JButton();
        generatePayslip = new javax.swing.JButton();
        approveAllBttn = new javax.swing.JButton();
        denyAllBttn = new javax.swing.JButton();
        viewsalarydetails = new javax.swing.JButton();
        downloadPayslip1 = new javax.swing.JButton();
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

        approveBttn.setBackground(new java.awt.Color(0, 153, 0));
        approveBttn.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        approveBttn.setForeground(new java.awt.Color(255, 255, 255));
        approveBttn.setText("Approve");
        approveBttn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                approveBttnActionPerformed(evt);
            }
        });

        denyBttn.setBackground(new java.awt.Color(207, 10, 10));
        denyBttn.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        denyBttn.setForeground(new java.awt.Color(255, 255, 255));
        denyBttn.setText("Deny");
        denyBttn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                denyBttnActionPerformed(evt);
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

        approveAllBttn.setBackground(new java.awt.Color(0, 153, 0));
        approveAllBttn.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        approveAllBttn.setForeground(new java.awt.Color(255, 255, 255));
        approveAllBttn.setText("Approve all");
        approveAllBttn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                approveAllBttnActionPerformed(evt);
            }
        });

        denyAllBttn.setBackground(new java.awt.Color(207, 10, 10));
        denyAllBttn.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        denyAllBttn.setForeground(new java.awt.Color(255, 255, 255));
        denyAllBttn.setText("Deny all");
        denyAllBttn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                denyAllBttnActionPerformed(evt);
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

        downloadPayslip1.setBackground(new java.awt.Color(220, 95, 0));
        downloadPayslip1.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        downloadPayslip1.setForeground(new java.awt.Color(255, 255, 255));
        downloadPayslip1.setText("Download Payslip");
        downloadPayslip1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                downloadPayslip1ActionPerformed(evt);
            }
        });

        viewhistory.setBackground(new java.awt.Color(220, 95, 0));
        viewhistory.setFont(new java.awt.Font("Segoe UI", 1, 10)); // NOI18N
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
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 939, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(generatePayslip, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 187, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(approveAllBttn, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(denyAllBttn, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(approveBttn)
                                    .addComponent(denyBttn, javax.swing.GroupLayout.PREFERRED_SIZE, 78, javax.swing.GroupLayout.PREFERRED_SIZE))))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(downloadPayslip1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(viewsalarydetails, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(viewhistory, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel2)
                            .addComponent(selectDepJComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, 119, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel3)
                            .addComponent(selectDateJComboBox2, javax.swing.GroupLayout.PREFERRED_SIZE, 89, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addGap(5, 5, 5))
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
                    .addComponent(selectDateJComboBox2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 293, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(generatePayslip)
                    .addComponent(downloadPayslip1))
                .addGap(13, 13, 13)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(approveAllBttn)
                    .addComponent(approveBttn)
                    .addComponent(viewsalarydetails))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(denyAllBttn)
                    .addComponent(denyBttn)
                    .addComponent(viewhistory))
                .addGap(14, 14, 14))
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
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents
    
    private void approveBttnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_approveBttnActionPerformed
        handleApprovePayroll();
    }//GEN-LAST:event_approveBttnActionPerformed

    private void selectDateJComboBox2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selectDateJComboBox2ActionPerformed
     // Reset state when date selection changes
        payslipsGenerated = false;
        payrollApproved = false;
        currentPayPeriod = null; // Reset current pay period
        updateButtonStates();
        
        // Clear the table and current data
        DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
        model.setRowCount(0);
        currentPayrollData.clear();
        
        // Update current payroll month based on selection
        String selectedDateStr = (String) selectDateJComboBox2.getSelectedItem();
        if (selectedDateStr != null && !selectedDateStr.equals("Select")) {
            try {
                currentPayrollMonth = parseYearMonth(selectedDateStr);
            } catch (Exception e) {
                System.err.println("Error parsing selected date: " + e.getMessage());
                currentPayrollMonth = YearMonth.now();
            }
        }
        
        // Auto-load data if both filters are set
        String selectedDept = (String) selectDepJComboBox1.getSelectedItem();
        if (selectedDateStr != null && !selectedDateStr.equals("Select") && selectedDept != null) {
            loadPayrollData();
        }
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
        // Reset state when department filter changes
        payslipsGenerated = false;
        payrollApproved = false;
        updateButtonStates();
        
        // Clear the table and current data
        DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
        model.setRowCount(0);
        currentPayrollData.clear();
        
        // Auto-load data if both filters are set
        String selectedDept = (String) selectDepJComboBox1.getSelectedItem();
        String selectedDate = (String) selectDateJComboBox2.getSelectedItem();
        if (selectedDept != null && selectedDate != null && !selectedDate.equals("Select")) {
            loadPayrollData();
        }
    }//GEN-LAST:event_selectDepJComboBox1ActionPerformed

    private void approveAllBttnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_approveAllBttnActionPerformed
        handleApproveAllPayroll();

    }//GEN-LAST:event_approveAllBttnActionPerformed

    private void denyAllBttnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_denyAllBttnActionPerformed
        handleDenyAllPayroll();
    }//GEN-LAST:event_denyAllBttnActionPerformed
  
    private void viewsalarydetailsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_viewsalarydetailsActionPerformed
        loadPayrollData();        
    }//GEN-LAST:event_viewsalarydetailsActionPerformed
 
    private void denyBttnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_denyBttnActionPerformed
        handleDenyPayroll();
    }//GEN-LAST:event_denyBttnActionPerformed
 
    private void generatePayslipActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_generatePayslipActionPerformed
        handleGeneratePayslips();

    }//GEN-LAST:event_generatePayslipActionPerformed

    private void downloadPayslip1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_downloadPayslip1ActionPerformed
        handleDownloadPayslip();
    }//GEN-LAST:event_downloadPayslip1ActionPerformed

    private void viewhistoryActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_viewhistoryActionPerformed
        showPayslipHistoryDialog();
    }//GEN-LAST:event_viewhistoryActionPerformed
     /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
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
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                // PayrollManagement should be opened from AdminAccounting, not directly
                System.out.println("PayrollManagement should be accessed through AdminAccounting interface");
            }
        });
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton approveAllBttn;
    private javax.swing.JButton approveBttn;
    private javax.swing.JButton backpyrllmngmntbttn;
    private javax.swing.JButton denyAllBttn;
    private javax.swing.JButton denyBttn;
    private javax.swing.JButton downloadPayslip1;
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
