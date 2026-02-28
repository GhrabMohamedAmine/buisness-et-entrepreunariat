package services;

import utils.database;

import java.sql.*;

public class AutoApprovalEngine {

    private final Connection cnx;

    public AutoApprovalEngine() {
        cnx = database.getInstance().getConnection();
    }

    public enum Decision { ACCEPTED, DECLINED, PENDING }

    public Decision decideForPhysicalRequest(String clientCode) throws SQLException {
        int score = getClientScore(clientCode);

        if (score <= 0) return Decision.DECLINED;

        int acceptedCount = countAcceptedAssignments(clientCode);

        if (score >= 100 && acceptedCount >= 3) return Decision.ACCEPTED;

        return Decision.PENDING;
    }

    private int getClientScore(String clientCode) throws SQLException {
        String sql = "SELECT score FROM utilisateurs WHERE client_code = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, clientCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("Client not found for client_code=" + clientCode);
                return rs.getInt("score");
            }
        }
    }

    private int countAcceptedAssignments(String clientCode) throws SQLException {
        String sql = "SELECT COUNT(*) c FROM resource_assignment WHERE client_code=? AND UPPER(status)='ACCEPTED'";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, clientCode);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("c") : 0;
            }
        }
    }
}