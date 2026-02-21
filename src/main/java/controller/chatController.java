package controller;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import model.Conversation;
import model.Message;
import services.EmojiRepo;
import services.ServiceConversation;
import services.ServiceMessage;
import org.kordamp.ikonli.javafx.FontIcon;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.net.http.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import javafx.animation.FadeTransition;
import javafx.scene.control.TextField;
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
import java.io.*;
import model.ParticipantView;
import java.util.*;
import java.sql.SQLException;
import java.nio.file.Files;
import java.awt.Desktop;
import java.net.URI;
import java.nio.file.Path;
import javax.sound.sampled.*;
import javax.swing.*;


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
    @FXML private Button addPeopleBtn;
    @FXML private Label dangerActionLabel;
    @FXML private FontIcon dangerActionIcon;
    @FXML private VBox customizeSection;
    @FXML private FontIcon sendIcon;
    @FXML private Button micBtn;
    @FXML private FontIcon micIcon;
    @FXML private Button emojiBtn;
    @FXML private Button recCancelBtn;
    @FXML private HBox recIndicatorBox;
    @FXML private Label recTimerLabel;
    @FXML private HBox recPreviewBox;
    @FXML private Button recPreviewPlayBtn;
    @FXML private FontIcon recPreviewPlayIcon;
    @FXML private Label recPreviewTimeLabel;
    @FXML private ToggleButton filterAllBtn;
    @FXML private ToggleButton filterPrivateBtn;
    @FXML private ToggleButton filterGroupsBtn;
    @FXML private TextField searchField;
    @FXML private VBox mediaPanel;
    @FXML private TabPane mediaTabs;
    @FXML private Tab tabMedia, tabFiles, tabLinks;
    @FXML private TilePane mediaGrid;
    @FXML private VBox filesBox;
    @FXML private VBox linksBox;
    @FXML private Button mediaBackBtn;
    @FXML private StackPane mediaOverlay;
    @FXML private StackPane aiSummaryOverlay;
    @FXML private Label aiSummaryTitle;
    @FXML private TextArea aiSummaryText;
    @FXML private ProgressIndicator aiSummaryLoading;

    @FXML private void toggleChatInfo()   { toggleSection(secChatInfo,   chevChatInfo); }
    @FXML private void toggleCustomize()  { toggleSection(secCustomize,  chevCustomize); }
    @FXML private void toggleMembers()    { toggleSection(secMembers,    chevMembers); }
    @FXML private void toggleMedia()      { toggleSection(secMedia,      chevMedia); }
    @FXML private void openMediaPanelMedia() throws SQLException { openMediaPanel(0); }
    @FXML private void openMediaPanelFiles() throws SQLException { openMediaPanel(1); }
    @FXML private void openMediaPanelLinks() throws SQLException { openMediaPanel(2); }

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
    private static final java.util.concurrent.atomic.AtomicBoolean VLC_INIT = new java.util.concurrent.atomic.AtomicBoolean(false);
    private static uk.co.caprica.vlcj.factory.MediaPlayerFactory VLC_FACTORY;
    private javafx.stage.Stage inlineVideoStage;
    private uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent inlineComp;
    private javafx.animation.AnimationTimer inlineTracker;
    private java.nio.file.Path inlineCurrentFile;
    private javafx.scene.Node inlineAnchor;
    private javax.swing.JWindow inlineSwingWindow;
    private uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent inlineSwingComp;
    private javafx.event.EventHandler<javafx.scene.input.KeyEvent> inlineEscFilter;
    private javafx.scene.Scene inlineEscScene;
    private javafx.beans.value.ChangeListener<Boolean> inlineOwnerFocusListener;
    private javafx.stage.Window inlineOwnerWindow;
    private javax.swing.Timer inlineSwingTimer;
    private double lastX = Double.NaN, lastY = Double.NaN, lastW = Double.NaN, lastH = Double.NaN;
    private javafx.event.EventHandler<javafx.scene.input.MouseEvent> inlineClickAwayFilter;
    private javafx.scene.Scene inlineClickAwayScene;
    private javafx.scene.media.MediaPlayer currentAudioPlayer;
    private long currentAudioAttachmentId = -1;
    private volatile boolean recording = false;
    private javax.sound.sampled.TargetDataLine recordLine;
    private Thread recordThread;
    private java.io.File lastRecordedFile;
    private enum MicState {
        NORMAL,
        RECORDING,
        PREVIEW
    }
    private MicState micState = MicState.NORMAL;
    private javafx.animation.Timeline recTimeline;
    private int recSeconds = 0;
    private javafx.scene.media.MediaPlayer previewPlayer;
    private final ToggleGroup filterToggleGroup = new ToggleGroup();
    private enum FilterMode { ALL, DM, GROUP }
    private FilterMode currentFilter = FilterMode.ALL;
    private List<Conversation> allConversations = new ArrayList<>();
    private boolean restoreDrawerAfterMedia = false;
    private static final String LM_BASE = "http://localhost:1234/v1";
    private final Set<Long> aiSummarySuppressed = new HashSet<>();
    private final Map<Long, Integer> aiSummarySeenAtUnread = new HashMap<>(); // convId -> unread count when dismissed
    private long aiPendingConvId = -1;
    private int aiPendingUnreadCount = 0;
    private String aiPendingTitle = "";
    private List<Message> aiPendingMessages = List.of();
    private final HttpClient http = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build();
    // LM Studio / OpenAI-compatible endpoint (recommended)
    private static final String LM_CHAT_URL = "http://localhost:1234/v1/chat/completions";
    private static final String LM_MODEL = "dolphin3.0-llama3.1-8b";
    private EmojiRepo emojiRepo;
    private javafx.stage.Popup emojiPopup;
    private TabPane emojiTabs;
    private TextField emojiSearch;
    private boolean emojiBuilt = false;
    private javafx.scene.layout.TilePane emojiSearchGrid;
    private javafx.scene.control.ScrollPane emojiSearchScroll;




    private int currentUserId = 2;

    //==========================
    //HELPER METHODS
    //==========================
    private static <T> List<T> lastN(List<T> list, int n) {
        if (list == null || list.isEmpty() || n <= 0) return List.of();
        int size = list.size();
        int from = Math.max(0, size - n);
        return new ArrayList<>(list.subList(from, size));
    }

    private static void initVlcOnce() {
        if (!VLC_INIT.compareAndSet(false, true)) return;

        java.nio.file.Path vlcDir = java.nio.file.Path.of(System.getProperty("user.dir"), "vlc").toAbsolutePath();

        // Required for bundled VLC
        System.setProperty("VLC_PLUGIN_PATH", vlcDir.resolve("plugins").toString());

        // Make sure JNA can find libvlc from your bundled folder
        String vlcPath = vlcDir.toString();
        System.setProperty("jna.library.path", vlcPath);
        try {
            com.sun.jna.NativeLibrary.addSearchPath("libvlc", vlcPath);
            com.sun.jna.NativeLibrary.addSearchPath("libvlccore", vlcPath);
        } catch (Throwable ignored) {
            // not fatal; some setups still work with jna.library.path alone
        }

        boolean ok = new uk.co.caprica.vlcj.factory.discovery.NativeDiscovery().discover();
        System.out.println("VLC discover ok=" + ok + " dir=" + vlcDir);

        VLC_FACTORY = new uk.co.caprica.vlcj.factory.MediaPlayerFactory(
                "--no-video-title-show",
                "--no-video-on-top",
                "--quiet"
        );
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
        emojiRepo = EmojiRepo.load();
        emojiBtn.setOnAction(e -> toggleEmojiPopup());
        initVlcOnce();
        newMsgPrimaryBtn.getStyleClass().add("overlay-btn-primary");
        newMsgBackBtn.getStyleClass().add("overlay-btn");
        newMsgCancelBtn.getStyleClass().add("overlay-btn");
        selectedConversationProperty.set(null);
        BooleanBinding hasSelection = selectedConversationProperty.isNotNull();

        chatHeader.visibleProperty().bind(hasSelection);
        chatHeader.managedProperty().bind(chatHeader.visibleProperty());

        composerBar.visibleProperty().bind(hasSelection);
        composerBar.managedProperty().bind(composerBar.visibleProperty());

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
        membersList.setFixedCellSize(48);

        membersList.prefHeightProperty().bind(
                membersList.fixedCellSizeProperty()
                        .multiply(Bindings.size(membersList.getItems()))
                        .add(2)
        );

        membersList.setFocusTraversable(false);

        nicknamesList.setItems(nicknamesData);
        nicknamesList.setCellFactory(lv -> new NicknamesRowCell());
        nicknamesList.setFocusTraversable(false);
        nicknamesList.setPlaceholder(new Label("No participants"));

        nicknamesList.setFixedCellSize(56);

        int maxRows = 6;

        nicknamesList.prefHeightProperty().bind(
                nicknamesList.fixedCellSizeProperty()
                        .multiply(Bindings.min(maxRows, Bindings.size(nicknamesData)))
                        .add(2)
        );

        nicknamesList.maxHeightProperty().bind(nicknamesList.prefHeightProperty());

        Platform.runLater(() -> drawer.setTranslateX(drawer.getWidth()));
        chatNameField.textProperty().addListener((obs, oldV, newV) -> updateSaveState());


        usersPickList.setItems(filteredUsers);
        usersPickList.setCellFactory(lv -> new UserPickCell()); // class below

        usersPickList.setFixedCellSize(64);
        usersPickList.prefHeightProperty().bind(
                usersPickList.fixedCellSizeProperty()
                        .multiply(Bindings.min(maxRows, Bindings.size(filteredUsers)))
                        .add(2)
        );
        usersPickList.maxHeightProperty().bind(usersPickList.prefHeightProperty());

        filterAllBtn.setToggleGroup(filterToggleGroup);
        filterPrivateBtn.setToggleGroup(filterToggleGroup);
        filterGroupsBtn.setToggleGroup(filterToggleGroup);

        filterAllBtn.setUserData(FilterMode.ALL);
        filterPrivateBtn.setUserData(FilterMode.DM);
        filterGroupsBtn.setUserData(FilterMode.GROUP);

        filterToggleGroup.selectedToggleProperty().addListener((obs, oldT, newT) -> {
            if (newT == null) {
                oldT.setSelected(true);
                return;
            }
            currentFilter = (FilterMode) newT.getUserData();
            loadConversations();
        });
        searchField.textProperty().addListener((obs, oldV, newV) -> {
            loadConversations();
        });

        userSearchField.textProperty().addListener((obs, o, q) -> applyUserFilter(q));
        if (micBtn != null) micBtn.setOnAction(e -> handleMicToggle());
        if (recCancelBtn != null) recCancelBtn.setOnAction(e -> handleCancelRecording());
        if (recPreviewPlayBtn != null) recPreviewPlayBtn.setOnAction(e -> handlePreviewPlayPause());

        setComposerModeNormalUI();
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
        if (inlineSwingWindow != null) {
            closeInlineOverlayVideoSwing();
        }
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

            // 🔹 store full list
            allConversations = conversationService.listForUser(currentUserId);

            for (Conversation conv : allConversations) {

                if (!matchesFilter(conv)) continue;

                VBox item = buildConversationItem(conv);

                conversationNodeById.put(conv.getId(), item);
                conversationContainer.getChildren().add(item);
            }

            // re-apply selection only if still visible
            if (selectedConversationId > 0 && conversationNodeById.containsKey(selectedConversationId)) {
                setSelectedConversationStyle(selectedConversationId);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private boolean matchesFilter(Conversation conv) {

        // 🔹 1) Filter by type
        String type = conv.getType();
        boolean typeMatch = switch (currentFilter) {
            case ALL -> true;
            case DM -> "DM".equalsIgnoreCase(type);
            case GROUP -> "GROUP".equalsIgnoreCase(type);
        };

        if (!typeMatch) return false;

        // 🔹 2) Filter by search
        String search = searchField.getText();

        if (search == null || search.isBlank()) return true;

        String title = conv.getTitle();
        if (title == null) return false;

        return title.toLowerCase().contains(search.toLowerCase());
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
            if (messages.isEmpty()) {
                return;
            }

            boolean isGroup = selectedConversation != null
                    && "GROUP".equalsIgnoreCase(selectedConversation.getType());

            long otherLastRead = messageService.getMaxReadByOthers(conversationId, currentUserId);

            // ===== UNREAD BEFORE we mark read (DB) =====
            int unreadBefore = 0;
            try {
                unreadBefore = messageService.countUnread(conversationId, currentUserId);
            } catch (SQLException ignored) {}

            // ===== AI gating: show when unread >= 10 and not dismissed for this unread count =====
            int seenAt = aiSummarySeenAtUnread.getOrDefault(conversationId, 0);
            boolean showAi = unreadBefore >= 10 && unreadBefore > seenAt;

            // cache pending context for popup / button
            aiPendingConvId = conversationId;
            aiPendingUnreadCount = unreadBefore;
            aiPendingTitle = (selectedConversation == null || selectedConversation.getTitle() == null || selectedConversation.getTitle().isBlank())
                    ? "Conversation"
                    : selectedConversation.getTitle();
            aiPendingMessages = new ArrayList<>(messages);

            // ===== Render messages normally =====
            for (Message msg : messages) {
                boolean outgoing = msg.getSenderId() == currentUserId;

                HBox row = new HBox();
                row.setAlignment(outgoing ? Pos.TOP_RIGHT : Pos.TOP_LEFT);

                VBox bubbleBox = new VBox(6);
                bubbleBox.setAlignment(outgoing ? Pos.TOP_RIGHT : Pos.TOP_LEFT);

                if (isGroup && !outgoing) {
                    Label sender = new Label(senderName(msg.getSenderId()));
                    sender.getStyleClass().add("sender-name");
                    bubbleBox.getChildren().add(sender);
                }

                Node bubbleNode = buildMessageNode(msg, outgoing);
                bubbleBox.getChildren().add(bubbleNode);

                if (outgoing) {
                    attachBubbleMenu(bubbleNode, msg);
                    showBubbleMenu(bubbleNode, msg);
                }

                HBox footer = new HBox(6);
                footer.setAlignment(outgoing ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

                if (msg.getEditedAt() != null) {
                    Label edited = new Label("édité");
                    edited.getStyleClass().add(outgoing ? "edited-out" : "edited-in");
                    footer.getChildren().add(edited);
                }

                if (isGroup && outgoing) {
                    HBox seenRow = buildSeenRow(conversationId, msg.getId(), currentUserId);
                    footer.getChildren().add(seenRow);
                }

                Label time = new Label(formatBubbleTime(msg.getCreatedAt()));
                time.getStyleClass().add(outgoing ? "timestamp-out" : "timestamp");
                footer.getChildren().add(time);

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

            // ===== Add ONE AI chip at the VERY BOTTOM (only if showAi) =====
            if (showAi) {
                final int unreadSnap = unreadBefore;
                final long convIdSnap = conversationId;
                final String titleSnap = aiPendingTitle;
                final List<Message> snap = new ArrayList<>(messages);

                addAiSummaryChip(() -> {
                    // dismiss for this unread burst; it will reappear only if unread increases later
                    aiSummarySeenAtUnread.put(convIdSnap, unreadSnap);

                    List<Message> toSummarize = lastN(snap, 10);

                    openAiSummaryFrom(titleSnap, toSummarize);
                });
            }

            // ===== Mark as read once (after rendering) =====
            long lastId = messages.get(messages.size() - 1).getId();
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

        if (micState == MicState.PREVIEW && editingMessage == null
                && lastRecordedFile != null && lastRecordedFile.exists()) {
            try {
                sendAttachment(lastRecordedFile);
                aiSummarySuppressed.add(selectedConversation.getId());
                cleanupPreviewPlayer();
                lastRecordedFile = null;
                setComposerModeNormalUI();

                loadMessages(selectedConversation.getId());
                loadConversations();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return;
        }

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
                aiSummarySuppressed.add(editingMessage.getConversationId());
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
            aiSummarySuppressed.add(selectedConversation.getId());
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
        updateAiSummaryAvailability();


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
        if (mediaPanel.isVisible()) {
            refreshMediaPanel(conv.getId());
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
        if (mime == null) {
            String n = f.getName().toLowerCase();
            if (n.endsWith(".wav")) mime = "audio/wav";
            else if (n.endsWith(".mp3")) mime = "audio/mpeg";
            else if (n.endsWith(".m4a")) mime = "audio/mp4";
            else if (n.endsWith(".aac")) mime = "audio/aac";
            else if (n.endsWith(".ogg")) mime = "audio/ogg";
            else mime = "application/octet-stream";
        }

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
            if (m.start() > last) {
                javafx.scene.text.Text t = new javafx.scene.text.Text(text.substring(last, m.start()));
                t.setFill(outgoing ? javafx.scene.paint.Color.WHITE : javafx.scene.paint.Color.BLACK);
                flow.getChildren().add(t);
            }

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

        if (last < text.length()) {
            javafx.scene.text.Text t = new javafx.scene.text.Text(text.substring(last));
            t.setFill(outgoing ? javafx.scene.paint.Color.WHITE : javafx.scene.paint.Color.BLACK);
            flow.getChildren().add(t);
        }

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
        if (mime.startsWith("audio/")) return buildInlineAudio(meta, outgoing);
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

            media.setOnMouseClicked(e -> {
                if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                    openAttachment(meta);
                }
            });
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

        card.setOnMouseClicked(e -> {
            if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                openAttachment(meta);
            }
        });
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
        card.setOnMouseClicked(e -> {
            if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                openInBrowser(url);
            }
        });
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

    private void openVideoInSwingWindow(java.nio.file.Path mediaFile, String title) {
        if (mediaFile == null) return;
        initVlcOnce();

        javax.swing.SwingUtilities.invokeLater(() -> {
            uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent comp =
                    new uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent();

            javax.swing.JFrame frame = new javax.swing.JFrame(title == null ? "Video" : title);
            frame.setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
            frame.setContentPane(comp);
            frame.setSize(900, 520);
            frame.setLocationRelativeTo(null);

            frame.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override public void windowClosing(java.awt.event.WindowEvent e) {
                    try { comp.mediaPlayer().controls().stop(); } catch (Throwable ignored) {}
                    try { comp.release(); } catch (Throwable ignored) {}
                }
                @Override public void windowClosed(java.awt.event.WindowEvent e) {
                    try { comp.release(); } catch (Throwable ignored) {}
                }
            });

            frame.setVisible(true);

            // Play AFTER visible so native handle exists
            comp.mediaPlayer().media().play(mediaFile.toString());
        });
    }

    private void openInlineOverlayVideoSwing(java.nio.file.Path file, javafx.scene.Node anchor, String title) {
        if (file == null || anchor == null) return;

        initVlcOnce();

        javafx.geometry.Bounds b = anchor.localToScreen(anchor.getBoundsInLocal());
        if (b == null) return;

        // Same bubble: visible -> pause+hide, hidden -> show+play
        if (inlineSwingWindow != null && inlineAnchor == anchor) {
            if (inlineSwingWindow.isVisible()) {
                suspendInlineOverlayVideoSwing(true);
            } else {
                suspendInlineOverlayVideoSwing(false);
            }
            return;
        }

        // Different bubble: fully close then open new
        if (inlineSwingWindow != null) {
            closeInlineOverlayVideoSwing();
        }

        inlineCurrentFile = file;
        inlineAnchor = anchor;

        // Attach ESC close (do NOT overwrite scene handlers)
        javafx.scene.Scene sc = anchor.getScene();
        if (sc != null) {
            detachInlineEsc();
            inlineEscScene = sc;
            inlineEscFilter = ev -> {
                if (ev.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                    closeInlineOverlayVideoSwing();
                    ev.consume();
                }
            };
            sc.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, inlineEscFilter);
        }

        // Click-away pause+hide
        attachInlineClickAway(anchor);

        // Close when owner window loses focus
        javafx.stage.Window w = sc != null ? sc.getWindow() : null;
        if (w != null) {
            detachInlineFocus();
            inlineOwnerWindow = w;
            inlineOwnerFocusListener = (o, old, focused) -> {
                if (!focused) closeInlineOverlayVideoSwing();
            };
            w.focusedProperty().addListener(inlineOwnerFocusListener);
        }

        // Create window on Swing EDT
        javax.swing.SwingUtilities.invokeLater(() -> {
            inlineSwingComp = new uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent();

            inlineSwingWindow = new javax.swing.JWindow();
            inlineSwingWindow.setAlwaysOnTop(true);
            inlineSwingWindow.setBackground(new java.awt.Color(0, 0, 0, 255));

            // Root layers
            javax.swing.JLayeredPane layers = new javax.swing.JLayeredPane();
            layers.setLayout(null);
            inlineSwingWindow.setContentPane(layers);

            // Video fills window
            java.awt.Component video = inlineSwingComp;
            layers.add(video, javax.swing.JLayeredPane.DEFAULT_LAYER);

            // Top bar (play/pause)
            javax.swing.JPanel topBar = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 8, 8));
            topBar.setOpaque(false);

            javax.swing.JButton btnPlayPause = new javax.swing.JButton("⏸");
            btnPlayPause.setFocusable(false);
            btnPlayPause.setBorderPainted(false);
            btnPlayPause.setContentAreaFilled(false);
            btnPlayPause.setForeground(java.awt.Color.WHITE);
            btnPlayPause.setFont(btnPlayPause.getFont().deriveFont(18f));
            topBar.add(btnPlayPause);

            // Bottom bar (seek)
            javax.swing.JPanel bottomBar = new javax.swing.JPanel(new java.awt.BorderLayout(8, 0));
            bottomBar.setOpaque(false);

            javax.swing.JSlider seek = new javax.swing.JSlider(0, 1000, 0);
            seek.setOpaque(false);
            seek.setFocusable(false);
            bottomBar.add(seek, java.awt.BorderLayout.CENTER);

            layers.add(topBar, javax.swing.JLayeredPane.PALETTE_LAYER);
            layers.add(bottomBar, javax.swing.JLayeredPane.PALETTE_LAYER);

            // Always show bars while visible (no hover logic)
            topBar.setVisible(true);
            bottomBar.setVisible(true);

            // Initial bounds from JavaFX anchor
            int x = (int) Math.round(b.getMinX());
            int y = (int) Math.round(b.getMinY());
            int ww = (int) Math.max(50, Math.round(b.getWidth()));
            int hh = (int) Math.max(50, Math.round(b.getHeight()));
            inlineSwingWindow.setBounds(x, y, ww, hh);

            java.util.function.Consumer<java.awt.Dimension> relayout = (dim) -> {
                int W = dim.width, H = dim.height;
                video.setBounds(0, 0, W, H);
                topBar.setBounds(0, 0, W, 44);
                bottomBar.setBounds(10, H - 34, W - 20, 24);
            };
            relayout.accept(inlineSwingWindow.getSize());

            // Show window then play
            inlineSwingWindow.setVisible(true);
            inlineSwingComp.mediaPlayer().media().play(file.toAbsolutePath().toString());

            // Play/pause button
            btnPlayPause.addActionListener(ev -> {
                if (inlineSwingComp == null) return;
                boolean playing = inlineSwingComp.mediaPlayer().status().isPlaying();
                if (playing) {
                    inlineSwingComp.mediaPlayer().controls().pause();
                    btnPlayPause.setText("▶");
                } else {
                    inlineSwingComp.mediaPlayer().controls().play();
                    btnPlayPause.setText("⏸");
                }
            });

            // Seek logic
            java.util.concurrent.atomic.AtomicBoolean dragging = new java.util.concurrent.atomic.AtomicBoolean(false);

            seek.addChangeListener(e -> {
                if (inlineSwingComp == null) return;

                if (seek.getValueIsAdjusting()) {
                    dragging.set(true);
                } else if (dragging.get()) {
                    float pos = seek.getValue() / 1000f;
                    inlineSwingComp.mediaPlayer().controls().setPosition(pos);
                    dragging.set(false);
                }
            });

            // Timer update (seek + icon)
            if (inlineSwingTimer != null) {
                inlineSwingTimer.stop();
                inlineSwingTimer = null;
            }
            inlineSwingTimer = new javax.swing.Timer(200, e -> {
                if (inlineSwingComp == null) return;
                if (dragging.get()) return;

                float pos = inlineSwingComp.mediaPlayer().status().position(); // 0..1
                seek.setValue((int) Math.round(pos * 1000));

                boolean playing = inlineSwingComp.mediaPlayer().status().isPlaying();
                btnPlayPause.setText(playing ? "⏸" : "▶");
            });
            inlineSwingTimer.start();

            // Relayout on resize
            inlineSwingWindow.addComponentListener(new java.awt.event.ComponentAdapter() {
                @Override public void componentResized(java.awt.event.ComponentEvent e) {
                    relayout.accept(inlineSwingWindow.getSize());
                }
            });
        });

        // Track anchor while scrolling/resizing (JavaFX thread)
        if (inlineTracker != null) inlineTracker.stop();
        inlineTracker = new javafx.animation.AnimationTimer() {
            @Override public void handle(long now) {
                javafx.scene.Node a = inlineAnchor;
                if (a == null) return;

                javafx.geometry.Bounds bb = a.localToScreen(a.getBoundsInLocal());
                if (bb == null) return;

                double nx = bb.getMinX();
                double ny = bb.getMinY();
                double nw = Math.max(50, bb.getWidth());
                double nh = Math.max(50, bb.getHeight());

                // Skip tiny changes
                if (!Double.isNaN(lastX)) {
                    if (Math.abs(nx - lastX) < 1 && Math.abs(ny - lastY) < 1 &&
                            Math.abs(nw - lastW) < 1 && Math.abs(nh - lastH) < 1) {
                        return;
                    }
                }
                lastX = nx; lastY = ny; lastW = nw; lastH = nh;

                javax.swing.SwingUtilities.invokeLater(() -> {
                    if (inlineSwingWindow == null) return;
                    inlineSwingWindow.setBounds(
                            (int) Math.round(nx),
                            (int) Math.round(ny),
                            (int) Math.round(nw),
                            (int) Math.round(nh)
                    );
                });
            }
        };
        inlineTracker.start();
    }


    private void closeInlineOverlayVideoSwing() {
        if (inlineTracker != null) { inlineTracker.stop(); inlineTracker = null; }

        lastX = lastY = lastW = lastH = Double.NaN;

        detachInlineEsc();
        detachInlineFocus();
        detachInlineClickAway();

        inlineAnchor = null;
        inlineCurrentFile = null;

        javax.swing.SwingUtilities.invokeLater(() -> {
            if (inlineSwingTimer != null) {
                inlineSwingTimer.stop();
                inlineSwingTimer = null;
            }

            try {
                if (inlineSwingComp != null) {
                    try { inlineSwingComp.mediaPlayer().controls().stop(); } catch (Throwable ignored) {}
                    try { inlineSwingComp.release(); } catch (Throwable ignored) {}
                }
            } finally {
                inlineSwingComp = null;

                if (inlineSwingWindow != null) {
                    inlineSwingWindow.setVisible(false);
                    inlineSwingWindow.dispose();
                    inlineSwingWindow = null;
                }
            }
        });
    }

    private static boolean isDescendant(javafx.scene.Node node, javafx.scene.Node ancestor) {
        for (javafx.scene.Node n = node; n != null; n = n.getParent()) {
            if (n == ancestor) return true;
        }
        return false;
    }

    private void detachInlineEsc() {
        if (inlineEscScene != null && inlineEscFilter != null) {
            inlineEscScene.removeEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, inlineEscFilter);
        }
        inlineEscScene = null;
        inlineEscFilter = null;
    }

    private void detachInlineFocus() {
        if (inlineOwnerWindow != null && inlineOwnerFocusListener != null) {
            inlineOwnerWindow.focusedProperty().removeListener(inlineOwnerFocusListener);
        }
        inlineOwnerWindow = null;
        inlineOwnerFocusListener = null;
    }

    private void detachInlineClickAway() {
        if (inlineClickAwayScene != null && inlineClickAwayFilter != null) {
            inlineClickAwayScene.removeEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, inlineClickAwayFilter);
        }
        inlineClickAwayScene = null;
        inlineClickAwayFilter = null;
    }

    private void attachInlineClickAway(javafx.scene.Node anchor) {
        if (anchor == null) return;
        javafx.scene.Scene sc = anchor.getScene();
        if (sc == null) return;

        detachInlineClickAway();

        inlineClickAwayScene = sc;
        inlineClickAwayFilter = ev -> {
            if (inlineAnchor == null) return;
            if (inlineSwingWindow == null) return;

            javafx.scene.Node target = (ev.getTarget() instanceof javafx.scene.Node) ? (javafx.scene.Node) ev.getTarget() : null;

            // If click is outside the anchor bubble -> pause + hide
            if (target != null && !isDescendant(target, inlineAnchor)) {
                if (inlineSwingWindow.isVisible()) {
                    suspendInlineOverlayVideoSwing(true);
                }
            }
        };

        sc.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, inlineClickAwayFilter);
    }


    private void suspendInlineOverlayVideoSwing(boolean suspend) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            if (inlineSwingWindow == null || inlineSwingComp == null) return;

            try {
                if (suspend) {
                    try { inlineSwingComp.mediaPlayer().controls().pause(); } catch (Throwable ignored) {}
                    inlineSwingWindow.setVisible(false);

                    if (inlineSwingTimer != null) inlineSwingTimer.stop();
                } else {
                    inlineSwingWindow.setVisible(true);

                    // Resume playback (or keep paused if you prefer)
                    try { inlineSwingComp.mediaPlayer().controls().play(); } catch (Throwable ignored) {}

                    if (inlineSwingTimer != null && !inlineSwingTimer.isRunning()) inlineSwingTimer.start();
                }
            } catch (Throwable ignored) {}
        });
    }

    private javafx.scene.Node buildInlineVideo(ServiceMessage.AttachmentMeta meta, boolean outgoing) {

        initVlcOnce(); // guarded init

        javafx.scene.layout.StackPane player = new javafx.scene.layout.StackPane();
        player.getStyleClass().add("media-player");

        // Big like image bubble (tweak)
        player.setPrefWidth(640);
        player.setPrefHeight(360);
        player.setMinHeight(220);

        // Rounded clip
        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle();
        clip.setArcWidth(18);
        clip.setArcHeight(18);
        clip.widthProperty().bind(player.widthProperty());
        clip.heightProperty().bind(player.heightProperty());
        player.setClip(clip);

        // Overlay play icon
        javafx.scene.control.Label playIcon = new javafx.scene.control.Label("▶");
        playIcon.getStyleClass().add("media-play-icon");

        javafx.scene.layout.StackPane overlay = new javafx.scene.layout.StackPane(playIcon);
        overlay.getStyleClass().add("media-overlay");
        overlay.setPickOnBounds(true);

        // Host content (black box for now)
        javafx.scene.layout.StackPane videoHost = new javafx.scene.layout.StackPane();
        videoHost.setMinSize(0, 0);
        videoHost.prefWidthProperty().bind(player.widthProperty());
        videoHost.prefHeightProperty().bind(player.heightProperty());

        player.getChildren().setAll(videoHost, overlay);

        java.util.concurrent.atomic.AtomicReference<java.nio.file.Path> tmpRef =
                new java.util.concurrent.atomic.AtomicReference<>();
        java.util.concurrent.atomic.AtomicBoolean disposed =
                new java.util.concurrent.atomic.AtomicBoolean(false);

        // Load attachment -> temp file async
        java.util.concurrent.CompletableFuture
                .supplyAsync(() -> {
                    try {
                        byte[] data = messageService.readAttachmentBytes(meta.id());
                        String safe = meta.fileName().replaceAll("[\\\\/:*?\"<>|]", "_");
                        java.nio.file.Path tmp = java.nio.file.Files.createTempFile("yedik_vlc_", "_" + safe);
                        java.nio.file.Files.write(tmp, data);
                        tmp.toFile().deleteOnExit();
                        return tmp;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .thenAccept(tmp -> javafx.application.Platform.runLater(() -> {
                    if (disposed.get()) return;
                    tmpRef.set(tmp);

                    javafx.event.EventHandler<javafx.scene.input.MouseEvent> open = ev -> {
                        if (disposed.get()) return;
                        java.nio.file.Path p = tmpRef.get();
                        if (p == null) return;

                        // Open Swing/VLC overlay aligned to this bubble
                        openInlineOverlayVideoSwing(p, player, meta.fileName());

                        // ✅ CRITICAL: give focus back to JavaFX so ESC + click-away filters work
                        javafx.scene.Scene sc = player.getScene();
                        if (sc != null) {
                            javafx.stage.Window win = sc.getWindow();
                            if (win != null) win.requestFocus();
                        }

                        ev.consume();
                    };

                    player.setOnMouseClicked(ev -> {
                        if (ev.getButton() == javafx.scene.input.MouseButton.PRIMARY) open.handle(ev);
                    });
                    overlay.setOnMouseClicked(ev -> {
                        if (ev.getButton() == javafx.scene.input.MouseButton.PRIMARY) open.handle(ev);
                    });
                }))
                .exceptionally(ex -> {
                    javafx.application.Platform.runLater(() -> {
                        ex.printStackTrace();
                        videoHost.getChildren().setAll(new javafx.scene.control.Label("Video error"));
                        overlay.setVisible(false);
                    });
                    return null;
                });

        // Cleanup when node removed (conversation switch etc.)
        player.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (oldScene != null && newScene == null) {
                disposed.set(true);
                player.setOnMouseClicked(null);
                overlay.setOnMouseClicked(null);

                // If overlay is currently attached to this bubble, close it
                if (inlineAnchor == player) {
                    closeInlineOverlayVideoSwing();
                }

                java.nio.file.Path tmp = tmpRef.getAndSet(null);
                if (tmp != null) {
                    try { java.nio.file.Files.deleteIfExists(tmp); } catch (Exception ignored) {}
                }
            }
        });

        return player;
    }

    private Node buildInlineAudio(ServiceMessage.AttachmentMeta meta, boolean outgoing) {
        // UI
        FontIcon playIco = new FontIcon("mdi2p-play");
        playIco.getStyleClass().add("icon");

        Button playBtn = new Button();
        playBtn.getStyleClass().add("audio-btn");
        playBtn.setGraphic(playIco);

        Slider slider = new Slider(0, 1, 0);
        slider.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(slider, Priority.ALWAYS);

        Label time = new Label("0:00");
        time.getStyleClass().add("audio-time");

        HBox row = new HBox(10, playBtn, slider, time);
        row.getStyleClass().addAll("audio-card", outgoing ? "audio-card-out" : "audio-card-in");

        // Playback logic (single active player globally)
        playBtn.setOnAction(e -> {
            try {
                toggleAudio(meta, playIco, slider, time);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        // seek
        slider.valueChangingProperty().addListener((obs, was, is) -> {
            if (!is && currentAudioPlayer != null && currentAudioAttachmentId == meta.id()) {
                javafx.util.Duration d = currentAudioPlayer.getTotalDuration();
                if (d != null && !d.isUnknown() && d.toMillis() > 0) {
                    currentAudioPlayer.seek(d.multiply(slider.getValue()));
                }
            }
        });

        return row;
    }

    private void toggleAudio(ServiceMessage.AttachmentMeta meta, FontIcon playIco, Slider slider, Label time) throws Exception {
        // if same audio is already playing -> pause/stop toggle
        if (currentAudioPlayer != null && currentAudioAttachmentId == meta.id()) {
            var st = currentAudioPlayer.getStatus();
            if (st == javafx.scene.media.MediaPlayer.Status.PLAYING) {
                currentAudioPlayer.pause();
                playIco.setIconLiteral("mdi2p-play");
            } else {
                currentAudioPlayer.play();
                playIco.setIconLiteral("mdi2p-pause");
            }
            return;
        }

        // stop previous audio
        stopCurrentAudio();

        // load bytes -> temp file -> MediaPlayer
        byte[] data = messageService.readAttachmentBytes(meta.id());
        if (data == null || data.length == 0) return;

        java.nio.file.Path tmp = writeTempMedia(meta.fileName(), data);
        javafx.scene.media.Media media = new javafx.scene.media.Media(tmp.toUri().toString());
        javafx.scene.media.MediaPlayer mp = new javafx.scene.media.MediaPlayer(media);

        currentAudioPlayer = mp;
        currentAudioAttachmentId = meta.id();

        mp.setOnReady(() -> {
            javafx.util.Duration total = mp.getTotalDuration();
            if (total != null && !total.isUnknown() && total.toMillis() > 0) {
                time.setText(formatTime(mp.getCurrentTime()) + " / " + formatTime(total));
            }
        });

        mp.currentTimeProperty().addListener((obs, old, cur) -> {
            if (mp != currentAudioPlayer) return;
            javafx.util.Duration total = mp.getTotalDuration();
            if (total != null && !total.isUnknown() && total.toMillis() > 0) {
                if (!slider.isValueChanging()) {
                    slider.setValue(cur.toMillis() / total.toMillis());
                }
                time.setText(formatTime(cur) + " / " + formatTime(total));
            } else {
                time.setText(formatTime(cur));
            }
        });

        mp.setOnEndOfMedia(() -> {
            playIco.setIconLiteral("mdi2p-play");
            slider.setValue(0);
            stopCurrentAudio();
        });

        playIco.setIconLiteral("mdi2p-pause");
        mp.play();
    }

    private void stopCurrentAudio() {
        if (currentAudioPlayer != null) {
            try { currentAudioPlayer.stop(); } catch (Exception ignored) {}
            try { currentAudioPlayer.dispose(); } catch (Exception ignored) {}
        }
        currentAudioPlayer = null;
        currentAudioAttachmentId = -1;
    }

    private java.nio.file.Path writeTempMedia(String fileName, byte[] data) throws Exception {
        String safe = (fileName == null ? "audio.bin" : fileName).replaceAll("[\\\\/:*?\"<>|]", "_");
        java.nio.file.Path tmp = java.nio.file.Files.createTempFile("yedik_media_", "_" + safe);
        java.nio.file.Files.write(tmp, data);
        tmp.toFile().deleteOnExit();
        return tmp;
    }

    private String formatTime(javafx.util.Duration d) {
        if (d == null) return "0:00";
        int s = (int) Math.floor(d.toSeconds());
        int m = s / 60;
        int r = s % 60;
        return m + ":" + (r < 10 ? "0" + r : String.valueOf(r));
    }
    // ==========================
    // Voice recording logic
    // ==========================

    @FXML
    private void handleMicToggle() {
        if (selectedConversation == null) return;

        switch (micState) {
            case NORMAL -> startVoiceRecording();
            case RECORDING -> stopRecordingIntoPreview();
            case PREVIEW -> {}
        }
    }

    private void startVoiceRecording() {
        try {
            // if you were editing a message, recording should cancel that UX
            if (editingMessage != null) cancelEdit();

            recording = true;

            setComposerModeRecordingUI();
            startRecTimer();

            AudioFormat format = new AudioFormat(16000f, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

            recordLine = (TargetDataLine) AudioSystem.getLine(info);
            recordLine.open(format);
            recordLine.start();

            java.nio.file.Path out = java.nio.file.Files.createTempFile("yedik_voice_", ".wav");
            lastRecordedFile = out.toFile();
            lastRecordedFile.deleteOnExit();

            AudioInputStream ais = new AudioInputStream(recordLine);

            recordThread = new Thread(() -> {
                try {
                    AudioSystem.write(ais, AudioFileFormat.Type.WAVE, lastRecordedFile);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }, "voice-recorder");

            recordThread.setDaemon(true);
            recordThread.start();

        } catch (Exception e) {
            e.printStackTrace();
            recording = false;
            stopRecTimer();
            cleanupRecorder();
            setComposerModeNormalUI();
        }
    }

    private void stopVoiceRecordingAndSend() {
        // kept for compatibility
        stopRecordingIntoPreview();
    }

    private void stopRecordingIntoPreview() {
        try {
            recording = false;

            stopRecTimer();

            if (recordLine != null) {
                try { recordLine.stop(); } catch (Exception ignored) {}
                try { recordLine.close(); } catch (Exception ignored) {}
            }

            if (recordThread != null) {
                try { recordThread.join(800); } catch (InterruptedException ignored) {}
            }

            // validate file
            if (lastRecordedFile == null || !lastRecordedFile.exists() || lastRecordedFile.length() <= 44) {
                lastRecordedFile = null;
                setComposerModeNormalUI();
                return;
            }
            setComposerModePreviewUI();
            if (recPreviewTimeLabel != null) recPreviewTimeLabel.setText("00:00");
            if (recPreviewPlayIcon != null) recPreviewPlayIcon.setIconLiteral("mdi2p-play");
            if (micIcon != null) micIcon.setIconLiteral("mdi2p-play");

        } catch (Exception e) {
            e.printStackTrace();
            lastRecordedFile = null;
            setComposerModeNormalUI();
        } finally {
            recordLine = null;
            recordThread = null;
        }
    }

    private void cleanupRecorder() {
        recordLine = null;
        recordThread = null;
        lastRecordedFile = null;
    }
    @FXML
    private void handleCancelRecording() {
        stopRecTimer();

        if (recordLine != null) {
            try { recordLine.stop(); } catch (Exception ignored) {}
            try { recordLine.close(); } catch (Exception ignored) {}
        }
        recordLine = null;
        recordThread = null;
        recording = false;

        cleanupPreviewPlayer();

        if (lastRecordedFile != null && lastRecordedFile.exists()) {
            try { lastRecordedFile.delete(); } catch (Exception ignored) {}
        }
        lastRecordedFile = null;

        setComposerModeNormalUI();
    }

    @FXML
    private void handlePreviewPlayPause() {
        togglePreviewPlayback();
    }

    private void togglePreviewPlayback() {
        if (lastRecordedFile == null || !lastRecordedFile.exists()) return;

        try {
            if (previewPlayer == null) {
                javafx.scene.media.Media media = new javafx.scene.media.Media(lastRecordedFile.toURI().toString());
                previewPlayer = new javafx.scene.media.MediaPlayer(media);

                previewPlayer.currentTimeProperty().addListener((obs, old, cur) -> {
                    if (recPreviewTimeLabel != null) recPreviewTimeLabel.setText(formatMMSS((int) cur.toSeconds()));
                });

                previewPlayer.setOnEndOfMedia(() -> {
                    if (recPreviewPlayIcon != null) recPreviewPlayIcon.setIconLiteral("mdi2p-play");
                    if (micIcon != null) micIcon.setIconLiteral("mdi2p-play");
                    previewPlayer.stop();
                });
            }

            var st = previewPlayer.getStatus();
            if (st == javafx.scene.media.MediaPlayer.Status.PLAYING) {
                previewPlayer.pause();
                if (recPreviewPlayIcon != null) recPreviewPlayIcon.setIconLiteral("mdi2p-play");
                if (micIcon != null) micIcon.setIconLiteral("mdi2p-play");
            } else {
                previewPlayer.play();
                if (recPreviewPlayIcon != null) recPreviewPlayIcon.setIconLiteral("mdi2p-pause");
                if (micIcon != null) micIcon.setIconLiteral("mdi2p-pause");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void cleanupPreviewPlayer() {
        if (previewPlayer != null) {
            try { previewPlayer.stop(); } catch (Exception ignored) {}
            try { previewPlayer.dispose(); } catch (Exception ignored) {}
            previewPlayer = null;
        }
    }

    private void startRecTimer() {
        recSeconds = 0;
        if (recTimerLabel != null) recTimerLabel.setText("00:00");

        stopRecTimer();
        recTimeline = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1), e -> {
                    recSeconds++;
                    if (recTimerLabel != null) recTimerLabel.setText(formatMMSS(recSeconds));
                })
        );
        recTimeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
        recTimeline.play();
    }

    private void stopRecTimer() {
        if (recTimeline != null) {
            recTimeline.stop();
            recTimeline = null;
        }
    }

    private String formatMMSS(int seconds) {
        int m = seconds / 60;
        int s = seconds % 60;
        return String.format("%02d:%02d", m, s);
    }

    private void setComposerModeNormalUI() {
        micState = MicState.NORMAL;

        micBtn.setVisible(true);
        micBtn.setManaged(true);

        messageInput.setVisible(true);
        messageInput.setManaged(true);

        emojiBtn.setVisible(true);
        emojiBtn.setManaged(true);

        recCancelBtn.setVisible(false);
        recCancelBtn.setManaged(false);

        recIndicatorBox.setVisible(false);
        recIndicatorBox.setManaged(false);

        recPreviewBox.setVisible(false);
        recPreviewBox.setManaged(false);

        if (micIcon != null) micIcon.setIconLiteral("mdi2m-microphone");
    }

    private void setComposerModeRecordingUI() {
        micState = MicState.RECORDING;

        micBtn.setVisible(true);
        micBtn.setManaged(true);

        messageInput.setVisible(false);
        messageInput.setManaged(false);

        emojiBtn.setVisible(false);
        emojiBtn.setManaged(false);

        recCancelBtn.setVisible(true);
        recCancelBtn.setManaged(true);

        recIndicatorBox.setVisible(true);
        recIndicatorBox.setManaged(true);

        recPreviewBox.setVisible(false);
        recPreviewBox.setManaged(false);

        recTimerLabel.setText("00:00");
        if (micIcon != null) micIcon.setIconLiteral("mdi2s-stop");
    }

    private void setComposerModePreviewUI() {
        micState = MicState.PREVIEW;
        micBtn.setVisible(false);
        micBtn.setManaged(false);

        messageInput.setVisible(false);
        messageInput.setManaged(false);

        emojiBtn.setVisible(false);
        emojiBtn.setManaged(false);

        recCancelBtn.setVisible(true);
        recCancelBtn.setManaged(true);

        recIndicatorBox.setVisible(false);
        recIndicatorBox.setManaged(false);

        recPreviewBox.setVisible(true);
        recPreviewBox.setManaged(true);

        if (recPreviewPlayIcon != null) recPreviewPlayIcon.setIconLiteral("mdi2p-play");
    }
    // ==========================
    // Media pannel
    // ==========================

    private void openMediaPanel(int tabIndex) throws SQLException {
        if (selectedConversation == null) return;
        restoreDrawerAfterMedia = isDrawerOpen();
        hideDrawer();
        mediaOverlay.setVisible(true);
        mediaOverlay.setManaged(true);
        mediaOverlay.toFront();
        mediaTabs.getSelectionModel().select(tabIndex);
        refreshMediaPanel(selectedConversation.getId());
    }

    @FXML
    private void closeMediaPanel() {
        mediaOverlay.setVisible(false);
        mediaOverlay.setManaged(false);

        if (restoreDrawerAfterMedia) {
            showDrawer();
        }
        restoreDrawerAfterMedia = false;
    }

    private void refreshMediaPanel(long convId) throws SQLException {
        // clear UI
        mediaGrid.getChildren().clear();
        filesBox.getChildren().clear();
        linksBox.getChildren().clear();

        List<Message> msgs = messageService.listByConversation(convId, 500);

        // 1) Attachments (images/videos/audio/files)
        for (Message m : msgs) {
            if (!"ATTACHMENT".equalsIgnoreCase(m.getKind())) continue;

            ServiceMessage.AttachmentMeta meta;
            try {
                meta = messageService.getAttachmentMeta(m.getId());
            } catch (Exception ex) {
                continue;
            }
            if (meta == null) continue;

            String mime = meta.mimeType();
            if (mime == null) mime = "application/octet-stream";

            if (mime.startsWith("image/") || mime.startsWith("video/")) {
                mediaGrid.getChildren().add(buildMediaThumb(meta));
            } else {
                filesBox.getChildren().add(buildFileRow(meta));
            }
        }

        // 2) Links from text messages
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("(https?://\\S+)");
        LinkedHashSet<String> uniq = new LinkedHashSet<>();

        for (Message m : msgs) {
            if ("ATTACHMENT".equalsIgnoreCase(m.getKind())) continue;
            String body = m.getBody();
            if (body == null) continue;

            var mm = p.matcher(body);
            while (mm.find()) uniq.add(mm.group(1));
        }

        for (String url : uniq) {
            linksBox.getChildren().add(buildLinkRow(url));
        }
    }

    private Node buildMediaThumb(ServiceMessage.AttachmentMeta meta) {
        StackPane box = new StackPane();
        box.setPrefSize(104, 104);
        box.getStyleClass().add("media-thumb");

        String mime = meta.mimeType() == null ? "" : meta.mimeType();

        if (mime.startsWith("image/")) {
            try {
                byte[] data = messageService.readAttachmentBytes(meta.id());
                Image img = new Image(new ByteArrayInputStream(data));
                ImageView iv = new ImageView(img);
                iv.setPreserveRatio(true);
                iv.setFitWidth(104);
                iv.setFitHeight(104);
                Rectangle clip = new Rectangle(104, 104);
                clip.setArcWidth(12);
                clip.setArcHeight(12);
                iv.setClip(clip);
                box.getChildren().add(iv);
            } catch (Exception e) {
                box.getChildren().add(new FontIcon("mdi2i-image-outline"));
            }
        } else {
            // video thumb placeholder (fast + stable)
            FontIcon play = new FontIcon("mdi2p-play-circle-outline");
            play.setIconSize(26);
            box.getChildren().add(play);
        }

        box.setOnMouseClicked(e -> {
            try {
                openAttachment(meta); // uses your existing temp-file open logic (works now)
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        return box;
    }

    private Node buildFileRow(ServiceMessage.AttachmentMeta meta) {
        HBox row = new HBox(10);
        row.getStyleClass().add("file-row");
        row.setAlignment(Pos.CENTER_LEFT);

        FontIcon ico = new FontIcon("mdi2f-file-outline");
        ico.setIconSize(18);

        Label name = new Label(meta.fileName());
        name.getStyleClass().add("drawer-row-title");

        Label sub = new Label(humanSize(meta.sizeBytes()) + (meta.mimeType() != null ? " • " + meta.mimeType() : ""));
        sub.getStyleClass().add("muted");

        VBox text = new VBox(2, name, sub);

        row.getChildren().addAll(ico, text);
        row.setOnMouseClicked(e -> openAttachment(meta));
        return row;
    }

    private Node buildLinkRow(String url) {
        VBox row = new VBox(6);
        row.getStyleClass().add("link-row");

        String vid = extractYoutubeId(url);
        if (vid != null) {
            // reuse your existing YouTube card UI
            row.getChildren().add(buildYoutubePreviewCard(url, false));
        } else {
            Label title = new Label(prettifyUrl(url));
            title.getStyleClass().add("drawer-row-title");
            Label full = new Label(url);
            full.getStyleClass().add("muted");
            row.getChildren().addAll(title, full);
        }

        row.setOnMouseClicked(e -> openInBrowser(url));
        return row;
    }

    private boolean isDrawerOpen() {
        return drawerOverlay.isVisible() && drawerOverlay.isManaged();
    }

    private void showDrawer() {
        drawerOverlay.setVisible(true);
        drawerOverlay.setManaged(true);
        drawerOverlay.toFront();
    }

    private void hideDrawer() {
        drawerOverlay.setVisible(false);
        drawerOverlay.setManaged(false);
    }
    // ==========================
    // AI Summary Chip Logic
    // ==========================

    private void addAiSummaryChip(Runnable onClick) {
        // Prevent duplicates
        if (messagesContainer.lookup(".ai-chip-wrap") != null) {
            return;
        }

        HBox wrap = new HBox();
        wrap.getStyleClass().add("ai-chip-wrap");
        wrap.setAlignment(Pos.CENTER);

        StackPane chip = new StackPane();
        chip.getStyleClass().add("ai-chip");
        chip.setPickOnBounds(true);
        chip.setStyle("-fx-cursor: hand;");

        Label t = new Label("RÉSUMÉ IA");
        t.getStyleClass().add("ai-chip-label");

        chip.getChildren().add(t);
        wrap.getChildren().add(chip);

        chip.setOnMouseClicked(e -> {
            e.consume();
            if (onClick != null) onClick.run();
        });

        messagesContainer.getChildren().add(wrap);
        Platform.runLater(() -> messagesScroll.setVvalue(1.0));
    }

    //=========================
    // Calls LM Studio OpenAI-compatible /v1/chat/completions and returns the assistant text.
    //==========================
    private String lmStudioChatSummary(String conversationTitle, List<Message> contextMsgs) throws Exception {
        StringBuilder convo = new StringBuilder();
        for (Message m : contextMsgs) {
            boolean outgoing = (m.getSenderId() == currentUserId);
            String who = outgoing ? "Moi" : "Autre";
            String body = m.getBody() == null ? "" : m.getBody().trim();
            if (!body.isEmpty()) convo.append(who).append(": ").append(body).append("\n");
        }

        String system =
                "Tu es un assistant qui résume une conversation de messagerie.\n" +
                        "Retourne un résumé court en français, en 3 parties:\n" +
                        "1) Sujet (1 ligne)\n" +
                        "2) Points clés (3 bullets max)\n" +
                        "3) Action suivante (1 ligne)\n" +
                        "Sois factuel, pas de blabla.";

        String user =
                "Titre: " + (conversationTitle == null ? "Conversation" : conversationTitle) + "\n" +
                        "Messages:\n" + convo;

        String json =
                "{"
                        + "\"model\":\"" + escapeJson(LM_MODEL) + "\","
                        + "\"temperature\":0.2,"
                        + "\"messages\":["
                        +   "{\"role\":\"system\",\"content\":\"" + escapeJson(system) + "\"},"
                        +   "{\"role\":\"user\",\"content\":\"" + escapeJson(user) + "\"}"
                        + "]"
                        + "}";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(LM_BASE + "/chat/completions"))
                // LM Studio accepts any string api_key; OpenAI client examples use "lm-studio"
                .header("Authorization", "Bearer lm-studio")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
            throw new RuntimeException("LM Studio HTTP " + resp.statusCode() + ": " + resp.body());
        }

        // Minimal JSON extraction (no extra libs):
        // expects: choices[0].message.content
        String body = resp.body();
        int idx = body.indexOf("\"content\"");
        if (idx < 0) return "Résumé indisponible.";
        int start = body.indexOf(':', idx);
        int q1 = body.indexOf('"', start + 1);
        int q2 = q1;
        while (true) {
            q2 = body.indexOf('"', q2 + 1);
            if (q2 < 0) break;
            if (body.charAt(q2 - 1) != '\\') break; // not escaped quote
        }
        if (q1 < 0 || q2 < 0 || q2 <= q1) return "Résumé indisponible.";
        String raw = body.substring(q1 + 1, q2);
        return raw.replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\").trim();
    }

    private StackPane popupHost() {
        // 1) preferred: your @FXML root (if wired)
        if (root != null) return root;

        // 2) fallback: nearest StackPane parent in the scene graph
        if (messagesScroll != null && messagesScroll.getScene() != null) {
            var r = messagesScroll.getScene().getRoot();
            if (r instanceof StackPane sp) return sp;
        }
        if (messagesContainer != null && messagesContainer.getScene() != null) {
            var r = messagesContainer.getScene().getRoot();
            if (r instanceof StackPane sp) return sp;
        }
        return null;
    }

    private void openAiSummaryFrom(String title, List<Message> toSummarize) {
        aiPendingTitle = (title == null || title.isBlank()) ? "Conversation" : title;
        aiPendingMessages = (toSummarize == null) ? List.of() : new ArrayList<>(toSummarize);

        openAiSummary();
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"'  -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.toString();
    }

    @FXML
    private void openAiSummary() {

        if (aiSummaryOverlay == null) return;

        aiSummaryTitle.setText("Résumé IA");
        aiSummaryText.clear();

        aiSummaryLoading.setVisible(true);
        aiSummaryLoading.setManaged(true);

        aiSummaryOverlay.setVisible(true);
        aiSummaryOverlay.setManaged(true);

        final long convIdSnap = aiPendingConvId;
        final String titleSnap = aiPendingTitle;
        final List<Message> msgsSnap = new ArrayList<>(aiPendingMessages);

        new Thread(() -> {
            try {
                String summary = lmStudioChatSummary(titleSnap, msgsSnap);

                Platform.runLater(() -> {
                    // Ignore result if user changed conversation
                    if (selectedConversation == null ||
                            selectedConversation.getId() != convIdSnap) {
                        return;
                    }

                    aiSummaryLoading.setVisible(false);
                    aiSummaryLoading.setManaged(false);
                    aiSummaryText.setText(summary == null ? "" : summary);
                });

            } catch (Exception ex) {
                Platform.runLater(() -> {
                    aiSummaryLoading.setVisible(false);
                    aiSummaryLoading.setManaged(false);
                    aiSummaryText.setText("Résumé indisponible (LM Studio non joignable).");
                });
            }
        }).start();
    }

    @FXML
    private void closeAiSummary() {
        aiSummaryOverlay.setVisible(false);
        aiSummaryOverlay.setManaged(false);

        // suppress chip until unread increases again
        if (aiPendingConvId > 0) {
            aiSummarySeenAtUnread.put(aiPendingConvId, aiPendingUnreadCount);
        }
    }

    private void updateAiSummaryAvailability() {
        if (selectedConversation == null) {
            return;
        }

        long convId = selectedConversation.getId();

        // aiPendingUnreadCount is set in loadMessages()
        int unread = aiPendingUnreadCount;

        // user dismissed/used AI at this unread count -> don’t show again until unread increases
        int seenAt = aiSummarySeenAtUnread.getOrDefault(convId, 0);

        boolean show = unread >= 10 && unread > seenAt;
    }
    // ==========================
    // API
    // ==========================

    private void toggleEmojiPopup() {
        if (!emojiBuilt) {
            buildEmojiPopup();
            emojiBuilt = true;
        }

        if (emojiPopup.isShowing()) {
            emojiPopup.hide();
            return;
        }

        javafx.geometry.Bounds b = emojiBtn.localToScreen(emojiBtn.getBoundsInLocal());
        if (b == null) return;

        // Desired popup size (must match buildEmojiPopup prefs)
        double popupW = 380;
        double popupH = 420;

        javafx.geometry.Rectangle2D screen = javafx.stage.Screen.getPrimary().getVisualBounds();

        double x = b.getMinX() - (popupW - 36);  // anchor near emoji button
        double y = b.getMaxY() + 8;

        // Clamp X/Y so it stays visible
        if (x + popupW > screen.getMaxX()) x = screen.getMaxX() - popupW - 8;
        if (x < screen.getMinX()) x = screen.getMinX() + 8;

        if (y + popupH > screen.getMaxY()) y = b.getMinY() - popupH - 8; // open above if no space below
        if (y < screen.getMinY()) y = screen.getMinY() + 8;

        emojiPopup.show(emojiBtn, x, y);
    }

    private void buildEmojiPopup() {
        emojiPopup = new javafx.stage.Popup();
        emojiPopup.setAutoHide(true);
        emojiPopup.setHideOnEscape(true);

        // Search field
        emojiSearch = new javafx.scene.control.TextField();
        emojiSearch.setPromptText("Search emoji...");
        emojiSearch.getStyleClass().add("eg-search");

        // Search results grid + scroll
        emojiSearchGrid = new javafx.scene.layout.TilePane();
        emojiSearchGrid.setPrefColumns(10);
        emojiSearchGrid.setHgap(6);
        emojiSearchGrid.setVgap(6);

        emojiSearchScroll = new javafx.scene.control.ScrollPane(emojiSearchGrid);
        emojiSearchScroll.setFitToWidth(true);
        emojiSearchScroll.setPrefViewportHeight(320); // IMPORTANT
        emojiSearchScroll.getStyleClass().add("eg-scroll");

        // Tabs per group
        emojiTabs = new javafx.scene.control.TabPane();
        emojiTabs.getStyleClass().add("eg-tabs");

        for (var entry : emojiRepo.byGroup().entrySet()) {
            String groupName = entry.getKey();
            java.util.List<String> emojis = entry.getValue();

            javafx.scene.layout.TilePane grid = createEmojiGrid(emojis);

            javafx.scene.control.ScrollPane sp = new javafx.scene.control.ScrollPane(grid);
            sp.setFitToWidth(true);
            sp.setPrefViewportHeight(320); // IMPORTANT
            sp.getStyleClass().add("eg-scroll");

            javafx.scene.control.Tab tab = new javafx.scene.control.Tab(groupName, sp);
            tab.setClosable(false);
            emojiTabs.getTabs().add(tab);
        }

        // Root container (tabs shown by default)
        javafx.scene.layout.VBox root = new javafx.scene.layout.VBox(10, emojiSearch, emojiTabs);
        root.getStyleClass().add("eg-popup");

        // Force a real size (prevents the "tiny corner" bug)
        root.setPrefSize(380, 420);
        root.setMinSize(javafx.scene.layout.Region.USE_PREF_SIZE, javafx.scene.layout.Region.USE_PREF_SIZE);
        root.setMaxSize(javafx.scene.layout.Region.USE_PREF_SIZE, javafx.scene.layout.Region.USE_PREF_SIZE);

        // Search behavior: swap tabs with search results while typing
        emojiSearch.textProperty().addListener((obs, oldVal, q) -> {
            if (q == null || q.isBlank()) {
                // restore tabs
                if (root.getChildren().contains(emojiSearchScroll)) root.getChildren().remove(emojiSearchScroll);
                if (!root.getChildren().contains(emojiTabs)) root.getChildren().add(emojiTabs);
                return;
            }

            // show search results instead of tabs
            if (root.getChildren().contains(emojiTabs)) root.getChildren().remove(emojiTabs);
            if (!root.getChildren().contains(emojiSearchScroll)) root.getChildren().add(emojiSearchScroll);

            java.util.List<String> results = emojiRepo.search(q, 240);

            emojiSearchGrid.getChildren().clear();
            for (String em : results) {
                javafx.scene.control.Button b = new javafx.scene.control.Button(em);
                b.getStyleClass().add("eg-emoji-btn");
                b.setOnAction(e -> {
                    insertAtCaret(messageInput, em);
                    emojiPopup.hide();
                });
                emojiSearchGrid.getChildren().add(b);
            }
        });

        emojiPopup.getContent().add(root);

        // Debug (remove later)
        System.out.println("Emoji groups loaded: " + emojiRepo.byGroup().size());
    }

    private void insertAtCaret(javafx.scene.control.TextField tf, String s) {
        int start = tf.getSelection().getStart();
        int end = tf.getSelection().getEnd();
        String t = tf.getText() == null ? "" : tf.getText();

        tf.setText(t.substring(0, start) + s + t.substring(end));
        tf.positionCaret(start + s.length());
        tf.requestFocus();
    }

    private javafx.scene.layout.TilePane createEmojiGrid(java.util.List<String> emojis) {
        javafx.scene.layout.TilePane grid = new javafx.scene.layout.TilePane();
        grid.setPrefColumns(10);
        grid.setHgap(6);
        grid.setVgap(6);

        for (String em : emojis) {
            javafx.scene.control.Button b = new javafx.scene.control.Button(em);
            b.getStyleClass().add("eg-emoji-btn");
            b.setOnAction(e -> {
                insertAtCaret(messageInput, em);
                emojiPopup.hide();
            });
            grid.getChildren().add(b);
        }
        return grid;
    }

}
