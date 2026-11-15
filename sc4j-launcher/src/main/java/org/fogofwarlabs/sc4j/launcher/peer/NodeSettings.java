package org.fogofwarlabs.sc4j.launcher.peer;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.beaconlink.protocol.file.ConfiguredNode;
import org.jetbrains.annotations.NotNull;

/**
 * Holds the settings of the launcher's node: its role and the UDP port it listens on in the {@link NodeRole#AUTOMATIC} role. Instances are
 * immutable.
 */
public final class NodeSettings {
    /**
     * The default UDP port of the launcher's node.
     */
    public static final int DEFAULT_PORT = 7455;
    /**
     * The settings before anything is saved: the automatic role on the default port.
     */
    public static final @NotNull NodeSettings DEFAULT = new NodeSettings(NodeRole.AUTOMATIC, DEFAULT_PORT);

    private final @NotNull NodeRole role;
    private final int port;

    /**
     * Creates a {@link NodeSettings} instance.
     *
     * @param role the node's role
     * @param port the UDP port the node listens on in the automatic role
     * @throws IllegalArgumentException if the port is outside {@value ConfiguredNode#MINIMUM_PORT} to {@value ConfiguredNode#MAXIMUM_PORT}
     */
    public NodeSettings(@NotNull NodeRole role, int port) {
        if (!ConfiguredNode.isValidPort(port)) {
            throw Exceptions.newIllegalArgumentException("The node port {0} is outside {1}..{2}.", port, ConfiguredNode.MINIMUM_PORT,
                                                         ConfiguredNode.MAXIMUM_PORT);
        }

        this.role = role;
        this.port = port;
    }

    /**
     * @return the node's role
     */
    public @NotNull NodeRole getRole() {
        return this.role;
    }

    /**
     * @return the UDP port the node listens on in the automatic role
     */
    public int getPort() {
        return this.port;
    }
}
