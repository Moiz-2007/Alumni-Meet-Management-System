package dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** Small null-safe helpers shared by the DAO classes. */
final class Jdbc {
    private Jdbc() {}

    static void setString(PreparedStatement ps, int i, String v) throws SQLException {
        if (v == null) ps.setNull(i, Types.VARCHAR); else ps.setString(i, v);
    }
    static void setDate(PreparedStatement ps, int i, LocalDate v) throws SQLException {
        if (v == null) ps.setNull(i, Types.DATE); else ps.setObject(i, v);
    }
    static void setTime(PreparedStatement ps, int i, LocalTime v) throws SQLException {
        if (v == null) ps.setNull(i, Types.TIME); else ps.setObject(i, v);
    }
    static void setDateTime(PreparedStatement ps, int i, LocalDateTime v) throws SQLException {
        if (v == null) ps.setNull(i, Types.TIMESTAMP); else ps.setObject(i, v);
    }
    static void setInt(PreparedStatement ps, int i, Integer v) throws SQLException {
        if (v == null) ps.setNull(i, Types.INTEGER); else ps.setInt(i, v);
    }
    static Integer getInteger(ResultSet rs, String col) throws SQLException {
        int v = rs.getInt(col);
        return rs.wasNull() ? null : v;
    }
    static String like(String keyword) { return "%" + keyword.trim() + "%"; }
    static boolean hasText(String s) { return s != null && !s.isBlank(); }
}
