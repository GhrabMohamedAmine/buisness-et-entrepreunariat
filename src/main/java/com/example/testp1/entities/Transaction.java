package com.example.testp1.entities;

import java.time.LocalDate;

public class Transaction {
    private int id;
    private String reference;
    private double cost;
    private LocalDate dateStamp;

    private String expenseCategory;
    private int projectBudgetId;
    private int description;

    public Transaction() {}


    public Transaction(String  reference,double cost, LocalDate dateStamp, String expenseCategory, int projectBudgetId, int description) {
        this.cost = cost;
        this.reference = reference;
        this.dateStamp = dateStamp;
        this.expenseCategory = expenseCategory;
        this.projectBudgetId = projectBudgetId;
        this.description = description;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public Transaction(int id, String reference ,double cost, LocalDate dateStamp, String expenseCategory, int projectBudgetId, int description) {
        this(reference,cost , dateStamp, expenseCategory, projectBudgetId, description);
        this.id = id;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public double getCost() { return cost; }
    public void setCost(double cost) { this.cost = cost; }
    public LocalDate getDateStamp() { return dateStamp; }
    public void setDateStamp(LocalDate dateStamp) { this.dateStamp = dateStamp; }
    public String getExpenseCategory() { return expenseCategory; }
    public void setExpenseCategory(String expenseCategory) { this.expenseCategory = expenseCategory; }
    public int getProjectBudgetId() { return projectBudgetId; }
    public void setProjectBudgetId(int projectBudgetId) { this.projectBudgetId = projectBudgetId; }
    public int getDescription() { return description; }
    public void setDescription(int description) { this.description = description; }

    @Override
    public String toString() {
        return String.format("ID: %d | Ref: %s | Cost: %.2f | Category: %s | BudgetID: %d | Desc: %d",
                id, reference, cost, expenseCategory, projectBudgetId, description);
    }
}