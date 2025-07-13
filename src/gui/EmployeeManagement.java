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
 * Employee Management GUI with integrated Government ID and Address handling
 * Enhanced to save/update employee data across employee, govid, and address tables
 * @author User
 */
public class EmployeeManagement extends javax.swing.JFrame {
    
    // User session information
    private final UserAuthenticationModel loggedInUser;
    private final String userRole;
    private final HRModel hrModel;
    
    // DAO instances
    private final DatabaseConnection databaseConnection;
    private final EmployeeDAO employeeDAO;
    private final PositionDAO positionDAO;
    private final AddressDAO addressDAO; // NEW: Address DAO
    
    // Current employee data
    private List<EmployeeModel> employeeList;
    private List<PositionModel> positionList;
    private EmployeeModel selectedEmployee;
    private GovIdModel selectedGovIds;
    private AddressModel selectedAddress; // NEW: Selected address
    
    // Position benefits cache
    private Map<Integer, Map<String, BigDecimal>> positionBenefitsCache = new HashMap<>();
    
    public EmployeeManagement(UserAuthenticationModel loggedInUser) {
        this.loggedInUser = loggedInUser;
        this.userRole = (loggedInUser != null) ? loggedInUser.getUserRole() : "HR";
        
        // Initialize HRModel
        this.hrModel = new HRModel();
        if (loggedInUser != null && "HR".equals(loggedInUser.getUserRole())) {
            this.hrModel.setHrId(loggedInUser.getEmployeeId());
            this.hrModel.setFirstName(loggedInUser.getFirstName());
            this.hrModel.setLastName(loggedInUser.getLastName());
            this.hrModel.setEmail(loggedInUser.getEmail());
            this.hrModel.setPosition("HR Manager");
            this.hrModel.setLastLogin(loggedInUser.getLastLogin());
        }
        
        this.databaseConnection = new DatabaseConnection();
        this.employeeDAO = new EmployeeDAO(databaseConnection);
        this.positionDAO = new PositionDAO(databaseConnection);
        this.addressDAO = new AddressDAO(databaseConnection); // NEW: Initialize Address DAO
        
        initializeGUI();
    }
    
    public EmployeeManagement() {
        this.loggedInUser = null;
        this.userRole = "HR";
        this.hrModel = new HRModel();
        
        this.databaseConnection = new DatabaseConnection();
        this.employeeDAO = new EmployeeDAO(databaseConnection);
        this.positionDAO = new PositionDAO(databaseConnection);
        this.addressDAO = new AddressDAO(databaseConnection); // NEW: Initialize Address DAO
        
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
            
            // Get proper HR user display name
            String hrDisplayName = getHRUserDisplayName();
            
            JOptionPane.showMessageDialog(this, 
                "Employee Management System loaded successfully!\n" +
                "HR: " + hrDisplayName + "\n" +
                "Benefits are automatically populated based on position\n" +
                "Click on any employee row to view/edit details",
                "System Ready", JOptionPane.INFORMATION_MESSAGE);
                
        } catch (Exception e) {
            System.err.println("Error initializing Employee Management: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, 
                "Error initializing Employee Management: " + e.getMessage(), 
                "Initialization Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Get the proper HR user display name for system messages
     */
    private String getHRUserDisplayName() {
        if (loggedInUser != null) {
            if (loggedInUser.getFirstName() != null && loggedInUser.getLastName() != null && 
                !loggedInUser.getFirstName().trim().isEmpty() && !loggedInUser.getLastName().trim().isEmpty()) {
                return loggedInUser.getFirstName().trim() + " " + loggedInUser.getLastName().trim();
            }
            
            String displayName = loggedInUser.getDisplayName();
            if (displayName != null && !displayName.startsWith("User ") && 
                !displayName.equals("HR") && !displayName.contains("HR ")) {
                return displayName;
            }
            
            if (loggedInUser.getEmail() != null && !loggedInUser.getEmail().trim().isEmpty()) {
                return loggedInUser.getEmail();
            }
            
            return "HR Employee #" + loggedInUser.getEmployeeId();
        }
        
        if (hrModel != null) {
            if (hrModel.getFirstName() != null && hrModel.getLastName() != null && 
                !hrModel.getFirstName().trim().isEmpty() && !hrModel.getLastName().trim().isEmpty()) {
                return hrModel.getFirstName().trim() + " " + hrModel.getLastName().trim();
            }
            
            String hrDisplayName = hrModel.getDisplayName();
            if (hrDisplayName != null && !hrDisplayName.startsWith("HR ") && 
                !hrDisplayName.equals("HR") && !hrDisplayName.contains("HR ")) {
                return hrDisplayName;
            }
            
            if (hrModel.getEmail() != null && !hrModel.getEmail().trim().isEmpty()) {
                return hrModel.getEmail();
            }
            
            if (hrModel.getHrId() > 0) {
                return "HR Employee #" + hrModel.getHrId();
            }
        }
        
        return "HR Administrator";
    }
    
    private void setupGUI() {
        // Remove old TFAddress setup since we now use individual fields
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
            if (!hrModel.isCanManagePositions()) {
                System.err.println("HR user does not have permission to manage positions");
                return;
            }
            
            positionList = positionDAO.findAll();
            System.out.println("Loaded " + positionList.size() + " positions");
        } catch (Exception e) {
            System.err.println("Error loading positions: " + e.getMessage());
            positionList = new ArrayList<>();
        }
    }
    
    private void loadPositionBenefits() {
        try {
            if (!hrModel.isCanManageBenefits()) {
                System.err.println("HR user does not have permission to manage benefits");
                return;
            }
            
            positionBenefitsCache.clear();
            
            for (PositionModel position : positionList) {
                Map<String, BigDecimal> benefits = new HashMap<>();
                
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
            DefaultComboBoxModel<String> positionModel = new DefaultComboBoxModel<>();
            positionModel.addElement("Select Position");
            for (PositionModel position : positionList) {
                positionModel.addElement(position.getPosition());
            }
            TFpos.setModel(positionModel);
            
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
            if (!hrModel.isCanManageBenefits()) {
                return;
            }
            
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
            if (!hrModel.isCanManageEmployees()) {
                JOptionPane.showMessageDialog(this, 
                    "You do not have permission to manage employees", 
                    "Access Denied", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
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
            
            // NEW: Load and display address
            AddressModel employeeAddress = getEmployeeAddress(employee.getEmployeeId());
            rowData[4] = employeeAddress != null ? employeeAddress.getShortAddress() : "";
            
            rowData[5] = employee.getPhoneNumber() != null ? employee.getPhoneNumber() : "";
            
            GovIdModel govIds = employeeDAO.getEmployeeGovIds(employee.getEmployeeId());
            rowData[6] = govIds != null ? (govIds.getSss() != null ? govIds.getSss() : "") : "";
            rowData[7] = govIds != null ? (govIds.getPhilhealth() != null ? govIds.getPhilhealth() : "") : "";
            rowData[8] = govIds != null ? (govIds.getPagibig() != null ? govIds.getPagibig() : "") : "";
            rowData[9] = govIds != null ? (govIds.getTin() != null ? govIds.getTin() : "") : "";
            
            rowData[10] = employee.getStatus() != null ? employee.getStatus().getValue() : "";
            
            PositionModel position = getEmployeePosition(employee.getPositionId());
            rowData[11] = position != null ? position.getPosition() : "";
            
            String supervisorName = getEmployeeSupervisorName(employee.getSupervisorId());
            rowData[12] = supervisorName != null ? supervisorName : "";
            
            rowData[13] = formatMoney(employee.getBasicSalary());
            
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
    
    /**
     * Get employee address from database via employeeaddress relationship
     */
    private AddressModel getEmployeeAddress(Integer employeeId) {
        if (employeeId == null) {
            return null;
        }
        
        try {
            String sql = """
                SELECT a.* FROM address a 
                JOIN employeeaddress ea ON a.addressId = ea.addressId 
                WHERE ea.employeeId = ?
                """;
            
            java.sql.Connection conn = databaseConnection.createConnection();
            java.sql.PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, employeeId);
            java.sql.ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                AddressModel address = new AddressModel();
                address.setAddressId(rs.getInt("addressId"));
                address.setStreet(rs.getString("street"));
                address.setBarangay(rs.getString("barangay"));
                address.setCity(rs.getString("city"));
                address.setProvince(rs.getString("province"));
                address.setZipCode(rs.getString("zipCode"));
                
                rs.close();
                pstmt.close();
                conn.close();
                
                return address;
            }
            
            rs.close();
            pstmt.close();
            conn.close();
            
        } catch (Exception e) {
            System.err.println("Error getting employee address: " + e.getMessage());
        }
        
        return null;
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
            selectedAddress = getEmployeeAddress(employeeId); // NEW: Load address
            
            TFenum.setText(String.valueOf(selectedEmployee.getEmployeeId()));
            TFlastn.setText(selectedEmployee.getLastName() != null ? selectedEmployee.getLastName() : "");
            TFfirstn.setText(selectedEmployee.getFirstName() != null ? selectedEmployee.getFirstName() : "");
            
            if (selectedEmployee.getBirthDate() != null) {
                jDateChooserBday.setDate(Date.valueOf(selectedEmployee.getBirthDate()));
            } else {
                jDateChooserBday.setDate(null);
            }
            
            // Display address in individual fields instead of text area
            if (selectedAddress != null) {
                TFAddressStreet.setText(selectedAddress.getStreet() != null ? selectedAddress.getStreet() : "");
                TFAddressBarangay.setText(selectedAddress.getBarangay() != null ? selectedAddress.getBarangay() : "");
                TFAddressCity.setText(selectedAddress.getCity() != null ? selectedAddress.getCity() : "");
                TFAddressProvince.setText(selectedAddress.getProvince() != null ? selectedAddress.getProvince() : "");
                TFAddressZipcode.setText(selectedAddress.getZipCode() != null ? selectedAddress.getZipCode() : "");
            } else {
                TFAddressStreet.setText("");
                TFAddressBarangay.setText("");
                TFAddressCity.setText("");
                TFAddressProvince.setText("");
                TFAddressZipcode.setText("");
            }
            
            TFphonenum.setText(selectedEmployee.getPhoneNumber() != null ? selectedEmployee.getPhoneNumber() : "");
            
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
            
            if (selectedEmployee.getStatus() != null) {
                TFstatus.setSelectedItem(selectedEmployee.getStatus().getValue());
            } else {
                TFstatus.setSelectedIndex(0);
            }
            
            PositionModel position = getEmployeePosition(selectedEmployee.getPositionId());
            if (position != null) {
                TFpos.setSelectedItem(position.getPosition());
            } else {
                TFpos.setSelectedIndex(0);
                clearBenefitFields();
            }
            
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
    
    /**
     * Add employee with address and government IDs
     */
    private void addEmployee() {
        try {
            if (!hrModel.isCanManageEmployees()) {
                JOptionPane.showMessageDialog(this, 
                    "You do not have permission to manage employees", 
                    "Access Denied", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
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
            
            // Create employee, address, and government IDs from form
            EmployeeModel newEmployee = createEmployeeFromForm();
            AddressModel newAddress = createAddressFromForm(); // NEW: Create address
            GovIdModel newGovIds = createGovIdsFromForm();
            
            Integer positionId = findPositionIdByName(TFpos.getSelectedItem().toString());
            boolean hrSuccess = hrModel.createEmployee(
                newEmployee.getFirstName(),
                newEmployee.getLastName(),
                newEmployee.getEmail(),
                positionId != null ? positionId : 0,
                newEmployee.getBasicSalary()
            );
            
            if (!hrSuccess) {
                JOptionPane.showMessageDialog(this, 
                    "Failed to validate employee creation through HR model", 
                    "HR Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // NEW: Save employee with address and government IDs
            boolean success = saveEmployeeWithAddressAndGovIds(newEmployee, newAddress, newGovIds);
            
            if (success) {
                loadEmployeeData();
                setupSupervisorDropdown();
                clearFields();
                
                JOptionPane.showMessageDialog(this, 
                    "Employee, address, and government IDs added successfully!\n" +
                    "Employee ID: " + newEmployee.getEmployeeId() + "\n" +
                    "Address: " + (newAddress != null ? newAddress.getShortAddress() : "No address") + "\n" +
                    "Created by: " + hrModel.getDisplayName(), 
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
    
    /**
     * Update employee with address and government IDs
     */
    private void updateEmployee() {
        try {
            if (!hrModel.isCanManageEmployees()) {
                JOptionPane.showMessageDialog(this, 
                    "You do not have permission to manage employees", 
                    "Access Denied", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
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
            
            Map<String, Object> updates = new HashMap<>();
            updates.put("firstName", TFfirstn.getText().trim());
            updates.put("lastName", TFlastn.getText().trim());
            updates.put("phoneNumber", TFphonenum.getText().trim());
            updates.put("email", generateEmailFromName(TFfirstn.getText(), TFlastn.getText()));
            
            boolean hrSuccess = hrModel.updateEmployee(selectedEmployee.getEmployeeId(), updates);
            
            if (!hrSuccess) {
                JOptionPane.showMessageDialog(this, 
                    "Failed to validate employee update through HR model", 
                    "HR Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Update employee object
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
            
            // NEW: Update address from form
            AddressModel updatedAddress = createAddressFromForm();
            
            if (selectedGovIds == null) {
                selectedGovIds = new GovIdModel(selectedEmployee.getEmployeeId());
            }
            updateGovIdsFromForm(selectedGovIds);
            
            // NEW: Update employee with address and government IDs
            boolean success = updateEmployeeWithAddressAndGovIds(selectedEmployee, updatedAddress, selectedGovIds);
            
            if (success) {
                loadEmployeeData();
                setupSupervisorDropdown();
                
                JOptionPane.showMessageDialog(this, 
                    "Employee information updated successfully!\n" +
                    "Employee: " + selectedEmployee.getFirstName() + " " + selectedEmployee.getLastName() + "\n" +
                    "Address: " + (updatedAddress != null ? updatedAddress.getShortAddress() : "No address") + "\n" +
                    "Updated by: " + hrModel.getDisplayName(), 
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
    
    /**
     * Save employee with address and government IDs in a single transaction
     */
    private boolean saveEmployeeWithAddressAndGovIds(EmployeeModel employee, AddressModel address, GovIdModel govIds) {
        java.sql.Connection conn = null;
        try {
            conn = databaseConnection.createConnection();
            conn.setAutoCommit(false); // Start transaction
            
            // 1. Save employee first
            boolean employeeSaved = employeeDAO.save(employee);
            if (!employeeSaved) {
                conn.rollback();
                System.err.println("Failed to save employee");
                return false;
            }
            
            // 2. Save address if provided and link to employee
            if (address != null && address.isValid()) {
                boolean addressSaved = addressDAO.save(address);
                if (addressSaved) {
                    // Link employee to address via employeeaddress table
                    String linkSQL = "INSERT INTO employeeaddress (employeeId, addressId) VALUES (?, ?)";
                    try (java.sql.PreparedStatement linkStmt = conn.prepareStatement(linkSQL)) {
                        linkStmt.setInt(1, employee.getEmployeeId());
                        linkStmt.setInt(2, address.getAddressId());
                        int linkResult = linkStmt.executeUpdate();
                        if (linkResult == 0) {
                            conn.rollback();
                            System.err.println("Failed to link employee to address");
                            return false;
                        }
                    }
                } else {
                    conn.rollback();
                    System.err.println("Failed to save address");
                    return false;
                }
            }
            
            // 3. Save government IDs if provided
            if (govIds != null) {
                govIds.setEmployeeId(employee.getEmployeeId());
                String govIdSQL = "INSERT INTO govid (sss, philhealth, tin, pagibig, employeeId) VALUES (?, ?, ?, ?, ?)";
                try (java.sql.PreparedStatement govStmt = conn.prepareStatement(govIdSQL)) {
                    govStmt.setString(1, govIds.getSss());
                    govStmt.setString(2, govIds.getPhilhealth());
                    govStmt.setString(3, govIds.getTin());
                    govStmt.setString(4, govIds.getPagibig());
                    govStmt.setInt(5, employee.getEmployeeId());
                    int govResult = govStmt.executeUpdate();
                    if (govResult == 0) {
                        conn.rollback();
                        System.err.println("Failed to save government IDs");
                        return false;
                    }
                }
            }
            
            conn.commit(); // Commit transaction
            System.out.println("Employee, address, and government IDs saved successfully");
            return true;
            
        } catch (Exception e) {
            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (Exception rollbackEx) {
                System.err.println("Error rolling back transaction: " + rollbackEx.getMessage());
            }
            System.err.println("Error saving employee with address and government IDs: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close();
                }
            } catch (Exception e) {
                System.err.println("Error closing connection: " + e.getMessage());
            }
        }
    }
    
    /**
     * NEW: Update employee with address and government IDs in a single transaction
     */
    private boolean updateEmployeeWithAddressAndGovIds(EmployeeModel employee, AddressModel address, GovIdModel govIds) {
        java.sql.Connection conn = null;
        try {
            conn = databaseConnection.createConnection();
            conn.setAutoCommit(false); // Start transaction
            
            // 1. Update employee
            boolean employeeUpdated = employeeDAO.update(employee);
            if (!employeeUpdated) {
                conn.rollback();
                return false;
            }
            
            // 2. Update or save address
            if (address != null && address.isValid()) {
                if (selectedAddress != null && selectedAddress.getAddressId() != null) {
                    // Update existing address
                    address.setAddressId(selectedAddress.getAddressId());
                    addressDAO.update(address);
                } else {
                    // Save new address and link to employee
                    boolean addressSaved = addressDAO.save(address);
                    if (addressSaved) {
                        // Link employee to new address
                        String linkSQL = """
                            INSERT INTO employeeaddress (employeeId, addressId) 
                            VALUES (?, ?) 
                            ON DUPLICATE KEY UPDATE addressId = VALUES(addressId)
                            """;
                        try (java.sql.PreparedStatement linkStmt = conn.prepareStatement(linkSQL)) {
                            linkStmt.setInt(1, employee.getEmployeeId());
                            linkStmt.setInt(2, address.getAddressId());
                            linkStmt.executeUpdate();
                        }
                    }
                }
            }
            
            // 3. Update or save government IDs
            if (govIds != null) {
                govIds.setEmployeeId(employee.getEmployeeId());
                
                // Check if government IDs exist
                String checkSQL = "SELECT COUNT(*) FROM govid WHERE employeeId = ?";
                try (java.sql.PreparedStatement checkStmt = conn.prepareStatement(checkSQL)) {
                    checkStmt.setInt(1, employee.getEmployeeId());
                    java.sql.ResultSet rs = checkStmt.executeQuery();
                    
                    if (rs.next() && rs.getInt(1) > 0) {
                        // Update existing government IDs
                        String updateSQL = "UPDATE govid SET sss = ?, philhealth = ?, tin = ?, pagibig = ? WHERE employeeId = ?";
                        try (java.sql.PreparedStatement updateStmt = conn.prepareStatement(updateSQL)) {
                            updateStmt.setString(1, govIds.getSss());
                            updateStmt.setString(2, govIds.getPhilhealth());
                            updateStmt.setString(3, govIds.getTin());
                            updateStmt.setString(4, govIds.getPagibig());
                            updateStmt.setInt(5, employee.getEmployeeId());
                            updateStmt.executeUpdate();
                        }
                    } else {
                        // Insert new government IDs
                        String insertSQL = "INSERT INTO govid (sss, philhealth, tin, pagibig, employeeId) VALUES (?, ?, ?, ?, ?)";
                        try (java.sql.PreparedStatement insertStmt = conn.prepareStatement(insertSQL)) {
                            insertStmt.setString(1, govIds.getSss());
                            insertStmt.setString(2, govIds.getPhilhealth());
                            insertStmt.setString(3, govIds.getTin());
                            insertStmt.setString(4, govIds.getPagibig());
                            insertStmt.setInt(5, employee.getEmployeeId());
                            insertStmt.executeUpdate();
                        }
                    }
                    rs.close();
                }
            }
            
            conn.commit(); // Commit transaction
            System.out.println("✅ Employee, address, and government IDs updated successfully");
            return true;
            
        } catch (Exception e) {
            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (Exception rollbackEx) {
                System.err.println("Error rolling back transaction: " + rollbackEx.getMessage());
            }
            System.err.println("Error updating employee with address and government IDs: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close();
                }
            } catch (Exception e) {
                System.err.println("Error closing connection: " + e.getMessage());
            }
        }
    }
    
    /**
     * Create AddressModel from individual address form fields
     */
    private AddressModel createAddressFromForm() {
        // Check if at least one address field has data
        if (TFAddressStreet.getText().trim().isEmpty() && 
            TFAddressBarangay.getText().trim().isEmpty() && 
            TFAddressCity.getText().trim().isEmpty() && 
            TFAddressProvince.getText().trim().isEmpty() && 
            TFAddressZipcode.getText().trim().isEmpty()) {
            return null; // No address data provided
        }
        
        AddressModel address = new AddressModel();
        
        // Set fields directly from individual text fields
        String street = TFAddressStreet.getText().trim();
        String barangay = TFAddressBarangay.getText().trim();
        String city = TFAddressCity.getText().trim();
        String province = TFAddressProvince.getText().trim();
        String zipCode = TFAddressZipcode.getText().trim();
        
        address.setStreet(street.isEmpty() ? null : street);
        address.setBarangay(barangay.isEmpty() ? null : barangay);
        address.setCity(city.isEmpty() ? null : city);
        address.setProvince(province.isEmpty() ? null : province);
        address.setZipCode(zipCode.isEmpty() ? null : zipCode);
        
        // Apply defaults for empty required fields
        if (address.getCity() == null || address.getCity().isEmpty()) {
            address.setCity("Manila"); // Default city
        }
        
        if (address.getProvince() == null || address.getProvince().isEmpty()) {
            address.setProvince("Metro Manila"); // Default province
        }
        
        // Normalize the address
        address.normalizeAddress();
        
        return address;
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
                    "This will set their status to TERMINATED (soft delete).\n" +
                    "Address and Government ID records will be preserved.", 
                    "Confirm Termination", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            
            if (confirm == JOptionPane.YES_OPTION) {
                boolean success = employeeDAO.deactivateEmployee(selectedEmployee.getEmployeeId());
                
                if (success) {
                    loadEmployeeData();
                    setupSupervisorDropdown();
                    clearFields();
                    selectedEmployee = null;
                    selectedGovIds = null;
                    selectedAddress = null;
                    
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
    
    /**
     * Validate individual address fields
     */
    private boolean isValidAddressInput() {
        // At least city should be provided for a valid address
        String city = TFAddressCity.getText().trim();
        String province = TFAddressProvince.getText().trim();
        String zipCode = TFAddressZipcode.getText().trim();
        
        // If any address field is filled, city should be mandatory
        boolean hasAnyAddressData = !TFAddressStreet.getText().trim().isEmpty() ||
                                   !TFAddressBarangay.getText().trim().isEmpty() ||
                                   !city.isEmpty() ||
                                   !province.isEmpty() ||
                                   !zipCode.isEmpty();
        
        if (hasAnyAddressData && city.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "City is required when providing address information", 
                "Address Validation", JOptionPane.WARNING_MESSAGE);
            TFAddressCity.requestFocus();
            return false;
        }
        
        // Validate ZIP code format if provided
        if (!zipCode.isEmpty() && !zipCode.matches("\\d{4}")) {
            JOptionPane.showMessageDialog(this, 
                "ZIP code must be 4 digits (e.g., 1000)", 
                "Address Validation", JOptionPane.WARNING_MESSAGE);
            TFAddressZipcode.requestFocus();
            return false;
        }
        
        return true;
    }
    
    private void addMonetaryFieldTooltips() {
        TFbasicsalary.setToolTipText("Enter amount without commas (e.g., 50000 not 50,000)");
        TFricesub.setToolTipText("Auto-populated based on position");
        TFphoneallow.setToolTipText("Auto-populated based on position");
        TFclothingallow.setToolTipText("Auto-populated based on position");
        TFhourlyrate.setToolTipText("Enter amount without commas (e.g., 250 not 250.00)");
        
        // Individual address field tooltips
        TFAddressStreet.setToolTipText("Enter house no./unit no., bldg. name, and street address (e.g., House #27, 123 Main Street)");
        TFAddressBarangay.setToolTipText("Enter barangay name (e.g., Barangay San Antonio)");
        TFAddressCity.setToolTipText("Enter city name (e.g., Manila City, Quezon City)");
        TFAddressProvince.setToolTipText("Enter province name (e.g., Metro Manila, Rizal)");
        TFAddressZipcode.setToolTipText("Enter 4-digit ZIP code (e.g., 1000)");
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
            
            // Validate address fields
            if (!isValidAddressInput()) {
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
        // Clear individual address fields
        TFAddressStreet.setText("");
        TFAddressBarangay.setText("");
        TFAddressCity.setText("");
        TFAddressProvince.setText("");
        TFAddressZipcode.setText("");
        TFphonenum.setText("");
        selectedEmployee = null;
        selectedGovIds = null;
        selectedAddress = null;
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
        LBLphonenum = new javax.swing.JLabel();
        TFphonenum = new javax.swing.JTextField();
        btnSave = new javax.swing.JButton();
        btnDelete = new javax.swing.JButton();
        btnUpdate = new javax.swing.JButton();
        btnReset1 = new javax.swing.JButton();
        jDateChooserBday = new com.toedter.calendar.JDateChooser();
        TFpos = new javax.swing.JComboBox<>();
        TFsupervisor = new javax.swing.JComboBox<>();
        TFAddressStreet = new javax.swing.JTextField();
        TFAddressBarangay = new javax.swing.JTextField();
        TFAddressCity = new javax.swing.JTextField();
        TFAddressProvince = new javax.swing.JTextField();
        TFAddressZipcode = new javax.swing.JTextField();
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
        tblERecords.setPreferredSize(new java.awt.Dimension(1450, 920));
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
        TFphoneallow.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                TFphoneallowActionPerformed(evt);
            }
        });

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
        TFricesub.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                TFricesubActionPerformed(evt);
            }
        });

        TFhourlyrate.setHorizontalAlignment(javax.swing.JTextField.LEFT);
        TFhourlyrate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                TFhourlyrateActionPerformed(evt);
            }
        });

        lbhourlyrate.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lbhourlyrate.setText("Hourly Rate");

        TFsss.setHorizontalAlignment(javax.swing.JTextField.LEFT);
        TFsss.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                TFsssActionPerformed(evt);
            }
        });

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

        TFAddressStreet.setText("House No. & Street Name");

        TFAddressBarangay.setText("Barangay");

        TFAddressCity.setText("City");

        TFAddressProvince.setText("Province");

        TFAddressZipcode.setText("ZIP Code");

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
                                    .addComponent(TFsupervisor, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(lblclothingallow, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(TFclothingallow, javax.swing.GroupLayout.Alignment.TRAILING)))
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
                        .addGap(110, 110, 110))
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
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(btnUpdate, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(btnReset1, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(btnDelete, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(btnSave, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addComponent(TFphonenum, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(jLabel7, javax.swing.GroupLayout.DEFAULT_SIZE, 127, Short.MAX_VALUE)
                                .addGap(135, 135, 135))
                            .addComponent(TFAddressStreet, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(TFAddressBarangay, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(TFAddressCity)
                            .addComponent(TFAddressProvince)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(TFAddressZipcode, javax.swing.GroupLayout.PREFERRED_SIZE, 131, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, Short.MAX_VALUE)))
                        .addGap(25, 25, 25))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(LBLphonenum, javax.swing.GroupLayout.PREFERRED_SIZE, 111, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel2Layout.createSequentialGroup()
                        .addGap(98, 98, 98)
                        .addComponent(TFpagibig, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(lblbasicsalary)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel2Layout.createSequentialGroup()
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                    .addGroup(jPanel2Layout.createSequentialGroup()
                                        .addGap(2, 2, 2)
                                        .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 18, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(TFfirstn, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(jPanel2Layout.createSequentialGroup()
                                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                            .addComponent(jLabel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                            .addComponent(TFenum, javax.swing.GroupLayout.DEFAULT_SIZE, 31, Short.MAX_VALUE)
                                            .addComponent(TFlastn))))
                                .addGap(18, 18, 18)
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabel11, javax.swing.GroupLayout.PREFERRED_SIZE, 13, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel10, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(jLabel12))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(TFtin, javax.swing.GroupLayout.DEFAULT_SIZE, 32, Short.MAX_VALUE)
                                    .addComponent(TFphilh))
                                .addGap(16, 16, 16)
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabel16, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel15, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(jPanel2Layout.createSequentialGroup()
                                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                            .addGroup(jPanel2Layout.createSequentialGroup()
                                                .addGap(1, 1, 1)
                                                .addComponent(TFsupervisor, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE))
                                            .addComponent(TFsss, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addComponent(TFbasicsalary))
                                        .addGap(18, 18, 18)
                                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(lbhourlyrate)
                                            .addComponent(lblricesubsidy, javax.swing.GroupLayout.PREFERRED_SIZE, 18, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addComponent(lblclothingallow))
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                .addComponent(TFricesub, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addComponent(TFclothingallow, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE))
                                            .addComponent(TFhourlyrate)))
                                    .addGroup(jPanel2Layout.createSequentialGroup()
                                        .addComponent(TFpos, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(18, 18, 18)
                                        .addComponent(lblphoneallow, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(TFphoneallow, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE))))
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(jLabel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(jPanel2Layout.createSequentialGroup()
                                        .addComponent(TFAddressStreet, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(TFAddressBarangay, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(TFAddressCity, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(TFAddressProvince, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(24, 24, 24)
                                        .addComponent(TFAddressZipcode, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(22, 22, 22)
                                        .addComponent(LBLphonenum, javax.swing.GroupLayout.DEFAULT_SIZE, 22, Short.MAX_VALUE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(TFphonenum, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                            .addComponent(btnUpdate)
                                            .addComponent(btnSave))
                                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(btnReset1)
                                            .addComponent(btnDelete)))
                                    .addGroup(jPanel2Layout.createSequentialGroup()
                                        .addComponent(jDateChooserBday, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(13, 13, 13)
                                        .addComponent(jLabel13, javax.swing.GroupLayout.PREFERRED_SIZE, 18, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(TFstatus, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(jLabel9)
                                        .addGap(0, 0, Short.MAX_VALUE)))))
                        .addGap(31, 31, 31))))
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
                        .addComponent(btnShowAll)
                        .addGap(652, 652, 652)
                        .addComponent(unifiedSearchBar, javax.swing.GroupLayout.PREFERRED_SIZE, 293, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnSearch1)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(24, 24, 24))
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

    private void TFricesubActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_TFricesubActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_TFricesubActionPerformed

    private void TFphoneallowActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_TFphoneallowActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_TFphoneallowActionPerformed

    private void TFsssActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_TFsssActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_TFsssActionPerformed

    private void TFhourlyrateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_TFhourlyrateActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_TFhourlyrateActionPerformed

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
    private javax.swing.JTextField TFAddressBarangay;
    private javax.swing.JTextField TFAddressCity;
    private javax.swing.JTextField TFAddressProvince;
    private javax.swing.JTextField TFAddressStreet;
    private javax.swing.JTextField TFAddressZipcode;
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
    private javax.swing.JLabel lbhourlyrate;
    private javax.swing.JLabel lblbasicsalary;
    private javax.swing.JLabel lblclothingallow;
    private javax.swing.JLabel lblphoneallow;
    private javax.swing.JLabel lblricesubsidy;
    private javax.swing.JTable tblERecords;
    private java.awt.TextField unifiedSearchBar;
    // End of variables declaration//GEN-END:variables

    


}

       
