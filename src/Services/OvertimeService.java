package Services;

import DAOs.*;
import Models.*;
import Models.OvertimeRequestModel.ApprovalStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.sql.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Objects;

/**
 * Enhanced OvertimeService - Rank-and-file overtime business logic
 * Only rank-and-file employees are eligible for overtime pay (1.25x)
 * @author chad
 */
public class OvertimeService {

    // Manila timezone constant
    public static final ZoneId MANILA_TIMEZONE = ZoneId.of("Asia/Manila");
    
    // DAO Dependencies
    private final DatabaseConnection databaseConnection;
    private final OvertimeRequestDAO overtimeDAO;
    private final EmployeeDAO employeeDAO;
    private final AttendanceDAO attendanceDAO;

    // Business Rules - Enhanced for rank-and-file
    private static final BigDecimal RANK_AND_FILE_OVERTIME_MULTIPLIER = new BigDecimal("1.25");
    private static final BigDecimal NIGHT_SHIFT_MULTIPLIER = new BigDecimal("1.10");
    private static final BigDecimal WEEKEND_MULTIPLIER = new BigDecimal("1.30");
    private static final int MAX_DAILY_OVERTIME_HOURS = 4;
    private static final int MAX_WEEKLY_OVERTIME_HOURS = 20;
    private static final int MIN_OVERTIME_MINUTES = 30;

    /**
     * Constructor with database connection
     */
    public OvertimeService(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
        this.overtimeDAO = new OvertimeRequestDAO(databaseConnection);
        this.employeeDAO = new EmployeeDAO(databaseConnection);
        this.attendanceDAO = new AttendanceDAO(databaseConnection);
    }

    /**
     * Default constructor
     */
    public OvertimeService() {
        this.databaseConnection = new DatabaseConnection();
        this.overtimeDAO = new OvertimeRequestDAO(databaseConnection);
        this.employeeDAO = new EmployeeDAO(databaseConnection);
        this.attendanceDAO = new AttendanceDAO(databaseConnection);
    }

    // RANK-AND-FILE BUSINESS LOGIC

    /**
     * Check if employee is rank-and-file and eligible for overtime
     */
    public boolean isEligibleForOvertime(Integer employeeId) {
        try {
            return employeeDAO.isEmployeeRankAndFile(employeeId);
        } catch (Exception e) {
            System.err.println("Error checking overtime eligibility: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get overtime eligibility message
     */
    public String getOvertimeEligibilityMessage(Integer employeeId) {
        try {
            EmployeeModel employee = employeeDAO.findById(employeeId);
            if (employee == null) {
                return "❌ Employee not found";
            }

            boolean isRankAndFile = employeeDAO.isEmployeeRankAndFile(employeeId);
            
            if (isRankAndFile) {
                return "✅ " + employee.getFullName() + " (Rank-and-File) is eligible for overtime pay at 1.25x rate";
            } else {
                return "❌ " + employee.getFullName() + " (Non Rank-and-File) is not eligible for overtime pay";
            }
        } catch (Exception e) {
            return "Error checking eligibility: " + e.getMessage();
        }
    }

    // MANILA TIMEZONE SUPPORT

    /**
     * Get current Manila time
     */
    public LocalDateTime getCurrentManilaTime() {
        return ZonedDateTime.now(MANILA_TIMEZONE).toLocalDateTime();
    }

    /**
     * Get current Manila date
     */
    public LocalDate getCurrentManilaDate() {
        return ZonedDateTime.now(MANILA_TIMEZONE).toLocalDate();
    }

    /**
     * Validate overtime request date
     */
    public boolean isValidOvertimeDate(LocalDate overtimeDate) {
        LocalDate today = getCurrentManilaDate();
        return !overtimeDate.isBefore(today);
    }

    // OVERTIME REQUEST OPERATIONS

    /**
     * Submit overtime request with rank-and-file validation
     */
    public OvertimeRequestResult submitOvertimeRequest(Integer employeeId, LocalDateTime overtimeStart,
                                                       LocalDateTime overtimeEnd, String reason) {
        OvertimeRequestResult result = new OvertimeRequestResult();

        try {
            if (!isEligibleForOvertime(employeeId)) {
                result.setSuccess(false);
                result.setMessage(getOvertimeEligibilityMessage(employeeId));
                return result;
            }

            EmployeeModel employee = employeeDAO.findById(employeeId);
            if (employee == null) {
                result.setSuccess(false);
                result.setMessage("Employee not found: " + employeeId);
                return result;
            }

            OvertimeValidationResult validation = validateOvertimeRequest(employeeId, overtimeStart, overtimeEnd);
            if (!validation.isValid()) {
                result.setSuccess(false);
                result.setMessage(validation.getErrorMessage());
                return result;
            }

            OvertimeRequestModel overtimeRequest = new OvertimeRequestModel(employeeId, overtimeStart, overtimeEnd, reason);
            overtimeRequest.setDateCreated(getCurrentManilaTime());
            
            boolean success = overtimeDAO.save(overtimeRequest);

            if (success) {
                result.setSuccess(true);
                result.setOvertimeRequestId(overtimeRequest.getOvertimeRequestId());
                result.setMessage("Overtime request submitted successfully for " + employee.getFullName());
                result.setOvertimeHours(BigDecimal.valueOf(overtimeRequest.getOvertimeHours()));
                result.setEstimatedPay(calculateRankAndFileOvertimePay(overtimeRequest, employee.getHourlyRate()));

                System.out.println("✅ Rank-and-file overtime request submitted: " + employee.getFullName());
            } else {
                result.setSuccess(false);
                result.setMessage("Failed to submit overtime request for " + employee.getFullName());
            }

        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("Error submitting overtime request: " + e.getMessage());
        }

        return result;
    }

    /**
     * Approve overtime request
     */
    public OvertimeApprovalResult approveOvertimeRequest(Integer overtimeRequestId, Integer supervisorId, String supervisorNotes) {
        OvertimeApprovalResult result = new OvertimeApprovalResult();

        try {
            OvertimeRequestModel overtimeRequest = overtimeDAO.findById(overtimeRequestId);
            if (overtimeRequest == null) {
                result.setSuccess(false);
                result.setMessage("Overtime request not found: " + overtimeRequestId);
                return result;
            }

            if (!isEligibleForOvertime(overtimeRequest.getEmployeeId())) {
                result.setSuccess(false);
                result.setMessage("Employee is no longer eligible for overtime (not rank-and-file)");
                return result;
            }

            if (overtimeRequest.getApprovalStatus() != ApprovalStatus.PENDING) {
                result.setSuccess(false);
                result.setMessage("Overtime request has already been " + overtimeRequest.getApprovalStatus().getValue().toLowerCase());
                return result;
            }

            boolean success = overtimeDAO.approveOvertime(overtimeRequestId, supervisorNotes);

            if (success) {
                result.setSuccess(true);
                result.setMessage("Overtime request approved successfully");

                EmployeeModel employee = employeeDAO.findById(overtimeRequest.getEmployeeId());
                if (employee != null) {
                    BigDecimal overtimePay = calculateRankAndFileOvertimePay(overtimeRequest, employee.getHourlyRate());
                    result.setOvertimePay(overtimePay);
                }
            } else {
                result.setSuccess(false);
                result.setMessage("Failed to approve overtime request");
            }

        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("Error approving overtime request: " + e.getMessage());
        }

        return result;
    }

    /**
     * Reject overtime request
     */
    public OvertimeApprovalResult rejectOvertimeRequest(Integer overtimeRequestId, Integer supervisorId, String supervisorNotes) {
        OvertimeApprovalResult result = new OvertimeApprovalResult();

        try {
            if (supervisorNotes == null || supervisorNotes.trim().isEmpty()) {
                result.setSuccess(false);
                result.setMessage("Supervisor notes are required when rejecting an overtime request");
                return result;
            }

            OvertimeRequestModel overtimeRequest = overtimeDAO.findById(overtimeRequestId);
            if (overtimeRequest == null) {
                result.setSuccess(false);
                result.setMessage("Overtime request not found: " + overtimeRequestId);
                return result;
            }

            if (overtimeRequest.getApprovalStatus() != ApprovalStatus.PENDING) {
                result.setSuccess(false);
                result.setMessage("Overtime request has already been " + overtimeRequest.getApprovalStatus().getValue().toLowerCase());
                return result;
            }

            boolean success = overtimeDAO.rejectOvertime(overtimeRequestId, supervisorNotes);

            if (success) {
                result.setSuccess(true);
                result.setMessage("Overtime request rejected successfully");
            } else {
                result.setSuccess(false);
                result.setMessage("Failed to reject overtime request");
            }

        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("Error rejecting overtime request: " + e.getMessage());
        }

        return result;
    }

    // RANK-AND-FILE OVERTIME CALCULATIONS

    /**
     * Calculate rank-and-file overtime pay (1.25x rate)
     */
    public BigDecimal calculateRankAndFileOvertimePay(OvertimeRequestModel overtimeRequest, BigDecimal hourlyRate) {
        if (hourlyRate == null || BigDecimal.valueOf(overtimeRequest.getOvertimeHours()).equals(BigDecimal.ZERO)) {
            return BigDecimal.ZERO;
        }

        BigDecimal overtimeHours = BigDecimal.valueOf(overtimeRequest.getOvertimeHours());
        BigDecimal multiplier = RANK_AND_FILE_OVERTIME_MULTIPLIER;

        if (isNightShift(overtimeRequest.getOvertimeStart())) {
            multiplier = multiplier.add(NIGHT_SHIFT_MULTIPLIER.subtract(BigDecimal.ONE));
        }

        if (isWeekend(overtimeRequest.getOvertimeStart())) {
            multiplier = multiplier.add(WEEKEND_MULTIPLIER.subtract(BigDecimal.ONE));
        }

        return overtimeHours.multiply(hourlyRate).multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Legacy method for backward compatibility
     */
    public BigDecimal calculateOvertimePay(OvertimeRequestModel overtimeRequest, BigDecimal hourlyRate) {
        return calculateRankAndFileOvertimePay(overtimeRequest, hourlyRate);
    }

    /**
     * Calculate total overtime hours for rank-and-file employee
     */
    public BigDecimal calculateTotalOvertimeHours(Integer employeeId, LocalDate startDate, LocalDate endDate) {
        try {
            if (employeeId == null || !isEligibleForOvertime(employeeId)) {
                return BigDecimal.ZERO;
            }

            LocalDateTime startDateTime = startDate.atStartOfDay();
            LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

            List<OvertimeRequestModel> overtimeRequests = overtimeDAO.findByDateRange(startDateTime, endDateTime);

            return overtimeRequests.stream()
                    .filter(req -> Objects.equals(req.getEmployeeId(), employeeId))
                    .filter(OvertimeRequestModel::isApproved)
                    .map(req -> BigDecimal.valueOf(req.getOvertimeHours()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

        } catch (Exception e) {
            System.err.println("Error calculating total overtime hours: " + e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    /**
     * Calculate monthly overtime pay for rank-and-file employee
     */
    public BigDecimal calculateMonthlyOvertimePay(Integer employeeId, YearMonth yearMonth) {
        try {
            if (!isEligibleForOvertime(employeeId)) {
                return BigDecimal.ZERO;
            }

            EmployeeModel employee = employeeDAO.findById(employeeId);
            if (employee == null || employee.getHourlyRate() == null) {
                return BigDecimal.ZERO;
            }

            return overtimeDAO.getTotalOvertimePay(employeeId, yearMonth.getYear(),
                    yearMonth.getMonthValue(), RANK_AND_FILE_OVERTIME_MULTIPLIER);

        } catch (Exception e) {
            System.err.println("Error calculating monthly overtime pay: " + e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    // VALIDATION AND BUSINESS RULES

    /**
     * Validate overtime request
     */
    private OvertimeValidationResult validateOvertimeRequest(Integer employeeId, LocalDateTime overtimeStart, LocalDateTime overtimeEnd) {
        OvertimeValidationResult result = new OvertimeValidationResult();

        if (overtimeStart == null || overtimeEnd == null) {
            result.setValid(false);
            result.setErrorMessage("Start time and end time are required");
            return result;
        }

        if (overtimeEnd.isBefore(overtimeStart) || overtimeEnd.isEqual(overtimeStart)) {
            result.setValid(false);
            result.setErrorMessage("End time must be after start time");
            return result;
        }

        if (!isValidOvertimeDate(overtimeStart.toLocalDate())) {
            result.setValid(false);
            result.setErrorMessage("Overtime can only be requested for today or future dates (Manila time)");
            return result;
        }

        Duration duration = Duration.between(overtimeStart, overtimeEnd);
        if (duration.toMinutes() < MIN_OVERTIME_MINUTES) {
            result.setValid(false);
            result.setErrorMessage("Minimum overtime duration is " + MIN_OVERTIME_MINUTES + " minutes");
            return result;
        }

        BigDecimal requestedHours = new BigDecimal(duration.toMinutes()).divide(new BigDecimal(60), 2, RoundingMode.HALF_UP);
        if (requestedHours.compareTo(new BigDecimal(MAX_DAILY_OVERTIME_HOURS)) > 0) {
            result.setValid(false);
            result.setErrorMessage("Maximum daily overtime is " + MAX_DAILY_OVERTIME_HOURS + " hours");
            return result;
        }

        try {
            LocalDate overtimeDate = overtimeStart.toLocalDate();
            AttendanceModel attendance = attendanceDAO.getAttendanceByEmployeeAndDate(employeeId, Date.valueOf(overtimeDate));
            // FIXED: Check if attendance exists and has both time in and time out
            if (attendance == null || attendance.getTimeIn() == null || attendance.getTimeOut() == null) {
                result.setValid(false);
                result.setErrorMessage("Employee must have complete regular attendance before requesting overtime");
                return result;
            }
        } catch (Exception e) {
            System.out.println("Warning: Could not verify attendance for overtime request: " + e.getMessage());
        }

        BigDecimal weeklyOvertimeHours = calculateWeeklyOvertimeHours(employeeId, overtimeStart.toLocalDate());
        if (weeklyOvertimeHours.add(requestedHours).compareTo(new BigDecimal(MAX_WEEKLY_OVERTIME_HOURS)) > 0) {
            result.setValid(false);
            result.setErrorMessage("Weekly overtime limit of " + MAX_WEEKLY_OVERTIME_HOURS + " hours would be exceeded");
            return result;
        }

        List<OvertimeRequestModel> existingRequests = overtimeDAO.findByEmployee(employeeId);
        for (OvertimeRequestModel existing : existingRequests) {
            if (existing.getApprovalStatus() == ApprovalStatus.APPROVED || existing.getApprovalStatus() == ApprovalStatus.PENDING) {
                if (timesOverlap(overtimeStart, overtimeEnd, existing.getOvertimeStart(), existing.getOvertimeEnd())) {
                    result.setValid(false);
                    result.setErrorMessage("Overtime request overlaps with existing request");
                    return result;
                }
            }
        }

        result.setValid(true);
        return result;
    }

    /**
     * Check if two time ranges overlap
     */
    private boolean timesOverlap(LocalDateTime start1, LocalDateTime end1, LocalDateTime start2, LocalDateTime end2) {
        return !(end1.isBefore(start2) || start1.isAfter(end2));
    }

    /**
     * Calculate weekly overtime hours for an employee
     */
    private BigDecimal calculateWeeklyOvertimeHours(Integer employeeId, LocalDate date) {
        LocalDate startOfWeek = date.minusDays(date.getDayOfWeek().getValue() - 1);
        LocalDate endOfWeek = startOfWeek.plusDays(6);
        return calculateTotalOvertimeHours(employeeId, startOfWeek, endOfWeek);
    }

    /**
     * Check if overtime is during night shift (10 PM to 6 AM)
     */
    private boolean isNightShift(LocalDateTime overtimeStart) {
        int hour = overtimeStart.getHour();
        return hour >= 22 || hour < 6;
    }

    /**
     * Check if overtime is during weekend
     */
    private boolean isWeekend(LocalDateTime overtimeStart) {
        int dayOfWeek = overtimeStart.getDayOfWeek().getValue();
        return dayOfWeek == 6 || dayOfWeek == 7;
    }

    // REPORTING AND QUERIES

    /**
     * Get pending overtime requests for rank-and-file employees only
     */
    public List<OvertimeRequestModel> getPendingOvertimeRequests() {
        List<OvertimeRequestModel> allPending = overtimeDAO.findPendingOvertimeRequests();
        return allPending.stream()
                .filter(req -> isEligibleForOvertime(req.getEmployeeId()))
                .toList();
    }

    /**
     * Get overtime requests for specific rank-and-file employee
     */
    public List<OvertimeRequestModel> getEmployeeOvertimeRequests(Integer employeeId) {
        if (!isEligibleForOvertime(employeeId)) {
            return new ArrayList<>();
        }
        return overtimeDAO.findByEmployee(employeeId);
    }

    /**
     * Get overtime summary for rank-and-file employee
     */
    public OvertimeSummary getEmployeeOvertimeSummary(Integer employeeId, YearMonth yearMonth) {
        OvertimeSummary summary = new OvertimeSummary();
        summary.setEmployeeId(employeeId);
        summary.setYearMonth(yearMonth);

        try {
            EmployeeModel employee = employeeDAO.findById(employeeId);
            if (employee != null) {
                summary.setEmployeeName(employee.getFullName());
                summary.setHourlyRate(employee.getHourlyRate());
                summary.setIsRankAndFile(isEligibleForOvertime(employeeId));
            }

            if (!isEligibleForOvertime(employeeId)) {
                summary.setTotalOvertimeHours(BigDecimal.ZERO);
                summary.setTotalOvertimePay(BigDecimal.ZERO);
                return summary;
            }

            LocalDateTime startOfMonth = yearMonth.atDay(1).atStartOfDay();
            LocalDateTime endOfMonth = yearMonth.atEndOfMonth().atTime(23, 59, 59);

            List<OvertimeRequestModel> monthlyRequests = overtimeDAO.findByDateRange(startOfMonth, endOfMonth)
                    .stream()
                    .filter(req -> Objects.equals(req.getEmployeeId(), employeeId))
                    .toList();

            summary.setOvertimeRequests(monthlyRequests);

            BigDecimal totalHours = monthlyRequests.stream()
                    .filter(OvertimeRequestModel::isApproved)
                    .map(req -> BigDecimal.valueOf(req.getOvertimeHours()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            summary.setTotalOvertimeHours(totalHours);
            summary.setTotalOvertimePay(calculateMonthlyOvertimePay(employeeId, yearMonth));

            long approvedCount = monthlyRequests.stream().filter(OvertimeRequestModel::isApproved).count();
            long pendingCount = monthlyRequests.stream().filter(OvertimeRequestModel::isPending).count();
            long rejectedCount = monthlyRequests.stream().filter(OvertimeRequestModel::isRejected).count();

            summary.setApprovedCount((int)approvedCount);
            summary.setPendingCount((int)pendingCount);
            summary.setRejectedCount((int)rejectedCount);

        } catch (Exception e) {
            System.err.println("Error generating overtime summary: " + e.getMessage());
        }

        return summary;
    }

    /**
     * Get rank-and-file employees with most overtime hours
     */
    public List<OvertimeRanking> getTopOvertimeEmployees(LocalDate startDate, LocalDate endDate, int limit) {
        List<OvertimeRanking> rankings = new ArrayList<>();

        try {
            List<EmployeeModel> rankAndFileEmployees = employeeDAO.getRankAndFileEmployees();

            for (EmployeeModel employee : rankAndFileEmployees) {
                BigDecimal totalHours = calculateTotalOvertimeHours(employee.getEmployeeId(), startDate, endDate);

                if (totalHours.compareTo(BigDecimal.ZERO) > 0) {
                    OvertimeRanking ranking = new OvertimeRanking();
                    ranking.setEmployeeId(employee.getEmployeeId());
                    ranking.setEmployeeName(employee.getFullName());
                    ranking.setTotalOvertimeHours(totalHours);

                    if (employee.getHourlyRate() != null) {
                        BigDecimal totalPay = totalHours.multiply(employee.getHourlyRate()).multiply(RANK_AND_FILE_OVERTIME_MULTIPLIER);
                        ranking.setTotalOvertimePay(totalPay);
                    }

                    rankings.add(ranking);
                }
            }

            rankings.sort((a, b) -> b.getTotalOvertimeHours().compareTo(a.getTotalOvertimeHours()));

            if (limit > 0 && rankings.size() > limit) {
                rankings = rankings.subList(0, limit);
            }

        } catch (Exception e) {
            System.err.println("Error getting top overtime employees: " + e.getMessage());
        }

        return rankings;
    }

    // INNER CLASSES

    /**
     * Result of overtime request operation
     */
    public static class OvertimeRequestResult {
        private boolean success = false;
        private String message = "";
        private Integer overtimeRequestId;
        private BigDecimal overtimeHours = BigDecimal.ZERO;
        private BigDecimal estimatedPay = BigDecimal.ZERO;

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public Integer getOvertimeRequestId() { return overtimeRequestId; }
        public void setOvertimeRequestId(Integer overtimeRequestId) { this.overtimeRequestId = overtimeRequestId; }
        public BigDecimal getOvertimeHours() { return overtimeHours; }
        public void setOvertimeHours(BigDecimal overtimeHours) { this.overtimeHours = overtimeHours; }
        public BigDecimal getEstimatedPay() { return estimatedPay; }
        public void setEstimatedPay(BigDecimal estimatedPay) { this.estimatedPay = estimatedPay; }
    }

    /**
     * Result of overtime approval/rejection operation
     */
    public static class OvertimeApprovalResult {
        private boolean success = false;
        private String message = "";
        private BigDecimal overtimePay = BigDecimal.ZERO;

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public BigDecimal getOvertimePay() { return overtimePay; }
        public void setOvertimePay(BigDecimal overtimePay) { this.overtimePay = overtimePay; }
    }

    /**
     * Result of overtime request validation
     */
    public static class OvertimeValidationResult {
        private boolean valid = false;
        private String errorMessage = "";

        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }

    /**
     * Enhanced overtime summary for rank-and-file reporting
     */
    public static class OvertimeSummary {
        private Integer employeeId;
        private String employeeName;
        private YearMonth yearMonth;
        private BigDecimal hourlyRate = BigDecimal.ZERO;
        private boolean isRankAndFile = false;
        private List<OvertimeRequestModel> overtimeRequests = new ArrayList<>();
        private BigDecimal totalOvertimeHours = BigDecimal.ZERO;
        private BigDecimal totalOvertimePay = BigDecimal.ZERO;
        private int approvedCount = 0;
        private int pendingCount = 0;
        private int rejectedCount = 0;

        public Integer getEmployeeId() { return employeeId; }
        public void setEmployeeId(Integer employeeId) { this.employeeId = employeeId; }
        public String getEmployeeName() { return employeeName; }
        public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
        public YearMonth getYearMonth() { return yearMonth; }
        public void setYearMonth(YearMonth yearMonth) { this.yearMonth = yearMonth; }
        public BigDecimal getHourlyRate() { return hourlyRate; }
        public void setHourlyRate(BigDecimal hourlyRate) { this.hourlyRate = hourlyRate; }
        public boolean isRankAndFile() { return isRankAndFile; }
        public void setIsRankAndFile(boolean isRankAndFile) { this.isRankAndFile = isRankAndFile; }
        public List<OvertimeRequestModel> getOvertimeRequests() { return overtimeRequests; }
        public void setOvertimeRequests(List<OvertimeRequestModel> overtimeRequests) { this.overtimeRequests = overtimeRequests; }
        public BigDecimal getTotalOvertimeHours() { return totalOvertimeHours; }
        public void setTotalOvertimeHours(BigDecimal totalOvertimeHours) { this.totalOvertimeHours = totalOvertimeHours; }
        public BigDecimal getTotalOvertimePay() { return totalOvertimePay; }
        public void setTotalOvertimePay(BigDecimal totalOvertimePay) { this.totalOvertimePay = totalOvertimePay; }
        public int getApprovedCount() { return approvedCount; }
        public void setApprovedCount(int approvedCount) { this.approvedCount = approvedCount; }
        public int getPendingCount() { return pendingCount; }
        public void setPendingCount(int pendingCount) { this.pendingCount = pendingCount; }
        public int getRejectedCount() { return rejectedCount; }
        public void setRejectedCount(int rejectedCount) { this.rejectedCount = rejectedCount; }

        public int getTotalRequests() {
            return approvedCount + pendingCount + rejectedCount;
        }

        public BigDecimal getAverageHoursPerRequest() {
            if (getTotalRequests() == 0) return BigDecimal.ZERO;
            return totalOvertimeHours.divide(new BigDecimal(getTotalRequests()), 2, RoundingMode.HALF_UP);
        }

        public String getEmployeeCategory() {
            return isRankAndFile ? "Rank-and-File (Overtime Eligible)" : "Non Rank-and-File (Not Eligible)";
        }

        public String getOvertimeMultiplier() {
            return isRankAndFile ? "1.25x (Rank-and-File Rate)" : "N/A (Not Eligible)";
        }

        @Override
        public String toString() {
            return String.format("OvertimeSummary{employee=%s, category=%s, hours=%.2f, pay=₱%.2f, requests=%d}",
                    employeeName, getEmployeeCategory(), totalOvertimeHours, totalOvertimePay, getTotalRequests());
        }
    }

    /**
     * Enhanced overtime ranking for rank-and-file employees
     */
    public static class OvertimeRanking {
        private Integer employeeId;
        private String employeeName;
        private BigDecimal totalOvertimeHours = BigDecimal.ZERO;
        private BigDecimal totalOvertimePay = BigDecimal.ZERO;

        public Integer getEmployeeId() { return employeeId; }
        public void setEmployeeId(Integer employeeId) { this.employeeId = employeeId; }
        public String getEmployeeName() { return employeeName; }
        public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
        public BigDecimal getTotalOvertimeHours() { return totalOvertimeHours; }
        public void setTotalOvertimeHours(BigDecimal totalOvertimeHours) { this.totalOvertimeHours = totalOvertimeHours; }
        public BigDecimal getTotalOvertimePay() { return totalOvertimePay; }
        public void setTotalOvertimePay(BigDecimal totalOvertimePay) { this.totalOvertimePay = totalOvertimePay; }

        @Override
        public String toString() {
            return String.format("%s (Rank-and-File): %.2f hours, ₱%.2f at 1.25x rate", 
                    employeeName, totalOvertimeHours, totalOvertimePay);
        }
    }
}