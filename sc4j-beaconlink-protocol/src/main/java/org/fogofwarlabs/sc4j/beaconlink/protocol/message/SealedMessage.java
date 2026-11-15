package org.fogofwarlabs.sc4j.beaconlink.protocol.message;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKey;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireReader;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Carries a whisper to one connection of a player through one of that connection's home peers: the sender seals the text for the
 * connection's whisper key (from the connection's signed presence) with a fresh X25519 key and ChaCha20-Poly1305, so the home peer sees only
 * the sender, the target and the size. The home peer relays the datagram unchanged to the target connection when it is registered there and
 * answers {@link ResultStatus#DELIVERED}, or {@link ResultStatus#UNKNOWN_TARGET} otherwise. A whisper to a player goes as one copy to each of
 * the player's live connections. Nothing is stored for an offline target.
 * <p>
 * Body: the target's key, the target's connection number (64-bit), the sender's ephemeral X25519 public key
 * ({@value #EPHEMERAL_KEY_LENGTH} bytes) and the sealed text (two-byte length, at most {@value #MAXIMUM_SEALED_LENGTH} bytes).
 */
public final class SealedMessage extends Message {
    /**
     * The length of the sender's ephemeral X25519 public key.
     */
    public static final int EPHEMERAL_KEY_LENGTH = 32;
    /**
     * The largest length of the sealed text.
     */
    public static final int MAXIMUM_SEALED_LENGTH = 512;

    /**
     * Reads the body.
     *
     * @param reader the body reader
     * @return the body, or {@code null} if the reader failed or the sealed text is too long
     */
    public static @Nullable SealedMessage read(@NotNull WireReader reader) {
        IdentityKey target = IdentityKey.read(reader);
        long targetConnection = reader.readInt64();
        byte[] ephemeralKey = reader.readBytes(EPHEMERAL_KEY_LENGTH);
        int length = reader.readUnsigned16();

        if (target == null || length > MAXIMUM_SEALED_LENGTH || reader.isFailed()) {
            return null;
        }

        byte[] sealed = reader.readBytes(length);

        return reader.isFailed() ? null : new SealedMessage(target, targetConnection, ephemeralKey, sealed);
    }


    private final @NotNull IdentityKey target;
    private final long targetConnection;
    private final byte @NotNull [] ephemeralKey;
    private final byte @NotNull [] sealed;

    /**
     * Creates a {@link SealedMessage} instance.
     *
     * @param target           the target's key
     * @param targetConnection the number of the target's connection the whisper is sealed for
     * @param ephemeralKey     the sender's ephemeral X25519 public key
     * @param sealed           the sealed text
     * @throws IllegalArgumentException if the ephemeral key has the wrong length or the sealed text is too long
     */
    public SealedMessage(@NotNull IdentityKey target, long targetConnection, byte @NotNull [] ephemeralKey, byte @NotNull [] sealed) {
        if (ephemeralKey.length != EPHEMERAL_KEY_LENGTH || sealed.length > MAXIMUM_SEALED_LENGTH) {
            throw Exceptions.newIllegalArgumentException("The ephemeral key has {0} bytes, not {1}, or the sealed text {2}, more than {3}.",
                                                         ephemeralKey.length, EPHEMERAL_KEY_LENGTH, sealed.length, MAXIMUM_SEALED_LENGTH);
        }

        this.target           = target;
        this.targetConnection = targetConnection;
        this.ephemeralKey     = ephemeralKey.clone();
        this.sealed           = sealed.clone();
    }

    /**
     * @return {@link MessageKind#SEALED}
     */
    @Override
    public @NotNull MessageKind getKind() {
        return MessageKind.SEALED;
    }

    /**
     * @return the target's key
     */
    public @NotNull IdentityKey getTarget() {
        return this.target;
    }

    /**
     * @return the number of the target's connection the whisper is sealed for
     */
    public long getTargetConnection() {
        return this.targetConnection;
    }

    /**
     * @return a copy of the sender's ephemeral X25519 public key
     */
    public byte @NotNull [] getEphemeralKey() {
        return this.ephemeralKey.clone();
    }

    /**
     * @return a copy of the sealed text
     */
    public byte @NotNull [] getSealed() {
        return this.sealed.clone();
    }


    /**
     * Writes the body fields.
     *
     * @param writer the writer
     */
    @Override
    public void writeBody(@NotNull WireWriter writer) {
        this.target.write(writer);
        writer.writeInt64(this.targetConnection);
        writer.writeBytes(this.ephemeralKey);
        writer.writeUnsigned16(this.sealed.length);
        writer.writeBytes(this.sealed);
    }
}
