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
    // ================= INTELLIGENT ALLOCATION POLICY =================
    private static final int MAX_ACTIVE_SOFTWARE = 3;
    private static final int MAX_ACTIVE_PHYSICAL = 5;
    private static final int BONUS_ON_TIME = 10;
    private static final int PENALTY_PER_DAY = 10; // if you want -25 just change to 25

    public java.sql.Date getProjectEndDate(String projectCode) throws SQLException {
        String sql = "SELECT end_date FROM projects WHERE id = CAST(? AS UNSIGNED)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, projectCode);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDate("end_date") : null;
            }
        }
    }

    public void markReturned(int assignmentId) throws SQLException {

        cnx.setAutoCommit(false);

        try {
            // lock assignment and fetch details + project end date
            String q =
                    "SELECT ra.resource_id, ra.quantity, ra.client_code, ra.project_code, ra.bonus_applied, " +
                            "       p.end_date " +
                            "FROM resource_assignment ra " +
                            "JOIN projects p ON p.id = CAST(ra.project_code AS UNSIGNED) " +
                            "WHERE ra.assignment_id=? FOR UPDATE";

            int resourceId, qty, bonusApplied;
            String clientCode;
            Date endDate;

            try (PreparedStatement ps = cnx.prepareStatement(q)) {
                ps.setInt(1, assignmentId);
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) throw new SQLException("Assignment not found.");

                resourceId = rs.getInt("resource_id");
                qty = rs.getInt("quantity");
                clientCode = rs.getString("client_code");
                bonusApplied = rs.getInt("bonus_applied");
                endDate = rs.getDate("end_date");
            }

            // set returned
            String upd =
                    "UPDATE resource_assignment " +
                            "SET return_date = CURDATE(), status = 'RETURNED' " +
                            "WHERE assignment_id=?";
            try (PreparedStatement ps = cnx.prepareStatement(upd)) {
                ps.setInt(1, assignmentId);
                ps.executeUpdate();
            }

            // restore stock
            String stock = "UPDATE resources SET available_quantity = available_quantity + ? WHERE resource_id=?";
            try (PreparedStatement ps = cnx.prepareStatement(stock)) {
                ps.setInt(1, qty);
                ps.setInt(2, resourceId);
                ps.executeUpdate();
            }

            // bonus if on time (<= end_date) and not applied yet
            if (endDate != null && bonusApplied == 0) {
                java.time.LocalDate today = java.time.LocalDate.now();
                java.time.LocalDate end = endDate.toLocalDate();

                if (!today.isAfter(end)) {
                    updateClientScoreDelta(clientCode, BONUS_ON_TIME);
                    String b = "UPDATE resource_assignment SET bonus_applied = 1 WHERE assignment_id=?";
                    try (PreparedStatement ps = cnx.prepareStatement(b)) {
                        ps.setInt(1, assignmentId);
                        ps.executeUpdate();
                    }
                }
            }

            cnx.commit();

        } catch (SQLException e) {
            cnx.rollback();
            throw e;
        } finally {
            cnx.setAutoCommit(true);
        }
    }

    public void applyLatePenaltiesForPhysicalOut() throws SQLException {

        String sql =
                "SELECT ra.assignment_id, ra.client_code, ra.penalty_days_applied, " +
                        "       p.end_date " +
                        "FROM resource_assignment ra " +
                        "JOIN resources r ON r.resource_id = ra.resource_id " +
                        "JOIN projects p ON p.id = CAST(ra.project_code AS UNSIGNED) " +
                        "WHERE r.resource_type='PHYSICAL' " +
                        "  AND UPPER(ra.status)='ACCEPTED' " +
                        "  AND ra.return_date IS NULL " +
                        "  AND p.end_date IS NOT NULL";

        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int assignmentId = rs.getInt("assignment_id");
                String clientCode = rs.getString("client_code");
                Date endDate = rs.getDate("end_date");
                int applied = rs.getInt("penalty_days_applied");

                long lateDaysTotal = daysBetween(endDate.toLocalDate(), java.time.LocalDate.now());
                if (lateDaysTotal <= 0) continue;

                int totalLate = (int) lateDaysTotal;
                int toApply = totalLate - applied;
                if (toApply <= 0) continue;

                // score -= toApply * PENALTY_PER_DAY
                updateClientScoreDelta(clientCode, -(toApply * PENALTY_PER_DAY));

                // persist applied days
                String upd = "UPDATE resource_assignment SET penalty_days_applied = penalty_days_applied + ? WHERE assignment_id=?";
                try (PreparedStatement ps2 = cnx.prepareStatement(upd)) {
                    ps2.setInt(1, toApply);
                    ps2.setInt(2, assignmentId);
                    ps2.executeUpdate();
                }
            }
        }
    }

    private long daysBetween(java.time.LocalDate from, java.time.LocalDate to) {
        return java.time.temporal.ChronoUnit.DAYS.between(from, to);
    }

    private void updateClientScoreDelta(String clientCode, int delta) throws SQLException {
        String sql = "UPDATE utilisateurs SET score = score + ? WHERE client_code = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, delta);
            ps.setString(2, clientCode);
            ps.executeUpdate();
        }
    }

    public List<ResourceAssignment> getPhysicalOut() throws SQLException {
        List<ResourceAssignment> list = new ArrayList<>();

        String sql =
                "SELECT ra.assignment_id, ra.resource_id, ra.project_code, ra.client_code, ra.quantity, " +
                        "       ra.assignment_date, ra.return_date, ra.total_cost, ra.status, " +
                        "       ra.penalty_days_applied, ra.bonus_applied, " +
                        "       r.resource_name, r.resource_type " +
                        "FROM resource_assignment ra " +
                        "JOIN resources r ON r.resource_id = ra.resource_id " +
                        "WHERE r.resource_type = 'PHYSICAL' " +
                        "  AND UPPER(ra.status) = 'ACCEPTED' " +
                        "  AND ra.return_date IS NULL " +
                        "ORDER BY ra.assignment_date DESC";

        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

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
                // If your entity doesn't have these yet, we’ll add them:
                // a.setPenaltyDaysApplied(rs.getInt("penalty_days_applied"));
                // a.setBonusApplied(rs.getInt("bonus_applied") == 1);
                list.add(a);
            }
        }
        return list;
    }

    // Rule 1: duplicate active resource for same client
    private boolean hasActiveDuplicate(String clientCode, int resourceId) throws SQLException {
        String sql =
                "SELECT COUNT(*) c " +
                        "FROM resource_assignment " +
                        "WHERE client_code = ? AND resource_id = ? " +
                        "AND UPPER(status) IN ('ACCEPTED','ACTIVE')";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, clientCode);
            ps.setInt(2, resourceId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt("c") > 0;
            }
        }
    }

    private String getResourceType(int resourceId) throws SQLException {
        String sql = "SELECT resource_type FROM resources WHERE resource_id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, resourceId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("resource_type");
            }
        }
        return null;
    }

    private int countActiveByType(String clientCode, String type) throws SQLException {
        String sql =
                "SELECT COUNT(*) c " +
                        "FROM resource_assignment ra " +
                        "JOIN resources r ON r.resource_id = ra.resource_id " +
                        "WHERE ra.client_code = ? " +
                        "AND r.resource_type = ? " +
                        "AND UPPER(ra.status) IN ('ACCEPTED','ACTIVE')";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, clientCode);
            ps.setString(2, type);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("c") : 0;
            }
        }
    }

    private void enforceClientAllocationPolicy(int resourceId, String clientCode) throws SQLException {

        // Rule 1: prevent duplicate resource already active
        if (hasActiveDuplicate(clientCode, resourceId)) {
            throw new SQLException("POLICY: Duplicate resource already assigned to this client.");
        }

        // Rule 2: limit by type
        String type = getResourceType(resourceId);
        if (type == null) return;

        int activeCount = countActiveByType(clientCode, type);

        if ("SOFTWARE".equalsIgnoreCase(type) && activeCount >= MAX_ACTIVE_SOFTWARE) {
            throw new SQLException("POLICY: Max ACTIVE SOFTWARE reached (" + MAX_ACTIVE_SOFTWARE + ").");
        }

        if ("PHYSICAL".equalsIgnoreCase(type) && activeCount >= MAX_ACTIVE_PHYSICAL) {
            throw new SQLException("POLICY: Max ACTIVE PHYSICAL reached (" + MAX_ACTIVE_PHYSICAL + ").");
        }
    }


    public List<ResourceAssignment> getByClient(String clientCode) throws SQLException {

        List<ResourceAssignment> list = new ArrayList<>();

        String sql =
                "SELECT ra.assignment_id, ra.resource_id, ra.project_code, ra.client_code, " +
                        "       ra.quantity, ra.assignment_date, ra.return_date, ra.total_cost, ra.status, " +
                        "       r.resource_name, r.resource_type, r.image_path " +   // ✅ add
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

            // ✅ NEW
            a.setResourceImagePath(rs.getString("image_path"));

            list.add(a);
        }

        return list;
    }

    public void requestResource(int resourceId, String projectCode, String clientCode, int quantity, double totalCost) throws SQLException {

        // still enforce your allocation policy
        enforceClientAllocationPolicy(resourceId, clientCode);

        String type = getResourceType(resourceId);
        String statusToSet = "PENDING";

        if ("PHYSICAL".equalsIgnoreCase(type)) {
            AutoApprovalEngine engine = new AutoApprovalEngine();
            AutoApprovalEngine.Decision d = engine.decideForPhysicalRequest(clientCode);
            statusToSet = d.name(); // ACCEPTED / DECLINED / PENDING
        }

        cnx.setAutoCommit(false);

        try {
            // If we are going to ACCEPT immediately, lock stock row and verify
            if ("ACCEPTED".equalsIgnoreCase(statusToSet)) {
                String qStock = "SELECT available_quantity FROM resources WHERE resource_id=? FOR UPDATE";
                try (PreparedStatement ps = cnx.prepareStatement(qStock)) {
                    ps.setInt(1, resourceId);
                    ResultSet rs = ps.executeQuery();
                    if (!rs.next()) throw new SQLException("Resource not found.");
                    int available = rs.getInt("available_quantity");
                    if (quantity > available) throw new SQLException("Not enough stock. Available: " + available);
                }
            }

            // Insert assignment
            String insert =
                    "INSERT INTO resource_assignment " +
                            "(resource_id, project_code, client_code, quantity, assignment_date, total_cost, status) " +
                            "VALUES (?, ?, ?, ?, CURDATE(), ?, ?)";

            try (PreparedStatement ps = cnx.prepareStatement(insert)) {
                ps.setInt(1, resourceId);
                ps.setString(2, projectCode);
                ps.setString(3, clientCode);
                ps.setInt(4, quantity);
                ps.setDouble(5, totalCost);
                ps.setString(6, statusToSet);
                ps.executeUpdate();
            }

            // If accepted, reduce stock now
            if ("ACCEPTED".equalsIgnoreCase(statusToSet)) {
                String reduce = "UPDATE resources SET available_quantity = available_quantity - ? WHERE resource_id=?";
                try (PreparedStatement ps = cnx.prepareStatement(reduce)) {
                    ps.setInt(1, quantity);
                    ps.setInt(2, resourceId);
                    ps.executeUpdate();
                }
            }

            cnx.commit();

        } catch (SQLException e) {
            cnx.rollback();
            throw e;
        } finally {
            cnx.setAutoCommit(true);
        }
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
