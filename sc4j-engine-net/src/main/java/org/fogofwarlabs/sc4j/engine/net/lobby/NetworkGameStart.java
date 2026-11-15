package org.fogofwarlabs.sc4j.engine.net.lobby;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.command.CommandRegistry;
import org.fogofwarlabs.sc4j.engine.command.CommandType;
import org.fogofwarlabs.sc4j.engine.game.Game;
import org.fogofwarlabs.sc4j.engine.game.GameSetup;
import org.fogofwarlabs.sc4j.engine.lockstep.*;
import org.fogofwarlabs.sc4j.engine.net.*;
import org.fogofwarlabs.sc4j.engine.net.beaconlink.LinkLockstepTransport;
import org.fogofwarlabs.sc4j.engine.net.beaconlink.LinkParticipant;
import org.fogofwarlabs.sc4j.engine.net.beaconlink.LinkPeer;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Holds a network game as its lobby started it, the same on every machine: the session id, the turn timing, the game setup (with its seed),
 * the members with their slots, and every participant's game address in execution order (peer number order): the address of its UDP endpoint
 * on a local network, or its Beacon.Link participant over Beacon.Link. It creates this machine's transport (the engine's lockstep transport
 * over UDP or over the overlay) and lockstep session; the mod creates the game from the setup.
 */
public final class NetworkGameStart {
    private final int sessionId;
    private final @NotNull TurnSchedule schedule;
    private final @NotNull GameSetup setup;
    private final @NotNull List<@NotNull LobbyMember> members;
    private final @NotNull List<@NotNull PeerAddress> addresses;
    private final @NotNull List<@NotNull LinkPeer> linkPeers;
    private final @NotNull LockstepPeer localPeer;

    /**
     * Creates a {@link NetworkGameStart} instance.
     *
     * @param sessionId the session id of the game's transport
     * @param schedule  the turn timing
     * @param setup     the game setup
     * @param members   the members in peer number order; copied
     * @param addresses the participants' UDP game addresses in the members' order, empty over Beacon.Link; copied
     * @param linkPeers the participants' Beacon.Link participants in the members' order, empty on a local network; copied
     * @param localPeer the local participant
     */
    NetworkGameStart(int sessionId, @NotNull TurnSchedule schedule, @NotNull GameSetup setup, @NotNull List<@NotNull LobbyMember> members,
                     @NotNull List<@NotNull PeerAddress> addresses, @NotNull List<@NotNull LinkPeer> linkPeers, @NotNull LockstepPeer localPeer) {
        this.sessionId = sessionId;
        this.schedule  = schedule;
        this.setup     = setup;
        this.members   = List.copyOf(members);
        this.addresses = List.copyOf(addresses);
        this.linkPeers = List.copyOf(linkPeers);
        this.localPeer = localPeer;
    }

    /**
     * @return the session id of the game's transport
     */
    public int getSessionId() {
        return this.sessionId;
    }

    /**
     * @return the turn timing
     */
    public @NotNull TurnSchedule getSchedule() {
        return this.schedule;
    }

    /**
     * @return the game setup, the seed included
     */
    public @NotNull GameSetup getSetup() {
        return this.setup;
    }

    /**
     * @return the members in peer number order
     */
    public @NotNull List<@NotNull LobbyMember> getMembers() {
        return this.members;
    }

    /**
     * @return the participants' UDP game addresses in execution order, empty for a game over Beacon.Link
     */
    public @NotNull List<@NotNull PeerAddress> getAddresses() {
        return this.addresses;
    }

    /**
     * @return the participants' Beacon.Link participants in execution order, empty for a game on a local network
     */
    public @NotNull List<@NotNull LinkPeer> getLinkPeers() {
        return this.linkPeers;
    }

    /**
     * @return {@code true} if the game runs over Beacon.Link, {@code false} otherwise (it runs on a local network)
     */
    public boolean isOverBeaconLink() {
        return !this.linkPeers.isEmpty();
    }

    /**
     * @return the local participant
     */
    public @NotNull LockstepPeer getLocalPeer() {
        return this.localPeer;
    }


    /**
     * Creates this machine's UDP transport of the game.
     *
     * @param endpoint the local datagram endpoint at the game port this machine announced in the lobby
     * @param clock    the clock of the resends and timeouts
     * @param timeouts the timing
     * @return the created {@link UdpLockstepTransport} instance
     * @throws IllegalStateException if the game runs over Beacon.Link
     */
    public @NotNull UdpLockstepTransport createTransport(@NotNull DatagramEndpoint endpoint, @NotNull NetworkClock clock,
                                                         @NotNull TransportTimeouts timeouts) {
        if (this.isOverBeaconLink()) {
            throw Exceptions.newIllegalStateException("The game runs over Beacon.Link, not over UDP.");
        }

        return new UdpLockstepTransport(endpoint, clock, timeouts, this.sessionId, this.addresses, this.localPeer);
    }

    /**
     * Creates this machine's transport of a game over Beacon.Link.
     *
     * @param port     this machine's game port of the overlay ({@link org.fogofwarlabs.sc4j.engine.net.beaconlink.LinkOverlay#GAME_PORT})
     * @param clock    the clock of the resends and timeouts
     * @param timeouts the timing
     * @return the created {@link LinkLockstepTransport} instance
     * @throws IllegalStateException if the game runs on a local network
     */
    public @NotNull LinkLockstepTransport createTransport(@NotNull DatagramLink<@NotNull LinkParticipant> port, @NotNull NetworkClock clock,
                                                          @NotNull TransportTimeouts timeouts) {
        if (!this.isOverBeaconLink()) {
            throw Exceptions.newIllegalStateException("The game runs on a local network, not over Beacon.Link.");
        }

        return new LinkLockstepTransport(port, clock, timeouts, this.sessionId, this.linkPeers, this.localPeer);
    }

    /**
     * Creates this machine's lockstep session of the game: each member plays the game's player of its slot, an observer none.
     *
     * @param game              this machine's game, created from the setup
     * @param transport         this machine's transport ({@link #createTransport})
     * @param commands          the command types of the game by id
     * @param desyncListener    the receiver of detected desyncs, or {@code null} to only count them
     * @param departureListener the receiver of the transport's departures, or {@code null} to only keep them
     * @return the created {@link LockstepSession} instance
     */
    public @NotNull LockstepSession createSession(@NotNull Game game, @NotNull LockstepTransport transport,
                                                  @NotNull CommandRegistry<? extends CommandType<?>> commands,
                                                  @Nullable DesyncListener desyncListener, @Nullable PeerDepartureListener departureListener) {
        int peerCount = this.members.getLast().getPeer().getNumber() + 1;
        List<Player> peerPlayers = new ArrayList<>(Collections.nCopies(peerCount, (Player) null));

        for (LobbyMember member : this.members) {
            if (!member.isObserver()) {
                peerPlayers.set(member.getPeer().getNumber(), game.getPlayer(member.getSlotIndex()));
            }
        }

        return new LockstepSession(transport, commands, this.localPeer, peerPlayers, this.schedule, desyncListener, departureListener);
    }
}
