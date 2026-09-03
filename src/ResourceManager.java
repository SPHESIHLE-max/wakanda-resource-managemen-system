import java.util.LinkedHashMap;
import java.util.*;

// ════════════════════════════════════════════════════════════════════
//  SERVICE — ResourceManager  implements Manageable
// ════════════════════════════════════════════════════════════════════
class ResourceManager implements Manageable
{
    private final Map<Integer, Resource> resources = new LinkedHashMap<>();
    private final SystemLog              sysLog;

    public ResourceManager(SystemLog log)
    {
        this.sysLog = log;
        // Seed demo data
        create(new WaterResource(1,       "Main Reservoir",    4200, "Good"));
        create(new ElectricityResource(2, "Grid A",            1850, 220, 1850));
        create(new WaterResource(3,       "District B Supply", 3100, "Excellent"));
        create(new ElectricityResource(4, "Solar Array",        920,  48,  920));
    }

    @Override
    public void create(Resource r)
    {
        resources.put(r.getResourceID(), r);
        sysLog.recordAction("SYSTEM", "Resource created: " + r.getResourceName());
    }

    @Override
    public Resource read(int id) { return resources.get(id); }

    @Override
    public void update(int id, Resource r)
    {
        resources.put(id, r);
        sysLog.recordAction("SYSTEM", "Resource updated: id=" + id);
    }

    @Override
    public void delete(int id)
    {
        Resource r = resources.remove(id);
        if (r != null) sysLog.recordAction("SYSTEM", "Resource deleted: " + r.getResourceName());
    }

    public Collection<Resource> getAll()                  { return resources.values(); }
    public List<Resource> search(String query)
    {
        List<Resource> result = new ArrayList<>();
        for (Resource r : resources.values())
            if (r.getResourceName().toLowerCase().contains(query.toLowerCase()))
                result.add(r);
        return result;
    }
    public List<Resource> sortByUsage()
    {
        List<Resource> sorted = new ArrayList<>(resources.values());
        sorted.sort(Comparator.comparingDouble(Resource::getUsage).reversed());
        return sorted;
    }
    public String generateReport()
    {
        StringBuilder sb = new StringBuilder("=== Resource Report ===\n");
        for (Resource r : sortByUsage())
            sb.append(String.format("[%s] %s — %.1f units%n",
                    r.getType(), r.getResourceName(), r.getUsage()));
        return sb.toString();
    }
}
