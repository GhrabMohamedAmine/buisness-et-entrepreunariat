package com.example.testp1.entities;

import java.time.LocalDate;

public class ProjectBudget {
    private int id;
    private String name;
    private double totalBudget;
    private double actualSpend;
    private String status;
    private LocalDate dueDate;
    private String description;
    private int projectId;

    public ProjectBudget() {}

    public ProjectBudget(String name, double totalBudget, double actualSpend, String status, LocalDate dueDate, int projectId) {
        this.name = name;
        this.totalBudget = totalBudget;
        this.actualSpend = actualSpend;
        this.status = status;
        this.dueDate = dueDate;
        this.projectId = projectId;
    }

    public ProjectBudget(int id, String name, double totalBudget, double actualSpend, String status, LocalDate dueDate, int projectId) {
        this(name, totalBudget, actualSpend, status, dueDate, projectId);
        this.id = id;
    }


    public int getProjectId() {
        return projectId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setProjectId(int projectId) {
        this.projectId = projectId;
    }

    public ProjectBudget(String name, double totalBudget,
                         double actualSpend, String status, LocalDate dueDate) {

        this.name = name;
        this.totalBudget = totalBudget;
        this.actualSpend = actualSpend;
        this.status = status;
        this.dueDate = dueDate;
    }


    public double getRemaining() {
        return this.totalBudget - this.actualSpend;
    }


    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getTotalBudget() { return totalBudget; }
    public void setTotalBudget(double totalBudget) { this.totalBudget = totalBudget; }

    public double getActualSpend() { return actualSpend; }
    public void setActualSpend(double actualSpend) { this.actualSpend = actualSpend; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    @Override
    public String toString() {
        return "id="+id+"name="+name;
    }
}