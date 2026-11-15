package org.fogofwarlabs.sc4j.beaconlink.peer;

import org.fogofwarlabs.sc4j.beaconlink.protocol.message.MessageKind;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.Map;

/**
 * Counts what a peer did with the datagrams it received: the handled messages per kind and the dropped datagrams per reason. The operator
 * reads them in the log; they are read on the peer's thread.
 */
public final class PeerStatistics {
    private final @NotNull EnumMap<@NotNull MessageKind, @NotNull Long> handled;
    private final @NotNull EnumMap<@NotNull DropReason, @NotNull Long> dropped;

    /**
     * Creates a {@link PeerStatistics} instance with every count at {@code 0}.
     */
    PeerStatistics() {
        this.handled = new EnumMap<>(MessageKind.class);
        this.dropped = new EnumMap<>(DropReason.class);
    }

    /**
     * Reads the count of handled messages of a kind.
     *
     * @param kind the message kind
     * @return the number of handled messages of the kind
     */
    public long getHandledCount(@NotNull MessageKind kind) {
        return this.handled.getOrDefault(kind, 0L);
    }

    /**
     * Reads the count of dropped datagrams of a reason.
     *
     * @param reason the reason
     * @return the number of datagrams dropped for the reason
     */
    public long getDroppedCount(@NotNull DropReason reason) {
        return this.dropped.getOrDefault(reason, 0L);
    }


    /**
     * Counts a handled message.
     *
     * @param kind the message kind
     */
    void countHandled(@NotNull MessageKind kind) {
        this.handled.merge(kind, 1L, Long::sum);
    }

    /**
     * Counts a dropped datagram.
     *
     * @param reason the reason
     */
    void countDropped(@NotNull DropReason reason) {
        this.dropped.merge(reason, 1L, Long::sum);
    }

    /**
     * @return the non-zero counts, for the log
     */
    @Override
    public @NotNull String toString() {
        StringBuilder text = new StringBuilder("handled");

        for (Map.Entry<MessageKind, Long> entry : this.handled.entrySet()) {
            text.append(' ').append(entry.getKey()).append('=').append(entry.getValue());
        }

        text.append(", dropped");

        for (Map.Entry<DropReason, Long> entry : this.dropped.entrySet()) {
            text.append(' ').append(entry.getKey()).append('=').append(entry.getValue());
        }

        return text.toString();
    }
}
