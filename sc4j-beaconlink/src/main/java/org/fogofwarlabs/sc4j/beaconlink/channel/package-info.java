/**
 * Defines the wire forms of Beacon.Link participant channels: the signals that open a channel through the nodes
 * ({@link org.fogofwarlabs.sc4j.beaconlink.channel.ChannelSignal}: offer and answer with the ephemeral keys and the address candidates),
 * the key schedule ({@link org.fogofwarlabs.sc4j.beaconlink.channel.EphemeralKeyPair},
 * {@link org.fogofwarlabs.sc4j.beaconlink.channel.ChannelKeys}) and the sealed channel datagrams that go between the participants
 * ({@link org.fogofwarlabs.sc4j.beaconlink.channel.ChannelCipher} with its {@link org.fogofwarlabs.sc4j.beaconlink.channel.ReplayWindow},
 * frames {@link org.fogofwarlabs.sc4j.beaconlink.channel.ChannelFrame}, among them the relay frames a participant passes on for two others
 * without opening them). An offer to a room's host also asks to join the room; the host answers or refuses it
 * ({@link org.fogofwarlabs.sc4j.beaconlink.channel.ChannelRefusal}). Whispers are sealed end to end with the same primitives
 * ({@link org.fogofwarlabs.sc4j.beaconlink.channel.WhisperSealUtils}). Readers never throw for malformed input.
 */
package org.fogofwarlabs.sc4j.beaconlink.channel;
