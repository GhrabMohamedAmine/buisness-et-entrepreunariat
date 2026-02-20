package services;

import org.junit.jupiter.api.*;

import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AssignmentServiceTest {

    private Connection cnx;
    private ResourceService resourceService;
    private AssignmentService assignmentService;

    // Test constants
    private static final int TEST_RESOURCE_CODE = 991001;
    private static final int TEST_RESOURCE_CODE_2 = 991002;

    private static final String TEST_CLIENT = "CL_TEST";
    private static final String TEST_PROJECT = "PR_TEST";

    private Integer resourceId1;
    private Integer resourceId2;
    private Integer assignmentId;

    @BeforeEach
    void setup() throws Exception {
        cnx = utils.database.getInstance().getConnection();
        assertNotNull(cnx);

        resourceService = new ResourceService();
        assignmentService = new AssignmentService();

        // cleanup old leftovers (by codes + client/project)
        cleanupAssignments(TEST_CLIENT, TEST_PROJECT);
        cleanupResourceByCode(TEST_RESOURCE_CODE);
        cleanupResourceByCode(TEST_RESOURCE_CODE_2);

        // create 2 resources for FK + update tests
        resourceId1 = insertResourceAndGetId(TEST_RESOURCE_CODE, "Assign Test Resource 1", "PHYSICAL");
        resourceId2 = insertResourceAndGetId(TEST_RESOURCE_CODE_2, "Assign Test Resource 2", "SOFTWARE");

        assertNotNull(resourceId1);
        assertNotNull(resourceId2);
    }

    @AfterEach
    void tearDown() throws Exception {
        cleanupAssignments(TEST_CLIENT, TEST_PROJECT);
        cleanupResourceByCode(TEST_RESOURCE_CODE);
        cleanupResourceByCode(TEST_RESOURCE_CODE_2);
    }

    // ---------------- HELPERS ----------------
    private void cleanupAssignments(String client, String project) throws SQLException {
        try (PreparedStatement ps = cnx.prepareStatement(
                "DELETE FROM resource_assignment WHERE client_code=? AND project_code=?")) {
            ps.setString(1, client);
            ps.setString(2, project);
            ps.executeUpdate();
        }
    }

    private void cleanupResourceByCode(int code) throws SQLException {
        try (PreparedStatement ps = cnx.prepareStatement(
                "DELETE FROM resources WHERE resource_code=?")) {
            ps.setInt(1, code);
            ps.executeUpdate();
        }
    }

    private Integer insertResourceAndGetId(int code, String name, String type) throws Exception {
        // Use your ResourceService.add()
        entities.Resource r = new entities.Resource();
        r.setCode(code);
        r.setName(name);
        r.setType(type);
        r.setUnitcost(10.0);
        r.setQuantity(100);
        r.setAvquant(100);
        resourceService.add(r);

        // get resource_id
        try (PreparedStatement ps = cnx.prepareStatement(
                "SELECT resource_id FROM resources WHERE resource_code=? LIMIT 1")) {
            ps.setInt(1, code);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("resource_id");
            return null;
        }
    }

    private Integer findLatestAssignmentId() throws SQLException {
        // find assignment for our (client, project), newest first
        try (PreparedStatement ps = cnx.prepareStatement(
                "SELECT assignment_id FROM resource_assignment " +
                        "WHERE client_code=? AND project_code=? " +
                        "ORDER BY assignment_date DESC, assignment_id DESC LIMIT 1")) {
            ps.setString(1, TEST_CLIENT);
            ps.setString(2, TEST_PROJECT);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("assignment_id");
            return null;
        }
    }

    // ---------------- TESTS ----------------

    @Test
    @Order(1)
    void requestResource_shouldInsertPendingRow() throws Exception {
        assignmentService.requestResource(resourceId1, TEST_PROJECT, TEST_CLIENT, 3, 30.0);

        assignmentId = findLatestAssignmentId();
        assertNotNull(assignmentId, "assignment_id should exist after insert.");

        // verify DB values
        try (PreparedStatement ps = cnx.prepareStatement(
                "SELECT resource_id, project_code, client_code, quantity, total_cost, status " +
                        "FROM resource_assignment WHERE assignment_id=?")) {
            ps.setInt(1, assignmentId);
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());

            assertEquals(resourceId1.intValue(), rs.getInt("resource_id"));
            assertEquals(TEST_PROJECT, rs.getString("project_code"));
            assertEquals(TEST_CLIENT, rs.getString("client_code"));
            assertEquals(3, rs.getInt("quantity"));
            assertEquals(30.0, rs.getDouble("total_cost"), 0.0001);
            assertEquals("PENDING", rs.getString("status"));
        }
    }

    @Test
    @Order(2)
    void getByClient_shouldReturnInsertedRequestWithJoinData() throws Exception {
        assignmentService.requestResource(resourceId1, TEST_PROJECT, TEST_CLIENT, 2, 20.0);

        var list = assignmentService.getByClient(TEST_CLIENT);

        assertNotNull(list);
        assertTrue(list.size() > 0);

        // Find our row
        var match = list.stream()
                .filter(a -> TEST_PROJECT.equals(a.getProjectCode()))
                .findFirst();

        assertTrue(match.isPresent(), "getByClient should include our inserted assignment.");

        // Join columns should be filled
        assertNotNull(match.get().getResourceName());
        assertNotNull(match.get().getResourceType());
    }

    @Test
    @Order(3)
    void updateRequest_shouldUpdateFieldsAndResetPending() throws Exception {
        // insert initial
        assignmentService.requestResource(resourceId1, TEST_PROJECT, TEST_CLIENT, 1, 10.0);
        assignmentId = findLatestAssignmentId();
        assertNotNull(assignmentId);

        // update to resource 2 + new qty/cost
        assignmentService.updateRequest(assignmentId, resourceId2, TEST_PROJECT, 7, 700.0);

        try (PreparedStatement ps = cnx.prepareStatement(
                "SELECT resource_id, quantity, total_cost, status FROM resource_assignment WHERE assignment_id=?")) {
            ps.setInt(1, assignmentId);
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());

            assertEquals(resourceId2.intValue(), rs.getInt("resource_id"));
            assertEquals(7, rs.getInt("quantity"));
            assertEquals(700.0, rs.getDouble("total_cost"), 0.0001);
            assertEquals("PENDING", rs.getString("status"), "updateRequest should reset status to PENDING.");
        }
    }

    @Test
    @Order(4)
    void delete_shouldRemoveAssignment() throws Exception {
        assignmentService.requestResource(resourceId1, TEST_PROJECT, TEST_CLIENT, 4, 40.0);
        assignmentId = findLatestAssignmentId();
        assertNotNull(assignmentId);

        assignmentService.delete(assignmentId);

        try (PreparedStatement ps = cnx.prepareStatement(
                "SELECT COUNT(*) FROM resource_assignment WHERE assignment_id=?")) {
            ps.setInt(1, assignmentId);
            ResultSet rs = ps.executeQuery();
            rs.next();
            assertEquals(0, rs.getInt(1), "Row should be deleted.");
        }
    }
}
