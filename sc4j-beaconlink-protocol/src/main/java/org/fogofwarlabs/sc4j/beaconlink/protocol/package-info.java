/**
 * Defines the Beacon.Link wire protocol shared by the node executable and the client: the protocol clock
 * ({@link org.fogofwarlabs.sc4j.beaconlink.protocol.ProtocolClock}), the identities ({@code protocol.identity}), the bounded wire reader and
 * writer ({@code protocol.wire}), the signed room advertisement ({@code protocol.room}), the topics and their records ({@code protocol.topic}),
 * the network coordinates ({@code protocol.coordinate}), the presences and chat lines ({@code protocol.community}) and the signed messages
 * ({@code protocol.message}).
 * <p>
 * Every decoder of the protocol reports malformed input as a failure value and never throws, so a socket loop never sees an exception from a
 * datagram.
 */
package org.fogofwarlabs.sc4j.beaconlink.protocol;
