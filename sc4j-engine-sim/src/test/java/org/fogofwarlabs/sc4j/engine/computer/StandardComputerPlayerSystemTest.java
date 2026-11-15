package org.fogofwarlabs.sc4j.engine.computer;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.command.CommandSink;
import org.fogofwarlabs.sc4j.engine.command.CommandType;
import org.fogofwarlabs.sc4j.engine.controller.ControllerSource;
import org.fogofwarlabs.sc4j.engine.game.*;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests the engine default computer player system without a mod: one controller per computer slot, run in slot order in its frame phase,
 * each running a small script through an instruction registry, and no commands from computer players.
 */
final class StandardComputerPlayerSystemTest {
    private static final int OPCODE_LOG = 0;
    private static final int OPCODE_WAIT = 1;
    private static final int OPCODE_END = 2;
    private static final int OPCODE_COUNT = 3;

    /**
     * The first value a player's script logs is its player index times this.
     */
    private static final int VALUES_PER_PLAYER = 10;

    /**
     * The context of the test interpreter: the game, the player of the running script, its program and its position.
     */
    private static final class ScriptContext implements ComputerScriptContext {
        private final @NotNull Game game;
        private final @NotNull Player player;
        private final @NotNull List<@NotNull String> log;
        private final int @NotNull [] program;

        private int position;
        private int wait;

        private ScriptContext(@NotNull Game game, @NotNull Player player, @NotNull List<@NotNull String> log, int @NotNull [] program) {
            this.game    = game;
            this.player  = player;
            this.log     = log;
            this.program = program;

            this.position = 0;
            this.wait     = 0;
        }

        @Override
        public @NotNull Game getGame() {
            return this.game;
        }

        @Override
        public @NotNull Player getPlayer() {
            return this.player;
        }

        private int readOperand() {
            return this.program[this.position++];
        }
    }

    /**
     * Runs the script of one computer player: waits, then the instructions up to the first that stops the script.
     */
    private static final class ScriptController implements ComputerController {
        private final @NotNull ComputerInstructionRegistry<ComputerInstruction<ScriptContext>> instructions;
        private final @NotNull ScriptContext context;

        private ScriptController(@NotNull ComputerInstructionRegistry<ComputerInstruction<ScriptContext>> instructions,
                                 @NotNull ScriptContext context) {
            this.instructions = instructions;
            this.context      = context;
        }

        @Override
        public @NotNull Player getPlayer() {
            return this.context.player;
        }

        @Override
        public void update(@NotNull Game game) {
            if (this.context.wait > 0) {
                this.context.wait--;

                return;
            }

            while (true) {
                ComputerInstruction<ScriptContext> instruction = this.instructions.find(this.context.readOperand());

                if (instruction == null) {
                    throw Exceptions.newIllegalStateException("Unknown opcode.");
                }

                if (!instruction.execute(this.context)) {
                    return;
                }
            }
        }
    }

    /**
     * Fails on every command: computer players submit none.
     */
    private static final class FailingSink implements CommandSink {
        @Override
        public <C> void submit(@NotNull Player player, @NotNull CommandType<C> type, @NotNull C command) {
            throw Exceptions.newIllegalStateException("A computer player submitted a command.");
        }
    }

    @Test
    void computer_players_run_their_scripts_in_slot_order_and_submit_no_commands() {
        List<String> log = new ArrayList<>();
        Game game = createGame(log, createSetup());
        ComputerPlayerSystem system = game.getComputerPlayers();

        for (int frame = 0; frame < 3; frame++) {
            game.getFrameRunner().runFrame();
        }

        Assertions.assertEquals(List.of("player 1 says 10 at frame 1", "player 3 says 30 at frame 1",
                                        "player 1 says 11 at frame 3", "player 3 says 31 at frame 3"), log);
        Assertions.assertSame(game.getModSystem(ComputerPlayerSystem.KEY), system);
        Assertions.assertNull(system.getController(game.getPlayer(0)));
        Assertions.assertNull(system.getController(game.getPlayer(2)));

        ComputerController controller = system.getController(game.getPlayer(1));

        Assertions.assertNotNull(controller);
        Assertions.assertSame(game.getPlayer(1), controller.getPlayer());
        Assertions.assertEquals(ControllerSource.COMPUTER, controller.getSource());
        Assertions.assertDoesNotThrow(() -> controller.collectCommands(game, new FailingSink()));
        Assertions.assertEquals(2, ((StandardComputerPlayerSystem) system).getControllers().size());
    }

    @Test
    void attach_should_reject_a_controller_of_another_player() {
        GameDefinition definition = new GameDefinition.Builder()
                .useSystem(ComputerPlayerSystem.KEY, context -> new StandardComputerPlayerSystem((game, player) -> new ScriptController(
                        new ComputerInstructionRegistry<>(0), new ScriptContext(game, game.getPlayer(0), new ArrayList<>(), new int[0])
                )))
                .build();

        Assertions.assertThrows(IllegalStateException.class, () -> Game.create(definition, createSetup()));
    }

    @Test
    void get_computer_players_should_fail_without_a_registered_computer_player_system() {
        Game game = Game.create(new GameDefinition.Builder().build(), createSetup());

        Assertions.assertFalse(game.hasSystem(ComputerPlayerSystem.KEY));
        Assertions.assertThrows(IllegalStateException.class, game::getComputerPlayers);
    }

    private static @NotNull Game createGame(@NotNull List<@NotNull String> log, @NotNull GameSetup setup) {
        ComputerInstructionRegistry<ComputerInstruction<ScriptContext>> instructions = new ComputerInstructionRegistry<>(OPCODE_COUNT);

        instructions.register(OPCODE_LOG, context -> {
            context.log.add("player " + context.getPlayer().getIndex() + " says " + context.readOperand() + " at frame " +
                            context.getGame().getClock().getFrame());

            return true;
        });
        instructions.register(OPCODE_WAIT, context -> {
            context.wait = context.readOperand();

            return false;
        });
        instructions.register(OPCODE_END, context -> {
            context.position--;

            return false;
        });
        instructions.freeze();

        GameDefinition definition = new GameDefinition.Builder()
                .useSystem(ComputerPlayerSystem.KEY, created -> new StandardComputerPlayerSystem((game, player) -> {
                    int base = player.getIndex() * VALUES_PER_PLAYER;
                    int[] program = {OPCODE_LOG, base, OPCODE_WAIT, 1, OPCODE_LOG, base + 1, OPCODE_END};

                    return new ScriptController(instructions, new ScriptContext(game, player, log, program));
                }))
                .addFramePhase(ComputerPlayerSystem.KEY)
                .build();

        return Game.create(definition, setup);
    }

    private static @NotNull GameSetup createSetup() {
        return new GameSetup.Builder(new MapReference("empty", new byte[0])).addPlayer(new PlayerSlot(0, ControllerKind.HUMAN, 0, 0, null))
                                                                            .addPlayer(new PlayerSlot(1, ControllerKind.COMPUTER, 1, 1, null))
                                                                            .addPlayer(new PlayerSlot(2, ControllerKind.NEUTRAL, 2, 2, null))
                                                                            .addPlayer(new PlayerSlot(3, ControllerKind.COMPUTER, 3, 3, null))
                                                                            .build();
    }
}
