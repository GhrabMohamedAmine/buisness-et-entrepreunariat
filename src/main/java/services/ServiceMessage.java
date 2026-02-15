package services;

import model.Message;
import utils.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceMessage implements IService<Message> {

    private final Connection connection;

    public ServiceMessage() {
        connection = DBConnection.getInstance().getConnection();
    }

    public void ajouter(Message message) throws SQLException {

        String insert = "INSERT INTO messages(conversation_id, sender_id, body) VALUES(?,?,?)";

        long newMessageId;

        try (PreparedStatement ps = connection.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, message.getConversationId());
            ps.setInt(2, message.getSenderId());
            ps.setString(3, message.getBody());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                newMessageId = rs.getLong(1);
            }
        }

        String upd = "UPDATE conversations SET last_message_id = ?, last_message_at = NOW() WHERE id = ?";
        try (PreparedStatement ps2 = connection.prepareStatement(upd)) {
            ps2.setLong(1, newMessageId);
            ps2.setLong(2, message.getConversationId());
            ps2.executeUpdate();
        }
    }




    @Override
    public void modifier(Message message) throws SQLException {
        String sql = """
        UPDATE messages
        SET body = ?, edited_at = NOW()
        WHERE id = ? AND sender_id = ? AND body <> ?
    """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, message.getBody());
            ps.setLong(2, message.getId());
            ps.setInt(3, message.getSenderId());      // ownership check
            ps.setString(4, message.getBody());       // prevents marking edited if unchanged
            ps.executeUpdate();
        }
    }


    @Override
    public void supprimer(Message message) throws SQLException {
        String sql = "DELETE FROM messages WHERE id = ? AND sender_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, message.getId());
            ps.setInt(2, message.getSenderId());  // ownership check
            ps.executeUpdate();
        }
    }



    @Override
    public List<Message> recuperer() throws SQLException {
        return new ArrayList<>();
    }

    public List<Message> listByConversation(long conversationId, int limit) throws SQLException {
        List<Message> out = new ArrayList<>();
        String sql =
                "SELECT id, conversation_id, sender_id, body, created_at, edited_at\n" +
                        "FROM messages\n" +
                        "WHERE conversation_id = ?\n" +
                        "ORDER BY created_at ASC\n" +
                        "LIMIT ?\n";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, conversationId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new Message(
                            rs.getLong("id"),
                            rs.getLong("conversation_id"),
                            rs.getInt("sender_id"),
                            rs.getString("body"),
                            rs.getTimestamp("created_at"),
                            rs.getTimestamp("edited_at")
                    ));
                }
            }
        }
        return out;
    }

    public void markRead(long conversationId, int userId, long lastReadMessageId) throws SQLException {
        String sql =
                "UPDATE conversation_participants " +
                        "SET last_read_message_id=? " +
                        "WHERE conversation_id=? AND user_id=? AND left_at IS NULL";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, lastReadMessageId);
            ps.setLong(2, conversationId);
            ps.setInt(3, userId);
            ps.executeUpdate();
        }
    }

    public long getMaxReadByOthers(long conversationId, int currentUserId) throws SQLException {
        String sql =
                "SELECT IFNULL(MAX(last_read_message_id), 0) AS other_last_read " +
                        "FROM conversation_participants " +
                        "WHERE conversation_id = ? AND user_id <> ? AND left_at IS NULL";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, conversationId);
            ps.setInt(2, currentUserId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong("other_last_read");
            }
        }
    }

}
