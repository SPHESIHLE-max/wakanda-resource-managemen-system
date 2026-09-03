 // ════════════════════════════════════════════════════════════════════
//  DOMAIN — ElectricityResource
// ════════════════════════════════════════════════════════════════════
class ElectricityResource extends Resource
{
    private double voltage;
    private double powerConsumption;

    public ElectricityResource(int id, String name, double usage,
                               double voltage, double powerConsumption)
    {
        super(id, name, usage);
        this.voltage          = voltage;
        this.powerConsumption = powerConsumption;
    }

    public double getVoltage()                    { return voltage; }
    public double getPowerConsumption()           { return powerConsumption; }
    public void   setPowerConsumption(double v)   { this.powerConsumption = v; }

    @Override public String getType() { return "Electricity"; }
}