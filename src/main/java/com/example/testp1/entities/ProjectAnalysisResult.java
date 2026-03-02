package com.example.testp1.entities;

import org.w3c.dom.ls.LSOutput;

import java.util.ArrayList;
import java.util.List;

public class ProjectAnalysisResult {
    private double projectedFinalCost;
    private double projectedFinalRevenue;
    private int successProbability;
    private String inflectionDate;
    private List<TimelinePoint> timelineData = new ArrayList<>();
    public double getOriginalBudget() {
        return originalBudget;
    }

    private double originalBudget;

    public void setOriginalBudget(double originalBudget) {
        this.originalBudget = originalBudget;
    }

    // --- Getters and Setters ---
    public double getProjectedFinalCost() { return projectedFinalCost; }
    public void setProjectedFinalCost(double projectedFinalCost) { this.projectedFinalCost = projectedFinalCost; }

    public double getProjectedFinalRevenue() { return projectedFinalRevenue; }
    public void setProjectedFinalRevenue(double projectedFinalRevenue) { this.projectedFinalRevenue = projectedFinalRevenue; }

    public int getSuccessProbability() { return successProbability; }
    public void setSuccessProbability(int successProbability) { this.successProbability = successProbability; }

    public String getInflectionDate() { return inflectionDate; }
    public void setInflectionDate(String inflectionDate) { this.inflectionDate = inflectionDate; }

    public List<TimelinePoint> getTimelineData() { return timelineData; }
    public void addTimelinePoint(TimelinePoint point) { this.timelineData.add(point); }

    // --- Inner Class for the Chart Array ---
    public static class TimelinePoint {
        private String month;
        private double spend;
        private double revenue;



        public TimelinePoint(String month, double spend, double revenue) {
            this.month = month;
            this.spend = spend;
            this.revenue = revenue;
        }

        public String getMonth() { return month; }
        public double getSpend() { return spend; }
        public double getRevenue() { return revenue; }

        @Override
        public String toString() {
            return String.format("Month: %-5s | Spend: $%,10.2f | Revenue: $%,10.2f", month, spend, revenue);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n=== AI PROJECT ANALYSIS RESULT ===\n");
        sb.append(String.format("Projected Final Cost:    $%,.2f\n", projectedFinalCost));
        sb.append(String.format("Projected Final Revenue: $%,.2f\n", projectedFinalRevenue));
        sb.append("Success Probability:     ").append(successProbability).append("%\n");
        sb.append("Inflection Date:         ").append(inflectionDate).append("\n");
        sb.append("Timeline Trajectory:\n");

        if (timelineData == null || timelineData.isEmpty()) {
            sb.append("  [No timeline data generated]\n");
        } else {
            for (TimelinePoint point : timelineData) {
                sb.append("  ").append(point.toString()).append("\n");
            }
        }
        sb.append("==================================\n");
        return sb.toString();
    }
}