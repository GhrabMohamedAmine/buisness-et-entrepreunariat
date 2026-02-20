package services;

import entities.Task;
import utils.MyDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static controllers.TaskValueMapper.normalizeStatus;

public class TaskService {
    public void addTask(Task task) {
        String query = "INSERT INTO tasks (title, description, status, priority, start_date, due_date, project_id, assigned_to ,created_by) VALUES (?, ?, ?, ?, ?, ?, ?, ? ,?)";

        try (Connection conn = MyDatabase.getConnection();
             PreparedStatement adt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {

            adt.setString(1, task.getTitle());
            adt.setString(2, task.getDescription());
            adt.setString(3, task.getStatus());
            adt.setString(4, task.getPriority());
            adt.setString(5, task.getStartDate());
            adt.setString(6, task.getDueDate());
            adt.setInt(7, task.getProjectId());
            adt.setInt(8, task.getAssignedTo());
            adt.setInt(9, task.getCreatedby());

            adt.executeUpdate();
            int taskId = -1;
            ResultSet keys = adt.getGeneratedKeys();
            if (keys.next()) {
                taskId = keys.getInt(1);
            }
            ActivityService.logTaskActivity("CREATED", task.getProjectId(), taskId, task.getTitle(), task.getCreatedby());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Task> getTasksByProject(int projectId) {
        List<Task> tasks = new ArrayList<>();
        String query = "SELECT t.*, CONCAT(u.prenom, ' ', u.nom) as user_name " +
                "FROM tasks t " +
                "LEFT JOIN utilisateurs u ON t.assigned_to = u.id " +
                "WHERE t.project_id = ? " +
                "ORDER BY t.due_date IS NULL, t.due_date ASC, t.id DESC";

        try (Connection conn = MyDatabase.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, projectId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Task task = new Task(
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getString("status"),
                        rs.getString("priority"),
                        rs.getString("start_date"),
                        rs.getString("due_date"),
                        rs.getInt("project_id"),
                        rs.getInt("assigned_to"),
                        rs.getInt("created_by")
                );
                task.setId(rs.getInt("id"));
                task.setAssignedToName(rs.getString("user_name"));
                tasks.add(task);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return tasks;
    }

    public int getTaskCount(int projectId) {
        String query = "SELECT COUNT(*) FROM tasks WHERE project_id = ?";
        try (Connection con = MyDatabase.getConnection();
             PreparedStatement pstmt = con.prepareStatement(query)) {
            pstmt.setInt(1, projectId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int getTaskCountByStatus(int projectId, String status) {
        String query = "SELECT COUNT(*) FROM tasks WHERE project_id = ? AND status = ?";
        try (Connection conn = MyDatabase.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, projectId);
            pstmt.setString(2, status);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int getOverdueTaskCount(int projectId) {
        String query = "SELECT COUNT(*) FROM tasks " +
                "WHERE project_id = ? " +
                "AND due_date IS NOT NULL " +
                "AND DATE(due_date) < CURDATE() " +
                "AND UPPER(REPLACE(REPLACE(COALESCE(status, ''), ' ', '_'), '-', '_')) <> 'DONE'";
        try (Connection conn = MyDatabase.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, projectId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int getTasksInProgressCount() {
        String query = "SELECT COUNT(*) FROM tasks " +
                "WHERE UPPER(REPLACE(REPLACE(COALESCE(status, ''), ' ', '_'), '-', '_')) = 'IN_PROGRESS'";
        try (Connection conn = MyDatabase.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int getCompletedTasksCount() {
        String query = "SELECT COUNT(*) FROM tasks " +
                "WHERE UPPER(REPLACE(REPLACE(COALESCE(status, ''), ' ', '_'), '-', '_')) = 'DONE'";
        try (Connection conn = MyDatabase.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int getUpcomingDeadlinesCount(int daysAhead) {
        String query = "SELECT COUNT(*) FROM tasks " +
                "WHERE due_date IS NOT NULL " +
                "AND DATE(due_date) >= CURDATE() " +
                "AND DATE(due_date) <= DATE_ADD(CURDATE(), INTERVAL ? DAY) " +
                "AND UPPER(REPLACE(REPLACE(COALESCE(status, ''), ' ', '_'), '-', '_')) <> 'DONE'";
        try (Connection conn = MyDatabase.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, daysAhead);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int getOverdueUndoneTasksCount() {
        String query = "SELECT COUNT(*) FROM tasks " +
                "WHERE due_date IS NOT NULL " +
                "AND DATE(due_date) < CURDATE() " +
                "AND UPPER(REPLACE(REPLACE(COALESCE(status, ''), ' ', '_'), '-', '_')) <> 'DONE'";
        try (Connection conn = MyDatabase.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public boolean updateTaskStatus(int taskId, String newStatus) {
        String fetchQuery = "SELECT id, title, project_id, assigned_to, created_by, status FROM tasks WHERE id = ?";
        String query = "UPDATE tasks SET status = ? WHERE id = ?";
        try (Connection conn = MyDatabase.getConnection();
             PreparedStatement fetchStmt = conn.prepareStatement(fetchQuery);
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            fetchStmt.setInt(1, taskId);
            ResultSet fetchRs = fetchStmt.executeQuery();
            if (!fetchRs.next()) {
                return false;
            }
            String oldStatus = fetchRs.getString("status");
            String title = fetchRs.getString("title");
            int projectId = fetchRs.getInt("project_id");
            int actorUserId = fetchRs.getInt("assigned_to");
            if (fetchRs.wasNull() || actorUserId <= 0) {
                actorUserId = fetchRs.getInt("created_by");
            }

            pstmt.setString(1, newStatus);
            pstmt.setInt(2, taskId);
            boolean updated = pstmt.executeUpdate() > 0;
            if (updated && !normalizeStatus(newStatus).equals(normalizeStatus(oldStatus))) {
                if ("DONE".equals(normalizeStatus(newStatus))) {
                    ActivityService.logTaskActivity("COMPLETED", projectId, taskId, title, actorUserId);
                } else {
                    ActivityService.logTaskActivity("UPDATED", projectId, taskId, title, actorUserId);
                }
            }
            return updated;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    public List<Task> getTasksByStatus(String status , int projectId) {
        List<Task> tasks = new ArrayList<>();
        String query = "SELECT t.*, CONCAT(u.prenom, ' ', u.nom) as user_name " +
                "FROM tasks t " +
                "LEFT JOIN utilisateurs u ON t.assigned_to = u.id " +
                "WHERE t.status = ? AND t.project_id = ? " +
                "ORDER BY t.due_date IS NULL, t.due_date ASC, t.id DESC";
        try (Connection conn = MyDatabase.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, status);
            pstmt.setInt(2, projectId);

            ResultSet rs = pstmt.executeQuery();
            while ( rs.next()){
                Task task = new Task(
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getString("status"),
                        rs.getString("priority"),
                        rs.getString("start_date"),
                        rs.getString("due_date"),
                        rs.getInt("project_id"),
                        rs.getInt("assigned_to"),
                        rs.getInt("created_by")
                );
                task.setId(rs.getInt("id"));
                task.setAssignedToName(rs.getString("user_name"));
                tasks.add(task);
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return tasks;
    }


    public List<Task> getTasksByAssignedUser(int userId) {
        List<Task> tasks = new ArrayList<>();
        String query = "SELECT t.*, CONCAT(u.prenom, ' ', u.nom) as user_name " +
                "FROM tasks t " +
                "LEFT JOIN utilisateurs u ON t.assigned_to = u.id " +
                "WHERE t.assigned_to = ? " +
                "ORDER BY t.due_date IS NULL, t.due_date ASC, t.id DESC";

        try (Connection conn = MyDatabase.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Task task = new Task(
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getString("status"),
                        rs.getString("priority"),
                        rs.getString("start_date"),
                        rs.getString("due_date"),
                        rs.getInt("project_id"),
                        rs.getInt("assigned_to"),
                        rs.getInt("created_by")
                );
                task.setId(rs.getInt("id"));
                task.setAssignedToName(rs.getString("user_name"));
                tasks.add(task);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return tasks;
    }

    public boolean updateTask(Task task) {
        String query = "UPDATE tasks SET title = ?, description = ?, status = ?, priority = ?, due_date = ?, assigned_to = ? WHERE id = ?";
        try (Connection conn = MyDatabase.getConnection();
             PreparedStatement upTa = conn.prepareStatement(query)) {
            upTa.setString(1, task.getTitle());
            upTa.setString(2, task.getDescription());
            upTa.setString(3, task.getStatus());
            upTa.setString(4, task.getPriority());
            upTa.setString(5, task.getDueDate());
            upTa.setInt(6, task.getAssignedTo());
            upTa.setInt(7, task.getId());
            boolean updated = upTa.executeUpdate() > 0;
            if (updated) {
                ActivityService.logTaskActivity("UPDATED", task.getProjectId(), task.getId(), task.getTitle(), task.getAssignedTo());
            }
            return updated;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteTaskById(int taskId) {
        String fetchQuery = "SELECT id, title, project_id, assigned_to, created_by FROM tasks WHERE id = ?";
        String query = "DELETE FROM tasks WHERE id = ?";
        try (Connection conn = MyDatabase.getConnection();
             PreparedStatement fetchStmt = conn.prepareStatement(fetchQuery);
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            fetchStmt.setInt(1, taskId);
            ResultSet fetchRs = fetchStmt.executeQuery();
            if (!fetchRs.next()) {
                return false;
            }
            String title = fetchRs.getString("title");
            int projectId = fetchRs.getInt("project_id");
            int actorUserId = fetchRs.getInt("assigned_to");
            if (fetchRs.wasNull() || actorUserId <= 0) {
                actorUserId = fetchRs.getInt("created_by");
            }

            pstmt.setInt(1, taskId);
            boolean deleted = pstmt.executeUpdate() > 0;
            if (deleted) {
                ActivityService.logTaskActivity("DELETED", projectId, taskId, title, actorUserId);
            }
            return deleted;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "TODO";
        }
        String normalized = status.trim().toUpperCase().replace(' ', '_').replace('-', '_');
        if ("TO_DO".equals(normalized)) {
            return "TODO";
        }
        return normalized;
    }
}
