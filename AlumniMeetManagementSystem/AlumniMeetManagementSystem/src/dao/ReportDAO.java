package dao;

import database.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** Fixed, read-only report queries. They contain no user input, so there is nothing to inject. */
public class ReportDAO {
    public record Report(String title, String sql) {
        @Override public String toString() { return title; }
    }
    public record QueryResult(List<String> columns, List<Object[]> rows) {}

    private static final String NAME = "CONCAT(A.First_Name, ' ', A.Last_Name)";

    public static final List<Report> REPORTS = List.of(
        new Report("1. Alumni registered for each event",
            "SELECT E.Meet_Name, E.Meet_Date, " + NAME + " AS Alumni, A.Email, R.Registration_Status\n" +
            "FROM Event E\n" +
            "JOIN Registration R ON R.Meet_ID = E.Meet_ID\n" +
            "JOIN Alumni A ON A.Alumni_ID = R.Alumni_ID\n" +
            "ORDER BY E.Meet_Date, E.Meet_Name, A.First_Name"),
        new Report("2. Registration count per event",
            "SELECT E.Meet_ID, E.Meet_Name, COUNT(R.Registration_ID) AS Registrations\n" +
            "FROM Event E\n" +
            "LEFT JOIN Registration R ON R.Meet_ID = E.Meet_ID\n" +
            "GROUP BY E.Meet_ID, E.Meet_Name\n" +
            "ORDER BY Registrations DESC, E.Meet_Name"),
        new Report("3. Available capacity per event",
            "SELECT E.Meet_Name, E.Capacity, COUNT(R.Registration_ID) AS Seats_Taken,\n" +
            "       E.Capacity - COUNT(R.Registration_ID) AS Seats_Available\n" +
            "FROM Event E\n" +
            "LEFT JOIN Registration R ON R.Meet_ID = E.Meet_ID AND R.Registration_Status <> 'Cancelled'\n" +
            "GROUP BY E.Meet_ID, E.Meet_Name, E.Capacity\n" +
            "ORDER BY Seats_Available"),
        new Report("4. Attendance summary by event",
            "SELECT E.Meet_Name, COUNT(T.Attendance_ID) AS Attendance_Records,\n" +
            "       COALESCE(SUM(T.Attendance_Status = 'Present'), 0) AS Present,\n" +
            "       COALESCE(SUM(T.Attendance_Status = 'Absent'), 0) AS Absent\n" +
            "FROM Event E\n" +
            "LEFT JOIN Registration R ON R.Meet_ID = E.Meet_ID\n" +
            "LEFT JOIN Attendance T ON T.Registration_ID = R.Registration_ID\n" +
            "GROUP BY E.Meet_ID, E.Meet_Name\n" +
            "ORDER BY E.Meet_Name"),
        new Report("5. Payment summary by event",
            "SELECT E.Meet_Name, COUNT(P.Payment_ID) AS Payments,\n" +
            "       COALESCE(SUM(CASE WHEN P.Payment_Status = 'Paid' THEN P.Amount END), 0) AS Total_Paid,\n" +
            "       COALESCE(SUM(CASE WHEN P.Payment_Status = 'Pending' THEN P.Amount END), 0) AS Total_Pending\n" +
            "FROM Event E\n" +
            "LEFT JOIN Registration R ON R.Meet_ID = E.Meet_ID\n" +
            "LEFT JOIN Payment P ON P.Registration_ID = R.Registration_ID\n" +
            "GROUP BY E.Meet_ID, E.Meet_Name\n" +
            "ORDER BY Total_Paid DESC"),
        new Report("6. Feedback ratings and average rating per event",
            "SELECT E.Meet_Name, COUNT(F.Feedback_ID) AS Feedback_Count,\n" +
            "       ROUND(AVG(F.Rating), 2) AS Average_Rating, MIN(F.Rating) AS Lowest, MAX(F.Rating) AS Highest\n" +
            "FROM Event E\n" +
            "LEFT JOIN Registration R ON R.Meet_ID = E.Meet_ID\n" +
            "LEFT JOIN Feedback F ON F.Registration_ID = R.Registration_ID\n" +
            "GROUP BY E.Meet_ID, E.Meet_Name\n" +
            "ORDER BY Average_Rating DESC"),
        new Report("7. Alumni who have not registered for any event",
            "SELECT A.Alumni_ID, " + NAME + " AS Alumni, A.Email, A.Department, A.Graduation_Year\n" +
            "FROM Alumni A\n" +
            "WHERE NOT EXISTS (SELECT 1 FROM Registration R WHERE R.Alumni_ID = A.Alumni_ID)\n" +
            "ORDER BY A.Alumni_ID"),
        new Report("8. Registrations with pending payments",
            "SELECT R.Registration_ID, " + NAME + " AS Alumni, E.Meet_Name,\n" +
            "       COALESCE(P.Payment_Status, 'No payment recorded') AS Payment_State, P.Transaction_ID, P.Amount\n" +
            "FROM Registration R\n" +
            "JOIN Alumni A ON A.Alumni_ID = R.Alumni_ID\n" +
            "JOIN Event E ON E.Meet_ID = R.Meet_ID\n" +
            "LEFT JOIN Payment P ON P.Registration_ID = R.Registration_ID\n" +
            "WHERE R.Registration_Status <> 'Cancelled'\n" +
            "  AND (P.Payment_ID IS NULL OR P.Payment_Status = 'Pending')\n" +
            "ORDER BY R.Registration_ID")
    );

    public QueryResult run(String sql) throws SQLException {
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            ResultSetMetaData md = rs.getMetaData();
            int n = md.getColumnCount();
            List<String> cols = new ArrayList<>();
            for (int i = 1; i <= n; i++) cols.add(md.getColumnLabel(i));
            List<Object[]> rows = new ArrayList<>();
            while (rs.next()) {
                Object[] row = new Object[n];
                for (int i = 0; i < n; i++) row[i] = rs.getObject(i + 1);
                rows.add(row);
            }
            return new QueryResult(cols, rows);
        }
    }
}
