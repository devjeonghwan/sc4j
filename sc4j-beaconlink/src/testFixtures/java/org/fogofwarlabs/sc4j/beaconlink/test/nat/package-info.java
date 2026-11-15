/**
 * Emulates NATs on 127.0.0.1 for the Beacon.Link tests: every public mapping is a real loopback UDP socket, so nodes and clients run
 * unchanged and observe the emulated public addresses ({@link org.fogofwarlabs.sc4j.beaconlink.test.nat.LoopbackNat}).
 */
package org.fogofwarlabs.sc4j.beaconlink.test.nat;
