package dao;

import database.DBConnection;
import model.Payment;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PaymentDAO {
    private static final String SELECT =
        "SELECT P.Payment_ID, P.Registration_ID, P.Transaction_ID, P.Amount, P.Payment_Mode, P.Payment_Date, P.Payment_Status, " +
        "CONCAT(A.First_Name, ' ', A.Last_Name) AS Alumni_Name, E.Meet_Name " +
        "FROM Payment P JOIN Registration R ON R.Registration_ID = P.Registration_ID " +
        "JOIN Alumni A ON A.Alumni_ID = R.Alumni_ID JOIN Event E ON E.Meet_ID = R.Meet_ID";

    public List<Payment> findAll(String keyword) throws SQLException {
        boolean filter = Jdbc.hasText(keyword);
        String sql = SELECT + (filter
            ? " WHERE P.Transaction_ID LIKE ? OR P.Payment_Status LIKE ? OR CAST(P.Registration_ID AS CHAR) LIKE ?"
              + " OR CONCAT(A.First_Name, ' ', A.Last_Name) LIKE ? OR E.Meet_Name LIKE ?" : "")
            + " ORDER BY P.Payment_ID";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            if (filter) for (int i = 1; i <= 5; i++) ps.setString(i, Jdbc.like(keyword));
            try (ResultSet rs = ps.executeQuery()) {
                List<Payment> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(new Payment(rs.getInt("Payment_ID"), rs.getInt("Registration_ID"), rs.getString("Transaction_ID"),
                        rs.getBigDecimal("Amount"), rs.getString("Payment_Mode"), rs.getObject("Payment_Date", LocalDate.class),
                        rs.getString("Payment_Status"), rs.getString("Alumni_Name"), rs.getString("Meet_Name")));
                }
                return list;
            }
        }
    }

    public boolean transactionExists(String transactionId, Integer excludeId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Payment WHERE Transaction_ID = ? AND (? IS NULL OR Payment_ID <> ?)";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, transactionId);
            Jdbc.setInt(ps, 2, excludeId);
            Jdbc.setInt(ps, 3, excludeId);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1) > 0; }
        }
    }

    public void insert(Payment p) throws SQLException {
        String sql = "INSERT INTO Payment (Registration_ID, Transaction_ID, Amount, Payment_Mode, Payment_Date, Payment_Status) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, p.registrationId());
            ps.setString(2, p.transactionId());
            ps.setBigDecimal(3, p.amount());
            ps.setString(4, p.mode());
            Jdbc.setDate(ps, 5, p.date());
            ps.setString(6, p.status());
            ps.executeUpdate();
        }
    }

    public boolean update(Payment p) throws SQLException {
        String sql = "UPDATE Payment SET Transaction_ID=?, Amount=?, Payment_Mode=?, Payment_Date=?, Payment_Status=? WHERE Payment_ID=?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, p.transactionId());
            ps.setBigDecimal(2, p.amount());
            ps.setString(3, p.mode());
            Jdbc.setDate(ps, 4, p.date());
            ps.setString(5, p.status());
            ps.setInt(6, p.id());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM Payment WHERE Payment_ID = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }
}
