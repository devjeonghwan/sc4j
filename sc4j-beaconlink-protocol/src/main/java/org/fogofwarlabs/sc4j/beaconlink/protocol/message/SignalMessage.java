package org.fogofwarlabs.sc4j.beaconlink.protocol.message;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKey;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireReader;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Sends a signal to another client connection registered at the same node: the handshake of a peer channel. The signal names one connection
 * of the target's key; the node relays the signed datagram unchanged to the registered address of that connection and answers the sender
 * {@link ResultStatus#DELIVERED} or {@link ResultStatus#UNKNOWN_TARGET}; the target checks the original sender's signature. The payload
 * belongs to the clients; nodes do not interpret it.
 * <p>
 * Body: the target's key, the target's connection number (eight bytes) and the payload (two-byte length, at most
 * {@value #MAXIMUM_PAYLOAD_LENGTH} bytes).
 */
public final class SignalMessage extends Message {
    /**
     * The largest payload length.
     */
    public static final int MAXIMUM_PAYLOAD_LENGTH = 512;

    /**
     * Reads the body.
     *
     * @param reader the body reader
     * @return the body, or {@code null} if the reader failed or the payload is too long
     */
    public static @Nullable SignalMessage read(@NotNull WireReader reader) {
        IdentityKey target = IdentityKey.read(reader);
        long targetConnection = reader.readInt64();
        int length = reader.readUnsigned16();

        if (length > MAXIMUM_PAYLOAD_LENGTH) {
            reader.fail();
        }

        byte[] payload = reader.readBytes(length);

        if (target == null || reader.isFailed()) {
            return null;
        }

        return new SignalMessage(target, targetConnection, payload);
    }


    private final @NotNull IdentityKey target;
    private final long targetConnection;
    private final byte @NotNull [] payload;

    /**
     * Creates a {@link SignalMessage} instance.
     *
     * @param target           the key of the receiving client
     * @param targetConnection the connection number of the receiving client
     * @param payload          the payload
     * @throws IllegalArgumentException if the payload is longer than {@value #MAXIMUM_PAYLOAD_LENGTH} bytes
     */
    public SignalMessage(@NotNull IdentityKey target, long targetConnection, byte @NotNull [] payload) {
        if (payload.length > MAXIMUM_PAYLOAD_LENGTH) {
            throw Exceptions.newIllegalArgumentException("The payload has {0} bytes, more than {1}.", payload.length, MAXIMUM_PAYLOAD_LENGTH);
        }

        this.target           = target;
        this.targetConnection = targetConnection;
        this.payload          = payload.clone();
    }

    /**
     * @return {@link MessageKind#SIGNAL}
     */
    @Override
    public @NotNull MessageKind getKind() {
        return MessageKind.SIGNAL;
    }

    /**
     * @return the key of the receiving client
     */
    public @NotNull IdentityKey getTarget() {
        return this.target;
    }

    /**
     * @return the connection number of the receiving client
     */
    public long getTargetConnection() {
        return this.targetConnection;
    }

    /**
     * @return a copy of the payload
     */
    public byte @NotNull [] getPayload() {
        return this.payload.clone();
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
        writer.writeUnsigned16(this.payload.length);
        writer.writeBytes(this.payload);
    }
}
