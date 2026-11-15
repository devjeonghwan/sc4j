/**
 * Edits and checks the Beacon.Link node list of the install without a user interface: entry checks with reasons and the proposed lists of
 * the add, replace, remove and move actions ({@link org.fogofwarlabs.sc4j.launcher.nodes.NodeListEditor}), and the node Ping with
 * reachability, round-trip delay, observed address and node version
 * ({@link org.fogofwarlabs.sc4j.launcher.nodes.NodePinger}). The list itself is read and written by the Beacon.Link protocol's shared node
 * list file.
 */
package org.fogofwarlabs.sc4j.launcher.nodes;
