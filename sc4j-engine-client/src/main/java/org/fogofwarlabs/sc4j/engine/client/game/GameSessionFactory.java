package org.fogofwarlabs.sc4j.engine.client.game;

import org.fogofwarlabs.sc4j.engine.client.view.GameView;
import org.fogofwarlabs.sc4j.engine.client.view.StandardGameView;
import org.fogofwarlabs.sc4j.engine.command.CommandSink;
import org.fogofwarlabs.sc4j.engine.content.economy.ResourceKind;
import org.fogofwarlabs.sc4j.engine.controller.ControllerCommandSource;
import org.fogofwarlabs.sc4j.engine.controller.PlayerController;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.game.Game;
import org.fogofwarlabs.sc4j.engine.lockstep.LockstepSession;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.fogofwarlabs.sc4j.engine.spatial.UnitBounds;
import org.fogofwarlabs.sc4j.engine.terrain.TerrainGrid;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Creates the {@link GameSession}s of a client: a local game, a replay or a network game of a created engine {@link Game}. Each session gets
 * the view of its game from the factory's {@link GameViewFactory} (the engine default {@link StandardGameView}, or the mod's own view) and a
 * {@link ControllerCommandSource} over the session's controllers in the order this factory documents per kind, on which a replay recorder of
 * the game is set ({@link GameSession#getCommandSource()}). The factory's {@link CommandPoint} says who applies that source: the game's frame
 * runner, whose command source the factory sets, or the mod's own game loop. The mod creates the game and its controllers (computer players,
 * replay readers, network turn readers); the factory only connects them to the client.
 */
public final class GameSessionFactory {
    private static @NotNull GameViewFactory createStandardViewFactory(@NotNull TerrainGrid terrain,
                                                                      @NotNull UnitBounds<Unit> bounds,
                                                                      int fractionBits,
                                                                      @NotNull List<@NotNull ResourceKind> resourceKinds,
                                                                      int @NotNull [] teamColors) {
        List<ResourceKind> kinds = List.copyOf(resourceKinds);
        int[] colors = teamColors.clone();

        return (game, localPlayer) -> new StandardGameView(game, localPlayer, terrain, bounds, fractionBits, kinds, colors);
    }


    private final @NotNull GameViewFactory viewFactory;
    private final @NotNull CommandPoint commandPoint;

    /**
     * Creates a {@link GameSessionFactory} instance of the engine defaults: each session shows its game through a {@link StandardGameView},
     * and the game's frame runner applies the session's commands ({@link CommandPoint#FRAME_RUNNER}).
     *
     * @param terrain       the terrain of the games' map
     * @param bounds        the bounds of the games' units, the same the game's systems use
     * @param fractionBits  the fraction bits of the units' hit points and damage layer points (the combat system's fixed-point unit)
     * @param resourceKinds the resource kinds in their order, copied
     * @param teamColors    the team colours by colour index, packed {@code 0xRRGGBB}, copied
     */
    public GameSessionFactory(@NotNull TerrainGrid terrain,
                              @NotNull UnitBounds<Unit> bounds,
                              int fractionBits,
                              @NotNull List<@NotNull ResourceKind> resourceKinds,
                              int @NotNull [] teamColors) {
        this(createStandardViewFactory(terrain, bounds, fractionBits, resourceKinds, teamColors), CommandPoint.FRAME_RUNNER);
    }

    /**
     * Creates a {@link GameSessionFactory} instance of a mod's own view and command point.
     *
     * @param viewFactory  the factory of the sessions' views
     * @param commandPoint who applies the sessions' commands
     */
    public GameSessionFactory(@NotNull GameViewFactory viewFactory, @NotNull CommandPoint commandPoint) {
        this.viewFactory  = viewFactory;
        this.commandPoint = commandPoint;
    }

    /**
     * @return who applies the commands of the sessions this factory creates
     */
    public @NotNull CommandPoint getCommandPoint() {
        return this.commandPoint;
    }

    /**
     * Creates a {@link GameSession} instance of a local game: the local player's commands go to a {@link LocalCommandQueue}, which hands
     * them over first at each frame, then the other controllers in their order.
     *
     * @param game        the game
     * @param localPlayer the player of the person at this machine
     * @param controllers the other players' controllers, for example computer players
     * @return the created {@link GameSession} instance
     */
    public @NotNull GameSession createLocal(@NotNull Game game, @NotNull Player localPlayer, @NotNull List<@NotNull PlayerController> controllers) {
        LocalCommandQueue queue = new LocalCommandQueue();
        List<PlayerController> all = new ArrayList<>(controllers.size() + 1);

        all.add(queue);
        all.addAll(controllers);

        return this.createSession(GameSession.Kind.LOCAL, game, localPlayer, all, queue, null);
    }

    /**
     * Creates a {@link GameSession} instance of a local game with a mod's own command path: the local player's commands go to the mod's sink
     * (for example its own turn buffer), and the controllers hand over every command, the local player's included.
     *
     * @param game        the game
     * @param localPlayer the player of the person at this machine
     * @param local       the sink of the local player's commands
     * @param controllers the controllers that hand over the commands, in their order
     * @return the created {@link GameSession} instance
     */
    public @NotNull GameSession createLocal(@NotNull Game game,
                                            @NotNull Player localPlayer,
                                            @NotNull CommandSink local,
                                            @NotNull List<@NotNull PlayerController> controllers) {
        return this.createSession(GameSession.Kind.LOCAL, game, localPlayer, controllers, local, null);
    }

    /**
     * Creates a {@link GameSession} instance of a replay without local player: the replay controllers hand over the recorded commands, for an
     * engine replay its {@link org.fogofwarlabs.sc4j.engine.replay.ReplayController} as the first controller.
     *
     * @param game        the game, created from the replay's setup
     * @param controllers the replay controllers in their order
     * @return the created {@link GameSession} instance
     */
    public @NotNull GameSession createReplay(@NotNull Game game, @NotNull List<@NotNull PlayerController> controllers) {
        return this.createSession(GameSession.Kind.REPLAY, game, null, controllers, null, null);
    }

    /**
     * Creates a {@link GameSession} instance of a network game over the engine's lockstep session: the local player's commands go to the
     * session's outgoing sink, every player's commands, the local player's included, come back with the turns through the session's
     * controller, then the other controllers hand over theirs (for example the computer players every machine runs). The factory attaches the
     * session to the game (its state hash exchange), and the game session runs a frame only once its turns arrived.
     *
     * @param game        the game
     * @param localPlayer the player of the person at this machine, or {@code null} for an observer
     * @param session     the lockstep session of this machine
     * @param controllers the other controllers, in their order
     * @return the created {@link GameSession} instance
     */
    public @NotNull GameSession createNetwork(@NotNull Game game,
                                              @Nullable Player localPlayer,
                                              @NotNull LockstepSession session,
                                              @NotNull List<@NotNull PlayerController> controllers) {
        List<PlayerController> all = new ArrayList<>(controllers.size() + 1);

        all.add(session.getController());
        all.addAll(controllers);
        session.attach(game);

        return this.createSession(GameSession.Kind.NETWORK, game, localPlayer, all, session.getOutgoingSink(), session);
    }

    /**
     * Creates a {@link GameSession} instance of a network game of a mod's own network protocol: the local player's commands go to the network
     * session's outgoing sink, and every player's commands, the local player's included, come back with the turns through the network
     * controllers. The client runs a frame only once the protocol's turns arrived.
     *
     * @param game        the game
     * @param localPlayer the player of the person at this machine
     * @param outgoing    the sink of the network session that sends the local commands with the next turn
     * @param controllers the network controllers that hand over the commands of each turn, in their order
     * @return the created {@link GameSession} instance
     */
    public @NotNull GameSession createNetwork(@NotNull Game game,
                                              @NotNull Player localPlayer,
                                              @NotNull CommandSink outgoing,
                                              @NotNull List<@NotNull PlayerController> controllers) {
        return this.createSession(GameSession.Kind.NETWORK, game, localPlayer, controllers, outgoing, null);
    }

    private @NotNull GameSession createSession(@NotNull GameSession.Kind kind,
                                               @NotNull Game game, @Nullable Player localPlayer,
                                               @NotNull List<@NotNull PlayerController> controllers,
                                               @Nullable CommandSink commandSink,
                                               @Nullable LockstepSession lockstepSession) {
        GameView view = this.viewFactory.createView(game, localPlayer);
        ControllerCommandSource source = new ControllerCommandSource(controllers);

        if (this.commandPoint == CommandPoint.FRAME_RUNNER) {
            game.getFrameRunner().setCommandSource(source);
        }

        return new GameSession(kind, game, localPlayer, view, this.commandPoint, source, commandSink, lockstepSession);
    }
}
