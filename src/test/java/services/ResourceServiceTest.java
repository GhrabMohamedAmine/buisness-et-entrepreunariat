package services;

import entities.Resource;
import org.junit.jupiter.api.*;

import java.sql.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ResourceServiceTest {

    private ResourceService service;
    private Connection cnx;

    // Use a unique code so tests don't clash with your real data
    private static final int TEST_CODE = 990001;
    private static final int TEST_CODE_UPDATED = 990002;

    @BeforeEach
    void setup() throws Exception {
        service = new ResourceService();
        cnx = utils.database.getInstance().getConnection();
        assertNotNull(cnx);

        // Clean any leftover test rows (by code)
        cleanupByCode(TEST_CODE);
        cleanupByCode(TEST_CODE_UPDATED);
    }

    @AfterEach
    void tearDown() throws Exception {
        cleanupByCode(TEST_CODE);
        cleanupByCode(TEST_CODE_UPDATED);
    }

    // ---------- HELPERS ----------
    private void cleanupByCode(int code) throws SQLException {
        try (PreparedStatement ps = cnx.prepareStatement("DELETE FROM resources WHERE resource_code = ?")) {
            ps.setInt(1, code);
            ps.executeUpdate();
        }
    }

    private Integer findIdByCode(int code) throws SQLException {
        try (PreparedStatement ps = cnx.prepareStatement(
                "SELECT resource_id FROM resources WHERE resource_code = ? LIMIT 1")) {
            ps.setInt(1, code);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("resource_id");
            return null;
        }
    }

    private int countRowsByCode(int code) throws SQLException {
        try (PreparedStatement ps = cnx.prepareStatement(
                "SELECT COUNT(*) FROM resources WHERE resource_code = ?")) {
            ps.setInt(1, code);
            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getInt(1);
        }
    }

    private Resource makeResource(int code, String name) {
        Resource r = new Resource();
        r.setCode(code);
        r.setName(name);
        r.setType("PHYSICAL");     // must match your allowed types
        r.setUnitcost(12.5);
        r.setQuantity(10);
        r.setAvquant(10);
        // status is set to "AVAILABLE" by your service
        return r;
    }

    // ---------- TESTS ----------

    @Test
    @Order(1)
    void add_shouldInsertRow() throws Exception {
        Resource r = makeResource(TEST_CODE, "JUnit Test Resource");

        service.add(r);

        assertEquals(1, countRowsByCode(TEST_CODE), "Row should be inserted in resources table.");
        Integer id = findIdByCode(TEST_CODE);
        assertNotNull(id, "Inserted resource_id should exist.");
    }

    @Test
    @Order(2)
    void update_shouldModifyRow() throws Exception {
        // First insert
        Resource r = makeResource(TEST_CODE, "Before Update");
        service.add(r);

        Integer id = findIdByCode(TEST_CODE);
        assertNotNull(id);

        // Prepare updated object (IMPORTANT: update() uses resource_id from r.getId())
        Resource updated = makeResource(TEST_CODE_UPDATED, "After Update");
        updated.setId(id);               // <- critical
        updated.setType("SOFTWARE");
        updated.setUnitcost(99.9);
        updated.setQuantity(50);
        updated.setAvquant(20);

        service.update(updated);

        // Old code should be gone, new code should exist (same row)
        assertEquals(0, countRowsByCode(TEST_CODE), "Old code should not remain after update.");
        assertEquals(1, countRowsByCode(TEST_CODE_UPDATED), "Updated code should exist.");

        // Verify values in DB
        try (PreparedStatement ps = cnx.prepareStatement(
                "SELECT resource_name, resource_type, unit_cost, total_quantity, available_quantity " +
                        "FROM resources WHERE resource_id = ?")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());

            assertEquals("After Update", rs.getString("resource_name"));
            assertEquals("SOFTWARE", rs.getString("resource_type"));
            assertEquals(99.9, rs.getDouble("unit_cost"), 0.0001);
            assertEquals(50, rs.getInt("total_quantity"));
            assertEquals(20, rs.getInt("available_quantity"));
        }
    }

    @Test
    @Order(3)
    void delete_shouldRemoveRowById() throws Exception {
        Resource r = makeResource(TEST_CODE, "To Delete");
        service.add(r);

        Integer id = findIdByCode(TEST_CODE);
        assertNotNull(id);

        service.delete(id);

        assertNull(findIdByCode(TEST_CODE), "Row should be deleted.");
    }

    @Test
    @Order(4)
    void getAll_shouldReturnList() throws Exception {
        // Ensure at least one test row exists
        service.add(makeResource(TEST_CODE, "GetAll Item"));

        List<Resource> all = service.getAll();

        assertNotNull(all);
        assertTrue(all.size() > 0, "getAll() should return at least one resource.");
        assertTrue(all.stream().anyMatch(r -> r.getCode() == TEST_CODE),
                "getAll() should contain the inserted test resource.");
    }

    @Test
    @Order(5)
    void stats_shouldWork() throws Exception {
        int beforeAll = service.countAll();
        int beforePhysical = service.countByType("PHYSICAL");

        service.add(makeResource(TEST_CODE, "Stats Item")); // PHYSICAL

        int afterAll = service.countAll();
        int afterPhysical = service.countByType("PHYSICAL");

        assertEquals(beforeAll + 1, afterAll);
        assertEquals(beforePhysical + 1, afterPhysical);
    }
}
