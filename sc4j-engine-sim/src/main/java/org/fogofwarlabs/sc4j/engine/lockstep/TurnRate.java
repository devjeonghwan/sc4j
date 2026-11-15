package org.fogofwarlabs.sc4j.engine.lockstep;

import org.jetbrains.annotations.NotNull;

/**
 * Describes the turn rate of a network lockstep session in turns per second: the rate the session's room chose and the defaults of the
 * network the session runs on (its turns per second and the local turns it keeps in transit). A mod's game speed derives its turn timing
 * ({@link TurnSchedule}) from them. A value of {@code 0} means none; the values are kept as the network names them.
 */
public final class TurnRate {
    /**
     * The turn rate of a transport that names none.
     */
    public static final @NotNull TurnRate UNSPECIFIED = new TurnRate(0, 0, 0);

    private final int turnsPerSecond;
    private final int networkTurnsPerSecond;
    private final int networkTurnsInTransit;

    /**
     * Creates a {@link TurnRate} instance.
     *
     * @param turnsPerSecond        the turns per second the session's room chose, {@code 0} for none
     * @param networkTurnsPerSecond the network's default turns per second, {@code 0} for none
     * @param networkTurnsInTransit the network's default local turns in transit, {@code 0} for none
     */
    public TurnRate(int turnsPerSecond, int networkTurnsPerSecond, int networkTurnsInTransit) {
        this.turnsPerSecond        = turnsPerSecond;
        this.networkTurnsPerSecond = networkTurnsPerSecond;
        this.networkTurnsInTransit = networkTurnsInTransit;
    }

    /**
     * @return the turns per second the session's room chose, {@code 0} when it chose none
     */
    public int getTurnsPerSecond() {
        return this.turnsPerSecond;
    }

    /**
     * @return the network's default turns per second, {@code 0} when the network names none
     */
    public int getNetworkTurnsPerSecond() {
        return this.networkTurnsPerSecond;
    }

    /**
     * @return the network's default local turns in transit, {@code 0} when the network names none
     */
    public int getNetworkTurnsInTransit() {
        return this.networkTurnsInTransit;
    }


    /**
     * @return the three rates, for diagnostics
     */
    @Override
    public @NotNull String toString() {
        return "TurnRate[" + this.turnsPerSecond + " turns per second, network " + this.networkTurnsPerSecond + " turns per second, " +
               this.networkTurnsInTransit + " turns in transit]";
    }
}
