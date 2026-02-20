package entities;

import javafx.beans.property.*;

public class Task {
    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty title = new SimpleStringProperty();
    private final StringProperty description = new SimpleStringProperty();
    private final StringProperty status = new SimpleStringProperty();
    private final StringProperty priority = new SimpleStringProperty(); // ENUM: 'LOW', 'MEDIUM', 'HIGH'
    private final StringProperty startDate = new SimpleStringProperty();
    private final StringProperty dueDate = new SimpleStringProperty();
    private final IntegerProperty projectId = new SimpleIntegerProperty();
    private final IntegerProperty assignedTo = new SimpleIntegerProperty();
    private final IntegerProperty createdby = new SimpleIntegerProperty();
    private String assignedToName;

    // Constructor for creating a NEW task
    public Task(String title, String description, String status, String priority,
                String startDate, String dueDate, int projectId, int assignedTo ,int createdby) {
        this.title.set(title);
        this.description.set(description);
        this.status.set(status);
        this.priority.set(priority);
        this.startDate.set(startDate);
        this.dueDate.set(dueDate);
        this.projectId.set(projectId);
        this.assignedTo.set(assignedTo);
        this.createdby.set(createdby);
    }



    // Getters
    public String getTitle() { return title.get(); }
    public String getDescription() { return description.get(); }
    public String getPriority() { return priority.get(); }
    public String getStartDate() { return startDate.get(); }
    public String getDueDate() { return dueDate.get(); }
    public int getId() { return id.get(); }
    public int getCreatedby() { return createdby.get(); }
    public String getAssignedToName() {
        return (assignedToName != null && !assignedToName.isEmpty())
                ? assignedToName
                : "Unassigned";
    }
    public void setAssignedToName(String name) {
        this.assignedToName = name;
    }

    public String getStatus() { return status.get(); }
    public int getProjectId() { return projectId.get(); }
    public int getAssignedTo() { return assignedTo.get(); }


    public void setId(int id) { this.id.set(id); }
    public void setStatus(String status) { this.status.set(status); }
    public void setAssignedTo(int assignedTo) { this.assignedTo.set(assignedTo); }
    public void setCreatedby(int createdby) { this.createdby.set(createdby); }


}
