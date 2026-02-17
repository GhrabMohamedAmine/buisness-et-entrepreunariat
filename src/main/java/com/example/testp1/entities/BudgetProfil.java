package com.example.testp1.entities;

import java.time.Year;

public class BudgetProfil {
    private int id;
    private Year fiscalYear;
    private double budgetDisposable;
    private double totalExpense;
    private float marginProfit;

    public BudgetProfil() {}


    public BudgetProfil(Year fiscalYear, double budgetDisposable, double totalExpense, float marginProfit) {
        this.fiscalYear = fiscalYear;
        this.budgetDisposable = budgetDisposable;
        this.totalExpense = totalExpense;
        this.marginProfit = marginProfit;
    }


    public BudgetProfil(int id, Year fiscalYear, double budgetDisposable, double totalExpense, float marginProfit) {
        this(fiscalYear, budgetDisposable, totalExpense, marginProfit);
        this.id = id;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public Year getFiscalYear() { return fiscalYear; }
    public void setFiscalYear(Year fiscalYear) { this.fiscalYear = fiscalYear; }
    public double getBudgetDisposable() { return budgetDisposable; }
    public void setBudgetDisposable(double budgetDisposable) { this.budgetDisposable = budgetDisposable; }
    public double getTotalExpense() { return totalExpense; }
    public void setTotalExpense(double totalExpense) { this.totalExpense = totalExpense; }
    public float getMarginProfit() { return marginProfit; }
    public void setMarginProfit(float marginProfit) { this.marginProfit = marginProfit; }

    @Override
    public String toString() {
        return String.format("Profile[%d] Year: %s | Disposable: %.2f | Expense: %.2f | Margin: %.2f%%",
                id, fiscalYear, budgetDisposable, totalExpense, marginProfit);
    }
}