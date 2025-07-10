package gui;

import DAOs.EmployeeDAO;
import DAOs.LeaveRequestDAO;
import DAOs.LeaveTypeDAO;
import DAOs.LeaveBalanceDAO;
import DAOs.DatabaseConnection;
import Models.EmployeeModel;
import Models.LeaveRequestModel;
import Models.LeaveTypeModel;
import Models.LeaveBalance;
import Models.UserAuthenticationModel;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.time.LocalDate;
import java.time.Year;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Date;
import java.util.Calendar;

// JCalendar imports
import com.toedter.calendar.JDateChooser;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * This screen allows employees to submit leave requests
 * Enhanced with database integration, leave balance management, and JDateChooser
 */
public class LeaveRequest extends javax.swing.JFrame {

    // Manila timezone for date operations
    private static final ZoneId MANILA_TIMEZONE = ZoneId.of("Asia/Manila");
    
    // Database components
    private LeaveRequestDAO leaveRequestDAO;
    private LeaveTypeDAO leaveTypeDAO;
    private LeaveBalanceDAO leaveBalanceDAO;
    private EmployeeDAO employeeDAO;
    private DatabaseConnection databaseConnection;
    
    // User authentication
    private UserAuthenticationModel loggedInUser;
    private String employeeId;
    private String employeeName;
    
    // Date formatters
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    
    /**
     * Constructor with logged-in user authentication
     * @param loggedInUser The authenticated user
     */
    public LeaveRequest(UserAuthenticationModel loggedInUser) {
        this.loggedInUser = loggedInUser;
        this.employeeId = String.valueOf(loggedInUser.getEmployeeId());
        this.employeeName = loggedInUser.getFirstName() + " " + loggedInUser.getLastName();
        
        // Initialize database components
        this.leaveRequestDAO = new LeaveRequestDAO();
        this.leaveTypeDAO = new LeaveTypeDAO();
        this.leaveBalanceDAO = new LeaveBalanceDAO();
        this.databaseConnection = new DatabaseConnection();
        this.employeeDAO = new EmployeeDAO(databaseConnection);
        
        initComponents();
        initializeCustomComponents();
        setupEventHandlers();
        
        // Load data from database - call this after initComponents to override NetBeans defaults
        loadEmployeeData();
        loadLeaveTypes(); // This will override the NetBeans combo box model
        loadLeaveRequests();
        
        // Test leave types loading for debugging
        testLeaveTypesLoading();
        
        setLocationRelativeTo(null);
        setTitle("Leave Request - " + employeeName);
    }
    
    /**
     * Initialize custom components (JDateChooser configuration)
     */
    private void initializeCustomComponents() {
        // Configure JDateChooser components
        configureDateChooser(jDateChooserStartDate);
        configureDateChooser(jDateChooserEndDate);
        
        // Set current date for submission date (Manila timezone)
        LocalDate todayManila = LocalDate.now(MANILA_TIMEZONE);
        dateofsubtxtfield.setText(todayManila.format(dateFormatter));
        dateofsubtxtfield.setEditable(false);
        
        // Set minimum date to today (prevent past date selection)
        Date today = Date.from(todayManila.atStartOfDay(MANILA_TIMEZONE).toInstant());
        jDateChooserStartDate.setMinSelectableDate(today);
        jDateChooserEndDate.setMinSelectableDate(today);
        
        // Set default dates to today
        jDateChooserStartDate.setDate(today);
        jDateChooserEndDate.setDate(today);
    }
    
    /**
     * Configure individual JDateChooser
     */
    private void configureDateChooser(JDateChooser dateChooser) {
        // Set date format
        dateChooser.setDateFormatString("MM/dd/yyyy");
        
        // Disable weekends selection
        dateChooser.getJCalendar().getDayChooser().addPropertyChangeListener("day", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                validateDateSelection();
            }
        });
    }
    
    /**
     * Setup event handlers for components
     */
    private void setupEventHandlers() {
        // Date filter change handler
        dateFilter3.addActionListener(e -> filterLeaveRequestsByDate());
        
        // Leave type selection handler for debugging
        typeofleavejcombobox.addActionListener(e -> {
            String selected = (String) typeofleavejcombobox.getSelectedItem();
            System.out.println("Leave type selected: " + selected); // Debug
        });
        
        // Start date change handler
        jDateChooserStartDate.addPropertyChangeListener("date", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                validateDateSelection();
                updateEndDateMinimum();
            }
        });
        
        // End date change handler
        jDateChooserEndDate.addPropertyChangeListener("date", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                validateDateSelection();
            }
        });
    }
    
    /**
     * Update end date minimum based on start date selection
     */
    private void updateEndDateMinimum() {
        Date startDate = jDateChooserStartDate.getDate();
        if (startDate != null) {
            jDateChooserEndDate.setMinSelectableDate(startDate);
            // If end date is before start date, reset it
            Date endDate = jDateChooserEndDate.getDate();
            if (endDate != null && endDate.before(startDate)) {
                jDateChooserEndDate.setDate(startDate);
            }
        }
    }
    
    /**
     * Load employee data into form fields
     */
    private void loadEmployeeData() {
        try {
            EmployeeModel employee = employeeDAO.findById(Integer.parseInt(employeeId));
            if (employee != null) {
                employeeIDtxtfield.setText(employeeId);
                firstNametxtfield.setText(employee.getFirstName());
                lastNametxtfield.setText(employee.getLastName());
                employmentStatustxtfield.setText(employee.getStatus().getValue());
                
                // Get position information
                try {
                    EmployeeDAO.EmployeeWithPosition empWithPos = employeeDAO.getEmployeeWithPosition(Integer.parseInt(employeeId));
                    if (empWithPos != null && empWithPos.getPosition() != null) {
                        positiontxtfield.setText(empWithPos.getPosition().getPosition());
                    }
                } catch (Exception e) {
                    positiontxtfield.setText("Employee");
                    System.err.println("Could not get position info: " + e.getMessage());
                }
                
                // Make fields read-only
                employeeIDtxtfield.setEditable(false);
                firstNametxtfield.setEditable(false);
                lastNametxtfield.setEditable(false);
                positiontxtfield.setEditable(false);
                employmentStatustxtfield.setEditable(false);
            }
        } catch (Exception e) {
            System.err.println("Error loading employee data: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error loading employee data: " + e.getMessage(), 
                                        "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Load leave types into the combo box from database
     */
    private void loadLeaveTypes() {
        try {
            // First, clear existing items
            typeofleavejcombobox.removeAllItems();
            
            // Create new model and add default option
            DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
            model.addElement("Select Leave Type");
            
            // Get all leave types from database
            List<LeaveTypeModel> leaveTypes = leaveTypeDAO.getAllLeaveTypes();
            System.out.println("Database returned " + leaveTypes.size() + " leave types");
            
            boolean foundValidTypes = false;
            for (LeaveTypeModel leaveType : leaveTypes) {
                // Only add Sick Leave and Vacation Leave as per business requirements
                if ("Sick Leave".equals(leaveType.getLeaveTypeName()) || 
                    "Vacation Leave".equals(leaveType.getLeaveTypeName())) {
                    model.addElement(leaveType.getLeaveTypeName());
                    foundValidTypes = true;
                    System.out.println("Added leave type: " + leaveType.getLeaveTypeName());
                }
            }
            
            // If no valid types found in database, use fallback
            if (!foundValidTypes) {
                System.out.println("No valid leave types found in database, using fallback values");
                model.addElement("Sick Leave");
                model.addElement("Vacation Leave");
            }
            
            // Set the new model to the combo box
            typeofleavejcombobox.setModel(model);
            typeofleavejcombobox.setSelectedIndex(0); // Select "Select Leave Type" by default
            
            // Make sure combo box is enabled
            typeofleavejcombobox.setEnabled(true);
            
        } catch (Exception e) {
            System.err.println("Error loading leave types: " + e.getMessage());
            e.printStackTrace();
            
            // Fallback: Create manual model if database completely fails
            System.out.println("Database failed, creating fallback combo box model");
            DefaultComboBoxModel<String> fallbackModel = new DefaultComboBoxModel<>();
            fallbackModel.addElement("Select Leave Type");
            fallbackModel.addElement("Sick Leave");
            fallbackModel.addElement("Vacation Leave");
            typeofleavejcombobox.setModel(fallbackModel);
            typeofleavejcombobox.setEnabled(true);
            
            JOptionPane.showMessageDialog(this, 
                "Could not load leave types from database.\nUsing default values.", 
                "Warning", JOptionPane.WARNING_MESSAGE);
        }
    }
    
    /**
     * Load leave requests into the table with balance information
     */
    private void loadLeaveRequests() {
        try {
            // Get leave requests for employee
            List<LeaveRequestModel> requests = 
                leaveRequestDAO.getLeaveRequestsByEmployee(Integer.parseInt(employeeId), null);
            
            DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
            model.setRowCount(0);
            
            // Get current year for balance lookup
            Year currentYear = Year.now(MANILA_TIMEZONE);
            
            // Get leave balances for current year
            List<LeaveBalance> leaveBalances = leaveBalanceDAO.getLeaveBalancesByEmployee(
                Integer.parseInt(employeeId), currentYear);
            
            for (LeaveRequestModel lr : requests) {
                // Get leave type name
                String leaveTypeName = "Unknown";
                try {
                    LeaveTypeModel leaveType = leaveTypeDAO.getLeaveTypeById(lr.getLeaveTypeId());
                    if (leaveType != null) {
                        leaveTypeName = leaveType.getLeaveTypeName();
                    }
                } catch (Exception e) {
                    System.err.println("Error getting leave type: " + e.getMessage());
                }
                
                // Format dates
                String startDateStr = lr.getLeaveStart() != null ? 
                    lr.getLeaveStart().toLocalDate().format(dateFormatter) : "";
                String endDateStr = lr.getLeaveEnd() != null ? 
                    lr.getLeaveEnd().toLocalDate().format(dateFormatter) : "";
                String createdDateStr = lr.getDateCreated() != null ? 
                    lr.getDateCreated().toLocalDateTime().format(DateTimeFormatter.ofPattern("MM/dd/yyyy")) : "";
                
                // Get remaining balances
                String vlRemaining = "N/A";
                String slRemaining = "N/A";
                
                for (LeaveBalance balance : leaveBalances) {
                    try {
                        LeaveTypeModel leaveType = leaveTypeDAO.getLeaveTypeById(balance.getLeaveTypeId());
                        if (leaveType != null) {
                            if ("Vacation Leave".equals(leaveType.getLeaveTypeName())) {
                                vlRemaining = String.valueOf(balance.getRemainingLeaveDays() != null ? 
                                    balance.getRemainingLeaveDays() : 0);
                            } else if ("Sick Leave".equals(leaveType.getLeaveTypeName())) {
                                slRemaining = String.valueOf(balance.getRemainingLeaveDays() != null ? 
                                    balance.getRemainingLeaveDays() : 0);
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Error getting leave type for balance: " + e.getMessage());
                    }
                }
                
                model.addRow(new Object[]{
                    createdDateStr,
                    employeeId,
                    firstNametxtfield.getText(),
                    lastNametxtfield.getText(),
                    positiontxtfield.getText(),
                    employmentStatustxtfield.getText(),
                    "N/A", // Supervisor (to be implemented)
                    leaveTypeName,
                    lr.getLeaveReason() != null ? lr.getLeaveReason() : "",
                    startDateStr,
                    endDateStr,
                    lr.getLeaveStatusDisplayText(),
                    vlRemaining,
                    slRemaining
                });
            }
            
        } catch (Exception e) {
            System.err.println("Error loading leave requests: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error loading leave requests: " + e.getMessage(), 
                                        "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Filter leave requests by selected date
     */
    private void filterLeaveRequestsByDate() {
        String selectedFilter = (String) dateFilter3.getSelectedItem();
        
        if (selectedFilter == null || selectedFilter.equals("Select Date")) {
            loadLeaveRequests();
            return;
        }
        
        try {
            List<LeaveRequestModel> requests;
            
            if (selectedFilter.length() == 7 && selectedFilter.contains("-")) {
                // Format: YYYY-MM
                String[] parts = selectedFilter.split("-");
                int year = Integer.parseInt(parts[0]);
                int month = Integer.parseInt(parts[1]);
                
                // Create date range for the month
                Calendar startCal = Calendar.getInstance();
                startCal.set(year, month - 1, 1);
                Calendar endCal = Calendar.getInstance();
                endCal.set(year, month - 1, startCal.getActualMaximum(Calendar.DAY_OF_MONTH));
                
                java.sql.Date startDate = new java.sql.Date(startCal.getTimeInMillis());
                java.sql.Date endDate = new java.sql.Date(endCal.getTimeInMillis());
                
                requests = leaveRequestDAO.getLeaveRequestsByEmployeeAndDateRange(
                    Integer.parseInt(employeeId), startDate, endDate);
            } else {
                requests = leaveRequestDAO.getLeaveRequestsByEmployee(Integer.parseInt(employeeId), null);
            }
            
            // Update table with filtered results (similar to loadLeaveRequests but with filtered data)
            DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
            model.setRowCount(0);
            
            Year currentYear = Year.now(MANILA_TIMEZONE);
            List<LeaveBalance> leaveBalances = leaveBalanceDAO.getLeaveBalancesByEmployee(
                Integer.parseInt(employeeId), currentYear);
            
            for (LeaveRequestModel lr : requests) {
                // Same logic as loadLeaveRequests...
                String leaveTypeName = "Unknown";
                try {
                    LeaveTypeModel leaveType = leaveTypeDAO.getLeaveTypeById(lr.getLeaveTypeId());
                    if (leaveType != null) {
                        leaveTypeName = leaveType.getLeaveTypeName();
                    }
                } catch (Exception e) {
                    System.err.println("Error getting leave type: " + e.getMessage());
                }
                
                String startDateStr = lr.getLeaveStart() != null ? 
                    lr.getLeaveStart().toLocalDate().format(dateFormatter) : "";
                String endDateStr = lr.getLeaveEnd() != null ? 
                    lr.getLeaveEnd().toLocalDate().format(dateFormatter) : "";
                String createdDateStr = lr.getDateCreated() != null ? 
                    lr.getDateCreated().toLocalDateTime().format(DateTimeFormatter.ofPattern("MM/dd/yyyy")) : "";
                
                String vlRemaining = "N/A";
                String slRemaining = "N/A";
                
                for (LeaveBalance balance : leaveBalances) {
                    try {
                        LeaveTypeModel leaveType = leaveTypeDAO.getLeaveTypeById(balance.getLeaveTypeId());
                        if (leaveType != null) {
                            if ("Vacation Leave".equals(leaveType.getLeaveTypeName())) {
                                vlRemaining = String.valueOf(balance.getRemainingLeaveDays() != null ? 
                                    balance.getRemainingLeaveDays() : 0);
                            } else if ("Sick Leave".equals(leaveType.getLeaveTypeName())) {
                                slRemaining = String.valueOf(balance.getRemainingLeaveDays() != null ? 
                                    balance.getRemainingLeaveDays() : 0);
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Error getting leave type for balance: " + e.getMessage());
                    }
                }
                
                model.addRow(new Object[]{
                    createdDateStr,
                    employeeId,
                    firstNametxtfield.getText(),
                    lastNametxtfield.getText(),
                    positiontxtfield.getText(),
                    employmentStatustxtfield.getText(),
                    "N/A",
                    leaveTypeName,
                    lr.getLeaveReason() != null ? lr.getLeaveReason() : "",
                    startDateStr,
                    endDateStr,
                    lr.getLeaveStatusDisplayText(),
                    vlRemaining,
                    slRemaining
                });
            }
            
        } catch (Exception e) {
            System.err.println("Error filtering leave requests: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error filtering leave requests: " + e.getMessage(), 
                                        "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Validate the selected date range and check for weekends
     */
    private void validateDateSelection() {
        Date startDate = jDateChooserStartDate.getDate();
        Date endDate = jDateChooserEndDate.getDate();
        
        if (startDate != null && endDate != null) {
            // Check date range
            if (endDate.before(startDate)) {
                JOptionPane.showMessageDialog(this, 
                    "End date cannot be before start date!", 
                    "Invalid Date Range", 
                    JOptionPane.WARNING_MESSAGE);
                jDateChooserEndDate.setDate(startDate);
                return;
            }
            
            // Check for weekends
            if (isWeekend(startDate)) {
                JOptionPane.showMessageDialog(this, 
                    "Start date cannot be on weekend!\nPlease select a weekday (Monday-Friday).", 
                    "Weekend Date Selected", 
                    JOptionPane.WARNING_MESSAGE);
                
                // Find next weekday
                Date nextWeekday = getNextWeekday(startDate);
                jDateChooserStartDate.setDate(nextWeekday);
                return;
            }
            
            if (isWeekend(endDate)) {
                JOptionPane.showMessageDialog(this, 
                    "End date cannot be on weekend!\nPlease select a weekday (Monday-Friday).", 
                    "Weekend Date Selected", 
                    JOptionPane.WARNING_MESSAGE);
                
                // Find previous weekday
                Date prevWeekday = getPreviousWeekday(endDate);
                jDateChooserEndDate.setDate(prevWeekday);
                return;
            }
        }
    }
    
    /**
     * Check if a date falls on weekend
     */
    private boolean isWeekend(Date date) {
        if (date == null) return false;
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        return dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY;
    }
    
    /**
     * Get next weekday from a given date
     */
    private Date getNextWeekday(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        
        while (isWeekend(cal.getTime())) {
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }
        
        return cal.getTime();
    }
    
    /**
     * Get previous weekday from a given date
     */
    private Date getPreviousWeekday(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        
        while (isWeekend(cal.getTime())) {
            cal.add(Calendar.DAY_OF_MONTH, -1);
        }
        
        return cal.getTime();
    }
    
    /**
     * Convert java.util.Date to java.sql.Date
     */
    private java.sql.Date dateToSqlDate(Date date) {
        if (date == null) return null;
        return new java.sql.Date(date.getTime());
    }
    
    /**
     * Calculate working days between two dates (excluding weekends)
     */
    private int calculateWorkingDays(Date startDate, Date endDate) {
        if (startDate == null || endDate == null) return 0;
        
        LocalDate start = startDate.toInstant().atZone(MANILA_TIMEZONE).toLocalDate();
        LocalDate end = endDate.toInstant().atZone(MANILA_TIMEZONE).toLocalDate();
        
        return LeaveTypeModel.calculateWeekdaysInLeave(start, end);
    }
    
    /**
     * Submit leave request with balance validation and updates
     */
    private void submitLeaveRequest() {
        try {
            // Validate form inputs
            if (!validateForm()) {
                return;
            }
            
            // Get selected leave type with better error handling
            String selectedLeaveTypeName = (String) typeofleavejcombobox.getSelectedItem();
            System.out.println("Selected leave type: '" + selectedLeaveTypeName + "'"); // Debug
            
            if (selectedLeaveTypeName == null || selectedLeaveTypeName.equals("Select Leave Type")) {
                JOptionPane.showMessageDialog(this, "Please select a valid leave type!", 
                                            "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            // Find the LeaveTypeModel based on selected name with multiple attempts
            LeaveTypeModel selectedLeaveType = null;
            try {
                // First attempt: exact match
                selectedLeaveType = leaveTypeDAO.getLeaveTypeByName(selectedLeaveTypeName);
                System.out.println("Exact match result: " + (selectedLeaveType != null ? "FOUND" : "NOT FOUND"));
                
                // If not found, try trimming whitespace
                if (selectedLeaveType == null) {
                    selectedLeaveType = leaveTypeDAO.getLeaveTypeByName(selectedLeaveTypeName.trim());
                    System.out.println("Trimmed match result: " + (selectedLeaveType != null ? "FOUND" : "NOT FOUND"));
                }
                
                // If still not found, try finding from all leave types (case-insensitive)
                if (selectedLeaveType == null) {
                    List<LeaveTypeModel> allTypes = leaveTypeDAO.getAllLeaveTypes();
                    for (LeaveTypeModel type : allTypes) {
                        if (type.getLeaveTypeName().equalsIgnoreCase(selectedLeaveTypeName.trim())) {
                            selectedLeaveType = type;
                            System.out.println("Case-insensitive match found: " + type.getLeaveTypeName());
                            break;
                        }
                    }
                }
                
            } catch (Exception e) {
                System.err.println("Error finding leave type: " + e.getMessage());
                e.printStackTrace();
            }
            
            if (selectedLeaveType == null) {
                // Show detailed error with available options
                StringBuilder availableTypes = new StringBuilder("Available leave types:\n");
                try {
                    List<LeaveTypeModel> allTypes = leaveTypeDAO.getAllLeaveTypes();
                    for (LeaveTypeModel type : allTypes) {
                        availableTypes.append("- '").append(type.getLeaveTypeName()).append("'\n");
                    }
                } catch (Exception e) {
                    availableTypes.append("Error loading available types: ").append(e.getMessage());
                }
                
                JOptionPane.showMessageDialog(this, 
                    "Leave type not found in database: '" + selectedLeaveTypeName + "'\n\n" +
                    availableTypes.toString() + "\n" +
                    "Please check if the leave type exists in your database or contact your administrator.", 
                    "Leave Type Not Found", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            System.out.println("Using leave type: ID=" + selectedLeaveType.getLeaveTypeId() + 
                             ", Name='" + selectedLeaveType.getLeaveTypeName() + "'");
            
            // Calculate working days requested
            Date startDate = jDateChooserStartDate.getDate();
            Date endDate = jDateChooserEndDate.getDate();
            int workingDaysRequested = calculateWorkingDays(startDate, endDate);
            
            if (workingDaysRequested <= 0) {
                JOptionPane.showMessageDialog(this, 
                    "No working days selected! Please select weekdays only.", 
                    "Invalid Date Range", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            // Check leave balance
            Year currentYear = Year.now(MANILA_TIMEZONE);
            LeaveBalance currentBalance = leaveBalanceDAO.getLeaveBalance(
                Integer.parseInt(employeeId), 
                selectedLeaveType.getLeaveTypeId(), 
                currentYear
            );
            
            if (currentBalance == null) {
                JOptionPane.showMessageDialog(this, 
                    "No leave balance found for " + selectedLeaveType.getLeaveTypeName() + 
                    " in " + currentYear + "!\nPlease contact HR.", 
                    "Balance Not Found", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Check if sufficient balance
            if (!currentBalance.canTakeLeave(workingDaysRequested)) {
                JOptionPane.showMessageDialog(this, 
                    "Insufficient leave balance!\n" +
                    "Requested: " + workingDaysRequested + " days\n" +
                    "Available: " + (currentBalance.getRemainingLeaveDays() != null ? 
                        currentBalance.getRemainingLeaveDays() : 0) + " days", 
                    "Insufficient Balance", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            // Create leave request model
            LeaveRequestModel leaveRequest = new LeaveRequestModel();
            leaveRequest.setEmployeeId(Integer.parseInt(employeeId));
            leaveRequest.setLeaveTypeId(selectedLeaveType.getLeaveTypeId());
            leaveRequest.setLeaveStart(dateToSqlDate(startDate));
            leaveRequest.setLeaveEnd(dateToSqlDate(endDate));
            
            // Set reason (optional)
            String reason = reasontxtfield.getText().trim();
            if (!reason.isEmpty()) {
                leaveRequest.setLeaveReason(reason);
            }
            
            // Additional validation for business rules
            if (!isValidLeaveRequest(leaveRequest)) {
                return;
            }
            
            // Check for overlapping requests
            List<LeaveRequestModel> overlapping = leaveRequestDAO.getOverlappingLeaveRequests(leaveRequest);
            if (!overlapping.isEmpty()) {
                JOptionPane.showMessageDialog(this, 
                    "You already have a leave request for overlapping dates!", 
                    "Overlapping Request", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            // Save to database
            if (leaveRequestDAO.createLeaveRequest(leaveRequest)) {
                // Show success message with request details
                JOptionPane.showMessageDialog(this, 
                    "Leave request submitted successfully!\n" +
                    "Request ID: " + leaveRequest.getLeaveRequestId() + "\n" +
                    "Working days requested: " + workingDaysRequested + "\n" +
                    "Status: Pending Approval\n\n" +
                    "Your " + selectedLeaveType.getLeaveTypeName() + " balance will be updated when approved.", 
                    "Success", JOptionPane.INFORMATION_MESSAGE);
                
                // Clear form and reload data
                clearForm();
                loadLeaveRequests();
            } else {
                JOptionPane.showMessageDialog(this, 
                    "Failed to submit leave request. Please try again.", 
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
            
        } catch (Exception e) {
            System.err.println("Error submitting leave request: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, 
                "Error submitting leave request: " + e.getMessage(), 
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Validate leave request according to business rules
     */
    private boolean isValidLeaveRequest(LeaveRequestModel leaveRequest) {
        // Check if valid according to model
        if (!leaveRequest.isValidLeaveRequest()) {
            JOptionPane.showMessageDialog(this, 
                "Invalid leave request data!", 
                "Validation Error", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        
        // Check for weekend dates
        Date startDate = jDateChooserStartDate.getDate();
        Date endDate = jDateChooserEndDate.getDate();
        
        if (isWeekend(startDate) || isWeekend(endDate)) {
            JOptionPane.showMessageDialog(this, 
                "Leave dates cannot include weekends. Please select only weekdays!", 
                "Weekend Date Error", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        
        // Check for past dates (Manila timezone)
        LocalDate todayManila = LocalDate.now(MANILA_TIMEZONE);
        LocalDate requestStartDate = startDate.toInstant().atZone(MANILA_TIMEZONE).toLocalDate();
        
        if (requestStartDate.isBefore(todayManila)) {
            JOptionPane.showMessageDialog(this, 
                "Leave start date cannot be in the past! (Manila time)", 
                "Past Date Error", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        
        return true;
    }
    
    /**
     * Validate form inputs
     */
    private boolean validateForm() {
        // Check leave type selection
        String selectedLeaveType = (String) typeofleavejcombobox.getSelectedItem();
        if (selectedLeaveType == null || selectedLeaveType.equals("Select Leave Type")) {
            JOptionPane.showMessageDialog(this, "Please select a leave type!", 
                                        "Validation Error", JOptionPane.WARNING_MESSAGE);
            typeofleavejcombobox.requestFocus();
            return false;
        }
        
        // Check start date
        if (jDateChooserStartDate.getDate() == null) {
            JOptionPane.showMessageDialog(this, "Please select a start date!", 
                                        "Validation Error", JOptionPane.WARNING_MESSAGE);
            jDateChooserStartDate.requestFocus();
            return false;
        }
        
        // Check end date
        if (jDateChooserEndDate.getDate() == null) {
            JOptionPane.showMessageDialog(this, "Please select an end date!", 
                                        "Validation Error", JOptionPane.WARNING_MESSAGE);
            jDateChooserEndDate.requestFocus();
            return false;
        }
        
        // Check if dates are in the past (Manila timezone)
        LocalDate todayManila = LocalDate.now(MANILA_TIMEZONE);
        
        Date startDate = jDateChooserStartDate.getDate();
        Date endDate = jDateChooserEndDate.getDate();
        
        LocalDate requestStartDate = startDate.toInstant().atZone(MANILA_TIMEZONE).toLocalDate();
        LocalDate requestEndDate = endDate.toInstant().atZone(MANILA_TIMEZONE).toLocalDate();
        
        if (requestStartDate.isBefore(todayManila)) {
            JOptionPane.showMessageDialog(this, "Start date cannot be in the past! (Manila time)", 
                                        "Validation Error", JOptionPane.WARNING_MESSAGE);
            jDateChooserStartDate.requestFocus();
            return false;
        }
        
        // Check date range
        if (requestEndDate.isBefore(requestStartDate)) {
            JOptionPane.showMessageDialog(this, "End date cannot be before start date!", 
                                        "Validation Error", JOptionPane.WARNING_MESSAGE);
            jDateChooserEndDate.requestFocus();
            return false;
        }
        
        // Check for weekends
        if (isWeekend(startDate)) {
            JOptionPane.showMessageDialog(this, "Start date cannot be on weekend!", 
                                        "Validation Error", JOptionPane.WARNING_MESSAGE);
            jDateChooserStartDate.requestFocus();
            return false;
        }
        
        if (isWeekend(endDate)) {
            JOptionPane.showMessageDialog(this, "End date cannot be on weekend!", 
                                        "Validation Error", JOptionPane.WARNING_MESSAGE);
            jDateChooserEndDate.requestFocus();
            return false;
        }
        
        return true;
    }
    
    /**
     * Clear the form fields
     */
    private void clearForm() {
        // Reset date choosers to current date (Manila timezone)
        LocalDate todayManila = LocalDate.now(MANILA_TIMEZONE);
        Date today = Date.from(todayManila.atStartOfDay(MANILA_TIMEZONE).toInstant());
        jDateChooserStartDate.setDate(today);
        jDateChooserEndDate.setDate(today);
        
        reasontxtfield.setText("");
        typeofleavejcombobox.setSelectedIndex(0);
    }
    
    /**
     * Test method to debug leave types loading
     */
    private void testLeaveTypesLoading() {
        try {
            System.out.println("=== Testing Leave Types Loading ===");
            List<LeaveTypeModel> leaveTypes = leaveTypeDAO.getAllLeaveTypes();
            System.out.println("Total leave types found: " + leaveTypes.size());
            
            for (LeaveTypeModel leaveType : leaveTypes) {
                System.out.println("Leave Type ID: " + leaveType.getLeaveTypeId() + 
                                 ", Name: '" + leaveType.getLeaveTypeName() + "'" +
                                 ", Max Days: " + leaveType.getMaxDaysPerYear());
                
                // Test each leave type lookup
                LeaveTypeModel testLookup = leaveTypeDAO.getLeaveTypeByName(leaveType.getLeaveTypeName());
                System.out.println("  -> Lookup test: " + (testLookup != null ? "SUCCESS" : "FAILED"));
            }
            
            // Test specific lookups
            System.out.println("\n=== Testing Specific Lookups ===");
            String[] testNames = {"Sick Leave", "Vacation Leave", "sick leave", "SICK LEAVE"};
            for (String testName : testNames) {
                LeaveTypeModel result = leaveTypeDAO.getLeaveTypeByName(testName);
                System.out.println("Testing '" + testName + "': " + (result != null ? "FOUND" : "NOT FOUND"));
            }
            
            // Test combo box model
            System.out.println("\n=== Testing Combo Box Model ===");
            ComboBoxModel<String> model = typeofleavejcombobox.getModel();
            System.out.println("Combo box item count: " + model.getSize());
            for (int i = 0; i < model.getSize(); i++) {
                System.out.println("Item " + i + ": '" + model.getElementAt(i) + "'");
            }
            System.out.println("Selected item: '" + typeofleavejcombobox.getSelectedItem() + "'");
            System.out.println("Is combo box enabled: " + typeofleavejcombobox.isEnabled());
            
            // Check if we need to initialize leave types
            if (leaveTypes.size() == 0) {
                System.out.println("\n=== No Leave Types Found - Attempting to Initialize ===");
                initializeLeaveTypesIfNeeded();
            }
            
            System.out.println("=== End Testing ===\n");
            
        } catch (Exception e) {
            System.err.println("Error in testing: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Initialize leave types if they don't exist
     */
    private void initializeLeaveTypesIfNeeded() {
        try {
            List<LeaveTypeModel> existingTypes = leaveTypeDAO.getAllLeaveTypes();
            boolean hasSickLeave = false;
            boolean hasVacationLeave = false;
            
            for (LeaveTypeModel type : existingTypes) {
                if ("Sick Leave".equals(type.getLeaveTypeName())) {
                    hasSickLeave = true;
                }
                if ("Vacation Leave".equals(type.getLeaveTypeName())) {
                    hasVacationLeave = true;
                }
            }
            
            // Create missing leave types
            if (!hasSickLeave) {
                LeaveTypeModel sickLeave = new LeaveTypeModel("Sick Leave", "Leave for illness or medical reasons", 15);
                boolean created = leaveTypeDAO.createLeaveType(sickLeave);
                System.out.println("Created Sick Leave: " + created);
            }
            
            if (!hasVacationLeave) {
                LeaveTypeModel vacationLeave = new LeaveTypeModel("Vacation Leave", "Annual vacation leave", 15);
                boolean created = leaveTypeDAO.createLeaveType(vacationLeave);
                System.out.println("Created Vacation Leave: " + created);
            }
            
            // Reload the combo box if we created any
            if (!hasSickLeave || !hasVacationLeave) {
                loadLeaveTypes();
                JOptionPane.showMessageDialog(this, 
                    "Missing leave types have been created in the database.\nPlease try selecting a leave type again.", 
                    "Leave Types Initialized", 
                    JOptionPane.INFORMATION_MESSAGE);
            }
            
        } catch (Exception e) {
            System.err.println("Error initializing leave types: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Navigate back to the appropriate dashboard
     */
    private void navigateBackToDashboard() {
        if (loggedInUser == null) {
            new Login().setVisible(true);
            this.dispose();
            return;
        }
        
        String userRole = loggedInUser.getUserRole();
        if (userRole == null) {
            new Login().setVisible(true);
            this.dispose();
            return;
        }
        
        try {
            String upperRole = userRole.toUpperCase();
            
            if (upperRole.contains("HR")) {
                AdminHR adminHR = new AdminHR(loggedInUser);
                adminHR.setVisible(true);
            } else if (upperRole.contains("ACCOUNTING")) {
                AdminAccounting adminAccounting = new AdminAccounting(loggedInUser);
                adminAccounting.setVisible(true);
            } else if (upperRole.contains("IT")) {
                AdminIT adminIT = new AdminIT(loggedInUser);
                adminIT.setVisible(true);
            } else if (upperRole.contains("IMMEDIATE SUPERVISOR") || 
                       upperRole.contains("SUPERVISOR") || 
                       upperRole.contains("MANAGER")) {
                AdminSupervisor adminSupervisor = new AdminSupervisor(loggedInUser);
                adminSupervisor.setVisible(true);
            } else if (upperRole.contains("EMPLOYEE")) {
                EmployeeSelfService employeeSelfService = new EmployeeSelfService(loggedInUser);
                employeeSelfService.setVisible(true);
            } else {
                JOptionPane.showMessageDialog(this, 
                    "Unknown user role: " + userRole + "\nRedirecting to login.", 
                    "Role Error", 
                    JOptionPane.WARNING_MESSAGE);
                new Login().setVisible(true);
            }
            
            this.dispose();
            
        } catch (Exception e) {
            System.err.println("Error navigating back to dashboard: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, 
                "Error returning to dashboard. Redirecting to login.", 
                "Navigation Error", 
                JOptionPane.ERROR_MESSAGE);
            new Login().setVisible(true);
            this.dispose();
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
        backattndncbttn = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jPanel4 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        employeeIDtxtfield = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        firstNametxtfield = new javax.swing.JTextField();
        lastNametxtfield = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        positiontxtfield = new javax.swing.JTextField();
        employmentStatustxtfield = new javax.swing.JTextField();
        jPanel5 = new javax.swing.JPanel();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        dateofsubtxtfield = new javax.swing.JTextField();
        jLabel11 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        reasontxtfield = new javax.swing.JTextField();
        jLabel13 = new javax.swing.JLabel();
        typeofleavejcombobox = new javax.swing.JComboBox<>();
        submitleavebtn = new javax.swing.JButton();
        jDateChooserStartDate = new com.toedter.calendar.JDateChooser();
        jDateChooserEndDate = new com.toedter.calendar.JDateChooser();
        jPanel6 = new javax.swing.JPanel();
        jLabel15 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        dateFilter3 = new javax.swing.JComboBox<>();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel1.setBackground(new java.awt.Color(220, 95, 0));
        jPanel1.setPreferredSize(new java.awt.Dimension(211, 57));

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(255, 255, 255));
        jLabel1.setText("LEAVE REQUEST PAGE");

        backattndncbttn.setBackground(new java.awt.Color(207, 10, 10));
        backattndncbttn.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        backattndncbttn.setForeground(new java.awt.Color(255, 255, 255));
        backattndncbttn.setText("Back");
        backattndncbttn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                backattndncbttnActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(25, 25, 25)
                .addComponent(backattndncbttn)
                .addGap(39, 39, 39)
                .addComponent(jLabel1)
                .addContainerGap(902, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(backattndncbttn))
                .addContainerGap(13, Short.MAX_VALUE))
        );

        jPanel2.setBackground(new java.awt.Color(255, 255, 255));

        jPanel3.setBackground(new java.awt.Color(220, 95, 0));

        jLabel2.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel2.setForeground(new java.awt.Color(255, 255, 255));
        jLabel2.setText("PERSONAL DETAILS");

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(32, 32, 32)
                .addComponent(jLabel2)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addComponent(jLabel2)
                .addContainerGap(16, Short.MAX_VALUE))
        );

        jPanel4.setBackground(new java.awt.Color(255, 255, 255));
        jPanel4.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        jLabel3.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel3.setText("Employee ID:");

        employeeIDtxtfield.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                employeeIDtxtfieldActionPerformed(evt);
            }
        });

        jLabel5.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel5.setText("First Name:");

        jLabel6.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel6.setText("Last Name:");

        firstNametxtfield.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                firstNametxtfieldActionPerformed(evt);
            }
        });

        jLabel7.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel7.setText("Position:");

        jLabel8.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel8.setText("Employment Status:");

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(positiontxtfield, javax.swing.GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE)
                    .addComponent(firstNametxtfield)
                    .addComponent(jLabel3)
                    .addComponent(jLabel5)
                    .addComponent(employeeIDtxtfield)
                    .addComponent(jLabel7))
                .addGap(33, 33, 33)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabel8)
                    .addComponent(jLabel6)
                    .addComponent(lastNametxtfield)
                    .addComponent(employmentStatustxtfield, javax.swing.GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE))
                .addContainerGap(36, Short.MAX_VALUE))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(employeeIDtxtfield, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(jLabel6))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(firstNametxtfield, javax.swing.GroupLayout.DEFAULT_SIZE, 36, Short.MAX_VALUE)
                    .addComponent(lastNametxtfield))
                .addGap(18, 18, 18)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(jLabel8))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(positiontxtfield, javax.swing.GroupLayout.DEFAULT_SIZE, 36, Short.MAX_VALUE)
                    .addComponent(employmentStatustxtfield))
                .addContainerGap(32, Short.MAX_VALUE))
        );

        jPanel5.setBackground(new java.awt.Color(255, 255, 255));
        jPanel5.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        jLabel9.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel9.setText("Date of Submission");

        jLabel10.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel10.setText("Type of Leave:");

        dateofsubtxtfield.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dateofsubtxtfieldActionPerformed(evt);
            }
        });

        jLabel11.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel11.setText("Start of Leave");

        jLabel12.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel12.setText("Reason:");

        jLabel13.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel13.setText("End of Leave:");

        submitleavebtn.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        submitleavebtn.setText("Submit");
        submitleavebtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                submitleavebtnActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jDateChooserEndDate, javax.swing.GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE)
                    .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(jLabel9)
                        .addComponent(jLabel11)
                        .addComponent(dateofsubtxtfield)
                        .addComponent(jLabel13)
                        .addComponent(jDateChooserStartDate, javax.swing.GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabel12)
                    .addComponent(reasontxtfield)
                    .addComponent(typeofleavejcombobox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(submitleavebtn, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel10, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(53, 53, 53))
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel9)
                    .addComponent(jLabel10))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(dateofsubtxtfield, javax.swing.GroupLayout.DEFAULT_SIZE, 36, Short.MAX_VALUE)
                    .addComponent(typeofleavejcombobox))
                .addGap(18, 18, 18)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel11)
                    .addComponent(jLabel12))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addComponent(jDateChooserStartDate, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel13)
                        .addGap(9, 9, 9)
                        .addComponent(jDateChooserEndDate, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(reasontxtfield, javax.swing.GroupLayout.PREFERRED_SIZE, 105, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(submitleavebtn)
                .addGap(20, 20, 20))
        );

        jPanel6.setBackground(new java.awt.Color(220, 95, 0));

        jLabel15.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel15.setForeground(new java.awt.Color(255, 255, 255));
        jLabel15.setText("LEAVE DETAILS");

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addGap(32, 32, 32)
                .addComponent(jLabel15)
                .addContainerGap(358, Short.MAX_VALUE))
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addComponent(jLabel15)
                .addContainerGap(16, Short.MAX_VALUE))
        );

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null, null, null, null}
            },
            new String [] {
                "Date", "ID #", "First Name", "Last Name", "Position", "Status", "Supervisor", "Leave Type", "Note", "Start", "End", "Leave Status", "VL Remaining", "SL Remaining"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false, false, false, false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane1.setViewportView(jTable1);

        dateFilter3.setEditable(true);
        dateFilter3.setForeground(new java.awt.Color(255, 255, 255));
        dateFilter3.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Select Date", "2024-06", "2024-07", "2024-08", "2024-09", "2024-10", "2024-11", "2024-12" }));

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(25, 25, 25)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(87, 87, 87)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jPanel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 1171, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(dateFilter3, javax.swing.GroupLayout.PREFERRED_SIZE, 170, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(21, 21, 21)
                .addComponent(dateFilter3, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 266, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(33, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 1227, Short.MAX_VALUE)
            .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(22, 22, 22))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void employeeIDtxtfieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_employeeIDtxtfieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_employeeIDtxtfieldActionPerformed

    private void firstNametxtfieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_firstNametxtfieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_firstNametxtfieldActionPerformed

    private void submitleavebtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_submitleavebtnActionPerformed
    //Submit leave request
        submitLeaveRequest();
    }//GEN-LAST:event_submitleavebtnActionPerformed

    private void backattndncbttnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_backattndncbttnActionPerformed
    // Navigate back to dashboard/landing page
        navigateBackToDashboard();
    }//GEN-LAST:event_backattndncbttnActionPerformed

    private void dateofsubtxtfieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dateofsubtxtfieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_dateofsubtxtfieldActionPerformed

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
            java.util.logging.Logger.getLogger(gui.LeaveRequest.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(gui.LeaveRequest.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(gui.LeaveRequest.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(gui.LeaveRequest.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>

       /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                System.out.println("LeaveRequest requires a valid UserAuthenticationModel instance.");
                System.out.println("Please use: new LeaveRequest(loggedInUser).setVisible(true);");
            }
        });
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton backattndncbttn;
    private javax.swing.JComboBox<String> dateFilter;
    private javax.swing.JComboBox<String> dateFilter1;
    private javax.swing.JComboBox<String> dateFilter2;
    private javax.swing.JComboBox<String> dateFilter3;
    private javax.swing.JTextField dateofsubtxtfield;
    private javax.swing.JTextField employeeIDtxtfield;
    private javax.swing.JTextField employmentStatustxtfield;
    private javax.swing.JTextField firstNametxtfield;
    private com.toedter.calendar.JDateChooser jDateChooserEndDate;
    private com.toedter.calendar.JDateChooser jDateChooserStartDate;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTable1;
    private javax.swing.JTextField lastNametxtfield;
    private javax.swing.JTextField positiontxtfield;
    private javax.swing.JTextField reasontxtfield;
    private javax.swing.JButton submitleavebtn;
    private javax.swing.JComboBox<String> typeofleavejcombobox;
    // End of variables declaration//GEN-END:variables
}
