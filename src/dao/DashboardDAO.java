package dao;

import database.DBConnection;
import model.DashboardStats;

import java.sql.*;

public class DashboardDAO {
    /** Every number comes from a live COUNT(*) query. An empty database simply returns zeros. */
    public DashboardStats load() throws SQLException {
        String sql = "SELECT (SELECT COUNT(*) FROM Alumni), (SELECT COUNT(*) FROM Event), (SELECT COUNT(*) FROM Registration), "
                   + "(SELECT COUNT(*) FROM Attendance), (SELECT COUNT(*) FROM Payment WHERE Payment_Status = 'Paid'), "
                   + "(SELECT COUNT(*) FROM Feedback)";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return new DashboardStats(rs.getLong(1), rs.getLong(2), rs.getLong(3), rs.getLong(4), rs.getLong(5), rs.getLong(6));
        }
    }
}
