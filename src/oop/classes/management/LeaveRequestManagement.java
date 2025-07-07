package oop.classes.management;

import gui.LeaveRequest;
import java.util.List;

/**
 * Interface for managing leave requests.
 * Defines methods for handling employee leave requests.
 */
public interface LeaveRequestManagement {
    
    boolean submitLeaveRequest(LeaveRequest leaveRequest);

    List<LeaveRequest> getAllLeaveRequests();

    /**
     * Approves a leave request based on request ID.
     * @param requestId The unique ID of the leave request.
     * @return True if approval was successful.
     */
    boolean approveLeaveRequest(int requestId);

    /**
     * Rejects a leave request based on request ID.
     * @param requestId The unique ID of the leave request.
     * @return True if rejection was successful.
     */
    boolean rejectLeaveRequest(int requestId);
}