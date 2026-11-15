package org.fogofwarlabs.sc4j.beaconlink.protocol.file;

/**
 * Lists why a node list file ({@link NodeListFile}) could not be read.
 */
public enum NodeListProblemKind {
    /**
     * The file exists but cannot be read.
     */
    UNREADABLE,
    /**
     * The file is longer than {@value NodeListFile#MAXIMUM_FILE_LENGTH} bytes.
     */
    TOO_LARGE,
    /**
     * The file is not valid JSON, or it nests deeper than a node list.
     */
    NOT_JSON,
    /**
     * The file is JSON but not an object with a {@code format} number and a {@code nodes} array.
     */
    NOT_A_NODE_LIST,
    /**
     * The file is a node list of another format version.
     */
    UNSUPPORTED_FORMAT,
    /**
     * An entry of the {@code nodes} array is not a valid node.
     */
    INVALID_ENTRY
}
