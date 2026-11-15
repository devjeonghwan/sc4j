package org.fogofwarlabs.sc4j.beaconlink.protocol.community;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKey;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKeyPair;
import org.fogofwarlabs.sc4j.beaconlink.protocol.topic.RecordKind;
import org.fogofwarlabs.sc4j.beaconlink.protocol.topic.TopicKey;
import org.fogofwarlabs.sc4j.beaconlink.protocol.topic.TopicRecord;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireReader;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;

/**
 * Signs a player out everywhere else: a record of the player's own topic, signed by the player key, that names the one connection of the
 * player that stays (the connection that signed it). Every other connection of the player that started before the record's timestamp ends
 * its session when it sees the record, and a node that holds the record refuses such a connection's registration and drops the ones it
 * holds; a connection that starts later is not affected. Of the records of one player, the higher sequence replaces the lower.
 * <p>
 * On the wire: the flags (one byte, zero), the player's topic, the player key, the kept connection number (64-bit), the sequence (unsigned
 * 32-bit), the timestamp and the expiry (epoch milliseconds, 64-bit each) and the Ed25519 signature of the player over a domain prefix and
 * every byte before it. At most {@value #MAXIMUM_ENCODED_LENGTH} bytes.
 */
public final class SignOutRecord extends TopicRecord {
    /**
     * The largest sequence number.
     */
    public static final long MAXIMUM_SEQUENCE = 0xffffffffL;
    /**
     * The length of a record's wire form.
     */
    public static final int MAXIMUM_ENCODED_LENGTH = 1 + TopicKey.LENGTH + IdentityKey.LENGTH + Long.BYTES + Integer.BYTES + Long.BYTES +
                                                     Long.BYTES + IdentityKey.SIGNATURE_LENGTH;

    private static final byte @NotNull [] SIGNATURE_DOMAIN = "Beacon.Link sign out 1\0".getBytes(StandardCharsets.US_ASCII);

    /**
     * Creates a {@link SignOutRecord} instance signed by the player, in the player's own topic.
     *
     * @param author          the player's key pair
     * @param keptConnection  the number of the connection that stays signed in
     * @param sequence        the sequence, higher than every earlier sign-out of the player
     * @param timestampMillis the epoch millisecond of the sign-out: the connections that started before it end
     * @param expiryMillis    the epoch millisecond at which the record expires
     * @return the created {@link SignOutRecord} instance
     * @throws IllegalArgumentException if the sequence is out of range
     */
    public static @NotNull SignOutRecord sign(@NotNull IdentityKeyPair author, long keptConnection, long sequence,
                                              long timestampMillis, long expiryMillis) {
        if (sequence < 0L || sequence > MAXIMUM_SEQUENCE) {
            throw Exceptions.newIllegalArgumentException("The sequence {0} is outside 0..{1}.", sequence, MAXIMUM_SEQUENCE);
        }

        TopicKey topic = TopicKey.ofPlayer(author.getPublicKey());
        byte[] unsigned = encodeUnsigned(topic, author.getPublicKey(), keptConnection, sequence, timestampMillis, expiryMillis);

        return new SignOutRecord(topic, author.getPublicKey(), keptConnection, sequence, timestampMillis, expiryMillis, unsigned,
                                 TopicRecord.sign(author, SIGNATURE_DOMAIN, unsigned));
    }

    /**
     * Reads a sign-out from the wire; the signature is checked separately.
     *
     * @param reader the reader
     * @return the sign-out, or {@code null} if the reader failed, a field is out of range or the topic is not the player's own
     */
    public static @Nullable SignOutRecord read(@NotNull WireReader reader) {
        int flags = reader.readUnsigned8();
        TopicKey topic = TopicKey.read(reader);
        IdentityKey author = IdentityKey.read(reader);
        long keptConnection = reader.readInt64();
        long sequence = reader.readUnsigned32();
        long timestampMillis = reader.readInt64();
        long expiryMillis = reader.readInt64();
        byte[] signature = reader.readBytes(IdentityKey.SIGNATURE_LENGTH);

        if (flags != 0 || topic == null || author == null || reader.isFailed() || !TopicKey.ofPlayer(author).equals(topic)) {
            reader.fail();

            return null;
        }

        return new SignOutRecord(topic, author, keptConnection, sequence, timestampMillis, expiryMillis,
                                 encodeUnsigned(topic, author, keptConnection, sequence, timestampMillis, expiryMillis), signature);
    }

    private static byte @NotNull [] encodeUnsigned(@NotNull TopicKey topic, @NotNull IdentityKey author, long keptConnection, long sequence,
                                                   long timestampMillis, long expiryMillis) {
        WireWriter writer = new WireWriter(MAXIMUM_ENCODED_LENGTH);

        writer.writeUnsigned8(0);
        topic.write(writer);
        author.write(writer);
        writer.writeInt64(keptConnection);
        writer.writeUnsigned32(sequence);
        writer.writeInt64(timestampMillis);
        writer.writeInt64(expiryMillis);

        return writer.toByteArray();
    }


    private final @NotNull TopicKey topic;
    private final @NotNull IdentityKey author;
    private final long keptConnection;
    private final long sequence;
    private final long timestampMillis;
    private final long expiryMillis;

    private SignOutRecord(@NotNull TopicKey topic, @NotNull IdentityKey author, long keptConnection, long sequence, long timestampMillis,
                          long expiryMillis, byte @NotNull [] unsigned, byte @NotNull [] signature) {
        super(unsigned, signature);

        this.topic           = topic;
        this.author          = author;
        this.keptConnection  = keptConnection;
        this.sequence        = sequence;
        this.timestampMillis = timestampMillis;
        this.expiryMillis    = expiryMillis;
    }

    /**
     * @return {@link RecordKind#SIGN_OUT}
     */
    @Override
    public @NotNull RecordKind getRecordKind() {
        return RecordKind.SIGN_OUT;
    }

    /**
     * @return the player's own topic
     */
    @Override
    public @NotNull TopicKey getTopic() {
        return this.topic;
    }

    /**
     * @return the player's key, which signs the record
     */
    @Override
    public @NotNull IdentityKey getAuthor() {
        return this.author;
    }

    /**
     * @return the number of the connection that stays signed in
     */
    public long getKeptConnection() {
        return this.keptConnection;
    }

    /**
     * @return the sequence; a higher sequence of the same player replaces a lower one
     */
    @Override
    public long getSequence() {
        return this.sequence;
    }

    /**
     * @return the epoch millisecond of the sign-out: the player's other connections that started before it end
     */
    public long getTimestampMillis() {
        return this.timestampMillis;
    }

    /**
     * @return the epoch millisecond at which the record expires
     */
    @Override
    public long getExpiryMillis() {
        return this.expiryMillis;
    }

    /**
     * @return {@code true} if the player's signature is valid for the record, {@code false} otherwise
     */
    @Override
    public boolean isSignatureValid() {
        return this.verify(this.author, SIGNATURE_DOMAIN);
    }


    /**
     * Checks whether the record ends a connection of its player: any other connection than the kept one that started before the record's
     * timestamp.
     *
     * @param connection            the connection number
     * @param connectionStartMillis the epoch millisecond at which the connection started
     * @return {@code true} if the connection is signed out, {@code false} otherwise
     */
    public boolean isSignedOut(long connection, long connectionStartMillis) {
        return connection != this.keptConnection && connectionStartMillis < this.timestampMillis;
    }

    /**
     * @return the player key, the kept connection, the sequence and the timestamp, for logs
     */
    @Override
    public @NotNull String toString() {
        return this.author + "#" + this.sequence + " signs out all but " + Long.toHexString(this.keptConnection) + " at " + this.timestampMillis;
    }
}
