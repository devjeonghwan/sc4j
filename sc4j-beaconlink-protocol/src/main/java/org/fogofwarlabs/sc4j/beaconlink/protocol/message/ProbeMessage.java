package org.fogofwarlabs.sc4j.beaconlink.protocol.message;

import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireReader;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Probes the address a forwarded {@link DialBackMessage} named: the third peer sends it unrequested to a peer it never heard from. It is
 * never answered and is shorter than the forwarded request.
 * <p>
 * Body: the requester's nonce (8).
 */
public final class ProbeMessage extends Message {
    /**
     * Reads the body.
     *
     * @param reader the body reader
     * @return the body, or {@code null} if the reader failed
     */
    public static @Nullable ProbeMessage read(@NotNull WireReader reader) {
        long nonce = reader.readInt64();

        return reader.isFailed() ? null : new ProbeMessage(nonce);
    }


    private final long nonce;

    /**
     * Creates a {@link ProbeMessage} instance.
     *
     * @param nonce the nonce of the dial-back request
     */
    public ProbeMessage(long nonce) {
        this.nonce = nonce;
    }

    /**
     * @return {@link MessageKind#PROBE}
     */
    @Override
    public @NotNull MessageKind getKind() {
        return MessageKind.PROBE;
    }

    /**
     * @return the nonce of the dial-back request
     */
    public long getNonce() {
        return this.nonce;
    }


    /**
     * Writes the body fields.
     *
     * @param writer the writer
     */
    @Override
    public void writeBody(@NotNull WireWriter writer) {
        writer.writeInt64(this.nonce);
    }
}
