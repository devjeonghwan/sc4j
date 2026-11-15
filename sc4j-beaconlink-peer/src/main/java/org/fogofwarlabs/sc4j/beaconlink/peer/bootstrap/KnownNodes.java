package org.fogofwarlabs.sc4j.beaconlink.peer.bootstrap;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.beaconlink.protocol.file.ConfiguredNode;
import org.fogofwarlabs.sc4j.beaconlink.protocol.file.NodeListFile;
import org.fogofwarlabs.sc4j.beaconlink.protocol.file.NodeListProblem;
import org.fogofwarlabs.sc4j.beaconlink.protocol.file.NodeListReading;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKey;
import org.fogofwarlabs.sc4j.beaconlink.protocol.message.NodeEndpoint;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Holds what a Beacon.Link process knows of the nodes it may contact, the same for the launcher's peer, a headless node and every game (its
 * session and its embedded peer): the seeds and the learned peers of the shared peers file ({@link PeerStore}).
 * <p>
 * The seeds of an install follow one rule ({@link #readSeeds}): the user's node list once the user saved one (even an empty one), otherwise
 * the seed list the install ships. A headless node takes its seeds from its command line.
 * <p>
 * A process takes its contacts at its start ({@link #getContacts}: the seeds, then the best learned peers), records the peers that answer
 * ({@link #recordSuccess}) and the peers others report ({@link #learn}), and merges what it learned into the peers file on a timer
 * ({@link #flushWhenDue}) and when it stops ({@link #flush}). Without a peers file the learned peers are neither read nor kept.
 */
public final class KnownNodes {
    /**
     * The number of learned peers a peer's start greets.
     */
    public static final int BOOTSTRAP_PEERS = 32;

    /**
     * Reads the seeds of an install: the user's node list once it exists, otherwise the seed list the install ships.
     *
     * @param nodesFile the user's node list, normally {@code <install>/userdata/beaconlink/nodes.json}
     * @param seedsFile the seed list the install ships, normally {@code <install>/launcher/seeds.json}, or {@code null} for none
     * @return the seeds in list order, empty if neither file exists
     * @throws IOException if the file in use cannot be read, or it is not a format 1 node list with valid addresses and keys
     */
    public static @NotNull List<@NotNull ConfiguredNode> readSeeds(@NotNull Path nodesFile, @Nullable Path seedsFile) throws IOException {
        NodeListFile userList = new NodeListFile(nodesFile);
        NodeListFile file = seedsFile == null ? userList : userList.selectExistingOr(new NodeListFile(seedsFile));
        NodeListReading reading = file.read();
        NodeListProblem problem = reading.getProblem();

        if (problem != null) {
            throw Exceptions.newIOException("Invalid node list {0}: {1}", file.getPath(), problem);
        }

        return reading.getNodes();
    }

    /**
     * Opens the known nodes of a process, reading the peers file.
     *
     * @param seeds     the seeds in list order
     * @param peersFile the shared peers file, or {@code null} to neither read nor keep learned peers
     * @return the known nodes
     */
    public static @NotNull KnownNodes open(@NotNull List<@NotNull ConfiguredNode> seeds, @Nullable Path peersFile) {
        return new KnownNodes(seeds, peersFile == null ? null : PeerStore.open(peersFile));
    }


    private final @NotNull List<@NotNull ConfiguredNode> seeds;
    private final @Nullable PeerStore peers;

    private KnownNodes(@NotNull List<@NotNull ConfiguredNode> seeds, @Nullable PeerStore peers) {
        this.seeds = List.copyOf(seeds);
        this.peers = peers;
    }

    /**
     * @return the seeds in list order
     */
    public @NotNull List<@NotNull ConfiguredNode> getSeeds() {
        return this.seeds;
    }

    /**
     * @return the shared peers file, or {@code null} if the learned peers are not kept
     */
    public @Nullable Path getPeersFile() {
        PeerStore store = this.peers;

        return store == null ? null : store.getFile();
    }

    /**
     * @return the learned peers in the process's view ({@link PeerStore#getPeers()}), empty without a peers file
     */
    public @NotNull List<@NotNull LearnedPeer> getPeers() {
        PeerStore store = this.peers;

        return store == null ? List.of() : store.getPeers();
    }

    /**
     * @return the number of learned peers in the process's view
     */
    public int getPeerCount() {
        PeerStore store = this.peers;

        return store == null ? 0 : store.getCount();
    }


    /**
     * Returns the contacts of a start: the seeds in list order, then the best learned peers ({@link #selectPeers}) with their keys pinned.
     *
     * @param limit    the most learned peers
     * @param excluded the keys never chosen, such as the process's own keys
     * @return the contacts
     */
    public @NotNull List<@NotNull ConfiguredNode> getContacts(int limit, @NotNull Collection<@NotNull IdentityKey> excluded) {
        List<ConfiguredNode> contacts = new ArrayList<>(this.seeds);

        for (NodeEndpoint peer : this.selectPeers(limit, excluded)) {
            contacts.add(ConfiguredNode.of(peer));
        }

        return contacts;
    }

    /**
     * Chooses the learned peers a start tries ({@link PeerStore#selectPeers}); each counts one failed start until it answers.
     *
     * @param limit    the most learned peers
     * @param excluded the keys never chosen, such as the process's own keys
     * @return the chosen peers' keys and addresses, the best first; empty without a peers file
     */
    public @NotNull List<@NotNull NodeEndpoint> selectPeers(int limit, @NotNull Collection<@NotNull IdentityKey> excluded) {
        PeerStore store = this.peers;

        return store == null ? List.of() : store.selectPeers(limit, excluded);
    }

    /**
     * Records a peer that answered at its address with its key ({@link PeerStore#recordSuccess}).
     *
     * @param endpoint  the peer's key and address
     * @param nowMillis the current time
     */
    public void recordSuccess(@NotNull NodeEndpoint endpoint, long nowMillis) {
        this.recordSuccess(List.of(endpoint), nowMillis);
    }

    /**
     * Records peers that answered at their address with their key ({@link PeerStore#recordSuccess}).
     *
     * @param endpoints the peers' keys and addresses
     * @param nowMillis the current time
     */
    public void recordSuccess(@NotNull List<@NotNull NodeEndpoint> endpoints, long nowMillis) {
        PeerStore store = this.peers;

        if (store != null) {
            store.recordSuccess(endpoints, nowMillis);
        }
    }

    /**
     * Learns a peer another peer reported, as never answered ({@link PeerStore#learn}).
     *
     * @param endpoint  the reported peer's key and address
     * @param nowMillis the current time
     * @return {@code true} if the peer was learned, {@code false} otherwise (it is known already or no peers file is kept)
     */
    public boolean learn(@NotNull NodeEndpoint endpoint, long nowMillis) {
        PeerStore store = this.peers;

        return store != null && store.learn(endpoint, nowMillis);
    }

    /**
     * Tells whether a merge into the peers file is due ({@link PeerStore#isMergeDue}).
     *
     * @param nowMillis the current time
     * @return {@code true} if a merge is due, {@code false} otherwise or without a peers file
     */
    public boolean isFlushDue(long nowMillis) {
        PeerStore store = this.peers;

        return store != null && store.isMergeDue(nowMillis);
    }

    /**
     * Merges what the process learned into the peers file when the merge interval passed ({@link PeerStore#mergeWhenDue}).
     *
     * @param nowMillis the current time
     */
    public void flushWhenDue(long nowMillis) {
        PeerStore store = this.peers;

        if (store != null) {
            store.mergeWhenDue(nowMillis);
        }
    }

    /**
     * Merges what the process learned into the peers file now ({@link PeerStore#merge}); never throws.
     *
     * @param nowMillis the current time
     */
    public void flush(long nowMillis) {
        PeerStore store = this.peers;

        if (store != null) {
            store.merge(nowMillis);
        }
    }
}
