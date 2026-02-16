package entities;

import javafx.beans.property.*;

public class User {
    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty name = new SimpleStringProperty();
    private final StringProperty firstName = new SimpleStringProperty();
    private final StringProperty email = new SimpleStringProperty();
    private final StringProperty phone = new SimpleStringProperty();
    private final StringProperty role = new SimpleStringProperty();
    private final StringProperty department = new SimpleStringProperty();
    private final StringProperty status = new SimpleStringProperty();
    private final StringProperty joinedDate = new SimpleStringProperty();
    // NOUVEAU CHAMP
    private final StringProperty imageLink = new SimpleStringProperty();

    // Constructeur mis à jour incluant imageLink
    public User(int id, String name, String firstName, String email, String phone, String role, String department, String status, String joinedDate, String imageLink) {
        this.id.set(id);
        this.name.set(name);
        this.firstName.set(firstName);
        this.email.set(email);
        this.phone.set(phone);
        this.role.set(role);
        this.department.set(department);
        this.status.set(status);
        this.joinedDate.set(joinedDate);
        this.imageLink.set(imageLink);
    }

    // Getters
    public int getId() { return id.get(); }
    public String getName() { return name.get(); }
    public String getFirstName() { return firstName.get(); }
    public String getEmail() { return email.get(); }
    public String getPhone() { return phone.get(); }
    public String getRole() { return role.get(); }
    public String getDepartment() { return department.get(); }
    public String getStatus() { return status.get(); }
    public String getJoinedDate() { return joinedDate.get(); }
    public String getImageLink() { return imageLink.get(); } // Nouveau Getter

    // Setters
    public void setId(int id) { this.id.set(id); }
    public void setRole(String role) { this.role.set(role); }
    public void setImageLink(String imageLink) { this.imageLink.set(imageLink); }

    public void setName(String name) { this.name.set(name);}
    public void setFirstName(String firstName) {this.firstName.set(firstName);}
    public void setEmail(String email) {this.email.set(email);}
    public void setPhone(String phone) {this.phone.set(phone);}
    public void setDepartment(String department) {this.department.set(department);}
    public void setStatus(String status) {this.status.set(status);}
    public void setJoinedDate(String joinedDate) {this.joinedDate.set(joinedDate);}


}