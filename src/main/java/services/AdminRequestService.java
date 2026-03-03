package services;

import entities.ResourceAssignment;
import services.notifications.SmsService;
import utils.database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AdminRequestService {

    private final Connection cnx;

    public AdminRequestService() {
        cnx = database.getInstance().getConnection();
    }

    // ================= GET PENDING =================
    public List<ResourceAssignment> getPendingRequests() throws SQLException {
        List<ResourceAssignment> list = new ArrayList<>();

        String sql =
                "SELECT ra.assignment_id, ra.resource_id, ra.project_code, ra.user_id, ra.quantity, " +
                        "       ra.assignment_date, ra.total_cost, ra.status, " +
                        "       r.resource_name, r.resource_type " +
                        "FROM resource_assignment ra " +
                        "JOIN resources r ON r.resource_id = ra.resource_id " +
                        "WHERE UPPER(ra.status) = 'PENDING' " +
                        "ORDER BY ra.assignment_date DESC";

        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                ResourceAssignment a = new ResourceAssignment();
                a.setAssignmentId(rs.getInt("assignment_id"));
                a.setResourceId(rs.getInt("resource_id"));
                a.setProjectCode(rs.getString("project_code"));
                a.setUserId(rs.getInt("user_id"));   // ✅ FIXED
                a.setQuantity(rs.getInt("quantity"));
                a.setAssignmentDate(rs.getDate("assignment_date"));
                a.setTotalCost(rs.getDouble("total_cost"));
                a.setStatus(rs.getString("status"));
                a.setResourceName(rs.getString("resource_name"));
                a.setResourceType(rs.getString("resource_type"));
                list.add(a);
            }
        }

        return list;
    }

    // ================= DECLINE =================
    public void declineRequest(int assignmentId) throws SQLException {

        AssignmentDetails d = getAssignmentDetails(assignmentId);

        String sql = "UPDATE resource_assignment SET status='DECLINED' WHERE assignment_id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, assignmentId);
            ps.executeUpdate();
        }

        sendStatusSmsSafe(d.userId, d.resourceName, d.qty, "DECLINED");
    }

    // ================= ACCEPT =================
    public void acceptRequest(int assignmentId) throws SQLException {

        cnx.setAutoCommit(false);

        AssignmentDetails d;

        try {
            d = getAssignmentDetailsForUpdate(assignmentId);

            // Lock resource row
            String q2 = "SELECT available_quantity FROM resources WHERE resource_id=? FOR UPDATE";
            try (PreparedStatement ps2 = cnx.prepareStatement(q2)) {
                ps2.setInt(1, d.resourceId);
                ResultSet rs2 = ps2.executeQuery();

                if (!rs2.next()) throw new SQLException("Resource not found.");

                int available = rs2.getInt("available_quantity");
                if (d.qty > available) {
                    throw new SQLException("Not enough stock. Available: " + available);
                }
            }

            // Update status
            String q3 = "UPDATE resource_assignment SET status='ACCEPTED' WHERE assignment_id=?";
            try (PreparedStatement ps3 = cnx.prepareStatement(q3)) {
                ps3.setInt(1, assignmentId);
                ps3.executeUpdate();
            }

            // Reduce stock
            String q4 = "UPDATE resources SET available_quantity = available_quantity - ? WHERE resource_id=?";
            try (PreparedStatement ps4 = cnx.prepareStatement(q4)) {
                ps4.setInt(1, d.qty);
                ps4.setInt(2, d.resourceId);
                ps4.executeUpdate();
            }

            cnx.commit();

        } catch (SQLException e) {
            cnx.rollback();
            throw e;
        } finally {
            cnx.setAutoCommit(true);
        }

        sendStatusSmsSafe(d.userId, d.resourceName, d.qty, "ACCEPTED");
    }

    // ================= HELPER CLASS =================
    private static class AssignmentDetails {
        int resourceId;
        int qty;
        int userId;          // ✅ FIXED
        String resourceName;
    }

    // ================= GET DETAILS =================
    private AssignmentDetails getAssignmentDetails(int assignmentId) throws SQLException {

        String sql =
                "SELECT ra.resource_id, ra.quantity, ra.user_id, r.resource_name " +
                        "FROM resource_assignment ra " +
                        "JOIN resources r ON r.resource_id = ra.resource_id " +
                        "WHERE ra.assignment_id = ?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, assignmentId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) throw new SQLException("Request not found.");

            AssignmentDetails d = new AssignmentDetails();
            d.resourceId = rs.getInt("resource_id");
            d.qty = rs.getInt("quantity");
            d.userId = rs.getInt("user_id");     // ✅ FIXED
            d.resourceName = rs.getString("resource_name");
            return d;
        }
    }

    private AssignmentDetails getAssignmentDetailsForUpdate(int assignmentId) throws SQLException {

        String sql =
                "SELECT ra.resource_id, ra.quantity, ra.user_id, r.resource_name " +
                        "FROM resource_assignment ra " +
                        "JOIN resources r ON r.resource_id = ra.resource_id " +
                        "WHERE ra.assignment_id = ? FOR UPDATE";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, assignmentId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) throw new SQLException("Request not found.");

            AssignmentDetails d = new AssignmentDetails();
            d.resourceId = rs.getInt("resource_id");
            d.qty = rs.getInt("quantity");
            d.userId = rs.getInt("user_id");     // ✅ FIXED
            d.resourceName = rs.getString("resource_name");
            return d;
        }
    }

    // ================= SMS =================
    private void sendStatusSmsSafe(int userId, String resourceName, int qty, String status) {
        try {
            UserService userService = new UserService();
            String phone = userService.getPhoneByUserId(userId);

            if (phone == null || phone.isBlank()) {
                System.out.println("[SMS] No phone for user_id=" + userId);
                return;
            }

            SmsService sms = new SmsService();

            String msg;
            if ("ACCEPTED".equals(status)) {
                msg = "NEXUM: ✅ Your request for " + resourceName + " (Qty: " + qty + ") was ACCEPTED.";
            } else if ("DECLINED".equals(status)) {
                msg = "NEXUM: ❌ Your request for " + resourceName + " (Qty: " + qty + ") was DECLINED.";
            } else {
                msg = "NEXUM: Update on your request for " + resourceName + " (Qty: " + qty + "): " + status;
            }

            sms.sendSms(phone, msg);

        } catch (Exception ex) {
            System.out.println("[SMS] Failed: " + ex.getMessage());
        }
    }
}