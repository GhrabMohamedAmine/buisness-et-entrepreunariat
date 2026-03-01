package services;

import utils.database;

import java.sql.*;

public class AutoApprovalEngine {

    private final Connection cnx;

    public AutoApprovalEngine() {
        cnx = database.getInstance().getConnection();
    }

    public enum Decision { ACCEPTED, DECLINED, PENDING }

    // ✅ Use userId instead of clientCode
    public Decision decideForPhysicalRequest(int userId) throws SQLException {
        int score = getUserScore(userId);

        if (score <= 0) return Decision.DECLINED;

        int acceptedCount = countAcceptedAssignments(userId);

        if (score >= 100 && acceptedCount >= 3) return Decision.ACCEPTED;

        return Decision.PENDING;
    }

    // ✅ user score by utilisateur.id
    private int getUserScore(int userId) throws SQLException {
        String sql = "SELECT score FROM utilisateurs WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("User not found for id=" + userId);
                return rs.getInt("score");
            }
        }
    }

    // ✅ count accepted assignments by resource_assignment.user_id
    private int countAcceptedAssignments(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) c FROM resource_assignment WHERE user_id=? AND UPPER(status)='ACCEPTED'";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("c") : 0;
            }
        }
    }
}