package tezfx.model;


import javafx.beans.property.*;
import javafx.beans.value.ObservableValue;

import java.sql.Date;

public class Project {
    // Fields matching your phpMyAdmin columns
    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty name = new SimpleStringProperty();
    private final StringProperty description = new SimpleStringProperty();
    private final IntegerProperty progress = new SimpleIntegerProperty();
    private final DoubleProperty budget = new SimpleDoubleProperty();
    private final StringProperty startDate = new SimpleStringProperty();
    private final StringProperty endDate = new SimpleStringProperty();
    private final IntegerProperty assignedTo = new SimpleIntegerProperty();
    private final IntegerProperty createdby = new SimpleIntegerProperty();

    // Constructor for loading from Database (includes ID)
    public Project(int id, String name, String description, int progress, double budget, String startDateva, String enddateva, int assignedTo, int createdby) {
        this.id.set(id);
        this.name.set(name);
        this.description.set(description);
        this.progress.set(progress);
        this.budget.set(budget);
        this.startDate.set(startDateva != null ? startDateva : "");
        this.endDate.set(enddateva != null ? enddateva : "");
        this.assignedTo.set(assignedTo);
        this.createdby.set(createdby);
    }

    // Constructor for creating NEW projects (ID is auto-incremented by SQL)
    public Project(String name, String description, int progress, double budget, String startDateva, String enddateva, int assignedTo, int createdby) {
        this.name.set(name);
        this.description.set(description);
        this.progress.set(progress);
        this.budget.set(budget);
        this.startDate.set(startDateva != null ? startDateva: "");
        this.endDate.set(enddateva != null ? enddateva : "");
        this.assignedTo.set(assignedTo);
        this.createdby.set(createdby);

    }

    // --- Property Methods (Required for JavaFX Data Binding) ---
    public IntegerProperty idProperty() { return id; }
    public StringProperty nameProperty() { return name; }
    public StringProperty descriptionProperty() { return description; }
    public IntegerProperty progressProperty() { return progress; }
    public DoubleProperty budgetProperty() { return budget; }
    public StringProperty startDateProperty() { return startDate; }
    public StringProperty endDateProperty() { return endDate; }
    public IntegerProperty assignedToProperty() { return assignedTo; }
    public ObservableValue<Number> createdByProperty() { return createdby; }

    // --- Standard Getters (Useful for Logic) ---
    public int getId() { return id.get(); }
    public String getName() { return name.get(); }
    public String getDescription() { return description.get(); }
    public int getProgress() { return progress.get(); }
    public double getBudget() { return budget.get(); }
    public String getStartDate() { return startDate.get(); }
    public String getEndDate() { return endDate.get(); }
    public int getCreatedby() { return createdby.get(); }
    public int getAssignedTo() { return assignedTo.get(); }


}