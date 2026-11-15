package org.fogofwarlabs.sc4j.beaconlink.protocol.message;

import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireReader;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Names a member of a topic: its {@link PeerContact} (key, proof nonce, address) and whether its self-test proved that strangers reach it.
 * It is an entry of a {@link TopicPeersMessage}; a receiver checks the proof before it uses the entry, and grafts a member that strangers
 * reach before one that they do not.
 * <p>
 * On the wire: the contact and the reachable flag (one byte, {@code 0} or {@code 1}).
 */
public final class TopicMember {
    /**
     * The longest wire form: with an IPv6 address.
     */
    public static final int MAXIMUM_ENCODED_LENGTH = PeerContact.MAXIMUM_ENCODED_LENGTH + 1;

    /**
     * Reads an entry.
     *
     * @param reader the reader
     * @return the entry, or {@code null} if the reader failed or the flag is out of range
     */
    public static @Nullable TopicMember read(@NotNull WireReader reader) {
        PeerContact contact = PeerContact.read(reader);
        int flag = reader.readUnsigned8();

        if (contact == null || flag > 1 || reader.isFailed()) {
            reader.fail();

            return null;
        }

        return new TopicMember(contact, flag == 1);
    }


    private final @NotNull PeerContact contact;
    private final boolean reachable;

    /**
     * Creates a {@link TopicMember} instance.
     *
     * @param contact   the member's key, proof nonce and address
     * @param reachable whether the member's self-test proved that strangers reach it
     */
    public TopicMember(@NotNull PeerContact contact, boolean reachable) {
        this.contact   = contact;
        this.reachable = reachable;
    }

    /**
     * @return the member's key, proof nonce and address
     */
    public @NotNull PeerContact getContact() {
        return this.contact;
    }

    /**
     * @return {@code true} if the member's self-test proved that strangers reach it, {@code false} otherwise
     */
    public boolean isReachable() {
        return this.reachable;
    }

    /**
     * @return the length of the entry's wire form
     */
    public int getEncodedLength() {
        return this.contact.getEncodedLength() + 1;
    }


    /**
     * Writes the entry.
     *
     * @param writer the writer
     */
    public void write(@NotNull WireWriter writer) {
        this.contact.write(writer);
        writer.writeUnsigned8(this.reachable ? 1 : 0);
    }

    /**
     * @param other the other object
     * @return {@code true} if the other object is an entry with the same contact and flag, {@code false} otherwise
     */
    @Override
    public boolean equals(@Nullable Object other) {
        return other instanceof TopicMember member && this.contact.equals(member.contact) && this.reachable == member.reachable;
    }

    /**
     * @return the hash code of the contact and the flag
     */
    @Override
    public int hashCode() {
        return Objects.hash(this.contact, this.reachable);
    }

    /**
     * @return the contact and the flag, for logs
     */
    @Override
    public @NotNull String toString() {
        return this.contact + (this.reachable ? " (reachable)" : " (leaf)");
    }
}
