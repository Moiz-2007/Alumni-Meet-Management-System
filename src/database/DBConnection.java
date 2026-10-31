package database;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Single place where JDBC connections are created.
 * Settings come from config/db.properties (not committed to Git) and can be
 * overridden with environment variables ALUMNI_DB_URL, ALUMNI_DB_USER, ALUMNI_DB_PASSWORD.
 * The password is never printed or logged.
 */
public final class DBConnection {
    private DBConnection() {}

    private static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3306/AlumniMeetAppDB?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8";
    private static final String CONFIG_FILE = "config/db.properties";

    private static Properties loadConfig() {
        Properties p = new Properties();
        String path = System.getenv("ALUMNI_DB_CONFIG");
        Path file = Paths.get(path != null && !path.isBlank() ? path : CONFIG_FILE);
        if (Files.isReadable(file)) {
            try (InputStream in = Files.newInputStream(file)) { p.load(in); }
            catch (IOException ignored) { /* fall back to defaults / environment */ }
        }
        return p;
    }

    private static String setting(Properties p, String envName, String key, String fallback) {
        String env = System.getenv(envName);
        if (env != null && !env.isBlank()) return env;
        return p.getProperty(key, fallback);
    }

    /** Opens a new connection. Callers must close it (use try-with-resources). */
    public static Connection getConnection() throws SQLException {
        Properties p = loadConfig();
        String url = setting(p, "ALUMNI_DB_URL", "db.url", DEFAULT_URL);
        String user = setting(p, "ALUMNI_DB_USER", "db.user", "root");
        String password = setting(p, "ALUMNI_DB_PASSWORD", "db.password", "");
        try {
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            throw new SQLException(explain(e, user), "08001", e.getErrorCode(), e);
        }
    }

    /** Human-readable description of the target (never includes the password). */
    public static String describe() {
        Properties p = loadConfig();
        String url = setting(p, "ALUMNI_DB_URL", "db.url", DEFAULT_URL);
        int q = url.indexOf('?');
        return (q > 0 ? url.substring(0, q) : url) + " as " + setting(p, "ALUMNI_DB_USER", "db.user", "root");
    }

    private static String explain(SQLException e, String user) {
        String msg = String.valueOf(e.getMessage());
        if (msg.contains("No suitable driver"))
            return "Cannot find the MySQL driver. Put the MySQL Connector/J JAR in the lib folder and include lib/* on the classpath.";
        if (e.getErrorCode() == 1045)
            return "Cannot log in: wrong MySQL username or password for '" + user + "'. Check config/db.properties.";
        if (e.getErrorCode() == 1049)
            return "Cannot find database AlumniMeetAppDB. Run database/schema.sql first.";
        return "Cannot connect to MySQL. Is the server running and is db.url correct? (" + msg + ")";
    }
}
