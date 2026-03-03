package services;

import com.example.testp1.model.ProjectDAO;
import entities.ResourceAssignment;
import utils.database;

import java.sql.*;
import java.time.LocalDate;
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
    private static final int PENALTY_PER_DAY = 10;

    private final ProjectDAO prj = new ProjectDAO();
    private final ProjectService project = new ProjectService();

    public java.sql.Date getProjectEndDate(String projectCode) throws SQLException {
        String sql = "SELECT end_date FROM projects WHERE id = CAST(? AS UNSIGNED)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, projectCode);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDate("end_date") : null;
            }
        }
    }

    // ================= RETURN / STOCK / BONUS =================
    public void markReturned(int assignmentId) throws SQLException {

        cnx.setAutoCommit(false);

        try {
            String q =
                    "SELECT ra.resource_id, ra.quantity, ra.user_id, ra.project_code, ra.bonus_applied, " +
                            "       p.end_date " +
                            "FROM resource_assignment ra " +
                            "JOIN projects p ON p.id = CAST(ra.project_code AS UNSIGNED) " +
                            "WHERE ra.assignment_id=? FOR UPDATE";

            int resourceId, qty, bonusApplied, userId;
            Date endDate;

            try (PreparedStatement ps = cnx.prepareStatement(q)) {
                ps.setInt(1, assignmentId);
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) throw new SQLException("Assignment not found.");

                resourceId = rs.getInt("resource_id");
                qty = rs.getInt("quantity");
                userId = rs.getInt("user_id");
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
                LocalDate today = LocalDate.now();
                LocalDate end = endDate.toLocalDate();

                if (!today.isAfter(end)) {
                    updateUserScoreDelta(userId, BONUS_ON_TIME);

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

    // ================= LATE PENALTIES =================
    public void applyLatePenaltiesForPhysicalOut() throws SQLException {

        String sql =
                "SELECT ra.assignment_id, ra.user_id, ra.penalty_days_applied, " +
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
                int userId = rs.getInt("user_id");
                Date endDate = rs.getDate("end_date");
                int applied = rs.getInt("penalty_days_applied");

                long lateDaysTotal = daysBetween(endDate.toLocalDate(), LocalDate.now());
                if (lateDaysTotal <= 0) continue;

                int totalLate = (int) lateDaysTotal;

                int toApply = totalLate;
                System.out.println("toApply: "+toApply +"\nApplied : "+applied +"\nLate : "+lateDaysTotal);
                if (toApply <= 0) continue;

                updateUserScoreDelta(userId, -(toApply * PENALTY_PER_DAY));

                String upd = "UPDATE resource_assignment SET penalty_days_applied = penalty_days_applied + ? WHERE assignment_id=?";
                try (PreparedStatement ps2 = cnx.prepareStatement(upd)) {
                    ps2.setInt(1, toApply);
                    ps2.setInt(2, assignmentId);
                    ps2.executeUpdate();
                }
            }
        }
    }

    private long daysBetween(LocalDate from, LocalDate to) {
        return java.time.temporal.ChronoUnit.DAYS.between(from, to);
    }

    private void updateUserScoreDelta(int userId, int delta) throws SQLException {
        String sql = "UPDATE utilisateurs SET score = score + ? WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, delta);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    // ================= LIST PHYSICAL OUT =================
    public List<ResourceAssignment> getPhysicalOut() throws SQLException {
        List<ResourceAssignment> list = new ArrayList<>();

        String sql =
                "SELECT ra.assignment_id, ra.resource_id, ra.project_code, ra.user_id, ra.quantity, " +
                        "       ra.assignment_date, ra.return_date, ra.total_cost, ra.status, " +
                        "       ra.penalty_days_applied, ra.bonus_applied, " +
                        "       r.resource_name, r.resource_type " +
                        "FROM resource_assignment ra " +
                        "JOIN resources r ON r.resource_id = ra.resource_id " +
                        "WHERE r.resource_type = 'PHYSICAL' " +
                        "  AND UPPER(ra.status) = 'ACCEPTED' " +
                        "  AND ra.return_date IS NOT NULL "+
                        "ORDER BY ra.assignment_date DESC";

        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                ResourceAssignment a = new ResourceAssignment();
                a.setAssignmentId(rs.getInt("assignment_id"));
                a.setResourceId(rs.getInt("resource_id"));
                a.setProjectCode(rs.getString("project_code"));
                a.setUserId(rs.getInt("user_id"));
                a.setQuantity(rs.getInt("quantity"));
                a.setAssignmentDate(rs.getDate("assignment_date"));
                a.setReturnDate(rs.getDate("return_date"));
                a.setTotalCost(rs.getDouble("total_cost"));
                a.setStatus(rs.getString("status"));
                a.setResourceName(rs.getString("resource_name"));
                a.setResourceType(rs.getString("resource_type"));
                list.add(a);
            }
        }
        return list;
    }

    // ================= POLICY HELPERS =================
    private boolean hasActiveDuplicate(int userId, int resourceId) throws SQLException {
        String sql =
                "SELECT COUNT(*) c " +
                        "FROM resource_assignment " +
                        "WHERE user_id = ? AND resource_id = ? " +
                        "AND UPPER(status) IN ('ACCEPTED','ACTIVE')";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
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

    private int countActiveByType(int userId, String type) throws SQLException {
        String sql =
                "SELECT COUNT(*) c " +
                        "FROM resource_assignment ra " +
                        "JOIN resources r ON r.resource_id = ra.resource_id " +
                        "WHERE ra.user_id = ? " +
                        "AND r.resource_type = ? " +
                        "AND UPPER(ra.status) IN ('ACCEPTED','ACTIVE')";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, type);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("c") : 0;
            }
        }
    }

    private void enforceUserAllocationPolicy(int resourceId, int userId) throws SQLException {

        if (hasActiveDuplicate(userId, resourceId)) {
            throw new SQLException("POLICY: Duplicate resource already assigned to this user.");
        }

        String type = getResourceType(resourceId);
        if (type == null) return;

        int activeCount = countActiveByType(userId, type);

        if ("SOFTWARE".equalsIgnoreCase(type) && activeCount >= MAX_ACTIVE_SOFTWARE) {
            throw new SQLException("POLICY: Max ACTIVE SOFTWARE reached (" + MAX_ACTIVE_SOFTWARE + ").");
        }

        if ("PHYSICAL".equalsIgnoreCase(type) && activeCount >= MAX_ACTIVE_PHYSICAL) {
            throw new SQLException("POLICY: Max ACTIVE PHYSICAL reached (" + MAX_ACTIVE_PHYSICAL + ").");
        }
    }

    // ================= GET ASSIGNMENTS BY USER =================
    public List<ResourceAssignment> getByUser(int userId) throws SQLException {

        List<ResourceAssignment> list = new ArrayList<>();

        String sql =
                "SELECT ra.assignment_id, ra.resource_id, ra.project_code, ra.user_id, " +
                        "       ra.quantity, ra.assignment_date, ra.return_date, ra.total_cost, ra.status, " +
                        "       r.resource_name, r.resource_type, r.image_path " +
                        "FROM resource_assignment ra " +
                        "JOIN resources r ON r.resource_id = ra.resource_id " +
                        "WHERE ra.user_id = ? " +
                        "ORDER BY ra.assignment_date DESC";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ResourceAssignment a = new ResourceAssignment();

                    a.setAssignmentId(rs.getInt("assignment_id"));
                    a.setResourceId(rs.getInt("resource_id"));
                    a.setProjectCode(rs.getString("project_code"));
                    a.setUserId(rs.getInt("user_id"));
                    a.setQuantity(rs.getInt("quantity"));
                    a.setAssignmentDate(rs.getDate("assignment_date"));
                    a.setReturnDate(rs.getDate("return_date"));
                    a.setTotalCost(rs.getDouble("total_cost"));
                    a.setStatus(rs.getString("status"));

                    a.setResourceName(rs.getString("resource_name"));
                    a.setResourceType(rs.getString("resource_type"));
                    a.setResourceImagePath(rs.getString("image_path"));

                    list.add(a);
                }
            }
        }

        return list;
    }

    // ================= REQUEST RESOURCE =================
    // ================= REQUEST RESOURCE =================
    public int requestResource(int resourceId, String projectCode, int userId, int quantity, double totalCost) throws SQLException {

        enforceUserAllocationPolicy(resourceId, userId);

        String type = getResourceType(resourceId);
        String statusToSet = "PENDING";

        if ("PHYSICAL".equalsIgnoreCase(type)) {
            AutoApprovalEngine engine = new AutoApprovalEngine();
            AutoApprovalEngine.Decision d = engine.decideForPhysicalRequest(userId);
            statusToSet = d.name();
        }

        cnx.setAutoCommit(false);

        try {
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

            LocalDate endDate = prj.getEndDateByName(prj.getNameById(Integer.parseInt(projectCode)));

            String insert =
                    "INSERT INTO resource_assignment " +
                            "(resource_id, project_code, user_id, quantity, assignment_date, return_date, total_cost, status) " +
                            "VALUES (?, ?, ?, ?, CURDATE(), ?, ?, ?)";

            int generatedId;

            try (PreparedStatement ps = cnx.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, resourceId);
                ps.setString(2, projectCode);
                ps.setInt(3, userId);
                ps.setInt(4, quantity);
                ps.setDate(5, endDate == null ? null : java.sql.Date.valueOf(endDate)); // expected return/end date
                ps.setDouble(6, totalCost);
                ps.setString(7, statusToSet);

                ps.executeUpdate();

                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) throw new SQLException("Failed to get generated assignment_id.");
                    generatedId = keys.getInt(1);
                }
            }

            if ("ACCEPTED".equalsIgnoreCase(statusToSet)) {
                String reduce = "UPDATE resources SET available_quantity = available_quantity - ? WHERE resource_id=?";
                try (PreparedStatement ps = cnx.prepareStatement(reduce)) {
                    ps.setInt(1, quantity);
                    ps.setInt(2, resourceId);
                    ps.executeUpdate();
                }
            }

            cnx.commit();
            return generatedId;

        } catch (SQLException e) {
            cnx.rollback();
            throw e;
        } finally {
            cnx.setAutoCommit(true);
        }
    }

    // ================= DELETE =================
    public void delete(int assignmentId) throws SQLException {
        String sql = "DELETE FROM resource_assignment WHERE assignment_id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, assignmentId);
            ps.executeUpdate();
        }
    }

    // ================= UPDATE REQUEST =================
    public void updateRequest(int assignmentId, int resourceId, String projectCode, int quantity, double totalCost) throws SQLException {
        String sql =
                "UPDATE resource_assignment SET " +
                        "resource_id = ?, project_code = ?, quantity = ?, total_cost = ?, status = 'PENDING' " +
                        "WHERE assignment_id = ?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, resourceId);
            ps.setString(2, projectCode);
            ps.setInt(3, quantity);
            ps.setDouble(4, totalCost);
            ps.setInt(5, assignmentId);
            ps.executeUpdate();
        }
    }

    // ================= ADMIN UPDATE STATUS + SMS =================
    public void adminUpdateStatus(int assignmentId, String newStatus) throws SQLException {

        String detailsSql =
                "SELECT ra.user_id, ra.quantity, r.resource_name " +
                        "FROM resource_assignment ra " +
                        "JOIN resources r ON r.resource_id = ra.resource_id " +
                        "WHERE ra.assignment_id = ?";

        int userId;
        int qty;
        String resourceName;

        try (PreparedStatement ps = cnx.prepareStatement(detailsSql)) {
            ps.setInt(1, assignmentId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                userId = rs.getInt("user_id");
                qty = rs.getInt("quantity");
                resourceName = rs.getString("resource_name");
            } else {
                throw new SQLException("Assignment not found (id=" + assignmentId + ")");
            }
        }

        String updateSql = "UPDATE resource_assignment SET status = ? WHERE assignment_id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(updateSql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, assignmentId);
            ps.executeUpdate();
        }

        try {
            UserService userService = new UserService();
            String phone = userService.getPhoneByUserId(userId);

            if (phone != null && !phone.isBlank()) {
                services.notifications.SmsService sms = new services.notifications.SmsService();

                String st = newStatus == null ? "" : newStatus.trim().toUpperCase();
                String msg;

                if (st.equals("ACCEPTED")) {
                    msg = "✅ Approved: " + resourceName + " (Qty: " + qty + ").";
                } else if (st.equals("DECLINED")) {
                    msg = "❌ Declined: " + resourceName + " (Qty: " + qty + "). Please adjust and try again.";
                } else {
                    msg = "ℹ️ Update: Your request for " + resourceName + " (Qty: " + qty + ") is now " + st + ".";
                }

                // sms.send(phone, msg);
            } else {
                System.out.println("[SMS API] No phone found for user_id=" + userId);
            }

        } catch (SQLException ex) {
            System.out.println("[SMS API] Failed to send SMS: " + ex.getMessage());
        }
    }
}