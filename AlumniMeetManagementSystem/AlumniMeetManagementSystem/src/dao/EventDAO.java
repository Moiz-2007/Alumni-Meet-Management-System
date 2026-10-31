package dao;

import database.DBConnection;
import model.Event;
import util.Choice;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class EventDAO {
    /** Registered = registrations that are not cancelled (they occupy a seat). */
    private static final String SELECT =
        "SELECT E.Meet_ID, E.Meet_Name, E.Meet_Date, E.Venue, E.Start_Time, E.End_Time, E.Capacity, " +
        "(SELECT COUNT(*) FROM Registration R WHERE R.Meet_ID = E.Meet_ID AND R.Registration_Status <> 'Cancelled') AS Registered " +
        "FROM Event E";

    public List<Event> findAll(String keyword) throws SQLException {
        boolean filter = Jdbc.hasText(keyword);
        String sql = SELECT + (filter
            ? " WHERE E.Meet_Name LIKE ? OR E.Venue LIKE ? OR CAST(E.Meet_Date AS CHAR) LIKE ?" : "")
            + " ORDER BY E.Meet_Date, E.Meet_ID";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            if (filter) for (int i = 1; i <= 3; i++) ps.setString(i, Jdbc.like(keyword));
            try (ResultSet rs = ps.executeQuery()) {
                List<Event> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(new Event(rs.getInt("Meet_ID"), rs.getString("Meet_Name"),
                        rs.getObject("Meet_Date", LocalDate.class), rs.getString("Venue"),
                        rs.getObject("Start_Time", LocalTime.class), rs.getObject("End_Time", LocalTime.class),
                        rs.getInt("Capacity"), rs.getInt("Registered")));
                }
                return list;
            }
        }
    }

    public void insert(Event e) throws SQLException {
        String sql = "INSERT INTO Event (Meet_Name, Meet_Date, Venue, Start_Time, End_Time, Capacity) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, e);
            ps.executeUpdate();
        }
    }

    public boolean update(Event e) throws SQLException {
        String sql = "UPDATE Event SET Meet_Name=?, Meet_Date=?, Venue=?, Start_Time=?, End_Time=?, Capacity=? WHERE Meet_ID=?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, e);
            ps.setInt(7, e.id());
            return ps.executeUpdate() > 0;
        }
    }

    private void bind(PreparedStatement ps, Event e) throws SQLException {
        ps.setString(1, e.name());
        Jdbc.setDate(ps, 2, e.date());
        Jdbc.setString(ps, 3, e.venue());
        Jdbc.setTime(ps, 4, e.startTime());
        Jdbc.setTime(ps, 5, e.endTime());
        ps.setInt(6, e.capacity());
    }

    public boolean delete(int id) throws SQLException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM Event WHERE Meet_ID = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    public int activeRegistrations(int meetId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Registration WHERE Meet_ID = ? AND Registration_Status <> 'Cancelled'";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, meetId);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
        }
    }

    public List<Choice> choices() throws SQLException {
        String sql = "SELECT Meet_ID, CONCAT(Meet_Name, ' - ', Meet_Date) AS Label FROM Event ORDER BY Meet_Date, Meet_Name";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Choice> list = new ArrayList<>();
            while (rs.next()) list.add(new Choice(rs.getInt(1), rs.getString(2)));
            return list;
        }
    }
}
