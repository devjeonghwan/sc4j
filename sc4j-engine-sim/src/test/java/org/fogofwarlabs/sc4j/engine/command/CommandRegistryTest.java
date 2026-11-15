package org.fogofwarlabs.sc4j.engine.command;

import org.fogofwarlabs.sc4j.engine.game.*;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Tests that a command type reads back the bytes it writes and that a {@link CommandRegistry} finds and runs the type of a command id.
 */
final class CommandRegistryTest {
    private static final int ID_COUNT = 4;
    private static final int RALLY_ID = 2;
    private static final int RALLY_X = 0x1234;
    private static final int RALLY_Y = 70000;

    /**
     * A test command: a rally point, a WORD x and a DWORD y.
     */
    private static final class Rally {
        private final int x;
        private final int y;

        private Rally(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    /**
     * The type of the test command; it records every executed command.
     */
    private static final class RallyType implements CommandType<Rally> {
        private final @NotNull List<String> executed;

        private RallyType() {
            this.executed = new ArrayList<>();
        }

        @Override
        public int getId() {
            return RALLY_ID;
        }

        @Override
        public @NotNull Rally read(@NotNull CommandInput input) throws IOException {
            int x = input.readUnsignedShort();
            int y = input.readInt();

            return new Rally(x, y);
        }

        @Override
        public void write(@NotNull Rally command, @NotNull CommandOutput output) {
            output.writeShort(command.x).writeInt(command.y);
        }

        @Override
        public void execute(@NotNull Game game, @NotNull Player player, @NotNull Rally command) {
            this.executed.add(player.getIndex() + ":" + command.x + "," + command.y);
        }
    }

    private static @NotNull Game createGame() {
        GameSetup setup = new GameSetup.Builder(new MapReference("empty", new byte[0])).addPlayer(new PlayerSlot(0, ControllerKind.HUMAN, 0, 0, null))
                                                                                       .addPlayer(new PlayerSlot(1, ControllerKind.HUMAN, 1, 1, null))
                                                                                       .build();

        return Game.create(new GameDefinition.Builder().build(), setup);
    }


    @Test
    void command_payloads_round_trip_through_write_and_read() throws IOException {
        RallyType type = new RallyType();
        CommandOutput output = new CommandOutput();

        type.write(new Rally(RALLY_X, RALLY_Y), output);

        byte[] payload = output.toByteArray();

        Assertions.assertArrayEquals(new byte[]{0x34, 0x12, 0x70, 0x11, 0x01, 0x00}, payload);

        Rally read = type.read(new CommandInput(payload));
        CommandOutput again = new CommandOutput();

        type.write(read, again);

        Assertions.assertArrayEquals(payload, again.toByteArray());
    }

    @Test
    void read_int_should_fail_past_the_payload_and_keep_the_position() {
        CommandInput input = new CommandInput(new byte[]{1, 2, 3});

        Assertions.assertThrows(IOException.class, input::readInt);
        Assertions.assertEquals(0, input.getPosition());
        Assertions.assertEquals(3, input.getRemaining());
    }

    @Test
    void registry_finds_the_type_of_an_id_and_runs_its_commands() throws IOException {
        RallyType type = new RallyType();
        CommandRegistry<CommandType<?>> registry = new CommandRegistry<>(ID_COUNT);
        Game game = createGame();
        CommandOutput output = new CommandOutput();

        registry.register(type);
        registry.freeze();
        type.write(new Rally(1, 2), output);

        CommandRegistry.readAndExecute(type, game, game.getPlayer(1), new CommandInput(output.toByteArray()));

        Assertions.assertSame(type, registry.get(RALLY_ID));
        Assertions.assertNull(registry.find(0));
        Assertions.assertNull(registry.find(ID_COUNT));
        Assertions.assertEquals(List.of("1:1,2"), type.executed);
        Assertions.assertThrows(IllegalStateException.class, () -> registry.register(new RallyType()));
    }

    @Test
    void register_should_reject_a_second_type_of_one_id() {
        CommandRegistry<CommandType<?>> registry = new CommandRegistry<>(ID_COUNT);

        registry.register(new RallyType());

        Assertions.assertThrows(IllegalArgumentException.class, () -> registry.register(new RallyType()));
    }
}
