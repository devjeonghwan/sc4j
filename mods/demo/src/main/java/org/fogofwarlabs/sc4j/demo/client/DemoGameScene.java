package org.fogofwarlabs.sc4j.demo.client;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.base.exception.IgnoreException;
import org.fogofwarlabs.sc4j.base.log.Logger;
import org.fogofwarlabs.sc4j.demo.game.DemoGames;
import org.fogofwarlabs.sc4j.demo.net.DemoNetworkGame;
import org.fogofwarlabs.sc4j.demo.order.DemoOrders;
import org.fogofwarlabs.sc4j.engine.client.app.ClientScene;
import org.fogofwarlabs.sc4j.engine.client.app.SceneClient;
import org.fogofwarlabs.sc4j.engine.client.command.RallyAction;
import org.fogofwarlabs.sc4j.engine.client.console.ConsoleCommand;
import org.fogofwarlabs.sc4j.engine.client.game.GameSession;
import org.fogofwarlabs.sc4j.engine.client.game.GameTimeUtils;
import org.fogofwarlabs.sc4j.engine.client.hud.OrderButton;
import org.fogofwarlabs.sc4j.engine.client.hud.StandardCommandCardSource;
import org.fogofwarlabs.sc4j.engine.client.hud.WaitingDialog;
import org.fogofwarlabs.sc4j.engine.client.input.InputEvent;
import org.fogofwarlabs.sc4j.engine.client.input.Key;
import org.fogofwarlabs.sc4j.engine.client.render.Canvas;
import org.fogofwarlabs.sc4j.engine.content.definition.UnitTrait;
import org.fogofwarlabs.sc4j.engine.content.faction.Faction;
import org.fogofwarlabs.sc4j.engine.game.Game;
import org.fogofwarlabs.sc4j.engine.game.GameSetup;
import org.fogofwarlabs.sc4j.engine.lockstep.LockstepPeer;
import org.fogofwarlabs.sc4j.engine.map.MapModel;
import org.fogofwarlabs.sc4j.engine.net.lobby.LobbyMember;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.fogofwarlabs.sc4j.engine.replay.EngineReplayFormat;
import org.fogofwarlabs.sc4j.engine.replay.ReplayRecorder;
import org.fogofwarlabs.sc4j.engine.trigger.PlayerOutcome;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Plays a demo game: the person against the computer in a local session, or a network game ({@link DemoNetworkGame}) of the people at two machines,
 * shown on the {@link DemoGameScreen} with the engine command card of the selection ({@link StandardCommandCardSource}: move, stop, attack
 * (attack-move on the ground or the minimap), patrol, hold position, gather, what the requirements let the selected worker build or building train,
 * rally for a producer, and cancel) with the queue panel, whose slots cancel on a press, and the demo's right press ({@link DemoSmartAction}). The
 * game runs at {@value #LOOP_MILLISECONDS} milliseconds per game loop times the game speed, a local game starting at the speed of the options
 * ({@link DemoOptions#getGameSpeed}); a network game runs a loop only once the other machine's turn for it arrived, and while it waits for an overdue
 * machine the screen's waiting dialog names the room's members ({@link WaitingDialog}). The person's notifications play and show after each update
 * ({@link DemoNotifications}). The replay is recorded through the engine replay format and stored in the replay folder of the mod storage when the
 * game ends or is left, for a network game on every machine with the same contents; then the outcome screen shows.
 * <p>
 * Escape that cancels nothing, or F10, opens the game menu ({@value #MENU_DIALOG}) over the game, which pauses a local game while it is
 * open: Resume closes it, Options opens the options over it ({@link OptionsPopup}; a changed scroll speed applies to the game at once),
 * Leave game stores the replay and returns to the main menu.
 * <p>
 * Slash commands, besides the camera commands: {@code /speed} (game speed) and {@code /pause}, {@code /resume} in a local game, and
 * {@code /leave} (store the replay and return to the main menu); {@code /help} lists them all.
 */
public final class DemoGameScene implements ClientScene {
    /**
     * The client time of one game loop at speed 1, in milliseconds.
     */
    public static final int LOOP_MILLISECONDS = 42;
    /**
     * The game loops between two state hash checkpoints of the recorded replay.
     */
    public static final int CHECKPOINT_INTERVAL = 100;
    /**
     * The most game loops one update runs; a client that falls further behind drops the rest.
     */
    public static final int MAXIMUM_LOOPS_PER_UPDATE = 64;
    /**
     * The game speeds {@code /speed} takes.
     */
    public static final @NotNull List<@NotNull Integer> SPEEDS = List.of(1, 2, 4, 8);
    /**
     * The pack id of the game menu's dialog.
     */
    public static final @NotNull String MENU_DIALOG = "game-menu";
    /**
     * The id of the game menu's resume button, which Escape activates.
     */
    public static final int RESUME_ID = 10;
    /**
     * The id of the game menu's leave button.
     */
    public static final int LEAVE_ID = 11;
    /**
     * The id of the game menu's options button.
     */
    public static final int OPTIONS_ID = 12;

    private static final @NotNull Logger LOGGER = Logger.of(DemoGameScene.class);
    private static final int CARD_CELLS = 9;
    private static final @NotNull String REPLAY_PREFIX = "game-";

    /**
     * Finds the name of the room member a participant is; a participant without member is named by its number.
     */
    private static @NotNull String findMemberName(@NotNull List<@NotNull LobbyMember> members, @NotNull LockstepPeer peer) {
        for (LobbyMember member : members) {
            if (member.getPeer().getNumber() == peer.getNumber()) {
                return member.getName();
            }
        }

        return peer.toString();
    }


    private final @NotNull DemoClient demo;
    private final @NotNull MainMenuScene menu;
    private final @NotNull GameSetup setup;
    private final @Nullable DemoNetworkGame network;

    private @Nullable Game game;
    private @Nullable GameSession session;
    private @Nullable ReplayRecorder recorder;
    private @Nullable DemoGameScreen screen;
    private @Nullable DemoNotifications notifications;

    private long lastTime;
    private long pendingMilliseconds;
    private int speed;
    private boolean paused;
    private boolean ended;

    /**
     * Creates a {@link DemoGameScene} instance; the game starts when the scene opens.
     *
     * @param demo  the demo's shared services
     * @param menu  the main menu the game returns to
     * @param setup the game's setup, the person in the first slot
     */
    public DemoGameScene(@NotNull DemoClient demo, @NotNull MainMenuScene menu, @NotNull GameSetup setup) {
        this(demo, menu, setup, null);
    }

    /**
     * Creates a {@link DemoGameScene} instance of a network game; the game's first loop runs when the scene opens. The scene closes the
     * network game.
     *
     * @param demo    the demo's shared services
     * @param menu    the main menu the game returns to
     * @param network the network game of this machine
     */
    public DemoGameScene(@NotNull DemoClient demo, @NotNull MainMenuScene menu, @NotNull DemoNetworkGame network) {
        this(demo, menu, network.getStart().getSetup(), network);
    }

    private DemoGameScene(@NotNull DemoClient demo, @NotNull MainMenuScene menu, @NotNull GameSetup setup, @Nullable DemoNetworkGame network) {
        this.demo    = demo;
        this.menu    = menu;
        this.setup   = setup;
        this.network = network;

        this.game          = null;
        this.session       = null;
        this.recorder      = null;
        this.screen        = null;
        this.notifications = null;

        this.lastTime            = -1;
        this.pendingMilliseconds = 0;
        this.speed               = 1;
        this.paused              = false;
        this.ended               = false;
    }

    /**
     * @return the demo's shared services
     */
    public @NotNull DemoClient getDemo() {
        return this.demo;
    }

    /**
     * @return the game, or {@code null} before the scene opens
     */
    public @Nullable Game getGame() {
        return this.game;
    }

    /**
     * @return the screen of the game, or {@code null} before the scene opens
     */
    public @Nullable DemoGameScreen getScreen() {
        return this.screen;
    }

    /**
     * @return the network game, or {@code null} for a local game
     */
    public @Nullable DemoNetworkGame getNetwork() {
        return this.network;
    }

    /**
     * @return the game speed: game loops per {@value #LOOP_MILLISECONDS} milliseconds
     */
    public int getSpeed() {
        return this.speed;
    }

    /**
     * Opens the scene: loads what it shows. Called once, before the first {@link #update}.
     * <p>
     * Creates the game and its local session with the replay recorder (a network game brings its own), and the screen with its command card
     * and commands, and plays the game music.
     *
     * @param context the client's shared services
     * @throws IOException if the map's tileset cannot be read
     */
    @Override
    public void open(@NotNull SceneClient context) throws IOException {
        DemoOrders orders = this.demo.getContent().getOrders();
        DemoNetworkGame networkGame = this.network;
        MapModel map = this.demo.getMaps().require(this.setup);
        Game created;
        Player person;
        GameSession session;
        ReplayRecorder startedRecorder;

        if (networkGame != null) {
            created         = networkGame.getGame();
            person          = networkGame.getLocalPlayer();
            session         = networkGame.getSession();
            startedRecorder = networkGame.getRecorder();
        } else {
            created         = this.demo.getGames().createGame(this.setup);
            person          = created.getPlayer(DemoGames.FIRST_SLOT);
            session         = DemoClient.createSessionFactory(this.demo.getContent(), created, map).createLocal(created, person, List.of());
            startedRecorder = this.demo.getReplayFormat().createRecorder(this.setup, CHECKPOINT_INTERVAL);

            session.getCommandSource().setRecorder(startedRecorder);
        }

        DemoGameScreen startedScreen = new DemoGameScreen(this.demo, session, map, person);
        List<OrderButton> buttons = List.of(new OrderButton(orders.getMove(), "Move", Key.M, true),
                                            new OrderButton(orders.getStop(), "Stop", Key.S, false),
                                            new OrderButton(orders.getAttack(), orders.getAttackMove(), "Attack", Key.A, true, null),
                                            new OrderButton(orders.getPatrol(), "Patrol", Key.P, true),
                                            new OrderButton(orders.getHoldPosition(), "Hold", Key.H, false),
                                            new OrderButton(orders.getGather(), "Gather", Key.G, true, UnitTrait.WORKER));

        StandardCommandCardSource commandCard = new StandardCommandCardSource(created, orders.getOrderCommand(), orders.getTrainCommand(),
                                                                              orders.getConstructCommand(), orders.getCancelCommand(),
                                                                              orders.getRallyCommand(), buttons, DemoClient::getName, CARD_CELLS);
        RallyAction rally = commandCard.getRallyAction();

        if (rally == null) {
            throw Exceptions.newIllegalStateException("The demo's command card has no rally action.");
        }

        startedScreen.getScreen().setCommandCardSource(commandCard, new DemoSmartAction(created, orders, rally));
        startedScreen.getQueuePanel().setCancelAction(commandCard.getCancelAction());

        WaitingDialog waitingDialog = startedScreen.getScreen().getWaitingDialog();

        if (networkGame != null && waitingDialog != null) {
            List<LobbyMember> members = networkGame.getStart().getMembers();

            waitingDialog.setPeerNames(peer -> findMemberName(members, peer));
        }

        this.registerCommands(startedScreen);
        startedScreen.addMessage(this.demo.describe(this.setup) + ".");

        this.game          = created;
        this.session       = session;
        this.recorder      = startedRecorder;
        this.screen        = startedScreen;
        this.notifications = person == null ? null : new DemoNotifications(this.demo, startedScreen, person);

        if (networkGame == null) {
            this.speed = this.demo.getOptions().getGameSpeed();
        }

        this.demo.playMusic(DemoClient.GAME_MUSIC);
    }

    /**
     * Updates the scene at the supplied time.
     * <p>
     * Runs the game loops the time since the last update allows at the game speed, at most {@value #MAXIMUM_LOOPS_PER_UPDATE} (a network game
     * only those whose turns arrived), notifies the person, and stores the replay and shows the outcome once the game is decided.
     *
     * @param time the client time in milliseconds
     */
    @Override
    public void update(long time) {
        Game current = this.requireGame();
        DemoGameScreen shown = this.requireScreen();
        long elapsed = this.lastTime < 0 ? 0 : Math.max(0, time - this.lastTime);

        this.lastTime = time;

        if (this.ended) {
            DemoNetworkGame networkGame = this.network;

            // A network game keeps answering the other machine until this machine's departure is complete.
            if (networkGame != null && !networkGame.isDepartureComplete()) {
                networkGame.poll();
            }

            return;
        }

        if (!this.paused) {
            this.pendingMilliseconds += elapsed * this.speed;

            GameSession playing = this.requireSession();

            for (int loop = 0; loop < MAXIMUM_LOOPS_PER_UPDATE && this.pendingMilliseconds >= LOOP_MILLISECONDS && !DemoGames.isOver(current); loop++) {
                if (!this.runFrame(playing)) {
                    break;
                }

                this.pendingMilliseconds -= LOOP_MILLISECONDS;
            }

            this.pendingMilliseconds = Math.min(this.pendingMilliseconds, LOOP_MILLISECONDS);
        }

        shown.update(elapsed);

        DemoNotifications currentNotifications = this.notifications;

        if (currentNotifications != null) {
            currentNotifications.update(this.requireSession().getView());
        }

        if (DemoGames.isOver(current)) {
            Player person = this.requireSession().getLocalPlayer();

            this.end(person != null ? person : current.getPlayer(DemoGames.FIRST_SLOT));
        }
    }

    /**
     * Draws the scene: the game screen with the game time, the game speed above normal and the pause in its status line.
     *
     * @param surface the surface
     * @throws IOException if a resource the scene draws cannot be read
     */
    @Override
    public void draw(@NotNull Canvas surface) throws IOException {
        Game current = this.requireGame();
        String status = GameTimeUtils.format(current.getClock().getFrame(), LOOP_MILLISECONDS) + (this.speed > 1 ? "  x" + this.speed : "") +
                        (this.paused ? "  Paused" : "");

        this.requireScreen().draw(surface, status);
    }

    /**
     * Handles an input event the scene's stack hands to it: the screen takes it.
     *
     * @param event the event
     * @return {@code true} if the scene used the event, {@code false} otherwise
     */
    @Override
    public boolean handleInput(@NotNull InputEvent event) {
        if (this.requireScreen().handleInput(event)) {
            return true;
        }

        if (event.getType() != InputEvent.Type.KEY_PRESSED || event.getKey() != Key.ESCAPE && event.getKey() != Key.F10 || this.ended) {
            return false;
        }

        this.openMenu();

        return true;
    }

    /**
     * Opens the game menu over the game; a local game pauses while it is open, unless it was paused already.
     */
    private void openMenu() {
        boolean pausing = this.network == null && !this.paused;

        try {
            DemoPopup.open(this.demo, MENU_DIALOG, (popup, id) -> {
                if (id == OPTIONS_ID) {
                    this.openOptions();

                    return;
                }

                if (id != RESUME_ID && id != LEAVE_ID) {
                    return;
                }

                popup.close();

                if (pausing) {
                    this.paused = false;
                }

                if (id == LEAVE_ID) {
                    this.leave();
                }
            });
        } catch (IOException exception) {
            LOGGER.warning("GAME MENU: {0}", exception.getMessage());

            return;
        }

        if (pausing) {
            this.paused = true;
        }
    }

    /**
     * Opens the options over the game menu; a changed scroll speed applies to the game's camera at once.
     */
    private void openOptions() {
        try {
            OptionsPopup.open(this.demo, () -> this.demo.getOptions().applyTo(this.requireScreen().getScreen().getScrollRate()));
        } catch (IOException exception) {
            LOGGER.warning("OPTIONS: {0}", exception.getMessage());
        }
    }

    private boolean runFrame(@NotNull GameSession playing) {
        DemoNetworkGame networkGame = this.network;

        return networkGame != null ? networkGame.runFrame() : playing.runFrame();
    }

    private void registerCommands(@NotNull DemoGameScreen target) {
        target.getCommands().register(ConsoleCommand.of("/leave", "", "store the replay and return to the main menu")
                                                    .whenEmpty("leave the game", arguments -> this.leave())
                                                    .availability("in a game."));

        if (this.network != null) {
            return;
        }

        target.getCommands().register(ConsoleCommand.of("/speed", "[1|2|4|8]", "show or set the game speed")
                                                    .describe("Sets how many game loops run in the time of one at normal speed.")
                                                    .whenEmpty("show the game speed", arguments -> "Speed x" + this.speed + ".")
                                                    .otherwise("<speed>", "set the game speed", this::answerSpeed)
                                                    .availability("in a local game.")
                                                    .example("/speed 4"));
        target.getCommands().register(ConsoleCommand.of("/pause", "", "pause the game")
                                                    .whenEmpty("pause the game", arguments -> this.answerPause(true))
                                                    .availability("in a local game."));
        target.getCommands().register(ConsoleCommand.of("/resume", "", "resume the paused game")
                                                    .whenEmpty("resume the game", arguments -> this.answerPause(false))
                                                    .availability("in a local game."));
    }

    private @NotNull String answerSpeed(@NotNull String @NotNull [] arguments) {
        int chosen;

        try {
            chosen = arguments.length == 1 ? Integer.parseInt(arguments[0]) : 0;
        } catch (NumberFormatException exception) {
            chosen = 0;
        }

        if (!SPEEDS.contains(chosen)) {
            return "Use /speed with one of " + SPEEDS + ".";
        }

        this.speed = chosen;

        return "Speed x" + this.speed + ".";
    }

    private @NotNull String answerPause(boolean paused) {
        this.paused = paused;

        return paused ? "Paused." : "Resumed.";
    }

    private @NotNull String leave() {
        this.storeReplay();
        this.ended = true;
        this.menu.show();

        return "Left the game.";
    }

    private void end(@NotNull Player person) {
        Game current = this.requireGame();
        PlayerOutcome outcome = DemoGames.getOutcome(current, person);
        Faction faction = this.requireSession().getView().getFaction(person);
        String replay = this.storeReplay();

        this.ended = true;
        this.demo.getClient().getScenes().replace(new OutcomeScene(this.demo, this.menu, outcome, faction, this.demo.describe(this.setup),
                                                                   current.getClock().getFrame(), replay));
    }

    /**
     * Stores the replay recorded so far in the replay folder of the mod storage.
     *
     * @return the replay's file name, or an empty text if it cannot be stored
     */
    private @NotNull String storeReplay() {
        ReplayRecorder current = this.recorder;

        if (current == null || this.ended) {
            return "";
        }

        String name = REPLAY_PREFIX + this.setup.getSeed() + "." + EngineReplayFormat.FILE_EXTENSION;
        Path file = this.demo.getClient().getStorage().resolve(DemoClient.REPLAY_FOLDER + "/" + name);

        try {
            Files.createDirectories(file.getParent());
            Files.write(file, this.demo.getReplayFormat().write(current.createReplay()));
        } catch (IOException exception) {
            LOGGER.warning("REPLAY NOT STORED: {0} ({1})", file, exception.getMessage());

            return "";
        }

        return name;
    }

    private @NotNull Game requireGame() {
        Game current = this.game;

        if (current == null) {
            throw Exceptions.newIllegalStateException("The game scene is not open.");
        }

        return current;
    }

    private @NotNull GameSession requireSession() {
        GameSession current = this.session;

        if (current == null) {
            throw Exceptions.newIllegalStateException("The game scene is not open.");
        }

        return current;
    }

    private @NotNull DemoGameScreen requireScreen() {
        DemoGameScreen current = this.screen;

        if (current == null) {
            throw Exceptions.newIllegalStateException("The game scene is not open.");
        }

        return current;
    }

    /**
     * Closes the scene: releases what it holds, the screen's pictures and a network game, which this machine leaves; it answers the other
     * machine until the departure is complete ({@link DemoNetworkGame#finishDeparture()}) before it closes the game's endpoint.
     */
    @Override
    public void close() {
        DemoGameScreen shown = this.screen;
        DemoNetworkGame networkGame = this.network;

        this.screen = null;

        if (networkGame != null) {
            networkGame.finishDeparture();
            networkGame.close();
        }

        if (shown != null) {
            IgnoreException.execute(
                    shown::close
            );
        }
    }
}
