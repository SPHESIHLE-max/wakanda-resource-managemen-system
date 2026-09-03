// ════════════════════════════════════════════════════════════════════
//  SERVICE — ReportGenerator
// ════════════════════════════════════════════════════════════════════
class ReportGenerator
{
    private final ResourceManager rm;
    private final SystemLog       sysLog;

    public ReportGenerator(ResourceManager rm, SystemLog log)
    {
        this.rm     = rm;
        this.sysLog = log;
    }

    public String generateResourceReport()
    {
        sysLog.recordAction("SYSTEM", "Resource report generated");
        return rm.generateReport();
    }

    public String generateLogReport()
    {
        sysLog.recordAction("SYSTEM", "Log report exported");
        return sysLog.exportLogs();
    }
}


// ════════════════════════════════════════════════════════════════════
//  DOMAIN — Request  (NormalUser → Technician)
// ════════════════════════════════════════════════════════════════════
class Request
{
    private final NormalUser user;
    private final Technician technician;
    private final String     problem;
    private       boolean    accepted = false;

    public Request(NormalUser user, Technician tech, String problem)
    {
        this.user       = user;
        this.technician = tech;
        this.problem    = problem;
    }

    public void   accept()      { accepted = true; }
    public boolean isAccepted() { return accepted; }
    public String getProblem()  { return problem; }
}