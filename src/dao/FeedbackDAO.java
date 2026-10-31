package dao;

import database.DBConnection;
import model.Feedback;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class FeedbackDAO {
    private static final String SELECT =
        "SELECT F.Feedback_ID, F.Registration_ID, F.Rating, F.Comments, F.Feedback_Date, " +
        "CONCAT(A.First_Name, ' ', A.Last_Name) AS Alumni_Name, E.Meet_Name " +
        "FROM Feedback F JOIN Registration R ON R.Registration_ID = F.Registration_ID " +
        "JOIN Alumni A ON A.Alumni_ID = R.Alumni_ID JOIN Event E ON E.Meet_ID = R.Meet_ID";

    public List<Feedback> findAll(String keyword) throws SQLException {
        boolean filter = Jdbc.hasText(keyword);
        String sql = SELECT + (filter
            ? " WHERE F.Comments LIKE ? OR CONCAT(A.First_Name, ' ', A.Last_Name) LIKE ? OR E.Meet_Name LIKE ?"
              + " OR CAST(F.Rating AS CHAR) = ?" : "")
            + " ORDER BY F.Feedback_ID";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            if (filter) {
                for (int i = 1; i <= 3; i++) ps.setString(i, Jdbc.like(keyword));
                ps.setString(4, keyword.trim());
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<Feedback> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(new Feedback(rs.getInt("Feedback_ID"), rs.getInt("Registration_ID"), rs.getInt("Rating"),
                        rs.getString("Comments"), rs.getObject("Feedback_Date", LocalDate.class),
                        rs.getString("Alumni_Name"), rs.getString("Meet_Name")));
                }
                return list;
            }
        }
    }

    public void insert(Feedback f) throws SQLException {
        String sql = "INSERT INTO Feedback (Registration_ID, Rating, Comments, Feedback_Date) VALUES (?, ?, ?, ?)";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, f.registrationId());
            ps.setInt(2, f.rating());
            Jdbc.setString(ps, 3, f.comments());
            Jdbc.setDate(ps, 4, f.date());
            ps.executeUpdate();
        }
    }

    public boolean update(Feedback f) throws SQLException {
        String sql = "UPDATE Feedback SET Rating = ?, Comments = ?, Feedback_Date = ? WHERE Feedback_ID = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, f.rating());
            Jdbc.setString(ps, 2, f.comments());
            Jdbc.setDate(ps, 3, f.date());
            ps.setInt(4, f.id());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM Feedback WHERE Feedback_ID = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }
}
