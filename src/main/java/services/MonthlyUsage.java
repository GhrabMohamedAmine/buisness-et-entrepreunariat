package services;

public class MonthlyUsage {
    private final String month;   // "2026-02"
    private final int quantity;

    public MonthlyUsage(String month, int quantity) {
        this.month = month;
        this.quantity = quantity;
    }

    public String getMonth() { return month; }
    public int getQuantity() { return quantity; }
}