package org.fogofwarlabs.sc4j.beaconlink.peer.store;

import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKey;
import org.fogofwarlabs.sc4j.beaconlink.protocol.room.ModFingerprint;
import org.jetbrains.annotations.NotNull;

import java.net.InetSocketAddress;

/**
 * Holds one client registration of a node: the client's key, the source address it proved with its address token, the connection number of
 * its client session and when that connection started, its display name and mod fingerprint, and the expiry.
 */
public final class Registration {
    private final @NotNull IdentityKey key;
    private final @NotNull InetSocketAddress address;
    private final long connection;
    private final long connectionStartMillis;

    private final @NotNull String displayName;
    private final @NotNull ModFingerprint fingerprint;

    private final long expiryMillis;

    /**
     * Creates a {@link Registration} instance.
     *
     * @param key                   the client's key
     * @param address               the proved source address
     * @param connection            the client's connection number
     * @param connectionStartMillis the epoch millisecond at which the client's connection started
     * @param displayName           the display name
     * @param fingerprint           the fingerprint of the mod the client runs
     * @param expiryMillis          the epoch millisecond at which the registration expires
     */
    public Registration(@NotNull IdentityKey key, @NotNull InetSocketAddress address, long connection, long connectionStartMillis,
                        @NotNull String displayName, @NotNull ModFingerprint fingerprint,
                        long expiryMillis) {
        this.key                   = key;
        this.address               = address;
        this.connection            = connection;
        this.connectionStartMillis = connectionStartMillis;

        this.displayName = displayName;
        this.fingerprint = fingerprint;

        this.expiryMillis = expiryMillis;
    }

    /**
     * @return the client's key
     */
    public @NotNull IdentityKey getKey() {
        return this.key;
    }

    /**
     * @return the proved source address
     */
    public @NotNull InetSocketAddress getAddress() {
        return this.address;
    }

    /**
     * @return the client's connection number
     */
    public long getConnection() {
        return this.connection;
    }

    /**
     * @return the epoch millisecond at which the client's connection started
     */
    public long getConnectionStartMillis() {
        return this.connectionStartMillis;
    }

    /**
     * @return the display name
     */
    public @NotNull String getDisplayName() {
        return this.displayName;
    }

    /**
     * @return the fingerprint of the mod the client runs
     */
    public @NotNull ModFingerprint getFingerprint() {
        return this.fingerprint;
    }

    /**
     * @return the epoch millisecond at which the registration expires
     */
    public long getExpiryMillis() {
        return this.expiryMillis;
    }


    /**
     * Checks the expiry.
     *
     * @param nowMillis the current time
     * @return {@code true} if the registration has expired, {@code false} otherwise
     */
    public boolean isExpired(long nowMillis) {
        return this.expiryMillis <= nowMillis;
    }
}
