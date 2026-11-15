package org.fogofwarlabs.sc4j.engine.lockstep;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * Connects the participants of a lockstep session inside one process: the engine's in-memory {@link LockstepTransport}, for tests, local
 * multi-machine simulations and a single-machine session (one participant, its turns come back to it). A sent turn is queued for every
 * participant at once; a receive takes the next turn of every participant once each has one, in the participants' list order.
 * <p>
 * A participant that left ({@link LockstepTransport#leave}) is no longer waited for once its queued turns were received, and each other
 * participant's transport reports its departure ({@link LockstepTransport#takeDepartures()}) at that receive; its own transport accepts no
 * further call. The network is not thread-safe: the participants run on one thread, or the caller synchronizes them.
 */
public final class InMemoryLockstepNetwork {
    /**
     * The transport of one participant.
     */
    private final class Transport implements LockstepTransport {
        private final int index;
        private final @NotNull List<@NotNull ArrayDeque<byte @NotNull []>> inbox;
        private final boolean @NotNull [] departureReported;
        private final @NotNull List<@NotNull PeerDeparture> departures;

        private int sent;
        private int received;
        private boolean left;
        private int leaveReason;

        private Transport(int index, int participantCount) {
            this.index = index;
            this.inbox = new ArrayList<>(participantCount);

            for (int sender = 0; sender < participantCount; sender++) {
                this.inbox.add(new ArrayDeque<>());
            }

            this.departureReported = new boolean[participantCount];
            this.departures        = new ArrayList<>();

            this.sent        = 0;
            this.received    = 0;
            this.left        = false;
            this.leaveReason = PeerDeparture.NO_REASON;
        }

        /**
         * @return the local turns sent and not yet received back
         */
        @Override
        public int getTurnsInTransit() {
            return this.sent - this.received;
        }

        /**
         * Sends the local participant's next turn.
         *
         * @param turn the turn bytes
         * @throws IllegalStateException if the participant left
         */
        @Override
        public void sendTurn(byte @NotNull [] turn) {
            this.requireJoined();

            byte[] copy = turn.clone();

            for (Transport receiver : InMemoryLockstepNetwork.this.transports) {
                receiver.inbox.get(this.index).addLast(copy);
            }

            this.sent++;
        }

        /**
         * Takes the next turn of every participant once all of them arrived.
         *
         * @return the participants' turns in execution order, or {@code null} while a participant's turn is missing
         * @throws IllegalStateException if the participant left
         */
        @Override
        public @Nullable List<@NotNull PeerTurn> receiveTurns() {
            this.requireJoined();

            List<Transport> transports = InMemoryLockstepNetwork.this.transports;

            for (int sender = 0; sender < this.inbox.size(); sender++) {
                if (this.inbox.get(sender).isEmpty() && !transports.get(sender).left) {
                    return null;
                }
            }

            List<PeerTurn> turns = new ArrayList<>(this.inbox.size());

            for (int sender = 0; sender < this.inbox.size(); sender++) {
                byte[] turn = this.inbox.get(sender).pollFirst();

                if (turn != null) {
                    turns.add(new PeerTurn(InMemoryLockstepNetwork.this.peers.get(sender), turn));
                } else if (!this.departureReported[sender]) {
                    this.departureReported[sender] = true;
                    this.departures.add(new PeerDeparture(InMemoryLockstepNetwork.this.peers.get(sender), DepartureKind.LEFT,
                                                          transports.get(sender).leaveReason));
                }
            }

            this.received++;

            return turns;
        }

        /**
         * Takes the departures of the participants that the receives since the previous call no longer waited for: a departure is reported at
         * the first receive without a turn of the participant, on every machine the same turn step. A transport without departures reports
         * none.
         *
         * @return the departures in the order of the receives, the participants' execution order within one receive; empty if there is none
         */
        @Override
        public @NotNull List<@NotNull PeerDeparture> takeDepartures() {
            List<PeerDeparture> taken = List.copyOf(this.departures);

            this.departures.clear();

            return taken;
        }

        /**
         * Leaves the session: the other participants take the local participant's departure ({@link #takeDepartures()}) once they received
         * every turn sent before it. A transport without departures ignores it.
         *
         * @param reason the departure reason, the session's own code
         */
        @Override
        public void leave(int reason) {
            this.left        = true;
            this.leaveReason = reason;
        }

        private void requireJoined() {
            if (this.left) {
                throw Exceptions.newIllegalStateException("{0} left the in-memory session.", InMemoryLockstepNetwork.this.peers.get(this.index));
            }
        }
    }

    private final @NotNull List<@NotNull LockstepPeer> peers;
    private final @NotNull List<@NotNull Transport> transports;

    /**
     * Creates an {@link InMemoryLockstepNetwork} instance with one transport per participant.
     *
     * @param peers the participants in execution order, copied
     * @throws IllegalArgumentException if the list is empty or names a participant twice
     */
    public InMemoryLockstepNetwork(@NotNull List<@NotNull LockstepPeer> peers) {
        if (peers.isEmpty()) {
            throw Exceptions.newIllegalArgumentException("An in-memory lockstep session needs a participant.");
        }

        for (int index = 0; index < peers.size(); index++) {
            for (int other = 0; other < index; other++) {
                if (peers.get(other) == peers.get(index) || peers.get(other).getNumber() == peers.get(index).getNumber()) {
                    throw Exceptions.newIllegalArgumentException("{0} takes part twice.", peers.get(index));
                }
            }
        }

        this.peers      = List.copyOf(peers);
        this.transports = new ArrayList<>(peers.size());

        for (int index = 0; index < peers.size(); index++) {
            this.transports.add(new Transport(index, peers.size()));
        }
    }

    /**
     * @return the participants in execution order
     */
    public @NotNull List<@NotNull LockstepPeer> getPeers() {
        return this.peers;
    }

    /**
     * Returns the transport of one participant.
     *
     * @param peer the participant
     * @return the participant's transport
     * @throws IllegalArgumentException if the peer does not take part
     */
    public @NotNull LockstepTransport getTransport(@NotNull LockstepPeer peer) {
        int index = this.peers.indexOf(peer);

        if (index < 0) {
            throw Exceptions.newIllegalArgumentException("{0} does not take part in the in-memory session.", peer);
        }

        return this.transports.get(index);
    }
}
