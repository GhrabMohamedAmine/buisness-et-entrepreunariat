package entities;


public class Resource {

    private int id;
    private String name;
    private int code;
    private double unitcost;
    private String type;
    private int quantity;
    private double avquant;

    public Resource() {}

    public Resource(String name, int code, double unitcost, String type, int quantity, double avquant) {
        this.name = name;
        this.code = code;
        this.unitcost = unitcost;
        this.type = type;
        this.quantity = quantity;
        this.avquant = avquant;
    }

    public Resource(int id, String name, String type, int quantity, int code, double unitcost, double avquant) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.unitcost = unitcost;
        this.type = type;
        this.quantity = quantity;
        this.avquant = avquant;

    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }

    public double getUnitcost() { return unitcost; }
    public void setUnitcost(double unitcost) { this.unitcost = unitcost; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getAvquant() { return avquant; }
    public void setAvquant(double avquant) { this.avquant = avquant; }

    @Override
    public String toString() {
        return name;
    }
}
