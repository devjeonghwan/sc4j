/**
 * Holds what client code may see of a running game: the read-only {@link org.fogofwarlabs.sc4j.engine.client.view.GameView} (frame, players, map,
 * units, visibility, resources) with its engine default {@link org.fogofwarlabs.sc4j.engine.client.view.StandardGameView} over an engine game, and
 * the read-only {@link org.fogofwarlabs.sc4j.engine.client.view.UnitView} of one unit with its engine default
 * {@link org.fogofwarlabs.sc4j.engine.client.view.StandardUnitView}. Units and players cross the view as the game's own objects;
 * the client changes the game only through commands.
 */
package org.fogofwarlabs.sc4j.engine.client.view;
