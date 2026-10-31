package util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

/** Input validation helpers. Database constraints remain the final enforcement layer. */
public final class Validator {
    private Validator() {}

    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm[:ss]");

    public static String required(String v, String field) {
        if (v == null || v.trim().isEmpty()) throw new ServiceException(field + " is required.");
        return v.trim();
    }

    public static String optional(String v) {
        return (v == null || v.trim().isEmpty()) ? null : v.trim();
    }

    public static void maxLen(String v, int max, String field) {
        if (v != null && v.length() > max) throw new ServiceException(field + " must be at most " + max + " characters.");
    }

    public static String email(String v) {
        String e = required(v, "Email");
        maxLen(e, 150, "Email");
        if (!EMAIL.matcher(e).matches()) throw new ServiceException("Email format is invalid (example: name@example.com).");
        return e;
    }

    public static String mobile(String v) {
        String m = optional(v);
        if (m == null) return null;
        String digits = m.replaceAll("[\\s-]", "");
        if (!digits.matches("\\+?[0-9]{7,14}")) throw new ServiceException("Mobile number must contain 7 to 14 digits.");
        return digits;
    }

    public static int integer(String v, String field) {
        try { return Integer.parseInt(required(v, field)); }
        catch (NumberFormatException e) { throw new ServiceException(field + " must be a whole number."); }
    }

    public static LocalDate date(String v, String field) {
        try { return LocalDate.parse(required(v, field)); }
        catch (DateTimeParseException e) { throw new ServiceException(field + " must be a date in yyyy-MM-dd format."); }
    }

    public static LocalTime time(String v, String field) {
        try { return LocalTime.parse(required(v, field)); }
        catch (DateTimeParseException e) { throw new ServiceException(field + " must be a time in HH:mm format (24-hour, e.g. 09:30)."); }
    }

    public static LocalDateTime dateTime(String v, String field) {
        try { return LocalDateTime.parse(required(v, field), DATE_TIME); }
        catch (DateTimeParseException e) { throw new ServiceException(field + " must be in yyyy-MM-dd HH:mm format."); }
    }

    /** Parses a money amount that fits DECIMAL(8,2). */
    public static BigDecimal amount(String v, String field) {
        BigDecimal a;
        try { a = new BigDecimal(required(v, field)); }
        catch (NumberFormatException e) { throw new ServiceException(field + " must be a number."); }
        if (a.signum() < 0) throw new ServiceException(field + " cannot be negative.");
        if (a.scale() > 2) throw new ServiceException(field + " can have at most 2 decimal places.");
        if (a.compareTo(new BigDecimal("999999.99")) > 0) throw new ServiceException(field + " is too large (max 999999.99).");
        return a;
    }
}
