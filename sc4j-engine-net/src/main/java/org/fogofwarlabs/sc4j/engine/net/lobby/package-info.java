/**
 * Defines the engine's network lobby: joining a hosted room over TCP on a local network or over the Beacon.Link overlay (a published room,
 * reliable lobby messages on the overlay's lobby port, a mesh of channels among the members), with the mod identity check and its refusal
 * reasons, the members with their player slots, factions and ready flags, the room's map checked by name and digest and sent to a member that
 * misses it ({@link org.fogofwarlabs.sc4j.engine.net.lobby.LobbyMaps}; by default {@link org.fogofwarlabs.sc4j.engine.net.lobby.FolderLobbyMaps}
 * over the {@link org.fogofwarlabs.sc4j.engine.net.lobby.MapFolders} of a mod), and the start that hands every machine the same setup, seed,
 * turn timing and participant addresses ({@link org.fogofwarlabs.sc4j.engine.net.lobby.NetworkLobby},
 * {@link org.fogofwarlabs.sc4j.engine.net.lobby.NetworkGameStart}).
 */
package org.fogofwarlabs.sc4j.engine.net.lobby;
