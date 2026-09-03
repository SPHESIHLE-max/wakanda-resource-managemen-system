import java.util.List;
import java.time.LocalDate;
import java.util.ArrayList;

// ════════════════════════════════════════════════════════════════════
//  DOMAIN — NormalUser  (extends User)
// ════════════════════════════════════════════════════════════════════
class NormalUser extends User
{
    private double                  waterTotal       = 0;
    private double                  electricityTotal = 0;
    private final List<UsageEntry>  usageEntries     = new ArrayList<>();

    public NormalUser(String email, String passwordHash, String name)
    { super(email, passwordHash, name); }

    /** Returns true if an entry already exists for the given date. */
    public boolean hasEntryForDate(LocalDate date)
    {
        return usageEntries.stream().anyMatch(e -> e.date.isEqual(date));
    }

    /**
     * Adds a usage entry only if no entry exists for that date.
     * Returns true on success, false if a duplicate date was detected.
     */
    public boolean addUsageEntry(LocalDate date, double water, double electricity)
    {
        if (hasEntryForDate(date)) return false;   // one entry per day per user
        waterTotal       += water;
        electricityTotal += electricity;
        usageEntries.add(new UsageEntry(date, water, electricity));
        return true;
    }

    /**
     * Deletes the usage entry for the given date (if it exists).
     * Returns true if an entry was found and removed.
     */
    public boolean deleteUsageEntry(LocalDate date)
    {
        UsageEntry target = usageEntries.stream()
                .filter(e -> e.date.isEqual(date))
                .findFirst().orElse(null);
        if (target == null) return false;
        waterTotal       -= target.water;
        electricityTotal -= target.electricity;
        usageEntries.remove(target);
        return true;
    }

    public List<UsageEntry> getUsageEntries()   { return usageEntries; }
    public double getWaterTotal()               { return waterTotal; }
    public double getElectricityTotal()         { return electricityTotal; }

    @Override public double getTotalUsage()     { return waterTotal + electricityTotal; }
    @Override public String getRoleLabel()      { return "User"; }
}
