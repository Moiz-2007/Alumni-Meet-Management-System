package dao;

import database.DBConnection;
import model.Registration;
import util.Choice;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class RegistrationDAO {
    private static final String SELECT =
        "SELECT R.Registration_ID, R.Alumni_ID, R.Meet_ID, R.Registration_Date, R.Registration_Status, " +
        "CONCAT(A.First_Name, ' ', A.Last_Name) AS Alumni_Name, E.Meet_Name " +
        "FROM Registration R JOIN Alumni A ON A.Alumni_ID = R.Alumni_ID JOIN Event E ON E.Meet_ID = R.Meet_ID";

    public List<Registration> findAll(String keyword) throws SQLException {
        boolean filter = Jdbc.hasText(keyword);
        String sql = SELECT + (filter
            ? " WHERE CONCAT(A.First_Name, ' ', A.Last_Name) LIKE ? OR E.Meet_Name LIKE ?"
              + " OR R.Registration_Status LIKE ? OR CAST(R.Registration_ID AS CHAR) LIKE ?" : "")
            + " ORDER BY R.Registration_ID";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            if (filter) for (int i = 1; i <= 4; i++) ps.setString(i, Jdbc.like(keyword));
            try (ResultSet rs = ps.executeQuery()) {
                List<Registration> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(new Registration(rs.getInt("Registration_ID"), rs.getInt("Alumni_ID"), rs.getInt("Meet_ID"),
                        rs.getObject("Registration_Date", LocalDate.class), rs.getString("Registration_Status"),
                        rs.getString("Alumni_Name"), rs.getString("Meet_Name")));
                }
                return list;
            }
        }
    }

    // ---- Methods that take a Connection so the SERVICE can run them in one transaction ----

    /** Locks the event row (SELECT ... FOR UPDATE) and returns its capacity, or null if it does not exist. */
    public Integer lockEventCapacity(Connection c, int meetId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT Capacity FROM Event WHERE Meet_ID = ? FOR UPDATE")) {
            ps.setInt(1, meetId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt(1) : null; }
        }
    }

    public int countActive(Connection c, int meetId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Registration WHERE Meet_ID = ? AND Registration_Status <> 'Cancelled'";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, meetId);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
        }
    }

    public void insert(Connection c, Registration r) throws SQLException {
        String sql = "INSERT INTO Registration (Alumni_ID, Meet_ID, Registration_Date, Registration_Status) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, r.alumniId());
            ps.setInt(2, r.meetId());
            Jdbc.setDate(ps, 3, r.date());
            ps.setString(4, r.status());
            ps.executeUpdate();
        }
    }

    /** Reads (and row-locks) one registration; alumni/event names are not loaded. */
    public Registration findForUpdate(Connection c, int id) throws SQLException {
        String sql = "SELECT Registration_ID, Alumni_ID, Meet_ID, Registration_Date, Registration_Status FROM Registration WHERE Registration_ID = ? FOR UPDATE";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new Registration(rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getObject(4, LocalDate.class), rs.getString(5), null, null);
            }
        }
    }

    public void updateStatus(Connection c, int id, String status) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE Registration SET Registration_Status = ? WHERE Registration_ID = ?")) {
            ps.setString(1, status);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    // ---- Simple single-statement methods ----

    public boolean delete(int id) throws SQLException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM Registration WHERE Registration_ID = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    public String statusOf(int registrationId) throws SQLException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT Registration_Status FROM Registration WHERE Registration_ID = ?")) {
            ps.setInt(1, registrationId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getString(1) : null; }
        }
    }

    /** Labels such as "#3 - Priya Patel -> Networking Lunch" for the Attendance/Payment/Feedback forms. */
    public List<Choice> choices() throws SQLException {
        String sql = "SELECT R.Registration_ID, CONCAT('#', R.Registration_ID, ' - ', A.First_Name, ' ', A.Last_Name, ' -> ', E.Meet_Name, ' [', R.Registration_Status, ']') AS Label "
                   + "FROM Registration R JOIN Alumni A ON A.Alumni_ID = R.Alumni_ID JOIN Event E ON E.Meet_ID = R.Meet_ID ORDER BY R.Registration_ID";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Choice> list = new ArrayList<>();
            while (rs.next()) list.add(new Choice(rs.getInt(1), rs.getString(2)));
            return list;
        }
    }
}
