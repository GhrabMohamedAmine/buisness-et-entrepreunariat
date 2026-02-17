package com.example.testp1.model;

import com.example.testp1.model.DB;
import com.example.testp1.entities.ProjectBudget;
import com.example.testp1.services.ServiceBudgetProfil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProjectBudgetDAO {

    private final Connection connection;

    public ProjectBudgetDAO() {
        this.connection = DB.getInstance().getConx();
    }

    public void add(ProjectBudget budget) {
        // Matches your schema: name, total_budget, actualSpend, status, dueDate, projectId
        String req = "INSERT INTO project_budget (name, total_budget, actualSpend, status, dueDate, projectId) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(req)) {
            ps.setString(1, budget.getName());
            ps.setDouble(2, budget.getTotalBudget());
            ps.setDouble(3, budget.getActualSpend());
            ps.setString(4, budget.getStatus());
            ps.setDate(5, Date.valueOf(budget.getDueDate()));
            ps.setInt(6, budget.getProjectId());

            ps.executeUpdate();
            System.out.println("Budget record created successfully.");
        } catch (SQLException e) {
            System.err.println("SQL Error (Add): " + e.getMessage());
        }
    }

    public List<ProjectBudget> getAll() {
        List<ProjectBudget> budgets = new ArrayList<>();
        String req = "SELECT * FROM project_budget"; // No JOIN needed as name is in the table

        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(req)) {
            while (rs.next()) {
                ProjectBudget budget = new ProjectBudget();
                budget.setId(rs.getInt("id"));
                budget.setName(rs.getString("name"));
                budget.setTotalBudget(rs.getDouble("total_budget"));
                budget.setActualSpend(rs.getDouble("actualSpend"));
                budget.setStatus(rs.getString("status"));
                budget.setProjectId(rs.getInt("projectId"));

                if (rs.getDate("dueDate") != null) {
                    budget.setDueDate(rs.getDate("dueDate").toLocalDate());
                }

                budgets.add(budget);
            }
        } catch (SQLException e) {
            System.err.println("SQL Error (GetAll): " + e.getMessage());
        }
        return budgets;
    }

    public void update(ProjectBudget budget) throws SQLException {
        String req = "UPDATE project_budget SET total_budget=?, actualSpend=?, dueDate=?, projectId=? WHERE id=?";

        try (PreparedStatement ps = connection.prepareStatement(req)) {
            ps.setDouble(1, budget.getTotalBudget());
            ps.setDouble(2, budget.getActualSpend());
            ps.setDate(3, Date.valueOf(budget.getDueDate()));
            ps.setInt(4, budget.getProjectId());
            ps.setInt(5, budget.getId());

            ps.executeUpdate();

            syncProjectBudgetStatus(budget.getId());

            System.out.println("Budget record and status updated successfully.");

        } catch (SQLException e) {
            System.err.println("SQL Error (Update): " + e.getMessage());
            throw e;
        }
    }

    public void delete(int id) {
        String deleteBudgetReq = "DELETE FROM project_budget WHERE id=?";
        String deleteTransactionsReq = "DELETE FROM transaction WHERE project_budget_id = ?";
        try {

            connection.setAutoCommit(false);


            try (PreparedStatement psTrans = connection.prepareStatement(deleteTransactionsReq)) {
                psTrans.setInt(1, id);
                psTrans.executeUpdate();
            }


            try (PreparedStatement psBudget = connection.prepareStatement(deleteBudgetReq)) {
                psBudget.setInt(1, id);
                psBudget.executeUpdate();
                System.out.println("Budget and all its transactions deleted successfully.");
            }

            // 5. Commit the changes
            ServiceBudgetProfil profileService = new ServiceBudgetProfil();
            profileService.syncdata();
            connection.commit();

        } catch (SQLException e) {
            // If anything fails, undo everything to prevent partial deletion
            try { connection.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            System.err.println("SQL Error (Cascade Delete): " + e.getMessage());
        } finally {
            try { connection.setAutoCommit(true); } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    public ProjectBudget getById(int id) {
        String req = "SELECT * FROM project_budget WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(req)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ProjectBudget budget = new ProjectBudget();
                    budget.setId(rs.getInt("id"));
                    budget.setName(rs.getString("name"));
                    budget.setTotalBudget(rs.getDouble("total_budget"));
                    budget.setActualSpend(rs.getDouble("actualSpend"));
                    budget.setStatus(rs.getString("status"));
                    budget.setProjectId(rs.getInt("projectId"));
                    if (rs.getDate("dueDate") != null) {
                        budget.setDueDate(rs.getDate("dueDate").toLocalDate());
                    }
                    return budget;
                }
            }
        } catch (SQLException e) {
            System.err.println("SQL Error (GetBudgetById): " + e.getMessage());
        }
        return null;
    }

    public void syncProjectBudgetStatus(int projectId) throws SQLException {
        String query = "UPDATE project_budget " +
                "SET status = CASE " +
                "    WHEN actualSpend > total_budget THEN 'OVER BUDGET' " +
                "    WHEN actualSpend >= (total_budget * 0.9) THEN 'AT RISK' " +
                "    ELSE 'ON TRACK' " +
                "END " +
                "WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, projectId);
            pstmt.executeUpdate();
            System.out.println("Project Status synced (incl. At Risk) for ID: " + projectId);
        }
    }

    public java.time.LocalDate getDueDateById(int budgetId) {
        String query = "SELECT dueDate FROM project_budget WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, budgetId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    java.sql.Date sqlDate = rs.getDate("dueDate");
                    return (sqlDate != null) ? sqlDate.toLocalDate() : null;
                }
            }
        } catch (SQLException e) {
            System.err.println("SQL Error (getDueDateById): " + e.getMessage());
        }
        return null;
    }
}