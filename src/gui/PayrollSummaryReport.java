package gui;

import Models.UserAuthenticationModel;
import DAOs.DatabaseConnection;
import Services.ReportService;
import Services.PurePDFPayrollSummaryGenerator;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.Cursor;
import java.awt.Desktop;
import java.util.List;
import java.util.ArrayList;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.sql.*;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Enhanced Payroll Summary Report GUI Class
 * Generates monthly payroll summary reports with enhanced PDF output
 * Matches MotorPH corporate branding and layout standards
 * @author MotorPH System - Enhanced Edition
 */
public class PayrollSummaryReport extends javax.swing.JFrame {
    
    // User session information
    private final UserAuthenticationModel loggedInUser;
    private final String userRole;
    
    // Service instances for business logic
    private final DatabaseConnection databaseConnection;
    private final ReportService reportService;
    
    // Current report data
    private ReportService.MonthlyPayrollSummaryReport currentReportData;
    
    // Date formatting
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    /**
     * Constructor with logged in user
     * @param loggedInUser The currently logged in user
     */
    public PayrollSummaryReport(UserAuthenticationModel loggedInUser) {
        this.loggedInUser = loggedInUser;
        this.userRole = (loggedInUser != null) ? loggedInUser.getUserRole() : "HR";
        
        // Initialize services
        this.databaseConnection = new DatabaseConnection();
        this.reportService = new ReportService(databaseConnection);
        
        initializeGUI();
        
        System.out.println("Enhanced PayrollSummaryReport initialized for user: " + 
            (loggedInUser != null ? loggedInUser.getDisplayName() : "Unknown"));
    }
    
    /**
     * Default constructor for testing
     */
    public PayrollSummaryReport() {
        this.loggedInUser = null;
        this.userRole = "HR";
        
        // Initialize services
        this.databaseConnection = new DatabaseConnection();
        this.reportService = new ReportService(databaseConnection);
        
        initializeGUI();
        
        System.out.println("Enhanced PayrollSummaryReport initialized without user (testing mode)");
    }
    
    /**
     * Initialize GUI components and setup
     */
    private void initializeGUI() {
        initComponents(); // Initialize Swing components
        setupUIComponents(); // Additional UI setup
        loadInitialData(); // Load dropdown data
    }
    
    /**
     * Sets up UI components with enhanced styling
     */
    private void setupUIComponents() {
        try {
            // Center the form on screen
            this.setLocationRelativeTo(null);
            
            // Set enhanced form title
            this.setTitle("MotorPH - Monthly Payroll Summary Report Generator");
            
            // Setup enhanced table
            setupEnhancedPayrollTable();
            
            // Set initial UI state
            updateUIState();
            
            System.out.println("Enhanced UI components setup completed successfully");
        } catch (Exception e) {
            System.err.println("Error during enhanced UI setup: " + e.getMessage());
            e.printStackTrace();
            showErrorDialog("Error initializing enhanced UI: " + e.getMessage(), "UI Error");
        }
    }
    
    /**
     * Setup the enhanced payroll summary table matching PDF format
     */
    private void setupEnhancedPayrollTable() {
        // Enhanced column names matching the PDF format
        String[] columnNames = {
            "Emp ID", "Employee Name", "Position", "Department", 
            "Base Salary", "Leaves", "Overtime", "Gross Income",
            "Total Benefits", "SSS", "PhilHealth", "Pag-Ibig", "Tax", "Net Pay"
        };

        DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table read-only
            }
            
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                // Define column classes for better rendering
                if (columnIndex == 0) return Integer.class; // Emp ID
                if (columnIndex >= 4) return String.class; // Currency columns
                return String.class; // Text columns
            }
        };

        jTable1.setModel(tableModel);
        jTable1.getTableHeader().setReorderingAllowed(false);

        // ENABLE HORIZONTAL SCROLLING for wide table
        jTable1.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        // Set optimized column widths matching PDF proportions
        TableColumnModel columnModel = jTable1.getColumnModel();
        if (columnModel.getColumnCount() > 0) {
            columnModel.getColumn(0).setPreferredWidth(60);   // Emp ID
            columnModel.getColumn(1).setPreferredWidth(150);  // Employee Name
            columnModel.getColumn(2).setPreferredWidth(180);  // Position
            columnModel.getColumn(3).setPreferredWidth(120);  // Department
            columnModel.getColumn(4).setPreferredWidth(90);   // Base Salary
            columnModel.getColumn(5).setPreferredWidth(70);   // Leaves
            columnModel.getColumn(6).setPreferredWidth(70);   // Overtime
            columnModel.getColumn(7).setPreferredWidth(100);  // Gross Income
            columnModel.getColumn(8).setPreferredWidth(90);   // Total Benefits
            columnModel.getColumn(9).setPreferredWidth(70);   // SSS
            columnModel.getColumn(10).setPreferredWidth(80);  // PhilHealth
            columnModel.getColumn(11).setPreferredWidth(70);  // Pag-Ibig
            columnModel.getColumn(12).setPreferredWidth(70);  // With. Tax
            columnModel.getColumn(13).setPreferredWidth(100); // Net Pay
        }

        // Enhanced scroll pane settings
        jScrollPane1.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        jScrollPane1.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        
        // Enhanced table appearance
        jTable1.setRowHeight(22);
        jTable1.setShowGrid(true);
        jTable1.setGridColor(java.awt.Color.LIGHT_GRAY);
        jTable1.getTableHeader().setBackground(new java.awt.Color(52, 73, 94));
        jTable1.getTableHeader().setForeground(java.awt.Color.WHITE);
        jTable1.getTableHeader().setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 11));
    }
    
    /**
     * Load initial data for dropdowns with enhanced error handling
     */
    private void loadInitialData() {
        try {
            // Load departments from database
            List<String> departments = getAvailableDepartments();
            selectDepJComboBox1.removeAllItems();
            for (String department : departments) {
                selectDepJComboBox1.addItem(department);
            }
            
            // Load pay periods with enhanced chronological order
            selectDateJComboBox2.removeAllItems();
            selectDateJComboBox2.addItem("Select");
            String[] chronologicalPeriods = {
                "2024-06", "2024-07", "2024-08", "2024-09", 
                "2024-10", "2024-11", "2024-12", "2025-01"
            };
            for (String period : chronologicalPeriods) {
                selectDateJComboBox2.addItem(period);
            }
            
            System.out.println("Enhanced initial data loaded: " + departments.size() + " departments");
            
        } catch (Exception e) {
            System.err.println("Error loading enhanced initial data: " + e.getMessage());
            e.printStackTrace();
            loadFallbackData();
        }
    }
    
    /**
     * Get available departments from database with enhanced query
     */
    private List<String> getAvailableDepartments() {
        List<String> departments = new ArrayList<>();
        departments.add("All");
        
        String sql = "SELECT DISTINCT department FROM position WHERE department IS NOT NULL ORDER BY department";
        
        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                String dept = rs.getString("department");
                if (dept != null && !dept.trim().isEmpty()) {
                    departments.add(dept);
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error loading departments: " + e.getMessage());
        }
        
        return departments;
    }
    
    /**
     * Load fallback data if database query fails
     */
    private void loadFallbackData() {
        // Enhanced fallback departments
        selectDepJComboBox1.removeAllItems();
        String[] fallbackDepartments = {
            "All", "Leadership", "IT", "HR", "Accounting", 
            "Accounts", "Sales & Marketing", "Supply Chain & Logistics", "Customer Service"
        };
        for (String dept : fallbackDepartments) {
            selectDepJComboBox1.addItem(dept);
        }
        
        // Enhanced fallback periods
        selectDateJComboBox2.removeAllItems();
        String[] fallbackPeriods = {
            "Select", "2024-06", "2024-07", "2024-08", "2024-09", 
            "2024-10", "2024-11", "2024-12", "2025-01"
        };
        for (String period : fallbackPeriods) {
            selectDateJComboBox2.addItem(period);
        }
        
        System.out.println("Enhanced fallback data loaded");
    }
    
    /**
     * Update UI state based on current data
     */
    private void updateUIState() {
        boolean hasData = (currentReportData != null && currentReportData.isSuccess());
        viewGeneratedReportsHistoryBtn.setEnabled(true);
        
        // Update generate button text
        if (hasData) {
            generatePayrollSummaryBtn.setText("Regenerate Report");
        } else {
            generatePayrollSummaryBtn.setText("Generate Payroll Summary");
        }
    }
    
    /**
     * Enhanced payroll summary generation handler
     */
    private void handleGeneratePayrollSummary() {
        try {
            // Validate inputs
            String selectedPeriod = (String) selectDateJComboBox2.getSelectedItem();
            String selectedDepartment = (String) selectDepJComboBox1.getSelectedItem();

            if (selectedPeriod == null || selectedPeriod.equals("Select")) {
                showWarningDialog("Please select a pay period from the dropdown.", "Selection Required");
                return;
            }

            if (selectedDepartment == null) {
                selectedDepartment = "All";
            }

            final String finalSelectedPeriod = selectedPeriod;
            final String finalSelectedDepartment = selectedDepartment;

            System.out.println("Generating enhanced payroll summary - Period: " + finalSelectedPeriod + ", Department: " + finalSelectedDepartment);

            // Show enhanced loading state
            setLoadingState(true);

            // Execute in background thread
            SwingUtilities.invokeLater(() -> {
                try {
                    // Parse YearMonth from selected period
                    YearMonth yearMonth = YearMonth.parse(finalSelectedPeriod);

                    // Generate report data using ReportService
                    currentReportData = reportService.generateMonthlyPayrollSummaryFromView(yearMonth);

                    if (currentReportData.isSuccess()) {
                        // Filter by department if specified
                        if (!"All".equals(finalSelectedDepartment)) {
                            filterReportByDepartment(finalSelectedDepartment);
                        }

                        // Update table with enhanced data display
                        updateEnhancedTableWithData(currentReportData);

                        // Show enhanced success message
                        showSuccessDialog(
                            "Payroll Summary Generated Successfully!\n\n" +
                            "Records found: " + currentReportData.getPayrollEntries().size() + " employees\n" +
                            "Period: " + finalSelectedPeriod + "\n" +
                            "Department: " + finalSelectedDepartment + "\n\n" +
                            "Click 'View Generated Reports History' then 'Download PDF' to create your report.", 
                            "Report Generated"
                        );

                    } else {
                        // Show enhanced error message
                        String errorMsg = currentReportData.getErrorMessage();
                        showErrorDialog(
                            "No Payroll Data Found\n\n" +
                            "Period: " + finalSelectedPeriod + "\n" +
                            "Department: " + finalSelectedDepartment + "\n\n" +
                            "Possible reasons:\n" +
                            "• No payroll processed for this period\n" +
                            "• No employees in selected department\n" +
                            "• Data not available in system\n\n" +
                            "Error details: " + (errorMsg != null ? errorMsg : "Unknown error"),
                            "No Data Found"
                        );
                    }

                } catch (Exception e) {
                    System.err.println("Error generating enhanced payroll summary: " + e.getMessage());
                    e.printStackTrace();
                    showErrorDialog("Error generating payroll summary:\n" + e.getMessage(), "Generation Error");
                } finally {
                    setLoadingState(false);
                    updateUIState();
                }
            });

        } catch (Exception e) {
            System.err.println("Error in handleGeneratePayrollSummary: " + e.getMessage());
            e.printStackTrace();
            showErrorDialog("Unexpected error: " + e.getMessage(), "Error");
            setLoadingState(false);
        }
    }
    
    /**
     * Filter report data by department
     */
    private void filterReportByDepartment(String department) {
        if (currentReportData != null && currentReportData.getPayrollEntries() != null) {
            List<ReportService.PayrollSummaryEntry> filteredEntries = currentReportData.getPayrollEntries()
                .stream()
                .filter(entry -> department.equals(entry.getDepartment()))
                .collect(java.util.stream.Collectors.toList());
            
            currentReportData.setPayrollEntries(filteredEntries);
            currentReportData.setTotalEmployees(filteredEntries.size());
            
            System.out.println("Filtered to " + filteredEntries.size() + " employees in " + department + " department");
        }
    }
    
    /**
     * Update table with enhanced data display matching PDF format
     */
    private void updateEnhancedTableWithData(ReportService.MonthlyPayrollSummaryReport reportData) {
        DefaultTableModel tableModel = (DefaultTableModel) jTable1.getModel();
        tableModel.setRowCount(0); // Clear existing data
        
        for (ReportService.PayrollSummaryEntry record : reportData.getPayrollEntries()) {
            Object[] row = {
                record.getEmployeeId(),
                record.getEmployeeName(),
                record.getPosition(),
                record.getDepartment(),
                formatCurrency(record.getBaseSalary()),
                formatCurrency(record.getLeaves()),
                formatCurrency(record.getOvertime()),
                formatCurrency(record.getGrossIncome()),
                formatCurrency(record.getTotalBenefits()),
                formatCurrency(record.getSssContribution()),
                formatCurrency(record.getPhilhealthContribution()),
                formatCurrency(record.getPagibigContribution()),
                formatCurrency(record.getWithholdingTax()),
                formatCurrency(record.getNetPay())
            };
            tableModel.addRow(row);
        }
        
        System.out.println("Enhanced table updated with " + reportData.getPayrollEntries().size() + " records");
    }
    
    /**
     * Format currency for enhanced display
     */
    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "0.00";
        return String.format("%,.2f", amount);
    }
    
    /**
     * Enhanced PDF download handler with professional formatting
     */
    private void handleDownloadEnhancedPayrollSummaryPDF() {
        if (currentReportData == null || !currentReportData.isSuccess()) {
            showWarningDialog("Please generate a payroll summary report first.", "No Data Available");
            return;
        }

        try {
            String selectedPeriod = (String) selectDateJComboBox2.getSelectedItem();
            String selectedDepartment = (String) selectDepJComboBox1.getSelectedItem();

            if (selectedPeriod == null || selectedPeriod.equals("Select")) {
                showWarningDialog("Please select a valid pay period.", "Invalid Period");
                return;
            }

            if (selectedDepartment == null) {
                selectedDepartment = "All";
            }

            System.out.println("Generating PDF for period: " + selectedPeriod + ", department: " + selectedDepartment);

            // Determine who generated the report
            String generatedBy = "System";
            Integer generatedByEmployeeId = null;

            if (loggedInUser != null) {
                generatedBy = loggedInUser.getFirstName() + " " + loggedInUser.getLastName();
                generatedByEmployeeId = loggedInUser.getEmployeeId();
            }

            // Create enhanced filename
            String baseFileName = String.format("MotorPH_Payroll_Summary_%s_%s.pdf", 
                selectedPeriod, selectedDepartment.replaceAll("[^a-zA-Z0-9]", ""));
            String basePath = System.getProperty("user.home") + "\\Downloads\\" + baseFileName;

            // Generate unique file path
            String uniqueFilePath = PurePDFPayrollSummaryGenerator.generateUniqueFilePath(basePath);
            String uniqueFileName = new File(uniqueFilePath).getName();

            // Generate the enhanced PDF
            boolean pdfGenerated = PurePDFPayrollSummaryGenerator.generatePayrollSummaryPDF(
                currentReportData, 
                selectedPeriod, 
                selectedDepartment, 
                generatedBy,
                uniqueFilePath
            );

            if (!pdfGenerated) {
                showErrorDialog("Failed to generate PDF report.", "PDF Generation Error");
                return;
            }

            // Get file size
            File pdfFile = new File(uniqueFilePath);
            long fileSize = pdfFile.exists() ? pdfFile.length() : 0;

            // Save to database with enhanced tracking
            boolean dbSaved = saveEnhancedReportToDatabase(
                selectedPeriod, 
                selectedDepartment, 
                uniqueFileName, 
                uniqueFilePath,
                fileSize,
                currentReportData.getPayrollEntries().size(),
                generatedBy,
                generatedByEmployeeId
            );

            if (dbSaved) {
                // Show enhanced success message with file details
                String message = String.format(
                    "PDF Payroll Summary Generated Successfully!\n\n" +
                    "File: %s\n" +
                    "Location: %s\n" +
                    "Generated by: %s\n" +
                    "Records: %d employees\n" +
                    "File size: %s\n\n" +
                    "The report has been saved to your Downloads folder and recorded in the database.\n\n" +
                    "Would you like to open the PDF now?",
                    uniqueFileName, 
                    uniqueFilePath, 
                    generatedBy, 
                    currentReportData.getPayrollEntries().size(),
                    formatFileSize(fileSize)
                );

                int choice = JOptionPane.showConfirmDialog(this, message, "PDF Generated Successfully", 
                    JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
                
                if (choice == JOptionPane.YES_OPTION) {
                    try {
                        Desktop.getDesktop().open(pdfFile);
                    } catch (IOException e) {
                        showWarningDialog("PDF generated successfully but could not open automatically.\nFile location: " + uniqueFilePath, "Cannot Open PDF");
                    }
                }

                System.out.println("Enhanced PDF payroll summary generated successfully: " + uniqueFilePath);
            } else {
                showWarningDialog("PDF generated successfully but failed to save record to database.\n\nFile location: " + uniqueFilePath, "Partial Success");
            }

        } catch (Exception e) {
            System.err.println("Error generating payroll summary: " + e.getMessage());
            e.printStackTrace();
            showErrorDialog("Error generating payroll summary: " + e.getMessage(), "Generation Error");
        }
    }

    /**
     * Enhanced database save with better tracking
     */
    private boolean saveEnhancedReportToDatabase(String period, String department, String fileName, 
                                                String filePath, long fileSize, int recordCount, 
                                                String generatedBy, Integer employeeId) {
        String insertSQL = """
            INSERT INTO generatedreport 
            (reportTitle, periodStartDate, periodEndDate, employeeId, fileName, filePath, 
             fileSize, fileFormat, recordCount, status, generatedAt) 
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {

            // Parse period to dates
            LocalDate periodStart = LocalDate.parse(period + "-01");
            LocalDate periodEnd = periodStart.withDayOfMonth(periodStart.lengthOfMonth());

            // Create enhanced report title
            String reportTitle = String.format("Payroll Summary - %s (%s) - %s", 
                period, department, generatedBy);

            // Set parameters
            pstmt.setString(1, reportTitle);
            pstmt.setDate(2, Date.valueOf(periodStart));
            pstmt.setDate(3, Date.valueOf(periodEnd));

            if (employeeId != null) {
                pstmt.setInt(4, employeeId);
            } else {
                pstmt.setNull(4, Types.INTEGER);
            }

            pstmt.setString(5, fileName);
            pstmt.setString(6, filePath);
            pstmt.setLong(7, fileSize);
            pstmt.setString(8, "PDF");
            pstmt.setInt(9, recordCount);
            pstmt.setString(10, "Completed");

            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("Enhanced report saved to database with user: " + generatedBy);
                return true;
            } else {
                System.err.println("Failed to save enhanced report: No rows affected");
                return false;
            }

        } catch (SQLException e) {
            System.err.println("Error saving enhanced report to database: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Enhanced reports history viewer with better UI
     */
    private void showEnhancedGeneratedReportsHistory() {
        try {
            // Create enhanced dialog
            JDialog historyDialog = new JDialog(this, "MotorPH - Generated Payroll Reports History", true);
            historyDialog.setSize(1200, 800);
            historyDialog.setLocationRelativeTo(this);
            
            // Enhanced table for reports
            String[] columnNames = {
                "Report Type", "Period", "Department", "Records", "Generated By", 
                "Generated Date", "File Size", "Status", "Actions", "File Path"
            };
            
            DefaultTableModel historyTableModel = new DefaultTableModel(columnNames, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
            
            JTable historyTable = new JTable(historyTableModel);
            historyTable.getTableHeader().setReorderingAllowed(false);
            historyTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            historyTable.setRowHeight(25);
            
            // Enhanced column widths
            TableColumnModel columnModel = historyTable.getColumnModel();
            columnModel.getColumn(0).setPreferredWidth(120); // Report Type
            columnModel.getColumn(1).setPreferredWidth(80);  // Period
            columnModel.getColumn(2).setPreferredWidth(100); // Department
            columnModel.getColumn(3).setPreferredWidth(70);  // Records
            columnModel.getColumn(4).setPreferredWidth(130); // Generated By
            columnModel.getColumn(5).setPreferredWidth(150); // Generated Date
            columnModel.getColumn(6).setPreferredWidth(80);  // File Size
            columnModel.getColumn(7).setPreferredWidth(80);  // Status
            columnModel.getColumn(8).setPreferredWidth(80);  // Actions
            columnModel.getColumn(9).setPreferredWidth(0);   // File Path (hidden)
            columnModel.getColumn(9).setMinWidth(0);
            columnModel.getColumn(9).setMaxWidth(0);
            
            // Load enhanced data
            loadEnhancedReportsHistory(historyTableModel);
            
            // Enhanced scroll pane
            JScrollPane scrollPane = new JScrollPane(historyTable);
            
            // Enhanced buttons panel
            JPanel buttonsPanel = new JPanel();
            JButton refreshButton = new JButton("Refresh");
            JButton openButton = new JButton("Open Selected Report");
            JButton downloadButton = new JButton("Download New PDF");
            JButton closeButton = new JButton("Close");
            
            // Enhanced button styling
            refreshButton.setBackground(new java.awt.Color(52, 152, 219));
            refreshButton.setForeground(java.awt.Color.WHITE);
            openButton.setBackground(new java.awt.Color(46, 204, 113));
            openButton.setForeground(java.awt.Color.WHITE);
            downloadButton.setBackground(new java.awt.Color(230, 126, 34));
            downloadButton.setForeground(java.awt.Color.WHITE);
            closeButton.setBackground(new java.awt.Color(231, 76, 60));
            closeButton.setForeground(java.awt.Color.WHITE);
            
            refreshButton.addActionListener(e -> loadEnhancedReportsHistory(historyTableModel));
            
            openButton.addActionListener(e -> {
                int selectedRow = historyTable.getSelectedRow();
                if (selectedRow >= 0) {
                    String filePath = (String) historyTableModel.getValueAt(selectedRow, 9);
                    openReportFile(filePath);
                } else {
                    showWarningDialog("Please select a report from the list.", "Selection Required");
                }
            });
            
            downloadButton.addActionListener(e -> {
                historyDialog.dispose();
                handleDownloadEnhancedPayrollSummaryPDF();
            });
            
            closeButton.addActionListener(e -> historyDialog.dispose());
            
            buttonsPanel.add(refreshButton);
            buttonsPanel.add(openButton);
            buttonsPanel.add(downloadButton);
            buttonsPanel.add(closeButton);
            
            // Enhanced title panel
            JPanel titlePanel = new JPanel();
            titlePanel.setBackground(new java.awt.Color(52, 73, 94));
            JLabel titleLabel = new JLabel("MotorPH Payroll Reports History");
            titleLabel.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 16));
            titleLabel.setForeground(java.awt.Color.WHITE);
            titlePanel.add(titleLabel);
            
            // Layout enhanced dialog
            historyDialog.setLayout(new java.awt.BorderLayout());
            historyDialog.add(titlePanel, java.awt.BorderLayout.NORTH);
            historyDialog.add(scrollPane, java.awt.BorderLayout.CENTER);
            historyDialog.add(buttonsPanel, java.awt.BorderLayout.SOUTH);
            
            historyDialog.setVisible(true);
            
        } catch (Exception e) {
            System.err.println("Error showing enhanced reports history: " + e.getMessage());
            e.printStackTrace();
            showErrorDialog("Error loading reports history: " + e.getMessage(), "Error");
        }
    }
    
    /**
     * Load enhanced reports history from database
     */
    private void loadEnhancedReportsHistory(DefaultTableModel historyTableModel) {
        String selectSQL = """
            SELECT gr.reportId, gr.reportTitle, gr.periodStartDate, gr.periodEndDate, 
                   gr.fileName, gr.filePath, gr.fileSize, gr.fileFormat, gr.recordCount, 
                   gr.status, gr.generatedAt, 
                   COALESCE(CONCAT(e.firstName, ' ', e.lastName), 'System') as generatedByName,
                   gr.employeeId
            FROM generatedreport gr
            LEFT JOIN employee e ON gr.employeeId = e.employeeId
            WHERE gr.status = 'Completed' AND gr.fileFormat = 'PDF'
            ORDER BY gr.generatedAt DESC
            LIMIT 100
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(selectSQL);
             ResultSet rs = pstmt.executeQuery()) {

            historyTableModel.setRowCount(0);
            int recordCount = 0;

            while (rs.next()) {
                String reportType = "Monthly Payroll Summary";
                String fullTitle = rs.getString("reportTitle");
                String period = "Unknown";
                String department = "Unknown";

                // Enhanced parsing of report title
                if (fullTitle != null && fullTitle.contains("Payroll Summary")) {
                    if (fullTitle.contains(" - ")) {
                        String[] parts = fullTitle.split(" - ");
                        if (parts.length >= 2) {
                            String periodPart = parts[1];
                            if (periodPart.matches("\\d{4}-\\d{2}.*")) {
                                period = periodPart.substring(0, 7);
                                if (periodPart.contains("(") && periodPart.contains(")")) {
                                    int start = periodPart.indexOf("(") + 1;
                                    int end = periodPart.indexOf(")", start);
                                    if (end > start) {
                                        department = periodPart.substring(start, end);
                                    }
                                }
                            }
                        }
                    }
                }

                String generatedBy = rs.getString("generatedByName");
                String generatedDate = DATE_FORMAT.format(rs.getTimestamp("generatedAt"));
                String status = rs.getString("status");
                String filePath = rs.getString("filePath");
                long size = rs.getLong("fileSize");
                String formattedSize = formatFileSize(size);

                historyTableModel.addRow(new Object[] {
                    reportType,
                    period,
                    department,
                    rs.getInt("recordCount"),
                    generatedBy,
                    generatedDate,
                    formattedSize,
                    status,
                    "Open",
                    filePath
                });
                recordCount++;
            }

            System.out.println("Loaded " + recordCount + " enhanced report records");

        } catch (SQLException e) {
            System.err.println("Error loading enhanced report history: " + e.getMessage());
            e.printStackTrace();
            historyTableModel.setRowCount(0);
            historyTableModel.addRow(new Object[] {
                "Error", "Failed", "Database error", "0", 
                "System", "N/A", "0 KB", "Failed", "Error", ""
            });
        }
    }
    
    /**
     * Open report file with enhanced error handling
     */
    private void openReportFile(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            showWarningDialog("No file path available for this report.", "No File");
            return;
        }
        
        try {
            File file = new File(filePath);
            if (file.exists()) {
                Desktop.getDesktop().open(file);
                System.out.println("Opened report file: " + filePath);
            } else {
                showWarningDialog("File not found: " + filePath + "\n\nThe file may have been moved or deleted.", "File Not Found");
            }
        } catch (IOException ex) {
            System.err.println("Error opening file: " + ex.getMessage());
            showErrorDialog("Error opening file: " + ex.getMessage(), "Error");
        }
    }
    
    /**
     * Enhanced file size formatting
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
    
    /**
     * Enhanced loading state management
     */
    private void setLoadingState(boolean loading) {
        setCursor(loading ? new Cursor(Cursor.WAIT_CURSOR) : new Cursor(Cursor.DEFAULT_CURSOR));
        generatePayrollSummaryBtn.setEnabled(!loading);
        viewGeneratedReportsHistoryBtn.setEnabled(!loading);
        selectDepJComboBox1.setEnabled(!loading);
        selectDateJComboBox2.setEnabled(!loading);
        
        if (loading) {
            generatePayrollSummaryBtn.setText("Generating Report...");
        } else {
            updateUIState();
        }
    }
    
    /**
     * Enhanced navigation back to dashboard
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
            else {
                showWarningDialog("Unknown user role: " + role + "\nRedirecting to login.", "Role Error");
                new Login().setVisible(true);
            }
            
            this.dispose();
            
        } catch (Exception e) {
            System.err.println("Error navigating back: " + e.getMessage());
            e.printStackTrace();
            showErrorDialog("Error navigating back: " + e.getMessage() + "\nReturning to login screen.", "Navigation Error");
            new Login().setVisible(true);
            this.dispose();
        }
    }
    
    // Enhanced dialog helper methods
    private void showErrorDialog(String message, String title) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
    }
    
    private void showWarningDialog(String message, String title) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.WARNING_MESSAGE);
    }
    
    private void showSuccessDialog(String message, String title) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.INFORMATION_MESSAGE);
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
        generatePayrollSummaryBtn = new javax.swing.JButton();
        viewGeneratedReportsHistoryBtn = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel1.setBackground(new java.awt.Color(220, 95, 0));

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(255, 255, 255));
        jLabel1.setText("Payroll Summary Report");

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
                .addContainerGap(656, Short.MAX_VALUE))
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

        selectDepJComboBox1.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "All", "Leadership", "IT", "HR", "Accounting", "Accounts", "Sales & Marketing", "Supply Chain & Logistics", "Customer Service" }));
        selectDepJComboBox1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectDepJComboBox1ActionPerformed(evt);
            }
        });

        selectDateJComboBox2.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Select", "2024-06", "2024-07", "2024-08", "2024-09", "2024-10", "2024-11", "2024-12" }));
        selectDateJComboBox2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectDateJComboBox2ActionPerformed(evt);
            }
        });

        generatePayrollSummaryBtn.setBackground(new java.awt.Color(220, 95, 0));
        generatePayrollSummaryBtn.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        generatePayrollSummaryBtn.setForeground(new java.awt.Color(255, 255, 255));
        generatePayrollSummaryBtn.setText("Generate Payroll Summary Report");
        generatePayrollSummaryBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                generatePayrollSummaryBtnActionPerformed(evt);
            }
        });

        viewGeneratedReportsHistoryBtn.setBackground(new java.awt.Color(220, 95, 0));
        viewGeneratedReportsHistoryBtn.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        viewGeneratedReportsHistoryBtn.setForeground(new java.awt.Color(255, 255, 255));
        viewGeneratedReportsHistoryBtn.setText("View Generated Reports History");
        viewGeneratedReportsHistoryBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                viewGeneratedReportsHistoryBtnActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(26, 26, 26)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel2)
                            .addComponent(selectDepJComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, 119, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel3)
                            .addComponent(selectDateJComboBox2, javax.swing.GroupLayout.PREFERRED_SIZE, 89, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addGroup(jPanel2Layout.createSequentialGroup()
                            .addComponent(generatePayrollSummaryBtn)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(viewGeneratedReportsHistoryBtn))
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 939, javax.swing.GroupLayout.PREFERRED_SIZE)))
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
                    .addComponent(selectDateJComboBox2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 330, Short.MAX_VALUE)
                .addGap(18, 18, 18)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(generatePayrollSummaryBtn)
                    .addComponent(viewGeneratedReportsHistoryBtn))
                .addGap(40, 40, 40))
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

 
    private void selectDateJComboBox2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selectDateJComboBox2ActionPerformed
      //Selects pay date data to generate into a monthly payroll summary report of all employees
        /* Drop-down choices
        Select
        2024-06
        2024-07
        2024-08
        2024-09
        2024-10
        2024-11
        2024-12
        */
        
         // Clear existing data when date changes
        if (currentReportData != null) {
            DefaultTableModel tableModel = (DefaultTableModel) jTable1.getModel();
            tableModel.setRowCount(0);
            currentReportData = null;
            updateUIState();
        }
    }//GEN-LAST:event_selectDateJComboBox2ActionPerformed

    private void backpyrllmngmntbttnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_backpyrllmngmntbttnActionPerformed
        //Navigates the user back to the dashboard
        navigateBackToDashboard();
    }//GEN-LAST:event_backpyrllmngmntbttnActionPerformed
    
    private void selectDepJComboBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selectDepJComboBox1ActionPerformed
        //Select department to filter the content on the jtable
        // Clear existing data when department changes
        if (currentReportData != null) {
            DefaultTableModel tableModel = (DefaultTableModel) jTable1.getModel();
            tableModel.setRowCount(0);
            currentReportData = null;
            updateUIState();
        }
    }//GEN-LAST:event_selectDepJComboBox1ActionPerformed

   
    private void generatePayrollSummaryBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_generatePayrollSummaryBtnActionPerformed
        //Generates the payroll summary report of the chosen month ; After clicking this, a window will open showing a preview of the payroll template with populated data and then a button to download/save the report to the local computer "Downloads" folder
        handleGeneratePayrollSummary();
    }//GEN-LAST:event_generatePayrollSummaryBtnActionPerformed

    private void viewGeneratedReportsHistoryBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_viewGeneratedReportsHistoryBtnActionPerformed
    // Show options: Download PDF or View History
        if (currentReportData != null && currentReportData.isSuccess()) {
            String[] options = {"Download PDF", "View Reports History", "Cancel"};
            int choice = JOptionPane.showOptionDialog(
                this,
                "Choose an action for the payroll summary report:",
                "MotorPH Report Actions",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
            );
            
            if (choice == 0) {
                // Download Enhanced PDF
                handleDownloadEnhancedPayrollSummaryPDF();
            } else if (choice == 1) {
                // View Enhanced History
                showEnhancedGeneratedReportsHistory();
            }
            // Cancel - do nothing
        } else {
            // No current data, just show enhanced history
            showEnhancedGeneratedReportsHistory();
        }
    }//GEN-LAST:event_viewGeneratedReportsHistoryBtnActionPerformed

  /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(PayrollSummaryReport.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(PayrollSummaryReport.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(PayrollSummaryReport.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(PayrollSummaryReport.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                // Create an Employee object (concrete subclass of User)
               
            }
        });
    }
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton backpyrllmngmntbttn;
    private javax.swing.JButton generatePayrollSummaryBtn;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTable1;
    private javax.swing.JComboBox<String> selectDateJComboBox2;
    private javax.swing.JComboBox<String> selectDepJComboBox1;
    private javax.swing.JButton viewGeneratedReportsHistoryBtn;
    // End of variables declaration//GEN-END:variables
}
