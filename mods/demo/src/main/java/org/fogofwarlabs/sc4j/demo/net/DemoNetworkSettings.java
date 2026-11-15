package org.fogofwarlabs.sc4j.demo.net;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.lockstep.TurnSchedule;
import org.fogofwarlabs.sc4j.engine.net.TransportTimeouts;
import org.fogofwarlabs.sc4j.engine.net.discovery.LanRoomDiscovery;
import org.fogofwarlabs.sc4j.engine.net.lobby.NetworkLobby;
import org.jetbrains.annotations.NotNull;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * Holds where the demo's network games run: every socket on the loopback address {@value #LOOPBACK} (the demo plays network games on one
 * machine only, so nothing reaches the real network and no firewall asks), the TCP port of a hosted room's lobby and the UDP port of its room
 * discovery, and the turn timing of the games: a host chooses the turn rate of its room from {@link #TURN_RATES}, by default
 * {@value #DEFAULT_TURN_RATE} turns per second, and a turn spans the game loops of one second at speed 1 ({@value #LOOPS_PER_SECOND}) shared
 * by the turns of a second. A game endpoint takes any free port; the lobby tells the others which.
 */
public final class DemoNetworkSettings {
    /**
     * The address every socket of the demo's network games binds to and every machine reaches the others at.
     */
    public static final @NotNull String LOOPBACK = "127.0.0.1";
    /**
     * The game loops of one second at speed 1, as the turn rates count them.
     */
    public static final int LOOPS_PER_SECOND = 24;
    /**
     * The turn rates a host chooses from, in turns per second; each divides {@value #LOOPS_PER_SECOND}.
     */
    public static final @NotNull List<@NotNull Integer> TURN_RATES = List.of(8, 12, 24);
    /**
     * The turn rate of a room whose host chose none, in turns per second.
     */
    public static final int DEFAULT_TURN_RATE = 12;
    /**
     * The turns a local command travels before it runs.
     */
    public static final int LATENCY_TURNS = 2;
    /**
     * The members a room takes: the two players.
     */
    public static final int ROOM_CAPACITY = 2;
    /**
     * The port that means any free port.
     */
    public static final int ANY_PORT = 0;
    /**
     * The settings of the client: the engine's default lobby and discovery ports.
     */
    public static final @NotNull DemoNetworkSettings DEFAULTS = new DemoNetworkSettings(NetworkLobby.DEFAULT_PORT, LanRoomDiscovery.DEFAULT_PORT);
    /**
     * The settings of runs that tell their ports by other means, such as the headless runner and the tests: any free port for the lobby
     * and the discovery.
     */
    public static final @NotNull DemoNetworkSettings ANY_PORTS = new DemoNetworkSettings(ANY_PORT, ANY_PORT);

    /**
     * Creates the turn timing of a turn rate: the game loops of a turn and the latency in turns.
     *
     * @param turnsPerSecond the turn rate, one of {@link #TURN_RATES}
     * @return the created {@link TurnSchedule} instance
     * @throws IllegalArgumentException if the turn rate is not one of {@link #TURN_RATES}
     */
    public static @NotNull TurnSchedule createSchedule(int turnsPerSecond) {
        if (!TURN_RATES.contains(turnsPerSecond)) {
            throw Exceptions.newIllegalArgumentException("Invalid turn rate {0}; the rates are {1}.", turnsPerSecond, TURN_RATES);
        }

        return new TurnSchedule(LOOPS_PER_SECOND / turnsPerSecond, LATENCY_TURNS);
    }


    private final int lobbyPort;
    private final int discoveryPort;

    /**
     * Creates a {@link DemoNetworkSettings} instance.
     *
     * @param lobbyPort     the TCP port of a hosted room's lobby, {@link #ANY_PORT} for any free port
     * @param discoveryPort the UDP port of the room discovery, {@link #ANY_PORT} for any free port
     */
    public DemoNetworkSettings(int lobbyPort, int discoveryPort) {
        this.lobbyPort     = lobbyPort;
        this.discoveryPort = discoveryPort;
    }

    /**
     * @return the address of a hosted room's lobby
     */
    public @NotNull InetSocketAddress getLobbyAddress() {
        return new InetSocketAddress(LOOPBACK, this.lobbyPort);
    }

    /**
     * @return the address of the room discovery: where a host answers and a joiner asks
     */
    public @NotNull InetSocketAddress getDiscoveryAddress() {
        return new InetSocketAddress(LOOPBACK, this.discoveryPort);
    }

    /**
     * @return the address a machine's game endpoint binds to: any free port
     */
    public @NotNull InetSocketAddress getGameAddress() {
        return new InetSocketAddress(LOOPBACK, ANY_PORT);
    }

    /**
     * @return the turn timing of the games at the default turn rate
     */
    public @NotNull TurnSchedule getSchedule() {
        return createSchedule(DEFAULT_TURN_RATE);
    }

    /**
     * @return the resend, overdue and drop times of the games' transport: the engine defaults
     */
    public @NotNull TransportTimeouts getTimeouts() {
        return TransportTimeouts.DEFAULTS;
    }
}
