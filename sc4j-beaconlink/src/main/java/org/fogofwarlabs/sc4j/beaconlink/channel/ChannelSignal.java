package org.fogofwarlabs.sc4j.beaconlink.channel;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.beaconlink.protocol.message.SignalMessage;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireReader;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * Holds the payload of a node {@link SignalMessage} between two participants that open a channel: an offer, an answer or a refusal. The
 * signal's envelope is signed by the sender's identity key and names the target's key and connection, and the node relays it unchanged, so
 * the receiver knows who sent the payload and that it was meant for it; the payload names the sender's connection, so that the receiver
 * knows which connection of the sender's key answers.
 * <p>
 * Payload: the kind (one byte, {@link ChannelSignalKind}), the sender's connection number (eight bytes) and the kind's fields, at most
 * {@value SignalMessage#MAXIMUM_PAYLOAD_LENGTH} bytes. An address candidate list is its count (one byte, at most {@value #MAXIMUM_CANDIDATES})
 * and the addresses.
 */
public abstract class ChannelSignal {
    /**
     * The most address candidates of one participant: the addresses its nodes observed and its local address.
     */
    public static final int MAXIMUM_CANDIDATES = 8;

    /**
     * Reads a payload. Never throws for malformed input.
     *
     * @param payload the signal's payload
     * @return the signal, or {@code null} if the payload is malformed, has trailing bytes or an unknown kind
     */
    public static @Nullable ChannelSignal read(byte @NotNull [] payload) {
        WireReader reader = new WireReader(payload, 0, payload.length);
        ChannelSignalKind kind = ChannelSignalKind.find(reader.readUnsigned8());
        long senderConnection = reader.readInt64();

        if (kind == null || reader.isFailed()) {
            return null;
        }

        ChannelSignal signal = switch (kind) {
            case OFFER -> ChannelOffer.read(reader, senderConnection);
            case ANSWER -> ChannelAnswer.read(reader, senderConnection);
            case REFUSAL -> ChannelRefusal.read(reader, senderConnection);
        };

        return signal != null && reader.isComplete() ? signal : null;
    }

    /**
     * Reads an address candidate list.
     *
     * @param reader the reader
     * @return the candidates, or {@code null} if the reader failed or the list is too long
     */
    static @Nullable List<@NotNull InetSocketAddress> readCandidates(@NotNull WireReader reader) {
        int count = reader.readUnsigned8();

        if (count > MAXIMUM_CANDIDATES) {
            reader.fail();

            return null;
        }

        List<InetSocketAddress> candidates = new ArrayList<>(count);

        for (int index = 0; index < count; index++) {
            InetSocketAddress candidate = reader.readAddress();

            if (candidate == null) {
                return null;
            }

            candidates.add(candidate);
        }

        return reader.isFailed() ? null : List.copyOf(candidates);
    }

    /**
     * Checks an address candidate list for a signal that is being created.
     *
     * @param candidates the candidates
     * @return a copy of the candidates
     * @throws IllegalArgumentException if there are more than {@value #MAXIMUM_CANDIDATES} candidates
     */
    static @NotNull List<@NotNull InetSocketAddress> copyCandidates(@NotNull List<@NotNull InetSocketAddress> candidates) {
        if (candidates.size() > MAXIMUM_CANDIDATES) {
            throw Exceptions.newIllegalArgumentException("A signal holds at most {0} address candidates, not {1}.",
                                                         MAXIMUM_CANDIDATES, candidates.size());
        }

        return List.copyOf(candidates);
    }

    /**
     * Checks an ephemeral public key for a signal that is being created.
     *
     * @param ephemeralKey the key bytes
     * @return a copy of the key bytes
     * @throws IllegalArgumentException if the key length is not {@value EphemeralKeyPair#KEY_LENGTH}
     */
    static byte @NotNull [] copyEphemeralKey(byte @NotNull [] ephemeralKey) {
        if (ephemeralKey.length != EphemeralKeyPair.KEY_LENGTH) {
            throw Exceptions.newIllegalArgumentException("An ephemeral key has {0} bytes, not {1}.",
                                                         EphemeralKeyPair.KEY_LENGTH, ephemeralKey.length);
        }

        return ephemeralKey.clone();
    }

    /**
     * Writes an address candidate list.
     *
     * @param writer     the writer
     * @param candidates the candidates, at most {@value #MAXIMUM_CANDIDATES}
     */
    static void writeCandidates(@NotNull WireWriter writer, @NotNull List<@NotNull InetSocketAddress> candidates) {
        writer.writeUnsigned8(candidates.size());

        for (InetSocketAddress candidate : candidates) {
            writer.writeAddress(candidate);
        }
    }


    private final long senderConnection;

    /**
     * Creates a {@link ChannelSignal} instance.
     *
     * @param senderConnection the connection number of the sender's session
     */
    protected ChannelSignal(long senderConnection) {
        this.senderConnection = senderConnection;
    }

    /**
     * @return the kind of the signal
     */
    public abstract @NotNull ChannelSignalKind getKind();

    /**
     * @return the connection number of the sender's session
     */
    public final long getSenderConnection() {
        return this.senderConnection;
    }


    /**
     * Writes the payload: the kind and the fields.
     *
     * @return the payload bytes
     */
    public final byte @NotNull [] write() {
        WireWriter writer = new WireWriter(SignalMessage.MAXIMUM_PAYLOAD_LENGTH);

        writer.writeUnsigned8(this.getKind().getCode());
        writer.writeInt64(this.senderConnection);
        this.writeFields(writer);

        return writer.toByteArray();
    }

    /**
     * Writes the kind's fields.
     *
     * @param writer the writer
     */
    protected abstract void writeFields(@NotNull WireWriter writer);
}
