package model;
import java.sql.Timestamp;
public class Message {
    private long id;
    private long conversationId;
    private int senderId;
    private String body;
    private Timestamp createdAt;
    private Timestamp editedAt;
    private String kind;

    public Message() {}

    public Message(long id, long conversationId, int senderId, String body, Timestamp createdAt, Timestamp editedAt, String kind) {
        this.id = id;
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.body = body;
        this.createdAt = createdAt;
        this.editedAt = editedAt;
        this.kind = kind;
    }

    public Message(long conversationId, int senderId, String body) {
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.body = body;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getConversationId() { return conversationId; }
    public void setConversationId(long conversationId) { this.conversationId = conversationId; }

    public int getSenderId() { return senderId; }
    public void setSenderId(int senderId) { this.senderId = senderId; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getEditedAt() { return editedAt; }
    public void setEditedAt(Timestamp editedAt) { this.editedAt = editedAt; }

    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }

    @Override
    public String toString() {
        return "Message{id=" + id + ", conversationId=" + conversationId + ", senderId=" + senderId + ", body='" + body + "', createdAt=" + createdAt + ", editedAt=" + editedAt + ", kind='" + kind + "'}";
    }
}
