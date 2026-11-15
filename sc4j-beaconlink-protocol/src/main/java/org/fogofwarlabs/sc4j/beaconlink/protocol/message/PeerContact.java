package org.fogofwarlabs.sc4j.beaconlink.protocol.message;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKey;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.PeerId;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireReader;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;
import java.util.Objects;

/**
 * Names a full peer: its key, its proof nonce and its address. It is an entry of a {@link PeersMessage} and of a routing table. A receiver
 * checks the proof ({@link #isProofValid}) before it uses the entry; the address is a claim of the peer that named it until the peer answers
 * there with its key.
 * <p>
 * On the wire: the key, the proof nonce (8) and the address.
 */
public final class PeerContact {
    /**
     * The longest wire form: with an IPv6 address.
     */
    public static final int MAXIMUM_ENCODED_LENGTH = IdentityKey.LENGTH + Long.BYTES + 1 + WireReader.IPV6_LENGTH + Short.BYTES;

    /**
     * Reads an entry.
     *
     * @param reader the reader
     * @return the entry, or {@code null} if the reader failed
     */
    public static @Nullable PeerContact read(@NotNull WireReader reader) {
        IdentityKey key = IdentityKey.read(reader);
        long proofNonce = reader.readInt64();
        InetSocketAddress address = reader.readAddress();

        if (key == null || address == null || reader.isFailed()) {
            return null;
        }

        return new PeerContact(key, proofNonce, address);
    }


    private final @NotNull IdentityKey key;
    private final long proofNonce;
    private final @NotNull InetSocketAddress address;

    private @Nullable PeerId id;

    /**
     * Creates a {@link PeerContact} instance.
     *
     * @param key        the peer's key
     * @param proofNonce the peer's proof nonce
     * @param address    the peer's address, resolved
     * @throws IllegalArgumentException if the address is not resolved
     */
    public PeerContact(@NotNull IdentityKey key, long proofNonce, @NotNull InetSocketAddress address) {
        if (address.isUnresolved()) {
            throw Exceptions.newIllegalArgumentException("The peer address {0} is not resolved.", address);
        }

        this.key        = key;
        this.proofNonce = proofNonce;
        this.address    = address;

        this.id = null;
    }

    /**
     * @return the peer's key
     */
    public @NotNull IdentityKey getKey() {
        return this.key;
    }

    /**
     * @return the peer's proof nonce
     */
    public long getProofNonce() {
        return this.proofNonce;
    }

    /**
     * @return the peer's address
     */
    public @NotNull InetSocketAddress getAddress() {
        return this.address;
    }

    /**
     * @return the peer's ID, from its key and its proof nonce
     */
    public @NotNull PeerId getId() {
        PeerId computed = this.id;

        if (computed == null) {
            computed = PeerId.of(this.key, this.proofNonce);
            this.id  = computed;
        }

        return computed;
    }

    /**
     * @return the peer's key and address
     */
    public @NotNull NodeEndpoint getEndpoint() {
        return new NodeEndpoint(this.key, this.address);
    }

    /**
     * @return the length of the entry's wire form
     */
    public int getEncodedLength() {
        return IdentityKey.LENGTH + Long.BYTES + WireWriter.getAddressLength(this.address);
    }

    /**
     * Checks the entry's proof.
     *
     * @param difficulty the proof difficulty of the network
     * @return {@code true} if the proof nonce is valid for the key, {@code false} otherwise
     */
    public boolean isProofValid(int difficulty) {
        return PeerId.isProofValid(this.key, this.proofNonce, difficulty);
    }


    /**
     * Writes the entry.
     *
     * @param writer the writer
     */
    public void write(@NotNull WireWriter writer) {
        this.key.write(writer);
        writer.writeInt64(this.proofNonce);
        writer.writeAddress(this.address);
    }

    /**
     * @param other the other object
     * @return {@code true} if the other object is an entry with the same key, proof nonce and address, {@code false} otherwise
     */
    @Override
    public boolean equals(@Nullable Object other) {
        return other instanceof PeerContact contact && this.key.equals(contact.key) && this.proofNonce == contact.proofNonce &&
               this.address.equals(contact.address);
    }

    /**
     * @return the hash code of the key, the proof nonce and the address
     */
    @Override
    public int hashCode() {
        return Objects.hash(this.key, this.proofNonce, this.address);
    }

    /**
     * @return the key and the address, for logs
     */
    @Override
    public @NotNull String toString() {
        return this.key + " at " + this.address;
    }
}
