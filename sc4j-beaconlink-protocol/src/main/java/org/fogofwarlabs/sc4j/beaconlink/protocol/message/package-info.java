/**
 * Defines the Beacon.Link messages: the signed envelope and its decoder
 * ({@link org.fogofwarlabs.sc4j.beaconlink.protocol.message.Envelope}), the encoder
 * ({@link org.fogofwarlabs.sc4j.beaconlink.protocol.message.MessageEncoder}), the kinds
 * ({@link org.fogofwarlabs.sc4j.beaconlink.protocol.message.MessageKind}) with one body class each, and the replay guard
 * ({@link org.fogofwarlabs.sc4j.beaconlink.protocol.message.ReplayGuard}).
 * <p>
 * Protocol {@value org.fogofwarlabs.sc4j.beaconlink.protocol.message.Envelope#PROTOCOL_VERSION}. Client to node (a peer serving the client):
 * {@code REGISTER} (answered by {@code OBSERVED_ADDRESS}), {@code PING}
 * ({@code PONG}), {@code PUBLISH_ROOM}, {@code WITHDRAW_ROOM} and {@code SIGNAL} ({@code RESULT}), {@code LIST_ROOMS} ({@code ROOM_LIST}),
 * {@code FIND_PEERS} ({@code PEERS}). Peer to peer: {@code HELLO} (answered by the same kind with the reply flag), {@code FIND_PEERS}
 * ({@code PEERS}), {@code DIAL_BACK} and {@code PROBE} (the reachability self-test). Topic swarms: {@code GET_TOPIC} ({@code TOPIC_PEERS}),
 * {@code ANNOUNCE} and {@code GRAFT} ({@code RESULT}), {@code PRUNE}, {@code GOSSIP}, {@code IHAVE} and {@code IWANT}. Community, client to
 * home peer: {@code PUBLISH_RECORD} and {@code RECENT} ({@code RESULT}; the peer then pushes {@code GOSSIP} and {@code IHAVE}, the client
 * asks with {@code IWANT}); player to a target's home peer: {@code SEALED} ({@code RESULT}). Peers are placed by their
 * {@link org.fogofwarlabs.sc4j.beaconlink.protocol.identity.PeerId} and named by
 * {@link org.fogofwarlabs.sc4j.beaconlink.protocol.message.PeerContact} entries.
 */
package org.fogofwarlabs.sc4j.beaconlink.protocol.message;
