// ════════════════════════════════════════════════════════════════════
//  DOMAIN — abstract User
// ════════════════════════════════════════════════════════════════════
public abstract class User
{
    private final String email;
    private final String name;
    private       String passwordHash;
    private       boolean active = true;

    public User(String email, String passwordHash, String name)
    {
        this.email        = email;
        this.passwordHash = passwordHash;
        this.name         = name;
    }

    public String  getEmail()                    { return email; }
    public String  getName()                     { return name; }
    public String  getRawHash()                  { return passwordHash; }
    public boolean isActive()                    { return active; }
    public void    setActive(boolean v)          { this.active = v; }
    public boolean checkPassword(String plain)   { return PasswordUtil.verify(plain, passwordHash); }
    public void    setPassword(String plain)     { this.passwordHash = PasswordUtil.hash(plain); }
    public double  getTotalUsage()               { return 0; }

    public abstract String getRoleLabel();
}
