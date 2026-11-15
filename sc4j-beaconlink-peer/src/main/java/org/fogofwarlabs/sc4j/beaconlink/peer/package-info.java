/**
 * Defines the Beacon.Link peer library: the peer ({@link org.fogofwarlabs.sc4j.beaconlink.peer.MeshPeer}) with its statistics and drop
 * reasons, the socket a peer of its own runs on ({@link org.fogofwarlabs.sc4j.beaconlink.peer.PeerSocket}), the sink through which an
 * embedded peer sends on its owner's socket ({@link org.fogofwarlabs.sc4j.beaconlink.peer.DatagramSink}).
 * <p>
 * A peer runs the peer layer of the protocol (package {@code routing}: proof-of-work IDs, k-bucket routing table, lookups), the topic layer
 * (package {@code topic}: who is in a topic, the swarm of each topic and the gossip of its signed records) and the home-peer roles: it
 * registers clients after they prove their source address, tells them the address it observed, takes part in the mod topics of their rooms
 * and searches and in the topics of their presences and channels (pushing them the records of the topics they subscribed to), relays signals
 * between registered clients and sealed whispers to a registered client. It carries no game traffic and needs no engine, so it runs in the
 * launcher, in the runtime's Beacon.Link sessions and in the headless node program.
 */
package org.fogofwarlabs.sc4j.beaconlink.peer;
