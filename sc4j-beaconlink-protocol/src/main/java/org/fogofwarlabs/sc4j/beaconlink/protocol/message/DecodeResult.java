package org.fogofwarlabs.sc4j.beaconlink.protocol.message;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Holds the outcome of {@link Envelope#decode}: the decoded envelope, or why the datagram was rejected.
 */
public final class DecodeResult {
    /**
     * Creates a {@link DecodeResult} instance for a decoded envelope.
     *
     * @param envelope the envelope
     * @return the created {@link DecodeResult} instance
     */
    static @NotNull DecodeResult decoded(@NotNull Envelope envelope) {
        return new DecodeResult(envelope, null);
    }

    /**
     * Creates a {@link DecodeResult} instance for a rejected datagram.
     *
     * @param failure why the datagram was rejected
     * @return the created {@link DecodeResult} instance
     */
    static @NotNull DecodeResult rejected(@NotNull DecodeFailure failure) {
        return new DecodeResult(null, failure);
    }


    private final @Nullable Envelope envelope;
    private final @Nullable DecodeFailure failure;

    private DecodeResult(@Nullable Envelope envelope, @Nullable DecodeFailure failure) {
        this.envelope = envelope;
        this.failure  = failure;
    }

    /**
     * @return the decoded envelope, or {@code null} if the datagram was rejected
     */
    public @Nullable Envelope getEnvelope() {
        return this.envelope;
    }

    /**
     * @return why the datagram was rejected, or {@code null} if it was decoded
     */
    public @Nullable DecodeFailure getFailure() {
        return this.failure;
    }
}
