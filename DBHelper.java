import java.sql.*;

/**
 * Optional lightweight helper to persist chat messages to a MySQL database.
 * To enable, configure DB_URL, DB_USER, DB_PASSWORD below and ensure the
 * MySQL JDBC driver is on the classpath when running the Server.
 */
public class DBHelper {
    // Set these when you want to enable DB logging
    private static final String DB_URL = ""; // e.g. "jdbc:mysql://localhost:3306/chatdb"
    private static final String DB_USER = "";
    private static final String DB_PASSWORD = "";

    private boolean configured;

    public DBHelper() {
        configured = DB_URL != null && !DB_URL.isEmpty()
                && DB_USER != null && !DB_USER.isEmpty();
        if (configured) {
            try {
                // Try to load driver (modern drivers register automatically)
                Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException e) {
                System.err.println("[DBHelper]: MySQL driver not found on classpath");
                configured = false;
            }
        }
    }

    public boolean isConfigured() { return configured; }

    public void insertMessage(String username, String message) {
        if (!configured) return;
        String sql = "INSERT INTO chat_history(username, message, timestamp) VALUES (?, ?, NOW())";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, message);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[DBHelper]: Failed to insert message - " + e.getMessage());
        }
    }
}
