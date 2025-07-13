package gui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.ArrayList;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import DAOs.DatabaseConnection;
import Models.UserAuthenticationModel;

/**
 * Popup window for submitting and viewing overtime requests.
 * Business rules:
 * - Only allows requests for today and future dates
 * - Validates time ranges and requires detailed reason
 * - Saves to overtimerequest table (MySQL DB) with 'Pending' status
 * - Needs to be resized to show the YYYY (from the right side)
 */
public class OvertimeRequestPopup extends JDialog {
    
    private JTable overtimeRequestsTable;
    private DefaultTableModel tableModel;
    private JComboBox<String> dayComboBox;
    private JComboBox<String> monthComboBox;
    private JComboBox<String> yearComboBox;
    private JSpinner startTimeSpinner;
    private JSpinner endTimeSpinner;
    private JTextArea reasonTextArea;
    private JButton addRequestButton;
    private JButton refreshButton;
    private JButton closeButton;
    private JLabel statusLabel;
    
    private String employeeId;
    private String employeeName;
    private DatabaseConnection databaseConnection;
    
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
    
    public OvertimeRequestPopup(JFrame parent, String employeeId, UserAuthenticationModel loggedInUser) {
        super(parent, "Overtime Request Management", true);
        this.employeeId = employeeId;
        this.employeeName = loggedInUser.getFirstName() + " " + loggedInUser.getLastName();
        this.databaseConnection = new DatabaseConnection();
        
        setTitle("Overtime Request Management - " + employeeName);
        
        initComponents();
        loadOvertimeRequests();
        setupEventHandlers();
        
        setLocationRelativeTo(parent);
    }
    
    private void initComponents() {
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        setSize(1200, 700);
        
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        mainPanel.setBackground(Color.WHITE);
        
        JPanel titlePanel = createTitlePanel();
        mainPanel.add(titlePanel, BorderLayout.NORTH);
        
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(800);
        splitPane.setResizeWeight(0.7);
        
        JPanel tablePanel = createTablePanel();
        splitPane.setLeftComponent(tablePanel);
        
        JPanel formPanel = createFormPanel();
        splitPane.setRightComponent(formPanel);
        
        mainPanel.add(splitPane, BorderLayout.CENTER);
        
        JPanel statusPanel = createStatusPanel();
        mainPanel.add(statusPanel, BorderLayout.SOUTH);
        
        add(mainPanel);
    }
    
    private JPanel createTitlePanel() {
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(Color.WHITE);
        titlePanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        
        JLabel titleLabel = new JLabel("Overtime Request Management - " + employeeName);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(new Color(220, 95, 0));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        JLabel subtitleLabel = new JLabel("Submit new overtime requests and view your submission history");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitleLabel.setForeground(new Color(102, 102, 102));
        subtitleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        titlePanel.add(titleLabel, BorderLayout.CENTER);
        titlePanel.add(subtitleLabel, BorderLayout.SOUTH);
        
        return titlePanel;
    }
    
    private JPanel createTablePanel() {
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBackground(Color.WHITE);
        tablePanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(220, 95, 0), 1),
            "Your Overtime Requests",
            0, 0, new Font("Segoe UI", Font.BOLD, 12), new Color(220, 95, 0)
        ));
        
        createTable();
        JScrollPane tableScrollPane = new JScrollPane(overtimeRequestsTable);
        tableScrollPane.setPreferredSize(new Dimension(750, 400));
        tableScrollPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        JPanel refreshPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        refreshPanel.setBackground(Color.WHITE);
        
        refreshButton = new JButton("Refresh");
        refreshButton.setBackground(new Color(220, 95, 0));
        refreshButton.setForeground(Color.WHITE);
        refreshButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        refreshButton.setFocusPainted(false);
        
        refreshPanel.add(refreshButton);
        
        tablePanel.add(refreshPanel, BorderLayout.NORTH);
        tablePanel.add(tableScrollPane, BorderLayout.CENTER);
        
        return tablePanel;
    }
    
    private void createTable() {
        String[] columnNames = {
            "Request ID", "Date", "Start Time", "End Time", "Hours", "Reason", "Status", "Date Submitted"
        };
        
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        
        overtimeRequestsTable = new JTable(tableModel);
        overtimeRequestsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        overtimeRequestsTable.setRowHeight(25);
        overtimeRequestsTable.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        overtimeRequestsTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 11));
        overtimeRequestsTable.getTableHeader().setBackground(new Color(245, 245, 245));
        overtimeRequestsTable.setGridColor(new Color(230, 230, 230));
        
        TableColumnModel columnModel = overtimeRequestsTable.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(80);
        columnModel.getColumn(1).setPreferredWidth(90);
        columnModel.getColumn(2).setPreferredWidth(80);
        columnModel.getColumn(3).setPreferredWidth(80);
        columnModel.getColumn(4).setPreferredWidth(60);
        columnModel.getColumn(5).setPreferredWidth(150);
        columnModel.getColumn(6).setPreferredWidth(80);
        columnModel.getColumn(7).setPreferredWidth(100);
    }
    
    private JPanel createFormPanel() {
        JPanel formPanel = new JPanel(new BorderLayout());
        formPanel.setBackground(Color.WHITE);
        formPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(34, 139, 34), 1),
            "Submit New Overtime Request",
            0, 0, new Font("Segoe UI", Font.BOLD, 12), new Color(34, 139, 34)
        ));
        
        JPanel infoPanel = new JPanel(new BorderLayout());
        infoPanel.setBackground(new Color(255, 248, 220));
        infoPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(255, 193, 7), 1),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        
        JLabel infoLabel = new JLabel("<html><b>📋 Important:</b> Overtime requests can only be submitted for today and future dates.<br/>All fields are required for submission.</html>");
        infoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        infoLabel.setForeground(new Color(133, 100, 4));
        infoPanel.add(infoLabel, BorderLayout.CENTER);
        
        formPanel.add(infoPanel, BorderLayout.NORTH);
        
        JPanel fieldsPanel = new JPanel(new GridBagLayout());
        fieldsPanel.setBackground(Color.WHITE);
        fieldsPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 5, 8, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        gbc.gridx = 0; gbc.gridy = 0;
        JLabel dateLabel = new JLabel("Date:");
        dateLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        fieldsPanel.add(dateLabel, gbc);
        
        gbc.gridx = 1; gbc.gridy = 0; gbc.fill = GridBagConstraints.HORIZONTAL;
        JPanel datePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        datePanel.setBackground(Color.WHITE);
        
        String[] months = {"01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12"};
        monthComboBox = new JComboBox<>(months);
        monthComboBox.setSelectedItem(String.format("%02d", LocalDate.now().getMonthValue()));
        monthComboBox.setPreferredSize(new Dimension(50, 25));
        
        String[] days = new String[31];
        for (int i = 1; i <= 31; i++) {
            days[i-1] = String.format("%02d", i);
        }
        dayComboBox = new JComboBox<>(days);
        dayComboBox.setSelectedItem(String.format("%02d", LocalDate.now().getDayOfMonth()));
        dayComboBox.setPreferredSize(new Dimension(50, 25));
        
        String[] years = new String[3];
        int currentYear = LocalDate.now().getYear();
        for (int i = 0; i < 3; i++) {
            years[i] = String.valueOf(currentYear + i);
        }
        yearComboBox = new JComboBox<>(years);
        yearComboBox.setSelectedItem(String.valueOf(currentYear));
        yearComboBox.setPreferredSize(new Dimension(65, 25));
        
        datePanel.add(new JLabel("MM:"));
        datePanel.add(monthComboBox);
        datePanel.add(new JLabel(" DD:"));
        datePanel.add(dayComboBox);
        datePanel.add(new JLabel(" YYYY:"));
        datePanel.add(yearComboBox);
        
        fieldsPanel.add(datePanel, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL;
        JLabel dateNoteLabel = new JLabel("<html><i>Note: Overtime requests are only accepted for today and future dates</i></html>");
        dateNoteLabel.setFont(new Font("Segoe UI", Font.ITALIC, 10));
        dateNoteLabel.setForeground(new Color(102, 102, 102));
        fieldsPanel.add(dateNoteLabel, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE;
        JLabel startTimeLabel = new JLabel("Start Time:");
        startTimeLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        fieldsPanel.add(startTimeLabel, gbc);
        
        gbc.gridx = 1; gbc.gridy = 2; gbc.fill = GridBagConstraints.HORIZONTAL;
        startTimeSpinner = new JSpinner(new SpinnerDateModel());
        JSpinner.DateEditor startTimeEditor = new JSpinner.DateEditor(startTimeSpinner, "HH:mm");
        startTimeSpinner.setEditor(startTimeEditor);
        java.util.Calendar cal1 = java.util.Calendar.getInstance();
        cal1.set(java.util.Calendar.HOUR_OF_DAY, 17);
        cal1.set(java.util.Calendar.MINUTE, 0);
        cal1.set(java.util.Calendar.SECOND, 0);
        cal1.set(java.util.Calendar.MILLISECOND, 0);
        startTimeSpinner.setValue(cal1.getTime());
        fieldsPanel.add(startTimeSpinner, gbc);
        
        gbc.gridx = 0; gbc.gridy = 3; gbc.fill = GridBagConstraints.NONE;
        JLabel endTimeLabel = new JLabel("End Time:");
        endTimeLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        fieldsPanel.add(endTimeLabel, gbc);
        
        gbc.gridx = 1; gbc.gridy = 3; gbc.fill = GridBagConstraints.HORIZONTAL;
        endTimeSpinner = new JSpinner(new SpinnerDateModel());
        JSpinner.DateEditor endTimeEditor = new JSpinner.DateEditor(endTimeSpinner, "HH:mm");
        endTimeSpinner.setEditor(endTimeEditor);
        java.util.Calendar cal2 = java.util.Calendar.getInstance();
        cal2.set(java.util.Calendar.HOUR_OF_DAY, 19);
        cal2.set(java.util.Calendar.MINUTE, 0);
        cal2.set(java.util.Calendar.SECOND, 0);
        cal2.set(java.util.Calendar.MILLISECOND, 0);
        endTimeSpinner.setValue(cal2.getTime());
        fieldsPanel.add(endTimeSpinner, gbc);
        
        gbc.gridx = 0; gbc.gridy = 4; gbc.fill = GridBagConstraints.NONE;
        JLabel reasonLabel = new JLabel("Reason (Required):");
        reasonLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        reasonLabel.setForeground(new Color(220, 95, 0));
        fieldsPanel.add(reasonLabel, gbc);
        
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.BOTH; gbc.weightx = 1.0; gbc.weighty = 1.0;
        reasonTextArea = new JTextArea(6, 25);
        reasonTextArea.setLineWrap(true);
        reasonTextArea.setWrapStyleWord(true);
        reasonTextArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        reasonTextArea.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        reasonTextArea.setBackground(new Color(252, 252, 252));
        
        reasonTextArea.setForeground(Color.GRAY);
        reasonTextArea.setText("Please provide a detailed explanation for your overtime request...");
        
        reasonTextArea.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent evt) {
                if (reasonTextArea.getText().equals("Please provide a detailed explanation for your overtime request...")) {
                    reasonTextArea.setText("");
                    reasonTextArea.setForeground(Color.BLACK);
                }
            }
            
            @Override
            public void focusLost(java.awt.event.FocusEvent evt) {
                if (reasonTextArea.getText().trim().isEmpty()) {
                    reasonTextArea.setForeground(Color.GRAY);
                    reasonTextArea.setText("Please provide a detailed explanation for your overtime request...");
                }
            }
        });
        
        JScrollPane reasonScrollPane = new JScrollPane(reasonTextArea);
        reasonScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        reasonScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        fieldsPanel.add(reasonScrollPane, gbc);
        
        formPanel.add(fieldsPanel, BorderLayout.CENTER);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttonPanel.setBackground(Color.WHITE);
        
        addRequestButton = new JButton("Submit Request");
        addRequestButton.setBackground(new Color(34, 139, 34));
        addRequestButton.setForeground(Color.WHITE);
        addRequestButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        addRequestButton.setPreferredSize(new Dimension(140, 35));
        addRequestButton.setFocusPainted(false);
        
        buttonPanel.add(addRequestButton);
        formPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        return formPanel;
    }
    
    private JPanel createStatusPanel() {
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBackground(Color.WHITE);
        statusPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        
        statusLabel = new JLabel("Ready to submit overtime requests");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setForeground(new Color(102, 102, 102));
        
        closeButton = new JButton("Close");
        closeButton.setBackground(new Color(207, 10, 10));
        closeButton.setForeground(Color.WHITE);
        closeButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        closeButton.setPreferredSize(new Dimension(100, 35));
        closeButton.setFocusPainted(false);
        
        statusPanel.add(statusLabel, BorderLayout.CENTER);
        statusPanel.add(closeButton, BorderLayout.EAST);
        
        return statusPanel;
    }
    
    private void loadOvertimeRequests() {
        SwingUtilities.invokeLater(() -> {
            try {
                List<OvertimeRequest> requests = getOvertimeRequestsFromDatabase();
                tableModel.setRowCount(0);
                
                for (OvertimeRequest request : requests) {
                    Object[] rowData = {
                        request.requestId,
                        request.overtimeDate.format(dateFormatter),
                        request.startTime.format(timeFormatter),
                        request.endTime.format(timeFormatter),
                        String.format("%.1f", request.hours),
                        request.reason.length() > 30 ? request.reason.substring(0, 30) + "..." : request.reason,
                        request.status,
                        request.dateCreated.format(DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm"))
                    };
                    tableModel.addRow(rowData);
                }
                
                statusLabel.setText(String.format("Loaded %d overtime requests", requests.size()));
                
            } catch (Exception e) {
                System.err.println("Error loading overtime requests: " + e.getMessage());
                e.printStackTrace();
                statusLabel.setText("Error loading overtime requests");
                JOptionPane.showMessageDialog(this,
                    "Error loading overtime requests: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        });
    }
    
    /**
     * Retrieves overtime requests from database for the current employee.
     * Calculates duration in hours using TIMESTAMPDIFF function.
     * Orders by creation date (newest first) and limits to 100 records.
     */
    private List<OvertimeRequest> getOvertimeRequestsFromDatabase() throws SQLException {
        List<OvertimeRequest> requests = new ArrayList<>();
        
        String sql = """
            SELECT 
                overtimeRequestId,
                DATE(overtimeStart) as overtime_date,
                TIME(overtimeStart) as start_time,
                TIME(overtimeEnd) as end_time,
                TIMESTAMPDIFF(MINUTE, overtimeStart, overtimeEnd) / 60.0 as hours,
                overtimeReason,
                approvalStatus,
                dateCreated
            FROM overtimerequest
            WHERE employeeId = ?
            ORDER BY dateCreated DESC
            LIMIT 100
        """;
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, Integer.parseInt(employeeId));
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    OvertimeRequest request = new OvertimeRequest();
                    request.requestId = rs.getInt("overtimeRequestId");
                    request.overtimeDate = rs.getDate("overtime_date").toLocalDate();
                    request.startTime = rs.getTime("start_time").toLocalTime();
                    request.endTime = rs.getTime("end_time").toLocalTime();
                    request.hours = rs.getDouble("hours");
                    request.reason = rs.getString("overtimeReason");
                    request.status = rs.getString("approvalStatus");
                    request.dateCreated = rs.getTimestamp("dateCreated").toLocalDateTime();
                    
                    requests.add(request);
                }
            }
        }
        
        return requests;
    }
    
    private void setupEventHandlers() {
        addRequestButton.addActionListener(e -> submitNewOvertimeRequest());
        refreshButton.addActionListener(e -> loadOvertimeRequests());
        closeButton.addActionListener(e -> dispose());
        
        getRootPane().setDefaultButton(addRequestButton);
        
        KeyStroke escapeKeyStroke = KeyStroke.getKeyStroke("ESCAPE");
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escapeKeyStroke, "ESCAPE");
        getRootPane().getActionMap().put("ESCAPE", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                dispose();
            }
        });
    }
    
    /**
     * Submits a new overtime request with comprehensive validation.
     * Business rules enforced:
     * - Date must be today or future
     * - End time must be after start time
     * - Reason must be at least 10 characters
     * - All fields are required
     */
    private void submitNewOvertimeRequest() {
        try {
            List<String> errors = new ArrayList<>();
            
            // Validate date - must be today or future
            LocalDate selectedDate = getSelectedDate();
            if (selectedDate == null) {
                errors.add("• Please select a valid date");
            } else if (selectedDate.isBefore(LocalDate.now())) {
                errors.add("• Date must be today or a future date");
            }
            
            // Validate start time
            LocalTime startTime = null;
            try {
                Object startValue = startTimeSpinner.getValue();
                if (startValue instanceof java.util.Date) {
                    java.util.Date startDate = (java.util.Date) startValue;
                    startTime = startDate.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalTime();
                } else if (startValue instanceof java.sql.Time) {
                    startTime = ((java.sql.Time) startValue).toLocalTime();
                }
                if (startTime == null) {
                    errors.add("• Please select a valid start time");
                }
            } catch (Exception e) {
                errors.add("• Please select a valid start time");
            }
            
            // Validate end time
            LocalTime endTime = null;
            try {
                Object endValue = endTimeSpinner.getValue();
                if (endValue instanceof java.util.Date) {
                    java.util.Date endDate = (java.util.Date) endValue;
                    endTime = endDate.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalTime();
                } else if (endValue instanceof java.sql.Time) {
                    endTime = ((java.sql.Time) endValue).toLocalTime();
                }
                if (endTime == null) {
                    errors.add("• Please select a valid end time");
                }
            } catch (Exception e) {
                errors.add("• Please select a valid end time");
            }
            
            // Validate time relationship
            if (startTime != null && endTime != null) {
                if (endTime.isBefore(startTime) || endTime.equals(startTime)) {
                    errors.add("• End time must be after start time");
                }
            }
            
            // Validate reason - minimum 10 characters for meaningful explanation
            String reason = getOvertimeReason();
            if (reason.isEmpty()) {
                errors.add("• Reason is required - please provide an explanation");
            } else if (reason.length() < 10) {
                errors.add("• Reason must be at least 10 characters long");
            }
            
            if (!errors.isEmpty()) {
                StringBuilder errorMessage = new StringBuilder("Please fix the following issues:\n\n");
                for (String error : errors) {
                    errorMessage.append(error).append("\n");
                }
                
                JOptionPane.showMessageDialog(this,
                    errorMessage.toString(),
                    "Validation Errors",
                    JOptionPane.WARNING_MESSAGE);
                
                if (selectedDate == null || selectedDate.isBefore(LocalDate.now())) {
                    monthComboBox.requestFocus();
                } else if (startTime == null) {
                    startTimeSpinner.requestFocus();
                } else if (endTime == null) {
                    endTimeSpinner.requestFocus();
                } else if (reason.isEmpty() || reason.length() < 10) {
                    reasonTextArea.requestFocus();
                }
                return;
            }
            
            double hours = java.time.Duration.between(startTime, endTime).toMinutes() / 60.0;
            
            String confirmMessage = String.format(
                "Submit overtime request?\n\n" +
                "Date: %s\n" +
                "Time: %s - %s (%.1f hours)\n" +
                "Reason: %s\n\n" +
                "This request will be sent to your supervisor for approval.",
                selectedDate.format(dateFormatter),
                startTime.format(timeFormatter),
                endTime.format(timeFormatter),
                hours,
                reason.length() > 50 ? reason.substring(0, 50) + "..." : reason
            );
            
            int confirm = JOptionPane.showConfirmDialog(this,
                confirmMessage,
                "Confirm Overtime Request",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
            
            if (confirm == JOptionPane.YES_OPTION) {
                submitToDatabase(selectedDate, startTime, endTime, reason);
                
                JOptionPane.showMessageDialog(this,
                    String.format(
                        "Overtime request submitted successfully!\n\n" +
                        "Date: %s\n" +
                        "Duration: %.1f hours\n" +
                        "Status: Pending approval\n\n" +
                        "You will be notified when your supervisor reviews the request.",
                        selectedDate.format(dateFormatter), hours
                    ),
                    "Request Submitted",
                    JOptionPane.INFORMATION_MESSAGE);
                
                clearForm();
                loadOvertimeRequests();
            }
            
        } catch (Exception e) {
            System.err.println("Error submitting overtime request: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Error submitting overtime request: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private LocalDate getSelectedDate() {
        try {
            int year = Integer.parseInt((String) yearComboBox.getSelectedItem());
            int month = Integer.parseInt((String) monthComboBox.getSelectedItem());
            int day = Integer.parseInt((String) dayComboBox.getSelectedItem());
            
            return LocalDate.of(year, month, day);
        } catch (Exception e) {
            return null;
        }
    }
    
    private String getOvertimeReason() {
        String reason = reasonTextArea.getText().trim();
        if (reason.equals("Please provide a detailed explanation for your overtime request...")) {
            return "";
        }
        return reason;
    }
    
    private void clearForm() {
        LocalDate today = LocalDate.now();
        monthComboBox.setSelectedItem(String.format("%02d", today.getMonthValue()));
        dayComboBox.setSelectedItem(String.format("%02d", today.getDayOfMonth()));
        yearComboBox.setSelectedItem(String.valueOf(today.getYear()));
        
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.HOUR_OF_DAY, 17);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        startTimeSpinner.setValue(cal.getTime());
        
        cal.set(java.util.Calendar.HOUR_OF_DAY, 19);
        endTimeSpinner.setValue(cal.getTime());
        
        reasonTextArea.setForeground(Color.GRAY);
        reasonTextArea.setText("Please provide a detailed explanation for your overtime request...");
    }
    
    /**
     * Saves overtime request to the overtimerequest table.
     * Creates timestamp fields by combining date and time components.
     * Sets initial status to 'Pending' for supervisor approval.
     */
    private void submitToDatabase(LocalDate date, LocalTime startTime, LocalTime endTime, String reason) throws SQLException {
        String sql = """
            INSERT INTO overtimerequest (employeeId, overtimeStart, overtimeEnd, overtimeReason, approvalStatus, dateCreated)
            VALUES (?, ?, ?, ?, 'Pending', NOW())
            """;
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            // Combine date and time into SQL timestamps
            java.sql.Timestamp overtimeStart = java.sql.Timestamp.valueOf(date.atTime(startTime));
            java.sql.Timestamp overtimeEnd = java.sql.Timestamp.valueOf(date.atTime(endTime));
            
            stmt.setInt(1, Integer.parseInt(employeeId));
            stmt.setTimestamp(2, overtimeStart);
            stmt.setTimestamp(3, overtimeEnd);
            stmt.setString(4, reason);
            
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected == 0) {
                throw new SQLException("Failed to insert overtime request - no rows affected");
            }
        }
    }
    
    /**
     * Data model for overtime requests from the overtimerequest table.
     * Includes calculated hours field for display purposes.
     */
    private static class OvertimeRequest {
        public int requestId;
        public LocalDate overtimeDate;
        public LocalTime startTime;
        public LocalTime endTime;
        public double hours;              // Calculated duration in hours
        public String reason;
        public String status;             // Pending, Approved, or Rejected
        public java.time.LocalDateTime dateCreated;
    }
}