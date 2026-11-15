package org.fogofwarlabs.sc4j.engine.client.game;

import org.fogofwarlabs.sc4j.engine.client.SkirmishSessionScenario;
import org.fogofwarlabs.sc4j.engine.client.command.CommandTarget;
import org.fogofwarlabs.sc4j.engine.client.command.CommandTranslator;
import org.fogofwarlabs.sc4j.engine.client.command.OrderAction;
import org.fogofwarlabs.sc4j.engine.client.command.OrderedSelection;
import org.fogofwarlabs.sc4j.engine.client.input.KeyBindingTable;
import org.fogofwarlabs.sc4j.engine.client.view.GameView;
import org.fogofwarlabs.sc4j.engine.combat.StandardCombatSystem;
import org.fogofwarlabs.sc4j.engine.command.CommandRegistry;
import org.fogofwarlabs.sc4j.engine.command.CommandSink;
import org.fogofwarlabs.sc4j.engine.command.CommandType;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.game.Game;
import org.fogofwarlabs.sc4j.engine.lockstep.InMemoryLockstepNetwork;
import org.fogofwarlabs.sc4j.engine.lockstep.LockstepPeer;
import org.fogofwarlabs.sc4j.engine.lockstep.LockstepSession;
import org.fogofwarlabs.sc4j.engine.lockstep.TurnSchedule;
import org.fogofwarlabs.sc4j.engine.order.IssueOrderCommandType;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

/**
 * Tests the engine game sessions on the skirmish map: the local player's commands reach the game at the next frame through the local queue,
 * a replay session has no local player and no command sink, a network session over the engine lockstep session runs the local player's
 * commands after the latency, and a session of the mod's own view and game loop applies its commands only through its command source.
 */
final class GameSessionFactoryTest {
    private static final int SCOUT_X = 400;
    private static final int SCOUT_Y = 144;
    private static final int TARGET_X = 600;
    private static final int TARGET_Y = 400;
    private static final int FRAMES = 4;
    private static final int COMMAND_ID = 0;
    private static final int CAPACITY = 12;
    private static final int LATENCY_TURNS = 2;

    @Test
    void local_session_applies_the_local_commands_at_the_next_frame() throws IOException {
        SkirmishSessionScenario skirmish = new SkirmishSessionScenario();
        GameSession session = skirmish.getSession();
        GameView view = session.getView();
        Player local = session.getLocalPlayer();
        CommandSink sink = session.getCommandSink();

        skirmish.runFrames(FRAMES);

        Unit scout = view.findUnitAt(SCOUT_X, SCOUT_Y);

        Assertions.assertEquals(GameSession.Kind.LOCAL, session.getKind());
        Assertions.assertNotNull(local);
        Assertions.assertInstanceOf(LocalCommandQueue.class, sink);
        Assertions.assertNotNull(scout);

        OrderedSelection selection = new OrderedSelection(CAPACITY);
        OrderAction move = new OrderAction(session.getGame(), new IssueOrderCommandType(COMMAND_ID, 0), skirmish.getScenario().getMove(), true);
        CommandTranslator translator = new CommandTranslator(local, sink, selection, new KeyBindingTable<>(),
                                                             (x, y) -> CommandTarget.at(TARGET_X, TARGET_Y));

        selection.select(scout);
        move.issue(translator, CommandTarget.at(TARGET_X, TARGET_Y), false);

        Assertions.assertEquals(1, ((LocalCommandQueue) sink).getQueuedCount());
        Assertions.assertNotSame(skirmish.getScenario().getMove(), view.getUnitView(scout).getOrderType(), "nothing changes before the frame");

        session.runFrame();

        Assertions.assertEquals(0, ((LocalCommandQueue) sink).getQueuedCount());
        Assertions.assertSame(skirmish.getScenario().getMove(), view.getUnitView(scout).getOrderType());
    }

    @Test
    void replay_session_has_no_local_player_and_no_sink() throws IOException {
        SkirmishSessionScenario skirmish = new SkirmishSessionScenario();
        GameSessionFactory factory = new GameSessionFactory(skirmish.getMap().getTerrain(), skirmish.getScenario().getBounds(),
                                                            StandardCombatSystem.FRACTION_BITS, skirmish.getScenario().getResourceKinds(),
                                                            SkirmishSessionScenario.TEAM_COLORS);
        GameSession replay = factory.createReplay(skirmish.getSession().getGame(), List.of());

        Assertions.assertEquals(GameSession.Kind.REPLAY, replay.getKind());
        Assertions.assertNull(replay.getLocalPlayer());
        Assertions.assertNull(replay.getCommandSink());

        replay.runFrame();

        Assertions.assertEquals(1, replay.getView().getFrame());
    }

    @Test
    void network_session_runs_the_local_commands_after_the_latency() throws IOException {
        SkirmishSessionScenario skirmish = new SkirmishSessionScenario();
        Game game = skirmish.getSession().getGame();
        GameSessionFactory factory = new GameSessionFactory(skirmish.getMap().getTerrain(), skirmish.getScenario().getBounds(),
                                                            StandardCombatSystem.FRACTION_BITS, skirmish.getScenario().getResourceKinds(),
                                                            SkirmishSessionScenario.TEAM_COLORS);
        IssueOrderCommandType orders = new IssueOrderCommandType(COMMAND_ID, 0);
        CommandRegistry<CommandType<?>> commands = new CommandRegistry<>(1);
        LockstepPeer peer = new LockstepPeer(0);
        InMemoryLockstepNetwork network = new InMemoryLockstepNetwork(List.of(peer));
        Player local = game.getPlayer(SkirmishSessionScenario.LOCAL_SLOT);

        commands.register(orders);
        commands.freeze();

        LockstepSession lockstep = new LockstepSession(network.getTransport(peer), commands, peer, List.of(local), new TurnSchedule(1, LATENCY_TURNS),
                                                       null, null);
        GameSession session = factory.createNetwork(game, local, lockstep, List.of());
        CommandSink sink = session.getCommandSink();

        for (int frame = 0; frame < FRAMES; frame++) {
            Assertions.assertTrue(session.runFrame());
        }

        Unit scout = session.getView().findUnitAt(SCOUT_X, SCOUT_Y);
        OrderedSelection selection = new OrderedSelection(CAPACITY);
        OrderAction move = new OrderAction(game, orders, skirmish.getScenario().getMove(), true);

        Assertions.assertEquals(GameSession.Kind.NETWORK, session.getKind());
        Assertions.assertSame(lockstep, session.getLockstepSession());
        Assertions.assertSame(lockstep.getOutgoingSink(), sink);
        Assertions.assertNotNull(sink);
        Assertions.assertNotNull(scout);

        selection.select(scout);
        move.issue(new CommandTranslator(local, sink, selection, new KeyBindingTable<>(), (x, y) -> CommandTarget.at(TARGET_X, TARGET_Y)),
                   CommandTarget.at(TARGET_X, TARGET_Y), false);

        for (int frame = 0; frame < LATENCY_TURNS; frame++) {
            session.runFrame();
        }

        Assertions.assertNotSame(skirmish.getScenario().getMove(), session.getView().getUnitView(scout).getOrderType(), "the turn is in transit");

        session.runFrame();

        Assertions.assertSame(skirmish.getScenario().getMove(), session.getView().getUnitView(scout).getOrderType());
    }

    @Test
    void mod_loop_session_applies_its_commands_only_through_its_command_source() throws IOException {
        SkirmishSessionScenario skirmish = new SkirmishSessionScenario();
        GameView modView = skirmish.getSession().getView();
        Game game = skirmish.getSession().getGame();
        Player local = game.getPlayer(SkirmishSessionScenario.LOCAL_SLOT);
        GameSessionFactory factory = new GameSessionFactory((viewGame, viewPlayer) -> modView, CommandPoint.MOD_LOOP);
        LocalCommandQueue turns = new LocalCommandQueue();
        LocalCommandQueue outgoing = new LocalCommandQueue();

        game.getFrameRunner().setCommandSource(null);

        GameSession session = factory.createLocal(game, local, outgoing, List.of(turns));
        Unit scout = modView.findUnitAt(SCOUT_X, SCOUT_Y);
        OrderedSelection selection = new OrderedSelection(CAPACITY);
        OrderAction move = new OrderAction(game, new IssueOrderCommandType(COMMAND_ID, 0), skirmish.getScenario().getMove(), true);

        Assertions.assertEquals(CommandPoint.MOD_LOOP, factory.getCommandPoint());
        Assertions.assertEquals(GameSession.Kind.LOCAL, session.getKind());
        Assertions.assertEquals(CommandPoint.MOD_LOOP, session.getCommandPoint());
        Assertions.assertSame(modView, session.getView());
        Assertions.assertSame(local, session.getLocalPlayer());
        Assertions.assertSame(outgoing, session.getCommandSink());
        Assertions.assertNull(game.getFrameRunner().getCommandSource(), "the mod's loop applies the commands");
        Assertions.assertThrows(IllegalStateException.class, session::runFrame);
        Assertions.assertNotNull(scout);

        selection.select(scout);
        move.issue(new CommandTranslator(local, turns, selection, new KeyBindingTable<>(), (x, y) -> CommandTarget.at(TARGET_X, TARGET_Y)),
                   CommandTarget.at(TARGET_X, TARGET_Y), false);
        game.getFrameRunner().runFrame();

        Assertions.assertEquals(1, turns.getQueuedCount(), "the frame runner does not ask the session's controllers");

        session.getCommandSource().applyCommands(game);

        Assertions.assertEquals(0, turns.getQueuedCount());
        Assertions.assertEquals(0, outgoing.getQueuedCount());
        Assertions.assertSame(skirmish.getScenario().getMove(), modView.getUnitView(scout).getOrderType());
    }
}
