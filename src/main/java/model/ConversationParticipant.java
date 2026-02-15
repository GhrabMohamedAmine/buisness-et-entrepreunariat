package model;

import java.sql.Timestamp;

public class ConversationParticipant {
    private long conversationId;
    private int userId;
    private String role;                 // MEMBER / ADMIN
    private String nickname;
    private Integer addedBy;
    private Timestamp joinedAt;
    private Timestamp leftAt;
    private Long lastReadMessageId;


    public ConversationParticipant() {
    }

    public ConversationParticipant(long conversationId, int userId, String role, String nickname, Integer addedBy, Timestamp joinedAt, Timestamp leftAt, Long lastReadMessageId) {
        this.conversationId = conversationId;
        this.userId = userId;
        this.role = role;
        this.nickname = nickname;
        this.addedBy = addedBy;
        this.joinedAt = joinedAt;
        this.leftAt = leftAt;
        this.lastReadMessageId = lastReadMessageId;
    }

    public long getConversationId() {
        return conversationId;
    }

    public void setConversationId(long conversationId) {
        this.conversationId = conversationId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public Integer getAddedBy() {
        return addedBy;
    }

    public void setAddedBy(Integer addedBy) {
        this.addedBy = addedBy;
    }

    public Timestamp getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(Timestamp joinedAt) {
        this.joinedAt = joinedAt;
    }

    public Timestamp getLeftAt() {
        return leftAt;
    }

    public void setLeftAt(Timestamp leftAt) {
        this.leftAt = leftAt;
    }

    public Long getLastReadMessageId() {
        return lastReadMessageId;
    }

    public void setLastReadMessageId(Long lastReadMessageId) {
        this.lastReadMessageId = lastReadMessageId;
    }
}
