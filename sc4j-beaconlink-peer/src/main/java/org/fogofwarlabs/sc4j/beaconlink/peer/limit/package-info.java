/**
 * Defines the peer's abuse limits: token-bucket rate limits with a bounded key table
 * ({@link org.fogofwarlabs.sc4j.beaconlink.peer.limit.RateLimiter}), the address tokens that prove a source address
 * ({@link org.fogofwarlabs.sc4j.beaconlink.peer.limit.AddressTokens}) and the local penalties and graylist of misbehaving peers
 * ({@link org.fogofwarlabs.sc4j.beaconlink.peer.limit.Graylist}).
 */
package org.fogofwarlabs.sc4j.beaconlink.peer.limit;
