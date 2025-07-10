package Models;

/**
 * AddressModel - Enhanced with Manila-specific address handling
 * Maps to address table with Philippines address validation
 * @author Chad
 */
public class AddressModel {
    
    // Database fields
    private Integer addressId;
    private String street;
    private String barangay;
    private String city;
    private String province;
    private String zipCode;
    
    // Philippines address constants
    private static final String DEFAULT_COUNTRY = "Philippines";
    private static final String[] METRO_MANILA_CITIES = {
        "Manila", "Quezon City", "Makati", "Pasig", "Taguig", "Muntinlupa", 
        "Parañaque", "Las Piñas", "Caloocan", "Malabon", "Navotas", 
        "Valenzuela", "Marikina", "Pasay", "Mandaluyong", "San Juan"
    };
    
    // Constructors
    public AddressModel() {}
    
    public AddressModel(String street, String barangay, String city, String province) {
        this.street = street;
        this.barangay = barangay;
        this.city = city;
        this.province = province;
    }
    
    public AddressModel(String street, String barangay, String city, String province, String zipCode) {
        this(street, barangay, city, province);
        this.zipCode = zipCode;
    }
    
    // ================================
    // MANILA-SPECIFIC METHODS
    // ================================
    
    /**
     * Check if address is in Metro Manila
     * @return true if in Metro Manila
     */
    public boolean isInMetroManila() {
        if (city == null) {
            return false;
        }
        
        for (String manilaCity : METRO_MANILA_CITIES) {
            if (city.toLowerCase().contains(manilaCity.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Check if address is in NCR (National Capital Region)
     * @return true if in NCR
     */
    public boolean isInNCR() {
        return isInMetroManila() || 
               (province != null && (province.toLowerCase().contains("ncr") || 
                                   province.toLowerCase().contains("metro manila")));
    }
    
    /**
     * Get region classification
     * @return region string
     */
    public String getRegion() {
        if (isInNCR()) {
            return "NCR (National Capital Region)";
        } else if (province != null) {
            return province;
        } else {
            return "Unknown Region";
        }
    }
    
    /**
     * Check if address is complete for Philippines
     * @return true if has required fields
     */
    public boolean isCompletePhilippinesAddress() {
        return street != null && !street.trim().isEmpty() &&
               barangay != null && !barangay.trim().isEmpty() &&
               city != null && !city.trim().isEmpty() &&
               province != null && !province.trim().isEmpty();
    }
    
    // ================================
    // VALIDATION METHODS
    // ================================
    
    /**
     * Validate Philippines ZIP code format
     * @return true if valid ZIP code
     */
    public boolean isValidZipCode() {
        if (zipCode == null || zipCode.trim().isEmpty()) {
            return false;
        }
        
        // Philippines ZIP codes are 4 digits
        return zipCode.trim().matches("\\d{4}");
    }
    
    /**
     * Validate address data
     * @return true if valid
     */
    public boolean isValid() {
        return isCompletePhilippinesAddress() && 
               (zipCode == null || isValidZipCode());
    }
    
    /**
     * Check if address is within payroll system service area
     * For now, assume Philippines-wide service
     * @return true if serviceable
     */
    public boolean isServiceable() {
        return isValid();
    }
    
    // ================================
    // FORMATTING METHODS
    // ================================
    
    /**
     * Get formatted address (Philippine style)
     * @return formatted address string
     */
    public String getFormattedAddress() {
        StringBuilder address = new StringBuilder();
        
        if (street != null && !street.trim().isEmpty()) {
            address.append(street.trim());
        }
        
        if (barangay != null && !barangay.trim().isEmpty()) {
            if (address.length() > 0) address.append(", ");
            address.append("Brgy. ").append(barangay.trim());
        }
        
        if (city != null && !city.trim().isEmpty()) {
            if (address.length() > 0) address.append(", ");
            address.append(city.trim());
        }
        
        if (province != null && !province.trim().isEmpty()) {
            if (address.length() > 0) address.append(", ");
            address.append(province.trim());
        }
        
        if (zipCode != null && !zipCode.trim().isEmpty()) {
            if (address.length() > 0) address.append(" ");
            address.append(zipCode.trim());
        }
        
        return address.toString();
    }
    
    /**
     * Get short address (City, Province)
     * @return short address string
     */
    public String getShortAddress() {
        StringBuilder address = new StringBuilder();
        
        if (city != null && !city.trim().isEmpty()) {
            address.append(city.trim());
        }
        
        if (province != null && !province.trim().isEmpty()) {
            if (address.length() > 0) address.append(", ");
            address.append(province.trim());
        }
        
        return address.toString();
    }
    
    /**
     * Get display address for UI
     * @return display-friendly address
     */
    public String getDisplayAddress() {
        String formatted = getFormattedAddress();
        if (formatted.isEmpty()) {
            return "No address provided";
        }
        return formatted;
    }
    
    // ================================
    // UTILITY METHODS
    // ================================
    
    /**
     * Normalize address fields (trim and proper case)
     */
    public void normalizeAddress() {
        if (street != null) {
            street = street.trim();
        }
        if (barangay != null) {
            barangay = barangay.trim();
        }
        if (city != null) {
            city = toProperCase(city.trim());
        }
        if (province != null) {
            province = toProperCase(province.trim());
        }
        if (zipCode != null) {
            zipCode = zipCode.trim().replaceAll("\\D", ""); // Remove non-digits
        }
    }
    
    /**
     * Convert string to proper case
     * @param text input text
     * @return properly cased text
     */
    private String toProperCase(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        String[] words = text.toLowerCase().split("\\s+");
        StringBuilder result = new StringBuilder();
        
        for (String word : words) {
            if (result.length() > 0) {
                result.append(" ");
            }
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                      .append(word.substring(1));
            }
        }
        
        return result.toString();
    }
    
    /**
     * Create a copy of this address
     * @return new AddressModel with same data
     */
    public AddressModel copy() {
        AddressModel copy = new AddressModel();
        copy.addressId = this.addressId;
        copy.street = this.street;
        copy.barangay = this.barangay;
        copy.city = this.city;
        copy.province = this.province;
        copy.zipCode = this.zipCode;
        return copy;
    }
    
    // ================================
    // GETTERS AND SETTERS
    // ================================
    
    public Integer getAddressId() { return addressId; }
    public void setAddressId(Integer addressId) { this.addressId = addressId; }
    
    public String getStreet() { return street; }
    public void setStreet(String street) { this.street = street; }
    
    public String getBarangay() { return barangay; }
    public void setBarangay(String barangay) { this.barangay = barangay; }
    
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    
    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }
    
    public String getZipCode() { return zipCode; }
    public void setZipCode(String zipCode) { this.zipCode = zipCode; }
    
    // ================================
    // OBJECT METHODS
    // ================================
    
    @Override
    public String toString() {
        return "AddressModel{" +
                "addressId=" + addressId +
                ", formattedAddress='" + getFormattedAddress() + '\'' +
                ", region='" + getRegion() + '\'' +
                '}';
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        AddressModel that = (AddressModel) obj;
        return addressId != null && addressId.equals(that.addressId);
    }
    
    @Override
    public int hashCode() {
        return addressId != null ? addressId.hashCode() : 0;
    }
    public String getFullAddress() {
    StringBuilder fullAddress = new StringBuilder();
    if (street != null) fullAddress.append(street).append(", ");
    if (barangay != null) fullAddress.append(barangay).append(", ");
    if (city != null) fullAddress.append(city).append(", ");
    if (province != null) fullAddress.append(province);
    if (zipCode != null) fullAddress.append(" ").append(zipCode);
    return fullAddress.toString();
}
}