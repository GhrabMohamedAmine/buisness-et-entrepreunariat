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
    private final ObjectProperty<byte[]> imageData = new SimpleObjectProperty<>();
    // Nouveau champ pour l'identifiant de visage CompreFace
    private final StringProperty faceId = new SimpleStringProperty();

    // Constructeur complet avec faceId
    public User(int id, String name, String firstName, String email, String phone,
                String role, String department, String status, String joinedDate,
                byte[] imageData, String faceId) {
        this.id.set(id);
        this.name.set(name);
        this.firstName.set(firstName);
        this.email.set(email);
        this.phone.set(phone);
        this.role.set(role);
        this.department.set(department);
        this.status.set(status);
        this.joinedDate.set(joinedDate);
        this.imageData.set(imageData);
        this.faceId.set(faceId);
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
    public byte[] getImageData() { return imageData.get(); }
    public String getFaceId() { return faceId.get(); }

    // Property getters (utiles pour les bindings JavaFX)
    public IntegerProperty idProperty() { return id; }
    public StringProperty nameProperty() { return name; }
    public StringProperty firstNameProperty() { return firstName; }
    public StringProperty emailProperty() { return email; }
    public StringProperty phoneProperty() { return phone; }
    public StringProperty roleProperty() { return role; }
    public StringProperty departmentProperty() { return department; }
    public StringProperty statusProperty() { return status; }
    public StringProperty joinedDateProperty() { return joinedDate; }
    public ObjectProperty<byte[]> imageDataProperty() { return imageData; }
    public StringProperty faceIdProperty() { return faceId; }

    // Setters
    public void setId(int id) { this.id.set(id); }
    public void setName(String name) { this.name.set(name); }
    public void setFirstName(String firstName) { this.firstName.set(firstName); }
    public void setEmail(String email) { this.email.set(email); }
    public void setPhone(String phone) { this.phone.set(phone); }
    public void setRole(String role) { this.role.set(role); }
    public void setDepartment(String department) { this.department.set(department); }
    public void setStatus(String status) { this.status.set(status); }
    public void setJoinedDate(String joinedDate) { this.joinedDate.set(joinedDate); }
    public void setImageData(byte[] imageData) { this.imageData.set(imageData); }
    public void setFaceId(String faceId) { this.faceId.set(faceId); }
}