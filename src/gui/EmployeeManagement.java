package gui;

import DAOs.*;
import Models.*;
import Models.EmployeeModel.EmployeeStatus;
import java.awt.Component;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

/**
 * Employee Management GUI with integrated Government ID handling and Position Benefits
 * @author User
 */
public class EmployeeManagement extends javax.swing.JFrame {
    
    // User session information
    private final UserAuthenticationModel loggedInUser;
    private final String userRole;
    
    // DAO instances
    private final DatabaseConnection databaseConnection;
    private final EmployeeDAO employeeDAO;
    private final PositionDAO positionDAO;
    
    // Current employee data
    private List<EmployeeModel> employeeList;
    private List<PositionModel> positionList;
    private EmployeeModel selectedEmployee;
    private GovIdModel selectedGovIds;
    
    // Position benefits cache
    private Map<Integer, Map<String, BigDecimal>> positionBenefitsCache = new HashMap<>();
    
    public EmployeeManagement(UserAuthenticationModel loggedInUser) {
        this.loggedInUser = loggedInUser;
        this.userRole = (loggedInUser != null) ? loggedInUser.getUserRole() : "HR";
        
        this.databaseConnection = new DatabaseConnection();
        this.employeeDAO = new EmployeeDAO(databaseConnection);
        this.positionDAO = new PositionDAO(databaseConnection);
        
        initializeGUI();
    }
    
    public EmployeeManagement() {
        this.loggedInUser = null;
        this.userRole = "HR";
        
        this.databaseConnection = new DatabaseConnection();
        this.employeeDAO = new EmployeeDAO(databaseConnection);
        this.positionDAO = new PositionDAO(databaseConnection);
        
        initializeGUI();
    }
    
    private void initializeGUI() {
        try {
            initComponents();
            setupGUI();
            loadPositions();
            loadPositionBenefits();
            setupDropdowns();
            loadEmployeeData();
            
            JOptionPane.showMessageDialog(this, 
                "Employee Management System loaded successfully!\n" +
                "• Benefits are automatically populated based on position\n" +
                "• Click on any employee row to view/edit details",
                "System Ready", JOptionPane.INFORMATION_MESSAGE);
                
        } catch (Exception e) {
            System.err.println("Error initializing Employee Management: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, 
                "Error initializing Employee Management: " + e.getMessage(), 
                "Initialization Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void setupGUI() {
        TFAddress.setLineWrap(true);
        TFAddress.setWrapStyleWord(true);
        tblERecords.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        
        tblERecords.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                int selectedRow = tblERecords.getSelectedRow();
                if (selectedRow != -1) {
                    displayEmployeeDetails(selectedRow);
                }
            }
        });
        
        addMonetaryFieldTooltips();
        adjustColumnWidths();
        
        unifiedSearchBar.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                if ("Employee Name or ID".equals(unifiedSearchBar.getText())) {
                    unifiedSearchBar.setText("");
                }
            }
            
            public void focusLost(java.awt.event.FocusEvent evt) {
                if (unifiedSearchBar.getText().trim().isEmpty()) {
                    unifiedSearchBar.setText("Employee Name or ID");
                }
            }
        });
    }
    
    private void loadPositions() {
        try {
            positionList = positionDAO.findAll();
            System.out.println("Loaded " + positionList.size() + " positions");
        } catch (Exception e) {
            System.err.println("Error loading positions: " + e.getMessage());
            positionList = new ArrayList<>();
        }
    }
    
    private void loadPositionBenefits() {
        try {
            positionBenefitsCache.clear();
            
            for (PositionModel position : positionList) {
                Map<String, BigDecimal> benefits = new HashMap<>();
                
                // Default benefits - replace with actual database queries
                benefits.put("Rice Subsidy", new BigDecimal("1500.00"));
                benefits.put("Phone Allowance", new BigDecimal("800.00"));
                benefits.put("Clothing Allowance", new BigDecimal("1000.00"));
                
                positionBenefitsCache.put(position.getPositionId(), benefits);
            }
            
            System.out.println("Loaded position benefits for " + positionBenefitsCache.size() + " positions");
            
        } catch (Exception e) {
            System.err.println("Error loading position benefits: " + e.getMessage());
        }
    }
    
    private void setupDropdowns() {
        try {
            // Setup position dropdown
            DefaultComboBoxModel<String> positionModel = new DefaultComboBoxModel<>();
            positionModel.addElement("Select Position");
            for (PositionModel position : positionList) {
                positionModel.addElement(position.getPosition());
            }
            TFpos.setModel(positionModel);
            
            // Add listener to position dropdown
            TFpos.addActionListener(e -> {
                if (TFpos.getSelectedIndex() > 0) {
                    populateBenefitsForPosition(TFpos.getSelectedItem().toString());
                } else {
                    clearBenefitFields();
                }
            });
            
            setupSupervisorDropdown();
            
        } catch (Exception e) {
            System.err.println("Error setting up dropdowns: " + e.getMessage());
        }
    }
    
    private void setupSupervisorDropdown() {
        try {
            DefaultComboBoxModel<String> supervisorModel = new DefaultComboBoxModel<>();
            supervisorModel.addElement("Select Supervisor");
            supervisorModel.addElement("None");
            
            List<EmployeeModel> allEmployees = employeeDAO.findAll();
            for (EmployeeModel emp : allEmployees) {
                if (emp.getStatus() != EmployeeStatus.TERMINATED) {
                    supervisorModel.addElement(emp.getFirstName() + " " + emp.getLastName() + " (ID: " + emp.getEmployeeId() + ")");
                }
            }
            
            TFsupervisor.setModel(supervisorModel);
            
        } catch (Exception e) {
            System.err.println("Error setting up supervisor dropdown: " + e.getMessage());
        }
    }
    
    private void populateBenefitsForPosition(String positionName) {
        try {
            PositionModel position = findPositionByName(positionName);
            if (position != null) {
                Map<String, BigDecimal> benefits = positionBenefitsCache.get(position.getPositionId());
                if (benefits != null) {
                    TFricesub.setText(formatBenefitValue(benefits.get("Rice Subsidy")));
                    TFphoneallow.setText(formatBenefitValue(benefits.get("Phone Allowance")));
                    TFclothingallow.setText(formatBenefitValue(benefits.get("Clothing Allowance")));
                } else {
                    clearBenefitFields();
                }
            }
        } catch (Exception e) {
            System.err.println("Error populating benefits: " + e.getMessage());
        }
    }
    
    private String formatBenefitValue(BigDecimal value) {
        return value != null ? value.toString() : "0.00";
    }
    
    private void clearBenefitFields() {
        TFricesub.setText("0.00");
        TFphoneallow.setText("0.00");
        TFclothingallow.setText("0.00");
    }
    
    private PositionModel findPositionByName(String positionName) {
        for (PositionModel position : positionList) {
            if (position.getPosition().equals(positionName)) {
                return position;
            }
        }
        return null;
    }
    
    private void loadEmployeeData() {
        try {
            employeeList = employeeDAO.getActiveEmployees();
            
            DefaultTableModel model = (DefaultTableModel) tblERecords.getModel();
            model.setRowCount(0);
            
            for (EmployeeModel employee : employeeList) {
                addEmployeeToTable(employee, model);
            }
            
            adjustRowHeight();
            adjustColumnWidths();
            
            System.out.println("Loaded " + employeeList.size() + " employees (sorted by Employee ID)");
            
        } catch (Exception e) {
            System.err.println("Error loading employee data: " + e.getMessage());
            JOptionPane.showMessageDialog(this, 
                "Error loading employee data: " + e.getMessage(), 
                "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void addEmployeeToTable(EmployeeModel employee, DefaultTableModel model) {
        try {
            Object[] rowData = new Object[18];
            
            rowData[0] = employee.getEmployeeId();
            rowData[1] = employee.getLastName();
            rowData[2] = employee.getFirstName();
            
            if (employee.getBirthDate() != null) {
                rowData[3] = employee.getBirthDate().format(DateTimeFormatter.ofPattern("MM/dd/yyyy"));
            } else {
                rowData[3] = "";
            }
            
            rowData[4] = "";
            rowData[5] = employee.getPhoneNumber() != null ? employee.getPhoneNumber() : "";
            
            // Get government IDs
            GovIdModel govIds = employeeDAO.getEmployeeGovIds(employee.getEmployeeId());
            rowData[6] = govIds != null ? (govIds.getSss() != null ? govIds.getSss() : "") : "";
            rowData[7] = govIds != null ? (govIds.getPhilhealth() != null ? govIds.getPhilhealth() : "") : "";
            rowData[8] = govIds != null ? (govIds.getPagibig() != null ? govIds.getPagibig() : "") : "";
            rowData[9] = govIds != null ? (govIds.getTin() != null ? govIds.getTin() : "") : "";
            
            rowData[10] = employee.getStatus() != null ? employee.getStatus().getValue() : "";
            
            // Get position information
            PositionModel position = getEmployeePosition(employee.getPositionId());
            rowData[11] = position != null ? position.getPosition() : "";
            
            // Get supervisor name
            String supervisorName = getEmployeeSupervisorName(employee.getSupervisorId());
            rowData[12] = supervisorName != null ? supervisorName : "";
            
            rowData[13] = formatMoney(employee.getBasicSalary());
            
            // Get benefits from position benefits cache
            if (position != null) {
                Map<String, BigDecimal> benefits = positionBenefitsCache.get(position.getPositionId());
                if (benefits != null) {
                    rowData[14] = formatMoney(benefits.get("Rice Subsidy"));
                    rowData[15] = formatMoney(benefits.get("Phone Allowance"));
                    rowData[16] = formatMoney(benefits.get("Clothing Allowance"));
                } else {
                    rowData[14] = "0.00";
                    rowData[15] = "0.00";
                    rowData[16] = "0.00";
                }
            } else {
                rowData[14] = "0.00";
                rowData[15] = "0.00";
                rowData[16] = "0.00";
            }
            
            rowData[17] = formatMoney(employee.getHourlyRate());
            
            model.addRow(rowData);
            
        } catch (Exception e) {
            System.err.println("Error adding employee to table: " + e.getMessage());
        }
    }
    
    private PositionModel getEmployeePosition(Integer positionId) {
        try {
            if (positionId == null) return null;
            return positionDAO.findById(positionId);
        } catch (Exception e) {
            System.err.println("Error getting position: " + e.getMessage());
            return null;
        }
    }
    
    private String getEmployeeSupervisorName(Integer supervisorId) {
        try {
            if (supervisorId == null) return "";
            EmployeeModel supervisor = employeeDAO.findById(supervisorId);
            if (supervisor != null) {
                return supervisor.getFirstName() + " " + supervisor.getLastName();
            }
            return "";
        } catch (Exception e) {
            System.err.println("Error getting supervisor name: " + e.getMessage());
            return "";
        }
    }
    
    private String formatMoney(BigDecimal value) {
        if (value == null) return "0.00";
        return String.format("%,.2f", value);
    }
    
    private void displayEmployeeDetails(int selectedRow) {
        try {
            if (selectedRow < 0) {
                return;
            }
            
            int modelRow = selectedRow;
            if (tblERecords.getRowSorter() != null) {
                modelRow = tblERecords.getRowSorter().convertRowIndexToModel(selectedRow);
            }
            
            Object employeeIdObj = tblERecords.getModel().getValueAt(modelRow, 0);
            if (employeeIdObj == null) {
                return;
            }
            
            int employeeId;
            try {
                employeeId = Integer.parseInt(employeeIdObj.toString());
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Invalid employee ID format", 
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            EmployeeDAO.EmployeeWithGovIds employeeWithGovIds = employeeDAO.getEmployeeWithGovIds(employeeId);
            
            if (employeeWithGovIds == null) {
                JOptionPane.showMessageDialog(this, "Employee not found in database", 
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            selectedEmployee = employeeWithGovIds.getEmployee();
            selectedGovIds = employeeWithGovIds.getGovIds();
            
            // Populate form fields
            TFenum.setText(String.valueOf(selectedEmployee.getEmployeeId()));
            TFlastn.setText(selectedEmployee.getLastName() != null ? selectedEmployee.getLastName() : "");
            TFfirstn.setText(selectedEmployee.getFirstName() != null ? selectedEmployee.getFirstName() : "");
            
            if (selectedEmployee.getBirthDate() != null) {
                jDateChooserBday.setDate(Date.valueOf(selectedEmployee.getBirthDate()));
            } else {
                jDateChooserBday.setDate(null);
            }
            
            TFAddress.setText("");
            TFphonenum.setText(selectedEmployee.getPhoneNumber() != null ? selectedEmployee.getPhoneNumber() : "");
            
            // Set government IDs
            if (selectedGovIds != null) {
                TFsss.setText(selectedGovIds.getSss() != null ? selectedGovIds.getSss() : "");
                TFphilh.setText(selectedGovIds.getPhilhealth() != null ? selectedGovIds.getPhilhealth() : "");
                TFpagibig.setText(selectedGovIds.getPagibig() != null ? selectedGovIds.getPagibig() : "");
                TFtin.setText(selectedGovIds.getTin() != null ? selectedGovIds.getTin() : "");
            } else {
                TFsss.setText("");
                TFphilh.setText("");
                TFpagibig.setText("");
                TFtin.setText("");
            }
            
            // Set status
            if (selectedEmployee.getStatus() != null) {
                TFstatus.setSelectedItem(selectedEmployee.getStatus().getValue());
            } else {
                TFstatus.setSelectedIndex(0);
            }
            
            // Set position and trigger benefit population
            PositionModel position = getEmployeePosition(selectedEmployee.getPositionId());
            if (position != null) {
                TFpos.setSelectedItem(position.getPosition());
            } else {
                TFpos.setSelectedIndex(0);
                clearBenefitFields();
            }
            
            // Set supervisor
            if (selectedEmployee.getSupervisorId() != null) {
                try {
                    EmployeeModel supervisor = employeeDAO.findById(selectedEmployee.getSupervisorId());
                    if (supervisor != null) {
                        String supervisorItem = supervisor.getFirstName() + " " + supervisor.getLastName() + " (ID: " + supervisor.getEmployeeId() + ")";
                        TFsupervisor.setSelectedItem(supervisorItem);
                    } else {
                        TFsupervisor.setSelectedItem("None");
                    }
                } catch (Exception e) {
                    TFsupervisor.setSelectedItem("None");
                }
            } else {
                TFsupervisor.setSelectedItem("None");
            }
            
            // Set monetary values (remove commas for editing)
            TFbasicsalary.setText(selectedEmployee.getBasicSalary() != null ? 
                    selectedEmployee.getBasicSalary().toString() : "");
            TFhourlyrate.setText(selectedEmployee.getHourlyRate() != null ? 
                    selectedEmployee.getHourlyRate().toString() : "");
            
            System.out.println("Loaded employee data for ID: " + employeeId);
            
        } catch (Exception e) {
            System.err.println("Error displaying employee details: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error displaying employee details: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void addEmployee() {
        try {
            if (!validateInput()) {
                JOptionPane.showMessageDialog(this, "Please fill in all required fields", 
                        "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (isDuplicateEmployeeID(TFenum.getText())) {
                JOptionPane.showMessageDialog(this, "Employee ID " + TFenum.getText() + " already exists.", 
                        "Duplicate Employee ID", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            EmployeeModel newEmployee = createEmployeeFromForm();
            GovIdModel newGovIds = createGovIdsFromForm();
            
            boolean success = employeeDAO.saveEmployeeWithGovIds(newEmployee, newGovIds);
            
            if (success) {
                loadEmployeeData();
                setupSupervisorDropdown();
                clearFields();
                
                JOptionPane.showMessageDialog(this, 
                    "Employee and government IDs added successfully!\n" +
                    "Employee ID: " + newEmployee.getEmployeeId(), 
                    "Success", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Failed to add employee to database", 
                        "Database Error", JOptionPane.ERROR_MESSAGE);
            }
            
        } catch (Exception e) {
            System.err.println("Error adding employee: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error adding employee: " + e.getMessage(), 
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void updateEmployee() {
        try {
            if (selectedEmployee == null) {
                JOptionPane.showMessageDialog(this, "Please select an employee to update", 
                        "No Selection", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            if (!validateInput()) {
                JOptionPane.showMessageDialog(this, "Please fill in all required fields", 
                        "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            System.out.println("Updating employee ID: " + selectedEmployee.getEmployeeId());
            
            selectedEmployee.setFirstName(TFfirstn.getText().trim());
            selectedEmployee.setLastName(TFlastn.getText().trim());
            selectedEmployee.setPhoneNumber(TFphonenum.getText().trim());
            selectedEmployee.setEmail(generateEmailFromName(TFfirstn.getText(), TFlastn.getText()));
            
            if (jDateChooserBday.getDate() != null) {
                selectedEmployee.setBirthDate(jDateChooserBday.getDate().toInstant()
                        .atZone(java.time.ZoneId.systemDefault()).toLocalDate());
            }
            
            if (TFstatus.getSelectedItem() != null && !TFstatus.getSelectedItem().toString().equals("Select")) {
                selectedEmployee.setStatus(EmployeeStatus.fromString(TFstatus.getSelectedItem().toString()));
            }
            
            if (!TFbasicsalary.getText().trim().isEmpty()) {
                selectedEmployee.setBasicSalary(new BigDecimal(TFbasicsalary.getText().replace(",", "")));
            }
            
            if (!TFhourlyrate.getText().trim().isEmpty()) {
                selectedEmployee.setHourlyRate(new BigDecimal(TFhourlyrate.getText().replace(",", "")));
            }
            
            Integer positionId = findPositionIdByName(TFpos.getSelectedItem().toString());
            if (positionId != null) {
                selectedEmployee.setPositionId(positionId);
            }
            
            Integer supervisorId = findSupervisorIdBySelection(TFsupervisor.getSelectedItem().toString());
            selectedEmployee.setSupervisorId(supervisorId);
            
            if (selectedGovIds == null) {
                selectedGovIds = new GovIdModel(selectedEmployee.getEmployeeId());
            }
            updateGovIdsFromForm(selectedGovIds);
            
            boolean success = performDirectUpdate();
            
            if (success) {
                loadEmployeeData();
                setupSupervisorDropdown();
                
                JOptionPane.showMessageDialog(this, 
                    "Employee information updated successfully!\n" +
                    "Employee: " + selectedEmployee.getFirstName() + " " + selectedEmployee.getLastName(), 
                    "Success", JOptionPane.INFORMATION_MESSAGE);
                    
                System.out.println("Employee updated successfully");
            } else {
                JOptionPane.showMessageDialog(this, "Failed to update employee information.", 
                        "Update Error", JOptionPane.ERROR_MESSAGE);
                System.err.println("Update failed for employee ID: " + selectedEmployee.getEmployeeId());
            }
            
        } catch (Exception e) {
            System.err.println("Error updating employee: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error updating employee: " + e.getMessage(), 
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private boolean performDirectUpdate() {
        try {
            java.sql.Connection conn = databaseConnection.createConnection();
            conn.setAutoCommit(false);
            
            String employeeSQL = """
                UPDATE employee SET 
                firstName = ?, lastName = ?, birthDate = ?, phoneNumber = ?, email = ?, 
                basicSalary = ?, hourlyRate = ?, userRole = ?, passwordHash = ?, status = ?, 
                lastLogin = ?, positionId = ?, supervisorId = ? 
                WHERE employeeId = ?
                """;
            
            try (java.sql.PreparedStatement empStmt = conn.prepareStatement(employeeSQL)) {
                int paramIndex = 1;
                empStmt.setString(paramIndex++, selectedEmployee.getFirstName());
                empStmt.setString(paramIndex++, selectedEmployee.getLastName());
                
                if (selectedEmployee.getBirthDate() != null) {
                    empStmt.setDate(paramIndex++, java.sql.Date.valueOf(selectedEmployee.getBirthDate()));
                } else {
                    empStmt.setNull(paramIndex++, java.sql.Types.DATE);
                }
                
                empStmt.setString(paramIndex++, selectedEmployee.getPhoneNumber());
                empStmt.setString(paramIndex++, selectedEmployee.getEmail());
                empStmt.setBigDecimal(paramIndex++, selectedEmployee.getBasicSalary());
                empStmt.setBigDecimal(paramIndex++, selectedEmployee.getHourlyRate());
                empStmt.setString(paramIndex++, selectedEmployee.getUserRole());
                empStmt.setString(paramIndex++, selectedEmployee.getPasswordHash());
                empStmt.setString(paramIndex++, selectedEmployee.getStatus().getValue());
                
                if (selectedEmployee.getLastLogin() != null) {
                    empStmt.setTimestamp(paramIndex++, selectedEmployee.getLastLogin());
                } else {
                    empStmt.setNull(paramIndex++, java.sql.Types.TIMESTAMP);
                }
                
                empStmt.setInt(paramIndex++, selectedEmployee.getPositionId());
                
                if (selectedEmployee.getSupervisorId() != null) {
                    empStmt.setInt(paramIndex++, selectedEmployee.getSupervisorId());
                } else {
                    empStmt.setNull(paramIndex++, java.sql.Types.INTEGER);
                }
                
                empStmt.setInt(paramIndex++, selectedEmployee.getEmployeeId());
                
                int empRows = empStmt.executeUpdate();
                
                if (empRows > 0 && selectedGovIds != null) {
                    String govIdSQL = """
                        INSERT INTO govid (sss, philhealth, tin, pagibig, employeeId) 
                        VALUES (?, ?, ?, ?, ?) 
                        ON DUPLICATE KEY UPDATE 
                        sss = VALUES(sss), 
                        philhealth = VALUES(philhealth), 
                        tin = VALUES(tin), 
                        pagibig = VALUES(pagibig)
                        """;
                    
                    try (java.sql.PreparedStatement govStmt = conn.prepareStatement(govIdSQL)) {
                        govStmt.setString(1, selectedGovIds.getSss());
                        govStmt.setString(2, selectedGovIds.getPhilhealth());
                        govStmt.setString(3, selectedGovIds.getTin());
                        govStmt.setString(4, selectedGovIds.getPagibig());
                        govStmt.setInt(5, selectedEmployee.getEmployeeId());
                        
                        govStmt.executeUpdate();
                    }
                }
                
                conn.commit();
                conn.close();
                return empRows > 0;
                
            }
            
        } catch (Exception e) {
            System.err.println("Direct update failed: " + e.getMessage());
            return false;
        }
    }
    
    private void updateEmployeeManually() {
        try {
            boolean employeeUpdated = employeeDAO.update(selectedEmployee);
            
            if (!employeeUpdated) {
                System.err.println("Failed to update employee information");
                return;
            }
            
            if (selectedGovIds != null) {
                try {
                    updateGovIdsManually(selectedGovIds);
                } catch (Exception govIdError) {
                    System.err.println("Government ID update failed but employee updated: " + govIdError.getMessage());
                }
            }
            
        } catch (Exception e) {
            System.err.println("Manual update failed: " + e.getMessage());
        }
    }
    
    private void updateGovIdsManually(GovIdModel govIds) {
        try {
            System.out.println("Government IDs updated (manual approach): " + govIds.toString());
        } catch (Exception e) {
            System.err.println("Error in manual government ID update: " + e.getMessage());
        }
    }
    

    
    private void deleteEmployee() {
        try {
            if (selectedEmployee == null) {
                JOptionPane.showMessageDialog(this, "Please select an employee to delete", 
                        "No Selection", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            String empName = selectedEmployee.getFirstName() + " " + selectedEmployee.getLastName();
            int confirm = JOptionPane.showConfirmDialog(this, 
                    "Are you sure you want to terminate employee " + 
                    selectedEmployee.getEmployeeId() + ": " + empName + "?\n\n" +
                    "This will set their status to TERMINATED (soft delete).", 
                    "Confirm Termination", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            
            if (confirm == JOptionPane.YES_OPTION) {
                boolean success = employeeDAO.deactivateEmployee(selectedEmployee.getEmployeeId());
                
                if (success) {
                    loadEmployeeData();
                    setupSupervisorDropdown();
                    clearFields();
                    selectedEmployee = null;
                    selectedGovIds = null;
                    
                    JOptionPane.showMessageDialog(this, "Employee terminated successfully!", 
                            "Success", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to terminate employee", 
                            "Database Error", JOptionPane.ERROR_MESSAGE);
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error deleting employee: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error deleting employee: " + e.getMessage(), 
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private EmployeeModel createEmployeeFromForm() throws Exception {
        EmployeeModel employee = new EmployeeModel();
        
        employee.setEmployeeId(Integer.parseInt(TFenum.getText()));
        employee.setFirstName(TFfirstn.getText().trim());
        employee.setLastName(TFlastn.getText().trim());
        employee.setEmail(generateEmailFromName(TFfirstn.getText(), TFlastn.getText()));
        employee.setPhoneNumber(TFphonenum.getText().trim());
        
        if (jDateChooserBday.getDate() != null) {
            employee.setBirthDate(jDateChooserBday.getDate().toInstant()
                    .atZone(java.time.ZoneId.systemDefault()).toLocalDate());
        }
        
        employee.setStatus(EmployeeStatus.fromString(TFstatus.getSelectedItem().toString()));
        employee.setBasicSalary(new BigDecimal(TFbasicsalary.getText().replace(",", "")));
        employee.setHourlyRate(new BigDecimal(TFhourlyrate.getText().replace(",", "")));
        
        Integer positionId = findPositionIdByName(TFpos.getSelectedItem().toString());
        if (positionId != null) {
            employee.setPositionId(positionId);
        } else {
            throw new Exception("Position not found: " + TFpos.getSelectedItem().toString());
        }
        
        Integer supervisorId = findSupervisorIdBySelection(TFsupervisor.getSelectedItem().toString());
        employee.setSupervisorId(supervisorId);
        
        employee.setUserRole("Employee");
        employee.setPasswordHash("defaultPassword123"); 
        
        return employee;
    }
    
    private GovIdModel createGovIdsFromForm() {
        GovIdModel govIds = new GovIdModel();
        
        govIds.setSss(TFsss.getText().trim());
        govIds.setPhilhealth(TFphilh.getText().trim());
        govIds.setTin(TFtin.getText().trim());
        govIds.setPagibig(TFpagibig.getText().trim());
        
        govIds.formatAllIds();
        
        return govIds;
    }
    
    private void updateGovIdsFromForm(GovIdModel govIds) {
        govIds.setSss(TFsss.getText().trim());
        govIds.setPhilhealth(TFphilh.getText().trim());
        govIds.setTin(TFtin.getText().trim());
        govIds.setPagibig(TFpagibig.getText().trim());
        
        govIds.formatAllIds();
    }
    
    private String generateEmailFromName(String firstName, String lastName) {
        return (firstName.toLowerCase() + "." + lastName.toLowerCase() + "@motorph.com")
                .replaceAll("\\s+", "");
    }
    
    private Integer findPositionIdByName(String positionName) {
        if ("Select Position".equals(positionName)) {
            return null;
        }
        for (PositionModel position : positionList) {
            if (position.getPosition().equalsIgnoreCase(positionName)) {
                return position.getPositionId();
            }
        }
        return null;
    }
    
    private Integer findSupervisorIdBySelection(String selection) {
        if ("Select Supervisor".equals(selection) || "None".equals(selection)) {
            return null;
        }
        
        if (selection.contains("(ID: ") && selection.contains(")")) {
            String idStr = selection.substring(selection.indexOf("(ID: ") + 5, selection.lastIndexOf(")"));
            try {
                return Integer.parseInt(idStr);
            } catch (NumberFormatException e) {
                System.err.println("Error parsing supervisor ID: " + idStr);
                return null;
            }
        }
        
        return null;
    }
    
    private boolean validateInput() {
        try {
            if (TFenum.getText().trim().isEmpty() ||
                TFlastn.getText().trim().isEmpty() ||
                TFfirstn.getText().trim().isEmpty() ||
                TFphonenum.getText().trim().isEmpty() ||
                TFbasicsalary.getText().trim().isEmpty() ||
                TFhourlyrate.getText().trim().isEmpty()) {
                return false;
            }
            
            if (jDateChooserBday.getDate() == null) {
                return false;
            }
            
            if (TFstatus.getSelectedItem() == null || 
                TFstatus.getSelectedItem().toString().equals("Select")) {
                return false;
            }
            
            if (TFpos.getSelectedItem() == null || 
                TFpos.getSelectedItem().toString().equals("Select Position")) {
                return false;
            }
            
            try {
                Integer.parseInt(TFenum.getText().trim());
                new BigDecimal(TFbasicsalary.getText().replace(",", ""));
                new BigDecimal(TFhourlyrate.getText().replace(",", ""));
            } catch (NumberFormatException e) {
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean isDuplicateEmployeeID(String empID) {
        try {
            Integer employeeId = Integer.parseInt(empID);
            EmployeeModel existing = employeeDAO.findById(employeeId);
            return existing != null;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    private void clearFields() {
        TFenum.setText("");
        TFlastn.setText("");
        TFfirstn.setText("");
        TFsss.setText("");
        TFphilh.setText("");
        TFtin.setText("");
        TFpagibig.setText("");
        TFstatus.setSelectedIndex(0);
        TFpos.setSelectedIndex(0);
        TFsupervisor.setSelectedIndex(0);
        TFbasicsalary.setText("");
        TFricesub.setText("");
        TFphoneallow.setText("");
        TFclothingallow.setText("");
        TFhourlyrate.setText("");
        jDateChooserBday.setDate(null);
        TFAddress.setText("");
        TFphonenum.setText("");
        selectedEmployee = null;
        selectedGovIds = null;
    }
    
    private void searchEmployees(String searchQuery) {
        try {
            if (searchQuery == null || searchQuery.trim().isEmpty()) {
                loadEmployeeData();
                return;
            }
            
            TableRowSorter<TableModel> sorter = new TableRowSorter<>(tblERecords.getModel());
            tblERecords.setRowSorter(sorter);
            
            List<RowFilter<Object, Object>> filters = new ArrayList<>();
            filters.add(RowFilter.regexFilter("(?i)" + searchQuery, 0));  // Employee ID
            filters.add(RowFilter.regexFilter("(?i)" + searchQuery, 1));  // Last Name
            filters.add(RowFilter.regexFilter("(?i)" + searchQuery, 2));  // First Name
            filters.add(RowFilter.regexFilter("(?i)" + searchQuery, 11)); // Position
            
            RowFilter<Object, Object> combinedFilter = RowFilter.orFilter(filters);
            sorter.setRowFilter(combinedFilter);
            
            if (tblERecords.getRowCount() == 0) {
                JOptionPane.showMessageDialog(this, "No matching records found.", 
                        "Search Results", JOptionPane.INFORMATION_MESSAGE);
            }
            
        } catch (Exception e) {
            System.err.println("Error searching employees: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error searching employees: " + e.getMessage(), 
                    "Search Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void addMonetaryFieldTooltips() {
        TFbasicsalary.setToolTipText("Enter amount without commas (e.g., 50000 not 50,000)");
        TFricesub.setToolTipText("Auto-populated based on position");
        TFphoneallow.setToolTipText("Auto-populated based on position");
        TFclothingallow.setToolTipText("Auto-populated based on position");
        TFhourlyrate.setToolTipText("Enter amount without commas (e.g., 250 not 250.00)");
    }
    
    private void adjustRowHeight() {
        for (int row = 0; row < tblERecords.getRowCount(); row++) {
            int rowHeight = tblERecords.getRowHeight();
            for (int column = 0; column < tblERecords.getColumnCount(); column++) {
                Component comp = tblERecords.prepareRenderer(tblERecords.getCellRenderer(row, column), row, column);
                rowHeight = Math.max(rowHeight, comp.getPreferredSize().height);
            }
            tblERecords.setRowHeight(row, rowHeight);
        }
    }
    
    private void adjustColumnWidths() {
        for (int column = 0; column < tblERecords.getColumnCount(); column++) {
            int width = getColumnWidth(column);
            tblERecords.getColumnModel().getColumn(column).setPreferredWidth(width);
        }
    }
    
    private int getColumnWidth(int column) {
        int width = 0;
        TableCellRenderer headerRenderer = tblERecords.getTableHeader().getDefaultRenderer();
        Component headerComp = headerRenderer.getTableCellRendererComponent(
                tblERecords, tblERecords.getColumnModel().getColumn(column).getHeaderValue(), 
                false, false, 0, column);
        width = Math.max(headerComp.getPreferredSize().width + tblERecords.getIntercellSpacing().width, width);
        
        for (int row = 0; row < tblERecords.getRowCount(); row++) {
            TableCellRenderer renderer = tblERecords.getCellRenderer(row, column);
            Component comp = tblERecords.prepareRenderer(renderer, row, column);
            width = Math.max(comp.getPreferredSize().width + tblERecords.getIntercellSpacing().width, width);
        }
        return width;
    }
    
    private void navigateBack() {
        try {
            if (loggedInUser != null) {
                AdminHR adminHR = new AdminHR(loggedInUser);
                adminHR.setVisible(true);
            } else {
                new Login().setVisible(true);
            }
            this.dispose();
        } catch (Exception e) {
            System.err.println("Error navigating back: " + e.getMessage());
            new Login().setVisible(true);
            this.dispose();
        }
    }
    
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        tblERecords = new javax.swing.JTable();
        jPanel1 = new javax.swing.JPanel();
        backbuttondetailsPB = new javax.swing.JButton();
        jLabel14 = new javax.swing.JLabel();
        btnShowAll = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        TFenum = new javax.swing.JTextField();
        jLabel10 = new javax.swing.JLabel();
        TFphilh = new javax.swing.JTextField();
        jLabel15 = new javax.swing.JLabel();
        lblphoneallow = new javax.swing.JLabel();
        TFphoneallow = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        TFlastn = new javax.swing.JTextField();
        jLabel11 = new javax.swing.JLabel();
        TFtin = new javax.swing.JTextField();
        jLabel16 = new javax.swing.JLabel();
        lblclothingallow = new javax.swing.JLabel();
        TFclothingallow = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        TFfirstn = new javax.swing.JTextField();
        jLabel12 = new javax.swing.JLabel();
        TFpagibig = new javax.swing.JTextField();
        lblbasicsalary = new javax.swing.JLabel();
        TFbasicsalary = new javax.swing.JTextField();
        lblricesubsidy = new javax.swing.JLabel();
        TFricesub = new javax.swing.JTextField();
        TFhourlyrate = new javax.swing.JTextField();
        lbhourlyrate = new javax.swing.JLabel();
        TFsss = new javax.swing.JTextField();
        jLabel9 = new javax.swing.JLabel();
        TFstatus = new javax.swing.JComboBox<>();
        jLabel13 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jScrollPane3 = new javax.swing.JScrollPane();
        TFAddress = new javax.swing.JTextArea();
        LBLphonenum = new javax.swing.JLabel();
        TFphonenum = new javax.swing.JTextField();
        btnSave = new javax.swing.JButton();
        btnDelete = new javax.swing.JButton();
        btnUpdate = new javax.swing.JButton();
        btnReset1 = new javax.swing.JButton();
        jDateChooserBday = new com.toedter.calendar.JDateChooser();
        TFpos = new javax.swing.JComboBox<>();
        TFsupervisor = new javax.swing.JComboBox<>();
        btnSearch1 = new javax.swing.JButton();
        unifiedSearchBar = new java.awt.TextField();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setBackground(new java.awt.Color(255, 255, 255));

        tblERecords.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Employee #", "Last Name", "First Name", "Birthday", "Address", "Phone #", "SSS #", "PhilHealth #", "Pag-Ibig #", "TIN", "Status", "Position", "Immediate Supervisor", "Basic Salary", "Rice Subsidy", "Phone Allowance", "Clothing Allowance", "Hourly Rate"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Object.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tblERecords.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_ALL_COLUMNS);
        jScrollPane1.setViewportView(tblERecords);
        if (tblERecords.getColumnModel().getColumnCount() > 0) {
            tblERecords.getColumnModel().getColumn(0).setMinWidth(60);
            tblERecords.getColumnModel().getColumn(0).setPreferredWidth(60);
            tblERecords.getColumnModel().getColumn(12).setPreferredWidth(60);
            tblERecords.getColumnModel().getColumn(13).setMinWidth(80);
            tblERecords.getColumnModel().getColumn(13).setPreferredWidth(80);
        }

        jPanel1.setBackground(new java.awt.Color(220, 95, 0));

        backbuttondetailsPB.setBackground(new java.awt.Color(204, 0, 0));
        backbuttondetailsPB.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        backbuttondetailsPB.setForeground(new java.awt.Color(255, 255, 255));
        backbuttondetailsPB.setText("Back");
        backbuttondetailsPB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                backbuttondetailsPBActionPerformed(evt);
            }
        });

        jLabel14.setBackground(new java.awt.Color(220, 95, 0));
        jLabel14.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel14.setForeground(new java.awt.Color(255, 255, 255));
        jLabel14.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel14.setText("Employee Management");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(25, 25, 25)
                .addComponent(backbuttondetailsPB)
                .addGap(30, 30, 30)
                .addComponent(jLabel14, javax.swing.GroupLayout.PREFERRED_SIZE, 1049, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap(17, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel14)
                    .addComponent(backbuttondetailsPB, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(26, 26, 26))
        );

        btnShowAll.setBackground(new java.awt.Color(220, 95, 0));
        btnShowAll.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnShowAll.setForeground(new java.awt.Color(255, 255, 255));
        btnShowAll.setText("Show all");
        btnShowAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnShowAllActionPerformed(evt);
            }
        });

        jPanel2.setBackground(new java.awt.Color(255, 255, 255));

        jLabel3.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel3.setText("Employee Number");

        TFenum.setHorizontalAlignment(javax.swing.JTextField.LEFT);

        jLabel10.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel10.setText("PhilHealth Number");

        TFphilh.setHorizontalAlignment(javax.swing.JTextField.LEFT);

        jLabel15.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel15.setText("Position");

        lblphoneallow.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lblphoneallow.setText("Phone Allowance");

        TFphoneallow.setHorizontalAlignment(javax.swing.JTextField.LEFT);

        jLabel4.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel4.setText("Last Name");

        TFlastn.setHorizontalAlignment(javax.swing.JTextField.LEFT);

        jLabel11.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel11.setText("TIN");

        TFtin.setHorizontalAlignment(javax.swing.JTextField.LEFT);

        jLabel16.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel16.setText("Immediate Supervisor");

        lblclothingallow.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lblclothingallow.setText("Clothing Allowance");

        TFclothingallow.setHorizontalAlignment(javax.swing.JTextField.LEFT);

        jLabel5.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel5.setText("First Name");

        TFfirstn.setHorizontalAlignment(javax.swing.JTextField.LEFT);

        jLabel12.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel12.setText("Pag-Ibig Number");

        TFpagibig.setHorizontalAlignment(javax.swing.JTextField.LEFT);

        lblbasicsalary.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lblbasicsalary.setText("Basic Salary");

        TFbasicsalary.setHorizontalAlignment(javax.swing.JTextField.LEFT);

        lblricesubsidy.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lblricesubsidy.setText("Rice Subsidy");

        TFricesub.setHorizontalAlignment(javax.swing.JTextField.LEFT);

        TFhourlyrate.setHorizontalAlignment(javax.swing.JTextField.LEFT);

        lbhourlyrate.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lbhourlyrate.setText("Hourly Rate");

        TFsss.setHorizontalAlignment(javax.swing.JTextField.LEFT);

        jLabel9.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel9.setText("SSS Number");

        TFstatus.setEditable(true);
        TFstatus.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Select", "Regular", "Probationary" }));
        TFstatus.setToolTipText("");

        jLabel13.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel13.setText("Status");

        jLabel6.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel6.setText("Birthday");

        jLabel7.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel7.setText("Address");

        TFAddress.setColumns(20);
        TFAddress.setRows(5);
        jScrollPane3.setViewportView(TFAddress);

        LBLphonenum.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        LBLphonenum.setText("Phone Number");

        TFphonenum.setHorizontalAlignment(javax.swing.JTextField.LEFT);

        btnSave.setBackground(new java.awt.Color(0, 153, 0));
        btnSave.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnSave.setForeground(new java.awt.Color(255, 255, 255));
        btnSave.setText("Save");
        btnSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSaveActionPerformed(evt);
            }
        });

        btnDelete.setBackground(new java.awt.Color(207, 10, 10));
        btnDelete.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnDelete.setForeground(new java.awt.Color(255, 255, 255));
        btnDelete.setText("Delete");
        btnDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDeleteActionPerformed(evt);
            }
        });

        btnUpdate.setBackground(new java.awt.Color(220, 95, 0));
        btnUpdate.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnUpdate.setForeground(new java.awt.Color(255, 255, 255));
        btnUpdate.setText("Update");
        btnUpdate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnUpdateActionPerformed(evt);
            }
        });

        btnReset1.setBackground(new java.awt.Color(220, 95, 0));
        btnReset1.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnReset1.setForeground(new java.awt.Color(255, 255, 255));
        btnReset1.setText("Reset");
        btnReset1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnReset1ActionPerformed(evt);
            }
        });

        TFpos.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Select" }));

        TFsupervisor.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Select" }));

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(17, 17, 17)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel11, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(161, 161, 161))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel2Layout.createSequentialGroup()
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(jPanel2Layout.createSequentialGroup()
                                        .addComponent(lblphoneallow, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addGap(65, 65, 65))
                                    .addGroup(jPanel2Layout.createSequentialGroup()
                                        .addComponent(jLabel15, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addGap(128, 128, 128))
                                    .addComponent(TFphoneallow)
                                    .addComponent(TFpos, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addGap(47, 47, 47)
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(jPanel2Layout.createSequentialGroup()
                                        .addComponent(jLabel16, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addGap(31, 31, 31))
                                    .addGroup(jPanel2Layout.createSequentialGroup()
                                        .addComponent(lblclothingallow, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addGap(52, 52, 52))
                                    .addComponent(TFclothingallow, javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(TFsupervisor, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(jPanel2Layout.createSequentialGroup()
                                            .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                            .addGap(105, 105, 105))
                                        .addGroup(jPanel2Layout.createSequentialGroup()
                                            .addComponent(TFenum, javax.swing.GroupLayout.PREFERRED_SIZE, 187, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                                    .addGroup(jPanel2Layout.createSequentialGroup()
                                        .addComponent(TFtin, javax.swing.GroupLayout.PREFERRED_SIZE, 187, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(44, 44, 44)))
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(jPanel2Layout.createSequentialGroup()
                                        .addComponent(jLabel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addGap(119, 119, 119))
                                    .addComponent(TFlastn)
                                    .addGroup(jPanel2Layout.createSequentialGroup()
                                        .addComponent(jLabel10, javax.swing.GroupLayout.PREFERRED_SIZE, 165, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(0, 0, Short.MAX_VALUE))
                                    .addComponent(TFphilh, javax.swing.GroupLayout.Alignment.TRAILING))))
                        .addGap(53, 53, 53)))
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel2Layout.createSequentialGroup()
                                .addComponent(lblricesubsidy, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGap(98, 98, 98))
                            .addComponent(TFbasicsalary, javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(TFpagibig, javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(TFricesub, javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel2Layout.createSequentialGroup()
                                .addComponent(jLabel12, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGap(62, 62, 62))
                            .addComponent(TFfirstn, javax.swing.GroupLayout.Alignment.LEADING))
                        .addGap(46, 46, 46))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(lblbasicsalary, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(180, 180, 180))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 84, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(TFhourlyrate)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(lbhourlyrate, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(101, 101, 101))
                    .addComponent(TFsss)
                    .addComponent(TFstatus, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel13, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(137, 137, 137))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(121, 121, 121))
                    .addComponent(jLabel9, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jDateChooserBday, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(47, 47, 47)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(135, 135, 135))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel2Layout.createSequentialGroup()
                        .addComponent(LBLphonenum, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(87, 87, 87))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(TFphonenum)
                            .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(btnUpdate, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btnReset1, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(btnDelete, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btnSave, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addGap(25, 25, 25))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 18, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(TFfirstn, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 11, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 14, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(TFenum, javax.swing.GroupLayout.DEFAULT_SIZE, 31, Short.MAX_VALUE)
                                    .addComponent(TFlastn))))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel11, javax.swing.GroupLayout.PREFERRED_SIZE, 13, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel10, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel12, javax.swing.GroupLayout.PREFERRED_SIZE, 17, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(TFtin, javax.swing.GroupLayout.DEFAULT_SIZE, 32, Short.MAX_VALUE)
                            .addComponent(TFphilh))
                        .addGap(20, 20, 20)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel15, javax.swing.GroupLayout.PREFERRED_SIZE, 12, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel16, javax.swing.GroupLayout.PREFERRED_SIZE, 15, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(TFpos, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(lblphoneallow, javax.swing.GroupLayout.PREFERRED_SIZE, 14, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(TFbasicsalary, javax.swing.GroupLayout.DEFAULT_SIZE, 28, Short.MAX_VALUE)
                                    .addComponent(TFsss)
                                    .addComponent(TFsupervisor))
                                .addGap(13, 13, 13)
                                .addComponent(lblclothingallow, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGap(4, 4, 4)))
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(TFclothingallow)
                            .addComponent(TFphoneallow)))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(98, 98, 98)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(TFpagibig, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(TFstatus, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(lblbasicsalary, javax.swing.GroupLayout.PREFERRED_SIZE, 18, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(58, 58, 58)
                        .addComponent(lblricesubsidy, javax.swing.GroupLayout.PREFERRED_SIZE, 18, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(TFricesub, javax.swing.GroupLayout.DEFAULT_SIZE, 27, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(jLabel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(jPanel2Layout.createSequentialGroup()
                                        .addComponent(jDateChooserBday, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(12, 12, 12)
                                        .addComponent(jLabel13, javax.swing.GroupLayout.PREFERRED_SIZE, 12, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(55, 55, 55))
                                    .addGroup(jPanel2Layout.createSequentialGroup()
                                        .addComponent(jScrollPane3)
                                        .addGap(18, 18, 18)))
                                .addComponent(LBLphonenum, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(TFphonenum))
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGap(0, 0, Short.MAX_VALUE)
                                .addComponent(jLabel9, javax.swing.GroupLayout.PREFERRED_SIZE, 14, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(43, 43, 43)))
                        .addGap(12, 12, 12)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(lbhourlyrate)
                            .addComponent(btnUpdate)
                            .addComponent(btnSave))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(TFhourlyrate, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btnDelete)
                            .addComponent(btnReset1))))
                .addGap(51, 51, 51))
        );

        btnSearch1.setBackground(new java.awt.Color(220, 95, 0));
        btnSearch1.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnSearch1.setForeground(new java.awt.Color(255, 255, 255));
        btnSearch1.setText("Search employee");
        btnSearch1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSearch1ActionPerformed(evt);
            }
        });

        unifiedSearchBar.setCursor(new java.awt.Cursor(java.awt.Cursor.TEXT_CURSOR));
        unifiedSearchBar.setFont(new java.awt.Font("Segoe UI", 0, 12)); // NOI18N
        unifiedSearchBar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                unifiedSearchBarActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGap(25, 25, 25)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(btnShowAll)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(unifiedSearchBar, javax.swing.GroupLayout.PREFERRED_SIZE, 293, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnSearch1)))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addGap(23, 23, 23))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(btnSearch1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(unifiedSearchBar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(btnShowAll))
                .addGap(12, 12, 12)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 274, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    
    private void btnSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSaveActionPerformed
    //This button handles "save" button action  
        addEmployee();
    }//GEN-LAST:event_btnSaveActionPerformed
    
    private void btnDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDeleteActionPerformed
    //This button handles "delete" button action
        deleteEmployee();
    }//GEN-LAST:event_btnDeleteActionPerformed
    //This button handles "reset" button action
    private void btnReset1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnReset1ActionPerformed
    // clear all the text fields
        clearFields();
    }//GEN-LAST:event_btnReset1ActionPerformed
    
    private void btnUpdateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnUpdateActionPerformed
    //This button handles "update" button action
        updateEmployee();
    }//GEN-LAST:event_btnUpdateActionPerformed
    
    private void btnSearch1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSearch1ActionPerformed
    // Get search query from unified search bar instead of showing popup dialog
    String searchQuery = unifiedSearchBar.getText().trim();
    
    // Check if search bar has default text or is empty
    if (searchQuery.isEmpty() || "Employee Name or ID".equals(searchQuery)) {
        JOptionPane.showMessageDialog(this, "Please enter a search term in the search bar.", 
                "Search Required", JOptionPane.INFORMATION_MESSAGE);
        unifiedSearchBar.requestFocus();
        return;
    }
    
    // Perform the search
    searchEmployees(searchQuery);
    }//GEN-LAST:event_btnSearch1ActionPerformed

    
    private void backbuttondetailsPBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_backbuttondetailsPBActionPerformed
    //This button handles "back" button action; also implements polymorphism as employee (child class)is treated as parent class object since user can't be initialized cause abstract 
        navigateBack();
    }//GEN-LAST:event_backbuttondetailsPBActionPerformed
    
    private void btnShowAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnShowAllActionPerformed
    //This button handles "show all" button action
        tblERecords.setRowSorter(null);
        loadEmployeeData();
    }//GEN-LAST:event_btnShowAllActionPerformed
    
    private void unifiedSearchBarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_unifiedSearchBarActionPerformed
    // Search bar for queries
                String searchText = unifiedSearchBar.getText().trim();
        if (!searchText.isEmpty() && !searchText.equals("Employee Name or ID")) {
            searchEmployees(searchText);
        }
    }//GEN-LAST:event_unifiedSearchBarActionPerformed

    /**
    /**
     * Main method for testing
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
            java.util.logging.Logger.getLogger(EmployeeManagement.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }

        java.awt.EventQueue.invokeLater(() -> {
            new EmployeeManagement().setVisible(true);
        });
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel LBLphonenum;
    private javax.swing.JTextArea TFAddress;
    private javax.swing.JTextField TFbasicsalary;
    private javax.swing.JTextField TFclothingallow;
    private javax.swing.JTextField TFenum;
    private javax.swing.JTextField TFfirstn;
    private javax.swing.JTextField TFhourlyrate;
    private javax.swing.JTextField TFlastn;
    private javax.swing.JTextField TFpagibig;
    private javax.swing.JTextField TFphilh;
    private javax.swing.JTextField TFphoneallow;
    private javax.swing.JTextField TFphonenum;
    private javax.swing.JComboBox<String> TFpos;
    private javax.swing.JTextField TFricesub;
    private javax.swing.JTextField TFsss;
    private javax.swing.JComboBox<String> TFstatus;
    private javax.swing.JComboBox<String> TFsupervisor;
    private javax.swing.JTextField TFtin;
    private javax.swing.JButton backbuttondetailsPB;
    private javax.swing.JButton btnDelete;
    private javax.swing.JButton btnReset1;
    private javax.swing.JButton btnSave;
    private javax.swing.JButton btnSearch1;
    private javax.swing.JButton btnShowAll;
    private javax.swing.JButton btnUpdate;
    private com.toedter.calendar.JDateChooser jDateChooserBday;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JLabel lbhourlyrate;
    private javax.swing.JLabel lblbasicsalary;
    private javax.swing.JLabel lblclothingallow;
    private javax.swing.JLabel lblphoneallow;
    private javax.swing.JLabel lblricesubsidy;
    private javax.swing.JTable tblERecords;
    private java.awt.TextField unifiedSearchBar;
    // End of variables declaration//GEN-END:variables

    


}

       
