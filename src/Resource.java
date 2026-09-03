// ════════════════════════════════════════════════════════════════════
//  DOMAIN — abstract Resource
// ════════════════════════════════════════════════════════════════════
abstract class Resource
{
    private final int    resourceID;
    private       String resourceName;
    private       double usage;

    public Resource(int id, String name, double usage)
    {
        this.resourceID   = id;
        this.resourceName = name;
        this.usage        = usage;
    }

    public int    getResourceID()               { return resourceID; }
    public String getResourceName()             { return resourceName; }
    public void   setResourceName(String n)     { this.resourceName = n; }
    public double getUsage()                    { return usage; }
    public void   setUsage(double u)            { this.usage = u; }

    public abstract String getType();
}
