//  DOMAIN — Administrator  (extends User)
// ════════════════════════════════════════════════════════════════════
class Administrator extends User
{
    private final String adminID;

    public Administrator(String email, String passwordHash, String adminID, String name)
    {
        super(email, passwordHash, name);
        this.adminID = adminID;
    }

    public String getAdminID() { return adminID; }

    public boolean login(String id, String email, String plain)
    {
        return this.adminID.equals(id)
            && this.getEmail().equals(email)
            && this.checkPassword(plain);
    }

    public void manageUsers()    { /* RBAC-guarded entry point */ }
    public void viewLogs()       { /* RBAC-guarded entry point */ }
    public void generateReports(){ /* RBAC-guarded entry point */ }

    @Override public String getRoleLabel() { return "Admin"; }
}