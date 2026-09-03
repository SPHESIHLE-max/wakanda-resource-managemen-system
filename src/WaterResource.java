
// ════════════════════════════════════════════════════════════════════
//  DOMAIN — WaterResource
// ════════════════════════════════════════════════════════════════════
class WaterResource extends Resource
{
    private String waterQuality;

    public WaterResource(int id, String name, double usage, String quality)
    {
        super(id, name, usage);
        this.waterQuality = quality;
    }

    public String getWaterQuality()           { return waterQuality; }
    public void   setWaterQuality(String q)   { this.waterQuality = q; }
    public void   monitorWater()              { /* monitoring logic */ }

    @Override public String getType() { return "Water"; }
}