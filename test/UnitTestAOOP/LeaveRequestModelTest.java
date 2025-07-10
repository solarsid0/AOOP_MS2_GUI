package UnitTestAOOP;

import org.junit.*;
import static org.junit.Assert.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.logging.ConsoleHandler;
import java.util.logging.SimpleFormatter;

import java.time.*;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;
import java.util.ArrayList;
import Models.LeaveRequestModel;

public class LeaveRequestModelTest {
    
    private static final Logger logger = Logger.getLogger(LeaveRequestModelTest.class.getName());
    private static final ZoneId MANILA_TIMEZONE = ZoneId.of("Asia/Manila");
    
    private LeaveRequestModel leaveRequest;
    private Date validStartDate;
    private Date validEndDate;
    private Date pastDate;
    private Date futureDate;
    private Date todayDate;
    
    @BeforeClass
    public static void setUpClass() {
        // Configure logging
        logger.setLevel(Level.INFO);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.INFO);
        handler.setFormatter(new SimpleFormatter());
        logger.addHandler(handler);
        logger.setUseParentHandlers(false);
        
        logger.info("=== Starting LeaveRequestModel Test Suite ===");
        logger.info("Test suite initialized with comprehensive positive and negative testing");
    }
    
    @AfterClass
    public static void tearDownClass() {
        logger.info("=== Completed LeaveRequestModel Test Suite ===");
        logger.info("All tests executed successfully");
    }
    
    @Before
    public void setUp() {
        logger.info("Setting up test data for individual test");
        leaveRequest = new LeaveRequestModel();
        
        // Setup test dates
        LocalDate today = LocalDate.now(MANILA_TIMEZONE);
        todayDate = Date.valueOf(today);
        validStartDate = Date.valueOf(today.plusDays(1));
        validEndDate = Date.valueOf(today.plusDays(5));
        pastDate = Date.valueOf(today.minusDays(5));
        futureDate = Date.valueOf(today.plusDays(10));
        
        logger.fine("Test dates initialized: start=" + validStartDate + ", end=" + validEndDate);
    }
    
    @After
    public void tearDown() {
        logger.fine("Cleaning up test data");
        leaveRequest = null;
    }
    
    // Constructor Tests
    @Test
    public void testDefaultConstructor() {
        logger.info("Testing default constructor");
        
        LeaveRequestModel request = new LeaveRequestModel();
        
        assertEquals(LeaveRequestModel.ApprovalStatus.PENDING, request.getApprovalStatus());
        assertNotNull(request.getDateCreated());
        assertFalse(request.isHasAttendanceConflict());
        assertEquals(0, request.getWorkingDaysCount());
        
        logger.info("Default constructor test passed");
    }
    
    @Test
    public void testParameterizedConstructor() {
        logger.info("Testing parameterized constructor with valid data");
        
        LeaveRequestModel request = new LeaveRequestModel(101, 1, validStartDate, validEndDate, "Vacation");
        
        assertEquals(101, request.getEmployeeId());
        assertEquals(1, request.getLeaveTypeId());
        assertEquals(validStartDate, request.getLeaveStart());
        assertEquals(validEndDate, request.getLeaveEnd());
        assertEquals("Vacation", request.getLeaveReason());
        assertEquals(LeaveRequestModel.ApprovalStatus.PENDING, request.getApprovalStatus());
        assertTrue(request.getWorkingDaysCount() > 0);
        
        logger.info("Parameterized constructor test passed - working days: " + request.getWorkingDaysCount());
    }
    
    @Test
    public void testParameterizedConstructorWithInvalidValues() {
        logger.info("Testing parameterized constructor with invalid values");
        
        LeaveRequestModel request = new LeaveRequestModel(-1, -1, null, null, "");
        
        assertEquals(-1, request.getEmployeeId());
        assertEquals(-1, request.getLeaveTypeId());
        assertNull(request.getLeaveStart());
        assertNull(request.getLeaveEnd());
        assertEquals("", request.getLeaveReason());
        assertEquals(0, request.getWorkingDaysCount());
        
        logger.info("Parameterized constructor negative test passed");
    }
    
    @Test
    public void testParameterizedConstructorWithZeroValues() {
        logger.info("Testing parameterized constructor with zero values");
        
        LeaveRequestModel request = new LeaveRequestModel(0, 0, validStartDate, validEndDate, null);
        
        assertEquals(0, request.getEmployeeId());
        assertEquals(0, request.getLeaveTypeId());
        assertNull(request.getLeaveReason());
        assertFalse(request.isValidLeaveRequest());
        
        logger.info("Zero values constructor test passed");
    }
    
    // ApprovalStatus Enum Tests
    @Test
    public void testApprovalStatusFromStringValid() {
        logger.info("Testing ApprovalStatus.fromString with valid values");
        
        assertEquals(LeaveRequestModel.ApprovalStatus.PENDING, 
                    LeaveRequestModel.ApprovalStatus.fromString("Pending"));
        assertEquals(LeaveRequestModel.ApprovalStatus.APPROVED, 
                    LeaveRequestModel.ApprovalStatus.fromString("Approved"));
        assertEquals(LeaveRequestModel.ApprovalStatus.REJECTED, 
                    LeaveRequestModel.ApprovalStatus.fromString("Rejected"));
        
        // Test case insensitive
        assertEquals(LeaveRequestModel.ApprovalStatus.PENDING, 
                    LeaveRequestModel.ApprovalStatus.fromString("pending"));
        assertEquals(LeaveRequestModel.ApprovalStatus.APPROVED, 
                    LeaveRequestModel.ApprovalStatus.fromString("APPROVED"));
        
        logger.info("ApprovalStatus.fromString positive tests passed");
    }
    
    @Test
    public void testApprovalStatusFromStringInvalid() {
        logger.info("Testing ApprovalStatus.fromString with invalid values");
        
        // Test invalid string
        assertEquals(LeaveRequestModel.ApprovalStatus.PENDING, 
                    LeaveRequestModel.ApprovalStatus.fromString("Invalid"));
        
        // Test empty string
        assertEquals(LeaveRequestModel.ApprovalStatus.PENDING, 
                    LeaveRequestModel.ApprovalStatus.fromString(""));
        
        // Test null
        assertEquals(LeaveRequestModel.ApprovalStatus.PENDING, 
                    LeaveRequestModel.ApprovalStatus.fromString(null));
        
        // Test whitespace
        assertEquals(LeaveRequestModel.ApprovalStatus.PENDING, 
                    LeaveRequestModel.ApprovalStatus.fromString("   "));
        
        logger.info("ApprovalStatus.fromString negative tests passed");
    }
    
    // Manila Timezone Tests
    @Test
    public void testManilaTimezoneOperations() {
        logger.info("Testing Manila timezone operations");
        
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        leaveRequest.setDateCreated(now);
        leaveRequest.setDateApproved(now);
        
        assertNotNull(leaveRequest.getCreatedDateInManila());
        assertNotNull(leaveRequest.getApprovedDateInManila());
        assertNotNull(LeaveRequestModel.nowInManila());
        
        logger.info("Manila timezone operations test passed");
    }
    
    @Test
    public void testManilaTimezoneOperationsWithNull() {
        logger.info("Testing Manila timezone operations with null values");
        
        leaveRequest.setDateCreated(null);
        leaveRequest.setDateApproved(null);
        
        assertNull(leaveRequest.getCreatedDateInManila());
        assertNull(leaveRequest.getApprovedDateInManila());
        
        logger.info("Manila timezone null handling test passed");
    }
    
    // Conflict Checking Tests
    @Test
    public void testHasConflictWithAttendancePositive() {
        logger.info("Testing attendance conflict detection");
        
        leaveRequest.setLeaveStart(validStartDate);
        leaveRequest.setLeaveEnd(validEndDate);
        
        // Test conflict on start date
        assertTrue(leaveRequest.hasConflictWithAttendance(validStartDate));
        
        // Test conflict on end date
        assertTrue(leaveRequest.hasConflictWithAttendance(validEndDate));
        
        // Test conflict in between
        Date middleDate = Date.valueOf(validStartDate.toLocalDate().plusDays(1));
        assertTrue(leaveRequest.hasConflictWithAttendance(middleDate));
        
        logger.info("Attendance conflict positive tests passed");
    }
    
    @Test
    public void testHasConflictWithAttendanceNegative() {
        logger.info("Testing attendance conflict detection - negative cases");
        
        leaveRequest.setLeaveStart(validStartDate);
        leaveRequest.setLeaveEnd(validEndDate);
        
        // Test no conflict before start
        Date beforeStart = Date.valueOf(validStartDate.toLocalDate().minusDays(1));
        assertFalse(leaveRequest.hasConflictWithAttendance(beforeStart));
        
        // Test no conflict after end
        Date afterEnd = Date.valueOf(validEndDate.toLocalDate().plusDays(1));
        assertFalse(leaveRequest.hasConflictWithAttendance(afterEnd));
        
        // Test with null values
        assertFalse(leaveRequest.hasConflictWithAttendance(null));
        
        leaveRequest.setLeaveStart(null);
        assertFalse(leaveRequest.hasConflictWithAttendance(validStartDate));
        
        leaveRequest.setLeaveStart(validStartDate);
        leaveRequest.setLeaveEnd(null);
        assertFalse(leaveRequest.hasConflictWithAttendance(validStartDate));
        
        logger.info("Attendance conflict negative tests passed");
    }
    
    @Test
    public void testGetConflictingAttendanceDatesPositive() {
        logger.info("Testing get conflicting attendance dates");
        
        leaveRequest.setLeaveStart(validStartDate);
        leaveRequest.setLeaveEnd(validEndDate);
        
        List<Date> attendanceDates = new ArrayList<Date>();
        attendanceDates.add(validStartDate);
        attendanceDates.add(Date.valueOf(validStartDate.toLocalDate().plusDays(1)));
        attendanceDates.add(Date.valueOf(validEndDate.toLocalDate().plusDays(1)));
        
        List<Date> conflicts = leaveRequest.getConflictingAttendanceDates(attendanceDates);
        
        assertEquals(2, conflicts.size());
        assertTrue(conflicts.contains(validStartDate));
        assertTrue(conflicts.contains(Date.valueOf(validStartDate.toLocalDate().plusDays(1))));
        assertFalse(conflicts.contains(Date.valueOf(validEndDate.toLocalDate().plusDays(1))));
        
        logger.info("Get conflicting attendance dates positive test passed - conflicts found: " + conflicts.size());
    }
    
    @Test
    public void testGetConflictingAttendanceDatesNegative() {
        logger.info("Testing get conflicting attendance dates - negative cases");
        
        leaveRequest.setLeaveStart(validStartDate);
        leaveRequest.setLeaveEnd(validEndDate);
        
        // Test with null list
        List<Date> conflicts = leaveRequest.getConflictingAttendanceDates(null);
        assertTrue(conflicts.isEmpty());
        
        // Test with empty list
        conflicts = leaveRequest.getConflictingAttendanceDates(new ArrayList<Date>());
        assertTrue(conflicts.isEmpty());
        
        // Test with list containing null dates
        List<Date> datesWithNull = new ArrayList<Date>();
        datesWithNull.add(null);
        datesWithNull.add(validStartDate);
        conflicts = leaveRequest.getConflictingAttendanceDates(datesWithNull);
        assertEquals(1, conflicts.size());
        
        logger.info("Get conflicting attendance dates negative tests passed");
    }
    
    // Leave Dates Tests
    @Test
    public void testGetAllLeaveDatesPositive() {
        logger.info("Testing get all leave dates");
        
        leaveRequest.setLeaveStart(validStartDate);
        leaveRequest.setLeaveEnd(validEndDate);
        
        List<Date> allDates = leaveRequest.getAllLeaveDates();
        
        assertTrue(allDates.size() >= 5);
        assertTrue(allDates.contains(validStartDate));
        assertTrue(allDates.contains(validEndDate));
        
        // Test single day leave
        leaveRequest.setLeaveStart(validStartDate);
        leaveRequest.setLeaveEnd(validStartDate);
        allDates = leaveRequest.getAllLeaveDates();
        assertEquals(1, allDates.size());
        
        logger.info("Get all leave dates positive test passed - total dates: " + allDates.size());
    }
    
    @Test
    public void testGetAllLeaveDatesNegative() {
        logger.info("Testing get all leave dates - negative cases");
        
        // Test with null start date
        leaveRequest.setLeaveStart(null);
        leaveRequest.setLeaveEnd(validEndDate);
        List<Date> allDates = leaveRequest.getAllLeaveDates();
        assertTrue(allDates.isEmpty());
        
        // Test with null end date
        leaveRequest.setLeaveStart(validStartDate);
        leaveRequest.setLeaveEnd(null);
        allDates = leaveRequest.getAllLeaveDates();
        assertTrue(allDates.isEmpty());
        
        // Test with both null
        leaveRequest.setLeaveStart(null);
        leaveRequest.setLeaveEnd(null);
        allDates = leaveRequest.getAllLeaveDates();
        assertTrue(allDates.isEmpty());
        
        logger.info("Get all leave dates negative tests passed");
    }
    
    @Test
    public void testGetWorkingDayLeaveDatesPositive() {
        logger.info("Testing get working day leave dates");
        
        // Set leave from Monday to Friday
        LocalDate monday = LocalDate.now(MANILA_TIMEZONE).with(DayOfWeek.MONDAY).plusWeeks(1);
        LocalDate friday = monday.plusDays(4);
        
        leaveRequest.setLeaveStart(Date.valueOf(monday));
        leaveRequest.setLeaveEnd(Date.valueOf(friday));
        
        List<Date> workingDays = leaveRequest.getWorkingDayLeaveDates();
        
        assertEquals(5, workingDays.size());
        
        // Verify no weekends
        for (Date date : workingDays) {
            DayOfWeek dayOfWeek = date.toLocalDate().getDayOfWeek();
            assertNotEquals(DayOfWeek.SATURDAY, dayOfWeek);
            assertNotEquals(DayOfWeek.SUNDAY, dayOfWeek);
        }
        
        logger.info("Get working day leave dates positive test passed - working days: " + workingDays.size());
    }
    
    @Test
    public void testGetWorkingDayLeaveDatesNegative() {
        logger.info("Testing get working day leave dates - negative cases");
        
        // Test with null dates
        List<Date> workingDays = leaveRequest.getWorkingDayLeaveDates();
        assertTrue(workingDays.isEmpty());
        
        // Test weekend only leave
        LocalDate saturday = LocalDate.now(MANILA_TIMEZONE).with(DayOfWeek.SATURDAY).plusWeeks(1);
        LocalDate sunday = saturday.plusDays(1);
        
        leaveRequest.setLeaveStart(Date.valueOf(saturday));
        leaveRequest.setLeaveEnd(Date.valueOf(sunday));
        
        workingDays = leaveRequest.getWorkingDayLeaveDates();
        assertEquals(0, workingDays.size());
        
        logger.info("Get working day leave dates negative tests passed");
    }
    
    // Validation Tests
    @Test
    public void testIsValidLeaveRequestPositive() {
        logger.info("Testing leave request validation - positive cases");
        
        leaveRequest.setEmployeeId(101);
        leaveRequest.setLeaveTypeId(1);
        leaveRequest.setLeaveStart(validStartDate);
        leaveRequest.setLeaveEnd(validEndDate);
        
        assertTrue(leaveRequest.isValidLeaveRequest());
        
        // Test with today's date
        leaveRequest.setLeaveStart(todayDate);
        leaveRequest.setLeaveEnd(validEndDate);
        assertTrue(leaveRequest.isValidLeaveRequest());
        
        logger.info("Leave request validation positive tests passed");
    }
    
    @Test
    public void testIsValidLeaveRequestNegative() {
        logger.info("Testing leave request validation - negative cases");
        
        // Test invalid employee ID
        leaveRequest.setEmployeeId(0);
        leaveRequest.setLeaveTypeId(1);
        leaveRequest.setLeaveStart(validStartDate);
        leaveRequest.setLeaveEnd(validEndDate);
        assertFalse(leaveRequest.isValidLeaveRequest());
        
        // Test negative employee ID
        leaveRequest.setEmployeeId(-1);
        assertFalse(leaveRequest.isValidLeaveRequest());
        
        // Test invalid leave type ID
        leaveRequest.setEmployeeId(101);
        leaveRequest.setLeaveTypeId(0);
        assertFalse(leaveRequest.isValidLeaveRequest());
        
        // Test negative leave type ID
        leaveRequest.setLeaveTypeId(-1);
        assertFalse(leaveRequest.isValidLeaveRequest());
        
        // Test null start date
        leaveRequest.setLeaveTypeId(1);
        leaveRequest.setLeaveStart(null);
        assertFalse(leaveRequest.isValidLeaveRequest());
        
        // Test null end date
        leaveRequest.setLeaveStart(validStartDate);
        leaveRequest.setLeaveEnd(null);
        assertFalse(leaveRequest.isValidLeaveRequest());
        
        // Test end date before start date
        leaveRequest.setLeaveStart(validEndDate);
        leaveRequest.setLeaveEnd(validStartDate);
        assertFalse(leaveRequest.isValidLeaveRequest());
        
        // Test past date
        leaveRequest.setLeaveStart(pastDate);
        leaveRequest.setLeaveEnd(validEndDate);
        assertFalse(leaveRequest.isValidLeaveRequest());
        
        logger.info("Leave request validation negative tests passed");
    }
    
    @Test
    public void testHasWorkingDays() {
        logger.info("Testing has working days");
        
        // Positive case - weekdays
        leaveRequest.setLeaveStart(validStartDate);
        leaveRequest.setLeaveEnd(validEndDate);
        assertTrue(leaveRequest.hasWorkingDays());
        
        // Negative case - weekend only
        LocalDate saturday = LocalDate.now(MANILA_TIMEZONE).with(DayOfWeek.SATURDAY).plusWeeks(1);
        LocalDate sunday = saturday.plusDays(1);
        leaveRequest.setLeaveStart(Date.valueOf(saturday));
        leaveRequest.setLeaveEnd(Date.valueOf(sunday));
        assertFalse(leaveRequest.hasWorkingDays());
        
        // Negative case - no dates set
        leaveRequest.setLeaveStart(null);
        leaveRequest.setLeaveEnd(null);
        assertFalse(leaveRequest.hasWorkingDays());
        
        logger.info("Has working days tests passed");
    }
    
    // Approval Workflow Tests
    @Test
    public void testApproveLeaveRequestPositive() {
        logger.info("Testing approve leave request");
        
        String notes = "Approved by supervisor";
        leaveRequest.approve(notes);
        
        assertTrue(leaveRequest.isApproved());
        assertFalse(leaveRequest.isPending());
        assertFalse(leaveRequest.isRejected());
        assertEquals(notes, leaveRequest.getSupervisorNotes());
        assertNotNull(leaveRequest.getDateApproved());
        
        logger.info("Approve leave request positive test passed");
    }
    
    @Test
    public void testApproveLeaveRequestNegative() {
        logger.info("Testing approve leave request - negative cases");
        
        // Test with null notes
        leaveRequest.approve(null);
        assertTrue(leaveRequest.isApproved());
        assertNull(leaveRequest.getSupervisorNotes());
        assertNotNull(leaveRequest.getDateApproved());
        
        // Test with empty notes
        leaveRequest = new LeaveRequestModel();
        leaveRequest.approve("");
        assertTrue(leaveRequest.isApproved());
        assertEquals("", leaveRequest.getSupervisorNotes());
        
        logger.info("Approve leave request negative tests passed");
    }
    
    @Test
    public void testRejectLeaveRequestPositive() {
        logger.info("Testing reject leave request");
        
        String notes = "Rejected due to staffing needs";
        leaveRequest.reject(notes);
        
        assertTrue(leaveRequest.isRejected());
        assertFalse(leaveRequest.isPending());
        assertFalse(leaveRequest.isApproved());
        assertEquals(notes, leaveRequest.getSupervisorNotes());
        assertNotNull(leaveRequest.getDateApproved());
        
        logger.info("Reject leave request positive test passed");
    }
    
    @Test
    public void testRejectLeaveRequestNegative() {
        logger.info("Testing reject leave request - negative cases");
        
        // Test with null notes
        leaveRequest.reject(null);
        assertTrue(leaveRequest.isRejected());
        assertNull(leaveRequest.getSupervisorNotes());
        assertNotNull(leaveRequest.getDateApproved());
        
        // Test with empty notes
        leaveRequest = new LeaveRequestModel();
        leaveRequest.reject("");
        assertTrue(leaveRequest.isRejected());
        assertEquals("", leaveRequest.getSupervisorNotes());
        
        logger.info("Reject leave request negative tests passed");
    }
    
    // Business Logic Tests
    @Test
    public void testCanBeModified() {
        logger.info("Testing can be modified");
        
        // Positive case - pending requests can be modified
        assertTrue(leaveRequest.canBeModified());
        
        // Negative case - approved requests cannot be modified
        leaveRequest.approve("Approved");
        assertFalse(leaveRequest.canBeModified());
        
        // Negative case - rejected requests cannot be modified
        leaveRequest = new LeaveRequestModel();
        leaveRequest.reject("Rejected");
        assertFalse(leaveRequest.canBeModified());
        
        logger.info("Can be modified tests passed");
    }
    
    @Test
    public void testCanBeCancelledPositive() {
        logger.info("Testing can be cancelled - positive cases");
        
        leaveRequest.setLeaveStart(futureDate);
        leaveRequest.setLeaveEnd(Date.valueOf(futureDate.toLocalDate().plusDays(2)));
        
        // Pending requests can be cancelled
        assertTrue(leaveRequest.canBeCancelled());
        
        // Approved future requests can be cancelled
        leaveRequest.approve("Approved");
        assertTrue(leaveRequest.canBeCancelled());
        
        logger.info("Can be cancelled positive tests passed");
    }
    
    @Test
    public void testCanBeCancelledNegative() {
        logger.info("Testing can be cancelled - negative cases");
        
        // Rejected requests cannot be cancelled
        leaveRequest.setLeaveStart(futureDate);
        leaveRequest.setLeaveEnd(Date.valueOf(futureDate.toLocalDate().plusDays(2)));
        leaveRequest.reject("Rejected");
        assertFalse(leaveRequest.canBeCancelled());
        
        // Past approved requests cannot be cancelled
        leaveRequest = new LeaveRequestModel();
        leaveRequest.setLeaveStart(pastDate);
        leaveRequest.setLeaveEnd(Date.valueOf(pastDate.toLocalDate().plusDays(2)));
        leaveRequest.approve("Approved");
        assertFalse(leaveRequest.canBeCancelled());
        
        logger.info("Can be cancelled negative tests passed");
    }
    
    @Test
    public void testGetLeaveStatusDisplayText() {
        logger.info("Testing get leave status display text");
        
        assertEquals("Pending", leaveRequest.getLeaveStatusDisplayText());
        
        leaveRequest.approve("Approved");
        assertEquals("Approved", leaveRequest.getLeaveStatusDisplayText());
        
        leaveRequest.reject("Rejected");
        assertEquals("Rejected", leaveRequest.getLeaveStatusDisplayText());
        
        logger.info("Get leave status display text tests passed");
    }
    
    // Setter Tests with Side Effects
    @Test
    public void testSettersRecalculateWorkingDays() {
        logger.info("Testing setters recalculate working days");
        
        int initialWorkingDays = leaveRequest.getWorkingDaysCount();
        
        leaveRequest.setLeaveStart(validStartDate);
        leaveRequest.setLeaveEnd(validEndDate);
        
        assertTrue(leaveRequest.getWorkingDaysCount() > initialWorkingDays);
        
        // Test changing dates
        int previousCount = leaveRequest.getWorkingDaysCount();
        leaveRequest.setLeaveEnd(Date.valueOf(validEndDate.toLocalDate().plusDays(2)));
        assertTrue(leaveRequest.getWorkingDaysCount() >= previousCount);
        
        logger.info("Setters recalculate working days tests passed");
    }
    
    @Test
    public void testWorkingDaysCountConsistency() {
        logger.info("Testing working days count consistency");
        
        leaveRequest.setLeaveStart(validStartDate);
        leaveRequest.setLeaveEnd(validEndDate);
        
        int calculatedCount = leaveRequest.getWorkingDayLeaveDates().size();
        int storedCount = leaveRequest.getWorkingDaysCount();
        
        assertEquals(calculatedCount, storedCount);
        
        // Test manual override
        leaveRequest.setWorkingDaysCount(999);
        assertEquals(999, leaveRequest.getWorkingDaysCount());
        
        // Setting dates should recalculate
        leaveRequest.setLeaveStart(validStartDate);
        assertNotEquals(999, leaveRequest.getWorkingDaysCount());
        
        logger.info("Working days count consistency tests passed");
    }
    
    // Object Methods Tests
    @Test
    public void testToString() {
        logger.info("Testing toString method");
        
        leaveRequest.setLeaveRequestId(1);
        leaveRequest.setEmployeeId(101);
        leaveRequest.setLeaveTypeId(1);
        leaveRequest.setLeaveStart(validStartDate);
        leaveRequest.setLeaveEnd(validEndDate);
        
        String toString = leaveRequest.toString();
        
        assertNotNull(toString);
        assertTrue(toString.contains("LeaveRequestModel"));
        assertTrue(toString.contains("leaveRequestId=1"));
        assertTrue(toString.contains("employeeId=101"));
        assertTrue(toString.contains("leaveTypeId=1"));
        
        logger.info("ToString method test passed: " + toString);
    }
    
    @Test
    public void testEquals() {
        logger.info("Testing equals method");
        
        LeaveRequestModel request1 = new LeaveRequestModel();
        LeaveRequestModel request2 = new LeaveRequestModel();
        
        // Test with same ID
        request1.setLeaveRequestId(1);
        request2.setLeaveRequestId(1);
        assertTrue(request1.equals(request2));
        
        // Test self equality
        assertTrue(request1.equals(request1));
        
        // Test with different IDs
        request2.setLeaveRequestId(2);
        assertFalse(request1.equals(request2));
        
        // Test with null
        assertFalse(request1.equals(null));
        
        // Test with different class
        assertFalse(request1.equals("string"));
        
        // Test with default IDs (should be 0)
        LeaveRequestModel request3 = new LeaveRequestModel();
        LeaveRequestModel request4 = new LeaveRequestModel();
        assertTrue(request3.equals(request4));
        
        logger.info("Equals method tests passed");
    }
    
    @Test
    public void testHashCode() {
        logger.info("Testing hashCode method");
        
        LeaveRequestModel request1 = new LeaveRequestModel();
        LeaveRequestModel request2 = new LeaveRequestModel();
        
        // Test with same ID
        request1.setLeaveRequestId(1);
        request2.setLeaveRequestId(1);
        assertEquals(request1.hashCode(), request2.hashCode());
        
        // Test with different IDs
        request2.setLeaveRequestId(2);
        assertNotEquals(request1.hashCode(), request2.hashCode());
        
        // Test consistency
        int hash1 = request1.hashCode();
        int hash2 = request1.hashCode();
        assertEquals(hash1, hash2);
        
        logger.info("HashCode method tests passed");
    }
    
    // Edge Cases and Error Handling
    @Test
    public void testSameDateLeaveRequest() {
        logger.info("Testing same date leave request");
        
        leaveRequest.setLeaveStart(validStartDate);
        leaveRequest.setLeaveEnd(validStartDate);
        
        List<Date> allDates = leaveRequest.getAllLeaveDates();
        assertEquals(1, allDates.size());
        assertTrue(allDates.contains(validStartDate));
        
        // Check if it's a working day
        DayOfWeek dayOfWeek = validStartDate.toLocalDate().getDayOfWeek();
        if (dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY) {
            assertEquals(1, leaveRequest.getWorkingDaysCount());
        } else {
            assertEquals(0, leaveRequest.getWorkingDaysCount());
        }
        
        logger.info("Same date leave request test passed");
    }
    
    @Test
    public void testNullSafety() {
        logger.info("Testing null safety across multiple scenarios");
        
        LeaveRequestModel nullRequest = new LeaveRequestModel();
        
        // Test all getter methods with null state
        assertNull(nullRequest.getLeaveStart());
        assertNull(nullRequest.getLeaveEnd());
        assertNull(nullRequest.getLeaveReason());
        assertNull(nullRequest.getSupervisorNotes());
        assertNull(nullRequest.getDateApproved());
        assertNotNull(nullRequest.getDateCreated()); // Should not be null from constructor
        
        // Test methods that should handle null gracefully
        assertFalse(nullRequest.hasConflictWithAttendance(validStartDate));
        assertTrue(nullRequest.getAllLeaveDates().isEmpty());
        assertTrue(nullRequest.getWorkingDayLeaveDates().isEmpty());
        assertTrue(nullRequest.getConflictingAttendanceDates(null).isEmpty());
        
        // Test validation with null values
        assertFalse(nullRequest.isValidLeaveRequest());
        assertFalse(nullRequest.hasWorkingDays());
        
        logger.info("Null safety tests passed");
    }
    
    @Test
    public void testDataConsistency() {
        logger.info("Testing data consistency in complex scenario");
        
        // Setup complex scenario
        leaveRequest.setLeaveRequestId(999);
        leaveRequest.setEmployeeId(101);
        leaveRequest.setLeaveTypeId(1);
        leaveRequest.setLeaveStart(validStartDate);
        leaveRequest.setLeaveEnd(validEndDate);
        leaveRequest.setLeaveReason("Annual vacation");
        leaveRequest.setHasAttendanceConflict(true);
        
        // Verify all data is consistent
        assertEquals(999, leaveRequest.getLeaveRequestId());
        assertEquals(101, leaveRequest.getEmployeeId());
        assertEquals(1, leaveRequest.getLeaveTypeId());
        assertEquals(validStartDate, leaveRequest.getLeaveStart());
        assertEquals(validEndDate, leaveRequest.getLeaveEnd());
        assertEquals("Annual vacation", leaveRequest.getLeaveReason());
        assertTrue(leaveRequest.isHasAttendanceConflict());
        
        // Test workflow consistency
        assertTrue(leaveRequest.canBeModified());
        assertTrue(leaveRequest.canBeCancelled());
        
        leaveRequest.approve("Approved by manager");
        assertFalse(leaveRequest.canBeModified());
        assertEquals("Approved", leaveRequest.getLeaveStatusDisplayText());
        
        logger.info("Data consistency test passed");
    }
    
    @Test
    public void testPerformanceLargeDateRange() {
        logger.info("Testing performance with large date range");
        
        LocalDate startDate = LocalDate.now(MANILA_TIMEZONE).plusDays(1);
        LocalDate endDate = startDate.plusDays(100); // 100 days for reasonable test time
        
        leaveRequest.setLeaveStart(Date.valueOf(startDate));
        leaveRequest.setLeaveEnd(Date.valueOf(endDate));
        
        long startTime = System.currentTimeMillis();
        
        List<Date> allDates = leaveRequest.getAllLeaveDates();
        List<Date> workingDays = leaveRequest.getWorkingDayLeaveDates();
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        assertEquals(101, allDates.size());
        assertTrue(workingDays.size() > 0);
        assertTrue(workingDays.size() < allDates.size());
        
        logger.info("Performance test passed - processed " + allDates.size() + 
                   " dates in " + duration + "ms");
        
        // Performance should be reasonable (less than 1 second for 100 days)
        assertTrue("Performance degradation: took " + duration + "ms", duration < 1000);
    }
    
    @Test
    public void testMultipleStatusTransitions() {
        logger.info("Testing multiple status transitions");
        
        // Start with pending
        assertTrue(leaveRequest.isPending());
        assertEquals("Pending", leaveRequest.getLeaveStatusDisplayText());
        
        // Approve
        leaveRequest.approve("First approval");
        assertTrue(leaveRequest.isApproved());
        assertEquals("Approved", leaveRequest.getLeaveStatusDisplayText());
        assertEquals("First approval", leaveRequest.getSupervisorNotes());
        Timestamp firstApprovalTime = leaveRequest.getDateApproved();
        
        // Reject (overriding previous approval)
        leaveRequest.reject("Changed mind - rejected");
        assertTrue(leaveRequest.isRejected());
        assertEquals("Rejected", leaveRequest.getLeaveStatusDisplayText());
        assertEquals("Changed mind - rejected", leaveRequest.getSupervisorNotes());
        
        // Verify timestamp was updated
        assertTrue(leaveRequest.getDateApproved().after(firstApprovalTime) || 
                  leaveRequest.getDateApproved().equals(firstApprovalTime));
        
        // Approve again
        leaveRequest.approve("Final approval");
        assertTrue(leaveRequest.isApproved());
        assertEquals("Final approval", leaveRequest.getSupervisorNotes());
        
        logger.info("Multiple status transitions test passed");
    }
    
    @Test
    public void testComplexConflictScenarios() {
        logger.info("Testing complex conflict scenarios");
        
        // Setup leave request
        leaveRequest.setLeaveStart(validStartDate);
        leaveRequest.setLeaveEnd(validEndDate);
        
        // Create complex attendance scenario
        List<Date> attendanceDates = new ArrayList<Date>();
        
        // Add dates before, during, and after leave
        LocalDate startLocal = validStartDate.toLocalDate();
        LocalDate endLocal = validEndDate.toLocalDate();
        
        // Before leave
        attendanceDates.add(Date.valueOf(startLocal.minusDays(1)));
        
        // During leave
        attendanceDates.add(Date.valueOf(startLocal));
        attendanceDates.add(Date.valueOf(startLocal.plusDays(1)));
        attendanceDates.add(Date.valueOf(endLocal));
        
        // After leave
        attendanceDates.add(Date.valueOf(endLocal.plusDays(1)));
        
        // Add some null dates
        attendanceDates.add(null);
        
        List<Date> conflicts = leaveRequest.getConflictingAttendanceDates(attendanceDates);
        
        // Should have 3 conflicts (start, middle, end)
        assertEquals(3, conflicts.size());
        assertTrue(conflicts.contains(Date.valueOf(startLocal)));
        assertTrue(conflicts.contains(Date.valueOf(startLocal.plusDays(1))));
        assertTrue(conflicts.contains(Date.valueOf(endLocal)));
        
        logger.info("Complex conflict scenarios test passed - conflicts found: " + conflicts.size());
    }
    
    @Test
    public void testBoundaryConditions() {
        logger.info("Testing boundary conditions");
        
        // Test with today's date as start
        leaveRequest.setLeaveStart(todayDate);
        leaveRequest.setLeaveEnd(todayDate);
        leaveRequest.setEmployeeId(101);
        leaveRequest.setLeaveTypeId(1);
        
        assertTrue(leaveRequest.isValidLeaveRequest());
        
        // Test minimum valid leave request
        LeaveRequestModel minRequest = new LeaveRequestModel();
        minRequest.setLeaveRequestId(1);
        minRequest.setEmployeeId(1);
        minRequest.setLeaveTypeId(1);
        minRequest.setLeaveStart(validStartDate);
        minRequest.setLeaveEnd(validStartDate);
        
        assertTrue(minRequest.isValidLeaveRequest());
        assertTrue(minRequest.getWorkingDaysCount() >= 0);
        
        logger.info("Boundary conditions test passed");
    }
    
    @Test
    public void testWeekendHandling() {
        logger.info("Testing weekend handling");
        
        // Find next Saturday
        LocalDate saturday = LocalDate.now(MANILA_TIMEZONE).with(DayOfWeek.SATURDAY);
        if (!saturday.isAfter(LocalDate.now(MANILA_TIMEZONE))) {
            saturday = saturday.plusWeeks(1);
        }
        LocalDate sunday = saturday.plusDays(1);
        
        leaveRequest.setLeaveStart(Date.valueOf(saturday));
        leaveRequest.setLeaveEnd(Date.valueOf(sunday));
        
        List<Date> allDates = leaveRequest.getAllLeaveDates();
        List<Date> workingDays = leaveRequest.getWorkingDayLeaveDates();
        
        assertEquals(2, allDates.size());
        assertEquals(0, workingDays.size());
        assertFalse(leaveRequest.hasWorkingDays());
        
        logger.info("Weekend handling test passed");
    }
    
    @Test
    public void testInvalidDateRanges() {
        logger.info("Testing invalid date ranges");
        
        leaveRequest.setEmployeeId(101);
        leaveRequest.setLeaveTypeId(1);
        
        // Test end date before start date
        leaveRequest.setLeaveStart(validEndDate);
        leaveRequest.setLeaveEnd(validStartDate);
        assertFalse(leaveRequest.isValidLeaveRequest());
        
        // Test past start date
        leaveRequest.setLeaveStart(pastDate);
        leaveRequest.setLeaveEnd(validEndDate);
        assertFalse(leaveRequest.isValidLeaveRequest());
        
        // Test past end date
        leaveRequest.setLeaveStart(validStartDate);
        leaveRequest.setLeaveEnd(pastDate);
        assertFalse(leaveRequest.isValidLeaveRequest());
        
        logger.info("Invalid date ranges test passed");
    }
    
    @Test
    public void testEmptyAndNullStrings() {
        logger.info("Testing empty and null string handling");
        
        // Test empty reason
        leaveRequest.setLeaveReason("");
        assertEquals("", leaveRequest.getLeaveReason());
        
        // Test null reason
        leaveRequest.setLeaveReason(null);
        assertNull(leaveRequest.getLeaveReason());
        
        // Test empty supervisor notes
        leaveRequest.setSupervisorNotes("");
        assertEquals("", leaveRequest.getSupervisorNotes());
        
        // Test null supervisor notes
        leaveRequest.setSupervisorNotes(null);
        assertNull(leaveRequest.getSupervisorNotes());
        
        // Test approval/rejection with empty strings
        leaveRequest.approve("");
        assertEquals("", leaveRequest.getSupervisorNotes());
        
        leaveRequest.reject("");
        assertEquals("", leaveRequest.getSupervisorNotes());
        
        logger.info("Empty and null string handling test passed");
    }
    
    @Test
    public void testWorkingDaysCalculationAccuracy() {
        logger.info("Testing working days calculation accuracy");
        
        // Test Monday to Friday (5 working days)
        LocalDate monday = LocalDate.now(MANILA_TIMEZONE).with(DayOfWeek.MONDAY);
        if (!monday.isAfter(LocalDate.now(MANILA_TIMEZONE))) {
            monday = monday.plusWeeks(1);
        }
        LocalDate friday = monday.plusDays(4);
        
        leaveRequest.setLeaveStart(Date.valueOf(monday));
        leaveRequest.setLeaveEnd(Date.valueOf(friday));
        
        assertEquals(5, leaveRequest.getWorkingDaysCount());
        
        // Test Monday to Sunday (5 working days)
        LocalDate sunday = monday.plusDays(6);
        leaveRequest.setLeaveEnd(Date.valueOf(sunday));
        
        assertEquals(5, leaveRequest.getWorkingDaysCount());
        
        // Test single working day
        leaveRequest.setLeaveEnd(Date.valueOf(monday));
        assertEquals(1, leaveRequest.getWorkingDaysCount());
        
        logger.info("Working days calculation accuracy test passed");
    }
    
    @Test
    public void testTimezoneConsistency() {
        logger.info("Testing timezone consistency");
        
        // Test that Manila time is used consistently
        LocalDateTime manilaTime1 = LeaveRequestModel.nowInManila();
        LocalDateTime manilaTime2 = LeaveRequestModel.nowInManila();
        
        assertNotNull(manilaTime1);
        assertNotNull(manilaTime2);
        
        // Times should be very close (within 1 second)
        assertTrue(Math.abs(manilaTime1.toEpochSecond(ZoneOffset.ofHours(8)) - 
                          manilaTime2.toEpochSecond(ZoneOffset.ofHours(8))) <= 1);
        
        // Test creation and approval timestamps
        leaveRequest.approve("Test approval");
        
        LocalDateTime createdTime = leaveRequest.getCreatedDateInManila();
        LocalDateTime approvedTime = leaveRequest.getApprovedDateInManila();
        
        assertNotNull(createdTime);
        assertNotNull(approvedTime);
        assertTrue(approvedTime.isAfter(createdTime) || approvedTime.equals(createdTime));
        
        logger.info("Timezone consistency test passed");
    }
}
