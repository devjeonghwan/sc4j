package org.fogofwarlabs.sc4j.engine.net;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.BiPredicate;

/**
 * Wraps a datagram endpoint with a link that loses, duplicates and reorders the sent datagrams by a seeded random choice: a held datagram goes
 * out after later ones. A target can be blocked completely, and a loss rule can lose chosen datagrams.
 */
public final class LossyDatagramEndpoint implements DatagramEndpoint {
    private static final int PERCENT = 100;

    /**
     * Holds a datagram that goes out later.
     */
    private static final class HeldDatagram {
        private final byte @NotNull [] bytes;
        private final @NotNull InetSocketAddress target;

        private HeldDatagram(byte @NotNull [] bytes, @NotNull InetSocketAddress target) {
            this.bytes  = bytes;
            this.target = target;
        }
    }

    private final @NotNull DatagramEndpoint endpoint;
    private final @NotNull Random random;
    private final int dropPercent;
    private final int duplicatePercent;
    private final int holdPercent;

    private final @NotNull List<@NotNull HeldDatagram> held;
    private final @NotNull List<@NotNull InetSocketAddress> blocked;
    private @Nullable BiPredicate<byte @NotNull [], @NotNull InetSocketAddress> lossRule;
    private int lostCount;

    /**
     * Creates a {@link LossyDatagramEndpoint} instance.
     *
     * @param endpoint         the wrapped endpoint
     * @param seed             the seed of the link's choices
     * @param dropPercent      the share of datagrams lost, in percent
     * @param duplicatePercent the share of datagrams sent twice, in percent
     * @param holdPercent      the share of datagrams sent after later ones, in percent
     */
    public LossyDatagramEndpoint(@NotNull DatagramEndpoint endpoint, long seed, int dropPercent, int duplicatePercent, int holdPercent) {
        this.endpoint         = endpoint;
        this.random           = new Random(seed);
        this.dropPercent      = dropPercent;
        this.duplicatePercent = duplicatePercent;
        this.holdPercent      = holdPercent;

        this.held      = new ArrayList<>();
        this.blocked   = new ArrayList<>();
        this.lossRule  = null;
        this.lostCount = 0;
    }

    /**
     * @return the local address the endpoint receives at
     * @throws IOException if the endpoint is closed
     */
    @Override
    public @NotNull InetSocketAddress getLocalAddress() throws IOException {
        return this.endpoint.getLocalAddress();
    }

    /**
     * @return the datagrams the link lost, blocked ones included
     */
    public int getLostCount() {
        return this.lostCount;
    }

    /**
     * Sets whether every datagram to a target is lost.
     *
     * @param target  the target
     * @param blocked {@code true} to lose every datagram to the target, {@code false} to send them again
     */
    public void setBlocked(@NotNull InetSocketAddress target, boolean blocked) {
        this.blocked.remove(target);

        if (blocked) {
            this.blocked.add(target);
        }
    }

    /**
     * Sets a rule that loses the datagrams it accepts, besides the blocked targets and the random losses.
     *
     * @param lossRule the rule over the datagram's bytes and its target, or {@code null} for none
     */
    public void setLossRule(@Nullable BiPredicate<byte @NotNull [], @NotNull InetSocketAddress> lossRule) {
        this.lossRule = lossRule;
    }


    /**
     * Sends one datagram through the lossy link: it is lost, sent twice, held for later or sent; a held datagram may go out after it.
     *
     * @param datagram the datagram, read from its position to its limit
     * @param target   the receiver's address
     * @throws IOException if the endpoint is closed or the datagram cannot be sent
     */
    @Override
    public void send(@NotNull ByteBuffer datagram, @NotNull InetSocketAddress target) throws IOException {
        byte[] bytes = new byte[datagram.remaining()];
        int choice = this.random.nextInt(PERCENT);

        datagram.get(bytes);

        BiPredicate<byte[], InetSocketAddress> rule = this.lossRule;

        if (this.blocked.contains(target) || rule != null && rule.test(bytes, target) || choice < this.dropPercent) {
            this.lostCount++;
        } else if (choice < this.dropPercent + this.duplicatePercent) {
            this.forward(bytes, target);
            this.forward(bytes, target);
        } else if (choice < this.dropPercent + this.duplicatePercent + this.holdPercent) {
            this.held.add(new HeldDatagram(bytes, target));
        } else {
            this.forward(bytes, target);
        }

        if (!this.held.isEmpty() && this.random.nextBoolean()) {
            this.releaseHeld();
        }
    }

    /**
     * Receives the next waiting datagram of the wrapped endpoint; a held datagram goes out first.
     *
     * @param buffer the buffer that receives the datagram
     * @return the sender's address, or {@code null} if no datagram is waiting
     * @throws IOException if the endpoint is closed
     */
    @Override
    public @Nullable InetSocketAddress receive(@NotNull ByteBuffer buffer) throws IOException {
        if (!this.held.isEmpty()) {
            this.releaseHeld();
        }

        return this.endpoint.receive(buffer);
    }

    private void releaseHeld() throws IOException {
        HeldDatagram datagram = this.held.removeFirst();

        if (this.blocked.contains(datagram.target)) {
            this.lostCount++;
        } else {
            this.forward(datagram.bytes, datagram.target);
        }
    }

    private void forward(byte @NotNull [] bytes, @NotNull InetSocketAddress target) throws IOException {
        this.endpoint.send(ByteBuffer.wrap(bytes), target);
    }

    /**
     * Closes the wrapped endpoint.
     *
     * @throws IOException if the endpoint cannot be closed
     */
    @Override
    public void close() throws IOException {
        this.endpoint.close();
    }
}
