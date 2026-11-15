package org.fogofwarlabs.sc4j.launcher.nodes;

/**
 * Lists why the node editor's fields do not make an entry that can be added to or replaced in the node list.
 */
public enum NodeEntryProblem {
    /**
     * The host is empty or holds white space or brackets.
     */
    HOST_INVALID,
    /**
     * The port is not a decimal number in the port range.
     */
    PORT_INVALID,
    /**
     * Another entry of the list has the same address.
     */
    DUPLICATE,
    /**
     * The list already holds {@value NodeListEditor#MAXIMUM_NODES} nodes.
     */
    LIST_FULL
}
