package dao;

import database.DBConnection;
import model.Alumni;
import util.Choice;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class AlumniDAO {
    private static final String SELECT =
        "SELECT Alumni_ID, First_Name, Last_Name, Gender, DOB, Email, Mobile_No, Degree, Department, " +
        "Graduation_Year, TIMESTAMPDIFF(YEAR, DOB, CURDATE()) AS Age FROM Alumni";

    public List<Alumni> findAll(String keyword) throws SQLException {
        boolean filter = Jdbc.hasText(keyword);
        String sql = SELECT + (filter
            ? " WHERE CONCAT(First_Name, ' ', Last_Name) LIKE ? OR Email LIKE ? OR Department LIKE ?"
              + " OR CAST(Graduation_Year AS CHAR) LIKE ?" : "") + " ORDER BY Alumni_ID";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            if (filter) for (int i = 1; i <= 4; i++) ps.setString(i, Jdbc.like(keyword));
            try (ResultSet rs = ps.executeQuery()) {
                List<Alumni> list = new ArrayList<>();
                while (rs.next()) list.add(map(rs));
                return list;
            }
        }
    }

    private Alumni map(ResultSet rs) throws SQLException {
        return new Alumni(rs.getInt("Alumni_ID"), rs.getString("First_Name"), rs.getString("Last_Name"),
            rs.getString("Gender"), rs.getObject("DOB", LocalDate.class), rs.getString("Email"),
            rs.getString("Mobile_No"), rs.getString("Degree"), rs.getString("Department"),
            Jdbc.getInteger(rs, "Graduation_Year"), Jdbc.getInteger(rs, "Age"));
    }

    public boolean emailExists(String email, Integer excludeId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Alumni WHERE Email = ? AND (? IS NULL OR Alumni_ID <> ?)";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            Jdbc.setInt(ps, 2, excludeId);
            Jdbc.setInt(ps, 3, excludeId);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1) > 0; }
        }
    }

    public void insert(Alumni a) throws SQLException {
        String sql = "INSERT INTO Alumni (First_Name, Last_Name, Gender, DOB, Email, Mobile_No, Degree, Department, Graduation_Year) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, a);
            ps.executeUpdate();
        }
    }

    public boolean update(Alumni a) throws SQLException {
        String sql = "UPDATE Alumni SET First_Name=?, Last_Name=?, Gender=?, DOB=?, Email=?, Mobile_No=?, "
                   + "Degree=?, Department=?, Graduation_Year=? WHERE Alumni_ID=?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, a);
            ps.setInt(10, a.id());
            return ps.executeUpdate() > 0;
        }
    }

    private void bind(PreparedStatement ps, Alumni a) throws SQLException {
        ps.setString(1, a.firstName());
        ps.setString(2, a.lastName());
        Jdbc.setString(ps, 3, a.gender());
        Jdbc.setDate(ps, 4, a.dob());
        ps.setString(5, a.email());
        Jdbc.setString(ps, 6, a.mobileNo());
        Jdbc.setString(ps, 7, a.degree());
        Jdbc.setString(ps, 8, a.department());
        Jdbc.setInt(ps, 9, a.graduationYear());
    }

    public boolean delete(int id) throws SQLException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM Alumni WHERE Alumni_ID = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    public List<Choice> choices() throws SQLException {
        String sql = "SELECT Alumni_ID, CONCAT(First_Name, ' ', Last_Name, ' (', Email, ')') AS Label FROM Alumni ORDER BY First_Name, Last_Name";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Choice> list = new ArrayList<>();
            while (rs.next()) list.add(new Choice(rs.getInt(1), rs.getString(2)));
            return list;
        }
    }
}
