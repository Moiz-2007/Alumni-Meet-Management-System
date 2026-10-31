package util;

import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Runs JDBC work and converts SQLException into a friendly ServiceException. */
public final class Sql {
    private Sql() {}

    @FunctionalInterface public interface Work<T> { T run() throws SQLException; }
    @FunctionalInterface public interface VoidWork { void run() throws SQLException; }

    public static <T> T call(Work<T> work) {
        try { return work.run(); }
        catch (SQLException e) { throw new ServiceException(friendly(e), e); }
    }

    public static void exec(VoidWork work) {
        try { work.run(); }
        catch (SQLException e) { throw new ServiceException(friendly(e), e); }
    }

    /** Maps MySQL error codes to messages a normal user can understand. */
    public static String friendly(SQLException e) {
        String state = e.getSQLState() == null ? "" : e.getSQLState();
        String msg = e.getMessage() == null ? "" : e.getMessage();
        if (state.startsWith("08")) {
            return msg.startsWith("Cannot") ? msg : "Lost connection to the MySQL server: " + msg;
        }
        switch (e.getErrorCode()) {
            case 1062:
                if (msg.contains("uq_alumni_email")) return "An alumnus with this email already exists.";
                if (msg.contains("uq_payment_transaction")) return "This transaction ID is already recorded.";
                if (msg.contains("uq_registration_alumni_event")) return "This alumnus is already registered for this event.";
                if (msg.contains("uq_attendance_registration")) return "Attendance is already recorded for this registration.";
                return "A record with the same unique value already exists.";
            case 1451: return "This record cannot be deleted because other records depend on it.";
            case 1452: return "The selected alumnus, event or registration does not exist.";
            case 3819: case 4025: return "A value breaks a database rule (" + constraintName(msg) + ").";
            case 1048: return "A required value is missing.";
            case 1406: return "A value is too long for its column.";
            default:   return "Database error " + e.getErrorCode() + ": " + msg;
        }
    }

    private static String constraintName(String msg) {
        Matcher m = Pattern.compile("'([^']+)'").matcher(msg);
        return m.find() ? m.group(1) : "CHECK constraint";
    }
}
