/**
 * Defines the Beacon.Link client: what the sessions of a run use as its command line names it, the account, the seeds and the shared peers
 * file ({@link org.fogofwarlabs.sc4j.beaconlink.client.ClientSetup}), sessions with several nodes (registration with the address token,
 * refresh, observed address, resend and timeout), room publication, refresh and withdrawal, room searches by mod fingerprint
 * ({@link org.fogofwarlabs.sc4j.beaconlink.client.BeaconLinkClient}), and the community part: presence, chat channels, whispers and
 * friends through the session's home nodes. It implements the engine's Beacon.Link interface, which the runtime gives a mod
 * ({@link org.fogofwarlabs.sc4j.beaconlink.client.ClientBeaconLink}). Nodes carry no game traffic.
 */
package org.fogofwarlabs.sc4j.beaconlink.client;
