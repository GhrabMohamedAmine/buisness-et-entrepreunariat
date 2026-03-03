package com.example.testp1.services;

import com.example.testp1.entities.ProjectBudget;
import com.example.testp1.entities.Transaction;

import java.sql.SQLException;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CashflowAnalyticsService {

    private final ServiceProjectBudget budgetService = new ServiceProjectBudget();
    private final ServiceTransaction transactionService = new ServiceTransaction();

    public CashflowSnapshot buildSnapshot() throws SQLException {
        List<ProjectBudget> budgets = budgetService.getAll();

        double totalBudget = 0.0;
        double totalSpent = 0.0;

        Map<String, Double> categoryTotals = new LinkedHashMap<>();
        Map<YearMonth, Double> monthlyExpenseTotals = new LinkedHashMap<>();
        List<ProjectBurnPoint> projectBurn = new ArrayList<>();

        for (ProjectBudget budget : budgets) {
            if (budget == null) {
                continue;
            }

            totalBudget += Math.max(0.0, budget.getTotalBudget());
            totalSpent += Math.max(0.0, budget.getActualSpend());

            List<Transaction> transactions = transactionService.getTransactionsByBudget(budget.getId());
            for (Transaction t : transactions) {
                if (t == null || t.getDateStamp() == null) {
                    continue;
                }

                String category = t.getExpenseCategory();
                if (category == null || category.isBlank()) {
                    category = "Other";
                }

                double cost = Math.max(0.0, t.getCost());
                categoryTotals.merge(category, cost, Double::sum);
                monthlyExpenseTotals.merge(YearMonth.from(t.getDateStamp()), cost, Double::sum);
            }

            double utilization = budget.getTotalBudget() <= 0.0
                    ? 0.0
                    : (Math.max(0.0, budget.getActualSpend()) / budget.getTotalBudget()) * 100.0;
            projectBurn.add(new ProjectBurnPoint(
                    budget.getName() == null || budget.getName().isBlank() ? "Budget #" + budget.getId() : budget.getName(),
                    Math.min(100.0, Math.max(0.0, utilization))
            ));
        }

        List<Map.Entry<YearMonth, Double>> sortedMonths = new ArrayList<>(monthlyExpenseTotals.entrySet());
        sortedMonths.sort(Map.Entry.comparingByKey());
        if (sortedMonths.size() > 8) {
            sortedMonths = sortedMonths.subList(sortedMonths.size() - 8, sortedMonths.size());
        }

        List<MonthlyExpensePoint> monthly = new ArrayList<>();
        for (Map.Entry<YearMonth, Double> entry : sortedMonths) {
            String label = entry.getKey().getMonth().getDisplayName(TextStyle.SHORT, Locale.US) + " " + entry.getKey().getYear();
            monthly.add(new MonthlyExpensePoint(label, entry.getValue()));
        }

        List<CategoryPoint> category = new ArrayList<>();
        categoryTotals.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(8)
                .forEach(e -> category.add(new CategoryPoint(e.getKey(), e.getValue())));

        projectBurn.sort(Comparator.comparingDouble(ProjectBurnPoint::utilizationPercent).reversed());
        if (projectBurn.size() > 8) {
            projectBurn = projectBurn.subList(0, 8);
        }

        int riskScore = computeRiskScore(totalBudget, totalSpent, monthly, category);

        return new CashflowSnapshot(totalBudget, totalSpent, category, monthly, projectBurn, riskScore);
    }

    private int computeRiskScore(
            double totalBudget,
            double totalSpent,
            List<MonthlyExpensePoint> monthly,
            List<CategoryPoint> categories
    ) {
        if (totalBudget <= 0.0 || monthly.isEmpty()) {
            return 0;
        }

        double utilization = Math.min(1.0, Math.max(0.0, totalSpent / totalBudget));

        double mean = monthly.stream().mapToDouble(MonthlyExpensePoint::amount).average().orElse(0.0);
        double variance = monthly.stream()
                .mapToDouble(p -> (p.amount() - mean) * (p.amount() - mean))
                .average()
                .orElse(0.0);
        double volatility = mean <= 0.0 ? 0.0 : Math.sqrt(variance) / mean;

        double concentration = 0.0;
        double totalCategoryAmount = categories.stream().mapToDouble(CategoryPoint::amount).sum();
        if (totalCategoryAmount > 0.0) {
            concentration = categories.stream().mapToDouble(CategoryPoint::amount).max().orElse(0.0) / totalCategoryAmount;
        }

        int score = (int) Math.round(
                45.0 * utilization +
                30.0 * Math.min(1.0, volatility) +
                25.0 * concentration
        );

        return Math.max(0, Math.min(100, score));
    }

    public record CashflowSnapshot(
            double totalBudget,
            double totalSpent,
            List<CategoryPoint> categoryBreakdown,
            List<MonthlyExpensePoint> monthlyExpenses,
            List<ProjectBurnPoint> projectBurn,
            int riskScore
    ) {}

    public record CategoryPoint(String category, double amount) {}
    public record MonthlyExpensePoint(String monthLabel, double amount) {}
    public record ProjectBurnPoint(String projectName, double utilizationPercent) {}
}
