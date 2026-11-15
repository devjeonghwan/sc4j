package org.fogofwarlabs.sc4j.launcher.ui.session;

import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.fogofwarlabs.sc4j.beaconlink.protocol.community.PresenceContent;
import org.fogofwarlabs.sc4j.engine.net.beaconlink.community.ChatChannel;
import org.fogofwarlabs.sc4j.engine.net.beaconlink.community.CommunitySession;
import org.fogofwarlabs.sc4j.launcher.i18n.LauncherTexts;
import org.fogofwarlabs.sc4j.launcher.session.*;
import org.fogofwarlabs.sc4j.launcher.ui.ComponentUtils;
import org.fogofwarlabs.sc4j.launcher.ui.LauncherShell;
import org.fogofwarlabs.sc4j.launcher.ui.ModalDialog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;

/**
 * Shows the logged-in account at the right end of the app bar and its menu: the account with its state over Friends, Whispers, My status,
 * Sign out everywhere else and Log out; each reads and commands the account's {@link LauncherSessionView}.
 * <p>
 * Friends opens the friends with their merged state, the own friend code with Copy, adding a friend by code, and Whisper, Ignore and Remove
 * for the selected friend. Whispers lists one conversation per player whispered with, friends first and then the newest first, each with its
 * unread count, and beside it the selected player's whispers both ways; the next whisper goes to that player whatever whisper is selected.
 * Whisper in Friends opens the friend's conversation, empty if new; the menu opens the newest conversation with unread whispers, else the
 * newest one, and counts the unread whispers of all conversations. Viewing a conversation reads its whispers.
 * My status chooses between Online and Away with an away text, and Sign out everywhere else asks once. Without a connection the panels and
 * the menu say why, and Sign out everywhere else and Send are disabled. Log out closes the menu and hands the log out to the window.
 */
public final class AccountMenu {
    private static final double MENU_WIDTH = 280;
    private static final double FRIENDS_HEIGHT = 220;
    private static final double WHISPERS_HEIGHT = 240;
    private static final double WHISPERS_WIDTH = 680;
    private static final double CONVERSATIONS_WIDTH = 210;
    private static final @NotNull String FRIEND_STATE_KEY_PREFIX = "session.friend.";
    private static final @NotNull String FAILURE_KEY_PREFIX = "session.failure.";
    private static final @NotNull String PART_SEPARATOR = " | ";

    /**
     * Receives what the menu hands to the window.
     */
    public interface Actions {
        /**
         * Called when the user logs out.
         */
        void onLogOut();

        /**
         * Called when the user copies text, the own friend code.
         *
         * @param text the text for the clipboard
         */
        void onCopy(@NotNull String text);
    }

    /**
     * Holds one conversation of the Whispers panel: the player whispered with, the player's name and tag, and the player's view as a friend.
     */
    private static final class Conversation {
        private final @NotNull FriendCode code;
        private final @NotNull String taggedName;
        private final @Nullable FriendView friend;

        /**
         * Creates a {@link Conversation} instance.
         *
         * @param code       the friend code of the player whispered with
         * @param taggedName the player's name and tag
         * @param friend     the player's view as a friend, or {@code null} for a player who is no friend
         */
        private Conversation(@NotNull FriendCode code, @NotNull String taggedName, @Nullable FriendView friend) {
            this.code       = code;
            this.taggedName = taggedName;
            this.friend     = friend;
        }


        /**
         * Indicates whether some other object is "equal to" this one: a conversation with the same player, shown the same way.
         *
         * @param other the reference object with which to compare
         * @return {@code true} if this object is the same as the other argument, {@code false} otherwise
         */
        @Override
        public boolean equals(@Nullable Object other) {
            return other instanceof Conversation conversation && this.code.equals(conversation.code) &&
                   this.taggedName.equals(conversation.taggedName) && Objects.equals(this.friend, conversation.friend);
        }

        /**
         * Returns a hash code value for the object.
         *
         * @return a hash code value for this object
         */
        @Override
        public int hashCode() {
            return Objects.hash(this.code, this.taggedName, this.friend);
        }
    }

    /**
     * Shows one friend of the Friends panel.
     */
    private final class FriendCell extends ListCell<FriendView> {
        /**
         * Updates the item associated with this cell.
         *
         * @param friend the new item for the cell
         * @param empty  whether or not this cell represents data from the list
         */
        @Override
        protected void updateItem(@Nullable FriendView friend, boolean empty) {
            super.updateItem(friend, empty);

            if (empty || friend == null) {
                this.setText(null);
                this.setGraphic(null);

                return;
            }

            VBox text = ComponentUtils.column(ComponentUtils.Spacing.XS, ComponentUtils.label(friend.getTaggedName(), "list-title"),
                                              ComponentUtils.label(AccountMenu.this.describe(friend), "text-caption"));
            HBox graphic = ComponentUtils.row(ComponentUtils.Spacing.M, ComponentUtils.statusMarker(getTone(friend.getState())), text);

            ComponentUtils.setCellGraphic(this, graphic);
        }
    }

    /**
     * Shows one conversation of the Whispers panel: the player's state marker (plain for a player who is no friend), name and tag, and the
     * number of unread whispers.
     */
    private final class ConversationCell extends ListCell<Conversation> {
        /**
         * Updates the item associated with this cell.
         *
         * @param conversation the new item for the cell
         * @param empty        whether or not this cell represents data from the list
         */
        @Override
        protected void updateItem(@Nullable Conversation conversation, boolean empty) {
            super.updateItem(conversation, empty);

            if (empty || conversation == null) {
                this.setText(null);
                this.setGraphic(null);

                return;
            }

            FriendView friend = conversation.friend;
            int unread = AccountMenu.this.countUnreadWhispers(conversation.code);
            ComponentUtils.Tone tone = friend == null ? ComponentUtils.Tone.NEUTRAL : getTone(friend.getState());
            HBox graphic = ComponentUtils.row(ComponentUtils.Spacing.M, ComponentUtils.statusMarker(tone),
                                              ComponentUtils.label(conversation.taggedName, "list-title"));

            if (unread > 0) {
                graphic.getChildren().addAll(ComponentUtils.growingSpacer(),
                                             ComponentUtils.badge(Integer.toString(unread), ComponentUtils.Tone.ACCENT));
            }

            ComponentUtils.setCellGraphic(this, graphic);
        }
    }

    /**
     * Shows one whisper of the Whispers panel: who sent it to whom, the text and, for a sent whisper, whether it is still under way or was
     * not delivered.
     */
    private final class WhisperCell extends ListCell<WhisperView> {
        /**
         * Updates the item associated with this cell.
         *
         * @param whisper the new item for the cell
         * @param empty   whether or not this cell represents data from the list
         */
        @Override
        protected void updateItem(@Nullable WhisperView whisper, boolean empty) {
            super.updateItem(whisper, empty);

            if (empty || whisper == null) {
                this.setText(null);
                this.setGraphic(null);

                return;
            }

            LauncherTexts texts = AccountMenu.this.texts;
            String title = texts.format(whisper.isIncoming() ? "session.whisper.from" : "session.whisper.to", whisper.getOtherTaggedName());
            VBox graphic = ComponentUtils.column(ComponentUtils.Spacing.XS, ComponentUtils.label(title, "list-title"),
                                                 ComponentUtils.paragraph(whisper.getText(), "text-body"));

            switch (whisper.getState()) {
                case SENDING -> graphic.getChildren().add(ComponentUtils.label(texts.get("session.whisper.sending"), "text-caption"));
                case NOT_DELIVERED -> graphic.getChildren().add(ComponentUtils.label(texts.get("session.whisper.not.delivered"), "text-caption"));
                case RECEIVED, DELIVERED -> {
                    // A received or delivered whisper needs no state line.
                }
            }

            ComponentUtils.setCellGraphic(this, graphic);
        }
    }

    /**
     * Shows the Friends panel: the own friend code with Copy, the friends, Whisper, Ignore and Remove for the selected friend, and adding a
     * friend by code.
     */
    private final class FriendsPanel {
        private final @NotNull ModalDialog dialog;
        private final @NotNull ListView<FriendView> list;
        private final @NotNull Button whisper;
        private final @NotNull Button ignore;
        private final @NotNull Button remove;
        private final @NotNull TextField added;

        /**
         * Creates a {@link FriendsPanel} instance showing the session's friends.
         */
        private FriendsPanel() {
            LauncherTexts texts = AccountMenu.this.texts;
            Label code = ComponentUtils.paragraph(AccountMenu.this.session.getFriendCode().getGroupedText(), "friend-code", "text-mono");
            Button copy = ComponentUtils.button(texts.get("session.friends.copy"), ComponentUtils.ButtonRole.SECONDARY);
            Button add = ComponentUtils.button(texts.get("session.friends.add"), ComponentUtils.ButtonRole.SECONDARY);
            Button close = ComponentUtils.button(texts.get("session.close"), ComponentUtils.ButtonRole.SECONDARY);

            this.dialog  = new ModalDialog(texts.get("session.friends.title"));
            this.list    = new ListView<>();
            this.whisper = ComponentUtils.button(texts.get("session.friends.whisper"), ComponentUtils.ButtonRole.SECONDARY);
            this.ignore  = ComponentUtils.button(texts.get("session.friends.ignore"), ComponentUtils.ButtonRole.SECONDARY);
            this.remove  = ComponentUtils.button(texts.get("session.friends.remove"), ComponentUtils.ButtonRole.SECONDARY);
            this.added   = new TextField();

            // The code wraps between its groups; Copy takes it whole.
            code.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(code, Priority.ALWAYS);
            copy.setOnAction(event -> AccountMenu.this.actions.onCopy(AccountMenu.this.session.getFriendCode().getText()));

            this.list.setCellFactory(view -> new FriendCell());
            this.list.setPlaceholder(ComponentUtils.paragraph(texts.get("session.friends.empty"), "text-caption"));
            this.list.setPrefHeight(FRIENDS_HEIGHT);
            this.list.getStyleClass().add("friend-list");
            this.list.getSelectionModel().selectedItemProperty().addListener((observable, previous, current) -> this.updateButtons());

            this.whisper.setOnAction(event -> {
                FriendView friend = this.list.getSelectionModel().getSelectedItem();

                if (friend != null) {
                    AccountMenu.this.showDialog(new WhisperPanel(new Conversation(friend.getCode(), friend.getTaggedName(), friend)));
                }
            });
            this.ignore.setOnAction(event -> {
                FriendView friend = this.list.getSelectionModel().getSelectedItem();

                if (friend != null) {
                    AccountMenu.this.session.setIgnored(friend.getCode(), !friend.isIgnored());
                }
            });
            this.remove.setOnAction(event -> {
                FriendView friend = this.list.getSelectionModel().getSelectedItem();

                if (friend != null) {
                    AccountMenu.this.session.removeFriend(friend.getCode());
                }
            });

            this.added.getStyleClass().add("friend-code-input");
            this.added.textProperty().addListener((observable, previous, current) -> this.dialog.showMessage(null));
            this.added.setOnAction(event -> this.add());
            HBox.setHgrow(this.added, Priority.ALWAYS);
            add.setOnAction(event -> this.add());

            AccountMenu.this.addConnectionNotice(this.dialog);
            this.dialog.addContent(ComponentUtils.field(texts.get("session.friends.code"), ComponentUtils.row(ComponentUtils.Spacing.S, code, copy)),
                                   this.list, ComponentUtils.row(ComponentUtils.Spacing.S, this.whisper, this.ignore, this.remove),
                                   ComponentUtils.field(texts.get("session.friends.add.code"),
                                                        ComponentUtils.row(ComponentUtils.Spacing.S, this.added, add)));
            this.dialog.setButtons(close, null);
            close.setOnAction(event -> AccountMenu.this.closeDialog());

            this.update();
        }

        /**
         * Shows the session's latest friends, keeping the selected friend selected.
         */
        private void update() {
            FriendView selected = this.list.getSelectionModel().getSelectedItem();

            this.list.getItems().setAll(AccountMenu.this.session.getFriends());

            if (selected != null) {
                for (FriendView friend : this.list.getItems()) {
                    if (friend.getCode().equals(selected.getCode())) {
                        this.list.getSelectionModel().select(friend);
                    }
                }
            }

            this.updateButtons();
        }

        private void updateButtons() {
            FriendView selected = this.list.getSelectionModel().getSelectedItem();
            LauncherTexts texts = AccountMenu.this.texts;

            this.whisper.setDisable(selected == null || selected.getState() == FriendState.OFFLINE);
            this.ignore.setDisable(selected == null);
            this.ignore.setText(texts.get(selected != null && selected.isIgnored() ? "session.friends.unignore" : "session.friends.ignore"));
            this.remove.setDisable(selected == null);
        }

        /**
         * Adds the friend of the entered code, or says in one line why not: the text is no code, it is the account's own code, the player is
         * a friend already, or the list is full.
         */
        private void add() {
            LauncherSessionView session = AccountMenu.this.session;
            LauncherTexts texts = AccountMenu.this.texts;
            List<FriendView> friends = session.getFriends();
            FriendCode code;

            try {
                code = FriendCode.parse(this.added.getText());
            } catch (IllegalArgumentException exception) {
                this.dialog.showMessage(texts.get("session.friends.invalid"));

                return;
            }

            if (code.equals(session.getFriendCode())) {
                this.dialog.showMessage(texts.get("session.friends.own"));

                return;
            }

            if (friends.stream().anyMatch(friend -> friend.getCode().equals(code))) {
                this.dialog.showMessage(texts.get("session.friends.known"));

                return;
            }

            if (friends.size() >= CommunitySession.MAXIMUM_FRIENDS) {
                this.dialog.showMessage(texts.format("session.friends.full", CommunitySession.MAXIMUM_FRIENDS));

                return;
            }

            session.addFriend(code);
            this.added.clear();
        }
    }

    /**
     * Shows the Whispers panel: the conversations and beside them the selected player's whispers both ways, the player the next whisper goes
     * to, and the message with Send.
     */
    private final class WhisperPanel {
        private final @NotNull ModalDialog dialog;
        private final @NotNull ListView<Conversation> conversations;
        private final @NotNull Label target;
        private final @NotNull ListView<WhisperView> list;
        private final @NotNull TextField message;
        private final @NotNull Button send;

        private final @Nullable Conversation opened;

        private @Nullable Conversation selected;

        /**
         * Creates a {@link WhisperPanel} instance on a player's conversation, listed while it has no whispers too, or on none.
         *
         * @param opened the conversation to open the panel on, or {@code null} for none
         */
        private WhisperPanel(@Nullable Conversation opened) {
            LauncherTexts texts = AccountMenu.this.texts;
            Button close = ComponentUtils.button(texts.get("session.close"), ComponentUtils.ButtonRole.SECONDARY);
            VBox chat;

            this.dialog        = new ModalDialog(texts.get("session.whispers.title"), WHISPERS_WIDTH);
            this.conversations = new ListView<>();
            this.target        = ComponentUtils.label("", "text-body");
            this.list          = new ListView<>();
            this.message       = new TextField();
            this.send          = ComponentUtils.button(texts.get("session.whispers.send"), ComponentUtils.ButtonRole.PRIMARY);

            this.opened = opened;

            this.selected = opened;

            this.conversations.setCellFactory(view -> new ConversationCell());
            this.conversations.setPlaceholder(ComponentUtils.paragraph(texts.get("session.whispers.empty"), "text-caption"));
            // The conversations take the height of the whispers, the message and the target beside them.
            this.conversations.setPrefSize(CONVERSATIONS_WIDTH, WHISPERS_HEIGHT);
            this.conversations.setMinWidth(CONVERSATIONS_WIDTH);
            this.conversations.getStyleClass().add("conversation-list");
            // Reselecting the shown conversation after the list was refreshed changes nothing.
            this.conversations.getSelectionModel().selectedItemProperty().addListener((observable, previous, current) -> {
                Conversation shown = this.selected;

                if (current != null && (shown == null || !current.code.equals(shown.code))) {
                    this.selected = current;
                    this.showSelected();
                    this.conversations.refresh();
                }
            });

            this.list.setCellFactory(view -> new WhisperCell());
            this.list.setPlaceholder(ComponentUtils.paragraph(texts.get("session.whispers.empty"), "text-caption"));
            this.list.setPrefHeight(WHISPERS_HEIGHT);
            this.list.getStyleClass().add("whisper-list");

            this.message.getStyleClass().add("whisper-input");
            this.message.textProperty().addListener((observable, previous, current) -> {
                this.dialog.showMessage(null);
                this.updateSend();
            });
            this.send.setOnAction(event -> this.sendMessage());
            close.setOnAction(event -> AccountMenu.this.closeDialog());

            chat = ComponentUtils.column(ComponentUtils.Spacing.S, this.target, this.list,
                                         ComponentUtils.field(texts.get("session.whispers.message"), this.message));
            HBox.setHgrow(chat, Priority.ALWAYS);

            AccountMenu.this.addConnectionNotice(this.dialog);
            this.dialog.addContent(ComponentUtils.row(ComponentUtils.Spacing.M, this.conversations, chat));
            this.dialog.setButtons(close, this.send);

            this.update();
        }

        /**
         * Shows the session's latest conversations, friends first, keeping the selected one selected, and the selected player's whispers.
         */
        private void update() {
            Conversation shown = this.selected;
            List<Conversation> conversations = new ArrayList<>(AccountMenu.this.listConversations(this.opened));
            Conversation selected = null;

            conversations.sort(Comparator.comparing(conversation -> conversation.friend == null));

            for (Conversation conversation : conversations) {
                if (shown != null && conversation.code.equals(shown.code)) {
                    selected = conversation;
                }
            }

            this.selected = selected;

            if (!conversations.equals(this.conversations.getItems())) {
                this.conversations.getItems().setAll(conversations);
            }

            if (selected != null) {
                this.conversations.getSelectionModel().select(selected);
            }

            this.showSelected();
            this.conversations.refresh();
        }

        /**
         * Shows the selected player's whispers, the newest in view, and counts them as read.
         */
        private void showSelected() {
            Conversation shown = this.selected;
            FriendCode code = shown == null ? null : shown.code;
            LauncherTexts texts = AccountMenu.this.texts;
            List<WhisperView> whispers = AccountMenu.this.session.getWhispers().stream().filter(whisper -> whisper.getOtherCode().equals(code)).toList();
            WhisperView newest = findNewestIncoming(whispers);

            if (!whispers.equals(this.list.getItems())) {
                this.list.getItems().setAll(whispers);
                this.list.scrollTo(whispers.size() - 1);
            }

            if (code != null && newest != null) {
                AccountMenu.this.lastReadWhispers.put(code, newest);
            }

            this.target.setText(shown == null ? texts.get("session.whispers.no.target") : texts.format("session.whisper.to", shown.taggedName));
            this.updateSend();
        }

        private void updateSend() {
            this.send.setDisable(this.selected == null || this.message.getText().isBlank() ||
                                 AccountMenu.this.session.getSelf().getState() != SessionState.CONNECTED);
        }

        private void sendMessage() {
            Conversation shown = this.selected;
            String text = this.message.getText().strip();

            if (shown == null || text.isEmpty()) {
                return;
            }

            if (getTextLength(text) > ChatChannel.MAXIMUM_TEXT_LENGTH) {
                this.dialog.showMessage(AccountMenu.this.texts.get("session.whispers.too.long"));

                return;
            }

            AccountMenu.this.session.whisper(shown.code, text);
            this.message.clear();
        }
    }

    /**
     * Returns the colour of the account's own state: green online, amber away, plain while not connected.
     *
     * @param self the account's own state
     * @return the colour role
     */
    public static @NotNull ComponentUtils.Tone getTone(@NotNull SelfView self) {
        if (self.getState() != SessionState.CONNECTED) {
            return ComponentUtils.Tone.NEUTRAL;
        }

        return self.getStatus() == SelfStatus.AWAY ? ComponentUtils.Tone.WARNING : ComponentUtils.Tone.SUCCESS;
    }

    private static @NotNull ComponentUtils.Tone getTone(@NotNull FriendState state) {
        return switch (state) {
            case OFFLINE -> ComponentUtils.Tone.NEUTRAL;
            case AWAY -> ComponentUtils.Tone.WARNING;
            case IN_LAUNCHER, ONLINE -> ComponentUtils.Tone.SUCCESS;
            case IN_ROOM, IN_GAME -> ComponentUtils.Tone.ACCENT;
        };
    }

    private static int getTextLength(@NotNull String text) {
        return text.getBytes(StandardCharsets.UTF_8).length;
    }

    private static @Nullable WhisperView findNewestIncoming(@NotNull List<@NotNull WhisperView> whispers) {
        for (int index = whispers.size() - 1; index >= 0; index--) {
            if (whispers.get(index).isIncoming()) {
                return whispers.get(index);
            }
        }

        return null;
    }


    private final @NotNull LauncherShell shell;
    private final @NotNull LauncherTexts texts;
    private final @NotNull String taggedName;
    private final @NotNull LauncherSessionView session;
    private final @NotNull Function<@NotNull String, @NotNull String> modNames;
    private final @NotNull Actions actions;

    private @Nullable VBox menu;
    private @Nullable ModalDialog dialog;
    private @Nullable FriendsPanel friendsPanel;
    private @Nullable WhisperPanel whisperPanel;

    private final @NotNull Map<@NotNull FriendCode, @NotNull WhisperView> lastReadWhispers;

    /**
     * Creates a {@link AccountMenu} instance; {@link #update()} shows the account in the app bar.
     *
     * @param shell      the shell whose app bar shows the account and which shows the menu and its dialogs
     * @param texts      the launcher texts of the display language
     * @param taggedName the account's name and tag ({@code Name#abcd12})
     * @param session    the account's session
     * @param modNames   finds the display name of a mod by its id, the id itself for a mod the install does not have
     * @param actions    receives what the menu hands to the window
     */
    public AccountMenu(@NotNull LauncherShell shell, @NotNull LauncherTexts texts, @NotNull String taggedName, @NotNull LauncherSessionView session,
                       @NotNull Function<@NotNull String, @NotNull String> modNames, @NotNull Actions actions) {
        this.shell      = shell;
        this.texts      = texts;
        this.taggedName = taggedName;
        this.session    = session;
        this.modNames   = modNames;
        this.actions    = actions;

        this.menu         = null;
        this.dialog       = null;
        this.friendsPanel = null;
        this.whisperPanel = null;

        this.lastReadWhispers = new HashMap<>();
    }

    /**
     * Shows the session's latest views: the account's state in the app bar, in the open menu and in the open panel.
     */
    public void update() {
        VBox opened = this.menu;
        FriendsPanel friends = this.friendsPanel;
        WhisperPanel whispers = this.whisperPanel;

        this.shell.setAccount(this.taggedName, getTone(this.session.getSelf()));

        if (friends != null) {
            friends.update();
        }

        if (whispers != null) {
            whispers.update();
        }

        // A click outside the menu closes it in the shell; a closed menu has left the scene.
        if (opened != null && opened.getScene() != null) {
            this.open();
        }
    }

    /**
     * Opens the menu under the account.
     */
    public void open() {
        SelfView self = this.session.getSelf();
        int unread = this.listConversations(null).stream().mapToInt(conversation -> this.countUnreadWhispers(conversation.code)).sum();
        VBox heading = ComponentUtils.column(ComponentUtils.Spacing.XS, ComponentUtils.label(this.taggedName, "list-title"),
                                             ComponentUtils.label(this.describe(self), "text-caption"));
        Button friends = ComponentUtils.menuButton(this.texts.get("session.menu.friends"));
        Button whispers = ComponentUtils.menuButton(unread > 0 ? this.texts.format("session.menu.whispers.new", unread)
                                                               : this.texts.get("session.menu.whispers"));
        Button status = ComponentUtils.menuButton(this.texts.get("session.menu.status"));
        Button signOut = ComponentUtils.menuButton(this.texts.get("session.menu.sign.out"));
        Button logOutButton = ComponentUtils.menuButton(this.texts.get("session.menu.log.out"));
        HBox header = ComponentUtils.row(ComponentUtils.Spacing.M, ComponentUtils.statusMarker(getTone(self)), heading);
        VBox opened = new VBox(header, new Separator(), friends, whispers, status, signOut, new Separator(), logOutButton);

        if (self.getState() == SessionState.CONNECTED && self.getDeviceCount() > 1) {
            heading.getChildren().add(ComponentUtils.label(this.texts.format("session.devices", self.getDeviceCount()), "text-caption"));
        }

        header.getStyleClass().add("account-menu-header");
        opened.getStyleClass().addAll("panel", "account-menu");
        opened.setPrefWidth(MENU_WIDTH);

        friends.setOnAction(event -> this.showDialog(new FriendsPanel()));
        whispers.setOnAction(event -> this.openWhispers());
        status.setOnAction(event -> this.openStatus());
        signOut.setOnAction(event -> this.openSignOut());
        signOut.setDisable(self.getState() != SessionState.CONNECTED);
        logOutButton.setOnAction(event -> {
            this.closeMenu();
            this.actions.onLogOut();
        });

        this.menu = opened;
        this.shell.openMenu(opened);
    }

    /**
     * Lists the conversations, one per player whispered with, the one of the newest whisper first; the conversation a panel was opened on
     * comes first while it has no whispers.
     *
     * @param opened the conversation a panel was opened on, or {@code null} for none
     */
    private @NotNull List<@NotNull Conversation> listConversations(@Nullable Conversation opened) {
        List<WhisperView> whispers = this.session.getWhispers();
        Map<FriendCode, Conversation> conversations = new LinkedHashMap<>();

        if (opened != null && whispers.stream().noneMatch(whisper -> whisper.getOtherCode().equals(opened.code))) {
            conversations.put(opened.code, new Conversation(opened.code, opened.taggedName, this.findFriend(opened.code)));
        }

        for (WhisperView whisper : whispers.reversed()) {
            conversations.computeIfAbsent(whisper.getOtherCode(),
                                          code -> new Conversation(code, whisper.getOtherTaggedName(), this.findFriend(code)));
        }

        return List.copyOf(conversations.values());
    }

    private @Nullable FriendView findFriend(@NotNull FriendCode code) {
        return this.session.getFriends().stream().filter(friend -> friend.getCode().equals(code)).findFirst().orElse(null);
    }

    /**
     * Counts the whispers a player sent after the newest one shown in an open Whispers panel.
     */
    private int countUnreadWhispers(@NotNull FriendCode player) {
        List<WhisperView> whispers = this.session.getWhispers();
        WhisperView read = this.lastReadWhispers.get(player);
        int unread = 0;

        // A read whisper the session no longer keeps is older than every whisper it keeps.
        for (int index = read == null ? 0 : whispers.lastIndexOf(read) + 1; index < whispers.size(); index++) {
            WhisperView whisper = whispers.get(index);

            if (whisper.isIncoming() && whisper.getOtherCode().equals(player)) {
                unread++;
            }
        }

        return unread;
    }

    /**
     * Opens the Whispers panel on the newest conversation with unread whispers, else on the newest one, if any.
     */
    private void openWhispers() {
        List<Conversation> conversations = this.listConversations(null);
        Conversation opened = conversations.stream().filter(conversation -> this.countUnreadWhispers(conversation.code) > 0).findFirst()
                                           .orElse(conversations.isEmpty() ? null : conversations.getFirst());

        this.showDialog(new WhisperPanel(opened));
    }

    private void openStatus() {
        SelfView self = this.session.getSelf();
        ModalDialog shown = new ModalDialog(this.texts.get("session.status.title"));
        ToggleGroup group = new ToggleGroup();
        ToggleButton online = new ToggleButton(this.texts.get("session.status.online"));
        ToggleButton away = new ToggleButton(this.texts.get("session.status.away"));
        TextField awayText = new TextField(self.getAwayText());
        Button cancel = ComponentUtils.button(this.texts.get("session.cancel"), ComponentUtils.ButtonRole.SECONDARY);
        Button save = ComponentUtils.button(this.texts.get("session.save"), ComponentUtils.ButtonRole.PRIMARY);

        online.setToggleGroup(group);
        away.setToggleGroup(group);
        group.selectToggle(self.getStatus() == SelfStatus.AWAY ? away : online);
        // A toggle group lets the selected button be clicked off; one of the two statuses always stays chosen.
        group.selectedToggleProperty().addListener((observable, previous, current) -> {
            if (current == null) {
                group.selectToggle(previous);
            }
        });
        awayText.disableProperty().bind(away.selectedProperty().not());
        awayText.textProperty().addListener((observable, previous, current) -> shown.showMessage(null));

        this.addConnectionNotice(shown);
        shown.addContent(ComponentUtils.row(ComponentUtils.Spacing.S, online, away),
                         ComponentUtils.field(this.texts.get("session.status.away.text"), awayText));
        shown.setButtons(cancel, save);
        cancel.setOnAction(event -> this.closeDialog());
        save.setOnAction(event -> {
            SelfStatus status = away.isSelected() ? SelfStatus.AWAY : SelfStatus.ONLINE;
            String text = status == SelfStatus.AWAY ? awayText.getText().strip() : "";

            if (getTextLength(text) > PresenceContent.MAXIMUM_AWAY_TEXT_LENGTH) {
                shown.showMessage(this.texts.get("session.status.too.long"));

                return;
            }

            this.closeDialog();
            this.session.setStatus(status, text);
        });

        this.showDialog(shown);
    }

    private void openSignOut() {
        ModalDialog shown = new ModalDialog(this.texts.get("session.sign.out.title"));
        Button cancel = ComponentUtils.button(this.texts.get("session.cancel"), ComponentUtils.ButtonRole.SECONDARY);
        Button signOut = ComponentUtils.button(this.texts.get("session.sign.out"), ComponentUtils.ButtonRole.DANGER);

        shown.addContent(ComponentUtils.paragraph(this.texts.format("session.sign.out.text", this.taggedName), "text-body"));
        shown.setButtons(cancel, signOut);
        cancel.setOnAction(event -> this.closeDialog());
        signOut.setOnAction(event -> {
            this.closeDialog();
            this.session.signOutOtherDevices();
        });

        this.showDialog(shown);
        cancel.requestFocus();
    }

    private void addConnectionNotice(@NotNull ModalDialog shown) {
        if (this.session.getSelf().getState() != SessionState.CONNECTED) {
            shown.addContent(ComponentUtils.notice(ComponentUtils.Tone.NEUTRAL, this.texts.get("session.not.connected")));
        }
    }

    private void closeMenu() {
        this.menu = null;
        this.shell.closeMenu();
    }

    private void showDialog(@NotNull FriendsPanel shown) {
        this.showDialog(shown.dialog);
        this.friendsPanel = shown;
    }

    private void showDialog(@NotNull WhisperPanel shown) {
        this.showDialog(shown.dialog);
        this.whisperPanel = shown;
    }

    /**
     * Shows a dialog in place of the menu and of the dialog shown before.
     */
    private void showDialog(@NotNull ModalDialog shown) {
        this.closeMenu();
        this.closeDialog();

        this.dialog = shown;
        this.shell.showDialog(shown);
    }

    private void closeDialog() {
        ModalDialog shown = this.dialog;

        this.dialog       = null;
        this.friendsPanel = null;
        this.whisperPanel = null;

        if (shown != null) {
            this.shell.closeDialog(shown);
        }
    }

    private @NotNull String describe(@NotNull SelfView self) {
        SessionFailure failure = self.getFailure();

        return switch (self.getState()) {
            case OFFLINE -> failure == null ? this.texts.get("session.state.offline")
                                            : this.texts.get(FAILURE_KEY_PREFIX + failure.name().toLowerCase(Locale.ROOT));
            case CONNECTING -> this.texts.get("session.state.connecting");
            case CONNECTED -> self.getStatus() == SelfStatus.ONLINE ? this.texts.get("session.status.online")
                                                                    : this.describeAway(self.getAwayText());
        };
    }

    private @NotNull String describe(@NotNull FriendView friend) {
        FriendState state = friend.getState();
        String modId = friend.getModId();
        List<String> parts = new ArrayList<>();

        parts.add(state == FriendState.AWAY ? this.describeAway(friend.getAwayText())
                                            : this.texts.get(FRIEND_STATE_KEY_PREFIX + state.name().toLowerCase(Locale.ROOT)));

        if (modId != null) {
            parts.add(this.modNames.apply(modId));
        }

        if (friend.getDeviceCount() > 1) {
            parts.add(this.texts.format("session.friend.devices", friend.getDeviceCount()));
        }

        if (friend.isIgnored()) {
            parts.add(this.texts.get("session.friend.ignored"));
        }

        return String.join(PART_SEPARATOR, parts);
    }

    private @NotNull String describeAway(@NotNull String awayText) {
        return awayText.isEmpty() ? this.texts.get("session.status.away") : this.texts.format("session.status.away.line", awayText);
    }
}
