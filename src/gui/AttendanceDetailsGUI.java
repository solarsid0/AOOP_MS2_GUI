package gui;

import Models.AttendanceModel;
import Models.UserAuthenticationModel;
import Services.AttendanceService;
import DAOs.DatabaseConnection;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * GUI for displaying employee attendance details with filtering capabilities.
 * Provides access to overtime request functionality for eligible employees.
 */
public class AttendanceDetailsGUI extends javax.swing.JFrame {

    private AttendanceService attendanceService;
    private String employeeId;
    private String employeeName;
    private UserAuthenticationModel loggedInUser;

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * Constructor initializes the attendance details GUI for the logged-in user.
     * Sets up service dependencies and loads initial data.
     */
    public AttendanceDetailsGUI(UserAuthenticationModel loggedInUser) {
        this.loggedInUser = loggedInUser;
        this.employeeId = String.valueOf(loggedInUser.getEmployeeId());
        this.employeeName = loggedInUser.getFirstName() + " " + loggedInUser.getLastName();

        initComponents();
        
        this.setLocationRelativeTo(null);
        
        // Initialize service layer
        DatabaseConnection databaseConnection = new DatabaseConnection();
        this.attendanceService = new AttendanceService(databaseConnection);

        setTitle("Attendance Details - " + employeeName);
        
        initializeDateFilter();
        setupOvertimeButtonVisibility();
        loadAttendanceData();
    }

    /**
     * Populates the date filter dropdown with available months.
     * Defaults to "Select" option for initial state.
     */
    private void initializeDateFilter() {
        dateFilter.removeAllItems();
        dateFilter.addItem("Select");
        dateFilter.addItem("2024-06");
        dateFilter.addItem("2024-07");
        dateFilter.addItem("2024-08");
        dateFilter.addItem("2024-09");
        dateFilter.addItem("2024-10");
        dateFilter.addItem("2024-11");
        dateFilter.addItem("2024-12");
        dateFilter.setSelectedItem("Select");
    }

    /**
     * Sets up overtime button visibility based on employee rank-and-file status.
     * Only rank-and-file employees can access overtime requests.
     */
    private void setupOvertimeButtonVisibility() {
        try {
            boolean isRankAndFile = attendanceService.isRankAndFileEmployee(Integer.parseInt(employeeId));
            otRequestsBtn.setVisible(isRankAndFile);
        } catch (Exception e) {
            System.err.println("Error checking employee rank for overtime button: " + e.getMessage());
            otRequestsBtn.setVisible(false);
        }
    }

    /**
     * Loads attendance data without any date filter applied.
     * Uses default date range from service.
     */
    private void loadAttendanceData() {
        loadAttendanceData(null);
    }

    /**
     * Loads attendance data with optional date filtering.
     * Updates the table with calculated hours based on employee type.
     */
    private void loadAttendanceData(String dateFilter) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Use AttendanceService to get calculated attendance records
                List<AttendanceModel> attendanceRecords = attendanceService.getAttendanceRecordsWithCalculatedHours(
                    Integer.parseInt(employeeId), dateFilter);
                
                DefaultTableModel model = (DefaultTableModel) AttendanceDetailsTbl.getModel();
                model.setRowCount(0);

                // Populate table with attendance data
                for (AttendanceModel attendance : attendanceRecords) {
                    model.addRow(new Object[]{
                            employeeId,
                            attendance.getDate() != null ? attendance.getDate().toLocalDate().format(dateFormatter) : "N/A",
                            attendance.getTimeIn() != null ? attendance.getTimeIn().toLocalTime().format(timeFormatter) : "N/A",
                            attendance.getTimeOut() != null ? attendance.getTimeOut().toLocalTime().format(timeFormatter) : "N/A",
                            String.format("%.2f", attendance.getComputedHours()),
                            String.format("%.2f", attendance.getLateHours()),
                            String.format("%.2f", attendance.getOvertimeHours())
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
     * Opens overtime request popup for eligible employees.
     * Overtime requests are saved to the overtimerequest table.
     */
    private void showOvertimeRequestsPopup() {
        try {
            // Double-check eligibility before opening popup
            boolean isRankAndFile = attendanceService.isRankAndFileEmployee(Integer.parseInt(employeeId));
            if (!isRankAndFile) {
                JOptionPane.showMessageDialog(this, 
                    "Only rank-and-file employees are eligible for overtime requests.", 
                    "Not Eligible", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            // Open overtime request popup which handles database operations
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

    /**
     * Applies date filter to attendance data when selection changes.
     * Handles "Select" option by loading data without filter.
     */
    private void filterAttendanceByMonth() {
        String selectedFilter = (String) dateFilter.getSelectedItem();
        if (selectedFilter != null && !selectedFilter.equals("Select")) {
            loadAttendanceData(selectedFilter);
        } else {
            loadAttendanceData(null);
        }
    }

    /**
     * Handles back button navigation to appropriate dashboard.
     * Routes user based on role hierarchy.
     */
    private void goBack() {
        navigateBackToDashboard();
    }

    /**
     * Navigates back to the appropriate dashboard based on user role.
     * Role priority: HR > Accounting > IT > Supervisor > Employee
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
            
            // Route to appropriate dashboard based on role
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
