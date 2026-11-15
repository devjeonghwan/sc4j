package org.fogofwarlabs.sc4j.beaconlink.test.simulation;

import org.fogofwarlabs.sc4j.beaconlink.peer.DatagramSink;
import org.fogofwarlabs.sc4j.beaconlink.protocol.coordinate.CoordinateReport;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKeyPair;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.PeerId;
import org.fogofwarlabs.sc4j.beaconlink.protocol.message.*;
import org.fogofwarlabs.sc4j.beaconlink.protocol.topic.TopicKey;
import org.fogofwarlabs.sc4j.beaconlink.protocol.topic.TopicRecord;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * Runs a flooding member of a topic on a {@link SimulatedNetwork}: a host with a valid peer proof that joins a victim's mesh the way an honest
 * member does ({@code GET_TOPIC} for the token, then {@code GRAFT}) and then pushes whatever records the test gives it ({@link #flood}),
 * one {@code GOSSIP} per record, as fast as the test calls it. It counts the answers it still gets.
 */
public final class TopicFlooder implements SimulatedNetwork.Endpoint {
    private static final long GET_TOPIC_REQUEST = 1L;
    private static final long GRAFT_REQUEST = 2L;

    private final @NotNull MessageEncoder encoder;
    private final @NotNull PeerCard card;
    private final @NotNull TopicKey topic;
    private final @NotNull InetSocketAddress victim;

    private @Nullable DatagramSink sink;
    private long nextRequestNumber;
    private boolean grafted;
    private int answerCount;
    private final @NotNull InetSocketAddress address;

    /**
     * Creates a {@link TopicFlooder} instance and adds its host to the network as a public host.
     *
     * @param network    the network
     * @param topic      the topic it floods
     * @param victim     the member whose mesh it joins
     * @param difficulty the proof difficulty of the network
     */
    public TopicFlooder(@NotNull SimulatedNetwork network, @NotNull TopicKey topic, @NotNull InetSocketAddress victim, int difficulty) {
        IdentityKeyPair keyPair = network.createKeyPair();

        this.encoder = new MessageEncoder(keyPair, network.getClock(), network.getRandom().split());
        this.card    = new PeerCard(PeerId.solveProof(keyPair.getPublicKey(), difficulty), true, CoordinateReport.UNKNOWN);
        this.topic   = topic;
        this.victim  = victim;

        this.sink              = null;
        this.nextRequestNumber = GRAFT_REQUEST + 1L;
        this.grafted           = false;
        this.answerCount       = 0;
        this.address           = network.addHost(network.createAddress(), null, sink -> {
            this.sink = sink;

            return this;
        });
    }

    /**
     * @return the flooder's public address
     */
    public @NotNull InetSocketAddress getAddress() {
        return this.address;
    }

    /**
     * @return {@code true} if the victim took the flooder into its mesh, {@code false} otherwise
     */
    public boolean isGrafted() {
        return this.grafted;
    }

    /**
     * @return the number of answers the flooder received from the victim
     */
    public int getAnswerCount() {
        return this.answerCount;
    }


    /**
     * Asks the victim for its token of the topic; the answer makes the flooder graft itself into the victim's mesh.
     */
    public void join() {
        DatagramSink output = this.sink;

        if (output != null) {
            output.send(this.encoder.encodePaddedRequest(new GetTopicMessage(this.topic), GET_TOPIC_REQUEST), this.victim);
        }
    }

    /**
     * Pushes records to the victim, one {@code GOSSIP} per record.
     *
     * @param records the records: room advertisements, presences or chat lines
     */
    public void flood(@NotNull List<? extends @NotNull TopicRecord> records) {
        DatagramSink output = this.sink;

        if (output == null) {
            return;
        }

        for (TopicRecord record : records) {
            output.send(this.encoder.encodeRequest(new GossipMessage(this.topic, List.of(record)), this.nextRequestNumber++), this.victim);
        }
    }

    /**
     * Handles a datagram that passed the host's NAT: the victim's token answer and graft result.
     *
     * @param datagram the datagram, owned by the endpoint
     * @param source   the sender's public address
     */
    @Override
    public void receive(byte @NotNull [] datagram, @NotNull InetSocketAddress source) {
        Envelope envelope = Envelope.decode(datagram, datagram.length).getEnvelope();
        DatagramSink output = this.sink;

        if (envelope == null || !source.equals(this.victim) || output == null) {
            return;
        }

        this.answerCount++;

        if (envelope.getBody() instanceof TopicPeersMessage answer && envelope.getRequestNumber() == GET_TOPIC_REQUEST) {
            output.send(this.encoder.encodeRequest(new GraftMessage(this.topic, this.card, answer.getToken()), GRAFT_REQUEST), this.victim);
        } else if (envelope.getBody() instanceof ResultMessage result && envelope.getRequestNumber() == GRAFT_REQUEST) {
            this.grafted = result.getStatus() == ResultStatus.ACCEPTED;
        }
    }

    /**
     * Does nothing: the flooder acts only when the test calls it.
     */
    @Override
    public void tick() {
        // The flooder acts only when the test calls join or flood.
    }
}
