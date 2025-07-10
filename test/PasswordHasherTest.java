import Utility.PasswordHasher;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import static org.junit.Assert.*;
import java.util.Base64;

/**
 * JUnit test class for PasswordHasher utility
 */
public class PasswordHasherTest {
    
    // Test data constants
    private static final String VALID_PASSWORD = "MySecure@Pass123";
    private static final String WEAK_PASSWORD = "password";
    private static final String EMPTY_PASSWORD = "";
    private static final String NULL_PASSWORD = null;
    
    @Before
    public void setUp() {
        System.out.println("Starting PasswordHasher test...");
    }
    
    @After
    public void tearDown() {
        System.out.println("Test completed.\n");
    }
    
    // =====================================
    // CORE FUNCTIONALITY TESTS
    // =====================================
    
    @Test
    public void testGenerateSalt_Success() {
        System.out.println("Testing salt generation - Basic functionality");
        
        String salt = PasswordHasher.generateSalt();
        
        assertNotNull("Salt should not be null", salt);
        assertFalse("Salt should not be empty", salt.isEmpty());
        
        // Verify it's valid Base64
        try {
            byte[] decoded = Base64.getDecoder().decode(salt);
            assertEquals("Decoded salt should be 16 bytes", 16, decoded.length);
        } catch (IllegalArgumentException e) {
            fail("Salt should be valid Base64: " + e.getMessage());
        }
    }
    
    @Test
    public void testGenerateSalt_Uniqueness() {
        System.out.println("Testing salt generation - Uniqueness property");
        
        String salt1 = PasswordHasher.generateSalt();
        String salt2 = PasswordHasher.generateSalt();
        String salt3 = PasswordHasher.generateSalt();
        
        assertFalse("Salts should be unique", salt1.equals(salt2));
        assertFalse("Salts should be unique", salt1.equals(salt3));
        assertFalse("Salts should be unique", salt2.equals(salt3));
    }
    
    @Test
    public void testHashPassword_Success() {
        System.out.println("Testing password hashing - Success case");
        
        String hashedPassword = PasswordHasher.hashPassword(VALID_PASSWORD);
        
        assertNotNull("Hashed password should not be null", hashedPassword);
        assertTrue("Hashed password should contain salt:hash format", 
                  hashedPassword.contains(":"));
        
        String[] parts = hashedPassword.split(":");
        assertEquals("Hashed password should have exactly 2 parts", 2, parts.length);
    }
    
    @Test
    public void testHashPassword_NullInput() {
        System.out.println("Testing password hashing - Null input (negative test)");
        
        try {
            PasswordHasher.hashPassword(NULL_PASSWORD);
            fail("Should throw exception for null password");
        } catch (Exception e) {
            assertNotNull("Exception should not be null", e);
            System.out.println("  Expected exception caught: " + e.getClass().getSimpleName());
        }
    }
    
    @Test
    public void testHashPasswordWithSalt_InvalidSalt() {
        System.out.println("Testing password hashing - Invalid salt (negative test)");
        
        try {
            PasswordHasher.hashPassword(VALID_PASSWORD, "invalid-base64!");
            fail("Should throw exception for invalid Base64 salt");
        } catch (Exception e) {
            assertTrue("Should throw appropriate exception", 
                      e instanceof RuntimeException || e instanceof IllegalArgumentException);
            System.out.println("  Expected exception caught: " + e.getClass().getSimpleName());
        }
    }
    
    // =====================================
    // PASSWORD VERIFICATION TESTS
    // =====================================
    
    @Test
    public void testVerifyPassword_Success() {
        System.out.println("Testing password verification - Correct password");
        
        String hashedPassword = PasswordHasher.hashPassword(VALID_PASSWORD);
        boolean isValid = PasswordHasher.verifyPassword(VALID_PASSWORD, hashedPassword);
        
        assertTrue("Valid password should verify successfully", isValid);
    }
    
    @Test
    public void testVerifyPassword_WrongPassword() {
        System.out.println("Testing password verification - Wrong password (negative test)");
        
        String hashedPassword = PasswordHasher.hashPassword(VALID_PASSWORD);
        boolean isValid = PasswordHasher.verifyPassword("WrongPassword123!", hashedPassword);
        
        assertFalse("Wrong password should not verify", isValid);
    }
    
    @Test
    public void testVerifyPassword_InvalidHashFormat() {
        System.out.println("Testing password verification - Invalid format (negative test)");
        
        assertFalse("Should reject hash without colon", 
                   PasswordHasher.verifyPassword(VALID_PASSWORD, "invalidhashwithoutcolon"));
        
        assertFalse("Should reject empty hash", 
                   PasswordHasher.verifyPassword(VALID_PASSWORD, ""));
        
        assertFalse("Should reject null hash", 
                   PasswordHasher.verifyPassword(VALID_PASSWORD, null));
    }
    
    @Test
    public void testVerifyPassword_CaseSensitivity() {
        System.out.println("Testing password verification - Case sensitivity");
        
        String password = "PassWord123!";
        String hashedPassword = PasswordHasher.hashPassword(password);
        
        assertFalse("Should be case sensitive", 
                   PasswordHasher.verifyPassword("password123!", hashedPassword));
        
        assertTrue("Should verify exact match", 
                  PasswordHasher.verifyPassword(password, hashedPassword));
    }
    
    // =====================================
    // PASSWORD VALIDATION TESTS
    // =====================================
    
    @Test
    public void testIsPasswordValid_ValidPassword() {
        System.out.println("Testing password validation - Valid password");
        
        assertTrue("Should accept valid password with all requirements", 
                  PasswordHasher.isPasswordValid("MySecure@Pass123"));
        
        assertTrue("Should accept minimum valid password", 
                  PasswordHasher.isPasswordValid("Abcdef1!"));
    }
    
    @Test
    public void testIsPasswordValid_InvalidLength() {
        System.out.println("Testing password validation - Length requirement (negative test)");
        
        assertFalse("Should reject password shorter than 8 characters", 
                   PasswordHasher.isPasswordValid("Abc1!"));
        
        assertTrue("Should accept exactly 8 characters", 
                  PasswordHasher.isPasswordValid("Abcdef1!"));
    }
    
    @Test
    public void testIsPasswordValid_MissingRequirements() {
        System.out.println("Testing password validation - Missing requirements (negative tests)");
        
        assertFalse("Should reject password without uppercase", 
                   PasswordHasher.isPasswordValid("abcdef1!"));
        
        assertFalse("Should reject password without lowercase", 
                   PasswordHasher.isPasswordValid("ABCDEF1!"));
        
        assertFalse("Should reject password without digit", 
                   PasswordHasher.isPasswordValid("Abcdefg!"));
        
        assertFalse("Should reject password without special character", 
                   PasswordHasher.isPasswordValid("Abcdef12"));
    }
    
    @Test
    public void testIsPasswordValid_EdgeCases() {
        System.out.println("Testing password validation - Edge cases (negative tests)");
        
        assertFalse("Should reject null password", 
                   PasswordHasher.isPasswordValid(null));
        
        assertFalse("Should reject empty password", 
                   PasswordHasher.isPasswordValid(""));
        
        assertFalse("Should reject common weak password", 
                   PasswordHasher.isPasswordValid("password"));
    }
    
    @Test
    public void testGetPasswordRequirements() {
        System.out.println("Testing password requirements message");
        
        String requirements = PasswordHasher.getPasswordRequirements();
        
        assertNotNull("Requirements message should not be null", requirements);
        assertTrue("Should mention minimum length", requirements.contains("8"));
        assertTrue("Should mention character requirements", 
                  requirements.toLowerCase().contains("uppercase") && 
                  requirements.toLowerCase().contains("lowercase"));
    }
    
    // =====================================
    // INTEGRATION TEST
    // =====================================
    
    @Test
    public void testCompletePasswordLifecycle() {
        System.out.println("Testing complete password lifecycle - Integration test");
        
        String originalPassword = "MyNewPass@2024";
        
        // Step 1: Validate password
        assertTrue("Password should meet requirements", 
                  PasswordHasher.isPasswordValid(originalPassword));
        
        // Step 2: Hash password
        String hashedPassword = PasswordHasher.hashPassword(originalPassword);
        assertNotNull("Hashed password should not be null", hashedPassword);
        
        // Step 3: Verify correct password
        assertTrue("Should verify correct password", 
                  PasswordHasher.verifyPassword(originalPassword, hashedPassword));
        
        // Step 4: Verify wrong password fails
        assertFalse("Should not verify wrong password", 
                   PasswordHasher.verifyPassword("WrongPass@2024", hashedPassword));
        
        // Step 5: Ensure password is not stored in plain text
        assertFalse("Hash should not contain original password", 
                   hashedPassword.contains(originalPassword));
        
        System.out.println("  Lifecycle test completed successfully");
    }
    
    @Test
    public void testPasswordChangeScenario() {
        System.out.println("Testing password change scenario - Real-world use case");
        
        String oldPassword = "OldPass@123";
        String newPassword = "NewPass@456";
        
        // User has existing password
        String oldHash = PasswordHasher.hashPassword(oldPassword);
        
        // User changes password
        String newHash = PasswordHasher.hashPassword(newPassword);
        
        // Verify old password no longer works
        assertFalse("Old password should not work with new hash", 
                   PasswordHasher.verifyPassword(oldPassword, newHash));
        
        // Verify new password works
        assertTrue("New password should work with new hash", 
                  PasswordHasher.verifyPassword(newPassword, newHash));
        
        // Verify hashes are different
        assertFalse("Old and new hashes should be different", oldHash.equals(newHash));
        
        System.out.println("  Password change scenario completed successfully");
    }
}