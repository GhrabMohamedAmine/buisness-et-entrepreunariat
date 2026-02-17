package com.example.testp1.model;

import com.example.testp1.entities.ProjectBudget;
import com.example.testp1.entities.Transaction;
import com.example.testp1.services.ServiceBudgetProfil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TransactionDAO {
    private final Connection connection;
    private final ProjectBudgetDAO projectBudgetDAO = new ProjectBudgetDAO();

    public TransactionDAO() {
        this.connection = DB.getInstance().getConx();
    }

    public void add(Transaction t) {
        String insertReq = "INSERT INTO transaction (reference, cost, date_stamp, expense_category, project_budget_id, description) VALUES (?, ?, ?, ?, ?, ?)";
        // We stick to your 'actualSpend' attribute
        String updateBudgetReq = "UPDATE project_budget SET actualSpend = actualSpend + ? WHERE id = ?";

        try {
            connection.setAutoCommit(false);

            // 1. Insert the Transaction
            try (PreparedStatement ps = connection.prepareStatement(insertReq)) {
                ps.setString(1, t.getReference());
                ps.setDouble(2, t.getCost());
                ps.setDate(3, Date.valueOf(t.getDateStamp()));
                ps.setString(4, t.getExpenseCategory());
                ps.setInt(5, t.getProjectBudgetId());
                ps.setInt(6, t.getDescription());
                ps.executeUpdate();
            }

            // 2. Increase the 'actualSpend' total
            try (PreparedStatement psUpdate = connection.prepareStatement(updateBudgetReq)) {
                psUpdate.setDouble(1, t.getCost());
                psUpdate.setInt(2, t.getProjectBudgetId());
                psUpdate.executeUpdate();
            }

            projectBudgetDAO.syncProjectBudgetStatus(t.getProjectBudgetId());
            ServiceBudgetProfil profileService = new ServiceBudgetProfil();
            profileService.syncdata();

            connection.commit();
            System.out.println("Transaction added and actualSpend updated.");

        } catch (SQLException e) {
            try { connection.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            System.err.println("SQL Error: " + e.getMessage());
        } finally {
            try { connection.setAutoCommit(true); } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    public List<Transaction> getAll() {
        List<Transaction> list = new ArrayList<>();
        String req = "SELECT * FROM transaction"; // Singular table name
        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(req)) {
            while (rs.next()) {
                list.add(new Transaction(
                        rs.getInt("id"), // IDs as int
                        rs.getString("reference"),
                        rs.getDouble("cost"),
                        rs.getDate("date_stamp").toLocalDate(),
                        rs.getString("expense_category"),
                        rs.getInt("project_budget_id"),
                        rs.getInt("description")
                ));
            }
        } catch (SQLException e) {
            System.err.println("SQL Error (GetAll): " + e.getMessage());
        }
        return list;
    }

    public List<Transaction> getByBudgetId(int budgetId) {
        List<Transaction> list = new ArrayList<>();
        // Use a Prepared Statement for filtering by budget_id
        String req = "SELECT * FROM transaction WHERE project_budget_id = ?";

        try (PreparedStatement pst = connection.prepareStatement(req)) {
            pst.setInt(1, budgetId);

            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    list.add(new Transaction(
                            rs.getInt("id"),
                            rs.getString("reference"),
                            rs.getDouble("cost"),
                            rs.getDate("date_stamp").toLocalDate(),
                            rs.getString("expense_category"),
                            rs.getInt("project_budget_id"),
                            rs.getInt("description")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("SQL Error (GetByBudgetId): " + e.getMessage());
        }
        return list;
    }

    public void update(Transaction newTransaction) {
        // Use the ID from the passed object to get the OLD version from the DB
        Transaction oldTransaction = getById(newTransaction.getId());
        if (oldTransaction == null) return;

        // Calculate the change in cost
        double costDifference = newTransaction.getCost() - oldTransaction.getCost();

        String updateReq = "UPDATE transaction SET reference=?, cost=?, date_stamp=?, expense_category=?, project_budget_id=?, description=? WHERE id=?";
        String updateBudgetReq = "UPDATE project_budget SET actualSpend = actualSpend + ? WHERE id = ?";

        try {
            connection.setAutoCommit(false);

            try (PreparedStatement ps = connection.prepareStatement(updateReq)) {
                ps.setString(1, newTransaction.getReference());
                ps.setDouble(2, newTransaction.getCost());
                ps.setDate(3, java.sql.Date.valueOf(newTransaction.getDateStamp()));
                ps.setString(4, newTransaction.getExpenseCategory());
                ps.setInt(5, newTransaction.getProjectBudgetId());
                ps.setInt(6, newTransaction.getDescription());
                ps.setInt(7, newTransaction.getId());
                ps.executeUpdate();
            }

            try (PreparedStatement psUpdate = connection.prepareStatement(updateBudgetReq)) {
                psUpdate.setDouble(1, costDifference);
                psUpdate.setInt(2, newTransaction.getProjectBudgetId());
                psUpdate.executeUpdate();
            }
            projectBudgetDAO.syncProjectBudgetStatus(newTransaction.getProjectBudgetId());
            ServiceBudgetProfil profileService = new ServiceBudgetProfil();
            profileService.syncdata();

            connection.commit();
        } catch (SQLException e) {
            try { connection.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            System.err.println("Update Error: " + e.getMessage());
        } finally {
            try { connection.setAutoCommit(true); } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    public void delete(int id) {
        // 1. We must fetch the transaction first to know how much to subtract from actualSpend
        Transaction t = getById(id);
        if (t == null) {
            System.err.println("Delete failed: Transaction not found.");
            return;
        }

        String deleteReq = "DELETE FROM transaction WHERE id=?";
        String updateBudgetReq = "UPDATE project_budget SET actualSpend = actualSpend - ? WHERE id = ?";

        try {
            connection.setAutoCommit(false);

            // 2. Delete the row
            try (PreparedStatement ps = connection.prepareStatement(deleteReq)) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }

            // 3. Update the budget total
            try (PreparedStatement psUpdate = connection.prepareStatement(updateBudgetReq)) {
                psUpdate.setDouble(1, t.getCost());
                psUpdate.setInt(2, t.getProjectBudgetId());
                psUpdate.executeUpdate();
            }

            projectBudgetDAO.syncProjectBudgetStatus(t.getProjectBudgetId());
            ServiceBudgetProfil profileService = new ServiceBudgetProfil();
            profileService.syncdata();

            connection.commit();
            System.out.println("Transaction deleted and budget updated.");
        } catch (SQLException e) {
            try { connection.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            System.err.println("SQL Error: " + e.getMessage());
        } finally {
            try { connection.setAutoCommit(true); } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    public Transaction getById(int id) {
        String req = "SELECT * FROM transaction WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(req)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Transaction(
                            rs.getInt("id"),
                            rs.getString("reference"),
                            rs.getDouble("cost"),
                            rs.getDate("date_stamp").toLocalDate(),
                            rs.getString("expense_category"),
                            rs.getInt("project_budget_id"),
                            rs.getInt("description")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("SQL Error (GetById): " + e.getMessage());
        }
        return null;
    }
}