package model;

import java.sql.Timestamp;

public class ParticipantView extends ConversationParticipant {

    private String username;

    public ParticipantView() {}

    public ParticipantView(long conversationId,
                           int userId,
                           String role,
                           String nickname,
                           Integer addedBy,
                           Timestamp joinedAt,
                           Timestamp leftAt,
                           Long lastReadMessageId,
                           String username) {

        super(conversationId, userId, role, nickname, addedBy, joinedAt, leftAt, lastReadMessageId);
        this.username = username;
    }

    public ParticipantView(int userId, String username) {
        super();
        setUserId(userId);
        this.username = username;
    }


    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}





