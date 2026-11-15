/**
 * Defines the engine's LAN room discovery: hosts answer UDP queries with their game room (name, members, lobby port and mod identity) and may
 * announce it unasked; clients collect the found rooms with their identity mismatch
 * ({@link org.fogofwarlabs.sc4j.engine.net.discovery.LanRoomDiscovery}).
 */
package org.fogofwarlabs.sc4j.engine.net.discovery;
