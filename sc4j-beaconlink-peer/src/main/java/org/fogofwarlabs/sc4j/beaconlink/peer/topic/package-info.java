/**
 * Defines the peer's topic layer: the announcements a peer stores for the topics it is a storing peer of
 * ({@link org.fogofwarlabs.sc4j.beaconlink.peer.topic.TopicDirectory}), and the topics it takes part in with their members, mesh, subscribed
 * clients and gossip of signed records: room advertisements, presences and chat lines
 * ({@link org.fogofwarlabs.sc4j.beaconlink.peer.topic.TopicSwarms}). The full peers closest to a topic's key store who is in it; the members
 * push new records to their mesh neighbours and their subscribed clients and name them lazily to others, so that the load of a topic stays
 * with the peers that take part in it.
 */
package org.fogofwarlabs.sc4j.beaconlink.peer.topic;
