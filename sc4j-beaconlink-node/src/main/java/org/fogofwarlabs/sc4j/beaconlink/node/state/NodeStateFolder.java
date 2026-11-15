package org.fogofwarlabs.sc4j.beaconlink.node.state;

import org.fogofwarlabs.sc4j.beaconlink.peer.bootstrap.PeerStore;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKeyFileUtils;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKeyPair;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;

/**
 * Holds the state folder a node operator names on the command line: the node's peer key file ({@value #KEY_FILE}), its learned peers
 * ({@value PeerStore#FILE_NAME}, the same shared peers file as the install's, {@link PeerStore}) and its logs ({@value #LOG_DIRECTORY}).
 * The node keeps nothing outside this folder.
 */
public final class NodeStateFolder {
    /**
     * The name of the key file.
     */
    public static final @NotNull String KEY_FILE = "identity.key";
    /**
     * The name of the log folder.
     */
    public static final @NotNull String LOG_DIRECTORY = "logs";

    /**
     * Creates a {@link NodeStateFolder} instance of a state folder, creating the folder when it does not exist.
     *
     * @param directory the folder
     * @return the created {@link NodeStateFolder} instance
     * @throws IOException if the folder cannot be created
     */
    public static @NotNull NodeStateFolder open(@NotNull Path directory) throws IOException {
        Path absolute = directory.toAbsolutePath().normalize();

        Files.createDirectories(absolute);

        return new NodeStateFolder(absolute);
    }


    private final @NotNull Path directory;

    private NodeStateFolder(@NotNull Path directory) {
        this.directory = directory;
    }

    /**
     * @return the absolute state folder
     */
    public @NotNull Path getDirectory() {
        return this.directory;
    }

    /**
     * @return the log folder inside the state folder
     */
    public @NotNull Path getLogDirectory() {
        return this.directory.resolve(LOG_DIRECTORY);
    }

    /**
     * @return the peers file inside the state folder
     */
    public @NotNull Path getPeersFile() {
        return this.directory.resolve(PeerStore.FILE_NAME);
    }


    /**
     * Reads the node's peer key pair, or creates and writes one at the first start.
     *
     * @param random the source of a new private key
     * @return the node's key pair
     * @throws IOException if the key file cannot be read or written, or it is broken
     */
    public @NotNull IdentityKeyPair readOrCreateKey(@NotNull SecureRandom random) throws IOException {
        return IdentityKeyFileUtils.readOrCreate(this.directory.resolve(KEY_FILE), random);
    }
}
