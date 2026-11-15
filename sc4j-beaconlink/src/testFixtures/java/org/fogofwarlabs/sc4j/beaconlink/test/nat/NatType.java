package org.fogofwarlabs.sc4j.beaconlink.test.nat;

/**
 * Lists the NAT behaviours the loopback emulator models: how mappings are created for outgoing datagrams and which incoming datagrams a
 * mapping lets through.
 */
public enum NatType {
    /**
     * One mapping per inside endpoint; any outside source may send to it.
     */
    FULL_CONE(false, false, false),
    /**
     * One mapping per inside endpoint; incoming datagrams only from hosts the inside already sent to.
     */
    ADDRESS_RESTRICTED(false, true, false),
    /**
     * One mapping per inside endpoint; incoming datagrams only from the addresses and ports the inside already sent to.
     */
    PORT_RESTRICTED(false, false, true),
    /**
     * A new mapping per destination address and port; incoming datagrams only from that destination.
     */
    SYMMETRIC(true, false, true);


    private final boolean mappingPerDestination;
    private final boolean filteringByHost;
    private final boolean filteringByAddress;

    /**
     * Creates a {@link NatType} instance.
     *
     * @param mappingPerDestination whether every destination gets its own mapping
     * @param filteringByHost       whether a mapping accepts only hosts the inside sent to
     * @param filteringByAddress    whether a mapping accepts only addresses and ports the inside sent to
     */
    NatType(boolean mappingPerDestination, boolean filteringByHost, boolean filteringByAddress) {
        this.mappingPerDestination = mappingPerDestination;
        this.filteringByHost       = filteringByHost;
        this.filteringByAddress    = filteringByAddress;
    }

    /**
     * @return {@code true} if every destination address and port gets its own mapping, {@code false} otherwise (one mapping serves every destination)
     */
    public boolean isMappingPerDestination() {
        return this.mappingPerDestination;
    }

    /**
     * @return {@code true} if a mapping accepts only datagrams from hosts the inside sent to, {@code false} otherwise
     */
    public boolean isFilteringByHost() {
        return this.filteringByHost;
    }

    /**
     * @return {@code true} if a mapping accepts only datagrams from addresses and ports the inside sent to, {@code false} otherwise
     */
    public boolean isFilteringByAddress() {
        return this.filteringByAddress;
    }
}
