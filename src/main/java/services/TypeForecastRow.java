package services;

public class TypeForecastRow {
    private final String type;
    private final int availableQuantity;
    private final int predictedDemand;
    private final String risk;

    public TypeForecastRow(String type, int availableQuantity, int predictedDemand, String risk) {
        this.type = type;
        this.availableQuantity = availableQuantity;
        this.predictedDemand = predictedDemand;
        this.risk = risk;
    }

    public String getType() { return type; }
    public int getAvailableQuantity() { return availableQuantity; }
    public int getPredictedDemand() { return predictedDemand; }
    public String getRisk() { return risk; }
}