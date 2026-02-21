package services;

import entities.ResourceAssignment;
import utils.database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AssignmentService {

    private final Connection cnx;

    public AssignmentService() {
        cnx = database.getInstance().getConnection();
    }

    public List<ResourceAssignment> getByClient(String clientCode) throws SQLException {

        List<ResourceAssignment> list = new ArrayList<>();

        String sql =
                "SELECT ra.assignment_id, ra.resource_id, ra.project_code, ra.client_code, " +
                        "       ra.quantity, ra.assignment_date, ra.return_date, ra.total_cost, ra.status, " +
                        "       r.resource_name, r.resource_type " +
                        "FROM resource_assignment ra " +
                        "JOIN resources r ON r.resource_id = ra.resource_id " +
                        "WHERE ra.client_code = ? " +
                        "ORDER BY ra.assignment_date DESC";

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, clientCode);

        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            ResourceAssignment a = new ResourceAssignment();

            a.setAssignmentId(rs.getInt("assignment_id"));
            a.setResourceId(rs.getInt("resource_id"));
            a.setProjectCode(rs.getString("project_code"));
            a.setClientCode(rs.getString("client_code"));
            a.setQuantity(rs.getInt("quantity"));
            a.setAssignmentDate(rs.getDate("assignment_date"));
            a.setReturnDate(rs.getDate("return_date"));
            a.setTotalCost(rs.getDouble("total_cost"));
            a.setStatus(rs.getString("status"));

            a.setResourceName(rs.getString("resource_name"));
            a.setResourceType(rs.getString("resource_type"));

            list.add(a);
        }

        return list;
    }

    public void requestResource(int resourceId, String projectCode, String clientCode, int quantity, double totalCost) throws SQLException {

        String sql =
                "INSERT INTO resource_assignment " +
                        "(resource_id, project_code, client_code, quantity, assignment_date, total_cost, status) " +
                        "VALUES (?, ?, ?, ?, CURDATE(), ?, 'PENDING')";

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, resourceId);
        ps.setString(2, projectCode);
        ps.setString(3, clientCode);
        ps.setInt(4, quantity);
        ps.setDouble(5, totalCost);

        ps.executeUpdate();
    }

    // ✅ DELETE request/assignment
    public void delete(int assignmentId) throws SQLException {
        String sql = "DELETE FROM resource_assignment WHERE assignment_id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, assignmentId);
        ps.executeUpdate();
    }

    // ✅ UPDATE request (client can update only when PENDING/DECLINED)
    public void updateRequest(int assignmentId, int resourceId, String projectCode, int quantity, double totalCost) throws SQLException {
        String sql =
                "UPDATE resource_assignment SET " +
                        "resource_id = ?, project_code = ?, quantity = ?, total_cost = ?, status = 'PENDING' " +
                        "WHERE assignment_id = ?";

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, resourceId);
        ps.setString(2, projectCode);
        ps.setInt(3, quantity);
        ps.setDouble(4, totalCost);
        ps.setInt(5, assignmentId);

        ps.executeUpdate();
    }
    public void adminUpdateStatus(int assignmentId, String newStatus) throws SQLException {

        // 1) Get details for SMS (client_code + resource_name + qty)
        String detailsSql =
                "SELECT ra.client_code, ra.quantity, r.resource_name " +
                        "FROM resource_assignment ra " +
                        "JOIN resources r ON r.resource_id = ra.resource_id " +
                        "WHERE ra.assignment_id = ?";

        String clientCode = null;
        int qty = 0;
        String resourceName = null;

        try (PreparedStatement ps = cnx.prepareStatement(detailsSql)) {
            ps.setInt(1, assignmentId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                clientCode = rs.getString("client_code");
                qty = rs.getInt("quantity");
                resourceName = rs.getString("resource_name");
            } else {
                throw new SQLException("Assignment not found (id=" + assignmentId + ")");
            }
        }

        // 2) Update status
        String updateSql = "UPDATE resource_assignment SET status = ? WHERE assignment_id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(updateSql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, assignmentId);
            ps.executeUpdate();
        }

        // 3) Send SMS (mock API)
        try {
            UserService userService = new UserService();
            String phone = userService.getPhoneByClientCode(clientCode);

            if (phone != null && !phone.isBlank()) {
                services.notifications.SmsService sms = new services.notifications.SmsService();

                String msg;
                String st = newStatus == null ? "" : newStatus.trim().toUpperCase();

                if (st.equals("ACCEPTED")) {
                    msg = "✅ Approved: " + resourceName + " (Qty: " + qty + ").";
                } else if (st.equals("DECLINED")) {
                    msg = "❌ Declined: " + resourceName + " (Qty: " + qty + "). Please adjust and try again.";
                } else {
                    msg = "ℹ️ Update: Your request for " + resourceName + " (Qty: " + qty + ") is now " + st + ".";
                }

                //sms.send(phone, msg);
            } else {
                System.out.println("[SMS API] No phone found for client_code=" + clientCode);
            }

        } catch (SQLException ex) {
            // don't fail the update if SMS fails
            System.out.println("[SMS API] Failed to send SMS: " + ex.getMessage());
        }
    }


}
