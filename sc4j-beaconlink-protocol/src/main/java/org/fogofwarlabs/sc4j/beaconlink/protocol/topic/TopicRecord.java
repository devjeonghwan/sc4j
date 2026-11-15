package org.fogofwarlabs.sc4j.beaconlink.protocol.topic;

import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKey;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKeyPair;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * Holds one signed record of a topic: a room advertisement, a player's presence, a chat line or a player's sign-out. Peers store and gossip
 * records unchanged; they check a record's topic, its author's signature and its expiry, and name it by its {@link RecordId}, the hash of its
 * whole wire form. Of the records of one author with the same slot (a room, a connection's presence, a sign-out), the higher sequence replaces
 * the lower.
 * <p>
 * The wire form is the record's fields followed by the author's Ed25519 signature over a domain text of the record kind and every field byte.
 */
public abstract class TopicRecord {
    /**
     * Signs the fields of a record.
     *
     * @param author   the author's key pair
     * @param domain   the domain text of the record kind
     * @param unsigned the record's fields
     * @return the signature
     */
    protected static byte @NotNull [] sign(@NotNull IdentityKeyPair author, byte @NotNull [] domain, byte @NotNull [] unsigned) {
        byte[] signed = prefixDomain(domain, unsigned);

        return author.sign(signed, 0, signed.length);
    }

    private static byte @NotNull [] prefixDomain(byte @NotNull [] domain, byte @NotNull [] unsigned) {
        byte[] signed = Arrays.copyOf(domain, domain.length + unsigned.length);

        System.arraycopy(unsigned, 0, signed, domain.length, unsigned.length);

        return signed;
    }


    private final byte @NotNull [] unsigned;
    private final byte @NotNull [] signature;

    private @Nullable RecordId recordId;
    private @Nullable Boolean verified;

    /**
     * Creates a {@link TopicRecord} instance from its wire form.
     *
     * @param unsigned  the record's fields
     * @param signature the author's signature
     */
    protected TopicRecord(byte @NotNull [] unsigned, byte @NotNull [] signature) {
        this.unsigned  = unsigned;
        this.signature = signature;

        this.recordId = null;
        this.verified = null;
    }

    /**
     * @return the kind of the record
     */
    public abstract @NotNull RecordKind getRecordKind();

    /**
     * @return the topic the record belongs to
     */
    public abstract @NotNull TopicKey getTopic();

    /**
     * @return the author's key, which signs the record
     */
    public abstract @NotNull IdentityKey getAuthor();

    /**
     * @return the sequence; a higher sequence of the same author and slot replaces a lower one
     */
    public abstract long getSequence();

    /**
     * @return the epoch millisecond at which the record expires
     */
    public abstract long getExpiryMillis();

    /**
     * @return {@code true} if the author's signature is valid for the record, {@code false} otherwise
     */
    public abstract boolean isSignatureValid();

    /**
     * @return the ID of the record, from its whole wire form
     */
    public @NotNull RecordId getRecordId() {
        RecordId computed = this.recordId;

        if (computed == null) {
            byte[] encoded = Arrays.copyOf(this.unsigned, this.unsigned.length + this.signature.length);

            System.arraycopy(this.signature, 0, encoded, this.unsigned.length, this.signature.length);
            computed      = RecordId.of(encoded);
            this.recordId = computed;
        }

        return computed;
    }

    /**
     * @return the length of the record's wire form
     */
    public int getEncodedLength() {
        return this.unsigned.length + IdentityKey.SIGNATURE_LENGTH;
    }


    /**
     * Checks the author's signature over the record's fields. The result is kept, so that a record checked before its author's rate is not
     * verified again when it is stored; a record has one author and one domain, so the result never changes.
     *
     * @param author the author's key
     * @param domain the domain text of the record kind
     * @return {@code true} if the signature is valid, {@code false} otherwise
     */
    protected boolean verify(@NotNull IdentityKey author, byte @NotNull [] domain) {
        Boolean known = this.verified;

        if (known == null) {
            byte[] signed = prefixDomain(domain, this.unsigned);

            known         = author.verify(signed, 0, signed.length, this.signature, 0);
            this.verified = known;
        }

        return known;
    }

    /**
     * Writes the record unchanged: its fields and its signature.
     *
     * @param writer the writer
     */
    public void write(@NotNull WireWriter writer) {
        writer.writeBytes(this.unsigned);
        writer.writeBytes(this.signature);
    }

    /**
     * @param other the other object
     * @return {@code true} if the other object is a record with the same wire form, {@code false} otherwise
     */
    @Override
    public boolean equals(@Nullable Object other) {
        return other instanceof TopicRecord record && Arrays.equals(this.unsigned, record.unsigned) &&
               Arrays.equals(this.signature, record.signature);
    }

    /**
     * @return the hash code of the wire form
     */
    @Override
    public int hashCode() {
        return Arrays.hashCode(this.unsigned) ^ Arrays.hashCode(this.signature);
    }
}
