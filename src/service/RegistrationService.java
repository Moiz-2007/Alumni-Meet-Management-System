package service;

import dao.RegistrationDAO;
import database.DBConnection;
import model.Registration;
import util.Choice;
import util.ServiceException;
import util.Sql;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public class RegistrationService {
    public static final Set<String> STATUSES = Set.of("Pending", "Confirmed", "Cancelled");
    private final RegistrationDAO dao = new RegistrationDAO();

    public List<Registration> search(String keyword) { return Sql.call(() -> dao.findAll(keyword)); }

    public List<Choice> choices() { return Sql.call(dao::choices); }

    /**
     * Registers an alumnus for an event inside ONE transaction.
     * The event row is locked with SELECT ... FOR UPDATE, so two people registering at the
     * same moment cannot both take the last seat. The UNIQUE (Alumni_ID, Meet_ID) constraint
     * blocks duplicate registrations.
     */
    public void register(int alumniId, int meetId, String status) {
        checkStatus(status);
        Registration r = new Registration(null, alumniId, meetId, LocalDate.now(), status, null, null);
        Sql.exec(() -> {
            try (Connection c = DBConnection.getConnection()) {
                c.setAutoCommit(false);
                try {
                    if (!"Cancelled".equals(status)) requireSeat(c, meetId);
                    dao.insert(c, r);
                    c.commit();
                } catch (SQLException | RuntimeException e) {
                    c.rollback();
                    throw e;
                }
            }
        });
    }

    /** Changes the status. Re-activating a cancelled registration needs a free seat again. */
    public void updateStatus(int registrationId, String newStatus) {
        checkStatus(newStatus);
        Sql.exec(() -> {
            try (Connection c = DBConnection.getConnection()) {
                c.setAutoCommit(false);
                try {
                    Registration current = dao.findForUpdate(c, registrationId);
                    if (current == null) throw new ServiceException("This registration no longer exists.");
                    boolean reactivating = "Cancelled".equals(current.status()) && !"Cancelled".equals(newStatus);
                    if (reactivating) requireSeat(c, current.meetId());
                    dao.updateStatus(c, registrationId, newStatus);
                    c.commit();
                } catch (SQLException | RuntimeException e) {
                    c.rollback();
                    throw e;
                }
            }
        });
    }

    public void delete(int registrationId) {
        Sql.exec(() -> { if (!dao.delete(registrationId)) throw new ServiceException("This registration no longer exists."); });
    }

    private void requireSeat(Connection c, int meetId) throws SQLException {
        Integer capacity = dao.lockEventCapacity(c, meetId);
        if (capacity == null) throw new ServiceException("The selected event does not exist.");
        if (dao.countActive(c, meetId) >= capacity)
            throw new ServiceException("This event is full (capacity " + capacity + "). No more registrations can be accepted.");
    }

    private void checkStatus(String status) {
        if (status == null || !STATUSES.contains(status))
            throw new ServiceException("Registration status must be Pending, Confirmed or Cancelled.");
    }
}
