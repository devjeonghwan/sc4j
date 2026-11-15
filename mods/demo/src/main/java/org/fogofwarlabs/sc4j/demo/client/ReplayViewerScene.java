package org.fogofwarlabs.sc4j.demo.client;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.base.exception.IgnoreException;
import org.fogofwarlabs.sc4j.demo.game.DemoGames;
import org.fogofwarlabs.sc4j.engine.client.app.ClientScene;
import org.fogofwarlabs.sc4j.engine.client.app.SceneClient;
import org.fogofwarlabs.sc4j.engine.client.console.ConsoleCommand;
import org.fogofwarlabs.sc4j.engine.client.game.GameSession;
import org.fogofwarlabs.sc4j.engine.client.game.GameSessionFactory;
import org.fogofwarlabs.sc4j.engine.client.game.GameTimeUtils;
import org.fogofwarlabs.sc4j.engine.client.hud.ReplayPanel;
import org.fogofwarlabs.sc4j.engine.client.input.InputEvent;
import org.fogofwarlabs.sc4j.engine.client.render.Canvas;
import org.fogofwarlabs.sc4j.engine.content.faction.Faction;
import org.fogofwarlabs.sc4j.engine.game.Game;
import org.fogofwarlabs.sc4j.engine.game.PlayerSlot;
import org.fogofwarlabs.sc4j.engine.map.MapModel;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.fogofwarlabs.sc4j.engine.replay.EngineReplay;
import org.fogofwarlabs.sc4j.engine.replay.ReplayController;
import org.fogofwarlabs.sc4j.engine.replay.ReplaySummary;
import org.fogofwarlabs.sc4j.engine.state.snapshot.StateSnapshot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Plays a replay of the engine format back on the {@link DemoGameScreen}: the replay's game in a replay session without local player (every
 * unit shown), at a speed of up to {@value #MAXIMUM_SPEED} times normal, pausable and seekable. A seek forward runs the game loops at once; a
 * seek back restores the nearest state snapshot taken while playing (one every {@value #SNAPSHOT_INTERVAL} game loops) and plays on from
 * there with a new replay controller in a new replay session of the same game (the screen keeps showing the game through its view). The
 * console's queue region holds the engine {@link ReplayPanel}: the game time reached and the replay's game time, the speed, a bar to seek
 * and buttons to pause, change the speed and follow the next player; the top bar shows the followed player's resources and supply, named by
 * its faction. The status line names the checkpoints the playback missed.
 * <p>
 * Slash commands, besides the camera commands: {@code /speed}, {@code /pause}, {@code /resume}, {@code /seek} (a game time) and {@code /leave}
 * (return to the main menu); {@code /help} lists them all.
 */
public final class ReplayViewerScene implements ClientScene, ReplayPanel.Playback {
    /**
     * The game loops between two state snapshots of the playback.
     */
    public static final int SNAPSHOT_INTERVAL = 1000;
    /**
     * The highest playback speed.
     */
    public static final int MAXIMUM_SPEED = 16;
    /**
     * The playback speeds {@code /speed} takes.
     */
    public static final @NotNull List<@NotNull Integer> SPEEDS = List.of(1, 2, 4, 8, MAXIMUM_SPEED);
    /**
     * The width of the replay panel's region in the console in logical units.
     */
    public static final float REPLAY_PANEL_WIDTH = 420;

    private static final @NotNull String STATUS = "Replay";

    private final @NotNull DemoClient demo;
    private final @NotNull MainMenuScene menu;
    private final @NotNull Path file;

    private @Nullable EngineReplay replay;
    private @Nullable Game game;
    private @Nullable GameSessionFactory sessions;
    private @Nullable GameSession session;
    private @Nullable ReplayController controller;
    private @Nullable DemoGameScreen screen;

    private final @NotNull TreeMap<@NotNull Long, @NotNull StateSnapshot> snapshots;
    private final @NotNull List<@NotNull Player> players;
    private int viewedPlayer;

    private long lastTime;
    private long pendingMilliseconds;
    private int speed;
    private boolean paused;

    /**
     * Creates a {@link ReplayViewerScene} instance; it reads the replay when it opens.
     *
     * @param demo the demo's shared services
     * @param menu the main menu the viewer returns to
     * @param file the replay file
     */
    public ReplayViewerScene(@NotNull DemoClient demo, @NotNull MainMenuScene menu, @NotNull Path file) {
        this.demo = demo;
        this.menu = menu;
        this.file = file;

        this.replay     = null;
        this.game       = null;
        this.sessions   = null;
        this.session    = null;
        this.controller = null;
        this.screen     = null;

        this.snapshots    = new TreeMap<>();
        this.players      = new ArrayList<>();
        this.viewedPlayer = 0;

        this.lastTime            = -1;
        this.pendingMilliseconds = 0;
        this.speed               = 1;
        this.paused              = false;
    }

    /**
     * @return the game loop the playback reached
     * @throws IllegalStateException if the scene is not open
     */
    @Override
    public long getLoop() {
        return this.requireController().getLoop();
    }

    /**
     * @return the game loops of the replay
     * @throws IllegalStateException if the scene is not open
     */
    @Override
    public long getLoopCount() {
        return this.requireReplay().getLoopCount();
    }

    /**
     * @return how many game loops play in the time of one at normal speed, one of {@link #SPEEDS}
     */
    @Override
    public int getSpeed() {
        return this.speed;
    }

    /**
     * @return {@code true} if the playback is paused, {@code false} otherwise
     */
    @Override
    public boolean isPaused() {
        return this.paused;
    }

    /**
     * Pauses or resumes the playback.
     *
     * @param paused whether the playback pauses
     */
    @Override
    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    /**
     * @return the shown name of the player the viewer follows: its faction's name
     */
    @Override
    public @NotNull String getViewedPlayerName() {
        if (this.players.isEmpty()) {
            return "";
        }

        Faction faction = this.requireScreen().getView().getFaction(this.players.get(this.viewedPlayer));

        return faction != null ? DemoClient.getName(faction) : this.players.get(this.viewedPlayer).toString();
    }

    /**
     * @return the screen of the playback, or {@code null} before the scene opens
     */
    public @Nullable DemoGameScreen getScreen() {
        return this.screen;
    }

    /**
     * Opens the scene: loads what it shows. Called once, before the first {@link #update}.
     * <p>
     * Reads the replay, creates its game in a replay session and the screen with the playback commands, and plays the game music.
     *
     * @param context the client's shared services
     * @throws IOException if the replay or the map's tileset cannot be read, or the demo has no map of the replay
     */
    @Override
    public void open(@NotNull SceneClient context) throws IOException {
        EngineReplay read = this.demo.getReplayFormat().read(Files.readAllBytes(this.file));
        Game created;
        MapModel map;

        try {
            created = this.demo.getGames().createGame(read.getSetup());
            map     = this.demo.getMaps().require(read.getSetup());
        } catch (IllegalArgumentException exception) {
            throw Exceptions.newIOException(exception, "The replay {0} is of a map the demo does not have.", this.file);
        }

        GameSessionFactory factory = DemoClient.createSessionFactory(this.demo.getContent(), created, map);
        ReplayController started = read.createController(null);
        GameSession session = factory.createReplay(created, List.of(started));
        DemoGameScreen shown = new DemoGameScreen(this.demo, session, map, created.getPlayer(DemoGames.FIRST_SLOT));

        this.replay     = read;
        this.game       = created;
        this.sessions   = factory;
        this.session    = session;
        this.controller = started;
        this.screen     = shown;

        this.players.clear();

        for (PlayerSlot slot : ReplaySummary.findPlayingSlots(read.getSetup().getPlayers())) {
            this.players.add(created.getPlayer(slot.getSlotIndex()));
        }

        this.viewedPlayer = 0;
        this.showViewedPlayer();
        shown.getScreen().setQueuePanel(new ReplayPanel(this, this.demo.getFont(), DemoGameScene.LOOP_MILLISECONDS), REPLAY_PANEL_WIDTH);
        this.registerCommands(shown);
        this.snapshots.put(0L, StateSnapshot.capture(created));
        shown.addMessage("Replay of " + this.demo.describe(read.getSetup()) + ".");
        this.demo.playMusic(DemoClient.GAME_MUSIC);
    }

    /**
     * Updates the scene at the supplied time.
     * <p>
     * Plays the game loops the time since the last update allows at the playback speed, at most
     * {@value DemoGameScene#MAXIMUM_LOOPS_PER_UPDATE} times the speed.
     *
     * @param time the client time in milliseconds
     */
    @Override
    public void update(long time) {
        long elapsed = this.lastTime < 0 ? 0 : Math.max(0, time - this.lastTime);
        ReplayController playing = this.requireController();
        int limit = DemoGameScene.MAXIMUM_LOOPS_PER_UPDATE * this.speed;

        this.lastTime = time;

        if (!this.paused) {
            this.pendingMilliseconds += elapsed * this.speed;

            for (int loop = 0; loop < limit && this.pendingMilliseconds >= DemoGameScene.LOOP_MILLISECONDS && !playing.isFinished(); loop++) {
                this.runLoop();
                this.pendingMilliseconds -= DemoGameScene.LOOP_MILLISECONDS;
            }

            this.pendingMilliseconds = Math.min(this.pendingMilliseconds, DemoGameScene.LOOP_MILLISECONDS);
        }

        this.requireScreen().update(elapsed);
    }

    /**
     * Draws the scene.
     *
     * @param surface the surface
     * @throws IOException if a resource the scene draws cannot be read
     */
    @Override
    public void draw(@NotNull Canvas surface) throws IOException {
        ReplayController playing = this.requireController();
        String status = playing.getMismatchCount() > 0 ? STATUS + "  " + playing.getMismatchCount() + " checkpoints missed" : STATUS;

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
        return this.requireScreen().handleInput(event);
    }

    /**
     * Plays faster: the next of {@link #SPEEDS}, up to {@value #MAXIMUM_SPEED}.
     */
    @Override
    public void increaseSpeed() {
        this.speed = SPEEDS.get(Math.min(SPEEDS.indexOf(this.speed) + 1, SPEEDS.size() - 1));
    }

    /**
     * Plays slower: the previous of {@link #SPEEDS}, down to normal speed.
     */
    @Override
    public void decreaseSpeed() {
        this.speed = SPEEDS.get(Math.max(SPEEDS.indexOf(this.speed) - 1, 0));
    }

    /**
     * Follows the next player who played: the top bar shows its resources and supply.
     */
    @Override
    public void viewNextPlayer() {
        if (this.players.isEmpty()) {
            return;
        }

        this.viewedPlayer = (this.viewedPlayer + 1) % this.players.size();
        this.showViewedPlayer();
    }

    /**
     * Seeks the playback to a game loop, as the replay panel's bar asks; a snapshot that cannot be restored leaves the playback where it is.
     *
     * @param loop the game loop, within the replay
     */
    @Override
    public void seek(long loop) {
        try {
            this.seekTo(loop);
        } catch (IOException | IllegalStateException exception) {
            this.requireScreen().addMessage("The seek failed: " + exception.getMessage());
        }
    }

    /**
     * Seeks the playback to a game loop: forward by playing on, back from the nearest earlier state snapshot.
     *
     * @param target the game loop, limited to the replay
     * @throws IOException if a snapshot cannot be restored
     */
    public void seekTo(long target) throws IOException {
        long loop = Math.clamp(target, 0, this.requireReplay().getLoopCount());

        if (loop < this.requireController().getLoop()) {
            Map.Entry<Long, StateSnapshot> snapshot = this.snapshots.floorEntry(loop);
            Game current = this.requireGame();
            GameSessionFactory factory = this.sessions;

            if (snapshot == null || factory == null) {
                throw Exceptions.newIllegalStateException("The replay viewer has no snapshot before game loop {0}.", loop);
            }

            snapshot.getValue().restore(current);

            ReplayController restarted = this.requireReplay().createController(null, snapshot.getKey());

            this.session    = factory.createReplay(current, List.of(restarted));
            this.controller = restarted;
        }

        while (this.requireController().getLoop() < loop && !this.requireController().isFinished()) {
            this.runLoop();
        }
    }

    private void runLoop() {
        Game current = this.requireGame();
        GameSession playing = this.session;

        if (playing == null) {
            throw Exceptions.newIllegalStateException("The replay viewer is not open.");
        }

        playing.runFrame();

        long loop = this.requireController().getLoop();

        if (loop % SNAPSHOT_INTERVAL == 0 && !this.snapshots.containsKey(loop)) {
            this.snapshots.put(loop, StateSnapshot.capture(current));
        }
    }

    private void showViewedPlayer() {
        if (this.players.isEmpty()) {
            return;
        }

        this.requireScreen().getScreen().setViewedPlayer(this.players.get(this.viewedPlayer), this.getViewedPlayerName());
    }

    private void registerCommands(@NotNull DemoGameScreen target) {
        target.getCommands().register(ConsoleCommand.of("/speed", "[1|2|4|8|16]", "show or set the playback speed")
                                                    .describe("Sets how many game loops play in the time of one at normal speed.")
                                                    .whenEmpty("show the playback speed", arguments -> "Speed x" + this.speed + ".")
                                                    .otherwise("<speed>", "set the playback speed", this::answerSpeed)
                                                    .availability("in the replay viewer.")
                                                    .example("/speed 8"));
        target.getCommands().register(ConsoleCommand.of("/pause", "", "pause the playback")
                                                    .whenEmpty("pause the playback", arguments -> this.answerPause(true))
                                                    .availability("in the replay viewer."));
        target.getCommands().register(ConsoleCommand.of("/resume", "", "resume the paused playback")
                                                    .whenEmpty("resume the playback", arguments -> this.answerPause(false))
                                                    .availability("in the replay viewer."));
        target.getCommands().register(ConsoleCommand.of("/seek", "<time>", "jump to a game time of the replay")
                                                    .describe("Plays forward to the game time at once, or goes back to it from the nearest " +
                                                              "earlier snapshot of the playback. The time is minutes and seconds, or seconds.")
                                                    .otherwise("<time>", "jump to the game time", this::answerSeek)
                                                    .availability("in the replay viewer.")
                                                    .example("/seek 2:30"));
        target.getCommands().register(ConsoleCommand.of("/leave", "", "return to the main menu")
                                                    .whenEmpty("leave the replay", arguments -> this.leave())
                                                    .availability("in the replay viewer."));
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
        this.setPaused(paused);

        return paused ? "Paused." : "Resumed.";
    }

    private @NotNull String answerSeek(@NotNull String @NotNull [] arguments) {
        long target = arguments.length == 1 ? GameTimeUtils.parse(arguments[0], DemoGameScene.LOOP_MILLISECONDS) : -1;

        if (target < 0) {
            return "Use /seek with a game time, for example /seek 2:30.";
        }

        try {
            this.seekTo(target);
        } catch (IOException exception) {
            return "The seek failed: " + exception.getMessage();
        }

        return "At " + GameTimeUtils.format(this.requireController().getLoop(), DemoGameScene.LOOP_MILLISECONDS) + " of " +
               GameTimeUtils.format(this.requireReplay().getLoopCount(), DemoGameScene.LOOP_MILLISECONDS) + ".";
    }

    private @NotNull String leave() {
        this.menu.show();

        return "Left the replay.";
    }

    private @NotNull EngineReplay requireReplay() {
        EngineReplay current = this.replay;

        if (current == null) {
            throw Exceptions.newIllegalStateException("The replay viewer is not open.");
        }

        return current;
    }

    private @NotNull Game requireGame() {
        Game current = this.game;

        if (current == null) {
            throw Exceptions.newIllegalStateException("The replay viewer is not open.");
        }

        return current;
    }

    private @NotNull ReplayController requireController() {
        ReplayController current = this.controller;

        if (current == null) {
            throw Exceptions.newIllegalStateException("The replay viewer is not open.");
        }

        return current;
    }

    private @NotNull DemoGameScreen requireScreen() {
        DemoGameScreen current = this.screen;

        if (current == null) {
            throw Exceptions.newIllegalStateException("The replay viewer is not open.");
        }

        return current;
    }

    /**
     * Closes the scene: releases what it holds, the screen's pictures.
     */
    @Override
    public void close() {
        DemoGameScreen shown = this.screen;

        this.screen = null;
        this.snapshots.clear();

        if (shown != null) {
            IgnoreException.execute(
                    shown::close
            );
        }
    }
}
