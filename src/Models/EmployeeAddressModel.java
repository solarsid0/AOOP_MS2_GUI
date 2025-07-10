package Models;

import java.util.Objects;
import java.util.List;
import java.util.ArrayList;

/**
 * EmployeeAddressModel class that maps to the employeeaddress table
 * Fields: employeeId, addressId
 * Enhanced address relationship management
 * @author User
 */
public class EmployeeAddressModel {
    
    private Integer employeeId;
    private Integer addressId;
    
    // For convenience, we can include the actual objects (will be loaded by DAO if needed)
    private EmployeeModel employee;
    private AddressModel address;
    
    // Address type enumeration for different types of addresses
    public enum AddressType {
        CURRENT("Current Address"),
        PERMANENT("Permanent Address"),
        EMERGENCY("Emergency Contact Address"),
        BILLING("Billing Address"),
        MAILING("Mailing Address");
        
        private final String displayName;
        
        AddressType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public static AddressType fromString(String text) {
            for (AddressType type : AddressType.values()) {
                if (type.displayName.equalsIgnoreCase(text)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("No constant with text " + text + " found");
        }
    }
    
    // Constructors
    public EmployeeAddressModel() {}
    
    public EmployeeAddressModel(Integer employeeId, Integer addressId) {
        this.employeeId = employeeId;
        this.addressId = addressId;
    }
    
    public EmployeeAddressModel(EmployeeModel employee, AddressModel address) {
        this.employee = employee;
        this.address = address;
        if (employee != null) this.employeeId = employee.getEmployeeId();
        if (address != null) this.addressId = address.getAddressId();
    }
    
    // Getters and Setters
    public Integer getEmployeeId() { return employeeId; }
    public void setEmployeeId(Integer employeeId) { this.employeeId = employeeId; }
    
    public Integer getAddressId() { return addressId; }
    public void setAddressId(Integer addressId) { this.addressId = addressId; }
    
    public EmployeeModel getEmployee() { return employee; }
    public void setEmployee(EmployeeModel employee) { 
        this.employee = employee;
        if (employee != null) this.employeeId = employee.getEmployeeId();
    }
    
    public AddressModel getAddress() { return address; }
    public void setAddress(AddressModel address) { 
        this.address = address;
        if (address != null) this.addressId = address.getAddressId();
    }
    
    // Business Methods - Enhanced Address Relationship Management
    
    /**
     * Check if both employee and address IDs are valid
     * @return 
     */
    public boolean isValid() {
        return employeeId != null && employeeId > 0 && 
               addressId != null && addressId > 0;
    }
    
    /**
     * Validate employee address association
     * @return 
     */
    public boolean isValidAssociation() {
        if (!isValid()) {
            return false;
        }
        
        // Check if employee and address objects are consistent
        if (employee != null && !employee.getEmployeeId().equals(employeeId)) {
            return false;
        }
        
        if (address != null && !address.getAddressId().equals(addressId)) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Get employee name if employee object is loaded
     * @return 
     */
    public String getEmployeeName() {
        if (employee != null) {
            return employee.getFirstName() + " " + employee.getLastName();
        }
        return "Employee ID: " + employeeId;
    }
    
    /**
     * Get employee full name with employee ID
     * @return 
     */
    public String getEmployeeFullIdentifier() {
        if (employee != null) {
            return String.format("%s %s (ID: %d)", 
                employee.getFirstName(), 
                employee.getLastName(), 
                employee.getEmployeeId());
        }
        return "Employee ID: " + employeeId;
    }
    
    /**
     * Get formatted address if address object is loaded
     * @return 
     */
    public String getFormattedAddress() {
        if (address != null) {
            return address.getFullAddress();
        }
        return "Address ID: " + addressId;
    }
    
    /**
     * Get address with type information
     * @param addressType
     * @return 
     */
    public String getFormattedAddressWithType(AddressType addressType) {
        String formattedAddress = getFormattedAddress();
        if (addressType != null) {
            return String.format("%s - %s", addressType.getDisplayName(), formattedAddress);
        }
        return formattedAddress;
    }
    
    /**
     * Check if this association has complete information loaded
     * @return 
     */
    public boolean isFullyLoaded() {
        return employee != null && address != null;
    }
    
    /**
     * Create a display summary of this employee-address association
     * @return 
     */
    public String getDisplaySummary() {
        StringBuilder summary = new StringBuilder();
        
        if (employee != null) {
            summary.append(employee.getFirstName()).append(" ").append(employee.getLastName());
        } else {
            summary.append("Employee ID: ").append(employeeId);
        }
        
        summary.append(" - ");
        
        if (address != null) {
            summary.append(address.getFullAddress());
        } else {
            summary.append("Address ID: ").append(addressId);
        }
        
        return summary.toString();
    }
    
    /**
     * Get detailed summary for reporting
     * @return 
     */
    public String getDetailedSummary() {
        StringBuilder summary = new StringBuilder();
        
        // Employee information
        if (employee != null) {
            summary.append("Employee: ").append(getEmployeeFullIdentifier()).append("\n");
            summary.append("Position ID: ").append(employee.getPositionId() != null ? 
                employee.getPositionId() : "N/A").append("\n");
            summary.append("User Role: ").append(employee.getUserRole() != null ? 
                employee.getUserRole() : "N/A").append("\n");
        } else {
            summary.append("Employee ID: ").append(employeeId).append("\n");
        }
        
        // Address information
        if (address != null) {
            summary.append("Address: ").append(address.getFullAddress()).append("\n");
            summary.append("City: ").append(address.getCity()).append("\n");
            summary.append("Province: ").append(address.getProvince()).append("\n");
            summary.append("ZIP Code: ").append(address.getZipCode());
        } else {
            summary.append("Address ID: ").append(addressId);
        }
        
        return summary.toString();
    }
    
    /**
     * Check if address is within Metro Manila
     * @return 
     */
    public boolean isWithinMetroManila() {
        if (address == null) {
            return false;
        }
        
        String province = address.getProvince();
        String city = address.getCity();
        
        if (province == null || city == null) {
            return false;
        }
        
        // Check if within Metro Manila
        if (province.toLowerCase().contains("metro manila") || 
            province.toLowerCase().contains("ncr") ||
            province.toLowerCase().contains("national capital region")) {
            return true;
        }
        
        // Check specific Metro Manila cities
        String[] metroManilaCities = {
            "manila", "quezon city", "makati", "pasig", "taguig", "bgc", 
            "mandaluyong", "san juan", "marikina", "pasay", "paranaque", 
            "las pinas", "muntinlupa", "valenzuela", "malabon", "navotas", 
            "caloocan"
        };
        
        String lowerCity = city.toLowerCase();
        for (String mmCity : metroManilaCities) {
            if (lowerCity.contains(mmCity)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Check if address is complete and valid
     * @return 
     */
    public boolean isAddressComplete() {
        if (address == null) {
            return false;
        }
        
        return address.getStreet() != null && !address.getStreet().trim().isEmpty() &&
               address.getCity() != null && !address.getCity().trim().isEmpty() &&
               address.getProvince() != null && !address.getProvince().trim().isEmpty();
    }
    
    /**
     * Get address completeness score (0-100)
     * @return 
     */
    public int getAddressCompletenessScore() {
        if (address == null) {
            return 0;
        }
        
        int score = 0;
        int totalFields = 5;
        
        if (address.getStreet() != null && !address.getStreet().trim().isEmpty()) score++;
        if (address.getBarangay() != null && !address.getBarangay().trim().isEmpty()) score++;
        if (address.getCity() != null && !address.getCity().trim().isEmpty()) score++;
        if (address.getProvince() != null && !address.getProvince().trim().isEmpty()) score++;
        if (address.getZipCode() != null && !address.getZipCode().trim().isEmpty()) score++;
        
        return (score * 100) / totalFields;
    }
    
    /**
     * Get missing address fields
     * @return 
     */
    public List<String> getMissingAddressFields() {
        List<String> missing = new ArrayList<>();
        
        if (address == null) {
            missing.add("Address object is null");
            return missing;
        }
        
        if (address.getStreet() == null || address.getStreet().trim().isEmpty()) {
            missing.add("Street");
        }
        if (address.getBarangay() == null || address.getBarangay().trim().isEmpty()) {
            missing.add("Barangay");
        }
        if (address.getCity() == null || address.getCity().trim().isEmpty()) {
            missing.add("City");
        }
        if (address.getProvince() == null || address.getProvince().trim().isEmpty()) {
            missing.add("Province");
        }
        if (address.getZipCode() == null || address.getZipCode().trim().isEmpty()) {
            missing.add("ZIP Code");
        }
        
        return missing;
    }
    
    /**
     * Check if employee has multiple addresses
     * @param allEmployeeAddresses
     * @return 
     */
    public boolean hasMultipleAddresses(List<EmployeeAddressModel> allEmployeeAddresses) {
        if (allEmployeeAddresses == null || employeeId == null) {
            return false;
        }
        
        long count = allEmployeeAddresses.stream()
            .filter(ea -> ea.getEmployeeId() != null && ea.getEmployeeId().equals(employeeId))
            .count();
        
        return count > 1;
    }
    
    /**
     * Get geographic region of the address
     * @return 
     */
    public String getGeographicRegion() {
        if (address == null || address.getProvince() == null) {
            return "Unknown";
        }
        
        String province = address.getProvince().toLowerCase();
        
        // Philippine regions (simplified)
        if (province.contains("metro manila") || province.contains("ncr")) {
            return "National Capital Region (NCR)";
        } else if (province.contains("laguna") || province.contains("cavite") || 
                   province.contains("rizal") || province.contains("batangas") || 
                   province.contains("quezon")) {
            return "CALABARZON (Region IV-A)";
        } else if (province.contains("cebu") || province.contains("bohol") || 
                   province.contains("negros")) {
            return "Central Visayas (Region VII)";
        } else if (province.contains("davao") || province.contains("bukidnon")) {
            return "Northern Mindanao (Region X)";
        } else {
            return "Other Region";
        }
    }
    
    /**
     * Check if address requires shipping considerations
     * @return 
     */
    public boolean requiresSpecialShipping() {
        if (address == null) {
            return true; // Assume special handling needed if no address
        }
        
        // Addresses outside Metro Manila might need special shipping
        return !isWithinMetroManila();
    }
    
    /**
     * Get address validation status
     * @return 
     */
    public String getAddressValidationStatus() {
        if (address == null) {
            return "No address information";
        }
        
        int completeness = getAddressCompletenessScore();
        
        if (completeness >= 80) {
            return "Complete";
        } else if (completeness >= 60) {
            return "Mostly Complete";
        } else if (completeness >= 40) {
            return "Partially Complete";
        } else {
            return "Incomplete";
        }
    }
    
    @Override
    public String toString() {
        return String.format("EmployeeAddressModel{employeeId=%d, addressId=%d}", employeeId, addressId);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        EmployeeAddressModel that = (EmployeeAddressModel) obj;
        return Objects.equals(employeeId, that.employeeId) && 
               Objects.equals(addressId, that.addressId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(employeeId, addressId);
    }
}