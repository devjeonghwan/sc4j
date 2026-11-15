package org.fogofwarlabs.sc4j.launcher.ui.session;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.Region;
import org.fogofwarlabs.sc4j.beaconlink.peer.account.Account;
import org.fogofwarlabs.sc4j.launcher.i18n.LauncherTexts;
import org.fogofwarlabs.sc4j.launcher.i18n.UiLanguage;
import org.fogofwarlabs.sc4j.launcher.session.*;
import org.fogofwarlabs.sc4j.launcher.ui.LauncherFxScenario;
import org.fogofwarlabs.sc4j.launcher.ui.LauncherShell;
import org.fogofwarlabs.sc4j.launcher.ui.ThemeUtils;
import org.fogofwarlabs.sc4j.manifest.install.InstallLayout;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * Tests the account menu at the top right at display scales of 100% and 200% in English and Korean: Friends, Whispers, My status, Sign out
 * everywhere else and Log out. With a connected session the Friends panel lists the friends with their state and device count, shows and
 * copies the own friend code, adds a friend by code (a text that is no code, the own code and a known friend are refused in one line),
 * whispers to, ignores and removes the selected friend; the Whispers panel keeps one conversation per player (friends first, newest first)
 * with that player's whispers both ways only, sends to the selected conversation's player whatever whisper is selected and counts unread
 * whispers per conversation, and the menu counts them all; My status sends the chosen status (an away text that is too long is refused), and
 * Sign out everywhere else asks once. Without a connection the panels and the menu say why, and Sign out everywhere else is disabled. With
 * the real sessions of three accounts at a node on 127.0.0.1 the panels show the friends added by code in the launcher, away with a text,
 * and the whispers of two friends in their own conversations. Each view is saved as a sample picture.
 */
final class AccountMenuTest {
    private static final double SCENE_WIDTH = 1280;
    private static final double SCENE_HEIGHT = 800;
    private static final double PERCENT_SCALE = 100;
    private static final @NotNull String TAGGED_NAME = "Raynor#60bf07";
    private static final @NotNull String AWAY_TEXT = "Back at 9";
    private static final @NotNull String TOO_LONG_AWAY_TEXT = "x".repeat(40);
    private static final @NotNull String TOO_LONG_WHISPER = "x".repeat(300);
    private static final @NotNull String MENU_SELECTOR = ".account-menu";
    private static final @NotNull String DIALOG_SELECTOR = ".modal-scrim";
    private static final @NotNull String ACCOUNT_SELECTOR = ".account-button";
    private static final @NotNull String FRIEND_LIST_SELECTOR = ".friend-list";
    private static final @NotNull String FRIEND_CODE_SELECTOR = ".friend-code";
    private static final @NotNull String FRIEND_CODE_INPUT_SELECTOR = ".friend-code-input";
    private static final @NotNull String WHISPER_INPUT_SELECTOR = ".whisper-input";
    private static final @NotNull String WHISPER_LIST_SELECTOR = ".whisper-list";
    private static final @NotNull String CONVERSATION_LIST_SELECTOR = ".conversation-list";
    private static final @NotNull String CELL_TITLE_SELECTOR = ".list-title";
    private static final @NotNull String BADGE_SELECTOR = ".badge";
    private static final @NotNull String WARNING_MARKER = "status-marker-warning";
    private static final @NotNull String GAME_MOD_ID = "test-game";
    private static final @NotNull String GAME_NAME = "Test game";
    private static final @NotNull FriendCode OWN_CODE = createCode(1);
    private static final @NotNull FriendCode KERRIGAN = createCode(2);
    private static final @NotNull FriendCode ZERATUL = createCode(3);
    private static final @NotNull FriendCode ARTANIS = createCode(4);
    private static final @NotNull FriendCode FENIX = createCode(5);
    private static final @NotNull FriendCode NEW_FRIEND = createCode(6);
    private static final @NotNull FriendCode TASSADAR = createCode(7);
    private static final @NotNull List<@NotNull FriendView> FRIENDS = List.of(
            new FriendView(KERRIGAN, "Kerrigan#a1b2c3", FriendState.IN_GAME, GAME_MOD_ID, "", 2, false),
            new FriendView(ZERATUL, "Zeratul#d4e5f6", FriendState.AWAY, null, "brb", 1, false),
            new FriendView(ARTANIS, "Artanis#0a0b0c", FriendState.IN_LAUNCHER, null, "", 1, true),
            new FriendView(FENIX, "Fenix#112233", FriendState.OFFLINE, null, "", 0, false)
    );
    private static final @NotNull List<@NotNull WhisperView> WHISPERS = List.of(
            new WhisperView(KERRIGAN, "Kerrigan#a1b2c3", "Ready for a game?", 1_000L, WhisperState.RECEIVED),
            new WhisperView(ZERATUL, "Zeratul#d4e5f6", "Are you there?", 2_000L, WhisperState.NOT_DELIVERED),
            new WhisperView(ZERATUL, "Zeratul#d4e5f6", "Here now", 3_000L, WhisperState.RECEIVED),
            new WhisperView(ZERATUL, "Zeratul#d4e5f6", "Need a hand?", 4_000L, WhisperState.RECEIVED),
            new WhisperView(TASSADAR, "Tassadar#998877", "Good game", 5_000L, WhisperState.RECEIVED)
    );

    /**
     * Plays a session with friends and whispers and records the commands of the menu.
     */
    private static final class RecordingSession implements LauncherSessionView {
        private final @NotNull List<@NotNull String> commands;
        private @NotNull SelfView self;
        private @Nullable Runnable listener;

        private RecordingSession(@NotNull SelfView self) {
            this.commands = new ArrayList<>();
            this.self     = self;
            this.listener = null;
        }

        @Override
        public @NotNull SelfView getSelf() {
            return this.self;
        }

        @Override
        public @NotNull FriendCode getFriendCode() {
            return OWN_CODE;
        }

        @Override
        public @NotNull List<@NotNull FriendView> getFriends() {
            return this.self.getState() == SessionState.CONNECTED ? FRIENDS : List.of();
        }

        @Override
        public @NotNull List<@NotNull WhisperView> getWhispers() {
            return this.self.getState() == SessionState.CONNECTED ? WHISPERS : List.of();
        }

        @Override
        public void setOnChanged(@Nullable Runnable listener) {
            this.listener = listener;
        }

        @Override
        public void setStatus(@NotNull SelfStatus status, @NotNull String awayText) {
            Runnable changed = this.listener;

            this.commands.add("status " + status + ":" + awayText);
            this.self = new SelfView(this.self.getState(), status, awayText, this.self.getDeviceCount(), this.self.getFailure());

            if (changed != null) {
                changed.run();
            }
        }

        @Override
        public void addFriend(@NotNull FriendCode friend) {
            this.commands.add("add " + friend.getText());
        }

        @Override
        public void removeFriend(@NotNull FriendCode friend) {
            this.commands.add("remove " + friend.getText());
        }

        @Override
        public void setIgnored(@NotNull FriendCode player, boolean ignored) {
            this.commands.add((ignored ? "ignore " : "unignore ") + player.getText());
        }

        @Override
        public void whisper(@NotNull FriendCode player, @NotNull String text) {
            this.commands.add("whisper " + player.getText() + ":" + text);
        }

        @Override
        public void signOutOtherDevices() {
            this.commands.add("sign out");
        }

        @Override
        public void close() {
            this.listener = null;
        }
    }

    /**
     * Records what the menu hands to the window.
     */
    private static final class RecordingActions implements AccountMenu.Actions {
        private final @NotNull AtomicInteger logOuts;
        private final @NotNull List<@NotNull String> copied;

        private RecordingActions() {
            this.logOuts = new AtomicInteger();
            this.copied  = new ArrayList<>();
        }

        @Override
        public void onLogOut() {
            this.logOuts.incrementAndGet();
        }

        @Override
        public void onCopy(@NotNull String text) {
            this.copied.add(text);
        }
    }

    @BeforeAll
    static void startToolkit() {
        LauncherFxScenario.startToolkit();
    }

    private static @NotNull FriendCode createCode(int fill) {
        byte[] key = new byte[FriendCode.KEY_LENGTH];

        Arrays.fill(key, (byte) fill);

        return FriendCode.of(key);
    }

    private static <T extends Node> @NotNull List<T> find(@NotNull Parent root, @NotNull Class<T> type) {
        List<T> found = new ArrayList<>();

        for (Node node : root.lookupAll("*")) {
            if (type.isInstance(node)) {
                found.add(type.cast(node));
            }
        }

        return found;
    }

    private static boolean showsText(@NotNull Parent root, @NotNull String text) {
        return find(root, Label.class).stream().anyMatch(label -> LauncherFxScenario.isShown(label) && label.getText().contains(text));
    }

    private static @NotNull Parent getMenu(@NotNull LauncherShell shell) {
        Node menu = shell.getRoot().lookup(MENU_SELECTOR);

        Assertions.assertNotNull(menu, "the account menu is open");

        return (Parent) menu;
    }

    private static @NotNull Parent getDialog(@NotNull LauncherShell shell) {
        Node dialog = shell.getRoot().lookup(DIALOG_SELECTOR);

        Assertions.assertNotNull(dialog, "a dialog is shown");

        return (Parent) dialog;
    }

    private static @NotNull TextField getField(@NotNull Parent root, @NotNull String selector) {
        Node field = root.lookup(selector);

        Assertions.assertNotNull(field, selector);

        return (TextField) field;
    }

    @SuppressWarnings("unchecked")
    private static @NotNull ListView<FriendView> getFriendList(@NotNull Parent dialog) {
        return (ListView<FriendView>) dialog.lookup(FRIEND_LIST_SELECTOR);
    }

    private static @NotNull ListView<?> getList(@NotNull Parent dialog, @NotNull String selector) {
        Node list = dialog.lookup(selector);

        Assertions.assertNotNull(list, selector);

        return (ListView<?>) list;
    }

    /**
     * Lists the conversations of the Whispers panel from the top: each player's name and tag, and after it the unread count if any.
     */
    private static @NotNull List<@NotNull String> listConversations(@NotNull Parent dialog) {
        ListView<?> list = getList(dialog, CONVERSATION_LIST_SELECTOR);
        String[] conversations = new String[list.getItems().size()];

        for (Node node : list.lookupAll(".list-cell")) {
            if (node instanceof ListCell<?> cell && !cell.isEmpty() && cell.isVisible() && cell.getIndex() >= 0) {
                String name = ((Label) cell.lookup(CELL_TITLE_SELECTOR)).getText();
                Node badge = cell.lookup(BADGE_SELECTOR);

                conversations[cell.getIndex()] = badge == null ? name : name + " " + ((Label) badge).getText();
            }
        }

        return Arrays.asList(conversations);
    }

    private static @NotNull String getSelectedConversation(@NotNull Parent dialog) {
        int index = getList(dialog, CONVERSATION_LIST_SELECTOR).getSelectionModel().getSelectedIndex();

        Assertions.assertTrue(index >= 0, "a conversation is selected");

        return listConversations(dialog).get(index);
    }

    private static void selectConversation(@NotNull LauncherShell shell, @NotNull String taggedName) {
        Parent dialog = getDialog(shell);
        List<String> conversations = listConversations(dialog);
        int index = 0;

        while (!conversations.get(index).startsWith(taggedName)) {
            index++;
        }

        getList(dialog, CONVERSATION_LIST_SELECTOR).getSelectionModel().select(index);
        lay(shell);
    }

    private static void lay(@NotNull LauncherShell shell) {
        shell.getRoot().applyCss();
        shell.getRoot().layout();
    }

    private static @NotNull List<@NotNull String> listMenuEntries(@NotNull LauncherShell shell) {
        return find(getMenu(shell), Button.class).stream().map(Button::getText).toList();
    }

    /**
     * Builds the shell of a logged-in account with its account menu, the way the window does.
     */
    private static @NotNull LauncherShell createShell(@NotNull LauncherTexts texts, @NotNull UiLanguage language, double scale,
                                                      @NotNull Path directory, @NotNull String taggedName, @NotNull LauncherSessionView session,
                                                      @NotNull AccountMenu.Actions actions) {
        AtomicReference<AccountMenu> menu = new AtomicReference<>();
        LauncherShell shell = new LauncherShell(texts, InstallLayout.of(directory), scale, section -> {
        }, () -> {
        }, () -> menu.get().open());
        Scene scene = new Scene(shell.getRoot(), SCENE_WIDTH, SCENE_HEIGHT);
        Function<String, String> modNames = modId -> modId.equals(GAME_MOD_ID) ? GAME_NAME : modId;

        menu.set(new AccountMenu(shell, texts, taggedName, session, modNames, actions));
        session.setOnChanged(() -> menu.get().update());
        ThemeUtils.apply(scene, language);
        shell.show(LauncherShell.Section.MODS, new Region());
        menu.get().update();
        lay(shell);

        return shell;
    }

    private static void openMenu(@NotNull LauncherShell shell) {
        ((Button) shell.getRoot().lookup(ACCOUNT_SELECTOR)).fire();
        lay(shell);
    }

    private static void openEntry(@NotNull LauncherShell shell, @NotNull String entry) {
        openMenu(shell);

        Button found = find(getMenu(shell), Button.class).stream().filter(button -> button.getText().startsWith(entry)).findFirst().orElseThrow();

        found.fire();
        lay(shell);
    }

    private static void addFriend(@NotNull LauncherShell shell, @NotNull LauncherTexts texts, @NotNull String code) {
        Parent friends = getDialog(shell);

        getField(friends, FRIEND_CODE_INPUT_SELECTOR).setText(code);
        LauncherFxScenario.findButton(friends, texts.get("session.friends.add")).fire();
        lay(shell);
    }

    private static @NotNull UiLanguage selectLanguage(@NotNull String languageCode) {
        return languageCode.equals(UiLanguage.KOREAN.getCode()) ? UiLanguage.KOREAN : UiLanguage.ENGLISH;
    }

    @ParameterizedTest
    @CsvSource({"1.0, en", "2.0, en", "1.0, ko", "2.0, ko"})
    void account_menu_shows_friends_whispers_status_and_sign_out_of_a_connected_session(double scale, @NotNull String languageCode,
                                                                                        @TempDir @NotNull Path directory) throws Exception {
        UiLanguage language = selectLanguage(languageCode);
        LauncherTexts texts = LauncherTexts.of(language);
        String suffix = Math.round(scale * PERCENT_SCALE) + "-" + language.getCode();
        RecordingSession session = new RecordingSession(new SelfView(SessionState.CONNECTED, SelfStatus.ONLINE, "", 2, null));
        RecordingActions actions = new RecordingActions();

        LauncherFxScenario.run(() -> {
            LauncherShell shell = createShell(texts, language, scale, directory, TAGGED_NAME, session, actions);

            // The menu: its five entries under the account, the unread whispers of all players counted, the account's two devices.
            openMenu(shell);

            Parent menu = getMenu(shell);

            Assertions.assertEquals(List.of(texts.get("session.menu.friends"), texts.format("session.menu.whispers.new", 4),
                                            texts.get("session.menu.status"), texts.get("session.menu.sign.out"), texts.get("session.menu.log.out")),
                                    listMenuEntries(shell));
            Assertions.assertTrue(showsText(menu, TAGGED_NAME));
            Assertions.assertTrue(showsText(menu, texts.format("session.devices", 2)));
            Assertions.assertFalse(LauncherFxScenario.findButton(menu, texts.get("session.menu.sign.out")).isDisabled());
            LauncherFxScenario.saveSample(shell.getRoot(), scale, "menu-" + suffix);

            // Whispers from the menu: one conversation per player, friends first, each newest first; the newest one with unread whispers
            // is open and read, and the others keep their unread counts.
            LauncherFxScenario.findButton(menu, texts.format("session.menu.whispers.new", 4)).fire();
            lay(shell);

            Parent unread = getDialog(shell);

            Assertions.assertEquals(List.of("Zeratul#d4e5f6 2", "Kerrigan#a1b2c3 1", "Tassadar#998877"), listConversations(unread));
            Assertions.assertEquals("Tassadar#998877", getSelectedConversation(unread));
            Assertions.assertTrue(showsText(unread, texts.format("session.whisper.to", "Tassadar#998877")));
            Assertions.assertTrue(showsText(unread, "Good game"));
            Assertions.assertFalse(showsText(unread, "Ready for a game?"), "a conversation shows its own player's whispers only");
            Assertions.assertFalse(showsText(unread, "Need a hand?"), "a conversation shows its own player's whispers only");
            LauncherFxScenario.saveSample(shell.getRoot(), scale, "whispers-unread-" + suffix);
            LauncherFxScenario.findButton(unread, texts.get("session.close")).fire();
            openMenu(shell);
            menu = getMenu(shell);

            Assertions.assertTrue(listMenuEntries(shell).contains(texts.format("session.menu.whispers.new", 3)), "the menu counts the rest");

            // Friends: every friend with its state, mod name, devices and ignore mark; the own friend code, copied as it is.
            LauncherFxScenario.findButton(menu, texts.get("session.menu.friends")).fire();
            lay(shell);

            Assertions.assertNull(shell.getRoot().lookup(MENU_SELECTOR), "an entry closes the menu");

            Parent friends = getDialog(shell);

            for (FriendView friend : FRIENDS) {
                Assertions.assertTrue(showsText(friends, friend.getTaggedName()), friend.getTaggedName());
            }

            Assertions.assertTrue(showsText(friends, texts.get("session.friend.in_game")));
            Assertions.assertTrue(showsText(friends, GAME_NAME));
            Assertions.assertTrue(showsText(friends, texts.format("session.friend.devices", 2)));
            Assertions.assertTrue(showsText(friends, "brb"));
            Assertions.assertTrue(showsText(friends, texts.get("session.friend.ignored")));
            Assertions.assertFalse(showsText(friends, texts.get("session.not.connected")));
            Assertions.assertEquals(OWN_CODE.getGroupedText(), ((Label) friends.lookup(FRIEND_CODE_SELECTOR)).getText());
            Assertions.assertTrue(LauncherFxScenario.findButton(friends, texts.get("session.friends.whisper")).isDisabled(), "no friend is selected");
            LauncherFxScenario.saveSample(shell.getRoot(), scale, "friends-" + suffix);

            LauncherFxScenario.findButton(friends, texts.get("session.friends.copy")).fire();

            Assertions.assertEquals(List.of(OWN_CODE.getText()), actions.copied);

            // Add friend: a text that is no code, the own code and a known friend are refused; a new code is added.
            addFriend(shell, texts, "Kerrigan#a1b2c3");

            Assertions.assertTrue(showsText(friends, texts.get("session.friends.invalid")));

            addFriend(shell, texts, OWN_CODE.getGroupedText());

            Assertions.assertTrue(showsText(friends, texts.get("session.friends.own")));

            addFriend(shell, texts, KERRIGAN.getText().toLowerCase(Locale.ROOT));

            Assertions.assertTrue(showsText(friends, texts.get("session.friends.known")));
            LauncherFxScenario.saveSample(shell.getRoot(), scale, "friends-add-refused-" + suffix);
            Assertions.assertTrue(session.commands.isEmpty(), "nothing refused reaches the session");

            addFriend(shell, texts, NEW_FRIEND.getGroupedText());

            Assertions.assertEquals(List.of("add " + NEW_FRIEND.getText()), session.commands);
            Assertions.assertTrue(getField(friends, FRIEND_CODE_INPUT_SELECTOR).getText().isEmpty(), "an added code leaves the field");

            // The selected friend: ignore, remove.
            getFriendList(friends).getSelectionModel().select(0);
            lay(shell);
            LauncherFxScenario.findButton(friends, texts.get("session.friends.ignore")).fire();
            LauncherFxScenario.findButton(friends, texts.get("session.friends.remove")).fire();
            getFriendList(friends).getSelectionModel().select(2);
            lay(shell);
            LauncherFxScenario.findButton(friends, texts.get("session.friends.unignore")).fire();

            Assertions.assertEquals(List.of("add " + NEW_FRIEND.getText(), "ignore " + KERRIGAN.getText(), "remove " + KERRIGAN.getText(),
                                            "unignore " + ARTANIS.getText()), session.commands);

            // Whisper to the selected friend: the Whispers panel opens on her conversation, which shows her whispers only and is read.
            session.commands.clear();
            getFriendList(friends).getSelectionModel().select(0);
            lay(shell);
            LauncherFxScenario.findButton(friends, texts.get("session.friends.whisper")).fire();
            lay(shell);

            Parent whispers = getDialog(shell);

            Assertions.assertEquals(List.of("Zeratul#d4e5f6 2", "Kerrigan#a1b2c3", "Tassadar#998877"), listConversations(whispers));
            Assertions.assertEquals("Kerrigan#a1b2c3", getSelectedConversation(whispers));
            Assertions.assertTrue(showsText(whispers, texts.format("session.whisper.to", "Kerrigan#a1b2c3")));
            Assertions.assertTrue(showsText(whispers, "Ready for a game?"));
            Assertions.assertTrue(showsText(whispers, texts.format("session.whisper.from", "Kerrigan#a1b2c3")));
            Assertions.assertFalse(showsText(whispers, "Are you there?"), "Zeratul's whispers stay in his conversation");
            Assertions.assertFalse(showsText(getList(whispers, WHISPER_LIST_SELECTOR), "Zeratul#d4e5f6"), "her conversation names no other player");

            getField(whispers, WHISPER_INPUT_SELECTOR).setText(TOO_LONG_WHISPER);
            LauncherFxScenario.findButton(whispers, texts.get("session.whispers.send")).fire();
            lay(shell);

            Assertions.assertTrue(showsText(whispers, texts.get("session.whispers.too.long")));

            // Selecting a whisper leaves the recipient as it is.
            getList(whispers, WHISPER_LIST_SELECTOR).getSelectionModel().select(0);
            getField(whispers, WHISPER_INPUT_SELECTOR).setText("gg");
            lay(shell);
            LauncherFxScenario.saveSample(shell.getRoot(), scale, "whispers-" + suffix);
            LauncherFxScenario.findButton(whispers, texts.get("session.whispers.send")).fire();

            Assertions.assertEquals(List.of("whisper " + KERRIGAN.getText() + ":gg"), session.commands);
            Assertions.assertTrue(getField(whispers, WHISPER_INPUT_SELECTOR).getText().isEmpty(), "a sent whisper leaves the field");

            // Zeratul's conversation: his whispers both ways only, read once shown; the next whisper goes to him alone.
            selectConversation(shell, "Zeratul#d4e5f6");

            Assertions.assertEquals(List.of("Zeratul#d4e5f6", "Kerrigan#a1b2c3", "Tassadar#998877"), listConversations(whispers));
            Assertions.assertTrue(showsText(whispers, texts.format("session.whisper.to", "Zeratul#d4e5f6")));
            Assertions.assertTrue(showsText(whispers, "Need a hand?"));
            Assertions.assertTrue(showsText(whispers, texts.get("session.whisper.not.delivered")));
            Assertions.assertFalse(showsText(whispers, "Ready for a game?"), "Kerrigan's whispers stay in her conversation");

            getList(whispers, WHISPER_LIST_SELECTOR).getSelectionModel().select(0);
            getField(whispers, WHISPER_INPUT_SELECTOR).setText("on my way");
            lay(shell);
            LauncherFxScenario.saveSample(shell.getRoot(), scale, "whispers-other-" + suffix);
            LauncherFxScenario.findButton(whispers, texts.get("session.whispers.send")).fire();

            Assertions.assertEquals(List.of("whisper " + KERRIGAN.getText() + ":gg", "whisper " + ZERATUL.getText() + ":on my way"), session.commands);

            LauncherFxScenario.findButton(whispers, texts.get("session.close")).fire();

            Assertions.assertNull(shell.getRoot().lookup(DIALOG_SELECTOR));

            // Whisper to a friend without whispers: a new empty conversation on top.
            session.commands.clear();
            openEntry(shell, texts.get("session.menu.friends"));
            getFriendList(getDialog(shell)).getSelectionModel().select(2);
            lay(shell);
            LauncherFxScenario.findButton(getDialog(shell), texts.get("session.friends.whisper")).fire();
            lay(shell);
            whispers = getDialog(shell);

            Assertions.assertEquals(List.of("Artanis#0a0b0c", "Zeratul#d4e5f6", "Kerrigan#a1b2c3", "Tassadar#998877"), listConversations(whispers));
            Assertions.assertEquals("Artanis#0a0b0c", getSelectedConversation(whispers));
            Assertions.assertTrue(showsText(whispers, texts.get("session.whispers.empty")));

            getField(whispers, WHISPER_INPUT_SELECTOR).setText("hello");
            LauncherFxScenario.findButton(whispers, texts.get("session.whispers.send")).fire();

            Assertions.assertEquals(List.of("whisper " + ARTANIS.getText() + ":hello"), session.commands);

            LauncherFxScenario.findButton(whispers, texts.get("session.close")).fire();

            // The whispers were read: the menu no longer counts them, and its entry opens the newest conversation.
            openEntry(shell, texts.get("session.menu.whispers"));

            Assertions.assertEquals("Tassadar#998877", getSelectedConversation(getDialog(shell)));

            LauncherFxScenario.findButton(getDialog(shell), texts.get("session.close")).fire();
            openMenu(shell);

            Assertions.assertTrue(listMenuEntries(shell).contains(texts.get("session.menu.whispers")));

            // My status: Away with a text; a text that is too long is refused.
            session.commands.clear();
            LauncherFxScenario.findButton(getMenu(shell), texts.get("session.menu.status")).fire();
            lay(shell);

            Parent status = getDialog(shell);
            TextField awayText = find(status, TextField.class).getFirst();

            Assertions.assertTrue(awayText.isDisabled(), "the away text goes with Away only");

            find(status, ToggleButton.class).stream().filter(toggle -> toggle.getText().equals(texts.get("session.status.away"))).findFirst()
                                            .orElseThrow().fire();
            awayText.setText(TOO_LONG_AWAY_TEXT);
            LauncherFxScenario.findButton(status, texts.get("session.save")).fire();
            lay(shell);

            Assertions.assertTrue(showsText(status, texts.get("session.status.too.long")));
            Assertions.assertTrue(session.commands.isEmpty());

            awayText.setText(AWAY_TEXT);
            lay(shell);
            LauncherFxScenario.saveSample(shell.getRoot(), scale, "status-" + suffix);
            LauncherFxScenario.findButton(status, texts.get("session.save")).fire();
            lay(shell);

            Assertions.assertEquals(List.of("status " + SelfStatus.AWAY + ":" + AWAY_TEXT), session.commands);
            Assertions.assertNull(shell.getRoot().lookup(DIALOG_SELECTOR));
            Assertions.assertNotNull(shell.getRoot().lookup(ACCOUNT_SELECTOR).lookup("." + WARNING_MARKER), "the account button shows Away");

            // Sign out everywhere else: one confirmation, then the session.
            session.commands.clear();
            openMenu(shell);

            Assertions.assertTrue(showsText(getMenu(shell), AWAY_TEXT), "the menu shows the away text");

            LauncherFxScenario.findButton(getMenu(shell), texts.get("session.menu.sign.out")).fire();
            lay(shell);
            LauncherFxScenario.saveSample(shell.getRoot(), scale, "sign-out-" + suffix);

            Assertions.assertTrue(session.commands.isEmpty());

            LauncherFxScenario.findButton(getDialog(shell), texts.get("session.sign.out")).fire();

            Assertions.assertEquals(List.of("sign out"), session.commands);
            Assertions.assertNull(shell.getRoot().lookup(DIALOG_SELECTOR));

            // Log out: the menu closes and the window logs out.
            openMenu(shell);
            LauncherFxScenario.findButton(getMenu(shell), texts.get("session.menu.log.out")).fire();

            Assertions.assertEquals(1, actions.logOuts.get());
            Assertions.assertNull(shell.getRoot().lookup(MENU_SELECTOR));

            return null;
        });
    }

    @ParameterizedTest
    @CsvSource({"1.0, en", "2.0, en", "1.0, ko", "2.0, ko"})
    void account_menu_shows_why_the_session_is_offline(double scale, @NotNull String languageCode, @TempDir @NotNull Path directory) throws Exception {
        UiLanguage language = selectLanguage(languageCode);
        LauncherTexts texts = LauncherTexts.of(language);
        String suffix = Math.round(scale * PERCENT_SCALE) + "-" + language.getCode();
        RecordingSession session = new RecordingSession(new SelfView(SessionState.OFFLINE, SelfStatus.ONLINE, "", 0, SessionFailure.SIGNED_OUT));

        LauncherFxScenario.run(() -> {
            LauncherShell shell = createShell(texts, language, scale, directory, TAGGED_NAME, session, new RecordingActions());

            openMenu(shell);

            Assertions.assertTrue(showsText(getMenu(shell), texts.get("session.failure.signed_out")));
            Assertions.assertTrue(LauncherFxScenario.findButton(getMenu(shell), texts.get("session.menu.sign.out")).isDisabled(),
                                  "nothing to sign out without a connection");
            LauncherFxScenario.saveSample(shell.getRoot(), scale, "menu-offline-" + suffix);

            LauncherFxScenario.findButton(getMenu(shell), texts.get("session.menu.friends")).fire();
            lay(shell);

            Assertions.assertTrue(showsText(getDialog(shell), texts.get("session.not.connected")));
            Assertions.assertTrue(showsText(getDialog(shell), texts.get("session.friends.empty")));
            Assertions.assertEquals(OWN_CODE.getGroupedText(), ((Label) getDialog(shell).lookup(FRIEND_CODE_SELECTOR)).getText(),
                                    "the own code is known offline");
            LauncherFxScenario.saveSample(shell.getRoot(), scale, "friends-offline-" + suffix);
            LauncherFxScenario.findButton(getDialog(shell), texts.get("session.close")).fire();

            openEntry(shell, texts.get("session.menu.whispers"));

            Assertions.assertTrue(showsText(getDialog(shell), texts.get("session.not.connected")));
            Assertions.assertTrue(showsText(getDialog(shell), texts.get("session.whispers.empty")));
            Assertions.assertTrue(LauncherFxScenario.findButton(getDialog(shell), texts.get("session.whispers.send")).isDisabled(),
                                  "nothing to send without a connection");
            LauncherFxScenario.saveSample(shell.getRoot(), scale, "whispers-offline-" + suffix);
            LauncherFxScenario.findButton(getDialog(shell), texts.get("session.close")).fire();

            openEntry(shell, texts.get("session.menu.status"));

            Assertions.assertTrue(showsText(getDialog(shell), texts.get("session.not.connected")));
            LauncherFxScenario.saveSample(shell.getRoot(), scale, "status-offline-" + suffix);

            find(getDialog(shell), ToggleButton.class).stream().filter(toggle -> toggle.getText().equals(texts.get("session.status.away"))).findFirst()
                                                      .orElseThrow().fire();
            LauncherFxScenario.findButton(getDialog(shell), texts.get("session.save")).fire();

            Assertions.assertEquals(SelfStatus.AWAY, session.getSelf().getStatus(), "the status is kept for the session");

            return null;
        });
    }

    @ParameterizedTest
    @CsvSource({"1.0, en", "2.0, en", "1.0, ko", "2.0, ko"})
    void account_menu_shows_the_real_sessions_of_three_accounts_at_a_node(double scale, @NotNull String languageCode,
                                                                          @TempDir @NotNull Path directory) throws Exception {
        UiLanguage language = selectLanguage(languageCode);
        LauncherTexts texts = LauncherTexts.of(language);
        String suffix = Math.round(scale * PERCENT_SCALE) + "-" + language.getCode();

        try (LauncherSessionScenario scenario = new LauncherSessionScenario(directory.resolve("node"))) {
            Account kerrigan = LauncherSessionScenario.createAccount(directory.resolve("kerrigan"), "Kerrigan");
            Account raynor = LauncherSessionScenario.createAccount(directory.resolve("raynor"), "Raynor");
            Account zeratul = LauncherSessionScenario.createAccount(directory.resolve("zeratul"), "Zeratul");
            LauncherSession kerriganSession = scenario.startSession(kerrigan, Runnable::run);
            LauncherSession raynorSession = scenario.startSession(raynor, Platform::runLater);
            LauncherSession zeratulSession = scenario.startSession(zeratul, Runnable::run);
            FriendCode kerriganCode = kerriganSession.getFriendCode();
            FriendCode raynorCode = raynorSession.getFriendCode();
            FriendCode zeratulCode = zeratulSession.getFriendCode();

            try {
                LauncherShell shell = LauncherFxScenario.run(() -> createShell(texts, language, scale, directory, raynor.getTaggedName(), raynorSession,
                                                                               new RecordingActions()));

                LauncherSessionScenario.await(() -> kerriganSession.getSelf().getState() == SessionState.CONNECTED &&
                                                    raynorSession.getSelf().getState() == SessionState.CONNECTED &&
                                                    zeratulSession.getSelf().getState() == SessionState.CONNECTED, "the three launchers connect");
                kerriganSession.addFriend(raynorCode);
                zeratulSession.addFriend(raynorCode);

                // Raynor adds Kerrigan and Zeratul in the Friends panel by their friend codes; Kerrigan shows in the launcher.
                LauncherFxScenario.run(() -> {
                    openEntry(shell, texts.get("session.menu.friends"));
                    addFriend(shell, texts, kerriganCode.getGroupedText());
                    addFriend(shell, texts, zeratulCode.getGroupedText());

                    return null;
                });
                LauncherFxScenario.await(() -> {
                    lay(shell);

                    return showsText(getDialog(shell), kerrigan.getTaggedName()) &&
                           showsText(getDialog(shell), texts.get("session.friend.in_launcher"));
                }, "Raynor's panel shows Kerrigan in the launcher");
                LauncherFxScenario.run(() -> {
                    LauncherFxScenario.saveSample(shell.getRoot(), scale, "friends-live-" + suffix);

                    return null;
                });

                // Kerrigan goes away with a text.
                kerriganSession.setStatus(SelfStatus.AWAY, AWAY_TEXT);
                LauncherFxScenario.await(() -> {
                    lay(shell);

                    return showsText(getDialog(shell), AWAY_TEXT);
                }, "Raynor's panel shows Kerrigan away");
                LauncherFxScenario.run(() -> {
                    LauncherFxScenario.saveSample(shell.getRoot(), scale, "friends-away-live-" + suffix);
                    LauncherFxScenario.findButton(getDialog(shell), texts.get("session.close")).fire();

                    return null;
                });

                // Kerrigan and then Zeratul whisper: the menu counts both, the Whispers panel opens on Zeratul's conversation with his whisper
                // only, and Kerrigan's conversation keeps hers unread.
                LauncherSessionScenario.await(() -> kerriganSession.getFriends().stream()
                                                                   .anyMatch(friend -> friend.getState() == FriendState.IN_LAUNCHER),
                                              "Kerrigan sees Raynor");
                LauncherSessionScenario.await(() -> zeratulSession.getFriends().stream()
                                                                  .anyMatch(friend -> friend.getState() == FriendState.IN_LAUNCHER),
                                              "Zeratul sees Raynor");
                kerriganSession.whisper(raynorCode, "Hi Raynor");
                LauncherFxScenario.await(() -> raynorSession.getWhispers().stream().anyMatch(WhisperView::isIncoming),
                                         "Raynor gets Kerrigan's whisper");
                zeratulSession.whisper(raynorCode, "En taro Adun");
                LauncherFxScenario.await(() -> raynorSession.getWhispers().stream().filter(WhisperView::isIncoming).count() == 2,
                                         "Raynor gets Zeratul's whisper");
                LauncherFxScenario.run(() -> {
                    openMenu(shell);

                    Assertions.assertTrue(listMenuEntries(shell).contains(texts.format("session.menu.whispers.new", 2)));

                    LauncherFxScenario.saveSample(shell.getRoot(), scale, "menu-live-" + suffix);
                    openEntry(shell, texts.get("session.menu.whispers"));

                    Parent whispers = getDialog(shell);

                    Assertions.assertEquals(List.of(zeratul.getTaggedName(), kerrigan.getTaggedName() + " 1"), listConversations(whispers));
                    Assertions.assertEquals(zeratul.getTaggedName(), getSelectedConversation(whispers));
                    Assertions.assertTrue(showsText(whispers, "En taro Adun"));
                    Assertions.assertFalse(showsText(whispers, "Hi Raynor"), "Kerrigan's whisper stays in her conversation");

                    // Raynor answers in Kerrigan's conversation; the answer reaches her alone.
                    selectConversation(shell, kerrigan.getTaggedName());

                    Assertions.assertEquals(List.of(zeratul.getTaggedName(), kerrigan.getTaggedName()), listConversations(whispers));
                    Assertions.assertTrue(showsText(whispers, "Hi Raynor"));
                    Assertions.assertFalse(showsText(whispers, "En taro Adun"), "Zeratul's whisper stays in his conversation");
                    Assertions.assertTrue(showsText(whispers, texts.format("session.whisper.to", kerrigan.getTaggedName())), "the answer goes back");

                    getField(whispers, WHISPER_INPUT_SELECTOR).setText("Hi Kerrigan");
                    LauncherFxScenario.findButton(whispers, texts.get("session.whispers.send")).fire();

                    return null;
                });
                LauncherSessionScenario.await(() -> kerriganSession.getWhispers().stream().anyMatch(whisper -> whisper.isIncoming() &&
                                                                                                               whisper.getText().equals("Hi Kerrigan")),
                                              "Kerrigan gets the answer");
                LauncherFxScenario.await(() -> raynorSession.getWhispers().stream().anyMatch(whisper -> whisper.getState() == WhisperState.DELIVERED),
                                         "Raynor's answer is delivered");
                LauncherFxScenario.run(() -> {
                    lay(shell);

                    Assertions.assertTrue(showsText(getDialog(shell), "Hi Kerrigan"));
                    Assertions.assertEquals(List.of(kerrigan.getTaggedName(), zeratul.getTaggedName()), listConversations(getDialog(shell)),
                                            "the answer moves her conversation to the top");
                    Assertions.assertTrue(zeratulSession.getWhispers().stream().noneMatch(WhisperView::isIncoming), "Zeratul gets no answer");

                    LauncherFxScenario.saveSample(shell.getRoot(), scale, "whispers-live-" + suffix);
                    LauncherFxScenario.findButton(getDialog(shell), texts.get("session.close")).fire();
                    openMenu(shell);

                    Assertions.assertTrue(listMenuEntries(shell).contains(texts.get("session.menu.whispers")), "both conversations were read");

                    return null;
                });
            } finally {
                raynorSession.close();
                zeratulSession.close();
                kerriganSession.close();
            }
        }
    }
}
