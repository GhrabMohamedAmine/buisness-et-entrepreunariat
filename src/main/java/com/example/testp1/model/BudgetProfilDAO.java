package com.example.testp1.model;

import com.example.testp1.entities.BudgetProfil;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.Year;

public class BudgetProfilDAO {
    private final Connection connection;

    public BudgetProfilDAO() {
        this.connection = DB.getInstance().getConx();
    }

    public void add(BudgetProfil p) {
        String req = "INSERT INTO budget_profile (fiscal_year, budget_disposable, total_expense, margin_profit, base_currency, start_date, end_date, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(req)) {
            ps.setInt(1, p.getFiscalYear().getValue());
            ps.setBigDecimal(2, p.getBudgetDisposable());
            ps.setBigDecimal(3, p.getTotalExpense());
            if (p.getMarginProfit() != null) {
                ps.setDouble(4, p.getMarginProfit());
            } else {
                ps.setNull(4, Types.DOUBLE);
            }
            ps.setString(5, p.getBaseCurrency());
            ps.setDate(6, p.getStartDate() != null ? Date.valueOf(p.getStartDate()) : null);
            ps.setDate(7, p.getEndDate() != null ? Date.valueOf(p.getEndDate()) : null);
            ps.setString(8, p.getStatus());
            ps.executeUpdate();
            System.out.println("Budget Profile added successfully.");
        } catch (SQLException e) {
            System.err.println("SQL Error (Add Profil): " + e.getMessage());
        }
    }


    public BudgetProfil getById(int id) {
        String req = "SELECT * FROM budget_profile WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(req)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToBudgetProfil(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("SQL Error (GetById): " + e.getMessage());
        }
        return null;
    }

    public BudgetProfil getActiveProfile() {
        String req = "SELECT * FROM budget_profile WHERE status = 'ACTIVE' LIMIT 1";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(req)) {
            if (rs.next()) {
                return mapResultSetToBudgetProfil(rs);
            }
        } catch (SQLException e) {
            System.err.println("SQL Error (GetActiveProfile): " + e.getMessage());
        }
        return null;
    }

    public java.util.List<BudgetProfil> getAllProfiles() {
        java.util.List<BudgetProfil> profiles = new java.util.ArrayList<>();
        String req = "SELECT * FROM budget_profile";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(req)) {
            while (rs.next()) {
                profiles.add(mapResultSetToBudgetProfil(rs));
            }
        } catch (SQLException e) {
            System.err.println("SQL Error (GetAllProfiles): " + e.getMessage());
        }
        return profiles;
    }

    private BudgetProfil mapResultSetToBudgetProfil(ResultSet rs) throws SQLException {
        Date startSql = rs.getDate("start_date");
        Date endSql = rs.getDate("end_date");
        
        return new BudgetProfil(
                rs.getInt("id"),
                Year.of(rs.getInt("fiscal_year")),
                rs.getBigDecimal("budget_disposable"),
                rs.getBigDecimal("total_expense"),
                rs.getDouble("margin_profit"),
                rs.getString("base_currency"),
                startSql != null ? startSql.toLocalDate() : null,
                endSql != null ? endSql.toLocalDate() : null,
                rs.getString("status")
        );
    }

    public void update(BudgetProfil p) {
        String req = "UPDATE budget_profile SET fiscal_year=?, budget_disposable=?, total_expense=?, margin_profit=?, base_currency=?, start_date=?, end_date=?, status=? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(req)) {
            ps.setInt(1, p.getFiscalYear().getValue());
            ps.setBigDecimal(2, p.getBudgetDisposable());
            ps.setBigDecimal(3, p.getTotalExpense());
            if (p.getMarginProfit() != null) {
                ps.setDouble(4, p.getMarginProfit());
            } else {
                ps.setNull(4, Types.DOUBLE);
            }
            ps.setString(5, p.getBaseCurrency());
            ps.setDate(6, p.getStartDate() != null ? Date.valueOf(p.getStartDate()) : null);
            ps.setDate(7, p.getEndDate() != null ? Date.valueOf(p.getEndDate()) : null);
            ps.setString(8, p.getStatus());
            ps.setInt(9, p.getId());
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
        String checkSql = "SELECT COUNT(*) FROM budget_profile WHERE status = 'ACTIVE'";

        try (Statement st = connection.createStatement();
             ResultSet rsCheck = st.executeQuery(checkSql)) {

            if (rsCheck.next() && rsCheck.getInt(1) > 0) {
                String sumSql = "SELECT COALESCE(SUM(actualSpend), 0.0) as grand_total FROM project_budget";
                String updateSql = "UPDATE budget_profile SET total_expense = ? WHERE status = 'ACTIVE'";

                try (ResultSet rsSum = st.executeQuery(sumSql)) {
                    if (rsSum.next()) {
                        BigDecimal grandTotal = rsSum.getBigDecimal("grand_total");
                        try (PreparedStatement pstmt = connection.prepareStatement(updateSql)) {
                            pstmt.setBigDecimal(1, grandTotal);
                            pstmt.executeUpdate();
                            System.out.println("Sync Successful: Profile updated with $" + grandTotal);
                        }
                    }
                }
            } else {
                System.out.println("Sync Skipped: No active budget profile found.");
            }
        } catch (SQLException e) {
            System.err.println("Sync Error: " + e.getMessage());
            throw e;
        }
    }
}