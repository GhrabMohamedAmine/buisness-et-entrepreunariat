package tezfx.model;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.awt.AWTEventMulticaster.add;

public class sql {
    public List<Project> getAllProjects() {
        List<Project> projects = new ArrayList<>();
        String query = "SELECT p.id, p.name, p.description, p.budget, p.start_date, p.end_date, p.assigned_to, p.created_by, " +
                "CASE WHEN COUNT(t.id) = 0 THEN 0 " +
                "ELSE ROUND(100 * SUM(CASE WHEN UPPER(REPLACE(REPLACE(COALESCE(t.status, ''), ' ', '_'), '-', '_')) = 'DONE' THEN 1 ELSE 0 END) / COUNT(t.id), 0) END AS computed_progress " +
                "FROM projects p " +
                "LEFT JOIN tasks t ON t.project_id = p.id " +
                "GROUP BY p.id, p.name, p.description, p.budget, p.start_date, p.end_date, p.assigned_to, p.created_by";

        try (Connection conn = databaseconnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                projects.add(new Project(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getInt("computed_progress"),
                        rs.getDouble("budget"),
                        rs.getString("start_date"),
                        rs.getString("end_date"),
                        rs.getInt("assigned_to"),
                        rs.getInt("created_by")
                ));

            }


        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return projects;
    }


    public int ReturnPrID(Project project) {
        String query = "INSERT INTO projects (name, description, progress, budget, start_date, end_date, assigned_to, created_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = databaseconnection.getConnection();
             PreparedStatement RID = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            RID.setString(1, project.getName());
            RID.setString(2, project.getDescription());
            RID.setInt(3, project.getProgress());
            RID.setDouble(4, project.getBudget());

            String startDateStr = project.getStartDate();
            if (startDateStr != null && startDateStr.length() == 10) {
                RID.setDate(5, Date.valueOf(startDateStr));
            } else {
                RID.setNull(5, Types.DATE);
            }

            String endDateStr = project.getEndDate();
            if (endDateStr != null && endDateStr.length() == 10) {
                RID.setDate(6, Date.valueOf(endDateStr));
            } else {
                RID.setNull(6, Types.DATE);
            }

            RID.setInt(7, project.getAssignedTo());
            RID.setInt(8, project.getCreatedby());
            RID.executeUpdate();
            ResultSet keys = RID.getGeneratedKeys();
            if (keys.next()) {
                return keys.getInt(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public void addTask(Task task) {
        String query = "INSERT INTO tasks (title, description, status, priority, start_date, due_date, project_id, assigned_to ,created_by) VALUES (?, ?, ?, ?, ?, ?, ?, ? ,?)";

        try (Connection conn = databaseconnection.getConnection();
             PreparedStatement adt = conn.prepareStatement(query)) {

            adt.setString(1, task.getTitle());
            adt.setString(2, task.getDescription());
            adt.setString(3, task.getStatus());
            adt.setString(4, task.getPriority());
            adt.setString(5, task.getStartDate()); // Ensure format YYYY-MM-DD
            adt.setString(6, task.getDueDate());
            adt.setInt(7, task.getProjectId());
            adt.setInt(8, task.getAssignedTo());
            adt.setInt(9, task.getCreatedby());

            adt.executeUpdate();
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

        try (Connection conn = databaseconnection.getConnection();
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
        try (Connection con = databaseconnection.getConnection();
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
        try (Connection conn = databaseconnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, projectId);
            pstmt.setString(2, status);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    public int getOverdueTaskCount(int projectId) {
        String query = "SELECT COUNT(*) FROM tasks " +
                "WHERE project_id = ? " +
                "AND due_date IS NOT NULL " +
                "AND DATE(due_date) < CURDATE() " +
                "AND UPPER(REPLACE(REPLACE(COALESCE(status, ''), ' ', '_'), '-', '_')) <> 'DONE'";
        try (Connection conn = databaseconnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, projectId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String query = "SELECT id, nom , prenom FROM utilisateurs"; // Adjust table/column names as needed
        try (Connection conn = databaseconnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                users.add(new User(rs.getInt("id"), rs.getString("nom"), rs.getString("prenom")));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return users;
    }

    private void ensureProjectAssignmentsTable() {
        String query = "CREATE TABLE IF NOT EXISTS project_assignments (" +
                "project_id INT NOT NULL, " +
                "user_id INT NOT NULL, " +
                "PRIMARY KEY (project_id, user_id)" +
                ")";
        try (Connection conn = databaseconnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(query);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void replaceProjectAssignments(int projectId, List<Integer> userIds) {
        ensureProjectAssignmentsTable();
        String deleteQuery = "DELETE FROM project_assignments WHERE project_id = ?";
        String insertQuery = "INSERT INTO project_assignments (project_id, user_id) VALUES (?, ?)";

        try (Connection conn = databaseconnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement deleteStmt = conn.prepareStatement(deleteQuery);
                 PreparedStatement insertStmt = conn.prepareStatement(insertQuery)) {
                deleteStmt.setInt(1, projectId);
                deleteStmt.executeUpdate();

                for (Integer userId : userIds) {
                    insertStmt.setInt(1, projectId);
                    insertStmt.setInt(2, userId);
                    insertStmt.addBatch();
                }
                insertStmt.executeBatch();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Integer> getProjectAssignmentUserIds(int projectId) {
        ensureProjectAssignmentsTable();
        List<Integer> userIds = new ArrayList<>();
        String query = "SELECT user_id FROM project_assignments WHERE project_id = ?";
        try (Connection conn = databaseconnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, projectId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                userIds.add(rs.getInt("user_id"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return userIds;
    }

    public Map<Integer, List<User>> getProjectAssigneesMap() {
        ensureProjectAssignmentsTable();
        Map<Integer, List<User>> result = new HashMap<>();
        String query = "SELECT p.id AS project_id, u.id AS user_id, u.nom, u.prenom " +
                "FROM projects p " +
                "LEFT JOIN project_assignments pa ON pa.project_id = p.id " +
                "LEFT JOIN utilisateurs u ON u.id = COALESCE(pa.user_id, p.assigned_to) " +
                "ORDER BY p.id";

        try (Connection conn = databaseconnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                int projectId = rs.getInt("project_id");
                result.computeIfAbsent(projectId, k -> new ArrayList<>());

                int userId = rs.getInt("user_id");
                if (rs.wasNull()) {
                    continue;
                }

                User user = new User(userId, rs.getString("nom"), rs.getString("prenom"));
                result.get(projectId).add(user);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    public int getTotalProjectsCount() {
        String query = "SELECT COUNT(*) FROM projects";
        try (Connection conn = databaseconnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int getTasksInProgressCount() {
        String query = "SELECT COUNT(*) FROM tasks " +
                "WHERE UPPER(REPLACE(REPLACE(COALESCE(status, ''), ' ', '_'), '-', '_')) = 'IN_PROGRESS'";
        try (Connection conn = databaseconnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int getCompletedTasksCount() {
        String query = "SELECT COUNT(*) FROM tasks " +
                "WHERE UPPER(REPLACE(REPLACE(COALESCE(status, ''), ' ', '_'), '-', '_')) = 'DONE'";
        try (Connection conn = databaseconnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            if (rs.next()) return rs.getInt(1);
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
        try (Connection conn = databaseconnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, daysAhead);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
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
        try (Connection conn = databaseconnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public boolean deleteProjectById(int projectId) {
        String deleteTasksSql = "DELETE FROM tasks WHERE project_id = ?";
        String deleteProjectSql = "DELETE FROM projects WHERE id = ?";

        try (Connection conn = databaseconnection.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement deleteTasksStmt = conn.prepareStatement(deleteTasksSql);
                 PreparedStatement deleteProjectStmt = conn.prepareStatement(deleteProjectSql)) {

                deleteTasksStmt.setInt(1, projectId);
                deleteTasksStmt.executeUpdate();

                deleteProjectStmt.setInt(1, projectId);
                int affectedRows = deleteProjectStmt.executeUpdate();

                conn.commit();
                return affectedRows > 0;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateProject(Project project) {
        String query = "UPDATE projects SET name = ?, description = ?, budget = ?, start_date = ?, end_date = ? WHERE id = ?";

        try (Connection conn = databaseconnection.getConnection();
             PreparedStatement upPro = conn.prepareStatement(query)) {
            upPro.setString(1, project.getName());
            upPro.setString(2, project.getDescription());
            upPro.setDouble(3, project.getBudget());

            String startDateStr = project.getStartDate();
            if (startDateStr != null && startDateStr.length() == 10) {
                upPro.setDate(4, Date.valueOf(startDateStr));
            } else {

                upPro.setNull(4, Types.DATE);
            }

            String endDateStr = project.getEndDate();
            if (endDateStr != null && endDateStr.length() == 10) {
                upPro.setDate(5, Date.valueOf(endDateStr));
            } else {
                upPro.setNull(5, Types.DATE);
            }

            upPro.setInt(6, project.getId());
            return upPro.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateTaskStatus(int taskId, String newStatus) {
        String query = "UPDATE tasks SET status = ? WHERE id = ?";
        try (Connection conn = databaseconnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, newStatus);
            pstmt.setInt(2, taskId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Task> getTasksByAssignedUser(int userId) {
        List<Task> tasks = new ArrayList<>();
        String query = "SELECT t.*, CONCAT(u.prenom, ' ', u.nom) as user_name " +
                "FROM tasks t " +
                "LEFT JOIN utilisateurs u ON t.assigned_to = u.id " +
                "WHERE t.assigned_to = ? " +
                "ORDER BY t.due_date IS NULL, t.due_date ASC, t.id DESC";

        try (Connection conn = databaseconnection.getConnection();
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
        try (Connection conn = databaseconnection.getConnection();
             PreparedStatement UpTa = conn.prepareStatement(query)) {
            UpTa.setString(1, task.getTitle());
            UpTa.setString(2, task.getDescription());
            UpTa.setString(3, task.getStatus());
            UpTa.setString(4, task.getPriority());
            UpTa.setString(5, task.getDueDate());
            UpTa.setInt(6, task.getAssignedTo());
            UpTa.setInt(7, task.getId());
            return UpTa.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteTaskById(int taskId) {
        String query = "DELETE FROM tasks WHERE id = ?";
        try (Connection conn = databaseconnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, taskId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
