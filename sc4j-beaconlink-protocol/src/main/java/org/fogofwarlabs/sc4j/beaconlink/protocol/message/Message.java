package org.fogofwarlabs.sc4j.beaconlink.protocol.message;

import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireWriter;
import org.jetbrains.annotations.NotNull;

/**
 * Holds the body of one Beacon.Link message. Each kind has its own final body class that validates its fields when created and reads itself
 * from the wire without throwing ({@link Envelope#decode}); the signed {@link Envelope} around it is written by {@link MessageEncoder}.
 */
public abstract class Message {
    /**
     * Creates a {@link Message} instance.
     */
    protected Message() {
    }

    /**
     * @return the kind of the message
     */
    public abstract @NotNull MessageKind getKind();


    /**
     * Writes the body fields.
     *
     * @param writer the writer
     */
    public abstract void writeBody(@NotNull WireWriter writer);
}
