package org.fogofwarlabs.sc4j.engine.controller;

import org.fogofwarlabs.sc4j.engine.command.CommandInput;
import org.fogofwarlabs.sc4j.engine.command.CommandOutput;
import org.fogofwarlabs.sc4j.engine.command.CommandSink;
import org.fogofwarlabs.sc4j.engine.command.CommandType;
import org.fogofwarlabs.sc4j.engine.game.*;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Tests that a {@link ControllerCommandSource} runs the commands of its controllers at each frame, in controller order and submission order,
 * before the frame counter advances.
 */
final class ControllerCommandSourceTest {
    private static final int FRAME_COUNT = 4;
    private static final int COMPUTER_PERIOD = 2;

    /**
     * A test command type: a note with an int value; it logs every executed note with its frame and player.
     */
    private static final class NoteType implements CommandType<Integer> {
        private final @NotNull List<String> log;

        private NoteType(@NotNull List<String> log) {
            this.log = log;
        }

        @Override
        public int getId() {
            return 0;
        }

        @Override
        public @NotNull Integer read(@NotNull CommandInput input) throws IOException {
            return input.readInt();
        }

        @Override
        public void write(@NotNull Integer command, @NotNull CommandOutput output) {
            output.writeInt(command);
        }

        @Override
        public void execute(@NotNull Game game, @NotNull Player player, @NotNull Integer command) {
            this.log.add(game.getClock().getFrame() + " " + player.getIndex() + " " + command);
        }
    }

    /**
     * A replay controller: the recorded notes of player 0 by frame.
     */
    private static final class ReplayController implements PlayerController {
        private final @NotNull NoteType type;
        private final @NotNull Map<Integer, List<Integer>> notes;

        private ReplayController(@NotNull NoteType type, @NotNull Map<Integer, List<Integer>> notes) {
            this.type  = type;
            this.notes = notes;
        }

        @Override
        public @NotNull ControllerSource getSource() {
            return ControllerSource.REPLAY;
        }

        @Override
        public void collectCommands(@NotNull Game game, @NotNull CommandSink sink) {
            for (Integer note : this.notes.getOrDefault(game.getClock().getFrame(), List.of())) {
                sink.submit(game.getPlayer(0), this.type, note);
            }
        }
    }

    /**
     * A computer controller: player 1 submits the frame number on every second frame.
     */
    private static final class ComputerController implements PlayerController {
        private final @NotNull NoteType type;

        private ComputerController(@NotNull NoteType type) {
            this.type = type;
        }

        @Override
        public @NotNull ControllerSource getSource() {
            return ControllerSource.COMPUTER;
        }

        @Override
        public void collectCommands(@NotNull Game game, @NotNull CommandSink sink) {
            int frame = game.getClock().getFrame();

            if (frame % COMPUTER_PERIOD == 0) {
                sink.submit(game.getPlayer(1), this.type, frame);
            }
        }
    }

    private static @NotNull Game createGame() {
        GameSetup setup = new GameSetup.Builder(new MapReference("empty", new byte[0])).addPlayer(new PlayerSlot(0, ControllerKind.HUMAN, 0, 0, null))
                                                                                       .addPlayer(new PlayerSlot(1, ControllerKind.COMPUTER, 1, 1, null))
                                                                                       .build();

        return Game.create(new GameDefinition.Builder().build(), setup);
    }


    @Test
    void apply_commands_should_run_controllers_in_order_before_the_frame_advances() {
        List<String> log = new ArrayList<>();
        NoteType type = new NoteType(log);
        ControllerCommandSource source = new ControllerCommandSource(List.of(new ReplayController(type, Map.of(0, List.of(7, 8), 2, List.of(9))),
                                                                             new ComputerController(type)));
        Game game = createGame();

        game.getFrameRunner().setCommandSource(source);

        for (int frame = 0; frame < FRAME_COUNT; frame++) {
            game.getFrameRunner().runFrame();
        }

        Assertions.assertEquals(List.of("0 0 7", "0 0 8", "0 1 0", "2 0 9", "2 1 2"), log);
        Assertions.assertEquals(ControllerSource.REPLAY, source.getControllers().get(0).getSource());
    }
}
