package services;

import utils.database;

import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public class AnalyticsService {

    private final Connection cnx;

    public AnalyticsService() {
        cnx = database.getInstance().getConnection();
    }

    // -------------------- 1) List all resource types --------------------
    public List<String> getAllTypes() throws SQLException {
        List<String> types = new ArrayList<>();
        String sql = "SELECT DISTINCT resource_type FROM resources ORDER BY resource_type";
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) types.add(rs.getString("resource_type"));
        }
        return types;
    }

    // -------------------- 2) Monthly usage for a given type --------------------
    // counts total requested/assigned quantity per month
    public List<MonthlyUsage> getMonthlyUsageByType(String type, int lastMonths) throws SQLException {

        List<MonthlyUsage> list = new ArrayList<>();

        // We only count ACCEPTED (real usage). If your DB uses ACTIVE instead, change it.
        String sql =
                "SELECT YEAR(ra.assignment_date) y, MONTH(ra.assignment_date) m, SUM(ra.quantity) total_qty " +
                        "FROM resource_assignment ra " +
                        "JOIN resources r ON r.resource_id = ra.resource_id " +
                        "WHERE r.resource_type = ? " +
                        "  AND ra.assignment_date >= DATE_SUB(CURDATE(), INTERVAL ? MONTH) " +
                        "  AND UPPER(ra.status) = 'ACCEPTED' " +
                        "GROUP BY y, m " +
                        "ORDER BY y, m";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, type);
            ps.setInt(2, lastMonths);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int y = rs.getInt("y");
                    int m = rs.getInt("m");
                    int qty = rs.getInt("total_qty");
                    String monthLabel = String.format("%04d-%02d", y, m);
                    list.add(new MonthlyUsage(monthLabel, qty));
                }
            }
        }

        return list;
    }

    // -------------------- 3) Available stock by type --------------------
    public int getAvailableQuantityByType(String type) throws SQLException {
        String sql = "SELECT COALESCE(SUM(available_quantity), 0) av " +
                "FROM resources WHERE resource_type = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, type);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("av");
            }
        }
        return 0;
    }

    // -------------------- 4) Forecast (moving average last 3 months) --------------------
    public TypeForecastRow forecastType(String type) throws SQLException {

        List<MonthlyUsage> usage = getMonthlyUsageByType(type, 6); // get up to 6 months history
        int predicted = movingAverageLast3(usage);

        int available = getAvailableQuantityByType(type);

        String risk;
        if (predicted <= 0) risk = "LOW";
        else if (predicted > available) risk = "HIGH";
        else risk = "MEDIUM";

        return new TypeForecastRow(type, available, predicted, risk);
    }

    public List<TypeForecastRow> forecastAllTypes() throws SQLException {
        List<TypeForecastRow> rows = new ArrayList<>();
        for (String t : getAllTypes()) rows.add(forecastType(t));

        // sort: HIGH first, then by predicted desc
        rows.sort((a, b) -> {
            int ra = riskRank(a.getRisk());
            int rb = riskRank(b.getRisk());
            if (ra != rb) return Integer.compare(rb, ra);
            return Integer.compare(b.getPredictedDemand(), a.getPredictedDemand());
        });

        return rows;
    }

    private int riskRank(String r) {
        if ("HIGH".equalsIgnoreCase(r)) return 3;
        if ("MEDIUM".equalsIgnoreCase(r)) return 2;
        return 1;
    }

    private int movingAverageLast3(List<MonthlyUsage> usage) {
        if (usage == null || usage.isEmpty()) return 0;

        int n = usage.size();
        int start = Math.max(0, n - 3);
        int sum = 0;
        int count = 0;
        for (int i = start; i < n; i++) {
            sum += usage.get(i).getQuantity();
            count++;
        }
        return count == 0 ? 0 : (int) Math.round(sum / (double) count);
    }
}