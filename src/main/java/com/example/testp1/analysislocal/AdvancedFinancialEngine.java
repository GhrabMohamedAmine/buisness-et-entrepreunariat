package com.example.testp1.analysislocal;

import com.example.testp1.entities.ProjectBudget;
import com.example.testp1.entities.Transaction;

import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AdvancedFinancialEngine {
    private static final int MONTHS_TO_PROJECT = 6;

    public AdvancedFinancialProjection project(ProjectBudget budget, List<Transaction> transactions, String userNotes) {
        double totalBudget = budget == null ? 0.0 : Math.max(0.0, budget.getTotalBudget());
        double currentSpend = budget == null ? 0.0 : Math.max(0.0, budget.getActualSpend());

        List<Double> monthlySpendHistory = extractMonthlySpendHistory(transactions);
        if (monthlySpendHistory.isEmpty()) {
            double syntheticBurn = currentSpend > 0 ? currentSpend / 3.0 : Math.max(200.0, totalBudget * 0.08);
            for (int i = 0; i < 3; i++) {
                monthlySpendHistory.add(syntheticBurn);
            }
        }

        double weightedBurn = weightedAverage(monthlySpendHistory);
        double volatility = normalizedVolatility(monthlySpendHistory, weightedBurn);
        double notesAdjustment = extractNotesAdjustment(userNotes);

        double baseMonthlyBurn = Math.max(120.0, weightedBurn * (1.0 + notesAdjustment));
        double cumulativeSpend = currentSpend;
        double cumulativeValue = Math.max(0.0, currentSpend * 0.52);

        String inflectionMonth = "N/A";
        List<AdvancedFinancialProjection.TimelinePoint> timeline = new ArrayList<>();

        YearMonth monthCursor = YearMonth.now();
        for (int i = 1; i <= MONTHS_TO_PROJECT; i++) {
            monthCursor = monthCursor.plusMonths(1);

            double trend = 1.0 + (0.025 * i) + (volatility * 0.03 * i);
            double monthlySpend = baseMonthlyBurn * trend;
            cumulativeSpend += monthlySpend;

            double valueEfficiency = 0.72 + (i * 0.11) - (volatility * 0.15);
            double monthlyValue = Math.max(0.0, monthlySpend * valueEfficiency);
            cumulativeValue += monthlyValue;

            String monthLabel = monthCursor.getMonth().getDisplayName(TextStyle.SHORT, Locale.US);
            timeline.add(new AdvancedFinancialProjection.TimelinePoint(monthLabel, cumulativeSpend, cumulativeValue));

            if ("N/A".equals(inflectionMonth) && cumulativeValue >= cumulativeSpend) {
                inflectionMonth = monthLabel;
            }
        }

        int probability = computeSuccessProbability(totalBudget, cumulativeSpend, volatility, inflectionMonth);
        String riskLabel = probability < 40 ? "CRITICAL RISK" : (probability < 70 ? "MODERATE RISK" : "LOW RISK");

        return new AdvancedFinancialProjection(
                totalBudget,
                currentSpend,
                cumulativeSpend,
                cumulativeValue,
                probability,
                riskLabel,
                inflectionMonth,
                timeline
        );
    }

    private List<Double> extractMonthlySpendHistory(List<Transaction> transactions) {
        List<Double> history = new ArrayList<>();
        if (transactions == null || transactions.isEmpty()) {
            return history;
        }

        Map<YearMonth, Double> perMonth = new HashMap<>();
        for (Transaction t : transactions) {
            if (t == null || t.getDateStamp() == null) {
                continue;
            }
            YearMonth month = YearMonth.from(t.getDateStamp());
            perMonth.merge(month, Math.max(0.0, t.getCost()), Double::sum);
        }

        List<Map.Entry<YearMonth, Double>> entries = new ArrayList<>(perMonth.entrySet());
        entries.sort(Comparator.comparing(Map.Entry::getKey));

        for (Map.Entry<YearMonth, Double> entry : entries) {
            history.add(entry.getValue());
        }
        return history;
    }

    private double weightedAverage(List<Double> values) {
        double numerator = 0.0;
        double denominator = 0.0;
        for (int i = 0; i < values.size(); i++) {
            double weight = i + 1;
            numerator += values.get(i) * weight;
            denominator += weight;
        }
        return denominator == 0.0 ? 0.0 : numerator / denominator;
    }

    private double normalizedVolatility(List<Double> values, double average) {
        if (values.isEmpty() || average <= 0.0) {
            return 0.0;
        }

        double variance = 0.0;
        for (Double value : values) {
            double delta = value - average;
            variance += delta * delta;
        }

        double stdDev = Math.sqrt(variance / values.size());
        return Math.min(1.0, stdDev / average);
    }

    private double extractNotesAdjustment(String notes) {
        if (notes == null || notes.trim().isEmpty()) {
            return 0.0;
        }

        String normalized = notes.toLowerCase(Locale.ROOT);
        double adjustment = 0.0;

        if (normalized.contains("hire") || normalized.contains("recruit") || normalized.contains("salary")) {
            adjustment += 0.10;
        }
        if (normalized.contains("marketing") || normalized.contains("campaign") || normalized.contains("ads")) {
            adjustment += 0.06;
        }
        if (normalized.contains("automation") || normalized.contains("optimiz") || normalized.contains("reduce") || normalized.contains("saving")) {
            adjustment -= 0.08;
        }

        Pattern percentPattern = Pattern.compile("([+-]?\\d+(?:\\.\\d+)?)\\s*%");
        Matcher matcher = percentPattern.matcher(normalized);
        while (matcher.find()) {
            double parsed = Double.parseDouble(matcher.group(1));
            adjustment += parsed / 100.0;
        }

        return Math.max(-0.35, Math.min(0.50, adjustment));
    }

    private int computeSuccessProbability(double totalBudget, double projectedSpend, double volatility, String inflectionMonth) {
        double ratio = totalBudget <= 0.0 ? 2.0 : projectedSpend / totalBudget;

        double score = 100.0;
        if (ratio > 1.0) {
            score -= Math.min(70.0, (ratio - 1.0) * 170.0);
        } else {
            score += Math.min(10.0, (1.0 - ratio) * 30.0);
        }

        score -= volatility * 36.0;
        if ("N/A".equals(inflectionMonth)) {
            score -= 14.0;
        } else {
            score += 8.0;
        }

        int rounded = (int) Math.round(score);
        return Math.max(1, Math.min(98, rounded));
    }
}
