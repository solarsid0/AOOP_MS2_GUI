package Utility;

/**
 * Enhanced utility class for secure password hashing and comprehensive validation
 * Uses PBKDF2 with SHA-256 for secure password storage with multiple validation levels
 * @author USER
 */
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * Enhanced utility class for secure password hashing and comprehensive validation
 * Uses PBKDF2 with SHA-256 for secure password storage with multiple validation levels
 */
public class PasswordHasher {
    
    // Security constants for password hashing
    private static final int SALT_LENGTH = 16;
    private static final int HASH_LENGTH = 32;
    private static final int ITERATIONS = 10000;
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    
    // Password validation constants
    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 128;
    private static final int RECOMMENDED_MIN_LENGTH = 12;
    
    // Common weak passwords (basic list - in production, use a larger dictionary)
    private static final String[] COMMON_WEAK_PASSWORDS = {
        "password", "123456", "123456789", "12345678", "12345", "1234567",
        "admin", "administrator", "root", "qwerty", "abc123", "password123",
        "welcome", "letmein", "monkey", "dragon", "master", "shadow",
        "superman", "michael", "password1", "123123", "111111", "000000"
    };
    
    // Regex patterns for password validation
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]");
    private static final Pattern SEQUENTIAL_PATTERN = Pattern.compile("(012|123|234|345|456|567|678|789|890|abc|bcd|cde|def|efg|fgh|ghi|hij|ijk|jkl|klm|lmn|mno|nop|opq|pqr|qrs|rst|stu|tuv|uvw|vwx|wxy|xyz)");
    private static final Pattern REPEATED_PATTERN = Pattern.compile("(.)\\1{2,}"); // 3 or more repeated characters
    
    /**
     * Password strength levels
     */
    public enum PasswordStrength {
        VERY_WEAK(0, "Very Weak", "Password is too weak and unsafe"),
        WEAK(1, "Weak", "Password is weak and should be improved"),
        FAIR(2, "Fair", "Password is acceptable but could be stronger"),
        GOOD(3, "Good", "Password is good and secure"),
        STRONG(4, "Strong", "Password is strong and very secure"),
        VERY_STRONG(5, "Very Strong", "Password is extremely strong and secure");
        
        private final int level;
        private final String description;
        private final String message;
        
        PasswordStrength(int level, String description, String message) {
            this.level = level;
            this.description = description;
            this.message = message;
        }
        
        public int getLevel() { return level; }
        public String getDescription() { return description; }
        public String getMessage() { return message; }
    }
    
    /**
     * Password validation result containing detailed feedback
     */
    public static class PasswordValidationResult {
        private final boolean isValid;
        private final PasswordStrength strength;
        private final List<String> issues;
        private final List<String> suggestions;
        private final int score;
        
        public PasswordValidationResult(boolean isValid, PasswordStrength strength, 
                                      List<String> issues, List<String> suggestions, int score) {
            this.isValid = isValid;
            this.strength = strength;
            this.issues = new ArrayList<>(issues);
            this.suggestions = new ArrayList<>(suggestions);
            this.score = score;
        }
        
        public boolean isValid() { return isValid; }
        public PasswordStrength getStrength() { return strength; }
        public List<String> getIssues() { return new ArrayList<>(issues); }
        public List<String> getSuggestions() { return new ArrayList<>(suggestions); }
        public int getScore() { return score; }
        
        public String getSummary() {
            StringBuilder sb = new StringBuilder();
            sb.append("Password Strength: ").append(strength.getDescription());
            sb.append(" (Score: ").append(score).append("/100)");
            if (!issues.isEmpty()) {
                sb.append("\nIssues: ").append(String.join(", ", issues));
            }
            if (!suggestions.isEmpty()) {
                sb.append("\nSuggestions: ").append(String.join(", ", suggestions));
            }
            return sb.toString();
        }
    }
    
    
    // CORE HASHING METHODS
    
    /**
     * Generates a random salt for password hashing
     * @return Base64 encoded salt string
     */
    public static String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }
    
    /**
     * Hashes a password with a given salt
     * @param password The plain text password
     * @param salt The salt to use for hashing
     * @return Base64 encoded hash string
     */
    public static String hashPassword(String password, String salt) {
        try {
            byte[] saltBytes = Base64.getDecoder().decode(salt);
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), saltBytes, ITERATIONS, HASH_LENGTH * 8);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            byte[] hash = factory.generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }
    
    /**
     * Generates a salt and hashes the password
     * @param password The plain text password
     * @return A string containing salt:hash format
     */
    public static String hashPassword(String password) {
        String salt = generateSalt();
        String hash = hashPassword(password, salt);
        return salt + ":" + hash;
    }
    
    /**
     * Verifies a password against a stored hash
     * @param password The plain text password to verify
     * @param storedHash The stored hash in salt:hash format
     * @return true if password matches, false otherwise
     */
    public static boolean verifyPassword(String password, String storedHash) {
        try {
            String[] parts = storedHash.split(":");
            if (parts.length != 2) {
                return false;
            }
            
            String salt = parts[0];
            String hash = parts[1];
            
            String newHash = hashPassword(password, salt);
            return hash.equals(newHash);
        } catch (Exception e) {
            return false;
        }
    }
    
    
    // BASIC VALIDATION METHODS (LEGACY COMPATIBILITY)
    
    /**
     * Checks if a password meets minimum security requirements
     * @param password The password to validate
     * @return true if password meets requirements, false otherwise
     */
    public static boolean isPasswordValid(String password) {
        if (password == null || password.length() < MIN_LENGTH) {
            return false;
        }
        
        boolean hasUpper = UPPERCASE_PATTERN.matcher(password).find();
        boolean hasLower = LOWERCASE_PATTERN.matcher(password).find();
        boolean hasDigit = DIGIT_PATTERN.matcher(password).find();
        boolean hasSpecial = SPECIAL_CHAR_PATTERN.matcher(password).find();
        
        return hasUpper && hasLower && hasDigit && hasSpecial;
    }
    
    /**
     * Gets password requirements message
     * @return String describing password requirements
     */
    public static String getPasswordRequirements() {
        return "Password must be at least " + MIN_LENGTH + " characters long and contain: " +
               "uppercase letter, lowercase letter, digit, and special character";
    }
    
    
    // ENHANCED VALIDATION METHODS
    
    /**
     * Validates password with detailed feedback and strength assessment
     * @param password The password to validate
     * @return PasswordValidationResult with detailed analysis
     */
    public static PasswordValidationResult validatePasswordStrength(String password) {
        List<String> issues = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();
        int score = 0;
        
        // Check for null or empty password
        if (password == null || password.isEmpty()) {
            issues.add("Password cannot be empty");
            return new PasswordValidationResult(false, PasswordStrength.VERY_WEAK, issues, suggestions, 0);
        }
        
        // Length checks
        if (password.length() < MIN_LENGTH) {
            issues.add("Password is too short (minimum " + MIN_LENGTH + " characters)");
        } else if (password.length() >= MIN_LENGTH && password.length() < RECOMMENDED_MIN_LENGTH) {
            suggestions.add("Consider using at least " + RECOMMENDED_MIN_LENGTH + " characters for better security");
            score += 10;
        } else if (password.length() >= RECOMMENDED_MIN_LENGTH) {
            score += 20;
        }
        
        if (password.length() > MAX_LENGTH) {
            issues.add("Password is too long (maximum " + MAX_LENGTH + " characters)");
        }
        
        // Character type checks
        boolean hasUpper = UPPERCASE_PATTERN.matcher(password).find();
        boolean hasLower = LOWERCASE_PATTERN.matcher(password).find();
        boolean hasDigit = DIGIT_PATTERN.matcher(password).find();
        boolean hasSpecial = SPECIAL_CHAR_PATTERN.matcher(password).find();
        
        if (!hasUpper) {
            issues.add("Password must contain at least one uppercase letter");
        } else {
            score += 15;
        }
        
        if (!hasLower) {
            issues.add("Password must contain at least one lowercase letter");
        } else {
            score += 15;
        }
        
        if (!hasDigit) {
            issues.add("Password must contain at least one digit");
        } else {
            score += 15;
        }
        
        if (!hasSpecial) {
            issues.add("Password must contain at least one special character");
        } else {
            score += 15;
        }
        
        // Additional strength checks
        
        // Check for common weak passwords
        if (isCommonWeakPassword(password)) {
            issues.add("Password is too common and easily guessable");
            score = Math.max(0, score - 30);
        }
        
        // Check for sequential characters
        if (SEQUENTIAL_PATTERN.matcher(password.toLowerCase()).find()) {
            issues.add("Password contains sequential characters");
            suggestions.add("Avoid using sequential characters like 'abc' or '123'");
            score = Math.max(0, score - 10);
        }
        
        // Check for repeated characters
        if (REPEATED_PATTERN.matcher(password).find()) {
            issues.add("Password contains repeated characters");
            suggestions.add("Avoid using repeated characters like 'aaa' or '111'");
            score = Math.max(0, score - 10);
        }
        
        // Check for keyboard patterns
        if (containsKeyboardPattern(password)) {
            issues.add("Password contains keyboard patterns");
            suggestions.add("Avoid using keyboard patterns like 'qwerty' or 'asdf'");
            score = Math.max(0, score - 15);
        }
        
        // Check for personal information patterns (basic check) - MADE LESS STRICT
        if (containsPersonalInfoPattern(password)) {
            // Changed from issue to suggestion - not blocking anymore
            suggestions.add("Consider avoiding obvious personal information like full names or birthdates");
            score = Math.max(0, score - 5); // Reduced penalty
        }
        
        // Bonus points for complexity
        if (password.length() >= 16) {
            score += 10; // Bonus for longer passwords
        }
        
        if (countCharacterTypes(password) >= 4) {
            score += 10; // Bonus for using all character types
        }
        
        // Calculate character diversity
        int uniqueChars = (int) password.chars().distinct().count();
        double diversityRatio = (double) uniqueChars / password.length();
        if (diversityRatio > 0.7) {
            score += 10; // Bonus for high character diversity
        }
        
        // Determine strength level
        PasswordStrength strength = determinePasswordStrength(score, issues.isEmpty());
        
        // Add general suggestions
        if (score < 70) {
            suggestions.add("Consider using a longer password with mixed characters");
        }
        if (score < 50) {
            suggestions.add("Use a combination of uppercase, lowercase, numbers, and symbols");
        }
        if (score < 30) {
            suggestions.add("Consider using a password manager to generate strong passwords");
        }
        
        boolean isValid = issues.isEmpty() && score >= 40;
        
        return new PasswordValidationResult(isValid, strength, issues, suggestions, score);
    }
    
    /**
     * Quick password strength check (simplified version)
     * @param password The password to check
     * @return Password strength level
     */
    public static PasswordStrength checkPasswordStrength(String password) {
        return validatePasswordStrength(password).getStrength();
    }
    
    /**
     * Check if password meets minimum requirements for the payroll system
     * @param password The password to check
     * @return true if password meets system requirements
     */
    public static boolean meetsSystemRequirements(String password) {
        PasswordValidationResult result = validatePasswordStrength(password);
        return result.isValid() && result.getStrength().getLevel() >= PasswordStrength.FAIR.getLevel();
    }
    
    /**
     * Get system password requirements for the MotorPH payroll system
     * @return Detailed requirements string
     */
    public static String getSystemPasswordRequirements() {
        return """
               Password Requirements for MotorPH Payroll System:
               \u2022 Minimum """ + MIN_LENGTH + " characters (recommended: " + RECOMMENDED_MIN_LENGTH + "+)\n" +
               "• At least one uppercase letter (A-Z)\n" +
               "• At least one lowercase letter (a-z)\n" +
               "• At least one digit (0-9)\n" +
               "• At least one special character (!@#$%^&*)\n" +
               "• Avoid common passwords, sequential characters, and keyboard patterns\n" +
               "• Minimum strength level: Fair or higher";
    }
    
    /**
     * Validate password for employee registration - MADE LESS STRICT
     * Includes additional checks for payroll system security
     * @param password The password to validate
     * @param firstName Employee's first name (to check against password)
     * @param lastName Employee's last name (to check against password) 
     * @param email Employee's email (to check against password)
     * @return PasswordValidationResult with detailed analysis
     */
    public static PasswordValidationResult validateEmployeePassword(String password, 
                                                                   String firstName, 
                                                                   String lastName, 
                                                                   String email) {
        PasswordValidationResult baseResult = validatePasswordStrength(password);
        
        if (!baseResult.isValid()) {
            return baseResult;
        }
        
        List<String> additionalIssues = new ArrayList<>(baseResult.getIssues());
        List<String> additionalSuggestions = new ArrayList<>(baseResult.getSuggestions());
        int adjustedScore = baseResult.getScore();
        
        // MADE PERSONAL INFORMATION CHECK LESS STRICT
        if (password != null && firstName != null && lastName != null && email != null) {
            String lowerPassword = password.toLowerCase();
            
            // Only check for exact full name matches, not partial matches
            String fullName = (firstName + lastName).toLowerCase();
            if (fullName.length() >= 6 && lowerPassword.equals(fullName)) {
                additionalIssues.add("Password should not be exactly your full name");
                adjustedScore = Math.max(0, adjustedScore - 20);
            }
            
            // Check email username only if it's the entire password
            if (email.contains("@")) {
                String emailUser = email.split("@")[0].toLowerCase();
                if (emailUser.length() >= 6 && lowerPassword.equals(emailUser)) {
                    additionalIssues.add("Password should not be exactly your email username");
                    adjustedScore = Math.max(0, adjustedScore - 15);
                }
            }
            
            // Only warn about obvious patterns, don't block
            if (firstName.length() >= 4 && lowerPassword.contains(firstName.toLowerCase()) &&
                lowerPassword.length() - firstName.length() <= 4) {
                additionalSuggestions.add("Consider using less obvious personal information");
                adjustedScore = Math.max(0, adjustedScore - 5);
            }
        }
        
        // Additional payroll system specific checks - MADE LESS STRICT
        if (password != null) {
            String lowerPassword = password.toLowerCase();
            
            // Only check for exact matches of company terms
            String[] payrollTerms = {"motorph", "payroll", "admin123"};
            for (String term : payrollTerms) {
                if (lowerPassword.equals(term) || lowerPassword.equals(term + "123")) {
                    additionalIssues.add("Password should not be common company terms");
                    adjustedScore = Math.max(0, adjustedScore - 15);
                    break;
                }
            }
        }
        
        // Determine final strength
        PasswordStrength finalStrength = determinePasswordStrength(adjustedScore, additionalIssues.isEmpty());
        boolean isValid = additionalIssues.isEmpty() && adjustedScore >= 40;
        
        return new PasswordValidationResult(isValid, finalStrength, additionalIssues, additionalSuggestions, adjustedScore);
    }
    
    /**
     * Generate a secure password suitable for payroll system employees
     * @return A secure password meeting all system requirements
     */
    public static String generateEmployeePassword() {
        return generateSecurePassword(14, true);
    }
    
    /**
     * Check if a password is suitable for admin/supervisor roles
     * Admin passwords should be stronger than regular employee passwords
     * @param password The password to check
     * @return true if password meets admin requirements
     */
    public static boolean meetsAdminRequirements(String password) {
        PasswordValidationResult result = validatePasswordStrength(password);
        return result.isValid() && 
               result.getStrength().getLevel() >= PasswordStrength.GOOD.getLevel() &&
               result.getScore() >= 70;
    }
    
    /**
     * Get admin password requirements
     * @return String describing admin password requirements
     */
    public static String getAdminPasswordRequirements() {
        return """
               Admin Password Requirements for MotorPH Payroll System:
               \u2022 Minimum """ + RECOMMENDED_MIN_LENGTH + " characters\n" +
               "• At least one uppercase letter (A-Z)\n" +
               "• At least one lowercase letter (a-z)\n" +
               "• At least one digit (0-9)\n" +
               "• At least one special character (!@#$%^&*)\n" +
               "• Avoid common passwords, sequential characters, and keyboard patterns\n" +
               "• Avoid personal information and company-related terms\n" +
               "• Minimum strength level: Good or higher\n" +
               "• Minimum score: 70/100";
    }
    
    
    // HELPER METHODS
    
    /**
     * Check if password is a common weak password
     */
    private static boolean isCommonWeakPassword(String password) {
        String lowerPassword = password.toLowerCase();
        for (String weak : COMMON_WEAK_PASSWORDS) {
            if (lowerPassword.equals(weak) || lowerPassword.contains(weak)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Check for keyboard patterns
     */
    private static boolean containsKeyboardPattern(String password) {
        String lower = password.toLowerCase();
        String[] patterns = {
            "qwerty", "asdf", "zxcv", "qwer", "asdf", "zxcv",
            "1234", "2345", "3456", "4567", "5678", "6789"
        };
        
        for (String pattern : patterns) {
            if (lower.contains(pattern)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Basic check for personal information patterns - MADE MUCH LESS STRICT
     */
    private static boolean containsPersonalInfoPattern(String password) {
        String lower = password.toLowerCase();
        
        // Only check for very obvious personal info patterns
        if (lower.matches(".*\\b(password|admin|login|user|guest)\\b.*")) {
            return true;
        }
        
        // Only flag very obvious birth year patterns (current year - 80 to current year - 10)
        if (lower.matches(".*(19[4-9][0-9]|20[0-1][0-9]).*") && 
            lower.length() <= 12) { // Only if password is short and mostly just a year
            return true;
        }
        
        return false;
    }
    
    /**
     * Count different character types in password
     */
    private static int countCharacterTypes(String password) {
        int types = 0;
        if (UPPERCASE_PATTERN.matcher(password).find()) types++;
        if (LOWERCASE_PATTERN.matcher(password).find()) types++;
        if (DIGIT_PATTERN.matcher(password).find()) types++;
        if (SPECIAL_CHAR_PATTERN.matcher(password).find()) types++;
        return types;
    }
    
    /**
     * Determine password strength based on score and issues
     */
    private static PasswordStrength determinePasswordStrength(int score, boolean hasNoIssues) {
        if (!hasNoIssues) {
            return PasswordStrength.VERY_WEAK;
        }
        
        if (score >= 90) return PasswordStrength.VERY_STRONG;
        if (score >= 75) return PasswordStrength.STRONG;
        if (score >= 60) return PasswordStrength.GOOD;
        if (score >= 40) return PasswordStrength.FAIR;
        if (score >= 20) return PasswordStrength.WEAK;
        return PasswordStrength.VERY_WEAK;
    }
    
    /**
     * Generate a secure random password
     * @param length The length of the password to generate
     * @param includeSymbols Whether to include special characters
     * @return A randomly generated secure password
     */
    public static String generateSecurePassword(int length, boolean includeSymbols) {
        if (length < MIN_LENGTH) {
            throw new IllegalArgumentException("Password length must be at least " + MIN_LENGTH);
        }
        
        String uppercase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lowercase = "abcdefghijklmnopqrstuvwxyz";
        String digits = "0123456789";
        String symbols = "!@#$%^&*()_+-=[]{}|;:,.<>?";
        
        String allChars = uppercase + lowercase + digits;
        if (includeSymbols) {
            allChars += symbols;
        }
        
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder();
        
        // Ensure at least one character from each required type
        password.append(uppercase.charAt(random.nextInt(uppercase.length())));
        password.append(lowercase.charAt(random.nextInt(lowercase.length())));
        password.append(digits.charAt(random.nextInt(digits.length())));
        if (includeSymbols) {
            password.append(symbols.charAt(random.nextInt(symbols.length())));
        }
        
        // Fill the rest randomly
        for (int i = password.length(); i < length; i++) {
            password.append(allChars.charAt(random.nextInt(allChars.length())));
        }
        
        // Shuffle the password to avoid predictable patterns
        char[] chars = password.toString().toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = chars[i];
            chars[i] = chars[j];
            chars[j] = temp;
        }
        
        return new String(chars);
    }
    
    /**
     * Generate a secure password with default settings
     * @return A 16-character secure password with symbols
     */
    public static String generateSecurePassword() {
        return generateSecurePassword(16, true);
    }
}