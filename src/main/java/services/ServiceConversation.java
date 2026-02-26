package services;
import model.Conversation;
import model.ParticipantView;
import utils.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceConversation {

    private final Connection connection;

    public ServiceConversation() {
        connection = DBConnection.getInstance().getConnection();
    }

    // Create or get DM (NO duplicate thanks to dm_key UNIQUE)
    public long createOrGetDM(int userA, int userB, int createdBy) throws SQLException {
        int min = Math.min(userA, userB);
        int max = Math.max(userA, userB);
        String dmKey = min + ":" + max;

        String sql =
                "INSERT INTO conversations(type, dm_key, created_by) " +
                        "VALUES('DM', ?, ?) " +
                        "ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id)";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, dmKey);
            ps.setInt(2, createdBy);
            ps.executeUpdate();
        }

        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT LAST_INSERT_ID()")) {
            if (!rs.next()) throw new SQLException("Cannot get DM conversation id");
            long convId = rs.getLong(1);
            // ensure participants exist (idempotent)
            addParticipant(convId, userA, "MEMBER", createdBy);
            addParticipant(convId, userB, "MEMBER", createdBy);
            return convId;
        }
    }

    public long createGroup(String title, int createdBy) throws SQLException {
        String sql = "INSERT INTO conversations(type, title, created_by, dm_key) VALUES('GROUP', ?, ?, NULL)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, title);
            ps.setInt(2, createdBy);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (!rs.next()) throw new SQLException("Cannot get group conversation id");
                long convId = rs.getLong(1);
                addParticipant(convId, createdBy, "ADMIN", createdBy);
                return convId;
            }
        }
    }

    public void addParticipant(long conversationId, int userId, String role, Integer addedBy) throws SQLException {
        String sql =
                "INSERT INTO conversation_participants(conversation_id, user_id, role, nickname, added_by) " +
                        "VALUES(?, ?, ?, NULL, ?) " +
                        "ON DUPLICATE KEY UPDATE left_at = NULL, role = role";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, conversationId);
            ps.setInt(2, userId);
            ps.setString(3, role);
            ps.setObject(4, addedBy, Types.INTEGER);
            ps.executeUpdate();
        }
    }

    public List<Conversation> listForUser(int userId) throws SQLException {
        List<Conversation> out = new ArrayList<>();

        String sql =
                "SELECT\n" +
                        "  c.id,\n" +
                        "  c.type,\n" +
                        "  CASE\n" +
                        "    WHEN UPPER(c.type) = 'DM' THEN (\n" +
                        "      SELECT COALESCE(\n" +
                        "               NULLIF(TRIM(cp2.nickname), ''),\n" +
                        "               CONCAT_WS(' ', u2.prenom, u2.nom)\n" +
                        "             )\n" +
                        "      FROM conversation_participants cp2\n" +
                        "      JOIN utilisateurs u2 ON u2.id = cp2.user_id\n" +
                        "      WHERE cp2.conversation_id = c.id\n" +
                        "        AND cp2.user_id <> ?\n" +
                        "        AND cp2.left_at IS NULL\n" +
                        "      LIMIT 1\n" +
                        "    )\n" +
                        "    ELSE c.title\n" +
                        "  END AS title,\n" +
                        "  c.created_at,\n" +
                        "  CASE\n" +
                        "    WHEN UPPER(c.type) = 'DM' THEN (\n" +
                        "      SELECT u2.imagelink\n" +
                        "      FROM conversation_participants cp2\n" +
                        "      JOIN utilisateurs u2 ON u2.id = cp2.user_id\n" +
                        "      WHERE cp2.conversation_id = c.id\n" +
                        "        AND cp2.user_id <> ?\n" +
                        "        AND cp2.left_at IS NULL\n" +
                        "      LIMIT 1\n" +
                        "    )\n" +
                        "    ELSE c.avatar\n" +
                        "  END AS avatar,\n" +
                        "  lm.id AS last_message_id,\n" +
                        "  lm.sender_id AS last_sender_id,\n" +
                        "  lm.body AS last_body,\n" +
                        "  lm.created_at AS last_at,\n" +
                        "  (\n" +
                        "    SELECT COUNT(*)\n" +
                        "    FROM messages m2\n" +
                        "    WHERE m2.conversation_id = c.id\n" +
                        "      AND m2.id > IFNULL(cp.last_read_message_id, 0)\n" +
                        "      AND m2.sender_id <> ?\n" +
                        "  ) AS unread_count\n" +
                        "FROM conversation_participants cp\n" +
                        "JOIN conversations c ON c.id = cp.conversation_id\n" +
                        "LEFT JOIN messages lm\n" +
                        "  ON lm.id = (SELECT MAX(id) FROM messages m WHERE m.conversation_id = c.id)\n" +
                        "WHERE cp.user_id = ?\n" +
                        "  AND cp.left_at IS NULL\n" +
                        "ORDER BY COALESCE(lm.created_at, c.last_message_at, c.created_at) DESC;\n";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            // used in DM title subquery
            ps.setInt(1, userId);
            // used in DM avatar subquery
            ps.setInt(2, userId);
            // unread_count excludes my own messages
            ps.setInt(3, userId);
            // WHERE cp.user_id = ?
            ps.setInt(4, userId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Long lastMsgId = rs.getObject("last_message_id") == null ? null : rs.getLong("last_message_id");
                    Integer lastSender = rs.getObject("last_sender_id") == null ? null : rs.getInt("last_sender_id");

                    out.add(new Conversation(
                            rs.getLong("id"),
                            rs.getString("type"),
                            rs.getString("title"),
                            rs.getTimestamp("created_at"),
                            0, // totalMessages not needed here
                            rs.getBytes("avatar"),
                            lastMsgId,
                            lastSender,
                            rs.getString("last_body"),
                            rs.getTimestamp("last_at"),
                            rs.getInt("unread_count")
                    ));
                }
            }
        }

        return out;
    }
    public Conversation getDetails(long conversationId, int currentUserId) throws SQLException {
        String sql =
                "SELECT\n" +
                        "  c.id,\n" +
                        "  c.type,\n" +
                        "  CASE\n" +
                        "    WHEN UPPER(c.type) = 'DM' THEN (\n" +
                        "      SELECT COALESCE(\n" +
                        "               NULLIF(TRIM(cp2.nickname), ''),\n" +
                        "               CONCAT_WS(' ', u2.prenom, u2.nom)\n" +
                        "             )\n" +
                        "      FROM conversation_participants cp2\n" +
                        "      JOIN utilisateurs u2 ON u2.id = cp2.user_id\n" +
                        "      WHERE cp2.conversation_id = c.id\n" +
                        "        AND cp2.user_id <> ?\n" +
                        "        AND cp2.left_at IS NULL\n" +
                        "      LIMIT 1\n" +
                        "    )\n" +
                        "    ELSE c.title\n" +
                        "  END AS title,\n" +
                        "  c.created_at,\n" +
                        "  CASE\n" +
                        "    WHEN UPPER(c.type) = 'DM' THEN (\n" +
                        "      SELECT u2.imagelink\n" +
                        "      FROM conversation_participants cp2\n" +
                        "      JOIN utilisateurs u2 ON u2.id = cp2.user_id\n" +
                        "      WHERE cp2.conversation_id = c.id\n" +
                        "        AND cp2.user_id <> ?\n" +
                        "        AND cp2.left_at IS NULL\n" +
                        "      LIMIT 1\n" +
                        "    )\n" +
                        "    ELSE c.avatar\n" +
                        "  END AS avatar,\n" +
                        "  (SELECT COUNT(*) FROM messages m WHERE m.conversation_id = c.id) AS total_messages\n" +
                        "FROM conversations c\n" +
                        "WHERE c.id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, currentUserId);      // DM title subquery
            ps.setInt(2, currentUserId);      // DM avatar subquery
            ps.setLong(3, conversationId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                return new Conversation(
                        rs.getLong("id"),
                        rs.getString("type"),
                        rs.getString("title"),
                        rs.getTimestamp("created_at"),
                        rs.getInt("total_messages"),
                        rs.getBytes("avatar"),
                        null, null, null, null, 0
                );
            }
        }
    }

    public String getNickname(long conversationId, int userId) throws SQLException {
        String sql = "SELECT nickname FROM conversation_participants WHERE conversation_id=? AND user_id=? AND left_at IS NULL";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, conversationId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return rs.getString("nickname");
            }
        }
    }

    public List<ParticipantView> listParticipantViews(long conversationId) throws SQLException {
        List<ParticipantView> out = new ArrayList<>();

        String sql =
                "SELECT cp.conversation_id, cp.user_id, cp.role, cp.nickname, cp.added_by, " +
                        "       cp.joined_at, cp.left_at, cp.last_read_message_id, " +
                        "       u.nom AS username " +
                        "FROM conversation_participants cp " +
                        "JOIN utilisateurs u ON u.id = cp.user_id " +
                        "WHERE cp.conversation_id=? AND cp.left_at IS NULL " +
                        "ORDER BY u.nom ASC";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, conversationId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {

                    // added_by (nullable)
                    Integer addedBy = null;
                    Object ab = rs.getObject("added_by");
                    if (ab != null) addedBy = ((Number) ab).intValue();

                    // last_read_message_id (nullable + UNSIGNED => BigInteger sometimes)
                    Long lastRead = null;
                    Object lrm = rs.getObject("last_read_message_id");
                    if (lrm != null) lastRead = ((Number) lrm).longValue();

                    out.add(new ParticipantView(
                            rs.getLong("conversation_id"),
                            rs.getInt("user_id"),
                            rs.getString("role"),
                            rs.getString("nickname"),
                            addedBy,
                            rs.getTimestamp("joined_at"),
                            rs.getTimestamp("left_at"),
                            lastRead,
                            rs.getString("username") // now exists because of alias
                    ));
                }
            }
        }

        return out;
    }




    public void setNickname(long convId, int userId, String newNickOrNull) throws SQLException {
        String sql = "UPDATE conversation_participants SET nickname=? WHERE conversation_id=? AND user_id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            if (newNickOrNull == null) ps.setNull(1, java.sql.Types.VARCHAR);
            else ps.setString(1, newNickOrNull);
            ps.setLong(2, convId);
            ps.setInt(3, userId);
            ps.executeUpdate();
        }
    }


    public void updateConversationName(long conversationId, String title) throws SQLException {
        String sql = "UPDATE conversations SET title = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setLong(2, conversationId);
            ps.executeUpdate();
        }
    }

    public void updateConversationPhoto(long conversationId, byte[] avatarBytes, String mime) throws SQLException {
        String sql = "UPDATE conversations SET avatar = ?, avatar_mime = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            if (avatarBytes == null) ps.setNull(1, java.sql.Types.BLOB);
            else ps.setBytes(1, avatarBytes);

            if (mime == null || mime.isBlank()) ps.setNull(2, java.sql.Types.VARCHAR);
            else ps.setString(2, mime);

            ps.setLong(3, conversationId);
            ps.executeUpdate();
        }
    }

    public List<ParticipantView> listAllUsersExcept(int currentUserId) throws SQLException {
        List<ParticipantView> out = new ArrayList<>();

        String sql = """
        SELECT id, CONCAT(prenom, ' ', nom) AS username
        FROM utilisateurs
        WHERE id <> ?
        ORDER BY prenom, nom
    """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, currentUserId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new ParticipantView(
                            rs.getInt("id"),
                            rs.getString("username")
                    ));
                }
            }
        }

        return out;
    }



    // DM: check dm_key before creating, return existing if found
    public long createPrivateConversation(int user1, int user2) throws SQLException {

        int min = Math.min(user1, user2);
        int max = Math.max(user1, user2);
        String dmKey = min + "_" + max;

        // 1) Try insert first (fast path)
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO conversations(type, dm_key, created_by) VALUES('DM', ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, dmKey);
            ps.setInt(2, user1);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("Cannot get DM conversation id");
                long convId = keys.getLong(1);

                addParticipant(convId, user1, "MEMBER", user1);
                addParticipant(convId, user2, "MEMBER", user1);

                return convId;
            }

        } catch (SQLException ex) {
            if ("23000".equals(ex.getSQLState()) || ex.getErrorCode() == 1062) {
                try (PreparedStatement ps2 = connection.prepareStatement(
                        "SELECT id FROM conversations WHERE type='DM' AND dm_key=? LIMIT 1")) {
                    ps2.setString(1, dmKey);
                    try (ResultSet rs = ps2.executeQuery()) {
                        if (rs.next()) return rs.getLong(1);
                    }
                }
            }
            throw ex;
        }
    }


    // GROUP: create conversation then add members (+ creator as ADMIN)
    public long createGroupConversation(String title, int creatorId, List<Integer> members) throws SQLException {

        String insertSql =
                "INSERT INTO conversations(type, title, created_by, dm_key) VALUES('GROUP', ?, ?, NULL)";

        long convId;
        try (PreparedStatement ps = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, title);
            ps.setInt(2, creatorId);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("Cannot get group conversation id");
                convId = keys.getLong(1);
            }
        }

        // Ensure creator is in members, avoid duplicates
        java.util.LinkedHashSet<Integer> uniq = new java.util.LinkedHashSet<>();
        uniq.add(creatorId);
        if (members != null) uniq.addAll(members);

        for (int uid : uniq) {
            addParticipant(convId, uid, (uid == creatorId) ? "ADMIN" : "MEMBER", creatorId);
        }

        return convId;
    }
    public void deleteConversation(long conversationId, int actorId) throws SQLException {

        // must be a participant (prevents random deletes)
        if (!isParticipant(conversationId, actorId)) return;

        String type = getConversationType(conversationId); // "DM" or "GROUP"

        if ("GROUP".equalsIgnoreCase(type)) {
            String role = getRoleRaw(conversationId, actorId);
            boolean canDelete = role != null && (
                    role.equalsIgnoreCase("OWNER") || role.equalsIgnoreCase("ADMIN")
            );
            if (!canDelete) return;
        }
        // DM: allowed for any participant

        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM conversations WHERE id=?")) {
            ps.setLong(1, conversationId);
            ps.executeUpdate();
        }
    }

    private boolean isParticipant(long conversationId, int userId) throws SQLException {
        String sql = "SELECT 1 FROM conversation_participants WHERE conversation_id=? AND user_id=? LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, conversationId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private String getConversationType(long conversationId) throws SQLException {
        String sql = "SELECT type FROM conversations WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, conversationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("type");
            }
        }
        return null;
    }


    public void kickParticipant(long conversationId, int actorId, int targetId) throws SQLException {
        if (actorId == targetId) return;

        // enforce role
        String actorRole = getRoleRaw(conversationId, actorId);
        boolean canManage = actorRole != null && (
                actorRole.equalsIgnoreCase("ADMIN")
        );
        if (!canManage) return;

        String sql = "DELETE FROM conversation_participants WHERE conversation_id=? AND user_id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, conversationId);
            ps.setInt(2, targetId);
            ps.executeUpdate();
        }
    }

    public void addMembers(long conversationId, int addedBy, List<Integer> userIds) throws SQLException {
        String sql = """
        INSERT INTO conversation_participants(conversation_id, user_id, added_by, joined_at, left_at)
        VALUES (?, ?, ?, NOW(), NULL)
        ON DUPLICATE KEY UPDATE
            added_by = VALUES(added_by),
            joined_at = CASE WHEN left_at IS NOT NULL THEN NOW() ELSE joined_at END,
            left_at = NULL
    """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (Integer uid : userIds) {
                if (uid == null) continue;
                if (uid == addedBy) continue; // don't add yourself

                ps.setLong(1, conversationId);
                ps.setInt(2, uid);
                ps.setInt(3, addedBy);
                ps.executeUpdate();
            }
        }
    }

    public void leaveConversation(long conversationId, int userId) throws SQLException {
        String sql = "DELETE FROM conversation_participants WHERE conversation_id=? AND user_id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, conversationId);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    public String getRoleRaw(long conversationId, int userId) throws SQLException {
        String sql = "SELECT role FROM conversation_participants WHERE conversation_id=? AND user_id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, conversationId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("role");
            }
        }
        return "MEMBER";
    }

    public String getDmDisplayName(long conversationId, int currentUserId) throws SQLException {

        String sql = """
        SELECT COALESCE(
                 NULLIF(TRIM(cp.nickname), ''),
                 CONCAT_WS(' ', u.prenom, u.nom)
               ) AS display_name
        FROM conversation_participants cp
        JOIN utilisateurs u ON u.id = cp.user_id
        WHERE cp.conversation_id = ?
          AND cp.user_id <> ?
          AND cp.left_at IS NULL
        LIMIT 1
    """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, conversationId);
            ps.setInt(2, currentUserId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("display_name");
            }
        }
        return "Discussion";
    }

    public void markConversationRead(long convId, int userId, long lastMsgId) throws SQLException {
        String sql = """
        UPDATE conversation_participants
        SET last_read_message_id = GREATEST(IFNULL(last_read_message_id,0), ?)
        WHERE conversation_id = ?
          AND user_id = ?
          AND left_at IS NULL
    """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, lastMsgId);
            ps.setLong(2, convId);
            ps.setInt(3, userId);
            ps.executeUpdate();
        }
    }

    public byte[] getUserAvatar(int userId) throws SQLException {
        String sql = "SELECT imagelink FROM utilisateurs WHERE id = ? LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                byte[] b = rs.getBytes(1);
                return (b != null && b.length > 0) ? b : null;
            }
        }
    }

    public byte[] getDmAvatar(long conversationId, int currentUserId) throws SQLException {

        String sql = """
        SELECT u.imagelink
        FROM conversation_participants cp
        JOIN utilisateurs u ON u.id = cp.user_id
        WHERE cp.conversation_id = ?
          AND cp.user_id <> ?
          AND cp.left_at IS NULL
        LIMIT 1
    """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, conversationId);
            ps.setInt(2, currentUserId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    byte[] b = rs.getBytes(1); // NULL if imagelink is NULL
                    return (b != null && b.length > 0) ? b : null;
                }
            }
        }
        return null;
    }

}

