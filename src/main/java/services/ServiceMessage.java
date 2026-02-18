package services;

import model.Message;
import model.ParticipantView;
import utils.DBConnection;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceMessage implements IService<Message> {
    public record AttachmentMeta(long id, String fileName, String mimeType, long sizeBytes) {}

    private final Connection connection;

    public ServiceMessage() {
        connection = DBConnection.getInstance().getConnection();
    }

    public long ajouter(Message message) throws SQLException {

        String insert = "INSERT INTO messages(conversation_id, sender_id, body, kind) VALUES (?,?,?,?)";

        long newMessageId;

        try (PreparedStatement ps = connection.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, message.getConversationId());
            ps.setInt(2, message.getSenderId());
            ps.setString(3, message.getBody());
            ps.setString(4, message.getKind() == null ? "TEXT" : message.getKind());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (!rs.next()) throw new SQLException("Insert message failed: no generated key returned.");
                newMessageId = rs.getLong(1);
            }
        }

        String upd = "UPDATE conversations SET last_message_id = ?, last_message_at = NOW() WHERE id = ?";
        try (PreparedStatement ps2 = connection.prepareStatement(upd)) {
            ps2.setLong(1, newMessageId);
            ps2.setLong(2, message.getConversationId());
            ps2.executeUpdate();
        }

        return newMessageId;
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

    public List<Message> listByConversation(long conversationId, int limit) throws SQLException {
        List<Message> out = new ArrayList<>();
        String sql =
                "SELECT id, conversation_id, sender_id, body, kind, created_at, edited_at\n" +
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
                            rs.getTimestamp("edited_at"),
                            rs.getString("kind")
                    ));
                }
            }
        }
        return out;
    }


    public void markRead(long conversationId, int userId, long lastReadMessageId) throws SQLException {
        String sql =
                "UPDATE conversation_participants " +
                        "SET last_read_message_id = GREATEST(IFNULL(last_read_message_id,0), ?) " +
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

    public List<ParticipantView> listReadersForMessage(long convId, long messageId, int currentUserId) throws SQLException {
        List<ParticipantView> out = new ArrayList<>();
        String sql = """
        SELECT u.id,
               CONCAT(u.prenom, ' ', u.nom) AS username
        FROM conversation_participants cp
        JOIN utilisateurs u ON u.id = cp.user_id
        WHERE cp.conversation_id = ?
          AND cp.left_at IS NULL
          AND cp.user_id <> ?
          AND IFNULL(cp.last_read_message_id, 0) >= ?
        ORDER BY u.prenom, u.nom
    """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, convId);
            ps.setInt(2, currentUserId);
            ps.setLong(3, messageId);
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

    public String getSenderDisplayName(int senderId) throws SQLException {
        String sql = "SELECT CONCAT(prenom, ' ', nom) AS name FROM utilisateurs WHERE id = ? LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, senderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("name");
            }
        }
        return "Utilisateur";
    }

    public void insertAttachmentBlob(long messageId,
                                     String fileName,
                                     String mimeType,
                                     long sizeBytes,
                                     InputStream inputStream) throws SQLException {
        String sql = """
        INSERT INTO message_attachments(message_id, file_name, mime_type, size_bytes, data)
        VALUES (?,?,?,?,?)
    """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, messageId);
            ps.setString(2, fileName);
            ps.setString(3, mimeType);
            ps.setLong(4, sizeBytes);
            ps.setBinaryStream(5, inputStream, sizeBytes);
            ps.executeUpdate();
        }
    }

    public AttachmentMeta getAttachmentMeta(long messageId) throws SQLException {
        String sql = "SELECT id, file_name, mime_type, size_bytes FROM message_attachments WHERE message_id = ? LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, messageId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new AttachmentMeta(
                        rs.getLong("id"),
                        rs.getString("file_name"),
                        rs.getString("mime_type"),
                        rs.getLong("size_bytes")
                );
            }
        }
    }

    public byte[] readAttachmentBytes(long attachmentId) throws SQLException, IOException {
        String sql = "SELECT data FROM message_attachments WHERE id = ? LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, attachmentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                try (InputStream in = rs.getBinaryStream(1)) {
                    return in.readAllBytes();
                }
            }
        }
    }


}
