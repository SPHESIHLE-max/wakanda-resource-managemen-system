import java.time.*;



// ════════════════════════════════════════════════════════════════════
//  DOMAIN — UsageEntry
// ════════════════════════════════════════════════════════════════════
class UsageEntry
{
    final LocalDate date;
    final double    water;
    final double    electricity;

    UsageEntry(LocalDate date, double water, double electricity)
    {
        this.date        = date;
        this.water       = water;
        this.electricity = electricity;
    }
}