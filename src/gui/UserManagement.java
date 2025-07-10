package gui;

import DAOs.DatabaseConnection;
import DAOs.EmployeeDAO;
import DAOs.UserAuthenticationDAO;
import DAOs.PositionDAO;
import Models.EmployeeModel;
import Models.UserAuthenticationModel;
import Models.PositionModel;
import Utility.PasswordHasher;
import java.awt.GridLayout;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.*;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.table.DefaultTableModel;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Enhanced UserManagement class using DAO/Model architecture
 * This class allows IT admins to manage user credentials of all employees in the payroll system.
 * Uses existing Model business logic methods instead of implementing logic in GUI.
 * 
 * @author USER
 */
public class UserManagement extends javax.swing.JFrame {
    
    // Core dependencies
    private UserAuthenticationModel loggedInUser;
    private EmployeeDAO employeeDAO;
    private UserAuthenticationDAO userAuthDAO;
    private PositionDAO positionDAO;
    private DatabaseConnection databaseConnection;
    
    // UI components
    private PasswordProtectedTableModel passwordTableModel;
    
    // Department filter options
    private final String[] DEPARTMENT_OPTIONS = {
        "All", "Leadership", "IT", "HR", "Accounting", "Accounts", 
        "Sales and marketing", "Supply chain and logistics", "Customer service"
    };

    /**
     * Constructor with logged-in user
     * @param user The currently logged-in user
     */
    public UserManagement(UserAuthenticationModel user) {
        this.loggedInUser = user;
        initializeDAOs();
        initComponents();
        initializeTable();
        setupDepartmentFilter();
        
        // Verify IT permissions
        if (!hasITPermissions()) {
            JOptionPane.showMessageDialog(this,
                "Access denied. This feature requires IT administrator privileges.",
                "Access Denied", JOptionPane.ERROR_MESSAGE);
            dispose();
            return;
        }
        
        setTitle("User Management - MotorPH");
        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }

    /**
     * Initialize DAO objects with database connection
     */
    private void initializeDAOs() {
        try {
            this.databaseConnection = new DatabaseConnection();
            this.employeeDAO = new EmployeeDAO(databaseConnection);
            this.userAuthDAO = new UserAuthenticationDAO();
            this.positionDAO = new PositionDAO(databaseConnection);
            
            // Test database connection
            if (!databaseConnection.testConnection()) {
                throw new RuntimeException("Failed to connect to database");
            }
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Database connection failed: " + e.getMessage() + 
                "\nPlease check your database configuration.",
                "Database Error", JOptionPane.ERROR_MESSAGE);
            System.err.println("Database initialization error: " + e.getMessage());
            dispose();
        }
    }

    /**
     * Setup department filter with dynamic options and action listener
     */
    private void setupDepartmentFilter() {
        try {
            // Get departments from database
            String[] dynamicOptions = getDepartmentFilterOptions();
            
            // Update the combo box model
            deptfiltercombobox.setModel(new DefaultComboBoxModel<>(dynamicOptions));
            deptfiltercombobox.setSelectedItem("All");
            
            // Add action listener for automatic filtering
            deptfiltercombobox.addActionListener(e -> {
                String selectedDepartment = (String) deptfiltercombobox.getSelectedItem();
                if (selectedDepartment != null) {
                    filterByDepartment(selectedDepartment);
                }
            });
            
        } catch (Exception e) {
            System.err.println("Error setting up department filter: " + e.getMessage());
            // Fall back to static options
            deptfiltercombobox.setModel(new DefaultComboBoxModel<>(DEPARTMENT_OPTIONS));
        }
    }

    /**
     * Check if current user has IT administrator permissions
     * @return true if user has IT permissions
     */
    private boolean hasITPermissions() {
        if (loggedInUser == null) {
            return false;
        }
        
        String userRole = loggedInUser.getUserRole();
        return userRole != null && 
               (userRole.toUpperCase().contains("IT") || 
                userRole.toUpperCase().contains("ADMIN"));
    }

    /**
     * Initialize the user table with data from database
     */
    private void initializeTable() {
        try {
            // Create custom table model with password protection
            String[] columnNames = {"Employee ID", "Last Name", "First Name", "Email", "Password", "Role", "Position", "Department", "Classification", "Status"};
            passwordTableModel = new PasswordProtectedTableModel(new Object[0][0], columnNames);
            UserMgmtTbl.setModel(passwordTableModel);
            
            // Set column widths for better display
            UserMgmtTbl.getColumnModel().getColumn(0).setPreferredWidth(80);  // Employee ID
            UserMgmtTbl.getColumnModel().getColumn(1).setPreferredWidth(100); // Last Name
            UserMgmtTbl.getColumnModel().getColumn(2).setPreferredWidth(100); // First Name
            UserMgmtTbl.getColumnModel().getColumn(3).setPreferredWidth(150); // Email
            UserMgmtTbl.getColumnModel().getColumn(4).setPreferredWidth(80);  // Password
            UserMgmtTbl.getColumnModel().getColumn(5).setPreferredWidth(80);  // Role
            UserMgmtTbl.getColumnModel().getColumn(6).setPreferredWidth(120); // Position
            UserMgmtTbl.getColumnModel().getColumn(7).setPreferredWidth(100); // Department
            UserMgmtTbl.getColumnModel().getColumn(8).setPreferredWidth(100); // Classification
            UserMgmtTbl.getColumnModel().getColumn(9).setPreferredWidth(80);  // Status
            
            // Load all employees
            loadAllEmployees();
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Error initializing user table: " + e.getMessage(),
                "Initialization Error", JOptionPane.ERROR_MESSAGE);
            System.err.println("Table initialization error: " + e.getMessage());
        }
    }

    /**
     * Load all employees from database into the table
     */
    private void loadAllEmployees() {
        try {
            passwordTableModel.setRowCount(0); // Clear existing data
            
            List<EmployeeModel> employees = employeeDAO.getActiveEmployees();
            
            for (EmployeeModel employee : employees) {
                addEmployeeToTable(employee);
            }
            
            updateStatusLabel(employees.size() + " employees loaded");
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Error loading employees: " + e.getMessage(),
                "Database Error", JOptionPane.ERROR_MESSAGE);
            System.err.println("Error loading employees: " + e.getMessage());
        }
    }

    /**
     * Add an employee to the table using Model methods
     * @param employee The employee to add
     */
    private void addEmployeeToTable(EmployeeModel employee) {
        try {
            // Use PositionModel methods for all position-related data
            String positionName = "Unknown Position";
            String department = "Unknown Department";
            String classification = "Unknown";
            
            if (employee.getPositionId() != null) {
                PositionModel position = positionDAO.findById(employee.getPositionId());
                if (position != null) {
                    // Use Model methods instead of implementing logic here
                    positionName = position.getPositionDisplayName();
                    department = position.getDepartmentDisplayName();
                    classification = position.getClassification();
                }
            }
            
            passwordTableModel.addRow(new Object[]{
                employee.getEmployeeId(),
                employee.getLastName() != null ? employee.getLastName() : "",
                employee.getFirstName() != null ? employee.getFirstName() : "",
                employee.getEmail() != null ? employee.getEmail() : "",
                employee.getPasswordHash() != null ? employee.getPasswordHash() : "",
                employee.getUserRole() != null ? employee.getUserRole() : "Employee",
                positionName,
                department,
                classification,
                employee.getStatus() != null ? employee.getStatus().getValue() : "Unknown"
            });
            
        } catch (Exception e) {
            System.err.println("Error adding employee to table: " + e.getMessage());
        }
    }

    /**
     * Get department filter options using PositionDAO methods
     * @return Array of department options
     */
    private String[] getDepartmentFilterOptions() {
        try {
            List<PositionModel> allPositions = positionDAO.findAll();
            Set<String> departmentSet = new HashSet<>();
            
            // Start with core departments
            departmentSet.add("All");
            
            // Add departments from your specified list
            for (String dept : DEPARTMENT_OPTIONS) {
                departmentSet.add(dept);
            }
            
            // Add any additional departments from database
            for (PositionModel position : allPositions) {
                if (position.getDepartment() != null && !position.getDepartment().trim().isEmpty()) {
                    departmentSet.add(position.getDepartment());
                }
            }
            
            // Convert to array while maintaining order (put "All" first)
            List<String> orderedDepts = new ArrayList<>();
            orderedDepts.add("All");
            for (String dept : DEPARTMENT_OPTIONS) {
                if (!dept.equals("All") && departmentSet.contains(dept)) {
                    orderedDepts.add(dept);
                }
            }
            
            // Add any extra departments not in the predefined list
            for (String dept : departmentSet) {
                if (!orderedDepts.contains(dept)) {
                    orderedDepts.add(dept);
                }
            }
            
            return orderedDepts.toArray(new String[0]);
            
        } catch (Exception e) {
            System.err.println("Error loading department options: " + e.getMessage());
            // Fallback to static options
            return DEPARTMENT_OPTIONS;
        }
    }

    /**
     * Filter employees by search term (ID or name) using EmployeeDAO methods
     * @param searchTerm The search term (ID or name)
     */
    private void filterBySearchTerm(String searchTerm) {
        try {
            passwordTableModel.setRowCount(0);
            
            if (searchTerm == null || searchTerm.trim().isEmpty()) {
                loadAllEmployees();
                return;
            }
            
            String trimmedSearch = searchTerm.trim();
            
            // Try to parse as employee ID first
            try {
                Integer id = Integer.valueOf(trimmedSearch);
                EmployeeModel employee = employeeDAO.findById(id);
                
                if (employee != null) {
                    addEmployeeToTable(employee);
                    updateStatusLabel("Found employee with ID: " + trimmedSearch);
                    return;
                } else {
                    updateStatusLabel("No employee found with ID: " + trimmedSearch);
                }
            } catch (NumberFormatException e) {
                // Not a valid ID, search by name
                filterByEmployeeName(trimmedSearch);
            }
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Error during search: " + e.getMessage(),
                "Search Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Filter employees by name using Employee methods
     * @param searchName The name to search for
     */
    private void filterByEmployeeName(String searchName) {
        try {
            List<EmployeeModel> allEmployees = employeeDAO.getActiveEmployees();
            List<EmployeeModel> matchingEmployees = new ArrayList<>();
            
            String searchLower = searchName.toLowerCase().trim();
            
            // Use Employee model's getFullName() method for consistency
            for (EmployeeModel employee : allEmployees) {
                if (employee.getFullName().toLowerCase().contains(searchLower)) {
                    matchingEmployees.add(employee);
                }
            }
            
            for (EmployeeModel employee : matchingEmployees) {
                addEmployeeToTable(employee);
            }
            
            updateStatusLabel("Found " + matchingEmployees.size() + " employees matching: " + searchName);
            
            if (matchingEmployees.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                    "No employees found matching: " + searchName,
                    "No Match", JOptionPane.INFORMATION_MESSAGE);
            }
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Error searching by employee name: " + e.getMessage(),
                "Search Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Filter employees by department using PositionDAO and PositionModel methods
     * @param filterValue The department to filter by
     */
    private void filterByDepartment(String filterValue) {
        try {
            passwordTableModel.setRowCount(0);
            
            if (filterValue == null || filterValue.equals("All")) {
                loadAllEmployees();
                return;
            }
            
            List<EmployeeModel> allEmployees = employeeDAO.getActiveEmployees();
            List<EmployeeModel> filteredEmployees = new ArrayList<>();
            
            for (EmployeeModel employee : allEmployees) {
                if (employee.getPositionId() != null) {
                    PositionModel position = positionDAO.findById(employee.getPositionId());
                    if (position != null) {
                        String empDepartment = position.getDepartment();
                        if (empDepartment != null && empDepartment.equalsIgnoreCase(filterValue)) {
                            filteredEmployees.add(employee);
                        }
                    }
                }
            }
            
            for (EmployeeModel employee : filteredEmployees) {
                addEmployeeToTable(employee);
            }
            
            updateStatusLabel("Found " + filteredEmployees.size() + " employees in " + filterValue + " department");
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Error filtering by department: " + e.getMessage(),
                "Filter Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Get next available employee ID using EmployeeDAO methods
     * @param currentInput Current input to start from
     * @return Next available employee ID
     */
    private String getNextEmployeeID(String currentInput) {
        try {
            int startingID;
            
            try {
                startingID = Integer.parseInt(currentInput);
            } catch (NumberFormatException e) {
                startingID = getHighestEmployeeID();
            }
            
            int suggestedID = startingID + 1;
            
            while (employeeDAO.findById(suggestedID) != null) {
                suggestedID++;
            }
            
            return String.valueOf(suggestedID);
            
        } catch (Exception e) {
            System.err.println("Error getting next employee ID: " + e.getMessage());
            return "10000"; // Default starting ID
        }
    }

    /**
     * Get highest existing employee ID using DAO methods
     * @return Highest employee ID in the database
     */
    private int getHighestEmployeeID() {
        try {
            List<EmployeeModel> allEmployees = employeeDAO.findAll();
            int highestID = 0;
            
            for (EmployeeModel employee : allEmployees) {
                if (employee.getEmployeeId() != null && employee.getEmployeeId() > highestID) {
                    highestID = employee.getEmployeeId();
                }
            }
            
            return highestID;
            
        } catch (Exception e) {
            System.err.println("Error getting highest employee ID: " + e.getMessage());
            return 10000; // Default starting point
        }
    }

    /**
     * Check if employee ID exists using DAO methods
     * @param employeeId The ID to check
     * @return true if ID exists, false otherwise
     */
    private boolean isEmployeeIDDuplicate(String employeeId) {
        try {
            Integer id = Integer.valueOf(employeeId);
            return employeeDAO.findById(id) != null;
        } catch (NumberFormatException e) {
            return true; // Invalid format counts as duplicate
        } catch (Exception e) {
            System.err.println("Error checking duplicate ID: " + e.getMessage());
            return true; // Assume duplicate on error for safety
        }
    }

    /**
     * Helper method to create consistent field panels
     * @param labelText The label text
     * @return JPanel with label
     */
    private JPanel createFieldPanel(String labelText) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel label = new JLabel(labelText);
        label.setPreferredSize(new Dimension(220, 25)); // Increased width to prevent text cutoff
        panel.add(label);
        return panel;
    }

    /**
     * Helper method to format department names properly
     * @param department Raw department name
     * @return Properly formatted department name
     */
    private String formatDepartmentName(String department) {
        if (department == null || department.trim().isEmpty()) {
            return "Unknown";
        }
        
        String formatted = department.trim();
        
        switch (formatted.toLowerCase()) {
            case "hr":
                return "HR";
            case "it":
                return "IT";
            default:
                String[] words = formatted.split(" ");
                StringBuilder result = new StringBuilder();
                for (String word : words) {
                    if (word.length() > 0) {
                        result.append(Character.toUpperCase(word.charAt(0)))
                              .append(word.substring(1).toLowerCase())
                              .append(" ");
                    }
                }
                return result.toString().trim();
        }
    }

    /**
     * Create a new user account using Model validation
     */
    private void createNewUser() {
        try {
            String suggestedID = getNextEmployeeID("");
            
            List<PositionModel> positions = positionDAO.findAll();
            if (positions.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                    "No positions available. Please create positions first.",
                    "No Positions", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            JPanel mainPanel = new JPanel(new BorderLayout());
            JPanel fieldsPanel = new JPanel();
            fieldsPanel.setLayout(new BoxLayout(fieldsPanel, BoxLayout.Y_AXIS));
            fieldsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            
            JPanel idPanel = createFieldPanel("Employee ID (suggested: " + suggestedID + "):");
            JTextField idField = new JTextField(suggestedID, 15);
            idPanel.add(idField);
            
            JPanel firstNamePanel = createFieldPanel("First Name:");
            JTextField firstNameField = new JTextField(15);
            firstNamePanel.add(firstNameField);
            
            JPanel lastNamePanel = createFieldPanel("Last Name:");
            JTextField lastNameField = new JTextField(15);
            lastNamePanel.add(lastNameField);
            
            JPanel emailPanel = createFieldPanel("Email:");
            JTextField emailField = new JTextField(15);
            emailPanel.add(emailField);
            
            JPanel positionPanel = createFieldPanel("Position:");
            JComboBox<String> positionCombo = new JComboBox<>();
            for (PositionModel position : positions) {
                String departmentName = formatDepartmentName(position.getDepartment());
                String displayText = position.getPositionDisplayName() + " - " + departmentName;
                positionCombo.addItem(displayText);
            }
            positionCombo.setPreferredSize(new Dimension(300, 25));
            positionPanel.add(positionCombo);
            
            JPanel rolePanel = createFieldPanel("User Role:");
            JComboBox<String> roleCombo = new JComboBox<>(new String[]{
                "Employee", "HR", "IT", "Supervisor", "Manager", "Admin"
            });
            roleCombo.setPreferredSize(new Dimension(150, 25));
            rolePanel.add(roleCombo);
            
            JPanel passwordPanel = createFieldPanel("Password:");
            JPasswordField passwordField = new JPasswordField(15);
            passwordPanel.add(passwordField);
            
            fieldsPanel.add(idPanel);
            fieldsPanel.add(Box.createVerticalStrut(5));
            fieldsPanel.add(firstNamePanel);
            fieldsPanel.add(Box.createVerticalStrut(5));
            fieldsPanel.add(lastNamePanel);
            fieldsPanel.add(Box.createVerticalStrut(5));
            fieldsPanel.add(emailPanel);
            fieldsPanel.add(Box.createVerticalStrut(5));
            fieldsPanel.add(positionPanel);
            fieldsPanel.add(Box.createVerticalStrut(5));
            fieldsPanel.add(rolePanel);
            fieldsPanel.add(Box.createVerticalStrut(5));
            fieldsPanel.add(passwordPanel);
            
            mainPanel.add(fieldsPanel, BorderLayout.CENTER);
            
            int result = JOptionPane.showConfirmDialog(this, mainPanel, "Create New User", 
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            
            if (result == JOptionPane.OK_OPTION) {
                int selectedIndex = positionCombo.getSelectedIndex();
                PositionModel selectedPosition = positions.get(selectedIndex);
                
                processNewUserCreation(idField, firstNameField, lastNameField, emailField,
                                     selectedPosition, roleCombo, passwordField);
            }
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Error creating user form: " + e.getMessage(),
                "Form Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Process new user creation using Model validation methods
     */
    private void processNewUserCreation(JTextField idField, JTextField firstNameField, 
                                      JTextField lastNameField, JTextField emailField,
                                      PositionModel selectedPosition,
                                      JComboBox<String> roleCombo,
                                      JPasswordField passwordField) {
        try {
            String employeeIdStr = idField.getText().trim();
            String firstName = firstNameField.getText().trim();
            String lastName = lastNameField.getText().trim();
            String email = emailField.getText().trim();
            String userRole = (String) roleCombo.getSelectedItem();
            String password = new String(passwordField.getPassword());
            
            if (employeeIdStr.isEmpty() || firstName.isEmpty() || lastName.isEmpty() || 
                email.isEmpty() || password.isEmpty() || selectedPosition == null) {
                JOptionPane.showMessageDialog(this,
                    "All fields are required.",
                    "Invalid Input", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (isEmployeeIDDuplicate(employeeIdStr)) {
                JOptionPane.showMessageDialog(this,
                    "Employee ID " + employeeIdStr + " already exists.\nPlease choose a different ID.",
                    "Duplicate ID", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (userAuthDAO.emailExists(email)) {
                JOptionPane.showMessageDialog(this,
                    "Email address " + email + " already exists.\nPlease choose a different email.",
                    "Duplicate Email", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            PasswordHasher.PasswordValidationResult passwordValidation = 
                PasswordHasher.validateEmployeePassword(password, firstName, lastName, email);
            
            if (!passwordValidation.isValid()) {
                JOptionPane.showMessageDialog(this,
                    "Password does not meet requirements:\n" + 
                    String.join("\n", passwordValidation.getIssues()) + "\n\n" +
                    PasswordHasher.getSystemPasswordRequirements(),
                    "Invalid Password", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            EmployeeModel newEmployee = new EmployeeModel();
            newEmployee.setEmployeeId(Integer.valueOf(employeeIdStr));
            newEmployee.setFirstName(firstName);
            newEmployee.setLastName(lastName);
            newEmployee.setEmail(email);
            newEmployee.setUserRole(userRole);
            newEmployee.setBasicSalary(new BigDecimal("25000.00"));
            newEmployee.setHourlyRate(newEmployee.calculateHourlyRate());
            newEmployee.setPositionId(selectedPosition.getPositionId());
            newEmployee.setStatus(EmployeeModel.EmployeeStatus.PROBATIONARY);
            newEmployee.setBirthDate(LocalDate.of(1990, 1, 1));
            
            String hashedPassword = PasswordHasher.hashPassword(password);
            newEmployee.setPasswordHash(hashedPassword);
            
            boolean success = employeeDAO.save(newEmployee);
            
            if (success) {
                loadAllEmployees();
                
                JOptionPane.showMessageDialog(this,
                    "User created successfully!\n" +
                    "ID: " + employeeIdStr + "\n" +
                    "Name: " + newEmployee.getFullName() + "\n" +
                    "Email: " + email + "\n" +
                    "Role: " + userRole + "\n" +
                    "Position: " + selectedPosition.getFullTitle() + "\n" +
                    "Classification: " + selectedPosition.getClassification(),
                    "User Created", JOptionPane.INFORMATION_MESSAGE);
                    
                updateStatusLabel("New user created: " + newEmployee.getDisplayName());
            } else {
                JOptionPane.showMessageDialog(this,
                    "Failed to create user. Please try again.",
                    "Creation Failed", JOptionPane.ERROR_MESSAGE);
            }
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Error creating user: " + e.getMessage(),
                "Creation Error", JOptionPane.ERROR_MESSAGE);
            System.err.println("Error creating user: " + e.getMessage());
        }
    }

    /**
     * Edit selected user using existing Model methods
     */
    private void editSelectedUser() {
        try {
            int selectedRow = UserMgmtTbl.getSelectedRow();
            if (selectedRow < 0) {
                JOptionPane.showMessageDialog(this,
                    "Please select a user to edit.",
                    "No User Selected", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            Integer employeeId = (Integer) passwordTableModel.getValueAt(selectedRow, 0);
            EmployeeModel currentEmployee = employeeDAO.findById(employeeId);
            
            if (currentEmployee == null) {
                JOptionPane.showMessageDialog(this,
                    "Employee not found in database.",
                    "Employee Not Found", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            List<PositionModel> positions = positionDAO.findAll();
            
            JPanel mainPanel = new JPanel(new BorderLayout());
            JPanel fieldsPanel = new JPanel();
            fieldsPanel.setLayout(new BoxLayout(fieldsPanel, BoxLayout.Y_AXIS));
            fieldsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            
            JPanel idPanel = createFieldPanel("Employee ID (cannot be changed):");
            JTextField idField = new JTextField(employeeId.toString(), 15);
            idField.setEditable(false);
            idField.setBackground(Color.LIGHT_GRAY);
            idPanel.add(idField);
            
            JPanel firstNamePanel = createFieldPanel("First Name:");
            JTextField firstNameField = new JTextField(currentEmployee.getFirstName(), 15);
            firstNamePanel.add(firstNameField);
            
            JPanel lastNamePanel = createFieldPanel("Last Name:");
            JTextField lastNameField = new JTextField(currentEmployee.getLastName(), 15);
            lastNamePanel.add(lastNameField);
            
            JPanel emailPanel = createFieldPanel("Email:");
            JTextField emailField = new JTextField(currentEmployee.getEmail(), 15);
            emailPanel.add(emailField);
            
            JPanel positionPanel = createFieldPanel("Position:");
            JComboBox<String> positionCombo = new JComboBox<>();
            int selectedIndex = 0;
            for (int i = 0; i < positions.size(); i++) {
                PositionModel position = positions.get(i);
                String departmentName = formatDepartmentName(position.getDepartment());
                String displayText = position.getPositionDisplayName() + " - " + departmentName;
                positionCombo.addItem(displayText);
                if (position.getPositionId().equals(currentEmployee.getPositionId())) {
                    selectedIndex = i;
                }
            }
            positionCombo.setSelectedIndex(selectedIndex);
            positionCombo.setPreferredSize(new Dimension(300, 25));
            positionPanel.add(positionCombo);
            
            JPanel rolePanel = createFieldPanel("User Role:");
            JComboBox<String> roleCombo = new JComboBox<>(new String[]{
                "Employee", "HR", "IT", "Supervisor", "Manager", "Admin"
            });
            roleCombo.setSelectedItem(currentEmployee.getUserRole());
            roleCombo.setPreferredSize(new Dimension(150, 25));
            rolePanel.add(roleCombo);
            
            JPanel passwordPanel = createFieldPanel("New Password:");
            JPasswordField passwordField = new JPasswordField(15);
            passwordPanel.add(passwordField);
            
            fieldsPanel.add(idPanel);
            fieldsPanel.add(Box.createVerticalStrut(5));
            fieldsPanel.add(firstNamePanel);
            fieldsPanel.add(Box.createVerticalStrut(5));
            fieldsPanel.add(lastNamePanel);
            fieldsPanel.add(Box.createVerticalStrut(5));
            fieldsPanel.add(emailPanel);
            fieldsPanel.add(Box.createVerticalStrut(5));
            fieldsPanel.add(positionPanel);
            fieldsPanel.add(Box.createVerticalStrut(5));
            fieldsPanel.add(rolePanel);
            fieldsPanel.add(Box.createVerticalStrut(5));
            fieldsPanel.add(passwordPanel);
            
            mainPanel.add(fieldsPanel, BorderLayout.CENTER);
            
            int result = JOptionPane.showConfirmDialog(this, mainPanel, "Edit User",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            
            if (result == JOptionPane.OK_OPTION) {
                int selectedPos = positionCombo.getSelectedIndex();
                PositionModel selectedPosition = positions.get(selectedPos);
                
                processUserEdit(currentEmployee, firstNameField, lastNameField, emailField,
                              selectedPosition, roleCombo, passwordField);
            }
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Error editing user: " + e.getMessage(),
                "Edit Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Process user edit using Model validation methods
     */
    private void processUserEdit(EmployeeModel employee, JTextField firstNameField,
                               JTextField lastNameField, JTextField emailField,
                               PositionModel selectedPosition,
                               JComboBox<String> roleCombo, JPasswordField passwordField) {
        try {
            String firstName = firstNameField.getText().trim();
            String lastName = lastNameField.getText().trim();
            String email = emailField.getText().trim();
            String userRole = (String) roleCombo.getSelectedItem();
            String newPassword = new String(passwordField.getPassword());
            
            if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || selectedPosition == null) {
                JOptionPane.showMessageDialog(this,
                    "First Name, Last Name, Email, and Position are required.",
                    "Invalid Input", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            EmployeeModel existingEmployee = employeeDAO.findByEmail(email);
            if (existingEmployee != null && !existingEmployee.getEmployeeId().equals(employee.getEmployeeId())) {
                JOptionPane.showMessageDialog(this,
                    "Email address already exists for another employee.",
                    "Duplicate Email", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (!newPassword.isEmpty()) {
                PasswordHasher.PasswordValidationResult passwordValidation = 
                    PasswordHasher.validateEmployeePassword(newPassword, firstName, lastName, email);
                
                if (!passwordValidation.isValid()) {
                    JOptionPane.showMessageDialog(this,
                        "Password does not meet requirements:\n" + 
                        String.join("\n", passwordValidation.getIssues()),
                        "Invalid Password", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
            
            employee.setFirstName(firstName);
            employee.setLastName(lastName);
            employee.setEmail(email);
            employee.setUserRole(userRole);
            employee.setPositionId(selectedPosition.getPositionId());
            
            if (!newPassword.isEmpty()) {
                String hashedPassword = PasswordHasher.hashPassword(newPassword);
                employee.setPasswordHash(hashedPassword);
            }
            
            boolean success = employeeDAO.update(employee);
            
            if (success) {
                loadAllEmployees();
                
                JOptionPane.showMessageDialog(this,
                    "User updated successfully!\n" +
                    "Name: " + employee.getFullName() + "\n" +
                    "Email: " + email + "\n" +
                    "Role: " + userRole + "\n" +
                    "Position: " + selectedPosition.getFullTitle() + "\n" +
                    "Classification: " + selectedPosition.getClassification(),
                    "User Updated", JOptionPane.INFORMATION_MESSAGE);
                    
                updateStatusLabel("User updated: " + employee.getDisplayName());
            } else {
                JOptionPane.showMessageDialog(this,
                    "Failed to update user. Please try again.",
                    "Update Failed", JOptionPane.ERROR_MESSAGE);
            }
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Error updating user: " + e.getMessage(),
                "Update Error", JOptionPane.ERROR_MESSAGE);
            System.err.println("Error updating user: " + e.getMessage());
        }
    }

    /**
     * Delete selected user using DAO methods (Soft Deletion)
     */
    private void deleteSelectedUser() {
        try {
            int selectedRow = UserMgmtTbl.getSelectedRow();
            if (selectedRow < 0) {
                JOptionPane.showMessageDialog(this,
                    "Please select a user to delete.",
                    "No User Selected", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            Integer employeeId = (Integer) passwordTableModel.getValueAt(selectedRow, 0);
            EmployeeModel employee = employeeDAO.findById(employeeId);
            
            if (employee == null) {
                JOptionPane.showMessageDialog(this,
                    "Employee not found in database.",
                    "Employee Not Found", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            String displayName = employee.getDisplayName();
            
            int choice = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to deactivate user: " + displayName + "?\n\n" +
                "This will mark the employee as terminated and remove them from the active list.",
                "Confirm Deactivation", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                
            if (choice == JOptionPane.YES_OPTION) {
                boolean success = employeeDAO.deactivateEmployee(employeeId);
                
                if (success) {
                    passwordTableModel.removeRow(selectedRow);
                    
                    JOptionPane.showMessageDialog(this,
                        "User " + displayName + " has been deactivated successfully.\n" +
                        "The employee record is marked as terminated.",
                        "User Deactivated", JOptionPane.INFORMATION_MESSAGE);
                        
                    updateStatusLabel("User deactivated: " + displayName);
                } else {
                    JOptionPane.showMessageDialog(this,
                        "Failed to deactivate user. Please try again.",
                        "Deactivation Failed", JOptionPane.ERROR_MESSAGE);
                }
            }
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Error deactivating user: " + e.getMessage(),
                "Delete Error", JOptionPane.ERROR_MESSAGE);
            System.err.println("Error deactivating user: " + e.getMessage());
        }
    }

    /**
     * Reset table to show all employees
     */
    private void resetTableView() {
        loadAllEmployees();
        searchBartxtfield.setText("");
        deptfiltercombobox.setSelectedItem("All");
        updateStatusLabel("Showing all employees");
    }

    /**
     * Update status label
     * @param message Status message to display
     */
    private void updateStatusLabel(String message) {
        System.out.println("Status: " + message);
    }

    /**
     * Password-protected table model that masks passwords
     */
    private class PasswordProtectedTableModel extends DefaultTableModel {
        public PasswordProtectedTableModel(Object[][] data, Object[] columnNames) {
            super(data, columnNames);
        }

        @Override
        public Object getValueAt(int row, int column) {
            if (column == 4) {
                String password = (String) super.getValueAt(row, column);
                if (password != null && !password.isEmpty()) {
                    return "*".repeat(Math.min(password.length(), 12));
                }
                return "No Password";
            }
            return super.getValueAt(row, column);
        }

        public String getRealPassword(int row) {
            return (String) super.getValueAt(row, 4);
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
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

        jPanel2 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        UserMgmtTbl = new javax.swing.JTable();
        jPanel3 = new javax.swing.JPanel();
        IDNoTrckrHR = new javax.swing.JLabel();
        searchBartxtfield = new javax.swing.JTextField();
        findEmployeeBtn1 = new javax.swing.JButton();
        jPanel4 = new javax.swing.JPanel();
        InputEmpNameTrckrHR = new javax.swing.JLabel();
        inputName = new javax.swing.JPanel();
        deptfiltercombobox = new javax.swing.JComboBox<>();
        EmpNameTrckrHR = new javax.swing.JLabel();
        InputEmpNameTrckrHR1 = new javax.swing.JLabel();
        editUserBtn = new javax.swing.JButton();
        resetTableBtn = new javax.swing.JButton();
        createNewUserBtn = new javax.swing.JButton();
        deleteUserBtn = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        backattndncbttn = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel2.setBackground(new java.awt.Color(255, 255, 255));
        jPanel2.setPreferredSize(new java.awt.Dimension(868, 442));

        UserMgmtTbl.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null}
            },
            new String [] {
                "Employee ID", "Last Name", "First Name", "Email", "Password"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane1.setViewportView(UserMgmtTbl);

        jPanel3.setBackground(new java.awt.Color(220, 95, 0));

        IDNoTrckrHR.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        IDNoTrckrHR.setForeground(new java.awt.Color(255, 255, 255));
        IDNoTrckrHR.setText("Search:");

        searchBartxtfield.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                searchBartxtfieldActionPerformed(evt);
            }
        });

        findEmployeeBtn1.setBackground(new java.awt.Color(207, 10, 10));
        findEmployeeBtn1.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        findEmployeeBtn1.setForeground(new java.awt.Color(255, 255, 255));
        findEmployeeBtn1.setText("Find Employee");
        findEmployeeBtn1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                findEmployeeBtn1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(22, 22, 22)
                .addComponent(IDNoTrckrHR)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(searchBartxtfield, javax.swing.GroupLayout.PREFERRED_SIZE, 269, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(findEmployeeBtn1)
                .addContainerGap(38, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(IDNoTrckrHR)
                    .addComponent(searchBartxtfield, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(findEmployeeBtn1))
                .addContainerGap(11, Short.MAX_VALUE))
        );

        jPanel4.setBackground(new java.awt.Color(220, 95, 0));

        InputEmpNameTrckrHR.setForeground(new java.awt.Color(255, 255, 255));

        inputName.setBackground(new java.awt.Color(220, 95, 0));

        deptfiltercombobox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "All", "Leadership", "IT", "HR", "Accounting", "Accounts", "Sales & Marketing", "Supply Chain & Logistics", "Customer Service" }));

        EmpNameTrckrHR.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        EmpNameTrckrHR.setForeground(new java.awt.Color(255, 255, 255));
        EmpNameTrckrHR.setText("Department:");

        InputEmpNameTrckrHR1.setForeground(new java.awt.Color(255, 255, 255));

        javax.swing.GroupLayout inputNameLayout = new javax.swing.GroupLayout(inputName);
        inputName.setLayout(inputNameLayout);
        inputNameLayout.setHorizontalGroup(
            inputNameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(inputNameLayout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addComponent(EmpNameTrckrHR)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(deptfiltercombobox, javax.swing.GroupLayout.PREFERRED_SIZE, 176, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(157, 157, 157)
                .addComponent(InputEmpNameTrckrHR1, javax.swing.GroupLayout.PREFERRED_SIZE, 316, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        inputNameLayout.setVerticalGroup(
            inputNameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, inputNameLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(inputNameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(EmpNameTrckrHR)
                    .addComponent(InputEmpNameTrckrHR1)
                    .addComponent(deptfiltercombobox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(12, 12, 12))
        );

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGap(74, 74, 74)
                .addComponent(InputEmpNameTrckrHR, javax.swing.GroupLayout.PREFERRED_SIZE, 316, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel4Layout.createSequentialGroup()
                    .addGap(1, 1, 1)
                    .addComponent(inputName, javax.swing.GroupLayout.PREFERRED_SIZE, 418, Short.MAX_VALUE)
                    .addGap(1, 1, 1)))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(InputEmpNameTrckrHR)
                .addGap(34, 34, 34))
            .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel4Layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(inputName, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addContainerGap(13, Short.MAX_VALUE)))
        );

        editUserBtn.setBackground(new java.awt.Color(220, 95, 0));
        editUserBtn.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        editUserBtn.setForeground(new java.awt.Color(255, 255, 255));
        editUserBtn.setText("Edit user");
        editUserBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editUserBtnActionPerformed(evt);
            }
        });

        resetTableBtn.setBackground(new java.awt.Color(220, 95, 0));
        resetTableBtn.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        resetTableBtn.setForeground(new java.awt.Color(255, 255, 255));
        resetTableBtn.setText("Reset");
        resetTableBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                resetTableBtnActionPerformed(evt);
            }
        });

        createNewUserBtn.setBackground(new java.awt.Color(0, 153, 0));
        createNewUserBtn.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        createNewUserBtn.setForeground(new java.awt.Color(255, 255, 255));
        createNewUserBtn.setText("Create new user +");
        createNewUserBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                createNewUserBtnActionPerformed(evt);
            }
        });

        deleteUserBtn.setBackground(new java.awt.Color(207, 10, 10));
        deleteUserBtn.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        deleteUserBtn.setForeground(new java.awt.Color(255, 255, 255));
        deleteUserBtn.setText("Delete user");
        deleteUserBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteUserBtnActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap(18, Short.MAX_VALUE)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(resetTableBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 96, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(editUserBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 93, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(66, 66, 66)
                                .addComponent(deleteUserBtn))
                            .addComponent(createNewUserBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 252, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 844, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(20, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(35, 35, 35)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 350, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(resetTableBtn)
                    .addComponent(createNewUserBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(deleteUserBtn)
                    .addComponent(editUserBtn))
                .addContainerGap(12, Short.MAX_VALUE))
        );

        jPanel1.setBackground(new java.awt.Color(220, 95, 0));
        jPanel1.setPreferredSize(new java.awt.Dimension(211, 57));

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(255, 255, 255));
        jLabel1.setText("USER MANAGEMENT");

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
                .addContainerGap(567, Short.MAX_VALUE))
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

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 882, Short.MAX_VALUE)
            .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, 882, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, 538, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void resetTableBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_resetTableBtnActionPerformed
        //Reset the table in its default state after searching
        resetTableView();
    }//GEN-LAST:event_resetTableBtnActionPerformed

    private void editUserBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editUserBtnActionPerformed
      //Edit user button action
        editSelectedUser();
    }//GEN-LAST:event_editUserBtnActionPerformed

    private void searchBartxtfieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_searchBartxtfieldActionPerformed
     filterBySearchTerm(searchBartxtfield.getText());
    }//GEN-LAST:event_searchBartxtfieldActionPerformed

    private void createNewUserBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_createNewUserBtnActionPerformed
       // Create new user button action 
        createNewUser();
    }//GEN-LAST:event_createNewUserBtnActionPerformed


    private void deleteUserBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteUserBtnActionPerformed
       // Delete user button action
        deleteSelectedUser();
    }//GEN-LAST:event_deleteUserBtnActionPerformed

    private void backattndncbttnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_backattndncbttnActionPerformed
        // Close this window and return to AdminIT dashboard
        dispose();
        
        try {
            // Create AdminIT page using the logged-in user
            gui.AdminIT adminIT = new gui.AdminIT(loggedInUser);
            adminIT.setVisible(true);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Error returning to Admin IT dashboard: " + e.getMessage(),
                "Navigation Error", JOptionPane.ERROR_MESSAGE);
            System.err.println("Error navigating back: " + e.getMessage());
        }
    }//GEN-LAST:event_backattndncbttnActionPerformed

    private void findEmployeeBtn1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_findEmployeeBtn1ActionPerformed
        //Search button action 
        try {
            String searchTerm = searchBartxtfield.getText().trim();
            String selectedDepartment = (String) deptfiltercombobox.getSelectedItem();
            
            // Priority: Search term first, then department filter
            if (!searchTerm.isEmpty()) {
                filterBySearchTerm(searchTerm);
            } else if (selectedDepartment != null && !selectedDepartment.equals("All")) {
                filterByDepartment(selectedDepartment);
            } else {
                // If no search criteria, show message and reset
                JOptionPane.showMessageDialog(this,
                    "Please enter a search term (Employee ID or Name) or select a Department to filter",
                    "No Search Criteria", JOptionPane.INFORMATION_MESSAGE);
                resetTableView();
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Error during search: " + e.getMessage(),
                "Search Error", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_findEmployeeBtn1ActionPerformed
    /**
     * Main method for testing
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
            java.util.logging.Logger.getLogger(UserManagement.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> {
            // For testing purposes, create a dummy IT user
            UserAuthenticationModel testUser = new UserAuthenticationModel();
            testUser.setEmployeeId(1);
            testUser.setEmail("admin@motorph.com");
            testUser.setUserRole("IT");
            testUser.setFirstName("Test");
            testUser.setLastName("Admin");
            
            new UserManagement(testUser).setVisible(true);
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel EmpNameTrckrHR;
    private javax.swing.JLabel IDNoTrckrHR;
    private javax.swing.JLabel InputEmpNameTrckrHR;
    private javax.swing.JLabel InputEmpNameTrckrHR1;
    private javax.swing.JTable UserMgmtTbl;
    private javax.swing.JButton backattndncbttn;
    private javax.swing.JButton createNewUserBtn;
    private javax.swing.JButton deleteUserBtn;
    private javax.swing.JComboBox<String> deptfiltercombobox;
    private javax.swing.JButton editUserBtn;
    private javax.swing.JButton findEmployeeBtn1;
    private javax.swing.JPanel inputName;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JButton resetTableBtn;
    private javax.swing.JTextField searchBartxtfield;
    // End of variables declaration//GEN-END:variables


}
