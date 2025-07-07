package gui;

import DAOs.DatabaseConnection;
import DAOs.UserAuthenticationDAO;
import Models.UserAuthenticationModel;
import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

/**
 * This GUI is responsible for handling user login using MySQL database
 * It checks the user's email and password using DAO pattern, then sends them to the right landing page according to their user role!
 * @author Admin
 */
public class Login extends javax.swing.JFrame {

    private final UserAuthenticationDAO userAuthDAO;
    private final DatabaseConnection databaseConnection;

    public Login() {
        // Initialize database connection and DAO
        databaseConnection = new DatabaseConnection();
        userAuthDAO = new UserAuthenticationDAO();
        
        // Test database connection
        if (!databaseConnection.testConnection()) {
            JOptionPane.showMessageDialog(this, 
                "Database connection failed! Please check your database settings.", 
                "Connection Error", 
                JOptionPane.ERROR_MESSAGE);
        }
        
        initComponents();
        setupKeyListeners();
        
        // TO CENTER THE WINDOW
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();  // Size the window to fit components
        setLocationRelativeTo(null);  // Center the window on screen
        setResizable(false);  // Optional: prevent resizing
    }
    
    /**
     * Setup keyboard listeners for better user experience
     */
    private void setupKeyListeners() {
        // Add Enter key listener to password field
        jPasswordField1.addKeyListener(new KeyListener() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    login();
                }
            }
            
            @Override
            public void keyTyped(KeyEvent e) {}
            
            @Override
            public void keyReleased(KeyEvent e) {}
        });
        
        // Add Enter key listener to email field
        txtEmail.addKeyListener(new KeyListener() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    jPasswordField1.requestFocus();
                }
            }
            
            @Override
            public void keyTyped(KeyEvent e) {}
            
            @Override
            public void keyReleased(KeyEvent e) {}
        });
    }

    /**
     * Main login method using MySQL DAO
     */
    private void login() {
        String email = txtEmail.getText().trim();
        String password = new String(jPasswordField1.getPassword());

        // Validate input
        if (email.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "Please enter both email and password!", 
                "Input Error", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Validate email format
        if (!isValidEmail(email)) {
            JOptionPane.showMessageDialog(this, 
                "Please enter a valid email address!", 
                "Invalid Email", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            // Show loading cursor
            setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR));
            
            // Authenticate user using DAO
            UserAuthenticationModel user = userAuthDAO.authenticateUser(email, password);

            if (user != null) {
                // Check if account is locked
                if (user.isAccountLocked()) {
                    JOptionPane.showMessageDialog(this, 
                        "Account is temporarily locked due to multiple failed login attempts.\nPlease try again later.", 
                        "Account Locked", 
                        JOptionPane.WARNING_MESSAGE);
                    return;
                }
                
                // Start user session
                String clientIP = getClientIP();
                String userAgent = getUserAgent();
                
                if (user.startSession(clientIP, userAgent)) {
                    System.out.println("Login successful for user: " + user.getEmail() + 
                                     " at " + user.getFormattedLastLogin());
                    
                    // Show success message
                    JOptionPane.showMessageDialog(this, 
                        "Welcome, " + user.getDisplayName() + "!\nLogin successful!", 
                        "Login Success", 
                        JOptionPane.INFORMATION_MESSAGE);
                    
                    // Redirect user based on role
                    redirectUserBasedOnRole(user);
                    
                } else {
                    JOptionPane.showMessageDialog(this, 
                        "Failed to create user session. Please try again.", 
                        "Session Error", 
                        JOptionPane.ERROR_MESSAGE);
                }
                
            } else {
                // Authentication failed
                System.out.println("Login failed for email: " + email);
                
                // Check if account exists but is locked
                if (userAuthDAO.emailExists(email)) {
                    UserAuthenticationModel existingUser = userAuthDAO.getUserByEmail(email);
                    if (existingUser != null && existingUser.isAccountLocked()) {
                        JOptionPane.showMessageDialog(this, 
                            "Account is locked due to multiple failed login attempts.", 
                            "Account Locked", 
                            JOptionPane.WARNING_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(this, 
                            "Invalid email or password!\nPlease check your credentials and try again.", 
                            "Login Failed", 
                            JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    JOptionPane.showMessageDialog(this, 
                        "Invalid email or password!\nPlease check your credentials and try again.", 
                        "Login Failed", 
                        JOptionPane.ERROR_MESSAGE);
                }
            }
            
        } catch (Exception e) {
            System.err.println("Login error: " + e.getMessage());
            JOptionPane.showMessageDialog(this, 
                "An error occurred during login. Please try again.\nError: " + e.getMessage(), 
                "Login Error", 
                JOptionPane.ERROR_MESSAGE);
        } finally {
            // Reset cursor
            setCursor(java.awt.Cursor.getDefaultCursor());
            
            // Clear password field for security
            jPasswordField1.setText("");
        }
    }

        /**
         * Redirect user to appropriate dashboard based on their role(s)
         * Handles multiple roles with priority: HR/Accounting/IT > Supervisor > Employee
         * @param user Authenticated user model
         */
        private void redirectUserBasedOnRole(UserAuthenticationModel user) {
            if (user == null) {
                JOptionPane.showMessageDialog(this, 
                    "User information is invalid!", 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
                return;
            }

            String role = user.getUserRole();
            if (role == null) {
                JOptionPane.showMessageDialog(this, 
                    "User role is not defined!", 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                // Convert to uppercase for case-insensitive comparison
                String upperRole = role.toUpperCase();

                // Priority 1: Check for major roles first (HR, Accounting, IT)
                // These take precedence even if user also has supervisor role
                if (upperRole.contains("HR")) {
                    AdminHR adminHR = new AdminHR(user);
                    adminHR.setVisible(true);
                } 
                else if (upperRole.contains("ACCOUNTING")) {
                    AdminAccounting adminAccounting = new AdminAccounting(user);
                    adminAccounting.setVisible(true);
                } 
                else if (upperRole.contains("IT")) {
                    AdminIT adminIT = new AdminIT(user);
                    adminIT.setVisible(true);
                }
                // Priority 2: Check for supervisor roles
                else if (upperRole.contains("IMMEDIATE SUPERVISOR") || 
                         upperRole.contains("SUPERVISOR") || 
                         upperRole.contains("MANAGER")) {
                    AdminSupervisor adminSupervisor = new AdminSupervisor(user);
                    adminSupervisor.setVisible(true);
                }
                // Priority 3: Regular employee
                else if (upperRole.contains("EMPLOYEE")) {
                    EmployeeSelfService employeeSelfService = new EmployeeSelfService(user);
                    employeeSelfService.setVisible(true);
                }
                else {
                    JOptionPane.showMessageDialog(this, 
                        "Unknown user role: " + role + "\nPlease contact system administrator.", 
                        "Role Error", 
                        JOptionPane.ERROR_MESSAGE);
                    return; // Don't close login window if role is invalid
                }

                // Close the login window after successful redirection
                this.dispose();

            } catch (Exception e) {
                System.err.println("Error opening dashboard for role " + role + ": " + e.getMessage());
                JOptionPane.showMessageDialog(this, 
                    "Error opening dashboard: " + e.getMessage() + 
                    "\nPlease contact system administrator.", 
                    "Dashboard Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    
    /**
     * Validate email format
     * @param email Email to validate
     * @return true if valid email format
     */
    private boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        
        // Simple email validation regex
        String emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        return email.matches(emailRegex);
    }
    
    /**
     * Get client IP address (placeholder implementation)
     * @return client IP
     */
    private String getClientIP() {
        try {
            return java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "unknown";
        }
    }
    
    /**
     * Get user agent (placeholder implementation)
     * @return user agent string
     */
    private String getUserAgent() {
        return "Java Swing Application - " + System.getProperty("os.name");
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        txtEmail = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        showPassword = new javax.swing.JCheckBox();
        forgetPassword = new javax.swing.JLabel();
        btnLogin = new javax.swing.JButton();
        jPasswordField1 = new javax.swing.JPasswordField();
        jPanel2 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setIconImages(null);
        getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jPanel1.setBackground(new java.awt.Color(255, 255, 255));

        jLabel2.setFont(new java.awt.Font("Segoe UI", 1, 36)); // NOI18N
        jLabel2.setText("LOGIN");

        jLabel3.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jLabel3.setText("Email");

        jLabel4.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jLabel4.setText("Password");

        showPassword.setText("Show password");
        showPassword.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showPasswordActionPerformed(evt);
            }
        });

        forgetPassword.setText("Forget password?");
        forgetPassword.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                forgetPasswordMouseClicked(evt);
            }
        });

        btnLogin.setBackground(new java.awt.Color(220, 95, 0));
        btnLogin.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        btnLogin.setForeground(new java.awt.Color(255, 255, 255));
        btnLogin.setText("Login");
        btnLogin.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnLoginActionPerformed(evt);
            }
        });

        jPasswordField1.setActionCommand("<Not Set>");
        jPasswordField1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jPasswordField1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(54, 54, 54)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(showPassword)
                                .addGap(115, 115, 115)
                                .addComponent(forgetPassword))
                            .addComponent(txtEmail, javax.swing.GroupLayout.PREFERRED_SIZE, 320, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jPasswordField1, javax.swing.GroupLayout.PREFERRED_SIZE, 320, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel3)
                            .addComponent(jLabel4)))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(156, 156, 156)
                        .addComponent(btnLogin, javax.swing.GroupLayout.PREFERRED_SIZE, 116, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(157, 157, 157)
                        .addComponent(jLabel2)))
                .addContainerGap(76, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(69, 69, 69)
                .addComponent(jLabel2)
                .addGap(36, 36, 36)
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtEmail, javax.swing.GroupLayout.PREFERRED_SIZE, 41, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(29, 29, 29)
                .addComponent(jLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPasswordField1, javax.swing.GroupLayout.PREFERRED_SIZE, 41, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(showPassword)
                    .addComponent(forgetPassword))
                .addGap(41, 41, 41)
                .addComponent(btnLogin, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(71, Short.MAX_VALUE))
        );

        getContentPane().add(jPanel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(485, 0, 450, 500));

        jPanel2.setLayout(null);
        getContentPane().add(jPanel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, -1, -1));

        jLabel1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/media/MPH_NEW_LOGIN2.jpg"))); // NOI18N
        getContentPane().add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(-3, 0, 490, -1));

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jPasswordField1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jPasswordField1ActionPerformed
        login();
    }//GEN-LAST:event_jPasswordField1ActionPerformed

    private void btnLoginActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLoginActionPerformed
        login();
    }//GEN-LAST:event_btnLoginActionPerformed

    private void forgetPasswordMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_forgetPasswordMouseClicked
        // Show password reset dialog
        String email = JOptionPane.showInputDialog(this,
            "Enter your email address to reset password:",
            "Password Reset",
            JOptionPane.QUESTION_MESSAGE);

        if (email != null && !email.trim().isEmpty()) {
            if (isValidEmail(email)) {
                if (userAuthDAO.emailExists(email)) {
                    JOptionPane.showMessageDialog(this,
                        "Password reset instructions have been sent to your email!\nPlease check your inbox and follow the instructions.",
                        "Password Reset",
                        JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this,
                        "Email address not found in our system.",
                        "Email Not Found",
                        JOptionPane.WARNING_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this,
                    "Please enter a valid email address.",
                    "Invalid Email",
                    JOptionPane.WARNING_MESSAGE);
            }
        }
    }//GEN-LAST:event_forgetPasswordMouseClicked

    private void showPasswordActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showPasswordActionPerformed
        if (showPassword.isSelected()) {
            jPasswordField1.setEchoChar((char) 0); // Show password
        } else {
            jPasswordField1.setEchoChar('•'); // Hide password
        }
    }//GEN-LAST:event_showPasswordActionPerformed

   
       
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
            java.util.logging.Logger.getLogger(Login.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Login.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Login.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Login.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Login().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnLogin;
    private javax.swing.JLabel forgetPassword;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPasswordField jPasswordField1;
    private javax.swing.JCheckBox showPassword;
    private javax.swing.JTextField txtEmail;
    // End of variables declaration//GEN-END:variables
}
