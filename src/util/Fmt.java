package util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Display helpers (null-safe). */
public final class Fmt {
    private Fmt() {}
    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static String str(Object o) { return o == null ? "" : o.toString(); }
    public static String dt(LocalDateTime d) { return d == null ? "" : d.format(DT); }
}
