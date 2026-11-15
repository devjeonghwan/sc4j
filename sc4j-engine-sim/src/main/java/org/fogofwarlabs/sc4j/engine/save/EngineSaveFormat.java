package org.fogofwarlabs.sc4j.engine.save;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.game.Game;
import org.fogofwarlabs.sc4j.engine.game.GameSetup;
import org.fogofwarlabs.sc4j.engine.game.GameSetupCodec;
import org.fogofwarlabs.sc4j.engine.mod.identity.IdentityFileFrame;
import org.fogofwarlabs.sc4j.engine.mod.identity.ModIdentity;
import org.fogofwarlabs.sc4j.engine.mod.identity.ModIdentityException;
import org.fogofwarlabs.sc4j.engine.state.snapshot.StateSnapshot;
import org.jetbrains.annotations.NotNull;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;

/**
 * Writes and reads the engine save format, the default {@link SaveFormat} of a mod without its own: the identity of the saving mod, the game
 * setup ({@link GameSetupCodec}) and the state snapshot of the game ({@link StateSnapshot}). A mod creates one format with its identity and
 * its setup codec; {@link #createSave} saves a running game, {@link #write} writes the file, {@link #read} reads it back and refuses a saved
 * game of another identity ({@link ModIdentityException}) or a damaged one; the mod then creates a game from the saved setup and restores the
 * snapshot into it ({@link EngineSave#restore}).
 * <p>
 * File layout ({@link DataOutputStream}, big-endian), inside the frame of the engine file formats ({@link IdentityFileFrame}): the magic
 * {@code SC4S} (four bytes), the format version (two bytes), the mod identity ({@link ModIdentity#write}), the setup
 * ({@link GameSetupCodec#write}), the state snapshot ({@link StateSnapshot#write}: frame, state hash, values length, values); the last four
 * bytes are the CRC-32 of everything before them.
 */
public final class EngineSaveFormat implements SaveFormat {
    /**
     * The name of the format for people.
     */
    public static final @NotNull String NAME = "SC4J engine save";
    /**
     * The file name extension of engine saved games, without the dot.
     */
    public static final @NotNull String FILE_EXTENSION = "sc4s";

    /**
     * The kind of file the identity refusal names.
     */
    private static final @NotNull String FILE_KIND = "saved game";
    /**
     * The name of the format in the messages.
     */
    private static final @NotNull String FORMAT_DESCRIPTION = "engine saved game";
    /**
     * The first four bytes of an engine saved game: {@code SC4S}.
     */
    private static final int MAGIC = 0x5343_3453;
    /**
     * The format version this engine writes and reads.
     */
    private static final int FORMAT_VERSION = 1;
    /**
     * The frame of engine saved games: magic, format version, identity and checksum.
     */
    private static final @NotNull IdentityFileFrame FRAME = new IdentityFileFrame(MAGIC, FORMAT_VERSION, FORMAT_DESCRIPTION, FILE_KIND);

    private final @NotNull ModIdentity identity;
    private final @NotNull GameSetupCodec setupCodec;

    /**
     * Creates an {@link EngineSaveFormat} instance.
     *
     * @param identity   the identity of the running mod: the saved games record it, a saved game of another identity is refused
     * @param setupCodec the codec of the mod's game setups
     */
    public EngineSaveFormat(@NotNull ModIdentity identity, @NotNull GameSetupCodec setupCodec) {
        this.identity   = identity;
        this.setupCodec = setupCodec;
    }

    /**
     * @return the identity of the running mod, which the saved games record and the reader requires
     */
    public @NotNull ModIdentity getIdentity() {
        return this.identity;
    }

    /**
     * @return the name of the format for people: {@value #NAME}
     */
    @Override
    public @NotNull String getName() {
        return NAME;
    }

    /**
     * @return the file name extension of the format's saved games, without the dot: {@value #FILE_EXTENSION}
     */
    @Override
    public @NotNull String getFileExtension() {
        return FILE_EXTENSION;
    }


    /**
     * Saves a game between two frames: the running mod's identity, the game's setup and its state snapshot.
     *
     * @param game the game
     * @return the created {@link EngineSave} instance
     * @throws IllegalStateException if a system of the game contributes to the state hash but writes no snapshot
     */
    public @NotNull EngineSave createSave(@NotNull Game game) {
        return new EngineSave(this.identity, game.getSetup(), StateSnapshot.capture(game));
    }

    /**
     * Tests whether file contents start like a saved game of this format: with the magic {@code SC4S}; the contents may still be damaged.
     *
     * @param contents the file contents
     * @return {@code true} if the contents start like a saved game of this format, {@code false} otherwise
     */
    @Override
    public boolean isSave(byte @NotNull [] contents) {
        return FRAME.matches(contents);
    }

    /**
     * Writes a saved game in the engine save format.
     *
     * @param save the saved game
     * @return the file contents
     * @throws IllegalArgumentException if the saved game's setup has no wire form ({@link GameSetupCodec#write})
     */
    public byte @NotNull [] write(@NotNull EngineSave save) {
        return FRAME.write(save.getIdentity(), output -> {
            this.setupCodec.write(save.getSetup(), output);
            save.getSnapshot().write(output);
        });
    }

    /**
     * Reads a saved game of the engine save format saved by the running mod's identity.
     *
     * @param contents the file contents
     * @return the created {@link EngineSave} instance
     * @throws ModIdentityException if the game was saved by another mod identity
     * @throws IOException          if the contents are no engine saved game, have another format version, or are truncated or damaged
     */
    public @NotNull EngineSave read(byte @NotNull [] contents) throws IOException {
        DataInputStream input = FRAME.openBody(contents, this.identity);

        try {
            GameSetup setup = this.setupCodec.read(input);
            StateSnapshot snapshot = StateSnapshot.read(input);

            if (input.available() != 0) {
                throw Exceptions.newIOException("The engine saved game has {0} bytes after its state snapshot.", input.available());
            }

            return new EngineSave(this.identity, setup, snapshot);
        } catch (EOFException exception) {
            throw Exceptions.newIOException(exception, "The engine saved game ends inside its contents.");
        }
    }

    /**
     * Reads what a load list shows of a saved game: its map, the frame it was saved at and its players. The summary of a saved game of another
     * mod identity is refused like the saved game.
     *
     * @param contents the file contents
     * @return the created {@link SaveSummary} instance
     * @throws ModIdentityException if the game was saved by another mod identity
     * @throws IOException          if the contents are no engine saved game, have another format version, or are truncated or damaged
     */
    @Override
    public @NotNull SaveSummary readSummary(byte @NotNull [] contents) throws IOException {
        DataInputStream input = FRAME.openBody(contents, this.identity);

        try {
            GameSetup setup = this.setupCodec.read(input);

            return new SaveSummary(setup.getMap().getName(), Integer.toUnsignedLong(input.readInt()), setup.getPlayers().size());
        } catch (EOFException exception) {
            throw Exceptions.newIOException(exception, "The engine saved game ends inside its header.");
        }
    }
}
