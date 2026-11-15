/**
 * Defines the Beacon.Link node executable, a headless peer: the entry point ({@link org.fogofwarlabs.sc4j.beaconlink.node.NodeMain}) runs
 * the same peer as the launcher's node, a peer of the peer library on a socket of its own
 * ({@link org.fogofwarlabs.sc4j.beaconlink.peer.PeerSocket}, {@link org.fogofwarlabs.sc4j.beaconlink.peer.bootstrap.PeerBootstrap}), with the
 * state folder and the command line of this module.
 * <p>
 * The peer takes part in the routing and the topics of the network and serves clients as their home peer. It carries no game traffic and
 * needs no engine.
 */
package org.fogofwarlabs.sc4j.beaconlink.node;
