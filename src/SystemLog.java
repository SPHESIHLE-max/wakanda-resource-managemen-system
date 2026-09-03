import java.util.List;
import java.util.ArrayList;
import java.io.FileWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

// ════════════════════════════════════════════════════════════════════
//  INFRASTRUCTURE — SystemLog  (append-only, encrypted stub)
// ════════════════════════════════════════════════════════════════════
class SystemLog
{
    private static final String LOG_FILE   = "system_log.txt";
    private static final String LOG_KEY    = "WKD-LOG-KEY";  // checksum prefix

    // In-memory log for the GUI
    private final List<LogEntry> entries = new ArrayList<>();

    public static class LogEntry
    {
        public final int    logID;
        public final String username;
        public final String action;
        public final String timestamp;

        LogEntry(int id, String user, String action, String ts)
        {
            this.logID     = id;
            this.username  = user;
            this.action    = action;
            this.timestamp = ts;
        }
    }

    public void recordAction(String username, String action)
    {
        String ts    = java.time.LocalDateTime.now()
                           .format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss"));
        int    id    = entries.size() + 1;
        entries.add(new LogEntry(id, username, action, ts));
        appendToFile(username, action, ts);
    }

    private void appendToFile(String username, String action, String ts)
    {
        try (FileWriter fw = new FileWriter(LOG_FILE, true))
        {
            String line = LOG_KEY + "|" + username + "|" + action + "|" + ts;
            fw.write(line + "\n");
        }
        catch (Exception e) { System.err.println("Log write error: " + e.getMessage()); }
    }

    public List<LogEntry> getEntries() { return Collections.unmodifiableList(entries); }

    public String exportLogs()
    {
        StringBuilder sb = new StringBuilder();
        for (LogEntry e : entries)
            sb.append(String.format("[%s] %s — %s%n", e.timestamp, e.username, e.action));
        return sb.toString();
    }
}