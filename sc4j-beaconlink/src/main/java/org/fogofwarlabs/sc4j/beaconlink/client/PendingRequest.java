package org.fogofwarlabs.sc4j.beaconlink.client;

import org.fogofwarlabs.sc4j.beaconlink.protocol.message.Message;
import org.jetbrains.annotations.NotNull;

/**
 * Holds one request that waits for its reply: the node, the body and whether it is padded, the owner of the outcome, the time it was sent,
 * whether it was resent, the sends left and the time of the next send. A resend is encoded again, with a new timestamp and nonce, so that the
 * node's replay guard accepts it; it keeps the request number, so the reply to a resent request gives no round trip time.
 */
final class PendingRequest {
    private final long number;
    private final @NotNull NodeSession node;
    private final @NotNull Message message;
    private final boolean padded;
    private final @NotNull RequestOwner owner;
    private final long sentMillis;

    private boolean resent;
    private int sendsLeft;
    private long nextSendMillis;

    /**
     * Creates a {@link PendingRequest} instance for a request that was just sent.
     *
     * @param number         the request number
     * @param node           the node
     * @param message        the body
     * @param padded         whether the request is padded to the length of an unvalidated answer
     * @param owner          the owner of the outcome
     * @param sentMillis     the protocol clock time of the send
     * @param sendsLeft      the resends left
     * @param nextSendMillis the protocol clock time of the next resend
     */
    PendingRequest(long number, @NotNull NodeSession node, @NotNull Message message, boolean padded, @NotNull RequestOwner owner, long sentMillis,
                   int sendsLeft, long nextSendMillis) {
        this.number     = number;
        this.node       = node;
        this.message    = message;
        this.padded     = padded;
        this.owner      = owner;
        this.sentMillis = sentMillis;

        this.resent         = false;
        this.sendsLeft      = sendsLeft;
        this.nextSendMillis = nextSendMillis;
    }

    /**
     * @return the request number
     */
    long getNumber() {
        return this.number;
    }

    /**
     * @return the node
     */
    @NotNull NodeSession getNode() {
        return this.node;
    }

    /**
     * @return the body
     */
    @NotNull Message getMessage() {
        return this.message;
    }

    /**
     * @return {@code true} if the request is padded to the length of an unvalidated answer, {@code false} otherwise
     */
    boolean isPadded() {
        return this.padded;
    }

    /**
     * @return the owner of the outcome
     */
    @NotNull RequestOwner getOwner() {
        return this.owner;
    }

    /**
     * @return the protocol clock time of the first send
     */
    long getSentMillis() {
        return this.sentMillis;
    }

    /**
     * @return {@code true} if the request was sent again, so that its reply may answer either send, {@code false} otherwise
     */
    boolean isResent() {
        return this.resent;
    }

    /**
     * @return the protocol clock time of the next resend, or of the timeout when no send is left
     */
    long getNextSendMillis() {
        return this.nextSendMillis;
    }

    /**
     * @return {@code true} if the request may be sent again, {@code false} otherwise (it timed out at its next send time)
     */
    boolean hasSendsLeft() {
        return this.sendsLeft > 0;
    }


    /**
     * Counts a resend and schedules the next one.
     *
     * @param nextSendMillis the protocol clock time of the next resend or the timeout
     */
    void countResend(long nextSendMillis) {
        this.resent = true;
        this.sendsLeft--;
        this.nextSendMillis = nextSendMillis;
    }
}
