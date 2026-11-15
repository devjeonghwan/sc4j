package org.fogofwarlabs.sc4j.engine.lockstep;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Runs the turn exchange of one turn step of a lockstep session over its transport: it tops the local turns in transit up to the session's
 * count, sending a turn of the outgoing source for each missing one, takes the next turn of every participant if all of them arrived, and tops
 * the turns in transit up again (the receive took one back). A step whose receive misses a turn sends nothing more and is retried as a whole.
 * <p>
 * The count of turns in transit is the latency in turn steps: a command the source took in a step runs that many steps later on every machine.
 * The first step therefore sends that many turns at once, the first one with the commands queued before it.
 */
public final class TurnExchange {
    private final @NotNull LockstepTransport transport;
    private final @NotNull TurnSource source;

    /**
     * Creates a {@link TurnExchange} instance.
     *
     * @param transport the transport of the session
     * @param source    the local participant's outgoing turns
     */
    public TurnExchange(@NotNull LockstepTransport transport, @NotNull TurnSource source) {
        this.transport = transport;
        this.source    = source;
    }

    /**
     * @return the transport of the session
     */
    public @NotNull LockstepTransport getTransport() {
        return this.transport;
    }


    /**
     * Runs the exchange of one turn step.
     *
     * @param turnsInTransit the local turns kept in transit; with {@code 0} the step sends nothing and only receives
     * @return the participants' turns of the step in execution order, or {@code null} while a participant's turn is missing
     */
    public @Nullable List<@NotNull PeerTurn> exchange(int turnsInTransit) {
        this.sendMissingTurns(turnsInTransit);

        List<PeerTurn> turns = this.transport.receiveTurns();

        if (turns == null) {
            return null;
        }

        this.sendMissingTurns(turnsInTransit);

        return turns;
    }

    private void sendMissingTurns(int turnsInTransit) {
        while (this.transport.getTurnsInTransit() < turnsInTransit) {
            this.transport.sendTurn(this.source.takeTurn());
        }
    }
}
