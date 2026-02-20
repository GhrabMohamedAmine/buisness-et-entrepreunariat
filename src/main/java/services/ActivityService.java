package services;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ActivityService {
    public static class TaskActivity {
        private final String type;
        private final int projectId;
        private final int taskId;
        private final String taskTitle;
        private final int actorUserId;
        private final String activityDate;
        private final long timestamp;

        public TaskActivity(String type, int projectId, int taskId, String taskTitle, int actorUserId, String activityDate, long timestamp) {
            this.type = type;
            this.projectId = projectId;
            this.taskId = taskId;
            this.taskTitle = taskTitle;
            this.actorUserId = actorUserId;
            this.activityDate = activityDate;
            this.timestamp = timestamp;
        }

        public String getType() { return type; }
        public int getProjectId() { return projectId; }
        public int getTaskId() { return taskId; }
        public String getTaskTitle() { return taskTitle; }
        public int getActorUserId() { return actorUserId; }
        public String getActivityDate() { return activityDate; }
        public long getTimestamp() { return timestamp; }
    }

    private static final List<TaskActivity> TASK_ACTIVITY_LOG = new ArrayList<>();

    public static synchronized void logTaskActivity(String type, int projectId, int taskId, String taskTitle, int actorUserId) {
        String safeType = type == null ? "UPDATED" : type;
        String safeTitle = (taskTitle == null || taskTitle.isBlank()) ? "Untitled task" : taskTitle.trim();
        TASK_ACTIVITY_LOG.add(new TaskActivity(
                safeType,
                projectId,
                taskId,
                safeTitle,
                actorUserId,
                LocalDate.now().toString(),
                System.currentTimeMillis()
        ));
    }

    public synchronized List<TaskActivity> getTaskActivitiesByProject(int projectId, int limit) {
        List<TaskActivity> result = new ArrayList<>();
        for (int i = TASK_ACTIVITY_LOG.size() - 1; i >= 0; i--) {
            TaskActivity activity = TASK_ACTIVITY_LOG.get(i);
            if (activity.getProjectId() != projectId) {
                continue;
            }
            result.add(activity);
            if (limit > 0 && result.size() >= limit) {
                break;
            }
        }
        return result;
    }
}
