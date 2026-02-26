package model;

public class CallSignal {

    public String type;          // RING, ACCEPT, REJECT, END
    public int conversationId;
    public int fromUserId;
    public int toUserId;
    public String callKind;      // AUDIO or VIDEO
    public String fromName;
    public String fromAvatarUrl;

    public CallSignal() {}
}
