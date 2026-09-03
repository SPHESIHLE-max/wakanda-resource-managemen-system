import java.util.List;
import java.util.ArrayList;

// ════════════════════════════════════════════════════════════════════
//  DOMAIN — Technician  (extends User)
// ════════════════════════════════════════════════════════════════════
class Technician extends User
{
    private final String       techID;
    private       boolean      approved     = false;
    private final List<String> workLogs     = new ArrayList<>();
    private final List<String> userRequests = new ArrayList<>();

    public Technician(String email, String passwordHash, String techID, String name)
    {
        super(email, passwordHash, name);
        this.techID = techID;
    }

    public String       getTechID()                  { return techID; }
    public boolean      isApproved()                 { return approved; }
    public void         approve()                    { approved = true; }
    public List<String> getWorkLogs()               { return workLogs; }
    public void         addLog(String entry)         { workLogs.add(entry); }
    public List<String> getUserRequests()           { return userRequests; }
    public void         addUserRequest(String req)  { userRequests.add(req); }

    @Override public String getRoleLabel() { return "Technician"; }
}