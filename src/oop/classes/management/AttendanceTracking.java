/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package oop.classes.management;

/**
 *This is an interface class that handles attendance approval methods.
 * @author Admin
 */

public interface AttendanceTracking {
    //Approval status for attendance; used by Immediate Supervisor and HR
    public abstract boolean approveAttendance(int attendanceID);
    public abstract boolean denyAttendance(int attendanceID, String reason);
}
