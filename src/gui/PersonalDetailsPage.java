package gui;

import DAOs.UserAuthenticationDAO;
import Models.UserAuthenticationModel;
import java.awt.HeadlessException;
import java.sql.*;
import javax.swing.JOptionPane;

public class PersonalDetailsPage extends javax.swing.JFrame {

    private String employeeID;
    private String userRole;
    private UserAuthenticationModel loggedInUser;
    private UserAuthenticationDAO userAuthDAO;
     
    /**
     * Creates a new PersonalDetailsPage for the specified user
     * This constructor is used from all dashboard screens (AdminIT, AdminHR, etc.)
     * @param user The currently logged in user
     */
    public PersonalDetailsPage(UserAuthenticationModel user) {
        initComponents();
        
        // Store the user object for later use
        this.loggedInUser = user;
        this.userAuthDAO = new UserAuthenticationDAO();
        
        // Get user details from the UserAuthenticationModel object
        this.employeeID = String.valueOf(user.getEmployeeId());
        this.userRole = user.getUserRole();
        
        // Set window title
        setTitle("Personal Details - " + user.getFirstName() + " " + user.getLastName());
        
        // Center window on screen
        setLocationRelativeTo(null);
        setResizable(false);
        
        // Debug output to trace execution
        System.out.println("PersonalDetailsPage constructor called for user: " + 
                           user.getDisplayName() + " (ID: " + employeeID + ", Role: " + userRole + ")");
        
        // Load and display the employee details
        loadEmployeeDetails();
    }
    
    /**
     * Default constructor required by NetBeans Form Editor
     * Not used directly in the application
     */
    public PersonalDetailsPage() {
        initComponents();
        this.userAuthDAO = new UserAuthenticationDAO();
        setLocationRelativeTo(null);
        setResizable(false);
        System.out.println("PersonalDetailsPage default constructor called (not normally used)");
    }

    /**
     * Loads employee details from the MySQL database and displays them in the UI
     */
    private void loadEmployeeDetails() {
        try {
            System.out.println("Loading employee details for ID: " + employeeID);
            
            // Get detailed employee information from database
            EmployeeDetails details = getEmployeeDetailsFromDatabase(Integer.parseInt(employeeID));
            
            if (details != null) {
                System.out.println("Successfully found employee record for ID: " + employeeID);
                
                // Update UI labels with employee data
                inputempidLBL.setText(String.valueOf(details.employeeId));
                inputfirstnameLBL.setText(details.firstName != null ? details.firstName : "N/A");
                inputlastnameLBL.setText(details.lastName != null ? details.lastName : "N/A");
                inputbdayLBL.setText(details.birthDate != null ? details.birthDate : "Not available");
                inputstatusLBL.setText(details.status != null ? details.status : "Unknown");
                inputpositionLBL.setText(details.position != null ? details.position : userRole);
                
                // Handle supervisor name
                if (details.supervisorName != null && !details.supervisorName.isEmpty() && 
                    !details.supervisorName.equals("N/A") && !details.supervisorName.contains("null")) {
                    inputsupervisorLBL.setText(details.supervisorName);
                } else {
                    inputsupervisorLBL.setText("None");
                }
                
                // Handle address - show full formatted address or fallback message
                if (details.address != null && !details.address.trim().isEmpty()) {
                    inputaddressLBL.setText(details.address);
                    System.out.println("Displaying address: " + details.address);
                } else {
                    inputaddressLBL.setText("No address on file");
                    System.out.println("No address found for employee");
                }
                
                // Handle phone number - should work since it's directly in employee table
                if (details.phoneNumber != null && !details.phoneNumber.trim().isEmpty()) {
                    inputphonenumLBL.setText(details.phoneNumber);
                    System.out.println("Displaying phone: " + details.phoneNumber);
                } else {
                    inputphonenumLBL.setText("No phone number on file");
                    System.out.println("No phone number found for employee");
                }
                
            } else {
                System.err.println("No employee record found for ID: " + employeeID);
                JOptionPane.showMessageDialog(this, 
                    "Could not find employee record for ID: " + employeeID, 
                    "Employee Not Found", JOptionPane.WARNING_MESSAGE);
                displayBasicUserInfo();
            }
        } catch (HeadlessException | NumberFormatException e) {
            System.err.println("Error loading employee details: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, 
                "Error loading employee details: " + e.getMessage(), 
                "Data Error", JOptionPane.ERROR_MESSAGE);
            displayBasicUserInfo();
        }
    }
    
    /**
     * Display basic user information from UserAuthenticationModel
     */
    private void displayBasicUserInfo() {
        inputempidLBL.setText(employeeID);
        inputfirstnameLBL.setText(loggedInUser.getFirstName());
        inputlastnameLBL.setText(loggedInUser.getLastName());
        inputbdayLBL.setText("Contact HR for details");
        inputaddressLBL.setText("Contact HR for address details");
        inputphonenumLBL.setText("Contact HR for phone details");
        inputstatusLBL.setText(loggedInUser.getStatus() != null ? loggedInUser.getStatus() : "Active");
        inputpositionLBL.setText(loggedInUser.getPosition() != null ? loggedInUser.getPosition() : userRole);
        inputsupervisorLBL.setText("Contact HR for details");
    }
    
    /**
     * Get detailed employee information from the database with proper address joins
     * @param employeeId Employee ID to lookup
     * @return EmployeeDetails object with all available information
     */
    private EmployeeDetails getEmployeeDetailsFromDatabase(int employeeId) {
        // SQL query to properly join address tables
        String sql = """
            SELECT e.employeeId, e.firstName, e.lastName, e.birthDate, e.status,
                   e.phoneNumber,
                   p.position,
                   CONCAT(supervisor.firstName, ' ', supervisor.lastName) as supervisorName,
                   CONCAT_WS(', ', 
                       NULLIF(a.street, ''), 
                       NULLIF(a.barangay, ''), 
                       NULLIF(a.city, ''), 
                       NULLIF(a.province, ''), 
                       NULLIF(a.zipCode, '')
                   ) as fullAddress
            FROM employee e
            LEFT JOIN position p ON e.positionId = p.positionId
            LEFT JOIN employee supervisor ON e.supervisorId = supervisor.employeeId
            LEFT JOIN employeeaddress ea ON e.employeeId = ea.employeeId
            LEFT JOIN address a ON ea.addressId = a.addressId
            WHERE e.employeeId = ?
            """;
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, employeeId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                EmployeeDetails details = new EmployeeDetails();
                details.employeeId = rs.getInt("employeeId");
                details.firstName = rs.getString("firstName");
                details.lastName = rs.getString("lastName");
                details.birthDate = rs.getString("birthDate");
                details.status = rs.getString("status");
                details.position = rs.getString("position");
                details.supervisorName = rs.getString("supervisorName");
                
                // Get phone number directly from employee table
                details.phoneNumber = rs.getString("phoneNumber");
                
                // Get formatted address from joined tables
                details.address = rs.getString("fullAddress");
                
                // Debug output
                System.out.println("Retrieved employee details:");
                System.out.println("- Phone: " + details.phoneNumber);
                System.out.println("- Address: " + details.address);
                
                return details;
            } else {
                System.out.println("No employee found with ID: " + employeeId);
            }
            
        } catch (SQLException e) {
            System.err.println("Database error getting employee details: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * Get database connection using same credentials as UserAuthenticationDAO
     */
    private Connection getConnection() throws SQLException {
        String DB_URL = "jdbc:mysql://localhost:3306/payrollsystem_db";
        String DB_USER = "root";
        String DB_PASSWORD = "Mmdc_2025*";
        
        Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        
        // Set connection timezone to Manila
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("SET time_zone = '+08:00'");
        }
        
        return conn;
    }
    
    /**
     * Navigate back to the appropriate dashboard based on user role
     */
    private void navigateBackToDashboard() {
        if (loggedInUser == null || userRole == null) {
            // No logged in user, go to login
            new Login().setVisible(true);
            this.dispose();
            return;
        }
        
        try {
            String upperRole = userRole.toUpperCase();
            
            // Priority 1: Check for major roles first (HR, Accounting, IT)
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
            // Priority 2: Check for supervisor roles
            else if (upperRole.contains("IMMEDIATE SUPERVISOR") || 
                     upperRole.contains("SUPERVISOR") || 
                     upperRole.contains("MANAGER")) {
                AdminSupervisor adminSupervisor = new AdminSupervisor(loggedInUser);
                adminSupervisor.setVisible(true);
            }
            // Priority 3: Regular employee
            else if (upperRole.contains("EMPLOYEE")) {
                EmployeeSelfService employeeSelfService = new EmployeeSelfService(loggedInUser);
                employeeSelfService.setVisible(true);
            }
            else {
                // Unknown role, go to login
                JOptionPane.showMessageDialog(this, 
                    "Unknown user role: " + userRole + "\nRedirecting to login.", 
                    "Role Error", 
                    JOptionPane.WARNING_MESSAGE);
                new Login().setVisible(true);
            }
            
            // Close the current page
            this.dispose();
            
        } catch (Exception e) {
            System.err.println("Error navigating back to dashboard: " + e.getMessage());
            JOptionPane.showMessageDialog(this, 
                "Error returning to dashboard. Redirecting to login.", 
                "Navigation Error", 
                JOptionPane.ERROR_MESSAGE);
            new Login().setVisible(true);
            this.dispose();
        }
    }
    
    /**
     * Inner class to hold employee details
     */
    private static class EmployeeDetails {
        int employeeId;
        String firstName;
        String lastName;
        String birthDate;
        String address;        // Formatted full address from address table
        String phoneNumber;    // Direct from employee table
        String status;
        String position;
        String supervisorName;
    }



    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        detailsmainPNL = new javax.swing.JPanel();
        detailsheaderPNL = new javax.swing.JPanel();
        detailsheaderLBL = new javax.swing.JLabel();
        backtoemppagePB = new javax.swing.JButton();
        infoboxPNL = new javax.swing.JPanel();
        dtfirstnameLBL = new javax.swing.JLabel();
        dtlastnameLBL = new javax.swing.JLabel();
        dtbdayLBL = new javax.swing.JLabel();
        dtphonenumLBL = new javax.swing.JLabel();
        dtstatusLBL = new javax.swing.JLabel();
        dtpositionLBL = new javax.swing.JLabel();
        dtsupervisorLBL = new javax.swing.JLabel();
        inputfirstnameLBL = new javax.swing.JLabel();
        inputlastnameLBL = new javax.swing.JLabel();
        inputbdayLBL = new javax.swing.JLabel();
        inputphonenumLBL = new javax.swing.JLabel();
        inputstatusLBL = new javax.swing.JLabel();
        inputpositionLBL = new javax.swing.JLabel();
        inputsupervisorLBL = new javax.swing.JLabel();
        dtemployeeidLBL = new javax.swing.JLabel();
        inputempidLBL = new javax.swing.JLabel();
        dtaddressLBL = new javax.swing.JLabel();
        inputaddressLBL = new javax.swing.JLabel();
        detailsiconPNL = new javax.swing.JPanel();
        icondetailsLBL = new javax.swing.JLabel();
        notedetailsLBL = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        detailsmainPNL.setBackground(new java.awt.Color(255, 255, 255));

        detailsheaderPNL.setBackground(new java.awt.Color(220, 95, 0));

        detailsheaderLBL.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        detailsheaderLBL.setForeground(new java.awt.Color(255, 255, 255));
        detailsheaderLBL.setText("EMPLOYEE'S PERSONAL DETAILS");

        backtoemppagePB.setBackground(new java.awt.Color(204, 0, 0));
        backtoemppagePB.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        backtoemppagePB.setForeground(new java.awt.Color(255, 255, 255));
        backtoemppagePB.setText("Back");
        backtoemppagePB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                backtoemppagePBActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout detailsheaderPNLLayout = new javax.swing.GroupLayout(detailsheaderPNL);
        detailsheaderPNL.setLayout(detailsheaderPNLLayout);
        detailsheaderPNLLayout.setHorizontalGroup(
            detailsheaderPNLLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(detailsheaderPNLLayout.createSequentialGroup()
                .addGap(22, 22, 22)
                .addComponent(backtoemppagePB)
                .addGap(249, 249, 249)
                .addComponent(detailsheaderLBL, javax.swing.GroupLayout.PREFERRED_SIZE, 386, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        detailsheaderPNLLayout.setVerticalGroup(
            detailsheaderPNLLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(detailsheaderPNLLayout.createSequentialGroup()
                .addGap(31, 31, 31)
                .addGroup(detailsheaderPNLLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(detailsheaderLBL)
                    .addComponent(backtoemppagePB))
                .addContainerGap(24, Short.MAX_VALUE))
        );

        infoboxPNL.setBackground(new java.awt.Color(255, 255, 255));
        infoboxPNL.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));

        dtfirstnameLBL.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        dtfirstnameLBL.setForeground(new java.awt.Color(102, 102, 102));
        dtfirstnameLBL.setText("First Name:");

        dtlastnameLBL.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        dtlastnameLBL.setForeground(new java.awt.Color(102, 102, 102));
        dtlastnameLBL.setText("Last Name:");

        dtbdayLBL.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        dtbdayLBL.setForeground(new java.awt.Color(102, 102, 102));
        dtbdayLBL.setText("Birthday");

        dtphonenumLBL.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        dtphonenumLBL.setForeground(new java.awt.Color(102, 102, 102));
        dtphonenumLBL.setText("Phone Number:");

        dtstatusLBL.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        dtstatusLBL.setForeground(new java.awt.Color(102, 102, 102));
        dtstatusLBL.setText("Status:");

        dtpositionLBL.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        dtpositionLBL.setForeground(new java.awt.Color(102, 102, 102));
        dtpositionLBL.setText("Position:");

        dtsupervisorLBL.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        dtsupervisorLBL.setForeground(new java.awt.Color(102, 102, 102));
        dtsupervisorLBL.setText("Immediate Supervisor:");

        inputfirstnameLBL.setFont(new java.awt.Font("Helvetica", 1, 12)); // NOI18N
        inputfirstnameLBL.setForeground(new java.awt.Color(102, 102, 102));
        inputfirstnameLBL.setText(". . .");

        inputlastnameLBL.setFont(new java.awt.Font("Helvetica", 1, 12)); // NOI18N
        inputlastnameLBL.setForeground(new java.awt.Color(102, 102, 102));
        inputlastnameLBL.setText(". . .");

        inputbdayLBL.setFont(new java.awt.Font("Helvetica", 1, 12)); // NOI18N
        inputbdayLBL.setForeground(new java.awt.Color(102, 102, 102));
        inputbdayLBL.setText(". . .");

        inputphonenumLBL.setFont(new java.awt.Font("Helvetica", 1, 12)); // NOI18N
        inputphonenumLBL.setForeground(new java.awt.Color(102, 102, 102));
        inputphonenumLBL.setText(". . .");

        inputstatusLBL.setFont(new java.awt.Font("Helvetica", 1, 14)); // NOI18N
        inputstatusLBL.setForeground(new java.awt.Color(102, 102, 102));
        inputstatusLBL.setText(". . .");

        inputpositionLBL.setFont(new java.awt.Font("Helvetica", 1, 12)); // NOI18N
        inputpositionLBL.setForeground(new java.awt.Color(102, 102, 102));
        inputpositionLBL.setText(". . .");

        inputsupervisorLBL.setFont(new java.awt.Font("Helvetica", 1, 12)); // NOI18N
        inputsupervisorLBL.setForeground(new java.awt.Color(102, 102, 102));
        inputsupervisorLBL.setText(". . .");

        dtemployeeidLBL.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        dtemployeeidLBL.setForeground(new java.awt.Color(102, 102, 102));
        dtemployeeidLBL.setText("Employee ID:");

        inputempidLBL.setFont(new java.awt.Font("Helvetica", 1, 12)); // NOI18N
        inputempidLBL.setForeground(new java.awt.Color(102, 102, 102));
        inputempidLBL.setText(". . .");

        dtaddressLBL.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        dtaddressLBL.setForeground(new java.awt.Color(102, 102, 102));
        dtaddressLBL.setText("Address");

        inputaddressLBL.setFont(new java.awt.Font("Helvetica", 1, 12)); // NOI18N
        inputaddressLBL.setForeground(new java.awt.Color(102, 102, 102));
        inputaddressLBL.setText(". . .");

        javax.swing.GroupLayout infoboxPNLLayout = new javax.swing.GroupLayout(infoboxPNL);
        infoboxPNL.setLayout(infoboxPNLLayout);
        infoboxPNLLayout.setHorizontalGroup(
            infoboxPNLLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(infoboxPNLLayout.createSequentialGroup()
                .addGap(38, 38, 38)
                .addGroup(infoboxPNLLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(infoboxPNLLayout.createSequentialGroup()
                        .addComponent(dtsupervisorLBL)
                        .addGap(18, 18, 18)
                        .addComponent(inputsupervisorLBL, javax.swing.GroupLayout.PREFERRED_SIZE, 325, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(infoboxPNLLayout.createSequentialGroup()
                        .addGroup(infoboxPNLLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(dtfirstnameLBL, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(dtlastnameLBL, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(dtbdayLBL, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(dtphonenumLBL)
                            .addComponent(dtstatusLBL)
                            .addComponent(dtpositionLBL)
                            .addComponent(dtemployeeidLBL, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(dtaddressLBL, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(66, 66, 66)
                        .addGroup(infoboxPNLLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(inputempidLBL, javax.swing.GroupLayout.PREFERRED_SIZE, 325, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(inputpositionLBL, javax.swing.GroupLayout.PREFERRED_SIZE, 325, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(inputstatusLBL, javax.swing.GroupLayout.PREFERRED_SIZE, 325, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(inputphonenumLBL, javax.swing.GroupLayout.PREFERRED_SIZE, 325, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(inputbdayLBL, javax.swing.GroupLayout.PREFERRED_SIZE, 325, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(inputlastnameLBL, javax.swing.GroupLayout.PREFERRED_SIZE, 325, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(inputfirstnameLBL, javax.swing.GroupLayout.PREFERRED_SIZE, 325, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(inputaddressLBL, javax.swing.GroupLayout.DEFAULT_SIZE, 523, Short.MAX_VALUE))))
                .addContainerGap())
        );
        infoboxPNLLayout.setVerticalGroup(
            infoboxPNLLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(infoboxPNLLayout.createSequentialGroup()
                .addGap(24, 24, 24)
                .addGroup(infoboxPNLLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(dtemployeeidLBL)
                    .addComponent(inputempidLBL))
                .addGap(18, 18, 18)
                .addGroup(infoboxPNLLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(dtfirstnameLBL)
                    .addComponent(inputfirstnameLBL))
                .addGap(18, 18, 18)
                .addGroup(infoboxPNLLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(dtlastnameLBL)
                    .addComponent(inputlastnameLBL))
                .addGap(18, 18, 18)
                .addGroup(infoboxPNLLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(dtbdayLBL)
                    .addComponent(inputbdayLBL))
                .addGap(18, 18, 18)
                .addGroup(infoboxPNLLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(dtaddressLBL)
                    .addComponent(inputaddressLBL))
                .addGap(18, 18, 18)
                .addGroup(infoboxPNLLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(dtphonenumLBL)
                    .addComponent(inputphonenumLBL))
                .addGap(18, 18, 18)
                .addGroup(infoboxPNLLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(dtstatusLBL)
                    .addComponent(inputstatusLBL))
                .addGap(18, 18, 18)
                .addGroup(infoboxPNLLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(dtpositionLBL)
                    .addComponent(inputpositionLBL))
                .addGap(18, 18, 18)
                .addGroup(infoboxPNLLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(dtsupervisorLBL)
                    .addComponent(inputsupervisorLBL))
                .addContainerGap(25, Short.MAX_VALUE))
        );

        detailsiconPNL.setBackground(new java.awt.Color(220, 95, 0));

        icondetailsLBL.setIcon(new javax.swing.ImageIcon(getClass().getResource("/media/USER 128 X 128.png"))); // NOI18N

        javax.swing.GroupLayout detailsiconPNLLayout = new javax.swing.GroupLayout(detailsiconPNL);
        detailsiconPNL.setLayout(detailsiconPNLLayout);
        detailsiconPNLLayout.setHorizontalGroup(
            detailsiconPNLLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(detailsiconPNLLayout.createSequentialGroup()
                .addGap(34, 34, 34)
                .addComponent(icondetailsLBL)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        detailsiconPNLLayout.setVerticalGroup(
            detailsiconPNLLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(detailsiconPNLLayout.createSequentialGroup()
                .addGap(21, 21, 21)
                .addComponent(icondetailsLBL, javax.swing.GroupLayout.PREFERRED_SIZE, 138, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(26, Short.MAX_VALUE))
        );

        notedetailsLBL.setFont(new java.awt.Font("Helvetica", 3, 12)); // NOI18N
        notedetailsLBL.setForeground(new java.awt.Color(51, 51, 51));
        notedetailsLBL.setText("Please contact HR for any revisions.");

        javax.swing.GroupLayout detailsmainPNLLayout = new javax.swing.GroupLayout(detailsmainPNL);
        detailsmainPNL.setLayout(detailsmainPNLLayout);
        detailsmainPNLLayout.setHorizontalGroup(
            detailsmainPNLLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(detailsheaderPNL, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, detailsmainPNLLayout.createSequentialGroup()
                .addGap(19, 19, 19)
                .addGroup(detailsmainPNLLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(notedetailsLBL, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(detailsiconPNL, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 29, Short.MAX_VALUE)
                .addComponent(infoboxPNL, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(26, 26, 26))
        );
        detailsmainPNLLayout.setVerticalGroup(
            detailsmainPNLLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(detailsmainPNLLayout.createSequentialGroup()
                .addComponent(detailsheaderPNL, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(detailsmainPNLLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(detailsmainPNLLayout.createSequentialGroup()
                        .addComponent(infoboxPNL, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 25, Short.MAX_VALUE))
                    .addGroup(detailsmainPNLLayout.createSequentialGroup()
                        .addComponent(detailsiconPNL, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(31, 31, 31)
                        .addComponent(notedetailsLBL)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(detailsmainPNL, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(detailsmainPNL, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void backtoemppagePBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_backtoemppagePBActionPerformed
    // Back button - navigate to appropriate dashboard
         navigateBackToDashboard();       
        
    
    }//GEN-LAST:event_backtoemppagePBActionPerformed
    
    
      /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(PersonalDetailsPage.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }

        java.awt.EventQueue.invokeLater(() -> {
            new PersonalDetailsPage().setVisible(true);
        });
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton backtoemppagePB;
    private javax.swing.JLabel detailsheaderLBL;
    private javax.swing.JPanel detailsheaderPNL;
    private javax.swing.JPanel detailsiconPNL;
    private javax.swing.JPanel detailsmainPNL;
    private javax.swing.JLabel dtaddressLBL;
    private javax.swing.JLabel dtbdayLBL;
    private javax.swing.JLabel dtemployeeidLBL;
    private javax.swing.JLabel dtfirstnameLBL;
    private javax.swing.JLabel dtlastnameLBL;
    private javax.swing.JLabel dtphonenumLBL;
    private javax.swing.JLabel dtpositionLBL;
    private javax.swing.JLabel dtstatusLBL;
    private javax.swing.JLabel dtsupervisorLBL;
    private javax.swing.JLabel icondetailsLBL;
    private javax.swing.JPanel infoboxPNL;
    private javax.swing.JLabel inputaddressLBL;
    private javax.swing.JLabel inputbdayLBL;
    private javax.swing.JLabel inputempidLBL;
    private javax.swing.JLabel inputfirstnameLBL;
    private javax.swing.JLabel inputlastnameLBL;
    private javax.swing.JLabel inputphonenumLBL;
    private javax.swing.JLabel inputpositionLBL;
    private javax.swing.JLabel inputstatusLBL;
    private javax.swing.JLabel inputsupervisorLBL;
    private javax.swing.JLabel notedetailsLBL;
    // End of variables declaration//GEN-END:variables
}
