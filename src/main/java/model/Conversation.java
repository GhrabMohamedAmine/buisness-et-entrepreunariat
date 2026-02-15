package model;

import java.sql.Timestamp;

public class Conversation {
    private long id;
    private String type;
    private String title;

    private Timestamp createdAt;
    private int totalMessages;
    private byte[] avatar;

    private Long lastMessageId;
    private Integer lastSenderId;
    private String lastBody;
    private Timestamp lastAt;

    private int unreadCount;

    public Conversation(long id, String type, String title,
                        Long lastMessageId, Integer lastSenderId,
                        String lastBody, Timestamp lastAt, int unreadCount) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.createdAt = null;
        this.totalMessages = 0;
        this.avatar = null;
        this.lastMessageId = lastMessageId;
        this.lastSenderId = lastSenderId;
        this.lastBody = lastBody;
        this.lastAt = lastAt;
        this.unreadCount = unreadCount;
    }

    public Conversation(long id, String type, String title,
                        Timestamp createdAt, int totalMessages, byte[] avatar,
                        Long lastMessageId, Integer lastSenderId,
                        String lastBody, Timestamp lastAt, int unreadCount) {
        this.id = id;
        this.type = type;
        this.title = title;

        this.createdAt = createdAt;
        this.totalMessages = totalMessages;
        this.avatar = avatar;

        this.lastMessageId = lastMessageId;
        this.lastSenderId = lastSenderId;
        this.lastBody = lastBody;
        this.lastAt = lastAt;
        this.unreadCount = unreadCount;
    }

    public long getId() { return id; }
    public String getType() { return type; }
    public String getTitle() { return title; }
    public Timestamp getCreatedAt() { return createdAt; }
    public int getTotalMessages() { return totalMessages; }
    public byte[] getAvatar() { return avatar; }
    public Long getLastMessageId() { return lastMessageId; }
    public Integer getLastSenderId() { return lastSenderId; }
    public String getLastBody() { return lastBody; }
    public Timestamp getLastAt() { return lastAt; }
    public int getUnreadCount() { return unreadCount; }

    public void setId(long id) {
        this.id = id;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public void setTotalMessages(int totalMessages) {
        this.totalMessages = totalMessages;
    }

    public void setAvatar(byte[] avatar) {
        this.avatar = avatar;
    }

    public void setLastMessageId(Long lastMessageId) {
        this.lastMessageId = lastMessageId;
    }

    public void setLastSenderId(Integer lastSenderId) {
        this.lastSenderId = lastSenderId;
    }

    public void setLastBody(String lastBody) {
        this.lastBody = lastBody;
    }

    public void setLastAt(Timestamp lastAt) {
        this.lastAt = lastAt;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }
}
