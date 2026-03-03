package com.example.testp1.analysislocal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AdvancedFinancialProjection {
    private final double totalBudget;
    private final double currentSpend;
    private final double projectedFinalSpend;
    private final double projectedFinalValue;
    private final int successProbability;
    private final String riskLabel;
    private final String inflectionMonth;
    private final List<TimelinePoint> timeline;

    public AdvancedFinancialProjection(
            double totalBudget,
            double currentSpend,
            double projectedFinalSpend,
            double projectedFinalValue,
            int successProbability,
            String riskLabel,
            String inflectionMonth,
            List<TimelinePoint> timeline
    ) {
        this.totalBudget = totalBudget;
        this.currentSpend = currentSpend;
        this.projectedFinalSpend = projectedFinalSpend;
        this.projectedFinalValue = projectedFinalValue;
        this.successProbability = successProbability;
        this.riskLabel = riskLabel;
        this.inflectionMonth = inflectionMonth;
        this.timeline = new ArrayList<>(timeline);
    }

    public double getTotalBudget() {
        return totalBudget;
    }

    public double getCurrentSpend() {
        return currentSpend;
    }

    public double getProjectedFinalSpend() {
        return projectedFinalSpend;
    }

    public double getProjectedFinalValue() {
        return projectedFinalValue;
    }

    public int getSuccessProbability() {
        return successProbability;
    }

    public String getRiskLabel() {
        return riskLabel;
    }

    public String getInflectionMonth() {
        return inflectionMonth;
    }

    public List<TimelinePoint> getTimeline() {
        return Collections.unmodifiableList(timeline);
    }

    public static class TimelinePoint {
        private final String month;
        private final double cumulativeSpend;
        private final double cumulativeValue;

        public TimelinePoint(String month, double cumulativeSpend, double cumulativeValue) {
            this.month = month;
            this.cumulativeSpend = cumulativeSpend;
            this.cumulativeValue = cumulativeValue;
        }

        public String getMonth() {
            return month;
        }

        public double getCumulativeSpend() {
            return cumulativeSpend;
        }

        public double getCumulativeValue() {
            return cumulativeValue;
        }
    }
}
