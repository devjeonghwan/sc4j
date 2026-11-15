package org.fogofwarlabs.sc4j.beaconlink.protocol.room;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.beaconlink.protocol.coordinate.NetworkCoordinate;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKey;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKeyPair;
import org.fogofwarlabs.sc4j.beaconlink.protocol.message.HomePeerListUtils;
import org.fogofwarlabs.sc4j.beaconlink.protocol.message.NodeEndpoint;
import org.fogofwarlabs.sc4j.beaconlink.protocol.topic.RecordId;
import org.fogofwarlabs.sc4j.beaconlink.protocol.topic.RecordKind;
import org.fogofwarlabs.sc4j.beaconlink.protocol.topic.TopicKey;
import org.fogofwarlabs.sc4j.beaconlink.protocol.topic.TopicRecord;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireReader;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Advertises a room, signed by its host: a record of the room's mod topic, which peers store and gossip unchanged, checking only its topic,
 * signature and expiry. A newer sequence of the same {@link RoomKey} replaces an older one; a withdrawal is an advertisement with the
 * withdrawn flag, kept until its expiry so that an older advertisement cannot return through gossip. Members of the topic name the
 * advertisement by its {@link RecordId}. The advertisement names the host's home peers, the peers the host is registered at, so that a joiner
 * registered elsewhere registers there too and reaches the host with its signals. A publication carries the host's settled network
 * coordinate when it has one, from which a searcher estimates its round trip time to the host.
 * <p>
 * On the wire: the flags (one byte, bit 0 withdrawn, the other bits zero), the {@link TopicKey} of the mod topic, the host key,
 * the host's connection number (eight bytes), the room number and the sequence (unsigned 32-bit), the expiry (epoch milliseconds, 64-bit),
 * the {@link RoomDescription}, the host's home peers ({@link HomePeerListUtils}), the host's optional {@link NetworkCoordinate} (flag and
 * coordinate) and the Ed25519 signature of the host over a domain prefix and every byte before the signature. At most
 * {@value #MAXIMUM_ENCODED_LENGTH} bytes.
 */
public final class RoomAdvertisement extends TopicRecord {
    /**
     * The largest sequence number.
     */
    public static final long MAXIMUM_SEQUENCE = 0xffffffffL;
    /**
     * The largest length of an advertisement's wire form: 447 bytes of room fields and signature, the room flags with a join key, the mod
     * data with its length byte, the home peers and the coordinate.
     */
    public static final int MAXIMUM_ENCODED_LENGTH = 447 + 1 + RoomDescription.JOIN_KEY_LENGTH + 1 + RoomDescription.MAXIMUM_MOD_DATA_LENGTH +
                                                     HomePeerListUtils.MAXIMUM_ENCODED_LENGTH + NetworkCoordinate.MAXIMUM_OPTIONAL_LENGTH;

    private static final int WITHDRAWN_FLAG = 1;
    private static final byte @NotNull [] SIGNATURE_DOMAIN = "Beacon.Link room advertisement 1\0".getBytes(StandardCharsets.US_ASCII);

    /**
     * Creates a {@link RoomAdvertisement} instance that publishes or updates a room, signed by its host.
     *
     * @param host         the host's key pair
     * @param connection   the connection number of the host's session that holds the room
     * @param topic        the mod topic of the room
     * @param number       the room number, {@code 0} to {@value RoomKey#MAXIMUM_NUMBER}
     * @param sequence     the sequence, higher than every earlier advertisement of the room
     * @param expiryMillis the epoch millisecond at which the advertisement expires
     * @param description  the room's description
     * @param homePeers    the peers the host is registered at, at most {@value HomePeerListUtils#MAXIMUM_COUNT}
     * @param coordinate   the host's settled network coordinate, or {@code null} if it has none yet
     * @return the created {@link RoomAdvertisement} instance
     * @throws IllegalArgumentException if the number or the sequence is out of range, or there are too many home peers
     */
    public static @NotNull RoomAdvertisement signPublication(@NotNull IdentityKeyPair host, long connection, @NotNull TopicKey topic, long number,
                                                             long sequence, long expiryMillis, @NotNull RoomDescription description,
                                                             @NotNull List<@NotNull NodeEndpoint> homePeers, @Nullable NetworkCoordinate coordinate) {
        return sign(host, connection, false, topic, number, sequence, expiryMillis, description, homePeers, coordinate);
    }

    /**
     * Creates a {@link RoomAdvertisement} instance that withdraws a room, signed by its host; a withdrawal carries no coordinate.
     *
     * @param host         the host's key pair
     * @param connection   the connection number of the host's session that holds the room
     * @param topic        the mod topic of the room
     * @param number       the room number, {@code 0} to {@value RoomKey#MAXIMUM_NUMBER}
     * @param sequence     the sequence, higher than every earlier advertisement of the room
     * @param expiryMillis the epoch millisecond until which peers keep the withdrawal
     * @param description  the room's last description
     * @param homePeers    the peers the host is registered at, at most {@value HomePeerListUtils#MAXIMUM_COUNT}
     * @return the created {@link RoomAdvertisement} instance
     * @throws IllegalArgumentException if the number or the sequence is out of range, or there are too many home peers
     */
    public static @NotNull RoomAdvertisement signWithdrawal(@NotNull IdentityKeyPair host, long connection, @NotNull TopicKey topic, long number,
                                                            long sequence, long expiryMillis, @NotNull RoomDescription description,
                                                            @NotNull List<@NotNull NodeEndpoint> homePeers) {
        return sign(host, connection, true, topic, number, sequence, expiryMillis, description, homePeers, null);
    }

    private static @NotNull RoomAdvertisement sign(@NotNull IdentityKeyPair host, long connection, boolean withdrawn, @NotNull TopicKey topic,
                                                   long number, long sequence, long expiryMillis, @NotNull RoomDescription description,
                                                   @NotNull List<@NotNull NodeEndpoint> homePeers, @Nullable NetworkCoordinate coordinate) {
        if (sequence < 0L || sequence > MAXIMUM_SEQUENCE) {
            throw Exceptions.newIllegalArgumentException("The sequence {0} is outside 0..{1}.", sequence, MAXIMUM_SEQUENCE);
        }

        RoomKey key = new RoomKey(host.getPublicKey(), connection, number);
        List<NodeEndpoint> checkedHomePeers = HomePeerListUtils.copy(homePeers);
        byte[] unsigned = encodeUnsigned(withdrawn, topic, key, sequence, expiryMillis, description, checkedHomePeers, coordinate);

        return new RoomAdvertisement(withdrawn, topic, key, sequence, expiryMillis, description, checkedHomePeers, coordinate, unsigned,
                                     TopicRecord.sign(host, SIGNATURE_DOMAIN, unsigned));
    }

    /**
     * Reads an advertisement from the wire; the signature is checked separately ({@link #isSignatureValid()}).
     *
     * @param reader the reader
     * @return the advertisement, or {@code null} if the reader failed or a field is out of range
     */
    public static @Nullable RoomAdvertisement read(@NotNull WireReader reader) {
        int flags = reader.readUnsigned8();
        TopicKey topic = TopicKey.read(reader);
        IdentityKey host = IdentityKey.read(reader);
        long connection = reader.readInt64();
        long number = reader.readUnsigned32();
        long sequence = reader.readUnsigned32();
        long expiryMillis = reader.readInt64();
        RoomDescription description = RoomDescription.read(reader);
        List<NodeEndpoint> homePeers = HomePeerListUtils.read(reader);
        NetworkCoordinate coordinate = NetworkCoordinate.readOptional(reader);
        byte[] signature = reader.readBytes(IdentityKey.SIGNATURE_LENGTH);

        if ((flags & ~WITHDRAWN_FLAG) != 0 || topic == null || host == null || description == null || homePeers == null || reader.isFailed()) {
            reader.fail();

            return null;
        }

        boolean withdrawn = (flags & WITHDRAWN_FLAG) != 0;
        RoomKey key = new RoomKey(host, connection, number);

        return new RoomAdvertisement(withdrawn, topic, key, sequence, expiryMillis, description, homePeers, coordinate,
                                     encodeUnsigned(withdrawn, topic, key, sequence, expiryMillis, description, homePeers, coordinate), signature);
    }

    private static byte @NotNull [] encodeUnsigned(boolean withdrawn, @NotNull TopicKey topic, @NotNull RoomKey key, long sequence,
                                                   long expiryMillis, @NotNull RoomDescription description,
                                                   @NotNull List<@NotNull NodeEndpoint> homePeers, @Nullable NetworkCoordinate coordinate) {
        WireWriter writer = new WireWriter(MAXIMUM_ENCODED_LENGTH);

        writer.writeUnsigned8(withdrawn ? WITHDRAWN_FLAG : 0);
        topic.write(writer);
        key.getHost().write(writer);
        writer.writeInt64(key.getConnection());
        writer.writeUnsigned32(key.getNumber());
        writer.writeUnsigned32(sequence);
        writer.writeInt64(expiryMillis);
        description.write(writer);
        HomePeerListUtils.write(writer, homePeers);
        NetworkCoordinate.writeOptional(writer, coordinate);

        return writer.toByteArray();
    }


    private final boolean withdrawn;
    private final @NotNull TopicKey topic;
    private final @NotNull RoomKey key;
    private final long sequence;
    private final long expiryMillis;
    private final @NotNull RoomDescription description;
    private final @NotNull List<@NotNull NodeEndpoint> homePeers;
    private final @Nullable NetworkCoordinate coordinate;

    private RoomAdvertisement(boolean withdrawn, @NotNull TopicKey topic, @NotNull RoomKey key, long sequence, long expiryMillis,
                              @NotNull RoomDescription description, @NotNull List<@NotNull NodeEndpoint> homePeers,
                              @Nullable NetworkCoordinate coordinate, byte @NotNull [] unsigned, byte @NotNull [] signature) {
        super(unsigned, signature);

        this.withdrawn    = withdrawn;
        this.topic        = topic;
        this.key          = key;
        this.sequence     = sequence;
        this.expiryMillis = expiryMillis;
        this.description  = description;
        this.homePeers    = homePeers;
        this.coordinate   = coordinate;
    }

    /**
     * @return {@link RecordKind#ROOM}
     */
    @Override
    public @NotNull RecordKind getRecordKind() {
        return RecordKind.ROOM;
    }

    /**
     * @return {@code true} if the advertisement withdraws the room, {@code false} otherwise (it publishes or updates it)
     */
    public boolean isWithdrawn() {
        return this.withdrawn;
    }

    /**
     * @return the mod topic of the room
     */
    @Override
    public @NotNull TopicKey getTopic() {
        return this.topic;
    }

    /**
     * @return the room's host, which signs the advertisement
     */
    @Override
    public @NotNull IdentityKey getAuthor() {
        return this.key.getHost();
    }

    /**
     * @return the room's key: the host's key, the host's connection number and the room number
     */
    public @NotNull RoomKey getKey() {
        return this.key;
    }

    /**
     * @return the sequence; a higher sequence of the same room replaces a lower one
     */
    @Override
    public long getSequence() {
        return this.sequence;
    }

    /**
     * @return the epoch millisecond at which the advertisement expires
     */
    @Override
    public long getExpiryMillis() {
        return this.expiryMillis;
    }

    /**
     * @return the room's description
     */
    public @NotNull RoomDescription getDescription() {
        return this.description;
    }

    /**
     * @return the peers the host was registered at when it signed the advertisement, unmodifiable
     */
    public @NotNull List<@NotNull NodeEndpoint> getHomePeers() {
        return this.homePeers;
    }

    /**
     * @return the host's settled network coordinate when it signed the advertisement, or {@code null} if it had none
     */
    public @Nullable NetworkCoordinate getCoordinate() {
        return this.coordinate;
    }

    /**
     * @return {@code true} if the host's signature is valid for the advertisement, {@code false} otherwise
     */
    @Override
    public boolean isSignatureValid() {
        return this.verify(this.key.getHost(), SIGNATURE_DOMAIN);
    }


    /**
     * @return the room key, the sequence and the description, for logs
     */
    @Override
    public @NotNull String toString() {
        return this.key + "#" + this.sequence + (this.withdrawn ? " withdrawn" : " " + this.description);
    }
}
