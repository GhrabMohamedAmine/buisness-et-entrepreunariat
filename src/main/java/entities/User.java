package entities;

public class User {
    private int id;
    private String firstName;
    private String lastName;
    private String role;

    public User(int id, String firstName, String lastName) {
        this(id, firstName, lastName, "");
    }

    public User(int id, String firstName, String lastName, String role) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role == null ? "" : role;
    }

    public int getId() { return id; }
    public String getRole() { return role; }
    public boolean isManager() { return role != null && role.toUpperCase().contains("MANAGER"); }

    // This is what the ComboBox uses to display the text
    public String getFullName() {
        return firstName + " " + lastName;
    }

    @Override
    public String toString() {
        return getFullName();
    }
}
