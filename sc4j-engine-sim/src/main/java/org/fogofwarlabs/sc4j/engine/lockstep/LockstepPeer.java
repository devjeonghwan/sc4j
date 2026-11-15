package org.fogofwarlabs.sc4j.engine.lockstep;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.jetbrains.annotations.NotNull;

/**
 * Identifies one participant of a lockstep session: a machine that sends a turn at every turn step and receives the turns of every other
 * participant. A participant may play a player of the game or only watch it (an observer); which player a peer plays is the session's
 * mapping ({@link LockstepSession}), because every machine has its own game and its own player objects.
 * <p>
 * The peer number is the participant's key on the wire. Instances compare by identity: a transport hands out the same instance for a
 * participant in every received turn.
 */
public final class LockstepPeer {
    private final int number;

    /**
     * Creates a {@link LockstepPeer} instance.
     *
     * @param number the peer number, {@code 0} or more: the participant's key on the wire
     * @throws IllegalArgumentException if the number is negative
     */
    public LockstepPeer(int number) {
        if (number < 0) {
            throw Exceptions.newIllegalArgumentException("Invalid peer number: {0}", number);
        }

        this.number = number;
    }

    /**
     * @return the peer number, the participant's key on the wire
     */
    public int getNumber() {
        return this.number;
    }


    /**
     * @return the peer number, for diagnostics
     */
    @Override
    public @NotNull String toString() {
        return "Peer " + this.number;
    }
}
