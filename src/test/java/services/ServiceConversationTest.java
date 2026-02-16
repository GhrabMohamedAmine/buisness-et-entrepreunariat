package services;

import model.Conversation;
import model.ParticipantView;
import org.junit.jupiter.api.*;
import utils.DBConnection;

import java.sql.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ServiceConversationTest {

    static ServiceConversation sc;
    static Connection cn;

    static int userA = 900001;
    static int userB = 900002;
    static int userC = 900003;

    static long dmId;
    static long groupId;

    @BeforeAll
    static void setup() throws SQLException {
        sc = new ServiceConversation();
        cn = DBConnection.getInstance().getConnection();

        insertUserIfMissing(userA, "Alice", "Alpha");
        insertUserIfMissing(userB, "Bob", "Bravo");
        insertUserIfMissing(userC, "Cara", "Charlie");
    }

    @Test
    @Order(1)
    void createOrGetDMTest() throws SQLException {
        dmId = sc.createOrGetDM(userA, userB, userA);
        assertTrue(dmId > 0);

        long dmId2 = sc.createOrGetDM(userB, userA, userA);
        assertEquals(dmId, dmId2);

        assertEquals(2, countParticipants(dmId));
    }

    @Test
    @Order(2)
    void createGroupConversationTest() throws SQLException {
        groupId = sc.createGroupConversation("JUnit Group", userA, List.of(userB, userC, userB));

        assertTrue(groupId > 0);
        assertEquals(3, countParticipants(groupId));

        String role = getSingleString(
                "SELECT role FROM conversation_participants WHERE conversation_id=? AND user_id=?",
                groupId, userA
        );
        assertEquals("ADMIN", role);
    }

    @Test
    @Order(3)
    void listForUserTest() throws SQLException {
        List<Conversation> list = sc.listForUser(userA);

        assertNotNull(list);
        assertFalse(list.isEmpty());

        boolean containsGroup = list.stream().anyMatch(c -> c.getId() == groupId);
        boolean containsDM = list.stream().anyMatch(c -> c.getId() == dmId);

        assertTrue(containsGroup);
        assertTrue(containsDM);
    }

    @Test
    @Order(4)
    void nicknameTest() throws SQLException {
        assertNull(sc.getNickname(groupId, userB));

        sc.setNickname(groupId, userB, "Neo");
        assertEquals("Neo", sc.getNickname(groupId, userB));

        sc.setNickname(groupId, userB, null);
        assertNull(sc.getNickname(groupId, userB));
    }

    @Test
    @Order(5)
    void updateConversationNameTest() throws SQLException {
        sc.updateConversationName(groupId, "Renamed Group");

        String title = getSingleString(
                "SELECT title FROM conversations WHERE id=?",
                groupId
        );

        assertEquals("Renamed Group", title);
    }

    @Test
    @Order(6)
    void kickAndReviveTest() throws SQLException {
        sc.kickParticipant(groupId, userC, userA);

        Timestamp leftAt = getSingleTimestamp(
                "SELECT left_at FROM conversation_participants WHERE conversation_id=? AND user_id=?",
                groupId, userC
        );
        assertNotNull(leftAt);

        sc.addMembers(groupId, userA, List.of(userC));

        Timestamp leftAt2 = getSingleTimestamp(
                "SELECT left_at FROM conversation_participants WHERE conversation_id=? AND user_id=?",
                groupId, userC
        );
        assertNull(leftAt2);
    }

    @Test
    @Order(7)
    void listParticipantViewsTest() throws SQLException {
        List<ParticipantView> views = sc.listParticipantViews(groupId);

        assertNotNull(views);
        assertEquals(3, views.size());
        assertTrue(views.stream().allMatch(v -> v.getUsername() != null));
    }

    @Test
    @Order(8)
    void listAllUsersExceptTest() throws SQLException {
        List<ParticipantView> list = sc.listAllUsersExcept(userA);

        assertNotNull(list);
        assertTrue(list.stream().noneMatch(p -> p.getUserId() == userA));
    }

    // -------------------- HELPERS --------------------

    private static void insertUserIfMissing(int id, String prenom, String nom) throws SQLException {

        String email = "test" + id + "@example.com";
        String role = "USER";   // must NOT be null
        Date today = new Date(System.currentTimeMillis());

        try (PreparedStatement ps = cn.prepareStatement(
                "INSERT INTO utilisateurs(id, nom, prenom, email, role, date_inscription) VALUES(?,?,?,?,?,?)"
        )) {
            ps.setInt(1, id);
            ps.setString(2, nom);
            ps.setString(3, prenom);
            ps.setString(4, email);
            ps.setString(5, role);
            ps.setDate(6, today);

            ps.executeUpdate();
        } catch (SQLException e) {
            // ignore duplicate key only
            if (!("23000".equals(e.getSQLState()) || e.getErrorCode() == 1062)) {
                throw e;
            }
        }
    }



    private static int countParticipants(long convId) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(
                "SELECT COUNT(*) FROM conversation_participants WHERE conversation_id=? AND left_at IS NULL"
        )) {
            ps.setLong(1, convId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private static String getSingleString(String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return rs.getString(1);
            }
        }
    }

    private static Timestamp getSingleTimestamp(String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return rs.getTimestamp(1);
            }
        }
    }

    private static void bind(PreparedStatement ps, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
    }
}
