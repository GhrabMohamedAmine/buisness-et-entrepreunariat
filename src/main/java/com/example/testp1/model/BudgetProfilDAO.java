package com.example.testp1.model;

import com.example.testp1.entities.BudgetProfil;
import java.sql.*;
import java.time.Year;

public class BudgetProfilDAO {
    private final Connection connection;

    public BudgetProfilDAO() {
        this.connection = DB.getInstance().getConx();
    }

    public void add(BudgetProfil p) {
        // Check if a profile already exists
        String countReq = "SELECT COUNT(*) FROM budget_profile";

        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(countReq)) {

            if (rs.next() && rs.getInt(1) > 0) {
                System.err.println("Aborted: A Budget Profile already exists. Use update instead.");
                return;
            }

            String req = "INSERT INTO budget_profile (fiscal_year, budget_disposable, total_expense, margin_profit) VALUES (?, ?, ?, ?)";
            try (PreparedStatement ps = connection.prepareStatement(req)) {
                ps.setInt(1, p.getFiscalYear().getValue());
                ps.setDouble(2, p.getBudgetDisposable());
                ps.setDouble(3, p.getTotalExpense());
                ps.setFloat(4, p.getMarginProfit());
                ps.executeUpdate();
                System.out.println("Single Budget Profile initialized successfully.");
            }
        } catch (SQLException e) {
            System.err.println("SQL Error (Add Profil): " + e.getMessage());
        }
    }


    public BudgetProfil getActiveProfile() {
        String req = "SELECT * FROM budget_profile LIMIT 1";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(req)) {
            if (rs.next()) {
                return new BudgetProfil(
                        rs.getInt("id"),
                        Year.of(rs.getInt("fiscal_year")),
                        rs.getDouble("budget_disposable"),
                        rs.getDouble("total_expense"),
                        rs.getFloat("margin_profit")
                );
            }
        } catch (SQLException e) {
            System.err.println("SQL Error (GetActiveProfile): " + e.getMessage());
        }
        return null;
    }

    public void update(BudgetProfil p) {
        String req = "UPDATE budget_profile SET fiscal_year=?, budget_disposable=?, total_expense=?, margin_profit=? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(req)) {
            ps.setInt(1, p.getFiscalYear().getValue());
            ps.setDouble(2, p.getBudgetDisposable());
            ps.setDouble(3, p.getTotalExpense());
            ps.setFloat(4, p.getMarginProfit());
            ps.setInt(5, p.getId());
            ps.executeUpdate();
            syncProfileTotalExpense();
            System.out.println("Budget Profile updated.");
        } catch (SQLException e) {
            System.err.println("SQL Error (Update): " + e.getMessage());
        }
    }

    public void delete(int id) {
        String req = "DELETE FROM budget_profile WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(req)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("SQL Error (Delete): " + e.getMessage());
        }
    }

    public void syncProfileTotalExpense() throws SQLException {

        String checkSql = "SELECT COUNT(*) FROM budget_profile";

        try (Statement st = connection.createStatement();
             ResultSet rsCheck = st.executeQuery(checkSql)) {

            if (rsCheck.next() && rsCheck.getInt(1) > 0) {

                String sumSql = "SELECT COALESCE(SUM(actualSpend), 0.0) as grand_total FROM project_budget";
                String updateSql = "UPDATE budget_profile SET total_expense = ?";

                try (ResultSet rsSum = st.executeQuery(sumSql)) {
                    if (rsSum.next()) {
                        double grandTotal = rsSum.getDouble("grand_total");
                        try (PreparedStatement pstmt = connection.prepareStatement(updateSql)) {
                            pstmt.setDouble(1, grandTotal);
                            pstmt.executeUpdate();
                            System.out.println("Sync Successful: Profile updated with $" + grandTotal);
                        }
                    }
                }
            } else {

                System.out.println("Sync Skipped: No budget profile found. User needs to 'Set Profile' first.");
            }
        } catch (SQLException e) {
            System.err.println("Sync Error: " + e.getMessage());
            throw e;
        }
    }


}