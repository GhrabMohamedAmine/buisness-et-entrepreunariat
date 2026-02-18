package controller;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import model.Conversation;
import model.Message;
import services.ServiceConversation;
import services.ServiceMessage;
import org.kordamp.ikonli.javafx.FontIcon;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import javafx.scene.layout.StackPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.util.Duration;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.paint.Color;
import javafx.scene.image.Image;
import javafx.scene.paint.ImagePattern;
import java.io.ByteArrayInputStream;
import javafx.stage.FileChooser;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import java.io.*;
import model.ParticipantView;
import java.util.*;
import java.sql.SQLException;
import java.nio.file.Files;
import java.awt.Desktop;
import java.net.URI;
import java.nio.file.Path;
import com.sun.jna.NativeLibrary;
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent;
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery;
import java.nio.file.Path;
import javafx.application.Platform;
import javafx.embed.swing.SwingNode;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent;
import javax.swing.SwingUtilities;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class chatController {


    @FXML private VBox conversationContainer;
    @FXML private VBox messagesContainer;
    @FXML private Label chatTitle;
    @FXML private Label chatSubtitle;
    @FXML private TextField messageInput;
    @FXML private ScrollPane messagesScroll;
    @FXML private Button sendButton;
    @FXML private Button optionsBtn;
    @FXML private StackPane drawerOverlay;
    @FXML private VBox drawer;
    @FXML private ListView<ParticipantView> membersList;
    @FXML private VBox secChatInfo, secCustomize, secMembers, secMedia;
    @FXML private FontIcon chevChatInfo, chevCustomize, chevMembers, chevMedia;
    @FXML private Label drawerTitle;
    @FXML private Label conversationIdLabel;
    @FXML private Label createdAtLabel;
    @FXML private Label conversationTypeLabel;
    @FXML private Label totalMessagesLabel;
    @FXML private Circle drawerAvatarCircle;
    @FXML private VBox emptyState;
    @FXML private Circle chatAvatarCircle;
    @FXML private FontIcon chatAvatarIcon;
    @FXML private HBox chatHeader;
    @FXML private HBox composerBar;
    @FXML private StackPane root;
    @FXML private StackPane chatNameModalOverlay;
    @FXML private TextField chatNameField;
    @FXML private Button saveChatNameBtn;
    @FXML private StackPane nicknamesOverlay;
    @FXML private ListView<ParticipantView> nicknamesList;
    @FXML private StackPane newMessageOverlay;
    @FXML private Label newMsgTitle;
    @FXML private VBox stepChooseType;
    @FXML private VBox stepPickUsers;
    @FXML private VBox choicePrivate;
    @FXML private VBox choiceGroup;
    @FXML private TextField userSearchField;
    @FXML private ListView<ParticipantView> usersPickList;
    @FXML private HBox groupNameRow;
    @FXML private TextField groupNameField;
    @FXML private Button newMsgBackBtn;
    @FXML private Button newMsgPrimaryBtn;
    @FXML private Button newMsgCancelBtn;
    @FXML private ScrollPane conversationScroll;
    @FXML private Button addPeopleBtn;
    @FXML private Label dangerActionLabel;
    @FXML private FontIcon dangerActionIcon;
    @FXML private VBox customizeSection;
    @FXML private FontIcon sendIcon;

    @FXML private void toggleChatInfo()   { toggleSection(secChatInfo,   chevChatInfo); }
    @FXML private void toggleCustomize()  { toggleSection(secCustomize,  chevCustomize); }
    @FXML private void toggleMembers()    { toggleSection(secMembers,    chevMembers); }
    @FXML private void toggleMedia()      { toggleSection(secMedia,      chevMedia); }

    private enum Role { OWNER, ADMIN, MEMBER }
    private boolean canManage(Role r) { return r == Role.OWNER || r == Role.ADMIN; }
    private boolean canDelete(Role r) { return r == Role.OWNER || r == Role.ADMIN; }
    private enum NewMsgMode { PRIVATE, GROUP }
    private NewMsgMode newMsgMode = NewMsgMode.PRIVATE;
    private boolean canManageMembersInSelectedConversation = false;
    private boolean canDeleteSelectedConversation = false;
    private long selectedConversationId = -1;
    private final ObservableList<ParticipantView> allUsers = FXCollections.observableArrayList();
    private final ObservableList<ParticipantView> filteredUsers = FXCollections.observableArrayList();
    private final ObservableList<ParticipantView> members = FXCollections.observableArrayList();
    private final ObservableList<ParticipantView> nicknamesData = FXCollections.observableArrayList();
    private boolean drawerOpen = false;
    private static final Locale FR = Locale.FRENCH;
    private final ServiceConversation conversationService = new ServiceConversation();
    private final ServiceMessage messageService = new ServiceMessage();
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm", FR);
    private static final DateTimeFormatter DAY_NAME_FMT =
            DateTimeFormatter.ofPattern("EEEE", FR);
    private static final DateTimeFormatter SHORT_DATE_FMT =
            DateTimeFormatter.ofPattern("d MMM", FR);
    private static final DateTimeFormatter FULL_DATE_FMT =
            DateTimeFormatter.ofPattern("d MMM yyyy", FR);
    private static final DateTimeFormatter CREATED_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd", FR);
    private Conversation selectedConversation;
    private final java.util.Timer dayTimer = new java.util.Timer(true);
    private Message editingMessage = null;
    private javafx.scene.effect.GaussianBlur blur = new javafx.scene.effect.GaussianBlur(12);
    private Long nicknamesConvId = null;
    private boolean addMembersMode = false;
    private final Map<Long, VBox> conversationNodeById = new HashMap<>();
    private final ObjectProperty<Conversation> selectedConversationProperty = new SimpleObjectProperty<>(null);
    private final Map<Long, Label> unreadBadgeByConvId = new HashMap<>();
    private final Map<Long, Label> timeLabelByConvId  = new HashMap<>();
    private final Map<Integer, String> senderNameCache = new HashMap<>();
    private static boolean VLC_INIT_DONE = false;
    private int currentUserId = 1;
    //==========================
    //HELPER METHODS
    //==========================

    private static void initVlcOnce() {
        Path vlcDir = Path.of(System.getProperty("user.dir"), "vlc").toAbsolutePath();

        System.setProperty("jna.library.path", vlcDir.toString());
        System.setProperty("VLC_PLUGIN_PATH", vlcDir.resolve("plugins").toString());

        boolean ok = new NativeDiscovery().discover();
        System.out.println("VLC discover ok=" + ok + " dir=" + vlcDir);
    }

    private static void vlcEdt(Runnable r) {
        javax.swing.SwingUtilities.invokeLater(r);
    }


    private static String displayName(ParticipantView p) {
        String n = p.getNickname();
        return (n == null || n.isBlank()) ? p.getUsername() : n.trim();
    }

    private void setSendMode() {
        sendButton.setText("");
        sendButton.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        sendIcon.setIconLiteral("mdi2s-send");   // normal send icon
    }

    private void setEditMode() {
        sendButton.setText("");
        sendButton.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        sendIcon.setIconLiteral("mdi2c-check");  // confirm/edit icon
    }

    private void applyRoleUiForConversation(Conversation conv) throws SQLException {

        boolean isGroup = "GROUP".equalsIgnoreCase(conv.getType());

        boolean manage = false;
        if (isGroup) {
            String role = conversationService.getRoleRaw(conv.getId(), currentUserId);
            manage = role != null && (role.equalsIgnoreCase("ADMIN") || role.equalsIgnoreCase("OWNER"));
        }

        canManageMembersInSelectedConversation = manage;

        // your rule: DM delete allowed for participant, GROUP delete only admin/owner
        canDeleteSelectedConversation = !isGroup || manage;

        // Add people only for GROUP admins/owners
        boolean showAddPeople = isGroup && manage;
        addPeopleBtn.setVisible(showAddPeople);
        addPeopleBtn.setManaged(showAddPeople);

        // Customize section hidden for DM
        customizeSection.setVisible(isGroup);
        customizeSection.setManaged(isGroup);

        // Danger action label + icon
        if (canDeleteSelectedConversation) {
            dangerActionLabel.setText("Supprimer la discussion");
            dangerActionIcon.setIconLiteral("mdi2t-trash-can-outline");
        } else {
            dangerActionLabel.setText("Quitter la discussion");
            dangerActionIcon.setIconLiteral("mdi2l-logout");
        }
    }

    @FXML
    private void onDangerAction() {
        if (selectedConversation == null) return;

        long convId = selectedConversation.getId();

        try {
            if (canDeleteSelectedConversation) {
                conversationService.deleteConversation(convId, currentUserId);
            } else {
                conversationService.leaveConversation(convId, currentUserId);
            }
            // ✅ hide drawer immediately (fix)
            closeDrawer();

            // remove sidebar item without full reload
            VBox node = conversationNodeById.remove(convId);
            if (node != null) conversationContainer.getChildren().remove(node);

            // if we were viewing it -> go back to empty state
            if (selectedConversationId == convId) {
                clearSelectionAndGoEmpty();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void setSelectedConversationStyle(long newId) {
        if (selectedConversationId > 0) {
            VBox oldNode = conversationNodeById.get(selectedConversationId);
            if (oldNode != null) {
                oldNode.getStyleClass().remove("chat-item-selected");
                if (!oldNode.getStyleClass().contains("chat-item")) oldNode.getStyleClass().add("chat-item");
            }
        }

        if (newId > 0) {
            VBox newNode = conversationNodeById.get(newId);
            if (newNode != null) {
                newNode.getStyleClass().remove("chat-item");
                if (!newNode.getStyleClass().contains("chat-item-selected")) newNode.getStyleClass().add("chat-item-selected");
            }
        }

        selectedConversationId = newId;
    }



    // =========================
    // INITIALIZATION
    // =========================

    @FXML
    public void initialize() {
        initVlcOnce();
        newMsgPrimaryBtn.getStyleClass().add("overlay-btn-primary");
        newMsgBackBtn.getStyleClass().add("overlay-btn");
        newMsgCancelBtn.getStyleClass().add("overlay-btn");
        selectedConversationProperty.set(null);
        BooleanBinding hasSelection = selectedConversationProperty.isNotNull();
        // show header + composer only when selected
        chatHeader.visibleProperty().bind(hasSelection);
        chatHeader.managedProperty().bind(chatHeader.visibleProperty());

        composerBar.visibleProperty().bind(hasSelection);
        composerBar.managedProperty().bind(composerBar.visibleProperty());

        // show empty state only when NOTHING selected
        emptyState.visibleProperty().bind(hasSelection.not());
        emptyState.managedProperty().bind(emptyState.visibleProperty());

        messageInput.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE && editingMessage != null) {
                cancelEdit();
            }
        });
        loadConversations();
        membersList.getStyleClass().add("members-list");
        membersList.setCellFactory(lv -> new MemberCell());
        membersList.setItems(members);
        membersList.setFixedCellSize(48); // height of one member row

        membersList.prefHeightProperty().bind(
                membersList.fixedCellSizeProperty()
                        .multiply(Bindings.size(membersList.getItems()))
                        .add(2) // small padding
        );

        membersList.setFocusTraversable(false);

        nicknamesList.setItems(nicknamesData);
        nicknamesList.setCellFactory(lv -> new NicknamesRowCell());
        nicknamesList.setFocusTraversable(false);
        nicknamesList.setPlaceholder(new Label("No participants"));

        nicknamesList.setFixedCellSize(56); // adjust if your row is taller/shorter

        int maxRows = 6;

        nicknamesList.prefHeightProperty().bind(
                nicknamesList.fixedCellSizeProperty()
                        .multiply(Bindings.min(maxRows, Bindings.size(nicknamesData)))
                        .add(2)
        );

        nicknamesList.maxHeightProperty().bind(nicknamesList.prefHeightProperty());
        // Make sure drawer starts hidden off-screen (after layout)
        Platform.runLater(() -> drawer.setTranslateX(drawer.getWidth()));
        chatNameField.textProperty().addListener((obs, oldV, newV) -> updateSaveState());

        // --- New Message modal setup ---
        usersPickList.setItems(filteredUsers);
        usersPickList.setCellFactory(lv -> new UserPickCell()); // class below

        usersPickList.setFixedCellSize(64);
        usersPickList.prefHeightProperty().bind(
                usersPickList.fixedCellSizeProperty()
                        .multiply(Bindings.min(maxRows, Bindings.size(filteredUsers)))
                        .add(2)
        );
        usersPickList.maxHeightProperty().bind(usersPickList.prefHeightProperty());

        // Search filter
        userSearchField.textProperty().addListener((obs, o, q) -> applyUserFilter(q));
    }

    private void updateSaveState() {
        if (saveChatNameBtn == null) return;

        String typed = chatNameField.getText() == null ? "" : chatNameField.getText().trim();
        String current = chatTitle.getText() == null ? "" : chatTitle.getText().trim();

        boolean enable = !typed.isBlank() && !typed.equals(current);
        saveChatNameBtn.setDisable(!enable);
    }


    private void toggleSection(VBox content, FontIcon chev) {
        boolean open = !content.isVisible();
        content.setVisible(open);
        content.setManaged(open);
        // rotate chevron
        chev.setRotate(open ? 180 : 0);
    }

    @FXML
    private void openOptionsMenu() {
        if (drawerOpen) closeDrawer();
        else openDrawer();
    }

    @FXML
    private void closeDrawer() {
        if (!drawerOpen) return;

        TranslateTransition tt = new TranslateTransition(Duration.millis(180), drawer);
        tt.setFromX(0);
        tt.setToX(drawer.getWidth());
        tt.setOnFinished(e -> {
            drawerOverlay.setVisible(false);
            drawerOverlay.setManaged(false);
            drawerOpen = false;
        });
        tt.play();
    }

    private void openDrawer() {
        if (drawerOpen) return;

        drawerOverlay.setVisible(true);
        drawerOverlay.setManaged(true);

        if (selectedConversation != null) refreshDrawer();

        // start off-screen
        drawer.setTranslateX(drawer.getWidth());

        TranslateTransition tt = new TranslateTransition(Duration.millis(180), drawer);
        tt.setFromX(drawer.getWidth());
        tt.setToX(0);
        tt.setOnFinished(e -> drawerOpen = true);
        tt.play();
    }

    private void refreshDrawer() {
        if (selectedConversation == null) return;

        try {
            var info = conversationService.getDetails(selectedConversation.getId());

            String title;
            if ("DM".equalsIgnoreCase(info.getType())) {
                title = conversationService.getDmDisplayName(info.getId(), currentUserId);
            } else {
                title = (info.getTitle() == null || info.getTitle().isBlank()) ? "Discussion" : info.getTitle();
            }
            drawerTitle.setText(title);
            conversationIdLabel.setText(String.valueOf(info.getId()));

            if (info.getCreatedAt() != null) {
                var dt = info.getCreatedAt().toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime();
                createdAtLabel.setText(dt.toLocalDate().format(CREATED_FMT));
            } else {
                createdAtLabel.setText("-");
            }

            conversationTypeLabel.setText("GROUP".equalsIgnoreCase(info.getType()) ? "Group" : "DM");
            totalMessagesLabel.setText(String.valueOf(info.getTotalMessages()));

            // avatar
            applyAvatar(drawerAvatarCircle, info.getAvatar(), info.getType());
            // permissions
            applyRoleUiForConversation(info);
            // participants
            loadMembers(selectedConversation.getId());

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private void applyAvatar(Circle circle, byte[] avatarBytes, String type) {
        try {
            // 1) DB avatar
            if (avatarBytes != null && avatarBytes.length > 0) {
                Image img = new Image(new ByteArrayInputStream(avatarBytes), 0, 0, true, true);
                circle.setFill(new ImagePattern(img));
                return;
            }

            // 2) fallback by type (RESOURCE PATH)
            String file = "GROUP".equalsIgnoreCase(type) ? "group-default.png" : "user-default.png";
            String resourcePath = "/assets/" + file;

            var url = getClass().getResource(resourcePath);
            if (url == null) {
                circle.setFill(javafx.scene.paint.Color.LIGHTGRAY);
                System.err.println("Missing resource: " + resourcePath);
                return;
            }

            Image img = new Image(url.toExternalForm(), 0, 0, true, true);
            circle.setFill(new ImagePattern(img));

        } catch (Exception ex) {
            circle.setFill(javafx.scene.paint.Color.LIGHTGRAY);
            ex.printStackTrace();
        }
    }



    private void setSelectedConversation(Conversation conv) {
        selectedConversation = conv;
        boolean has = (conv != null);

        emptyState.setVisible(!has);
        emptyState.setManaged(!has);

        chatHeader.setVisible(has);
        chatHeader.setManaged(has);

        composerBar.setVisible(has);
        composerBar.setManaged(has);

        if (!has) {
            chatTitle.setText("");
            chatSubtitle.setText("");
            messagesContainer.getChildren().clear();
            closeDrawer();
        }
    }


    // =========================
    // LOAD TIME
    // =========================
    private String formatConversationTime(java.sql.Timestamp ts) {
        if (ts == null) return "";

        var dt = ts.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

        LocalDate messageDate = dt.toLocalDate();
        LocalDate today = LocalDate.now(ZoneId.systemDefault());

        if (messageDate.equals(today)) {
            return dt.toLocalTime().format(TIME_FMT);
        }

        if (messageDate.equals(today.minusDays(1))) {
            return "Hier";
        }

        if (messageDate.isAfter(today.minusDays(7))) {
            return dt.format(DAY_NAME_FMT);
        }

        if (messageDate.getYear() == today.getYear()) {
            return dt.format(SHORT_DATE_FMT);
        }

        return dt.format(FULL_DATE_FMT);
    }

    private String formatBubbleTime(java.sql.Timestamp ts) {
        if (ts == null) return "";

        var dt = ts.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

        LocalDate messageDate = dt.toLocalDate();
        LocalDate today = LocalDate.now(ZoneId.systemDefault());

        if (messageDate.equals(today)) {
            return dt.toLocalTime().format(TIME_FMT);
        }

        if (messageDate.equals(today.minusDays(1))) {
            return "Hier " + dt.toLocalTime().format(TIME_FMT);
        }

        if (messageDate.isAfter(today.minusDays(7))) {
            return dt.format(DAY_NAME_FMT) + " " +
                    dt.toLocalTime().format(TIME_FMT);
        }

        if (messageDate.getYear() == today.getYear()) {
            return dt.format(SHORT_DATE_FMT) + " " +
                    dt.toLocalTime().format(TIME_FMT);
        }

        return dt.format(FULL_DATE_FMT) + " " +
                dt.toLocalTime().format(TIME_FMT);
    }
    // =========================
    // LOAD CONVERSATIONS
    // =========================
    private void loadConversations() {
        conversationContainer.getChildren().clear();
        conversationNodeById.clear();

        try {
            List<Conversation> list = conversationService.listForUser(currentUserId);

            for (Conversation conv : list) {
                VBox item = buildConversationItem(conv);

                conversationNodeById.put(conv.getId(), item);
                conversationContainer.getChildren().add(item);
            }

            // re-apply selection if still visible
            if (selectedConversationId > 0) {
                setSelectedConversationStyle(selectedConversationId);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private VBox buildConversationItem(Conversation conv) {
        boolean hasUnread = conv.getUnreadCount() > 0;

        VBox wrapper = new VBox();
        wrapper.getStyleClass().add("chat-item");  // ALWAYS
        wrapper.setPadding(new Insets(12));

        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);

        // Avatar
        Circle c = new Circle(21);
        applyAvatar(c, conv.getAvatar(), conv.getType());
        StackPane avatar = new StackPane(c);

        // Name + preview
        VBox textBox = new VBox(6);

        Label name = new Label(conv.getTitle() == null ? "Chat" : conv.getTitle());
        name.getStyleClass().add("chat-name");

        String preview = conv.getLastBody();
        if (preview == null || preview.isBlank()) preview = "Open conversation...";

        Label previewLbl = new Label(preview);
        previewLbl.getStyleClass().add("chat-preview");
        previewLbl.setWrapText(true);

        textBox.getChildren().addAll(name, previewLbl);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Right side: time + badge
        VBox rightBox = new VBox(8);
        rightBox.setAlignment(Pos.CENTER_RIGHT);

        String timeTxt = formatConversationTime(conv.getLastAt());
        Label time = new Label(timeTxt == null ? "" : timeTxt);
        time.getStyleClass().add(hasUnread ? "chat-time-unread" : "chat-time");
        rightBox.getChildren().add(time);

        Label badge = new Label(String.valueOf(conv.getUnreadCount()));
        badge.getStyleClass().add("chat-badge");

        // show/hide (IMPORTANT: managed too)
        badge.setVisible(hasUnread);
        badge.setManaged(hasUnread);

        rightBox.getChildren().add(badge);

        timeLabelByConvId.put(conv.getId(), time);
        unreadBadgeByConvId.put(conv.getId(), badge);
        row.getChildren().addAll(avatar, textBox, spacer, rightBox);
        wrapper.getChildren().add(row);

        wrapper.setOnMouseClicked(e -> {
            try {
                selectConversation(conv);
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
            clearUnreadUI(conv.getId());
        });
        return wrapper;
    }


    private void highlightSelected(VBox selected) {
        for (var node : conversationContainer.getChildren()) {
            node.getStyleClass().remove("chat-item-selected");
            node.getStyleClass().add("chat-item");
        }
        selected.getStyleClass().remove("chat-item");
        selected.getStyleClass().add("chat-item-selected");
    }

    // =========================
    // LOAD MESSAGES
    // =========================

    private void showBubbleMenu(Node bubble, Message msg) {
        ContextMenu menu = new ContextMenu();
        menu.getStyleClass().add("bubble-menu");

        MenuItem edit = new MenuItem("Modifier");
        edit.getStyleClass().add("bubble-menu-item");

        MenuItem delete = new MenuItem("Supprimer");
        delete.getStyleClass().addAll("bubble-menu-item", "danger");

        edit.setOnAction(e -> beginEditMessage(msg));
        delete.setOnAction(e -> {
            try {
                msg.setSenderId(currentUserId);       // ownership enforcement
                messageService.supprimer(msg);        // permanent delete
                loadMessages(msg.getConversationId());
                loadConversations();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        menu.getItems().addAll(edit, new SeparatorMenuItem(), delete);

        // show at cursor position (native style)
        bubble.setOnContextMenuRequested(ev -> {
            if (menu.isShowing()) menu.hide();
            menu.show(bubble, ev.getScreenX(), ev.getScreenY());
            ev.consume();
        });
    }

    private void loadMessages(long conversationId) {
        messagesContainer.getChildren().clear();

        try {
            List<Message> messages = messageService.listByConversation(conversationId, 100);
            if (messages.isEmpty()) return;
            boolean isGroup = selectedConversation != null
                    && "GROUP".equalsIgnoreCase(selectedConversation.getType());

            long otherLastRead = messageService.getMaxReadByOthers(conversationId, currentUserId);

            for (Message msg : messages) {
                boolean outgoing = msg.getSenderId() == currentUserId;

                HBox row = new HBox();
                row.setAlignment(outgoing ? Pos.TOP_RIGHT : Pos.TOP_LEFT);

                VBox bubbleBox = new VBox(6);
                bubbleBox.setAlignment(outgoing ? Pos.TOP_RIGHT : Pos.TOP_LEFT);

                // GROUP incoming: sender name above bubble
                if (isGroup && !outgoing) {
                    Label sender = new Label(senderName(msg.getSenderId()));
                    sender.getStyleClass().add("sender-name");
                    bubbleBox.getChildren().add(sender);
                }

                // Bubble
                Node bubbleNode = buildMessageNode(msg, outgoing);
                bubbleBox.getChildren().add(bubbleNode);
                if (outgoing) {
                    attachBubbleMenu(bubbleNode, msg);
                    showBubbleMenu(bubbleNode, msg);
                }


                // Footer line: [seen bubbles  time] OR [time ticks] for DM
                HBox footer = new HBox(6);
                footer.setAlignment(outgoing ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

                // Optional "édité"
                if (msg.getEditedAt() != null) {
                    Label edited = new Label("édité");
                    edited.getStyleClass().add(outgoing ? "edited-out" : "edited-in");
                    footer.getChildren().add(edited);
                }

                // GROUP outgoing: add seen bubbles on the left of time (same line)
                if (isGroup && outgoing) {
                    HBox seenRow = buildSeenRow(conversationId, msg.getId(), currentUserId);
                    footer.getChildren().add(seenRow);
                }

                // Time
                Label time = new Label(formatBubbleTime(msg.getCreatedAt()));
                time.getStyleClass().add(outgoing ? "timestamp-out" : "timestamp");
                footer.getChildren().add(time);

                // DM outgoing: add ticks after time (same line)
                if (!isGroup && outgoing) {
                    FontIcon ticks = new FontIcon("mdi2c-check-all");
                    ticks.setIconSize(14);
                    if (msg.getId() <= otherLastRead) {
                        ticks.setStyle("-fx-icon-color: #7c3aed; -fx-font-family: 'Material Design Icons';");
                    } else {
                        ticks.setStyle("-fx-icon-color: #9aa0a6; -fx-font-family: 'Material Design Icons';");
                    }
                    footer.getChildren().add(ticks);
                }

                bubbleBox.getChildren().add(footer);

                row.getChildren().add(bubbleBox);
                messagesContainer.getChildren().add(row);
            }
            // Mark as read (your pointer)
            long lastId = messages.get(messages.size() - 1).getId(); // assumes ASC order
            messageService.markRead(conversationId, currentUserId, lastId);
            Platform.runLater(() -> messagesScroll.setVvalue(1.0));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String senderName(int id) {
        return senderNameCache.computeIfAbsent(id, k -> {
            try {
                return messageService.getSenderDisplayName(k);
            } catch (SQLException e) {
                return "Utilisateur";
            }
        });
    }

    private HBox buildSeenRow(long convId, long messageId, int currentUserId) {
        HBox seenRow = new HBox(4);
        seenRow.setAlignment(Pos.CENTER_RIGHT);
        seenRow.getStyleClass().add("seen-row");

        try {
            // returns users (except me) who have last_read_message_id >= messageId
            List<ParticipantView> readers = messageService.listReadersForMessage(convId, messageId, currentUserId);

            int max = 6;
            int shown = 0;

            StringBuilder names = new StringBuilder();

            for (ParticipantView p : readers) {
                if (shown >= max) break;

                Circle mini = new Circle(7);
                // simplest: default-user.png always (no DB user avatar needed)
                applyAvatar(mini, null, "DM"); // uses your default-user.png logic
                // if you later have user avatar bytes, swap to applyUserAvatar(mini, p.getUserId())

                seenRow.getChildren().add(mini);

                if (names.length() > 0) names.append(", ");
                names.append(p.getUsername());

                shown++;
            }

            int remaining = readers.size() - shown;
            if (remaining > 0) {
                Label more = new Label("+" + remaining);
                more.getStyleClass().add("seen-more");
                seenRow.getChildren().add(more);
            }

            if (readers.size() > 0) {
                Tooltip.install(seenRow, new Tooltip("Vu par: " + names));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return seenRow;
    }

    private void attachBubbleMenu(Node bubble, Message msg) {
        ContextMenu menu = new ContextMenu();
        menu.getStyleClass().add("bubble-menu");

        MenuItem edit = new MenuItem("Modifier");
        MenuItem delete = new MenuItem("Supprimer");
        delete.getStyleClass().add("danger");

        edit.setOnAction(e -> beginEditMessage(msg));

        delete.setOnAction(e -> {
            try {
                msg.setSenderId(currentUserId); // ownership check
                messageService.supprimer(msg);  // permanent delete
                loadMessages(msg.getConversationId());
                loadConversations();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        menu.getItems().addAll(edit, new SeparatorMenuItem(), delete);

        bubble.setOnContextMenuRequested(ev -> {
            menu.show(bubble, ev.getScreenX(), ev.getScreenY());
            ev.consume();
        });
    }

    private void beginEditMessage(Message msg) {
        editingMessage = msg;
        messageInput.setText(msg.getBody());
        messageInput.requestFocus();
        setEditMode();
    }

    private void cancelEdit() {
        editingMessage = null;
        messageInput.clear();
        setSendMode();
    }

    // =========================
    // SEND MESSAGE
    // =========================
    @FXML
    private void handleSend() {
        if (selectedConversation == null) return;

        String body = messageInput.getText() == null ? "" : messageInput.getText().trim();
        if (body.isBlank()) return;

        try {
            // ✅ EDIT MODE
            if (editingMessage != null) {
                String oldBody = editingMessage.getBody() == null ? "" : editingMessage.getBody().trim();

                // unchanged => cancel (no edited_at)
                if (body.equals(oldBody)) {
                    cancelEdit();
                    return;
                }

                editingMessage.setBody(body);
                editingMessage.setSenderId(currentUserId);
                messageService.modifier(editingMessage);

                loadMessages(editingMessage.getConversationId());
                loadConversations();
                cancelEdit();
                return;
            }

            // ✅ NORMAL SEND MODE
            Message msg = new Message();
            msg.setConversationId(selectedConversation.getId());
            msg.setSenderId(currentUserId);
            msg.setBody(body);

            messageService.ajouter(msg);

            messageInput.clear();
            loadMessages(selectedConversation.getId());
            loadConversations();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // =========================
    // NICKNAME EDITOR CELL
    // =========================

    @FXML
    private void onEditNicknames() {
        if (selectedConversation == null) return;

        nicknamesConvId = selectedConversation.getId();

        try {
            nicknamesData.setAll(conversationService.listParticipantViews(nicknamesConvId));
        } catch (SQLException e) {
            e.printStackTrace();
            nicknamesData.clear();
        }

        nicknamesOverlay.setVisible(true);
        nicknamesOverlay.setManaged(true);

        // IMPORTANT: refresh so cells render immediately
        nicknamesList.refresh();
    }


    @FXML
    private void closeNicknamesModal() {
        nicknamesOverlay.setVisible(false);
        nicknamesOverlay.setManaged(false);
    }

    private class NicknamesRowCell extends ListCell<ParticipantView> {

        private final HBox row = new HBox(12);
        private final Circle avatar = new Circle(18);
        private final VBox texts = new VBox(2);
        private final Label title = new Label();
        private final Label subtitle = new Label();
        private final Region spacer = new Region();

        private final Button actionBtn = new Button();
        private final TextField editor = new TextField();

        private boolean editing;
        private String original;

        NicknamesRowCell() {
            row.setAlignment(Pos.CENTER_LEFT);

            // default avatar
            Image img = new Image(getClass().getResourceAsStream("/assets/user-default.png"));
            avatar.setFill(new ImagePattern(img));

            title.getStyleClass().add("member-name");
            subtitle.getStyleClass().add("member-sub");

            texts.getChildren().addAll(title, subtitle);

            HBox.setHgrow(spacer, Priority.ALWAYS);

            actionBtn.getStyleClass().add("icon-btn");
            actionBtn.setGraphic(new FontIcon("mdi2p-pencil"));
            actionBtn.setOnAction(e -> {
                if (!editing) enterEdit();
                else commit();
            });

            // press Enter to save, Esc to cancel
            editor.setOnAction(e -> commit());
            editor.setOnKeyPressed(e -> {
                switch (e.getCode()) {
                    case ESCAPE -> exitEdit();
                }
            });

            row.getChildren().addAll(avatar, texts, spacer, actionBtn);
        }

        @Override
        protected void updateItem(ParticipantView item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setGraphic(null);
                return;
            }

            // main line: nickname if exists else username
            title.setText(displayName(item));

            // second line like FB: "Set nickname" or current nickname
            String nick = item.getNickname();
            subtitle.setText((nick == null || nick.isBlank()) ? "Set nickname" : nick.trim());

            if (editing) exitEdit();

            setGraphic(row);
        }

        private void enterEdit() {
            ParticipantView item = getItem();
            if (item == null) return;

            editing = true;
            original = item.getNickname() == null ? "" : item.getNickname().trim();

            editor.setText(original);
            editor.setPromptText("Nickname");

            // replace subtitle label by textfield
            texts.getChildren().set(1, editor);

            actionBtn.setGraphic(new FontIcon("mdi2c-check"));

            editor.requestFocus();
            editor.positionCaret(editor.getText().length());
        }

        private void commit() {
            ParticipantView item = getItem();
            if (item == null) return;

            if (nicknamesConvId == null) {   // safety: modal not initialized correctly
                exitEdit();
                return;
            }

            String typed = editor.getText() == null ? "" : editor.getText().trim();
            String old   = item.getNickname() == null ? "" : item.getNickname().trim();

            if (typed.equals(old)) { // unchanged
                exitEdit();
                return;
            }

            String toStore = typed.isEmpty() ? null : typed;

            try {
                conversationService.setNickname(nicknamesConvId, item.getUserId(), toStore);
                item.setNickname(toStore);
                if (getListView() != null) getListView().refresh();

                // If your drawer "Chat members" list uses nicknames too, refresh it:
                refreshDrawer();

            } catch (SQLException e) {
                e.printStackTrace();
            }

            exitEdit();
        }


        private void exitEdit() {
            editing = false;
            texts.getChildren().set(1, subtitle);
            actionBtn.setGraphic(new FontIcon("mdi2p-pencil"));
        }
    }



    // =========================
    // CHAT SETTINGS photo
    // =========================
    @FXML
    private void onChangeChatPhoto() {
        if (selectedConversation == null) return;

        FileChooser fc = new FileChooser();
        fc.setTitle("Choose a photo");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.webp")
        );

        java.io.File file = fc.showOpenDialog(optionsBtn.getScene().getWindow());
        if (file == null) return;

        try {
            byte[] bytes = java.nio.file.Files.readAllBytes(file.toPath());
            String mime = java.nio.file.Files.probeContentType(file.toPath()); // can be null

            conversationService.updateConversationPhoto(selectedConversation.getId(), bytes, mime);

            // refresh UI immediately
            selectedConversation.setAvatar(bytes);
            applyAvatar(chatAvatarCircle, bytes, selectedConversation.getType());
            applyAvatar(drawerAvatarCircle, bytes, selectedConversation.getType());
            loadConversations();
            refreshDrawer();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // =========================
    // CHAT SETTINGS name
    // =========================

    @FXML
    private void onChangeChatName() {
        // show only (no update)
        chatNameModalOverlay.setVisible(true);
        chatNameModalOverlay.setManaged(true);

        // optional: preload current title
        if (selectedConversation != null) {
            chatNameField.setText(chatTitle.getText());
            chatNameField.positionCaret(chatNameField.getText().length());
        } else {
            chatNameField.clear();
        }
        updateSaveState();
        chatNameField.requestFocus();
    }

    @FXML
    private void closeChatNameModal() {
        chatNameModalOverlay.setVisible(false);
        chatNameModalOverlay.setManaged(false);
    }

    @FXML
    private void saveChatName() {
        if (selectedConversation == null) return;

        String newTitle = chatNameField.getText() == null ? "" : chatNameField.getText().trim();
        if (newTitle.isBlank()) return;

        try {
            conversationService.updateConversationName(selectedConversation.getId(), newTitle);

            // update UI immediately
            chatTitle.setText(newTitle);
            drawerTitle.setText(newTitle);

            // update model (so list rebuild shows correct title)
            selectedConversation.setTitle(newTitle);

            loadConversations();
            refreshDrawer();

            closeChatNameModal();

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    // =========================
    // CHAT PARTICIPANTS
    // =========================

    private void loadAllUsersForPicker() {
        try {
            allUsers.setAll(conversationService.listAllUsersExcept(currentUserId));
            applyUserFilter(userSearchField.getText());
            usersPickList.getSelectionModel().clearSelection();
        } catch (SQLException e) {
            e.printStackTrace();
            allUsers.clear();
            filteredUsers.clear();
        }
    }


    private void applyUserFilter(String q) {
        String s = (q == null) ? "" : q.trim().toLowerCase();

        if (s.isBlank()) {
            filteredUsers.setAll(allUsers);
            return;
        }

        filteredUsers.setAll(allUsers.filtered(u -> {
            String username = (u.getUsername() == null) ? "" : u.getUsername().toLowerCase();
            String nick = (u.getNickname() == null) ? "" : u.getNickname().toLowerCase();
            return username.contains(s) || nick.contains(s);
        }));
    }


    private void loadMembers(long conversationId) {
        try {
            List<ParticipantView> list = conversationService.listParticipantViews(conversationId);
            members.setAll(list);
        } catch (SQLException e) {
            e.printStackTrace();
            members.clear();
        }
    }

    private Image getDefaultUserAvatar() {
        // put user-default.png in resources: /assets/user-default.png
        InputStream is = getClass().getResourceAsStream("/assets/user-default.png");
        return (is == null) ? null : new Image(is);
    }
    private class MemberCell extends ListCell<ParticipantView> {

        private final HBox row = new HBox(10);
        private final Circle avatar = new Circle(16);
        private final VBox texts = new VBox(2);
        private final Label name = new Label();
        private final Label sub = new Label();



        MemberCell() {
            row.getStyleClass().add("member-cell");
            name.getStyleClass().add("member-name");
            sub.getStyleClass().add("member-sub");

            texts.getChildren().addAll(name, sub);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getChildren().addAll(avatar, texts);

            // default avatar
            Image img = getDefaultUserAvatar();
            if (img != null) avatar.setFill(new ImagePattern(img));

            MenuItem kick = new MenuItem("Exclure du groupe");
            kick.getStyleClass().add("danger-item");

            ContextMenu menu = new ContextMenu(kick);
            menu.getStyleClass().add("danger-menu");

            // Right click
            setOnContextMenuRequested(ev -> {
                ParticipantView item = getItem();
                if (item == null || selectedConversation == null) return;

                boolean isGroup = "GROUP".equalsIgnoreCase(selectedConversation.getType());
                boolean isSelf  = item.getUserId() == currentUserId;

                // ✅ Only for group, only admins/owners, never self
                if (!isGroup || isSelf || !canManageMembersInSelectedConversation) {
                    ev.consume();
                    return;
                }

                kick.setOnAction(a -> {
                    try {
                        conversationService.kickParticipant(
                                selectedConversation.getId(),
                                currentUserId,
                                item.getUserId()
                        );
                        refreshDrawer();
                        loadConversations();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                });

                menu.show(this, ev.getScreenX(), ev.getScreenY());
                ev.consume();
            });
        }

        @Override
        protected void updateItem(ParticipantView item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                return;
            }

            String display = (item.getNickname() != null && !item.getNickname().isBlank())
                    ? item.getNickname().trim()
                    : item.getUsername();

            name.setText(display);

            String role = item.getRole() == null ? "" : item.getRole().toLowerCase();
            sub.setText(item.getUsername() + (role.isBlank() ? "" : " • " + role));

            setText(null);
            setGraphic(row);
        }
    }


    // =========================
    // NEW MESSAGE MODAL
    // =========================


    @FXML
    private void openNewMessageModal() {
        // Load users once per open (or cache)
        loadAllUsersForPicker();

        newMessageOverlay.setVisible(true);
        newMessageOverlay.setManaged(true);

        // default screen
        stepChooseType.setVisible(true);
        stepChooseType.setManaged(true);
        stepPickUsers.setVisible(false);
        stepPickUsers.setManaged(false);

        newMsgBackBtn.setVisible(false);
        newMsgBackBtn.setManaged(false);

        newMsgPrimaryBtn.setText("Continue");
        newMsgTitle.setText("New Message");
        newMsgBackBtn.getStyleClass().setAll("overlay-btn");
        newMsgPrimaryBtn.getStyleClass().setAll("overlay-btn-primary");
        newMsgCancelBtn.getStyleClass().setAll("overlay-btn");
        pickPrivateMode(); // default highlight + behavior
    }

    @FXML
    private void closeNewMessageModal() {
        newMessageOverlay.setVisible(false);
        newMessageOverlay.setManaged(false);

        userSearchField.clear();
        usersPickList.getSelectionModel().clearSelection();
        groupNameField.clear();
    }

    @FXML
    private void pickPrivateMode() {
        newMsgMode = NewMsgMode.PRIVATE;
        highlightChoice(true);
    }

    @FXML
    private void pickGroupMode() {
        newMsgMode = NewMsgMode.GROUP;
        highlightChoice(false);
    }

    private void highlightChoice(boolean privateSelected) {
        // simple inline style swap (you can move to CSS later)
        String sel = "-fx-border-color: #7c3aed; -fx-border-width: 2; -fx-background-color: rgba(124,58,237,0.06); -fx-padding: 14; -fx-background-radius: 14; -fx-border-radius: 14;";
        String norm = "-fx-border-color: #e5e7eb; -fx-border-width: 1; -fx-background-color: transparent; -fx-padding: 14; -fx-background-radius: 14; -fx-border-radius: 14;";

        choicePrivate.setStyle(privateSelected ? sel : norm);
        choiceGroup.setStyle(privateSelected ? norm : sel);
    }

    @FXML
    private void newMessagePrimary() {

        // STEP 1 → go to user selection screen
        if (stepChooseType.isVisible()) {
            goToPickUsers();
            return;
        }

        // STEP 2 → create conversation
        if (newMsgMode == NewMsgMode.PRIVATE) {

            ParticipantView selected =
                    usersPickList.getSelectionModel().getSelectedItem();

            if (selected == null) return;

            try {
                long convId = conversationService
                        .createPrivateConversation(currentUserId, selected.getUserId());
                loadConversations();
                openConversationById(convId);
                closeNewMessageModal();

            } catch (SQLException e) {
                e.printStackTrace();
            }

            return;
        }

        // GROUP
        List<Integer> ids =
                usersPickList.getSelectionModel()
                        .getSelectedItems()
                        .stream()
                        .map(ParticipantView::getUserId)
                        .toList();

        if (ids.isEmpty()) return;

        String title = groupNameField.getText();
        if (title == null || title.isBlank()) {
            title = "New Group";
        }

        try {
            if (addMembersMode) {
                conversationService.addMembers(selectedConversation.getId(), currentUserId, ids);

                loadMembers(selectedConversation.getId());
                refreshDrawer();
                loadConversations();

                addMembersMode = false;
                closeNewMessageModal();
                return;
            }
            long convId = conversationService.createGroupConversation(title, currentUserId, ids);
            loadConversations();
            openConversationById(convId);
            closeNewMessageModal();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void selectConversation(Conversation conv) throws SQLException {
        if (conv == null) return;

        selectedConversation = conv;
        selectedConversationProperty.set(conv); //UI binding

        setSelectedConversationStyle(conv.getId());

        applyAvatar(chatAvatarCircle, conv.getAvatar(), conv.getType());
        loadMessages(conv.getId());


        try {
            if ("DM".equalsIgnoreCase(conv.getType())) {
                chatTitle.setText(conversationService.getDmDisplayName(conv.getId(), currentUserId));
            } else {
                chatTitle.setText((conv.getTitle() == null || conv.getTitle().isBlank())
                        ? "Discussion"
                        : conv.getTitle());
            }
        } catch (SQLException e) {
            chatTitle.setText("Discussion");
        }

        chatSubtitle.setText("Conversation active");
        Long lastMsgId = conv.getLastMessageId();
        if (lastMsgId != null && lastMsgId > 0) {
            conversationService.markConversationRead(conv.getId(), currentUserId, lastMsgId);
        }
        clearUnreadUI(conv.getId());
        refreshDrawer();
    }

    private void clearUnreadUI(long convId) {
        Label badge = unreadBadgeByConvId.get(convId);
        if (badge != null) {
            badge.setText("0");          // optional
            badge.setVisible(false);
            badge.setManaged(false);
        }

        Label time = timeLabelByConvId.get(convId);
        if (time != null) {
            time.getStyleClass().remove("chat-time-unread");
            if (!time.getStyleClass().contains("chat-time")) {
                time.getStyleClass().add("chat-time");
            }
        }
    }



    private void clearSelectionAndGoEmpty() {
        selectedConversation = null;
        selectedConversationProperty.set(null);

        // remove sidebar highlight safely
        setSelectedConversationStyle(-1);

        // clear message view (use your real container)
        messagesContainer.getChildren().clear();
    }


    private void openConversationById(long convId) {
        try {
            List<Conversation> list = conversationService.listForUser(currentUserId);
            for (Conversation c : list) {
                if (c.getId() == convId) {
                    selectConversation(c);
                    return;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    private void goToPickUsers() {
        newMsgBackBtn.getStyleClass().setAll("overlay-btn");
        newMsgPrimaryBtn.getStyleClass().setAll("overlay-btn-primary");
        stepChooseType.setVisible(false);
        stepChooseType.setManaged(false);

        stepPickUsers.setVisible(true);
        stepPickUsers.setManaged(true);

        newMsgBackBtn.setVisible(true);
        newMsgBackBtn.setManaged(true);


        // Configure selection rules
        FontIcon backIcon = new FontIcon("mdi2c-chevron-left");
        backIcon.setIconSize(16);
        newMsgBackBtn.setGraphic(backIcon);
        if (newMsgMode == NewMsgMode.PRIVATE) {
            newMsgTitle.setText("Sélectionner des membres");
            groupNameRow.setVisible(false);
            groupNameRow.setManaged(false);

            usersPickList.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.SINGLE);
            newMsgPrimaryBtn.setText("selctionner");
            FontIcon sendIcon = new FontIcon("mdi2s-send");
            sendIcon.setIconSize(16);
            newMsgPrimaryBtn.setGraphic(sendIcon);


        } else {
            newMsgTitle.setText("Crée un groupe");
            groupNameRow.setVisible(true);
            groupNameRow.setManaged(true);

            usersPickList.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.MULTIPLE);
            newMsgPrimaryBtn.setText("créer un groupe");
            FontIcon groupIcon = new FontIcon("mdi2a-account-multiple-plus");
            groupIcon.setIconSize(16);
            newMsgPrimaryBtn.setGraphic(groupIcon);

        }

        usersPickList.getSelectionModel().clearSelection();
        userSearchField.requestFocus();
    }

    @FXML
    private void backNewMessage() {
        // back to step 1
        stepPickUsers.setVisible(false);
        stepPickUsers.setManaged(false);

        stepChooseType.setVisible(true);
        stepChooseType.setManaged(true);

        newMsgBackBtn.setVisible(false);
        newMsgBackBtn.setManaged(false);

        newMsgPrimaryBtn.setText("Continuer");
        newMsgTitle.setText("Nouveau message");

        userSearchField.clear();
        usersPickList.getSelectionModel().clearSelection();
        groupNameField.clear();
    }

    private class UserPickCell extends ListCell<ParticipantView> {
        private final HBox row = new HBox(12);
        private final Circle avatar = new Circle(18);
        private final VBox texts = new VBox(2);
        private final Label name = new Label();
        private final Label sub = new Label();
        private final Region spacer = new Region();
        private final StackPane indicator = new StackPane();
        private final Image fallback = new Image(getClass().getResourceAsStream("/assets/user-default.png"));
        // circle/check

        UserPickCell() {
            row.setAlignment(Pos.CENTER_LEFT);
            name.getStyleClass().add("member-name");
            sub.getStyleClass().add("member-sub");

            texts.getChildren().addAll(name, sub);
            HBox.setHgrow(spacer, Priority.ALWAYS);

            indicator.setMinSize(28, 28);
            indicator.setMaxSize(28, 28);
            row.getChildren().addAll(avatar, texts, spacer, indicator);

            row.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, ev -> {
                if (newMsgMode != NewMsgMode.GROUP) return;

                var lv = getListView();
                if (lv == null) return;

                ParticipantView item = getItem();
                if (item == null) return;

                var sm = lv.getSelectionModel();

                // Toggle using the actual object reference (works with filtered lists)
                if (sm.getSelectedItems().contains(item)) {
                    sm.clearSelection(lv.getItems().indexOf(item));
                } else {
                    sm.select(item);
                }

                lv.requestFocus();
                ev.consume();
                lv.refresh();
            });


        }

        @Override
        protected void updateItem(ParticipantView item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                return;
            }

            // avatar fallback (you can enhance to use item avatar if you have it)
            avatar.setFill(new ImagePattern(fallback));
            /*try {
                Image img = new Image(getClass().getResourceAsStream("/assets/user-default.png"));
                avatar.setFill(new ImagePattern(img));
            } catch (Exception ignore) {}*/

            name.setText(displayName(item));
            sub.setText(item.getUsername() == null ? "" : item.getUsername());

            boolean selected = getListView() != null
                    && getListView().getSelectionModel().getSelectedItems().contains(item);

            indicator.getChildren().clear();

            if (newMsgMode == NewMsgMode.PRIVATE) {
                Circle ring = new Circle(10);
                ring.setFill(Color.TRANSPARENT);
                ring.setStroke(selected ? Color.web("#7c3aed") : Color.web("#d1d5db"));
                ring.setStrokeWidth(2);
                indicator.getChildren().add(ring);
                if (selected) {
                    Circle dot = new Circle(6);
                    dot.setFill(Color.web("#7c3aed"));
                    indicator.getChildren().add(dot);
                }
            } else {
                Circle bg = new Circle(10);
                bg.setFill(Color.TRANSPARENT);
                bg.setStroke(selected ? Color.web("#7c3aed") : Color.web("#d1d5db"));
                bg.setStrokeWidth(2);
                indicator.getChildren().add(bg);

                if (selected) {
                    Circle dot = new Circle(6);
                    dot.setFill(Color.web("#7c3aed"));
                    indicator.getChildren().add(dot);
                }
            }

            setGraphic(row);
        }
    }

    // =========================
    // ADD MEMBER TO GROUP
    // =========================
    @FXML
    private void onAddPeople() {
        if (selectedConversation == null) return;
        if (!"GROUP".equalsIgnoreCase(selectedConversation.getType())) return;

        addMembersMode = true;
        newMsgMode = NewMsgMode.GROUP;

        loadAllUsersForPicker();

        newMessageOverlay.setVisible(true);
        newMessageOverlay.setManaged(true);

        // Jump directly to picker step (no type choice)
        stepChooseType.setVisible(false);
        stepChooseType.setManaged(false);

        stepPickUsers.setVisible(true);
        stepPickUsers.setManaged(true);

        newMsgBackBtn.setVisible(true);
        newMsgBackBtn.setManaged(true);

        newMsgTitle.setText("Add people");

        // No group name when adding members
        groupNameRow.setVisible(false);
        groupNameRow.setManaged(false);

        usersPickList.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.MULTIPLE);

        newMsgPrimaryBtn.setText("Add");
        usersPickList.getSelectionModel().clearSelection();
    }

    // =========================
    // METIER
    // =========================

    @FXML
    private void handleAttachFile() {
        if (selectedConversation == null) return;

        FileChooser fc = new FileChooser();
        fc.setTitle("Choose file(s)");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("All files", "*.*"));

        var owner = composerBar.getScene().getWindow(); // or any node you have
        List<File> files = fc.showOpenMultipleDialog(owner);
        if (files == null || files.isEmpty()) return;

        try {
            for (File f : files) {
                sendAttachment(f);
            }
            loadMessages(selectedConversation.getId());
            loadConversations();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendAttachment(File f) throws Exception {
        String mime = Files.probeContentType(f.toPath());
        if (mime == null) mime = "application/octet-stream";

        long size = Files.size(f.toPath());
        String name = f.getName();

        Message msg = new Message();
        msg.setConversationId(selectedConversation.getId());
        msg.setSenderId(currentUserId);
        msg.setBody(name);               // show filename in chat list fallback
        msg.setKind("ATTACHMENT");

        long messageId = messageService.ajouter(msg); // now returns id

        try (InputStream in = new BufferedInputStream(new FileInputStream(f))) {
            messageService.insertAttachmentBlob(messageId, name, mime, size, in);
        }
    }

    private Node buildMessageNode(Message msg, boolean outgoing) throws SQLException {
        String kind = msg.getKind();
        if ("ATTACHMENT".equalsIgnoreCase(kind)) {
            return buildAttachmentBubble(msg, outgoing);
        }
        return buildTextBubbleWithLinks(msg.getBody(), outgoing);
    }

    private Node buildTextBubbleWithLinks(String text, boolean outgoing) {
        if (text == null) text = "";

        var p = java.util.regex.Pattern.compile("(https?://\\S+)");
        var m = p.matcher(text);

        javafx.scene.text.TextFlow flow = new javafx.scene.text.TextFlow();
        flow.setMaxWidth(480);

        int last = 0;
        while (m.find()) {
            if (m.start() > last) flow.getChildren().add(new javafx.scene.text.Text(text.substring(last, m.start())));
            String url = m.group(1);
            String vid = extractYoutubeId(url);
            if (vid != null) {
                flow.getChildren().add(buildYoutubePreviewCard(url, outgoing));
            } else {
                Hyperlink link = new Hyperlink(prettifyUrl(url));
                link.setTooltip(new Tooltip(url));
                link.getStyleClass().add("link-pill");
                link.setOnAction(e -> openInBrowser(url));
                flow.getChildren().add(link);
            }

            last = m.end();
        }
        if (last < text.length()) flow.getChildren().add(new javafx.scene.text.Text(text.substring(last)));

        VBox bubble = new VBox(flow);
        bubble.setMaxWidth(480);
        bubble.getStyleClass().add(outgoing ? "bubble-out" : "bubble-in");
        return bubble;
    }

    private String prettifyUrl(String url) {
        try {
            URI u = new URI(url);
            String host = (u.getHost() == null) ? url : u.getHost().replaceFirst("^www\\.", "");
            String path = (u.getRawPath() == null) ? "" : u.getRawPath();
            String shortPath = path.length() > 22 ? path.substring(0, 22) + "…" : path;
            return host + (shortPath.isBlank() ? "" : " " + shortPath);
        } catch (Exception e) {
            return url.length() > 34 ? url.substring(0, 34) + "…" : url;
        }
    }

    private void openInBrowser(String url) {
        try {
            Desktop.getDesktop().browse(new URI(url));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private Node buildAttachmentBubble(Message msg, boolean outgoing) throws SQLException {
        var meta = messageService.getAttachmentMeta(msg.getId());
        if (meta == null) {
            Label fallback = new Label(msg.getBody());
            fallback.getStyleClass().add(outgoing ? "bubble-out" : "bubble-in");
            return fallback;
        }

        String mime = meta.mimeType();
        if (mime.startsWith("image/")) return buildInlineImage(meta, outgoing);
        if (mime.startsWith("video/")) return buildInlineVideo(meta, outgoing);
        return buildFileCard(meta, outgoing);
    }

    private Node buildInlineImage(ServiceMessage.AttachmentMeta meta, boolean outgoing) {
        try {
            byte[] data = messageService.readAttachmentBytes(meta.id());
            Image img = new Image(new ByteArrayInputStream(data));

            ImageView iv = new ImageView(img);
            iv.setPreserveRatio(true);
            iv.setFitWidth(420);
            iv.setSmooth(true);
            iv.setCache(true);

            StackPane media = new StackPane(iv);
            media.getStyleClass().addAll("media-bubble", outgoing ? "media-out" : "media-in");

            // Clip rounded corners (clean outline)
            Rectangle clip = new Rectangle();
            clip.setArcWidth(18);
            clip.setArcHeight(18);
            clip.widthProperty().bind(media.widthProperty());
            clip.heightProperty().bind(media.heightProperty());
            media.setClip(clip);

            media.setOnMouseClicked(e -> openAttachment(meta));
            return media;

        } catch (Exception e) {
            e.printStackTrace();
            return buildFileCard(meta, outgoing);
        }
    }


    private Node buildFileCard(ServiceMessage.AttachmentMeta meta, boolean outgoing) {
        Label name = new Label(meta.fileName());
        name.getStyleClass().add("file-name");

        Label info = new Label(humanSize(meta.sizeBytes()) + " • " + meta.mimeType());
        info.getStyleClass().add("file-meta");

        FontIcon icon = new FontIcon("mdi2f-file-outline");
        icon.getStyleClass().add("file-icon");

        VBox texts = new VBox(2, name, info);
        HBox card = new HBox(12, icon, texts);
        card.getStyleClass().addAll("file-card", outgoing ? "file-card-out" : "file-card-in");

        card.setOnMouseClicked(e -> openAttachment(meta));
        card.setCursor(javafx.scene.Cursor.HAND);

        return card;
    }


    private void openAttachment(ServiceMessage.AttachmentMeta meta) {
        try {
            byte[] data = messageService.readAttachmentBytes(meta.id());

            String safe = meta.fileName().replaceAll("[\\\\/:*?\"<>|]", "_");
            Path tmp = Files.createTempFile("yedik_", "_" + safe);
            Files.write(tmp, data);

            Desktop.getDesktop().open(tmp.toFile());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private String humanSize(long bytes) {
        double b = bytes;
        String[] u = {"B","KB","MB","GB"};
        int i = 0;
        while (b >= 1024 && i < u.length-1) { b /= 1024; i++; }
        return String.format(java.util.Locale.US, "%.1f %s", b, u[i]);
    }

    private static final java.net.http.HttpClient HTTP = java.net.http.HttpClient.newHttpClient();
    private final java.util.Map<String, YoutubeMeta> ytCache = new java.util.concurrent.ConcurrentHashMap<>();

    private record YoutubeMeta(String title, String thumbUrl, String site) {}

    private String extractYoutubeId(String url) {
        try {
            java.net.URI u = new java.net.URI(url);
            String host = u.getHost() == null ? "" : u.getHost().toLowerCase();
            String path = u.getPath() == null ? "" : u.getPath();

            if (host.contains("youtu.be")) {
                String id = path.startsWith("/") ? path.substring(1) : path;
                int cut = id.indexOf('?');
                return cut > 0 ? id.substring(0, cut) : id;
            }

            if (host.contains("youtube.com")) {
                String q = u.getRawQuery();
                if (q == null) return null;
                for (String part : q.split("&")) {
                    if (part.startsWith("v=")) return part.substring(2);
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private void fetchYoutubeMetaAsync(String url, java.util.function.Consumer<YoutubeMeta> onDone) {
        if (ytCache.containsKey(url)) {
            onDone.accept(ytCache.get(url));
            return;
        }

        String vid = extractYoutubeId(url);
        String thumb = (vid == null) ? null : ("https://img.youtube.com/vi/" + vid + "/hqdefault.jpg");

        try {
            String oembed = "https://www.youtube.com/oembed?url=" +
                    java.net.URLEncoder.encode(url, java.nio.charset.StandardCharsets.UTF_8) +
                    "&format=json";

            var req = java.net.http.HttpRequest.newBuilder(new java.net.URI(oembed)).GET().build();

            HTTP.sendAsync(req, java.net.http.HttpResponse.BodyHandlers.ofString())
                    .thenApply(r -> r.body())
                    .thenApply(json -> {
                        // minimal JSON parse without extra libs:
                        String title = extractJsonString(json, "title");
                        return new YoutubeMeta(title != null ? title : "YouTube", thumb, "youtube.com");
                    })
                    .exceptionally(ex -> new YoutubeMeta("YouTube", thumb, "youtube.com"))
                    .thenAccept(meta -> {
                        ytCache.put(url, meta);
                        javafx.application.Platform.runLater(() -> onDone.accept(meta));
                    });

        } catch (Exception e) {
            onDone.accept(new YoutubeMeta("YouTube", thumb, "youtube.com"));
        }
    }

    private String extractJsonString(String json, String key) {
        // naive but works for oEmbed response
        String needle = "\"" + key + "\":";
        int i = json.indexOf(needle);
        if (i < 0) return null;
        int q1 = json.indexOf('"', i + needle.length());
        if (q1 < 0) return null;
        int q2 = json.indexOf('"', q1 + 1);
        if (q2 < 0) return null;
        return json.substring(q1 + 1, q2);
    }

    private Node buildYoutubePreviewCard(String url, boolean outgoing) {
        // skeleton card (instant)
        var title = new Label("Loading…");
        title.getStyleClass().add("lp-title");

        var domain = new Label("youtube.com");
        domain.getStyleClass().add("lp-domain");

        var thumb = new javafx.scene.image.ImageView();
        thumb.setFitWidth(320);
        thumb.setFitHeight(180);
        thumb.setPreserveRatio(false);
        thumb.getStyleClass().add("lp-thumb");

        var play = new Label("▶");
        play.getStyleClass().add("lp-play");

        var thumbWrap = new StackPane(thumb, play);
        thumbWrap.getStyleClass().add("lp-thumb-wrap");

        var body = new VBox(6, title, domain);
        body.getStyleClass().add("lp-body");

        var card = new VBox(thumbWrap, body);
        card.getStyleClass().addAll("link-card", outgoing ? "link-card-out" : "link-card-in");
        card.setOnMouseClicked(e -> openInBrowser(url));
        card.setCursor(javafx.scene.Cursor.HAND);

        // async fill
        fetchYoutubeMetaAsync(url, meta -> {
            title.setText(meta.title());
            domain.setText(meta.site());
            if (meta.thumbUrl() != null) {
                thumb.setImage(new javafx.scene.image.Image(meta.thumbUrl(), true));
            }
        });

        return card;
    }

    private Node buildInlineVideo(ServiceMessage.AttachmentMeta meta, boolean outgoing) {

        initVlcOnce();

        Label overlay = new Label("▶");
        overlay.getStyleClass().add("vid-play");

        Label loading = new Label("Chargement…");
        loading.getStyleClass().add("vid-loading");

        StackPane player = new StackPane(loading, overlay);
        player.getStyleClass().addAll("media-bubble", outgoing ? "media-out" : "media-in");
        player.setPrefWidth(420);

        Rectangle clip = new Rectangle();
        clip.setArcWidth(18);
        clip.setArcHeight(18);
        clip.widthProperty().bind(player.widthProperty());
        clip.heightProperty().bind(player.heightProperty());
        player.setClip(clip);

        AtomicReference<EmbeddedMediaPlayerComponent> compRef = new AtomicReference<>();
        AtomicReference<Path> tmpRef = new AtomicReference<>();
        AtomicBoolean disposed = new AtomicBoolean(false);

        CompletableFuture
                .supplyAsync(() -> {
                    try {
                        byte[] data = messageService.readAttachmentBytes(meta.id());
                        String safe = meta.fileName().replaceAll("[\\\\/:*?\"<>|]", "_");
                        Path tmp = Files.createTempFile("yedik_vlc_", "_" + safe);
                        Files.write(tmp, data);
                        return tmp;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .thenAccept(tmp -> {
                    tmpRef.set(tmp);

                    SwingUtilities.invokeLater(() -> {

                        EmbeddedMediaPlayerComponent comp = new EmbeddedMediaPlayerComponent();
                        compRef.set(comp);

                        SwingNode swingNode = new SwingNode();
                        swingNode.setContent(comp);

                        Platform.runLater(() -> {
                            player.getChildren().setAll(swingNode, overlay);

                            player.setOnMouseClicked(e -> {
                                if (disposed.get()) return;

                                EmbeddedMediaPlayerComponent c = compRef.get();
                                if (c == null) return;

                                vlcEdt(() -> {
                                    try {
                                        boolean isPlaying = c.mediaPlayer().status().isPlaying();

                                        if (isPlaying) {
                                            c.mediaPlayer().controls().pause();
                                            Platform.runLater(() -> overlay.setVisible(true));
                                        } else {
                                            c.mediaPlayer().media().play(tmp.toAbsolutePath().toString());
                                            Platform.runLater(() -> overlay.setVisible(false));
                                        }
                                    } catch (Throwable t) {
                                        t.printStackTrace();
                                    }
                                });
                            });
                        });
                    });
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        ex.printStackTrace();
                        player.getChildren().setAll(buildFileCard(meta, outgoing));
                    });
                    return null;
                });

        // SAFE cleanup
        player.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (oldScene != null && newScene == null) {
                disposed.set(true);

                EmbeddedMediaPlayerComponent comp = compRef.getAndSet(null);
                Path tmp = tmpRef.getAndSet(null);

                if (comp != null) {
                    vlcEdt(() -> {
                        try { comp.mediaPlayer().controls().stop(); } catch (Throwable ignored) {}
                        try { comp.release(); } catch (Throwable ignored) {}
                    });
                }

                if (tmp != null) {
                    try { Files.deleteIfExists(tmp); } catch (Exception ignored) {}
                }
            }
        });

        return player;
    }


}
