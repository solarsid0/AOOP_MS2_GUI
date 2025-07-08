package gui;

import DAOs.EmployeeDAO;
import DAOs.DatabaseConnection;
import Models.UserAuthenticationModel;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class AttendanceDetailsGUI extends javax.swing.JFrame {

    private EmployeeDAO employeeDAO;
    private DatabaseConnection databaseConnection;
    private String employeeId;
    private String employeeName;
    private UserAuthenticationModel loggedInUser;

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    public AttendanceDetailsGUI(UserAuthenticationModel loggedInUser) {
        initComponents();

        this.loggedInUser = loggedInUser;
        this.employeeId = String.valueOf(loggedInUser.getEmployeeId());
        this.employeeName = loggedInUser.getFirstName() + " " + loggedInUser.getLastName();

        this.setLocationRelativeTo(null);
        this.databaseConnection = new DatabaseConnection();
        this.employeeDAO = new EmployeeDAO(databaseConnection);

        setTitle("Attendance Details - " + employeeName);
        
        initializeDateFilter();
        loadAttendanceData();
    }


    private void initializeDateFilter() {
        dateFilter.removeAllItems();
        dateFilter.addItem("Select");

        // Add only specific months from 2024
        dateFilter.addItem("2024-06");
        dateFilter.addItem("2024-07");
        dateFilter.addItem("2024-08");
        dateFilter.addItem("2024-09");
        dateFilter.addItem("2024-10");
        dateFilter.addItem("2024-11");
        dateFilter.addItem("2024-12");

        dateFilter.setSelectedItem("Select");
    }

    private void loadAttendanceData() {
        loadAttendanceData(null);
    }

    private void loadAttendanceData(String dateFilter) {
        SwingUtilities.invokeLater(() -> {
            try {
                List<AttendanceRecord> attendanceRecords = getAttendanceFromPayslipView(dateFilter);
                DefaultTableModel model = (DefaultTableModel) AttendanceDetailsTbl.getModel();
                model.setRowCount(0);

                for (AttendanceRecord record : attendanceRecords) {
                    model.addRow(new Object[]{
                            employeeId,
                            record.date != null ? record.date.format(dateFormatter) : "N/A",
                            record.timeIn != null ? record.timeIn.format(timeFormatter) : "N/A",
                            record.timeOut != null ? record.timeOut.format(timeFormatter) : "N/A",
                            String.format("%.2f", record.hoursWorked),
                            String.format("%.2f", record.lateHours),
                            String.format("%.2f", record.overtimeHours)
                    });
                }
                
            } catch (Exception e) {
                System.err.println("Error loading attendance data: " + e.getMessage());
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, 
                    "Error loading attendance data: " + e.getMessage(), 
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    /**
     * Fetches attendance records with calculated hours from the attendance table.
     * Business rules:
     * - Regular hours: calculated only within 8AM-5PM window (excluding 1hr lunch)
     * - Late hours: only calculated for rank-and-file employees (0.00 for non rank-and-file)
     * - Overtime: only calculated for rank-and-file employees (0.00 for non rank-and-file)
     * - Non rank-and-file employees get 8.00 hours every work day regardless of actual time
     * - Shows missing work days where employee didn't attend
     */
    private List<AttendanceRecord> getAttendanceFromPayslipView(String dateFilter) {
        List<AttendanceRecord> records = new ArrayList<>();
        
        // Get employee's position info to determine if rank-and-file
        boolean isRankAndFile = false;
        try {
            isRankAndFile = employeeDAO.isEmployeeRankAndFile(Integer.parseInt(employeeId));
        } catch (Exception e) {
            System.err.println("Error checking employee rank: " + e.getMessage());
        }

        // Use a simpler approach: get attendance records and generate missing days in Java
        String sql = """
            SELECT 
                a.employeeId,
                a.date,
                a.timeIn,
                a.timeOut,
                -- Hours worked based on employee type
                CASE 
                    WHEN ? = 1 THEN  -- Rank-and-file employee
                        CASE 
                            WHEN a.timeIn IS NOT NULL AND a.timeOut IS NOT NULL THEN
                                GREATEST(0, 
                                    (TIME_TO_SEC(LEAST(a.timeOut, '17:00:00')) - TIME_TO_SEC(GREATEST(a.timeIn, '08:00:00'))) / 3600.0
                                    - CASE 
                                        WHEN GREATEST(a.timeIn, '08:00:00') <= '12:00:00' AND LEAST(a.timeOut, '17:00:00') >= '13:00:00' 
                                        THEN 1.0 
                                        ELSE 0.0 
                                      END
                                )
                            ELSE 0.00 
                        END
                    ELSE  -- Non rank-and-file employee: always 8 hours if present
                        CASE WHEN a.timeIn IS NOT NULL THEN 8.00 ELSE 0.00 END
                END AS hours_worked,
                -- Late hours: only for rank-and-file employees
                CASE 
                    WHEN ? = 1 AND a.timeIn > '08:10:00'
                    THEN (TIME_TO_SEC(a.timeIn) - TIME_TO_SEC('08:00:00')) / 3600.0
                    ELSE 0.00 
                END AS late_hours,
                -- Overtime hours: only for rank-and-file employees
                CASE 
                    WHEN ? = 1 AND a.timeIn IS NOT NULL AND a.timeOut IS NOT NULL THEN
                        CASE WHEN a.timeIn < '08:00:00' 
                             THEN (TIME_TO_SEC('08:00:00') - TIME_TO_SEC(a.timeIn)) / 3600.0 
                             ELSE 0 END
                        +
                        CASE WHEN a.timeOut > '17:00:00' 
                             THEN (TIME_TO_SEC(a.timeOut) - TIME_TO_SEC('17:00:00')) / 3600.0 
                             ELSE 0 END
                    ELSE 0.00 
                END AS overtime_hours,
                DATE_FORMAT(a.date, '%Y-%m') AS pay_month
            FROM attendance a
            WHERE a.employeeId = ?
        """;

        // Apply date filtering
        if (dateFilter != null && !dateFilter.equals("All")) {
            if (dateFilter.length() == 4) { // Year format
                sql += " AND YEAR(a.date) = ?";
            } else { // Month format
                sql += " AND DATE_FORMAT(a.date, '%Y-%m') = ?";
            }
        } else {
            // Default to last 3 months if no filter
            sql += " AND a.date >= DATE_SUB(CURDATE(), INTERVAL 90 DAY)";
        }

        sql += " ORDER BY a.date DESC LIMIT 1000";

        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, isRankAndFile ? 1 : 0);  // hours_worked logic
            stmt.setInt(2, isRankAndFile ? 1 : 0);  // late_hours logic
            stmt.setInt(3, isRankAndFile ? 1 : 0);  // overtime_hours logic
            stmt.setInt(4, Integer.parseInt(employeeId));

            if (dateFilter != null && !dateFilter.equals("All")) {
                if (dateFilter.length() == 4) {
                    stmt.setInt(5, Integer.parseInt(dateFilter));
                } else {
                    stmt.setString(5, dateFilter);
                }
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    AttendanceRecord record = new AttendanceRecord();
                    record.employeeId = rs.getInt("employeeId");
                    
                    Date sqlDate = rs.getDate("date");
                    if (sqlDate != null) {
                        record.date = sqlDate.toLocalDate();
                    }
                    
                    Time sqlTimeIn = rs.getTime("timeIn");
                    if (sqlTimeIn != null) {
                        record.timeIn = sqlTimeIn.toLocalTime();
                    }
                    
                    Time sqlTimeOut = rs.getTime("timeOut");
                    if (sqlTimeOut != null) {
                        record.timeOut = sqlTimeOut.toLocalTime();
                    }
                    
                    record.hoursWorked = rs.getDouble("hours_worked");
                    record.lateHours = rs.getDouble("late_hours");
                    record.overtimeHours = rs.getDouble("overtime_hours");
                    record.payMonth = rs.getString("pay_month");

                    records.add(record);
                }
            }
            
            
        } catch (SQLException e) {
            System.err.println("Error fetching attendance records: " + e.getMessage());
            e.printStackTrace();
        }

        return records;
    }
    


    /**
     * Opens the overtime request popup if the employee is eligible.
     * Business rule: Only rank-and-file employees can submit overtime requests.
     */
    private void showOvertimeRequestsPopup() {
        try {
            // Check employee eligibility for overtime requests
            boolean isRankAndFile = employeeDAO.isEmployeeRankAndFile(Integer.parseInt(employeeId));
            if (!isRankAndFile) {
                JOptionPane.showMessageDialog(this, 
                    "Only rank-and-file employees are eligible for overtime requests.", 
                    "Not Eligible", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            OvertimeRequestPopup popup = new OvertimeRequestPopup(this, employeeId, loggedInUser);
            popup.setVisible(true);

        } catch (Exception e) {
            System.err.println("Error opening overtime request window: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, 
                "Error opening overtime request window: " + e.getMessage(), 
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void filterAttendanceByMonth() {
        String selectedFilter = (String) dateFilter.getSelectedItem();
        if (selectedFilter != null) {
            loadAttendanceData(selectedFilter.equals("All") ? null : selectedFilter);
        }
    }

    private void goBack() {
        navigateBackToDashboard();
    }

    /**
     * Navigate back to the appropriate dashboard based on user role.
     * Role hierarchy: HR > Accounting > IT > Supervisor > Employee
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
            
            // Route to appropriate dashboard based on role priority
            if (upperRole.contains("HR")) {
                AdminHR adminHR = new AdminHR(loggedInUser);
                adminHR.setVisible(true);
            } 
            else if (upperRole.contains("ACCOUNTING")) {
                AdminAccounting adminAccounting = new AdminAccounting(loggedInUser);
                adminAccounting.setVisible(true);
            } 
            else if (upperRole.contains("IT")) {
                AdminIT adminIT = new AdminIT(loggedInUser);
                adminIT.setVisible(true);
            }
            else if (upperRole.contains("IMMEDIATE SUPERVISOR") || 
                     upperRole.contains("SUPERVISOR") || 
                     upperRole.contains("MANAGER")) {
                AdminSupervisor adminSupervisor = new AdminSupervisor(loggedInUser);
                adminSupervisor.setVisible(true);
            }
            else if (upperRole.contains("EMPLOYEE")) {
                EmployeeSelfService employeeSelfService = new EmployeeSelfService(loggedInUser);
                employeeSelfService.setVisible(true);
            }
            else {
                // Unknown role - redirect to login for security
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
        backattnddtlsbttn = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        AttendanceDetailsTbl = new javax.swing.JTable();
        otRequestsBtn = new javax.swing.JButton();
        jPanel6 = new javax.swing.JPanel();
        InputIDNo3 = new javax.swing.JLabel();
        Date2 = new java.awt.Label();
        dateFilter = new javax.swing.JComboBox<>();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel1.setBackground(new java.awt.Color(220, 95, 0));
        jPanel1.setPreferredSize(new java.awt.Dimension(211, 57));

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(255, 255, 255));
        jLabel1.setText("ATTENDANCE DETAILS");

        backattnddtlsbttn.setBackground(new java.awt.Color(207, 10, 10));
        backattnddtlsbttn.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        backattnddtlsbttn.setForeground(new java.awt.Color(255, 255, 255));
        backattnddtlsbttn.setText("Back");
        backattnddtlsbttn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                backattnddtlsbttnActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(25, 25, 25)
                .addComponent(backattnddtlsbttn)
                .addGap(39, 39, 39)
                .addComponent(jLabel1)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(backattnddtlsbttn))
                .addContainerGap(13, Short.MAX_VALUE))
        );

        jPanel2.setBackground(new java.awt.Color(255, 255, 255));
        jPanel2.setPreferredSize(new java.awt.Dimension(984, 442));
        jPanel2.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        AttendanceDetailsTbl.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null}
            },
            new String [] {
                "Employee ID", "Date", "Time In", "Time Out", "Hours Worked", "Late Hours", "Overtime Hours"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        AttendanceDetailsTbl.setColumnSelectionAllowed(true);
        jScrollPane1.setViewportView(AttendanceDetailsTbl);
        AttendanceDetailsTbl.getColumnModel().getSelectionModel().setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);

        jPanel2.add(jScrollPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 71, 830, 300));

        otRequestsBtn.setBackground(new java.awt.Color(220, 95, 0));
        otRequestsBtn.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        otRequestsBtn.setForeground(new java.awt.Color(255, 255, 255));
        otRequestsBtn.setText("Overtime Requests");
        otRequestsBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                otRequestsBtnActionPerformed(evt);
            }
        });
        jPanel2.add(otRequestsBtn, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 390, 220, 45));

        jPanel6.setBackground(new java.awt.Color(220, 95, 0));

        InputIDNo3.setForeground(new java.awt.Color(255, 255, 255));

        Date2.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        Date2.setForeground(new java.awt.Color(255, 255, 255));
        Date2.setText("Date:");

        dateFilter.setEditable(true);
        dateFilter.setForeground(new java.awt.Color(255, 255, 255));
        dateFilter.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Select", "2024-06", "2024-07", "2024-08", "2024-09", "2024-10", "2024-11", "2024-12" }));
        dateFilter.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dateFilterActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addGap(25, 25, 25)
                .addComponent(Date2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(23, 23, 23)
                .addComponent(dateFilter, javax.swing.GroupLayout.PREFERRED_SIZE, 155, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(565, 565, 565)
                .addComponent(InputIDNo3, javax.swing.GroupLayout.PREFERRED_SIZE, 321, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(dateFilter, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel6Layout.createSequentialGroup()
                            .addGap(15, 15, 15)
                            .addComponent(InputIDNo3))
                        .addGroup(jPanel6Layout.createSequentialGroup()
                            .addContainerGap()
                            .addComponent(Date2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel2.add(jPanel6, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 20, 250, 40));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 870, Short.MAX_VALUE)
            .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, 870, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void backattnddtlsbttnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_backattnddtlsbttnActionPerformed
      // Navigate back to appropriate dashboard based on user role
        goBack();      
    }//GEN-LAST:event_backattnddtlsbttnActionPerformed

    private void otRequestsBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_otRequestsBtnActionPerformed
        // Opens the Overtime Requests Pop-up Window
        showOvertimeRequestsPopup();
    }//GEN-LAST:event_otRequestsBtnActionPerformed

    private void dateFilterActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dateFilterActionPerformed
        // Apply the date filter immediately when selection changes
        filterAttendanceByMonth();
    }//GEN-LAST:event_dateFilterActionPerformed
                                         
    /**
     * Data model for attendance records with calculated hours.
     * Maps to the attendance table with business logic calculations.
     */
    public static class AttendanceRecord {
        public int employeeId;
        public LocalDate date;
        public LocalTime timeIn;
        public LocalTime timeOut;
        public double hoursWorked;    // Regular hours only (max 8 hours)
        public double lateHours;      // Hours late if after 8:10 AM
        public double overtimeHours;  // Hours beyond 8-hour workday
        public String payMonth;       // Format: YYYY-MM

        @Override
        public String toString() {
            return String.format("AttendanceRecord{employeeId=%d, date=%s, timeIn=%s, timeOut=%s, hoursWorked=%.2f, lateHours=%.2f, overtimeHours=%.2f, payMonth='%s'}", 
                employeeId, date, timeIn, timeOut, hoursWorked, lateHours, overtimeHours, payMonth);
        }
    }

    public static void main(String[] args) {
        java.awt.EventQueue.invokeLater(() -> {
            System.out.println("AttendanceDetailsGUI requires a valid UserAuthenticationModel instance.");
            System.out.println("Please use: new AttendanceDetailsGUI(loggedInUser).setVisible(true);");
        });
    }



    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTable AttendanceDetailsTbl;
    private java.awt.Label Date2;
    private javax.swing.JLabel InputIDNo3;
    private javax.swing.JButton backattnddtlsbttn;
    private javax.swing.JComboBox<String> dateFilter;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JButton otRequestsBtn;
    // End of variables declaration//GEN-END:variables
}
