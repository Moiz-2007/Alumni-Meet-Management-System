package dao;

import database.DBConnection;
import model.Attendance;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AttendanceDAO {
    private static final String SELECT =
        "SELECT T.Attendance_ID, T.Registration_ID, T.Attendance_Status, T.Check_In_Time, T.Check_Out_Time, " +
        "CONCAT(A.First_Name, ' ', A.Last_Name) AS Alumni_Name, E.Meet_Name " +
        "FROM Attendance T JOIN Registration R ON R.Registration_ID = T.Registration_ID " +
        "JOIN Alumni A ON A.Alumni_ID = R.Alumni_ID JOIN Event E ON E.Meet_ID = R.Meet_ID";

    public List<Attendance> findAll(String keyword) throws SQLException {
        boolean filter = Jdbc.hasText(keyword);
        String sql = SELECT + (filter
            ? " WHERE CONCAT(A.First_Name, ' ', A.Last_Name) LIKE ? OR E.Meet_Name LIKE ? OR T.Attendance_Status LIKE ?" : "")
            + " ORDER BY T.Attendance_ID";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            if (filter) for (int i = 1; i <= 3; i++) ps.setString(i, Jdbc.like(keyword));
            try (ResultSet rs = ps.executeQuery()) {
                List<Attendance> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(new Attendance(rs.getInt("Attendance_ID"), rs.getInt("Registration_ID"),
                        rs.getString("Attendance_Status"), rs.getObject("Check_In_Time", LocalDateTime.class),
                        rs.getObject("Check_Out_Time", LocalDateTime.class), rs.getString("Alumni_Name"), rs.getString("Meet_Name")));
                }
                return list;
            }
        }
    }

    public void insert(Attendance a) throws SQLException {
        String sql = "INSERT INTO Attendance (Registration_ID, Attendance_Status, Check_In_Time, Check_Out_Time) VALUES (?, ?, ?, ?)";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, a.registrationId());
            ps.setString(2, a.status());
            Jdbc.setDateTime(ps, 3, a.checkIn());
            Jdbc.setDateTime(ps, 4, a.checkOut());
            ps.executeUpdate();
        }
    }

    public boolean update(Attendance a) throws SQLException {
        String sql = "UPDATE Attendance SET Attendance_Status = ?, Check_In_Time = ?, Check_Out_Time = ? WHERE Attendance_ID = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, a.status());
            Jdbc.setDateTime(ps, 2, a.checkIn());
            Jdbc.setDateTime(ps, 3, a.checkOut());
            ps.setInt(4, a.id());
            return ps.executeUpdate() > 0;
        }
    }

    /** Uses the MySQL server clock (NOW()) so the timestamp is stored by the database itself. */
    public boolean checkInNow(int id) throws SQLException {
        String sql = "UPDATE Attendance SET Attendance_Status = 'Present', Check_In_Time = NOW(), Check_Out_Time = NULL WHERE Attendance_ID = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    /** Only works when the person has already checked in. */
    public boolean checkOutNow(int id) throws SQLException {
        String sql = "UPDATE Attendance SET Check_Out_Time = NOW() WHERE Attendance_ID = ? AND Check_In_Time IS NOT NULL";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM Attendance WHERE Attendance_ID = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }
}
