/**
 * Holds the engine game sessions of a client and their default screen: the {@link org.fogofwarlabs.sc4j.engine.client.game.GameSessionFactory}
 * creates local, replay and network {@link org.fogofwarlabs.sc4j.engine.client.game.GameSession}s of an engine game (local commands through a
 * {@link org.fogofwarlabs.sc4j.engine.client.game.LocalCommandQueue} or the mod's own sink), each with the view of its
 * {@link org.fogofwarlabs.sc4j.engine.client.game.GameViewFactory} and one command source applied at its
 * {@link org.fogofwarlabs.sc4j.engine.client.game.CommandPoint}, and the {@link org.fogofwarlabs.sc4j.engine.client.game.GameScreen} shows a
 * session with the engine world view and HUD. The {@link org.fogofwarlabs.sc4j.engine.client.game.SimulationWatchdog} reports a simulation frame
 * that does not return, and the {@link org.fogofwarlabs.sc4j.engine.client.game.ReplayAutosave} writes a live game's replay at its end or while
 * a frame stalls.
 */
package org.fogofwarlabs.sc4j.engine.client.game;
