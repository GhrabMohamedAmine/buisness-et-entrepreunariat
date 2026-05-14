package com.example.testp1.entities;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;

public class BudgetProfil {
    private int id;
    private Year fiscalYear;
    private BigDecimal budgetDisposable;
    private BigDecimal totalExpense;
    private Double marginProfit;

    private String baseCurrency;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;

    public BudgetProfil() {}

    public BudgetProfil(Year fiscalYear, BigDecimal budgetDisposable, BigDecimal totalExpense, Double marginProfit, String baseCurrency, LocalDate startDate, LocalDate endDate, String status) {
        this.fiscalYear = fiscalYear;
        this.budgetDisposable = budgetDisposable;
        this.totalExpense = totalExpense;
        this.marginProfit = marginProfit;
        this.baseCurrency = baseCurrency;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
    }

    public BudgetProfil(int id, Year fiscalYear, BigDecimal budgetDisposable, BigDecimal totalExpense, Double marginProfit, String baseCurrency, LocalDate startDate, LocalDate endDate, String status) {
        this(fiscalYear, budgetDisposable, totalExpense, marginProfit, baseCurrency, startDate, endDate, status);
        this.id = id;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public Year getFiscalYear() { return fiscalYear; }
    public void setFiscalYear(Year fiscalYear) { this.fiscalYear = fiscalYear; }
    public BigDecimal getBudgetDisposable() { return budgetDisposable; }
    public void setBudgetDisposable(BigDecimal budgetDisposable) { this.budgetDisposable = budgetDisposable; }
    public BigDecimal getTotalExpense() { return totalExpense; }
    public void setTotalExpense(BigDecimal totalExpense) { this.totalExpense = totalExpense; }
    public Double getMarginProfit() { return marginProfit; }
    public void setMarginProfit(Double marginProfit) { this.marginProfit = marginProfit; }
    public String getBaseCurrency() { return baseCurrency; }
    public void setBaseCurrency(String baseCurrency) { this.baseCurrency = baseCurrency; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return String.format("Profile[%d] Year: %s | Disposable: %s | Expense: %s | Margin: %.2f%% | Status: %s",
                id, fiscalYear, budgetDisposable != null ? budgetDisposable.toString() : "null", 
                totalExpense != null ? totalExpense.toString() : "null", marginProfit, status);
    }
}
