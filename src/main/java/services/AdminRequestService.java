package services;

import entities.ResourceAssignment;
import utils.database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AdminRequestService {

    private final Connection cnx;

    public AdminRequestService() {
        cnx = database.getInstance().getConnection();
    }

    public List<ResourceAssignment> getPendingRequests() throws SQLException {
        List<ResourceAssignment> list = new ArrayList<>();

        String sql =
                "SELECT ra.assignment_id, ra.resource_id, ra.project_code, ra.client_code, ra.quantity, " +
                        "       ra.assignment_date, ra.total_cost, ra.status, " +
                        "       r.resource_name, r.resource_type " +
                        "FROM resource_assignment ra " +
                        "JOIN resources r ON r.resource_id = ra.resource_id " +
                        "WHERE UPPER(ra.status) = 'PENDING' " +
                        "ORDER BY ra.assignment_date DESC";

        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {
            ResourceAssignment a = new ResourceAssignment();
            a.setAssignmentId(rs.getInt("assignment_id"));
            a.setResourceId(rs.getInt("resource_id"));
            a.setProjectCode(rs.getString("project_code"));
            a.setClientCode(rs.getString("client_code"));
            a.setQuantity(rs.getInt("quantity"));
            a.setAssignmentDate(rs.getDate("assignment_date"));
            a.setTotalCost(rs.getDouble("total_cost"));
            a.setStatus(rs.getString("status"));
            a.setResourceName(rs.getString("resource_name"));
            a.setResourceType(rs.getString("resource_type"));
            list.add(a);
        }

        return list;
    }

    public void declineRequest(int assignmentId) throws SQLException {
        String sql = "UPDATE resource_assignment SET status='DECLINED' WHERE assignment_id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, assignmentId);
        ps.executeUpdate();
    }

    public void acceptRequest(int assignmentId) throws SQLException {
        // Transaction: verify availability + update assignment + reduce stock
        cnx.setAutoCommit(false);

        try {
            // 1) Get the request details
            String q1 = "SELECT resource_id, quantity FROM resource_assignment WHERE assignment_id=? FOR UPDATE";
            PreparedStatement ps1 = cnx.prepareStatement(q1);
            ps1.setInt(1, assignmentId);
            ResultSet rs1 = ps1.executeQuery();

            if (!rs1.next()) throw new SQLException("Request not found.");

            int resourceId = rs1.getInt("resource_id");
            int qty = rs1.getInt("quantity");

            // 2) Check current available quantity
            String q2 = "SELECT available_quantity FROM resources WHERE resource_id=? FOR UPDATE";
            PreparedStatement ps2 = cnx.prepareStatement(q2);
            ps2.setInt(1, resourceId);
            ResultSet rs2 = ps2.executeQuery();

            if (!rs2.next()) throw new SQLException("Resource not found.");

            int available = rs2.getInt("available_quantity");
            if (qty > available) {
                throw new SQLException("Not enough stock. Available: " + available + ", requested: " + qty);
            }

            // 3) Update assignment status
            String q3 = "UPDATE resource_assignment SET status='ACCEPTED' WHERE assignment_id=?";
            PreparedStatement ps3 = cnx.prepareStatement(q3);
            ps3.setInt(1, assignmentId);
            ps3.executeUpdate();

            // 4) Reduce available quantity
            String q4 = "UPDATE resources SET available_quantity = available_quantity - ? WHERE resource_id=?";
            PreparedStatement ps4 = cnx.prepareStatement(q4);
            ps4.setInt(1, qty);
            ps4.setInt(2, resourceId);
            ps4.executeUpdate();

            cnx.commit();

        } catch (SQLException e) {
            cnx.rollback();
            throw e;
        } finally {
            cnx.setAutoCommit(true);
        }
    }
}
