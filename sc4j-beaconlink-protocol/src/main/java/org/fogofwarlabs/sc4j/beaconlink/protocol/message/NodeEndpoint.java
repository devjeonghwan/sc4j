package org.fogofwarlabs.sc4j.beaconlink.protocol.message;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKey;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireReader;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;
import java.util.Objects;

/**
 * Names a node by its key and the address it was seen at: a node of a client, a seed with a pinned key or a learned peer. An entry
 * learned from another node is a claim until a hello round trip at that address answers with that key.
 * <p>
 * On the wire: the key and the address.
 */
public final class NodeEndpoint {
    /**
     * The largest length of an entry's wire form: the key and an IPv6 address.
     */
    public static final int MAXIMUM_ENCODED_LENGTH = IdentityKey.LENGTH + WireWriter.MAXIMUM_ADDRESS_LENGTH;

    /**
     * Reads an entry.
     *
     * @param reader the reader
     * @return the entry, or {@code null} if the reader failed
     */
    public static @Nullable NodeEndpoint read(@NotNull WireReader reader) {
        IdentityKey key = IdentityKey.read(reader);
        InetSocketAddress address = reader.readAddress();

        if (key == null || address == null) {
            return null;
        }

        return new NodeEndpoint(key, address);
    }


    private final @NotNull IdentityKey key;
    private final @NotNull InetSocketAddress address;

    /**
     * Creates a {@link NodeEndpoint} instance.
     *
     * @param key     the node's key
     * @param address the node's address, resolved
     * @throws IllegalArgumentException if the address is not resolved
     */
    public NodeEndpoint(@NotNull IdentityKey key, @NotNull InetSocketAddress address) {
        if (address.isUnresolved()) {
            throw Exceptions.newIllegalArgumentException("The node address {0} is not resolved.", address);
        }

        this.key     = key;
        this.address = address;
    }

    /**
     * @return the node's key
     */
    public @NotNull IdentityKey getKey() {
        return this.key;
    }

    /**
     * @return the node's address
     */
    public @NotNull InetSocketAddress getAddress() {
        return this.address;
    }

    /**
     * @return the length of the entry's wire form
     */
    public int getEncodedLength() {
        return IdentityKey.LENGTH + WireWriter.getAddressLength(this.address);
    }


    /**
     * Writes the entry.
     *
     * @param writer the writer
     */
    public void write(@NotNull WireWriter writer) {
        this.key.write(writer);
        writer.writeAddress(this.address);
    }

    /**
     * @param other the other object
     * @return {@code true} if the other object is an entry with the same key and address, {@code false} otherwise
     */
    @Override
    public boolean equals(@Nullable Object other) {
        return other instanceof NodeEndpoint endpoint && this.key.equals(endpoint.key) && this.address.equals(endpoint.address);
    }

    /**
     * @return the hash code of the key and the address
     */
    @Override
    public int hashCode() {
        return Objects.hash(this.key, this.address);
    }

    /**
     * @return the key and the address, for logs
     */
    @Override
    public @NotNull String toString() {
        return this.key + " at " + this.address;
    }
}
