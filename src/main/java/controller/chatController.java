package controller;

import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
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
import javafx.scene.layout.BorderPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.stage.Window;
import javafx.scene.input.KeyCode;
import javafx.geometry.Pos;
import javafx.scene.paint.Color;
import javafx.scene.image.Image;
import javafx.scene.paint.ImagePattern;
import java.io.ByteArrayInputStream;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Dialog;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ButtonBar;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.scene.control.TextArea;




import model.ParticipantView;


import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;




import java.sql.SQLException;
import javafx.application.Platform;
import javafx.scene.control.ScrollPane;
import java.util.List;
import java.util.Locale;

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



    @FXML private void toggleChatInfo()   { toggleSection(secChatInfo,   chevChatInfo); }
    @FXML private void toggleCustomize()  { toggleSection(secCustomize,  chevCustomize); }
    @FXML private void toggleMembers()    { toggleSection(secMembers,    chevMembers); }
    @FXML private void toggleMedia()      { toggleSection(secMedia,      chevMedia); }

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
    private int currentUserId = 1;
    //==========================
    //HELPER METHODS
    //==========================

    private static String displayName(ParticipantView p) {
        String n = p.getNickname();
        return (n == null || n.isBlank()) ? p.getUsername() : n.trim();
    }


    // =========================
    // INITIALIZATION
    // =========================

    @FXML
    public void initialize() {
        setSelectedConversation(null);
        messageInput.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE && editingMessage != null) {
                cancelEdit();
            }
        });
        loadConversations();
        dayTimer.scheduleAtFixedRate(new java.util.TimerTask() {
            @Override public void run() {
                Platform.runLater(() -> loadConversations());
            }
        }, 60_000, 60_000);
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

            drawerTitle.setText(info.getTitle() == null || info.getTitle().isBlank() ? "Chat" : info.getTitle());
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

            // 2) fallback by type
            String path = "assets/" + ("GROUP".equalsIgnoreCase(type) ? "group-default.png" : "user-default.png");
            var is = getClass().getClassLoader().getResourceAsStream(path);


            var stream = getClass().getClassLoader().getResourceAsStream(path);
            if (stream == null) {
                // last resort: plain color instead of crashing
                circle.setFill(javafx.scene.paint.Color.LIGHTGRAY);
                return;
            }

            Image img = new Image(stream, 0, 0, true, true);
            circle.setFill(new ImagePattern(img));

        } catch (Exception ex) {
            // last resort: never break UI for an image
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

        try {
            List<Conversation> list = conversationService.listForUser(currentUserId);

            for (Conversation conv : list) {
                VBox item = buildConversationItem(conv);
                conversationContainer.getChildren().add(item);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private VBox buildConversationItem(Conversation conv) {
        boolean hasUnread = conv.getUnreadCount() > 0;

        VBox wrapper = new VBox();
        wrapper.getStyleClass().add(
                (selectedConversation != null && selectedConversation.getId() == conv.getId())
                        ? "chat-item-selected"
                        : "chat-item"
        );
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

        if (hasUnread) {
            Label badge = new Label(String.valueOf(conv.getUnreadCount()));
            badge.getStyleClass().add("chat-badge"); // purple bubble + white text
            rightBox.getChildren().add(badge);
        }

        row.getChildren().addAll(avatar, textBox, spacer, rightBox);
        wrapper.getChildren().add(row);

        wrapper.setOnMouseClicked(e -> {
            setSelectedConversation(conv);
            selectedConversation = conv;
            applyAvatar(chatAvatarCircle,
                    selectedConversation.getAvatar(),
                    selectedConversation.getType());
            highlightSelected(wrapper);
            loadMessages(conv.getId());
            loadConversations();
            chatTitle.setText(name.getText());
            chatSubtitle.setText("Active conversation");
            refreshDrawer();
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

    private void showBubbleMenu(Label bubble, Message msg) {
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

            long otherLastRead = messageService.getMaxReadByOthers(conversationId, currentUserId);

            for (Message msg : messages) {
                boolean outgoing = msg.getSenderId() == currentUserId;

                // Row (left/right)
                HBox row = new HBox();
                row.setAlignment(outgoing ? Pos.TOP_RIGHT : Pos.TOP_LEFT);

                // Bubble container
                VBox bubbleBox = new VBox(6);
                bubbleBox.setAlignment(outgoing ? Pos.TOP_RIGHT : Pos.TOP_LEFT);

                // Bubble text
                Label bubble = new Label(msg.getBody());
                bubble.setWrapText(true);
                bubble.setMaxWidth(480);
                bubble.getStyleClass().add(outgoing ? "bubble-out" : "bubble-in");

                // Meta (time + ticks)
                HBox meta = new HBox(6);
                meta.setAlignment(outgoing ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

                if (msg.getEditedAt() != null) {
                    Label edited = new Label("édité");
                    edited.getStyleClass().add(outgoing ? "edited-out" : "edited-in");
                    meta.getChildren().add(edited);
                }

                Label time = new Label(formatBubbleTime(msg.getCreatedAt()));
                time.getStyleClass().add(outgoing ? "timestamp-out" : "timestamp");

                meta.getChildren().add(time);

                if (outgoing) {
                    FontIcon ticks = new FontIcon("mdi2c-check-all");
                    ticks.setIconSize(14);
                    if (msg.getId() <= otherLastRead) {
                        ticks.setStyle("-fx-icon-color: #7c3aed; -fx-font-family: 'Material Design Icons';");
                    } else {
                        ticks.setStyle("-fx-icon-color: #9aa0a6; -fx-font-family: 'Material Design Icons';");
                    }
                    meta.getChildren().add(ticks);
                    attachBubbleMenu(bubble, msg);
                    showBubbleMenu(bubble, msg);
                }

                bubbleBox.getChildren().addAll(bubble, meta);
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

    private void attachBubbleMenu(Label bubble, Message msg) {
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

        // Optional: visual hint in UI
        sendButton.setText("✔");
    }

    private void cancelEdit() {
        editingMessage = null;
        messageInput.clear();
        sendButton.setText("➤");
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
                editingMessage.setSenderId(currentUserId); // ownership check
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

            // default avatar image
            Image img = getDefaultUserAvatar();
            if (img != null) avatar.setFill(new ImagePattern(img));
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

            // small secondary line like FB (“username • role”)
            String role = item.getRole() == null ? "" : item.getRole().toLowerCase();
            sub.setText(item.getUsername() + (role.isBlank() ? "" : " • " + role));

            setText(null);
            setGraphic(row);
        }
    }



}
