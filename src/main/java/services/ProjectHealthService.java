package services;

import entities.Task;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class ProjectHealthService {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH);
    private static final double PROGRESS_WEIGHT = 70.0;
    private static final double DELAY_WEIGHT = 30.0;

    public ProjectHealthResult calculateForProject(List<Task> tasks) {
        int totalTasks = tasks == null ? 0 : tasks.size();
        int completedTasks = 0;
        int overdueTasks = 0;
        LocalDate today = LocalDate.now();

        if (tasks != null) {
            for (Task task : tasks) {
                if (task == null) {
                    continue;
                }
                if (isDone(task.getStatus())) {
                    completedTasks++;
                }
                LocalDate dueDate = parseDate(task.getDueDate());
                if (dueDate != null && dueDate.isBefore(today) && !isDone(task.getStatus())) {
                    overdueTasks++;
                }
            }
        }

        double progressRatio = totalTasks > 0 ? completedTasks / (double) totalTasks : 0.0;
        double delayRatio = totalTasks > 0 ? 1.0 - (overdueTasks / (double) totalTasks) : 0.0;

        double progressScore = progressRatio * PROGRESS_WEIGHT;
        double delayScore = clamp(delayRatio, 0.0, 1.0) * DELAY_WEIGHT;

        double totalScore = clamp(progressScore + delayScore, 0.0, 100.0);
        int roundedScore = (int) Math.round(totalScore);
        HealthLevel level = classify(roundedScore);

        return new ProjectHealthResult(
                roundedScore,
                level,
                progressScore,
                delayScore,
                totalTasks,
                completedTasks,
                overdueTasks
        );
    }

    private HealthLevel classify(int score) {
        if (score >= 80) {
            return HealthLevel.HEALTHY;
        }
        if (score >= 50) {
            return HealthLevel.AT_RISK;
        }
        return HealthLevel.CRITICAL;
    }

    private boolean isDone(String status) {
        if (status == null) {
            return false;
        }
        String normalized = status.trim()
                .toUpperCase(Locale.ROOT)
                .replace(' ', '_')
                .replace('-', '_');
        return "DONE".equals(normalized);
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim(), DATE_FORMAT);
        } catch (Exception e) {
            return null;
        }
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public enum HealthLevel {
        HEALTHY("Healthy"),
        AT_RISK("At Risk"),
        CRITICAL("Critical");

        private final String label;

        HealthLevel(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    public static class ProjectHealthResult {
        private final int score;
        private final HealthLevel level;
        private final double progressScore;
        private final double delayScore;
        private final int totalTasks;
        private final int completedTasks;
        private final int overdueTasks;

        public ProjectHealthResult(int score,
                                   HealthLevel level,
                                   double progressScore,
                                   double delayScore,
                                   int totalTasks,
                                   int completedTasks,
                                   int overdueTasks) {
            this.score = score;
            this.level = level;
            this.progressScore = progressScore;
            this.delayScore = delayScore;
            this.totalTasks = totalTasks;
            this.completedTasks = completedTasks;
            this.overdueTasks = overdueTasks;
        }

        public int getScore() {
            return score;
        }

        public HealthLevel getLevel() {
            return level;
        }

        public double getProgressScore() {
            return progressScore;
        }

        public double getDelayScore() {
            return delayScore;
        }

        public int getTotalTasks() {
            return totalTasks;
        }

        public int getCompletedTasks() {
            return completedTasks;
        }

        public int getOverdueTasks() {
            return overdueTasks;
        }
    }
}
