package DAOs;

import Models.AddressModel;
import java.sql.*;
import java.util.*;

/**
 * AddressDAO - Data Access Object for AddressModel
 * Enhanced with Manila-specific address operations and Philippine location support
 * FIXED for enhanced AddressModel compatibility
 * @author User
 */
public class AddressDAO {
    
    private final DatabaseConnection databaseConnection;
    
    public AddressDAO() {
        this.databaseConnection = new DatabaseConnection();
    }
    
    public AddressDAO(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }
    
    // BASIC CRUD OPERATIONS
    
    /**
     * Create - Insert new address into database
     * @param address
     * @return 
     */
    public boolean save(AddressModel address) {
        if (address == null || !address.isValid()) {
            System.err.println("Cannot save invalid address");
            return false;
        }
        
        // Normalize address before saving
        address.normalizeAddress();
        
        String sql = "INSERT INTO address (street, barangay, city, province, zipCode) VALUES (?, ?, ?, ?, ?)";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, address.getStreet());
            pstmt.setString(2, address.getBarangay());
            pstmt.setString(3, address.getCity());
            pstmt.setString(4, address.getProvince());
            pstmt.setString(5, address.getZipCode());
            
            int affectedRows = pstmt.executeUpdate();
            
            if (affectedRows > 0) {
                // Get the generated address ID
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        address.setAddressId(generatedKeys.getInt(1));
                    }
                }
                System.out.println("Address saved: " + address.getShortAddress());
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error saving address: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Read - Find address by ID
     * @param addressId
     * @return 
     */
    public AddressModel findById(int addressId) {
        String sql = "SELECT * FROM address WHERE addressId = ?";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, addressId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return mapResultSetToAddress(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error finding address: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Read - Get all addresses
     * @return 
     */
    public List<AddressModel> findAll() {
        List<AddressModel> addresses = new ArrayList<>();
        String sql = "SELECT * FROM address ORDER BY city, province";
        
        try (Connection conn = databaseConnection.createConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                addresses.add(mapResultSetToAddress(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving addresses: " + e.getMessage());
        }
        return addresses;
    }
    
    /**
     * Update - Update existing address
     * @param address
     * @return 
     */
    public boolean update(AddressModel address) {
        if (address == null || address.getAddressId() == null || address.getAddressId() <= 0) {
            System.err.println("Cannot update address: Invalid address ID");
            return false;
        }
        
        if (!address.isValid()) {
            System.err.println("Cannot update address: Invalid address data");
            return false;
        }
        
        // Normalize address before updating
        address.normalizeAddress();
        
        String sql = "UPDATE address SET street = ?, barangay = ?, city = ?, province = ?, zipCode = ? WHERE addressId = ?";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, address.getStreet());
            pstmt.setString(2, address.getBarangay());
            pstmt.setString(3, address.getCity());
            pstmt.setString(4, address.getProvince());
            pstmt.setString(5, address.getZipCode());
            pstmt.setInt(6, address.getAddressId());
            
            boolean success = pstmt.executeUpdate() > 0;
            if (success) {
                System.out.println("Address updated: " + address.getShortAddress());
            }
            return success;
        } catch (SQLException e) {
            System.err.println("Error updating address: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Delete - Remove address from database
     * @param address
     * @return 
     */
    public boolean delete(AddressModel address) {
        if (address == null || address.getAddressId() == null) {
            return false;
        }
        return deleteById(address.getAddressId());
    }
    
    /**
     * Delete - Remove address by ID
     * @param addressId
     * @return 
     */
    public boolean deleteById(int addressId) {
        String sql = "DELETE FROM address WHERE addressId = ?";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, addressId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting address: " + e.getMessage());
        }
        return false;
    }
    
    // MANILA-SPECIFIC ADDRESS OPERATIONS
    
    /**
     * Find all addresses in Manila area (Metro Manila)
     * @return List of Manila area addresses
     */
    public List<AddressModel> findManilaAreaAddresses() {
        List<AddressModel> addresses = new ArrayList<>();
        String sql = """
            SELECT * FROM address 
            WHERE LOWER(province) LIKE '%metro manila%' 
               OR LOWER(province) LIKE '%ncr%' 
               OR LOWER(province) LIKE '%national capital region%'
               OR LOWER(city) IN ('manila', 'quezon city', 'makati', 'taguig', 'pasig', 
                                  'mandaluyong', 'san juan', 'muntinlupa', 'paranaque', 
                                  'las pinas', 'caloocan', 'malabon', 'navotas', 
                                  'valenzuela', 'marikina', 'pasay', 'pateros')
            ORDER BY city, street
            """;
        
        try (Connection conn = databaseConnection.createConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                addresses.add(mapResultSetToAddress(rs));
            }
            System.out.println("✅ Found " + addresses.size() + " Manila area addresses");
        } catch (SQLException e) {
            System.err.println("Error finding Manila area addresses: " + e.getMessage());
        }
        return addresses;
    }
    
    /**
     * Find all addresses in Naaldwijk area (user's location from preferences)
     * @return List of Naaldwijk addresses
     */
    public List<AddressModel> findNaaldwijkAddresses() {
        List<AddressModel> addresses = new ArrayList<>();
        String sql = "SELECT * FROM address WHERE LOWER(city) LIKE '%naaldwijk%' ORDER BY street";
        
        try (Connection conn = databaseConnection.createConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                addresses.add(mapResultSetToAddress(rs));
            }
            System.out.println("✅ Found " + addresses.size() + " Naaldwijk addresses");
        } catch (SQLException e) {
            System.err.println("Error finding Naaldwijk addresses: " + e.getMessage());
        }
        return addresses;
    }
    
    /**
     * Find addresses outside Manila area
     * @return List of non-Manila addresses
     */
    public List<AddressModel> findNonManilaAddresses() {
        List<AddressModel> addresses = new ArrayList<>();
        String sql = """
            SELECT * FROM address 
            WHERE NOT (LOWER(province) LIKE '%metro manila%' 
                      OR LOWER(province) LIKE '%ncr%' 
                      OR LOWER(province) LIKE '%national capital region%'
                      OR LOWER(city) IN ('manila', 'quezon city', 'makati', 'taguig', 'pasig', 
                                         'mandaluyong', 'san juan', 'muntinlupa', 'paranaque', 
                                         'las pinas', 'caloocan', 'malabon', 'navotas', 
                                         'valenzuela', 'marikina', 'pasay', 'pateros'))
            ORDER BY province, city, street
            """;
        
        try (Connection conn = databaseConnection.createConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                addresses.add(mapResultSetToAddress(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error finding non-Manila addresses: " + e.getMessage());
        }
        return addresses;
    }
    
    /**
     * Find addresses with valid Manila ZIP codes (1000-1799)
     * @return List of addresses with Manila ZIP codes
     */
    public List<AddressModel> findAddressesWithManilaZipCodes() {
        List<AddressModel> addresses = new ArrayList<>();
        String sql = "SELECT * FROM address WHERE zipCode REGEXP '^1[0-7][0-9]{2}$' ORDER BY zipCode, city";
        
        try (Connection conn = databaseConnection.createConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                addresses.add(mapResultSetToAddress(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error finding addresses with Manila ZIP codes: " + e.getMessage());
        }
        return addresses;
    }
    
    /**
     * Get address statistics by location category
     * @return Map with address counts by location
     */
    public Map<String, Integer> getAddressLocationStatistics() {
        Map<String, Integer> stats = new HashMap<>();
        
        // Count Manila area addresses
        String manilaSQL = """
            SELECT COUNT(*) FROM address 
            WHERE LOWER(province) LIKE '%metro manila%' 
               OR LOWER(province) LIKE '%ncr%' 
               OR LOWER(province) LIKE '%national capital region%'
               OR LOWER(city) IN ('manila', 'quezon city', 'makati', 'taguig', 'pasig', 
                                  'mandaluyong', 'san juan', 'muntinlupa', 'paranaque', 
                                  'las pinas', 'caloocan', 'malabon', 'navotas', 
                                  'valenzuela', 'marikina', 'pasay', 'pateros')
            """;
        
        // Count total addresses
        String totalSQL = "SELECT COUNT(*) FROM address";
        
        try (Connection conn = databaseConnection.createConnection()) {
            // Manila area count
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(manilaSQL)) {
                if (rs.next()) {
                    stats.put("manilaArea", rs.getInt(1));
                }
            }
            
            // Total count
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(totalSQL)) {
                if (rs.next()) {
                    int total = rs.getInt(1);
                    stats.put("total", total);
                    stats.put("nonManilaArea", total - stats.getOrDefault("manilaArea", 0));
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting address location statistics: " + e.getMessage());
        }
        
        return stats;
    }
    
    /**
     * Auto-correct Manila area addresses (fix province field)
     * @return Number of addresses corrected
     */
    public int autoCorrectManilaProvinces() {
        String sql = """
            UPDATE address 
            SET province = 'Metro Manila' 
            WHERE LOWER(city) IN ('manila', 'quezon city', 'makati', 'taguig', 'pasig', 
                                  'mandaluyong', 'san juan', 'muntinlupa', 'paranaque', 
                                  'las pinas', 'caloocan', 'malabon', 'navotas', 
                                  'valenzuela', 'marikina', 'pasay', 'pateros')
            AND NOT (LOWER(province) LIKE '%metro manila%' 
                    OR LOWER(province) LIKE '%ncr%' 
                    OR LOWER(province) LIKE '%national capital region%')
            """;
        
        try (Connection conn = databaseConnection.createConnection();
             Statement stmt = conn.createStatement()) {
            
            int corrected = stmt.executeUpdate(sql);
            if (corrected > 0) {
                System.out.println("✅ Auto-corrected " + corrected + " Manila province entries");
            }
            return corrected;
            
        } catch (SQLException e) {
            System.err.println("Error auto-correcting Manila provinces: " + e.getMessage());
            return 0;
        }
    }
    
    // ENHANCED SEARCH AND FILTER METHODS
    
    /**
     * Find addresses by city
     * @param city
     * @return 
     */
    public List<AddressModel> findByCity(String city) {
        List<AddressModel> addresses = new ArrayList<>();
        if (city == null || city.trim().isEmpty()) {
            return addresses;
        }
        
        String sql = "SELECT * FROM address WHERE LOWER(city) = LOWER(?) ORDER BY street";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, city.trim());
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                addresses.add(mapResultSetToAddress(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error finding addresses by city: " + e.getMessage());
        }
        return addresses;
    }
    
    /**
     * Find addresses by province
     * @param province
     * @return 
     */
    public List<AddressModel> findByProvince(String province) {
        List<AddressModel> addresses = new ArrayList<>();
        if (province == null || province.trim().isEmpty()) {
            return addresses;
        }
        
        String sql = "SELECT * FROM address WHERE LOWER(province) = LOWER(?) ORDER BY city, street";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, province.trim());
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                addresses.add(mapResultSetToAddress(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error finding addresses by province: " + e.getMessage());
        }
        return addresses;
    }
    
    /**
     * Find addresses by ZIP code range
     * @param startZip Starting ZIP code
     * @param endZip Ending ZIP code
     * @return List of addresses in ZIP code range
     */
    public List<AddressModel> findByZipCodeRange(String startZip, String endZip) {
        List<AddressModel> addresses = new ArrayList<>();
        if (startZip == null || endZip == null) {
            return addresses;
        }
        
        String sql = "SELECT * FROM address WHERE zipCode BETWEEN ? AND ? ORDER BY zipCode, city";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, startZip);
            pstmt.setString(2, endZip);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                addresses.add(mapResultSetToAddress(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error finding addresses by ZIP code range: " + e.getMessage());
        }
        return addresses;
    }
    
    /**
     * Search addresses by partial text with location priority
     * Manila area addresses appear first
     * @param searchText
     * @return 
     */
    public List<AddressModel> searchAddressesWithLocationPriority(String searchText) {
        List<AddressModel> addresses = new ArrayList<>();
        if (searchText == null || searchText.trim().isEmpty()) {
            return addresses;
        }
        
        String sql = """
            SELECT *, 
                   CASE WHEN (LOWER(province) LIKE '%metro manila%' 
                             OR LOWER(province) LIKE '%ncr%' 
                             OR LOWER(province) LIKE '%national capital region%'
                             OR LOWER(city) IN ('manila', 'quezon city', 'makati', 'taguig', 'pasig', 
                                                'mandaluyong', 'san juan', 'muntinlupa', 'paranaque', 
                                                'las pinas', 'caloocan', 'malabon', 'navotas', 
                                                'valenzuela', 'marikina', 'pasay', 'pateros'))
                        THEN 1 ELSE 2 END as location_priority
            FROM address 
            WHERE street LIKE ? OR barangay LIKE ? OR city LIKE ? OR province LIKE ? OR zipCode LIKE ?
            ORDER BY location_priority, city, street
            """;
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            String searchPattern = "%" + searchText.trim() + "%";
            for (int i = 1; i <= 5; i++) {
                pstmt.setString(i, searchPattern);
            }
            
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                addresses.add(mapResultSetToAddress(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error searching addresses with location priority: " + e.getMessage());
        }
        return addresses;
    }
    
    /**
     * Standard search addresses method
     * @param searchText
     * @return 
     */
    public List<AddressModel> searchAddresses(String searchText) {
        List<AddressModel> addresses = new ArrayList<>();
        if (searchText == null || searchText.trim().isEmpty()) {
            return addresses;
        }
        
        String sql = """
            SELECT * FROM address WHERE 
            street LIKE ? OR barangay LIKE ? OR city LIKE ? OR province LIKE ? OR zipCode LIKE ? 
            ORDER BY city, street
            """;
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            String searchPattern = "%" + searchText.trim() + "%";
            for (int i = 1; i <= 5; i++) {
                pstmt.setString(i, searchPattern);
            }
            
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                addresses.add(mapResultSetToAddress(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error searching addresses: " + e.getMessage());
        }
        return addresses;
    }
    
    // UTILITY METHODS
    
    /**
     * Check if address exists
     * @param addressId
     * @return 
     */
    public boolean exists(int addressId) {
        String sql = "SELECT COUNT(*) FROM address WHERE addressId = ?";
        
        try (Connection conn = databaseConnection.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, addressId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error checking address existence: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Get count of all addresses
     * @return 
     */
    public int getAddressCount() {
        String sql = "SELECT COUNT(*) FROM address";
        
        try (Connection conn = databaseConnection.createConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error getting address count: " + e.getMessage());
        }
        return 0;
    }
    
    /**
     * Validate and clean address data
     * @param address Address to validate and clean
     * @return true if address was modified during cleaning
     */
    public boolean validateAndCleanAddress(AddressModel address) {
        if (address == null) {
            return false;
        }
        
        // Normalize the address (this will modify it)
        address.normalizeAddress();
        
        // Return true to indicate the address might have been modified
        return true;
    }
    
    /**
     * FIXED: Helper method to map ResultSet to AddressModel
     * Uses the enhanced AddressModel with proper setters
     */
    private AddressModel mapResultSetToAddress(ResultSet rs) throws SQLException {
        AddressModel address = new AddressModel();
        
        // Set all fields using setters
        address.setAddressId(rs.getInt("addressId"));
        address.setStreet(rs.getString("street"));
        address.setBarangay(rs.getString("barangay"));
        address.setCity(rs.getString("city"));
        address.setProvince(rs.getString("province"));
        address.setZipCode(rs.getString("zipCode"));
        
        return address;
    }
    
    /**
     * Get unique cities
     * @return 
     */
    public List<String> getUniqueCities() {
        List<String> cities = new ArrayList<>();
        String sql = "SELECT DISTINCT city FROM address WHERE city IS NOT NULL ORDER BY city";
        
        try (Connection conn = databaseConnection.createConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                cities.add(rs.getString("city"));
            }
        } catch (SQLException e) {
            System.err.println("Error getting unique cities: " + e.getMessage());
        }
        return cities;
    }
    
    /**
     * Get unique provinces
     * @return 
     */
    public List<String> getUniqueProvinces() {
        List<String> provinces = new ArrayList<>();
        String sql = "SELECT DISTINCT province FROM address WHERE province IS NOT NULL ORDER BY province";
        
        try (Connection conn = databaseConnection.createConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                provinces.add(rs.getString("province"));
            }
        } catch (SQLException e) {
            System.err.println("Error getting unique provinces: " + e.getMessage());
        }
        return provinces;
    }
}