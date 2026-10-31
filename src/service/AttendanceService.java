package service;

import dao.AttendanceDAO;
import dao.RegistrationDAO;
import model.Attendance;
import util.ServiceException;
import util.Sql;
import util.Validator;

import java.time.LocalDateTime;
import java.util.List;

public class AttendanceService {
    private final AttendanceDAO dao = new AttendanceDAO();
    private final RegistrationDAO registrations = new RegistrationDAO();

    public List<Attendance> search(String keyword) { return Sql.call(() -> dao.findAll(keyword)); }

    public void add(int registrationId, String status, String checkIn, String checkOut) {
        Attendance a = build(null, registrationId, status, checkIn, checkOut);
        Sql.exec(() -> {
            String regStatus = registrations.statusOf(registrationId);
            if (regStatus == null) throw new ServiceException("The selected registration does not exist.");
            if ("Cancelled".equals(regStatus)) throw new ServiceException("Cannot record attendance for a cancelled registration.");
            dao.insert(a); // UNIQUE (Registration_ID) rejects a second record
        });
    }

    public void update(int attendanceId, int registrationId, String status, String checkIn, String checkOut) {
        Attendance a = build(attendanceId, registrationId, status, checkIn, checkOut);
        Sql.exec(() -> { if (!dao.update(a)) throw new ServiceException("This attendance record no longer exists."); });
    }

    public void checkInNow(int attendanceId) {
        Sql.exec(() -> { if (!dao.checkInNow(attendanceId)) throw new ServiceException("This attendance record no longer exists."); });
    }

    public void checkOutNow(int attendanceId) {
        Sql.exec(() -> { if (!dao.checkOutNow(attendanceId)) throw new ServiceException("Check-out failed: the person has not checked in yet."); });
    }

    public void delete(int attendanceId) {
        Sql.exec(() -> { if (!dao.delete(attendanceId)) throw new ServiceException("This attendance record no longer exists."); });
    }

    private Attendance build(Integer id, int registrationId, String status, String checkIn, String checkOut) {
        if (!"Present".equals(status) && !"Absent".equals(status))
            throw new ServiceException("Attendance status must be Present or Absent.");
        LocalDateTime in = Validator.optional(checkIn) == null ? null : Validator.dateTime(checkIn, "Check-in time");
        LocalDateTime out = Validator.optional(checkOut) == null ? null : Validator.dateTime(checkOut, "Check-out time");
        if ("Absent".equals(status)) {
            if (in != null || out != null) throw new ServiceException("An absent person cannot have check-in or check-out times. Clear them or mark Present.");
        } else {
            if (out != null && in == null) throw new ServiceException("Check-out time needs a check-in time.");
            if (in != null && out != null && out.isBefore(in)) throw new ServiceException("Check-out time cannot be earlier than check-in time.");
        }
        return new Attendance(id, registrationId, status, in, out, null, null);
    }
}
