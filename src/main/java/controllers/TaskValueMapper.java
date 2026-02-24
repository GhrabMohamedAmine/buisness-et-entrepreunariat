package controllers;

import java.util.Locale;

public final class TaskValueMapper {
    public static final String STATUS_TODO = "TODO";
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_DONE = "DONE";
    public static final String PRIORITY_LOW = "LOW";
    public static final String PRIORITY_MEDIUM = "MEDIUM";
    public static final String PRIORITY_HIGH = "HIGH";
    public static final String FILTER_ALL = "ALL";

    private TaskValueMapper() {
    }

    public static String normalizeStatus(String value) {
        if (value == null || value.isBlank()) return STATUS_TODO;

        String trimmed = value.trim();
        if ("To Do".equalsIgnoreCase(trimmed)) return STATUS_TODO;
        if ("In Progress".equalsIgnoreCase(trimmed)) return STATUS_IN_PROGRESS;
        if ("Done".equalsIgnoreCase(trimmed)) return STATUS_DONE;

        String normalized = trimmed.toUpperCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
        if ("TO_DO".equals(normalized)) return STATUS_TODO;
        if (STATUS_IN_PROGRESS.equals(normalized)) return STATUS_IN_PROGRESS;
        if (STATUS_DONE.equals(normalized)) return STATUS_DONE;
        return STATUS_TODO;
    }

    public static String toStatusLabel(String status) {
        String normalized = normalizeStatus(status);
        if (STATUS_DONE.equals(normalized)) return "Done";
        if (STATUS_IN_PROGRESS.equals(normalized)) return "In Progress";
        return "To Do";
    }

    public static String statusPillStyleClass(String status) {
        String normalized = normalizeStatus(status);
        if (STATUS_DONE.equals(normalized)) return "task-status-done";
        if (STATUS_IN_PROGRESS.equals(normalized)) return "task-status-progress";
        return "task-status-todo";
    }

    public static String statusBadgeStyleClass(String status) {
        String normalized = normalizeStatus(status);
        if (STATUS_DONE.equals(normalized)) return "badge-done";
        if (STATUS_IN_PROGRESS.equals(normalized)) return "badge-progress";
        return "badge-todo";
    }

    public static String normalizePriority(String value) {
        if (value == null || value.isBlank()) return PRIORITY_LOW;
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (PRIORITY_HIGH.equals(normalized)) return PRIORITY_HIGH;
        if (PRIORITY_MEDIUM.equals(normalized)) return PRIORITY_MEDIUM;
        return PRIORITY_LOW;
    }

    public static String priorityPillStyleClass(String priority) {
        String normalized = normalizePriority(priority);
        if (PRIORITY_HIGH.equals(normalized)) return "task-priority-high";
        if (PRIORITY_MEDIUM.equals(normalized)) return "task-priority-medium";
        return "task-priority-low";
    }

    public static String priorityBadgeStyleClass(String priority) {
        String normalized = normalizePriority(priority);
        if (PRIORITY_HIGH.equals(normalized)) return "badge-priority-high";
        if (PRIORITY_MEDIUM.equals(normalized)) return "badge-priority-medium";
        return "badge-priority-low";
    }

    public static String toStatusFilterLabel(String statusFilter) {
        if (statusFilter == null || statusFilter.isBlank() || FILTER_ALL.equals(statusFilter)) return "All";
        return toStatusLabel(statusFilter);
    }

    public static String fromStatusFilterLabel(String label) {
        if (label == null || label.isBlank() || "All".equalsIgnoreCase(label.trim())) return FILTER_ALL;
        return normalizeStatus(label);
    }

    public static String toPriorityFilterLabel(String priorityFilter) {
        if (priorityFilter == null || priorityFilter.isBlank() || FILTER_ALL.equals(priorityFilter)) return "All";
        String normalized = normalizePriority(priorityFilter);
        if (PRIORITY_HIGH.equals(normalized)) return "High";
        if (PRIORITY_MEDIUM.equals(normalized)) return "Medium";
        return "Low";
    }

    public static String fromPriorityFilterLabel(String label) {
        if (label == null || label.isBlank() || "All".equalsIgnoreCase(label.trim())) return FILTER_ALL;
        return normalizePriority(label);
    }
}
