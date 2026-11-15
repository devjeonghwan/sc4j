package org.fogofwarlabs.sc4j.beaconlink.protocol.message;

import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireReader;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireWriter;
import org.jetbrains.annotations.NotNull;

/**
 * Checks a node: the node answers with a {@link PongMessage}, from which the sender learns that the node is reachable, its key, its software
 * version and the sender's observed address. A ping needs no registration; like a first registration it must be padded to the length of the
 * answer.
 * <p>
 * Body: empty.
 */
public final class PingMessage extends Message {
    /**
     * Reads the body.
     *
     * @param reader the body reader
     * @return the body
     */
    public static @NotNull PingMessage read(@NotNull WireReader reader) {
        return new PingMessage();
    }


    /**
     * Creates a {@link PingMessage} instance.
     */
    public PingMessage() {
    }

    /**
     * @return {@link MessageKind#PING}
     */
    @Override
    public @NotNull MessageKind getKind() {
        return MessageKind.PING;
    }


    /**
     * Writes the body fields.
     *
     * @param writer the writer
     */
    @Override
    public void writeBody(@NotNull WireWriter writer) {
    }
}
