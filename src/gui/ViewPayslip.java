package gui;

import Models.UserAuthenticationModel;
import DAOs.DatabaseConnection;
import DAOs.EmployeeDAO;
import Services.ReportService;
import Services.JasperReportGenerator;
import Services.PurePDFPayslipGenerator;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.Cursor;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import javax.swing.JFileChooser;

/**
 * ViewPayslip GUI Class - Follows proper separation of concerns
 * Contains UI helper methods (formatting, display, navigation)
 * Event handlers that delegate to services  
 */
public class ViewPayslip extends javax.swing.JFrame {
    
    // Services that handle business logic
    private UserAuthenticationModel loggedInUser;
    private DatabaseConnection databaseConnection;
    private ReportService reportService;              // Handles payroll calculations
    private EmployeeDAO employeeDAO;
    private String employeeId;
    private String employeeName;

    // Current UI state
    private ReportService.EmployeePayslipReport currentPayslipReport;
    
    /**
     * Creates new form ViewPayslip without user data
     */
    public ViewPayslip() {
        initializeServices(); // Initialize business services
        initComponents();
        setupUIComponents(); // UI setup only
        System.out.println("ViewPayslip initialized without user data");
    }
    
    /**
     * Creates new form ViewPayslip with logged in user data
     * @param user The currently logged in user
     */
    public ViewPayslip(UserAuthenticationModel user) {
        this.loggedInUser = user;
        initializeServices(); // Initialize business services

        // Initialize employee info BEFORE setting up UI
        if (loggedInUser != null) {
            this.employeeId = String.valueOf(loggedInUser.getEmployeeId());
            this.employeeName = loggedInUser.getFirstName() + " " + loggedInUser.getLastName();
            System.out.println("ViewPayslip initialized for user: " + 
                user.getFirstName() + " " + user.getLastName() + 
                " (ID: " + user.getEmployeeId() + ")");
        } else {
            System.err.println("ViewPayslip initialized with null user");
        }

        initComponents();
        setupUIComponents(); // UI setup only

        if (loggedInUser != null) {
            loadUserDataToUI(); // UI data loading
        }
    }
    
    // SERVICE INITIALIZATION - Business Logic Setup
    
    /**
     * Initialize business services and DAOs
     * This is dependency injection, not business logic
     */
    private void initializeServices() {
        try {
            this.databaseConnection = new DatabaseConnection();
            this.employeeDAO = new EmployeeDAO(databaseConnection);
            this.reportService = new ReportService(databaseConnection);
            
            System.out.println("Services initialized successfully");
        } catch (Exception e) {
            System.err.println("Error initializing services: " + e.getMessage());
            e.printStackTrace();
            showErrorDialog("Error initializing database services: " + e.getMessage(), 
                          "Initialization Error");
        }
    }
    
    // UI HELPER METHODS - These belong in GUI
    
    /**
     * Sets up UI components and styling
     */
    private void setupUIComponents() {
        try {
            // Center the form on screen
            this.setLocationRelativeTo(null);
            
            // Set form title
            this.setTitle("Payslip Details - "+ employeeName);
            
            // Populate dropdown with available months
            initializeDateFilter();
            
            // Set monospaced font for better text alignment
            jTextArea1.setFont(new java.awt.Font("Monospaced", 0, 12));
            jTextArea1.setEditable(false);
            
            // Initial welcome message
            showWelcomeMessage();
            
            System.out.println("UI components setup completed successfully");
        } catch (Exception e) {
            System.err.println("Error during UI setup: " + e.getMessage());
            e.printStackTrace();
            showErrorDialog("Error initializing UI: " + e.getMessage(), "UI Error");
        }
    }
    
    /**
     * Loads user data into UI fields
     */
    private void loadUserDataToUI() {
        try {
            if (loggedInUser == null) {
                System.err.println("No logged in user to load data for");
                return;
            }
            
            // Set basic employee info in UI fields
            inputpayslipempid.setText(String.valueOf(loggedInUser.getEmployeeId()));
            inputpayslipfirstnm.setText(loggedInUser.getFirstName());
            inputpaysliplastnm.setText(loggedInUser.getLastName());
            inputpayslipposition.setText(loggedInUser.getUserRole());
            
            System.out.println("Employee data loaded successfully to UI");
            
        } catch (Exception e) {
            System.err.println("Error loading employee data to UI: " + e.getMessage());
            e.printStackTrace();
            showErrorDialog("Error loading employee data: " + e.getMessage(), "Data Error");
            
            // Set fallback values from loggedInUser
            if (loggedInUser != null) {
                inputpayslipempid.setText(String.valueOf(loggedInUser.getEmployeeId()));
                inputpayslipfirstnm.setText(loggedInUser.getFirstName());
                inputpaysliplastnm.setText(loggedInUser.getLastName());
                inputpayslipposition.setText(loggedInUser.getUserRole());
            }
        }
    }
    
    /**
     * Initialize date filter with specific 2024 months
     */
    private void initializeDateFilter() {
        try {
            monthcombo.removeAllItems();
            monthcombo.addItem("Select");
            // Add only specific months from 2024
            monthcombo.addItem("2024-06");
            monthcombo.addItem("2024-07");
            monthcombo.addItem("2024-08");
            monthcombo.addItem("2024-09");
            monthcombo.addItem("2024-10");
            monthcombo.addItem("2024-11");
            monthcombo.addItem("2024-12");
            monthcombo.setSelectedItem("Select");
            
        } catch (Exception e) {
            System.err.println("Error populating month dropdown: " + e.getMessage());
            // Fallback to simple options
            monthcombo.removeAllItems();
            monthcombo.addItem("Select");
            monthcombo.addItem("2024-06");
        }
    }
    
    /**
     * Gets selected YearMonth from dropdown
     */
    private YearMonth getSelectedYearMonth() {
        String selected = (String) monthcombo.getSelectedItem();
        if (selected == null || selected.equals("Select")) {
            return null;
        }
        
        // Parse directly from yyyy-MM format
        try {
            return YearMonth.parse(selected);
        } catch (Exception e) {
            System.err.println("Error parsing selected month: " + selected);
            return null;
        }
    }
    
    /**
     * Formats and displays payslip details to match JasperReports template format
     */
    private void displayPayslipInUI(ReportService.PayslipDetails payslip) {
        StringBuilder payslipText = new StringBuilder();
        
        // Header - Match JasperReports template
        payslipText.append("=".repeat(70)).append("\n");
        payslipText.append("                          MOTORPH\n");
        payslipText.append("                    The Filipino's Choice\n");
        payslipText.append("                      Employee Payslip\n");
        payslipText.append("=".repeat(70)).append("\n\n");
        
        // Payslip Number and Dates - Match template format
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
        
        // Employee Information - Match template layout
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
            "Monthly Rate:", formatCurrencyForUI(payslip.getMonthlyRate()),
            "Social Security System:", formatCurrencyForUI(payslip.getSocialSecuritySystem())));
        payslipText.append(String.format("%-20s %14s %20s %14s\n", 
            "Daily Rate:", formatCurrencyForUI(payslip.getDailyRate()),
            "Philhealth:", formatCurrencyForUI(payslip.getPhilhealth())));
        payslipText.append(String.format("%-20s %14s %20s %14s\n", 
            "Days Worked:", payslip.getDaysWorked().toString(),
            "PagIbig:", formatCurrencyForUI(payslip.getPagIbig())));
        payslipText.append(String.format("%-20s %14s %20s %14s\n", 
            "Leaves Taken:", payslip.getLeavesTaken().toString(),
            "Withholding Tax:", formatCurrencyForUI(payslip.getWithholdingTax())));
        
        // Show overtime if applicable
        if (payslip.getOvertimeHours().doubleValue() > 0) {
            payslipText.append(String.format("%-20s %14s\n", 
                "Overtime Hours:", payslip.getOvertimeHours().toString()));
        }
        
        payslipText.append("\n");
        payslipText.append(String.format("%-20s %14s %20s %14s\n", 
            "GROSS INCOME:", formatCurrencyForUI(payslip.getGrossIncome()),
            "TOTAL DEDUCTIONS:", formatCurrencyForUI(payslip.getTotalDeductions())));
        payslipText.append("\n");
        
        // Benefits section
        payslipText.append("BENEFITS\n");
        payslipText.append("=".repeat(70)).append("\n");
        payslipText.append(String.format("%-40s %15s\n", "Rice Subsidy:", formatCurrencyForUI(payslip.getRiceSubsidy())));
        payslipText.append(String.format("%-40s %15s\n", "Phone Allowance:", formatCurrencyForUI(payslip.getPhoneAllowance())));
        payslipText.append(String.format("%-40s %15s\n", "Clothing Allowance:", formatCurrencyForUI(payslip.getClothingAllowance())));
        payslipText.append(String.format("%-40s %15s\n", "TOTAL BENEFITS:", formatCurrencyForUI(payslip.getTotalBenefits())));
        payslipText.append("\n");
        
        // Net Pay - prominently displayed
        payslipText.append("=".repeat(70)).append("\n");
        payslipText.append(String.format("%25s %-20s %15s\n", "", "NET PAY:", formatCurrencyForUI(payslip.getNetPay())));
        payslipText.append("=".repeat(70)).append("\n\n");
        
        // Footer
        payslipText.append("THIS IS A SYSTEM GENERATED PAYSLIP AND DOES NOT REQUIRE A SIGNATURE.\n");
        
        // Display in text area
        jTextArea1.setText(payslipText.toString());
        jTextArea1.setCaretPosition(0); // Scroll to top
    }
    
    /**
     * Formats currency for UI display
     */
    private String formatCurrencyForUI(java.math.BigDecimal amount) {
        if (amount == null) {
            return "₱0.00";
        }
        return String.format("₱%,.2f", amount);
    }
    
    /**
     * Shows welcome message
     */
    private void showWelcomeMessage() {
        jTextArea1.setText("Select a pay period from the dropdown, then click 'View Payslip' to display your payslip.\n\n" +
                          "Click 'Download Payslip' to save a professional PDF copy.\n\n" +
                          "Available periods: June 2024 - December 2024\n\n" +
                          "PDF template: MotorPH Employee Payslip.jrxml\n" +
                          "Logo: src/media/OG Logo _ 100X124.png");
    }
    
    /**
     * Shows loading message
     */
    private void showLoadingMessage(String operation, YearMonth period) {
        jTextArea1.setText(operation + " for " + period.format(DateTimeFormatter.ofPattern("MMMM yyyy")) + "...\n" +
                          "Please wait...");
    }
    
    /**
     * Returns to appropriate dashboard based on user role
     */
    private void navigateBackToDashboard() {
        try {
            if (loggedInUser == null) {
                System.out.println("No user logged in, navigating to Login screen");
                new Login().setVisible(true);
                this.dispose();
                return;
            }
            
            String role = loggedInUser.getUserRole();
            System.out.println("Navigating back to " + role + " dashboard");
            
            // Use same navigation logic as AboutPage
            String upperRole = role.toUpperCase();
            
            if (upperRole.contains("HR")) {
                new AdminHR(loggedInUser).setVisible(true);
            } 
            else if (upperRole.contains("ACCOUNTING")) {
                new AdminAccounting(loggedInUser).setVisible(true);
            } 
            else if (upperRole.contains("IT")) {
                new AdminIT(loggedInUser).setVisible(true);
            }
            else if (upperRole.contains("IMMEDIATE SUPERVISOR") || 
                     upperRole.contains("SUPERVISOR") || 
                     upperRole.contains("MANAGER")) {
                new AdminSupervisor(loggedInUser).setVisible(true);
            }
            else if (upperRole.contains("EMPLOYEE")) {
                new EmployeeSelfService(loggedInUser).setVisible(true);
            }
            else {
                // Unknown role, go to login
                showWarningDialog("Unknown user role: " + role + "\nRedirecting to login.", "Role Error");
                new Login().setVisible(true);
            }
            
            this.dispose(); // Close this window
            
        } catch (Exception e) {
            System.err.println("Error navigating back: " + e.getMessage());
            e.printStackTrace();
            showErrorDialog("Error navigating back: " + e.getMessage() + "\nReturning to login screen.", 
                          "Navigation Error");
            
            // Fall back to login screen on error
            new Login().setVisible(true);
            this.dispose();
        }
    }
    
    /**
     * Shows error dialog
     */
    private void showErrorDialog(String message, String title) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
    }
    
    /**
     * Shows warning dialog
     */
    private void showWarningDialog(String message, String title) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.WARNING_MESSAGE);
    }
    
    /**
     * Shows success dialog
     */
    private void showSuccessDialog(String message, String title) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.INFORMATION_MESSAGE);
    }
    
    // EVENT HANDLERS - Delegate to services, handle UI updates
    
    /**
     * Generates and displays payslip
     * Delegates business logic to ReportService, handles UI updates
     */
    private void handleViewPayslipRequest() {
        try {
            // UI Validation
            if (loggedInUser == null) {
                showWarningDialog("No user logged in. Please log in to view payslip.", "Authentication Required");
                return;
            }
            
            YearMonth selectedYearMonth = getSelectedYearMonth(); // UI input parsing
            if (selectedYearMonth == null) {
                showWarningDialog("Please select a month and year from the dropdown.", "Selection Required");
                return;
            }
            
            System.out.println("Generating payslip for Employee ID: " + loggedInUser.getEmployeeId() + 
                              ", Period: " + selectedYearMonth);
            
            // Show loading message (UI)
            showLoadingMessage("Generating payslip", selectedYearMonth);
            
            // DELEGATE TO SERVICE - This is where business logic happens
            currentPayslipReport = reportService.generateEmployeePayslipFromView(
                loggedInUser.getEmployeeId(), selectedYearMonth);
            
            // Handle service response (UI logic)
            if (currentPayslipReport.isSuccess() && currentPayslipReport.getPayslip() != null) {
                displayPayslipInUI(currentPayslipReport.getPayslip()); // UI display
                System.out.println("Payslip generated successfully");
            } else {
                String errorMsg = currentPayslipReport.getErrorMessage();
                String userFriendlyError = "No payslip data found for the selected period.\n\n" +
                                         "This could mean:\n" +
                                         "• No attendance records for " + selectedYearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")) + "\n" +
                                         "• Payroll has not been processed yet\n" +
                                         "• Selected period is outside your employment dates\n\n" +
                                         "Error details: " + (errorMsg != null ? errorMsg : "Unknown error");
                jTextArea1.setText(userFriendlyError); // UI error display
                System.err.println("No payslip data found: " + errorMsg);
            }
            
        } catch (Exception e) {
            System.err.println("Error generating payslip: " + e.getMessage());
            e.printStackTrace();
            showErrorDialog("Error generating payslip: " + e.getMessage(), "Error");
            
            jTextArea1.setText("Error generating payslip:\n\n" + e.getMessage() + 
                              "\n\nPlease try again or contact system administrator.");
        }
    }
    
/**
 * Downloads payslip as PDF using Pure Java PDF Generator
 * NO MORE JASPERREPORTS DEPENDENCIES!
 */
private void handleDownloadPayslipRequest() {
    try {
        // UI Validation
        if (loggedInUser == null) {
            showWarningDialog("No user logged in. Please log in to download payslip.", "Authentication Required");
            return;
        }

        YearMonth selectedYearMonth = getSelectedYearMonth();
        if (selectedYearMonth == null) {
            showWarningDialog("Please select a month and year from the dropdown.", "Selection Required");
            return;
        }

        System.out.println("Downloading payslip PDF for Employee ID: " + loggedInUser.getEmployeeId() + 
                          ", Period: " + selectedYearMonth);

        // Create default filename
        String defaultFileName = String.format("MotorPH_Payslip_%s_%s_%s.pdf", 
            loggedInUser.getLastName().replaceAll("\\s+", ""), 
            loggedInUser.getEmployeeId(),
            selectedYearMonth.format(DateTimeFormatter.ofPattern("yyyy-MM")));

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
            System.out.println("User cancelled payslip download");
            return;
        }

        // Get chosen file path
        File selectedFile = fileChooser.getSelectedFile();
        String userChosenPath = selectedFile.getAbsolutePath();
        if (!userChosenPath.toLowerCase().endsWith(".pdf")) {
            userChosenPath += ".pdf";
        }

        // Update UI
        String currentText = jTextArea1.getText();
        jTextArea1.setText(currentText + "\n\nGenerating PDF payslip...\nPlease wait...");

        // STEP 1: Get payslip data from ReportService
        var payslipReport = reportService.generateEmployeePayslipFromView(
            loggedInUser.getEmployeeId(), selectedYearMonth);

        if (!payslipReport.isSuccess() || payslipReport.getPayslip() == null) {
            throw new Exception("Failed to retrieve payslip data: " + payslipReport.getErrorMessage());
        }

        // STEP 2: Generate PDF using Pure Java Generator (NO JASPERREPORTS)
        boolean success = PurePDFPayslipGenerator.generatePayslip(
            payslipReport.getPayslip(), 
            userChosenPath
        );

        if (success) {
            // Success! Update UI
            jTextArea1.setText(currentText + "\n\nPayslip PDF successfully generated!\n" +
                             "Filename: " + new File(userChosenPath).getName() + "\n" +
                             "Saved to: " + userChosenPath);

            // Show success dialog with option to open file
            int openChoice = JOptionPane.showConfirmDialog(this,
                "Payslip successfully downloaded!\n" +
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
                    showWarningDialog("File saved successfully but couldn't open it automatically.\n" +
                                    "Please navigate to: " + userChosenPath, 
                                    "File Saved");
                }
            }

            System.out.println("PDF payslip generated successfully: " + userChosenPath);

        } else {
            throw new Exception("PDF generation failed - check console for details");
        }

    } catch (Exception e) {
        System.err.println("Error downloading payslip: " + e.getMessage());
        e.printStackTrace();

        showErrorDialog("Error downloading payslip: " + e.getMessage(), "Download Error");

        // Update UI with error message
        jTextArea1.setText("Error downloading payslip PDF:\n\n" + e.getMessage() + 
                          "\n\nUsing Pure Java PDF Generator (no JasperReports dependencies)");
    }
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
        backpayslipbttn = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        payslipempid = new javax.swing.JLabel();
        payslipfirstnm = new javax.swing.JLabel();
        paysliplastnm = new javax.swing.JLabel();
        paysliposition = new javax.swing.JLabel();
        inputpayslipempid = new javax.swing.JLabel();
        inputpayslipfirstnm = new javax.swing.JLabel();
        inputpaysliplastnm = new javax.swing.JLabel();
        inputpayslipposition = new javax.swing.JLabel();
        payslipselectmonth = new javax.swing.JLabel();
        monthcombo = new javax.swing.JComboBox<>();
        viewpayslipbtn = new javax.swing.JButton();
        downloadpayslipbtn = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel1.setBackground(new java.awt.Color(220, 95, 0));
        jPanel1.setPreferredSize(new java.awt.Dimension(211, 57));

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(255, 255, 255));
        jLabel1.setText("PAYSLIP PAGE");

        backpayslipbttn.setBackground(new java.awt.Color(207, 10, 10));
        backpayslipbttn.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        backpayslipbttn.setForeground(new java.awt.Color(255, 255, 255));
        backpayslipbttn.setText("Back");
        backpayslipbttn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                backpayslipbttnActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(25, 25, 25)
                .addComponent(backpayslipbttn)
                .addGap(39, 39, 39)
                .addComponent(jLabel1)
                .addContainerGap(483, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(backpayslipbttn))
                .addContainerGap(13, Short.MAX_VALUE))
        );

        jPanel2.setBackground(new java.awt.Color(255, 255, 255));

        payslipempid.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        payslipempid.setText("Employee ID:");

        payslipfirstnm.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        payslipfirstnm.setText("First Name:");

        paysliplastnm.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        paysliplastnm.setText("Last Name:");

        paysliposition.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        paysliposition.setText("Position:");

        inputpayslipempid.setText(". . .");

        inputpayslipfirstnm.setText(". . .");

        inputpaysliplastnm.setText(". . .");

        inputpayslipposition.setText(". . .");

        payslipselectmonth.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        payslipselectmonth.setText("Select Date:");

        monthcombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                monthcomboActionPerformed(evt);
            }
        });

        viewpayslipbtn.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        viewpayslipbtn.setText("View Payslip");
        viewpayslipbtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                viewpayslipbtnActionPerformed(evt);
            }
        });

        downloadpayslipbtn.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        downloadpayslipbtn.setText("Download Payslip");
        downloadpayslipbtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                downloadpayslipbtnActionPerformed(evt);
            }
        });

        jTextArea1.setColumns(20);
        jTextArea1.setRows(5);
        jScrollPane1.setViewportView(jTextArea1);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(48, 48, 48)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(viewpayslipbtn, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(payslipempid)
                            .addComponent(payslipfirstnm)
                            .addComponent(paysliplastnm)
                            .addComponent(paysliposition))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(inputpayslipempid, javax.swing.GroupLayout.DEFAULT_SIZE, 105, Short.MAX_VALUE)
                            .addComponent(inputpayslipfirstnm, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(inputpaysliplastnm, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(inputpayslipposition, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addComponent(downloadpayslipbtn, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(payslipselectmonth, javax.swing.GroupLayout.PREFERRED_SIZE, 81, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(monthcombo, javax.swing.GroupLayout.PREFERRED_SIZE, 127, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(18, 18, 18)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 423, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(34, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(37, 37, 37)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(payslipempid)
                    .addComponent(inputpayslipempid))
                .addGap(18, 18, 18)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(payslipfirstnm)
                    .addComponent(inputpayslipfirstnm))
                .addGap(18, 18, 18)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(paysliplastnm)
                    .addComponent(inputpaysliplastnm))
                .addGap(18, 18, 18)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(paysliposition)
                    .addComponent(inputpayslipposition))
                .addGap(89, 89, 89)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(payslipselectmonth)
                    .addComponent(monthcombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(56, 56, 56)
                .addComponent(viewpayslipbtn)
                .addGap(18, 18, 18)
                .addComponent(downloadpayslipbtn)
                .addContainerGap(49, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 400, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 749, Short.MAX_VALUE)
            .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void viewpayslipbtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_viewpayslipbtnActionPerformed
        // UI responsiveness - disable button and show busy cursor
        viewpayslipbtn.setEnabled(false);
        setCursor(new Cursor(Cursor.WAIT_CURSOR));
        
        // Execute in background to avoid UI freezing
        SwingUtilities.invokeLater(() -> {
            try {
                handleViewPayslipRequest(); // Delegate to event handler
            } finally {
                // Restore UI state
                viewpayslipbtn.setEnabled(true);
                setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }
        });
    }//GEN-LAST:event_viewpayslipbtnActionPerformed

    private void downloadpayslipbtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_downloadpayslipbtnActionPerformed
       // UI responsiveness - disable button and show busy cursor
        downloadpayslipbtn.setEnabled(false);
        setCursor(new Cursor(Cursor.WAIT_CURSOR));
        
        // Execute in background to avoid UI freezing
        SwingUtilities.invokeLater(() -> {
            try {
                handleDownloadPayslipRequest(); // Delegate to event handler
            } finally {
                // Restore UI state
                downloadpayslipbtn.setEnabled(true);
                setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }
        });
    }//GEN-LAST:event_downloadpayslipbtnActionPerformed

    private void monthcomboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_monthcomboActionPerformed
        // Clear any existing payslip data when month changes
        if (jTextArea1.getText().contains("NET PAY:")) {
            jTextArea1.setText("Period changed. Click 'View Payslip' to generate payslip for the selected period.");
        }
    }//GEN-LAST:event_monthcomboActionPerformed

    private void backpayslipbttnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_backpayslipbttnActionPerformed
        navigateBackToDashboard(); // UI navigation
    }//GEN-LAST:event_backpayslipbttnActionPerformed

  /**
     * @param args the command line arguments
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
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(ViewPayslip.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        
        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> new ViewPayslip().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton backpayslipbttn;
    private javax.swing.JButton downloadpayslipbtn;
    private javax.swing.JLabel inputpayslipempid;
    private javax.swing.JLabel inputpayslipfirstnm;
    private javax.swing.JLabel inputpaysliplastnm;
    private javax.swing.JLabel inputpayslipposition;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JComboBox<String> monthcombo;
    private javax.swing.JLabel payslipempid;
    private javax.swing.JLabel payslipfirstnm;
    private javax.swing.JLabel paysliplastnm;
    private javax.swing.JLabel paysliposition;
    private javax.swing.JLabel payslipselectmonth;
    private javax.swing.JButton viewpayslipbtn;
    // End of variables declaration//GEN-END:variables
}
