package org.fogofwarlabs.sc4j.engine.lockstep;

import org.fogofwarlabs.sc4j.engine.command.*;
import org.fogofwarlabs.sc4j.engine.game.Game;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Tests the engine turn format: a {@link CommandFrameWriter} turn decodes into the same commands with the sender's state hash, and malformed
 * turns, unknown command ids, commands of an observer and commands of another player are refused.
 */
final class CommandFrameTest {
    private static final int NOTE_ID = 1;
    private static final int FRAME = 48;
    private static final long HASH = 0x0123_4567_89AB_CDEFL;

    /**
     * A test command type: a note with an int value.
     */
    private static final class NoteType implements CommandType<Integer> {
        @Override
        public int getId() {
            return NOTE_ID;
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
        }
    }

    /**
     * Logs the submitted notes with their player.
     */
    private static final class LogSink implements CommandSink {
        private final @NotNull List<String> log;

        private LogSink() {
            this.log = new ArrayList<>();
        }

        @Override
        public <C> void submit(@NotNull Player player, @NotNull CommandType<C> type, @NotNull C command) {
            this.log.add(player.getIndex() + ":" + command);
        }
    }

    private final @NotNull NoteType notes;
    private final @NotNull CommandRegistry<CommandType<?>> registry;
    private final @NotNull Player player;
    private final @NotNull LockstepPeer peer;

    CommandFrameTest() {
        this.notes    = new NoteType();
        this.registry = new CommandRegistry<>(NOTE_ID + 1);
        this.player   = new Player(2);
        this.peer     = new LockstepPeer(5);

        this.registry.register(this.notes);
        this.registry.freeze();
    }

    @Test
    void read_should_decode_the_commands_and_the_state_hash_a_writer_wrote() throws IOException {
        CommandFrameWriter writer = new CommandFrameWriter(this.player);
        LogSink sink = new LogSink();

        writer.submit(this.player, this.notes, 7);
        writer.submit(this.player, this.notes, -3);
        writer.setStateHash(FRAME, HASH);

        CommandFrame frame = CommandFrame.read(new PeerTurn(this.peer, writer.takeTurn()), this.player, this.registry);
        CommandFrame empty = CommandFrame.read(new PeerTurn(this.peer, writer.takeTurn()), this.player, this.registry);

        frame.submitTo(sink);

        Assertions.assertEquals(List.of("2:7", "2:-3"), sink.log);
        Assertions.assertTrue(frame.hasStateHash());
        Assertions.assertEquals(FRAME, frame.getHashFrame());
        Assertions.assertEquals(HASH, frame.getStateHash());
        Assertions.assertEquals(0, empty.getCommandCount());
        Assertions.assertEquals(0, writer.getQueuedCount());
    }

    @Test
    void read_should_refuse_unknown_ids_observer_commands_and_truncated_turns() {
        CommandFrameWriter writer = new CommandFrameWriter(this.player);

        writer.submit(this.player, this.notes, 1);

        byte[] turn = writer.takeTurn();
        byte[] unknown = turn.clone();
        byte[] truncated = new byte[turn.length - 1];

        // The type id of the first command follows the flags byte and the command count.
        unknown[1 + Short.BYTES] = 0;
        System.arraycopy(turn, 0, truncated, 0, truncated.length);

        Assertions.assertThrows(IOException.class, () -> CommandFrame.read(new PeerTurn(this.peer, unknown), this.player, this.registry));
        Assertions.assertThrows(IOException.class, () -> CommandFrame.read(new PeerTurn(this.peer, turn), null, this.registry));
        Assertions.assertThrows(IOException.class, () -> CommandFrame.read(new PeerTurn(this.peer, truncated), this.player, this.registry));
    }

    @Test
    void submit_should_refuse_a_player_the_local_participant_does_not_play() {
        CommandFrameWriter writer = new CommandFrameWriter(this.player);
        CommandFrameWriter observer = new CommandFrameWriter(null);

        Assertions.assertThrows(IllegalArgumentException.class, () -> writer.submit(new Player(3), this.notes, 1));
        Assertions.assertThrows(IllegalArgumentException.class, () -> observer.submit(this.player, this.notes, 1));
    }
}
